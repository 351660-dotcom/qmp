package com.qmp.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.hotel.dto.AvailabilityResponse;
import com.qmp.hotel.entity.RoomInventoryBucket;
import com.qmp.hotel.entity.RoomNightReservation;
import com.qmp.hotel.error.HotelErrorCode;
import com.qmp.hotel.mapper.RoomInventoryBucketMapper;
import com.qmp.hotel.mapper.RoomNightReservationMapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 房晚库存服务：连住「多夜原子预占」+ 确认/释放（11 文档 1.3）。
 *
 * <p>一次连住预占 = 区间 [check_in, check_out) 内 N 个不同 {@code room_inventory_bucket} 的预占，
 * 必须全部成功；任一晚不足则对已成功的前序晚执行补偿释放（与门票链路 order-service 创建订单的
 * 显式补偿同构）。v1 仅用 DB 条件更新第二道防线防超卖；高并发下可再叠加 inventory-kernel 的
 * Redis Lua 第一道防线（与 inventory-service 同款，见 CLAUDE.md）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotelInventoryService {

    private final RoomInventoryBucketMapper bucketMapper;
    private final RoomNightReservationMapper nightReservationMapper;

    @Value("${hotel.reservation.hold-minutes:30}")
    private int holdMinutes;

    /** 连住的间夜列表：钟点房（check_in == check_out）占用「当晚」一个桶。 */
    public List<LocalDate> nightsOf(LocalDate checkIn, LocalDate checkOut) {
        if (checkOut.isBefore(checkIn)) {
            throw new BizException(HotelErrorCode.INVALID_DATE_RANGE);
        }
        List<LocalDate> nights = new ArrayList<>();
        if (checkIn.equals(checkOut)) {
            nights.add(checkIn); // day-use
            return nights;
        }
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            nights.add(d);
        }
        return nights;
    }

    /**
     * 多夜原子预占。幂等：若该预订单已存在间夜预占记录则直接返回（不重复预占）。
     */
    @Transactional
    public void reserveStay(Long hotelReservationId, Long skuId, List<LocalDate> nights, int roomCount) {
        List<RoomNightReservation> existing = nightReservationMapper.selectList(
                new LambdaQueryWrapper<RoomNightReservation>()
                        .eq(RoomNightReservation::getHotelReservationId, hotelReservationId));
        if (!existing.isEmpty()) {
            return;
        }

        List<RoomInventoryBucket> locked = new ArrayList<>();
        try {
            for (LocalDate night : nights) {
                RoomInventoryBucket bucket = findBucket(skuId, night);
                if (bucket == null) {
                    throw new BizException(HotelErrorCode.BUCKET_NOT_FOUND,
                            "房型 " + skuId + " 在 " + night + " 未配置房晚库存");
                }
                if (bucketMapper.tryLock(bucket.getBucketId(), roomCount) == 0) {
                    throw new BizException(HotelErrorCode.INSUFFICIENT_ROOM,
                            night + " 房晚库存不足");
                }
                locked.add(bucket);
            }
        } catch (RuntimeException e) {
            // 补偿：释放已成功锁定的前序间夜（多夜原子性）
            for (RoomInventoryBucket b : locked) {
                bucketMapper.releaseLock(b.getBucketId(), roomCount);
            }
            throw e;
        }

        Long tenantId = TenantContext.get();
        for (int i = 0; i < nights.size(); i++) {
            LocalDate night = nights.get(i);
            RoomInventoryBucket bucket = locked.get(i);
            RoomNightReservation r = new RoomNightReservation();
            r.setReservationId(hotelReservationId + ":" + night);
            r.setTenantId(tenantId);
            r.setHotelReservationId(hotelReservationId);
            r.setSaleDate(night);
            r.setBucketId(bucket.getBucketId());
            r.setQuantity(roomCount);
            r.setStatus("HOLDING");
            r.setHoldExpireAt(java.time.LocalDateTime.now().plusMinutes(holdMinutes));
            nightReservationMapper.insert(r);
        }
        log.info("连住预占成功: reservationId={}, nights={}, roomCount={}", hotelReservationId, nights.size(), roomCount);
    }

    @Transactional
    public void confirmStay(Long hotelReservationId) {
        for (RoomNightReservation r : listNights(hotelReservationId)) {
            if ("HOLDING".equals(r.getStatus())) {
                bucketMapper.confirmLock(r.getBucketId(), r.getQuantity());
                r.setStatus("CONFIRMED");
                nightReservationMapper.updateById(r);
            }
        }
    }

    @Transactional
    public void releaseStay(Long hotelReservationId) {
        for (RoomNightReservation r : listNights(hotelReservationId)) {
            switch (r.getStatus()) {
                case "HOLDING" -> {
                    bucketMapper.releaseLock(r.getBucketId(), r.getQuantity());
                    r.setStatus("RELEASED");
                    nightReservationMapper.updateById(r);
                }
                case "CONFIRMED" -> {
                    bucketMapper.releaseSold(r.getBucketId(), r.getQuantity());
                    r.setStatus("RELEASED");
                    nightReservationMapper.updateById(r);
                }
                default -> { /* RELEASED/EXPIRED 幂等跳过 */ }
            }
        }
    }

    public AvailabilityResponse availability(Long skuId, LocalDate checkIn, LocalDate checkOut) {
        List<LocalDate> nights = nightsOf(checkIn, checkOut);
        List<AvailabilityResponse.Night> views = new ArrayList<>();
        int minRemain = Integer.MAX_VALUE;
        for (LocalDate night : nights) {
            RoomInventoryBucket bucket = findBucket(skuId, night);
            int remain = bucket == null ? 0
                    : nz(bucket.getTotalQuota()) - nz(bucket.getSoldCount()) - nz(bucket.getLockedCount());
            remain = Math.max(remain, 0);
            minRemain = Math.min(minRemain, remain);
            views.add(AvailabilityResponse.Night.builder().saleDate(night).remain(remain).build());
        }
        return AvailabilityResponse.builder()
                .skuId(skuId)
                .minRemain(minRemain == Integer.MAX_VALUE ? 0 : minRemain)
                .nights(views)
                .build();
    }

    private List<RoomNightReservation> listNights(Long hotelReservationId) {
        return nightReservationMapper.selectList(new LambdaQueryWrapper<RoomNightReservation>()
                .eq(RoomNightReservation::getHotelReservationId, hotelReservationId)
                .orderByAsc(RoomNightReservation::getSaleDate));
    }

    private RoomInventoryBucket findBucket(Long skuId, LocalDate saleDate) {
        return bucketMapper.selectOne(new LambdaQueryWrapper<RoomInventoryBucket>()
                .eq(RoomInventoryBucket::getSkuId, skuId)
                .eq(RoomInventoryBucket::getSaleDate, saleDate)
                .eq(RoomInventoryBucket::getTimeSlotId, 0L));
    }

    private int nz(Integer v) {
        return v != null ? v : 0;
    }
}

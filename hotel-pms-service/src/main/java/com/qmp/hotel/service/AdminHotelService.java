package com.qmp.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.hotel.dto.admin.CreateRoomTypeRequest;
import com.qmp.hotel.dto.admin.UpsertRoomBucketRequest;
import com.qmp.hotel.entity.RoomInventoryBucket;
import com.qmp.hotel.entity.RoomType;
import com.qmp.hotel.error.HotelErrorCode;
import com.qmp.hotel.mapper.RoomInventoryBucketMapper;
import com.qmp.hotel.mapper.RoomTypeMapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

/**
 * 酒店后台管理：维护房型、按区间铺/调房晚库存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminHotelService {

    private static final Set<String> VALID_STATUS = Set.of("DRAFT", "ON_SALE", "OFF_SALE");

    private final RoomTypeMapper roomTypeMapper;
    private final RoomInventoryBucketMapper bucketMapper;

    public Long createRoomType(CreateRoomTypeRequest request) {
        String status = request.getStatus() != null ? request.getStatus() : "DRAFT";
        if (!VALID_STATUS.contains(status)) {
            throw new BizException(HotelErrorCode.RESERVATION_INVALID_STATE, "非法房型状态");
        }
        RoomType rt = new RoomType();
        rt.setTenantId(TenantContext.get());
        rt.setScenicId(request.getScenicId());
        rt.setMerchantId(request.getMerchantId());
        rt.setSkuId(request.getSkuId());
        rt.setName(request.getName());
        rt.setStatus(status);
        rt.setBasePrice(request.getBasePrice());
        roomTypeMapper.insert(rt);
        log.info("后台创建房型: roomTypeId={}, skuId={}", rt.getRoomTypeId(), request.getSkuId());
        return rt.getRoomTypeId();
    }

    public void updateStatus(Long roomTypeId, String status) {
        if (!VALID_STATUS.contains(status)) {
            throw new BizException(HotelErrorCode.RESERVATION_INVALID_STATE, "非法房型状态");
        }
        RoomType rt = roomTypeMapper.selectById(roomTypeId);
        if (rt == null) {
            throw new BizException(HotelErrorCode.ROOM_TYPE_NOT_FOUND);
        }
        rt.setStatus(status);
        roomTypeMapper.updateById(rt);
    }

    /** 按区间 [from, to) 铺房晚库存；to 缺省为 from 次日（铺一晚）。返回写入晚数。 */
    public int upsertBuckets(UpsertRoomBucketRequest request) {
        LocalDate from = request.getFromDate();
        LocalDate to = request.getToDate() != null ? request.getToDate() : from.plusDays(1);
        if (to.isBefore(from) || to.equals(from)) {
            throw new BizException(HotelErrorCode.INVALID_DATE_RANGE);
        }
        int count = 0;
        for (LocalDate d = from; d.isBefore(to); d = d.plusDays(1)) {
            upsertOneNight(request, d);
            count++;
        }
        log.info("后台铺房晚库存: skuId={}, [{}, {}), nights={}, quota={}",
                request.getSkuId(), from, to, count, request.getTotalQuota());
        return count;
    }

    private void upsertOneNight(UpsertRoomBucketRequest request, LocalDate date) {
        RoomInventoryBucket existing = bucketMapper.selectOne(new LambdaQueryWrapper<RoomInventoryBucket>()
                .eq(RoomInventoryBucket::getSkuId, request.getSkuId())
                .eq(RoomInventoryBucket::getSaleDate, date)
                .eq(RoomInventoryBucket::getTimeSlotId, 0L));
        if (existing != null) {
            int used = nz(existing.getSoldCount()) + nz(existing.getLockedCount());
            if (request.getTotalQuota() < used) {
                throw new BizException(HotelErrorCode.INVALID_QUOTA);
            }
            existing.setTotalQuota(request.getTotalQuota());
            bucketMapper.updateById(existing);
            return;
        }
        RoomInventoryBucket bucket = new RoomInventoryBucket();
        bucket.setTenantId(TenantContext.get());
        bucket.setScenicId(request.getScenicId());
        bucket.setSkuId(request.getSkuId());
        bucket.setSaleDate(date);
        bucket.setTimeSlotId(0L);
        bucket.setTotalQuota(request.getTotalQuota());
        bucket.setSoldCount(0);
        bucket.setLockedCount(0);
        bucket.setChannelQuota("{\"direct\": " + request.getTotalQuota() + "}");
        bucket.setVersion(0);
        bucketMapper.insert(bucket);
    }

    private int nz(Integer v) {
        return v != null ? v : 0;
    }
}

package com.qmp.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.inventory.dto.AvailabilityResponse;
import com.qmp.inventory.dto.CreateReservationRequest;
import com.qmp.inventory.dto.ReservationResponse;
import com.qmp.inventory.entity.InventoryBucket;
import com.qmp.inventory.entity.InventoryReservation;
import com.qmp.inventory.error.InventoryErrorCode;
import com.qmp.inventory.mapper.InventoryBucketMapper;
import com.qmp.inventory.mapper.InventoryReservationMapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.inventory.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 库存预占核心服务：落地 ADR-025 / 06 文档「Redis 预扣 + DB 条件更新」两道防线
 * + HOLDING/CONFIRMED/RELEASED/EXPIRED 状态机（07 文档 1.4，对外接口见 09 文档五）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryBucketMapper bucketMapper;
    private final InventoryReservationMapper reservationMapper;
    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> inventoryReserveScript;
    private final RedisScript<Long> inventoryReleaseScript;

    @Value("${inventory.reservation.hold-minutes:15}")
    private long holdMinutes;

    public AvailabilityResponse getAvailability(Long skuId, LocalDate saleDate, Long timeSlotId) {
        InventoryBucket bucket = findBucket(skuId, saleDate, timeSlotId);
        int remain = bucket.getTotalQuota() - bucket.getSoldCount() - bucket.getLockedCount();
        return AvailabilityResponse.builder()
                .totalQuota(bucket.getTotalQuota())
                .soldCount(bucket.getSoldCount())
                .lockedCount(bucket.getLockedCount())
                .remain(Math.max(remain, 0))
                .build();
    }

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        InventoryReservation existing = reservationMapper.selectById(request.getReservationId());
        if (existing != null) {
            return toResponse(existing);
        }

        long timeSlotId = request.getTimeSlotId() != null ? request.getTimeSlotId() : 0L;
        InventoryBucket bucket = findBucket(request.getSkuId(), request.getSaleDate(), timeSlotId);
        int qty = request.getQuantity();

        // 第一道防线：Redis 预扣（冷启动时按 DB 当前余量初始化缓存）
        String remainKey = remainKey(bucket.getBucketId());
        int dbRemain = bucket.getTotalQuota() - bucket.getSoldCount() - bucket.getLockedCount();
        redisTemplate.opsForValue().setIfAbsent(remainKey, String.valueOf(Math.max(dbRemain, 0)));

        Long redisRemain = redisTemplate.execute(inventoryReserveScript,
                Collections.singletonList(remainKey), String.valueOf(qty));
        if (redisRemain == null || redisRemain < 0) {
            throw new BizException(InventoryErrorCode.INSUFFICIENT_STOCK);
        }

        // 第二道防线：DB 条件更新，最终防止超卖
        int updated = bucketMapper.tryLock(bucket.getBucketId(), qty);
        if (updated == 0) {
            redisTemplate.execute(inventoryReleaseScript, Collections.singletonList(remainKey), String.valueOf(qty));
            throw new BizException(InventoryErrorCode.INSUFFICIENT_STOCK);
        }

        LocalDateTime holdExpireAt = LocalDateTime.now().plusMinutes(holdMinutes);
        InventoryReservation reservation = new InventoryReservation();
        reservation.setReservationId(request.getReservationId());
        reservation.setTenantId(TenantContext.get());
        reservation.setBucketId(bucket.getBucketId());
        reservation.setQuantity(qty);
        reservation.setStatus(ReservationStatus.HOLDING.name());
        reservation.setHoldExpireAt(holdExpireAt);
        reservationMapper.insert(reservation);

        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse confirmReservation(String reservationId) {
        InventoryReservation reservation = getReservationOrThrow(reservationId);

        if (ReservationStatus.CONFIRMED.name().equals(reservation.getStatus())) {
            return toResponse(reservation);
        }
        if (!ReservationStatus.HOLDING.name().equals(reservation.getStatus())) {
            throw new BizException(InventoryErrorCode.RESERVATION_INVALID_STATE);
        }

        int updated = bucketMapper.confirmLock(reservation.getBucketId(), reservation.getQuantity());
        if (updated == 0) {
            throw new BizException(InventoryErrorCode.RESERVATION_INVALID_STATE);
        }

        reservation.setStatus(ReservationStatus.CONFIRMED.name());
        reservationMapper.updateById(reservation);
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse releaseReservation(String reservationId) {
        InventoryReservation reservation = getReservationOrThrow(reservationId);

        if (ReservationStatus.RELEASED.name().equals(reservation.getStatus())
                || ReservationStatus.EXPIRED.name().equals(reservation.getStatus())) {
            return toResponse(reservation);
        }

        if (ReservationStatus.HOLDING.name().equals(reservation.getStatus())) {
            bucketMapper.releaseLock(reservation.getBucketId(), reservation.getQuantity());
        } else {
            bucketMapper.releaseSold(reservation.getBucketId(), reservation.getQuantity());
        }
        redisTemplate.execute(inventoryReleaseScript,
                Collections.singletonList(remainKey(reservation.getBucketId())),
                String.valueOf(reservation.getQuantity()));

        reservation.setStatus(ReservationStatus.RELEASED.name());
        reservationMapper.updateById(reservation);
        return toResponse(reservation);
    }

    /**
     * ExpireReservation 定时任务（10 文档 5.2 {@code idx_expire_scan}）：
     * 扫描 HOLDING 且已超时的预占，释放库存并置为 EXPIRED。
     */
    @Transactional
    public void expireReservation(InventoryReservation reservation) {
        bucketMapper.releaseLock(reservation.getBucketId(), reservation.getQuantity());
        redisTemplate.execute(inventoryReleaseScript,
                Collections.singletonList(remainKey(reservation.getBucketId())),
                String.valueOf(reservation.getQuantity()));

        reservation.setStatus(ReservationStatus.EXPIRED.name());
        reservationMapper.updateById(reservation);
    }

    public List<InventoryReservation> findExpiredHoldings(LocalDateTime now) {
        return reservationMapper.selectList(new LambdaQueryWrapper<InventoryReservation>()
                .eq(InventoryReservation::getStatus, ReservationStatus.HOLDING.name())
                .lt(InventoryReservation::getHoldExpireAt, now));
    }

    private InventoryReservation getReservationOrThrow(String reservationId) {
        InventoryReservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BizException(InventoryErrorCode.RESERVATION_NOT_FOUND);
        }
        return reservation;
    }

    private InventoryBucket findBucket(Long skuId, LocalDate saleDate, Long timeSlotId) {
        long slot = timeSlotId != null ? timeSlotId : 0L;
        InventoryBucket bucket = bucketMapper.selectOne(new LambdaQueryWrapper<InventoryBucket>()
                .eq(InventoryBucket::getSkuId, skuId)
                .eq(InventoryBucket::getSaleDate, saleDate)
                .eq(InventoryBucket::getTimeSlotId, slot));
        if (bucket == null) {
            throw new BizException(InventoryErrorCode.BUCKET_NOT_FOUND);
        }
        return bucket;
    }

    private String remainKey(Long bucketId) {
        return "inv:" + bucketId + ":remain";
    }

    private ReservationResponse toResponse(InventoryReservation reservation) {
        return ReservationResponse.builder()
                .reservationId(reservation.getReservationId())
                .status(reservation.getStatus())
                .holdExpireAt(reservation.getHoldExpireAt())
                .build();
    }
}

package com.qmp.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.marketing.dto.SnapResponse;
import com.qmp.marketing.entity.SeckillActivity;
import com.qmp.marketing.entity.SeckillInventoryBucket;
import com.qmp.marketing.entity.SeckillReservation;
import com.qmp.marketing.error.MarketingErrorCode;
import com.qmp.marketing.mapper.SeckillActivityMapper;
import com.qmp.marketing.mapper.SeckillInventoryBucketMapper;
import com.qmp.marketing.mapper.SeckillReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 秒杀抢购（13 文档 5.1）。独立库存桶（ADR-025）+ 复用防超卖条件更新。v1 限购 1（reservation_id=activity:user）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillService {

    private final SeckillActivityMapper activityMapper;
    private final SeckillInventoryBucketMapper bucketMapper;
    private final SeckillReservationMapper reservationMapper;

    @Value("${marketing.seckill.hold-minutes:10}")
    private int holdMinutes;

    @Transactional
    public SnapResponse snap(Long activityId, Long userId) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(MarketingErrorCode.SECKILL_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        boolean active = "ACTIVE".equals(activity.getStatus())
                && !now.isBefore(activity.getStartTime()) && now.isBefore(activity.getEndTime());
        if (!active) {
            throw new BizException(MarketingErrorCode.SECKILL_NOT_ACTIVE);
        }
        SeckillInventoryBucket bucket = bucketMapper.selectOne(new LambdaQueryWrapper<SeckillInventoryBucket>()
                .eq(SeckillInventoryBucket::getActivityId, activityId));
        if (bucket == null) {
            throw new BizException(MarketingErrorCode.SECKILL_BUCKET_NOT_FOUND);
        }

        String reservationId = activityId + ":" + userId;
        if (reservationMapper.selectById(reservationId) != null) {
            throw new BizException(MarketingErrorCode.SECKILL_ALREADY_SNAPPED);
        }
        if (bucketMapper.tryLock(bucket.getBucketId(), 1) == 0) {
            throw new BizException(MarketingErrorCode.SECKILL_SOLD_OUT);
        }

        SeckillReservation reservation = new SeckillReservation();
        reservation.setReservationId(reservationId);
        reservation.setTenantId(TenantContext.get());
        reservation.setActivityId(activityId);
        reservation.setUserId(userId);
        reservation.setBucketId(bucket.getBucketId());
        reservation.setQuantity(1);
        reservation.setStatus("HOLDING");
        reservation.setHoldExpireAt(now.plusMinutes(holdMinutes));
        reservationMapper.insert(reservation);

        log.info("秒杀抢购成功: activityId={}, userId={}", activityId, userId);
        return SnapResponse.builder()
                .reservationId(reservationId).status("HOLDING")
                .seckillPrice(activity.getSeckillPrice()).build();
    }

    /** 放弃/超时释放（HOLDING -> RELEASED）。 */
    @Transactional
    public void release(String reservationId) {
        SeckillReservation r = reservationMapper.selectById(reservationId);
        if (r == null || !"HOLDING".equals(r.getStatus())) {
            return;
        }
        bucketMapper.releaseLock(r.getBucketId(), r.getQuantity());
        r.setStatus("RELEASED");
        reservationMapper.updateById(r);
    }
}

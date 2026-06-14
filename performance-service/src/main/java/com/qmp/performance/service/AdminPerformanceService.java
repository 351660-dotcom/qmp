package com.qmp.performance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.performance.dto.admin.CreateSessionRequest;
import com.qmp.performance.dto.admin.UpsertSeatBucketRequest;
import com.qmp.performance.dto.admin.UpsertSessionBucketRequest;
import com.qmp.performance.entity.PerformanceSession;
import com.qmp.performance.entity.SeatInventoryBucket;
import com.qmp.performance.entity.SessionInventoryBucket;
import com.qmp.performance.error.PerformanceErrorCode;
import com.qmp.performance.mapper.PerformanceSessionMapper;
import com.qmp.performance.mapper.SeatInventoryBucketMapper;
import com.qmp.performance.mapper.SessionInventoryBucketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 演出后台管理：建场次、铺场次/座位库存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPerformanceService {

    private final PerformanceSessionMapper sessionMapper;
    private final SessionInventoryBucketMapper sessionBucketMapper;
    private final SeatInventoryBucketMapper seatBucketMapper;

    public Long createSession(CreateSessionRequest request) {
        PerformanceSession session = new PerformanceSession();
        session.setTenantId(TenantContext.get());
        session.setScenicId(request.getScenicId());
        session.setMerchantId(request.getMerchantId());
        session.setSkuId(request.getSkuId());
        session.setName(request.getName());
        session.setSessionType(request.getSessionType());
        session.setStartTime(request.getStartTime());
        session.setBasePrice(request.getBasePrice());
        session.setStatus(request.getStatus() != null ? request.getStatus() : "DRAFT");
        sessionMapper.insert(session);
        log.info("后台建场次: sessionId={}, type={}", session.getSessionId(), request.getSessionType());
        return session.getSessionId();
    }

    public void updateStatus(Long sessionId, String status) {
        PerformanceSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(PerformanceErrorCode.SESSION_NOT_FOUND);
        }
        session.setStatus(status);
        sessionMapper.updateById(session);
    }

    public Long upsertSessionBucket(UpsertSessionBucketRequest request) {
        SessionInventoryBucket existing = sessionBucketMapper.selectOne(
                new LambdaQueryWrapper<SessionInventoryBucket>()
                        .eq(SessionInventoryBucket::getSessionId, request.getSessionId()));
        if (existing != null) {
            int used = nz(existing.getSoldCount()) + nz(existing.getLockedCount());
            if (request.getTotalQuota() < used) {
                throw new BizException(PerformanceErrorCode.INVALID_QUOTA);
            }
            existing.setTotalQuota(request.getTotalQuota());
            sessionBucketMapper.updateById(existing);
            return existing.getBucketId();
        }
        SessionInventoryBucket bucket = new SessionInventoryBucket();
        bucket.setTenantId(TenantContext.get());
        bucket.setScenicId(request.getScenicId());
        bucket.setSessionId(request.getSessionId());
        bucket.setSkuId(0L);
        bucket.setSaleDate(java.time.LocalDate.now());
        bucket.setTimeSlotId(0L);
        bucket.setTotalQuota(request.getTotalQuota());
        bucket.setSoldCount(0);
        bucket.setLockedCount(0);
        bucket.setChannelQuota("{\"direct\": " + request.getTotalQuota() + "}");
        bucket.setVersion(0);
        sessionBucketMapper.insert(bucket);
        log.info("后台铺场次库存: sessionId={}, quota={}", request.getSessionId(), request.getTotalQuota());
        return bucket.getBucketId();
    }

    public Long upsertSeatBucket(UpsertSeatBucketRequest request) {
        int capacity = request.getCapacity() != null ? request.getCapacity() : 1;
        SeatInventoryBucket existing = seatBucketMapper.selectOne(new LambdaQueryWrapper<SeatInventoryBucket>()
                .eq(SeatInventoryBucket::getSessionId, request.getSessionId())
                .eq(SeatInventoryBucket::getSeatId, request.getSeatId()));
        if (existing != null) {
            int used = nz(existing.getSoldCount()) + nz(existing.getLockedCount());
            if (capacity < used) {
                throw new BizException(PerformanceErrorCode.INVALID_QUOTA);
            }
            existing.setTotalQuota(capacity);
            seatBucketMapper.updateById(existing);
            return existing.getBucketId();
        }
        SeatInventoryBucket bucket = new SeatInventoryBucket();
        bucket.setTenantId(TenantContext.get());
        bucket.setScenicId(request.getScenicId());
        bucket.setSessionId(request.getSessionId());
        bucket.setSeatId(request.getSeatId());
        bucket.setSkuId(0L);
        bucket.setSaleDate(java.time.LocalDate.now());
        bucket.setTimeSlotId(0L);
        bucket.setTotalQuota(capacity);
        bucket.setSoldCount(0);
        bucket.setLockedCount(0);
        bucket.setChannelQuota("{\"direct\": " + capacity + "}");
        bucket.setVersion(0);
        seatBucketMapper.insert(bucket);
        log.info("后台铺座位库存: sessionId={}, seatId={}, cap={}",
                request.getSessionId(), request.getSeatId(), capacity);
        return bucket.getBucketId();
    }

    private int nz(Integer v) {
        return v != null ? v : 0;
    }
}

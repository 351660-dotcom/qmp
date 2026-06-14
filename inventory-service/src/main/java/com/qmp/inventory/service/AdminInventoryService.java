package com.qmp.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.inventory.dto.admin.UpsertBucketRequest;
import com.qmp.inventory.entity.InventoryBucket;
import com.qmp.inventory.error.InventoryErrorCode;
import com.qmp.inventory.mapper.InventoryBucketMapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 库存中心后台管理服务：创建/调整库存桶。让后台维护库存配额，替代 Flyway 种子
 * （见 inventory-service CLAUDE.md「后台管理」）。
 *
 * <p>调整 {@code total_quota} 后会删除该桶的 Redis 余量 key（{@code inv:{bucketId}:remain}），
 * 使下一次预占按 DB 最新余量重新初始化第一道防线缓存，避免缓存与 DB 配额不一致。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInventoryService {

    private final InventoryBucketMapper bucketMapper;
    private final StringRedisTemplate redisTemplate;

    public Long upsertBucket(UpsertBucketRequest request) {
        long timeSlotId = request.getTimeSlotId() != null ? request.getTimeSlotId() : 0L;

        InventoryBucket existing = bucketMapper.selectOne(new LambdaQueryWrapper<InventoryBucket>()
                .eq(InventoryBucket::getSkuId, request.getSkuId())
                .eq(InventoryBucket::getSaleDate, request.getSaleDate())
                .eq(InventoryBucket::getTimeSlotId, timeSlotId));

        if (existing != null) {
            int used = nz(existing.getSoldCount()) + nz(existing.getLockedCount());
            if (request.getTotalQuota() < used) {
                throw new BizException(InventoryErrorCode.INVALID_QUOTA);
            }
            existing.setTotalQuota(request.getTotalQuota());
            if (request.getChannelQuota() != null) {
                existing.setChannelQuota(request.getChannelQuota().toString());
            }
            bucketMapper.updateById(existing);
            redisTemplate.delete(remainKey(existing.getBucketId()));
            log.info("后台调整库存桶: bucketId={}, totalQuota={}", existing.getBucketId(), request.getTotalQuota());
            return existing.getBucketId();
        }

        InventoryBucket bucket = new InventoryBucket();
        bucket.setTenantId(TenantContext.get());
        bucket.setScenicId(request.getScenicId());
        bucket.setSkuId(request.getSkuId());
        bucket.setSaleDate(request.getSaleDate());
        bucket.setTimeSlotId(timeSlotId);
        bucket.setTotalQuota(request.getTotalQuota());
        bucket.setSoldCount(0);
        bucket.setLockedCount(0);
        bucket.setChannelQuota(request.getChannelQuota() != null
                ? request.getChannelQuota().toString()
                : "{\"direct\": " + request.getTotalQuota() + "}");
        bucket.setVersion(0);
        bucketMapper.insert(bucket);
        log.info("后台创建库存桶: bucketId={}, sku={}, date={}, slot={}, quota={}",
                bucket.getBucketId(), request.getSkuId(), request.getSaleDate(), timeSlotId, request.getTotalQuota());
        return bucket.getBucketId();
    }

    public InventoryBucket getBucket(Long skuId, LocalDate saleDate, Long timeSlotId) {
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

    private int nz(Integer v) {
        return v != null ? v : 0;
    }

    private String remainKey(Long bucketId) {
        return "inv:" + bucketId + ":remain";
    }
}

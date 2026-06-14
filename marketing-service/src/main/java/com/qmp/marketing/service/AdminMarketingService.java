package com.qmp.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.marketing.dto.admin.CreatePromotionRuleRequest;
import com.qmp.marketing.dto.admin.CreateSeckillRequest;
import com.qmp.marketing.dto.admin.CreateTemplateRequest;
import com.qmp.marketing.dto.admin.UpsertSeckillBucketRequest;
import com.qmp.marketing.entity.CouponTemplate;
import com.qmp.marketing.entity.PromotionRule;
import com.qmp.marketing.entity.SeckillActivity;
import com.qmp.marketing.entity.SeckillInventoryBucket;
import com.qmp.marketing.error.MarketingErrorCode;
import com.qmp.marketing.mapper.CouponTemplateMapper;
import com.qmp.marketing.mapper.PromotionRuleMapper;
import com.qmp.marketing.mapper.SeckillActivityMapper;
import com.qmp.marketing.mapper.SeckillInventoryBucketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 营销后台管理：维护优惠券模板、营销规则、秒杀活动与秒杀库存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMarketingService {

    private final CouponTemplateMapper templateMapper;
    private final PromotionRuleMapper promotionRuleMapper;
    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillInventoryBucketMapper seckillBucketMapper;

    public Long createTemplate(CreateTemplateRequest request) {
        CouponTemplate template = new CouponTemplate();
        template.setTenantId(TenantContext.get());
        template.setName(request.getName());
        template.setCouponType(request.getCouponType());
        template.setFaceValue(request.getFaceValue());
        template.setDiscountRate(request.getDiscountRate());
        template.setApplicableScope(request.getApplicableScope() != null
                ? request.getApplicableScope().toString() : null);
        template.setValidPeriodRule(request.getValidPeriodRule() != null
                ? request.getValidPeriodRule().toString() : null);
        template.setIssueQuota(request.getIssueQuota());
        template.setStatus("ACTIVE");
        templateMapper.insert(template);
        log.info("后台创建券模板: templateId={}, name={}", template.getTemplateId(), request.getName());
        return template.getTemplateId();
    }

    public List<CouponTemplate> listTemplates() {
        return templateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .orderByDesc(CouponTemplate::getTemplateId));
    }

    // ---- 营销规则 ----
    public Long createPromotionRule(CreatePromotionRuleRequest request) {
        PromotionRule rule = new PromotionRule();
        rule.setTenantId(TenantContext.get());
        rule.setScope(request.getScope() != null ? request.getScope() : "SCENIC");
        rule.setScopeId(request.getScopeId());
        rule.setRuleType(request.getRuleType());
        rule.setConditions(request.getConditions() != null ? request.getConditions().toString() : null);
        rule.setActions(request.getActions() != null ? request.getActions().toString() : "{}");
        rule.setStackPolicy(request.getStackPolicy() != null ? request.getStackPolicy() : "EXCLUSIVE");
        rule.setStatus("ACTIVE");
        promotionRuleMapper.insert(rule);
        log.info("后台创建营销规则: ruleId={}, type={}", rule.getRuleId(), request.getRuleType());
        return rule.getRuleId();
    }

    public List<PromotionRule> listPromotionRules() {
        return promotionRuleMapper.selectList(new LambdaQueryWrapper<PromotionRule>()
                .orderByDesc(PromotionRule::getRuleId));
    }

    // ---- 秒杀活动 + 秒杀库存 ----
    public Long createSeckill(CreateSeckillRequest request) {
        SeckillActivity activity = new SeckillActivity();
        activity.setTenantId(TenantContext.get());
        activity.setSkuId(request.getSkuId());
        activity.setSeckillPrice(request.getSeckillPrice());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        seckillActivityMapper.insert(activity);
        log.info("后台创建秒杀活动: activityId={}, skuId={}", activity.getActivityId(), request.getSkuId());
        return activity.getActivityId();
    }

    public Long upsertSeckillBucket(UpsertSeckillBucketRequest request) {
        SeckillInventoryBucket existing = seckillBucketMapper.selectOne(
                new LambdaQueryWrapper<SeckillInventoryBucket>()
                        .eq(SeckillInventoryBucket::getActivityId, request.getActivityId()));
        if (existing != null) {
            int used = nz(existing.getSoldCount()) + nz(existing.getLockedCount());
            if (request.getTotalQuota() < used) {
                throw new BizException(MarketingErrorCode.INVALID_QUOTA);
            }
            existing.setTotalQuota(request.getTotalQuota());
            seckillBucketMapper.updateById(existing);
            return existing.getBucketId();
        }
        SeckillInventoryBucket bucket = new SeckillInventoryBucket();
        bucket.setTenantId(TenantContext.get());
        bucket.setActivityId(request.getActivityId());
        bucket.setSkuId(request.getSkuId());
        bucket.setSaleDate(LocalDate.now());
        bucket.setTimeSlotId(0L);
        bucket.setTotalQuota(request.getTotalQuota());
        bucket.setSoldCount(0);
        bucket.setLockedCount(0);
        bucket.setChannelQuota("{\"direct\": " + request.getTotalQuota() + "}");
        bucket.setVersion(0);
        seckillBucketMapper.insert(bucket);
        log.info("后台铺秒杀库存: activityId={}, quota={}", request.getActivityId(), request.getTotalQuota());
        return bucket.getBucketId();
    }

    private int nz(Integer v) {
        return v != null ? v : 0;
    }
}

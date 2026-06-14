package com.qmp.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.marketing.dto.CouponView;
import com.qmp.marketing.entity.CouponInstance;
import com.qmp.marketing.entity.CouponTemplate;
import com.qmp.marketing.error.MarketingErrorCode;
import com.qmp.marketing.mapper.CouponInstanceMapper;
import com.qmp.marketing.mapper.CouponTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券服务（13 文档四）：发券、查券、核销、回退。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponTemplateMapper templateMapper;
    private final CouponInstanceMapper instanceMapper;

    @Transactional
    public CouponView issue(Long templateId, Long userId) {
        CouponTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException(MarketingErrorCode.TEMPLATE_NOT_FOUND);
        }
        if (template.getIssueQuota() != null) {
            Long issued = instanceMapper.selectCount(new LambdaQueryWrapper<CouponInstance>()
                    .eq(CouponInstance::getTemplateId, templateId));
            if (issued != null && issued >= template.getIssueQuota()) {
                throw new BizException(MarketingErrorCode.ISSUE_QUOTA_EXCEEDED);
            }
        }
        CouponInstance coupon = new CouponInstance();
        coupon.setTenantId(TenantContext.get());
        coupon.setTemplateId(templateId);
        coupon.setUserId(userId);
        coupon.setStatus("UNUSED");
        coupon.setIssuedAt(LocalDateTime.now());
        instanceMapper.insert(coupon);
        log.info("发券: couponId={}, templateId={}, userId={}", coupon.getCouponId(), templateId, userId);
        return toView(coupon);
    }

    public List<CouponView> listUserCoupons(Long userId, String status) {
        LambdaQueryWrapper<CouponInstance> wrapper = new LambdaQueryWrapper<CouponInstance>()
                .eq(CouponInstance::getUserId, userId)
                .orderByDesc(CouponInstance::getCouponId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(CouponInstance::getStatus, status);
        }
        return instanceMapper.selectList(wrapper).stream().map(this::toView).toList();
    }

    /** 核销：UNUSED -> USED，关联订单（订单 PAID 后调用）。 */
    @Transactional
    public CouponView redeem(Long couponId, Long orderId) {
        CouponInstance coupon = getOrThrow(couponId);
        if (!"UNUSED".equals(coupon.getStatus())) {
            throw new BizException(MarketingErrorCode.COUPON_NOT_USABLE);
        }
        coupon.setStatus("USED");
        coupon.setOrderId(orderId);
        coupon.setUsedAt(LocalDateTime.now());
        instanceMapper.updateById(coupon);
        log.info("核销优惠券: couponId={}, orderId={}", couponId, orderId);
        return toView(coupon);
    }

    /** 回退：USED -> UNUSED（订单取消/退款且仍在有效期内）。幂等。 */
    @Transactional
    public CouponView revert(Long couponId) {
        CouponInstance coupon = getOrThrow(couponId);
        if ("USED".equals(coupon.getStatus())) {
            coupon.setStatus("UNUSED");
            coupon.setOrderId(null);
            coupon.setUsedAt(null);
            instanceMapper.updateById(coupon);
            log.info("回退优惠券: couponId={}", couponId);
        }
        return toView(coupon);
    }

    private CouponInstance getOrThrow(Long couponId) {
        CouponInstance coupon = instanceMapper.selectById(couponId);
        if (coupon == null) {
            throw new BizException(MarketingErrorCode.COUPON_NOT_FOUND);
        }
        return coupon;
    }

    private CouponView toView(CouponInstance c) {
        return CouponView.builder()
                .couponId(c.getCouponId())
                .templateId(c.getTemplateId())
                .userId(c.getUserId())
                .status(c.getStatus())
                .issuedAt(c.getIssuedAt())
                .usedAt(c.getUsedAt())
                .orderId(c.getOrderId())
                .build();
    }
}

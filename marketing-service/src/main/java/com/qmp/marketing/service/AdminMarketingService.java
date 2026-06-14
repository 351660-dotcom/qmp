package com.qmp.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.marketing.dto.admin.CreateTemplateRequest;
import com.qmp.marketing.entity.CouponTemplate;
import com.qmp.marketing.mapper.CouponTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 营销后台管理：维护优惠券模板。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMarketingService {

    private final CouponTemplateMapper templateMapper;

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
}

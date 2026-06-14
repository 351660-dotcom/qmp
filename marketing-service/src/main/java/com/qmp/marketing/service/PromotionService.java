package com.qmp.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.marketing.dto.PromotionCalcRequest;
import com.qmp.marketing.dto.PromotionCalcResponse;
import com.qmp.marketing.entity.PromotionRule;
import com.qmp.marketing.mapper.PromotionRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 营销规则引擎（13 文档 3.3）。v1 计算顺序：① DISCOUNT（乘法，取力度最大一条）→
 * ② FULL_REDUCTION（加法，取满足门槛且减额最大一条）。叠加策略 v1 简化为「每类取最优」（EXCLUSIVE）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRuleMapper ruleMapper;
    private final ObjectMapper objectMapper;

    public PromotionCalcResponse calculate(PromotionCalcRequest request) {
        BigDecimal original = BigDecimal.ZERO;
        if (request.getItems() != null) {
            for (PromotionCalcRequest.Item item : request.getItems()) {
                original = original.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        original = original.setScale(2, RoundingMode.HALF_UP);

        List<PromotionRule> active = ruleMapper.selectList(new LambdaQueryWrapper<PromotionRule>()
                .eq(PromotionRule::getStatus, "ACTIVE"));

        List<PromotionCalcResponse.Applied> applied = new ArrayList<>();
        BigDecimal running = original;

        // ① DISCOUNT：取折扣率最低（力度最大）一条
        PromotionRule bestDiscount = null;
        BigDecimal bestRate = null;
        for (PromotionRule rule : active) {
            if (!"DISCOUNT".equals(rule.getRuleType())) {
                continue;
            }
            BigDecimal rate = num(rule.getActions(), "discount_rate");
            if (rate != null && rate.signum() > 0 && (bestRate == null || rate.compareTo(bestRate) < 0)) {
                bestRate = rate;
                bestDiscount = rule;
            }
        }
        if (bestDiscount != null) {
            BigDecimal after = running.multiply(bestRate).setScale(2, RoundingMode.HALF_UP);
            applied.add(PromotionCalcResponse.Applied.builder()
                    .ruleId(bestDiscount.getRuleId()).ruleType("DISCOUNT")
                    .discountAmount(running.subtract(after)).build());
            running = after;
        }

        // ② FULL_REDUCTION：满足门槛、减额最大一条
        PromotionRule bestFr = null;
        BigDecimal bestReduce = null;
        for (PromotionRule rule : active) {
            if (!"FULL_REDUCTION".equals(rule.getRuleType())) {
                continue;
            }
            BigDecimal threshold = num(rule.getActions(), "threshold");
            BigDecimal reduce = num(rule.getActions(), "reduce");
            if (threshold != null && reduce != null && running.compareTo(threshold) >= 0
                    && (bestReduce == null || reduce.compareTo(bestReduce) > 0)) {
                bestReduce = reduce;
                bestFr = rule;
            }
        }
        if (bestFr != null) {
            BigDecimal after = running.subtract(bestReduce).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            applied.add(PromotionCalcResponse.Applied.builder()
                    .ruleId(bestFr.getRuleId()).ruleType("FULL_REDUCTION")
                    .discountAmount(running.subtract(after)).build());
            running = after;
        }

        return PromotionCalcResponse.builder()
                .originalAmount(original)
                .discountAmount(original.subtract(running))
                .payableAmount(running)
                .appliedRules(applied)
                .build();
    }

    private BigDecimal num(String actionsJson, String field) {
        if (actionsJson == null || actionsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(actionsJson).get(field);
            return node != null ? node.decimalValue() : null;
        } catch (Exception e) {
            log.warn("解析 promotion actions 失败: {}", actionsJson, e);
            return null;
        }
    }
}

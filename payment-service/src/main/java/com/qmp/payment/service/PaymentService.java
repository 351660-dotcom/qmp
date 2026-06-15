package com.qmp.payment.service;

import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.payment.dto.CreatePaymentRequest;
import com.qmp.payment.dto.CreateRefundRequest;
import com.qmp.payment.dto.MockCallbackRequest;
import com.qmp.payment.dto.PaymentResponse;
import com.qmp.payment.dto.RefundResponse;
import com.qmp.payment.entity.PaymentOrder;
import com.qmp.payment.entity.RefundRecord;
import com.qmp.payment.entity.MerchantCommission;
import com.qmp.payment.entity.SettlementRecord;
import com.qmp.payment.error.PaymentErrorCode;
import com.qmp.payment.event.PaymentSucceededPayload;
import com.qmp.payment.event.RefundSucceededPayload;
import com.qmp.payment.mapper.PaymentOrderMapper;
import com.qmp.payment.mapper.RefundRecordMapper;
import com.qmp.payment.mapper.MerchantCommissionMapper;
import com.qmp.payment.mapper.SettlementRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 支付分账核心服务（09 文档六）：创建支付单、关闭支付单、模拟渠道回调、发起退款。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String TOPIC_PAYMENT_SUCCEEDED = "payment_payment-succeeded";
    private static final String TOPIC_REFUND_SUCCEEDED = "payment_refund-succeeded";
    private static final DateTimeFormatter PAYMENT_ID_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PaymentOrderMapper paymentOrderMapper;
    private final SettlementRecordMapper settlementRecordMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final MerchantCommissionMapper merchantCommissionMapper;
    private final RocketMQTemplate rocketMQTemplate;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        PaymentOrder existing = findByOrderId(request.getOrderId());
        if (existing != null) {
            return toResponse(existing);
        }

        String paymentId = "PAY-" + LocalDateTime.now().format(PAYMENT_ID_DATE)
                + "-" + String.format("%06d", request.getOrderId());

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentId(paymentId);
        paymentOrder.setTenantId(request.getTenantId());
        paymentOrder.setMerchantId(request.getMerchantId());
        paymentOrder.setOrderId(request.getOrderId());
        paymentOrder.setAmount(request.getAmount());
        paymentOrder.setChannel(request.getChannel());
        paymentOrder.setStatus("CREATED");
        paymentOrderMapper.insert(paymentOrder);

        return toResponse(paymentOrder);
    }

    @Transactional
    public PaymentResponse closePayment(String paymentId) {
        PaymentOrder paymentOrder = getOrThrow(paymentId);

        if ("PAID".equals(paymentOrder.getStatus())) {
            throw new BizException(PaymentErrorCode.PAYMENT_ALREADY_PAID);
        }
        if (!"CLOSED".equals(paymentOrder.getStatus())) {
            paymentOrder.setStatus("CLOSED");
            paymentOrderMapper.updateById(paymentOrder);
        }
        return toResponse(paymentOrder);
    }

    /**
     * v1 模拟渠道回调（09 文档 6.1）：验签后更新 {@code PaymentOrder.status=PAID}，发布 {@code PaymentSucceeded} 事件。
     * 真实微信/支付宝回调签名校验留待渠道接入链路补齐。
     */
    @Transactional
    public PaymentResponse handleMockCallback(MockCallbackRequest request) {
        PaymentOrder paymentOrder = getOrThrow(request.getPaymentId());

        if ("PAID".equals(paymentOrder.getStatus())) {
            return toResponse(paymentOrder);
        }

        LocalDateTime now = LocalDateTime.now();
        paymentOrder.setStatus("PAID");
        paymentOrder.setChannelTradeNo(request.getChannelTradeNo());
        paymentOrder.setPaidAt(now);
        paymentOrderMapper.updateById(paymentOrder);

        createSettlementRecord(paymentOrder);
        publishPaymentSucceeded(paymentOrder, now);

        return toResponse(paymentOrder);
    }

    /**
     * 发起退款（09 文档六）。v1 无真实渠道，退款在本次调用内同步完成（mock），
     * 立即置为 SUCCEEDED 并发布 {@code RefundSucceeded} 事件——见本服务 CLAUDE.md「v1 退款同步完成」说明。
     */
    @Transactional
    public RefundResponse createRefund(String paymentId, CreateRefundRequest request) {
        PaymentOrder paymentOrder = getOrThrow(paymentId);

        RefundRecord existing = refundRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RefundRecord>()
                        .eq(RefundRecord::getCredentialId, request.getCredentialId()));
        if (existing != null) {
            return RefundResponse.builder()
                    .refundId(existing.getRefundId())
                    .status(existing.getStatus())
                    .build();
        }

        RefundRecord refund = new RefundRecord();
        refund.setTenantId(paymentOrder.getTenantId());
        refund.setPaymentId(paymentId);
        refund.setCredentialId(request.getCredentialId());
        refund.setAmount(request.getAmount());
        refund.setStatus("SUCCEEDED");
        refund.setRefundChannelNo("MOCK-" + request.getCredentialId());
        refundRecordMapper.insert(refund);

        publishRefundSucceeded(paymentOrder, refund);

        return RefundResponse.builder()
                .refundId(refund.getRefundId())
                .status(refund.getStatus())
                .build();
    }

    /**
     * 分账：按商户配置的抽成比例（{@code merchant_commission}）计算平台/商户金额。
     * platform_amount = amount × rate（四舍五入 2 位），merchant_amount = amount − platform_amount；
     * 商户未配置比例时按 0 处理（全额归商户），与历史占位行为兼容。状态置 SETTLED（v1 mock 即时清算）。
     */
    private void createSettlementRecord(PaymentOrder paymentOrder) {
        BigDecimal rate = commissionRate(paymentOrder.getMerchantId());
        BigDecimal platformAmount = paymentOrder.getAmount()
                .multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal merchantAmount = paymentOrder.getAmount().subtract(platformAmount);

        SettlementRecord settlement = new SettlementRecord();
        settlement.setTenantId(paymentOrder.getTenantId());
        settlement.setPaymentId(paymentOrder.getPaymentId());
        settlement.setMerchantId(paymentOrder.getMerchantId());
        settlement.setPlatformAmount(platformAmount);
        settlement.setMerchantAmount(merchantAmount);
        settlement.setStatus("SETTLED");
        settlementRecordMapper.insert(settlement);
    }

    /** 取商户抽成比例；未配置返回 0。 */
    private BigDecimal commissionRate(Long merchantId) {
        MerchantCommission cfg = merchantCommissionMapper.selectById(merchantId);
        return (cfg != null && cfg.getCommissionRate() != null) ? cfg.getCommissionRate() : BigDecimal.ZERO;
    }

    /** 后台 upsert 商户抽成比例（0..1）。 */
    @Transactional
    public void upsertCommission(Long merchantId, BigDecimal rate) {
        MerchantCommission existing = merchantCommissionMapper.selectById(merchantId);
        if (existing == null) {
            MerchantCommission cfg = new MerchantCommission();
            cfg.setMerchantId(merchantId);
            cfg.setTenantId(TenantContext.get());
            cfg.setCommissionRate(rate);
            merchantCommissionMapper.insert(cfg);
        } else {
            existing.setCommissionRate(rate);
            merchantCommissionMapper.updateById(existing);
        }
    }

    private void publishPaymentSucceeded(PaymentOrder paymentOrder, LocalDateTime paidAt) {
        PaymentSucceededPayload payload = PaymentSucceededPayload.builder()
                .orderId(paymentOrder.getOrderId())
                .paymentId(paymentOrder.getPaymentId())
                .merchantId(paymentOrder.getMerchantId())
                .amount(paymentOrder.getAmount())
                .channel(paymentOrder.getChannel())
                .paidAt(paidAt.atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
        EventEnvelope<PaymentSucceededPayload> event =
                EventEnvelope.of("PaymentSucceeded", paymentOrder.getTenantId(), payload);
        // 分区 key 取 order_id，保证同一订单的事件顺序消费（见 09 文档 1.3）
        rocketMQTemplate.syncSendOrderly(TOPIC_PAYMENT_SUCCEEDED, event, String.valueOf(paymentOrder.getOrderId()));
        log.info("发布 PaymentSucceeded: orderId={}, paymentId={}", paymentOrder.getOrderId(), paymentOrder.getPaymentId());
    }

    private void publishRefundSucceeded(PaymentOrder paymentOrder, RefundRecord refund) {
        RefundSucceededPayload payload = RefundSucceededPayload.builder()
                .orderId(paymentOrder.getOrderId())
                .paymentId(paymentOrder.getPaymentId())
                .merchantId(paymentOrder.getMerchantId())
                .refundId(refund.getRefundId())
                .credentialId(refund.getCredentialId())
                .amount(refund.getAmount())
                .build();
        EventEnvelope<RefundSucceededPayload> event =
                EventEnvelope.of("RefundSucceeded", paymentOrder.getTenantId(), payload);
        rocketMQTemplate.syncSendOrderly(TOPIC_REFUND_SUCCEEDED, event, String.valueOf(paymentOrder.getOrderId()));
        log.info("发布 RefundSucceeded: orderId={}, refundId={}", paymentOrder.getOrderId(), refund.getRefundId());
    }

    private PaymentOrder findByOrderId(Long orderId) {
        return paymentOrderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getOrderId, orderId));
    }

    private PaymentOrder getOrThrow(String paymentId) {
        PaymentOrder paymentOrder = paymentOrderMapper.selectById(paymentId);
        if (paymentOrder == null) {
            throw new BizException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
        return paymentOrder;
    }

    private PaymentResponse toResponse(PaymentOrder paymentOrder) {
        Map<String, Object> payParams = "CREATED".equals(paymentOrder.getStatus())
                ? Map.of("prepay_id", "mock-prepay-" + paymentOrder.getPaymentId())
                : null;
        return PaymentResponse.builder()
                .paymentId(paymentOrder.getPaymentId())
                .status(paymentOrder.getStatus())
                .payParams(payParams)
                .build();
    }
}

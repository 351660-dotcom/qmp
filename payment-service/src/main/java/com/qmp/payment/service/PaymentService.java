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
import com.qmp.payment.entity.SettlementRecord;
import com.qmp.payment.error.PaymentErrorCode;
import com.qmp.payment.event.PaymentSucceededPayload;
import com.qmp.payment.event.RefundSucceededPayload;
import com.qmp.payment.mapper.PaymentOrderMapper;
import com.qmp.payment.mapper.RefundRecordMapper;
import com.qmp.payment.mapper.SettlementRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    private static final String TOPIC_PAYMENT_SUCCEEDED = "payment.payment-succeeded";
    private static final String TOPIC_REFUND_SUCCEEDED = "payment.refund-succeeded";
    private static final DateTimeFormatter PAYMENT_ID_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PaymentOrderMapper paymentOrderMapper;
    private final SettlementRecordMapper settlementRecordMapper;
    private final RefundRecordMapper refundRecordMapper;
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
     * v1 占位分账：平台抽成比例未在 08/13 文档中定义，先记 100% 归商户、平台分账金额为 0，
     * 状态置为 SETTLED（mock）。后续接入真实分账规则时按 {@code merchant_amount}/{@code platform_amount} 重算。
     */
    private void createSettlementRecord(PaymentOrder paymentOrder) {
        SettlementRecord settlement = new SettlementRecord();
        settlement.setTenantId(paymentOrder.getTenantId());
        settlement.setPaymentId(paymentOrder.getPaymentId());
        settlement.setMerchantId(paymentOrder.getMerchantId());
        settlement.setPlatformAmount(BigDecimal.ZERO);
        settlement.setMerchantAmount(paymentOrder.getAmount());
        settlement.setStatus("SETTLED");
        settlementRecordMapper.insert(settlement);
    }

    private void publishPaymentSucceeded(PaymentOrder paymentOrder, LocalDateTime paidAt) {
        PaymentSucceededPayload payload = PaymentSucceededPayload.builder()
                .orderId(paymentOrder.getOrderId())
                .paymentId(paymentOrder.getPaymentId())
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

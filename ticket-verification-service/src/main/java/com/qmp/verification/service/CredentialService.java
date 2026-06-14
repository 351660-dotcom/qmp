package com.qmp.verification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.verification.client.PaymentClient;
import com.qmp.verification.dto.CredentialDto;
import com.qmp.verification.dto.IssueCredentialsRequest;
import com.qmp.verification.dto.IssueCredentialsResponse;
import com.qmp.verification.dto.RefundRequestResponse;
import com.qmp.verification.dto.VerifyRequest;
import com.qmp.verification.dto.VerifyResponse;
import com.qmp.verification.entity.TicketCredential;
import com.qmp.verification.entity.VerificationRecord;
import com.qmp.verification.error.VerificationErrorCode;
import com.qmp.verification.event.TicketVerifiedPayload;
import com.qmp.verification.mapper.TicketCredentialMapper;
import com.qmp.verification.mapper.VerificationRecordMapper;
import com.qmp.verification.util.VerifyCodeSigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 核验中心核心服务（09 文档七）：出票、核验、申请退票。
 *
 * <p>状态机（07 文档 2.4）：{@code UNUSED -> VERIFIED}（核销，终态）/
 * {@code UNUSED -> REFUNDING -> REFUNDED}（退票，REFUNDED 为终态）/ {@code UNUSED -> EXPIRED}。
 * 退票时本服务同步调用 payment-service 创建退款；库存释放由 {@code RefundSucceeded} 事件消费侧完成
 * （见 {@code RefundSucceededConsumer}）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialService {

    private static final String TOPIC_TICKET_VERIFIED = "ticket-verification_ticket-verified";

    private final TicketCredentialMapper credentialMapper;
    private final VerificationRecordMapper verificationRecordMapper;
    private final VerifyCodeSigner verifyCodeSigner;
    private final ObjectMapper objectMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final PaymentClient paymentClient;

    // ------------------------------------------------------------------
    // 出票（幂等键 = order_item_id）
    // ------------------------------------------------------------------
    @Transactional
    public IssueCredentialsResponse issueCredentials(IssueCredentialsRequest request) {
        List<TicketCredential> existing = credentialMapper.selectList(
                new LambdaQueryWrapper<TicketCredential>()
                        .eq(TicketCredential::getOrderItemId, request.getOrderItemId())
                        .orderByAsc(TicketCredential::getTicketIndex));
        if (!existing.isEmpty()) {
            return toResponse(existing);
        }

        String policyJson = request.getRefundPolicySnapshot() != null
                ? request.getRefundPolicySnapshot().toString() : null;

        for (int index = 1; index <= request.getQuantity(); index++) {
            long credentialId = IdWorker.getId();

            TicketCredential credential = new TicketCredential();
            credential.setCredentialId(credentialId);
            credential.setTenantId(TenantContext.get());
            credential.setOrderId(request.getOrderId());
            credential.setOrderItemId(request.getOrderItemId());
            credential.setTicketIndex(index);
            credential.setStatus("UNUSED");
            credential.setScenicId(request.getScenicId());
            credential.setSaleDate(request.getSaleDate());
            credential.setUnitPrice(request.getUnitPrice());
            credential.setPaymentId(request.getPaymentId());
            credential.setRefundPolicySnapshot(policyJson);
            // verify_code 内嵌 credential_id，故先生成 ID 再签名，单次插入（见 CLAUDE.md）
            credential.setVerifyCode(verifyCodeSigner.sign(new VerifyCodeSigner.Payload(
                    credentialId,
                    request.getOrderItemId(),
                    request.getSkuId() != null ? request.getSkuId() : 0L,
                    request.getSaleDate() != null ? request.getSaleDate().toString() : null)));

            credentialMapper.insert(credential);
            existing.add(credential);
        }
        log.info("出票成功: orderItemId={}, quantity={}", request.getOrderItemId(), request.getQuantity());
        return toResponse(existing);
    }

    // ------------------------------------------------------------------
    // 核验（UNUSED -> VERIFIED）
    // ------------------------------------------------------------------
    @Transactional
    public VerifyResponse verify(VerifyRequest request) {
        VerifyCodeSigner.Payload payload = verifyCodeSigner.verify(request.getVerifyCode());

        TicketCredential credential = credentialMapper.selectById(payload.cid());
        if (credential == null) {
            throw new BizException(VerificationErrorCode.CREDENTIAL_NOT_FOUND);
        }
        // 防御：签名合法但与库内凭证码不一致（理论不会发生）
        if (!request.getVerifyCode().equals(credential.getVerifyCode())) {
            throw new BizException(VerificationErrorCode.INVALID_SIGNATURE);
        }

        switch (credential.getStatus()) {
            case "VERIFIED" -> {
                recordVerification(credential, request.getTerminalId(), "ALREADY_VERIFIED");
                throw new BizException(VerificationErrorCode.ALREADY_VERIFIED);
            }
            case "EXPIRED" -> {
                recordVerification(credential, request.getTerminalId(), "EXPIRED");
                throw new BizException(VerificationErrorCode.EXPIRED);
            }
            case "UNUSED" -> {
                // 正常核销
            }
            default -> {
                // REFUNDING / REFUNDED / VOIDED
                recordVerification(credential, request.getTerminalId(), "NOT_VERIFIABLE");
                throw new BizException(VerificationErrorCode.NOT_VERIFIABLE);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        credential.setStatus("VERIFIED");
        credential.setVerifiedAt(now);
        credential.setVerifyTerminalId(request.getTerminalId());
        credentialMapper.updateById(credential);

        recordVerification(credential, request.getTerminalId(), "SUCCESS");
        publishTicketVerified(credential, now);

        log.info("核销成功: credentialId={}, terminalId={}", credential.getCredentialId(), request.getTerminalId());
        return VerifyResponse.builder()
                .result("SUCCESS")
                .credentialId(credential.getCredentialId())
                .build();
    }

    // ------------------------------------------------------------------
    // 申请退票（UNUSED -> REFUNDING，同步调用 payment-service）
    // ------------------------------------------------------------------
    public RefundRequestResponse requestRefund(Long credentialId) {
        TicketCredential credential = credentialMapper.selectById(credentialId);
        if (credential == null) {
            throw new BizException(VerificationErrorCode.CREDENTIAL_NOT_FOUND);
        }

        switch (credential.getStatus()) {
            case "UNUSED" -> {
                if (!isRefundWindowOpen(credential)) {
                    throw new BizException(VerificationErrorCode.REFUND_NOT_ALLOWED);
                }
                // 先落库 REFUNDING 并提交（无 @Transactional，逐语句自动提交），
                // 再调 payment-service，避免 RefundSucceeded 消费侧读到旧状态
                credential.setStatus("REFUNDING");
                credentialMapper.updateById(credential);
            }
            case "REFUNDING", "REFUNDED" -> {
                // 幂等：重复申请，下方再次调用 payment（幂等键 credential_id）取回同一 refund_id
            }
            default -> {
                // VERIFIED / EXPIRED / VOIDED
                throw new BizException(VerificationErrorCode.REFUND_NOT_ALLOWED);
            }
        }

        BigDecimal amount = computeRefundAmount(credential);
        Long refundId = paymentClient.createRefund(
                credential.getTenantId(), credential.getPaymentId(), credentialId, amount);

        log.info("申请退票成功: credentialId={}, amount={}, refundId={}", credentialId, amount, refundId);
        return RefundRequestResponse.builder().refundId(refundId).build();
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /**
     * 可退窗口判断：{@code refund_policy_snapshot.type=NONE} 不可退；
     * 否则若设置了 {@code cutoff_hours}，要求当前时间早于「游玩日 0 点 - cutoff_hours」。
     */
    private boolean isRefundWindowOpen(TicketCredential credential) {
        JsonNode policy = readPolicy(credential.getRefundPolicySnapshot());
        if (policy == null) {
            return true;
        }
        JsonNode typeNode = policy.get("type");
        if (typeNode != null && "NONE".equalsIgnoreCase(typeNode.asText())) {
            return false;
        }
        JsonNode cutoffNode = policy.get("cutoff_hours");
        if (cutoffNode == null || credential.getSaleDate() == null) {
            return true;
        }
        LocalDate saleDate = credential.getSaleDate();
        LocalDateTime cutoff = saleDate.atStartOfDay().minusHours(cutoffNode.asLong());
        return LocalDateTime.now().isBefore(cutoff);
    }

    /** 退款金额 = 单票价 × refund_ratio（默认 1.0），保留 2 位小数。 */
    private BigDecimal computeRefundAmount(TicketCredential credential) {
        BigDecimal unitPrice = credential.getUnitPrice() != null ? credential.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal ratio = BigDecimal.ONE;
        JsonNode policy = readPolicy(credential.getRefundPolicySnapshot());
        if (policy != null && policy.has("refund_ratio")) {
            ratio = policy.get("refund_ratio").decimalValue();
        }
        return unitPrice.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }

    private JsonNode readPolicy(String policyJson) {
        if (policyJson == null || policyJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(policyJson);
        } catch (Exception e) {
            log.warn("退改规则快照解析失败，按无规则处理: {}", policyJson, e);
            return null;
        }
    }

    private void recordVerification(TicketCredential credential, String terminalId, String result) {
        VerificationRecord record = new VerificationRecord();
        record.setTenantId(credential.getTenantId());
        record.setCredentialId(credential.getCredentialId());
        record.setScenicId(credential.getScenicId());
        record.setTerminalId(terminalId);
        record.setResult(result);
        record.setVerifiedAt(LocalDateTime.now());
        verificationRecordMapper.insert(record);
    }

    private void publishTicketVerified(TicketCredential credential, LocalDateTime verifiedAt) {
        TicketVerifiedPayload payload = TicketVerifiedPayload.builder()
                .orderId(credential.getOrderId())
                .orderItemId(credential.getOrderItemId())
                .credentialId(credential.getCredentialId())
                .verifiedAt(verifiedAt.atZone(ZoneId.systemDefault()).toInstant())
                .build();
        EventEnvelope<TicketVerifiedPayload> event =
                EventEnvelope.of("TicketVerified", credential.getTenantId(), payload);
        // 分区 key 取 order_id，保证同一订单的事件顺序消费（09 文档 1.3）
        rocketMQTemplate.syncSendOrderly(TOPIC_TICKET_VERIFIED, event, String.valueOf(credential.getOrderId()));
    }

    private IssueCredentialsResponse toResponse(List<TicketCredential> credentials) {
        List<CredentialDto> dtos = credentials.stream()
                .map(c -> CredentialDto.builder()
                        .credentialId(c.getCredentialId())
                        .ticketIndex(c.getTicketIndex())
                        .verifyCode(c.getVerifyCode())
                        .status(c.getStatus())
                        .build())
                .toList();
        return IssueCredentialsResponse.builder().credentials(dtos).build();
    }

    /**
     * 由 {@code RefundSucceeded} 消费侧调用：将凭证置为终态 REFUNDED（幂等）。
     * 与库存释放分离，便于消费侧按「先释放库存、后置终态、最后写去重」顺序保证可重试。
     */
    @Transactional
    public void markRefunded(Long credentialId) {
        TicketCredential credential = credentialMapper.selectById(credentialId);
        if (credential == null) {
            log.warn("markRefunded: 凭证不存在 credentialId={}", credentialId);
            return;
        }
        if ("REFUNDED".equals(credential.getStatus())) {
            return;
        }
        credential.setStatus("REFUNDED");
        credentialMapper.updateById(credential);
    }

    /** 由消费侧定位凭证对应的 order_item_id（= inventory reservation_id）。 */
    public TicketCredential findById(Long credentialId) {
        return credentialMapper.selectById(credentialId);
    }

    // ------------------------------------------------------------------
    // 凭证过期（UNUSED -> EXPIRED）：游玩日已过仍未核销的票按 no-show 作废
    // ------------------------------------------------------------------
    /** 扫描当前租户下游玩日已过（sale_date < today）且仍 UNUSED 的凭证（供 ExpireCredentialJob 调用）。 */
    public List<TicketCredential> findExpirableCredentials(LocalDate today) {
        return credentialMapper.selectList(new LambdaQueryWrapper<TicketCredential>()
                .eq(TicketCredential::getStatus, "UNUSED")
                .lt(TicketCredential::getSaleDate, today));
    }

    /**
     * 将一张过期未核销的凭证置为 EXPIRED（no-show：不退款、不释放库存——票款已确认收入）。
     * 用条件更新 {@code UNUSED -> EXPIRED} 保证与并发核验/退票互斥与幂等（命中 0 行即跳过）。
     */
    @Transactional
    public boolean expireCredential(Long credentialId) {
        int updated = credentialMapper.update(null, new LambdaUpdateWrapper<TicketCredential>()
                .eq(TicketCredential::getCredentialId, credentialId)
                .eq(TicketCredential::getStatus, "UNUSED")
                .set(TicketCredential::getStatus, "EXPIRED"));
        if (updated > 0) {
            log.info("凭证过期作废: credentialId={}", credentialId);
        }
        return updated > 0;
    }
}

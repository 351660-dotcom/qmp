package com.qmp.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.order.client.InventoryClient;
import com.qmp.order.client.MemberClient;
import com.qmp.order.client.PaymentClient;
import com.qmp.order.client.PricingClient;
import com.qmp.order.client.ProductClient;
import com.qmp.order.client.TicketVerificationClient;
import com.qmp.order.dto.CreateOrderRequest;
import com.qmp.order.dto.CreateOrderResponse;
import com.qmp.order.dto.OrderDetailResponse;
import com.qmp.order.dto.PayResponse;
import com.qmp.order.entity.OrderItem;
import com.qmp.order.entity.TradeOrder;
import com.qmp.order.error.OrderErrorCode;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.order.event.OrderPaidPayload;
import com.qmp.order.event.PaymentSucceededPayload;
import com.qmp.order.event.TicketVerifiedPayload;
import com.qmp.order.mapper.OrderItemMapper;
import com.qmp.order.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单编排核心服务（09 文档八）。创建订单采用「模块化单体伪分布式事务」——
 * 多 item 预占不在同一本地事务内，靠显式补偿保证一致性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    /** v1 占位退改规则：无独立策略服务，统一用 09 文档示例值，出票时透传给凭证。见 CLAUDE.md。 */
    private static final String DEFAULT_REFUND_POLICY =
            "{\"type\":\"TIERED\",\"cutoff_hours\":24,\"refund_ratio\":0.8}";
    private static final int PAY_EXPIRE_MINUTES = 15;
    private static final String TOPIC_ORDER_PAID = "order_order-paid";

    private final TradeOrderMapper tradeOrderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductClient productClient;
    private final PricingClient pricingClient;
    private final MemberClient memberClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final TicketVerificationClient ticketVerificationClient;
    private final RocketMQTemplate rocketMQTemplate;

    // ------------------------------------------------------------------
    // 创建订单（编排 + 显式补偿，09 文档 8.1）
    // ------------------------------------------------------------------
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        Long tenantId = TenantContext.get();
        boolean isMember = memberClient.isMember(request.getUserId());

        long orderId = IdWorker.getId();
        List<OrderItem> items = new ArrayList<>();
        List<String> reserved = new ArrayList<>();
        Long scenicId = null;
        Long merchantId = null;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderRequest.Item reqItem : request.getItems()) {
            ProductClient.SkuView sku = productClient.getSku(reqItem.getSkuId());
            if (!"ON_SALE".equals(sku.getStatus())) {
                compensate(reserved);
                throw new BizException(OrderErrorCode.SKU_NOT_ON_SALE);
            }
            // v1 单商户购物车：以最后一个 item 的 scenic/merchant 落单（黄金路径同景区同商户）
            scenicId = sku.getScenicId();
            merchantId = sku.getMerchantId();

            PricingClient.PriceView price = pricingClient.getPrice(reqItem.getSkuId(), reqItem.getSaleDate(), isMember);

            String orderItemId = "OI-" + IdWorker.getId();
            long timeSlotId = reqItem.getTimeSlotId() != null ? reqItem.getTimeSlotId() : 0L;
            try {
                inventoryClient.createReservation(orderItemId, reqItem.getSkuId(),
                        reqItem.getSaleDate(), timeSlotId, reqItem.getQuantity());
                reserved.add(orderItemId);
            } catch (HttpStatusCodeException e) {
                compensate(reserved);
                if (e.getStatusCode().value() == 409) {
                    throw new BizException(OrderErrorCode.INSUFFICIENT_STOCK);
                }
                log.error("库存预占调用失败: orderItemId={}, status={}, body={}",
                        orderItemId, e.getStatusCode(), e.getResponseBodyAsString());
                throw new BizException(OrderErrorCode.UPSTREAM_ERROR);
            }

            BigDecimal unitPrice = price.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(reqItem.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = new OrderItem();
            item.setOrderItemId(orderItemId);
            item.setTenantId(tenantId);
            item.setOrderId(orderId);
            item.setSkuId(reqItem.getSkuId());
            item.setSaleDate(reqItem.getSaleDate());
            item.setTimeSlotId(timeSlotId);
            item.setQuantity(reqItem.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            item.setRefundPolicySnapshot(DEFAULT_REFUND_POLICY);
            item.setVerifiedCount(0);
            items.add(item);
        }

        LocalDateTime payExpireAt = LocalDateTime.now().plusMinutes(PAY_EXPIRE_MINUTES);
        TradeOrder order = new TradeOrder();
        order.setOrderId(orderId);
        order.setTenantId(tenantId);
        order.setScenicId(scenicId);
        order.setMerchantId(merchantId);
        order.setUserId(request.getUserId());
        order.setStatus("PENDING_PAYMENT");
        order.setTotalAmount(totalAmount);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRefundAmount(BigDecimal.ZERO);
        order.setPayExpireAt(payExpireAt);
        order.setVersion(0);
        persist(order, items);

        log.info("创建订单成功: orderId={}, items={}, total={}", orderId, items.size(), totalAmount);
        return toCreateResponse(order, items, payExpireAt);
    }

    @Transactional
    public void persist(TradeOrder order, List<OrderItem> items) {
        tradeOrderMapper.insert(order);
        for (OrderItem item : items) {
            orderItemMapper.insert(item);
        }
    }

    /** 补偿：释放已成功预占的前序 item（best-effort）。 */
    private void compensate(List<String> reserved) {
        for (String reservationId : reserved) {
            try {
                inventoryClient.releaseReservation(reservationId);
            } catch (Exception e) {
                log.error("补偿释放预占失败（需人工/对账介入）: reservationId={}", reservationId, e);
            }
        }
    }

    // ------------------------------------------------------------------
    // 发起支付（09 文档八）
    // ------------------------------------------------------------------
    public PayResponse pay(Long orderId, String channel) {
        TradeOrder order = getOrThrow(orderId);
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BizException(OrderErrorCode.ORDER_INVALID_STATE);
        }
        // 超时关单任务可能尚未扫到，但已过支付截止时间的订单不允许再发起支付（与关单任务最终一致）
        if (order.getPayExpireAt() != null && order.getPayExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(OrderErrorCode.ORDER_PAY_EXPIRED);
        }
        PaymentClient.PaymentView payment = paymentClient.createPayment(
                orderId, order.getMerchantId(), order.getTotalAmount(), channel);

        order.setPaymentId(payment.getPaymentId());
        tradeOrderMapper.updateById(order);

        return PayResponse.builder()
                .paymentId(payment.getPaymentId())
                .payParams(payment.getPayParams())
                .build();
    }

    // ------------------------------------------------------------------
    // 查询订单详情（09 文档八）
    // ------------------------------------------------------------------
    public OrderDetailResponse getOrder(Long orderId) {
        TradeOrder order = getOrThrow(orderId);
        List<OrderItem> items = listItems(orderId);
        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(items.stream().map(it -> OrderDetailResponse.Item.builder()
                        .orderItemId(it.getOrderItemId())
                        .skuId(it.getSkuId())
                        .quantity(it.getQuantity())
                        .verifiedCount(it.getVerifiedCount() != null ? it.getVerifiedCount() : 0)
                        .build()).toList())
                .build();
    }

    // ------------------------------------------------------------------
    // 消费 PaymentSucceeded（09 文档 8.2）：逐 item 确认预占 + 出票，订单置 PAID
    // ------------------------------------------------------------------
    @Transactional
    public void handlePaymentSucceeded(PaymentSucceededPayload payload) {
        TradeOrder order = tradeOrderMapper.selectById(payload.getOrderId());
        if (order == null) {
            log.warn("PaymentSucceeded: 订单不存在 orderId={}", payload.getOrderId());
            return;
        }
        List<OrderItem> items = listItems(order.getOrderId());
        for (OrderItem item : items) {
            inventoryClient.confirmReservation(item.getOrderItemId());
            ticketVerificationClient.issueCredentials(item, order.getOrderId(), order.getScenicId(), payload.getPaymentId());
        }
        if (!"PAID".equals(order.getStatus())) {
            order.setStatus("PAID");
            order.setPaidAmount(payload.getAmount());
            order.setPaymentId(payload.getPaymentId());
            tradeOrderMapper.updateById(order);
            publishOrderPaid(order);
        }
        log.info("PaymentSucceeded 处理完成: orderId={}", order.getOrderId());
    }

    /** 发布 OrderPaid（13 文档 2.4/3.4），供 member-service 积分入账、marketing-service 营销履约消费。 */
    private void publishOrderPaid(TradeOrder order) {
        OrderPaidPayload payload = OrderPaidPayload.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .merchantId(order.getMerchantId())
                .totalAmount(order.getTotalAmount())
                .build();
        EventEnvelope<OrderPaidPayload> event = EventEnvelope.of("OrderPaid", order.getTenantId(), payload);
        rocketMQTemplate.syncSendOrderly(TOPIC_ORDER_PAID, event, String.valueOf(order.getOrderId()));
        log.info("发布 OrderPaid: orderId={}, userId={}, amount={}",
                order.getOrderId(), order.getUserId(), order.getTotalAmount());
    }

    // ------------------------------------------------------------------
    // 消费 TicketVerified（09 文档 8.2）：凭证聚合，全部核销则订单 CLOSED
    // ------------------------------------------------------------------
    @Transactional
    public void handleTicketVerified(TicketVerifiedPayload payload) {
        OrderItem item = orderItemMapper.selectById(payload.getOrderItemId());
        if (item == null) {
            log.warn("TicketVerified: 订单明细不存在 orderItemId={}", payload.getOrderItemId());
            return;
        }
        int verified = (item.getVerifiedCount() != null ? item.getVerifiedCount() : 0) + 1;
        item.setVerifiedCount(verified);
        orderItemMapper.updateById(item);

        TradeOrder order = tradeOrderMapper.selectById(payload.getOrderId());
        if (order == null || "CLOSED".equals(order.getStatus())) {
            return;
        }
        List<OrderItem> items = listItems(order.getOrderId());
        boolean allVerified = items.stream()
                .allMatch(it -> (it.getVerifiedCount() != null ? it.getVerifiedCount() : 0) >= it.getQuantity());
        if (allVerified && "PAID".equals(order.getStatus())) {
            order.setStatus("CLOSED");
            tradeOrderMapper.updateById(order);
            log.info("订单全部核销，置为 CLOSED: orderId={}", order.getOrderId());
        }
    }

    // ------------------------------------------------------------------
    // 超时关单（PENDING_PAYMENT 且超过支付截止时间）：释放预占 + 置 CANCELLED
    // ------------------------------------------------------------------
    /** 扫描当前租户下已过支付截止时间且仍待支付的订单（供 CancelExpiredOrderJob 调用）。 */
    public List<TradeOrder> findExpiredPendingOrders(LocalDateTime now) {
        return tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getStatus, "PENDING_PAYMENT")
                .lt(TradeOrder::getPayExpireAt, now));
    }

    /**
     * 关闭一笔超时未支付的订单：best-effort 释放各明细预占（幂等，库存侧可能已自行 EXPIRED），
     * 再将订单置 CANCELLED。重读校验状态 + 乐观锁（@Version）防止与支付成功并发误关。
     */
    @Transactional
    public void cancelExpiredOrder(Long orderId) {
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null || !"PENDING_PAYMENT".equals(order.getStatus())) {
            return; // 已被支付/已关闭，幂等跳过
        }
        for (OrderItem item : listItems(orderId)) {
            try {
                inventoryClient.releaseReservation(item.getOrderItemId());
            } catch (Exception e) {
                log.error("超时关单释放预占失败（留待库存超时任务/对账兜底）: orderItemId={}", item.getOrderItemId(), e);
            }
        }
        order.setStatus("CANCELLED");
        tradeOrderMapper.updateById(order);
        log.info("订单超时未支付，已关闭: orderId={}", orderId);
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------
    private TradeOrder getOrThrow(Long orderId) {
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private List<OrderItem> listItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getOrderItemId));
    }

    private CreateOrderResponse toCreateResponse(TradeOrder order, List<OrderItem> items, LocalDateTime payExpireAt) {
        return CreateOrderResponse.builder()
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .payExpireAt(payExpireAt.atZone(ZoneId.systemDefault()).toInstant())
                .items(items.stream().map(it -> CreateOrderResponse.Item.builder()
                        .orderItemId(it.getOrderItemId())
                        .skuId(it.getSkuId())
                        .quantity(it.getQuantity())
                        .unitPrice(it.getUnitPrice())
                        .build()).toList())
                .build();
    }
}

package com.qmp.dining.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.dining.client.MemberClient;
import com.qmp.dining.dto.AddLineRequest;
import com.qmp.dining.dto.CheckoutRequest;
import com.qmp.dining.dto.CheckoutResponse;
import com.qmp.dining.dto.OpenTableRequest;
import com.qmp.dining.dto.TableOrderView;
import com.qmp.dining.entity.DiningTable;
import com.qmp.dining.entity.OrderLine;
import com.qmp.dining.entity.TableOrder;
import com.qmp.dining.error.DiningErrorCode;
import com.qmp.dining.event.DiningCheckedPayload;
import com.qmp.dining.mapper.OrderLineMapper;
import com.qmp.dining.mapper.TableOrderMapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 台账与点单服务（12 文档一/二/四）：开台、加菜、KDS 状态流转、退菜、结账。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableOrderService {

    private static final String TOPIC_DINING_CHECKED = "dining_dining-checked";
    private static final Set<String> CHECKOUT_TERMINAL = Set.of("SERVED", "CANCELLED", "RETURNED");

    private final TableOrderMapper tableOrderMapper;
    private final OrderLineMapper orderLineMapper;
    private final TableService tableService;
    private final DishAvailabilityService dishAvailabilityService;
    private final MemberClient memberClient;
    private final RocketMQTemplate rocketMQTemplate;

    @Transactional
    public TableOrderView openTable(OpenTableRequest request) {
        DiningTable table = tableService.getOrThrow(request.getTableId());
        if (!"IDLE".equals(table.getStatus())) {
            throw new BizException(DiningErrorCode.TABLE_NOT_IDLE);
        }
        TableOrder order = new TableOrder();
        order.setTenantId(TenantContext.get());
        order.setMerchantId(table.getMerchantId());
        order.setTableId(table.getTableId());
        order.setStatus("OPEN");
        order.setGuestCount(request.getGuestCount());
        order.setMemberId(request.getMemberId());
        order.setTotalAmount(BigDecimal.ZERO);
        order.setOpenedAt(LocalDateTime.now());
        tableOrderMapper.insert(order);

        table.setStatus("OCCUPIED");
        table.setCurrentTableOrderId(order.getTableOrderId());
        tableService.updateTable(table);
        log.info("开台: tableOrderId={}, tableId={}", order.getTableOrderId(), table.getTableId());
        return view(order);
    }

    @Transactional
    public TableOrderView addLine(Long tableOrderId, AddLineRequest request) {
        TableOrder order = getOpenOrder(tableOrderId);
        if (dishAvailabilityService.isSoldOut(request.getSkuId())) {
            throw new BizException(DiningErrorCode.DISH_SOLD_OUT);
        }
        boolean requiresKitchen = request.getRequiresKitchen() == null || request.getRequiresKitchen();
        OrderLine line = new OrderLine();
        line.setTenantId(TenantContext.get());
        line.setTableOrderId(tableOrderId);
        line.setSkuId(request.getSkuId());
        line.setQuantity(request.getQuantity());
        line.setUnitPriceSnapshot(request.getUnitPrice());
        line.setSubtotal(request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        line.setRemark(request.getRemark());
        line.setSource(request.getSource() != null ? request.getSource() : "STAFF_PDA");
        line.setRequiresKitchen(requiresKitchen);
        // 不过厨房（饮料/零售）直接 SERVED 计入账单；过厨房先 ORDERED 走 KDS
        line.setStatus(requiresKitchen ? "ORDERED" : "SERVED");
        orderLineMapper.insert(line);
        recomputeTotal(tableOrderId);
        log.info("加菜: tableOrderId={}, skuId={}, qty={}", tableOrderId, request.getSkuId(), request.getQuantity());
        return getView(tableOrderId);
    }

    @Transactional
    public void sendToKds(Long orderLineId) {
        OrderLine line = getLine(orderLineId);
        if (!Boolean.TRUE.equals(line.getRequiresKitchen()) || !"ORDERED".equals(line.getStatus())) {
            throw new BizException(DiningErrorCode.LINE_INVALID_STATE);
        }
        line.setStatus("SENT_TO_KDS");
        orderLineMapper.updateById(line);
    }

    /** KDS 推进：SENT_TO_KDS->COOKING->READY->SERVED。 */
    @Transactional
    public void advanceLine(Long orderLineId, String target) {
        OrderLine line = getLine(orderLineId);
        boolean ok = switch (target) {
            case "COOKING" -> "SENT_TO_KDS".equals(line.getStatus());
            case "READY" -> "COOKING".equals(line.getStatus());
            case "SERVED" -> "READY".equals(line.getStatus());
            default -> false;
        };
        if (!ok) {
            throw new BizException(DiningErrorCode.LINE_INVALID_STATE);
        }
        line.setStatus(target);
        orderLineMapper.updateById(line);
    }

    /** 退菜（未制作）：ORDERED/SENT_TO_KDS -> CANCELLED，不计入账单。 */
    @Transactional
    public void cancelLine(Long orderLineId) {
        OrderLine line = getLine(orderLineId);
        if (!Set.of("ORDERED", "SENT_TO_KDS").contains(line.getStatus())) {
            throw new BizException(DiningErrorCode.LINE_INVALID_STATE);
        }
        line.setStatus("CANCELLED");
        orderLineMapper.updateById(line);
        recomputeTotal(line.getTableOrderId());
    }

    /** 退菜（已制作）：COOKING/READY/SERVED -> RETURNED（v1 未接审批，见 CLAUDE.md）。 */
    @Transactional
    public void returnLine(Long orderLineId) {
        OrderLine line = getLine(orderLineId);
        if (!Set.of("COOKING", "READY", "SERVED").contains(line.getStatus())) {
            throw new BizException(DiningErrorCode.LINE_INVALID_STATE);
        }
        line.setStatus("RETURNED");
        orderLineMapper.updateById(line);
        recomputeTotal(line.getTableOrderId());
    }

    @Transactional
    public CheckoutResponse checkout(Long tableOrderId, CheckoutRequest request) {
        TableOrder order = getOpenOrder(tableOrderId);
        List<OrderLine> lines = listLines(tableOrderId);
        for (OrderLine line : lines) {
            if (!CHECKOUT_TERMINAL.contains(line.getStatus())) {
                throw new BizException(DiningErrorCode.CHECKOUT_LINE_NOT_READY);
            }
        }
        BigDecimal payable = lines.stream()
                .filter(l -> "SERVED".equals(l.getStatus()))
                .map(OrderLine::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal walletPaid = BigDecimal.ZERO;
        BigDecimal remaining = payable;
        if (Boolean.TRUE.equals(request.getUseWallet()) && order.getMemberId() != null
                && payable.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balance = memberClient.getWalletBalance(order.getMemberId());
            if (balance.compareTo(payable) >= 0) {
                memberClient.deductWallet(order.getMemberId(), payable, order.getMerchantId(),
                        "DINING:" + tableOrderId);
                walletPaid = payable;
                remaining = BigDecimal.ZERO;
            } else {
                log.info("储值余额不足，全额转聚合支付: tableOrderId={}, balance={}, payable={}",
                        tableOrderId, balance, payable);
            }
        }
        // v1：剩余 remaining 视为当面（CASH/POS/聚合渠道）收讫记账，不再异步等待支付回调（见 CLAUDE.md）

        order.setStatus("CLOSED");
        order.setTotalAmount(payable);
        order.setWalletPaidAmount(walletPaid);
        order.setPayableAmount(remaining);
        order.setClosedAt(LocalDateTime.now());
        tableOrderMapper.updateById(order);

        if (order.getTableId() != null) {
            DiningTable table = tableService.getOrThrow(order.getTableId());
            table.setStatus("CLEANING");
            tableService.updateTable(table);
        }

        publishDiningChecked(order, lines);
        log.info("结账: tableOrderId={}, payable={}, wallet={}, remaining={}",
                tableOrderId, payable, walletPaid, remaining);
        return CheckoutResponse.builder()
                .tableOrderId(tableOrderId)
                .status("CLOSED")
                .totalAmount(payable)
                .walletPaidAmount(walletPaid)
                .payableAmount(remaining)
                .build();
    }

    public TableOrderView getView(Long tableOrderId) {
        TableOrder order = getOrder(tableOrderId);
        return view(order);
    }

    // ------------------------------------------------------------------
    private void recomputeTotal(Long tableOrderId) {
        BigDecimal total = listLines(tableOrderId).stream()
                .filter(l -> !"CANCELLED".equals(l.getStatus()) && !"RETURNED".equals(l.getStatus()))
                .map(OrderLine::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TableOrder order = getOrder(tableOrderId);
        order.setTotalAmount(total);
        tableOrderMapper.updateById(order);
    }

    private void publishDiningChecked(TableOrder order, List<OrderLine> lines) {
        List<DiningCheckedPayload.Line> served = lines.stream()
                .filter(l -> "SERVED".equals(l.getStatus()))
                .map(l -> DiningCheckedPayload.Line.builder()
                        .skuId(l.getSkuId()).quantity(l.getQuantity()).build())
                .toList();
        DiningCheckedPayload payload = DiningCheckedPayload.builder()
                .tableOrderId(order.getTableOrderId())
                .merchantId(order.getMerchantId())
                .lines(served)
                .build();
        EventEnvelope<DiningCheckedPayload> event = EventEnvelope.of("DiningChecked", order.getTenantId(), payload);
        rocketMQTemplate.syncSendOrderly(TOPIC_DINING_CHECKED, event, String.valueOf(order.getTableOrderId()));
    }

    private TableOrder getOrder(Long tableOrderId) {
        TableOrder order = tableOrderMapper.selectById(tableOrderId);
        if (order == null) {
            throw new BizException(DiningErrorCode.TABLE_ORDER_NOT_FOUND);
        }
        return order;
    }

    private TableOrder getOpenOrder(Long tableOrderId) {
        TableOrder order = getOrder(tableOrderId);
        if (!"OPEN".equals(order.getStatus())) {
            throw new BizException(DiningErrorCode.TABLE_ORDER_NOT_OPEN);
        }
        return order;
    }

    private OrderLine getLine(Long orderLineId) {
        OrderLine line = orderLineMapper.selectById(orderLineId);
        if (line == null) {
            throw new BizException(DiningErrorCode.ORDER_LINE_NOT_FOUND);
        }
        return line;
    }

    private List<OrderLine> listLines(Long tableOrderId) {
        return orderLineMapper.selectList(new LambdaQueryWrapper<OrderLine>()
                .eq(OrderLine::getTableOrderId, tableOrderId)
                .orderByAsc(OrderLine::getOrderLineId));
    }

    private TableOrderView view(TableOrder order) {
        List<TableOrderView.Line> lineViews = listLines(order.getTableOrderId()).stream()
                .map(l -> TableOrderView.Line.builder()
                        .orderLineId(l.getOrderLineId())
                        .skuId(l.getSkuId())
                        .quantity(l.getQuantity())
                        .unitPrice(l.getUnitPriceSnapshot())
                        .subtotal(l.getSubtotal())
                        .status(l.getStatus())
                        .requiresKitchen(l.getRequiresKitchen())
                        .remark(l.getRemark())
                        .build())
                .toList();
        return TableOrderView.builder()
                .tableOrderId(order.getTableOrderId())
                .tableId(order.getTableId())
                .status(order.getStatus())
                .memberId(order.getMemberId())
                .totalAmount(order.getTotalAmount())
                .lines(lineViews)
                .build();
    }
}

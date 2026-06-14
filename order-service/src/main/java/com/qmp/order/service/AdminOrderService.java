package com.qmp.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.order.dto.admin.OrderSummaryView;
import com.qmp.order.entity.TradeOrder;
import com.qmp.order.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单中心后台管理服务：按租户（+可选状态）查询订单列表，供运营后台查看。
 */
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private static final int MAX_ROWS = 200;

    private final TradeOrderMapper tradeOrderMapper;

    public List<OrderSummaryView> listOrders(String status) {
        LambdaQueryWrapper<TradeOrder> wrapper = new LambdaQueryWrapper<TradeOrder>()
                .orderByDesc(TradeOrder::getOrderId)
                .last("LIMIT " + MAX_ROWS);
        if (status != null && !status.isBlank()) {
            wrapper.eq(TradeOrder::getStatus, status);
        }
        return tradeOrderMapper.selectList(wrapper).stream()
                .map(o -> OrderSummaryView.builder()
                        .orderId(o.getOrderId())
                        .userId(o.getUserId())
                        .status(o.getStatus())
                        .totalAmount(o.getTotalAmount())
                        .paidAmount(o.getPaidAmount())
                        .paymentId(o.getPaymentId())
                        .createdAt(o.getCreatedAt())
                        .build())
                .toList();
    }
}

package com.qmp.order.job;

import com.qmp.kernel.context.TenantContext;
import com.qmp.order.entity.TradeOrder;
import com.qmp.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时关单定时任务：扫描 {@code status=PENDING_PAYMENT AND pay_expire_at < now()} 的订单，
 * 释放其预占库存并置为 {@code CANCELLED}（与库存侧 {@code ExpireReservationJob} 各司其职——
 * 库存任务回补库存桶余量，本任务收口订单状态；释放调用幂等，两者顺序无关）。
 *
 * <p><b>v1 已知限制</b>（与 inventory ExpireReservationJob 同）：{@code trade_order} 启用租户行级拦截，
 * 单次查询只能扫一个租户。本任务按 {@code order.cancel-job.tenant-ids} 配置逐租户扫描，默认仅含
 * 门票黄金路径示例租户 1001。租户增长后应改为租户注册表 + 分片调度，而非配置枚举。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancelExpiredOrderJob {

    private final OrderService orderService;

    @Value("${order.cancel-job.tenant-ids:1001}")
    private List<Long> tenantIds;

    @Scheduled(fixedDelayString = "${order.cancel-job.fixed-delay-ms:60000}")
    public void run() {
        for (Long tenantId : tenantIds) {
            TenantContext.set(tenantId);
            try {
                List<TradeOrder> expired = orderService.findExpiredPendingOrders(LocalDateTime.now());
                for (TradeOrder order : expired) {
                    orderService.cancelExpiredOrder(order.getOrderId());
                }
            } catch (Exception e) {
                log.error("超时关单任务执行异常: tenantId={}", tenantId, e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}

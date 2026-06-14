package com.qmp.inventory.job;

import com.qmp.inventory.entity.InventoryReservation;
import com.qmp.inventory.service.InventoryService;
import com.qmp.kernel.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ExpireReservation 定时任务（10 文档 5.2 {@code idx_expire_scan}）：
 * 扫描 HOLDING 且已超过 {@code hold_expire_at} 的预占，释放库存并置为 EXPIRED（07 文档 1.4 / 06 文档六.1）。
 *
 * <p><b>v1 已知限制</b>：{@code inventory_reservation} 启用了租户行级拦截（ADR-021），
 * 单次查询只能扫描一个租户。本任务按 {@code inventory.expire-job.tenant-ids} 配置的租户列表逐一扫描，
 * 默认仅包含门票黄金路径示例租户 1001。后续若租户数量增长，应改为维护租户注册表 + 分片调度
 * （见 10 文档「分库分表预案」），而非在配置中枚举租户 ID。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpireReservationJob {

    private final InventoryService inventoryService;

    @Value("${inventory.expire-job.tenant-ids:1001}")
    private List<Long> tenantIds;

    @Scheduled(fixedDelayString = "${inventory.expire-job.fixed-delay-ms:60000}")
    public void run() {
        for (Long tenantId : tenantIds) {
            TenantContext.set(tenantId);
            try {
                for (InventoryReservation reservation : inventoryService.findExpiredHoldings(LocalDateTime.now())) {
                    inventoryService.expireReservation(reservation);
                    log.info("预占已超时释放: reservationId={}, bucketId={}, quantity={}",
                            reservation.getReservationId(), reservation.getBucketId(), reservation.getQuantity());
                }
            } finally {
                TenantContext.clear();
            }
        }
    }
}

package com.qmp.kernel.inventory;

/**
 * 库存预占状态机（见 07 文档 1.4 / ADR-025 参考实现）：
 * {@code HOLDING}（预占中）→ {@code CONFIRMED}（已确认，支付成功）/ {@code RELEASED}（已释放，取消或退票）/
 * {@code EXPIRED}（预占超时，由定时任务扫描 {@code hold_expire_at} 置为该状态）。
 */
public enum ReservationStatus {
    HOLDING,
    CONFIRMED,
    RELEASED,
    EXPIRED
}

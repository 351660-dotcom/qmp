package com.qmp.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qmp.kernel.inventory.InventoryReservationBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预占记录（复用 {@link InventoryReservationBase} 的预占状态机）。
 * {@code reservation_id} = 预订单ID:座位ID（或 :SESSION）；{@code bucketRef} 区分 SESSION/SEAT。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("performance_reservation")
public class PerformanceReservation extends InventoryReservationBase {

    private Long performanceBookingId;

    private Long sessionId;

    private String seatId;

    /** SESSION/SEAT，决定 bucket_id 归属哪张桶表。 */
    private String bucketRef;
}

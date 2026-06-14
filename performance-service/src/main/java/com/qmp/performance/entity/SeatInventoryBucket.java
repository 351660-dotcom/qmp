package com.qmp.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qmp.kernel.inventory.InventoryBucketBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 座位库存桶（14 文档 2.2，剧院选座/游船舱位）。复用同一套 {@link InventoryBucketBase}，
 * 只是粒度更细：每个座位/舱位一条桶（{@code total_quota}=座位容量，剧院座位=1，舱位=定员）。
 * 维度 = {@code session_id + seat_id}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seat_inventory_bucket")
public class SeatInventoryBucket extends InventoryBucketBase {

    private Long sessionId;

    private String seatId;

    private Long scenicId;
}

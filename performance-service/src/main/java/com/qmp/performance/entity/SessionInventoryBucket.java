package com.qmp.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qmp.kernel.inventory.InventoryBucketBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 场次库存桶（14 文档 1.2，无座位业态）。按 ADR-025 独立建表，复用 {@link InventoryBucketBase}。
 * 库存维度 = {@code session_id}（对照门票 sale_date+time_slot_id）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("session_inventory_bucket")
public class SessionInventoryBucket extends InventoryBucketBase {

    private Long sessionId;

    private Long scenicId;
}

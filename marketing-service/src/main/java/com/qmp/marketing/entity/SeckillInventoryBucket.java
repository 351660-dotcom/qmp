package com.qmp.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qmp.kernel.inventory.InventoryBucketBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 秒杀库存桶（13 文档 5.1，ADR-025 独立表，复用 {@link InventoryBucketBase}）。
 * 维度 = sku_id + activity_id；与该 SKU 的常规库存彼此独立。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_inventory_bucket")
public class SeckillInventoryBucket extends InventoryBucketBase {

    private Long activityId;
}

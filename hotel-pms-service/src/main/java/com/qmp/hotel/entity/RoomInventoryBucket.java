package com.qmp.hotel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qmp.kernel.inventory.InventoryBucketBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房晚库存桶（11 文档 1.2 {@code room_inventory_bucket}）。按 ADR-025 独立建表，
 * 字段范式与防超卖两道防线复用 {@link InventoryBucketBase}（与门票 inventory_bucket 同设计、不共表）。
 * 维度 = {@code sku_id}（房型）+ {@code sale_date}（间夜），{@code time_slot_id} 固定 0。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("room_inventory_bucket")
public class RoomInventoryBucket extends InventoryBucketBase {

    /** 门票域特有字段对齐：景区维度。 */
    private Long scenicId;
}

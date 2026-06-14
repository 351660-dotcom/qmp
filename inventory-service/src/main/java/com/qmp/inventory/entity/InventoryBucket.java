package com.qmp.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qmp.kernel.inventory.InventoryBucketBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 门票「日期/场次型」库存桶（10 文档 5.1 {@code inventory_bucket}），继承 ADR-025 参考实现。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inventory_bucket")
public class InventoryBucket extends InventoryBucketBase {

    private Long scenicId;
}

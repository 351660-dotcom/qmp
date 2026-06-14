package com.qmp.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qmp.kernel.inventory.InventoryReservationBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存预占记录（10 文档 5.2 {@code inventory_reservation}），继承 ADR-025 参考实现。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inventory_reservation")
public class InventoryReservation extends InventoryReservationBase {
}

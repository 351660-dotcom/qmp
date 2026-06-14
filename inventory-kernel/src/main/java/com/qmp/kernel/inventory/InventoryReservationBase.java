package com.qmp.kernel.inventory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存预占记录通用字段，ADR-025 参考实现（见 07 文档 1.2/1.4）。
 *
 * <p>{@code reservationId} 由调用方传入（约定取 {@code order_item_id} 的字符串形式），
 * 作为幂等键：{@code CreateReservation} 若发现记录已存在直接返回已有结果。</p>
 */
@Data
public abstract class InventoryReservationBase {

    @TableId(type = IdType.INPUT)
    private String reservationId;

    private Long tenantId;

    private Long bucketId;

    private Integer quantity;

    /** {@link ReservationStatus} 的字符串值：HOLDING/CONFIRMED/RELEASED/EXPIRED。 */
    private String status;

    private LocalDateTime holdExpireAt;

    /** 由 MySQL {@code DEFAULT CURRENT_TIMESTAMP} 填充，应用层不写入。 */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    /** 由 MySQL {@code DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP} 填充，应用层不写入。 */
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}

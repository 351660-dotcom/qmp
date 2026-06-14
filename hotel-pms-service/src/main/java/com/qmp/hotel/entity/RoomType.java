package com.qmp.hotel.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房型（11 文档 1.1 RoomType，对应门票域的 TicketSku）。
 * {@code sku_id} 与门票 SKU 同一序列，房晚库存桶以此为关联键。
 */
@Data
@TableName("room_type")
public class RoomType {

    @TableId(type = IdType.ASSIGN_ID)
    private Long roomTypeId;

    private Long tenantId;

    private Long scenicId;

    private Long merchantId;

    /** 库存桶关联键（与门票 SKU 同序列）。 */
    private Long skuId;

    private String name;

    /** DRAFT/ON_SALE/OFF_SALE。 */
    private String status;

    /** v1 房晚基准价（未接价格日历/连住价，见 CLAUDE.md）。 */
    private BigDecimal basePrice;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}

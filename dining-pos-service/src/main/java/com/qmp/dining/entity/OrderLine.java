package com.qmp.dining.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 点单项（12 文档 1.4）。状态机见 12 文档 1.5。
 */
@Data
@TableName("order_line")
public class OrderLine {

    @TableId(type = IdType.ASSIGN_ID)
    private Long orderLineId;

    private Long tenantId;

    private Long tableOrderId;

    private Long skuId;

    private Integer quantity;

    private BigDecimal unitPriceSnapshot;

    private BigDecimal subtotal;

    private String remark;

    /** CUSTOMER_QR/STAFF_PDA。 */
    private String source;

    /** 是否需过厨房（决定走 KDS 流程 + BOM 核减）。 */
    private Boolean requiresKitchen;

    /** ORDERED/SENT_TO_KDS/COOKING/READY/SERVED/CANCELLED/RETURNED。 */
    private String status;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}

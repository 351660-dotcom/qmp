package com.qmp.performance.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 场次（14 文档 1.1）：游船班次/游乐项目/演出场次。
 */
@Data
@TableName("performance_session")
public class PerformanceSession {

    @TableId(type = IdType.ASSIGN_ID)
    private Long sessionId;

    private Long tenantId;

    private Long scenicId;

    private Long merchantId;

    /** 关联商品 SKU（与门票同序列）。 */
    private Long skuId;

    private String name;

    /** SHOW(演出)/BOAT(游船)/RIDE(游乐)。 */
    private String sessionType;

    private LocalDateTime startTime;

    /** DRAFT/ON_SALE/CANCELLED/CLOSED。 */
    private String status;

    private BigDecimal basePrice;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}

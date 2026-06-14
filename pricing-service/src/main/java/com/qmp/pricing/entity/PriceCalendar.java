package com.qmp.pricing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 价格日历（10 文档 3.1 {@code price_calendar}）。
 */
@Data
@TableName("price_calendar")
public class PriceCalendar {

    @TableId(type = IdType.ASSIGN_ID)
    private Long priceCalendarId;

    private Long tenantId;

    private Long skuId;

    private LocalDate saleDate;

    /** RETAIL/MEMBER */
    private String priceType;

    private BigDecimal price;

    @TableField(value = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}

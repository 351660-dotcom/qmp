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
 * 手牌/腕带二次消费账户（14 文档 4.1，温泉/水乐园）。
 */
@Data
@TableName("wristband_account")
public class WristbandAccount {

    @TableId(type = IdType.ASSIGN_ID)
    private Long wristbandId;

    private Long tenantId;

    private Long scenicId;

    /** 可选绑定会员（出示会员码时回填）。 */
    private Long userId;

    private BigDecimal balance;

    /** ACTIVE/CLOSED（退还结算后关闭）。 */
    private String status;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}

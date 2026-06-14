package com.qmp.member.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员等级（13 文档 1.2）。成长值达到 {@code minGrowthValue} 自动升级。
 */
@Data
@TableName("member_level")
public class MemberLevel {

    @TableId(type = IdType.ASSIGN_ID)
    private Long levelId;

    private Long tenantId;

    private String levelName;

    private Integer minGrowthValue;

    /** 权益 JSON：会员价折扣率/积分倍率/专属券模板等。 */
    private String benefits;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}

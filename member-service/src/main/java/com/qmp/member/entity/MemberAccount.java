package com.qmp.member.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员账户（10 文档 4.1 {@code member_account}）。
 */
@Data
@TableName("member_account")
public class MemberAccount {

    @TableId
    private Long userId;

    private Long tenantId;

    private Boolean isMember;

    private LocalDateTime memberSince;

    @TableField(value = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}

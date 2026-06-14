package com.qmp.verification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 核验流水（10 文档 7.2 {@code verification_record}）。仅追加写，用于在园人数等聚合统计。
 */
@Data
@TableName("verification_record")
public class VerificationRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long verificationId;

    private Long tenantId;

    private Long credentialId;

    /** 冗余存储，免 join（10 文档设计决策 4）。 */
    private Long scenicId;

    private String terminalId;

    /** SUCCESS/ALREADY_VERIFIED/EXPIRED/INVALID_SIGNATURE。 */
    private String result;

    private LocalDateTime verifiedAt;
}

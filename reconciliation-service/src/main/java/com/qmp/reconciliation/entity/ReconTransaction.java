package com.qmp.reconciliation.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 统一对账流水：各业态资金事件归集成的一条记录。
 */
@Data
@TableName("recon_transaction")
public class ReconTransaction {

    @TableId(type = IdType.ASSIGN_ID)
    private Long txnId;

    private Long tenantId;

    private Long merchantId;

    /** 资金来源：PAYMENT/REFUND/WALLET/WRISTBAND。 */
    private String source;

    /** 资金方向：IN（商户收款）/ OUT（退款/出账）。 */
    private String direction;

    /** 业务关联单据（payment_id / order_id / source_ref）。 */
    private String bizRef;

    private BigDecimal amount;

    /** 支付渠道（WALLET/WRISTBAND 等非聚合渠道可空）。 */
    private String channel;

    private LocalDateTime occurredAt;

    private LocalDate reconDate;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}

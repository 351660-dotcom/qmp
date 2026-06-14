-- 会员体系扩展（13 文档一）：等级 / 积分（账户+账本）/ 储值（账户+账本）/ 事件去重。
-- member_account 扩展等级与成长值（13 文档 1.1）。

ALTER TABLE member_account
  ADD COLUMN level_id     BIGINT UNSIGNED NULL COMMENT '关联 member_level' AFTER tenant_id,
  ADD COLUMN growth_value INT NOT NULL DEFAULT 0 COMMENT '成长值，累计消费折算' AFTER level_id;

CREATE TABLE member_level (
  level_id         BIGINT UNSIGNED NOT NULL,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  level_name       VARCHAR(32)     NOT NULL COMMENT '普通/银卡/金卡/黑卡',
  min_growth_value INT             NOT NULL COMMENT '达到该成长值自动升级',
  benefits         JSON            NULL COMMENT '会员价折扣率/积分倍率/专属券模板等',
  created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (level_id),
  KEY idx_tenant_growth (tenant_id, min_growth_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE point_account (
  user_id    BIGINT UNSIGNED NOT NULL,
  tenant_id  BIGINT UNSIGNED NOT NULL,
  balance    INT             NOT NULL DEFAULT 0 COMMENT '当前积分余额',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE point_ledger (
  ledger_id          BIGINT UNSIGNED NOT NULL,
  tenant_id          BIGINT UNSIGNED NOT NULL,
  user_id            BIGINT UNSIGNED NOT NULL,
  change_amount      INT             NOT NULL COMMENT '正获取/负消耗',
  balance_after      INT             NOT NULL COMMENT '记账后余额，便于审计对账',
  type               VARCHAR(20)     NOT NULL COMMENT 'EARN/REDEEM/EXPIRE/ADJUST',
  source_merchant_id BIGINT UNSIGNED NULL COMMENT '产生/核销积分的商户，用于跨商户分摊',
  source_ref         VARCHAR(64)     NOT NULL COMMENT '来源单据，幂等键',
  created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (ledger_id),
  UNIQUE KEY uk_source_type (source_ref, type) COMMENT '(source_ref,type) 幂等：PAID 重复投递不重复入账',
  KEY idx_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE member_wallet (
  user_id    BIGINT UNSIGNED NOT NULL,
  tenant_id  BIGINT UNSIGNED NOT NULL,
  balance    DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '储值余额（集团级跨商户通用）',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wallet_ledger (
  ledger_id     BIGINT UNSIGNED NOT NULL,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  user_id       BIGINT UNSIGNED NOT NULL,
  change_amount DECIMAL(12,2)   NOT NULL COMMENT '正充值/退款，负消费',
  balance_after DECIMAL(12,2)   NOT NULL,
  type          VARCHAR(20)     NOT NULL COMMENT 'RECHARGE/CONSUME/REFUND/ADJUST',
  merchant_id   BIGINT UNSIGNED NULL COMMENT 'CONSUME 时实际消费商户，用于分摊',
  source_ref    VARCHAR(64)     NOT NULL COMMENT '幂等键',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (ledger_id),
  UNIQUE KEY uk_source_type (source_ref, type),
  KEY idx_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 事件消费去重（OrderPaid 等）。无 tenant_id 列，已在 kernel TenantLineHandlerImpl 中排除。
CREATE TABLE processed_event (
  consumer_group VARCHAR(64)  NOT NULL,
  event_id       VARCHAR(64)  NOT NULL,
  event_type     VARCHAR(64)  NOT NULL,
  processed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (consumer_group, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

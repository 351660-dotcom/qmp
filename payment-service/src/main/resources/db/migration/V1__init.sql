CREATE TABLE payment_order (
  payment_id       VARCHAR(64)   NOT NULL,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  merchant_id      BIGINT UNSIGNED NOT NULL,
  order_id         BIGINT UNSIGNED NOT NULL,
  amount           DECIMAL(10,2) NOT NULL,
  channel          VARCHAR(20)   NOT NULL COMMENT 'WECHAT/ALIPAY',
  status           VARCHAR(20)   NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/PAID/CLOSED',
  channel_trade_no VARCHAR(64)   NULL,
  paid_at          DATETIME      NULL,
  created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (payment_id),
  UNIQUE KEY uk_order (order_id) COMMENT '一个订单仅一个有效支付单',
  KEY idx_tenant_merchant (tenant_id, merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE settlement_record (
  settlement_id    BIGINT UNSIGNED NOT NULL,
  tenant_id        BIGINT UNSIGNED NOT NULL,
  payment_id       VARCHAR(64)     NOT NULL,
  merchant_id      BIGINT UNSIGNED NOT NULL,
  platform_amount  DECIMAL(10,2)   NOT NULL,
  merchant_amount  DECIMAL(10,2)   NOT NULL,
  status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SETTLED/FAILED',
  settle_channel_no VARCHAR(64)    NULL,
  created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (settlement_id),
  UNIQUE KEY uk_payment (payment_id) COMMENT 'v1两级分账，一个支付单一条分账记录',
  KEY idx_merchant_status (merchant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refund_record (
  refund_id         BIGINT UNSIGNED NOT NULL,
  tenant_id         BIGINT UNSIGNED NOT NULL,
  payment_id        VARCHAR(64)     NOT NULL,
  credential_id     BIGINT UNSIGNED NOT NULL,
  amount            DECIMAL(10,2)   NOT NULL,
  status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCEEDED/FAILED',
  refund_channel_no VARCHAR(64)     NULL,
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (refund_id),
  UNIQUE KEY uk_credential (credential_id) COMMENT '一张票仅一条有效退款记录，失败重试更新同一行',
  KEY idx_payment (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

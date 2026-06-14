-- 跨业态统一对账（④）。

CREATE TABLE recon_transaction (
  txn_id      BIGINT UNSIGNED NOT NULL,
  tenant_id   BIGINT UNSIGNED NOT NULL,
  merchant_id BIGINT UNSIGNED NULL,
  source      VARCHAR(20)     NOT NULL COMMENT 'PAYMENT/REFUND/WALLET/WRISTBAND',
  direction   VARCHAR(8)      NOT NULL COMMENT 'IN/OUT',
  biz_ref     VARCHAR(64)     NULL COMMENT 'payment_id / order_id / source_ref',
  amount      DECIMAL(12,2)   NOT NULL,
  channel     VARCHAR(20)     NULL,
  occurred_at DATETIME        NOT NULL,
  recon_date  DATE            NOT NULL,
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (txn_id),
  KEY idx_recon (tenant_id, recon_date, merchant_id),
  KEY idx_source (source, recon_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 事件消费去重（10 文档 9.1）。无 tenant_id 列，已在 kernel TenantLineHandlerImpl 中排除。
CREATE TABLE processed_event (
  consumer_group VARCHAR(64)  NOT NULL,
  event_id       VARCHAR(64)  NOT NULL,
  event_type     VARCHAR(64)  NOT NULL,
  processed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (consumer_group, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

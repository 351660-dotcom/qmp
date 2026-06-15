-- 分账抽成按商户配置：每个商户可设不同的平台抽成比例（如零售业态下挂多商户各设各的）。
-- createSettlementRecord 据此算 platform_amount = amount × rate、merchant_amount = amount - platform_amount。
-- 未配置的商户默认 0（全额归商户），与原占位行为一致。
CREATE TABLE merchant_commission (
  merchant_id     BIGINT UNSIGNED NOT NULL COMMENT '商户ID',
  tenant_id       BIGINT UNSIGNED NOT NULL,
  commission_rate DECIMAL(5,4)    NOT NULL DEFAULT 0 COMMENT '平台抽成比例，0..1（如 0.0600=6%）',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ticket_product (
  product_id          BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  tenant_id           BIGINT UNSIGNED NOT NULL,
  scenic_id           BIGINT UNSIGNED NOT NULL,
  merchant_id         BIGINT UNSIGNED NOT NULL,
  name                VARCHAR(128)    NOT NULL,
  description         TEXT            NULL,
  status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING_REVIEW/ON_SALE/OFF_SALE',
  valid_period_rule   JSON            NOT NULL,
  real_name_rule      VARCHAR(20)     NOT NULL DEFAULT 'NONE' COMMENT 'NONE/ONE_TICKET_ONE_ID/ONE_ORDER_MULTI_PERSON',
  created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (product_id),
  KEY idx_tenant_scenic (tenant_id, scenic_id),
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ticket_sku (
  sku_id                BIGINT UNSIGNED NOT NULL,
  tenant_id             BIGINT UNSIGNED NOT NULL,
  product_id            BIGINT UNSIGNED NOT NULL,
  ticket_type           VARCHAR(20)     NOT NULL COMMENT 'ADULT/CHILD/SENIOR/STUDENT/MILITARY',
  requires_time_slot    TINYINT(1)      NOT NULL DEFAULT 0,
  time_slot_definitions JSON            NULL,
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (sku_id),
  KEY idx_product (product_id),
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

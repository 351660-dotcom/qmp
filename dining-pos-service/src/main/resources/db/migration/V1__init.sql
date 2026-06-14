-- 餐饮 POS v1 表结构（12 文档一/二）。

CREATE TABLE dining_table (
  table_id                BIGINT UNSIGNED NOT NULL,
  tenant_id               BIGINT UNSIGNED NOT NULL,
  merchant_id             BIGINT UNSIGNED NOT NULL,
  area_id                 BIGINT UNSIGNED NULL,
  table_no                VARCHAR(32)     NOT NULL,
  capacity                INT             NOT NULL DEFAULT 0,
  status                  VARCHAR(20)     NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE/OCCUPIED/RESERVED/CLEANING',
  current_table_order_id  BIGINT UNSIGNED NULL,
  created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (table_id),
  KEY idx_merchant (merchant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE table_order (
  table_order_id     BIGINT UNSIGNED NOT NULL,
  tenant_id          BIGINT UNSIGNED NOT NULL,
  merchant_id        BIGINT UNSIGNED NOT NULL,
  table_id           BIGINT UNSIGNED NULL COMMENT '零售即时收银为空',
  status             VARCHAR(20)     NOT NULL COMMENT 'OPEN/SETTLING/CLOSED/VOIDED',
  guest_count        INT             NULL,
  member_id          BIGINT UNSIGNED NULL COMMENT '关联会员，用于储值抵扣/积分',
  total_amount       DECIMAL(10,2)   NOT NULL DEFAULT 0,
  wallet_paid_amount DECIMAL(10,2)   NULL COMMENT '会员储值抵扣额',
  payable_amount     DECIMAL(10,2)   NULL COMMENT '抵扣后仍需聚合支付额',
  opened_at          DATETIME        NULL,
  closed_at          DATETIME        NULL,
  created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (table_order_id),
  KEY idx_merchant_status (merchant_id, status),
  KEY idx_table (table_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_line (
  order_line_id        BIGINT UNSIGNED NOT NULL,
  tenant_id            BIGINT UNSIGNED NOT NULL,
  table_order_id       BIGINT UNSIGNED NOT NULL,
  sku_id               BIGINT UNSIGNED NOT NULL,
  quantity             INT             NOT NULL,
  unit_price_snapshot  DECIMAL(10,2)   NOT NULL,
  subtotal             DECIMAL(10,2)   NOT NULL,
  remark               VARCHAR(255)    NULL,
  source               VARCHAR(20)     NOT NULL DEFAULT 'STAFF_PDA' COMMENT 'CUSTOMER_QR/STAFF_PDA',
  requires_kitchen     TINYINT(1)      NOT NULL DEFAULT 1,
  status               VARCHAR(20)     NOT NULL COMMENT 'ORDERED/SENT_TO_KDS/COOKING/READY/SERVED/CANCELLED/RETURNED',
  created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (order_line_id),
  KEY idx_table_order (table_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dish_availability (
  sku_id      BIGINT UNSIGNED NOT NULL,
  tenant_id   BIGINT UNSIGNED NOT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL,
  status      VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE/SOLD_OUT',
  updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (sku_id),
  KEY idx_merchant (merchant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

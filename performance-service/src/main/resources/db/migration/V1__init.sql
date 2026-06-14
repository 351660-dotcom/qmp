-- 演出/游船/游乐 v1 表结构（14 文档）。场次/座位库存桶字段范式复用门票 inventory_bucket（ADR-025，独立表）。

CREATE TABLE performance_session (
  session_id   BIGINT UNSIGNED NOT NULL,
  tenant_id    BIGINT UNSIGNED NOT NULL,
  scenic_id    BIGINT UNSIGNED NOT NULL,
  merchant_id  BIGINT UNSIGNED NOT NULL,
  sku_id       BIGINT UNSIGNED NOT NULL,
  name         VARCHAR(128)    NOT NULL,
  session_type VARCHAR(20)     NOT NULL COMMENT 'SHOW/BOAT/RIDE',
  start_time   DATETIME        NULL,
  status       VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ON_SALE/CANCELLED/CLOSED',
  base_price   DECIMAL(10,2)   NOT NULL,
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (session_id),
  KEY idx_tenant_scenic (tenant_id, scenic_id, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE session_inventory_bucket (
  bucket_id     BIGINT UNSIGNED NOT NULL,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  scenic_id     BIGINT UNSIGNED NOT NULL,
  session_id    BIGINT UNSIGNED NOT NULL,
  sku_id        BIGINT UNSIGNED NOT NULL DEFAULT 0,
  sale_date     DATE            NOT NULL,
  time_slot_id  BIGINT UNSIGNED NOT NULL DEFAULT 0,
  total_quota   INT             NOT NULL,
  sold_count    INT             NOT NULL DEFAULT 0,
  locked_count  INT             NOT NULL DEFAULT 0,
  channel_quota JSON            NOT NULL,
  version       INT             NOT NULL DEFAULT 0,
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (bucket_id),
  UNIQUE KEY uk_session (session_id),
  CONSTRAINT chk_session_not_oversell CHECK (sold_count + locked_count <= total_quota)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE seat_inventory_bucket (
  bucket_id     BIGINT UNSIGNED NOT NULL,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  scenic_id     BIGINT UNSIGNED NOT NULL,
  session_id    BIGINT UNSIGNED NOT NULL,
  seat_id       VARCHAR(32)     NOT NULL,
  sku_id        BIGINT UNSIGNED NOT NULL DEFAULT 0,
  sale_date     DATE            NOT NULL,
  time_slot_id  BIGINT UNSIGNED NOT NULL DEFAULT 0,
  total_quota   INT             NOT NULL COMMENT '座位容量：剧院座位=1，舱位=定员',
  sold_count    INT             NOT NULL DEFAULT 0,
  locked_count  INT             NOT NULL DEFAULT 0,
  channel_quota JSON            NOT NULL,
  version       INT             NOT NULL DEFAULT 0,
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (bucket_id),
  UNIQUE KEY uk_session_seat (session_id, seat_id),
  CONSTRAINT chk_seat_not_oversell CHECK (sold_count + locked_count <= total_quota)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE performance_reservation (
  reservation_id          VARCHAR(80)   NOT NULL COMMENT '预订单ID:座位ID（或:SESSION）',
  tenant_id               BIGINT UNSIGNED NOT NULL,
  performance_booking_id  BIGINT UNSIGNED NOT NULL,
  session_id              BIGINT UNSIGNED NOT NULL,
  seat_id                 VARCHAR(32)   NULL,
  bucket_ref              VARCHAR(20)   NOT NULL COMMENT 'SESSION/SEAT',
  bucket_id               BIGINT UNSIGNED NOT NULL,
  quantity                INT           NOT NULL,
  status                  VARCHAR(20)   NOT NULL COMMENT 'HOLDING/CONFIRMED/RELEASED/EXPIRED',
  hold_expire_at          DATETIME      NOT NULL,
  created_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (reservation_id),
  KEY idx_booking (performance_booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE performance_booking (
  booking_id   BIGINT UNSIGNED NOT NULL,
  tenant_id    BIGINT UNSIGNED NOT NULL,
  scenic_id    BIGINT UNSIGNED NOT NULL,
  merchant_id  BIGINT UNSIGNED NOT NULL,
  user_id      BIGINT UNSIGNED NOT NULL,
  session_id   BIGINT UNSIGNED NOT NULL,
  bucket_ref   VARCHAR(20)     NOT NULL,
  seat_id      VARCHAR(32)     NULL,
  quantity     INT             NOT NULL,
  status       VARCHAR(20)     NOT NULL COMMENT 'PENDING_PAYMENT/CONFIRMED/CANCELLED',
  total_amount DECIMAL(10,2)   NOT NULL,
  payment_id   VARCHAR(64)     NULL,
  version      INT             NOT NULL DEFAULT 0,
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (booking_id),
  KEY idx_tenant_user (tenant_id, user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wristband_account (
  wristband_id BIGINT UNSIGNED NOT NULL,
  tenant_id    BIGINT UNSIGNED NOT NULL,
  scenic_id    BIGINT UNSIGNED NOT NULL,
  user_id      BIGINT UNSIGNED NULL COMMENT '可选绑定会员',
  balance      DECIMAL(12,2)   NOT NULL DEFAULT 0,
  status       VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (wristband_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wristband_ledger (
  ledger_id     BIGINT UNSIGNED NOT NULL,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  wristband_id  BIGINT UNSIGNED NOT NULL,
  change_amount DECIMAL(12,2)   NOT NULL,
  balance_after DECIMAL(12,2)   NOT NULL,
  type          VARCHAR(20)     NOT NULL COMMENT 'RECHARGE/CONSUME/REFUND',
  merchant_id   BIGINT UNSIGNED NULL,
  source_ref    VARCHAR(64)     NOT NULL,
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (ledger_id),
  UNIQUE KEY uk_source_type (source_ref, type),
  KEY idx_wristband (wristband_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 事件消费去重（10 文档 9.1）。无 tenant_id 列，已在 kernel TenantLineHandlerImpl 中排除。
CREATE TABLE processed_event (
  consumer_group VARCHAR(64)  NOT NULL,
  event_id       VARCHAR(64)  NOT NULL,
  event_type     VARCHAR(64)  NOT NULL,
  processed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (consumer_group, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 酒店 PMS v1 表结构（11 文档）。按 ADR-025 独立建表，字段范式复用门票域 inventory_bucket/inventory_reservation。

CREATE TABLE room_type (
  room_type_id BIGINT UNSIGNED NOT NULL,
  tenant_id    BIGINT UNSIGNED NOT NULL,
  scenic_id    BIGINT UNSIGNED NOT NULL,
  merchant_id  BIGINT UNSIGNED NOT NULL,
  sku_id       BIGINT UNSIGNED NOT NULL COMMENT '与门票SKU同序列，库存桶关联键',
  name         VARCHAR(128)    NOT NULL,
  status       VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ON_SALE/OFF_SALE',
  base_price   DECIMAL(10,2)   NOT NULL COMMENT 'v1房晚基准价（未接价格日历/连住价）',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (room_type_id),
  UNIQUE KEY uk_sku (sku_id),
  KEY idx_tenant_scenic (tenant_id, scenic_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 房晚库存桶（字段范式同门票 inventory_bucket，独立表）
CREATE TABLE room_inventory_bucket (
  bucket_id     BIGINT UNSIGNED NOT NULL,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  scenic_id     BIGINT UNSIGNED NOT NULL,
  sku_id        BIGINT UNSIGNED NOT NULL,
  sale_date     DATE            NOT NULL COMMENT '间夜日期',
  time_slot_id  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '房晚固定为0',
  total_quota   INT             NOT NULL,
  sold_count    INT             NOT NULL DEFAULT 0,
  locked_count  INT             NOT NULL DEFAULT 0,
  channel_quota JSON            NOT NULL COMMENT 'v1: {"direct": total_quota}',
  version       INT             NOT NULL DEFAULT 0 COMMENT '乐观锁',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (bucket_id),
  UNIQUE KEY uk_sku_date_slot (sku_id, sale_date, time_slot_id),
  KEY idx_tenant_scenic_date (tenant_id, scenic_id, sale_date),
  CONSTRAINT chk_room_not_oversell CHECK (sold_count + locked_count <= total_quota)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 间夜预占（连住中的单晚），状态机复用门票预占
CREATE TABLE room_night_reservation (
  reservation_id        VARCHAR(80)   NOT NULL COMMENT '预订单ID:间夜日期',
  tenant_id             BIGINT UNSIGNED NOT NULL,
  hotel_reservation_id  BIGINT UNSIGNED NOT NULL COMMENT '所属预订单',
  sale_date             DATE          NOT NULL,
  bucket_id             BIGINT UNSIGNED NOT NULL,
  quantity              INT           NOT NULL,
  status                VARCHAR(20)   NOT NULL COMMENT 'HOLDING/CONFIRMED/RELEASED/EXPIRED',
  hold_expire_at        DATETIME      NOT NULL,
  created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (reservation_id),
  KEY idx_reservation (hotel_reservation_id),
  KEY idx_bucket (bucket_id),
  KEY idx_expire_scan (status, hold_expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预订单
CREATE TABLE room_reservation (
  reservation_id BIGINT UNSIGNED NOT NULL,
  tenant_id      BIGINT UNSIGNED NOT NULL,
  scenic_id      BIGINT UNSIGNED NOT NULL,
  merchant_id    BIGINT UNSIGNED NOT NULL,
  user_id        BIGINT UNSIGNED NOT NULL,
  sku_id         BIGINT UNSIGNED NOT NULL,
  check_in_date  DATE            NOT NULL,
  check_out_date DATE            NOT NULL,
  nights         INT             NOT NULL,
  room_count     INT             NOT NULL,
  status         VARCHAR(20)     NOT NULL COMMENT 'PENDING_PAYMENT/CONFIRMED/CANCELLED',
  total_amount   DECIMAL(10,2)   NOT NULL,
  payment_id     VARCHAR(64)     NULL,
  version        INT             NOT NULL DEFAULT 0,
  created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (reservation_id),
  KEY idx_tenant_user (tenant_id, user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 事件消费去重表（10 文档 9.1）。无 tenant_id 列，已在 kernel TenantLineHandlerImpl 中排除。
CREATE TABLE processed_event (
  consumer_group VARCHAR(64)  NOT NULL,
  event_id       VARCHAR(64)  NOT NULL,
  event_type     VARCHAR(64)  NOT NULL,
  processed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (consumer_group, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

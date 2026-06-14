-- 营销规则引擎 + 秒杀（13 文档三/五）。

CREATE TABLE promotion_rule (
  rule_id      BIGINT UNSIGNED NOT NULL,
  tenant_id    BIGINT UNSIGNED NOT NULL,
  scope        VARCHAR(20)     NOT NULL DEFAULT 'SCENIC' COMMENT 'MERCHANT/SCENIC/GROUP',
  scope_id     BIGINT UNSIGNED NULL,
  rule_type    VARCHAR(20)     NOT NULL COMMENT 'FULL_REDUCTION/DISCOUNT',
  conditions   JSON            NULL COMMENT '适用条件（v1 预留）',
  actions      JSON            NOT NULL COMMENT 'DISCOUNT:{discount_rate}; FULL_REDUCTION:{threshold,reduce}',
  stack_policy VARCHAR(20)     NOT NULL DEFAULT 'EXCLUSIVE' COMMENT 'EXCLUSIVE/STACKABLE',
  status       VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'DRAFT/ACTIVE/EXPIRED',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (rule_id),
  KEY idx_tenant_status_type (tenant_id, status, rule_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE seckill_activity (
  activity_id   BIGINT UNSIGNED NOT NULL,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  sku_id        BIGINT UNSIGNED NOT NULL,
  seckill_price DECIMAL(10,2)   NOT NULL,
  start_time    DATETIME        NOT NULL,
  end_time      DATETIME        NOT NULL,
  status        VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACTIVE/ENDED',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (activity_id),
  KEY idx_tenant_sku (tenant_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 秒杀独立库存桶（ADR-025：与常规库存彼此独立；字段范式同门票 inventory_bucket）
CREATE TABLE seckill_inventory_bucket (
  bucket_id     BIGINT UNSIGNED NOT NULL,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  activity_id   BIGINT UNSIGNED NOT NULL,
  sku_id        BIGINT UNSIGNED NOT NULL,
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
  UNIQUE KEY uk_activity (activity_id),
  CONSTRAINT chk_seckill_not_oversell CHECK (sold_count + locked_count <= total_quota)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 秒杀预占（v1 限购 1：reservation_id = activity_id:user_id，唯一即限购）
CREATE TABLE seckill_reservation (
  reservation_id VARCHAR(80)   NOT NULL COMMENT 'activity_id:user_id',
  tenant_id      BIGINT UNSIGNED NOT NULL,
  activity_id    BIGINT UNSIGNED NOT NULL,
  user_id        BIGINT UNSIGNED NOT NULL,
  bucket_id      BIGINT UNSIGNED NOT NULL,
  quantity       INT           NOT NULL,
  status         VARCHAR(20)   NOT NULL COMMENT 'HOLDING/CONFIRMED/RELEASED/EXPIRED',
  hold_expire_at DATETIME      NOT NULL,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (reservation_id),
  KEY idx_activity (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_bucket (
  bucket_id     BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  tenant_id     BIGINT UNSIGNED NOT NULL,
  scenic_id     BIGINT UNSIGNED NOT NULL,
  sku_id        BIGINT UNSIGNED NOT NULL,
  sale_date     DATE            NOT NULL,
  time_slot_id  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '无分时预约固定为0',
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
  CONSTRAINT chk_inventory_not_oversell CHECK (sold_count + locked_count <= total_quota)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_reservation (
  reservation_id  VARCHAR(64)  NOT NULL COMMENT '调用方传入，约定=order_item_id，幂等键',
  tenant_id       BIGINT UNSIGNED NOT NULL,
  bucket_id       BIGINT UNSIGNED NOT NULL,
  quantity        INT          NOT NULL,
  status          VARCHAR(20)  NOT NULL COMMENT 'HOLDING/CONFIRMED/RELEASED/EXPIRED',
  hold_expire_at  DATETIME     NOT NULL,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (reservation_id),
  KEY idx_bucket (bucket_id),
  KEY idx_expire_scan (status, hold_expire_at) COMMENT 'ExpireReservation定时任务扫描'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

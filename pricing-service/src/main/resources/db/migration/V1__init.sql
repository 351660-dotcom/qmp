CREATE TABLE price_calendar (
  price_calendar_id BIGINT UNSIGNED NOT NULL COMMENT '雪花ID',
  tenant_id         BIGINT UNSIGNED NOT NULL,
  sku_id            BIGINT UNSIGNED NOT NULL,
  sale_date         DATE            NOT NULL,
  price_type        VARCHAR(10)     NOT NULL COMMENT 'RETAIL/MEMBER',
  price             DECIMAL(10,2)   NOT NULL,
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (price_calendar_id),
  UNIQUE KEY uk_sku_date_type (sku_id, sale_date, price_type),
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

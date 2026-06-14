-- 供应链 v1 表结构（12 文档五/六）。采购/调拨/中央厨房/成本核算为后续迭代。

CREATE TABLE warehouse (
  warehouse_id BIGINT UNSIGNED NOT NULL,
  tenant_id    BIGINT UNSIGNED NOT NULL,
  owner_scope  VARCHAR(20)     NOT NULL COMMENT 'HQ/CENTRAL_KITCHEN/STORE',
  merchant_id  BIGINT UNSIGNED NULL COMMENT 'owner_scope=STORE 时门店商户',
  name         VARCHAR(128)    NOT NULL,
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (warehouse_id),
  KEY idx_tenant_scope (tenant_id, owner_scope),
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- v1 偏离：用代理主键 stock_id + 唯一键 (warehouse_id, sku_id)（10 文档复合主键 MP 支持弱）
CREATE TABLE sku_stock (
  stock_id      BIGINT UNSIGNED NOT NULL,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  warehouse_id  BIGINT UNSIGNED NOT NULL,
  sku_id        BIGINT UNSIGNED NOT NULL COMMENT '原材料/半成品/成品 SKU',
  quantity      DECIMAL(14,4)   NOT NULL DEFAULT 0 COMMENT '结存数量（POS核减允许临时为负）',
  unit          VARCHAR(20)     NULL COMMENT '计量单位 kg/份/箱',
  reorder_point DECIMAL(14,4)   NULL COMMENT '预警阈值',
  version       INT             NOT NULL DEFAULT 0 COMMENT '乐观锁',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (stock_id),
  UNIQUE KEY uk_warehouse_sku (warehouse_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dish_bom (
  bom_id          BIGINT UNSIGNED NOT NULL,
  tenant_id       BIGINT UNSIGNED NOT NULL,
  output_sku_id   BIGINT UNSIGNED NOT NULL COMMENT '产出 SKU（菜品/半成品/成品）',
  output_quantity DECIMAL(14,4)   NOT NULL DEFAULT 1,
  materials       JSON            NOT NULL COMMENT '[{material_sku_id, quantity, unit}]',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (bom_id),
  UNIQUE KEY uk_output_sku (output_sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 事件消费去重（10 文档 9.1）。无 tenant_id 列，已在 kernel TenantLineHandlerImpl 中排除。
CREATE TABLE processed_event (
  consumer_group VARCHAR(64)  NOT NULL,
  event_id       VARCHAR(64)  NOT NULL,
  event_type     VARCHAR(64)  NOT NULL,
  processed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (consumer_group, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

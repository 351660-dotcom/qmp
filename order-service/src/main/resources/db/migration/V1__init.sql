-- 订单编排域表结构（10 文档 6.1 / 6.2 / 9.1）。
-- 偏离说明：order_item.order_item_id 用 VARCHAR(64)（形如 OI-...）而非 10 文档的 BIGINT，
-- 以与 inventory.reservation_id / ticket_credential.order_item_id 的字符串约定一致；
-- 新增 verified_count 用于凭证核销聚合。详见本服务 CLAUDE.md。

CREATE TABLE trade_order (
  order_id      BIGINT UNSIGNED NOT NULL,
  tenant_id     BIGINT UNSIGNED NOT NULL,
  scenic_id     BIGINT UNSIGNED NOT NULL,
  merchant_id   BIGINT UNSIGNED NOT NULL,
  user_id       BIGINT UNSIGNED NOT NULL,
  status        VARCHAR(20)     NOT NULL COMMENT 'PENDING_PAYMENT/PAID/CANCELLED/CLOSED',
  total_amount  DECIMAL(10,2)   NOT NULL,
  paid_amount   DECIMAL(10,2)   NOT NULL DEFAULT 0,
  refund_amount DECIMAL(10,2)   NOT NULL DEFAULT 0,
  pay_expire_at DATETIME        NOT NULL,
  payment_id    VARCHAR(64)     NULL,
  version       INT             NOT NULL DEFAULT 0,
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (order_id),
  KEY idx_tenant_user (tenant_id, user_id, created_at),
  KEY idx_pay_expire_scan (status, pay_expire_at) COMMENT '超时关单定时任务扫描'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_item (
  order_item_id          VARCHAR(64)     NOT NULL COMMENT '同时作为inventory.reservation_id（v1用VARCHAR）',
  tenant_id              BIGINT UNSIGNED NOT NULL,
  order_id               BIGINT UNSIGNED NOT NULL,
  sku_id                 BIGINT UNSIGNED NOT NULL,
  sale_date              DATE            NOT NULL,
  time_slot_id           BIGINT UNSIGNED NOT NULL DEFAULT 0,
  quantity               INT             NOT NULL,
  unit_price             DECIMAL(10,2)   NOT NULL,
  subtotal               DECIMAL(10,2)   NOT NULL,
  refund_policy_snapshot JSON            NOT NULL,
  verified_count         INT             NOT NULL DEFAULT 0 COMMENT 'v1扩展：已核销凭证数，TicketVerified自增',
  created_at             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (order_item_id),
  KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 事件消费去重表（10 文档 9.1）。无 tenant_id 列，已在 kernel TenantLineHandlerImpl 中排除。
CREATE TABLE processed_event (
  consumer_group VARCHAR(64)  NOT NULL COMMENT '如 order-service-payment-consumer',
  event_id       VARCHAR(64)  NOT NULL,
  event_type     VARCHAR(64)  NOT NULL,
  processed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (consumer_group, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

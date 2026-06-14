-- 核验中心表结构（10 文档 7.1 / 7.2 / 9.1）。
-- 注：ticket_credential 在 10 文档基础上增加 v1 扩展列（order_id/scenic_id/sale_date/unit_price/
-- payment_id/refund_policy_snapshot），且 order_item_id 用 VARCHAR(64) 而非 BIGINT
-- （与 inventory.reservation_id 约定一致）。详见本服务 CLAUDE.md「v1 扩展决策」。

CREATE TABLE ticket_credential (
  credential_id         BIGINT UNSIGNED NOT NULL,
  tenant_id             BIGINT UNSIGNED NOT NULL,
  order_id              BIGINT UNSIGNED NOT NULL COMMENT 'v1扩展：所属订单，TicketVerified分区key',
  order_item_id         VARCHAR(64)     NOT NULL COMMENT '= inventory.reservation_id（v1用VARCHAR）',
  ticket_index          INT             NOT NULL COMMENT '第几张票，1..quantity',
  verify_code           VARCHAR(255)    NOT NULL COMMENT '签名串，支持离线验签',
  status                VARCHAR(20)     NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED/VERIFIED/REFUNDING/REFUNDED/VOIDED/EXPIRED',
  scenic_id             BIGINT UNSIGNED NOT NULL COMMENT 'v1扩展：冗余景区ID，写入verification_record',
  sale_date             DATE            NOT NULL COMMENT 'v1扩展：游玩日期，可退窗口计算',
  unit_price            DECIMAL(10,2)   NOT NULL COMMENT 'v1扩展：单票价，退款金额计算',
  payment_id            VARCHAR(64)     NOT NULL COMMENT 'v1扩展：所属支付单，退票回调payment用',
  refund_policy_snapshot JSON           NULL COMMENT 'v1扩展：退改规则快照{type,cutoff_hours,refund_ratio}',
  verified_at           DATETIME        NULL,
  verify_terminal_id    VARCHAR(64)     NULL,
  created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (credential_id),
  UNIQUE KEY uk_order_item_index (order_item_id, ticket_index) COMMENT '出票幂等：同一明细同一张票只生成一次',
  UNIQUE KEY uk_verify_code (verify_code),
  KEY idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE verification_record (
  verification_id BIGINT UNSIGNED NOT NULL,
  tenant_id       BIGINT UNSIGNED NOT NULL,
  credential_id   BIGINT UNSIGNED NOT NULL,
  scenic_id       BIGINT UNSIGNED NOT NULL COMMENT '冗余存储，在园人数聚合免join',
  terminal_id     VARCHAR(64)     NOT NULL,
  result          VARCHAR(20)     NOT NULL COMMENT 'SUCCESS/ALREADY_VERIFIED/EXPIRED/NOT_VERIFIABLE',
  verified_at     DATETIME        NOT NULL,
  PRIMARY KEY (verification_id),
  KEY idx_credential (credential_id),
  KEY idx_scenic_date (scenic_id, verified_at) COMMENT '在园人数统计聚合'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 事件消费去重表（10 文档 9.1）。无 tenant_id 列，已在 kernel TenantLineHandlerImpl 中排除。
CREATE TABLE processed_event (
  consumer_group VARCHAR(64)  NOT NULL COMMENT '如 ticket-verification-refund-consumer',
  event_id       VARCHAR(64)  NOT NULL,
  event_type     VARCHAR(64)  NOT NULL,
  processed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (consumer_group, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

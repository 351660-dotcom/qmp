-- 营销中心 v1：优惠券模板 + 实例（13 文档四）。

CREATE TABLE coupon_template (
  template_id       BIGINT UNSIGNED NOT NULL,
  tenant_id         BIGINT UNSIGNED NOT NULL,
  name              VARCHAR(128)    NOT NULL,
  coupon_type       VARCHAR(20)     NOT NULL COMMENT 'FULL_REDUCTION/DISCOUNT/EXCHANGE',
  face_value        DECIMAL(10,2)   NULL COMMENT '满减面额',
  discount_rate     DECIMAL(5,4)    NULL COMMENT '折扣率',
  applicable_scope  JSON            NULL COMMENT '商品/商户范围',
  valid_period_rule JSON            NULL COMMENT '领取后N天/指定起止',
  issue_quota       INT             NULL COMMENT '发放总量上限，NULL 不限',
  status            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (template_id),
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE coupon_instance (
  coupon_id   BIGINT UNSIGNED NOT NULL,
  tenant_id   BIGINT UNSIGNED NOT NULL,
  template_id BIGINT UNSIGNED NOT NULL,
  user_id     BIGINT UNSIGNED NOT NULL,
  status      VARCHAR(20)     NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED/USED/EXPIRED',
  issued_at   DATETIME        NOT NULL,
  used_at     DATETIME        NULL,
  order_id    BIGINT UNSIGNED NULL COMMENT '核销时关联订单',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (coupon_id),
  KEY idx_user_status (user_id, status),
  KEY idx_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE member_account (
  user_id      BIGINT UNSIGNED NOT NULL COMMENT '与C端用户账号一致',
  tenant_id    BIGINT UNSIGNED NOT NULL,
  is_member    TINYINT(1)      NOT NULL DEFAULT 0,
  member_since DATETIME        NULL,
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

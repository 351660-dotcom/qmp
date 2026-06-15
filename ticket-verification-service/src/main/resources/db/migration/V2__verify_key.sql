-- verify_code 按租户/景区独立密钥 + 轮换：每个景区一组带版本的签名密钥。
-- 签发时用该景区 ACTIVE 密钥，verify_code 内嵌 kid(=verify_key.id)；验签按 kid 取密钥，
-- 轮换后旧密钥置 RETIRED 仍保留（旧码仍可离线验签），新码用新 ACTIVE 密钥。未配置景区密钥时回落全局密钥。
CREATE TABLE verify_key (
  id          BIGINT UNSIGNED NOT NULL COMMENT '雪花ID，即 verify_code 内嵌的 kid',
  tenant_id   BIGINT UNSIGNED NOT NULL,
  scenic_id   BIGINT UNSIGNED NOT NULL,
  key_version INT             NOT NULL,
  secret      VARCHAR(128)    NOT NULL,
  status      VARCHAR(20)     NOT NULL COMMENT 'ACTIVE/RETIRED',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_scenic_version (tenant_id, scenic_id, key_version),
  KEY idx_active (tenant_id, scenic_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

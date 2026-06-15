-- 核销规则之「核销介质」：产品支持的核销介质列表（如 ["QR_CODE","IC_CARD","FACE"]）。
-- 与已有 valid_period_rule（有效期）共同构成核销规则；供核验终端/前端识别可用介质。
ALTER TABLE ticket_product
  ADD COLUMN verification_medium JSON NULL COMMENT '核销介质列表 如 ["QR_CODE","IC_CARD","FACE"]' AFTER refund_policy;

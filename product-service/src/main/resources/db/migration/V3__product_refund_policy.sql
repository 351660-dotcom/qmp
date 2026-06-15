-- 退改签规则按产品配置（后台每景区可为不同产品设置）：产品上挂退改规则快照，
-- order 下单时取该产品的 refund_policy 落到 order_item.refund_policy_snapshot（替代硬编码默认值）。
-- 形如 {"type":"TIERED","cutoff_hours":24,"refund_ratio":0.8}；NONE 表示不可退。
ALTER TABLE ticket_product
  ADD COLUMN refund_policy JSON NULL COMMENT '退改签规则快照' AFTER real_name_rule;

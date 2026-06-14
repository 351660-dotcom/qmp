-- 黄金路径示例数据，与 docs/09-门票全链路服务契约.md 示例保持一致
INSERT INTO ticket_product (
  product_id, tenant_id, scenic_id, merchant_id, name, description,
  status, valid_period_rule, real_name_rule
) VALUES (
  9001, 1001, 3001, 2001, '示例景区门票', '黄金路径集成测试用产品',
  'ON_SALE', JSON_OBJECT('type', 'FIXED_DATE', 'valid_days', 1), 'ONE_TICKET_ONE_ID'
);

INSERT INTO ticket_sku (
  sku_id, tenant_id, product_id, ticket_type, requires_time_slot, time_slot_definitions
) VALUES (
  1001, 1001, 9001, 'ADULT', 1, JSON_ARRAY('09:00-11:00', '11:00-13:00')
);

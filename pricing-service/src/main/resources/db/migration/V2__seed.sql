-- 黄金路径示例数据，与 docs/09-门票全链路服务契约.md 示例保持一致（sku_id=1001, sale_date=2026-07-01, MEMBER price=88.00）
INSERT INTO price_calendar (price_calendar_id, tenant_id, sku_id, sale_date, price_type, price) VALUES
  (100101, 1001, 1001, '2026-07-01', 'RETAIL', 98.00),
  (100102, 1001, 1001, '2026-07-01', 'MEMBER', 88.00);

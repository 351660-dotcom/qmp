-- 黄金路径示例数据：sku_id=1001 / sale_date=2026-07-01 的两个场次（与 docs/09 文档示例 time_slot_id=1 一致）
INSERT INTO inventory_bucket (
  bucket_id, tenant_id, scenic_id, sku_id, sale_date, time_slot_id,
  total_quota, sold_count, locked_count, channel_quota
) VALUES
  (300101, 1001, 3001, 1001, '2026-07-01', 1, 1000, 0, 0, JSON_OBJECT('direct', 1000)),
  (300102, 1001, 3001, 1001, '2026-07-01', 2, 1000, 0, 0, JSON_OBJECT('direct', 1000));

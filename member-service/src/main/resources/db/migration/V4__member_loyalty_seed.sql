-- 会员等级示例数据（tenant 1001）。等级 ID 用固定值便于演示（生产由后台创建分配雪花 ID）。
INSERT INTO member_level (level_id, tenant_id, level_name, min_growth_value, benefits) VALUES
  (5001, 1001, '普通会员', 0,    JSON_OBJECT('point_rate', 1.0)),
  (5002, 1001, '银卡会员', 1000, JSON_OBJECT('point_rate', 1.2)),
  (5003, 1001, '金卡会员', 5000, JSON_OBJECT('point_rate', 1.5));

-- 既有会员 user_id=123 归入普通会员档位。
UPDATE member_account SET level_id = 5001 WHERE user_id = 123;

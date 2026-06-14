-- 黄金路径示例数据：user_id=123 为会员，与 docs/09-门票全链路服务契约.md 示例（is_member=true）一致
INSERT INTO member_account (user_id, tenant_id, is_member, member_since) VALUES
  (123, 1001, 1, '2025-01-01 00:00:00');

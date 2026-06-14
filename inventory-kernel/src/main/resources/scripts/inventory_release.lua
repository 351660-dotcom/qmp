-- 库存释放/回补：取消、超时释放、退票时把数量还回 Redis 余量（见 06 文档五）
-- KEYS[1]: 余量 key，约定 inv:{bucket_id}:remain
-- ARGV[1]: 回补数量
-- 返回值：回补后的余量
local qty = tonumber(ARGV[1])

return redis.call('INCRBY', KEYS[1], qty)

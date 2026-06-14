-- 库存预占：防超卖第一道防线（见 06 文档一/07 文档 1.3）
-- KEYS[1]: 余量 key，约定 inv:{bucket_id}:remain
-- ARGV[1]: 预占数量
-- 返回值：扣减后剩余量（>=0 表示成功）；-1 表示余量不足，拒绝
local remain = tonumber(redis.call('GET', KEYS[1]) or '0')
local qty = tonumber(ARGV[1])

if remain < qty then
    return -1
end

return redis.call('DECRBY', KEYS[1], qty)

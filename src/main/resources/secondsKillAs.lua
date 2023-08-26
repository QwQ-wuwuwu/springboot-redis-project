--参数列表
--优惠卷id
local couponId = ARGV[1]
--用户id
local userId = ARGV[2]
--订单id
local couponOrderId = ARGV[3]

-- 数据key
-- 库存key
local couponKey = 'coupon:' .. couponId
-- 订单key
local couponOrderKey = 'couponOrder:' .. couponId

-- 判断库存是否充足
if (tonumber(redis.call('get',couponKey)) <= 0) then
    -- 库存不足，返回1
    return 1
end

-- 判断用户是否已经下单
if (redis.call('sismember', couponOrderKey, userId) == 1) then
    -- 存在，说明重复下单，返回2
    return 2;
end

-- 优惠卷数量减一，并保存用户
redis.call('incrby',couponKey,-1)
redis.call('sadd',couponOrderKey,userId)
-- 成功，返回0

--发送消息到stream消息阻塞队列
redis.call('xadd','stream.orders','*','userId',userId,'couponId',couponId,'id',couponOrderId)
return 0;

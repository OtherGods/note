我们的项目中，在做库存扣减的时候，先基于 Lua 脚本和 Redis 实现库存的预扣减的，这样可以在秒杀扣减时候确保操作的原子性和高效性。

库存扣减部分的 lua 脚本代码如下：
```lua
String luaScript = """
        if redis.call('hexists', KEYS[2], ARGV[2]) == 1 then
            return redis.error_reply('OPERATION_ALREADY_EXECUTED')
        end

        local current = redis.call('get', KEYS[1])
        if current == false then
            return redis.error_reply('KEY_NOT_FOUND')
        end
        if tonumber(current) == nil then
            return redis.error_reply('current value is not a number')
        end
        if tonumber(current) < tonumber(ARGV[1]) then
            return redis.error_reply('INVENTORY_NOT_ENOUGH')
        end
                        
        local new = tonumber(current) - tonumber(ARGV[1])
        redis.call('set', KEYS[1], tostring(new))
                        
        local time = redis.call("time")
        local currentTimeMillis = (time[1] * 1000) + math.floor(time[2] / 1000)
                        
        redis.call('hset', KEYS[2], ARGV[2], cjson.encode({
            action = "increase",
            from = current,
            to = new,
            change = ARGV[1],
            by = ARGV[2],
            timestamp = currentTimeMillis
        }))
                        
        return new
        """;
```

如果你看不懂，给大家把详细的注释写上：
1. `KEYS[1]` = 库存的 key，示例：`clc:inventory:10010`
2. `KEYS[2]` = 流水的 key，示例：`clc:inventory:stream:10010`
3. `ARGV[1]` = 本次要扣减的库存数，示例：`1`
4. `AGRV[2]` = 本次扣减的唯一编号，示例：`DECREASE_1019593009082742333440003`

```lua
String luaScript = """
    -- 检查操作是否已经执行过，通过检查哈希表(KEYS[2])中是否存在标识(ARGV[2])
    if redis.call('hexists', KEYS[2], ARGV[2]) == 1 then
        -- 如果存在，返回错误信息'OPERATION_ALREADY_EXECUTED'
        return redis.error_reply('OPERATION_ALREADY_EXECUTED')
    end

    -- 获取当前库存值
    local current = redis.call('get', KEYS[1])
    -- 如果库存键不存在，返回错误信息'KEY_NOT_FOUND'
    if current == false then
        return redis.error_reply('KEY_NOT_FOUND')
    end
    -- 如果当前值不是数字，返回错误信息'current value is not a number'
    if tonumber(current) == nil then
        return redis.error_reply('current value is not a number')
    end
    -- 如果当前库存小于请求的扣减数量，返回错误信息'INVENTORY_NOT_ENOUGH'
    if tonumber(current) < tonumber(ARGV[1]) then
        return redis.error_reply('INVENTORY_NOT_ENOUGH')
    end

    -- 计算新的库存值
    local new = tonumber(current) - tonumber(ARGV[1])
    -- 设置新的库存值
    redis.call('set', KEYS[1], tostring(new))

    -- 获取Redis服务器的当前时间（time[1]秒和time[2]微秒）
    local time = redis.call("time")
    -- 转换为毫秒级时间戳
    local currentTimeMillis = (time[1] * 1000) + math.floor(time[2] / 1000)

    -- 使用哈希结构存储操作日志
    redis.call('hset', KEYS[2], ARGV[2], cjson.encode({
        action = "increase",  -- 操作类型：增加
        from = current,       -- 操作前的库存值
        to = new,             -- 操作后的库存值
        change = ARGV[1],     -- 变化量
        by = ARGV[2],         -- 操作标识
        timestamp = currentTimeMillis  -- 操作时间戳
    }))

    -- 返回新的库存值
    return new
""";
```

也就是说，我们的 lua 脚本一共干了这么几件事：
1. 幂等校验、合法性检验：判断消息是否重复、库存是否存在、是否足够等等
3. 库存的原子性扣减
4. 记录一条库存扣减流水

在这个 lua 执行成功的时候，就会**保证==库存能够被扣减成功==，并且==记录了一条流水==**。如果运行失败，那么会根据他的返回的错误异常信息，我们对应的返回给调用方不同的错误信息。比如是库存不足、还是库存不存在等等。

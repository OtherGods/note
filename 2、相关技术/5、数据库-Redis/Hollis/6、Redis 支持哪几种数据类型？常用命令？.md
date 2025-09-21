#reids数据结构 #String #Hash #List #Set #Zset 

# 1、常用命令

- `keys *`：查看当前库所有key    (匹配：`keys *1`)
- `exists key`：判断某个key是否存在 存在返回1，不存在返回0；
- `type key`：查看你的key是什么类型
- `del key`：删除指定的key数据
- `unlink key`：根据value选择非阻塞删除
  仅将keys从keyspace元数据中删除，真正的删除会在后续异步操作。
- `expire key 10`：10秒钟，为给定的key设置过期时间
- `ttl key`：查看还有多少秒过期，-1表示永不过期，-2表示已过期
- `select`：命令切换数据库
- `dbsize`：查看当前数据库的key的数量
- `flushdb`：清空当前库
- `flushall`通杀全部库

# 2、数据类型

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509202147318.png)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509202148323.png)

Redis中以键值对维护映射关系，其中key是`String`类型，value有多种数据类型；Redis 中支持了多种数据类型，其中比较常用的有五种：
1. 字符串（String）：是 *==二进制安全的==*，string可以包含任何数据，比如jpg图片或者序列化的对象，*==最多可以是512M，一般设置几M==*；内部为当前字符串实际分配的空间capacity一般要高于实际字符串长度len，当字符串长度小于1M时，扩容都是加倍现有的空间，如果超过1M，扩容时一次只会多扩1M的空间
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509202211848.png)
   - **==简单动态字符串（SDS）==**，类似于Java中`ArrayList`
   - 常用命令：
	   - `set key value`（不存在key则 **==创建==**，存在则 **==修改(覆盖)==**）
	   - `setex key seconds value`（不存在key则 **==创建==**，存在则 **==修改(覆盖)==** key对应value的过期时间,以秒为单位）
	   - `setnx key value`（不存在key则 **==创建==**，成功返回1，失败返回0）
	   - `mset key value [key value ...]`（同时设置一个或多个 key-value 对，不存在key则 **==创建==**，存在则 **==修改(覆盖)==**）
	   - `mget key [key ...]`（同时获取一个或多个key）
	   - `incrby key increment`（不存在key则 **==创建==**，存在则 **==修改(覆盖)==**，原子增加指定值并返回）
	   - `decrby key decrement`（不存在key则 **==创建==**，存在则 **==修改(覆盖)==**，原子减少指定值并返回）
	   - `append key value`（不存在key则 **==创建==**，存在则 **==修改(追加到末尾)==**，返回字符串你长度）
	   - `strlen key`（返回字符串长度，key不存在返回0）
	   - `del key [key ...]`（**==删除==** 并返回被删除的数量）
	   - `get key`（**==查==**，若不存在返回nil，否则返回对应的值）
	   - `getrange key start end`（获取start到end闭区间内的数据，-1为最后一个字符，-2为倒数第二个字符）
2. 哈希（Hash）：是键值对的集合
   - **==压缩列表==**、**==哈希表==**，类似于Java中的`Map`
   - 常用命令：
	   - `hset key field value`（不存在该key则 **==创建==**，存在则 **==增加或修改(覆盖)==**，如果是新增field且成功则返回1，如果是修改已存在的field则返回0）
	   - `hmset key field value [field value ...]`（不存在该key则创建，存在则 **==增加或修改(覆盖)==**，成功返回ok）
	   - `hsetnx key field value`（不存在该key则 **==创建==**，key存在但field不存在则设置指定value，并返回1，若field存在则返回0）
	   - `hincrby key field increment`（不存在该key则 **==创建==**，key存在但field不存在则创建，field存在则原子给value增加increment并返回）
	     - 若对应的value为字符串，则在执行时会出错
	   - `hdel key field [field ...]`（**==删除==** 哈希结构中的一个或多个指定field，返回被删除的field数量）
	   - `hget key field`（**==查询==** key中指定field对应的value，不存在则返回nil）
	   - `hexists key field`（**==查询==** 哈希表key中fIeld是否存在，存在返回1，否则返回0）
	   - `hkeys key`（返回key下所有的field）
	   - `hvals key`（返回key下所有field的value）

3. 列表（List）：是 *==简单的字符串列表==*，按照插入顺序排序，可以添加一个元素到列表的头部（左边）或者尾部（右边）
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509202236701.png)
   - **==压缩列表==**、**==双向链表==** 
   - 常用命令：
	   - `L/R push key value [value ...]`(不存在key则 **==创建==**，存在则向表头/尾 **==插入==** 多个数据，返回列表长度)
	   - `L/R pop key`(**==删除==** 表头/尾并返回，不存在返回nil)
	   - `Lrem key count value`（根据参数 count 的值，移除列表中与参数 value 相等的元素，`count>0`则从表头开始删count个元素，`count<0`则从表尾开始删abs(count)个元素）
	   - `Lset key index value`（将列表 key 下标为 index 的元素的值 **==修改==** 为 value）
	     - 当 index 参数超出范围，或对一个空列表( key 不存在)进行 Lset 时，返回一个错误
	   - `Lindex key index`（按照索引下标从0开始从左到右 **==获取==** 元素，不存在返回nil）
	   - `Lrange key start stop`（从左到右按照索引下标获得元素，-1代表最右边的元素）
	   - `Llen`（返回列表的长度）
4. 集合（Set）：自动去重，无序，value可为null
   - **==哈希表==**、**==整数数组==**
   - 常用命令：
	   - `sadd key member [member ...]`（不存在该key则创建，存在则 **==增加==**，已存在于`Set`中的元素将被忽略，返回被添加到Set中的元素）
	   - `srem key member [member ...]`（**==删除==** 集合中的一个或多个元素，返回被删除的数量）
	   - `spop key`（移除并返回集合中的一个随机元素，，*==值在键在值光键亡==*）
	   - `scard key`（返回集合中元素数量，key不存在返回0）
	   - `sismember key member`（**==查询==** 元素是否在集合中，存在返回1，不存在返回0）
	   - `smembers key`（返回key对应的所有value）
	   - `sinter key [key ...]`（交集，返回给定集合们的交集）
	   - `sunion key [key ...]`（并集，返回给定集合们的并集）
	   - `sdiff key [key ...]`（差集，返回给定集合们的差集）
5. 有序集合（Sorted Set）：自动去重，有序；每个member都有一个score，对应的值可以是整数值或双精度浮点数
   - **==压缩列表==**、**==哈希表==**、**==跳表==**
   - 常用命令：
	   - `zadd key score member [[score member] [score member] ...]`（不存在该key则创建，存在则 **==增加或更新==** 这个member的score值，返回被添加成功的数量不包含更新的数量）
	   - `zincrby key increment member`（不存在该key则 **==创建==**，key存在但member不存在则创建，member存在则原子给score增加increment并返回）
	   - `zrem key member [member ...]`（**==删除==** member，不存在的被忽略，返回被成功删除的数量）
	   - `zremrangebyscore key min max`（**==删除==** Zset中score在闭区间min到max内的值，并返回被删除的数量）
	   - `zcard key`（查询并返回指定key中member的数量）
	   - `zrange start stop [WITHSCORES]`（**==查询==** 按score递增返回指定区间内的Zset，可带score）
	   - `zrevrange start stop [WITHSCORES]`（与`zrange`类似，按照递减返回）
	   - `zrangebyscore key min max [WITHSCORES] [LIMIT offset count]`（**==查询==** 按score递增返回指定区间内的Zset，可带score，LIMIT 参数指定返回结果的数量及区间，就像SQL中的`LIMIT offset,count`）
	   - `zrevrangebyscore key max min [WITHSCORES] [LIMIT offset count]`（与`zrangebyscore`类似，按递减返回）
	   - `zcount key min max`（返回key中，score值在闭区间min和max之间的成员的数量）

另外，Redis中还支持一些高级的数据类型，如：Streams、Bitmap、Geospatial以及HyperLogLog

# 3、字符串

[7、Redis为什么要自己定义SDS](2、相关技术/5、数据库-Redis/Hollis/7、Redis为什么要自己定义SDS.md)

# 4、有序集合

[8、Redis中的Zset是怎么实现的](2、相关技术/5、数据库-Redis/Hollis/8、Redis中的Zset是怎么实现的.md)

# 5、Streams

[15、Redis 5.0中的 Stream是什么](2、相关技术/5、数据库-Redis/Hollis/15、Redis%205.0中的%20Stream是什么.md)

# 6、GEO

[9、什么是GEO，有什么用](2、相关技术/5、数据库-Redis/Hollis/9、什么是GEO，有什么用.md)

# 7、扩展知识
## 7.1、使用场景

[11、Redis的zset实现排行榜，实现分数相同按照时间顺序排序，怎么做](8、场景题/11、Redis的zset实现排行榜，实现分数相同按照时间顺序排序，怎么做.md)

[10、如何用Redis实现朋友圈点赞功能](8、场景题/10、如何用Redis实现朋友圈点赞功能.md)

[12、如何实现查找附近的人功能](8、场景题/12、如何实现查找附近的人功能.md)

[41、如何基于Redis实现滑动窗口限流](2、相关技术/5、数据库-Redis/Hollis/41、如何基于Redis实现滑动窗口限流.md)








## 使用

在电商场景中，商品查询是非常高频的，而且也是非常重要的。而且对于商品信息来说，很多东西都是变化不频繁的，除了库存。

所以，在商品搜索这里，我们其实是做了缓存的。包括库存。

上面不是说库存频繁变化么？频繁变化也能走缓存吗？因为我们的库存这里是先在缓存做的预扣减，所以所有变化 redis 是都可以感知到的，所以可以走缓存查询。

在缓存的实现上，我们是基于 `Redis` 做分布式缓存，并且本地缓存使用 `Caffeine`，二者组成了二级缓存，并且使用 `JetCache` 做二级缓存的管理。

以下是我们的商品详情的查询:
```java
public SingleResponse<CollectionVO> queryById(Long collectionId) {  
    CollectionVO collectionVO = CollectionConvertor.INSTANCE.mapToVo(collectionService.queryById(collectionId));  
      
    CollectionInventoryRequest request = new CollectionInventoryRequest();  
    request.setCollectionId(collectionId.toString());  
    Integer inventory = collectionInventoryRedisService.getInventory(request);  
      
    if (inventory <= 0) {  
        collectionVO.setState(CollectionVoState.SOLD_OUT);  
    }  
      
    collectionVO.setInventory(inventory.longValue());  
    return SingleResponse.of(collectionVO);  
}
```

这里主要是先调用`collectionService`查询商品信息，然后再查询 `Redis` 中的库存信息，二者结合后返回给前端。

而`collectionService`的查询，我们是用了二级缓存的，把商品`id` 当作缓存的 `key`，并且在 **==本地存储10分钟==**，**==分布式缓存中存60分钟==**，主要是本地内存资源有限，不太热的数据就可以让他淘汰掉。

并且我们为了提升缓存的命中了，设置了 **==50分钟更新一次==**，也就意味着，Redis 中的数据是不太会过期的，因为还没等到60分钟的过期时间，就自动刷新了。

因为商品信息其实变化根本就不太频繁，所以设置个50分钟更新一次即可。

那为啥 Redis 还要设置60分钟的过期时间呢？为了避免万一定时任务执行的时候应用重启了，导致数据没更新。
```java
@Cached(name = ":collection:cache:id:", expire = 60,localExpire = 10, timeUnit = TimeUnit.MINUTES, cacheType = CacheType.BOTH, key = "#collectionId", cacheNullValue = true)  
@CacheRefresh(refresh = 50, timeUnit = TimeUnit.MINUTES)  
public Collection queryById(Long collectionId) {  
    return getById(collectionId);  
}
```

后续，等我们的后台功能上了之后，商品信息修改的时候，会让缓存失效。这样下次查询的时候就可以把新值更新进去了。

## ChatGPT解释

### 配置总览

这个配置定义了一个两级（本地 + 远程）缓存，其行为如下：
1. **缓存位置** (`CacheType.BOTH`)： 同时使用本地缓存 (如 Caffeine) 和远程缓存 (如 Redis)。
2. **键生成**： Redis 中的 Key 将是 `{keyPrefix}:collection:cache:id:{collectionId的值}`。
3. **空值缓存** (`cacheNullValue = true`)： 即使方法返回 `null`，也会将这个 `null` 结果缓存起来，防止缓存穿透。

### 重点一：过期时间 (Expire)

过期时间指的是缓存数据的**生存时间 (TTL - Time To Live)**。一旦超过这个时间，数据就会被自动**删除或视为无效**。

在你的配置中，有两个独立的过期时间，因为使用了 `BOTH` 类型：

|参数|解释|在你的配置中|
|---|---|---|
|**`expire`**|**远程缓存** (如 Redis) 的过期时间。|`60` `TimeUnit.MINUTES`  <br>**含义：Redis 中的数据在 60 分钟后会过期并被自动删除。**|
|**`localExpire`**|**本地缓存** (如 Caffeine) 的过期时间。|`10` `TimeUnit.MINUTES`  <br>**含义：本地内存中的数据在 10 分钟后会过期并被自动删除。**|

**为什么两个时间不同？**  
这是一种常见的优化策略。本地缓存的生命周期通常设置得比远程缓存更短，主要是为了保障**集群环境下的数据一致性**。当某个节点更新了数据并清除了 Redis 中的缓存后，其他节点本地内存中的旧数据最多只存在 10 分钟，之后就会因过期而失效，从而从 Redis 获取新的数据。这在一定程度上平衡了性能和一致性。

**过期后的行为：**  
当下一个请求到来时，如果发现缓存已过期（不存在），会**同步地**执行被注解的方法（例如查询数据库），获取到新数据后重新填充缓存，然后返回结果。这个过程会导致请求的响应时间增加。

### 重点二：刷新时间 (Refresh)

刷新时间指的是缓存数据的**自动刷新时间**。这是 JetCache 的一个高级特性，用于解决“击穿”问题和提升性能。

在你的配置中：

|参数|解释|在你的配置中|
|---|---|---|
|**`@CacheRefresh(refresh)`**|**自动刷新**的时间间隔。|`refresh = 50`, `timeUnit = TimeUnit.MINUTES`  <br>**含义：Redis 中的数据在加载成功后，**50 分钟**后会触发一次自动刷新。**|

**刷新后的行为：**  
当到达刷新时间点时，JetCache 会**异步地**（默认开启 refreshThreadPool）执行被注解的方法（例如查询数据库），获取最新数据并更新到 Redis 中。

**关键区别：过期 vs. 刷新**

|特性|**过期 (Expire)**|**刷新 (Refresh)**|
|---|---|---|
|**触发时机**|缓存存活时间 **> `expire`**|缓存存活时间 **> `refresh`**|
|**执行方式**|**同步**|**异步**|
|**对用户的影响**|用户请求会**阻塞等待**新数据加载完成，**响应变慢**|用户请求**无感知**，直接返回**即将过期的旧数据**，后台默默更新|
|**数据状态**|缓存被删除，数据不存在|缓存数据**仍然存在**，但会被标记为需要刷新|
|**主要目的**|保证数据的最终一致性，防止使用过于陈旧的数据|防止缓存同时失效导致大量请求击穿到数据库，实现**“准实时”更新**|

### 整体工作流程与时间线 (Timeline)

假设你的 `collectionId = 100` 的数据在 **00:00** 被首次缓存。

1. **00:00 - 00:50**：
    - 所有请求优先命中本地缓存（10分钟过期）或 Redis 缓存（60分钟过期）。
    - 这是缓存的有效期，性能最佳。
2. **00:10**：
    - 本地缓存 (`localExpire=10`) 数据过期。下一个请求会无法命中本地缓存，转而查询 Redis。
    - Redis 中数据依然存在，所以会取回数据并**重新填充本地缓存**，然后返回结果。
3. **00:50** (**关键节点**)：
    - 刷新时间 (`refresh=50`) 已到。
    - JetCache 会**异步**地发起一个请求去执行原始方法（如查询数据库）。
    - **对于正在到来的用户请求**：它仍然会得到 **00:00 时刻的旧数据**，但系统不会阻塞。
    - **在后台**：新的查询结果会更新到 **Redis** 中（Key 的过期时间会重置为新的 60 分钟）。
    - **注意**：**本地缓存不会自动更新**，它会在自己过期（10分钟）后，下一次请求时从已刷新的 Redis 中获取新数据。
4. **01:00**：
    - Redis 中原有的旧数据本应过期 (`expire=60`)，但由于在 00:50 时发生了刷新，**过期时间被重置了**。所以此时数据仍然存在且有效（新的过期时间变成了 01:50）。
5. **01:50**：
    - 下一次刷新周期开始。

### 总结与最佳实践提醒

- **`expire`**：定义了数据的**最大生命周期**，保证数据不会无限期陈旧。
- **`refresh`**：定义了数据的**自动更新频率**，用于在用户无感的情况下提前更新数据，避免请求阻塞。
- **黄金法则**：**必须确保 `refresh` 时间小于 `expire` 时间**（在你的配置中 50 < 60，这是正确的）。如果 `refresh >= expire`，缓存会在触发刷新前就过期失效，从而使刷新功能失去意义。
- **本地缓存**：`localExpire` 是一个独立于这个刷新周期的生命周期，主要目的是在多节点间更快地淘汰可能过期的数据。

你的这个配置是一个非常经典的“**异步刷新+多级过期**”策略，能在保证数据相对新鲜的同时，极大地提升系统的响应速度和抗压能力。

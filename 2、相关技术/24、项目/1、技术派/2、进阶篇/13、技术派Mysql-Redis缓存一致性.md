

这篇文章，从理论到实战，告诉大家如何去保证 MySQL 和 Redis 的一致性。

根据网上的众多解决方案，我们总结出 6 种：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311042207270.png)


# 1、不好的方案
## 1.1、先写MySQL，再写Redis
(A写B写)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311042219800.png)

图解说明：
- 这是一张时序图，描述请求的先后调用顺序
- 两个请求都是在修改同一个数据，只是修改时间不同
- 橘色的线是请求A，黑色的线条是请求B
- 橘色的文字，是MySQL和Redis最终不一致的数据
- 数据是从10更新为11
- 后面所有的图都是这个含义，不再赘述

请求A、B都是先写MySQL，然后再写Redis，在高并发的情况下，如果请求A在写Redis时卡了一会，请求B已经依次完成数据的更新，就会出现图中的问题。

这个图已经画的很清晰了，我就不用再去啰嗦了吧，不过这里有个前提，就是对于读请求，先去读 Redis，如果没有，再去读 DB，但是读请求不会再回写 Redis。 大白话说一下，就是读请求不会更新 Redis。

## 1.2、先写Redis，再写MySQL
(A写B写)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311042222811.png)

同“先写 MySQL，再写 Redis”，看图可秒懂。
## 1.3、先删除Redis，再写MySQL
(A写B读)
这幅图和上面有些不一样，前面的请求 A 和 B 都是更新请求，这里的请求 A 是更新请求，但是请求 B 是读请求，且请求 B 的读请求会回写 Redis。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311042225220.png)

请求A先删除缓存，可能因为卡顿，数据一直没有更新到MySQL，导致两者数据不一致。

**这种情况出现的概率比较大，因为请求A更新MySQL可能耗时会比较长，而请求B的前两步都是查询，会非常快**

# 2、好的方案
## 2.1、先删Redis，再写MySQL，再删Redis
(A写B读)
对于 “先删Redis，再写MySQL”，如果瑶解决最后的不一致问题，其实再对Redis重新删除即可，这个也是大家常说的“缓存双删”
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311042229792.png)

为了便于大家看图，对于蓝色的文字，“删除缓存 10”必须在“回写缓存10”后面，那如何才能保证一定是在后面呢？**网上给出的第一个方案是，让请求 A 的最后一次删除，等待 500ms**。

对于这种方案，看看就行，反正我是不会用，太 Low 了，风险也不可控。

**那有没有更好的方案呢，我建议异步串行化删除，即删除请求入队列**
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311042305767.png)

异步删除对线上业务无影响，串行化处理保障并发情况下正确删除。

如果双删失败怎么办，网上有给 Redis 加一个缓存过期时间的方案，这个不敢苟同。个人建议整个重试机制，可以借助消息队列的重试机制，也可以自己整个表，记录重试次数，方法很多。

简单小结一下：
- “缓存双闪”不要无脑的sleep 500 ms
- 通过消息队列的异步 & 串行，实现最后一次缓存删除
- 缓存删除失效，增加重试机制【我认为是在 + 500ms的条件下在增加重试机制】

## 2.2、先写MySQL，再删Redis

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311042310307.png)

对于上面这种情况，对于第一次查询，请求 B 查询的数据是 10，但是 MySQL 的数据是 11，只存在这一次不一致的情况，对于不是强一致性要求的业务，可以容忍。（那什么情况下不能容忍呢，比如秒杀业务、库存服务等。）

当请求 B 进行第二次查询时，因为没有命中 Redis，会重新查一次 DB，然后再回写到 Reids。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311042325536.png)

这里需要满足 2 个条件：
- 缓存刚好自动失效；
- 请求 B 从数据库查出 10，回写缓存的耗时，比请求 A 写数据库，并且删除缓存的还长。

对于第二个条件，我们都知道更新 DB 肯定比查询耗时要长，所以出现这个情况的概率很小，同时满足上述条件的情况更小。

我个人认为，作者的这种写法包含在2.1的情况中，实时一致性更弱

## 2.3、先写MySQL，通过Binlog，异步更新Redis

这种方案，主要是监听 MySQL 的 Binlog，然后通过异步的方式，将数据更新到 Redis，这种方案有个前提，查询的请求，不会回写 Redis。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311042329411.png)

这个方案，会保证 MySQL 和 Redis 的最终一致性，但是如果中途请求 B 需要查询数据，如果缓存无数据，就直接查 DB；如果缓存有数据，查询的数据也会存在不一致的情况。

所以这个方案，是实现最终一致性的终极解决方案，但是不能保证实时性（感觉作者说的这种方案没啥好处）。

# 3、几种方案比较

我们对比上面讨论的 6 种方案：
1. 先写Redis，再写MySQL
	1. 这种方案，我肯定不会用，万一 DB 挂了，你把数据写到缓存，DB 无数据，这个是灾难性的；
	2. 我之前也见同学这么用过，如果写 DB 失败，对 Redis 进行逆操作，那如果逆操作失败呢，是不是还要搞个重试？
2. 先写 MySQL，再写 Redis
	1. **对于并发量、一致性要求不高的项目，很多就是这么用的**，我之前也经常这么搞，但是不建议这么做；
	2. 当 Redis 瞬间不可用的情况，需要报警出来，然后线下处理。
3. 先删除 Redis，再写 MySQL
	1. 这种方式，我还真没用过，直接忽略吧。
4. 先删除 Redis，再写 MySQL，再删除 Redis
	1. 这种方式虽然可行，但是感觉好复杂，还要搞个消息队列去异步删除 Redis。
	2. 这个方案，我个人认为是实时性中最好的方案，在一些高并发场景中，但是作者推荐第5种。
5. 先写 MySQL，再删除 Redis
	1. 比较推荐这种方式，删除 Redis 如果失败，可以再多重试几次，否则报警出来；
6. 先写 MySQL，通过 Binlog，异步更新 Redis
	1. 对于异地容灾、数据汇总等，建议会用这种方式，比如 binlog + kafka，数据的一致性也可以达到秒级；
	2. 纯粹的高并发场景，不建议用这种方案，比如抢购、秒杀等。

**个人结论**
- **实时一致性方案**：采用“先写 MySQL，再删除 Redis”的策略，这种情况虽然也会存在两者不一致，但是需要满足的条件有点苛刻，所以是满足实时性条件下，能尽量满足一致性的最优解。
- **最终一致性方案**：采用“先写 MySQL，通过 Binlog，异步更新 Redis”，可以通过 Binlog，结合消息队列异步更新 Redis，是最终一致性的最优解。


# 4、项目实战
## 5.1、数据更新

因为项目对实时性要求较高，且为了演示简单，所以采用方案 5，先写 MySQL，再删除 Redis 的方式。

下面只是一个示例，我们将文章的标签放入 MySQL 之后，再删除 Redis，所有涉及到 DB 更新的操作都需要按照这种方式处理。

这里加了一个事务，如果 Redis 删除失败，MySQL 的更新操作也需要回滚，避免查询时读取到脏数据。
```java
@Override  
@Transactional(rollbackFor = Exception.class)  
public void saveTag(TagReq tagReq) {

	TagDO tagDO = ArticleConverter.toDO(tagReq);
	
	// 先写 MySQL
	if (NumUtil.nullOrZero(tagReq.getTagId())) {  
	    tagDao.save(tagDO);  
	} else {  
	    tagDO.setId(tagReq.getTagId());  
	    tagDao.updateById(tagDO);  
	}  
	  
	// 再删除 RedisString redisKey = CACHE_TAG_PRE + tagDO.getId();  
	RedisClient.del(redisKey);
}

@Override  
@Transactional(rollbackFor = Exception.class)  
public void deleteTag(Integer tagId) {  
    TagDO tagDO = tagDao.getById(tagId);  
    if (tagDO != null){  
        // 先写 MySQL
        tagDao.removeById(tagId);  
        
        // 再删除 Redis
        String redisKey = CACHE_TAG_PRE + tagDO.getId();  
        RedisClient.del(redisKey);  
    }  
}

@Override  
public void operateTag(Integer tagId, Integer pushStatus) {  
    TagDO tagDO = tagDao.getById(tagId);  
    if (tagDO != null){  
  
        // 先写 MySQL        tagDO.setStatus(pushStatus);  
        tagDao.updateById(tagDO);  
  
        // 再删除 Redis        String redisKey = CACHE_TAG_PRE + tagDO.getId();  
        RedisClient.del(redisKey);  
    }  
}
```

## 5.2、数据获取

这个也很简单，先查询缓存，如果有就直接返回；如果未查询到，需要先查询 DB ，再写入缓存。

我们放入缓存时，加了一个过期时间，用于兜底，万一两者不一致，缓存过期后，数据会重新更新到缓存。
```java
@Override  
public TagDTO getTagById(Long tagId) {  
  
    String redisKey = CACHE_TAG_PRE + tagId;  
  
    // 先查询缓存，如果有就直接返回  
    String tagInfoStr = RedisClient.getStr(redisKey);  
    if (tagInfoStr != null && !tagInfoStr.isEmpty()) {  
        return JsonUtil.toObj(tagInfoStr, TagDTO.class);  
    }  
  
    // 如果未查询到，需要先查询 DB ，再写入缓存  
    TagDTO tagDTO = tagDao.selectById(tagId);  
    tagInfoStr = JsonUtil.toStr(tagDTO);  
    RedisClient.setStrWithExpire(redisKey, tagInfoStr, CACHE_TAG_EXPRIE_TIME);  
  
    return tagDTO;  
}
```

## 5.3、测试用例

```java
/**  
 * @author Louzai  
 * @date 2023/5/5  
 */@Slf4j  
public class MysqlRedisService extends BasicTest {  
  
    @Autowired  
    private TagSettingService tagSettingService;  
  
    @Test  
    public void save() {  
        TagReq tagReq = new TagReq();  
        tagReq.setTag("Java");  
        tagReq.setTagId(1L);  
        tagSettingService.saveTag(tagReq);  
        log.info("save success:{}", tagReq);  
    }  
  
    @Test  
    public void query() {  
        TagDTO tagDTO = tagSettingService.getTagById(1L);  
        log.info("query tagInfo:{}", tagDTO);  
    }  
}
```

我们看一下 Redis：
```shell
127.0.0.1:6379> get pai_cache_tag_pre_1
"{\"tagId\":1,\"tag\":\"Java\",\"status\":1,\"selected\":null}"
```

以及结果输出：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311042351735.png)

大家也可以直接下载技术派项目，里面都有代码和测试用例哈，如果有问题，也可以评论给我们留言，欢迎一起讨论！















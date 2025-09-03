
通过技术派进行发文的小伙伴应该注意到了，发的文章会先进行审核，只有审核通过之后才会在网站上进行展示。那么是不是所有的作者发文都需要审核呢？

答案当然是否定的。我们做了一个白名单，在白名单中的用户发文之后不需要进入审核，可以直接上线。

看到这里自然会有几个疑问

**为什么要审核？**

如后台的文章列表
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312104995.png)

大部分的小伙伴发布的文章还是很值得点赞、推荐的；但是也会有很多小伙伴在体验技术派这个项目的过程中，只是单纯的体验下发文的过程，所以对应的文章内容可能只是测试内容，若不经过审核即直接上线，则会导致整个社区的文章质量下滑

其次就是一个正常的文章交流社区，审核当然是标配；这里还涉及到不少相关知识点，我们怎么可以省略掉呢O(∩_∩)O~

**白名单是怎么实现的？**
对于作者添加白名单，有很多可选的方案：
1. 配置文件写死（相当于硬编码的方式）
	1. 优点：简单
	2. 缺点：不灵活
2. 数据库配置一个白名单表
	1. 优点：灵活，适用性强
	2. 缺点：实现优点重
3. 基于redis的set实现白名单
	1. 优点：实现简单，轻量
	2. 缺点：依赖redis

技术派中的白名单就是基于redis的set来实现的，接下来我们看一下详细的实现策略

# 1、redis实现白名单

对于使用redis来实现白名单，最容易想到的数据结构就是set （题外话，关于redis的string,list,set,zset,hash五种数据结构，各自的应用场景你能想出几个?）

## 1.1、set基本知识点

接下来我们先熟悉一下redis中的set的相关命令操作

**添加**
```sql
# 向集合中添加多个成员
sadd key val1 val2
```

**集合数量**
```sql
scard key
```

**判断集合是否包含元素**
```sql
// 返回1表示在里面，0表示不在里面
sismember key val
```

**返回集合所有成员**
```sql
# 向集合中添加多个成员
sadd key val1 val2
```

**随机移除集合中的一个元素**
```sql
spop key
```

**随机移除集合中的几个元素**
```sql
srandmember key count
```

**删除集合中成员**
```sql
srem key val
```

一个简单的实例演示如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312127125.png)

集合除了上面的基本操作之外，还支持多集合之间的相互操作。

**返回第一个集合与其他集合之间的差异**
```sql
sdiff key1 key2 key3...
```

**返回所有给定集合的差值，并存储在destination**
```sql
sdiffstore destination key1 key2 key3...
```

**返回给定集合的交集**
```sql
sinter key1 key2
```

**返回所有给定集合的并集**
```sql
sunion key1 key2...
```

**返回所有给定集合的并集，并存储在sestination集合中**
```sql
sunionstore destination key1 key2...
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312131743.png)

## 1.2、RedisTemplate操作知识点

Spring项目中，借助RedisTemplate操作set也非常简单

### 1.2.1、新增
```java
/**
 * 新增一个  sadd
 *
 * @param key
 * @param value
 */
public void add(String key, String value) {
    redisTemplate.opsForSet().add(key, value);
}
```

### 1.2.2、删除
```java
/**
 * 删除集合中的值  srem
 *
 * @param key
 * @param value
 */
public void remove(String key, String value) {
    redisTemplate.opsForSet().remove(key, value);
}
```

### 1.2.3、判断是否存在
```java
/**
 * 判断是否包含  sismember
 *
 * @param key
 * @param value
 */
public void contains(String key, String value) {
    redisTemplate.opsForSet().isMember(key, value);
}
```

### 1.2.4、获取所有的value
```java
/**
 * 获取集合中所有的值 smembers
 *
 * @param key
 * @return
 */
public Set<String> values(String key) {
    return redisTemplate.opsForSet().members(key);
}
```

### 1.2.5、集合运算
```java
/**
 * 返回多个集合的并集  sunion
 *
 * @param key1
 * @param key2
 * @return
 */
 public Set<String> union(String key1, String key2) {
    return redisTemplate.opsForSet().union(key1, key2);
}


/**
 * 返回多个集合的交集 sinter
 *
 * @param key1
 * @param key2
 * @return
 */
public Set<String> intersect(String key1, String key2) {
    return redisTemplate.opsForSet().intersect(key1, key2);
}


/**
 * 返回集合key1中存在，但是key2中不存在的数据集合  sdiff
 *
 * @param key1
 * @param key2
 * @return
 */
public Set<String> diff(String key1, String key2) {
    return redisTemplate.opsForSet().difference(key1, key2);
}
```


## 1.3、白名单使用实例

白名单的相关业务封装在 `ArticleWhiteListService` 中
```java
/**
 * 判断作者是否再文章发布的白名单中；
 * 这个白名单主要是用于控制作者发文章之后是否需要进行审核
 *
 * @param authorId
 * @return
 */
boolean authorInArticleWhiteList(Long authorId);


/** 
 * 获取所有的白名单用户
 * 
 * @return
 */
List<BaseUserInfoDTO> queryAllArticleWhiteListAuthors();


/**
 * 将用户添加到白名单中
 *
 * @param userId
 */
void addAuthor2ArticleWhitList(Long userId);


/**
 * 从白名单中移除用户
 *
 * @param userId
 */
void removeAuthorFromArticelWhiteList(Long userId);
```

核心主要使用的就是第一个，判断作者是否在白名单用户中；剩下的三个用于管理员后台维护白名单使用，具体实现如下：
```java
/**
 * 实用 redis - set 来存储允许直接发文章的白名单
 */
private static final String ARTICLE_WHITE_LIST = "auth_article_white_list";

@Autowired
private UserService userService;

@Override
public boolean authorInArticleWhiteList(Long authorId) {
    return RedisClient.sIsMember(ARTICLE_WHITE_LIST, authorId);
}
```

对于set操作我们进行了统一的封装，和上面介绍到的知识点差不多，不过这里选择的是另一种实现策略：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312210497.png)

核心封装的几个公共方法，大家也可以直接在源码中获取
>com.github.paicoding.forum.core.cache.RedisClient#sIsMember

```java
/**
 * 判断value是否再set中
 *
 * @param key
 * @param value
 * @return
 */
public static <T> Boolean sIsMember(String key, T value) {
    return template.execute(new RedisCallback<Boolean>() {
        @Override
        public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
            return connection.sIsMember(keyBytes(key), valBytes(value));
        }
    });
}


/**
 * 获取set中的所有内容
 *
 * @param key
 * @param clz
 * @param <T>
 * @return
 */
public static <T> Set<T> sGetAll(String key, Class<T> clz) {
    return template.execute(new RedisCallback<Set<T>>() {
        @Override
        public Set<T> doInRedis(RedisConnection connection) throws DataAccessException {
            Set<byte[]> set = connection.sMembers(keyBytes(key));
            if (CollectionUtils.isEmpty(set)) {
                return Collections.emptySet();
			}
			return set.stream().map(s -> toObj(s, clz)).collect(Collectors.toSet());
		}
	});
}


/**
 * 往set中添加内容
 *
 * @param key
 * @param val
 * @param <T>
 * @return
 */
 public static <T> boolean sPut(String key, T val) {
    return template.execute(new RedisCallback<Long>() {
        @Override
        public Long doInRedis(RedisConnection connection) throws DataAccessException {
            return connection.sAdd(keyBytes(key), valBytes(val));
        }
    }) > 0;
}


/**
 * 移除set中的内容
 *
 * @param key
 * @param val
 * @param <T>
 */
 public static <T> void sDel(String key, T val) {
    template.execute(new RedisCallback<Void>() {
        @Override
        public Void doInRedis(RedisConnection connection) throws DataAccessException {
            connection.sRem(keyBytes(key), valBytes(val));
            return null;
        }
    });
}
```

接下来我们再看一下具体的使用场景，在文章发布的核心service中：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312228098.png)

- 对于非白名单的用户，若操作的是上线的文章，则需要进入审核
如发布文章：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312233235.png)

如更新一个在线的文章：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312236923.png)

以上是使用白名单的场景，当然对于管理员而言，还有操作白名单的入口，统一收拢在 `ArticleWhiteListContoller` 。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312237479.png)


## 1.4、小结

本片文章的整体内容都比较简单，核心的思考点就是基于白名单这个场景，给大家普及一下redis中set的具体使用姿势

通常来讲，redis的五种基本数据结构属于必须掌握的知识点；但是就我这段时间面试超过三位数的经验来看，差不多有80%以上的人，无法给每种数据结构找一个适用的应用场景，说实话这让我有些意外；光背知识点用处并不大，关键要结合实际场景来选型，这样才能加深你的知识点储备

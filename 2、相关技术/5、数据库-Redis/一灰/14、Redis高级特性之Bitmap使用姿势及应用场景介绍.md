
前面介绍过redis的五种基本数据结构，如String，List, Set, ZSet, Hash，这些属于相对常见了；在这些基本结果之上，redis还提供了一些更高级的功能，如geo, bitmap, hyperloglog，pub/sub，本文将主要介绍Bitmap的使用姿势以及其适用场景，主要知识点包括

- bitmap 基本使用
- 日活统计应用场景中bitmap使用姿势
- 点赞去重应用场景中bitmap使用姿势
- 布隆过滤器bloomfilter基本原理及体验case

# 1、基本使用
## 1.1、配置

我们使用SpringBoot `2.2.1.RELEASE`来搭建项目环境，直接在`pom.xml`中添加redis依赖
```xml
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-data-redis</artifactId>  
</dependency>
```

如果我们的redis是默认配置，则可以不额外添加任何配置；也可以直接在`application.yml`配置中，如下
```yml
spring:  
  redis:  
    host: 127.0.0.1  
    port: 6379  
    password
```

## 1.2、使用姿势

bitmap主要就三个操作命令，`setbit`，`getbit`以及 `bitcount`

### 1.2.1、设置标记

即`setbit`，主要是指将某个索引，设置为1(设置0表示抹去标记)，基本语法如下
```shell
# 请注意这个index必须是数字，后面的value必须是0/1  
setbit key index 0/1
```

对应的SpringBoot中，借助RestTemplate可以比较容易的实现，通常有两种写法，都可以
```java
@Autowired  
private StringRedisTemplate redisTemplate;  
  
/**  
 * 设置标记位  
 *  
 * @param key  
 * @param offset  
 * @param tag  
 * @return  
 */  
public Boolean mark(String key, long offset, boolean tag) {  
    return redisTemplate.opsForValue().setBit(key, offset, tag);  
}  
  
public Boolean mark2(String key, long offset, boolean tag) {  
    return redisTemplate.execute(new RedisCallback<Boolean>() {  
        @Override  
        public Boolean doInRedis(RedisConnection connection) throws DataAccessException {  
            return connection.setBit(key.getBytes(), offset, tag);  
        }  
    });  
}
```

上面两种写法的核心区别，就是key的序列化问题，第一种写法使用默认的jdk字符串序列化，和后面的`getBytes()`会有一些区别，关于这个，有兴趣的小伙伴可以看一下我之前的博文: [4、Spring之RedisTemplate配置与使用](2、相关技术/5、数据库-Redis/一灰/4、Spring之RedisTemplate配置与使用.md)

### 1.2.2、判断存在与否

即 `getbit key index`，如果返回1，表示存在否则不存在
```java
/**  
 * 判断是否标记过  
 *  
 * @param key  
 * @param offest  
 * @return  
 */  
public Boolean container(String key, long offest) {  
    return redisTemplate.opsForValue().getBit(key, offest);  
}
```

### 1.2.3、计数

即 `bitcount key`，统计和
```java
/**  
 * 统计计数  
 *  
 * @param key  
 * @return  
 */  
public long bitCount(String key) {  
    return redisTemplate.execute(new RedisCallback<Long>() {  
        @Override  
        public Long doInRedis(RedisConnection redisConnection) throws DataAccessException {  
            return redisConnection.bitCount(key.getBytes());  
        }  
    });  
}
```

## 1.3、应用场景

前面的基本使用比较简单，在介绍String数据结构的时候也提过，我们重点需要关注的是bitmap的使用场景，它可以干嘛用，什么场景下使用它会有显著的优势

- 日活统计
- 点赞
- bloomfilter

上面三个场景虽有相似之处，但实际的应用场景还是些许区别，接下来我们逐一进行说明

### 1.3.1、日活统计

统计应用或网站的日活，这个属于比较常见的case了，如果是用redis来做这个事情，首先我们最容易想到的是Hash结构，一般逻辑如下

- 根据日期，设置key，如今天为 `2020/10/13`, 那么key可以为 `app_20_10_13`
- 其次当用户访问时，设置field为userId, value设置为true
- 判断日活则是统计map的个数`hlen app_20_10_13`

上面这个逻辑有毛病么？当然没有问题，但是想一想，当我们的应用做的很nb的时候，每天的日活都是百万，千万级时，这个内存开销就有点吓人了

接下来我们看一下bitmap可以怎么做

- 同样根据日期设置key
- 当用户访问时，index设置为userId，`setbit app_20_10_13 uesrId 1`
- 日活统计 `bitcount app_20_10_13`

**简单对比一下上面两种方案**

当数据量小时，且userid分布不均匀，小的为个位数，大的几千万，上亿这种，使用bitmap就有点亏了，因为userId作为index，那么bitmap的长度就需要能容纳最大的userId，但是实际日活又很小，说明bitmap中间有大量的空白数据

反之当数据量很大时，比如百万/千万，userId是连续递增的场景下，bitmap的优势有两点：1.存储开销小， 2.统计总数快

### 1.3.2、点赞

点赞的业务，最主要的一点是一个用户点赞过之后，就不能继续点赞了（当然某些业务场景除外），所以我们需要知道是否可以继续点赞

上面这个hash当然也可以实现，我们这里则主要讨论一下bitmap的实现逻辑

- 比如我们希望对一个文章进行点赞统计，那么我们根据文章articleId来生成redisKey=`like_1121`，将userId作为index
- 首先是通过`getbit like_1121 userId` 来判断是否点赞过，从而限制用户是否可以操作

Hash以及bitmap的选择和上面的考量范围差不多

### 1.3.3、布隆过滤器bloomfilter

布隆过滤器可谓是大名鼎鼎了，我们这里简单的介绍一下这东西是啥玩意

- 底层存储为一个bitmap
- 当来一个数据时，经过n个hash函数，得到n个数值
- 将hash得到的n个数值，映射到bitmap，标记对应的位置为1

如果来一个数据，通过hash计算之后，若这个n个值，对应的bitmap都是1，那么表示这个数据可能存在；如果有一个不为1，则表示这个数据一定不存在

**请注意：不存在时，是一定不存在；存在时，则不一定**

从上面的描述也知道，bloomfilter的底层数据结构就是bitmap，当然它的关键点在hash算法；根据它未命中时一定不存在的特性，非常适用于缓存击穿的问题解决

**体验说明**

Redis的布隆过滤器主要针对>=4.0，通过插件的形式提供，项目源码地址为: [https://github.com/RedisBloom/RedisBloom](https://github.com/RedisBloom/RedisBloom)，下面根据readme的说明，简单的体验一下redis中bloomfilter的使用姿势
```shell
# docker 方式安装  
docker run -p 6379:6379 --name redis-redisbloom redislabs/rebloom:latest  
  
# 通过redis-cli方式访问  
docker exec -it redis-redisbloom bash  
  
# 开始使用  
# redis-cli  
127.0.0.1:6379> keys *  
(empty array)  
127.0.0.1:6379> bf.add newFilter hello  
(integer) 1  
127.0.0.1:6379> bf.exists newFilter hello  
(integer) 1  
127.0.0.1:6379> bf.exists newFilter hell  
(integer) 0
```

bloomfilter的使用比较简单，主要是两个命令`bf.add`添加元素，`bf.exists`判断是否存在，请注意它没有删除哦

## 1.4、小结

bitmap位图属于一个比较精巧的数据结构，通常在数据量大的场景下，会有出现的表现效果；redis本身基于String数据结构来实现bitmap的功能支持，使用方式比较简单，基本上就下面三个命令

- `setbit key index 1/0`: 设置
- `getbit key index`: 判断是否存在
- `bitcount key`: 计数统计

本文也给出了bitmap的三个常见的应用场景

- 日活统计：主要借助`bitcount`来获取总数（后面会介绍，在日活十万百万以上时，使用hyperLogLog更优雅）
- 点赞: 主要借助`setbit/getbit`来判断用户是否赞过，从而实现去重
- bloomfilter: 基于bitmap实现的布隆过滤器，广泛用于去重的业务场景中（如缓存穿透，爬虫url去重等）

总的来讲，bitmap属于易用，巧用的数据结构，用得好即能节省内存也可以提高效率，用得不好貌似也不会带来太大的问题

# 2、其他
## 2.1、项目

工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)



转载自：[【DB系列】Redis高级特性之Bitmap使用姿势及应用场景介绍](https://spring.hhui.top/spring-blog/2020/10/14/201014-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8BRedis%E9%AB%98%E7%BA%A7%E7%89%B9%E6%80%A7%E4%B9%8BBitmap%E4%BD%BF%E7%94%A8%E5%A7%BF%E5%8A%BF%E5%8F%8A%E5%BA%94%E7%94%A8%E5%9C%BA%E6%99%AF%E4%BB%8B%E7%BB%8D/)


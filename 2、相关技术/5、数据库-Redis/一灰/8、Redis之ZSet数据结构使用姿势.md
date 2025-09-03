
Redis的五大数据结构，目前就剩下最后的ZSET，可以简单的理解为带权重的集合；与前面的set最大的区别，就是每个元素可以设置一个score，从而可以实现各种排行榜的功能

# 1、基本使用

在开始之前，序列化的指定需要额外处理，上一篇已经提及，相关内容可以参考：
- [5、Redis之List数据结构使用姿势](2、相关技术/5、数据库-Redis/一灰/5、Redis之List数据结构使用姿势.md)

## 1.1、新增元素

新增元素时，用起来和set差不多，无非是多一个score的参数指定而已

如果元素存在，会用新的score来替换原来的，返回0；如果元素不存在，则会会新增一个
```java
/**  
 * 添加一个元素, zset与set最大的区别就是每个元素都有一个score，因此有个排序的辅助功能;  zadd  
 *  
 * @param key  
 * @param value  
 * @param score  
 */  
public void add(String key, String value, double score) {  
    redisTemplate.opsForZSet().add(key, value, score);  
}
```

## 1.2、删除元素

删除就和普通的set没啥区别了
```java
/**  
 * 删除元素 zrem  
 *  
 * @param key  
 * @param value  
 */  
public void remove(String key, String value) {  
    redisTemplate.opsForZSet().remove(key, value);  
}
```

## 1.3、修改score

zset中的元素塞入之后，可以修改其score的值，通过 `zincrby` 来对score进行加/减；当元素不存在时，则会新插入一个

从上面的描述来看，`zincrby` 与 `zadd` 最大的区别是前者是增量修改；后者是覆盖score方式
```java
/**  
 * score的增加or减少 zincrby  
 *  
 * @param key  
 * @param value  
 * @param score  
 */  
public Double incrScore(String key, String value, double score) {  
    return redisTemplate.opsForZSet().incrementScore(key, value, score);  
}
```

## 1.4、获取value对应的score

这个需要注意的是，当value在集合中时，返回其score；如果不在，则返回null
```java
/**  
 * 查询value对应的score   zscore  
 *  
 * @param key  
 * @param value  
 * @return  
 */  
public Double score(String key, String value) {  
    return redisTemplate.opsForZSet().score(key, value);  
}
```

## 1.5、获取value在集合中排名

前面是获取value对应的score；这里则是获取排名；这里score越小排名越高;

从这个使用也可以看出结合4、5, 用zset来做排行榜可以很简单的获取某个用户在所有人中的排名与积分
```java
/**  
 * 判断value在zset中的排名  zrank  
 *  
 * @param key  
 * @param value  
 * @return  
 */  
public Long rank(String key, String value) {  
    return redisTemplate.opsForZSet().rank(key, value);  
}
```

## 1.6、集合大小

```java
/**  
 * 返回集合的长度  
 *  
 * @param key  
 * @return  
 */  
public Long size(String key) {  
    return redisTemplate.opsForZSet().zCard(key);  
}
```

## 1.7、获取集合中数据

因为是有序，所以就可以获取指定范围的数据，下面有两种方式

- 根据排序位置获取数据
- 根据score区间获取排序位置
```java
/**  
 * 查询集合中指定顺序的值， 0 -1 表示获取全部的集合内容  zrange  
 *  
 * 返回有序的集合，score小的在前面  
 *  
 * @param key  
 * @param start  
 * @param end  
 * @return  
 */  
public Set<String> range(String key, int start, int end) {  
    return redisTemplate.opsForZSet().range(key, start, end);  
}  
  
/**  
 * 查询集合中指定顺序的值和score，0, -1 表示获取全部的集合内容  
 *  
 * @param key  
 * @param start  
 * @param end  
 * @return  
 */  
public Set<ZSetOperations.TypedTuple<String>> rangeWithScore(String key, int start, int end) {  
    return redisTemplate.opsForZSet().rangeWithScores(key, start, end);  
}  
  
/**  
 * 查询集合中指定顺序的值  zrevrange  
 *  
 * 返回有序的集合中，score大的在前面  
 *  
 * @param key  
 * @param start  
 * @param end  
 * @return  
 */  
public Set<String> revRange(String key, int start, int end) {  
    return redisTemplate.opsForZSet().reverseRange(key, start, end);  
}  
  
/**  
 * 根据score的值，来获取满足条件的集合  zrangebyscore  
 *  
 * @param key  
 * @param min  
 * @param max  
 * @return  
 */  
public Set<String> sortRange(String key, int min, int max) {  
    return redisTemplate.opsForZSet().rangeByScore(key, min, max);  
}
```

# 2、其他
## 2.1、项目

工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)

来自：[【DB系列】Redis之ZSet数据结构使用姿势](https://spring.hhui.top/spring-blog/2018/12/12/181212-SpringBoot%E9%AB%98%E7%BA%A7%E7%AF%87Redis%E4%B9%8BZSet%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84%E4%BD%BF%E7%94%A8%E5%A7%BF%E5%8A%BF/)
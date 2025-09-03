
Redis的五大数据结构，前面讲述了String和List,Hash的使用姿势，接下来就是Set集合，与list最大的区别就是里面不允许存在重复的数据

# 1、基本使用

在开始之前，序列化的指定需要额外处理，上一篇已经提及，相关内容可以参考：
- [5、Redis之List数据结构使用姿势](2、相关技术/5、数据库-Redis/一灰/5、Redis之List数据结构使用姿势.md)

## 1.1、新增元素

新增元素时，可以根据返回值来判断是否添加成功, 如下面的单个插入时，如果集合中之前就已经有数据了，那么返回0，否则返回1
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

## 1.2、删除元素

因为list是有序的，所以在list的删除需要指定位置；而set则不需要
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

## 1.3、判断是否存在

set一个最大的应用场景就是判断某个元素是否有了，从而决定怎么执行后续的操作, 用的是 `isMember`方法，来判断集合中是否存在某个value
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

## 1.4、获取所有的value

set无序，因此像list一样获取某个范围的数据，不太容易，更常见的方式就是全部获取出来
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

## 1.5、集合运算

set还提供了另外几个高级一点的功能，就是集合的运算，如求并集，交集等操作，虽然在我有限的业务应用中，并没有使用到这些高级功能，下面依然个给出使用的姿势
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

# 2、其他
## 2.1、项目

工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)

来自：[【DB系列】Redis之Set数据结构使用姿势](https://spring.hhui.top/spring-blog/2018/12/11/181211-SpringBoot%E9%AB%98%E7%BA%A7%E7%AF%87Redis%E4%B9%8BSet%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84%E4%BD%BF%E7%94%A8%E5%A7%BF%E5%8A%BF/)
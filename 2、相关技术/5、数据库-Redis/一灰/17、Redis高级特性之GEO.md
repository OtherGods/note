
GEO用于存储地理信息，最直观的就是我们日常使用的地图app中，如果我想查询我所在地的周边餐饮，就可以利用geo中的以`(x,y)`为圆心，以n为半径，扫描坐标在这个圈内的所有餐饮店，这个case借助redis的geo可以很方便的实现

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
    password:
```

## 1.2、使用姿势

geo有6个常见的命令，下面逐一进行解释说明

### 1.2.1、geoadd 添加

```java
private final StringRedisTemplate redisTemplate;  
  
public GeoBean(StringRedisTemplate stringRedisTemplate) {  
    this.redisTemplate = stringRedisTemplate;  
}  
  
/**  
 * 添加geo信息  
 *  
 * @param key       缓存key  
 * @param longitude 经度  
 * @param latitude  纬度  
 * @param member    位置名  
 */  
public void add(String key, double longitude, double latitude, String member) {  
    // geoadd xhh_pos 114.31 30.52 武汉 116.46 39.92 北京  
    redisTemplate.opsForGeo().add(key, new Point(longitude, latitude), member);  
}
```

### 1.2.2、geopos 获取坐标

上面添加一组坐标 + 地理位置到redis中，如果我们想知道某个位置的坐标，则可以借助`geopos`来获取
```java
/**  
 * 获取某个地方的坐标  
 *  
 * @param key  
 * @param member  
 * @return  
 */  
public List<Point> get(String key, String... member) {  
    // geopos xhh_pos 武汉  
    List<Point> list = redisTemplate.opsForGeo().position(key, member);  
    return list;  
}
```

### 1.2.3、geodist 获取距离

计算两个位置之间的距离，比如我已经写入了武汉、北京的经纬度，这个时候希望知道他们两的距离，直接`geodist`即可
```java
/**  
 * 判断两个地点的距离  
 *  
 * @param key  
 * @param source  
 * @param dest  
 * @return  
 */  
public Distance distance(String key, String source, String dest) {  
    // 可以指定距离单位，默认是米, ft->英尺, mi->英里  
    // geodist xhh_pos 武汉 北京 km  
    return redisTemplate.opsForGeo().distance(key, source, dest);  
}
```

### 1.2.4、georadius 获取临近元素

georadius 以给定的经纬度为中心， 返回与中心的距离不超过给定最大距离的所有位置元素。
```java
public void near(String key, double longitude, double latitude) {  
    // georadius xhh_pos 114.31 30.52 5km  
    Circle circle = new Circle(longitude, latitude, 5 * Metrics.KILOMETERS.getMultiplier());  
    RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()  
            .includeDistance()  
            .includeCoordinates()  
            .sortAscending().limit(5);  
    GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()  
            .radius(key, circle, args);  
    System.out.println(results);  
}
```

### 1.2.5、georadiusbymember 获取临近元素

和上面的作用差不多，区别在于上面参数是经纬度，这里是位置
```java
public void nearByPlace(String key, String member) {  
    // georadiusbymember xhh_pos 武汉 1100 km  
    Distance distance = new Distance(5, Metrics.KILOMETERS);  
    RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()  
            .includeDistance()  
            .includeCoordinates()  
            .sortAscending()  
            .limit(5);  
    GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()  
            .radius(key, member, distance, args);  
    System.out.println(results);  
}
```

### 1.2.6、geohash

GeoHash将二维的经纬度转换成字符串，将二维的经纬度转换为一维的字符串，可以方便业务优化；geohash有自己的一套算法，这里不详细展开，有兴趣的小伙伴可以搜索一下
```java
public void geoHash(String key) {  
    // geohash xhh_pos 武汉  
    List<String> results = redisTemplate.opsForGeo()  
            .hash(key, "北京", "上海", "深圳");  
    System.out.println(results);  
}
```

## 1.3、小结

geo更适用于地图这种业务场景中，关于这块的业务没怎么接触过，也不太好确定诸如百度地图、高德地图这种是否有在真实业务中采用；如果我们把目标缩小一点，改成一个地下车库的导航，统计所在位置周边的空余车位，位置导航，停车位记录，感觉有点靠谱

注意上面的六个操作命令，没有删除，但如果我们错误的写入了一个数据，难道没法删除么？

- 使用 `zrem key member` 执行删除操作，如上面的case中，删除北京的坐标，可以: `zrem xhh_pos 北京`

为什么可以这么操作？

- geo的底层存储借助`ZSET`来实现的，因此zset的操作符都是支持的，geo添加的元素，会通过算法得出一个score，如上面case中的北京，武汉添加之后，zset值为
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202405232201919.png)

# 2、其他
## 2.1、项目

工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)


转载自：[【DB系列】Redis高级特性之GEO](https://spring.hhui.top/spring-blog/2020/10/27/201027-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8BRedis%E9%AB%98%E7%BA%A7%E7%89%B9%E6%80%A7%E4%B9%8BGEO/)


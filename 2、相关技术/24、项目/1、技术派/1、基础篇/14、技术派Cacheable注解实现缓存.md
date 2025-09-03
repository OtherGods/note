
对照：
- [16、技术派Caffeine整合本地缓存](2、相关技术/24、项目/1、技术派/1、基础篇/16、技术派Caffeine整合本地缓存.md)
- [17、技术派Caffeine整合本地缓存采坑实录](2、相关技术/24、项目/1、技术派/1、基础篇/17、技术派Caffeine整合本地缓存采坑实录.md)
- [1、SpringBoot缓存注解@Cacheable之自定义key策略及缓存失效时间指定](2、相关技术/16、常用框架-SpringBoot/补10_缓存注解@Cacheable等相关/1、SpringBoot缓存注解@Cacheable之自定义key策略及缓存失效时间指定.md)

- [20、缓存注解@Cacheable @CacheEvit @CachePut使用姿势介绍](2、相关技术/5、数据库-Redis/一灰/20、缓存注解@Cacheable%20@CacheEvit%20@CachePut使用姿势介绍.md)
- [21、SpringBoot缓存注解@Cacheable之自定义key策略及缓存失效时间指定](2、相关技术/5、数据库-Redis/一灰/21、SpringBoot缓存注解@Cacheable之自定义key策略及缓存失效时间指定.md)

在前面介绍到Caffeine的时候，我们主要结合Spring的@Cachebale注解来使用方法级别缓存的；当时的主题主要在Caffeine，接下来我们就重点看一下Spring中缓存注解的相关知识点

重点注意，Spring的缓存注解不限定底层缓存实现，可以是Caffeine也可以是redis、memcache、guava

---

Spring在3.1版本，就是提供了一条基于注解的缓存策略，在技术派中也实际使用到了这些

本文主要知识点：
- `@Cacheable`：缓存存在，则使用缓存；不存在，则执行方法，并将结果塞入缓存
- `@CacheEvict`：失效缓存
- `@CachePut`：更新缓存

# 1、使用配置
## 1.1、使用实例

我们针对侧边栏的服务加上了缓存，主要考虑点是出于最大缓存效果
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310051754037.png)

当然由于技术派中使用的场景不太多，在这盘教程中会对相关知识点进行扩展

## 1.2、核心配置

对缓存注解而言，单纯添加下面的依赖即可
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

但是需要注意的是具体的缓存实现也是不可缺少的，比如技术派选择Caffeine做本地缓存，因此还添加了
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

# 2、缓存注解介绍

> 关于注解的介绍，我会mock几个更简单好理解的例子，与技术派的具体实现会不太一致，请勿care；如有疑问，本地搞个demo工程跑一下是最好的选择

## 2.1、@Cacheable

这个注解用于修饰方法or类，当我们访问它修饰的方法时，优先从缓存中获取，若缓存中存在，则直接获取缓存的值；缓存不存在时，执行方法，并将结果写入缓存

这个注解，有两个比较核心的设置
```java
/**
与cacheNames效果等价
*/
@AliasFor("cacheNames")  
String[] value() default {};  
  
@AliasFor("value")  
String[] cacheNames() default {};  
  
/**
缓存key
*/
String key() default "";
```

cacheNames可以理解为缓存key的前缀，可以为组件缓存的key变量；当key不设置时，使用方法参数来初始化，注意key为SpEL表达式，因此如果要写字符串时，用单引号括起来

一个简单的使用姿势
```java
/**
 * 首先从缓存中查，查到之后，直接返回缓存数据；否则执行方法，并将结果缓存
 * <p>
 * redisKey: cacheNames + key 组合而成 --> 支持SpEL
 * redisValue: 返回结果
 *
 * @param name
 * @return
 */
@Cacheable(cacheNames = "say", key = "'p_'+ #name")
public String sayHello(String name) {
    return "hello+" + name + "-->" + UUID.randomUUID().toString();
}
```

如我们传参为 yihuihui, 那么缓存key为 `say::p_yihuihui`

除了上面三个配置值之外，查看 `@Cacheable` 注解源码的童鞋可以看到还有condition设置，这个表示当它设置的条件达成时，才写入缓存
```java
/**
 * 满足condition条件的才写入缓存
 *
 * @param age
 * @return
 */
@Cacheable(cacheNames = "condition", key = "#age", condition = "#age % 2 == 0")
public String setByCondition(int age) {
    return "condition:" + age + "-->" + UUID.randomUUID().toString();
}
```

上面这个case中，age为偶数的时候，才走缓存；否则不写缓存
接下来是unless参数，从名字上可以看出它表示不满足条件时才写入缓存
```java
/**
 * unless, 不满足条件才写入缓存
 *
 * @param age
 * @return
 */
@Cacheable(cacheNames = "unless", key = "#age", unless = "#age % 2 == 0")
public String setUnless(int age) {
    return "unless:" + age + "-->" + UUID.randomUUID().toString();
}
```

## 2.2、@CachePut

不管缓存有没有，都将方法的返回结果写入缓存；适用于缓存更新
```java
/**
 * 不管缓存有没有，都写入缓存
 *
 * @param age
 * @return
 */
@CachePut(cacheNames = "t4", key = "#age")
public String cachePut(int age) {
    return "t4:" + age + "-->" + UUID.randomUUID().toString();
}
```

## 2.3、@CacheEvict

这个就是我们理解的删除缓存
```java
/**
 * 失效缓存
 *
 * @param name
 * @return
 */
@CacheEvict(cacheNames = "say", key = "'p_'+ #name")
public String evict(String name) {
    return "evict+" + name + "-->" + UUID.randomUUID().toString();
}
```

## 2.4、@Caching

在实际的工作中，经常会遇到一个数据变动，更新多个缓存的场景，对于这个场景，可以通过@Caching来实现
```java
/**
 * caching实现组合，添加缓存，并失效其他的缓存
 *
 * @param age
 * @return
 */
@Caching(cacheable = @Cacheable(cacheNames = "caching", key = "#age"), evict = @CacheEvict(cacheNames = "t4", key = "#age"))
public String caching(int age) {
    return "caching: " + age + "-->" + UUID.randomUUID().toString();
}
```

上面这个就是组合操作
- 从 caching::age缓存取数据，不存在时执行方法并写入缓存；
- 失效缓存 t4::age

## 2.5、异常时，缓存会怎么样？

上面的几个case，都是正常的场景，当方法抛出异常时，这个缓存表现会怎样？
```java
/**
 * 用于测试异常时，是否会写入缓存
 *
 * @param age
 * @return
 */
@Cacheable(cacheNames = "exception", key = "#age")
@CacheEvict(cacheNames = "say", key = "'p_yihuihui'")
public int exception(int age) {
    return 10 / age;
}
```

根据实测结果，当 `age == 0` 时，上面两个缓存都不会成功
`@CacheEvict` 注解报错不清除缓存
`@CachePut` 注解也同样，报错不更新缓存

## 2.6、测试用例

接下来验证下缓存注解与上面描述的是否一致
```java
@RestController
public class IndexRest {
    @Autowired
    private BasicDemo helloService;

    @GetMapping(path = {"", "/"})
    public String hello(String name) {
        return helloService.sayHello(name);
    }
}
```

上面这个主要是验证@Cacheable注解，若缓存不命中，每次返回的结果应该都不一样，然而实际访问时，会发现返回的都是相同的
```shell
curl http://localhost:8080/?name=yihuihui
```

**失效缓存**
```java
@GetMapping(path = "evict")
public String evict(String name) {
    return helloService.evict(String.valueOf(name));
}
```

失效缓存，需要和上面的case配合起来使用
```shell
curl http://localhost:8080/evict?name=yihuihui
curl http://localhost:8080/?name=yihuihui
```

剩下其他的相关测试类就比较好理解了，一并贴出对应的代码
```java
@GetMapping(path = "condition")
public String t1(int age) {
    return helloService.setByCondition(age);
}

@GetMapping(path = "unless")
public String t2(int age) {
    return helloService.setUnless(age);
}

@GetMapping(path = "exception")
public String exception(int age) {
    try {
        return String.valueOf(helloService.exception(age));
    } catch (Exception e) {
        return e.getMessage();
    }
}

@GetMapping(path = "cachePut")
public String cachePut(int age) {
    return helloService.cachePut(age);
}
```

## 2.7、小结
### 2.7.1、示例源码

上面的测试用例源码可以在下面自助获取
- https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/125-cache-ano
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)

### 2.7.2、知识点小结

最后管理小结一下Spring提供的几个缓存注解
- `@Cacheable`：缓存存在，则从缓存取；否则执行方法，并将返回结果写入缓存
- `@CacheEvict`：失效缓存
- `@CachePut`：更新缓存
- `@Caching`：都注解组合

### 2.7.3、扩展知识点

还有一个知识点没有在上面列出来，当一个Service内的所有缓存方法的cacheNames都是同一个时，或者cacheManager都相同时，可以考虑在类上添加注解
```java
@Service
@CacheConfig(cacheNames = "customCache", cacheManager = "customCacheManager")
public class AnoCacheService {
  // ...
}
```

这样在方法内的@Cachebale等注解，就可以只指定 key 属性即可

### 2.7.4、灵魂拷问

上面虽说可以满足常见的缓存使用场景，但是有一个非常重要的点没有说明，缓存失效时间应该怎么设置？？？

在技术派这个项目中，缓存的整体配置都是由Caffeine的管理来处理的，核心定义在 ForumCoreAutoConfig
```java
@Bean("caffeineCacheManager")  
public CacheManager cacheManager() {  
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();  
    cacheManager.setCaffeine(Caffeine.newBuilder().  
            // 设置过期时间，写入后五分钟过期  
                    expireAfterWrite(5, TimeUnit.MINUTES)  
            // 初始化缓存空间大小  
            .initialCapacity(100)  
            // 最大的缓存条数  
            .maximumSize(200)  
    );  
    return cacheManager;  
}
```

但是需要注意一点，如果我是redis做缓存，两者结合，通常不同的缓存内容失效时间都是不一致的，那么这些失效时间可以怎么搞呢?

强烈建议大家思考一下，将你的解决方案贴在评论区吧
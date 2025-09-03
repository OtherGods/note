对照
- [16、技术派Caffeine整合本地缓存](2、相关技术/24、项目/1、技术派/1、基础篇/16、技术派Caffeine整合本地缓存.md)
- [14、技术派Cacheable注解实现缓存](2、相关技术/24、项目/1、技术派/1、基础篇/14、技术派Cacheable注解实现缓存.md)
- [17、技术派Caffeine整合本地缓存采坑实录](2、相关技术/24、项目/1、技术派/1、基础篇/17、技术派Caffeine整合本地缓存采坑实录.md)

上一篇博文介绍了Spring中缓存注解`@Cacheable` `@CacheEvit` `@CachePut`的基本使用，接下来我们将看一下更高级一点的知识点

- key生成策略
- 超时时间指定

# 1、项目环境
## 1.1、项目依赖

本项目借助`SpringBoot 2.2.1.RELEASE` + `maven 3.5.3` + `IDEA` + `redis5.0`进行开发

开一个web服务用于测试
```xml
<dependencies>  
    <dependency>  
        <groupId>org.springframework.boot</groupId>  
        <artifactId>spring-boot-starter-web</artifactId>  
    </dependency>  
    <dependency>  
        <groupId>org.springframework.boot</groupId>  
        <artifactId>spring-boot-starter-data-redis</artifactId>  
    </dependency>  
</dependencies>
```

# 2、扩展知识点
## 2.1、key生成策略

对于`@Cacheable`注解，有两个参数用于组装缓存的key

- cacheNames/value: 类似于缓存前缀
- key: ===SpEL表达式，通常**根据传参**来生成最终的缓存key===

默认的 `redisKey = cacheNames::key` (注意中间的两个冒号)

如：
```java
/**  
 * 没有指定key时，采用默认策略 {@link org.springframework.cache.interceptor.SimpleKeyGenerator } 生成key  
 * <p>  
 * 对应的key为: k1::id  
 * value --> 等同于 cacheNames  
 * @param id  
 * @return  
 */  
@Cacheable(value = "k1")  
public String key1(int id) {  
    return "defaultKey:" + id;  
}
```

缓存key默认采用`SimpleKeyGenerator`来生成，比如上面的调用，如果`id=1`， 那么对应的缓存key为 `k1::1`

如果没有参数，或者多个参数呢？
```java
/**  
 * redis_key :  k0::SimpleKey[]  
 *  
 * @return  
 */  
@Cacheable(value = "k0")  
public String key0() {  
    return "key0";  
}  
  
/**  
 * redis_key :  k2::SimpleKey[id,id2]  
 *  
 * @param id  
 * @param id2  
 * @return  
 */  
@Cacheable(value = "k2")  
public String key2(Integer id, Integer id2) {  
    return "key1" + id + "_" + id2;  
}  
  
/**
 * redis_key :  k3::{ooo=xxx,……}
*/
@Cacheable(value = "k3")  
public String key3(Map map) {  
    return "key3" + map;  
}
```

然后写一个测试case
```java
@RestController  
@RequestMapping(path = "extend")  
public class ExtendRest {  
    @Autowired  
    private RedisTemplate redisTemplate;  
  
    @Autowired  
    private ExtendDemo extendDemo;  
  
    @GetMapping(path = "default")  
    public Map<String, Object> key(int id) {  
        Map<String, Object> res = new HashMap<>();  
        res.put("key0", extendDemo.key0());  
        res.put("key1", extendDemo.key1(id));  
        res.put("key2", extendDemo.key2(id, id));  
        res.put("key3", extendDemo.key3(res));  
  
        // 这里将缓存key都捞出来  
        Set<String> keys = (Set<String>) redisTemplate.execute((RedisCallback<Set<String>>) connection -> {  
            Set<byte[]> sets = connection.keys("k*".getBytes());  
            Set<String> ans = new HashSet<>();  
            for (byte[] b : sets) {  
                ans.add(new String(b));  
            }  
            return ans;  
        });  
  
        res.put("keys", keys);  
        return res;  
    }  
}
```

访问之后，输出结果如下：
```java
{  
    "key1": "defaultKey:1",  
    "key2": "key11_1",  
    "key0": "key0",  
    "key3": "key3{key1=defaultKey:1, key2=key11_1, key0=key0}",  
    "keys": [  
        "k2::SimpleKey [1,1]",  
        "k1::1",  
        "k3::{key1=defaultKey:1, key2=key11_1, key0=key0}",  
        "k0::SimpleKey []"  
    ]  
}
```

小结一下 `redis_key` 的几种情况：
- 单参数：`cacheNames::arg`
- 无参数: `cacheNames::SimpleKey []`, 后面使用 `SimpleKey []`来补齐
- 多参数: `cacheNames::SimpleKey [arg1, arg2...]`
- 非基础对象：`cacheNames::obj.toString()`

## 2.2、自定义key生成策略

如果希望使用自定义的key生成策略，只需继承KeyGenerator，并声明为一个bean
```java
@Component("selfKeyGenerate")  
public static class SelfKeyGenerate implements KeyGenerator {  
    @Override  
    public Object generate(Object target, Method method, Object... params) {
	// target.getClass().getSimpleName() 指的是target对应类全限定名的简写（只有类名）
	return target.getClass().getSimpleName() + "#" + method.getName() + "(" + JSON.toJSONString(params) + ")";  
    }  
}
```

然后在使用的地方，利用注解中的`keyGenerator`来指定key生成策略
```java
/**  
 * 对应的redisKey 为： get  vv::ExtendDemo#selfKey([id])  
 *  
 * @param id  
 * @return  
 */  
@Cacheable(value = "vv", keyGenerator = "selfKeyGenerate")  
public String selfKey(int id) {  
    return "selfKey:" + id + " --> " + UUID.randomUUID().toString();  
}
```

测试用例
```java
@GetMapping(path = "self")  
public Map<String, Object> self(int id) {  
    Map<String, Object> res = new HashMap<>();  
    res.put("self", extendDemo.selfKey(id));  
    Set<String> keys = (Set<String>) redisTemplate.execute((RedisCallback<Set<String>>) connection -> {  
        Set<byte[]> sets = connection.keys("vv*".getBytes());  
        Set<String> ans = new HashSet<>();  
        for (byte[] b : sets) {  
            ans.add(new String(b));  
        }  
        return ans;  
    });  
    res.put("keys", keys);  
    return res;  
}
```

缓存key放在了返回结果的keys中，输出如下，和预期的一致
```java
{  
    "keys": [  
        "vv::ExtendDemo#selfKey([1])"  
    ],  
    "self": "selfKey:1 --> f5f8aa2a-0823-42ee-99ec-2c40fb0b9338"  
}
```

## 2.3、缓存失效时间

以上所有的缓存都没有设置失效时间，实际的业务场景中，不设置失效时间的场景有；但更多的都需要设置一个ttl，对于Spring的缓存注解，原生没有额外提供一个指定ttl的配置，如果我们希望指定ttl，可以通过`RedisCacheManager`来完成
```java
private RedisCacheConfiguration getRedisCacheConfigurationWithTtl(Integer seconds) {  
    // 设置 json 序列化  
    Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);  
    ObjectMapper om = new ObjectMapper();  
    om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);  
    jackson2JsonRedisSerializer.setObjectMapper(om);  
  
    RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();  
    redisCacheConfiguration = redisCacheConfiguration.serializeValuesWith(  
            RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer)).  
            // 设置过期时间  
            entryTtl(Duration.ofSeconds(seconds));  
  
    return redisCacheConfiguration;  
}
```

上面是一个设置RedisCacheConfiguration的方法，其中有两个点
- 序列化方式：采用json对缓存内容进行序列化
- 失效时间：根据传参来设置失效时间

如果希望针对特定的key进行定制化的配置的话，可以如下操作
```java
private Map<String, RedisCacheConfiguration> getRedisCacheConfigurationMap() {  
    Map<String, RedisCacheConfiguration> redisCacheConfigurationMap = new HashMap<>(8);  
    // 自定义设置缓存时间  
    // 这个k0 表示的是缓存注解中的 cacheNames/value  
    redisCacheConfigurationMap.put("k0", this.getRedisCacheConfigurationWithTtl(60 * 60));  
    return redisCacheConfigurationMap;  
}
```

最后就是定义我们需要的`RedisCacheManager`
```java
@Bean  
public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {  
    return new RedisCacheManager(  
            RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory),  
            // 默认策略，未配置的 key 会使用这个  
            this.getRedisCacheConfigurationWithTtl(60),  
            // 指定 key 策略  
            this.getRedisCacheConfigurationMap()  
    );  
}
```

在前面的测试case基础上，添加返回ttl的信息
```java
private Object getTtl(String key) {  
    return redisTemplate.execute(new RedisCallback() {  
        @Override  
        public Object doInRedis(RedisConnection connection) throws DataAccessException {  
            return connection.ttl(key.getBytes());  
        }  
    });  
}  
  
@GetMapping(path = "default")  
public Map<String, Object> key(int id) {  
    Map<String, Object> res = new HashMap<>();  
    res.put("key0", extendDemo.key0());  
    res.put("key1", extendDemo.key1(id));  
    res.put("key2", extendDemo.key2(id, id));  
    res.put("key3", extendDemo.key3(res));  
  
    Set<String> keys = (Set<String>) redisTemplate.execute((RedisCallback<Set<String>>) connection -> {  
        Set<byte[]> sets = connection.keys("k*".getBytes());  
        Set<String> ans = new HashSet<>();  
        for (byte[] b : sets) {  
            ans.add(new String(b));  
        }  
        return ans;  
    });  
  
    res.put("keys", keys);  
  
    Map<String, Object> ttl = new HashMap<>(8);  
    for (String key : keys) {  
        ttl.put(key, getTtl(key));  
    }  
    res.put("ttl", ttl);  
    return res;  
}
```

返回结果如下，注意返回的ttl失效时间
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310062336583.png)

## 2.4、自定义失效时间扩展

虽然上面可以实现失效时间指定，但是用起来依然不是很爽，要么是全局设置为统一的失效时间；要么就是在代码里面硬编码指定，失效时间与缓存定义的地方隔离，这就很不直观了

接下来介绍一种，直接在注解中，设置失效时间的case

如下面的使用case
```java
/**  
 * 通过自定义的RedisCacheManager, 对value进行解析，=后面的表示失效时间  
 * @param key  
 * @return  
 */  
@Cacheable(value = "ttl=30")  
public String ttl(String key) {  
    return "k_" + key;  
}
```

自定义的策略如下：

- value中，等号左边的为cacheName, 等号右边的为失效时间

要实现这个逻辑，可以扩展一个自定义的`RedisCacheManager`，如
```java
public class TtlRedisCacheManager extends RedisCacheManager {  
    public TtlRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {  
        super(cacheWriter, defaultCacheConfiguration);  
    }  
  
    @Override  
    protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {  
        String[] cells = StringUtils.delimitedListToStringArray(name, "=");  
        name = cells[0];  
        if (cells.length > 1) {  
            long ttl = Long.parseLong(cells[1]);  
            // 根据传参设置缓存失效时间  
            cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(ttl));  
        }  
        return super.createRedisCache(name, cacheConfig);  
    }  
}
```

重写`createRedisCache`逻辑， 根据name解析出失效时间；

注册使用方式与上面一致，声明为Spring的bean对象
```java
@Primary  
@Bean  
public RedisCacheManager ttlCacheManager(RedisConnectionFactory redisConnectionFactory) {  
    return new TtlRedisCacheManager(RedisCacheWriter.lockingRedisCacheWriter(redisConnectionFactory),  
            // 默认缓存配置  
            this.getRedisCacheConfigurationWithTtl(60));  
}
```

测试case如下
```java
@GetMapping(path = "ttl")  
public Map ttl(String k) {  
    Map<String, Object> res = new HashMap<>();  
    res.put("execute", extendDemo.ttl(k));  
    res.put("ttl", getTtl("ttl::" + k));  
    return res;  
}
```

验证结果如下
![0.jpg](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310062341556.jpg)

## 2.5、小结

到此基本上将Spring中缓存注解的常用姿势都介绍了一下，无论是几个注解的使用case，还是自定义的key策略，失效时间指定，单纯从使用的角度来看，基本能满足我们的日常需求场景

下面是针对缓存注解的一个知识点抽象

### 2.5.1、**缓存注解**

- `@Cacheable`: 缓存存在，则从缓存取；否则执行方法，并将返回结果写入缓存
- `@CacheEvit`: 失效缓存
- `@CachePut`: 更新缓存
- `@Caching`: 都注解组合

### 2.5.2、**配置参数**

- `cacheNames/value`: 可以理解为缓存前缀
- `key`: 可以理解为缓存key的变量，支持SpEL表达式
- `keyGenerator`: key组装策略
- `condition/unless`: 缓存是否可用的条件

### 2.5.3、**默认缓存ke策略y**

> 下面的cacheNames为注解中定义的缓存前缀，两个分号固定

- 单参数：`cacheNames::arg`
- 无参数: `cacheNames::SimpleKey []`, 后面使用 `SimpleKey []`来补齐
- 多参数: `cacheNames::SimpleKey [arg1, arg2...]`
- 非基础对象：`cacheNames::obj.toString()`

### 2.5.4、**缓存失效时间**

失效时间，本文介绍了两种方式，一个是集中式的配置，通过设置`RedisCacheConfiguration`来指定ttl时间

另外一个是扩展`RedisCacheManager`类，实现自定义的`cacheNames`扩展解析

Spring缓存注解知识点到此告一段落，我是一灰灰，欢迎关注长草的公众号`一灰灰blog`

# 3、不能错过的源码和相关知识点
## 3.1、项目

### 0. 项目

**系列博文**

- [Spring系列缓存注解@Cacheable @CacheEvit @CachePut 使用姿势介绍](http://mp.weixin.qq.com/s?__biz=MzU3MTAzNTMzMQ==&mid=2247486428&idx=1&sn=e64947b13d5261db72e7c8d3e56e9cfe&chksm=fce71070cb90996677ae7a42600977855e45a9fe2fa24700b21936287c6dcfa44b9eb2ff68dd&token=1673053889&lang=zh_CN#rd)

**源码**

- 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 源码：[https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)


转载自：[【DB系列】SpringBoot缓存注解@Cacheable之自定义key策略及缓存失效时间指定](https://spring.hhui.top/spring-blog/2021/07/01/210701-SpringBoot%E7%BC%93%E5%AD%98%E6%B3%A8%E8%A7%A3-Cacheable%E4%B9%8B%E8%87%AA%E5%AE%9A%E4%B9%89key%E7%AD%96%E7%95%A5%E5%8F%8A%E7%BC%93%E5%AD%98%E5%A4%B1%E6%95%88%E6%97%B6%E9%97%B4%E6%8C%87%E5%AE%9A/)




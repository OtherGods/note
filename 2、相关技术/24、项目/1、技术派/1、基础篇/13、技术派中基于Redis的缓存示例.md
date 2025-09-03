今天由我来给大家讲一下技术派中是如何基于 Redis 实现 Session  缓存的。要知道，互联网应用一般都比较喜欢追求高并发，高可用，因此，缓存的地位可以说是举足轻重，对程序的性能提升有很大的帮助。

参照：
1. [14、技术派Cacheable注解实现缓存](2、相关技术/24、项目/1、技术派/1、基础篇/14、技术派Cacheable注解实现缓存.md)
2. [15、技术派整合本地缓存之Guava](2、相关技术/24、项目/1、技术派/1、基础篇/15、技术派整合本地缓存之Guava.md)
3. [16、技术派Caffeine整合本地缓存](2、相关技术/24、项目/1、技术派/1、基础篇/16、技术派Caffeine整合本地缓存.md)

相比本地缓存 Guava Cache 和 Caffeine，Redis 更加强大，主要表现在以下几个方面：
- Redis支持集群和分布式部署，可以横向扩展缓存容量和负载能力，适合大型分布式系统的缓存需求。
- Redis支持数据持久化存储，可以将缓存数据存储到磁盘中，保证数据不会丢失，即使系统宕机或重启也不会造成数据丢失。
- Redis支持多种数据结构，如字符串、哈希表、列表、集合和有序集合等，可以适应不同的缓存需求，提供更灵活的缓存能力。
- Redis支持主从同步和哨兵机制，可以实现高可用性和容错能力，提供更稳定的缓存服务。
- Redis提供丰富的数据处理命令，如排序、聚合、管道和 Lua 脚本等，可以更方便、更高效地处理缓存数据。

# 0、ChatGPT：Guava Cache、Caffeine、Redis的区别

Guava Cache、Caffeine 和 Redis 是三种常用的缓存技术，它们各自有不同的特点和适用场景。以下是它们的主要区别和特点：

Guava Cache、Caffeine 和 Redis 是三种常用的缓存技术，它们各自有不同的特点和适用场景。以下是它们的主要区别和特点：
## Guava Cache

### 特点
- **本地缓存**：Guava Cache 是一个在 JVM 内运行的本地缓存，适合单节点应用。
- **轻量级**：它是 Google Guava 库的一部分，非常轻量级，易于使用和集成。
- **缓存策略**：支持多种缓存失效策略，例如基于时间的过期、基于大小的回收、基于引用的回收等。
- **线程安全**：Guava Cache 是线程安全的，适用于多线程环境。
- **弱一致性**：由于是本地缓存，它不能在分布式环境中提供一致性保证。

### 适用场景
- 小型应用或工具，缓存需求较少。
- 需要轻量级、快速集成的缓存解决方案。
- 不需要分布式缓存和高可用性。

### 示例代码
```java
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class GuavaCacheExample {

    public static void main(String[] args) throws ExecutionException {
        LoadingCache<String, String> cache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) {
                        return "Value for " + key;
                    }
                });

        System.out.println(cache.get("key1"));
    }
}
```

## Caffeine

### 特点
- **本地缓存**：Caffeine 是一个在 JVM 内运行的本地缓存，适合单节点应用。
- **高性能**：Caffeine 是对 Guava Cache 的改进版，性能更高，尤其是在高并发场景下。
- **高级特性**：支持多种缓存失效策略、写入监听器、异步加载等特性。
- **线程安全**：Caffeine 是线程安全的，适用于多线程环境。
- **弱一致性**：由于是本地缓存，它不能在分布式环境中提供一致性保证。

### 适用场景
- 高并发、高性能需求的本地缓存。
- 需要高级缓存特性和优化的场景。
- 不需要分布式缓存和高可用性。

### 示例代码
```java
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class CaffeineCacheExample {

    public static void main(String[] args) {
        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();

        cache.put("key1", "Value for key1");

        System.out.println(cache.getIfPresent("key1"));
    }
}
```

## Redis

### 特点
- **分布式缓存**：Redis 是一个分布式内存数据存储，适合分布式系统和集群环境。
- **高性能**：Redis 以极高的读写性能著称，适用于高并发场景。
- **多数据结构支持**：支持字符串、哈希、列表、集合、有序集合等多种数据结构。
- **持久化**：支持数据持久化到磁盘，防止数据丢失。
- **高可用性和扩展性**：通过 Redis Sentinel 和 Redis Cluster 提供高可用性和扩展性。
- **一致性**：在分布式环境中，Redis 可以通过配置提供一定的一致性保证。

### 适用场景
- 分布式系统，需要共享缓存的场景。
- 高并发、高性能需求的缓存。
- 需要持久化、高可用性和扩展性的缓存场景。

### 示例代码
```java
import redis.clients.jedis.Jedis;

public class RedisCacheExample {

    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost");

        jedis.set("key1", "Value for key1");

        System.out.println(jedis.get("key1"));

        jedis.close();
    }
}
```

## 总结

| 特性               | Guava Cache                       | Caffeine                        | Redis                             |
|--------------------|-----------------------------------|---------------------------------|-----------------------------------|
| 缓存类型           | 本地缓存                           | 本地缓存                         | 分布式缓存                         |
| 性能               | 高                                  | 非常高                           | 极高                              |
| 线程安全           | 是                                  | 是                               | 是                                |
| 高可用性和扩展性   | 否                                  | 否                               | 是                                |
| 持久化             | 否                                  | 否                               | 是                                |
| 适用场景           | 单节点应用，小型缓存需求            | 单节点应用，高并发需求            | 分布式系统，高并发，高可用性需求   |

选择哪种缓存技术取决于应用的具体需求和场景。如果是单节点应用且缓存需求较小，可以选择 Guava Cache 或 Caffeine；如果是分布式系统，需要高可用性和扩展性，则 Redis 是更好的选择。


# 1、关于Redis

Redis 本身是一个开源的、基于内存的 NoSQL 数据库，不过与其说它是数据库，不如说它是一个中间件，因为体积小、效率高等特点，Redis 更适合拿来做数据库前面的缓存层组件。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308292243568.png)

Redis 支持多种类型的数据结构，例如 String 字符串、List 列表、Set 集合、Hash 哈希、ZSet 有序集合等。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308292245043.png)

Redis 支持数据持久化，可以将内存的数据利用快照和日志的形式保存到硬盘上，待 Redis 重启后可以再次加载到内存中使用。此外它的主从复制、哨兵等高级特性使得它成为最受欢迎的缓存中间件。

我经常给读者强调 Redis 的重要性，甚至把 Redis 列入 Java 后端开发四大件之一，另外三件是 Java 基础、Spring Boot 和 MySQL，我相信大家是认可这个观点的（反驳无效😆）。

如果球友们看过一些 Java 后端的面经的话，应该就能清楚地感觉到，Redis 是面试中最经常问到的技术栈之一，非常能考验一名技术人的功底。我简单举几个例子：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308292246936.png)

所以，在日常开发和学习当中，一定要抽出一部分时间和精力把 Redis 相关的知识点学扎实了。
# 2、整合Redis

前面的章节已经讲过 Redis 的整合方案了，但是为了这篇教程的读者更顺畅地了解技术派，这里简单过一遍。

## 2.1、第一步，在 pom.xml 中添加 Redis 的依赖。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

默认情况下，Spring Boot 使用 Lettuce 作为 Redis 连接池，可以避免频繁地创建和销毁连接，提高应用程序的性能和可靠性。下图是 starter 中包含的依赖：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308292249062.png)

## 2.2、第二步，在 application.yml 中添加 Redis 的配置

本地比较简单，指定 host 和 port 就可以了。生产环境中的配置可以参照前一篇教程。
```yml
redis:
  host: localhost
  port: 6379
  password:
```

## 2.3、第三步，在本地启动 Redis 服务

我用的 macOS，终端用的 Wrap，目前已经集成了 ChatGPT 的人工智能，非常强大👍，如果不确定使用什么命令，可以直接问他。

比如说 redis-cli ping 用来查看 Redis 服务是否正确安装和运行，redis-server 用来启动 Redis 服务，可以看到默认的端口是 6379。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308292250637.png)

## 2.4、四步，编写 Redis 测试类

快速测试 Redis 在技术派项目中是否可用。
```java
@SpringBootTest(classes = QuickForumApplication.class)
public class RedisTemplateDemo {
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testPut() {
        //设置 key 和 value，并保存到 Redis 中
        redisTemplate.opsForValue().set("itwanger", "沉默王二");
        stringRedisTemplate.opsForList().rightPush("girl", "陈清扬");
        stringRedisTemplate.opsForList().rightPush("girl", "小转玲");
        stringRedisTemplate.opsForList().rightPush("girl", "茶花女");
	}
	
	@Test
    public void testGet() {
        //从 Redis 中获取 key 对应的 value
        Object value = redisTemplate.opsForValue().get("itwanger");
        System.out.println(value);
        
        List<String> girls = stringRedisTemplate.opsForList().range("girl", 0, -1);
        System.out.println(girls);
	}
}
```

简单解释一下这段代码：
- `@SpringBootTest(classes = QuickForumApplication.class)` 注解指定了技术派的启动类为 QuickForumApplication，表示该类是一个 Spring Boot 的单元测试。@Autowired 注解用于将 RedisTemplate 和 StringRedisTemplate 注入到测试类中。
  
  RedisTemplate 和 StringRedisTemplate 是 Spring Boot 帮我们预先初始化好的 Redis 连接工厂，可以快速对 Redis 进行操作。RedisTemplate 是泛型类，可以操作任意类型的数据，而 StringRedisTemplate 只能操作字符串类型数据。
  
- 在 testPut() 方法中，使用 redisTemplate.opsForValue() 和 stringRedisTemplate.opsForList() 方法分别操作 Redis 中的字符串类型和列表类型数据。
  
- 在 testGet() 方法中，使用 redisTemplate.opsForValue().get("itwanger") 和 stringRedisTemplate.opsForList().range("girl", 0, -1) 方法分别获取 Redis 中的字符串类型和列表类型数据。
  
  先运行 testPut 方法，再运行 testGet 方法，可以看到如下结果：
  ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308292304191.png)

# 3、操作Redis

下面分别举例说明如何在 Spring Boot 中使用 RedisTemplate 和 StringRedisTemplate 操作 Redis 的字符串、列表、哈希、集合和有序列表，方便初次接触 Redis 的球友快速了解 Redis 的操作方法（已经学过的球友可以跳过）。

## 3.1、字符串

字符串是 Redis 中最基本的数据类型之一，可以存储任意类型的数据。在下面的示例中，我们注入了 RedisTemplate，并使用 opsForValue() 方法获取 Redis 字符串的操作对象，然后使用 set() 方法设置键值对，使用 get() 方法获取键对应的值。
```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

public void set(String key, Object value) {
    redisTemplate.opsForValue().set(key, value);
}

public Object get(String key) {
    return redisTemplate.opsForValue().get(key);
}
```

## 3.2、列表

列表是一个有序的字符串列表，可以存储多个字符串。在下面的示例中，我们注入了 StringRedisTemplate，并使用 opsForList() 方法获取 Redis 列表的操作对象，然后使用 rightPush() 方法向列表右侧添加元素，使用 range() 方法获取列表指定下标范围内的元素。
```java
@Autowired
private StringRedisTemplate stringRedisTemplate;

public void push(String key, String value) {
    stringRedisTemplate.opsForList().rightPush(key, value);
}

public List<String> range(String key, int start, int end) {
    return stringRedisTemplate.opsForList().range(key, start, end);
}
```

## 3.3、哈希

哈希是一种键值对的数据结构，可以存储多个字段和对应的值。在下面的示例中，我们使用 opsForHash() 方法获取 Redis 哈希的操作对象，然后使用 put() 方法向哈希中添加字段和对应的值，使用 get() 方法获取指定字段的值。
```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

public void hset(String key, String field, Object value) {
    redisTemplate.opsForHash().put(key, field, value);
}

public Object hget(String key, String field) {
    return redisTemplate.opsForHash().get(key, field);
}
```

## 3.4、集合

集合是一个无序的字符串集合，可以存储多个字符串。在下面的示例中，我们使用 opsForSet() 方法获取 Redis 集合的操作对象，然后使用 add() 方法向集合中添加元素，使用 members() 方法获取集合中所有的元素。
```java
@Autowired
private StringRedisTemplate stringRedisTemplate;

public void sadd(String key, String value) {
    stringRedisTemplate.opsForSet().add(key, value);
}

public Set<String> smembers(String key) {
    return stringRedisTemplate.opsForSet().members(key);
}
```

## 3.5、有序集合

有序集合是一个有序的字符串集合，可以存储多个字符串，每个字符串都有一个分值，可以根据分值排序。在下面的示例中，我们使用 opsForZSet() 方法获取 Redis 有序集合的操作对象，然后使用 add() 方法向列表中添加元素和分值，使用 range() 方法获取列表指定下标范围内的元素。
```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

public void zadd(String key, String value, double score) {
    redisTemplate.opsForZSet().add(key, value, score);
}

public Set<Object> zrange(String key, long start, long end) {
    return redisTemplate.opsForZSet().range(key, start, end);
}
```

# 4、技术派中的Redis实例

技术派目前一共有两处用到 Redis 缓存，一处会使用 Redis 来保存用户的 session 信息，另外一处会使用 Redis 来保存 sitemap（一种 XML 文件，用于向搜索引擎描述网站的结构和内容，包括网站中的页面、文本内容、图片、视频等信息，以帮助搜索引擎更好地索引网站，说人话就是，帮助搜索引擎更好地搜到技术派网站的内容）。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308292331438.png)

之前的教程里也提到了 RedisClient 这个类，该类就是基于 RedisTemplate 做的封装，用来简化使用成本。我会结合具体的业务和源码，来帮大家彻底搞清楚。我们来详细地分析一下这个类。

## 4.1、注入RedisTemplate

打开 RedisClient 源码，映入眼帘的是下面这段代码：
```java
private static RedisTemplate<String, String> template;

public static void register(RedisTemplate<String, String> template) {
    RedisClient.template = template;
}
```

- 一个 RedisTemplate 类型的静态变量 template
- 一个静态方法 register，它会在 ForumCoreAutoConfig 类中被调用到：
  ```java
@Configuration
@ComponentScan(basePackages = "com.github.paicoding.forum.core")
public class ForumCoreAutoConfig {
	public ForumCoreAutoConfig(RedisTemplate<String, String> redisTemplate) {
        RedisClient.register(redisTemplate);
    }
}
  ```

这段代码定义了一个 Spring 配置类 ForumCoreAutoConfig，其中包含了一个构造方法，该构造方法有一个参数 RedisTemplate<String, String> redisTemplate。

在构造方法内，我们调用了 RedisClient.register(redisTemplate)，将 redisTemplate 注册到 RedisClient 中去。

需要注意的是，我们并没有在这个配置类中定义 RedisTemplate 的 Bean，而是通过 Spring Boot 的自动配置机制进行自动注入。Spring Boot 会根据我们的配置，自动创建并配置一个 RedisTemplate 实例，并将其注入到这个类的构造方法中。在这个类中，我们可以直接使用注入的 RedisTemplate 而无需关心它是如何创建和配置的。

如果在 ForumCoreAutoConfig 的构造方法中打入断点进行 debug 的时候，会看到这样一条日志：
>o.s.b.f.s.DefaultListableBeanFactory.createArgumentArray(ConstructorResolver.java:808) - Autowiring by type from bean name 'com.github.paicoding.forum.core.ForumCoreAutoConfig' via constructor to bean named 'stringRedisTemplate'
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308292336134.png)

这段日志表示在自动装配 ForumCoreAutoConfig 这个 bean 的时候，Spring 容器会尝试自动装配它的构造方法参数 RedisTemplate<String, String> redisTemplate。

可以在 `logback.xml` 中配置Spring框架源码的日志级别，通过这样设置，可以在控制台看到上面说的日志
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406032158345.png)

关于 Spring Boot 的自动状态，我们随后会开一篇文档单独来详细地讲，这里先留个坑位。

## 4.2、使用RedisClient

前面也提到了，技术派目前有两处在用 Redis 缓存：用户 session 和 sitemap。

### 4.2.1、我们先来看用户session这里

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308292337192.png)

1. 校验成功后会调用RedisClient的setStrWithExpire方法：
   ```java
/**
 * 将字符串类型的值 value 关联到 key，并设置过期时间 expire（单位：秒）。
 * @param key 键
 * @param value 值
 * @param expire 过期时间（单位：秒）
 * @return 如果 SET 操作成功，则返回 true；否则返回 false。
 */
 public static Boolean setStrWithExpire(String key, String value, Long expire) {
    // 使用 RedisTemplate 实例执行 Redis 命令
    return template.execute(new RedisCallback<Boolean>() {
        @Override
        public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {
	        // 调用 RedisConnection 实例的 setEx 方法，将 key-value 对存储到 Redis 中，并设置过期时间
            return redisConnection.setEx(keyBytes(key), expire, valBytes(value));
        }
    });
}
   ```
   这个方法用于将以恶搞字符串的值与给定的key关联，并设置过期时间。在方法中，我们使用了RedisTemplate实例的excute方法来执行Redis命令。
   RedisCallback是一个回调接口，我们需要实现其中的doInRedis方法，在方法中编写Redis命令的实现。在这个方法中，我们调用了RedisConnection实例的setEx方法，将key-value存储到Redis中，并设置过期时间。最后，将setEx方法的返回值作为方法返回值返回，表示SET操作是否成功。
   
   需要注意的是：keyBytes和valBytes方法用于将字符串类型的key和value转换为字节数组类型。
   ```java
/**
 * 生成技术派的缓存key
 *
 * @param key
 * @return
 */
 public static byte[] keyBytes(String key) {
    nullCheck(key);
    key = KEY_PREFIX + key;
    return key.getBytes(CODE);
}

/**
 * 技术派的缓存值序列化处理
 *
 * @param val
 * @param <T>
 * @return
 */
 public static <T> byte[] valBytes(T val) {

    if (val instanceof String) {
        return ((String) val).getBytes(CODE);
    } else {
        return JsonUtil.toStr(val).getBytes(CODE);
    }
}
   ```
   
   对应技术派中的业务是输入验证码登录成功后会存储session。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308292347130.png)

2. 用户登出的时候会调用RedisClient的del方法，删除session
   ```java
public static void del(String key) {
    template.execute((RedisCallback<Long>) con -> con.del(keyBytes(key)));
}
   ```

3. 用户登录的时候会调用RedisClient的getStr方法，根据session获取用户ID
   ```java
public static String getStr(String key) {
    return template.execute((RedisCallback<String>) con -> {
        byte[] val = con.get(keyBytes(key));
        return val == null ? null : new String(val);
    });
}
   ```
   
### 4.2.2、再来看sitemap这里

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308292353399.png)

1. 获取 sitemap 的时候会调用 RedisClient 的 hGetAll 方法
   ```java
/**
 * 获取哈希表 key 中的所有字段和值，以 Map<String, T> 的形式返回。
 * @param key 哈希表的 key
 * @param clz 值的类型
 * @param <T> 值的类型
 * @return 哈希表中的所有字段和值，以 Map<String, T> 的形式返回。
 */
public static <T> Map<String, T> hGetAll(String key, Class<T> clz) {
	// 使用 RedisTemplate 实例执行 Redis 命令
    Map<byte[], byte[]> records = template.execute((RedisCallback<Map<byte[], byte[]>>) con -> con.hGetAll(keyBytes(key)));
    if (records == null) {
        return Collections.emptyMap();
    }
    
    // 创建一个 Map 对象，用于存储哈希表中的所有字段和值
    Map<String, T> result = Maps.newHashMapWithExpectedSize(records.size());
    // 遍历哈希表中的所有字段和值，并将它们转换为字符串类型和指定类型的对象
    for (Map.Entry<byte[], byte[]> entry : records.entrySet()) {
        if (entry.getKey() == null) {
            continue;
        }
        
        // 将 byte[] 类型的值转换为指定类型的对象
        result.put(new String(entry.getKey()), toObj(entry.getValue(), clz));
    }
    return result;
}

   ```
   在方法中，我们使用了RedisTemplate实例的execute方法来执行Redsi命令。我们使用了 RedisCallback 接口作为回调函数，通过 Lambda 表达式实现了其中的 doInRedis 方法。在 doInRedis 方法中，我们调用了 RedisConnection 实例的 hGetAll 方法来获取哈希表中的所有字段和值。hGetAll 方法返回一个 `Map<byte[], byte[]>` 对象，其中的 key 和 value 均为字节数组类型。
   
   在方法中，我们首先判断 records 是否为 null，如果为 null，则直接返回一个空的不可变的 Map 对象。如果 records 不为 null，则遍历其中的所有字段和值，并将它们转换为字符串类型和指定类型的对象。在转换时，我们使用了 toObj 方法，该方法用于将字节数组类型的值转换为指定类型的对象。最后，将所有的字段和值存储到一个新的 Map 对象中，并将其返回。

2. 添加文章的时候调用 RedisClient 的 hSet 方法：
   ```java
/**
 * 在指定的哈希表中，将指定字段的值设置为指定的值，并返回设置操作的结果
 * @param key 哈希表的键名
 * @param field 哈希表的字段名
 * @param ans 哈希表字段的值
 * @param <T> 哈希表字段的值的类型
 * @return 设置操作的结果，如果设置成功则返回 true，否则返回 false
 */
public static <T> Boolean hSet(String key, String field, T ans) {
    return template.execute(new RedisCallback<Boolean>() {
        @Override
        public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {
            String save;
            // 判断 ans 是否是字符串类型
            if (ans instanceof String) {
                // 如果是字符串类型，则将其转换为字符串类型的 save
                save = (String) ans;
            } else {
                // 如果不是字符串类型，则将其转换为 JSON 格式的字符串
                save = JsonUtil.toStr(ans);
            }
            // 调用 RedisConnection 实例的 hSet 方法，设置指定哈希表字段的值，并返回设置操作的结果
            return redisConnection.hSet(keyBytes(key), valBytes(field), valBytes(save));
        }
	}
});
   ```
   
   在方法中，我们使用了 RedisTemplate 实例的 execute 方法来执行 Redis 命令。我们使用了 RedisCallback 接口作为回调函数，并通过匿名内部类实现了其中的 doInRedis 方法。在 doInRedis 方法中，我们首先将 ans 转换为字符串类型的 save，然后调用 RedisConnection 实例的 hSet 方法来设置指定的哈希表字段的值。hSet 方法返回一个 Boolean 类型的值，表示设置操作是否成功。
   
   需要注意的是，在方法中，我们通过 instanceof 运算符判断 ans 是否为 String 类型。如果是 String 类型，则直接将其转换为字符串类型的 save，否则将其转换为 JSON 格式的字符串。

3. 移除文章的时候调用 RedisClient 的 hDel 方法：
   ```java
/**
 * 从指定的哈希表中，删除指定的字段，并返回删除操作的结果
 * @param key 哈希表的键名
 * @param field 哈希表的字段名
 * @return 删除操作的结果，如果删除成功则返回 true，否则返回 false
 */
public static <T> Boolean hDel(String key, String field) {
    return template.execute(new RedisCallback<Boolean>() {
        @Override
        public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
            // 调用 RedisConnection 实例的 hDel 方法，删除指定哈希表的字段，并返回删除操作的结果
            return connection.hDel(keyBytes(key), valBytes(field)) > 0;
        }
	});
}
   ```

4. 初始化 sitemap 的时候会先调用 RedisClient 的 del 方法，这个前面讲过了，这里略过，随后会调用 RedisClient 的 hMSet 方法。
   ```java
/**
 * 在指定的哈希表中，将多个字段的值设置为指定的值
 * @param key 哈希表的键名
 * @param fields 哈希表的字段和对应的值
 * @param <T> 哈希表字段的值的类型
 */
public static <T> void hMSet(String key, Map<String, T> fields) {
    // 创建一个空的哈希表 val
    Map<byte[], byte[]> val = Maps.newHashMapWithExpectedSize(fields.size());
    // 将传入的字段和对应的值转换为字节数组类型，并添加到 val 中
    for (Map.Entry<String, T> entry : fields.entrySet()) {
        val.put(valBytes(entry.getKey()), valBytes(entry.getValue()));
    }
    // 使用 RedisTemplate 实例的 execute 方法执行 Redis 命令
    template.execute((RedisCallback<Object>) connection -> {
        // 调用 RedisConnection 实例的 hMSet 方法，一次性设置多个哈希表字段的值
        connection.hMSet(keyBytes(key), val);
        return null;
	});
}
   ```
   
   在 doInRedis 方法中，我们通过调用 RedisConnection 实例的 hMSet 方法一次性设置多个哈希表字段的值。
   
   需要注意的是，在方法中，我们使用了 Maps.newHashMapWithExpectedSize 方法创建了一个空的哈希表 val，在将字段和对应的值转换为字节数组类型后，我们将它们添加到 val 中。最后，我们通过调用 RedisConnection 实例的 hMSet 方法，将 val 中的多个字段一次性地设置到指定的哈希表中。
   
   如果希望搜索引擎快速收录网站内容的话，sitemap 就会非常有用，度娘是在「站长工具」这里提交的。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308300006790.png)
   
   为了加强搜索引擎对技术派的收录，我们做了一个 sitemap 的自动生成工具。可在 SitemapServiceImpl 中看到该定时任务。
   ```java
/**
 * 采用定时器方案，每天5:15分刷新站点地图，确保数据的一致性
 */
@Scheduled(cron = "0 15 5 * * ?")
public void autoRefreshCache() {
    log.info("开始刷新sitemap.xml的url地址，避免出现数据不一致问题!");
    refreshSitemap();
    log.info("刷新完成！");
}
   ```


### 4.3.3、关于RedisTemplate的execute方法

在 RedisClient 中，经常看到 `execute(RedisCallback<T> action)` 方法，可以用于执行任意的 Redis 命令。该方法接收一个 RedisCallback 接口作为参数，并将 Redis 连接传递给回调接口来执行 Redis 命令。
```java
@Override
@Nullable
public <T> T execute(RedisCallback<T> action) {
  return execute(action, isExposeConnection());
}
```

我们来写个测试用例：
```java
@Test
public void testExecute() {
    // 使用 execute 方法执行 Redis 命令
    redisTemplate.execute(new RedisCallback<Object>() {
        @Override
        public Object doInRedis(RedisConnection connection) throws DataAccessException {
            // 执行 Redis 命令，例如 set 和 get 命令
            connection.set("itwanger".getBytes(), "沉默王二".getBytes());
            byte[] value = connection.get("itwanger".getBytes());
            String strValue = new String(value);
            // 输出获取到的值
            System.out.println(strValue);
            return null;
		}
	});
}
```

运行测试用例后，可得到如下结果：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308300009564.png)

# 5、小结

Redis 是一款高性能的内存数据存储系统，支持多种数据结构，包括字符串、列表、哈希表、集合和有序集合等。在 Spring Boot 中，我们可以使用 Redis 作为缓存，提高应用程序的性能和响应速度。

Spring Boot 提供了 spring-boot-starter-data-redis，使得在应用程序中整合 Redis 变得非常容易。我们只需在 pom.xml 文件中添加 starter，然后在配置文件中增加一下 Redis 的连接信息就可以使用了。

我们可以使用 RedisTemplate 来执行 Redis 命令该类提供了多种方法，例如 opsForValue、opsForList、opsForHash、execute 等，用于执行不同类型的 Redis 命令。

在技术派中，我们使用 Redis 来缓存 session 和 sitemap，用到了 Redis 的字符串和哈希表两种数据结构。像其他的数据结构，以及其他高级功能，例如发布/订阅、事务、Lua 脚本等，我们会在后续的源码和教程中给大家提供，敬请期待。
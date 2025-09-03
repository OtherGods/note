### 一、客户端介绍

Redis 作为一个流行的开源内存键值数据库，拥有多个 Java 客户端，常见的包括：

1. **Jedis**: 这是最广泛使用的 Redis Java 客户端。它提供了一个小巧而且直接的 API 来与 Redis 交互。
2. **Lettuce**: 另一个流行的 Java 客户端，特别注重于可扩展性和性能。Lettuce 基于 Netty 构建，支持同步、异步和响应式模式。
3. **Redisson**: 这个客户端提供了丰富的 Redis 数据结构映射，如分布式集合、分布式锁、分布式队列等。它还支持集群模式和哨兵模式。
4. **Spring Data Redis**: 这是 Spring 框架的一部分，为 Redis 提供了高级抽象和模板。它简化了数据访问和整合，并支持 Jedis 和 Lettuce 作为底层客户端。默认使用的是 lettuce。
5. **JRedis**: 这是一个较老的 Java 客户端，可能在一些遗留系统中还在使用，但在新项目中使用频率较低。
6. **Redis Java**: 它是一个较轻量级的客户端，支持基本的 Redis 功能。

其中 JRedis和 Redis Java 使用较少，这里不做过多解读。如果用 Spring 开发建议使用 Spring Data Redis，如果不是建议使用 Redisson。

### 二、客户端对比

|客户端|线程安全|可伸缩性|优点|缺点|
|---|---|---|---|---|
|Jedis|否|Jedis 支持基本的分片和哨兵模式，但在高级特性和扩展方面相对有限|它直接提供了 Redis 命令的 Java 版本|Jedis 在实例上不是线程安全的。如果需要在多线程环境下使用，需要通过连接池来管理 Jedis 实例，或者每个线程一个实例|
|Lettuce|是|Lettuce 很好地支持高并发环境，适用于需要高吞吐量和低延迟的应用|Lettuce 基于 Netty，其主要优势在于线程安全和可伸缩性。它支持同步、异步和响应式编程|相对于 Jedis，Lettuce 更复杂，可能需要更多的学习成本。在某些使用场景下，其性能可能不如 Jedis|
|Redisson|是|提供了丰富的特性来支持可伸缩性，如集群支持、分片支持等|Redisson 提供了许多高级功能，如分布式数据结构和同步器，非常适合需要这些功能的复杂应用|相对复杂，学习曲线陡峭。在只需要基本 Redis 功能的场景中可能过于臃肿|
|Spring Data Redis|取决于底层使用的客户端，如 Lettuce 或 Jedis|通过整合如 Lettuce 等客户端，可以提供良好的可伸缩性|与 Spring 框架集成良好，提供了高级抽象和模板，简化了数据访问和整合|受限于 Spring 框架，不适用于非 Spring 环境。性能方面可能不如直接使用客户端|

### 三、客户端API

Jedis API ：[javadoc.io/doc/redis.c…](https://link.juejin.cn?target=https%3A%2F%2Fjavadoc.io%2Fdoc%2Fredis.clients%2Fjedis%2Flatest%2Findex.html "https://javadoc.io/doc/redis.clients/jedis/latest/index.html")  
Lettuce API ： [javadoc.io/doc/io.lett…](https://link.juejin.cn?target=https%3A%2F%2Fjavadoc.io%2Fdoc%2Fio.lettuce%2Flettuce-core%2Flatest%2Findex.html "https://javadoc.io/doc/io.lettuce/lettuce-core/latest/index.html")  
Redisson API ：[javadoc.io/doc/org.red…](https://link.juejin.cn?target=https%3A%2F%2Fjavadoc.io%2Fdoc%2Forg.redisson%2Fredisson%2Flatest%2Findex.html "https://javadoc.io/doc/org.redisson/redisson/latest/index.html")  
Spring Data Redis API : [javadoc.io/doc/org.spr…](https://link.juejin.cn?target=https%3A%2F%2Fjavadoc.io%2Fdoc%2Forg.springframework.data%2Fspring-data-redis%2Flatest%2Findex.html "https://javadoc.io/doc/org.springframework.data/spring-data-redis/latest/index.html")

### 四、客户端使用举例

#### 1. Jedis

首先添加 Jedis 依赖：
```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>最新版本号</version>
</dependency>
```

示例代码：
```java
import redis.clients.jedis.Jedis;

public class JedisDemo {
    public static void main(String[] args) {
        // 连接到 Redis 服务器
        Jedis jedis = new Jedis("localhost");
        System.out.println("连接成功");

        // 设置 redis 字符串数据
        jedis.set("mykey", "Hello, Redis!");
        // 获取存储的数据并输出
        System.out.println("redis 存储的字符串为: " + jedis.get("mykey"));
    }
}
```

#### 2. Lettuce

首先添加 Lettuce 依赖：
```xml
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
    <version>最新版本号</version>
</dependency>
```

示例代码：
```java
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

public class LettuceDemo {
    public static void main(String[] args) {
        // 连接到 Redis 服务器
        RedisClient redisClient = RedisClient.create("redis://localhost");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        System.out.println("连接成功");

        // 设置和获取数据
        connection.sync().set("mykey", "Hello, Redis!");
        String value = connection.sync().get("mykey");
        System.out.println("redis 存储的字符串为: " + value);

        // 关闭连接
        connection.close();
        redisClient.shutdown();
    }
}
```

#### 3. Redisson

首先添加 Redisson 依赖：
```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>最新版本号</version>
</dependency>
```

示例代码：
```java
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedissonDemo {
    public static void main(String[] args) {
        // 配置 Redisson 客户端
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        RedissonClient redisson = Redisson.create(config);

        // 操作 Redis 数据
        redisson.getBucket("mykey").set("Hello, Redisson!");
        String value = (String) redisson.getBucket("mykey").get();
        System.out.println("redis 存储的字符串为: " + value);

        // 关闭 Redisson 客户端
        redisson.shutdown();
    }
}
```

#### 4. Spring Data Redis

首先添加 Spring Data Redis 依赖：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <version>最新版本号</version>
</dependency>
```

示例代码（需要结合 Spring Boot 使用）：
```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SpringDataRedisDemo {

    @Autowired
    private RedisTemplate<String, String> template;

    public void demo() {
        // 设置和获取 Redis 数据
        template.opsForValue().set("mykey", "Hello, Spring Data Redis!");
        String value = template.opsForValue().get("mykey");
        System.out.println("redis 存储的字符串为: " + value);
    }
}
```

  

作者：Java36计  
链接：[https://juejin.cn/post/7321588019680116786](https://juejin.cn/post/7321588019680116786)  

来源：稀土掘金  
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
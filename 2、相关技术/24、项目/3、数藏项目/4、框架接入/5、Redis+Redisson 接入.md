# 1、依赖引入

```xml
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-data-redis</artifactId>  
</dependency>
```

# 2、增加配置

```yml
spring:  
  data:  
    redis:  
      host: r-xxxx.redis.rds.aliyuncs.com #Redis的Host  
      port: 6379 # Redis服务器连接端口  
      password: xxxx # Redis服务器连接密码（默认为空）
```

# 3、单元测试

```java
import cn.hollis.NFTurbo.NfTurboApplication;  
import org.junit.Test;  
import org.junit.runner.RunWith;  
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.boot.test.context.SpringBootTest;  
import org.springframework.data.redis.core.RedisTemplate;  
import org.springframework.test.context.junit4.SpringRunner;  
  
@RunWith(SpringRunner.class)  
@SpringBootTest(classes = {NfTurboApplication.class})  
public class RedisTest {  
      
    @Autowired  
    private RedisTemplate redisTemplate;  
      
    @Test  
    public void testRedisConnect() {  
          
        redisTemplate.opsForValue().set("test", "test");  
        Assert.assertTrue(redisTemplate.opsForValue().get("test").equals("test"));  
          
        if (redisTemplate.opsForValue().get("test").equals("test")) {  
            System.out.println("redis connect success");  
        }  
    }  
}
```

```xml
<dependency>  
    <groupId>junit</groupId>  
    <artifactId>junit</artifactId>  
    <version>4.13.1</version>  
    <scope>test</scope>  
</dependency>  
  
<dependency>  
    <groupId>org.springframework</groupId>  
    <artifactId>spring-test</artifactId>  
    <version>6.0.8</version>  
    <scope>test</scope>  
</dependency>  
  
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-test</artifactId>  
    <version>3.1.5</version>  
    <scope>test</scope>  
</dependency>  
  
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-test</artifactId>  
    <scope>test</scope>  
</dependency>
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411101045247.png)

### Redisson

```xml
<!--    Redisson    -->  
<dependency>  
    <groupId>org.redisson</groupId>  
    <artifactId>redisson-spring-boot-starter</artifactId>  
    <version>3.24.3</version>  
</dependency>
```

增加配置

```yml
spring:  
  redis:  
    redisson:  
      config:  
        singleServerConfig:  
          idleConnectionTimeout: 10000  
          connectTimeout: 10000  
          timeout: 3000  
          retryAttempts: 3  
          retryInterval: 1500  
          password: 'NFTurbo666'  
          subscriptionsPerConnection: 5  
          clientName: null  
          address: "redis://r-xxxx.redis.rds.aliyuncs.com:6379"  
          subscriptionConnectionMinimumIdleSize: 1  
          subscriptionConnectionPoolSize: 50  
          connectionMinimumIdleSize: 24  
          connectionPoolSize: 64  
          database: 0  
          dnsMonitoringInterval: 5000  
        threads: 16  
        nettyThreads: 32  
        codec: !<org.redisson.codec.JsonJacksonCodec> {}  
        transportMode: "NIO"
```

AI解释配置:
这段配置是Spring Boot应用中关于Redisson客户端的配置，Redisson是一个在Redis的基础上实现的Java驻留型内存数据网格（In-Memory Data Grid）。它提供了一套丰富的Redis客户端功能，包括分布式锁、分布式集合等。

下面是这段配置的详细解释：
1. **spring.redis.redisson.config**: 这部分是Redisson的详细配置，采用了YAML格式。
    - **singleServerConfig**: 表示单节点Redis服务器的配置。
        - **idleConnectionTimeout**: 空闲连接超时时间，单位是毫秒。这里设置为10000，表示如果连接在10秒内没有被使用，则会被关闭。
        - **connectTimeout**: 连接超时时间，单位是毫秒。设置为10000，表示尝试连接到Redis服务器的最大等待时间为10秒。
        - **timeout**: 命令执行超时时间，单位是毫秒。这里设置为3000，即每个命令的最大执行时间为3秒。
        - **retryAttempts**: 如果命令执行失败，重试的次数。这里设置为3。
        - **retryInterval**: 重试间隔，单位是毫秒。设置为1500，表示每次重试之间会等待1.5秒。
        - **password**: Redis服务器的密码。这里设置为'NFTurbo666'。
        - **subscriptionsPerConnection**: 每个连接可以订阅的频道数量。设置为5。
        - **clientName**: 客户端名称，这里留空（null）。
        - **address**: Redis服务器的地址和端口。这里是一个阿里云的Redis实例地址。
        - **subscriptionConnectionMinimumIdleSize**: 订阅连接的最小空闲大小。设置为1。
        - **subscriptionConnectionPoolSize**: 订阅连接池的大小。设置为50。
        - **connectionMinimumIdleSize**: 连接的最小空闲大小。设置为24。
        - **connectionPoolSize**: 连接池的大小。设置为64。
        - **database**: 使用的Redis数据库编号。默认为0。
        - **dnsMonitoringInterval**: DNS监控间隔时间，用于检查Redis服务器地址是否有变化。设置为5000毫秒。
    - **threads**: Redisson的工作线程数量。设置为16。
    - **nettyThreads**: Netty的线程数量，用于网络通信。设置为32。
    - **codec**: 编码解码器配置，这里使用了JsonJacksonCodec，表示使用Jackson库对Redis数据进行JSON格式的编码和解码。
    - **transportMode**: 传输模式，设置为"NIO"，表示使用Java的NIO（非阻塞IO）作为网络通信的基础。

总的来说，这段配置详细定义了Redisson客户端如何连接到Redis服务器，包括连接参数、重试策略、线程配置等，以及数据的编码解码方式和网络通信模式。





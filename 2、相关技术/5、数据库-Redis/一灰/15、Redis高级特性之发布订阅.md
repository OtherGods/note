
通常来讲，当我们业务存在消息的业务逻辑时更多的是直接使用成熟的rabbitmq,rocketmq，但是一些简单的业务场景中，真的有必要额外的引入一个mq么？本文将介绍一下redis的发布订阅方式，来实现简易的消息系统逻辑

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

redis的发布/订阅，主要就是利用两个命令`publish/subscribe`; 在SpringBoot中使用发布订阅模式比较简单，借助RedisTemplate可以很方便的实现

### 1.2.1、消息发布

```java
@Service  
public class PubSubBean {  
    @Autowired  
    private StringRedisTemplate redisTemplate;  
  
    public void publish(String key, String value) {  
        redisTemplate.execute(new RedisCallback<Object>() {  
            @Override  
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {  
                redisConnection.publish(key.getBytes(), value.getBytes());  
                return null;  
            }  
        });  
    }  
}
```

### 1.2.2、订阅消息

消息订阅这里，需要注意我们借助`org.springframework.data.redis.connection.MessageListener`来实现消费逻辑
```java
public void subscribe(MessageListener messageListener, String key) {  
    redisTemplate.execute(new RedisCallback<Object>() {  
        @Override  
        public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {  
            redisConnection.subscribe(messageListener, key.getBytes());  
            return null;  
        }  
    });  
}
```

### 1.2.3、测试case

写一个简单的测试case，来验证一下上面的发布订阅，顺带理解一下这个`MessageListener`的使用姿势；我们创建一个简单的WEB工程，提供两个rest接口
```java
@RestController  
@RequestMapping(path = "rest")  
public class DemoRest {  
    @Autowired  
    private PubSubBean pubSubBean;  
      
    // 发布消息  
    @GetMapping(path = "pub")  
    public String pubTest(String key, String value) {  
        pubSubBean.publish(key, value);  
        return "over";  
    }  
      
    // 新增消费者  
    @GetMapping(path = "sub")  
    public String subscribe(String key, String uuid) {  
	    // MessageListener: Redis中的消息监听器；这个匿名内部类监听器的作用是：当监听到某个事件时打印出uuid和监听到的消息
        pubSubBean.subscribe(new MessageListener() {  
            @Override  
            public void onMessage(Message message, byte[] bytes) {  
                System.out.println(uuid + " ==> msg:" + message);  
            }  
        }, key);  
        return "over";  
    }  
}
```

下面通过一个动图来演示一下case
![00.gif](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202405202135713.gif)


## 1.3、使用说明与应用场景

redis的发布订阅，只适用于比较简单的场景，从上面的使用说明也能看出，它就是一个简单的发布订阅模型，支持1对N，而且发送的消息，只有在线的消费者才能get到（至于不在线的，那就只能说遗憾了）而且对于redis而言，消息推出去之后就完事了，至于消费者能不能正常消费，那就不care了

划重点：

- 只有在线的消费者能接收到消息
- 对于消费者一个消息只能拿到一次

接下来的问题就来了，什么样的场景下可以使用redis的发布订阅呢？

**基于内存的缓存失效**

利用reids + 内存做二级缓存，可以说是比较常见的方式了，借助基于内存的缓存，可以有效的提高系统的负载，但是问题也很明显，内存中缓存数据失效是个问题，特别是当一个应用部署多台服务器时，如果我希望同时失效所有服务器的某个内存缓存，使用redis的发布/订阅就是一个比较好的选择

**SpringCloud Config配置刷新**

使用SpringCloud Config做配置中心的小伙伴可能会经常遇到这个问题，配置修改之后的动态刷新是个问题（当然官方是支持通过mq走bus总线来同步，也可以通过spring boot admin来强刷）

借助redis发布/订阅，实现配置动态刷新也是一个不错的备选方案（后面给出一个具体的实现demo，如有兴趣请持续关注一灰灰Blog）

**redis key失效订阅**

我们在使用redis做缓存时，通常会设置一个过期时间，redis提供了一个过期的事件，当然默认是不开启的；我们也是可以通过subscribe来订阅缓存失效的事件

修改配置，开启key失效事件
```shell
notify-keyspace-events Ex
```

重启redis之后，订阅失效事件即可
```shell
subscribe __keyevent@0__:expired
```

# 2、ChatGPT：SpringBoot结合redis实现发布订阅

## 2.1、创建SpringBoot项目

使用 Spring Initializr 创建一个新的 Spring Boot 项目，添加以下依赖：

- Spring Web
- Spring Data Redis
- Spring Boot Actuator

确保你的 `pom.xml` 文件中包含以下依赖：
```xml
<dependencies>
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Boot Starter Data Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Lettuce Redis Client -->
    <dependency>
        <groupId>io.lettuce.core</groupId>
        <artifactId>lettuce-core</artifactId>
    </dependency>
    
    <!-- Spring Boot Starter Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

在 `src/main/resources/application.properties` 文件中添加 Redis 配置信息：
```properties
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=yourpassword  # 如果没有密码，这行可以省略
```

## 2.2、创建消息接收器

创建一个类，用于处理接收到的消息：
```java
package com.example.redis;

import org.springframework.stereotype.Component;
import java.util.concurrent.CountDownLatch;

@Component
public class RedisMessageSubscriber {
    private CountDownLatch latch = new CountDownLatch(1);

    public void receiveMessage(String message) {
        System.out.println("Received message: " + message);
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }
}
```

## 2.3、配置Redis监听器

创建一个配置类，用于配置 Redis 消息监听器容器和 Redis 模板：
```java
package com.example.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	/**
	Spring 容器会自动配置并使用 RedisMessageListenerContainer 来监听 Redis 消息。
	`RedisMessageListenerContainer` 是一个特殊的 Bean，它在初始化时会自动连接到 Redis 服务器，并开始监听指定的通道。当 Redis 上有消息发布到这些通道时，容器会自动将消息分发给配置的消息监听器。
	*/
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                                   MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("myTopic"));
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "receiveMessage");
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

**`RedisConfig` 类作用总结：**
1. **RedisMessageListenerContainer**：
    - `container` 方法返回 `RedisMessageListenerContainer` 对象，负责管理和分发 Redis 消息。
    - 它监听 Redis 通道 `myTopic`，并将接收到的消息分发给 `MessageListenerAdapter`。
2. **MessageListenerAdapter**：
    - `listenerAdapter` 方法返回 `MessageListenerAdapter` 对象，包装了 `RedisMessageSubscriber` 对象，并指定 `receiveMessage` 方法处理接收到的消息。
    - 当 `RedisMessageListenerContainer` 接收到消息时，它会调用 `RedisMessageSubscriber` 的 `receiveMessage` 方法。
3. **RedisTemplate**：
    - `redisTemplate` 方法返回 `RedisTemplate<String, Object>` 对象，用于与 Redis 进行交互。
    - 这个模板配置了 `StringRedisSerializer` 作为键和值的序列化器，确保数据以字符串形式存储和读取。

**自动配置和使用**
- 当 Spring Boot 启动时，它会自动扫描带有 `@Configuration` 注解的类，并创建这些类中带有 `@Bean` 注解的方法返回的对象。
- `RedisMessageListenerContainer` 被创建并管理后，会自动开始监听 Redis 的 `myTopic` 通道。
- 任何发布到 `myTopic` 通道的消息都会通过 `MessageListenerAdapter` 传递给 `RedisMessageSubscriber` 的 `receiveMessage` 方法。
## 2.4、创建消息发布者

创建一个服务类，用于发布消息：
```java
package com.example.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisMessagePublisher {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void publish(String topic, String message) {
        redisTemplate.convertAndSend(topic, message);
    }
}
```

## 2.5、测试发布-订阅功能

在 Spring Boot 主类中测试发布-订阅功能：
```java
package com.example.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RedisPubSubApplication implements CommandLineRunner {

    @Autowired
    private RedisMessagePublisher messagePublisher;

    public static void main(String[] args) {
        SpringApplication.run(RedisPubSubApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        messagePublisher.publish("myTopic", "Hello, Redis!");
    }
}
```

## 2.6、项目结构

确保你的项目结构如下：
```css
src
├── main
│   ├── java
│   │   └── com
│   │       └── example
│   │           └── redis
│   │               ├── RedisPubSubApplication.java
│   │               ├── RedisConfig.java
│   │               ├── RedisMessagePublisher.java
│   │               └── RedisMessageSubscriber.java
│   └── resources
│       └── application.properties
```

## 2.7、项目运行

运行 `RedisPubSubApplication` 类，应该会看到控制台输出：
```yaml
Received message: Hello, Redis!
```

这说明消息发布和订阅功能已经成功实现。你可以根据需要扩展这个基本示例，例如添加更多的订阅者或处理更多的通道。

# 3、其他
## 3.1、项目

工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)


转载自：[【DB系列】Redis高级特性之发布订阅](https://spring.hhui.top/spring-blog/2020/10/17/201017-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8BRedis%E9%AB%98%E7%BA%A7%E7%89%B9%E6%80%A7%E4%B9%8B%E5%8F%91%E5%B8%83%E8%AE%A2%E9%98%85/#google_vignette)


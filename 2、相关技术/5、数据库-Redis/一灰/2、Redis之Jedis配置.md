
SpringBoot2之后，默认采用Lettuce作为redis的连接客户端，当然我们还是可以强制捡回来，使用我们熟悉的Jedis的，本篇简单介绍下使用Jedis的相关配置

# 1、基本配置
## 1.1、依赖

使用Jedis与Lettuce不同的是，需要额外的引入Jedis包的依赖
```xml
<dependencies>  
    <dependency>  
        <groupId>org.springframework.boot</groupId>  
        <artifactId>spring-boot-starter-data-redis</artifactId>  
    </dependency>  
  
    <dependency>  
        <groupId>org.apache.commons</groupId>  
        <artifactId>commons-pool2</artifactId>  
    </dependency>  
  
    <dependency>  
        <groupId>redis.clients</groupId>  
        <artifactId>jedis</artifactId>  
    </dependency>  
</dependencies>
```

## 1.2、配置

redis的相关配置，和前面的差不多，只是线程池的参数稍稍有点区别
```yml
spring:  
  redis:  
    host: 127.0.0.1  
    port: 6379  
    password:  
    database: 0  
    jedis:  
      pool:  
        max-idle: 6  
        max-active: 32  
        max-wait: 100  
        min-idle: 4
```

## 1.3、AutoConfig

与前面不同的是，我们需要定义一个`RedisConnectionFactory`的bean作为默认的连接工厂，以此来确定底层的连接采用的是Jedis客户端
```java
@Configuration  
public class RedisAutoConfig {  
  
    @Bean  
    public RedisConnectionFactory redisConnectionFactory(JedisPoolConfig jedisPool,  
            RedisStandaloneConfiguration jedisConfig) {  
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(jedisConfig);  
        connectionFactory.setPoolConfig(jedisPool);  
        return connectionFactory;  
    }  
  
    @Configuration  
    public static class JedisConf {  
        @Value("${spring.redis.host:127.0.0.1}")  
        private String host;  
        @Value("${spring.redis.port:6379}")  
        private Integer port;  
        @Value("${spring.redis.password:}")  
        private String password;  
        @Value("${spring.redis.database:0}")  
        private Integer database;  
  
        @Value("${spring.redis.jedis.pool.max-active:8}")  
        private Integer maxActive;  
        @Value("${spring.redis.jedis.pool.max-idle:8}")  
        private Integer maxIdle;  
        @Value("${spring.redis.jedis.pool.max-wait:-1}")  
        private Long maxWait;  
        @Value("${spring.redis.jedis.pool.min-idle:0}")  
        private Integer minIdle;  
  
        @Bean  
        public JedisPoolConfig jedisPool() {  
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();  
            jedisPoolConfig.setMaxIdle(maxIdle);  
            jedisPoolConfig.setMaxWaitMillis(maxWait);  
            jedisPoolConfig.setMaxTotal(maxActive);  
            jedisPoolConfig.setMinIdle(minIdle);  
            return jedisPoolConfig;  
        }  
  
        @Bean  
        public RedisStandaloneConfiguration jedisConfig() {  
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();  
            config.setHostName(host);  
            config.setPort(port);  
            config.setDatabase(database);  
            config.setPassword(RedisPassword.of(password));  
            return config;  
        }  
    }  
}
```

## 1.4、测试

测试主要就是查看下RedisTemplate的连接工厂类，到底是啥，简单的是截图如下
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404292250704.png)


# 2、其他
## 2.1、项目

工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/121-redis-jedis-config](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/121-redis-jedis-config)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)

来自：[【DB系列】Redis之Jedis配置](https://spring.hhui.top/spring-blog/2018/11/01/181101-SpringBoot%E9%AB%98%E7%BA%A7%E7%AF%87Redis%E4%B9%8BJedis%E9%85%8D%E7%BD%AE/#0-%E9%A1%B9%E7%9B%AE)

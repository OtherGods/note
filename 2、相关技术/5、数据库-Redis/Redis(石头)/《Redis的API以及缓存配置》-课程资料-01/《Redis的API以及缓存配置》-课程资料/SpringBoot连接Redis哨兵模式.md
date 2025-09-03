# 1.application.yml

```yml
###################以下为Redis增加的配置###########################
spring:
  redis:
    #单机配置
    # host: 122.51.50.249
    # port: 6380
    timeout: 6000
    # password: 123456
    ###################以下为redis哨兵增加的配置###########################
    sentinel:
      nodes: 122.51.50.249:26379,122.51.50.249:26380,122.51.50.249:26381
      master: mymaster
    ###################以下为lettuce连接池增加的配置###########################
    lettuce:
      pool:
        max-active:  100 # 连接池最大连接数（使用负值表示没有限制）
        max-idle: 100 # 连接池中的最大空闲连接
        min-idle: 50 # 连接池中的最小空闲连接
        max-wait: 6000 # 连接池最大阻塞等待时间（使用负值表示没有限制
```
# 2.配置文件

**RedisSentinelConfig.java**

```java
package com.bruceliu.config;

/**
 * @author bruceliu
 * @create 2019-10-30 14:28
 * @description
 */
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "spring.redis.sentinel")
public class RedisSentinelConfig {
    private Set<String> nodes;
    private String master;

    @Value("${spring.redis.timeout}")
    private long timeout;
    //@Value("${spring.redis.password}")
    //private String password;
    @Value("${spring.redis.lettuce.pool.max-idle}")
    private int maxIdle;
    @Value("${spring.redis.lettuce.pool.min-idle}")
    private int minIdle;
    @Value("${spring.redis.lettuce.pool.max-wait}")
    private long maxWait;
    @Value("${spring.redis.lettuce.pool.max-active}")
    private int maxActive;


    @Bean
    public RedisConnectionFactory lettuceConnectionFactory() {
        RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration(master, nodes);
        //redisSentinelConfiguration.setPassword(RedisPassword.of(password.toCharArray()));
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxIdle(maxIdle);
        genericObjectPoolConfig.setMinIdle(minIdle);
        genericObjectPoolConfig.setMaxTotal(maxActive);
        genericObjectPoolConfig.setMaxWaitMillis(maxWait);
        LettucePoolingClientConfiguration lettuceClientConfiguration = LettucePoolingClientConfiguration.builder()
                .poolConfig(genericObjectPoolConfig)
                .build();
        return new LettuceConnectionFactory(redisSentinelConfiguration, lettuceClientConfiguration);
    }

    public void setNodes(Set<String> nodes) {
        this.nodes = nodes;
    }

    public void setMaster(String master) {
        this.master = master;
    }
}
```
**RedisTemplateConfig.java**

```java
package com.bruceliu.config;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

/*
 * 将默认序列化改为Jackson2JsonRedisSerializer序列化，
 * */
@Configuration
public class RedisTemplateConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory) {
        //创建Json序列化对象
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);

        //解决查询缓存转换异常的问题
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // 将默认序列化改为Jackson2JsonRedisSerializer序列化
        RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setKeySerializer(jackson2JsonRedisSerializer);// key序列化
        template.setValueSerializer(jackson2JsonRedisSerializer);// value序列化
        template.setHashKeySerializer(jackson2JsonRedisSerializer);// Hash key序列化
        template.setHashValueSerializer(jackson2JsonRedisSerializer);// Hash value序列化
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }
}

```

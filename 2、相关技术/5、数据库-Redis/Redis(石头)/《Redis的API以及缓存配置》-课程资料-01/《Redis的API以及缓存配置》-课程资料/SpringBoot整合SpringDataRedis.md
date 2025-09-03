# 1. SpringDataRedis简介

## 1.1.Jedis（过时）

Jedis是Redis官方推出的一款面向Java的客户端，提供了很多接口供Java语言调用。可以在Redis官网下载，当然还有一些开源爱好者提供的客户端，如Jredis、SRP等等，推荐使用Jedis。
## 1.2.Spring Data Redis（推荐）

Spring-data-redis是spring大家族的一部分，提供了在srping应用中通过简单的配置访问redis服务，对reids底层开发包(Jedis,  JRedis, and RJC)进行了高度封装，RedisTemplate提供了redis各种操作、异常处理及序列化，支持发布订阅，并对spring 3.1 cache进行了实现。
spring-data-redis针对jedis提供了如下功能：
1.连接池自动管理，提供了一个高度封装的“RedisTemplate”类
2.针对jedis客户端中大量api进行了归类封装,将同一类型操作封装为operation接口

>	ValueOperations：简单K-V操作
	SetOperations：set类型数据操作
	ZSetOperations：zset类型数据操作
	HashOperations：针对map类型的数据操作
	ListOperations：针对list类型的数据操作
# 2.Spring Data Redis入门小Demo

## 2.1.准备工作

（1）构建Maven工程SpringDataRedisDemo
（2）引入Springboot和SpringDataRedis依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- lookup parent from repository -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.4.RELEASE</version>
        <relativePath/>
    </parent>

    <groupId>com.bruceliu.redis.pubsub</groupId>
    <artifactId>springboot-redis</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <!-- springBoot 的启动器 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Data Redis 的启动器 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>2.9.0</version>
        </dependency>
    </dependencies>

</project>
```
（4）在src/main/resources下创建properties文件夹，建立application.properties

```java
spring.redis.jedis.pool.max-idle=10
spring.redis.jedis.pool.min-idle=5
spring.redis.pool.max-total=20
spring.redis.hostName=122.51.50.249
spring.redis.port=6379
```
(5) 添加Redis的配置类
添加Redis的java配置类，设置相关的信息。

```java
package com.bruceliu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author bruceliu
 * @create 2019-10-29 11:55
 * @description
 */
@Configuration
public class RedisConfig {

    /**
     * 1.创建JedisPoolConfig对象。在该对象中完成一些链接池配置
     * @ConfigurationProperties:会将前缀相同的内容创建一个实体。
     */
    @Bean
    @ConfigurationProperties(prefix="spring.redis.pool")
    public JedisPoolConfig jedisPoolConfig(){
        JedisPoolConfig config = new JedisPoolConfig();
		/*//最大空闲数
		config.setMaxIdle(10);
		//最小空闲数
		config.setMinIdle(5);
		//最大链接数
		config.setMaxTotal(20);*/
        System.out.println("默认值："+config.getMaxIdle());
        System.out.println("默认值："+config.getMinIdle());
        System.out.println("默认值："+config.getMaxTotal());
        return config;
    }

    /**
     * 2.创建JedisConnectionFactory：配置redis链接信息
     */
    @Bean
    @ConfigurationProperties(prefix="spring.redis")
    public JedisConnectionFactory jedisConnectionFactory(JedisPoolConfig config){
        System.out.println("配置完毕："+config.getMaxIdle());
        System.out.println("配置完毕："+config.getMinIdle());
        System.out.println("配置完毕："+config.getMaxTotal());

        JedisConnectionFactory factory = new JedisConnectionFactory();
        //关联链接池的配置对象
        factory.setPoolConfig(config);
        //配置链接Redis的信息
        //主机地址
		/*factory.setHostName("192.168.70.128");
		//端口
		factory.setPort(6379);*/
        return factory;
    }

    /**
     * 3.创建RedisTemplate:用于执行Redis操作的方法
     */
    @Bean
    public RedisTemplate<String,Object> redisTemplate(JedisConnectionFactory factory){
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        //关联
        template.setConnectionFactory(factory);

        //为key设置序列化器
        template.setKeySerializer(new StringRedisSerializer());
        //为value设置序列化器
        template.setValueSerializer(new StringRedisSerializer());

        return template;
    }
}


```
##### 2.2.实体类

```java
package com.bruceliu.bean;

import java.io.Serializable;

/**
 * @author bruceliu
 * @create 2019-10-29 11:56
 * @description
 */
public class Users implements Serializable {

    private static final long serialVersionUID = 6206472024994638411L;

    private Integer id;
    private String name;
    private Integer age;
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getAge() {
        return age;
    }
    public void setAge(Integer age) {
        this.age = age;
    }
    @Override
    public String toString() {
        return "Users [id=" + id + ", name=" + name + ", age=" + age + "]";
    }

}


```
# 3. 字符串类型操作

```java
package com.bruceliu.test;

import com.bruceliu.APP;
import com.bruceliu.bean.Users;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author bruceliu
 * @create 2019-10-29 11:59
 * @description
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = APP.class)
public class SpringbootRedisDemoApplicationTests {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 添加一个字符串
     */
    @Test
    public void testSet(){
        this.redisTemplate.opsForValue().set("key", "bruceliu...");
    }

    /**
     * 获取一个字符串
     */
    @Test
    public void testGet(){
        String value = (String)this.redisTemplate.opsForValue().get("key");
        System.out.println(value);
    }

    /**
     * 添加Users对象
     */
    @Test
    public void testSetUesrs(){
        Users users = new Users();
        users.setAge(20);
        users.setName("张三丰");
        users.setId(1);
        //重新设置序列化器
        this.redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        this.redisTemplate.opsForValue().set("users", users);
    }

    /**
     * 取Users对象
     */
    @Test
    public void testGetUsers(){
        //重新设置序列化器
        this.redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        Users users = (Users)this.redisTemplate.opsForValue().get("users");
        System.out.println(users);
    }

    /**
     * 基于JSON格式存Users对象
     */
    @Test
    public void testSetUsersUseJSON(){
        Users users = new Users();
        users.setAge(20);
        users.setName("李四丰");
        users.setId(1);
        this.redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Users.class));
        this.redisTemplate.opsForValue().set("users_json", users);
    }

    /**
     * 基于JSON格式取Users对象
     */
    @Test
    public void testGetUseJSON(){
        this.redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Users.class));
        Users users = (Users)this.redisTemplate.opsForValue().get("users_json");
        System.out.println(users);
    }

}


```

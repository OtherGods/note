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
# 4. SET类型操作

```java
package com.bruceliu.test;

import com.bruceliu.APP;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

/**
 * @author bruceliu
 * @create 2019-10-29 12:10
 * @description
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = APP.class)
public class TestSet {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void setValue() {
        redisTemplate.boundSetOps("nameset").add("曹操");
        redisTemplate.boundSetOps("nameset").add("刘备");
        redisTemplate.boundSetOps("nameset").add("孙权");
    }

    @Test
    public void getValue() {
        Set set = redisTemplate.boundSetOps("nameset").members();
        System.out.println(set);
    }

    @Test
    public void removeValue() {
        redisTemplate.boundSetOps("nameset").remove("孙权");
    }

    @Test
    public void delete() {
        redisTemplate.delete("nameset");
    }
}
```
# 5. List类型操作

```java
package com.bruceliu.test;

import com.bruceliu.APP;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Set;

/**
 * @author bruceliu
 * @create 2019-10-29 12:12
 * @description
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = APP.class)
public class TestList {

    @Autowired
    private RedisTemplate redisTemplate;

    /*
     * 右压栈 : 后添加的元素排在后边
     */
    @Test
    public void testSetValue1() {
        redisTemplate.boundListOps("namelist1").rightPush("刘备");
        redisTemplate.boundListOps("namelist1").rightPush("关羽");
        redisTemplate.boundListOps("namelist1").rightPush("张飞");
    }

    /**
     * 显示右压栈的值
     */
    @Test
    public void testGetValue1() {
        List list = redisTemplate.boundListOps("namelist1").range(0, 10);
        System.out.println(list);
    }

    @Test
    public void delete() {
        redisTemplate.delete("namelist1");
    }

    /**
     * 左压栈
     */
    @Test
    public void testSetValue2() {
        redisTemplate.boundListOps("namelist2").leftPush("刘备");
        redisTemplate.boundListOps("namelist2").leftPush("关羽");
        redisTemplate.boundListOps("namelist2").leftPush("张飞");
    }

    /**
     * 显示左压栈的值
     */
    @Test
    public void testGetValue2() {
        List list = redisTemplate.boundListOps("namelist2").range(0, 10);
        System.out.println(list);
    }

    /**
     * 删除值
     */
    @Test
    public void removeValue() {
        redisTemplate.boundListOps("namelist1").remove(0, "刘备");
    }
}

```
# 6. Hash类型操作
```java
package com.bruceliu.test;

import com.bruceliu.APP;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Set;

/**
 * @author bruceliu
 * @create 2019-10-29 12:14
 * @description
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = APP.class)
public class TestHash {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 存值
     */
    @Test
    public void testSetValue() {
        redisTemplate.boundHashOps("namehash").put("a", "唐僧");
        redisTemplate.boundHashOps("namehash").put("b", "悟空");
        redisTemplate.boundHashOps("namehash").put("c", "八戒");
        redisTemplate.boundHashOps("namehash").put("d", "沙僧");
    }

    /**
     * 获取所有的key
     */
    @Test
    public void testGetKes() {
        Set keys = redisTemplate.boundHashOps("namehash").keys();
        System.out.println(keys);
    }

    /**
     * 获取所有的值
     */
    @Test
    public void testGetValues() {
        List list = redisTemplate.boundHashOps("namehash").values();
        System.out.println(list);
    }

    /**
     * 根据KEY取值
     */
    @Test
    public void searchValueByKey() {
        String str = (String) redisTemplate.boundHashOps("namehash").get("b");
        System.out.println(str);
    }

    /**
     * 移除某个小key的值
     */
    @Test
    public void removeValue() {
        redisTemplate.boundHashOps("namehash").delete("c");
    }
}

```
# 7. Zset类型操作

```java
package com.bruceliu.test;

import com.bruceliu.APP;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author bruceliu
 * @create 2019-10-29 12:17
 * @description
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = APP.class)
public class TestZset {

    @Autowired
    RedisTemplate redisTemplate;

    /**
     * Boolean add(V value, double score);
     * 添加元素到变量中同时指定元素的分值。
     */
    @Test
    public void testAdd() {
        redisTemplate.boundZSetOps("zSetValue").add("A", 1);
        redisTemplate.boundZSetOps("zSetValue").add("B", 3);
        redisTemplate.boundZSetOps("zSetValue").add("C", 2);
        redisTemplate.boundZSetOps("zSetValue").add("D", 5);
        redisTemplate.boundZSetOps("zSetValue").add("E", 4);
    }

    /**
     * Set<V> range(long start, long end);
     * 获取变量指定区间的元素。
     * 通过索引区间返回有序集合成指定区间内的成员，其中有序集成员按分数值递增(从小到大)顺序排列
     */
    @Test
    public void testRange(){
        Set zSetValue = redisTemplate.boundZSetOps("zSetValue").range(0,-1);
        System.out.println("获取指定区间的元素:" + zSetValue);
    }

    /**
     * 新增一个有序集合
     */
    @Test
    public void testAdd1(){
        ZSetOperations.TypedTuple<Object> objectTypedTuple1 = new DefaultTypedTuple<Object>("zset-5",9.6);
        ZSetOperations.TypedTuple<Object> objectTypedTuple2 = new DefaultTypedTuple<Object>("zset-6",9.9);
        Set<ZSetOperations.TypedTuple<Object>> tuples = new HashSet<ZSetOperations.TypedTuple<Object>>();
        tuples.add(objectTypedTuple1);
        tuples.add(objectTypedTuple2);

        System.out.println(redisTemplate.boundZSetOps("zset1").add(tuples));

        System.out.println(redisTemplate.boundZSetOps("zset1").range(0,-1));
    }


    /**
     * 从有序集合中移除一个或者多个元素
     */
    @Test
    public void testRemove(){
        System.out.println(redisTemplate.boundZSetOps("zset1").range(0,-1));
        System.out.println(redisTemplate.boundZSetOps("zset1").remove("zset1","zset-6"));
        System.out.println(redisTemplate.boundZSetOps("zset1").range(0,-1));
    }

    /**
     * 增加元素的score值，并返回增加后的值
     */
    @Test
    public void testIncrementScore(){
        System.out.println(redisTemplate.boundZSetOps("zset1").incrementScore("zset-1",1.1));  //原为1.1
    }

    /**
     * 返回有序集中指定成员的排名，其中有序集成员按分数值递增(从小到大)顺序排列
     */
    @Test
    public void testRank(){
        System.out.println(redisTemplate.boundZSetOps("zset1").range(0,-1));
        System.out.println(redisTemplate.boundZSetOps("zset1").rank("zset-5"));
    }

    /**
     * 返回有序集中指定成员的排名，其中有序集成员按分数值递减(从大到小)顺序排列
     */
    @Test
    public void testReverseRank(){
        System.out.println(redisTemplate.boundZSetOps("zset1").range(0,-1));
        System.out.println(redisTemplate.boundZSetOps("zset1").reverseRank("zset-5"));
    }

    /**
     * 通过索引区间返回有序集合成指定区间内的成员对象，其中有序集成员按分数值递增(从小到大)顺序排列
     */
    @Test
    public void testRangeWithScore(){
        Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.boundZSetOps("zset1").rangeWithScores(0,-1);
        Iterator<ZSetOperations.TypedTuple<Object>> iterator = tuples.iterator();
        while (iterator.hasNext()) {
            ZSetOperations.TypedTuple<Object> typedTuple = iterator.next();
            System.out.println("value:" + typedTuple.getValue() + "score:" + typedTuple.getScore());
        }
    }

    /**
     * 获取有序集合的成员数
     */
    @Test
    public void testzCard(){
        System.out.println(redisTemplate.boundZSetOps("zset1").zCard());
    }

    /**
     * 移除指定索引位置的成员，其中有序集成员按分数值递增(从小到大)顺序排列
     */
    @Test
    public void testRemoveRange(){
        System.out.println(redisTemplate.boundZSetOps("zset1").range(0,-1));
        redisTemplate.boundZSetOps("zset1").removeRange(1,2);
        System.out.println(redisTemplate.boundZSetOps("zset1").range(0,-1));
    }
}
```


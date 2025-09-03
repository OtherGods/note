基本上，主要是后端，现在都离不开redis这个知识点，在我们的技术派项目，也同样使用了redis来做一些相关业务支持（如缓存、排行榜、计数器等）

由于redis的知识点相对较多，所以我们下来看一下，如何在我们的实际项目中，引入redis

# 1、Redis集成
## 1.1、配置依赖

Spring-Boot提供了非常简单的redis集成，引入一个包，并配置上对应的redis相关连接配置即可以
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

当我们使用默认的127.0.0.1，端口号3379，无密码时，你完全无需在配置文件中做任何其他操作，直接在需要的地方，引入RestTemplate就可以直接使用了，如：
```java
@SpringBootApplication
public class Application {

    public Application(RedisTemplate<String, String> redisTemplate) {
        // 往Redis中写入key=hello, value=world
        redisTemplate.opsForValue().set("hello", "world");
        // 查询redis中key=hello的值
        String ans = redisTemplate.opsForValue().get("hello");
        Assert.isTrue("world".equals(ans));
	}
	
	public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 1.2、自定义参数配置

在技术派中，自定义的参数配置在 `paicoding\paicoding-web\src\main\resources-env\dev\application-dal.yml` （本机开发时，默认是dev环境）
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308282245731.png)

但是，若当我们需要添加连接池相关配置时，如
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308282246428.png)

此时，我们可能需要在pom.xml中添加
```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

## 1.3、多Redis连接配置

虽然实际的项目中，不太可能出现一个项目连接多个redis示例的情况，但是，当真的出现了，也是允许的，这种时候，就不能直接使用默认的，需要我们自己来声明ConnectionFactory和RedisTemplate

配置如下：
```yml
spring:
	redis:
		host: 127.0.0.1
		port: 6379
		password:
		lettuce:
			pool:
				max-active: 32
				max-wait: 300
				max-idle: 16
				min-idle: 8
		database: 0
	local-redis:
	    host: 127.0.0.1
	    port: 6379
	    database: 0
	    password:
	    lettuce:
			pool:
				max-active: 16
				max-wait: 100
				max-idle: 8
				min-idle: 4
```

对应的配置类，采用Lettuce，基本设置如下，套路都差不多，先读取配置，初始化ConnectionFactory，然后创建RedisTemplate实例，设置连接工厂
```java
@Configuration
public class RedisAutoConfig {

    @Bean
    public LettuceConnectionFactory defaultLettuceConnectionFactory
				(RedisStandaloneConfiguration defaultRedisConfig,
	            GenericObjectPoolConfig defaultPoolConfig) {
		LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder().commandTimeout(Duration.ofMillis(100))
				.poolConfig(defaultPoolConfig).build();
		
		return new LettuceConnectionFactory(defaultRedisConfig, clientConfig);
	}
	
	@Bean
    public RedisTemplate<String, String> defaultRedisTemplate(
            LettuceConnectionFactory defaultLettuceConnectionFactory) {
		RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(defaultLettuceConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
	}
	
	@Bean
    @ConditionalOnBean(name = "localRedisConfig")
    public LettuceConnectionFactory localLettuceConnectionFactory(RedisStandaloneConfiguration localRedisConfig,
            GenericObjectPoolConfig localPoolConfig) {
		LettuceClientConfiguration clientConfig =
                LettucePoolingClientConfiguration.builder().commandTimeout(Duration.ofMillis(100))
                        .poolConfig(localPoolConfig).build();
		return new LettuceConnectionFactory(localRedisConfig, clientConfig);
	}
	
	@Bean
    @ConditionalOnBean(name = "localLettuceConnectionFactory")
    public RedisTemplate<String, String> localRedisTemplate(LettuceConnectionFactory localLettuceConnectionFactory) {
	    RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(localLettuceConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
    
    @Configuration
    @ConditionalOnProperty(name = "host", prefix = "spring.local-redis")
    public static class LocalRedisConfig {
	    @Value("${spring.local-redis.host:127.0.0.1}")
        private String host;
        @Value("${spring.local-redis.port:6379}")
        private Integer port;
        @Value("${spring.local-redis.password:}")
        private String password;
        @Value("${spring.local-redis.database:0}")
        private Integer database;
        
        @Value("${spring.local-redis.lettuce.pool.max-active:8}")
        private Integer maxActive;
        @Value("${spring.local-redis.lettuce.pool.max-idle:8}")
        private Integer maxIdle;
        @Value("${spring.local-redis.lettuce.pool.max-wait:-1}")
        private Long maxWait;
        @Value("${spring.local-redis.lettuce.pool.min-idle:0}")
        private Integer minIdle;
        
        @Bean
        public GenericObjectPoolConfig localPoolConfig() {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal(maxActive);
            config.setMaxIdle(maxIdle);
            config.setMinIdle(minIdle);
            config.setMaxWaitMillis(maxWait);
            return config;
        }
        
        @Bean
        public RedisStandaloneConfiguration localRedisConfig() {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(host);
            config.setPassword(RedisPassword.of(password));
            config.setPort(port);
            config.setDatabase(database);
            return config;
        }
    }
    
    @Configuration
    public static class DefaultRedisConfig {
        @Value("${spring.redis.host:127.0.0.1}")
        private String host;
        @Value("${spring.redis.port:6379}")
        private Integer port;
        @Value("${spring.redis.password:}")
        private String password;
        @Value("${spring.redis.database:0}")
        private Integer database;
        
        @Value("${spring.redis.lettuce.pool.max-active:8}")
        private Integer maxActive;
        @Value("${spring.redis.lettuce.pool.max-idle:8}")
        private Integer maxIdle;
        @Value("${spring.redis.lettuce.pool.max-wait:-1}")
        private Long maxWait;
        @Value("${spring.redis.lettuce.pool.min-idle:0}")
        private Integer minIdle;
        
        @Bean
        public GenericObjectPoolConfig defaultPoolConfig() {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal(maxActive);
            config.setMaxIdle(maxIdle);
            config.setMinIdle(minIdle);
            config.setMaxWaitMillis(maxWait);
            return config;
        }
        
        @Bean
        public RedisStandaloneConfiguration defaultRedisConfig() {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(host);
            config.setPassword(RedisPassword.of(password));
            config.setPort(port);
            config.setDatabase(database);
            return config;
		}
	}
}
```

测试类如下，简单的演示下两个template的读写
```java
@SpringBootApplication
public class Application {

    public Application(RedisTemplate<String, String> localRedisTemplate, RedisTemplate<String, String>
            defaultRedisTemplate)
            throws InterruptedException {
		// 10s的有效时间
        localRedisTemplate.delete("key");
        localRedisTemplate.opsForValue().set("key", "value", 100, TimeUnit.MILLISECONDS);
        String ans = localRedisTemplate.opsForValue().get("key");
        System.out.println("value".equals(ans));
        TimeUnit.MILLISECONDS.sleep(200);
        ans = localRedisTemplate.opsForValue().get("key");
        System.out.println("value".equals(ans) + " >> false ans should be null! ans=[" + ans + "]");
        
        defaultRedisTemplate.opsForValue().set("key", "value", 100, TimeUnit.MILLISECONDS);
        ans = defaultRedisTemplate.opsForValue().get("key");
        System.out.println(ans);
    }
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

上面的代码执行演示如下
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308282331952.gif)

上面的演示为动图，抓一下重点：
1. 注意 localRedisTemplate, defaultRedisTemplate 两个对象不相同（看debug窗口后面的@xxx)
2. 同样两个RedisTemplate的ConnectionFactory也是两个不同的实例（即分别对应前面配置类中的两个Factory)
3. 执行后输出的结果正如我们预期的redis操作
	1. 塞值，马上取出没问题
	2. 失效后，再查询，返回null
4. 最后输出异常日志，提示如下
```java
Description:

Parameter 0 of method redisTemplate in org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration required a single bean, but 2 were found:
- defaultLettuceConnectionFactory: defined by method 'defaultLettuceConnectionFactory' in class path resource [com/git/hui/boot/redis/config/RedisAutoConfig.class]
- localLettuceConnectionFactory: defined by method 'localLettuceConnectionFactory' in class path resource [com/git/hui/boot/redis/config/RedisAutoConfig.class]

Action:
Consider marking one of the beans as @Primary, updating the consumer to accept multiple beans, or using @Qualifier to identify the bean that should be consumed
```

上面表示说有多个ConnectionFactory存在，然后创建默认的RedisTemplate就不知道该选择哪一个了，有两种方法:
1. 方法一：指定默认的ConnectionFactory
	1. 借助@Primary来指定默认的连接工厂，然后在使用工程的时候，通过@Qualifier注解来显示指定，我需要的工厂是哪个（主要是localRedisTemplate这个bean的定义，如果不加，则会根据defaultLettuceConnectionFactory这个实例来创建Redis连接了）
	2. **`@Primary`注解：** 是 Spring 框架中的一个注解，用于解决当存在多个符合条件的 Bean 时(Spring 无法确定应该选择哪个 Bean)，优先选择一个 Bean 进行注入的问题。它通过标记一个默认的 Bean，使其在自动装配时被优先考虑。
		- **应用场景**：解决多个候选 Bean 的自动装配冲突问题。

	   **`@Qualifier` 注解：** 在某些情况下你需要特定的 Bean 而不是 `@Primary` 标记的 Bean，可以使用 `@Qualifier` 注解指定特定的 Bean

	   **总结：** **`@Primary` 注解** 用于在存在多个相同类型的 Bean 时，指定一个默认的 Bean; 结合 `@Qualifier` 使用：在需要时可以使用 `@Qualifier` 注解来指定特定的 Bean，从而覆盖 `@Primary` 的默认行为。
	   ```java
@Bean
@Primary
public LettuceConnectionFactory defaultLettuceConnectionFactory(RedisStandaloneConfiguration defaultRedisConfig,
        GenericObjectPoolConfig defaultPoolConfig) {
    // ...
}

@Bean
public RedisTemplate<String, String> defaultRedisTemplate(
        @Qualifier("defaultLettuceConnectionFactory") LettuceConnectionFactory defaultLettuceConnectionFactory) {
    // ....
}

@Bean
@ConditionalOnBean(name = "localRedisConfig")
public LettuceConnectionFactory localLettuceConnectionFactory(RedisStandaloneConfiguration localRedisConfig,
        GenericObjectPoolConfig localPoolConfig) {
    // ...
}

@ConditionalOnBean(name = "localLettuceConnectionFactory")
public RedisTemplate<String, String> localRedisTemplate(
        @Qualifier("localLettuceConnectionFactory") LettuceConnectionFactory localLettuceConnectionFactory) {
    // ...
}
	   ```

2. 方法二：忽略默认的自动配置类
   既然提示的是`org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration`类加载bean冲突，那么就不加载这个配置即可
   ```java
@SpringBootApplication
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class})
public class Application {
  // ...
}
   ```

## 1.4、Redis集群

在实际的生产环境中，redis通常是以集群的方式提供服务的，因此当我们的服务需要连接的是redis集群时，可以如下配置
```yml
spring:
  redis:
    password:
    cluster:
      nodes: 192.168.0.203:7000,192.168.0.203:7001,192.168.0.203:7002
      max-redirects: 3
    lettuce:
      pool:
        max-idle: 16
        max-active: 32
        min-idle: 8
```

与前面的区别在于，cluster.nodes
在让我们自己来搭建redis集群时，有几个坑很容易掉进去，重点说明一下：
- 注释bind 127.0.0.1，允许其他机器访问
- 在创建集群时，使用局域网ip而不是127.0.0.1
有兴趣的小伙伴可以翻一下：[【DB系列】Redis集群环境配置 | 一灰灰Blog](https://spring.hhui.top/spring-blog/2019/09/27/190927-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8B%E4%B9%8BRedis%E9%9B%86%E7%BE%A4%E7%8E%AF%E5%A2%83%E9%85%8D%E7%BD%AE/)

## 1.5、Redis操作封装类RedisClient

最后再来看一下，技术派中redis集成完毕之后是怎么使用的，我们封装了一个基于RedisTemplate的RedisClient工具类，再项目启动之时，主动注入RestTemplate
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308282349507.png)

RedisClient当前封装了redis的几种数据结构的使用姿势，主要是为了简化调用者的使用成本：
```java
public class RedisClient {  
    private static final Charset CODE = StandardCharsets.UTF_8;  
    private static final String KEY_PREFIX = "pai_";  
    private static RedisTemplate<String, String> template;  
  
    public static void register(RedisTemplate<String, String> template) {  
        RedisClient.template = template;  
    }  
  
    public static void nullCheck(Object... args) {  
        for (Object obj : args) {  
            if (obj == null) {  
                throw new IllegalArgumentException("redis argument can not be null!");  
            }  
        }  
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
  
    /**  
     * 生成技术派的缓存key  
     *     * @param key  
     * @return  
     */  
    public static byte[] keyBytes(String key) {  
        nullCheck(key);  
        key = KEY_PREFIX + key;  
        return key.getBytes(CODE);  
    } 
  
    /**  
     * 查询缓存  
     *  
     * @param key  
     * @return  
     */  
    public static String getStr(String key) {  
        return template.execute((RedisCallback<String>) con -> {  
            byte[] val = con.get(keyBytes(key));  
            return val == null ? null : new String(val);  
        });  
    }  
  
    /**  
     * 设置缓存  
     *  
     * @param key  
     * @param value  
     */  
    public static void setStr(String key, String value) {  
        template.execute((RedisCallback<Void>) con -> {  
            con.set(keyBytes(key), valBytes(value));  
            return null;        });  
    }  
  
    /**  
     * 删除缓存  
     *  
     * @param key  
     */  
    public static void del(String key) {  
        template.execute((RedisCallback<Long>) con -> con.del(keyBytes(key)));  
    }
}
```

上面的几个方法上也带上了，总的来说理解起来比较简单；不过大家可以思考一下，为啥这里的都是使用redisTemplate.execute()方法，而不是类似template.opsForValue().get()的使用姿势
# 2、小结

这一篇内容比较简单，但是强烈推荐有兴趣的小伙伴实操一下；新建一个springboot项目，连本机的redis，连远程的redis；一个项目配置多个redis；连接redis集群，都有哪些区别

关于redis的实际使用姿势，我们下篇再来介绍

# 3、扩展阅读

- [【DB系列】Redis之基本配置 | 一灰灰blog](https://spring.hhui.top/spring-blog/2018/10/29/181029-SpringBoot%E9%AB%98%E7%BA%A7%E7%AF%87Redis%E4%B9%8B%E5%9F%BA%E6%9C%AC%E9%85%8D%E7%BD%AE/)
- [【DB系列】Redis之Jedis配置 | 一灰灰Blog](https://spring.hhui.top/spring-blog/2018/11/01/181101-SpringBoot%E9%AB%98%E7%BA%A7%E7%AF%87Redis%E4%B9%8BJedis%E9%85%8D%E7%BD%AE/)
- [【DB系列】Redis集群环境配置 | 一灰灰Blog](https://spring.hhui.top/spring-blog/2019/09/27/190927-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8B%E4%B9%8BRedis%E9%9B%86%E7%BE%A4%E7%8E%AF%E5%A2%83%E9%85%8D%E7%BD%AE/)
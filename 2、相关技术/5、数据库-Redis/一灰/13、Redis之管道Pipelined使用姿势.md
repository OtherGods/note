
redis管道技术，可以在服务端未响应时，客户端可以继续向服务端发送请求，并最终一次性读取所有服务端的响应，这种技术可以很方便的支持我们的批量请求，下面简单介绍下如何使用`RedisTemplate`来使用管道

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

这里我们主要借助`org.springframework.data.redis.core.RedisTemplate#executePipelined(org.springframework.data.redis.core.RedisCallback<?>)`，如下
```java
@Component  
public class PipelineBean {  
  
    private RedisTemplate<String, String> redisTemplate;  
  
    public PipelineBean(RedisTemplate<String, String> redisTemplate) {  
        this.redisTemplate = redisTemplate;  
    }  
  
  
    public void counter(String prefix, String key, String target) {  
        // 请注意，返回的结果与内部的redis操作顺序是匹配的  
        List<Object> res = redisTemplate.executePipelined(new RedisCallback<Long>() {  
            @Override  
            public Long doInRedis(RedisConnection redisConnection) throws DataAccessException {  
                String mapKey = prefix + "_mp_" + key;  
                String cntKey = prefix + "_cnt_" + target;  
      
                redisConnection.openPipeline();  
                redisConnection.incr(mapKey.getBytes());  
                redisConnection.incr(cntKey.getBytes());  
                return null;  
            }  
        });  
        System.out.println(res);  
    }  
}
```

上面的使用中，有几个注意事项

- `redisConnection.openPipeline();` 开启管道
- 返回结果为列表，内部第一个redis操作，对应的返回结果塞在列表的下标0；依次…

# 2、其他
## 2.1、项目

工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)


转载自：[【DB系列】Redis之管道Pipelined使用姿势](https://spring.hhui.top/spring-blog/2020/04/11/200411-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8BRedis%E4%B9%8B%E7%AE%A1%E9%81%93Pipelined%E4%BD%BF%E7%94%A8%E5%A7%BF%E5%8A%BF/)


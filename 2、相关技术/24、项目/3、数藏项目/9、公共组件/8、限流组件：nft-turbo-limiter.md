![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503021633764.png)

在高并发的场景中，我们需要对单个时间内的同一类请求进行限制，以防止过多的请求在短时间内对系统造成太大压力。

这个组件中，我们封装了 Sentinel、Redis 以及Redisson等，其中用Redisson实现的限流器，这种方法利用Redis进行分布式限流，很适合高并发和分布式环境。
```xml
<dependencies>  
  
    <dependency>  
        <groupId>cn.hollis</groupId>  
        <artifactId>nft-turbo-cache</artifactId>  
    </dependency>  
  
    <!--    Sentinel    -->  
    <dependency>  
        <groupId>com.alibaba.cloud</groupId>  
        <artifactId>spring-cloud-alibaba-sentinel-gateway</artifactId>  
    </dependency>  
  
    <dependency>  
        <groupId>com.alibaba.cloud</groupId>  
        <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>  
    </dependency>  
  
    <!--    Sentinel集成Nacos持久化配置    -->  
    <dependency>  
        <groupId>com.alibaba.cloud</groupId>  
        <artifactId>spring-cloud-alibaba-sentinel-datasource</artifactId>  
    </dependency>  
  
    <dependency>  
        <groupId>com.alibaba.csp</groupId>  
        <artifactId>sentinel-datasource-nacos</artifactId>  
        <version>1.8.7</version>  
    </dependency>  
  
    <dependency>  
        <groupId>org.springframework.boot</groupId>  
        <artifactId>spring-boot-starter-test</artifactId>  
        <scope>test</scope>  
    </dependency>  
  
    <dependency>  
        <groupId>junit</groupId>  
        <artifactId>junit</artifactId>  
        <scope>test</scope>  
    </dependency>  
  
</dependencies>
```

定义了一个通用的滑动窗口限流器：
```java
import org.redisson.api.RRateLimiter;  
import org.redisson.api.RateIntervalUnit;  
import org.redisson.api.RateType;  
import org.redisson.api.RedissonClient;  
  
/**  
 * 滑动窗口限流服务  
 *  
 * @author Hollis 
 */
public class SlidingWindowRateLimiter implements RateLimiter {  
      
    private RedissonClient redissonClient;  
      
    public SlidingWindowRateLimiter(RedissonClient redissonClient) {  
        this.redissonClient = redissonClient;  
    }  
      
    @Override  
    public Boolean tryAcquire(String key, int limit, int windowSize) {  
        RRateLimiter rRateLimiter = redissonClient.getRateLimiter(key);  
          
        if (!rRateLimiter.isExists()) {  
            rRateLimiter.trySetRate(RateType.OVERALL, limit, windowSize, RateIntervalUnit.SECONDS);  
        }  
          
        return rRateLimiter.tryAcquire();  
    }  
}
```

### 限流方法逻辑解析

1.获取限流器实例：
```java
RRateLimiter rRateLimiter = redissonClient.getRateLimiter(key);
```

2.初始化限流器配置：
```java
if (!rRateLimiter.isExists()) {  
        rRateLimiter.trySetRate(RateType.OVERALL, limit, windowSize, RateIntervalUnit.SECONDS);  
}
```

判断限流器是否已经存在，如果不存在则进行配置。
- 使用`RateType.OVERALL`表示集群限流策略。
- 设置限流速率，即在`windowSize`秒内最多允许`limit`个请求。

3.尝试获取令牌：
```java
return rRateLimiter.tryAcquire();
```

尝试从限流器中获取令牌，如果成功则返回true，否则返回false。

### 限流实例配置

为了方便使用，我们自定义了 bean——slidingWindowRateLimiter：
```java
import cn.hollis.nft.turbo.limiter.SlidingWindowRateLimiter;  
import org.redisson.api.RedissonClient;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
  
/**  
 * @author Hollis 
 */
@Configuration  
public class RateLimiterConfiguration {  
      
    @Bean  
    public SlidingWindowRateLimiter slidingWindowRateLimiter(RedissonClient redisson) {  
        return new SlidingWindowRateLimiter(redisson);  
    }  
}
```

并且新建org.springframework.boot.autoconfigure.AutoConfiguration.imports，内容如下：

```java
cn.hollis.nft.turbo.limiter.configuration.RateLimiterConfiguration
```

### RRateLimiter 的实现原理

github 上有个原理解析写的挺好的，大家可以看一下：
[41.1、Redisson分布式限流器RRateLimiter原理解析](2、相关技术/5、数据库-Redis/Hollis/41.1、Redisson分布式限流器RRateLimiter原理解析.md)


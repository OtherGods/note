![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503042328169.png)

这是我们项目中的缓存组件，他主要集成了 Redis、Redisson、Caffeine 以及 JetCache 等多个缓存组件。
```java
<?xml version="1.0" encoding="UTF-8"?>  
<project xmlns="http://maven.apache.org/POM/4.0.0"  
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">  
    <modelVersion>4.0.0</modelVersion>  
    <parent>  
        <groupId>cn.hollis</groupId>  
        <artifactId>nft-turbo-common</artifactId>  
        <version>1.0.0-SNAPSHOT</version>  
    </parent>  
  
    <groupId>cn.hollis</groupId>  
    <artifactId>nft-turbo-cache</artifactId>  
    <description>缓存模块</description>  
    <version>1.0.0-SNAPSHOT</version>  
  
    <properties>  
        <maven.compiler.source>21</maven.compiler.source>  
        <maven.compiler.target>21</maven.compiler.target>  
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    </properties>  
  
  
    <dependencies>  
        <!--     Redis  -->  
        <dependency>  
            <groupId>org.springframework.boot</groupId>  
            <artifactId>spring-boot-starter-data-redis</artifactId>  
        </dependency>  
  
        <!--    Redisson    -->  
        <dependency>  
            <groupId>org.redisson</groupId>  
            <artifactId>redisson-spring-boot-starter</artifactId>  
            <version>3.24.3</version>  
        </dependency>  
  
        <!--    Caffeine    -->  
        <dependency>  
            <groupId>com.github.ben-manes.caffeine</groupId>  
            <artifactId>caffeine</artifactId>  
            <version>3.1.8</version>  
        </dependency>  
  
        <!--    JetCache    -->  
        <dependency>  
            <groupId>com.alicp.jetcache</groupId>  
            <artifactId>jetcache-starter-redisson</artifactId>  
            <version>2.7.5</version>  
            <exclusions>  
                <exclusion>  
                    <groupId>org.springframework.boot</groupId>  
                    <artifactId>spring-boot-starter-logging</artifactId>  
                </exclusion>  
            </exclusions>  
        </dependency>  
  
    </dependencies>  
  
</project>
```

同时，我们定义了一个 cache.yml 文件，在其中，把一些通用的配置放到里面配置好了，方面各个微服务在引用的时候可以简化一些通用化的配置：
```yml
spring:  
  data:  
    redis:  
      host: r-xxxxx.redis.rds.aliyuncs.com  
      port: 6379 # Redis服务器连接端口  
      password: 'xxxx' # Redis服务器连接密码（默认为空）  
      ssl:  
        enabled: true  
  redis:  
    redisson:  
      config: |  
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
jetcache:  
  statIntervalMinutes: 1  
  areaInCacheName: false  
  local:  
    default:  
      type: caffeine  
      keyConvertor: fastjson2  
  remote:  
    default:  
      type: redisson  
      keyConvertor: fastjson2  
      broadcastChannel: ${spring.application.name}  
      keyPrefix: ${spring.application.name}  
      valueEncoder: java  
      valueDecoder: java  
      defaultExpireInMillis: 5000
```

这里包含了 Redis、Redisson 以及 jetcache 的通用配置，一份配置配置好之后，其他的地方在需要引入缓存相关的内容，就不需要这么多额外的配置了。

其中`jetcache.remote.default.keyPrefix`的值是所有`key`的默认全局前缀
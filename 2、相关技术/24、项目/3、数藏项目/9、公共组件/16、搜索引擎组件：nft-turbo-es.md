![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092211012.png)

项目中用到了 ES 作为搜索引擎，所以我们把 es 相关的东西都封装到了 nft-turbo-es 这个组件中。

首先是 pom 依赖：
```xml
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
    <artifactId>nft-turbo-es</artifactId>  
    <version>1.0.0-SNAPSHOT</version>  
  
    <properties>  
        <maven.compiler.source>21</maven.compiler.source>  
        <maven.compiler.target>21</maven.compiler.target>  
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    </properties>  
  
    <dependencies>  
        <dependency>  
            <groupId>org.springframework.boot</groupId>  
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>  
        </dependency>  
  
        <dependency>  
            <groupId>org.elasticsearch.client</groupId>  
            <artifactId>elasticsearch-rest-high-level-client</artifactId>  
            <version>7.17.20</version>  
        </dependency>  
  
        <dependency>  
            <groupId>org.dromara.easy-es</groupId>  
            <artifactId>easy-es-boot-starter</artifactId>  
            <version>2.0.0-beta8</version>  
        </dependency>  
  
    </dependencies>  
  
</project>
```

spring-boot-starter-data-elasticsearch是 elasticsearch 的 spring boot的 starter，不需要多说，就是把es 相关配置、bean 都给你定义好了，开箱即用。

elasticsearch-rest-high-level-client和easy-es-boot-starter是两个客户端，一个是官方提供的(elasticsearch-rest-high-level-client)，一个是第三方的(easy-es-boot-starter)。

官方提供的用起来没那么舒服，需要掌握很多 ES 的语法，而 easy-es可以像 mybatis-plus 一样帮你屏蔽了底层的语法，像链接数据库一样操作 es。

所以这两个我们都依赖了进来，都可以用。

配置文件中没啥特别的，就是 es 和 easy-es 需要的一些配置，主要是地址、用户名、密码啥的。spring.elasticsearch.enable=true 的时候表示 ES 生效。
```yml
spring:  
  elasticsearch:  
    enable: ${nft.turbo.elasticsearch.enable}  
    uris: http://${nft.turbo.elasticsearch.url}  
    username: ${nft.turbo.elasticsearch.username}  
    password: ${nft.turbo.elasticsearch.password}  
easy-es:  
  enable: ${nft.turbo.elasticsearch.enable}  
  address : ${nft.turbo.elasticsearch.url}  
  username: ${nft.turbo.elasticsearch.username}  
  password: ${nft.turbo.elasticsearch.password}
```

这个组件中有一个 Config，就是这个EsConfiguration：
```java
package cn.hollis.nft.turbo.es.config;  
  
import org.dromara.easyes.starter.register.EsMapperScan;  
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;  
import org.springframework.context.annotation.Configuration;  
  
/**  
 * ES配置  
 *  
 * @author hollis */@Configuration  
@EsMapperScan("cn.hollis.nft.turbo.*.infrastructure.es.mapper")  
@ConditionalOnProperty(value = "easy-es.enable", havingValue = "true")  
public class EsConfiguration {  
      
}
```

这个类主要是给 easy-es用的，帮助我们实现开启 easy-es 的扫描的，主要就是@EsMapperScan这个注解，就是要告诉 easy-es 去哪里扫描相关的 mapper 类作为 EasyEs 的 mapper，如我们定义的：
```java
package cn.hollis.nft.turbo.collection.infrastructure.es.mapper;  
  
import cn.hollis.nft.turbo.collection.domain.entity.Collection;  
import org.dromara.easyes.core.kernel.BaseEsMapper;  
  
/**  
 * <p>  
 * 藏品信息 Mapper 接口  
 * </p>  
 * * @author hollis * @since 2024-05-05 */public interface CollectionEsMapper extends BaseEsMapper<Collection> {  
      
}
```

他的包路径cn.hollis.nft.turbo.collection.infrastructure.es.mapper正好是`@EsMapperScan("cn.hollis.nft.turbo.*.infrastructure.es.mapper")`
中配置的范围，那么在应用启动时，这个CollectionEsMapper就会被当做一个 Mapper 注册成为一个 bean，就像 MyBatis-Plus 中的 Mapper 一样，可以调用他里面内置的方法了，这些方法你可以自己定义，也可以直接用他的父类BaseEsMapper中的默认方法。

﻿

但是需要注意，EsConfiguration上面还有一个@ConditionalOnProperty(value = "easy-es.enable", havingValue = "true")，也就意味着，只有配置项中 easy-es.enable=true 的时候，这个配置才会生效，即制定的 mapper 目录下的 mapper 类才会被注册到Spring 中，才能用。

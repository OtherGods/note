数据源组件，其实就是把数据库的对接这一层封装到一起了，即 mysql、myabtis、mybatis-plus、sharding-jdbc、druid 等。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503042339855.png)

pom 依赖如下：
```xml
<dependencies>  
  
    <!--    Mybatis    -->  
    <dependency>  
        <groupId>org.mybatis.spring.boot</groupId>  
        <artifactId>mybatis-spring-boot-starter</artifactId>  
        <version>3.0.3</version>  
    </dependency>  
  
    <!--     Mybatis Plus    -->  
    <dependency>  
        <groupId>com.baomidou</groupId>  
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>  
        <version>3.5.5</version>  
    </dependency>  
  
    <!-- MyBatis-Plus Generator -->  
    <dependency>  
        <groupId>com.baomidou</groupId>  
        <artifactId>mybatis-plus-generator</artifactId>  
        <version>3.5.5</version>  
    </dependency>  
  
    <!-- MySQL JDBC 驱动 -->  
    <dependency>  
        <groupId>mysql</groupId>  
        <artifactId>mysql-connector-java</artifactId>  
        <version>8.0.27</version>  
    </dependency>  
  
    <!-- Druid 数据库连接池 -->  
    <dependency>  
        <groupId>com.alibaba</groupId>  
        <artifactId>druid</artifactId>  
        <version>1.2.20</version>  
    </dependency>  
  
    <!--shardingsphere-->  
    <dependency>  
        <groupId>org.apache.shardingsphere</groupId>  
        <artifactId>shardingsphere-jdbc-core-spring-boot-starter</artifactId>  
        <version>5.2.1</version>  
    </dependency>  
  
    <!--    shardingsphere和2.2不兼容，需要使用1.33，但是1.33和springboot 3.2.2 不兼容，所以自定义了 TagInspector和 UnTrustedTagInspector  -->    
    <dependency>  
        <groupId>org.yaml</groupId>  
        <artifactId>snakeyaml</artifactId>  
        <version>1.33</version>  
    </dependency>  
  
</dependencies>
```

在配置文件这部分，我们定义了两个配置文件，分别是 datasource.yml 和datasource-sharding.yml。

datasource.yml 用于配置单库单表，datasource-sharding.yml用于配置分库分表。

同时，在我们的项目中用到了 mybatis-plus，有一些通用的配置，如插件、字段的自动填充等，都在这里做了通用的配置：
[6、MyBatis-Plus插件使用](2、相关技术/24、项目/3、数藏项目/7、关键技术/6、MyBatis-Plus插件使用.md)

[7、MyBatis-Plus实现字段自动填充](2、相关技术/24、项目/3、数藏项目/7、关键技术/7、MyBatis-Plus实现字段自动填充.md)

同时，因为我们还支持分库分表，所以一些通用的和分库分表有关的，比如分布式 ID、分表算法等，也都封装在这个组件中。
[3、全局唯一订单号生成（分布式ID）](2、相关技术/24、项目/3、数藏项目/7、关键技术/3、全局唯一订单号生成（分布式ID）.md)

[12、自定义多Key分片算法](2、相关技术/24、项目/3、数藏项目/6、通用设计/12、自定义多Key分片算法.md)

[13、自定义强制路由分片算法](2、相关技术/24、项目/3、数藏项目/6、通用设计/13、自定义强制路由分片算法.md)


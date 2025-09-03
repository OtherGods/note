![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503052311048.png)

在我们的项目中，我们引入了 Seata 作为分布式事务的解决方案之一，在 nft-turbo-seata 这个包中，我们把 seata 相关的东西封装进来了。

首先在pom文件中引入相关依赖：
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
    <artifactId>nft-turbo-seata</artifactId>  
    <version>1.0.0-SNAPSHOT</version>  
  
    <properties>  
        <maven.compiler.source>21</maven.compiler.source>  
        <maven.compiler.target>21</maven.compiler.target>  
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    </properties>  
  
    <dependencies>  
  
        <dependency>  
            <groupId>io.seata</groupId>  
            <artifactId>seata-all</artifactId>  
            <version>2.0.0</version>  
            <exclusions>  
                <exclusion>  
                    <groupId>org.antlr</groupId>  
                    <artifactId>antlr4-runtime</artifactId>  
                </exclusion>  
            </exclusions>  
        </dependency>  
  
        <dependency>  
            <groupId>com.alibaba.cloud</groupId>  
            <artifactId>spring-cloud-starter-alibaba-seata</artifactId>  
        </dependency>  
  
        <dependency>  
            <groupId>org.apache.shardingsphere</groupId>  
            <artifactId>shardingsphere-transaction-base-seata-at</artifactId>  
            <version>5.2.1</version>  
        </dependency>  
  
        <dependency>  
            <groupId>org.apache.shardingsphere</groupId>  
            <artifactId>shardingsphere-transaction-core</artifactId>  
            <version>5.2.1</version>  
            <scope>compile</scope>  
        </dependency>  
  
    </dependencies>  
  
</project>
```

其中spring-cloud-starter-alibaba-seata是 seata 的 spring cloud的组件，里面帮我们封装好了相关的 bean。

seata-all是 seata 的客户端的依赖，我们需要的一些 seata 的一些工具类，如TransactionHook就在 seata-all 中。

shardingsphere-transaction-base-seata-at 、shardingsphere-transaction-core是seata 和 shardingjdbc 一起使用的时候需要依赖的一个包，这个包其实是 shardingjdbc 提供的，主要是做 seata 和 shardingjdbc的适配，

在这个包中，还有一个关键的配置文件 seata.yml，内容如下：
```yml
seata:  
  application-id: ${spring.application.name}  
  tx-service-group: default_tx_group  
  #  use-jdk-proxy: true  
  #  enable-auto-data-source-proxy: false  config:  
    type: nacos  
    nacos:  
      server-addr: ${nft.turbo.nacos.server.url}  
      group: ${nft.turbo.seata.nacos.group}  
      data-id: ${nft.turbo.seata.nacos.data-id}  
      namespace: ${nft.turbo.seata.nacos.namespace}  
  registry:  
    type: nacos  
    nacos:  
      application: seata-server  
      server-addr: ${nft.turbo.nacos.server.url}  
      group: ${nft.turbo.seata.nacos.group}  
      cluster: default  
      namespace: ${nft.turbo.seata.nacos.namespace}
```

其中主要是关于 seata 的相关配置，这个在 seata 接入中介绍过：
[12、Seata 接入](2、相关技术/24、项目/3、数藏项目/4、框架接入/12、Seata%20接入.md)

然后这个包中还有个特殊的类：SeataATShardingSphereTransactionManager，这个主要是为了解决 seata 和 shardingjdbc 的一个兼容性问题，详见：
[2、重写ShardingSphere源码，解决集成Seata的事务失效问题](2、相关技术/24、项目/3、数藏项目/14、问题排查/1、重写中间件源码/2、重写ShardingSphere源码，解决集成Seata的事务失效问题.md)



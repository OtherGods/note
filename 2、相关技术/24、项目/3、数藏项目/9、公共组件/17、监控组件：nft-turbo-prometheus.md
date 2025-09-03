![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092225360.png)

这个组件很简单，主要是把 Prometheus 集成进来做监控的，里面的东西也很少，只有一份 pom 依赖和配置文件 Prometheus.yml
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
    <artifactId>nft-turbo-promethues</artifactId>  
    <version>1.0.0-SNAPSHOT</version>  
  
    <properties>  
        <maven.compiler.source>21</maven.compiler.source>  
        <maven.compiler.target>21</maven.compiler.target>  
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    </properties>  
  
    <dependencies>  
        <!--  prometheus -->  
        <dependency>  
            <groupId>org.springframework.boot</groupId>  
            <artifactId>spring-boot-starter-actuator</artifactId>  
        </dependency>  
  
        <dependency>  
            <groupId>io.micrometer</groupId>  
            <artifactId>micrometer-registry-prometheus</artifactId>  
        </dependency>  
  
        <!--  dubbo 监控:https://cn.dubbo.apache.org/zh-cn/overview/tasks/observability/metrics-start/ -->  
        <dependency>  
            <groupId>org.apache.dubbo</groupId>  
            <artifactId>dubbo-spring-boot-observability-starter</artifactId>  
            <version>3.2.10</version>  
        </dependency>  
  
    </dependencies>  
  
</project>
```

这里面主要依赖了spring-boot-starter-actuator、micrometer-registry-prometheus、dubbo-spring-boot-observability-starter

详见：
[13、接入Prometheus 做应用监控](2、相关技术/24、项目/3、数藏项目/4、框架接入/13、接入Prometheus%20做应用监控.md)


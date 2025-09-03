![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503091916432.png)

我们的项目中，有很多module 是需要对外提供 web 服务的，也就是 controller，我们就把和 web 服务相关的内容都封装到这个 web 组件中了
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
    <artifactId>nft-turbo-web</artifactId>  
    <description>Web组件</description>  
    <version>1.0.0-SNAPSHOT</version>  
  
    <properties>  
        <maven.compiler.source>21</maven.compiler.source>  
        <maven.compiler.target>21</maven.compiler.target>  
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    </properties>  
  
    <dependencies>  
  
        <dependency>  
            <groupId>cn.hollis</groupId>  
            <artifactId>nft-turbo-base</artifactId>  
        </dependency>  
  
        <dependency>  
            <groupId>cn.hollis</groupId>  
            <artifactId>nft-turbo-sa-token</artifactId>  
        </dependency>  
  
        <dependency>  
            <groupId>cn.hollis</groupId>  
            <artifactId>nft-turbo-cache</artifactId>  
        </dependency>  
  
        <!--    Spring Boot Web   -->  
        <dependency>  
            <groupId>org.springframework.boot</groupId>  
            <artifactId>spring-boot-starter-web</artifactId>  
        </dependency>  
  
    </dependencies>  
  
</project>
```

并且将一些通用的能力，也封装到这个组件中，如全局过滤器、全局异常处理器、参数转换器、以及 VO 中的返回值封装等。


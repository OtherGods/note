![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503042332215.png)

pom依赖中需要增加 Nacos 相关的配置信息：
```xml
<dependencies>  
  
    <!--       Nacos 服务注册与发现-->  
    <dependency>  
        <groupId>com.alibaba.cloud</groupId>  
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>  
        <exclusions>  
            <exclusion>  
                <groupId>org.springframework.cloud</groupId>  
                <artifactId>spring-cloud-starter-netflix-ribbon</artifactId>  
            </exclusion>  
        </exclusions>  
    </dependency>  
  
    <!--    配置中心    -->  
    <dependency>  
        <groupId>com.alibaba.cloud</groupId>  
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>  
    </dependency>  
  
    <!--    bootstrap     -->  
    <dependency>  
        <groupId>org.springframework.cloud</groupId>  
        <artifactId>spring-cloud-starter-bootstrap</artifactId>  
    </dependency>  
  
</dependencies>
```

我们把一些基础已经通用或者基础设施的配置放在config这一层，目前把nacos相关的配置放在config里面
```yml
spring:  
  cloud:  
    nacos:  
      discovery:  
        server-addr: 114.xx.xx.45:8848  
      config:  
        server-addr: 114.xx.xx.45:8848  
        file-extension: properties  
        name: ${spring.application.name}
```

### 配置项详细解释

- spring.cloud.nacos.discovery.server-addr:
	- 这是Nacos服务发现的服务器地址。
- spring.cloud.nacos.config.server-addr:
	- 这是Nacos配置管理的服务器地址。
- spring.cloud.nacos.config.file-extension:
	- 这是配置文件的扩展名。
	- 这里设置为properties，表示配置文件是.properties格式。
- spring.cloud.nacos.config.name:
	- 配置文件的名称。
	- ${spring.application.name}是一个占位符，它会被Spring应用的名称所替换。

### 使用示例

在nft-turbo-user中的bootstrap.yml中
```yml
spring:
  application:
    name: @application.name@
  config:
    import: classpath:config.yml
```

spring.config.import:

这个配置项用于导入其他配置文件。

classpath:config.yml表示从类路径中导入config.yml的配置文件

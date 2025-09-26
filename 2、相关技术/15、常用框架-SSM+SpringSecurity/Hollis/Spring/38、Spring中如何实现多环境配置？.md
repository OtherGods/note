# 典型回答

我们平常在开发的时候，会有多套环境，开发环境、测试环境、预发布环境以及生产环境，不同的环境中应用的配置可能不太一样。那么在如何利用Spring来管理这些配置呢？

Spring 的 Profile 是一种通过在不同环境中使用不同配置的方式来实现条件化配置。Profile 允许你为不同的环境定义特定的配置。

使用 Profile 的步骤：

1. 定义 Profiles：在配置类或配置文件中定义不同的 Profiles，例如使用 `@Profile("dev")` 在 Java 配置类上。
```java
@Configuration
@Profile("dev")
public class DevConfig {
    // 开发环境的特定配置
}

@Configuration
@Profile("prod")
public class ProdConfig {
    // 生产环境的特定配置
}
```

2. 激活 Profile：可以通过多种方式激活 Profile，如设置环境变量、JVM 参数、Web 应用的上下文参数或在 application.properties 文件中使用 `spring.profiles.active` 属性。
```java
● 环境变量：SPRING_PROFILES_ACTIVE=prod
● JVM 参数：-Dspring.profiles.active=prod
```

这样，启动应用时，Spring 将根据激活的 Profile 加载相应的配置。在这个例子中，如果激活的是 prod Profile，将创建并使用 ProdConfig 中的 Bean；

## spring.profiels.actice的替代

### 使用spring.config.import

**==Spring Boot 2.4为了提升对Kubernetes的支持 将 `spring.profiels.actice` 作废了，可以使用`spring.config.import`替换它==**：
```yml
spring:  
  application:  
    name: paicoding  
  #  Spring Boot 2.4为了提升对Kubernetes的支持 将 spring.profiles 作废了  
  #  profiles:  
  #    active: dal,web,config,image  # 替换上面作废的spring.profiels.actice配置参数  
  config:  
    import: application-dal.yml,application-web.yml,application-config.yml,application-image.yml,application-email.yml,application-rabbitmq.yml,application-ai.yml,application-pay.yml
```

### 使用Maven中profiles标签

```xml
<profiles>  
    <!-- 本地开发 -->  
    <profile>  
        <id>dev</id>  
        <properties>  
            <env>dev</env>  
        </properties> 
        <!-- 指定默认环境 -->  
        <activation>  
            <activeByDefault>true</activeByDefault>  
        </activation>  
    </profile>  
    <!-- 测试 -->  
    <profile>  
        <id>test</id>  
        <properties>  
            <env>test</env>  
        </properties>  
    </profile>  
    <!-- 预发 -->  
    <profile>  
        <id>pre</id>  
        <properties>  
            <env>pre</env>  
        </properties>  
    </profile>  
    <!-- 生产 -->  
    <profile>  
        <id>prod</id>  
        <properties>  
            <env>prod</env>  
        </properties>  
    </profile>  
</profiles>
```

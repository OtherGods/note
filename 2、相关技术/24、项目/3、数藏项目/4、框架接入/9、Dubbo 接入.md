这里主要是说在代码中如何把 dubbo 依赖进来，关于 dubbo 的注册中心的搭建参考：
[4、Nacos部署](2、相关技术/24、项目/3、数藏项目/2、中间件部署/4、Nacos部署.md)

# 1、增加pom依赖

```xml
<!--   dubbo   -->  
<dependency>  
    <groupId>org.apache.dubbo</groupId>  
    <artifactId>dubbo-spring-boot-starter</artifactId>  
    <version>3.2.10</version>  
</dependency>  
  
<dependency>  
    <groupId>org.apache.dubbo</groupId>  
    <artifactId>dubbo-registry-nacos</artifactId>  
    <version>3.2.10</version>  
</dependency>
```

# 2、增加yml配置

```yml
dubbo:  
  consumer:  
    timeout: 3000  
    check: false  
  protocol:  
    name: dubbo  
    port: -1  
  registry:  
    address: nacos://114.xx.xx.45:8848  
    parameters:  
      namespace: dca38c77-bef4-40e0-97c3-7779f508b899  
      group: dubbo  
  application:  
    name: ${spring.application.name}  
    qos-enable: true  
    qos-accept-foreign-ip: false
```

这里主要是增加一些关于 dubbo 的配置：

- dubbo.registry.address：表示 dubbo 的注册中心的的地址，这里用的是 nacos
- dubbo.registry.parameters：表示注册中心需要的一些特殊配置，这里针对 nacos 的 namespace 和 group 做了定制，主要是为了隔离，默认的 namespace 中包含了服务、元数据、以及配置信息，会导致 Dubbo 调用的时候出现错误调用，出现失败的情况。
namespace需要自己去 nacos 上创建一下，然后把他对应的值放到这个 yml 文件中
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411101545250.png)
- dubbo.application：指定应用名
- dubbo.consumer.check：Dubbo 默认会在启动时检查依赖的服务是否可用，不可用时会抛出异常，为了避免检查，我们将这个值设置为 false
- dubbo.consumer.timeout：就是默认的超时时长


> AI解释：
> 这段配置是关于Apache Dubbo的，Dubbo是一个高性能、轻量级的开源Java RPC框架。以下是对这段配置的详细解释：
> 
> 1. **dubbo**:
>     - 这是Dubbo配置的根节点，所有Dubbo相关的配置都放在这个节点下。
> 2. **consumer**:
>     - 这个节点用于配置Dubbo消费者（即调用远程服务的客户端）的相关参数。
>         - **timeout**: 消费者调用远程服务的超时时间，单位是毫秒。这里设置为3000，表示如果远程服务在3秒内没有响应，则视为超时。
>         - **check**: 是否在启动时检查依赖的服务是否可用。这里设置为false，表示启动时不会检查依赖的服务。
> 3. **protocol**:
>     - 这个节点用于配置Dubbo协议的相关参数。
>         - **name**: 使用的协议名称。这里设置为dubbo，表示使用Dubbo协议。
>         - **port**: 服务暴露的端口号。这里设置为-1，表示由Dubbo自动分配端口。
> 4. **registry**:
>     - 这个节点用于配置服务注册中心的相关参数。
>         - **address**: 注册中心的地址。这里使用的是Nacos作为注册中心，地址是`nacos://114.xx.xx.45:8848`。
>         - **parameters**: 注册中心的一些额外参数。
>             - **namespace**: Nacos的命名空间，用于隔离不同的环境或租户。
>             - **group**: 服务分组，可以将不同的服务划分到不同的分组中，便于管理。
> 5. **application**:
>     - 这个节点用于配置Dubbo应用的相关参数。
>         - **name**: 应用名称。这里使用了Spring的占位符`${spring.application.name}`，表示应用名称将从Spring的配置中获取。
>         - **qos-enable**: 是否启用QoS（Quality of Service）服务。QoS服务用于收集Dubbo的运行时信息，如CPU、内存使用情况等。这里设置为true，表示启用QoS服务。
>         - **qos-accept-foreign-ip**: 是否允许外部IP访问QoS服务。这里设置为false，表示只允许本地访问QoS服务，提高了安全性。
> 
> 总的来说，这段配置定义了Dubbo消费者的超时时间和启动检查、Dubbo协议的名称和端口、服务注册中心的地址和额外参数、以及Dubbo应用的名称和QoS服务的相关设置。这些配置共同决定了Dubbo服务的行为和性能。


> ChatGPT:Dubbo和nacos
> 
> Dubbo 和 Nacos 是两个不同的工具，但它们可以协同工作来实现微服务架构中的服务治理。以下是两者的区别及它们在微服务架构中的作用：
> 
> **Dubbo简介**
> Dubbo 是一个开源的高性能 Java RPC 框架，主要用于服务治理，包括服务注册、发现、负载均衡、容错、服务调用等。Dubbo 提供了一整套的 RPC 调用机制，适用于微服务架构下的分布式系统。
> 
> **主要功能**
> 1. **服务注册与发现**：服务提供者启动时将服务注册到注册中心，消费者从注册中心获取服务提供者信息。
> 2. **负载均衡**：提供多种负载均衡策略，如随机、轮询、一致性哈希等。
> 3. **容错机制**：支持重试、失败转移、失败通知等容错机制。
> 4. **服务治理**：提供服务降级、流量控制等服务治理功能。
> 5. **高性能通信**：基于 Netty 的高性能通信框架。
> 
> **Nacos简介**
> Nacos 是一个更广泛的服务管理平台，提供动态服务发现、配置管理和服务治理等功能。Nacos 支持多种服务注册和发现协议，适用于多语言开发环境，不仅限于 Java。
> 
> 主要功能
> 1. **服务注册与发现**：支持 HTTP、gRPC、Dubbo 等多种协议的服务注册与发现。
> 2. **配置管理**：提供集中化的配置管理和动态配置更新能力。
> 3. **服务健康检查**：提供服务的健康检查和故障隔离功能。
> 4. **命名服务**：支持 DNS 和 RPC 方式的服务命名。
> 
> **Dubbo 与 Nacos 的区别**
> ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411101543205.png)
> 
> **Dubbo 与 Nacos 的协同使用**
> 
> 1. **服务注册与发现**：可以使用 Nacos 作为 Dubbo 的注册中心，Nacos 支持 Dubbo 的服务注册和发现。
> 2. **配置管理**：通过 Nacos 实现 Dubbo 应用的配置管理，动态更新配置。
> 3. **服务治理**：Nacos 提供的健康检查和故障隔离功能可以增强 Dubbo 的服务治理能力。
> 
> **结论**
> 
> Dubbo 专注于高性能的服务调用和治理，而 Nacos 提供了更全面的服务管理和配置管理能力。两者可以结合使用，通过 Nacos 作为 Dubbo 的注册中心和配置中心，提升微服务架构的灵活性和可管理性。

# 3、在Application上增加注解

如：
```java
/**  
 * @author hollis 
*/

@SpringBootApplication(scanBasePackages = "cn.hollis.nft.turbo.user")  
@EnableDubbo  
public class NfTurboUserApplication {  
    public static void main(String[] args) {  
        SpringApplication.run(NfTurboUserApplication.class, args);  
    }  
}
```

也可以做通用配置：
```java
@EnableDubbo
@Configuration
public class RpcConfiguration {}
```

# 4、提供RPC服务

```java
@DubboService(version = "1.0.0")  
public class UserFacadeServiceImpl implements UserFacadeService {  
      
    public UserQueryResponse<UserInfo> query(UserQueryRequest userLoginRequest) {  
        UserQueryResponse response = new UserQueryResponse();  
        response.setResponseMessage("hehaha");  
        return response;  
    }  
}
```

通过@DubboService声明一个 RPC 的服务。

# 5、定义服务调用方

```java
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("auth")
public class AuthController {
      
    @DubboReference(version = "1.0.0")
    private UserFacadeService userFacadeService;
      
    @GetMapping("/get")  
    public String get(){  
        UserQueryResponse response = userFacadeService.query(new UserQueryRequest());  
        return response.getResponseMessage();  
    }  
}
```

通过@DubboReference声明一个远程的 dubbo 服务，然后就可以像本地的 bean 一样调用了。

扩展：
[19、Dubbo的Bean注入为什么不直接用 @DubboReference](2、相关技术/24、项目/3、数藏项目/13、项目答疑/19、Dubbo的Bean注入为什么不直接用%20@DubboReference.md)



#SpringCloud组件 
# 典型回答

<font color="red" size=5>Spring Cloud是基于Spring Boot的分布式系统开发工具</font>，它**提供了一系列开箱即用的、针对分布式系统开发的特性和组件，用于帮助开发人员快速构建和管理云原生应用程序**。

Spring Cloud的主要目标是解决**分布式系统**中的常见问题，例如服务发现、负载均衡、配置管理、断路器、消息总线等。

所以，单体应用使用Spring，需要快速构建，简化开发使用SpringBoot，构建分布式、微服务应用，使用SpringCloud。

下图是我画的一张SpringCloud中核心组件起到的作用以及所处的位置：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508182024568.png)

下面是Spring Cloud常用的一些组件：
1. `Eureka`：服务发现和注册中心，可以帮助服务消费者自动发现和调用服务提供者。
   - [14、Eureka和Zookeeper有什么区别？](2、相关技术/22、微服务/SpringCloud/Hollis/14、Eureka和Zookeeper有什么区别？.md)
   - [15、介绍一下Eureka的缓存机制](2、相关技术/22、微服务/SpringCloud/Hollis/15、介绍一下Eureka的缓存机制.md)
   - [16、什么是Eureka的自我保护模式？](2、相关技术/22、微服务/SpringCloud/Hollis/16、什么是Eureka的自我保护模式？.md)
   - [20、Eureka 在 Spring Boot 3.x 之后被移除了，如何替代？](2、相关技术/22、微服务/SpringCloud/Hollis/20、Eureka%20在%20Spring%20Boot%203.x%20之后被移除了，如何替代？.md)
2. `Ribbon`：负载均衡组件，可以帮助客户端在多个服务提供者之间进行负载均衡。
   - [7、Ribbon是怎么做负载均衡的？](2、相关技术/22、微服务/SpringCloud/Hollis/7、Ribbon是怎么做负载均衡的？.md)
   - [5、服务端负载均衡与客户端负载均衡（Ribbon和Nginx的区别）](2、相关技术/22、微服务/SpringCloud/Hollis/5、服务端负载均衡与客户端负载均衡（Ribbon和Nginx的区别）.md)
   - [10、LoadBalancer和Ribbon的区别是什么？为什么用他替代Ribbon？](2、相关技术/22、微服务/SpringCloud/Hollis/10、LoadBalancer和Ribbon的区别是什么？为什么用他替代Ribbon？.md)
   - [21、LoadBalancer支持哪些负载均衡策略？如何修改？](2、相关技术/22、微服务/SpringCloud/Hollis/21、LoadBalancer支持哪些负载均衡策略？如何修改？.md)
   - **==在不被维护后，可以用`Spring Cloud LoadBalancer`来替代，`Spring Cloud`自带的==**
3. ~~`OpenFeign`：声明式HTTP客户端，可以帮助开发人员更容易地编写HTTP调用代码。~~
   - [9、OpenFeign 不支持了怎么办？](2、相关技术/22、微服务/SpringCloud/Hollis/9、OpenFeign%20不支持了怎么办？.md)
   - [13、Dubbo和Feign有什么区别？](2、相关技术/22、微服务/SpringCloud/Hollis/13、Dubbo和Feign有什么区别？.md)
   - [17、Feigin 第一次调用为什么很慢？可能的原因是什么？](2、相关技术/22、微服务/SpringCloud/Hollis/17、Feigin%20第一次调用为什么很慢？可能的原因是什么？.md)
   - [22、Feign 和 RestTemplate 有什么不同？](2、相关技术/22、微服务/SpringCloud/Hollis/22、Feign%20和%20RestTemplate%20有什么不同？.md)
   - [23、Feign和OpenFeign 有什么区别？](2、相关技术/22、微服务/SpringCloud/Hollis/23、Feign和OpenFeign%20有什么区别？.md)
   - [24、OpenFeign 是如何实现负载均衡的？](2、相关技术/22、微服务/SpringCloud/Hollis/24、OpenFeign%20是如何实现负载均衡的？.md)
   - [25、OpenFeign如何处理超时？如何处理异常？如何记录客户端日志？](2、相关技术/22、微服务/SpringCloud/Hollis/25、OpenFeign如何处理超时？如何处理异常？如何记录客户端日志？.md)
   - [26、Feign调用超时，会自动重试吗？如何设置？](2、相关技术/22、微服务/SpringCloud/Hollis/26、Feign调用超时，会自动重试吗？如何设置？.md)
   - **==在不被维护后，可以用`@HttpExchange`来替代，`Spring Cloud`自带的==**
4. `Hystrix`：断路器组件，可以帮助应用程序处理服务故障和延迟问题。
   - [8、Hystrix和Sentinel的区别是什么？](2、相关技术/22、微服务/SpringCloud/Hollis/8、Hystrix和Sentinel的区别是什么？.md)
   - [18、Hystrix熔断器的工作原理是什么？](2、相关技术/22、微服务/SpringCloud/Hollis/18、Hystrix熔断器的工作原理是什么？.md)
   - [19、介绍一下 Hystrix 的隔离策略，你用哪个？](2、相关技术/22、微服务/SpringCloud/Hollis/19、介绍一下%20Hystrix%20的隔离策略，你用哪个？.md)
   - **==可以用`Sentinel`代替==**
5. ~~`Zuul`：API网关，可以帮助应用程序处理API请求的路由、负载均衡、安全和监控等问题。~~
   - [6、Zuul、Gateway和Nginx有什么区别？](2、相关技术/22、微服务/SpringCloud/Hollis/6、Zuul、Gateway和Nginx有什么区别？.md)
   - [4、什么是Zuul网关，有什么用？](2、相关技术/22、微服务/SpringCloud/Hollis/4、什么是Zuul网关，有什么用？.md)
   - [12、为什么需要SpringCloud Gateway，他起到了什么作用？](2、相关技术/22、微服务/SpringCloud/Hollis/12、为什么需要SpringCloud%20Gateway，他起到了什么作用？.md)
   - **==可以用`SpringCloudGateway`代替==**
6. `Config`：分布式配置管理组件，可以帮助应用程序从远程配置源获取配置信息。
   **==可以用`Nacos`代替 `Euraka` + `Config`==**
7. `Bus`：消息总线组件，可以帮助应用程序实现分布式事件传递和消息广播。
8. `Sleuth`：Sleuth是Spring Cloud生态系统中的一个分布式追踪解决方案，可以帮助开发人员实现对分布式系统中请求链路的追踪和监控。 
9. `Gateway`：spring Cloud Gateway是Spring Cloud推出的第二代网关框架，取代Zuul网关。提供了路由转发、权限校验、限流控制等作用。
   [12、为什么需要SpringCloud Gateway，他起到了什么作用？](2、相关技术/22、微服务/SpringCloud/Hollis/12、为什么需要SpringCloud%20Gateway，他起到了什么作用？.md)
10. `Security`：用于简化 OAuth2 认证和资源保护。

但是，有的时候我们并不一定非要全部都用`SpringCloud`的这些组件，有的时候我们也可以选择其他的开源组件替代。比如经常用`Dubbo/gRPC`来代替Feign进行服务间调用。经常使用`Nacos`代替`Config`+`Eureka`来实现服务发现/注册及配置中心的功能。

如下图是我[项目课中的技术栈](https://www.yuque.com/hollis666/io9xi1/dgolk0cckpb94sia)，目前比较主流的SpringCloud的架构：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508182223243.png)


- 请求先到达Nginx，在Nginx层做负载均衡、静态文件服务器、缓存、跨域等；
- 经过Nginx后，请求到达网关，在网关层做请求路由、转发、结合loadbalancer做负载均衡、权限控制、流量管理、日志记录等，之后调用具体服务；
- 如果有服务与服务之间的调用，会从注册中心拉拉取服务信息，经过负载均衡调用其他服务，远程调用之间用openfegin、dubbo等；
- 考虑到服务于服务间数据的一致性，可以引入seata做分布式事务，rocketmq做流量削峰填谷、分布式事务等；
- 在数据持久化方面，通过MySQL，考虑到性能，可以在持久层与服务之间加缓存，比如redis（一般用框架caffeine、jetcache），在大数据量模糊匹配等sql优化后效果不明显的而情况，可以引入es，通过cancel监听mysql的binlog日志在es中重做;
- 定时任务方面考虑用xxl-job（在延迟消息场景下，采用消息队列后可能会出现大量无效消息，可靠性虽然高，但是不能保证100%不丢失消息，时间精度不够）

# 扩展知识

## Spring Cloud Alibaba

Spring Cloud Alibaba则是由Alibaba推出的分布式开发框架，它主要针对于微服务和云原生应用的开发和部署。Spring Cloud Alibaba提供了一系列的组件和解决方案，例如服务注册、配置管理、消息驱动等。Spring Cloud Alibaba的组件通常基于阿里巴巴自研的组件，例如Nacos、Sentinel、RocketMQ等。

依托 Spring Cloud Alibaba，您只需要添加一些注解和少量配置，就可以将 Spring Cloud 应用接入阿里分布式应用解决方案，通过阿里中间件来迅速搭建分布式应用系统。

目前 Spring Cloud Alibaba 提供了如下功能:
1. **服务限流降级**：支持 WebServlet、WebFlux, OpenFeign、RestTemplate、Dubbo 限流降级功能的接入，可以在运行时通过控制台实时修改限流降级规则，还支持查看限流降级 Metrics 监控。
2. **服务注册与发现**：适配 Spring Cloud 服务注册与发现标准，默认集成了 Ribbon 的支持。
3. **分布式配置管理**：支持分布式系统中的外部化配置，配置更改时自动刷新。
4. **Rpc服务**：扩展 Spring Cloud 客户端 RestTemplate 和 OpenFeign，支持调用 Dubbo RPC 服务
5. 消息驱动能力：基于 Spring Cloud Stream 为微服务应用构建消息驱动能力。
6. **分布式事务**：使用 @GlobalTransactional 注解， 高效并且对业务零侵入地解决分布式事务问题。
7. 阿里云对象存储：阿里云提供的海量、安全、低成本、高可靠的云存储服务。支持在任何应用、任何时间、任何地点存储和访问任意类型的数据。
8. 分布式任务调度：提供秒级、精准、高可靠、高可用的定时（基于 Cron 表达式）任务调度服务。同时提供分布式的任务执行模型，如网格任务。网格任务支持海量子任务均匀分配到所有 Worker（schedulerx-client）上执行。
9. 阿里云短信服务：覆盖全球的短信服务，友好、高效、智能的互联化通讯能力，帮助企业迅速搭建客户触达通道。

## Spring Cloud Tencent

Spring Cloud Tencent 是腾讯开源的一站式微服务解决方案。

Spring Cloud Tencent 实现了Spring Cloud 标准微服务 SPI，开发者可以基于 Spring Cloud Tencent 快速开发 Spring Cloud 云原生分布式应用。

Spring Cloud Tencent提供的能力包括但不限于：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508182237327.png)

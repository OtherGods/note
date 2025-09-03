# 典型回答

`Feign`是由 `Netflix` 开发的一个 HTTP 客户端库，它简化了 Web 服务的调用。`Feign` 使得服务之间的 HTTP 调用变得更加简单，提供了基于注解的声明式 HTTP 请求方式。  

`OpenFeign`是 Spring Cloud 团队对`Feign`的封装与扩展，它增强了`Feign`的功能，特别是通过与 Spring Cloud 的其他组件（如 `Spring Cloud LoadBalancer`, `Spring Cloud Eureka`, `Spring Cloud Config`）进行集成，实现了更强的功能和易用性。  

所以可以认为`OpenFeign`是 `Feign` 的一个“Spring Cloud 版”，并且在 Spring Cloud 中得到了维护和扩展。`OpenFeign`的版本通常会紧跟 Spring Cloud 的更新（但是后来也不更新了），而 Feign是由 Netflix 维护的独立项目。
[9、OpenFeign 不支持了怎么办？](2、相关技术/22、微服务/SpringCloud/Hollis/9、OpenFeign%20不支持了怎么办？.md)

| 特性         | Fegin         | OpenFegin                               |
| ---------- | ------------- | --------------------------------------- |
| **维护者**    | NetFlix       | Spring Cloud 官方                         |
| **集成负载均衡** | 需要与 Ribbon 集成 | 自动集成 Spring Cloud LoadBalancer 或 Ribbon |
| **服务发现**   | 不支持或需要手动配置    | 自动集成 Spring Cloud Eureka 或 Consul       |
| **自动配置**   | 无自动配置         | 提供自动配置                                  |

# 典型回答

Spring Boot 3.x 移除了对 Java EE（Jakarta EE）旧 API（如 javax 包）的支持，而 `Eureka` 仍然依赖 `javax.xml.bind` 等 API，因此官方不再维护 `Eureka`，并推荐其他方案。  

`Eureka`的主要功能是实现服务注册中心，所以其实他的替代产品还挺多的，比如nacos、zk、consul等 。
[1、注册中心如何选型？](2、相关技术/22、微服务/配置中心/1、注册中心如何选型？.md)

## Nacos

[2、什么是Nacos，主要用来作什么？](2、相关技术/22、微服务/配置中心/2、什么是Nacos，主要用来作什么？.md)

`Nacos`是阿里巴巴开源的服务注册中心和配置中心。与`Zookeeper`不同的是，`Nacos`自带了配置中心功能，并提供了更多的可视化配置管理工具。Nacos的目标是成为一个更全面的云原生服务发现、配置和管理平台。

## Consul

Consul是HashiCorp开源的服务注册中心和配置中心，提供了服务发现、健康检查、KV存储和多数据中心功能。Consul提供了更丰富的健康检查和路由功能，同时也提供了丰富的API和Web UI。

## Zookeeper

Zookeeper是最早流行的开源分布式协调服务框架之一，同时也提供了分布式配置中心的功能。Zookeeper以高可用、一致性和可靠性著称，但是需要用户自己来开发实现分布式配置的功能。

他们的职责是不一样的

~~LoadBalancer 是做 HTTP请求的负载均衡的，即我们在页面上输入 /trade/xxx 的时候他帮我们路由到 trade这个模块的 controller 中。~~【这段话有问题，这里描述的应该是网关的路由功能，LoadBalancer主要是用来做负载均衡的】

~~Dubbo 是帮我们做 Dubbo 服务的负载均衡的，即我们在 trade的 controller 中调用 order 模块的 Dubbo 服务的时候，他帮我们做具体调用哪个 order 的应用的负载均衡。~~【感觉数藏项目中Dubbo主要是做远程调用】

但是他们都是基于 Nacos 做的服务发现，从 Nacos 上获取被调用方的 IP 列表，然后基于负载均衡策略进行负载均衡。

## ChatGPT介绍

|**需求类型**|**推荐方案**|**说明**|
|---|---|---|
|全栈Spring Cloud + 中低性能需求|**仅用Spring LoadBalancer**|保持架构简洁，利用Gateway+LB+OpenFeign组合|
|高并发核心模块|**局部引入Dubbo**|核心服务间用Dubbo，非核心服务保留HTTP|
|多语言混合架构|**Dubbo为主**|利用Dubbo多语言SDK整合异构系统|
[10、为什么RPC要比HTTP更快一些？](2、相关技术/22、微服务/RPC/1、Dubbo/Hollis/10、为什么RPC要比HTTP更快一些？.md)

**Spring LoadBalancer是Spring Cloud微服务间HTTP调用的基础设施支撑，而Dubbo是解决高性能RPC与深度服务治理的完整方案**。若你的系统以Spring Cloud为主体，且未遭遇HTTP协议的性能或治理瓶颈，则无需引入Dubbo；反之若内部服务出现高频、低延迟的调用需求，可针对性地在局部服务间替换为Dubbo协议，同时保持API网关的HTTP入口统一性。
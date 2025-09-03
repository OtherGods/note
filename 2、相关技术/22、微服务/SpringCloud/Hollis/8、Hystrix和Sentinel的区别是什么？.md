# 典型回答

`Hystrix`和`Sentinel`都是SpringCloud中可以用来做限流、降级的组件。

Hystrix 的关注点在于以 **隔离** 和 **熔断** 为主的容错机制，超时或被熔断的调用将会快速失败，并可以提供 fallback 机制。而 Sentinel 的侧重点在于多样化的**流量控制**、**熔断降级**、**系统负载保护**以及**实时监控**和控制台。

关于`Hystrix`和`Sentinel`的对比，在`Sentinel`的官网上有一篇文章写的挺详细的： https://sentinelguard.io/zh-cn/blog/sentinel-vs-hystrix.html 

二者的主要差异如下表：
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506042257526.png)












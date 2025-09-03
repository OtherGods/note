原因很简单， 一方面是我比较熟悉 Dubbo，另外一方面，OpenFeign 和 Feign 官方都已经停止更新维护了。

Feign是Spring Cloud中的一个声明式的HTTP客户端库，用于简化编写基于HTTP的服务调用代码。**但是从Spring Cloud 2020版本开始，官方宣布Feign将不再维护和支持，推荐使用OpenFeign作为替代方案。**

但是，**随着SpringCloud 2022的发布，官方宣布OpenFeign将被视为功能完整。这意味着Spring Cloud团队将不再向模块添加新特性。只会修复bug和安全问题。**

其实你在面试的时候，面试官问你这个问题，你也可以说Dubbo 更熟悉，开发团队选择技术框架，肯定选择自己团队最熟悉的技术，这个非常的正常。

面试的时候还可以说的几个原因：

1、Dubbo提供服务治理功能，包括服务发现、负载均衡、故障转移和动态配置等。而 Feign 需要配合 Ribbon 和 Hystrix 来实现这些功能。

2、Dubbo 是 RPC 框架，而 Feign&OpenFeign 是HTTP 客户端，他的性能也没有 RPC框架号，因为 RPC 有自己的各种协议封装，性能要更好一些。

顺便贴一下我八股文中关于二者区别的介绍的截图吧：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503192316386.png)

[1、什么是RPC，和HTTP有什么区别？](2、相关技术/22、微服务/RPC/1、Dubbo/Hollis/1、什么是RPC，和HTTP有什么区别？.md)


大家好，我是**华仔**, 又跟大家见面了。

今天我们来讲解下 **RocketMQ 从阿里云到自建迁移注意事项**，下面是正文。

# **01 总体概述**

在之前公司，由于某些原因，我们需要将阿里云的 RocketMQ 迁移到自建 RocketMQ 中。

那么开源版本和阿里云版本究竟有哪些差异，我们在迁移过程中该注意什么呢？

# **02 阿里云与自建 RocketMQ 对比**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292350924.png)

从上图可以看出自建 RocketMQ 需要考虑的问题非常多，特别是协议方面，自建 RocketMQ 是不支持 http 协议接入的，不过好消息是 RocketMQ5.0 之后支持 GRPC 的方式接入了，所以没有特别的要求，我们可以将 http 接入的方式都换成 GRPC 的方式接入。

功能方面 RocketMQ4.x 版本是不支持任意时间的延迟消息，但是在 5.x 版本也支持了任意界别的时间轮延时消息

具体相关的讨论可以参考官方的文档
1. 石墨文档: [https://shimo.im/docs/gXqme9PKKpIeD7qo/read](https://shimo.im/docs/gXqme9PKKpIeD7qo/read)
2. Apache RocketMQ Feature: [https://rocketmq.apache.org/release-notes/2022/09/09/5.0.0/](https://rocketmq.apache.org/release-notes/2022/09/09/5.0.0)
3. github pr:[https://github.com/apache/rocketmq/pull/4642/files](https://github.com/apache/rocketmq/pull/4642/files)

# **03 总结**

总的来说开源的 RocketMQ 和阿里云消息队列的差异还是挺大的，核心还是「**运维**」。自建需要自己搭建整个RocketMQ 集群，并且 RocketMQ 没有合适好用的 dashboard，但是基本功能 RocketMQ5.x 都是满足的，已经支持了任意秒级别的延时消息，也支持 「**GRPC**」的接入方式。但是在扩容和监控方面仍然需要我们自己去定制化开发。


# 典型回答

`RocketMQ`中有这样几个角色：`NameServer`、`Broker`、`Producer`和`Consumer`
- `NameServer`：`NameServer`是RocketMQ的 **==路由和寻址中心，它维护了Broker和Topic的路由信息==**，提供了Producer和Consumer与正确的Broker建立连接的能力。NameServer还负责 **==监控Broker的状态==**，并提供 **==自动发现和故障恢复的功能==**。
- `Broker`：`Broker`是RocketMQ的核心组件，负责 **==存储、传输和路由消息==**。它接收Producer发送的消息，并将其存储在内部存储中。并且还负责处理Consumer的订阅请求，将消息推送给订阅了相应Topic的Consumer。
- `Producer`（消息生产者）：Producer是消息的生产者，用于**将消息发送到RocketMQ系统**。
- `Consumer`（消息消费者）：Consumer是消息的消费者，用于**从RocketMQ系统中订阅和消费消息**。

RocketMQ的工作过程大致如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508281820047.png)

参考：[2.2.5、工作流程](2、相关技术/23、高并发高可用高性能/MQ/尚硅谷/RocketMQ教程#2.2.5、工作流程)

1. *==启动NameServer==*：**==`NameServer`启动后开始监听端口（默认`9876`），等待Broker、Producer、Consumer连接==**。
2. *==启动Broker==*：`Broker`会与所有的`NameServer`建立并保持长连接，然后 **==每 30 秒向NameServer定时发送心跳包==**。
3. *==创建Topic==*，**==创建`Topic`时需要指定该`Topic`要存储在哪些`Broker`上==**，当然，在创建`Topic`时也会将`Topic`与`Broker`的关系写入到`NameServer`中。不过，这步是可选的，也可以在发送消息时自动创建`Topic`。
4. *==Producer发送消息==*：参考[3.1.1、消息的生产过程](、相关技术/23、高并发高可用高性能/MQ/尚硅谷/RocketMQ教程#3.1.1、消息的生产过程)
   启动时 **==先跟`NameServer`集群中的其中一台建立长连接，并从`NameServer`中获取路由信息==**，即当前发送的`Topic`消息的`Queue`与`Broker`的地址（`IP + Port`）的映射关系。然后 **==根据 [Queue选择算法](2、相关技术/23、高并发高可用高性能/MQ/尚硅谷/RocketMQ教程.md#3.1.2、Queue选择算法) 从队选择一个`Queue`，与队列所在的`Broker`建立长连接从而向`Broker`发消息==**。当然，在获取到路由信息后，`Producer`会首先将路由信息缓存到本地，再每 30 秒从`NameServer`更新一次路由信息。
5. *==Consumer跟Producer类似==*，跟其中一台`NameServer`建立长连接，**==获取其所订阅`Topic`的路由信息，然后根据 [Queue分配算法](2、相关技术/23、高并发高可用高性能/MQ/尚硅谷/RocketMQ教程.md#3.4.4、Queue分配算法) 从路由信息中获取到其所要消费的`Queue`==**，然后直接跟`Broker`建立长连接，开始消费其中的消息。`Consumer`在获取到路由信息后，同样也会每 30 秒从`NameServer`更新一次路由信息。不过不同于`Producer`的是，`Consumer`还会向`Broker`发送心跳，以确保Broker的存活状态。
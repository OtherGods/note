  
大家好，我是 **华仔**, 又跟大家见面了。

从今天开始我会带领大家去学习 **RocketMQ 集群原理以及集群部署实战篇**。
# **01 总述**

在上一篇中，我带大家一步步搭建起来 RocketMQ，又带大家测试消息生产和消费，最后安装了一套可视化控制台系统来帮助你更好的理解。

# **02 RocketMQ 架构**
## **2.1、 RocketMQ 集群组成**

RocketMQ部署结构如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410061114282.png)

这里我从消息中间件服务的角度来看整个 RocketMQ 系统，主要分为：「**NameServer**」、「**Broker**」、「**Producer**」、「**Consumer**」四个部分。

## **2.2、 NameServer**

**NameServer** 是一个功能齐全的服务器，其角色类似 Dubbo 中的 Zookeeper，但 NameServer 与 Zookeeper 相比更加轻量。主要是因为每个 NameServer 节点互相之间是独立的，没有任何信息交互。

它的压力也不会太大，平时主要开销就是在「**维持心跳**」、「**提供 Topic - Broker 关系数据**」。

另外 **NameServer** 被设计成无状态，可以横向扩展，节点之间无通信，每个 **Broker** 在启动的时候会向 **NameServer** 进行注册，而 **Producer** 在发送消息前会根据对应 **Topic** 向 **NameServer** 获取到 **Broker** 的路由信息，**Consumer** 也会定时获取 Topic 的路由信息。

综上，可以得出两点：
1. 提供服务发现和注册机制，NameServer 接收来自 Broker 的注册，并通过心跳机制来检测 Broker 服务的健康性。
2. 提供路由功能，NameServer 集群中的每个 NameServer 都保存了 Broker 集群中整个的路由信息和队列信息。这里需要注意，在 NameServer 集群中，每个 NameServer 都是相互独立的，所以每个 Broker 需要连接所有的 NameServer，每创建一个新的 Topic 都要同步到所有的 NameServer 上。

## **2.3、 Broker**

**Broker** 是指具体提供业务的服务器，负责消息存储，以 Topic 纬度支持轻量级的队列，单机可以支撑**上万队列规模**，具有**上亿级消息堆积能力，可以严格保证消息的有序性**。

它有如下几个模块组成：
1. **远程连接模块**：指整个 Broker 实体，负责处理来自客户端请求。
2. **客户端管理模块**：负责管理客户端「**Producer**」、「**Consumer**」和维护 Consumer 上的 Topic 订阅信息。
3. **存储服务模块**：提供了方便简单的 API 接口来处理消息，存储到物理硬盘以及查询相关功能。
4. **HA 服务模块**：提供高可用服务，Master Broker 和 Slave Broker 之间的数据同步功能。
5. **索引服务**：创建消息的索引，以提供快速查询功能。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410061158911.png)

## **2.4、 Producer**

**Producer** 负责产生消息，由用户进行分布式部署，提供了三种方式来发送消息。
1. **同步发送**：指消息发送方发出数据后会在收到服务器返回响应之后才发下一个，适用于重要通知消息场景。
2. **异步发送**：指发送方发出数据后，不等待服务器返回响应，接着发送下一个，适用于耗时较长，但对响应时间敏感的业务场景。
3. **单向发送**：指只负责发送消息而不等待服务器回应且没有回调函数触发，适用于某些耗时非常短但对可靠性要求并不高的场景。

## **2.5、 Consumer**

**Consumer** 负责消费消息，同样由用户进行分布式部署，支持以 「**push 推模式**」、 「**pull 拉模式**」对消息进行消费，同时也支持「**集群方式**」、「**广播方式**」的消费，它提供实时消息订阅机制，用来满足大多数用户的需求。

# **03 RocketMQ 集群模式**

在上一篇中，我带大家部署了 「**单 Master 模式**」，但是这种方式风险较大，一旦 **Broker** 重启或者宕机时会导致整个服务不可用，这里再给大家介绍几种部署方式。

## **3.1、 多 Master 集群模式**

在一个 RocketMQ 集群中没有 **Slave**，全是 **Master** ，比如 2个 Master，这种模式的优缺点如下：
1. **优点**：
	- 配置比较简单，单台 Master 宕机或重启对应用无影响，在磁盘配置为 RAID10 时，即使机器宕机不可恢复情况下，由于 RAID10 磁盘非常可靠，消息也不会丢「**异步刷盘会丢失少量消息，同步消息不丢失**」，性能最高。
	- **高可用性:** 任何一个Master节点宕机，不会影响整个集群的可用性，其他节点可以继续提供服务。
	- **高性能:** 多个Master节点分担负载，提高了系统的整体吞吐量。
	- **易于扩展:** 可以通过增加Master节点来线性扩展系统的容量。
2. **缺点**：
	- 就是单台 Master 机器宕机期间，未被消费的消息在机器恢复之前不可订阅，消息实时性会受到影响。
	- **数据一致性问题:** 在极端情况下，如果消息刚发送到一个Master节点，该节点就宕机了，那么这条消息可能会丢失。
	- **脑裂问题:** 在网络分区的情况下，可能会出现多个Master节点同时对外提供服务，导致数据不一致。

### **3.1.1、 环境**

| 主机        | IP地址         | 组件部署                        |
| --------- | ------------ | --------------------------- |
| rocketmq1 | 192.168.56.1 | Nameserver1、Broker_Master-1 |
| rocketmq2 | 192.168.56.2 | Nameserver2、Broker_Master-2 |

### **3.1.2、 创建数据目录**

```shell
# 先在各服务器执行以下命令创建数据目录
[wangjianghua@rocketmq* rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store

[wangjianghua@rocketmq* rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store/commitlog

[wangjianghua@rocketmq* rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store/consumequeue

[wangjianghua@rocketmq* rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store/index
```

### **3.1.3、 修改日志路径**

```shell
# 在各服务器执行以下命令修改日志存储路径
[wangjianghua@rocketmq* rocketmq]$ sed -i 's#${user.home}#/home/wangjianghua/src/rocketmq/data#g' conf/*.xml
```

### **3.1.4、 配置 RocketMQ**

[【入门实战系列第七篇】RocketMQ 核心配置详解](https://articles.zsxq.com/id_zx4zg5m8r3xz.html)
```shell
# 配置Broker_Master-1

[wangjianghua@rocketmq1 rocketmq]$ cat conf/2m-noslave/broker-a.properties

brokerClusterName=rocketmq-2m-noslave-cluster

brokerName=rocketmq-broker-m1

brokerId=0

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort

brokerRole=ASYNC_MASTER

flushDiskType=ASYNC_FLUSH
```  

```shell
# 配置Broker_Master-2

[wangjianghua@rocketmq2 rocketmq]$ cat conf/2m-noslave/broker-b.properties

brokerClusterName=rocketmq-2m-noslave-cluster

brokerName=rocketmq-broker-m2

brokerId=0

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort

brokerRole=ASYNC_MASTER

flushDiskType=ASYNC_FLUSH
```

### **3.1.5、 启动 NameServer**

启动 Broker 前需要先启动 NameServer，如果在生产环境使用，为了保证高可用，建议集群启动3个 NameServer，各节点的启动命令相同，如下：
```shell
# 启动Nameserver1
[wangjianghua@rocketmq1 rocketmq]$ nohup sh bin/mqnamesrv &

# 启动Nameserver2
[wangjianghua@rocketmq2 rocketmq]$ nohup sh bin/mqnamesrv &
```

### **3.1.6、 启动 Broker**

```shell
# 启动Broker_Master-1
[wangjianghua@rocketmq1 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-noslave/broker-a.properties &


# 启动Broker_Master-2
[wangjianghua@rocketmq2 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-noslave/broker-b.properties &
```

### **3.1.7、 关闭服务**

```shell
# 关闭NameServer
[wangjianghua@rocketmq1 rocketmq]$ bin/mqshutdown namesrv

# 关闭 broker
[wangjianghua@rocketmq2 rocketmq]$ bin/mqshutdown broker
```

### **3.1.8、总结**
在RocketMQ的多Master集群模式中，每个Master节点都是独立的，都对外提供服务。它们之间没有主备关系，而是并行工作，共同承担系统的负载。

**具体的工作原理**
1. **消息路由:**
    - 生产者发送消息时，消息会根据一定的负载均衡算法（如轮询、随机等）被路由到不同的Master节点上。
    - 负载均衡算法可以根据配置进行调整，以适应不同的业务需求。
2. **消息存储:**
    - 每个Master节点都有独立的存储空间，用于存储分配给它的消息。
    - 消息存储的方式可以是同步刷盘或异步刷盘，根据对数据可靠性的要求进行选择。
3. **消息消费:**
    - 消费者订阅主题后，会从订阅的主题对应的队列中消费消息。
    - 每个Master节点上的队列都可以被多个消费者组同时消费。
4. **NameServer的作用:**
    - NameServer负责维护整个集群的路由信息，包括每个Master节点的地址和Topic与队列的映射关系。
    - 生产者和消费者通过NameServer获取路由信息，从而找到目标Master节点发送或消费消息。

**多Master集群模式的本质是将负载分散到多个节点上，每个节点都可以独立地处理消息。** 这种模式提高了系统的可用性和性能，但同时也带来了一些数据一致性问题。在选择部署模式时，需要根据实际业务需求综合考虑。

## **3.2、 多 Master 多 Slave 异步复制模式**

这种模式下每个 Master 都配置一个 Slave，有多对 Master-Slave，HA 采用异步复制方式，主备有毫秒级消息延迟，其优缺点如下：
1. **优点**：
	- 即使磁盘损坏，消息丢失的非常少，且消息实时性不会受影响，同时 Master 宕机后，消费者仍然可以从 Slave 消费，而且此过程对应用透明，不需要人工干预，性能同多Master模式几乎一样；
	- **高可用性:** 即使Master节点发生故障，Slave节点也可以提供服务，保证系统的可用性。
	- **高性能:** 异步复制的方式不会影响消息的发送和消费性能。
	- **数据冗余:** Slave节点存储了消息的副本，提高了数据的可靠性。
2. **缺点**：
	- **数据丢失风险**：当 Master 宕机、磁盘损坏时，如果数据还未复制到slave，会丢失少量消息。
	- **数据一致性:** 由于是异步复制，存在短暂的数据不一致性。

### **3.2.1、 环境**

| 主机      | IP地址       | 组件部署                           |
| --------- | ------------ | ---------------------------------- |
| rocketmq1 | 192.168.56.1 | Nameserver1、Broker_Master-1       |
| rocketmq2 | 192.168.56.2 | Nameserver2、Broker_Master-2       |
| rocketmq3 | 192.168.56.3 | Nameserver3、Broker_Master_slave-1 |
| rocketmq4 | 192.168.56.4 | Nameserver4、Broker_Master_slave-2 |

### **3.2.2、 创建数据目录**

```shell
# 在各服务器执行以下命令创建数据目录
[wangjianghua@rocketmq1 rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store

[wangjianghua@rocketmq1 rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store/commitlog

[wangjianghua@rocketmq1 rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store/consumequeue

[wangjianghua@rocketmq1 rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store/index
```

### **3.2.3、 修改日志路径**

```shell
# 在各服务器执行以下命令修改日志存储路径
[wangjianghua@rocketmq1 rocketmq]$ sed -i 's#${user.home}#/home/wangjianghua/src/rocketmq/data#g' conf/*.xml
```

### **3.2.4、 配置 RocketMQ**

```shell
# 配置Broker_Master-1

[wangjianghua@rocketmq1 rocketmq]$ cat conf/2m-2s-async/broker-a.properties

brokerClusterName=rocketmq-2m-slave-cluster-async

brokerName=rocketmq-broker-m1

brokerId=0

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876;192.168.56.3:9876;192.168.56.4:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort

brokerRole=ASYNC_MASTER

flushDiskType=ASYNC_FLUSH
```

```shell
# 配置Broker_Master-2

[wangjianghua@rocketmq2 rocketmq]$ cat conf/2m-2s-async/broker-b.properties

brokerClusterName=rocketmq-2m-slave-cluster-async

brokerName=rocketmq-broker-m2

brokerId=0

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876;192.168.56.3:9876;192.168.56.4:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort

brokerRole=ASYNC_MASTER

flushDiskType=ASYNC_FLUSH
```

```shell
# 配置Broker_Master_slave-1

[wangjianghua@rocketmq3 rocketmq]$ cat conf/2m-2s-async/broker-a-s.properties

brokerClusterName=rocketmq-2m-slave-cluster-async

brokerName=rocketmq-broker-m1

brokerId=1

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876;192.168.56.3:9876;192.168.56.4:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort

brokerRole=SLAVE

flushDiskType=ASYNC_FLUSH
```

```shell
# 配置Broker_Master_slave-2

[wangjianghua@rocketmq3 rocketmq]$ cat conf/2m-2s-async/broker-b-s.properties

brokerClusterName=rocketmq-2m-slave-cluster-async

brokerName=rocketmq-broker-m2

brokerId=1

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876;192.168.56.3:9876;192.168.56.4:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store1

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog1

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue1

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index1

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint1

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort1

brokerRole=SLAVE

flushDiskType=ASYNC_FLUSH
```

### **3.2.5、 启动 NameServer**

```shell
# 启动Nameserver1
[wangjianghua@rocketmq1 rocketmq]$ nohup sh bin/mqnamesrv &

# 启动Nameserver2
[wangjianghua@rocketmq2 rocketmq]$ nohup sh bin/mqnamesrv &

# 启动Nameserver3
[wangjianghua@rocketmq3 rocketmq]$ nohup sh bin/mqnamesrv &

# 启动Nameserver4
[wangjianghua@rocketmq4 rocketmq]$ nohup sh bin/mqnamesrv &
```

### **3.2.6、 启动 Broker**

```shell
# 启动Broker_Master-1
[wangjianghua@rocketmq1 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-2s-async/broker-a.properties &

# 启动Broker_Master-2
[wangjianghua@rocketmq2 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-2s-async/broker-b.properties &

# 启动Broker_Master_slave-1
[wangjianghua@rocketmq3 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-2s-async/broker-a-s.properties &

# 启动Broker_Master_slave-2
[wangjianghua@rocketmq4 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-2s-async/broker-b-s.properties &
```

### **3.2.7、 关闭服务**

```shell
# 关闭 NameServer
[wangjianghua@rocketmq* rocketmq]$ bin/mqshutdown namesrv

# 关闭 Broker
[wangjianghua@rocketmq* rocketmq]$ bin/mqshutdown broker
```

### **3.2.8、总结**

在RocketMQ的多Master多Slave异步复制模式中，对外提供服务的主要是**Master节点**。Slave节点主要用于数据备份和容灾。

**工作原理**
1. **消息生产:**
    - 生产者发送消息时，消息会根据负载均衡策略被路由到某个Master节点。
    - Master节点收到消息后，会将消息持久化到本地存储，并异步复制到对应的Slave节点。
    - 一旦Master节点确认消息写入成功，就会向生产者返回确认应答。
2. **消息消费:**
    - 消费者订阅主题后，会从订阅的主题对应的队列中消费消息。
    - 消费者通常会从Master节点消费消息，以保证消费数据的最新性。
    - 在Master节点发生故障的情况下，消费者可以自动切换到Slave节点进行消费。
3. **数据同步:**
    - Master节点会异步地将消息复制到Slave节点。
    - 异步复制的优点是性能高，不会影响消息的发送和消费。
    - 缺点是存在数据丢失的风险，如果Master节点在消息复制完成之前宕机，那么该消息可能会丢失。

**外部访问**
- **客户端访问:**
    - 客户端（生产者或消费者）在连接RocketMQ集群时，会通过NameServer获取到所有Master节点的地址。
    - 客户端会根据负载均衡策略选择一个Master节点进行连接，并发送或消费消息。
    - 如果选择的Master节点发生故障，客户端会重新从NameServer获取路由信息，并选择一个可用的Master节点进行连接。

在多Master多Slave异步复制模式中，**Master节点是对外提供服务的主力**，Slave节点主要用于数据备份和容灾。这种模式兼顾了高可用性、高性能和数据冗余，是RocketMQ常用的部署模式之一。

**需要注意的是**，虽然Slave节点可以提供服务，但一般情况下，我们还是建议消费者从Master节点消费消息，以保证消费数据的最新性。

## **3.3、 多 Master 多 Slave 同步双写模式**

每个 Master 配置一个 Slave，有多对 Master-Slave，HA采用同步双写方式，即只有主备都写成功，才返回成功，这种模式的优缺点如下：
1. **优点**：
	- 数据与服务都无单点故障，Master 宕机情况下，消息无延迟，服务可用性与数据可用性都非常高；
	- **高可用性:** 即使Master节点发生故障，Slave节点也可以立即提供服务，保证系统的可用性。
	- **高可靠性:** 同步复制保证了Master节点和Slave节点的数据一致性，减少了数据丢失的风险。
	- **强一致性:** 消息只有在所有副本都同步成功后才会返回确认应答，保证了强一致性。
2. **缺点**：性能比异步复制模式略低10%左右，发送单个消息的 RT 会略高，且目前版本在主节点宕机后，备机不能自动切换为主机。
	- **性能开销:** 同步复制会增加消息发送的延迟，降低系统的吞吐量。

### **3.3.1、 环境**

| 主机        | IP地址         | 组件部署                              |     |
| --------- | ------------ | --------------------------------- | --- |
| rocketmq1 | 192.168.56.1 | Nameserver1、Broker_Master-1       |     |
| rocketmq2 | 192.168.56.2 | Nameserver2、Broker_Master-2       |     |
| rocketmq3 | 192.168.56.3 | Nameserver3、Broker_Master_slave-1 |     |
| rocketmq4 | 192.168.56.4 | Nameserver4、Broker_Master_slave-2 |     |

### **3.3.2、 创建数据目录**

```shell
# 在各服务器执行以下命令创建数据目录
[wangjianghua@rocketmq1 rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store

[wangjianghua@rocketmq1 rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store/commitlog

[wangjianghua@rocketmq1 rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store/consumequeue

[wangjianghua@rocketmq1 rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store/index
```

### **3.3.3、 修改日志路径**

```shell
# 在各服务器执行以下命令修改日志存储路径

[wangjianghua@rocketmq1 rocketmq]$ sed -i 's#${user.home}#/home/wangjianghua/src/rocketmq/data#g' conf/*.xml
```

### **3.3.4、 配置 RocketMQ**

```shell
# 配置Broker_Master-1
[wangjianghua@rocketmq1 rocketmq]$ cat conf/2m-2s-async/broker-a.properties

brokerClusterName=rocketmq-2m-slave-cluster-async

brokerName=rocketmq-broker-m1

brokerId=0

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876;192.168.56.3:9876;192.168.56.4:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort

brokerRole=SYNC_MASTER

flushDiskType=SYNC_FLUSH
```

```shell
# 配置Broker_Master-2
[wangjianghua@rocketmq2 rocketmq]$ cat conf/2m-2s-async/broker-b.properties

brokerClusterName=rocketmq-2m-slave-cluster-async

brokerName=rocketmq-broker-m2

brokerId=0

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876;192.168.56.3:9876;192.168.56.4:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort

brokerRole=SYNC_MASTER

flushDiskType=SYNC_FLUSH
```
  
```shell
# 配置Broker_Master_slave-1
[wangjianghua@rocketmq3 rocketmq]$ cat conf/2m-2s-async/broker-a-s.properties

brokerClusterName=rocketmq-2m-slave-cluster-async

brokerName=rocketmq-broker-m1

brokerId=1

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876;192.168.56.3:9876;192.168.56.4:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort

brokerRole=SLAVE

flushDiskType=ASYNC_FLUSH
```

```shell
# 配置Broker_Master_slave-2
[wangjianghua@rocketmq3 rocketmq]$ cat conf/2m-2s-async/broker-b-s.properties

brokerClusterName=rocketmq-2m-slave-cluster-async

brokerName=rocketmq-broker-m2

brokerId=1

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876;192.168.56.3:9876;192.168.56.4:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store1

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog1

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue1

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index1

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint1

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort1

brokerRole=SLAVE

flushDiskType=ASYNC_FLUSH
```

### **3.3.5、 启动 NameServer**

```shell
# 启动Nameserver1
[wangjianghua@rocketmq1 rocketmq]$ nohup sh bin/mqnamesrv &

# 启动Nameserver2
[wangjianghua@rocketmq2 rocketmq]$ nohup sh bin/mqnamesrv &

# 启动Nameserver3
[wangjianghua@rocketmq3 rocketmq]$ nohup sh bin/mqnamesrv &

# 启动Nameserver4
[wangjianghua@rocketmq4 rocketmq]$ nohup sh bin/mqnamesrv &
```

### **3.3.6、 启动 Broker**

```shell
# 启动Broker_Master-1
[wangjianghua@rocketmq1 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-2s-async/broker-a.properties &

# 启动Broker_Master-2
[wangjianghua@rocketmq2 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-2s-async/broker-b.properties &

# 启动Broker_Master_slave-1
[wangjianghua@rocketmq3 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-2s-async/broker-a-s.properties &

# 启动Broker_Master_slave-2
[wangjianghua@rocketmq4 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-2s-async/broker-b-s.properties &
```

### **3.3.7、 关闭服务**

```shell
# 关闭 NameServer
[wangjianghua@rocketmq* rocketmq]$ bin/mqshutdown namesrv

# 关闭 Broker
[wangjianghua@rocketmq* rocketmq]$ bin/mqshutdown broker
```

### **3.3.8、总结**

**工作原理**
在RocketMQ的多Master多Slave同步复制模式下，**Master节点**和**Slave节点**都对外提供服务，但是**Master节点**是主要的对外服务节点。

**具体工作流程如下：**
1. **消息生产:**
    - 生产者发送消息时，消息会根据负载均衡策略被路由到某个Master节点。
    - Master节点收到消息后，会将消息持久化到本地存储，同时同步复制到所有的Slave节点。
    - **只有当所有Slave节点都确认收到消息后，Master节点才会向生产者返回确认应答。**
2. **消息消费:**
    - 消费者通常会优先从Master节点消费消息，以保证消费数据的最新性。
    - 在Master节点发生故障的情况下，消费者可以自动切换到Slave节点进行消费。
3. **数据同步:**
    - Master节点会同步地将消息复制到所有的Slave节点。
    - 同步复制保证了Master节点和Slave节点的数据一致性。

**外部访问**
- **客户端访问:**
    - 客户端（生产者或消费者）在连接RocketMQ集群时，会通过NameServer获取到所有Master节点的地址。
    - 客户端会根据负载均衡策略选择一个Master节点进行连接，并发送或消费消息。

**与异步复制模式的对比**

| 特点    | 同步复制 | 异步复制  |
| ----- | ---- | ----- |
| 数据一致性 | 强一致性 | 最终一致性 |
| 性能    | 相对较低 | 相对较高  |
| 复杂度   | 相对较高 | 相对较低  |
| 容错性   | 高    | 较低    |

**总结**
在多Master多Slave同步复制模式下，**Master节点和Slave节点都对外提供服务**，但Master节点是主要的对外服务节点。这种模式保证了数据的一致性，提高了系统的可靠性，适合对数据一致性要求较高的场景。但是，同步复制会增加消息发送的延迟，降低系统的吞吐量。

**选择同步复制还是异步复制，需要根据具体的业务需求来决定。** 如果对数据一致性要求较高，可以选择同步复制；如果对性能要求较高，可以考虑异步复制。

## **3.4、 Dledger 模式**

在 RocketMQ 4.5 以前的版本大多都是采用 Master-Slave 架构来部署，能在一定程度上保证数据的不丢失，也能保证一定的可用性。

但是**这种方式缺陷很明显**，最大的问题就是当 Master Broker 挂了之后 ，**没办法让 Slave Broker 自动切换为新的 Master Broker**，需要手动更改配置将 Slave Broker 设置为 Master Broker，再重启机器，这个非常麻烦。而在手动运维的期间，可能会导致系统的不可用。

使用 Dledger 技术要求至少由三个 Broker 组成 ，一个 Master 和两个 Slave，这样三个 Broker 就可以组成一个 分组来运行。一但 Master 宕机，Dledger 就可以从剩下的两个 Broker 中选举一个 Master 继续对外提供服务。

### **3.4.1、 环境**

| 主机        | IP地址         | 组件部署                              |
| --------- | ------------ | --------------------------------- |
| rocketmq1 | 192.168.56.1 | Nameserver1、Broker_Master-1       |
| rocketmq2 | 192.168.56.2 | Nameserver2、Broker_Master_slave-1 |
| rocketmq3 | 192.168.56.3 | Nameserver3、Broker_Master_slave-2 |

### **3.4.2、 创建数据目录**

```shell
# 在各服务器执行以下命令创建数据目录
[wangjianghua@rocketmq1 rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store

[wangjianghua@rocketmq1 rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store/commitlog

[wangjianghua@rocketmq1 rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store/consumequeue

[wangjianghua@rocketmq1 rocketmq]$ mkdir -p /home/wangjianghua/src/rocketmq/data/store/index
```

### **3.4.3、 修改日志路径**

```shell
#在各服务器执行以下命令修改日志存储路径

[wangjianghua@rocketmq1 rocketmq]$ sed -i 's#${user.home}#/home/wangjianghua/src/rocketmq/data#g' conf/*.xml
```

### **3.4.4、 配置 RocketMQ**

```shell
# 配置Broker_Master-1

[wangjianghua@rocketmq1 rocketmq]$ cat conf/2m-2s-async/broker-a.properties

brokerClusterName=rocketmq-2m-slave-cluster-async

brokerName=rocketmq-broker-m1

brokerId=0

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876;192.168.56.3:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort

brokerRole=SYNC_MASTER

flushDiskType=SYNC_FLUSH

# dledger 相关的配置属性

enableDLegerCommitLog=true

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store/dledger

dLegerGroup=rocketmq-broker-m1

dLegerPeers=n0-192.168.56.1:40911;n1-192.168.56.2:40911;n2-192.168.56.3:40911

dLegerSelfId=n0
```  

```shell
# 配置Broker_Master_slave-1

[wangjianghua@rocketmq2 rocketmq]$ cat conf/2m-2s-sync/broker-a-s.properties

brokerClusterName=rocketmq-2m-slave-cluster

brokerName=rocketmq-broker-m1

brokerId=1

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876;192.168.56.3:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort

brokerRole=SLAVE

flushDiskTyp=ASYNC_FLUSH


# dledger 相关的配置属性

enableDLegerCommitLog=true

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store/dledger

dLegerGroup=rocketmq-broker-m1

dLegerPeers=n0-192.168.56.1:40911;n1-192.168.56.2:40911;n2-192.168.56.3:40911

dLegerSelfId=n1
```

```shell
# 配置Broker_Master_slave-1

[wangjianghua@rocketmq3 rocketmq]$ cat conf/2m-2s-sync/broker-a-s.properties

brokerClusterName=rocketmq-2m-slave-cluster

brokerName=rocketmq-broker-m1

brokerId=2

namesrvAddr=192.168.56.1:9876;192.168.56.2:9876;192.168.56.3:9876

defaultTopicQueueNums=4

autoCreateTopicEnable=true

autoCreateSubscriptionGroup=true

listenPort=10911

deleteWhen=04

fileReservedTime=120

mapedFileSizeCommitLog=1073741824

mapedFileSizeConsumeQueue=50000000

destroyMapedFileIntervalForcibly=120000

redeleteHangedFileInterval=120000

diskMaxUsedSpaceRatio=88

maxMessageSize=65536

sendMessageThreadPoolNums=128

pullMessageThreadPoolNums=128

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store

storePathCommitLog=/home/wangjianghua/src/rocketmq/data/store/commitlog

storePathConsumeQueue=/home/wangjianghua/src/rocketmq/data/store/consumequeue

storePathIndex=/home/wangjianghua/src/rocketmq/data/store/index

storeCheckpoint=/home/wangjianghua/src/rocketmq/data/store/checkpoint

abortFile=/home/wangjianghua/src/rocketmq/data/store/abort

brokerRole=SLAVE

flushDiskTyp=ASYNC_FLUSH


# dledger 相关的配置属性

enableDLegerCommitLog=true

storePathRootDir=/home/wangjianghua/src/rocketmq/data/store/dledger

dLegerGroup=rocketmq-broker-m1

dLegerPeers=n0-192.168.56.1:40911;n1-192.168.56.2:40911;n2-192.168.56.3:40911

dLegerSelfId=n2
``` 

### **3.4.5、 启动 NameServer**

```shell
# 启动Nameserver1
[wangjianghua@rocketmq1 rocketmq]$ nohup sh bin/mqnamesrv &

# 启动Nameserver2
[wangjianghua@rocketmq2 rocketmq]$ nohup sh bin/mqnamesrv &

# 启动Nameserver3
[wangjianghua@rocketmq3 rocketmq]$ nohup sh bin/mqnamesrv &

# 启动Nameserver4
[wangjianghua@rocketmq4 rocketmq]$ nohup sh bin/mqnamesrv &
```

### **3.3.6、 启动 Broker**

```shell
# 启动Broker_Master-1
[wangjianghua@rocketmq1 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-2s-async/broker-a.properties &

# 启动Broker_Master_slave-1
[wangjianghua@rocketmq2 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-2s-async/broker-a-s.properties &

# 启动Broker_Master_slave-2
[wangjianghua@rocketmq3 rocketmq]$ nohup sh bin/mqbroker -c conf/2m-2s-async/broker-a-s.properties &
```  

### **3.3.7、 关闭服务**

```shell
# 关闭 NameServer
[wangjianghua@rocketmq* rocketmq]$ bin/mqshutdown namesrv

# 关闭 Broker
[wangjianghua@rocketmq* rocketmq]$ bin/mqshutdown broker
```



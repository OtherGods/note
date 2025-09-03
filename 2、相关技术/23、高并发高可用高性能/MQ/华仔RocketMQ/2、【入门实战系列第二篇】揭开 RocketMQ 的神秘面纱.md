大家好，我是 **华仔**, 又跟大家见面了。

从今天开始我会带领大家去学习 **RocketMQ**，逐一剖解揭开它的神秘面纱。

# **01 总述**

在上一篇我们了解了 **RocketMQ** 的特性以及它的商业成就，在阿里内部，它既能够扛住双十一的流量洪峰，也能被当作核心组件运用在支付宝交易的关键链路上，最后被封装到阿里云商业化。

你可能觉得对于 RocketMQ 这样如此优秀的消息系统内部实现一定相当复杂吧。今天我会带领大家一起揭开 RocketMQ 神秘的面纱，一起窥探它的真面目。

这里会**将** **RocketMQ 核心的概念全部串起来**，比如：Broker、NameServer、Producer、Consumer、MessageQueue、Topic等。

为了更好地让你了解 RocketMQ 的底层设计实现，这里用**一步一图、层层递进**的方式来剖析，让你在了解每个组件的前提下，同时对 RocketMQ 有一个全局的视角的认知，对于后续深入学习原理以及源码有很大帮助。

# **02 消息队列的核心之 Broker**

这里举个例子，将消息队列作为一个**黑匣子**应该是什么样子呢？可能是下面这样：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410041856769.png)

作为**黑匣子，生产者**「**Producer**」只管向消息队列投递消息，而 **消费者**「**Consumer**」只管从消息队列拉取消息并消费，但是对于内部是如何实现的，我们目前暂时看不到其内部的实现细节。

对于业界主流的消息队列，RocketMQ 远不止这么简单，它是一个内部拥有多个组件以及组件之间相互配合协作的精密系统。

这里将视角切回来，给「**Producer**」、「**Consumer**」提供服务的只是消息队列本身吗？**它内部究竟有什么组件组成呢？**

举一个例子，假如我们现在要去「**上海**」旅游，在我们到达目的地后，接待我们的并不是「**上海**」本身，而是它的某个机场或者某个火车站。

这里「**上海**」只是对类似旅游地方的统一「**封装**」，RocketMQ 也类似，实际上内部提供服务的是 「**Broker**」，如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410041901509.png)

「**Broker**」对于 RocketMQ 来说是 「**核心中的核心**」，它负责接收 「**Producer**」发送过来的消息，并持久化，同时它还负责处理 「**Consumer**」的消费请求。

对于上图来说，消息的投递、存储以及最后的消费，「**Broker**」统统都参与了。到这里我们知道了其内部有个 「**Broker**」组件，但是对于这样一个号称「**高并发**」、「**高性能**」、「**高可用**」的分布式消息系统来说，这么重要的组件就一个怎么能行？万一挂了，整个消息系统就不能提供服务了。

所以，为了保证高可用，RocketMQ 会部署多台 Broker 组成一个集群对外提供服务，如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410051814852.png)

如果想要存储海量消息数据，那么「**Producer**」发送的消息会被「**分散**」的存储在这些「**Broker**」上，每台 「**Broker**」机器上存储的消息内容都是不同的。将所有的 「**Broker**」存储的消息全部加起来就是全部的数据，到这里，大家可能有个疑问？**既然每台 Broker 存储的消息不同，那如果某台 Broker 突然宕机了，那么这部分消息不就丢失了吗？还何谈高可用呢？**

**我们继续往下看。**

# **03 消息组织者之 Topic**

如果 RocketMQ 真是上面这么设计的话，那肯定会有问题。当然，并不会这么搞，在 RocketMQ 内部有个「**Topic**」的概念，它表示消息的分类，即一类消息的集合。

举个例子，假如某个 「**Topic**」中存的都是跟订单相关的，里面的消息可能就包括 「**订单已创建**」、「**订单已更新**」、「**订单已付款**」、「**订单已发货**」等等诸如此类的消息。

对于 RocketMQ 或者 Kafka 来说，「**Topic**」都是一个抽象概念，「**Producer**」在生产消息时候，会指定将消息投递到一个 「**Topic**」，而「**Consumer**」消费消息的时候，也会指定一个「**Topic**」进行消费，如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410051814056.png)

从上图我们能够得出一个结论：一个 「**Topic**」只会存储到某一台 「**Broker**」上，这里你可以认真的思考一下，真实情况是这样吗？

其实并不是这样的，这里我们从两个方面来说明下。
1. 假如 Topic A、Topic C 里面的消息体量非常小，而 Topic B 的消息体量非常大，那么会导致 Broker 的「**负载压力**」、「**存储压力**」变大，导致严重的数据倾斜问题。
2. 这样的设计不具备「**高可用性**」，当 Broker 2 意外宕机后，也就意味着 Topic B 里面的消息会全部丢失。

所以，为了让大家更好地了解 RocketMQ 的底层实现原理，我们还需要引入「**新的组件**」来帮助我们解密。

# **04 Topic 细分之 MessageQueue**

我们知道在 Kafka 中，为了实现「**Topic**」的扩展性，提高并发能力，内部是有个「**Partition**」的概念，那么对于 RocketMQ 来说，这里就是 「**MessageQueue**」。

假如现在有 「**Topic A**」，内部有 3 个 「**MessageQueue**」, 如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410051814445.png)

从上图得出， 分散的 「**MessageQueue**」一起组成一个完整的 「**Topic**」。跟 Topic 一样，「**MessageQueue**」也是一个**逻辑**上的概念。但是最后消息还是要被持久化到 Broker 所在机器的「**磁盘**」上的，这才是消息的最终归宿。
  
同一个 「**Topic**」下的消息会分散存储在各个「**Broker**」上，这样有2个好处： 「**能够最大程度的进行数据容灾**」、 「**能够防止数据倾斜问题**」。

这就像俗话说的 「**鸡蛋不能放在一个篮子里**」，数据被均衡的分散存储，出现数据倾斜问题的概率就降低了。

但是就算引入了「**MessageQueue**」，并对数据进行分散存储， 如果 Broker 2 挂了后，数据还是会丢失的。

之前只会丢一个 Topic 的消息，而现在 3 个 Topic 的数据都丢失了，尴尬不？

所以实际上在 RocketMQ 4.5 版本之前提供了基于「**主从架构**」的高可用机制。即将 Broker 分为 「**Master**」、「**Slave**」，主从之间定期进行数据同步。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410051815079.png)

1. **Master** 负责处理客户端的「**读写请求**」、「**存储消息**」等等。
2. **Slave** 只负责一件事情，那就是从 **Master** 同步数据。

而在 RocketMQ 4.5 之后提供了高可用集群的实现 「**Dledger**」，它会在集群发生故障时进行自动切换，不需要人工介入，会解决上面「**主从架构**」的缺陷，后面再讲它的实现原理。

我们再将视角切回来，虽然我们引入了 「**MessageQueue**」，看似可以解决所有的问题，但如果放到真实场景下来看，就会发现问题。

比如：有多台「**Broker**」组成一个大集群对外提供服务，当「**Producer**」建立连接发送数据时，应该选择往哪台 「**Broker**」上发送数据呢？
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410051815479.png)

看到上图是不是缺少类似「**配置中心**」这样一个组件，如果是配死在项目中的话，如果配置的这台 Broker 突然挂掉了或者在业务高峰期挂掉了，难道我们要去修改项目的配置文件重新发布吗？ 这明显不靠谱。

因此为了解决这个问题，我们还需要引入新的组件 「**NameServer**」。

# **05 RocketMQ 集群大脑之 NameServer**

「**NameServer**」用于存储整个 RocketMQ 集群的 「**元数据**」，就像 Kafka 会采用 Zookeeper 来存储、管理集群的元数据一样。

那么「**NameServer**」中存放的 「**元数据**」都有哪些呢？
1. 集群有哪些 Topic。
2. 这些 Topic 的 MessageQueue 分别在哪些 Broker 节点上。
3. 集群中都有哪些活跃的 Broker 节点。
4. 。。。

那 「**NameServer**」都是如何感知这些信息呢？这些信息不会凭空出现在 「**NameServer**」中，而是注册进来的。
1. Broker 在启动时会将自己注册到 NameServer 上，且通过心跳的方式持续地更新元数据。
2. Producer、Consumer 都会和 NameServer 建立连接、进行交互来动态地获取集群中的数据，这样一来就知道自己该连接哪台 Broker 了。

如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410051815793.png)

这次看上去就比较完美了，「**Broker**」、「**Producer**」、「**Consumer**」动态的将自己注册到「**NameServer**」上，但是眼尖的同学就发现了这里 「**NameServer**」还是个单点，既然说是 RocktMQ 集群的大脑，如果只有一个「**NameServer**」，挂掉了话，岂不是整个集群都无法正常工作了。

没错。所以实际的生产环境中，会**部署多台 NameServer 组成一个集群对外提供服务**，如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410051815539.png)

这里我们可以将「**NameServer**」理解成一个无状态的节点。

既然是要存储元数据，那么怎么还能是无状态呢？ 这是因为「**Broker**」会将自己注册到 **每一个**「**NameServer**」上，这样每个「**NameServer**」上都有**完整**完整的数据，所以我们可以将多个「**NameServer**」看成是一个无状态节点。所以这样的多实例部署保证了整个 RocketMQ 的高可用。

# **05 总结**

本文通过一个场景驱动的方式剖析了 RocketMQ 各个组件的演进过程，让你对 RocketMQ 整体的架构有了一个较为全面的认知。对于后续的深入学习，奠定了基础，就可以更加得心应手。



Redis Sentinel：如何实现自动化地故障转移？

普通地主从赋值方案下，一旦master宕机，我们需要从slave中手动选择一个新的master，同时需要修改应用方地主节点地址，还需要命令所有从节点去复制新地主节点，整个过程需要人工干预。人工干预大大增加了问题地处理时间以及出错地可能性。



我们可以借助Redis官方地Sentinel（哨兵）方案来帮我们解决这个痛点，实现自动化故障切换。

# 1、什么是Sentinel？

**Sentinel（哨兵）**只是Redis地一种运行模式，不提供读写服务，默认运行在26379端口上，依赖于Redis工作。Redis Sentinel地稳定版本是在Redis2.8之后发布地。

Redis在Sentinel这种特殊地运行模式下，使用专门地命令表，也就是说普通模式运行下地Redis命令将无法使用。

通过下面地命令就可以让Redis以Sentinel地方式运行：
![image-20221015161831704](D:\Tyora\AssociatedPicturesInTheArticles\5-Redis Sentinel：如何实现自动化地故障转移？\image-20221015161831704.png)

Redis源码中的sentinel.conf是用来配置Sentinel的，一个常见的最小配置如下所示：
![image-20221015161944144](D:\Tyora\AssociatedPicturesInTheArticles\5-Redis Sentinel：如何实现自动化地故障转移？\image-20221015161944144.png)

Redis Sentinel实现Redis集群高可用，只是在主从复制实现集群的基础下，多了一个Sentinel角色来帮助我们监控Redis节点的运行状态并自动实现故障转移。

在master节点出现故障的时候，Sentinel会帮助我们实现故障转移，自动根据一定规则选出一个slave升级为master，确保整个Redis系统的可用性。整个过程完全自动不需要人工介入。

![image-20221015162243692](D:\Tyora\AssociatedPicturesInTheArticles\5-Redis Sentinel：如何实现自动化地故障转移？\image-20221015162243692.png)

# 2、Sentinel有什么作用？

根据Redis Sentinel 官方文档的介绍，sentinel节点主要可以提供四个功能：

1. **监控：**监控所有redis节点（包括sentinel节点自身）的状态是否正常
2. **故障转移：**如果一个master出现故障，Sentinel会帮助我们实现故障转移，自动将某一台slave升级为master，确保整个Redis系统的可用性
3. **通知：**通知slave新的master连接信息，让他们执行replicaof成为新的master的slave
4. **配置提供：**客户端连接sentinel请求master的地址，如果发生故障转移，sentinel会通知新的master连接信息给客户端。

Redis Sentinel本身设计就是一个分布式系统，建议多个sentinel节点写作运行，这样做的好处是：

1. 多个Sentinel节点通过投票的方式来确定Sentinel节点是否真的不可用，避免误判（比如网络问题可能导致误判）。
2. Sentinel自身就是高可用

**如果要实现高可用，建议将哨兵Sentinel配置成单数且大小等于3台**

一个最简易的Redis Sentinel集群如下锁是（官网中的一个例子），其中:

1. M1表示master，R2、R3表示slave；
2. S1、S2、S3都是Sentinel
3. quorum表示判定master失效最少需要的仲裁节点数量。这里的值为2，也就是说当有2个Sentinel认为master失效时，master才算真正失效

![image-20221015164355443](D:\Tyora\AssociatedPicturesInTheArticles\5-Redis Sentinel：如何实现自动化地故障转移？\image-20221015164355443.png)

如果M1出现问题，只要S1、S2、S3其中的两个投票赞同的话，就会开始故障转移工作，从R2或R3中重新选出一个作为master。

# 3、Sentinel如何检测节点是否下线？

> 相关问题：
>
> 1. 主观与客观下线的区别？
> 2. Sentinel是如何实现故障转移的？
> 3. 为什么建议部署多个sentinel饥饿点（哨兵集群）？

Redis Sentinel中有两个下线（Down）的概念：

1. **主管下线：**Sentinel节点认为某个Redis节点已经下线了（主观下线），但是还不确定，需要其他Sentinel节点的投票
2. **客观下线：**法定数量（通常为过半）的Sentinel节点认定某个Redis节点已经下线（客观下线），那它就是算真的下线了。

也就是说，**主观下线**当前的Sentinel自己认为节点宕机，客观下线是Sentinel整体达成一致认为节点宕机。

每个Sentinel节点以每秒钟一次的频率像整个集群钟的master、slave以及其他Sentinel节点发送一个PING命令。

![image-20221015165632618](D:\Tyora\AssociatedPicturesInTheArticles\5-Redis Sentinel：如何实现自动化地故障转移？\image-20221015165632618.png)

如果对应的节点超过规定的时间没有进行有效回复的话，就会被认定为是**主观下线**。注意！这里的有效回复不一定是PONG，可以是LOADINNG或MASTERDOWN。

![image-20221015165830635](D:\Tyora\AssociatedPicturesInTheArticles\5-Redis Sentinel：如何实现自动化地故障转移？\image-20221015165830635.png)

如果被认定为主观下线的是slave的话，sentinel不会做什么事情，因为slave下线对Redis集群的影响不大，Redis集群对外正常提供服务。但如果是master被认定为主观下线就不一样了，sentinel整体还要对其进一步核实，确保master是真的下线了。

所以sentinel节点要以每一次的频率确认master的确下线了，当法定数量（通常过半）的sentinel节点认定master已经下线了，master才被判定为客观下线。这样做的目的是为了防止误判，毕竟故障转移的开销还是比较大的，这也是为什么Redis官方推荐部署多个sentinel节点（哨兵集群）。

![image-20221015170243093](D:\Tyora\AssociatedPicturesInTheArticles\5-Redis Sentinel：如何实现自动化地故障转移？\image-20221015170243093.png)

随后，sentinel中有一个Leader的角色来负责故障转移，也就是自动的从slave中选出一个新的master并执行完相关的一些工作（比如通知slave新的master连接信息，让它们执行replicaof成为新的master的slave）。

如果没有足够数量的sentinel节点认定master已经下线的话，当master能对sentinel的PING命令进行有效回复之后，master也就不再被认定为主观下线，回归正常。



# 4、Sentinel如何选出新地master？

slave必须是在线状态才能参加新的master的选举，筛选出所有在线的slave之后，通过下面三个维度进行最后的筛选（优先级依次降低）：

1. **slave优先级：**可以通过slave-proiority的优先级，优先级越高得分越高，优先级最高的直接成为新的master。如果没有优先级最后高的，再判断复制进度
2. **复制进度：**Sentinel总是希望选择出数据最完整（与旧master数据最接近）也就是复制进度最快的slave被提升为新的master，复制进度越快得分也就越高
3. **runid（运行id）：**通常经过前面两轮以及筛选出新的master，万一真要有多个slave的优先级和复制进度一样的话，那就runid小的成为新的master，每个redis节点启动时都有一个40字节随机字符串作为运行id。

# 5、如何从Sentinel集群中选择出Leader？

我们千米按说路，当sentinel集群确认有master客观下线了，就会开始故障转移流程，故障转移流程的第一步就是在sentinel集群选择一个leader，让leader来负责完成故障转移。

**如何选择出Leader角色呢？**

这就需要用到分布式领域的共识算法了。简单来说，共识算法就是让分布式系统钟的节点就一个问题达成共识。在sentinel选举leader这个场景下，这些sentinel要达成的共识就是谁才是leader。

![image-20221015174754587](D:\Tyora\AssociatedPicturesInTheArticles\5-Redis Sentinel：如何实现自动化地故障转移？\image-20221015174754587.png)

1. [Raft算法详解](https://javaguide.cn/distributed-system/theorem&algorithm&protocol/raft-algorithm.html)
2. [Raft协议实战之Redis Sentinel的选举Leader源码解析](https://cloud.tencent.com/developer/article/1021467)

# 6、Sentinel可以防止脑裂吗？

![image-20221015175123409](D:\Tyora\AssociatedPicturesInTheArticles\5-Redis Sentinel：如何实现自动化地故障转移？\image-20221015175123409.png)

![image-20221015175136483](D:\Tyora\AssociatedPicturesInTheArticles\5-Redis Sentinel：如何实现自动化地故障转移？\image-20221015175136483.png)

![image-20221015175151883](D:\Tyora\AssociatedPicturesInTheArticles\5-Redis Sentinel：如何实现自动化地故障转移？\image-20221015175151883.png)








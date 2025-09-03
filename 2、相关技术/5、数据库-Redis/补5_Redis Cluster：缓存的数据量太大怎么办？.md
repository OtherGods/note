![image-20221015181819445](D:\Tyora\AssociatedPicturesInTheArticles\6-Redis Cluster：缓存的数据量太大怎么办？\image-20221015181819445.png)

# 1、为什么需要Redis Cluster？

高并发场景下，使用Redis主要会遇到两个问题：

1. **缓存的数据量太大：**实际缓存的数量可以达到几十G，甚至是成百上千个G
2. **并发量要求太大：**虽然Redis号称单机可以支持10W并发，但是实际项目中，不可靠因素太多，比如一些复杂的写/读操作就可能会让这个并发量大打折扣。而，就算真的可以实际支持10W并发，达到瓶颈了，可能没有办法满足系统的实际需求。

<u>***主从复制***</u>和***<u>Redis Sentinel</u>***这两种方案<font color = "red">***本质都是通过增加主库（master）的副本（slave）数量的方式来提高Redis服务的整体可用性和读吞吐量，都不支持横向扩展来缓解写压力以及解决缓存数量过大的问题。***</font>

![image-20221015181257145](D:\Tyora\AssociatedPicturesInTheArticles\6-Redis Cluster：缓存的数据量太大怎么办？\image-20221015181257145.png)

对于这两种方案来说，如果写压力太大或者缓存数量太大的话，我们可以考虑提高服务器硬件的配置。不过，提高硬件配置成本太高，能力有限，无法动态扩容缩容，局限性太大。从本质上来说，靠堆硬件配置的方式并没有实质性的解决问题，依然无法满足高并发场景下分布式缓存的要求。

通常情况下，更建议使用Redis切片集群这种方式，更能满足高并发场景下分布式缓存的要求。

简单来说，***Redis切片集群就是部署多台Redis主节点（master），这些节点之间平等，并没有主从之说，同时对外提供读/写服务。***缓存的数据库相对均匀的分布在这些Redis实例上，客户端的请求通过路由规则转发到目标master上。

为了保障集群整体的高可用，我们需要保证集群中每一个master的高可用，可以通过主从复制给每个master配置一个或者多个从节点（slave）。

![image-20221015182813741](D:\Tyora\AssociatedPicturesInTheArticles\6-Redis Cluster：缓存的数据量太大怎么办？\image-20221015182813741.png)

**Redis切片集群对于横向扩展非常友好，只需要增加Redis节点到集群中即可。**

在Redis3.0之前，我们通常使用的是Twemproxy、Codis这类开源分片集群方案，但是这两者索然未被淘汰，但是官方已经没有继续维护了。

![image-20221015215119616](D:\Tyora\AssociatedPicturesInTheArticles\6-Redis Cluster：缓存的数据量太大怎么办？\image-20221015215119616.png)

Redis Cluster通过**分片**来进行数据管理，提供主从复制、故障转移等开箱即用的功能，可以非常方便的帮助我们解决Redis大量数据量缓存以及Redis服务高可用的问题。

Redis Cluster这种方案可以很方便的进行**横向拓展**，内置了开箱即用的解决方案。当Redis Cluster的处理能力达到瓶颈无法满足系统要求的时候，直接动态添加Redis节点到集群中即可。根据官方文档中的介绍，Redis Cluster支持扩展到1000个节点。反之，当Redis Cluster的处理能力远远满足系统要求，同样可以动态删除集群中的Redis节点，节省资源。

![image-20221015215558141](D:\Tyora\AssociatedPicturesInTheArticles\6-Redis Cluster：缓存的数据量太大怎么办？\image-20221015215558141.png)

可以说，Redis Cluster的动态扩容和收缩是其最大的优势。

虽说Redis Cluster可以扩展到1000个节点，但是不建议这样做，应该金陵避免集中的节点过多。这是因为Redis Cluster中的各个节点基于Gossip协议来进行通信共享信息，当节点过多时，Gossip协议的效率会显著下降，通信成本剧增。

最后总结一下Redis Cluster的主要优势：

1. 可以横<font color = "red">***向扩展缓解写压力和存储压力***</font>，***支持动态扩容和缩容***
2. ***具备主从复制，故障转移***（内置了Sentinel机制，无需单独部署Sentinel集群）等开箱即用的功能。

# 2、一个最基本的Redis Cluster架构是怎样的？

为了保证高可用，Redis Cluster至少需要3个master以及3个slave，也就是说每个master必须有1个slave。master和slave之间做主从复制，slave会实时同步master上的数据。

不是同于Redis主从架构，这里的slave不对外提供读服务，主要用来保障master的高可用，当master出现故障的时候替代它。

如果master只有一个slave的话，master宕机之后就直接使用这个slave替代master继续提供服务。假设master1出现故障，slave1会直接替代master1，保障Redis Cluster的高可用。

![image-20221015220538259](D:\Tyora\AssociatedPicturesInTheArticles\6-Redis Cluster：缓存的数据量太大怎么办？\image-20221015220538259.png)

如果master有多个slave的话，Redis Cluster中的其他节点会从这个master的所有slave中选出一个替代master继续提供服务。Redis Cluster总是希望数据最完整的slave被提升为新的master。

Redis Cluster是去中心化的，任何一个master出现故障，其他的master节点不受影响，因为key找的是哈希槽而不是Redis节点。不过Redis Cluster至少要保证宕机的master有一个slave可用。

![image-20221015221345611](D:\Tyora\AssociatedPicturesInTheArticles\6-Redis Cluster：缓存的数据量太大怎么办？\image-20221015221345611.png)



# 3、Redis Cluster是如何分片的？





# 4、为什Redis Cluster的哈希槽是16384？





# 5、Redis Cluster如何重新分配哈希槽





# 6、Redis Cluster扩容缩容期可以提供服务吗？





# 7、Redis Cluster中的节点是怎样进行通信的？


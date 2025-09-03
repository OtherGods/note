大家好，我是 **华仔**, 又跟大家见面了。

今天我们来讲解下 **RocketMQ 的核心配置，比较简单，** 分为五部分 「**生产者核心配置**」、「**客户端公共配置**」、「**Broker 核心配置**」、「**消费者核心配置**」、「 **NameServer 核心配置**」。

# **01 生产者核心配置详解**

RocketMQ 生产者的核心配置参数名包括：

## **1.1、producerGroup**

该参数是指生产者所属的组别，用于标识一类生产者。生产者组名的作用是**组织和管理一类生产者**，可以进行负载均衡和容错等操作。其默认值为 **DEFAULT_PRODUCER**。

## **1.2、createTopicKey**

该参数是指在发送消息时， 自动创建服务器不存在的 Topic， 需要指定 Key， 该 Key 可用于配置发送消息所在topic 的默认路由。其默认值为 **TBW102**。 这个 topicKey 的作用是**当 Producer 发送一个不存在的 Topic 消息时，Broker 可以基于它的配置自动创建新的 Topic，新创建的 Topic 权限、读写队列数等都会继承自 TBW102,因此它的配置极其重要。不过一般情况下，还是推荐你提取创建好 Topic**。

其源码实现流程如下图，待源码剖析再详细剖析：
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201436971.png)

## **1.3、defaultTopicQueueNums**

该参数是指在发送消息， 自动创建服务器不存在的 Topic 时， 默认要创建的队列数。其默认值为 **4**。

## **1.4、topicQueueNums**

该参数与上一个类似，也是指主题 Topic 下面的队列数量，其默认值为 **4**。

## **1.5、sendMsgTimeout**

该参数是指发送消息超时时间， 单位毫秒，其默认值为 **10000**。

## **1.6、compressMsgBodyOverHowmuch**

该参数是指消息 Body 超过多大开始压缩 「**consumer 收到消息会自动解压缩**」，单位字节。其默认值为 **4096**。

## **1.7、retryAnotherBrokerWhenNotStoreOk**

该参数是指如果发送消息返回 sendResult， 但是 sendStatus != SEND_OK， 是否要重试发送。其默认值为 **false**。

## **1.8、retryTimesWhenSendFailed**

该参数是指如果消息发送失败时， 最大重试次数， **只对同步发送模式起作用**。其默认值为 **2**。

## **1.9、maxMessageSize**

该参数是指客户端限制的消息大小，超过报错，同时服务端也会限制（默认128k)。其默认值为 **131072**。

## **1.10、transactionCheckListener**

该参数是指事务消息回查监听器， 如果发送事务消息必须设置。

## **1.11、checkThreadPoolMinSize**

该参数是指 Broker 端回查 Producer 事务状态时， 线程池最小线程数。其默认值为 **1**。

## **1.12、checkThreadPoolMaxSize**

该参数是指 Broker 端回查 Producer 事务状态时， 线程池最大线程数。其默认值为 **1**。

## **1.13、checkRequestHoldMax**

该参数是指 Broker 端回查 Producer 事务状态时， Producer 本地缓冲请求队列大小。其默认值为 **2000**。

## **1.14、RPCHook**

该参数是指在 Producer 端创建时传入的， 包含「**消息发送前预处理**」、「**消息响应后处理**」两个接口， 用户可以在第一个接口中做一些安全控制或者其他操作。其默认值为 **null**。

## **1.15、topicdefaultTopicQueueNums**

该参数是指在发送消息时，自动创建服务器不存在的 topic，默认创建的队列数。其默认值为 **4**。

# **02 客户端公共核心配置详解**

## **2.1、namesrvAddr**

该参数是指 Name Server 地址列表，多个 NameServer 地址用分号隔开。

## **2.2、clientIP**

该参数是指客户端本机 IP 地址，某些机器会发生无法识别客户端 IP 地址情况，需要应用在代码中强制指定。其默认值为 **本机IP**。

## **2.3、instanceName**

该参数是指客户端实例名称，客户端创建的多个 Producer、 Consumer 实际是共用一个内部实例，其中这个实例包含网络连接、线程资源等。其默认值为 **DEFAULT**。

##   
**2.4、clientCallbackExecutorThreads**

该参数是指通信层异步回调线程数。其默认值为 **4**。

## **2.5、pollNameServerInterval**

该参数是指轮询 NameServer 间隔时间，单位毫秒。其默认值为 **30000**。

## **2.6、heartbeatBrokerInterval**

该参数是指向 Broker 端发送心跳间隔时间，单位毫秒。其默认值为 **30000**。

## **2.7、persistConsumerOffsetInterval**

该参数是指持久化 Consumer 消费进度间隔时间，单位毫秒。其默认值为 **5000**。

# **03 Broker 核心配置详解**

## **3.1、listenPort**

该参数是指 Broker 端对外服务的监听端口。其默认值为 **10911**。

## **3.2、namesrvAddr**

该参数是指 Name Server 地址列表，多个 NameServer 地址用分号隔开。

## **3.2、brokerIP1**

该参数是指 当前 broker 监听的 IP。其默认值为 **网卡的 InetAddress**。

## **3.3、brokerIP2**

该参数是指当存在主从 broker 时，如果在 broker 主节点上配置了 brokerIP2 属性，broker 从节点会连接主节点配置的 brokerIP2 进行同步。其默认值为 **网卡的 InetAddress**。

## **3.4、brokerName**

该参数是指 broker 的名称。其默认值为 **null**。

## **3.4、brokerClusterName**

该参数是指 broker 所属集群的名称。其默认值为 **DefaultCluster**。

## **3.5、brokerId**

该参数是指 BrokerId, 必须是大于等于0的整数，0 表示 Master, 大于0 表示 Slave, 一个 Master 可以挂多个Slave, Master 和 Slave 通过 BrokerName 来配对。其默认值为 **0**。

## **3.6、storePathCommitLog**

该参数是指 commitLog 的存储路径。其默认值为 **$HOME/store/commitlog/**。

## **3.7、storePathConsumerQueue**

该参数是指 consumer queue 消费队列存储路径。其默认值为 **$HOME/store/consumerqueue/**。

## **3.8、storePathIndex**

该参数是指消息索引存储队列。其默认值为 **$HOME/store/index/**。

## **3.9、storePathRootDir**

该参数是指文件存储路径。其默认值为 **$HOME/store/**。

## **3.10、storeCheckpoint**

该参数是指存储 checkpoint 文件路径。其默认值为 **$HOME/store/checkpoint/**。

## **3.11、mapedFileSizeCommitLog**

该参数是指 commit log 的映射文件大小。其默认值为 **1024 * 1024 * 1024 即 1G**。

## **3.12、deleteWhen**

该参数是指删除时间点，其默认值为 **凌晨 4 点**。删除文件时有很多磁盘读，这个默认值是合理的，有条件的话还是建议低峰删除。

## **3.13、fileReservedTime**

该参数是指允许文件保留的最长时间。其默认值为 **72 小时**。当文件最后一次更新的时间到现在的时间超过该参数的时间值，则认为是过期文件，那么这个文件将会被删除。

另外还有 2 个参数跟删除有关。

## **3.14、deletePhysicFilesInterval**

该参数是指删除物理文件的时间间隔，其默认值为 **100 ms**。在一次定时任务触发时，可能会有多个物理文件超过过期时间可被删除， 因此删除一个文件后需要间隔多长时间才能再删除另外一个文件，该值指定两次删除文件的间隔时间。

由于删除文件是一个非常耗费 IO 的操作，会引起消息消费的延迟（相比于正常情况下），所以不建议直接删除所有过期文件。

## **3.15、destroyMapedFileIntervalForcibly**

该参数是指在删除过期文件时，如果该文件被其他线程所引用「**比如读取数据等**」，此时会阻止此次删除任务操作，同时在第一次试图删除该文件时将其标记为不可用并且记录当前时间戳 。**该参数表示文件在第一次删除拒绝后，文件能保存的最大时间，在此时间内一直会被拒绝删除，当超过该时间后，会将引用每次减去1000，直到引用大于等于0为止，会被强制删除该文件**。

## **3.16、redeleteHangedFileInterval**

该参数是指重新删除 commitLog 间隔时间，其默认值为 **1000 * 120 = 120000** 。

当第一次执行删除定时任务时，一定会执行并记录时间戳，每次执行更新时间戳为当前执行时间，当「**当前时间 - 上一次重试时间戳 > redeleteHangedFileInterval**」时执行：取第一个 commitLog 文件即 commitLog目录下最早创建的文件，判断该文件是否已经执行过 mappedFile.destroy == true「**正常情况下是执行过**」，则删除执行 mappedFile.destroy。

最后不管执行 mappedFile.destroy 失败或成功都会记录"re delete"。

[https://www.tabnine.com/code/java/methods/org.apache.rocketmq.store.MappedFile/destroy](https://www.tabnine.com/code/java/methods/org.apache.rocketmq.store.MappedFile/destroy)

## **3.17、brokerRole**

该参数是指 Broker 的角色。有以下三种：
1. ASYNC_MASTER 是指异步复制 Master。
2. SYNC_MASTER 同步双写 Master。
3. SLAVE 从节点。

其默认值为 **ASYNC_MASTER**。

## **3.18、flushDiskType**

该参数是指刷盘方式，主要有两种，SYNC_FLUSH/ASYNC_FLUSH。
1. SYNC_FLUSH 是指同步刷盘模式，当消息来了之后，尽可能快地从内存持久化到磁盘上，保证尽量不丢消息，性能会有损耗。
2. ASYNC_FLUSH 是指异步刷盘模式，消息到了内存之后，不急于马上落盘，极端情况可能会丢消息，但是性能较好。

其默认值为 **ASYNC_FLUSH 模式**。

## **3.19、maxTransferBytesOnMessageInMemory**

该参数是指消费者端单次 pull 消息（内存）传输的最大字节数。其默认值为 **262144**。

## **3.20、maxTransferCountOnMessageInMemory**

该参数是指消费者端单次 pull 消息（内存）传输的最大条数。其默认值为 **32 条**。

## **3.21、maxTransferBytesOnMessageInDisk**

该参数是指消费者端单次 pull 消息（磁盘）传输的最大字节数。其默认值为 **65535**。
## **3.22、maxTransferCountOnMessageInDisk**

该参数是指消费者端单次 pull 消息（磁盘）传输的最大条数。其默认值为 **8**。
## **3.23、messageIndexEnable**

该参数是指是否开启消息索引功能。其默认值为 **true**。

## **3.24、messageIndexSafe**

该参数是指是否提供安全的消息索引机制，索引保证不丢。其默认值为 **false**。

## **3.25、autoCreateTopicEnable**

该参数是指是否自动创建 Topic。其默认值为 **true**。开发测试环境可以设置为 **true**，生产环境建议设置为 **false**。

## **3.26、autoCreateSubscriptionGroup**

该参数是指是否允许 broker 自动创建订阅组。其默认值为 **true**。开发测试环境可以设置为 **true**，生产环境建议设置为 **false**。

## **3.27、mapedFileSizeConsumerQueue**

该参数是指 ConsumeQueue 每个文件的存储条数。其默认值为 **300000**。

## **3.28、diskMaxUsedSpaceRatio**

该参数是指检测可用的磁盘空间大小，其默认值为 **75%**。 当磁盘被占用超过75%，消息写入会直接报错。

## **3.29、sendMessageThreadPoolNums**

该参数是指发送生产消息的线程数，这个线程干的事情很多，**建议设置为 2 ~ 4**。但太多也没有什么用，因为最终写 commit log 的时候只有一个线程能拿到锁。

## **3.30、pullMessageThreadPoolNums**

该参数是服务端处理消息拉取线程池线程数量，其默认值为 **16**。

## **3.31、flushCommitLogTimed**

该参数是指控制刷盘线程阻塞等待的方式，低版本 flushCommitLogTimed 为 false，默认使用CountDownLatch，而高版本则直接使用 [Thread.sleep](http://thread.sleep/) 替代。 **其默认值不合理，异步刷盘该参数应该设置为 true，导致频繁刷盘，对性能影响极大**。

## **3.32、diskSpaceCleanForciblyRatio**

该参数是指磁盘占用的阈值。当检查磁盘是否充足，若是磁盘占用超过百分之85(**其默认值**)时，则会触发过期文件删除操作。

## **3.33、abortFile**

该参数是指临时文件，主要记录是否正常关闭。其默认值为 **$HOME/store/abort**。

## **3.34、sendThreadPoolQueueCapacity**

该参数是指处理生产消息的队列大小，默认值为 **10000**。 可能有点小，比如 5 万 TPS（异步发送）的情况下，卡 200ms 就会爆。设置比较小的数字可能是担心有大量大消息撑爆内存（比如 100K 的话， 1 万个的消息大概占用 1G 内存，也还好），具体可以自己算，如果都是小消息，可以把这个数字改大。可以修改 Broker 参数限制 Client 发送大消息。

## **3.35、brokerFastFailureEnable**

该参数是指Broker 端快速失败（限流），其默认值为 **true**。它会和下面两个参数配合使用。这个机制可能有争议，client 设置了超时时间，如果 client 还愿意等，并且 sendThreadPoolQueue 还没有满，不应该失败，sendThreadPoolQueue 满了自然会拒绝新的请求。但如果 Client 设置的超时时间很短，没有这个机制可能导致消息重复。可以自行决定是否开启。理想情况下，能根据 Client 设置的超时时间来清理队列是最好的。

## **3.36、waitTimeMillsInSendQueue**

该参数是指发送队列等待时间，其默认值为 **200 ms**。200 ms 很容易导致发送失败，建议改大，比如 1000 ms。

## **3.37、osPageCacheBusyTimeOutMills**

该参数是指设置 PageCache 系统超时的时间，其默认值为 **1000 ms**。如果内存比较多，比如 32G 以上，建议改大点。

# **04 消费者核心配置详解**

这里分两类：pullConsumer、pushConsumer。对应文档：[https://help.aliyun.com/document_detail/444764.htm?spm=a2c4g.444759.0.0.c3751d003S7eoS#concept-2228658](https://help.aliyun.com/document_detail/444764.htm?spm=a2c4g.444759.0.0.c3751d003S7eoS#concept-2228658)

## **4.1 pullConsumer**

### **4.1.1 consumerGroup**

该参数是指 Consumer 组名，多个 Consumer 如果属于一个应用，订阅同样的消息，且消费逻辑一致，则应将它们归为同一组。

### **4.1.2 brokerSuspendMaxTimeMills**

该参数是指长轮询，即 Consumer 端拉消息请求在 Broker 端挂起最长时间，单位毫秒。其默认值为 **20000**。

### **4.1.3 consumerPullTimeout**

该参数是指非长轮询，即 Consumer 端拉消息超时时间，单位毫秒。其默认值为 **20000**。

### **4.1.4 consumerTimeoutMillisWhenSuspend**

该参数是指长轮询，即 Consumer 拉消息请求 Broker 挂起超过指定时间后客户端就认为超时，单位毫秒。其默认值为 **30000**。

### **4.1.5 messageModel**

该参数是指消息类型，支持一下两种：「**集群消费模式 MessageModel.CLUSTERING**」、「**广播模式 MessageModel.BROADCASTING**」。

**集群消费模式:** 同一消费者组内的每个消费者，只消费到Topic的一部分消息，所有消费者消费的消息加起来就是Topic的所有消息。

**广播模式:** 同一消费者组内的每个消费者，都消费到Topic的所有消息。

[https://help.aliyun.com/document_detail/43163.html?spm=a2c4g.445491.0.0.31441e6aMzxAuE](https://help.aliyun.com/document_detail/43163.html?spm=a2c4g.445491.0.0.31441e6aMzxAuE)

### **4.1.6 messageQueueListener**

该参数是指监听队列变化。

### **4.1.7 offsetStore**

该参数是指消费进度存储。

### **4.1.8 registerTopics**

该参数是指注册的 topic 集合。

### **4.1.9 allocateMessageQueueStrategyRebalance**

该参数是指算法实现策略。

## **4.2 pushConsumer**

### **4.2.1 consumerGroup**

该参数是指 Consumer 组名，多个 Consumer 如果属于一个应用，订阅同样的消息，且消费逻辑一致，则应将它们归为同一组。其默认值为 **DEFAULT_CONSUMER**。

### **4.2.2 messageModel**

同上 4.1.5。

### **4.2.3 consumerFromWhere**

该参数是指 Consumer 启动后，默认从什么位置开始消费。其默认值 **CONSUME_FROM_LAST_OFFSET**。

总共有三种消费位置。
1. CONSUME_FROM_LAST_OFFSET：第一次启动从队列最后位置消费，后续再启动接着上次消费的进度开始消费。
2. CONSUME_FROM_FIRST_OFFSET：第一次启动从队列初始位置消费，后续再启动接着上次消费的进度开始消费。
3. CONSUME_FROM_TIMESTAMP：第一次启动从指定时间点位置消费，后续再启动接着上次消费的进度开始消费。

### **4.2.4 allocateMessageQueueStragy**

该参数是指算法实现策略。

### **4.2.5 subscription**

该参数是指订阅关系。文档：[https://help.aliyun.com/document_detail/444760.html?spm=a2c4g.440346.0.0.264c4355RG6yOD](https://help.aliyun.com/document_detail/444760.html?spm=a2c4g.440346.0.0.264c4355RG6yOD)。

### **4.2.6 messageListener**

该参数是指消息监听器。

### **4.2.7 offsetStore**

该参数是指消费进度存储。

### **4.2.8 consumerThreadMin**

该参数是指消费线程池数量。其默认值为 **10**。

### **4.2.9 consumerThreadMax**

该参数是指消费线程池数量。其默认值为 **20**。

### **4.2.10 consumerConcurrentlyMaxSpan**

该参数是指单队列并行消费允许的最大跨度。其默认值为 **2000**。

### **4.2.11 pullThresholdForQueue**

该参数是指拉消息本地队列缓冲消息最大数。其默认值为 **1000**。

### **4.2.12 pullinterval**

该参数是指拉消息间隔，由于是长轮询，所以为 0，但是如果应用了流控，也可以设置大于0的值，单位毫秒。其默认值为 **0**。

### **4.2.13 consumeMessageBatchMaxSize**

该参数是指批量消费，一次消费多少条消息。其默认值为 **1**。

### **4.2.14 pullBatchSize**

该参数是指批量拉消息，一次最多拉多少条。其默认值为 **32**。

# **05 NameServer 核心配置详解**

## **5.1 serverWorkerThreads**

该参数是指 Netty 业务线程池线程个数，其默认值为 **8**。

## **5.2 serverCallbackExecutorThreads**

该参数是指 Netty 回调执行任务线程池线程个数。Netty网络设计，根据业务类型会创建不同的线程池，比如处理消息发送、消息消费、心跳检测等。

## **5.3 serverSelectorThreads**

该参数是指 I/O 线程池线程个数，其默认值为 **3**。主要是 NameServer Broker 端解析请求、返回相应的线程个数。这类线程主要是处理网络请求的，解析请求包， 然后转发到各个业务线程池完成具体的业务操作，然后将结果再返回调用方。

类似 Kafka 中的 「**KafkaRequestHandlerPool**」线程池中 「**KafkaRequestHandler**」线程。

## **5.4 serverOnewaySemaphoreValue**

该参数是指单次消息请求最大并发度，其默认值为 **256**。

## **5.5 serverAsyncSemaphoreValue**

该参数是指异步消息请求最大并发度，其默认值为 **64**。

## **5.5 serverChannelMaxIdleTimeSeconds**

该参数是指网络最大空闲时间，其默认值为 **120 秒**。










大家好，我是 **华仔**, 又跟大家见面了。

今天我们来讲解下 **RocketMQ 的高级特性以及原理实现**，内容相对比较多，不着急，接下来我们娓娓道来。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242351658.png)

# **01 消息发送**

生产者会向消息中间件写入消息，不同的业务场景需要生产者采用不同的写入策略。比如「**同步发送**」、「**异步发送**」、「**OneWay 发送**」、「**延迟发送**」、「**事务消息发送**」等。

这些默认都是通过 [DefaultMQProducer](http://defaultmqproducer/) 类来发送的，其发送消息要经过以下五个步骤：
1. 设置 Producer 的 GroupName。
2. 设置 InstanceName，当一个 Jvm 需要启动多个 Producer 的时候，通过设置不同的 InstanceName 来区分，不设置的话系统使用默认名称「**DEFAULT**」。
3. 设置发送失败重试次数，当网络出现异常的时候，这个次数影响消息的重复投递次数。想保证不丢消息，可以设置多重试几次。
4. 设置 NameServer 地址。
5. 组装消息并发送。

## **1.1 消息发送注意事项**

### **1.1.1 Tags 使用**

一个应用尽可能用一个 Topic，而消息子类型则可以用 tags 来标识。tags 可以由应用自由设置，只有生产者在发送消息设置了 tags，消费方在订阅消息时才可以利用 tags 通过 broker 做消息过滤。

```java
message.setTags("TagA")
```

### **1.1.2 Keys 使用**

每个消息在业务层面的唯一标识码要设置到 keys 字段，方便将来定位消息丢失问题。服务器会为每个消息创建索引（哈希索引），应用可以通过 topic、key 来查询这条消息内容，以及消息被谁消费。由于是哈希索引，请务必保证key尽可能唯一，这样可以避免潜在的哈希冲突。
```java
// 订单Id
String orderId = "20034568923546";
message.setKeys(orderId);
```

### **1.1.3 发送返回状态**

消息发送返回状态 [SendResult#SendStatus](http://sendresult/#SendStatus)，有以下四种：
```java
public class SendResult {

	private SendStatus sendStatus;
	....
}

public enum SendStatus {
	SEND_OK, // 发生成功
	FLUSH_DISK_TIMEOUT, // 没有在规定时间内完成刷盘
	FLUSH_SLAVE_TIMEOUT, // 没有在规定时间内完成主从同步
	SLAVE_NOT_AVAILABLE, //
}
```

不同状态在不同「**刷盘策略**」和「**同步策略**」的配置下含义是不同的：
1. [FLUSH_DISK_TIMEOUT](http://flush_disk_timeout/)：表示消息发送成功但是服务器没有在规定时间内完成刷盘。此时消息已经进入服务端队列内存中，只有服务器宕机，消息才会丢失。消息存储配置参数中可以设置刷盘方式和同步刷盘时间长度，如果 Broker 服务端设置了刷盘方式为同步刷盘，即 [FlushDiskType=SYNC_FLUSH](http://flushdisktype=sync_flush/)（默认为异步刷盘方式），当 Broker 服务端未在同步刷盘时间内（默认为 5 秒）完成刷盘，则将返回该状态——刷盘超时。
2. [FLUSH_SLAVE_TIMEOUT](http://flush_slave_timeout/)：表示在主备方式下，消息发送成功，但是服务器同步到 Slave 时超时。此时消息已经进入服务端队列，只有服务端宕机，消息才会丢失。如果 Broker 服务器的角色是同步 Master，即 [SYNC_MASTER](http://sync_master/)（默认是异步 Master 即 [ASYNC_MASTER](http://async_master/)），并且从 Broker 服务器未在同步刷盘时间（默认为 5 秒）内完成与主服务器的同步，则将返回该状态——数据同步到Slave服务器超时。
3. [SLAVE_NOT_AVAILABLE](http://slave_not_available/)：这个状态产生的场景和 [FLUSH_SLAVE_TIMEOUT](http://flush_slave_timeout/) 类似，表示在主备方式下，消息发送成功，但是此时 Slave 不可用。如果 Broker服务器的角色是同步 Master，即 [SYNC_MASTER](http://sync_master/)（默认是异步 Master 服务器即 [ASYNC_MASTER](http://%20async_master/)），但没有配置 slave Broker 服务器，则将返回该状态——无 Slave 服务器可用。
4. [SEND_OK](http://send_ok/)：表示发送成功，要注意的是消息发送成功也不意味着它是可靠的，比如「**消息是否已经被存储到磁盘？**」、「**消息是否被同步到 Slave 上？**」、「**消息在 Slave 上是否被写入磁盘？**」要确保不会丢失任何消息，需要结合所配置的「**刷盘策略 SYNC_MASTER | SYNC_FLUSH**」、「**主从策略**」来定。不过这个状态你也可以这么认为，没有发生上面列出的三个问题状态就是 [SEND_OK](http://send_ok/)。

要写一个高质量的生产者端程序，重点在于对发送结果的处理，所以你一定要充分考虑各种异常，以及对应处理逻辑。

## **1.2 消息发送失败处理方式**

生产者端 send 方法本身支持内部重试，重试逻辑如下：
1. 至多重试 2 次。
2. 如果同步模式发送失败，则轮转到下一个 Broker，如果异步模式发送失败，则只会在当前 Broker 进行重试。这个方法的总耗时时间不超过 [sendMsgTimeout](http://sendmsgtimeout/) 设置的值，默认10s。
3. 如果本身向 broker 发送消息产生超时异常，就不会再重试。

以上这三种策略在一定程度上保证了消息可以发送成功。如果业务对「**消息可靠性**」要求比较高，建议业务应用增加相应的重试逻辑：比如调用「**send 同步方法**」发送失败时，则尝试将消息存储到 DB 中，然后由后台线程定时重试，确保消息一定到达Broker。

上述 DB 重试方式为什么没有集成到 MQ 客户端内部做，而是要求业务应用自己去完成，主要基于以下几点考虑：
1. 首先 MQ 客户端设计为「**无状态模式**」，方便任意的水平扩展，且对机器资源的消耗仅仅是「**cpu**」、「**内存**」、「**网络**」。
2. 其次如果 MQ 客户端内部集成一个 KV 存储模块，那么数据只有「**同步落盘**」才能较可靠，而同步落盘本身性能开销较大，所以通常会采用「**异步落盘**」，由于业务应用关闭过程不受 MQ 运维人员控制，可能经常会发生 [kill -9](http://kill%20-9/) 这样暴力方式关闭，造成数据没有及时落盘而丢失。
3. Producer 所在机器的可靠性较低，一般为虚拟机，不适合存储重要数据。

综上，建议重试逻辑放在业务应用来进行控制。

## **1.3 如何提升写入性能**

通常发送一条消息出去要经过以下三步：
1. 客户端发送请求到服务端。
2. 服务端接收并处理该请求。
3. 服务端向客户端返回响应请求。

一次消息的发送耗时来说就是这三个步骤的总和。对于提升写入性能主要可以从下面 2 点出发：
1. 一种是一些对速度要求高，但可靠性要求不高的场景。比如：日志收集类场景，可以采用「**Oneway 方式发送**」，该方式「**只发送请求不等待响应**」，即将数据写入客户端的 Socket 缓冲区就返回，不需要等待对方返回结果，用该方式发送消息的耗时可以缩短到「**微秒级**」。
2. 另一种提高发送速度的方法是「**增加 Producer 并发量**」，可以使用多个 Producer 同时发送，我们不用担心多 Producer 同时写会降低消息写磁盘的效率，RocketMQ 内部引入了一个「**并发窗口**」，在窗口内消息可以并发地写入 [DirectMemory](http://directmemory/) 中，然后异步地将连续一段无空洞的数据刷入日志文件系统中。「**顺序写**」[CommitLog](http://commitlog/) 可让RocketMQ 无论在 HDD 还是 SSD 磁盘情况下都能保持较高的写入性能。

目前在阿里内部经过调优的服务器上，写入性能达到「**90 万+ TPS**」，我们可以参考这个数据来进行系统优化。在 Linux 操作系统层级进行调优，推荐使用 [EXT4](http://ext4/) 文件系统，I/O 调度算法使用 [deadline](http://deadline/) 算法。

# **02 消息消费**

这里我们简单来总结一下消息消费的几个要点：
1. 消息消费方式「**Pull**」、「**Push**」。
2. 消息消费的模式「**广播模式**」、「**集群模式**」。
3. 结合 sentinel 来进行流量控制，后面单独讲。
4. 并发线程数设置。
5. 消息的过滤。

## **2.1 消费幂等**

RocketMQ 无法避免消息重复「**Exactly-Once**」，如果业务对消费重复非常敏感，必须要在业务层面进行幂等处理。此时你可以借助关系数据库进行去重。

首先需要确定消息的唯一键，可以是 msgId，也可以是消息内容中的唯一标识字段，例如订单 Id 等。在消费之前判断唯一键是否在关系数据库中存在。如果不存在则插入并消费，否则跳过。（实际过程要考虑原子性问题，判断是否存在可以尝试插入，如果报主键冲突，则插入失败，直接跳过）

msgId 一定是全局唯一标识符，但是实际使用中可能会存在相同的消息有两个不同 msgId 的情况「**消费者主动重发**」、「**因客户端重投机制导致的重复**」等，这些情况就需要使业务字段进行重复消费。

## **2.2 如何提升消费性能**

当 Consumer 端处理速度跟不上消息的生产速度时，会造成越来越多的消息积压，此时首先要查看消费逻辑本身有没有优化空间，除此之外还有以下三种方法可以提高 Consumer 端的处理能力。

### **2.2.1 提高消费并行度**

绝大部分消息消费行为都属于 I/O 密集型，即可能是操作数据库，或者调用 RPC，这类消费行为的消费速度在于后端数据库或者外系统的吞吐量，通过增加消费并行度，可以提高总的消费吞吐量，但是并行度增加到一定程度，反而会下降。所以，应用必须要设置合理的并行度。有以下两种修改消费并行度的方法：
1. 同一个 ConsumerGroup 下「**集群模式**」，可以通过增加 Consumer 实例数量来提高并行度。通过加机器，或者在已有机器中启动多个 Consumer 进程都可以增加 Consumer 实例数。（需要注意的是总的 Consumer 数量不要超过 Topic 下 [Read Queue](http://read%20queue/) 数量，超过的订阅队列数的 Consumer 实例接收不到消息）。
2. 通过提高单个 Consumer 实例中的消费并行线程数，通过修改参数 [consumeThreadMin](http://consumethreadmin/) 和 [consumeThreadMax](http://consumethreadmax/) 来实现。

### **2.2.2 批量消费**

某些业务场景流程下如果支持批量方式消费，则可以很大程度上提高消费吞吐量，例如订单扣款类应用，一次处理一个订单耗时 1 s，一次处理 10 个订单可能也只耗时 2 s，这样即可大幅度提高消费的吞吐量。

可以通过「**批量消费**」来提高消费的吞吐量，实现方法是设置 Consumer 的 [consumeMessageBatchMaxSize](http://consumemessagebatchmaxsize/) 这个参数，默认是 1，即一次只消费一条消息，例如设置为 N，在消息多的时候每次收到的是个长度为 N 的消息链表。

### **2.2.3 跳过非重要消息**

Consumer 在消费的过程中，如果发现由于某种原因「**消费速度一直追不上发送速度**」发生严重的消息堆积，短时间无法消除堆积时，如果业务对数据要求不高的话，可以「**选择丢弃不重要的消息**」。例如，当某个队列的消息数堆积到 100000 条以上，则尝试丢弃部分或全部消息，这样就可以使 Consumer 快速追上发送消息的速度。示例代码如下：
```java
public ConsumeConcurrentlyStatus consumeMessage(  
        List<MessageExt> msgs,  
        ConsumeConcurrentlyContext context) {  
    long offset = msgs.get(0).getQueueOffset();  
    String maxOffset = msgs.get(0).getProperty(Message.PROPERTY_MAX_OFFSET);  
    long diff = Long.parseLong(maxOffset) - offset;  
    if (diff > 100000) {  
        // TODO 消息堆积情况的特殊处理  
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;  
    }  
    // TODO 正常消费过程  
    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;  
}
```

### **2.2.4 优化每条消息消费过程**

举例如下，某条消息的消费过程如下：
1. 根据消息从 DB 查询【数据 1】。
2. 根据消息从 DB 查询【数据 2】。
3. 进行复杂的业务计算。
4. 向 DB 插入【数据 3】。
5. 向 DB 插入【数据 4】。

这条消息的消费过程中有 4 次与 DB 的交互，如果按照每次 5 ms 计算，那么总共耗时 20 ms，假设业务计算耗时 5 ms，那么总过耗时 25 ms，所以如果能把 4 次 DB 交互优化为 2 次，那么总耗时就可以优化到 15 ms，即总体性能提高了 40 %。所以应用如果对时延敏感的话，可以把 DB 部署在 SSD 硬盘，相比于 SCSI 磁盘，前者的 RT 会小很多。

## **2.3 消费打印日志**

如果消息量较少，建议在消费入口方法打印消息，消费耗时等，方便后续排查问题。
```java
public ConsumeConcurrentlyStatus consumeMessage(  
        List<MessageExt> msgs,  
        ConsumeConcurrentlyContext context)  
{  
    log.info("RECEIVE_MSG_BEGIN: " + msgs.toString());  
    // TODO 正常消费过程  
    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;  
}
```

如果能打印每条消息消费耗时，那么在排查消费慢等线上问题时，会更方便。

## **2.4 其他消费建议**

### **2.4.1 关于消费者和订阅**

​第一件需要注意的事情是：不同的消费者组可以独立的消费一些 Topic，并且每个消费者组都有自己的消费偏移量，请确保同一组内的每个消费者订阅信息保持一致。

### **2.4.2 关于有序消息**

消费者将锁定每个消息队列，以确保他们被逐个消费，虽然这将会导致性能下降，但是当你关心消息顺序的时候会很有用。我们不建议抛出异常，你可以返回 [ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT](http://consumeorderlystatus.suspend_current_queue_a_moment/) 作为替代。

### **2.4.3 关于并发消费**

顾名思义，消费者将并发消费这些消息，建议你使用它来获得良好性能，我们不建议抛出异常，你可以返回 [ConsumeConcurrentlyStatus.RECONSUME_LATER](http://consumeconcurrentlystatus.reconsume_later/) 作为替代。

### **2.4.4 关于消费状态**

对于并发的消费监听器，你可以返回 [RECONSUME_LATER](http://reconsume_later/) 来通知消费者现在不能消费这条消息，并且希望可以稍后重新消费它。然后你可以继续消费其他消息。对于有序的消息监听器，因为你关心它的顺序，所以不能跳过消息，但是你可以返回 [SUSPEND_CURRENT_QUEUE_A_MOMENT](http://suspend_current_queue_a_moment/) 告诉消费者等待片刻。

### **2.4.5 关于 Blocking**

不建议阻塞监听器，因为它会阻塞线程池，并最终可能会终止消费进程

### **2.4.6 关于线程数设置**

消费者使用 [ThreadPoolExecutor](http://threadpoolexecutor/) 在内部对消息进行消费，所以你可以通过设置 [setConsumeThreadMin](http://setconsumethreadmin/) 或 [setConsumeThreadMax](http://setconsumethreadmax/) 来改变它。

### **2.4.7 关于消费位点**

当建立一个新的消费者组时，需要决定是否需要消费已经存在于 Broker 中的历史消息:
1. [CONSUME_FROM_LAST_OFFSET](http://consume_from_last_offset/) 将会忽略历史消息，并消费之后生成的任何消息。
2. [CONSUME_FROM_FIRST_OFFSET](http://consume_from_first_offset/) 将会消费每个存在于 Broker 中的信息。你也可以使用 
3. [CONSUME_FROM_TIMESTAMP](http://consume_from_timestamp/) 来消费在指定时间戳后产生的消息。

# **03 消息存储**

## **3.1 消息存储**

目前的高性能磁盘，顺序写速度可以达到 600 MB/s， 超过了一般网卡的传输速度。但是磁盘随机写的速度只有大概100KB/s，和顺序写的性能相差 6000 倍！

因为有如此巨大的速度差别，好的消息队列系统会比普通的消息队列系统速度快多个数量级。RocketMQ 的消息使用「**顺序写**」，保证了消息存储的速度。

## **3.2 存储结构**

RocketMQ 消息存储是由 [ConsumeQueue](http://consumequeue/) 和 [CommitLog](http://commitlog/) 配合完成的，消息真正的物理存储文件是 [CommitLog](http://commitlog/)，而 [ConsumeQueue](http://consumequeue/) 是消息的逻辑队列，类似数据库的索引文件，存储的是指向物理存储的地址。每个 Topic 下的每个 [MessageQueue](http://messagequeue/) 都有一个对应的 [ConsumeQueue](http://consumequeue/) 文件，如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271557944.png)

上面消息存储架构图中主要有下面三个跟消息存储相关的文件构成：
1. **CommitLog**：消息主体以及元数据的存储主体，存储 Producer 端写入的消息主体内容,消息内容不是定长的。单个文件大小默认 1G ，文件名长度为 20 位，左边补零，剩余为起始偏移量，比如00000000000000000000 代表了第一个文件，起始偏移量为 0，文件大小为 1G=1073741824；当第一个文件写满了，第二个文件为 00000000001073741824，起始偏移量为 1073741824，以此类推。消息主要是顺序写入日志文件，当文件满了，写入下一个文件。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271620589.png)
2. **ConsumeQueue**：消息消费队列，引入的目的主要是提高消息消费的性能，RocketMQ 是基于主题 Topic 的订阅模式，消息消费是针对主题进行。如果要遍历 [commitLog](http://commitlog/) 文件根据 Topic 检索消息是非常低效。 Consumer 可以根据 [ConsumeQueue](http://consumequeue/) 来查找待消费的消息。其中，[ConsumeQueue](http://consumequeue/)（逻辑消费队列）作为消费消息的索引：
	1. 保存了指定Topic下的队列消息在 [CommitLog](http://commitlog/) 中的起始物理偏移量 offset。
	2. 消息大小 size
	3. 消息 Tag 的 HashCode 值。
   
   [ConsumeQueue](http://consumequeue/) 文件可以看成是基于 Topic 的 [CommitLog](http://commitlog/) 索引文件，所以 [ConsumeQueue](http://consumequeue/) 文件夹的组织方式如下：[topic/queue/file](http://topic/queue/file) 三层组织结构。具体存储路径为：[$HOME/store/consumequeue/{topic}/{queueId}/{fileName}](http://$home/store/consumequeue/%7Btopic%7D/%7BqueueId%7D/%7BfileName%7D)。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271630607.png)
   另外 [ConsumeQueue](http://consumequeue/) 文件采取定长设计，每个条目共 20 个字节，分别为：「**8 字节的 commitlog 物理偏移量**」、「**4 字节的消息长度**」、「**8 字节的 tag hashcode**」。单个文件由 30 W 个条目组成，可以像数组一样随机访问每一个条目，每个 [ConsumeQueue](http://consumequeue/) 文件大小约 5.72M。
3. IndexFile：它提供了一种可以通过 key 或时间区间来查询消息的方法。
	1. Index 文件的存储位置是： [$HOME/store/index/${fileName}](http://$home/store/index/%24%7BfileName%7D)。
	2. 文件名 fileName 是以创建时的时间戳命名的。
	3. 固定的单个 IndexFile 文件大小约为 400M。
	4. 一个 IndexFile 可以保存 2000W 个索引。
	5. IndexFile 的底层存储设计为在文件系统中实现 HashMap 结构，所以 rocketmq 的索引文件其底层实现为 Hash 索引。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271633325.png)

# **04 过滤消息**

RocketMQ 的消息过滤方式有别于其它 MQ 中间件，是在 Consumer 端**订阅消息时再做消息过滤的**。这么做是在于其 Producer 端写入消息和 Consumer 端订阅消息采用「**分离存储机制**」来实现的，Consumer 端订阅消息是需要通过 [ConsumeQueue](http://consumequeue/) 这个消息消费的逻辑队列拿到一个索引，然后再从 [CommitLog](http://commitlog/) 里面读取真正的消息实体内容。

[ConsumeQueue](http://consumequeue/) 的存储结构如下，可以看到其中有 8 个字节存储的 [Message Tag](http://messagetag/) 的哈希值，基于 Tag 的消息过滤正式基于该字段值来实现的。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271635655.png)

主要支持以下 3 种的过滤方式。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271639860.png)

## **4.1 Tag 过滤方式**

Consumer 端在订阅消息时除了指定 Topic 还可以指定 TAG，如果一个消息有多个 TAG，可以用 || 分隔。
1. Consumer 端会将这个订阅请求构建成一个 SubscriptionData，发送一个 Pull 消息的请求给Broker端。
2. Broker 端从 RocketMQ 的文件存储层 Store 读取数据之前，会用这些数据先构建一个 MessageFilter，然后传给 Store。
3. Store 从 ConsumeQueue 读取到一条记录后，会用它记录的消息 tag hash 值去做过滤。
4. 在服务端只是根据 hashcode 进行判断，无法精确对 tag 原始字符串进行过滤，在消息消费端拉取到消息后，还需要对消息的原始 tag 字符串进行比对，如果不同则丢弃该消息，不进行消息消费。

## **4.2 SQL 92 表达式过滤方式**

该方式仅对 push 的消费者起作用。Tag 方式虽然效率高，但是支持的过滤逻辑比较简单。而 SQL 表达式可以更加灵活的支持复杂过滤逻辑，这种方式的大致做法和上面的 Tag 过滤方式一样，只是在 Store 层的具体过滤过程不太一样。

真正的 [SQL expression](http://sql%20expression/) 的构建和执行由 [rocketmq-filter](http://rocketmq-filter/) 模块来负责的。每次过滤都去执行 SQL 表达式会影响效率，所以 RocketMQ 使用了 [BloomFilter](http://bloomfilter/) 避免了每次都去执行。

SQL 92 的表达式上下文为消息的属性。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271710900.png)

配置文件：[conf/broker.conf](http://conf/broker.conf)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271711401.png)

```shell
# 没有重新启动 直接查看配置
./mqbroker -p | grep enablePropertyFilter
  

# 没有重新启动 根据配置文件查看配置
./mqbroker -c ../conf/broker.conf -p | grep enablePropertyFilter
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271712908.png)

首先需要开启支持 SQL92 的特性，需要重启 Broker：
```shell
./mqbroker -n localhost:9876 -c /home/wangjianghua/src/rocketmq/rocketmq-all-4.9.4-bin-release/conf/broker.conf
```  

RocketMQ 仅定义了几种基本的语法，用户可以扩展：
1. 数字比较： >, >=, <, <=, BETWEEN, =。
2. 字符串比较： =, <>, IN; IS NULL或者IS NOT NULL。
3. 逻辑比较： AND, OR, NOT。
4. Constant types are: 数字如：123, 3.1415; 字符串如：'abc'，必须是单引号引起来 NULL，特殊常量布尔型如：TRUE or FALSE。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271721385.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271721842.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271723302.png)

## **4.3 Filter Server 过滤方式**

这是一种比 SQL 表达式更灵活的过滤方式，允许用户自定义 Java 函数，根据 Java 函数的逻辑对消息进行过滤。要使用 [Filter Server](http://filter%20server/)，首先要在启动 Broker 前在配置文件里加上 [filterServer-Nums=3](http://filterserver-nums=3/) 这样的配置，Broker 在启动的时候，就会在本机启动 3 个 [Filter Server](http://filter%20server/) 进程。

[Filter Server](http://filter%20server/) 类似一个 RocketMQ 的 Consumer 进程，它从本机 Broker 获取消息，然后根据用户上传过来的Java 函数进行过滤，过滤后的消息再传给远端的 Consumer。

这种方式会占用很多 Broker 机器的CPU资源，要根据实际情况谨慎使用。上传的 java 代码也要经过检查，不能有申请大内存、创建线程等这样的操作，否则容易造成 Broker 服务器宕机。

# **05 零拷贝原理**

零拷贝是一种优化技术，旨在减少数据在内存和设备之间的复制操作，以提高数据传输效率。在传统的数据传输过程中，数据通常需要在多个缓冲区之间复制，这样不仅增加了CPU负担，还可能导致性能瓶颈。通过使用零拷贝，数据可以直接从源（如磁盘）传输到目标（如网络），而不需要经过中间的缓冲区，从而减少了CPU的使用和内存复制的开销。

## **5.1 PageCache**

什么是 PageCache 呢？ 

**PageCache** 是一种内存管理机制，用于提高文件系统性能。它通过将磁盘上的数据块（block）缓存到内存中，减少对磁盘的读取和写入操作，从而加快文件访问速度。

**PageCache** 位于操作系统的内核态。

**PageCache** 是由内存中的物理 page 组成，其内容对应磁盘上的 block。PageCache 的大小是动态变化的。

Backing Store: cache 缓存的存储设备，一个 Page 通常包含多个 block, 而 block 不一定是连续的。

**关键概念**：
1. **物理页面（Physical Page）**：PageCache 是由内存中的物理页面组成。物理页面是操作系统内存管理中的基本单位，通常大小为4KB或8KB。
2. **块（Block）**：磁盘上的数据单位，通常大小为512字节或4KB。一个物理页面通常可以存储多个磁盘块。
3. **动态大小**： PageCache 的大小是动态变化的。操作系统会根据当前的内存使用情况和I/O需求来调整PageCache的大小。当系统内存充足时，PageCache可以占用更多内存；当系统需要释放内存时，PageCache的大小会被压缩。
4. **Backing Store**：Backing Store 指的是用于缓存的存储设备，例如硬盘、SSD等。虽然PageCache存储在内存中，但它的内容来自Backing Store。
5. **缓存策略**： PageCache 使用各种缓存策略（如LRU、FIFO等）来决定哪些页面保留在内存中，哪些页面被替换。常用的策略是最近最少使用（LRU），即优先替换最久未使用的页面。

**作用：**
- **提高性能**：通过减少磁盘I/O操作，PageCache可以显著提高文件访问速度。
- **减少延迟**：由于大部分读取请求可以在内存中满足，因此应用程序的响应时间会减少。
- **降低硬盘磨损**：减少对硬盘的写入和读取次数，可以延长硬盘的使用寿命。

### **5.1.1 读 Cache**

当内核发起一个读请求时, 先会检查请求的数据是否缓存到了 PageCache 中。如果有，那么直接从内存中读取，不需要访问磁盘, 即「**缓存命中**」。如果没有, 就必须从磁盘中读取数据，然后内核将读取的数据再缓存到 cache 中, 如此后续的读请求就可以命中缓存了。Page 可以只缓存一个文件的部分内容，而不需要把整个文件都缓存进来。

### **5.1.2 写 Cache**

当内核发起一个写请求时, 也是直接往 cache 中写入，后备存储中的内容不会直接更新。内核会将被写入的 Page 标记为 dirty，并将其加入到 dirty list 中。内核会周期性地将 dirty list 中的 Page 写回到磁盘上，从而使磁盘上的数据和内存中缓存的数据一致。

### **5.1.3 Cache 回收**

PageCache 的另一个重要工作是释放 Page，从而释放内存空间。Cache 回收的任务是选择合适的 Page 释放,如果 Page 是 dirty 的，需要将 Page 写回到磁盘中再释放。

## **5.2 Cache & Buffer 区别**

### **5.2.1 Cache**

它是缓存区，高速缓存，位于「**CPU**」和「**主内存**」之间的「**容量较小**」但「**速度很快**」的存储器，因为 CPU 的速度远远高于主内存的速度，CPU 从内存中读取数据需等待很长的时间，而 Cache 保存着 CPU 刚用过的数据或循环使用的部分数据，此时从 Cache 中读取数据会更快，减少了 CPU 等待的时间，提高了系统的性能。

但 Cache 并不是缓存文件的，而是「**缓存块**」的(块是 I/O 读写最小的单元)，Cache 一般会用在 I/O 请求上，如果多个进程要访问某个文件，可以把此文件读入 Cache 中，这样下一个进程获取 CPU 控制权并访问此文件直接从 Cache读取，提高系统性能。

### **5.2.2 Buffer**

它是缓冲区，用于存储「**速度不同步的设备**」或「**优先级不同的设备**」之间传输数据。通过 buffer 可以减少进程间通信需要等待的时间，当存储速度快的设备与存储速度慢的设备进行通信时，存储慢的数据先把数据存放到 buffer，达到一定程度存储快的设备再读取 buffer 的数据，在此期间存储快的设备 CPU 可以干其他的事情。

Buffer 一般是用在写入磁盘的，例如：某个进程要求多个字段被读入，当所有要求的字段被读入，之前已经读入的字段会先放到 buffer 中。

### **5.2.3、总结**

**ChatGPT**

1. 定义
	- **Cache**：
    
	    - Cache 是一种高速存储区，用于存储最近使用的数据或指令，以减少访问延迟和提高系统性能。
	    - Cache 通常位于CPU与主内存之间，设计用来快速响应CPU对数据的访问请求。
	- **Buffer**：
    
	    - Buffer 是一个内存区域，用于临时存储在输入和输出操作之间的数据，以应对数据流速不匹配的问题。
	    - Buffer 的作用是平滑数据传输，确保数据可以在不同设备或程序之间有效地移动。
2. 目的
	- **Cache**：
	    - 提高数据访问速度，减少访问延迟。
	    - 存储重复使用的数据，以减少访问主内存的频率。
	- **Buffer**：
	    - 处理数据流速的不匹配，确保数据在生产者和消费者之间顺利传输。
	    - 临时存储数据，以提高数据传输的效率。
3. 工作原理
	- **Cache**：
	    - 根据一定的算法（如LRU、FIFO等）管理存储的数据。
	    - 当CPU请求数据时，首先检查Cache是否命中（即数据是否在Cache中），如果命中则直接使用，否则从主内存加载并更新Cache。
	- **Buffer**：
    
	    - 通常使用先进先出（FIFO）策略处理数据。
	    - 当数据被写入Buffer时，它会被暂时存储，待条件满足（如I/O设备准备好）后再进行处理。
4. 使用场景
	- **Cache**：
	    - 主要用于CPU和内存之间的高速数据访问，常见于CPU Cache（L1、L2、L3缓存）。
	    - 也可以用于Web缓存、数据库缓存等。
	- **Buffer**：
	    - 常用于I/O操作，如文件读写、网络数据传输等。
	    - 例如，操作系统在读写磁盘时使用Buffer，以避免CPU与磁盘之间的速度不匹配问题。
5. 数据一致性
	- **Cache**：
	    - 可能存在数据一致性问题，因为Cache中的数据与主内存之间可能不一致，需要通过Cache一致性协议来管理。
	- **Buffer**：
	    - 通常不涉及数据一致性问题，因为Buffer只是暂时存储数据，数据在处理后将会被传送到目标位置。

## **5.3 HeapByteBuffer & DirectByteBuffer**

[HeapByteBuffer](http://heapbytebuffer/) 是在 JVM 堆上面一个 buffer，底层的本质是一个数组，用类封装维护了很多的索引（[limit/position/capacity](http://limit/position/capacity) 等）。

而 [DirectByteBuffer](http://directbytebuffer/) 底层的数据是维护在操作系统的内存中，而不是 JVM 里，[DirectByteBuffer](http://directbytebuffer/) 里维护了一个引用 address 指向数据，进而操作数据。

它们的优点如下：
1. **HeapByteBuffer**：内容维护在 JVM 里，把内容写进 buffer 里速度快，更容易进行回收。
2. **DirectByteBuffer**：跟外设 I/O 设备打交道时会快很多，因为外设读取JVM堆里的数据时，不是直接读取的，而是把 JVM 里的数据读到一个内存块里，再在这个块里读取的，如果使用 [DirectByteBuffer](http://directbytebuffer/)，则可以省去这一步，实现 zero copy（零拷贝）机制。

外设 I/O 设备之所以要把 JVM 堆里的数据 copy 出来再操作，不是因为操作系统不能直接操作 JVM 内存，而是

因为 JVM 在进行 GC（垃圾回收）时，会对数据进行移动，一旦出现这种问题，外设 I/O 设备就会出现数据错乱的情况。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271835992.png)

所有的通过 allocate() 方法创建的 buffer 都是 HeapByteBuffer。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271837991.png)

使用堆外内存来实现零拷贝：
1. 前者分配在 JVM 堆上（[ByteBuffer.allocate()](http://bytebuffer.allocate()/)），后者分配在操作系统物理内存上（[ByteBuffer.allocateDirect()](http://bytebuffer.allocatedirect()/)，JVM 使用 C 库中的 [malloc()](http://malloc()/) 方法分配堆外内存）。
2. [DirectByteBuffer](http://directbytebuffer/) 可以减少 [JVM GC](http://jvm%20gc/) 压力，当然堆中依然保存对象引用，fullgc 发生时也会回收直接内存，也可以通过 [system.gc](http://system.gc/) 主动通知 JVM 回收，或者通过 [cleaner.clean](http://cleaner.clean/) 主动清理。[Cleaner.create()](http://cleaner.create()/) 方法需要传入一个 DirectByteBuffer 对象和一个 Deallocator（一个堆外内存回收线程）。GC 发生时发现堆中的DirectByteBuffer 对象没有强引用了，则调用 [Deallocator#run()](http://deallocator/#run()) 方法回收直接内存，并释放堆中DirectByteBuffer 的对象引用。
3. 底层 I/O 操作需要连续的内存（JVM 堆内存容易发生 GC 和对象移动），所以在执行 write 操作时需要将HeapByteBuffer 数据拷贝到一个临时的操作系统用户态内存空间中，会多一次额外拷贝。而DirectByteBuffer 则可以省去这个拷贝动作，这是 Java 层面的 「**零拷贝**」技术，在 netty 中广泛使用。
4. MappedByteBuffer 底层使用了操作系统的 mmap 机制，[FileChannel#map()](http://filechannel/#map()) 方法就会返回[MappedByteBuffer](http://mappedbytebuffer/)。[DirectByteBuffer](http://directbytebuffer/) 虽然实现了 [MappedByteBuffer](http://mappedbytebuffer/)，不过 [DirectByteBuffer](http://directbytebuffer/) 默认并没有直接使用 mmap 机制。

## **5.4 缓存 I/O & 直接 I/O**

### **5.4.1 缓存 I/O**

大多数文件系统的默认 I/O 操作都是缓存 I/O。在 Linux 的缓存 I/O 机制中，数据先从「**磁盘**」复制到「**内核空间**」的缓冲区，然后从内核空间缓冲区复制到应用程序的地址空间。

#### **读操作**

操作系统检查内核的缓冲区有没有需要的数据，如果已经缓存了那么就直接从缓存中返回，否则从磁盘中读取，然后缓存在操作系统的缓存中。

#### **写操作**

将数据从「**用户空间**」复制到「**内核空间**」的缓存中。这时对用户程序来说写操作就已经完成，至于什么时候再写到磁盘中由「**操作系统**」决定，除非显示地调用了 sync 同步命令。

#### **缓存 I/O 优点**

1. 在一定程度上分离了内核空间和用户空间，保护系统本身的运行安全。
2. 可以减少读盘的次数，从而提高性能。

#### **缓存 I/O 缺点**

1. 在缓存 I/O 机制中，DMA 方式可以将数据直接从磁盘读到页缓存中，或者将数据从页缓存直接写回到磁盘上，而不能直接在应用程序地址空间和磁盘之间进行数据传输。数据在传输过程中就需要在应用程序地址空间（用户空间）和缓存（内核空间）之间进行多次数据拷贝操作，这些数据拷贝操作所带来的 CPU 以及内存开销是非常大的。

### **5.4.2 直接 I/O**

直接 I/O 就是应用程序直接访问磁盘数据，而不经过「**内核缓冲区**」，这样做的目的是减少一次从「**内核缓冲区**」到「**用户程序缓存**」的数据复制。比如说数据库管理系统这类应用，它们更倾向于选择它们自己的缓存机制，因为数据库管理系统往往比操作系统更了解数据库中存放的数据，数据库管理系统可以提供一种更加有效的缓存机制来提高数据库中数据的存取性能。

当然它也有缺点，如果访问的数据不在应用程序缓存中，那么每次数据都会直接从磁盘加载，这种直接加载会非常缓慢。通常直接 I/O 与异步 I/O 结合使用，会得到比较好的性能。

下图分析了写场景下的 DirectIO 和 BufferIO：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271903957.png)

## **5.5 内存映射文件（Mmap）**

在 LINUX 中我们可以使用 mmap 用来在进程虚拟内存地址空间中分配地址空间，创建和物理内存的映射关系。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271906827.png)

**映射关系**
1. 私有文件映射：多个进程使用同样的物理内存页进行初始化，但是各个进程对内存文件的修改不会共享，也不会反应到物理文件中。
2. 私有匿名映射：mmap会创建一个新的映射，各个进程不共享，这种使用主要用于分配内存 (malloc分配大内存会调用mmap)。 例如开辟新进程时，会为每个进程分配虚拟的地址空间，这些虚拟地址映射的物理内存空间各个进程间读的时候共享，写的时候会 [copy-on-write](http://copy-on-write/)。
3. 共享文件映射：多个进程通过虚拟内存技术共享同样的物理内存空间，对内存文件 的修改会反应到实际物理文件中，他也是进程间通信(IPC)的一种机制。
4. 共享匿名映射 这种机制在进行fork的时候不会采用写时复制，父子进程完全共享同样的物理内存页，这也就实现了父子进程通信(IPC)。

mmap 只是在虚拟内存分配了地址空间，只有在第一次访问虚拟内存的时候才分配物理内存。在 mmap 之后，并没有在将文件内容加载到物理页上，只上在虚拟内存中分配了地址空间。当进程在访问这段地址时，通过「**查找页表**」，发现「**虚拟内存**」对应的页没有在「**物理内存**」中缓存，则产生「**缺页**」，由内核的缺页异常处理程序处理，将文件对应内容，以页为单位 4096 加载到物理内存，注意是只加载缺页，但也会受操作系统一些调度策略影响，加载的比所需的多。

关于零拷贝相关的可以查看： [原来 8 张图，就可以搞懂「零拷贝」了](https://mp.weixin.qq.com/s/P0IP6c_qFhuebwdwD8HM7w)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271910881.png)

## **5.6 零拷贝小结**
1. 虽然叫零拷贝，但实际上 sendfile 还是有 2 次数据拷贝的。第 1 次是从磁盘拷贝到内核缓冲区，第二次是从内核缓冲区拷贝到网卡。如果网卡支持 [SG-DMA](http://sg-dma/)（The Scatter-Gather Direct Memory Access）技术，就无需从「**PageCache**」拷贝至 Socket 缓冲区。
2. 之所以叫零拷贝，是从内存角度来看的，数据在内存中没有发生过拷贝，只是在内存和 I/O 设备之间传输。很多时候我们认为 sendfile 才是零拷贝，mmap 严格来说不算。
3. Linux 中的 API 为 sendfile、mmap，Java 中的 API 为 [FileChanel.transferTo()](http://filechanel.transferto()/)、[FileChannel.map()](http://filechannel.map()/) 等。
4. Netty、Kafka(sendfile)、Rocketmq（mmap）、Nginx 等高性能中间件中，都有大量利用操作系统零拷贝特性。

# **06 同步复制和异步复制**

如果一个 Broker 有 Master 和 Slave ，消息需要从 Master 复制到 Slave 上，有「**同步**」和「**异步**」两种复制方式。

## **6.1 同步复制**

同步复制方式是等 Master 和 Slave 都写入成功后才返回给客户端写成功状态。

在同步复制方式下，如果 Master 出故障，Slave 上有全部的备份数据容易恢复，但是同步复制会增大数据写入延迟，降低系统吞吐量。

## **6.2 异步复制**

异步复制方式是只要 Master 写成功即可返回给客户端写成功状态。

在异步复制方式下，系统拥有较低的延迟和较高的吞吐量，但是如果 Master 出了故障，有些数据因为没有被写入Slave，有可能会丢失。

## **6.3 配置**

「**同步复制**」和「**异步复制**」是通过 Broker 配置文件里的 brokerRole 参数进行设置的，这个参数可以被设置

成 [ASYNC_MASTER](http://async_master/)、 [SYNC_MASTER](http://sync_master/)、[SLAVE](http://slave/) 三个值中的一个。

`/home/wangjianghua/src/rocketmq/rocketmq-all-4.9.4-bin-release/conf/broker.conf` 文件：Broker的配置文件
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271913519.png)

## **6.4 总结**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271913137.png)
实际应用中要结合业务场景，合理设置「**刷盘方式**」和「**主从复制**」方式， 尤其是 [SYNC_FLUSH](http://sync_flush/) 方式，由于

频繁地触发磁盘写动作，会明显降低性能。通常情况下，应该把 Master 和 Save 配置成 [ASYNC_FLUSH](http://async_flush/) 的

刷盘方式，主从之间配置成 [SYNC_MASTER](http://sync_master/) 的复制方式，这样即使有一台机器出故障，仍然能保证数据不丢，是个不错的选择。

# **07 高可用机制**

RocketMQ 分布式集群是通过 Master 和 Slave 的配合达到高可用性的。

Master 和 Slave 的区别：
1. 在 Broker 的配置文件中，参数 brokerId 的值为 0 表明这个 Broker 是 Master，大于 0 表明这个 Broker 是 Slave。
2. brokerRole 参数用来表示该 Broker 是 Master 还是 Slave，参数值：[SYNC_MASTER](http://sync_master/)、[ASYNC_MASTER](http://async_master/)、[SALVE](http://salve/)。
3. Master 角色的 Broker 支持读和写，Slave 角色的 Broker 仅支持读。
4. Consumer 可以连接 Master 角色的 Broker，也可以连接 Slave 角色的 Broker 来读取消息。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271914639.png)

## **7.1 消息消费高可用**

在 Consumer 的配置文件中，并不需要设置是从 Master 读还是从 Slave 读，当 Master 不可用或者繁忙的时候，Consumer 会被自动切换到从 Slave 读。有了自动切换 Consumer 这种机制，当一个 Master 角色的机器出现故障后，Consumer 仍然可以从 Slave 读取消息，不影响 Consumer 应用程序。这就达到了消费端的高可用性。

## **7.2 消息发送高可用**

那么如何达到发送端的高可用性呢？

在创建 Topic 的时候，把 Topic 的多个 [MessageQueue](http://messagequeue/) 创建在多个 Broker 组上「**相同 Broker 名称，不同 brokerId 的机器组成一个 Broker 组**」，这样既可以在「**性能方面**」具有扩展性，也可以「**降低主节点故障**」

对整体上带来的影响，而且当一个 Broker 组的 Master 不可用后，其他组的 Master 仍然可用，Producer仍然可以发送消息的。

RocketMQ 目前还不支持把 Slave 自动转成 Master，如果机器资源不足，需要把 Slave 转成 Master。
1. 手动停止 Slave 角色的 Broker。
2. 更改配置文件。
3. 用新的配置文件启动 Broker。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271916128.png)

上面这种早期方式在大多数场景下都可以很好的工作，但也面临一些问题。

比如，在需要保证「**消息严格顺序**」的场景下，由于在 Topic 层面无法保证严格顺序，所以必须「**指定队列**」

来发送消息，对于任何一个队列，它一定是落在一组「**特定主从节点**」上，如果这个主节点宕机，其他的主节点是无法替代这个主节点的，否则就无法保证严格顺序。

**在这种复制模式下，严格顺序和高可用只能选择一个，鱼和熊掌不能兼得**。

因此 RocketMQ 在 2018 年底迎来了一次重大的更新，引入「**Dledger**」，增加了一种全新的复制方式，可以很好地解决这个问题。

「**Dledger**」 在写入消息的时候，要求「**至少消息复制到半数以上的节点**」之后，才给客户端返回写入成功，并且它是支持通过「**选举**」来「**动态切换**」主节点的。

假如现在有 3 个节点，当主节点宕机的时候，2 个从节点会通过投票选出一个新的主节点来继续提供服务，相比主从的复制模式，解决了可用性的问题。

由于消息要至少复制到 2 个节点上才会返回写入成功，即使主节点宕机了，也至少有一个节点上的消息是和主节点一样的。

「**Dledger**」 在选举时，总会把数据和主节点一样的从节点选为新的主节点，这样就保证了数据的一致性，既不会丢消息，还可以保证严格顺序。

当然，「**Dledger**」 的复制方式也不是完美的，依然存在一些不足：
1. 选举过程中不能提供服务。
2. 最少需要 3 个节点才能保证数据一致性，3 节点时，只能保证 1 个节点宕机时可用，如果 2 个节点同时宕机，即使还有 1 个节点存活也无法提供服务，资源的利用率比较低。
3. 除此之外，由于至少要复制到半数以上的节点才返回写入成功，性能上也不如主从异步复制的方式快。

# **08 刷盘机制**

RocketMQ 的所有消息都是需要「**持久化**」到磁盘上，以保证断电后消息不会丢失。同时这样才可以**让存储的消息量可以超出内存的限制**。首先写入系统「**PageCache**」，然后再进行「**刷盘**」，可以保证内存与磁盘都有一份数据。 在访问时直接从内存读取。

消息在通过 Producer 端写入 RocketMQ 的时候，为了提高性能，会尽量保证磁盘的顺序写。消息在写入磁盘时，有两种写磁盘的方式：「**同步刷盘**」和「**异步刷盘**」。
1. **同步刷盘**：在返回写成功状态时，消息已经被写入磁盘。
	1. 具体流程是，消息写入内存的「**PageCache**」后，立刻通知刷盘线程刷盘，然后等待刷盘完成，刷盘线程执行完成后唤醒等待的线程，返回消息写成功的状态。
	2. **安全，效率不高**
2. **异步刷盘**：在返回写成功状态时，消息可能只是被写入了内存的「**PageCache**」，写操作的返回快，吞吐量大；当内存里的消息量积累到一定程度时，统一触发写磁盘动作，快速写入。
	1. **效率高，但是可能丢消息**

刷盘方式是通过 Broker 配置文件里的 flushDiskType 参数设置的，这个参数配置成 [SYNC_FLUSH](http://sync_flush/) (同步刷盘)或者[ASYNC_FLUSH](http://async_flush/) (异步刷盘)。

## **8.1 同步刷盘**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271923497.png)

同步刷盘与异步刷盘的唯一区别是异步刷盘写完「**PageCache**」直接返回，而同步刷盘需要等待刷盘完成才返回， 同步刷盘流程如下：
1. 写入「**PageCache**」 后，线程等待，通知刷盘线程刷盘。
2. 刷盘线程刷盘后，唤醒前端等待线程，可能是一批线程。
3. 等待线程向用户返回成功。

## **8.2 异步刷盘**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271924536.png)

在有 RAID 卡，SAS 15000 转磁盘测试顺序写文件，速度可以达到 300M 每秒左右，而线上的网卡一般都为千兆网卡，写磁盘速度明显快于数据网络入口速度，那么是否可以做到写完内存就向用户返回，由后台线程刷盘呢？
1. 由于磁盘速度大于网卡速度，那么刷盘的进度肯定可以跟上消息的写入速度。

2. 由于此时系统压力过大，可能堆积消息，除了写 I/O，还有读 I/O，万一出现磁盘读取落后情况， 会不会导致系统内存溢出，答案是否定的，原因如下：
	1. 写入消息到「**PageCache**」时，如果内存不足，则尝试丢弃干净的 PAGE，腾出内存供新消息使用，策略是 LRU 方式。
	2. 如果干净页不足，此时写入「**PageCache**」会被阻塞，系统尝试刷盘部分数据，大约每次尝试 32 个 Page, 来找出更多干净 Page。

综上，内存溢出的情况不会出现。

# **09 负载均衡**

RocketMQ 中的「**负载均衡**」都在 Client 端完成，具体来说的话主要可以分为「**Producer 端发送消息的负载均衡**」和 「**Consumer 端订阅消息的负载均衡**」。

## **9.1 Producer 端负载均衡**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271935453.png)

如上图，Producer 端，每个实例在发消息的时候，默认会轮询所有的 messagequeue 发送，以达到让消息平均落在不同的 queue 上。而由于 queue 可以散落在不同的 broker，所以消息就发送到不同的 broker 下。

图中箭头线条上的标号代表顺序，发布方会把第一条消息发送至 [Queue 0](http://queue%200/)，然后第二条消息发送至 [Queue 1](http://queue%201/)，以此类推。

## **9.2 Consumer 端负载均衡**

### **9.2.1 集群模式**

在集群消费模式下，每条消息只需要投递到订阅这个 topic 的 Consumer Group 下的一个实例即可。RocketMQ采用主动拉取的方式拉取并消费消息，在拉取的时候需要明确指定拉取哪一条messagequeue。

而每当实例的数量有变更，都会触发一次所有实例的负载均衡，这时候会按照 queue 的数量和实例的数量平均分配queue 给每个实例。

默认的分配算法是 [AllocateMessageQueueAveragely](http://allocatemessagequeueaveragely/)。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271937893.png)

如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271937392.png)

还有另外一种平均的算法是 [AllocateMessageQueueAveragelyByCircle](http://allocatemessagequeueaveragelybycircle/)，也是平均分摊每一条 queue，只是以环状轮流分queue 的形式。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271937314.png)

如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271938429.png)

需要注意的是，集群模式下，queue 都是只允许分配只一个实例，这是由于如果多个实例同时消费一个 queue 的消息，由于拉取哪些消息是 consumer 主动控制的，那样会导致同一个消息在不同的实例下被消费多次，所以算法上都是一个 queue 只分给一个 consumer 实例，一个 consumer 实例可以允许同时分到不同的 queue。

通过增加 consumer 实例去分摊 queue 的消费，可以起到水平扩展的消费能力的作用。而有实例下线的时候，会重新触发负载均衡，这时候原来分配到的 queue 将分配到其他实例上继续消费。

但是如果 consumer 实例的数量比 message queue 的总数量还多的话，多出来的 consumer 实例将无法分到queue，也就无法消费到消息，也就无法起到分摊负载的作用了。所以需要控制让 queue 的总数量大于等于consumer 的数量。

### **9.2.2 广播模式**

由于广播模式下要求一条消息需要投递到一个消费组下面所有的消费者实例，所以也就没有消息被分摊消费的说法。

在实现上，其中一个不同就是在 consumer 分配 queue 的时候，所有 consumer 都分到所有的 queue。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271946806.png)



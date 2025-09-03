大家好，我是**华仔**, 又跟大家见面了。

今天我们来讲解下 **RocketMQ 的高级特性以及原理实现（下）**，内容相对比较多，不着急，接下来我们娓娓道来。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410271948323.png)

# **10 消息重试**

RocketMQ中消息重试指的是消费者消费消息失败后的重试。

这里分两种消息的重试，分别为「**顺序消息重试**」和 「**无序消息重试**」。

## **10.1 顺序消息重试**

对于顺序消息来说，当消费者消费消息失败后，RocketMQ 会自动不断进行消息重试（每次间隔时间为 1 秒），此时应用会出现「**消息消费被阻塞**」的情况。因此在使用「**顺序消息**」时，务必保证应

用能够及时「**监控并处理消费失败**」的情况，避免阻塞现象的发生。
```java
DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("consumer_group_01");  
consumer.setNamesrvAddr("localhost:9876");  
consumer.setConsumeMessageBatchMaxSize(1);  
consumer.setConsumeThreadMin(1);  
consumer.setConsumeThreadMax(1);  
  
// 消息订阅  
consumer.subscribe("tp_message_01", "*");  
// 顺序消费  
consumer.setMessageListener(new MessageListenerOrderly()  
{  
    @Override  
    public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs,  
                                               ConsumeOrderlyContext context)  
    {  
        for (MessageExt msg : msgs)  
        {  
            System.out.println(msg.getMsgId() + "\t" + msg.getQueueId() + "\t" + new String(msg.getBody()));  
        }  
        return null;  
    }  
});  
// 启动消费者  
consumer.start();
```

## **10.2 无序消息重试**

对于无序消息「**普通消息**」、「**定时消息**」、「**延时消息**」、「**事务消息**」，当消费者消费消息失败时，您可以通过设置返回状态达到消息重试的结果。

「**无序消息**」的重试只针对「**集群消费方式**」生效，「**广播方式**」不提供失败重试特性，即消费失败后，失败消息不再重试，继续消费新的消息。

### **10.2.1 重试次数**

RocketMQ 默认允许每条消息最多重试 「**16**」次，每次重试的间隔时间如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410272220842.png)

如果消息重试 16 次后仍然失败，消息将不再投递。如果严格按照上述重试时间间隔计算，某条消

息在一直消费失败的前提下，将会在接下来的 4 小时 46 分钟之内进行 16 次重试，超过这个时间范围消息将不再重试投递。

> 注意： 一条消息无论重试多少次，这些重试消息的 Message ID 不会改变。
  
### **10.2.2 配置方式**

#### **消费失败后，重试配置方式**

集群消费方式下，消息消费失败后如果希望消息重试，需要在消息监听器接口的实现中明确进行配置

（三种方式任选一种）：
1. 返回 [ConsumeConcurrentlyStatus.RECONSUME_LATER](http://consumeconcurrentlystatus.reconsume_later/)（推荐）。
2. 返回 Null。
3. 抛出异常。
```java
public class MyConcurrentlyMessageListener implements  
        MessageListenerConcurrently {  
    @Override  
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        //处理消息  
        doConsumeMessage(msgs);  
        // 方式1:返回 ConsumeConcurrentlyStatus.RECONSUME_LATER，消息将重试  
        return ConsumeConcurrentlyStatus.RECONSUME_LATER;  
        // 方式2:返回 null，消息将重试  
        return null;  
        // 方式3:直接抛出异常， 消息将重试  
        throw new RuntimeException("Consumer Message exceotion");  
    }  
}
```

#### **消费失败后，不重试配置方式**

集群消费方式下，消息失败后期望消息不重试，需要捕获消费逻辑中可能抛出的异常，最终返回

[ConsumeConcurrentlyStatus.CONSUME_SUCCESS](http://consumeconcurrentlystatus.consume_success/)，此后这条消息将不会再重试。
```java
public class MyConcurrentlyMessageListener implements  
        MessageListenerConcurrently {  
    @Override  
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,  
                                                    ConsumeConcurrentlyContext context) {  
        try {  
            doConsumeMessage(msgs);  
        } catch (Throwable e) {  
            //捕获消费逻辑中的所有异常，并返回  
            ConsumeConcurrentlyStatus.CONSUME_SUCCESS  
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;  
        }  
        //消息处理正常，直接返回 ConsumeConcurrentlyStatus.CONSUME_SUCCESS
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;  
    }  
}
```

#### **自定义消息最大重试次数**

RocketMQ 允许 Consumer 启动的时候设置最大重试次数，重试时间间隔将按照如下策略：
1. 最大重试次数小于等于 16 次，则重试时间间隔同上表描述。
2. 最大重试次数大于 16 次，超过 16 次的重试时间间隔均为每次 2 小时。
```java
DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("consumer_group_01");  
// 设置重新消费的次数  
// 共16个级别，大于16的一律按照2小时重试  
consumer.setMaxReconsumeTimes(20);
```

这里需要注意的是：
1. 消息最大重试次数的设置对相同 Group ID 下的所有 Consumer 实例有效。
2. 如果只对相同 Group ID 下两个 Consumer 实例中的其中一个设置了 MaxReconsumeTimes，那么该配置对两个 Consumer 实例均生效。
3. 配置采用覆盖的方式生效，即最后启动的 Consumer 实例会覆盖之前的启动实例的配置。

#### **获取消息重试次数**

消费者收到消息后，可按照如下方式获取消息的重试次数：
```java
public class MyConcurrentlyMessageListener implements  
        MessageListenerConcurrently {  
    @Override  
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,  
                                                    ConsumeConcurrentlyContext context) {  
        for (MessageExt msg : msgs) {  
            System.out.println(msg.getReconsumeTimes());  
        }  
        doConsumeMessage(msgs);  
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;  
    }  
}
```

# **11 死信队列**

在 RocketMQ 中消息重试超过一定次数后（默认16次）就会被放到死信队列中，在 RocketMQ 中，这种正常情况下无法被消费的消息称为死信消息（Dead-Letter Message），存储死信消息的特殊队列称为死信队列（Dead-Letter Queue）。

可以在控制台 Topic 列表中看到“DLQ”相关的 Topic，默认命名是：
1. %RETRY% 消费组名称叫 重试 Topic。
2. %DLQ% 消费组名称叫死信 Topic。死信队列也可以被订阅和消费，并且也会过期。

关于可视化控制台安装和使用指南可以点击以下链接：
1. [3、【入门实战系列第三篇】RocketMQ 安装入门实战](2、相关技术/23、高并发高可用高性能/MQ/华仔RocketMQ/3、【入门实战系列第三篇】RocketMQ%20安装入门实战.md)
2. [【核心运维系列第一篇】RocketMQ 运维控制台使用详解](https://articles.zsxq.com/id_ck4dqpg8pcuz.html)

## **11.1 死信特性**

死信消息具有以下特性：
1. 不会再被消费者正常消费。
2. 有效期与正常消息相同，均为 3 天，3 天后会被自动删除。因此请在死信消息产生后的 3 天内及时处理。

死信队列具有以下特性：
1. 一个死信队列对应一个 Group ID(消费组)， 而不是对应单个消费者实例。
2. 如果一个 Group ID(消费组) 未产生死信消息，消息队列 RocketMQ 不会为其创建相应的死信队列。
3. 一个死信队列包含了对应 Group ID(消费组) 产生的所有死信消息，不论该消息属于哪个 Topic。

## **11.2 查看死信信息**

1. 在控制台查询出现死信队列的主题信息
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410272255531.png)
2. 在消息界面根据主题查询死信消息
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410272304246.png)
3. 选择重新发送消息
   一条消息进入死信队列，意味着某些因素导致消费者无法正常消费该消息，通常需要对其进行特殊处理。排查可疑因素并解决问题后，可以在 RocketMQ 控制台重新发送该消息，让消费者重新消费一次。

# **12 延迟队列**

定时消息/延迟队列是指消息发送到 Broker 端后，不会立即被消费，等待特定时间投递给真正的 Topic。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410272306192.png)

在 Broker 端有配置项 [messageDelayLevel](http://messagedelaylevel/)，默认值为“1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h”总共 18 个 level。当然你也可以配置自定义 messageDelayLevel。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410272307743.png)

注意：[messageDelayLevel](http://messagedelaylevel/) 是 Broker 端属性，不属于某个 Topic。发消息时设置 [delayLevel](http://delaylevel%20/) 等级即可。
```java
msg.setDelayLevel(level)
```

其中 level 有以下三种情况：
1. `level == 0` ：消息为非延迟消息。
2. `1<=level<=maxLevel` ：消息延迟特定时间，例如 `level==1` ，延迟 1s。
3. `level > maxLevel` ：则 `level== maxLevel` ，例如 `level==20` ，延迟 2h。

定时消息会暂存在名为 S[CHEDULE_TOPIC_XXXX](http://chedule_topic_xxxx/) 的 Topic 中，并根据 delayTimeLevel 存入特定的queue。
```java
queueId = delayTimeLevel – 1
```

一个 queue 只存相同延迟的消息，保证具有相同发送延迟的消息能够顺序消费。

Broker 端会调度地消费 [SCHEDULE_TOPIC_XXXX](http://schedule_topic_xxxx/)，将消息写入真实的 Topic。

> 需要注意的是定时消息会在第一次写入和调度写入真实 Topic 时都会计数，所以发送数量、tps 都会变高。

## **生产者**

```java
public class MyProducer {  
    public static void main(String[] args) throws MQClientException,  
            RemotingException, InterruptedException, MQBrokerException {  
        DefaultMQProducer producer = new DefaultMQProducer("producer_group_01");  
        producer.setNamesrvAddr("localhost:9876");  
        producer.start();  
        Message message = null;  
        for (int i = 0; i < 20; i++) {  
            // 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h  
            message = new Message("tp_message_01", ("hello huazai - " + i).getBytes());  
            // 设置延迟级别，0表示不延迟，大于18的总是延迟2h  
            message.setDelayTimeLevel(i);  
            producer.send(message);  
        }  
        producer.shutdown();  
    }  
}
```

## **消费者**

```java
public class MyConsumer {  
    public static void main(String[] args) throws MQClientException {  
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("consumer_group_01");  
        consumer.setNamesrvAddr("localhost:9876");  
        consumer.subscribe("tp_message_01", "*");  
        consumer.setMessageListener(new MessageListenerConcurrently() {  
            @Override  
            public ConsumeConcurrentlyStatus  
            consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {  
                System.out.println(System.currentTimeMillis() / 1000);  
                for (MessageExt msg : msgs) {  
                    System.out.println(  
                            msg.getTopic() + "\t"  
                                    + msg.getQueueId() + "\t"  
                                    + msg.getMsgId() + "\t"  
                                    + msg.getDelayTimeLevel() + "\t"  
                                    + new String(msg.getBody())  
                    );  
                }  
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;  
            }  
        });  
        consumer.start();  
    }  
}
```

# **13 顺序消息**

顺序消息是指消息的「**消费顺序**」和「**生产顺序**」相同，在某些业务逻辑下，必须保证严格顺序。比如订单的「**生产**」、「**付款**」、「**发货**」这3个消息必须按顺序处理才行。

顺序消息分为「**全局顺序消息**」和「**部分顺序消息**」两种：
1. 全局顺序消息指某个 Topic 下的所有消息都要保证顺序。
2. 部分顺序消息只要保证每一组消息被顺序消费即可，比如上面订单消息的例子，只要保证同一个订单ID的三个消息能按顺序消费即可。

而在大多数的业务场景中实际上只需要保证「**局部有序**」就可以了。RocketMQ 在默认情况下不保证顺序，比如创建一个 Topic，默认 16 个写队列，16 个读队列。这时候一条消息可能被写入任意一个队列里。在数据的读取过程中，可能有多个 Consumer，每个 Consumer 也可能启动多个线程并行处理，所以消息被哪个 Consumer 消费，被消费的顺序和写入的顺序是否一致是不确定的。

要保证「**全局顺序消息**」，需要先把 Topic 的读写队列数设置为 1，然后 Producer 和 Consumer 的并发设置也要是1。简单来说，为了保证整个 Topic 的「**全局消息有序**」，只能消除所有的并发处理，各部分都设置成单线程处理。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410282157453.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410282157938.png)

如上图所示：

要保证部分消息有序，需要发送端和消费端配合处理。在发送端，要做到把同一业务 ID 的消息发送到同一个 Message Queue 中。而在消费过程中，要保证从同一个 Message Queue 读取的消息不被「**并发**」处理，这样才能达到「**部分消息有序**」。消费端通过使用 [MessageListenerOrderly](http://messagelistenerorderly/) 类来解决单 Message Queue 的消息被并发处理的问题。

在 Consumer 端使用 [MessageListenerOrderly](http://messagelistenerorderly/) 的时候，下面有四个 Consumer 的设置可以使用：
1. setConsumeThreadMin。
2. setConsumeThreadMax。
3. setPullBatchSize。
4. setConsumeMessageBatchMaxSize。

前两个参数是用来设置 Consumer 的线程数的，[PullBatchSize](http://pullbatchsize/) 指的是一次从 Broker 的一个 Message Queue 拉取消息的最大数量，默认值是 32。[ConsumeMessageBatchMaxSize](http://consumemessagebatchmaxsize/) 指的是 Consumer 的 Executor（也就是调用 [MessageListener](http://messagelistener/) 处理的地方）一次传入的消息数（[`List<MessageExt>msgs`](http://listmessageextmsgs/) 这个链表的最大长度），默认值是1。

上述四个参数可以使用，说明 [MessageListenerOrderly](http://messagelistenerorderly/) 并不是简单地禁止并发处理。在 [MessageListenerOrderly](http://messagelistenerorderly/) 的实现中，为每个 [ConsumeQueue](http://consumequeue/) 加个锁，消费每个消息前需要先获得这个消息对应的 [ConsumeQueue](http://consumequeue/) 所对应的锁，这样保证了同一时间，同一个 [ConsumeQueue](http://consumequeue/) 的消息不被并发消费，但不同 [ConsumeQueue](http://consumequeue/) 的消息可以并发处理。部分有序：

## **顺序消息的生产和消费**

OrderProducer.java
```java
package com.huazai.rocket.demo.producer;  
import org.apache.rocketmq.client.exception.MQBrokerException;  
import org.apache.rocketmq.client.exception.MQClientException;  
import org.apache.rocketmq.client.producer.DefaultMQProducer;  
import org.apache.rocketmq.common.message.Message;  
import org.apache.rocketmq.common.message.MessageQueue;  
import org.apache.rocketmq.remoting.exception.RemotingException;  
import java.util.List;  
public class OrderProducer {  
    public static void main(String[] args) throws MQClientException,  
            RemotingException, InterruptedException, MQBrokerException {  
        DefaultMQProducer producer = new DefaultMQProducer("producer_group_01");  
        producer.setNamesrvAddr("localhost:9876");  
        producer.start();  
        Message message = null;  
        List<MessageQueue> queues = producer.fetchPublishMessageQueues("tp_message_01");  
        System.err.println(queues.size());  
        MessageQueue queue = null;  
        for (int i = 0; i < 100; i++) {  
            queue = queues.get(i % 8);  
            message = new Message("tp_message_01", ("hello huazai - order create" + i).getBytes());  
            producer.send(message, queue);  
              
            message = new Message("tp_message_01", ("hello huazai - order payed" + i).getBytes());  
            producer.send(message, queue);  
              
            message = new Message("tp_message_01", ("hello huazai - order ship" + i).getBytes());  
            producer.send(message, queue);  
        }  
        producer.shutdown();  
    }  
}
```

OrderConsumer.java
```java
package com.huazai.rocket.demo.consumer;  
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;  
import org.apache.rocketmq.client.consumer.PullResult;  
import org.apache.rocketmq.client.exception.MQBrokerException;  
import org.apache.rocketmq.client.exception.MQClientException;  
import org.apache.rocketmq.common.message.MessageExt;  
import org.apache.rocketmq.common.message.MessageQueue;  
import org.apache.rocketmq.remoting.exception.RemotingException;  
import java.util.List;  
import java.util.Set;  
public class OrderConsumer {  
    public static void main(String[] args) throws MQClientException,  
            RemotingException, InterruptedException, MQBrokerException {  
        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer("consumer_group_01");  
        consumer.setNamesrvAddr("localhost:9876");  
        consumer.start();  
          
        Set<MessageQueue> messageQueues = consumer.fetchSubscribeMessageQueues("tp_message_01");  
        System.err.println(messageQueues.size());  
        for (MessageQueue messageQueue : messageQueues) {  
            long nextBeginOffset = 0;  
            System.out.println("===============================");  
            do {  
                PullResult pullResult = consumer.pull(messageQueue, "*", nextBeginOffset, 1);  
                if (pullResult == null || pullResult.getMsgFoundList() == null) break;  
                nextBeginOffset = pullResult.getNextBeginOffset();  
                List<MessageExt> msgFoundList =  
                        pullResult.getMsgFoundList();  
                System.out.println(messageQueue.getQueueId() + "\t" +                            msgFoundList.size());  
                for (MessageExt messageExt : msgFoundList) {  
                    System.out.println(messageExt.getTopic() + "\t" +  messageExt.getQueueId() + "\t" + messageExt.getMsgId() + "\t" + new String(messageExt.getBody())  
                    );  
                }  
            } while (true);  
        }  
        consumer.shutdown();  
    }  
}
```

## **全局有序**

GlobalOrderProduer.java
```java
package com.huazai.rocket.demo.producer;  
import org.apache.rocketmq.client.exception.MQBrokerException;  
import org.apache.rocketmq.client.exception.MQClientException;  
import org.apache.rocketmq.client.producer.DefaultMQProducer;  
import org.apache.rocketmq.common.message.Message;  
import org.apache.rocketmq.remoting.exception.RemotingException;  
public class GlobalOrderProducer {  
    public static void main(String[] args) throws MQClientException,  
            RemotingException, InterruptedException, MQBrokerException {  
        DefaultMQProducer producer = new DefaultMQProducer("producer_group_02");  
        producer.setNamesrvAddr("localhost:9876");  
        producer.start();  
        Message message = null;  
        for (int i = 0; i < 100; i++) {  
            message = new Message("tp_message_01", ("hello huazai" + i).getBytes());  
            producer.send(message);  
        }  
        producer.shutdown();  
    }  
}
```

GlobalOrderConsumer.java
```java
package com.huazai.rocket.demo.consumer;  
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;  
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;  
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;  
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;  
import org.apache.rocketmq.client.exception.MQClientException;  
import org.apache.rocketmq.common.message.MessageExt;  
import java.util.List;  
public class GlobalOrderConsumer {  
    public static void main(String[] args) throws MQClientException {  
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("consumer_group_02");  
        consumer.setNamesrvAddr("localhost:9876");  
        consumer.subscribe("tp_message_01", "*");  
        consumer.setConsumeThreadMin(1);  
        consumer.setConsumeThreadMax(1);  
        consumer.setPullBatchSize(1);  
        consumer.setConsumeMessageBatchMaxSize(1);  
        consumer.setMessageListener(new MessageListenerOrderly() {  
            @Override  
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt>  
                                                               msgs, ConsumeOrderlyContext context) {  
                for (MessageExt msg : msgs) {  
                    System.out.println(new String(msg.getBody()));  
                }  
                return ConsumeOrderlyStatus.SUCCESS;  
            }  
        });  
        consumer.start();  
    }  
}
```

# **14 事务消息**

RocketMQ的事务消息，是指发送消息事件和其他事件需要同时成功或同时失败。比如银行转账，A 银行的某账户要转一万元到 B 银行的某账户。A银行发送“B银行账户增加一万元”这个消息，要和“从A银

行账户扣除一万元”这个操作同时成功或者同时失败。

  

RocketMQ 采用「**两阶段提交**」的方式实现事务消息，[TransactionMQProducer](http://transactionmqproducer/) 处理上面情况的流程是，先发一个“准备从B银行账户增加一万元”的消息，发送成功后做从A银行账户扣除一万元的操作，根据操作结果是否成功，确定之前的“准备从B银行账户增加一万元”的消息是做 commit 还是 rollback，具体流程如下：

  

1. 发送方向 RocketMQ 中发送“待确认”消息。
2. RocketMQ 将收到的“待确认”消息持久化成功后，向发送方回复消息已经发送成功，此时第一阶段消息发送完成。
3. 发送方开始执行本地事件逻辑。
4. 发送方根据本地事件执行结果向 RocketMQ 发送二次确认（Commit或是Rollback）消息，RocketMQ 收到 Commit 状态则将第一阶段消息标记为可投递，订阅方将能够收到该消息；收到 Rollback 状态则删除第一阶段的消息，订阅方接收不到该消息。
5. 如果出现异常情况，步骤4 提交的二次确认最终未到达 RocketMQ，服务器在经过固定时间段后将对“待确认”消息发起回查请求。
6. 发送方收到消息回查请求后（如果发送一阶段消息的Producer不能工作，回查请求将被发送到和 Producer 在同一个Group 里的其他 Producer），通过检查对应消息的本地事件执行结果返回 Commit 或 Rollback 状态。
7. RocketMQ 收到回查请求后，按照步骤4 的逻辑处理。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410282306421.png)

上面的逻辑似乎很好地实现了事务消息功能，它也是 RocketMQ 之前的版本实现事务消息的逻辑。但是因为RocketMQ 依赖将「**数据顺序写到磁盘**」这个特征来提高性能，步骤 4 却需要更改第一阶段消息的状态，这样会造成「**磁盘Cache 的脏页过多**」，降低系统的性能。

所以RocketMQ 在 4.x 的版本中将这部分功能去除。系统中的一些上层 Class 都还在，用户可以根据实际需求实现自己的事务功能。

客户端有三个类来支持用户实现事务消息：
1. 第一个类是 [LocalTransaction-Executer](http://localtransaction-executer/)，用来实例化步骤 3 的逻辑，根据情况返回 [LocalTransactionState.ROLLBACK_MESSAGE](http://localtransactionstate.rollback_message/) 或者 [LocalTransactionState.COMMIT_MESSAGE](http://localtransactionstate.commit_message/) 状态。
2. 第二个类是 [TransactionMQProducer](http://transactionmqproducer/)，它的用法和 [DefaultMQProducer](http://defaultmqproducer/) 类似，要通过它启动一个 Producer 并发消息，但是比 [DefaultMQProducer](http://defaultmqproducer/) 多设置「**本地事务处理函数**」和「**回查状态函数**」。
3. 第三个类是 [TransactionCheckListener](http://transactionchecklistener/)，实现步骤 5 中 MQ 服务器的回查请求，返回[LocalTransactionState.ROLLBACK_MESSAGE](http://localtransactionstate.rollback_message/) 或者 [LocalTransactionState.COMMIT_MESSAGE](http://localtransactionstate.commit_message/)。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410282321520.png)

上图说明了事务消息的大致方案，其中分为两个流程：正常事务消息的发送及提交、事务消息的补偿流程。

1.事务消息发送及提交：
1. 发送消息（half消息）。
2. 服务端响应消息写入结果。
3. 根据发送结果执行本地事务（如果写入失败，此时half消息对业务不可见，本地逻辑不执行）。
4. 根据本地事务状态执行Commit或者Rollback（Commit操作生成消息索引，消息对消费者可见）。

2.补偿流程：
1. 对没有Commit/Rollback的事务消息（pending状态的消息），从服务端发起一次“回查”
2. Producer收到回查消息，检查回查消息对应的本地事务的状态
3. 根据本地事务状态，重新Commit或者Rollback。其中，补偿阶段用于解决消息Commit或者Rollback发生超时或者失败的情况。

## **14.1 事务消息设计**

### **14.1.1 事务消息在一阶段对用户不可见**

在RocketMQ事务消息的主要流程中，一阶段的消息如何对用户不可见。其中，事务消息相对普通消息最大的特点就是一阶段发送的消息对用户是不可见的。

那么，如何做到写入消息但是对用户不可见呢？

RocketMQ事务消息的做法是：如果消息是 half 消息，将备份原消息的主题与消息消费队列，然后改变主题为 [RMQ_SYS_TRANS_HALF_TOPIC](http://rmq_sys_trans_half_topic/)。由于消费组未订阅该主题，所以消费端无法消费 half 类型的消息。然后二阶段会显示执行提交或者回滚half消息（逻辑删除）。

当然，为了防止二阶段操作失败，RocketMQ 会开启一个定时任务，从 Topic 为 [RMQ_SYS_TRANS_HALF_TOPIC](http://rmq_sys_trans_half_topic/)中拉取消息进行消费，根据生产者组获取一个服务提供者发送回查事务状态请求，根据事务状态来决定是提交或回滚消息。

在 RocketMQ 中，消息在服务端的存储结构如下，每条消息都会有对应的索引信息，Consumer 通过[ConsumeQueue](http://consumequeue/) 这个二级索引来读取消息实体内容，其流程如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410282344461.png)
  
RocketMQ 的具体实现策略是：写入的如果事务消息，对消息的 Topic 和 Queue 等属性进行替换，同时将原来的Topic 和 Queue 信息存储到消息的属性中，正因为消息主题被替换，所以消息并不会转发到该原主题的消息消费队列，消费者无法感知消息的存在，不会消费。

### **14.1.2 Commit、Rollback 操作以及 Op 消息引入**

在完成一阶段写入一条对用户不可见的消息后，二阶段如果是 Commit 操作，则需要让消息对用户可见。如果是Rollback 则需要撤销一阶段的消息。先说 Rollback 的情况。对于Rollback，本身一阶段的消息对用户是不可见的，其实不需要真正撤销消息（实际上 RocketMQ 也无法去真正的删除一条消息，因为是顺序写文件的）。但是区别于这条消息没有确定状态（Pending状态，事务悬而未决），需要一个操作来标识这条消息的「**最终状态**」。

RocketMQ 事务消息方案中引入了 Op 消息的概念，用 Op 消息标识事务消息已经确定的状态（Commit或者Rollback）。如果一条事务消息没有对应的Op消息，说明这个事务的状态还无法确定（可能是二阶段失败了）。引入 Op 消息后，事务消息无论是 Commit 或者 Rollback 都会记录一个 Op 操作。Commit 相对于 Rollback 只是在写入Op 消息前创建 Half 消息的索引。

### **14.1.3 Op 消息存储和对应关系**

RocketMQ 将 Op 消息写入到全局一个特定的Topic中通过源码中的方法
```java
TransactionalMessageUtil.buildOpTopic();
```

这个 Topic 是一个内部的Topic（像 Half 消息的 Topic 一样），不会被用户消费。Op 消息的内容为对应的 Half 消息的存储的 Offset，这样通过 Op 消息能索引到 Half 消息进行后续的回查操作。

### **14.1.4 Half 消息的索引构建**

在执行二阶段 Commit 操作时，需要构建出 Half 消息的索引。一阶段的 Half 消息由于是写到一个特殊的 Topic，所以二阶段构建索引时需要读取出 Half 消息，并将 Topic 和 Queue 替换成真正的目标的 Topic 和 Queue，之后通过一次普通消息的写入操作来生成一条对用户可见的消息。所以 RocketMQ 事务消息二阶段其实是利用了一阶段存储的消息的内容，在二阶段时恢复出一条完整的普通消息，然后走一遍消息写入流程。

### **14.1.5 如何处理二阶段失败的消息**

如果在 RocketMQ 事务消息的二阶段过程中失败了，例如在做 Commit 操作时，出现网络问题导致 Commit 失败，那么需要通过一定的策略使这条消息最终被Commit。

RocketMQ 采用了一种补偿机制，称为「**回查**」。Broker 端对未确定状态的消息发起回查，将消息发送到对应的Producer 端（同一个 Group 的 Producer），由 Producer 根据消息来检查本地事务的状态，进而执行 Commit或者 Rollback。

Broker 端通过对比 Half 消息和 Op 消息进行事务消息的回查并且推进 CheckPoint（记录那些事务消息的状态是确定的）。

> 值得注意的是，RocketMQ 并不会无休止的的信息事务状态回查，默认回查 15 次，如果 15 次回查还是无法得知事务状态，RocketMQ 默认回滚该消息。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292156624.png)

事务消息 TxProducer.java
```java
package com.huazai.rocket.demo.producer;  
import org.apache.rocketmq.client.exception.MQClientException;  
import org.apache.rocketmq.client.producer.LocalTransactionState;  
import org.apache.rocketmq.client.producer.TransactionListener;  
import org.apache.rocketmq.client.producer.TransactionMQProducer;  
import org.apache.rocketmq.common.message.Message;  
import org.apache.rocketmq.common.message.MessageExt;  
public class TxProducer {  
    public static void main(String[] args) throws MQClientException {  
        TransactionListener listener = new TransactionListener() {  
              
            @Override  
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {  
                // 当发送事务消息prepare(half)成功后，调用该方法执行本地事务  
                System.out.println("执行本地事务，参数为：" + arg);  
                try {  
                    Thread.sleep(100000);  
                } catch (InterruptedException e) {  
                    e.printStackTrace();  
                }  
                // return LocalTransactionState.ROLLBACK_MESSAGE;  
                return LocalTransactionState.COMMIT_MESSAGE;  
            }  
              
            @Override  
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {  
                // 如果没有收到生产者发送的Half Message的响应，broker发送请求到生产者回查生产者本地事务的状态，该方法用于获取本地事务执行的状态。  
                System.out.println("检查本地事务的状态：" + msg);  
                return LocalTransactionState.COMMIT_MESSAGE;  
                // return LocalTransactionState.ROLLBACK_MESSAGE;  
            }  
        };  
        TransactionMQProducer producer = new TransactionMQProducer("tx_producer_group_01");  
        producer.setTransactionListener(listener);  
        producer.setNamesrvAddr("localhost:9876");  
        producer.start();  
        Message message = null;  
        message = new Message("tp_message_01", "hello huazai- tx".getBytes());  
        producer.sendMessageInTransaction(message, "{\"name\":\"zhangsan\"}");  
    }  
}
```

TxConsumer.java
```java
package com.huazai.rocket.demo.consumer;  
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;  
import  
        org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;  
import  
        org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;  
import  
        org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;  
import org.apache.rocketmq.client.exception.MQClientException;  
import org.apache.rocketmq.common.message.MessageExt;  
import java.util.List;  
public class TxConsumer {  
    public static void main(String[] args) throws MQClientException {  
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("txconsumer_group_01");  
        consumer.setNamesrvAddr("localhost:9876");  
        consumer.subscribe("tp_message_01", "*");  
        consumer.setMessageListener(new MessageListenerConcurrently() {  
            @Override  
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {  
                for (MessageExt msg : msgs) {  
                    System.out.println(new String(msg.getBody()));  
                }  
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;  
            }  
        });  
        consumer.start();  
    }  
}
```

# **15 消息查询**

RocketMQ 支持按照下面两种维度进行消息查询
1. 按照 Message Id 查询消息。
2. 按照 Message Key 查询消息。

## **15.1 按照 Message Id 查询消息**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292216198.png)

MsgId 总共 16 字节，包含消息存储主机地址（ip/port），消息 Commit Log offset。从 MsgId 中解析出 Broker 的地址和 Commit Log 的偏移地址，然后按照存储格式所在位置将消息 buffer 解析成一个完整的消息。

在 RocketMQ 中具体做法是：Client 端从 MessageId 中解析出 Broker 的地址（IP地址和端口）和Commit Log的偏移地址后封装成一个 RPC 请求后，通过 [Remoting](http://remoting/) 通信层发送（业务请求码：VIEW_MESSAGE_BY_ID）。Broker 使用 [QueryMessageProcessor](http://querymessageprocessor/)，使用请求中的 commitLog offset 和 size 去 [commitLog](http://commitlog/) 中找到真正的记录并解析成一个完整的消息返回。

## **15.2 按照 Message key 查询消息**

主要是基于 RocketMQ 的 IndexFile 索引文件来实现的。RocketMQ 的索引文件逻辑结构，类似 JDK 中HashMap 的实现。索引文件的具体结构如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292236391.png)

1. 根据查询的 key 的 hashcode%slotNum 得到具体的槽的位置（slotNum 是一个索引文件里面包含的最大槽的数目， 例如图中所示 slotNum=5000000）。
2. 根据 slotValue（slot 位置对应的值）查找到索引项列表的最后一项（倒序排列，slotValue 总是指向最新的一个索引项）。
3. 遍历索引项列表返回查询时间范围内的结果集（默认一次最大返回的 32 条记录）
4. Hash 冲突：
5. 第一种，key 的 hash 值不同但模数相同，此时查询的时候会再比较一次 key 的 hash 值（每个索引项保存了 key 的 hash 值），过滤掉 hash 值不相等的项。
6. 第二种，hash 值相等但 key 不等， 出于性能的考虑冲突的检测放到客户端处理（key 的原始值是存储在消息文件中的，避免对数据文件的解析）， 客户端比较一次消息体的 key 是否相同。
7. 存储：为了节省空间索引项中存储的时间是时间差值（存储时间-开始时间，开始时间存储在索引文件头中）， 整个索引文件是定长的，结构也是固定的。

API的使用：
```java
package com.huazai.rocket.demo.query;  
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;  
import org.apache.rocketmq.client.exception.MQBrokerException;  
import org.apache.rocketmq.client.exception.MQClientException;  
import org.apache.rocketmq.common.message.MessageExt;  
import org.apache.rocketmq.remoting.exception.RemotingException;  
public class QueryingMessageDemo {  
    public static void main(String[] args) throws InterruptedException,  
            RemotingException, MQClientException, MQBrokerException {  
        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer("consumer_group_01");  
        consumer.setNamesrvAddr("localhost:9876");  
        consumer.start();  
        MessageExt message = consumer.viewMessage("tp_message_01","0A4E00A7178878308DB150A780BB0000");  
        System.out.println(message);  
        System.out.println(message.getMsgId());  
        consumer.shutdown();  
    }  
}
```

# **16 消息优先级**

有些场景，需要应用程序处理几种类型的消息，不同消息的优先级不同。RocketMQ 是个 「**先入先出**」 的队列，不支持「**消息级别**」或者「**Topic 级别**」的优先级。业务中简单的优先级需求，可以通过间接的方式解决，下面列举三种优先级相关需求的具体处理方法。

## **16.1 第一种 Topic 分类**

多个不同的消息类型使用同一个 Topic 时，由于某一个种消息流量非常大，导致其他类型的消息无法及时消费，造成不公平，所以把流量大的类型消息在一个单独的 Topic，其他类型消息在另外一个 Topic，应用程序创建两个 Consumer，分别订阅不同的 Topic，这样就可以了。

## **16.2 第二种**

情况和第一种情况类似，但是不用创建大量的 Topic。

举个实际应用场景: 一个订单处理系统，接收从 100 家快递门店过来的请求，把这些请求通过 Producer 写入RocketMQ。订单处理程序通过 Consumer 从队列里读取消息并处理，每天最多处理 1 万单。如果这 100 个快递门店中某几个门店订单量大增，比如门店一接了个大客户，一个上午就发出 2 万单消息请求，这样其他的 99 家门店可能被迫等待门店一的 2 万单处理完，也就是两天后订单才能被处理，显然很不公平 。

这时可以创建一个 Topic， 设置 Topic 的 [MessageQueue](http://messagequeue/) 数量超过 100 个，Producer 根据订单的门店号，把每个门店的订单写人一个 [MessageQueue](http://messagequeue/)。 [DefaultMQPushConsumer](http://defaultmqpushconsumer/) 默认是采用循环的方式逐个读取一个 Topic 的所有 [MessageQueue](http://messagequeue/)，这样如果某家门店订单量大增，这家门店对应的 [MessageQueue](http://messagequeue/) 消息数增多，等待时间增长，但不会造成其他家门店等待时间增长。

[DefaultMQPushConsumer](http://defaultmqpushconsumer/) 默认的 [pullBatchSize](http://pullbatchsize/) 是 32，也就是每次从某个 [MessageQueue](http://messagequeue/) 读取消息的时候，最多可以读 32 个 。 在上面的场景中，为了更加公平，可以把 [pullBatchSize](http://pullbatchsize/) 设置成1。

## **16.3 第三种 强制优先级**

TypeA、 TypeB、 TypeC 三类消息 。 TypeA 处于第一优先级，要确保只要有 TypeA 消息，必须优先处理。TypeB 处于第二优先级，TypeC 处于第三优先级 。

对这种要求，或者逻辑更复杂的要求，就要用户自己编码实现优先级控制，如果上述的三类消息在一个 Topic 里，可以使用 [PullConsumer](http://pullconsumer/)，自主控制 [MessageQueue](http://messagequeue/) 的遍历，以及消息的读取。如果上述三类消息在三个 Topic下，需要启动三个 [Consumer](http://consumer/)， 实现逻辑控制三个 [Consumer](http://consumer/) 的消费 。

# **17 底层网络通信**

RocketMQ 底层通信的实现是在 Remoting 模块里，因为借助了 Netty 而没有重复造轮子，RocketMQ 的通信部分没有很多的代码，就是用 Netty 实现了一个自定义协议的客户端/服务器程序。
1. 自定义 ByteBuf 可以从底层解决 ByteBuffer 的一些问题，并且通过“内存池”的设计来提升性能。
2. Reactor 主从多线程模型。
3. 充分利用了零拷贝，CAS/volatite 高效并发编程特性。
4. 无锁串行化设计。
5. 管道责任链的编程模型。
6. 高性能序列化框架的支持。
7. 灵活配置 TCP 协议参数。

RocketMQ 集群主要包括「**NameServer**」、「**Broker（Master/Slave）**」、「**Producer**」、「**Consumer**」4 个角色，基本通讯流程如下：
1. Broker 启动后需要完成一次将自己注册至「**NameServer**」的操作，随后每隔「**30s**」时间定时向 NameServer 上报 Topic 路由信息。
2. 消息生产者 Producer 作为客户端发送消息时候，需要根据消息的 Topic 从本地缓存的 [TopicPublishInfoTable](http://topicpublishinfotable/) 获取路由信息。如果没有则更新路由信息会从 NameServer 上重新拉取，同时Producer 会默认每隔「**30s**」向 NameServer 拉取一次路由信息。
3. 消息生产者 Producer 根据第二步中获取的路由信息选择一个队列 MessageQueue 进行消息发送，而 Broker 作为消息的接收者接收消息并落盘存储。
4. 消息消费者 Consumer 根据第二步中获取的路由信息，并再完成客户端的负载均衡后，选择其中的某一个或者某几个消息队列来拉取消息并进行消费。
5. 从上面 `第一步~第三步` 中可以看出在消息生产者，Broker 和 NameServer 之间都会发生通信（这里只说了MQ的部分通信），因此如何设计一个良好的网络通信模块在 MQ 中至关重要，它将决定 RocketMQ 集群整体的消息传输能力与最终的性能。

[rocketmq-remoting](http://rocketmq-remoting/) 模块是 RocketMQ 中负责网络通信的模块，它几乎被其他所有需要网络通信的模块（诸如[rocketmq-client](http://rocketmq-client/)、[rocketmq-broker](http://rocketmq-broker/)、[rocketmq-namesrv](http://rocketmq-namesrv/)）所依赖和引用。为了实现客户端与服务器之间高效的数据请求与接收，RocketMQ 消息队列自定义了通信协议并在 Netty 的基础之上扩展了通信模块。

**RocketMQ中惯用的套路：**

请求报文和响应都使用 [RemotingCommand](http://remotingcommand/)，然后在 [Processor](http://processor/) 处理器中根据 RequestCode 请求码来匹配对应的处理方法。

处理器通常继承至 [NettyRequestProcessor](http://nettyrequestprocessor/)，使用前需要先注册才行，注册方式[remotingServer.registerDefaultProcessor](http://remotingserver.registerdefaultprocessor/)。

**网络通信核心的东西无非是：**
1. 线程模型
2. 私有协议定义
3. 编解码器
4. 序列化/反序列化
5. …

既然是基于Netty的网络通信，当然少不了一堆自定义实现的Handler，例如继承至：[SimpleChannelInboundHandler ChannelDuplexHandler](http://simplechannelinboundhandler%20channelduplexhandler/)。

## **17.1 Remoting 通信类结构**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292311533.png)

## **17.2 协议设计与编码**

在 Client 和 Server 之间完成一次消息发送时，需要对发送的消息进行一个协议约定，因此就有必要自定义 RocketMQ 的消息协议。同时为了高效地在网络中传输消息和对收到的消息读取，就需要对消息进行编解码。

在 RocketMQ 中，[RemotingCommand](http://remotingcommand/) 这个类在消息传输过程中对所有数据内容的封装，不但包含了所有的数据结构，还包含了编码解码操作。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292314267.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292317791.png)

可见传输内容主要可以分为以下 4 部分：
1. 消息长度：总长度，四个字节存储，占用一个int类型。
2. 序列化类型&消息头长度：同样占用一个int类型，第一个字节表示序列化类型，后面三个字节表示消息头长度。
3. 消息头数据：经过序列化后的消息头数据。
4. 消息主体数据：消息主体的二进制字节数据内容。

## **17.3 消息通信方式和流程**

在 RocketMQ 中支持通信的方式主要有「**同步 Sync**」、「**异步 Async**」、「**单向 Oneway**」三种。其中「**单向 Oneway**」通信模式相对简单，一般用在发送心跳包场景下，无需关注其 Response。这里，主要介绍RocketMQ 的异步通信流程。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292325595.png)

## **17.4 Reactor 主从多线程模型**

RocketMQ 的 RPC 通信采用 Netty 组件作为底层通信库，同样也遵循了 Reactor 多线程模型，同时又在这之上做了一些扩展和优化。

下面先给出一张 RocketMQ 的 RPC 通信层的 Netty 多线程模型框架图，让大家对 RocketMQ 的 RPC 通信中的多线程分离设计有一个大致的了解。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292332456.png)

上图就是 RocketMQ 中 RPC 通信的 [1+N+M1+M2](http://1+n+m1+m2/) 的 Reactor 多线程设计与实现。可以大致了解 RocketMQ 中NettyRemotingServer 的 Reactor 多线程模型。一个 Reactor 主线程（eventLoopGroupBoss，即为上面的1）负责监听 TCP网络连接请求，建立好连接，创建 SocketChannel，并注册到 selector 上。RocketMQ 的源码中会自动根据 OS 的类型选择 NIO 和 Epoll，也可以通过参数配置）,然后监听真正的网络数据。

拿到网络数据后，再丢给 Worker 线程池（[eventLoopGroupSelector](http://eventloopgroupselector/)，即为上面的“N”，源码中默认设置为3），在真正执行业务逻辑之前需要进行SSL验证、编解码、空闲检查、网络连接管理，这些工作交给[defaultEventExecutorGroup](http://defaulteventexecutorgroup/)（即为上面的“M1”，源码中默认设置为8）去做。处理业务操作放在业务线程池中执行，根据 [RomotingCommand](http://romotingcommand/) 的业务请求码 code 去 [processorTable](http://processortable/) 这个本地缓存变量中找到对应的 processor，然后封装成task任务后，提交给对应的业务 processor 处理线程池来执行（[sendMessageExecutor](http://sendmessageexecutor/)，以发送消息为例，即为上面的 “M2”）。从入口到业务逻辑的几个步骤中线程池一直再增加，这跟每一步逻辑复杂性相关，越复杂，需要的并发通道越宽。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292335372.png)

# **18 限流**

RocketMQ消费端中我们可以：
1. 设置最大消费线程数
2. 每次拉取消息条数等

同时：
1. PushConsumer会判断获取但还未处理的消息个数、消息总大小、Offset 的跨度。
2. 任何一个值超过设定的大小就隔一段时间再拉取消息，从而达到流量控制的目的。

在 Apache RocketMQ 中，当消费者去消费消息的时候，无论是通过 pull 的方式还是 push 的方式，都可能会出现大批量的消息突刺。如果此时要处理所有消息，很可能会导致系统负载过高，影响稳定性。但其实可能后面几秒之内都没有消息投递，若直接把多余的消息丢掉则没有充分利用系统处理消息的能力。我们希望可以把消息突刺均摊到一段时间内，让系统负载保持在消息处理水位之下的同时尽可能地处理更多消息，从而起到「**削峰填谷**」的效果：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292340355.png)

上图中红色的部分代表超出消息处理能力的部分。我们可以看到消息突刺往往都是瞬时的、不规律的，其后一段时间系统往往都会有空闲资源。我们希望把红色的那部分消息平摊到后面空闲时去处理，这样既可以保证系统负载处在一个稳定的水位，又可以尽可能地处理更多消息。

## **18.1 Sentinel 介绍**

[Sentinel](https://github.com/alibaba/Sentinel) 是阿里中间件团队开源的，面向分布式服务架构的轻量级流量控制产品，主要以流量为切入点，从「**流量控制**」、「**熔断降级**」、「**系统负载保护**」等多个维度来帮助用户保护服务的稳定性。

## **18.2 Sentinel 原理**

Sentinel 专门为这种场景提供了匀速器的特性，可以把突然到来的大量请求以匀速的形式均摊，以固定的间隔时间让请求通过，以稳定的速度逐步处理这些请求，起到“削峰填谷”的效果，从而避免流量突刺造成系统负载过高。同时堆积的请求将会排队，逐步进行处理；当请求排队预计超过最大超时时长的时候则直接拒绝，而不是拒绝全部请求。

比如在 RocketMQ 的场景下配置了匀速模式下请求 QPS 为 5，则会每 200 ms 处理一条消息，多余的处理任务将排队；同时设置了超时时间为 5 s，预计排队时长超过 5s 的处理任务将会直接被拒绝。

示意图如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292343268.png)

RocketMQ 用户可以根据不同的 group 和不同的 topic 分别设置限流规则，限流控制模式设置为匀

速器模式（[RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER](http://ruleconstant.control_behavior_rate_limiter/)），比如：
```java
private void initFlowControlRule() {  
    FlowRule rule = new FlowRule();  
    rule.setResource(KEY); // 对应的 key 为 groupName:topicName    rule.setCount(5);  
    rule.setGrade(RuleConstant.FLOW_GRADE_QPS);  
    rule.setLimitApp("default");  
    // 匀速器模式下，设置了 QPS 为 5，则请求每 200 ms 允许通过 1 个  
    rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER);  
    // 如果更多的请求到达，这些请求会被置于虚拟的等待队列中。等待队列有一个 max timeout，如果请求预计的等待时间超过这个时间会直接被 block    // 在这里，timeout 为 5s    rule.setMaxQueueingTimeMs(5 * 1000);  
    FlowRuleManager.loadRules(Collections.singletonList(rule));  
}
```




参考: [https://github.com/alibaba/Sentinel/wiki/Sentinel-%E4%B8%BA-RocketMQ-%E4%BF%9D%E9%A9%BE%E6%8A%A4%E8%88%AA](https://github.com/alibaba/Sentinel/wiki/Sentinel-%E4%B8%BA-RocketMQ-%E4%BF%9D%E9%A9%BE%E6%8A%A4%E8%88%AA)


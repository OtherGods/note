
大家好，我是**华仔**, 又跟大家见面了。

这里我会梳理并更新一部分关于 **RocketMQ** 基础功能操作的文章，今天是第二篇，我们先来讲解下 **RocketMQ 顺序消息发送的模式分类以及具体的代码示例**，下面是正文。

# **01 什么是顺序消息**

顺序消息指的是严格按照消息的发送顺序进行消费的消息，即 FIFO。

默认情况下生产者会把消息以「**轮询方式**」发送到不同的「**Queue 分区队列**」上，而消费消息时会从多个「**Queue 分区队列**」上拉取消息，在这种情况下的发送和消费是「**不能保证顺序**」。

如果将消息仅发送到「**同一个 Queue**」中，消费时也只从「**该 Queue**」上拉取消息，就严格保证了消息的顺序性。

# **02 为什么需要顺序消息**

这里举例说明下顺序消息在实际业务中如何使用？

比如：我们在做电商系统时肯定需要订单相关操作，根据当前订单的不同状态，此时订单可能会有以下几种状态：
1. 订单待支付。
2. 订单已支付。
3. 订单发货中。
4. 订单发货成功。
5. 订单发货失败。

针对该业务，我们现在有 [TOPIC ORDER_STATUS](http://topic%20order_status/) (订单状态)，其下有「**4 个 Queue 分区队列**」 。

根据以上订单状态，生产者从时序上可以生成如下几个消息：

> 订单 T0000001:未支付 --> 订单 T0000001:已支付 --> 订单 T0000001:发货中 --> 订单 T0000001:发货成功

消息发送到 RocketMQ 中之后，Queue 的选择如果采用「**轮询策略**」，消息在 RocketMQ 的存储可能如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410302335252.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410302335650.png)

在这种情况下，我们希望 Consumer 消费消息的顺序和我们发送是一致的，然而如果按照上图投递和消费方式，是无法保证顺序是正确的。

对于顺序异常的消息，Consumer 即使设置有一定的状态容错，也不能完全处理好这么多种随机出现组合情况。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410302335110.png)

基于该情况，可以设计如下方案：对于「**相同订单号**」的消息，通过一定的策略，将其存储到同一个「**Queue 分区队列**」中，然后消费者再采用一定的策略，能够保证消费的顺序性。

> 消费者可以采用一个线程独立处理一个 queue，保证处理消息的顺序性。

# **03 顺序性分类**

根据顺序范围的不同，RocketMQ 可以严格地保证两种消息的有序性：「**全局有序**」和「**分区有序**」。
1. 全局顺序消息指某个 Topic 下的所有消息都要保证顺序。
2. 部分顺序消息只要保证每一组消息被顺序消费即可，比如上面订单消息的例子，只要保证同一个订单ID的三个消息能按顺序消费即可。

## **3.1 全局顺序**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410302340504.png)

当发送和消费时只有一个 Queue时才能保证整个 Topic 中消息的有序性， 简单称为「**全局有序**」。

> 在创建 Topic 时指定 Queue 的数量，这里有三种指定方式，如下：
> 1. 在代码中创建 Producer 时，可以指定其自动创建的 Topic 的 Queue 数量。
> 2. 在 RocketMQ 可视化控制台中手动创建 Topic 时指定 Queue 数量。
> 3. 使用 mqadmin 命令手动创建 Topic 时指定 Queue 数量。

## **3.2 分区顺序**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410302342821.png)

如果有多个 Queue 参与，只能保证在某一个 Queue 分区队列上的消息顺序，简单称为「**分区有序**」。

那么我们该如何选择 Queue 呢？

在定义 Producer 时我们可以指定消息队列选择器，而这个选择器是我们自己实现了 [MessageQueueSelector](http://messagequeueselector/) 接口定义的。

在定义选择器的选择算法时，一般需要使用选择 key。这个选择 key 可以是消息 key 也可以是其它数据。但无论谁做选择 key，都不能重复，「**必须唯一**」。

通常的选择算法是，让消息 key 或者消息 key 的 hash 值与该 Topic 所包含的 Queue 的数量取模，其结果即为选择出的 Queue 的 QueueId。

但是取模算法存在一个问题：

> 不同选择 key 与 Queue 数量取模结果可能会是相同的，即不同选择 key 的消息可能会出现在相同的 Queue，即同一个 Consuemr 可能会消费到不同选择 key 的消息。

那么这个问题如何解决？

这里我们可以先从消息中获取到选择key，然后对其进行判断。如果当前 Consumer 需要消费的消息，则直接消费，否则什么也不做。

这种做法要求选择 key 要能够随着消息一起被 Consumer 获取到。

> 此时使用消息 key 作为选择 key 是比较好的做法。

这样的话会不会出现如下新的问题呢？即不属于那个 Consumer 的消息被拉取走了，那么应该消费该消息的 Consumer 是否还能再消费该消息呢？

因为同一个 Queue 中的消息不可能被同一个 ConsumerGroup 中的不同 Consumer 同时消费。所以消费现一个 Queue 的不同选择 key 的消息的 Consumer 一定属于不同的 ConsumerGroup。而不同的 ConsumerGroup 中的 Consumer 间的消费是相互隔离的，互不影响的。

# **04 代码示例**

## **4.1 顺序消息的生产和消费**

[OrderProducer.java](http://orderproducer.java/)
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

[OrderConsumer.java](http://orderconsumer.java/)
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

## **4.2 全局有序**

[GlobalOrderProduer.java](http://globalorderproduer.java/)
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

[GlobalOrderConsumer.java](http://globalorderconsumer.java/)
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



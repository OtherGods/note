大家好，我是**华仔**, 又跟大家见面了。

这里我会梳理并更新一部分关于 **RocketMQ** 基础功能操作的文章，今天是第三篇，我们先来讲解下 **RocketMQ 延迟消息发送以及具体的代码示例**，下面是正文。

# **01 什么是延时消息**

当消息写入到 Broker 后，在指定的时长后才可被消费处理的消息，称为延时消息。

采用 RocketMQ 的延时消息可以实现「**定时任务**」的功能，而无需使用定时器。典型的应用场景是，电商交易中「**超时未支付关闭订单**」场景，12306平台订票「**超时未支付取消订票**」的场景。

> 在电商平台中，订单创建时会发送一条延迟消息。这条消息将会在 30 分钟后投递给后台业务系统Consumer，后台业务系统收到该消息后会判断对应的订单是否已经完成支付。如果未完成，则取消订单，将商品再次放回到库存；如果完成支付则忽略。
> 
> 在12306平台中，车票预订成功后就会发送一条延迟消息。这条消息将会在 45 分钟后投递给后台业务系统Consumer，后台业务系统收到该消息后会判断对应的订单是否已经完成支付。如果未完成则取消预订，将车票再次放回到票池；如果完成支付则忽略。

# **02 延时等级**

延时消息的延迟时长不支持随意时长的延迟，是通过特定的延迟等级来指定的。

延时等级定义在 RocketMQ 服务端的 [MessageStoreConfig](http://messagestoreconfig/) 类中的如下变量中：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411031309919.png)

即，若指定的延时等级为 3，则表示延迟时长为 10s，即延迟等级是从 1 开始计数的。

当然，如果需要自定义的延时等级，可以通过在 broker 加载的配置中新增如下配置（例如下面增加了1天这个等级1d）。配置文件在 RocketMQ 安装目录下的 conf 目录中。
```java
messageDelayLevel = 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h 1d
```

# **03 延时消息实现原理**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411031315040.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411031316912.png)

具体实现方案是：

## **3.1 修改消息**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411031318900.png)

Producer 将消息发送到 Broker 后，Broker 会首先将消息写入到 commitlog 文件，然后需要将其分发到相应的 [consumequeue](http://consumequeue/)。不过，在分发之前，系统会先判断消息中是否带有延时等级。如果没有则直接正常分发，否则需要经历一个复杂的过程：
1. 修改消息的 Topic 为 [SCHEDULE_TOPIC_XXXX](http://schedule_topic_xxxx/)。
2. 根据延时等级，在 [consumequeue](http://consumequeue/) 目录中 [SCHEDULE_TOPIC_XXXX](http://schedule_topic_xxxx/) 主题下创建出相应的 [queueId](http://queueid/) 目录与 [consumequeue](http://consumequeue/) 文件（如果没有这些目录与文件的话）。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411031321565.png)
> 延迟等级delayLevel与queueId的对应关系为queueId = delayLevel -1。
> 
> 需要注意，在创建queueId目录时，并不是一次性地将所有延迟等级对应的目录全部创建完毕，而是用到哪个延迟等级创建哪个目录。
3. 修改消息索引单元内容。索引单元中的 [Message Tag HashCode](http://message%20tag%20hashcode/) 部分原本存放的是消息的 [Tag](http://tag/) 的 [Hash](http://hash/) 值。现修改为消息的投递时间。投递时间是指该消息被重新修改为原 Topic 后再次被写入到 [commitlog](http://commitlog/) 中的时间。投递时间 = 消息存储时间 + 延时等级时间。消息存储时间指的是消息被发送到 [Broker](http://broker/) 时的时间戳。
4. 将消息索引写入到 [SCHEDULE_TOPIC_XXXX](http://schedule_topic_xxxx/) 主题下相应的 [consumequeue](http://consumequeue/) 中。
> SCHEDULE_TOPIC_XXXX 目录中各个延时等级 Queue 中的消息是如何排序的？
> 
> 是按照消息投递时间排序的。一个 Broker 中同一等级的所有延时消息会被写入到 consumequeue 目录中 SCHEDULE_TOPIC_XXXX 目录下相同 Queue 中。即一个 Queue 中消息投递时间的延迟等级时间是相同的。那么投递时间就取决于于消息存储时间了。即按照消息被发送到 Broker 的时间进行排序的。

## **3.2 投递延时消息**

Broker 内部有⼀个延迟消息服务类 [ScheuleMessageService](http://scheulemessageservice/)，其会消费 [SCHEDULE_TOPIC_XXXX](http://schedule_topic_xxxx/) 中的消息，即按照每条消息的投递时间，将延时消息投递到⽬标 [Topic](http://topic/) 中。不过，在投递之前会从 [commitlog](http://commitlog/) 中将原来写入的消息再次读出，并将其原来的延时等级设置为 0，即原消息变为了一条不延迟的普通消息。然后再次将消息投递到目标 Topic 中。

> ScheuleMessageService 在 Broker 启动时，会创建并启动一个定时器 Timer，用于执行相应的定时任务。
> 
> 系统会根据延时等级的个数，定义相应数量的 TimerTask，每个 TimerTask 负责一个延迟等级消息的消费与投递。每个 TimerTask 都会检测相应 Queue 队列的第一条消息是否到期。如果第一条消息未到期，则后面的所有消息更不会到期（消息是按照投递时间排序的）。如果第一条消息到期了，则将该消息投递到目标 Topic，即消费该消息。

## **3.3 将消息重新写入 CommitLog**

延迟消息服务类 [ScheuleMessageService](http://scheulemessageservice/) 将延迟消息再次发送给了 [commitlog](http://commitlog/)，并再次形成新的消息索引条目，分发到相应 [Queue](http://queue/)。

这其实就是一次普通消息发送。只不过这次的消息 Producer 是延迟消息服务类 [ScheuleMessageService](http://scheulemessageservice/)。

# **04 代码示例**

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




大家好，我是**华仔**, 又跟大家见面了。

这里我会梳理并更新一部分关于 **RocketMQ** 基础功能操作的文章，今天是第一篇，我们先来讲解下 **RocketMQ 普通消息发送的模式分类以及具体的代码示例**，下面是正文。

# **01 消息分类**

Producer 端对于消息的发送方式也有多种选择，不同的方式会产生不同的系统效果。

关于普通消息的分类主要有三种：「**同步发送**」、「**异步发送**」、「**单向发送**」。

## **1.1 同步发送消息**

同步发送消息主要是指 Producer 端发出⼀条消息后，会在收到 RocketMQ 返回的 ACK 之后才发下⼀条消息。

> 该方式的消息可靠性最高，但消息发送效率最低。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292351747.png)

## **1.2 异步发送消息**

异步发送消息主要是指 Producer 端发出消息后无需等待 RocketMQ 返回 ACK，直接发送下⼀条消息。

> 该方式的消息可靠性可以得到保障，消息发送效率也还可以。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292352109.png)

## **1.3 单向发送消息**

单向发送消息主要是指 Producer 端仅负责发送消息，不需要等待也不处理 RocketMQ 返回的 ACK。

> 该方式的消息发送效率最高，但消息可靠性最差。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410292352351.png)

# **02 代码示例**

## **2.1 创建工程**

先新创建一个 Maven 的 Java 工程 rocketmq-client-test。

## **2.2 导入依赖**

接着导入 rocketmq 的 client 依赖。
```xml
<properties>  
    <project.build.sourceEncoding>UTF8</project.build.sourceEncoding>  
    <maven.compiler.source>1.8</maven.compiler.source>  
    <maven.compiler.target>1.8</maven.compiler.target>  
</properties>  
<dependencies>  
    <dependency>  
         <groupId>org.apache.rocketmq</groupId>  
         <artifactId>rocketmq-client</artifactId>  
         <version>4.8.0</version>  
    </dependency>  
</dependencies>
```

## **2.3 同步发送生产者**

```java
// 消息发送的状态  
public enum SendStatus {
	// 发送成功  
    SEND_OK,
    // 刷盘超时。当Broker设置的刷盘策略为同步刷盘时才可能出现这种异常状态。异步刷盘不会出现
    FLUSH_DISK_TIMEOUT,
    // Slave同步超时。当Broker集群设置的Master-Slave的复制方式为同步复制时才可能出现这种异常状态。异步复制不会出现
    FLUSH_SLAVE_TIMEOUT,
    // 没有可用的Slave。当Broker集群设置为Master-Slave的复制方式为同步复制时才可能出现这种异常状态。异步复制不会出现
    SLAVE_NOT_AVAILABLE,
}  
  
public class SyncProducer {  
    public static void main(String[] args) throws Exception {  
        // 创建一个producer，参数为Producer Group名称  
        DefaultMQProducer producer = new DefaultMQProducer("test-huazai");  
        // 指定nameServer地址  
        producer.setNamesrvAddr("localhost:9876");  
        // 设置当发送失败时重试发送的次数，默认为2次  
        producer.setRetryTimesWhenSendFailed(3);  
        // 设置发送超时时限为5s，默认3s  
        producer.setSendMsgTimeout(5000);  
        // 开启生产者  
        producer.start();  
          
        // 生产并发送100条消息  
        for (int i = 0; i < 100; i++) {  
            byte[] body = ("Hi," + i).getBytes();  
            Message msg = new Message("someTopic", "someTag", body);  
            // 为消息指定key  
            msg.setKeys("key-" + i);  
            // 发送消息  
            SendResult sendResult = producer.send(msg);  
            System.out.println(sendResult);  
        }  
        // 关闭producer  
        producer.shutdown();  
    }  
}
```

## **2.4 异步发送生产者**

```java
public class AsyncProducer {  
    public static void main(String[] args) throws Exception {  
        DefaultMQProducer producer = new DefaultMQProducer("test-huazai");  
        producer.setNamesrvAddr("localhost:9876");  
        // 指定异步发送失败后不进行重试发送  
        producer.setRetryTimesWhenSendAsyncFailed(0);  
        // 指定新创建的Topic的Queue数量为2，默认为4  
        producer.setDefaultTopicQueueNums(2);  
        // 开启生产者  
        producer.start();  
          
        // 发送消息  
        for (int i = 0; i < 100; i++) {  
            byte[] body = ("Hi," + i).getBytes();  
            try {  
                Message msg = new Message("myTopicA", "myTag", body);  
                // 异步发送 指定回调  
                producer.send(msg, new SendCallback() {  
                    // 当producer接收到MQ发送来的ACK后就会触发该回调方法的执行  
                    @Override  
                    public void onSuccess(SendResult sendResult) {  
                        System.out.println(sendResult);  
                    }  
                    @Override  
                    public void onException(Throwable e) {  
                        e.printStackTrace();  
                    }  
                })  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
        } // end-for  
        // sleep一会儿  
        // 由于采用的是异步发送，所以若这里不sleep，  
        // 则消息还未发送就会将producer给关闭，报错  
        TimeUnit.SECONDS.sleep(3);  
        producer.shutdown();  
    }  
}
```

## **2.5 单向发送生产者**

```java
public class OnewayProducer {  
    public static void main(String[] args) throws Exception{  
        DefaultMQProducer producer = new DefaultMQProducer("test-huazai");  
        producer.setNamesrvAddr("localhost:9876");  
        // 开启生产者  
        producer.start();  
        for (int i = 0; i < 10; i++) {  
            byte[] body = ("Hi," + i).getBytes();  
            Message msg = new Message("single", "someTag", body);  
            // 单向发送  
            producer.sendOneway(msg);  
        }  
        producer.shutdown();  
        System.out.println("producer shutdown");  
    }  
}
```

## **2.6 定义单消费者**

```java
public class SomeConsumer {  
    public static void main(String[] args) throws MQClientException {  
        // 定义一个pull消费者  
        // DefaultLitePullConsumer consumer = new  
        DefaultLitePullConsumer("test-huazai-cg");  
        // 定义一个push消费者  
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("test-huazai-cg");  
        // 指定nameServer  
        consumer.setNamesrvAddr("localhost:9876");  
        // 指定从第一条消息开始消费  
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET  
        );  
        // 指定消费topic与tag  
        consumer.subscribe("someTopic", "*");  
        // 指定采用“广播模式”进行消费，默认为“集群模式”  
        // consumer.setMessageModel(MessageModel.BROADCASTING);        // 注册消息监听器  
        consumer.registerMessageListener(new  
                                                 MessageListenerConcurrently() {  
                                                     // 一旦broker中有了其订阅的消息就会触发该方法的执行，  
                                                     // 其返回值为当前consumer消费的状态  
                                                     @Override  
                                                     public ConsumeConcurrentlyStatus  
                                                     consumeMessage(List<MessageExt> msgs,  
                                                                    ConsumeConcurrentlyContext context) {  
                                                         // 逐条消费消息  
                                                         for (MessageExt msg : msgs) {  
                                                             System.out.println(msg);  
                                                         }  
                                                         // 返回消费状态：消费成功  
                                                         return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;  
                                                     }  
                                                 });  
        // 开启消费者消费  
        consumer.start();  
        System.out.println("Consumer Started");  
    }  
}
```













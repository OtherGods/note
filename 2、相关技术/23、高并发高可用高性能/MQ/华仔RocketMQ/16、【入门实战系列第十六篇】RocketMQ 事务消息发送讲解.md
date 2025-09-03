大家好，我是**华仔**, 又跟大家见面了。

这里我会梳理并更新一部分关于 **RocketMQ** 基础功能操作的文章，今天是第四篇，我们先来讲解下 **RocketMQ 事务消息发送以及具体的代码示例**，下面是正文。

# **01 需求引入**

这里的一个需求场景是：招行用户 A 向建行用户 B 转账1万元，我们可以使用同步消息来处理该需求场景：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411031403918.png)

整个同步消息流程如下：
1. 招行系统发送一个给用户 B 增款1万元的同步消息 M 给 Broker。
2. 消息被 Broker 成功接收后，向招行系统发送成功 ACK。
3. 招行系统收到成功 ACK 后从用户 A 中扣款 1 万元。
4. 建行系统从 Broker 中获取到消息 M。
5. 建行系统消费消息 M，即向用户 B 中增加 1 万元。

> 但是这里面是有问题的：如果上面第 3 步中的扣款操作失败，但消息已经成功发送到了 Broker。对于 MQ 来
> 
> 说，只要消息写入成功，那么这个消息就可以被消费。此时建行系统中用户 B 增加了1万元，出现了数据不一致问题。

# **02 如何解决**

其实上面方案中问题的解决思路就是让第 1、2、3 步具有「**原子性**」，要么「**全部成功**」，要么「**全部失败**」。即消息发送成功后，必须要保证扣款成功。如果扣款失败，则「**回滚发送成功**」的消息。

而找个思路需要使用事务消息，也就是我们经常提到的「**分布式事务解决方案**」。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411031417225.png)

使用事务消息来处理该需求场景：
1. 事务管理器 TM 向事务协调器 TC 发起指令，开启全局事务。
2. 招行系统发一个给 B 增款 1 万元的事务消息 M 给 TC。
3. TC 会向 Broker 发送半事务消息 prepareHalf，将消息 M 预提交到 Broker。此时的建行系统是看不到 Broker 中的消息 M的。
4. Broker 会将预提交执行结果 Report 给 TC。
5. 如果预提交失败，则 TC 会向 TM 上报预提交失败的响应，全局事务结束；如果预提交成功，TC 会调用招行系统的回调操作，去完成招行用户 A 的预扣款 1 万元的操作。
6. 招行系统会向 TC 发送预扣款执行结果，即本地事务的执行状态。
7. TC 收到预扣款执行结果后，会将结果上报给 TM。

> // 预扣款执行结果存在三种可能性，描述本地事务执行状态
> public enum LocalTransactionState { 
>    // 本地事务执行成功
>    COMMIT_MESSAGE, 
>    // 本地事务执行失败
>    ROLLBACK_MESSAGE, 
>    // 不确定，表示需要进行回查以确定本地事务的执行结果
>    UNKNOW, 
> }
8. TM 会根据上报结果向 TC 发出不同的确认指令。
	1. 如果预扣款成功（本地事务状态为 [COMMIT_MESSAGE](http://commit_message/)），则 TM 向 TC 发送 [Global Commit](http://global%20commit/) 指令。
	2. 如果预扣款失败（本地事务状态为 [ROLLBACK_MESSAGE](http://rollback_message/)），则 TM 向 TC 发送 [Global Rollback](http://global%20rollback/) 指令。
	3. 如果现未知状态（本地事务状态为 [UNKNOW](http://unknow/)），则会触发招行系统的本地事务状态回查操作。回查操作会将回查结果，即[COMMIT_MESSAGE](http://commit_message/) 或 [ROLLBACK_MESSAGE](http://rollback_message%20/) Report 给 TC。TC 将结果上报给 TM，TM 会再向 TC 发送最终确认指令[Global Commit](http://global%20commit/) 或 [Global Rollback](http://global%20rollback/)。
9. TC 在接收到指令后会向 Broker 与招行系统发出确认指令。
10. TC 接收的如果是 [Global Commit](http://global%20commit/) 指令，则向 Broker 与招行系统发送 [Branch Commit](http://branch%20commit/) 指令。此时 Broker 中的消息 M 才可被建行系统看到；此时的招行用户 A 中的扣款操作才真正被确认。
11. TC 接收到的如果是 [Global Rollback](http://global%20rollback/) 指令，则向 Broker 与招行系统发送 [Branch Rollback](http://branch%20rollback/) 指令。此时 Broker 中的消息 M将被撤销；招行用户 A 中的扣款操作将被回滚。
> 以上方案就是为了确保消息投递与扣款操作能够在一个事务中，要成功都成功，有一个失败，则全部回滚。但是该方案并不是一个典型的 XA 模式。因为 XA 模式中的分支事务是异步的，而事务消息方案中的消息预提交与预扣款操作间是同步的。

# **03 基础知识**

## **3.1 分布式事务**

对于分布式事务，通俗地说就是，一次操作由若干分支操作组成，这些分支操作分属不同应用，分布在不同服务器上。分布式事务需要保证这些分支操作要么全部成功，要么全部失败。分布式事务与普通事务一样，就是为了保证操作结果的一致性。

## **3.2 事务消息**

RocketMQ 提供了类似 X/Open XA 的分布式事务功能，通过事务消息能达到分布式事务的最终一致。XA 是一种分布式事务解决方案，一种分布式事务处理模式。

## **3.3 半事务消息**

暂不能投递的消息，发送方已经成功地将消息发送到了 Broker，但是 Broker 未收到最终确认指令，此时该消息被标记成「**暂不能投递**」状态，即不能被消费者看到，处于该种状态下的消息即半事务消息。

## **3.4 本地事务状态**

Producer 回调操作执行的结果为本地事务状态，其会发送给 TC，而 TC 会再发送给 TM。TM 会根据 TC 发送来的本地事务状态来决定全局事务确认指令。

## **3.5 消息回查**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411031625214.png)

消息回查，即重新查询本地事务的执行状态。此处就是重新到DB中查看预扣款操作是否执行成功。

## **3.6 RocketMQ 消息回查设置**

关于消息回查，有三个常见的属性设置。它们都在broker加载的配置文件中设置，例如：
1. [transactionTimeout=20](http://transactiontimeout=20/)，指定TM在20秒内应将最终确认状态发送给TC，否则引发消息回查。默认为60秒。
2. [transactionCheckMax=5](http://transactioncheckmax=5/)，指定最多回查5次，超过后将丢弃消息并记录错误日志。默认15次。
3. [transactionCheckInterval=10](http://transactioncheckinterval=10/)，指定设置的多次消息回查的时间间隔为10秒。默认为60秒。
# **04 代码示例**

## **4.1 定义招行事务监听器**

```java
public class CMBTransactionListener implements TransactionListener {  
    // 回调操作方法  
    // 消息预提交成功就会触发该方法的执行，用于完成本地事务  
    @Override  
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {  
        System.out.println("预提交消息成功：" + msg);  
        // 假设接收到TAGA的消息就表示扣款操作成功，TAGB的消息表示扣款失败，TAGC表示扣款结果不清楚，需要执行消息回查  
        if (StringUtils.equals("TAGA", msg.getTags())) {  
            return LocalTransactionState.COMMIT_MESSAGE;  
        } else if (StringUtils.equals("TAGB", msg.getTags())) {  
            return LocalTransactionState.ROLLBACK_MESSAGE;  
        } else if (StringUtils.equals("TAGC", msg.getTags())) {  
            return LocalTransactionState.UNKNOW;  
        }  
        return LocalTransactionState.UNKNOW;  
    }  
      
    // 消息回查方法  
    // 引发消息回查的原因最常见的有两个：  
    // 1)回调操作返回UNKNWON  
    // 2)TC没有接收到TM的最终全局事务确认指令  
    @Override  
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {  
        System.out.println("执行消息回查" + msg.getTags());  
        return LocalTransactionState.COMMIT_MESSAGE;  
    }  
}
```

## **4.2 定义事务消息生产者**

```java
public class TransactionProducer {  
    public static void main(String[] args) throws Exception {  
        TransactionMQProducer producer = new TransactionMQProducer("tpg");  
        producer.setNamesrvAddr("localhost:9876");  
        /**  
         * 定义一个线程池  
         * @param corePoolSize 线程池中核心线程数量  
         * @param maximumPoolSize 线程池中最多线程数  
         * @param keepAliveTime 这是一个时间。当线程池中线程数量大于核心线程数量是，多余空闲线程的存活时长  
         * @param unit 时间单位  
         * @param workQueue 临时存放任务的队列，其参数就是队列的长度  
         * @param threadFactory 线程工厂  
         */  
        ExecutorService executorService = new ThreadPoolExecutor(2, 5, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(2000), new ThreadFactory() {  
            @Override  
            public Thread newThread(Runnable r) {  
                Thread thread = new Thread(r);  
                thread.setName("client-transaction-msg-check-thread");  
                return thread;  
            }  
        });  
          
        // 为生产者指定一个线程池  
        producer.setExecutorService(executorService);  
        // 为生产者添加事务监听器  
        producer.setTransactionListener(new CMBTransactionListener());  
        producer.start();  
        String[] tags = {"TAGA","TAGB","TAGC"};  
        for (int i = 0; i < 3; i++) {  
            byte[] body = ("Hi," + i).getBytes();  
            Message msg = new Message("TTopic", tags[i], body);  
            // 发送事务消息  
            // 第二个参数用于指定在执行本地事务时要使用的业务参数  
            SendResult sendResult = producer.sendMessageInTransaction(msg,null);  
            System.out.println("发送结果为：" +  
                    sendResult.getSendStatus());  
        }  
    }  
}
```

## **4.3 定义消费者**

```java
// 直接使用普通消息的 SomeConsumer 作为消费者即可
public class SomeConsumer  
{  
    public static void main(String[] args) throws MQClientException  
    {  
        // 定义一个pull消费者  
        // DefaultLitePullConsumer consumer = new DefaultLitePullConsumer("cg");  
        // 定义一个push消费者  
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("cg");  
        // 指定nameServer  
        consumer.setNamesrvAddr("localhost:9876");  
        // 指定从第一条消息开始消费  
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);  
        // 指定消费topic与tag  
        consumer.subscribe("TTopic", "*");  
        // 指定采用“广播模式”进行消费，默认为“集群模式”  
        // consumer.setMessageModel(MessageModel.BROADCASTING);        // 注册消息监听器  
        consumer.registerMessageListener(new MessageListenerConcurrently()  
        {  
            // 一旦broker中有了其订阅的消息就会触发该方法的执行，  
            // 其返回值为当前consumer消费的状态  
            @Override  
            public ConsumeConcurrentlyStatus  
            consumeMessage(List<MessageExt> msgs,  
                           ConsumeConcurrentlyContext context)  
            {  
                // 逐条消费消息  
                for (MessageExt msg : msgs)  
                {  
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










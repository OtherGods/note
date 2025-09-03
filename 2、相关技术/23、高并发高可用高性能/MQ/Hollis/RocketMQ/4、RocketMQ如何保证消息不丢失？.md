# 典型回答

**==RocketMQ的消息想要确保不丢失，需要生产者、消费者以及Broker的共同努力，缺一不可。==**

## 生产端

首先在生产者端，消息的发送分为同步、异步、单向发送（单向发送不保证成功，不建议使用）：
1. **在==同步发送==消息的情况下，消息的发送会同步阻塞等待Broker返回结果**，在Broker确认收到消息之后，生产者才会拿到SendResult。如果这个过程中发生异常，那么就说明消息发送可能失败了，就需要生产者进行**重新发送消息**
```java
try {
    SendResult sendResult = producer.send(msg);
    // 同步发送消息，只要不抛异常就是成功。
    if (sendResult != null) {
        //重试逻辑
    }
}
catch (Exception e) {
    //重试逻辑
}
```

2. **==异步发送==** 的时候，会有**成功和失败的回调**，这还是**需要在失败回调中处理重试确保成功**；需要生产者重写`SendCallback`的`onSuccess`和`onException`方法，用于给`Broker`进行回调。在方法中实现消息的确认或者重新发送。
```java
// 异步发送消息, 发送结果通过callback返回给客户端。
producer.sendAsync(msg, new SendCallback() {
    @Override
    public void onSuccess(final SendResult sendResult) {
        // 消息发送成功。
        System.out.println("send message success. topic=" + sendResult.getTopic() + ", msgId=" + sendResult.getMessageId());
    }

    @Override
    public void onException(OnExceptionContext context) {
        // 消息发送失败
        //重试逻辑
    }
});
```

## Broker

生产端同步发送消息给Broker后，**Broker并不会立即把消息存储到磁盘上，而是先存储到内存中，内存存储成功之后**，就返回给确认结果给生产者了。然后再通过 **==异步刷盘==** 的方式将内存中的数据存储到磁盘上。但是这个过程中，如果机器挂了，那么就可能会导致数据丢失。

如果想要保证消息不丢失，可以**将消息保存机制修改为==同步刷盘==**，这样，Broker会在同步请求中把数据保存在磁盘上，确保保存成功后再返回确认结果给生产者。
```text
## 默认情况为 ASYNC_FLUSH 
flushDiskType = SYNC_FLUSH
```

1. **==异步刷盘==**（默认）
2. **==同步刷盘==**

为了保证消息不丢失，`RocketMQ`肯定要通过集群方式进行部署`Broker`，`Broker` 通常采用**一主多从部署**方式，并且采用**主从同步的方式做数据复制**。

当主`Broker`宕机时，`从Broker`会接管`主Broker`的工作，**保证消息不丢失**。同时，`RocketMQ`的`Broker`还可以配置多个实例，消息会在多个`Broker`之间进行冗余备份，从而保证数据的可靠性。
1. 异步复制（默认）
2. 同步复制

**默认方式下，`Broker`在接收消息后，写入 `master` 成功，就可以返回确认响应给生产者了**，接着 **==消息将会异步复制到 `slave` 节点==**。但是如果这个过程中，`Master`的磁盘损坏了。那就会导致数据丢失了。

如果想要解决这个问题，可以配置 **==同步复制==** 的方式，即`Master`在将数据同步到`Slave`节点后，再返回给生产者确认结果。
```text
## 默认为 ASYNC_MASTER
brokerRole=SYNC_MASTER
```

1. **==异步复制==**（默认）
2. **==同步复制==**

## 消费者端

在消费者端，需要确保在消息拉取并消费成功之后再给`Broker`返回`ACK`，就可以保证消息不丢失了，如果这个过程中`Broker`一直没收到`ACK`，那么就可以重试。

所以，在消费者的代码中，一定要在业务逻辑的最后一步 `return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;` 当然，也可以先把数据保存在数据库中，就返回，然后自己再慢慢处理。

但是，需要注意的是`RocketMQ`和`Kafka`一样，**==只能最大限度的保证消息不丢失，但是没办法做到100%保证不丢失==**。原理类似：
[为什么Kafka没办法100%保证消息不丢失？](2、相关技术/23、高并发高可用高性能/MQ/Hollis/Kafka/为什么Kafka没办法100%保证消息不丢失？.md)

大家可以在项目中看到，我们有NewBuyBatchMsgListener和NewBuyMsgListener这两个用来监听newBuy的消息的监听器。为啥会有两个呢？

其实最开始只有NewBuyMsgListener这一个，他是通过SpringCloud Stream的方式配置的监听，主要是通过以下方式定义newBuy的这个topic的消息的发送和监听配置

```yml
spring:
  cloud:
    function:
      definition: newBuy
    stream:
      rocketmq:
        bindings:
          newBuy-out-0:
            producer:
              producerType: Trans
              transactionListener: inventoryDecreaseTransactionListener
      bindings:
        newBuy-out-0:
          content-type: application/json
          destination: new-buy-topic
          group: trade-group
          binder: rocketmq
        newBuy-in-0:
          content-type: application/json
          destination: new-buy-topic
          group: trade-group
          binder: rocketmq
```

然后在NewBuyMsgListener中定义一个名字为newBuy的Bean（这个bean name一定要和bindings中配置的xxx-in-0中的xxx保持一致）
```java
@Component
@Slf4j
@ConditionalOnProperty(value = "rocketmq.broker.check", havingValue = "false", matchIfMissing = true)
public class NewBuyMsgListener {
﻿
    @Autowired
    private OrderFacadeService orderFacadeService;
﻿
    @Autowired
    private OrderReadService orderReadService;
﻿
    @Autowired
    private InventoryFacadeService inventoryFacadeService;
﻿
    @Bean
    Consumer<Message<MessageBody>> newBuy() {
        return msg -> {
            String messageId = msg.getHeaders().get("ROCKET_MQ_MESSAGE_ID", String.class);
            String tag = msg.getHeaders().get("ROCKET_TAGS", String.class);
            OrderCreateRequest orderCreateRequest = JSON.parseObject(msg.getPayload().getBody(), OrderCreateRequest.class);
            log.info("Received NewBuy Message messageId:{},orderCreateRequest:{}，tag:{}", messageId, orderCreateRequest, tag);
            doNewBuyExecute(orderCreateRequest);
        };
    }
﻿
}
```

这个监听器用着没啥问题，挺方便的，但是他有个问题，就是他无法批量消费消息，只能单条消费，但是单条消费速度比较慢，会导致消息堆积，于是我们考虑搞一个批量消费。

但是很遗憾，Spring Cloud Stream对RocketMQ的支持中，不包含对批量消费的支持。所以没办法，就需要用RocketMQ原生的方式实现，那就是我们的NewBuyBatchMsgListener了。他的监听配置主要在：

```yml
rocketmq:
  consumer:
    group: trade-group
    # 一次拉取消息最大值，注意是拉取消息的最大值而非消费最大值
    pull-batch-size: 64
    consume-message-batch-max-size: 32
```

然后在bean上也要有@RocketMQMessageListener(topic = "new-buy-topic", consumerGroup = "trade-group")这样的配置，指明监听的topic。

然后重写其中的prepareStart方法，在其中设置批量消费的参数：

```java
consumer.setPullInterval(1000);
consumer.setConsumeMessageBatchMaxSize(128);
consumer.setPullBatchSize(64);
```

紧接着就可以用MessageListenerConcurrently来批量处理消息了。

批量处理代码如下：

```java
@Override
    public void prepareStart(DefaultMQPushConsumer consumer) {
        consumer.setPullInterval(1000);
        consumer.setConsumeMessageBatchMaxSize(128);
        consumer.setPullBatchSize(64);
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            log.info("NewBuyBatchMsgListener receive message size: {}", msgs.size());
﻿
            CompletionService<Boolean> completionService = new ExecutorCompletionService<>(executor);
            List<Future<Boolean>> futures = new ArrayList<>();
﻿
            // 1. 提交所有任务
            msgs.forEach(messageExt -> {
                Callable<Boolean> task = () -> {
                    try {
                        OrderCreateRequest orderCreateRequest = JSON.parseObject(JSON.parseObject(messageExt.getBody()).getString("body"), OrderCreateRequest.class);
                        return doNewBuyExecute(orderCreateRequest);
                    } catch (Exception e) {
                        log.error("Task failed", e);
                        return false; // 标记失败
                    }
                };
                futures.add(completionService.submit(task));
            });
﻿
            // 2. 检查结果
            boolean allSuccess = true;
            try {
                for (int i = 0; i < msgs.size(); i++) {
                    Future<Boolean> future = completionService.take();
                    if (!future.get()) { // 发现一个失败立即终止
                        allSuccess = false;
                        break;
                    }
                }
            } catch (Exception e) {
                allSuccess = false;
            }
﻿
            // 4. 根据结果返回消费状态
            return allSuccess ? ConsumeConcurrentlyStatus.CONSUME_SUCCESS
                    : ConsumeConcurrentlyStatus.RECONSUME_LATER;
        });
    }
```

主要思想就是借助CompletionService并发执行任务，并且在所有任务都成功的时候返回CONSUME_SUCCESS，否则如果有任何一个消息失败了，都返回RECONSUME_LATER，让MQ重新投递，重新消费。

这两个监听器，哪个生效是如何控制的呢？

通过这个参数

```
rocketmq.broker.check=true
```

批量消费MQ的newBuy消息（NewBuyBatchMsgListener），在rocketmq.broker.check=true （stream.yml） 的时候会生效。

单条消费MQ的newBuy消息（NewBuyMsgListener），在rocketmq.broker.check=fasle （stream.yml） 的时候会生效
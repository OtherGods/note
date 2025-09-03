以下是我们的用户下单功能的顺序图 ：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202502242333849.png)

从图中可以看到，在我们的项目的下单场景中，先是通过Redis 进行库存的扣减，然后开始创建订单，并且订单在创建成功之后，状态为INIT。

这时候，我们会在一个异步链路上进行订单的确认，所谓订单确认，主要做2件事：
1、将在 Redis 中扣减的库存，持久化到数据库中。
- 调用collectionService 的 trySale方法
2、将订单状态从 INIT 推进到 CONFIRM
- 调用orderService 的 confirm 方法

那么，这个订单的确认功能，其实需要满足两点要求。即解耦+异步。

**解耦**：订单确认是个自动的过程，他是在下单过程中，即订单的创建完成后的一个后置操作，我们需要把他从订单创建的方法中解耦出来，因为他严格来说并不是订单创建的一部分。至少在 OrderManageServie 中，create 和 confirm 是两个完全不相关的方法。

**异步：** 这个比较容易理解，就是他并不在订单创建方法的主流程中，不要阻塞主流程的执行，所以需要做异步处理。

能起到解耦+异步的，那就是事件机制了，常见的事件机制有以下几个方案：

**1、通过 RocketMQ 发一个消息，然后再接收这个消息进行处理。**
- 好处是基于 MQ 进行处理，可以做到更好的削峰填谷
- 缺点是 MQ 投递会存在延迟

**2、通过事件机制，如 SpringEvent 进行应用内的异步处理。**
- 好处是延迟的问题要比 MQ 好很多，并且可以配合线程池提升性能
- 缺点1：无法做到削峰填谷，可能会给数据库的热点扣减带来压力
- 缺点2：任务没有持久化，应用重启后任务就没了

一般来说，用第一种方案的比较多，尤其是在秒杀的这个场景中，基于 MQ 来做削峰填谷，利用 MQ 的队列机制来减少数据库的压力。但是我们项目中并没有直接采用这个方案，而是采用了方案2

主要原因是我们的做数据库扣减这里，依赖了阿里云上 DMS 提供的 Inventory Hint 的机制，说白了就是这里不用 MQ 做缓冲数据库也能扛得住，所以就干脆不用了。
[4、基于InventoryHint实现库存的热点扣减](2、相关技术/24、项目/3、数藏项目/8、最佳实践/2、秒杀（热点扣减）/4、基于InventoryHint实现库存的热点扣减.md)

所以，就采用了方案2这样主要以 SpringEvent 做异步处理，但是，方案2有个致命的缺点，那就是没办法做持久化，一旦运行过程中应用重启了，这个任务就没了。所以我们在这个方案的基础上引入了 XXL-JOB进行补偿。

所以，整个方案是基于SpringEvent和XXL-JOB实现订单确认的自动推进，其实分为两步：
- 第一步：通过springEvent的事件进行驱动，把订单确认的流程丢到异步线程池中进行
- 第二步：通过XXL-JOB查找未处理确认的订单，通过补偿任务的形式把这些订单进行推进，从而保证最终订单确认的处理完成

### 创建event

首先我们定义一个事件，并且这个事件需要实现Spring 的ApplicationEvent类：
```java
public class OrderCreateEvent extends ApplicationEvent {  
    public OrderCreateEvent(TradeOrder tradeOrder) {
        super(tradeOrder);  
    }  
}
```

### 在业务逻辑中发布event

然后需要在订单创建结束之后，把这个事件发出来：
```java
/**  
 * 订单创建  
 *  
 * @param request 
 * @return 
 */
@Transactional(rollbackFor = Exception.class)  
public OrderResponse create(OrderCreateRequest request) {  
    TradeOrder existOrder = orderMapper.selectByIdentifier(request.getIdentifier(), request.getBuyerId());  
    if (existOrder != null) {  
        return new OrderResponse.OrderResponseBuilder().orderId(existOrder.getOrderId()).buildSuccess();  
    }  
      
    TradeOrder tradeOrder = TradeOrder.createOrder(request);  
      
    boolean result = save(tradeOrder);  
    Assert.isTrue(result, () -> new BizException(RepoErrorCode.INSERT_FAILED));  
      
    TradeOrderStream orderStream = new TradeOrderStream(tradeOrder, request.getOrderEvent(), request.getIdentifier());  
    result = orderStreamMapper.insert(orderStream) == 1;  
    Assert.isTrue(result, () -> new BizException(RepoErrorCode.INSERT_FAILED));  
      
    applicationContext.publishEvent(new OrderCreateEvent(tradeOrder));  
    return new OrderResponse.OrderResponseBuilder().orderId(tradeOrder.getOrderId()).buildSuccess();  
}
```

这里通过applicationContext.publishEvent()进行事件的发布

### 定义事件监听器

同时我们定义一个事件的监听器，用来处理这个事件
```java
@Component  
public class OrderEventListener {  
      
    @Autowired  
    private OrderFacadeService orderFacadeService;  
      
    @EventListener(OrderCreateEvent.class)  
    @Async("orderListenExecutor")  
    public void onApplicationEvent(OrderCreateEvent event) {  
          
        TradeOrder tradeOrder = (TradeOrder) event.getSource();  
        OrderConfirmRequest confirmRequest = new OrderConfirmRequest();  
        confirmRequest.setOperator(UserType.PLATFORM.name());  
        confirmRequest.setOperatorType(UserType.PLATFORM);  
        confirmRequest.setOrderId(tradeOrder.getOrderId());  
        confirmRequest.setIdentifier(tradeOrder.getIdentifier());  
        confirmRequest.setOperateTime(new Date());  
          
        orderFacadeService.confirm(confirmRequest);  
    }  
}
```

@EventListener(OrderCreateEvent.class)表示接收并处理OrderCreateEvent事件

@Async("orderListenExecutor") 表示将使用名称为orderListenExecutor的自定义线程池来异步执行该方法

线程池定义如下：
```java
@Configuration  
@EnableAsync  
public class OrderListenerConfig {  
    @Bean("orderListenExecutor")  
    public Executor orderListenExecutor() {  
          
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()  
                .setNameFormat("orderListener-%d").build();  
          
        ExecutorService executorService = new ThreadPoolExecutor(10, 20,  
                0L, TimeUnit.MILLISECONDS,  
                new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());  
          
        return executorService;  
    }  
}
```

完成以上配置后，订单创建过程中就会发布一个OrderCreateEvent，这个事件会被OrderEventListener给订阅到，然后基于我们自定义的线程池进行异步处理。

### 配置XXL-JOB定时任务

为了做失败的补偿，我们定义一个 xxl-job 的回调任务，这个任务也是借助了 XXL-JOB 的分片任务，以及生产者消费者模式来实现的，主要就是为了提升性能。

原理和下面这篇文章中讲的是一样的：
[2、基于生产者消费者+线程池实现并发关闭订单](2、相关技术/24、项目/3、数藏项目/8、最佳实践/3、关单（高并发扫表）/2、基于生产者消费者+线程池实现并发关闭订单.md)

```java
private final ForkJoinPool forkJoinPool = new ForkJoinPool(10);  
  
@XxlJob("orderConfirmExecute")  
@Deprecated  
public ReturnT<String> orderConfirmExecute() {  
      
    int shardIndex = XxlJobHelper.getShardIndex();  
    int shardTotal = XxlJobHelper.getShardTotal();  
      
    LOG.info("orderConfirmExecute start to execute , shardIndex is {} , shardTotal is {}", shardIndex, shardTotal);  
      
    List<String> buyerIdTailNumberList = new ArrayList<>();  
    for (int i = 0; i < MAX_TAIL_NUMBER; i++) {  
        if (i % shardTotal == shardIndex) {  
            buyerIdTailNumberList.add(StringUtils.leftPad(String.valueOf(i), 2, "0"));  
        }  
    }  
      
    try {  
        buyerIdTailNumberList.forEach(buyerIdTailNumber -> {  
            int currentPage = 1;  
            Page<TradeOrder> page = orderReadService.pageQueryNeedConfirmOrders(currentPage, PAGE_SIZE, buyerIdTailNumber);  
            orderConfirmBlockingQueue.addAll(page.getRecords());  
            forkJoinPool.execute(this::executeConfirm);  
              
            while (page.hasNext()) {  
                currentPage++;  
                page = orderReadService.pageQueryNeedConfirmOrders(currentPage, PAGE_SIZE, buyerIdTailNumber);  
                orderConfirmBlockingQueue.addAll(page.getRecords());  
            }  
        });  
    } finally {  
        orderConfirmBlockingQueue.add(POISON);  
        LOG.info("POISON added to blocking queue");  
    }  
      
    return ReturnT.SUCCESS;  
}  
  
private void executeConfirm() {  
    TradeOrder tradeOrder = null;  
    try {  
        while (true) {  
            tradeOrder = orderConfirmBlockingQueue.take();  
            if (tradeOrder == POISON) {  
                break;  
            }  
            executeConfirmSingle(tradeOrder);  
        }  
    } catch (InterruptedException e) {  
        LOG.error("executeConfirm failed", e);  
    }  
}  
  
private void executeConfirmSingle(TradeOrder tradeOrder) {  
    OrderConfirmRequest confirmRequest = new OrderConfirmRequest();  
    confirmRequest.setOperator(UserType.PLATFORM.name());  
    confirmRequest.setOperatorType(UserType.PLATFORM);  
    confirmRequest.setOrderId(tradeOrder.getOrderId());  
    confirmRequest.setIdentifier(tradeOrder.getIdentifier());  
    confirmRequest.setOperateTime(new Date());  
      
    orderFacadeService.confirm(confirmRequest);  
}
```

XxlJobHelper.getShardIndex()：获取当前执行器的分片索引。

XxlJobHelper.getShardTotal()：获取总的分片数。

buyerIdTailNumberList 用于存储当前分片所负责的买家ID尾号列表。

for 循环通过 i % shardTotal == shardIndex 判断当前的 i 是否属于当前分片，并将符合条件的 i 转为两位字符串添加到 buyerIdTailNumberList 中

处理核心逻辑：
- 通过买家id尾号 orderReadService.pageQueryNeedConfirmOrders 分页查询需要处理的待确认数据
- 把它放入阻塞队列中，然后把阻塞队列放入forkJoinPool中进行处理
- executeConfirm核心是从阻塞队列一直取并通过executeConfirmSingle处理，取到POISON表示已经没有待处理数据，然后结束

orderFacadeService confirm逻辑
```java
@Override  
public OrderResponse confirm(OrderConfirmRequest request) {  
      
    TradeOrder existOrder = orderReadService.getOrder(request.getOrderId());  
      
    CollectionSaleRequest collectionSaleRequest = new CollectionSaleRequest();  
    collectionSaleRequest.setUserId(existOrder.getBuyerId());  
    collectionSaleRequest.setCollectionId(Long.valueOf(existOrder.getGoodsId()));  
    collectionSaleRequest.setIdentifier(request.getIdentifier());  
    collectionSaleRequest.setQuantity((long) existOrder.getItemCount());  
    CollectionSaleResponse response = collectionFacadeService.trySale(collectionSaleRequest);  
      
    if (response.getSuccess()) {  
        return orderService.confirm(request);  
    }  
      
    return new OrderResponse.OrderResponseBuilder().orderId(existOrder.getOrderId()).buildFail(response.getResponseMessage(), response.getResponseCode());  
}
```


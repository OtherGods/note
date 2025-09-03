在订单完成支付后，我们会接受到外部支付渠道的支付成功消息。接收到消息之后需要做以下事情：
```java
 *     正常支付成功：
 *     1、查询订单状态
 *     2、推进订单状态到支付成功
 *     3、藏品库存真正扣减
 *     4、创建持有的藏品
 *     5、推进支付状态到支付成功
 *     6、持有的藏品上链
 *
 *     支付幂等成功：
 *      1、查询订单状态
 *      2、推进支付状态到支付成功
 *
 *     重复支付：
 *      1、查询订单状态
 *      2、创建退款单
 *      3、重试退款直到成功
```

三种情况，支付成功、重复支付、以及支付幂等。其中支付成功这个子流程，可以看到我们操作了很多模块，包括了订单、支付、藏品、链等等。

那么，我们如何保证这个过程中的一致性呢？这里我们用到了seata的分布式事务，用的是AT模式，之所以用这个模式，是因为他的侵入性低，性能也不差。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411182221813.png)

(关于 AT 的原理，以及和 Seata 中其他模式的区别，详见我的八股文）
[20、Seata的实现原理是什么](2、相关技术/21、分布式/Hollis/20、Seata的实现原理是什么.md)

在项目中接入了seata之后，其实使用seata的AT模式还是比较简单的，只需要在事务发起处加一个 `@GlobalTransactional` ，这样seata就会开启一个全局事务，就会通过和我们搭建好的seata服务端进行交互，进行整个分布式事务的协调。

Seata中包含了三个组件：
- **TC (Transaction Coordinator)**：事务协调器，负责管理全局事务的生命周期，包括开始事务、提交事务和回滚事务。
- **TM (Transaction Manager)**：事务管理器，定义事务的范围，负责开启和结束全局事务。
- **RM (Resource Manager)**：资源管理器，管理资源对象，如数据库连接，负责资源的注册与释放。

在我们这个场景中，TC就是我们搭建好的seata的服务端，和我们的代码没关系。TM就是我们的支付模块，RM就是我们的订单模块、藏品模块等。

用了Seata之后，一次分布式事务的大致流程如下：
1. TM（支付服务）在接收到支付成功的回调请求后，会先调用 **TC（Seata服务器）** 创建一个全局事务，并且从TC获取到他生成的XID。
2. TM（支付服务）开始通过Dubbo调用各个RM，调用过程中需要把XID同时传递过去。
3. **RM（订单、藏品等服务）** 通过其接收到的XID，将其所管理的资源且被该调用所使用到的资源在TC注册为一个事务分支(Branch Transaction)
4. 当该请求的调用链全部结束时，TM（支付服务）根据本次调用是否有失败的情况，如果所有调用都成功，则决议Commit，如果有超时或者失败，则决议Rollback。
5. **TM（支付服务）** 将事务的决议结果通知TC（Seata服务器），TC（Seata服务器）将协调所有RM（订单、藏品等服务）进行事务的二阶段动作，该回滚回滚，该提交提交。

回滚的实现是通过我们的数据库中创建的undo_log实现的。所以，如果我们多个模块，用的是不同数据库，都需要单独在数据库中创建一个undo_log，**这个undo_log和innodb的那个undo_log不是一回事，需要根据部署文档创建的**。

[9、Seata部署](2、相关技术/24、项目/3、数藏项目/2、中间件部署/9、Seata部署.md)

## 代码实现

```java
@GlobalTransactional(rollbackFor = Exception.class)  
public boolean paySuccess(PaySuccessEvent paySuccessEvent) {  
      
    PayOrder payOrder = payOrderService.queryByOrderId(paySuccessEvent.getPayOrderId());  
    if (payOrder.isPaid()) {  
        return true;  
    }  
      
    SingleResponse<TradeOrderVO> response = orderFacadeService.getTradeOrder(payOrder.getBizNo());  
    TradeOrderVO tradeOrderVO = response.getData();  
      
    OrderPayRequest orderPayRequest = getOrderPayRequest(paySuccessEvent, payOrder);  
    OrderResponse orderResponse = RemoteCallWrapper.call(req -> orderFacadeService.pay(req), orderPayRequest, "orderFacadeService.pay");  
    if (orderResponse.getResponseCode() != null && orderResponse.getResponseCode().equals(OrderErrorCode.ORDER_ALREADY_PAID.getCode())) {  
        doChargeBack(paySuccessEvent);  
        return true;  
    }  
      
    if (!orderResponse.getSuccess()) {  
        log.error("orderFacadeService.pay error, response = {}", JSON.toJSONString(orderResponse));  
        return false;  
    }  
      
    CollectionSaleRequest collectionSaleRequest = getCollectionSaleRequest(tradeOrderVO);  
    CollectionSaleResponse collectionSaleResponse = RemoteCallWrapper.call(req -> collectionFacadeService.confirmSale(req), collectionSaleRequest, "collectionFacadeService.confirmSale");  
      
    TransactionHookManager.registerHook(new PaySuccessTransactionHook(collectionSaleResponse.getHeldCollectionId()));  
      
    Boolean result = payOrderService.paySuccess(paySuccessEvent);  
    Assert.isTrue(result, "payOrderService.paySuccess failed");  
      
    return true;  
}
```

我们在支付服务的paySuccess方法上增加 `@GlobalTransactional(rollbackFor = Exception.class)` ，代码执行到这一行的时候就会开启全局事务。

然后在这个方法中通过Dubbo去远程调用各个其他服务，并且这里我们大量用到了Assert，来判断远程服务的结果，如果有某个服务失败了，则抛出异常。然后seata会拦截到异常，然后进行全局事务的回滚动作。

各个参与事务的服务，如订单服务、藏品服务等，如果用了支持seata的rpc框架，则不需要做任何事情，只需要同样接入seata，他么就能自动的创建分支事务、以及进行事务的提交和回滚。

但是如果用到了shardingjdbc，则需要做一些配置和改动，可以参考：
[12、Seata 接入](2、相关技术/24、项目/3、数藏项目/4、框架接入/12、Seata%20接入.md)

```java
/** 
* 订单支付 
* 
* @param request 
* @return 
* */
@Transactional(rollbackFor = Exception.class)  
@ShardingSphereTransactionType(TransactionType.BASE)  
public OrderResponse pay(OrderPayRequest request) {  
    return doExecuteWithOutTrans(request, tradeOrder -> tradeOrder.pay(request));  
}
```

这里就是多用了一个@ShardingSphereTransactionType(TransactionType.BASE)。并且在doExecuteWithOutTrans里面不需要再加其他的事务了。其他的doExecute方法我们为了减少事务粒度，用了编程式事务，这里不能用了，所以就单独加了个doExecuteWithOutTrans。

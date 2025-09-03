[2、基于XXL-JOB的分片实现分库分表后的扫表](2、相关技术/24、项目/3、数藏项目/8、最佳实践/10、分库分表(ShardingJDBC)/2、基于XXL-JOB的分片实现分库分表后的扫表.md)

定时任务的调度部分和上文一致，这里就不再重复了，只介绍下关单操作的具体内容。

这里在关单时做一次判断，查询支付单，判断是否有已经支付过的，避免渠道支付回调已经回来了，但是还没来得及通知订单模块，导致误关单的问题。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202502231714676.png)

也就是说，上面的第一步发生了之后，第三步没来得及执行，第二步先执行了，如果不做判断的话，就会导致第三步再过来的时候，订单已经被关了的情况。所以这里反查一下，减少这个情况发生的概率。（但是也会存在渠道侧成功了，但是还没来得及通知支付模块的情况，这种再回来就退款好了）
```java
private void executeTimeoutSingle(TradeOrder tradeOrder) {  
    //查询支付单，判断是否已经支付成功。  
    PayQueryRequest request = new PayQueryRequest();  
    request.setPayerId(tradeOrder.getBuyerId());  
    request.setPayOrderState(PayOrderState.PAID);  
    PayQueryByBizNo payQueryByBizNo = new PayQueryByBizNo();  
    payQueryByBizNo.setBizNo(tradeOrder.getOrderId());  
    payQueryByBizNo.setBizType(BizOrderType.TRADE_ORDER.name());  
    request.setPayQueryCondition(payQueryByBizNo);  
    MultiResponse<PayOrderVO> payQueryResponse = payFacadeService.queryPayOrders(request);  
      
    if (payQueryResponse.getSuccess() && CollectionUtils.isEmpty(payQueryResponse.getDatas())) {  
        LOG.info("start to execute order timeout , orderId is {}", tradeOrder.getOrderId());  
        OrderTimeoutRequest orderTimeoutRequest = new OrderTimeoutRequest();  
        orderTimeoutRequest.setOrderId(tradeOrder.getOrderId());  
        orderTimeoutRequest.setOperateTime(new Date());  
        orderTimeoutRequest.setOperator(UserType.PLATFORM.name());  
        orderTimeoutRequest.setOperatorType(UserType.PLATFORM);  
        orderFacadeService.timeout(orderTimeoutRequest);  
    }  
}
```

timeout 的接口实现如下：
```java
@Facade  
public OrderResponse timeout(OrderTimeoutRequest request) {  
    return sendTransactionMsgForClose(request);  
}
```

这里其实是依赖了 RocketMQ 的事务消息来实现的。具体参考：
[3、基于RocketMQ事务消息实现订单取消的一致性](2、相关技术/24、项目/3、数藏项目/8、最佳实践/6、数据一致性（分布式事务）/3、基于RocketMQ事务消息实现订单取消的一致性.md)




#MQ消息重投配合幂等解决一致性问题  

# 描述

我们的项目中，秒杀有两套方案，一种是基于`InventoryHint` 的方案，还有一种是不用 `InventoryHint`，而是用 `RocketMQ` 的方案。

为了可以抗更高的并发，我们把数据库中的库存扣减，放到了 `Redis` 中，在 Redis 进行扣减，但是，最终还是要同步到数据库的，因为 `Redis` 不可靠，所以我们还是要在数据库做真正的扣减。

只不过在数据库中扣减因为前面有 `Redis` 拦了一道了，真正过来到数据库中流量会小很多，所以数据库就能扛得住了。

那么，有什么办法可以让 `Redis` 扣减成功后，数据库也能进行扣减呢，并且能够在数据库扣减失败后重试。那就是 MQ 了。

# 方案设计图

我们在项目中引入 MQ，帮我们做下单环节的高并发流量的雪峰填谷。整体流程如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202502222226961.png)

1. 用户秒杀请求过来后，先在 `Redis` 中进行扣减，利用 `Redis` 的单线程、高性能机制，可以**保证在有库存的时候多个用户按顺序扣减**。如果库存没有了，则失败；
>    *==优点是提前将无效的库存更新请求过滤掉==*，如果不过滤这些无效请求，它们会进入MQ，最后由MySQL消费，无效请求在更新数据库时肯定不满足扣减时`update`语句中的`where`条件，虽然不满足，但还是会锁住表中这行数据导致无效的锁占用，也就是热点行更新
2. 对于在 `Redis` 中扣减成功的请求，说明他们下单的时候库存还有，那么就可以把这个用户的流量放过。这时候就发一个 MQ，告诉数据库，这个用户是可以下单的。
3. 数据库在接受到 MQ 的消息后，在进行数据库层面的库存扣减。这里的流量就小很多了，而且可以做限流和降级，因为 MQ 本身可以重试，也能控制速率。

详细的过程如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202502222228041.png)

!!!异步消费中保证 **==库存模块==和==订单模块==** 的 **数据库库存扣减、订单创建**之间的 **==原子性==、==一致性==** 的方式：<font color="red" size=5>在库存模块和订单模块做好幂等，失败了靠mq重投，做最终一致性</font>

`newBuy` 是基于 MQ 的方案的秒杀方案入口：
```java
/**  
 * 下单（不基于inventory hint的实现）  
 *  
 * @param 
 * @return 幂等号  
 */  
@PostMapping("/newBuy")  
public Result<String> newBuy(@Valid @RequestBody BuyParam buyParam) {  
    OrderCreateRequest orderCreateRequest = getOrderCreateRequest(buyParam);  
      
    boolean result = streamProducer.send("newBuy-out-0", buyParam.getGoodsType(), JSON.toJSONString(orderCreateRequest));  
      
    if (!result) {  
        throw new TradeException(TradeErrorCode.ORDER_CREATE_FAILED);  
    }  
      
    return Result.success(orderCreateRequest.getOrderId());  
}
```

这里，为了保证 Redis 扣减成功后，MQ 一定可以发成功，我们用了 `RocketMQ` 的事务消息。`newBuy` 是一个事务消息，可以先发半消息，再执行本地事务，成功后再发送另外一个半消息。（事务消息原理见：[3、基于RocketMQ事务消息实现订单取消的一致性](2、相关技术/24、项目/3、数藏项目/8、最佳实践/6、数据一致性（分布式事务）/3、基于RocketMQ事务消息实现订单取消的一致性.md)

配置如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202502222237617.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202502222238484.png)

在本地事务操作中。我们直接去 `Redis` 做扣减：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202502222239569.png)

同时在 `Redis` 扣减成功后 `commit` 这个消息，失败的话，则 `rollback` 这个消息。

并且提供一个供 MQ 反查的方法，当消息丢失时也可以知道要不要提交：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202502222250008.png)

实现也很简单，就是去 `Redis` 中查询是否有扣减流水，如果有，则说明扣减成功了。如果没有，这说明没扣减成功。

只有 `Redis` 扣减成功，最终这个事务消息就会发出去，然后在 `order` 模块中就可以监听这个消息，做订单创建以及库存扣减了。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202502222254529.png)

这里的 `createAndConfirm` 方法，就是具体的订单创建、以及库存扣减的过程了；在这个方法中进行库存扣减和订单创建时会进行幂等判断，主要是为了处理<font color="red" size=5>使用<font color="blue" size=5>消息重投</font>解决库存扣减和订单创建存在的<font color="blue" size=5>分布式事务的一致性问题</font></font>。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508220116934.png)

流程和用 Hint 的方案差不多，只不过库存扣减的 SQL 上是无 Hint 的：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202502222258330.png)

```java
<!--  库存预扣减-无hint版  -->  
<update id="trySaleWithoutHint">  
UPDATE collection  
SET saleable_inventory = saleable_inventory - #{quantity}, lock_version = lock_version + 1,gmt_modified = now()  
WHERE id = #{id} and <![CDATA[saleable_inventory >= #{quantity}]]>  
</update>
```

另外，有个地方需要注意，`RocketMQ` 的事务消息，不管本地事务是否成功，只要一阶段消息发成功都会返回 true，所以，在发消息之后，需要确认下本地事务是否执行成功了。即在newBuy方法中增加以下校验：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202502222259795.png)

这里之所以可以直接反查，是因为在RocketMQ的事务消息中，本地事务的操作（InventoryDecreaseTransactionListener#executeLocalTransaction）是**同步执行(我理解的是：串行执行、本地事务方法执行完毕后调用send方法才相当于结束，才会向下走)** 的，所以只要同步调用成功了，这里一定能查得到。



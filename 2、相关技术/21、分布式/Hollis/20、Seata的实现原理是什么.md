# 典型回答

Seata是一个阿里开源的分布式事务解决方案（Simple Extensible Autonomous Transaction Architecture），用于在分布式系统中实现分布式事务。它旨在简化分布式事务的开发和管理，帮助解决分布式系统中的数据一致性问题。

因为Seata的开发者坚定地认为：**一个分布式事务是有若干个本地事务组成的**。所以他们给Seata体系的所有组件定义成了三种，分别是**Transaction Coordinator**、**Transaction Manager**和**Resource Manager**
1. **Transaction Coordinator(TC)**:  这是一个独立的服务，是一个<font color="red" size=5>独立的 JVM 进程</font>，里面不包含任何业务代码，它的主要职责：<font color="blue" size=5>维护着整个事务的全局状态，负责通知 RM 执行回滚或提交</font>；
2. **Transaction Manager(TM)**: <font color="red" size=5>微服务架构中的聚合服务</font>，即将不同的微服务组合起来成一个完整的业务流程，TM 的职责是<font color="blue" size=5>开启一个全局事务或者提交或回滚一个全局事务</font>；
3. **Resource Manager(RM)**：RM 在微服务框架中对应具体的某个<font color="red" size=5>微服务为事务的分支</font>，RM 的职责是：<font color="blue" size=5>执行每个事务分支的操作</font>。

看上去好像很难理解？举个例子你就知道了：
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411241825349.png)

在一个下单事务中，我们有一个聚合的服务，姑且把他叫做TradeCenter吧，他负责接收并处理用户的下单请求，并且下单过程中需要调用订单服务（Order）、库存服务（Stock）及账户服务（Account）进行创建订单、扣减库存及增加积分。

所以TradeCenter担当的就是TM的角色，而Order、Stock及Account三个微服务就是RM的角色。在此之外，还需要一个独立的服务，维护分布式事务的全局状态，他就是TC。

因为TC维护着整个事务的全局状态，负责通知 RM 执行回滚或提交，所以他和TM、RM都是有交互的。并且TM和RM之间也有调用关系。多个RM之间可以是独立的。

上面这个场景中，要想保证分布式事务，就需要Order、Stock及Account三个服务对应的数据库表操作，要么都成功、要么都失败。不能有部分成功、部分失败的情况。

在用了Seata之后，一次分布式事务的大致流程如下（*==不同的模式略有不同==*，在介绍具体模式的时候分别展开）：
1. **==TM在接收到==** 用户的下单请求后，会 **==先调用TC创建一个全局事务==**，并且 **==从TC获取到他生成的XID==**。
2. **TM开始通过 `RPC/Restful` 调用各个RM，调用过程中需要==把XID同时传递过去==**。
   ![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411241852281.png)
3. **==RM通过其接收到的XID==**,将其所管理的资源且被该调用所使用到的资源 **==注册为一个事务分支==**(Branch Transaction)
   ![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411241853711.png)
4. 当该请求的调用链全部结束时，**==TM根据本次调用是否有失败的情况==**，如果所有调用都成功，则 **==决议Commit==**，如果有超时或者失败，则 **==决议Rollback==**。
5. **==TM==将事务的==决议结果通知TC==**，**==TC将协调所有RM==进行事务的==二阶段动作==**，*==该回滚回滚，该提交提交==*。
   ![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411241858482.png)

**要求**:
- **==所有的RM都能做到2阶段==**：
	1. 第一阶段在<font color="red" size=5>RM中做事务的预处理</font>
	2. 第二阶段做由<font color="red" size=5>TC控制做事务的提交或者回滚</font>
具体怎么实现，是否需要自己改代码，这个不同的模式不太一样。

# 扩展知识

## Seata 的四种事务模式

[36、Seata的4种事务模式，各自适合的场景是什么？](2、相关技术/21、分布式/Hollis/36、Seata的4种事务模式，各自适合的场景是什么？.md)

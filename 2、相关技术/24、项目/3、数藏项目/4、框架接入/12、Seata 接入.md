
Seata 是一个分布式事务组件，可以帮我们实现分布式事务。

想要接入他，你需要先按照以下文档部署好中间件：
[9、Seata部署](2、相关技术/24、项目/3、数藏项目/2、中间件部署/9、Seata部署.md)

然后按照以下步骤进行：

### 项目依赖

```xml
<dependency>  
    <groupId>io.seata</groupId>  
    <artifactId>seata-all</artifactId>  
    <version>2.0.0</version>  
    <exclusions>  
        <exclusion>  
            <groupId>org.antlr</groupId>  
            <artifactId>antlr4-runtime</artifactId>  
        </exclusion>  
    </exclusions>  
</dependency>  
  
<dependency>  
    <groupId>com.alibaba.cloud</groupId>  
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>  
</dependency>
```

以上，把seata-all和spring-cloud-starter-alibaba-seata给依赖导入到项目中即可，如果项目同时集成了Seata和ShardingJDBC，则需要额外引入以下jar包：

```xml
<dependency>  
    <groupId>org.apache.shardingsphere</groupId>  
    <artifactId>shardingsphere-transaction-base-seata-at</artifactId>  
    <version>5.2.1</version>  
</dependency>
```

但是我们为了解决Seata和ShardingJDBC的兼容问题，额外引入了一个shardingsphere-transaction-core的5.2.1版本，这个具体在下面展开介绍：
[2、重写ShardingSphere源码，解决集成Seata的事务失效问题](2、相关技术/24、项目/3、数藏项目/14、问题排查/1、重写中间件源码/2、重写ShardingSphere源码，解决集成Seata的事务失效问题.md)

### 初始化表结构

这个 SQL 要在你的应用的数据库（`nft_turbo`）中创建，而不是在 seata 的数据库创建。
```sql
-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT       NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created`   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT ='AT transaction mode undo table';
ALTER TABLE `undo_log` ADD INDEX `ix_log_created` (`log_created`);
```

### 配置文件

在加好了jar包依赖后，就是seata的配置了。在你的application.yml中增加以下内容：
```yml
seata:  
  application-id: ${spring.application.name}  
  tx-service-group: default_tx_group # 事务的服务的group，默认用default_tx_group即可  
  #  use-jdk-proxy: true  
  #  enable-auto-data-source-proxy: false  config:  
    type: nacos #使用nacos作为配置中心  
    nacos:  
      server-addr: 114.xx.xxx.45:8848 #使用nacos作为配置中心  
      group: seata #nacos上的group  
      data-id: seataServer.properties  #nacos上配置的data-id  
      namespace: 7ebdfb9b-cd9d-4a5e-8969-1ada0bb9ba04 #nacos上配置的namespace  
  registry:  
    type: nacos  #使用nacos作为注册中心  
    nacos:  
      application: seata-server  
      server-addr: 114.xx.xx.45:8848  
      group: seata  
      cluster: default  
      namespace: 7ebdfb9b-cd9d-4a5e-8969-1ada0bb9ba04
```

这里的use-jdk-proxy和enable-auto-data-source-proxy，只需要在使用了shardingJDBC当做数据源的模块中依赖，其他模块中不需要依赖。

```yml
use-jdk-proxy: true
enable-auto-data-source-proxy: false
```

如果使用了shardingJDBC，同时需要再application.yml的同级目录增加一个seata.conf文件，内容如下：
```conf
client {
    application.id = nft-turbo-order
    transaction.service.group = default_tx_group
}
```

主要配置的是application.id和transaction.service.group，这个也是只有shardingjdbc代理过的数据源的情况下才需要的。

### 接入成功检查

按照以上配置完成配置之后，就相当于把seata已经接进来了，启动应用如果没有报错的话，到seata的部署服务器上看一下日志，看看是否有注册成功的日志输出：

vim /root/logs/seata/seata-server.8091.all.log
```java
2024-08-05 18:04:10.191  INFO --- [ServerHandlerThread_1_46_500] [io.seata.core.rpc.processor.server.RegRmProcessor] [onRegRmMessage] []: RM register success,message:RegisterRMRequest{resourceIds='jdbc:mysql://rm-xxxx.mysql.rds.aliyuncs.com:3306/nfturbo', version='2.0.0', applicationId='nft-turbo-order', transactionServiceGroup='default_tx_group', extraData='null'},channel:[id: 0xa46c4edf, L:/172.21.109.90:8091 - R:/125.ss.ss.253:59994],client version:2.0.0
```

接下来就可以进行事务的配置了，以AT事务为例。

### AT 事务配置

在AT事务中，我们只需要在事务的发起处，用`@GlobalTransactional` 代替`@Transactional` 即可，如：**【在TM中用GlobalTransactional】**
```java
@GlobalTransactional(rollbackFor = Exception.class)  
public boolean paySuccess(PaySuccessEvent paySuccessEvent) {  
    //远程调用订单服务  
    //远程调用藏品服务  
    //远程调用XX服务  
    //调用本地的支付服务  
}
```

如果参与者没有用ShardingJDBC的话，就啥都不需要额外做，只需要也像前面几步一样把seata接入就行了。

如果某个事务参与者用了shardingjdbc。需要在自己的服务上增加一个额外注解：**【在RM中用Transactional】**
```java
@Transactional(rollbackFor = Exception.class)  
@ShardingSphereTransactionType(TransactionType.BASE)  
public OrderResponse pay(OrderPayRequest request) {  
    return doExecuteWithOutTrans(request, tradeOrder -> tradeOrder.pay(request));  
}
```

@ShardingSphereTransactionType(TransactionType.BASE)作用是告诉ShardingJDBC，这个方法要加入到seata事务中。

以上，就是完整的接入过程了，大家主要看代码的时候，看以下几个地方：

1、seata接入部分：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411102312812.png)

2、全局事务开启处：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411102313835.png)

3、订单（分库分表）模块配置：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411102313310.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411102314699.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411102314264.png)

4、其他模块配置：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411102314971.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411102315208.png)

5、订单（分库分表）模块代码：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411102315030.png)

### ChatGPT关于AT的解释

AT (Automatic Transaction) 事务是分布式事务管理中的一种模式，通常与阿里巴巴的分布式事务解决方案 Seata（Simple Extensible Autonomous Transaction Architecture）相关联。在 AT 模式中，事务是自动管理的，主要通过代理数据库操作来实现事务的分段和最终的一致性。

#### AT 事务模式的关键概念

1. **全局事务**（Global Transaction）
   - **解释**：由全局事务协调器（Transaction Coordinator，TC）管理的事务，包含多个子事务（分支事务）。
   - **作用**：保证分布式系统中多个数据库操作在全局范围内的一致性。

2. **分支事务**（Branch Transaction）
   - **解释**：参与全局事务的每个具体业务操作，通常是针对单个数据源的本地事务。
   - **作用**：保证局部操作的原子性和一致性。

3. **全局事务 ID**（Global Transaction ID，XID）
   - **解释**：唯一标识一个全局事务，所有的分支事务都绑定到这个全局事务 ID 上。
   - **作用**：用于追踪和管理分布式事务的状态。

4. **事务协调器**（Transaction Coordinator，TC）
   - **解释**：管理全局事务的生命周期，包括创建、提交、回滚和状态维护。
   - **作用**：协调各分支事务的提交和回滚操作，保证全局事务的一致性。

5. **事务管理器**（Transaction Manager，TM）
   - **解释**：管理全局事务的创建、提交和回滚。
   - **作用**：负责分布式事务的开始和结束。

6. **资源管理器**（Resource Manager，RM）
   - **解释**：管理分支事务的资源（如数据库连接）。
   - **作用**：负责本地事务的提交和回滚操作，并与事务协调器进行通信。

#### AT 事务的执行流程

1. **全局事务开始**
   - 事务管理器 TM 请求事务协调器 TC 创建一个全局事务。
   - TC 生成一个唯一的全局事务 ID（XID）。

2. **分支事务注册**
   - 各个业务服务在执行具体操作时，向事务协调器 TC 注册分支事务，绑定到全局事务 ID 上。
   - 每个分支事务在本地数据库上执行操作，并记录 Undo Log（撤销日志）。

3. **提交全局事务**
   - 业务操作完成后，事务管理器 TM 请求事务协调器 TC 提交全局事务。
   - TC 通知所有的分支事务提交本地事务。
   - 分支事务提交后，删除对应的 Undo Log。

4. **回滚全局事务**
   - 如果在全局事务的执行过程中发生错误，事务管理器 TM 请求事务协调器 TC 回滚全局事务。
   - TC 通知所有的分支事务回滚本地事务。
   - 分支事务根据 Undo Log 撤销已执行的操作，恢复数据的初始状态。

#### AT 事务的优点

1. **自动管理**：数据库代理自动管理事务的提交和回滚，无需业务代码处理。
2. **高效**：通过本地事务的两阶段提交，实现高效的分布式事务管理。
3. **易于集成**：与现有的数据库系统和 ORM 框架无缝集成，开发者无需改变业务代码。

#### AT 事务的缺点

1. **依赖数据库代理**：需要在数据库操作前后插入代理，增加了系统复杂性。
2. **资源消耗**：维护 Undo Log 和事务协调的开销较大，可能影响系统性能。
3. **有限的数据库支持**：并非所有的数据库都支持 Undo Log 或相关功能，限制了 AT 事务的适用范围。

#### 示例代码

以下是一个使用 Seata 实现 AT 事务的简单示例：

```java
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BusinessService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AccountService accountService;

    @GlobalTransactional
    public void placeOrder(String userId, String productId, int count) {
        orderService.createOrder(userId, productId, count);
        accountService.debit(userId, count * 100);
    }
}
```

在这个示例中，`placeOrder` 方法被标记为全局事务 (`@GlobalTransactional`)，其中调用了两个本地服务 `orderService` 和 `accountService`。如果任何一个服务操作失败，整个事务将被回滚。

#### 总结

AT 事务模式通过数据库代理和事务协调器，实现分布式事务的管理和一致性保证。它在简化分布式系统的事务管理方面具有重要作用，但也带来了一定的复杂性和性能开销。在实际应用中，需要根据系统需求和环境选择合适的事务管理方案。








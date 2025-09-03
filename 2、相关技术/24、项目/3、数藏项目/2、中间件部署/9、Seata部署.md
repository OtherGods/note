### 1、下载安装包

到Seata的官方下载最新的安装包：

https://seata.apache.org/zh-cn/unversioned/download/seata-server/

我下载的是2.0.0的版本
[https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202412291051858.zip](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202412291051858.zip)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022206609.png)

下载后解压，unzip `unzip seata-server-2.0.0.zip` ，解压后的目录结构如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022207506.png)

### 2、配置文件修改

进入conf目录，找到application.yml，修改其中的配置：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022207865.png)

配置中心参考：[https://seata.apache.org/zh-cn/docs/user/configuration/nacos](https://seata.apache.org/zh-cn/docs/user/configuration/nacos)

注册中心参考：[https://seata.apache.org/zh-cn/docs/user/registry/nacos](https://seata.apache.org/zh-cn/docs/user/registry/nacos)

对应的nacos上的namespace配置如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022235069.png)

### 3、在nacos上增加配置信息

在nacos上增加配置，namespace选择刚刚创建好的，和seata的配置对应上，创建一个data_id为seataServer.properties的文件
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022236129.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022236114.png)

文件内容来自：[https://github.com/apache/incubator-seata/blob/develop/script/config-center/config.txt](https://github.com/apache/incubator-seata/blob/develop/script/config-center/config.txt)

把这里面的内容复制过来，修改其中的部分内容，并配置到seataServer.properties中：
```txet
#For details about configuration items, see https://seata.io/zh-cn/docs/user/configurations.html
#Transport configuration, for client and server
transport.type=TCP
transport.server=NIO
transport.heartbeat=true
transport.enableTmClientBatchSendRequest=false
transport.enableRmClientBatchSendRequest=true
transport.enableTcServerBatchSendResponse=false
transport.rpcRmRequestTimeout=30000
transport.rpcTmRequestTimeout=30000
transport.rpcTcRequestTimeout=30000
transport.threadFactory.bossThreadPrefix=NettyBoss
transport.threadFactory.workerThreadPrefix=NettyServerNIOWorker
transport.threadFactory.serverExecutorThreadPrefix=NettyServerBizHandler
transport.threadFactory.shareBossWorker=false
transport.threadFactory.clientSelectorThreadPrefix=NettyClientSelector
transport.threadFactory.clientSelectorThreadSize=1
transport.threadFactory.clientWorkerThreadPrefix=NettyClientWorkerThread
transport.threadFactory.bossThreadSize=1
transport.threadFactory.workerThreadSize=default
transport.shutdown.wait=3
transport.serialization=seata
transport.compressor=none

#Transaction routing rules configuration, only for the client
service.vgroupMapping.default_tx_group=default
#If you use a registry, you can ignore it
service.default.grouplist=127.0.0.1:8091
service.enableDegrade=false
service.disableGlobalTransaction=false

#Transaction rule configuration, only for the client
client.rm.asyncCommitBufferLimit=10000
client.rm.lock.retryInterval=10
client.rm.lock.retryTimes=30
client.rm.lock.retryPolicyBranchRollbackOnConflict=true
client.rm.reportRetryCount=5
client.rm.tableMetaCheckEnable=true
client.rm.tableMetaCheckerInterval=60000
client.rm.sqlParserType=druid
client.rm.reportSuccessEnable=false
client.rm.sagaBranchRegisterEnable=false
client.rm.sagaJsonParser=fastjson
client.rm.tccActionInterceptorOrder=-2147482648
client.tm.commitRetryCount=5
client.tm.rollbackRetryCount=5
client.tm.defaultGlobalTransactionTimeout=60000
client.tm.degradeCheck=false
client.tm.degradeCheckAllowTimes=10
client.tm.degradeCheckPeriod=2000
client.tm.interceptorOrder=-2147482648
client.undo.dataValidation=true
client.undo.logSerialization=jackson
client.undo.onlyCareUpdateColumns=true
server.undo.logSaveDays=7
server.undo.logDeletePeriod=86400000
client.undo.logTable=undo_log
client.undo.compress.enable=true
client.undo.compress.type=zip
client.undo.compress.threshold=64k
#For TCC transaction mode
tcc.fence.logTableName=tcc_fence_log
tcc.fence.cleanPeriod=1h

#Log rule configuration, for client and server
log.exceptionRate=100

#Transaction storage configuration, only for the server. The file, db, and redis configuration values are optional.
store.mode=db
store.lock.mode=db
store.session.mode=db
#Used for password encryption
store.publicKey=

#These configurations are required if the `store mode` is `db`. If `store.mode,store.lock.mode,store.session.mode` are not equal to `db`, you can remove the configuration block.
store.db.datasource=druid
store.db.dbType=mysql
store.db.driverClassName=com.mysql.jdbc.Driver
store.db.url=jdbc:mysql://rm-xxxxx:3306/seata?useUnicode=true&rewriteBatchedStatements=true
store.db.user=nfturbo
store.db.password=NFTurbo666
store.db.minConn=5
store.db.maxConn=30
store.db.globalTable=global_table
store.db.branchTable=branch_table
store.db.distributedLockTable=distributed_lock
store.db.queryLimit=100
store.db.lockTable=lock_table
store.db.maxWait=5000

#Transaction rule configuration, only for the server
server.recovery.committingRetryPeriod=1000
server.recovery.asynCommittingRetryPeriod=1000
server.recovery.rollbackingRetryPeriod=1000
server.recovery.timeoutRetryPeriod=1000
server.maxCommitRetryTimeout=-1
server.maxRollbackRetryTimeout=-1
server.rollbackRetryTimeoutUnlockEnable=false
server.distributedLockExpireTime=10000
server.xaerNotaRetryTimeout=60000
server.session.branchAsyncQueueSize=5000
server.session.enableBranchAsyncRemove=false
server.enableParallelRequestHandle=false

#Metrics configuration, only for the server
metrics.enabled=false
metrics.registryType=compact
metrics.exporterList=prometheus
metrics.exporterPrometheusPort=9898
```

其中需要修改的部分如下，然后保存即可。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022238444.png)

### 4、创建数据库及表结构

在你第三步中指定的那个数据库实例中创建一个库名为seata的数据库，然后执行https://github.com/apache/incubator-seata/blob/develop/script/server/db/mysql.sql 文件中的SQL内容：
```sql
-- -------------------------------- The script used when storeMode is 'db' --------------------------------
-- the table to store GlobalSession data
CREATE TABLE IF NOT EXISTS `global_table`
(
    `xid`                       VARCHAR(128) NOT NULL,
    `transaction_id`            BIGINT,
    `status`                    TINYINT      NOT NULL,
    `application_id`            VARCHAR(32),
    `transaction_service_group` VARCHAR(32),
    `transaction_name`          VARCHAR(128),
    `timeout`                   INT,
    `begin_time`                BIGINT,
    `application_data`          VARCHAR(2000),
    `gmt_create`                DATETIME,
    `gmt_modified`              DATETIME,
    PRIMARY KEY (`xid`),
    KEY `idx_status_gmt_modified` (`status` , `gmt_modified`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- the table to store BranchSession data
CREATE TABLE IF NOT EXISTS `branch_table`
(
    `branch_id`         BIGINT       NOT NULL,
    `xid`               VARCHAR(128) NOT NULL,
    `transaction_id`    BIGINT,
    `resource_group_id` VARCHAR(32),
    `resource_id`       VARCHAR(256),
    `branch_type`       VARCHAR(8),
    `status`            TINYINT,
    `client_id`         VARCHAR(64),
    `application_data`  VARCHAR(2000),
    `gmt_create`        DATETIME(6),
    `gmt_modified`      DATETIME(6),
    PRIMARY KEY (`branch_id`),
    KEY `idx_xid` (`xid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- the table to store lock data
CREATE TABLE IF NOT EXISTS `lock_table`
(
    `row_key`        VARCHAR(128) NOT NULL,
    `xid`            VARCHAR(128),
    `transaction_id` BIGINT,
    `branch_id`      BIGINT       NOT NULL,
    `resource_id`    VARCHAR(256),
    `table_name`     VARCHAR(32),
    `pk`             VARCHAR(36),
    `status`         TINYINT      NOT NULL DEFAULT '0' COMMENT '0:locked ,1:rollbacking',
    `gmt_create`     DATETIME,
    `gmt_modified`   DATETIME,
    PRIMARY KEY (`row_key`),
    KEY `idx_status` (`status`),
    KEY `idx_branch_id` (`branch_id`),
    KEY `idx_xid` (`xid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `distributed_lock`
(
    `lock_key`       CHAR(20) NOT NULL,
    `lock_value`     VARCHAR(20) NOT NULL,
    `expire`         BIGINT,
    primary key (`lock_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

INSERT INTO `distributed_lock` (lock_key, lock_value, expire) VALUES ('AsyncCommitting', ' ', 0);
INSERT INTO `distributed_lock` (lock_key, lock_value, expire) VALUES ('RetryCommitting', ' ', 0);
INSERT INTO `distributed_lock` (lock_key, lock_value, expire) VALUES ('RetryRollbacking', ' ', 0);
INSERT INTO `distributed_lock` (lock_key, lock_value, expire) VALUES ('TxTimeoutCheck', ' ', 0);
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022240918.png)

### 5、启动Seata服务端

到机器上，seata/bin目录下，执行：`sh seata-server.sh -h 116.62.53.29` 命令。通过-h指定本地的ip。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022241455.png)

然后创建一个目录：`/root/logs/seata/` 之后运行后会打印如下日志：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022241600.png)

接下来，打开你的机器的7091和8091端口，通过7091端口可以访问你的seata的管理控制台页面：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022241936.png)

输入用户名（默认seata）、密码（默认seata)，即可进入控制台。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022242615.png)

同时在nacos上能看到有一个seata的服务注册上去了：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022242661.png)

### 6、常见问题

#### 6.1、执行完启动命令之后，日志正常打印了，但是服务没起来。

7091端口无法访问，nacos上也没有对应的服务注册上去。

a、首先确认下端口是不是开启了，如果没开启，记得开启一下端口。

b、直接执行一下日志提示中对应的命令，看看报啥错。比如下面这个日志
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022243089.png)

去掉后面的`>> /dev/null 2>&1 &`部分内容，直接执行下这个命令，就能看到具体的报错了。 
```shell
/root/package/jdk-21.0.2/bin/java -Dlog.home=/root/logs/seata -server -Dloader.path=/root/package/seata/lib -Xmx2048m -Xms2048m -Xss640k -XX:SurvivorRatio=10 -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=1024m -XX:-OmitStackTraceInFastThrow -XX:-UseAdaptiveSizePolicy -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/root/logs/seata/java_heapdump.hprof -XX:+DisableExplicitGC -Xlog:gc*:file=/root/logs/seata/seata_gc.log:time,tags:filecount=10,filesize=102400 -Dio.netty.leakDetectionLevel=advanced -Dapp.name=seata-server -Dapp.pid=1602373 -Dapp.home=/root/package/seata -Dbasedir=/root/package/seata -Dspring.config.additional-location=/root/package/seata/conf/ -Dspring.config.location=/root/package/seata/conf/application.yml -Dlogging.config=/root/package/seata/conf/logback-spring.xml -jar /root/package/seata/target/seata-server.jar -h 116.62.53.29 -p 8091
```

我遇到的是

```
[0.000s][error][logging] Initialization of output 'file=/root/logs/seata/seata_gc.log' using options 'filecount=10,filesize=102400' failed.
```

所以我手动创建了一个/root/logs/seata/目录重启就可以了。

#### 6.2、应用启动报错-service.vgroupMapping.default_tx_group configuration item is required
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022244089.png)

在你的 seata 的 namespace 中，定义一个配置，名字为：service.vgroupMapping.default_tx_group
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022244801.png)

值为：default
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022244835.png)





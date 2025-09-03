### 配置要求

CPU：1核

内存：2G

### 安装Canal-deployer

我用的Canal-deployer是1.1.7版本。

1、Canal-deployer下载

到https://github.com/alibaba/canal/releases

下载canal.deployer，可以直接监听MySQL的binlog，把自己伪装成MySQL的从库，只负责接收数据，并不做处理。

然后解压：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022153599.png)

2、配置修改

修改canal.properties，增加

```shell
canal.ip = 127.0.0.1
```

修改example/instance.properties

```properties
# 需要同步数据的MySQL地址
canal.instance.master.address=数据库地址:3306
canal.instance.master.journal.name=
canal.instance.master.position=
canal.instance.master.timestamp=
canal.instance.master.gtid=
# 用于同步数据的数据库账号
canal.instance.dbUsername=账号
# 用于同步数据的数据库密码
canal.instance.dbPassword=密码
# 数据库连接编码
canal.instance.connectionCharset = UTF-8
# 需要订阅binlog的表过滤正则表达式
canal.instance.filter.regex=.*\\..*
```

重点修改`canal.instance.master.address` 、`canal.instance.dbUsername`和`canal.instance.dbPassword`三个字段。

3、启动 Canal

到 bin 目录下，执行命令：./startup.sh

canal/logs/canal/canal_stdout.log 报错：

```
Unrecognized VM option 'AggressiveOpts'Error: Could not create the Java Virtual Machine.Error: A fatal exception has occurred. Program will exit.
```

修改canal/bin/startup.sh：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022155557.png)

移除AggressiveOpts、UseBiasedLocking 等 JDK 21中已经不在支持的配置，重新启动.

4、启动成功检查

启动成功，检查canal/logs/canal.log：
```log
2024-04-18 16:56:35.787 [main] INFO  com.alibaba.otter.canal.deployer.CanalLauncher - ## set default uncaught exception handler

2024-04-18 16:56:35.801 [main] INFO  com.alibaba.otter.canal.deployer.CanalLauncher - ## load canal configurations

2024-04-18 16:56:35.825 [main] INFO  com.alibaba.otter.canal.deployer.CanalStarter - ## start the canal server.

2024-04-18 16:56:35.879 [main] INFO  com.alibaba.otter.canal.deployer.CanalController - ## start the canal server[127.0.0.1(127.0.0.1):11111]

2024-04-18 16:56:37.614 [main] INFO  com.alibaba.otter.canal.deployer.CanalStarter - ## the canal server is running now ......
```

看到`the canal server is running now ......`表示启动成功。

### 安装Canal-adapter

同样是使用1.1.7版本，和deployer 保持一致。

canal.adapter，相当于canal的客户端，会从canal-deployer中获取数据，然后对数据进行同步，可以同步到MySQL、Elasticsearch和HBase等存储中去。

1、下载canal.adapter

到https://github.com/alibaba/canal/releases 下载

然后解压

2、修改配置

修改 application.properties：
```yml
server:
  port: 8081
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null

canal.conf:
  mode: tcp #tcp kafka rocketMQ rabbitMQ
  flatMessage: true
  zookeeperHosts:
  syncBatchSize: 1000
  retries: -1
  timeout:
  accessKey:
  secretKey:
  consumerProperties:
    # canal tcp consumer
    canal.tcp.server.host: 127.0.0.1:11111
    canal.tcp.zookeeper.hosts:
    canal.tcp.batch.size: 500
    canal.tcp.username:
    canal.tcp.password:
    # kafka consumer
    kafka.bootstrap.servers: 127.0.0.1:9092
    kafka.enable.auto.commit: false
    kafka.auto.commit.interval.ms: 1000
    kafka.auto.offset.reset: latest
    kafka.request.timeout.ms: 40000
    kafka.session.timeout.ms: 30000
    kafka.isolation.level: read_committed
    kafka.max.poll.records: 1000
    # rocketMQ consumer
    rocketmq.namespace:
    rocketmq.namesrv.addr: 127.0.0.1:9876
    rocketmq.batch.size: 1000
    rocketmq.enable.message.trace: false
    rocketmq.customized.trace.topic:
    rocketmq.access.channel:
    rocketmq.subscribe.filter:
    # rabbitMQ consumer
    rabbitmq.host:
    rabbitmq.virtual.host:
    rabbitmq.username:
    rabbitmq.password:
    rabbitmq.resource.ownerId:

  srcDataSources:
    defaultDS:
      url: jdbc:mysql://rm-xxx.com:3306/nfturbo?useUnicode=true
      username: xxx
      password: xxx
  canalAdapters:
  - instance: example # canal instance Name or mq topic name
    groups:
    - groupId: g1
      outerAdapters:       
      - name: logger
      - name: es8
        hosts: localhost:9200 # 127.0.0.1:9200 for rest mode
        properties:
          mode: transport # or rest
          # security.auth: test:123456 #  only used for rest mode
          cluster.name: nfturbo-cluster
```

这里的数据库配置记得改：
```yml
srcDataSources:
    defaultDS:
      url: jdbc:mysql://rm-xxx.com:3306/nfturbo?useUnicode=true
      username: xxx
      password: xxx
  canalAdapters:
  - instance: example # canal instance Name or mq topic name
    groups:
    - groupId: g1
      outerAdapters:       
      - name: logger
      - name: es8
        hosts: localhost:9200 # 127.0.0.1:9200 for rest mode
        properties:
          mode: transport # or rest
          # security.auth: test:123456 #  only used for rest mode
          cluster.name: nfturbo-cluster
```

因为在outerAdapters下面，我们配置的 name 是 es8（如果你自己安装的是 es7，记得修改成对应的版本。），所以adapter将会自动加载 conf/es8 下的所有.yml结尾的配置文件，适配器表映射文件，创建并修改 conf/es8/mytest_user.yml文件:
```yml
dataSourceKey: defaultDS
destination: example
groupId: g1
esMapping:
  _index: nfturbo_users
  _id: _id
  #  upsert: true
  #  pk: id
  sql: "select t.id as _id, t.nick_name as  nick_name, t.state as state,t.telephone as telephone  from users as t"
  #  objFields:
  #    _labels: array:;
  #etlCondition: "where a.c_time>={}"
  commitBatch: 3000
```

3、启动canal.adapter

同样到 bin 目录下，执行命令：./startup.sh

启动成功(canal-adapter/logs/adapter/adapter.log)：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022159261.png)

4、到 es 上创建 ES 索引

访问 es（http://ip:5601/app/home#/ ），然后进入开发工具，在控制台创建如下索引：
```text
PUT nfturbo_users
{
  "mappings": {
    "properties": {
      "nickname": {
        "type": "text"
      },
      "telephone": {
        "type": "text"
      },
      "state": {
        "type": "text"
      }
    }
  }
}
```

5、数据库执行INSERT
```sql
INSERT INTO `nfturbo`.`users` (`id`,`gmt_create`,`gmt_modified`,`nick_name`,`password_hash`,`state`,`telephone`,`user_role`) VALUES (14,'2024-04-18 17:47:40','2024-04-18 17:47:42','test11111','c2975f0faec10adca0ecd729c8cbc0aa','INIT','13000000000','CUSTOMER')
```

可以在日志中看到监听到了相关变更。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022201719.png)

6、从 ES 查询：
```text
GET nfturbo_users/_search
{"_source": ["nick_name","telephone","state"],
  "query": {
    "match": {
      "nick_name": "test11111"
    }
  }
}
```

查询成功：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022201845.png)

### 注意事项

因为我用的 MySQL 是在阿里云上直接购买的 DMS，所以 不需要做单独的binlog dump配置，。如果是自己搭建的，需要开启 binlog 写入，并且给 canal账号授权他作为 slave 权限。

1、开启 Binlog 写入功能，配置 binlog-format 为 ROW 模式，my.cnf 中配置如下:
```properties
[mysqld]
log-bin=mysql-bin # 开启 binlog
binlog-format=ROW # 选择 ROW 模式
server_id=1 # 配置 MySQL replaction 需要定义，不要和 canal 的 slaveId 重复
```

重启 MySQL

2、授权 canal 链接 MySQL 账号具有作为 MySQL slave 的权限, 如果已有账户可直接 grant
```sql
CREATE USER canal IDENTIFIED BY 'canal';  
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
-- GRANT ALL PRIVILEGES ON *.* TO 'canal'@'%' ;
FLUSH PRIVILEGES;
```

### 常见问题

**1、could not be instantiated: class could not be found**

启动报错
```java
java.lang.IllegalStateException: Extension instance(name: es, class: interface com.alibaba.otter.canal.client.adapter.OuterAdapter)  could not be instantiated: class could not be found
```

这是因为对于 es，需要指定具体的版本才行，如 es8，详见：

﻿https://github.com/alibaba/canal/wiki/Sync-ES﻿

2、**Could not find first log file name in binary log index file**

﻿https://github.com/alibaba/canal/issues/156﻿

先关闭 canal，然后删除 meta.dat (在目录canal/conf/example下），然后再启动即可





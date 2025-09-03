

小伙伴们大家好，我先做个自我介绍，大家以后可以喊我老闫哈。第一次为技术派贡献文章给小伙伴们做分享真的是我的荣幸，此时真的心情比较小激动，由于水平有限，接下来在文章中那些做的不足的地方还恳请大家多多指正和担待。在这里先谢谢大家了。

这篇文章主要讲解 <font size = 5 color = "red">MySQL向ES(elasticsearch)做数据同步</font> ，其实同步数据有很多方式，有双写同步数据，异步同步数据；前者双写同步数据我们肯定不用的，它实现原理是同时向MySQL和ES中写入数据，这种性能慢不说，还存在二者还涉及到了分布式事务了，无法保证数据一致性问题，而且还将业务深深耦合起来了，无法做扩展，因此pass。后者异步同步数据方案比较多，比如目前市面上比较火的阿里的Canal和Debezium工具等等，他们都是利用的CDC(数据抓取变更)，监听binlog日志做的同步。由于后者Debezium需要集成Kafka，而且需要手写Kafka消费者代码去同步，使得系统更加复杂，实现起来相对Canal比较复杂，因此采用了阿里Canal去做数据同步。

# 1、基础知识
## 1.1、主从复制原理

MySQL 的主从复制是依赖于 binlog，也就是记录 MySQL 上的所有变化并以二进制形式保存在磁盘上二进制日志文件。

主从复制就是将 binlog 中的数据从主库传输到从库上，一般这个过程是异步的，即主库上的操作不会等待 binlog 同步地完成。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311261722600.png)

详细流程如下：
1. 主库写binlog：主库更新SQL（update、insert、delete）被写道binlog
2. 主库发送binlog：主库创建一个 log dump 线程发送binlog给从库
3. 从库写relay log：从库在连接到主节点时会创建一个IO线程，已请求主库更新的binlog，并且把接收到的binlog信息写入一个叫relay log的日志文件
4. 从库回放：从库还会建立一个SQL线程读取realy log中的内容，并且在从库中做回放，最终实现主从的一致性

## 1.2、Cannel基础
Canel是一款常用的数据同步工具，其原理是基于Binlog订阅的方式实现，**模拟一个MySQL Slave订阅Binlog日志，从而实现CDC（Change Data Capture）**，将已提交的更改发送到下游。

主要流程如下：
1. Canal服务端向MySQL的master节点传输dump协议
2. MySQL的master节点收到dump请求后推送binlog日志给Canal服务端，解析binlog对象（原始为byte流）转成Json格式
3. Canal客户端通过TCP协议或MQ形式监听Canal服务端，同步数据到ES
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311261747575.png)

下面斯Canal执行的核心流程，其中Binlog Parser主要负责Binlog的提取、解析和推送，EventSink负责数据的过滤、路由和加工，仅做了解即可。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311261751804.png)

# 2、软件下载安装

我的电脑是 Macos-x64，所以后面的软件安装，都是基于这个。

## 2.1、Java JDK

- 官网：https://www.oracle.com/java/technologies/downloads/
- JDK 版本：11.0.19

由于 Canal 和 ES 的安装，都强依赖 JDK，所以这里有必要先说明
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311261754438.png)

如果你选的版本不对，ES 安装可能会失败，然后 Canal 同步数据到 ES 时，也会出现很多诡异的问题。

## 2.2、MYSQL

MySQL 大家应该都安装了，这里需要打开 MySQL 的 BinLog。【我自己电脑好像没配置过，但是命令结果显示已经开启binlog了】

我是 Mac，主要新建一个 my.cnf 文件，然后再重启 MySQL。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311261754122.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311261812347.png)

这里重启 MySQL，我搞了半天，BinLog 开启后，会看到 BinLog 日志。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311261817203.png)

然后需要创建一个账号，账号和密码都是 Cannal，给后面 Canal 使用。
```sql
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'localhost' IDENTIFIED BY 'canal' ;

-- 上一条sql报错，改成了下面的sql
create user 'canal'@'localhost'  identified by 'canal';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'localhost';

-- 最后刷新
flush privileges;
```

## 2.3、Canal

- 官网：https://github.com/alibaba/canal/releases
- 版本：v1.1.6
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311261843517.png)

下载 canal.adapter 和 canal.deployer 两个就可以：
- canal.deployer：相当于 canal 的服务端，启动它才可以在客户端接收数据库变更信息。
- canal.adapter：增加客户端数据落地的适配及启动功能(当 deployer 接收到消息后，会根据不同的目标源做适配，比如是 es 目标源适配和 hbase 适配等等)。

备注：canal.admin 为 canal提供整体配置管理、节点运维等面向运维的功能，提供相对友好的 WebUI 操作界面，方便更多用户快速和安全的操作，我这边使用的是单机的，因此就没有下载安装，大家也可以拉 source code 源码去研究下。

## 2.4、ES

- ES 官网：https://www.elastic.co/cn/downloads/elasticsearch
- ES 版本：7.17.4

Mac 安装 ES 非常简单：
```shell
brew install elasticsearch
```

安装细节不赘述，安装成功后，输入以下网址：
```
http://localhost:9200/?pretty
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272142369.png)


## 2.5、Kibana

- 下载网址：https://www.elastic.co/cn/downloads/past-releases#kibana
- 版本：7.14.0

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272144896.png)

它是 ES 的界面化操作工具，安装细节不赘述，安装成功后，输入以下网址：
```
http://localhost:5601/app/dev_tools#/console
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272148535.png)

## 2.6、IK分词器

- 下载网址：https://github.com/medcl/elasticsearch-analysis-ik/releases/tag/v7.17.2
- 版本：v7.17.2（我安装的是7.17.4，和elasticsearch版本一样）

它是 ES 的分词器，安装细节不赘述，安装成功后，可以验证一下分词效果。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272149466.png)

## 2.7、小节

MySQL 开启 BinLog，这个不难，主要观察是否有 BinLog 日志。

Canal 的安装是最复杂的，涉及到很多配置修改，后面会讲解。

最后是 ES + Kibana + IK 分词器，这个其实也不难，主要关注 ES 绑定的 JDK 版本，三款软件的安装可以参考这篇：https://blog.csdn.net/weixin_46049028/article/details/129956485


# 3、Canal配置
## 3.1、canal.deployer配置

修改 conf—>example 文件夹的 instance.properties 监听的数据库配置。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272151903.png)

这里主要修改的监听 MySQL 的 URL、用户名和密码。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272151687.png)

这里默认的账号密码就是 canal，前面已经教大家如何创建了。

## 3.2、canal.deployer启动

在 canal.deployer 中的 bin 文件下去启动命令 startup.sh
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272151764.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272152817.png)

这样就代表已经启动了，我们可以去看下启动日志。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272152502.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272152858.png)

上面 start successful 代表已经启动成功，并且已经监听我的 MySQL 数据库。

## 3.3、canal.adapte配置

Step1: 先把 adapter 下面的 bootstrap.yml，全部注释掉，否则会提示你 XX 表不存在，这里坑了我好惨。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272154127.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272154898.png)

Step2: 再修改 adapter 的 application.yml 配置文件。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272154407.png)

这里的坑，一般就是 mysql 的账号密码不对，或者给的 es 链接，没有`"http://"`前缀，这些都是我过踩的坑。

Step3: 修改我们在 application.yml 中配置的目标数据源 es7 文件夹内容。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272155418.png)

由于我们这里是对表 article 进行去监听，因此我们在  es7 文件夹中去创建 article.yml 文件。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272155059.png)

由于我们需要把技术派项目中的文章查询功能，改造成 ES 的查询方式，所以我们就把技术派的文章表 article，同步到 ES 中。

yml文件配置如下
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272155775.png)

```java
dataSourceKey: defaultDS # 源数据源的key, 对应上面配置的srcDataSources中的值
destination: example  # canal的instance或者MQ的topic
groupId: g1 # 对应MQ模式下的groupId, 只会同步对应groupId的数据
esMapping:
  _index: article # es 的索引名称
  _id: _id  # es 的_id, 如果不配置该项必须配置下面的pk项_id则会由es自动分配
  sql: "SELECT t.id AS _id,t.id,t.user_id,t.article_type,t.title,t.short_title,t.picture,t.summary,t.category_id,t.source,t.source_url,t.offical_stat,t.topping_stat,t.cream_stat,t.`status`,t.deleted,t.create_time,t.update_time FROM article t"        # sql映射
  commitBatch: 1   # 提交批大小
```

Step4: 在 Kibana 中创建 ES 的 article 索引
代码如下：
```text
PUT /article
{
		"mappings" : {
			"properties" : {
			"id" : {
			"type" : "integer"
			},
			"user_id" : {
			"type" : "integer"
			},
			"article_type" : {
			"type" : "integer"
			},
			"picture" : {
			"type" : "text",
			"analyzer": "ik_max_word"
			},
			"summary" : {
			"type" : "text",
			"analyzer": "ik_max_word"
			},
			"category_id" : {
			"type" : "integer"
			},
			"source" : {
			"type" : "integer"
			},
			"source_url" : {
			"type" : "text",
			"analyzer": "ik_max_word"
			},
			"offical_stat" : {
			"type" : "integer"
			},
			"topping_stat" : {
			"type" : "integer"
			},
			"cream_stat" : {
			"type" : "integer"
			},
			"status" : {
			"type" : "integer"
			},
			"deleted" : {
			"type" : "integer"
			},
			"create_time" : {
			"type" : "date"
			},
			"update_time" : {
			"type" : "date"
			}
		}
	}
}

```

## 3.4、canal.adapte启动

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272202781.png)

我们看下启动日志：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272202282.png)

上面没有任何报错，并且已经启动了 8081 端口，说明已经启动成功，此时我们就可以操作了。


# 4、数据同步实战
## 4.1、全量同步

在开始 adapter 之后，我们应该先来一把全量数据同步，在源码中提供了一个接口进行全量同步，命令如下：
```shell
curl http://127.0.0.1:8081/etl/es7/article.yml -X POST
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272203439.png)

上面就是执行同步成功后，提示已经导入 10 条。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272206299.png)

## 4.2、增量同步

增量数据就是当我在 MySQL 中 update、delete 和 insert 时，那么 ES 中数据也会对应发生变化，我下面演示下修改：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272206694.png)

日志打印如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272206226.png)

ES查询结果如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272207140.png)

上面结果中说明 ES 已经更改成功。

# 5、总结

我们再回顾一下整体执行流程：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311272207277.png)

写到这里，就结束了，是不是满满的干货呢？基本是手把手教你如何将 MySQL 同步到 ES，不仅是增量同步，还包括全量同步，如果你的项目也需要用到该场景，基本可以直接照搬。

这个其实是技术派教程的其中一篇文章，类似的干货文章还有很多，也欢迎大家加入我们的星球哈。


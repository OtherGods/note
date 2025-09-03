RocketMQ 大家不陌生，是一个消息中间件，我们的项目中很多地方用了，比如订单关闭这里用了事务消息，上链成功后的订单状态推进也是基于 MQ 的。

所以，项目中如果想要全流程走通，需要部署MQ 的，如果不需要走所有流程，比如只验证网关、认证、用户等模块，则可以先不部署 rocketmq

## 配置要求

CPU：1核

内存：8G

### 下载RocketMQ

下载rocketmq：[https://rocketmq.apache.org/download](https://rocketmq.apache.org/download)

我下载的是5.1.4：wget https://dist.apache.org/repos/dist/release/rocketmq/5.1.4/rocketmq-all-5.1.4-bin-release.zip

下载后用unzip解压就行了

### 启动name server

`nohup sh bin/mqnamesrv -n "你的ip地址:9876" &`

### 启动Broker

修改conf/broker.conf，加入：brokerIP1=你的ip地址

`nohup ./bin/mqbroker -n localhost:9876 -c conf/broker.conf autoCreateTopicEnable=true &`

### 安装dashboard

1、安装docker
	[3、Docker & DokcerCompose安装](2、相关技术/24、项目/3、数藏项目/2、中间件部署/3、Docker%20&%20DokcerCompose安装.md)

2、安装dashboard：`docker pull apacherocketmq/rocketmq-dashboard:latest`

3、启动：
```shell
docker run -d --name rocketmq-dashboard -e "JAVA_OPTS=-Drocketmq.namesrv.addr=IP地址:9876 -Dcom.rocketmq.sendMessageWithVIPChannel=false" -p 8080:8080 -t apacherocketmq/rocketmq-dashboard:latest
```

4、开启端口：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408262329056.png)

5、访问 ： http://ip:8080/#/
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408262330572.png)

## 常见问题

### Not enough space
```shell
[root@iZ8vbjc1yb8tjr5y2npxrbZ rocketmq-all-5.1.4-bin-release]# sh bin/mqnamesrv

Java HotSpot(TM) 64-Bit Server VM warning: INFO: os::commit_memory(0x0000000700000000, 4294967296, 0) failed; error='Not enough space' (errno=12)

#

# There is insufficient memory for the Java Runtime Environment to continue.

# Native memory allocation (mmap) failed to map 4294967296 bytes for committing reserved memory.

# An error report file with more information is saved as:

# /root/package/rocketmq-all-5.1.4-bin-release/hs_err_pid4583.log
```

内存不够，修改bin/runserver.sh【NameServer】，把内存调小一点。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408262332468.png)

内存不够，修改bin/runbroker.sh【Broker】，把内存调小一点。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408262333567.png)

### Brocker 启动报错：Killed $JAVA ${JAVA_OPT} $@

这也是因为可用内存不足导致的，和上面一样调整内存

### Unrecognized VM option 'UseBiasedLocking'

```shell
Unrecognized VM option 'UseBiasedLocking'

Error: Could not create the Java Virtual Machine.

Error: A fatal exception has occurred. Program will exit.

[2]+ Exit 1 sh bin/mqbroker -n localhost:9876
```

偏向锁被废弃了，移除这个VM参数

打开rocketmq-all-5.1.4-bin-release/bin/runbroker.sh，移除参数即可。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408262337015.png)

### org.apache.rocketmq.remoting.exception.RemotingConnectException: connect to <127.0.0.1:9876> failed

在控制台上无法访问broker，报错如上。

可能是启动控制台的时候ip地址不对。如果按照官方文档，你用的是
```shell
docker run -d --name rocketmq-dashboard -e "JAVA_OPTS=-Drocketmq.namesrv.addr=127.0.0.1:9876" -p 8080:8080 -t apacherocketmq/rocketmq-dashboard:latest
```

那么就会有问题。因为，docker容器 是一个容器，doccker容器 与 宿主机 可以看成是两个独立的服务器，在 docker 容器内访问 127.0.0.1或localhost都是访问的这个docker容器，如果要访问宿主机必须使用宿主机IP
```shell
docker run -d --name rocketmq-dashboard -e "JAVA_OPTS=-Drocketmq.namesrv.addr=IP地址:9876 -Dcom.rocketmq.sendMessageWithVIPChannel=false" -p 8081:8080 -t apacherocketmq/rocketmq-dashboard:latest
```

```shell
docker run --name rmqconsole --link rmqserver:namesrv -e "JAVA_OPTS=-Drocketmq.namesrv.addr=IP地址:9876 -Dcom.rocketmq.sendMessageWithVIPChannel=false" -p 8180:8080 -t styletang/rocketmq-console-ng
```

### RocketMQ连接异常sendDefaultImpl call timeout

装并启动好RocketMQ后，在代码中远程连接RocketMQ，报以下错误：
```java
org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException:sendDefaultImpl call timeout at org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl.sendDefaultImpl(DefaultMQProducerImpl.java:588)at org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl.send(DefaultMQProducerImpl.java:1223)at org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl.send(DefaultMQProducerImpl.java:1173)at org.apache.rocketmq.client.producer.DefaultMQProducer.send(DefaultMQProducer.java:214)at com.flying.demo.Producer.main(Producer.java:25)
```

这个错误其实是启动RocketMQ的namesrv，broker没有指IP。

假如IP是：192.168.1.135

那么启动namesrv时，用以下方法：
```shell
nohup sh bin/mqnamesrv -n "192.168.1.135:9876" &
```

启动broker时，用以下方法：

修改conf/broker.conf，加入：brokerIP1=192.168.1.135

启动：
```shell
nohup ./bin/mqbroker -n localhost:9876 -c conf/broker.conf autoCreateTopicEnable=true &
```


可以参考文章
- 官网：[本地部署 RocketMQ](https://rocketmq.apache.org/zh/docs/quickStart/01quickstart/)
- 博客：[RocketMQ 安装教程 (Docker)](https://www.jansora.com/notebook/107520)




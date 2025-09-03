大家好，我是**华仔**, 又跟大家见面了。

从今天开始我会带领大家去学习 **RocketMQ 安装篇**。

# **01 总述**

通过上一篇的揭秘，我们可以了解到在 RocketMQ 中有 「**NameServer**」、「**Broker**」、「**生产者**」、「**消费者**」等四种角色，对于生产者、消费者来说都是业务系统，搭建环境的时候不需要进行搭建，而真正需要搭建的就是 「**NameServer**」、「**Broker**」。为了更好的了解 RocketMQ，这里会搭建一套可视化的服务。

搭建的过程相对比较简单，这里就按照步骤一步一步完成即可。如果提示一些命令不存在的话，可以通过 yum 命令安装即可。

# **02 环境搭建**
## **2.1、准备工作**

这里我们需要准备一台 Linux 服务器，需要提前安装好 JDK。

关闭下防火墙。
```shell
# 关闭防火墙
systemctl stop firewalld

# 设置开机禁用防火墙
systemctl disable firewalld
```

接下来，我们就可以下载 RocketMQ 了。

### **2.1.1、创建目录**

```shell
mkdir /home/wangjianghua/src/rocketmq && cd /home/wangjianghua/src/rocketmq
```

### **2.1.2、下载并解压 RocketMQ**

官网下载地址： [https://archive.apache.org/dist/rocketmq/](https://archive.apache.org/dist/rocketmq/)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410051818594.png)

这里以 4.9.4 版本为例：
```shell
# 下载
wget https://archive.apache.org/dist/rocketmq/4.9.4/rocketmq-all-4.9.4-bin-release.zip

# 解压
unzip rocketmq-all-4.9.4-bin-release.zip
```

看到 rocketmq-all-4.9.4-bin-release/ 文件夹说明解压完成了。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410051819470.png)

然后进入 rocketmq-all-4.9.4-bin-release 文件夹

```shell
cd rocketmq-all-4.9.4-bin-release
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410051820828.png)

## **2.2、搭建 NameServer**
### **2.2.1、修改 jvm 参数**

在启动 NameServer 之前，强烈建议修改一下启动时的 jvm 参数，因为默认的参数都比较大，为了避免内存不够，建议修改小，当然，如果你的内存足够大，可以忽略。

```shell
vi bin/runserver.sh
```

找到如下图红框的这一行进行修改
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410051820565.png)

这里你可以直接修改成跟我一样的配置，点保存。
```shell
-server -Xms512m -Xmx512m -Xmn256m -XX:MetaspaceSize=32m -XX:MaxMetaspaceSize=50m
```

### **2.2.2、启动 NameServer**

修改完 jvm 参数之后，执行如下命令就可以启动 NameServer 了。
```shell
nohup sh bin/mqnamesrv &
```

查看 NameServer 输出日志。
```shell
tail -f ~/logs/rocketmqlogs/namesrv.log
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052141680.png)

至此 NameServer 服务就启动完毕了。

## **2.3、搭建 Broker**
### **2.3.1、修改 jvm 参数**

同上面启动 NameServer 一样，这里也建议去修改 jvm 参数。
```shell
vi bin/runbroker.sh
```

找到如下图红框的这一行进行修改，设置小点，当然也别太小啊。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052143440.png)

这里你可以直接修改成跟我一样的配置，点保存。
```shell
-server -Xms1g -Xmx1g -Xmn512m
```

### **2.3.2、修改 Broker 配置文件**

这里需要改一下 Broker 对应的配置文件，需要指定 NameServer 的地址，因为 Broker 需要往 NameServer 进行注册。
```shell
vi conf/broker.conf
```

Broker配置文件内容如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052152608.png)

通过上面就可以看出 Broker 都配置了哪些信息，例如：「**Broker集群名称**」、「**Broker名称**」、「**Broker Id**」、「**Broker 角色**」、「**刷写磁盘类型**」等。

这里在文件末尾追加如下地址：
```shell
namesrvAddr = localhost:9876
```

这里本地搭建，NameServer 跟 Broker 都在同一台机器上，所以配置是 **localhost** ，NameServer 端口默认的是**9876**。

不过我还建议大家再修改一处信息，因为 Broker 向 NameServer 注册时，带过去的 ip 如果不指定就会自动获取，**但是自动获取的有个坑，就是有可能你的电脑无法访问到这个自动获取的 ip，所以我建议手动指定你的电脑可以访问到的服务器 ip**。

我的虚拟机的 ip 是 192.168.56.1，所以就指定为 192.168.56.1，如下：
```shell
# 这里以我本地为例，我本地目前是一台，所以写了一样的，真实环境按真实ip填写
brokerIP1 = 192.168.56.1
brokerIP2 = 192.168.56.1
```

![](https://article-images.zsxq.com/FsXpgNMFnbBwZ311obHmwaldBlaj)

### **2.3.3、启动 Broker 服务**

```shell
# -c 参数就是指定配置文件
nohup sh bin/mqbroker -c conf/broker.conf &
```

查看 Broker 输出日志：
```shell
tail -f ~/logs/rocketmqlogs/broker.log
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052201690.png)

至此 Broker 服务就启动完毕了。

### **2.3.4、查看 RocketMQ 进程**

执行 jps 查看对应进程。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052202426.png)

### **2.3.5、关闭 RocketMQ**

```shell
# 关闭 broker
bin/mqshutdown broker

# 关闭 nameserver
bin/mqshutdown namesrv
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052218827.png)

# **03 搭建可视化监控控制台**

其实前面 NameServer 和 Broker 搭建完成之后，就可以用来收发消息了，这里为了更加直观，搭一套可视化的服务帮助你更好的理解。

下载地址：[https://github.com/apache/rocketmq-externals](https://github.com/apache/rocketmq-externals)

RocketMQ 控制台需要自己去下载编译打包，可以在编译时设置其 namesrvAddr 和端口号，也可以在程序启动时指定这些参数（SpringBoot 项目）
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052219331.png)

使用 maven 打包
```shell
mvn clean package -Dmaven.test.skip=true
```

如果你打包失败的话，也可以使用下面方式安装。

说白了可视化服务其实就是一个 jar 包，启动就行了。
`链接: https://pan.baidu.com/s/1Yci-Lt5i_fW3plolfG5HNA?pwd=jpr6 提取码: jpr6`

下载后将 jar 包上传到服务器，放到 [/home/wangjianghua/src/rocketmq](http://home/wangjianghua/src/rocketmq) 的目录下，你可以放到任意可以找到的位置。

**这里依赖的JDK，最好是 jdk8，测试jdk11启动不起来，没找到解决办法。**

然后进入 /home/wangjianghua/src/rocketmq 目录下，执行命令：
```shell
nohup java -jar -server -Xms256m -Xmx256m -Drocketmq.config.namesrvAddr=localhost:9876 -Dserver.port=8088 rocketmq-console-ng-1.0.1.jar &
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052243331.png)

其中 rocketmq.config.namesrvAddr 用来指定 NameServer 地址的。

查看对应输出日志：
```shell
tail -f ~/logs/consolelogs/rocketmq-console.log
```

当看到如下日志，就说明启动成功了。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052244187.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052244906.png)

此时你可以在浏览器中输入 http://192.168.56.1:8088/#/ ，这里可以换成你自己的 Linux 服务器的 IP。就可以看到控制台了，如果无法访问，可以看看防火墙有没有关闭。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052245078.png)

在右上角可以把语言切换成中文。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052245206.png)

通过控制台可以查看生产者、消费者、Broker集群、主题、消息、消息轨迹等信息，非常直观。

# **04 测试**

RocketMQ 默认提供发送 1000 条写的测试，运行测试。启动生产者发送消息, 消费者接收消息。
```shell
# 添加临时的环境变量 通过环境变量, 告诉客户端程序name server的地址
export NAMESRV_ADDR=localhost:9876

cd /home/wangjianghua/src/rocketmq/rocketmq-all-4.9.4-bin-release

# 启动生产者来测试发送消息
sh bin/tools.sh org.apache.rocketmq.example.quickstart.Producer

# 启动消费者来测试接收消息
sh bin/tools.sh org.apache.rocketmq.example.quickstart.Consumer
```

执行后如下图所示，表示生产和消费成功。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052247424.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052247583.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052249561.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052249601.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410052249308.png)







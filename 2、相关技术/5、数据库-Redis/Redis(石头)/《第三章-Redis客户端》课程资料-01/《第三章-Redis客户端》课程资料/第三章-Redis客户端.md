## 第三章-Redis客户端
#### 1.客户端简介
Redis 客户端是一个程序，通过网络连接到 Redis 服务器， 在客户端软件中使用 Redis可以识别的命令，向 Redis 服务器发送命令， 告诉 Redis 想要做什么。Redis 把处理结果显示在客户端界面上。 通过 Redis 客户端和 Redis 服务器交互。Redis 客户端发送命令，同时显示 Redis 服务器的处理结果。
#### 2.Redis 命令行客户端
redis-cli （Redis Command Line Interface）是 Redis 自带的基于命令行的 Redis 客户端，
用于与服务端交互，我们可以使用该客户端来执行 redis 的各种命令。
两种常用的连接方式：

**A、直接连接 redis (默认 ip127.0.0.1，端口 6379)：./redis-cli**
在 redis 安装目录\src, 执行 ./redis-cli
此命令是连接本机 127.0.0.1 ，端口 6379 的 redis

**B、 指定 IP 和端口连接 redis：./redis-cli -h 127.0.0.1 -p 6379**
-h redis 主机 IP（可以指定任意的 redis 服务器）
-p 端口号（不同的端口表示不同的 redis 应用）
在 redis 安装目录\src, 执行 ./redis-cli -h 127.0.0.1 -p 6379

例 1：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613122040523.png)
#### 3.图形界面客户端
Redis Desktop Manager：C++ 编写，响应迅速，性能好。
官网地址： https://redisdesktop.com/
github: https://github.com/uglide/RedisDesktopManager
使用文档：http://docs.redisdesktop.com/en/latest/
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613122138662.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
点击“DOWNLOAD”
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613122202440.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**A、安装客户端软件**
在 Windows 系统使用此工具，连接 Linux 上或 Windows 上的 Redis , 双击此 exe 文件执
行安装
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613122248787.png)
**B、安装后启动界面**
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019061312234690.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**C、配置 Redis Desktop Manamager(RDM)，连接 Redis**
在 RDM 的主窗口，点击左下的“Connect to Redis Server”
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613122526634.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
连接成功后：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613122551460.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
#### 4.Redis 编程客户端
**A、Jedis**
redis 的 Java 编程客户端，Redis 官方首选推荐使用 Jedis，jedis 是一个很小但很健全的
redis 的 java 客户端。通过 Jedis 可以像使用 Redis 命令行一样使用 Redis。
jedis 完全兼容 redis 2.8.x and 3.x.x
Jedis 源码：https://github.com/xetorthio/jedis
api 文档：http://xetorthio.github.io/jedis/

**B、SpringData Redis**
Spring-data-redis是spring大家族的一部分，提供了在srping应用中通过简单的配置访问redis服务，对reids底层开发包(Jedis, JRedis, and RJC)进行了高度封装，RedisTemplate提供了redis各种操作、异常处理及序列化，支持发布订阅，并对spring 3.1 cache进行了实现。

**C、 Lettuce:**
Lettuce 是 一 个 可 伸 缩 线 程 安 全 的 Redis 客 户 端 。 多 个 线 程 可 以 共 享 同 一 个
RedisConnection。它能够高效地管理多个连接。
Lettuce 源码：https://github.com/lettuce-io/lettuce-core

**D、 redis 的其他编程语言客户端:**
C 、C++ 、C# 、Erlang、Lua 、Objective-C 、Perl 、PHP 、Python 、Ruby 、Scala 、
Go 等 40 多种语言都有连接 redis 的编程客户端
#### 5.Redis 客户端连接超时解决
远程连接redis服务，需要关闭或者修改防火墙配置。

1.将修改后的端口添加到防火墙中.

```
/sbin/iptables -I INPUT -p tcp --dport 8081 -j ACCEPT
/etc/rc.d/init.d/iptables save
```

第一步：编辑iptables  /etc/sysconfig/iptables

    #vim

在命令模式下，选定要复制的那一行的末尾，然后点击键盘yyp，就完成复制，然后修改。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613123149412.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
第二步：重启防火墙

```
# service iptables restart
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613123225725.png)
注意：
默认一共是16个数据库，每个数据库之间是相互隔离。数据库的数量是在redis.conf中配置的
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613123311941.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
切换数据库使用命令：select 数据库编号
例如：select 1【相当于mysql 的use databasename】
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613123327275.png)




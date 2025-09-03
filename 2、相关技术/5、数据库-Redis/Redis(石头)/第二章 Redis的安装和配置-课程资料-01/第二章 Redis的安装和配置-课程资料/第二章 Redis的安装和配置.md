### 第二章 Redis的安装和配置
#### 1.Redis介绍
##### 1.1 Redis历史发展
&emsp;&emsp;2008年，意大利的一家创业公司Merzia推出了一款基于MySQL的网站实时统计系统LLOOGG，然而没过多久该公司的创始人 Salvatore Sanfilippo便 对MySQL的性能感到失望，于是他决定亲自为LLOOGG量身定做一个数据库，并于2009年开发完成，这个数据库就是Redis。 不过Salvatore Sanfilippo并不满足只将Redis用于LLOOGG这一款产品，而是希望更多的人使用它，于是在同一年Salvatore Sanfilippo将Redis开源发布，并开始和Redis的另一名主要的代码贡献者Pieter Noordhuis一起继续着Redis的开发，直到今天。
&emsp;&emsp;Salvatore Sanfilippo自己也没有想到，短短的几年时间，Redis就拥有了庞大的用户群体。Hacker News在2012年发布了一份数据库的使用情况调查，结果显示有近12%的公司在使用Redis。国内如新浪微博、街旁网、知乎网，国外如GitHub、Stack Overflow、Flickr等都是Redis的用户。
&emsp;&emsp;VMware公司从2010年开始赞助Redis的开发， Salvatore Sanfilippo和Pieter Noordhuis也分别在3月和5月加入VMware，全职开发Redis。
&emsp;&emsp;Redis的代码托管在GitHub上[https://github.com/antirez/redis](https://github.com/antirez/redis)，开发十分活跃，代码量只有3万多行。
##### 1.2.Redis的简介
&emsp;&emsp;Remote Dictionary Server(Redis) 是一个开源的使用 ANSI C 语言编写、支持网络、可基于内存亦可持久化的 Key-Value 数据库. Key 字符类型，其值（value）可以是 字符串(String), 哈
希(Map), 列表(list), 集合(sets) 和 有序集合(sorted sets)等类型，每种数据类型有自己的专属命令。所以它通常也被称为数据结构服务器。
&emsp;&emsp;Redis 的作者是 Salvatore Sanfilippo，来自意大利的西西里岛，现在居住在卡塔尼亚。目
前供职于 Pivotal 公司（Pivotal 是 Spring 框架的开发团队），Salvatore Sanfilippo 被称为 Redis
之父。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190604233131575.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
官网：https://redis.io/
github: https://github.com/antirez/redis
中文：http://www.redis.cn/
&emsp;&emsp;&emsp;http://www.runoob.com/redis/redis-tutorial.html

Redis 与其他 key - value 缓存产品有以下三个特点
i）Redis支持数据的持久化，可以将内存中的数据保持在磁盘中，重启的时候可以再次加载进行使用
ii）Redis不仅仅支持简单的key-value类型的数据，同时还提供list，set，zset，hash等数据结构的存储
iii）Redis支持数据的备份，即master-slave模式的数据备份

##### 1.3.Redis优势
- 性能极高 – Redis能读的速度是110000次/s,写的速度是81000次/s 。
- 丰富的数据类型 – Redis支持二进制案例的 Strings, Lists, Hashes, Sets 及 Ordered Sets 数据类型操作。
- 原子 – Redis的所有操作都是原子性的，同时Redis还支持对几个操作全并后的原子性执行。
- 丰富的特性 – Redis还支持 publish/subscribe, 通知, key 过期等等特性
##### 1.4.Redis的应用场景
- 缓存（数据查询、短连接、新闻内容、商品内容等等）（最多使用）
- 分布式集群架构中的session分离。
- 聊天室的在线好友列表。
- 任务队列。（秒杀、抢购、12306等等）
- 应用排行榜。
- 网站访问统计。
- 数据过期处理（可以精确到毫秒）
#### 2.Window 上安装 Redis
Windows 版本的 Redis 是 Microsoft 的开源部门提供的 Redis. 这个版本的 Redis 适合开发
人员学习使用，生产环境中使用 Linux 系统上的 Redis
##### 2.1.Redis下载
官网地址：http://redis.io/
windows 版本：https://github.com/MSOpenTech/redis/releases
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019060423451937.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
##### 2.2.Redis安装
下载的 Redis-x64-3.2.100.zip 解压后，放到某个目录（例如 d:\tools\），即可使用。
目录结构：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613114621281.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
##### 2.3.Redis启动
A、Windows7 系统双击 redis-server.exe 启动 Redis
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613114749992.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
B、 Windows 10 系统
有的机器双击 redis-server.exe 执行失败，找不到配置文件，可以采用以下执行方式：
在命令行（cmd）中按如下方式执行：
D:\tools\Redis-x64-3.2.100>redis-server.exe redis.conf
如图：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613114911752.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
##### 2.4.Redis关闭
按 ctrl+c 退出 Redis 服务程序。

#### 3.Linux 上安装 Redis
##### 3.1 Linux版Redis下载
官网地址：http://redis.io/
下载地址：http://download.redis.io/releases/redis-3.0.0.tar.gz
 在Linux中使用wget下载到linux或者下载到window在上传到linux
> wget http://download.redis.io/releases/redis-3.0.0.tar.gz

##### 3.2 Linux版Redis安装
Redis是C语言开发，建议在linux上运行，本教程使用Centos6.5作为安装环境。
第一步：在VMware中安装CentOS（参考Linux教程中的安装虚拟机）
第二步：在Linux下安装gcc环境（该步骤可以省略，CentOS中默认自带C环境）

    # yum install gcc-c++
    
可以通过rpm -qa | grep gcc 来查询是否已经安装了gcc

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613120116492.png)
第三步：将下载的Redis源码包上传到Linux服务器中【如果是linux直接下载的，就省略这个步骤】
第四步：解压缩Redis源码包

    # tar -zxf redis-3.0.0.tar.gz 【直接解压到当前文件夹】
第五步：编译redis源码

```
# cd redis-3.0.0
# make
```
第六步：安装redis

    # make install PREFIX=/usr/local/redis
##### 3.3 Linux版Redis启动
###### 3.3.1 Linux版Redis前端启动
直接运行bin/redis-server将以前端模式启动。【bin目录是在/usr/local/redis/bin】

    # ./redis-server   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613120826837.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
   ssh命令窗口关闭则redis-server程序结束，不推荐使用此方法
   
###### 3.3.2 Linux版Redis前端关闭
前端启动的关闭：ctrl+c

###### 3.3.3 Linux版Redis后端启动
第一步：将redis源码包中的redis.conf配置文件复制到/usr/local/redis/bin/下

    # cd /root/redis-3.0.0
    # cp redis.conf /usr/local/redis/bin/
第二步：修改redis.conf，将daemonize由no改为yes

    # vi redis.conf
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613120543683.png)
第三步：执行命令

    # ./redis-server redis.conf
###### 3.3.4 Linux版Redis后端关闭
方式1：
① 使用 redis 客户端关闭， 向服务器发出关闭命令切换到 redis-3.2.9/src/ 目录，执行 ./redis-cli shutdown 推荐使用这种方式， redis 先完成数据操作，然后再关闭。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613121156945.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
方式2：
② kill pid 或者 kill -9 pid
 这种不会考虑当前应用是否有数据正在执行操作，直接就关闭应用。先使用 ps -ef | grep redis 查出进程号， 在使用 kill pid
 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613121238773.png)
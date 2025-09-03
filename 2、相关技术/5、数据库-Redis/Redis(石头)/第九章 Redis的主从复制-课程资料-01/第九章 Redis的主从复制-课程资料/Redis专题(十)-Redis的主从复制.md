### 1.主从复制概念（master/slave）

持久化保证了即使redis服务重启也不会丢失数据，因为redis服务重启后会将硬盘上持久化的数据恢复到内存中，但是当redis服务器的硬盘损坏了可能会导致数据丢失，如果通过redis的主从复制机制就可以避免这种单点故障，如下图：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171250764.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
说明：

- 主redis中的数据有两个副本（replication）即从redis1和从redis2，即使一台redis服务器宕机其它两台redis服务也可以继续提供服务。
- 主redis中的数据和从redis上的数据保持实时同步，当主redis写入数据时通过主从复制机制会复制到两个从redis服务上。
- 只有一个主redis，可以有多个从redis。
- 主从复制不会阻塞master，在同步数据时，master 可以继续处理client 请求

### 2.主从复制（master/slave）实现

修改配置文件，启动时，服务器读取配置文件，并自动成为指定服务器的从服
务器，从而构成主从复制的关系

**A、新建三个Redis的配置**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171342707.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**B、修改三个Redis的配置端口**

**C、修改其中2个从Redis的配置**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171403388.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**D、启动服务器 Master/Slave 都启动**
启动方式 ./redis-server 配置文件
启动 Redis,并查看启动进程
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171417102.png)
**E、 查看配置后的服务信息**
命令：
①： Redis 客户端使用指定端口连接 Redis 服务器
 ./bin/redis-cli -h 192.168.6.129 -p 6379
②：查看服务器信息
info replication
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171430187.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
在新的窗口分别登录到 6380 ，6381 查看信息
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171439998.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**F、 向 Master 写入数据**
在 6379 执行 flushall 清除数据，避免干扰的测试数据。 生产环境避免使用
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171541726.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**G、在从 Slave 读数据**
6380,6380都可以读主 Master 的数据，不能写
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171600169.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
Slave 写数据失败
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171609755.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

### 3.主从复制（master/slave）容灾处理

当Master 服务出现故障，需手动将 slave 中的一个提升为 master，剩下的 slave 挂至新的
master 上（冷处理：机器挂掉了，再处理）
命令：
①：slaveof no one，将一台 slave 服务器提升为 Master （提升某 slave 为 master）
②：slaveof 127.0.0.1 6381 （将 slave 挂至新的 master 上）

执行步骤：
**A、将 Master:6379停止（模拟挂掉）**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171654503.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171659821.png)
**B、选择一个 Slave 升到 Master，其它的 Slave 挂到新提升的 Master**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171715206.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**C、 将其他 Slave 挂到新的 Master**
在 Slave 6380 上执行
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171729646.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
现在的主从（Master/Slave）关系：Master 是 6381 ， Slave 是 6380
查看 6381 ：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171739661.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**D、原来的服务器重新添加到主从结构中**
6379的服务器修改后，从新工作，需要把它添加到现有的 Master/Slave 中
先启动 6379 的 Redis 服务
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171802552.png)
连接到 6379 端口
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171811849.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
新增加的默认是master
当前服务挂到 Master上
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171825653.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**E、 查看新的 Master 信息**
在 6381 执行
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620171843517.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
现在的 Master/Slaver 关系是：
 Master: 6381
 Slave: 6380
      6379

### 4.总结
1、一个 master 可以有多个 slave
2、slave 下线，读请求的处理性能下降
3、master 下线，写请求无法执行
4、当 master 发生故障，需手动将其中一台 slave 使用 slaveof no one 命令提升为 master，其
它 slave 执行 slaveof 命令指向这个新的 master，从新的 master 处同步数据
5、主从复制模式的故障转移需要手动操作，要实现自动化处理，这就需要 Sentinel 哨兵，
实现故障自动转移

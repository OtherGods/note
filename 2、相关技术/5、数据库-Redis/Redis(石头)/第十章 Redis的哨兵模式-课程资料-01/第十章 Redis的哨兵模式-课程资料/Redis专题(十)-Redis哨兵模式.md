### 1.高可用Sentinel哨兵介绍
Sentinel哨兵是redis官方提供的高可用方案，可以用它来监控多个Redis服务实例的运
行情况。RedisSentinel是一个运行在特殊模式下的Redis服务器。RedisSentinel是在多个
Sentinel进程环境下互相协作工作的。
Sentinel系统有三个主要任务：
- 监控：Sentinel不断的检查主服务和从服务器是否按照预期正常工作。
- 提醒：被监控的Redis出现问题时，Sentinel会通知管理员或其他应用程序。
- 自动故障转移：监控的主Redis不能正常工作，Sentinel会开始进行故障迁移操作。将
一个从服务器升级新的主服务器。让其他从服务器挂到新的主服务器。同时向客户端
提供新的主服务器地址。
### 2.高可用Sentinel哨兵环境搭建
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019062017231214.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**（1）Sentinel 配置文件**
复制三份sentinel.conf文件
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172350802.png)
**（2） 三份 sentinel 配置文件修改**：
1、修改 `port 26379`、 `port 26380`、 `port 26381`

2、修改 `sentinel monitor mymaster 127.0.0.1 6380 2`
格式：`Sentinel monitor <name> <masterIP> <masterPort><Quorum 投票数>`
`Sentinel`监控主(Master)`Redis`, `Sentinel`根据`Master`的配置自动发现`Master`的`Slave`,`Sentinel`
默认端口号为`26379`

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172405855.png)
sentinel26380.conf
1)修改 port
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172503874.png)
2）修改监控的 master 地址
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172513198.png)
sentinel26379.conf 修改port 26379 , master的port 6381
sentinel26381.conf 修改port 26381 , master的port 6381

（3） 启动主从（Master/Slave）Redis
启动 Reids
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172532414.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
（4） 启动 Sentinel
redis安装时make编译后就产生了redis-sentinel程序文件，可以在一个redis中运行多个
sentinel进程。
启动一个运行在Sentinel模式下的Redis服务实例
./redis-sentinel sentinel 配置文件
执行以下三条命令，将创建三个监视主服务器的Sentinel实例：

    ./redis-sentinel   ../sentinel26379.conf

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172602895.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

    ./redis-sentinel ../sentinel26380.conf
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172619726.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

    ./redis-sentinel ../sentinel26381.conf
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172635224.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
（5） 主 Redis 不能工作
让 Master 的 Redis 停止服务， 执行 shutdown
先执行 info replication 确认 Master 的 Redis ，再执行 shutdown
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172644842.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
（6） Sentinel 的起作用
在 Master 执行 shutdown 后，稍微等一会 Sentinel 要进行投票计算，从可用的 Slave
选举新的 Master。
查看 Sentinel 日志，三个 Sentinel 窗口的日志是一样的。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172654637.png)
查看新的 Master
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172703581.png)
查看原 Slave 的变化
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172714303.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
（7） 新的 Redis 加入 Sentinel 系统，自动加入 Master
重新启动 6381
查看 6380 的信息
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172723189.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
测试数据：在 Master 写入数据
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172731906.png)
在 6381 上读取数据，不能写入
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190620172741156.png)
（8）监控
1）Sentinel会不断检查Master和Slave是否正常
2）如果Sentinel挂了，就无法监控，所以需要多个哨兵，组成Sentinel网络，一个健康的
Sentinel至少有3个Sentinel应用。彼此在独立的物理机器或虚拟机。
3）监控同一个Master的Sentinel会自动连接，组成一个分布式的Sentinel网络，互相通信
并交换彼此关于被监控服务器的信息
4）当一个Sentinel认为被监控的服务器已经下线时，它会向网络中的其它Sentinel进行确
认，判断该服务器是否真的已经下线
5）如果下线的服务器为主服务器，那么Sentinel网络将对下线主服务器进行自动故障转移，
通过将下线主服务器的某个从服务器提升为新的主服务器，并让其从服务器转移到新的主服
务器下，以此来让系统重新回到正常状态
6）下线的旧主服务器重新上线，Sentinel会让它成为从，挂到新的主服务器下

（9）总结
主从复制，解决了读请求的分担，从节点下线，会使得读请求能力有所下降，Master下
线，写请求无法执行
Sentinel会在Master下线后自动执行故障转移操作，提升一台Slave为Master，并让其它
Slave成为新Master的Slave

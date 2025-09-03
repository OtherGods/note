### 1.MySQL主从复制概念

MySQL 主从复制是指**数据可以从一个MySQL数据库服务器**主节点复制到一个或多个从节点。**MySQL 默认采用异步复制方式**，这样从节点不用一直访问主服务器来更新自己的数据，数据的更新可以在远程连接上进行，从节点可以复制主数据库中的**所有数据库**或者特定的数据库，或者特定的表。

> 优点：
>
> 1. 避免单点故障造成数据丢失
> 2. 本质数据备份
> 3. 如果想搭建读写分离，必须先主从复制！

### 2.MySQL中复制的优点包括
**横向扩展解决方案** - 在多个从站之间分配负载以提高性能。在此环境中，所有写入和更新都必须在主服务器上进行。但是，读取可以在一个或多个从设备上进行。该模型可以提高写入性能（因为主设备专用于更新），同时显着提高了越来越多的从设备的读取速度。

**数据安全性** - 因为数据被复制到从站，并且从站可以暂停复制过程，所以可以在从站上运行备份服务而不会破坏相应的主数据。

**分析** - 可以在主服务器上创建实时数据，而信息分析可以在从服务器上进行，而不会影响主服务器的性能。

**远程数据分发** - 您可以使用复制为远程站点创建数据的本地副本，而无需永久访问主服务器。

![1608273467781](D:\Tyora\AssociatedPicturesInTheArticles\01-Mysql主从复制(MySQL Replication)\1608273467781.png)

### 3.复制最大问题

延时

### 4.Replication的原理(面试题)

前提是作为主服务器角色的数据库服务器必须**开启二进制日志**
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\01-Mysql主从复制(MySQL Replication)\20200306134929888.png)

主服务器上面的任何修改都会通过自己的 I/O tread(I/O 线程)保存在二进制日志 Binary log 里面，I/O进程根据请求的信息读取指定的⽇志位置之后的⽇志信息，返回给从节点。【这里说的有些问题，视频中讲的是：这个线程会将修改保存在日志文件中，但是在代码随想录的八股文中说这个线程是给从服务器发送日志】

从服务器上面也启动一个 **I/O thread**，通过配置**好的用户名和密码,** 连接到主服务器上面请求读取二进制日志，然后把读取到的二进制日志写到本地的一个Realy log（中继日志）里面。

从服务器上面同时开启一个 SQL thread 定时检查 Realy log(这个文件也是二进制的)，如果发现有更新立即把更新的内容在本机的数据库上面执行一遍。

每个从站(从服务器)都会记录二进制日志坐标：

```
文件名
文件中它已经从主站读取和处理的位置。
```

由于每个从服务器都分别记录了自己当前处理二进制日志中的位置，因此可以断开从服务器的连接，重新连接然后恢复继续处理。

#### 4.1主从复制基本原则

- 每个slave只有一个master
- 每个slave只能有一个唯一的服务器ID
- 每个master可以有多个slave

#### 4.2三步骤

- master将改变记录到二进制日志（binary log）。这些记录过程叫做二进制日志事件，binary log events
- slave将master的binary log ebents拷贝到它的中继日志（relay log）
- slave重做中继日志中的时间，将改变应用到自己的数据库中。MySQL复制是异步的且串行化的

> 代码随想录中的对应的步骤是：
>
> ![image-20220911214058116](D:\Tyora\AssociatedPicturesInTheArticles\01-Mysql主从复制(MySQL Replication)\image-20220911214058116.png)

### 5.Mysql主从配置步骤

**主机配置**
修改配置文件： vi /etc/my.cnf

```shell
#主服务器唯一ID
server-id=1
#启用二进制日志
log-bin=mysql-bin
# 设置不要复制的数据库(可设置多个)
binlog-ignore-db=mysql
binlog-ignore-db=information_schema
binlog-ignore-db=performance_schema
#设置需要复制的数据库
binlog-do-db=需要复制的主数据库名字
#设置logbin格式
binlog_format=STATEMENT
```

**从机配置**
修改配置文件： vi /etc/my.cnf
```shell
#从服务器唯一ID
server-id=2
#启用中继日志
relay-log=mysql-relay
```
**主机、从机重启 MySQL 服务**

```shell
systemctl restart mariadb.service
```

**主机从机都关闭防火墙**

```shell
systemctl stop firewalld.service
```
**在主机上建立帐户并授权 slave**

```shell
#在主机MySQL里执行授权命令
GRANT REPLICATION SLAVE ON *.* TO 'slave'@'%' IDENTIFIED BY '123123';

#查询master的状态
show master status;

MariaDB [(none)]> show master status;
+------------------+----------+--------------+--------------------------+
| File             | Position | Binlog_Do_DB | Binlog_Ignore_DB         |
+------------------+----------+--------------+--------------------------+
| mysql-bin.000002 |      245 | studentdb    | mysql,information_schema |
+------------------+----------+--------------+--------------------------+
1 row in set (0.00 sec)
```
>记录下File和Position的值，执行完此步骤后不要再操作主服务器MySQL，防止主服务器状态值变化，从机复制 的时候就是从File文件的Position位置开始进行复制的。

**在从机上配置需要复制的主机 ** 

```shell
#复制主机的命令
CHANGE MASTER TO MASTER_HOST='主机的IP地址',MASTER_PORT=3306,
MASTER_USER='slave',
MASTER_PASSWORD='123123',
MASTER_LOG_FILE='mysql-bin.具体数字',MASTER_LOG_POS=具体值;

#启动从服务器复制功能
start slave;

#查看从服务器状态
show slave status\G;
```
示例：
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\01-Mysql主从复制(MySQL Replication)\20200306145845508.png)

![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\01-Mysql主从复制(MySQL Replication)\20200306150015388.png)

```shell
#下面两个参数都是Yes，则说明主从配置成功！
# Slave_IO_Running: Yes
# Slave_SQL_Running: Yes
```
**主机新建库、新建表、 insert 记录， 从机复制**
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\01-Mysql主从复制(MySQL Replication)\20200306181534867.png)
 **如何停止从服务复制功能**


```shell
stop slave;
```
**如何重新配置主从**

```shell
stop slave;
reset master;
```



### 6.一主一从常见配置步骤（补）

来自B站尚硅谷的补充

1. mysql版本一致且后台以服务运行
2. 主从都配置在我【mysqld】节点下，都是小写；主机配置再Windows中，从机配置在Linux中
3. 主机修改my.ini配置文件
   1. 【必须】主服务器唯一ID
      1. server-id = 1
   2. 【必须】启动二进制文件
      1. log-bin=自己的路径/mysqlbin（这是二进制文件名）
   3. 【可选】启动错误日志
      1. log-err=自己本地的路径/mysqlerr（文件名）
      2. log-err=D:/devSoft/MySQLServer5.5/data/mysqlerr
   4. 【可选】根目录
      1. basedir="自己本地路径"
      2. basedir="D：/devSoft/MySQLService5.5/"
   5. 【可选】临时目录
      1. tmpdir="自己的本地路劲"
      2. tmpdir="D：/devSoft/MySQLService5.5/"
   6. 【可选】数据目录
      1. datadir="自己本地路径/Data/"
      2. datadir="D：/devSoft/MySQLService5.5/Data/"
   7. read-only=0
      1. 主机，读写都可以
   8. 【可选】设置不要复制的数据库
      1. binlog-lgnore-db=mysql
   9. 【可选】设置需要复制的数据库
      1. binlog-do-db=需要复制的主数据库名字
4. 从机修改my.cnf配置文件
   1. 【必须】从服务器唯一ID
   2. 【可选】启用二进制文件
5. 因为修改过配置文件，请主机+从机都启动后台mysql服务
6. 主机从机都关闭防火墙
   1. windows手动关闭
   2. 关闭虚拟机linux防火墙systemctl stop firewall
7. 在Windows主机上建立账户并授权slave
   1. GRANT REPLICATION SLAVE  ON*.* TO 'zhangsan'@'从机器数据库IP‘ IDENTIFIED BY '123456';
   2. flush privileges;
   3. 查询master的状态
      1. show master status;
      2. 记录下File和Position的值
   4. 执行完此步骤后不再执行主服务器MySQL，防止主服务器状态值变化
8. 在Linux从机上配置需要复制主机
   1. CHANGE MASTER TO MASTER_HOST='主机IP',MASTER_USER='zhangsan'，MASTER_PASSWORD='123456',MASTER_LOG_FILE='File名字'，MASTER_LOG_POS=Position数字；
      ![image-20220904215843924](D:\Tyora\AssociatedPicturesInTheArticles\01-Mysql主从复制(MySQL Replication)\image-20220904215843924.png)
   2. 启动从服务器复制功能
      1. start slave;
   3. show slave status\G
      1. 下面两个参数都YES，则说明主从配置成功！
      2. Slave_IO_Running;Yes
      3. Slave_SQL_Running;Yes
9. 主机新建库、新建表、insert记录，主机复制
10. 如何停止从服务复制功能
    1. stop slave；


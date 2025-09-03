# 1.主从复制原理

![image.png | left | 682x296](D:\Tyora\AssociatedPicturesInTheArticles\Mysql主从复制原理\1544058365225-07c30ebb-5d1b-4779-a492-c8cf997965f9.png)

## 1.1.主从复制的前提

1）两台或两台以上的数据库实例
2）主库要开启二进制日志
3）主库要有复制用户
4）主库的server_id和从库不同
5）从库需要在开启复制功能前，要获取到主库之前的数据（主库备份，并且记录binlog当时位置）
6）从库在第一次开启主从复制时，时必须获知主库：ip，port，user，password，logfile，pos
7）从库要开启相关线程：IO、SQL
8）从库需要记录复制相关用户信息，还应该记录到上次已经从主库请求到哪个二进制日志
9）从库请求过来的binlog，首先要存下来，并且执行binlog，执行过的信息保存下来

## 1.2.从复制涉及到的文件和线程

++*主库：*++

1）主库binlog：记录主库发生过的修改事件
2）dump thread：给从库传送（TP）二进制日志线程

++*从库：*++

1）relay-log（中继日志）：存储所有主库TP过来的binlog事件
2）master.info：存储复制用户信息，上次请求到的主库binlog位置点
3）IO thread：接收主库发来的binlog日志，也是从库请求主库的线程
4）SQL thread：执行主库TP过来的日志

## 1.3.原理

1）通过change master to语句告诉从库主库的ip，port，user，password，file，pos
2）从库通过start slave命令开启复制必要的IO线程和SQL线程
3）从库通过IO线程拿着change master to用户密码相关信息，连接主库，验证合法性
4）从库连接成功后，会根据binlog的pos问主库，有没有比这个更新的
5）主库接收到从库请求后，比较一下binlog信息，如果有就将最新数据通过dump线程给从库IO线程
6）从库通过IO线程接收到主库发来的binlog事件，存储到TCP/IP缓存中，并返回ACK更新master.info
7）将TCP/IP缓存中的内容存到relay-log中
8）SQL线程读取relay-log.info，读取到上次已经执行过的relay-log位置点，继续执行后续的relay-log日志，执行完成后，更新relay-log.info
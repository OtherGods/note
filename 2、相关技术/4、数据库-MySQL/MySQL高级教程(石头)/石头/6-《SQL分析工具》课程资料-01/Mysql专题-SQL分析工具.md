# 1.引入

在应用的的开发过程中，由于初期数据量小，开发人员写 SQL 语句时更重视功能上的实现，但是当应用系统正式上线后，随着生产数据量的急剧增长，很多 SQL 语句开始逐渐显露出性能问题，对生产的影响也越来越大，此时这些有问题的 SQL 语句就成为整个系统性能的瓶颈，因此我们必须要对它们进行优化，本章将详细介绍在 MySQL 中优化 SQL 语句的方法。

**当面对一个有 SQL 性能问题的数据库时，我们应该从何处入手来进行系统的分析，使得能够尽快定位问题 SQL 并尽快解决问题。**

1. **查看 SQL 执行频率**
2. 定位低效率执行 SQL
3. show profile 分析 SQL
4. trace 分析优化器执行计划

# 2.查看SQL执行频率

MySQL 客户端连接成功后，通过 `show [session|global] status` 命令可以提供服务器状态信息。该命令可以根据需要加上参数“`session`”或者“`global`”来显示 `session 级（当前连接）`的统计结果和 `global 级（自数据库上次启动至今）`的统计结果。如果不写，**默认使用参数是**`“session”`。

```sql
show status like 'Com_______';
show global status like 'Com_______';
show status like 'Innodb_rows_%';
```
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-SQL分析工具\20201219093027419.png)
Com_xxx 表示每个 xxx 语句执行的次数，我们通常比较关心的是以下几个统计参数。
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-SQL分析工具\20201219093420157.png)
`Com_***`：这些参数对于所有存储引擎的表操作都会进行累计。
`Innodb_***`：这几个参数只是针对 InnoDB 存储引擎的，累加的算法也略有不同。

# 3.定位低效率执行  SQL
可以通过如下这两种方式定位执行效率较低的 SQL 语句。
## 3.1.慢查询日志
### 3.1.1.概述
MySQL 的慢查询日志是 MySQL 提供的一种日志记录，它用来记录在 MySQL 中响应时间超过**阀值**的语句，具体指运行时间超过 `long_query_time` 值的 SQL，则会被记录到**慢查询日志**中。

具体指运行时间超过 `long_query_time` 值的 SQL，则会被记录到慢查询日志中。`long_query_time` 的默认值为 10，意思是运行 10 秒以上的语句。

由他来查看哪些 SQL 超出了我们的最大忍耐时间值，比如一条 SQL 执行超过 5 秒钟，我们就算慢 SQL，希望能收集超过 5 秒的 SQL，结合 explain 进行全面分析。

### 3.1.2.开启

默认情况下，MySQL 数据库没有开启慢查询日志，需要我们手动来设置这个参数。

当然，如果不是调优需要的话，一般不建议启动该参数，因为开启慢查询日志会或多或少带来一定的性能影响。慢查询日志支持将日志记录写入文件。

通过 `SHOW VARIABLES LIKE '%slow_query_log%'`; 查看是否开启，默认情况下 `slow_query_log` 的值为 `OFF`，表示慢查询日志是禁用的.，可以通过设置 `slow_query_log` 的值来开启。
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-SQL分析工具\20201219094000255.png)
使用 `set global slow_query_log=1;` 开启了慢查询日志只对当前数据库生效，如果 MySQL 重启后则会失效。

如果要永久生效，就必须修改配置文件 `my.cnf`（其它系统变量也是如此）。在 [mysqld] 下增加或修改参数 `slow_query_log` 和 `slow_query_log_file` 后，然后重启 MySQL 服务器。也就是将如下两行配置进 my.cnf 文件：

```java
slow_query_log =1
slow_query_log_file=/var/lib/mysql/nuist-slow.log
```
### 3.1.3.使用

>开启了慢查询日志后，什么样的 SQL 才会记录到慢查询日志里面呢？

这个是由参数 `long_query_time` 控制，通过命令 `SHOW VARIABLES LIKE 'long_query_time%';` 查看知默认值为 10 秒。
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-SQL分析工具\20201219094233228.png)

可以使用命令 `SET long_query_time=0.1` 修改慢的阙值时间，也可以在 `my.cnf` 参数里面修改。

假如运行时间正好等于 `long_query_time` 的情况，并不会被记录下来。也就是说，在 MySQL 源码里是判断大于 `long_query_time`，而非大于等于。
想要模拟查询时间较长的sql可以使用select sleep(时间);![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-SQL分析工具\20201219100849233.png)
查询当前系统中有多少条慢查询记录：`show global status like '%Slow_queries%';`
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-SQL分析工具\20201219100922192.png)

## 3.2.show processlist

**慢查询日志在查询结束以后才纪录**，所以在应用反映执行效率出现问题的时候查询慢查询日志并不能定位问题。此时可以使用 `show processlist` 命令查看当前 MySQL 在进行的线程，包括线程的状态、是否锁表等，可以实时地查看 SQL 的执行情况，同时对一些锁表操作进行优化。
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-SQL分析工具\20201219101222696.png)

- 【id】用户登录 MySQL 时，系统分配的"connection_id"，可以使用函数 connection_id() 查看。
- 【user】显示当前用户。如果不是 root，这个命令就只显示用户权限范围的 SQL 语句。
- 【host】显示这个语句是从哪个 IP 的哪个端口上发的，可以用来跟踪出现问题语句的用户。
- 【db】显示这个进程目前连接的是哪个数据库
- 【command】显示当前连接的执行的命令，一般取值为休眠(sleep)，查询(query)，连接(connect) 等。
- 【time】显示这个状态持续的时间，单位是秒。
- 【state】显示使用当前连接的 SQL 语句的状态，是很重要的列。state 描述的是语句执行中的某一个状态，以一个查询 SQL 为例，可能需要经过 `copying to tmp table`、`sorting result`、`sending data` 等状态才可以完成。
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-SQL分析工具\20201219101905217.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201219101931347.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

- 【info】显示这个 SQL 语句，是判断问题语句的一个重要依据。

# 4.show profile 分析 SQL

官网：http://dev.mysql.com/doc/refman/5.5/en/show-profile.html

是什么：是mysql提供可以用来分析当前会话中语句执行的资源消耗情况。可以用于SQL的调优测量

MySQL 从 5.0.37 版本开始增加了对 `show profiles` 和 `show profile` 语句的支持。`show profiles` 能够在做 SQL 优化时帮助我们了解时间都耗费到哪里去了【在SQL语句执行完之后，执行这个语句可以查看时间的耗时】。

通过 `have_profiling` 参数，能够看到当前 MySQL 是否支持 `profile`。默认 `profiling` 是关闭的，可以通过 set 语句在 Session 级别开启 profiling。通过 profile，我们能够更清楚地了解 SQL 执行的过程。
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-SQL分析工具\20201219102511612.png)

执行一段很耗时的SQL语句

执行 show profiles 指令，来查看 SQL 语句执行的耗时：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201219102609983.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

通过 `show profile for query query_id` 语句可以查看到该 SQL 执行过程中每个线程的状态和消耗的时间：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201219102628305.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

【注意】`Sending data` 状态表示 MySQL 线程开始访问数据行并把结果返回给客户端，而不仅仅是返回个客户端。由于在 `Sending data` 状态下，MySQL 线程往往需要做大量的磁盘读取操作，所以经常是整个查询中耗时最长的状态。

在获取到最消耗时间的线程状态后，MySQL 支持进一步选择下图中的类型查看 MySQL 在使用什么资源上耗费了过高的时间。
![image-20220903194812605](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-SQL分析工具\image-20220903194812605.png)

例如，选择查看 CPU 的耗费时间：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201219102753469.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201219102801236.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)



status的比较危险的四种状态：

1. converting HEAP to MyISAM 查询结果太大，内存都不够用了往磁盘上搬了。
2. Creating tmp table 创建临时表
   拷贝数据到临时表
      			用完再删除
3. Copying to tmp table on disk 把内存中临时表复制到磁盘，危险！！！
4. locked

# 5.trace 分析优化器执行计划

MySQL 5.6 提供了对 SQL 的跟踪 `trace`，通过 trace 文件能够进一步了解为什么 `Optimizer` 选择 A 计划，而不是选择 B 计划。

打开 trace，设置格式为 JSON，并设置 trace 最大能够使用的内存大小，避免解析过程中因为默认内存过小而不能够完整展示。

```java
-- 临时开启
set @@session.optimizer_trace='enabled=on,one_line=on';
```

```sql
-- 永久开启
SET optimizer_trace="enabled=on", end_markers_in_json=on;
SET optimizer_trace_max_mem_size=1000000;
```

示例：执行 `select * from tb_item where id < 4`;，然后通过 `select * from information_schema.optimizer_trace\G`; 查看 MySQL 是如何执行 SQL 的：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201219103343268.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)



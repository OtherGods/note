当我们开始关注数据库整体性能优化时，我们需要一套 MySQL 查询分析工具。特别是在开发中大型项目时，往往有数百个查询分布在代码库中的各个角落，并实时对数据库进行大量访问和查询。如果没有一套趁手的分析方法和工具，就很难发现在执行过程中代码的效率瓶颈，我们需要通过这套工具去定位 SQL 语句在执行中缓慢的问题和原因。

  

本教程带领大家学习和实践 MySQL Server 内置的查询分析工具 —— 慢查询日志、`mysqldumpslow`、`Profiling`，详细讲解如何使用他们提升代码执行效率。如果你想根据自己的工作流开发一套数据库查询管理工具，推荐使用卡拉云。只要你会写 SQL，无需会前端也可以轻松搭建属于自己的后台查询工具，详见本文文末。

## 一. 有关 MySQL 慢查询日志

### 1.慢查询日志是什么？

MySQL 慢查询日志是用来记录 MySQL 在执行命令中，响应时间超过预设阈值的 SQL 语句。

记录这些执行缓慢的 SQL 语句是优化 MySQL 数据库效率的第一步。

默认情况下，慢查询日志功能是关闭的，需要我们手动打开。当然，如果不是调优需求的话，一般也不建议长期启动这个功能，因为开启慢查询多少会对数据库的性能带来一些影响。慢查询日志支持将记录写入文件，当然也可以直接写入数据库的表中。

### 2.配置并打开慢查询日志

**（1）在 MySQL Server 中临时开启慢查询功能**

在 MySQL Server 中，默认情况慢查询功能是关闭的，我们可以通过查看此功能的状态
```sql
show variables like 'slow_query_log'; 
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292151662.png)

如上图所示，慢查询日志（slow_query_log ）的状态为关闭。

我们可以使用以下命令开启并配置慢查询日志功能，**在 mysql 中执行以下命令**：
```sql
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL slow_query_log_file = '/var/log/mysql/kalacloud-slow.log';
SET GLOBAL log_queries_not_using_indexes = 'ON';
SET SESSION long_query_time = 1;
SET SESSION min_examined_row_limit = 100;
```

- **`SET GLOBAL slow_query_log`** ：全局开启慢查询功能。
- **`SET GLOBAL slow_query_log_file`** ：指定慢查询日志存储文件的地址和文件名。
- **`SET GLOBAL log_queries_not_using_indexes`**：无论是否超时，未被索引的记录也会记录下来。
- **`SET SESSION long_query_time`**：慢查询阈值（秒），SQL 执行超过这个阈值将被记录在日志中。
- **`SET SESSION min_examined_row_limit`**：慢查询仅记录扫描行数大于此参数的 SQL。

**特别注意：** 在实践中常常会碰到无论慢查询阈值调到多小，日志就是不被记录。这个问题很有可能是 `min_examined_row_limit` 行数过大，导致没有被记录。`min_examined_row_limit` 在配置中常被忽略，这里要特别注意。

接着我们来执行查询语句，看看配置。（在 MySQL Server 中执行）
```sql
show variables like 'slow_query_log%';
show variables like 'log_queries_not_using_indexes';
show variables like 'long_query_time';
show variables like 'min_examined_row_limit';
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292155350.png)

以上修改 MySQL 慢查询配置的方法是用在**临时监测数据库运行状态**的场景下，当 MySQL Server 重启时，以上修改全部失效并恢复原状。

扩展阅读：[六类 MySQL 触发器使用教程及应用场景实战案例](https://xie.infoq.cn/link?target=https%3A%2F%2Fkalacloud.com%2Fblog%2Fhow-to-manage-and-use-mysql-database-triggers%2F) | [MySQL 触发器的创建、使用、查看、删除教程及应用场景实战案例](https://zhuanlan.zhihu.com/p/421593989)

**（2）将慢查询设置写入 MySQL 配置文件，永久生效**

虽然我们可以在命令行中对慢查询进行动态设置，但动态设置会随着重启服务而失效。如果想长期开启慢查询功能，需要把慢查询的设置写入 MySQL 配置文件中，这样无论是重启服务器，还是重启 MySQL ，慢查询的设置都会保持不变。

MySQL conf 配置文件通常在 `/etc` 或 `/usr` 中。我们可以使用 `find` 命令找到配置文件具体的存放位置。
```shell
sudo find /etc -name my.cnf
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292202085.png)

找到位置后，使用 `nano` 编辑 `my.cnf` 将慢查询设置写入配置文件。
```shell
sudo nano /etc/mysql/my.cnf
```

```sql
[mysqld]

slow-query-log = 1
slow-query-log-file = /var/log/mysql/localhost-slow.log
long_query_time = 1
log-queries-not-using-indexes
```

使用 `nano` 打开配置文件，把上面的的代码写在 `[mysqld]` 的下面即可。 `ctrl+X` 保存退出。
```shell
sudo systemctl restart mysql
```

重启 MySQL Server 服务，使刚刚修改的配置文件生效。

**特别注意：** 直接在命令行中设置的慢查询动态变量与直接写入 my.cnf 配置文件的语法有所不同。

扩展阅读：10 种 [MySQL 管理工具](https://xie.infoq.cn/link?target=https%3A%2F%2Fkalacloud.com%2Fblog%2Fbest-mysql-gui-tools%2F) 横向测评 - 免费和付费到底怎么选?

举例：动态变量是`slow_query_log`，写入配置文件是`slow-query-log`。这里要特别注意。

更多 MySQL 8.0 动态变量语法可查看 [MySQL 官方文档](https://xie.infoq.cn/link?target=https%3A%2F%2Fdev.mysql.com%2Fdoc%2Frefman%2F8.0%2Fen%2Fdynamic-system-variables.html)。

## 二. 使用慢查询功能记录日志

到这里我们已经配置好慢查询功能所需要的一切。下面咱们写一个示例，在这个示例中我们来一起学习如何查看和分析慢查询日志。

你可以打开两个连接到服务器的命令行窗口，一个用来写 MySQL 代码，另一个用来查看日志。

**注意：以下教程中，有些代码是在命令行中执行，有些是在 MySQL Server 中执行，请注意分辨。**

登录 MySQL Server，创建一个数据库，写入一组示例数据。
```sql
CREATE DATABASE kalacloud_demo;
USE kalacloud_demo;
CREATE TABLE users ( id TINYINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255) );
INSERT INTO users (name) VALUES ('Jack Ma'),('Lei Jun'),('Wang Xing'),('Pony Ma'),('Zhang YiMing'),('Ding Lei'),('Robin Li'),('Xu Yong'),('Huang Zheng'),('Richard Liu');
```

为了保证大家与教程配置保持一致，咱们一起使用动态变量，再设置一边慢查询参数。

在 MySQL Server 中执行以下 SQL 代码：
```sql
SET GLOBAL slow_query_log = 1;
SET GLOBAL slow_query_log_file = '/var/log/mysql/kalacloud-slow.log';
SET GLOBAL log_queries_not_using_indexes = 1;
SET long_query_time = 10;
SET min_examined_row_limit = 0;
```

现在我们有了一个表中有数据的示例数据库。慢查询功能也已经打开，我们特意把时间阈值（long_query_time）设置为 10 并且把最小行（min_examined_row_limit）设置为 0。

接着我们来运行一段代码测试一下：
```sql
USE kalacloud_demo;
SELECT * FROM users WHERE id = 1;
```

使用主键索引对表进行 `select` 查询，这种查询速度非常快，又使用了索引。因此慢查询日志中不会有任何记录。

我们打开慢查询日志，验证一下是否有记录，在命令行中执行以下命令：
```shell
sudo cat /var/log/mysql/kalacloud-slow.log
```

可以看到`kalacloud-slow.log`还没有任何记录。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292207053.png)

接着我们在 MySQL Server 中执行以下代码：

```sql
SELECT * FROM users WHERE name = 'Wang Xing';
```

这段查询代码使用非索引列（name）来进行查询，所以慢查询日志在会记录下这个查询。

我们打开日志查看记录变化：

```shell
sudo cat /var/log/mysql/kalacloud-slow.log
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292207716.png)

我们可以看到这个非索引查询，已经被记录在慢查询日志中了。

再举个例子。我们提高最小检查行（min_examined_row_limit）的检查行数设置为 100，然后再执行查询。

在 MySQL Server 中执行以下代码：
```sql
SET min_examined_row_limit = 100;
SELECT * FROM users WHERE name = 'Zhang YiMing';
```

执行后，再打开 `kalacloud-slow.log` ，可以看到条小于 `100` 行的查询，没有被记录到日志中。

**特别注意**：如果慢查询日志中，没有记录任何数据，可以检查以下内容。

（1）创建日志的目录权限问题，是否有对应的权限。
```shell
cd /var/log
mkdir mysql
chmod 755 mysql
chown mysql:mysql mysql
```

（2）另一个可能是查询变量配置问题，把 `my.conf` 文件内有关慢查询的配置清干净，然后重启服务，重新配置。看看是不是这里出的问题。

扩展阅读：[如何将 MySQL 的查询结果保存到文件](https://xie.infoq.cn/link?target=https%3A%2F%2Fkalacloud.com%2Fblog%2Fhow-to-save-mysql-mariadb-query-output-to-a-file%2F)

## 三. 慢查询日志记录参数详解

接着我们来讲解慢查询日志应该如何分析
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292230600.png)

日志中信息的说明：
- `Time` ：被日志记录的代码在服务器上的运行时间。
- `User@Host`：谁执行的这段代码。
- `Query_time`：这段代码运行时长。
- `Lock_time`：执行这段代码时，锁定了多久。
- `Rows_sent`：慢查询返回的记录。
- `Rows_examined`：慢查询扫描过的行数。

这些被记录的信息非常有意义，所有超过阈值的代码都会被记录在日志中，我们可以通过这些信息找到 MySQL 查询时效率不佳的代码，有助于我们优化 MySQL 性能。

扩展阅读：[如何在 MySQL 里查询数据库中带有某个字段的所有表名](https://xie.infoq.cn/link?target=https%3A%2F%2Fkalacloud.com%2Fblog%2Ffind-all-tables-with-specific-column-names-in-mysql%2F)

## 四. 使用 mysqldumpslow 工具对慢查询日志进行分析

实际工作中，慢查询日志可不像上文描述的那样，仅仅有几行记录。现实中慢查询日志会记录大量慢查询信息，写入也非常频繁。日志记录的内容会越来越长，分析数据也变的困难。 好在 MySQL 内置了 `mysqldumpslow` 工具，它可以把相同的 SQL 归为一类，并统计出归类项的执行次数和每次执行的耗时等一系列对应的情况。

我们先来执行几行代码让慢查询日志记录下来，然后再用 `mysqldumpslow` 进行分析。

上文我们把`min_examined_row_limit` 设置为 100，在这里，我们要将它改为 0 ，慢查询才能有记录。在 MySQL Server 中执行以下代码：
```sql
SET min_examined_row_limit = 0;
```

接着我们执行几条查询命令：
```sql
SELECT * FROM users WHERE name = 'Wang Xing';
SELECT * FROM users WHERE name = 'Huang Zheng';
SELECT * FROM users WHERE name = 'Zhang YiMing';
```

根据前文的慢查询设置，这三条记录都将被记录在日志中。

现在，**我们切换到命令行的窗口中**，执行 `mysqldumpslow` 命令：
```sql
sudo mysqldumpslow -s at /var/log/mysql/kalacloud-slow.log
```

返回的数据：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292236502.png)

我们可以看到，返回的数据中，已经把三条类似的 SQL 语句记录抽象成一条记录`SELECT * FROM users WHERE name = 'S'` 并且针对这条记录列出了对应的总量和平均量的记录。

**常见的** `mysqldumpslow` **命令** 平时大家也可以根据自己的常用需求来总结，存好这些脚本备用。

- `mysqldumpslow -s at -t 10 kalacloud-slow.log`：平均执行时长最长的前 10 条 SQL 代码。
- `mysqldumpslow -s al -t 10 kalacloud-slow.log`：平均锁定时间最长的前 10 条 SQL 代码。
- `mysqldumpslow -s c -t 10 kalacloud-slow.log`：执行次数最多的前 10 条 SQL 代码。
- `mysqldumpslow -a -g 'user' kalacloud-slow.log`：显示所有 `user` 表相关的 SQL 代码的具体值
- `mysqldumpslow -a kalacloud-slow.log`：直接显示 SQL 代码的情况。

`mysqldumpslow` 的参数命令
```sql
Usage: mysqldumpslow [ OPTS... ] [ LOGS... ]

Parse and summarize the MySQL slow query log. Options are

  --verbose    verbose
  --debug      debug
  --help       write this text to standard output
  -v           verbose
  -d           debug
  -s ORDER     what to sort by (al, at, ar, c, l, r, t), 'at' is default
                al: average lock time
                ar: average rows sent
                at: average query time
                 c: count
                 l: lock time
                 r: rows sent
                 t: query time
  -r           reverse the sort order (largest last instead of first)
  -t NUM       just show the top n queries
  -a           don't abstract all numbers to N and strings to 'S'
  -n NUM       abstract numbers with at least n digits within names
  -g PATTERN   grep: only consider stmts that include this string
  -h HOSTNAME  hostname of db server for *-slow.log filename (can be wildcard),
               default is '*', i.e. match all
  -i NAME      name of server instance (if using mysql.server startup script)
  -l           don't subtract lock time from total time
```

常用的参数讲解：
`-s`  
- al：平均锁定时间
- at：平均查询时间 [默认]
- ar：平均返回记录时间
- c：count 总执行次数
- l：锁定时间
- r：返回记录
- t：查询时间
`-t`：返回前 N 条的数据
`-g`：可写正则表达，类似于 grep 命令，过滤出需要的信息。如，只查询 X 表的慢查询记录。
`-r`：rows sent 总返回行数。

`mysqldumpslow` 日志查询工具好用就好用在它特别灵活，又可以合并同类项式的分析慢查询日志。我们在日常工作的使用中，就能够体会 `mysqldumpslow` 的好用之处。

另外 `mysqldumpslow` 的使用参数也可在 [MySQL 8.0 使用手册](https://xie.infoq.cn/link?target=https%3A%2F%2Fdev.mysql.com%2Fdoc%2Frefman%2F8.0%2Fen%2Fmysqldumpslow.html) 中找到。

扩展阅读：[如何查看 MySQL 数据库、表、索引容量大小](https://xie.infoq.cn/link?target=https%3A%2F%2Fkalacloud.com%2Fblog%2Fhow-to-get-the-sizes-of-the-tables-of-a-mysql-database%2F)？找到占用空间最大的表

## 五. Profilling - MySQL 性能分析工具

为了更精准的定位一条 SQL 语句的性能问题，我们需要拆分这条语句运行时到底在什么地方消耗了多少资源。 我们可以使用 Profilling 工具来进行这类细致的分析。我们可通过 Profilling 工具获取一条 SQL 语句在执行过程中对各种资源消耗的细节。

进入 MySQL Server 后，执行以下代码，启动 Profilling

```sql
SET SESSION profiling = 1; 
```

检查 profiling 的状态

```sql
SELECT @@profiling;
```

返回数据： 0 表示未开启，1 表示已开启。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292248269.png)

执行需要定位问题的 SQL 语句。
```sql
USE kalacloud_demo;
SELECT * FROM users WHERE name = 'Jack Ma';
```

查看 SQL 语句状态。

```sql
SHOW PROFILES;
```

打开 profiling 后，`SHOW PROFILES;` 会显示一个将 `Query_ID` 链接到 SQL 语句的表。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292250539.png)

`Query_ID`：SQL 语句的 ID 编号。`Duration`：SQL 语句执行时长。`Query`：具体的 SQL 语句。

执行以下 SQL 代码，将 `[# Query_ID]` 替换为我们要分析的 SQL 代码`Query_ID`的编号。

```sql
SHOW PROFILE CPU, BLOCK IO FOR QUERY [# Query_ID];
```

即

```sql
SHOW PROFILE CPU, BLOCK IO FOR QUERY 4;
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292251823.png)

`Status` 是执行查询过程中的具体步骤，`Duration` 是完成该步骤所需的时间（以秒为单位）。

我们可以根据这些细节来具体分析，如何优化对应的 SQL 代码。

## 六. 慢查询教程总结

慢查询是让我们看到数据库真实运行状态的工具，对服务器和数据库性能优化有着指导性的意义。无论是生产环境、开发、QA，都可以谨慎的打开慢查询来记录性能日志。

我们可以先把动态变量`long_query_time` 设置的大一些，观察一下，然后在进行微调。有了慢查询日志，我们就有了优化性能的方向和目标，再使用 `mysqldumpslow` 和 `profiling` 进行宏观和微观的日志分析。找到低效 SQL 语句的细节，进行微调，最终使我们的系统可以获得最佳执行性能。

至此，MySQL 慢查询日志我们就讲解完了，如果你周期性的查看 log 日志，可以使用卡拉云搭一个日志看板，自己不仅查看、分析数据方便，还可以一键分享给组内的小伙伴共享数据。

卡拉云是新一代低代码开发工具，免安装部署，可一键接入包括 MySQL 在内的常见数据库及 API。不仅可以像命令行一样灵活，还可根据自己的工作流，定制开发。无需繁琐的前端开发，只需要简单拖拽，即可快速搭建企业内部工具。**数月的开发工作量，使用卡拉云后可缩减至数天**，欢迎使用我开发的[卡拉云](https://xie.infoq.cn/link?target=https%3A%2F%2Fkalacloud.com%2F%3Futm_medium%3Dregister)。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292255744.png)

卡拉云可快速接入的常见数据库及 API

卡拉云可根据公司工作流需求，轻松搭建数据看板，并且可分享给组内的小伙伴共享数据
![1.gif](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292256510.gif)

仅需拖拽一键生成前端代码，简单一行代码即可映射数据到指定组件中。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407292256330.png)

卡拉云可直接添加导出按钮，导出适用于各类分析软件的数据格式，方便快捷。立即开通[卡拉云](https://xie.infoq.cn/link?target=https%3A%2F%2Fkalacloud.com%2F%3Futm_medium%3Dregister)，快速搭建属于你自己的后台管理系统。

有关 MySQL 教程，可继续拓展学习：
- [如何远程连接 MySQL 数据库，阿里云腾讯云外网连接教程](https://xie.infoq.cn/link?target=https%3A%2F%2Fkalacloud.com%2Fblog%2Fhow-to-allow-remote-access-to-mysql%2F)  
- [如何在 MySQL / MariaDB 中导入导出数据，导入导出数据库文件、Excel、CSV](https://xie.infoq.cn/link?target=https%3A%2F%2Fkalacloud.com%2Fblog%2Fhow-to-import-and-export-databases-excel-csv-in-mysql-or-mariadb-from-terminal%2F)  
- [如何在两台服务器之间迁移 MySQL 数据库 阿里云腾讯云迁移案例](https://xie.infoq.cn/link?target=https%3A%2F%2Fkalacloud.com%2Fblog%2Fhow-to-migrate-a-mysql-database-between-two-servers-aliyun-tencentyun%2F)  
- [MySQL 中如何实现 BLOB 数据类型的存取，BLOB 有哪些应用场景？](https://xie.infoq.cn/link?target=https%3A%2F%2Fkalacloud.com%2Fblog%2Fhow-to-use-the-mysql-blob-data-type-to-store-images-with-php-or-kalacloud%2F)  
- [如何使用 MySQL Workbench 操作 MySQL / MariaDB 数据库中文指南](https://xie.infoq.cn/link?target=https%3A%2F%2Fkalacloud.com%2Fblog%2Fmysql-workbench-tutorial%2F)


转载自：[如何使用 MySQL 慢查询日志进行性能优化 - Profiling、mysqldumpslow 实例详解](https://xie.infoq.cn/article/e40754334e18838fe100b5526)
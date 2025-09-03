关于事务基本概览的介绍，请看这篇文章的介绍：[5、Mysql常见面试题总结](2、相关技术/4、数据库-MySQL/JavaGuide/5、Mysql常见面试题总结.md)

参考：
[补1-面试中的老大难-mysql事务和锁，一次性讲清楚！](2、相关技术/4、数据库-MySQL/补1-面试中的老大难-mysql事务和锁，一次性讲清楚！.md)
[4、InnoDB存储引擎对MVCC的实现](2、相关技术/4、数据库-MySQL/JavaGuide/7、重要知识点/4、InnoDB存储引擎对MVCC的实现.md)

## 1、事务隔离级别总结

SQL 标准定义了四个隔离级别：

- **READ-UNCOMMITTED(读取未提交)** ：最低的隔离级别，允许读取尚未提交的数据变更，可能会导致脏读、幻读或不可重复读。
- **READ-COMMITTED(读取已提交)** ：允许读取并发事务已经提交的数据，可以阻止脏读，但是幻读或不可重复读仍有可能发生。
- **REPEATABLE-READ(可重复读)** ：对同一字段的多次读取结果都是一致的，除非数据是被本身事务自己所修改，可以阻止脏读和不可重复读，但幻读仍有可能发生。
- **SERIALIZABLE(可串行化)** ：最高的隔离级别，完全服从 ACID 的隔离级别。所有的事务依次逐个执行，这样事务之间就完全不可能产生干扰，也就是说，该级别可以防止脏读、不可重复读以及幻读。

|隔离级别|脏读|不可重复读|幻读|
|---|---|---|---|
|READ-UNCOMMITTED|√|√|√|
|READ-COMMITTED|×|√|√|
|REPEATABLE-READ|×|×|√|
|SERIALIZABLE|×|×|×|

MySQL InnoDB 存储引擎的默认支持的隔离级别是 **REPEATABLE-READ（可重读）**。我们可以通过`SELECT @@tx_isolation;`命令来查看，MySQL 8.0 该命令改为`SELECT @@transaction_isolation;`

从上面对 SQL 标准定义了四个隔离级别的介绍可以看出，标准的 SQL 隔离级别定义里，REPEATABLE-READ(可重复读)是不可以防止幻读的。

但是！InnoDB 实现的 REPEATABLE-READ 隔离级别其实是可以解决幻读问题发生的，主要有下面两种情况：

- **快照读**：由 MVCC 机制来保证不出现幻读。
- **当前读**：使用 Next-Key Lock 进行加锁来保证不出现幻读，Next-Key Lock 是行锁（Record Lock）和间隙锁（Gap Lock）的结合，行锁只能锁住已经存在的行，为了避免插入新行，需要依赖间隙锁。

因为隔离级别越低，事务请求的锁越少，所以大部分数据库系统的隔离级别都是 **READ-COMMITTED** ，但是你要知道的是 InnoDB 存储引擎默认使用 **REPEATABLE-READ** 并不会有任何性能损失。

InnoDB 存储引擎在分布式事务的情况下一般会用到 SERIALIZABLE 隔离级别。

《MySQL 技术内幕：InnoDB 存储引擎(第 2 版)》7.7 章这样写到：

> InnoDB 存储引擎提供了对 XA 事务的支持，并通过 XA 事务来支持分布式事务的实现。<font color = "red">分布式事务指的是允许多个独立的事务资源（transactional resources）参与到一个全局的事务中。事务资源通常是关系型数据库系统，但也可以是其他类型的资源。全局事务要求在其中的所有参与的事务要么都提交，要么都回滚，这对于事务原有的 ACID 要求又有了提高。另外，在使用分布式事务时，InnoDB 存储引擎的事务隔离级别必须设置为 SERIALIZABLE。</font>

## 2、实际情况演示

在下面我会使用 2 个命令行 MySQL ，模拟多线程（多事务）对同一份数据的脏读问题。

MySQL 命令行的默认配置中事务都是自动提交的，即执行 SQL 语句后就会马上执行 COMMIT 操作。

设置非自动提交：
```sql
-- 值为 0 和值为 OFF：关闭事务自动提交。如果关闭自动提交，用户将会一直处于某个事务中，只有提交或回滚后才会结束当前事务，重新开始一个新事务。
-- 值为 1 和值为 ON：开启事务自动提交。如果开启自动提交，则每执行一条 SQL 语句，事务都会提交一次。
SET autocommit = 0|1|OFF|ON;
```

如果要显式地开启一个事务需要使用命令：`START TRANSACTION`。

我们可以通过下面的命令来设置隔离级别。

```
SET [SESSION|GLOBAL] TRANSACTION ISOLATION LEVEL [READ UNCOMMITTED|READ COMMITTED|REPEATABLE READ|SERIALIZABLE]
```

我们再来看一下我们在下面实际操作中使用到的一些并发控制语句:
- `START TRANSACTION` |`BEGIN`：显式地开启一个事务。
- `COMMIT`：提交事务，使得对数据库做的所有修改成为永久性。
- `ROLLBACK`：回滚会结束用户的事务，并撤销正在进行的所有未提交的修改。

### 2.1、脏读(读未提交)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407202256838.png)

### 2.2、避免脏读(读已提交)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407202259631.png)

### 2.3、不可重复读

还是刚才上面的读已提交的图，虽然避免了读未提交，但是却出现了，一个事务还没有结束，就发生了 不可重复读问题。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407202318696.png)

### 2.4、可重复读

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407202320733.png)

### 2.5、幻读

关于脏读、不可重复读、幻读的异同点：[补5-脏读、不可重复、幻读的区分](2、相关技术/4、数据库-MySQL/补5-脏读、不可重复、幻读的区分.md)

演示幻读出现的情况
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407202329673.png)

SQL 脚本 1 在第一次查询工资为 500 的记录时只有一条，SQL 脚本 2 插入了一条工资为 500 的记录，提交之后；SQL 脚本 1 在同一个事务中再次使用当前读查询发现出现了两条工资为 500 的记录这种就是幻读。

#### 2.5.1、解决幻读的三种方法

解决幻读的方式有很多，但是它们的核心思想就是一个事务在操作某张表数据的时候，另外一个事务不允许新增或者删除这张表中的数据了。解决幻读的方式主要有以下几种：

1. 将事务隔离级别调整为 `SERIALIZABLE` 。
2. 在可重复读的事务级别下，给事务操作的这张表添加表锁。
3. 在可重复读的事务级别下，给事务操作的这张表中的记录添加 `Next-key Lock（Record Lock+Gap Lock）` 或 `Gap Lock`。

Next-key Lock（临键锁）解决幻读示例：
t表结构
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408090013159.png)

事务1中临键锁锁住的范围是（20，35），所以事务2中f2是20和35时能插入，f2是21和34时因为被锁，所以不能插入
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408090021059.png)



### 2.6、参考

- 《MySQL 技术内幕：InnoDB 存储引擎》
- [https://dev.MySQL.com/doc/refman/5.7/en/open in new window](https://dev.MySQL.com/doc/refman/5.7/en/)
- [补15_Mysql锁：灵魂七拷问](2、相关技术/4、数据库-MySQL/补15_Mysql锁：灵魂七拷问.md)
- [Innodb中的事务隔离级别和锁的关系](2、相关技术/4、数据库-MySQL/美团技术团队/Innodb中的事务隔离级别和锁的关系.md)


---

著作权归JavaGuide(javaguide.cn)所有 基于MIT协议 原文链接：[https://javaguide.cn/database/mysql/transaction-isolation-level.html](https://javaguide.cn/database/mysql/transaction-isolation-level.html)


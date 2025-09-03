# 典型回答

现需要了解下这几种log：
[41、binlog、redolog和undolog区别？](2、相关技术/4、数据库-MySQL/Hollis/41、binlog、redolog和undolog区别？.md)

- **redolog**：是MySQL用于实现<font color="blue" size=5>崩溃恢复、数据持久性</font>的一种机制。在事务进行过程中，MySQL会**将事务做了什么改动到Redo Log中**。当**系统崩溃或者发生异常情况时，MySQL会利用Redo Log中的记录信息来进行恢复操作，将事务所做的修改持久化到磁盘中**。
- **binlog**：是MySQL用于**记录数据库中的所有DDL语句和DML语句的一种二进制日志**。它记录了所有对数据库结构和数据的修改操作，如INSERT、UPDATE和DELETE等。binlog主要用来对数据库进行<font color="blue" size=5>数据备份、灾难恢复和数据复制等操作</font>。binlog的格式分为基于语句的格式和基于行的格式。
- **undolog**：用于<font color="blue" size=5>在事务回滚或系统崩溃时撤销（回滚）事务所做的修改</font>。当一个事务执行过程中，MySQL会**将事务修改前的数据记录到Undo Log中**。如果事务需要回滚，则会从Undo Log中找到相应的记录来撤销事务所做的修改。另外，Undo Log还支持MVCC（多版本并发控制）机制，用于在并发事务执行时提供一定的隔离性。

当执行一次insert操作时，MySQL InnoDB引擎的日志写入顺序如下：
1. 写入 **`undolog`**，先将**事务修改前的数据**记录到Undo Log中。
2. 写入 **`redolog`**，处于**prepare阶段 （表示事务已修改但未提交）**。
3. 写入 **`binlog`**，**将 binlog 内存日志数据写入文件缓冲区并刷新到磁盘中**。
4. 写入 **`redolog`**，处于**commit阶段**。

所以写入顺序是：
```sql
undo log → redo log (prepare) → binlog → redo log (commit)
```

为什么这么个顺序？

首先，undolog要记录变更前的数据，所以一定要最先执行。

redolog和binlog的写入需要保证原子性，先写 redo log，后写 binlog，中间MySQL 崩溃，会导致主从数据不一致（因为binlog用于主从同步）。  而先写 binlog，后写 redo log，中间MySQL 崩溃，数据页没有更新，事务无法恢复。  所以需要用2阶段提交的方式写入。
[71、什么是事务的2阶段提交？](2、相关技术/4、数据库-MySQL/Hollis/71、什么是事务的2阶段提交？.md)

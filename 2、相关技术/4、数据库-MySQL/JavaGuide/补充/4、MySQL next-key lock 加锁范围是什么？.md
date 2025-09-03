某天，突然被问到 MySQL 的 next-key lock，我瞬间的反应就是：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408102300104.png)

这都是啥啥啥？？？
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408102301079.png)

这一个截图我啥也看不出来呀？

仔细一看，好像似曾相识，这不是《MySQL 45 讲》里面的内容么？

# 1、什么是 next-key lock

> A next-key lock is a combination of a record lock on the index record and a gap lock on the gap before the index record.

官网的解释大概意思就是：[next-key](https://link.segmentfault.com/?enc=7uCuXjt3mW7TUMZGgf%2FGVw%3D%3D.RxF8ift7GanqaMps0stMAKC%2FPP%2F0CFFZE0Iz0fm6b8rNOgOcofkKYF9bnHZnIYDcmK0gW2F68m4ewFL93na2Re1QwyuFUSWBDhEGfjyGkzgqIGUphpnQWq%2BWXYt%2F8WGJ) 锁是索引记录上的记录锁和索引记录之前的间隙上的间隙锁的组合。

先给自己来一串小问号？？？

1. 在主键、唯一索引、普通索引以及普通字段上加锁，是锁住了哪些索引？
2. 不同的查询条件，分别锁住了哪些范围的数据？
3. for share 和 for update 等值查询和范围查询的锁范围？
4. 当查询的等值不存在时，锁范围是什么？
5. 当查询条件分别是主键、唯一索引、普通索引时有什么区别？
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408102306866.png)

既然啥都不懂，那只好从头开始操作实践一把了！

先看看看 《MySQL 45 讲》中丁奇老师的结论：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408102306220.png)

看了这结论，应该可以解答一大部分问题，不过有一句非常非常重点的话需要关注：`MySQL 后面的版本可能会改变加锁策略，所以这个规则只限于截止到现在的最新版本，即 5.x 系列<=5.7.24，8.0 系列 <=8.0.13`

所以，以上的规则，对现在的版本并不一定适用，下面我以 `MySQL 8.0.25` 版本为例，进行多角度验证 next-key lock 加锁范围。

# 2、环境准备

MySQL 版本：8.0.25

隔离级别：可重复读（RR）

存储引擎：InnoDB
```sql
mysql> select @@global.transaction_isolation,@@transaction_isolation\G
mysql> show create table t\G
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408102312466.png)

> 如何使用 Docker 安装 MySQL，可以参考另一篇文章《使用 Docker 安装并连接 MySQL》

# 3、主键索引

首先来验证主键索引的 next-key lock 的范围
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408102316158.png)

此时数据库的数据如图所示，对主键索引来说此时数据间隙如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408102316219.png)

#### 3.1、关于mysql锁释放时机（CHatGPT）

在 MySQL 中，锁的释放时间取决于锁的类型以及事务的行为。以下是不同类型锁的释放时机：
##### 0、关于自动提交和手动开启事务

使用 BEGIN 或 START TRANSACTION 开启一个事务之后，自动提交将保持禁用状态，直到使用 COMMIT 或 ROLLBACK 结束事务。之后，自动提交模式会恢复到之前的状态，即如果 BEGIN 前 autocommit = 1，则完成本次事务后 autocommit 还是 1。如果 BEGIN 前 autocommit = 0，则完成本次事务后 autocommit 还是 0。

当 MySQL 使用 `BEGIN` 或 `START TRANSACTION` 显式地开启事务后，自动提交功能会暂时失效，直到事务明确地通过 `COMMIT` 或 `ROLLBACK` 结束为止。在此期间，所有对数据库的操作都不会自动提交，而是等待手动提交或回滚。这意味着：

- **自动提交被暂时禁用**：虽然 MySQL 的 `autocommit` 设置可能是开启的（`autocommit = 1`），但一旦你显式地开始了一个事务（使用 `BEGIN`、`START TRANSACTION`），自动提交就会暂时失效。**所有在该事务中的操作都会被延迟到事务结束时再提交**。
- **手动提交或回滚**：<font size = 5 color = "red">你需要明确地使用 `COMMIT` 提交事务，或使用 `ROLLBACK` 回滚事务，才能完成并释放事务中持有的锁</font>。

**示例**

```sql
SET autocommit = 1;  -- 自动提交模式

BEGIN;  -- 显式开启事务，自动提交功能暂时失效

INSERT INTO users (id, name) VALUES (1, 'Alice');  -- 这个操作不会立即提交

-- 此时查询数据锁，可以看到持有的锁
SELECT * FROM performance_schema.data_locks\G;

COMMIT;  -- 提交事务，所有更改生效，锁释放
```

在这个示例中：

- 虽然自动提交是开启的，但由于使用了 `BEGIN` 开启事务，`INSERT` 语句不会立即生效，而是被保留在事务中，直到你明确提交或回滚事务。
- 在你提交事务之前，自动提交机制不会作用于事务中的任何语句。

**总结**

在使用 `BEGIN` 或 `START TRANSACTION` 显式开启事务后，MySQL 的自动提交功能将暂时失效，直到事务结束。此时，所有数据库操作都需要手动提交或回滚，自动提交模式在事务结束之前不起作用。

##### 1. **自动提交模式下的锁**

- **行级锁**（如 `SELECT ... FOR UPDATE`、`SELECT ... LOCK IN SHARE MODE`）：在 SQL 语句执行完成后立即释放。如果使用的是自动提交模式（默认情况下 `autocommit=1`），每个单独的 SQL 语句都被当作一个事务处理，语句执行完成后，锁就会被释放。
- **表锁**（如 `LOCK TABLES`）：在表操作完成或者当 `UNLOCK TABLES` 命令执行后立即释放。

##### 2. **非自动提交模式（显式事务）**
- **事务中的行级锁**：在事务提交 (`COMMIT`) 或回滚 (`ROLLBACK`) 后释放。这意味着事务中所有操作的锁，只有在事务结束时才会被释放。
- **表锁**：在事务结束时（`COMMIT` 或 `ROLLBACK`）或者执行 `UNLOCK TABLES` 命令时释放。

##### 3. **全局锁**
- **全局读锁**（`FLUSH TABLES WITH READ LOCK`）：全局读锁在执行 `UNLOCK TABLES` 命令或者会话断开时释放。

##### 4. **死锁**
- 当检测到死锁时，MySQL 会自动回滚其中一个事务，以解除死锁并释放锁。

##### 5. **锁等待超时**
- 如果设置了锁等待超时参数（`innodb_lock_wait_timeout`），在超时后会自动回滚事务并释放锁。

##### 6. **连接关闭**
- 当客户端连接关闭时，MySQL 会自动回滚当前事务并释放所有持有的锁。

#### 3.2、主键等值查询 —— 数据存在

```sql
mysql> begin; select * from t where id = 10 for update;
```

这条 SQL，对 `id = 10` 进行加锁，可以先思考一下加了什么锁？锁住了什么数据？

可以通过 `data_locks` 查看锁信息，SQL 如下：
```sql
# mysql> select * from performance_schema.data_locks;
mysql> select * from performance_schema.data_locks\G
```

具体字段含义可以参考 [官方文档](https://link.segmentfault.com/?enc=8n9mNdAHfRFCh7oVcAydLQ%3D%3D.Kjin1e6%2B7cOPGEmiJovkpv0nOlBxRbYZqKRzpAFCowL9BfgrQwh5N8VKqCgFvKEHn8LLEcQq776PpFE38ZWGqyugPZkCGk5KJMMWThF2CdRha36uYz6P0vbbpWeyZj2FCrb3mxNROLOgzAgWPm%2FglA%3D%3D)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111038373.png)

结果主要包含引擎、库、表等信息，咱们需要重点关注以下几个字段：
- INDEX_NAME：锁定索引的名称
- LOCK_TYPE：锁的类型，对于 InnoDB，允许的值为 RECORD 行级锁 和 TABLE 表级锁。
- LOCK_MODE：锁的类型：S, X, IS, IX, and gap locks
- LOCK_DATA：锁关联的数据，对于 InnoDB，当 LOCK_TYPE 是 RECORD（行锁），则显示值。当锁在主键索引上时，则值是锁定记录的主键值。当锁是在辅助索引上时，则显示辅助索引的值，并附加上主键值。

结果很明显，这里是对表添加了一个 IX 锁 并对主键索引 id = 10 的记录，添加了一个 `X,REC_NOT_GAP` 锁，表示只锁定了记录。

同样 `for share` 是对表添加了一个 IS 锁并对主键索引 id = 10 的记录，添加了一个 S 锁。

可以得出结论：

<font color = "red">对主键等值加锁，且值存在时，会对表添加意向锁，同时会对主键索引添加行锁。</font>

#### 3.3、主键等值查询 —— 数据不存在

```sql
mysql> select * from t where id = 11 for update;
```

如果是数据不存在的时候，会加什么锁呢？锁的范围又是什么？

在验证之前，分析一下数据的间隙。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111120194.png)

1. `id = 11` 是肯定不存在的。但是加了 `for update`，这时需要加 next-key lock，`id = 11` 所属区间为 (10,15] 的~~前开后闭~~区间；
2. 因为是`等值查询`，不需要锁 `id = 15` 那条记录，next-key lock 会退化为间隙锁；
3. 最终区间为 (10,15) 的前开后开区间。

使用 data_locks 分析一下锁信息：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111122326.png)

看下锁的信息 `X,GAP` 表示加了间隙锁，其中 LOCK_DATA = 15，表示锁的是 主键索引 id = 15 之前的间隙。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111123836.png)

此时在另一个 Session 执行 SQL，答案显而易见，是 id = 12 不可以插入，而 id = 15 是可以更新的。

可以得出结论，在数据不存在时，主键等值查询，会锁住该主键查询条件所在的间隙。

#### 3.4、主键范围查询（重点）
```sql
mysql> begin; select * from t where id >= 10 and id < 11 for update;
```

根据 《MySQL 45 讲》分析得出下面结果：

1. `id >= 10` 定位到 10 所在的区间 (10,+∞)；
2. 因为是 >= 存在等值判断，所以需要包含 10 这个值，变为 [10,+∞) 前闭后闭区间；
3. `id < 11` 限定后续范围，则根据 11 判断下一个区间为 15 的~~前开后闭~~区间；
4. 结合起来则是 [10,15]。（不完全正确）

先看下 data_locks
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111134246.png)

可以看到除了表锁之外，还有 id = 10 的行锁（`X,REC_NOT_GAP`）以及主键索引 id = 15 之前的间隙锁（`X,GAP`）。

所以实际上 id = 15 是可以进行更新的。也就是说`前开后闭区间`出现了问题，个人认为应该是 `id < 11` 这个条件判断，导致不需要进行了锁 15 这个行锁。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111147422.png)

结果验证也是正确的，id = 12 插入阻塞，id = 15 更新成功。

当范围的右侧是包含等值查询呢？
```sql
mysql> begin; select * from t where id > 10 and id <= 15 for update;
```

来分析一下这个 SQL：

1. `id > 10` 定位到 10 所在的区间 (10,+∞)；
2. `id <= 15` 定位是 (-∞, 15]；
3. 结合起来则是 (10,15]。

同样先看一下 data_locks
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111148301.png)

可以看出只添加了一个主键索引 id = 15 的 X 锁。

验证下 id = 15 是否可以更新？再验证 id = 16 是否可以插入？
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111149565.png)

事实证明是没有问题的！

当然，这里有小伙伴会说，在 《MySQL 45 讲》 里面说这里有一个 bug，会锁住下一个 next-key。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111546527.png)

事实证明，这个 bug 已经被修复了。修复版本为 `MySQL 8.0.18`。但是并没有完全修复！！！

>参考链接地址：
>[https://dev.mysql.com/doc/rel...](https://link.segmentfault.com/?enc=kFSxNYHdKugREc7K7RuGfw%3D%3D.N5%2FqEf0JNiTstAgOFzQsI8gWefO6M7KUAegXbxabM%2BM%2BwyUWKqj7uFu9LbZCIqcjKmSAxcJnF5Oh3cAZNmFpUIyxNL5L7%2BUe1xeSaDC%2FJjCld4ACu2xjvqGs6h5bGlrt)
>搜索关键字：Bug #29508068)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111548516.png)

咱们可以分别用 8.0.17 进行复现一下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111548785.png)

在 8.0.17 中 `id <= 15` 会将 id = 20 这条数据也锁着，而在 8.0.25 版本中则不会。所以这个 bug 是被修复了的。

再来看下是`前开后闭`还是`前开后开`的问题，严谨一下，使用 8.0.17 和 8.0.18 做比较。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111549220.png)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111549395.png)

现在我估计大概率是在 8.0.18 版本修复 `Bug #29508068` 的时候，把这个`前开后闭`给优化成了`前开后开`了。

对比 data_locks 数据：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408111550545.png)

注意红色下划线部分，在 8.0.17 版本中 `id < 17` 时 LOCK_MODE 是 `X`，而在 8.0.25 版本中则是 `X,GAP`。

# 4、总结

本文主要通过实际操作，对主键加锁时的 next-key lock 范围进行了验证，并查阅资料，对比版本得出不同的结论。

#### 结论一：

1. 加锁时，会先给表添加意向锁，IX 或 IS；
2. 加锁是如果是多个范围，是分开加了多个锁，每个范围都有锁；（这个可以实践下 id < 20 的情况）
3. 主键等值查询，数据存在时，会对该主键索引的值加行锁 `X,REC_NOT_GAP`；
4. 主键等值查询，数据不存在时，会对查询条件主键值所在的间隙添加间隙锁 `X,GAP`；
5. 主键等值查询，范围查询时情况则比较复杂：
    1. 8.0.17 版本是前开后闭，而 8.0.18 版本及以后，进行了优化，主键时判断不等，不会锁住后闭的区间。
    2. 临界 `<=` 查询时，8.0.17 会锁住下一个 next-key 的前开后闭区间，而 8.0.18 及以后版本，修复了这个 bug。
> 优化后，导致后开，这个不知道是因为优化后，主键的区间会直接后开，还是因为是个 bug。具体小伙伴可以尝试一下。

#### 结论二

通过使用 `select * from performance_schema.data_locks;` 和操作实践，可以看出 LOCK_MODE 和 LOCK_DATE 的关系：

|LOCK_MODE|LOCK_DATA|锁范围|
|:--|:--|:--|
|X,REC_NOT_GAP|15|15 那条数据的行锁|
|X,GAP|15|15 那条数据之前的间隙，不包含 15|
|X|15|15 那条数据的间隙，包含 15|

1. `LOCK_MODE = X` 是前开后闭区间；
2. `X,GAP` 是前开后开区间（间隙锁）；
3. `X,REC_NOT_GAP` 行锁。

基本已经摸清主键的 next-key lock 范围，注意版本使用的是 8.0.25。

#### 疑问

1. 那唯一索引的 next-key lock 范围是什么?
2. 当索引覆盖时锁的范围和加锁的索引分别是什么？
3. 我为什么说这个 bug 没有完全修复，也是在非主键唯一索引中复现了这个 bug​。

文章篇幅有限，小伙伴可以先自己思考一下，尽量自己操作试一试，实践出真知。至于具体答案，那就需要下一篇文章进行验证并总结结论了。



转载自：[MySQL next-key lock 加锁范围是什么？](https://segmentfault.com/a/1190000040129107)


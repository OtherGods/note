
> 本文来自公号 MySQL 技术，JavaGuide 对其做了补充完善。原文地址：[https://mp.weixin.qq.com/s/d5OowNLtXBGEAbT31sSH4gopen in new window](https://mp.weixin.qq.com/s/d5OowNLtXBGEAbT31sSH4g)

优化 SQL 的第一步应该是读懂 SQL 的执行计划。本篇文章，我们一起来学习下 MySQL `EXPLAIN` 执行计划相关知识。

## 1、什么是执行计划？

**执行计划** 是指一条 SQL 语句在经过 **MySQL 查询优化器** 的优化会后，具体的执行方式。

执行计划通常用于 SQL 性能分析、优化等场景。通过 `EXPLAIN` 的结果，可以了解到如数据表的查询顺序、数据查询操作的操作类型、哪些索引可以被命中、哪些索引实际会命中、每个数据表有多少行记录被查询等信息。

## 2、如何获取执行计划？

MySQL 为我们提供了 `EXPLAIN` 命令，来获取执行计划的相关信息。

需要注意的是，`EXPLAIN` 语句并不会真的去执行相关的语句，而是通过查询优化器对语句进行分析，找出最优的查询方案，并显示对应的信息。

`EXPLAIN` 执行计划支持 `SELECT`、`DELETE`、`INSERT`、`REPLACE` 以及 `UPDATE` 语句。我们一般多用于分析 `SELECT` 查询语句，使用起来非常简单，语法如下：

```
EXPLAIN + SELECT 查询语句；
```

我们简单来看下一条查询语句的执行计划：
```sql
EXPLAIN + SELECT 查询语句；
```

我们简单来看下一条查询语句的执行计划：
```sql
mysql> explain SELECT * FROM dept_emp WHERE emp_no IN (SELECT emp_no FROM dept_emp GROUP BY emp_no HAVING COUNT(emp_no)>1);
+----+-------------+----------+------------+-------+-----------------+---------+---------+------+--------+----------+-------------+
| id | select_type | table    | partitions | type  | possible_keys   | key     | key_len | ref  | rows   | filtered | Extra       |
+----+-------------+----------+------------+-------+-----------------+---------+---------+------+--------+----------+-------------+
|  1 | PRIMARY     | dept_emp | NULL       | ALL   | NULL            | NULL    | NULL    | NULL | 331143 |   100.00 | Using where |
|  2 | SUBQUERY    | dept_emp | NULL       | index | PRIMARY,dept_no | PRIMARY | 16      | NULL | 331143 |   100.00 | Using index |
+----+-------------+----------+------------+-------+-----------------+---------+---------+------+--------+----------+-------------+
```

可以看到，执行计划结果中共有 12 列，各列代表的含义总结如下表：
| 列名          | 含义                                         |
| ------------- | -------------------------------------------- |
| id            | SELECT 查询的序列标识符                      |
| select_type   | SELECT 关键字对应的查询类型                  |
| table         | 用到的表名                                   |
| partitions    | 匹配的分区，对于未分区的表，值为 NULL        |
| type          | 表的访问方法                                 |
| possible_keys | 可能用到的索引                               |
| key           | 实际用到的索引                               |
| key_len       | 所选索引的长度                               |
| ref           | 当使用索引等值查询时，与索引作比较的列或常量 |
| rows          | 预计要读取的行数                             |
| filtered      | 按表条件过滤后，留存的记录数的百分比         |
| Extra         | 附加信息                                     |

## 3、如何分析 EXPLAIN 结果？

为了分析 `EXPLAIN` 语句的执行结果，我们需要搞懂执行计划中的重要字段。

### 3.1、id

SELECT 标识符，是查询中 SELECT 的序号，用来标识整个查询中 SELELCT 语句的顺序。

id 如果相同，从上往下依次执行。id 不同，id 值越大，执行优先级越高，如果行引用其他行的并集结果，则该值可以为 NULL。

### 3.2、select_type

查询的类型，主要用于区分普通查询、联合查询、子查询等复杂的查询，常见的值有：

- **SIMPLE**：简单查询，不包含 UNION 或者子查询。
- **PRIMARY**：查询中如果包含子查询或其他部分，外层的 SELECT 将被标记为 PRIMARY。
- **SUBQUERY**：子查询中的第一个 SELECT。
- **DEPENDENT UNION**：一般是子查询中的第二个select语句（取决于外查询，mysql内部也有些优化）
```sql
explain select * from student s where s.c_id in ( select s1.c_id from student s1 where c_id = 2 union select s2.c_id from student s2 where s2.id >2000);
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407252311373.png)

- **UNION**：在 UNION 语句中，UNION 之后出现的 SELECT。
```sql
EXPLAIN select id from student s UNION select id from class c
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407252310824.png)

- **DERIVED**：在 FROM 中出现的子查询将被标记为 DERIVED。
  MySQL 5.7 之后好像没有这个状态了
- **UNION RESULT**：包含union的结果集，在union和union all语句中,因为它不需要参与查询，所以id字段为null
```sql
EXPLAIN select id from student s UNION select id from class c 
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407252312629.png)

- **uncacheable subquery**:表示使用子查询的结果不能被缓存
```sql
explain select * from student where c_id = (select id from student where id = 1 and c_id=@@sort_buffer_size);
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407252313062.png)

- **unchchaable union**：union 的结果不能被缓存

### 3.3、table

查询用到的表名，每行都有对应的表名，表名除了正常的表之外，也可能是以下列出的值：

- **`<unionM,N>`** : 本行引用了 id 为 M 和 N 的行的 UNION 结果；
- **`<derivedN>`** : 本行引用了 id 为 N 的表所产生的的派生表结果。派生表有可能产生自 FROM 语句中的子查询。
- **`<subqueryN>`** : 本行引用了 id 为 N 的表所产生的的物化子查询结果。

### 3.4、type（重要，判断是否走索引）

查询执行的类型，描述了查询是如何执行的。所有值的顺序从最优到最差排序为：system > const > eq_ref > ref > fulltext > ref_or_null > index_merge > unique_subquery > index_subquery > range > index > ALL

一般情况下，要保证查询达到range级别，最好达到ref级别。

常见的几种类型具体含义如下：
- **system**：如果表使用的引擎对于表行数统计是精确的（如：MyISAM），且表中只有一行记录的情况下，访问方法是 system ，是 const 的一种特例。
- **const**：表中最多只有一行匹配的记录，一次查询就可以找到，常用于使用主键或唯一索引的所有字段作为查询条件。
- **eq_ref**：当连表查询时，前一张表的行在当前这张表中只有一行与之对应。是除了 system 与 const 之外最好的 join 方式，常用于使用主键或唯一索引的所有字段作为连表条件。
- **ref**：使用普通索引作为查询条件，查询结果可能找到多个符合条件的行。
- **index_merge**：当查询条件使用了多个索引时，表示开启了 Index Merge 优化，此时执行计划中的 key 列列出了使用到的索引。
- **range**：对索引列进行范围查询，执行计划中的 key 列表示哪个索引被使用了。
- **index**：查询遍历了整棵索引树，与 ALL 类似，只不过扫描的是索引，而索引一般在内存中，速度更快。
- **ALL**：全表扫描。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407252317430.png)

#### 3.4.1、SQL演示
1. All
	全表扫描，出现这个，数据量比较大的情况下，需要优化  
	`explain select * from emp;`  
2. index
	全索引扫描，效率比All好。两种情况：1.使用了覆盖索引。2.使用了索引排序  
	`explain select empno from emp;`  
3. range
	表示利用索引查询限制了范围，在指定范围内查询  
	`explain select * from emp where empno between 7000 and 7500;`  
4. index_subquery
	利用索引来关联子查询，不再扫描全表  
	`explain select * from emp where emp.job in (select job from t_job);`  
5. unique_subquery和index_subquery类似，使用的是唯一索引  
	`explain select * from emp e where e.deptno in (select distinct deptno from dept);`  
6. index_merge  
	查询中使用多个索引组合使用  
7. ref_or_null  
	对某个索引需要关联查询，也需要null值的条件  
	`explain select * from emp e where e.mgr is null or e.mgr=7369;`  
8. ref  
	使用了非唯一性索引进行数据查找  
	`create index idx_3 on emp(deptno);`  
	`explain select * from emp e,dept d where e.deptno =d.deptno;`  
9. eq_ref  
	使用了唯一性索引进行数据查找  
	`explain select * from emp,emp2 where emp.empno = emp2.empno;`
10. const  
	`表里最多就只有一行匹配`
11. system  
	`表只有一行记录，等于系统表`

参考：[https://juejin.cn/post/7161254854571065375](https://juejin.cn/post/7161254854571065375)

### 3.5、possible_keys

possible_keys 列表示 MySQL 执行查询时可能用到的索引。如果这一列为 NULL ，则表示没有可能用到的索引；这种情况下，需要检查 WHERE 语句中所使用的的列，看是否可以通过给这些列中某个或多个添加索引的方法来提高查询性能。

### 3.6、key（重要，判断是否走索引）

key 列表示 MySQL 实际使用到的索引。如果为 NULL，则表示未用到索引。

### 3.7、key_len

key_len 列表示 MySQL 实际使用的索引的最大长度；当使用到联合索引时，有可能是多个列的长度和。在满足需求的前提下越短越好。如果 key 列显示 NULL ，则 key_len 列也显示 NULL 。

### 3.8、rows

rows 列表示根据表统计信息及选用情况，大致估算出找到所需的记录或所需读取的行数，数值越小越好。

### 3.9、Extra（重要）

这列包含了 MySQL 解析查询的额外信息，通过这些信息，可以更准确的理解 MySQL 到底是如何执行查询的。常见的值如下：

- **Using filesort**：在排序时使用了外部的索引排序，没有用到表内索引进行排序。
- **Using temporary**：MySQL 需要创建临时表来存储查询的结果，常见于 ORDER BY 和 GROUP BY。
	- 建立临时表来保存中间结果，查询完成之后把临时表删除
	- `explain select ename,count(*) from emp where deptno = 10 group by ename;`
- **Using index**：表明查询使用了覆盖索引，不用回表，查询效率非常高。
- **Using index condition**：表示查询优化器选择使用了索引条件下推这个特性。
- **Using where**：表明查询使用了 WHERE 子句进行条件过滤。~~一般在没有使用到索引的时候会出现~~ （ChatGPT说：Using where不代表未使用索引）。
- **Using join buffer (Block Nested Loop)**：连表查询的方式，表示当被驱动表的没有使用索引的时候，MySQL 会先将驱动表读出来放到 join buffer 中，再遍历被驱动表与驱动表进行查询。

这里提醒下，当 Extra 列包含 Using filesort 或 Using temporary 时，MySQL 的性能可能会存在问题，需要尽可能避免。

### 3.10、partitions

查询所匹配记录所在的分区，对于未分区的表，值为 `NULL`。

### 3.11、ref

表示在查询索引时，哪些列或者常量被用来与索引的值进行比较。

### 3.12、filtered

表示估算的经过查询条件删选出的列数的百分比。例如 `rows` 是 1000，`filtered` 是 50（50%），则实际筛选出的列数为 1000 * 50% = 500。

## 4、参考

- [https://dev.mysql.com/doc/refman/5.7/en/explain-output.htmlopen in new window](https://dev.mysql.com/doc/refman/5.7/en/explain-output.html)
- [https://juejin.cn/post/6953444668973514789](https://juejin.cn/post/6953444668973514789)

---

著作权归JavaGuide(javaguide.cn)所有 基于MIT协议 原文链接：[https://javaguide.cn/database/mysql/mysql-query-execution-plan.html](https://javaguide.cn/database/mysql/mysql-query-execution-plan.html)





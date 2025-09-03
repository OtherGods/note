#深度分页 

对照：[4、高性能：深度分页介绍及优化建议](2、相关技术/23、高并发高可用高性能/高性能-JavaGuide/3、数据库优化/4、高性能：深度分页介绍及优化建议.md)

这里只是对深度分页理论的介绍，深度分页进一步介绍及将深度分页运用到实际场景中（定时任务循环分批扫表）参照 [68、MySQL的深度分页如何优化](2、相关技术/4、数据库-MySQL/Hollis/68、MySQL的深度分页如何优化.md)


## 1、深度分页SQL

深度分页SQL：`select * from t_pz_admin_user limit 390000,10;` 查询(390000,390010]区间的记录，**记录中包含所有数据**；
- 这句 SQL 要求`limit 390000,10`，也就是查询第390001到390010个数据，但是 MySQL 会**查询前390010行，然后将前390000行抛弃**，最后结果集中就只剩下了第101到110行，执行结束

## 2、优化思路如下

### 1、减少回表(子查询)

子查询中使用覆盖索引优化，子查询使用覆盖索引快速找到第3900001行记录对应的id，在主查询的where条件中使用

```sql
-- sql①——深度分页sql
select * from t_pz_admin_user limit 390000,10;
-- sql①——走覆盖索引去除回表
select id from t_pz_admin_user limit 390000,10;

-- sql②的子查询
select id from t_pz_admin_user limit 390000,1;
-- sql②的主查询
select * from t_pz_admin_user where id >= 390001 limit 10;
-- sql②——深度分页优化后的sql
SELECT
	* 
FROM
	t_pz_admin_user 
WHERE
	id >= ( SELECT id FROM t_pz_admin_user LIMIT 390000, 1 ) 
	LIMIT 10;

-- 上述sql从上到下执行对应的执行计划
explain select * from t_pz_admin_user limit 390000,10;
explain select id from t_pz_admin_user limit 390000,10;
explain select id from t_pz_admin_user limit 390000,1;
explain select * from t_pz_admin_user where id >= 390001 limit 10;
explain select * from t_pz_admin_user where id >= (select id from t_pz_admin_user limit 390000,1) limit 10;
```

![image-20230622191757300](D:\Tyora\AssociatedPicturesInTheArticles\limit深度分页优化\image-20230622191757300.png)
![image-20230622194016908](D:\Tyora\AssociatedPicturesInTheArticles\limit深度分页优化\image-20230622194016908.png)


通过执行计划的Extra字段可以看出深度分页优化后的sql的子查询使用了覆盖索引；通过上图的执行时间可以看出优化后的效果。

![image-20230622193844717](D:\Tyora\AssociatedPicturesInTheArticles\limit深度分页优化\image-20230622193844717.png)

### 2、《高性能MSQL》介绍的（延迟连接）

在《高性能MySQL》这本书中，专门有一个章节介绍了如何优化LIMIT和OFFSET字句的，他提到了一种优化方案。

```sql
select film_id,desc from film order by titile limit 50,5;
```

优化后的sql：

```sql
select film_id,desc 
from film
INNER JOIN (SELECT film_id FROM film ORDER BY titile LIMIT 50, 5) AS lim
USING (film_id)

-- `USING (film_id)` 表示连接条件是 `film_id` 字段。
```

这是一种"延迟连接"的方式，他允许服务器在不访问行的情况下检查索引中尽可能少的数据，然后一旦找到所需的行，就将它们与整个表连接，以从该行中检索其他列。

### 3、提前预估

还有一种方式，就是如果能提前预估要查询的分页的条件的话，是可以很大程度提升性能的。比如记住上一页的最大ID，下一页查询的时候，就可以根据id >　max_id_in_last_page 进行查询。
# 问题发现

线上有一个反欺诈相关的定时任务执行连续多次失败：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507191908112.png)

于是紧急排查日志，发现在任务执行的时间段，有大量报错：
```sql
Cause: ERR-CODE: [TDDL-4202][ERR_SQL_QUERY_TIMEOUT] Slow query leads to a timeout exception, please contact DBA to check slow sql. SocketTimout:12000 ms,
```

在这个日志的上下文不远处就定位到这条慢SQL：
```sql
SELECT DISTINCT
	buyer_id AS buyerId,
	seller_id AS sellerId 
FROM
	fraud_risk_case 
WHERE
	subject_id_enum = 'BUYER_SELLER_BOTH' 
	AND ( buyer_id = ? OR seller_id = ? ) 
	AND product_type_enum = ? 
ORDER BY
	id DESC 
	LIMIT 100
```

这条SQL的主要目的是找到是否有买卖家之间存在关联关系的数据。

通过执行explain，我们看了一下执行计划：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507191913625.png)

通过这个执行计划可以发现，type = index ，extra = Using where; Using index ，表示SQL因为不符合最左前缀匹配，而扫描了整颗索引树，故而很慢。

于是查看这张表的建表语句，确实存在subject_id_enum和product_type_enum字段的联合索引，但是这个字段并不是前导列：
```sql
idx_subject_product(subject_id,subject_id_enum,product_type)
```

# 问题解决

定位到问题之后，那解决起来就很简单了，只需要增加正确的索引或者修改SQL就行了。于是修改表结构，增加新的索引：
```sql
ALTER TABLE `fraud_risk_case`
	ADD KEY `idx_subject_type_product_user` (`subject_id_enum`,`product_type_enum`,`buyer_id`,`seller_id`);
```

经过修改后，再执行以下执行计划：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507191920857.png)

可以看到，type=ref，说明用到了普通索引，你并且rows也变少了，整个SQL大大提升了查询速度，任务失败的问题得到解决。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507191921555.png)

参考：
[43、SQL执行计划分析的时候，要关注哪些信息？](2、相关技术/4、数据库-MySQL/Hollis/43、SQL执行计划分析的时候，要关注哪些信息？.md)

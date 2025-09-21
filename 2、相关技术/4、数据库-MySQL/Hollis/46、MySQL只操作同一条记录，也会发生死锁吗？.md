#数据库死锁 

# 典型回答

会。

因为数据库的锁锁的是索引，并不是记录。

当我们在事务中，更新一条记录的时候，如果用到普通索引作为条件，那么会先获取普通索引的锁，然后再尝试获取主键索引的锁。

那么这个时候，如果刚好有一个线程，已经拿到了这条记录的主键索引的锁后，同时尝试在该事务中去拿该记录的普通索引的锁。

这时候就会发生死锁。
```sql
update my_table set name = 'hollis',age = 22 where name = "hollischuang";

这个SQL会先对name加锁， 然后再回表对id加锁。

-----

select * from my_table where id = 15 for update;

update my_table set age = 33 where name like "hollis%";

以上SQL，会先获取主键的锁，然后再获取name的锁。
```

为了避免这种死锁情况的发生，可以在应用程序中设置一个规定的索引获取顺序，例如，只能按照主键索引->普通索引的顺序获取锁，这样就可以避免不同的线程出现获取不同顺序锁的情况，进而避免死锁的发生（靠SQL保证）。

# MySQL实战45讲

对比：[案例八：一个死锁的例子](21、为什么我只改一行的语句，锁这么多？#案例八：一个死锁的例子)

表结构和数据
```sql
--表结构
CREATE TABLE `t` (
  `id` int NOT NULL,
  `c` int DEFAULT NULL,
  `d` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `c` (`c`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 数据
INSERT INTO `hhy`.`t`(`id`, `c`, `d`) VALUES (0, 0, 0);
INSERT INTO `hhy`.`t`(`id`, `c`, `d`) VALUES (5, 5, 5);
INSERT INTO `hhy`.`t`(`id`, `c`, `d`) VALUES (10, 10, 10);
INSERT INTO `hhy`.`t`(`id`, `c`, `d`) VALUES (15, 15, 15);
INSERT INTO `hhy`.`t`(`id`, `c`, `d`) VALUES (20, 20, 20);
INSERT INTO `hhy`.`t`(`id`, `c`, `d`) VALUES (25, 25, 25);
```

开启两个事务分别更新普通索引列c等于10的记录，
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507191508386.png)

可以配合sql查看死锁：
```sql
SHOW ENGINE INNODB STATUS
```
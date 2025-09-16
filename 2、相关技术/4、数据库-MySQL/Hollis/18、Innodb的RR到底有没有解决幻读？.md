# 典型回答

InnoDB中的REPEATABLE READ这种隔离级别通过 **`间隙锁+MVCC` 解决了大部分的幻读问题，但是并不是所有的幻读都能解读，想要彻底解决幻读，需要使用Serializable的隔离级别**。

RR中，通过间隙锁解决了部分当前读的幻读问题，通过增加间隙锁将记录之间的间隙锁住，**避免新的数据插入**。

RR中，通过MVCC机制的，解决了快照读的幻读问题，RR中的快照读只有第一次读时会创建ReadView，在同一个事务中后面如不修改这行数据那么ReadView不发生变化，在第二次快照读时根据ReadView可以找到第一次快照读时的数据（无论数据是在undolog中还是表中）,所以不会发生幻读。
- <font color="red" size=5>一个事务中多次快照读只要ReadView相同(即当前事务中没有发生当前读)那么每次读取到的都是相同的快照<font color="blue">(我总结的应该没问题)</font></font>

**但是，如果两个事务，事务1先进行快照读，然后事务2插入了一条记录并提交，再在事务1中进行update新插入的这条记录是可以更新成功的，这就是发生了幻读。**

**还有一种场景，如果两个事务，事务1先进行快照读，然后事务2插入了一条记录并提交，在事务1中进行了当前读之后，再进行快照读也会发生幻读。**

# 扩展知识

## MVCC解决幻读（快照读）

<font size=5 color="red">MVCC能解决RR级别下面的快照读的幻读问题</font>

MVCC，是Multiversion Concurrency Control的缩写，翻译过来是多版本并发控制，和数据库锁一样，他也是一种并发控制的解决方案。它**主要用来解决 `读-写` 并发的情况**。
[19、如何理解MVCC？](2、相关技术/4、数据库-MySQL/Hollis/19、如何理解MVCC？.md)

我们知道，在MVCC中有两种读，一种是快照读、一种是当前读：
- [20、当前读和快照读有什么区别？](2、相关技术/4、数据库-MySQL/Hollis/20、当前读和快照读有什么区别？.md)
- [110、什么是ReadView，什么样的ReadView可见？](2、相关技术/4、数据库-MySQL/Hollis/110、什么是ReadView，什么样的ReadView可见？.md)

所谓快照读，就是读取的是快照数据，即快照生成的那一刻的数据，像我们常用的普通的SELECT语句在不加锁情况下就是快照读。
```sql
SELECT * FROM xx_table WHERE ...
```

- 在 RC 中，每次读取都会重新生成一个`ReadView`，总是读取行的最新版本。
- 在 RR 中，`ReadView`会在事务中第一次SELECT语句执行时生成，只有在本事务中对数据进行更改才会更新`ReadView`。

那么也就是说，如果在RR下，一个事务中的多次查询，是不会查询到其他的事务中的变更内容的，所以，也就是可以解决幻读的。

如果我们把事务隔离级别设置为RR，那么因为有了MVCC的机制，就能解决幻读的问题：

有这样一张表：
```sql
CREATE TABLE `users` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `gmt_create` datetime NOT NULL,
  `age` int NOT NULL,
  `name` varchar(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `age` (`age`)
) ENGINE=InnoDB;

INSERT INTO users(gmt_create,age,name) values(now(),18,'Hollis');
INSERT INTO users(gmt_create,age,name) values(now(),28,'HollisChuang');
INSERT INTO users(gmt_create,age,name) values(now(),38,'Hollis666');
```

执行如下事务时序：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507171949095.png)

可以看到，同一个事务中的两次查询结果是一样的，就是在RR级别下，因为有快照读，所以第二次查询其实读取的是一个快照数据。

## 间隙锁与幻读

上面我们讲过了MVCC能解决RR级别下面的快照读的幻读问题，那么当前读下面的幻读问题怎么解决呢？

当前读就是读取最新数据，所以，加锁的 SELECT，或者对数据进行增删改都会进行当前读，比如：
```sql
SELECT * FROM xx_table LOCK IN SHARE MODE;

SELECT * FROM xx_table FOR UPDATE;

INSERT INTO xx_table ...

DELETE FROM xx_table ...

UPDATE xx_table ...
```

举一个下面的例子：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507171957456.png)

像上面这种情况，在RR的级别下，当我们使用SELECT … FOR UPDATE的时候，会进行加锁，不仅仅会对行记录进行加锁，还会对记录之间的间隙进行加锁，这就叫做间隙锁。
[22、MySQL的行级锁锁的到底是什么？](2、相关技术/4、数据库-MySQL/Hollis/22、MySQL的行级锁锁的到底是什么？.md)

因为记录之间的间隙被锁住了，所以事务2的插入操作就被阻塞了，一直到事务1把锁释放掉他才能执行成功。

因为事务2无法插入数据成功，所以也就不会存在幻读的现象了。所以，在RR级别中，通过加入间隙锁的方式，就避免了幻读现象的发生。

## 解决不了的幻读

前面我们介绍了快照读（无锁查询）和当前读（有锁查询）下是如何解决幻读的问题的，但是，上面的例子就是幻读的所有情况了吗？显然并不是。

我们说MVCC只能解决快照读的幻读，那如果在一个事务中发生了当前读，并且在另一个事务插入数据前没来得及加间隙锁的话，会发生什么呢？

那么，我们稍加修改一下上面的SQL代码，通过当前读的方式进行查询数据：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507181553680.png)

在上面的例子中，在事务1中，我们并没有在事务开启后立即加锁，而是进行了一次普通的查询，然后事务2插入数据成功之后，再通过事务1进行了2次查询。

我们发现，事务1后面的两次查询结果完全不一样，没加锁的情况下，就是快照读，读到的数据就和第一次查询是一样的（因为`ReadView`相同），就不会发生幻读。但是第二次查询加了锁，就是当前读，那么读取到的数据就有其他事务提交的数据了（重新生成了`ReadView`），就发生了幻读。

那么，如果你理解了上面的这个例子，并且你也理解了当前读的概念，那么你很容易就能想到，下面的这个CASE其实也是会发生幻读的：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507181602297.png)

这里发生幻读的原理，和上面的例子其实是一样的，那就是MVCC只能解决快照读中的幻读问题，而对于当前读（SELECT FOR UPDATE、UPDATE、DELETE等操作）还是会产生幻读的现象的。即，在同一个事务里面，如果既有快照读，又有当前读，那是会产生幻读的、

**UPDATE语句也是一种当前读，所以它是可以读到其他事务的提交结果的。**

为什么事务1的最后一次查询和倒数第二次查询的结果也不一样呢？

是<font color="red" size=5>因为根据快照读的定义，在RR中，<font color="blue" size=5>如果本事务中发生了数据的修改，那么就会更新快照</font>，那么最后一次查询的结果也就发生了变化</font>。

## 如何避免幻读

那么了解了幻读的解决场景，以及不能解决的几个CASE之后，我们来总结一下该如何解决幻读的问题呢？

首先，如果想要彻底解决幻读的问题，在InnoDB中只能使用Serializable这种隔离级别。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507181605873.png)

那么，如果想在一定程度上解决或者避免发生幻读的话，使用RR也可以，但是RC、RU肯定是不行的。

在RR级别中，能使用快照读（无锁查询）的就使用快照读，这样不仅可以减少锁冲突，提升并发度，而且还能避免幻读的发生。

那么，如果在并发场景中，一定要加锁的话怎么办呢？那就一定要在事务一开始就立即加锁，这样就会有间隙锁，也能有效的避免幻读的发生。
但是需要注意的是，<font color="red">间隙锁是导致死锁的一个重要根源</font>~所以，用起来也需要慎重。

# 1、概述

知乎文章[深入理解数据库行锁](https://zhuanlan.zhihu.com/p/52678870)上盗来的图：

![img](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\v2-5cf8b96fdca1428e6f3cce863fdfa73e_b.jpg)



## 1.1 定义

锁是计算机协调多个进程或线程并发访问某一资源的机制

在出具库中，除传统的计算资源（如CPU、RAM、I/O德国）的争用之外，数据也是一种共许多用户共享的资源，如何保证数据并发访问的一致性、有效性是所有数据库必须解决的一个问题，锁冲突也是影响数据并发访问性能的一个重要因素。从这个角度来说，锁对数据库而言是非常重要，也更加复杂。

## 1.2 生活购物

![image-20220903221742685](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220903221742685.png)

## 1.3 锁的分类

从数据操作的类型分为：读锁、写锁

- 读锁：共享锁，针对每一份数据，多个读操作可以同时进行而不会相互影响
- 写锁：排他锁，当前写操作没有完成之前，他会阻塞其他写锁和读锁

从对数据操作的粒度分为：表锁和行锁



# 2、三锁

## 2.1 表锁（偏读）

### 2.1.1特点

表锁偏向MyISAM存储引擎，开销小，加锁快，无死锁，锁定粒度大，发生锁冲突的概率最高，并发最低

> 摘自：https://blog.csdn.net/soonfly/article/details/70238902
>
> MySQL的表级锁有两种模式：**表共享读锁（Table Read Lock）**和**表独占写锁（Table Write Lock）**。

**与表锁相关的操作**

> 摘自：https://blog.csdn.net/soonfly/article/details/70238902
>
> MyISAM在执行查询语句（SELECT）前，会自动给涉及的所有表加读锁，在执行更新操作 （UPDATE、DELETE、INSERT等）前，会自动给涉及的表加写锁，这个过程并不需要用户干预，因此，用户一般不需要直接用LOCK TABLE命令给MyISAM表显式加锁。

1. 查看表上加过的锁：show open tables；Database代表数据库、Table代表某个数据库下的表、in_use代表的是表有没有使用锁
   ![image-20220903224023444](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220903224023444.png)
2. 手动加表锁的操作：lock table 表名1 read(write),表名2 read(write),……；
3. 解表上的锁：unlock tables;

### 2.1.2 案例分析：

1. 建表SQL

   ```sql
   create table mylock (
       id int not null primary key auto_increment,
       name varchar(20) default ''
   ) engine myisam;
   
   insert into mylock(name) values('a');
   insert into mylock(name) values('b');
   insert into mylock(name) values('c');
   insert into mylock(name) values('d');
   insert into mylock(name) values('e');
   ```

   分别给两个表加读锁和写锁示例（例如：mylock表和book表加读锁和写锁）：
   ![image-20220903224439339](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220903224439339.png)

2. 加读锁
   mylock表加读锁后：

   ![image-20220903233022721](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220903233022721.png)
   ![image-20220903233116897](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220903233116897.png)
   ![image-20220903233143101](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220903233143101.png)
   ![image-20220903233212512](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220903233212512.png)

3. 加写锁
   mylock表加读锁后：

   ![image-20220903234153824](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220903234153824.png)
   ![image-20220903234232009](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220903234232009.png)

4. 结论
   ![image-20220903234425920](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220903234425920.png)
   简而言之就是读锁会阻塞写，但是不会阻塞读；而写锁会阻塞读和写。

   MyISAM的读写锁是调度是写优先，这也是myisam不适合作为主的引擎的原因，因为写锁后，其他线程不能做任何操作，大量的更新会使查询很难得到锁，从而造成永远阻塞。



### 2.1.3 表锁分析

读锁

> 对于加锁的表：
>
> 1. <u>在当前加锁的会话中只能对该表执行查询操作，其他操作都会报错</u>
> 2. <u>在另一个会话中只能对加锁的表执行查询操作，增删改操作会阻塞，直到该表解锁</u>
>
> 对于未加锁的表：
>
> 1. 在当前加锁的会话中对该表执行的任何操作都会报错
> 2. 在另一个会话中可以对该表执行增删改查操作

写锁

> 对于加写锁的表：
>
> 1. <u>在当前加锁的会话中对该表可以执行增上改查操作</u>
> 2. <u>在另一个会话中对加锁的表执行的任何操作都会被阻塞，直到表解锁</u>
>
> 对于未加写锁的表：
>
> 1. 在当前加锁的会话中对该表执行的任何操作都会报错
> 2. 在另一个会话中可以对该表执行增删改查

表锁分析：可以通过检查table_locks_waited和table_locks_immediate状态变量来查分系统上的表锁；
SQL：show status like 'table%';
![image-20220903235307302](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220903235307302.png)

## 2.2 行锁（偏写）

### 2.2.1 特点：

1. 偏向InnoDB存储引擎，开销大，加锁慢；会出现死锁；锁定粒度最小，发生锁冲突的概率最低，并发也是最高
2. InnoDB也MyISAM的最大不同有两点：一是支持事务（TRANSACTION）,并且sql语句中每一个分号都会自动提交事务；二是采用了行级锁

> 摘自：https://blog.csdn.net/soonfly/article/details/70238902
>
> InnoDB实现了以下两种类型的行锁：
> **共享锁（s）：**又称读锁。允许一个事务去读一行，阻止其他事务获得相同数据集的排他锁。若事务T对数据对象A加上S锁，则事务T可以读A但不能修改A，其他事务只能再对A加S锁，而不能加X锁，直到T释放A上的S锁。这保证了其他事务可以读A，但在T释放A上的S锁之前不能对A做任何修改。
>
> **排他锁（Ｘ）：**又称写锁。允许获取排他锁的事务更新数据，阻止其他事务取得相同的数据集共享读锁和排他写锁。若事务T对数据对象A加上X锁，事务T可以读A也可以修改A，其他事务不能再对A加任何锁，直到T释放A上的锁。

**与表锁相关的操作**

> 摘自：https://blog.csdn.net/soonfly/article/details/70238902
>
> 对于共享锁大家可能很好理解，就是多个事务只能读数据不能改数据。
> 对于排他锁大家的理解可能就有些差别，我当初就犯了一个错误，以为排他锁锁住一行数据后，其他事务就不能读取和修改该行数据，其实不是这样的。排他锁指的是一个事务在一行数据加上排他锁后，其他事务不能再在其上加其他的锁。
>
> mysql InnoDB引擎默认的修改数据语句：update,delete,insert都会自动给涉及到的数据加上排他锁，select语句默认不会加任何锁类型；
>
> 如果加排他锁可以使用select …for update语句，加共享锁可以使用select … lock in share mode语句。
>
> 所以加过排他锁的数据行在其他事务种是不能修改数据的，也不能通过for update和lock in share mode锁的方式查询数据，但可以直接通过select …from…查询数据，因为普通查询没有任何锁机制。
>
> ## InnoDB行锁实现方式
>
> **InnoDB行锁是通过给索引上的索引项加锁来实现的**，这一点MySQL与Oracle不同，后者是通过在数据块中对相应数据行加锁来实现的。InnoDB这种行锁实现特点意味着：**<u>*只有通过索引条件检索数据，InnoDB才使用行级锁，否则，InnoDB将使用表锁！*</u>**
> 在实际应用中，要特别注意InnoDB行锁的这一特性，不然的话，可能导致大量的锁冲突，从而影响并发性能。下面通过一些实际例子来加以说明。
>

1. 手动关闭InnoDB引擎在MySQL5.5之后的自动提交事务：SET autocommit=0;
2. 手动加共享行锁和排他行锁的操作在上面的引用中有

### 2.2.2 案例分析：

1. 建表SQL

   ```sql
   CREATE TABLE test_innodb_lock (
       a INT(11),
       b VARCHAR(16)
   )ENGINE=INNODB;
   
   INSERT INTO test_innodb_lock VALUES(1,'b2');
   INSERT INTO test_innodb_lock VALUES(3,'3');
   INSERT INTO test_innodb_lock VALUES(4, '4000');
   INSERT INTO test_innodb_lock VALUES(5,'5000');
   INSERT INTO test_innodb_lock VALUES(6, '6000');
   INSERT INTO test_innodb_lock VALUES(7,'7000');
   INSERT INTO test_innodb_lock VALUES(8, '8000');
   INSERT INTO test_innodb_lock VALUES(9,'9000');
   INSERT INTO test_innodb_lock VALUES(1,'b1');
   
   CREATE INDEX test_innodb_a_ind ON test_innodb_lock(a);
   
   CREATE INDEX test_innodb_lock_b_ind ON test_innodb_lock(b);
   
   SET autocommit=0;
   ```

   

2. 行锁定基本演示（这里只演示了行锁的排他锁的使用方式）
   ![image-20220904104008682](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220904104008682.png)

3. 无索引行锁升级为表锁
   
   1. varchar类型的字段在使用的时候不用‘’导致系统自动转换类型，使索引失效，从而导致行锁变为表锁
   
4. 间隙锁
   什么是间隙锁：当我们使用范围条件而不是相等条件检索数据，并请求共享或排他锁的时候，InnoDB会给符合条件的已有数据记录的索引项加锁；对于键值在条件范围内但并不存在的记录，叫做“间隙（GAP）”

   间隙锁的危害：
   因为Query执行过程中通过范围查找的话，他会锁定整个范围内所有的索引键值，即使这个键值并不存在。

   间隙锁有一个比较致命的弱点：当锁定一个范围键值之后，即使某些不存在的键值也会被我姑的锁定，而造成在锁定的时候无法擦哈如锁定键值范围内的任何数据。在某些场景下这可能会对性能造成很大的危害。

   间隙锁危害的示例：

   ![image-20220904115724540](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220904115724540.png)

5. 面试题：常考如何锁定一行
   ![image-20220904120729953](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\image-20220904120729953.png)

6. 结论
   InnoDB存储引擎由于实现了行级锁定，虽然在锁定机制的实现方面所带来的性能损耗可能比表级别的锁定要高一些，但是在整体并发处理能力方面要远远优于MyISAM的表级锁定的。当系统并发量比较高的时候，InnoDB的整体性能和MyISAM相比就会有比较明显的优势了。

   但是InnoDB的行级锁同样也有脆弱的一面，当我们使用不当的时候，可能会让InnoDB的整体性能表现不仅没有MyISAM高，甚至可能会更差。

### 2.2.3 行锁分析

共享行锁

> 对于加锁的行：
>
> 1. <u>在当前加锁的会话中可以获取对应行的共享锁和排他锁</u>
> 2. 在另一个会话中对于对应行只能获取共享锁，排他锁的获取会被阻塞
>
> 对于相同表中未加锁的行：没有影响

排他行锁

> 对于加写锁的行：
>
> 1. <u>在当前加锁的会话中可以获取对应行的共享锁和排他锁</u>
> 2. 在另一个会话中对加锁的表执行的任何操作都会被阻塞，直到表解锁
>
> 对于相同表中未加锁的行：没有影响
>



分析行锁定：通过检查InnoDB_row_lock状态变量来分析系统上的行锁争夺情况



对各个状态量的说明如下：

Innodb_row_lock_currnet_waits：当前正在等待锁定的数量
Innodb_row_lock_time：从系统启动到现在锁定总时间长度
Innodb_row_lock_time_avg：每次等待所花平均时间
Innodb_row_lock_time——max：从系统启动到现在等待最长的一次所花费的时间
Innodb_row_lock_wait：系统启动后到现在总共等待的次数

对于这5个状态变量，比较重要的主要是：
Innodb_row_lock_time_avg（等待平均时长），
Innodb_row_lock_waits（等待总次数），
Innodb_row_lock_time（等待总时长）这三项。

尤其是当等待次数很高，而且等待时长也不小的时候，我们就需要分析系统中为什么会有如此多的等待，然后根据分析结果着手制定优化计划。

### 2.2.4 优化建议

1. 尽可能让所有数据检索都通过索引来完成，避免无索引行锁升级为表锁
2. 合理设计索引，尽量缩小锁的范围
3. 尽可能较少检索条件，避免间隙锁
4. 计量控制事务大小，减少锁定资源量和时间长度
5. 尽可能低级别事务隔离

## 2.3 页锁

开销和加锁时间介于表锁和行锁之间；会出现死锁；锁定粒度介于表锁和行锁之间，并发度一般。

了解一下即可

以上说的三个锁的开销、加锁速度、死锁、粒度、并发性能只能就具体应用的特定特点来说那种锁更合适



# 3、锁补充

基于锁的属性分类：共享锁、排他锁

基于锁的粒度分类：行级锁（InnoDB）、表级锁（InnoDB、MyISAM）、页级锁（BDB引擎）、记录锁、间隙锁、临键锁

基于锁的状态分类：意向共享锁、意向排他锁

共享锁：（InnoDB中的共享行锁）
![锁001-共享锁](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\锁001-共享锁.png)

排他锁：（InnoDB引擎中的排他行锁）
![锁002-排他锁](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\锁002-排他锁.png)

行锁：（综合上面的共享锁和排他锁的内容）
![锁003-行锁](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\锁003-行锁.png)

表锁：（在InnoDB引擎中，只有通过索引条件检索数据时，InnoDB才使用行锁）
![锁004-表锁](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\锁004-表锁.png)

自增锁：
![锁005-自增锁](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\锁005-自增锁.png)

临键锁：
![锁006-临键锁](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\锁006-临键锁.png)

间隙锁：
![锁007-间隙锁](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\锁007-间隙锁.png)

记录锁：
![锁008-记录锁](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\锁008-记录锁.png)

悲观锁：![锁009-悲观锁](D:\Tyora\AssociatedPicturesInTheArticles\补3-MySQL锁机制-来自B站\锁009-悲观锁.png)



关于锁更多的内容可以去看《MySQL中的锁（表锁、行锁、共享锁、排他锁、间隙锁）.md》文章，是我转载的一篇不错的文章。

或者在简书中有一个人写的这方面的东西好像比较全面，但是我看不懂：他的个人主页——>https://www.jianshu.com/u/b12af9baadf4


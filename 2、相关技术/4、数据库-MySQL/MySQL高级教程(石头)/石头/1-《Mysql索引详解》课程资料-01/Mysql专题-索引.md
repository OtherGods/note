# 1.索引概述

MySQL官方对索引的定义为：

**索引（index**）是帮助MySQL高效获取数据的数据结构（有序）。在数据之外，数据库系统还维护着满足**特定查找算法的数据结构**，这些数据结构以某种方式<u>引用（指向）数据</u>，这样就可以在这些数据结构上实现高级查找算法，这种数据结构就是索引。

如下面的示意图所示：
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216223633292.png)

左边是数据表，一共有两列七条记录，最左边的是数据记录的物理地址（注意逻辑上相邻的记录在磁盘上也并不是一定物理相邻的）。为了加快Col2的查找，可以维护一个右边所示的二叉查找树，每个节点分别包含**索引键值**和一个指向对应数据记录物理地址的指针，这样就可以运用二叉查找快速获取到相应数据。

一般来说索引本身也很大，不可能全部存储在内存中，因此索引往往以索引文件的形式存储在磁盘上。索引是数据库中用来提高性能的最常用的工具。

# 2.索引优势劣势
## 2.1.优势
1）类似于书籍的目录索引，提高数据检索的效率，降低数据库的IO成本。
2）通过索引列对数据进行排序，**降低数据排序的成本，降低CPU的消耗**

## 2.2.劣势

1）实际上索引也是一张表，该表中保存了主键与索引字段，并指向实体类的记录，所以索引列也是要占用空间的。

> 以空间换时间： 索引、ES、Redis、页面静态化

2）虽然索引**大大提高了查询效率，同时却也降低更新表的速度，如对表进行INSERT、UPDATE、DELETE**。因为更新表时，MySQL 不仅要保存数据，还要保存一下索引文件每次更新添加了索引列的字段，都会调整因为更新所带来的键值变化后的索引信息。

> CRUD     CREATE     READ(使用最频繁)    UPDATE    DELETE

# 3.常见索引

<font color="red"><u>*索引是在MySQL的**存储引擎层**中实现的，而不是在服务器层实现的。*</u></font>所以每种**存储引擎**的索引都不一定完全相同，也不是所有的存储引擎都支持所有的索引类型的。MySQL目前提供了以下4种索引：

- **BTREE 索引**：**最常见的索引类型，大部分索引都支持 B 树索引**。
- **HASH 索引**：只有Memory引擎支持，使用场景简单。
- **R-tree 索引**（空间索引）：空间索引是MyISAM引擎的一个特殊索引类型，主要用于地理空间数据类型，通常使用较少，不做特别介绍。
- **Full-text** （全文索引）：全文索引也是MyISAM的一个特殊索引类型，主要用于全文索引，InnoDB从Mysql5.6版本开始支持全文索引。

![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216224141789.png)

我们平常所说的索引，如果没有特别指明，都是指B+树（多路搜索树，并不一定是二叉的）结构组织的索引。其中聚集索引、复合索引、前缀索引、唯一索引默认都是使用 **B+tree 索引**，统称为索引。

# 3.索引结构

## 3.1.BTree
BTree（B-Tree）又叫**多路平衡搜索树**，一颗m叉的BTree特性如下：

- 树中每个节点最多包含m个孩子。
- 除根节点与叶子节点外，每个节点至少有[**ceil**(m/2)]个孩子【向上取整】。
- 若根节点不是叶子节点，则至少有两个孩子。
- 所有的叶子节点都在同一层。
- 每个非叶子节点由**n个key与n+1个指针**组成，其中`[ceil(m/2)-1] <= n <= m-1`

**以5叉BTree为例**，key的数量：公式推导`[ceil(m/2)-1] <= n <= m-1`；所以 `2 <= n <=4` ；**当n>4时，中间节点分裂到父节点**，两边节点分裂。插入 C N G A H E K Q M F W L T Z D P R X Y S 数据为例。

演变过程如下：
1). 插入前4个字母 C N G A
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216224432272.png)
2). 插入H，n>4，中间元素G字母向上分裂到新的节点
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216224447922.png)
3). 插入E，K，Q不需要分裂
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216224508906.png)

4). 插入M，中间元素M字母向上分裂到父节点G
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216224524701.png)
5). 插入F，W，L，T不需要分裂
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216224552882.png)
6). 插入Z，中间元素T向上分裂到父节点中
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216224618504.png)
7). 插入D，中间元素D向上分裂到父节点中。然后插入P，R，X，Y不需要分裂
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216224651954.png)
8). 最后插入S，N， P， Q， R节点n>5，中间节点Q向上分裂，但分裂后父节点DGMT的n>5，中间节点M向上分裂
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216224711724.png)
到此，该BTREE树就已经构建完成了， BTREE树和二叉树相比，查询数据的效率更高，因为对于相同的数据量来说，BTREE的层级结构比二叉树小，因此搜索速度快！

## 3.2.B+Tree
B+Tree是**B树的变种**，有着比B树更高的查询性能，来看下m阶B+Tree特征：

1、有m个子树的节点包含有m个元素（B-Tree中是m-1）

2、**根节点和分支节点中不保存数据**，**只用于索引，所有数据都保存在叶子节点中**。

3、所有分支节点和根节点都同时存在于子节点中，在子节点元素中是最大或者最小的元素。

4、叶子节点会包含所有的关键字，以及指向数据记录的指针，并且叶子节点本身是根据关键字的大小从小到大顺序链接。
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216225837921.png)

```java
1、红点表示是指向卫星数据的指针，指针指向的是存放实际数据的磁盘页，卫星数据就是数据库中一条数据记录。

2、叶子节点中还有一个指向下一个叶子节点的next指针，所以叶子节点形成了一个有序的链表，方便遍历B+树。
```

### 3.2.1.B+树的优势

**1、更加高效的单元素查找**
B+树的查找元素**3**的过程：

**第一次磁盘IO**
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216230021926.png)
**第二次磁盘IO**
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216230042993.png)
**第三次磁盘IO**
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216230102663.png)
这个过程看下来，貌似与B树的查询过程没有什么区别。但实际上有两点不一样：

a、首先B+树的中间节点不存储卫星数据，所以同样大小的磁盘页可以容纳更多的节点元素，如此一来，相同数量的数据下，B+树就相对来说要更加**矮胖**些，磁盘IO的次数更少。

b、由于只有叶子节点才保存卫星数据，B+树每次查询都要到叶子节点；而B树每次查询则不一样，最好的情况是根节点，最坏的情况是叶子节点，没有B+树稳定。

**2、叶子节点形成有顺链表，范围查找性能更优**
**B树范围查找3-8的过程**

a、先查找3

![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\2020121623023778.png)
b、再查找4、5、6、7、8，中间过程省略，直接到8的查找
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\2020121623025122.png)
这里查找的范围跨度越大，则磁盘IO的次数越多，性能越差。

**B+树范围查找3-11的过程**
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216230316372.png)
先从上到下找到下限元素3，然后通过链表指针，依次遍历得到元素5/6/8/9/11；如此一来，就不用像B树那样一个个元素进行查找。

### 3.2.2.小结
1.单节点可以存储更多的元素，使得查询磁盘IO次数更少。

2.所有查询都要查找到叶子节点，查询性能稳定。

3.所有叶子节点形成有序链表，便于范围查询。


## 3.3.MySQL中的B+Tree
MySql索引数据结构对经典的B+Tree进行了优化。在原B+Tree的基础上，增加一个指向相邻叶子节点的链表指针，就形成了带有**顺序指针的B+Tree，提高区间访问的性能**。

MySQL中的 B+Tree 索引结构示意图:
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\20201216230659908.png)

# 4.索引分类

> 摘自：《MySQL 技术内幕 InnoDB 存储引擎 第 2 版》第四章
>
> 1. 索引组织表
>
>    在InnoDB存储引擎中，表是根据主键顺序组织存放的，这种存储方的表称之为**<u>*索引组织表*</u>**。在InnoDB存储引擎表中，每张表都有个主键，如果在创建表的时候没有显示的定义主键，则InnoDB存储引擎会按照如下方式选择或者创建主键：
>
>    1. 首先判断表中是否有非空的唯一索引（Unique NOT NULL），如果有，则该列即为主键
>    2. 如果不符合上述条件，InnoDB存储引擎自动创建一个6字节大小的指针。
>
>    当表中有多个非空唯一索引的时候，InnoDB存储引擎将选择创建表时第一个定义的非空唯一索引为主键。【需要注意：主键的选择根据的是定义索引的顺序，而不是建表时的列的顺序】。
>
> 2. InnoDB逻辑存储结构
>
>    从InnoDB存储引擎的逻辑存储结构来看，所有的数据都被逻辑的存放在一个空间中，称之为表空间。表又由段（segment）、区（extent）、页（page）组成。页在一些文档中有时候被称为块，InnoDB的存储引擎的逻辑存储结构大致如下图：
>    ![image-20220830103110012](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\image-20220830103110012.png)
>
>    

> 摘自：《MySQL 技术内幕 InnoDB 存储引擎 第 2 版》第五章
>
> 第一节：InnoDB存储引擎概述
> InnoDB存储引擎支持以下几种常见的索引：
>
> 1. B+树索引
> 2. 全文索引
> 3. 哈希索引
>
> B+树索引是传统意义上的索引，这是目前关系型数据库中查找最常用和最有效的索引。B+树索引的构造类似于二叉树，根据键值快速找到数据。
>
> B+树索引并不能找到一个给定键值的具体行。B+树索引找到的是被查找数据行所在的页，然后数据库通过把页读入内存中，再在内存中进行查找，最后得到想要查找的数据。
>
> 第三节：B+树
> 看书吧
>
> 
>
> 第四节：B+树索引
> 数据库中的B+树索引可以分为聚集索引和辅助索引（非聚集索引），但是不管是聚集索引还是辅助索引，其内部都是B+树做因，即高度平衡的，叶子节点所有的数据。聚集索引和辅助索引的不同是：叶子节点存放的是否是一整行的信息。
>
> - 聚集索引
>
>   - 之前介绍过，InnoDB存储引擎是索引组织表，即表中的数据按照主键顺序存储。而聚集索引就是按照每张表的主键构造一颗B+树，同时叶子节点中存放的即为整张表中的行记录数据，也将聚集索引的叶子节点称为数据页。聚集索引的这个特性决定了索引组织表中数据也是索引的一部分。同B+树数据结构一样，每个数据页都通过一个双向链表来进行链接。
>   - 由于实际的数据页只能按照一棵B+树进行排序，因此每张表中只能拥有一个聚集索引。
>   - 许多数据库的文档会告诉读者：聚集索引按照顺序物理的存储数据。但是如果聚集索引必须按照特定的顺序存放物理记录，则维护成本会显得非常高。所以，聚集索引的存储并不是物理上连续的，而是逻辑上连续的，这其中有两点：
>     1. 页通过双向链表链接，页按照主键的顺序排序
>     2. 每个页中的记录也是通过双向链表进行维护的，物理上存储可以同样不按照主键存储
>
> - 辅助索引（非聚集索引）
>
>   - 对于辅助索引，叶子节点并不包含行记录的全部数据。叶子节点处理包含键值以外，每个叶子节点中的索引行中还包含了一个书签。该书签用来告诉InnDB存储引擎哪里可以找到与索引相对应的行数据。由于InnoDB是索引组织表，因此InnoDB存储引擎的辅助索引的书签就是相应行数据的聚集索引键。
>
> - B+树索引的管理
>
>   - 索引的创建和删除可以通过两种方法：【下面这两张照片的内容比较全，但是其中有的结构用不到，实际使用看示例】
>
>     - 使用ALTER关键字创建和删除索引
>
>       ![image-20220830101257733](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\image-20220830101257733.png)
>   
> - 使用CREATE和DROP关键字创建和删除索引
>       ![image-20220830101332434](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\image-20220830101332434.png)
>   
>
>   
>
>
> 索引创建和删除的实际使用：（网上找的）
> 链接：https://www.cnblogs.com/yingyue23564/p/16638779.html
>
> 创建索引
>
> > 方式一：
> >
> > - ```sql
> >   ALTER TABLE 表名 ADD 索引类型 （unique,primary key,fulltext,index）[索引名]（字段名）
> >   ```
> >
> >   ```sql
> >   //普通索引
> >   alter table table_name add index index_name (column_list) ;
> >   //唯一索引
> >   alter table table_name add unique (column_list) ;
> >   //主键索引
> >   alter table table_name add primary key (column_list) ;
> >   ```
> >
> >   ALTER TABLE可用于创建普通索引、UNIQUE索引和PRIMARY KEY索引3种索引格式，**table_name**是要增加索引的表名，**column_list**指出对哪些列进行索引，多列时各列之间用逗号分隔。索引名index_name**可选**，缺省时，MySQL将根据第一个索引列赋一个名称。另外，ALTER TABLE允许在单个语句中更改多个表，因此可以同时创建多个索引。
> >
> > 
> >
> > 方式二：
> >
> > - ```sql
> >   CREATE INDEX index_name ON table_name(username(length)); 
> >   ```
> >
> >   如果是CHAR，VARCHAR类型，length可以小于字段实际长度；如果是BLOB和TEXT类型，必须指定 length。
> >
> >   ```sql
> >   //create只能添加这两种索引;
> >   CREATE INDEX index_name ON table_name (column_list);
> >   CREATE UNIQUE INDEX index_name ON table_name (column_list);
> >   ```
> >
> >   table_name、index_name和column_list具有与ALTER TABLE语句中相同的含义，**索引名可不选**。另外，**不能用CREATE INDEX语句创建PRIMARY KEY索引**。
> >
>
> 
>
> 索引删除：CREATE / DROP INDEX
>
> > 方式一 + 方式二：
> >
> > -    删除索引可以使用ALTER TABLE或DROP INDEX语句来实现。DROP INDEX可以在ALTER TABLE内部作为一条语句处理，其格式如下：
> >
> >   ```sql
> >   //方式二
> >   drop index index_name on table_name ;
> >   
> >   //方式一
> >   alter table table_name drop index index_name ;
> >   
> >   ```
> >
> > alter table table_name drop primary key ;
> >   ```
> > 
> >   　　其中，在前面的两条语句中，都删除了table_name中的索引index_name。而在最后一条语句中，只在删除PRIMARY KEY索引中使用，因为**一个表只可能有一个PRIMARY KEY索引**，因此不需要指定索引名。如果没有创建PRIMARY KEY索引，但表具有一个或多个UNIQUE索引，则MySQL将删除第一个UNIQUE索引。
> >   
> >      如果从表中删除某列，则索引会受影响。对于多列组合的索引，如果删除其中的某列，则该列也会从索引中删除。如果删除组成索引的所有列，则整个索引将被删除。
> >   ```

**聚集索引**

1）主键索引

https://www.cnblogs.com/starcrm/p/12971702.html



**非聚集索引   ：**

1）单值索引：即一个索引只包含单个列，一个表可以有多个单列索引
2）唯一索引：索引列的值必须唯一，但允许有空值
3）复合索引：即一个索引包含多个列 
4）全文索引

# 5.索引实践

本篇文章，我们将从索引基础开始，介绍什么是索引以及索引的几种类型，然后学习如何创建索引以及索引设计的基本原则。
本篇文章中用于测试索引创建的user表的结构如下：

```sql
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `gender` int(1) NOT NULL,
  `age` int(3) NOT NULL,
  `status` int(1) NOT NULL,
  `remark` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;
```

## 5.1索引类型

查看索引详情

```
SHOW INDEX FROM table_name;
```
例如：

![img](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-索引\7095269-4f5cf1a934af7428.png)

该表中各个列的含义：

内容来源：https://www.jianshu.com/p/17a34668904a

- Table：表名
- Non_unique：如果索引不能包括重复的值，则为0；如果可以，则为1；也就是平时说的唯一索引
- Key_name：索引名称，如果名字相同表明是同一个索引，而并不是重复，例如上图中第四、五条数据，索引名字都是name，其实是一个联合索引
- Seq_in_index：索引中的列序列化，从1开始.上图中的第四、五条数据，Seq_in_index一个是1一个是2，就是表明在联合索引中的顺序，我们就能推断出联合索引的前后顺序
- Cloumn_name：索引的列名
- Collation：指的是该列在索引中的排序方式，A就是ASC（顺序），D就是DESC（逆序）。
- Cardinality：是我基数的意思，表示索引中唯一值的数目的估计值。我们知道某个字段的重复值越少越合适建立索引，所以我们一般都是根据Cardinality来判断索引是否具有高选择性，如果这个值非常小，那就需要重新评估这个字段是否适合建立索引
- Sub_part：前置索引的意思，入宫列只是被部分的编入索引，则为编入所以你的字符的数目
- Packed：只是关键字如何被压缩。如果美哟被压缩，则为Null。压缩一般包括传输协议、压缩列解决方案和压缩表解决方案
- Null：如果列含有NULL，则含有YES
- Index_type：表示所以类型，Mysql目前主要有以下几种索引：FULLTEXT,HASH,BTREE,RTREE。

## 5.2.主键索引

1）单值索引：即一个索引只包含单个列，一个表可以有多个单列索引
2）唯一索引：索引列的值必须唯一，但允许有空值
3）复合索引：即一个索引包含多个列

它是一种特殊的唯一索引，不允许有空值。一般是在建表的时候同时创建主键索引。主键索引就是之前的主键约束！

**Primary key**

```sql
mysql> SHOW INDEX FROM user;
+-------+------------+----------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
| Table | Non_unique | Key_name | Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment |
+-------+------------+----------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
| user  |          0 | PRIMARY  |            1 | id          | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
+-------+------------+----------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
1 row in set (0.03 sec)

```
>注意：一个表只能有一个主键
## 4.2.唯一索引
唯一索引列的值必须唯一，但允许有空值。如果是组合索引，则列值的组合必须唯一。
**创建唯一索引:**

```
ALTER TABLE table_name ADD UNIQUE (column);
```
示例：

```sql
mysql> alter table user add unique(name);
Query OK, 0 rows affected (0.01 sec)
Records: 0  Duplicates: 0  Warnings: 0
mysql> SHOW INDEX FROM user;
+-------+------------+----------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
| Table | Non_unique | Key_name | Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment |
+-------+------------+----------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
| user  |          0 | PRIMARY  |            1 | id          | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          0 | name     |            1 | name        | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
+-------+------------+----------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
2 rows in set (0.03 sec)
```
**创建唯一组合索引：**

```
ALTER TABLE table_name ADD UNIQUE (column1,column2);
```
**示例：**

```sql
mysql> ALTER TABLE user ADD UNIQUE unique_name_age (name,age);
Query OK, 0 rows affected (0.01 sec)
Records: 0  Duplicates: 0  Warnings: 0
mysql> SHOW INDEX FROM user;
+-------+------------+-----------------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
| Table | Non_unique | Key_name        | Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment |
+-------+------------+-----------------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
| user  |          0 | PRIMARY         |            1 | id          | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          0 | name            |            1 | name        | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          0 | unique_name_age |            1 | name        | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          0 | unique_name_age |            2 | age         | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
+-------+------------+-----------------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
4 rows in set (0.03 sec)
```
## 5.3.普通索引

最基本的索引，它没有任何限制，只是起到加快查询速度的作用。

**创建普通索引：**

```
ALTER TABLE table_name ADD INDEX index_name (column);
```
**示例：**

```sql
mysql> alter table user add index index_name(name);
Query OK, 0 rows affected (0.01 sec)
Records: 0  Duplicates: 0  Warnings: 0
mysql> SHOW INDEX FROM user;
+-------+------------+-----------------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
| Table | Non_unique | Key_name        | Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment |
+-------+------------+-----------------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
| user  |          0 | PRIMARY         |            1 | id          | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          0 | name            |            1 | name        | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          0 | unique_name_age |            1 | name        | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          0 | unique_name_age |            2 | age         | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          1 | index_name      |            1 | name        | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
+-------+------------+-----------------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
5 rows in set (0.03 sec)
```
## 5.4.组合索引

组合索引，即一个索引包含多个列。多用于避免回表查询。
**创建组合索引：**

```
ALTER TABLE table_name ADD INDEX index_name(column1, column2, column3);
```
示例：

```sql
mysql> alter table user add index index_name_age(name,age);
Query OK, 0 rows affected (0.01 sec)
Records: 0  Duplicates: 0  Warnings: 0
mysql> SHOW INDEX FROM user;
+-------+------------+-----------------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
| Table | Non_unique | Key_name        | Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment |
+-------+------------+-----------------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
| user  |          0 | PRIMARY         |            1 | id          | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          0 | name            |            1 | name        | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          0 | unique_name_age |            1 | name        | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          0 | unique_name_age |            2 | age         | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          1 | index_name      |            1 | name        | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          1 | index_name_age  |            1 | name        | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          1 | index_name_age  |            2 | age         | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
+-------+------------+-----------------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
7 rows in set (0.05 sec)
```
> 组合索引补充：（来自：https://www.cnblogs.com/yingyue23564/p/16638779.html）
> 组合索引和前缀索引：
>
> 在这里要指出，组合索引和前缀索引是对建立索引技巧的一种称呼，并不是索引的类型。为了更好的表述清楚，建立一个demo表如下：
>
> ```sql
> create table USER_DEMO
> (
>     ID                   int not null auto_increment comment '主键',
>     LOGIN_NAME           varchar(100) not null comment '登录名',
>     PASSWORD             varchar(100) not null comment '密码',
>     CITY                 varchar(30) not null comment '城市',
>     AGE                  int not null comment '年龄',
>     SEX                  int not null comment '性别(0:女 1：男)',
>     primary key (ID)
> );
> ```
>
> 为了进一步榨取mysql的效率，就可以考虑建立组合索引，即将LOGIN_NAME,CITY,AGE建到一个索引里：
>
> ```sql
> ALTER TABLE USER_DEMO ADD INDEX name_city_age (LOGIN_NAME(16),CITY,AGE); 
> ```
>
> 建表时，LOGIN_NAME长度为100，这里用16，是因为一般情况下名字的长度不会超过16，这样会加快索引查询速度，还会减少索引文件的大小，提高INSERT，UPDATE的更新速度。
>
> ​    如果分别给LOGIN_NAME,CITY,AGE建立单列索引，让该表有3个单列索引，查询时和组合索引的效率是大不一样的，甚至远远低于我们的组合索引。虽然此时有三个索引，但mysql只能用到其中的那个它认为似乎是最有效率的单列索引，另外两个是用不到的，也就是说还是一个全表扫描的过程。
>
> ​    建立这样的组合索引，就相当于分别建立如下三种组合索引：
>
> ```sql
> LOGIN_NAME,CITY,AGE
> LOGIN_NAME,CITY
> LOGIN_NAME
> ```
>
> 为什么没有CITY,AGE等这样的组合索引呢？这是因为mysql组合索引“最左前缀”的结果。简单的理解就是只从最左边的开始组合，并不是只要包含这三列的查询都会用到该组合索引。也就是说：**name_city_age(LOGIN_NAME(16),CITY,AGE)从左到右进行索引，如果没有左前索引，mysql不会执行索引查询**。
>
> 
>
>  如果索引列长度过长,这种列索引时将会产生很大的索引文件,不便于操作,可以使用前缀索引方式进行索引，前缀索引应该控制在一个合适的点,控制在0.31黄金值即可(大于这个值就可以创建)。
>
> ```sql
> SELECT COUNT(DISTINCT(LEFT(`title`,10)))/COUNT(*) FROM Arctic; -- 这个值大于0.31就可以创建前缀索引,Distinct去重复
> 
> ALTER TABLE `user` ADD INDEX `uname`(title(10)); -- 增加前缀索引SQL,将人名的索引建立在10,这样可以减少索引文件大小,加快索引查询速度
> ```
>
> 





## 5.5.全文索引

全文索引（也称全文检索）是目前搜索引擎使用的一种关键技术。
**创建全文索引**

```
ALTER TABLE table_name ADD FULLTEXT (column);
```
**示例：**

```sql
mysql> ALTER TABLE user ADD FULLTEXT (remark);
Database changed
Records: 0  Duplicates: 0  Warnings: 0
mysql> SHOW INDEX FROM user;
+-------+------------+----------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
| Table | Non_unique | Key_name | Seq_in_index | Column_name | Collation | Cardinality | Sub_part | Packed | Null | Index_type | Comment | Index_comment |
+-------+------------+----------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
| user  |          0 | PRIMARY  |            1 | id          | A         |           0 | NULL     | NULL   |      | BTREE      |         |               |
| user  |          1 | remark   |            1 | remark      | NULL      | NULL        | NULL     | NULL   | YES  | FULLTEXT   |         |               |
+-------+------------+----------+--------------+-------------+-----------+-------------+----------+--------+------+------------+---------+---------------+
2 rows in set
```
>备注:  如果是InnoDB，改为MyISAM，InnoDB老版本不支持FULLTEXT类型的索引
## 5.6.删除索引

索引一经创建不能修改，如果要修改索引，只能删除重建。
删除索引：

```
DROP INDEX index_name ON table_name;
```

# 7.索引设计原则

>索引的设计可以遵循一些已有的原则，创建索引的时候请尽量考虑符合这些原则，便于提升索引的使用效率，更高效的使用索引。

需要创建索引的情况：

- 对查询频次较高，且数据量比较大的表建立索引。
- 频繁更新的字段不适合建立索引
- 查询中与其他表关联的字段，外键关系建立索引
- 单/组合索引的选择？在高并发下倾向于创建组合索引
- 查询中排序的字段，排序字段若通过索引去访问将大大提高排序的速度
- 索引字段的选择，最佳候选列应当从where子句的条件中提取，如果where子句中的组合比较多，那么应当挑选最常用、过滤效果最好的列的组合；where条件中用不到的字段不创建索引
- 使用唯一索引，区分度越高，使用索引的效率越高。**电话号码、邮箱**
- 索引可以有效的提升查询数据的效率，但索引数量不是多多益善，索引越多，维护索引的代价自然也就水涨船高。对于插入、更新、删除等DML操作比较频繁的表来说，索引过多，会引入相当高的维护代价，降低DML操作的效率，增加相应操作的时间消耗。另外索引过多的话，MySQL也会犯选择困难病，虽然最终仍然会找到一个可用的索引，但无疑提高了选择的代价。
- 使用短索引，索引创建之后也是使用硬盘来存储的，因此提升索引访问的I/O效率，也可以提升总体的访问效率。假如构成索引的字段总长度比较短，那么在给定大小的存储块内可以存储更多的索引值，相应的可以有效的提升MySQL访问索引的I/O效率。
- 利用最左前缀，N个列组合而成的组合索引，那么相当于是创建了N个索引，如果查询时where子句中使用了组成该索引的前几个字段，那么这条查询SQL可以利用组合索引来提升查询效率。

不需要创建索引的情况：

- 表记录太少
- 经常增删改的表
- 数据重复且分布平均的表字段，因此应该只为经常查询和经常排序的数据列建立索引。
  注意，如果某个数据列包含许多重复的内容，为它建立索引就没有太大的实际效果。

> 具体的SQL语句，添加合适的索引！

# 8.优化分类

优化分为：sql层面的优化和架构层面的优化

sql层面的优化可以去看文件夹《6-《SQL分析工具》课程资料-01》中的内容；

> B站尚硅谷视频总结
>
> SQL层面优化的主要步骤有：
> 1、慢查询的开启并捕获
> 2、explain+慢SQL分析  查询到的慢SQL
> 3、show profile查询SQL在Mysql服务器里面的执行细节和生命周期的情况
> 4、SQL数据库服务器的参数调优（运维、DBA做）



架构层面的优化可以去看文件夹《2-《Mysql存储引擎》课程资料-01》、《3-《Mysql主从复制》课程资料-01》、《4-《Mysql读写分离》课程资料-01》、《5-《Mysql分库分表》课程资料-01》中的内容。
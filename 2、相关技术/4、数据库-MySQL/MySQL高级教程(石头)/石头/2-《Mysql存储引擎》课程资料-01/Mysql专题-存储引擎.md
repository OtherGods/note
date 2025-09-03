# 1.Mysql的体系结构概览

![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-存储引擎\20201217224217128.png)

整个MySQL Server由以下组成 ：
- **Connection Pool** ：连接池组件
- **Management Services & Utilities** ：管理服务和工具组件
- **SQL Interface** ：SQL接口组件
- **Parser** ：查询分析器组件
- **Optimizer** ：优化器组件
- **Caches & Buffers** ：缓冲池组件
- **Pluggable Storage Engines** ：可插拔 存储引擎，可定制
- **File System** ：文件系统

> B站中尚硅谷对MySQL存储引擎的介绍：
> 1.Connectors
> 指的是不同语言中与SQL的交互
> 2 Management Serveices & Utilities： 
> 系统管理和控制工具
> 3 Connection Pool: 连接池
> 管理缓冲用户连接，线程处理等需要缓存的需求。
> 负责监听对 MySQL Server 的各种请求，接收连接请求，转发所有连接请求到线程管理模块。每一个连接上 MySQL Server 的客户端请求都会被分配（或创建）一个连接线程为其单独服务。而连接线程的主要工作就是负责 MySQL Server 与客户端的通信，
> 接受客户端的命令请求，传递 Server 端的结果信息等。线程管理模块则负责管理维护这些连接线程。包括线程的创建，线程的 cache 等。
> 4 SQL Interface: SQL接口。
> 接受用户的SQL命令，并且返回用户需要查询的结果。比如select from就是调用SQL Interface
> 5 Parser: 解析器。
> SQL命令传递到解析器的时候会被解析器验证和解析。解析器是由Lex和YACC实现的，是一个很长的脚本。
> 在 MySQL中我们习惯将所有 Client 端发送给 Server 端的命令都称为 query ，在 MySQL Server 里面，连接线程接收到客户端的一个 Query 后，会直接将该 query 传递给专门负责将各种 Query 进行分类然后转发给各个对应的处理模块。
> 主要功能：
> a . 将SQL语句进行语义和语法的分析，分解成数据结构，然后按照不同的操作类型进行分类，然后做出针对性的转发到后续步骤，以后SQL语句的传递和处理就是基于这个结构的。
> b.  如果在分解构成中遇到错误，那么就说明这个sql语句是不合理的
> 6 Optimizer: 查询优化器。
> SQL语句在查询之前会使用查询优化器对查询进行优化。就是优化客户端请求的 query（sql语句） ，根据客户端请求的 query 语句，和数据库中的一些统计信息，在一系列算法的基础上进行分析，得出一个最优的策略，告诉后面的程序如何取得这个 query 语句的结果
> 他使用的是“选取-投影-联接”策略进行查询。
>        用一个例子就可以理解： select uid,name from user where gender = 1;
>        这个select 查询先根据where 语句进行选取，而不是先将表全部查询出来以后再进行gender过滤
>        这个select查询先根据uid和name进行属性投影，而不是将属性全部取出以后再进行过滤
>        将这两个查询条件联接起来生成最终查询结果
> 7 Cache和Buffer： 查询缓存。
> 他的主要功能是将客户端提交 给MySQL 的 Select 类 query 请求的返回结果集 cache 到内存中，与该 query 的一个 hash 值 做一个对应。该 Query 所取数据的基表发生任何数据的变化之后， MySQL 会自动使该 query 的Cache 失效。在读写比例非常高的应用系统中， Query Cache 对性能的提高是非常显著的。当然它对内存的消耗也是非常大的。
> 如果查询缓存有命中的查询结果，查询语句就可以直接去查询缓存中取数据。这个缓存机制是由一系列小缓存组成的。比如表缓存，记录缓存，key缓存，权限缓存等
> 8 、存储引擎接口
> 存储引擎接口模块可以说是 MySQL 数据库中最有特色的一点了。目前各种数据库产品中，基本上只有 MySQL 可以实现其底层数据存储引擎的插件式管理。这个模块实际上只是 一个抽象类，但正是因为它成功地将各种数据处理高度抽象化，才成就了今天 MySQL 可插拔存储引擎的特色。
>      从图2还可以看出，MySQL区别于其他数据库的最重要的特点就是其插件式的表存储引擎。MySQL插件式的存储引擎架构提供了一系列标准的管理和服务支持，这些标准与存储引擎本身无关，可能是每个数据库系统本身都必需的，如SQL分析器和优化器等，而存储引擎是底层物理结构的实现，每个存储引擎开发者都可以按照自己的意愿来进行开发。
>     注意：存储引擎是基于表的，而不是数据库。

## 1.1.连接层

最上层是一些客户端和链接服务，包含本地**sock通信**和大多数基于客户端/服务端工具实现的类似于TCP/IP的通信。主要完成一些类似于**连接处理、授权认证、及相关的安全方案**。在该层上引入了线程池的概念，为通过认证安全接入的客户端提供线程。同样在该层上可以实现基于SSL的安全链接。服务器也会为安全接入的每个客户端验证它所具有的操作权限。

## 1.2.服务层
第二层架构主要完成大多数的核心服务功能，如SQL接口，并完成缓存的查询，SQL的分析和优化，部分内置函数的执行。所有跨存储引擎的功能也在这一层实现，如过程、函数等。在该层，服务器会解析查询并创建相应的内部解析树，并对其完成相应的优化如确定表的查询的顺序，是否利用索引等，最后生成相应的执行操作。如果是select语句，服务器还会查询内部的缓存，如果缓存空间足够大，这样在解决大量读操作的环境中能够很好的提升系统的性能。

**Management Serveices & Utilities：**

```java
系统管理和控制工具  
```
**SQL Interface: SQL接口**

```java
接受用户的SQL命令，并且返回用户需要查询的结果。比如select from就是调用SQL Interface
```
**Parser: 解析器**

```java
SQL命令传递到解析器的时候会被解析器验证和解析。 
```
**Optimizer: 查询优化器**

```java
SQL语句在查询之前会使用查询优化器对查询进行优化。 
用一个例子就可以理解： select uid,name from user where  gender= 1;
优化器来决定先投影还是先过滤。
```
**Cache和Buffer： 查询缓存**

```java
如果查询缓存有命中的查询结果，查询语句就可以直接去查询缓存中取数据。
这个缓存机制是由一系列小缓存组成的。比如表缓存，记录缓存，key缓存，权限缓存等缓存是负责读，缓冲负责写。
```
## 1.3.引擎层
存储引擎层，存储引擎真正的负责了MySQL中数据的存储和提取，服务器通过API与存储引擎进行通信。不同的存储引擎具有的功能不同，这样我们可以根据自己的实际需要进行选取。后面介绍MyISAM和InnoDB

## 1.4.存储层

```java
数据存储层，主要是将数据存储在运行于裸设备的文件系统之上，并完成与存储引擎的交互。
```
## 1.5 *Mysql的执行大概流程*

客户端请求 ---> 连接器（验证用户身份，给予权限）  ---> 查询缓存（存在缓存则直接返回，不存在则执行后续操作） ---> 分析器（对SQL进行词法分析和语法分析操作）  ---> 优化器（主要对执行的sql优化选择最优的执行方案方法）  ---> 执行器（执行时会先看用户是否有执行权限，有才去使用这个引擎提供的接口） ---> 去引擎层获取数据返回（如果开启查询缓存则会缓存查询结果）

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201217224308942.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

和其他数据库相比，MySQL有点与众不同，它的架构可以在多种不同场景中应用并发挥良好作用。主要体现在**存储引擎**上，插件式的存储引擎架构，将查询处理和其他的系统任务以及数据的存储提取分离。这种架构可以根据业务的需求和实际需要选择合适的存储引擎。

> 如果数据库只读  用 MyIsAm  但是它不支持事务！
>
> 如果数据库增删改  用InnoDB  但是它支持事务！

# 2.Mysql的查询流程
我们总是希望MySQL能够获得更高的查询性能，最好的办法是弄清楚MySQL是如何优化和执行查询的。一旦理解了这一点，就会发现：很多的查询优化工作实际上就是**遵循一些原则让MySQL的优化器**能够按照预想的合理方式运行而已。

当向MySQL发送一个请求的时候，MySQL到底做了些什么呢？
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201217224144229.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)


mysql客户端通过协议与mysql服务器建连接,发送查询语句,先检查查询缓存,如果命中(一模一样的sql才能命中),直接返回结果,否则进行语句解析,也就是说,在解析查询之前,服务器会先访问查询缓存--它存储select语句以及相应的查询结果集,如果某个查询结果已经位于缓存中,服务器就不会再对查询进行解析优化执行。它仅仅将缓存中的结果返回给用户即可,这将大大提高系统的性能。

**语法解析器和预处理**:首先mysql通过关键字将SQL语句进行解析,并生成一颗对应的'解析树'。mysql解析器将使用mysql语法规则验证和解析查询;预处理器根据一些mysql规则进一步检查解析树是否合法。

**查询优化器**:当解析树被认为是合法的了,并且由优化器将其转化成执行计划。一条查询可以有很多种执行方式,最后都返回了相同的结果。优化器的作用就是找到最好的执行计划。


# 3. 存储引擎
## 3.1.存储引擎概述
和大多数的数据库不同，MySQL中有一个存储引擎的概念，针对不同的存储需求可以选择最优的存储引擎。**存储引擎就是存储数据，建立索引，更新查询数据等等技术的实现方式**。**存储引擎是基于表，而不是基于库的。所以存储引擎也可被称为表类型。**

Oracle、SqlServer等数据库只有一种存储引擎。MySQL提供插件式的存储引擎架构。所以MySQL存在多种存储引擎，可以根据需要使用相应的引擎，或者编写存储引擎。

MySQL5.0支持的存储引擎包含 ：**InooDB**、**MyISAM**、BDB、MEMORY、MERGE、EXAMPLE、NDB Cluster、ARCHIVE、CSV、BLACKHOLE、FEDERATED等，其中InnoDB和BDB提供事物安全表，其他存储引擎是非事物安全表。

可以通过指定**show engines**，来查询当前数据库支持的存储引擎 ：	
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201217223432891.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

创建新表时如果不指定存储引擎，那么系统就会使用默认的存储引擎，**MySQL5.5之前的默认存储引擎MyISAM**，5.5之后就改为了InnoDB。

查看MySQL数据库默认的存储引擎 ，指令 ：

```java
show variables like '%storage_engine%';
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201217223611362.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

## 3.2.各种存储引擎特性
下面重点介绍几种常用的存储引擎，并对比各个存储引擎之间的区别，如下表所示 ：
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-存储引擎\20201217223903976.png)

### 3.2.1 InnoDB和MyISAM引擎的区别？

得分点 

1. 事务、
2. 锁、
3. 读写性能、
4. 外键/全文索引 、
5. 聚簇索引和非聚簇索引、
6. 存储结构（关于存储结构可以去看3.3和3.4中说的文件存储结构小节中介绍的内容）

标准回答：InnoDB是具有事务、回滚和崩溃修复能力的事务安全型引擎,它可以实现行级锁来保证高性能的大量数据中的并发操作；MyISAM是具有默认支持全文索引、压缩功能及较高查询性能的非事务性引擎。默认推荐：InnoDB是MySQL5.5之后的默认引擎。



具体来说,可以在以下角度上形成对比： 

1. 事务：InnoDB支持事务；MyISAM不支持。 

   1. InnoDB支持回滚，MyISAM不支持
   2. 崩溃恢复：InnoDB有崩溃恢复机制；MyISAM没有。 

2. 数据锁：InnoDB支持行级锁；MyISAM只支持表级锁。 

   1. InnoDB中行级锁是怎么实现的？ InnoDB行级锁是通过给索引上的索引项加锁来实现的。只有通过索引条件检索数据,InnoDB才使用行级锁,否则,InnoDB将使用表锁。 当表中锁定其中的某几行时,不同的事务可以使用不同的索引锁定不同的行。另外,不论使用主键索引、唯一索引还是普通索引,InnoDB都会使用行锁来对数据加锁。
   2. 具体的行锁的内容可以去看 《补9-面试中的老大难-mysql事务和锁，一次性讲清楚！.md》（因为这个文件所在的路径不能正确的在md文件中使用，所以只是写出了文件的名字）

3. 读写性能：InnoDB增删改性能更优；MyISAM查询性能更优。

4.  全文索引：InnoDB不支持（但可通过插件等方式支持）；MyISAM默认支持。 

   外键：InnoDB支持外键；MyISAM不支持。 

5. 存储结构：InnoDB在磁盘存储为两个文件（聚簇索引）或三个文件（非聚簇索引）；MyISAM在磁盘上存储成三个文件（表定义、数据、索引）。

6. InnoDB中有聚簇索引和非聚簇索引（可以通过存储结构体现出来），MyISAM中只有非聚簇索引（可以通过存储结构体现出来）

7. ~~存储空间：InnoDB需要更多的内存和存储；MyISAM支持支持三种不同的存储格式：静态表(默认)、动态表、压缩表。 移植：InnoDB在数据量小时可通过拷贝数据文件、备份 binlog、mysqldump工具移植,数据量大时比较麻烦；可单独对某个表通过拷贝表文件移植。~~ 



下面我们将重点介绍最长使用的两种存储引擎： InnoDB、MyISAM ，另外两种 MEMORY、MERGE ，了解即可。



## 3.3. InnoDB
InnoDB存储引擎是Mysql的默认存储引擎。InnoDB存储引擎提供了具有提交、回滚、崩溃恢复能力的事务安全。但是对比MyISAM的存储引擎，InnoDB写的处理效率差一些，并且会占用更多的磁盘空间以保留数据和索引。

InnoDB存储引擎不同于其他存储引擎的特点
### 3.3.1.事务控制

```sql
CREATE TABLE goods_innodb (
	id INT NOT NULL AUTO_INCREMENT,
	NAME VARCHAR (20) NOT NULL,
	PRIMARY KEY (id)
) ENGINE = INNODB DEFAULT CHARSET = utf8;

CREATE TABLE goods_myisAm (
	id INT NOT NULL AUTO_INCREMENT,
	NAME VARCHAR (20) NOT NULL,
	PRIMARY KEY (id)
) ENGINE = MyISAm DEFAULT CHARSET = utf8;
```

```sql
START TRANSACTION;

INSERT INTO goods_innodb (id, NAME)
VALUES
	(NULL, 'Meta20');

COMMIT;

INSERT INTO goods_myisAm (id, NAME)
VALUES
	(NULL, 'Meta20');
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020121722535314.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
测试，发现在InnoDB中是存在事务的；

### 3.3.2.外键约束
MySQL支持外键的存储引擎只有InnoDB ，在创建外键的时候，要求父表必须有对应的索引，子表在创建外键的时候，也会自动的创建对应的索引。

下面两张表中， country_innodb是父表， country_id为主键索引，city_innodb表是子表，country_id字段为外键，对应于country_innodb表的主键country_id 

```sql
create table country_innodb (
	country_id int NOT NULL AUTO_INCREMENT,
	country_name VARCHAR (100) NOT NULL,
	PRIMARY KEY (country_id)
) ENGINE = INNODB DEFAULT CHARSET = utf8;

create table city_innodb (
	city_id int NOT NULL AUTO_INCREMENT,
	city_name VARCHAR (50) NOT NULL,
	country_id int NOT NULL,
	PRIMARY KEY (city_id),
	KEY idx_fk_country_id (country_id),
	CONSTRAINT `fk_city_country` FOREIGN KEY(country_id) REFERENCES country_innodb(country_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = INNODB DEFAULT CHARSET = utf8;

insert into country_innodb
VALUES
	(NULL, 'China'),
	(NULL, 'America'),
	(NULL, 'Japan');

insert into city_innodb
VALUES
	(NULL, 'Xian', 1),
	(NULL, 'NewYork', 2),
	(NULL, 'BeiJing', 1);
```


在创建索引时，可以指定在删除、更新父表（主表）时，对子表（从表）进行的相应操作，包括 `RESTRICT`、`CASCADE`、`SET NULL`和 `NO ACTION`。
- RESTRICT和NO ACTION相同，是指**限制**在子表（从表）有关联记录的情况下，父表（主表）不能更新；
- CASCADE表示父表（主表）在更新或者删除时，更新或者删除子表（从表）对应的记录；
- SET NULL 则表示父表（主表）在更新或者删除的时候，子表（从表）的对应字段被SET NULL 。

针对上面创建的两个表，子表的外键指定是ON DELETE RESTRICT ON UPDATE CASCADE 方式的，那么在主表删除记录的时候，如果子表有对应记录，则不允许删除，主表在更新记录的时候，如果子表有对应记录，则子表对应更新。表中数据如下图所示:
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201217230501667.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
外键信息可以使用如下两种方式查看：

```sql
show create table city_innodb ;
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201217230558255.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
删除country_id为1 的country数据：

```sql
delete from country_innodb where country_id = 1;
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201217230640485.png)
更新主表country表的字段 country_id :

```sql
update country_innodb set country_id = 100 where country_id = 1;
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201217230717509.png)
更新后，子表的数据信息为：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201217230730313.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
### 3.3.3.存储方式
InnoDB 存储表和索引有以下两种方式：

①. 使用共享表空间存储，这种方式创建的表的表结构保存在`.frm`文件中，数据和索引保存在`innodb_data_home_dir` 和 `innodb_data_file_path`定义的表空间中，可以是多个文件。

②. 使用多表空间存储，这种方式创建的表的表结构仍然存在 .frm 文件中，但是每个表的数据和索引单独保存在.ibd 中。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201217231108932.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
## 3.4. MyISAM
MyISAM 不支持事务、也不支持外键，其优势是访问的速度快，对事务的完整性没有要求或者以SELECT、INSERT为主的应用基本上都可以使用这个引擎来创建表。

有以下两个比较重要的特点：

### 3.4.1.不支持事务

```sql
create table goods_myisam (
	id int NOT NULL AUTO_INCREMENT,
	NAME VARCHAR (20) NOT NULL,
	PRIMARY KEY (id)
) ENGINE = myisam DEFAULT CHARSET = utf8;
```

![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-存储引擎\20201217231632528.png)
通过测试，我们发现，在MyISAM存储引擎中，是没有事务控制的；

### 3.4.2.文件存储方式
每个MyISAM在磁盘上存储成3个文件，其文件名都和表名相同，但拓展名分别是：

```java
.frm (存储表定义)；
.MYD(MYData , 存储数据)；
.MYI(MYIndex , 存储索引)；
```
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-存储引擎\20201217231836231.png)
## 3.4.MEMORY
**Memory存储引擎将表的数据存放在内存中**。每个MEMORY表实际对应一个磁盘文件，格式是.frm ，**该文件中只存储表的结构，而其数据文件，都是存储在内存中**，这样有利于数据的快速处理，提高整个表的效率。MEMORY类型的表访问非常地快，因为他的数据是存放在内存中的，并且**默认使用HASH索引**，但是服务一旦关闭，表中的数据就会丢失.

## 3.5.MERGE

> git branch 创建分支
>
> git merge 合并分支
>
> 冲突！

MERGE存储引擎是一组MyISAM表的组合，这些MyISAM表必须结构完全相同，MERGE表本身并没有存储数据，对MERGE类型的表可以进行查询、更新、删除操作，这些操作实际上是对内部的MyISAM表进行的。

对于MERGE类型表的插入操作，是通过INSERT_METHOD子句定义插入的表，可以有3个不同的值，使用FIRST 或LAST 值使得插入操作被相应地作用在第一或者最后一个表上，不定义这个子句或者定义为NO，表示不能对这个MERGE表执行插入操作。

可以对MERGE表进行DROP操作，但是这个操作只是删除MERGE表的定义，对内部的表是没有任何影响的。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201217232122190.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
下面是一个创建和使用MERGE表的示例：

1）. 创建3个测试表 order_1990, order_1991, order_all , 其中order_all是前两个表的MERGE表：

```sql
create table order_1990(
	order_id int,
    order_money double(10,2),
    order_address varchar(50),
    primary key(order_id)
)engine = myisam default charset=utf8;

create table order_1991(
	order_id int,
    order_money double(10,2),
    order_address varchar(50),
    primary key(order_id)
)engine = myisam default charset=utf8;

create table order_all(
	order_id int,
    order_money double(10,2),
    order_address varchar(50),
    primary key(order_id)
)engine =merge union(order_1990,order_1991) INSERT_METHOD=LAST default charset=utf8;

```
2).分别向两张表中插入记录

```java
INSERT INTO order_1990 VALUES(1,100.0,'北京');
INSERT INTO order_1990 VALUES(2,100.0,'上海');

INSERT INTO order_1991 VALUES(10,200.0,'北京');
INSERT INTO order_1991 VALUES(11,200.0,'上海');
```
order_all结果：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020121723250752.png)

## 3.6. InnoDB、MyISAM存储引擎对比（面试这么回答）

- InnoDB 支持事务，MyISAM 不支持事务。这是 MySQL 将默认存储引擎从 MyISAM 变成 InnoDB 的重要原因之一；
- InnoDB 支持外键，而 MyISAM 不支持。对一个包含外键的 InnoDB 表转为 MYISAM 会失败；
- **InnoDB 是聚簇索引，MyISAM 是非聚簇索引**。聚簇索引的文件存放在主键索引的叶子节点上，因此 InnoDB 必须要有主键，通过主键索引效率很高。但是辅助索引需要两次查询，先查询到主键，然后再通过主键查询到数据。因此，主键不应该过大，因为主键太大，其他索引也都会很大。而 MyISAM 是非聚集索引，数据文件是分离的，索引保存的是数据文件的指针。主键索引和辅助索引是独立的。
- InnoDB 不保存表的具体行数，执行`select count(*) from table` 时需要全表扫描。而 MyISAM 用一个变量保存了整个表的行数，执行上述语句时只需要读出该变量即可，速度很快；
- InnoDB 最小的锁粒度是`行锁`，MyISAM 最小的锁粒度是`表锁`。一个更新语句会锁住整张表，导致其他查询和更新都会被阻塞，因此并发访问受限。这也是 MySQL 将默认存储引擎从 MyISAM 变成 InnoDB 的重要原因之一；
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-存储引擎\20201217224710893.png)

# 4. 存储引擎的选择

在选择存储引擎时，应该根据应用系统的特点选择合适的存储引擎。对于复杂的应用系统，还可以根据实际情况选择多种存储引擎进行组合.以下是几种常用的存储引擎的使用环境.

- **InnoDB**:是MySQL的默认存储引擎，用于事务处理的程序，支持外键.如果应用对事务的完整性有比较高的需求，在并发条件下要求数据的一致性，数据操作除了插入和查询以外，还包含很多的更新、删除操作，那么InnoDB存储引擎是比较合适的选择。InnoDB存储引擎除了有效的降低由于删除和更新导致的锁定，还可以确保事务的完整提交和回滚，对于类似于计费系统或者财务系统等对数据准确性要求比较高的系统，InnoDB是最合适的选择.
- **MyISAM**：如果应用是以读操作和插入操作为主，只有很少的更新和删除操作，并且对事务的完整性、并发性要求不是很高，那么选择这个存储引擎是非常合适的.
- **MEMORY**：将所有数据保存在RAM（随机存取存储器（英语：Random Access Memory，缩写：RAM），也叫主存，是与CPU直接交换数据的内部存储器。它可以随时读写（刷新时除外），而且速度很快，通常作为操作系统或其他正在运行中的程序的临时数据存储介质。RAM工作时可以随时从任何一个指定的地址写入（存入）或读出（取出）信息。它与ROM的最大区别是数据的易失性，即一旦断电所存储的数据将随之丢失。RAM在计算机和数字系统中用来暂时存储程序、数据和中间结果）中，在需要快速定位记录和其他类似数据环境下，可以提供几块的访问。MEMORY的缺陷就是对表的大小有限制，太大的表无法缓存在内存中，其次是要确保表的数据可以恢复，数据库异常终止后表中的数据是可以恢复的。MEMORY表通常用于更新不太频繁的小表，用以快速得到访问结果.
- **MERGE**：用于将一系列等同的MyISAM表以逻辑方式组合在一起，并作为一个对象引用他们.MERGE表的优点在于**可以突破对单个MyISAM表的大小限制**，并且通过将不同的表分布在多个磁盘上，可以有效的改善MERGE表的访问效率.这对于存储诸如数据仓库等**VLDB(海量数据量)**环境十分合适.



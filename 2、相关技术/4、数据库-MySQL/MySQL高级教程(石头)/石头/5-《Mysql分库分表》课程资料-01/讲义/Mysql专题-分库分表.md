### 1.简介
**分库**：将原本**一个数据库中的数据，拆分到多个数据库进行保存**，然后我们通过访问一个数据库，在MyCat中成为逻辑库。

> 分库的目的：扩容，IO速度快！

**分表**：如果一个数据表的数据量已经超过千万，那么查询速度会比较慢，这时候可以考虑进行分表操作，将一个表的数据保存到多个表中，这样可以控制单表的数据量。

> 水平拆分 垂直拆分   需要遵循一定的原则！！

### 2.作用

分库分表的作用可以通过下表对比可见：
|  | 分库分表前 | 	分库分表后|
|--|--|--|
|并发情况  |  	MySql单机部署，扛不住高并发需求|MySql从单台机器到多台机器，并发访问效率提高不少|
|磁盘使用情况  |  	单机磁盘使用几乎爆满|	拆分为多个库，每个库的磁盘使用率大大降低|
|SQL执行性能  |  单个数据表的数据量太大，查询缓慢|	单个数据表数据量减少，查询效率大大提高|

### 3.如何进行分库分表(规则)

**水平拆分**：就是表一张表的数据给弄到**多个库**的多个表进行保存，但是**每个库里面的表结构都是一样的**， 只不过每个库表放的数据不同，所有**库表的数据加起来就是全部数据**。  

 意义：就是将数据均匀地拆分到更多的库中，然后用多个库来扛更多的并发，还有就是用多个库的存储容量来进行扩容。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308163648983.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

> 分表需要定义一种规则、例如按照月份、季度、地区、IP范围、ID取余......

水平拆分简单示例：


```shell
database1: 192.168.1.12   user(id,name,age)

database2: 192.168.1.11   user(id,name,age)
 
两个数据库中的数据表结构都一样，只是根据某种规则，比如按id的范围，如果id在某个范围内就将该条数据保存在database1; 如果在另外一个范围内就保存在database2中。
```
**垂直拆分**：就是一个表有很多字段，给拆分到多个表中或者是多个库上去。每个库表的数据表结构都不同，每个库表只包含部分字段。一般来说，会将较少的访问频率比较高的表放到同一张表中，然后将较多的访问频率比较低的字段放到另外一张表中。因为数据库是有缓存的，你访问频率高的字段少，在缓存中存放的行越多，查询性能就越好。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308164012410.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

```shell
垂直拆分示例：
 
database1 : 192.168.1.12
basic_student_info(id,xm,sfzjh,zzmm,xh) 这个表主要用于保存学生信息经常出现的字段
 
expand_student_info(id,dh,mz,syd) 这个表主要用于保存学生信息中不太常见的字段
 
将一个数据字段比较多的表拆为两个表进行保存，两边数据库表的结构不一致。
```

 ### 4.分库分表方式
 a. **按range进行**：每个库一段连续的数据，一般按**时间范围进行拆分**，**较少用**，

> 缺点：会产生热点问题，大量的流量都打在最新的数据上。

```
range拆分的好处在于扩容的时候很简单，只需要预备好，给每个月的数据准备一个数据库，到了一个新的月份，自然就保存到别的库上面去了，缺点就是大量的请求，都是访问最新的数据
```
b. **按照某个字段hash一下均匀分散**，较常见。

```
hash的好处就是平均分配每个库的数据量和请求压力；

缺点就是扩容起来比较麻烦，会有一个数据迁移的难点，之前的数据需要重新计算hash然后分发到不同的数据库或表中。
```
> 根据存储的数据库数据的特点，自定义一种分片的规则，尽量避免热点数据！！

### 5.示例

下面通过一个简单的示例说明一下MyCat是如何进行分库分表的。

**环境信息**：

```shell
centos7.0 
主机: 
    192.168.70.129、  mycat
    192.168.70.128、  mysql-server1
    192.168.70.130    mysql-server2

-----------------------------------------------------
mycat：1.6
需要jdk环境变量
```
这里主要要配置几个文件： `schema.xml` 、`server.xml` 、`sequence_conf.properties`等
![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-分库分表\20200308174510737.png)
【a】编辑server.xml，修改逻辑库的地址（即我们后面配置数据源的时候就只需要连接这个逻辑库即可

```xml
vim server.xml
```

```xml
<user name="root">
              <property name="password">123456</property>
              <property name="schemas">mycat_order</property>

              <!-- 表级 DML 权限设置 -->
              <!--            
              <privileges check="false">
                      <schema name="TESTDB" dml="0110" >
                              <table name="tb01" dml="0000"></table>
                              <table name="tb02" dml="1111"></table>
                      </schema>
              </privileges>           
               -->
</user>

<user name="user">
	<property name="password">123456</property>
	<property name="schemas">mycat_order</property>
	<property name="readOnly">true</property>
</user>
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308174721940.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
这里配置了一个**逻辑库mycat_order**以及加了两个用户root/123456和user/123456. 这里为了测试范围分片规则，所以server.xml中还需要修改本地序列化规则：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308174935409.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

>注意：需要修改为0，才能使用本地序列化的值。
>
>原始表ID不能自增了，需要使用MyCat的序列化文件生成ID值完成自增。

【b】创建数据库和表：这里需要在两台服务器192.168.70.128和192.168.70.130上面都执行。

```sql
-- t_order 、 t_order_detail
 
CREATE TABLE `t_order` (
  `order_id` INT(20) NOT NULL COMMENT '订单ID',
  `user_id` INT(11) DEFAULT NULL COMMENT '用户ID',
  `pay_mode` TINYINT(4) DEFAULT NULL COMMENT '支付方式',
  `amount` FLOAT DEFAULT NULL COMMENT '金额',
  `order_date` DATETIME DEFAULT NULL COMMENT '订单时间',
  PRIMARY KEY (`order_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;
 
CREATE TABLE `t_order_detail` (
  `od_id` INT(20) NOT NULL COMMENT '订单详情ID',
  `order_id` INT(20) DEFAULT NULL COMMENT '订单ID',
  `goods_id` INT(20) DEFAULT NULL COMMENT '商品ID',
  `unit_price` FLOAT DEFAULT NULL COMMENT '单价',
  `qty` INT(11) DEFAULT NULL,
  PRIMARY KEY (`od_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8
```
登录Mysql:

```
mysql -u root -p123
```
创建数据库:
```
create database test_mycat;
show databases;
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308175321617.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

```sql
use test_mycat;
```
分别执行上面的两条创建表语句：
**订单表：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308175438169.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**订单详情表：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308175513309.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

```sql
show tables;
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308175539284.png)
因为测试mycat分库分表，所以需要在另外一台服务器上面执行上面的创建表语句。

```sql
mysql -uroot -p123
show databases;
create database test_mycat;
use test_mycat;
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308175810573.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308175828691.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
至此，192.168.70.128 和192.168.70.130两台服务器上的数据库和表都一模一样。

【c】编辑schema.xml：配置数据节点datanode、datahost等信息

```xml
vim schema.xml
```

```xml
<?xml version="1.0"?>
<!DOCTYPE mycat:schema SYSTEM "schema.dtd">
<mycat:schema xmlns:mycat="http://io.mycat/">

	<schema name="mycat_order" checkSQLschema="false" sqlMaxLimit="100">
                <table name="t_order"  dataNode="dn1,dn2" rule="mod-long">
                        <childTable name="t_order_detail" primaryKey="od_id" joinKey="order_id" parentKey="order_id"></childTable>
                </table>
        </schema>
 
        <dataNode name="dn1" dataHost="host1" database="test_mycat" />
        <dataNode name="dn2" dataHost="host2" database="test_mycat" />
 
        <dataHost name="host1" maxCon="1000" minCon="10" balance="0"
                          writeType="0" dbType="mysql" dbDriver="native" switchType="1"  slaveThreshold="100">
                <heartbeat>select user()</heartbeat>
                <!-- can have multi write hosts -->
                <writeHost host="host1" url="192.168.70.128:3306" user="root"
                                   password="123">
                </writeHost>
        </dataHost>
 
        <dataHost name="host2" maxCon="1000" minCon="10" balance="0"
                          writeType="0" dbType="mysql" dbDriver="native" switchType="1"  slaveThreshold="100">
                <heartbeat>select user()</heartbeat>
                <!-- can have multi write hosts -->
                <writeHost host="host2" url="192.168.70.130:3306" user="root"
                                   password="123">
                </writeHost>
        </dataHost>
	
</mycat:schema>
```
【d】编辑rule.xml： 配置根据主表的主键ID 即order_id进行分库分表

```shell
vim rule.xml
```

```xml
<tableRule name="mod-long">
	<rule>
		<columns>order_id</columns>
		<algorithm>mod-long</algorithm>
	</rule>
</tableRule>
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308180236240.png)
接着继续修改mod-long规则的数据节点个数，**因为这里只用到了两个节点（两个数据库服务器）**。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308180352193.png)
【e】vim sequence_conf.properties ： 配置序列化ID

```shell
vim sequence_conf.properties
```

```shell
ORDER.HISIDS=
ORDER.MINID=1001
ORDER.MAXID=2000
ORDER.CURID=1000
 
ORDERDETAIL.HISIDS=
ORDERDETAIL.MINID=1001
ORDERDETAIL.MAXID=2000
ORDERDETAIL.CURID=1000
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308180815998.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
【f】启动MyCat：

```
./mycat start

[root@centos1 bin]# ./mycat status
Mycat-server is running (18207).
```
【g】测试
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308181219148.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308181248248.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

```
select * from t_order;
select * from t_order_detail;
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308181947134.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
【h】插入数据测试分库分表

```sql
INSERT INTO `t_order`
            (`order_id`,
             `user_id`,
             `pay_mode`,
             `amount`)
VALUES (NEXT VALUE FOR MYCATSEQ_ORDER,101,1,111.1);
 
INSERT INTO `t_order`
            (`order_id`,
             `user_id`,
             `pay_mode`,
             `amount`)
VALUES (NEXT VALUE FOR MYCATSEQ_ORDER,102,5,222.2);
 
INSERT INTO `t_order`
            (`order_id`,
             `user_id`,
             `pay_mode`,
             `amount`)
VALUES (NEXT VALUE FOR MYCATSEQ_ORDER,103,7,333.3);
select * from t_order;
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308182249416.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308182512683.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308182615824.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
【i】vim sequence_conf.properties ： 可以看到ORDER.CURID=1003记录了当前序列已经到了1003，说明本地序列配置生效。

```shell
vim sequence_conf.properties 
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308182721492.png)
【j】测试订单详情表数据分表情况

插入数据库数据：

```sql
INSERT INTO `t_order_detail`
            (`od_id`,
             `order_id`,
             `goods_id`,
             `unit_price`,
             `qty`)
VALUES (NEXT VALUE FOR MYCATSEQ_ORDERDETAIL,
        1003,
        55,
        10,
        20);
        
INSERT INTO `t_order_detail`
            (`od_id`,
             `order_id`,
             `goods_id`,
             `unit_price`,
             `qty`)
VALUES (NEXT VALUE FOR MYCATSEQ_ORDERDETAIL,
       1002,
        66,
        20,
        30);
        
INSERT INTO `t_order_detail` (
  `od_id`,
  `order_id`,
  `goods_id`,
  `unit_price`,
  `qty`
) 
VALUES
  (
    NEXT VALUE FOR MYCATSEQ_ORDERDETAIL,
    1001,
    77,
    30,
    40
  ) ;
```
逻辑数据库中的数据分布：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020030818294171.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
实际物理库中数据分布：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308183030593.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308183108686.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
```shell
vim sequence_conf.properties 
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200308183204403.png)
至此，一个比较简单的分库分表示例就完成了!

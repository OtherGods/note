# 0.关于几个从Django中带过来的概念

主表：表中的主键作为关联表的外键的表，并且表中没有外键的表叫做主表。
从表：外键所在的表叫做从表。

正向查询：查询从表中某条记录的详细内容，包括关联的从表中的一条内容。

反向查询：查询主表中某条记录的详细内容，包括关联该记录的从表中的多条内容（记作P）

1. 无论是一对多还是多对多，这个P都有多条

# 1.关联映射(多表查询)

## 1.1 表关系

数据库中多表之间存在着三种关系，如图所示。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190904174614140.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

> 一对一：在任意一方中引入对方的主键作为外键；
> 一对多：在 “ 多 ” 的一方，添加 “ 一 ” 的以访的主键作为外键；
> 多对多：产生中间关系表，引入两张表的主键作为外键，两个主键成为联合主键或使用新的字段作为主键。

## 1.2 表对应的实体类的关系

通过数据库表可以描述数据之间的关系，在Java中通过对象也可以进行关系描述，如下代码所示：

> 在定义实体类的时候之，类中的属性所以有下面这些情况，是因为要**<u>*方便存储正向查询和反向查询的结果*</u>**。
>
> 举个例子：使用一对多的表tb_user和tb_orders
> 	1.管理员要查询tb_user表中的某个用户的详细信息（包括该用户的订单）；
> 			查询返回的结果是User类的实例对象，所以要在User类中增加一个保存Orders类型的List集合属性用来存储该用户的订单；
>
> ​	2.管理员要查询tb_user表中的某个订单的详细信息（包括该订单是由哪个用户下单的）；
> ​			查询返回的结果是Orders类的实例对象，所以要在Orders类中增加一个User类类型的属性来存储该订单属于哪个用户。
>
> **所以在定义实体类的时候最好在关联的两个类中都定义对方的类类型的属性，以方便用户进行正向查询和反向查询**

```java
//一对一，这种关系的实体类的结构有三种情况：
//	1.A类中有B类类型的字段，B类中没有A类类型的字段
//	2.B类中有A类类型的字段，A类中没有B类类型的字段
//	3.A类和B类中都有对应类类型的字段

//这两个类分别对应2.2节中的Person类和Idcard类
Class A{ B b; }		//外键所在表对应的类
Class B{ A a; }		

//一对多，这种关系的实体类的结构有三种情况：类似于一对
//一关系中的三种情况；
//区别只是：在一对多关系中 “ 一 ” 所在表（没有外键的
//表）对应的实体类（也就是A类）中保存的关联字
//段是以B类类型作为实际类型变量的List集合

//这两个类分别对应2.2节中的User类和Orders类
Class A{ List<B> b };
Class B{ A a; }		//外键所在表对应的类

//多对多，这是种关系的实体类的结构有三种情况：类似于多对
//多关系中的三种i情况：
//区别只是：在多对多关系中只有在中间表中有外键，而中间
//表没有定义对应的POJO实体类，在和中间表关联的另外两张
//表对应的POJO实体类A类、B类中保存的关联字段是以
//B类、A类类型作为实际类型变量的List集合

//这两个类分别对应2.2节中的Orders类和Product类
Class A{ List<B> b; }
Class B{ List<A> a; }
```

在上面Java代码中三种关联关系的描述如下：

> 一对一关系：在本类中定义对方类类型的对象，如A类中定义B类型的属性b，B类中定义A类类型的属性a；
>
> 一对多关系：一个A类类型对应的多个B类类型的情况，需要在A类中以集合的方式引入B类类型的对象，在B类中定义A类类型的属性a；
>
> 多对多关系：在A类中定义B类类型的集合，在B类中定义A类类型的集合。



系统设计的三种实体关系分别为：多对多、一对多和一对一关系。注意：一对多关系可以看为两种：  即一对多，多对一。

> 现实生活中实体和实体之间的关系：  一对多     多对多    一对一

关系：**关联关系是双向的！！！A关联B，同样B也关联A**

# 2.关联映射作用

在现实的项目中进行数据库建模时，我们要遵循数据库设计范式的要求，会对现实中的业务模型进行拆分，封装在不同的数据表中，**表与表之间存在着一对多或是多对多的对应关系**。进而，我们对数据库的增删改查操作的主体，也就从**单表变成了多表**。那么Mybatis中是如何实现这种多表关系的映射呢？（主要用于查询的时候，返回值的映射）

## 2.1查询结果集ResultMap来源

```
resultMap元素是 MyBatis中最重要最强大的元素。它就是让你远离90%的需要从结果集中取出数据的JDBC代码的那个东西，而且在一些情形下允许你做一些 JDBC 不支持的事情。	

有朋友会问，之前的示例中我们没有用到结果集，不是也可以正确地将数据表中的数据映射到Java对象的属性中吗？是的。

这是resultMap元素设计的初衷：就是简单语句不需要明确的结果映射，而很多复杂语句确实需要描述它们的关系；在默认情况下，Mybatis程序在运行时会自动将查询到的数据与需要返回的对象的属性进行匹配赋值（需要表中的列名与对象的属性名称完全一致），然而在实际开发时，数据表中的列和需要返回的对象的属性可能不会完全一致，这种情况下Mybatis是不会自动赋值的。此时就可以使用<resultMap>元素进行处理。
```

## 2.2 Mybatis在映射文件中加载关联关系的方式

> Mybatis在映射文件中加载关联关系对象主要通过两种方式：嵌套查询和嵌套结果——>
>
> 1. 嵌套查询
>    通过指定另外一条SQL映射语句来返回预期的复杂类型
> 2. 嵌套结果
>    使用嵌套结果映射来处理重读的联合结果的子集

### 2.2.1 使用嵌套结果的注意事项

​		如果关联的表中有字段的名称相同，那么在使用Mybatis的时候需要给这些相同名字的字段起一个别名，并且在resultMap中使用这个别名做区分，否则不会报错，但是Mybatis中查询得到的结果会出现问题。

下面是视频中给出的一对多的例子，在多对多中同样也要给关联表中相同的字段起一个别名：

![一对多](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\一对多.png)

## 2.3 resultMap元素

### 2.3.1 准备的表和实体类

> ​		对于一对一关系、一对多关系、多对多关系的简单示例（表关系和POJO之间的关系，以及在这些关系上使用resultMap元素的方式）
>
> ​		一对一关系：一个人与他的身份证
>
> ​		一对多关系：一个人与他的订单
>
> ​		多对多关系：一个订单和商品
>
> 在这里创建六张表：
> 	tb_idcard和tb_person用于表示一对一关系
> 	tb_orders和tb_user用于表示多对一关系
>
> 对应的六个POJO实体类分别是：
> 	IdCard、Person 和 Orders、User 和 Orders、Product
>
> 1. 表结构：
>    一对一表
>
>    1. ***tb_person表***
>       ![image-20220805095247359](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805095247359.png)
>       **外键：**
>       ![image-20220805095344838](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805095344838.png)
>    2. tb_idcard表
>       ![image-20220805095401686](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805095401686.png)
>
>    ------
>
>    一对多表
>
>    1. tb_user表
>       ![image-20220805095439310](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805095439310.png)
>
>    2. ***tb_orders表***
> ![image-20220805095516612](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805095516612.png)
>       
>
> **外键：**
>       
>    ![image-20220805095547378](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805095547378.png)
>
> ---
>
> 多对多表
>
>    1. tb_product表
>       ![image-20220805103444921](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805103444921.png)
>    2. ***tb_ordersitem表（中间表）***
>       ![image-20220805103511221](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805103511221.png)
>       ***外键：***
>       ![image-20220805103604279](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805103604279.png)
>    3. tb_orders表（在这个多对多关系中不考虑tb_orders表和tb_tb_user表的关系，所以不考虑tb_orders表结构中的外键）
>       ![image-20220805103757593](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805103757593.png)
>    4. 这些表的ER图
>       1. ![image-20220805120750224](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805120750224.png)
>       2. ![image-20220805120818006](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805120818006.png)
>    3. ![image-20220805120855019](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805120855019.png)
>
> 
>
> 2. 表对应的POJO实体类（实体类中的字段可以看1.2节中介绍的内容）
>    一对一POJO
>
>    1. Person类的字段（对应的表中有外键，在这个实体类中增加了一个字段用来**保存外键关联的表对应的实体类实例**）
>
>       ```java
>       	private Integer id;
>       	private String name;
>       	private Integer age;
>       	private String sex;
>       	//个人关联的证件，
>       	private IdCard idcard;
>       ```
>
>    2. IdCard类的字段
>
>       ```java
>       	private Integer id;
>       	private String code;
>       	//这个字段是我添加的，为了完成我的猜想
>       	private Person person;
>       ```
>
>    ---
>
>    一对多POJO
>
>    1. User类的字段（对应的表中没有外键，但是在这个实体类中增加了一个字段用来**保存tb_orders表中与tb_user表中某一条目相关联的条目对应的实体类实例集合**）
>
>       ```java
>       	// 用户编号
>       	private Integer id;
>           // 用户姓名
>       	private String username;
>       	// 用户地址
>       	private String address;
>       	//用户关联的订单
>       	`private List<Orders> ordersList`;
>       ```
>
>    2. Orders类的字段
>
>       ```java
>       	//订单id
>       	private Integer id;
>       	//订单编号
>       	private String number;
>       	//这个字段是我添加的，为了完成我的猜想
>       	private User user;
>       ```
>
>    ---
>
>    多对多POJO
>
>    1. Product类的字段（对应的表中没有外键，但是在这个实体类中增加了一个字段用来**保存tb_orders表中与tb_product表中某一条目相关联的条目对应的实体类实例集合**）
>
>       ```java
>       	//商品id
>       	private Integer id;
>       	//商品名称
>       	private String name;
>       	//商品单价
>       	private Double price;
>       	//该字段用于多对多关系，关联订单集合信息
>       	`private List<Orders> ordersList;` 
>       ```
>
>    2. Orders类的字段（对应的表中没有外键，但是在这个实体类中增加了一个字段用来**保存tb_product表中与tb_orders表中某一条目相关联的条目对应的实体类实例集合**）
>
>       ```java
>       	//订单id
>       	private Integer id;
>       	//订单编号
>       	private String number;
>       
>       	//在多对多关系中增加字段
>       	//该字段用于多对对关系，关联商品集合信息
>       	`private List<Product> productList;`
>       ```
>
>       **注意：多对多关系的表对应的实体类中没有定义中间表的实体类**
>
> 

### 2.3.2 resultMap元素介绍

#### 2.3.2.1 属性

```
type —— 标识需要映射的POJO类，也就是编写的SQL语句返回的实体对象
		的类型，例如：正向查询的SQL语句返回的对象就是有外键的表对
		应的实体类的类型，反向查询的SQL语句返回的对象就是没有外键
		的表对应的实体类的类型。
id —— 这和resultMap的唯一标识
```

#### 2.3.2.2 子元素

```java
constructeor —— 用于配置构造方法（当一个POJO中未定义无参的构造方法时，就可以使用这个元素进行构造）
id —— 作用与result相同，同时可以标识出用这个字段值可以区分其他对象实例。可以理解为数据表中的主键，可以定位数据表中唯一一笔记录
result —— 将数据表中的字段注入到Java对象属性中

association —— 关联，简单的讲，就是“有一个”关系，如“用户”有一个“帐号”   has a 
collection —— 集合，顾名思议，就是“有很多”关系，如“客户”有很多“订单”    has many
```

##### 2.3.2.2.1 id、result子元素

​		这两个子元素中的属性

```
property：表述POJO类中的属性名
column：表示POJO类对应的表中的字段名
```



##### 2.3.2.2.2 association子元素（一对一，一对多的正向查询）

​		这个子元素只用于一对一关联，也就是上面表tb_person和tb_idcard

​		该子元素的属性的介绍

> # **前提**
>
> 这里对于这个association子元素的属性的描述是基于 一个实际的查询例子（使用2.3.1节中的类和表），例如：用户要查询的是tb_person表，在查询的结果中需要展示出该用户的身份证号（tb_idcard表）；
>
> 分析：
> 		有两种SQL语句的格式分别是
> 			**select * from tb_person inner join tb_idcard……**（教材中使用的方式）
> 				和
> 			select * from tb_idcard inner join tb_person…… 
> 				关于这种方式在使用嵌套查询的时候association元素中的属性column的值是被嵌套的SQL语句中的参数
> 				，因为tb_idcard表中没有外键字段，只是在IdCard类中有一个Person类型的属性；
> 				这种情况在一对多中，SQL语句的形式如右：……from tb_user inner join tb_orders…… ，关联关系是嵌
> 				套查询的时候也有出现，同样在这种情况下collection元素中属性column的值是被嵌套的SQL中的参数
> 				，因为tb_user中同样没有外键，但是在User实体类中有以Order类型为类型参数的List集合。
>
> 在教材中编写的SQL语句是用的第一种方式，也就是查询tb_person表，这种方式对应的加载关联关系的方式有两种，分别是嵌套查询和嵌套结果：
>
> 1. ![image-20220805163035497](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\image-20220805163035497.png)
> 2. ![1](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\1.png)

```
//SQl语句是select *from tb_person inner join //tb_idcard……的情况下，关联关系的两种方式中
//使用的这些属性的介绍：

property：指定映射到的实体类（Person）对象的属性名（card_id）。

column：这个属性只有在嵌套查询中才会被用到；这个属性有两个作用——>
	1. 当SQL语句中inner join、leftjoin、righ join前面是有外键的表时，这个属性用来指定表（tb_person）中对应的字段名card_id；
	2. 当SQL语句中inner join、leftjoin、righ join前面是没有外键的表时，这个属性是嵌套的SQL语句中的参数，例如多对多中的嵌套查询的例子。

javaType：指定映射到实体对象属性的类型（tb_person表关联的tb_orders表对应的Orders实体类的类型），这个
		  属性可以在一对一中使用，也可以在一对多的正向查询中使用（在一对多的反向查询要使用ofType）

select：指定引入嵌套查询的子SQL语句，该属性用于关联映射表中的嵌套查询

fetchType：指定在嵌套查询时是否启用延迟加载，该属性有两个lazy和eager两个属性值，默认值为lazy（即默认关联映射延迟加载）
```

​		实际使用：（按照教材中使用的SQL语句来写）

1. 嵌套查询
   ![IMG_20220805_164510](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\IMG_20220805_164510-1659689954879.jpg)
   ![IMG_20220805_164515](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\IMG_20220805_164515-1659690012711.jpg)
2. 嵌套结果
   ![IMG_20220805_164528](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\IMG_20220805_164528.jpg)



​		**总结：**在使用一对一关系中使用association元素的时候

1. 在使用嵌套查询的时候，尽量使SQL语句的形式如右——>**select * from 有外键的表 inner join 没有外键的表 on……**，这样可以确保在使用association元素的时候，属性column的值为 “ 有外键的表 ” 中的外键字段的名字。
2. 在使用嵌套结果的时候，对于SQL语句形式的没有限制，因为产生混乱的地方（也就是association元素的属性column的值）没有了，使用这种方式仅仅需要对应好表和相应实体类的属性的映射即可



##### 2.3.2.2.3 collection子元素（多对多、一对多的反向查询）

​		该子元素只用于一对多和多对多关联，也就是上面的表tb_user与tb_orders，tb_orders与tb_product

​		这个子元素的属性与2.3.2.2.2节类似，对于这个元素中特有属性的介绍

> # **前提**
>
> ## 一对多的前提
>
> 这里对于这个collection子元素的属性的描述是基于 一个实际的查询例子（使用2.3.1节中的类和表），例如：用户要查询的是tb_user表，在查询的结果中需要展示出该用户的的所有订单（tb_orders表）；
>
> 分析：
> 		有两种SQL语句的格式分别是：
> 			select * from tb_orders inner join tb_user on ……（正向查询）
> 				如果在一对多中使用collection元素，那么就需要使用该元素的属性ofType，这个属性的值是集合的属
> 				性的元素类型，而在tb_orders表对应的实体类Orders中没有也不能有集合类型的字段，所以在这
> 				种SQL语句的条件下collection中不能使用collection元素，需要使用association元素。
> 				和
> 			**select * from tb_user inner join tb_orders on ……**（教材中使用的反方式）（反向查询）
> 				这种SQL语句在使用嵌套查询的时候，弊端同样和一对一中的分析一样，因为tb_user表中同样和
> 				tb_person表一样没有它们所关联的表的外键
>
> 在教材中编写的SQL语句是用的第二种方式，也就是查询tb_user表，这种方式对应的加载关联关系的方式有两种，分别是嵌套查询和嵌套结果：
>
> 1. ![1](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\1-1659713301050.png)
> 2. ![1](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\1-1659713415176.png)
>
> 
>
> ## 多对多的前提
>
> 这里对于这个collection子元素的属性的描述是基于 一个实际的查询例子（使用2.3.1节中的类和表），例如：用户要查询的是tb_orders表，在查询的结果中需要展示出该订单的所有商品（tb_product表）；
>
> 分析：
> 		有两种SQL语句的格式分别是（这两种SQL语句都算是反向查询）
> 			**select * from tb_orders inner join tb_ordersitem…… inner join tb_product……**（教材中使用的方式）
> 				和
> 			select * from tb_product inner join tb_ordersitem…… inner join tb_orders……
>
> ​			这两种SQL语句格式中主要的两张表tb_orders和tb_orders中都没有外键，所以在嵌套查询的时候，这两种
> ​			方式使用的collestion元素的column的值为嵌套查询中被嵌套的SQL语句中的参数。
>
> 

```
ofType：该属性和javaType属性对应，它用于指定实体对象（PPOJO）中集合类属性（pf1）所包含的元素类型；

因为collection子属性是用于一对多和多对多，所以column指定字段所在表（记作Q）中的一行记录对应于Q表所关联的表（也就是ofType指定的POJO实体类对应的数据表）中多行记录
```

​		实际使用：（按照教材中使用的SQL语句来写）

1. 一对多

   1. 嵌套查询		
      		因为SQL语句的形式是：select * from 没有外键的表 inner join 有外键的表 on……，所以在collection元素中column的值的含义是嵌套查询中嵌套的SQL语句中的参数；（这种形式只有在多对多的嵌套查询的例子中有出现过）
         		如果SQL语句的形式是：select * from 有外键的表 inner join 没有外键的表 on……，那么在collection元素中column的值的含义是有外键的表中外键字段的名字。
   2. 嵌套结果
      ![IMG_20220805_180232](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\IMG_20220805_180232.jpg)

2. 多对多

   1. 嵌套查询
      ![IMG_20220805_235749](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\IMG_20220805_235749.jpg)
      ![IMG_20220805_235759](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\IMG_20220805_235759.jpg)
   2. 嵌套结果
      ![IMG_20220805_235909](D:\Tyora\AssociatedPicturesInTheArticles\MyBatis关联映射\IMG_20220805_235909.jpg)

   

​		**总结：**在使用一对多、多对多关系中使用collection元素的时候

1. 在使用嵌套查询的时候，尽量使SQL语句的形式如右——>**select * from 有外键的表 inner join 没有外键的表 on……**，这样可以确保在使用association元素的时候，属性column的值明确为表中的外键的字段，而不是嵌套查询中被嵌套的SQL语句中的参数（这种形式让人难以理解column的值为什么是被嵌套的SQL语句的参数）。
2. 在使用嵌套结果的时候，对于SQL语句形式的没有限制，因为产生混乱的地方（也就是collection元素的属性column的值）没有了，使用这种方式仅仅需要对应好表和相应实体类的属性的映射即可

每个元素的用法及属性我会在下面结合使用进行讲解。

### 2.4总结

> # 在使用关联映射的时候有很重要的两步：
>
> 1. ## **确定SQL语句的结构：（确定正向查询还是反向查询）**
>
>    **确定在inner join、left join（right join和它类似）前的表中有没有外键（因为这里SQL语句的选择会决定在第二步中选择嵌套查询后column的值的含义）**，也就是要明确什么是主要要查询的，什么是查询的附带，即确定SQL语句的结构，例如一对多示例中，如果查询的目的是为了得到所有用户的详细信息（包括该用户的订单信息），那么left join前的表就是tb_user，如果查询的目的是为了得到所有订单表的详细信息（包括下该订单的用户信息），那么left join前的表就是tb_orders。
>
> 2. ## **确定关联方式：**
>
>    **在确定了第一步之后，还需要再次确定在加载关联关系的方式使用的是嵌套查询还是嵌套结果，如果选择的是嵌套查询，那么需要根据第一步中的SQL语句的结构确定association、collection元素中属性column的值的含义**
>
> # 嵌套查询的使用速查表
>
> 嵌套结果也可以对照这个表来快速查询，只是不必在意association和collection子元素中的属性column的值，以及注意关联表中给重名字段起别名并在后面的SQL中使用该别名。
>
> **一对一、一对多、多对多，分别使用正向查询（查从表）和反向查询（查主表）关于使用嵌套查询的表格**（嵌套结果没有那么多的问题，主要就是声明别名，解决表中同名字段冲突的问题）
>
> |        |                正向查询（查从表，有外键的表）                |               反向查询（查主表，没有外键的表）               |
> | :----: | :----------------------------------------------------------: | :----------------------------------------------------------: |
> | 一对一 | 描述：查询tb_person表中的某条记录的详细信息，这个详细信息中**包含关联的tb_idcard表中的某一条记录**<br />resultMap元素中使用association子元素；该子元素的属性property和column分别指的是：POJO实体类（Person）中关联的另一个POJO类（IdCard）类型的属性的名称（idCard）、tb_person表中外键的字段名（card_id）；在该子元素中使用javaType属性指定tb_person表关联的表tb_idcard对应实体类的类型（IdCard） | 描述：查询tb_idcard表中的某条记录的详细信息，这个详细信息中**包含关联的tb_person表中的一条记录**<br />resultMap元素中使用association子元素，该子元素的属性property和column分别指的是：POJO实体类（IdCard）中关联的另一个POJO类（Person）类型的属性的名称（person）、传递到嵌套查询中被嵌套的SQL语句中的参数（可以去看多对多的实例理解一下）；在该子元素属性中使用javaType属性指定tb_idcard表关联的表tb_person对应实体类的类型（Person） |
> | 一对多 | 描述：查询tb_orders表中的某条记录的详细信息，这个信息中**包含关联的tb_user表中的某一条记录**<br />resultMap元素中使用association子元素，该子元素的属性property和column分别指的是：POJO实体类（Orders）中保存关联的另一个POJO类（User）类型的属性的名称（user）、tb_orders表中外键的字段名（user_id）；在该子元素中使用javaType属性指定tb_orders表关联的表tb_user表对应实体类的类型（User） | 描述：查询tb_user表中的某条记录的详细信息，这个信息中**包含关联的tb_orders表中的多条记录**<br />resultMap元素中使用collection子元素，该子元素的属性property和column分别指的是：POJO实体类（User）中保存  “以关联的另一个POJO实体类（Orders）类型作为实际类型参数的List集合 ” 的属性（User类中List<Orders>类型的属性）的名称（ordersList）、传递到嵌套查询中被嵌套的SQL语句中的参数（可以去看多对多的实例理解一下）；在该子元素中使用ofType属性指定tb_user表关联的表tb_orders表对应实体类的类型（Orders） |
> | 多对多 |                         暂时没有想法                         | 描述：查询tb_orders表中某条记录的详细信息，这个信息中**包含多对多关联的tb_product表中的多条记录**（查询tb_product表中某条记录的详细信息也是同样的）<br />resultMap元素中使用collection子元素，该子元素的属性property和column分别指的是：POJO实体类（Orders）中保存  “ 以关联的另一个POJO实体类（Product）类型作为实际类型参数的List集合 ”  的属性（Orders类中的List<Product>类型的属性）的名称（productList）、传递到嵌套查询中被嵌套的SQL语句中的参数（可以去看多对多的实例理解一下）；在该子元素属性中使用ofType属性指定tb_orders表关联的表tb_product表对应的实体类的类型（Product） |
>
> 



# 3. 一对一关联(了解)

## 3.1. 需求
根据班级id查询班级信息(带老师的信息)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190904174839717.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

## 3.2.创建数据表
创建一张教师表和班级表，这里我们假设一个老师只负责教一个班，那么老师和班级之间的关系就是一种一对一的关系。

```sql
CREATE TABLE teacher(
    t_id INT PRIMARY KEY AUTO_INCREMENT, 
    t_name VARCHAR(20)
);
CREATE TABLE class(
    c_id INT PRIMARY KEY AUTO_INCREMENT, 
    c_name VARCHAR(20), 
    teacher_id INT
);
ALTER TABLE class ADD CONSTRAINT fk_teacher_id FOREIGN KEY (teacher_id) REFERENCES teacher(t_id);    

INSERT INTO teacher(t_name) VALUES('teacher1');
INSERT INTO teacher(t_name) VALUES('teacher2');

INSERT INTO class(c_name, teacher_id) VALUES('class_a', 1);
INSERT INTO class(c_name, teacher_id) VALUES('class_b', 2);
```
## 3.3.定义实体类
1、Teacher类，Teacher类是teacher表对应的实体类。

```java
public class Teacher {

	// 定义实体类的属性，与teacher表中的字段对应
	private int id; // id===>t_id
	private String name; // name===>t_name

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Teacher [id=" + id + ", name=" + name + "]";
	}
}
```
　2、Classes类，Classes类是class表对应的实体类
　

```java
public class Classes {

	// 定义实体类的属性，与class表中的字段对应
	private int id; // id===>c_id
	private String name; // name===>c_name

	/**
	 * class表中有一个teacher_id字段，所以在Classes类中定义一个teacher属性，
	 * 用于维护teacher和class之间的一对一关系，通过这个teacher属性就可以知道这个班级是由哪个老师负责的
	 */
	private Teacher teacher;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Teacher getTeacher() {
		return teacher;
	}

	public void setTeacher(Teacher teacher) {
		this.teacher = teacher;
	}

	@Override
	public String toString() {
		return "Classes [id=" + id + ", name=" + name + ", teacher=" + teacher + "]";
	}
}
```
## 3.4.定义sql映射文件classMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.mybatis.mapper.ClassMapper">

    <!-- 
        
        select * from class c, teacher t where c.teacher_id=t.t_id and c.c_id=1
    -->
    <select id="getClass1" parameterType="int" resultMap="ClassResultMap1">
        select * from class c, teacher t where c.teacher_id=t.t_id and c.c_id=#{id}
    </select>
    
    <!-- 使用resultMap映射实体类和字段之间的一一对应关系 -->
    <resultMap type="Classes" id="ClassResultMap1">
        <id property="id" column="c_id"/>
        <result property="name" column="c_name"/>
        <association property="teacher" javaType="Teacher">
            <id property="id" column="t_id"/>
            <result property="name" column="t_name"/>
        </association>
    </resultMap>
    
   
</mapper>
```
## 3.5 编写单元测试类

```java
	@Test
	public void test1(){
		Classes c1 = mapper.getClass1(1);
		System.out.println(c1);
	}
	
	@Test
	public void test2(){
		Classes c1 = mapper.getClass2(1);
		System.out.println(c1);
	}
```
## 3.6 MyBatis一对一嵌套查询总结
MyBatis中使用association（有一个）标签来解决一对一的嵌套查询，association标签可用的属性如下：

```
property:对象属性的名称
javaType:对象属性的类型
column:所对应的外键字段名称
select:使用另一个查询封装的结果
```
# 4. 一对多关联（重点）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190904175125956.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/201909041751312.png)
## 4.1.需求
根据classId查询对应的班级信息,包括学生
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190904175159578.png)
## 4.2.定义实体类
1、Student类

```java
/**
 * 学生实体
 */
public class Student {

  private long sId;
  private String sName;
  private long sAge;
  private String sEmail;
  private long classId;

  //额外准备一个班级对象
  private Classes classes;  //体现一个学生在一个班级中

  public Classes getClasses() {
    return classes;
  }

  public void setClasses(Classes classes) {
    this.classes = classes;
  }

  public long getSId() {
    return sId;
  }

  public void setSId(long sId) {
    this.sId = sId;
  }


  public String getSName() {
    return sName;
  }

  public void setSName(String sName) {
    this.sName = sName;
  }


  public long getSAge() {
    return sAge;
  }

  public void setSAge(long sAge) {
    this.sAge = sAge;
  }


  public String getSEmail() {
    return sEmail;
  }

  public void setSEmail(String sEmail) {
    this.sEmail = sEmail;
  }


  public long getClassId() {
    return classId;
  }

  public void setClassId(long classId) {
    this.classId = classId;
  }


  @Override
  public String toString() {
    return "Student{" +
            "sId=" + sId +
            ", sName='" + sName + '\'' +
            ", sAge=" + sAge +
            ", sEmail='" + sEmail + '\'' +
            ", classId=" + classId +
            ", classes=" + classes +
            '}';
  }
}
```
2、修改Classes类，添加一个List<Student> students属性，使用一个List<Student>集合属性表示班级拥有的学生，如下：

```java
package com.bruceliu.bean;


import java.util.List;

/**
 * 班级实体类  1的一方
 */
public class Classes {

  private long cId;
  private String cName;

  //表示含义 1个班级下有多个学生
  private List<Student> students; //学生集合  多的一方


  public long getCId() {
    return cId;
  }

  public void setCId(long cId) {
    this.cId = cId;
  }


  public String getCName() {
    return cName;
  }

  public void setCName(String cName) {
    this.cName = cName;
  }

  public List<Student> getStudents() {
    return students;
  }

  public void setStudents(List<Student> students) {
    this.students = students;
  }

  @Override
  public String toString() {
    return "Classes{" +
            "cId=" + cId +
            ", cName='" + cName + '\'' +
            ", students=" + students +
            '}';
  }
}
```
## 4.3.修改sql映射文件classMapper.xml
添加如下的SQL映射信息

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.bruceliu.mapper.ClassesMapper">

    <!--配置1对多 结果集映射-->
    <resultMap id="classMap" type="Classes">
        <!--主键-->
        <id property="cId" column="C_ID"/>
        <result property="cName" column="c_name"/>

        <!--配置一个包含关系 “有很多”关系 -->
        <collection property="students" ofType="Student">
            <id property="sId" column="s_id"/>
            <result property="sName" column="s_name"/>
            <result property="sAge" column="s_age"/>
            <result property="sEmail" column="s_email"/>
            <result property="classId" column="class_id"/>
        </collection>

    </resultMap>


    <select id="getById" resultMap="classMap">
        SELECT C.*,S.* FROM classes C INNER JOIN student S on C.c_id=S.class_id where C.c_id=#{classId}
    </select>


</mapper>

```
## 4.4. 编写单元测试类

```java
package com.bruceliu.test;

import com.bruceliu.bean.Classes;
import com.bruceliu.mapper.ClassesMapper;
import com.bruceliu.utils.MyBatisUtils;
import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author bruceliu
 * @create 2019-07-09 10:06
 * @description 测试1对多
 */
public class TestOne2Many {

    SqlSession session=null;
    ClassesMapper classesMapper=null;

    @Before
    public void init(){
        session = MyBatisUtils.getSession();
        classesMapper = session.getMapper(ClassesMapper.class);
    }

    @Test
    public void test1(){
        Classes c = classesMapper.getById(1L);
        System.out.println(c);
    }

    @After
    public void destory(){
        session.close();
    }


}
```
## 4.5. MyBatis一对多嵌套查询总结
MyBatis中使用collection标签来解决一对多的嵌套查询，ofType属性指定集合中元素的对象类型。

# 5. 多对多关联（重点）
## 5.1 需求
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190904175443864.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/201909041754495.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
## 5.2. 准备SQL语句
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190904175505552.png)
## 5.3 User实体类

```java
package com.bruceliu.bean;


import java.util.List;

public class User {

  private long uId;
  private String uName;
  private String uSex;
  private long uAge;

  private List<Role> roles; //一个用户下面多个角色

  public long getUId() {
    return uId;
  }

  public void setUId(long uId) {
    this.uId = uId;
  }


  public String getUName() {
    return uName;
  }

  public void setUName(String uName) {
    this.uName = uName;
  }


  public String getUSex() {
    return uSex;
  }

  public void setUSex(String uSex) {
    this.uSex = uSex;
  }


  public long getUAge() {
    return uAge;
  }

  public void setUAge(long uAge) {
    this.uAge = uAge;
  }

  public List<Role> getRoles() {
    return roles;
  }

  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }

  @Override
  public String toString() {
    return "User{" +
            "uId=" + uId +
            ", uName='" + uName + '\'' +
            ", uSex='" + uSex + '\'' +
            ", uAge=" + uAge +
            ", roles=" + roles +
            '}';
  }
}
```
## 5.4 Role实体类

```java
package com.bruceliu.bean;


public class Role {

  private long rId;
  private String rName;


  public long getRId() {
    return rId;
  }

  public void setRId(long rId) {
    this.rId = rId;
  }


  public String getRName() {
    return rName;
  }

  public void setRName(String rName) {
    this.rName = rName;
  }


  @Override
  public String toString() {
    return "Role{" +
            "rId=" + rId +
            ", rName='" + rName + '\'' +
            '}';
  }
}
```
## 5.5 UserMapper

```java
package com.bruceliu.mapper;

import com.bruceliu.bean.User;

/**
 * @author bruceliu
 * @create 2019-07-09 11:10
 * @description
 */
public interface UserMapper {

    //1.根据ID查询user(同时嵌套查询一下role集合信息)
    public User getUserByid(long uid);
}
```
## 5.6 UserMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >
<mapper namespace="com.bruceliu.mapper.UserMapper">

    <resultMap id="userMap" type="User">
        <id property="uId" column="u_id"/>
        <result property="uAge" column="u_age"/>
        <result property="uName" column="u_name"/>
        <result property="uSex" column="u_sex"/>

        <!--一个用户多个角色-->
        <collection property="roles" ofType="Role">
            <id property="rId" column="r_id"/>
            <result property="rName" column="r_name"/>
        </collection>

    </resultMap>
    
    
    <select id="getUserByid" resultMap="userMap">
        SELECT * FROM `user` U INNER JOIN role_user RU ON U.u_id=RU.uu__id INNER JOIN role R ON RU.rr_id=R.r_id
where U.u_id=#{uid}
    </select>

</mapper>
```
## 5.7 测试

```java
package com.bruceliu.test;

import com.bruceliu.bean.Student;
import com.bruceliu.bean.User;
import com.bruceliu.mapper.StudentMapper;
import com.bruceliu.mapper.UserMapper;
import com.bruceliu.utils.MyBatisUtils;
import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author bruceliu
 * @create 2019-07-09 11:20
 * @description
 */
public class TesMany2Many {

    SqlSession session=null;
    UserMapper userMapper=null;

    @Before
    public void init(){
        session = MyBatisUtils.getSession();
        userMapper = session.getMapper(UserMapper.class);
    }

    /**
     *   测试根据人查询所属的角色集合
     */
    @Test
    public void test1(){
        User user = userMapper.getUserByid(1);
        System.out.println(user);
    }

    @After
    public void destory(){
        session.close();
    }

}
```


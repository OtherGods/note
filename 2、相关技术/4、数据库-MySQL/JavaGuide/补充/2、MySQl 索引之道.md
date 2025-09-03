# 前言

SQL 写不好 加班少不了 日常工作中SQL 是必不可少的一项技术 但是很多人不会过多的去关注SQL问题 一是数据量小 二是没有意识到索引的重要性 本文主要是整理 SQL失效场景 如果里面的细节你都知道 那你一定是学习能力比较好的人 膜拜 写完这篇文章 我感觉自己之前知道的真的是 “目录” 没有明白其中的内容 如果你能跟着节奏看完文章 一定会有收获 至少我写完感觉思维通透很多 以后百分之九十的 SQl索引问题 和 面试这方面问题都能拿捏两

# 1、文章概要
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407252252761.png)

# 2、索引失效 整理

## 2.1、基础数据准备

准备一个数据表作为 数据演示 这里面一共 创建了三个索引

- 联合索引 `sname`, `s_code`, `address`
- 主键索引 `id`
- 普通索引 `height`
```sql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sname` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `s_code` int(100) NULL DEFAULT NULL,
  `address` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `height` double NULL DEFAULT NULL,
  `classid` int(11) NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `普通索引`(`height`) USING BTREE,
  INDEX `联合索引`(`sname`, `s_code`, `address`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES (1, '学生1', 1, '上海', 170, 1, '2022-11-02 20:44:14');
INSERT INTO `student` VALUES (2, '学生2', 2, '北京', 180, 2, '2022-11-02 20:44:16');
INSERT INTO `student` VALUES (3, '变成派大星', 3, '京东', 185, 3, '2022-11-02 20:44:19');
INSERT INTO `student` VALUES (4, '学生4', 4, '联通', 190, 4, '2022-11-02 20:44:25');
```

## 2.2、问题思考

上面的SQL 我们已经创建好基本的数据 在验证之前 先带着几个问题
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407252253632.png)

我们先从上往下进行验证

## 2.3、最左匹配原则

写在前面：我很早之前就听说过数据库的最左匹配原则，当时是通过各大博客论坛了解的，但是这些博客的局限性在于它们对最左匹配原则的描述就像一些数学定义一样，往往都是列出123点，满足这123点就能匹配上索引，否则就不能。 最左匹配原则就是指在联合索引中，如果你的 SQL 语句中用到了联合索引中的最左边的索引，那么这条 SQL 语句就可以利用这个联合索引去进行匹配，我们上面建立了联合索引 可以用来测试最左匹配原则 `sname`, `s_code`, `address`

请看下面SQL语句 进行思考 是否会走索引
```sql
-- 联合索引 sname,s_code,address

1、select create_time from student where sname = "变成派大星"  -- 会走索引吗？

2、select create_time from student where s_code = 1   -- 会走索引吗？

3、select create_time from student where address = "上海"  -- 会走索引吗？

4、select create_time from student where address = "上海" and s_code = 1 -- 会走索引吗？

5、select create_time from student where address = "上海" and sname = "变成派大星"  -- 会走索引吗？

6、select create_time from student where sname = "变成派大星" and address = "上海"  -- 会走索引吗？

7、select create_time from student where sname = "变成派大星" and s_code = 1 and address = "上海"  -- 会走索引吗？
```

凭你的经验 哪些会使用到索引呢 ？ 可以先思考一下 在心中记下数字

**走索引例子**

java

 代码解读

复制代码

`EXPLAIN  select create_time from student where sname = "变成派大星"  -- 会走���引吗？`

![图片.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d253165cfba04252aa4c4a219b697a81~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp?)

**未走索引例子**

java

 代码解读

复制代码

`EXPLAIN select create_time from student where address = "上海" and s_code = 1 -- 会走索引吗？`

走的全表扫描 rows = 4 ![图片.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9395730bd49645f1848278ff1d7d2467~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp?) 如果不知道`EXPLAIN` 是什么的 或者看不懂分析出来的数据的话 建议去看看另一篇文章[分析命令EXPLAIN超详解](https://juejin.cn/post/7161254854571065375 "https://juejin.cn/post/7161254854571065375")

如果你内心的答案没有全部说对就接着往下看

最左匹配原则顾名思义：最左优先，以最左边的为起点任何连续的索引都能匹配上。**同时遇到范围查询(>、<、between、like)就会停止匹配**。  
例如：s_code = 2 如果建立(`sname`, `s_code`)顺序的索引，是匹配不到(`sname`, `s_code`)索引的;

但是如果查询条件是sname = "变成派大星" and s_code = 2或者a=1(又或者是s_code = 2 and sname = "变成派大星" )就可以，**因为优化器会自动调整`sname`, `s_code`的顺序**。再比如sname = "变成派大星" and s_code > 1 and address = "上海" `address是用不到索引的`，因为s_code字段是一个范围查询，它之后的字段会停止匹配。

不带范围查询 索引使用类型 ![图片.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/62cf1fbb4d694a32b1374a7621259924~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp?)

带范围使用类型

![图片.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a8bf03bef225405b83de31f9ffcdd1af~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp?)

根据上一篇文章的讲解 可以明白 ref 和range的含义 级别还是相差很多的

![图片.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/dd7c90f9067344fdaa91724d0f6882f1~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp?)

### 2.3.1、思考

为什么左链接一定要遵循最左缀原则呢？

### 2.3.2、验证

看过一个比较好玩的回答

> 你可以认为联合索引是闯关游戏的设计  
> 例如你这个联合索引是state/city/zipCode  
> 那么state就是第一关 city是第二关， zipCode就是第三关  
> 你必须匹配了第一关，才能匹配第二关，匹配了第一关和第二关，才能匹配第三关

这样描述不算完全准确 但是确实是这种思想

要想理解联合索引的最左匹配原则，先来理解下索引的底层原理。索引的底层是一颗B+树，那么联合索引的底层也就是一颗B+树，只不过联合索引的B+树节点中存储的是键值。由于构建一棵B+树只能根据一个值来确定索引关系，所以数据库依赖联合索引最左的字段来构建 文字比较抽象 我们看一下

加入我们建立 A,B 联合索引 他们在底层储存是什么样子呢？

- 橙色代表字段 A
- 浅绿色 代表字段B

图解： 
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407252344537.png)

我们可以看出几个特点

- A 是有顺序的 1，1，2，2，3，4
- B 是没有顺序的 1，2，1，4，1，2 这个是散列的
- 如果A是等值的时候 B是有序的 例如 （1，1），（1，2） 这里的B有序的 （2，1）,(2,4) B 也是有序的

这里应该就能看出 如果没有A的支持 B的索引是散列的 不是连续的

再细致一点 我们重新创建一个表
```sql
DROP TABLE IF EXISTS `leftaffix`;

CREATE TABLE `leftaffix`  (

  `a` int(11) NOT NULL AUTO_INCREMENT,

  `b` int(11) NULL DEFAULT NULL,

  `c` int(11) NULL DEFAULT NULL,

  `d` int(11) NULL DEFAULT NULL,

  `e` varchar(11) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,

  PRIMARY KEY (`a`) USING BTREE,

  INDEX `联合索引`(`b`, `c`, `d`) USING BTREE

) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;
 
-- ----------------------------
-- Records of leftaffix
-- ----------------------------
INSERT INTO `leftaffix` VALUES (1, 1, 1, 1, '1');

INSERT INTO `leftaffix` VALUES (2, 2, 2, 2, '2');

INSERT INTO `leftaffix` VALUES (3, 3, 2, 2, '3');

INSERT INTO `leftaffix` VALUES (4, 3, 1, 1, '4');

INSERT INTO `leftaffix` VALUES (5, 2, 3, 5, '5');

INSERT INTO `leftaffix` VALUES (6, 6, 4, 4, '6');

INSERT INTO `leftaffix` VALUES (7, 8, 8, 8, '7');
SET FOREIGN_KEY_CHECKS = 1;
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407252345322.png)

> 在创建索引树的时候会对数据进行排序 根据最左缀原则 会先通过 B 进行排序 也就是 如果出现值相同就 根据 C 排序 如果 C相同就根据D 排序 排好顺序之后就是如下图：

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407252346585.png)

> 索引的生成就会根据图二的顺序进行生成 我们看一下 生成后的树状数据是什么样子

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407252347878.png)


> 解释一些这个树状图 首先根据图二的排序 我们知道顺序 是 1111a 2222b 所以 在第三层 我们可以看到 1111a 在第一层 2222b在第二层 因为 111 < 222 所以 111 进入第二层 然后得出第一层

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407252347595.png)

简化一下就是这个样子

但是这种顺序是相对的。这是因为MySQL创建联合索引的规则是首先会对联合索引的最左边`第一个字段排序`，在第一个字段的排序基础上，然后在对第二个字段进行排序。所以B=2这种查询条件没有办法利用索引。

<font color = "red" size = 5>我的理解：从多路平衡查找树中查询数据是以树有顺序为基础，按照大小进行查找，当字段在树中全局无序时（例如：index(a,b) 组合索引中，a全局有序，b在a相同的情况下局部有序），也就没办法查找了</font>

看到这里还可以明白一个道理 为什么我们建立索引的时候不推荐建立在经常改变的字段 因为这样的话我们的索引结构就要跟着你的改变而改动 所以很消耗性能

### 2.3.3、补充

评论区老哥的提示 最左缀原则可以通过跳跃扫描的方式打破 简单整理一下这方面的知识

这个是在 8.0 进行的优化

`MySQL8.0版本`开始增加了索引跳跃扫描的功能，当第一列索引的唯一值较少时，即使where条件没有第一列索引，查询的时候也可以用到联合索引。 比如我们使用的联合索引是 bcd 但是b中字段比较少 我们在使用联合索引的时候没有 使用 b 但是依然可以使用联合索引 **MySQL联合索引有时候遵循最左前缀匹配原则，有时候不遵循。**

### 2.3.4、小总结

前提 如果创建 b,c,d 联合索引面

- 如果 我where 后面的条件是`c = 1 and d = 1`为什么不能走索引呢 如果没有b的话 你查询的值相当于 `*11` 我们都知道`*`是所有的意思也就是我能匹配到所有的数据
- 如果 我 where 后面是 `b = 1 and d =1` 为什么会走索引呢？ 你等于查询的数据是 `1*1` 我可以通过前面 1 进行索引匹配 所以就可以走索引
- 最左缀匹配原则的最重要的就是 第一个字段

我们接着看下一个失效场景

## 2.4、select * （与失效无关）

### 2.4.1、思考

这里是我之前的一个思维误区 select * 不会导致索引失效 之前测试发现失效是因为where 后面的查询范围过大 导致索引失效 并不是Select * 引起的 但是为什么不推荐使用select *

### 2.4.2、解释

- 增加查询分析器解析成本。
- 增减字段容易与 resultMap 配置不一致。
- 无用字段增加网络 消耗，尤其是 text 类型的字段。  
    在阿里的开发手册中，大面的概括了上面几点。

在使用Select * 索引使用正常 
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281321244.png)


虽然走了索引但是 也不推荐这种写法 为什么呢？

首先我们在上一个验证中创建了联合索引 我们使用B=1 会走索引 但是 与直接查询索引字段不同 使用`SELECT*`,获取了不需要的数据，则首先通过辅助索引过滤数据，然后再通过聚集索引获取所有的列，这就多了一次b+树查询，速度必然会慢很多，减少使用select * 就是降低回表带来的损耗。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281324997.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281324885.png)

也就是 Select * 在一些情况下是会走索引的 如果不走索引就是 where 查询范围过大 导致MySQL 最优选择全表扫描了 并不是Select * 的问题

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281325845.png)

上图就是索引失效的情况

范围查找也不是一定会索引失效 下面情况就会索引生效就是 级别低 生效的原因是因为缩小了范围

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281326227.png)


### 2.4.3、小总结

- select * 与走不走索引无关
- 范围查找有概率索引失效但是在特定的情况下会生效 范围小就会使用 也可以理解为 返回结果集小就会使用索引
- mysql中连接查询的原理是先对驱动表进行查询操作，然后再用从驱动表得到的数据作为条件，逐条的到被驱动表进行查询。
- 每次驱动表加载一条数据到内存中，然后被驱动表所有的数据都需要往内存中加载一遍进行比较。效率很低，所以mysql中可以指定一个缓冲池的大小，缓冲池大的话可以同时加载多条驱动表的数据进行比较，放的数据条数越多性能io操作就越少，性能也就越好。所以，如果此时使用`select *` 放一些无用的列，只会白白的占用缓冲空间。浪费本可以提高性能的机会。
- 按照评论区老哥的说法 select * 不是造成索引失效的直接原因 大部分原因是 where 后边条件的问题 但是还是尽量少去使用select * 多少还是会有影响的

## 2.5、使用函数

使用在Select 后面使用函数可以使用索引 但是下面这种做法就不能

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281328932.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281328344.png)

因为索引保存的是索引字段的原始值，而不是经过函数计算后的值，自然就没办法走索引了。

不过，从 MySQL 8.0 开始，索引特性增加了函数索引，即可以针对函数计算后的值建立一个索引，也就是说该索引的值是函数计算后的值，所以就可以通过扫描索引来查询数据。

这种写法我没使用过 感觉情况比较少 也比较容易注意到这种写法

## 2.6、计算操作

这个情况和上面一样 之所以会导致索引失效是因为改变了索引原来的值 在树中找不到对应的数据只能全表扫描

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281329706.png)

因为索引保存的是索引字段的原始值，而不是 b - 1 表达式计算后的值，所以无法走索引，只能通过把索引字段的取值都取出来，然后依次进行表达式的计算来进行条件判断，因此采用的就是全表扫描的方式。

下面这种计算方式就会使用索引

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281331733.png)

Java比较熟悉的可能会有点疑问，这种对索引进行简单的表达式计算，在代码特殊处理下，应该是可以做到索引扫描的，比方将 b - 1 = 6 变成 b = 6 - 1。 是的，是能够实现，但是 MySQL 还是偷了这个懒，没有实现。

### 2.6.1、小总结

总而言之 言而总之 只要是影响到索引列的值 索引就是失效

## 2.7、Like %

这个真的是难受哦 因为经常使用这个 所以还是要小心点 在看为什么失效之前 我们先看一下 Like % 的解释

1. `%百分号通配符`: 表示任何字符出现任意次数(可以是0次).
2. `_下划线通配符`: 表示只能匹配单个字符,不能多也不能少,就是一个字符.
3. `like操作符`: LIKE作用是指示mysql后面的搜索模式是利用通配符而不是直接相等匹配进行比较.

**注意:** 
1. 如果在使用like操作符时,后面的没有使用通用匹配符效果是和=一致的,
```sql
SELECT * FROM products WHERE products.prod_name like '1000';
```

2. 匹配包含"Li"的记录(包括记录"Li") :
```sql
SELECT* FROM products WHERE products.prod_name like '%Li%';
```

3. 匹配以"Li"结尾的记录(包括记录"Li",不包括记录"Li ",也就是Li后面有空格的记录,这里需要注意)
```sql
SELECT * FROM products WHERE products.prod_name like '%Li';
```

在左不走 在右走

`右：` 虽然走 但是索引级别比较低主要是模糊查询 范围比较大 所以索引级别就比较低 
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281337634.png)

`左：` 这个范围非常大 所以没有使用索引的必要了 这个可能不是很好优化 还好不是一直拼接上面的
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281337560.png)

### 2.7.1、小总结

索引的时候和查询范围关系也很大 范围过大造成索引没有意义从而失效的情况也不少

## 2.8、使用Or导致索引失效

这个原因就更简单了

在 WHERE 子句中，如果在 OR 前的条件列是索引列，而在 OR 后的条件列不是索引列，那么索引会失效 举个例子，比如下面的查询语句，b 是主键，e 是普通列，从执行计划的结果看，是走了全表扫描。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281338970.png)
### 2.8.1、优化

这个的优化方式就是 在Or的时候两边都加上索引

就会使用索引 避免全表扫描 
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281338007.png)

## 2.9、in使用不当

首先使用In 不是一定会造成全表扫描的 **IN肯定会走索引，但是当IN的取值范围较大时会导致索引失效，走全表扫描**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281349464.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281349626.png)

in 在结果集 大于30%的时候索引失效

## 2.10、not in 和 In的失效场景相同

## 2.11、order By

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281350525.png)

这一个主要是Mysql 自身优化的问题 我们都知道OrderBy 是排序 那就代表我需要对数据进行排序 如果我走索引 索引是排好序的 但是我需要回表 消耗时间 另一种 我直接全表扫描排序 不用回表 也就是
- 走索引 + 回表
- 不走索引 直接全表扫描

Mysql 认为直接全表扫面的速度比 回表的速度快所以就直接走索引了 在Order By 的情况下 走全表扫描反而是更好的选择

## 2.12、子查询会走索引吗

答案是会 但是使用不好就不会

## 2.13、大总结

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281354333.png)


## 2.14、减少回表优化思路

**这个对于SQL有研究的人可能是比较了解的但是对于工作时长不久的会比较陌生的词语 但是这个是非常有意思 且重要的**

在这个索引问题上面还有一个细节的东西 其中印象比较深刻的是回表会造成效率下降 但是在我们日常工作中是比较常用单列索引 联合索引对于新手来说不是很常用 但是单列索引在一些情况下肯定不是最优解 例如 like % 问题 会造成索引问题 近期了解到一个 `ICP` 知识 我之前都没有关注过 不知道大家对这个了解多少 我这里就进行一些整理

首先我们ICP 全称是 `Index Condition Pushdown` 中文可以说成是索引下推 主要的作用解决数据查询回表的问题 但是前提是和联合索引进行使用 才能发挥出来功效 接下来不了解的小伙伴可以认真看一下这一点 个人感觉还是比较有意思的东西

### 2.14.1、回表问题

上面其实对于回表查询没有过多的解释 就再提一什么是回表查询

回表查询一般发生在非主键索引上面 需要进行两次树查询 所以效率会有所折扣 我们要想解决这个行为就可以使用 联合索引去优化

### 2.14.2、ICP 索引下推

这个是在MySQL 5.6 之后提供的特性 这个如果面试中问到 我们平常面试的时候 面试官都有喜欢问什么版本 增加了什么 如果问你 MySQL 5.6 之后增加什么优化 不知道大家都能说出什么 这个就是一个很加分点 你能说明白什么是索引下推 面试官会对你增加好感 至少说明你还是有点东西在身上的 不啰嗦了 开始研究

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281400872.png)

我们先看一下 5.6 之前 和 5.6 之后 查询流程会有什么变化

假设 我我们需要查询 `select * from table1 where b like '3%' and c = 3`

**5.6 之前**

- 先通过 联合索引 查询到 开头为 3 的数据 然后拿到主键（上图中青色块为主键）
- 然后通过主键去主键索引里面去回表查询 二级索引里面查询出来几个 3 开头的就回表几次

**5.6 之后**

- 先通过 二级索引 查询到开头为 3 的数据 然后 再找到 c = 3 的数据进行过滤 之后拿到主键
- 通过主键进行回表查询

上面都会进行回表查询但是 5.6 之前没有完全去利用 二级缓存进行数据过滤 如果 3 开头的数据非常多 那就要一直回表 但是 5.6 之后去利用后续索引字段进行查询

怎么说呢 就是为什么索引下推要和联合索引进行使用 普通所以没有 索引下推就是充分利用 联合索引的字段进过滤 尽量减少需要回表的数据 来增加查询效率 感觉思路是很简单的

对于Innodb 引擎的ICP 只适合 二级索引

`小细节：`

索引下推除了依赖 联合索引之外 还不能在子查询下面进行使用 存储函数也不能使用

怎么查看是否使用索引下推

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202407281403204.png)

看这个你可能有点印象但是 理论上不是很明白 但是我现在是明白了 还有很多知识点要补充啊 慢慢写

- 如果你是直接跳到这里 看看文章有多长 `建议收藏`
- 如果你一步步看到这里 感觉有点帮助 `赞赞来一个`
- 如果感觉文章有问题 建议评论区指出 `会修正`

周五愉快 文章完结🥰

持续更新SQL相关系列 可追更 不可催更


作者：糊涂码  
链接：[MySQl 索引之道](https://juejin.cn/post/7161964571853815822)
来源：稀土掘金  
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
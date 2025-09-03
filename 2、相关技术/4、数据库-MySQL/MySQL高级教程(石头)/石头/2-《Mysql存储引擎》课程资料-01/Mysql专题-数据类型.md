

# 1.Mysql数据类型

主要包括以下五大类：

- **整数类型**：**BIT**、TINY INT、SMALL INT、MEDIUM INT、 **INT**、 BIG INT

  ![1608261858308](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-数据类型\1608261858308.png)

- **浮点数类型**：FLOAT、**DOUBLE**、DECIMAL

  ![1608261903931](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-数据类型\1608261903931.png)

- **字符串类型**：**CHAR、VARCHAR**、TINY TEXT、**TEXT**、MEDIUM TEXT、LONGTEXT、TINY BLOB、BLOB、MEDIUM BLOB、LONG BLOB

  ![1608261950988](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-数据类型\1608261950988.png)

- **日期类型**：**Date**、**DateTime**、TimeStamp、Time、Year

  ![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\Mysql专题-数据类型\20201217234238152.png)

- **其他数据类型**：BINARY、VARBINARY、ENUM、SET、Geometry、Point、MultiPoint、LineString、MultiLineString、Polygon、GeometryCollection等

# 2.CHAR 和 VARCHAR 的区别？

char是固定长度，varchar长度可变：

char(n) 和 varchar(n) 中括号中 n 代表字符的个数，并不代表字节个数，比如 **CHAR(30)** 就可以存储 30 个字符。

存储时，前者不管实际存储数据的长度，**直接按 char 规定的长度分配存储空间**；**而后者会根据实际存储的数据长度（而不是定义表结构时指定的varchar的长度）分配最终的存储空间**

**相同点：**

```java
1.char(n)，varchar(n)中的n都代表字符的个数，不是字节的个数，例如：一个中文和一个英文字符都是代表一个字符；
2.超过char，varchar最大长度n的限制后，字符串会被截断。
```
**不同点：**

```java
1.char不论实际存储的字符数都会占用n个字符的空间，而varchar只会占用实际字符应该占用的字节空间加1（实际长度length，0<=length<255）或加2（length>255）。因为varchar保存数据时除了要保存字符串之外还会加一个字节来记录长度（如果列声明长度大于255则使用两个字节来保存长度）。
2.能存储的最大空间限制不一样：char的存储上限为255字节。
3.char在存储时会截断尾部的空格（自动删除用户添加到该列对应值尾部的空格，无论是否超过长度），而varchar不会（如果空格+内容的长度超过表结构中指定的字符个数，那么只会删除超过长度的空格）。
```
**char是适合存储很短的、一般固定长度的字符串。例如，char非常适合存储密码的MD5值**，因为这是一个定长的值。对于非常短的列，char比varchar在存储空间上也更有效率。

```
name varchar(4)
sex char(1)
```



# 3.列的字符串类型可以是什么？

字符串类型是：SET、BLOB、ENUM、**CHAR、VARCHAR、TEXT、VARCHAR**

# 4.BLOB和TEXT有什么区别？

**BLOB**是一个二进制对象，可以容纳可变数量的数据。有四种类型的BLOB：TINYBLOB、BLOB、MEDIUMBLO和 LONGBLOB

**TEXT**是一个不区分大小写的BLOB。四种TEXT类型：TINYTEXT、TEXT、MEDIUMTEXT 和 LONGTEXT。

**BLOB** 保存**二进制数据**，TEXT **保存字符数据**。
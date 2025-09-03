写SQL语句的时候我们往往关注的是SQL的执行结果，但是是否真的关注了SQL的执行效率，是否注意了SQL的写法规范？

以下的干货分享是在实际开发过程中总结的，希望对大家有所帮助！

### 1. limit分页优化

当偏移量特别大时，limit效率会非常低。

**SELECT id FROM A LIMIT 1000,10   很快**

SELECT id FROM A LIMIT 90000,10 很慢

**方案一**：

```n1ql
n1ql
复制代码select id from A order by id limit 90000,10;
```

如果我们结合order by使用。很快，0.04秒就OK。 因为使用了id主键做索引！当然，是否能够使用索引还需要根据业务逻辑来定，这里只是为了提醒大家，在分页的时候还需谨慎使用！

**方案二**

```applescript
applescript
复制代码select id from A order by id  between 90000 and 90010;
```

### 2.利用limit 1 、top 1 取得一行

有些业务逻辑进行查询操作时(特别是在根据某一字段DESC,取最大一笔).可以使用limit 1 或者 top 1 来终止[数据库索引]继续扫描整个表或索引。

反例

```n1ql
n1ql
复制代码SELECT id FROM A LIKE 'abc%' 
```

正例

```n1ql
n1ql
复制代码SELECT id FROM A LIKE 'abc%' limit 1
```

### 3. 任何情况都不要用 select * from table ，用具体的字段列表替换"*"，不要返回用不到的字段,避免全盘扫描！

反例

```css
css
复制代码SELECT * FROM A
```

正例

```css
css
复制代码SELECT id FROM A 
```

### 4. 批量插入优化

反例

```pgsql
pgsql复制代码INSERT into person(name,age) values('A',24)
INSERT into person(name,age) values('B',24)
INSERT into person(name,age) values('C',24)
```

正例

```pgsql
pgsql
复制代码INSERT into person(name,age) values('A',24),('B',24),('C',24),
```

> sql语句的优化主要在于对索引的正确使用，而我们在开发中经常犯的错误便是对表进行全盘扫描，一来影响性能，而来耗费时间!

### 5.like语句的优化

反例

```pgsql
pgsql
复制代码SELECT id FROM A WHERE name like '%abc%'
```

由于abc前面用了“%”，因此该查询必然走全表查询,除非必要(模糊查询需要包含abc)，否则不要在关键词前加%

正例

```pgsql
pgsql
复制代码SELECT id FROM A WHERE name like 'abc%'
```

**实例**

mysql版本：5.7.26

```sql
sql
复制代码select nick_name from member where nick_name like '%小明%'
```



![img](https://p1-jj.byteimg.com/tos-cn-i-t2oaga2asx/gold-user-assets/2020/1/7/16f7db54ac687fda~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)



**like'%小明%'** 并未使用索引！

```sql
sql
复制代码select nick_name from member where nick_name like '小明%'
```



![img](https://p1-jj.byteimg.com/tos-cn-i-t2oaga2asx/gold-user-assets/2020/1/7/16f7db68e3935ae1~tplv-t2oaga2asx-zoom-in-crop-mark:3024:0:0:0.awebp)



**like'小明%'** 成功使用索引！

### 6.where子句使用or的优化

通常使用 union all 或 union 的方式替换“or”会得到更好的效果。where子句中使用了or关键字,索引将被放弃使用。

反例

```n1ql
n1ql
复制代码SELECT id FROM A WHERE num = 10 or num = 20
```

正例

```n1ql
n1ql
复制代码SELECT id FROM A WHERE num = 10 union all SELECT id FROM A WHERE num=20
```

### 7.where子句中使用 IS NULL 或 IS NOT NULL 的优化

反例

```n1ql
n1ql
复制代码SELECT id FROM A WHERE num IS NULL
```

在where子句中使用 IS NULL 或 IS NOT NULL 判断，索引将被放弃使用，会进行全表查询。

正例

**优化成num上设置默认值0**，确保表中num没有null值, IS NULL 的用法在实际业务场景下SQL使用率极高，我们应注意**避免全表扫描**

```n1ql
n1ql
复制代码SELECT id FROM A WHERE num=0
```

### 8.where子句中对字段进行表达式操作的优化

不要在where子句中的“=”左边进行函数、算数运算或其他表达式运算，否则系统将可能无法正确使用索引。

- **1**

```sql
sql
复制代码SELECT id FROM A WHERE datediff(day,createdate,'2019-11-30')=0 
```

优化为

```apache
apache
复制代码SELECT id FROM A WHERE createdate>='2019-11-30' and createdate<'2019-12-1'
```

- **2**

```sas
sas
复制代码SELECT id FROM A WHERE year(addate) <2020
```

优化为

```n1ql
n1ql
复制代码SELECT id FROM A where addate<'2020-01-01'
```

### 9.排序的索引问题 

mysql查询只是用一个索引，因此如果where子句中已经使用了索引的话，那么**order by中的列是不会使用索引**。因此数据库默认排序可以符合要求情况下不要使用排序操作；

尽量不要包含多个列的排序，如果需要最好给这些列创建**复合索引**。

### 10. 尽量用 union all 替换 union

union和union all的差异主要是**前者**需要将两个（或者多个）结果集合并后再进行唯一性过滤操作，这就会涉及到排序，增加大量的cpu运算，加大资源消耗及延迟。所以当我们可以确认不可能出现重复结果集或者不在乎重复结果集的时候，尽量使用union all而不是union

### 11.Inner join 和 left join、right join、子查询

- 第一：inner join内连接也叫等值连接是，left/rightjoin是外连接。

```pgsql
pgsql复制代码SELECT A.id,A.name,B.id,B.name FROM A LEFT JOIN B ON A.id =B.id;

SELECT A.id,A.name,B.id,B.name FROM A RIGHT JOIN ON B A.id= B.id;

SELECT A.id,A.name,B.id,B.name FROM A INNER JOIN ON A.id =B.id;
```

经过来之多方面的证实 inner join性能比较快，因为inner join是等值连接，或许返回的行数比较少。但是我们要记得有些语句隐形的用到了等值连接，如：

**SELECT A.id,A.name,B.id,B.name FROM A,B WHERE A.id = B.id;**

**推荐：能用inner join连接尽量使用inner join连接**

- 第二：子查询的性能又比外连接性能慢，尽量用外连接来替换子查询。

反例

mysql是先对外表A执行全表查询，然后根据uuid逐次执行子查询，如果外层表是一个很大的表，我们可以想象查询性能会表现比这个更加糟糕。

```n1ql
n1ql
复制代码Select* from A where exists (select * from B where id>=3000 and A.uuid=B.uuid);
```

**执行时间：2s左右**

正例

```n1ql
n1ql
复制代码Select* from A inner join B ON A.uuid=B.uuid where b.uuid>=3000;  这个语句执行测试不到一秒；
```

**执行时间：1s不到**

- 第三：使用JOIN时候，应该用小的结果驱动大的结果

left join 左边表结果尽量小，如果有条件应该放到左边先处理，right join同理反向。如：

反例

```n1ql
n1ql
复制代码Select * from A left join B A.id=B.ref_id where  A.id>10
```

正例

```n1ql
n1ql
复制代码select * from (select * from A wehre id >10) T1 left join B on T1.id=B.ref_id;
```

### 12.exist & in 优化

```n1ql
n1ql
复制代码SELECT * from A WHERE id in ( SELECT id from B )
n1ql
复制代码SELECT * from A WHERE id EXISTS ( SELECT 1 from A.id= B.id )
```

**分析:**

in 是在内存中遍历比较

exist 需要查询数据库，所以当B的数据量比较大时，exists效率优于in**

in()只执行一次，把B表中的所有id字段缓存起来，之后检查A表的id是否与B表中的id相等，如果id相等则将A表的记录加入到结果集中，直到遍历完A表的所有记录。

In 操作的流程原理如同一下代码

```abnf
abnf复制代码    List resultSet={};

    Array A=(select * from A);
    Array B=(select id from B);

    for(int i=0;i<A.length;i++) {
          for(int j=0;j<B.length;j++) {
          if(A[i].id==B[j].id) {
             resultSet.add(A[i]);
             break;
          }
       }
    }
    return resultSet;
```

可以看出，当B表数据较大时不适合使用in()，因为会把B表数据全部遍历一次

如：A表有10000条记录，B表有1000000条记录，那么最多有可能遍历10000*1000000次，效率很差。

再如：A表有10000条记录，B表有100条记录，那么最多有可能遍历10000*100次，遍历次数大大减少，效率大大提升。

<font color = "red">**结论：in()适合B表比A表数据小的情况**</font>

exist()会执行A.length()次，执行过程代码如下

```pgsql
pgsql复制代码
List resultSet={};
Array A=(select * from A);
for(int i=0;i<A.length;i++) {
    if(exists(A[i].id) {  //执行select 1 from B where B.id=A.id是否有记录返回
       resultSet.add(A[i]);
    }
}return resultSet;
```

当B表比A表数据大时适合使用exists()，因为它没有那么多遍历操作，只需要再执行一次查询就行。

如：A表有10000条记录，B表有1000000条记录，那么exists()会执行10000次去判断A表中的id是否与B表中的id相等。

如：A表有10000条记录，B表有100000000条记录，那么exists()还是执行10000次，因为它只执行A.length次，可见B表数据越多，越适合exists()发挥效果。

再如：A表有10000条记录，B表有100条记录，那么exists()还是执行10000次，还不如使用in()遍历10000*100次，因为in()是在内存里遍历比较，而exists()需要查询数据库，

我们都知道查询数据库所消耗的性能更高，而内存比较很快。  

<font color = "red">**结论：exists()适合B表比A表数据大的情况**</font>



作者：java酱
链接：https://juejin.cn/post/6844903573935882247
来源：稀土掘金
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
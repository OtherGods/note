# 典型回答

当我们在做查询的时候，经常会使用order by进行排序，而当我们只想查询部分数量的时候，也会使用limit进行限制条数。

但是，有的时候，同样的一条SQL语句，多次执行得到的结果可能是不同的，在实际表现中可能就是在分页查询中，一条记录可能出现在多个页中。

很多人会以为是因为查询过程中有别的事务新增或者删除了数据。其实也不一定，即使没有数据变化，这种情况也有可能会发生。

因为在MySQL的Limit的实际实现中，有以下描述：

> If multiple rows have identical values in the ORDER BY columns, the server is free to return those rows in any order, and may do so differently depending on the overall execution plan. In other words, the sort order of those rows is nondeterministic with respect to the nonordered columns.
> 
> https://dev.mysql.com/doc/refman/8.0/en/limit-optimization.html


也就是说，如果**ORDER BY的列中，多行具有相同的值，服务器可以自由地以任何顺序返回这些行，并且根据整体执行计划的不同可能会以不同的方式返回它们**。

所以，当我们**在进行Limit+order by的时候，一定要尽量避免使用可能重复的字段，如时间、名称等。而应该选择唯一性的字段，如主键ID，或者唯一性索引**。

> ChatGPT补充：
> 在 MySQL 中，当使用 `ORDER BY f1` 对包含 NULL 值的字段排序时，**多次执行相同查询的输出记录顺序可能会不同**，特别是当存在多个 NULL 值时。原因如下：
> 
> 核心原因：NULL 值的排序不确定性
> 1. **所有 NULL 值被视为相等**  
>     MySQL 将 NULL 视为相同的值，但不会保持它们之间的原始相对顺序
> 2. **缺少确定性排序依据**  
>     当多个行在排序字段上具有相同的值（包括多个 NULL）时，MySQL 不保证这些行的顺序稳定

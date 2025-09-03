## Redis的事务机制
### 1.什么是事务
事务是指一系列操作步骤，这一系列的操作步骤，要么完全地执行，要么完全地不执行。
比如微博中：A用户关注了B用户，那么A的关注人列表里面就会有B用户，B的粉丝列表
里面就会有A用户。

这个关注与被关注的过程是由一系列操作步骤构成：
（1）A用户添加到B的粉丝列表里面
（2）B用户添加到A的关注列表里面；
这两个步骤必须全部执行成功，整个逻辑才是正确的，否则就会产生数据的错误，比如A
用户的关注列表有B用户，但B的粉丝列表里没有A用户；
要保证一系列的操作都完全成功，提出了事务控制的概念。

Redis中的事务（transaction）是一组命令的集合，至少是两个或两个以上的命令，redis事
务保证这些命令被执行时中间不会被任何其他操作打断。

###2.事务操作的命令
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615222107920.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**multi -----------> beginTransaction 开启事务** 
语法： multi
作用：标记一个事务的开始。事务内的多条命令会按照先后顺序被放进一个队列当中。
返回值：总是返回 ok

**exec -------> commit提交**
语法：exec
作用：执行所有事务块内的命令
返回值：事务内的所有执行语句内容，事务被打断（影响）返回 nil

**discard------> Rollback 回滚**
语法：discard
作用：取消事务，放弃执行事务块内的所有命令
返回值：总是返回 ok

**watch**
语法：watch key [key ...]
作用：监视一个(或多个) key ，如果在事务执行之前这个(或这些) key 被其他命令所改动，
那么事务将被打断。
返回值：总是返回 ok

**unwatch**
语法：unwatch
作用：取消 WATCH 命令对所有 key 的监视。如果在执行 WATCH 命令之后， EXEC 命令
或 DISCARD 命令先被执行了的话，那么就不需要再执行 UNWATCH 了
返回值：总是返回 ok

### 3.事务的实现

**● 正常执行事务**
事务的执行步骤： 首先开启事务， 其次向事务队列中加入命令，最后执行事务提交

例 1：事务的执行:
1）multi ： 用 multi 命令告诉 Redis，接下来要执行的命令你先不要执行，而是把它们暂
时存起来 （开启事务）
2）sadd works john 第一条命令进入等待队列（命令入队）
3）sadd works rose 第二条命令进入等待队列（命令入队）
4）exce 告知 redis 执行前面发送的两条命令
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615222233277.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
查看works集合
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615222242937.png)
**● 事务执行exec之前，入队命令错误（语法错误；严重错误导致服务器不能正常工作（例如内存不足）），放弃事务。**

执行事务步骤：
1）MULTI正常命令
2）SETkeyvalue正常命令
3）INCR命令语法错误
4）EXEC无法执行事务，那么第一条正确的命令也不会执行，所以key的值不会设置成
功
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615222315635.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
结论：事务执行exec之前，入队命令错误，事务终止，取消，不执行。

**●  事务执行exec命令后，执行队列命令，命令执行错误，事务提交**
执行步骤：
1）MULTI正常命令
2）SET username zhangsan正常命令
3）lpop username正常命令，语法没有错误，执行命令时才会有错误。
4）EXEC正常执行,发现错误可以在事务提交前放弃事务，执行discard.
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615222400392.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
结论：在 exec 执行后的所产生的错误， 即使事务中有某个/某些命令在执行时产生了错误，
事务中的其他命令仍然会继续执行。Redis 在事务失败时不进行回滚，而是继续执行余下的命令。

Redis 这种设计原则是：Redis 命令只会因为错误的语法而失败（这些问题不能在入队时发
现），或是命令用在了错误类型的键上面，失败的命令并不是 Redis 导致，而是由编程错误
造成的，这样错误应该在开发的过程中被发现，生产环境中不应出现语法的错误。就是在程
序的运行环境中不应该出现语法的错误。而 Redis 能够保证正确的命令一定会被执行。再者不需要对回滚进行支持，所以 Redis 的内部可以保持简单且快速。

**●  放弃事务**
执行步骤：
1) MULTI 开启事务
2) SET age 25 命令入队
3) SET age 30 命令入队
4) DISCARD 放弃事务，则命令队列不会被执行
例 1：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615222435793.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

**●  Redis的watch机制**

**A、Redis的WATCH机制**
WATCH机制原理：
WATCH机制：使用WATCH监视一个或多个key,跟踪key的value修改情况，如果有
key的value值在事务EXEC执行之前被修改了，整个事务被取消。EXEC返回提示信息，表
示事务已经失败。
WATCH机制使的事务EXEC变的有条件，事务只有在被WATCH的key没有修改的前提下
才能执行。不满足条件，事务被取消。使用WATCH监视了一个带过期时间的键，那么即使
这个键过期了，事务仍然可以正常执行
大多数情况下，不同的客户端会访问不同的键，相互同时竞争同一key的情况一般都
很少，乐观锁能够以很好的性能解决数据冲突的问题。

**B、何时取消key的监视（WATCH）？**
①WATCH命令可以被调用多次。对键的监视从WATCH执行之后开始生效，直到调
用EXEC为止。不管事务是否成功执行，对所有键的监视都会被取消。
②当客户端断开连接时，该客户端对键的监视也会被取消。
③UNWATCH命令可以手动取消对所有键的监视

**C、 WATCH 的事例**
执行步骤：
首先启动 redis-server , 在开启两个客户端连接。 分别叫 A 客户端 和 B 客户端。
启动 Redis 服务器

A 客户端：WATCH 某个 key, 同时执行事务
B 客户端：对 A 客户端 WATCH 的 key 修改其 value 值

1） 在 A 客户端设置 key : str.lp 登录人数为 10
2） 在 A 客户端监视 key : str.lp
3） 在 A 客户端开启事务 multi
4） 在 A 客户端修改 str.lp 的值为 11
5） 在 B 客户端修改 str.lp 的值为 15
6） 在 A 客户端执行事务 exec
7） 在 A 客户端查看 str.lp 值，A 客户端执行的事务没有提交，因为 WATCH 的 str.lp 的值已
经被修改了， 所有放弃事务。

**例 1：乐观锁**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615222553107.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

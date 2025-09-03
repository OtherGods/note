## 第四章 Redis基本操作命令
#### 1.基本操作命令
手册地址：
redis 英文版命令大全：https://redis.io/commands
redis 中文版命令大全：http://redisdoc.com/

redis 默认为 16 个库 (在 redis.conf 文件可配置，该文件很重要，后续很多操作都是这个配
置文件) redis 默认自动使用 0 号库。

**A、沟通命令，查看状态**
redis >ping 返回 PONG
解释：输入 ping，redis 给我们返回 PONG，表示 redis 服务运行正常
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615091624655.png)
**B、查看 redis 服务器的统计信息：info**
语法：info [section]
作用：以一种易于解释且易于阅读的格式，返回关于 Redis 服务器的各种信息和统计数值。
section 用来返回指定部分的统计信息。 section 的值：server , clients ，
memory 等等。不加 section 返回全部统计信息
返回值：指定 section 的统计信息或全部信息

例 1：统计 server 的信息
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615091726871.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 2：统计全部信息
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615091754920.png)
**C、查看当前数据库中 key 的数目：dbsize**
语法：dbsize
作用：返回当前数据库的 key 的数量。
返回值：数字，key 的数量

例：先查索引 5 的 key 个数， 再查 0 库的 key 个数
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615091833252.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**D、redis 默认使用 16 个库**
Redis 默认使用 16 个库，从 0 到 15。 对数据库个数的修改，在 redis.conf 文件中 databases 16
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615091915118.png)
**E、切换库命令：select db**
使用其他数据库，命令是 select index
例 1： select 5
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019061509200187.png)
 **F、config get parameter 获得 redis 的配置值**
语法：config get parameter
作用：获取运行中 Redis 服务器的配置参数， 获取全部配置可以使用 * 。参数信息来自
redis.conf 文件的内容。

例 1：获取数据库个数 config get databases
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615092441271.png)
例 2：获取端口号 config get port
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615092503744.png)
例 3：获取所有配置参数 config get *
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615092531702.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
 **F、删除所有库的数据：flushall**
 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615092749143.png)
**G、删除当前库的数据：flushdb**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615092823419.png)
 **H、redis 自带的客户端退出当前 redis 连接: exit 或 quit**
 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615092853316.png)
 

#### 2.Redis 的 Key 的操作命令
 **A、keys**
语法：keys pattern
作用：查找所有符合模式 pattern 的 key. pattern 可以使用通配符。
通配符：
● * ：表示 0-多个字符 ，例如：keys * 查询所有的 key。
●  ？：表示单个字符，例如：wo?d , 匹配 word , wood
●  [] ：表示选择[]内的字符，例如 wo[or]d, 匹配 word, wood, 不匹配 wold

例 1：显示所有的 key
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615093257631.png)
例 2：使用 * 表示 0 或多个字符
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615093318617.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 3：使用 ？ 表示单个字符
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615093338768.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**B、 exists**
语法：exists key [key…]
作用：判断 key 是否存在
返回值：整数，存在 key 返回 1，其他返回 0. 使用多个 key，返回存在的 key 的数量。

例 1：检查指定 key 是否存在
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615093407320.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 2：检查多个 key
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615093431876.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**C、 expire**
语法：expire key seconds
作用：设置 key 的生存时间，超过时间，key 自动删除。单位是秒。
返回值：设置成功返回数字 1， 其他情况是 0 。

例 1： 设置红灯的倒计时是 5 秒
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615093457981.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**D、ttl**
语法：ttl key
作用：以秒为单位，返回 key 的剩余生存时间（ttl: time to live）
返回值：
● -1 ：没有设置 key 的生存时间， key 永不过期。
●  -2 ：key 不存在
● 数字：key 的剩余时间，秒为单位

例 1：设置 redlight 的过期时间是 10， 查看剩余时间
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615093541857.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**E、 type**
语法：type key
作用：查看 key 所存储值的数据类型
返回值：字符串表示的数据类型
● none (key 不存在)
● string (字符串)
● list (列表)
● set (集合)
● zset (有序集)
● hash (哈希表)

例 1：查看存储字符串的 key ：wood
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019061509364059.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 2：查看不存在的 key
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615093658805.png)
**F、 del**
语法：del key [key…]
作用：删除存在的 key ，不存在的 key 忽略。
返回值：数字，删除的 key 的数量。

例 1：删除指定的 key
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019061509372373.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**E、 rename**
语法：rename key
作用：修改key的名字
返回值：修改之后的状态
```java
redis 127.0.0.1:6379[1]> keys *
1) "age"
redis 127.0.0.1:6379[1]> rename age age_new
OK
redis 127.0.0.1:6379[1]> keys *
1) "age_new"
redis 127.0.0.1:6379[1]>
```





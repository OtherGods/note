### 第五章-Redis数据类型
#### 1.数据类型概述 
Redis中存储数据是通过key-value存储的，对于value的类型有以下几种：
- **字符串类型 string**
字符串类型是 Redis 中最基本的数据类型，它能存储任何形式的字符串，包括二进制
数据，序列化后的数据，JSON 化的对象甚至是一张图片。最大 512M。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613124302949.png)
- **哈希类型 hash**
Redis hash 是一个 string 类型的 field 和 value 的映射表，hash 特别适合用于存储对象。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613124331895.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
- **列表类型 list**
Redis 列表是简单的字符串列表，按照插入顺序排序。你可以添加一个元素到列表的头
部（左边）或者尾部（右边）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613124414911.png)
- **集合类型 set**
Redis 的 Set 是 string 类型的无序集合，集合成员是唯一的，即集合中不能出现重复的数
据.
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613124453295.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
- **有序集合类型 zset （sorted set)**
Redis 有序集合 zset 和集合 set 一样也是 string 类型元素的集合，且不允许重复的成员。
不同的是 zset 的每个元素都会关联一个分数（分数可以重复），redis 通过分数来为集合中的
成员进行从小到大的排序。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190613124538485.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
#### 2.Redis 数据类型操作命令
##### 2.1 字符串类型（string）
字符串类型是 Redis 中最基本的数据类型，它能存储任何形式的字符串，包括二进制数
据，序列化后的数据，JSON 化的对象甚至是一张图片。
###### 2.1.1 基本操作命令
**A、set**
将字符串值 value 设置到 key 中
语法：set key value
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614112746693.png)
查看已经插入的 key
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614112810230.png)
向已经存在的 key 设置新的 value，会覆盖原来的值
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614112838575.png)
**B、 get**
获取 key 中设置的字符串值
语法： get key
例如：获取 username 这个 key 对应的 value
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614112907217.png)

**C、 incr**
将 key 中储存的数字值加 1，如果 key 不存在，则 key 的值先被初始化为 0 再执行
incr 操作（只能对数字类型的数据操作）
语法：incr key
例 1：操作key,值增加 1
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614113032192.png)
例 2：对非数字的值操作是不行的
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614113050400.png)
**D、 decr**
将 key 中储存的数字值减1，如果 key 不存在，则么 key 的值先被初始化为 0 再执
行 decr 操作（只能对数字类型的数据操作）
语法：decr key
例1：不存在的key，初值为0，再减 1 。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614113128138.png)
例2：对存在的数字值的 key ，减 1 。
先执行 incr index ,增加到 3
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614113149641.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
incr ，decr 在实现关注人数上，文章的点击数上。

**E、 append**
语法：append key value
说明：如果 key 存在， 则将 value 追加到 key 原来旧值的末尾
如果 key 不存在， 则将 key 设置值为 value
返回值：追加字符串之后的总长度
例 1：追加内容到存在的 key
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614113248237.png)
例 2：追加到不存在的 key，同 set key value
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019061411331150.png)
###### 2.1.2 其他操作命令
**A、 strlen**
语法：strlen key
说明：返回 key 所储存的字符串值的长度
返回值：
①：如果key存在，返回字符串值的长度
②：key不存在，返回0
例 1：计算存在 key 的字符串长度
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614113429361.png)
设置中文 set k4 中文长度 ， 按字符个数计算

例 2：计算不存在的 key
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019061411350912.png)

**B、 getrange**
语法：getrange key start end
作用：获取 key 中字符串值从 start 开始 到 end 结束 的子字符串,包括 start 和 end, 负数
表示从字符串的末尾开始， -1 表示最后一个字符
返回值：截取的子字符串。
使用的字符串 key: school, value: bejingzhiyejishuxueyuan


例 1: 截取从 2 到 5 的字符
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614114306501.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
 位置是从0开始计算
 
例 2：从字符串尾部截取，start ,end 是负数，最后一位是 -1
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614114513429.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 3：超出字符串范围的截取 ，获取合理的子串
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614114554589.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**C、 setrange**
语法：setrange key offset value
说明：用 value 覆盖（替换）key 的存储的值从 offset 开始,不存在的 key 做空白字符串。
返回值：修改后的字符串的长度
例 1：替换给定的字符串
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614114629982.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 2：设置不存在的 key
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614114715538.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**D、mset**
语法：mset key value [key value…]
说明：同时设置一个或多个 key-value 对
返回值： OK
例 1：一次设置多个 key， value
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614114742555.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**E、 mget**
语法：mget key [key …]
作用：获取所有(一个或多个)给定 key 的值
返回值：包含所有 key 的列表

例 1：返回多个 key 的存储值
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614114809192.png)
例 2：返回不存在的 key
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614114829496.png)
###### 2.1.3 应用-自增主键
商品编号、订单号采用string的递增数字特性生成。

定义商品编号key：items:id

    192.168.101.3:7003> INCR items:id
    (integer) 2
    192.168.101.3:7003> INCR items:id
    (integer) 3
   
##### 2.2 哈希类型 hash
###### 2.2.1 使用string的问题
假设有User对象以JSON序列化的形式存储到Redis中，User对象有id，username、password、age、name等属性，存储的过程如下： 
保存、更新： User对象 →json(string) →redis 

如果在业务上只是更新age属性，其他的属性并不做更新我应该怎么做呢？ 如果仍然采用上边的方法在传输、处理时会造成资源浪费，下边讲的hash可以很好的解决这个问题。
User “{“username”:”gyf”,”age”:”80”}”
###### 2.2.2 hash介绍
hash叫散列类型，它提供了字段和字段值的映射。字段值只能是字符串类型，不支持散列类型、集合类型等其它类型。如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614115341982.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
##### 2.2.3 基本操作命令
**A、hset**
语法：hset hash 表的 key field value
作用：将哈希表 key 中的域 field 的值设为 value ，如果 key 不存在，则新建 hash 表，执
行赋值，如果有 field ,则覆盖值。
返回值：
①如果 field 是 hash 表中新 field，且设置值成功，返回 1
②如果 field 已经存在，旧值覆盖新值，返回 0

例 1：新的 field
 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614115458202.png)
 例 2：覆盖旧的的 field
 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614115521154.png)
**B、 hget**
语法：hget key field
作用：获取哈希表 key 中给定域 field 的值
返回值：field 域的值，如果 key 不存在或者 field 不存在返回 nil
例 1：获取存在 key 值的某个域的值
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614115959240.png)
例 2：获取不存在的 field
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614120128954.png)
**C、 hmset**
语法：hmset key field value [field value…]
说明：同时将多个 field-value (域-值)设置到哈希表 key 中，此命令会覆盖已经存在的 field，
hash 表 key 不存在，创建空的 hash 表，执行 hmset.
返回值：设置成功返回 ok， 如果失败返回一个错误

例 1：同时设置多个 field-value
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614120158583.png)
使用 redis-desktop-manager 工具查看 hash 表 website 的数据结构
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614120226249.png)
例 2：key 类型不是 hash,产生错误
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614120256819.png)
**D、hmget**
语法：hmget key field [field…]
作用:获取哈希表 key 中一个或多个给定域的值
返回值：返回和 field 顺序对应的值，如果 field 不存在，返回 nil

例 1：获取多个 field 的值
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019061412034990.png)

E、 hgetall
语法：hgetall key
作用：获取哈希表 key 中所有的域和值
返回值：以列表形式返回 hash 中域和域的值 ，key 不存在，返回空 hash

例 1：返回 key 对应的所有域和值
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614120445653.png)
例 2：不存在的 key，返回空列表
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614120509543.png)
**F、 hdel**
语法：hdel key field [field…]
作用：删除哈希表 key 中的一个或多个指定域 field，不存在 field 直接忽略
返回值：成功删除的 field 的数量

例 1：删除指定的 field
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614120731399.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
##### 2.2.4 其他操作命令
**A、hlen**
语法：hlen key
作用：获取哈希表 key 中域 field 的个数
返回值：数值，field 的个数。key 不存在返回 0.

例 1：获取指定 key 域的个数
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614120911772.png)
例 2：不存在的 key，返回 0
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614120935122.png)
**B、 hkeys**
语法：hkeys key
作用：查看哈希表 key 中的所有 field 域
返回值：包含所有 field 的列表，key 不存在返回空列表

例 1：查看 website 所有的域名称
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614121003168.png)

**C、 hvals**
语法：hvals key
作用：返回哈希表 中所有域的值
返回值：包含哈希表所有域值的列表，key 不存在返回空列表

例 1：显示 website 哈希表所有域的值
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614121032610.png)

**D、hexists**
语法：hexists key field
作用：查看哈希表 key 中，给定域 field 是否存在
返回值：如果 field 存在，返回 1， 其他返回 0

例 1：查看存在 key 中 field 域是否存在
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614121058331.png)

###### 2.2.5 应用-存储商品信息
商品字段
【商品id、商品名称、商品描述、商品库存、商品好评】

定义商品信息的key
商品1001的信息在 Redis中的key为：[items:1001]

存储商品信息

    192.168.101.3:7003> HMSET items:1001 id 3 name apple price 999.9
    OK
   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614121358926.png)
获取商品信息

    192.168.101.3:7003> HGET items:1001 id
    "3"
    192.168.101.3:7003> HGETALL items:1001
    1) "id"
    2) "3"
    3) "name"
    4) "apple"
    5) "price"
    6) "999.9"

##### 2.3 列表类型（list）
###### 2.3.1 ArrayList与LinkedList的区别
ArrayList使用数组方式存储数据，所以根据索引查询数据速度快，而新增或者删除元素时需要设计到位移操作，所以比较慢。 

LinkedList使用双向链表方式存储数据，每个元素都记录前后元素的指针，所以插入、删除数据时只是更改前后元素的指针指向即可，速度非常快。然后通过下标查询元素时需要从头开始索引，所以比较慢，但是如果查询前几个元素或后几个元素速度比较快。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614121557539.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614121608926.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
###### 2.3.2 list类型介绍
列表类型（list）可以存储一个有序的字符串列表，常用的操作是向列表两端添加元素，或者获得列表的某一个片段。

列表类型内部是使用双向链表（double linked list）实现的，所以向列表两端添加元素的时间复杂度为0(1)，获取越接近两端的元素速度就越快。这意味着即使是一个有几千万个元素的列表，获取头部或尾部的10条记录也是极快的。

###### 2.3.3 基本命令
**A、lpush**
语法：lpush key value [value…]
作用：将一个或多个值 value 插入到列表 key 的表头（最左边），从左边开始加入值，从左
到右的顺序依次插入到表头
返回值：数字，新列表的长度

例 1：将 a,b,c 插入到 mylist 列表类型
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614122206309.png)
在 redis-desktop-manager 显示
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614122226308.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
插入图示：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019061412224842.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 2：插入重复值到 list 列表类型
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614122309101.png)
在 redis-desktop-manager 显示
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614122328558.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**B、 rpush**
语法：rpush key value [value…]
作用：将一个或多个值 value 插入到列表 key 的表尾（最右边），各个 value 值按从左到右
的顺序依次插入到表尾
返回值：数字，新列表的长度

例 1：插入多个值到列表
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614122355359.png)
在 redis-desktop-manager 显示：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614122419697.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**C、 lrange**
语法：lrange key start stop
作用：获取列表 key 中指定区间内的元素，0 表示列表的第一个元素，以 1 表示列表的第
二个元素；start , stop 是列表的下标值，也可以负数的下标， -1 表示列表的最后一
个元素， -2 表示列表的倒数第二个元素，以此类推。 start ，stop 超出列表的范围
不会出现错误。
返回值：指定区间的列表

例 1：返回列表的全部内容
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614122502256.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 2：显示列表中第 2 个元素，下标从 0 开始
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614122833787.png)
**D、lpop**
语法：lpop key
作用：移除并返回列表 key 头部第一个元素，即列表左侧的第一个下标值。相当于栈（stack）
返回值：列表左侧第一个下标值； 列表 key 不存在，返回 nil

例 1：取出列表的左侧第一个下标值
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614122907245.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

**E、 rpop**
语法：rpop key
作用：移除并返回 key 的尾部元素
返回值：列表的尾部元素；key 不存在返回 nil

例 1：移除列表的尾部元素
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614122933604.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

**F、 lindex**
语法：lindex key index
作用：获取列表 key 中下标为指定 index 的元素，列表元素不删除，只是查询。0 表示列
表的第一个元素，以 1 表示列表的第二个元素；start , stop 是列表的下标值，也可以负数的下标， -1 表示列表的最后一个元素， -2 表示列表的倒数第二个元素，以此类推。
返回值：指定下标的元素；index 不在列表范围，返回 nil

例 1：返回下标是 1 的元素
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614123438142.png)
例 2：不存在的下标
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019061412350430.png)

**G、llen**
语法：llen key
作用：获取列表 key 的长度
返回值：数值，列表的长度； key 不存在返回 0

例 1：显示存在 key 的列表元素的个数
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614123541403.png)
###### 2.3.4 其他命令
**A、lrem**
语法：lrem key count value
作用：根据参数 count 的值，移除列表中与参数 value 相等的元素， count >0 ，从列表的
左侧向右开始移除； count < 0 从列表的尾部开始移除；count = 0 移除表中所有
与 value 相等的值。
返回值：数值，移除的元素个数

例 1：删除 2 个相同的列表元素
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614232311636.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

例 2：删除列表中所有的指定元素，删除所有的 java
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614232352384.png)

**B、 ltrim**
语法：ltrim key start stop
作用：删除指定区域外的元素，比如 LTRIM list 0 2 ，表示只保留列表 list 的前三个元素，
其余元素全部删除。0 表示列表的第一个元素，以 1 表示列表的第二个元素；start , 
stop 是列表的下标值，也可以负数的下标， -1 表示列表的最后一个元素， -2 表示
列表的倒数第二个元素，以此类推。
返回值：执行成功返回 ok

例 1：保留列表的前三个元素
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614232428800.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

**C、 lset**
语法：lset key index value
作用：将列表 key 下标为 index 的元素的值设置为 value。
返回值：设置成功返回 ok ; key 不存在或者 index 超出范围返回错误信息

例 1：设置下标 2 的 value 为“c”。
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019061423251696.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**D、linsert**
语法：linsert key BEFORE|ALFTER pivot value
作用：将值 value 插入到列表 key 当中位于值 pivot 之前或之后的位置。key 不存在，pivot
不在列表中，不执行任何操作。
返回值：命令执行成功，返回新列表的长度。没有找到 pivot 返回 -1， key 不存在返回 0。

例 1：修改列表 arch，在值 dao 之前加入 service
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614232548108.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 2：操作不存在的 pivot
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614232610220.png)
###### 2.3.5 应用-商品评论列表
思路：
在Redis中创建商品评论列表
用户发布商品评论，将评论信息转成json存储到list中。
用户在页面查询评论列表，从redis中取出json数据展示到页面。

定义商品评论列表key：
商品编号为1001的商品评论key【items: comment:1001】

    192.168.101.3:7001> LPUSH items:comment:1001 '{"id":1,"name":"商品不错，很好！！","date":1430295077289}'

##### 2.4 集合类型 set
###### 2.4.1 集合类型Set介绍
集合中的数据是不重复且没有顺序。
集合类型和列表类型的对比：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019061423301178.png)
	集合类型的常用操作是向集合中加入或删除元素、判断某个元素是否存在等，由于集合类型的Redis内部是使用值为空的散列表实现，所有这些操作的时间复杂度都为0(1)。 
Redis还提供了多个集合之间的交集、并集、差集的运算。

###### 2.4.2 基本命令
**A、sadd**
语法：sadd key member [member…]
作用：将一个或多个 member 元素加入到集合 key 当中，已经存在于集合的 member 元
素将被忽略，不会再加入。
返回值：加入到集合的新元素的个数。不包括被忽略的元素。

例 1：添加单个元素
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233239837.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 2：添加多个元素
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233305740.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**B、 smembers**
语法：smembers key
作用：获取集合 key 中的所有成员元素，不存在的 key 视为空集合

例 1：查看集合的所有元素
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233333802.png)
例 2：查看不存在的集合
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233355614.png)

**C、 sismember**
语法：sismember key member
作用：判断 member 元素是否是集合 key 的成员
返回值：member 是集合成员返回 1，其他返回 0 。

例 1：检查元素是否存在集合中
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233501316.png)

**D、scard**
语法：scard key
作用：获取集合里面的元素个数
返回值：数字，key 的元素个数。 其他情况返回 0 。

例 1：统计集合的大小
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233533174.png)
例 2：统计不存在的 key
![在这里插入图片描述](https://img-blog.csdnimg.cn/201906142335526.png)
**E、 srem**
语法：srem key member [member…]
作用：删除集合 key 中的一个或多个 member 元素，不存在的元素被忽略。
返回值：数字，成功删除的元素个数，不包括被忽略的元素。

例 1：删除存在的一个元素，返回数字 1
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233621411.png)
例 2：删除不存在的元素
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233654246.png)
###### 2.4.3 其他命令
**A、srandmember**
语法：srandmember key [count]
作用：只提供 key，随机返回集合中一个元素，元素不删除，依然在集合中；提供了 count
时，count 正数, 返回包含 count 个数元素的集合， 集合元素各不相同。count 是负
数，返回一个 count 绝对值的长度的集合， 集合中元素可能会重复多次。
返回值：一个元素；多个元素的集合

例 1：随机显示集合的一个元素
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233737321.png)
例 2：使用 count 参数， count 是正数
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233810364.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 3：使用 count 参数，count 是负数
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233835752.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**B、 spop**
语法：spop key [count]
作用：随机从集合中删除一个元素, count 是删除的元素个数。
返回值：被删除的元素，key 不存在或空集合返回 nil

例如 1：随机从集合删除一个元素
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233904985.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 2：随机删除指定个数的元素
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614233934734.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
###### 2.4.3 set命令运算
- 集合的差集运算 A-B
属于A并且不属于B的元素构成的集合。 
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614234159832.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614234217999.png)![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614234219620.png)
语法：SDIFF key [key ...]
```java
127.0.0.1:6379> sadd setA 1 2 3
(integer) 3
127.0.0.1:6379> sadd setB 2 3 4
(integer) 3
127.0.0.1:6379> sdiff setA setB 
1) "1"
127.0.0.1:6379> sdiff setB setA 
1) "4"
```
- 集合的交集运算 A ∩ B
属于A且属于B的元素构成的集合。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614234331411.png)
语法：SINTER key [key ...]
```java
127.0.0.1:6379> sinter setA setB 
1) "2"
2) "3"
```
- 集合的并集运算 A ∪ B
属于A或者属于B的元素构成的集合
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019061423441233.png)
语法：SUNION key [key ...]
```java
127.0.0.1:6379> sunion setA setB
1) "1"
2) "2"
3) "3"
4) "4"
```
##### 2.5 有序集合类型 zset (sorted set)
###### 2.5.1 sorted set介绍
在集合类型的基础上，有序集合类型为集合中的每个元素都关联一个分数，这使得我们不仅可以完成插入、删除和判断元素是否存在在集合中，还能够获得分数最高或最低的前N个元素、获取指定分数范围内的元素等与分数有关的操作。 

在某些方面有序集合和列表类型有些相似。 
1、二者都是有序的。 
2、二者都可以获得某一范围的元素。 
但是，二者有着很大区别： 
1、列表类型是通过链表实现的，获取靠近两端的数据速度极快，而当元素增多后，访问中间数据的速度会变慢。 
2、有序集合类型使用散列表实现，所有即使读取位于中间部分的数据也很快。 
3、列表中不能简单的调整某个元素的位置，但是有序集合可以（通过更改分数实现） 
4、有序集合要比列表类型更耗内存。
###### 2.5.2 基本命令
**A、zadd**
语法：zadd key score member [score member…]
作用：将一个或多个 member 元素及其 score 值加入到有序集合 key 中，如果 member
存在集合中，则更新值；score 可以是整数或浮点数
返回值：数字，新添加的元素个数

例 1：创建保存学生成绩的集合
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614234708129.png)
例 2：使用浮点数作为 score
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614234741721.png)
**B、 zrange**
语法：zrange key start stop [WITHSCORES]
作用：查询有序集合，指定区间的内的元素。集合成员按 score 值从小到大来排序。 start，
stop 都是从 0 开始。0 是第一个元素，1 是第二个元素，依次类推。以 -1 表示最后一
个成员，-2 表示倒数第二个成员。WITHSCORES 选项让 score 和 value 一同返回。
返回值：自定区间的成员集合

例 1：显示集合的全部元素，不显示 score，不使用 WITHSCORES
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614234815426.png)
例 2：显示集合全部元素，并使用 WITHSCORES
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614234832241.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 3：显示第 0,1 二个成员
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614234851900.png)
例 4：排序显示浮点数的 score
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614234914763.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**C、 zrevrange**
语法：zrevrange key start stop [WITHSCORES]
作用：返回有序集 key 中，指定区间内的成员。其中成员的位置按 score 值递减(从大到小)
来排列。其它同 zrange 命令。
返回值：自定区间的成员集合

例 1：成绩榜
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614234940523.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**D、zrem**
语法：zrem key member [member…]
作用：删除有序集合 key 中的一个或多个成员，不存在的成员被忽略
返回值：被成功删除的成员数量，不包括被忽略的成员。

例 1：删除指定一个成员 wangwu
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614235009591.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**E、 zcard**
语法：zcard key
作用：获取有序集 key 的元素成员的个数
返回值：key 存在返回集合元素的个数， key 不存在，返回 0

例 1：查询集合的元素个数
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614235032467.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
###### 2.5.3 其他命令
**A、zrangebyscore**
语法：zrangebyscore key min max [WITHSCORES ] [LIMIT offset count]
作用：获取有序集 key 中，所有 score 值介于 min 和 max 之间（包括 min 和 max）的成
员，有序成员是按递增（从小到大）排序。
 min ,max 是包括在内 ， 使用符号 ( 表示不包括。 min ， max 可以使用 -inf ，
+inf 表示最小和最大 limit 用来限制返回结果的数量和区间。
 withscores 显示 score 和 value
返回值：指定区间的集合数据
使用的准备数据
![在这里插入图片描述](https://img-blog.csdnimg.cn/201906142352187.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 1：显示指定具体区间的数据
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614235241184.png)
例 2：显示指定具体区间的集合数据，开区间（不包括 min，max）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614235304609.png)
例 3：显示整个集合的所有数据
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614235327602.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
例 4：使用 limit
增加新的数据：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614235349926.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614235403515.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
显示从第一个位置开始，取一个元素。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614235436409.png)
**B、 zrevrangebyscore**
语法：zrevrangebyscore key max min [WITHSCORES ] [LIMIT offset count]
作用：返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有
的成员。有序集成员按 score 值递减(从大到小)的次序排列。其他同 zrangebyscore

例 1：查询工资最高到 3000 之间的员工
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614235537282.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
C、 zcount
语法：zcount key min max
作用：返回有序集 key 中， score 值在 min 和 max 之间(默认包括 score 值等于 min 或 max )
的成员的数量

例 1：求工资在 3000-5000 的员工数量
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190614235610319.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
###### 2.5.4 应用-商品销售排行榜
需求：根据商品销售量对商品进行排行显示
思路：定义商品销售排行榜（sorted set集合），Key为items:sellsort，分数为商品销售量。

写入商品销售量：
商品编号1001的销量是9，商品编号1002的销量是10
```java
192.168.101.3:7007> ZADD items:sellsort 9 1001 10 1002
```
商品编号1001的销量加1
```java
192.168.101.3:7001> ZINCRBY items:sellsort 1 1001
```
商品销量前10名：
```java
192.168.101.3:7001> ZRANGE items:sellsort 0 9 withscores
```

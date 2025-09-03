#分布式 #唯一ID

TODO: 只记录到第五小节

> 本文主要介绍常见的分布式ID生成方式，大致分类的话可以分为两类：**一种是类DB型的**，根据设置不同起始值和步长来实现趋势递增，需要考虑服务的容错性和可用性; **另一种是类snowflake型**，这种就是将64位划分为不同的段，每段代表不同的涵义，基本就是时间戳、机器ID和序列数。这种方案就是需要考虑时钟回拨的问题以及做一些 buffer的缓冲设计提高性能。@pdai

# 1、[为什么需要全局唯一ID](/md/arch/arch-z-id.html#为什么需要全局唯一id)

传统的单体架构的时候，我们基本是单库然后业务单表的结构。每个业务表的ID一般我们都是从1增，通过AUTO_INCREMENT=1设置自增起始值，但是在分布式服务架构模式下分库分表的设计，使得多个库或多个表存储相同的业务数据。这种情况根据数据库的自增ID就会产生相同ID的情况，不能保证主键的唯一性。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404152308977.png)

如上图，如果第一个订单存储在 DB1 上则订单 ID 为1，当一个新订单又入库了存储在 DB2 上订单 ID 也为1。我们系统的架构虽然是分布式的，但是在用户层应是无感知的，重复的订单主键显而易见是不被允许的。那么针对分布式系统如何做到主键唯一性呢？

# 2、[UUID](/md/arch/arch-z-id.html#uuid)

`UUID （Universally Unique Identifier）`，通用唯一识别码的缩写。UUID是由一组32位数的16进制数字所构成，所以UUID理论上的总数为 `16^32=2^128`，约等于 `3.4 x 10^38`。也就是说若每纳秒产生1兆个UUID，要花100亿年才会将所有UUID用完。

生成的UUID是由 8-4-4-4-12格式的数据组成，其中32个字符和4个连字符' - '，一般我们使用的时候会将连字符删除 uuid.`toString().replaceAll("-","")`。

目前UUID的产生方式有5种版本，每个版本的算法不同，应用范围也不同。

- `基于时间的UUID` - 版本1： 这个一般是通过当前时间，随机数，和本地Mac地址来计算出来，可以通过 org.apache.logging.log4j.core.util包中的 UuidUtil.getTimeBasedUuid()来使用或者其他包中工具。由于使用了MAC地址，因此能够确保唯一性，但是同时也暴露了MAC地址，私密性不够好。
    
- `DCE安全的UUID` - 版本2 DCE（Distributed Computing Environment）安全的UUID和基于时间的UUID算法相同，但会把时间戳的前4位置换为POSIX的UID或GID。这个版本的UUID在实际中较少用到。
    
- `基于名字的UUID（MD5）`- 版本3 基于名字的UUID通过计算名字和名字空间的MD5散列值得到。这个版本的UUID保证了：相同名字空间中不同名字生成的UUID的唯一性；不同名字空间中的UUID的唯一性；相同名字空间中相同名字的UUID重复生成是相同的。
    
- `随机UUID` - 版本4 根据随机数，或者伪随机数生成UUID。这种UUID产生重复的概率是可以计算出来的，但是重复的可能性可以忽略不计，因此该版本也是被经常使用的版本。JDK中使用的就是这个版本。
    
- `基于名字的UUID（SHA1）` - 版本5 和基于名字的UUID算法类似，只是散列值计算使用SHA1（Secure Hash Algorithm 1）算法。
    

我们 Java中 JDK自带的 UUID产生方式就是版本4根据随机数生成的 UUID 和版本3基于名字的 UUID，有兴趣的可以去看看它的源码。
```java
public static void main(String[] args) {  
      
    //获取一个版本4根据随机字节数组的UUID。  
    UUID uuid = UUID.randomUUID();  
    System.out.println(uuid.toString().replaceAll("-",""));  
      
    //获取一个版本3(基于名称)根据指定的字节数组的UUID。  
    byte[] nbyte = {10, 20, 30};  
    UUID uuidFromBytes = UUID.nameUUIDFromBytes(nbyte);  
    System.out.println(uuidFromBytes.toString().replaceAll("-",""));  
}
```

得到的UUID结果，
```java
59f51e7ea5ca453bbfaf2c1579f09f1d
7f49b84d0bbc38e9a493718013baace6
```

虽然 UUID 生成方便，本地生成没有网络消耗，但是使用起来也有一些缺点，

- **不易于存储**：UUID太长，16字节128位，通常以36长度的字符串表示，很多场景不适用。
- **信息不安全**：基于MAC地址生成UUID的算法可能会造成MAC地址泄露，暴露使用者的位置。
- **对MySQL索引不利**：如果作为数据库主键，在InnoDB引擎下，UUID的无序性可能会引起数据位置频繁变动，严重影响性能，可以查阅 Mysql 索引原理 B+树的知识。

# 3、[数据库生成](/md/arch/arch-z-id.html#数据库生成)

是不是一定要基于外界的条件才能满足分布式唯一ID的需求呢，我们能不能在我们分布式数据库的基础上获取我们需要的ID？

由于分布式数据库的起始自增值一样所以才会有冲突的情况发生，那么我们将分布式系统中数据库的同一个业务表的自增ID设计成不一样的起始值，然后设置固定的步长，步长的值即为分库的数量或分表的数量。

以MySQL举例，利用给字段设置`auto_increment_increment`和`auto_increment_offset`来保证ID自增。

- `auto_increment_offset`：表示自增长字段从那个数开始，他的取值范围是1 .. 65535。
- `auto_increment_increment`：表示自增长字段每次递增的量，其默认值是1，取值范围是1 .. 65535。

假设有三台机器，则DB1中order表的起始ID值为1，DB2中order表的起始值为2，DB3中order表的起始值为3，它们自增的步长都为3，则它们的ID生成范围如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404152314126.png)

通过这种方式明显的优势就是依赖于数据库自身不需要其他资源，并且ID号单调自增，可以实现一些对ID有特殊要求的业务。

但是缺点也很明显，首先它**强依赖DB**，当DB异常时整个系统不可用。虽然配置主从复制可以尽可能的增加可用性，但是**数据一致性在特殊情况下难以保证**。主从切换时的不一致可能会导致重复发号。还有就是**ID发号性能瓶颈限制在单台MySQL的读写性能**。

# 4、[使用redis实现](/md/arch/arch-z-id.html#使用redis实现)

Redis实现分布式唯一ID主要是通过提供像 `INCR` 和 `INCRBY` 这样的自增原子命令，由于Redis自身的单线程的特点所以能保证生成的 ID 肯定是唯一有序的。

但是单机存在性能瓶颈，无法满足高并发的业务需求，所以可以采用集群的方式来实现。集群的方式又会涉及到和数据库集群同样的问题，所以也需要设置分段和步长来实现。

为了避免长期自增后数字过大可以通过与当前时间戳组合起来使用，另外为了保证并发和业务多线程的问题可以采用 Redis + Lua的方式进行编码，保证安全。

Redis 实现分布式全局唯一ID，它的性能比较高，生成的数据是有序的，对排序业务有利，但是同样它依赖于redis，**需要系统引进redis组件，增加了系统的配置复杂性**。

当然现在Redis的使用性很普遍，所以如果其他业务已经引进了Redis集群，则可以资源利用考虑使用Redis来实现。

# 5、[雪花算法-Snowflake](/md/arch/arch-z-id.html#雪花算法-snowflake)

Snowflake，雪花算法是由Twitter开源的分布式ID生成算法，以划分命名空间的方式将 64-bit位分割成多个部分，每个部分代表不同的含义。而 Java中64bit的整数是Long类型，所以在 Java 中 SnowFlake 算法生成的 ID 就是 long 来存储的。

- **第1位**占用1bit，其值始终是0，可看做是符号位不使用。
- **第2位**开始的41位是时间戳，41-bit位可表示2^41个数，每个数代表毫秒，那么雪花算法可用的时间年限是`(1L<<41)/(1000L360024*365)`=69 年的时间。
- **中间的10-bit位**可表示机器数，即2^10 = 1024台机器，但是一般情况下我们不会部署这么台机器。如果我们对IDC（互联网数据中心）有需求，还可以将 10-bit 分 5-bit 给 IDC，分5-bit给工作机器。这样就可以表示32个IDC，每个IDC下可以有32台机器，具体的划分可以根据自身需求定义。
- **最后12-bit位**是自增序列，可表示2^12 = 4096个数。

这样的划分之后相当于**在一毫秒一个数据中心的一台机器上可产生4096个有序的不重复的ID**。但是我们 IDC 和机器数肯定不止一个，所以毫秒内能生成的有序ID数是翻倍的。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404152320811.png)

Snowflake 的Twitter官方原版是用Scala写的，对Scala语言有研究的同学可以去阅读下，以下是 Java 版本的写法。

```java
package com.jajian.demo.distribute;  
  
/**  
 * Twitter_Snowflake<br>  
 * SnowFlake的结构如下(每部分用-分开):<br>  
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000 <br>  
 * 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0<br>  
 * 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)  
 * 得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69<br>  
 * 10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId<br>  
 * 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号<br>  
 * 加起来刚好64位，为一个Long型。<br>  
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。  
 */  
public class SnowflakeDistributeId {  
      
      
    // ==============================Fields===========================================  
    /**     * 开始时间截 (2015-01-01)  
     */    private final long twepoch = 1420041600000L;  
      
    /**  
     * 机器id所占的位数  
     */  
    private final long workerIdBits = 5L;  
      
    /**  
     * 数据标识id所占的位数  
     */  
    private final long datacenterIdBits = 5L;  
      
    /**  
     * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)  
     */    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);  
      
    /**  
     * 支持的最大数据标识id，结果是31  
     */    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);  
      
    /**  
     * 序列在id中占的位数  
     */  
    private final long sequenceBits = 12L;  
      
    /**  
     * 机器ID向左移12位  
     */  
    private final long workerIdShift = sequenceBits;  
      
    /**  
     * 数据标识id向左移17位(12+5)  
     */    private final long datacenterIdShift = sequenceBits + workerIdBits;  
      
    /**  
     * 时间截向左移22位(5+5+12)  
     */    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;  
      
    /**  
     * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)  
     */    private final long sequenceMask = -1L ^ (-1L << sequenceBits);  
      
    /**  
     * 工作机器ID(0~31)  
     */    private long workerId;  
      
    /**  
     * 数据中心ID(0~31)  
     */    private long datacenterId;  
      
    /**  
     * 毫秒内序列(0~4095)  
     */    private long sequence = 0L;  
      
    /**  
     * 上次生成ID的时间截  
     */  
    private long lastTimestamp = -1L;  
      
    //==============================Constructors=====================================  
        /**  
     * 构造函数  
     *  
     * @param workerId     工作ID (0~31)  
     * @param datacenterId 数据中心ID (0~31)  
     */    public SnowflakeDistributeId(long workerId, long datacenterId) {  
        if (workerId > maxWorkerId || workerId < 0) {  
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));  
        }  
        if (datacenterId > maxDatacenterId || datacenterId < 0) {  
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));  
        }  
        this.workerId = workerId;  
        this.datacenterId = datacenterId;  
    }  
      
    // ==============================Methods==========================================  
        /**  
     * 获得下一个ID (该方法是线程安全的)  
     *     * @return SnowflakeId     */    public synchronized long nextId() {  
        long timestamp = timeGen();  
          
        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常  
        if (timestamp < lastTimestamp) {  
            throw new RuntimeException(  
                    String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));  
        }  
          
        //如果是同一时间生成的，则进行毫秒内序列  
        if (lastTimestamp == timestamp) {  
            sequence = (sequence + 1) & sequenceMask;  
            //毫秒内序列溢出  
            if (sequence == 0) {  
                //阻塞到下一个毫秒,获得新的时间戳  
                timestamp = tilNextMillis(lastTimestamp);  
            }  
        }  
        //时间戳改变，毫秒内序列重置  
        else {  
            sequence = 0L;  
        }  
          
        //上次生成ID的时间截  
        lastTimestamp = timestamp;  
          
        //移位并通过或运算拼到一起组成64位的ID  
        return ((timestamp - twepoch) << timestampLeftShift) //  
                | (datacenterId << datacenterIdShift) //  
                | (workerId << workerIdShift) //  
                | sequence;  
    }  
      
    /**  
     * 阻塞到下一个毫秒，直到获得新的时间戳  
     *  
     * @param lastTimestamp 上次生成ID的时间截  
     * @return 当前时间戳  
     */  
    protected long tilNextMillis(long lastTimestamp) {  
        long timestamp = timeGen();  
        while (timestamp <= lastTimestamp) {  
            timestamp = timeGen();  
        }  
        return timestamp;  
    }  
      
    /**  
     * 返回以毫秒为单位的当前时间  
     *  
     * @return 当前时间(毫秒)  
     */    protected long timeGen() {  
        return System.currentTimeMillis();  
    }  
}
```

测试的代码如下：
```java
public static void main(String[] args) {  
	SnowflakeDistributeId idWorker = new SnowflakeDistributeId(0, 0);  
	for (int i = 0; i < 1000; i++) {  
		long id = idWorker.nextId();  
//      System.out.println(Long.toBinaryString(id));  
		System.out.println(id);  
	}  
}
```

**雪花算法提供了一个很好的设计思想，雪花算法生成的ID是趋势递增，不依赖数据库等第三方系统，以服务的方式部署，稳定性更高，生成ID的性能也是非常高的，而且可以根据自身业务特性分配bit位，非常灵活**。

但是雪花算法强**依赖机器时钟**，如果机器上时钟回拨，会导致发号重复或者服务会处于不可用状态。如果恰巧回退前生成过一些ID，而时间回退后，生成的ID就有可能重复。官方对于此并没有给出解决方案，而是简单的抛错处理，这样会造成在时间被追回之前的这段时间服务不可用。

很多其他类雪花算法也是在此思想上的设计然后改进规避它的缺陷，后面介绍的`百度 UidGenerator` 和 `美团分布式ID生成系统 Leaf` 中snowflake模式都是在 snowflake 的基础上演进出来的。

# 6、[百度-UidGenerator](/md/arch/arch-z-id.html#百度-uidgenerator)

> 百度的 `UidGenerator` 是百度开源基于Java语言实现的唯一ID生成器，是在雪花算法 snowflake 的基础上做了一些改进。`UidGenerator`以组件形式工作在应用项目中, 支持自定义workerId位数和初始化策略，适用于docker等虚拟化环境下实例自动重启、漂移等场景。




## 6.1、[DefaultUidGenerator 实现](/md/arch/arch-z-id.html#defaultuidgenerator-实现)



## 6.2、[CachedUidGenerator 实现](/md/arch/arch-z-id.html#cacheduidgenerator-实现)



# 7、[美团Leaf](/md/arch/arch-z-id.html#美团leaf)



## 7.1、[Leaf-segment 数据库方案](/md/arch/arch-z-id.html#leaf-segment-数据库方案)



## 7.2、[Leaf-snowflake方案](/md/arch/arch-z-id.html#leaf-snowflake方案)



# 8、[Mist 薄雾算法](/md/arch/arch-z-id.html#mist-薄雾算法)



## 8.1、[考量了什么业务场景和要求呢？](/md/arch/arch-z-id.html#考量了什么业务场景和要求呢)



## 8.2、[薄雾算法的设计思路是怎么样的？](/md/arch/arch-z-id.html#薄雾算法的设计思路是怎么样的)



## 8.3、[薄雾算法生成的数值是什么样的？](/md/arch/arch-z-id.html#薄雾算法生成的数值是什么样的)



## 8.4[薄雾算法 mist 和雪花算法 snowflake 有何区别？](/md/arch/arch-z-id.html#薄雾算法-mist-和雪花算法-snowflake-有何区别)



## 8.5、[为什么薄雾算法不受时间回拨影响？](/md/arch/arch-z-id.html#为什么薄雾算法不受时间回拨影响)



## 8.6、[为什么说薄雾算法的结果值不可预测？](/md/arch/arch-z-id.html#为什么说薄雾算法的结果值不可预测)



## 8.7、[当程序重启，薄雾算法的值会重复吗？](/md/arch/arch-z-id.html#当程序重启-薄雾算法的值会重复吗)



## 8.8、[薄雾算法的值会重复，那我要它干嘛？](/md/arch/arch-z-id.html#薄雾算法的值会重复-那我要它干嘛)



## 8.9、[是否提供薄雾算法的工程实践或者架构实践？](/md/arch/arch-z-id.html#是否提供薄雾算法的工程实践或者架构实践)



## 8.10、[薄雾算法的分布式架构，推荐 CP 还是 AP？](/md/arch/arch-z-id.html#薄雾算法的分布式架构-推荐-cp-还是-ap)



# 9、[总结](/md/arch/arch-z-id.html#总结)



# 10、[参考文章](/md/arch/arch-z-id.html#参考文章)




转载自：[https://pdai.tech/md/arch/arch-z-id.html](https://pdai.tech/md/arch/arch-z-id.html)
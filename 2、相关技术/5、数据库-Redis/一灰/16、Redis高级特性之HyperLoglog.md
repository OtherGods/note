
hyperloglog算法，利用非常少的空间，实现比较大的数据量级统计；比如我们前面在介绍bitmap的过程中，说到了日活的统计，当数据量达到百万时，最佳的存储方式是hyperloglog，本文将介绍一下hyperloglog的基本原理，以及redis中的使用姿势

# 1、基本使用
## 1.1、配置

我们使用SpringBoot `2.2.1.RELEASE`来搭建项目环境，直接在`pom.xml`中添加redis依赖
```xml
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-data-redis</artifactId>  
</dependency>
```

如果我们的redis是默认配置，则可以不额外添加任何配置；也可以直接在`application.yml`配置中，如下
```yml
spring:  
  redis:  
    host: 127.0.0.1  
    port: 6379  
    password:
```

## 1.2、使用姿势

redis中，`hyperlolog`使用非常简单，一般就两个操作命令，添加`pfadd` + 计数`pfcount`；另外还有一个不常用的`merge`

### 1.2.1、add

添加一条记录
```java
public boolean add(String key, String obj) {  
    // pfadd key obj  
    return stringRedisTemplate.opsForHyperLogLog().add(key, obj) > 0;  
}
```

### 1.2.2、pfcount

非精准的计数统计
```java
public long count(String key) {  
    // pfcount 非精准统计 key的计数  
    return stringRedisTemplate.opsForHyperLogLog().size(key);  
}
```

### 1.2.3、merge

将多个hyperloglog合并成一个新的hyperloglog；感觉用的场景并不会特别多
```java
public boolean merge(String out, String... key) {  
    // pfmerge out key1 key2  ---> 将key1 key2 合并成一个新的hyperloglog out  
    return stringRedisTemplate.opsForHyperLogLog().union(out, key) > 0;  
}
```

## 1.3、原理说明

关于HyperLogLog的原理我这里也不进行详细赘述，说实话那一套算法以及调和平均公式我自己也没太整明白；下面大致说一下我个人的朴素理解

Redis中的HyperLogLog一共分了`2^14=16384`个桶，每个桶占6个bit

一个数据，塞入HyperLogLog之前，先hash一下，得到一个64位的二进制数据
- 取低14位，用来定位桶的index
- 高50位，从低到高数，找到第一个为1出现的位置n
    - 若桶中值 > n，则丢掉
    - 反之，则设置桶中的值为n
那么怎么进行计数统计呢？
- 拿所有桶中的值，代入下面的公式进行计算

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202405212324684.png)

上面这个公式怎么得出的?
之前看到一篇文章，感觉不错，有兴趣了解原理的，可以移步: [https://www.jianshu.com/p/55defda6dcd2](https://www.jianshu.com/p/55defda6dcd2)

## 1.4、应用场景

`hyperloglog`通常是用来非精确的计数统计，前面介绍了日活统计的case，当时使用的是bitmap来作为数据统计，然而当userId分散不均匀，小的特别小，大的特别大的时候，并不适用

在数据量级很大的情况下，`hyperloglog`的优势非常大，它所占用的存储空间是固定的`2^14`  
下图引用博文[《用户日活月活怎么统计》](https://mp.weixin.qq.com/s/AvPoG8ZZM8v9lKLyuSYnHQ)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202405212324575.png)

使用HyperLogLog进行日活统计的设计思路比较简单

- 每日生成一个key
- 某个用户访问之后，执行 `pfadd key userId`
- 统计总数: `pfcount key`

# 2、菜鸟

## 2.1、Redis HyperLogLog

Redis 在 2.8.9 版本添加了 HyperLogLog 结构。

Redis HyperLogLog 是用来做基数统计的算法，HyperLogLog 的优点是，在输入元素的数量或者体积非常非常大时，计算基数所需的空间总是固定 的、并且是很小的。

在 Redis 里面，每个 HyperLogLog 键只需要花费 12 KB 内存，就可以计算接近 2^64 个不同元素的基 数。这和计算基数时，元素越多耗费内存就越多的集合形成鲜明对比。

但是，因为 HyperLogLog 只会根据输入元素来计算基数，而不会储存输入元素本身，所以 HyperLogLog 不能像集合那样，返回输入的各个元素。

## 2.2、什么是基数?

比如数据集 {1, 3, 5, 7, 5, 7, 8}， 那么这个数据集的基数集为 {1, 3, 5 ,7, 8}, 基数(不重复元素)为5。 基数估计就是在误差可接受的范围内，快速计算基数。

## 2.3、Redis HyperLogLog 命令

下表列出了 redis HyperLogLog 的基本命令：

| 序号  | 命令及描述                                                                                                                                  |
| --- | -------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | PFADD key element [element ...]<br>添加指定元素到 HyperLogLog 中。                                                                              |
| 2   | PFCOUNT key [key ...]<br>返回给定 HyperLogLog 的基数估算值。                                                                                      |
| 3   | PFMERGE destkey sourcekey [sourcekey ...]<br>将多个 HyperLogLog 合并为一个 HyperLogLog，合并后的 HyperLogLog 的基数估算值是通过对所有 给定 HyperLogLog 进行并集计算得出的。 |

### 2.3.1、Pfadd 命令

命令基本语法如下：

```shell
redis 127.0.0.1:6379> PFADD key element [element ...]
```

可用版本 >= 2.8.9

返回值：整型，如果至少有个元素被添加返回 1， 否则返回 0。
```shell
redis 127.0.0.1:6379> PFADD mykey a b c d e f g h i j
(integer) 1
redis 127.0.0.1:6379> PFCOUNT mykey
(integer) 10
```

### 2.3.2、Pfcount 命令

命令基本语法如下：

```shell
redis 127.0.0.1:6379> PFCOUNT key [key ...]
```

可用版本 >= 2.8.9

返回值：整数，返回给定 HyperLogLog 的基数值，如果多个 HyperLogLog 则返回基数估值之和。

```shell
redis 127.0.0.1:6379> PFADD hll foo bar zap
(integer) 1
redis 127.0.0.1:6379> PFADD hll zap zap zap
(integer) 0
redis 127.0.0.1:6379> PFADD hll foo bar
(integer) 0
redis 127.0.0.1:6379> PFCOUNT hll
(integer) 3
redis 127.0.0.1:6379> PFADD some-other-hll 1 2 3
(integer) 1
redis 127.0.0.1:6379> PFCOUNT hll some-other-hll
(integer) 6
redis> 
```

### 2.3.3、PFMERGE 命令

命令基本语法如下：

```shell
PFMERGE destkey sourcekey [sourcekey ...]
```

可用版本：>= 2.8.9

返回值：返回 OK。

```shell
redis> PFADD hll1 foo bar zap a
(integer) 1
redis> PFADD hll2 a b c foo
(integer) 1
redis> PFMERGE hll3 hll1 hll2
"OK"
redis> PFCOUNT hll3
(integer) 6
redis>  
```

# 3、其他
## 3.1、项目

工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)



转载自：[【DB系列】Redis高级特性之HyperLoglog](https://spring.hhui.top/spring-blog/2020/10/21/201021-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8BRedis%E9%AB%98%E7%BA%A7%E7%89%B9%E6%80%A7%E4%B9%8BHyperLoglog/)
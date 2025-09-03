#堆分代 #新生代 #老年代 #永久代 #对象分代晋升 #动态年龄判断 
# 典型回答

Java的<font color="red" size=5>堆内存分代</font>是指<font color="blue" size=5>将不同生命周期的堆内存对象存储在不同的堆内存区域中，这里的不同的堆内存区域被定义为“代”</font>。这样做**有助于提升垃圾回收的效率，因为这样的话就可以为不同的"代"设置不同的回收策略**。

一般来说，**Java中的大部分对象都是朝生夕死的**，同时也有一部分对象会持久存在。因为如果把这两部分对象放到一起分析和回收，这样效率实在是太低了。**通过将不同时期的对象存储在不同的内存池中，就可以节省宝贵的时间和空间，从而改善系统的性能**。

Java的堆由 **新生代（Young Generation）** 和 **老年代（Old Generation）** 组成。**新生代**存放**新分配的对象**，**老年代**存放**长期存在的对象**。

新生代（Young）由**年轻区（Eden）**、**Survivor区组成（From Survivor、To Survivor）**。默认情况下，新生代的**Eden区**和**Survivor区**的空间**大小比例是8:2**，可以**通过-XX:SurvivorRatio参数调整**。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507291557015.png)

1. ~~很多对象都会出现在Eden区~~(应该是新生的对象都会出现在Eden区)
2. 当**Eden区的内存容量用完**的时候，**GC**会发起，非存活对象会被标记为死亡，**存活的对象被移动到Survivor区**
3. 如果**Survivor的内存容量也用完，那么存活对象会被移动到老年代**。
4. **老年代（Old）是对象存活时间最长的部分**，它由单一存活区（Tenured）组成，并且把**经历过若干轮GC回收还存活下来的对象移动而来**。在老年代中，大部分对象都是活了很久的，所以**GC回收它们会很慢**。

# 扩展知识

## 对象的分代晋升

一般情况下，对象将在新生代进行分配，首先会尝试**在Eden区分配对象**，当**Eden内存耗尽，无法满足新的对象分配请求时，将触发新生代的GC**(Young GC、MinorGC)，在新生代的GC过程中，**没有被回收的对象会从Eden区被搬运到Survivor区**，这个过程通常被称为"晋升"

同样的，**对象也可能会晋升到老年代，触发条件主要看对象的大小和年龄**。对象进入老年代的条件有三个，满足一个就会进入到老年代：
1. **躲过15次GC**。每次垃圾回收后，存活的对象的年龄就会加1（存储在对象头Mark Word中），累计加到15次（jdk8默认的），也就是某个对象躲过了15次垃圾回收，那么JVM就认为这个是经常被使用的对象，就没必要再待在年轻代中了。具体的次数可以通过 `-XX:MaxTenuringThreshold` 来设置在躲过多少次垃圾收集后进去老年代。
2. **动态对象年龄判断**。规则：**如果在Survivor空间中小于等于某个年龄的所有对象大小的总和大于Survivor空间的一半时，那么就把大于等于这个年龄的对象都晋升到老年代。**
3. **大对象直接进入老年代**。`-XX:PretenureSizeThreshold` 来设置大对象的**临界值**，大于该值的就被认为是大对象，就会直接进入老年代。（PretenureSizeThreshold默认是0，也就是说，默认情况下对象不会提前进入老年代，而是直接在新生代分配。然后就GC次数和基于动态年龄判断来进入老年代。）
针对上面的三点来逐一分析。

### 动态年龄判断

为了能更好地适应不同程序的内存状况，HotSpot虚拟机并不是永远要求对象的年龄必须达到- XX:M axTenuringThreshold才能晋升老年代，他还有一个动态年龄判断的机制。

在《深入理解Java虚拟机（第三版）》中是这么描述动态年龄判断的过程的：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507291611450.png)

<font color="red" size=5>但是，这段描述是不正确的！</font>

JVM中，动态年龄判断的代码如下：
```java
uint ageTable::compute_tenuring_threshold(size_t survivor_capacity) {
  
  size_t desired_survivor_size = (size_t)((((double) survivor_capacity)*TargetSurvivorRatio)/100);
  size_t total = 0;
  uint age = 1;
  while (age < table_size) {
    total += sizes[age];
    if (total > desired_survivor_size) break;
    age++;
  }
  uint result = age < MaxTenuringThreshold ? age : MaxTenuringThreshold;
    ...
}
```

它的过程是从年龄小的对象开始，不断地累加对象的大小，当年龄达到N时，刚好达到TargetSurvivorRatio这个阈值，那么就把所有年龄大于等于N的对象全部晋升到老年代去！

所以，这过程应该是这样的：

**如果在Survivor空间中小于等于某个年龄的所有对象大小的总和大于Survivor空间的一半时，那么就把大于等于这个年龄的对象都晋升到老年代。**<font color="red" size=5>Hollis在这里的意思不是等于某年龄，而是小于等于某年龄</font>

## 新生代如果只有两个区域可以吗？

[8、新生代如果只有一个Eden+一个Survivor可以吗？](2、相关技术/3、JVM/Hollis/8、新生代如果只有一个Eden+一个Survivor可以吗？.md)

## 什么是永久代？

永久代（Permanent Generation）是HotSpot虚拟机在以前版本中使用的一个永久内存区域，是JVM中垃圾收集堆之外的另一个内存区域，它主要用来实现方法区的，其中存储了Class类信息、常量池以及静态变量等数据。

Java 8以后，永久代被重构为元空间（MetaSpace）。

但是，<font color="red" size=5>和新生代、老年代一样，永久代也是可能会发生GC的</font>。而且，永久代也是有**可能导致内存溢出。只要永久代的内存分配超过限制指定的最大值，就会出现内存溢出**。

## 不同分代的GC方式？

1. <font color="red" size=5>Minor GC（YoungGC）</font>，主要用于对<font color="blue" size=5>新生代垃圾回收</font>
2. <font color="red" size=5>Full GC</font>，对<font color="blue" size=5> 老年代 或者 永久代 进行回收</font>
相比较于Minor GC，Full GC的收集频率更低，耗时更长。

[16、新生代和老年代的垃圾回收器有何区别？](2、相关技术/3、JVM/Hollis/16、新生代和老年代的垃圾回收器有何区别？.md)

# 典型回答

参考：
- [9、YoungGC和FullGC的触发条件是什么？](2、相关技术/3、JVM/Hollis/9、YoungGC和FullGC的触发条件是什么？.md)
- [空间分配担保机制](8、新生代如果只有一个Eden+一个Survivor可以吗？#空间分配担保机制)
- [对象的分代晋升](7、Java的堆是如何分代的？为什么分代？#对象的分代晋升)：GC通常伴随着对象的分代晋升
- [41、内存泄漏和内存溢出的区别是什么？](2、相关技术/3、JVM/Hollis/41、内存泄漏和内存溢出的区别是什么？.md)

一次完整的GC流程大致如下，基于JDK 1.8：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508012035764.png)

1. 一般来说，GC的触发是在对象分配过程中，当一个<font color="blue" size=5>对象在创建时，根据对象的大小决定是进入年轻代或者老年代</font>。如果他的大小超过`-XX:PretenureSizeThreshold`就会被认为是大对象，直接进入老年代，否则就会在年轻代进行创建。（`PretenureSizeThreshold`默认是0，也就是说，默认情况下对象不会提前进入老年代，而是直接在新生代分配。然后就GC次数和基于动态年龄判断来进入老年代。）
2. 在年轻代创建对象，会发生在`Eden`区，但是这个时候有可能会因为<font color="blue" size=5>Eden区内存不够，尝试触发一次YoungGC</font>。（会在`YoungGC`前做一次空间分配担保，**如果失败可能直接触发`FullGC`**）
> 年轻代采用的是标记复制算法，主要分为，标记、复制、清除三个步骤，会从GC Root开始进行存活对象的标记，然后把Eden区和Survivor区复制到另外一个Survivor区。然后再把Eden和From Survivor区的对象清理掉。
3. `YoungGC` 之前，可能会发生两件事情：
	1. <font color="blue" size=5>空间分配担保</font>：Survivor有可能存不下这些存活的对象，这时候就会进行空间分配担保。如果担保成功了，那么就没什么事儿，正常进行Young GC就行了。但是如果担保失败了，说明老年代可能也不够了，这时候就会触发一次FullGC了。
	   [空间分配担保机制](8、新生代如果只有一个Eden+一个Survivor可以吗？#空间分配担保机制)
	2. <font color="blue" size=5>对象分代晋升</font>：分为新生代内部的对象晋升、新生代-老年代对象晋升两种；
	   - `YoungGC` 导致的新生代内部晋升
	   - 对象头MarkWord中GC年龄超过15次（默认）、动态年龄判断导致的新生代到老年代的晋升
	   [对象的分代晋升](7、Java的堆是如何分代的？为什么分代？#对象的分代晋升)
4. <font color="blue" size=5>老年代如果不够了，那么就会触发FullGC</font>，一般来说，现在用的比较多的老年代的垃圾收集器是CMS或者G1，他们采用的都是三色标记法。
   [9、YoungGC和FullGC的触发条件是什么？](2、相关技术/3、JVM/Hollis/9、YoungGC和FullGC的触发条件是什么？.md)
> 也就是分为四个阶段：初始标记、并发标记、重新标记、及并发清理。
5. 老年代在做FullGC之后，如果空间还是不够，那就要<font color="blue" size=5>触发OOM了</font>。
   [41、内存泄漏和内存溢出的区别是什么？](2、相关技术/3、JVM/Hollis/41、内存泄漏和内存溢出的区别是什么？.md)

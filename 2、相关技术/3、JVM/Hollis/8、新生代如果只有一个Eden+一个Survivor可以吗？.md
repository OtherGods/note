#堆分代 #新生代 #标记复制算法 #标记复制 #空间分配担保 
# 典型回答

**答案是不行，如果只有两个区域，也能实现复制算法，但是会大大浪费空间。**

我们知道，新生代进一步区分了 **一个Eden区** 和 **2个Survivor区** ，**一共有Eden、Survivor From、Survivor To这三个区域**，那么，为什么需要三个区域呢？2个行不行呢？

这其实涉及到新生代的垃圾回收算法了：
[15、新生代和老年代的GC算法](2、相关技术/3、JVM/Hollis/15、新生代和老年代的GC算法.md)

根据默认配置，新生代有一个 Eden区，两个survivor区，**eden区占80%内存空间，每一块survivor区占 10%**
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507292341829.png)

因为新生代主要使用的是 `标记-复制` 算法进行垃圾回收的：
1. 第一次<font color="red" size=4>对象都分配在Eden区</font>
2. <font color="blue" size=4>Eden区快满了就触发垃圾回收</font>，把<font color="blue" size=4>Eden区中的存活对象转移到一块空着的survivor区，eden区清空</font>
3. <font color="red" size=4>然后再次分配新对象到eden区</font>
4. <font color="blue" size=4>Eden区再快满，再触发垃圾回收</font>，就把<font color="blue">eden区存活的和survivor区存活的转移</font>到另一块空着的survivor，同时将<font color="blue">eden区和原来的survivor区清空</font>。

那么也就是说，在平常的时候，<font color="blue" size=5>新生代的区域中是只有一块eden和一块survivor区在被使用的，而另一块Survivor区是空着的，所以内存使用率大约 90%。</font>

如果没有三个区域，只有两个，比如只有一个Eden和一个Survivor：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507292356228.png)

如果此时Eden区进行YoungGC之后，会如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507292357554.png)

那么，接下来继续创建对象的时候，如果继续向Eden分配：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507292358904.png)

如果之后进行**第二次YoungGC的时候，就不能只扫描Eden区，还要扫描Survivor区。那么，就不能使用标记复制算法了**，因为**标记复制算法的要求是必须有一块区域是空着的**。

而如果使用标记-清除算法或者标记-整理算法的话，就会存在碎片和效率等问题。

那么，如果改一下，从Eden复制到Survivor之后，再次分配新对象的时候分配到Survivor呢？然后Survivor满了再把对象复制到Eden，这样循环往复？
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507300002654.png)

这样做，或许可以实现复制算法了，但是**带来的问题就是两个区域都会承担新对象的分配工作，那么他的内存就都得足够大，那么就要分配成1：1**，这样的话，**整个新生代的同一时刻只能有1/2的空间被使用，利用率很低**。

# 扩展知识

## Survivor不够怎么办？

在YoungGC之后，如果存活的对象所需要的空间比Survivor区域的空间大怎么办呢？毕竟一块Survivor区域的比例只是年轻代的10%而已。

**这时候就需要把对象移动到老年代**。

### 空间分配担保机制

> 我理解的空间分配担保的核心就是：在YoungGC开始时，校验 ==**老年代此刻可用连续内存空间**== 先后与 ==**新生代此刻存活对象占用空间**==、==**历次晋升到老年代的对象的平均占用空间**== 的大小，校验成功（担保成功）正常执行YoungGC，否则执行FullGC。

如果Survivor区域的空间不够，就要分配给老年代，也就是说，老年代起到了一个兜底的作用。但是，老年代也是可能空间不足的。所以，在这个过程中就需要做一次**空间分配担保**（CMS）：

在<font color="blue" size=5>每一次执行YoungGC之前</font>，**虚拟机会检查老年代最大可用的连续空间是否大于新生代所有对象的总空间**。
1. 如果**大于**，那么说明本次Young GC是安全的。
2. 如果**小于**，那么虚拟机会查看 `HandlePromotionFailure` 参数设置的值判断是否允许担保失败。
	- 如果值为true，那么会<font color="red">继续检查老年代最大可用连续空间是否大于历次晋升到老年代的对象的平均大小</font>（一共有多少对象在内存回收后存活下来是不可预知的，因此只好取之前每次垃圾回收后晋升到老年代的对象大小的平均值作为参考）：
		1. 如果大于，则尝试进行一次YoungGC，但这次YoungGC依然是有风险的
		2. 如果小于则会直接触发一次Full GC。
	- 如果值为false，则会直接触发一次Full GC。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507312118102.png)

**但是，需要注意的是 `HandlePromotionFailure` 这个参数，在JDK 7中就不再支持了：**
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507300007025.png)

在JDK代码中，移除了这个参数的判断（[https://github.com/openjdk/jdk/commit/cbc7f8756a7e9569bbe1a38ce7cab0c0c6002bf7](https://github.com/openjdk/jdk/commit/cbc7f8756a7e9569bbe1a38ce7cab0c0c6002bf7) ），也就是说，在后续的版本中， 只要检查老年代最大可用连续空间是否大于历次晋升到老年代的对象的平均大小，如果大于，则认为担保成功。

但是需要注意的是，担保的结果可能成功，也可能失败。所以，在YoungGC的复制阶段执行之后，会发生以下三种情况：
- 剩余的存活对象大小，小于Survivor区，那就直接**进入Survivor区**
- 剩余的存活对象大小，大于Survivor区，小于老年代可用内存，那就**直接去老年代**
- 剩余的存活对象大小，大于Survivor并且大于老年代，**触发`"FullGC"`**。

#YoungGC #触发YoungGC #FullGC #触发FullGC #空间分配担保 

<font color="red" size=5>YoungGC的触发条件</font> 比较简单，那就是当<font color="blue" size=5>年轻代中的Eden区分配满</font>的时候就会触发。

<font color="red" size=5>FullGC的触发条件</font> 比较复杂也比较多，主要以下几种：
- 老年代空间不足
	- 创建一个大对象，超过指定阈值会直接保存在老年代当中，如果老年代空间也不足，会触发Full GC。
	- YoungGC之后，发现要移到老年代的对象，老年代存不下的时候，会触发一次FullGC
- 空间分配担保失败(空间分配担保详见：[空间分配担保机制](8、新生代如果只有一个Eden+一个Survivor可以吗？#空间分配担保机制))
	- 当准备要**触发一次YoungGC**时，会进行**空间分配担保**，在担保过程中，发现虚拟机会检查 ==*老年代最大可用的连续空间*小于*新生代所有对象的总空间*==，但是==*HandlePromotionFailure=false*==，那么就会触发一次FullGC（HandlePromotionFailure 这个配置，在JDK 7中并不在支持了，这一步骤在该版本已取消）
	- 当准备要**触发一次YoungGC**时，会进行**空间分配担保**，在担保过程中，发现虚拟机会检查 ==*老年代最大可用的连续空间*小于*新生代所有对象的总空间*== ，但是==*HandlePromotionFailure=true*==，继续检查发现==*老年代最大可用连续空间*小于*历次晋升到老年代的对象的平均大小*==时，会触发一次FullGC
- 永久代空间不足
	- 如果有永久代的话，当在永久代分配空间时没有足够空间的时候，会触发FullGC
- 代码中执行`System.gc()`
	- 代码中执行`System.gc()`的时候，会触发FullGC，但是并不保证一定会立即触发
	- 在 Java 中，调用 `System.gc()` **通常会触发 Full GC**，但具体行为取决于 JVM 实现和垃圾收集器（GC）配置

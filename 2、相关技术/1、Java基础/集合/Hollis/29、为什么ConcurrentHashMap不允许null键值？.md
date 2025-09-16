# 典型回答

<font color="blue" size=5>总结本文就是</font>：`ConcurrentHashMap`不允许null键和值，这种同步容器只能保证 *==单个操作是原子性==* 的，*==多个操作无法保证原子性==*，所以在`ConcurrentHashMap#get`获取到的值为null时，无法通过`ConcurrentHashMap#contains`判断是这个键不存在还是对应的值为null，<font color="red" size=5>因为 get 和 contains 不是原子操作</font>；而`HashMap`中可以存储null的键和值是因为HashMap是为单线程设计的，不需要原子性保证并发线程安全。

我们知道，ConcurrentHashMap在使用时，和HashMap有一个比较大的区别，那就是**HashMap中，null可以作为键或者值都可以。而在ConcurrentHashMap中，key和value都不允许为null**。

那么，为什么呢？为啥ConcurrentHashMap要设计成这样的呢？

关于这个问题，其实最有发言权的就是ConcurrentHashMap的作者——Doug Lea

他自己曾经出面解释过这个问题，内容如下（[http://cs.oswego.edu/pipermail/concurrency-interest/2006-May/002485.html](http://cs.oswego.edu/pipermail/concurrency-interest/2006-May/002485.html)，原文地址已经打不开了，大家将就着看一下截图吧） ：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507082253695.png)

## 主要意思就是说

ConcurrentMap（如ConcurrentHashMap、ConcurrentSkipListMap）不允许使用null值的主要原因是，在非并发的Map中（如HashMap)，是可以容忍模糊性（二义性）的，而在并发Map中是无法容忍的。

假如说，所有的Map都支持null的话，那么map.get(key)就可以返回null，但是，这时候就会存在一个不确定性，当你拿到null的时候，你是不知道他是因为本来就存了一个null进去还是说就是因为没找到而返回了null。

**在HashMap中，因为它的设计就是给单线程用的，所以当我们map.get(key)返回null的时候，我们是可以通过map.contains(key)检查来进行检测的**，如果它返回true，则认为是存了一个null，否则就是因为没找到而返回了null。

但是，像**ConcurrentHashMap，它是为并发而生的，它是要用在并发场景中的，当我们map.get(key)返回null的时候，是没办法通过map.contains(key)检查来准确的检测，因为在检测过程中可能会被其他线程所修改**，而导致检测结果并不可靠。
> `ConcurrentHashMap`不能存null是因为`ConcurrentHashMap`虽然是线程安全的，但是 **==这种同步容器只能保证单个操作是*原子性*的，*多个操作无法保证原子性*==**，这里的多个操作就是`concurrentHashMap.get(key) + concurrentHashMap.contains(key)`；
> 
> *==如果可以存null，会存在这样的情况==*：现有一个对象`ConcurrentHashMap concurrentHashMap`被A线程与B线程共享，在线程A中操作这个对象时，这个对象的变化可能是下面这样
> 1. A线程执行`concurrentHashMap.put("key", null)`
> 2. A线程执行`Object o = concurrentHashMap.get("key")`，得到的`o`是null，需要判断这个`"key"`是否在Map中存在
> 3. B线程执行`concurrentHashMap.remove("key")`
> 4. A线程执行`boolean b = concurrentHashMap.contains("key")`，结果得到的`b`为false，在A线程中出现了幻觉

所以，为了让ConcurrentHashMap的语义更加准确，不存在二义性的问题，他就不支持null。

## 总结对比

| 特性                           | HashMap                       | ConcurrentHashMap |
| ---------------------------- | ----------------------------- | ----------------- |
| **线程安全**                     | 否                             | 是                 |
| **允许 `null` 键**              | 是                             | **否**             |
| **允许 `null` 值**              | 是                             | **否**             |
| **`get(key)` 返回 `null` 的含义** | 1. 键不存在  <br>2. 键存在，值为 `null` | **明确表示键不存在**      |
| **设计优先级**                    | 单线程下的灵活性                      | 多线程下的明确性和安全性      |
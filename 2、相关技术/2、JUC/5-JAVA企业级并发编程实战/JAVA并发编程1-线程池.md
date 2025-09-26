# 1、线程池的核心原理

## 1.1、线程池的核心思想

1. 利用池化思想，管理线程的工具
2. 避免线程创建、销毁、调度的开销
3. 避免线程数量膨胀，保证对内核的充分利用

## 1.2、线程池的原理动画

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509121720681.png)

## 1.3、线程池7大核心参数

核心类 java.util.concurrent.ThreadPoolExecutor，构造函数参数如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509121722640.png)
- 这里核心线程说错了，核心线程是在向线程池中提交任务才创建的，不是线程池一创建就创建核心线程

## 1.4、线程池的7种阻塞队列

1. `ArrayBlockingQueue`：一个由数组结构组成的**有界阻塞队列**
2. `LinkedBlockingQueue`：一个由链表结构组成的**有界/无界阻塞队列**（常用）
3. `PriorityBlockingQueue`：一个支持优先级排序的**无界阻塞队列**
4. `DelayQueue`： 一个使用优先级队列实现的**无界阻塞队列**
5. `SynchronousQueue`： 一个**不存储元素的阻塞队列**（常用）
6. `LinkedTransferQueue`： 一个由链表结构组成的**无界阻塞队列**
7. `LinkedBlockingDeque`： 一个由链表结构组成的**双向阻塞队列**
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509241643983.png)

## 1.5、线程池4种拒绝策略

1. `ThreadPoolExecutor.AbortPolicy`:丢弃任务并抛出`RejectedExecutionException`异常
2. `ThreadPoolExecutor.DiscardPolicy`：丢弃任务，但是不抛出异常
3. `ThreadPoolExecutor.DiscardOldestPolicy`：丢弃队列最前面的任务，然后重新提交被拒绝的任务
4. `ThreadPoolExecutor.CallerRunsPolicy`：由调用线程（提交任务的线程）处理该任务

# 2、线程池最佳使用原则

## 2.1、合理使用拒绝策略

常见错误：
1. 一律使用默认的拒绝策略，没有考虑后果
2. 任务莫名其妙丢失，找不到问题，没有日志
3. 虽然知道任务被丢弃了，但不知道哪些任务被丢弃了，甚至造成了数据丢失

解决方式：
1. 控制任务尽量不要进入拒绝策略
	1. 增加最大线程数：可能会导致频繁发生 [上下文切换](2、相关技术/2、JUC/Hollis/Java并发/1、什么是多线程中的上下文切换？.md)、甚至因线程数过多而内存溢出
	2. 降低提交任务的频率
	   - 最佳实践：在哦循环中提交大量任务的时候，一定要每隔N个任务，sleep几百毫秒，如果一次性全部提交，可能导致大量任进入拒绝策略
	3. 增加工作队列长度：可能会导致因阻塞队列过长而内存溢出
	   - 适用于以下场景：系统任务单一、可预估最大任务量、内存重组
2. 如果无法避免任务被丢弃，则可以重写拒绝策略，暂存被拒绝的任务（比如写入数据库），留给后续任务处理

## 2.2、禁止使用Executors线程工具类


**线程池使用原则**：
- 禁止最大线程数使用MAX_INTEGER
- 禁止使用无界队列

禁用Executors各个方法的原因：
- `newFixedThreadPool` 和 `newSingleThreadExecutor`:
  都使用`LinkedBlockingQueue`无界队列，堆积的任务处理队列会耗费非常大的内存，甚至内存溢出。
- `newCachedThreadPool` 和 `newScheduledThreadPool`:
    线程数最大数都是`Integer.MAX_VALUE`，可能会创建数量非常多的线程，打爆CPU、甚至OOM。

## 2.3、线程池调优原则

[9、线程数设定成多少更合适？](2、相关技术/2、JUC/Hollis/Java并发/9、线程数设定成多少更合适？.md)

1. 核心线程数与最大线程数不能写死为固定值，而是使用 `Runtime.getRuntime().availableProcessors()` 动态计算
2. 对于日常任务量较少，少数时间任务量较大的情况，核心线程数与最大线程数应按照比例设置，避免浪费
3. 对于任务数量较为稳定的情况，为了避免线程创建和销毁的开销，可以将核心线程数与最大线程数设置为相同的数值，这样可以避免线程创建和销毁的开销，思想类似于JVM设置最大和最小堆内存

- 快捷算法
	- CPU密集型应用，执行复杂算法、大量循环等：`N + 1`
	- IO密集型应用，数据库、文件、网络读写传输等：`2N + 1`
	- 固定任务应用、例如每次计算30个机构的收益数据：固定数量即可
- 最佳公式:
	- 最佳线程数 = `((线程等待时间+线程CPU时间)/线程CPU时间+1) * CPU数目`；其中，线程等待时间可以是IO时间、网络等待时间、队列等待时间
	- 额外还需要考虑：内存、文件打开数、端口数等其他资源限制
- 压测算法
	- 模拟线程数被打满状态下的CPU利用率、磁盘IO、网络IO、内存等指标。 
	- 任务专用服务器，CPU、内存、IO等指标应该较高，充分利用的状态。 
	- 任务混用服务器，CPU、内存、IO等指标应适当，不能占用过多资源。

## 2.4、避免线程池死锁/饿死

1. 线程池中执行单一任务，不要在任务中再提交新任务到原线程池，造成循环阻塞
2. **设置任务超时机制**，任务中不要使用永久阻塞API
3. 使用ForkJoin线程池，替代普通线程池
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509121831303.png)

# 3、ForkJoin线程池

## 3.1、ForkJoinPool线程池是什么？

1. ForkJoinPool是自java7开始，JVM提供的一个用于并行执行的任务框架。其主旨是**将大任务分成若干小任务，之后再并行对这些小任务进行计算，最终汇总这些任务的结果**。得到最终的结果。其广泛用在java8的Stream中。
2. 这个描述实际上比较接近于单机版的`map-reduce`。都是**采用了分治算法**，将大的任务拆分到可执行的任务，之后并行执行，最终合并结果集。区别就在于ForkJoin机制可能只能在单个jvm上运行，而map-reduce则是在集群上执行。
3. 此外，ForkJoinPool采取**工作窃取算法**，以避免工作线程由于拆分了任务之后的join等待过程。这样处于空闲的工作线程将从其他工作线程的队列中主动去窃取任务来执行。这里涉及到的两个基本知识点是**分治法和工作窃取**。

## 3.2、ForkJoin核心思想 分治算法

**分治法的基本思想是将一个规模为N的问题分解为K个规模较小的子问题，这些子问题的相互独立且与原问题的性质相同，求出子问题的解之后，将这些解合并，就可以得到原有问题的解。是一种分目标完成的程序算法。**

将一个大的任务，通过 **`fork`方法** 不断拆解，直到能够计算为止，之后，再将这些结果用 **`join`方法** 合并。这样逐次递归，就得到了我们想要的结果。这就是再ForkJoinPool中的分治法。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509121836332.png)

## 3.3、ForkJoin核心思想 窃取算法

**当某个线程的任务队列中没有可执行任务的时候，从其他线程的任务队列中窃取任务来执行，以充分利用工作线程的计算能力，减少线程由于获取不到任务而造成的空闲浪费。**

在ForkJoinpool中，**==工作任务的队列都采用双端队列Deque容器==**。通常使用队列的过程中，我们都在队尾插入，而在队头消费以实现FIFO。而为了实现工作窃取。一般我们会改成 **==工作线程在自己的工作队列上LIFO==**，而 **==窃取其他线程的任务的时候，从队列头部取获取FIFO==**。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509121840652.png)

## 3.4、ForkJoinPool的企业级应用

> **==并行度 == 工作线程数==**

1. 使用`java.util.concurrent.ForkJoinPool`类的构造器
```java
// 并行度默认等于CPU核数
ForkJoinPool()

// 手动指定并行度
ForkJoinPool(int)

// 并行度、线程工厂、异常处理器、同步还是异步（指定队列是先进先出还是后进先出）
ForkJoinPool(int, ForkJoinWorkerThreadFactory, UncaughtExceptionHandler, asyncMode)
```

2. 使用`java.util.concurrent.Executors`类的快捷方法
```java
// 并行度默认等于CPU核数
newWorkStealingPool()

// 手动指定并行度
newWorkStealingPool(int)
```

`ForkJoin`比较适合可以执行可以分支的任务和递归任务，一般都要借助`ForkJoinTask`来执行

## 3.5、Parallel Strem 并行流

Stream不支持并行，Parallel Stream支持并行，编写简洁，并行流底层使用同一个`ForkJoinPool.common`公共线程池
要点：
- 并行流线程不安全，因此任务逻辑中一定要使用Atomic类、ConcurrentHashMap等并非类容器
- 避免执行大批量任务和阻塞类型任务
- 当`ForkJoinPool.common`处理不过来时，会使用提交任务的线程执行任务

用法：
- 集合.stream().parallel() 
- 集合.parallelStream()

企业中，对于信息查询类接口应用较多，例如某个页面要展示产品信息列表。包括产品名称、图片、库存、价格等信息。但是产品的图片来源于产品服务，库存来源于库存服务，价格来源于价格服务，还有部分信息来源于自己的数据库。这个时候就需要并行的去调用远程服务，组装为最终的结果。并行流由于其简单方便的特点，在企业中较为广泛的使用。
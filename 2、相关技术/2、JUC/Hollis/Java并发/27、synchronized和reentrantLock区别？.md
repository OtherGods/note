# 典型回答

ReentrantLock 和 synchronized 都是用于线程的同步控制，但它们在功能上来说差别还是很大的。对比下来 ReentrantLock 功能明显要丰富的多：

异同参考：[3. 两种锁的比较](2、相关技术/2、JUC/补7_并发编程的锁机制：synchronized和lock.md#3.%20两种锁的比较)

| 功能对比            | synchronized                                                                                     | ReentrantLock                                                                                                                                                                                 |
| --------------- | ------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 锁种类             | **==可重入锁==**、[非公平锁](2、相关技术/2、JUC/Hollis/Java并发/68、sychronized是非公平锁吗，那么是如何体现的？.md)                | **==可重入锁==**、公平锁、非公平锁、可中断锁                                                                                                                                                                    |
| 使用方式            | 隐式获取/释放锁                                                                                         | 显式调用`lock(`)/`unlock()`                                                                                                                                                                       |
| 遇到异常            | 自动释放锁，不会导致死锁发生                                                                                   | 如果没有主动通过`unLock()`去释放锁，那么不会释放锁，很可能造成死锁现象                                                                                                                                                      |
| 同步队列            | `ObjectMonitor`的`entrySet`                                                                       | AQS中的CLH先进先出队列                                                                                                                                                                                |
| 条件队列            | `ObjectMonitor`的`waitSet`；<br><br>通过 `Object.wait()`, `notify()`, `notifyAll()` 操作，一个锁只能有一个等待队列。 | 通过 `Condition.await()`, `signal()`, `signalAll()` 操作，一个锁可以创建多个 `Condition`。<br><br>可以实现更精细的线程通知和唤醒。例如，生产者-消费者模型中，可以分别唤醒等待“非空”和“非满”条件的线程，避免了无效的“全通知”（`notifyAll()`），从而减少了不必要的线程争用和上下文切换，提升了性能。 |
| 尝试获取锁、有等待事件的获取锁 | 无，获取锁失败后会一直阻塞                                                                                    | `tryLock()`、`tryLock(long, TimeUnit)`方法                                                                                                                                                       |
| 等待获取锁的线程是否可以被打断 | 获取锁失败后一直等待锁，等待时无法打断                                                                              | `lockInterruptibly()`方法                                                                                                                                                                       |

性能对比：`ReentrantLock`在竞争激烈或并发高的情况下性能更好，在竞争不激烈的情况下`synchronized`性能更好；在日常开发中，鉴于 `synchronized` 的简单性和在无竞争/低竞争场景下的卓越性能，它仍然是大多数情况下的首选。

| 性能对比     | synchronized                                          | ReentrantLock                                                                                                    |
| -------- | ----------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| 实现       | C++实现，完全依赖JVM内置的 **`ObjectMonitor`** 实现，与JVM深度绑定。     | 基于 **`AbstractQueuedSynchronizer (AQS)`** 这个Java类库实现的同步器框架。                                                      |
| 获取锁失败的线程 | **==直接被挂起==，进ObjectMonitor的entrySet**，线程状态变为 BLOCKED。 | **先尝试==CAS自旋==一定次数**，尝试获取锁。**失败后，才会将线程包装为Node节点==加入AQS队列==，并==可能被挂起==**。                                         |
| 挂起与唤醒    | 通过 **==操作系统内核的系统调用==** 来完成线程的挂起和唤醒                    | 挂起和唤醒线程使用 CAS + `LockSupport.park()`、`LockSupport.unpark()`。<br>虽然最终也可能涉及系统调用，但 **==与AQS的状态机制紧密结合，可以减少不必要的唤醒==** |
| 核心开销     | **==用户态到内核态的切换==**、**==线程上下文切换==**                    | **==CAS操作==**（用户态）、**==可能的用户态/内核态切换==**。在竞争不极端激烈时，CAS自旋可能成功，避免了昂贵的切换开销。                                          |

另外，随着JDK21的发布，虚拟线程已经推出，在虚拟线程中，不建议使用synchronized，而是建议用ReentrantLock。

[65、为什么虚拟线程不能用synchronized？](2、相关技术/2、JUC/Hollis/Java并发/65、为什么虚拟线程不能用synchronized？.md)

# 扩展知识

## ReentrantLock用法

Java语言直接提供了synchronized关键字用于加锁，但这种锁一是很重，二是获取时必须一直等待，没有额外的尝试机制。

java.util.concurrent.locks包提供的ReentrantLock用于替代synchronized加锁，ReentrantLock 内部是基于 AbstractQueuedSynchronizer（简称AQS）实现的。

ReentrantLock是可重入锁，它和synchronized一样，一个线程可以多次获取同一个锁。

用法：
```java
public class Counter {
	private final Lock lock = new ReentrantLock();
	private int count;
	public void add(int n) {
		lock.lock();
		try {
			count += n;
		} finally {
			lock.unlock();
		}
	}
}
```

### 怎么创建公平锁？

new ReentrantLock() 默认创建的为非公平锁，如果要创建公平锁可以使用 new ReentrantLock(true)。

### lock() 和 lockInterruptibly() 的区别

lock() 和 lockInterruptibly() 的**区别在于获取锁的途中如果所在的线程中断**，lock() 会忽略异常继续等待获取锁，而 lockInterruptibly() 则会抛出 InterruptedException 异常。

### tryLock() 

tryLock(5, TimeUnit.SECONDS) 表示获取锁的最大等待时间为 5 秒，期间会一直尝试获取，而不是等待 5 秒之后再去获取锁。

## ReentrantLock 如何实现可重入的

可重入锁指的是同一个线程中可以多次获取同一把锁。比如在JAVA中，当一个线程调用一个对象的加锁的方法后,还可以调用其他加同一把锁的方法，这就是可重入锁。

ReentrantLock 加锁的时候，看下当前持有锁的线程和当前请求的线程是否是同一个，一样就可重入了。 只需要简单得将state值加1，记录当前线程的重入次数即可。
```java
if (current == getExclusiveOwnerThread()) {
     int nextc = c + acquires;
     if (nextc < 0)
     	throw new Error("Maximum lock count exceeded");
     setState(nextc);
     return true;
 }
```

同时，在锁进行释放的时候，需要确保state=0的时候才能执行释放资源的动作，也就是说，一个可重入锁，重入了多少次，就得解锁多少次。
```java
protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
}
```

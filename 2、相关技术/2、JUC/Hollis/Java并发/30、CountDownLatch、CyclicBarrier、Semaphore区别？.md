#线程同步 #Semaphore #CountDownLatch #CyclicBarrier
# 典型回答

CountDownLatch、CyclicBarrier、Semaphore都是Java并发库中的同步辅助类，它们**都可以用来协调多个线程之间的执行**。

但是，它们三者之间还是有一些区别的：

**CountDownLatch是一个计数器**，它允许一个或多个线程等待其他线程完成操作。<font color="red" size=5>它通常用来实现一个线程等待其他多个线程完成操作之后再继续执行的操作。</font>

**CyclicBarrier是一个同步屏障**，它允许多个线程相互等待，直到到达某个公共屏障点，才能继续执行。<font color="red" size=5>它通常用来实现多个线程在同一个屏障处等待，然后再一起继续执行的操作。</font>

**Semaphore是一个计数信号量**，它允许多个线程同时访问共享资源，并通过计数器来控制访问数量。<font color="red" size=5>它通常用来实现一个线程需要等待获取一个许可证才能访问共享资源，或者需要释放一个许可证才能完成操作的操作。</font>

- CountDownLatch适用于**一个线程等待多个线程**完成操作的情况
- CyclicBarrier适用于**多个线程在同一个屏障处等待**
- Semaphore适用于**一个线程需要等待获取许可证才能访问共享**

使用CountDownLatch、CyclicBarrier、Semaphore实现线程协调：
[32、有三个线程T1,T2,T3如何保证顺序执行？](2、相关技术/2、JUC/Hollis/Java并发/32、有三个线程T1,T2,T3如何保证顺序执行？.md)


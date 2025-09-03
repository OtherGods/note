# 典型回答

**InheritableThreadLocal是用于主子线程之间参数传递的，但是，这种方式有一个问题，那就是必须要是在主线程中手动创建的子线程才可以，而现在池化技术非常普遍了，很多时候线程都是通过线程池进行创建和复用的，这时候InheritableThreadLocal就不行了。**

InheritableThreadLocal在线程池中失效的原因：（我分析的）
1. 在主线程中创建子线程时，会基于主线程的ThreadLocalMap创建一个新的ThreadLocalMap，和主线程中的ThreadLocalMap内容一样但 **==是两个对象==**，导致在主线程中对主线程中ThreadLocalMap做修改时子线程感知不到
2. 线程池中 **==线程复用==**

TransmittableThreadLocal是阿里开源的一个方案 （[开源地址](https://github.com/alibaba/transmittable-thread-local) ） ，这个类继承并加强InheritableThreadLocal类。用来实现线程之间的参数传递，一经常被用在以下场景中：
1. 分布式跟踪系统 或 全链路压测（即链路打标）
2. 日志收集记录系统上下文
3. Session级Cache
4. 应用容器或上层框架跨应用代码给下层SDK传递信息

`TransmittableThreadLocal` 的核心原理是：
1. **包装任务**：通过 `TtlRunnable`/`TtlCallable` 包装线程池提交的原始任务。
2. **捕获快照**：在包装时（提交任务时）捕获父线程上下文的快照。
3. **重现上下文**：在线程池线程执行任务**前**，将快照重现到该线程中。
4. **恢复现场**：在线程池线程执行任务**后**，恢复该线程的原始上下文。

使用时只需注意一点：**提交到线程池的任务必须是经过 `TtlRunnable` 或 `TtlCallable` 包装后的对象**。

# 扩展知识

## 使用方式

对照：[38.1、基于TTL 解决线程池中 ThreadLocal 线程无法共享的问题](2、相关技术/2、JUC/Hollis/Java并发/38.1、基于TTL%20解决线程池中%20ThreadLocal%20线程无法共享的问题.md)

先需要导入依赖：
```yml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>transmittable-thread-local</artifactId>
    <version>2.14.2</version>
</dependency>
```

### 父子线程

对于简单的父子线程之间参数传递，可以用以下方式：
```java
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();

// 在父线程中设置
context.set("value-set-in-parent");

// 在子线程中可以读取，值是"value-set-in-parent"
String value = context.get();
```

### 线程池

#### Runnable

如果在线程池中，可以用如下方式使用：
```java
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();

// 在父线程中设置
context.set("value-set-in-parent");

Runnable task = new RunnableTask();
// 额外的处理，生成修饰了的对象ttlRunnable
Runnable ttlRunnable = TtlRunnable.get(task);
executorService.submit(ttlRunnable);


// Task中可以读取，值是"value-set-in-parent"
String value = context.get();
```

#### Callable

```java
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();


// 在父线程中设置
context.set("value-set-in-parent");

Callable call = new CallableTask();
// 额外的处理，生成修饰了的对象ttlCallable
Callable ttlCallable = TtlCallable.get(call);
executorService.submit(ttlCallable);


// Call中可以读取，值是"value-set-in-parent"
```

#### 直接用于线程池

也可以直接用在线程池上，而不是Runnable和Callable上：
```java
ExecutorService executorService = ...
// 额外的处理，生成修饰了的对象executorService
executorService = TtlExecutors.getTtlExecutorService(executorService);

TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();


// 在父线程中设置
context.set("value-set-in-parent");

Runnable task = new RunnableTask();
Callable call = new CallableTask();
executorService.submit(task);
executorService.submit(call);


// Task或是Call中可以读取，值是"value-set-in-parent"
String value = context.get();
```


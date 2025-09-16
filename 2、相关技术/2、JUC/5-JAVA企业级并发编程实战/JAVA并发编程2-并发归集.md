# 1、Future模式

## 1.1、Future模式是什么？

张三去饭店吃饭
- 同步模式：张三去饭店吃饭，点完餐之后就站在出餐口等着，什么时候饭做好了再拿走用餐。
- Future模式：张三去饭店吃饭，点完餐之后，服务员给了他一张小票。然后张三就隔壁奶茶店买了一杯奶茶，20分钟之后才回来。然后他拿出小票，到出餐口取餐。如果饭做好了就拿走用餐。如果饭还没做好则原地等待。
- 回调模式：张三去饭店吃饭，点完餐之后，然后张三就隔壁奶茶店买了一杯奶茶，又回到饭店坐着刷手机。什么时候饭做好了，服务员通知他。

## 1.2、Future模式时序图

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509122055310.png)

## 1.3、Future模式的实现 FutureTask

FutureTask实现了Future模式，提供最简单的Future实现。 
- 弊端十分明显，要想获得异步的执行结果，只能轮询或者阻塞。
最佳实践
- 适合前后端同步类操作，缩短响应时间，让耗时的操作先使用FutureTask异步执行，然后去执行其他的同步操作。最后再将FutureTask的异步执行的结果进行组装。

## 1.4、Future模式 企业案例

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509122100316.png)

# 2、CompletionService

**==Future模式==**

## 2.1、 CompletionService 是什么


## 2.2、CompletionService 原理


## 2.3、CompletionService 企业案例


# 3、CompletableFuture

**==回调模式==**

## 3.1、为什么要使用 CompletableFuture

Future模式缺点：
- `Future`、`FutureTask`、`CompletionService`只能阻塞式的获取结果

回调模式优点：
1. `CompletableFuture` 实现了 `java.util.concurrent.Future` 接口，具备 `Future` 的所哟u特性
2. 基于JDK1.8的流式编程以及Lambda表达式等实现一元操作符、异步编程、事件驱动编程模型，支持异步回调
3. 可以用来实现多线程的串行关系、并行关系、聚合关系

## 3.2、CompletableFuture 经典场景

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202509122129623.png)

## 3.3、CompletableFuture 核心方法

1. 实例化
   - `supplyAsync`：有返回值，异步执行，使用ForkJoinPool.common线程池
   - `runAsync`：无返回值，异步执行，使用ForkJoinPool.common线程池
   - 构造函数创建对象 + `thenRun` 向线程池提交任务
     - `thenRun`：无返回值，异步执行，使用ForkJoinPool.common线程池
   其中 `runAsync` 和 `thenRun` 都没有返回值，需要配合一个存返回值的对象一起使用，在任务中向该对象中存输出
2. 获取结果
   - `get`
   - `getNow`
   - `join`
3. 异常捕获
   - `exceptionally`
4. 后续操作（串行操作）
   - `whenComplete`
   - `whenCompleteAsync`
   - `thenApply`：获取上一步的返回值作为这一步的入参；执行这个方法参数中任务的线程与执行上一步任务的线程是同一个
   - `thenApplyAsync`：相比于`thenApply`；执行这个方法参数中任务的线程可能是新的线程（如果上一步任务所在的线程空闲，执行这个方法参数中任务的线程可能与执行上一步任务的线程相同）
   - `thenAccept`：相比于`thenApply`，这个方法没有返回值
   - `thenAcceptAsync`
   - `handle`：无论前一步执行成功还是失败，都会执行`handle`，前面执行成功则`handle`方法参数对象的`apply`方法第二个参数为null，否则第二个参数不为null
   - `handleAsync`
5. 组合操作（并行操作）
   - `thenCombine`：A、B两个任务并行执行，都运行完毕后，使用A、B输出新的结果C
   - `thenCombineAsync`
   - `allOf`：多个任务并行，必须所有任务均执行完毕才能继续执行
   - `anyOf`：多个任务并行，任意1个完成即可






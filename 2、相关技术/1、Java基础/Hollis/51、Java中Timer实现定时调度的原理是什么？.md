# 典型回答

Java中的`Timer`类是一个 **==定时调度器==**，用于在指定的时间点执行任务。JDK 中`Timer`类的定义如下：
```java
public class Timer {
    /**
     * The timer task queue.  This data structure is shared with the timer
     * thread.  The timer produces tasks, via its various schedule calls,
     * and the timer thread consumes, executing timer tasks as appropriate,
     * and removing them from the queue when they're obsolete.
     */
    private final TaskQueue queue = new TaskQueue();

    /**
     * The timer thread.
     */
    private final TimerThread thread = new TimerThread(queue);
}
```

以上就是`Timer`中最重要的两个成员变量：
1. `TaskQueue`：一个 **==任务队列==，用于存储已计划的定时任务**。任务队列<u>按照任务的执行时间进行排序，确保最早执行的任务排在队列前面。在队列中的任务可能是一次性的，也可能是周期性的。</u>
2. `TimerThread`：**==`Timer` 内部的后台线程==，它负责扫描 `TaskQueue` 中的任务**，<u>检查任务的执行时间，然后在执行时间到达时执行任务的 `run()` 方法。</u>

任务的定时调度的核心代码就在`TimerThread`中：
```java
class TimerThread extends Thread {
    public void run() {
        try {
            mainLoop();
        } finally {
            synchronized (queue) {
                newTasksMayBeScheduled = false;
                queue.clear(); 
            }
        }
    }
	
    /**
     * 主要的计时器循环。 
     */
    private void mainLoop() {
        while (true) {
            try {
                TimerTask task;
                boolean taskFired;
                synchronized (queue) {
                    // 等待队列变为非空
                    while (queue.isEmpty() && newTasksMayBeScheduled)
                        queue.wait();
                    if (queue.isEmpty())
                        break; // 队列为空，将永远保持为空；线程终止

                    // 队列非空；查看第一个事件并执行相应操作
                    long currentTime, executionTime;
                    task = queue.getMin();
                    synchronized (task.lock) {
                        if (task.state == TimerTask.CANCELLED) {
                            queue.removeMin();
                            continue;  // 无需执行任何操作，再次轮询队列
                        }
                        currentTime = System.currentTimeMillis();
                        executionTime = task.nextExecutionTime;
                        if (taskFired = (executionTime <= currentTime)) {
                            if (task.period == 0) { // 非重复，移除
                                queue.removeMin();
                                task.state = TimerTask.EXECUTED;
                            } else { // 重复任务，重新安排
                                queue.rescheduleMin(
                                  task.period < 0 ? currentTime   - task.period
                                                : executionTime + task.period);
                            }
                        }
                    }
                    if (!taskFired) // 任务尚未触发；等待
                        queue.wait(executionTime - currentTime);
                }
                if (taskFired)  // 任务触发；运行它，不持有锁
                    task.run();
            } catch (InterruptedException e) {
            }
        }
    }
}
```

# 扩展知识

## 优缺点

`Timer` 类用于实现定时任务，**最大的好处就是他的实现非常简单，特别的轻量级**，因为它是Java内置的，所以只需要简单调用就行了。

但是他并不是特别的解决定时任务的好的方案，因为他存在以下问题：
1. **`Timer`内部是单线程执行任务**的，如果某个任务执行时间较长，会影响后续任务的执行。
2. 如果**任务抛出未捕获异常，将导致整个 `Timer` 线程终止**，影响其他任务的执行。
3. **`Timer` 无法提供高精度的定时任务**。因为系统调度和任务执行时间的不确定性，可能导致任务执行的时间不准确。
4. **虽然可以使用 `cancel` 方法取消任务**，但这仅仅是将任务标记为取消状态，**仍然会在任务队列中占用位置，无法释放资源**。这**可能导致内存泄漏**。
5. 当有大量任务时，`Timer` 的性能可能受到影响，因为它在每次扫描任务队列时都要进行时间比较。
6. `Timer`执行任务完全基于JVM内存，一旦应用重启，那么队列中的任务就都没有了

#ThreadLocal 
# 典型回答

ThreadLocal是java.lang下面的一个类，是用来解决java多线程程序中并发问题的一种途径；通过为每一个线程创建一份共享变量的副本来保证各个线程之间的变量的访问和修改互相不影响；

ThreadLocal存放的值是线程内共享的，线程间互斥的，主要用于线程内共享一些数据，避免通过参数来传递，这样处理后，能够优雅的解决一些实际问题。

比如一次用户的页面操作请求，我们可以在最开始的filter中，把用户的信息保存在ThreadLocal中，在同一次请求中，在使用到用户信息，就可以直接到ThreadLocal中获取就可以了。

ThreadLocal有四个方法，分别为：
- initialValue
	- 返回此线程局部变量的初始值
- get
	- 返回此线程局部变量的当前线程副本中的值。如果这是线程第一次调用该方法，则创建并初始化此副本。
- set
	- 将此线程局部变量的当前线程副本中的值设置为指定值。许多应用程序不需要这项功能，它们只依赖于 initialValue() 方法来设置线程局部变量的值。
- remove
	- 移除此线程局部变量的值。

# 扩展知识

## ThreadLocal的实现原理

ThreadLocal中用于保存线程的独有变量的数据结构是一个内部类：ThreadLocalMap，也是k-v结构。
key就是当前的ThreadLocal对象，而v就是我们想要保存的值。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507032237590.png)

上图中基本描述出了Thread、ThreadLocalMap以及ThreadLocal三者之间的包含关系。

**Thread类对象中维护了ThreadLocalMap成员变量，而ThreadLocalMap维护了以ThreadLocal为key，需要存储的数据为value的Entry数组。** 这是它们三者之间的基本包含关系，我们需要进一步到源码中寻找踪迹。

查看Thread类，内部维护了两个变量，threadLocals和inheritableThreadLocals，它们的默认值是null，它们的类型是ThreadLocal.ThreadLocalMap，也就是ThreadLocal类的一个静态内部类ThreadLocalMap。

在静态内部类ThreadLocalMap维护一个数据结构类型为Entry的数组，节点类型如下代码所示：
```java
static class Entry extends WeakReference<ThreadLocal<?>> {
    /** The value associated with this ThreadLocal. */
    Object value;

    Entry(ThreadLocal<?> k, Object v) {
        super(k);
        value = v;
    }
}
```

从源码中我们可以看到，Entry结构实际上是继承了一个ThreadLocal类型的弱引用并将其作为key，value为Object类型。这里使用弱引用是否会产生问题，我们这里暂时不讨论，在文章结束的时候一起讨论一下，暂且可以理解key就是ThreadLocal对象。对于ThreadLocalMap，我们一起来了解一下其内部的变量：
```java
// 默认的数组初始化容量
private static final int INITIAL_CAPACITY = 16;
// Entry数组，大小必须为2的幂
private Entry[] table;
// 数组内部元素个数
private int size = 0;
// 数组扩容阈值，默认为0，创建了ThreadLocalMap对象后会被重新设置
private int threshold;
```

这几个变量和HashMap中的变量十分类似，功能也类似。
ThreadLocalMap的构造方法如下所示：
```java
/**
 * Construct a new map initially containing (firstKey, firstValue).
 * ThreadLocalMaps are constructed lazily, so we only create
 * one when we have at least one entry to put in it.
 */
ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
    // 初始化Entry数组，大小 16
    table = new Entry[INITIAL_CAPACITY];
    // 用第一个键的哈希值对初始大小取模得到索引，和HashMap的位运算代替取模原理一样
    int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
    // 将Entry对象存入数组指定位置
    table[i] = new Entry(firstKey, firstValue);
    size = 1;
    // 初始化扩容阈值，第一次设置为10
    setThreshold(INITIAL_CAPACITY);
}
```

## 应用场景

[57、ThreadLocal的应用场景有哪些？](2、相关技术/2、JUC/Hollis/Java并发/57、ThreadLocal的应用场景有哪些？.md)

## ThreadLocal内存泄露问题

[58、ThreadLocal为什么会导致内存泄漏？如何解决的？](2、相关技术/2、JUC/Hollis/Java并发/58、ThreadLocal为什么会导致内存泄漏？如何解决的？.md)

# 总结

## 四个类之间的关系

当 Thread 使用 ThreadLocal 的时候，以 set 方法为例，会发生：
- ThreadLocal 从 Thread 的 成员变量里获取或创建 ThreadLocalMap；
- 以当前 ThreadLocal 的 hashcode 生成 Entry 数组下标，以指向当前 ThreadLocal 的弱引用为 key，需与线程绑定的值为 value，添加到 ThreadLocalMap 内的 `Entry[]` 里；
因此，有以下数量关系表

|                                    |      |                                                                                                                                                             |
| ---------------------------------- | ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 概念                                 | 数量关系 | 原因                                                                                                                                                          |
| Thread 与 ThreadLocal               | 多对多  | 1个 Thread 可以同时使用多个 ThreadLocal，1个 ThreadLocal 也可以同时被多个 Thread 使用                                                                                            |
| Thread 与 ThreadLocalMap            | 1对1  | ThreadLocalMap 是 Thread 的成员变量                                                                                                                               |
| ThreadLocalMap 与 ThreadLocal       | 多对多  | 1个 Thread 可以同时使用多个 ThreadLocal，因此 ThreadLocalMap 需要记录多个 ThreadLocal；<br>多个ThreadMap也可以同时记录一个ThreadLocal；<br>因此才会用一个 `Entry[]` 数组来记录 ThreadLocal 与绑定的 value； |
| ThreadLocalMap.Entry 与 ThreadLocal | 多对1  | Entry 数据结构的 key 是指向 ThreadLocal 对象的弱引用，value 是需要绑定的变量；<br>多个ThreadLocalMap.Entry记录一个ThreadLocal；                                                            |
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507311129682.png)

![image-20230614194217813.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508021147968.png)

## 多个线程使用同一个ThreadLcoal

多个线程使用同一个ThreadLocal示例代码：（在代码注释中说要加断点位置加断点看多个线程使用同一个ThreadLocal效果）
```java
import java.util.concurrent.*;  

/**
* 在代码注释中说要加断点位置加断点看多个线程使用同一个ThreadLocal效果
*/
public class test  
{  
    public static void main(String[] args) throws InterruptedException  
    {  
        int count = 10;  
        CountDownLatch countDownLatch = new CountDownLatch(count);  
        ExecutorService executorService = Executors.newFixedThreadPool(count);  
        for(int i = 0; i < count; i++){  
            final int finalI = i;  
            executorService.execute(() -> {  
                TestThreadLocal TestThreadLocal = new TestThreadLocal();  
                TestThreadLocal.setThreadLocal(finalI + "");  
                countDownLatch.countDown();  
            });  
        }  
        countDownLatch.await();
        // 加多线程断点
        TestThreadLocal.threadLocal.set("end");
        executorService.shutdown();
    }
}  

class TestThreadLocal { 
	// 类静态变量，线程池中多个线程都使用这个类静态变量
    public static final ThreadLocal<String> threadLocal = new ThreadLocal<>();  
    
    public void setThreadLocal(String arg) {  
	    // 每个线程在这里第一次set值时，都会新创建一个ThreadLocalMap
	    // 在ThreadLocalMap有参构造函数加多线程断点
        threadLocal.set(arg);  
        System.out.println(threadLocal.get());  
    }  
}
```

验证过程：
1. 在`ThreadLocalMap`有参构造函数加多线程断点，启动程序
2. 10个线程都停在`ThreadLocalMap`有参构造函数中，可以看到构造函数传进来的`ThreadLocal`是同一个

## 一个线程使用多个ThreadLocal

当`thReadLocal1`被设置为null但未调用`threadLocal1.remove();`的情况下，可以使用`thReadLocal2`来清空`thReadLocal1`中key为null的value的引用及对应的`Entry`；示例代码：
```java
import java.lang.reflect.Field;  
import java.util.Arrays;  
import java.util.concurrent.ExecutorService;  
import java.util.concurrent.Executors;  
  
public class test {  
      
    static class LargeObject {  
        private final byte[] data = new byte[1024 * 1024 * 5]; // 5MB 大对象  
        @Override  
        public String toString() {  
            return "LargeObject@" + hashCode();  
        }  
          
    }  
      
    public static void main(String[] args) throws Exception {  
        // 创建线程池（核心场景：线程复用导致内存泄漏）  
        ExecutorService executor = Executors.newFixedThreadPool(1);  
        executor.submit(() -> {  
            ThreadLocal<LargeObject> threadLocal1 = new ThreadLocal<>();  
            ThreadLocal<LargeObject> threadLocal2 = new ThreadLocal<>();  
            try  
            {  
                forceHashCollision(threadLocal2, threadLocal1);  
            } catch (Exception e)  
            {  
                throw new RuntimeException(e);  
            }  
              
            // 1. 设置大对象到 threadLocal1            threadLocal1.set(new LargeObject());  
            System.out.println("【阶段1】设置 threadLocal1 的值");  
              
            // 2. 模拟 threadLocal1 不再使用（但未调用 remove()）  
            threadLocal1 = null; // 关键步骤：解除强引用  
            // 3. 强制触发 GC（尝试回收 threadLocal1 实例）  
            System.out.println("【阶段2】触发 GC 回收 ThreadLocal 实例");  
            System.gc();  
            try  
            {  
                Thread.sleep(1000); // 给 GC 时间  
            } catch (InterruptedException e)  
            {  
                throw new RuntimeException(e);  
            }  
              
            // 4. 检查 ThreadLocalMap 状态（应存在 key=null 的 Entry）  
            try  
            {  
                printThreadLocalMapState("GC后");  
            } catch (Exception e)  
            {  
                throw new RuntimeException(e);  
            }  
              
            // 5. 操作另一个 ThreadLocal 变量（触发惰性清理）  
            System.out.println("【阶段3】操作 threadLocal2 触发清理");  
              
            // 通过threadLocal2清空threadLocal1中key为空的value的引用  
            // 方式一：set，对照jdk8源码ThreadLocal#570行
            //threadLocal2.set(new LargeObject());            
            // 方式二：get，对照jdk8源码ThreadLocal#594行
            threadLocal2.get();  
              
            // 6. 再次检查 ThreadLocalMap 状态（key=null 的 Entry 应被清除）  
            try  
            {  
                printThreadLocalMapState("threadLocal2.set() 后");  
            } catch (Exception e)  
            {  
                throw new RuntimeException(e);  
            }  
              
            // 7. 显式调用 remove() 最佳实践  
            threadLocal2.remove();  
        });  
          
        Thread.sleep(3000);  
        executor.shutdown();  
    }  
      
    /**  
    * 强制制造哈希冲突，确保targetTl的哈希值等于originTl的哈希值  
    */  
    private static void forceHashCollision(ThreadLocal<?> originTl, ThreadLocal<?> targetTl)  
            throws Exception {  
          
        Field hashField = ThreadLocal.class.getDeclaredField("threadLocalHashCode");  
        hashField.setAccessible(true);  
        int targetHash = (int) hashField.get(targetTl);  
        hashField.set(originTl, targetHash);  
    }  
      
    // 反射获取当前线程的 ThreadLocalMap 并打印内部状态  
    private static void printThreadLocalMapState(String phase) throws Exception  
    {  
        Thread thread = Thread.currentThread();  
        Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");  
        threadLocalsField.setAccessible(true);  
          
        Object threadLocalMap = threadLocalsField.get(thread);  
        if (threadLocalMap == null) {  
            System.out.println(phase + " - ThreadLocalMap: null");  
            return;  
        }  
          
        // 获取 ThreadLocalMap 内部的 Entry 数组  
        Class<?> mapClass = threadLocalMap.getClass();  
        Field tableField = Arrays.stream(mapClass.getDeclaredFields())  
                .filter(f -> f.getName().equals("table"))  
                .findFirst()  
                .get();  
        tableField.setAccessible(true);  
        Object[] entries = (Object[]) tableField.get(threadLocalMap);  
          
        System.out.println("\n===== " + phase + " ThreadLocalMap 状态 =====");  
        int staleCount = 0;  
        for (int i = 0; i < entries.length; i++) {  
            Object entry = entries[i];  
            if (entry == null) continue;  
              
            // 获取 Entry 的 key 和 value            
            Field valueField = entry.getClass().getDeclaredField("value");  
            valueField.setAccessible(true);  
            Object value = valueField.get(entry);  
              
            Field referenceField = entry.getClass().getSuperclass().getSuperclass().getDeclaredField("referent");  
            referenceField.setAccessible(true);  
            Object key = referenceField.get(entry);  
              
            // 打印条目信息  
            String keyStatus = (key == null) ? "null (GC回收)" : key.getClass().getSimpleName() + "@" + key.hashCode();  
            String valueStatus = (value == null) ? "null" : value.toString();  
              
            System.out.printf("Slot %d: Key=%s, Value=%s\n", i, keyStatus, valueStatus);  
              
            // 统计 key=null 的 Entry            if (key == null) staleCount++;  
        }  
        System.out.printf(">> 存在 %d 个 key=null 的 Entry\n\n", staleCount);  
    }  
}
```

输出结果：
```java
【阶段1】设置 threadLocal1 的值
【阶段2】触发 GC 回收 ThreadLocal 实例

===== GC后 ThreadLocalMap 状态 =====
Slot 5: Key=@871923388, Value=[com.intellij.rt.debugger.agent.CaptureStorage$ExceptionCapturedStack@28a05356]
Slot 7: Key=ThreadLocal@315497418, Value=java.lang.ref.SoftReference@20fc48d4
Slot 10: Key=null (GC回收), Value=LargeObject@2090123542
Slot 14: Key=ThreadLocal@42739574, Value=java.lang.ref.SoftReference@171915db
>> 存在 1 个 key=null 的 Entry

【阶段3】操作 threadLocal2 触发清理

===== threadLocal2.set() 后 ThreadLocalMap 状态 =====
Slot 5: Key=@871923388, Value=[com.intellij.rt.debugger.agent.CaptureStorage$ExceptionCapturedStack@28a05356]
Slot 7: Key=ThreadLocal@315497418, Value=java.lang.ref.SoftReference@20fc48d4
Slot 10: Key=ThreadLocal@1978354873, Value=null
Slot 14: Key=ThreadLocal@42739574, Value=java.lang.ref.SoftReference@171915db
>> 存在 0 个 key=null 的 Entry

```
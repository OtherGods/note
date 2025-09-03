## 前言

大家好，我是**捡田螺的小男孩**。

无论是**工作还是面试**，我们都会跟`ThreadLocal`打交道，今天就跟大家聊聊`ThreadLocal`哈~

1. ThreadLocal是什么?为什么要使用ThreadLocal
2. 一个ThreadLocal的使用案例
3. ThreadLocal的原理
4. 为什么不直接用线程id作为ThreadLocalMap的key
5. 为什么会导致内存泄漏呢？是因为弱引用吗？
6. Key为什么要设计成弱引用呢？强引用不行？
7. InheritableThreadLocal保证父子线程间的共享数据
8. ThreadLocal的应用场景和使用注意点

- [github地址](https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2Fwhx123%2FJavaHome)，麻烦给个star鼓励一下，感谢感谢
- 公众号：捡田螺的小男孩（欢迎关注，干货多多）

## 1. ThreadLocal是什么?为什么要使用ThreadLocal？

### 1.1、**ThreadLocal是什么?**

`ThreadLocal`，即线程本地变量。如果你创建了一个`ThreadLocal`变量，那么访问这个变量的每个线程都会有这个变量的一个本地拷贝，<font color = "red" size = 4>**多个线程直接操作这个共享变量的时候，实际是在间接操作自己本地内存中独有的变量，从而起到线程隔离的作用，避免了并发场景下的线程安全问题**</font>。

ThreadLocal在使用过程种的状态：
![image-20221023145846820](D:\Tyora\AssociatedPicturesInTheArticles\补5_面试必备：ThreadLocal详解\image-20221023145846820.png)



```java
//创建一个ThreadLocal变量
static ThreadLocal<String> localVariable = new ThreadLocal<>();
复制代码
```

### 1.2、**为什么要使用ThreadLocal**

并发场景下，会存在多个线程同时修改一个共享变量的场景。这就可能会出现**线性安全问题**。

为了解决线性安全问题，可以用加锁的方式，比如使用`synchronized` 或者`Lock`。但是加锁的方式，可能会导致系统变慢。加锁示意图如下：

![image-20221023145002028](D:\Tyora\AssociatedPicturesInTheArticles\补5_面试必备：ThreadLocal详解\image-20221023145002028.png)

还有另外一种方案，就是使用空间换时间的方式，即使用`ThreadLocal`。使用`ThreadLocal`类访问共享变量时，会在每个线程的本地，都保存一份共享变量的拷贝副本。多线程对共享变量修改时，实际上操作的是这个变量副本，从而保证线性安全。

![image-20221023145201381](D:\Tyora\AssociatedPicturesInTheArticles\补5_面试必备：ThreadLocal详解\image-20221023145201381.png)

例如：两个线程执行i++操作，i初始化为0，两个线程分别对i进行++100次，在这个背景下分别分析无操作、使用synchronized锁、使用ThreadLocal三种情况。

1. 如果不加任何操作，仅仅在两个线程中循环判断i<100，并执行i++，那么最终导致i的值可能是2~100种的某个值；

   ```java
   static int i = 0;
   public static void main(String[] args) throws InterruptedException
   {
       new Thread(new Runnable(){
           @Override
           public void run()
           {
               while(i < 100){
                   i++;
               }
           }
       },"t1").start();
       new Thread(new Runnable(){
       
           @Override
           public void run()
           {
               while(i < 100){
                   i++;
               }
           }
       },"t2").start();
   }
   ```

   

2. 使用synchronized，可能会造成代码运行速度变慢；但是当使用ThreadLocal将加100次的操作平均分配给两个线程，每个线程对于独有的资源进行操作，这样即使线程之间交叉执行时也不会发生混乱，因为两个线程操作的不是共享资源，当每个线程执行完50次+1操作之后，将两个结果相加就得出了最终的操作。

### 1.3、ThreadLocal与Synchronized的区别

1. Synchronized用于线程之间的数据共享，而ThreadLocal用于线程间的数据隔离

   1. Synchronized是利用锁的机制，使变量或代码块在某一时刻只能被一个线程访问，保证多线程操作共享数据时不会出现问题。

      ThreadLocal为每一个线程都提供了变量的副本，使得每个线程在某一时间访问到的并不是同一个对象，这样就隔离了多个线程对数据的共享。

<font color = "blue" size = 4>我对ThreadLocal的理解：ThreadLocal对象是作为当前线程中属性ThreadLocalMap中某一个Entry（key，value）的key引用的对象；虽然不同线程可以使用相同的ThreadLocal对象，但是不同线程所拥有的ThreadLocalMap是独一无二的，也就是不同线程间同一个ThreadLocal（key）对应存储的值不一样，从而到达了线程间变量隔离的目的，但是在同一个线程种这个value变量地址是一样的。<font color = "red">多个线程间使用ThreadLocal类型的共享变量（指向内存中一个ThreadLocal对象）**【也就是ThreadLocalMap中Entry的key】**来操作不同线程间相互独立的变量**【也就是ThreadLocalMap中Entry的value】**</font></font>



## 2. 一个ThreadLocal的使用案例

日常开发中，`ThreadLocal`经常在日期转换工具类中出现，我们先来看个**反例**：

```typescript
/**
 * 日期工具类
 */
public class DateUtil {

    private static final SimpleDateFormat simpleDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static Date parse(String dateString) {
        Date date = null;
        try {
            date = simpleDateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
}
复制代码
```

我们在多线程环境跑`DateUtil`这个工具类：

```ini
public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 10; i++) {
            executorService.execute(()->{
                System.out.println(DateUtil.parse("2022-07-24 16:34:30"));
            });
        }
        executorService.shutdown();
    }
复制代码
```

运行后，发现报错了：

![image-20221023161918956](D:\Tyora\AssociatedPicturesInTheArticles\补5_面试必备：ThreadLocal详解\image-20221023161918956.png)

如果在`DateUtil`工具类，加上`ThreadLocal`，运行则不会有这个问题：

```typescript
/**
 * 日期工具类
 */
public class DateUtil {

    private static ThreadLocal<SimpleDateFormat> dateFormatThreadLocal =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    public static Date parse(String dateString) {
        Date date = null;
        try {
            date = dateFormatThreadLocal.get().parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 10; i++) {
            executorService.execute(()->{
                System.out.println(DateUtil.parse("2022-07-24 16:34:30"));
            });
        }
        executorService.shutdown();
    }
}
复制代码
```

运行结果：

```yaml
Sun Jul 24 16:34:30 GMT+08:00 2022
Sun Jul 24 16:34:30 GMT+08:00 2022
Sun Jul 24 16:34:30 GMT+08:00 2022
Sun Jul 24 16:34:30 GMT+08:00 2022
Sun Jul 24 16:34:30 GMT+08:00 2022
Sun Jul 24 16:34:30 GMT+08:00 2022
Sun Jul 24 16:34:30 GMT+08:00 2022
Sun Jul 24 16:34:30 GMT+08:00 2022
Sun Jul 24 16:34:30 GMT+08:00 2022
Sun Jul 24 16:34:30 GMT+08:00 2022
复制代码
```

刚刚**反例中**，为什么会报错呢？这是因为`SimpleDateFormat`不是线性安全的，它以共享变量出现时，并发多线程场景下即会报错。

为什么加了`ThreadLocal`就不会有问题呢？并发场景下，`ThreadLocal`是如何保证的呢？我们接下来看看`ThreadLocal`的核心原理。

## 3. ThreadLocal的原理

### 3.1 ThreadLocal的内存结构图

为了有个宏观的认识，我们先来看下`ThreadLocal`的内存结构图

![image-20221023161953749](D:\Tyora\AssociatedPicturesInTheArticles\补5_面试必备：ThreadLocal详解\image-20221023161953749.png)

从内存结构图，我们可以看到：

- `Thread`类中，有个`ThreadLocal.ThreadLocalMap`  的成员变量。
- `ThreadLocalMap`内部维护了`Entry`数组，每个`Entry`代表一个完整的对象，`key`是`ThreadLocal`本身，`value`是`ThreadLocal`的泛型对象值。

### 3.2 关键源码分析

对照着几段关键源码来看，更容易理解一点哈~我们回到`Thread`类源码，可以看到成员变量`ThreadLocalMap`的初始值是为`null`

```java
public class Thread implements Runnable {
   //ThreadLocal.ThreadLocalMap是Thread的属性
   ThreadLocal.ThreadLocalMap threadLocals = null;
}
复制代码
```

`ThreadLocalMap`的关键源码如下：

```scala
static class ThreadLocalMap {
    
    static class Entry extends WeakReference<ThreadLocal<?>> {
        /** The value associated with this ThreadLocal. */
        Object value;

        Entry(ThreadLocal<?> k, Object v) {
            super(k);
            value = v;
        }
    }
    //Entry数组
    private Entry[] table;
    
    // ThreadLocalMap的构造器，ThreadLocal作为key
    ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
        table = new Entry[INITIAL_CAPACITY];
        int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
        table[i] = new Entry(firstKey, firstValue);
        size = 1;
        setThreshold(INITIAL_CAPACITY);
    }
}
复制代码
```

`ThreadLocal`类中的关键`set()`方法：

```typescript
 public void set(T value) {
        Thread t = Thread.currentThread(); //获取当前线程t
        ThreadLocalMap map = getMap(t);  //根据当前线程获取到ThreadLocalMap
        if (map != null)  //如果获取的ThreadLocalMap对象不为空
            map.set(this, value); //K，V设置到ThreadLocalMap中
        else
            createMap(t, value); //创建一个新的ThreadLocalMap
    }
    
     ThreadLocalMap getMap(Thread t) {
       return t.threadLocals; //返回Thread对象的ThreadLocalMap属性
    }

    void createMap(Thread t, T firstValue) { //调用ThreadLocalMap的构造函数
        t.threadLocals = new ThreadLocalMap(this, firstValue); this表示当前类ThreadLocal
    }
    
复制代码
```

`ThreadLocal`类中的关键`get()`方法

```scss
    public T get() {
        Thread t = Thread.currentThread();//获取当前线程t
        ThreadLocalMap map = getMap(t);//根据当前线程获取到ThreadLocalMap
        if (map != null) { //如果获取的ThreadLocalMap对象不为空
            //由this（即ThreadLoca对象）得到对应的Value，即ThreadLocal的泛型值
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value; 
                return result;
            }
        }
        return setInitialValue(); //初始化threadLocals成员变量的值
    }
    
     private T setInitialValue() {
        T value = initialValue(); //初始化value的值
        Thread t = Thread.currentThread(); 
        ThreadLocalMap map = getMap(t); //以当前线程为key，获取threadLocals成员变量，它是一个ThreadLocalMap
        if (map != null)
            map.set(this, value);  //K，V设置到ThreadLocalMap中
        else
            createMap(t, value); //实例化threadLocals成员变量
        return value;
    }
复制代码
```

所以怎么回答**ThreadLocal的实现原理**？如下，最好是能结合以上结构图一起说明哈~

- `Thread`线程类有一个类型为`ThreadLocal.ThreadLocalMap`的实例变量`threadLocals`，即每个线程都有一个属于自己的`ThreadLocalMap`。
- `ThreadLocalMap`内部维护着`Entry`数组，每个`Entry`代表一个完整的对象，`key`是`ThreadLocal`本身，`value`是`ThreadLocal`的泛型值。
- 并发多线程场景下，每个线程`Thread`，在往`ThreadLocal`里设置值的时候，都是往自己的`ThreadLocalMap`里存，读也是以某个`ThreadLocal`作为引用，在自己的`map`里找对应的`key`，从而可以实现了**线程隔离**。

了解完这几个核心方法后，有些小伙伴可能会有疑惑，`ThreadLocalMap`为什么要用`ThreadLocal`作为key呢？直接用`线程Id`不一样嘛？

## 4. 为什么不直接用线程id作为ThreadLocalMap的key呢？

举个代码例子，如下：

```arduino
public class TianLuoThreadLocalTest {

    private static final ThreadLocal<String> threadLocal1 = new ThreadLocal<>();
    private static final ThreadLocal<String> threadLocal2 = new ThreadLocal<>();
 
}
复制代码
```

这种场景：一个使用类，有两个共享变量，也就是说用了两个`ThreadLocal`成员变量的话。如果用线程`id`作为`ThreadLocalMap`的`key`，怎么区分哪个`ThreadLocal`成员变量呢？因此还是需要使用`ThreadLocal`作为`Key`来使用。每个`ThreadLocal`对象，都可以由`threadLocalHashCode`属性**唯一区分**的，每一个ThreadLocal对象都可以由这个对象的名字唯一区分（**下面的例子**）。看下`ThreadLocal`代码：

```csharp
public class ThreadLocal<T> {
  private final int threadLocalHashCode = nextHashCode();
  
  private static int nextHashCode() {
    return nextHashCode.getAndAdd(HASH_INCREMENT);
  }
}
复制代码
```

然后我们再来看下一个代码例子：

```csharp
public class TianLuoThreadLocalTest {

    public static void main(String[] args) {
        Thread t = new Thread(new Runnable(){
            public void run(){
                ThreadLocal<TianLuoDTO> threadLocal1 = new ThreadLocal<>();
                threadLocal1.set(new TianLuoDTO("公众号：捡田螺的小男孩"));
                System.out.println(threadLocal1.get());
                ThreadLocal<TianLuoDTO> threadLocal2 = new ThreadLocal<>();
                threadLocal2.set(new TianLuoDTO("公众号：程序员田螺"));
                System.out.println(threadLocal2.get());
            }});
        t.start();
    }

}
//运行结果
TianLuoDTO{name='公众号：捡田螺的小男孩'}
TianLuoDTO{name='公众号：程序员田螺'}
复制代码
```

再对比下这个图，可能就更清晰一点啦：

![image-20221023162123071](D:\Tyora\AssociatedPicturesInTheArticles\补5_面试必备：ThreadLocal详解\image-20221023162123071.png)

## 5. TreadLocal为什么会导致内存泄漏呢？

### 5.1 弱引用导致的内存泄漏？

我们先来看看TreadLocal的引用示意图哈：

![image-20221023162142373](D:\Tyora\AssociatedPicturesInTheArticles\补5_面试必备：ThreadLocal详解\image-20221023162142373.png)

关于ThreadLocal内存泄漏，网上比较流行的说法是这样的：

> `ThreadLocalMap`使用`ThreadLocal`的**弱引用**作为`key`，当`ThreadLocal`变量被手动设置为`null`，即一个`ThreadLocal`没有外部强引用来引用它，当系统GC时，`ThreadLocal`一定会被回收。这样的话，`ThreadLocalMap`中就会出现`key`为`null`的`Entry`，就没有办法访问这些`key`为`null`的`Entry`的`value`，如果当前线程再迟迟不结束的话(比如线程池的核心线程)，这些`key`为`null`的`Entry`的`value`就会一直存在一条强引用链：Thread变量 -> Thread对象 -> ThreaLocalMap -> Entry -> value -> Object 永远无法回收，造成内存泄漏。

当ThreadLocal变量被手动设置为`null`后的引用链图：

![image-20221023162248825](D:\Tyora\AssociatedPicturesInTheArticles\补5_面试必备：ThreadLocal详解\image-20221023162248825.png)

实际上，`ThreadLocalMap`的设计中已经考虑到这种情况。所以也加上了一些防护措施：即在`ThreadLocal`的`get`,`set`,`remove`方法，都会清除线程`ThreadLocalMap`里所有`key`为`null`的`value`。

源代码中，是有体现的，如`ThreadLocalMap`的`set`方法：

```ini
  private void set(ThreadLocal<?> key, Object value) {

      Entry[] tab = table;
      int len = tab.length;
      int i = key.threadLocalHashCode & (len-1);

      for (Entry e = tab[i];
            e != null;
            e = tab[i = nextIndex(i, len)]) {
          ThreadLocal<?> k = e.get();

          if (k == key) {
              e.value = value;
              return;
          }

           //如果k等于null,则说明该索引位之前放的key(threadLocal对象)被回收了,这通常是因为外部将threadLocal变量置为null,
           //又因为entry对threadLocal持有的是弱引用,一轮GC过后,对象被回收。
            //这种情况下,既然用户代码都已经将threadLocal置为null,那么也就没打算再通过该对象作为key去取到之前放入threadLocalMap的value, 因此ThreadLocalMap中会直接替换调这种不新鲜的entry。
          if (k == null) {
              replaceStaleEntry(key, value, i);
              return;
          }
        }

        tab[i] = new Entry(key, value);
        int sz = ++size;
        //触发一次Log2(N)复杂度的扫描,目的是清除过期Entry  
        if (!cleanSomeSlots(i, sz) && sz >= threshold)
          rehash();
    }
复制代码
```

如ThreadLocal的`get`方法：

```ini
  public T get() {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        //去ThreadLocalMap获取Entry，方法里面有key==null的清除逻辑
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null) {
            @SuppressWarnings("unchecked")
            T result = (T)e.value;
            return result;
        }
    }
    return setInitialValue();
}

private Entry getEntry(ThreadLocal<?> key) {
        int i = key.threadLocalHashCode & (table.length - 1);
        Entry e = table[i];
        if (e != null && e.get() == key)
             return e;
        else
          //里面有key==null的清除逻辑
          return getEntryAfterMiss(key, i, e);
    }
        
private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
        Entry[] tab = table;
        int len = tab.length;

        while (e != null) {
            ThreadLocal<?> k = e.get();
            if (k == key)
                return e;
            // Entry的key为null,则表明没有外部引用,且被GC回收,是一个过期Entry
            if (k == null)
                expungeStaleEntry(i); //删除过期的Entry
            else
                i = nextIndex(i, len);
            e = tab[i];
        }
        return null;
    }
复制代码
```

### 5.2 key是弱引用，GC回收会影响ThreadLocal的正常工作嘛？

到这里，有些小伙伴可能有疑问，`ThreadLocal`的`key`既然是**弱引用**.会不会GC贸然把`key`回收掉，进而影响`ThreadLocal`的正常使用？

> - **弱引用**:具有弱引用的对象拥有更短暂的生命周期。如果一个对象只有弱引用存在了，则下次GC**将会回收掉该对象**（不管当前内存空间足够与否）

其实不会的，因为有`ThreadLocal变量`引用着它，是不会被GC回收的，除非手动把`ThreadLocal变量设置为null`，我们可以跑个demo来验证一下：

```csharp
  public class WeakReferenceTest {
    public static void main(String[] args) {
        Object object = new Object();
        WeakReference<Object> testWeakReference = new WeakReference<>(object);
        System.out.println("GC回收之前，弱引用："+testWeakReference.get());
        //触发系统垃圾回收
        System.gc();
        System.out.println("GC回收之后，弱引用："+testWeakReference.get());
        //手动设置为object对象为null
        object=null;
        System.gc();
        System.out.println("对象object设置为null，GC回收之后，弱引用："+testWeakReference.get());
    }
}
运行结果：
GC回收之前，弱引用：java.lang.Object@7b23ec81
GC回收之后，弱引用：java.lang.Object@7b23ec81
对象object设置为null，GC回收之后，弱引用：null
复制代码
```

结论就是，小伙伴放下这个疑惑了，哈哈~

### 5.3 ThreadLocal内存泄漏的demo

给大家来看下一个内存泄漏的例子，其实就是用线程池，一直往里面放对象

```csharp
public class ThreadLocalTestDemo {

    private static ThreadLocal<TianLuoClass> tianLuoThreadLocal = new ThreadLocal<>();


    public static void main(String[] args) throws InterruptedException {

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5, 5, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>());

        for (int i = 0; i < 10; ++i) {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println("创建对象：");
                    TianLuoClass tianLuoClass = new TianLuoClass();
                    tianLuoThreadLocal.set(tianLuoClass);
                    tianLuoClass = null; //将对象设置为 null，表示此对象不在使用了
                   // tianLuoThreadLocal.remove();
                }
            });
            Thread.sleep(1000);
        }
    }

    static class TianLuoClass {
        // 100M
        private byte[] bytes = new byte[100 * 1024 * 1024];
    }
}


创建对象：
创建对象：
创建对象：
创建对象：
Exception in thread "pool-1-thread-4" java.lang.OutOfMemoryError: Java heap space
	at com.example.dto.ThreadLocalTestDemo$TianLuoClass.<init>(ThreadLocalTestDemo.java:33)
	at com.example.dto.ThreadLocalTestDemo$1.run(ThreadLocalTestDemo.java:21)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
复制代码
```

运行结果出现了OOM，`tianLuoThreadLocal.remove();`加上后，则不会`OOM`。

```erlang
创建对象：
创建对象：
创建对象：
创建对象：
创建对象：
创建对象：
创建对象：
创建对象：
......
复制代码
```

我们这里**没有手动设置**`tianLuoThreadLocal`变量为`null`，但是还是**会内存泄漏**。因为我们使用了线程池，线程池有很长的生命周期，因此线程池会一直持有`tianLuoClass`对象的`value`值，即使设置`tianLuoClass = null;`引用还是存在的。这就好像，你把一个个对象`object`放到一个`list`列表里，然后再单独把`object`设置为`null`的道理是一样的，列表的对象还是存在的。

```typescript
    public static void main(String[] args) {
        List<Object> list = new ArrayList<>();
        Object object = new Object();
        list.add(object);
        object = null;
        System.out.println(list.size());
    }
    //运行结果
    1
复制代码
```

所以内存泄漏就这样发生啦，最后内存是有限的，就抛出了`OOM`了。如果我们加上`threadLocal.remove();`，则不会内存泄漏。为什么呢？因为`threadLocal.remove();`会清除`Entry`，源码如下：

```ini
    private void remove(ThreadLocal<?> key) {
      Entry[] tab = table;
      int len = tab.length;
      int i = key.threadLocalHashCode & (len-1);
      for (Entry e = tab[i];
          e != null;
          e = tab[i = nextIndex(i, len)]) {
          if (e.get() == key) {
              //清除entry
              e.clear();
            expungeStaleEntry(i);
            return;
        }
    }
}
复制代码
```

有些小伙伴说，既然内存泄漏不一定是因为弱引用，那为什么需要设计为弱引用呢？我们来探讨下：

## 6. Entry的Key为什么要设计成弱引用呢？

通过源码，我们是可以看到`Entry`的`Key`是设计为弱引用的(`ThreadLocalMap`使用`ThreadLocal`的弱引用作为`Key`的)。为什么要设计为弱引用呢？

![image-20221023163249865](D:\Tyora\AssociatedPicturesInTheArticles\补5_面试必备：ThreadLocal详解\image-20221023163249865.png)

我们先来回忆一下四种引用：

- **强引用**:我们平时`new`了一个对象就是强引用，例如` Object obj = new Object();`即使在内存不足的情况下，JVM宁愿抛出OutOfMemory错误也不会回收这种对象。
- **软引用**：如果一个对象只具有软引用，则内存空间足够，垃圾回收器就不会回收它；如果内存空间不足了，就会回收这些对象的内存。
- **弱引用**:具有弱引用的对象拥有更短暂的生命周期。如果一个对象只有弱引用存在了，则下次GC**将会回收掉该对象**（不管当前内存空间足够与否）。
- **虚引用**:如果一个对象仅持有虚引用，那么它就和没有任何引用一样，在任何时候都可能被垃圾回收器回收。虚引用主要用来跟踪对象被垃圾回收器回收的活动。

我们先来看看官方文档，为什么要设计为弱引用：

```vbnet
To help deal with very large and long-lived usages, the hash table entries use WeakReferences for keys.
为了应对非常大和长时间的用途，哈希表使用弱引用的 key。
复制代码
```

我再把ThreadLocal的引用示意图搬过来：

![image-20221023163409357](D:\Tyora\AssociatedPicturesInTheArticles\补5_面试必备：ThreadLocal详解\image-20221023163409357.png)

下面我们分情况讨论：

- 如果`Key`使用强引用：当`ThreadLocal`的对象被回收了，但是`ThreadLocalMap`还持有`ThreadLocal`的强引用的话，如果没有手动删除，ThreadLocal就不会被回收，会出现Entry的内存泄漏问题。
- 如果`Key`使用弱引用：当`ThreadLocal`的对象被回收了，因为`ThreadLocalMap`持有ThreadLocal的弱引用，即使没有手动删除，ThreadLocal也会被回收。`value`则在下一次`ThreadLocalMap`调用`set,get，remove`的时候会被清除。

<font color = red>**因此可以发现，使用弱引用作为`Entry`的`Key`，可以多一层保障：弱引用`ThreadLocal`不会轻易内存泄漏，对应的`value`在下一次该线程的另一个ThreadLocal对象【一个线程可以关联多个ThreadLocal对象，都作为Thread中ThreadLocalMap中的key】通过`ThreadLocalMap`调用`set,get,remove`的时候会被清除；这也就是key使用弱引用的好处，<font color="blue" size=5>简单概括就是可以通过只设置ThreadLocal类型的共享变量为null清除ThreadLocalMap中的key + 该线程另一个ThreadLocal对象释放ThreadLocalMap中key为null的value的方式，释放内存，而不需要手动去去删除除key和value达到释放内存的目的。</font>**</font>

实际上，我们的内存泄漏的根本原因是，不再被使用的`Entry`，没有从线程的`ThreadLocalMap`中删除。一般删除不再使用的`Entry`有这两种方式：

- 一种就是，使用完`ThreadLocal`，手动调用`remove()`，把`Entry从ThreadLocalMap`中删除
- 另外一种方式就是：`ThreadLocalMap`的自动清除机制去清除过期`Entry`.（`ThreadLocalMap`的`get(),set()`时都会触发对过期`Entry`的清除）

## 7. InheritableThreadLocal保证父子线程间的共享数据

我们知道`ThreadLocal`是线程隔离的，如果我们希望父子线程共享数据，如何做到呢？可以使用`InheritableThreadLocal`。先来看看`demo`：

```csharp
public class InheritableThreadLocalTest {

   public static void main(String[] args) {
       ThreadLocal<String> threadLocal = new ThreadLocal<>();
       InheritableThreadLocal<String> inheritableThreadLocal = new InheritableThreadLocal<>();

       threadLocal.set("关注公众号：捡田螺的小男孩");
       inheritableThreadLocal.set("关注公众号：程序员田螺");

       Thread thread = new Thread(()->{
           System.out.println("ThreadLocal value " + threadLocal.get());
           System.out.println("InheritableThreadLocal value " + inheritableThreadLocal.get());
       });
       thread.start();
       
   }
}
//运行结果
ThreadLocal value null
InheritableThreadLocal value 关注公众号：程序员田螺
复制代码
```

可以发现，在子线程中，是可以获取到父线程的 `InheritableThreadLocal `类型变量的值，但是不能获取到 `ThreadLocal `类型变量的值。

获取不到`ThreadLocal `类型的值，我们可以好理解，因为它是线程隔离的嘛。`InheritableThreadLocal `是如何做到的呢？原理是什么呢？

在`Thread`类中，除了成员变量`threadLocals`之外，还有另一个成员变量：`inheritableThreadLocals`。它们两类型是一样的：

```java
public class Thread implements Runnable {
   ThreadLocalMap threadLocals = null;
   ThreadLocalMap inheritableThreadLocals = null;
 }
复制代码
```

`Thread`类的`init`方法中，有一段初始化设置：

```typescript
 private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize, AccessControlContext acc,
                      boolean inheritThreadLocals) {
      
        ......
        if (inheritThreadLocals && parent.inheritableThreadLocals != null)
            this.inheritableThreadLocals =
                ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
        /* Stash the specified stack size in case the VM cares */
        this.stackSize = stackSize;

        /* Set thread ID */
        tid = nextThreadID();
    }
 static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }
复制代码
```

可以发现，当`parent的inheritableThreadLocals`不为`null`时，就会将`parent`的`inheritableThreadLocals`，赋值给前线程的`inheritableThreadLocals`。说白了，就是如果当前线程的`inheritableThreadLocals`不为`null`，就从父线程哪里拷贝过来一个过来，类似于另外一个`ThreadLocal`，但是数据从父线程那里来的。有兴趣的小伙伴们可以在去研究研究源码~

## 8. ThreadLocal的应用场景和使用注意点

`ThreadLocal`的**很重要一个注意点**，就是使用完，要手动调用`remove()`。

而`ThreadLocal`的应用场景主要有以下这几种：

- 使用日期工具类，当用到`SimpleDateFormat`，使用ThreadLocal保证线性安全
- 全局存储用户信息（用户信息存入`ThreadLocal`，那么当前线程在任何地方需要时，都可以使用）。
- 保证同一个线程，获取的数据库连接`Connection`是同一个，使用`ThreadLocal`来解决线程安全的问题，而不同线程使用的是不同的Connection。
- 使用`MDC`保存日志信息。



> 仅供参考——>关于使用场景的介绍可以去看笔记：[补6_史上最全ThreadLocal 详解（一）.md](D:\c_51CTO资料\Java\JUC\补6_史上最全ThreadLocal 详解（一）.md)

## 参考与感谢

- [彻底理解ThreadLocal](https://link.juejin.cn?target=https%3A%2F%2Fwww.cnblogs.com%2Fxzwblog%2Fp%2F7227509.html)
- [ThreadLocal是如何导致内存泄漏的](https://link.juejin.cn?target=https%3A%2F%2Fzhuanlan.zhihu.com%2Fp%2F346291694)
- [深入分析 ThreadLocal 内存泄漏问题](https://link.juejin.cn?target=https%3A%2F%2Fwww.jianshu.com%2Fp%2F1342a879f523)



作者：捡田螺的小男孩
链接：https://juejin.cn/post/7126708538440679460
来源：稀土掘金
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
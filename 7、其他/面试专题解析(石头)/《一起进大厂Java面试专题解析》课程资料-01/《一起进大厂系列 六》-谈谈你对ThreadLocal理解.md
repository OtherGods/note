

[TOC]



## 1.ThreadLocal初识

ThreadLocal概念：线程局部变量。是一种多线程间并发访问某一个变量的解决方案。与Synchronized等加锁的方式有所不同，ThreadLocal完全不提供锁。而是使用以空间换时间的手段，为每个线程提供变量的独立副本，以保障线程的安全。

从性能上来说，ThreadLocal不具有绝对的优势，在并发不是很高的情况下，加锁的性能会更好，但是作为一套与锁完全无关的线程安全解决方案，在高并发或者是竞争激烈的场景，使用ThreadLocal可以在一定的情况下减少锁竞争的问题。

**示例1：**
```java
/**
 * ThreadLocal 线程局部变量
 */
public class ThreadLocalTest {

    public static ThreadLocal<String> th = new ThreadLocal<String>();

    public void setTh(String value) {
        th.set(value);
    }

    public void getTh() {
        System.out.println(Thread.currentThread().getName() + ":" + this.th.get());
    }

    public static void main(String[] args) throws InterruptedException {

        final ThreadLocalTest ct = new ThreadLocalTest();
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                ct.setTh("张三");
                ct.getTh();
            }
        }, "t1");

        Thread t2 = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);
                    ct.getTh();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "t2");

        t1.start();
        t2.start();
    }

}
```
**运行结果：**
```xml
t1:张三
t2:null
```
>服务程序是由进程构成，进程是由无数个线程构成，线程是一组代码片段组成。在Java的多线程编程中，为保证多个线程对共享变量的安全访问，通常会使用synchronized来保证同一时刻只有一个线程对共享变量进行操作。这种情况下可以将类变量放到ThreadLocal类型的对象中，使变量在每个线程中都有独立拷贝，不会出现一个线程读取变量时而被另一个线程修改的现象。
## 2.ThreadLocal底层原理
下图为ThreadLocal的内部结构图
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210703185020165.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210703185157661.png)


ThreadLocal结构内部

从上面的结构图，我们已经窥见ThreadLocal的核心机制：

>每个线程都会有一个局部变量 threadLocals，存放在各自线程栈帧局部变量表中，指向堆中的ThreadLocalMap实例对象
不同的线程在堆中对应不同的ThreadLocalMap实例对象。ThreadLocalMap的key是ThreadLocal实例对象

所以对于不同的线程，每次获取副本值时，别的线程并不能获取到当前线程的副本值，形成了副本的隔离，互不干扰。

**Thread线程内部的Map在类中描述如下：**

```java
public class Thread implements Runnable {
    /* ThreadLocal values pertaining to this thread. This map is maintained
     * by the ThreadLocal class. */
    ThreadLocal.ThreadLocalMap threadLocals = null;
}
```

**每个线程拥有各自的ThreadLocalMap实例对象**

在threadLocal set值的时候，若threadLocalMap为null，new一个ThreadLocalMap对象。所以每个线程都是新new的ThreadLocalMap对象，堆中是不同的实例。

```java
 void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
}
```
## 3.ThreadLocal核心API


ThreadLocal类提供如下几个核心方法

```java
public T get()
public void set(T value)
public void initialValue()
public void remove()

get()方法用于获取当前线程的副本变量值。
set()方法用于保存当前线程的副本变量值。
initialValue()为当前线程初始副本变量值。
remove()方法移除当前前程的副本变量值。
```
### 3.1.get()方法

```java
/**
 * Returns the value in the current thread's copy of this
 * thread-local variable.  If the variable has no value for the
 * current thread, it is first initialized to the value returned
 * by an invocation of the {@link #initialValue} method.
 *
 * @return the current thread's value of this thread-local
 */
public T get() {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null)
            return (T)e.value;
    }
    return setInitialValue();
}

ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;
}

private T setInitialValue() {
    T value = initialValue();
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null)
        map.set(this, value);
    else
        createMap(t, value);
    return value;
}

protected T initialValue() {
    return null;
}
```
步骤：

```
1.获取当前线程的ThreadLocalMap对象threadLocals
2.从map中获取线程存储的K-V Entry节点。
3.从Entry节点获取存储的Value副本值返回。
4.map为空的话返回初始值null，即线程变量副本为null，在使用时需要注意判断NullPointerException。
```
### 3.2.set()方法

```java
/**
 * Sets the current thread's copy of this thread-local variable
 * to the specified value.  Most subclasses will have no need to
 * override this method, relying solely on the {@link #initialValue}
 * method to set the values of thread-locals.
 *
 * @param value the value to be stored in the current thread's copy of
 *        this thread-local.
 */
public void set(T value) {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null)
        map.set(this, value);
    else
        createMap(t, value);
}

ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;
}

void createMap(Thread t, T firstValue) {
    t.threadLocals = new ThreadLocalMap(this, firstValue);
}
```
步骤：
```
1.获取当前线程的成员变量map
2.map非空，则重新将ThreadLocal和新的value副本放入到map中。
3.map空，则对线程的成员变量ThreadLocalMap进行初始化创建，并将ThreadLocal和value副本放入map中。
```
### 3.3.remove()方法

```java
/**
 * Removes the current thread's value for this thread-local
 * variable.  If this thread-local variable is subsequently
 * {@linkplain #get read} by the current thread, its value will be
 * reinitialized by invoking its {@link #initialValue} method,
 * unless its value is {@linkplain #set set} by the current thread
 * in the interim.  This may result in multiple invocations of the
 * <tt>initialValue</tt> method in the current thread.
 *
 * @since 1.5
 */
public void remove() {
 ThreadLocalMap m = getMap(Thread.currentThread());
 if (m != null)
     m.remove(this);
}

ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;
}
```
remove方法比较简单，不做赘述。

### 3.4.核心代码及流程
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210703191254848.png)
## 4.ThreadLocalMap
`ThreadLocalMap`是ThreadLocal的内部类，没有实现Map接口，用独立的方式实现了Map的功能，其内部的Entry也独立实现。

**ThreadLocalMap类图**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200221172611411.png)
在`ThreadLocalMap`中，也是用Entry来保存K-V结构数据的。但是Entry中key只能是ThreadLocal对象，这点被Entry的构造方法已经限定死了。

>ThreadLocalMap数据结构是一个Entry数组，Entry的key是ThreadLocal类型，value是Objcet；
>Entry的一个key只对应一个value值，即一个线程中一个ThreadLocal实例中只能存一个数据（value），不同与HashMap等
>
>Entry数组初始容量为16
>ThreadLocalMap的负载因子为2/3，超过阈值便进行扩容

```java
public class ThreadLocal<T> {
    static class ThreadLocalMap {
        // 主要因为key为ThreadLocal，所以继承弱引用
        static class Entry extends WeakReference<ThreadLocal<?>> {
            Object value;
            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }
        //数组初始容量
        private static final int INITIAL_CAPACITY = 16;
        private Entry[] table;
        private int size = 0;
        //阈值
        private int threshold; // Default to 0
        //负载因子
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        //初始数组容量、初始数组下标及元素、初始扩容阈值
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            table = new Entry[INITIAL_CAPACITY];
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
            setThreshold(INITIAL_CAPACITY);
        }
    }
}
```
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
Entry继承自WeakReference（弱引用，生命周期只能存活到下次GC前），但只有Key是弱引用类型的，Value并非弱引用。

## 5.Hash冲突怎么解决
和HashMap的最大的不同在于，ThreadLocalMap结构非常简单，没有next引用，也就是说ThreadLocalMap中解决Hash冲突的方式并非链表的方式，而是采用线性探测的方式，所谓线性探测，就是根据初始key的hashcode值确定元素在table数组中的位置，如果发现这个位置上已经有其他key值的元素被占用，则利用固定的算法寻找一定步长的下个位置，依次判断，直至找到能够存放的位置。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210703192616356.png)
使用CAS，每次增加固定的值，所以采用的是线性探测法解决HasH冲突
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210703232513554.png)

ThreadLocalMap解决Hash冲突的方式就是简单的步长加1或减1，寻找下一个相邻的位置。


显然ThreadLocalMap采用线性探测的方式解决Hash冲突的效率很低，如果有大量不同的ThreadLocal对象放入map中时发送冲突，或者发生二次冲突，则效率很低。

所以这里引出的良好建议是：**每个线程只存一个变量，这样的话所有的线程存放到map中的Key都是相同的ThreadLocal，如果一个线程要保存多个变量，就需要创建多个ThreadLocal，多个ThreadLocal放入Map中时会极大的增加Hash冲突的可能。**

## 6.ThreadLocal内存泄漏问题及解决办法

`ThreadLocal` 在 `ThreadLocalMap` 中是以一个弱引用身份被 `Entry` 中的 `Key` 引用的，因此如果 `ThreadLocal` 没有外部强引用来引用它，那么 `ThreadLocal` 会在下次 JVM 垃圾收集时被回收。这个时候 Entry 中的 key 已经被回收，但是 value 又是一强引用不会被垃圾收集器回收，这样 ThreadLocal 的线程如果一直持续运行，value 就一直得不到回收，这样就会发生内存泄露。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210703232827671.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210703233114995.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210703233239217.png)

**解决办法**

1.使用完后记得remove

2.ThreadLocal自己提供了这个问题的解决方案。

每次操作set、get、remove操作时，会相应调用 `ThreadLocalMap` 的三个方法，`ThreadLocalMap`的三个方法在每次被调用时 都会直接或间接调用一个 `expungeStaleEntry`() 方法，这个方法会将key为null的 `Entry` 删除，从而避免内存泄漏。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210703233853795.png)

3. 使用static修饰ThreadLocal

还可以使用 static 修饰ThreadLocal，保证ThreadLocal为强引用，也就能保证任何时候都能通过ThreadLocal的弱引用访问到Entry的value值，进而清除掉 。
>原因：根据可达性算法分析，类静态属性引用的对象可作为GC Roots根节点，即保证了ThreadLocal为不可回收对象

## 7.应用场景
还记得Hibernate的session获取场景吗？

```java
private static final ThreadLocal<Session> threadLocal = new ThreadLocal<Session>();

//获取Session
public static Session getCurrentSession(){
    Session session =  threadLocal.get();
    //判断Session是否为空，如果为空，将创建一个session，并设置到本地线程变量中
    try {
        if(session ==null&&!session.isOpen()){
            if(sessionFactory==null){
                rbuildSessionFactory();// 创建Hibernate的SessionFactory
            }else{
                session = sessionFactory.openSession();
            }
        }
        threadLocal.set(session);
    } catch (Exception e) {
        // TODO: handle exception
    }

    return session;
}
```
为什么？每个线程访问数据库都应当是一个独立的Session会话，如果多个线程共享同一个Session会话，有可能其他线程关闭连接了，当前线程再执行提交时就会出现会话已关闭的异常，导致系统异常。此方式能避免线程争抢Session，提高并发下的安全性。

使用ThreadLocal的典型场景正如上面的数据库连接管理，线程会话管理等场景，只适用于独立变量副本的情况，如果变量为全局共享的，则不适用在高并发下使用。

## 8.总结
每个ThreadLocal只能保存一个变量副本，如果想要上线一个线程能够保存多个副本以上，就需要创建多个ThreadLocal。

ThreadLocal内部的ThreadLocalMap键为弱引用，会有内存泄漏的风险。

适用于无状态，副本变量独立后不影响业务逻辑的高并发场景。如果如果业务逻辑强依赖于副本变量，则不适合用ThreadLocal解决，需要另寻解决方案。


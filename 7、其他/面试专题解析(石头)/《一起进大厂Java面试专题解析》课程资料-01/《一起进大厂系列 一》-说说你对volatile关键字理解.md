[TOC]

volatile是Java提供的轻量级的同步机制，主要有三个特性：

>1.保证内存可见性
2.不保证原子性
3.禁止指令重排序

## 1.保证内存可见性
volatile是Java提供的轻量级的同步机制，保证了**可见性**，不保证**原子性**。了解volatile工作机制，首先要对Java内存模型（JMM）有初步的认识：

- 每个线程创建时，JVM会为其创建一份私有的工作内存（栈空间），不同线程的工作内存之间不能直接互相访问。JMM规定所有的变量都存在主内存，主内存是共享内存区域，所有线程都可以访问
- 线程对变量进行读写，会从主内存拷贝一份副本到自己的工作内存，操作完毕后刷新到主内存。所以，线程间的通信要通过主内存来实现。

**volatile的作用是**：线程对副本变量进行修改后，其他线程能够立刻同步刷新最新的数值。这个就是**可见性**。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201210223309377.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

## 2.可见性验证
我们来看一个例子：

```java
package com.bruceliu.demo15;

import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: Thread0509
 * @BelongsPackage: com.bruceliu.demo15
 * @Author: bruceliu
 * @QQ:1241488705
 * @CreateTime: 2020-05-13 23:16
 * @Description: TODO
 */
public class VolatileDemo {

    int x = 0;
    //注意：这里的b没有被volatile修饰
    boolean b = false;

    /**
     * 写操作
     */
    private void write() {
        x = 5;
        b = true;
        System.out.println("x=>" + x);
        System.out.println("b =>" + b);
    }

    /**
     * 读操作
     */
    private void read() {
        //如果b=false的话，就会无限循环，直到b=true才会执行结束，会打印出x的值
        while (!b) {

        }
        System.out.println("x=" + x);
    }

    public static void main(String[] args) throws Exception {
        final VolatileDemo volatileDemo = new VolatileDemo();

        //线程1执行写操作
        Thread thread1 = new Thread(new Runnable() {
            public void run() {
                volatileDemo.write();
            }
        });

        //线程2执行读操作
        Thread thread2 = new Thread(new Runnable() {
            public void run() {
                volatileDemo.read();
            }
        });

        //我们让线程2的读操作先执行
        thread2.start();

        //睡1毫秒，为了保证线程2比线程1先执行
        TimeUnit.MILLISECONDS.sleep(1);

        //再让线程1的写操作执行
        thread1.start();

        thread1.join();
        thread2.join();

        //等待线程1和线程2全部结束后，打印执行结束
        System.out.println("执行结束");
    }
}
```
注意我们的b没有用volatile修饰，我们先启动了线程2的读操作，后启动了线程1的写操作，由于线程1和线程2会保存x和b的副本到自己的工作内存中，线程2执行后，由于他副本b=false，所以会进入到无限循环中，线程1执行后修改的也是自己副本中的b=true，然而线程2无法立即察觉到，所以执行上面代码后，不会打印“执行结束”，因为线程2一直在执行！
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210430110611970.png)

运行之后会一直出于运行状态，并且没有打印“执行结束”

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200513232714638.png)
此时的流程会是这样子

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210430110707147.png)

给b加了volatile关键字修饰后，线程1对b做了修改，然后会立即更新内存中的值，线程2通过嗅探发现自己的副本已经过期了，然后重新从内存中拿到b=true的值，然后跳出while循环，执行结束！

>我们知道volatile关键字的作用是保证变量在多线程之间的可见性，它是java.util.concurrent包的核心，没有volatile就没有这么多的并发类给我们使用.
## 3.原子性验证
看下面一段代码，number变量加了volatile修饰。创建了10个子线程，每个线程循环1000次执行number++。

```java
package com.bruce.demo8;

import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: SingleTon-2020
 * @BelongsPackage: com.bruce.demo8
 * @CreateTime: 2020-12-10 23:08
 * @Description: TODO
 */
public class Demo8 {

    static class MyTest {
        public volatile int number = 0;
        public void incr(){
            number++;
        }
    }


    public static void main(String[] args) {
        MyTest myTest = new MyTest();

        for (int i = 1; i <= 10; i++){

            new Thread(() -> {
                for (int j = 1; j <= 1000; j++){
                    myTest.incr();
                }
            }, "Thread"+String.valueOf(i)).start();
        }

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //等线程执行结束了，输出number值
        System.out.println("当前number：" + myTest.number);
    }
}

```
按理说number最终应该是10000，但是这边执行后，结果如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020121023125816.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
## 4.原子性问题解决
方法一：使用 synchronized 关键字

```java
//给函数增加synchronized修饰，相当于加锁了
 public synchronized void incr(){
     number++;
 }
```
结果如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201210231426923.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
方法二：使用AtomicInteger

```java
public class Demo8 {

    static class MyTest {

        public volatile AtomicInteger number = new AtomicInteger();
        
        public void incr(){
            number.getAndIncrement();
        }
    }


    public static void main(String[] args) {
        MyTest myTest = new MyTest();

        for (int i = 1; i <= 10; i++){

            new Thread(() -> {
                for (int j = 1; j <= 1000; j++){
                    myTest.incr();
                }
            }, "Thread"+String.valueOf(i)).start();
        }

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //等线程执行结束了，输出number值
        System.out.println("当前number：" + myTest.number);
    }
}
```

## 5.禁止指令重排序
体现了JMM的有序性
## 6.JMM谈谈你的理解
### 6.1.基本概念
JMM 本身是一种抽象的概念并不是真实存在，它描述的是一组规定或则规范，通过这组规范定义了程序中的访问方式。

### 6.2.JMM同步规定
#### 6.2.1.可见性
**可见性**：一个线程对某一共享变量修改之后，另一个线程要立即获取到修改后的结果。

- 线程解锁前，必须把共享变量的值刷新回主内存
- 线程加锁前，必须读取主内存的最新值到自己的工作内存
- 加锁解锁是同一把锁

由于 JVM 运行程序的实体是线程，而每个线程创建时 JVM 都会为其创建一个工作内存，工作内存是每个线程的私有数据区域，而 Java 内存模型中规定所有变量的储存在主内存，主内存是共享内存区域，所有的线程都可以访问，但线程对变量的操作（读取赋值等）必须都工作内存进行看。

首先要将变量从主内存拷贝的自己的工作内存空间，然后对变量进行操作，操作完成后再将变量写回主内存，不能直接操作主内存中的变量，工作内存中存储着主内存中的变量副本拷贝，前面说过，工作内存是每个线程的私有数据区域，因此不同的线程间无法访问对方的工作内存，线程间的通信(传值)必须通过主内存来完成。

内存模型图
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210627234009635.png)
#### 6.2.2.原子性
**原子性：** 不可分割，完整性，也就是说某个线程正在做某个具体业务时，中间不可以被加塞或者被分割，需要具体完成，要么同时成功，要么同时失败。

数据库也经常提到事务具备原子性!
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210627235256429.png)
>在putfield时，其他线程挂起，没有及时得到主内存的值改变消息

n++在多线程下是非线程安全的，如何不加synchronized解决？
- 使用原子类(java.util.concurrent.atomic)
- 为什么使用了原子类可以解决？原理是什么？CAS
#### 6.2.3.有序性
计算机在执行程序时，为了提高性能，编译器和处理器常常会对指令做重排，一般分一下3种：

>源代码->编译器优化的重排->指令并行的重排->内存系统的重排->最终执行的指令

单线程环境里面确保程序最终执行结果和代码顺序执行的结果一致。

处理器在进行重排序时必须考虑指令之间的数据依赖性。

多线程环境中线程交替执行，由于编译器优化重排的存在，两个线程中使用的变量能否保证一致性是无法确定的，结果无法预测。

**指令重排 - example 1**

```java
public void mySort() {
    
	int x = 11;   // 1
    int y = 12;   // 2
    
	x = x + 5;  // 3
     y = x * x;  // 4
	
}
```
按照正常单线程环境，执行顺序是 1 2 3 4
但是在多线程环境下，可能出现以下的顺序：

```
2 1 3 4

1 3 2 4
```
上述的过程就可以当做是指令的重排，即内部执行顺序，和我们的代码顺序不一样。但是指令重排也是有限制的，即不会出现下面的顺序

```
4 3 2 1
```
因为处理器在进行重排时候，必须考虑到指令之间的**数据依赖性**

因为步骤 4：需要依赖于 y的申明，以及x的申明，故因为存在数据依赖，无法首先执行

**例子**
int a,b,x,y = 0

| 线程1 |线程2  |
|--|--|
| x = a; |y = b;  |
| b = 1; |a = 2;  |
| x = 0; y = 0 |  |

因为上面的代码，不存在数据的依赖性，因此编译器可能对数据进行重排
| 线程1 |线程2  |
|--|--|
| b = 1; |a = 2;  |
| x = a; |y = b; |
| x = 2; y = 1 |  |

这样造成的结果，和最开始的就不一致了，这就是导致重排后，结果和最开始的不一样，因此为了防止这种结果出现，volatile就规定禁止指令重排，为了保证数据的一致性

**指令重排 - example 2**
比如下面这段代码

```java
public class ResortSeqDemo {
    int a= 0;
    boolean flag = false;

    public void method01() {
        a = 1;
        flag = true;
    }

    public void method02() {
        if(flag) {
            a = a + 5;
            System.out.println("reValue:" + a);
        }
    }
}
```
我们按照正常的顺序，分别调用method01() 和 method02() 那么，最终输出就是 a = 6

但是如果在多线程环境下，因为方法1 和 方法2，他们之间不能存在数据依赖的问题，因此原先的顺序可能是

```java
a = 1;
flag = true;

a = a + 5;
System.out.println("reValue:" + a);
```
但是在经过编译器，指令，或者内存的重排后，可能会出现这样的情况

```java
flag = true;

a = a + 5;
System.out.println("reValue:" + a);

a = 1;
```
也就是先执行 flag = true后，另外一个线程马上调用方法2，满足 flag的判断，最终让a + 5，结果为5，这样同样出现了数据不一致的问题

为什么会出现这个结果：多线程环境中线程交替执行，由于编译器优化重排的存在，两个线程中使用的变量能否保证一致性是无法确定的，结果无法预测。

**这样就需要通过volatile来修饰，来禁止指令重排序保证线程安全性**

### 6.3.Volatile针对指令重排做了啥
Volatile实现禁止指令重排优化，从而避免了多线程环境下程序出现乱序执行的现象
首先了解一个概念，内存屏障（Memory Barrier）又称内存栅栏，是一个CPU指令，它的作用有两个：

- 保证特定操作的顺序
- 保证某些变量的内存可见性（利用该特性实现volatile的内存可见性）

由于编译器和处理器都能执行指令重排的优化，如果在指令间插入一条Memory Barrier则会告诉编译器和CPU，不管什么指令都不能和这条Memory Barrier指令重排序，也就是说 **通过插入内存屏障禁止在内存屏障前后的指令执行重排序优化**。 内存屏障另外一个作用是刷新出各种CPU的缓存数，因此任何CPU上的线程都能读取到这些数据的最新版本。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210628172835759.png)
也就是过在Volatile的写 和 读的时候，加入屏障，防止出现指令重排的
## 7.你在哪些地方用过Volatile？

工作内存与主内存同步延迟现象导致的可见性问题

- 可通过synchronized或volatile关键字解决，他们都可以使一个线程修改后的变量立即对其它线程可见，对于指令重排导致的可见性问题和有序性问题
- 可以使用volatile关键字解决，因为volatile关键字的另一个作用就是禁止重排序优化

举例：

```java
public class LazySafe {
    private LazySafe(){
    }
    //对象加上了volatile关键字是为了保证变量的可见性，防止指令重排序
    //第二个线程拿到的可能是半实列化的对象，所以要加volatile防止指令重排序
    private volatile static LazySafe lazySafe;

    public static  LazySafe getInstance(){
        if(lazySafe==null){
            //双重判定
            synchronized (LazySafe.class){
                if(lazySafe==null){
                    lazySafe=new LazySafe(); //不是原子性的！
                }
            }
        }
        return lazySafe;
    }
}
```
DCL（双端检锁）机制不一定是线程安全的，原因是有指令重排的存在，加入volatile可以禁止指令重排

原因是在某一个线程执行到第一次检测的时候，读取到 instance 不为null，instance的引用对象可能没有完成实例化。因为 instance = new SingletonDemo()；可以分为以下三步进行完成：

```java
memory = allocate(); // 1、分配对象内存空间
instance(memory); // 2、初始化对象
instance = memory; // 3、设置instance指向刚刚分配的内存地址，此时instance != null
```

但是我们通过上面的三个步骤，能够发现，步骤2 和 步骤3之间不存在 数据依赖关系，而且无论重排前 还是重排后，程序的执行结果在单线程中并没有改变，因此这种重排优化是允许的。

```java
memory = allocate(); // 1、分配对象内存空间
instance = memory; // 3、设置instance指向刚刚分配的内存地址，此时instance != null，但是对象还没有初始化完成
instance(memory); // 2、初始化对象
```

这样就会造成什么问题呢？

也就是当我们执行到重排后的步骤2，试图获取instance的时候，会得到null，因为对象的初始化还没有完成，而是在重排后的步骤3才完成，因此执行单例模式的代码时候，就会重新在创建一个instance实例

指令重排只会保证串行语义的执行一致性（单线程），但并不会关系多线程间的语义一致性

所以当一条线程访问instance不为null时，由于instance实例未必已初始化完成，这就造成了线程安全的问题

所以需要引入volatile，来保证出现指令重排的问题，从而保证单例模式的线程安全性！

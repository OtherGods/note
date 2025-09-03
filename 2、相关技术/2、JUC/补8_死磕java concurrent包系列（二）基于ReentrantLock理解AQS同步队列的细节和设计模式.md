## 前言

之前介绍过并发问题的解决方式就是一般通过锁，concurrent包中最重要的接口就是lock接口，它可以显示的获取或者释放锁，对于lock接口来说最常见的实现就是ReetrantLock（可重入锁），而ReetrantLock的实现又离不开AQS。

AQS是concurrent包中最核心的并发组件，在读本文之前建议先阅读：

https://juejin.cn/post/6844903728781197320 彻底理解CAS机制，因为CAS在整个ReetrantLock中随处可见，它是lock的基础。

网上有许多类似文章，但是这一部分的东西比较抽象，需要不断理解，本文将基于源码分析concurrent包的最核心的组件AQS，将不好理解的部分尽量用图片来分析彻底理解ReetrantLock的原理

这部分是concurrent包的核心，理解了之后再去理解SemaPhore LinkedBlockingQueue ArrayBlockingQueue 等就信手拈来了，所以会花比较多的篇幅

## 重入锁ReetrantLock

### Lock接口

先大概看一看lock接口

```csharp
public interface Lock {
    // 加锁
    void lock();
    // 可中断获取锁，获取锁的过程中可以中断。
    void lockInterruptibly() throws InterruptedException;
    //立即返回的获取锁，返回true表示成功，false表示失败
    boolean tryLock();
    //根据传入时间立即返回的获取锁，返回true表示成功，false表示失败
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
    //解锁
    void unlock();
    //获取等待的条件队列（之后会详细分析）
    Condition newCondition();
}复制代码
```

而我们一般使用ReetrantLock：









```csharp
Lock lock = new ReentrantLock(); 
lock.lock(); 
try{ 
 //业务代码...... 
}finally{ 
 lock.unlock(); 
}复制代码
```

它在使用上是比较简单的，在正式分析之前我们先看看什么是公平锁和非公平锁

### 公平锁和非公平锁

公平锁是指多个线程按照申请锁的顺序来获取锁，线程直接进入FIFO队列，队列中的第一个线程才能获得锁。

用一个打水的例子来理解：

![image-20221126201236265](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201236265.png)



**公平锁的优点是等待锁的线程不会夯死。缺点是吞吐效率相对非公平锁要低，等待队列中除第一个线程以外的所有线程都会阻塞，CPU唤醒阻塞线程的开销比非公平锁大。**



非公平锁是多个线程加锁时直接尝试获取锁，获取不到才会到等待队列的队尾等待。但如果此时锁刚好可用，那么这个线程可以无需阻塞直接获取到锁，所以非公平锁有可能出现后申请锁的线程先获取锁的场景。

![image-20221126201257162](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201257162.png)

**非公平锁的优点是可以减少唤起线程的开销（因为可能有的线程可以直接获取到锁，CPU也就不用唤醒它），所以整体的吞吐效率高。缺点是处于等待队列中的线程可能会夯死（试想恰好每次有新线程来，它恰巧都每次获取到锁，此时还在排队等待获取锁的线程就悲剧了****），或者等很久才会获得锁。**

### 总结

公平锁和非公平锁的差异在于是否按照申请锁的顺序来获取锁，非公平锁可能会出现有多个线程等待时，有一个人品特别的好的线程直接没有等待而直接获取到了锁的情况，他们各有利弊;ReetrantLock在构造时默认是非公平的，可以通过参数控制。

## ReetrantLock与AQS

这里以ReentrantLock为例，简单讲解ReentrantLock与AQS的关系

![image-20221126201313228](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201313228.png)

![image-20221126201328838](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201328838.png)

从上图我们可以总结：

1. ReetrantLock:实现了lock接口，它的内部类有Sync、NonfairSync、FairSync（他们三个是继承了AQS），创建构造ReetrantLock时可以指定是非公平锁(NonfairSync)还是公平锁(FairSync)
2. Sync:他是抽象类，也是ReetrantLock内部类，实现了tryRelease方法，tryAccquire方法由它的子类NonfairSync、FairSync自己实现。
3. AQS：它是一个抽象类，但是值得注意的是它代码中却没有一个抽象方法，其中获取锁(tryAcquire方法)和释放锁(tryRelease方法)也没有提供默认实现，需要子类重写这两个方法实现具体逻辑（典型的模板方法设计模式）。
4. Node：AQS的内部类，本质上是一个双向链表，用来管理获取锁的线程（后续详细解读）

### 这样设计的结构有几点好处：

\1. 首先为什么要有Sync这个内部类呢？

 因为无论是NonfairSync还是FairSync，他们解锁的过程是一样的，不同只是加锁的过程，Sync提供加锁的模板方法让子类自行实现

\2. AQS为什么要声明为Abstract，内部却没有任何abstract方法？

这是因为AQS只是作为一个基础组件，从上图可以看出countDownLatch,Semaphore等并发组件都依赖了它，它并不希望直接作为直接操作类对外输出，而更倾向于作为一个基础并发组件，为真正的实现类提供基础设施，例如构建同步队列，控制同步状态等。

AQS是采用模板方法的设计模式，它作为基础组并发件，封装了一层核心并发操作（比如获取资源成功后封装成Node加入队列，对队列双向链表的处理），但是实现上分为两种模式，即**共享模式(如Semaphore)与独占模式（如ReetrantLock，这两个模式的本质区别在于多个线程能不能共享一把锁）**，而这两种模式的加锁与解锁实现方式是不一样的，但AQS只关注内部公共方法实现并不关心外部不同模式的实现，所以提供了模板方法给子类使用：例如：

ReentrantLock需要自己实现tryAcquire()方法和tryRelease()方法，而实现共享模式的Semaphore，则需要实现tryAcquireShared()方法和tryReleaseShared()方法，这样做的好处？因为无论是共享模式还是独占模式，其基础的实现都是同一套组件(AQS)，只不过是加锁解锁的逻辑不同罢了，更重要的是如果我们需要自定义锁的话，也变得非常简单，只需要选择不同的模式实现不同的加锁和解锁的模板方法即可。

### 总结

ReetrantLock:实现了lock接口，内部类有Sync、NonfairSync、FairSync（他们三个是继承了AQS）这里用了模板方法的设计模式。

### Node和AQS工作原理

之前介绍AQS是提供基础设施，如构建同步队列，控制同步状态等，它的工作原理是怎样的呢？

我们先看看AQS类中几个重要的字段：







```java
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer{
//指向同步队列队头
private transient volatile Node head;

//指向同步的队尾
private transient volatile Node tail;

//同步状态，0代表锁未被占用，1代表锁已被占用
private volatile int state;

//省略.
}
复制代码
```



再看看Node这个内部类：**它是对每一个访问同步代码块的线程的封装**

关于等待状态，我们暂时只需关注SIGNAL 和初始化状态即可

![image-20221126201346192](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201346192.png)

AQS本质上就是由node构成的双向链表，内部有node head和node tail。

AQS通过定义的state字段来控制同步状态,当state=0时，说明没有锁资源被站东，当state=1时，说明有线程目前正在使用锁的资源，这个时候其他线程必须加入同步队列进行等待；

既然要加入队列，那么AQS是内部通过内部类Node构成FIFO的同步队列实现线程获取锁排队，同时利用内部类ConditionObject构建条件队列，当调用condition.wait()方法后，线程将会加入条件队列中，而当调用signal()方法后，线程将从条件队列移动到同步队列中进行锁竞争。注意这里涉及到两种队列，一种的**同步队列**，当锁资源已经被占用，而又有线程请求锁而等待的后将加入同步队列等待，而另一种则是**条件队列**(可有多个)，通过Condition调用await()方法释放锁后，将加入等待队列。

条件队列可以暂时先放一边，下一节再详细分析，因为当我们调用ReetrantLock.lock()方法时，实际操作的是基于node结构的同步队列，此时AQS中的**state**变量则是代表**同步状态**，加锁后，如果此时state的值为0，则说明当前线程可以获取到锁，同时将state设置为1，表示获取成功。如果调用ReetrantLock.lock()方法时state已为1，也就是当前锁已被其他线程持有，那么当前执行线程将被封装为Node结点加入同步队列等待。

![image-20221126201401951](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201401951.png)

### 总结

如上图所示为AQS的同步队列模型；

1. AQS内部有一个由Node组成的同步队列，它是双向链表结构
2. AQS内部通过state来控制同步状态,当执行lock时，如果state=0时，说明没有任何线程占有共享资源的锁，此时线程会获取到锁并把state设置为1；当state=1时，则说明有线程目前正在使用共享变量，其他线程必须加入同步队列进行等待.

AQS内部分为共享模式(如Semaphore)和独占模式(如ReetrantLock)，无论是共享模式还是独占模式的实现类，都维持着一个虚拟的同步队列，当请求锁的线程超过现有模式的限制时，会将线程包装成Node结点并将线程当前必要的信息存储到node结点中，然后加入同步队列等会获取锁，而这系列操作都有AQS协助我们完成，这也是作为基础组件的原因，无论是Semaphore还是ReetrantLock，其内部绝大多数方法都是间接调用AQS完成的。

接下来我们看详细实现

## 基于ReetrantLock分析AQS独占锁模式的实现

### 非公平锁的获取锁

AQS的实现依赖于内部的同步队列(就是一个由node构成的FIFO的双向链表对列)来完成对同步状态(state)的管理，当前线程获取锁失败时，AQS会将该线程封装成一个Node并将其加入同步队列，同时会阻塞当前线程，当同步资源释放时，又会将头结点head中的线程唤醒，让其尝试获取同步状态。这里从ReetrantLock入手分析AQS的具体实现，我们先以非公平锁为例进行分析。

### 获取锁

来看ReetrantLock的源码：







```csharp
//默认构造，创建非公平锁NonfairSync
public ReentrantLock() {
    sync = new NonfairSync();
}
//根据传入参数创建锁类型
public ReentrantLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
}

//加锁操作
public void lock() {
     sync.lock();
}

复制代码
```



这里说明ReetrantLock默认构造方法就是构造一个非公平锁，调用lock方法时候：







```scala
/**
 * 非公平锁实现
 */
static final class NonfairSync extends Sync {
    //加锁
    final void lock() {
        //执行CAS操作，本质就是CAS更新state：
        //判断state是否为0，如果为0则把0更新为1，并返回true否则返回false
        if (compareAndSetState(0, 1))
       //成功则将独占锁线程设置为当前线程  
          setExclusiveOwnerThread(Thread.currentThread());
        else
            //否则再次请求同步状态
            acquire(1);
    }
}

复制代码
```



也就是说，通过CAS机制保证并发的情况下只有一个线程可以成功将state设置为1，获取到锁；

此时，其它线程在执行compareAndSetState时，因为state此时不是0，所以会失败并返回false，执行acquire(1);

### 加入同步队列







```scss
public final void acquire(int arg) {
    //再次尝试获取同步状态
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}

复制代码
```



这里传入参数arg是state的值，因为要获取锁，而status为0时是释放锁，1则是获取锁，所以这里一般传递参数为1，进入方法后首先会执行tryAcquire(1)方法，在前面分析过该方法在AQS中并没有具体实现，而是交由子类实现，因此该方法是由**ReetrantLock**类**内部类实现**的







```scala
//NonfairSync类
static final class NonfairSync extends Sync {

    protected final boolean tryAcquire(int acquires) {
         return nonfairTryAcquire(acquires);
     }
 }
复制代码
```



![image-20221126201431859](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201431859.png)

假设有三个线程：线程1已经获得到了锁，线程2正在同步队列中排队，此时线程3执行lock方法尝试获取锁的时，线程1正好释放了锁，将state更新为0，那么线程3就可能在线程2还没有被唤醒之前去获取到这个锁。

如果此时还没有获取到锁（nonfairTryAcquire返回false），那么接下来会把该线程封装成node去同步队列里排队，代码层面上执行的是acquireQueued(addWaiter(Node.EXCLUSIVE), arg)

ReetrantLock为独占锁，所以传入的参数为Node.EXCLUSIVE







```ini
private Node addWaiter(Node mode) {
    //将请求同步状态失败的线程封装成结点
    Node node = new Node(Thread.currentThread(), mode);

    Node pred = tail;
    //如果是第一个结点加入肯定为空，跳过。
    //如果非第一个结点则直接执行CAS入队操作，尝试在尾部快速添加
    if (pred != null) {
        node.prev = pred;
        //使用CAS执行尾部结点替换，尝试在尾部快速添加
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    //如果第一次加入或者CAS操作没有成功执行enq入队操作
    enq(node);
    return node;
}
复制代码
```



其中tail是AQS的成员变量，指向队尾(这点前面的我们分析过AQS维持的是一个双向的链表结构同步队列)，如果第一次获取到锁，AQS还没有初始化，则为tail肯定为空，那么将执行enq(node)操作，如果非第一个结点即tail指向不为null，直接**尝试执行CAS操作加入队尾**（再一次使用CAS操作实现线程安全），如果**CAS操作失败或第一次加入同步队列**还是会执行enq(node)，继续看enq(node)：







```ini
private Node enq(final Node node) {
    //死循环
    for (;;) {
         Node t = tail;
         //如果队列为null，即没有头结点
         if (t == null) { // Must initialize
             //创建并使用CAS设置头结点
             if (compareAndSetHead(new Node()))
                 tail = head;
         } else {//队尾添加新结点
             node.prev = t;
             if (compareAndSetTail(t, node)) {
                 t.next = node;
                 return t;
             }
         }
     }
}
复制代码
```



这个方法使用一个死循环进行CAS操作，可以解决多线程并发问题。这里做了两件事：

一是队列不存在的创建新结点并初始化tail、head：使用compareAndSetHead设置头结点，head和tail都指向head。

二是队列已存在，则将新结点node添加到队尾。

**注意****addWaiter****和****enq****这两个方法都存在同样的代码将线程设置为同步队列的队尾：**

```ini
             node.prev = t;
             if (compareAndSetTail(t, node)) {
                 t.next = node;
                 return t;
             }复制代码
```

这是因为，在多线程环境中，假设线程1、2、3、4同时执行addWaiter()方法入队，而此时头节点不为null，那么他们会同时执行addWaiter中的compareAndSetTail方法将队尾指向它，添加到队尾。

但这个时候CAS操作保证只有一个可以成功，假设此时线程1成功添加到队尾，那么线程2、3、4执行CAS都会失败，那么线程2、3、4会在enq这个方法内部**死循环执行compareAndSetTail**方法将队尾指向它，直到成功添加到队尾为止。enq这个方法在内部对并发情况下进行补偿。

### 自旋

回到之前的`acquire()`方法，添加到同步队列后，结点就会进入一个自旋过程，自旋的意思就是原地转圈圈：即结点都在观察时机准备获取同步状态,自旋过程是在`acquireQueued(addWaiter(Node.EXCLUSIVE), arg))`方法中执行的，先看前半部分







```ini
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        //自旋，死循环
        for (;;) {
            //获取前结点
            final Node p = node.predecessor();
            当且仅当p为头结点才尝试获取同步状态,FIFO
            if (p == head && tryAcquire(arg)) {
                //此时当前node前驱节点为head且已经tryAcquire获取到了锁，正在执行了它的相关信息
                //已经没有任何用处了，所以现在需要考虑将它GC掉
                //将node设置为头结点
                setHead(node);
                //清空原来头结点的引用便于GC
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            //如果前驱结点不是head，判断是否挂起线程
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;       
        }
    } finally {
        if (failed)
            //最终都没能获取同步状态，结束该线程的请求
            cancelAcquire(node);
    }
}
复制代码
```









```ini
//设置为头结点
private void setHead(Node node) {
        head = node;
        //清空结点数据以便于GC
        node.thread = null;
        node.prev = null;
}
复制代码
```



死循环中，如果满足了if (p == head && tryAcquire(arg))

如下图，会执行sethead方法：

![image-20221126201454220](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201454220.png)

当然如果前驱结点不是head而它又没有获取到锁，那么执行如下：







```arduino
//如果前驱结点不是head，判断是否挂起线程
if (shouldParkAfterFailedAcquire(p, node) &&parkAndCheckInterrupt())

      interrupted = true;
}

private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        //获取当前结点的等待状态
        int ws = pred.waitStatus;
        //如果为等待唤醒（SIGNAL）状态则返回true
        if (ws == Node.SIGNAL)
            return true;
        //如果ws>0 则说明是结束状态，
        //遍历前驱结点直到找到没有结束状态的结点
        if (ws > 0) {
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            //如果ws小于0又不是SIGNAL状态，
            //则将其设置为SIGNAL状态，代表该结点的线程正在等待唤醒。
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

private final boolean parkAndCheckInterrupt() {
        //将当前线程挂起,线程会阻塞住
        LockSupport.park(this);
        //获取线程中断状态,interrupted()是判断当前中断状态，
        //并非中断线程，因此可能true也可能false,并返回
        return Thread.interrupted();
}

复制代码
```



这段代码有个设计比较好的点：

通常我们在设计队列时，我们需要考虑如何最大化的减少后续排队节点对于CPU的消耗，而在AQS中，只要当前节点的前驱节点不是头结点，再把当前节点加到队列后就会执行LockSupport.park(this);将当前线程挂起，这样可以最大程度减少CPU消耗。

### 总结：

是不是还是有点一头雾水？

没关系，为了方便理解：我们假设ABC三个线程现在同时去获取锁，A首先获取到锁后一直不释放，BC加入队列。那么对于AQS的同步队列结构是如何变化的呢？

**1、A直接获取到锁:**

代码执行路径：

(ReetranLock.lock()-> compareAndSetState(0, 1) -> setExclusiveOwnerThread(Thread.currentThread())

此时AQS结构还没有初始化：

![image-20221126201512571](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201512571.png)

**2、B尝试获取锁：**

因为A存在把state设置为1，所以B获取锁失败，进行入队操作加入同步队列，入队时发现AQS还没有初始化(AQS中的tail为null)，会在入队前初始化代码在enq方法的死循环中：

![image-20221126201524280](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201524280.png)

初始化之后改变tail的prev指向，把自己加到队尾：

![image-20221126201536684](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201536684.png)

接着会执行acquireQueued方法：

```ini
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}复制代码
```

第一次执行：发现自己前序节点是head节点，于是乎再次尝试获取锁，获取失败后再shouldParkAfterFailedAcquire方法中把前序节点设置为Singal状态

第二次执行：再次尝试获取锁，但因为前序节点是Signal状态了,所以执行parkAndCheckInterrupt把自己休眠起来进行自旋

![image-20221126201553008](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201553008.png)

**3、C尝试获取锁：**

C获取锁和B完全一样，不同的是它的前序节点是B，所以它并不会一直尝试获取锁，只会呆在B后面park住

![image-20221126201605630](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201605630.png)

AQS通过最简单的CAS和LockSupport的park，设计出了高效的队列模型和机制：

1、AQS结构其实是在第二个线程获取锁的时候再初始化的，就是lazy-Init的思想，最大程度减少不必要的代码执行的开销

2、为了最大程度上提升效率，尽量避免线程间的通讯，采用了双向链表的Node结构去存储线程

3、为了最大程度上避免CPU上下文切换执行的消耗，在设计排队线程时，只有头结点的下一个的线程在一直重复执行获取锁，队列后面的线程会通过LockSupport进行休眠。

### 非公平锁的释放锁

上代码：







```java
//ReentrantLock类的unlock
public void unlock() {
    sync.release(1);
}

//AQS类的release()方法
public final boolean release(int arg) {
    //尝试释放锁
    if (tryRelease(arg)) {
        Node h = head;
        if (h != null && h.waitStatus != 0)
            //唤醒后继结点的线程
            unparkSuccessor(h);
        return true;
    }
    return false;
}

//ReentrantLock类中的内部类Sync实现的tryRelease(int releases) 
protected final boolean tryRelease(int releases) {

      int c = getState() - releases;
      if (Thread.currentThread() != getExclusiveOwnerThread())
          throw new IllegalMonitorStateException();
      boolean free = false;
      //判断状态是否为0，如果是则说明已释放同步状态
      if (c == 0) {
          free = true;
          //设置Owner为null
          setExclusiveOwnerThread(null);
      }
      //设置更新同步状态
      setState(c);
      return free;
  }

复制代码
```



一句话总结：释放锁首先就是把volatile类型的变量state减1。state从1变成0.

unparkSuccessor(h)的作用的唤醒后续的节点：







```ini
private void unparkSuccessor(Node node) {
    //这里，node是head节点。
    int ws = node.waitStatus;
    if (ws < 0)//置零当前线程所在的结点状态，允许失败。
        compareAndSetWaitStatus(node, ws, 0);

    Node s = node.next;//找到下一个需要唤醒的结点s
    if (s == null || s.waitStatus > 0) {//如果为空或已取消
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)//从这里可以看出，<=0的结点，都是还有效的结点。
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);//唤醒
}

复制代码
```



从代码执行操作来看，这里主要作用是用unpark()唤醒同步队列中最前边未放弃线程(也就是状态为CANCELLED的线程结点s)。此时，回忆前面分析进入自旋的函数acquireQueued()，s结点的线程被唤醒后，会进入acquireQueued()函数的if (p == head && tryAcquire(arg))的判断，然后s把自己设置成head结点，表示自己已经获取到资源了，最终acquire()也返回了，这就是独占锁释放的过程。 

### 总结

回到之前的图：A B C三个线程获取锁，A已经获取到了锁，BC在队列里面，此时A释放锁

![image-20221126201624823](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201624823.png)

执行b.unpark，B被唤醒后继续执行



```ini
if (p == head && tryAcquire(arg))
复制代码
```



因为B的前序节点是head，所以会执行tryAcquire方法尝试获取锁，获取到锁之后执行setHead方法把自己设置为头节点，并且把之前的头结点也就是上图中的new Node()设置为null以便于GC：







```ini
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                //获取到锁之后将当前node设置为头结点 head指向当前节点node
                setHead(node);
                //p.next就是之前的头结点，它没有用了，所以把它gc掉
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}复制代码
```



![image-20221126201642293](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201642293.png)

总之，AQS内部有一个同步队列，线程获取同步状态失败之后会被封装成node通过park进行自旋，而在释放同步状态时，通过unpark进行唤醒后面一个线程，让后面线程得以继续获取锁。

### ReetrantLock中的公平锁

了解完ReetrantLock中非公平锁的实现后，我们再来看看公平锁。与非公平锁不同的是，**在获取锁的时，公平锁的获取顺序是完全遵循时间上的FIFO规则**，也就是说先请求的线程一定会先获取锁，后来的线程肯定需要排队。

下面比较一下公平锁和非公平锁lock方法： 

![image-20221126201655466](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201655466.png)

再比较一下公平锁和非公平锁lock方法：tryAcquire方法：

![image-20221126201712692](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201712692.png)

![image-20221126201727325](D:\Tyora\AssociatedPicturesInTheArticles\补8_死磕java concurrent包系列（二）基于ReentrantLock理解AQS同步队列的细节和设计模式\image-20221126201727325.png)

唯一的差别就是`hasQueuedPredecessors()`判断同步队列是否存在结点，这就是非公平锁与公平锁最大的区别，**即公平锁在线程请求到来时先会判断同步队列是否存在结点**，如果存在先执行同步队列中的结点线程，当前线程将封装成node加入同步队列等待。而非公平锁呢，当线程请求到来时，不管同步队列是否存在线程结点，直接上去尝试获取同步状态，获取成功直接访问共享资源，但请注意在绝大多数情况下，非公平锁才是我们理想的选择，毕竟从效率上来说非公平锁总是胜于公平锁。

## 总结

 以上便是ReentrantLock的内部实现原理，这里我们简单进行小结，重入锁ReentrantLock，是一个基于AQS并发框架的并发控制类，其内部实现了3个类，分别是Sync、NoFairSync以及FairSync类，其中Sync继承自AQS，实现了释放锁的模板方法tryRelease(int)，而NoFairSync和FairSync都继承自Sync，实现各种获取锁的方法tryAcquire(int)。

ReentrantLock的所有方法实现几乎都间接调用了这3个类，因此当我们在使用ReentrantLock时，大部分使用都是在间接调用AQS同步器中的方法。

AQS在设计时将性能优化到了极致，具体体现在同步队列的park和unpark，初始化AQS时的懒加载，以及线程之间通过Node这样的数据结构从而避免线程间通讯造成的额外开销，这种由释放锁的线程主动唤醒后续线程的方式也是我们再实际过程中可以借鉴的。

AQS还不止于同步队列，接下来我们会继续探讨**AQS的条件队列**



作者：lyowish
链接：https://juejin.cn/post/6844903730156929032
来源：稀土掘金
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
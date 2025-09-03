## 1. 锁的种类

锁的种类挺多，包括：自旋锁、自旋锁的其他种类、阻塞锁、可重入锁、读写锁、互斥锁、悲观锁、乐观锁、公平锁、可重入锁等等，其余就不列出了。我们这边重点看如下几种：可重入锁、读写锁、可中断锁、公平锁。

### 1.1 可重入锁

如果锁具备可重入性，则称作为可重入锁。synchronized和ReentrantLock都是可重入锁，可重入性在我看来实际上表明了锁的分配机制：基于线程的分配，而不是基于方法调用的分配。举比如说，当一个线程执行到method1 的synchronized方法时，而在method1中会调用另外一个synchronized方法method2，此时该线程不必重新去申请锁，而是可以直接执行方法method2。

### 1.2 读写锁

读写锁将对一个资源的访问分成了2个锁，如文件，一个读锁和一个写锁。正因为有了读写锁，才使得多个线程之间的读操作不会发生冲突。`ReadWriteLock`就是读写锁，它是一个接口，ReentrantReadWriteLock实现了这个接口。可以通过readLock()获取读锁，通过writeLock()获取写锁。

### 1.3 可中断锁

可中断锁，即可以中断的锁。在Java中，synchronized就不是可中断锁，而Lock是可中断锁。 如果某一线程A正在执行锁中的代码，另一线程B正在等待获取该锁，可能由于等待时间过长，线程B不想等待了，想先处理其他事情，我们可以让它中断自己或者在别的线程中中断它，这种就是可中断锁。

Lock接口中的lockInterruptibly()方法就体现了Lock的可中断性。

实例代码：

```java
/**
在代码中一共使用了四个线程，主线程控制t1、t2、t3线程执行的流程，这三个线程t1、t2、t3间隔1秒开始执行确保线程之间是顺序执行的；在主线程中创建了三个锁，这三个锁在三个线程中保证每个线程执行的时候不会被别的线程影响，这个示例代码的目的是为了测试线程2调用了rl1.lockInterruptibly方法后，在等待获取锁rl1的过程中被线程3打断的实际情况。

在线程1种占有锁rl1 6S，之后在线程2中调用lockInterruptibly方法尝试获取线程1占有的rl1锁，方法介绍如下：
	1.首先线程2未被中断，才可以去获取rl1锁；
	2.如果rl1锁被线程1释放了，并且rl1锁并没有被别的线程占有，那么这里就可以成功获取锁，并且返回；
	3.如果rl1锁没有被线程1释放，那么就会禁用当前线程（也就是线程2被中断，中断状态为true），并且在发生以下两种情况之一之前，该线程（线程2）一直处于休眠状态；
		1.锁由当前线程获得——>那么当前线程（线程2）就会继续执行，并且清除中断状态。
		2.其他某个线程（也就是线程3）中断当前线程（也就是线程2），并且支持对锁获取的中断——>
			1.如果当前线程（线程2）被别的线程（线程3）或者自己（在执行这个方法之前自己设置自己的中断状态）设置了中断状态，那么这个方法就会抛出InterruptedException，并且清除当前线程的已中断状态。
			
对方法lockInterruptibly的总结：在线程2种调用rl1对象种的lockInterruptibly方法后，线程2的中断状态一定是false，即未中断的，这个方法正常执行完毕后，线程2可以获得rl1锁，如果这个方法是抛出异常而执行完毕后，线程2不能获得rl1锁。
*/

public static void main(String[] args) throws InterruptedException
{
    ReentrantLock rl1 = new ReentrantLock();
    ReentrantLock rl2 = new ReentrantLock();
    ReentrantLock rl3 = new ReentrantLock();
    Thread t1 = new Thread(new Runnable(){
        @Override
        public void run()
        {
            rl1.lock();
            
            System.out.println("线程一——同步代码块即将开始执行");
            try
            {
                Thread.sleep(6000);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            System.out.println("线程一——同步代码块即将执行结束");
            
            rl1.unlock();
        }
    },"t1");
    t1.start();
    Thread.sleep(1000);
    
    Thread t2 = new Thread(new Runnable(){
        @Override
        public void run()
        {
            rl2.lock();
            long start = System.currentTimeMillis();
            System.out.println("线程二——执行可中断的lockInterruptibly，尝试去获取锁");
            try{
                rl1.lockInterruptibly();
            } catch (InterruptedException e) {
                System.out.println("线程二——检测到线程二设置了中断状态");
            }
            //在线程3中设置了线程2的中断状态，所以在try块中的lockInterruptibly方法会抛出异常
            //停止
            long end = System.currentTimeMillis();
            System.out.println("线程二——本应该等待：5 S 才能获取到锁,但是实际使用时间："+ ((end - start)/1000));
            
            rl2.unlock();
        }
    },"t2");
    t2.start();
    
    Thread.sleep(1000);
    Thread t3 = new Thread(new Runnable(){
        @Override
        public void run()
        {
            rl3.lock();
            System.out.println("线程三开始，给线程二设置中断标志");
            t2.interrupt();
            System.out.println("线程三结束");
            rl3.unlock();
        }
    },"t3");
    t3.start();
}
```



### 1.4 公平锁

公平锁即尽量以请求锁的顺序来获取锁。同时有多个线程在等待一个锁，当这个锁被释放时，等待时间最久的线程（最先请求的线程）会获得该锁，这种就是公平锁。

非公平锁即无法保证锁的获取是按照请求锁的顺序进行的，这样就可能导致某个或者一些线程永远获取不到锁。

`synchronized`是非公平锁，它无法保证等待的线程获取锁的顺序。对于`ReentrantLock`和`ReentrantReadWriteLock`，默认情况下是非公平锁，但是可以设置为公平锁。

## 2. synchronized和lock的用法

### 2.1 synchronized

synchronized是Java的关键字，当它用来修饰一个方法或者一个代码块的时候，能够保证在同一时刻最多只有一个线程执行该段代码。简单总结如下四种用法。

#### 2.1.1 代码块

对某一代码块使用，synchronized后跟括号，括号里是变量，一次只有一个线程进入该代码块。

```java
public int synMethod(int m){
    synchronized(m) {
     //...
    }
 }
复制代码
```

#### 2.1.2 方法声明时

方法声明时使用，放在范围操作符之后,返回类型声明之前。即一次只能有一个线程进入该方法，其他线程要想在此时调用该方法，只能排队等候。

```java
public synchronized void synMethod() {
   //...
}
复制代码
```

#### 2.1.3 synchronized后面括号里是对象

synchronized后面括号里是一对象，此时线程获得的是对象锁。

```csharp
public void test() {
  synchronized (this) {
      //...
  }
}
复制代码
```

#### 2.1.4 synchronized后面括号里是类名.class

synchronized后面括号里是类，如果线程进入，则线程在该类中所有操作不能进行，包括静态变量和静态方法，对于含有静态方法和静态变量的代码块的同步，通常使用这种方式。【Java中一个类只有一个Class类的实例对象】

### 2.2 Lock

Lock接口主要相关的类和接口如下。

![Lock](https://p1-jj.byteimg.com/tos-cn-i-t2oaga2asx/gold-user-assets/2017/12/27/160985d95e5fa4ab~tplv-t2oaga2asx-zoom-in-crop-mark:4536:0:0:0.awebp)Lock



ReadWriteLock是读写锁接口，其实现类为ReetrantReadWriteLock。ReetrantLock实现了Lock接口。

#### 2.2.1 Lock

Lock中有如下方法：

```java
public interface Lock {
	void lockInterruptibly() throws InterruptedException;  
	boolean tryLock();  
	boolean tryLock(long time, TimeUnit unit) throws InterruptedException;  
	void unlock();  
	Condition newCondition();
}
复制代码
```

- lock：用来获取锁，如果锁被其他线程获取，处于等待状态。如果采用Lock，必须主动去释放锁，并且在发生异常时，不会自动释放锁。因此一般来说，使用Lock必须在try{}catch{}块中进行，并且将释放锁的操作放在finally块中进行，以保证锁一定被被释放，防止死锁的发生。
- lockInterruptibly：通过这个方法去获取锁时，如果线程正在等待获取锁，则这个线程能够响应中断，即中断线程的等待状态。
- tryLock：tryLock方法是有返回值的，它表示用来尝试获取锁，如果获取成功，则返回true，如果获取失败（即锁已被其他线程获取），则返回false，也就说这个方法无论如何都会立即返回。在拿不到锁时不会一直在那等待。
- tryLock（long，TimeUnit）：与tryLock类似，只不过是有等待时间，在等待时间内获取到锁返回true，超时返回false。
- unlock：释放锁，一定要在finally块中释放

#### 2.2.2 ReetrantLock

实现了Lock接口，可重入锁，内部定义了公平锁与非公平锁。默认为非公平锁：

```java
public ReentrantLock() {  
  sync = new NonfairSync();  
} 
复制代码
```

可以手动设置为公平锁：

```java
public ReentrantLock(boolean fair) {  
  sync = fair ? new FairSync() : new NonfairSync();  
}  
复制代码
```

### 2.2.3 ReadWriteLock

```java
public interface ReadWriteLock {  
    Lock readLock();       //获取读锁  
    Lock writeLock();      //获取写锁  
}  
复制代码
```

一个用来获取读锁，一个用来获取写锁。也就是说将文件的读写操作分开，分成2个锁来分配给线程，从而使得多个线程可以同时进行读操作。ReentrantReadWirteLock实现了ReadWirteLock接口，并未实现Lock接口。 不过要注意的是：

如果有一个线程已经占用了读锁，则此时其他线程如果要申请写锁，则申请写锁的线程会一直等待释放读锁。

如果有一个线程已经占用了写锁，则此时其他线程如果申请写锁或者读锁，则申请的线程会一直等待释放写锁。

#### 2.2.4 ReentrantReadWriteLock

ReentrantReadWriteLock同样支持公平性选择，支持重进入，锁降级。

```java
public class RWLock {
    static Map<String, Object> map = new HashMap<String, Object>();
    static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    static Lock r = rwLock.readLock();
    static Lock w = rwLock.writeLock();
    //读
    public static final Object get(String key){
        r.lock();
        try {
            return map.get(key);
        } finally {
            r.unlock();
        }
    }
    //写
    public static final Object put(String key, Object value){
        w.lock();
        try {
            return map.put(key, value);
        } finally {
            w.unlock();
        }
    }
}
复制代码
```

只需在读操作时获取读锁，写操作时获取写锁。当写锁被获取时，后续的读写操作都会被阻塞，写锁释放后，所有操作继续执行。

## 3. 两种锁的比较

### 3.1 synchronized和lock的区别

- Lock是一个接口，而synchronized是Java中的关键字，synchronized是内置的语言实现；
- synchronized在发生异常时，会自动释放线程占有的锁，因此不会导致死锁现象发生；而Lock在发生异常时，如果没有主动通过unLock()去释放锁，则很可能造成死锁现象，因此使用Lock时需要在finally块中释放锁；
- 性能上来说，在资源竞争不激烈的情形下，Lock性能稍微比synchronized差点（编译程序通常会尽可能的进行优化synchronized）。但是当同步非常激烈的时候，synchronized的性能一下子能下降好几十倍。而ReentrantLock确还能维持常态；
  <font color = "blue">**资源竞争激烈的解释**</font>：在synchronized使用的时候，假设有两个线程竞争资源，首先A线程获取锁之后，锁是偏向锁，当线程B开始获取锁的时候，如果没有成功获取锁，那么锁就会升级为轻量级锁（假设：CAS需要循环10次锁才会升级为重量级锁），那么当B线程竞争资源的时候某个线程在CAS上循环的次数达超过10次那么锁才会升级为重量级锁<font color = "red">**（可以称之为资源竞争激烈）**</font>，如果线程B和A竞争资源的时候在CAS上循环的次数没有超过10次，那么轻量级锁不会升级为重量级锁<font color = "red">**（可以称之为资源竞争不激烈）**</font>。
- lock接口下的锁中定义了一些synchronized关键字没有的方法，如下：
  - synchronized是非公平锁，在lock接口下即有公平锁，也有非公平锁
  - Lock可以让等待锁的线程响应中断（这个说的也就是Lock类种的lockInterruptibly方法），而synchronized却不行，使用synchronized时，等待的线程会一直等待下去，不能够响应中断；
  - 通过Lock可以知道有没有成功获取锁（这个说的是tryLock()方法），而synchronized却无法办到。
  - 有等待时间的获取锁（我加的）
  - Lock可以提高多个线程进行读操作的效率。（可以通过readwritelock实现读写分离）

### 3.2 性能比较

下面对synchronized与Lock进行性能测试，分别开启10个线程，每个线程计数到1000000，统计两种锁同步所花费的时间。网上也能找到这样的例子。

```java
public class TestAtomicIntegerLock {

    private static int synValue;

    public static void main(String[] args) {
        int threadNum = 10;
        int maxValue = 1000000;
        testSync(threadNum, maxValue);
        testLocck(threadNum, maxValue);
    }
	//test synchronized
    public static void testSync(int threadNum, int maxValue) {
        Thread[] t = new Thread[threadNum];
        Long begin = System.nanoTime();
        for (int i = 0; i < threadNum; i++) {
            Lock locks = new ReentrantLock();
            synValue = 0;
            t[i] = new Thread(() -> {

                for (int j = 0; j < maxValue; j++) {
                    locks.lock();
                    try {
                        synValue++;
                    } finally {
                        locks.unlock();
                    }
                }

            });
        }
        for (int i = 0; i < threadNum; i++) {
            t[i].start();
        }
        //main线程等待前面开启的所有线程结束
        for (int i = 0; i < threadNum; i++) {
            try {
                t[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("使用lock所花费的时间为：" + (System.nanoTime() - begin));
    }
	// test Lock
    public static void testLocck(int threadNum, int maxValue) {
        int[] lock = new int[0];
        Long begin = System.nanoTime();
        Thread[] t = new Thread[threadNum];
        for (int i = 0; i < threadNum; i++) {
            synValue = 0;
            t[i] = new Thread(() -> {
                for (int j = 0; j < maxValue; j++) {
                    synchronized(lock) {
                        ++synValue;
                    }
                }
            });
        }
        for (int i = 0; i < threadNum; i++) {
            t[i].start();
        }
        //main线程等待前面开启的所有线程结束
        for (int i = 0; i < threadNum; i++) {
            try {
                t[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("使用synchronized所花费的时间为：" + (System.nanoTime() - begin));
    }

}
复制代码
```

测试结果的差异还是比较明显的，Lock的性能明显高于synchronized。本次测试基于JDK1.8。

```java
使用lock所花费的时间为：436667997
使用synchronized所花费的时间为：616882878
复制代码
```

JDK1.5中，synchronized是性能低效的。因为这是一个重量级操作，它对性能最大的影响是阻塞的是实现，挂起线程和恢复线程的操作都需要转入内核态中完成，这些操作给系统的并发性带来了很大的压力。相比之下使用Java提供的Lock对象，性能更高一些。多线程环境下，synchronized的吞吐量下降的非常严重，而ReentrankLock则能基本保持在同一个比较稳定的水平上。

到了JDK1.6，发生了变化，对synchronize加入了很多优化措施，有自适应自旋，锁消除，锁粗化，轻量级锁，偏向锁等等。导致在JDK1.6上synchronize的性能并不比Lock差。官方也表示，他们也更支持synchronize，在未来的版本中还有优化余地，所以还是提倡在synchronized能实现需求的情况下，优先考虑使用synchronized来进行同步。

## 4. 总结

本文主要对并发编程中的锁机制synchronized和lock，进行详解。synchronized是基于JVM实现的，内置锁，Java中的每一个对象都可以作为锁。对于同步方法，锁是当前实例对象。对于静态同步方法，锁是当前对象的Class对象。对于同步方法块，锁是Synchonized括号里配置的对象。Lock是基于在语言层面实现的锁，Lock锁可以被中断，支持定时锁。Lock可以提高多个线程进行读操作的效率。通过对比得知，Lock的效率是明显高于synchronized关键字的，一般对于数据结构设计或者框架的设计都倾向于使用Lock而非Synchronized。

#### 



作者：aoho
链接：https://juejin.cn/post/6844903542440869896
来源：稀土掘金
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
#线程池 

掘金：[深入理解 Java 线程池：ThreadPoolExecutor](https://juejin.cn/post/6844903475197788168)

# 0、什么是线程池

线程池是池化技术的一种典型实现，所谓池化技术就是提前保存大量的资源，以备不时之需。在机器资源有限的情况下，使用池化技术可以大大的提高资源的利用率，提升性能等。

线程池，说的就是提前创建好一批线程，然后保存在线程池中，当有任务需要执行的时候，从线程池中选一个线程来执行任务。

线程池可以有效的管理线程

1. 管理线程数量，避免无限制的创建线程导致系统崩溃
2. 复用线程，减少创建和销毁线程带来的开销

# 1. 四种线程池

## 1.1 Executors四种线程和ThreadPoolExecutor参数

使用Executors类中的这四个方法可以创建四个线程池，这四个线程池都是ExecutorService类型的，关闭这四种线程池都是调用ExecutorService类中的shoutdown方法

- newCachedThreadPool 具有缓存性质的线程池,该线程池中的线程最大空闲时间60s，在某个线程空闲的60s内，该线程可重复利用(缓存特性)，没有最大线程数限制。一个没有容量的等待队列<font color = "red">**【SynchronousQueue】**</font><font color = "gree">（因为不需要，根据创建ThreadPoolExecutor时指定的参数可以知道这个线程池没有核心线程，应急线程可以有Integer.MAX个，如果一个任务P被提交到这个线程池中，但是这个线程池中没有空闲的线程，那么会直接创建一个新的线程存放在线程池中，用这个新创建的线程运行任务P）</font>。

  

  该线程池适用场景：高并发情况下  &&  任务执行时间短的场景
  
- newFixedThreadPool 具有固定数量的线程池，核心线程数等于最大线程数，线程池中空闲线程的最大空闲时间为0，超出最大线程数的任务进入等待队列<font color = "red">**【LinkedBlockingQueue】**</font>（优先使用这个线程池）

  

  该线程池适用场景：高并发下控制性能
  
- newScheduledThreadPool 具有时间调度特性的线程池，必须初始化核心线程数，最大线程数为Integer.MAX，线程池中线程的最大空闲时间为0，底层使用<font color = "red">**【DelayedWorkQueue】**</font>实现延迟特性。

  

  该线程池适用场景：延迟双删用到的线程池
  
- newSingleThreadExecutor 核心线程数与最大线程数均为1；这个方法在创建类的时候和使用newFixedThreadPool方法创建的类的时候底层都是使用ThreadPoolExecutor类，并且参数除了core和max之外全都相同，区别只是core和max都是1；同样使用的队列也是<font color = "red">**【LinkedBlockingQueue】**</font>

  该线程池适用场景：不需要并发，但需要一定执行顺序的场景


- 这四个线程池的底层参数都有四个，只是这四个参数的值不同：
  这四个线程底层都是创建ThreadPoolExecutor类的实例对象（区别只是这些参数不同），该类的构造器中有六个参数，下面列举出的四个是这四个线程池用到的，还有参数：timeout的单位、拒绝策略。

  -   core 核心线程数，线程池初始化的时候指定的线程数量，如果核心线程数量为0，那么每一个任务都会创建一个新的线程；核心线程不会自动结束。
  -   max  最大线程数，线程池的等待队列加满之后可以最多可以运行的线程数量是：应急线程数量+核心线程数【max-核心线程数 = 应急线程数】，最大线程数不能小于核心线程数，最少应该是相等的
  -   timeout 超时时间，线程池中应急线程的的最大空闲时间<u>（如果在创建了线程池之后调用allowsCoreThreadTimeOut方法并且传递参数为true，那么这个时间也是核心线程的超时时间）</u>；线程池中的线程（应急+核心线程）如果空闲时间超过 keepAliveTime，将被终止。这提供了一种在池没有被积极使用时减少资源消耗的方法。如果池稍后变得更加活跃（有新的任务提交），则将构造新线程。
  - queue    等待队列（存放任务），当正在运行的线程数等于核心线程数之后，再次向线程池提交任务，这个时候这个任务就会进入等待队列，当线程池中有线程空闲的时候，等待队列中的任务才会被执行；如果线程池中的核心线程一直没有空闲的，就会直接在线程池中创建应急线程（有数量限制）；
  
  
  
    关于阻塞队列、核心线程数、最大线程数、拒绝策略之间的关系（我的理解）：
    <font color ="red">***如果核心线程数已经全部启动，并且没处理完执行的任务，同时阻塞队列已经放满，并且有限的应急线程全部创建并还未处理完任务，核心线程也没有处理完任务，但是还是有任务被提交到线程池中，那么就会触发拒绝策略。***</font>

关于ThreadPoolExecutor类构造器的七个参数在   向线程池中任务的流程图中   的作用【七个参数分别为：核心线程数、最大线程数、线程最大空闲时间、时间单位、等待队列、ThreadFactory接口实例对象、拒绝策略】

![image-20230525164655491](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20230525164655491.png)

## 1.2 线程数设定成多少

一般情况下，需要根据你的任务情况来设置线程数，任务可能是两种类型，分别是CPU密集型和IO密集型。

1. 如果是CPU密集型应用，则线程池大小设置为N+1
2. 如果是IO密集型应用，则线程池大小设置为2N+1

上面的N为CPU总核数，但是实际应用起来，也不要死守着公式不放，公式只是可以当作参考。

而且，上面的公式中，前提要求是知道你的应用是IO密集型还是CPU密集型，那么，到底怎么样算IO密集，怎么样又算CPU密集呢？一个应用就真的能明确的定位出来是CPU密集还是IO密集吗？、

还有，现在很多CPU都采用了超线程技术，也就是利用特殊的硬件指令，把两个逻辑内核模拟成两个物理芯片，让单个处理器都能使用线程级并行计算。所以我们经常可以看到"4核8线程的CPU"，也就是物理内核有4个，逻辑内核有8个，但是按照4和8配置都不合理，因为超线程技术整体性能提升也并不是100%的。

所以，我们可以在刚上线的时候，先根据公式大致的设置一个数值，然后再根据你自己的实际业务情况，以及不断的压测结果，再不断调整，最终达到一个相对合理的值。

## 1.3 四种线程池使用示例


- 示例： 
  CachedThreadPoolTest（在代码中两处sleep的地方用于对比，当注释掉外部的sleep，打开run方法中的sleep方法之后由于每个线程运行的时间不超过1S，导致在1S后下一个线程开始的时候会使用上线程池中空闲的线程而不是新创建一个线程）
  FixedThreadPoolTest

  ```java
  //得到运行时环境下可用的线程数量，这个数量可以作为使用newFixedThreadPool创建线程时的核心线程数量
  System.out.println(Runtime.getRuntime().availableProcessors());
  ExecutorService fixedThreadPool = Executors.newFixedThreadPool(4*20);
  for (int i = 0; i < 10; i++) {
      final int index = i;
      fixedThreadPool.execute(new Runnable() {
          public void run() {
              try {
                  System.out.println(Thread.currentThread().getName()+">>"+index);
                  /*
                  * run方法中的sleep的作用是为了模拟线程运行所需要的时间，当当前正在运行
                  * 的线程的数量超过核心线程数之后就会等待，直到正在运行的核心线程运行完毕才
                  * 能有新的线程创建并运行
                  **/
                  Thread.sleep(2000);
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }
          }
      });
  }
  ```

  、
  ScheduledThreadPoolTest（有三种方式）

  ```java
  public static void test1() {
      ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
  
      for (int i = 0; i < 10; i++) {
          final int index = i;
          Runnable task = new Runnable() {
              public void run() {
                  System.out.println(Thread.currentThread().getName() + ">> delay " + index + " seconds run....");
              }
          };
          //此处的意思是：task指定的任务要延迟i秒后执行
          ScheduledFuture<?> schedule = scheduledThreadPool.schedule(task, i, TimeUnit.SECONDS);
      }
      scheduledThreadPool.shutdown();
  }
  
  public static void test2() {
      ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(12);
      Runnable task = new Runnable() {
          public void run() {
              try {
                  System.out.println(Thread.currentThread().getName() + ">> sleep..." + System.currentTimeMillis());
                  //将这里的参数由3秒换成1秒，可以看出来当任务的运行时间大于执行		 						//scheduleAtFixedRate方法时指定的周期
                  //时，一个任务两次运行的状况
                  Thread.sleep(3000);
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }
              System.out.println(Thread.currentThread().getName() + ">> run....." + System.currentTimeMillis());
          }
      };
  
      //第三个参数是第二个参数的单位，第二个参数是一个任务周期性运行的固定频率
      // 此处的意思是：一个任务在0毫秒之后执行（立即执行），之后的该任务每隔2秒执行1次（如果任务执行的时间小于2秒），如果任务本身运行的时间比2秒长，那么这个任
      //务就是在第一次运行完之才会再次运行，而不是在【从第一个任务开始运行2秒后，第一个任务运行中】的情况下执行第二个任务。
      ScheduledFuture<?> scheduleAtFixedRate = scheduledThreadPool.scheduleAtFixedRate(task, 0, 2, TimeUnit.SECONDS);
  
      try {
          Thread.sleep(10000);
      } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
      }
      //对定时任务进行停止操作，参数为true意味着当第一次任务开始执行10秒之后，无论该任务是否还在运行都会停止
      //传递参数为false的时候，意味着10秒之后，如果还有线程在运行，那么会等待该线程运行完毕
      scheduleAtFixedRate.cancel(true);
      //关闭线程池
      scheduledThreadPool.shutdown();
  }
  
  public static void test3() {
      ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(10);
      Runnable task = new Runnable() {
          public void run() {
              try {
                  System.out.println(Thread.currentThread().getName() + ">> sleep..." + System.currentTimeMillis());
                  Thread.sleep(3000);
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }
              System.out.println(Thread.currentThread().getName() + ">> run....." + System.currentTimeMillis());
          }
      };
      //第三个参数是第二个参数的单位，第二个参数是一个任务周期性运行的固定延迟时间
      //此处的意思是：一个任务在0毫秒之后执行（立即执行）,之后该任务执行完毕之后等待2秒再次执行
      ScheduledFuture<?> scheduleWithFixedDelay = scheduledThreadPool.scheduleWithFixedDelay(task, 0, 2, TimeUnit.SECONDS);
  
      try {
          Thread.sleep(10000);
      } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
      }
      //对定时任务进行停止操作，参数为true意味着当第一次任务开始执行10秒之后，无论该任务是否还在运行都会停止
      //传递参数为false的时候，意味着10秒之后，如果还有现成在运行，那么会等待该线程运行完毕
      scheduleWithFixedDelay.cancel(true);
      //关闭线程池
      scheduledThreadPool.shutdown();
  }
  
  public static void main(String[] args) {
      //test1();
      test2();
      //test3();
  }
  ```

  、 
  SingleThreadExecutorTest

## 1.4 向线程池中提交任务的方式

- 向ThreadPoolExecutor类构造的线程池中提交任务的两种方式：execute()和submit()

  ![聊聊线程池的提交方式_线程池](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\dc1127800d4996b4f592e26fabbed5ab.png)

  ![聊聊线程池的提交方式_Java_02](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\38364dcb0b0dae7b09f96f203e5f72c5.png)

  两种方式提交的区别：

  - execute只能提交Runnable类型的任务，无返回值。
  - submit既可以提交Runnable类型的任务，也可以提交Callable类型的任务，会有一个类型为Future的返回值，但当任务类型为Runnable时，返回值为null。

  

  - execute在执行任务时，如果遇到异常会直接抛出
  - submit不会直接抛出，只有在使用Future的get方法获取返回值时，才会抛出异常。

- 向ScheduledThreadPoolExecutor类（ThreadPoolExecutor类的子类）构造的线程池中提交任务的三种方式：（有关这三种方式可以去看上面的示例中的注释，示例中的三个方法分别使用了下表的方法）

  - ![image-20220826120313210](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220826120313210.png)
    对scheduleAtFixedRate方法的补充：
    ![image-20220826120647571](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220826120647571.png)

    ![image-20220826121609791](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220826121609791.png)

  - 关于这四个方法的返回值的类型：ScheduledFuture<V>

    - ```java
      public interface ScheduledFuture<V>
      extends Delayed, Future<V>
      ```

      一个延迟的、结果可接受的操作，可以将其取消。通常已安排的future是用ScheduledExecutorService安排任务的结果（官网描述，但我没看懂）。
      
    - cancel(boolean mayInterruptIfRunning)：继承自Future<V>；试图取消对此任务的执行。如果任务已完成、或已取消，或者由于某些其他原因而无法取消，则此尝试将失败。当调用 cancel 时，如果调用成功，而此任务尚未启动，则此任务将永不运行。如果任务已经启动，则 mayInterruptIfRunning 参数确定是否应该以试图停止任务的方式来中断执行此任务的线程。 
    
  - ![image-20250706225947519](D:\Obsidian\知识库—Java相关\2、相关技术\2、JUC\3-JAVA并发编程核心技术-精通篇锁线程池并发模式aqsjuc等等\KEVIN授权学员专属资料-并发编程精通篇-01\Java并发编程精通篇-2.assets\image-20250706225947519.png)



# 2. ThreadPoolExecutor

- 四种线程池都是通过Executors类（工厂类）创建的，底层创建的都是ThreadPoolExecutor类，可以构建自己需要的线程类。

  

- 类图：

  创建第一二种线程池的时候使用的ThreadPoolExecutor类的类图
![创建第一二中线程池的时候使用的ThreadPoolExecutor类的类图](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220823114720133.png)

  创建第三种线程池的时候使用的ScheduledThreadPoolExecutor类的类图
  ![创建第三种线程池的时候使用的ScheduledThreadPoolExecutor类的类图](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220823114920789.png)

  创建第四种线程池使用的类图

  ![创建第四种线程池的时候使用的类图](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220823115031635.png)



# 3. 线程池使用拒绝策略


- 如果线程池中所有核心线程以及全部启动(正在执行任务)，并且阻塞队列已经装满，并且应急线程已经全部启动(应急线程数量+核心线程数量已经达到最大值)，这个时候就需要使用拒绝策略对这些超出 【最大线程数量 + 阻塞队列长度 + 等待队列长度】 个数的线程进行处理，JDK为我们提供了四种默认的拒绝策略：

  -   AbortPolicy       抛出异常,不影响其他线程运行<font color = "red">**【默认的】**</font>
  -   CallerRunsPolicy   使用调用线程调用当前任务（也就是执行线程池对象.execute()方法的线程去运行这个任务）
  -   DiscardOldestPolicy    丢弃最老的任务（将线程池中第一个线程开始运行的时候执行的任务丢弃，之后如果还需要执行拒绝策略，则按照线程池中线程开始运行的顺序逐个丢弃这些线程对应的任务），并执行当前任务
  -   DiscardPolicy     直接丢弃,什么也不做

  这些决绝策略都是实现了RejectedExecutionHandler接口的类，通过实现该接口中的rejectedExecution方法来实现拒绝策略，我们也可以定义自己的拒绝策略。

  

- 示例：ThreadPoolExecutorTest
  示例代码中构建自己需要的简单的线程类的方式（没有使用拒绝策略）：

  ```java
  class MyTask implements Runnable{
  
  	int index = 0;
  	
  	public MyTask(int index) {
  		this.index = index;
  	}
  	
  	@Override
  	public void run() {
  		try {
  			Thread.sleep(3000);
  		} catch (InterruptedException e) {
  			e.printStackTrace();
  		}
  		System.out.println(Thread.currentThread().getName()+">>run "+index);
  	}
  }
  public class test{
      	public static void main(String[] args) {
  		ThreadPoolExecutor executor = new ThreadPoolExecutor(7,7,60L,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>());
  		for(int i=0;i<8;i++){
  			MyTask task = new MyTask(i);
  			executor.submit(task);
  		}
  		executor.shutdown();
  	}
  }
  ```

  示例代码中使用拒绝策略部分的代码：

  ```java
  ThreadPoolExecutor executor = new ThreadPoolExecutor(3,3,0,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(1),new AbortPolicy());
  
  ThreadPoolExecutor executor = new ThreadPoolExecutor(3,3,0,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(1),new CallerRunsPolicy());
  
  ThreadPoolExecutor executor = new ThreadPoolExecutor(3,3,0,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(1),new DiscardOldestPolicy());
  
  ThreadPoolExecutor executor = new ThreadPoolExecutor(3,3,0,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(1),new DiscardPolicy());
  ```

  示例代码中自定义拒绝策略的方式以及使用的代码：

  ```java
  class MyPolicy implements RejectedExecutionHandler{
      //参数中的r指的是被拒绝策略处理的任务
  	@Override
  	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
  		System.out.println(r+"被拒绝执行"+System.currentTimeMillis());
  	}
  }
  public class test{
      //使用自定义的拒绝策略创建自定义的线程池
      ThreadPoolExecutor executor = new ThreadPoolExecutor(3,3,0,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(1),new MyPolicy());
      //使用这个线程池对任务（任何实现了Runnable接口、Callable接口的实现类的实例对象）进行处理
      
  }
  ```

  



# 4. ThreadFactory

改变线程池中，线程创建的行为

知识点

我们通常使用线程池的submit方法将任务提交到线程池内执行。

<u>*如果此时线程池内有空闲的线程，则会立即执行该任务，如果没有则需要根据线程池的类型选择等待，或者新建线程。*</u>

所以<font color="red">**线程池内的线程并不是线程池对象初始化（new）的时候就创建好的，而是当有任务被提交进来之后才创建的，而创建线程的过程在线程池创建的时候不指定ThreadFactory参数时是无法干预的**</font>。

如果我们想在每个线程创建时记录一些日志，或者推送一些消息那怎么做？使用ThreadFactory接口。

![image-20220823115534287](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220823115534287.png)

使用ThreadFactory接口的步骤：

1.  编写ThreadFactory接口的实现类
    1. 该节口中只有一个newThread方法，参数是任务（Runnable接口的实现类）的实例对象，该方法的作用是将传入进来的任务转成线程对象
2.  创建线程池时传入ThreadFactory对象

Demo：
com.mkevin.demo13.DemoThreadFactory

```java
class DemoThreadFactory implements ThreadFactory {
    // 控制线程创建时是否记录日志
    private boolean saveLog;
    // 工场名称
    private String factoryName;

    public DemoThreadFactory(String factoryName, boolean saveLog) {
        this.factoryName = factoryName;
        this.saveLog = saveLog;
    }

    public Thread newThread(Runnable r) {
        if (saveLog) {
            //动态输出日志
            P.l(System.currentTimeMillis() + this.factoryName + " create start");
        }
        //创建执行指定任务r的线程
        Thread thread = new Thread(r);
        //自定义线程名字
        thread.setName("Kevin-Thread-" + thread.getName() + ":" + thread.getId());
        try {
            //模拟线程初始化时间
            Thread.sleep(ThreadLocalRandom.current().nextInt(1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (saveLog) {
            P.l(System.currentTimeMillis() + this.factoryName + " " + thread.getName() + " create end");
        }
        return thread;
    }
}
```

com.mkevin.demo13.ThreadFactoryDemo0




# 5. 线程池内异常的优雅处理

优雅的处理线程池内未捕获异常

线程池状态

-   线程池内运行的线程如果发生异常，一定要捕获，要养成习惯。

-   可以采用更优雅的方式处理所有线程的异常，例如记录日志，发送预警消息等。

    -   结合ThreadFactory以及线程的setUncaughtExceptionHandler方法来处理最为优雅（在newThread方法中调用Thread类中的这个方法设置未捕获异常处理器）

    -   **这种方式对execute提交的任务有效，对submit提交的任务无效，巨坑！**

**Demo**：
com.mkevin.demo15.ThreadExceptionDemo1

# 6. 关闭线程池

shutdown和shutdownNow的作用和区别

shutdown 与 shutdownNow

知识点

- shutdown让线程池内的任务继续执行完毕，但是不允许新的任务提交

- shutdown方法不阻塞, 等所有线程执行完毕后，销毁线程

- shutdown之后提交的任务会抛出RejectedExecutionException异常，代表拒绝接收

  

- shutdownNow之后会引发sleep、join、wait方法的InterruptedException异常，并且该线程执行的任务不会执行完毕

- 如果正在运行的任务中没有捕获InterruptedException异常的条件，则任务会继续运行直到结束，此时shutdownNow不起任何作用；如果运行中的任务中有处理InterruptedException异常的条件，那么会直接销毁线程（不管线程是否执行完毕）

- shutdownNow之后提交的任务会抛出RejectedExecutionException异常，代表拒绝接收

- 我大概看了一下对应的源码，按照源码中的使用方式我感觉这个方法的作用是：给当前正在运行的线程设置中断标志为true（中断状态），如果正在执行的任务中有处理InterruptedException异常的操作，并且该操作会结束任务的执行，那么才会真正结束任务的执行；但是如果正在执行的任务中没有处理InterruptedException的条件（或者仅仅只是try了该异常catch语句中是空的）也不会中断该任务。

Demo：
com.mkevin.demo14.ThreadPoolDemo1

```java
public static void main(String[] args) throws InterruptedException {

    // 创建线程池，而不提交任何任务，则无任何线程被创建，程序直接运行结束
    /*ExecutorService executorService = Executors.newCachedThreadPool();
        P.l("main is over");*/


    // 创建线程池，并向线程池提交任务，则创建线程，任务执行完毕，而线程池中刚刚创建的线程不销
    // 毁，JVM继续运行等待60秒后，自动销毁空闲线程，JVM退出
    //Runner runner = new Runner();
    //ExecutorService executorService = Executors.newCachedThreadPool();
    //executorService.submit(runner);
    //P.l("main is over");


    // 创建线程池，并向线程池提交任务，则创建线程，任务执行完毕而线程不销毁，JVM继续运行
    // 始终保持有1个线程存活,因为这个线程是核心线程，核心线程不会随着超时时间的到达而关闭
    // ，所以JVM不会退出，应该使用shutdown方法结束这个线程
    /*Runner runner = new Runner();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(runner);
        P.l("main is over");*/

    //shutdown让线程池内的任务继续执行完毕，但是不允许新的任务提交，shutdow方法不阻塞
    //等所有线程执行完毕后，销毁线程，JVM退出
    /*Runner runner = new Runner();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(runner);
        executorService.shutdown();
        P.l("main is over");*/

    //shutdown之后提交的任务会抛出RejectedExecutionException异常，代表拒绝接收
    /*Runner runner = new Runner();
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.submit(runner);
        executorService.shutdown();
        P.l("main is over");
        executorService.submit(runner);*/

    //shutdownNow之后提交的任务会抛出RejectedExecutionException异常，代表拒绝接收
    //shutdownNow之后会引发sleep、join、wait方法的InterruptedException异常，并且
    //线程池中的线程执行的任务不会执行完毕
    /*ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.submit(new Runner());
        executorService.shutdownNow();
        P.l("main is over");
        executorService.submit(new Runner());*/

    //如果线程池中正在执行的任务中没有触发InterruptedException的条件，则任务会继续运行直
    //到结束，这种情况下shoutdownNow方法不会对已经提交并且正在运行的任务进行处理
    /*ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.submit(new Runner1());
        executorService.shutdownNow();
        P.l("main is over");*/

    //可以在任务中判断Thread.currentThread().isInterrupted()来规避shutdownNow的问题
    ExecutorService executorService = Executors.newFixedThreadPool(5);
    executorService.submit(new Runner2());
    executorService.shutdownNow();
    P.l("main is over");
}

static class Runner implements Runnable {
    public void run() {
        try {
            P.l(Thread.currentThread().getName()+" begin");
            Thread.sleep(2000);
            P.l(Thread.currentThread().getName()+" end");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

//不做特殊处理的任务，无法规避shutdownNow的问题
static class Runner1 implements Runnable {
    public void run() {
        while(true){
            P.l(Thread.currentThread().getName()+ " " + System.currentTimeMillis());
        }
    }
}

//判断线程是否Interrupted的程序，规避shutdownNow的问题
static class Runner2 implements Runnable {
    public void run() {
        while(true){
            P.l(Thread.currentThread().getName()+ " " + System.currentTimeMillis());
            if(Thread.currentThread().isInterrupted()){
                P.l(Thread.currentThread().getName()+ " interrupted");
                break;
            }
        }
    }
}
```



# 7. 线程池的结束状态

线程池内线程运行结束的标志

线程池状态

-   isShutdown：用来判断线程池是否已经关闭，只要执行了shutdown方法这个方法就会返回true，但是并不代表任务已经都执行完毕了

-   isTerminated：任务全部执行完毕，并且线程池已经关闭，才会返回true

-   awaitTermination 阻塞（阻塞参数指定的时间去看线程池内线程运行的状态），直到所有任务在关闭请求后完成执行，或发生超时，或当前线程中断（以先发生者为准）。

Demo:
com.mkevin.demo14.ThreadPoolDemo2



# 8、线程池的几种状态

![image-20230524184429328](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20230524184429328.png)

1. RUNNING：能接受新提交的任务，并且能处理阻塞队列中的任务

2. SHUTDOWN：关闭状态，不能再提交新的任务，但是可以继续处理阻塞队列中已经存在的任务（finalize方法在执行的过程中也会调用shutdown方法进入该状态）

3. STOP：不能接受新的任务，也不能处理队列中的任务，会中断正在执行任务的线程（给对应线程设置中断状态为已中断）

4. TIDYING：如果所有的任务都已经终止了，有效线程数（workerCount）为0，队列为空，线程池进入该状态后调用terminated()方法进入TERMINATED状态

5. TERMINATED：在terminated()方法执行完后进入该状态，默认terminated()方法中什么也么有做，这个状态标识线程池已死亡。

   进入TERMINATED的条件如下：

   1. 线程池不是RUNNING状态
   2. 线程池不是TINDING状态或TERMINATED状态
   3. 如果线程池状态是SHOUTDOWN并且workerQueue为空
   4. workerCounnt为0
   5. 设置TIDYING状态成功



# 9. 允许核心线程超时策略

核心线程也允许超时销毁，但是需要我们手动设置，默认是没有超时销毁的。

在调用ThreadPoolExecutor的构造方法创建线程池的时候，给这个类传递的参数中的core指的就是***<font color="red">核心线程数</font>***

**<font color = "red">核心线程 + 应急线程：</font>**<font color="blue">像核心线程这样的线程是常驻线程池内部的，是不会被销毁的，这样的线程处理完各自的任务之后会依然停留在线程池内部，如果没有新的任务提交进来，那么它们依旧保持空闲状态；如果创建好线程池之后（创建ThreadPoolExecutor实例对象时指定的参数是：3,4,120,queue），有一个任务被提交进来，那么线程池中的3个核心线程中的某个线程就会运行这个任务，如果又一次性提交了2个任务，导致线程池中的核心线程都在运行，这时一个新的任务（记作Q）被提交进来，就需要去看构造ThreadPoolExecutor的时候传递的第二个参数max（上面的4），如果  3 + N(Q)  的个数没有超过max（上面的4）【假设阻塞队列容量为0】 这个时候就会在线程池中再创建一个线程（称之为应急线程），而这个新创建的线程有空闲的指定时间：keepAliveTime（也就是上面的120），当这个新创建的应急线程执行完Q任务之后的keepAliveTime时间内没有被分配到别的任务，那么该线程就会被销毁。</font>

以上说的核心线程的属性是默认的，再这一节中我们可以让核心线程也想应急线程一样有超时策略



允许核心线程超时策略

-   核心线程也允许销毁， allowsCoreThreadTimeOut就用来做这个事

    -   设置控制核心线程是否可能超时的策略，如果在保持活动时间内没有任务到达，则该策略将在新任务到达时根据需要被替换。下面是参数值对应的功能：

        -   如果为false，则不会由于缺少传入任务而终止核心线程。

        -   如果为true，则应用于非核心线程的相同保持活动策略也适用于核心线程。

            -   *<u>为避免连续更换线程，设置为true时保持活动时间必须大于零。</u>*

-   **通常应该在池被激活（提交任务）之前调用此方法。**

Demo:
com.mkevin.demo16.allowCoreThreadTimeOutDemo



# 10. 核心线程预启动策略

核心线程预启动

核心线程预启动策略

-   **默认情况下，核心线程只有在任务提交的时候才会创建**；
    而预启动策略，可以让核心线程提前启动，从而增强最初提交的线程运行性能

-   prestartCoreThread：调用一次该方法启动1个核心线程，返回值为true和false，分别代表预启动成功是失败。覆盖仅在执行新任务时启动核心线程的默认策略。如果所有核心线程都已启动，则此方法将返回false。
    在JDK API中：
    ![image-20230412174300752](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20230412174300752.png)

-   prestartAllCoreThreads：调用一次该方法启动所有核心线程，返回值为启动成功的线程的数量。覆盖仅在执行新任务时启动核心线程的默认策略。如果核心线程全部启动后再次调用，则会返回0

Demo:
com.mkevin.demo17.prestartAllCoreThreadsDemo

# 11. 自定义线程及线程池切面

切面

线程及线程池切面

-   在线程执行前、执行后增加切面，在线程池关闭时执行某段程序。

-   需要实现自己的线程池类（继承ThreadPoolExecutor），并覆写beforeExecute、afterExecute、terminated方法

Demo：
com.mkevin.demo19.BeforAfterTerminatedDemo

# 12. 移除线程池中的任务

怎样删除线程池中的任务

移除线程池中的任务

- 使用线程池的remove方法

- 已经正在运行中的任务不可以删除，该方法会返回false

- execute方法提交的，未运行的任务可以删除

- submit方法提交的，未运行任务也不可以删除，小心采坑！

**Demo**：
com.mkevin.demo20.TaskRemoveDemo



# 13. 获取各种线程池状态数据

大量的get方法怎么玩?

获取各种线程池状态数据

可以获取线程池的各种动态和静态数据，用于程序控制。

-   返回当前线程池在创建时指定的核心线程数：getCorePoolSize

-   返回当前线程池中的线程数：getPoolSize

-   返回最大允许的线程数：getMaximumPoolSize

-   返回池中同时存在的最大线程数：getLargestPoolSize

-   返回预定执行的任务总和：getTaskCount【这个方法得到的是一个预估值，不一定准确】

-   返回当前线程池已经完成的任务数：getCompletedTaskCount

-   返回正在执行任务的线程的大致数目：getActiveCount

-   返回线程池空闲时间：getKeepAliveTime【调用的时候传入一个参数：时间的单位】

Demo：
com.mkevin.demo18.ThreadPoolGetDemo

# 14. 设计模式-单例模式

- 饿汉模式：类加载的时候，就进行对象的创建，系统开销较大，但是不存在线程安全问题。
  饿汉示例： DemoThread22
  示例化简后的形式

  ```java
  class Singleton1 {
      private static Singleton1 singleton = new Singleton1(); // 建立对象
      private Singleton1() {}
  }
  ```

  

- 懒汉模式：多数采用饿汉模式，在使用时才真正的创建单例对象，但是存在线程安全问题

  - 懒汉示例： DemoThread23 （线程安全问题和解决方案）
    化简后的存在线程安全问题的示例：

    ```java
    class Singleton2 {
    	private static Singleton2 singleton = null; // 不建立对象
    	private Singleton2() {
    	}
    	/*synchronized 可以解决线程安全问题,但是存在性能问题,即使singleton!=null也需要先获得锁*/
    	public /*synchronized*/ static Singleton2 getInstance() {
    		if (singleton == null) { // 先判断是否为空
    			//一系列操作...
    			singleton = new Singleton2(); // 懒汉式做法
    		}
    		return singleton;
    	}
    }
    ```

    线程安全问题存在的原因可以去看《Java核心技术卷Ⅰ 第十二章 并发.xmind》中对voliate关键字的介绍，导致多个线程分别创建多个不同的实例对象。

    解决示例中线程安全问题最简单的方式就是直接在方法上使用synchronized关键字

    
  
- 懒汉模式： DemoThread24 （线程安全的性能优化），在这个示例中没有使用voliate关键字保证对象在创建的时候保持指令的有序性，可能会在使用这个对象的时候出现问题，相关东西可以去看《Java核心技术卷Ⅰ 第十二章 并发.xmind》中堆voliate关键字的介绍。
  
  
  
- 静态内部类单例：兼具懒汉模式和饿汉模式的优点

  -   静态内部类单例： DemoThread25

  ```java
  /**静态内部类-单例对象构建方法
  (
  利用JDK的特性：类级内部类只有在第一次被使用的时候才被会装载（被初始化），
  这样可以保证单例对象只有在第一次被使用的时候初始化一次，
  并且不需要加锁，性能得到大大提高，并且保证了线程安全。
  )
  */
  class Singleton4 {
      private static class InnerSingletion {
          private static Singleton4 single = new Singleton4();
      }
  
      private Singleton4(){}
  
      public static Singleton4 getInstance(){
          return InnerSingletion.single;
      }
  }
  ```

  

# 15. 设计模式-Future

- 简单来说,客户端请求之后，先返回一个应答结果，然后异步的去准备数据，客户端可以先去处理其他事情，当需要最终结果的时候再来获取, 如果此时数据已经准备好，则将真实数据返回；如果此时数据还没有准备好，则阻塞等待。

- 模拟Future设计模式的示例： com.mimaxueyuan.demo.high.futrue包下的Main类是程序的入口
  在该包下对于Future设计模式的模拟的类图：

  ![image-20220826160848667](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220826160848667.png)

  这是Main类执行的时序图：

  ![image-20220821090044126](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220821090044126.png)

- JDK的Concurrent包提供了Futrue模式的实现，可以直接使用。

- 使用Futrue模式需要实现Callable接口，并使用FutureTask进行封装，使用线程池进行提交。

- 示例：com.mimaxueyuan.demo.high.futrue.JdkFuture
  在这个类中使用JDK提供的Future模式，最终的表现结果和上面模拟Future模式一样；
  代码：

  ```java
  public class JdkFuture implements Callable<String>{
      private String para;
  
      public JdkFuture(String para){
          this.para = para;
      }
  
      /**
  	 * 这里是真实的业务逻辑，其执行可能很慢
  	 */
      @Override
      public String call() throws Exception {
          //模拟执行耗时
          Thread.sleep(5000);
          String result = this.para + "处理完成";
          return result;
      }
  
      //主控制函数
      public static void main(String[] args) throws Exception {
          String queryStr = "zhangsan";
  
          //构造FutureTask，并且传入需要真正进行业务逻辑处理的类,该类一定是实现了Callable接口的类
          FutureTask<String> future = new FutureTask<String>(new JdkFuture(queryStr));
          FutureTask<String> future2 = new FutureTask<String>(new JdkFuture(queryStr));
  
          //创建一个固定线程的线程池且线程数为1，上面创建的FutureTask任务可以放在这个线程池中。
          ExecutorService executor = Executors.newFixedThreadPool(2);
          //这里提交任务future,则开启线程执行JdkFuture的call()方法执行
          //submit和execute的区别： 第一点是submit可以传入实现Callable接口的实例对象， 第二点是submit方法有返回值
  
          Future f1 = executor.submit(future);		//单独启动一个线程去执行的
          Future f2 = executor.submit(future2);
          System.out.println("请求完毕");
  
          try {
              //这里可以做额外的数据操作，也就是主程序执行其他业务逻辑
              System.out.println("处理实际的业务逻辑...");
              Thread.sleep(1000);
          } catch (Exception e) {
              e.printStackTrace();
          }
  
          //调用获取数据方法,如果call()方法没有执行完成,则依然会进行等待
          System.out.println("数据：" + future.get());
          System.out.println("数据：" + future2.get());
  
          executor.shutdown();
      }
  }
  ```

  这段代码中涉及到的类的类图：
  ![image-20220826164247619](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220826164247619.png)



# 16. 设计模式-Producer-Consumer

-   Producer-Consumer称为生产者消费者模式，是消息队列中间件的核心实现模式，ActiveMQ、RocketMQ、Kafka、RabbitMQ。
-   示例： com.mimaxueyuan.demo.high.mq.Main

# 17. 设计模式-Master-Worker

-   Master-Worker模式是一种将串行任务并行化的方案，被分解的子任务在系统中可以被并行处理，同时，如果有需要，Master（这个Master不是一个任务，没有实现Runnable/Callable接口）进程不需要等待所有子任务都完成计算，就可以根据已有的部分结果集计算最终结果集。
-   客户端将所有任务提交给Master，Master分配Worker去并发处理任务，并将每一个任务的处理结果返回给Master，所有的任务处理完毕后,由Master进行结果汇总再返回给Client
-   Master和Worker共享两个对象，通过这两个对象可以让它们进行交互；这两个对象分别是：一个存放Task的CurrentLinkedQueue队列、一个存放结果的CurrentHashMap；
    责任：
    1. Master：
       1. 将任务（注意：这里说的任务并不是实现了一个Runnable/Callable接口的类的实例对象）存放在CurrentLinkedQueue队列中；
       2. 同时将用户传递进来的Worker封装到不同的Thread中【注意：在代码中创建Master时只传递了一个Worker的实例对象，但是创建了多个Thread，它们都封装的是同一个Worker】，并把这个Thread和这个线程对应的名字封装到一个HashMap中，线程名作为Map的key，线程的实例对象作为Map的value；
       3. 从Worker存放任务结果的ConcurrentHashMap中取出结果
    2. Worker：
       1. 从Master存放任务（注意：这个任务并不是实现了Runnable/Callable接口的类的实例对象）的CurrentLinkedQueue队列中取出任务，并调用自己的方法处理这个任务
       2. 向ConcurrentHashMap中存放当前Worker实例对象对已获得的任务的处理结果
-   示例： com.mimaxueyuan.demo.high.masterworker.Main

![image-20220821090643369](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220821090643369.png)

![image-20220821090724868](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220821090724868.png)



# 18. CompletionService

轻松搞定Master Worker模式

CompletionService接口

![image-20220821095229151](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220821095229151.png)

这张图说明了CompletionService的使用模式

你可用它不断的提交任务（线程）给Executor处理

处理后的结果都会自动放入BlockedQueue另外一个线程不断的从队列里取得处理结果

好处是，哪个任务先处理完就能先得到哪个结果最后做汇总处理

从而轻松完成MasterWorker模式相同的功能

![image-20220821095324694](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220821095324694.png)

这张图说明了CompletionService的本质：就是线程池Executor加上阻塞队列BlockedQueue，线程池Executor用来处理任务，BlockedQueue用来存储每个线程的运行结果。

![image-20220821095421041](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220821095421041.png)

-   submit用于提交任务

-   take用于获取处理结果（阻塞式），得到一个Future对象

-   poll也用于获取处理结果（非阻塞式）



**Demo** ：
com.mkevin.demo11.CompletionServiceDemo0 

com.mkevin.demo11.CompletionServiceDemo1（这个示例的目的是：证明在使用take获取结果的时候，即使提交的任务的call方法中抛出了异常，也会将每个线程的运行结果存放在阻塞队列中，只是在调用take方法返回的Future对象的get方法获得值的时候才会报异常）

com.mkevin.demo11.CompletionServiceDemo2（这个示例的目的是：展示在使用submit(Runnable,V)的时候怎么获得返回结果，因为Runnable接口中的run方法是没有返回值的；可以通过在Runnable接口的实现类中加入一个“存放结果对象（可以使是们自定义的对象）”，在run方法运行结束的时候，会把运行的结果存放在这个“存放结果”的对象中）

```java
/**
 * 让Runnable也具有获得结果的特性
 */
public class CompletionServiceDemo2 {
    public static void main(String[] args) {
        try {
            //创建结果对象，用于获取结果
            Result result = new Result();
            ExecutorService executorService = Executors.newCachedThreadPool();
            CompletionService cs = new ExecutorCompletionService(executorService);
            //创建Runnable对象，在创建SalaryRunner2类的实例对象的时候，会把存放结果的Result的对象
            //的引用传递到SalaryRunner2类中，在SalaryRunner2类的run方法执行完毕的时候会对这个对象
            //进行赋值
            SalaryRunner2 runner1 = new SalaryRunner2(result, 10, 1000);
            SalaryRunner2 runner2 = new SalaryRunner2(result, 20, 2000);
            //提交对象, 并使用result对象接收结果，这个对象在SalaryRunner2类中也有定义
            Future<Result> f1 = cs.submit(runner1,result);
            Future<Result> f2 = cs.submit(runner2,result);
            //获取结果
            System.out.println("f1:" + f1.get().getValue());
            System.out.println("f2:" + f2.get().getValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

```java
/**
 * 结果计算
 */
public class SalaryRunner2 implements Runnable {

    //结果,因为这个类实现的Runnable接口中的run方法没有返回值
    //所以，可以在这个类中增加一个存放结果的对象，这个对象是
    //通过参数被初始化的，在run方法执行完的时候会给这个对象的
    //属性赋值
    private Result result;
    //工资
    private long salary;
    //耗时
    private long costTime;

    public SalaryRunner2(Result result,long salary, long costTime){
        this.result = result;
        this.costTime = costTime;
        this.salary=salary;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(this.costTime);
            this.result.setValue(this.salary*1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

com.mkevin.demo11.CompletionServiceDemo3（这个示例中展示了：一个线程向CompletionService的线程池中不断地提交任务，另一个线程不断地从CompletionService地阻塞队列中获取）



# 19. ForkJoin

ForkJoin模式的使用

ForkJoin思想

![image-20220821095538680](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220821095538680.png)



**ForkJoin使用**

![image-20220821095554484](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220821095554484.png)

-   两个重要的实现类，又同时是抽象类
-   *java.util.concurrent.RecursiveTask*  递归任务类
-   *java.util.concurrent.RecursiveAction*  递归活动类
-   ForkJoin的使用方式：
    -   先创建一个ForkJoinPool
    -   之后创建RecursiveTask或RecursiveAction的子类的实例对象提交到ForkJoinPool中

Demo：
com.mkevin.demo12.ForkJoinDemo0 至 ForkJoinDemo7

ForkJoinDemo0：展示了简单的ForkJoinTask的子类RecursiveAction的使用
ForkJoinDemo1：展示了怎么获得RecursiveTask类中compute方法的执行结果
ForkJoinDemo2：展示了提交到ForkJoinPool中的任务可以是不同的任务
ForkJoinDemo3：没有这个类
ForkJoinDemo4：分治模型的典型例子，展示了计算1+2+3+4+……+10使用ForkJoinTask的情况
分治思想：
![image-20220829142045138](D:\Tyora\AssociatedPicturesInTheArticles\Java并发编程精通篇-2\image-20220829142045138.png)
		窃取算法：别的线程没做完的事情，我在做完了我自己的事情之后去帮他们做。



ForkJoinDemo5：ForkJoinPool可以直接执行Runnable类型的任务（需要获取返回结果）
ForkJoinDemo6
ForkJoinDemo7：说明ForkJoinPool中也有shoutdown方法



# 20. DeepSeek介绍FrokJoin

### Java.util.concurrent.ForkJoinTask 全面解析

#### **1. 概述**
`ForkJoinTask` 是 Java 并发框架中用于实现 **分治算法（Divide-and-Conquer）** 的核心抽象类，专为高效利用多核处理器设计。它通过将大任务递归拆分为小任务（Fork），并行执行后合并结果（Join），显著提升计算密集型任务的性能。其核心实现依赖于 `ForkJoinPool` 线程池的 **工作窃取（Work-Stealing）** 算法。

---

#### **2. 核心机制与设计思想**
- **分治策略**：任务递归分解为子任务，直到足够小可直接计算。
- **工作窃取（Work-Stealing）**：
  - 每个线程维护一个双端队列（Deque），优先处理自己的任务。
  - 空闲线程从其他队列的 **尾部** 窃取任务，减少竞争并平衡负载。
  - 特点：避免线程饥饿，最大化 CPU 利用率。
- **轻量级任务调度**：任务由线程自身调度，减少上下文切换。

---

#### **3. 核心子类与使用方式**
- **`RecursiveAction`**：无返回值的任务（如排序、遍历）。
  ```java
  class MyAction extends RecursiveAction {
      @Override
      protected void compute() {
          if (任务足够小) {
              // 直接执行
          } else {
              // 拆分任务，fork() 子任务，invokeAll() 触发执行
          }
      }
  }
  ```
  
- **`RecursiveTask<T>`**：有返回值的任务（如累加、搜索）。
  ```java
  class MyTask extends RecursiveTask<Integer> {
      @Override
      protected Integer compute() {
          if (任务足够小) {
              return 直接计算结果;
          } else {
              MyTask left = new MyTask(...);
              MyTask right = new MyTask(...);
              left.fork(); // 异步执行左子任务
              int rightResult = right.compute(); // 同步执行右子任务
              int leftResult = left.join(); // 等待左任务结果
              return merge(leftResult, rightResult);
          }
      }
  }
  ```

---

#### **4. 关键方法详解**
- **`fork()`**：将任务异步提交到当前线程的队列。
- **`join()`**：阻塞等待任务完成并返回结果（或抛出异常）。
- **`invoke()`**：同步执行任务并返回结果（等效于 `fork()` + `join()`）。
- **`invokeAll(ForkJoinTask<?>... tasks)`**：并行执行多个任务。

---

#### **5. 异常处理**
- **受检异常**：无法通过 `compute()` 直接抛出，需在代码中捕获处理。
- **未受检异常**：可通过 `getException()` 或 `join()` 捕获。
  ```java
  if (task.isCompletedAbnormally()) {
      ThrowexException ex = task.getException();
      // 处理异常
  }
  ```

---

#### **6. 使用场景**
- **分治算法**：如归并排序、快速排序。
- **递归并行计算**：大规模数组处理（求和、求最大值）。
- **并行流（Parallel Streams）**：Java 8+ 的并行流底层依赖 `ForkJoinPool`。
- **树/图遍历**：并行处理树节点或图结构。

---

#### **7. 性能优化与最佳实践**
- **合理任务粒度**：避免拆分过细（如数组长度阈值设为 1000-10000）。
- **避免阻塞操作**：I/O 或同步代码会降低并行效率。
- **无状态设计**：任务独立，避免共享可变状态；若必须共享，使用线程安全结构。
- **结果合并优化**：合并操作应尽量高效（如累加比链表合并更优）。

---

#### **8. 示例：并行计算数组和**
```java
class SumTask extends RecursiveTask<Long> {
    private final int[] array;
    private final int start, end;

    SumTask(int[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        if (end - start <= 1000) { // 阈值设为1000
            long sum = 0;
            for (int i = start; i < end; i++) sum += array[i];
            return sum;
        } else {
            int mid = (start + end) / 2;
            SumTask left = new SumTask(array, start, mid);
            SumTask right = new SumTask(array, mid, end);
            left.fork();
            return right.compute() + left.join();
        }
    }
}

// 使用方式
ForkJoinPool pool = new ForkJoinPool();
long result = pool.invoke(new SumTask(array, 0, array.length));
```

---

#### **9. 注意事项与局限性**
- **不适合I/O密集型任务**：线程阻塞会降低吞吐量。
- **避免过度拆分**：任务管理开销可能超过并行收益。
- **共享状态需谨慎**：若必须共享数据，使用 `Phaser`、`CyclicBarrier` 或并发集合。

---

#### **10. 高级主题**
- **`CountedCompleter`**：复杂依赖任务，子任务完成时触发回调。
- **`ManagedBlocker`**：处理可能阻塞的任务（如I/O），避免线程饥饿。
- **自定义 `ForkJoinPool`**：调整并行度（如指定线程数）。

---

#### **11. 与其他并发工具对比**
| **工具**            | **适用场景**         | **特点**                       |
| ------------------- | -------------------- | ------------------------------ |
| `ForkJoinTask`      | 分治、递归并行任务   | 工作窃取、自动任务拆分         |
| `ExecutorService`   | 独立任务、固定线程池 | 无任务窃取，适合异构任务       |
| `CompletableFuture` | 异步编程、任务流水线 | 组合多个异步操作，灵活回调处理 |

---

#### **12. 总结**
`ForkJoinTask` 是处理 **可拆分计算密集型任务** 的利器，通过分治与工作窃取最大化多核性能。使用时需注意任务粒度、避免阻塞，合理设计任务结构。对于复杂依赖或I/O场景，可结合其他并发工具（如 `CompletableFuture`）实现高效并行处理。


















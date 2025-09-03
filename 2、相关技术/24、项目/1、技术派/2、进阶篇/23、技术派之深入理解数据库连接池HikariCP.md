大家好，我是技术派的二哥呀！

今天由我来给大家讲一下技术派的数据库配置 HikariCP，虽然从 Spring Boot 2.x 开始，HikariCP 成为了 Spring Boot 默认的数据库连接池，意味着我们不需要额外的配置就可以使用 HikariCP，但作为有追求的技术人来说，我们不应该就此忽视 HikariCP 的存在。

这篇内容会从源码的角度来带大家看一下 HikariCP 的默认配置，以及为什么 Spring Boot 会选择 HikariCP，HikariCP 到底快在哪里？什么是线程连接池？包括 HikariCP 的核心源码解析。让大家真正能从 HikariCP 学到点什么 🤔。

HikariCP 目前在 GitHub 上已有 18k+ 的 star 了，上一次更新还是在两年前，说明已经非常稳定了（手动狗头）。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309102218415.png)

Hikari 来源于日语，也就是“光”的意思，这意味着 HikariCP 快得像光速一样！讲真，看简介（又快又简单又好用）的感觉就好像在和我的女神“汤唯”握手一样刺激和震撼。

# 1、整合HikariCP

既然已经是 Spring Boot 的默认数据库连接池了，也就意味着不需要在 pom.xml 中引入 HikariCP 的 Maven 坐标了。在项目的终端中执行以下命令：
> mvn depenedncy:tree

我们可以在 Mybatis-Plus 的依赖中找得到 HikariCP 的影子。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309102217159.png)

接着，在 application.yml 文件中添加数据库链接信息就好了。
```yml
database:
  name: pai_coding
spring:
  datasource:
    # 数据库名，从配置 database.name 中获取
    url: jdbc:mysql://localhost:3306/${database.name}?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: xxxxxx
```

如果使用的是 MySQL 数据库，记得添加 MySQL 的依赖。

```properties
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```

启动项目，就可以在控制台中看到 HikariCP 的连接池信息。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309102220499.png)

# 2、HikariCP的默认配置

HikariCP 的默认配置可以在 com.zaxxer.hikari.HikariConfig 类中找得到。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309102220408.png)

下面是这些配置选项的简要说明：
- mindle：连接池中保持的最小空闲连接数。默认值为-1，表示将使用maximumPoolSize作为最小空闲连接数。
- maxPoolSize：最大连接池大小。默认值为-1，表示将使用默认的最大连接池大小，即10。
- maxLifetime：连接在连接池中的最长生命周期（以毫秒为单位）。默认值为 MAX_LIFETIME，即 1800000（30 分钟）。
- connectionTimeout：等待获取连接的最长时间（以毫秒为单位）。默认值为 CONNECTION_TIMEOUT，即 30000（30 秒）。
- validationTimeout：连接验证超时时间（以毫秒为单位）。默认值为 VALIDATION_TIMEOUT，即 5000（5 秒）。
- idleTimeout：连接空闲超过此时间（以毫秒为单位）后，将从连接池中删除。默认值为 IDLE_TIMEOUT，即 600000（10 分钟）。
- initializationFailTimeout：连接池初始化失败的超时时间（以毫秒为单位）。默认值为 1，表示初始化失败时立即抛出异常。
- isAutoCommit：是否自动提交。默认值为 true，表示连接池中的连接会自动提交事务。
- keepaliveTime：连接的保活时间（以毫秒为单位）。默认值为 DEFAULT_KEEPALIVE_TIME，即 0，表示禁用保活功能。

搞清楚了这些配置项，我们就很容易针对不同的项目来改变默认的配置，比如说下面是一个高并发项目的链接配置。
```yml
spring:
  datasource:
    # 数据库名，从配置 database.name 中获取
    url: jdbc:mysql://localhost:3306/${database.name}?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: xxxxx
    hikari:
      pool-name: HighConcurrencyHikariCP
      minimum-idle: 20
      maximum-pool-size: 50
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      auto-commit: true
      leak-detection-threshold: 60000
```

pool-name 被设置为 HighConcurrencyHikariCP，可以方便我们在日志记录中进行区分。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309102234532.png)


# 3、什么是数据库连接池

说完 HikariCP 的默认配置，我们再来说说什么是数据库连接池（Database Connection Pooling）。

数据库连接池和我们常用的线程池一样，都属于池化技术，它在程序初始化时创建一定数量的数据连接对象并将其保存在一块内存当中。

它允许应用程序重复使用一个现有的数据库连接，当需要执行SQL时，直接从连接池中获取一个连接，而不是重新建立一个数据库连接，当SQL执行完，也并不是将数据库连接直接丢弃，而是将其归还到数据库连接池中。

数据库连接池示意图如下所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309102239166.png)

数据库连接池的主要目的是提高性能和资源利用率。创建和关闭数据库连接会产生额外的开销，尤其是在高并发的场景下。通过使用连接池，应用程序可以复用已经创建好的连接，减少创建新连接所需要的时间和资源。此外，连接池还可以根据需求自动调整连接数量，以适应不同的负载情况。

# 4、为什么SpringBoot会选择HikariCP？

HikariCP 团队为了证明自己性能最佳，特意找了几个背景对比了下。不幸充当背景的有 c3p0、dbcp2、tomcat 等传统的连接池。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309102245691.png)

没有大家比较喜欢的 Druid，比较遗憾，不过 vivo 技术团队曾说：HikariCP 在性能上是完全优于 Druid 连接池的，主要是锁机制的不同。不过，Druid 提供了更丰富的功能，比如说监控、SQL 拦截和解析。

从上图中，我们能感受出背景的尴尬，HikariCP 可以说是鹤立鸡群了。HikariCP 如此优秀的原因大致有下面这些：

## 4.1、字节码级别上的优化

要求编译后的字节码最少，这样 CPU 缓存就可以加载更多的程序代码。

HikariCP 优化前的代码片段：
```java
public final PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
{
    return PROXY_FACTORY.getProxyPreparedStatement(this, delegate.prepareStatement(sql, columnNames));
}
```

HikariCP 优化后的代码片段：

```java
public final PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
{
    return ProxyFactory.getProxyPreparedStatement(this, delegate.prepareStatement(sql, columnNames));
}
```

以上两段代码的差别只有一处，就是 ProxyFactory 替代了 PROXY_FACTORY，这个改动后的字节码比优化前减少了 3 行指令。具体的分析参照 HikariCP 的 Wiki 文档。
> https://github.com/brettwooldridge/HikariCP/wiki/Down-the-Rabbit-Hole

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309102247506.png)

## 4.2、JDK工具库级别上的优化

使用自定义的列表（FastList）代替 ArrayList，可以避免 get() 的时候进行范围检查，remove() 的时候从头到尾的扫描。

虽然是很细微的改变，但积少成多，HikariCP 在这方面也是下足了功夫。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309102248656.png)

再比如说，使用无锁的 ConcurrentBag 来管理连接池，最大程度上提高连接池的性能。该类也是我们接下来要重点分析的。

# 5、HikariCP的核心源码解析

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401012202512.png)



ConcurrentBag 是一个 Lock free 的数据结构，主要用作数据库连接的存储，可以说整个 HikariCP 的核心就是它。删掉乱七八糟的注释和异常处理，关键的代码也就百十来行，但里面的道道却非常的多。
```java
public class ConcurrentBag<T extends IConcurrentBagEntry> implements AutoCloseable  
{  
   private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentBag.class);  
  
   private final CopyOnWriteArrayList<T> sharedList;  
   private final boolean weakThreadLocals;  
  
   private final ThreadLocal<List<Object>> threadList;  
   private final IBagStateListener listener;  
   private final AtomicInteger waiters;  
   private volatile boolean closed;  
  
   private final SynchronousQueue<T> handoffQueue;
   
   public ConcurrentBag(final IBagStateListener listener)  
	{  
	   // IBagStateListener必须传入
	   this.listener = listener;  
	   // threadList是否使用WeakReference保存元素
	   this.weakThreadLocals = useWeakThreadLocals();  
	   // 交接队列，fair=true
	   this.handoffQueue = new SynchronousQueue<>(true);  
	   // 等待线程数量
	   this.waiters = new AtomicInteger();  
	   // 保存容器内所有元素
	   this.sharedList = new CopyOnWriteArrayList<>();  
	   if (weakThreadLocals) {  
		  // 如果使用WeakReference，用ArrayList
	      this.threadList = ThreadLocal.withInitial(() -> new ArrayList<>(16));  
	   }  
	   else {  
	      // 否则使用FastList，默认
	      this.threadList = ThreadLocal.withInitial(() -> new FastList<>(IConcurrentBagEntry.class, 16));  
	   }  
	}  
  
	public T borrow(long timeout, final TimeUnit timeUnit) throws InterruptedException  
	{  
	   // Try the thread-local list first  
	   final List<Object> list = threadList.get();  
	   for (int i = list.size() - 1; i >= 0; i--) {  
	      final Object entry = list.remove(i);  
	      @SuppressWarnings("unchecked")  
	      final T bagEntry = weakThreadLocals ? ((WeakReference<T>) entry).get() : (T) entry;  
	      if (bagEntry != null && bagEntry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {  
	         return bagEntry;  
	      }  
	   }  
	  
	   // Otherwise, scan the shared list ... then poll the handoff queue  
	   final int waiting = waiters.incrementAndGet();  
	   try {  
	      for (T bagEntry : sharedList) {  
	         if (bagEntry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {  
	            // If we may have stolen another waiter's connection, request another bag add.  
	            if (waiting > 1) {  
	               listener.addBagItem(waiting - 1);  
	            }  
	            return bagEntry;  
	         }  
	      }  
	  
	      listener.addBagItem(waiting);  
	  
	      timeout = timeUnit.toNanos(timeout);  
	      do {  
	         final long start = currentTime();  
	         final T bagEntry = handoffQueue.poll(timeout, NANOSECONDS);  
	         if (bagEntry == null || bagEntry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {  
	            return bagEntry;  
	         }  
	  
	         timeout -= elapsedNanos(start);  
	      } while (timeout > 10_000);  
	  
	      return null;   
	  }  
	   finally {  
	      waiters.decrementAndGet();  
	  }  
	}
	
	public void requite(final T item) {
	}

	public boolean add(final T item) {
	  sharedList.add(bagEntry);
	}
	
	public boolean remove(final T item) {
	  // 从 sharedList 中移除连接
	  return sharedList.remove(item);
	}
	
	public int size() {
	  return sharedList.size();
	}
	
	private boolean useWeakThreadLocals() {  
	    try  
	    {  
	        // 如果系统变量（-D参数或环境变量）有配置com.zaxxer.hikari.useWeakReferences，走系统变量配置  
	        // 这个没有标注在文档里，因为一般不建议修改  
	        if (System.getProperty("com.zaxxer.hikari.useWeakReferences") != null)  
	        {  
	            of WeakReference behavior  
	            return Boolean.getBoolean("com.zaxxer.hikari.useWeakReferences");  
	        }  
	        // 如果当前类加载器和系统类加载器不一致，返回true  
	        return getClass().getClassLoader() != ClassLoader.getSystemClassLoader();  
	    } catch (SecurityException se)  
	    {  
	        return true;  
	    }  
	}
}
```

要想速度快，就需要一些关键的数据结构来支持，并且最好是支持并发环境的。
```java
private final CopyOnWriteArrayList<T> sharedList;
private final ThreadLocal<List<Object>> threadList;
private final AtomicInteger waiters;
private final SynchronousQueue<T> handoffQueue;
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401012204142.png)

- sharedList（类型为 `CopyOnWriteArrayList<T>`）：这是一个线程安全的共享列表，用于存储对象池中的对象。当多个线程需要访问列表时，它提供了高性能的读取操作，同时支持并发写入操作。
	- CopyOnWriteArrayList sharedList：保存容器内所有元素，使用CopyOnWriteArrayList，写操作加锁并复制底层数据，适用于读多写少的场景。HikariCP作者建议连接池最大连接数与最小连接数保持一致，这可能也是其中一个原因。如果连接池配置为弹性容量，遇到突发流量，sharedList扩张就导致CopyOnWriteArrayList加锁并做数组拷贝；流量过后，sharedList收缩也会导致加锁和数组拷贝。[来自掘金](https://juejin.cn/post/6887371883810357255)
- boolean weakThreadLocals：对于threadList中的元素是否使用WeakReference保存，默认否。[来自掘金](https://juejin.cn/post/6887371883810357255)
- threadList（类型为 `ThreadLocal<List<Object>>`）：这是一个线程局部变量，用于存储每个线程独立的对象列表。这样可以在不同的线程之间隔离对象，减少锁的竞争和提高性能。
	- 当前线程持有的元素。可以认为sharedList包含了所有的threadList里的元素。[来自掘金](https://juejin.cn/post/6887371883810357255)
- IBagStateListener listener：通知外部往ConcurrentBag加入元素[来自掘金](https://juejin.cn/post/6887371883810357255)
- waiters（类型为 AtomicInteger）：这是一个原子整数，用于记录正在等待可用对象的线程数量。通过原子操作实现线程安全地增加和减少等待线程的数量。
- boolean closed：标记ConcurrentBag是否关闭，默认false。当关闭后ConcurrentBag无法添加新元素。[来自掘金](https://juejin.cn/post/6887371883810357255)
- handoffQueue（类型为 `SynchronousQueue<T>`）：这是一个无容量的阻塞队列，用于在线程之间传递对象。它允许将对象从生产者线程直接传递给消费者线程，而无需将对象存储在队列中。这有助于减小内存占用，同时实现了高效的对象传输。
	- 接队列。主要用到SynchronousQueue的两个方法`offer`（当没有线程获取走offer的元素时返回false）和`poll(timeout,unit)`（指定时间内没有获取到元素时返回null）。[来自掘金](https://juejin.cn/post/6887371883810357255)

ConcurrentBag 里面的元素，为了能够无锁化操作，需要使用一些变量来标识现在处于的状态。抽象的接口如下：
```java
public interface IConcurrentBagEntry{
	// STATE_NOT_IN_USE：未使用。可以被借走
    int STATE_NOT_IN_USE = 0;
    // STATE_IN_USE：正在使用
    int STATE_IN_USE = 1;
    // STATE_REMOVED：被移除，只有调用remove方法时会CAS改变为这个状态，修改成功后会从容器中被移除
    int STATE_REMOVED = -1;
    // STATE_RESERVED：被保留，不能被使用。往往是移除前执行保留操作。
    int STATE_RESERVED = -2;
    
    boolean compareAndSet(int expectState, int newState);
    void setState(int newState);
    int getState();
}
```

## 5.0、add新增元素

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401012205924.png)

Add a new object to the bag for others to borrow. 新增一个元素到bag里，供borrow方法借出。
```java
private volatile boolean closed;
private final CopyOnWriteArrayList<T> sharedList;
private final AtomicInteger waiters;
private final SynchronousQueue<T> handoffQueue;
public void add(final T bagEntry) {
  // 如果容器关闭，抛出IllegalStateException
  if (closed) {
     throw new IllegalStateException("ConcurrentBag has been closed, ignoring add()");
  }
  // 放入sharedList，此时其他线程已经可以获取这个元素了
  sharedList.add(bagEntry);
  // 持续尝试将元素放入交接队列
  while (waiters.get() > 0 && bagEntry.getState() == STATE_NOT_IN_USE && !handoffQueue.offer(bagEntry)) {
  	 // 当前线程主动放弃cpu执行，回到就绪状态
     Thread.yield();
  }
}
```

重点关注`waiters.get() > 0 && bagEntry.getState() == STATE_NOT_IN_USE && !handoffQueue.offer(bagEntry)`这个循环条件。

- `waiters.get() > 0`：需要有正在等待获取元素的线程，才会循环。
- `bagEntry.getState() == STATE_NOT_IN_USE`：因为元素已经放入shareList了，可能被其他线程改变状态，需要判断当前元素仍然是未使用状态。
- `!handoffQueue.offer(bagEntry)`：尝试放入交接队列，如果失败继续循环。

## 5.1、borrow

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401012219040.png)

The method will borrow a BagEntry from the bag, blocking for the specified timeout if none are available. 从bag中借出元素，如果没有可以获取的元素，会阻塞指定时长。

borrow 方法用来获取连接：
```java
public T borrow(long timeout, final TimeUnit timeUnit) throws InterruptedException
```

### 5.1.1、
①、使用 ThreadLocal 的方式快速获取连接对象。这段代码的作用是优先从当前线程的局部列表中获取可用的对象，以减少在多线程环境下的资源竞争。
```java
// 1. Try the thread-local list first
final var list = threadList.get();
for (int i = list.size() - 1; i >= 0; i--) {
   final var entry = list.remove(i);
   @SuppressWarnings("unchecked")
   final T bagEntry = weakThreadLocals ? ((WeakReference<T>) entry).get() : (T) entry;
   // CAS修改元素状态为使用中 // 因为元素可能被其他线程偷取，所以要cas修改状态
   if (bagEntry != null && bagEntry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {
      return bagEntry;
   }
}
```

- 通过调用 threadList.get() 获取当前线程的局部列表。
- 从列表的尾部开始遍历（这样做的原因是：新加入的对象通常位于列表尾部，这样可以提高获取可用对象的效率）。
- 移除当前遍历到的元素，并将其赋值给 entry。
- 如果 weakThreadLocals 为真，说明使用的是弱引用，需要将 entry 强制转换为 `WeakReference<T>` 类型，然后调用 get() 方法获取对象。否则，直接将 entry 强制转换为 T 类型。将结果赋值给 bagEntry。
- 检查 bagEntry 是否为 null，且其状态是否可以从未使用（STATE_NOT_IN_USE）更新为使用中（STATE_IN_USE）。如果条件满足，返回 bagEntry

在 ConcurrentBag 里，每个 ThreadLocal 最多缓存 50 个连接对象引用。这个数量在 requite 方法中有所体现。
```java
public void requite(final T bagEntry)
{
  final var threadLocalList = threadList.get();
  if (threadLocalList.size() < 50) {
     threadLocalList.add(weakThreadLocals ? new WeakReference<>(bagEntry) : bagEntry);
  }
}
```

- 首先获取当前线程的 ThreadLocal 缓存列表：threadLocalList。
- 检查 threadLocalList 的大小是否小于 50。
- 如果 threadLocalList 的大小小于 50，则将对象 bagEntry 添加到本地缓存列表中。如果 weakThreadLocals 为 true，则将 bagEntry 包装为一个 WeakReference 对象再添加到列表中；否则，直接添加 bagEntry。

通过这种方式，每个线程的 ThreadLocal 缓存列表中最多只会存储 50 个对象。当对象数量达到这个限制时，不会再向本地缓存列表中添加更多的对象。这有助于控制内存使用，并在一定程度上提高了多线程环境下资源的共享效率。

### 5.1.2、
②、当 ThreadLocal 里找不到可复用的连接对象，会到大池子里去取。
```java
// 增加等待资源的线程数量
final int waiting = waiters.incrementAndGet();

try {
   // 2. 遍历共享列表以查找可用的资源
   for (T bagEntry : sharedList) {
      // 尝试将资源的状态从 STATE_NOT_IN_USE 更改为 STATE_IN_USE
      if (bagEntry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {
         // 如果我们可能抢占了其他等待者的连接，请求添加一个新的资源到 bag
         if (waiting > 1) {
            listener.addBagItem(waiting - 1);
         }
         // 返回找到的可用资源
         return bagEntry;
      }
   }

   // 在共享列表中没有找到可用资源时，通知 listener 添加一个新的资源
   listener.addBagItem(waiting);
   
   // 将 timeout 转换为纳秒
   timeout = timeUnit.toNanos(timeout);
   // 循环尝试从 handoffQueue 获取资源，直到超时
   do {
      final var start = currentTime();
      // 3. 尝试从交接队列获取元素
      final T bagEntry = handoffQueue.poll(timeout, NANOSECONDS);
      // 如果从 handoffQueue 获取到资源且其状态为 STATE_NOT_IN_USE，将其设置为 STATE_IN_USE 并返回
      if (bagEntry == null || bagEntry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {
         return bagEntry;
      }

      // 更新剩余超时时间
      timeout -= elapsedNanos(start);
   } while (timeout > 10_000);

   // 如果超时，则返回 null
   return null;
}
// 无论成功与否，都要减少等待资源的线程数量
finally {
   waiters.decrementAndGet();
}
```

sharedList 是线程安全的 CopyOnWriteArrayList，适合读多写少的场景，所以我们可以直接进行遍历。

**从共享列表shareList里偷取元素之后，是否能省略调用`listener.addBagItem(waiting - 1)`？**

不能，这会导致其他线程获取元素失败。参考下图的ThreadA，ThreadA因为从shareList获取元素失败，通知Listener往ConcurrentBag放入元素，但是外部元素刚被放入shareList就被窃取了，导致ThreadA从交接队列获取元素失败，最终导致超时。 
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401012254787.png)

## 5.2、requite

requite() 方法，用于将一个已经借用的对象归还到资源池（bag）。如果从资源池中借用的对象没有归还，将会导致内存泄漏。
```java
/**
  * 将借用的对象归还到资源池。从资源池借用但未归还的对象会导致内存泄漏。
  *
  * @param bagEntry 要归还到资源池的对象
  * @throws NullPointerException 如果 value 为空
  * @throws IllegalStateException 如果 bagEntry 不是从资源池借用的对象
  */
 public void requite(final T bagEntry)
 {
	// 1. 设置对象状态为未使用
    bagEntry.setState(STATE_NOT_IN_USE);

    // 2. 如果有等待获取资源的线程，尝试将对象放入 handoffQueue
    for (var i = 0; waiters.get() > 0; i++) 
	    // 再次判断元素状态，因为可能被其他线程抢走，如果不是未使用状态直接结束 
	    // 尝试放入交接队列，如果放入成功直接结束
       if (bagEntry.getState() != STATE_NOT_IN_USE || handoffQueue.offer(bagEntry)) {
          return;
       }
       // 根据尝试次数选择暂停线程或让出 CPU 时间片
       // 如果循环了255次，把当前线程挂起一会
       else if ((i & 0xff) == 0xff) {
          parkNanos(MICROSECONDS.toNanos(10));
       }
       else {
	      // 放弃cpu
          Thread.yield();
       }
   }
   // 3. 将对象放入线程局部存储，如果大小未超过 50
    final var threadLocalList = threadList.get();
    if (threadLocalList.size() < 50) {
       threadLocalList.add(weakThreadLocals ? new WeakReference<>(bagEntry) : bagEntry);
    }   
 }
```

当归还对象时，requite方法会首先尝试将对象放回handoffQueue，这样等待获取资源的线程可以快速获取到资源，提高了性能。同时，方法内部使用了适当的策略（如Thread.yield()和parkNanos）来平衡CPU占用和响应速度。

此外，每个线程局部存储的大小限制为50，有助于减少线程之间的资源抢占，从而提高整体性能。

## 5.3、小结

这段代码能在高并发的场景下表现得非常优异，是有不少知识点值得我们去深挖的。比如说：
- 使用 ThreadLocal 来缓存本地资源引用
- 采用读多写少的 CopyOnWriteArrayList 来缓存所有对象
- 使用基于 CAS 的 AtomicInteger 来计算等待者的数量
- 采用 compareAndSet 的 CAS 来控制状态的变更
- 在循环中使用 park、yield 等方法，避免死循环占用大量 CPU
- CAS 在设置状态时，采用了 volatile 关键字修饰
- 弱引用 WeakReference 在垃圾回收时的效率

# 6、参考链接

- [Hikari 中的 ConcurrentBag](https://mp.weixin.qq.com/s/CN9-NpyzujWZHbN2cNmkZQ)
- [Spring Boot 整合 HikariCP](https://mp.weixin.qq.com/s/9R3U4-Uzg3eaXJS20izS9A)
- [HikariCP 的原理分析](https://mp.weixin.qq.com/s/4ty3MrsymRsdz0BSB_lfyw)

# 7、总结

Hikari 作为 SpringBoot2.0 默认的连接池，在业界得到了普遍的认可，对于大部分业务场景，都可以实现快速高效的连接使用。

本篇内容针对什么是数据库连接池，如何在 Spring Boot 项目使用 HikariCP，如何在高并发场景下调整 HikariCP 的默认配置，为什么 Spring Boot 会相中 HikariCP而不是其他数据库连接池，以及通过源码来分析了 HikariCP 为什么这么快的原因，其中重点分析了 ConcurrentBag 这个类。

其实除了 ConcurrentBag，还有一个类，也就是 FastList 也非常值得说一说，但限于篇幅原因，我们就一笔带过了，其中 get() 时删除了 rangeCheck() 是因为数据库连接池满足索引的合法性，能保证不会越界，rangeCheck 属于无效的计算开销，所以不用每次都进行越界检查。

还有一个点是，remove 的时候，FastList 和 ArrayList 的做法也不同，ArrayList 在 remove 的时候采用的是顺序遍历，而 FastList则相反。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312242232203.png)

原因我们来解释一下。
```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class StatementExample {
    public static void main(String[] args) {
        Connection connection = null;
        Statement stmt1 = null;
        Statement stmt2 = null;
        Statement stmt3 = null;
        
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/db_name", "username", "password");

            // 创建Statement
            stmt1 = connection.createStatement();
            stmt2 = connection.createStatement();
            stmt3 = connection.createStatement();

            // 使用Statement执行SQL查询
            ResultSet resultSet1 = stmt1.executeQuery("SELECT * FROM table1");
            ResultSet resultSet2 = stmt2.executeQuery("SELECT * FROM table2");
            ResultSet resultSet3 = stmt3.executeQuery("SELECT * FROM table3");

            // 处理结果集
            // ...
		} catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 关闭Statement，顺序与创建顺序相反
            try {
                if (stmt3 != null) {
	                stmt3.close();
                }
                if (stmt2 != null) {
                    stmt2.close();
                }
                if (stmt1 != null) {
                    stmt1.close();
                }
                if (connection != null) {
                    connection.close();
                }
			} catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
```

在这个示例中，我们创建了3个Statement（stmt1、stmt2、stmt3）。当程序完成处理结果集后，我们需要关闭Statement（从stmt3到stmt1）。

这样做的原因是，如果stmt3依赖于stmt2的结果，关闭stmt2之前确保stmt3已经关闭，避免在处理依赖关系时出现问题。

假设一个 Connection 依次创建 6 个 Statement，分别是 S1、S2、S3、S4、S5、S6，而关闭 Statement 的顺序一般都是逆序的，从S6 到 S1。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312242236704.png)

那么 ArrayList 的 remove 就不符合要求了，因为它是按照正序遍历删除的。



参考：[HikariCP源码阅读（二）ConcurrentBag与FastList](https://juejin.cn/post/6887371883810357255)
本篇文章将深入浅出的介绍Java中的内存溢出与内存泄漏并说明强引用、软引用、弱引用、虚引用的特点与使用场景

### 引用

在栈上的`reference`类型存储的数据代表某块内存地址，称`reference`为某内存、某对象的引用

实际上引用分为很多种，从强到弱分为：**强引用 > 软引用 > 弱引用 > 虚引用**

平常我们使用的引用实际上是强引用，各种引用有自己的特点，下文将一一介绍

强引用就是Java中普通的对象，而软引用、弱引用、虚引用在JDK中定义的类分别是`SoftReference`、`WeakReference`、`PhantomReference`

下图是软引用、弱引用、虚引用、引用队列（搭配虚引用使用）之间的继承关系
![image-20230618171728398](D:\Tyora\AssociatedPicturesInTheArticles\深入浅出JVM（十四）之内存溢出、泄漏与引用\image-20230618171728398.png)

#### 内存溢出与内存泄漏

为了更清除的描述引用之间的作用，首先需要介绍一下内存溢出和内存泄漏

当发生内存溢出时，表示**JVM没有空闲内存为新对象分配空间，抛出`OutOfMemoryError(OOM)`**

当应用程序占用内存速度大于垃圾回收内存速度时就可能发生OOM

抛出OOM之前通常会进行Full GC，如果进行Full GC后依旧内存不足才抛出OOM

JVM参数`-Xms10m -Xmx10m -XX:+PrintGCDetails`
![image-20230618171829761](D:\Tyora\AssociatedPicturesInTheArticles\深入浅出JVM（十四）之内存溢出、泄漏与引用\image-20230618171829761.png)

内存溢出可能发生的两种情况：

1. 必须的资源确实很大，**堆内存设置太小** （通过`-Xmx`来调整）

1. **发生内存泄漏**，创建大量对象，且生命周期长，不能被回收

**内存泄漏Memory Leak: 对象不会被程序用到了，但是不能回收它们**

对象不再使用并且不能回收就会一直占用空间，大量对象发生内存泄漏可能发生内存溢出OOM

广义内存泄漏：不正确的操作导致对象生命周期变长

1. 单例中引用外部对象，当这个外部对象不用了，但是因为单例还引用着它导致内存泄漏
2. 一些需要关闭的资源未关闭导致内存泄漏

#### 强引用

强引用是程序代码中普遍存在的引用赋值，比如`List list = new ArrayList();`

**只要强引用在可达性分析算法中可达时，垃圾收集器就不会回收该对象，因此不当的使用强引用是造成Java内存泄漏的主要原因**

#### 软引用

当内存充足时不会回收软引用

**只有当内存不足时，发生Full GC时才将软引用进行回收，如果回收后还没充足内存则抛出OOM异常**

JVM中针对不同的区域（年轻代、老年代、元空间）有不同的GC方式，**Full GC的回收区域为整个堆和元空间**

软引用使用`SoftReference`

> 内存充足情况下的软引用

```java
public static void main(String[] args) {
         int[] list = new int[10];
         SoftReference listSoftReference = new SoftReference(list);
         //[I@61bbe9ba
         System.out.println(listSoftReference.get());
     }
```

> 内存不充足情况下的软引用(JVM参数:-Xms5m -Xmx5m -XX:+PrintGCDetails)

```java
//-Xms5m -Xmx5m -XX:+PrintGCDetails
 public class SoftReferenceTest {
     public static void main(String[] args) {
         int[] list = new int[10];
         SoftReference listSoftReference = new SoftReference(list);
         list = null;
 
         //[I@61bbe9ba
         System.out.println(listSoftReference.get());
 
         //模拟空间资源不足
         try{
             byte[] bytes = new byte[1024 * 1024 * 4];
             System.gc();
         }catch (Exception e){
             e.printStackTrace();
         }finally {
             //null
             System.out.println(listSoftReference.get());
         }
     }
 }
```

#### 弱引用

**无论内存是否足够，当发生GC时都会对弱引用进行回收**

弱引用使用`WeakReference`

> 内存充足情况下的弱引用

```java
public static void test1() {
         WeakReference<int[]> weakReference = new WeakReference<>(new int[1]);
         //[I@511d50c0
         System.out.println(weakReference.get());
 
         System.gc();
         
         //null
         System.out.println(weakReference.get());
     }
```

> WeakHashMap

JDK中有一个WeakHashMap，使用与Map相同，只不过节点为弱引用
![image-20230618171948538](D:\Tyora\AssociatedPicturesInTheArticles\深入浅出JVM（十四）之内存溢出、泄漏与引用\image-20230618171948538.png)

当key的引用不存在引用的情况下，发生GC时，WeakHashMap中该键值对就会被删除

```java
public static void test2() {
         WeakHashMap<String, String> weakHashMap = new WeakHashMap<>();
         HashMap<String, String> hashMap = new HashMap<>();
 
         String s1 = new String("3.jpg");
         String s2 = new String("4.jpg");
 
         hashMap.put(s1, "图片1");
         hashMap.put(s2, "图片2");
         weakHashMap.put(s1, "图片1");
         weakHashMap.put(s2, "图片2");
 
         //只将s1赋值为空时,堆中的3.jpg字符串还会存在强引用,所以要remove
         hashMap.remove(s1);
         s1=null;
         s2=null;
 
         System.gc();
 
         //4.jpg=图片2
         test2Iteration(hashMap);
 
         //4.jpg=图片2
         test2Iteration(weakHashMap);
     }
 
     private static void test2Iteration(Map<String, String>  map){
         Iterator iterator = map.entrySet().iterator();
         while (iterator.hasNext()){
            Map.Entry entry = (Map.Entry) iterator.next();
             System.out.println(entry);
         }
     }
```

未显示删除weakHashMap中的该key，当这个key没有其他地方引用时就删除该键值对

> 软引用，弱引用适用的场景

数据量很大占用内存过多可能造成内存溢出的场景

比如需要加载大量数据，全部加载到内存中可能造成内存溢出，就可以使用软引用、弱引用来充当缓存，当内存不足时，JVM对这些数据进行回收

使用软引用时，可以自定义Map进行存储`Map<String,SoftReference<XXX>> cache`

使用弱引用时，则可以直接使用`WeakHashMap`

软引用与弱引用的区别则是GC回收的时机不同，软引用存活可能更久，Full GC下才回收；而弱引用存活可能更短，发生GC就会回收

#### 虚引用

**使用`PhantomReference`创建虚引用，需要搭配引用队列`ReferenceQueue`使用**

**无法通过虚引用得到该对象实例**（其他引用都可以得到实例）

**虚引用只是为了能在这个对象被收集器回收时收到一个通知**

> 引用队列搭配虚引用使用

```java
public class PhantomReferenceTest {
     private static PhantomReferenceTest reference;
     private static ReferenceQueue queue;
 
     @Override
     protected void finalize() throws Throwable {
         super.finalize();
         System.out.println("调用finalize方法");
         //搭上引用链
         reference = this;
     }
 
     public static void main(String[] args) {
         reference = new PhantomReferenceTest();
         //引用队列
         queue = new ReferenceQueue<>();
         //虚引用
         PhantomReference<PhantomReferenceTest> phantomReference = new PhantomReference<>(reference, queue);
         
         Thread thread = new Thread(() -> {
             PhantomReference<PhantomReferenceTest> r = null;
             while (true) {
                 if (queue != null) {
                     r = (PhantomReference<PhantomReferenceTest>) queue.poll();
                     //说明被回收了,得到通知
                     if (r != null) {
                         System.out.println("实例被回收");
                     }
                 }
             }
         });
         thread.setDaemon(true);
         thread.start();
 
 
         //null (获取不到虚引用)
         System.out.println(phantomReference.get());
 
         try {
             System.out.println("第一次gc 对象可以复活");
             reference = null;
             //第一次GC 引用不可达 守护线程执行finalize方法 重新变为可达对象
             System.gc();
             TimeUnit.SECONDS.sleep(1);
             if (reference == null) {
                 System.out.println("object is dead");
             } else {
                 System.out.println("object is alive");
             }
             reference = null;
             System.out.println("第二次gc 对象死了");
             //第二次GC 不会执行finalize方法 不能再变为可达对象
             System.gc();
             TimeUnit.SECONDS.sleep(1);
             if (reference == null) {
                 System.out.println("object is dead");
             } else {
                 System.out.println("object is alive");
             }
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
 
     }
 }
```

结果:

```python
/*
 null
 第一次gc 对象可以复活
 调用finalize方法
 object is alive
 第二次gc 对象死了
 实例被回收
 object is dead
 */
```

第一次GC时，守护线程执行finalize方法让虚引用重新可达，所以没死

第二次GC时，不再执行finalize方法，虚引用已死

虚引用回收后，引用队列有数据，来通知告诉我们reference这个对象被回收了

> 使用场景

GC只能回收堆内内存，而直接内存GC是无法回收的，直接内存代表的对象创建一个虚引用，加入引用队列，当这个直接内存不使用，这个代表直接内存的对象为空时，这个虚内存就死了，然后引用队列会产生通知，就可以通知JVM去回收堆外内存（直接内存）

### 总结

本篇文章围绕引用深入浅出的解析内存溢出与泄漏、强引用、软引用、弱引用、虚引用

**当JVM没有足够的内存为新对象分配空间时就会发生内存溢出抛出OOM**

**内存溢出有两种情况，一种是分配的资源太少，不满足必要对象的内存；另一种是发生内存泄漏，不合理的设置对象的生命周期、不关闭资源都会导致内存泄漏**

**使用最常见的就是强引用，强引用只有在可达性分析算法中不可达时才会回收，强引用使用不当是造成内存泄漏的原因之一**

**使用`SoftReference`软引用时，只要内存不足触发Full GC时就会对软引用进行回收**

**使用`WeakReference`弱引用时，只要发生GC就会对弱引用进行回收**

**软、弱引用可以用来充当大数据情况下的缓存，它们的区别就是软引用可能活的更久Full GC才回收，使用弱引用时可以直接使用JDK中提供的WeakHashMap**

**虚引用无法在程序中获取，与引用队列搭配使用，当虚引用被回收时，能够从引用队列中取出（感知），可以在直接引用不使用时，发出消息让JVM进行回收**

### 最后

- 参考资料
  - 《深入理解Java虚拟机》

本篇文章将被收入JVM专栏，觉得不错感兴趣的同学可以收藏专栏哟~

觉得菜菜写的不错，可以点赞、关注支持哟~

有什么问题可以在评论区交流喔~



作者：菜菜的后端私房菜
链接：https://juejin.cn/post/7181615888976576567
来源：稀土掘金
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
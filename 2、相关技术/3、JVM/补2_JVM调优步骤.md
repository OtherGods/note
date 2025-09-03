从面经中整理的，面经来源已不可考察，不过我感觉应该没问题。

调优目标：

1.  **GC低停顿；**
2.  GC低频率；
3.  低内存占用；
4.  **高吞吐量；**
5.  **降低响应时间**

调优步骤：

1. 在程序运行之前可以使用以下步骤进行调优：

- 熟悉业务场景，选定垃圾回收器（没有最好的垃圾回收器，只有最合适的垃圾回收器）
  1. 响应时间、停顿时间 [CMS G1 ZGC] （需要给用户作响应），实现地停顿的垃圾回收器有：CMS、G1
  2. 吞吐量 = 用户时间 /( 用户时间 + GC时间) [PS]，实现高吞吐量的垃圾回收器有：Parallel Scavenge、Parallel Old
  3. ![image-20221126205426987](D:\Tyora\AssociatedPicturesInTheArticles\JVM调优步骤\image-20221126205426987.png)
- 计算内存需求（设置内存大小 1.5G 16G）
  - 设置堆：
    - -Xms1024k：设置堆的最小值（初始化堆大小），一开始使用的内存大小，如果超出该大小就会自动扩容；相当于-XX:InitialHeap1024k【可以使用-Xmn或-XX:New1024k设置年轻代的堆的初始大小】。
    - -Xmx1024k：设置堆的最大值，初始化系统就会分配，虽然一开始使用的是初始堆大小，但是可以根据情况动态扩容，但是不能超过最大堆大小，否则会抛出outOfMemoryError；相当于-XX:MaxHeap1024k
      【可以使用-XX:MaxNew1024k设置年轻代的最大值】
  - 设置栈：
    - -Xss1024k:设置每个线程的栈大小；相当于-XX:ThreadStack1024k
  - 设置元空间：
    - -XX:MetaspaceSize=1024k
    - -XX:MaxMetaspaceSize=1024k 元空间最大值（注意这里是堆外内存，他是不计入堆大小内存中的）；
- 选定CPU：越高越好

2. 在程序运行的时候可以使用以下工具进行调优：
   1. jcmd：查看线程堆栈，涵盖所有功能（对GC跟踪缺失）：
      ![image-20221123182818435](D:\Tyora\AssociatedPicturesInTheArticles\JVM调优步骤\image-20221123182818435.png)
   2. jstat：对GC有很轻大的跟踪监视能力，支持远程调用；可以显示Java虚拟机中的类加载、内存、垃圾收集、即时编译等运行状态的信息。
   3. jstack：Java堆栈跟踪工具，打印指定的Java进程/线程的堆栈跟踪信息（与jmap、jcmd、jinfo选择一个）
      ![image-20221123183001869](D:\Tyora\AssociatedPicturesInTheArticles\JVM调优步骤\image-20221123183001869.png)

3. 在程序运行之后可以使用以下步骤进行调优：
   1. 分析GC日志及dump文件，判断是否需要优化，确定瓶颈问题点；
      ![image-20221126205817030](D:\Tyora\AssociatedPicturesInTheArticles\JVM调优步骤\image-20221126205817030.png)


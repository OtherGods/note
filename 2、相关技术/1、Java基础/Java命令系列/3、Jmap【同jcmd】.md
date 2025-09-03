Jmap

> jmap是JDK自带的工具软件，主要用于打印指定Java进程(或核心文件、远程调试服务器)的共享对象内存映射或堆内存细节。可以使用jmap生成Heap Dump。在 [3.1、常见命令及Java Dump介绍](2、相关技术/1、Java基础/Java命令系列/3.1、常见命令及Java%20Dump介绍.md) 和 [2、Jstack【同jcmd】](2、相关技术/1、Java基础/Java命令系列/2、Jstack【同jcmd】.md) 中分别有关于Java Dump以及线程 Dump的介绍。 **这篇文章主要介绍Java的堆Dump以及jamp命令**

### 什么是堆Dump

堆Dump是反应Java堆使用情况的内存镜像，其中主要包括**系统信息**、**虚拟机属性**、**完整的线程Dump**、**所有类和对象的状态**等。 一般，在内存不足、GC异常等情况下，我们就会怀疑有 [内存泄漏](2、相关技术/3、JVM/Hollis/41、内存泄漏和内存溢出的区别是什么？.md)。这个时候我们就可以制作堆Dump来查看具体情况。分析原因。

### 基础知识

[Java虚拟机的内存组成以及堆内存介绍](http://www.hollischuang.com/archives/80) 、[Java GC工作原理](http://www.hollischuang.com/archives/76) 常见内存错误：

> outOfMemoryError **年老代内存不足。**
> outOfMemoryError:PermGen Space **永久代内存不足。**
> outOfMemoryError:GC overhead limit exceed **垃圾回收时间占用系统运行时间的98%或以上。**

### 语法

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508052252609.png)

> **指定进程号(pid)的进程** `jmap [ option ] <pid>` 
> **指定核心文件** `jmap [option] <executable <core>` 
> **指定远程调试服务器** `jmap [option] [server_id@]<remote server IP or hostname>`

> **参数：**
>
> > **option** 选项参数是互斥的(不可同时使用)。想要使用选项参数，直接跟在命令名称后即可。
> > **pid** 需要打印配置信息的进程ID。该进程必须是一个Java进程。想要获取运行的Java进程列表，你可以使用jps。
> > **executable** 产生核心dump的Java可执行文件。
> > **core** 需要打印配置信息的核心文件。
> > **remote-hostname-or-IP** 远程调试服务器的(请查看jsadebugd)主机名或IP地址。
> > **server-id** 可选的唯一id，如果相同的远程主机上运行了多台调试服务器，用此选项参数标识服务器。
>
> **选项:**
>
> > `<no option>` 如果使用不带选项参数的jmap打印共享对象映射，将会打印目标虚拟机中加载的每个共享对象的起始地址、映射大小以及共享对象文件的路径全称。这与Solaris的pmap工具比较相似。
> > `-dump:[live,]format=b,file=<filename>` 以hprof二进制格式转储Java堆到指定`filename`的文件中。live子选项是可选的。如果指定了live子选项，堆中只有活动的对象会被转储。想要浏览heap dump，你可以使用~~jhat~~、jProfile分析展示。
> > `-finalizerinfo` 打印等待终结的对象信息。
> > `-heap` 打印一个堆的摘要信息，包括使用的GC算法、堆配置信息和generation wise heap usage。
> > `-histo[:live]` 打印堆的柱状图。其中包括每个Java类、对象数量、内存大小(单位：字节)、完全限定的类名。打印的虚拟机内部的类名称将会带有一个`'*'`前缀。如果指定了live子选项，则只计算活动的对象。
> > `-permstat` 打印Java堆内存的永久保存区域的类加载器的智能统计信息。对于每个类加载器而言，它的名称、活跃度、地址、父类加载器、它所加载的类的数量和大小都会被打印。此外，包含的字符串数量和大小也会被打印。
> > `-F` 强制模式。如果指定的pid没有响应，请使用jmap -dump或jmap -histo选项。此模式下，不支持live子选项。
> > `-h` 打印帮助信息。
> > `-help` 打印帮助信息。
> > `-J<flag>` 指定传递给运行jmap的JVM的参数。

### 示例

#### `jmap -heap pid`

对照：[GC.heap_info(替代jmap)](2、相关技术/1、Java基础/Java命令系列/0、jcmd命令.md#GC.heap_info(替代jmap))

**查看java 堆（heap）使用情况,** 执行命令： `jmap -heap 31846`
```shell
Attaching to process ID 31846, please wait...
Debugger attached successfully.
Server compiler detected.
JVM version is 24.71-b01

using thread-local object allocation.
Parallel GC with 4 thread(s)//GC 方式

Heap Configuration: //堆内存初始化配置
   MinHeapFreeRatio = 0 //对应jvm启动参数-XX:MinHeapFreeRatio设置JVM堆最小空闲比率(default 40)
   MaxHeapFreeRatio = 100 //对应jvm启动参数 -XX:MaxHeapFreeRatio设置JVM堆最大空闲比率(default 70)
   MaxHeapSize      = 2082471936 (1986.0MB) //对应jvm启动参数-XX:MaxHeapSize=设置JVM堆的最大大小
   NewSize          = 1310720 (1.25MB)//对应jvm启动参数-XX:NewSize=设置JVM堆的‘新生代’的默认大小
   MaxNewSize       = 17592186044415 MB//对应jvm启动参数-XX:MaxNewSize=设置JVM堆的‘新生代’的最大大小
   OldSize          = 5439488 (5.1875MB)//对应jvm启动参数-XX:OldSize=<value>:设置JVM堆的‘老生代’的大小
   NewRatio         = 2 //对应jvm启动参数-XX:NewRatio=:‘新生代’和‘老生代’的大小比率
   SurvivorRatio    = 8 //对应jvm启动参数-XX:SurvivorRatio=设置年轻代中Eden区与Survivor区的大小比值 
   PermSize         = 21757952 (20.75MB)  //对应jvm启动参数-XX:PermSize=<value>:设置JVM堆的‘永生代’的初始大小
   MaxPermSize      = 85983232 (82.0MB)//对应jvm启动参数-XX:MaxPermSize=<value>:设置JVM堆的‘永生代’的最大大小
   G1HeapRegionSize = 0 (0.0MB)

Heap Usage://堆内存使用情况
PS Young Generation
Eden Space://Eden区内存分布
   capacity = 33030144 (31.5MB)//Eden区总容量
   used     = 1524040 (1.4534378051757812MB)  //Eden区已使用
   free     = 31506104 (30.04656219482422MB)  //Eden区剩余容量
   4.614088270399305% used //Eden区使用比率
From Space:  //其中一个Survivor区的内存分布
   capacity = 5242880 (5.0MB)
   used     = 0 (0.0MB)
   free     = 5242880 (5.0MB)
   0.0% used
To Space:  //另一个Survivor区的内存分布
   capacity = 5242880 (5.0MB)
   used     = 0 (0.0MB)
   free     = 5242880 (5.0MB)
   0.0% used
PS Old Generation //当前的Old区内存分布
   capacity = 86507520 (82.5MB)
   used     = 0 (0.0MB)
   free     = 86507520 (82.5MB)
   0.0% used
PS Perm Generation//当前的 “永生代” 内存分布
   capacity = 22020096 (21.0MB)
   used     = 2496528 (2.3808746337890625MB)
   free     = 19523568 (18.619125366210938MB)
   11.337498256138392% used

670 interned Strings occupying 43720 bytes.
```

`Heap Configuration`：堆内存初始化值，可以由JVM参数调整
1. `MinHeapFreeRatio`：指定jvm heap在使用率小于 n 的情况下,heap 进行收缩，`Xmx==Xms` 的情况下无效，如:`-XX:MinHeapFreeRatio=30`，通常我们设置`Xms==Xmx`所以该值为0
2. `MaxHeapFreeRatio`：指定  jvm heap 在使用率大于  n 的情况下，heap 进行扩张，`Xmx==Xms`的情况下无效，如:`-XX:MaxHeapFreeRatio=70`，通常我们设置`Xms==Xmx`所以该值为100
3. `NewRatio`：老年代与新生代的比例
4. `SurvivorRatio`：Eden区与一个`s0/s1`的比例，但是心神大概中Eden、s0、s1的容量不一定按照这个比例，可能与TLAB有关

#### `jmap –histo pid`

对照：[GC.class_histogram(替代jmap)](2、相关技术/1、Java基础/Java命令系列/0、jcmd命令.md#GC.class_histogram(替代jmap))

**查看堆内存(histogram)中的对象数量及大小**。执行命令： `jmap -histo 3331`

```
num     #instances         #bytes  class name
编号     个数                字节     类名
----------------------------------------------
   1:             7        1322080  [I
   2:          5603         722368  <methodKlass>
   3:          5603         641944  <constMethodKlass>
   4:         34022         544352  java.lang.Integer
   5:           371         437208  <constantPoolKlass>
   6:           336         270624  <constantPoolCacheKlass>
   7:           371         253816  <instanceKlassKlass>
```

> **jmap -histo:live 这个命令执行，JVM会先触发gc，然后再统计信息。**

#### `jmap –finalizerinfo pid`

- 查找在F-Queue队列等待Finalizer线程执行finalizer方法的对象有多少
- Number of objects pending for finalization: 0 说明当前F-QUEUE队列中并没有等待Fializer线程执行final
	- 这个值越大，代表越多的对象没有被回收

#### `jmap -dump[live,]:format=b,file=heapDump pid`

对照：[GC.heap_dump(替代jmap)](2、相关技术/1、Java基础/Java命令系列/0、jcmd命令.md#GC.heap_dump(替代jmap))

- 以hprof二进制格式转储Java堆到指定filename的文件中。live子选项是可选的。如果指定了live子选项，堆中只有活动的对象会被转储。想要浏览heap dump，你可以使用jhat(Java堆分析工具)读取生成的文件
- 执行的过程中为了保证dump的信息是可靠的，会暂停应用，生产系统不推荐使用。

执行命令： `jmap -dump:format=b,file=heapDump 6900`
> 等价于：在发生内存溢出时转储堆快照文件、指定快照文件的存储位置
> `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/logs/heap_dump.hprof`

~~然后用`jhat`命令可以参看 `jhat -port 5000 heapDump` 在浏览器中访问：`http://localhost:5000/` 查看详细信息【鸡肋】~~

### 总结

1.如果程序内存不足或者频繁GC，很有可能存在内存泄露情况，这时候就要借助Java堆Dump查看对象的情况。
2.要制作堆Dump可以直接使用jvm自带的jmap命令
3.可以先使用`jmap -heap pid`命令查看堆的使用情况，看一下各个堆空间的占用情况。
4.使用`jmap -histo:[live]  pid`查看堆内存中的对象的情况。如果有大量对象在持续被引用，并没有被释放掉，那就产生了内存泄露，就要结合代码，把不用的对象释放掉。
5.也可以使用 `jmap -dump:format=b,file=<fileName> pid`命令将堆信息保存到一个文件中，再借助jhat命令查看详细内容
6.在内存出现泄露、溢出或者其它前提条件下，建议多dump几次内存，把内存文件进行编号归档，便于后续内存整理分析。

### 异常

`Error attaching to process: sun.jvm.hotspot.debugger.DebuggerException: Can't attach to the process`

在ubuntu中第一次使用jmap会报错：`Error attaching to process: sun.jvm.hotspot.debugger.DebuggerException: Can't attach to the process`，这是oracla文档中提到的一个bug:http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7050524,解决方式如下：

> 1. echo 0 | sudo tee /proc/sys/kernel/yama/ptrace_scope 该方法在下次重启前有效。
> 2. 永久有效方法 sudo vi /etc/sysctl.d/10-ptrace.conf 编辑下面这行: kernel.yama.ptrace_scope = 1 修改为: kernel.yama.ptrace_scope = 0 重启系统，使修改生效。


转载自：https://www.hollischuang.com/archives/303
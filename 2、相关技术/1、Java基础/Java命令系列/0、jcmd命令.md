jcmd与其他命令对比整理
**可视化工具**通常用来实时监控：内存（堆、原空间）、线程、cpu、类、JVM参数、系统属性等信息；
除了以上常用功能，额外的功能：
- **==jconsole==**：执行GC、**MBean**（可以查看和操作已注册的MBean，具体代码看项目`mima-jvm-server`中接口`/demo/mbean`，curl调用该接口后jconsole上就能看到效果）
- **==jvisualvm==**：执行GC、**堆Dump**、**线程状态可视化**、**线程Dump**、抽样统计某包下对CPU、内存的占用情况
- **==jmc==**：**MBean**、**触发器**（监控系统资源，达到阈值后发出通知或记录）、执行jcmd命令、**飞行记录器**

| 工具                        | 说明                                                         | 重要程度            |
| ------------------------- | ---------------------------------------------------------- | --------------- |
| jcmd                      | 最全面，**==基本涵盖所有功能==**，但是对于gc的跟踪缺失，通常 **==使用jstat配合看GC日志==** | 强大              |
| jconsole                  | 配合JMX进行 **==监控==**                                         | **可视化工具**       |
| jvisualvm                 | 配合JMX或JSTATD服务 **==监控==**                                  | **可视化工具**       |
| jmc                       | 配合JMX **==监控==** 及 **==预警==**（触发器发邮件记日志等）、**==配合JFR做分析==** | **可视化（强）**      |
| [jstat](4、jstat【重要】)      | 对于 **==gc有强大的跟踪监视能力==**,配合**jsadebugd支持远程调用**              | 配合jcmd**看GC日志** |
| [jinfo](5、jinfo【同jcmd】)   | 查看jvm信息、**==动态修改flag参数==**,配合**jsadebugd支持远程调用**           | 被jcmd覆盖         |
| [jstack](2、Jstack【同jcmd】) | 打印线程堆栈、可以检测死锁（jcmd覆盖）配合**jsadebugd支持远程调用**                 | 被jcmd覆盖         |
| [jmap](3、Jmap【同jcmd】)     | 打印堆、实例、导出dump（jcmd覆盖）配合**jsadebugd支持远程调用**                 | 被jcmd覆盖         |
| jps                       | 查看本机和远程的jvm进行,配合jstatd支持远程调用                               | 一般              |
| jstatd                    | 开启服务端jstat监听                                               | 一般              |
| jhat                      | 解析Heap dump快照，使用http服务器方式展示                                | 鸡肋              |

<font color="red" size=5>！！！注</font>：<font color="blue" size=5>这些工具的设计目标是对整个JVM进程进行操作</font>，而不是针对单个线程，所以<font color="blue" size=5>这些命令的参数中的pid值得是进程ID</font>

# 功能

导出堆、查看Java进程、导出线程信息、执行GC、还可以进行采样分析。【功能齐全】

## `jcmd –h` 查看帮助信息

```shell
jcmd <pid | main class> <command ... | PerfCounter.print | -f  file>
```
- pid：接收诊断命令请求的进程ID。
- main class ：接收诊断命令请求的进程的main类。匹配进程时，main类名称中包含指定子字符串的任何进程均是匹配的。 
  如果多个正在运行的Java进程共享同一个main类，诊断命令请求将会发送到所有的这些进程中。

- command：命令
- Perfcounter.print：打印目标Java进程上可用的性能计数器。性能计数器的列表可能会随着Java进程的不同而产生变化。
- `-f file`：从文件file中读取命令，然后在目标Java进程上调用这些命令。在file中，每个命令必须写在单独的一行。以“#”开头的行会被忽略。当所有行的命令被调用完毕后，或者读取到含有stop关键字的命令，将会终止对file的处理。(使用较少)
- `-l`：查看所有的进程列表信息。
	- 等价于 `jcmd`、`jps`【jpd可以看用`IP+端口`查看远程服务器的进程信息，但不推荐使用】
- `-h`：查看帮助信息。（同 -help）

## `jcmd pid command`

### `jcmd pid help` 查看进程可执行的命令

```shell
[root@dev-yd-jyh01 /]# jcmd 6835 help
6835:
The following commands are available:
JFR.stop
JFR.start
JFR.dump
JFR.check
VM.native_memory
VM.check_commercial_features 
VM.unlock_commercial_features
ManagementAgent.stop
ManagementAgent.start_local
ManagementAgent.start 
GC.rotate_log 
Thread.print
GC.class_stats
GC.class_histogram 
GC.heap_dump 
GC.run_finalization 
GC.run
VM.uptime
VM.flags
VM.system_properties 
VM.command_line 
VM.version
help
```

### `jcmd pid help 具体命令` 查看命令详细信息

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508042112168.png)

### jcmd 与虚拟机相关参数

#### VM.uptime

`jcmd 6835 VM.uptime -date=true`：6835启动时长

#### VM.flags（替代jinfo）
VM的标志选项和他们的当前值，使用jinfo改变JVM参数后也可以看到修改后的值；
等价于
- [5、jinfo【同jcmd】](2、相关技术/1、Java基础/Java命令系列/5、jinfo【同jcmd】.md)

`jcmd 6835 VM.flags`：打印出**进程6835的jvm启动的时候设置的参数**
`jcmd 6835 VM.flags –all`：打印**JVM所有支持的参数**

#### VM.system_properties（替代jinfo）
系统属性

`jcmd 6835 VM.system_properties `：打印虚拟机的系统属性

当程序在本机运行和生产环境运行的时候程序运行效果不一样或者出现一些问题的时候，需要去对比本机和环境和开发环境有哪些区别的时候用到这个命令下的参数，或者想了解系统属性有那些值的时候可以用这个命令下的参数

#### VM.command_line
VM实例启动时使用的命令行

`jcmd 6835 VM.command_line`：打印**JVM实例启动时使用的参数**

有点像jps

#### VM.version
版本信息

`jcmd 6835 VM.version`：输出VM版本

#### VM.native_memory

可以用来<font color="red" size=5>查看本地内存分配情况</font>

##### 语法

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508042136410.png)

scale：刻度尺，内存占用情况不够1刻度的，不在结果中显示，或者显示为0

##### Native Memory Tracking (NMT)

Java8的HotSpot VM引入了`Native Memory Tracking (NMT)`特性，可以<font color="red" size=5>用于追踪JVM的内部内存</font>使用。
**==启用此功能将导致5-10%的开销==**

NMT默认是关闭的，需要使用 `-XX:NativeMemoryTracking=[off|summary|detail]` 参数去开启，默认是off状态：
- summary：仅收集系统聚合的内存使用情况
- detail：收集各个调用站点的内存使用情况（额外开销比summary高）
- off：关闭

- `jcmd pid VM.native_memory summary scale=MB` 显示**摘要**：打印按照类别汇总的摘要，在显示的时候以MB为单位，不够1个刻度（单位）就不会显示在jcmd的结果中
- `jcmd pid VM.native_memory detail scale=MB` 显示**明细**: 按类别聚合的打印内存使用情况,打印虚拟内存映射,按调用站点聚合的打印内存使用情况
- `jcmd pid VM.native_memory baseline` **创建基线(内存使用快照)用于比较**
- `jcmd pid VM.native_memory summary.diff scale=MB` **显示摘要差异,与基线比较**
- `jcmd pid VM.native_memory detail.diff scale=MB` **显示明细差异，与基线比较**
- `jcmd pid VM.native_memory shutdown` **关闭NMT, 关闭后不可再跟踪(不能再使用上面的命令),必须重启JVM进程（应用程序，如pid=15372的进程）**

##### 配置NMT参数启动项目

JVM参数配置 `-XX:NativeMemoryTracking=summary` 后启动项目`mima-jvm-server`，该项目对应pdi为15372。

执行命令`jcmd 15372 VM.native_memory`（效果等价于`jcmd 15372 VM.native_memory summary`）结果：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508042212880.png)

- **reserved（保留内存）**：指JVM向操作系统申请保留的地址空间总量。这部分内存不一定全部被使用，但JVM保留了这个空间，防止其他进程使用。
- **committed（提交内存）**：指JVM实际已经使用的物理内存或交换空间（swap）的大小。这是实际占用的内存。

### jcmd 线程相关参数(替代jstack)

等价于[2、Jstack【同jcmd】](2、相关技术/1、Java基础/Java命令系列/2、Jstack【同jcmd】.md)

#### 语法

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508042251987.png)

只能打印JUC包下的类引起的锁

`jcmd 6835 Thread.print`

示例：
[04JAVA虚拟机-jcmd Thread.print 解析](2、相关技术/3、JVM/带你学习jvmjava虚拟机arthas_性能调优_故障排除_gc回收_内存溢出等/KEVIN授权学员专属资料-JVM课程51CTO-01/外发资料/04JAVA虚拟机-jcmd%20Thread.print%20解析.pdf)

参考对比：[2、Jstack【同jcmd】](2、相关技术/1、Java基础/Java命令系列/2、Jstack【同jcmd】.md)

### jcmd GC相关参数

#### GC.run 
执行System.gc()
#### GC.run_finalization
执行finalize方法
#### GC.class_stats
类的元数据统计信息，平时不使用这个命令，使用 `GC.class_histogram`
执行System.runFinalization()
#### GC.class_histogram(替代jmap)
打印java堆的使用统计
与 `GC.class_stats` 类似，平常使用这个
等价于[3、Jmap【同jcmd】](2、相关技术/1、Java基础/Java命令系列/3、Jmap【同jcmd】.md) 中`jmap –histo pid`

#### GC.heap_info(替代jmap)
堆信息
等价于[3、Jmap【同jcmd】](2、相关技术/1、Java基础/Java命令系列/3、Jmap【同jcmd】.md) 中`jmap -heap`区别是jcmd展示的是比例，jmap展示的是容量 ，并且会打印出JVM参数配置等信息
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508052247332.png)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508052248163.png)

#### GC.heap_dump(替代jmap)

手动转储堆快照文件
等价于[3、Jmap【同jcmd】](2、相关技术/1、Java基础/Java命令系列/3、Jmap【同jcmd】.md) 中
```shell
jmap -dump[live,]:format=b,file=heapDump pid
```

常用命令：
```shell
#导出所有对象,指定路径和文件名
jcmd pid GC.heap_dump -all /data/heap_dump_all.hprof
```

通常使用JVM参数在发生内存溢出时转储堆快照文件、指定快照文件的存储位置：
```shell
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/logs/heap_dump.hprof
```

快照文件可以使用Memory Analyzer Tool (MAT)分析、也可以使用JProfiler分析

### jcmd ManagementAgent参数

ManagementAgent参数其实是JMX（Java Management Extension）的参数

JMX是管理系统和资源之间的一个接口，它定义了<font color="red" size=5>管理系统和资源之间交互的标准</font>；用于监控应用程序的各个方方面面，CPU、内存、线程、栈等，可以使用jconsole、jvisualvm、jmc等进行可视化监控

jcmd只是提供了一个开启和关闭的命令，核心还在JMX命令

#### ManagementAgent.start_local

启动本地JMX代理
`jcmd pid ManagementAgent.start_local`

#### ManagementAgent.stop

关闭JMX的远程代理
`jcmd pid ManagementAgent.stop`

#### ManagementAgent.start

启动远程代理，在CPU过高的情况下可能启动失败，可以在应用程序启动时指定JVM参数，如下：
```shell
java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=7777 -Dcom.sun.management.jmxremote.rmi.port=7777 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false ... [其他参数] -jar your-application.jar
```

##### 语法

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508051539527.png)

使用示例：
`jcmd pid ManagementAgent.start jmxremote.port=7777 jmxremote.rmi.port=7777 jmxremote.ssl=false jmxremote.authenticate=false`

指定本地和远程JMX端口7777，ip为远程主机IP地址，不使用ssl连接，不开启密码认证，之后使用jconsole命令连接JMX，可以选择本地进程，也可以在远程进程里输入`127.0.0.1:7777`【通过这种方式连接的好处是：应用程序重启后只要jxm这几个参数没变（ip + port + ……）只要点重新连接就能连上而不需要重新打开窗口】
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508051547991.png)

远程连接失败可能的问题：
1. 关闭防火墙（CentOS为例）：firewall-cmd –state 查看防火墙状态、`systemctl stop firewalld.service` 停止
2. 修改hosts文件为真实IP（vim /etc/hosts），使用hostname –i 查看是否为真实IP，而不是127.0.0.1这种
	- `vim /etc/hosts` 
		- 重启java应用(因启动时已经加载了这个值)，否则hosts修改了也没用
	- 或使用JVM参数：`-Djava.rmi.server.hostname=真实IP`

### jcmd jfr

Java Flight Recorder（jfr）记录一段时间内JVM的发生的所有事，类似于飞机上的黑匣子，深入分析JVM的工具

可以由jcmd启动，主要结合JMC使用；
也可以直接在JMC连接到pid服务后双击飞行记录器启动,而无需再用jcmd配合下面的命令启动；

#### JFR.start
启动一个新的JFR录制

语法：用默认值就可以
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508051812213.png)

示例命令：
`jcmd pid JFR.start`

```shell
-- 启动jfr
D:\Java\jdk\bin>jcmd 16936 JFR.start
16936:
Java Flight Recorder not enabled.

Use VM.unlock_commercial_features to enable.

-- 需要先启用商用功能
D:\Java\jdk\bin>jcmd 16936 VM.unlock_commercial_features
16936:
Commercial Features now unlocked.

-- 正常启动，启动后会输出【recording 1】用于stop时指定
D:\Java\jdk\bin>jcmd 16936 JFR.start
16936:
Started recording 1. No limit (duration/maxsize/maxage) in use.

Use JFR.dump recording=1 filename=FILEPATH to copy recording data to file.
```

#### JFR.stop
停止一个JFR录制
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508051813498.png)

示例命令：
`jcmd pid JFR.stop recording=1 filename=d:\temp\stop.jfr`

#### JFR.check 
检查正在运行的JFR录制
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508051816335.png)

#### JFR.dump
将JFR录制内容转出为文件
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508051817883.png)

示例命令：
`jcmd pid JFR.dump recording=1 filename=d:\temp\12.jfr`


## MBean

### 定义

**MBean（Managed Bean）是 Java 管理扩展（JMX）技术的核心概念。** 你可以把它理解为一个**代表可管理资源（如应用程序组件、服务或 JVM 内部实体）的 Java 对象**，它通过一个**标准化的接口**向外部管理工具（如 JConsole, JMC, VisualVM 或自定义管理控制台）暴露该资源的关键信息和操作。像 **==`java.lang.MemoryImpl`就是一个MBean可以操作`gc`方法==**。

**通俗地说，MBean 就是：**
1.  **一个“遥控器”或“仪表盘”**：它为外部管理工具提供了一种标准化的方式来“查看”（读取属性）、“调整”（写入属性）和“操作”（调用方法）它所代表的那个资源（比如你的应用服务器、数据库连接池、缓存模块、甚至是 JVM 的垃圾回收器）。
2.  **一个“标准化接口”**：无论底层资源的具体实现多么复杂，MBean 都通过一组预定义的规则（属性、操作、通知）来描述如何管理它。这使得管理工具可以用统一的方式与各种不同的资源交互。
3.  **一个“注册在中央目录（MBean Server）里的组件”**：创建好的 MBean 需要注册到一个叫做 `MBeanServer` 的中心注册表中。管理工具通过连接到这个 `MBeanServer` 来发现和操作所有已注册的 MBean。

**MBean 的核心作用：暴露管理接口**
一个 MBean 主要通过以下三种方式定义其管理接口：
1.  **属性：**
    *   代表资源的状态或配置信息。
    *   可以被**读取**（`getAttribute`）以获取当前值（例如：当前活动线程数、内存使用量、日志级别）。
    *   有些属性还可以被**写入**（`setAttribute`）以改变资源的状态或配置（例如：设置新的日志级别、调整连接池的最大连接数）。
    *   类似于 JavaBean 的 getter/setter 方法。
2.  **操作：**
    *   代表可以在资源上执行的动作或命令。
    *   可以被管理工具**调用**（`invoke`）。
    *   可以接受参数，也可以有返回值。
    *   例如：手动触发垃圾回收 (`gc()`)、获取线程转储 (`dumpAllThreads(...)`)、生成堆转储 (`dumpHeap(...)`)、重置计数器、重新加载配置等。
3.  **通知：**
    *   允许 MBean **主动发送事件**给已注册的监听器（如管理工具）。
    *   用于在资源发生特定事件时（如错误、状态变更、达到阈值）**异步通知**管理者。
    *   例如：当内存使用超过 90% 时发送警报通知、当检测到死锁时发送通知、当有新的客户端连接时发送通知。

**MBean 的类型：**
JMX 规范定义了几种不同类型的 MBean，主要是为了提供不同的编程模型来创建它们：
1.  **标准 MBean：**
    *   最简单、最常用的类型。
    *   管理接口通过一个与 MBean 实现类同名的 Java `接口` 来明确定义（例如，实现类 `MyService` 对应的接口必须是 `MyServiceMBean`）。
    *   接口中定义了所有可暴露的属性（getter/setter）和操作（方法）。
2.  **动态 MBean：**
    *   管理接口在运行时动态定义（而不是通过静态接口）。
    *   MBean 类实现 `javax.management.DynamicMBean` 接口，并负责在运行时提供其属性、操作和通知的描述信息。
    *   更灵活，但实现也更复杂。通常用于需要动态改变接口或包装已有非 MBean 对象的场景。
3.  **开放 MBean：**
    *   一种特殊的动态 MBean。
    *   使用一组预定义的基本数据类型（如 `OpenType` 及其子类 `SimpleType`, `CompositeType`, `TabularType`），旨在提供**更通用的、工具友好的**管理接口。
    *   目标是让管理工具即使没有 MBean 的特定类信息，也能理解并展示其数据（因为使用的是标准类型）。
4.  **模型 MBean：**
    *   也是动态 MBean 的一种。
    *   实现类通常是 `javax.management.modelmbean.RequiredModelMBean`。
    *   管理接口（属性、操作、通知等）通过元数据（`ModelMBeanInfo` 对象）在运行时配置。
    *   提供了很大的灵活性，常用于框架或容器中动态管理资源。
5.  **MXBean：**
    *   **目前最推荐和广泛使用的类型**（尤其是在 JDK 标准库和现代框架中）。
    *   本质上是一种特殊的“标准 MBean”。
    *   关键优势在于**类型兼容性**。MXBean 接口中使用的方法参数和返回类型被限制在一组预定义的“开放类型”（如基本类型、String、`CompositeData` 等）。这使得不同版本的客户端（管理工具）即使没有 MBean 接口的精确类定义，也能与之交互，解决了标准 MBean 的类加载器依赖问题。
    *   接口命名规则是 `SomethingMXBean`（例如：`MemoryMXBean`, `ThreadMXBean`）。
    *   JDK 提供的 JVM 监控 MBean 几乎都是 MXBean。

**MBean 的生命周期：**
1.  **创建**：开发者编写一个符合 MBean 类型规则（如实现接口或继承特定类）的 Java 类。
2.  **注册**：在应用程序启动时（或资源初始化时），通过代码（通常是 `ManagementFactory.getPlatformMBeanServer().registerMBean(...)`）将这个 MBean 对象实例注册到 `MBeanServer` 中，并赋予一个唯一的**对象名称**（`ObjectName`）。对象名称通常遵循 `domain:key=value[,key=value]...` 的格式（例如：`java.lang:type=Memory`）。
3.  **管理**：注册后，任何连接到该 `MBeanServer` 的 JMX 客户端（如 JConsole）都能发现这个 MBean，并通过其暴露的属性、操作、通知来监控和管理它。
4.  **注销**：当资源不再需要管理时（通常在资源销毁时），可以通过 `MBeanServer.unregisterMBean(ObjectName)` 将其注销。

**总结：**
**MBean 是 JMX 框架中用于表示可管理资源的标准化 Java 对象。** 它通过定义一组清晰的 **属性（状态/配置）、操作（命令/动作）和通知（事件）**，为外部管理工具提供了一个统一的接口来监控应用的运行状况（如查看内存、线程）、调整配置（如修改日志级别）和执行管理任务（如触发垃圾回收、获取诊断信息）。它是实现 Java 应用运行时监控、管理和运维自动化的基石。

### 使用示例

1. 定义
```java
package com.mimaxueyuan.jvm.service;  
  
public interface MyServiceInfoMBean {  
  
    public String getOrderName();  
  
    public int getOrderCount();  
  
    public void printOrderInfo();  
  
}

package com.mimaxueyuan.jvm.service;  
  
public class MyServiceInfo implements MyServiceInfoMBean{  
  
    private String orderName;  
    private int orderCount;  
  
    public MyServiceInfo(String orderName, int orderCount) {  
        this.orderName = orderName;  
        this.orderCount = orderCount;  
    }  
  
    @Override  
    public String getOrderName() {  
        return orderName;  
    }  
  
    @Override  
    public int getOrderCount() {  
        return orderCount;  
    }  
  
    @Override  
    public void printOrderInfo() {  
        System.out.println("This is Kevin's order information.");  
    }  
}
```

2. 注册
```java
package com.mimaxueyuan.jvm.service;  
  
import org.springframework.stereotype.Service;  
  
import javax.management.*;  
import java.lang.management.ManagementFactory;  
  
@Service  
public class KevinMBeanService {  
  
    public void mbean() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {  
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();  
        ObjectName name = new ObjectName("myServiceInfoMBean:name=myServiceInfo1");  
        MyServiceInfo info = new MyServiceInfo("odr1",100001);  
        server.registerMBean(info, name);  
    }  
}
```

3. 调用
```java
@Controller    // This means that this class is a Controller  
@RequestMapping(path="/demo") // This means URL's start with /demo (after Application path)  
public class MainController {
	@GetMapping(path="/mbean")  
	public @ResponseBody String mbean() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {  
	    kevinMBeanService.mbean();  
	    return "ok";  
	}
}
```

```shell
curl localhost:8080/demo/mbean
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508061138573.png)

**==`java.lang` 自己实现的MBean==**
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508061140828.png)
对应的类和方法
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508061141454.png)

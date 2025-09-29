#JVM运行时内存区域 

对比：[13、什么是Java内存模型（JMM）？](2、相关技术/2、JUC/Hollis/Java并发/13、什么是Java内存模型（JMM）？.md)
参考：[51、一个Java进程占用的内存都哪些部分？](2、相关技术/3、JVM/Hollis/51、一个Java进程占用的内存都哪些部分？.md)

# 典型回答

根据Java虚拟机规范的定义，JVM的运行时内存区域主要由**Java堆**、**方法区**、**运行时常量池**、**虚拟机栈**、**本地方法栈**、**程序计数器**组成。其中<font color="red" size=5>堆、方法区以及运行时常量池是<font color="blue" size=5>线程之间共享的区域</font></font>，而**栈（本地方法栈+虚拟机栈）、程序计数器都是*线程独享***的。

需要注意的是，上面的这6个区域，是虚拟机规范中定义的，但是在具体的实现上，不同的虚拟机，甚至是同一个虚拟机的不同版本，在实现细节上也是有区别的。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507282251374.png)

1. **程序计数器**：一个**只读的存储器**，用于记录Java虚拟机正在执行的字节码指令的地址。它是线程私有的，为<font color="blue" size=5>每个线程维护一个独立的程序计数器，用于指示下一条将要被执行的字节码指令的位置</font>。它保证线程执行一个字节码指令以后，才会去执行下一个字节码指令。
2. **Java虚拟机栈**：一种**线程私有的存储器**，用于**存储Java中的局部变量**。根据Java虚拟机规范，每次<font color="blue" size=5>方法调用都会创建一个栈帧，该栈帧用于存储局部变量，操作数栈，动态链接，方法出口等信息</font>。当方法执行完毕之后，这个栈帧就会被弹出，变量作用域就会结束，数据就会从栈中消失。
3. **本地方法栈**：本地方法栈是一种特殊的栈，它与Java虚拟机栈有着相同的功能，但是它**支持本地代码（ Native Code ）的执行**。本地方法栈中<font color="blue" size=5>存放本地方法（ Native Method ）的参数和局部变量，以及其他一些附加信息</font>。这些本地方法一般是用C等本地语言实现的，虚拟机在执行这些方法时就会通过本地方法栈来调用这些本地方法。
4. **Java堆**：是**存储对象实例的运行时内存区域**。它是虚拟机运行时的内存总体的最大的一块，也一直**占据着虚拟机内存总量的一大部分**。Java堆由Java虚拟机管理，用于<font color="blue" size=5>存放对象实例，是几乎所有的对象实例都要在上面分配内存</font>。此外，Java堆还用于垃圾回收，虚拟机发现没有被引用的对象时，就会对堆中对象进行垃圾回收，以释放内存空间。
5. **方法区**：用于**存储已被加载的类信息、常量、静态变量、即时编译（JIT即时编译器）后的代码等数据的内存区域**。每加载一个类，方法区就会分配一定的内存空间，<font color="blue" size=5>用于存储该类的相关信息</font>，这部分空间随着需要而动态变化。方法区的*具体实现形式可以有多种，比如堆、永久代、元空间等*。
6. **运行时常量池**：是**方法区的一部分**。用于<font color="blue" size=5>存储编译阶段生成的信息，主要有字面量和符号引用常量两类</font>。其中符号引用常量包括了类的全限定名称、字段的名称和描述符、方法的名称和描述符。
   
   **编译期常量池**是在**编译期间生成的Constant_pool数据**， 会在**JVM启动时、类加载后，放入运行时常量池中**。运行时常量池中的数据也可以在程序运行期间加入

[37、运行时常量池和字符串常量池的关系是什么？](2、相关技术/3、JVM/Hollis/37、运行时常量池和字符串常量池的关系是什么？.md)

# 扩展知识

## 堆和栈的区别

堆和栈是Java程序运行过程中主要存储区域，经常被拿来对比，他们主要有以下区别（这里的栈主要指的是虚拟机栈）：
1. **存储位置不同**，堆是在JVM堆内存中分配空间，而栈是在JVM的栈内存中分配空间。
2. **存储的内容不同**，堆中主要存储对象，栈中主要存储本地变量
3. **堆是线程共享的，栈是线程独享的**
4. **堆是垃圾回收的主要区域**，不再引用这个对象，会被垃圾回收机制自动回收。栈的内存使用是一种先进后出的机制，**栈中的变量会在程序执行完毕后自动释放**
5. **栈的大小比堆要小的多**，一般是几百到几千字节
6. 栈的存储速度比堆快，代码执行效率高
7. 堆上会发生OutofMemoryError，栈上会发生StackOverflowError
[44、OutOfMemory和StackOverflow的区别是什么](2、相关技术/3、JVM/Hollis/44、OutOfMemory和StackOverflow的区别是什么.md)

## Java中的对象一定在堆上分配内存吗？

[6、Java中的对象一定在堆上分配内存吗？](2、相关技术/3、JVM/Hollis/6、Java中的对象一定在堆上分配内存吗？.md)

## 什么是堆外内存？

<font color="red" size=5>堆外内存是指将数据存储在堆以外的内存中，主要是指将一些直接内存分配在堆以外</font>，以提高应用程序的性能或节省堆内存空间。堆外内存可以**用于分配容量较大的缓冲区，比如文件缓冲区等等**。

Java 中的直接内存是通过使用 `java.nio` 包中的 `DirectByteBuffer` 类来实现的。`DirectByteBuffer` **类可以用来分配本地内存，并允许在 Java 和本地内存之间交换数据**。使用 `DirectByteBuffer` 可以减少从 Java 堆和本地堆之间进行复制和读写的开销，从而提升程序的性能。

## 方法区的变迁

前面我们提过，方法区其实是JVM规范中定义出来的一块区域，具体的虚拟机实现上是有很大的差别的。在**不同的版本中，方法区的位置也不尽相同**。

[34、什么是方法区？是如何实现的？](2、相关技术/3、JVM/Hollis/34、什么是方法区？是如何实现的？.md)

## JVM 参数

### 打印JVM参数

| 工具                         | 说明                                      |
| -------------------------- | --------------------------------------- |
| -XX:+PrintCommandLineFlags | 打印出用户手动设置或者JVM自动设置的XX选项(如堆空间大小和所选垃圾收集器) |
| -XX:+PrintFlagsInitial     | 打印出所有XX选项的默认值                           |
| -XX:+PrintFlagsFinal       | 打印出XX选项在运行程序时生效的值                       |

示例：`java -XX:+PrintCommandLineFlags -version`

`-XX:+PrintFlagsFinal`：可以打印出<font color="red" size=5>JVM在运行期间所有生效的参数</font>
Demo: `com.mimaxueyuan.jvm.xxparam.PrintXXParamDemo`

### 虚拟机栈

**参数说明**：-Xss 设置栈的大小，栈的大小直接决定函数调用的可达深度

`-Xss*size*`
设置线程堆栈大小（以字节为单位）。附加字母k或K表示KB，m或M表示MB，g或G表示GB。默认值取决于虚拟内存。 
以下示例以不同的单位将线程堆栈大小设置为1024 KB：
	-Xss1m 
	-Xss1024k 
	-Xss1048576
此选项相当于`-XX:ThreadStackSize=*size*`(可能会被弃用)

一般不建议调这个参数，一般会调代码

**异常说明**
JAVA虚拟机规范,对于虚拟机栈规定了两种异常；
1. 如果线程请求的栈的深度大于虚拟机所允许的最大值，则跑出StackOverflowException（虚拟机栈的深度允许动态扩展、也允许固定）
2. 当虚拟机栈扩展的时候无法申请到足够的内存则跑出`OutOfMemoryException`

### 本地方法栈

Sun HotSpot虚拟机直接把虚拟机栈和本地方法栈合二为一了

- 内部存放的是栈帧(Stack Frame)
- 每个Native方法执行的时候都会创建一个栈帧
- 虚拟机规范没有强制此部分实现使用的语言和数据结构，可以自由实现
- 虚拟机规范没有规定大小，可以固定也可以动态计算，如果固定则在每个线程创建的时候都可以单独设置，如果动态计算则要提供参数设置最大最小值。

### JAVA堆

1. `-Xms*size*`
   设置**堆的初始大小**（以字节为单位）。该值必须是1024的倍数且大于1 MB。附加字母k或K表示千字节，m或M指示兆字节，g或G指示千兆字节。以下示例显示如何使用各种单位将分配的内存大小设置为6 MB：
	1. -Xms6291456 
	2. -Xms6144k 
	3. -Xms6m
   如果未设置此选项，则初始大小将设置为老年代和年轻代分配的大小的总和。可以使用`-Xmn*size*`选项或`-XX:NewSize=*size*`选项设置年轻代的堆的初始大小，我们一般不直接设置老年代大小，而是自动计算(堆-新生代)。
2. `-Xmx*size*`
   指定**堆的最大大小**（以字节为单位），以字节为单位。该值**必须是1024的倍数且大于`2 MB`**。附加字母k或K表示千字节，m或M指示兆字节，g或G指示千兆字节。根据系统配置在运行时选择默认值。对于服务器部署，<font color="red" size=5>-Xms并-Xmx经常设置为相同的值</font>。以下示例显示如何使用各种单位将分配的内存的最大允许大小设置为80 MB：
	1. -Xmx83886080 
	2. -Xmx81920k 
	3. -Xmx80m
   该`-Xmx*size*`选项相当于`-XX:MaxHeapSize=*size*`。
3. `-XX:+HeapDumpOnOutOfMemoryError`
   在`java.lang.OutOfMemoryError`抛出异常时，通过使用堆分析器（HPROF）将Java堆转储到当前目录中的文件。您可以使用该`-XX:HeapDumpPath`选项显式设置堆转储文件路径和名称。**默认情况下，禁用此选项**，并在OutOfMemoryError抛出异常时不转储堆。
4. `-XX:HeapDumpPath=path`
   设置`-XX:+HeapDumpOnOutOfMemoryError`选项设置时，设置用于写入堆分析器（HPROF）提供的堆转储的路径和文件名。 
   默认情况下，该文件在当前工作目录中创建，并且名为`java_pid%p.hprof`，其中pid是导致错误的进程的标识符。
   以下示例显示如何显式设置默认文件（%p表示当前进程标识符）：
   `-XX：HeapDumpPath = /java_pid％p.hprof`
   以下示例显示如何将堆转储文件设置为C:/log/java/java_heapdump.log:`-XX：HeapDumpPath=C:/log/java/java_heapdump.log`

推荐设置以上3、4参数，并且HeapDumpPath要指定一块有存储空间的区域，否则可能导致存储失败。
- Demo: `com.mimaxueyuan.jvm.heap.HeapDemo0`

#### TLAB

`-XX:+UseTLAB`
允许在年轻代空间中使用线程局部分配块（TLAB）。<font color="red" size=5>默认情况下启用此选项</font>。要禁用TLAB，请指定`-XX:-UseTLAB`。

`-XX:TLABSize=*size*` **==不要轻易动这个参数==**
设置线程局部分配缓冲区（TLAB）的初始大小（以字节为单位）。附加字母k或K表示千字节，m或M指示兆字节，g或G指示千兆字节。如果此选项设置为0，则JVM会自动选择初始大小。
以下示例显示如何将初始TLAB大小设置为512 KB：
1. `-XXTLABSize=512K`

`-XX:TLABRefillWasteFraction=*size*`
表示，设置进入TLAB空间，单个对象大小;是一个比例值，默认为64;如果，对象小于整个`TLAB空间的1/64`，则放在TLAB区。如果，对象大于整个空间的`1/64`，则放在堆区。

`-XX:+PrintTLAB`
表示，查看TLAB信息
-  JDK8 `com.mimaxueyuan.jvm.heap.TLABDemo0`

`-XX:ResizeTLAB`
表示，自动调整`TLABRefillWasteFraction`阈值

### 方法区

**==不要轻易动这个参数==**

`-XX:PermSize=*size*`
设置分配给永久代的空间（以字节为单位），如果超出则会触发垃圾回收。此选项在JDK 8中已弃用，并被该`-XX:MetaspaceSize=*size*`选项取代。

`-XX:MaxPermSize=*size*`
设置最大永久代空间大小（以字节为单位）。此选项在JDK 8中已弃用，并由该`-XX:MaxMetaspaceSize=*size*`选项取代。

`-XX:MetaspaceSize=*size*`
设置分配的类元数据空间的大小，该空间将在第一次超出时触发垃圾回收。根据使用的元数据量，增加或减少垃圾收集的阈值。默认大小取决于平台。

`-XX:MaxMetaspaceSize=*size*`
设置可以为类元数据分配的最大本机内存量。默认情况下，大小不受限制。应用程序的元数据量取决于应用程序本身，其他正在运行的应用程序以及系统上可用的内存量。
以下示例显示如何将最大类元数据大小设置为256 MB：
`-XX：MaxMetaspaceSize=256m`

- JDK7 Demo: `com.mimaxueyuan.jvm.methodarea.PermGenDemo0`
- JDK8 Demo: `com.mimaxueyuan.jvm.methodarea.MetadataSpaceDemo0`

### GC参数

参考：[GC日志参数解析](2、相关技术/3、JVM/Hollis/16、新生代和老年代的垃圾回收器有何区别？.md#GC日志参数解析)

这些参数默认都是关闭

1. `-XX:+PrintGC`：输出GC简要日志，等价于`-verbose:gc`
2. `-XX:+PrintGCDetails`：输出GC的详细日志
3. `-XX:+PrintGCTimeStamps`：输出GC的时间戳（以基 准时间 的形式）
4. `-XX:+PrintGCDateStamps`：输出GC的时间戳（以日 期的形 式，如  2013-05-04T21:53:59.234+0800）
5. `-XX:+PrintHeapAtGC`：在进行<font color="red" size=5>GC的前后打印出堆的信息</font> 
6. `-Xloggc:../logs/gc.log`：日志文件的输出路径【GC日志默认输出到控制台】

示例代码：`com.mimaxueyuan.jvm.gc.GCLogDemo0`

关于垃圾收集器JVM参数设置参考：[垃圾收集器参数设置](16、新生代和老年代的垃圾回收器有何区别？#垃圾收集器参数设置)

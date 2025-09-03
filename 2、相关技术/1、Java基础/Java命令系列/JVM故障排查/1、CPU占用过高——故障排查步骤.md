
**==除了高并发导致的，其他的代码都是有问题的；单点程序、GC导致的CPU占用过高都是属于<font color="red" size=5>故障排查</font>，高并发导致CPU占用过高属于<font color="red" size=5>性能调优</font>==**

# 故障排查步骤

**有bug的程序、环境配置等引起的占用过高**
1. 使用 **`top`命令查看进程状态**，查看CPU和内存的占用情况是否异常
2. top命令查看cpu高的进程，然后**使用`top -H -p PID`命令查看这个==进程下的所有线程==**
	1. 如果 **==所有线程的CPU占用比例相差不大，并且都比较低==**，则是线程数量过大（启用线程过多或者请求量过大）<font color="red" size=5>考虑水平或垂直扩展</font>，**属于性能调优**
	   *cpu情况看下图*
	2. 如果是 **==某一个/某些线程占用比例特别大==**，则需要找到具体是哪个线程，记录TID，将TID转为十六进制（`printf "%x\n" TID`），这个16进制的数字就是JVM线程堆栈中的nid。
		- 去查看`jstack -l PID`，在线程堆栈中找出对应nid的线程，从而定位问题产生的位置；
		  问题的产生可能是因为该线程在进行大量的计算，也有可能因内存不足导致大量的GC
			- 十六进制的nid对应的线程是处理业务的线程：<font color="red" size=5>分析具体的业务代码存在的问题</font>
			  *cpu情况看下图*
			- 十六进制的nid对应的线程是GC的线程：<font color="red" size=5>频繁GC导致的CPU飙高</font>，*cpu情况看下图*，排查步骤：
				1. 查看GC频率：`jstat -gc -t -h10 pid 1s 100` 
				2. 导出内存信息，用`jProfile`分析
		- 也可以使用`arthas`定位问题，监控CPU占用过高的进程，在进入arthas命令行后执行`thread`、`thread -n 3`查看这个进程下线程情况，使用`thread pid`查看该线程的堆栈
---

1. 由 **==单点程序导致的CPU飙高==** 问题，对应的Java进程CPU占400%左右，下图是该进程对应的线程情况，**某线程导致的CPU过高**
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071556087.png)
2. 由 **==电脑配置不够导致CPU飙高==** 问题，对应的Java进程CPU占300%左右，下图是该进程对应的线程情况，**所有线程CPU的占用比例都很低，且相差不大**
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071121817.png)
3. 由 **==GC导致的CPU飙高==** 问题，对应的Java进程CPU占400%左右，下图是该进程对应的线程情况，**部分线程CPU占用过高**
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071236317.png)


# 1、排查单点程序导致的CPU占用过高实操

CPU、内存图形：[1、由单点程序导致CPU飙高图（有问题的图）](2、相关技术/1、Java基础/Java命令系列/JVM故障排查/2、CPU过高——GC和CPU图分析.md#1、由单点程序导致CPU飙高图（有问题的图）)

<font color="red" size=5>异常情况</font>，**==由代码问题导致的，需要修改代码==**

在项目`mini-java-server`执行：`curl localhost:8082/demo/highcpu` 运行以下代码
```java
@Controller    // This means that this class is a Controller  
@RequestMapping(path="/demo") // This means URL's start with /demo (after Application path)  
public class MainController {

	@GetMapping(path="/highcpu")  
	public @ResponseBody String highcpu() {  
	    for(int i=0;i<10000;i++){  
	       for(int j=0;i<10000;j++){  
	          new BigObject("bo"+i,new byte[1024*1024]);  
	       }  
	    }  
	    return "ok";  
	}
}
```

## 方式一：使用jstack、jcmd等

### 第一步：找出占用CPU高的进程

1. `top`：先查找CPU高的Java进程，PID为4442
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071555642.png)

2. `top -Hp 4442`：查看某个进程中所运行的线程的所有状态，找到占CPU高的Java线程，PID为4492
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071556087.png)

3. `printf "%x/n" 4492`：把线程pid从十进制转换为十六进制，获得操作系统中的线程id（线程堆栈中用十六进制保存的线程ID）

### 第二步：导出JVM进程Dump并分析其中的线程堆栈

```shell
jstack -l 4442 > D:/temp/1.log
```

在`1.log`文件中找十六进制的线程id对应的线程堆栈

## 方式二：使用arthas

启动arthas，执行`thread -n 3`查看cpu占用最高前三个线程，查看相应线程的堆栈
```shell
java -jar D:\arthas\arthas-3.1.1-bin\arthas-boot.jar

thread -n 3

# 查看占用CPU最高的线程堆栈
thread pid
```

# 2、排查高并发导致的CPU占用高实操和处理方案

<font color="red" size=5>正常情况</font>，**==代码没太大问题，但是性能不是特别好，可以通过硬件配置和JVM参数调优来优化==**

CPU、内存图：[2、由高并发导致的CPU占用高（正常图）](2、相关技术/1、Java基础/Java命令系列/JVM故障排查/2、CPU过高——GC和CPU图分析.md#2、由高并发导致的CPU占用高（正常图）)

在项目`mini-java-server`中对接口 `/jvm/simple` 进行压测，100个线程分别执行20000次
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071117653.png)
```java
@Controller  
@RequestMapping(path="/jvm")  
public class JVMController {  
  
    @Autowired  
    private UserRepository userRepository;  
  
    @GetMapping(path="/simple")  
    public @ResponseBody Iterable<User> simple() {  
       return userRepository.findAll();  
    }
}
```

执行`top`命令查看cpu、内存的情况，可以看到6961对应线程的CPU占用过高
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071119473.png)

执行`top -Hp 6961`查看cpu占用最高的进程下的线程，发现该进程下每个线程占用cpu都很低（这是正常情况，异常情况是：占用CPU过高且居高不下），但是线程多，这种情况不是代码的问题，可以考虑水平或垂直扩展。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071121817.png)

# 3、排查GC导致的CPU占用过高实操

<font color="red" size=5>异常情况</font>，**==由代码问题导致的，需要修改代码==**

CPU、内存图：[3、由GC导致CPU飙高图（有问题的图）](2、相关技术/1、Java基础/Java命令系列/JVM故障排查/2、CPU过高——GC和CPU图分析.md#3、由GC导致CPU飙高图（有问题的图）)

在项目`mini-java-server`执行：`curl localhost:8082/demo/highcpu` 运行以下代码
```java
@Controller  
@RequestMapping(path="/jvm")  
public class JVMController {  

    @GetMapping(path="/highmem")  
    public @ResponseBody String highmem() throws Exception{  
       List<BigObject> list = new ArrayList<BigObject>();  
       for(int i=0;i<90000;i++){  
          list.add(new BigObject("bo"+i,new byte[100*1024*1024]));  
          Thread.sleep(100);  
       }  
       return "ok high mem";  
    }
}
```

执行 `top` 命令查看哪些进程占用CPU过高，找到`PID=6961`的进程CPU占用高
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071549283.png)

执行 `top -Hp 6961` 查看CPU占用过高的进程下线程情况，发现部分线程CPU占用高，查看其中某些线程堆栈
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071236317.png)

执行 `jstack -l 6961 > D:/temp/1.log` 将堆栈打印到日志，在日志中查找上图占用CPU过高的线程的pid（把十进制转为十六进制），可以在下图看到，这些线程在执行GC（比如：6964【0x1b34】、6965【0x1b35】）
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071257664.png)

执行 `jstat -gc -t -h10 6961 1s 10000`查看gc情况，发现FullGC执行很频繁，1秒大概执行五六次FullGC，且GC后老年代容量几乎没变化，YoungGC每增加
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071310041.png)

1. 获得内存溢出的转储文件（JVM参数指定`-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/logs/heap_dump.hprof`）
2. `jmap -dump[live,]:format=b,file=heapDump pid`
3. `jcmd pid GC.heap_dump -all /data/heap_dump_all.hprof`

在JProfile中进行分析，分析内存泄漏在哪些对象上，计算出保留大小、找到保留大小最大的几个、查找这些大对象在那里被引用；

如果在JProfile实在分析不出来，可以尝试对服务水平扩展、增加JVM内存
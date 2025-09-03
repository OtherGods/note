对比：[1、CPU占用过高——故障排查步骤](2、相关技术/1、Java基础/Java命令系列/JVM故障排查/1、CPU占用过高——故障排查步骤.md)
除了使用这些图形化工具外还可以用GCViewer、GCeasy：[3、GC日志查看工具](2、相关技术/1、Java基础/Java命令系列/JVM故障排查/3、GC日志查看工具.md)

**==除了高并发导致的，其他的代码都是有问题的；单点程序、GC导致的CPU占用过高都是属于<font color="red" size=5>故障排查</font>，高并发导致CPU占用过高属于<font color="red" size=5>性能调优</font>==**

# 1、由单点程序导致CPU飙高图（有问题的图）

在项目`mini-java-server`执行：`curl localhost:8082/jvm/highcpu` 运行以下代码
```java
@Controller  
@RequestMapping(path="/jvm")  
public class JVMController {
	@GetMapping(path="/highcpu")  
	public @ResponseBody String highcpu() throws Exception{  
	    for(int i=0;i<9000000;i++){  
	       for(int j=0;j<9000000;j++){  
	          new User("Kevin"+i+j,"kevin@163.com");  
	       }  
	    }  
	    return "ok high cpu";  
	}
}
```

发现CPU持续居高不下
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071605505.png)

堆内存呈现不规律的锯齿状变化（Eden区呈现锯齿状，老年代几乎没什么变化【因为创建的对象小，达不到进入老年代的条件】），说明GC在频繁回收，每次都能回收很大空间（频繁GC、频繁STW，导致响应速度变慢）
关注点：
1. 垃圾回收的密集程度
2. 触发的YoungGC、FullGC的次数
3. 回收内存的趋势（可以回收的对象越来越小）
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071610944.png)

# 2、由高并发导致的CPU占用高（正常图）

虽然是正常情况，但是对于性能来说还有提升空间，可以通过JVM参数调优、硬件配置升级来解决存在的问题

在项目`mini-java-server`中对接口 `/jvm/simple` 进行压测，在3秒钟内20个线程分别执行200000次
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071830973.png)

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

<font color="red" size=5>正常情况</font>：
- **==堆内存（Eden区）呈现有规律的锯齿状，老年代维持平稳，小幅增长==**：因为GC在频繁回收
- **==在刚接收到请求时CPU刚开始呈现激增，后续维持平稳的锯齿状态==**：因为`21:06`有大量请求到来，需要在线程池创建大量线程

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071827937.png)
幸存区
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071840384.png)
老年代
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071839509.png)

频繁的GC会导致频繁的STW，对用户不友好，我们可以通过增大Eden区，使GC不是那么频繁，让每次GC回收的垃圾数量增加，调大Eden区前后内存对比图如下，左侧是Eden区小时的内存情况，右侧是Eden大时内存情况（修改应用启动时JVM的参数重新启动，jmx参数不要改动（IP + port + ……）就可以重新连接，而不是打开新的jconsole窗口）
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071718665.png)

可以看到上图中一次垃圾回收的量增加了(峰值与谷值差距)，GC回收的频率降低了

# 3、由GC导致CPU飙高图（有问题的图）

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

在方法执行期间CPU飙升，方法执行完毕后下降
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071651045.png)

堆内存在某段时间持续居高不下（这里最后下降，是因为list在方法内部，当方法抛出内存溢出异常时highmem方法退出，list释放，内存下降，如果list字段作为类的成员变量那么内存就一直居高不下）
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071648733.png)

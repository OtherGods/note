# 工具
在 [1、CPU占用过高——故障排查步骤](2、相关技术/1、Java基础/Java命令系列/JVM故障排查/1、CPU占用过高——故障排查步骤.md)、[2、CPU过高——GC和CPU图分析](2、相关技术/1、Java基础/Java命令系列/JVM故障排查/2、CPU过高——GC和CPU图分析.md) 中我们分析由GC导致的CPU过高的场景时候，使用与gc有关的命令和可视化工具（jstat、jconsole等），这些工具的缺点是我们无法提前知道成需要发生GC，或者在问题发生之后根据GC日志去分析垃圾回收情况和内存占用情况，但GC日志很难阅读，这种情况可以用<font color="red" size=5>借助可视化工具来阅读保存的GC日志</font>

1. **GCViewer**
  github地址:https://github.com/chewiebug/GCViewer    需要自己打包
  可以直接下载现成的jar,地址: http://sourceforge.net/projects/gcviewer/files/gcviewer-1.35-SNAPSHOT.jar/download
	- 除了这个工具还有很多，例如GCLogViewer-0.3-win， google出品的工具，但是只支持32位JRE，所以不建议采用。
2. **GCeasy**
   https://gceasy.io/ 在线分析工具无需下载;

这些工具都需要一些gc日志参数，一般这样设置heyang：
```shell
# 可以使用 -verbose:gc 试试
-verbose.gc -Xloggc:gc.log -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCTimeStamps -XX:+PrintGCDetails
```

**其中参数 `-XX:+PrintGCApplicationStoppedTime` 可以打印出由GC引起应用暂停（STW）的时间是什么**

# 测试

### 异常图

在项目`mini-java-server`中对接口 `/demo/simple` 进行压测，100个线程分别执行20000次
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508071117653.png)
```java
@Controller    // This means that this class is a Controller  
@RequestMapping(path="/demo") // This means URL's start with /demo (after Application path)  
public class MainController {
	@GetMapping(path="/highmem")
	public @ResponseBody String highmem() {  
	    for(int i=0;i<1000;i++){  
	       new BigObject("bo"+i,new byte[1024*1024]);  
	    }  
	    return "ok";  
	}
}
```

在**GCViewer**中效果，图中橙色是新生代，紫色是老年代，可以看到不管是老年代还是新生代都在频繁的gc
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508072310098.png)
新生代回收6876次老年代回收294次
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508072312136.png)

### 正常图

在项目`mini-java-server`中对接口 `/demo/simple` 进行压测，100个线程分别执行20000次
```java
@Controller  
@RequestMapping(path="/jvm")  
public class JVMController {  
  
    @Autowired  
    private UserRepository userRepository;  
  
    @GetMapping(path="/simple")  
    public @ResponseBody Iterable<User> simple() throws InterruptedException  
    {  
       Thread.sleep(100);  
       return userRepository.findAll();  
    }
}
```

在**GCViewer**中效果，图中橙色是新生代，紫色是老年代，可以看到新生代执行呈现稳定的锯齿状，老年代稳定不变
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508072349127.png)

在Event details中可以看到YoungGC执行了177次，平均执行时间为12ms，FullGC执行了3次，平均执行时间为98ms
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508072356894.png)

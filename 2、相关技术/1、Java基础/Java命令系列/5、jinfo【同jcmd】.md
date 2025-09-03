对照：[VM.flags（替代jinfo）](2、相关技术/1、Java基础/Java命令系列/0、jcmd命令.md#VM.flags（替代jinfo）)、[VM.system_properties（替代jinfo）](2、相关技术/1、Java基础/Java命令系列/0、jcmd命令.md#VM.system_properties（替代jinfo）)

jinfo可以输出java进程、core文件或远程debug服务器的配置信息。这些配置信息包括JAVA系统参数及命令行参数,如果进程运行在64位虚拟机上，需要指明`-J-d64`参数，如：`jinfo -J-d64 -sysprops pid`

另外，Java7的官方文档指出，这一命令在后续的版本中可能不再使用。笔者使用的版本(jdk8)中已经不支持该命令(笔者翻阅了[java8中该命令的文档](http://docs.oracle.com/javase/8/docs/technotes/tools/unix/jinfo.html)，其中已经明确说明不再支持)。提示如下：

```
HollisMacBook-Air:test-workspace hollis$ jinfo 92520
Attaching to process ID 92520, please wait...
^@

Exception in thread "main" java.lang.reflect.InvocationTargetException
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    at java.lang.reflect.Method.invoke(Method.java:606)
    at sun.tools.jinfo.JInfo.runTool(JInfo.java:97)
    at sun.tools.jinfo.JInfo.main(JInfo.java:71)
Caused by: sun.jvm.hotspot.runtime.VMVersionMismatchException: Supported versions are 24.79-b02. Target VM is 25.40-b25
    at sun.jvm.hotspot.runtime.VM.checkVMVersion(VM.java:234)
    at sun.jvm.hotspot.runtime.VM.<init>(VM.java:297)
    at sun.jvm.hotspot.runtime.VM.initialize(VM.java:368)
    at sun.jvm.hotspot.bugspot.BugSpotAgent.setupVM(BugSpotAgent.java:598)
    at sun.jvm.hotspot.bugspot.BugSpotAgent.go(BugSpotAgent.java:493)
    at sun.jvm.hotspot.bugspot.BugSpotAgent.attach(BugSpotAgent.java:331)
    at sun.jvm.hotspot.tools.Tool.start(Tool.java:163)
    at sun.jvm.hotspot.tools.JInfo.main(JInfo.java:128)
    ... 6 more
```

由于打印jvm常用信息可以使用[Jps](http://www.hollischuang.com/archives/105)命令，并且在后续的java版本中可能不再支持，所以这个命令笔者就不详细介绍了。下面给出help信息，读者可自行阅读使用。（这就好像上高中，老师讲到一些难点的时候说，不明白也不要紧，知道有这么一回事就可以了！）

### 用法摘要

#### 语法
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508060010664.png)

- `-flag <name>`：打印指定name的 JVM flag
- `-flag [+|-]<name>`：启用或者禁用指定name的 JVM flag
- `-flag <name>=<value>`：设置指定名称的name flag 为给定的值，**==很多参数是不允许调整的==**
- `-flags`：打印  JVM flags
- `-sysprops`：打印Java 系统属性
- `<no option>`：不指定任何option，则打印出所有的JVM flags和sysprops
- `-h | -help`：打印帮助信息

**==与JPS的差异是：jinfo修改完JVM参数值后jsp看不到修改后的效果，但是jinfo可以看到==**

**存在的问题：`jinfo -flag xx=oo pid` 后执行 `jinfo -flags pid`看到的xx可能不是我们设置的oo，必须执行 `jinfo -flag xx pid`后才能看到**
```shell
D:\Java\jdk\bin>jinfo -flags 10456
Attaching to process ID 10456, please wait...
Debugger attached successfully.
……
Command line:  -Xmn200M -Xms300M -Xmx300M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=D:\temp\heap_dump.hprof
……

D:\Java\jdk\bin>jinfo -flag HeapDumpPath=d:\temp\1 10456

-- 修改值后
D:\Java\jdk\bin>jinfo -flags 10456
Attaching to process ID 10456, please wait...
Debugger attached successfully.
……
Command line:  -Xmn200M -Xms300M -Xmx300M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=null
……

-- 执行【jinfo -flag HeapDumpPath 10456】才能看到效果
D:\Java\jdk\bin>jinfo -flag HeapDumpPath 10456
-XX:HeapDumpPath=d:\temp\1
```


### 参考资料

[jinfo](http://docs.oracle.com/javase/7/docs/technotes/tools/share/jinfo.html)

**(全文完)**
参考：
- [1、CPU占用过高——故障排查步骤](2、相关技术/1、Java基础/Java命令系列/JVM故障排查/1、CPU占用过高——故障排查步骤.md)
- [2、CPU过高——GC和CPU图分析](2、相关技术/1、Java基础/Java命令系列/JVM故障排查/2、CPU过高——GC和CPU图分析.md)
- [3、GC日志查看工具](2、相关技术/1、Java基础/Java命令系列/JVM故障排查/3、GC日志查看工具.md)

# 典型回答

[1、什么是POI，为什么它会导致内存溢出？](2、相关技术/29、文件处理/1、什么是POI，为什么它会导致内存溢出？.md)

上一篇中介绍了POI的内存溢出以及几种Workbook，那么，我们在做文件写入的时候，该如何选择呢？他们的在内存使用上有啥差异呢？

我们接下来分别使用XSSFWorkbook和SXSSFWorkbook来写入一个Excel文件，分别看一下堆内存的使用情况。

## 使用XSSF写入文件

pom.xml
```xml
<!-- https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-to-slf4j -->  
<dependency>  
    <groupId>org.apache.logging.log4j</groupId>  
    <artifactId>log4j-to-slf4j</artifactId>  
    <version>2.17.2</version>  
</dependency>  
  
<!-- https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-api -->  
<dependency>  
    <groupId>org.apache.logging.log4j</groupId>  
    <artifactId>log4j-api</artifactId>  
    <version>2.17.2</version>  
</dependency>

<!-- https://mvnrepository.com/artifact/org.apache.poi/poi -->  
<dependency>  
    <groupId>org.apache.poi</groupId>  
    <artifactId>poi</artifactId>  
    <version>5.2.2</version>  
</dependency>  
<dependency>  
    <groupId>org.apache.poi</groupId>  
    <artifactId>poi-ooxml</artifactId>  
    <version>5.2.2</version>  
</dependency>
```

JVM参数配置：
```shell
# 堆分布情况
-Xms512M -Xmx1024M -Xmn512M 

# GC日志
-verbose.gc -Xloggc:gc.log -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCTimeStamps -XX:+PrintGCDetails 

# jmx，用于jconsole
-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=7777 -Dcom.sun.management.jmxremote.rmi.port=7777 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=D:\temp\heap_dump.hprof
```

对应Java代码
```java
package com.mimaxueyuan.jvm.test;  
  
import org.apache.poi.ss.usermodel.Cell;  
import org.apache.poi.ss.usermodel.Row;  
import org.apache.poi.ss.usermodel.Sheet;  
import org.apache.poi.xssf.usermodel.XSSFWorkbook;  
  
import java.io.FileOutputStream;  
import java.io.IOException;  
import java.util.UUID;  
  
public class XSSFExcelTest {  
      
    public static void main(String[] args) throws InterruptedException {  
        // 创建一个新的工作簿  
        XSSFWorkbook workbook = new XSSFWorkbook();  
          
        // 创建一个新的表格  
        Sheet sheet = workbook.createSheet("Example Sheet");  
        for (int i = 0; i < 100000; i++) {  
            // 创建行（从0开始计数）  
            Row row = sheet.createRow(i);  
            for (int j = 0; j < 100; j++) {  
                // 在行中创建单元格（从0开始计数）  
                Cell cell = row.createCell(j);  
                  
                // 设置单元格的值  
                cell.setCellValue(UUID.randomUUID().toString());  
            }  
        }  
          
        // 设置文件路径和名称  
        String filename = "D:\\temp\\example.xlsx";  
          
        try (FileOutputStream outputStream = new FileOutputStream(filename)) {  
            // 将工作簿写入文件  
            workbook.write(outputStream);  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            try {  
                // 关闭工作簿资源  
                workbook.close();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
    }  
}
```

运行main方法的过程中，通过arthas看一下堆内存的使用情况：
```shell
curl -O https://arthas.aliyun.com/arthas-boot.jar

java -jar arthas-boot.jar
```

```shell
➜  java -jar arthas-boot.jar
[INFO] JAVA_HOME: /Library/Java/JavaVirtualMachines/jdk1.8.0_311.jdk/Contents/Home/jre
[INFO] arthas-boot version: 3.7.1
[INFO] Found existing java process, please choose one and input the serial number of the process, eg : 1. Then hit ENTER.
* [1]: 41417 org.jetbrains.idea.maven.server.RemoteMavenServer36
  [2]: 4874 
  [3]: 43484 org.jetbrains.jps.cmdline.Launcher
  [4]: 43485 excel.write.XSSFExcelTest
4
[INFO] arthas home: /Users/hollis/.arthas/lib/3.7.1/arthas
[INFO] Try to attach process 43485
Picked up JAVA_TOOL_OPTIONS: 
[INFO] Attach process 43485 success.
[INFO] arthas-client connect 127.0.0.1 3658
```

执行memory命令（这个执行的时间点很重要，我是在String filename = "example.xlsx";前输出了一行日志，然后sleep 50s，我在控制台看到这行日志之后开始查看堆内存情况）：
```shell
[arthas@43485]$ memory
```

得到结果：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508081121709.png)

即占用堆内存1200+M。

## 使用SXSSFWorkbook写入文件

替换API`XSSFWorkbook`为`SXSSFWorkbook`
```java
SXSSFWorkbook workbook = new SXSSFWorkbook();
```

同样通过Arthas查看内存占用情况：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508081613665.png)

占用内存在148M左右。

## 对比结果

同样的一份文件写入，XSSFWorkbook需要1200+M，SXSSFWorkbook只需要148M。所以大文件的写入，使用SXSSFWorkbook是可以更加节省内存的。

如果不方便使用arthas，也可以直接在JVM启动参数中增加Xmx150m的参数，运行以上两段代码，使用XSSFWorkbook的会抛出OOM：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508081630102.png)

而使用SXSSFWorkbook时则不会。

所以，在使用POI时，如果要做大文件的写入，建议使用`SXSSFWorkbook`，会更加节省内存。

## 内存、日志情况对比

我本地执行后对比的

API修改前后jconsole中内存情况，在`16:10`到`16:14`之间使用`XSSFWorkbook`导出了两次，可以看到内存占用飙升，在`16:14`之后使用`SXSSFWorkbook`导出，内存占用明显下降了，这三次导出在内存占用上主要体现在老年代，新生代没太大差别
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508081618832.png)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508081624956.png)

使用API`XSSFWorkbook`对应的GC日志分析
在GCview中，橙色表示新生代，紫色表示老年代，可以看到新生代已使用内存逐渐趋于平稳，GC收集垃圾大小的能力逐渐减小（图片呈收敛趋势）；老年代容量持续居高不下，虽然也在频繁GC，但GC回收的内存不大；老年代中展示的绿色折线是GC停顿时间
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508081115549.png)

在JProfiler中可以看到内存溢出时保存的内存快照中，某大对象对应的调用路径，可以找到对应的代码
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508081546685.png)

# 扩展知识

## 为啥SXSSFWorkbook占用内存更小?

[3、为啥POI的SXSSFWorkbook占用内存更小？](2、相关技术/29、文件处理/3、为啥POI的SXSSFWorkbook占用内存更小？.md)

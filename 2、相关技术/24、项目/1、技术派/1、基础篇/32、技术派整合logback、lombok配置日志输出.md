对于一个后端来说，日志是不可缺少的，这个东西，形象点就像剑客手中的剑，没有剑你就刷不了帅，砍不了人；对于我们而言，没有日志文件，你就很难定位问题。

技术派项目中，有很多关键地方添加了日志，比如业务节点日志，比如外部请求相关日志，比如基于日志的异常报警等。我们这篇教程主要目的就是让大家知道怎么再项目中集成日志框架，怎么在实际的业务代码中使用日志，主要的知识点将包含以下几点：
1. 日志级别选择
2. 日志输出到文件
3. 格式化输出
4. 日志文件管理（自动删除，压缩归档等）
5. 常用的logback-spring.xml配置


# 1、日志文件配置
一个SpringBoot项目，根据官方文档的说明，默认选择的是Logback来记录日志；logback也是相对来说用的比较多的框架了，技术派整个项目也是使用的logback，当然业界内除了它之外，log4j2也是非常多人的选择；我们这里主要以logback进行说明

## 1.1、集成lombok
lombok中提供了一个@Slf4j的注解，放在类上，这样就可以直接在类中引入log对象进行日志输出；

具体的关于lombok的知识点，可以参考教程：[33、技术派整合Lombok，让代码更简洁](2、相关技术/24、项目/1、技术派/1、基础篇/33、技术派整合Lombok，让代码更简洁.md)

集成方式也比较简单，项目的pom.xml中添加依赖
```yml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

其次，对于idea进行开发的小伙伴来说，需要安装一下lombok的插件
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221708111.png)


## 1.2、配置说明
进入logback的配置文件之前，先看一下默认的配置有哪些，以及如何使用，先整一个基础的使用demo

### 1.2.1、使用demo

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221712744.png)

看下控制台输出结果，info,warn,error可以正常输出且输出格式包含一些附加信息，System.out也可以正常输出，debug的日志没有
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221713985.png)

### 1.2.2、debug日志支持
默认是不输出debug日志的，如果需要，则需要在properties配置文件中添加 debug=true 属性；同样可以设置trace=true，就可以看应用输出的trace日志

但是，请注意，即便配置了debug=true，上面测试中的debug日志也依然不会输出，主要原因就是下面的log level的参数配置

### 1.2.3、log level
上面的配置虽然在控制台打印了**一些debug日志**，但并没有打印我们业务代码中的debug日志，因为除了上面的配置之外，还需要开启下面这个，如下设置
```properties
# 下面这个表示兜底的日志输出级别，默认为DEBUG
logging.level.root=DEBUG
# 下面这个表示，对于包路径为 org.springframework 的日志输出时，只打印info日志
logging.level.org.springframework=INFO
```

上面这个配置就是指定包下日志输出的等级，root表示默认的级别
如上配置后，再次执行上面的代码，输出结果如下
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221724914.png)


### 1.2.4、输出格式
默认的输出格式如前面的截图，如果希望更改下输出的日志格式，可以通过修改属性来完成，一个也实例如下
```prtperties
logging.pattern.console=%date{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

再次执行，输出样式如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221727063.png)

至于这个属性值的构成原则，在后面说到logback.xml配置文件语法时，一并再说

**说明**
还有个参数可以设置文本的颜色，个人感觉实用性不是特别大，只贴下配置如下（如对日志显示有啥需求的，idea上装一个 Grep Console 插件可能更合适）
```properties
## 检测终端是否支持ANSI，是的话就采用彩色输出
spring.output.ansi.enabled=detect
## %clr(){} 格式，使文本以蓝色输出
logging.pattern.console=%clr(%d{yyyy-MM-dd HH:mm:ss.SSS} %thread] %-5level %logger{36} - %msg%n){blue}
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221731300.png)


### 1.2.5、输出到文件

上面所有的日志都是输出到控制台，在实际的生成环境中，一般要求日志写到文件，可以方便随时进行查看，通过设置相关参数也可以很简单实现
```properties
## 输出的日志文件
logging.file=logs/info.log
## 当文件超过1G时，归档压缩
logging.file.max-size=1GB
## 日志文件最多保存3天
logging.file.max-history=3
```

同样执行前面的代码两次，输出如下, 两次的输出结果都可以在日志文件中查到，相比较于控制台而言，用于查历史日志就更加的方便了
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221732209.png)

上图中，控制台的输出格式和日志文件的输出格式不一样，因为前面修改了控制台的输出样式；如果希望修改文件中的日志格式，也可以通过修改配置logging.pattern.file来实现

# 2、logbak.xml日志配置
前面介绍的是SpringBoot集成的日志配置方式，技术派选择更灵活的logback.xml的日志文件配置方式，接下来我们看一下相关知识点
配置文件名：logback-spring.xml
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221734445.png)

## 2.1、配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>  
<configuration>  
    <!-- fixme 知识点：logback配置变量的姿势，如何读取Spring配置参数   -->  
    <springProperty scope="context" name="log.path" source="log.path" defaultValue="logs"/>  
    <springProperty scope="context" name="log.env" source="env.name" defaultValue="NO"/>  
    <property name="log.service.name" value="pai"/>  
    <property name="log.req.name" value="req"/>  
  
  
    <!-- %m输出的信息,%p日志级别,%t线程名,%d日期,%c类的全名,%i索引【从数字0开始递增】,,, -->  
    <!-- appender是configuration的子节点，是负责写日志的组件。 -->  
    <!-- ConsoleAppender：把日志输出到控制台 -->  
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">  
        <encoder>  
            <pattern>%d [%t] %-5level|%mdc{traceId}|%mdc{bizCode}|%logger{36}.%M\(%file:%line\) - %msg%n</pattern>  
            <!-- 控制台也要使用UTF-8，不要使用GBK，否则会中文乱码 -->  
            <charset>UTF-8</charset>  
        </encoder>  
    </appender>  
  
    <!-- 当出现error异常日志时，邮件报警   -->  
    <appender name="errorAlarm" class="com.github.paicoding.forum.core.util.AlarmUtil">  
        <!--如果只是想要 Error 级别的日志，那么需要过滤一下，默认是 info 级别的，ThresholdFilter-->  
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">  
            <level>ERROR</level>  
        </filter>  
    </appender>  
  
    <!-- RollingFileAppender：滚动记录文件，先将日志记录到指定文件，当符合某个条件时，将日志记录到其他文件 -->  
    <!-- 以下的大概意思是：1.先按日期存日志，日期变了，将前一天的日志文件名重命名为XXX%日期%索引，新的日志仍然是demo.log -->  
    <!--             2.如果日期没有发生变化，但是当前日志的文件大小超过1KB时，对当前日志进行分割 重命名-->  
    <appender name="service" class="ch.qos.logback.core.rolling.RollingFileAppender">  
        <!--如果只是想要 Error 级别的日志，那么需要过滤一下，默认是 info 级别的，ThresholdFilter-->  
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">  
            <level>INFO</level>  
        </filter>  
        <File>${log.path}/${log.service.name}-${log.env}.log</File>  
        <!-- rollingPolicy:当发生滚动时，决定 RollingFileAppender 的行为，涉及文件移动和重命名。 -->  
        <!-- TimeBasedRollingPolicy： 最常用的滚动策略，它根据时间来制定滚动策略，既负责滚动也负责出发滚动 -->  
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">  
            <!-- 活动文件的名字会根据fileNamePattern的值，每隔一段时间改变一次 -->  
            <!-- 定义归档文件名 -->  
            <fileNamePattern>${log.path}/arch/${log.service.name}-${log.env}.%d.%i.log</fileNamePattern>  
            <!-- 每产生一个日志文件，该日志文件的保存期限为3天 -->  
            <maxHistory>3</maxHistory>  
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">  
                <!-- maxFileSize:这是活动文件的大小，默认值是10MB，测试时可改成1KB看效果 -->  
                <maxFileSize>100MB</maxFileSize>  
            </timeBasedFileNamingAndTriggeringPolicy>  
        </rollingPolicy>  
        <encoder>  
            <!-- pattern节点，用来设置日志的输入格式 -->  
            <pattern>  
                [%d{yyyy-MM-dd HH:mm:ss}]|%mdc{traceId}|%mdc{bizCode}|{"logger":"%logger{36}", "thread":"%thread", "msg":"%msg"}%n  
            </pattern>  
            <!-- 记录日志的编码:此处设置字符集 - -->            <charset>UTF-8</charset>  
        </encoder>  
    </appender>  
  
    <!--  请求日志 -->  
    <appender name="reqLog" class="ch.qos.logback.core.rolling.RollingFileAppender">  
        <File>${log.path}/${log.req.name}-${log.env}.log</File>  
        <!--滚动策略，按照时间滚动 TimeBasedRollingPolicy-->        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">  
            <!--文件路径,定义了日志的切分方式——把每一天的日志归档到一个文件中,以防止日志填满整个磁盘空间-->  
            <FileNamePattern>${log.path}/arch/req/req.%d{yyyy-MM-dd}.%i.log.gz</FileNamePattern>  
            <!-- 单个日志文件最多 100MB -->            <maxFileSize>100MB</maxFileSize>  
            <!--只保留最近10天的日志-->  
            <maxHistory>10</maxHistory>  
            <!--用来指定日志文件的上限大小，那么到了这个值，就会删除旧的日志-->  
            <totalSizeCap>1GB</totalSizeCap>  
        </rollingPolicy>  
        <!--日志输出编码格式化-->  
        <encoder>  
            <charset>UTF-8</charset>  
            <pattern>[%d{yyyy-MM-dd HH:mm:ss}|%mdc{traceId}|] - %msg%n</pattern>  
        </encoder>  
    </appender>  
  
    <logger name="req" level="info" additivity="false">  
        <appender-ref ref="reqLog"/>  
    </logger>  
  
  
    <logger name="springfox.documentation.spring" level="warn" additivity="false">  
        <appender-ref ref="STDOUT"/>  
        <appender-ref ref="service"/>  
    </logger>  
  
    <!-- 指定项目中某个包，当有日志操作行为时的日志记录级别 -->  
    <!-- 级别依次为【从高到低】：FATAL > ERROR > WARN > INFO > DEBUG > TRACE  -->  
    <!-- additivity=false 表示匹配之后，不再继续传递给其他的logger-->  
    <logger name="com.github.paicoding.forum" level="INFO" additivity="false">  
        <appender-ref ref="service"/>  
        <appender-ref ref="STDOUT"/>  
        <appender-ref ref="errorAlarm"/>  
    </logger>  
  
    <!-- 控制台输出日志级别 -->  
    <root level="INFO">  
        <appender-ref ref="STDOUT"/>  
        <appender-ref ref="service"/>  
    </root>  
</configuration>
```


## 2.2、属性参数

上面的配置文件中开头就有四个配置参数:
```xml
-- 从配置文件中读取参数 name:log.path；value:配置文件中log.path的值
<springProperty scope="context" name="log.path" source="log.path" defaultValue="logs"/>  
<springProperty scope="context" name="log.env" source="env.name" defaultValue="NO"/>  

-- 自定义参数 name：log.service.name；value:pai
<property name="log.service.name" value="pai"/>  
<property name="log.req.name" value="req"/>
```
1. **springProperty**：表示这里定义的参数，从spring的配置中获取，如log.path拿的就是spring配置文件中配置的log.path的值；
   
   如下图，dev与prod的日志存储路径不同，本地开发时，日志文件放在项目目录下，生成环境部署时，放在确定的目录下：
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221754869.png)
2. **property**: 这个则是定义日志文件参数，供日志配置文件实际使用使用方式：`${配置名}`
   如：
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307221834970.png)

## 2.3、appender标签
在前面的配置文件中，appender这个标签算是比较重要的，上面定义了两类，一个控制台输出，一个文件输出

### 2.3.1、控制台or文件的选择
appender的class属性来选择
1. 控制台： ch.qos.logback.core.ConsoleAppender
2. 滚动时间窗口文件：ch.qos.logback.core.rolling.RollingFileAppender

### 2.3.2、输出格式
以实例 `<pattern>%d [%t] %-5level %logger{36}.%M\(%file:%line\) - %msg%n</pattern>` 进行说明
1. %m输出的信息,
	1. %msg——日志消息
2. %p日志级别,
	1. %-5level——日志级别，并且使用5个字符靠左对齐
3. %t线程名,
	1. %thread——输出日志的进程名字，这在Web应用以及异步任务处理中很有用
4. %d日期,
	1. %d{HH: mm:ss.SSS}——日志输出时间
5. %c类的全名,
6. %i索引【从数字0开始递增】,
7. %M方法名,
8. %lines输出日志的行数,
9. %F/%file源码文件名。
10. %logger{36}——日志输出者的名字
11. %n——平台的换行符


### 2.3.3、日志归档相关
一般是每天归档一下日志文件，避免所有的日志都堆积到一个文件，相关配置如下：
```xml
<!-- RollingFileAppender：滚动记录文件，先将日志记录到指定文件，当符合某个条件时，将日志记录到其他文件 -->  
<!-- 以下的大概意思是：1.先按日期存日志，日期变了，将前一天的日志文件名重命名为XXX%日期%索引，新的日志仍然是demo.log -->  
<!--             2.如果日期没有发生变化，但是当前日志的文件大小超过1KB时，对当前日志进行分割 重命名-->  
<appender name="service" class="ch.qos.logback.core.rolling.RollingFileAppender">  
    <!--如果只是想要 Error 级别的日志，那么需要过滤一下，默认是 info 级别的，ThresholdFilter-->  
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">  
        <level>INFO</level>  
    </filter>    
    <File>${log.path}/${log.service.name}-${log.env}.log</File>  
    <!-- rollingPolicy:当发生滚动时，决定 RollingFileAppender 的行为，涉及文件移动和重命名。 -->  
    <!-- TimeBasedRollingPolicy： 最常用的滚动策略，它根据时间来制定滚动策略，既负责滚动也负责出发滚动 -->  
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">  
        <!-- 活动文件的名字会根据fileNamePattern的值，每隔一段时间改变一次 -->  
        <!-- 定义归档文件名 -->  
        <fileNamePattern>${log.path}/arch/${log.service.name}-${log.env}.%d.%i.log</fileNamePattern>  
        <!-- 每产生一个日志文件，该日志文件的保存期限为3天 -->  
        <maxHistory>3</maxHistory>  
        <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">  
            <!-- maxFileSize:这是活动文件的大小，默认值是10MB，测试时可改成1KB看效果 -->  
            <maxFileSize>100MB</maxFileSize>  
        </timeBasedFileNamingAndTriggeringPolicy>    
	</rollingPolicy>    
	<encoder>        <!-- pattern节点，用来设置日志的输入格式 -->  
        <pattern>  
            [%d{yyyy-MM-dd HH:mm:ss}]|%mdc{traceId}|%mdc{bizCode}|{"logger":"%logger{36}", "thread":"%thread", "msg":"%msg"}%n  
        </pattern>  
        <!-- 记录日志的编码:此处设置字符集 - -->        
        <charset>UTF-8</charset>  
    </encoder>
</appender>
```

当单文件特别大时，分析也不是一件容易的事情，常见的两个设置参数为日志文件最多保存时间、文件大小，相关配置如下：
```xml
<!--  请求日志 -->  
<appender name="reqLog" class="ch.qos.logback.core.rolling.RollingFileAppender">  
    <File>${log.path}/${log.req.name}-${log.env}.log</File>  
    <!--滚动策略，按照时间滚动 TimeBasedRollingPolicy-->    
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">  
        <!--文件路径,定义了日志的切分方式——把每一天的日志归档到一个文件中,以防止日志填满整个磁盘空间-->  
        <FileNamePattern>${log.path}/arch/req/req.%d{yyyy-MM-dd}.%i.log.gz</FileNamePattern>  
        <!-- 单个日志文件最多 100MB -->        
        <maxFileSize>100MB</maxFileSize>  
        <!--只保留最近10天的日志-->  
        <maxHistory>10</maxHistory>  
        <!--用来指定日志文件的上限大小，那么到了这个值，就会删除旧的日志-->  
        <totalSizeCap>1GB</totalSizeCap>  
    </rollingPolicy>    <!--日志输出编码格式化-->  
    <encoder>  
        <charset>UTF-8</charset>  
        <pattern>[%d{yyyy-MM-dd HH:mm:ss}|%mdc{traceId}|] - %msg%n</pattern>  
    </encoder>
</appender>
```


## 2.4、logger标签
logger标签中name表示那些包路径下的日志输出会匹配这个（或者logger直接使用了这个name【如代码中Java代码：`private static Logger REQ_LOG = LoggerFactory.getLogger("req");`】，也会匹配他）。
另外两个重要的属性：
1. level：表示输出日志的级别，可以根据实际场景设置某些日志输出，如框架层我只关系warn级别日志，我自己的业务可能就想关注info级别的日志
2. additivity：这个属性很容易不设置，如果不设置，那么当一个日志输出，有多个logger匹配时，这个日志就会被输出多次，建议设置为false

示例：
```xml
<!-- 
指定项目中某个包，当有日志操作行为时的日志记录级别；
级别依次为【从高到低】：FATAL > ERROR > WARN > INFO > DEBUG > TRACE
additivity=false 表示匹配之后，不再继续传递给其他的logger
-->  
<logger name="com.github.paicoding.forum" level="INFO" additivity="false">  
    <appender-ref ref="service"/>  
    <appender-ref ref="STDOUT"/>  
    <appender-ref ref="errorAlarm"/>  
</logger>
```


# 3、使用实例

在技术派中，有很多地方都可以看到日志的身影，使用姿势也没有什么特殊的，常见的四种级别：
1. log.debug：调试
2. log.info：信息输出
3. log.warn：警告
4. log.error：错误
需要关注的一点是，在输出debug日志时，推荐使用下面这种姿势
```java
//第一种
//不要直接使用这种，因为存在日志不打印，但拼接字符串的情况
//log.debug("current request: {}" +  request);  

//第二种，比第一种方式好
if (log.isDebugEnabled()) {  
    log.debug("current request: {}", request);
}

//第三种
//有的地方会使用这种方式，这种方式也比第一种方式好
//log.debug("current request: {}", request);  
```

在技术派中全局搜索一下log.info，可以看到具体的使用示例
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307222045230.png)

最后，强烈推荐看完这边文章的小伙伴，再瞅一下下面两个关联教程，借助error日志来实现邮件报警以及如何记录整个项目的请求日志、访问耗时等基本信息
1. [24、技术派异常日志报警通知](2、相关技术/24、项目/1、技术派/2、进阶篇/24、技术派异常日志报警通知.md)
2. [21、技术派中基于Filter实现请求日志记录](2、相关技术/24、项目/1、技术派/2、进阶篇/21、技术派中基于Filter实现请求日志记录.md)



















#SkyWalking #JavaAgent 

参考：[什么是JavaAgent](2、相关技术/1、Java基础/Java命令系列/什么是JavaAgent.md)

# 接入

## 准备Skywalking Agent 

下载`SkyWalking Agent`：从[Apache SkyWalking](https://skywalking.apache.org/downloads/)官方页面下载最新版本的`SkyWalking Agent`压缩包。后面会用到。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508251713649.png)

**`Agent`** 是 **==集成在应用中的轻量级探针==**，负责 **==自动采集应用性能数据==**，并 **==将数据上报给`OAP`服务==**。

**具体功能**：
- **自动埋点**：通过字节码增强技术（`Bytecode Instrumentation`），**无侵入式**地拦截应用代码（如`HTTP`请求、`RPC`调用、数据库访问等），生成分布式链路追踪（`Trace`）和指标（`Metrics`）。
- **数据采集**：收集以下数据：
	- **调用链（`Traces`）**：请求在分布式系统中的流转路径和耗时。
	- **指标（`Metrics`）**：如`JVM`性能（`CPU`、内存）、`HTTP`请求量、响应时间等。
	- **拓扑关系（Topology）**：服务之间的依赖关系。
- **数据上报**：将采集到的数据通过`gRPC/HTTP`协议发送到`OAP`服务集群。
- **本地缓存**：网络异常时临时缓存数据，保证数据可靠性。

以**独立进程**或**Java Agent**（通过`-javaagent`参数启动）的形式附着在目标应用中。支持多种语言（Java、.NET、Node.js、Go等），不同语言有对应的`Agent`实现。

## 引入依赖

```xml
<dependency>  
    <groupId>org.apache.skywalking</groupId>  
    <artifactId>apm-toolkit-logback-1.x</artifactId>  
    <version>${skywalking.version}</version>  
</dependency>  
  
<dependency>  
    <groupId>org.apache.skywalking</groupId>  
    <artifactId>apm-toolkit-trace</artifactId>  
    <version>${skywalking.version}</version>  
</dependency>
```

skywalking.version 我用的是 9.4.0

## 日志输出配置

```xml
<?xml version="1.0" encoding="UTF-8"?>  
<configuration>  
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>  
  
    <springProperty scope="context" name="app.name" source="spring.application.name"/>  
    <!-- 基于 converter -->    <conversionRule conversionWord="sensitive" converterClass="com.github.houbb.sensitive.logback.converter.SensitiveLogbackConverter" />  
  
    <!-- 添加 SkyWalking 的 TraceId 转换器 -->  
    <conversionRule conversionWord="tid"  
                    converterClass="org.apache.skywalking.apm.toolkit.log.logback.v1.x.LogbackPatternConverter"/>  
  
    <property name="APP_NAME" value="${app.name}"/>  
    <property name="LOG_PATH" value="${user.home}/${APP_NAME}/logs"/>  
    <property name="LOG_FILE" value="${LOG_PATH}/application.log"/>  
    <property name="FILE_LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%tid] %-5level [%thread] %logger{36} - %sensitive%n"/>  
  
    <appender name="APPLICATION" class="ch.qos.logback.core.rolling.RollingFileAppender">  
        <file>${LOG_FILE}</file>  
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">  
            <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.TraceIdPatternLogbackLayout">  
                <Pattern>${FILE_LOG_PATTERN}</Pattern>  
            </layout>  
        </encoder>  
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">  
            <fileNamePattern>${LOG_FILE}.%d{yyyy-MM-dd}.%i.log</fileNamePattern>  
            <maxHistory>7</maxHistory>  
            <maxFileSize>50MB</maxFileSize>  
            <totalSizeCap>20GB</totalSizeCap>  
        </rollingPolicy>  
    </appender>  
  
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">  
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">  
            <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.TraceIdPatternLogbackLayout">  
                <Pattern>${FILE_LOG_PATTERN}</Pattern>  
            </layout>  
        </encoder>  
    </appender>  
  
    <!-- 添加异步 appender，提高性能 -->  
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">  
        <discardingThreshold>0</discardingThreshold>  
        <queueSize>512</queueSize>  
        <includeCallerData>true</includeCallerData>  
        <appender-ref ref="APPLICATION"/>  
    </appender>  
  
    <root level="INFO">  
        <appender-ref ref="ASYNC"/>  
        <appender-ref ref="CONSOLE"/>  
    </root>  
</configuration>
```

添加 `SkyWalking` 的 `TraceId` 转换器
```xml
<conversionRule conversionWord="tid"   converterClass="org.apache.skywalking.apm.toolkit.log.logback.v1.x.LogbackPatternConverter"/>
```

格式化输出
```xml
<property name="FILE_LOG_PATTERN"value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%tid] %-5level [%thread] %logger{36} - %sensitive%n"/>
```

在`appender`中配置`encoder`
```xml
<encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">            <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.TraceIdPatternLogbackLayout">                <Pattern>${FILE_LOG_PATTERN}</Pattern>            </layout>  </encoder>
```

如果需要代码中强制打印`TraceId`
```java
String traceId = TraceContext.traceId();
log.info("TraceContext class: {}",TraceContext.class.getClassLoader());
log.info("Current TraceId: {}", traceId);
log.info("SpanId: {}", TraceContext.spanId());
```

## 配置探针

添加JVM参数：在启动Java应用时，需要为JVM添加以下参数来激活`SkyWalking Agent`：
```shell
-javaagent:/path/to/skywalking/agent/skywalking-agent.jar
-Dskywalking.agent.service_name=your-service-name
-Dskywalking.collector.backend_service=oap-server-ip:11800
```

- `/path/to/skywalking/agent/skywalking-agent.jar`：请替换为你的`skywalking-agent.jar`的实际路径。
- `your-service-name`：替换为你想要在`SkyWalking UI`中显示的服务名称。
- `oap-server-ip:11800`：`OAP`服务器地址及默认端口。

如果是IDEA的话，通过如下方式配置
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508251725956.png)

如：
```shell
-javaagent:/Users/whatisgod/Documents/skywalking-agent/skywalking-agent.jar
-Dskywalking.agent.service_name=nft-turbo-bussiness 
-Dskywalking.collector.backend_service=139.129.xx.xx:11800
```

## 检查和验证

启动Java应用：使用上述参数启动你的Java应用。

检查日志：检查`SkyWalking Agent`的日志，确保`Agent`成功启动并连接到`OAP`服务器。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508251727337.png)

查看SkyWalking UI：通过浏览器访问[http://localhost:8080](http://localhost:8080)，查看应用的监控数据。

一起请求之后，就可以在控制台看到一个`trace`：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508251728512.png)

# SkyWalking接入原理

我们在`Skywalking`部署的文档中介绍需要部署一个`OAP`，然后本文又要求部署一个`agent`，那么他们有啥关系，是如何协作的呢？
[【可选】Skywalking部署](2、相关技术/24、项目/3、数藏项目/2、中间件部署/【可选】Skywalking部署.md)






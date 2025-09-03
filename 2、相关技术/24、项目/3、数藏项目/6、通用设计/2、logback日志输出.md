logback是一个日志框架，通过他可以帮我们实现把日志输出到文件中，默认情况下，如果不使用logback，日志是不会输出到文件中，只会输出到控制台。

接入logback非常简单，首先需要依赖spring-boot-starter-logging，但是这个包我们一般不需要主动依赖，spring-boot-starter已经依赖他了。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202412292306635.png)

接下来我们只需要在我们的resources目录下，增加配置文件 logback-spring.xml即可，参考`NFTurbo/nft-turbo-common/nft-turbo-base/src/main/resources/logback-spring.xml`

因为这个配置基本上是通用的，所以直接放到base包下面了。

配置内容如下：
```xml
<?xml version="1.0" encoding="UTF-8"?>  
<configuration>  
  
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>  
  
    <springProperty scope="context" name="app.name" source="spring.application.name"/>  
  
    <property name="APP_NAME" value="${app.name}"/>  
    <property name="LOG_PATH" value="${user.home}/${APP_NAME}/logs"/>  
    <property name="LOG_FILE" value="${LOG_PATH}/application.log"/>  
    <property name="FILE_LOG_PATTERN" value="%d %-5level [%thread %logger - %msg%n"/>  
  
    <appender name="APPLICATION"  
              class="ch.qos.logback.core.rolling.RollingFileAppender">  
        <file>${LOG_FILE}</file>  
        <encoder>  
            <pattern>${FILE_LOG_PATTERN}</pattern>  
        </encoder>  
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">  
            <fileNamePattern>${LOG_FILE}.%d{yyyy-MM-dd}.%i.log</fileNamePattern>  
            <maxHistory>7</maxHistory>  
            <maxFileSize>50MB</maxFileSize>  
            <totalSizeCap>20GB</totalSizeCap>  
        </rollingPolicy>  
    </appender>  
  
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">  
        <encoder>  
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>  
            <charset>utf8</charset>  
        </encoder>  
    </appender>  
  
    <root level="INFO">  
        <appender-ref ref="APPLICATION"/>  
    </root>  
</configuration>
```

以下是主要配置项的详细说明：

1. include: 这一行包含了一个默认的Logback配置文件org/springframework/boot/logging/logback/defaults.xml，提供了Spring Boot项目中通用的日志配置设置。
2. springProperty: 通过springProperty标签，可以从Spring Boot的上下文中引用属性（比如spring.application.name），并给这个属性命名为app.name。
3. property: 这些行定义了多个变量，包括：
	- APP_NAME：使用了上面从Spring Boot上下文中获取的app.name属性。
	- LOG_PATH：定义了日志文件存储的路径。
	- LOG_FILE：定义了日志文件的具体名称，位于LOG_PATH指定的目录下。
	- FILE_LOG_PATTERN：定义了文件日志的格式，包括时间戳、日志级别、线程名、日志产生的类或接口名以及日志信息。
4. application appender: 这个部分配置了一个基于文件的日志处理器，包含如下设置：
	- 日志文件的具体位置。
	- 日志的格式编码器，指定使用FILE_LOG_PATTERN变量。
	- 一个基于大小和时间的滚动策略，配置包括日志文件滚动前的最大大小、保留旧日志文件的天数、文件大小上限以及总大小上限。
5. root: 定义日志的全局级别（在这里是INFO），并关联了application appender，表明所有INFO级别以上的日志都将通过APPLICATION appender处理。

通过以上配置，我们的应用日志会被输出到`~/appName/applicaiton.log`文件中。

这里面我们需要让每个应用都输出到自己的目录中，所以用了一个 `<springProperty>` 标签读取Spring的配置。

```java
 <springProperty scope="context" name="app.name" source="spring.application.name"/>
```

这样，他就可以从spring配置文件中读取配置项spring.application.name的值。


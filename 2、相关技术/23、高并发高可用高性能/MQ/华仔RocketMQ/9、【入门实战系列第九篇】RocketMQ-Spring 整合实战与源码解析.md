大家好，我是 **华仔**, 又跟大家见面了。

RocketMQ 是大家耳熟能详的消息队列，开源项目 rocketmq-spring 可以帮助开发者在 Spring Boot 项目中快速整合 RocketMQ。

这篇文章会介绍 **Spring Boot 项目使用 rocketmq-spring SDK 实现消息收发的操作流程**，同时笔者会**从开发者的角度解读 SDK 的设计逻辑**。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410222150751.png)

# **01 SDK 简介**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410222202084.png)

项目地址：[https://github.com/apache/rocketmq-spring](https://github.com/apache/rocketmq-spring)

**rocketmq-spring 的本质是一个 Spring Boot starter**。

Spring Boot 基于“**约定大于配置**”（Convention over configuration）这一理念来快速地开发、测试、运行和部署 Spring 应用，并能通过简单地与各种启动器（如 spring-boot-web-starter）结合，让应用直接以命令行的方式运行，不需再部署到独立容器中。

Spring Boot starter 构造的启动器使用起来非常方便，开发者只需要在 [pom.xml](http://pom.xml/) **引入 starter 的依赖**定义，在配置文件中**编写约定的配置**即可。

下面我们看下 rocketmq-spring-boot-starter 的配置：

## **1.1 引入依赖**

```xml
<dependency>
  <groupId>org.apache.rocketmq</groupId>
  <artifactId>rocketmq-spring-boot-starter</artifactId>
  <version>2.2.3</version>
</dependency>
```

## **1.2 约定配置**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410222209405.png)

接下来，我们分别按照生产者和消费者的顺序，详细的讲解消息收发的操作过程。

# **02 生产者**

首先我们添加依赖后，进行如下三个步骤：

## **2.1 配置文件配置**

```yml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
      group: platform-sms-server-group
    # access-key: myaccesskey
    # secret-key: mysecretkey
  topic: sms-common-topic
```

生产者配置非常简单，主要配置**名字服务地址和生产者组**。

## **2.2 需要发送消息的类中注入 RocketMQTemplate**

```java
@Autowired
private RocketMQTemplate rocketMQTemplate;

@Value("${rocketmq.topic}")
private String smsTopic;
```

## **2.3 发送消息，消息体可以是自定义对象，也可以是 Message 对象**

rocketMQTemplate 类包含多钟发送消息的方法：
1. 同步发送 syncSend
2. 异步发送 asyncSend
3. 顺序发送 syncSendOrderly
4. oneway 发送 sendOneWay

下面的代码展示如何同步发送消息。
```java
String destination = StringUtils.isBlank(tags) ? topic : topic + ":" + tags;  
SendResult sendResult =  
        rocketMQTemplate.syncSend(  
                destination,  
                MessageBuilder.withPayload(messageContent).  
                        setHeader(MessageConst.PROPERTY_KEYS, uniqueId).  
                        build()  
        );  
if (sendResult != null) {  
    if (sendResult.getSendStatus() == SendStatus.SEND_OK) {  
        // send message success ，do something  
    }  
}
```

syncSend 方法的第一个参数是发送的目标，格式是：**topic + ":" + tags** ，第二个参数是：**tspring-message 规范**的 message 对象 ，而 MessageBuilder 是一个工具类，**方法链式调用**创建消息对象。

# **03 消费者**

## **3.1 配置文件配置**

```yml
rocketmq:
  name-server: 127.0.0.1:9876
  consumer1:
    group: platform-sms-worker-common-group
    topic: sms-common-topic
```

## **3.2 实现消息监听器**

```java
@Component  
@RocketMQMessageListener(  
        consumerGroup = "${rocketmq.consumer1.group}",  //消费组  
        topic = "${rocketmq.consumer1.topic}"       //主题  
)  
public class SmsMessageCommonConsumer implements RocketMQListener<String> {  
    public void onMessage(String message) {  
        System.out.println("普通短信:" + message);  
    }  
}
```

消费者实现类也可以实现 RocketMQListener`<MessageExt>`, 在 onMessage 方法里**通过 RocketMQ 原生消息对象 MessageExt 获取更详细的消息数据**。
```java
public void onMessage(MessageExt message) {  
    try {  
        String body = new String(message.getBody(), "UTF-8");  
        logger.info("普通短信:" + message);  
    } catch (Exception e) {  
        logger.error("common onMessage error:", e);  
    }  
}
```

# **04 源码概览**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242250425.png)

  
最新源码中，我们可以看到源码中包含四个模块：
1. rocketmq-spring-boot-parent：该模块是父模块，定义项目所有依赖的 jar 包。
2. rocketmq-spring-boot：核心模块，实现了 starter 的核心逻辑。
3. rocketmq-spring-boot-starter：SDK 模块，简单封装，外部项目引用。
4. rocketmq-spring-boot-samples：示例代码模块。这个模块非常重要，当用户使用 SDK 时，可以参考示例快速开发。

# **05 starter 实现**

我们重点分析下 rocketmq-spring-boot 模块的核心源码：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242253752.png)

spring-boot-starter 实现需要包含如下三个部分：

## **5.1 定义 Spring 自身的依赖包和 RocketMQ 依赖包**

## **5.2 定义 Spring.factories 文件**

在 resources 包下创建 META-INF 目录后，新建 spring.factories 文件，并在文件中**定义自动加载类**，文件内容是：
```xml
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\

org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration
```

spring boot 会根据文件中配置的自动化配置类来自动初始化相关的 Bean、Component 或 Service。

## **5.3 实现自动加载类**

在 RocketMQAutoConfiguration 类的具体实现中，我们重点分析下生产者和消费者是如何分别启动的。

**生产者发送模板类： RocketMQTemplate**

RocketMQAutoConfiguration 类定义了两个默认的 Bean ：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242257567.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242303351.png)

首先SpringBoot项目中配置文件中的配置值会根据属性条件绑定到 RocketMQProperties 对象 中，然后使用 RocketMQ 的原生 API 分别创建生产者 Bean 和拉取消费者 Bean , 分别将两个 bean 设置到 RocketMQTemplate 对象中。

两个重点需要强调：
1. 发送消息时，将 spring-message 规范下的消息对象封装成 RocketMQ 消息对象
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242319420.png)
2. 默认拉取消费者 litePullConsumer 。拉取消费者一般用于大数据批量处理场景 。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242324201.png)
3. 原生方式
   RocketMQTemplate 类封装了拉取消费者的receive方法，以方便开发者使用。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242329477.png)

**自定义消费者类**

下图是并发消费者的例子：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242331469.png)

消费者示例代码

那么 rocketmq-spring 是如何自动启动消费者呢 ？
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242334253.png)
  
spring 容器首先注册了消息监听器**后置处理器**，然后调用 ListenerContainerConfiguration 类的 registerContainer 方法 。

对比并发消费者的例子，我们可以看到：DefaultRocketMQListenerContainer 是对 DefaultMQPushConsumer 消费逻辑的封装。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242345027.png)

封装**消费消息的逻辑**，同时**定满足 RocketMQListener 泛化接口支持不同参数**，比如 String 、MessageExt 、自定义对象 。

首先DefaultRocketMQListenerContainer初始化之后， 获取 onMessage 方法的参数类型 。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242346119.png)

然后消费者调用 consumeMessage 处理消息时，封装了一个 handleMessage 方法 ，将原生 RocketMQ 消息对象 MessageExt 转换成 onMessage 方法定义的参数对象，然后调用 rocketMQListener 的 onMessage  方法。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410242346751.png)

上图右侧标红的代码也就是该方法的精髓：

```java
rocketMQListener.onMessage(doConvertMessage(messageExt));
```

# **06 写到最后**

开源项目 rocketmq-spring 有很多值得学习的地方 ，我们可以从如下四个层面逐层进阶：
1. **学会如何使用**：参考 rocketmq-spring-boot-samples 模块的示例代码，学会如何发送和接收消息，快速编码；
2. **模块设计**：学习项目的模块分层 （父模块、SDK 模块、核心实现模块、示例代码模块）；
3. **starter 设计思路** ：定义自动配置文件 spring.factories 、设计配置属性类 、在 RocketMQ client 的基础上实现优雅的封装、深入理解 RocketMQ 源码等；
4. **举一反三**：当我们理解了 rocketmq-spring 的源码，我们可以尝试模仿该项目写一个简单的 spring boot starter。

  

原文连接：[https://mp.weixin.qq.com/s/CxKmKqkMwKfVZLsf8TPKlQ](https://mp.weixin.qq.com/s/CxKmKqkMwKfVZLsf8TPKlQ)



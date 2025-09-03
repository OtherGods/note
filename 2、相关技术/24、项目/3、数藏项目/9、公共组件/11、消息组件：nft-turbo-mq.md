![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503052318107.png)

我们的项目在多个微服务中都需要进行消息的发送与接收，我们把消息相关的内容封装到消息组件中。主要是封装了 rokect-mq
```xml
<?xml version="1.0" encoding="UTF-8"?>  
<project xmlns="http://maven.apache.org/POM/4.0.0"  
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">  
    <modelVersion>4.0.0</modelVersion>  
    <parent>  
        <groupId>cn.hollis</groupId>  
        <artifactId>nft-turbo-common</artifactId>  
        <version>1.0.0-SNAPSHOT</version>  
    </parent>  
  
    <artifactId>nft-turbo-mq</artifactId>  
  
    <properties>  
        <maven.compiler.source>21</maven.compiler.source>  
        <maven.compiler.target>21</maven.compiler.target>  
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    </properties>  
  
    <dependencies>  
  
        <dependency>  
            <groupId>com.alibaba.cloud</groupId>  
            <artifactId>spring-cloud-starter-stream-rocketmq</artifactId>  
        </dependency>  
  
    </dependencies>  
  
</project>
```

同时定义了一个strem.yml 把消息的通用配置放在里面
```yml
spring:  
  cloud:  
    stream:  
      rocketmq:  
        binder:  
          # rocketmq 地址  
          name-server: 116.62.53.29:9876
```

个性化的配置放在相关引用的moudel里面，比如在collection 的module里面进行了使用
```yml
spring:  
  cloud:  
    stream:  
      rocketmq:  
        bindings:  
          chain-in-0: # 定义一个输入绑定（即消费者）  
            consumer:  
              subscription:  
                expression: 'COLLECTION_MINT || COLLECTION_CHAIN' # 定义消费者订阅的tag（标签），可以指定多个，使用"||"逻辑或分隔  
      bindings:  
        chain-in-0: # 输入绑定的详细配置  
          content-type: application/json # 消息内容的类型，这里设为JSON格式  
          destination: chain-result-topic # 指定订阅的RocketMQ主题名称  
          group: chain-group # 消费者组名称，具有相同group的消费者将会以负载均衡方式消费消息  
          binder: rocketmq # 指明使用的消息中间件类型是RocketMQ  
        orderClose-in-0: # 另一个输入绑定的配置，表示另一个消费者  
          content-type: application/json # 同样设定消息类型为JSON  
          destination: order-close-topic # 指定订阅的主题名称  
          group: collection-group # 消费者组名称  
          binder: rocketmq # 指定使用RocketMQ作为消息中间件
```

在Spring Cloud Stream中：
- destination：表示消息通道所对应的外部中间件的主题或队列。
- group：表示消费者所属的组的名称。在同一组内的消费者将以负载均衡的方式消费消息，即同一消息只会被组内的一个消费者消费。
- binder：指的是实现绑定功能的中间件，可以是Kafka、RabbitMQ、RocketMQ等。
- content-type：表示消息的内容类型，常用的为application/json。
- subscription：在RocketMQ中，通过expression设置Tag过滤，选择只消费带有特定Tag的消息。

这段配置将会创建两个消息消费者，一个订阅chain-result-topic主题的chain-in-0，另一个订阅order-close-topic主题的orderClose-in-0。

同时，为了方便使用，我们自定义了 bean——streamProducer：
```java
package cn.hollis.turbo.stream.config;  
  
import cn.hollis.turbo.stream.producer.StreamProducer;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
  
/**  
 * @author Hollis 
 * 
 */
@Configuration  
public class StreamConfiguration {  
    @Bean  
    public StreamProducer streamProducer() {  
        StreamProducer streamProducer = new StreamProducer();  
        return streamProducer;  
    }  
      
}
```

```java
package cn.hollis.turbo.stream.producer;  
  
import java.util.UUID;  
  
import cn.hollis.turbo.stream.param.MessageBody;  
import com.alibaba.fastjson.JSON;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.cloud.stream.function.StreamBridge;  
import org.springframework.messaging.support.MessageBuilder;  
  
public class StreamProducer {
      
    private static Logger logger = LoggerFactory.getLogger(StreamProducer.class);  
      
    @Autowired  
    private StreamBridge streamBridge;  
      
    public boolean send(String bingingName, String tag, String msg) {  
        // 构建消息对象  
        MessageBody message = new MessageBody()  
                .setIdentifier(UUID.randomUUID().toString())  
                .setBody(msg);  
        logger.info("send message : {} , {}", bingingName, JSON.toJSONString(message));  
        boolean result = streamBridge.send(bingingName, MessageBuilder.withPayload(message).setHeader("TAGS", tag)  
                .build());  
        logger.info("send result : {} , {}", bingingName, result);  
        return result;  
    }  
      
    public boolean send(String bingingName, String tag, String msg, String headerKey, String headerValue) {  
        // 构建消息对象  
        MessageBody message = new MessageBody()  
                .setIdentifier(UUID.randomUUID().toString())  
                .setBody(msg);  
        logger.info("send message : {} , {}", bingingName, JSON.toJSONString(message));  
        boolean result = streamBridge.send(bingingName, MessageBuilder.withPayload(message).setHeader("TAGS", tag).setHeader(headerKey, headerValue)  
                .build());  
        logger.info("send result : {} , {}", bingingName, result);  
        return result;  
    }  
}
```

并且新建org.springframework.boot.autoconfigure.AutoConfiguration.imports，内容如下：

```
cn.hollis.turbo.stream.config.StreamConfiguration
```



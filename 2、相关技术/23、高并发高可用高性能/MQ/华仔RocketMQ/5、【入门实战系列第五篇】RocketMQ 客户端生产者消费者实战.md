本文以 阿里云 RocketMQ 版本的 HTTP 协议为例，对应 TCP 协议的，可以参考相关文档。

[云消息队列 RocketMQ 4.x系列](https://help.aliyun.com/zh/apsaramq-for-rocketmq/cloud-message-queue-rocketmq-4-x-series/?spm=a2c4g.255810.0.0.5cf5584frn7sKQ)

大家好，我是 **华仔**, 又跟大家见面了。

今天我们来看下 **RocketMQ** **相关语言客户端** 的简单使用和场景说明。

# **00 RocketMQ 客户端介绍**

本文以 阿里云 RocketMQ 版本的 HTTP 协议为例，对应 TCP 协议的，可以参考相关文档。

[4.0 系列实例文档](https://help.aliyun.com/document_detail/445491.html?spm=a2c4g.255810.0.0.5cf5584frn7sKQ)、[5.0 系列实例文档](https://help.aliyun.com/document_detail/445489.html?spm=a2c4g.201502.0.0.1f4f112b0cYr9c)

# **01 Java 客户端**

在使用 Java SDK 收发消息前，您需按照下面提供的内容来准备环境。

## **1.1 环境要求**

1. 安装 JDK 1.6 或以上版本。更多信息，请参见 [安装JDK](https://www.oracle.com/java/technologies/javase-downloads.html?spm=a2c4g.11186623.2.4.26e8598ax6300E)。
2. 安装 Maven。更多信息，请参见 [安装Maven](https://maven.apache.org/download.cgi?spm=a2c4g.11186623.2.5.26e8598ax6300E&file=download.cgi)。

## **1.2 安装 Java SDK**

通过Maven方式引入依赖，在[pom.xml](http://pom.xml/)中添加以下依赖。
```xml
<dependency>  
    <groupId>com.aliyun.mq</groupId>  
    <artifactId>mq-http-sdk</artifactId>  
    <!--以下版本号请替换为Java SDK的最新版本号-->  
    <version>1.0.3.2</version>  
    <classifier>jar-with-dependencies</classifier>  
</dependency>
```

## **1.3 收发普通消息**

普通消息是指消息队列 RocketMQ 版中无特性的消息，区别于有特性的定时和延时消息、顺序消息和事务消息。下面提供使用 HTTP 协议下的 Java SDK 收发普通消息的示例代码。
```java
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQProducer;  
import com.aliyun.mq.http.model.TopicMessage;  
import java.util.Date;  
  
public class Producer {  
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        // 不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        final String instanceId = "${INSTANCE_ID}";  
        // 获取Topic的生产者。  
        MQProducer producer;  
        if (instanceId != null && instanceId != "") {  
            producer = mqClient.getProducer(instanceId, topic);  
        } else {  
            producer = mqClient.getProducer(topic);  
        }  
        try {  
            // 循环发送4条消息。  
            for (int i = 0; i < 4; i++) {  
                TopicMessage pubMsg;        // 普通消息。  
                pubMsg = new TopicMessage(  
                        // 消息内容。  
                        "hello mq!".getBytes(),  
                        // 消息标签。  
                        "A"  
                );  
                // 设置消息的自定义属性。  
                pubMsg.getProperties().put("a", String.valueOf(i));  
                // 设置消息的Key。  
                pubMsg.setMessageKey("MessageKey");  
                  
                // 同步发送消息，只要不抛异常就是成功。  
                TopicMessage pubResultMsg = producer.publishMessage(pubMsg);  
                // 同步发送消息，只要不抛异常就是成功。  
                System.out.println(new Date() + " Send mq message success. Topic is:" + topic + ", msgId is: " + pubResultMsg.getMessageId()  
                        + ", bodyMD5 is: " + pubResultMsg.getMessageBodyMD5());  
            }  
        } catch (Throwable e) {  
            // 消息发送失败，需要进行重试处理，可重新发送这条消息或持久化这条数据进行补偿处理。  
            System.out.println(new Date() + " Send mq message failed. Topic is:" + topic);  
            e.printStackTrace();  
        }  
        mqClient.close();  
    }  
}
```

消费普通消息的示例代码如下：
```java
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQConsumer;  
import com.aliyun.mq.http.common.AckMessageException;  
import com.aliyun.mq.http.model.Message;  
import java.util.ArrayList;  
import java.util.List;  
  
public class Consumer {  
      
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        //不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // 您在消息队列RocketMQ版控制台创建的Group ID。  
        final String groupId = "${GROUP_ID}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        final String instanceId = "${INSTANCE_ID}";  
          
        final MQConsumer consumer;  
        if (instanceId != null && instanceId != "") {  
            consumer = mqClient.getConsumer(instanceId, topic, groupId, null);  
        } else {  
            consumer = mqClient.getConsumer(topic, groupId);  
        }  
          
        // 在当前线程循环消费消息，建议多开个几个线程并发消费消息。  
        do {  
            List<Message> messages = null;  
              
            try {  
                // 长轮询消费消息。  
                // 长轮询表示如果Topic没有消息,则请求会在服务端挂起3s，3s内如果有消息可以消费则立即返回客户端。  
                messages = consumer.consumeMessage(  
                        3,// 一次最多消费3条消息（最多可设置为16条）。  
                        3// 长轮询时间3秒（最多可设置为30秒）。  
                );  
            } catch (Throwable e) {  
                e.printStackTrace();  
                try {  
                    Thread.sleep(2000);  
                } catch (InterruptedException e1) {  
                    e1.printStackTrace();  
                }  
            }  
            // Topic中没有消息可消费。  
            if (messages == null || messages.isEmpty()) {  
                System.out.println(Thread.currentThread().getName() + ": no new message, continue!");  
                continue;  
            }  
              
            // 处理业务逻辑。  
            for (Message message : messages) {  
                System.out.println("Receive message: " + message);  
            }  
              
            // 消息重试时间到达前若不确认消息消费成功，则消息会被重复消费。  
            // 消息句柄有时间戳，同一条消息每次消费的时间戳都不一样。  
            {  
                List<String> handles = new ArrayList<String>();  
                for (Message message : messages) {  
                    handles.add(message.getReceiptHandle());  
                }  
                try {  
                    consumer.ackMessage(handles);  
                } catch (Throwable e) {  
                    // 某些消息的句柄可能超时，会导致消息消费状态确认不成功。  
                    if (e instanceof AckMessageException) {  
                        AckMessageException errors = (AckMessageException) e;  
                        System.out.println("Ack message fail, requestId is:" + errors.getRequestId() + ", fail handles:");  
                        if (errors.getErrorMessages() != null) {  
                            for (String errorHandle :errors.getErrorMessages().keySet()) {  
                                System.out.println("Handle:" + errorHandle + ", ErrorCode:" + errors.getErrorMessages().get(errorHandle).getErrorCode()  
                                        + ", ErrorMsg:" + errors.getErrorMessages().get(errorHandle).getErrorMessage());  
                            }  
                        }  
                        continue;  
                    }  
                    e.printStackTrace();  
                }  
            }  
        } while (true);  
    }  
}
```  

## **1.4 收发顺序消息**

顺序消息（FIFO消息）是消息队列 RocketMQ 版提供的一种严格按照顺序来发布和消费的消息类型。下面提供使用HTTP 协议下的 Java SDK 收发顺序消息的示例代码。

发送顺序消息的示例代码如下：
```java
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQProducer;  
import com.aliyun.mq.http.model.TopicMessage;  
import java.util.Date;  
  
public class OrderProducer {  
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        // 不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        final String instanceId = "${INSTANCE_ID}";  
          
        // 获取Topic的生产者。  
        MQProducer producer;  
        if (instanceId != null && instanceId != "") {  
            producer = mqClient.getProducer(instanceId, topic);  
        } else {  
            producer = mqClient.getProducer(topic);  
        }  
          
        try {  
            // 循环发送8条消息。  
            for (int i = 0; i < 8; i++) {  
                TopicMessage pubMsg = new TopicMessage(  
                        // 消息内容。  
                        "hello mq!".getBytes(),  
                        // 消息标签。  
                        "A"  
                );  
                // 设置分区顺序消息的Sharding Key，用于标识不同的分区。Sharding Key与消息的Key是完全不同的概念。  
                pubMsg.setShardingKey(String.valueOf(i % 2));  
                pubMsg.getProperties().put("a", String.valueOf(i));  
                // 同步发送消息，只要不抛异常就是成功。  
                TopicMessage pubResultMsg = producer.publishMessage(pubMsg);  
                  
                // 同步发送消息，只要不抛异常就是成功。  
                System.out.println(new Date() + " Send mq message success. Topic is:" + topic + ", msgId is: " + pubResultMsg.getMessageId()  
                        + ", bodyMD5 is: " + pubResultMsg.getMessageBodyMD5());  
            }  
        } catch (Throwable e) {  
            // 消息发送失败，需要进行重试处理，可重新发送这条消息或持久化这条数据进行补偿处理。  
            System.out.println(new Date() + " Send mq message failed. Topic is:" + topic);  
            e.printStackTrace();  
        }  
        mqClient.close();  
    }  
}
```  

消费顺序消息的示例代码如下：
```java
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQConsumer;  
import com.aliyun.mq.http.common.AckMessageException;  
import com.aliyun.mq.http.model.Message;  
import java.util.ArrayList;  
import java.util.List;  
  
public class OrderConsumer {  
      
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        //不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // 您在消息队列RocketMQ版控制台创建的Group ID。  
        final String groupId = "${GROUP_ID}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        final String instanceId = "${INSTANCE_ID}";  
          
        final MQConsumer consumer;  
        if (instanceId != null && instanceId != "") {  
            consumer = mqClient.getConsumer(instanceId, topic, groupId, null);  
        } else {  
            consumer = mqClient.getConsumer(topic, groupId);  
        }  
          
        // 在当前线程循环消费消息，建议多开个几个线程并发消费消息。  
        do {  
            List<Message> messages = null;  
              
            try {  
                // 长轮询顺序消费消息, 拉取到的消息可能是多个分区的（对于分区顺序消息），一个分区内的消息一定是顺序的。  
                // 对于分区顺序消息，只要一个分区内存在没有被确认消费的消息，那么该分区下次还会消费到相同的消息。  
                // 对于一个分区，只有所有消息确认消费成功才能消费下一批消息。  
                // 长轮询表示如果Topic没有消息，则请求会在服务端挂起3s，3s内如果有消息可以消费则服务端立即返回响应。  
                messages = consumer.consumeMessageOrderly(  
                        3,  // 一次最多消费3条消息（最多可设置为16条）。  
                        3   // 长轮询时间为3秒（最多可设置为30秒）。  
                );  
            } catch (Throwable e) {  
                e.printStackTrace();  
                try {  
                    Thread.sleep(2000);  
                } catch (InterruptedException e1) {  
                    e1.printStackTrace();  
                }  
            }  
            // Topic中没有消息可消费。  
            if (messages == null || messages.isEmpty()) {  
                System.out.println(Thread.currentThread().getName() + ": no new message, continue!");  
                continue;  
            }  
              
            // 处理业务逻辑。  
            System.out.println("Receive " + messages.size() + " messages:");  
            for (Message message : messages) {  
                System.out.println(message);  
                System.out.println("ShardingKey: " + message.getShardingKey() + ", a:" + message.getProperties().get("a"));  
            }  
              
            // 消息重试时间到达前若不确认消息消费成功，则消息会被重复消费。  
            // 消息句柄有时间戳，同一条消息每次消费拿到的都不一样。  
            {  
                List<String> handles = new ArrayList<String>();  
                for (Message message : messages) {  
                    handles.add(message.getReceiptHandle());  
                }  
                  
                try {  
                    consumer.ackMessage(handles);  
                } catch (Throwable e) {  
                    // 某些消息的句柄可能超时，会导致消息消费状态确认不成功。  
                    if (e instanceof AckMessageException) {  
                        AckMessageException errors = (AckMessageException) e;  
                        System.out.println("Ack message fail, requestId is:" + errors.getRequestId() + ", fail handles:");  
                        if (errors.getErrorMessages() != null) {  
                            for (String errorHandle :errors.getErrorMessages().keySet()) {  
                                System.out.println("Handle:" + errorHandle + ", ErrorCode:" + errors.getErrorMessages().get(errorHandle).getErrorCode()  
                                        + ", ErrorMsg:" + errors.getErrorMessages().get(errorHandle).getErrorMessage());  
                            }  
                        }  
                        continue;  
                    }  
                    e.printStackTrace();  
                }  
            }  
        } while (true);  
    }  
}
```

## **1.5 收发定时消息和延时消息**

下面提供使用 HTTP 协议下的 Java SDK 收发定时消息和延时消息的示例代码。

发送定时消息或延时消息的示例代码如下：
```java
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQProducer;  
import com.aliyun.mq.http.model.TopicMessage;  
import java.util.Date;  
  
public class Producer {  
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        //不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        final String instanceId = "${INSTANCE_ID}";  
          
        // 获取Topic的生产者。  
        MQProducer producer;  
        if (instanceId != null && instanceId != "") {  
            producer = mqClient.getProducer(instanceId, topic);  
        } else {  
            producer = mqClient.getProducer(topic);  
        }  
          
        try {  
            // 循环发送4条消息。  
            for (int i = 0; i < 4; i++) {  
                TopicMessage pubMsg;  
                pubMsg = new TopicMessage(  
                        // 消息内容。  
                        "hello mq!".getBytes(),  
                        // 消息标签。  
                        "A"  
                );  
                // 设置消息的自定义属性。  
                pubMsg.getProperties().put("a", String.valueOf(i));  
                // 设置消息的Key。  
                pubMsg.setMessageKey("MessageKey");  
                // 延时消息，发送时间为10s后。该参数格式为毫秒级别的时间戳。  
                // 若发送定时消息，设置该参数时需要计算定时时间与当前时间的时间差。  
                pubMsg.setStartDeliverTime(System.currentTimeMillis() + 10 * 1000);  
                  
                // 同步发送消息，只要不抛异常就是成功。  
                TopicMessage pubResultMsg = producer.publishMessage(pubMsg);  
                  
                // 同步发送消息，只要不抛异常就是成功。  
                System.out.println(new Date() + " Send mq message success. Topic is:" + topic + ", msgId is: " + pubResultMsg.getMessageId()  
                        + ", bodyMD5 is: " + pubResultMsg.getMessageBodyMD5());  
            }  
        } catch (Throwable e) {  
            // 消息发送失败，需要进行重试处理，可重新发送这条消息或持久化这条数据进行补偿处理。  
            System.out.println(new Date() + " Send mq message failed. Topic is:" + topic);  
            e.printStackTrace();  
        }  
        mqClient.close();  
    }  
}
```

消费定时消息或延时消息的示例代码如下：
```java
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQConsumer;  
import com.aliyun.mq.http.common.AckMessageException;  
import com.aliyun.mq.http.model.Message;  
import java.util.ArrayList;  
import java.util.List;  
  
public class Consumer {  
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        //不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // 您在消息队列RocketMQ版控制台创建的Group ID。  
        final String groupId = "${GROUP_ID}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        final String instanceId = "${INSTANCE_ID}";  
          
        final MQConsumer consumer;  
        if (instanceId != null && instanceId != "") {  
            consumer = mqClient.getConsumer(instanceId, topic, groupId, null);  
        } else {  
            consumer = mqClient.getConsumer(topic, groupId);  
        }  
          
        // 在当前线程循环消费消息，建议多开个几个线程并发消费消息。  
        do {  
            List<Message> messages = null;  
              
            try {  
                // 长轮询消费消息。  
                // 长轮询表示如果Topic没有消息,则请求会在服务端挂起3s，3s内如果有消息可以消费则立即返回客户端。  
                messages = consumer.consumeMessage(  
                        3,// 一次最多消费3条消息（最多可设置为16条）。  
                        3// 长轮询时间3秒（最多可设置为30秒）。  
                );  
            } catch (Throwable e) {  
                e.printStackTrace();  
                try {  
                    Thread.sleep(2000);  
                } catch (InterruptedException e1) {  
                    e1.printStackTrace();  
                }  
            }  
            // Topic中没有消息可消费。  
            if (messages == null || messages.isEmpty()) {  
                System.out.println(Thread.currentThread().getName() + ": no new message, continue!");  
                continue;  
            }  
              
            // 处理业务逻辑。  
            for (Message message : messages) {  
                System.out.println("Receive message: " + message);  
            }  
              
            // 消息重试时间到达前若不确认消息消费成功，则消息会被重复消费。  
            // 消息句柄有时间戳，同一条消息每次消费的时间戳都不一样。  
            {  
                List<String> handles = new ArrayList<String>();  
                for (Message message : messages) {  
                    handles.add(message.getReceiptHandle());  
                }  
                  
                try {  
                    consumer.ackMessage(handles);  
                } catch (Throwable e) {  
                    // 某些消息的句柄可能超时，会导致消息消费状态确认不成功。  
                    if (e instanceof AckMessageException) {  
                        AckMessageException errors = (AckMessageException) e;  
                        System.out.println("Ack message fail, requestId is:" + errors.getRequestId() + ", fail handles:");  
                        if (errors.getErrorMessages() != null) {  
                            for (String errorHandle :errors.getErrorMessages().keySet()) {  
                                System.out.println("Handle:" + errorHandle + ", ErrorCode:" + errors.getErrorMessages().get(errorHandle).getErrorCode()  
                                        + ", ErrorMsg:" + errors.getErrorMessages().get(errorHandle).getErrorMessage());  
                            }  
                        }  
                        continue;  
                    }  
                    e.printStackTrace();  
                }  
            }  
        } while (true);  
    }  
}
```

## **1.6 收发事务消息**

消息队列 RocketMQ 版提供类似 XA 或 Open XA 的分布式事务功能，通过消息队列 RocketMQ 版事务消息，能达到分布式事务的最终一致。下面提供使用 HTTP 协议下的 Java SDK 收发事务消息的示例代码。

事务消息的交互流程如下图所示：
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410112326216.png)

发送事务消息的示例代码如下：
```java
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQTransProducer;  
import com.aliyun.mq.http.common.AckMessageException;  
import com.aliyun.mq.http.model.Message;  
import com.aliyun.mq.http.model.TopicMessage;  
  
import java.util.List;  
  
public class TransProducer {  
      
      
    static void processCommitRollError(Throwable e) {  
        if (e instanceof AckMessageException) {  
            AckMessageException errors = (AckMessageException) e;  
            System.out.println("Commit/Roll transaction error, requestId is:" + errors.getRequestId() + ", fail handles:");  
            if (errors.getErrorMessages() != null) {  
                for (String errorHandle :errors.getErrorMessages().keySet()) {  
                    System.out.println("Handle:" + errorHandle + ", ErrorCode:" + errors.getErrorMessages().get(errorHandle).getErrorCode()  
                            + ", ErrorMsg:" + errors.getErrorMessages().get(errorHandle).getErrorMessage());  
                }  
            }  
        }  
    }  
      
    public static void main(String[] args) throws Throwable {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        // 不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        final String instanceId = "${INSTANCE_ID}";  
        // 您在消息队列RocketMQ版控制台创建的Group ID。  
        final String groupId = "${GROUP_ID}";  
          
        final MQTransProducer mqTransProducer = mqClient.getTransProducer(instanceId, topic, groupId);  
          
        for (int i = 0; i < 4; i++) {  
            TopicMessage topicMessage = new TopicMessage();  
            topicMessage.setMessageBody("trans_msg");  
            topicMessage.setMessageTag("a");  
            topicMessage.setMessageKey(String.valueOf(System.currentTimeMillis()));  
            // 设置事务第一次回查的时间，为相对时间。单位：秒，取值范围：10~300。  
            // 第一次事务回查后如果消息没有提交或者回滚，则之后每隔10s左右会回查一次，共回查24小时。  
            topicMessage.setTransCheckImmunityTime(10);  
            topicMessage.getProperties().put("a", String.valueOf(i));  
              
            TopicMessage pubResultMsg = null;  
            pubResultMsg = mqTransProducer.publishMessage(topicMessage);  
            System.out.println("Send---->msgId is: " + pubResultMsg.getMessageId()  
                    + ", bodyMD5 is: " + pubResultMsg.getMessageBodyMD5()  
                    + ", Handle: " + pubResultMsg.getReceiptHandle()  
            );  
            if (pubResultMsg != null && pubResultMsg.getReceiptHandle() != null) {  
                if (i == 0) {  
                    // 发送完事务消息后能获取到半消息句柄，可以直接提交或回滚事务消息。  
                    try {  
                        mqTransProducer.commit(pubResultMsg.getReceiptHandle());  
                        System.out.println(String.format("MessageId:%s, commit", pubResultMsg.getMessageId()));  
                    } catch (Throwable e) {  
                        // 如果提交或回滚事务消息时超过了TransCheckImmunityTime（针对发送事务消息的句柄）设置的时长，则会失败。  
                        if (e instanceof AckMessageException) {  
                            processCommitRollError(e);  
                            continue;  
                        }  
                    }  
                }  
            }  
        }  
          
        // 客户端需要有一个线程或者进程来消费没有确认的事务消息。  
        // 启动一个线程来检查没有确认的事务消息。  
        Thread t = new Thread(new Runnable() {  
            public void run() {  
                int count = 0;  
                while(true) {  
                    try {  
                        if (count == 3) {  
                            break;  
                        }  
                        List<Message> messages = mqTransProducer.consumeHalfMessage(3, 3);  
                        if (messages == null) {  
                            System.out.println("No Half message!");  
                            continue;  
                        }  
                        System.out.println(String.format("Half---->MessageId:%s,Properties:%s,Body:%s,Latency:%d",  
                                messages.get(0).getMessageId(),  
                                messages.get(0).getProperties(),  
                                messages.get(0).getMessageBodyString(),  
                                System.currentTimeMillis() - messages.get(0).getPublishTime()));  
                          
                        for (Message message : messages) {  
                            try {  
                                if (Integer.valueOf(message.getProperties().get("a")) == 1) {  
                                    // 确认提交事务消息。  
                                    mqTransProducer.commit(message.getReceiptHandle());  
                                    count++;  
                                    System.out.println(String.format("MessageId:%s, commit", message.getMessageId()));  
                                } else if (Integer.valueOf(message.getProperties().get("a")) == 2  
                                        && message.getConsumedTimes() > 1) {  
                                    // 确认提交事务消息。  
                                    mqTransProducer.commit(message.getReceiptHandle());  
                                    count++;  
                                    System.out.println(String.format("MessageId:%s, commit", message.getMessageId()));  
                                } else if (Integer.valueOf(message.getProperties().get("a")) == 3) {  
                                    // 确认回滚事务消息。  
                                    mqTransProducer.rollback(message.getReceiptHandle());  
                                    count++;  
                                    System.out.println(String.format("MessageId:%s, rollback", message.getMessageId()));  
                                } else {  
                                    // 什么都不做，下次再检查。  
                                    System.out.println(String.format("MessageId:%s, unknown", message.getMessageId()));  
                                }  
                            } catch (Throwable e) {  
                                // 如果提交或回滚消息时超过了TransCheckImmunityTime（针对发送事务消息的句柄）或者超过10s（针对consumeHalfMessage的句柄）则会失败。  
                                processCommitRollError(e);  
                            }  
                        }  
                    } catch (Throwable e) {  
                        System.out.println(e.getMessage());  
                    }  
                }  
            }  
        });  
          
        t.start();  
          
        t.join();  
          
        mqClient.close();  
    }  
}
```

消费事务消息的示例代码如下：
```java
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQConsumer;  
import com.aliyun.mq.http.common.AckMessageException;  
import com.aliyun.mq.http.model.Message;  
  
import java.util.ArrayList;  
import java.util.List;  
  
public class Consumer {  
      
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        //不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // 您在消息队列RocketMQ版控制台创建的Group ID。  
        final String groupId = "${GROUP_ID}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        final String instanceId = "${INSTANCE_ID}";  
          
        final MQConsumer consumer;  
        if (instanceId != null && instanceId != "") {  
            consumer = mqClient.getConsumer(instanceId, topic, groupId, null);  
        } else {  
            consumer = mqClient.getConsumer(topic, groupId);  
        }  
          
        // 在当前线程循环消费消息，建议多开个几个线程并发消费消息。  
        do {  
            List<Message> messages = null;  
              
            try {  
                // 长轮询消费消息。  
                // 长轮询表示如果Topic没有消息,则请求会在服务端挂起3s，3s内如果有消息可以消费则立即返回客户端。  
                messages = consumer.consumeMessage(  
                        3,// 一次最多消费3条消息（最多可设置为16条）。  
                        3// 长轮询时间3秒（最多可设置为30秒）。  
                );  
            } catch (Throwable e) {  
                e.printStackTrace();  
                try {  
                    Thread.sleep(2000);  
                } catch (InterruptedException e1) {  
                    e1.printStackTrace();  
                }  
            }  
            // Topic中没有消息可消费。  
            if (messages == null || messages.isEmpty()) {  
                System.out.println(Thread.currentThread().getName() + ": no new message, continue!");  
                continue;  
            }  
              
            // 处理业务逻辑。  
            for (Message message : messages) {  
                System.out.println("Receive message: " + message);  
            }  
              
            // 消息重试时间到达前若不确认消息消费成功，则消息会被重复消费。  
            // 消息句柄有时间戳，同一条消息每次消费的时间戳都不一样。  
            {  
                List<String> handles = new ArrayList<String>();  
                for (Message message : messages) {  
                    handles.add(message.getReceiptHandle());  
                }  
                  
                try {  
                    consumer.ackMessage(handles);  
                } catch (Throwable e) {  
                    // 某些消息的句柄可能超时，会导致消息消费状态确认不成功。  
                    if (e instanceof AckMessageException) {  
                        AckMessageException errors = (AckMessageException) e;  
                        System.out.println("Ack message fail, requestId is:" + errors.getRequestId() + ", fail handles:");  
                        if (errors.getErrorMessages() != null) {  
                            for (String errorHandle :errors.getErrorMessages().keySet()) {  
                                System.out.println("Handle:" + errorHandle + ", ErrorCode:" + errors.getErrorMessages().get(errorHandle).getErrorCode()  
                                        + ", ErrorMsg:" + errors.getErrorMessages().get(errorHandle).getErrorMessage());  
                            }  
                        }  
                        continue;  
                    }  
                    e.printStackTrace();  
                }  
            }  
        } while (true);  
    }  
}
```


# **02 PHP 客户端**

在使用 PHP SDK 收发消息前，您需按照下面提供的内容来准备环境。

## **2.1 环境要求**
1. 安装PHP 5.5.0或以上版本，更多信息，请参见 [安装PHP](https://www.php.net/manual/install.php)。
2. 重要对于 PHP 版本小于 7.2.5 的运行环境，需要将 Composer 依赖降低到 2.2.x 或以下版本。
3. 安装 Composer，更多信息，请参见 [安装Composer](https://getcomposer.org/download/?spm=a2c4g.11186623.2.5.623f68b1vzOYhf)。

安装完成后，您可以执行 php -v 命令查看PHP语言版本。

## **2.2 安装 SDK**

执行以下步骤安装PHP SDK。

1. 在您PHP安装目录下的 [composer.json](http://composer.json/) 文件中加入以下依赖：
```json
{
	"require": {
		"aliyunmq/mq-http-sdk": ">=1.0.4"
	}
}
```
2. 执行以下命令，通过Composer安装依赖。
```shell
composer install
```

等集成好 SDK 后，我们就可以开始正式使用了，这里分别讲几种消息的使用场景。

## **2.3 收发普通消息**

普通消息是指消息队列 RocketMQ 版中无特性的消息，区别于有特性的定时和延时消息、顺序消息和事务消息。下面提供使用 HTTP 协议下的 PHP SDK 收发普通消息的示例代码。

发送普通消息的示例代码如下：
```PHP
<?php  
require "vendor/autoload.php";  
use MQ\Model\TopicMessage;  
use MQ\MQClient;  
  
class ProducerTest  
{  
    private $client;  
    private $producer;  
      
    public function __construct()  
    {  
        $this->client = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        $topic = "${TOPIC}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        $instanceId = "${INSTANCE_ID}";  
          
        $this->producer = $this->client->getProducer($instanceId, $topic);  
    }  
      
    public function run()  
    {  
        try  
        {  
            for ($i=1; $i<=4; $i++)  
            {  
                $publishMessage = new TopicMessage(  
                        // 消息内容。  
                        "hello mq!"  
                );  
                // 设置消息的自定义属性。  
                $publishMessage->putProperty("a", $i);  
                // 设置消息的Key。  
                $publishMessage->setMessageKey("MessageKey");  
                $result = $this->producer->publishMessage($publishMessage);  
                print "Send mq message success. msgId is:" . $result->getMessageId() . ", bodyMD5 is:" . $result->getMessageBodyMD5() . "\n";  
            }  
        } catch (\Exception $e) {  
        print_r($e->getMessage() . "\n");  
    }  
    }  
}  
  
$instance = new ProducerTest();  
$instance->run();  
?>
```

消费普通消息的示例代码如下：
```php
<?php  
require "vendor/autoload.php";  
  
use MQ\MQClient;  
  
class ConsumerTest  
{  
    private $client;  
    private $consumer;  
      
    public function __construct()  
    {  
        $this->client = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        $topic = "${TOPIC}";  
        // 您在消息队列RocketMQ版控制台创建的Group ID。  
        $groupId = "${GROUP_ID}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        $instanceId = "${INSTANCE_ID}";  
        $this->consumer = $this->client->getConsumer($instanceId, $topic, $groupId);  
    }  
      
    public function ackMessages($receiptHandles)  
    {  
        try {  
            $this->consumer->ackMessage($receiptHandles);  
        } catch (\Exception $e) {  
        if ($e instanceof MQ\Exception\AckMessageException) {  
            // 某些消息的句柄可能超时，会导致消费确认失败。  
            printf("Ack Error, RequestId:%s\n", $e->getRequestId());  
            foreach ($e->getAckMessageErrorItems() as $errorItem) {  
                printf("\tReceiptHandle:%s, ErrorCode:%s, ErrorMsg:%s\n", $errorItem->getReceiptHandle(), $errorItem->getErrorCode(), $errorItem->getErrorCode());  
            }  
        }  
    }  
    }  
      
    public function run()  
    {  
        // 在当前线程循环消费消息，建议多开个几个线程并发消费消息。  
        while (True) {  
            try {  
                // 通过长轮询方式消费消息。  
                // 若Topic内没有消息，请求会在服务端挂起一段时间（长轮询时间），期间如果有消息可以消费则立即返回客户端。  
                $messages = $this->consumer->consumeMessage(  
                        3, // 一次最多消费3条（最多可设置为16条）。  
                        3 // 长轮询时间3秒（最多可设置为30秒）。  
                );  
            } catch (\MQ\Exception\MessageResolveException $e) {  
                // 当出现消息Body存在不合法字符，无法解析的时候，会抛出此异常。  
                // 可以正常解析的消息列表。  
                $messages = $e->getPartialResult()->getMessages();  
                // 无法正常解析的消息列表。  
                $failMessages = $e->getPartialResult()->getFailResolveMessages();  
                $receiptHandles = array();  
                foreach ($messages as $message) {  
                    // 处理业务逻辑。  
                    $receiptHandles[] = $message->getReceiptHandle();  
                    printf("MsgID %s\n", $message->getMessageId());  
                }  
                foreach ($failMessages as $failMessage) {  
                    // 处理存在不合法字符，无法解析的消息。  
                    $receiptHandles[] = $failMessage->getReceiptHandle();  
                    printf("Fail To Resolve Message. MsgID %s\n", $failMessage->getMessageId());  
                }  
                $this->ackMessages($receiptHandles);  
                continue;  
            } catch (\Exception $e) {  
                if ($e instanceof MQ\Exception\MessageNotExistException) {  
                    // 没有消息可以消费，继续轮询。  
                    printf("No message, contine long polling!RequestId:%s\n", $e->getRequestId());  
                    continue;  
                }  
                print_r($e->getMessage() . "\n");  
                sleep(3);  
                continue;  
            }  
              
            print "consume finish, messages:\n";  
              
            // 处理业务逻辑。  
            $receiptHandles = array();  
            foreach ($messages as $message) {  
                $receiptHandles[] = $message->getReceiptHandle();  
                printf("MessageID:%s TAG:%s BODY:%s \nPublishTime:%d, FirstConsumeTime:%d, \nConsumedTimes:%d, NextConsumeTime:%d,MessageKey:%s\n",  
                        $message->getMessageId(), $message->getMessageTag(), $message->getMessageBody(),  
                        $message->getPublishTime(), $message->getFirstConsumeTime(), $message->getConsumedTimes(), $message->getNextConsumeTime(),  
                        $message->getMessageKey());  
                print_r($message->getProperties());  
            }  
            // $message->getNextConsumeTime()前若不确认消息消费成功，则消息会被重复消费。  
            // 消息句柄有时间戳，同一条消息每次消费拿到的都不一样。  
            print_r($receiptHandles);  
            $this->ackMessages($receiptHandles);  
            print "ack finish\n";  
        }  
    }  
}  
  
$instance = new ConsumerTest();  
$instance->run();  
?>
```

## **2.4 收发顺序消息**

顺序消息（FIFO消息）是消息队列 RocketMQ 版提供的一种严格按照顺序来发布和消费的消息类型。下面提供使用HTTP 协议下的 PHP SDK 收发顺序消息的示例代码。

顺序消息分为两类：
1. 全局顺序：对于指定的一个Topic，所有消息按照严格的先入先出FIFO（First In First Out）的顺序进行发布和消费。
2. 分区顺序：对于指定的一个Topic，所有消息根据Sharding Key进行区块分区。同一个分区内的消息按照严格的FIFO顺序进行发布和消费。Sharding Key是顺序消息中用来区分不同分区的关键字段，和普通消息的Key是完全不同的概念。

发送顺序消息的示例代码如下：
```php
<?php  
require "vendor/autoload.php";  
  
use MQ\Model\TopicMessage;  
use MQ\MQClient;  
  
class ProducerTest  
{  
    private $client;  
    private $producer;  
      
    public function __construct()  
    {  
        $this->client = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        $topic = "${TOPIC}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        $instanceId = "${INSTANCE_ID}";  
        $this->producer = $this->client->getProducer($instanceId, $topic);  
    }  
      
    public function run()  
    {  
        try  
        {  
            for ($i=1; $i<=4; $i++)  
            {  
                $publishMessage = new TopicMessage(  
                        "hello mq!"// 消息内容。  
                );  
                // 设置消息的自定义属性。  
                $publishMessage->putProperty("a", $i);  
                // 重点：设置分区顺序消息的Sharding Key，用于标识不同的分区。Sharding Key与消息的Key是完全不同的概念。  
                $publishMessage->setShardingKey($i % 2);  
                $result = $this->producer->publishMessage($publishMessage);  
                print "Send mq message success. msgId is:" . $result->getMessageId() . ", bodyMD5 is:" . $result->getMessageBodyMD5() . "\n";  
            }  
        } catch (\Exception $e) {  
        print_r($e->getMessage() . "\n");  
    }  
    }  
}  
  
$instance = new ProducerTest();  
$instance->run();  
?>
```

消费顺序消息的示例代码如下：
```php
<?php  
require "vendor/autoload.php";  
  
use MQ\MQClient;  
  
class ConsumerTest  
{  
    private $client;  
    private $consumer;  
      
    public function __construct()  
    {  
        $this->client = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        $topic = "${TOPIC}";  
        // 您在消息队列RocketMQ版控制台创建的Group ID。  
        $groupId = "${GROUP_ID}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        $instanceId = "${INSTANCE_ID}";  
        $this->consumer = $this->client->getConsumer($instanceId, $topic, $groupId);  
    }  
      
    public function ackMessages($receiptHandles)  
    {  
        try {  
            $this->consumer->ackMessage($receiptHandles);  
        } catch (\Exception $e) {  
        if ($e instanceof MQ\Exception\AckMessageException) {  
            // 某些消息的句柄可能超时，会导致消费确认失败。  
            printf("Ack Error, RequestId:%s\n", $e->getRequestId());  
            foreach ($e->getAckMessageErrorItems() as $errorItem) {  
                printf("\tReceiptHandle:%s, ErrorCode:%s, ErrorMsg:%s\n", $errorItem->getReceiptHandle(), $errorItem->getErrorCode(), $errorItem->getErrorCode());  
            }  
        }  
    }  
    }  
      
    public function run()  
    {  
        // 在当前线程循环消费消息，建议多开个几个线程并发消费消息。  
        while (True) {  
            try {  
                // 长轮询顺序消费消息, 拉取到的消息可能是多个分区的（对于分区顺序消息），一个分区内的消息一定是顺序的。  
                // 对于分区顺序消息，只要一个分区内存在没有被确认消费的消息，那么该分区下次还会消费到相同的消息。  
                // 对于一个分区，只有所有消息确认消费成功才能消费下一批消息。  
                // 长轮询消费消息。若Topic内没有消息，请求会在服务端挂起一段时间（长轮询时间），期间如果有消息可以消费则立即返回客户端。  
                $messages = $this->consumer->consumeMessageOrderly(  
                        3, // 一次最多消费3条（最多可设置为16条）。  
                        3  // 长轮询时间3秒（最多可设置为30秒）。  
                );  
            } catch (\MQ\Exception\MessageResolveException $e) {  
                // 当出现消息Body存在不合法字符，无法解析的时候，会抛出此异常。  
                // 可以正常解析的消息列表。  
                $messages = $e->getPartialResult()->getMessages();  
                // 无法正常解析的消息列表。  
                $failMessages = $e->getPartialResult()->getFailResolveMessages();  
                $receiptHandles = array();  
                foreach ($messages as $message) {  
                    // 处理业务逻辑。  
                    $receiptHandles[] = $message->getReceiptHandle();  
                    printf("MsgID %s\n", $message->getMessageId());  
                }  
                foreach ($failMessages as $failMessage) {  
                    // 处理存在不合法字符，无法解析的消息。  
                    $receiptHandles[] = $failMessage->getReceiptHandle();  
                    printf("Fail To Resolve Message. MsgID %s\n", $failMessage->getMessageId());  
                }  
                $this->ackMessages($receiptHandles);  
                continue;  
            } catch (\Exception $e) {  
                if ($e instanceof MQ\Exception\MessageNotExistException) {  
                    // 没有消息，则继续长轮询服务器。  
                    printf("No message, contine long polling!RequestId:%s\n", $e->getRequestId());  
                    continue;  
                }  
                print_r($e->getMessage() . "\n");  
                sleep(3);  
                continue;  
            }  
            print "======>consume finish, messages:\n";  
            // 处理业务逻辑。  
            $receiptHandles = array();  
            foreach ($messages as $message) {  
                $receiptHandles[] = $message->getReceiptHandle();  
                printf("MessageID:%s TAG:%s BODY:%s \nPublishTime:%d, FirstConsumeTime:%d, \nConsumedTimes:%d, NextConsumeTime:%d,ShardingKey:%s\n",  
                        $message->getMessageId(), $message->getMessageTag(), $message->getMessageBody(),  
                        $message->getPublishTime(), $message->getFirstConsumeTime(), $message->getConsumedTimes(), $message->getNextConsumeTime(),  
                        $message->getShardingKey());  
                print_r($message->getProperties());  
            }  
            // $message->getNextConsumeTime()前若不确认消息消费成功，则消息会被重复消费。  
            // 消息句柄有时间戳，同一条消息每次消费拿到的都不一样。  
            print_r($receiptHandles);  
            $this->ackMessages($receiptHandles);  
            print "=======>ack finish\n";  
        }  
    }  
}  
  
$instance = new ConsumerTest();  
$instance->run();  
?>
```

## **2.5 收发定时消息和延时消息**

1. 延时消息：Producer将消息发送到消息队列RocketMQ版服务端，但并不期望立马投递这条消息，而是延迟一定时间后才投递到Consumer进行消费，该消息即延时消息。
2. 定时消息：Producer将消息发送到消息队列RocketMQ版服务端，但并不期望立马投递这条消息，而是推迟到在当前时间点之后的某一个时间投递到Consumer进行消费，该消息即定时消息。

发送定时消息或延时消息的示例代码如下:
```php
<?php  
require "vendor/autoload.php";  
  
use MQ\Model\TopicMessage;  
use MQ\MQClient;  
  
class ProducerTest  
{  
    private $client;  
    private $producer;  
      
    public function __construct()  
    {  
        $this->client = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        $topic = "${TOPIC}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        $instanceId = "${INSTANCE_ID}";  
        $this->producer = $this->client->getProducer($instanceId, $topic);  
    }  
      
    public function run()  
    {  
        try  
        {  
            for ($i=1; $i<=4; $i++)  
            {  
                $publishMessage = new TopicMessage(  
                        "hello mq!"// 消息内容。  
                );  
                // 设置消息的自定义属性。  
                $publishMessage->putProperty("a", $i);  
                // 设置消息的Key。  
                $publishMessage->setMessageKey("MessageKey");  
                // 延时消息，发送时间为10s后。该参数格式为毫秒级别的时间戳。  
                // 若发送定时消息，设置该参数时需要计算定时时间与当前时间的时间差。  
                $publishMessage->setStartDeliverTime(time() * 1000 + 10 * 1000);  
                $result = $this->producer->publishMessage($publishMessage);  
                print "Send mq message success. msgId is:" . $result->getMessageId() . ", bodyMD5 is:" . $result->getMessageBodyMD5() . "\n";  
            }  
        } catch (\Exception $e) {  
        print_r($e->getMessage() . "\n");  
    }  
    }  
}  
  
$instance = new ProducerTest();  
$instance->run();  
?>
```

消费定时消息或延时消息的示例代码如下：
```php
<?php  
require "vendor/autoload.php";  
use MQ\MQClient;  
  
class ConsumerTest  
{  
    private $client;  
    private $consumer;  
      
    public function __construct()  
    {  
        $this->client = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        $topic = "${TOPIC}";  
        // 您在消息队列RocketMQ版控制台创建的Group ID。  
        $groupId = "${GROUP_ID}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        $instanceId = "${INSTANCE_ID}";  
        $this->consumer = $this->client->getConsumer($instanceId, $topic, $groupId);  
    }  
      
    public function ackMessages($receiptHandles)  
    {  
        try {  
            $this->consumer->ackMessage($receiptHandles);  
        } catch (\Exception $e) {  
        if ($e instanceof MQ\Exception\AckMessageException) {  
            // 某些消息的句柄可能超时，会导致消费确认失败。  
            printf("Ack Error, RequestId:%s\n", $e->getRequestId());  
            foreach ($e->getAckMessageErrorItems() as $errorItem) {  
                printf("\tReceiptHandle:%s, ErrorCode:%s, ErrorMsg:%s\n", $errorItem->getReceiptHandle(), $errorItem->getErrorCode(), $errorItem->getErrorCode());  
            }  
        }  
    }  
    }  
      
    public function run()  
    {  
        // 在当前线程循环消费消息，建议多开个几个线程并发消费消息。  
        while (True) {  
            try {  
                // 长轮询消费消息。  
                // 若Topic内没有消息，请求会在服务端挂起一段时间（长轮询时间），期间如果有消息可以消费则立即返回客户端。  
                $messages = $this->consumer->consumeMessage(  
                        3, // 一次最多消费3条（最多可设置为16条）。  
                        3 // 长轮询时间3秒（最多可设置为30秒）。  
                );  
            } catch (\MQ\Exception\MessageResolveException $e) {  
                // 当出现消息Body存在不合法字符，无法解析的时候，会抛出此异常。  
                // 可以正常解析的消息列表。  
                $messages = $e->getPartialResult()->getMessages();  
                // 无法正常解析的消息列表。  
                $failMessages = $e->getPartialResult()->getFailResolveMessages();  
                $receiptHandles = array();  
                foreach ($messages as $message) {  
                    // 处理业务逻辑。  
                    $receiptHandles[] = $message->getReceiptHandle();  
                    printf("MsgID %s\n", $message->getMessageId());  
                }  
                foreach ($failMessages as $failMessage) {  
                    // 处理存在不合法字符，无法解析的消息。  
                    $receiptHandles[] = $failMessage->getReceiptHandle();  
                    printf("Fail To Resolve Message. MsgID %s\n", $failMessage->getMessageId());  
                }  
                $this->ackMessages($receiptHandles);  
                continue;  
            } catch (\Exception $e) {  
                if ($e instanceof MQ\Exception\MessageNotExistException) {  
                    // 没有消息可以消费，继续轮询。  
                    printf("No message, contine long polling!RequestId:%s\n", $e->getRequestId());  
                    continue;  
                }  
                print_r($e->getMessage() . "\n");  
                sleep(3);  
                continue;  
            }  
            print "consume finish, messages:\n";  
            // 处理业务逻辑。  
            $receiptHandles = array();  
            foreach ($messages as $message) {  
                $receiptHandles[] = $message->getReceiptHandle();  
                printf("MessageID:%s TAG:%s BODY:%s \nPublishTime:%d, FirstConsumeTime:%d, \nConsumedTimes:%d, NextConsumeTime:%d,MessageKey:%s\n",  
                        $message->getMessageId(), $message->getMessageTag(), $message->getMessageBody(),  
                        $message->getPublishTime(), $message->getFirstConsumeTime(), $message->getConsumedTimes(), $message->getNextConsumeTime(),  
                        $message->getMessageKey());  
                print_r($message->getProperties());  
            }  
            // $message->getNextConsumeTime()前若不确认消息消费成功，则消息会被重复消费。  
            // 消息句柄有时间戳，同一条消息每次消费拿到的都不一样。  
            print_r($receiptHandles);  
            $this->ackMessages($receiptHandles);  
            print "ack finish\n";  
        }  
    }  
}  
  
$instance = new ConsumerTest();  
$instance->run();  
?>
```

## **2.6 收发事务消息**

消息队列 RocketMQ 版提供类似 XA 或 Open XA 的分布式事务功能，通过消息队列 RocketMQ 版事务消息，能达到分布式事务的最终一致。下面提供使用 HTTP 协议下的 PHP SDK 收发事务消息的示例代码。

发送事务消息的示例代码如下：
```php
<?php  
require "vendor/autoload.php";  
  
use MQ\Model\TopicMessage;  
use MQ\MQClient;  
  
class ProducerTest  
{  
    private $client;  
    private $transProducer;  
    private $count;  
    private $popMsgCount;  
      
    public function __construct()  
    {  
        $this->client = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        $topic = "${TOPIC}";  
        // 您在消息队列RocketMQ版控制台创建的Group ID。  
        $groupId = "${GROUP_ID}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        $instanceId = "${INSTANCE_ID}";  
        $this->transProducer = $this->client->getTransProducer($instanceId,$topic, $groupId);  
        $this->count = 0;  
        $this->popMsgCount = 0;  
    }  
      
    function processAckError($e) {  
        if ($e instanceof MQ\Exception\AckMessageException) {  
            // 如果Commit或Rollback时超过了TransCheckImmunityTime（针对发送事务消息的句柄）或者超过NextConsumeTime（针对consumeHalfMessage的句柄），则Commit或Rollback会失败。  
            printf("Commit/Rollback Error, RequestId:%s\n", $e->getRequestId());  
            foreach ($e->getAckMessageErrorItems() as $errorItem) {  
                printf("\tReceiptHandle:%s, ErrorCode:%s, ErrorMsg:%s\n", $errorItem->getReceiptHandle(), $errorItem->getErrorCode(), $errorItem->getErrorCode());  
            }  
        } else {  
            print_r($e);  
        }  
    }  
      
    function consumeHalfMsg() {  
        while($this->count < 3 && $this->popMsgCount < 15) {  
            $this->popMsgCount++;  
            try {  
                $messages = $this->transProducer->consumeHalfMessage(4, 3);  
            } catch (\Exception $e) {  
                if ($e instanceof MQ\Exception\MessageNotExistException) {  
                    print "no half transaction message\n";  
                    continue;  
                }  
                print_r($e->getMessage() . "\n");  
                sleep(3);  
                continue;  
            }  
              
            foreach ($messages as $message) {  
                printf("ID:%s TAG:%s BODY:%s \nPublishTime:%d, FirstConsumeTime:%d\nConsumedTimes:%d, NextConsumeTime:%d\nPropA:%s\n",  
                        $message->getMessageId(), $message->getMessageTag(), $message->getMessageBody(),  
                        $message->getPublishTime(), $message->getFirstConsumeTime(), $message->getConsumedTimes(), $message->getNextConsumeTime(),  
                        $message->getProperty("a"));  
                print_r($message->getProperties());  
                $propA = $message->getProperty("a");  
                $consumeTimes = $message->getConsumedTimes();  
                try {  
                    if ($propA == "1") {  
                        print "\n commit transaction msg: " . $message->getMessageId() . "\n";  
                        $this->transProducer->commit($message->getReceiptHandle());  
                        $this->count++;  
                    } else if ($propA == "2" && $consumeTimes > 1) {  
                        print "\n commit transaction msg: " . $message->getMessageId() . "\n";  
                        $this->transProducer->commit($message->getReceiptHandle());  
                        $this->count++;  
                    } else if ($propA == "3") {  
                        print "\n rollback transaction msg: " . $message->getMessageId() . "\n";  
                        $this->transProducer->rollback($message->getReceiptHandle());  
                        $this->count++;  
                    } else {  
                        print "\n unknown transaction msg: " . $message->getMessageId() . "\n";  
                    }  
                } catch (\Exception $e) {  
                    $this->processAckError($e);  
                }  
            }  
        }  
    }  
      
    public function run()  
    {  
        // 循环发送4条事务消息。  
        for ($i = 0; $i < 4; $i++) {  
            $pubMsg = new TopicMessage("hello,mq");  
            // 设置消息的自定义属性。  
            $pubMsg->putProperty("a", $i);  
            // 设置消息的Key。  
            $pubMsg->setMessageKey("MessageKey");  
            // 设置事务第一次回查的时间，为相对时间。单位：秒，范围：10~300。  
            // 第一次事务回查后如果消息没有Commit或者Rollback，则之后每隔10s左右会回查一次，共回查24小时。  
            $pubMsg->setTransCheckImmunityTime(10);  
            $topicMessage = $this->transProducer->publishMessage($pubMsg);  
            print "\npublish -> \n\t" . $topicMessage->getMessageId() . " " . $topicMessage->getReceiptHandle() . "\n";  
            if ($i == 0) {  
                try {  
                    // 发送完事务消息后能获取到半消息句柄，可以直接Commit或Rollback事务消息。  
                    $this->transProducer->commit($topicMessage->getReceiptHandle());  
                    print "\n commit transaction msg when publish: " . $topicMessage->getMessageId() . "\n";  
                } catch (\Exception $e) {  
                    // 如果Commit或Rollback时超过了TransCheckImmunityTime则会失败。  
                    $this->processAckError($e);  
                }  
            }  
        }  
        // 客户端需要有一个线程或者进程来消费没有确认的事务消息。  
        // 检查没有确认的事务消息。  
        $this->consumeHalfMsg();  
    }  
}  
  
$instance = new ProducerTest();  
$instance->run();  
?>
```

消费事务消息的示例代码如下：
```php
<?php  
require "vendor/autoload.php";  
use MQ\MQClient;  
  
class ConsumerTest  
{  
    private $client;  
    private $consumer;  
      
    public function __construct()  
    {  
        $this->client = new MQClient(  
                // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
        // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
        $topic = "${TOPIC}";  
        // 您在消息队列RocketMQ版控制台创建的Group ID。  
        $groupId = "${GROUP_ID}";  
        // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
        $instanceId = "${INSTANCE_ID}";  
        $this->consumer = $this->client->getConsumer($instanceId, $topic, $groupId);  
    }  
      
    public function ackMessages($receiptHandles)  
    {  
        try {  
            $this->consumer->ackMessage($receiptHandles);  
        } catch (\Exception $e) {  
        if ($e instanceof MQ\Exception\AckMessageException) {  
            // 某些消息的句柄可能超时，会导致消费确认失败。  
            printf("Ack Error, RequestId:%s\n", $e->getRequestId());  
            foreach ($e->getAckMessageErrorItems() as $errorItem) {  
                printf("\tReceiptHandle:%s, ErrorCode:%s, ErrorMsg:%s\n", $errorItem->getReceiptHandle(), $errorItem->getErrorCode(), $errorItem->getErrorCode());  
            }  
        }  
    }  
    }  
      
    public function run()  
    {  
        // 在当前线程循环消费消息，建议多开个几个线程并发消费消息。  
        while (True) {  
            try {  
                // 长轮询消费消息。  
                // 若Topic内没有消息，请求会在服务端挂起一段时间（长轮询时间），期间如果有消息可以消费则立即返回客户端。  
                $messages = $this->consumer->consumeMessage(  
                        3, // 一次最多消费3条（最多可设置为16条）。  
                        3 // 长轮询时间3秒（最多可设置为30秒）。  
                );  
            } catch (\MQ\Exception\MessageResolveException $e) {  
                // 当出现消息Body存在不合法字符，无法解析的时候，会抛出此异常。  
                // 可以正常解析的消息列表。  
                $messages = $e->getPartialResult()->getMessages();  
                // 无法正常解析的消息列表。  
                $failMessages = $e->getPartialResult()->getFailResolveMessages();  
                $receiptHandles = array();  
                foreach ($messages as $message) {  
                    // 处理业务逻辑。  
                    $receiptHandles[] = $message->getReceiptHandle();  
                    printf("MsgID %s\n", $message->getMessageId());  
                }  
                foreach ($failMessages as $failMessage) {  
                    // 处理存在不合法字符，无法解析的消息。  
                    $receiptHandles[] = $failMessage->getReceiptHandle();  
                    printf("Fail To Resolve Message. MsgID %s\n", $failMessage->getMessageId());  
                }  
                $this->ackMessages($receiptHandles);  
                continue;  
            } catch (\Exception $e) {  
                if ($e instanceof MQ\Exception\MessageNotExistException) {  
                    // 没有消息可以消费，继续轮询。  
                    printf("No message, contine long polling!RequestId:%s\n", $e->getRequestId());  
                    continue;  
                }  
                print_r($e->getMessage() . "\n");  
                sleep(3);  
                continue;  
            }  
            print "consume finish, messages:\n";  
            // 处理业务逻辑。  
            $receiptHandles = array();  
            foreach ($messages as $message) {  
                $receiptHandles[] = $message->getReceiptHandle();  
                printf("MessageID:%s TAG:%s BODY:%s \nPublishTime:%d, FirstConsumeTime:%d, \nConsumedTimes:%d, NextConsumeTime:%d,MessageKey:%s\n",  
                        $message->getMessageId(), $message->getMessageTag(), $message->getMessageBody(),  
                        $message->getPublishTime(), $message->getFirstConsumeTime(), $message->getConsumedTimes(), $message->getNextConsumeTime(),  
                        $message->getMessageKey());  
                print_r($message->getProperties());  
            }  
              
            // $message->getNextConsumeTime()前若不确认消息消费成功，则消息会被重复消费。  
            // 消息句柄有时间戳，同一条消息每次消费拿到的都不一样。  
            print_r($receiptHandles);  
            $this->ackMessages($receiptHandles);  
            print "ack finish\n";  
        }  
    }  
}  
  
$instance = new ConsumerTest();  
$instance->run();  
?>
```

# **03 GO 客户端**

在使用Go SDK收发消息前，您需按照下面提供的内容来准备环境。

## **3.1 环境要求**

1. 安装 Golang。更多信息，请参见 [安装Golang](https://golang.org/doc/install/source?spm=a2c4g.11186623.2.4.509c3c4eTDXQo2)。
2. 安装完成后，您可以执行 go version 命令查看 Go 语言版本。

## **3.2 安装 GO SDK**

1. 执行以下命令启用Go mod。
```go
go env -w GO111MODULE=on
```

2. 执行以下命令设置Go mod代理。
```go
go env -w GOPROXY=https://goproxy.cn,direct
```

3. 执行以下命令进行初始化，生成[go.mod](http://go.mod/)文件。
```go
go mod init
```

4. 执行以下命令安装Go SDK。
```go
go get github.com/aliyunmq/mq-http-go-sdk
```

## **3.3 收发普通消息**

普通消息是指消息队列 RocketMQ 版中无特性的消息，区别于有特性的定时和延时消息、顺序消息和事务消息。下面提供使用 HTTP 协议下的 Go SDK 收发普通消息的示例代码。

发送普通消息的示例代码如下：
```go
package main  
  
import (  
        "fmt"  
        "time"        "strconv"        "github.com/aliyunmq/mq-http-go-sdk"        )  
  
func main() {  
    // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
    endpoint := "${HTTP_ENDPOINT}"  
    // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
    accessKey := "${ACCESS_KEY}"  
    // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
    secretKey := "${SECRET_KEY}"  
    // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
    topic := "${TOPIC}"  
    // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
    // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
    instanceId := "${INSTANCE_ID}"  
    client := mq_http_sdk.NewAliyunMQClient(endpoint, accessKey, secretKey, "")  
    mqProducer := client.GetProducer(instanceId, topic)  
    // 循环发送4条消息。  
    for i := 0; i < 4; i++ {  
        var msg mq_http_sdk.PublishMessageRequest  
                  
                msg = mq_http_sdk.PublishMessageRequest{  
            MessageBody: "hello mq!",         //消息内容。  
                    MessageTag:  "",                  // 消息标签。  
                    Properties:  map[string]string{}, // 消息属性。  
        }  
        // 设置消息的Key。  
        msg.MessageKey = "MessageKey"  
        // 设置消息自定义属性。  
        msg.Properties["a"] = strconv.Itoa(i)  
        ret, err := mqProducer.PublishMessage(msg)  
        if err != nil {  
            fmt.Println(err)  
            return  
        } else {  
            fmt.Printf("Publish ---->\n\tMessageId:%s, BodyMD5:%s, \n", ret.MessageId, ret.MessageBodyMD5)  
        }  
        time.Sleep(time.Duration(100) * time.Millisecond)  
    }  
}
```

消费普通消息的示例代码如下：
```go
package main  
  
import (  
        "fmt"  
        "github.com/gogap/errors"        "strings"        "time"        "github.com/aliyunmq/mq-http-go-sdk"        )  
  
func main() {  
    // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
    endpoint := "${HTTP_ENDPOINT}"  
    // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
    accessKey := "${ACCESS_KEY}"  
    // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
    secretKey := "${SECRET_KEY}"  
    // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
    //不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
    topic := "${TOPIC}"  
    // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
    // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
    instanceId := "${INSTANCE_ID}"  
    // 您在控制台创建的Group ID。  
    groupId := "${GROUP_ID}"  
    client := mq_http_sdk.NewAliyunMQClient(endpoint, accessKey, secretKey, "")  
    mqConsumer := client.GetConsumer(instanceId, topic, groupId, "")  
    for {  
        endChan := make(chan int)  
        respChan := make(chan mq_http_sdk.ConsumeMessageResponse)  
        errChan := make(chan error)  
        go func() {  
            select {  
                case resp := <-respChan:  
                {  
                    // 处理业务逻辑。  
                    var handles []string  
                    fmt.Printf("Consume %d messages---->\n", len(resp.Messages))  
                    for _, v := range resp.Messages {  
                    handles = append(handles, v.ReceiptHandle)  
                    fmt.Printf("\tMessageID: %s, PublishTime: %d, MessageTag: %s\n"+  
                                    "\tConsumedTimes: %d, FirstConsumeTime: %d, NextConsumeTime: %d\n"+  
                                    "\tBody: %s\n"+  
                                    "\tProps: %s\n",  
                            v.MessageId, v.PublishTime, v.MessageTag, v.ConsumedTimes,  
                            v.FirstConsumeTime, v.NextConsumeTime, v.MessageBody, v.Properties)  
                }  
                    // NextConsumeTime前若不确认消息消费成功，则消息会被重复消费。  
                    // 消息句柄有时间戳，同一条消息每次消费拿到的都不一样。  
                    ackerr := mqConsumer.AckMessage(handles)  
                    if ackerr != nil {  
                    // 某些消息的句柄可能超时，会导致消息消费状态确认不成功。  
                    fmt.Println(ackerr)  
                    if errAckItems, ok := ackerr.(errors.ErrCode).Context()["Detail"].([]mq_http_sdk.ErrAckItem); ok {  
                        for _, errAckItem := range errAckItems {  
                            fmt.Printf("\tErrorHandle:%s, ErrorCode:%s, ErrorMsg:%s\n",  
                                    errAckItem.ErrorHandle, errAckItem.ErrorCode, errAckItem.ErrorMsg)  
                        }  
                    } else {  
                        fmt.Println("ack err =", ackerr)  
                    }  
                    time.Sleep(time.Duration(3) * time.Second)  
                } else {  
                    fmt.Printf("Ack ---->\n\t%s\n", handles)  
                }  
                    endChan <- 1  
                }  
                case err := <-errChan:  
                {  
                    // Topic中没有消息可消费。  
                    if strings.Contains(err.(errors.ErrCode).Error(), "MessageNotExist") {  
                    fmt.Println("\nNo new message, continue!")  
                } else {  
                    fmt.Println(err)  
                    time.Sleep(time.Duration(3) * time.Second)  
                }  
                    endChan <- 1  
                }  
                case <-time.After(35 * time.Second):  
                {  
                    fmt.Println("Timeout of consumer message ??")  
                    endChan <- 1  
                }  
            }  
        }()  
        // 长轮询消费消息，网络超时时间默认为35s。  
        // 长轮询表示如果Topic没有消息，则客户端请求会在服务端挂起3s，3s内如果有消息可以消费则立即返回响应。  
        mqConsumer.ConsumeMessage(respChan, errChan,  
                3, // 一次最多消费3条（最多可设置为16条）。  
                3, // 长轮询时间3s（最多可设置为30s）。  
        )  
                <-endChan  
    }  
}
```

## **3.4 收发顺序消息**

顺序消息（FIFO消息）是消息队列 RocketMQ 版提供的一种严格按照顺序来发布和消费的消息类型。下面提供使用HTTP 协议下的 Go SDK 收发顺序消息的示例代码。

发送顺序消息的示例代码如下：
```go
package main  
  
import (  
        "fmt"  
        "time"        "strconv"        "github.com/aliyunmq/mq-http-go-sdk"        )  
  
func main() {  
    // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
    endpoint := "${HTTP_ENDPOINT}"  
    // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
    accessKey := "${ACCESS_KEY}"  
    // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
    secretKey := "${SECRET_KEY}"  
    // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
    topic := "${TOPIC}"  
    // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
    // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
    instanceId := "${INSTANCE_ID}"  
    client := mq_http_sdk.NewAliyunMQClient(endpoint, accessKey, secretKey, "")  
    mqProducer := client.GetProducer(instanceId, topic)  
    // 循环发送8条消息。  
    for i := 0; i < 8; i++ {  
        msg := mq_http_sdk.PublishMessageRequest{  
            MessageBody: "hello mq!",         //消息内容。  
                    MessageTag:  "",                  // 消息标签。  
                    Properties:  map[string]string{}, // 消息属性。  
        }  
        // 设置消息的Key。  
        msg.MessageKey = "MessageKey"  
        // 设置消息的自定义属性。  
        msg.Properties["a"] = strconv.Itoa(i)  
        // 设置分区顺序消息的Sharding Key，用于标识不同的分区。Sharding Key与消息的Key是完全不同的概念。  
        msg.ShardingKey = strconv.Itoa(i % 2)  
        ret, err := mqProducer.PublishMessage(msg)  
        if err != nil {  
            fmt.Println(err)  
            return  
        } else {  
            fmt.Printf("Publish ---->\n\tMessageId:%s, BodyMD5:%s, \n", ret.MessageId, ret.MessageBodyMD5)  
        }  
        time.Sleep(time.Duration(100) * time.Millisecond)  
    }  
}
```

消费顺序消息的示例代码如下：
```go
package main  
  
import (  
        "fmt"  
        "github.com/gogap/errors"        "strings"        "time"        "github.com/aliyunmq/mq-http-go-sdk"        )  
  
func main() {  
    // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
    endpoint := "${HTTP_ENDPOINT}"  
    // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
    accessKey := "${ACCESS_KEY}"  
    // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
    secretKey := "${SECRET_KEY}"  
    // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
    topic := "${TOPIC}"  
    // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
    // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
    instanceId := "${INSTANCE_ID}"  
    // 您在控制台创建的Group ID。  
    groupId := "${GROUP_ID}"  
    client := mq_http_sdk.NewAliyunMQClient(endpoint, accessKey, secretKey, "")  
    mqConsumer := client.GetConsumer(instanceId, topic, groupId, "")  
      
    for {  
        endChan := make(chan int)  
        respChan := make(chan mq_http_sdk.ConsumeMessageResponse)  
        errChan := make(chan error)  
        go func() {  
            select {  
                case resp := <-respChan:  
                {  
                    // 处理业务逻辑。  
                    var handles []string  
                    fmt.Printf("Consume %d messages---->\n", len(resp.Messages))  
                    for _, v := range resp.Messages {  
                    handles = append(handles, v.ReceiptHandle)  
                    fmt.Printf("\tMessageID: %s, PublishTime: %d, MessageTag: %s\n"+  
                                    "\tConsumedTimes: %d, FirstConsumeTime: %d, NextConsumeTime: %d\n"+  
                                    "\tBody: %s\n"+  
                                    "\tProps: %s\n"+  
                                    "\tShardingKey: %s\n",  
                            v.MessageId, v.PublishTime, v.MessageTag, v.ConsumedTimes,  
                            v.FirstConsumeTime, v.NextConsumeTime, v.MessageBody, v.Properties, v.ShardingKey)  
                }  
                    // NextConsumeTime前若不确认消息消费成功，则消息会被重复消费。  
                    // 消息句柄有时间戳，同一条消息每次消费拿到的都不一样。  
                    ackerr := mqConsumer.AckMessage(handles)  
                    if ackerr != nil {  
                    // 某些消息的句柄可能超时，会导致消息消费状态确认不成功。  
                    fmt.Println(ackerr)  
                    if errAckItems, ok := ackerr.(errors.ErrCode).Context()["Detail"].([]mq_http_sdk.ErrAckItem); ok {  
                        for _, errAckItem := range errAckItems {  
                            fmt.Printf("\tErrorHandle:%s, ErrorCode:%s, ErrorMsg:%s\n",  
                                    errAckItem.ErrorHandle, errAckItem.ErrorCode, errAckItem.ErrorMsg)  
                        }  
                    } else {  
                        fmt.Println("ack err =", ackerr)  
                    }  
                    time.Sleep(time.Duration(3) * time.Second)  
                } else {  
                    fmt.Printf("Ack ---->\n\t%s\n", handles)  
                }  
                    endChan <- 1  
                }  
                case err := <-errChan:  
                {  
                    // Topic中没有消息可消费。  
                    if strings.Contains(err.(errors.ErrCode).Error(), "MessageNotExist") {  
                    fmt.Println("\nNo new message, continue!")  
                } else {  
                    fmt.Println(err)  
                    time.Sleep(time.Duration(3) * time.Second)  
                }  
                    endChan <- 1  
                }  
                case <-time.After(35 * time.Second):  
                {  
                    fmt.Println("Timeout of consumer message ??")  
                    endChan <- 1  
                }  
            }  
        }()  
        // 拉取到的消息可能是多个分区的（对于分区顺序消息），一个分区内的消息一定是顺序的。  
        // 对于分区顺序消息，只要一个分区内存在没有被确认消费的消息，那么该分区下次还会消费到相同的消息。  
        // 对于一个分区，只有所有消息确认消费成功才能消费下一批消息。  
        // 长轮询顺序消费消息, 网络超时时间为35s。  
        // 长轮询表示如果Topic没有消息，则请求会在服务端挂起3s，3s内如果有消息可以消费则服务端立即返回响应。  
        mqConsumer.ConsumeMessageOrderly(respChan, errChan,  
                3, // 一次最多消费3条（最多可设置为16条）。  
                3, // 长轮询时间3s（最多可设置为30s）。  
        )  
                <-endChan  
    }  
}
```

## **3.5 收发定时消息和延时消息**

下面提供使用 HTTP 协议下的 Go SDK 收发定时消息和延时消息的示例代码。

发送定时消息或延时消息的示例代码如下:
```go
package main  
  
import (  
        "fmt"  
        "time"        "strconv"        "github.com/aliyunmq/mq-http-go-sdk"        )  
  
func main() {  
    // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
    endpoint := "${HTTP_ENDPOINT}"  
    // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
    accessKey := "${ACCESS_KEY}"  
    // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
    secretKey := "${SECRET_KEY}"  
    // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
    topic := "${TOPIC}"  
    // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
    // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。   
instanceId := "${INSTANCE_ID}"  
    client := mq_http_sdk.NewAliyunMQClient(endpoint, accessKey, secretKey, "")  
    mqProducer := client.GetProducer(instanceId, topic)  
    // 循环发送4条消息。  
    for i := 0; i < 4; i++ {  
        var msg mq_http_sdk.PublishMessageRequest  
                msg = mq_http_sdk.PublishMessageRequest{  
            MessageBody: "hello mq!",         // 消息内容。  
                    MessageTag:  "",                  // 消息标签。  
                    Properties:  map[string]string{}, // 消息属性。  
        }  
        // 设置消息的Key。  
        msg.MessageKey = "MessageKey"  
        // 设置消息自定义属性。  
        msg.Properties["a"] = strconv.Itoa(i)  
        // 延时消息，发送时间为10s后。该参数格式为毫秒级别的时间戳。  
        // 若发送定时消息，设置该参数时需要计算定时时间与当前时间的时间差。  
        msg.StartDeliverTime = time.Now().UTC().Unix() * 1000 + 10 * 1000  
        ret, err := mqProducer.PublishMessage(msg)  
        if err != nil {  
            fmt.Println(err)  
            return  
        } else {  
            fmt.Printf("Publish ---->\n\tMessageId:%s, BodyMD5:%s, \n", ret.MessageId, ret.MessageBodyMD5)  
        }  
        time.Sleep(time.Duration(100) * time.Millisecond)  
    }  
}
```

消费定时消息或延时消息的示例代码如下:
```go
package main  
  
import (  
        "fmt"  
        "github.com/gogap/errors"        "strings"        "time"        "github.com/aliyunmq/mq-http-go-sdk"        )  
  
func main() {  
    // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
    endpoint := "${HTTP_ENDPOINT}"  
    // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
    accessKey := "${ACCESS_KEY}"  
    // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
    secretKey := "${SECRET_KEY}"  
    // 消息所属的Topic，在消息队列RocketMQ版控制台创建。  
    //不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
    topic := "${TOPIC}"  
    // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
    // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
    instanceId := "${INSTANCE_ID}"  
    // 您在控制台创建的Group ID。  
    groupId := "${GROUP_ID}"  
    client := mq_http_sdk.NewAliyunMQClient(endpoint, accessKey, secretKey, "")  
    mqConsumer := client.GetConsumer(instanceId, topic, groupId, "")  
    for {  
        endChan := make(chan int)  
        respChan := make(chan mq_http_sdk.ConsumeMessageResponse)  
        errChan := make(chan error)  
        go func() {  
            select {  
                case resp := <-respChan:  
                {  
                    // 处理业务逻辑。  
                    var handles []string  
                    fmt.Printf("Consume %d messages---->\n", len(resp.Messages))  
                    for _, v := range resp.Messages {  
                    handles = append(handles, v.ReceiptHandle)  
                    fmt.Printf("\tMessageID: %s, PublishTime: %d, MessageTag: %s\n"+  
                                    "\tConsumedTimes: %d, FirstConsumeTime: %d, NextConsumeTime: %d\n"+  
                                    "\tBody: %s\n"+  
                                    "\tProps: %s\n",  
                            v.MessageId, v.PublishTime, v.MessageTag, v.ConsumedTimes,  
                            v.FirstConsumeTime, v.NextConsumeTime, v.MessageBody, v.Properties)  
                }  
                    // NextConsumeTime前若不确认消息消费成功，则消息会被重复消费。  
                    // 消息句柄有时间戳，同一条消息每次消费拿到的都不一样。  
                    ackerr := mqConsumer.AckMessage(handles)  
                    if ackerr != nil {  
                    // 某些消息的句柄可能超时，会导致消息消费状态确认不成功。  
                    fmt.Println(ackerr)  
                    if errAckItems, ok := ackerr.(errors.ErrCode).Context()["Detail"].([]mq_http_sdk.ErrAckItem); ok {  
                        for _, errAckItem := range errAckItems {  
                            fmt.Printf("\tErrorHandle:%s, ErrorCode:%s, ErrorMsg:%s\n",  
                                    errAckItem.ErrorHandle, errAckItem.ErrorCode, errAckItem.ErrorMsg)  
                        }  
                    } else {  
                        fmt.Println("ack err =", ackerr)  
                    }  
                    time.Sleep(time.Duration(3) * time.Second)  
                } else {  
                    fmt.Printf("Ack ---->\n\t%s\n", handles)  
                }  
                    endChan <- 1  
                }  
                case err := <-errChan:  
                {  
                    // Topic中没有消息可消费。  
                    if strings.Contains(err.(errors.ErrCode).Error(), "MessageNotExist") {  
                    fmt.Println("\nNo new message, continue!")  
                } else {  
                    fmt.Println(err)  
                    time.Sleep(time.Duration(3) * time.Second)  
                }  
                    endChan <- 1  
                }  
                case <-time.After(35 * time.Second):  
                {  
                    fmt.Println("Timeout of consumer message ??")  
                    endChan <- 1  
                }  
            }  
        }()  
        // 长轮询消费消息，网络超时时间默认为35s。  
        // 长轮询表示如果Topic没有消息，则客户端请求会在服务端挂起3s，3s内如果有消息可以消费则立即返回响应。  
        mqConsumer.ConsumeMessage(respChan, errChan,  
                3, // 一次最多消费3条（最多可设置为16条）。  
                3, // 长轮询时间3s（最多可设置为30s）。  
        )  
                <-endChan  
    }  
}
```

## **3.6 收发事务消息**

消息队列 RocketMQ 版提供类似 XA 或 Open XA 的分布式事务功能，通过消息队列 RocketMQ 版事务消息，能达到分布式事务的最终一致。下面提供使用 HTTP 协议下的 Go SDK 收发事务消息的示例代码。

发送事务消息的示例代码如下：
```go
package main  
  
import (  
        "fmt"  
        "github.com/gogap/errors"        "strconv"        "strings"        "time"        "github.com/aliyunmq/mq-http-go-sdk"        )  
  
var loopCount = 0  
func ProcessError(err error) {  
    // 如果Commit或Rollback时超过了TransCheckImmunityTime（针对发送事务消息的句柄）或者超过10s（针对consumeHalfMessage的句柄），则Commit或Rollback失败。  
    if err == nil {  
        return  
    }  
    fmt.Println(err)  
    for _, errAckItem := range err.(errors.ErrCode).Context()["Detail"].([]mq_http_sdk.ErrAckItem)  
    {  
        fmt.Printf("\tErrorHandle:%s, ErrorCode:%s, ErrorMsg:%s\n",  
                errAckItem.ErrorHandle, errAckItem.ErrorCode, errAckItem.ErrorMsg)  
    }  
}  
  
func ConsumeHalfMsg(mqTransProducer *mq_http_sdk.MQTransProducer) {  
    for {  
        if loopCount >= 10 {  
            return  
        }  
        loopCount++  
        endChan := make(chan int)  
        respChan := make(chan mq_http_sdk.ConsumeMessageResponse)  
        errChan := make(chan error)  
        go func() {  
            select {  
                case resp := <-respChan:  
                {  
                    // 处理业务逻辑。  
                    var handles []string  
                    fmt.Printf("Consume %d messages---->\n", len(resp.Messages))  
                    for _, v := range resp.Messages {  
                    handles = append(handles, v.ReceiptHandle)  
                    fmt.Printf("\tMessageID: %s, PublishTime: %d, MessageTag: %s\n"+  
                                    "\tConsumedTimes: %d, FirstConsumeTime: %d, NextConsumeTime: %d\n\tBody: %s\n"+  
                                    "\tProperties:%s, Key:%s, Timer:%d, Trans:%d\n",  
                            v.MessageId, v.PublishTime, v.MessageTag, v.ConsumedTimes,  
                            v.FirstConsumeTime, v.NextConsumeTime, v.MessageBody,  
                            v.Properties, v.MessageKey, v.StartDeliverTime, v.TransCheckImmunityTime)  
                      
                    a, _ := strconv.Atoi(v.Properties["a"])  
                    var comRollErr error  
                    if a == 1 {  
                        // 确认提交事务消息。  
                        comRollErr = (*mqTransProducer).Commit(v.ReceiptHandle)  
                        fmt.Println("Commit---------->")  
                    } else if a == 2 && v.ConsumedTimes > 1 {  
                        // 确认提交事务消息。  
                        comRollErr = (*mqTransProducer).Commit(v.ReceiptHandle)  
                        fmt.Println("Commit---------->")  
                    } else if a == 3 {  
                        // 确认回滚事务消息。  
                        comRollErr = (*mqTransProducer).Rollback(v.ReceiptHandle)  
                        fmt.Println("Rollback---------->")  
                    } else {  
                        // 什么都不做，下次再检查。  
                        fmt.Println("Unknown---------->")  
                    }  
                    ProcessError(comRollErr)  
                }  
                    endChan <- 1  
                }  
                case err := <-errChan:  
                {  
                    // Topic中没有消息可消费。  
                    if strings.Contains(err.(errors.ErrCode).Error(), "MessageNotExist") {  
                    fmt.Println("\nNo new message, continue!")  
                } else {  
                    fmt.Println(err)  
                    time.Sleep(time.Duration(3) * time.Second)  
                }  
                    endChan <- 1  
                }  
                case <-time.After(35 * time.Second):  
                {  
                    fmt.Println("Timeout of consumer message ??")  
                    return  
                }  
            }  
        }()  
        // 长轮询检查半事务消息。  
        // 长轮询表示如果Topic没有消息则请求会在服务端挂起3s，3s内如果有消息可以消费则立即返回响应。  
        (*mqTransProducer).ConsumeHalfMessage(respChan, errChan,  
                3, // 一次最多消费3条（最多可设置为16条）。  
                3, // 长轮询时间3秒（最多可设置为30秒）。  
        )  
                <-endChan  
    }  
}  
  
func main() {  
    // 设置HTTP协议客户端接入点，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看。  
    endpoint := "${HTTP_ENDPOINT}"  
    // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
    accessKey := "${ACCESS_KEY}"  
    // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
    secretKey := "${SECRET_KEY}"  
    // 消息所属的Topic，在消息队列RocketMQ版控制台创建。     
//不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
    topic := "${TOPIC}"  
    // Topic所属的实例ID，在消息队列RocketMQ版控制台创建。  
    // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在消息队列RocketMQ版控制台的实例详情页面查看。  
    instanceId := "${INSTANCE_ID}"  
    // 您在控制台创建的Group ID。  
    groupId := "${GROUP_ID}"  
    client := mq_http_sdk.NewAliyunMQClient(endpoint, accessKey, secretKey, "")  
    mqTransProducer := client.GetTransProducer(instanceId, topic, groupId)  
    // 客户端需要有一个线程或者进程来消费没有确认的事务消息。  
    // 启动一个Goroutines来检查没有确认的事务消息。  
    go ConsumeHalfMsg(&mqTransProducer)  
    // 发送4条事务消息，1条发送完就提交，其余3条通过检查半事务消息处理。  
    for i := 0; i < 4; i++ {  
        msg := mq_http_sdk.PublishMessageRequest{  
            MessageBody:"I am transaction msg!",  
                    Properties: map[string]string{"a":strconv.Itoa(i)},  
        }  
        // 设置事务第一次回查的时间，为相对时间。单位：秒，范围：10~300。  
        // 第一次事务回查后如果消息没有Commit或者Rollback，则之后每隔10s左右会回查一次，共回查24小时。  
        msg.TransCheckImmunityTime = 10  
        resp, pubErr := mqTransProducer.PublishMessage(msg)  
        if pubErr != nil {  
            fmt.Println(pubErr)  
            return  
        }  
        fmt.Printf("Publish ---->\n\tMessageId:%s, BodyMD5:%s, Handle:%s\n",  
                resp.MessageId, resp.MessageBodyMD5, resp.ReceiptHandle)  
        if i == 0 {  
            // 发送完事务消息后能获取到半消息句柄，可以直接Commit或Rollback事务消息。  
            ackErr := mqTransProducer.Commit(resp.ReceiptHandle)  
            fmt.Println("Commit---------->")  
            ProcessError(ackErr)  
        }  
    }  
    for ; loopCount < 10 ; {  
        time.Sleep(time.Duration(1) * time.Second)  
    }  
}
```

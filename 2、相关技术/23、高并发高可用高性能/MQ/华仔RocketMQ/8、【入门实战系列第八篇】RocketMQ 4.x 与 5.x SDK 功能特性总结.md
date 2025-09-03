大家好，我是 **华仔**, 又跟大家见面了。

对于 RocketMQ 来说，大家用的最多的还是云服务，今天我们来讲解下 **阿里云版 RocketMQ 4.x 与 5.x SDK 相关功能特性总结，帮助没有使用过的朋友们快速熟悉，下面进入正题**。

# **01 版本差异总结**

云消息队列 RocketMQ 版服务端 4.x 版本和 5.x 版本的差异如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201556960.png)

## **1.1 版本使用限制**
1. 云消息队列 RocketMQ 版 4.x 版本实例依然可以继续使用，但仅支持历史 3.x 和 4.x 版本的SDK访问。
2. 云消息队列 RocketMQ 版 5.x 版本兼容大部分存量TCP协议SDK，具体参数使用差异和细节约束，请参见 [5.x版本SDK参考概述](https://help.aliyun.com/document_detail/441918.htm#concept-2234978)。
3. 当前**暂不支持 4.x 版本实例原地升级为 5.x 版本，**如需升级实例版本，建议您创建新的5.x版本实例，并分批迁移业务流量至新的 5.x 版本实例。

## **1.2 SDK 兼容性约束**

服务端版本和各客户端SDK版本的兼容情况如下表所示。

例如您购买了 5.x 版本的实例：
1. 若您的客户端使用的是 5.x 对应的最新版SDK，则功能 **5.x 版本服务端功能全部支持**。
2. 若您的客户端使用 **4.x 版本对应的 SDK**，则基础消息收发功能支持，**应付费监控指标、消息轨迹数据无法获取**。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201558575.png)

## **1.3 功能行为约束**

云消息队列 RocketMQ 版5.x版本实例基于大规模企业客户的生产实践经验，对消息收发流程中的部分功能行为进行了优化调整。因此，部分场景下的参数配置、功能行为会有差异，一般情况下不影响主要消息收发链路，对于存量4.x版本升级到5.x版本，请您根据业务情况评估风险。

具体的功能行为差异如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201558911.png)

## **1.4 版本升级说明**

当前**暂不支持使用升级工具将 4.x 版本实例原地升级为 5.x 版本**。如果您需要将存量实例升级至 5.x 版本实例，建议您参考如下流程购买 5.x 版本实例，逐步将业务流量迁移至新的实例。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201558052.png)

业务迁移时，您可以**参考以下双读双写、分批发布**的方案进行操作。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201559977.png)

# **02 领域模型概述**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201600283.png)

如上图所示，云消息队列 RocketMQ 版中消息的生命周期主要分为「**消息生产**」、「**消息存储**」、「**消息消费**」这三部分。

简单流程：生产者生产消息并发送至云消息队列 RocketMQ 版服务端，消息被存储在服务端的主题 Topic 中，消费者通过订阅主题 Topic 进行消费消息。

## **2.1 消息生产**

[生产者（Producer）](https://help.aliyun.com/document_detail/444758.htm#concept-2217221)：云消息队列 RocketMQ 版中用于产生消息的运行实体，一般集成于业务调用链路的上游。生产者是轻量级匿名无身份的。

## **2.2 消息存储**

1. [主题（Topic）](https://help.aliyun.com/document_detail/440309.htm#concept-2214802)：云消息队列 RocketMQ 版消息传输和存储的分组容器，主题内部由多个队列组成，消息的存储和水平扩展实际是通过主题内的队列实现的。
2. [队列（MessageQueue）](https://help.aliyun.com/document_detail/440344.htm#concept-2214805)：云消息队列 RocketMQ 版消息传输和存储的实际单元容器，类比于Kafka中的分区。云消息队列 RocketMQ 版通过流式特性的无限队列结构来存储消息，消息在队列内具备顺序性存储特征。
3. [消息（Message）](https://help.aliyun.com/document_detail/440345.htm#concept-2214804)：云消息队列 RocketMQ 版的最小传输单元。消息具备不可变性，在初始化发送和完成存储后即不可变。

## **2.3 消息消费**

1. [消费者分组（ConsumerGroup）](https://help.aliyun.com/document_detail/440346.htm#concept-2217224)：云消息队列 RocketMQ 版发布订阅模型中定义的独立的消费身份分组，用于统一管理底层运行的多个消费者（Consumer）。同一个消费组的多个消费者必须保持消费逻辑和配置一致，共同分担该消费组订阅的消息，实现消费能力的水平扩展。
2. [消费者（Consumer）](https://help.aliyun.com/document_detail/444759.htm#concept-2217222)：云消息队列 RocketMQ 版消费消息的运行实体，一般集成在业务调用链路的下游。消费者必须被指定到某一个消费组中。
3. [订阅关系（Subscription）](https://help.aliyun.com/document_detail/444760.htm#concept-2217225)：云消息队列 RocketMQ 版发布订阅模型中消息过滤、重试、消费进度的规则配置。订阅关系以消费组粒度进行管理，消费组通过定义订阅关系控制指定消费组下的消费者如何实现消息过滤、消费重试及消费进度恢复等。
4. 云消息队列 RocketMQ 版的订阅关系除过滤表达式之外都是持久化的，即服务端重启或请求断开，订阅关系依然保留。

## **2.4 消息传输模型**

主流的消息中间件的传输模型主要为「**点对点模型**」、「**发布订阅模型**」两种。

### **2.4.1 点对点模型**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201619055.png)

点对点模型也叫队列模型，具有如下特点：
1. 消费匿名：消息上下游沟通的唯一的身份就是队列，下游消费者从队列获取消息无法申明独立身份。
2. 一对一通信：基于消费匿名特点，下游消费者即使有多个，但都没有自己独立的身份，因此共享队列中的消息，每一条消息都只会被唯一一个消费者处理。因此点对点模型只能实现一对一通信。

### **2.4.2 发布订阅模型**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201621584.png)

发布订阅模型具有如下特点：
1. 消费独立：相比队列模型的匿名消费方式，发布订阅模型中消费方都会具备的身份，一般叫做订阅组（订阅关系），不同订阅组之间相互独立不会相互影响。
2. 一对多通信：基于独立身份的设计，同一个主题内的消息可以被多个订阅组处理，每个订阅组都可以拿到全量消息。因此发布订阅模型可以实现一对多通信。

「**点对点模型**」、「**发布订阅模型**」各有优势，点对点模型更为简单，而发布订阅模型的扩展性更高。云消息队列 RocketMQ 版使用的传输模型为「**发布订阅模型**」，因此也具有「**发布订阅模型**」的特点。

接下来，我们来看 2 个重要概念，「**队列**」、「**订阅关系**」。

## **2.5 队列 messagequeue**

首先队列 messagequeue 是RocketMQ 消息存储和传输的实际容器，也是 RocketMQ 中消息的最小存储单元。

对于云消息队列 RocketMQ 版的所有主题都是由多个队列组成，以此实现「**队列数量的水平拆分**」和「**队列内部的流式存储**」。

队列的主要作用如下：
1. 存储顺序性：队列天然具备顺序性，即消息按照进入队列的顺序写入存储，同一队列间的消息天然存在顺序关系，队列头部为最早写入的消息，队列尾部为最新写入的消息。消息在队列中的位置和消息之间的顺序通过位点（Offset）进行标记管理。
2. 流式操作语义：云消息队列 RocketMQ 版基于队列的存储模型可确保消息从任意位点读取任意数量的消息，以此实现类似聚合读取、回溯读取等特性，这些特性是RabbitMQ、ActiveMQ等非队列存储模型不具备的。

### **2.5.1 模型关系**

其模型关系如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201642530.png)

云消息队列 RocketMQ 版默认提供「**消息可靠存储机制**」，所有发送成功的消息都被持久化存储到队列中，配合生产者和消费者客户端的调用可实现至少投递一次的可靠性语义。

此处的队列 messagequeue 类似 Kafka 中的分区 parititon 概念，在云消息队列 RocketMQ 版消息收发模型中，队列属于主题的一部分，虽然所有的消息资源以主题粒度管理，但实际的操作实现是面向队列。

例如，生产者指定某个主题，向主题内发送消息，但实际消息发送到该主题下的某个队列中。

云消息队列 RocketMQ 版中通过修改队列数量，以此实现横向的水平扩容和缩容。

### **2.5.2 版本兼容性**

队列的名称属性在云消息队列 RocketMQ 版服务端的不同版本中有如下差异：
1. 服务端3.x/4.x版本：队列名称由{主题名称}+{BrokerID}+{QueueID}三元组组成，和物理节点绑定。
2. 服务端5.x版本：队列名称为一个集群分配的全局唯一的字符串组成，和物理节点解耦。

因此，在开发过程中，建议不要对队列名称做任何假设和绑定。**消如果你在代码中自定义拼接队列名词和其他操作进行绑定，一旦服务端版本升级后，可能会出现队列名词无法解析的兼容性问题，需要你注意下**。

### **2.5.3 使用建议**

**按照实际业务消耗设置队列数**

云消息队列 RocketMQ 版的应遵循**少用够用原则，避免随意增加队列数量**。

主题内**队列数过多**可能对导致如下问题：
1. 集群元数据膨胀：云消息队列 RocketMQ 版会以队列粒度采集指标和监控数据，队列过多容易造成管控元数据膨胀。
2. 客户端压力过大：云消息队列 RocketMQ 版的消息读写都是针对队列进行操作，队列过多容易产生空轮询请求，增加系统负荷。

**常见队列增加场景如下：**
1. 需要增加队列实现物理节点负载均衡。
2. 云消息队列 RocketMQ 版每个主题的多个队列可以分布在不同的服务节点上，在集群水平扩容增加节点后，为了保证集群流量的负载均衡，建议在新的服务节点上新增队列，或将旧的队列迁移到新的服务节点上。
3. 需要增加队列实现顺序消息性能扩展。
4. 在云消息队列 RocketMQ 版服务端 4.x 版本中，顺序消息的顺序性在队列内生效的，因此顺序消息的并发度会在一定程度上受队列数量的影响，因此建议仅在系统性能瓶颈时再增加队列。
5. 非顺序消息消费的负载均衡与队列数无关。
6. 云消息队列 RocketMQ 版服务端 5.x 系列实例的消费者负载均衡已升级为消息粒度负载均衡，同一队内的消息可以被所有消费者均匀消费。因此，您无需担心队列数对消费并发度的影响。负载均衡详细信息，请参见 [消费者负载均衡](https://help.aliyun.com/document_detail/444763.htm#concept-2224298)。

## **2.6 订阅关系**

订阅关系是云消息队列 RocketMQ 版系统中消费者「**获取消息**」、「**队处理消息规则**」、「**状态配置**」。

订阅关系由消费者分组动态注册到服务端系统，并在后续的消息传输中按照订阅关系定义的过滤规则进行消息匹配和消费进度维护。

通过配置订阅关系，可控制如下传输行为：
1. 消息过滤规则：用于控制消费者在消费消息时，选择主题内的哪些消息进行消费，设置消费过滤规则可以高效地过滤消费者需要的消息集合，灵活根据不同的业务场景设置不同的消息接收范围。具体信息，请参见[消息过滤](https://help.aliyun.com/document_detail/444765.htm#concept-2228659)。
2. 消费状态：云消息队列 RocketMQ 版服务端默认提供订阅关系持久化的能力，即消费者分组在服务端注册订阅关系后，当消费者离线并再次上线后，可以获取离线前的消费进度并继续消费。

### **2.6.1 订阅关系判断原则**

云消息队列 RocketMQ 版的订阅关系按照「**消费者组**」、「**主题粒度**」设计，因此一个订阅关系指的是**度指定某个消费者组对于某个主题的订阅关系**，判断原则如下：
1. 不同消费者组对于同一个主题的订阅相互独立。如下图所示，消费者组 Group A 和消费者组 Group B 分别以不同的订阅关系订阅了同一个主题Topic A，这两个订阅关系互相独立，可以各自定义，不受影响。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201657891.png)
2. 同一个消费者组对于不同主题的订阅也相互独立。如下图所示，消费者组 Group A 订阅了两个主题 Topic A 和 Topic B，对于 Group A 中的消费者来说，订阅的 Topic A 为一个订阅关系，订阅的 Topic B 为另外一个订阅关系，且这两个订阅关系互相独立，可以各自定义，不受影响。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201657953.png)

### **2.6.2 模型关系**

在云消息队列 RocketMQ 版的领域模型中，订阅关系的位置和流程如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201658890.png)

1. 消息由生产者初始化并发送到云消息队列 RocketMQ 版服务端。
2. 消息按照到达云消息队列 RocketMQ 版服务端的顺序存储到主题的指定队列中。
3. 消费者按照指定的订阅关系从云消息队列 RocketMQ 版服务端中获取消息并消费。

### **2.6.3 订阅关系一致**

云消息队列 RocketMQ 版是按照消费者分组粒度管理订阅关系，因此，同一消费者分组内的消费者在消费逻辑上必须保持一致，否则会出现消费冲突，导致部分消息消费异常。
```java
// 正确示例：

//Consumer c1Consumer c1 = ConsumerBuilder.build(groupA);
c1.subscribe(topicA,"TagA");
//Consumer c2Consumer c2 = ConsumerBuilder.build(groupA);
c2.subscribe(topicA,"TagA");

// 错误示例

//Consumer c1Consumer c1 = ConsumerBuilder.build(groupA);
c1.subscribe(topicA,"TagA");
//Consumer c2Consumer c2 = ConsumerBuilder.build(groupA);
c2.subscribe(topicA,"TagB");
```

### **2.6.4 使用建议**

**建议不要频繁修改订阅关系**

在云消息队列 RocketMQ 版领域模型中，订阅关系关联了「**过滤规则**」、「**消费进度**」等元数据和相关配置，同时系统需要保证消费者分组下的所有消费者的「**消费行为**」、「**消费逻辑**」、「**负载策略**」等一致，整体运算逻辑比较复杂。

因此，不建议在生产环境中通过**频繁修改订阅关系**来实现业务逻辑的变更，这样可能会导致客户端一直处于负载均衡调整和变更的过程，从而影响消息接收。

接下来，我们来看看几种消息类型的具体实现方式，以及版本差异。

# **03 普通消息**

普通消息为云消息队列 RocketMQ 版中最基础的消息，区别于有特性的顺序消息、定时/延时消息和事务消息。

## **3.1 应用场景**

普通消息一般应用于「**微服务解耦**」、「**事件驱动**」、「**数据集成**」等场景，这些场景大多数要求数据传输通道具有可靠传输的能力，且对消息的处理时机、处理顺序没有特别要求。

**典型场景一：微服务异步解耦**
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201700784.png)

如上图所示，以**在线电商交易场景**为例，上游订单系统将用户下单支付这一业务事件封装成独立的普通消息并发送至云消息队列 RocketMQ 版服务端，下游按需从服务端订阅消息并按照本地消费逻辑处理下游任务。每个消息之间都是相互独立的，且不需要产生关联。

**典型场景二：数据集成传输**
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201701611.png)

如上图所示，以**离线的日志收集场景**为例，通过埋点组件收集前端应用的相关操作日志，并转发到云消息队列 RocketMQ 版。每条消息都是一段日志数据，云消息队列 RocketMQ 版不做任何处理，只需要将日志数据可靠投递到下游的存储系统和分析系统即可，后续功能由后端应用完成。

## **3.2 使用示例**

普通消息支持设置「**消息索引键**」、「**消息过滤标签**」等信息，用于消息过滤和搜索查找。

这里以 Java 语言为例，5.x 版本收发普通消息的示例代码如下：
```java
//普通消息发送。  
MessageBuilder messageBuilder = new MessageBuilder();  
Message message = messageBuilder.setTopic("topic")  
        //设置消息索引键，可根据关键字精确查找某条消息。  
        .setKeys("messageKey")  
        //设置消息Tag，用于消费端根据指定Tag过滤消息。  
        .setTag("messageTag")  
        //消息体。  
        .setBody("messageBody".getBytes())  
        .build();  
try {  
//发送消息，需要关注发送结果，并捕获失败等异常。  
SendReceipt sendReceipt = producer.send(message);  
    System.out.println(sendReceipt.getMessageId());  
        } catch (ClientException e) {  
        e.printStackTrace();  
}  
  
//消费示例一：使用PushConsumer消费普通消息，只需要在消费监听器中处理即可。  
MessageListener messageListener = new MessageListener() {  
    @Override  
    public ConsumeResult consume(MessageView messageView) {  
        System.out.println(messageView);  
        //根据消费结果返回状态。  
        return ConsumeResult.SUCCESS;  
    }  
};  
  
//消费示例二：使用SimpleConsumer消费普通消息，主动获取消息进行消费处理并提交消费结果。  
List<MessageView> messageViewList = null;  
try {  
messageViewList = simpleConsumer.receive(10, Duration.ofSeconds(30));  
        messageViewList.forEach(messageView -> {  
        System.out.println(messageView);  
//消费处理完成后，需要主动调用ACK提交消费结果。  
        try {  
                simpleConsumer.ack(messageView);  
        } catch (ClientException e) {  
        e.printStackTrace();  
        }  
                });  
                } catch (ClientException e) {  
        //如果遇到系统流控等原因造成拉取失败，需要重新发起获取消息请求。  
        e.printStackTrace();  
}
```

4.x 版本收发普通消息的示例代码如下：
```java
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQProducer;  
import com.aliyun.mq.http.model.TopicMessage;  
import java.util.Date;  
// 普通消息发送。  
public class Producer {  
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP接入域名（此处以公共云生产环境为例）  
                "${HTTP_ENDPOINT}",  
                // AccessKey 阿里云身份验证，在阿里云服务器管理控制台创建  
                "${ACCESS_KEY}",  
                // SecretKey 阿里云身份验证，在阿里云服务器管理控制台创建  
                "${SECRET_KEY}"  
        );  
          
        // 所属的 Topic        final String topic = "${TOPIC}";  
        // Topic所属实例ID，默认实例为空  
        final String instanceId = "${INSTANCE_ID}";  
          
        // 获取Topic的生产者  
        MQProducer producer;  
        if (instanceId != null && instanceId != "") {  
            producer = mqClient.getProducer(instanceId, topic);  
        } else {  
            producer = mqClient.getProducer(topic);  
        }  
          
        try {  
            // 循环发送4条消息  
            for (int i = 0; i < 4; i++) {  
                TopicMessage pubMsg;  
                if (i % 2 == 0) {  
                    // 普通消息  
                    pubMsg = new TopicMessage(  
                            // 消息内容  
                            "hello mq!".getBytes(),  
                            // 消息标签  
                            "A"  
                    );  
                    // 设置属性  
                    pubMsg.getProperties().put("a", String.valueOf(i));  
                    // 设置KEY  
                    pubMsg.setMessageKey("MessageKey");  
                } else {  
                    pubMsg = new TopicMessage(  
                            // 消息内容  
                            "hello mq!".getBytes(),  
                            // 消息标签  
                            "A"  
                    );  
                    // 设置属性  
                    pubMsg.getProperties().put("a", String.valueOf(i));  
                    // 定时消息, 定时时间为10s后  
                    pubMsg.setStartDeliverTime(System.currentTimeMillis() + 10 * 1000);  
                }  
                // 同步发送消息，只要不抛异常就是成功  
                TopicMessage pubResultMsg = producer.publishMessage(pubMsg);  
                  
                // 同步发送消息，只要不抛异常就是成功  
                System.out.println(new Date() + " Send mq message success. Topic is:" + topic + ", msgId is: " + pubResultMsg.getMessageId()  
                        + ", bodyMD5 is: " + pubResultMsg.getMessageBodyMD5());  
            }  
        } catch (Throwable e) {  
            // 消息发送失败，需要进行重试处理，可重新发送这条消息或持久化这条数据进行补偿处理  
            System.out.println(new Date() + " Send mq message failed. Topic is:" + topic);  
            e.printStackTrace();  
        }  
        mqClient.close();  
    }  
}  
  
  
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQConsumer;  
import com.aliyun.mq.http.common.AckMessageException;  
import com.aliyun.mq.http.model.Message;  
import java.util.ArrayList;  
import java.util.List;  
  
// 消费示例  
public class Consumer {  
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP接入域名（此处以公共云生产环境为例）  
                "${HTTP_ENDPOINT}",  
                // AccessKey 阿里云身份验证，在阿里云服务器管理控制台创建  
                "${ACCESS_KEY}",  
                // SecretKey 阿里云身份验证，在阿里云服务器管理控制台创建  
                "${SECRET_KEY}"  
        );  
          
        // 所属的 Topic        final String topic = "${TOPIC}";  
        // 您在控制台创建的 Consumer ID(Group ID)        final String groupId = "${GROUP_ID}";  
        // Topic所属实例ID，默认实例为空  
        final String instanceId = "${INSTANCE_ID}";  
          
        final MQConsumer consumer;  
        if (instanceId != null && instanceId != "") {  
            consumer = mqClient.getConsumer(instanceId, topic, groupId, null);  
        } else {  
            consumer = mqClient.getConsumer(topic, groupId);  
        }  
          
        // 在当前线程循环消费消息，建议是多开个几个线程并发消费消息  
        do {  
            List<Message> messages = null;  
              
            try {  
                // 长轮询消费消息  
                // 长轮询表示如果topic没有消息则请求会在服务端挂住3s，3s内如果有消息可以消费则立即返回  
                messages = consumer.consumeMessage(  
                        3,// 一次最多消费3条(最多可设置为16条)  
                        3// 长轮询时间3秒（最多可设置为30秒）  
                );  
            } catch (Throwable e) {  
                e.printStackTrace();  
                try {  
                    Thread.sleep(2000);  
                } catch (InterruptedException e1) {  
                    e1.printStackTrace();  
                }  
            }  
            // 没有消息  
            if (messages == null || messages.isEmpty()) {  
                System.out.println(Thread.currentThread().getName() + ": no new message, continue!");  
                continue;  
            }  
              
            // 处理业务逻辑  
            for (Message message : messages) {  
                System.out.println("Receive message: " + message);  
            }  
              
            // Message.nextConsumeTime前若不确认消息消费成功，则消息会重复消费  
            // 消息句柄有时间戳，同一条消息每次消费拿到的都不一样  
            {  
                List<String> handles = new ArrayList<String>();  
                for (Message message : messages) {  
                    handles.add(message.getReceiptHandle());  
                }  
                  
                try {  
                    consumer.ackMessage(handles);  
                } catch (Throwable e) {  
                    // 某些消息的句柄可能超时了会导致确认不成功  
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

4.x 代码地址：[https://help.aliyun.com/document_detail/201449.html?spm=a2c4g.201500.0.0.3870112bTq5V48](https://help.aliyun.com/document_detail/201449.html?spm=a2c4g.201500.0.0.3870112bTq5V48)

5.x 代码地址：[https://help.aliyun.com/document_detail/442626.html?spm=a2c4g.441918.0.0.67d9e8cfXnCg1r](https://help.aliyun.com/document_detail/442626.html?spm=a2c4g.441918.0.0.67d9e8cfXnCg1r)

## **3.3 使用建议**

**设置全局唯一的业务索引键，方便问题追踪**

云消息队列 RocketMQ 版支持自定义索引键（消息的Key），在消息查询和轨迹查询时，可以通过索引键高效精确地查询到消息。

因此，发送消息时，建议设置业务上唯一的信息作为索引，方便后续快速定位消息。例如，订单ID，用户ID等。

# **04 顺序消息**

顺序消息为云消息队列 RocketMQ 版中的高级特性消息。

## **4.1 应用场景**

在「**有序事件处理**」、「**撮合交易**」、「**数据实时增量同步**」等场景下，异构系统间需要维持**强一致**的状态同步，上游的事件变更需要**按照顺序**传递到下游进行处理。在这类场景下使用云消息队列 RocketMQ 版的顺序消息可以有效保证数据传输的顺序性。

**典型场景一：撮合交易**
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201704149.png)

以**证券、股票交易撮合场景**为例，对于出价相同的交易单，坚持按照**先出价先交易**的原则，下游处理订单的系统需要严格按照**出价顺序**来处理订单。

**典型场景二：数据实时增量同步**

图 1. 普通消息
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201704962.png)

图 2. 顺序消息
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201704304.png)

这里以**数据库变更增量同步场景**为例，上游源端数据库按需执行增删改操作，将二进制操作日志作为消息，通过云消息队列 RocketMQ 版传输到下游搜索系统，下游系统按**顺序还原消息数据**，实现状态数据按序刷新。如果是普通消息则可能会导致状态混乱，和预期操作结果不符，基于顺序消息可以实现下游状态和上游操作结果一致。

接下来，我们看看什么是顺序消息，它有什么特色。

顺序消息是云消息队列 RocketMQ 版提供的一种高级消息类型，**支持消费者按照发送消息的先后顺序获取消息**，从而实现业务场景中的顺序处理。

相比其他类型消息，**顺序消息在发送、存储、投递的处理过程中，更多强调多条消息的先后顺序关系**。

云消息队列 RocketMQ 版顺序消息的顺序关系通过**消息组**（MessageGroup）来判定和识别，发送顺序消息时需要**为每条消息设置归属的消息组**。

> 注意： 只有同一消息组的消息才能保证顺序，不同消息组或未设置消息组的消息之间不涉及顺序性。

基于消息组的顺序判定逻辑，支持按照业务逻辑做细粒度拆分，可以在满足业务局部顺序的前提下提高系统的并行度和吞吐能力，那么如何保证消息的顺序性，下面来看看。

## **4.2 如何保证消息的顺序性**

云消息队列 RocketMQ 版的消息的顺序性分为两部分，「**生产顺序性**」、「**消费顺序性**」。

### **4.2.1 生产顺序性**

云消息队列 RocketMQ 版通过生产者和服务端的协议保障**单个生产者串行地发送消息，并按序存储和持久化**。

如需保证消息生产的顺序性，则**必须满足以下条件**：
1. 同一消息组（MessageGroup）：消息生产顺序性的范围为消息组，生产者发送消息时可以为每条消息设置消息组，只有同一消息组内的消息可以保证顺序性，不同消息组或未设置消息组的消息之间不保证顺序。
2. 单一生产者：消息生产的顺序性仅支持单一生产者，不同生产者分布在不同的系统，即使设置相同的消息组，不同生产者之间产生的消息也无法判定其先后顺序。
3. 串行发送：云消息队列 RocketMQ 版生产者客户端支持多线程安全访问，但如果生产者使用多线程并行发送，则不同线程间产生的消息将无法判定其先后顺序。

满足以上条件的生产者，将顺序消息发送至云消息队列 RocketMQ 版后，会**保证设置了同一个消息组地消息**，**按照发送顺序存储在同一个队列中**。服务端顺序存储逻辑如下：
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201705189.png)

1. 相同消息组的消息按照先后顺序被存储在同一个队列。
2. 不同消息组的消息可以混合在同一个队列中，且不保证连续。

如上图所示，消息组1和消息组4的消息混合存储在队列1中，云消息队列 RocketMQ 版保证消息组1中的消息 G1-M1、G1-M2、G1-M3 是按发送顺序存储，且消息组4的消息 G4-M1、G4-M2 也是按顺序存储，但消息组1和消息组4中的消息不涉及顺序关系。

### **4.2.2 消费顺序性**

云消息队列 RocketMQ 版通过消费者和服务端的协议保障**消息消费严格按照存储地先后顺序来处理**。

如需保证消息消费的顺序性，则**必须满足以下条件**：
1. 投递顺序：云消息队列 RocketMQ 版通过客户端SDK和服务端通信协议保障消息按照服务端存储顺序投递，但业务方消费消息时需要严格按照接收—处理—应答的语义处理消息，避免因异步处理导致消息乱序。
2. 有限重试：云消息队列 RocketMQ 版顺序消息投递仅在重试次数限定范围内，即一条消息如果一直重试失败，超过最大重试次数后将不再重试，跳过这条消息消费，不会一直阻塞后续消息处理。
3. 对于需要严格保证消费顺序的场景，请务设置合理的重试次数，避免参数不合理导致消息乱序。

> 这里需要注意的是：
> 
> 消费者类型为 PushConsumer 时，云消息队列 RocketMQ 版保证消息按照存储顺序一条一条投递给消费者，
> 
> 消费者类型为 SimpleConsumer 时，则消费者有可能一次拉取多条消息。
> 
> 此时，消息消费的顺序性需要由业务方自行保证。

### **4.2.3 生产顺序性和消费顺序性**

如果消息需要严格按照先进先出（FIFO）的原则处理，即先发送的先消费、后发送的后消费，则必须要同时满足生产顺序性和消费顺序性。但一般业务场景下，同一个生产者可能对接多个下游消费者，不一定所有的消费者业务都需要顺序消费，您可以将生产顺序性和消费顺序性进行差异化组合，应用于不同的业务场景。例如发送顺序消息，但使用非顺序的并发消费方式来提高吞吐能力。更多组合方式如下表所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201706488.png)

## **4.3 使用限制**

顺序消息仅支持使用**MessageType**为**FIFO**的主题，即**顺序消息只能发送至类型为顺序消息的主题中**，发送的消息的类型必须和主题的类型一致。

## **4.4 使用示例**

和普通消息发送相比，**顺序消息发送必须设置消息组**。消息组的粒度建议按照业务场景，尽可能细粒度设计，以便实现业务拆分和并发扩展。

以Java语言为例，5.x 版本收发顺序消息的示例代码如下：
```java
//顺序消息发送。  
MessageBuilder messageBuilder = null;  
Message message = messageBuilder.setTopic("topic")  
        //设置消息索引键，可根据关键字精确查找某条消息。  
        .setKeys("messageKey")  
        //设置消息Tag，用于消费端根据指定Tag过滤消息。  
        .setTag("messageTag")  
        //设置顺序消息的排序分组，该分组尽量保持离散，避免热点排序分组。  
        .setMessageGroup("fifoGroup001")  
        //消息体。  
        .setBody("messageBody".getBytes())  
        .build();  
try {  
//发送消息，需要关注发送结果，并捕获失败等异常  
SendReceipt sendReceipt = producer.send(message);  
    System.out.println(sendReceipt.getMessageId());  
        } catch (ClientException e) {  
        e.printStackTrace();  
}  
//消费顺序消息时，需要确保当前消费者分组是顺序投递模式，否则仍然按并发乱序投递。  
  
//消费示例一：使用PushConsumer消费顺序消息，只需要在消费监听器处理即可。  
MessageListener messageListener = new MessageListener() {  
    @Override  
    public ConsumeResult consume(MessageView messageView) {  
        System.out.println(messageView);  
        //根据消费结果返回状态。  
        return ConsumeResult.SUCCESS;  
    }  
};  
  
//消费示例二：使用SimpleConsumer消费顺序消息，主动获取消息进行消费处理并提交消费结果。  
//需要注意的是，同一个MessageGroup的消息，如果前序消息没有消费完成，再次调用Receive是获取不到后续消息的。  
List<MessageView> messageViewList = null;  
try {  
messageViewList = simpleConsumer.receive(10, Duration.ofSeconds(30));  
        messageViewList.forEach(messageView -> {  
        System.out.println(messageView);  
//消费处理完成后，需要主动调用ACK提交消费结果。  
       try {  
                simpleConsumer.ack(messageView);  
       } catch (ClientException e) {  
        e.printStackTrace();  
       }  
                });  
                } catch (ClientException e) {  
        //如果遇到系统流控等原因造成拉取失败，需要重新发起获取消息请求。  
        e.printStackTrace();  
}
```

4.x 版本收发顺序消息的示例代码如下：
```java
// 发送消息  
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQProducer;  
import com.aliyun.mq.http.model.TopicMessage;  
import java.util.Date;  
public class OrderProducer {  
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入云消息队列 RocketMQ 版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在云消息队列 RocketMQ 版控制台创建。  
        // 不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // Topic所属的实例ID，在云消息队列 RocketMQ 版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在云消息队列 RocketMQ 版控制台的实例详情页面查看。  
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
  
  
//订阅消息  
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQConsumer;  
import com.aliyun.mq.http.common.AckMessageException;  
import com.aliyun.mq.http.model.Message;  
import java.util.ArrayList;  
import java.util.List;  
public class OrderConsumer {  
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入云消息队列 RocketMQ 版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在云消息队列 RocketMQ 版控制台创建。  
        //不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // 您在云消息队列 RocketMQ 版控制台创建的Group ID。  
        final String groupId = "${GROUP_ID}";  
        // Topic所属的实例ID，在云消息队列 RocketMQ 版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在云消息队列 RocketMQ 版控制台的实例详情页面查看。  
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
  
4.x 代码地址：[https://help.aliyun.com/document_detail/201500.html?spm=a2c4g.201501.0.0.70333b2eeSsGOd](https://help.aliyun.com/document_detail/201500.html?spm=a2c4g.201501.0.0.70333b2eeSsGOd)

5.x 代码地址：[https://help.aliyun.com/document_detail/442626.html?spm=a2c4g.441918.0.0.67d9e8cfXnCg1r](https://help.aliyun.com/document_detail/442626.html?spm=a2c4g.441918.0.0.67d9e8cfXnCg1r)

**这里简单总结下：5.x 版本是通过设置消息组来实现，而 4.x 则是通过设置 shardingkey 来实现，后面会单篇剖析其底层实现。**

# **05 定时/延时消息**

定时/延时消息为云消息队列 RocketMQ 版中的高级特性消息。

## **5.1 应用场景**

在分布式定时调度触发、任务超时处理等场景，需要实现精准、可靠的定时事件触发。使用云消息队列 RocketMQ 版的定时消息可以简化定时调度任务的开发逻辑，实现高性能、可扩展、高可靠的定时触发能力。

**典型场景一：分布式定时调度**
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201708871.png)

在分布式定时调度场景下，需要实现各类精度的定时任务，例如每天5点执行文件清理，每隔2分钟触发一次消息推送等需求。传统基于数据库的定时调度方案在分布式场景下，性能不高，实现复杂。基于云消息队列 RocketMQ 版的定时消息可以封装出多种类型的定时触发器。

**典型场景二：任务超时处理**
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201708039.png)


以电商交易场景为例，订单下单后暂未支付，此时不可以直接关闭订单，而是需要等待一段时间后才能关闭订单。使用云消息队列 RocketMQ 版定时消息可以实现超时任务的检查触发。

基于定时消息的超时任务处理具备如下优势：
1. 精度高、开发门槛低：基于消息通知方式不存在定时阶梯间隔。可以轻松实现任意精度事件触发，无需业务去重。
2. 高性能可扩展：传统的数据库扫描方式较为复杂，需要频繁调用接口扫描，容易产生性能瓶颈。云消息队列 RocketMQ 版的定时消息具有高并发和水平扩展的能力。

## **5.2 功能原理实现**

### **5.2.1 什么是定时消息**

定时消息是云消息队列 RocketMQ 版提供的一种高级消息类型，消息被发送至服务端后，在指定时间后才能被消费者消费。通过设置一定的定时时间可以实现分布式场景的延时调度触发效果。

### **5.2.2 时间设置规则**

1. 云消息队列 RocketMQ 版定时消息设置的定时时间是一个预期触发的系统时间戳，延时时间也需要转换成当前系统时间后的某一个时间戳，而不是一段延时时长。
2. 定时时间的格式为毫秒级的Unix时间戳，您需要将要设置的时刻转换成时间戳形式。具体方式，请参见[Unix时间戳转换工具](https://www.unixtimestamp.com/)。
3. 定时时间必须设置在定时时长范围内，超过范围则定时不生效，服务端会立即投递消息。
4. 定时时长最大值默认为24小时，不支持自定义修改，更多信息，请参见[参数限制](https://help.aliyun.com/document_detail/440347.htm#section-0x5-01t-nv7)。
5. 定时时间必须设置为当前时间之后，若设置到当前时间之前，则定时不生效，服务端会立即投递消息。

示例如下：
1. 定时消息：例如，当前系统时间为2022-06-09 17:30:00，您希望消息在下午19:20:00定时投递，则定时时间为2022-06-09 19:20:00，转换成时间戳格式为1654773600000。
2. 延时消息：例如，当前系统时间为2022-06-09 17:30:00，您希望延时1个小时后投递消息，则您需要根据当前时间和延时时长换算成定时时刻，即消息投递时间为2022-06-09 18:30:00，转换为时间戳格式为1654770600000。

## **5.3 使用限制**

### **5.3.1 消息类型一致性**

定时消息仅支持在MessageType为Delay的主题内使用，即定时消息只能发送至类型为定时消息的主题中，发送的消息的类型必须和主题的类型一致。

### **5.3.2 定时精度约束**

云消息队列 RocketMQ 版定时消息的定时时长参数精确到毫秒级，但是默认精度为 1000 ms，即定时消息为秒级精度。

云消息队列 RocketMQ 版定时消息的状态支持持久化存储，系统由于故障重启后，仍支持按照原来设置的定时时间触发消息投递。若存储系统异常重启，可能会导致定时消息投递出现一定延迟。

## **5.3 使用示例**

和普通消息相比，定时消费发送时，必须设置定时触发的目标时间戳。

这里以 Java 语言为例，5.x 版本收发定时消息示例参考如下：
```java
//定时/延时消息发送  
MessageBuilder messageBuilder = null;  
//以下示例表示：延迟时间为10分钟之后的Unix时间戳。  
Long deliverTimeStamp = System.currentTimeMillis() + 10L * 60 * 1000;  
Message message = messageBuilder.setTopic("topic")  
        //设置消息索引键，可根据关键字精确查找某条消息。  
        .setKeys("messageKey")  
        //设置消息Tag，用于消费端根据指定Tag过滤消息。  
        .setTag("messageTag")  
        .setDeliveryTimestamp(deliverTimeStamp)  
        //消息体  
        .setBody("messageBody".getBytes())  
        .build();  
try {  
//发送消息，需要关注发送结果，并捕获失败等异常。  
SendReceipt sendReceipt = producer.send(message);  
    System.out.println(sendReceipt.getMessageId());  
        } catch (ClientException e) {  
        e.printStackTrace();  
}  
  
//消费示例一：使用PushConsumer消费定时消息，只需要在消费监听器处理即可。  
MessageListener messageListener = new MessageListener() {  
    @Override  
    public ConsumeResult consume(MessageView messageView) {  
        System.out.println(messageView.getDeliveryTimestamp());  
        //根据消费结果返回状态。  
        return ConsumeResult.SUCCESS;  
    }  
};  
  
//消费示例二：使用SimpleConsumer消费定时消息，主动获取消息进行消费处理并提交消费结果。  
List<MessageView> messageViewList = null;  
try {  
messageViewList = simpleConsumer.receive(10, Duration.ofSeconds(30));  
        messageViewList.forEach(messageView -> {  
        System.out.println(messageView);  
//消费处理完成后，需要主动调用ACK提交消费结果。  
       try {  
                simpleConsumer.ack(messageView);  
       } catch (ClientException e) {  
        e.printStackTrace();  
       }  
                });  
                } catch (ClientException e) {  
        //如果遇到系统流控等原因造成拉取失败，需要重新发起获取消息请求。  
        e.printStackTrace();  
}
```

4.x 版本收发定时/延时消息的示例代码如下：
```java
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQProducer;  
import com.aliyun.mq.http.model.TopicMessage;  
  
import java.util.Date;  
  
public class Producer {  
      
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入云消息队列 RocketMQ 版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在云消息队列 RocketMQ 版控制台创建。  
        //不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // Topic所属的实例ID，在云消息队列 RocketMQ 版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在云消息队列 RocketMQ 版控制台的实例详情页面查看。  
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
  
  
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQConsumer;  
import com.aliyun.mq.http.common.AckMessageException;  
import com.aliyun.mq.http.model.Message;  
  
import java.util.ArrayList;  
import java.util.List;  
  
public class Consumer {  
      
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入云消息队列 RocketMQ 版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在云消息队列 RocketMQ 版控制台创建。  
        //不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // 您在云消息队列 RocketMQ 版控制台创建的Group ID。  
        final String groupId = "${GROUP_ID}";  
        // Topic所属的实例ID，在云消息队列 RocketMQ 版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在云消息队列 RocketMQ 版控制台的实例详情页面查看。  
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

4.x 代码地址：[https://help.aliyun.com/document_detail/201501.html?spm=a2c4g.201500.0.0.7fb2112bdTFBA2](https://help.aliyun.com/document_detail/201501.html?spm=a2c4g.201500.0.0.7fb2112bdTFBA2)

5.x 代码地址：[https://help.aliyun.com/document_detail/442626.html?spm=a2c4g.441918.0.0.67d9e8cfXnCg1r](https://help.aliyun.com/document_detail/442626.html?spm=a2c4g.441918.0.0.67d9e8cfXnCg1r)

## **5.4 使用建议**

**避免出现大量相同定时时刻德消息**

定时消息的实现逻辑需要先经过定时存储等待触发，定时时间到达后才会被投递给消费者。因此，如果将大量定时消息的定时时间设置为同一时刻，则到达该时刻后会有大量消息同时需要被处理，会造成系统压力过大，导致消息分发延迟，影响定时精度。

# **06 事务消息**

事务消息为云消息队列 RocketMQ 版中的高级特性消息，本文为您介绍事务消息的应用场景、功能原理、使用限制、使用方法和使用建议。

## **6.1 应用场景**

**分布式事务的诉求**

分布式系统调用的特点为一个核心业务逻辑的执行，同时需要调用多个下游业务进行处理。因此，如何保证核心业务和多个下游业务的执行结果完全一致，是分布式事务需要解决的主要问题。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201711495.png)

以电商交易场景为例，用户支付订单这一核心操作的同时会涉及到下游物流发货、积分变更、购物车状态清空等多个子系统的变更。

当前业务的处理分支包括：
1. 主分支订单系统状态更新：由未支付变更为支付成功。
2. 物流系统状态新增：新增待发货物流记录，创建订单物流记录。
3. 积分系统状态变更：变更用户积分，更新用户积分表。
4. 购物车系统状态变更：清空购物车，更新用户购物车记录。

**传统XA事务方案：性能不足**

为了保证上述四个分支的执行结果一致性，典型方案是基于XA协议的分布式事务系统来实现。将四个调用分支封装成包含四个独立事务分支的大事务。基于XA分布式事务的方案可以满足业务处理结果的正确性，但最大的缺点是多分支环境下资源锁定范围大，并发度低，随着下游分支的增加，系统性能会越来越差。

**基于普通消息方案：一致性保障困难**

将上述基于XA事务的方案进行简化，将订单系统变更作为本地事务，剩下的系统变更作为普通消息的下游来执行，事务分支简化成普通消息+订单表事务，充分利用消息异步化的能力缩短链路，提高并发度。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201711193.png)

该方案中消息下游分支和订单系统变更的主分支很容易出现不一致的现象，例如：
1. 消息发送成功，订单没有执行成功，需要回滚整个事务。
2. 订单执行成功，消息没有发送成功，需要额外补偿才能发现不一致。
3. 消息发送超时未知，此时无法判断需要回滚订单还是提交订单变更。

**基于云消息队列 RocketMQ 版分布式事务消息：支持最终一致性**

上述普通消息方案中，普通消息和订单事务无法保证一致的原因，本质上是由于普通消息无法像单机数据库事务一样，具备提交、回滚和统一协调的能力。

而基于云消息队列 RocketMQ 版实现的分布式事务消息功能，在普通消息基础上，支持二阶段的提交能力。将二阶段提交和本地事务绑定，实现全局提交结果的一致性。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201712928.png)

云消息队列 RocketMQ 版事务消息的方案，具备高性能、可扩展、业务开发简单的优势。具体事务消息的原理和流程，请参见下文的[功能原理](https://help.aliyun.com/document_detail/440244.html?spm=a2c4g.442626.0.0.447c284cwnEbvV#section-969-obq-bbt)。

## **6.2 功能实现原理**

**什么是事务消息**

事务消息是云消息队列 RocketMQ 版提供的一种高级消息类型，支持在分布式场景下保障消息生产和本地事务的最终一致性。

**事务消息处理流程**

事务消息交互流程如下图所示。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201712749.png)

具体步骤如下：
1. 生产者将消息发送至云消息队列 RocketMQ 版服务端。
2. 云消息队列 RocketMQ 版服务端将消息持久化成功之后，向生产者返回 Ack 确认消息已经发送成功，此时消息被标记为“暂不能投递”，这种状态下的消息即为半事务消息。
3. 生产者开始执行本地事务逻辑。
4. 生产者根据本地事务执行结果向服务端提交二次确认结果（Commit 或是 Rollback），服务端收到确认结果后处理逻辑如下：
5. 二次确认结果为 Commit：服务端将半事务消息标记为可投递，并投递给消费者。
6. 二次确认结果为 Rollback：服务端将回滚事务，不会将半事务消息投递给消费者。
7. 在断网或者是生产者应用重启的特殊情况下，若服务端未收到发送者提交的二次确认结果，或服务端收到的二次确认结果为Unknown 未知状态，经过固定时间后，服务端将对消息生产者即生产者集群中任一生产者实例发起消息回查。
8. 生产者收到消息回查后，需要检查对应消息的本地事务执行的最终结果。
9. 生产者根据检查到的本地事务的最终状态再次提交二次确认，服务端仍按照步骤 4 对半事务消息进行处理。

## **6.3 使用限制**

**消息类型一致性**

事务消息仅支持在 **MessageType** 为 **Transaction** 的主题内使用，即事务消息只能发送至类型为事务消息的主题中，发送的消息的类型必须和主题的类型一致。

**消息事务性**

云消息队列 RocketMQ 版事务消息保证本地主分支事务和下游消息发送事务的一致性，但不保证消息消费结果和上游事务的一致性。因此需要下游业务分支自行保证消息正确处理，建议消费端做好[消费重试](https://help.aliyun.com/document_detail/440356.htm#concept-2224300)，如果有短暂失败可以利用重试机制保证最终处理成功。

**中间状态可见性**

云消息队列 RocketMQ 版事务消息为最终一致性，即在消息提交到下游消费端处理完成之前，下游分支和上游事务之间的状态会不一致。因此，事务消息仅适合接受异步执行的事务场景。

**事务超时机制**

云消息队列 RocketMQ 版事务消息的命周期存在超时机制，即半事务消息被生产者发送服务端后，如果在指定时间内服务端无法确认提交或者回滚状态，则消息默认会被回滚。事务超时时间，请参见[参数限制](https://help.aliyun.com/document_detail/440347.htm#section-0x5-01t-nv7)。

## **6.4 使用示例**

事务消息相比普通消息发送时需要修改以下几点：
1. 发送事务消息前，需要开启事务并关联本地的事务执行。
2. 为保证事务一致性，在构建生产者时，必须设置事务检查器和预绑定事务消息发送的主题列表，客户端内置的事务检查器会对绑定的事务主题做异常状态恢复。

以Java语言为例，5.x 版本收发事务消息示例参考如下：
```java
//演示demo，模拟订单表查询服务，用来确认订单事务是否提交成功。  
private static boolean checkOrderById(String orderId) {  
    return true;  
}  
  
//演示demo，模拟本地事务的执行结果。  
private static boolean doLocalTransaction() {  
    return true;  
}  
  
public static void main(String[] args) throws ClientException {  
    ClientServiceProvider provider = new ClientServiceProvider();  
    MessageBuilder messageBuilder = new MessageBuilder();  
    //构造事务生产者：事务消息需要生产者构建一个事务检查器，用于检查确认异常半事务的中间状态。  
    Producer producer = provider.newProducerBuilder()  
            .setTransactionChecker(messageView -> {  
                /**  
                 * 事务检查器一般是根据业务的ID去检查本地事务是否正确提交还是回滚，此处以订单ID属性为例。  
                 * 在订单表找到了这个订单，说明本地事务插入订单的操作已经正确提交；如果订单表没有订单，说明本地事务已经回滚。  
                 */  
                final String orderId = messageView.getProperties().get("OrderId");  
                if (Strings.isNullOrEmpty(orderId)) {  
                    // 错误的消息，直接返回Rollback。  
                    return TransactionResolution.ROLLBACK;  
                }  
                return checkOrderById(orderId) ? TransactionResolution.COMMIT : TransactionResolution.ROLLBACK;  
            })  
            .build();  
    //开启事务分支。  
    final Transaction transaction;  
    try {  
        transaction = producer.beginTransaction();  
    } catch (ClientException e) {  
        e.printStackTrace();  
        //事务分支开启失败，直接退出。  
        return;  
    }  
    Message message = messageBuilder.setTopic("topic")  
            //设置消息索引键，可根据关键字精确查找某条消息。  
            .setKeys("messageKey")  
            //设置消息Tag，用于消费端根据指定Tag过滤消息。  
            .setTag("messageTag")  
            //一般事务消息都会设置一个本地事务关联的唯一ID，用来做本地事务回查的校验。  
            .addProperty("OrderId", "xxx")  
            //消息体。  
            .setBody("messageBody".getBytes())  
            .build();  
    //发送半事务消息  
    final SendReceipt sendReceipt;  
    try {  
        sendReceipt = producer.send(message, transaction);  
    } catch (ClientException e) {  
        //半事务消息发送失败，事务可以直接退出并回滚。  
        return;  
    }  
    /**  
     * 执行本地事务，并确定本地事务结果。  
     * 1. 如果本地事务提交成功，则提交消息事务。  
     * 2. 如果本地事务提交失败，则回滚消息事务。  
     * 3. 如果本地事务未知异常，则不处理，等待事务消息回查。  
     *  
     */    
    boolean localTransactionOk = doLocalTransaction();  
    if (localTransactionOk) {  
        try {  
            transaction.commit();  
        } catch (ClientException e) {  
            // 业务可以自身对实时性的要求选择是否重试，如果放弃重试，可以依赖事务消息回查机制进行事务状态的提交。  
            e.printStackTrace();  
        }  
    } else {  
        try {  
            transaction.rollback();  
        } catch (ClientException e) {  
            // 建议记录异常信息，回滚异常时可以无需重试，依赖事务消息回查机制进行事务状态的提交。  
            e.printStackTrace();  
        }  
    }  
}
```

4.x 版本收发事务消息示例参考如下：
```java
// 发送事务消息的示例代码  
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
                // 设置HTTP协议客户端接入点，进入云消息队列 RocketMQ 版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在云消息队列 RocketMQ 版控制台创建。  
        // 不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // Topic所属的实例ID，在云消息队列 RocketMQ 版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在云消息队列 RocketMQ 版控制台的实例详情页面查看。  
        final String instanceId = "${INSTANCE_ID}";  
        // 您在云消息队列 RocketMQ 版控制台创建的Group ID。  
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
  
  
//订阅事务消息的示例代码  
import com.aliyun.mq.http.MQClient;  
import com.aliyun.mq.http.MQConsumer;  
import com.aliyun.mq.http.common.AckMessageException;  
import com.aliyun.mq.http.model.Message;  
  
import java.util.ArrayList;  
import java.util.List;  
  
public class Consumer {  
      
    public static void main(String[] args) {  
        MQClient mqClient = new MQClient(  
                // 设置HTTP协议客户端接入点，进入云消息队列 RocketMQ 版控制台实例详情页面的接入点区域查看。  
                "${HTTP_ENDPOINT}",  
                // AccessKey ID，阿里云身份验证标识。获取方式，请参见创建AccessKey。  
                "${ACCESS_KEY}",  
                // AccessKey Secret，阿里云身份验证密钥。获取方式，请参见创建AccessKey。  
                "${SECRET_KEY}"  
        );  
          
        // 消息所属的Topic，在云消息队列 RocketMQ 版控制台创建。  
        //不同消息类型的Topic不能混用，例如普通消息的Topic只能用于收发普通消息，不能用于收发其他类型的消息。  
        final String topic = "${TOPIC}";  
        // 您在云消息队列 RocketMQ 版控制台创建的Group ID。  
        final String groupId = "${GROUP_ID}";  
        // Topic所属的实例ID，在云消息队列 RocketMQ 版控制台创建。  
        // 若实例有命名空间，则实例ID必须传入；若实例无命名空间，则实例ID传入null空值或字符串空值。实例的命名空间可以在云消息队列 RocketMQ 版控制台的实例详情页面查看。  
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

## **6.5 使用建议**

**避免大量未决事务导致超时**

云消息队列 RocketMQ 版支持在事务提交阶段异常的情况下发起事务回查，保证事务一致性。但生产者应该尽量避免本地事务返回未知结果。大量的事务检查会导致系统性能受损，容易导致事务处理延迟。

**正确处理“进行中”的事务**

消息回查时，对于正在进行中的事务不要返回Rollback或Commit结果，应继续保持Unknown的状态。

一般出现消息回查时事务正在处理的原因为：事务执行较慢，消息回查太快。

解决方案如下：
1. 将第一次事务回查时间设置较大一些，但可能导致依赖回查的事务提交延迟较大。
2. 程序能正确识别正在进行中的事务。

至此，常用的消息类型就带大家梳理完毕，其他类型消息请自行参考如下：

5.x 版本： [https://help.aliyun.com/document_detail/440186.html?spm=a2c4g.440186.0.0.ce79374e2lENTt](https://help.aliyun.com/document_detail/440186.html?spm=a2c4g.440186.0.0.ce79374e2lENTt)

4.x 版本：[https://help.aliyun.com/document_detail/155951.html?spm=a2c4g.29543.0.0.5ecb5284uR3N2k](https://help.aliyun.com/document_detail/155951.html?spm=a2c4g.29543.0.0.5ecb5284uR3N2k)













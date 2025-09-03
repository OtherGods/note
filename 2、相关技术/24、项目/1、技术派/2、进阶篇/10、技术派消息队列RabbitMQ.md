RabbitMQ 的文章之前写过，但是当时给的示例是 Demo 版的，这篇文章主要是结合之前写的理论知识，将 RabbitMQ 集成到技术派项目中。

文章目录
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310302203317.png)

下面我们先回顾一下理论知识，如果对这块知识已经清楚的同学，可以直接跳到实战部分。

# 1、消息队列
## 1.1、消息队列模式

消息队列目前主要 2 种模式，分别为“点对点模式”和“发布/订阅模式”。

### 1.1.1、点对点模式

一个具体的消息只能由一个消费者消费，多个生产者可以向同一个消息队列发送消息，但是一个消息在被一个消息者处理的时候，这个消息在队列上会被锁住或者被移除并且其他消费者无法处理该消息。

需要额外注意的是，如果消费者处理一个消息失败了，消息系统一般会把这个消息放回队列，这样其他消费者可以继续处理。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310302217113.png)

### 1.1.2、发布/订阅模式

单个消息可以被多个订阅者并发的获取和处理。一般来说，订阅有两种类型：
- **临时（ephemeral）订阅**：这种订阅只有在消费者启动并且运行的时候才存在。一旦消费者退出，相应的订阅以及尚未处理的消息就会丢失。
- **持久（durable）订阅**：这种订阅会一直存在，除非主动去删除。消费者退出后，消息系统会继续维护该订阅，并且后续消息可以被继续处理。
  ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310302223435.png)

## 1.2、RabbitMQ特征

- **消息路由（支持）**：RabbitMQ可以通过不同的交换器支持不同种类的消息路由
- **消息有序（不支持）**：当消费消息时，如果消费失败，消息会被放回队列，然后重新消费，这样会导致消息无序
- **消息时序（非常好）**：通过延时队列，可以指定消息的延时时间，过期时间TTL等
- **容错处理（非常好）**：通过交付重试和死信交换器（DLX）来处理消息处理故障
- **伸缩（一般）**：伸缩并没有非常智能，因为即使伸缩了，master queue还是只有一个，负载还是只有这一个master queue去抗，所以我理解RabbmitMQ的伸缩很弱（个人理解）
- **持久化（不太好）**：没有消费过的消息，可以支持持久化，这个是为了保证及其当即时消息可以恢复，但是消费过的消息，就会被马上删除掉，因为RabitMQ设计时，就不是为存储历史数据的
- **消息回溯（支持）**：因为消息不支持永久保存，所以自然就不支持回溯
- **高吞吐（中等）**：因为所有的请求的执行，都是在master queue，它的这个设计，导致单机性能达不到十万级的标准。

# 2、RabbitMQ原理初探

RabbitMQ 2007 年发布，是使用 Erlang 语言开发的开源消息队列系统，基于 AMQP 协议来实现。

## 2.1、基本概念

提到RabbitMQ，就不得不提AMQP协议。AMQP协议是具有现代特征的二进制协议。是一个提供统一消息服务的应用层标准高级消息队列协议，是应用层协议的一个开放标准，为面向消息的中间件设计。

先了解一下AMQP协议中间的几个重点概念：
- Server：接收客户端的连接，实现AMQP实体服务。
- Connection：连接，应用程序与Server的网络连接，TCP连接。
	- Connection 是 AMQP 客户端与消息代理（broker）之间的物理连接。
	- 一个 Connection 可以包含多个 Channel。
	- Connection 的建立是一个相对耗费资源的操作，因此通常在应用程序的生命周期中维持一个长期的 Connection。
	- Connection 提供了身份验证和安全性等功能，并负责处理底层的网络通信。
- Channel：信道，消息读写等操作在信道中进行。客户端可以建立多个信道，每个信道代表一个会话任务。
	- Channel 是在 Connection 内部创建的逻辑通信通道。
	- 每个 Channel 都有独立的通信路径，可以并行处理多个操作。
	- Channel 可以看作是在单个 Connection 上建立的轻量级会话，它允许多个操作在同一个连接上进行。
	- Channel 通过复用单个 Connection 的资源来减少网络开销，提高了系统的性能和效率。
	- 每个 Channel 都有自己的上下文环境，包括声明队列、定义交换机、发送和接收消息等操作。
	  
> 	总结来说，Connection 是 AMQP 客户端与消息代理之间的物理连接，而 Channel 则是在 Connection 内部创建的逻辑通信通道。Connection 负责底层的网络通信和安全性，而 Channel 则提供了更高层次的消息传递和操作控制。使用 Connection 和 Channel 可以实现高效且可靠的消息传递系统。


- Message：消息，应用程序和服务器之间传送的数据，消息可以非常简单，也可以很复杂。由Properties和Body组成。Properties为外包装，可以对消息进行修饰，比如消息的优先级、延迟等高级特性；Body就是消息体内容。
- Virtual Host：虚拟主机，用于逻辑隔离。一个虚拟主机里面可以有若干个Exchange和Queue，同一个虚拟主机里面不能有相同名称的Exchange或Queue。
- Exchange：交换器，接收消息，按照路由规则将消息路由到一个或者多个队列。如果路由不到，或者返回给生产者，或者直接丢弃。RabbitMQ常用的交换器常用类型有direct、topic、fanout、headers四种，后面详细介绍。
- Binding：绑定，交换器和消息队列之间的虚拟连接，绑定中可以包含一个或者多个RoutingKey。
- RoutingKey：路由键，生产者将消息发送给交换器的时候，会发送一个RoutingKey，用来指定路由规则，这样交换器就知道把消息发送到哪个队列。路由键通常为一个“.”分割的字符串，例如“com.rabbitmq”。
- Queue：消息队列，用来保存消息，供消费者消费。

## 2.2、工作原理

AMQP 协议模型由三部分组成：生产者、消费者和服务端，执行流程如下：
1. 生产者是连接到 Server，建立一个连接，开启一个信道。
2. 生产者声明交换器和队列，设置相关属性，并通过路由键将交换器和队列进行绑定。
3. 消费者也需要进行建立连接，开启信道等操作，便于接收消息。
4. 生产者发送消息，发送到服务端中的虚拟主机。
5. 虚拟主机中的交换器根据路由键选择路由规则，发送到不同的消息队列中。
6. 订阅了消息队列的消费者就可以获取到消息，进行消费。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310312013978.png)

## 2.3、常用交换器

RabbitMQ常用的交换器类型有direct、topic、fanout、headers四种：
- Direct Exchange：见文知意，直连交换机意思是此交换机需要绑定一个队列，要求该消息与一个特定的路由键完全匹配。简单点说就是一对一的，点对点的发送。
  ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310312014126.png)
- Fanout Exchange：这种类型的交换机需要将队列绑定到交换机上。一个发送到交换机的消息都会被转发到与该交换机绑定的所有队列上。很像子网广播，每台子网内的主机都获得了一份复制的消息。简单点说就是发布订阅。
  ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310312014513.png)
- Topic Exchange：直接翻译的话叫做主题交换机，如果从用法上面翻译可能叫通配符交换机会更加贴切。这种交换机是使用通配符去匹配，路由到对应的队列。通配符有两种：`*` 、`#`。需要注意的是通配符前面必须要加上 `.` 符号。
	- `*` 符号：有且只有一个匹配词。比如 `a.*` 可以匹配到 `a.b`、`a.c` ，但是匹配不了 `a.b.c`
	- `#` 符号：匹配一个或多个词。比如 `rabbit.#` 即可以匹配到 `rabbit.a.b`、`rabbit.a`也可以匹配到 `rabbit.a.b.c`
	![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310312039258.png)
- Headers Exchange：这种交换机用的相对没这么多。它跟上面三种有点区别，它的路由不是用routingKey进行路由匹配，而是在匹配请求头中所带的键值进行路由。创建队列需要设置绑定的头部信息，有两种模式：全部匹配和部分匹配。如下图所示，交换机会根据生产者发送过来的头部信息携带的键值去匹配队列绑定的键值，路由到对应的队列。
  ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310312050913.png)

# 3、RabbitMQ环境搭建

因为我用的是Mac，所以直接可以参考官网：

https://www.rabbitmq.com/install-homebrew.html

需要注意的是，一定需要先执行：`brew update`
然后再执行：`brew install rabbitmq`
> 之前没有执行brew update，直接执行brew install rabbitmq时，会报各种各样奇怪的错误，其中“403 Forbidde”居多。

但是在执行“brew install rabbitmq”，会自动安装其它的程序，如果你使用源码安装Rabbitmq，因为启动该服务依赖erlang环境，所以你还需手动安装erlang，但是目前官方已经一键给你搞定，会自动安装Rabbitmq依赖的所有程序，是不是很棒！

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310312112739.png)

最后执行成功的输出如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310312112626.png)

启动服务：
```
# 启动方式1：后台启动
brew services start rabbitmq
# 启动方式2：当前窗口启动
cd /usr/local/Cellar/rabbitmq/3.8.19
rabbitmq-server
```

在浏览器输入：
`http://localhost:15672/`

会出现RabbitMQ后台管理界面（用户名和密码都为guest）：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310312116457.png)

通过brew安装，一行命令搞定，真香！

# 4、RabbtitMQ集成
## 4.1、前置工作

添加账号：
```
## 添加账号
./rabbitmqctl add_user admin admin
## 添加访问权限
./rabbitmqctl set_permissions -p "/" admin ".*" ".*" ".*"
## 设置超级权限
./rabbitmqctl set_user_tags admin administrator

## 在windows上执行这些命令的时候命令后加上.bat后缀
```

pom引入依赖
```xml
<dependency>
    <groupId>com.rabbitmq</groupId>
    <artifactId>amqp-client</artifactId>
    <version>5.5.1</version>
</dependency>
```

## 4.2、代码实现
### 4.2.1、核心代码

先整一个 ConnectionFactory 单例，每台机器都有自己的 ConnectionFactory，防止每次都初始化（在后面的迭代中，我会把这个去掉，整成连接池）。
```java
/**  
 * 说明：添加rabbitmq连接池后，这个就可以废弃掉  
 * @author Louzai  
 * @date 2023/5/10  
 */public class RabbitmqUtil {  
  
    /**  
     * 每个 host 都有自己的工厂，便于后面改造成多机的方式；
     * 这种方式不需要在这个字段上增加volatile，和常规的双重校验锁示例有
     * 所区别。
     */  
    private static Map<String, ConnectionFactory> executors = new ConcurrentHashMap<>();  
  
    /**  
     * 初始化一个工厂  
     *  
     * @param host  
     * @param port  
     * @param username  
     * @param passport  
     * @param virtualhost  
     * @return  
     */  
    private static ConnectionFactory init(String host,  
                                  Integer port,  
                                  String username,  
                                  String passport,  
                                  String virtualhost) {  
        ConnectionFactory factory = new ConnectionFactory();  
        factory.setHost(host);  
        factory.setPort(port);  
        factory.setUsername(username);  
        factory.setPassword(passport);  
        factory.setVirtualHost(virtualhost);
        return factory;  
    }  
  
    /**  
     * 工厂单例，每个key都有属于自己的工厂
     *  
     * @param key
     * @param host  
     * @param port  
     * @param username  
     * @param passport  
     * @param virtualhost  
     * @return  
     */  
    public static ConnectionFactory getOrInitConnectionFactory(String key,
															    String host,  
																Integer port,  
																String username,  
																String passport,  
																String virtualhost) {
        ConnectionFactory connectionFactory = executors.get(key);  
        if (null == connectionFactory) {  
            synchronized (RabbitmqUtil.class) {  
                connectionFactory = executors.get(key);  
                if (null == connectionFactory) {  
                    connectionFactory = init(host, port, username, passport, virtualhost);  
                    executors.put(key, connectionFactory);  
                }  
            }  
        }  
        return connectionFactory;  
    }  
  
    /**  
     * 获取key  
     * @param host  
     * @param port  
     * @return  
     */  
    private static String getConnectionFactoryKey(String host, Integer port) {  
        return host + ":" + port;  
    }  
}
```

获取 RabbitmqClient：
```java
/**
 * @author Louzai
 * @date 2023/5/10
 */
@Component
public class RabbitmqClient {

    @Autowired
    private RabbitmqProperties rabbitmqProperties;
    
    /**
     * 创建一个工厂
     * @param key
     * @return
     */
    public ConnectionFactory getConnectionFactory(String key) {
	    String host = rabbitmqProperties.getHost();
        Integer port = rabbitmqProperties.getPort();
        String userName = rabbitmqProperties.getUsername();
        String password = rabbitmqProperties.getPassport();
        String virtualhost = rabbitmqProperties.getVirtualhost();
        return RabbitmqUtil.getOrInitConnectionFactory(key, host, port, userName,password, virtualhost);
    }
}
```

**重点！敲黑板！！！这里就是 RabbmitMQ 的核心逻辑了。**

我们使用的交换机类型是 Direct Exchange，此交换机需要绑定一个队列，要求该消息与一个特定的路由键完全匹配，简单点说就是一对一的，点对点的发送。

至于为什么不用广播和主题交换机模式，因为技术派的使用场景就是发送单个消息，点到点发送和消费的模式完全可以满足我们的需求。

下面 3 个方法都很简单：
- 发送消息：拿到工厂 -> 创建链接 -> 创建通道 -> 声明交换机 -> 发送消息 -> 关闭链接；
- 消费消息：拿到工厂 -> 创建链接 -> 创建通道 -> 确定消息队列 -> 绑定队列到交换机 -> 接收并消费消息；
- 消息消费永动模式：非阻塞式消费 RabbitMQ消息

```java
@Component
public class RabbitmqServiceImpl implements RabbitmqService {

    @Autowired
    private RabbitmqClient rabbitmqClient;

    @Autowired
    private NotifyService notifyService;

    @Override
    @Override
    public void publishMsg(String exchange,
                           BuiltinExchangeType exchangeType,
                           String toutingKey,
                           String message) throws IOException, TimeoutException {
		// TODO: 这种并发量起不来，需要改造成连接池，如果这里改成连接池
        // 的话，rabbitmqClient工具类就可以去掉，直接从连接池中获取链接
        ConnectionFactory factory = rabbitmqClient.getConnectionFactory(toutingKey);

        //创建连接
        Connection connection = factory.newConnection();
        //创建消息通道
        Channel channel = connection.createChannel();

        // 声明exchange中的消息为可持久化，不自动删除
        channel.exchangeDeclare(exchange, exchangeType, true, false, null);

        // 发布消息
        channel.basicPublish(exchange, toutingKey, null, message.getBytes());

        System.out.println("Publish msg:" + message);
        channel.close();
        connection.close();
    }
    
    @Override
    public void consumerMsg(String exchange,
                            String queue,
                            String routingKey) throws IOException, TimeoutException {
		// TODO: 这种并发量起不来，需要改造成连接池，如果这里改成连接池
        // 的话，rabbitmqClient工具类就可以去掉，直接从连接池中获取链接
        ConnectionFactory factory = rabbitmqClient.getConnectionFactory(routingKey);

        //创建连接
        Connection connection = factory.newConnection();
        //创建消息信道
        final Channel channel = connection.createChannel();
        //消息队列
        channel.queueDeclare(queue, true, false, false, null);
        //绑定队列到交换机
        channel.queueBind(queue, exchange, routingKey);
        
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Consumer msg:" + message);

                // 获取Rabbitmq消息，并保存到DB
                notifyService.saveArticleNotify(JsonUtil.toObj(message, UserFootDO.class), NotifyTypeEnum.PRAISE);

                channel.basicAck(envelope.getDeliveryTag(), false);
			}
		};
		// 取消自动ack
        channel.basicConsume(queue, false, consumer);
    }
    
    @Override
    public void processConsumerMsg() {
        System.out.println("Begin to processConsumerMsg.");

        Integer stepTotal = 1;
        Integer step = 0;
        
        // TODO: 这种方式非常 Low，后续会改造成阻塞 I/O 模式
        while (true) {
            step ++;
            try {
                System.out.println("processConsumerMsg cycle.");
                consumerMsg(CommonConstants.EXCHANGE_NAME_DIRECT, CommonConstants.QUERE_NAME_PRAISE,
                        CommonConstants.QUERE_KEY_PRAISE);
                if (step.equals(stepTotal)) {
	                Thread.sleep(10000);
                    step = 0;
                }
            } catch (Exception e) {
            }
		}
	}
}
```

这里只是给个示例，如果要真正用到生产环境，你觉得有哪些问题呢？ 你自己先想想，文末再告诉你。

### 4.2.2、调用入口

其实之前我们是通过 Java 的内置异步调用方式，为了方便验证，我把文章点赞的功能迁移到 RabbitMQ 中，只要是点赞，就走 RabbitMQ 模式。
```java
// 点赞消息走 RabbitMQ，其它走 Java 内置消息机制
if (notifyType.equals(NotifyTypeEnum.PRAISE) && rabbitmqProperties.getSwitchFlag()) {
    rabbitmqService.publishMsg(
		CommonConstants.EXCHANGE_NAME_DIRECT,
		BuiltinExchangeType.DIRECT,
		CommonConstants.QUERE_KEY_PRAISE,
JsonUtil.toStr(foot));
} else {
	Optional.ofNullable(notifyType).ifPresent(notify -> SpringUtil.publishEvent(new NotifyMsgEvent<>(this, notify, foot)));
}
```

那消费入口放哪里呢？其实是在程序启动的时候，我们就启动 RabbitMQ 进行消费，然后整个进程一直在程序中跑。
```java
@Override
public void run(ApplicationArguments args) {
    // 设置类型转换, 主要用于mybatis读取varchar/json类型数据据，并写入到json格式的实体Entity中
    JacksonTypeHandler.setObjectMapper(new ObjectMapper());
    // 应用启动之后执行
    GlobalViewConfig config = SpringUtil.getBean(GlobalViewConfig.class);
    if (webPort != null) {
        config.setHost("http://127.0.0.1:" + webPort);
    }
    // 启动 RabbitMQ 进行消费
    if (rabbitmqProperties.getSwitchFlag()) {
        taskExecutor.execute(() -> rabbitmqService.processConsumerMsg());
    }
    log.info("启动成功，点击进入首页: {}", config.getHost());
}
```

## 4.3、演示一下

我们多次点击“点赞”按钮，触发 RammitMQ 消息发送。
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311012248949.png)

可以通过日志，也可以看到发送和消费过的消息。
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311012248949.png)

我靠！好多没有关闭的链接。。。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311012257582.png)

还有一堆没有关闭的 channel。。。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311012256904.png)

估计再多跑一会，内存全部吃光，机器就死机了，怎么破？答案是连接池！

## 4.4、代码分支

为了方便大家学习功能演变的过程，每个模块都会单独开个分支，包括后面的升级版：
- 代码仓库：[https://github.com/itwanger/paicoding](https://github.com/itwanger/paicoding)
- 代码分支：feature/add_rabbitmq_20230506

如果需要运行 RabbitMQ，下面的配置需要改成 true，因为代码默认是 false。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409222017396.png)

# 5、后记

这篇文章，让大家知道 RabbitMQ 的基本原理，以及如何去集成 RabbitMQ，但是还不能用到实际生产环境，但是这个确实是我写的第一个版本，存粹是搞着玩的，因为里面存在的问题还非常多。

我简单列举一下：
1. 需要给 Connection 加个连接池，否则内存会持续消耗，机器肯定扛不住；
2. 需要对 RabbitMQ 的消费方式进行改造，因为 while + sleep 的方式过于简单粗暴；
3. 假如消费的任务挂掉了，你需要有重启 RabbitMQ 的消费机制；
4. 假如机器挂了，重启后，RabbitMQ 内部的消息不能丢失。

如果你对上面的问题也非常感兴趣，可以直接基于分支 feature/add_rabbitmq_20230506，然后给我提 PR，技术嘛，我喜欢边玩边学。

预告一下，我后面会给 RabbitMQ 加个连接池，代码已经写完了，还是用 ChatGPT 帮忙完成的，下一篇文章发出，敬请期待！






上次我把 RabbitMQ 集成到项目中，但是每次使用 RabbitMQ 时都去 New 一个连接，导致并发起不来，所以这次我们就给 RabbitMQ 加一个连接池。

为了偷个懒，我直接用 ChatGPT 教我加。

# 1、ChatGPT生成的代码

我输入的关键字如下：
`rabbitmq 实现一个连接池 java`

下面是 GhatGPT 4.0 给我的回复：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311012313194.png)
该示例代码创建了大小为10的连接池，并创建了两个消费者线程来消费队列，并使用一个生产者向队列发送消息。每个消费者从连接池获取连接，并消费队列中的消息，当完成任务后，将连接还回连接池。在实际部署中，您需要根据您的应用程序需求选择合适大小的池，并调整代码以使用正确的参数。


# 2、给RabbitMQ加连接池
## 2.1、加入连接池

之前我们给 ConnectionFactory 加了个单例工厂，具体可以参考我上一篇文章，由于我们有连接池，所以这个单例工厂就直接废弃了。
```java
public class RabbitmqConnection {  
  
    private Connection connection;  
  
    public RabbitmqConnection(String host, int port, String userName, String password, String virtualhost) {  
        ConnectionFactory connectionFactory = new ConnectionFactory();  
        connectionFactory.setHost(host);  
        connectionFactory.setPort(port);  
        connectionFactory.setUsername(userName);  
        connectionFactory.setPassword(password);  
        connectionFactory.setVirtualHost(virtualhost);  
        try {  
            connection = connectionFactory.newConnection();  
        } catch (IOException | TimeoutException e) {  
            e.printStackTrace();  
        }  
    }  
  
    /**  
     * 获取链接  
     *  
     * @return  
     */  
    public Connection getConnection() {  
        return connection;  
    }  
  
    /**  
     * 关闭链接  
     *  
     */    public void close() {  
        try {  
            connection.close();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    }  
}
```
这个代码就是 ChatGPT 给我写的，我是直接 Copy 过来，然后稍微改动了一下。

```java
public class RabbitmqConnectionPool {  
  
    private static BlockingQueue<RabbitmqConnection> pool;  
  
    public static void initRabbitmqConnectionPool(String host, int port, String userName, String password,  
                                             String virtualhost,  
                                           Integer poolSize) {  
        pool = new LinkedBlockingQueue<>(poolSize);  
        for (int i = 0; i < poolSize; i++) {  
            pool.add(new RabbitmqConnection(host, port, userName, password, virtualhost));  
        }  
    }  
  
    public static RabbitmqConnection getConnection() throws InterruptedException {  
        return pool.take();  
    }  
  
    public static void returnConnection(RabbitmqConnection connection) {  
        pool.add(connection);  
    }  
  
    public static void close() {  
        pool.forEach(RabbitmqConnection::close);  
    }  
}
```

## 2.2、RabbitMQ发送消息消费

RabbitMQ 发送消息：从连接池拿到连接 ->  创建通道 -> 声明交换机 -> 发送消息 -> 将连接归还连接池。

这里的逻辑基本和之前的一样，只是之前是 New 一个连接，现在是直接从连接池拿到连接，然后最后多了一步归还连接的操作。
```java
@Override  
public void publishMsg(String exchange,  
                       BuiltinExchangeType exchangeType,  
                       String toutingKey,  
                       String message) {  
    try {  
        //创建连接  
        RabbitmqConnection rabbitmqConnection = RabbitmqConnectionPool.getConnection();  
        Connection connection = rabbitmqConnection.getConnection();  
        //创建消息通道  
        Channel channel = connection.createChannel();  
        // 声明exchange中的消息为可持久化，不自动删除  
        channel.exchangeDeclare(exchange, exchangeType, true, false, null);  
        // 发布消息  
        channel.basicPublish(exchange, toutingKey, null, message.getBytes());  
        log.info("Publish msg: {}", message);  
        channel.close();  
        RabbitmqConnectionPool.returnConnection(rabbitmqConnection);  
    } catch (InterruptedException | IOException | TimeoutException e) {  
        e.printStackTrace();  
    } 
}
```

RabbitMQ 消费消息：从连接池拿到连接 -> 创建通道 -> 确定消息队列 -> 绑定队列到交换机 -> 接受并消费消息 -> 将连接归还连接池。

同上，这里的逻辑基本和之前的一样，只是多了一个拿连接和归还连接的过程。
```java
@Override  
public void consumerMsg(String exchange,  
                        String queueName,  
                        String routingKey) {  
  
    try {  
        //创建连接  
        RabbitmqConnection rabbitmqConnection = RabbitmqConnectionPool.getConnection();  
        Connection connection = rabbitmqConnection.getConnection();  
        //创建消息信道  
        final Channel channel = connection.createChannel();  
        //消息队列  
        channel.queueDeclare(queueName, true, false, false, null);  
        //绑定队列到交换机  
        channel.queueBind(queueName, exchange, routingKey);  
  
        Consumer consumer = new DefaultConsumer(channel) {  
            @Override  
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,  
                                       byte[] body) throws IOException {  
                String message = new String(body, "UTF-8");  
                log.info("Consumer msg: {}", message);  
  
                // 获取Rabbitmq消息，并保存到DB  
                // 说明：这里仅作为示例，如果有多种类型的消息，可以根据消息判定，简单的用 if...else 处理，复杂的用工厂 + 策略模式  
                notifyService.saveArticleNotify(JsonUtil.toObj(message, UserFootDO.class), NotifyTypeEnum.PRAISE);  
  
                channel.basicAck(envelope.getDeliveryTag(), false);  
            }  
        };  
        // 取消自动ack  
        channel.basicConsume(queueName, false, consumer);  
        channel.close();  
        RabbitmqConnectionPool.returnConnection(rabbitmqConnection);  
    } catch (InterruptedException | IOException | TimeoutException e) {  
        e.printStackTrace();  
    }  
}
```

这个代码，其实 ChatGPT 写的有问题，你再回过头去看 ChatGPT 写的代码，发现连接取出，但是没有归还，那会出现什么问题呢？

这里给大家分析一下，由于我们的连接池用的是 BlockingQueue，连接池大小是 5，如果连接全部取出并都不归还，当第 6 个请求过来后，请求就卡住了，导致界面操作会被阻塞，请求完全没有反应。

不要问我怎么知道，因为我是踩坑过来的。

## 2.3、代码仓库

为了方便大家学习功能演变的过程，每个模块都会单独开个分支，连接池的分支和仓库如下：
- 代码仓库：[https://github.com/itwanger/paicoding](https://github.com/itwanger/paicoding)
- 代码分支：feature/rabbitmq_connection_pool_20230511
如果需要运行 RabbitMQ，下面的配置需要改成 true，因为代码默认是 false。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311012323312.png)


# 3、实际效果

我们是把技术派的“点赞”功能消息，通过 RabbitMQ 方式处理，我们多次点击“点赞”按钮，触发 RammitMQ 消息发送。

可以通过日志，也可以看到发送和消费过的消息。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311012324017.png)

最后就是大家关心的连接池个数，打开 RabbitMQ 后台，发现永远只有 5 个连接，和我们的连接池大小一致，符合预期。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311012324433.png)

再看看打开的 Channel，由于每次都关闭，所以也没有了，也符合预期。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311012324470.png)

这里抛个疑问，每次新开一个 Channel，用完后关闭，是不是也很耗时？是否需要给 Channel 也搞一个连接池呢？可以评论区告诉我哈~~

# 4、后记

如果用 ChatGPT 3.5，给的结果就不一样，需要加入更多关键字，如果需要达到 GhatGPT 4.0 给的结果，你需要给 ChatGPT 3.5 以下关键字。
```
rabbitmq 用BlockingQueue实现一个连接池  java
```

学习嘛，就是边学边玩，后面有空，我想再加点东西，主要是想把 RabbitMQ 的消费方式，由阻塞改成非阻塞方式，可能会在下一篇文章给出，敬请期待！

最后，把楼仔的座右铭送给你：我从清晨走过，也拥抱夜晚的星辰，人生没有捷径，你我皆平凡，你好，陌生人，一起共勉。

## 4.1、硬核推荐

**硬核推荐：**
- [从原理到实战，手把手教你在项目中使用RabbitMQ](https://mp.weixin.qq.com/s?__biz=Mzg3OTU5NzQ1Mw==&mid=2247495630&idx=1&sn=0bbab9b562359d100870c62f773baffa&chksm=cf00ad2cf877243a8e4781b456a057f420fbff59907483aa55416d2b7892f99878696dd68feb&token=407408896&lang=zh_CN#rd)
- [技术派中的缓存一致性解决方案](https://mp.weixin.qq.com/s?__biz=Mzg3OTU5NzQ1Mw==&mid=2247495609&idx=1&sn=3ebf0145c89ab44f128ee869f43dd203&chksm=cf00ad5bf877244d18523ace01a2912c6b965c6de9f48c52cfbc566a4e690eba3dfcfae215e8#rd)
- [如何将技术派项目写入简历](https://mp.weixin.qq.com/s?__biz=Mzg3OTU5NzQ1Mw==&mid=2247495588&idx=1&sn=d2b7c7da309b4ff38c8c99c28662ae27&chksm=cf00ad46f877245056a50b3e39a28614ad6e84732dade0f0351b01a7940e807f06ddbf986b0c&token=882597009&lang=zh_CN#rd)
- [5 种微服务网关，该选哪个？](https://mp.weixin.qq.com/s?__biz=Mzg3OTU5NzQ1Mw==&mid=2247495234&idx=1&sn=5cbbaafad904bbefee443ec882b1a340&chksm=cf00aca0f87725b60114dcf353f715d3d8692c7d3f2fd60506e5a91a6877faa8e9c07d039cec&token=1148172764&lang=zh_CN#rd)
- [对标大厂的技术派的详细方案设计](https://mp.weixin.qq.com/s?__biz=Mzg3OTU5NzQ1Mw==&mid=2247495400&idx=1&sn=e783ca0eeb596469341c814850d5e66c&chksm=cf00ac0af877251cf45258a2de5d59e7da62c4b49d950547fa3f8d4731de6322c0373a128b08&token=882597009&lang=zh_CN#rd)
- [31岁，还可以拼一拼大厂么？](https://mp.weixin.qq.com/s?__biz=Mzg3OTU5NzQ1Mw==&mid=2247493903&idx=1&sn=01732a16ca4580edeef252ef2c37ed0b&chksm=cf00abedf87722fb991b79e835d27ea7c4013a21a2e5fb74f715bb55a298d8261240d996ea16&token=1495229386&lang=zh_CN#rd)
- [4 个维度搞懂 Nacos 注册中心](https://mp.weixin.qq.com/s?__biz=Mzg3OTU5NzQ1Mw==&mid=2247493608&idx=1&sn=1006a75cdbe4f0537de1fe48c4111381&chksm=cf00a50af8772c1c4c1308df1f595347ccb1ed03244b5314d78c98748e079183304dfffe7ace#rd)
- [还在用 Zookeeper 作为注册中心？小心坑死你！](https://mp.weixin.qq.com/s?__biz=Mzg3OTU5NzQ1Mw==&mid=2247493183&idx=1&sn=14286549788f698fb67571621b18127b&chksm=cf00a4ddf8772dcb08ad38d33688c4d3de9bad988d146d8408ca34fdc16d4e779032d1ee719a#rd)























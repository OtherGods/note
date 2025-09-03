### 第六章 Redis的发布与订阅
#### 1.发布和订阅
##### 1.1什么是发布和订阅
发布订阅是一种应用程序（系统）之间通讯，传递数据的技术手段。特别是在异构（不
同语言）系统之间作用非常明显。发布订阅可以是实现应用（系统）之间的解耦合。

● 发布订阅：类似微信中关注公众号/订阅号，公众号/订阅号发布的文章，信息。订阅
者能及时获取到最新的内容。微博的订阅也是类似的。日常生活中听广播，看电视。都
需要有信息的发布者，收听的人需要订阅（广播、电视需要调动某个频道）。
发布订阅是一对多的关系。

● 订阅：对某个内容感兴趣，需要实时获取新的内容。只要关注的内容有变化就能立即得
到通知。多的一方。

● 发布：提供某个内容，把内容信息发送给多个对此内容感兴趣的订阅者。是有主动权，
是一的一方。

发布订阅应用在即时通信应用中较多，比如网络聊天室，实时广播、实时提醒等。滴滴
打车软件的抢单；外卖的抢单；在微信群发红包，抢红包都可以使用发布订阅实现。

##### 1.2 Redis的发布和订阅
Redis发布订阅(pub/sub)是一种消息通信模式：发送者(publish)发送消息，订阅者
(subscribe)接收消息。发布订阅也叫生产者消费者模式,是实现消息队列的一种方式。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615220641973.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
##### 1.3 如何实现
发布订阅的相关命令：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615221219669.png)
**A、publish发布消息**

语法：publish chanel message
作用：将message消息发送到channel频道。message是要发送的消息，channel是自定
义的频道名称（例如cctv1，cctv5），唯一标识发布者。
返回值：数字。接收到消息订阅者的数量

**B、subscribe订阅频道**
语法：subscribe channel[channel…]
作用：订阅一个或多个频道的信息
返回值：订阅的消息

**C、unsubscribe退订频道**
语法：unsubscribe channel [channel]
作用：退出指定的频道，不订阅。
返回值：退订的告知消息

###### 1.3.1 命令行实现
注意要启动订阅者，等待接收发布者的消息，否则订阅者接收不到消息
**A、开启4个redis客户端，3个客户端作为消息订阅者，1个为消息发布者：./redis-cli**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615220749684.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
在发布者或其他窗口启动redis

**B、让3个消息订阅者订阅某个频道主题：subscribechannel
在订阅者的三个窗口中分别启动redis客户端，redis安装目录/src下执行./redis-cli**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615220828844.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**C、让1个消息发布者向频道主题上发布消息：publish channel message在发布者窗口：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615220844256.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**D、 然后观察消息的发布和订阅情况，在任意一个订阅窗口：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615220921998.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
###### 1.3.2 编程实现
JedisPubSub类：Jedis中的JedisPubSub类是Jedis的一个抽象类，此类定义了publish
/subscribe的回调方法，通过继承JedisPubSub类，重写回调方法。实现java中Redis
的发布订阅。当Reids发生发布或订阅的相关事件时会调用这些回调方法。只在回调方法中
实现自己的业务逻辑。

onMessage()：发布者发布消息时，会执行订阅者的回调方法onMessage(),接收发布的
消息。在此方法实现消息接收后的，自定义业务逻辑处理，比如访问数据库，更新库存等。

A、订阅者SUB工程
①：新建 Java Project， 名称：MyRedisSubScribe
②：导入 jar : jedis-2.9.0.jar 加入 Build Path
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615221024470.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
③：定义订阅者者 RedisSubScribe ,继承 JedisPubSub
```java
public class RedisSubScribe extends JedisPubSub {

	/**
	 * 当订阅者接收到消息时回自动调用改方法 String channel--->频道的名称 String message--->发布的消息
	 */
	@Override
	public void onMessage(String channel, String message) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println("我是订阅者:订阅频道[" + channel + "],收到的消息是:[" + message + "],时间为:[" + df.format(new Date()) + "]");
	}

	public static void main(String[] args) {
		// 创建Jedis
		Jedis jedis = new Jedis("192.168.6.129", 6379);
		// 创建redisSubScribe对象
		RedisSubScribe redisSubScribe = new RedisSubScribe();
		// 从Redis订阅
		jedis.subscribe(redisSubScribe, "cctv6");
	}
}
```
B、 发布者工程
①：新建 Java Project 名称 MyRedisPublish
②：导入 jar : jedis-2.9.0.jar 加入 Build Path
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615221104784.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
③：定义发布者类。发布消息
```java
public class MyRedisPublish {

	public static void main(String[] args) {
		// 创建Jedis
		Jedis jedis = new Jedis("192.168.6.129", 6379);
		jedis.publish("cctv6", "战狼2");
		System.out.println("发布消息完毕....");
	}
}
```
**C、 执行程序**
先启动订阅者，在运行发布者。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190615221136177.png)

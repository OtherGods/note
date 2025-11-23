
- Netty 基于 NIO （NIO 是一种同步非阻塞的 I/O 模型，在 Java 1.4 中引入了 NIO），使用 Netty 可以极大地简化 TCP 和 UDP 套接字服务器等网络编程，并且性能以及安全性等很多方面都非常优秀。
- 我们平常经常接触的 Dubbo、RocketMQ、Elasticsearch、gRPC、Spark 等等热门开源项目都用到了 Netty。
- 大部分微服务框架底层涉及到网络通信的部分都是基于 Netty 来做的，比如说 Spring Cloud 生态系统中的网关 Spring Cloud Gateway。

简单总结一下和 Netty 相关问题。

# BIO,NIO 和 AIO 区别

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202511231537617.png)

1. BIO (Blocking I/O): 同步阻塞 I/O 模式，数据的读取写入必须阻塞在一个线程内等待其完成。在客户端连接数量不高的情况下，是没问题的。但是，当面对十万甚至百万级连接的时候，传统的 BIO 模型是无能为力的。因此，我们需要一种更高效的 I/O 处理模型来应对更高的并发量。
2. NIO (Non-blocking/New I/O): NIO 是一种同步非阻塞的 I/O 模型，于 Java 1.4 中引入，对应 java.nio包，提供了 Channel , Selector，Buffer 等抽象。NIO 中的 N 可以理解为 Non-blocking，不单纯是 New。它支持面向缓冲的，基于通道的 I/O 操作方法。 NIO 提供了与传统 BIO 模型中的 Socket 和 ServerSocket 相对应的 SocketChannel 和 ServerSocketChannel 两种不同的套接字通道实现,两种通道都支持阻塞和非阻塞两种模式。对于高负载、高并发的（网络）应用，应使用 NIO 的非阻塞模式来开发
3. AIO (Asynchronous I/O): AIO 也就是 NIO 2。在 Java 7 中引入了 NIO 的改进版 NIO 2,它是异步非阻塞的 IO 模型。异步 IO 是基于事件和回调机制实现的，也就是应用操作之后会直接返回，不会堵塞在那里，当后台处理完成，操作系统会通知相应的线程进行后续的操作。AIO 是异步 IO 的缩写，虽然 NIO 在网络操作中，提供了非阻塞的方法，但是 NIO 的 IO 行为还是同步的。对于 NIO 来说，我们的业务线程是在 IO 操作准备好时，得到通知，接着就由这个线程自行进行 IO 操作，IO 操作本身是同步的。查阅网上相关资料，我发现就目前来说 AIO 的应用还不是很广泛，Netty 之前也尝试使用过 AIO，不过又放弃了。

- [Java IO模型常见面试题](2、相关技术/1、Java基础/IO/Java%20IO模型常见面试题.md)
- [3、操作系统的IO模型有哪些？](4、操作系统/Hollis/3、操作系统的IO模型有哪些？.md)
- [36、什么是AIO、BIO和NIO？](2、相关技术/1、Java基础/Hollis/36、什么是AIO、BIO和NIO？.md)

# Netty 是什么？

1. Netty 是一个 基于 NIO 的 client-server(客户端服务器)框架，使用它可以快速简单地开发网络应用程序。
2. 它极大地简化并优化了 TCP 和 UDP 套接字服务器等网络编程,并且性能以及安全性等很多方面甚至都要更好。
3. 支持多种协议 如 FTP，SMTP，HTTP 以及各种二进制和基于文本的传统协议。

用官方的总结就是：Netty 成功地找到了一种在不妥协可维护性和性能的情况下实现易于开发，性能，稳定性和灵活性的方法。

# 为什么不直接用NIO？

不用 NIO 主要是因为 NIO 的编程模型复杂而且存在一些 BUG，并且对编程功底要求比较高。下图就是一个典型的使用 NIO 进行编程的案例：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202511231551194.png)

# 为什么要用 Netty？

因为 Netty 具有下面这些优点，并且相比于直接使用 JDK 自带的 NIO 相关的 API 来说更加易用。
- 统一的 API，支持多种传输类型，阻塞和非阻塞的。
- 简单而强大的线程模型。
- 自带编解码器解决 TCP 粘包/拆包问题。
- 自带各种协议栈。
- 真正的无连接数据包套接字支持。
- 比直接使用 Java 核心 API 有更高的吞吐量、更低的延迟、更低的资源消耗和更少的内存复制。
- 安全性不错，有完整的 SSL/TLS 以及 StartTLS 支持。
- 社区活跃
- 成熟稳定，经历了大型项目的使用和考验，而且很多开源项目都使用到了 Netty， 比如我们经常接触的 Dubbo、RocketMQ 等等。
- ......

# Netty 应用场景

理论上来说，NIO 可以做的事情 ，使用 Netty 都可以做并且更好。Netty 主要用来做网络通信 :
1. 作为 RPC 框架的网络通信工具 ： 我们在分布式系统中，不同服务节点之间经常需要相互调用，这个时候就需要 RPC 框架了。不同服务节点的通信是如何做的呢？可以使用 Netty 来做。比如我调用另外一个节点的方法的话，至少是要让对方知道我调用的是哪个类中的哪个方法以及相关参数吧！
2. 实现一个自己的 HTTP 服务器 ：通过 Netty 我们可以自己实现一个简单的 HTTP 服务器，这个大家应该不陌生。说到 HTTP 服务器的话，作为 Java 后端开发，我们一般使用 Tomcat 比较多。一个最基本的 HTTP 服务器可要以处理常见的 HTTP Method 的请求，比如 POST 请求、GET 请求等等。
3. 实现一个即时通讯系统 ： 使用 Netty 我们可以实现一个可以聊天类似微信的即时通讯系统，这方面的开源项目还蛮多的，可以自行去 Github 找一找。
4. 实现消息推送系统 ：市面上有很多消息推送系统都是基于 Netty 来做的。
5. ......

# 哪些开源项目用到了 Netty?

我们平常经常接触的 Dubbo、RocketMQ、Elasticsearch、gRPC 等等都用到了 Netty。

可以说大量的开源项目都用到了 Netty，所以掌握 Netty 有助于你更好的使用这些开源项目并且让你有能力对其进行二次开发。

实际上还有很多很多优秀的项目用到了 Netty,Netty 官方也做了统计，统计结果在这里：[https://netty.io/wiki/related-projects.html](https://netty.io/wiki/related-projects.html) 。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202511231600387.png)

# Netty 的核心组件

简单介绍 Netty 最核心的一些组件（对于每一个组件这里不详细介绍）。通过下面这张图你可以将我提到的这些 Netty 核心组件串联起来。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202511231601698.png)

# Bytebuf（字节容器）

**网络通信最终都是通过字节流进行传输的。 `ByteBuf` 就是 Netty 提供的一个字节容器，其内部是一个字节数组**。 当我们通过 Netty 传输数据的时候，就是通过 ByteBuf 进行的。

我们可以将 ByteBuf 看作是 Netty 对 Java NIO 提供了 ByteBuffer 字节容器的封装和抽象。

有很多小伙伴可能就要问了：为什么不直接使用 Java NIO 提供的 ByteBuffer 呢？

因为 ByteBuffer 这个类使用起来过于复杂和繁琐。

# Bootstrap 和 ServerBootstrap（启动引导类）

**`Bootstrap` 是客户端的启动引导类/辅助类**，具体使用方法如下：
```java
EventLoopGroup group = new NioEventLoopGroup();
try {
	//创建客户端启动引导/辅助类：Bootstrap
	Bootstrap b = new Bootstrap();
	//指定线程模型
	b.group(group).
			......
	// 尝试建立连接
	ChannelFuture f = b.connect(host, port).sync();
	f.channel().closeFuture().sync();
} finally {
	// 优雅关闭相关线程组资源
	group.shutdownGracefully();
}
```

**`ServerBootstrap` 是服务端的启动引导类/辅助类**，具体使用方法如下：
```java
// 1.bossGroup 用于接收连接，workerGroup 用于具体的处理
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
EventLoopGroup workerGroup = new NioEventLoopGroup();
try {
	//2.创建服务端启动引导/辅助类：ServerBootstrap
	ServerBootstrap b = new ServerBootstrap();
	//3.给引导类配置两大线程组,确定了线程模型
	b.group(bossGroup, workerGroup).
		   ......
	// 6.绑定端口
	ChannelFuture f = b.bind(port).sync();
	// 等待连接关闭
	f.channel().closeFuture().sync();
} finally {
	//7.优雅关闭相关线程组资源
	bossGroup.shutdownGracefully();
	workerGroup.shutdownGracefully();
}
```

从上面的示例中，我们可以看出：
1. `Bootstrap` 通常使用 `connect()` 方法连接到远程的主机和端口，作为一个 Netty TCP 协议通信中的客户端。另外，Bootstrap 也可以通过 `bind()` 方法绑定本地的一个端口，作为 UDP 协议通信中的一端。
2. `ServerBootstrap`通常使用 `bind()` 方法绑定本地的端口上，然后等待客户端的连接。
3. `Bootstrap` 只需要配置一个线程组— EventLoopGroup ,而 ServerBootstrap需要配置两个线程组— EventLoopGroup ，一个用于接收连接，一个用于具体的 IO 处理。

# Channel（网络操作抽象类）

Channel 接口是 Netty 对网络操作抽象类。通过 Channel 我们可以进行 I/O 操作。

一旦客户端成功连接服务端，就会新建一个 Channel 同该用户端进行绑定，示例代码如下：


















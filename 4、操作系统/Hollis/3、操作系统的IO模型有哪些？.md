#IO 
对比：[Java IO模型常见面试题](2、相关技术/1、Java基础/IO/Java%20IO模型常见面试题.md)、[36、什么是AIO、BIO和NIO？](2、相关技术/1、Java基础/IO/Hollis/36、什么是AIO、BIO和NIO？.md)
# 1、典型回答

为了保护操作系统的安全，通过缓存加快系统读写，会将内存分为 **用户空间和内核空间** 两个部分。**如果用户想要操作内核空间的数据，则需要把数据从内核空间拷贝到用户空间（数据会放到内核空间的page cache中，这种也叫缓存IO）**。

举个栗子，如果服务器收到了从客户端过来的请求，并且想要进行处理，那么需要经过这几个步骤：
1. 服务器的网络驱动接受到消息之后，向内核申请空间，并在收到完整的数据包（这个过程会产生延时，因为有可能是通过分组传送过来的）后，将其复制到内核空间；
2. 数据从内核空间拷贝到用户空间；
3. 用户程序进行处理。

![1.jpg](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404201143154.jpg)

我们再详细的探究服务器中的文件读取，对于Linux来说，Linux是一个将所有的外部设备都看作是文件来操作的操作系统，在它看来：everything is a file，那么我们就把对与外部设备的操作都看作是对文件进行操作。而且我们对一个文件进行读写，都需要通过调用内核提供的系统调用。

而在Linux中，一个基本的IO会涉及到两个系统对象：一个是调用这个IO的进程对象（用户进程），另一个是系统内核。也就是说，当一个read操作发生时，将会经历这些阶段：
- 通过read系统调用，向内核发送读请求；
- 内核向硬件发送读指令，并等待读就绪；
- DMA把将要读取的数据复制到指定的内核缓存区中；
- **内核将数据从内核缓存区拷贝到用户进程空间中。**
![1.jpg](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404201150819.jpg)

正是由于上面的几个阶段，导致了file中的数据被用户进程消费是需要过程的，这也就延伸出了五种IO方式，分别是同步阻塞型IO模型、同步非阻塞型IO模型、IO复用模型、信号驱动模型以及异步IO模型

# 2、扩展知识

我们通过小J要去银行柜台办事，拿号排队的例子来分别说一下这五种IO模型。

## 2.1、同步阻塞IO模型

从系统调用recv到将数据从内核复制到用户空间并返回，在这段时间内进程始终阻塞。就相当于，小J想去柜台办理业务，如果柜台业务繁忙，他也要排队，直到排到他办理完业务，才能去做别的事。显然，这个IO模型是同步且阻塞的。
![2.jpg](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404201153440.jpg)

## 2.2、同步非阻塞IO模型

在这里recv不管有没有获得到数据都返回，如果没有数据的话就过段时间再调用recv看看，如此循环。就像是小J来柜台办理业务，发现柜员休息，他离开了，过一会又过来看看营业了没，直到终于碰到柜员营业了，这才办理了业务。而小J在中间离开的时间，可以做他自己的事情。![2.jpg](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404201456250.jpg)
但是这个模型只有在**检查无数据的时候是非阻塞**的，在**数据到达的时候依然要等待复制数据到用户空间（办理业务），因此它还是同步IO**。

## 2.3、IO复用模型

**在IO复用模型中，调用recv之前会先调用select或poll，这两个系统调用都可以在内核准备好数据（网络数据已经到达内核了）时告知用户进程，它准备好了，这时候再调用recv时是一定有数据的。因此在这一模型中，进程阻塞于select或poll，而没有阻塞在recv上。** 就相当于，小J来银行办理业务，大堂经理告诉他现在所有柜台都有人在办理业务，等有空位再告诉他。于是小J就等啊等（select或poll调用中），过了一会儿大堂经理告诉他有柜台空出来可以办理业务了，但是具体是几号柜台，你自己找下吧，于是小J就只能挨个柜台地找。

![1.jpg](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404201505325.jpg)

## 2.4、信号驱动IO模型

**此处会通过调用sigaction注册信号函数，在内核数据准备好的时候系统就中断当前程序，执行信号函数（在这里调用recv）**。相当于，小J让大堂经理在柜台有空位的时候通知他（注册信号函数），等没多久大堂经理通知他，因为他是银行的VIPPP会员，所以专门给他开了一个柜台来办理业务，小J就去特席柜台办理业务了。但即使在等待的过程中是非阻塞的，但在办理业务的过程中依然是同步的。

## 2.5、异步IO模型

调用aio_read令内核把数据准备好，并且复制到用户进程空间后执行事先指定好的函数。就像是，小J交代大堂经理把业务给办理好了就通知他来验收，在这个过程中小J可以去做自己的事情。这就是真正的异步IO。
![1.jpg](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404201509991.jpg)

我们可以看到，前四种模型都是属于同步IO，因为在内核数据复制到用户空间的这一过程都是阻塞的。而最后一种异步IO，通过将IO操作交给操作系统处理，当前进程不关心具体IO的实现，后来再通过回调函数，或信号量通知当前进程直接对IO返回结果进行处理。

# 3、知识扩展
## 3.1、什么是同步、异步、阻塞、非阻塞？

[2、同步、异步、阻塞、非阻塞怎么理解？](4、操作系统/Hollis/2、同步、异步、阻塞、非阻塞怎么理解？.md)

## 3.2、如何理解select、poll、epoll？

[4、如何理解select、poll、epoll？](4、操作系统/Hollis/4、如何理解select、poll、epoll？.md)


# 4、ChatGPT补充

## 4.1、I/O多路复用介绍

<font color="red" size=5><font color="blue" size=5>I/O 多路复用</font>：用一个线程监视多个 I/O 事件，并在有事件时才处理</font>

I/O 多路复用（I/O Multiplexing）是操作系统提供的一种 **高效处理多个文件描述符（如 socket）读写事件** 的机制，广泛应用于高并发网络编程中，如 Nginx、Redis、Netty、Java NIO 等。

### 一、I/O 多路复用的背景与问题

在早期的服务模型中：
- **每个客户端连接创建一个线程**或进程处理。
- 如果连接数太多，会引起 **线程/进程资源浪费、上下文切换频繁**。
- 而大多数网络连接是 **"长时间等待，短时间处理"**，如：
    - 客户端连接但长时间没发请求。
    - Redis 等服务读数据只用很短时间，但大多数时间是空等。

因此，需要一种方式可以<font color="red" size=5>用一个线程监视多个 I/O 事件，并在有事件时才处理</font>——这就是 I/O 多路复用。

### 二、I/O 模型对比

| 模型       | 特点                                     |
| -------- | -------------------------------------- |
| 阻塞 I/O   | 调用阻塞函数（如 `read()`），**一直等到数据准备好并完成读操作** |
| 非阻塞 I/O  | `read()` 等函数立即返回，不管有没有数据               |
| I/O 多路复用 | **一个线程监听多个 fd 的状态变化**                  |
| 信号驱动 I/O | 号机制（如 `SIGIO`）通知数据可读                   |
| 异步 I/O   | 统完成整个 I/O 操作并通知用户                      |

### 三、I/O 多路复用的三种实现方式
#### 1. `select`
- 最早实现。
- 将多个 fd 添加到一个 bitmap 中，由内核监视。
- 缺点：
    - **fd 数量有限（1024）**。
    - **每次调用都要拷贝整个 fd 集合**。
    - 每次都要重新构造 fd 集合。
    - 内核扫描所有 fd，效率低。
#### 2. `poll`
- 使用链表替代 bitmap，支持更多 fd。
- 与 `select` 相比：
    - **打破了 fd 数量上限**。
    - 但仍需 **每次重新构建 fd 列表**。
    - 扫描所有 fd（O(n)），效率仍低。
#### 3. `epoll`（Linux特有）
- **事件驱动方式（event-driven）**。
- 使用事件通知机制，不再遍历所有 fd。
- 有 3 个重要函数：

|函数|作用|
|---|---|
|`epoll_create`|创建 epoll 实例，返回 epoll fd|
|`epoll_ctl`|添加、修改或删除要监听的 fd|
|`epoll_wait`|等待事件发生（可以阻塞或带超时）|
- 优点：
    - **不会随着 fd 数量增长而降低性能**（O(1)）。
    - 支持**边缘触发（edge-triggered）**和**水平触发（level-triggered）模式。
    - 内核和用户空间通过共享内存传递事件，**无需重复复制 fd 列表**。

### 四、I/O 多路复用流程图（epoll 示例）

```
程序启动
   ↓
创建 epoll 实例：epoll_create
   ↓
注册关注事件：epoll_ctl
   ↓
循环等待事件：epoll_wait（阻塞）
   ↓
一旦事件就绪，处理读写
   ↓
继续 epoll_wait（循环）
```

### 五、代码示例（C语言简化版，使用 epoll）

```c
int epfd = epoll_create(1024);
struct epoll_event ev, events[10];

ev.events = EPOLLIN;  // 监听读事件
ev.data.fd = listen_fd;
epoll_ctl(epfd, EPOLL_CTL_ADD, listen_fd, &ev);

while (1) {
    int nfd = epoll_wait(epfd, events, 10, -1); // 阻塞等待事件
    for (int i = 0; i < nfd; i++) {
        int fd = events[i].data.fd;
        if (fd == listen_fd) {
            int client = accept(fd, NULL, NULL);
            ev.events = EPOLLIN;
            ev.data.fd = client;
            epoll_ctl(epfd, EPOLL_CTL_ADD, client, &ev);
        } else {
            // 处理 client fd 的读写
        }
    }
}
```

### 六、应用场景

- 高并发服务器（如 Nginx、Redis、MySQL）。
- Java NIO（Netty、Tomcat、Spring WebFlux）。
- Node.js 底层（libuv）。
- 多客户端聊天室、即时通讯系统等。

### 七、优缺点总结

#### 优点：

- 一个线程能同时处理多个 I/O 事件。
- 高并发性能优异。
- 节省线程/进程资源。
- `epoll` 性能优秀（高效、无上限、事件驱动）。

#### 缺点：

- 编程复杂度高，需要自己管理事件驱动逻辑。
- 对文件 I/O 不适用（仅支持 socket 等网络 I/O）。
- 边缘触发模式处理不当容易造成“丢事件”。

### 八、Java 中的体现

Java 的 NIO 底层使用 I/O 多路复用：

- `Selector` 对应操作系统的 `epoll`/`select`。
- `SocketChannel` 配合 `Selector` 实现非阻塞网络通信。
- `Netty`、`Tomcat NIO` 都使用了 Selector。

如果你对某一部分（如 Java 中怎么实现、epoll 边缘触发机制、select/poll 的底层结构等）感兴趣，我可以进一步深入讲解。

## 4.2、Java中I/O多路复用

在 Java 中，I/O 多路复用是通过 **NIO（New I/O）** 提供的 `Selector` 机制实现的。Java NIO 底层基于操作系统提供的多路复用机制（Linux 是 epoll，Windows 是 IOCP，macOS 是 kqueue），从而实现高效的网络通信。

### 一、核心组件

| 类名                    | 作用描述                               |
| --------------------- | ---------------------------------- |
| `Selector`            | 多路复用器，监视多个通道（Channel）上的事件          |
| `SelectableChannel`   | 支持非阻塞 I/O 的通道（如 `SocketChannel`）   |
| `SelectionKey`        | 代表 Channel 在 Selector 上的注册关系及其事件状态 |
| `ServerSocketChannel` | 服务端监听连接的通道                         |
| `SocketChannel`       | 客户端读写数据的通道                         |
| `ByteBuffer`          | 数据缓冲区，用于读写数据                       |

### 二、Java NIO 多路复用原理图

```
          ┌────────────────────────────┐
          │         Selector           │
          └────────────┬───────────────┘
                       │
         ┌─────────────┴────────────┐
         │           注册事件        │
         ↓                          ↓
┌────────────────┐        ┌──────────────────┐
│ServerSocketChannel│     │  SocketChannel   │
│（监听连接）       │     │（读写数据）       │
└────────────────┘        └──────────────────┘
         ↑                          ↑
         └───── 注册感兴趣的事件（如 ACCEPT、READ） ─────┘
```

### 三、核心流程（服务端代码示例）

```java
public class NioServerExample {
    public static void main(String[] args) throws IOException {
        // 1. 创建 Selector 和 Channel
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(8080));
        serverChannel.configureBlocking(false); // 设置非阻塞

        // 2. 注册事件到 Selector，监听“接收连接”事件
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("服务启动：监听端口 8080");

        // 3. 循环等待事件发生
        while (true) {
            selector.select(); // 阻塞等待有事件发生
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove(); // 移除已处理的 key

                if (key.isAcceptable()) {
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel client = server.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                    System.out.println("新连接：" + client.getRemoteAddress());
                }

                if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int len = client.read(buffer);
                    if (len > 0) {
                        buffer.flip();
                        String msg = Charset.defaultCharset().decode(buffer).toString();
                        System.out.println("收到消息：" + msg);
                        client.write(ByteBuffer.wrap(("Echo: " + msg).getBytes()));
                    } else {
                        client.close(); // 客户端断开连接
                    }
                }
            }
        }
    }
}
```

### 四、Java NIO 的运行机制（底层原理）

#### 1. 注册事件

- 每个 `Channel`（如 `SocketChannel`）通过 `register()` 方法注册到 `Selector` 上。
- 并指定感兴趣的事件类型（如 `OP_READ`, `OP_WRITE`, `OP_ACCEPT`）。

#### 2. `selector.select()` 机制

- 实际底层会调用 OS 的 `epoll_wait`（在 Linux 上）。
- **阻塞直到有注册事件发生，然后返回事件集。**
- `Selector` 会维护一组 `SelectionKey`，表示哪些通道有事件。

#### 3. 响应事件处理

- 拿到事件后，调用 `SelectionKey.isAcceptable()`、`isReadable()` 等方法判断事件类型。
- 调用对应处理逻辑。

### 五、Selector 支持的事件类型

|事件常量|说明|
|---|---|
|`SelectionKey.OP_ACCEPT`|接收连接事件（仅服务端）|
|`SelectionKey.OP_CONNECT`|连接就绪事件（客户端）|
|`SelectionKey.OP_READ`|数据可读事件|
|`SelectionKey.OP_WRITE`|数据可写事件|

### 六、Selector 多路复用的优势

| 优势        | 说明                   |
| --------- | -------------------- |
| 单线程处理多个连接 | 节省线程资源、减少上下文切换       |
| 非阻塞 I/O   | **通道不阻塞**，数据未准备好也不挂起 |
| 基于事件驱动    | 有事件才处理，无事件时 CPU 负载低  |
| 灵活扩展      | 可同时支持上万个连接，适合高并发     |

### 七、Netty 等高级框架的实现机制

- Netty 是对 Java NIO 的封装和扩展，内部使用多个 Selector。
- 使用 Reactor 模式，分为：
    - `BossGroup`（监听连接）
    - `WorkerGroup`（处理读写）
- 避免了重复创建 Selector、过多 channel 切换的问题。

### 八、注意事项

|问题|说明|
|---|---|
|`Selector` 线程不安全|多线程使用时要加锁或使用多个 Selector|
|事件未及时清理|`selector.selectedKeys().remove(key)` 必须调用|
|`select()` 空轮询问题|某些 JDK 版本存在 bug，可能导致空轮询，占满 CPU|
|非阻塞细节易出错|忘记调用 `configureBlocking(false)` 会导致注册失败或阻塞|

- `selector.select()` 是阻塞方法，但它 **只阻塞线程以等待事件**，而不是像传统阻塞 I/O 一样直接阻塞在 `read()`。
- Java NIO 所谓的“非阻塞 I/O”，是指对 socket 的读写操作不会阻塞。
- 所以这两者并不矛盾，只要理解“阻塞的是事件等待，而非数据操作”就清楚了。
对应在代码：
```java
selector.select(); // 阻塞等待事件发生
// select() 返回后，说明“某些 Channel 有事件发生”
for (SelectionKey key : selector.selectedKeys()) {
    if (key.isReadable()) {
        SocketChannel channel = (SocketChannel) key.channel();
        // 这里的 read 是非阻塞的，读不到数据会返回 0
        channel.read(buffer);
    }
}
```

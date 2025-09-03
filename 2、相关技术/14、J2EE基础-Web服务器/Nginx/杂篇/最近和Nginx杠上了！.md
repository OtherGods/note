
> Nginx（engine x）是一个高性能的 HTTP 和反向代理 Web 服务器，同时也提供了 IMAP/POP3/SMTP 服务。

Nginx 以高性能和高可用性备受广大程序员的青睐，今天我们会从 Nginx 的整体架构入手，介绍 Nginx 进程结构，进程之间的关系以及如何对进程进行控制和管理。

今天大家会学到如下内容：

- Nginx 总体架构
- Nginx 进程定义
- Nginx 启动过程
- Master 启动过程
- 进程之间的信号发送方式
- 进程协助处理网络请求

# 1、Nginx 总体架构

对于传统的 HTTP 和反向代理服务器而言，在处理并发请求的时候会使用单进程或线程的模式处理，同时会止网络或输入/输出操作。

这种方式会消耗大量的内存和 CPU 资源。因为每产生一个单独的进程或线程需要准备一套新的运行时环境，包括分配堆和堆栈内存，以及创建新的执行上下文。

可以想象在处理多请求时会生成对应数目的线程或进程，导致由于线程在不断上下文切换上耗费大量资源。

由于上面的原因，Nginx 在设计之初就使用了模块化、事件驱动、异步处理，非阻塞的架构。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403192152354.png)

_图 1：Nginx 总体架构_

让我们通过一张图来了解 Nginx 的总结架构，如图 1 所示：

1. Nginx 启动时，并不会马上处理网络请求，负责调度工作进程。包括 Load configuration（加载配置），Launch workers（启动工作进程）以及 Non-stop upgrade（平滑升级）。
   
   因此在 Nginx 启动以后，在操作系统中会看到 Master 和 Worker 两类进程。在图上方的 Master 进程负责加载分析配置文件、启动/管理 Worker 进程以及平滑升级。
   
   一个 Master 进程可以管理多个 Worker 进程，而 Worker 进程负责处理并响应用户请求，也就是来自图左边的 HTTP/HTTPS 请求。

2. 由于网络请求属于 IO 请求，为了应对高并发 Nginx 采取了 kevent/epoll/select 模式的多路复用技术。由于采取了这种技术每个 Worker 进程都可以同时处理数以千计的网络请求。

3. 为了处理网络请求在 Worker 中会包含模块，分为核心模块和功能性模块。
   
   核心模块负责维持一个运行循环（run-loop），执行网络请求处理的不同阶段的模块功能，如网络读写、存储读写、内容传输、外出过滤，以及将请求发往上游服务器等。
   
   而围绕着核心模块会有一些功能模块，就是实现具体的请求处理功能的。例如有处理 http 请求的 ht_core 模块，有实现负载均衡的 ht_upstream 模块，以及实现 FastCGI 的 ht_fastcgi 模块。
   
   这些模块会负责与后端的服务器进行交互，完成用户请求，同时可以根据需要的功能自由加载模块，甚至可以扩展第三方的模块。
   
4. Worker 进程可以和本地磁盘进行数据通信，支持 Advanced I/O（高级I/O）、sendfile 机制、AIO 机制、mmap 等机制等。
   
通过上面的介绍发现 Nginx 不会为每个连接生成一个进程或线程，而是通过 Worker 进程使用多路复用的方式处理多个请求。
   
这里会使用到共享监听套接字的方式接受新请求，并在每个 Worker 内执行高效的运行循环，从而达到每个 Worker 处理成千上万的连接。

Work 启动后将创建一组侦听套接字，并且在处理 HTTP 请求和响应过程中不断接受，读取和写入套接字信息。

运行循环（run-loop）包括全面的内部调用，并且在很大程度上依赖异步任务处理。

通过模块化，事件通知，回调函数和定时器来支撑异步操作的实现。其目的是为了实现高并发请求下的不阻塞（尽可能不阻赛）。 

基于上面的机制，Nginx 通过检查网络和存储的状态并初始化新连接，将其添加到运行循环中，并异步处理直到其完成，处理完毕的连接会被重新分配并从运行循环中删除。因此 Nginx 可以在极端工作负载下实现较低的CPU使用率。

另外，由于 Work 会在磁盘上进行写入操作，为了避免磁盘 I/O 上的阻塞请求，特别是磁盘满的情况。

可以设置机制和配置文件指令来减轻此类磁盘 I/O 阻塞情况的发生。例如使用 sendfile 和 AIO 组合选项提升磁盘性能。

# 2、Nginx 进程定义

从架构介绍我们知道 Nginx 是由不同的进程组成的，这些进程各司其职用来处理高并发下的网络请求，接下来就看看他们的定义和如何工作的。

上面介绍了 Nginx 的总体架构，其中重点提到了 Master 进程和 Worker 进程，其实还有另外两个进程在架构中也起到了重要的作用。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403192209418.png)
_图 2：Nginx 的四类进程_

这里我们一起通过图 2 来认识他们：
- **Master 进程：它作为父进程会在 Nginx 初始化的时候生成并启动，其他的进程都是它子进程，Master 进程对其他进行进行创建和管理。**
- **Worker 进程：是 Master 的子进程负责处理网络请求。这里需要说明一下 Nginx 为什么采用了多进程而不是多线程的结构，其原因是为了保证高可用性，进程不像线程那样共享地址空间，也避免了当一个线程中的第三方模块出错引而影响其他其他线程的情况发生。**
- **Cache Manager 和 Cache Loader 进程：Cache Loader 负责缓存载入，Cache Manager 负责缓存管理，每一个请求所使用的缓存还是由 Worker 来决定的，而进程间通信都是通过共享内存来实现的。**

从上图大家一定注意到了只有 Worker 进程是多个，这是因为 Nginx 采用了事件驱动的模型。

为了提高处理请求的效率每个 Worker 进程会找那个一个 CPU内核，提高 CPU 的缓存命中率，将某个 Worker 进程与一个 CPU 核绑定在一起。

需要说明的是，我们需要根据具体的应用场景来定义 Worker 进程的数量：
- **CPU 密集型请求，**例如，处理大量 TCP/IP，执行 SSL 或压缩，需要 Worker 数与 CPU 内核数量相匹配。
- **IO 密集型请求，**Worker 数需要是 CPU 内核数量的一到两倍。

上面提到了 Master 通过控制多个 Worker 进程来处理网络请求，对于 Worker 的独立进程来说使用资源的时候不需要考虑不需要加锁的问题，节省了因为加锁带来的系统开销。同时多进程的设计让进程之间不会互相影响。

当一个进程退出后，其它进程还在工作，Nginx 所提供的网络请求服务不会因为其中一个进程的退出而中断，Master 进程一旦发现有 Worker 进程退出会启动新的 Worker 进程。

这里我们会发现 Master 进程为了控制 Worker 需要对其进行通信，同时 Worker 进程也需要与 Master 进程交换信息。

# 3、Nginx 启动过程

谈到了 Master 进程如此的重要，那么一起来看看 Nginx 进程的启动过程。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403192215367.png)
_图 3：Nginx 启动 Master 进程_

如图 3 所示，在 Nginx 启动的时候会根据配置文件进行解析和初始化的工作，同时会从主进程中 fork 出一个 Master 进程作为自己的子进程，也就是启动 Master 进程，此时 Master 进程就诞生了。

Nginx 的主进程在 fork 出 Master 进程以后就退出了。然后 Master 进程会 fork 并启动 Worker 进程，以及 Cache Manager 、Cache Loader 进程，接着 Master 进程会进入主循环。

需要注意的是，这里使用的 fork 会复制一个和当前启动进程具有相同代码段、数据段、堆和栈、fd 等信息的子进程。

也就是说我们说的四类进程都是通过 Nginx 启动进程复制出来的子进程。

# 4、Master 启动过程

接着上面的流程 Master 进程被 fork 之后，会执行 ngx_master_process_cycle 函数。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403192217128.png)
_图 4：Master 进程执行 ngx_master_process_cycle 函数_

如图 4 所示，这个函数主要进行如下操作：
- 设置进程的初始信号掩码，屏蔽相关信号。
- Master 进程 fork 出 Worker 、Cache Manager 以及 Cache Loader 等子进程。
- 进入主循环，通过 sigsuspend 系统调用，等待着信号的到来。
- 一旦信号到来，会进入信号处理程序 ngx_signal_handler。
- 信号处理程序执行之后，程序执行流程会判断各种状态位，来执行不同的操作。

上面的流程中提到了几个概念，这里对其进行说明，以便我们更好的理解 Master 进程的执行过程。

1. **信号**：用来完成进程中信息传递的媒介。Master 进程的主循环里面，一直通过等待各种信号事件，来处理不同的指令。
   
   这个信号可以传递给 Master 进程，也可以从 Master 进程传递给其他的进程。
   
   信号分为标准信号和实时信号，标准信号是从 1-31，实时信号是从 32-64。例如：INT、QUIT、KILL 就是标准信号。Master 进程监听的信号也是标准信号。
   
   标准信号和实时信号的区别是：标准信号，是基于位的标记，假设在阻塞等待的时候，多个相同的信号到来，最终解除阻塞时，只会传递一次信号，无法统计等待期间信号的计数。
   
   而实时信号是通过队列来实现，在阻塞等待的时候，多个相同的实时信号会存放到队列中。一旦解除阻塞的时候，会将队列中的信号都进行传递，结果会收到多次信号。

2. **信号处理器**：信号处理器是指当捕获指定信号时（传递给进程）时将会调用的一个函数，它存在与进程中，它可以随时打断进程的主程序流程。
   
3. **发送信号**：发送信号的操作可以使用 kill 这个 shell 命令完成。比如 kill -9 pid，就是发送 KILL 信号。
   
   kill -INT pid 就是发送 INT 信号。与 shell 命令类似，可以使用 kill 系统调用来向进程发送信号。

4. **信号掩码**：用来控制信号阻赛的编码方式。每个进程都有一个信号掩码（signal mask），也称为信号屏蔽字，它规定了当前要屏蔽或要阻塞递送到该进程的信号集。
   
   对于每种可能的信号，该掩码中都有一位与之对应。对于某种信号，若其对应位（bit）已设置，则它当前是被阻塞的。
   
   简单地说，信号掩码是一个“位图”，其中每一位都对应着一种信号。如果位图中的某一位为 1，就表示在执行当前信号集的处理程序期间相应的信号暂时被“屏蔽”或“阻塞”，使得在执行的过程中不会嵌套地响应那个信号。
   
   说白了就是使用信号编码来阻赛信号，告诉其他发送信号的进程说：“我在忙着处理事情，你先等等，等会我再处理你的信号。”

# 5、进程之间的信号发送方式

有了上面的基础以后再回头看看 Nginx 中进程中是如何进行信息交互的，以及 Master 是如何通过信号与 Worker 进行沟通的。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403192226392.png)
_图 5：Master 与 Worker 通信_

从图 5 中可以看出，Master 可以接受 TERM，INT、QUIT、HUP、USR1、USR2、WINCH 这些信号，这些信号的含义会在后面提供一张表给大家解释。

同时 Master 进程也可以给Worker进程传递信号，于是 Worker 进程可以接收以下信号：TERM，INT、QUIT、HUP、USR1、WINCH。之后 Worker 再去响应 Client 的请求。

这里先将信号的对应的命令和含义通过表格的方式列出来，再来对其进行讲解。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403192227166.png)

从表格上面看，每个信号都有特定的含义。比如 QUIT 信号表示优雅地关闭服务，并且对应 quit 命令。

这里的 quit 命令指的是可以通过命令行的方式对 Master 进程下命令，从而达到发送信号的效果，当然 Master 接受到命令以后会转化为信号发送给对应的 Worker 达到关闭服务的效果。

需要说明的是，Worker 是不会接受命令的，而是通过 Worker 接受命令来统一管理所有 Worker 的行为。

# 6、进程协助处理网络请求

知道 Master 与 Worker 之间如何通信之后再来看看它们是如何合作完成客户端请求的。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403192229695.png)
_图 6：Client 请求流程_

如图 6 所示，这里描绘了 Master 创建 Listen 以及 fork 出 Worker 的过程，以及客户端请求和 Worker 响应请求的过程。

1. 先从最上面开始看，顺着从上至下红色的箭头看，Master 进程创建以后会通过 socket 方法创建 socket 的 IO 通道。
   
   接着执行 bind 方法将其与监听器 listen 进行绑定，然后通过 fork 方法 fork 出多个 Worker 进程（绿色虚线）。

2. 在每个 Worker 进程中的 accept 方法就监听 socket 请求了，一旦 listen 监听到 socket 请求 Worker 进程就可以通过 accept 接受到。

3. 再看最下面的 client 模块，当 client 通过 connect 方法与 Nginx 发生连接时，所有拥有 accept 方法的 Worker 进程都会接受到来自 listen 的通知，但是只有一个 Worker 进程能够成功 accept 到，其他的进程则会失败。
   
   这里 Nginx 提供了一把共享锁 accept_mutex 来保证同一时刻只有一个 Worker 进程在 accept 连接，从而解决惊群问题。

4. 当 Worker 进程 accept 到 socket 请求以后，client 会通过 send 方法发送请求（绿色虚线）给 Worker。
   
   而 Worker 使用 recv 方法接受请求，同时通过 parse（解析）、process（处理）、generate（生成响应）几个步骤将返回的响应通过 send 方法传送给 client，而 client 会使用 recv 方法接受响应。
   
   最后 Worker 调用 close 方法断开和 client 的连接。

# 7、总结

本文从 Nginx 总体架构开始，介绍了 Nginx 的主要组成部分和处理流程。然后介绍 Nginx 的 4 个进程，以及 Nginx 在启动过程中这些进程都是如何产生的。

然后聚焦到最为主要的 Master 进程的启动过程做了哪些具体的事情，特别是 Master 进程和 Worker、Cache Manager、Cache Load 之间的关系。

在进程之间的信号发送方式的章节中，我们建立了信号、发送信号、信号处理、信号掩码的概念，这有助于理解进程之间的通信。

最后，趁热打铁把 Nginx 接受网络请求以及进程之间如何合作处理请求的过程进行了讲解。


转载自：[https://mp.weixin.qq.com/s/CxapDUkSdqBbuJU4JrQ8Aw](https://mp.weixin.qq.com/s/CxapDUkSdqBbuJU4JrQ8Aw)





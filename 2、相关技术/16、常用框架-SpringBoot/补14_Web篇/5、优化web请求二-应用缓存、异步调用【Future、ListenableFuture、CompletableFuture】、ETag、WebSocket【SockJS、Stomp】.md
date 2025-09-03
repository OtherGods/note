
## 四、应用缓存

　　使用spring应用缓存。使用方式：使用@EnableCache注解激活Spring的缓存功能，需要创建一个CacheManager来处理缓存。如使用一个内存缓存示例

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

package com.github.bjlhx15.gradle.demotest;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager(){
        SimpleCacheManager simpleCacheManager=new SimpleCacheManager();
        simpleCacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("searches")));
        return simpleCacheManager;
    }
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

　　其他实现如：EhCacheManager、GuavaCacheManager等

　　主要标记：

　　　　@CacheEvict：将会从缓存中移除一个条目

　　　　@CachePut:会将方法结果放到缓存中，而不会影响到方法调用本身。

　　　　@Caching：将缓存注解重新分组

　　　　@CacheConfig：指向不同的缓存配置

　　更多spring 应用缓存：[https://www.cnblogs.com/bjlhx/category/1233985.html](https://www.cnblogs.com/bjlhx/category/1233985.html)

## 五、分布式缓存

　　推荐使用redis，系列文章：[https://www.cnblogs.com/bjlhx/category/1066467.html](https://www.cnblogs.com/bjlhx/category/1066467.html)

　　spring使用也比较方便：[https://www.cnblogs.com/bjlhx/category/1233985.html](https://www.cnblogs.com/bjlhx/category/1233985.html)

## 六、异步方法-EnableAsync

　　在程序执行时候还有一个瓶颈，串行执行，可以通过使用不同线程类快速提升应用的速度。

　　要启用Spring的异步功能，必须要使用@EnableAsync注解。这样将会透明地使用java.util.concurrent.Executor来执行所有带有@Async注解的方法。

　　@Async所修饰的函数不要定义为static类型，这样异步调用不会生效

　　针对调用的Async，如果不做Future特殊处理，执行完调用方法会立即返回结果，如异步邮件发送，不会真的等邮件发送完毕才响应客户，如需等待可以使用Future阻塞处理。

## 6.1、原始使用

1、main方法增加@EnableAsync注解　

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

@SpringBootApplication
@EnableAsync
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        System.out.println("ThreadId:"+Thread.currentThread().getId());
    }
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

2、在所需方法增加@Async注解

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

@Component
public class Task {
    @Async
    public void doTaskOne() throws Exception {
        for (int i = 0; i < 3; i++) {
            Thread.sleep(200);
            System.out.println("ThreadId:"+Thread.currentThread().getId()+":doTaskOne");
        }
    }

    @Async
    public void doTaskTwo() throws Exception {
        for (int i = 0; i < 3; i++) {
            Thread.sleep(200);
            System.out.println("ThreadId:"+Thread.currentThread().getId()+":doTaskTwo");
        }
    }

    @Async
    public void doTaskThree() throws Exception {
        for (int i = 0; i < 3; i++) {
            Thread.sleep(200);
            System.out.println("ThreadId:"+Thread.currentThread().getId()+":doTaskThree");
        }
    }
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

3、查看调用

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

@RestController
public class TestAsyns {
    @Autowired
    private Task task;
    @RequestMapping("/testAsync")
    public ResponseEntity testAsync() throws Exception {
        task.doTaskOne();
        task.doTaskTwo();
        task.doTaskThree();
        return ResponseEntity.ok("ok");
    }
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

上述方法依次调用三个方法。

如果去除@EnableAsync注解，输出如下：【可见是串行执行】

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

ThreadId:33:doTaskOne
ThreadId:33:doTaskOne
ThreadId:33:doTaskOne
ThreadId:33:doTaskTwo
ThreadId:33:doTaskTwo
ThreadId:33:doTaskTwo
ThreadId:33:doTaskThree
ThreadId:33:doTaskThree
ThreadId:33:doTaskThree

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

如果增加@EnableAsync注解，输出如下：【可见是并行执行】

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

ThreadId:56:doTaskThree
ThreadId:55:doTaskTwo
ThreadId:54:doTaskOne
ThreadId:54:doTaskOne
ThreadId:55:doTaskTwo
ThreadId:56:doTaskThree
ThreadId:54:doTaskOne
ThreadId:56:doTaskThree
ThreadId:55:doTaskTwo

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

## 6.2、自定义执行器使用异步

1、配置类

　　方式一、注入Bean方式

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

import java.util.concurrent.Executor;  
  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.ComponentScan;  
import org.springframework.context.annotation.Configuration;  
import org.springframework.scheduling.annotation.EnableAsync;  
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;  
  
@Configuration  
public class ThreadConfig  {  
  
     // 执行需要依赖线程池，这里就来配置一个线程池  
     @Bean  
     public Executor getExecutor() {  
          ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();  
          executor.setCorePoolSize(5);  
          executor.setMaxPoolSize(10);  
          executor.setQueueCapacity(25);  
          executor.initialize();  
          return executor;  
     }  
}  

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

　　方式二、通过实现AsyncConfigurer接口，可以自定义默认的执行（executor）。新增如下配置类：

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

@Configuration
public class AsyncConfiguration implements AsyncConfigurer {
    protected final Logger logger = LoggerFactory.getLogger(AsyncConfiguration.class);

    @Override
    public Executor getAsyncExecutor() {
        //做好不超过10个，这里写两个方便测试
        return Executors.newFixedThreadPool(2);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex,method,params)->logger.error("Uncaught async error",ex);
    }
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

　　Executor的初始化配置，还有很多种，可以参看[https://www.cnblogs.com/bjlhx/category/1086008.html](https://www.cnblogs.com/bjlhx/category/1086008.html)

　　Spring 已经实现的异步线程池：  
　　　　1. SimpleAsyncTaskExecutor：不是真的线程池，这个类不重用线程，每次调用都会创建一个新的线程。  
　　　　2. SyncTaskExecutor：这个类没有实现异步调用，只是一个同步操作。只适用于不需要多线程的地方  
　　　　3. ConcurrentTaskExecutor：Executor的适配类，不推荐使用。如果ThreadPoolTaskExecutor不满足要求时，才用考虑使用这个类  
　　　　4. SimpleThreadPoolTaskExecutor：是Quartz的SimpleThreadPool的类。线程池同时被quartz和非quartz使用，才需要使用此类  
　　　　5. ThreadPoolTaskExecutor ：最常使用，推荐。 其实质是对java.util.concurrent.ThreadPoolExecutor的包装

　　使用上述配置，能够确保在应用中，用来处理异步任务的线程不会超过10个。这对于web应用很重要，因为每个客户端都会有一个专用的线程。你所使用的线程越多，阻塞时间越长那么能够处理的客户端就会越少。

　　如果设置成两个，程序中有3个异步线程，也会只有两个运行，如下

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

ThreadId:55:doTaskTwo
ThreadId:54:doTaskOne
ThreadId:55:doTaskTwo
ThreadId:54:doTaskOne
ThreadId:55:doTaskTwo
ThreadId:54:doTaskOne
ThreadId:55:doTaskThree
ThreadId:55:doTaskThree
ThreadId:55:doTaskThree

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

2、使用

　　同上述一致。

## 6.3、异步返回处理

### 方式一、使用Future【FutureTask是默认实现】处理+轮询处理【jdk1.5产物,没有提供Callback机制，只能主动轮询，通过get去获取结果】【不推荐】

修改异步执行的方法

![](https://images.cnblogs.com/OutliningIndicators/ContractedBlock.gif) View Code

修改调用的方法

![](https://images.cnblogs.com/OutliningIndicators/ContractedBlock.gif) View Code

### 方式二、Spring的ListenableFuture和CountDownLatch处理

Service实现类

![](https://images.cnblogs.com/OutliningIndicators/ContractedBlock.gif) View Code

调用方法

![](https://images.cnblogs.com/OutliningIndicators/ContractedBlock.gif) View Code

### 方式三、使用CompletableFuture【推荐】

User实体类

![](https://images.cnblogs.com/OutliningIndicators/ContractedBlock.gif) View Code

CompletableFuture的服务

![](https://images.cnblogs.com/OutliningIndicators/ContractedBlock.gif) View Code

调用方法

![](https://images.cnblogs.com/OutliningIndicators/ContractedBlock.gif) View Code

## 七、ETag

## 7.1、什么是ETag？ 

　　ETag：是实体标签(Entity Tag)的缩写。ETag一般不以明文形式相应给客户端。在资源的各个生命周期中，它都具有不同的值，用于标识出资源的状态。当资源发生变更时，如果其头信息中一个或者多个发生变化，或者消息实体发生变化，那么ETag也随之发生变化。

　　ETag值的变更说明资源状态已经被修改。往往可以通过时间戳就可以便宜的得到ETag头信息。在服务端中如果发回给消费者的相应从一开始起就由ETag控制，那么可以确保更细粒度的ETag升级完全由服务来进行控制。服务计算ETag值，并在相应客户端请求时将它返回给客户端。

## 7.2、计算ETag值

　　在HTTP1.1协议中并没有规范如何计算ETag。ETag值可以是唯一标识资源的任何东西，如持久化存储中的某个资源关联的版本、一个或者多个文件属性，实体头信息和校验值、(CheckSum)，也可以计算实体信息的散列值。有时候，为了计算一个ETag值可能有比较大的代价，此时可以采用生成唯一值等方式(如常见的GUID)。无论怎样，服务都应该尽可能的将ETag值返回给客户端。客户端不用关心ETag值如何产生，只要服务在资源状态发生变更的情况下将ETag值发送给它就行。

　　ETag值可以通过uuid、整数、长整形、字符串等四种类型。

　　计算ETag值时，需要考虑两个问题：计算与存储。如果一个ETag值只需要很小的代价以及占用很低的存储空间，那么我们可以在每次需要发送给客户端ETag值值的时候计算一遍就行行了。相反的，我们需要将之前就已经计算并存储好的ETag值发送给客户端。之前说：将时间戳作为字符串作为一种廉价的方式来获取ETag值。对于不是经常变化的消息，它是一种足够好的方案。注意：如果将时间戳做为ETag值，通常不应该用Last-Modified的值。由于HTTP机制中，所以当我们在通过服务校验资源状态时，客户端不需要进行相应的改动。计算ETag值开销最大的一般是计算采用哈希算法获取资源的表述值。可以只计算资源的哈希值，也可以将头信息和头信息的值也包含进去。如果包含头信息，那么注意不要包含计算机标识的头信息。同样也应该避免包含Expires、Cache-Control和Vary头信息。注意：在通过哈希算法。

## 7.3、ETag的类型以及他们之间的区别

　　ETag有两种类型：强ETag(strong ETag)与弱ETag(weak ETag)。

　　　　强ETag表示形式："22FAA065-2664-4197-9C5E-C92EA03D0A16"。

　　　　弱ETag表现形式：w/"22FAA065-2664-4197-9C5E-C92EA03D0A16"。

　　强、弱ETag类型的出现与[Apache](https://www.baidu.com/s?wd=Apache&tn=24004469_oem_dg&rsv_dl=gh_pl_sl_csd)服务器计算ETag的方式有关。Apache默认通过FileEtag中FileEtag INode Mtime Size的配置自动生成ETag(当然也可以通过用户自定义的方式)。假设服务端的资源频繁被修改(如1秒内修改了N次)，此时如果有用户将Apache的配置改为MTime，由于MTime只能精确到秒，那么就可以避免强ETag在1秒内的ETag总是不同而频繁刷新Cache(如果资源在秒级经常被修改，也可以通过Last-Modified来解决)。

## 7.4、Etag - 作用

Etag 主要为了解决 Last-Modified 无法解决的一些问题。

1、 一些文件也许会周期性的更改，但是他的内容并不改变(仅仅改变的修改时间)，这个时候我们并不希望客户端认为这个文件被修改了，而重新GET;

2、某些文件修改非常频繁，比如在秒以下的时间内进行修改，(比方说1s内修改了N次)，If-Modified-Since能检查到的粒度是s级的，这种修改无法判断(或者说UNIX记录MTIME只能精确到秒 

3、某些服务器不能精确的得到文件的最后修改时间；

　　为此，HTTP/1.1 引入了 Etag(Entity Tags).Etag仅仅是一个和文件相关的标记，可以是一个版本标记,比如说v1.0.0或者说"2e681a-6-5d044840"这么一串看起来很神秘的编码。但是HTTP/1.1标准并没有规定Etag的内容是什么或者说要怎么实现，唯一规定的是Etag需要放在""内。

## 7.5、Etag - 工作原理

Etag由服务器端生成，客户端通过If-Match或者说If-None-Match这个条件判断请求来验证资源是否修改。常见的是使用If-None-Match.请求一个文件的流程可能如下：

====第一次请求===  
1.客户端发起 HTTP GET 请求一个文件；

2.服务器处理请求，返回文件内容和一堆Header，当然包括Etag(例如"2e681a-6-5d044840")(假设服务器支持Etag生成和已经开启了Etag).状态码200

====第二次请求===  
1.客户端发起 HTTP GET 请求一个文件，注意这个时候客户端同时发送一个If-None-Match头，这个头的内容就是第一次请求时服务器返回的Etag：2e681a-6-5d044840

2.服务器判断发送过来的Etag和计算出来的Etag匹配，因此If-None-Match为False，不返回200，返回304，客户端继续使用本地缓存；

　　流程很简单，问题是，如果服务器又设置了Cache-Control:max-age和Expires呢，怎么办？  
　　答案是同时使用，也就是说在完全匹配If-Modified-Since和If-None-Match即检查完修改时间和Etag之后，服务器才能返回304.

##  7.6、在spring中实践

　　虽然对请求已经做了应用缓存等处理，但是持续请求一个restful接口请求还是会发送到服务端去读取缓存，即使结果没有发生改变，但结果本身还是会多次发送给用户，造成浪费带宽。

　　ETag是Web响应数据的一个散列（Hash），并且会在头信息中进行发送。客户端可以记住资源的ETag，并且通过If-None-Match头信息将最新的已知版本发送给服务器。如果在这段时间内请求没有发生变化的话，服务器就会返回304 Not Modified。

　　在Spring中有一个特殊的Servlet过滤器来处理ETag，名为ShallowEtagHeaderFilter。只需将此类注入即可：

    @Bean
    public Filter etagFilter(){
        return new ShallowEtagHeaderFilter();
    }

　　只要响应头没有缓存控制头信息的话，系统就会为你的响应生成ETag。

示例

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

    @GetMapping("/testNoChangeContent")
    public ResponseEntity testNoChangeContent(){
        return ResponseEntity.ok("OK");
    }
    @GetMapping("/testChangeContent")
    public ResponseEntity testChangeContent(){
        return ResponseEntity.ok("OK:"+LocalDateTime.now());
    }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

接口一、testNoChangeContent，是测试内容没有改变的，第一次请求是200，以后请求是304

接口二、testChangeContent，是测试内容有改变的，第一次请求是200，以后请求均是200

## 八、WebSocket

在优化web请求时，这是一种优化方案，在服务器端有可用数据时，就立即将其发送到客户端。通过多线程方式获取搜索结果，所以数据会分为多个块。这时可以一点点地进行发送，而不必等待所有结果。

## 8.1、概述

### 1、WebSocket

　　Http连接为一次请求(request)一次响应(response)，必须为同步调用方式。WebSocket 协议提供了通过一个套接字实现全双工通信的功能。一次连接以后，会建立tcp连接，后续客户端与服务器交互为全双工方式的交互方式，客户端可以发送消息到服务端，服务端也可将消息发送给客户端。

　　WebSocket 是发送和接收消息的底层API，WebSocket 协议提供了通过一个套接字实现全双工通信的功能。也能够实现 web 浏览器和 server 间的异步通信，全双工意味着 server 与浏览器间可以发送和接收消息。需要注意的是必须考虑浏览器是否支持。

### 2、SockJS

　　SockJS 是 WebSocket 技术的一种模拟。为了应对许多浏览器不支持WebSocket协议的问题，设计了备选SockJs。开启并使用SockJS后，它会优先选用Websocket协议作为传输协议，如果浏览器不支持Websocket协议，则会在其他方案中，选择一个较好的协议进行通讯。原来在不支持WebSocket的情况下，也可以很简单地实现WebSocket的功能的，方法就是使用 SockJS。它会优先选择WebSocket进行连接，但是当服务器或客户端不支持WebSocket时，会自动在 XHR流、XDR流、iFrame事件源、iFrame HTML文件、XHR轮询、XDR轮询、iFrame XHR轮询、JSONP轮询 这几个方案中择优进行连接。

### 3、Stomp

        STOMP 中文为: 面向消息的简单文本协议。websocket定义了两种传输信息类型: 文本信息和二进制信息。类型虽然被确定，但是他们的传输体是没有规定的。所以，需要用一种简单的文本传输类型来规定传输内容，它可以作为通讯中的文本传输协议,即交互中的高级协议来定义交互信息。

　　STOMP本身可以支持流类型的网络传输协议: websocket协议和tcp协议。

　　Stomp还提供了一个stomp.js,用于浏览器客户端使用STOMP消息协议传输的js库。

　　STOMP的优点如下:

　　（1）不需要自建一套自定义的消息格式

　　（2）现有stomp.js客户端(浏览器中使用)可以直接使用

　　（3）能路由信息到指定消息地点

　　（4）可以直接使用成熟的STOMP代理进行广播 如:RabbitMQ, ActiveMQ

### 4、WebSocket、SockJs、STOMP三者关系

　　简而言之，WebSocket 是底层协议，SockJS 是WebSocket 的备选方案，也是 底层协议，而 STOMP 是基于 WebSocket（SockJS） 的上层协议

1. 假设HTTP协议并不存在，只能使用TCP套接字来编写web应用，你可能认为这是一件疯狂的事情。
2. 不过幸好，我们有HTTP协议，它解决了 web 浏览器发起请求以及 web 服务器响应请求的细节。
3. 直接使用 WebSocket（SockJS） 就很类似于 使用 TCP 套接字来编写 web 应用；因为没有高层协议，因此就需要我们定义应用间所发送消息的语义，还需要确保 连接的两端都能遵循这些语义。
4. **同HTTP在TCP套接字上添加请求-响应模型层一样，STOMP在 WebSocket之上提供了一个基于帧的线路格式层，用来定义消息语义。**

## 8.2、不支持WebSocket的场景有：

　　浏览器不支持  
　　Web容器不支持，如tomcat7以前的版本不支持WebSocket  
　　防火墙不允许  
　　Nginx没有开启WebSocket支持

　　当遇到不支持WebSocket的情况时，SockJS会尝试使用其他的方案来连接，刚开始打开的时候因为需要尝试各种方案，所以会阻塞一会儿，之后可以看到连接有异常，那就是尝试失败的情况。

　　为了测试，使用Nginx做反向代理，把www.test.com指到项目启动的端口上，然后本地配HOST来达到模拟真实场景的效果。因为Nginx默认是不支持WebSocket的，所以这里模拟出了服务器不支持WebSocket的场景。、

## 8.3、spring下的WebSocket使用【WebSocket→sockJs→stomp】

项目中使用的pom

    compile 'org.springframework.boot:spring-boot-starter-websocket'
    compile 'org.springframework.boot:spring-messaging'
    compile group: 'org.webjars', name: 'sockjs-client', version: '1.1.2'
    compile group: 'org.webjars', name: 'stomp-websocket', version: '2.3.3'
    compile group: 'org.webjars', name: 'jquery', version: '3.3.1-1'

客户端JS

    <script src="/webjars/jquery/3.3.1-1/jquery.js"></script>
    <script src="/webjars/sockjs-client/1.1.2/sockjs.min.js"></script>
    <script src="/webjars/stomp-websocket/2.3.3/stomp.min.js"></script>
    <script src="test.js"></script>

### 8.3.1、使用Spring的底层级WebSocket API

　　Spring为WebSocket提供了良好支持，WebSocket协议允许客户端维持与服务器的长连接。数据可以通过WebSocket在这两个端点之间进行双向传输，因此消费数据的一方能够实时获取数据。

　　按照其最简单的形式，WebSocket只是两个应用之间通信的通道。位于WebSocket一端的应用发送消息，另外一端处理消息。因为它是全双工的，所以每一端都可以发送和处理消息。如图18.1所示。

　　　　![](https://img2018.cnblogs.com/blog/292888/201902/292888-20190215131840219-353033089.png)  
　　WebSocket通信可以应用于任何类型的应用中，但是WebSocket最常见的应用场景是实现服务器和基于浏览器的应用之间的通信。

实现步骤：

1、编写Handler消息处理器类

方法一：实现 WebSocketHandler 接口，WebSocketHandler 接口如下 

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

public interface WebSocketHandler {
    void afterConnectionEstablished(WebSocketSession session) throws Exception;
    void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception;
    void handleTransportError(WebSocketSession session, Throwable exception) throws Exception; 
    void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception; 
    boolean supportsPartialMessages();
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

方法二：扩展 AbstractWebSocketHandler

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

@Service
public class ChatHandler extends AbstractWebSocketHandler {
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        session.sendMessage(new TextMessage("hello world."));
    }
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

　　为了在Spring使用较低层级的API来处理消息，必须编写一个实现WebSocketHandler的类.WebSocketHandler需要我们实现五个方法。相比直接实现WebSocketHandler，更为简单的方法是扩展AbstractWebSocketHandler，这是WebSocketHandler的一个抽象实现。

　　除了重载WebSocketHandler中所定义的五个方法以外，我们还可以重载AbstractWebSocketHandler中所定义的三个方法：

- handleBinaryMessage()
- handlePongMessage()
- handleTextMessage()   
    这三个方法只是handleMessage()方法的具体化，每个方法对应于某一种特定类型的消息。

方案三、扩展TextWebSocketHandler或BinaryWebSocketHandler。

　　TextWebSocketHandler是AbstractWebSocketHandler的子类，它会拒绝处理二进制消息。它重载了handleBinaryMessage()方法，如果收到二进制消息的时候，将会关闭WebSocket连接。与之类似，BinaryWebSocketHandler也是AbstractWeb-SocketHandler的子类，它重载了handleTextMessage()方法，如果接收到文本消息的话，将会关闭连接。

2、增加websocket拦截器，管理用户

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            attributes.put("username","lhx");
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

- beforeHandshake()方法，在调用 handler 前调用。常用来注册用户信息，绑定 WebSocketSession，在 handler 里根据用户信息获取WebSocketSession发送消息

3、WebSocketConfig配置

方式一、注解配置

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private ChatHandler chatHandler;
    @Autowired
    private WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler,"/chat")
                .addInterceptors(webSocketHandshakeInterceptor);
    }
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192*4);
        container.setMaxBinaryMessageBufferSize(8192*4);
        return container;
    }
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

- 实现 WebSocketConfigurer 接口，重写 registerWebSocketHandlers 方法，这是一个核心实现方法，配置 websocket 入口，允许访问的域、注册 Handler、SockJs 支持和拦截器。
- registry.addHandler()注册和路由的功能，当客户端发起 websocket 连接，把 /path 交给对应的 handler 处理，而不实现具体的业务逻辑，可以理解为收集和任务分发中心。
- addInterceptors，顾名思义就是为 handler 添加拦截器，可以在调用 handler 前后加入我们自己的逻辑代码。
- ServletServerContainerFactoryBean可以添加对WebSocket的一些配置

方式二、xml配置

![](https://img2018.cnblogs.com/blog/292888/201902/292888-20190215134648794-939237679.png)

4、客户端配置

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

function contect() {
    var  wsServer = 'ws://'+window.location.host+'/chat';
    var  websocket = new WebSocket(wsServer);
    websocket.onopen = function (evt) { onOpen(evt) };
    websocket.onclose = function (evt) { onClose(evt) };
    websocket.onmessage = function (evt) { onMessage(evt) };
    websocket.onerror = function (evt) { onError(evt) };
    function onOpen(evt) {
        console.log("Connected to WebSocket server.");
        websocket.send("test");//客户端向服务器发送消息
    }
    function onClose(evt) {
        console.log("Disconnected");
    }
    function onMessage(evt) {
        console.log('Retrieved data from server: ' + evt.data);
    }
    function onError(evt) {
        console.log('Error occured: ' + evt.data);
    }
}

contect();

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

### 8.3.2、SockJs针对WebSocket支持稍差的场景

　　为了应对许多浏览器不支持WebSocket协议的问题，设计了备选`SockJs`。

　　SockJS 是 WebSocket 技术的一种模拟。SockJS 会 尽可能对应 WebSocket API，但如果 WebSocket 技术不可用的话，就会选择另外的通信方式协议。

1、服务端只需增加：.withSockJS()即可

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private ChatHandler chatHandler;
    @Autowired
    private WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // withSockJS() 方法声明我们想要使用 SockJS 功能，如果WebSocket不可用的话，会使用 SockJS；
        registry.addHandler(chatHandler,"/chat")
                .addInterceptors(webSocketHandshakeInterceptor).withSockJS();
    }
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

或者xml配置

<websocket:sockjs />

2、客户端

只需对请求

    // var  server = 'ws://'+window.location.host+'/chat';
    var  server = 'http://'+window.location.host+'/chatsockjs';
    var  websocket = new SockJS(server);

- SockJS 所处理的 URL 是 “http://“ 或 “https://“ 模式，而不是 “ws://“ or “wss://“；
- 其他的函数如 onopen, onmessage, and onclose ，SockJS 客户端与 WebSocket 一样，

### 8.3.3、Stomp方式

　　STOMP帧由命令，一个或多个头信息以及负载所组成。

　　直接使用WebSocket（或SockJS）就很类似于使用TCP套接字来编写Web应用。因为没有高层级的线路协议（wire protocol），因此就需要我们定义应用之间所发送消息的语义，还需要确保连接的两端都能遵循这些语义。   
　　不过，好消息是我们并非必须要使用原生的WebSocket连接。就像HTTP在TCP套接字之上添加了请求-响应模型层一样，STOMP在WebSocket之上提供了一个基于帧的线路格式（frame-based wire format）层，用来定义消息的语义。

　　乍看上去，STOMP的消息格式非常类似于HTTP请求的结构。与HTTP请求和响应类似，STOMP帧由命令、一个或多个头信息以及负载所组成。例如，如下就是发送数据的一个STOMP帧：

SEND
destination:/app/room-message
content-length:20

{\"message\":\"Hello!\"}

对以上代码分析：

1. SEND：STOMP命令，表明会发送一些内容；
2. destination：头信息，用来表示消息发送到哪里；
3. content-length：头信息，用来表示 负载内容的 大小；
4. 空行；
5. 帧内容（负载）内容

8.3.3.1、基本用法

1、服务端Configuration配置

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration  implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //添加一个/socket-server-point 连接端点，客户端就可以通过这个端点来进行连接；withSockJS作用是添加SockJS支持
        registry.addEndpoint("/socket-server-point").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //定义了一个客户端订阅地址的前缀信息，也就是客户端接收服务端发送消息的前缀信息
        registry.enableSimpleBroker("/topic");
        //定义了服务端接收地址的前缀，也即客户端给服务端发消息的地址前缀
        registry.setApplicationDestinationPrefixes("/ws");
    }
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

**对以上代码分析：**

1. EnableWebSocketMessageBroker 注解表明： 这个配置类不仅配置了 WebSocket，还配置了基于代理的 STOMP 消息；
2. 它复写了 registerStompEndpoints() 方法：添加一个服务端点，来接收客户端的连接。将 “/socket-server-point” 路径注册为 STOMP 端点。这个路径与之前发送和接收消息的目的路径有所不同， 这是一个端点，客户端在订阅或发布消息到目的地址前，要连接该端点，即用户发送请求 ：url=’/127.0.0.1:8080/socket-server-point’ 与 STOMP server 进行连接，之后再转发到订阅url；
3. 它复写了 configureMessageBroker() 方法：配置了一个 简单的消息代理，通俗一点讲就是设置消息连接请求的各种规范信息。
4. 发送应用程序的消息将会带有 “/ws” 前缀。

2、控制器以及逻辑开发  
Service服务处理

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

@Service
public class HandlerService {
    private static final Logger logger = LoggerFactory.getLogger(HandlerService.class);
    @Async
    public CompletableFuture<String> handle(String key) throws Exception {
        Thread.sleep(new Random().nextInt(3000));
        logger.info("Looking up " + key);
        key=key+":"+LocalDateTime.now();
        return CompletableFuture.completedFuture(key);
    }
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

Controller控制开发

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

    @MessageMapping("/searchBase")
    public ResponseEntity searchBase() throws Exception {
        Consumer<List<String>> callback = p -> websocket.convertAndSend("/topic/searchResults", p);
        List<String> list = Arrays.asList("bba", "aaa", "ccc");
        localSearch(list, callback);
        Map map = new HashMap();
        map.put("list", list);
        map.put("date", LocalDateTime.now());
        return ResponseEntity.ok(map);
    }

    public void localSearch(List<String> keys, Consumer<List<String>> callback) throws Exception {
        Thread.sleep(2000);
        List<String> list = new ArrayList<>();
        for (String key : keys) {
            CompletableFuture<String> completableFuture = handlerService.handle(key);
            completableFuture.thenAcceptAsync(p -> {
                list.clear();
                list.add(p);
                callback.accept(list);
            });
        }
    }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

8.3.3.2、消息流

　　![](https://img2018.cnblogs.com/blog/292888/201902/292888-20190215181607073-459543791.png)

8.3.3.3、**启用STOMP代理中继**

　　对于生产环境下的应用来说，你可能会希望使用真正支持STOMP的代理来支撑WebSocket消息，如RabbitMQ或ActiveMQ。这样的代理提供了可扩展性和健壮性更好的消息功能，当然它们也会完整支持STOMP命令。我们需要根据相关的文档来为STOMP搭建代理。搭建就绪之后，就可以使用STOMP代理来替换内存代理了，只需按照如下方式重载configureMessageBroker()方法即可：

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/queue", "/topic");
        //定义了一个客户端订阅地址的前缀信息，也就是客户端接收服务端发送消息的前缀信息
//        registry.enableSimpleBroker("/topic");
        //定义了服务端接收地址的前缀，也即客户端给服务端发消息的地址前缀
        registry.setApplicationDestinationPrefixes("/ws");
    }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

- 上述configureMessageBroker()方法的第一行代码启用了STOMP代理中继（broker relay）功能，并将其目的地前缀设置为“/topic”和“/queue”。这样的话，Spring就能知道所有目的地前缀为“/topic”或“/queue”的消息都会发送到STOMP代理中。
    
- 在第二行的configureMessageBroker()方法中将应用的前缀设置为“/ws”。所有目的地以“/ws”打头的消息都将会路由到带有@MessageMapping注解的方法中，而不会发布到代理队列或主题中。
    

默认情况下，STOMP代理中继会假设代理监听localhost的61613端口，并且客户端的username和password均为“guest”。如果你的STOMP代理位于其他的服务器上，或者配置成了不同的客户端凭证，那么我们可以在启用STOMP代理中继的时候，需要配置这些细节信息：

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableStompBrokerRelay("/queue", "/topic")
            .setRelayHost("rabbit.someotherserver")
            .setRelayPort(62623)
            .setClientLogin("marcopolo")
            .setClientPasscode("letmein01")
    registry.setApplicationDestinationPrefixes("/app");
  }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

### 8.3.3.4、处理来自客户端的STOMP消息

### 1、应用消息MessageMapping

Spring 4.0引入了@MessageMapping注解，它用于STOMP消息的处理，类似于Spring MVC的@RequestMapping注解。当消息抵达某个特定的目的地时，带有@MessageMapping注解的方法能够处理这些消息。

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

    /**
     * 处理来自客户端的STOMP消息
     * @param incoming
     * @return
     */
    @MessageMapping("/incoming")
    public Shout handleShout(Shout incoming) {
        logger.info("Received message: " + incoming.getMessage());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        Shout outgoing = new Shout();
        outgoing.setMessage("incoming!");
        return outgoing;
    }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

消息接受类

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

public class Shout {
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

客户端

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

function contect() {
    var socket=new SockJS('/socket-server-point');
    stompCliet=Stomp.over(socket);
    stompCliet.connect({},function (frame) {
        console.log('Connected ：'+frame);
        stompCliet.send("/ws/incoming",{},"{\"message\":\"Hello!\"}");
    });
}
contect();

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

### 2、订阅模式SubscribeMapping

@SubscribeMapping的主要应用场景是实现请求-回应模式。在请求-回应模式中，客户端订阅某一个目的地，然后预期在这个目的地上获得一个一次性的响应。   
例如，考虑如下@SubscribeMapping注解标注的方法：

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

    @SubscribeMapping("/sub")
    public Shout handleSubscription(){
        logger.info("Received message: " +"subscription");
        Shout outgoing = new Shout();
        outgoing.setMessage("subscription!");
        return outgoing;
    }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

当处理这个订阅时，handleSubscription()方法会产生一个输出的Shout对象并将其返回。然后，Shout对象会转换成一条消息，并且会按照客户端订阅时相同的目的地发送回客户端。

客户端

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

function contect() {
    var socket=new SockJS('/socket-server-point');
    stompCliet=Stomp.over(socket);
    stompCliet.connect({},function (frame) {
        console.log('Connected ：'+frame);
        stompCliet.subscribe('/ws/sub',function (result) {
            console.log("aaaa",JSON.parse(result.body));
        });
        stompCliet.send("/ws/sub",{},"{\"message\":\"Hello!\"}");
    });
}
contect();

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

这种请求-回应模式与HTTP GET的请求-响应模式并没有太大差别。但是，这里的关键区别在于HTTP GET请求是同步的，而订阅的请求-回应模式则是异步的，这样客户端能够在回应可用时再去处理，而不必等待。

### 8.3.3.5、发送消息到客户端

 Spring提供了两种发送数据给客户端的方法：

- 作为处理消息或处理订阅的附带结果；
- 使用消息模板。

**方式一、作为处理消息或处理订阅的附带结果、在处理消息之后，发送消息**

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

    @MessageMapping("/incoming")
    public Shout handleShout(Shout incoming) {
        logger.info("Received message: " + incoming.getMessage());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        Shout outgoing = new Shout();
        outgoing.setMessage("incoming!");
        return outgoing;
    }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

当@MessageMapping注解标示的方法有返回值的时候，返回的对象将会进行转换（通过消息转换器）并放到STOMP帧的负载中，然后发送给消息代理。

默认情况下，帧所发往的目的地会与触发处理器方法的目的地相同，只不过会添加上“/topic”前缀。就本例而言，这意味着handleShout()方法所返回的Shout对象会写入到STOMP帧的负载中，并发布到“/topic/incoming”目的地。不过，我们可以通过为方法添加@SendTo注解，重载目的地：

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

    @MessageMapping("/incoming")
    @SendTo("/topic/shout")
    public Shout handleShout(Shout incoming) {
        logger.info("Received message: " + incoming.getMessage());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        Shout outgoing = new Shout();
        outgoing.setMessage("incoming!");
        return outgoing;
    }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

按照这个@SendTo注解，消息将会发布到“/topic/shout”。所有订阅这个主题的应用（如客户端）都会收到这条消息。 

按照类似的方式，@SubscribeMapping注解标注的方式也能发送一条消息，作为订阅的回应。

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

    @SubscribeMapping("/sub")
    public Shout handleSubscription(){
        logger.info("Received message: " +"subscription");
        Shout outgoing = new Shout();
        outgoing.setMessage("subscription!");
        return outgoing;
    }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

@SubscribeMapping的区别在于这里的Shout消息将会直接发送给客户端，而不必经过消息代理。如果你为方法添加@SendTo注解的话，那么消息将会发送到指定的目的地，这样会经过代理。

对应客户端需要增加订阅即可

        stompCliet.subscribe('/ws/sub',function (result) {
            console.log("aaaa",JSON.parse(result.body));
        });

正如前面看到的那样，使用 @MessageMapping 或者 @SubscribeMapping 注解可以处理客户端发送过来的消息，并选择方法是否有返回值。

    如果 @MessageMapping 注解的控制器方法有返回值的话，返回值会被发送到消息代理，只不过会添加上"/topic"前缀。可以使用@SendTo 重写消息目的地；

    如果 @SubscribeMapping 注解的控制器方法有返回值的话，返回值会直接发送到客户端，不经过代理。如果加上@SendTo 注解的话，则要经过消息代理。

方式二、使用消息模板【在任意地方发送消息】

　　@MessageMapping和@SubscribeMapping提供了一种很简单的方式来发送消息，这是接收消息或处理订阅的附带结果。不过，Spring的SimpMessagingTemplate能够在应用的任何地方发送消息，甚至不必以首先接收一条消息作为前提。

　　我们不必要求用户刷新页面，而是让首页订阅一个STOMP主题.

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

function contect() {
    var socket=new SockJS('/socket-server-point');
    stompCliet=Stomp.over(socket);
    stompCliet.connect({},function (frame) {
        console.log('Connected ：'+frame);
        stompCliet.subscribe('/topic/sendDataToClient',function (result) {
            console.log("aaaa",JSON.parse(result.body));
        });
        // stompCliet.send("/ws/sub",{},"{\"message\":\"Hello!\"}");
    });
}
contect();

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

使用SimpMessagingTemplate能够在应用的任何地方发布消息

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

    @Autowired(required = false)
    private SimpMessagingTemplate websocket;

    @GetMapping("/sendDataToClient")
    public ResponseEntity sendDataToClient() throws Exception {
        Map map=new HashMap();
        map.put("aa","aaa");
        map.put("bb","bbb");
        websocket.convertAndSend("/topic/sendDataToClient",map);
        return ResponseEntity.ok("ok");
    }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

当然此处的SimpMessagingTemplate也可以使用父接口SimpMessageSendingOperations注入

在这个场景下，我们希望所有的客户端都能及时看到实时的/topic/sendDataToClient，这种做法是很好的。但有的时候，我们希望发送消息给指定的用户，而不是所有的客户端。 

### 8.3.3.6、为目标用户发送消息

　　以上说明了如何广播消息，订阅目的地的所有用户都能收到消息。如果消息只想发送给特定的用户呢？spring-websocket 介绍了以下

在使用Spring和STOMP消息功能的时候，我们有两种方式利用认证用户：

　　1、@MessageMapping和@SubscribeMapping标注的方法基于@SendToUser注解和Principal参数来获取认证用户；@MessageMapping、@SubscribeMapping和@MessageException方法返回的值能够以消息的形式发送给认证用户；

　　2、SimpMessageSendingOperations接口或SimpMessagingTemplate的convertAndSendToUser方法能够发送消息给特定用户。

1、在控制器中处理用户的消息

　　在控制器的@MessageMapping或@SubscribeMapping方法中，处理消息时有两种方式了解用户信息。在处理器方法中，通过简单地添加一个Principal参数，这个方法就能知道用户是谁并利用该信息关注此用户相关的数据。除此之外，处理器方法还可以使用@SendToUser注解，表明它的返回值要以消息的形式发送给某个认证用户的客户端（只发送给该客户端）。

　　@SendToUser 表示要将消息发送给指定的用户，会自动在消息目的地前补上"/user"前缀。如下，最后消息会被发布在  /user/queue/notifications-username。但是问题来了，这个username是怎么来的呢？就是通过 principal 参数来获得的。那么，principal 参数又是怎么来的呢？需要在spring-websocket 的配置类中重写 configureClientInboundChannel 方法，添加上用户的认证。

服务端增加configuration

![](https://images.cnblogs.com/OutliningIndicators/ContractedBlock.gif) View Code

服务端处理逻辑

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

    @MessageMapping("/shout")
    @SendToUser("/queue/notifications")
    public Shout userStomp(Principal principal, Shout shout) {
        String name = principal.getName();
        String message = shout.getMessage();
        logger.info("认证的名字是：{}，收到的消息是：{}", name, message);
        return shout;
    }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

前端js代码

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

var headers={
    username:'admin',
    password:'admin'
};
function contect() {
    var socket=new SockJS('/socket-server-point');
    stompCliet=Stomp.over(socket);
    stompCliet.connect(headers,function (frame) {
        console.log('Connected ：'+frame);
        stompCliet.subscribe('/user/queue/notifications',function (result) {
            console.log("aaaa",JSON.parse(result.body));
        });
        stompCliet.send("/ws/shout",{},"{\"message\":\"Hello!\"}");
    });
}
contect();

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

2、**convertAndSendToUser方法**

　　 **除了convertAndSend()以外，SimpMessageSendingOperations 还提供了convertAndSendToUser()方法。按照名字就可以判断出来，convertAndSendToUser()方法能够让我们给特定用户发送消息。**

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

    @MessageMapping("/singleShout")
    public void singleUser(Shout shout, StompHeaderAccessor stompHeaderAccessor) {
        String message = shout.getMessage();
        LOGGER.info("接收到消息：" + message);
        Principal user = stompHeaderAccessor.getUser();
        simpMessageSendingOperations.convertAndSendToUser(user.getName(), "/queue/shouts", shout);
    }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

　　如上，这里虽然我还是用了认证的信息得到用户名。但是，其实大可不必这样，因为 convertAndSendToUser 方法可以指定要发送给哪个用户。也就是说，完全可以把用户名的当作一个参数传递给控制器方法，从而绕过身份认证！convertAndSendToUser 方法最终会把消息发送到 /user/sername/queue/shouts 目的地上。

8.3.3.7、处理消息异常

　　在处理消息的时候，有可能会出错并抛出异常。因为STOMP消息异步的特点，发送者可能永远也不会知道出现了错误。@MessageExceptionHandler标注的方法能够处理消息方法中所抛出的异常。我们可以把错误发送给用户特定的目的地上，然后用户从该目的地上订阅消息，从而用户就能知道自己出现了什么错误

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

     @MessageExceptionHandler(Exception.class)
     @SendToUser("/queue/errors")
     public Exception handleExceptions(Exception t){
         t.printStackTrace();
         return t;
     }

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

8.3.3.8、更多stomp配置

1、发起连接

其中headers表示客户端的认证信息:

若无需认证，直接使用空对象 “{}” 即可；

 （1）connectCallback 表示连接成功时（服务器响应 CONNECTED 帧）的回调方法； 

 （2）errorCallback 表示连接失败时（服务器响应 ERROR 帧）的回调方法，非必须；

默认链接端点

//默认的和STOMP端点连接
/*stomp.connect("guest", "guest", function (franme) {
});*/

有用户认证的

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

var headers={
    username:'admin',
    password:'admin'
};

stomp.connect(headers, function (frame) {

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

示例

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

// 建立连接对象（还未发起连接）
 var socket=new SockJS("/endpointChat"); 
 // 获取 STOMP 子协议的客户端对象 
 var stompClient = Stomp.over(socket); 
 // 向服务器发起websocket连接并发送CONNECT帧 
 stompClient.connect( {}, 
function connectCallback (frame) { 
     // 连接成功时（服务器响应 CONNECTED 帧）的回调方法 
console.log('已连接【' + frame + '】'); 
//订阅一个消息
     stompClient.subscribe('/topic/getResponse',
function (response) { 
showResponse(response.body);
});
 },
     function errorCallBack (error) { 
     // 连接失败时（服务器响应 ERROR 帧）的回调方法 
console.log('连接失败【' + error + '】'); 
} );

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

2、断开连接

若要从客户端主动断开连接，可调用 disconnect() 方法：

client.disconnect(
function () { 
    alert("断开连接");
});

3、发送消息

连接成功后，客户端可使用 send() 方法向服务器发送信息：

client.send(destination url, headers, body);

其中： 

（1）destination url 为服务器 controller中 @MessageMapping 中匹配的URL，字符串，必须参数； 

（2）headers 为发送信息的header，JavaScript 对象，可选参数； 

（3）body 为发送信息的 body，字符串，可选参数；  
示例

client.send("/queue/test", {priority: 9}, "Hello, STOMP");
client.send("/queue/test", {}, "Hello, STOMP");

4、订阅、接收消息

STOMP 客户端要想接收来自服务器推送的消息，必须先订阅相应的URL，即发送一个 SUBSCRIBE 帧，然后才能不断接收来自服务器的推送消息。

订阅和接收消息通过 subscribe() 方法实现：

subscribe(destination url, callback, headers)

其中 

（1）destination url 为服务器 @SendTo 匹配的 URL，字符串； 

（2）callback 为每次收到服务器推送的消息时的回调方法，该方法包含参数 message； 

（3）headers 为附加的headers，JavaScript 对象；该方法返回一个包含了id属性的 JavaScript 对象，可作为 unsubscribe() 方法的参数；默认情况下，如果没有在headers额外添加，这个库会默认构建一个独一无二的ID。在传递headers这个参数时，可以使用你自己id。  
示例

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

var headers = {
ack: 'client',
//这个客户端指定了它会确认接收的信息，只接收符合这个selector : location = 'Europe'的消息。
 'selector': "location = 'Europe'",
//id:’myid’
}; 
var callback = function(message) {
if (message.body) {
 alert("got message with body " +JSON.parse( message.body)) }
 else{
alert("got empty message"); 
} }); 
var subscription = client.subscribe("/queue/test", callback, headers);
 
如果想让客户端订阅多个目的地，你可以在接收所有信息的时候调用相同的回调函数：
onmessage = function(message) {
    // called every time the client receives a message
}
var sub1 = client.subscribe("queue/test", onmessage);
var sub2 = client.subscribe("queue/another", onmessage)

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

5、取消订阅

var subscription = client.subscribe(...);
 
subscription.unsubscribe();

6、事务支持

可以在将消息的发送和确认接收放在一个事务中。

客户端调用自身的begin()方法就可以开始启动事务了，begin()有一个可选的参数transaction，一个唯一的可标识事务的字符串。如果没有传递这个参数，那么库会自动构建一个。

这个方法会返回一个object。这个对象有一个id属性对应这个事务的ID，还有两个方法：

commit()提交事务
 
abort()中止事务

在一个事务中，客户端可以在发送/接受消息时指定transaction id来设置transaction。

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

// start the transaction
 
var tx = client.begin();
 
// send the message in a transaction
 
client.send("/queue/test", {transaction: tx.id}, "message in a transaction");
 
// commit the transaction to effectively send the message
 
tx.commit();

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

如果你在调用send()方法发送消息的时候忘记添加transction header，那么这不会称为事务的一部分，这个消息会直接发送，不会等到事务完成后才发送。

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

var txid = "unique_transaction_identifier";
 
// start the transaction
 
var tx = client.begin();
 
// oops! send the message outside the transaction
 
client.send("/queue/test", {}, "I thought I was in a transaction!");
 
tx.abort(); // Too late! the message has been sent

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

7、消息确认

默认情况，在消息发送给客户端之前，服务端会自动确认（acknowledged）。

客户端可以选择通过订阅一个目的地时设置一个ack header为client或client-individual来处理消息确认。

在下面这个例子，客户端必须调用message.ack()来通知客户端它已经接收了消息。

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

var subscription = client.subscribe("/queue/test",
    function(message) {
        // do something with the message
        ...
        // and acknowledge it
        message.ack();
    },
    {ack: 'client'}
);

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

ack()接受headers参数用来附加确认消息。例如，将消息作为事务(transaction)的一部分，当要求接收消息时其实代理（broker）已经将ACK STOMP frame处理了。

var tx = client.begin();
message.ack({ transaction: tx.id, receipt: 'my-receipt' });
tx.commit();

ack()也可以用来通知STOMP 1.1.brokers（代理）：客户端不能消费这个消息。与ack()方法的参数相同。

8、debug调试

有一些测试代码能有助于你知道库发送或接收的是什么，从而来调试程序。

客户端可以将其debug属性设置为一个函数，传递一个字符串参数去观察库所有的debug语句。

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

client.debug = function(str) {
 
    // append the debug log to a #debug div somewhere in the page using JQuery:
 
    $("#debug").append(str + "\n");
};

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

默认情况，debug消息会被记录在在浏览器的控制台。

9、心跳机制

如果STOMP broker(代理)接收STOMP 1.1版本的帧，heart-beating是默认启用的。heart-beating也就是频率，incoming是接收频率，outgoing是发送频率。

通过改变incoming和outgoing可以更改客户端的heart-beating(默认为10000ms)：

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

client.heartbeat.outgoing = 20000;
 
// client will send heartbeats every 20000ms
 
client.heartbeat.incoming = 0;
 
// client does not want to receive heartbeats
 
// from the server

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

heart-beating是利用window.setInterval()去规律地发送heart-beats或者检查服务端的heart-beats。

更多

![](https://images.cnblogs.com/OutliningIndicators/ExpandedBlockStart.gif)

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

stomp.connect(headers, function (frame) {

    //发送消息
    //第二个参数是一个头信息的Map，它会包含在STOMP的帧中
    //事务支持
    var tx = stomp.begin();
    stomp.send("/app/marco", {transaction: tx.id}, strJson);
    tx.commit();


    //订阅服务端消息 subscribe(destination url, callback[, headers])
    stomp.subscribe("/topic/marco", function (message) {
        var content = message.body;
        var obj = JSON.parse(content);
        console.log("订阅的服务端消息：" + obj.message);
    }, {});


    stomp.subscribe("/app/getShout", function (message) {
        var content = message.body;
        var obj = JSON.parse(content);
        console.log("订阅的服务端直接返回的消息：" + obj.message);
    }, {});


    /*以下是针对特定用户的订阅*/
    var adminJSON = JSON.stringify({'message': 'ADMIN'});
    /*第一种*/
    stomp.send("/app/singleShout", {}, adminJSON);
    stomp.subscribe("/user/queue/shouts",function (message) {
        var content = message.body;
        var obj = JSON.parse(content);
        console.log("admin用户特定的消息1：" + obj.message);
    });
    /*第二种*/
    stomp.send("/app/shout", {}, adminJSON);
    stomp.subscribe("/user/queue/notifications",function (message) {
        var content = message.body;
        var obj = JSON.parse(content);
        console.log("admin用户特定的消息2：" + obj.message);
    });

    /*订阅异常消息*/
    stomp.subscribe("/user/queue/errors", function (message) {
        console.log(message.body);
    });

    //若使用STOMP 1.1 版本，默认开启了心跳检测机制（默认值都是10000ms）
    stomp.heartbeat.outgoing = 20000;

    stomp.heartbeat.incoming = 0; //客户端不从服务端接收心跳包
});

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

参看代码：https://github.com/JMCuixy/SpringWebSocket



转载自：[https://www.cnblogs.com/bjlhx/p/10364385.html](https://www.cnblogs.com/bjlhx/p/10364385.html)



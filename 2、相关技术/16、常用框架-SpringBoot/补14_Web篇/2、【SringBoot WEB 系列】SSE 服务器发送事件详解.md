
SSE 全称`Server Sent Event`，直译一下就是服务器发送事件，一般的项目开发中，用到的机会不多，可能很多小伙伴不太清楚这个东西，到底是干啥的，有啥用

本文主要知识点如下：
- SSE 扫盲，应用场景分析
- 借助异步请求实现 sse 功能，加深概念理解
- 使用`SseEmitter`实现一个简单的推送示例

# 1、SSE扫盲
## 1.1、概念介绍

sse(`Server Sent Event`)，直译为服务器发送事件，顾名思义，也就是客户端可以获取到服务器发送的事件

我们常见的 http 交互方式是客户端发起请求，服务端响应，然后一次请求完毕；但是在 sse 的场景下，客户端发起请求，连接一直保持，服务端有数据就可以返回数据给客户端，这个返回可以是多次间隔的方式

## 1.2、特点分析

SSE 最大的特点，可以简单规划为两个
- 长连接
- 服务端可以向客户端推送信息

了解 websocket 的小伙伴，可能也知道它也是长连接，可以推送信息，但是它们有一个明显的区别

**sse 是单通道，只能服务端向客户端发消息；而 webscoket 是双通道**

那么为什么有了 webscoket 还要搞出一个 sse 呢？既然存在，必然有着它的优越之处

| sse          | websocket        |
| ------------ | ---------------- |
| http 协议      | 独立的 websocket 协议 |
| 轻量，使用简单      | 相对复杂             |
| 默认支持断线重连     | 需要自己实现断线重连       |
| 文本传输         | 二进制传输            |
| 支持自定义发送的消息类型 | -                |

## 1.3、应用场景

从 sse 的特点出发，我们可以大致的判断出它的应用场景，需要轮询获取服务端最新数据的 case 下，多半是可以用它的

比如显示当前网站在线的实时人数，法币汇率显示当前实时汇率，电商大促的实时成交额等等...

# 2、手动实现sse功能

sse 本身是有自己的一套玩法的，后面会进行说明，这一小节，则主要针对 sse 的两个特点`长连接 + 后端推送数据`，如果让我们自己来实现这样的一个接口，可以怎么做？

## 1.1、项目创建

借助 SpringBoot 2.2.1.RELEASE来创建一个用于演示的工程项目，核心的 xml 依赖如下
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.2.1.RELEASE</version>
    <relativePath/><!-- lookup parent from repository -->
</parent>

<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <java.version>1.8</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>

<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </pluginManagement>
</build>

<repositories>
    <repository>
        <id>spring-snapshots</id>
        <name>Spring Snapshots</name>
        <url>https://repo.spring.io/libs-snapshot-local</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>

    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/libs-milestone-local</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>

	<repository>
        <id>spring-releases</id>
        <name>Spring Releases</name>
        <url>https://repo.spring.io/libs-release-local</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```

## 2.2、功能实现

在 Http1.1 支持了长连接，请求头添加一个`Connection: keep-alive`即可

在这里我们借助异步请求来实现 sse 功能，至于什么是异步请求，推荐查看博文: [3、【WEB 系列】异步请求知识点与使用姿势小结](2、相关技术/16、常用框架-SpringBoot/补14_Web篇/3、【WEB%20系列】异步请求知识点与使用姿势小结.md)

因为后端可以不定时返回数据，所以我们需要注意的就是需要保持连接，不要返回一次数据之后就断开了；其次就是需要设置请求头`Content-Type: text/event-stream;charset=UTF-8` （如果不是流的话会怎样？）
```java
// 新建一个容器，保存连接，用于输出返回  
private Map<String, PrintWriter> responseMap = new ConcurrentHashMap<>();  
  
// 发送数据给客户端  
private void writeData(String id, String msg, boolean over) throws IOException {  
    PrintWriter writer = responseMap.get(id);  
    if (writer == null) {  
        return;  
    }  
      
    writer.println(msg);  
    writer.flush();  
    if (over) {  
        responseMap.remove(id);  
    }  
}  
  
// 推送  
@ResponseBody  
@GetMapping(path = "subscribe")  
public WebAsyncTask<Void> subscribe(String id, HttpServletResponse response) {  
      
    Callable<Void> callable = () -> {  
        response.setHeader("Content-Type", "text/event-stream;charset=UTF-8");  
        responseMap.put(id, response.getWriter());  
        writeData(id, "订阅成功", false);  
        while (true) {  
            Thread.sleep(1000);  
            if (!responseMap.containsKey(id)) {  
                break;  
            }  
        }  
        returnnull;  
    };  
      
    // 采用WebAsyncTask 返回 这样可以处理超时和错误 同时也可以指定使用的Excutor名称  
    WebAsyncTask<Void> webAsyncTask = new WebAsyncTask<>(30000, callable);  
    // 注意：onCompletion表示完成，不管你是否超时、是否抛出异常，这个函数都会执行的  
    webAsyncTask.onCompletion(() -> System.out.println("程序[正常执行]完成的回调"));  
      
    // 这两个返回的内容，最终都会放进response里面去===========  
    webAsyncTask.onTimeout(() -> {  
        responseMap.remove(id);  
        System.out.println("超时了!!!");  
        returnnull;  
    });  
    // 备注：这个是Spring5新增的  
    webAsyncTask.onError(() -> {  
        System.out.println("出现异常!!!");  
        return null;  
    });  
      
      
    return webAsyncTask;  
}
```

看一下上面的实现，基本上还是异步请求的那一套逻辑，请仔细看一下`callable`中的逻辑，有一个 while 循环，来保证长连接不中断

接下来我们新增两个接口，用来模拟后端给客户端发送消息，关闭连接的场景
```java
@ResponseBody  
@GetMapping(path = "push")  
public String push![00.gif](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406262332628.gif)
Data(String id, String content) throws IOException {  
    writeData(id, content, false);  
    return"over!";  
}  
  
@ResponseBody  
@GetMapping(path = "over")  
public String over(String id) throws IOException {  
    writeData(id, "over", true);  
    return"over!";  
}
```

![00.gif](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406262334288.gif)
# 3、SseEmitter

上面只是简单实现了 sse 的长连接 + 后端推送消息，但是与标准的 SSE 还是有区别的，sse 有自己的规范，而我们上面的实现，实际上并没有管这个，导致的问题是前端按照 sse 的玩法来请求数据，可能并不能正常工作

## 3.1、sse规范

在 html5 的定义中，服务端 sse，一般需要遵循以下要求

**请求头**

开启长连接 + 流方式传递

```
Content-Type: text/event-stream;charset=UTF-8Cache-Control: no-cacheConnection: keep-alive
```

**数据格式**

服务端发送的消息，由 message 组成，其格式如下:

```
field:value\n\n
```

其中 field 有五种可能

- 空: 即以`:`开头，表示注释，可以理解为服务端向客户端发送的心跳，确保连接不中断
- data：数据
- event: 事件，默认值
- id: 数据标识符用 id 字段表示，相当于每一条数据的编号
- retry: 重连时间

## 3.2、实现

SpringBoot 利用 SseEmitter 来支持 sse，可以说非常简单了，直接返回`SseEmitter`对象即可；重写一下上面的逻辑
```java
@RestController  
@RequestMapping(path = "sse")  
public class SseRest  
{  
    privatestatic Map<String, SseEmitter>sseCache =new ConcurrentHashMap<>();  
      
    @GetMapping(path = "subscribe")  
    public SseEmitter push(String id)  
    {  
        // 超时时间设置为1小时  
        SseEmitter sseEmitter = new SseEmitter(3600_000L);  
        sseCache.put(id, sseEmitter);  
        sseEmitter.onTimeout(() -> sseCache.remove(id));  
        sseEmitter.onCompletion(() -> System.out.println("完成！！！"));  
        return sseEmitter;  
    }  
      
    @GetMapping(path = "push")  
    public String push(String id, String content) throws IOException  
    {  
        SseEmitter sseEmitter = sseCache.get(id);  
        if (sseEmitter != null)  
        {  
            sseEmitter.send(content);  
        }  
        return "over";  
    }  
      
    @GetMapping(path = "over")  
    public String over(String id)  
    {  
        SseEmitter sseEmitter = sseCache.get(id);  
        if (sseEmitter != null)  
        {  
            sseEmitter.complete();  
            sseCache.remove(id);  
        }  
        return "over";  
    }  
}
```

上面的实现，用到了 SseEmitter 的几个方法，解释如下

- `send()`: 发送数据，如果传入的是一个非`SseEventBuilder`对象，那么传递参数会被封装到 data 中
- `complete()`: 表示执行完毕，会断开连接
- `onTimeout()`: 超时回调触发
- `onCompletion()`: 结束之后的回调触发

同样演示一下访问请求
![00.gif](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406272201466.gif)
上图总的效果和前面的效果差不多，而且输出还带上了前缀，接下来我们写一个简单的 html 消费端，用来演示一下完整的 sse 的更多特性
```html
<!doctype html>  
<html lang="en">  
<head>  
    <title>Sse测试文档</title>  
</head>  
<body>  
<div>sse测试</div>  
<div id="result"></div>  
</body>  
</html>  
<script>  
    var source = new EventSource('http://localhost:8080/sse/subscribe?id=yihuihui');  
    source.onmessage = function (event) {  
        text = document.getElementById('result').innerText;  
        text += '\n' + event.data;  
        document.getElementById('result').innerText = text;  
    };  
    <!-- 添加一个开启回调 -->    source.onopen = function (event) {  
        text = document.getElementById('result').innerText;  
        text += '\n 开启: ';  
        console.log(event);  
        document.getElementById('result').innerText = text;  
    };  
</script>
```

将上面的 html 文件放在项目的`resources/static`目录下；然后修改一下前面的`SseRest`
```java
@Controller  
@RequestMapping(path = "sse")  
public class SseRest  
{  
    @GetMapping(path = "")  
    public String index()  
    {  
        return "index.html";  
    }  
      
    @ResponseBody  
    @GetMapping(path = "subscribe", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})  
    public SseEmitter push(String id)  
    {  
        // 超时时间设置为3s，用于演示客户端自动重连  
        SseEmitter sseEmitter = new SseEmitter(1_000L);  
        // 设置前端的重试时间为1s  
        sseEmitter.send(SseEmitter.event().reconnectTime(1000).data("连接成功"));  
        sseCache.put(id, sseEmitter);  
        System.out.println("add " + id);  
        sseEmitter.onTimeout(() ->  
        {  
            System.out.println(id + "超时");  
            sseCache.remove(id);  
        });  
        sseEmitter.onCompletion(() -> System.out.println("完成！！！"));  
        return sseEmitter;  
    }  
}
```

我们上面超时时间设置的比较短，用来测试下客户端的自动重连，如下，开启的日志不断增加![00.gif](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406272210280.gif)
其次将 SseEmitter 的超时时间设长一点，再试一下数据推送功能
![00.gif](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406272215260.gif)
请注意上面的演示，当后端结束了长连接之后，客户端会自动重新再次连接，不用写额外的重试逻辑了，就这么神奇

## 3.3、小结

本篇文章介绍了 SSE 的相关知识点，并对比 websocket 给出了 sse 的优点（至于啥优点请往上翻）

请注意，本文虽然介绍了两种 sse 的方式，第一种借助异步请求来实现，如果需要完成 sse 的规范要求，需要自己做一些适配，如果需要了解 sse 底层实现原理的话，可以参考一下；在实际的业务开发中，推荐使用`SseEmitter`


# 4、其他
## 4.1、项目
 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/220-web-sse](https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/220-web-sse)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)

转载自：[【WEB系列】SSE服务器发送事件详解](https://spring.hhui.top/spring-blog/2020/04/01/200401-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8B%E4%B9%8BSSE%E6%9C%8D%E5%8A%A1%E5%99%A8%E5%8F%91%E9%80%81%E4%BA%8B%E4%BB%B6%E8%AF%A6%E8%A7%A3/)

# 参考资料

1. https://github.com/liuyueyi/spring-boot-demo: https://github.com/liuyueyi/spring-boot-demo_
2. https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/220-web-sse: _https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/220-web-sse_
3. https://blog.hhui.top: _https://blog.hhui.top_
4. http://spring.hhui.top: _http://spring.hhui.top_

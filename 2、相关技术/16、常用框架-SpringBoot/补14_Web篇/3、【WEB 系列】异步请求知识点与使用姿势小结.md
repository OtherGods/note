
在 Servlet3.0 就引入了异步请求的支持，但是在实际的业务开发中，可能用过这个特性的童鞋并不多？

本篇博文作为异步请求的扫盲和使用教程，将包含以下知识点

- 什么是异步请求，有什么特点，适用场景
- 四种使用姿势：
	- AsyncContext 方式
	- Callable
	- WebAsyncTask
	- DeferredResult

# 1、异步请求

异步对于我们而言，应该属于经常可以听到的词汇了，在实际的开发中多多少少都会用到，那么什么是异步请求呢

## 1. 异步请求描述

**先介绍一下同步与异步：**

一个正常调用，吭哧吭哧执行完毕之后直接返回，这个叫同步；

接收到调用，自己不干，新开一个线程来做，主线程自己则去干其他的事情，等后台线程吭哧吭哧的跑完之后，主线程再返回结果，这个就叫异步

**异步请求：**

我们这里讲到的异步请求，主要是针对 web 请求而言，后端响应请求的一种手段，同步/异步对于前端而言是无感知、无区别的

同步请求，后端接收到请求之后，直接在处理请求线程中，执行业务逻辑，并返回
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406231130818.png)

异步请求，后端接收到请求之后，新开一个线程，来执行业务逻辑，释放请求线程，避免请求线程被大量耗时的请求沾满，导致服务不可用
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406231130895.png)

## 2. 特点

通过上面两张图，可以知道异步请求的最主要特点
- 业务线程，处理请求逻辑
- 请求处理线程立即释放，通过回调处理线程返回结果

## 3. 场景分析

从特点出发，也可以很容易看出异步请求，更适用于耗时的请求，快速的释放请求处理线程，避免 web 容器的请求线程被打满，导致服务不可用

举一个稍微极端一点的例子，比如我以前做过的一个多媒体服务，提供图片、音视频的编辑，这些服务接口有同步返回结果的也有异步返回结果的；同步返回结果的接口有快有慢，大部分耗时可能`<10ms`，而有部分接口耗时则在几十甚至上百

这种场景下，耗时的接口就可以考虑用异步请求的方式来支持了，避免占用过多的请求处理线程，影响其他的服务

# II. 使用姿势

接下来介绍四种异步请求的使用姿势，原理一致，只是使用的场景稍有不同

## 1. AsyncContext

在 Servlet3.0+之后就支持了异步请求，第一种方式比较原始，相当于直接借助 Servlet 的规范来实现，当然下面的 case 并不是直接创建一个 servlet，而是借助`AsyncContext`来实现

```java
@RestController  
@RequestMapping(path = "servlet")  
public class ServletRest {  
  
    @GetMapping(path = "get")  
    public void get(HttpServletRequest request) {  
        AsyncContext asyncContext = request.startAsync();  
        asyncContext.addListener(new AsyncListener() {  
            @Override  
            public void onComplete(AsyncEvent asyncEvent) throws IOException {  
                System.out.println("操作完成:" + Thread.currentThread().getName());  
            }  
  
            @Override  
            public void onTimeout(AsyncEvent asyncEvent) throws IOException {  
                System.out.println("超时返回!!!");  
                asyncContext.getResponse().setCharacterEncoding("utf-8");  
                asyncContext.getResponse().setContentType("text/html;charset=UTF-8");  
                asyncContext.getResponse().getWriter().println("超时了！！！!");  
            }  
  
            @Override  
            public void onError(AsyncEvent asyncEvent) throws IOException {  
                System.out.println("出现了m某些异常");  
                asyncEvent.getThrowable().printStackTrace();  
  
                asyncContext.getResponse().setCharacterEncoding("utf-8");  
                asyncContext.getResponse().setContentType("text/html;charset=UTF-8");  
                asyncContext.getResponse().getWriter().println("出现了某些异常哦！！！!");  
            }  
  
            @Override  
            public void onStartAsync(AsyncEvent asyncEvent) throws IOException {  
                System.out.println("开始执行");  
            }  
        });  
  
        asyncContext.setTimeout(3000L);  
        asyncContext.start(new Runnable() {  
            @Override  
            public void run() {  
                try {  
                    Thread.sleep(Long.parseLong(request.getParameter("sleep")));  
                    System.out.println("内部线程：" + Thread.currentThread().getName());  
                    asyncContext.getResponse().setCharacterEncoding("utf-8");  
                    asyncContext.getResponse().setContentType("text/html;charset=UTF-8");  
                    asyncContext.getResponse().getWriter().println("异步返回!");  
                    asyncContext.getResponse().getWriter().flush();  
                    // 异步完成，释放  
                    asyncContext.complete();  
                } catch (Exception e) {  
                    e.printStackTrace();  
                }  
            }  
        });  
  
        System.out.println("主线程over!!! " + Thread.currentThread().getName());  
    }  
}
```

完整的实现如上，简单的来看一下一般步骤

- `javax.servlet.ServletRequest#startAsync()`获取`AsyncContext`
- 添加监听器 `asyncContext.addListener(AsyncListener)`（这个是可选的）

- 用户请求开始、超时、异常、完成时回调

- 设置超时时间 `asyncContext.setTimeout(3000L)` （可选）
- 异步任务`asyncContext.start(Runnable)`

## 2. Callable

`Callable` 接口可以用于 Spring MVC 控制器方法，以异步方式处理请求。Spring MVC 会在后台线程中执行 `Callable` 的 `call` 方法。

相比较于上面的复杂的示例，SpringMVC 可以非常 easy 的实现，直接返回一个`Callable`即可
```java
@RestController  
@RequestMapping(path = "call")  
public class CallableRest {  
  
    @GetMapping(path = "get")  
    public Callable<String> get() {  
        Callable<String> callable = new Callable<String>() {  
            @Override  
            public String call() throws Exception {  
                System.out.println("do some thing");  
                Thread.sleep(1000);  
                System.out.println("执行完毕，返回!!!");  
                return "over!";  
            }  
        };  
  
        return callable;  
    }  
  
  
    @GetMapping(path = "exception")  
    public Callable<String> exception() {  
        Callable<String> callable = new Callable<String>() {  
            @Override  
            public String call() throws Exception {  
                System.out.println("do some thing");  
                Thread.sleep(1000);  
                System.out.println("出现异常，返回!!!");  
                throw new RuntimeException("some error!");  
            }  
        };  
  
        return callable;  
    }  
}
```

ChatGPT对这段代码的解释及涉及到代码的介绍：
1. 异步处理的关键点
	1. **`Callable` 接口**：Java 并发库中的接口，用于封装一个任务，该任务在将来某个时间点执行，并返回结果或抛出异常。`Callable` 类似于 `Runnable`，但 `Callable` 可以返回结果，并且允许抛出异常。
	2. **Spring MVC 异步请求处理**：当 Spring MVC 控制器方法返回一个 `Callable` 对象时，Spring MVC 会在后台线程中异步执行 `Callable` 的 `call` 方法，而不是在主线程中同步执行。这种机制可以提高系统的并发能力，特别是在处理耗时任务时。
2. 详细的实现过程
	1. **异步请求启动**：当一个请求到达并匹配到 `get` 或 `exception` 方法时，Spring MVC 会立即返回一个异步结果给客户端，并释放当前线程以处理其他请求。
	2. **异步处理**：Spring MVC 通过 `TaskExecutor` 来管理和执行这些异步任务。`TaskExecutor` 是 Spring 提供的一个接口，允许配置不同的线程池策略。
		1. 默认情况下，Spring MVC 使用 `SimpleAsyncTaskExecutor`，它会为每个任务创建一个新线程。
		2. 你可以通过配置 TaskExecutor 来使用其他类型的线程池，例如 ThreadPoolTaskExecutor，来优化性能和资源使用。
	3. **任务执行**：`Callable` 的 `call` 方法将在后台线程中执行，不会阻塞主线程。这是异步处理的核心。比如在 `get` 方法中：
	4. **异常处理**：`exception` 方法中，`Callable` 的 `call` 方法抛出一个 `RuntimeException`；Spring MVC 会捕获这个异常，并通过其异常处理机制处理。例如，可以通过 `@ExceptionHandler` 方法或全局异常处理器（如 `@ControllerAdvice`）来处理这个异常，并返回适当的响应。

请注意上面的两种 case，一个正常返回，一个业务执行过程中，抛出来异常

分别请求，输出如下

```
# http://localhost:8080/call/get
do some thing
执行完毕，返回!!!
```

异常请求: `http://localhost:8080/call/exception`
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406231553762.png)
```java
do some thing  
出现异常，返回!!!  
2020-03-29 16:12:06.014 ERROR 24084 --- [nio-8080-exec-5] o.a.c.c.C.[.[.[/].[dispatcherServlet] : Servlet.service() for servlet [dispatcherServlet] threw exception  
  
java.lang.RuntimeException: some error!  
	at com.git.hui.boot.async.rest.CallableRest$2.call(CallableRest.java:40) ~[classes/:na]  
	at com.git.hui.boot.async.rest.CallableRest$2.call(CallableRest.java:34) ~[classes/:na]  
	at org.springframework.web.context.request.async.WebAsyncManager.lambda$startCallableProcessing$4(WebAsyncManager.java:328) ~[spring-web-5.2.1.RELEASE.jar:5.2.1.RELEASE]  
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511) ~[na:1.8.0_171]  
	at java.util.concurrent.FutureTask.run$$$capture(FutureTask.java:266) ~[na:1.8.0_171]  
	at java.util.concurrent.FutureTask.run(FutureTask.java) ~[na:1.8.0_171]  
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149) ~[na:1.8.0_171]  
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624) ~[na:1.8.0_171]  
	at java.lang.Thread.run(Thread.java:748) [na:1.8.0_171]
```


## 3. WebAsyncTask

callable 的方式，非常直观简单，但是我们经常关注的超时+异常的处理却不太好，这个时候我们可以用`WebAsyncTask`，实现姿势也很简单，包装一下`callable`，然后设置各种回调事件即可
```java
@RestController  
@RequestMapping(path = "task")  
public class WebAysncTaskRest {  
  
    @GetMapping(path = "get")  
    public WebAsyncTask<String> get(long sleep, boolean error) {  
        Callable<String> callable = () -> {  
            System.out.println("do some thing");  
            Thread.sleep(sleep);  
  
            if (error) {  
                System.out.println("出现异常，返回!!!");  
                throw new RuntimeException("异常了!!!");  
            }  
  
            return "hello world";  
        };  
  
        WebAsyncTask<String> webTask = new WebAsyncTask<>(3000, callable);  
        webTask.onCompletion(() -> System.out.println("over!!!"));  
  
        webTask.onTimeout(() -> {  
            System.out.println("超时了");  
            return "超时返回!!!";  
        });  
  
        webTask.onError(() -> {  
            System.out.println("出现异常了!!!");  
            return "异常返回";  
        });  
  
        return webTask;  
    }  
}
```

## 4. DeferredResult

`DeferredResult`与`WebAsyncTask`最大的区别就是前者不确定什么时候会返回结果；`DeferredResult` 提供了一种在控制器方法返回后延迟处理请求结果的方式，适用于长时间的异步任务。

> `DeferredResult`的这个特点，可以用来做实现很多有意思的东西，如后面将介绍的`SseEmitter`就用到了它

下面给出一个实例
```java
@RestController  
@RequestMapping(path = "defer")  
public class DeferredResultRest {  
  
    private Map<String, DeferredResult> cache = new ConcurrentHashMap<>();  
  
    @GetMapping(path = "get")  
    public DeferredResult<String> get(String id) {  
        DeferredResult<String> res = new DeferredResult<>(3000L);  
        cache.put(id, res);  
  
        res.onCompletion(new Runnable() {  
            @Override  
            public void run() {  
                System.out.println("over!");  
            }  
        });  
        return res;  
    }  
  
    @GetMapping(path = "pub")  
    public String publish(String id, String content) {  
        DeferredResult<String> res = cache.get(id);  
        if (res == null) {  
            return "no consumer!";  
        }  
  
        res.setResult(content);  
        return "over!";  
    }  
}
```

在上面的实例中，用户如果先访问`http://localhost:8080/defer/get?id=yihuihui`，不会立马有结果，直到用户再次访问`http://localhost:8080/defer/pub?id=yihuihui&content=哈哈`时，前面的请求才会有结果返回
![00.gif](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406231557111.gif)

那么这个可以设置超时么，如果一直把前端挂住，貌似也不太合适吧
- 在构造方法中指定超时时间: `new DeferredResult<>(3000L)`
- 设置全局的默认超时时间
```java
@Configuration  
@EnableWebMvc  
public class WebConf implements WebMvcConfigurer {  
  
    @Override  
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {  
        // 超时时间设置为60s  
        configurer.setDefaultTimeout(TimeUnit.SECONDS.toMillis(10));  
    }  
}
```

## 5、`@Async` 注解（ChatGPT）

`@Async` 注解用于标记异步执行的方法。Spring 会在后台线程池中执行这些方法，而不会阻塞调用者的线程。

**配置步骤**
1. **启用异步支持**：在配置类中添加 `@EnableAsync` 注解。
2. **标记异步方法**：在方法上添加 `@Async` 注解。

**示例代码**
**配置类**
```java
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
}
```

**异步服务类**
```java
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {

    @Async
    public void asyncMethod() {
        System.out.println("Start async method");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("End async method");
    }
}
```

**调用服务类**
```java
import org.springframework.stereotype.Service;

@Service
public class CallerService {

    private final AsyncService asyncService;

    public CallerService(AsyncService asyncService) {
        this.asyncService = asyncService;
    }

    public void callAsyncMethod() {
        asyncService.asyncMethod();
        System.out.println("Async method called");
    }
}
```

## 6、接口`CompletableFuture`（ChatGPT）

**介绍**
`CompletableFuture` 是 Java 8 引入的一个类，提供了丰富的 API 用于异步编程。Spring 也支持返回 `CompletableFuture` 以实现异步处理。

**示例代码**
**配置类**
```java
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
}
```

**异步服务类**
```java
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class CompletableFutureService {

    @Async
    public CompletableFuture<String> asyncMethod() {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Start CompletableFuture async method");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "CompletableFuture result";
        });
    }
}
```

**控制器**
```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/async")
public class CompletableFutureController {

    private final CompletableFutureService completableFutureService;

    public CompletableFutureController(CompletableFutureService completableFutureService) {
        this.completableFutureService = completableFutureService;
    }

    @GetMapping("/completableFuture")
    public CompletableFuture<String> asyncCompletableFuture() {
        return completableFutureService.asyncMethod();
    }
}
```


# Ⅲ、其他
## 1、项目
 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/219-web-asyn](https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/219-web-asyn)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)



转载自：[【WEB 系列】异步请求知识点与使用姿势小结](https://spring.hhui.top/spring-blog/2020/03/29/200329-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8B%E4%B9%8B%E5%BC%82%E6%AD%A5%E8%AF%B7%E6%B1%82%E6%9C%80%E5%85%A8%E7%9F%A5%E8%AF%86%E7%82%B9%E4%B8%8E%E4%BD%BF%E7%94%A8%E5%A7%BF%E5%8A%BF/)



参考：
1. [4、优化web请求三-异步调用【WebAsyncTask】](2、相关技术/16、常用框架-SpringBoot/补14_Web篇/4、优化web请求三-异步调用【WebAsyncTask】.md)
2. [https://blog.csdn.net/f641385712/article/details/88692534](https://blog.csdn.net/f641385712/article/details/88692534)
3. [https://github.com/liuyueyi/spring-boot-demo: _https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo: _https://github.com/liuyueyi/spring-boot-demo)
4. [https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/219-web-asyn: https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/219-web-asyn](https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/219-web-asyn: https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/219-web-asyn)
5. [https://blog.hhui.top: _https://blog.hhui.top](https://blog.hhui.top: _https://blog.hhui.top)
6. [http://spring.hhui.top: http://spring.hhui.top](http://spring.hhui.top: http://spring.hhui.top)





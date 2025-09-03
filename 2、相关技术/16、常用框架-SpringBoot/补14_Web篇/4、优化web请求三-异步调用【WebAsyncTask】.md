# 一、什么是同步调用

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406252236784.png)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406252236542.png)


浏览器发起请求，Web服务器开一个线程处理，处理完把处理结果返回浏览器。好像没什么好说的了，绝大多数Web服务器都如此般处理。现在想想如果处理的过程中需要调用后端的一个业务逻辑服务器

请求处理线程会在Call了之后等待Return，自身处于阻塞状态。这也是绝大多数Web服务器的做法。一般此种做法主要适用于，后端处理响应比较快，并且并发数比较低的情况。

主要弊端，在高并发请求下，请求处理线程的短缺！因为请求处理线程的总数是有限的，如果类似的请求多了，所有的处理线程处于阻塞的状态，那新的请求也就无法处理了，也就所谓影响了服务器的吞吐能力。要更加好地发挥服务器的全部性能，就要使用异步。

**同步过程**：
1. 请求发起者发起一个request，然后会一直等待一个response，这期间它是阻塞的
2. 请求处理线程会在Call了之后等待Return，`自身处于阻塞状态`
3. 然后都等待return，知道处理线程全部完事后返回了，然后把response反给调用者就算全部结束了

Tomcat等应用服务器的连接线程池实际上是有限制的；每一个连接请求都会耗掉线程池的一个连接数；如果某些耗时很长的操作，如对大量数据的查询操作、调用外部系统提供的服务以及一些IO密集型操作等，会占用连接很长时间，这个时候这个连接就无法被释放而被其它请求重用。如果连接占用过多，服务器就很可能无法及时响应每个请求；极端情况下如果将线程池中的所有连接耗尽，服务器将长时间无法向外提供服务！

注意：比如tomcat，它既是一个web服务器，同时它也是个servlet后端容器（调ava后端服务），所以要区分清楚这两个概念。请求处理线程是有限的，tomcat需配置线程池。

# 二、什么是异步

　　![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406252239476.png)

最大的不同在于请求处理线程对后台处理的调用使用了“invoke”的方式，就是说调了之后直接返回，而不等待，这样请求处理线程就“自由”了，它可以接着去处理别的请求，当后端处理完成后，会钩起一个回调处理线程来处理调用的结果，这个回调处理线程跟请求处理线程也许都是线程池中的某个线程，相互间可以完全没有关系，由这个回调处理线程向浏览器返回内容。这就是异步的过程。

带来的改进是显而易见的，请求处理线程不需要阻塞了，它的能力得到了更充分的使用，带来了服务器吞吐能力的提升。

# 三、使用Spring MVC 和Servlet3异步线程

## 3.1、前提

要使用Spring MVC的异步功能，你得先确保你用的是Servlet 3.0或以上的版本，Maven中如此配置：
```java
<dependency>
      <groupId>javax.servlet</groupId>
      <artifactId>javax.servlet-api</artifactId>
      <version>3.1.0</version>
      <scope>provided</scope>
</dependency>
```

Spring MVC 3.2以后版本开始引入了基于Servlet3的异步请求处理

## 3.2、概述原理

相比以前，控制器方法已经不一定需要一个值，而是可以直接返回一个Callable对象，并通过Spring MVC所管理的线程来产生返回值，与此同时，Servlet容器的主线程则可以退出并释放其资源，同时也允许容器去处理其它请求。通过一个TaskExecutor，Spring MVC可以在另外的线程中调用Callable。当Callable返回时，请求在携带Callable返回的值，再次被分配到Servlet容器中恢复处理流程。

[官方文档](https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-async)中说DeferredResult和Callable都是为了异步生成返回值提供基本的支持。简单来说就是一个请求进来，如果你使用了DeferredResult或者Callable，在没有得到返回数据之前，DispatcherServlet和所有Filter就会退出Servlet容器线程，但响应保持打开状态，一旦返回数据有了，这个DispatcherServlet就会被再次调用并且处理，以异步产生的方式，向请求端返回值。 

这么做的好处就是请求不会长时间占用服务连接池，提高服务器的吞吐量。

**异步模式处理步骤概述如下**：

　　当Controller返回值是Callable的时候

　　Spring就会将Callable交给TaskExecutor去处理（一个隔离的线程池）

　　与此同时将DispatcherServlet里的拦截器、Filter等等都马上退出主线程，但是response仍然保持打开的状态

　　Callable线程处理完成后，Spring MVC讲请求重新派发给容器 **（注意这里的重新派发，和拦截器密切相关）**

　　根据Callabel返回结果，继续处理（比如参数绑定、视图解析等等就和之前一样了）

其中，如果我们需要超时处理的回调或者错误处理的回调，我们可以使用`WebAsyncTask`代替Callable。实际使用中，我并不建议直接使用Callable ，而是使用Spring提供的`WebAsyncTask` 代替，它包装了Callable，功能更强大些

### 1、Servlet 3.0异步请求运作机制的部分原理

1. Servlet请求ServletRequest可以通过调用request.startAsync()方法而进入异步模式，这样做的主要结果就是该Servlet以及所有的过滤器都可以结束但其相应(Response)会留待异步处理结束后在返回调用。
2. request.startAsync()方法会返回一个AsyncContext对象，可以用他对异步处理进行进一步的控制和操作，比如说他也提供了一个与反转(forward)很相似的[dispatch](https://www.baidu.com/s?wd=dispatch&tn=24004469_oem_dg&rsv_dl=gh_pl_sl_csd)方法，只不过他允许应用恢复Servlet容器的请求处理进程。
3. ServletRequest提供了获取当前DispatherType的方式，后者可以用来区别当前处理的是原始请求，异步分发请求，转向或者是其它类型的请求分发类型。

### 2、Callable的异步请求被处理时所发生的事件

官方介绍

> Controller returns a Callable.
> Spring MVC calls request.startAsync() and submits the Callable to a TaskExecutor for processing in a separate thread.
> Meanwhile the DispatcherServlet and all Filter’s exit the Servlet container thread but the response remains open.
> Eventually the Callable produces a result and Spring MVC dispatches the request back to the Servlet container to complete processing.
> The DispatcherServlet is invoked again and processing resumes with the asynchronously produced return value from the Callable.

1. Controller返回Callable
2. Spring MVC调用request.startAsync（）并将Callable提交给TaskExecutor，以便在单独的线程中进行处理。
3. 同时DispatcherServlet和所有Filter都退出Servlet容器线程，但响应仍保持打开状态。
4. 最终，Callable生成一个结果，Spring MVC将请求调度回Servlet容器以完成处理。
5. 再次调用DispatcherServlet，并使用来自Callable的异步生成的返回值继续处理。

流程上大体与`DeferredResult`类似，只不过`Callable`是由`TaskExecutor`来处理的，而`TaskExecutor`继承自`java.util.concurrent.Executor`。我们来看一下它的源代码，它也是在`WebAsyncManager`中处理的：
```java
/**
     * Use the given {@link WebAsyncTask} to configure the task executor as well as
     * the timeout value of the {@code AsyncWebRequest} before delegating to
     * {@link #startCallableProcessing(Callable, Object...)}.
     * @param webAsyncTask a WebAsyncTask containing the target {@code Callable}
     * @param processingContext additional context to save that can be accessed
     * via {@link #getConcurrentResultContext()}
     * @throws Exception if concurrent processing failed to start
     */
    public void startCallableProcessing(final WebAsyncTask<?> webAsyncTask, Object... processingContext) throws Exception {
        Assert.notNull(webAsyncTask, "WebAsyncTask must not be null");
        Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");

        Long timeout = webAsyncTask.getTimeout();
        if (timeout != null) {
            this.asyncWebRequest.setTimeout(timeout);
        }

        AsyncTaskExecutor executor = webAsyncTask.getExecutor();
        if (executor != null) {
            this.taskExecutor = executor;
        }

        List<CallableProcessingInterceptor> interceptors = new ArrayList<CallableProcessingInterceptor>();
        interceptors.add(webAsyncTask.getInterceptor());
        interceptors.addAll(this.callableInterceptors.values());
        interceptors.add(timeoutCallableInterceptor);

        final Callable<?> callable = webAsyncTask.getCallable();
        final CallableInterceptorChain interceptorChain = new CallableInterceptorChain(interceptors);

        this.asyncWebRequest.addTimeoutHandler(new Runnable() {
            @Override
            public void run() {
                logger.debug("Processing timeout");
                Object result = interceptorChain.triggerAfterTimeout(asyncWebRequest, callable);
                if (result != CallableProcessingInterceptor.RESULT_NONE) {
                    setConcurrentResultAndDispatch(result);
                }
            }
        });

        this.asyncWebRequest.addCompletionHandler(new Runnable() {
            @Override
            public void run() {
                interceptorChain.triggerAfterCompletion(asyncWebRequest, callable);
            }
        });

        interceptorChain.applyBeforeConcurrentHandling(this.asyncWebRequest, callable);
        startAsyncProcessing(processingContext);
        //启动线程池的异步处理
        try {
            this.taskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    Object result = null;
                    try {
                        interceptorChain.applyPreProcess(asyncWebRequest, callable);
                        result = callable.call();
                    }
                    catch (Throwable ex) {
                        result = ex;
                    }
                    finally {
                        result = interceptorChain.applyPostProcess(asyncWebRequest, callable, result);
                    }
                    //设置当前的结果并转发
                    setConcurrentResultAndDispatch(result);
                }
            });
        }
        catch (RejectedExecutionException ex) {
            Object result = interceptorChain.applyPostProcess(this.asyncWebRequest, callable, ex);
            setConcurrentResultAndDispatch(result);
            throw ex;
        }
    }
```

对比`DeferredResult`，在这里刚开始也是添加拦截器，只不过拦截器的名称是`CallableProcessingInterceptor` ，同时也需要设置WebAsyncRequest的超时处理，完成时处理的响应操作。这其中最大的区别就是使用`TaskExecutor`来对`Callable`进行异步处理

### 3、DeferredResult对象请求

处理顺序也非常类似，区别在于应用可以通过任何线程来计算返回一个结果

官网描述
```java
DeferredResult processing:

Controller returns a DeferredResult and saves it in some in-memory queue or list where it can be accessed.
Spring MVC calls request.startAsync().
Meanwhile the DispatcherServlet and all configured Filter’s exit the request processing thread but the response remains open.
The application sets the DeferredResult from some thread and Spring MVC dispatches the request back to the Servlet container.
The DispatcherServlet is invoked again and processing resumes with the asynchronously produced return value.
```

1. 将Controller返回的`DeferredResult`值保存到内存队列或集合当中以便存取
2. SpringMVC调用`HttpServletRequest`的`startAsync()`方法，异步处理
3. 同时，DispatcherServlet和所有已配置的Filter都退出请求处理线程，但响应仍保持打开状态，此时方法的响应对象仍未返回。
4. 应用程序从某个线程设置DeferredResult，Spring MVC将请求调度回Servlet容器，恢复处理
5. 再次调用DispatcherServlet，并使用异步生成的返回值继续处理

源码分析：

当一个请求被`DispatcherServlet`处理时，会试着获取一个`WebAsyncManager`对象
```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpServletRequest processedRequest = request;
        HandlerExecutionChain mappedHandler = null;
        boolean multipartRequestParsed = false;

          // 获取WebAsyncManager
        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
        try {
          // ......省略部分代码
          // 执行子控制器的方法
          mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
        //如果当前的请求需要异步处理，则终止当前请求，但是响应是打开的
          if (asyncManager.isConcurrentHandlingStarted()) {
              return;
          }
        //....省略部分代码
       }
        //....省略部分代码
}
```

对于每一个子控制器的方法返回值，都是HandlerMethodReturnValueHandler接口处理的，其中有一个实现类是DeferredResultMethodReturnValueHandler，关键代码如下：
```java
package org.springframework.web.servlet.mvc.method.annotation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import org.springframework.core.MethodParameter;
import org.springframework.lang.UsesJava8;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Handler for return values of type {@link DeferredResult}, {@link ListenableFuture},
 * {@link CompletionStage} and any other async type with a {@link #getAdapterMap()
 * registered adapter}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
@SuppressWarnings("deprecation")
public class DeferredResultMethodReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

    //存放DeferredResult的适配集合
    private final Map<Class<?>, DeferredResultAdapter> adapterMap;


    public DeferredResultMethodReturnValueHandler() {
        this.adapterMap = new HashMap<Class<?>, DeferredResultAdapter>(5);
        this.adapterMap.put(DeferredResult.class, new SimpleDeferredResultAdapter());
        this.adapterMap.put(ListenableFuture.class, new ListenableFutureAdapter());
        if (ClassUtils.isPresent("java.util.concurrent.CompletionStage", getClass().getClassLoader())) {
            this.adapterMap.put(CompletionStage.class, new CompletionStageAdapter());
        }
    }


    /**
     * Return the map with {@code DeferredResult} adapters.
     * <p>By default the map contains adapters for {@code DeferredResult}, which
     * simply downcasts, {@link ListenableFuture}, and {@link CompletionStage}.
     * @return the map of adapters
     * @deprecated in 4.3.8, see comments on {@link DeferredResultAdapter}
     */
    @Deprecated
    public Map<Class<?>, DeferredResultAdapter> getAdapterMap() {
        return this.adapterMap;
    }

    private DeferredResultAdapter getAdapterFor(Class<?> type) {
        for (Class<?> adapteeType : getAdapterMap().keySet()) {
            if (adapteeType.isAssignableFrom(type)) {
                return getAdapterMap().get(adapteeType);
            }
        }
        return null;
    }


    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return (getAdapterFor(returnType.getParameterType()) != null);
    }

    @Override
    public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
        return (returnValue != null && (getAdapterFor(returnValue.getClass()) != null));
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType,
            ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

        if (returnValue == null) {
            mavContainer.setRequestHandled(true);
            return;
        }
       //根据返回值的类型获取对应的DeferredResult适配器
        DeferredResultAdapter adapter = getAdapterFor(returnValue.getClass());
        if (adapter == null) {
            throw new IllegalStateException(
                    "Could not find DeferredResultAdapter for return value type: " + returnValue.getClass());
        }
        DeferredResult<?> result = adapter.adaptToDeferredResult(returnValue);
        //开启异步请求
        WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(result, mavContainer);
    }

}
```

在这里我们重点关注`handleReturnValue`的方法，在经过适配包装后获取`DeferredResult`开启了异步之旅

紧接着查看handleReturnValue方法中调用的WebAsyncManager的startDeferredResultProcessing方法
```java
public void startDeferredResultProcessing(
            final DeferredResult<?> deferredResult, Object... processingContext) throws Exception {

        Assert.notNull(deferredResult, "DeferredResult must not be null");
        Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");
        //设置超时时间
        Long timeout = deferredResult.getTimeoutValue();
        if (timeout != null) {
            this.asyncWebRequest.setTimeout(timeout);
        }

        //获取所有的延迟结果拦截器
        List<DeferredResultProcessingInterceptor> interceptors = new ArrayList<DeferredResultProcessingInterceptor>();
        interceptors.add(deferredResult.getInterceptor());
        interceptors.addAll(this.deferredResultInterceptors.values());
        interceptors.add(timeoutDeferredResultInterceptor);

        final DeferredResultInterceptorChain interceptorChain = new DeferredResultInterceptorChain(interceptors);
       
        this.asyncWebRequest.addTimeoutHandler(new Runnable() {
            @Override
            public void run() {
                try {
                    interceptorChain.triggerAfterTimeout(asyncWebRequest, deferredResult);
                }
                catch (Throwable ex) {
                    setConcurrentResultAndDispatch(ex);
                }
            }
        });

        this.asyncWebRequest.addCompletionHandler(new Runnable() {
            @Override
            public void run() {
                interceptorChain.triggerAfterCompletion(asyncWebRequest, deferredResult);
            }
        });

        interceptorChain.applyBeforeConcurrentHandling(this.asyncWebRequest, deferredResult);
         //开始异步处理
        startAsyncProcessing(processingContext);

        try {
            interceptorChain.applyPreProcess(this.asyncWebRequest, deferredResult);
            deferredResult.setResultHandler(new DeferredResultHandler() {
                @Override
                public void handleResult(Object result) {
                    result = interceptorChain.applyPostProcess(asyncWebRequest, deferredResult, result);
                    //设置结果并转发
                    setConcurrentResultAndDispatch(result);
                }
            });
        }
        catch (Throwable ex) {
            setConcurrentResultAndDispatch(ex);
        }
    }

    private void startAsyncProcessing(Object[] processingContext) {
        clearConcurrentResult();
        this.concurrentResultContext = processingContext;
        //实际上是执行的是HttpServletRequest对应方法
        this.asyncWebRequest.startAsync();

        if (logger.isDebugEnabled()) {
            HttpServletRequest request = this.asyncWebRequest.getNativeRequest(HttpServletRequest.class);
            String requestUri = urlPathHelper.getRequestUri(request);
            logger.debug("Concurrent handling starting for " + request.getMethod() + " [" + requestUri + "]");
        }
    }
```

在这里首先收集所有配置好的`DeferredResultProcessingInterceptor` ,然后设置asyncRequest的超时处理，完成时的处理等，同时会分阶段执行拦截器中的各个方法。最后我们关注一下如下代码：
```java
deferredResult.setResultHandler(result -> {
                result = interceptorChain.applyPostProcess(this.asyncWebRequest, deferredResult, result);
                //设置结果并转发
                setConcurrentResultAndDispatch(result);
            });
```

查看setConcurrentResultAndDispatch内实现：其最终还是要调用AsyncWebRequest接口中的dispatch方法进行转发，让DispatcherServlet重新处理异步结果：this.asyncWebRequest.dispatch();

其实在这里都是封装自`HttpServletRequest`的异步操作,我们可以看一下`StandardServletAsyncWebRequest`的类结构图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406252305969.png)


可以在其父类`ServletRequestAttributes`里找到对应的实现：
```java
private final HttpServletRequest request;
/**
 * Exposes the native {@link HttpServletRequest} that we're wrapping.
 */
public final HttpServletRequest getRequest() {
	return this.request;
}
```

`StandardServletAsyncWebRequest`代码，方便理解整个异步是怎么执行：
```java
//java.servlet.AsnycContext
    private AsyncContext asyncContext;
  
    @Override
    public void startAsync() {
        Assert.state(getRequest().isAsyncSupported(),
                "Async support must be enabled on a servlet and for all filters involved " +
                "in async request processing. This is done in Java code using the Servlet API " +
                "or by adding \"<async-supported>true</async-supported>\" to servlet and " +
                "filter declarations in web.xml.");
        Assert.state(!isAsyncComplete(), "Async processing has already completed");

        if (isAsyncStarted()) {
            return;
        }
        this.asyncContext = getRequest().startAsync(getRequest(), getResponse());
        this.asyncContext.addListener(this);
        if (this.timeout != null) {
            this.asyncContext.setTimeout(this.timeout);
        }
    }

    @Override
    public void dispatch() {
        Assert.notNull(this.asyncContext, "Cannot dispatch without an AsyncContext");
        this.asyncContext.dispatch();
    }
```

### 4、异步结果的异常处理

如果Callable在执行过程中抛出异常 与一般的控制器异常一样，会被正常的异常处理流程捕获处理

如果返回方法是一个DeferredResult对象，可以选择

deferredResult.setErrorResult()

### 5、拦截异步请求

处理连接器HandlerInterceptor可以实现AsyncHandlerInterceptor接口拦截异步请求，因为在异步请求的开始时，被调用的回调方法是该接口的afterConcurrentHandlingStarted方法，而不是一般的postHandle 和 afterCompletion方法。如果需要与异步请求处理的生命流程有更深入的集成，比如需要处理timeout的事件等，则HandlerInterceptor需要注册CallableProcessingInterceptor或DeferredResultProcessingInterceptor拦截器，更多细节需要参考AsyncHandlerInterceptor类的Java文档。

DeferredResult类还提供了onTimeout(Runnable)和onCompletion(Runnable)等方法可以参考DeferredResult的java文档

Callable需要请求过期(timeout)和完成后的拦截时，可以把他包装在一个WebAsyncTask实例中，后者提供了相关技术支持。

## 3.3、异步拦截器

　　1）、原生API的AsyncListener  
　　2）、SpringMVC：实现AsyncHandlerInterceptor；

# 四、使用

## 4.1、Callable使用
```java
@GetMapping("/callable")
public Callable<String> testCallable() throws Exception {
	log.info("主线程开始！");

	Callable<String> result = new Callable<String>() {
		@Override
		public String call() throws Exception {
			log.info("副线程开始1！");
			Thread.sleep(3000);
			log.info("副线程结束1！");
			return "SUCCESS1" + new Date();
		}
	};
	log.info("主线程结束！");
	return result;
}
```

请求地址查看
```java
2019-02-27 14:25:00.197  INFO 13815 --- [nio-8080-exec-3] c.g.b.g.d.w.WebCallableAsyncController   : 主线程开始！
2019-02-27 14:25:00.197  INFO 13815 --- [nio-8080-exec-3] c.g.b.g.d.w.WebCallableAsyncController   : 主线程结束！
2019-02-27 14:25:00.197  INFO 13815 --- [      MvcAsync2] c.g.b.g.d.w.WebCallableAsyncController   : 副线程开始1！
2019-02-27 14:25:03.200  INFO 13815 --- [      MvcAsync2] c.g.b.g.d.w.WebCallableAsyncController   : 副线程结束1！
```

返回Callable意味着Spring MVC将调用在不同的线程中执行定义的任务。Spring将使用TaskExecutor来管理线程。在等待完成的长期任务之前，servlet线程将被释放。

在长时间运行的任务执行完毕之前就已经从servlet返回了。这并不意味着客户端收到了一个响应。与客户端的通信仍然是开放的等待结果，但接收到的请求的线程已被释放，并可以服务于另一个客户的请求。

两个概念：
1. **请求处理线程**：处理线程 属于 web 服务器线程，负责 处理用户请求，采用 线程池 管理。
2. **异步线程**：异步线程 属于 用户自定义的线程，可采用 线程池管理。

前端页面等待3秒出现结果。

注意：异步模式对前端来说，是无感知的，这是后端的一种技术。所以这个和我们自己开启一个线程处理，立马返回给前端是有非常大的不同的，需要注意。

由此可以看出，主线程早早就结束了（需要注意，此时还并没有把response返回的，此处一定要注意），真正干事的是子线程（交给TaskExecutor去处理的）

**注意：很大程度上提高了我们`请求处理线程`的利用率，从而肯定就提高了我们系统的吞吐量。**

## 4.2、WebAsyncTask升级版callable，增加超时异常等处理

1. 常规调用

查看WebAsyncTask，有说明：Holder for a {@link Callable}, a timeout value, and a task executor.其实是一个Callable。

示例
```java
@RequestMapping(value="/longtimetask", method = RequestMethod.GET)
public WebAsyncTask longTimeTask(){
	System.out.println("/longtimetask被调用 thread id is : " + Thread.currentThread().getId());
	Callable<String> callable = new Callable<String>() {
		public String call() throws Exception {
			Thread.sleep(3000); //假设是一些长时间任务
			System.out.println("执行成功 thread id is : " + Thread.currentThread().getId());
			return "ok";
		}
	};
	return new WebAsyncTask(callable);
}
```

事实上，直接返回 `Callable<String>` 都是可以的，但这里包装了一层，以便做后面提到的“超时处理”。和前一个方案的差别在于这个Callable的call方法并不是我们直接调用的，而是在longTimeTask返回后，由Spring MVC用一个工作线程来调用，执行，打印出来的结果：

```java
/longtimetask被调用 thread id is : 47  
执行成功 thread id is : 48
```

2. 超时处理

如果“长时间处理任务”一直没返回，那也不应该让客户端无限等下去，需要服务端终结，即“超时”处理。如图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406252313054.png)

“超时处理线程”和“回调处理线程”可能都是线程池中的某个线程，我为了清晰点把它们分开画而已。
```java
@RequestMapping(value="/longtimetaskTimeout", method = RequestMethod.GET)
public WebAsyncTask longtimetaskTimeout(){
	System.out.println("/longtimetask被调用 thread id is : " + Thread.currentThread().getId());
	Callable<String> callable = new Callable<String>() {
		public String call() throws Exception {
			Thread.sleep(3000); //假设是一些长时间任务
			System.out.println("执行成功 thread id is : " + Thread.currentThread().getId());
			return "ok";
		}
	};
	WebAsyncTask webAsyncTask = new WebAsyncTask(2000,callable);
	webAsyncTask.onTimeout(()->{
		System.out.println("执行超时 thread id is ：" + Thread.currentThread().getId());
		return "执行超时";
	});
	return webAsyncTask;
}
```

这就是前面提到的为什么Callable还要外包一层的缘故，给WebAsyncTask设置一个超时回调，即可实现超时处理，在这个例子中，超时设置为3秒
```java
情况一、处理5秒：http://localhost:8080/async/longtimetaskTimeout?tr=5000
返回：程序[超时]的回调
服务器：
/longtimetask被调用 thread id is : 55
程序[正常执行]完成的回调 
```

```java
情况二、处理2秒：http://localhost:8080/async/longtimetaskTimeout?tr=2000
返回：OK
服务器：
/longtimetask被调用 thread id is : 46
执行成功 thread id is : 57
程序[正常执行]完成的回调
```

情况二、处理2秒：
```java
情况二、处理2秒：http://localhost:8080/async/longtimetaskTimeout?tr=1000
返回：抛出异常终结
```

自定义线程池，new WebAsyncTask可以自定义线程池
```java
@Configuration
public class TaskConfiguration {
    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(5);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.setQueueCapacity(10);
        taskExecutor.setThreadNamePrefix("asyncTask");
        return taskExecutor;
    }
}
```


## 4.3、DeferredResult，延迟结果

DeferredResult使用方式与Callable类似，但在返回结果上不一样，它返回的时候实际结果可能没有生成，实际的结果可能会在另外的线程里面设置到DeferredResult中去。

这个特性非常非常的重要，对后面实现复杂的功能（比如服务端推技术、订单过期时间处理、长轮询、模拟MQ的功能等等高级应用）

一旦在Servlet容器中启用了异步请求处理功能，控制器方法就可以使用DeferredResult包装任何支持的控制器方法返回值，

 DeferredResult这个类代表延迟结果，我们先看一看spring的API文档给我们的解释：
```java
{@code DeferredResult} provides an alternative to using a {@link Callable} for asynchronous request processing.   
While a {@code Callable} is executed concurrently on behalf of the application,  
 with a {@code DeferredResult} the application can produce the result from a thread of its choice.
```

根据文档说明`DeferredResult`可以替代`Callable`来进行异步的请求处理。只不过这个类可以从其他线程里拿到对应的结果。当使用`DeferredResult`，我们可以将DefferedResult的类型并将其保存到可以获取到该对象的地方，比如说队列或者集合当中，这样方便其它线程能够取到并设置`DefferedResult`的值。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406252316094.png)

实例
```java
//接收队列
private BlockingQueue<DeferredResult<String>> blockingQueue = new ArrayBlockingQueue(1024);
//接收队列 或者ConcurrentLinkedQueue
//    private static Queue<DeferredResult<String>> queue = new ConcurrentLinkedQueue<DeferredResult<String>>();

/**
 * 返回值是DeferredResult类型，如果没有结果请求阻塞
 *
 * @return
 */
@GetMapping("/quotes")
public DeferredResult<String> quotes() {
	//指定超时时间，及出错时返回的值
	DeferredResult<String> result = new DeferredResult(3000L, "error");
	//先存储起来，等待触发
	blockingQueue.add(result);
//        queue.add(result);
	return result;
}

/**
 * 另外一个请求(新的线程)设置值
 *
 * @throws InterruptedException
 */

@GetMapping("take")
public void take() throws InterruptedException {
	DeferredResult<String> result = blockingQueue.take();
	result.setResult("route");
//        DeferredResult<String> poll = queue.poll();
//        poll.setResult("OK");
}
```

控制器可以从不同的线程异步生成返回值，例如响应外部事件(JMS消息)、计划任务等，那么在这里我先使用另外一个请求来模拟这个过程  

此时我们启动tomcat,先访问地址[http://localhost:8080/quotes](http://localhost:8080/quotes) ,此时我们会看到发送的请求由于等待响应遭到了阻塞：

当在规定时间内访问[http://localhost:8080/take](http://localhost:8080/take) 时，则能成功显示结果：

如果有另一个线程给DeferredResult赋值后，DeferredResult在感知到自己的对象被赋值后就返回页面成功；

一个独立的示例
```java
/**
 * 一个独立的示例
 * @return
 */
@RequestMapping(value = "/deferred", method = RequestMethod.GET)
public DeferredResult<String> executeSlowTask() {
	log.info("Request received");
	DeferredResult<String> deferredResult = new DeferredResult<>();
	CompletableFuture.supplyAsync(()->{
		try {
			Thread.sleep(5000);
			log.info("Slow task executed");
			return "Task finished";
		} catch (InterruptedException e) {
			e.printStackTrace();
			return "Task exception";
		}
	}).whenCompleteAsync((result, throwable) -> deferredResult.setResult(result));
	log.info("Servlet thread released");

	return deferredResult;
}
```

返回DeferredResult和返回Callable有什么区别？不同的是返回DeferredResult的线程是由我们管理。创建一个线程并将结果set到DeferredResult是由我们自己来做的。

用completablefuture创建一个异步任务。这将创建一个新的线程，在那里我们的长时间运行的任务将被执行。也就是在这个线程中，我们将set结果到DeferredResult并返回。

是在哪个线程池中我们取回这个新的线程？默认情况下，在completablefuture的supplyasync方法将在forkjoin池运行任务。如果你想使用一个不同的线程池，你可以通过传一个executor到supplyasync方法：

```java
public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor)
```

注意：Callable和Deferredresult做的是同样的事情——释放容器线程，在另一个线程上异步运行长时间的任务。不同的是谁管理执行任务的线程。

## 4.4、@Async

参看：[https://www.cnblogs.com/bjlhx/p/10364385.html](https://www.cnblogs.com/bjlhx/p/10364385.html) 

## 4.5、异步模式中使用Filter和HandlerInterceptor

以默认访问async/callable为例，现在是直接反问，增加Filter以及Interceptor后

### 4.5.1、Filter

```java
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class HelloRequsetFilter extends OncePerRequestFilter {
    @Override
    protected void initFilterBean() throws ServletException {
        System.out.println("Filter初始化……");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("Filter Chain :" + Thread.currentThread().getName() + "----->" + request.getRequestURI());
        filterChain.doFilter(request, response);
    }
}
```

输出：
```java
Filter Chain :http-nio-8080-exec-4----->/async/callable
[2022-07-02 06:42:35:581-http-nio-8080-exec-4] [INFO] - com.github.bjlhx15.boot2async.AsyncController.testCallable(AsyncController.java:26) - 主线程开始！
[2022-07-02 06:42:35:583-http-nio-8080-exec-4] [INFO] - com.github.bjlhx15.boot2async.AsyncController.testCallable(AsyncController.java:37) - 主线程结束！
[2022-07-02 06:42:35:601-MvcAsync1] [INFO] - com.github.bjlhx15.boot2async.AsyncController$1.call(AsyncController.java:31) - 副线程开始1！
[2022-07-02 06:42:38:605-MvcAsync1] [INFO] - com.github.bjlhx15.boot2async.AsyncController$1.call(AsyncController.java:33) - 副线程结束1！
```

由此可以看出，异步上下文，Filter还是只会被执行一次拦截的，符合预期

### 4.5.2、Interceptor

代码
```java
public class HelloInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("HandlerInterceptor :" + Thread.currentThread().getName() + "---preHandle-->" + request.getRequestURI());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("HandlerInterceptor :" + Thread.currentThread().getName() + "---postHandle-->" + request.getRequestURI());

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        System.out.println("HandlerInterceptor :" + Thread.currentThread().getName() + "---afterCompletion-->" + request.getRequestURI());
    }
}
```

配置类
```java
@Configuration
@EnableWebMvc
public class AppConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HelloInterceptor()).addPathPatterns("/**");
    }
}
```

输出
```java
Filter Chain :http-nio-8080-exec-4----->/async/callable
HandlerInterceptor :http-nio-8080-exec-4---preHandle-->/async/callable
[2022-07-02 06:49:59:957-http-nio-8080-exec-4] [INFO] - com.github.bjlhx15.boot2async.AsyncController.testCallable(AsyncController.java:26) - 主线程开始！
[2022-07-02 06:49:59:960-http-nio-8080-exec-4] [INFO] - com.github.bjlhx15.boot2async.AsyncController.testCallable(AsyncController.java:37) - 主线程结束！
[2022-07-02 06:49:59:980-MvcAsync1] [INFO] - com.github.bjlhx15.boot2async.AsyncController$1.call(AsyncController.java:31) - 副线程开始1！
[2022-07-02 06:50:02:986-MvcAsync1] [INFO] - com.github.bjlhx15.boot2async.AsyncController$1.call(AsyncController.java:33) - 副线程结束1！
HandlerInterceptor :http-nio-8080-exec-5---preHandle-->/async/callable
HandlerInterceptor :http-nio-8080-exec-5---postHandle-->/async/callable
HandlerInterceptor :http-nio-8080-exec-5---afterCompletion-->/async/callable
```


普通的Spring MVC的拦截器，preHandler会执行两次，在写preHandler的时候，一定要特别的注意，要让preHandler即使执行多次，也不要受到影响（幂等）

异步拦截器 `AsyncHandlerInterceptor【常用】、CallableProcessingInterceptor、DeferredResultProcessingInterceptor`

```java
public class AsyncHelloInterceptor implements AsyncHandlerInterceptor  {
    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("AsyncHandlerInterceptor :" + Thread.currentThread().getName() + "---afterConcurrentHandlingStarted-->" + request.getRequestURI());

    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("AsyncHandlerInterceptor :" + Thread.currentThread().getName() + "---preHandle-->" + request.getRequestURI());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("AsyncHandlerInterceptor :" + Thread.currentThread().getName() + "---postHandle-->" + request.getRequestURI());

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        System.out.println("AsyncHandlerInterceptor :" + Thread.currentThread().getName() + "---afterCompletion-->" + request.getRequestURI());
    }
}
```

输出：
```java
Filter Chain :http-nio-8080-exec-6----->/async/callable
AsyncHandlerInterceptor :http-nio-8080-exec-6---preHandle-->/async/callable
[2022-07-02 06:57:48:159-http-nio-8080-exec-6] [INFO] - com.github.bjlhx15.boot2async.AsyncController.testCallable(AsyncController.java:26) - 主线程开始！
[2022-07-02 06:57:48:162-http-nio-8080-exec-6] [INFO] - com.github.bjlhx15.boot2async.AsyncController.testCallable(AsyncController.java:37) - 主线程结束！
AsyncHandlerInterceptor :http-nio-8080-exec-6---afterConcurrentHandlingStarted-->/async/callable
[2022-07-02 06:57:48:178-MvcAsync1] [INFO] - com.github.bjlhx15.boot2async.AsyncController$1.call(AsyncController.java:31) - 副线程开始1！
[2022-07-02 06:57:51:181-MvcAsync1] [INFO] - com.github.bjlhx15.boot2async.AsyncController$1.call(AsyncController.java:33) - 副线程结束1！
AsyncHandlerInterceptor :http-nio-8080-exec-7---preHandle-->/async/callable
AsyncHandlerInterceptor :http-nio-8080-exec-7---postHandle-->/async/callable
AsyncHandlerInterceptor :http-nio-8080-exec-7---afterCompletion-->/async/callable
```

AsyncHandlerInterceptor提供了一个afterConcurrentHandlingStarted()方法, 这个方法会在Controller方法异步执行时开始执行, 而Interceptor的postHandle方法则是需要等到Controller的异步执行完才能执行

（比如我们用DeferredResult的话，afterConcurrentHandlingStarted是在return的之后执行，而postHandle()是执行.setResult()之后执行）

需要说明的是：如果不是异步请求，afterConcurrentHandlingStarted是不会执行的。所以可以把它当做加强版的HandlerInterceptor来用。平时若要使用拦截器，建议使用它。（Spring5，JDK8以后，很多的xxxAdapter都没啥用了，直接implements接口就成~）

只是一般来说，我们并不需要注册这种精细的拦截器，绝大多数情况下，使用`AsyncHandlerInterceptor`是够了的。（Spring MVC的很多默认设置，请参考`WebMvcConfigurationSupport`）

# 五、Callable、DeferredResult、WebAsyncTask、Async对比

|   |   |   |   |   |
|---|---|---|---|---|
||Callable|WebAsyncTask|DeferredResult|Async|
|针对问题点|异步请求处理|异步请求处理|异步请求处理|异步方法|
|目标|释放容器线程|释放容器线程|释放容器线程|服务线程内多线程执行|
|拦截器|CallableProcessingInterceptor||DeferredResultProcessingInterceptor||
|超时拦截器|TimeoutCallableProcessingInterceptor||TimeoutDeferredResultProcessingInterceptor||
||基础版本|升级Callable，需要超时处理的回调或者错误处理的回调<br><br>WebAsyncTask 的异步编程 API。相比于 @[Async](https://so.csdn.net/so/search?q=Async&spm=1001.2101.3001.7020) 注解，WebAsyncTask 提供更加健全的 超时处理 和 异常处理 支持。但是@Async也有更优秀的地方，就是他不仅仅能用于controller中~~~~（任意地方）|`DeferredResult`需要自己用线程来处理结果`setResult`，而`Callable`的话不需要我们来维护一个结果处理线程。||

常用类：
```java
NoSupportAsyncWebRequest.java  
　　不支持异步处理模式的web请求  
DeferredResultProcessingInterceptor.java  
　　DeferredResult处理过程拦截器  
　　在start async前，超时后/异步处理完成后/网络超时后触发拦截  
DeferredResultProcessingInterceptorAdapter.java  
　　抽象类实现DeferredResultProcessingInterceptor,做空实现  
DeferredResultInterceptorChain.java  
　　调用DeferredResultProcessingInterceptor的辅助类  
DeferredResult.java  
　　递延结果，在两个线程中传递的对象结果  
　　实现Comparable接口以保证加入PriorityQueue队列的正确顺序  
CallableProcessingInterceptor.java  
　　Callable拦截器  
CallableProcessingInterceptorAdapter.java  
　　抽象类实现CallableProcessingInterceptor接口，空实现  
CallableInterceptorChain.java  
　　调用CallableProcessingInterceptor的辅助类  
TimeoutCallableProcessingInterceptor.java  
　　继承CallableProcessingInterceptorAdapter  
　　实现超时处理方法  
TimeoutDeferredResultProcessingInterceptor.java  
　　继承DeferredResultProcessingInterceptorAdapter  
　　实现超时处理方法  
WebAsyncTask.java  
　　web异步任务  
　　包含一个Callable类，一个超时时间，一个任务执行着或名字  
WebAsyncUtils.java  
　　实现getAsyncManager  
　　实现createAsyncWebRequest  
WebAsyncManager.java  
　　对Callables和DeferredResults启动的管理，包括拦截器的注入，Excutor的注入等  
　　异步处理的入口类
```


转载自： [https://www.cnblogs.com/bjlhx/p/10444814.html](https://www.cnblogs.com/bjlhx/p/10444814.html)




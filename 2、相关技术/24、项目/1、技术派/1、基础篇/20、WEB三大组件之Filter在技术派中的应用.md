在技术派项目中，我们通过 filter 来实现用户身份识别，并将识别出来的用户信息，保存到 ThreadLocal 对应的上下文中，这样在后续的请求链路中，在任何地方都可以直接获取当前的登录用户了

接下来，今天看一下 Java WEB 三大组件之一的过滤器 Filter，是如何在技术派中发挥作用的

# 1、使用场景

实用类路径：`com.github.paicoding.forum.web.hook.filter.ReqRecordFilter`
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307290937047.png)

## 1.1、Filter基础知识

在进入上面的源码分析之前，有必要给大家介绍一下 Filter 相关的基本知识点

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307290937476.png)

Filter 称为过滤器，主要用来拦截 http 请求，做一些其他的事情

### 1.1.1、流程说明
一个http请求活过来后：
1. 首先进入 filter，执行相关业务逻辑
2. 若判定通行，则进入 Servlet 逻辑，Servlet 执行完毕之后，又返回 Filter，最后在返回给请求方
3. 判定失败，直接返回，不需要将请求发给 Servlet

### 1.1.2、应用场景

通过上面的流程，可以推算下具体的使用场景：
1. 在 filter 层，来获取用户的身份
2. 可以考虑在 filter 层做一些常规的校验（如参数校验，referer 校验、权限控制等）
3. 可以在 filter 层做运维、安全防护相关的工作（如全链路打点，可以在 filter 层分配一个 traceId；也可以在这一层做限流等）

## 1.2、使用说明

filter 的基本使用比较简单，实现 Fitler 接口即可，如
```java
/**  
 * 1. 请求参数日志输出过滤器  
 * 2. 判断用户是否登录  
 *  
 * @author YiHui  
 * @date 2022/7/6  
 */@Slf4j  
@WebFilter(urlPatterns = "/*", filterName = "reqRecordFilter", asyncSupported = true)  
public class ReqRecordFilter implements Filter {  
    private static Logger REQ_LOG = LoggerFactory.getLogger("req");  
  
    @Autowired  
    private GlobalInitService globalInitService;  
  
    @Autowired  
    private StatisticsSettingService statisticsSettingService;  
  
    @Override  
    public void init(FilterConfig filterConfig) {  
    }  
  
    @Override  
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {  
        long start = System.currentTimeMillis();  
        HttpServletRequest request = null;  
        try {  
            request = this.initReqInfo((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);  
            CrossUtil.buildCors(request, (HttpServletResponse) servletResponse);  
            filterChain.doFilter(request, servletResponse);  
        } finally {  
            buildRequestLog(ReqInfoContext.getReqInfo(), request, System.currentTimeMillis() - start);  
            // 一个链路请求完毕，清空MDC相关的变量(如GlobalTraceId，用户信息)  
            MdcUtil.clear();  
            ReqInfoContext.clear();  
        }  
    }  
  
    @Override  
    public void destroy() {  
    }
    
    // ……省略
```

上面有三个方法:
1. init：初始化时执行
2. destory：销毁时执行
3. doFilter：重点关注这个，filter规则命中的请求，都会走进来
	1. 三个参数，注意第三个FilterChina，这里是经典的责任链设计模式
	2. 执行filterChain.doFilter(servletRequest, servletResponse)表示会继续将请求执行下去；若不执行这一句，表示这一次的http请求到此为止，后面的走步不下去了

## 1.3、filter注册

过滤器注册到spsring容器中有多种使用方式，除了上面技术派中使用的注解@WebFilter之外还有其它几种使用姿势，下面逐一进行介绍

### 1.3.1、WebFilter+@ServletComponentScan注解

使用WebFilter注解，标注到自己实现的过滤器上，其中有几个参数需要注意

WebFilter常用属性如下，其中urlPatterns最为常用，表示这个filter适用那些url请求（默认场景下全部请求都被拦截）

| 属性名             | 类型             | 描述                                                                                     |     |
| --------------- | -------------- | -------------------------------------------------------------------------------------- | --- |
| filterName      | String         | 指定过滤器的 name 属性，等价于 `<filter-name>`标签                                                   |     |
| value           | String[]       | 该属性等价于 urlPatterns 属性。但是两者不应该同时使用。                                                     |     |
| urlPatterns     | String[]       | 指定一组过滤器的 URL 匹配模式。等价于 `<url-pattern>`标签                                                |     |
| servletName     | String[]       | 指定过滤器将应用于哪些 Servlet。取值是 `@WebServlet` 中的 name 属性的取值，或者是 web.xml 中 `<servlet-name>`的取值。 |     |
| dispatcherTypes | DispatcherType | 指定过滤器的转发模式。具体取值包括：ASYNC、ERROR、FORWARD、INCLUDE、REQUEST。                                 |     |
| initParams      | WebInitParam[] | 指定一组过滤器初始化参数，等价于 `<init-param>` 标签。                                                    |     |
| asyncSupported  | boolean        | 声明过滤器是否支持异步操作模式，等价于 `<async-supported>` 标签。                                            |     |
| description     | String         | 该过滤器的描述信息，等价于 `<description>` 标签。                                                      |     |
| displayName     | String         | 该过滤器的显示名，通常配合工具使用，等价于 `<display-name>` 标签。                                             |     |

使用这个注解时，请注意，需要在启动类/配置类上添加@ServletComponentScan注解来启用

如技术派中的启动类：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307292358321.png)


### 1.3.2、FilterRegistrationBean注解

上面一种方式比较简单，但是再指定 Filter 的优先级比较麻烦，不如下面这种方式简单
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307300001642.png)

特别说明：@WebFilter注解结合@Order来定义filter注解，可能不会生效，详情见：
1. [补2_【WEB系列】过滤器Filter使用指南](2、相关技术/16、常用框架-SpringBoot/补6_SpringBoot过滤器/补2_【WEB系列】过滤器Filter使用指南.md)
2. [补3_【WEB系列】过滤器Filter使用指南扩展篇](2、相关技术/16、常用框架-SpringBoot/补6_SpringBoot过滤器/补3_【WEB系列】过滤器Filter使用指南扩展篇.md)


## 1.4、实例说明
接下来我们再看一下，filter的具体表现，技术派中，这个过滤器中，主要干了三件事情：
1. 身份识别，并保存身份到ReqInfoContext上下文中
	1. 详情博文：[https://www.yuque.com/itwanger/az7yww/yk1x4v6wt5gz103q](https://www.yuque.com/itwanger/az7yww/yk1x4v6wt5gz103q)
2. 记录请求记录 
	1. 详情博文: [https://www.yuque.com/itwanger/az7yww/wb3pz26699c86nuz](https://www.yuque.com/itwanger/az7yww/wb3pz26699c86nuz)
3. 添加跨域支持
	1. 详情博文：[https://www.yuque.com/itwanger/az7yww/pznv1robndgbuyhh](https://www.yuque.com/itwanger/az7yww/pznv1robndgbuyhh)
我们以请求日志来看一下，filter的使用case
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307301100985.png)


# 2、小结
## 2.1、Filter使用

**自定义Filter的实现：**
1. 实现Filter接口
2. doFIlter方法中，显示调用chain.doFilter(request,response); 表示请求继续，否则表示请求被过滤。
**注册生效：**
1. @ServletComponentScan自动扫描带有@WebFilter注解的Filter
2. 创建Bean：FilerRegistrationBean来包装自定义的Filter

## 2.2、IoC/DI

在SpringBoot中Filter可以和一般的Bean一样使用，直接通过Autowired注入其依赖的Spring Bean对象

## 2.3、优先级

通过两种方式确定优先级：
1. 在创建FilterRegistrationBean的时候调用setOrder方法指定
	通过创建FilterRegistrationBean的时候指定优先级，如下：
	```java
	@Bean
	public FilterRegistrationBean<OrderFilter> orderFilter() {
		FilterRegistrationBean<OrderFilter> filter = new FilterRegistrationBean<>();
		filter.setName("orderFilter");
	    filter.setFilter(new OrderFilter());
	    filter.setOrder(-1);
	    return filter;
	}
	```
2. 在定义Filter的时候添加@Component和@Order注解

注意：使用@WebFilter注解声明的Filter优先级默认是214783647（最低优先级）
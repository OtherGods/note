在这篇文章中会介绍通过AOP实现切面日志，也会借机深入探讨Spring中的AOP机制，毕竟这是一道很常见的题目，简历中写专业技能的时候，一般也会协商“深入了解过Spring AOP机制”，这样写会给HR/面试官一个不错的印象

技术派中关于AOP切面的应用有两处：
1. MdcAspect用于方法执行耗时统计
2. DsAspect用于动态切换数据源
文章中重点是第一处，第二处会在“动态切换数据源”中详解。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308061948581.png)

本篇文章的内容会结合技术派源码来梳理AOP及其原理。
# 1、什么是AOP

AOP也就是面向切面编程，是计算机科学中的一个设计思想，旨在通过切面技术为业务主体增加额外的通知（Advice），从而对声明为“切点”的代码进行统一管理和装饰。

这种思想非常适用于将那些与核心业务不是那密切相关的功能添加到程序中，就好比我们我们今日的主题——日志功能，就是一个典型的案例。

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308061955926.png)

AOP是面向对象编程（OOP）的一种补充，OOP的核心单元是类，而AOP的核心单元是切面（Aspect），利用AOP可以对业务逻辑的各个部分进行隔离，从而降低耦合度，提高程序的可重用性，同时也提高了开发效率。

我们可以简单的把AOP理解为贯穿于方法之中，在方法执行前、执行时、执行后、返回值后、异常后要执行的操作。
# 2、AOP的相关术语

来看下面这幅图，这是一个AOP的模型图，就是在某些方法执行前后执行一些通用的操作，并且这些操作不会影响程序本身的运行
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308062026877.png)

我们先来了解AOP涉及的5个关键术语：
1. 横切关注点：从每个方法中抽取出来同一类非核心业务，每个横切关注点体现为一个<font color = "red">通知方法</font>。
2. 切面（Aspect）：对横切关注点（也就是<font color = "red">通知方法</font>）进行封装的类，通常使用@Aspect注解来定义切面
3. 通知（Advice）：切面必须要完成的各个具体工作，比如我们的日志切面需要记录接口调用前后的时长，就需要在接口调用前后记录时间，再取差值。通知的方式有五种：
	1. @Before：通知方法（横切关注点，也就是切面中的方法）会在目标方法调用之前执行
	2. @After：通知方法（横切关注点，也就是切面中的方法）会在目标方法调用之后执行
	3. @AfterReturning：通知方法（横切关注点，也就是切面中的方法）会在目标方法返回后执行
	4. @AfterThrowing：通知方法（横切关注点，也就是切面中的方法）会在目标方法抛出异常后执行
	5. @Around：把整个目标方法包裹起来，在被调用前和被调用后分别执行通知方法（横切关注点，也就是切面中的方法）
4. 连接点（JoinPoint）：通知应用的时机，比如接口方法被调用时就是日志切面的连接点
5. 切入点（Pointcut）：通知方法被应用的范围，比如本篇日志切面的应用范围是所有controller的接口。通常使用@Pointcut注解来定义切点表达式

切入点表达式的语法格式如下所示：
`execution(modifiers-pattern? ret-type-pattern declaring-type-pattern?name-pattern(param-pattern)throws-pattern?)`
1. modifiers-pattern?：为访问权限修饰符
2. ret-type-pattern：为返回类型
3. declaring-type-pattern?：为包名
4. name-pattern：为方法名，可以使用`*`来表示所有，或者`set*`来表示所有set开头的方法名
5. param-pattern)：为参数类型，多个参数可用 `,` 隔开，各个参数也可使用 `*` 来表示所有类型的参数，还可以是使用 `(..)` 表示零个或者任意参数
6. throws-pattern?：表示异常类型
7. ？：表示前面的为可选项

举个例子（来自技术派中 com.github.paicoding.forum.core.mdc.MdcAspect 类）：
```java
@Pointcut("@annotation(MdcDot) || @within(MdcDot)")
public void getLogAnnotation() {}
```

这个切入点定义的意思是：“拦截所有被MdcDot注解的方法，以及所有在被MdcDot注解的类中的方法”

# 3、实操AOP记录接口访问日志

技术派中的 AOP 记录接口访问日志是放在 paicoding-core 模块下的 mdc 包下。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308062220262.png)

## 3.1、SkyWalkingTraceIdGenerator

该类是从SkyWalking直接copy过来的，一种生成traceId的方式
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308062225861.png)

SkyWalking是以款开源的应用性能监控系统，它支持对分布式系统中的服务进行追踪、监控和诊断
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308062226697.png)

最主要的方法就是一个静态的generate方法，用于生成traceId
这个类生成的traceId包含三部分：
1. 第一部分是应用实例ID，它是在类加载时生成的一个UUID，对于每个进程，它是唯一的
2. 第二部分是当前线程的ID
3. 第三部分是一个由时间戳和线程序列号组成的数字，时间戳是毫秒级的，而线程序列号是一个在0~9999之间的数字。

这个工具类的设计思想主要是生成一个既唯一又能包含一些上下文信息的traceId，帮助我们更好的追踪和理解分布式系统中的请求执行路径。

## 3.2、SelfTraceIdGenerator

这是技术派自定义的一个traceId生成器，可以来详细看一下其中的generate方法
```java
/**  
 * <p>  
 * 生成32位traceId，规则是 服务器 IP + 产生ID时的时间 + 自增序列 + 当前进程号  
 * IP 8位：39.105.208.175 -> 2769d0af  
 * 产生ID时的时间 13位： 毫秒时间戳 -> 1403169275002  
 * 当前进程号 5位： PID  
 * 自增序列 4位： 1000-9999循环  
 * </p>  
 * w  
 * * @return ac13e001.1685348263825.095001000  
 */public static String generate() {  
    StringBuilder traceId = new StringBuilder();  
    try {  
        // 1. IP - 8  
        InetAddress ip = InetAddress.getLocalHost();  
        traceId.append(convertIp(IpUtil.getLocalIp4Address())).append(".");  
        // 2. 时间戳 - 13        traceId.append(Instant.now().toEpochMilli()).append(".");  
        // 3. 当前进程号 - 5        traceId.append(getProcessId());  
        // 4. 自增序列 - 4        traceId.append(getAutoIncreaseNumber());  
    } catch (Exception e) {  
        log.error("generate trace id error!", e);  
        return UUID.randomUUID().toString().replaceAll("-", "");  
    }  
    return traceId.toString();  
}
```

生成的traceId包含以下四部分：
1. IP地址（8位）：取得当前机器的IP地址，并将其转换为16进制格式
2. 时间戳（13位）：使用Java8的instant类获取当前的毫秒级时间戳
3. 进程号（5位）：使用Java的ManagementFactory类获取当前JVM进程的PID，并保证总长度为5位
4. 自增序列号（4位）：一个在1000到9999之间循环的自增的数

我们来对比一下 SelfTraceIdGenerator（前两个）和 SkyWalkingTraceIdGenerator（后一个）生成的 traceid。
```java
00000000.1686895888832.745811000
00000000.1686895888838.745811001
75e0cde204164cda98b0cca40b2999da.1.16868958889180000
```

## 3.3、为什么需要traceid呢？

当你的系统是分布式或微服务时，一个球友可能会穿过多个服务，每个服务可能都会生成一些日志，但是由于服务是微服务/分布式的，会运行在不同的物理机器上，如果没有一个统一的标识符来连接这些日志，就很难理解一个请求的完整过程。

traceId就是这样一个标识符，它在请求进入系统时生成，然后沿着请求的执行路径传递给所有参与处理该请求的服务。这些服务在生成日志时，会把traceId包含在日志中。这样，通过搜索同一traceId的所有日志，就可以追踪到整个请求的执行过程。

## 3.4、MdcUtil

MDC全称为Mapped Diagnostic Context，可以译为上下文诊断映射，也不知道标准不标准，大概就是这么一个意思。主要用于在多线程环境中存储每个线程特定的诊断信息，比如traceId。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308120939162.png)

该类主要提供了五个方法：
1. add方法：向MDC中添加一个键值对
2. addTraceId方法：生成一个traceId并添加到MDC中
3. getTraceId方法：从MDC中获取traceId
4. reset方法：清除MDC中的所有信息，然后把traceId添加回去
5. clear方法：清除MDC中的所有信息

如果你在技术派的源码中搜索MdcUtil的话，可以在ReqRecordFilter中找到，顾名思义，该类是对请求的一个过滤器，会在每个请求中加上全链路的traceId。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308121014722.png)

在req-dev.log中可以找到
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308121034706.png)

## 3.5、MdcDot

```java
@Target({ElementType.METHOD, ElementType.TYPE})  
@Retention(RetentionPolicy.RUNTIME)  
@Documented  
public @interface MdcDot {  
    String bizCode() default "";  
}
```

这段代码定义了一个Java注解@MdcDot，直接搜可以在以下这些地方得到：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308121041214.png)

## 3.6、MdcAspect

这是一个用于实现面向切面编程（AOP）的AspectJ切面。@AspectJ注解我们前面也讲到了它的作用
```java
@Aspect
public class MdcAspect implements ApplicationContextAware {}
```

MdcAspectJ切面的目的是处理添加了@MdcDot注解的方法或类。具体如何处理，由@Around注解标注的handle方法定义。
```java
@Pointcut("@annotation(MdcDot) || @within(MdcDot)")  
public void getLogAnnotation() {  
}  
  
@Around("getLogAnnotation()")  
public Object handle(ProceedingJoinPoint joinPoint) throws Throwable {  
    long start = System.currentTimeMillis();  
    boolean hasTag = addMdcCode(joinPoint);  
    try {  
        Object ans = joinPoint.proceed();  
        return ans;  
    } finally {  
        log.info("方法执行耗时: {}#{} = {}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName() , System.currentTimeMillis() - start);  
        if (hasTag) {  
            MdcUtil.reset();  
        }  
    }  
}
```

@Pointcut注解前面已经介绍过了，直接来看handle方法：

在handle方法中，首先记录了方法调用的开始时间，然后检查是否存在@MdcDot注解，并获取业务编码。

```java
private boolean addMdcCode(ProceedingJoinPoint joinPoint) {  
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();  
    Method method = signature.getMethod();  
    MdcDot dot = method.getAnnotation(MdcDot.class);  
    if (dot == null) {  
        dot = (MdcDot) joinPoint.getSignature().getDeclaringType().getAnnotation(MdcDot.class);  
    }  
  
    if (dot != null) {  
	    // loadBizCode(dot.bizCode(), joinPoint)获得SPEL表达式中内容对应的值（值从入参中获取）
        MdcUtil.add("bizCode", loadBizCode(dot.bizCode(), joinPoint));  
        return true;    }  
    return false;  
}
```

其中addMdcCode方法用于检查方法或类是否有@MdcDot注解并获取业务编码，loadBizCode方法用于解析@MdcDot注解的bizCode元素的值。

```java
private String loadBizCode(String key, ProceedingJoinPoint joinPoint) {  
    if (StringUtils.isBlank(key)) {  
        return "";  
    }  
  
    StandardEvaluationContext context = new StandardEvaluationContext();  
  
    context.setBeanResolver(new BeanFactoryResolver(applicationContext));  
    String[] params = parameterNameDiscoverer.getParameterNames(((MethodSignature) joinPoint.getSignature()).getMethod());  
    Object[] args = joinPoint.getArgs();  
    for (int i = 0; i < args.length; i++) {  
        context.setVariable(params[i], args[i]);  
    }  
    return parser.parseExpression(key).getValue(context, String.class);  
}
```

如果存在（这里说的是注解属性bizCode元素的值）则将业务编码添加到MDC中。接着调用原方法并返回结果，最后记录方法的执行时间并打印日志，如果方法有@MdcDot注解则重置MDC。

## 3.7、具体怎么用？

我们找到@MdcDot注解，直接看ArticleRestController的recommend方法吧
```java
/**  
 * 文章的关联推荐  
 *  
 * @param articleId  
 * @param page  
 * @param size  
 * @return  
 */  
@RequestMapping(path = "recommend")  
@MdcDot(bizCode = "#articleId")  
public ResVo<NextPageHtmlVo> recommend(@RequestParam(value = "articleId") Long articleId,  
                                       @RequestParam(name = "page") Long page,  
                                       @RequestParam(name = "size", required = false) Long size) {  
    size = Optional.ofNullable(size).orElse(PageParam.DEFAULT_PAGE_SIZE);  
    size = Math.min(size, PageParam.DEFAULT_PAGE_SIZE);  
    PageListVo<ArticleDTO> articles = articleRecommendService.relatedRecommend(articleId, PageParam.newPageInstance(page, size));  
    String html = templateEngineHelper.renderToVo("views/article-detail/article/list", "articles", articles);  
    return ResVo.ok(new NextPageHtmlVo(html, articles.getHasMore()));  
}
```

就是在方法上加注解@MdcDot(bizCode="#articleId")这段代码。

recommend方法对应的业务，是点击文章详情的时候触发相应的推荐文章。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308121136019.png)

我们再来看一下控制台的日志输出，内容如下所示：
```java
2023-06-16 11:06:13,008 [http-nio-8080-exec-3] INFO |00000000.1686884772947.468581113|101|c.g.p.forum.core.mdc.MdcAspect.handle(MdcAspect.java:47) - 方法执行耗时: com.github.paicoding.forum.web.front.article.rest.ArticleRestController#recommend = 47
```

其中traceId为 `00000000.1686884772947.468581113`

handle方法和recommend方法的执行顺序是这样的：（因为是环绕通知）
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308121145598.png)

# 4、小结
三道关于SpringAop的面试题，大家可以自行回答以下，这不是单纯的八股文，需要结合实际的项目来回答
1. 说说什么是AOP？
2. AOP有哪些核心概念？
3. AOP有哪些环绕方式？
4. 说说你平时都是怎么使用AOP的？
5. 说说Spring AOP和AspecJ AOP有什么区别？
6. 说说JDK动态代理和CGLIB代理？


答案可以在[《二哥的 Java 进阶之路》](https://tobebetterjavaer.com/sidebar/sanfene/spring.html)上找到答案哦。


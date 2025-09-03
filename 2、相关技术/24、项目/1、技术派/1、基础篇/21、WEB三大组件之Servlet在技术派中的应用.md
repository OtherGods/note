技术派中使用web三大组件中的filter做了不少操作；既然提到了filter，那么相关的另外两个组件，也有必要给大家介绍一下
> 以下为选修知识点，实际的业务开发中，用servlet的机会非常少

我们接下来主要看一下Servlet的使用姿势，以及注册自定义的Servelt的四种姿势：
1. @WebServlet注解
2. ServletRegistrationBean
3. ServletContext动态添加
4. 普通的spring bean模式
# 1、技术派使用实例

在技术派中有一个HealthServlet类，主要功能是用来校验服务的健康情况，在微服务中，判断应用是否存活，有一个方案就是定时的调用服务的一个接口，判断是否有正常返回，或者返回结果是否符合预期

具体实现也比较简单
```java
/**  
 * @author YiHui  
 * @date 2023/3/25  
 */
@WebServlet(urlPatterns = "/check")  
public class HealthServlet extends HttpServlet {  
  
    @Override  
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {  
        PrintWriter writer = resp.getWriter();  
        writer.write("ok");  
        writer.flush();  
        writer.close();  
    }  
}
```

访问上面的接口也很简单，直接 http://127.0.0.1:8080/check
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308061048820.png)

# 2、Servelt知识点

自定义一个Servlet比较简单，一般常见的操作是继承HttpServlet，然后覆盖doGet, doPost等方法即可；然而<font color = "red">重点是我们自定义的这些Servlet如何才能被SpringBoot识别并使用才是关键</fonnt>，下面介绍四种注册方式
## 2.1、@WebServlet

在自定义的servelt上添加Servelt3+的注解@WebServelt，来声明这个类是一个Servlet，和Filter的注册方式一样，使用这个注解，需要配合SpringBoot的@ServeltComponentScan，否则单纯的添加上面的注解并不会生效
```java
/**
使用注解的方式来定义并注册一个自定义的Servlet
*/
@WebServlet(urlPatterns = "/annotation")
public class AnnotationServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        PrintWriter writer = resp.getWriter();
        writer.write("[AnnotationServlet] welcome " + name);
        writer.flush();
        writer.close();
	}
}
```

上面是一个简单的测试Servlet，接收请求参数name，并返回welcome xxx；为了让上面的注解生效，需要设置下启动类：
```java
@ServletComponentScan
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
```

然后启动测试，输出结果下：
```java
➜  ~ curl http://localhost:8080/annotation\?name\=yihuihui
# 输出结果
[AnnotationServlet] welcome yihuihui%
```
## 2.2、ServletRegistrationBean

在Filter的注册中，我们知道有一种方式是定义一个Spring的Bean `FilterRegistrationBean` 来包装我们自定义Filter，从而让Spring容器来管理我们过滤器；同样的在Servelt中，也有类似的包装bean：ServeltRegistrationBean

自定义的bean如下，注意类上没有任何注解
```java
public class RegisterBeanServlet extends HttpServlet {
	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        PrintWriter writer = resp.getWriter();
        writer.write("[RegisterBeanServlet] welcome " + name);
        writer.flush();
        writer.close();
	}
}
```

接下来我们需要定义一个ServletRegistrationBean，让它持有RegisterBeanServlet的实例：
```java
@Bean
public ServletRegistrationBean servletBean() {
    ServletRegistrationBean registrationBean = new ServletRegistrationBean();
    registrationBean.addUrlMappings("/register");
    registrationBean.setServlet(new RegisterBeanServlet());
    return registrationBean;
}
```

测试请求输出如下：
```java
➜  ~ curl 'http://localhost:8080/register?name=yihuihui'
# 输出结果
[RegisterBeanServlet] welcome yihuihui%
```
## 2.3、ServletContext

这种姿势，在实际的Servlet注册中，其实用的并不太多，主要思路是在ServletContext初始化后，借助`javax.servlet.ServletContext#addServlet(java.lang.String, java.lang.Class<? extends javax.servlet.Servlet>)`方法来主动添加一个Servlet（也可以添加Servlet、Listener，需要分别调用addServlet、addListener方法）

所以我们需要找一个合适的时机，获取ServletContext实例，并注册Servlet，在SpringBoot生态下，可以借助ServletContextInitializer

> ServletContextInitializer主要被RegistrationBean实现用于往ServletContext容器中注册Servlet、Filter或者EventListener。这些ServletContextInitializer的设计目的主要是用于这些实例被Sprinng IoC容器管理

```java
public class ContextServlet extends HttpServlet {
	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        PrintWriter writer = resp.getWriter();
        writer.write("[ContextServlet] welcome " + name);
        writer.flush();
        writer.close();
    }
}

@Component
public class SelfServletConfig implements ServletContextInitializer {
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        ServletRegistration initServlet = servletContext.addServlet("contextServlet", ContextServlet.class);
        initServlet.addMapping("/context");
	}
}    
```

测试结果如下：
```java
➜  ~ curl 'http://localhost:8080/context?name=yihuihui'
# 输出结果
[ContextServlet] welcome yihuihui%
```
## 2.4、bean（有坑，没发现，慎用）

接下来的这种注册方式，并不优雅，但是可以实现Servlet的注册目的，但是有坑，请谨慎使用。

看过[WEB三大组件之Filter在技术派中的应用](https://www.yuque.com/itwanger/az7yww/tlxgqguw9ocxkhs5)的可能会有一些印象，可以直接在Filter上添加@Component注解，Spring容器扫描bena的时候，会查找所有实现Filter的子类，并主动将它包装到FilterRegistrationBean，实现注册的目的

我们的Servlet也可以这样，实测：
```java
@Component
public class BeanServlet1 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        PrintWriter writer = resp.getWriter();
        writer.write("[BeanServlet1] welcome " + name);
        writer.flush();
        writer.close();
	}
}
```

现在问题来了，上面这个Servelt没有定义urlMapping规则，怎么请求呢？为了确定上面的Servlet被注册了，借着前面Filter的源码分析的关键路径，我们找到了实际注册的地方ServletContextInitializerBeans#addAsRegistrationBean
```java
//org.springframework.boot.web.servlet.ServletContextInitializerBeans#addAsRegistrationBean(org.springframework.beans.factory.ListableBeanFactory, java.lang.Class<T>, java.lang.Class<B>, org.springframework.boot.web.servlet.ServletContextInitializerBeans.RegistrationBeanAdapter<T>)

@Override
public RegistrationBean createRegistrationBean(String name, Servlet source, int totalNumberOfSourceBeans) {
	String url = (totalNumberOfSourceBeans != 1) ? "/" + name + "/" : "/";
	if (name.equals(DISPATCHER_SERVLET_NAME))
	{
		url = "/"; // always map the main dispatcherServlet to "/"
	}
	ServletRegistrationBean<Servlet> bean = new ServletRegistrationBean<>(source, url);
	bean.setName(name);
	bean.setMultipartConfig(this.multipartConfig);
	return bean;
}
```

从上面的源码上可以看出来，这个Servlet的url要么是 `/` 要么是 `/beanName/`

接下来进行实测，全是404
```java
➜  ~ curl 'http://localhost:8080/?name=yihuihui'
{"timestamp":"2019-11-22T00:52:00.448+0000","status":404,"error":"Not Found","message":"No message available","path":"/"}%

➜  ~ curl 'http://localhost:8080/beanServlet1?name=yihuihui'
{"timestamp":"2019-11-22T00:52:07.962+0000","status":404,"error":"Not Found","message":"No message available","path":"/beanServlet1"}%

➜  ~ curl 'http://localhost:8080/beanServlet1/?name=yihuihui'
{"timestamp":"2019-11-22T00:52:11.202+0000","status":404,"error":"Not Found","message":"No message available","path":"/beanServlet1/"}%
```

然后再定义一个Servlet时
```java
@Component
public class BeanServlet2 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        PrintWriter writer = resp.getWriter();
        writer.write("[BeanServlet2] welcome " + name);
        writer.flush();
        writer.close();
    }
}
```

再次测试：
```java
➜  ~ curl 'http://localhost:8080/beanServlet1?name=yihuihui'
{"timestamp":"2019-11-22T00:54:12.692+0000","status":404,"error":"Not Found","message":"No message available","path":"/beanServlet1"}%

➜  ~ curl 'http://localhost:8080/beanServlet1/?name=yihuihui'
[BeanServlet1] welcome yihuihui%

➜  ~ curl 'http://localhost:8080/beanServlet2/?name=yihuihui'
[BeanServlet2] welcome yihuihui%
```

从实际的测试结果可以看出，使用这种定义方式时，这个servlet相应的url为beanName + '/'

**注意事项**
然后问题来了，只定义一个Servlet的时候，根据前面的源码分析，这个Servlet应该会响应`http://localhost:8080/`的请求，然而测试的时候为啥是404？

这个问题也好解答，主要就是Servelt的优先级问题，上面这种方式的Servlet的相应优先级低于SpringWeb的Servlet优先级，相同的url请求分配给Spring的Servlet了，验证可以分为两步：
1. 先注释BeanServlet2类上的注解@Component
2. 在BeanServlet1的类上，添加注解@Order(-10000)
然后再启动测试，输入如下：
```java
➜  ~ curl 'http://localhost:8080/?name=yihuihui'
[BeanServlet1] welcome yihuihui%

➜  ~ curl 'http://localhost:8080?name=yihuihui'
[BeanServlet1] welcome yihuihui%
```

## 2.5、小结
示例工程源码
- 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 项目：[https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/210-web-filter](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/210-web-filter)  
    已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)


**四种注册方式**
这里主要介绍了四种Servlet的注册方式

常见的两种注册case：
1. @WebServlet注解放在Servlet类上，然后启动类上添加@ServletComponentScan，确保Servlet3+的注解可以被Spring识别；
2. 将自定义Servlet实例委托给bean `ServletRegistrationBean`。

不常见的两种注册case：
1. 实现接口ServletContextInitializer，通过ServletContext.addServlet来注册自定义Servlet；
2. 直接将Servlet当做普通的bean注册给Spring
	1. 当项目只有一个此种case的servelt时，它响应url：`/`，但是需要注意不指定优先级别时，默认场景下Spring的Servlet优先级更高，所以它接收不到请求；
	2. 当项目有多个此种case的servlet时，响应的url为beanName + `/`，注意后面 `/` 必须有。






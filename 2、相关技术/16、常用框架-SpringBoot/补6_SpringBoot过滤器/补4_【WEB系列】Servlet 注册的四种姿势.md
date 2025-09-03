前面介绍了java web三要素中filter的使用指南与常见的易错事项，接下来我们来看一下Servlet的使用姿势，本篇主要带来在 [SpringBoot](https://spring.hhui.top/spring-blog/2019/11/22/191122-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Servlet-%E6%B3%A8%E5%86%8C%E7%9A%84%E5%9B%9B%E7%A7%8D%E5%A7%BF%E5%8A%BF/#)环境下，注册自定义的Servelt的四种姿势

- `@WebServlet` 注解
- `ServletRegistrationBean` bean定义
- `ServletContext` 动态添加
- 普通的 [spring](https://spring.hhui.top/spring-blog/2019/11/22/191122-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Servlet-%E6%B3%A8%E5%86%8C%E7%9A%84%E5%9B%9B%E7%A7%8D%E5%A7%BF%E5%8A%BF/#) bean模式

## 1、环境配置

### 1.1、项目搭建

首先我们需要搭建一个web工程，以方便后续的servelt注册的实例演示，可以通过spring boot官网创建工程，也可以建立一个maven工程，在pom.xml中如下配置
```xml
<parent>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-parent</artifactId>  
    <version>2.2.1.RELEASE</version>  
    <relativePath/> <!-- lookup parent from repository -->  
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
                <groupId>org.springframework.boot</groupId>  
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

**特别说明：**

为了紧跟 [SpringBoot](https://spring.hhui.top/spring-blog/2019/11/22/191122-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Servlet-%E6%B3%A8%E5%86%8C%E7%9A%84%E5%9B%9B%E7%A7%8D%E5%A7%BF%E5%8A%BF/#)的最新版本，从本篇文章开始，博文对应的示例工程中SpringBoot版本升级到`2.2.1.RELEASE`

## 2、Servlet注册

自定义一个Servlet比较简单，一般常见的操作是继承`HttpServlet`，然后覆盖`doGet`, `doPost`等方法即可；然而重点是我们自定义的这些Servlet如何才能被SpringBoot识别并使用才是关键，下面介绍四种注册方式

### 2.1、@WebServlet

在自定义的servlet上添加Servlet3+的注解`@WebServlet`，来声明这个类是一个Servlet

和Fitler的注册方式一样，使用这个注解，需要配合 [Spring Boot](https://spring.hhui.top/spring-blog/2019/11/22/191122-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Servlet-%E6%B3%A8%E5%86%8C%E7%9A%84%E5%9B%9B%E7%A7%8D%E5%A7%BF%E5%8A%BF/#)的`@ServletComponentScan`，否则单纯的添加上面的注解并不会生效
```java
/**  
 * 使用注解的方式来定义并注册一个自定义Servlet  
 * Created by @author yihui in 19:08 19/11/21.  
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

上面是一个简单的测试Servlet，接收请求参数`name`, 并返回 `welcome xxx`；为了让上面的的注解生效，需要设置下启动类

```java
@ServletComponentScan  
@SpringBootApplication  
public class Application {  
    public static void main(String[] args) {  
        SpringApplication.run(Application.class);  
    }  
}
```

然后启动测试，输出结果如:

```java
➜  ~ curl http://localhost:8080/annotation\?name\=yihuihui  
# 输出结果  
[AnnotationServlet] welcome yihuihui%
```

### 2.2、ServletRegistrationBean

在Filter的注册中，我们知道有一种方式是定义一个 [Spring](https://spring.hhui.top/spring-blog/2019/11/22/191122-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Servlet-%E6%B3%A8%E5%86%8C%E7%9A%84%E5%9B%9B%E7%A7%8D%E5%A7%BF%E5%8A%BF/#)的Bean `FilterRegistrationBean`来包装我们的自定义Filter，从而让Spring容器来管理我们的过滤器；同样的在Servlet中，也有类似的包装bean: `ServletRegistrationBean`

自定义的bean如下，注意类上没有任何注解
```java
/**  
 * Created by @author yihui in 19:17 19/11/21.  
 */  
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

接下来我们需要定义一个`ServletRegistrationBean`，让它持有`RegisterBeanServlet`的实例

```java
@Bean  
public ServletRegistrationBean servletBean() {  
    ServletRegistrationBean registrationBean = new ServletRegistrationBean();  
    registrationBean.addUrlMappings("/register");  
    registrationBean.setServlet(new RegisterBeanServlet());  
    return registrationBean;  
}
```

测试请求输出如下:
```java
➜  ~ curl 'http://localhost:8080/register?name=yihuihui'  
# 输出结果  
[RegisterBeanServlet] welcome yihuihui%
```

### 2.3、ServletContext

这种姿势，在实际的Servlet注册中，其实用得并不太多，主要思路是在ServletContext初始化后，借助`javax.servlet.ServletContext#addServlet(java.lang.String, java.lang.Class<? extends javax.servlet.Servlet>)`方法来主动添加一个Servlet

所以我们需要找一个合适的时机，获取`ServletContext`实例，并注册Servlet，在 [SpringBoot](https://spring.hhui.top/spring-blog/2019/11/22/191122-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Servlet-%E6%B3%A8%E5%86%8C%E7%9A%84%E5%9B%9B%E7%A7%8D%E5%A7%BF%E5%8A%BF/#)生态下，可以借助`ServletContextInitializer`

> ServletContextInitializer主要被RegistrationBean实现用于往ServletContext容器中注册Servlet,Filter或者EventListener。这些ServletContextInitializer的设计目的主要是用于这些实例被Spring IoC容器管理

```java
/**  
 * Created by @author yihui in 19:49 19/11/21.  
 */  
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
  
  
/**  
 * Created by @author yihui in 19:50 19/11/21.  
 */  
@Component  
public class SelfServletConfig implements ServletContextInitializer {  
    @Override  
    public void onStartup(ServletContext servletContext) throws ServletException {  
        ServletRegistration initServlet = servletContext.addServlet("contextServlet", ContextServlet.class);  
        initServlet.addMapping("/context");  
    }  
}
```

测试结果如下

```java
➜  ~ curl 'http://localhost:8080/context?name=yihuihui'  
# 输出结果  
[ContextServlet] welcome yihuihui%
```

### 2.4、bean

接下来的这种注册方式，并不优雅，但是也可以实现Servlet的注册目的，但是有坑，请各位大佬谨慎使用

看过我的前一篇博文[191016-SpringBoot系列教程web篇之过滤器Filter使用指南](https://mp.weixin.qq.com/s/f01KWO3d2zhoN0Qa9-Qb6w)的同学，可能会有一点映象，可以在Filter上直接添加`@Component`注解， [Spring](https://spring.hhui.top/spring-blog/2019/11/22/191122-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Servlet-%E6%B3%A8%E5%86%8C%E7%9A%84%E5%9B%9B%E7%A7%8D%E5%A7%BF%E5%8A%BF/#)容器扫描bean时，会查找所有实现Filter的子类，并主动将它包装到`FilterRegistrationBean`，实现注册的目的

我们的Servlet是否也可以这样呢？接下来我们实测一下
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

现在问题来了，上面这个Servlet没有定义urlMapping规则，怎么请求呢？

为了确定上面的Servlet被注册了，借着前面Filter的源码分析的关键链路，我们找到了实际注册的地方`ServletContextInitializerBeans#addAsRegistrationBean`

```java
// org.springframework.boot.web.servlet.ServletContextInitializerBeans#addAsRegistrationBean(org.springframework.beans.factory.ListableBeanFactory, java.lang.Class<T>, java.lang.Class<B>, org.springframework.boot.web.servlet.ServletContextInitializerBeans.RegistrationBeanAdapter<T>)  
  
@Override  
public RegistrationBean createRegistrationBean(String name, Servlet source, int totalNumberOfSourceBeans) {  
	String url = (totalNumberOfSourceBeans != 1) ? "/" + name + "/" : "/";  
	if (name.equals(DISPATCHER_SERVLET_NAME)) {  
		url = "/"; // always map the main dispatcherServlet to "/"  
	}  
	ServletRegistrationBean<Servlet> bean = new ServletRegistrationBean<>(source, url);  
	bean.setName(name);  
	bean.setMultipartConfig(this.multipartConfig);  
	return bean;  
}
```

从上面的源码上可以看到，这个Servlet的url要么是`/`, 要么是`/beanName/`

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

再次测试
```java
➜  ~ curl 'http://localhost:8080/beanServlet1?name=yihuihui'  
{"timestamp":"2019-11-22T00:54:12.692+0000","status":404,"error":"Not Found","message":"No message available","path":"/beanServlet1"}%                                            
  
➜  ~ curl 'http://localhost:8080/beanServlet1/?name=yihuihui'  
[BeanServlet1] welcome yihuihui%                                                                                                                                                  
  
➜  ~ curl 'http://localhost:8080/beanServlet2/?name=yihuihui'  
[BeanServlet2] welcome yihuihui%
```

从实际的测试结果可以看出，使用这种定义方式时，这个servlet相应的url为`beanName + '/'`

**注意事项**

然后问题来了，只定义一个Servlet的时候，根据前面的源码分析，这个Servlet应该会相应`http://localhost:8080/`的请求，然而测试的时候为啥是404？

这个问题也好解答，主要就是Servlet的优先级问题，上面这种方式的Servlet的相应优先级低于 [Spring](https://spring.hhui.top/spring-blog/2019/11/22/191122-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Servlet-%E6%B3%A8%E5%86%8C%E7%9A%84%E5%9B%9B%E7%A7%8D%E5%A7%BF%E5%8A%BF/#) Web的Servelt优先级，相同的url请求先分配给Spring的Servlet了，为了验证这个也简单，两步

- 先注释`BeanServlet2`类上的注解`@Component`
- 在`BeanServlet1`的类上，添加注解`@Order(-10000)`

然后再次启动测试,输出如下
```java
➜  ~ curl 'http://localhost:8080/?name=yihuihui'  
[BeanServlet1] welcome yihuihui%  
  
➜  ~ curl 'http://localhost:8080?name=yihuihui'  
[BeanServlet1] welcome yihuihui%
```

### 2.5、小结

本文主要介绍了四种Servlet的注册方式，至于Servlet的使用指南则静待下篇

常见的两种注册case:

- `@WebServlet`注解放在Servlet类上，然后启动类上添加`@ServletComponentScan`，确保Serlvet3+的注解可以被Spring识别
- 将自定义Servlet实例委托给bean `ServletRegistrationBean`

不常见的两种注册case:

- 实现接口`ServletContextInitializer`，通过`ServletContext.addServlet`来注册自定义Servlet
- 直接将Serlvet当做普通的bean注册给Spring
    - 当项目中只有一个此种case的servlet时，它响应url: ‘/‘, 但是需要注意不指定优先级时，默认场景下Spring的Servlet优先级更高，所以它接收不到请求
    - 当项目有多个此种case的servlet时，响应的url为`beanName + '/'`， 注意后面的’/‘必须有

## 3、其他
## 3.1、项目源码
- 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 项目：[https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/210-web-filter](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/210-web-filter)
已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)

转载自：[【WEB系列】Servlet 注册的四种姿势](https://spring.hhui.top/spring-blog/2019/11/22/191122-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Servlet-%E6%B3%A8%E5%86%8C%E7%9A%84%E5%9B%9B%E7%A7%8D%E5%A7%BF%E5%8A%BF/)



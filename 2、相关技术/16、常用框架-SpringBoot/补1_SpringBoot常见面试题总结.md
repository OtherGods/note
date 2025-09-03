文章来自JavaGuide中文章：[Java面试指北](https://www.yuque.com/books/share/04ac99ea-7726-4adb-8e57-bf21e2cc7183)

# 1、简单介绍以下Spring有啥缺点？

Spring是重量级企业开发框架Enterprise JavaBean（EJB）的替代品，Spring为企业级Java开发提供了一种相对简单的防范，通过依赖注入和面向切面编程，用简单的Java对象实现了EJB的功能。

**虽然Spring的组件代码是轻量级的，但是它的配置是重量级的（需要大量XML配置）。**

为此，Spring2.5中引入了基于注解的组件扫描，这消除了大量针对应用程序自身组件的显示XML配置。Sprig3.0引入了基于Java的配置，这是一种类型安全的可重构配置方式，可以替代XML。

尽管如此，我们依旧没能逃脱配置的魔抓。开启某些Spring特性时，比如事务管理和Spring MVC，还是需要用到XML或Java进行显示配置。启动第三方酷时也需要显示配置，比如基于Thymeleaf的Web视图。配置Servlet和过滤器（比如Spring的DispatcherServlet）通用需要在web.xml或Servlet初始化代码中进行显示配置。组件扫描减少了配置量，Java配置让它看上去简介了不少，但是Spring还是需要不少配置。

单配置这些XML文件都够我们头疼了，占用了我们大部分时间和经历。除此之外，相关库的依赖非常让人们头疼，不同库之间的版本冲突也非常常见。

# 2、为什么要有SpringBoot？

Spring旨在简化J2EE企业应用程序开发。Spring Boot旨在简化Spring开发（减少配置文件，开箱即用）。
![image-20221015223806834](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221015223806834.png)

# 3、说出使用SpringBoot的主要优点

1. 开发基于Spring的应用程序很容易
2. Spring Boot项目所需的开发或工程时间明显减少，通常会提高整体生产力
3. Spring Boot不需要编写大量样板代码、XML配置和注解
4. Spring引导应用程序可以很容易的与Spring生态系统继承，如Spring JDBC、Spring ORM、Spring Data、Spring Security等
5. Spring Boot遵循“固执己见的默认配置”，以减少开发工作（默认配置可以修改）
6. Spring Boot应用程序提供嵌入式HTTP服务器，如果Tomcat和Jetty，可以轻松的开发和测试web应用程序（这点很赞！普通运行Java程序的方式就能运行基于Spring Boot web项目，省事很多）
7. Spring Boot提供命令接口（CLI）工具，用于开发和测试Spring Boot应用程序，如Java或Groovy
8. Spring Boot提供了多种擦火箭，可以使用内置工具（如Maven和Gradle）开发和测试Spring Boot应用程序

# 4、什么是Spring Boot Staeters？

Spring Boot Starts是一系列依赖关系的集合，因为它的存在，项目的依赖之间的关系对我们来说变得更加简单了。

举个例子：在没有Spring Boot Starts之前，我们开发REST服务或Web应用程序时；我们需要使用像Spring MVC、Tomcat、Jackson这样的库，这些依赖我们需要手动一个一个添加。但是有了Spring Boot Starters我们只需要添加一个spring-boot-starter-web一个依赖就可以了，这个依赖包含的子依赖中包含了我们开发REST服务需要的所有依赖。

![image-20221015224809482](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221015224809482.png)



# 5、Spring Boot支持哪些内嵌Servlet容器？

Spring Boot支持以下嵌入式Servlet容器：
![image-20221015224920888](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221015224920888.png)

你还可以将Spring引导应用程序部署到任何Servlet3.1+兼容的Web容器中。

这就是你为什么可以通过直接像运行普通Java项目一样运行SpringBoot项目。这样的确省了很多事，方便了我们进行开发，降低了学习难度。

# 6、如何在Spring Boot应用程序中使用Jetty而不是Tomcat？

Spring Boot（Spring-boot-starter-web）使用Tomcat作为默认的嵌入式servlet容器，如果你想要使用Jetty的话只需要修改pom.xml（Maven）或者build.gradle（Gradle）就可以了。

**Maven：**

![image-20221015225628564](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221015225628564.png)

**Gradle：**

![image-20221015225718968](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221015225718968.png)

从上面可以看出使用Gradle更加简介明了，但是国内目前还是Maven使用的多。



# 7、介绍一下@SpringBootApplication注解

![image-20221015225915151](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221015225915151.png)

可以看出大概可以把@SpringBootApplication看作是@Configuration、@EnableAutoConfiguration、@ComponentScan注解的集合。根据SpringBoot官网，这三个注解的作用分别是：

1. @EnableAutoConfiguration：启用SpringBoot的自动配置机制
2. @ComponentScan：扫描被@Component、@Service、@Controller注解的bean，注解默认会扫描该注解注解的类所在的包下所有的类
3. @Configuration：允许在上下文中注册额外的bean或者导入其他配置类

# 8、Spring Boot的自动配置是如何实现的？

这个是因为@SpringBootApplication注解的原因，在上一个问题中已经提到了这个注解。我们知道@SpringBootApplication看作是@Configuration、@EnableAutoConfiguration、@ComponentScan注解的集合

1. @EnableAutoConfiguration：启用SpringBoot的自动配置机制
2. @ComponentScan：扫描被@Component、@Service、@Controller注解的bean，注解默认会扫描该注解注解的类所在的包下所有的类
3. @Configuration：允许在上下文中注册额外的bean或者导入其他配置类

@EnableAutoConfiguration是启动自动配置的关键，源码如下（建议打断点调试，走一遍基本的流程）：
![image-20221015230729733](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221015230729733.png)

@EnableAutoConfiguration注解通过Spring提供的@Import注解导入了AutoConfigurationImportSelector类（@Import注解可以岛主配置类或者Bean到当前类中）

AutoConfigurationImportSelector类中的getCandidateConfigurations方法将所有自动配置类的信息以List的形式返回。这些配置信息会被Spring容器作bean来管理。

@Conditional注解。@ConditionalOnClass（指定的类必须存在于类路径下），@ConditionalOnBean（容器中必须有指定的Bean）等等都是对@Conditional注解的扩展。

拿Spring Security的自动配置举个例子：SecurityAutoConfiguration中导入了WebSecurityEnablerConfiguration类，WebSecurityEnablerConfiguration类的源代码如下：
![image-20221015231926088](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221015231926088.png)

WebSecurityEnablerConfiguration类中使用@ConditionalOnBean指定了容器中必须还有WebSecurityConfigurerAdapter类或者其实现类。所以，一般情况下Spring Security配置类都会去实现WebSecurityConfigurerAdapter，这样自动配置就完成了。

# 9、开发RESTful Web服务常用的注解有哪些？

Spring Bean相关：

1. @Autowired：自动导入对象到类中，被注入进的类同样要被Spring容器管理
2. @RestController：@RestController注解是@Controller和@ResponseBody的合集，表示这是个控制器bean，并且是将函数的返回值直接填入HTTP响应体中，是REST风格的控制器
3. @Component：通用的出街，而可以标注任意类为Spring组件。如果一个Bean不知道属于哪个层，可以使用@Component注解标注
4. @Respository：对应持久层即Dao层，主要用于数据库相关操作
5. @Service：对应服务层，主要涉及一些复杂的逻辑，需要用到Dao层
6. @Controller：对应Spring MVC控制层，主要用于接受用户请求并调用Service层返回数据给前端页面

处理常见的HTTP请求类型：

1. @GetMapping：GET请求
2. @PostMapping：POST请求
3. @PutMapping：PUT请求
4. @DeleteMapping：DELETE请求

前后端传值：

1. @RequestParam以及@Pathvairable：@Pathvairable用于获取路径参数，@RequestParam用于获取查询参数
2. @RequestBody：用于获取Request请求（可能是POST、PUT、DELETE、GET请求）的body部分并且Content-Type为application/json格式的数据，接受到数据之后会自动将数据绑定到Java对象上去。系统会使用HttpMessageConverter或则和自定义的HttpMessageConverter将请求的body中的json字符串转换为Java对象



# 10、Spring Boot常用的两种配置文件

![image-20221016115005211](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221016115005211.png)

# 11、什么是YAML？YAML配置的优势在哪里？

![image-20221016115150924](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221016115150924.png)

![image-20221016115209821](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221016115209821.png)

但是YAML配置的方式有一个缺点，那就是不支持@PropertySource注解导入自定义的YAML配置。

# 12、Spring Boot常用的读取配置文件的方法有哪些？

我们需要读取配置文件application.yml内容如下：
![image-20221016115427475](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221016115427475.png)

## 12.1、通过@Value读取比较简单的配置信息

使用@Value("${property}")读取比较简单的配置信息：

```java
@Value("${wuhan2020}")
String wuhan2020;
```

> 需要注意的是@value这种方式是不被推荐的，Spring比较建议的是使用下面几种方式读取配置信息。

## 12.2、通过@ConfigurationProperties读取并于bean绑定

> LibraryProperties类加上了@Component注解，我们可以像使用普通bean一样将其注入到类中使用。

![image-20221016120054710](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221016120054710.png)



## 12.3、通过@ConfigurationProperties读取并检验





## 12.4、@PropertySource读取指定的properties文件



# 13、Spring Boot加载配置文件的优先级了解吗？

![image-20221016120319165](D:\Tyora\AssociatedPicturesInTheArticles\SpringBoot常见面试题总结\image-20221016120319165.png)



# 14、常用的Bean映射工具有哪些？



# 15、Spring Boot如何监控系统实际运行状况？





# 16、Spring Boot如何做请求参数校验？



## 16.1、校验注解





## 16.2、验证请求体（RequestBody）



## 16.3、验证请求参数（Path Variables和Request Parameters）



# 17、如何使用Spring Boot实现全局异常处理？





# 18、Spring Boot中如何实现定时任务？
















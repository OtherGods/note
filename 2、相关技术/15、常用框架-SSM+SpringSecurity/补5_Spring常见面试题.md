来自：[Spring常见面试题总结-JavaGuide.pdf](D:\z_知识星球\JavaGuide\JavaGuide的知识星球\Spring常见面试题总结-JavaGuide.pdf)



# 1、Spring基础

## 1.1 什么是Spring框架？

Spring框架是一款开源的轻量级Java开源框架，旨在提高人们的开发效率以及系统的可维护性

我么一般说的Spring框架指的都是Spring Framework，它是很多模块的集合，使用这些模块可以很方便的协助我们开发，比如Spring支持IoC（控制反转）和AOP（面向切面编程）、可以很方便的对数据库进行访问、可以很方便的集成第三方组件（电子邮件，任务调度，缓存等等）、对单元测试支持比较好、支持RESTful Java应用程序的开发。

Spring最核心的思想就是不重复造轮子，开箱即用，提高开发效率。

## 1.2、Spring模块

### 1.2.1 Core Container

Spring框架的核心模块，也可以说是基础模块，主要提供IoC依赖注入功能的支持。Spring其他所有功能基本都是依赖于该模块。

1. Spring-core：Spring框架基本的核心工具类
2. Spring-beans：提供对bean的创建、配置和管理等功能的支持
3. Spring-context：提供对国际化、事件传播、资源加载等功能的支持
4. Spring-expression：提供对表达式语言SpEL的支持，只依赖于core模块，不依赖于其他模块，可以单独使用。

### 1.2.2 AOP

1. Spring-aspects：该模块为与AspectJ的集成提供支持
2. Spring-aop：提供了面向切面的编程实现
3. Spring-instrument：提供了为JVM添加代理的功能。具体来讲，他为Tomcat提供了一个织入代理，能够为Tomcat传递类文件，就像这些文件是被类加载器加载的一样。

### 1.2.3 Data Access/Integration

1. Spring-jdbc：提供了数据库访问的抽象JDBC。不同的数据库都有自己独立的API用于操作数据库，而Java程序只需要和JDBC API交互，这样就屏蔽了数据库的影响
2. Spring-tx：提供了对事务的支持
3. Spring-orm：提供对Hibernate、JPA、iBatie等ORM框架的支持
4. Spring-oxm：提供一个抽象层支持OXM，例如：JAXB、Castor、XMLBeand、JiBX、XStream等
5. Spring-jms：消息服务。

### 1.2.4 Spring Web

1. Srping-web：对Web功能的实现提供一些最基础的支持
2. Spring-webmvc：提供对Spring MVC的实现
3. Spring-websocket：提供了对WebSocket的支持，WebSocket可以让客户端和服务端进行双向通信
4. Spring-webflux：提供对WebFlux的支持。WebFlux是Spring Framework5.0引入的响应式框架，与Spring MVC不同，它不需要Servlet API，是完全异步

### 1.2.5 Messaging

Spring-messaging 是从Spring 4.0开始新加入的一个模块，主要职责是未Spring框架继承一些基础的报文传送应用

### 1.2.6 Spring Test

Spring团队提倡测试驱动开发（TDD）。有了控制翻转（IoC）的帮助，单元测试和集成测试变得更加简单。

Spring的测试模块对Junit（单元测试框架）、TestNG（类似JUnit）、Mockito（主要用来Mock对象）、PowerMock（解决Mockito的问题比如无法模拟final、static、private方法）等等常用的测试框架支持的都比较好。

## 1.3、Spring、Spring MVC、Spring Boot之间什么关系

Spring包含了多个功能模块，其中最重要的是Spring-Core（主要提供IoC依赖注入功能的支持）模块，Spring中的其他模块（比如Spring MVC）的功能实现基本都需要依赖于该模块。

SpringMVC是Spring中的一个很重要的模块，主要赋予Spring快速构建MVC架构的Web程序的能力。MVC是模型、视图、控制器的缩写，其核心思想是通过业务逻辑、数据、显示分离来组织代码。

使用Spring进行开发各种配置过于麻烦比如开启某些Spring特性时，需要用XML或Java进行显示配置。于是，Spring Boot诞生了！

Spring旨在简化J2EE企业应用程序开发，Spring Boot旨在简化Spring开发（减少配置文件，开箱即用）。

Spring Boot只是简化了配置，如果你需要构建MVC架构的Web程序，你还是需要使用Spring MVC作为MVC框架，只是说Spring Boot帮你简化了Spring MVC的很多配置，做到开箱即用！



# 2、Spring IoC

### 2.1 谈谈自己对Spring IoC的了解

IoC是一种设计思想，而不是一个具体的技术实现。IoC的思想就是将原本在程序中手动创建对象的控制权，交给Spring框架来管理。不过，IoC并非Spring特有，在其他语言中也有。

为什么叫做控制反转？

1. 控制：指的是对象创建（实例化、管理）的权力
2. 反转：控制权交给外部环境（Spring框架、IoC容器）

![image-20221005090634529](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005090634529.png)

将对象之间的相互依赖关系交给IoC容器来管理，并由IoC容器完成对象的注入。这样可以很大程度上简化应用的开发，把应用从复杂的依赖关系中解放出来，IoC容器就像是一个工厂一样，当我们需要创建一个对象的时候，只需要配置好配置文件/注解即可以，完全不用考虑对象是如何被创建出来的。

在实际项目中一个Service类可能依赖了很多其他的类，假如我们需要实例化这个Service，你可能要每次都搞清这个Service所有底层类的构造函数，这可能会把人逼疯。如果利用IoC的话，你只需要配置好，然后再需要的地方引入就可以了，这大大增加了项目的可维护性且降低了开发难度。

在Spring中，IoC容器是Spring用来实现IoC的载体，IoC容器实际上就是个Map(key,value)，Map中存放的是各种对象。

Spring时代我们一般通过XML文件来配置Bean，后来开发人员觉得XML文件来配置不太好，于是SpringBoot注解配置就慢慢开始流行起来

### 2.2 IoC、AOP

来自：[面试被问了几百遍的 IoC 和 AOP ，还在傻傻搞不清楚？](https://mp.weixin.qq.com/s?__biz=Mzg2OTA0Njk0OA==&mid=2247486938&idx=1&sn=c99ef0233f39a5ffc1b98c81e02dfcd4&chksm=cea24211f9d5cb07fa901183ba4d96187820713a72387788408040822ffb2ed575d28e953ce7&token=1736772241&lang=zh_CN#rd)

主要内容：

1. 什么是IoC？
2. IoC解决了什么问题？
3. IoC和DI的区别？
4. 什么是AOP？
5. AOP解决了什么问题？
6. AOP为什么叫做切面编程？

首先声明：IoC & AOP 不是Spring提出来的，它们在Spring之前其实已经存在了，只不过当时更加偏向理论。Spring在技术层次将这两个思想进行了很好的实现。

#### 2.2.1什么是IoC

IoC控制反转，是一种思想不是一个技术实现，描述的是：Java卡法领域对象的创建以及管理的问题。

例如：现有类A依赖于类B

1. 传统的开发方式：往往是在类A中手动通过new关键字来new一个B的对象对象
2. 使用IoC思想的开发方式：不通过new关键字来创建对象，而是通过IoC容器（Spring框架）来帮助我们实例化对象。我们需要哪个对象，直接从IoC容器中拿即可。

从上面两种方式对比来看：我们 “丧失了一个权力 ” （创建、管理对象的权力），从而也得到了一个好处（不用再考虑对象的创建啊你、管理等一些列的事情）

**为什么叫做控制反转？**

1. 控制：指的是对象创建（实例化、管理）的权力
2. 控制权交给外部环境（Spring框架、IoC容器）
   ![image-20221005094511298](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005094511298.png)



### 2.2.2 IoC和DI的区别

IoC（控制反转）是一种设计思想或者是某种模式。这个设计思想就是<font color = "red">**将原本在程序中手动创建对象的控制权，交给Spring框架来管理。**</font>IoC在其他语言中也有应用，并非Spring特有。<font color = "red">**IoC容器是Spring用来实现IoC的载体，IoC容器实际上就是个Map(key , value)，Map中存放的是各种对象**</font>。

IoC最常见以及最合理的实现发过誓叫做依赖注入（简称DI）。

并且，老马（Martin Fowler）在一篇文章中提到将 IoC 改名为 DI，原文如下，原文地址：https://martinfowler.com/articles/injection.html 。

![图片](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\640)

老马的大概意思是 IoC 太普遍并且不表意，很多人会因此而迷惑，所以，使用 DI 来精确指名这个模式比较好。

### 2.2.3 什么是AOP

AOP：面向切面编程，AOP是OOP（面向对象编程）的一种延续。

来看一个OOP的例子。

例如：有三个类，Horse、Pig、Dog，这三个类中豆油eat和run两个方法。

通过OOP思想中的继承，我们可以提取出一个Animal的父类，然后将eat和run方法放入父类中，Horse、Pis、Dog通过继承Animal类即可以自动获得eat和run方法，这样将会少些很多重复的代码。

![image-20221005100231158](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005100231158.png)

OOP编程思想可以解决大部分的代码重复问题。但是有一些问题是处理不了的。比如在父类Animal中的很多个方法的相同位置中出现了重复的代码，OOP就解决不了。

```java
/**
 * 动物父类
 */
public class Animal {

    /** 身高 */
    private String height;

    /** 体重 */
    private double weight;

    public void eat() {
        // 性能监控代码
        long start = System.currentTimeMillis();

        // 业务逻辑代码
        System.out.println("I can eat...");

        // 性能监控代码
        System.out.println("执行时长：" + (System.currentTimeMillis() - start)/1000f + "s");
    }

    public void run() {
        // 性能监控代码
        long start = System.currentTimeMillis();

        // 业务逻辑代码
        System.out.println("I can run...");

        // 性能监控代码
        System.out.println("执行时长：" + (System.currentTimeMillis() - start)/1000f + "s");
    }
}
```

这部分重复的代码，一般统称为横切逻辑代码
![image-20221005100524700](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005100524700.png)

横切逻辑代码存在的问题：

1. 代码重复问题
2. 横切逻辑代码和业务代码混在在一起，代码臃肿，不便维护

AOP就是用来解决这些问题的

AOP另辟蹊径，提出横向抽取机制，将横切逻辑代码和业务逻辑代码分离
![image-20221005100741316](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005100741316.png)

代码拆分比较容易，难的是如何在不改变原有业务逻辑的情况下，悄无声息的将横向逻辑代码应用到原有的业务逻辑中，达到和原来一样的效果。

### 2.2.4 AOP解决了什么问题

通过上面的分析可以发现，AOP主要用来解决：在不改变原有业务逻辑的情况下，增强横切逻辑代码，根本上解耦合，避免横切逻辑代码重复。

### 2.2.5 AOP为什么叫做面向切面编程

1. 切：指的是横切逻辑，原有逻辑代码不动，只能操作横切逻辑代码，所以面向横切逻辑
2. 面：横切逻辑代码往往要影响的是很多个方法，每个方法如同一个点，多个点构成一个面，这里有一个面的概念。

## 2.3 什么是Spring Bean

简单来说，Bean代指的是那些被IoC容器所管理的对象。

我们告诉IoC容器帮助我们管理那些对象，这个是通过配置元数据来定义的。配置元数据可以是XML文件、注解或Java配置类。

```html
<!-- Constructor-arg with 'value' attribute --> 
<bean id="..." class="...">
  <constructor-arg value="..."/> 
</bean>
```

下面简单的展示了IoC容器如何使用配置元数据来管理对象：
![image-20221005102114215](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005102114215.png)

org.springframework.beans 和      org.springframework.context 这两个包是 IoC 实现的基础，如果想要研究 IoC 相关的源码的话，可以去看看

## 2.4 将一个类声明为Bean的注解有那些

1. @Component：通过注解，可以标注任意类为Spring组件。如果一个Bean不知道属于那个层次，可以使用@Component注解标注
2. @Repository：对应持久层即Dao层，主要用于数据库相关操作
3. @Service：对应服务层，主要涉及一些复杂的逻辑，需要用到Dao层
4. @Controller：对应Spring MVC控制层，主要用于接受用户请求并调用Service层返回数据给前端页面

## 2.5 @Component和@Bean的区别是什么

1. @Component注解作用于类，而@Bean注解作用于方法
2. @Component通常是通过类路径扫描来自动侦测以及自动装配到Spring容器中（我们可以使用@ComponentScan注解定义要扫描的路径，从中找出标识了需要装配的类自动装配到Spring的bean容器中）。
   @Bean注解通常是我们在标有该注解的方法中定义产生这个bean，@Bean告诉了Spring这是某个类的实例，当我们需要用它的时候还给我。
3. @Bean注解比@Component注解的自定义性更强，而且很多地方我们只能通过@Bean注解来注册bean。比如当我们引用第三方库中的类需要装配到Spring容器时，只能通过@Bean来实现

@Bean注解使用示例：

```java
@Configuration
public class AppConfig { 
    @Bean
    public TransferService transferService() { 
        return new TransferServiceImpl();
    } 
}
```

上面的代码相当于下面的xml配置

```xml
<beans>
   <bean id="transferService"
class="com.acme.TransferServiceImpl"/> 
</beans>
```

下面这个例子是通过@Component无法实现的：

```java
@Bean
public OneService getService(status) { 
   case (status)  {
       when 1:
               return new serviceImpl1(); 
       when 2:
               return new serviceImpl2(); 
       when 3:
               return new serviceImpl3(); 
   }
}
```

## 2.6 注入Bean的注解有哪些

Spring内置的@Autowired以及JDK内置的@Resource和@Injet都可以用于注入Bean。

![image-20221005103911983](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005103911983.png)

@Autowired和@Resource使用的比较多一些。

## 2.7 @Autowired和@Resource的区别是什么

@Autowired属于Spring内置的注解，默认的注入方式为byType（根据类型进行匹配），也就是说会优先根据接口去匹配并注入Bean（接口的实现类）。

这会有什么问题呢？当一个接口存在多个实现类的话，byType这种方式就无法正确注入对象了，因为这个时候Spring会同时找到多个满足条件的选择，默认情况下他自己不知道选择哪一个。

示例：

```java
public interface SmsService{}
```

```java
@Component	//该注解创建的bean的id默认为smsServiceImpl1
public class SmsServiceImpl1 implements SmsService{}
```

```java
@Component	//该注解创建的bean的id默认为smsServiceImpl2
public class SmsServiceImpl2 implements SmsService{}
```

这种情况下，注入方式自动会变为byName（根据名称进行匹配），这个名称通常就是类名（首字母小写）。就比如下面代码中的smsService就是我这里说的名称：

```java
// smsService 就是我们上⾯所说的名称
@Autowired
private SmsService smsService;
```

举个例子，SmsService接口有两个实现类：SmsServiceImpl1和SmsServiceImpl2，其他们都已经被Spring容器所管理。

```java
// 报错，byName 和 byType 都⽆法匹配到 bean
@Autowired
private SmsService smsService;

// 正确注⼊ SmsServiceImpl1 对象对应的 bean
@Autowired
private SmsService smsServiceImpl1;

// 正确注⼊  SmsServiceImpl1 对象对应的 bean 
// smsServiceImpl1 就是我们上⾯所说的名称
@Autowired
@Qualifier(value = "smsServiceImpl1") 
private SmsService smsService;
```

我们还是建议通过@Qualifier注解来显示指定名称而不是依赖变量的名称。

@Resource属于JDK提供的注解，默认注入方式为byName，如果无法通过名称匹配到对应的Bean的话，注入方式会变为byType。

@Resource有两个比较重要且日常开发常用的属性：name（名称）、type（类型）。

```java
public @interface Resource { 
   String name() default "";
   Class<?> type() default Object.class; 
}
```

如果禁止顶name属性，则称注入方式为byName，如果仅指定type属性则注入方式为byType，如果同时指定name和type属性（不建议这么做）则注入方式为byType+byName。

```java
// 报错，byName 和 byType 都⽆法匹配到 bean
@Resource
private SmsService smsService;
// 正确注⼊ SmsServiceImpl1 对象对应的 bean
@Resource
private SmsService smsServiceImpl1;
// 正确注⼊ SmsServiceImpl1 对象对应的 bean（⽐较推荐这种⽅式） 
@Resource(name = "smsServiceImpl1")
private SmsService smsService;
```

简单总结一下：

1. @Autowired是Spring提供的注解，@Resource是JDK提供的注解
2. @Autowired默认的注入方式为byType（根据类型匹配），@Resource默认的注入方式为byName（根据名称匹配）
3. 当一个接口存在多个实现类的情况下，@Autowired和@Resource都需要通过名称才能匹配到正确的Bean。@Autowired可以通过@Qualifier注解来显示指定名称，@Resource可以通过name属性来显示指定名称。



## 2.8 Bean的作用域有那些

Spring中Bean的作用域通常有下面几种：

1. singleton：IoC容器中只唯一的bean示例。Spring中的bean默认都是单例的，是对单例设计模式的应用。
2. prototyperequest：每次获取都会创建一个新的bean实例。也就是说，连接getBean两次，得到的是不同的Bean实例。
3. request（仅Web应用可用）：每一次HTTP请求都会产生一个新的bean（请求bean），该bean仅在但钱HTTP request内有效
4. session（仅Web应用可用）：每一次来自新的session的HTTP请求产生一个新的bean（会话bean），该bean仅在当前HTTP session内有效
5. application/global-session（仅Web应用可用）：每个Web应用在启动时创建一个Bean（应用Bean），该bean仅在当前应用启动时间内有效
6. websocket（仅Web应用可用）：每一次WebSocket会话产生一个新的bean。

如何配置bean的作用域呢？

1. xml方式：

   ```xml
   <bean id="..." class="..." scope="singleton"></bean>
   ```

2. 注解方式：

   ```java
   @Bean
   @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE) 
   public Person personPrototype() {
      return new Person(); 
   }
   ```

## 2.9 单例Bean的线程安全问题

大部分时候我们并没有在项目中使用多线程，所以很少会有人去关注这个问题。单例Bean存在线程问题，主要是因为当多个线程操作同一个对象的时候是存在资源进程的

常见的有两种解决方法：

1. 在Bean中尽量避免定义可变的成员变量
2. 在类中定义一个ThreadLocal成员变量，将需要的可变成员变量保存在ThreadLocal中（推荐的一种方式）

不过大部分Bean实际都是无状态（没有实例变量）的（比如：Dao、Service），这种情况下，Bean是线程安全的。

## 2.10 Bean的生命周期

1. Bean容器找到配置文件中Spring Bean的定义
2. Bean容器利用Java Reflection API创建一个Bean的实例
3. 如果涉及到一些属性值利用set()方法设置一些属性值
4. 如果Bean实现了BeanNameAware接口，调用setBeanName方法，传入Bean的名字
5. 如果Bean实现了BeanClassLoaderAware接口，调用setBeanClassLoader()方法，传入ClassLoader对象的实例
6. 如果Bean实现了BeanFactoryAware接口，调用setBeanFactory方法，传入BeanFactory对象的实例
7. 与上⾯的类似，如果实现了其他      *.Aware 接⼝，就调⽤相应的⽅法。
8. 如果有和加载这个 Bean 的 Spring 容器相关的      BeanPostProcessor 对象，执 
   ⾏    postProcessBeforeInitialization() ⽅法
9. 如果 Bean 实现了    InitializingBean 接⼝，执⾏afterPropertiesSet() ⽅法。 
10. 如果 Bean 在配置⽂件中的定义包含 init-method 属性，执⾏指定的⽅法。 
11. 如果有和加载这个 Bean 的 Spring 容器相关的BeanPostProcessor 对象，执⾏postProcessAfterInitialization() ⽅法
12. 当要销毁 Bean 的时候，如果 Bean 实现了DisposableBean 接⼝，执⾏destroy() ⽅法。
13. 当要销毁 Bean 的时候，如果 Bean 在配置⽂件中的定义包含destroy-method 属性，执⾏指定的⽅法。

图示：
![image-20221005134013246](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005134013246.png)





# 3、Spring AoP

## 3.1 谈谈你对AOP的了解

AOP能够将那些与业务无关，却为业务模块所共同调用的逻辑或责任（例如：事务处理、日志管理、权限控制等）封装起来，便于减少系统的重复代码，降低模块间的耦合度，并有利于未来的可拓展性和可维护性。

Spring AOP就是基于是动态代理的，如果要代理的对象，实现了某个接口，那么Spring AOP会使用JDK Proxy，去创建代理对象，而对于没有实现接口的对象，就无法使用JDK Proxy去进行代理了，这时候Spring AOP会使用Cglib生成一个被代理对象的字类来作为代理，如下图所示：

![image-20221005192816285](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005192816285.png)

当然你也可以使用AspectJ！Spring AOP中已经继承了AspectJ，AspectJ应该算得上是Java生态系统中最完整的AOP框架了。

AOP切面变成设计到的一些专业术语：

| 术语                | 含义                                                         |
| ------------------- | ------------------------------------------------------------ |
| 目标（Target）      | 被通知的对象                                                 |
| 代理（Proxy）       | 向目标对象应用通知之后创建的代理对象                         |
| 连接点（JoinPoint） | 目标对象的所属类中，定义的所有方法均为连接点                 |
| 切入点（Pointcut）  | 被切面拦截/增强的连接点（切入点一定是连接点，连接点不一定是切入点） |
| 通知（Advice）      | 增强的逻辑/代码，也即拦截到目标对象的连接点之后要做的事情    |
| 切面（AspectJ）     | 切入点（Pointcut）+通知（Advice）                            |
| Weaving（织入）     | 将通知应用到目标对象，进而生成代理对象的过程动作             |



## 3.2 SpringAOP和AspectJ AOP有什么区别？

Spring AOP属于运行时增强，而AspectJ是编译时增强。Spring AOP基于代理（Proxying），而AspectJ基于字节码操作（Bytecode Mainipulation）。

Spring AOP已经集成了AspectJ，AspectJ应该算上是Java生态系统中最完整的AOP框架了。AspectJ相比于Spring AOP功能更加强大，但是Spring AOP相对来说更加简单。

如果我们的切面比较少，那么两者的性能差异并不大；但是当切面太多的话，最好选择ApspectJ，它比Spring AOP快很多。

## 3.3 AspectJ定义的通知类型有那些？

1. Before（前置通知）：目标对象的方法调用之前触发
2. After（后置通知）：目标对象的方法调用之后触发
3. AfterReturning（返回通知）：目标对象的方法调用完成，在返回结果值之后触发
4. AfterThrowing（异常通知）：目标对象的方法运行中抛出 / 触发异常后触发。AfterReturning 和 AfterThrowing两者互斥。如果方法调用成功无异常，则会有返回值；如果方法抛出了异常，则不会有返回值
5. Around（环绕通知）：编程式控制目标对象的方法调用。环绕通知是所有通知类型中可操作范围最大的一种，因为它可以直接拿到目标对象，以及要执行的方法，所以环绕通知可以任意的在目标对象的方法调用前后搞事，甚至不调用目标对象的方法

## 3.4 多个切面的执行顺序如何控制？

1. 通常使用@Order注解直接定义切面顺序

   ```java
   // 值越⼩优先级越⾼ 
   @Order(3)
   @Component 
   @Aspect
   public class LoggingAspect implements Ordered {}
   ```

2. 实现Ordered接口重写getOrder方法

   ```java
   @Component 
   @Aspect
   public class LoggingAspect implements Ordered { 
      // ....
      @Override
      public int getOrder() { 
          // 返回值越⼩优先级越⾼ 
          return 1;
      } 
   }
   ```

   





# 4、Spring MVC

## 4.1 说说自己对于Spring MVC的了解？

MVC是模型（Model）、视图（View）、控制器（Controller）的缩写，其核心思想是通过将业务逻辑、数据、显示分离来组织代码。

![image-20221005201116775](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005201116775.png)

网上有很多上说MVC不是设计模式，只是软件设计规范，作者认为MVC同样是众多设计模式中的一种。

要想真正理解Spring MVC，我们先来看看Model1和Model2这两个没有Spring MVC的时代。

### 4.1.1Model1时代

很多学Java后端比较晚的同学可能没有接触过Model1时代下的javaWeb应用开发。在Model1模式下，整个Web应用几乎全部用JSP页面组成，只用少量的javaBean来处理数据库连接、访问等操作。

这个模式下JSP即是控制层（Controller）又是表现层（View）。显然，这种模式存在很多问题。比如控制逻辑和表现逻辑混在在一起，导致代码重用率较低；再比如前端和后端相互依赖，难以进行测试维护并且开发效率低。
![image-20221005201547056](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005201547056.png)



### 4.1.2 Model2时代

学过Servlet并做过相关Demo的朋友应该了解“Java Bean（Model） + JSP（View） + Servlet（Controller）”这种开发模式，这就是早期的JavaWeb MVC开发模式。

1. Model：系统涉及的数据，也就是dao和bean
2. View：展示模型中的数据，只是用来展示
3. Controller：处理用户请求都发送给它，返回数据给JSP并展示给用户

![image-20221005201850997](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005201850997.png)

Model2模式下还存在很多问题，Model2的抽象和封装成都还是远远不够，使用Model2进行开始时不可避免地重复造轮子，这就大大降低了程序地可维护性和复用性；于是很多JavaWeb开发相关地MVC框架应运而生比如Struts2，但是比较笨重

### 4.1.3 Spring MVC时代

随着Spring轻量级开发框架地流行，Spring生态圈出现了Spring MVC框架，Spring MVC是当前最优秀地MVC框架。相比于Structs2，Spring MVC使用更加简单和方便，开发效率更高，并且Spring MVC运行速度更快。

MVC是一种设计模式，Spring MVC是一种很优秀地MVC框架，Spring MVC可以帮助我们进行更加简介的Web层的开发，并且他天生与Spring框架集成。Spring MVC下我们一般把后端项目分为Service层（处理业务）、Dao层（数据库操作）、Entity层（实体类）、Controller层（控制层，返回数据给前台页面）。

## 4.2 Spring MVC的核心组件有那些？

1. **DispatcherServlet**：核心的中央处理器，负责接收请求、分发，并给与客户端响应
2. **HandlerMapping**：处理器映射器，根据URI去匹配查找能处理的Handler，并会将请求涉及到的拦截器和Handler一起封装
3. **HandlerAdapter**：处理器适配器，根据HandlerMapping找到Handler，适配执行对应的Handler
4. **Handler**：请求处理器，处理实际请求的处理器
5. **ViewResolver**：视图解析器，根据Handler返回的逻辑视图/视图，解析并渲染真正的视图，并传递给DispactherServlet响应客户端

## 4.3 Spring MVC工作原理

![image-20221005223001094](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005223001094.png)

流程说明：

1. 客户端（浏览器）发送请求，DispatcherServlet拦截请求
2. DispatcherServlet根据请求信息调用HandlerMapping。HandlerMapping根据URI去匹配查找能处理的Handler（也就是我们常说的Controller控制器），并会将请求涉及到的拦截器和Handler一起封装
3. DispatcherServlet调用HandlerAdapter适配器执行Handler
4. Handler完成对用户请求的处理后，会返回一个ModelAndView对象给DispatcherServlet，ModerAndView顾名思义，包含了数据模型以及相应的视图信息。Model是返回的数据对象，View是个逻辑上的View
5. ViewResolver会根据逻辑View查找实际的View
6. DispaterServlet把返回的Mode传给View（视图渲染）
7. 把View返回给请求者（浏览器）

## 4.4 统一异常处理怎么做？

推荐使用注解的方式统一异常处理，具体会使用到@ControllerAdive + @ExceptionHandler这两个注解

```java
@ControllerAdvice 
@ResponseBody
public class GlobalExceptionHandler {
   @ExceptionHandler(BaseException.class)
   public ResponseEntity<?> handleAppException(BaseException 
ex, HttpServletRequest request) {
     //......
   }
   @ExceptionHandler(value =
ResourceNotFoundException.class)
   public ResponseEntity<ErrorReponse>
handleResourceNotFoundException(ResourceNotFoundException ex,HttpServletRequest request) {
     //...... 
   }
}
```

这种异常处理方式下，会给所有或者指定的Controller织入异常处理的逻辑（AOP），当Controller中的方法抛出异常的时候，由被@ExceptionHandler注解修饰的方法进行处理。

ExceptionHandlerMethodResolver中getMapperMethod方法决定了异常具体被哪个被@ExceptionHandler注解修饰的方法处理异常。

```java
@Nullable
private Method getMappedMethod(Class<? extends Throwable> exceptionType) {
    List<Class<? extends Throwable>> matches = new 
        ArrayList<>();
    //找到可以处理的所有异常信息。mappedMethods 中存放了异常和处理异常的⽅法的对应关系
    for (Class<? extends Throwable> mappedException : this.mappedMethods.keySet()) {
        if (mappedException.isAssignableFrom(exceptionType)) {
            matches.add(mappedException); 
        }
    }
    // 不为空说明有⽅法处理异常
    if (!matches.isEmpty()) { 
        // 按照匹配程度从⼩到⼤排序
        matches.sort(new ExceptionDepthComparator(exceptionType)); 
        // 返回处理异常的⽅法
        return this.mappedMethods.get(matches.get(0)); 
    }
    else {
        return null; 
    }
}
```

从源代码中可以看出：getMappedMethod方法会首先找到可以匹配处理异常的所有方法信息，然后对其进行从小到大的排序，最后取最小的哪一个匹配的方法（即匹配度最高的哪个）。

# 5、Spring 框架中用到了那些设计模式

1. *<u>**工厂设计模式：<font color = "red">属于创建型模式 + 类模式</font>**</u>*Spring使用工厂模式通过BeanFactory、AppliactionContext创建bean对象

   两者对比：

   1. BeanFactory：延迟注入（使用到某个bean的时候才会注入），相比于BeanFactory来说会占用更少的内容，程序启动速度更快
   2. ApplicationContext：容器启动的时候，不管你有没有用到，一次性创建所有的bean。BeanFactory仅提供了最基本的依赖注入支持，ApplicationContext扩展了BeanFactory，除了有BeanFactory的功能还有额外的更过功能，所以，一般开发人员使用ApplicationContext会更多。
      1. ClassPathXmlApplication：把上下文文件当作类路径资源
      2. FileSystemXmlApplication：从文件系统中的XML文件载入上下文定义信息
      3. XmlWebApplicationContext：从Web系统的XML文件载入上下文定义信息

2. <u>***单例设计模式：<font color = "red">属于创建型模式 + 对象模式</font>***</u>Spring中的Bean默认都是单例的
   使用单例模式的好处：

   1. 对于频繁使用的对象，可以省略创建对象所花费的时间，这对那些重量级对象而言，是非常可观的一笔系统开销
   2. 由于new操作的次数减少，因而对于系统内存的使用频率也会降低，这将减轻GC压力，缩短GC停顿时间

3. <u>***适配器模式：<font color = "red">属于结构型模式 + 类模式 / 对象模式</font>***</u>Spring AOP的增强或通知（Advice）使用到了适配器模式、Spring MVC中也是用到了适配器模式适配Controller；***适配器别名为包装器***。

   适配器模式将一个接口转换为用户希望的另一个接口，适配器模式使接口不兼容的那些类可以一起工作，其别名为包装器。

   1. **Spring AOP中的适配器模式**
      我们知道Spring AOP的实现是基于代理模式，但是Spring AOP的增强通知使用到了适配器模式，与之相关的接口是AdvisorAdapter。
      Advice常用的类型有：

      1. BeforeAdvice（目标方法调用前，前置通知）
      2. AfterAdvice（目标方法调用后，后置通知）
      3. AfterReturnAdvice（目标方法执行结束后，return之前）等等。

      每个类型Advice（通知）都有对应的拦截器：

      1. MethodBeforeAdviceInterceptor
      2. AfterReturningAdviceAdapter
      3. AfterReturningAdviceInterceptor

      Spring预定义的通知要通过对应的适配器，适配成MethodInterceptor接口（方法拦截器）类型的对象（如：MethodBeforeAdviceInterceptor负责适配MethodBeforeAdvice）

   2. Spring MVC中的适配器模式

      具体解释可以去看：[补1-谈谈Spring用到了哪些设计模式？](D:\c_51CTO资料\Java\石头老师Java工程师养成之路（部分）\Java互联网架构师-SpringMvc核心技术实战\补1-谈谈Spring用到了哪些设计模式？.md)

      在Spring MVC中，DispatcherServlet根据请求信息调用HandlerMapping，解析请求对应的Handler。解析到对应的Handler（也就是我们平时说的Controller控制器）后，开始由HandlerAdapter适配器处理。HadnlerAdapter作为期望接口，具体的适配器实现类对目标类进行适配，Controller作为需要适配的类。

      为什么要在Spring MVC中使用适配器模式？Spring MVC中的Controller终类众多，不同类型的Controller通过不同的方法来对于请求进行处理。如果不利用适配器模式的话，DispatcherServlet直接获取对应类型的Controller，需要自行来判断，像下面的代码一样：

      ```java
      if(mappedHandler.getHandler() instanceof MultiActionController){  
          ((MultiActionController)mappedHandler.getHandler()).xxx  
      }else if(mappedHandler.getHandler() instanceof XXX){  
          ...  
      }else if(...){  
          ...  
      }  
      ```

      假如我们再增加一个Controller类型就要在上面代码中再假如一行判断语句，这种形式就使得程序难以维护，也违反了设计模式中的开闭原则—对扩展开放，对修改关闭。

4. <u>***装饰者器设计模式：<font color = "red">属于结构型模式 + 对象模式</font>***</u>我们的项目需要连接多个数据库，而且不同的客户在每次访问中根据需要会去访问不同的数据库。这种模式让我们可以根据客户的需求能够动态切换不同的数据源

   装饰器模式可以动态给对象添加一些额外的属性或行为，相比于使用继承，装饰者模式更加灵活。简单点儿说就是当我们需要修改原有的功能，但是我们又不愿意直接去修改原有的代码时，设计以恶搞装饰器套在原有代码外面。其实在JDK中有很多地方用到了装饰器模式，比如InputStream家族，InputStream类下有FileInputStream（读取文件）、BufferedInputStream（增加缓存，使读取文件速度大大提升）等子类都在不修改InputStream代码的情况下扩展了它的功能。
   ![image-20221006172245439](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221006172245439.png)

   Spring配置DataSource的时候，DataSource可能是不同的数据库和数据员，我们能否根据客户的需求在少修改原有类的代码下动态切换不同的数据源？这个时候就需要用到装饰着模式。Spring中用到的包装器模式在类名上含有Wrapper或Decorator。这些基本上都是动态的给一个对象添加一些额外的职责。

   

5. <u>***代理设计模式：<font color = "red">属于结构模式 + 对象模式</font>***</u>Spring AOP功能的实现
   略，看第3小节

6. <u>***模板方法模式：<font color = "red">属于行为型模式 + 类模式</font>***</u>Spring中jdbcTemplate、hibernateTemplate等以Template结尾的对数据库操作的类，它们就是用到了模板模式

   模板方法模式是一种行为设计模式，它定义一个操作中的算法的骨架，而将一些步骤延迟到子类中。模板方法使得子类可以不改变一个算法的结构即可重定义该算法的某些特定步骤的实现方式。
   ![image-20221005233557214](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221005233557214.png)
   示例代码：

   ```java
   public abstract class Template {
       //这是我们的模板方法
       public final void TemplateMethod(){
           PrimitiveOperation1();  
           PrimitiveOperation2();
           PrimitiveOperation3();
       }
   
       protected void  PrimitiveOperation1(){
           //当前类实现
       }
   
       //被子类实现的方法
       protected abstract void PrimitiveOperation2();
       protected abstract void PrimitiveOperation3();
   
   }
   public class TemplateImpl extends Template {
   
       @Override
       public void PrimitiveOperation2() {
           //当前类实现
       }
   
       @Override
       public void PrimitiveOperation3() {
           //当前类实现
       }
   }
   ```

   Spring中的jdbcTemplate、hibarnateTemplate等以Template结尾的对数据库操作的类，它们就使用到了模板模式。一般情况下我们都是使用继承的方式来实现模板模式，但是Spring中没有使用这种方式，而是使用Callback模式与模板方法模式配合，即达到了代码复用的效果，同时增加了灵活性。

7. <u>***观察者模式：<font color = "red">行为型模式 + 对象模式</font>***</u>Spring事前驱动模型就是观察者模型很经典的一个应用

   观察者模式是一种对象行为型模式，它表示的是一种对象与对象之间有依赖关系，当一个对象发生改变的时候，这个对象所以来的对象也会作出反应。Spring事件驱动模型就是观察者模式很景点的一个应用。Spring事件驱动模型非常有用，在很多场景都可以解耦我们的代码。比如我们每次添加商品的时候都需要重新更新商品索引，这个时候就可以利用观察者模式来解决这个问题。

   Spring事件驱动模型中的三种角色：

   1. 事件角色
   2. 事件监听者角色
   3. 时间发布者角色

   Spring的事件流程总结



# 6、Spring事务

## 6.1 Spring 管理事务的方式有几种？

1. 编程式事务：在代码中硬编码（不推荐），通过TransactionTemplate或TransactionManager手动管理事务，实际应用中很少使用，但是对于你理解Spring事务管理原理有帮助
2. 声明式事务：在XML配置文件中配置或直接基于注解（推荐使用）实际是通过AOP实现（基于@Transactional的全注解方式使用最多）

## 6.2 Spring事务中哪几种事务传播行为

事务传播行为是为了解决业务层方法之间相互调用的事务问题。

当事务方法被另一个事务调用时，必须指定事务应该如何传播。例如：方法可能继续在现有事务中运行，也可能开启一个新事物，并在自己的事务中运行。

正确的事务传播行为可能的值如下：

1. TransactionDefinition.PROPAGATION_REQUIRED：使用的最多的一个事务传播行为，我们平时经常使用的@Transactional注解默认使用就是这个事务传播行为。如果当前存在事务，则假如该事务；如果当前没有食物，则创建一个新的事务
2. TransactionDefinition.PROPAGATION_REQUIRES_NEW：创建一个新的事务，如果当前存在事务，则把当前事务挂起。也就是说不管外部方法是否开启事务，Propagation.REQUIRES_NEW修饰内部方法会新开启自己的事务，且事务相互独立，互不干扰
3. TransactionDefinition.PROPAGATION_NESTED：如果当前存在事务，则创建一个事务作为当前事务的嵌套事务来运行；如果当前没有事务，则该取值等价于TransactionDefinition.PROPAGATION_REQUIRED。
4. TransactionDefinition.PROPAGATION_MANDATORY：如果当前存在事务，则加入该事务；如果当前没有事务，则抛出异常（mandatory：强制性），这个使用的很少

若是错误的配置以下三种事务传播行为，事务将不会发生回滚：

1. TransactionDefinition.PROPAGATION_SUPPORTS：如果当前存在事务，则加入该事务；如果当前没有事务，则以非事务的方式继续运行
2. TransactionDefinition.PROPAGATION_NOT_SUPPORTED：以非事务方式运行，如果当前存在事务，则把当前事务挂起
3. TransactionDefinition.PROPAGATION_NEVER：以非事务方式运行，如果当前存在事务，则抛出异常

## 6.3 Spring事务中的隔离级别有哪几种？

和事务传播行为这一块，为了方便使用，Spring也相应的定义了一个枚举类：Isolation

```java
public enum Isolation {
    DEFAULT(TransactionDefinition.ISOLATION_DEFAULT),
    READ_UNCOMMITTED(TransactionDefinition.ISOLATION_READ_UNCOMMITTED),

 READ_COMMITTED(TransactionDefinition.ISOLATION_READ_COMMITTED),

 REPEATABLE_READ(TransactionDefinition.ISOLATION_REPEATABLE_READ),
    SERIALIZABLE(TransactionDefinition.ISOLATION_SERIALIZABLE); 

    private final int value;
    Isolation(int value) { 
        this.value = value; 
    }
    public int value() { 
        return this.value; 
    }
}
```

下面我一次对每一种事务隔离级别进行介绍：

1. TransactionDefinition.ISOLATION_DEFAULT：使用后端数据库默认的隔离级别，MySQL默认采用的是REPEATABLE_READ隔离级别，Oracle默认采用的是READ_COMMITTED隔离级别
2. TransactionDefinition.ISOLATION_READ_UNCOMMITTED：最低的隔离级别，使用这个隔离级别的很少，因为它允许读尚未提交的数据变更，可能会导致脏读、幻读、不可重复读
3. TransactionDefinition.ISOLATION_READ_COMMITTED：允许读取并发事务以及提交的数据，可以阻止脏读，但是幻读、不可重复读仍然有可能发生
4. TransactionDefinition.ISOLATION_REPEATABLE_READ：对同一段的多次读取结果是一致的，除非数据是被本身事务自己所修改，可以阻止脏读、不可重复读，但幻读仍可能会发生
5. TransactionDefinition.ISOLATION_SERIALIZABLE：最高的隔离级别，完全服从ACID的隔离级别。所有的事务一次诸葛执行，这样事务之间就完全不可能产生干扰，也就是说，该隔离级别可以防止脏读、不可重复读、幻读。但是这将严重影响程序的性能。通常情况下也不会用到该级别。

## 6.4 @Transactional(rollbackFor=Exception.class)注解

Exception分为运行时异常RuntimeException和非运行时异常。事务管理对于企业应用来说是至关重要的，即使出现异常情况，它也可以保证数据的一致性。

当@Transactional注解作用于类上时，该类的所有public方法将具有该类型的事务属性；同时，我们也可以在方法级别使用该标注来覆盖类级别的定义。如果类或者方法加了这个注解，那么这个类里面的方法抛出异常，就会回滚，数据库里面的数据也会回滚。

在@Transaction注解中如果不配置rollbackFor属性，那么事务只会在遇到RuntimeException的时候才会回滚，加上rollback=Exception.class，可以让事务在遇到非运行时异常时也回滚。

# 7、Spring Data JPA

JPA重要的是实战，这里仅对小部分知识点进行总结

## 7.1、如何使用JAP在数据库中非持久化一个字段？

假设我们有下面一个类：

```java
@Entity(name="USER") 
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) 
    @Column(name = "ID")
    private Long id;
    @Column(name="USER_NAME") 
    private String userName;
    @Column(name="PASSWORD") 
    private String password;
    private String secrect; 
}
```

如果我们想让secrect这个字段不被持久化，也就是不被数据库存储怎么办？

可以采用下面几种方法：

1. static String transient1：不会对static字段持久化
2. final String transient2：不会对final持久化
3. transient String transient3：不会对被transient修饰的字段持久化
4. @Transient：不会对被@Transient注解注释的字段持久化

一般情况下使用注解使用后两种方式比较多。

## 7.2 JPA的审计功能是做什么的？有什么用？

审计功能主要是帮助我们记录数据库操作的具体行为，比如某条记录是谁创建的、什么时间创建、最后修改人是谁、最后修改时间是什么时候

```java
@Data
@AllArgsConstructor 
@NoArgsConstructor 
@MappedSuperclass
@EntityListeners(value = AuditingEntityListener.class) 
public abstract class AbstractAuditBase {
   @CreatedDate
   @Column(updatable = false) 
   @JsonIgnore
   private Instant createdAt; 
    
   @LastModifiedDate
   @JsonIgnore
   private Instant updatedAt; 
    
   @CreatedBy
   @Column(updatable = false) 
   @JsonIgnore
   private String createdBy; 
    
   @LastModifiedBy
   @JsonIgnore
   private String updatedBy;
}
```

1. @CreatedDate：表示该字段为创建时间字段，在这个实体被insert的时候会设置值。
2. @CreatedBy：表示该字段为创建人，在这个实体被insert的时候，会设置值
3. @LastModifiedDate、@LatsModifiedBy同理

## 7.3 实体之间的关联关系注解有那些？

1. @OneToOne：一对一
2. @ManyToMany：多对多
3. @OneToMany：一对多
4. @ManyToOne：多对一

利用@ManyToOne和@OneToMany也可以表达多对多的关联关系

# 8、Spring Security

Spring Security重要的是实战，这里仅对小部分知识点进行总结

## 8.1 有那些控制请求访问权限的方法？

![image-20221006194823304](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221006194823304.png)

hasRole 和 hasAuthority 有区别吗？
可以看看松哥的这篇⽂章：[Spring Security 中的 hasRole 和 hasAuthority 有区别吗？](https://mp.weixin.qq.com/s/GTNOa2k9_n_H0w24upClRw)



## 8.2 如何对密码进行加密

如果我们需要保存密码这类敏感数据到数据库的话，需要先加密再保存。

Spring Security提供了多种加密算法的实现，开箱即用，非常方便。这些加密算法实现类的父类是PasswordEncoder，如果你想要自己实现一个加密算法的话，也要继承PasswordEncoder。

PasswordEncoder接口也就三个必须实现的方法：

```java
public interface PasswordEncoder { 
   // 加密也就是对原始密码进⾏编码
   String encode(CharSequence var1); 
   // ⽐对原始密码和数据库中保存的密码
   boolean matches(CharSequence var1, String var2); 
   // 判断加密密码是否需要再次进⾏加密，默认返回 false
   default boolean upgradeEncoding(String encodedPassword) { 
       return false;
   } 
}
```

![image-20221006195244597](D:\Tyora\AssociatedPicturesInTheArticles\Spring常见面试题\image-20221006195244597.png)

官方推荐使用基于bcrypt强哈希函数的加密算法实现类。

## 8.3 如何优雅的更换系统使用的加密算法？

如果我们在开发过程中，突然发现有的加密算法无法满足我们的需求，需要更换成另一个加密算法，这个时候应该怎么办呢？

推荐的做法是通过DelegatingPasswordEncoder兼容多种不同的密码加密方案，以适应不同的业务需求。

从名字也能看出来，DelegatingPasswordEncoder是一个代理类，并非是一种全新的加密算法，他做的事情就是代理上面提到的加密算法实现类。在Spring Security5.0之后，默认就是基于DelegatingPasswordEncoder进行密码加密的。


















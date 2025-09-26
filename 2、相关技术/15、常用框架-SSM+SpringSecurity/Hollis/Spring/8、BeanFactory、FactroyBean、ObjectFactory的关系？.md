#BeanFactory是SpringIOC的一部分 #BeanFactory负责对象的关键与对象生命周期的管理 #AbstractAutowireCapableBeanFactory、ApplicationContext是BeanFactory的实现 #FactroyBean是对象工厂，复杂对象的创建可以交给它，可以用来创建代理对象 #Dubbo中ReferenceBean是FactoryBean的实现 

# 典型回答

`FactoryBean` 和 `BeanFactory` 是Spring中的两个重要的概念。先看一下他们的类定义：

FactoryBean：
```java
package org.springframework.beans.factory;

public interface FactoryBean<T> {

  T getObject() throws Exception;
  
  Class<?> getObjectType();
  
  boolean isSingleton();
}
```

BeanFactory：
```java
package org.springframework.beans.factory;

public interface BeanFactory {
Object getBean(String name) throws BeansException;

  <T> T getBean(String name, Class<T> requiredType) throws BeansException;

  Object getBean(String name, Object... args) throws BeansException;

  <T> T getBean(Class<T> requiredType) throws BeansException;

  <T> T getBean(Class<T> requiredType, Object... args) throws BeansException;

  boolean containsBean(String name);

  boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

  boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

  // ...
}
```

至少从代码上来看，这两个东西**都是接口**（interface)，然后**都是在`org.springframework.beans.factory`包下**面的。

网上有很多概念的解释，说明他俩的区别，但是很多人还是看不懂，下面是的解释我结合了具体的case，帮助大家更好地理解他们的作用，理解了各自的作用，那么区别自然也就理解了。

## BeanFactory

BeanFactory比较常用，名字也比较容易理解，就是<font color="red" size=5>Bean工厂</font>，他是整个 **==Spring IoC容器的一部分==，负责==管理Bean的创建和生命周期==**。

其中提供了一系列方法，可以让我们获取到具体的Bean实例。你可能没有直接用过BeanFactory，但是你肯定用过或者见过：
```java
applicationContext.getBean(requiredType);

applicationContext.getBean(name);
```

以上就是我们经常用的，在Spring的上下文中 **通过bean名称** 或者 **类型** 获取bean的方式，而这里的**ApplicationContext，其实就是一种BeanFactory**。这里面调用的getBean方法，就是上面我们在BeanFactory中看到的方法。

所以，BeanFactory是Spring IoC容器的一个接口，用来获取Bean以及管理Bean的依赖注入和生命周期。

## FactoryBean

FactoryBean是一个接口，用于定义一个<font color="red" size=5>工厂Bean</font>，它可以**产生某种类型的对象**。当在Spring配置文件中定义一个Bean时，如果这个**Bean实现了FactoryBean接口，那么Spring容器不直接返回这个Bean实例，而是返回FactoryBean#getObject()方法所返回的对象**。

是不是还是听不懂？

那我给你举个具体的例子你就知道了。

Dubbo用过吧（没用过？那可能理解起来比较吃力，因为FactoryBean确实是在很多框架中用到的比较多，比如Kafka、dubbo等各种框架中都会用他来和Spring做集成）。

当我们想要在Dubbo中定义一个远程的提供者提供的的Bean的时候，可以用@DubboReference或者`<dubbo:reference>`

**而这两种定义方式的最终实现都是一个Dubbo中的ReferenceBean ，它负责创建并管理远程服务代理对象。而这个ReferenceBean就是一个FactoryBean的实现。**
```java
public class ReferenceBean<T> implements FactoryBean<T>,
        ApplicationContextAware, BeanClassLoaderAware, BeanNameAware, InitializingBean, DisposableBean {

}
```

> ReferenceBean的主要作用是**创建并配置Dubbo服务的代理对象**。这些代理对象**允许客户端像调用本地方法一样调用远程服务**。创建Dubbo服务代理通常涉及复杂的配置和初始化过程，包括网络通信设置、序列化配置等。通过ReferenceBean将这些复杂性封装起来，对于使用者来说，只需要通过简单的Spring配置即可使用服务。

ReferenceBean 实现了 FactoryBean 接口并实现了getObject方法。**在getObject()方法中，ReferenceBean会给要调用的服务创建一个动态代理对象。这个代理对象负责与远程服务进行通信，封装了网络调用的细节，使得远程方法调用对于开发者来说是透明的**。

通过 FactoryBean 实现，ReferenceBean 还可以**延迟创建代理对象直到真正需要时**，这样可以提升启动速度并减少资源消耗。此外，它还可以实现更复杂的加载策略和优化。

通过实现 FactoryBean，ReferenceBean 能够很好地与Spring框架集成。这意味着它可以利用Spring的依赖注入，生命周期管理等特性，并且能够被Spring容器所管理。

所以，**FactoryBean通常用于创建很复杂的对象**，比如需要通过某种特定的创建过程才能得到的对象。例如，创建与JNDI资源的连接或与代理对象的创建。就如我们的Dubbo中的ReferenceBean。

## ObjectFactory

`ObjectFactory` 是一个简单的 **==函数式接口==**，主要用于**延迟获取Bean实例**。
- **延迟加载**：只有在调用 `getObject()` 时才真正获取Bean实例
- **轻量级**：本身不参与Bean的生命周期管理
- **解决循环依赖**：Spring内部用它来解决构造函数循环依赖

|       | ObjectFactory    | FactoryBean   |
| ----- | ---------------- | ------------- |
| 本质    | 对象提供者（Provider）  | 对象工厂（Factory） |
| 复杂度   | 简单，单一方法          | 复杂，完整生命周期     |
| 使用时机  | **运行时获取、延迟获取**   | 容器启动时创建       |
| 适用场景  | 循环依赖、延迟加载、原型Bean | 复杂对象创建、第三方集成  |
| 与容器关系 | 客户端的工具类          | 容器的基础设施       |

# 扩展知识

## ApplicationContext使用

我们知道，ApplicationContext作为BeanFactory的具体实现，他可以在Spring的的上下文中获取Bean，那么什么时候可以用到它呢？

一般来说，我们的Bean都是有Spring自动注入的，不太需要我们自己从上下文中获取，但是想让Spring帮忙注入，有一个前提，那就是必须被注入的Bean和注入的Bean都交给Spring托管，简单点说就是要有@Service和@Autowire一起用。
```java
@Service
public class HollisTestService{

	@Autowired
  private HollisTestRepo hollisTestRepo;

}
```

这样才能把HollisTestRepo注入到HollisTestService中，但是有的时候，我们的HollisTestService如果不是Spring托管的呢，比如是自己new的，那就不行了。

比较典型的是当有的时候我们使用充血模型，需要在里面去查询或者操作数据库的时候，就需要在这个模型中获取到对应的bean。那么就需要ApplicationContext了。
```java
public class CaseModel {
	 CollectionCaseItemService collectionCaseItemService = SpringContextHolder.getBean(CollectionCaseItemService.class);
}
```

这里面SpringContextHolder的实现如下：
```java
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring上下文工具
 *
 * @author Hollis
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.applicationContext = applicationContext;
    }

    /**
     * 根据bean的名字获取Bean
     *
     * @param name
     * @return
     * @throws BeansException
     */
    public static Object getBean(String name) throws BeansException {
        return applicationContext.getBean(name);
    }

    /**
     * 根据bean的类型获取Bean
     *
     * @param requiredType
     * @param <T>
     * @return
     * @throws BeansException
     */
    public static <T> T getBean(Class<T> requiredType) throws BeansException {
        return applicationContext.getBean(requiredType);
    }
}
```

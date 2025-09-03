## 1.背景

随着`Spring Boot`的盛行，注解配置式开发受到了大家的青睐，从此告别了基于`Spring`开发的繁琐`XML`配置。这里先来提纲挈领的了解一下`Spring`内部对于配置注解的定义，如`@Component、@Configuration、@Bean、@Import`等注解，从功能上来讲，这些注解所负责的功能的确不相同，但是从本质上来讲，`Spring`内部都将其作为配置注解进行处理。

对于一个成熟的框架来讲，简单及多样化的配置是至关重要的，那么`Spring`也是如此，从`Spring`的配置发展过程来看，整体的配置方式从最初比较“原始”的阶段到现在非常“智能”的阶段，**这期间`Spring`做出的努力是非常巨大的，从XML到自动装配，从Spring到Spring Boot，从@Component到@Configuration以及@Conditional，Spring发展到今日，在越来越好用的同时，也为我们隐藏了诸多的细节，那么今天让我们一起探秘@Component与@Configuration。**

我们平时在Spring的开发工作中，基本都会使用配置注解，尤其以`@Componen`t及`@Configuration`为主，当然在Spring中还可以使用其他的注解来标注一个类为配置类，这是广义上的配置类概念，但是这里我们只讨论@Component和@Configuration，因为与我们的开发工作关联比较紧密，那么接下来我们先讨论下一个问题，就是 **@Component与@Configuration有什么区别?**

> **项目推荐**：基于SpringBoot2.x、SpringCloud和SpringCloudAlibaba企业级系统架构底层框架封装，解决业务开发时常见的非功能性需求，防止重复造轮子，方便业务快速开发和企业技术栈框架统一管理。引入组件化的思想实现高内聚低耦合并且高度可配置化，做到可插拔。严格控制包依赖和统一版本管理，做到最少化依赖。注重代码规范和注释，非常适合个人学习和企业使用
>
> **Github地址**：[github.com/plasticene/…](https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2Fplasticene%2Fplasticene-boot-starter-parent)
>
> **Gitee地址**：[gitee.com/plasticene3…](https://link.juejin.cn?target=https%3A%2F%2Fgitee.com%2Fplasticene3%2Fplasticene-boot-starter-parent)
>
> **微信公众号：Shepherd进阶笔记**

## 2.@Component与@Configuration使用

### 2.1注解定义

在讨论两者区别之前，先来看看两个注解的定义：

```less
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface Component {

  /**
   * The value may indicate a suggestion for a logical component name,
   * to be turned into a Spring bean in case of an autodetected component.
   * @return the suggested component name, if any (or empty String otherwise)
   */
  String value() default "";

}
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {

  @AliasFor(annotation = Component.class)
  String value() default "";

  boolean proxyBeanMethods() default true;

}
```

从定义来看， @Configuration 注解本质上还是 @Component，因此 @ComponentScan 能扫描到@Configuration 注解的类。

### 2.2注解使用

接下来看看两者在我们日常开发中的使用：以这两种注解来标注一个类为配置类

```less
@Configuration
public class AppConfig {
}

@Component
public class AppConfig {
}
```

上面的程序，Spring会将其认为配置类来做处理，但是这里有一个概念需要明确一下，就是在Spring中，对于配置类来讲，其实是有分类的，大体可以分为两类，**一类称为LITE模式，另一类称为FULL模式**，那么对应上面的注解，@Component就是LITE类型，@Configuration就是FULL类型，如何理解这两种配置类型呢？我们先来看这个程序。

当我们使用`@Component`实现配置类时：

```csharp
@Component
public class AppConfig {
  @Bean
  public Foo foo() {
    System.out.println("foo() invoked...");
    Foo foo = new Foo();
    System.out.println("foo() 方法的 foo hashcode: " + foo.hashCode());
    return foo;
  }

  @Bean
  public Eoo eoo() {
    System.out.println("eoo() invoked...");
    Foo foo = foo();
    System.out.println("eoo() 方法的 foo hashcode: "+ foo.hashCode());
    return new Eoo();
  }

  public static void main(String[] args) {
    AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(AppConfig.class);
  }
}
```

执行结果如下：

```scss
foo() invoked...
foo() 方法的 foo hashcode: 815992954
eoo() invoked...
foo() invoked...
foo() 方法的 foo hashcode: 868737467
eoo() 方法的 foo hashcode: 868737467
```

从结果可知，`foo()`方法执行了两次，一次是bean方法执行的，一次是`eoo()`调用执行的，所以两次生成的`foo`对象是不一样的。很符合大家的预期，但是当我们使用`@Configuration`标注配置类时，执行结果如下：

```scss
foo() invoked...
foo() 方法的 foo hashcode: 849373393
eoo() invoked...
eoo() 方法的 foo hashcode: 849373393
```

这里可以看到`foo()`方法只执行了一次，同时`eoo()`方法调用`foo()`生成的foo对象是同一个。这也就是`@Component`和`@Configuration`的区别现象展示，那么为什么会有这样的一个现象?我们来考虑一个问题，就是eoo()方法中调用了foo()方法，很明显这个foo()这个方法就是会形成一个新对象，假设我们调用的foo()方法不是原来的foo()方法，是不是就可能不会形成新对象？如果我们在调用foo()方法的时候去容器中获取一下foo这个Bean，是不是就可以达到这样的效果？那如何才能达到这样的效果呢？有一个方法，**代理**！换句话说，我们调用的eoo()和foo()方法，包括AppConfig都被Spring代理了，那么这里我们明白了@Component与@Configuration最根本的区别，那就是@Configuration标注的类会被Spring代理，其实这样描述不是非常严谨，更加准确的来说应该是如果一个类的BeanDefinition的Attribute中有Full配置属性，那么这个类就会被Spring代理

## 3.Spring如何实现FULL配置的代理

如果要明白这一点，那么还需要明确一个前提，就是Spring在什么时间将这些配置类转变成FULL模式或者LITE模式的，接下来我们就要介绍个人认为在Spring中非常重要的一个类，ConfigurationClassPostProcessor。

### 3.1ConfigurationClassPostProcessor是什么

首先来简单的看一下这个类的定义:

```java
public class ConfigurationClassPostProcessor implements 
                                              BeanDefinitionRegistryPostProcessor,
                                               PriorityOrdered, 
                                               ResourceLoaderAware, 
                                               BeanClassLoaderAware, 
                                               EnvironmentAware {}
```

由这个类定义可知这个类的类型为BeanDefinitionRegistryPostProcessor，以及实现了众多Spring内置的Aware接口，如果了解Beanfactory的后置处理器，那应该清楚ConfigurationClassPostProcessor的执行时机，当然不了解也没有问题，我们会在后面将整个流程阐述清楚，现在需要知道的是ConfigurationClassPostProcessor这个类是在什么时间被实例化的？

### 3.2ConfigurationClassPostProcessor在什么时间被实例化

要回答这个问题，需要先明确一个前提，那就是ConfigurationClassPostProcessor这个类对应的BeanDefinition在什么时间注册到Spring的容器中的，因为Spring的实例化比较特殊，主要是基于BeanDefinition来处理的，那么现在这个问题就可以转变为ConfigurationClassPostProcessor这个类是在什么时间被注册为一个Beandefinition的？这个可以在源代码中找到答案，具体其实就是在初始化这个Spring容器的时候。

```scss
new AnnotationConfigApplicationContext(ConfigClass.class)
  -> new AnnotatedBeanDefinitionReader(this);
    -> AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
      -> new RootBeanDefinition(ConfigurationClassPostProcessor.class);
        -> registerPostProcessor(BeanDefinitionRegistry registry, RootBeanDefinition definition, String beanName)
```

从这里可以看出，ConfigurationClassPostProcessor已经被注册为了一个BeanDefinition，上面我们讲了Spring是通过对BeanDefinition进行解析，处理，实例化，填充，初始化以及众多回调等等步骤才会形成一个Bean，那么现在ConfigurationClassPostProcessor既然已经形成了一个BeanDefinition。

### 3.3 @Component与@Configuration的实现区别

上面ConfigurationClassPostProcessor已经注册到BeanDefinition注册中心了，说明Spring会在某个时间点将其处理成一个Bean，那么具体的时间点就是在BeanFactory所有的后置处理器的处理过程。

```scss
AbstractApplicationContext
  -> refresh()
    -> invokeBeanFactoryPostProcessors(beanFactory);
      -> PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
```

这个处理BeanFactory的后置处理器的方法比较复杂，简单说来就是主要处理所有实现了BeanFactoryPostProcessor及BeanDefinitionRegistryPostProcessor的类，当然ConfigurationClassPostProcessor就是其中的一个，那么接下来我们看看实现的方法：

```csharp
csharp复制代码  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    int registryId = System.identityHashCode(registry);
    if (this.registriesPostProcessed.contains(registryId)) {
      throw new IllegalStateException(
          "postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
    }
    if (this.factoriesPostProcessed.contains(registryId)) {
      throw new IllegalStateException(
          "postProcessBeanFactory already called on this post-processor against " + registry);
    }
    this.registriesPostProcessed.add(registryId);

    processConfigBeanDefinitions(registry);
  }
  
  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    int factoryId = System.identityHashCode(beanFactory);
    if (this.factoriesPostProcessed.contains(factoryId)) {
      throw new IllegalStateException(
          "postProcessBeanFactory already called on this post-processor against " + beanFactory);
    }
    this.factoriesPostProcessed.add(factoryId);
    if (!this.registriesPostProcessed.contains(factoryId)) {
      // BeanDefinitionRegistryPostProcessor hook apparently not supported...
      // Simply call processConfigurationClasses lazily at this point then.
      processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
    }

    enhanceConfigurationClasses(beanFactory);
    beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
  }
```

这两个方法应该就是ConfigurationClassPostProcessor最为关键的了，我们在这里先简单的总结一下，第一个方法主要完成了内部类，@Component，@ComponentScan，@Bean，@Configuration，@Import等等注解的处理，然后生成对应的BeanDefinition，另一个方法就是对@Configuration使用CGLIB进行增强，那我们先来看Spring是在哪里区分配置的LITE模式和FULL模式？在第一个方法中有一个checkConfigurationClassCandidate方法：

```typescript
typescript复制代码public static boolean checkConfigurationClassCandidate(
      BeanDefinition beanDef, MetadataReaderFactory metadataReaderFactory) {
      
        // ...
        
      Map<String, Object> config = metadata.getAnnotationAttributes(Configuration.class.getName());
        if (config != null && !Boolean.FALSE.equals(config.get("proxyBeanMethods"))) {
          beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
        }
        else if (config != null || isConfigurationCandidate(metadata)) {
          beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_LITE);
        }
        else {
          return false;
        }
        // ...      
      }
```

根据程序的判断可知，如果一个类被@Configuration标注且代理模式为true，那么这个类对应的BeanDefinition将会被Spring添加一个FULL配置模式的属性，有些同学可能对这个”属性“不太理解，这里可以简单说一下，其实这个”属性“在Spring中有一个特定的接口就是AttributeAccessor，BeanDefinition就是继承了这个接口，如何理解这个AttributeAccessor呢？其实也很简单，想想看，BeanDefinition主要是做什么的？这个主要是用来描述Class对象的，例如这个Class是不是抽象的，作用域是什么，是不是懒加载等等信息，那如果一个Class对象有一个“属性”是BeanDefinition描述不了的，那这个要如何处理呢？那这个接口AttributeAccessor又派上用场了，你可以向其中存放任何你定义的数据，可以理解为一个map，现在了解BeanDefinition的属性的含义了么？

在这里也能看到`@Configuration(proxyBeanMethods = false)`和`@Component`一样效果，都是LITE模式

在这里第一步先判断出这个类是FULL模式还是LITE模式，那么下一步就需要开始执行对配置类的注解的解析了，在ConfigurationClassParser这个类有一个processConfigurationClass方法，里面有一个doProcessConfigurationClass方法，这里就是解析前文所列举的@Component等等注解的过程，解析完成之后，在ConfigurationClassPostProcessor类的方法processConfigBeanDefinitions，有一个loadBeanDefinitions方法，这个方法就是将前文解析成功的注解数据全都注册成BeanDefinition，这就是ConfigurationClassPostProcessor这个类的第一个方法所完成的任务，另外这个方法在这里是非常简单的描述了一下，实际上这个方法非常的复杂，需要慢慢的研究。

接下来再说ConfigurationClassPostProcessor类的`enhanceConfigurationClasses`方法，这个方法主要完成了对@Configuration注解标注的类的增强，进行CGLIB代理，代码如下：

```typescript
typescript复制代码public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
        Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<String, AbstractBeanDefinition>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
            if (ConfigurationClassUtils.isFullConfigurationClass(beanDef)) {//判断是否被@Configuration标注
                if (!(beanDef instanceof AbstractBeanDefinition)) {
                    throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
                            beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
                }
                else if (logger.isWarnEnabled() && beanFactory.containsSingleton(beanName)) {
                    logger.warn("Cannot enhance @Configuration bean definition '" + beanName +
                            "' since its singleton instance has been created too early. The typical cause " +
                            "is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " +
                            "return type: Consider declaring such methods as 'static'.");
                }
                configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
            }
        }
        if (configBeanDefs.isEmpty()) {
            // nothing to enhance -> return immediately
            return;
        }
        ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
        for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
            AbstractBeanDefinition beanDef = entry.getValue();
            // If a @Configuration class gets proxied, always proxy the target class
            beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
            try {
                // Set enhanced subclass of the user-specified bean class
                Class<?> configClass = beanDef.resolveBeanClass(this.beanClassLoader);
                Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);//生成代理的class
                if (configClass != enhancedClass) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Replacing bean definition '%s' existing class '%s' with " +
                                "enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
                    }
                    //替换class，将原来的替换为CGLIB代理的class
                    beanDef.setBeanClass(enhancedClass);
                }
            }
            catch (Throwable ex) {
                throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
            }
        }
    }
```

这里需要CGLIB的一些知识，我就简单的在这里总结一下，这个方法从所有的BeanDefinition中找到属性为FULL模式的BeanDefinition，然后对其进行代理增强，设置BeanDefinition的beanClass。然后在增强时有一些细节稍微需要明确一下，就是我们这个普通类中的方法，比如eoo(),foo()等方法，将会被MethodInterceptor所拦截，这个方法的调用将会被BeanMethodInterceptor所代理，到这里我们大家应该稍微明确了ConfigurationClassPostProcessor是在什么时间被实例化，什么时间解析注解配置，什么时间进行配置增强。如果看到这里不太明白，那欢迎与我来讨论。

## 4.总结

@Component在Spring中是代表LITE模式的配置注解，这种模式下的注解不会被Spring所代理，就是一个标准类，如果在这个类中有@Bean标注的方法，那么方法间的相互调用，其实就是普通Java类的方法的调用。

@Configuration在Spring中是代表FULL模式的配置注解，这种模式下的类会被Spring所代理，那么在这个类中的@Bean方法的相互调用，就相当于调用了代理方法，那么在代理方法中会判断，是否调用getBean方法还是invokeSuper方法，这里就是这两个注解的最根本的区别。

> 一句话概括就是 `@Configuration` 中所有带 `@Bean` 注解的方法都会被动态代理，因此调用该方法返回的都是同一个实例。



作者：shepherd111
链接：https://juejin.cn/post/7217693476493819961
来源：稀土掘金
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
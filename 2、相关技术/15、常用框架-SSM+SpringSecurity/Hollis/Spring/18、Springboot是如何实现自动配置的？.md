#SpringBoot自动装配的作用是根据条件（ConditionXxx注解）和依赖（SpringBootStart）自动注册BeanDefinition 
#自定义SpringBootStart（自定义配置类、在spring-factories文件中指定） 
#SpringBoot项目启动时根据EnableAutoConfiguration注解（主要依赖Import注解）扫描类路径下spring-factories文件定义BeanDefinition（主要依赖ConfigurationClassPostProcessor类将Import中的Class数组转换为BeanDefinition） 

主要参考：
- [45、Spring中创建Bean有几种方式？](2、相关技术/15、常用框架-SSM+SpringSecurity/Hollis/Spring/45、Spring中创建Bean有几种方式？.md)
- [扒一扒Bean注入到Spring的那些姿势](2、相关技术/15、常用框架-SSM+SpringSecurity/扒一扒Bean注入到Spring的那些姿势.md)
- [2、常用注解](2、相关技术/25、源码/Spring框架/2、常用注解.md)
- 自动装配与Bean初始化的关系：[33.1、SpringBoot启动流程中自动装配和Bean初始化关系](2、相关技术/15、常用框架-SSM+SpringSecurity/Hollis/Spring/33.1、SpringBoot启动流程中自动装配和Bean初始化关系.md)

# 1、典型回答

Spring Boot会根据类路径中的jar包、类，为jar包里的类自动配置，这样可以极大的减少配置的数量。简单点说就是它会根据定义在classpath下的类，自动的给你生成一些Bean，并加载到Spring的Context中。

自动装配步骤：
1. SpringBoot通过Spring 的条件配置决定哪些bean可以被配置，将这些条件定义成具体的Configuration，然后将这些Configuration配置到spring.factories文件中作为key:  `org.springframework.boot.autoconfigure.EnableAutoConfiguration` 的值。（spring.factories文件这种方式Springboot 2.7.0版本已不建议使用，最新的方式是使用 `/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` ）。
2. 容器在启动的时候，由于使用了 `EnableAutoConfiguration` 注解，该注解上的注解 `Import` 的`AutoConfigurationImportSelector` 会去扫描classpath下的所有 `spring.factories` 文件，定义`BeanDefinition`实现bean的自动化配置
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202405302157884.png)

# 2、扩展知识

## 2.1、条件配置

假设你希望一个或多个bean只有在某种特殊的情况下才需要被创建，比如，一个应用同时服务于中美用户，要在中美部署，有的服务在美国集群中需要提供，在中国集群中就不需要提供。在Spring 4之前，要实现这种级别的条件化配置是比较复杂的，但是，Spring 4引入了一个新的@Conditional注解可以有效的解决这类问题。
```java
@Bean
@Conditional(ChinaEnvironmentCondition.class)
public ServiceBean serviceBean(){
    return new ServiceBean();
}
```

当`@Conditional(ChinaEnvironmentCondition.class)`条件的值为true的时候，该ServiceBean才会被创建，否则该bean就会被忽略。

`@Conditional` 指定了一个条件。他的条件的实现是一个Java类——ChinaEnvironmentCondition，要实现以上功能就要定义ChinaEnvironmentCondition类，并继承Condition接口并重写其中的matches方法。
```java
class ChinaEnvironmentCondition implements Condition{
	public final boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

        Environment env = context.getEnvironment();
        return env.containProperty("ENV_CN");
    }
}
```

在上面的代码中，matches方法的内容比较简单，他通过给定的ConditionContext对象进而获取Environment对象，然后使用该对象检查环境中是否存在ENV_CN属性。如果存在该方法则直接返回true，反之返回false。当该方法返回true的时候，就符合了@Conditional指定的条件，那么ServiceBean就会被创建。反之，如果环境中没有这个属性，那么这个ServiceBean就不会被创建。

除了可以自定义一些条件之外，Spring 4本身提供了很多已有的条件供直接使用，如：
```java
@ConditionalOnBean
@ConditionalOnClass
@ConditionalOnExpression
@ConditionalOnMissingBean
@ConditionalOnMissingClass
@ConditionalOnNotWebApplication
```

## 2.2、SpringBoot应用启动入口

自动配置充分的利用了spring 4.0的条件化配置特性，那么，Spring Boot是如何实现自动配置的？Spring 4中的条件化配置又是怎么运用到Spring Boot中的呢？这要从Spring Boot的启动类说起。Spring Boot应用通常有一个名为 `*Application` 的入口类，入口类中有一个main方法，这个方法其实就是一个标准的Java应用的入口方法。一般在main方法中使用`SpringApplication.run()`来启动整个应用。值得注意的是，这个入口类要使用 `@SpringBootApplication` 注解声明。 `@SpringBootApplication` 是Spring Boot的核心注解，他是一个组合注解。
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
		@Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public @interface SpringBootApplication {
}
```

`@SpringBootApplication` 是一个组合注解，它主要包含 `@SpringBootConfiguration`、`@EnableAutoConfiguration` 等几个注解。也就是说可以直接在启动类中使用这些注解来代替 `@SpringBootApplication` 注解。 关于Spring Boot中的Spring自动化配置主要是 `@EnableAutoConfiguration` 的功劳。该注解可以让Spring Boot根据类路径中的jar包依赖为当前项目进行自动配置。

至此，我们知道，Spring Boot的自动化配置主要是通过`@EnableAutoConfiguration`来实现的，因为我们在程序的启动入口使用了`@SpringBootApplication`注解，而该注解中组合了`@EnableAutoConfiguration`注解。所以，在启动类上使用`@EnableAutoConfiguration`注解，就会开启自动配置。

那么，本着刨根问底的原则，当然要知道`@EnableAutoConfiguration`又是如何实现自动化配置的，因为目前为止，我们还没有发现Spring 4中条件化配置的影子。

## 2.3、EnableAutoConfiguration

其实Spring框架本身也提供了几个名字为`@Enable`开头的Annotation定义。比如`@EnableScheduling`、`@EnableCaching`、`@EnableMBeanExport`等，`@EnableAutoConfiguration`的理念和这些注解其实是一脉相承的。

> **@EnableScheduling** 是通过`@Import`将Spring调度框架相关的bean定义都加载到IoC容器。
>  
> **@EnableMBeanExport** 是通过`@Import`将JMX相关的bean定义加载到IoC容器。
>  
> **@EnableAutoConfiguration** 也是借助`@Import`的帮助，将所有符合自动配置条件的bean定义加载到IoC容器。

下面是`EnableAutoConfiguration`注解的源码：
```java
@Target(ElementType.TYPE)  
@Retention(RetentionPolicy.RUNTIME)  
@Documented  
@Inherited  
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {
	// ……
}
```

观察 `@EnableAutoConfiguration` 可以发现，这里Import了`AutoConfigurationImportSelector` ，这就是Spring Boot自动化配置的“始作俑者”。

至此，我们知道，由于我们在Spring Boot的启动类上使用了 `@SpringBootApplication` 注解，而该注解组合了 `@EnableAutoConfiguration` 注解， `@EnableAutoConfiguration` 是自动化配置的“始作俑者”，而 `@EnableAutoConfiguration` 中Import了`AutoConfigurationImportSelector` 类，该注解的内部实现已经很接近我们要找的“真相”了。

## 2.4、AutoConfigurationImportSelector

`AutoConfigurationImportSelector` 的源码在这里就不贴了，感兴趣的可以直接去看一下，其实实现也比较简单，主要就是使用Spring 4 提供的的`SpringFactoriesLoader`工具类。通过`SpringFactoriesLoader.loadFactoryNames()`读取了ClassPath下面的`META-INF/spring.factories`文件。

> 这里要简单提一下spring.factories文件，它是一个典型的java properties文件，配置的格式为Key = Value形式。

`AutoConfigurationImportSelector` 通过读取spring.factories中的key为 `org.springframework.boot.autoconfigure.EnableAutoConfiguration` 的值。如spring-boot-autoconfigure-1.5.1.RELEASE.jar中的spring.factories文件包含以下内容：
```java
# Auto Configure
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration,\
org.springframework.boot.autoconfigure.aop.AopAutoConfiguration,\
org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,\
org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration,\
org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration,\
org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration,\
org.springframework.boot.autoconfigure.cloud.CloudAutoConfiguration,\
......
org.springframework.boot.autoconfigure.webservices.WebServicesAutoConfiguration
```

上面的EnableAutoConfiguration配置了多个类，这些都是Spring Boot中的自启动配置相关类；在动过程中会解析对应类配置信息。每个Configuration都定义了相关bean的实例化配置。都说明了哪些bean可以被自动配置，什么条件下可以自动配置，并把这些bean实例化出来。

> 如果我们新定义了一个starter的话，也要在该starter的jar包中提供 spring.factories文件，并且为其配置org.springframework.boot.autoconfigure.EnableAutoConfiguration对应的配置类。

## 2.5、Configuration

我们从`spring-boot-autoconfigure-1.5.1.RELEASE.jar`中的spring.factories文件随便找一个Configuration，看看他是如何自动加载bean的。
```java
@Configuration
@AutoConfigureAfter({JmxAutoConfiguration.class})
@ConditionalOnProperty(
    prefix = "spring.application.admin",
    value = {"enabled"},
    havingValue = "true",
    matchIfMissing = false
)
public class SpringApplicationAdminJmxAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public SpringApplicationAdminMXBeanRegistrar springApplicationAdminRegistrar() throws MalformedObjectNameException {
	    String jmxName = this.environment.getProperty("spring.application.admin.jmx-name", "org.springframework.boot:type=Admin,name=SpringApplication");
	    if(this.mbeanExporter != null) {
            this.mbeanExporter.addExcludedBean(jmxName);
        }
	    return new SpringApplicationAdminMXBeanRegistrar(jmxName);
    }
}
```

看到上面的代码，终于找到了我们要找的东西——Spring 4的条件化配置。上面`SpringApplicationAdminJmxAutoConfiguration`在决定对哪些bean进行自动化配置的时候，使用了两个条件注解：`ConditionalOnProperty`和`ConditionalOnMissingBean`。只有满足这种条件的时候，对应的bean才会被创建。这样做的好处是什么？这样可以保证某些bean在没满足特定条件的情况下就可以不必初始化，避免在bean初始化过程中由于条件不足，导致应用启动失败。

# 3、解释自动装配作用

SpringBoot的 **==自动配置是一个<font color="blue" size=5>根据依赖和环境条件自动注册 BeanDefinition</font>的机制==**，它的作用是 **减少手动配置、提供开箱即用的默认 Bean**，本质上是在容器启动早期阶段动态注册配置类。下面结合 Spring Boot 3.x/Spring 6 源码解释：

## 3.1、自动配置的作用

1. **自动注册 BeanDefinition**
	- 根据类路径、环境、已有 Bean 条件，自动加载特定 `@Configuration` 配置类。
	- 例如引入 `spring-boot-starter-web`，就会自动注册 `DispatcherServlet`、`WebMvcConfigurer` 等。
2. **提供合理的默认值**
	- 提供“约定优于配置”的默认 Bean，开发者可通过 `@Bean` 或 `@ConditionalOnMissingBean` 自定义覆盖。
3. **解耦 Starter 与业务代码**
	- 允许第三方库通过 Starter 暴露自己的自动配置，无需修改项目代码。

## 3.2、源码流程：Spring Boot 自动配置是怎么跑起来的？

### 3.2.1、入口：`@SpringBootApplication`

```java
@SpringBootConfiguration
@EnableAutoConfiguration    // <- 核心
@ComponentScan
public @interface SpringBootApplication {}
```

`@EnableAutoConfiguration` 的源码：

```java
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {}
```

这里通过 `@Import` 把 **AutoConfigurationImportSelector** 导入到容器。

### 3.2.2、`AutoConfigurationImportSelector` 选择配置类

过程中设计到定义`BeanDefinition`的类：[3、BeanFactoryPostProcessor](1、常用类、接口、方法……#3、BeanFactoryPostProcessor)

Spring Boot 启动时会执行 `AbstractApplicationContext.refresh()` →  
`invokeBeanFactoryPostProcessors()` → `ConfigurationClassPostProcessor`，  
然后扫描 `@EnableAutoConfiguration` 并调用 `AutoConfigurationImportSelector#selectImports()`：

```java
@Override
public String[] selectImports(AnnotationMetadata metadata) {
    AutoConfigurationEntry autoConfigurationEntry =
            getAutoConfigurationEntry(metadata);
    return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
}
```

`getAutoConfigurationEntry()` 核心步骤：
1. **加载候选自动配置类**
    ```java
    List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);
    ```
    - Spring Boot 3.x：读取 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
    - Spring Boot 2.x：读取 `META-INF/spring.factories`
2. **按条件过滤**  
    调用 `AutoConfigurationImportFilter` 检查每个配置类上的 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等。
3. **返回最终要导入的配置类**

### 3.2.3、注册 BeanDefinition

`AutoConfigurationImportSelector#selectImports()`方法返回的类会被当作 `@Configuration` 处理，Spring 通过 `ConfigurationClassPostProcessor` 将其中的 `@Bean` 方法解析成 `BeanDefinition` 并注册到 `BeanFactory`。

**示例：DataSource 自动配置**
```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceProperties properties) {
        return createDataSource(properties);
    }
}
```

- 如果类路径存在 `DataSource`
- 如果容器中没有 `DataSource` Bean
- → 注册 `HikariDataSource` BeanDefinition

### 3.2.4、真正的 Bean 创建

当容器进入 `finishBeanFactoryInitialization()`，`AbstractAutowireCapableBeanFactory` 会根据 BeanDefinition 调用 `createBean()` 完成实例化和依赖注入。

> 这说明：**自动配置只负责提前“准备” BeanDefinition，实际 Bean 的生命周期依然由 Spring Framework 执行。**

## 3.3、自动配置的作用总结（结合源码）

1. **阶段**：
	- **==发生在 `invokeBeanFactoryPostProcessors()`，Bean 实例化之前==**。
2. **职责**：
	- **==动态收集 Starter 中的配置类==**。
	- 判断环境是否满足条件（`@ConditionalOnClass`、`@ConditionalOnProperty`）。
	- **==注册默认的 BeanDefinition==**。
3. **价值**：
	- 避免重复配置，提供即插即用的模块化组件。
	- 让 Spring Boot Starter 能以“零配置”方式集成。

## 3.4、一句话总结

- **Spring Boot 自动配置 = 在容器刷新早期阶段，通过 `AutoConfigurationImportSelector` + 条件注解，把 Starter 的配置类对应的`BeanDefinition`动态注册到 BeanFactory 中。**
- **AbstractAutowireCapableBeanFactory = 真正执行 Bean 实例化和依赖注入的底层工厂。**
- 两者的关系：自动配置负责“告诉容器要创建哪些 Bean”，BeanFactory负责“如何创建这些 Bean”。

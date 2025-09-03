# 典型回答

Spring 的 Bean 的创建有以下几种方式，从常见到不常见开始逐一列举：

## 通过@Component系列注解

Spring 中提供了很多注解，可以直接把一个类的实例定义成 Bean。常见的有：

- @Component
- @Service
- @Repository
- @Controller
[30、Spring中@Service 、@Component、@Repository等注解区别是什么？](2、相关技术/15、常用框架-SSM+SpringSecurity/Hollis/Spring/30、Spring中@Service%20、@Component、@Repository等注解区别是什么？.md)

代码实现如下：
```java
@Service
public class HollisService {

    public String helloWorld(String name) {
        return "Hello, " + name + "!";
    }
}

@Component
public class HollisInvokeHandler {

    public String helloWorld(String name) {
        return "Hello, " + name + "!";
    }
}

@Controller
public class HollisController {

    public String helloWorld(String name) {
        return "Hello, " + name + "!";
    }
}


@Repository
public class HollisRepository {

    public String helloWorld(String name) {
        return "Hello, " + name + "!";
    }
}
```

## 通过@Bean 注解

在 SpringBoot 的应用中，我们通常会见到通过@Bean 注解来定义 Bean 的代码，尤其是在我们自己需要封装 Starter 的时候。
[39、如何自定义一个starter？](2、相关技术/15、常用框架-SSM+SpringSecurity/Hollis/Spring/39、如何自定义一个starter？.md)

通过在类上使用 `@Configuration` 注解，然后类内部的方法上增加 `@Bean` 注解，来用该方法来定义一个 Bean。
```java
@Configuration
public class HollisConfiguration {

    @Bean
    public HollisService hollisService() {
        return new HollisChuangServiceImpl();
    }
}
```

## 通过 xml 配置

在SpringBoot 流行以前，这种方式挺多的， SpringBoot 流行起来之后，这么用的越来越少了。通过 xml 的方式来定义 Bean。
```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
    <bean id="hollisService" class="com.java.bagu.demo.HollisChuangServiceImpl"/>
</beans>
```

这种方式会调用HollisChuangServiceImpl的无参构造函数创建 Bean，同时还没用工厂 ：
```java
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
    <bean id="hollisService" class="com.java.bagu.demo.HollisChuangServiceImpl" factory-method="init"/>
</beans>
```

## 使用 @Import 注解

参照：[@Import](扒一扒Bean注入到Spring的那些姿势#@Import)

**`@Import`注解的作用是快速导入某一个或多个类，使这些类能够被Spring加载到IOC容器中进行管理**。让类被 Spring 的 IOC 容器管理，这不就是创建 Bean 么，所以，这种方式也可以。
```java
@Import({HollisChuangServiceImpl.class})
@Configuration
public class HollisConfiguration {
}
```

## 其他注解

先说一个大家可能都用过（或者见过，或者听说过）的一种 bean 注入的方式：
```java
@DubboService(version = "1.0.0")
public class HollisRemoteServiceImpl implements HollisRemoteFacadeService {

}
```

这就是直接没有用前面提到的任何一种方式，而是直接用了`@DubboService`注解，这个其实是 RPC框架 Dubbo 提供的一个注解，他也能把一个类的实例创建出来，并且放到 Spring 的容器中作为一个 Bean，等待后续被远程调用。

在 Spring 应用启动过程中，Dubbo 通过自定义的 `BeanDefinitionRegistryPostProcessor` 和 `BeanFactoryPostProcessor` 来扫描配置的包路径，识别出带有 `@DubboService` 注解的类。这些处理器解析注解中的属性（如接口类、版本号、超时时间等），并基于这些信息创建 Spring 的 `BeanDefinition`。

# ChatGPT 关于@Import注解

基于 **Spring Boot 3.1.5** 和 **Spring Framework 6.0.13** 的源码，来分析 **@Import 注解的原理及作用**。

## `@Import` 的作用

- 允许在 `@Configuration` 或 `@Component` 类上通过注解导入其他类，让 Spring 将这些类注册为 Bean。
- 可导入的类型：
    1. 普通 `@Configuration` 或组件类（注册为 BeanDefinition）
    2. 实现 `ImportSelector` 的类（动态选择要导入的配置类）
    3. 实现 `ImportBeanDefinitionRegistrar` 的类（手动注册 BeanDefinition）

👉 是 Spring **配置类解析机制** 的重要扩展点，也是 Spring Boot 自动装配的核心。

## 入口：`ConfigurationClassPostProcessor`

- `@Import` 的解析发生在 **BeanDefinition 解析阶段**，即 `AbstractApplicationContext.invokeBeanFactoryPostProcessors()`。
- `ConfigurationClassPostProcessor` 是关键处理器。

源码（Spring 6.0.13）：
```java
public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    processConfigBeanDefinitions(registry);
}
```

`processConfigBeanDefinitions()` 内部使用 `ConfigurationClassParser` 解析所有配置类。

## 源码调用链（Spring Boot 3.1.5）

```
SpringApplication.run()
   └─ AbstractApplicationContext.refresh()
       └─ invokeBeanFactoryPostProcessors()
           └─ ConfigurationClassPostProcessor.postProcessBeanDefinitionRegistry()
               └─ ConfigurationClassParser.parse()
                   └─ ConfigurationClassParser.processImports()
                       ├─ 处理普通类
                       ├─ 处理 ImportSelector
                       └─ 处理 ImportBeanDefinitionRegistrar
```

### 关键方法：`ConfigurationClassParser.processImports()`

源码逻辑（精简）：
```java
private void processImports(ConfigurationClass configClass, 
        SourceClass currentSourceClass, 
        Collection<SourceClass> importCandidates, ...) {

    for (SourceClass candidate : importCandidates) {
        Class<?> importedClass = candidate.loadClass();
        if (ImportSelector.class.isAssignableFrom(importedClass)) {
            // 处理 ImportSelector
        }
        else if (ImportBeanDefinitionRegistrar.class.isAssignableFrom(importedClass)) {
            // 处理 ImportBeanDefinitionRegistrar
        }
        else {
            // 普通配置类，直接注册 BeanDefinition
            this.configurationClasses.add(new ConfigurationClass(candidate));
        }
    }
}
```

## @Import 三种模式

### ① 导入普通类

```java
@Configuration
@Import(MyService.class)
public class AppConfig {}
```

- Spring 将 `MyService` 注册为 BeanDefinition。
- 等价于 XML `<bean class="MyService"/>`。

### ② 导入 `ImportSelector`

```java
public class MyImportSelector implements ImportSelector {
    public String[] selectImports(AnnotationMetadata metadata) {
        return new String[]{"com.example.BeanA", "com.example.BeanB"};
    }
}
```

- 可根据条件动态返回要导入的配置类。
- Spring Boot 的 `EnableAutoConfigurationImportSelector` 就是基于此。

### ③ 导入 `ImportBeanDefinitionRegistrar`

```java
public class MyRegistrar implements ImportBeanDefinitionRegistrar {
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        registry.registerBeanDefinition("myBean", new RootBeanDefinition(MyBean.class));
    }
}
```

- 可以手动注册 BeanDefinition，完全控制 Bean 元数据。

## @Import 与 Spring Boot 自动装配

- `@EnableAutoConfiguration` 自身就是通过 `@Import(AutoConfigurationImportSelector.class)` 工作的。
- `AutoConfigurationImportSelector` 读取 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件，动态导入 Starter 配置类。

调用链：
```
@EnableAutoConfiguration
   └─ @Import(AutoConfigurationImportSelector.class)
       └─ selectImports()
           └─ 加载所有自动配置类
```

## 原理总结

- `@Import` 通过 `ConfigurationClassPostProcessor` 解析，在 **BeanDefinition 注册阶段** 将指定类加载到容器。
- 支持三种模式：
    - 普通类 → 直接注册
    - ImportSelector → 动态返回配置类名
    - ImportBeanDefinitionRegistrar → 手动注册 BeanDefinition
- 是 Spring Boot 自动装配的底层机制。

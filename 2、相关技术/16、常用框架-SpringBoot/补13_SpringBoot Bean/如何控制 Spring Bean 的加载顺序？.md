**关于注解 @Order**
来自：[如何控制 Spring Bean 的加载顺序？](https://springdoc.cn/how-to-control-the-loading-order-of-beans-in-spring/)

先说结论，**使用 @Order 注解或者是实现 `Ordered` 接口并不能控制 Bean 的加载顺序**。

## 一、@Order 注解和 Ordered 接口

在 [Spring](https://springdoc.cn/spring/) 框架中，`@Order` 是一个非常实用的元注解，它位于 `spring-core` 包下，主要用于控制某些特定上下文（Context）中组件的执行顺序或排序，但它并不直接控制 Bean 的初始化顺序。

### 1.1、用途

`@Order` 注解或者是 `Ordered` 接口，常见的用途主要是两种：

- **定义执行顺序**：当多个组件（如 Interceptor、Filter、Listrner 等）需要按照特定的顺序执行时，`@Order` 注解可以用来指定这些组件的执行优先级。数值越小，优先级越高，相应的组件会更早被执行或被放置在集合的前面（`@Order` 注解接受一个整数值，这个值可以是负数、零或正数。Spring 框架提供了 `Ordered.HIGHEST_PRECEDENCE`（默认最低优先级）和 `Ordered.LOWEST_PRECEDENCE`（默认最高优先级）常量，分别对应于 `Integer.MIN_VALUE` 和 `Integer.MAX_VALUE`，可以方便地设定优先级。【我理解的这种用途可以指定某方法上不同注解的执行顺序（通过将注解@Order加载注解解析器上，值越小注解解析器越先执行）】
- **集合排序**：当相同类型的组件被自动装配到一个集合中时，`@Order` 注解会影响它们在这个集合中的排列顺序。

### 1.2、使用场景

经典的使用场景如下。

#### 拦截器的排序

在 Spring MVC 中，可以使用 `@Order` 来控制拦截器（`Interceptor`）的执行顺序。

#### Spring Security Filter

在 Spring Security 中，过滤器链（Filter Chain）的顺序通过 `@Order` 来定义，确保正确的安全处理流程。

```java
//  HttpSecurity 的 performBuild 方法
@Override
protected DefaultSecurityFilterChain performBuild() {
    // 对 Filter 进行排序
    this.filters.sort(OrderComparator.INSTANCE);
    List<Filter> sortedFilters = new ArrayList<>(this.filters.size());
    for (Filter filter : this.filters) {
        sortedFilters.add(((OrderedFilter) filter).filter);
    }
    return new DefaultSecurityFilterChain(this.requestMatcher, sortedFilters);
}
```

#### Event Listener

当有多个监听同一事件的监听器时，可以通过 `@Order` 来控制它们的触发顺序。

#### Bean 的集合注入

当一个 Bean 依赖于一个特定类型的 Bean 集合时，带有 `@Order` 注解的 Bean 将按照指定顺序被注入。

**可以看到，`@Order` 注解的使用场景中，主要是相同类型的 Bean 存在多个时，这多个 Bean 的执行顺序可以通过 `@Order` 注解或者实现 `Ordered` 接口来确定。**

但是！！！

**`@Order` 注解不控制初始化和加载顺序**：
- ==`@Order` 注解不直接影响 `Bean` 的创建和初始化过程==，这些由 `Spring IoC` 容器基于依赖关系和配置来决定。==`Spring IoC` 容器根据依赖关系图来决定 `Bean` 的初始化顺序==，而不是依据 `@Order` 注解。依赖关系决定了哪些 Bean 需要在其他 Bean 初始化之前被创建。

## 二、设置 Bean 的加载顺序

有两种方式来设置 Bean 的加载顺序。

### 2.1、`@DependsOn`

`@DependsOn` 是 Spring 框架提供的一个注解，用于指示 Spring 容器在初始化一个 Bean 之前，必须先初始化其依赖的其他 Bean。这个注解可以帮助解决 Bean 间的依赖关系，确保依赖的 Bean 已经准备就绪。

`@DependsOn` 可以放在任何一个 Spring 管理的 Bean 定义上，包括但不限于配置类（Configuration）、服务类（Service）、数据访问对象（Repository/Dao）等。其语法如下：

```java
@DependsOn({"beanName1", "beanName2", ...})
public class MyBean {
    // ...
}
```

在这个例子中，`MyBean` 类声明了它依赖于名为 `beanName1` 和 `beanName2` 的 Bean。这意味着，当 Spring 容器创建 `MyBean` 的实例时，它会首先确保 `beanName1` 和 `beanName2` 已经被正确初始化。

相关的源码在 `AbstractBeanFactory#doGetBean` 方法中：

```java
@SuppressWarnings("unchecked")
protected <T> T doGetBean(
        String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly)
        throws BeansException {
    // ....
    // 保证初始化当前 bean 依赖的 bean。
    String[] dependsOn = mbd.getDependsOn();
    if (dependsOn != null) {
        for (String dep : dependsOn) {
            if (isDependent(beanName, dep)) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
            }
            registerDependentBean(dep, beanName);
            try {
                getBean(dep);
            }
            catch (NoSuchBeanDefinitionException ex) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
            }
            catch (BeanCreationException ex) {
                if (requiredType != null) {
                    // Wrap exception with current bean metadata but only if specifically
                    // requested (indicated by required type), not for depends-on cascades.
                    throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                            "Failed to initialize dependency '" + ex.getBeanName() + "' of " +
                                    requiredType.getSimpleName() + " bean '" + beanName + "': " +
                                    ex.getMessage(), ex);
                }
                throw ex;
            }
        }
    }

// ...
}
```

在创建的 `Bean` 的时候，先检查该 `Bean` 是否依赖了其他 Bean，如果依赖了，则先把其他 `Bean` 创建出来，然后再继续创建当前 `Bean`，这样就确保了 `Bean` 的加载顺序。

### 2.2、BeanFactoryPostProcessor

第二种方式就是利用 `BeanFactoryPostProcessor`，`BeanFactoryPostProcessor` 的执行时机比较早，从下面这张流程图中可以看到，`BeanFactoryPostProcessor` 在正常的 Bean 初始化之前就执行了。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506272126494.png)

那么对于想要提前初始化的 `Bean`，我们可以在 `BeanFactoryPostProcessor` 中手动调用，类似下面这样：

```java
@Component
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        //想要提前初始化的 Bean 在这里执行
        beanFactory.getBean("serviceB");
    }
}
```

## 三、总结

多个相同类型的 Bean 该如何确保其 **执行顺序？** 这个靠 `@Order` 注解或者 `Ordered` 接口来解决。但是这两者并不能解决 Bean 的加载顺序。Bean 的加载顺序有两种方式可以调整：

1. 通过 `@DependsOn` 注解加载。
2. 手动在 `BeanFactoryPostProcessor#postProcessBeanFactory` 方法中提前调用 `getBean` 方法去初始化 Bean。

---

Ref：`https://mp.weixin.qq.com/s/a37zVQ7h-Iz4LEP1fiks2Q`

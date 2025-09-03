上一篇博文介绍了`@Order`注解的常见错误理解，它并不能指定bean的加载顺序，那么问题来了，如果我需要指定bean的加载顺序，那应该怎么办呢？

本文将介绍几种可行的方式来控制bean之间的加载顺序
- 构造方法依赖
- @DependOn 注解
- BeanPostProcessor 扩展

# 1、环境搭建

我们的测试项目和上一篇博文公用一个项目环境，当然也可以建一个全新的测试项目，对应的配置如下：（文末有源码地址）
```xml
<parent>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-parent</artifactId>  
    <version>2.1.7</version>  
    <relativePath/> <!-- lookup parent from update -->  
</parent>  
  
<properties>  
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>  
    <spring-cloud.version>Finchley.RELEASE</spring-cloud.version>  
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
        <id>spring-milestones</id>  
        <name>Spring Milestones</name>  
        <url>https://repo.spring.io/milestone</url>  
        <snapshots>  
            <enabled>false</enabled>  
        </snapshots>  
    </repository>  
</repositories>
```


# 2、初始化顺序指定
## 2.1、构造方法依赖

这种可以说是最简单也是最常见的使用姿势，但是在使用时，需要注意循环依赖等问题

我们知道bean的注入方式之中，有一个就是通过构造方法来注入，借助这种方式，我们可以解决有优先级要求的bean之间的初始化顺序

比如我们创建两个Bean，要求CDemo2在CDemo1之前被初始化，那么我们的可用方式
```java
@Component  
public class CDemo1 {  
  
    private String name = "cdemo 1";  
  
    public CDemo1(CDemo2 cDemo2) {  
        System.out.println(name);  
    }  
}  
  
@Component  
public class CDemo2 {  
  
    private String name = "cdemo 2";  
  
    public CDemo2() {  
        System.out.println(name);  
    }  
}
```

实测输出结果如下，和我们预期一致
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402222220650.png)

虽然这种方式比较直观简单，但是有几个限制

- 需要有注入关系，如CDemo2通过构造方法注入到CDemo1中，如果需要指定两个没有注入关系的bean之间优先级，则不太合适（比如我希望某个bean在所有其他的Bean初始化之前执行）
- 循环依赖问题，如过上面的CDemo2的构造方法有一个CDemo1参数，那么循环依赖产生，应用无法启动

另外一个需要注意的点是，在构造方法中，不应有复杂耗时的逻辑，会拖慢应用的启动时间

## 2.2、@DependOn 注解

这是一个专用于解决bean的依赖问题，当一个bean需要在另一个bean初始化之后再初始化时，可以使用这个注解

使用方式也比较简单了，下面是一个简单的实例case
```java
@DependsOn("rightDemo2")  
@Component  
public class RightDemo1 {  
    private String name = "right demo 1";  
  
    public RightDemo1() {  
        System.out.println(name);  
    }  
}  
  
@Component  
public class RightDemo2 {  
    private String name = "right demo 2";  
  
    public RightDemo2() {  
        System.out.println(name);  
    }  
}
```

上面的注解放在 `RightDemo1` 上，表示`RightDemo1`的初始化依赖于`rightDemo2`这个bean
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402222223972.png)

在使用这个注解的时候，有一点需要特别注意，它能控制bean的实例化顺序，但是bean的初始化操作（如构造bean实例之后，调用`@PostConstruct`注解的初始化方法）顺序则不能保证，比如我们下面的一个实例，可以说明这个问题
```java
@DependsOn("rightDemo2")  
@Component  
public class RightDemo1 {  
    private String name = "right demo 1";  
  
    @Autowired  
    private RightDemo2 rightDemo2;  
  
    public RightDemo1() {  
        System.out.println(name);  
    }  
  
    @PostConstruct  
    public void init() {  
        System.out.println(name + " _init");  
    }  
}  
  
@Component  
public class RightDemo2 {  
    private String name = "right demo 2";  
  
    @Autowired  
    private RightDemo1 rightDemo1;  
  
    public RightDemo2() {  
        System.out.println(name);  
    }  
  
    @PostConstruct  
    public void init() {  
        System.out.println(name + " _init");  
    }  
}
```

注意上面的代码，虽然说有循环依赖，但是通过`@Autowired`注解方式注入的，所以不会导致应用启动失败，我们先看一下输出结果
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402222227664.png)

有意思的地方来了，我们通过`@DependsOn`注解来确保在创建`RightDemo1`之前，先得创建`RightDemo2`；

所以从构造方法的输出可以知道，先实例RightDemo2, 然后实例RightDemo1；

然后从初始化方法的输出可以知道，在上面这个场景中，虽然RightDemo2这个bean创建了，但是它的初始化代码在后面执行
> 题外话：  
> 有兴趣的同学可以试一下把上面测试代码中的`@Autowired`的依赖注入删除，即两个bean没有相互注入依赖，再执行时，会发现输出顺序又不一样
> 
> 未去掉注解前实例化和初始化顺序（有依赖注入的情况）：
> 	right demo 2
> 	right demo 1
> 	right demo 1 _init
> 	right demo 2 _init
> 去掉注解后实例化和初始化顺序（无依赖注入的情况）：
> 	right demo 2
> 	right demo 2 _init
> 	right demo 1
> 	right demo 1 _init

## 2.3、循环依赖（2.2节）
#循环依赖

在Spring中，当两个或多个bean相互依赖时，就会发生循环依赖。Spring的依赖注入机制能够处理某些情况下的循环依赖，具体取决于依赖的类型（构造函数注入或字段/属性注入）。

### 2.3.1、为什么不会发生循环依赖

在你的示例中，`RightDemo1` 和 `RightDemo2` 之间存在字段/属性注入的循环依赖，而不是构造函数注入的循环依赖。Spring可以处理这种情况，因为它能够通过“三级缓存”机制提前暴露bean的早期引用。

#### 2.3.1.1、三级缓存机制
Spring的三级缓存机制包括：

1. **一级缓存**：单例池，存放完全初始化好的单例bean。
2. **二级缓存**：早期单例池，存放实例化但尚未完全初始化的单例bean。
3. **三级缓存**：对象工厂缓存，存放可以创建早期单例对象的工厂。

在你的示例中，当Spring创建`RightDemo1`时：

1. **创建`RightDemo1`的实例**：Spring首先实例化`RightDemo1`并将其放入三级缓存中。
2. **注入依赖`RightDemo2`**：Spring发现`RightDemo1`依赖于`RightDemo2`，因此Spring开始创建`RightDemo2`。
3. **创建`RightDemo2`的实例**：Spring实例化`RightDemo2`并将其放入三级缓存中。
4. **注入依赖`RightDemo1`**：Spring发现`RightDemo2`依赖于`RightDemo1`，此时`RightDemo1`已经在三级缓存中，Spring将其早期引用注入到`RightDemo2`中。
5. **完成初始化**：Spring完成`RightDemo2`的初始化并将其移到一级缓存中。
6. **完成`RightDemo1`的初始化**：Spring完成`RightDemo1`的依赖注入，将`RightDemo2`注入到`RightDemo1`中，然后将`RightDemo1`从三级缓存移到一级缓存中。

### 2.3.2、代码解析
```java
@DependsOn("rightDemo2")  
@Component  
public class RightDemo1 {  
    private String name = "right demo 1";  
  
    @Autowired  
    private RightDemo2 rightDemo2;  
  
    public RightDemo1() {  
        System.out.println(name);  
    }  
  
    @PostConstruct  
    public void init() {  
        System.out.println(name + " _init");  
    }  
}  
  
@Component  
public class RightDemo2 {  
    private String name = "right demo 2";  
  
    @Autowired  
    private RightDemo1 rightDemo1;  
  
    public RightDemo2() {  
        System.out.println(name);  
    }  
  
    @PostConstruct  
    public void init() {  
        System.out.println(name + " _init");  
    }  
}
```

### 2.3.3、`@DependsOn` 注解的作用
在你的示例中，`@DependsOn("rightDemo2")` 注解表明 `RightDemo1` 在初始化时需要 `RightDemo2` 先被初始化。这通常用于解决初始化顺序问题，而不是直接解决循环依赖问题。然而在字段注入的循环依赖情况下，这个注解并不是必需的，因为Spring能够通过上述的三级缓存机制处理字段注入的循环依赖。

### 2.3.4、总结
在你的示例中，字段注入的循环依赖能够被Spring的三级缓存机制处理，从而避免循环依赖的问题。因此，即使存在相互依赖的情况，Spring也能够正确地初始化这两个bean。


## 2.4、BeanPostProcessor

最后再介绍一种非典型的使用方式，如非必要，请不要用这种方式来控制bean的加载顺序

先创建两个测试bean
```java
@Component  
public class HDemo1 {  
    private String name = "h demo 1";  
  
    public HDemo1() {  
        System.out.println(name);  
    }  
}  
  
@Component  
public class HDemo2 {  
    private String name = "h demo 2";  
  
    public HDemo2() {  
        System.out.println(name);  
    }  
}
```

我们希望HDemo2在HDemo1之前被加载，借助BeanPostProcessor，我们可以按照下面的方式来实现
```java
@Component  
public class DemoBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter implements BeanFactoryAware {  
    private ConfigurableListableBeanFactory beanFactory;  
    @Override  
    public void setBeanFactory(BeanFactory beanFactory) {  
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {  
            throw new IllegalArgumentException(  
                    "AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);  
        }  
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;  
    }  
  
    @Override  
    @Nullable  
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {  
        // 在bean实例化之前做某些操作  
        if ("HDemo1".equals(beanName)) {  
            HDemo2 demo2 = beanFactory.getBean(HDemo2.class);  
        }  
        return null;  
    }  
}
```

请将目标集中在`postProcessBeforeInstantiation`，这个方法在某个bean的实例化之前，会被调用，这就给了我们控制bean加载顺序的机会
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402222241157.png)

看到这种骚操作，是不是有点蠢蠢欲动，比如我有个bean，希望在应用启动之后，其他的bean实例化之前就被加载，用这种方式是不是也可以实现呢?

下面是一个简单的实例demo，重写`DemoBeanPostProcessor`的`postProcessAfterInstantiation`方法，在application创建之后，就加载我们的FDemo这个bean
```java
@Override  
public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {  
    if ("application".equals(beanName)) {  
        beanFactory.getBean(FDemo.class);  
    }  
  
    return true;  
}  
  
  
@DependsOn("HDemo")  
@Component  
public class FDemo {  
    private String name = "F demo";  
  
    public FDemo() {  
        System.out.println(name);  
    }  
}  
  
@Component  
public class HDemo {  
    private String name = "H demo";  
  
    public HDemo() {  
        System.out.println(name);  
    }  
}
```

从下图输出可以看出，`HDemo`, `FDemo`的实例化顺序放在了最前面了
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402222244510.png)

## 2.5、小结

在小结之前，先指出一下，一个完整的bean创建，在本文中区分了两块顺序

- 实例化 （调用构造方法）
- 初始化 （注入依赖属性，调用`@PostConstruct`方法）

本文主要介绍了三种方式来控制bean的加载顺序，分别是

- 通过构造方法依赖的方式，来控制有依赖关系的bean之间初始化顺序，但是需要注意循环依赖的问题
- `@DependsOn`注解，来控制bean之间的实例顺序，需要注意的是bean的初始化方法调用顺序无法保证
- BeanPostProcessor方式，来手动控制bean的加载顺序

# 3、其他
## 3.1、源码
- 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: 
	- [https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/008-beanorder](https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/008-beanorder)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)
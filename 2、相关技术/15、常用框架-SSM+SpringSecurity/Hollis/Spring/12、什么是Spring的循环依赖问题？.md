#A依赖B、B依赖A
#Spring只能自动处理非构造注入且单例的循环依赖 
#通过一个存储了正在创建对象的集合来判断是否发生了循环依赖 
#一句话描述发现循环依赖过程：从一级缓存找不到A对象且当前正在创建对象的集合没有A对象的名字，则实例化A对象并将A写入当前正在创建对象的集合中，初始化A对象，初始化A对象时会向A对象注入B对象，从一级缓存中获取不到B对象且当前正在创建对象的集合没有B对象的名字，则实例化B对象并将B写入当前正在创建对象的集合中，初始化B对象，初始化B对象时会向B对象注入A对象，从一级缓存中获取不到A对象且当前正在创建对象的集合有A对象的名字，发现了循环依赖 

# 典型回答

在Spring框架中，循环依赖是指两个或多个bean之间相互依赖，形成了一个循环引用的情况。如果不加以处理，这种情况会导致应用程序启动失败。
```java
@Service
public class ServiceA{

	@Autowired
	private ServiceB serviceB;

}


@Service
public class ServiceB{

	@Autowired
	private ServiceA serviceA;

}
```

如以上情况，两个Bean就发生了互相依赖。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507240908874.png)

**在Spring中，解决循环依赖的方式就是引入了三级缓存。**
[25、什么是Spring的三级缓存](2、相关技术/15、常用框架-SSM+SpringSecurity/Hollis/Spring/25、什么是Spring的三级缓存.md)

但是，Spring解决循环依赖是有一定限制的：
- 首先就是要求互相依赖的Bean必须要是<font color="red" size=5>单例的Bean</font>
- 另外就是依赖注入的方式<font color="red" size=5><font color="blue" size=5>不能都是</font>构造函数注入的方式</font>

# 扩展知识

## 判断是否存在循环依赖

`通过三级缓存 + 标记创建状态的集合` 这两个数据结构去判断是否存在循环依赖；这两个数据结构都在类：`DefaultSingletonBeanRegistry` 中：
1. 三级缓存：
	1. 一级缓存：`Map<String, Object> singletonObjects`
	2. 二级缓存：`Map<String, Object> earlySingletonObjects`
	3. 三级缓存：`Map<String, ObjectFactory<?>> singletonFactories`
2. 创建集合状态：是整个IOC容器共享的，是线程安全的
	1. `Set<String> singletonsCurrentlyInCreation`

标记创建状态的集合：`Set<String> singletonsCurrentlyInCreation`；这个集合用来标记当前**正在创建中的 Bean**（应该是在Bean初始化之前就会将当前正在创建的Bean加入这个Set中【我还没debug过，这是我的猜测，不过应该没问题】）。<font color="red" size=5>一旦发现某个 Bean A 还没创建完，另一个 Bean B 又来依赖它，就说明发生了循环依赖</font>。可以通过 [三级缓存解决循环依赖问题步骤：](26、三级缓存是如何解决循环依赖的问题的？#三级缓存解决循环依赖问题步骤：) 实例来理解。

## 为什么只支持单例

为什么只支持单例，我感觉应该对照怎么判断是否发生了循环依赖，结合下面的代码可以看出，使用二级缓存和三级缓存只有在 **一级缓存中找不到指定对象 且 在当前正在创建的单例Set中存在这个beanName** 情况，这种情况会被判断为发生了循环依赖。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507242224398.png)

Spring**循环依赖的解决方案**主要是**通过对象的提前暴露来实现的**。当一个对象在创建过程中需要引用到另一个正在创建的对象时，Spring会先**提前暴露一个尚未完全初始化的对象实例，以解决循环依赖的问题**。这个尚未完全初始化的对象实例就是半成品对象。

在 Spring 容器中，**单例对象**的**创建**和**初始化**只会发生**一次**，并且在容器启动时就完成了。这意味着，<font color="red" size=5>在容器运行期间，单例对象的<font color="blue" size=5>依赖关系不会发生变化</font>。因此，可以通过提前暴露半成品对象的方式来解决循环依赖的问题。</font>

相比之下，**原型对象**的**创建**和**初始化**可以发生**多次**，并且可能<font color="red" size=5>在容器运行期间动态地发生变化</font>。因此，对于原型对象，提前暴露半成品对象并不能解决循环依赖的问题，因为在后续的创建过程中，可能会涉及到不同的原型对象实例，无法像单例对象那样缓存并复用半成品对象。

因此，Spring只支持通过单例对象的提前暴露来解决循环依赖问题。

## 为什么不支持构造函数注入

对照：[31、Spring Bean的生命周期是怎么样的？](2、相关技术/15、常用框架-SSM+SpringSecurity/Hollis/Spring/31、Spring%20Bean的生命周期是怎么样的？.md) 中的概念：实例化和初始化

Spring无法解决构造函数的循环依赖，是因为在对象实例化过程中，构造函数是最先被调用的，而此时对象还未完成实例化，**无法注入一个尚未完全创建的对象**，因此Spring容器无法在构造函数注入中实现循环依赖的解决。
```java
@Component
public class ClassA {
    private final ClassB classB;

    @Autowired
    public ClassA(@Lazy ClassB classB) {
        this.classB = classB;
    }

    // ...
}

@Component
public class ClassB {
    private final ClassA classA;

    @Autowired
    public ClassB(ClassA classA) {
        this.classA = classA;
    }

    // ...
}
```

在属性注入中，Spring容器可以通过先创建一个空对象或者提前暴露一个半成品对象来解决循环依赖的问题。但在构造函数注入中，对象的实例化是在构造函数中完成的，这样就无法使用类似的方式解决循环依赖问题了。

## 如何解决构造器注入的循环依赖

构造器注入的循环依赖，可以通过一定的手段解决。

1. **重新设计，彻底消除循环依赖**
   循环依赖，一般都是设计不合理导致的，可以从根本上做一些重构，来彻底解决，
2. **改成非构造器注入**
   可以改成setter注入或者字段注入。
3. **使用@Lazy解决**
   [35、@Lazy注解能解决循环依赖吗？](2、相关技术/15、常用框架-SSM+SpringSecurity/Hollis/Spring/35、@Lazy注解能解决循环依赖吗？.md)

#二级缓存能解决三循环依赖问题，只不过会将bean的初始化和代理耦合起来 #第三级缓存的作用是为了解耦Bean的初始化和代理的创建，当发生了循环依赖才在初始化时创建代理对象，如果没有发生循环依赖仍在初始化后创建代理对象 

# 典型回答

其实，<font color="red" size=5>使用<font color="blue" size=5>二级缓存也能解决循环依赖的问题</font>，但是<u>如果完全依靠二级缓存解决循环依赖，意味着当我们依赖了一个代理类的时候，就需要在Bean实例化之后完成AOP代理（也就是在二级缓存中存储代理的半对象和半对象）</u>。而在Spring的设计中，<font color="blue" size=5>为了解耦Bean的初始化和代理</font>，通过 AnnotationAwareAspectJAutoProxyCreator 这个后置处理器来在Bean生命周期的最后一步来完成AOP代理的</font>。

但是，在Spring的初始化过程中，他是不知道哪些Bean可能有循环依赖的，那么，这时候Spring面临两个选择：
1. **==不管有没有循环依赖，都提前把代理对象创建出来==**，并将代理对象缓存起来，出现循环依赖时，其他对象直接就可以取到代理对象并注入。
2. **==不提前创建代理对象，在出现循环依赖时（初始化过程中），再生成代理对象，否则按照原有流程在初始化最后生成代理对象==**。这样在没有循环依赖的情况下，Bean就可以按着Spring设计原则的步骤来创建，即先实例化一个空对象，再初始化该对象，并且在初始化最后再创建对应的代理对象。

第一个方案看上去比较简单，只需要二级缓存就可以了。但是他也意味着，Spring需要在所有的bean的创建过程中就要先生成代理对象再初始化；那么这就和spring的aop的设计原则（前文提到的：在Spring的设计中，**==为了解耦`Bean`的初始化和代理，是通过`AnnotationAwareAspectJAutoProxyCreator`(AbstractAutoProxyCreator的子类)这个后置处理器来在`Bean`生命周期的最后一步来完成AOP代理的）==** 是相悖的。

而Spring为了不破坏AOP的代理设计原则，则引入第三级缓存，在三级缓存中保存对象工厂，因为**通过对象工厂我们可以在想要创建对象的时候直接获取对象**。有了它，在后续发生循环依赖时，如果依赖的Bean被AOP代理，那么**通过这个工厂获取到的就是代理后的对象**，如果没有被AOP代理，那么这个工厂获取到的就是实例化的真实对象。

# 扩展知识

其实，只用二级缓存，也是可以解决循环依赖的问题的，如下图，没有三级缓存，只有二级缓存的话，也是可以解决循环依赖的。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506041033636.png)

那么，为什么还需要引入三级缓存呢？我们看下两个过程的区别主要是什么呢？
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506041035361.png)

我给大家标出来了，如果使用三级缓存，<font color="red" size=5>在实例化之后，初始化之前，向三级缓存中保存的是ObjectFactory</font>。而如果使用二级缓存，那么在这个步骤中保存的就是具体的Object。

这里如果我们只用二级缓存，对于普通对象的循环依赖问题是都可以正常解决的，但是如果是代理对象的话就麻烦多了，并且AOP又是Spring中很重要的一个特性，代理又不能忽略。

**我们都知道，我们是可以在一个ServiceA中注入另外一个ServiceB的代理对象的，那么在解决循环依赖过程中，如果需要注入ServiceB的代理对象，就需要把ServiceB的代理对象创建出来，但是这时候还只是ServiceB的实例化阶段，代理对象的创建要等到初始化之后，在后置处理的postProcessAfterInitialization方法中对初始化后的Bean完成AOP代理的。**

那怎么办好呢？Spring想到了一个好的办法，那就是使用三级缓存，并且在这个三级缓存中，并没有存入一个实例化的对象，而是存入了一个匿名类ObjectFactory（其实本质是一个函数式接口() -> getEarlyBeanReference(beanName, mbd, bean)），具体代码如下：
```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
		implements AutowireCapableBeanFactory {
  protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
    throws BeanCreationException {
    ....
    
    // 如果允许循环引用，且beanName对应的单例bean正在创建中，则早期暴露该单例bean，以便解决潜在的循环引用问题
    	boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
    			isSingletonCurrentlyInCreation(beanName));
    	if (earlySingletonExposure) {
    		if (logger.isTraceEnabled()) {
    			logger.trace("Eagerly caching bean '" + beanName +
    					"' to allow for resolving potential circular references");
    		}
    		// 向singletonFactories添加该beanName及其对应的提前引用对象，以便解决潜在的循环引用问题
    		addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    	}
    
    ...
    }
}
```

# ChatGPT解释第三级缓存存在的意义

## 为什么不能只用二级缓存？

如果只用二级缓存：
- 你只能注入“原始对象”，而不是代理对象；
- 对于像 `@Transactional`、`@Async` 等 **需要代理对象的注解**，就无法正常生效；
- 比如事务根本不会开启（因为调用的是原始对象）；

## 第三级缓存的意义

| 点位   | 描述                                                               |
| ---- | ---------------------------------------------------------------- |
| 目标   | 延迟暴露 Bean 的代理对象，用于字段注入中提前使用                                      |
| 核心   | 提前注册 `ObjectFactory`，允许注入增强后的代理对象                                |
| 关键接口 | `SmartInstantiationAwareBeanPostProcessor#getEarlyBeanReference` |
| 所在类  | `AbstractAutoProxyCreator`                                       |
| 场景   | AOP、事务、异步等增强功能 + 循环依赖                                            |

## 如何保证循环依赖注入的是代理对象而不是原始对象

**关键点**：Spring 是如何保证在循环依赖中注入的是**代理对象而不是原始对象**的，尤其是 `B` 有增强（如 `@Transactional`）的情况。
```java
@Component
class A {
    @Autowired
    private B b;
}

@Component
class B {
    @Autowired
    private A a;

    @Transactional
    public void doSomething() {}
}
```

你希望的是：
- A 注入的是 “已经增强后的 B（代理）”
- B 也能注入 “正在初始化中的 A”

### 步骤还原：如何注入的是 “代理的 B”？

1. Spring 先创建 A，进入 `doCreateBean("a")`
2. Spring 发现 A 依赖 B，于是去创建 B → `doCreateBean("b")`
3. B 又依赖 A：
    - 由于 A 已在创建中（还没完成），Spring 只能从缓存中“提前获取” A（这是循环依赖关键）
    - Spring 调用 `getSingleton("a")` → 从三级缓存中尝试获取 A（这是 A 的 early reference，但**不是代理**，因为 A 没有增强）
4. 接着初始化 B 的过程继续（执行 `BeanPostProcessor` 中 `postProcessAfterInstantiation` 方法）：
    - `AbstractAutoProxyCreator` 识别到 B 有 `@Transactional`
    - Spring 调用 `wrapIfNecessary(b)` → 创建 B 的代理对象
5. Spring 把这个代理对象 `bProxy` 放入一级缓存
6. 回到 A 的初始化阶段，此时对 B 的注入，会从一级缓存中拿到 **增强后的代理对象 bProxy**

### 看源码核心逻辑：

Spring 在调用 `populateBean()` 注入字段时，会通过：
```java
DependencyDescriptor → resolveDependency → getBean("b")
```

这个 `getBean("b")` 实际走的是：
```java
singletonObjects.get("b") → 返回的是 B 的代理对象
```

原因：
- 在创建 B 时，代理对象已经生成并放入一级缓存（`singletonObjects`）

**==所以最终注入到 A.b 的是代理对象，不是原始对象！==**

### 关键点串联

| 步骤            | 行为                           | 说明                                          |
| ------------- | ---------------------------- | ------------------------------------------- |
| 创建 A          | 进入 doCreateBean              | A 依赖 B，需创建 B                                |
| 创建 B          | 进入 doCreateBean              | B 依赖 A，从 3 级缓存中拿到 A 的 ObjectFactory 创建的原始对象 |
| 继续初始化B，判断是否增强 | AbstractAutoProxyCreator     | B 有 @Transactional，创建代理 bProxy              |
| 注入 A.b        | A 注入的是 singletonObjects["b"] | 即 bProxy，确保代理注入成功                           |

### 小结回答

> **怎么在还没初始化完成的 A 中注入一个已经被代理的 B？**

Spring 的做法是：
1. **创建 A 时遇到 B，会去创建 B**
2. B 被代理后（如带 `@Transactional`），Spring 会将 **增强后的 B（bProxy）放入一级缓存**
3. A 继续初始化，注入 B 时，`@Autowired` 实际从一级缓存中拿到的是 **bProxy**
4. 所以 A 中注入的是 “已经被代理的 B”，问题完美解决 

> **简介**：Spring的AOP中的一个核心概念是切入点（Pointcut）,切入点表达式定义通知（Advice）执行的范围。

# 1、概述

Spring AOP 只支持 Spring Bean 的方法切入，所以切点表达式只会匹配 Bean 类中的方法。

# 2、切入点表达式配置
## 2.1、内置配置

定义切面通知时，在 `@Before` 或 `@AfterReturning` 等通知注解中指定切入点表达式。
```java
@Aspect
@Component
public class DemoAspect {
    @Before("execution(* cn.codeartist.spring.aop.advice.*.*(..))")
    public void doBefore() {        
    // 自定义逻辑    
    }
}
```

## 2.2、注解配置

在切面类中，先定义一个方法并使用 `@Pointcut` 注解来指定切入点表达式。

然后在定义切面通知时，在通知注解中指定定义切入点表达式的方法签名。
```java
@Aspect
@Component
public class DemoAspect {
    @Pointcut("execution(* cn.codeartist.spring.aop.aspectj.*.*(..))")    
    private void pointcut() {        
    // 切点表达式定义方法，方法修饰符可以是private或public    
    }    
    
    @Before("pointcut()")    
	public void doBefore(JoinPoint joinPoint) {     
	// 自定义逻辑    
    }
}
```

## 2.3、公共配置

在任意类中，定义一个公共方法并使用 `@Pointcut` 注解来指定切入点表达式。
```java
public class CommonPointcut {
    @Pointcut("execution(* cn.codeartist.aop.*..*(..))")    
    public void pointcut() {        
    // 注意定义切点的方法的访问权限为public    
    }
}
```

在切面类中定义切面通知时，在通知注解中指定定义表达式的方法签名全路径。
```java
@Aspect
@Component
public class DemoAspect {
    
    @Before("cn.codeartist.aop.CommonPointcut.pointcut()")
    public void commonPointcut() {        
    // 自定义逻辑    
    }
}
```

# 3、切入点表达式类型

PCD（切入点表达式）一览图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308102050843.png)

SpringAOP是基于动态代理实现的，以下以目标对象表示被代理的bean，目标方法表示被代理的方法，代理对象表示AOP构建出来的bean。

Spring AOP 支持以下几种切点表达式类型
## 3.1、execution

最常用的PCD。

匹配方法执行的切入点。根据表达式描述匹配方法，是最通用的表达式类型，可以匹配方法、类
包。
表达式模式：
```java
execution(modifier? ret-type declaring-type?name-pattern(param-pattern) throws-pattern?)

execution(修饰符匹配式? 返回类型匹配式 类名匹配式? 方法名匹配式(参数匹配式) 异常匹配式?)
```

代码块中带有 `？` 符号的匹配式都是可选的，对于execution PCD有三个必不可少的：
1. <font color = "blue">返回值匹配式</font>：可以使用 `*` 匹配任意类型
2. <font color = "blue">方法名匹配式</font>：可以使用 `*` 匹配任意方法
3. <font color = "blue">参数匹配式</font>：可以使用 `*` 匹配任意参数

- **modifier**：匹配修饰符，`public, private` 等，<font color = "red">省略时匹配任意修饰符</font>
- **ret-type**：匹配返回类型，<font color = "blue">使用 * 匹配任意类型</font>
- **declaring-type**：匹配目标类，<font color = "red">省略时匹配任意类型</font>
	- `..` 匹配包及其子包的所有类
- **name-pattern**：匹配方法名称，<font color = "blue">使用 * 表示通配符</font>
	- `*` 匹配任意方法  
	- `set*` 匹配名称以 `set` 开头的方法
- **param-pattern**：匹配参数类型和数量
	- `()` 匹配没有参数的方法  
	- `(..)` 匹配有任意数量参数的方法  
	- <font color = "blue">(*)</font>：匹配有一个任意类型参数的方法
	- <font color = "blue">(*,String)</font>：匹配有两个参数的方法，并且第一个为任意类型，第二个为 `String` 类型
- **throws-pattern**：匹配抛出异常类型，<font color = "red">省略时匹配任意类型</font>
- **？**：表示前面的为可选项

举例分析：
`execution(public * ServiceDemo.*(..))` 匹配public修饰符，返回值是 `*` ,即任意返回值类型都行，`ServiceDemo` 是类名匹配式（不一定要全路径，只要全局可见性唯一就行），`.*` 是方法名匹配式，匹配所有方法，`..` 是参数匹配式，匹配任意数量、任意类型参数的方法。

使用示例
```java
// 拦截public方法
execution(public * *(..))
// 拦截名称以set开头的所有方法
execution(* set*(..))
// 拦截AccountService接口或类下的所有方法
execution(* com.xyz.service.AccountService.*(..))
// 拦截service包及其子包的类或接口下的所有方法
execution(* com.xyz.service..*(..))
```

## 3.2、within

限制匹配在特定类型内的连接点（给定class的所有方法）。<font color = "red">拦截指定类中定义的方法。</font>匹配指定类的任意方法，不能匹配接口。
表达式模式：
```java
within(declaring-type)
```

使用示例：
```java
// 拦截service包的类中的所有方法
within(com.xyz.service.*)
// 拦截service包及其子包的类的所有方法
within(com.xyz.service..*)
// 拦截AccountServiceImpl类的所有方法
within(com.xyz.service.AccountServiceImpl)
```

## 3.3、this

限制匹配是给定类型的实例的bean引用（Spring AOP Proxy）的连接点（代理类是给定类型的类的所有方法）。<font color = "red">代理对象为指定的类型时才会被拦截。</font>(我感觉应该是在代理对象匹配指定类型，并且使用代理对象调用方法时才会被拦截)。
表达式模式：
```java
this(declaring-type)
```

使用示例：
```java
// 拦截代理对象类型为service包下的类中的所有方法
this(com.xyz.service.*)
// 拦截代理对象类型为service包及其子包下的类的所有方法
this(com.xyz.service..*)
// 拦截代理对象类型为AccountServiceImpl的类的所有方法
this(com.xyz.service.AccountServiceImpl)
```

## 3.4、target

限制匹配是给定类型的实例的目标对象（被代理对象）的连接点（目标对象是给定类型的类的所有方法）。<font color = "red">目标对象为指定的类型时才会被拦截。</font>(我感觉应该是在目标对象匹配指定类型，并且使用目标对象调用方法时才会被拦截)。
表达式模式：
```java
target(declaring-type)
```

使用示例：
```java
// 拦截目标对象类型为service包下的类中的所有方法
target(com.xyz.service.*)
// 拦截目标对象类型为service包及其子包下的类的所有方法
target(com.xyz.service..*)
// 拦截目标对象类型为AccountServiceImpl的类的所有方法
target(com.xyz.service.AccountServiceImpl)
```

三个表达式匹配范围如下：

| 表达式匹配范围 | within | this | target |
| -------------- | ------ | ---- | ------ |
| 接口           | ❌     | ✔    | ✔      |
| 实现接口的类   | ✔      | ⭕   | ✔      |
| 不实现接口的类 | ✔      | ✔    | ✔      | 

**this和target的不同点：**
1. this作用于代理对象，target作用于目标对象；
2. this表示目标对象被代理之后生成的代理对象和指定的类型匹配会被拦截，匹配的是代理对象对应的类；
3. target表示目标和指定的类型匹配会被拦截，匹配的是目标对象对应的类。

## 3.5、args

匹配参数是给定类型/数量的连接点（方法入参对应的类上有给定注解）。<font color = "red">方法的参数对应类型为指定类型时才会被拦截</font>，参数类型可以为指定类型及其子类。
> 使用`execution`表达式匹配参数时，不能匹配参数类型为子类的方法。

表达式模式：
```java
args(param-pattern)
```

使用示例：
```java
// 拦截参数只有一个且为Serializable类型（或实现Serializable接口的类）的方法
args(java.io.Serializable)
// 拦截参数个数至少有一个且为第一个为Example类型（或实现Example接口的类）的方法
args(cn.codeartist.spring.aop.pointcut.Example,..)
```

## 3.6、bean

通过bean的id或名称匹配，支持 `*` 通配符。
表达式模式：
```java
bean(bean-name)
```

使用示例：
```java
// 匹配名称以Service结尾的bean
bean(*Service)
// 匹配名称为demoServiceImpl的bean
bean(demoServiceImpl)
```

## 3.7、@within

匹配有给定注解的类型的连接点（class上有给定注解的class的所有方法）。<font color = "red">当定义类时使用了注解，该类的方法会被拦截，但在接口上使用注解不拦截。</font>
使用示例：
```java
// 声明使用了Demo注解的类中的所有方法都会被拦截
@within(cn.codeartist.spring.aop.pointcut.Demo)
```
## 3.8、@target

匹配有给定注解的执行对象的class的连接点（目标对象class上有给定注解的类的所有方法）。<font color = "red">当运行时对象实例对应的类的声明上使用了指定注解注解，该类的方法会被拦截，在接口上使用注解不拦截。</font>
使用示例：
```java
// 如果目标对象中包含Demo注解，那么调用该目标对象的任意方法都会被拦截
@target(cn.codeartist.spring.aop.pointcut.Demo)
```

**@target和@within的不同点：**
1. @target(注解A)：判断被调用的目标对象所在的类中是否声明了注解A，如果有，那么该对象所在的类中的方法会被拦截；
2. @within(注解A)：判断被调用的方法所属的类中声明声明了注解A，如果有，会被拦截；
3. @target关注的是被调用的对象，@within关注的是被调用的方法所在的类。
## 3.9、@annotation

匹配连接点的subject有给定注解的连接点（方法上有给定注解的方法）。匹配方法是否含有注解，<font color = "red">当方法上使用了注解，该方法会被匹配，在接口方法上使用注解不匹配。</font>
使用示例：
```java
// 匹配使用了Demo注解的方法
@annotation(cn.codeartist.spring.aop.pointcut.Demo)
```

## 3.10、@args

匹配方法的实际参数类型（参数所在的类）是否含有注解。<font color = "red">当方法的实际参数类型上使用了注解，该方法会被匹配。</font>
使用示例：
```java
// 匹配参数只有一个且参数对应的类使用了Demo注解
@args(cn.codeartist.spring.aop.pointcut.Demo)
// 匹配参数个数至少有一个且为第一个参数对应的类使用了Demo注解
@args(cn.codeartist.spring.aop.pointcut.Demo,..)
```

## 3.11、切入点表达式的参数匹配

切入点表达式中的参数类型，可以和通知方法的参数通过名称绑定，表达式中不需要写类或者注解的全路径，而且能直接获取到切面拦截的参数或注解信息。
```java
@Before("pointcut() && args(name,..)")
public void doBefore(String name) {
    // 切点表达式增加参数匹配，可以获取到name的信息
}
@Before("@annotation(demo)")
public void doBefore(Demo demo) {
    // 这里可以直接获取到Demo注解的信息
}
```

> 切入点表达式的参数匹配同样适用于@within、@target、@args。

## 3.12、怎样编写一个好的切入点表达式？

要使切入点的匹配性能达到最佳，编写表达式时，应该尽可能缩小匹配范围，切入点表达式分为三大类：
1. 类型表达式：匹配某个特定切入点，如execution
2. 作用域表达式：匹配某组切入点，如within——匹配的性能非常快，所以表达式中尽可能使用作用域类型。
3. 上下文表达式：基于上下文匹配某些切入点，如this、target和@annotation——上下文表达式可以基于切入点上下文匹配或在通知中绑定上下文。
一个好的切入点表达式应该至少包含两种（类型和作用域）类型。
单独使用类型表达式或上下文表达式比较消耗性能（时间或内存使用）。


# 4、切入点表达式组合

使用&&、|| 和 | 来组合多个切入点表达式，表示多个表达式“与”、“或”、“非”的逻辑关系。
这个可以用来组合多种类型的表达式，来提升匹配效率。
```java
// 匹配doExecution()切点表达式并且参数第一个为Account类型的方法
@Before("doExecution() && args(account,..)")
public void validateAccount(Account account) {
    // 自定义逻辑
}
```
# 5、附录


## 5.1、常用注解

| 注解      | 描述             |
| --------- | ---------------- |
| @Pointcut | 指定切入点表达式 | 

## 5.2、切入点表达式类型

| 表达式类型  | 描述                               |
| ----------- | ---------------------------------- |
| execution   | 匹配方法切入点                     |
| within      | 匹配指定类型                       |
| this        | 匹配代理对象实例的类型             |
| target      | 匹配目标对象实例的类型             |
| args        | 匹配方法参数                       |
| bean        | 匹配bean的id或者名称               |
| @within     | 匹配类型是否含有注解               |
| @target     | 匹配目标对象实例的类型是否含有注解 |
| @annotation | 匹配方法是否含有注解               |
| @args       | 匹配方法参数类型是否含有注解                                   |

## 5.3、示例代码

Gitee仓库：[https://gitee.com/code_artist/spring](https://gitee.com/code_artist/spring?spm=a2c6h.12873639.article-detail.150.5f1a17d5eZSTy0)
已经frok到gitee上：https://gitee.com/li-xudongtttttt/spring




参考：
1. [https://developer.aliyun.com/article/928976](https://developer.aliyun.com/article/928976
2. [https://blog.csdn.net/buzhimingyue/article/details/106071059](https://blog.csdn.net/buzhimingyue/article/details/106071059)（已经整理到[2、Spring AOP AspectJ切点表达式详解](2、相关技术/15、常用框架-SSM+SpringSecurity/补7_SpringAOP/2、Spring%20AOP%20AspectJ切点表达式详解.md)）
3. [https://zhuanlan.zhihu.com/p/63001123](https://zhuanlan.zhihu.com/p/63001123)



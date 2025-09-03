#注解 #元注解 

> **==注解元素==**：注解接口被当作注解的时候注解名后面括号中的值
> **==注解接口方法==**：注解接口在定义的时候声明的方法

参考：[Java注解（Annotation）【来自菜鸟教程】.xmind](D:\Xmind中的思维导图\java\Java注解（Annotation）【来自菜鸟教程】.xmind)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508141804825.png)

从上图中可以看出：每1个 `Annotation` 都与1个 `RetentionPolicy` 关联，并且与n个 `ElementType` 关联。可以通俗的理解为：每1个 `Annotation` 对象，都会有唯一的 `RetentionPolicy` 属性，有n个 `ElementType` 属性。

# 典型回答

**Java 注解用于为 Java 代码提供元数据**。作为元数据，注解不直接影响你的代码执行，但也有一些类型的注解实际上可以用于这一目的。Java 注解是从 Java5 开始添加到 Java 的。

注解分为两种：
1. 标注到业务逻辑代码上的注解：我们自定义的注解
2. 标注在注解上的上的注解：[元注解](28、Java注解的作用是啥#什么是元注解)

Java的注解，通常和反射、AOP结合起来使用。中间件一般会定义注解，如果某些类或字段符合条件，就执行某些能力。
[8、使用自定义注解+切面减少冗余代码，提升代码的鲁棒性](9、项目难点&亮点/8、使用自定义注解+切面减少冗余代码，提升代码的鲁棒性.md)

`@interface`声明该接口继承自`Annotation`接口；自定义的注解反编译后可以看出来是个接口，这个接口继承自 `Annotation` 接口，反编译注解 `MyInheritedAnnotation` 的字节码：
```java
import java.lang.annotation.*;

@Inherited  
@Retention(RetentionPolicy.RUNTIME)  
@interface MyInheritedAnnotation {  
}

// -----jad MyInheritedAnnotation.class------

// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Base.java

import java.lang.annotation.Annotation;

interface MyInheritedAnnotation extends Annotation
{
}
```

# 扩展知识

## 什么是元注解

说简单点，就是 定义其他注解的注解 。
比如Override这个注解，就不是一个元注解。而是通过元注解定义出来的。
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Override {
}
```

这里面的`@Target`、`@Retention`就是元注解。
元注解有四个:
1. `@Target`（表示该注解可以用于什么地方）
2. `@Retention`（表示该注解在什么时候生效）：**==编译之前都生效==（`SOURCE`）、==运行之前都生效==（`CLASS`）、==运行时也生效==（`RUNTIME`）**
3. `@Documented`（将此注解包含在javadoc中）
4. `@Inherited`（允许子类继承父类中的注解）。

一般`@Target`是被用的最多的。

### `@Retention`

**==指定被修饰的注解的生命周期==，即注解在==源代码==、==编译时==**、**==运行时==** 保留。它有三个可选的枚举值：`SOURCE`、`CLASS`、`RUNTIME`。**默认为`CLASS`**：
1. `SOURCE`：`Annotation` **==仅存在于编译器处理期间，编译器处理完之后，该 Annotation 就没用了==**。 例如，" @Override" 标志就是一个 Annotation。当它修饰一个方法的时候，就意味着该方法覆盖父类的方法；并且在编译期间会进行语法检查！编译器处理完后，`@Override`就没有任何作用了。
2. `CLASS`：**==编译器将 `Annotation` 存储于类对应的 `.class` 文件中==**，它是 Annotation 的 <font color="blue" size=5>默认行为</font>。
3. `RUNTIME`：**==编译器将 `Annotation` 存储于 `class` 文件中，并且可由JVM读入==**，<font color="blue" size=5>这三种方式中只有这种，才可以在代码中通过反射获取被上的注解</font>
```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MyRuntimeAnnotation {
    // some elements and values
}
```

### `@Target`

**==指定被修饰的注解可以应用于的元素类型，如类、方法、字段、参数、构造方法、局部变量==** 等，<font color="blue" size=5>若我们自定义的注解不使用这个给元注解，那么我们自定义的注解可以用在任何地方</font>。这样可以限制注解的使用范围，避免错误使用。
```java
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Target({ElementType.TYPE, ElementType.METHOD})
public @interface MyTargetAnnotation {
    // some elements and values
}
```

### `@Documented`

**==用于指示注解是否会出现在生成的Java文档中==**。如果一个注解被`@Documented`元注解修饰，则该注解的信息会出现在API文档中，方便开发者查阅。
```java
import java.lang.annotation.Documented;

@Documented
public @interface MyDocumentedAnnotation {
    // some elements and values
}
```

### `@Inherited`

**==指示被该注解修饰的注解是否可以被继承==**。<font color="blue" size=5>默认情况下，注解不会被继承，即子类不会继承父类上标注的注解</font>。但如果将一个注解用`@Inherited`修饰，那么它就可以被子类继承。
```java
import java.lang.annotation.Inherited;

@Inherited
public @interface MyInheritedAnnotation {
    // some elements and values
}
```

示例：sub类虽然没有标注注解`@MyInheritedAnnotation`，但是因为因为该注解存在继承性，所以通过反射可以看到`Sub`元信息中存在`@MyInheritedAnnotation`注解
```java
@MyInheritedAnnotation
class Base {}

class Sub extends Base {}

import java.lang.annotation.*;  
import java.util.Arrays;  
  
public class test  
{  
    public static void main(String[] args) throws NoSuchMethodException  
    {  
        Annotation[] baseAnnotations = Base.class.getAnnotations();  
        System.out.println(Arrays.toString(baseAnnotations));  
        Annotation[] subAnnotations = Sub.class.getAnnotations();  
        System.out.println(Arrays.toString(subAnnotations));  
    }  
}  
@Inherited  
@Retention(RetentionPolicy.RUNTIME)  
@interface MyInheritedAnnotation {  
}  
  
@MyInheritedAnnotation  
class Base {}  
class Sub extends Base {}
```

打印结果：
```java
[@com.mimaxueyuan.jvm.test.MyInheritedAnnotation()]
[@com.mimaxueyuan.jvm.test.MyInheritedAnnotation()]
```

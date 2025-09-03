# 典型回答

JDK 8中推出了`Lambda`表达式、`Stream`、`Optional`、新的日期API、**接口中增加默认方法**等
JDK 9中推出了**模块化**、**接口中增加私有方法**
JDK 10中推出了**本地变量类型推断**
JDK 12中增加了switch表达式
JDK 13中增加了**text block**
JDK 14中增加了**Records**
JDK 14中增加了instance模式匹配
JDK 15中增加了封闭类
JDK 17中扩展了switch模式匹配
JDK 21中增加了**协程**、虚拟线程

（以上没有把所有版本都列出是因为某些版本的特性并不重要，或者开发者不太需要关注）

# 扩展知识

## 本地变量类型推断

在Java 10之前版本中，我们想定义定义局部变量时。我们需要在赋值的左侧提供显式类型，并在赋值的右边提供实现类型：
```java
MyObject value = new MyObject();
```

在Java 10中，提供了本地变量类型推断的功能，可以通过var声明变量：
```java
var value = new MyObject();
```

本地变量类型推断将引入 `var` 关键字，而不需要显式的规范变量的类型。

其实，所谓的本地变量类型推断，也是Java 10提供给开发者的语法糖。

虽然我们在代码中使用`var`进行了定义，但是对于虚拟机来说他是不认识这个`var`的，在java文件编译成class文件的过程中，会进行解糖，使用变量真正的类型来替代`var`

## Switch 表达式

在JDK 12中引入了`Switch`表达式作为预览特性。并在Java 13中修改了这个特性，引入了`yield`语句，用于返回值。

而在之后的Java 14中，这一功能正式作为标准功能提供出来。

在以前，我们想要在`switch`中返回内容，还是比较麻烦的，一般语法如下：
```java
int i;
switch (x) {
    case "1":
        i=1;
        break;
    case "2":
        i=2;
        break;
    default:
        i = x.length();
        break;
}
```

在JDK13中使用以下语法：
```java
int i = switch (x) {
    case "1" -> 1;
    case "2" -> 2;
    default -> {
        int len = args[1].length();
        yield len;
    }
};
```

或者
```java
int i = switch (x) {
    case "1": yield 1;
    case "2": yield 2;
    default: {
        int len = args[1].length();
        yield len;
    }
};
```

在这之后，`switch`中就多了一个关键字用于跳出`switch`块了，那就是`yield`，他用于返回一个值。

和`return`的区别在于：`return`会直接跳出当前循环或者方法，而`yield`只会跳出当前`switch`块。

## Text Blocks

Java 13中提供了一个Text Blocks的预览特性，并且在Java 14中提供了第二个版本的预览。

text block，文本块，是一个 **==多行字符串文字==**，它避免了对大多数转义序列的需要，以可预测的方式自动格式化字符串，并在需要时让开发人员控制格式。

我们以前从外部copy一段文本串到Java中，会被自动转义，如有一段以下字符串：
```java
<html>
  <body>
      <p>Hello, world</p>
  </body>
</html>
```

将其复制到Java的字符串中，会展示成以下内容：
```java
"<html>\n" +
"    <body>\n" +
"        <p>Hello, world</p>\n" +
"    </body>\n" +
"</html>\n";
```

即被自动进行了转义，这样的字符串看起来不是很直观，在JDK 13中，就可以使用以下语法了：
```java
"""
<html>
  <body>
      <p>Hello, world</p>
  </body>
</html>
""";
```

使用`"""`作为文本块的开始符合结束符，在其中就可以放置多行的字符串，不需要进行任何转义。看起来就十分清爽了。

如常见的SQL语句：
```java
String query = """
    SELECT `EMP_ID`, `LAST_NAME` FROM `EMPLOYEE_TB`
    WHERE `CITY` = 'INDIANAPOLIS'
    ORDER BY `EMP_ID`, `LAST_NAME`;
""";
```

看起来就比较直观，清爽了。

## Records

Java 14 中便包含了一个新特性：EP 359: Records，

`Records`的目标是扩展Java语言语法，`Records`为声明类提供了一种紧凑的语法，用于创建一种类中是 **字段，只是字段，除了字段什么都没有** 的类。

通过对类做这样的声明，编译器可以通过自动创建所有方法并让所有字段参与`hashCode()`等方法。这是JDK 14中的一个预览特性。

使用`record`关键字可以定义一个记录：
```java
record Person (String firstName, String lastName) {}
```

`record` 解决了使用类作为数据包装器的一个常见问题。纯数据类从几行代码显著地简化为一行代码。（详见：[41.1、Java 14 发布了，不使用class也能定义类了？还顺手要干掉Lombok！](2、相关技术/1、Java基础/Hollis/41.1、Java%2014%20发布了，不使用class也能定义类了？还顺手要干掉Lombok！.md)）

## 封闭类

在Java 15之前，Java认为"代码重用"始终是一个终极目标，所以，一个类和接口都可以被任意的类实现或继承。

但是，在很多场景中，这样做是容易造成错误的，而且也不符合物理世界的真实规律。

例如，假设一个业务领域只适用于汽车和卡车，而不适用于摩托车。

在Java中创建Vehicle抽象类时，应该只允许Car和Truck类扩展它。

通过这种方式，我们希望确保在域内不会出现误用Vehicle抽象类的情况。

为了解决类似的问题，在Java 15中引入了一个新的特性——密闭。

想要定义一个密闭接口，可以将`sealed`修饰符应用到接口的声明中。然后，`permit`子句指定允许实现密闭接口的类：
```java
public sealed interface Service permits Car, Truck {
}
```

以上代码定义了一个密闭接口`Service`，它规定只能被`Car`和`Truck`两个类实现。

与接口类似，我们可以通过使用相同的`sealed`修饰符来定义密闭类：
```java
public abstract sealed class Vehicle permits Car, Truck {
}
```

通过密闭特性，我们定义出来的`Vehicle`类只能被`Car`和`Truck`继承。

## instanceof 模式匹配

`instanceof`是Java中的一个关键字，我们在对类型做强制转换之前，会使用`instanceof`做一次判断，例如：
```java
if (animal instanceof Cat) {
    Cat cat = (Cat) animal;
    cat.miaow();
} else if (animal instanceof Dog) {
    Dog dog = (Dog) animal;
    dog.bark();
}
```

Java 14带来了改进版的`instanceof`操作符，这意味着我们可以用更简洁的方式写出之前的代码例子：
```java
if (animal instanceof Cat cat) {
    cat.miaow();
} else if(animal instanceof Dog dog) {
    dog.bark();
}
```

我们都不难发现这种写法大大简化了代码，省略了显式强制类型转换的过程，可读性也大大提高了。

## switch 模式匹配

基于`instanceof`模式匹配这个特性，我们可以使用如下方式来对对象o进行处理：
```java
static String formatter(Object o) {
    String formatted = "unknown";
    if (o instanceof Integer i) {
        formatted = String.format("int %d", i);
    } else if (o instanceof Long l) {
        formatted = String.format("long %d", l);
    } else if (o instanceof Double d) {
        formatted = String.format("double %f", d);
    } else if (o instanceof String s) {
        formatted = String.format("String %s", s);
    }
    return formatted;
}
```

可以看到，这里使用了很多`if-else`，其实，Java中给我们提供了一个多路比较的工具，那就是`switch`，而且从Java 14开始支持`switch`表达式，但`switch`的功能一直都是非常有限的。

在Java 17中，Java的工程师们扩展了`switch`语句和表达式，使其可以适用于任何类型，并允许`case`标签中不仅带有变量，还能带有模式匹配。我们就可以更清楚、更可靠地重写上述代码，例如：
```java
static String formatterPatternSwitch(Object o) {
    return switch (o) {
        case Integer i -> String.format("int %d", i);
        case Long l    -> String.format("long %d", l);
        case Double d  -> String.format("double %f", d);
        case String s  -> String.format("String %s", s);
        default        -> o.toString();
    };
}
```

可以看到，以上的`switch`处理的是一个`Object`类型，而且`case`中也不再是精确的值匹配，而是模式匹配了。

## 虚拟线程

[5、JDK21 中的虚拟线程是怎么回事？](2、相关技术/2、JUC/Hollis/Java并发/5、JDK21%20中的虚拟线程是怎么回事？.md)

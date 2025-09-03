在Java 10之前版本中，我们想定义定义局部变量时。我们需要在赋值的左侧提供显式类型，并在赋值的右边提供实现类型：
```java
MyObject value = new MyObject();
```

在Java 10中，提供了本地变量类型推断的功能，可以通过var声明变量：

```java
var value = new MyObject();
```

本地变量类型推断将引入“var”关键字，而不需要显式的规范变量的类型。

其实，所谓的本地变量类型推断，也是Java 10提供给开发者的语法糖。

虽然我们在代码中使用var进行了定义，但是对于虚拟机来说他是不认识这个var的，在java文件编译成class文件的过程中，会进行解糖，使用变量真正的类型来替代var

我们的项目中，有很多地方为了方便，也用了 var 来声明变量，大家看到的时候不要感到奇怪。﻿
```java
var transferRes = chainFacadeService.transfer(chainProcessRequest);
```


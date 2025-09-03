#序列化 
[4、Java transient关键字使用](2、相关技术/1、Java基础/4、Java%20transient关键字使用.md)
[5、Java 序列化和反序列化的底层原理](2、相关技术/1、Java基础/5、Java%20序列化和反序列化的底层原理.md)
[如何破坏单例模式？](2、相关技术/1、Java基础/Hollis/如何破坏单例模式？.md)

# 典型回答

<font color="red" size=5>序列化是<font color="blue" size=5>将对象转换为可传输格式的过程</font></font>。 是**一种数据的持久化手段**。一般广泛<u>应用于网络传输、持久化到磁盘、RMI、RPC等场景中</u>。几乎所有的商用编程语言都有序列化的能力，不管是数据存储到硬盘，还是通过网络的微服务传输，都需要序列化能力。

在Java的序列化机制中，如果是String，枚举或者实现了`Serializable`接口的类，均可以通过Java的序列化机制，将类序列化为符合编码的数据流，然后通过`InputStream`和`OutputStream`将内存中的类持久化到硬盘或者网络中；同时，也可以通过反序列化机制将磁盘中的字节码再转换成内存中的类。

**如果一个类想被序列化，需要实现`Serializable`接口**。否则将抛出`NotSerializableException`异常。`Serializable`接口没有方法或字段，仅用于标识可序列化的语义。

自定义类通过实现`Serializable`接口做标识，进而在IO中实现序列化和反序列化，<font color="blue" size=5>序列化具体的执行路径如下</font>：
```java
writeObject
-> writeObject0(判断类是否是自定义类) 
-> writeOrdinaryObject(区分Serializable和Externalizable) 
-> writeSerialData(序列化fields【对象中的属性】) 
-> invokeWriteObject(反射调用类自己的序列化策略【自定义的writeObject方法】)
```

其中，**==在`invokeWriteObject`的阶段，系统就会处理自定义类的序列化方案，也就是我们在实现了接口`Serializable`的类中自定义的`writeObject`方法==**。

这是因为，在序列化操作过程中会对类型进行检查，要求被序列化的类必须属于`String`、`Enum`、`Array`、`Serializable`类型其中的任何一种。

# 知识拓展

## Serializable 和 Externalizable 接口有何不同？

类通过实现 `java.io.Serializable` 接口以启用其序列化功能。未实现此接口的类将无法使其任何状态序列化或反序列化。可序列化类的所有子类型本身都是可序列化的。序列化接口没有方法或字段，仅用于标识可序列化的语义。

当试图对一个对象进行序列化的时候，如果遇到不支持 `Serializable` 接口的对象。在此情况下，将抛出 `NotSerializableException`。

如果要序列化的类有父类，要想同时将在父类中定义过的变量持久化下来，那么父类也应该实现`java.io.Serializable`接口。

**==`Externalizable`继承了`Serializable`==**，该接口中定义了两个抽象方法：`writeExternal()`与`readExternal()`。当使用`Externalizable`接口来进行序列化与反序列化的时候需要开发人员重写`writeExternal()`与`readExternal()`方法。**==如果没有在这两个方法中定义序列化实现细节，那么序列化之后，对象内容为空==**。实现 **==`Externalizable`接口的类必须要提供一个`public`的无参的构造器==**。

**==所以，实现`Externalizable`，并实现`writeExternal()`和`readExternal()`方法可以指定序列化哪些属性==**。

### `Externalizable`、`Serializable`对比

|                    | `Externalizable`                                                                                                                                                                       | `Serializable`                                                                                                                                                                                                                                                                                                                                    |
| ------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 接口                 | **==继承自`Serializable`==**；实现类需实现`writeExternal`、`readExternal`方法                                                                                                                       | **==标记接口==**；接口中没有任何内容；实现了该接口的类                                                                                                                                                                                                                                                                                                                   |
| 序列化和反序列化           | **==手动序列化和反序列化==**，通过实现`writeExternal`、`readExternal`对当前类及超类的字段进行序列化和反序列化；                                                                                                             | **==自动序列化和反序列化==**，也可以 **==手动控制序列化和反序列化==**；手动控制需要自定义`writeObject`、`readObject`方法，对当前类及超类的字段**手动**序列化和反序列化                                                                                                                                                                                                                                        |
| 手动控制对象中字段的序列化和反序列化 | **==如果`writeExternal`、`readExternal`方法内容为空，那么序列化后反序列化得到的对象就是空对象==**<br><br><font color="blue">注：writeExternal中向ObjectOut中写入数据的顺序必须和readExternal中从ObjectIn中获取数据的顺序一致</font><br><br><br> | 1. 如果 **==没有自定义`writeObject`、`readObject`方法，默认会自动对非`transient`修饰的字段进行序列化和反序列化；==**<br>2. 如果 **==自定义了`writeObject`、`readObject`方法，就依赖这两个自定义的方法对字段序列化和反序列化==**<br><br>**==如果自定义的`writeObject`、`readObject`方法内容为空，那么反序列化得到的对象是空对象==**<br><br><font color="blue">注：writeObject中向ObjectOutStream中写入数据的顺序必须和readObejct中从ObjectInStream中获取数据的顺序一致</font> |
| 反序列化创建对象原理         | **==反射==**：<br>1. 通过反射调用**公共的无参数构造器**创建空对象<br>2. 调用空对象的`readExternal`方法，从`ObjectInput`中获取数据对属性赋值                                                                                       | **==反射 + Unsafe==**：<br>1. 直接通过`Unsafe`创建空对象<br>2. 给空对象赋值<br><br>若没自定义`readObject`方法，调用默认方法给空对象赋值；<br><br>若自定义了`readObject`方法，通过反射调用`readObject`，从`ObjectInputStream`中获取数据对属性赋值                                                                                                                                                                   |
| 反序列化创建对象是否需要配合构造器  | 实现了`Externalizable`接口的类必须有公共的无参构造                                                                                                                                                      | 反序列化创建对象与构造器无关                                                                                                                                                                                                                                                                                                                                    |
| `transient`        | 不受影响                                                                                                                                                                                   | 若未自定义`writeObject`、`readObject`方法，只有非`transient`声明的字段可以被序列化和反序列化；<br><br>若未自定义`writeObject`、`readObject`方法，则不受影响                                                                                                                                                                                                                                  |
| 序列化、反序列化超类中属性的方式   | 手动在`writeExternal`、`readExternal`中操作超类字段                                                                                                                                               | 两种方式：<br>1. 父类实现`Serializable`接口<br>2. 子类自定义`writeObject`、`readObject`方法，在这两个方法中操作超类字段，父类不实现`Serializable`接口                                                                                                                                                                                                                                      |

## 如果序列化后的文件或者原始类被篡改，还能被反序列化吗？

[30、serialVersionUID 有何用途？如果没定义会有什么问题？](2、相关技术/1、Java基础/Hollis/30、serialVersionUID%20有何用途？如果没定义会有什么问题？.md)

## 在Java中，有哪些好的序列化框架，有什么好处

Java中常用的序列化框架：

`java`、`kryo`、`hessian`、`protostuff`、`gson`、`fastjson`等。

Kryo：速度快，序列化后体积小；跨语言支持较复杂

Hessian：默认支持跨语言；效率不高

Protostuff：速度快，基于protobuf；需静态编译

Protostuff-Runtime：无需静态编译，但序列化前需预先传入schema；不支持无默认构造函数的类，反序列化时需用户自己初始化序列化后的对象，其只负责将该对象进行赋值

Java：使用方便，可序列化所有类；速度慢，序列化后体积大，占空间
对比MySQL架构：[8、说一说MySQL一条SQL语句的执行过程？](2、相关技术/4、数据库-MySQL/Hollis/8、说一说MySQL一条SQL语句的执行过程？.md)

# 典型回答

Java中类的加载阶段分为**加载（Loading）**、**链接（Linking）**、**初始化（Initialization）**。其中**连接**过程又包含了**验证**、**准备**、**解析**。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507311630298.png)

## 加载阶段

加载阶段的目的是 **==将类的`.class`文件加载到JVM中==**。在这个阶段，<font color="red" size=5>JVM根据类的全限定名来获取定义该类的<font color="blue" size=5>二进制字节流</font></font>，并将这个字节流所代表的静态存储结构<font color="red" size=5>转换为方法区的<font color="blue" size=5>运行时数据结构</font></font>。

加载过程会创建一个`java.lang.Class`类的实例来表示这个类。这个`Class`对象作为程序中每个类的**数据访问入口**。

## 链接阶段

在**链接阶段**，Java类加载器对类进行==**验证**、**准备**、**解析**==操作。将类与类的关系（符号引用转为直接引用）确定好，校验字节码

1. **验证**：校验**类的正确性**（文件格式，元数据，字节码，二进制兼容性），保证类的结构符合JVM规范。
2. **准备**：<font color="red" size=5>为类变量分配内存并设置类变量的默认初始值，这些变量使用的内存都在方法区中分配</font>。（这里初始化的是类变量，即static字段，实例变量会在对象实例化时随对象一起分配在Java堆中。）
3. **解析**：把<font color="red" size=5>类的符号引用转为直接引用</font>(类或接口、字段、类方法、接口方法、方法类型、方法句柄和访问控制修饰符7类符号引用 )

## 初始化阶段

初始化是类加载的最后一步，也是真正执行类中定义的 Java 程序代码(字节码)，**==初始化阶段是执行类构造器 `<clinit> ()`方法的过程==**。这里利用了一种懒加载的思想，所有Java虚拟机实现（如HotSpot等）必须在每个类或接口被Java程序首次主动使用时才初始化，但类加载不一定，静态代码块在类初始化时执行
1. 当遇到 `new`、`getstatic`、`putstatic`、`invokestatic` 这4条字节码指令时，比如 new 一个类，读取一个静态字段(未被 final 修饰)、或调用一个类的静态方法时会进行类的初始化
2. 使用 `java.lang.reflect` 包的方法对类进行反射调用时 ，如果类没初始化，需要触发其初始化
3. 初始化一个类，如果其父类还未初始化，则先触发该父类的初始化
4. 当虚拟机启动时，用户需要定义一个要执行的主类 (包含 main 方法的那个类)，虚拟机会先初始化这个类
5. 当使用 JDK1.7 的动态语言时，如果一个 MethodHandle 实例的最后解析结构为 REF_getStatic、REF_putStatic、REF_invokeStatic、的方法句柄，并且这个句柄没有初始化，则需要先触发器初始化

# 扩展知识

## 什么是符号引用和直接引用

**符号引用**（Symbolic Reference）是一种用来<font color="red" size=5>表示引用目标的符号名称</font>，比如类名、字段名、方法名等。<font color="blue" size=5>符号引用与实际的内存地址无关，只是一个标识符，用于描述被引用的目标，类似于变量名</font>。符号引用是在编译期间产生的，在编译后的class文件中存储。

**直接引用**（Direct Reference）是<font color="red" size=5>实际指向目标的内存地址</font>，比如类的实例、方法的字节码等。<font color="blue" size=5>直接引用与具体的内存地址相关，是在程序运行期间动态生成的</font>。

假设有两个类A和B，其中A类中有一个成员变量x，B类中有一个方法foo，其中会调用A类中的成员变量x：
```java
public class A {
    public int x;
}

public class B {
    public void foo() {
        A a = new A();
        a.x = 10;
        System.out.println("x = " + a.x);
    }
}
```

在B类中调用A类的成员变量x时，实际上是通过符号引用来引用A类中的x变量。在解析阶段，Java虚拟机会将A类中的符号引用转换为直接引用，定位到具体的x变量实现，并为B类生成一条指令，用于获取该变量的内存地址。

假设A类的x变量的内存地址为0x1000，在解析阶段，Java虚拟机会为B类生成一条指令，用于获取x变量的内存地址，比如：
```java
getstatic 0x1000
```

这条指令会将0x1000作为直接引用，用于访问A类中的x变量。

**也就是说，在类的解析阶段进行的，Java虚拟机会根据符号引用定位到具体的内存地址，并生成一条指令，用于访问该内存地址。**
#final #finally #finalize 
# 典型回答

`final`、`finally`、`finalize`有什么区别？这个问题就像周杰、周杰伦和周星驰之间有啥关系的问题一样。其实没啥关系，放在一起比较无非是名字有点像罢了。

`final`、`finally`、`finalize`是Java中的三个不同的概念。
- `final`：用于声明变量、方法或类，使之不可变、不可重写或不可继承。
- `finally`：是异常处理的一部分，用于确保代码块（通常用于资源清理）总是执行。
- `finalize`：是Object类的一个方法，用于在对象被垃圾回收前执行清理操作，但通常不推荐使用。

## final

final是一个关键字，可以用来**修饰变量、方法和类**。分别代表着不同的含义。

**final变量**：即我们所说的常量，一旦被赋值后，就不能被修改。
```java
final int x = 100;
// x = 200; // 编译错误，不能修改final变量的值

public static final String AUTHOR_NAME = "Hollis";
```

`final`字段的初始化规则：
1. 实例成员变量：**必须在以下几种时机进行初始化，且只能赋值一次，==否则报错==**
	1. 声明时初始化
	2. 实例初始化块中初始化
	3. 构造函数中初始化
2. 静态成员变量：**必须在以下几种时机进行初始化，且只能赋值一次，==否则报错==**
	1. 声明时初始化
	2. 静态初始化块中初始化

**final方法**：不能被子类重写。
```java
public final void show() {
    // ...
}
```

**final类**：不能被继承。
```java
public final class MyFinalClass {
    // ...
}
```

## finally

finally是一个用于**异常处理**，它和try、catch块一起使用。无论是否捕获或处理异常，finally块中的代码总是执行（程序正常执行的情况）。通常用于关闭资源，如输入/输出流、数据库连接等。
```java
try {
    // 可能产生异常的代码
} catch (Exception e) {
    // 异常处理代码
} finally {
    // 清理代码，总是执行
}
```

[34、finally中代码一定会执行吗？](2、相关技术/1、Java基础/Hollis/34、finally中代码一定会执行吗？.md)

[53、try中return A，catch中return B，finally中return C，最终返回值是什么？](2、相关技术/1、Java基础/Hollis/53、try中return%20A，catch中return%20B，finally中return%20C，最终返回值是什么？.md)

## finalize

`finalize`是`Object`类的一个方法，**用于垃圾收集过程中的资源回收**。**在对象被垃圾收集器回收之前，finalize方法会被调用**，用于执行清理操作（例如释放资源）。但是，不推荐依赖finalize方法进行资源清理，因为它的调用时机不确定且不可靠。
```java
protected void finalize() throws Throwable {
    // 在对象被回收时执行清理工作
}
```

### 关键特性与注意事项
1. **执行时机不确定**
    - 对象不可达后到`finalize()`执行可能有任意延迟
    - 不保证方法会被执行（如JVM快速退出时）
2. **最多执行一次**  
    即使对象在`finalize()`中"复活"，下次回收时也不会再调用该方法
3. **性能影响**  
    有`finalize()`的对象需要至少**两次GC周期**才能回收：
    - 第一次：移入Finalization队列
    - 第二次：真正回收内存
4. **执行线程**  
    由`Finalizer`守护线程（单线程）调用，可能引发：
    - 线程安全问题
    - 长时间阻塞导致队列堆积

### 危险操作："对象复活"示例

```java
public class Zombie {
    static Zombie saved;
    
    @Override
    protected void finalize() {
        System.out.println("Finalized!");
        saved = this; // 对象复活!
    }
    
    public static void main(String[] args) {
        new Zombie();
        System.gc();
        Thread.sleep(1000);  // 等待finalize执行
        System.out.println(saved); // 输出非null!
    }
}
```

**后果**：
1. 对象逃脱回收，但失去`finalize()`保护
2. 可能导致资源泄漏
3. 违反垃圾回收预期行为

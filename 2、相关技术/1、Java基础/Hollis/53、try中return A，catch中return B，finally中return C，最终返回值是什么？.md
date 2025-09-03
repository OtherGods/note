#try-catch-finally #returun 

# 典型回答

最终的返回值将会是 `C` ！ 

[`try-catch-finally` 的 `return` 执行情况](2、相关技术/1、Java基础/Hollis/53、try中return%20A，catch中return%20B，finally中return%20C，最终返回值是什么？.md#`try-catch-finally`%20的%20`return`%20执行情况)

因为`finally`块总是在`try`和`catch`块之后执行，无论是否有异常发生。如果`finally`块中有一个`return`语句，它将覆盖`try`块和`catch`块中的任何`return`语句。
```java
//无异常情况
public static String getValue(){
    try{
        return "A";
    }catch (Exception e){
        return "B";
    }finally {
        return "C";
    }
}

//有异常情况
public static String getValue(){
    try{
        System.out.println(1/0);
        return "A";
    }catch (Exception e){
        return "B";
    }finally {
        return "C";
    }
}
```

所以在这种情况下，无论`try`和`catch`块的执行情况如何，`finally`块中的`return C;`总是最后执行的语句，并且其返回值将是整个代码块的返回值。

这个问题还有一个兄弟问题，那就是如下代码得到的结果是什么：
```java
public static void getValue() {

    int i = 0;

    try {
        i = 1;
    } catch (Exception e) {
        i = 2;
    } finally {
        i = 3;
    }
    System.out.println(i);
}
```

原理和上面的是一样的，最终输出内容为3。

# 扩展知识

## `try-catch-finally` 的执行顺序

```java
try {
    // 1
    System.out.println("try");
    // 2
} catch (Exception e) {
    // 3
    System.out.println("catch");
    // 4
} finally {
    // 5
    System.out.println("finally");
}
// 6
```

执行顺序：
1. `try`没有抛出异常：1、2、5、6
2. `try`抛出异常 且 `catch`没抛出异常：1、3、4、5、6
3. `try`抛出异常 且 `catch`抛出异常：1、3、5
4. `try`抛出异常 但 `catch`未成功捕获：1、5

## `try-with-resources`

在`try-with-resources`语句块执行完后被自动关闭的对象需要实现`AutoCloseable`接口（标记接口）
- 该接口中的`close`方法：抛出一个Exception异常

`try-with-resources`的优点：
1. 自动调用资管关闭方法`void close() throws Exception;`，无需我们手动在`finally`中调用
2. 当 **`try`代码块抛出异常** 且 **资源关闭方法抛出异常**时，抑制close抛出的异常，自动调用`Throwable#addSuppressed`方法将`close`抛出的异常增加到`try`抛出的异常中；可以在 **`try`抛出的异常对象**上使用`Throwable#getSuppressed`方法获取到被抑制的异常数据

关于 **优点2** 可以通过反编译查看，的代码示例：
```java
public class test {
    static class Mytest implements AutoCloseable {
        @Override
        public void close() {
            throw new RuntimeException("内部异常2");
        }
    }
    public static void main(String args[]) {
        try(Mytest my = new Mytest()) {
            throw new RuntimeException("内部异常1");
        }catch(Throwable t) {
            System.out.println(t);
        }
    }
}
```

jdk11反编译后代码如下：（不知道为什么没finally）
```java
public class test {
    static class Mytest implements AutoCloseable {
        public void close() {
            throw new RuntimeException("内部异常2");
        }
        Mytest() {}
    }
    public test(){}
    public static void main(String args[]){
        try {
            Mytest mytest = new Mytest();
            try {
                throw new RuntimeException("内部异常1");
            }
            catch(Throwable throwable1) {
                try {
                    mytest.close();
                }
                // 调用close方法发生异常后抑制该异常，并添
                // 加到外部异常Suppressed中
                catch(Throwable throwable2) {
                    throwable1.addSuppressed(throwable2);
                }
                throw throwable1;
            }
        }
        catch(Throwable throwable) {
            System.out.println(throwable);
        }
    }
}
```

## `finally` 和 `return` 的关系

很多时候，我们的一个方法会通过`return`返回一个值，那么如以下代码：
```java
public static int getValue() {

    int i = 1;

    try {
         i++;
         return i;
    } catch (Exception e) {
        i = 66;
    } finally {
        i = 100;
    }

    return i;
}
```

这个代码得到的结果是`2`，`try-catch-finally`的执行顺序是`try->finally`或者`try-catch-finally`，然后<font color="red" size=5>在执行每一个代码块的过程中，如果<font color="blue" size=5>遇到return那么就会把当前的结果暂存或者覆盖之前的暂存</font>，然后再执行后面的代码块，然后再把之前暂存的结果返回回去。</font>

所以以上代码，会先把`i++`即`2`的结果暂存，然后执行`i=100`，接着再把`2`返回。

但是，<font color="red" size=5>在执行后续的代码块过程中，如果<font color="blue" size=5>遇到了新的 return ，那么之前的暂存结果就会被覆盖</font></font>；如：
```java
public static int getValue() {

    int i = 1;

    try {
         i++;
         return i;
    } catch (Exception e) {
        i = 66;
    } finally {
        i = 100;
        return i;
    }
}
```

以上代码方法得到的结果是`100`，是因为在`finally`中遇到了一个新的`return`，就会把之前的结果给覆盖掉。

如果代码出现异常也同理：
```java
public static int getValue() {

    int i = 1;

    try {
        i++;
        System.out.println(1 / 0);
        return i;
    } catch (Exception e) {
        i = 66;
        return i;
    } finally {
        i = 100;
        return i;
    }
}
```

在try中出现一个异常之后，会执行`catch`，再执行`finally`，最终得到`100`。

如果没有`finally`：
```java
public static int getValue() {

    int i = 1;

    try {
        i++;
        System.out.println(1 / 0);
        return i;
    } catch (Exception e) {
        i = 66;
        return i;
    } 
}
```

那么得到的结果将是`66`。

## `try-catch-finally` 的 `return` 执行情况

finally子句的体主要用于清理资源，**==不要把改变控制流的语句（return、throw、break、continue）放在finally子句中==**

**==代码按照`try-catch-finally`顺序执行==**，`try-catch-finally`中包含的 **`return`执行情况如下**：
1. 如果`finally`块中有`return`语句，则其返回值将是整个`try-catch-finally`结构的返回值。
2. 如果`finally`块中没有`return`语句，则`try`或`catch`块中的`return`语句（取决于哪个执行了）将确定最终的返回值。
	- **返回的变量的值，==只取决于该变量在当前`try`或`catch`块作用域的赋值==**

## `finally`中包含`return`吞异常

从上面知道`finally`中的`return`会覆盖`try`中的`return`，如果try中`return`中返回的是一个方法调用，**由于被`finally`中的`return`覆盖，导致==不会执行`try`中`return`的方法调用==**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508151647209.png)

如果调用`pareInt`方法的时候传递的字符串不是数字类型的字符串，那么在try语句块内的return语句执行的时候就会发生异常；但是由于finally语句块内的return语句会先执行，所以就不会执行到try语句块内的return，这个时候就会出现finally语句块内的return语句 **吞掉** 这个异常！！！

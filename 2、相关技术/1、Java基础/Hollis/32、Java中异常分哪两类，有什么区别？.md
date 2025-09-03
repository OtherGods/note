#异常 #受检异常 #非运行时异常 #非受检异常 #运行时异常 

参考：[53、try中return A，catch中return B，finally中return C，最终返回值是什么？](2、相关技术/1、Java基础/Hollis/53、try中return%20A，catch中return%20B，finally中return%20C，最终返回值是什么？.md)

# 典型回答

Java中的异常，主要可以分为两大类，即**受检异常**（checked exception）和 **非受检异常**（unchecked exception）
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508151149682.png)

**Java中将派生于`Error`和`RuntimeException`类的所有异常称为==非检查异常==**；**其他所有的异常称之为==检查形异常==**

**==非检查形式异常和检查形异常的区别在于==**：异常的处理是取决于环境还是取决于你的代码；如果**取决于环境那么就是==检查形异常==**，如果**取决于你的代码那么就是==非检查形式异常==**。
- **检查型异常不可以由你的代码来避免发生，==通过代码，不能在问题发生之前阻止这样问题的产生==** ，例如我们在程序A中操作一个文件的同时，这个文件可能被我们手动在资源管理器中删除，我们在程序A中不能使用代码来解决这种情况产生的问题。
- **非检查型异常可以由你的代码来避免发生，==通过代码，可以在问题发生前阻止问题发生==**，例如按照开发规范写代码等

## 受检异常

对于 **受检异常** 来说，如果一个方法在声明的过程中证明了其要有受检异常抛出：
`public void test() throws Exception{}`

那么，当我们在程序中调用他的时候，**一定要对该异常进行处理（捕获或者向上抛出），否则是无法编译通过的。这是一种强制规范**。

这种异常在IO操作中比较多。比如 `FileNotFoundException` ，当我们使用IO流处理一个文件的时候，有一种特殊情况，就是文件不存在，所以，在文件处理的接口定义时他会显示抛出`FileNotFoundException`，起目的就是告诉这个方法的调用者，我这个方法不保证一定可以成功，是有可能找不到对应的文件的，你要明确的对这种情况做特殊处理哦。

所以说，当我们希望我们的方法调用者，明确的处理一些特殊情况的时候，就应该使用受检异常。

## 非受检异常

对于**非受检异常**来说，一般是**运行时异常**，**继承自`RuntimeException`、`Error`**。在**编写代码的时候，不需要显式的处理（捕获或抛出），但是如果不处理，在运行期如果发生异常就会中断程序的执行**。

<font color="red" size=5>这种异常一般可以理解为是代码原因导致的</font>。比如发生空指针、数组越界等。所以，只要代码写的没问题，这些异常都是可以避免的。也就不需要我们显示的进行处理。

试想一下，如果你要对所有可能发生空指针的地方做异常处理的话，那相当于你的所有代码都需要做这件事。

# 知识扩展

## 什么是Throwable

`Throwable`是java中最顶级的异常类，继承Object，实现了序列化接口，有两个重要的子类：`Exception` 和 `Error`，二者都是 Java 异常处理的重要子类，各自都包含大量子类。

### 打印线程堆栈

```java
 //打印堆栈轨迹方式1：
StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
for(StackTraceElement s: stackTrace)
{
    System.out.println(s);
}

//打印堆栈轨迹方式2：
Throwable t = new Throwable();
StringWriter out = new StringWriter();
t.printStackTrace(new PrintWriter(out));
String description = out.toString();
System.out.println(description);
```

## Error和Exception的区别和联系

**error表示系统级的错误，是java运行环境内部错误或者硬件问题，不能指望程序来处理这样的问题，可以在代码`try-catch`这样的异常**。如OutOfMemoryError、StackOverflowError这两种常见的错误都是ERROR。

**exception 表示程序需要捕捉、需要处理的异常，是由与程序设计的不完善而出现的问题，程序必须处理的问题**。分为RuntimeException和其他异常

## 常用的受检异常

- **IOException**
	- FileNotFoundException
- **InterruptedException**（线程中断异常）
- **SQLException**（数据库访问异常）
- **ParseException**（日期/数据解析异常）
- **ClassNotFoundException**（反射找不到指定类异常）
- **InvocationTargetException**（反射调用异常）

## 常用的非受检异常（RuntimeException）

这个题目，其实面试官考的还挺多的，主要是考察面试者实战经验是否丰富，所以常见的`RuntimeException`要能回答的尽量多回答一些。

- **NullPointerException**（空指针异常）
- **ArithmeticException**（数学运算错误）
- **IndexOutOfBoundsException**（集合越界）
	- ArrayIndexOutOfBoundsException（数组越界）
- **ClassCastException**（类型强转失败）
- **FileSystemNotFoundException**
- **ConcurrentModificationException**（并发修改异常）
- **IllegalArgumentException**（方法传参非法）
- AnnotationTypeMismatchException
- ArrayStoreException
- BufferOverflowException
- BufferUnderflowException
- CannotRedoException
- CannotUndoException
- CMMException
- DataBindingException
- DOMException
- EmptyStackException
- EnumConstantNotPresentException
- EventException
- FileSystemAlreadyExistsException
- IllegalMonitorStateException
- IllegalPathStateException
- IllegalStateException
- IllformedLocaleException
- ImagingOpException
- IncompleteAnnotationException
- JMRuntimeException
- LSException
- MalformedParameterizedTypeException
- MirroredTypesException
- MissingResourceException
- NegativeArraySizeException
- NoSuchElementException
- NoSuchMechanismException
- ProfileDataException
- ProviderException
- ProviderNotFoundException
- RasterFormatException
- RejectedExecutionException
- SecurityException
- SystemException
- TypeConstraintException
- TypeNotPresentException
- UndeclaredThrowableException
- UnknownEntityException
- UnmodifiableSetException
- UnsupportedOperationException
- WebServiceException
- WrongMethodTypeException

## 说一说个Java异常处理相关的几个关键字，以及简单用法

throws、throw、try、catch、finally
1. try用来指定一块预防所有异常的程序；
2. catch子句紧跟在try块后面，用来指定你想要捕获的异常的类型；
3. finally为确保一段代码不管发生什么异常状况都要被执行；
4. throw语句用来明确地抛出一个异常；
5. throws用来声明一个方法可能抛出的各种异常；

[53、try中return A，catch中return B，finally中return C，最终返回值是什么？](2、相关技术/1、Java基础/Hollis/53、try中return%20A，catch中return%20B，finally中return%20C，最终返回值是什么？.md)

[34、finally中代码一定会执行吗？](2、相关技术/1、Java基础/Hollis/34、finally中代码一定会执行吗？.md)

## 什么是自定义异常，如何使用自定义异常？

自定义异常就是开发人员自己定义的异常，一般通过继承Exception的子类的方式实现。

编写自定义异常类实际上是继承一个API标准异常类，用新定义的异常处理信息覆盖原有信息的过程。

这种用法在Web开发中也比较常见，一般可以用来自定义业务异常。如余额不足、重复提交等。这种自定义异常有业务含义，更容易让上层理解和处理。

## 为什么不建议使用异常控制业务流程

[57、为什么不建议使用异常控制业务流程](2、相关技术/1、Java基础/Hollis/57、为什么不建议使用异常控制业务流程.md)

## 使用异常技巧

1. **捕获异常花费的时间比简单的逻辑判断所花费的时间长**，所以可以用业务逻辑判断就提前阻止异常发生；比如发生空指针之前先判空
2. 不要过分的细化捕获异常，有必要将整个任务包装在一个try语句块中，而**不必把整个任务细化成多个小任务包装在不同的try块**中
3. 抛出异常的时候不要直接抛出最高的父类，应该选择一个合适的异常抛出
4. 不要吞掉异常；比如：`try{……}catch{}`中`catch{}`中为空
5. **早抛出异常，晚捕获异常**；
   - 意思是当可能发生异常的时候要**早点抛出去**，而不是返回一个没有意义的null值，导致之后程序运行的时候抛出一个由于前面抛出的nul值导致的NullPointerException的异常；
   - 晚捕获指的是发生异常的时候要**尽量传递异常**，尽量在最后的时候才捕获异常。
6. 非检查型异常不需要在方法上声明（throws）；检查型异常需要在方法上声明（throws）可能会抛出的检查型异常。



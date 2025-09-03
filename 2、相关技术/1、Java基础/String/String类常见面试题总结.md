String字符串是我们日常工作中常用的一个类，在面试题中也是高频考点，这里精心总结了一波常见，但是烧脑的String面试题，一共5道，从简单到难；本文是基于jdk8版本中的STring进行讨论，文章中的代码运行结果基于java 1.8.0_261-b12

# 1、奇怪的nullnull

下面这段代码会打印什么？

不要改动这段代码的行数，因为在下面的字节码指令的照片中有源文件映射到字节码指令的内容，其中会涉及到源文件的行数。

```java
public class test{

	private static String s1;
	private static String s2;

	public static void main(String[] args){

		String s = s1 + s2;
		System.out.println(s);

	}

}
```

运行之后，你会发现打印了nullnull

> 在分析这个结果之前，先分析一下为null的字符串的打印原理，查看一下PrintStream类的源码，Print方法在打印null前进行了处理：
> ![image-20221010113027705](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221010113027705.png)
>
> 因此，一个为null的字符串就可以被打印在我们的控制台上了。

在这道题中，s1和s2没有经过初始化所以都是空对象null，需要注意是不是字符串 “null”，而是一个空对象，打印结果的产生我们可以看一下字节码文件：

```java
  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=2, locals=2, args_size=1
         0: new           #2                  // class java/lang/StringBuilder
         3: dup
         4: invokespecial #3                  // Method java/lang/StringBuilder."<init>":()V
         7: getstatic     #4                  // Field s1:Ljava/lang/String;
        10: invokevirtual #5                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        13: getstatic     #6                  // Field s2:Ljava/lang/String;
        16: invokevirtual #5                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        19: invokevirtual #7                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
        22: astore_1
        23: getstatic     #8                  // Field java/lang/System.out:Ljava/io/PrintStream;
        26: aload_1
        27: invokevirtual #9                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
        30: return
      LineNumberTable:
        line 8: 0
        line 9: 23
        line 11: 30
```

这里只是把main方法的内容贴过来了，没有贴其他的内容。

反编译test.class文件后的内容：

```java
// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   test.java

import java.io.PrintStream;

public class test
{

    public test()
    {
    }

    public static void main(String args[])
    {
        String s = (new StringBuilder()).append(s1).append(s2).toString();
        System.out.println(s);
    }

    private static String s1;
    private static String s2;
}
```

在字节码指令以及反编译文件中可以看出来：编译器会对String字符串相加的操作进行优化，会把这一过程转换为StringBuilder的append方法。如下为append方法的部分内容：
![image-20221010114409426](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221010114409426.png)

如果append方法的参数字符串为null，那么这里会调用其父类AbstractStringBuilder中的appendNull方法（部分内容如下）：
![image-20221010114546565](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221010114546565.png)

这里的value就是底层用来存储字符的char类型数组，从这里可以看出来在控制台中显示出null，并不是因为print方法中将一个null对象以字符串的形式打印出来，而是因为：StringBuilder对null的字符串进行了特殊的处理，在append的过程中如果碰到的是null字符串，就会以null的形式添加进value字符数组中，这样也就导致了两个为null的字符串相加后会打印为“nullnull”。

# 2、改变String的值

String本质是一个char类型的数组，是final类型的，我们知被final修饰的数据类型在使用的时候能够保证指向该数组地址的引用不能改变，但是数组本身内的值可以被修改。

如下：
![image-20221010140701822](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221010140701822.png)
编译器会报错显示：Cannot assign a value to final  variable 'one'，说明被我final修饰的数组的引用地址是不可改变的，但是下面的代码不会报错：
![image-20221010140836387](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221010140836387.png)

也就是说，即使被final修饰了，但是直接操作数组中的元素还是可以的，所以这里加了一个关键字private，防止从外部进行修改。此外，String类本身也是被添加了final关键字修饰，防止被继承后对属性进行修改。

那么怎么样才能改变一个String的值，同时又不想让它重新指向其他对象呢？可以使用反射修改char数组的值。

![image-20221010142351721](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221010142351721.png)

# 3、创建了几个对象

如下代码到底创建了几个对象：

```java
String s = new String("Hydra");
```

首先要了解三个关于常量池的概念，以下还是基于是jdk8进行说明：

1. class文件常量池：在class文件中保存了一份常量池（Constant Pool），主要存储编译时确定的数据，包括代码中的字面量和符号引用
2. 运行时常量池：位于方法区中，全局共享，class文件常量池中的内容会在类加载后存放到方法的运行时常量池中。除此之外，在运行期间可以将新的变量放入运行时常量池中，相对于class文件而言运行时常量池更具有动态性
3. 字符串常量池，位于堆中，全局共享，这里可以先粗略的认为它存储的是String对象的直接引用，而不是直接存放的对象，具体的实例对象是在堆中存放

可以用一张图来描述它们各自所处的位置：
![image-20221010143538900](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221010143538900.png)

接下来细说以下**字符串常量池**的结构，其实在Hostpot JVM中，字符串常量池 StringTable本质上是一张HashTable，那么当我们说将一个字符串放入字符串常量池的时候，实际上放进入的是什么呢？

## 3.1、用字符串字面量的方式创建

以字面量的方式创建String对象为例，字符串常量池以及堆栈的结构如下图所示（忽略了jvm中的各种OopDesc实例）：
![image-20221010143816254](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221010143816254.png)


*<u>实际上字符串常量池HashTable采用的是数组+链表的结构，链表中的节点是一个个的HashTableEntry，而HashTableEntry中的value则存储了堆上String对象的**引用**。</u>*


那么，下一个问题来了：这个字符串对象的引用是**什么时候**被放到字符串常量池中的？具体可以分为两种情况：

1. 使用字面量声明String对象的时候，也就是被双引号包围的字符串，在堆上创建对象，并**驻留**到字符串常量池中（注意这个用词）
2. 调用intern()方法，当字符串常量池没有相等的字符串时，会保留该字符串的引用。

注意！我们在上面用到了一个词**驻留**，这里对它进行一下规范。<font color = "red">***当我们说驻留一个字符串到字符串常量池时，指的是创建了HahsTbaleEntry，再使它的value指向堆上的String实例，并把HashTableEntry放入字符串常量池，而不是直接把String对象放入字符串常量池中***</font>。简单来说，可以理解为将String对象的引用保存在字符串常量池中。

我们把intern()方法放在后面细说，先主要看第一种情况，这里直接整理引用R大的结论：

> 在类加载阶段，JVM会在堆中创建对应这些class文件常量池中的字符串对象实例，并在字符串常量池中驻留其引用。
>
> 这一过程具体是在resolve阶段（作者的理解是resolution解析阶段）执行，但是并不是立即就创建对象并驻留了引用，因为在JVM规范中声明了resolve阶段可以是lazy的。CONSTANT——String会在第一次引用该项的idc指令被第一次执行到的时候才会resolve。
>
> **<u>就HostSpot VM的实现来说，加载类时字符串字面量会进入到运行时常量池，不会进入全局的字符串常量池，即在StringTable中并没有相应的引用，在堆中也没有对应的对象产生。</u>**

这里大家可以暂时记住这个结论，在后面还会用到。


在弄清楚上面几个概念后，我们可以再回过头来，先看看用字面量声明String的方式，代码如下：
![image-20221012085140237](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012085140237.png)

javap -v test生成的字节码指令内容如下：

```java
public class test
  minor version: 0
  major version: 52
  flags: ACC_PUBLIC, ACC_SUPER
Constant pool:
   #1 = Methodref          #4.#13         // java/lang/Object."<init>":()V
   #2 = String             #14            // Hydra
   #3 = Class              #15            // test
   #4 = Class              #16            // java/lang/Object
   #5 = Utf8               <init>
   #6 = Utf8               ()V
   #7 = Utf8               Code
   #8 = Utf8               LineNumberTable
   #9 = Utf8               main
  #10 = Utf8               ([Ljava/lang/String;)V
  #11 = Utf8               SourceFile
  #12 = Utf8               test.java
  #13 = NameAndType        #5:#6          // "<init>":()V
  #14 = Utf8               Hydra
  #15 = Utf8               test
  #16 = Utf8               java/lang/Object
{
  public test();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 1: 0

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=1, locals=2, args_size=1
         0: ldc           #2                  // String Hydra
         2: astore_1
         3: return
      LineNumberTable:
        line 3: 0
        line 4: 3
}
SourceFile: "test.java"
```

其中main方法中的字节码指令解析如下：

1. <font color = "red">**0: ldc，查找后面索引为#2对应的项，#2表示常量在常量池中的位置。在这个过程中，会触发前面提到的lazy resolve，在resolve过程中如果发现StringTable已经有了内容匹配的String引用，则返回这个引用，反之如果StringTable中没有内容匹配的String对象的引用，则会在堆中创建一个对应内容的String对象，然后在StringTable驻留这个对象引用，并返回这个引用，之后再压入操作数栈中**</font>
2. 2: astore_1，弹出栈顶元素，并将栈顶引用类型值保存到局部变量1中，也就是保存到变量s中
3. 3: return，执行void函数返回

可以看到，在这是种模式下，只有堆中创建了一个“Hydra”对象，在字符串常量池中驻留 它的引用。并且，如果再给字符串s2、s3也用字符串字面量“Hydra”，它们用的都是堆中的唯一这一个对象。


## 3.2、用String类的构造方法的方式创建

以构造方法的方式创建字符串的方式：

![image-20221012091106917](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012091106917.png)

这种方式的字符串常量池以及堆栈的结构如下图所示（忽略了jvm中的各种OopDesc实例）：
![String类的构造方法创建String类的实例对象](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\String类的构造方法创建String类的实例对象.png)



javap -v test后的字节码指令内容如下：

```java
public class test
  minor version: 0
  major version: 52
  flags: ACC_PUBLIC, ACC_SUPER
Constant pool:
   #1 = Methodref          #6.#15         // java/lang/Object."<init>":()V
   #2 = Class              #16            // java/lang/String
   #3 = String             #17            // Hydra
   #4 = Methodref          #2.#18         // java/lang/String."<init>":(Ljava/lang/String;)V
   #5 = Class              #19            // test
   #6 = Class              #20            // java/lang/Object
   #7 = Utf8               <init>
   #8 = Utf8               ()V
   #9 = Utf8               Code
  #10 = Utf8               LineNumberTable
  #11 = Utf8               main
  #12 = Utf8               ([Ljava/lang/String;)V
  #13 = Utf8               SourceFile
  #14 = Utf8               test.java
  #15 = NameAndType        #7:#8          // "<init>":()V
  #16 = Utf8               java/lang/String
  #17 = Utf8               Hydra
  #18 = NameAndType        #7:#21         // "<init>":(Ljava/lang/String;)V
  #19 = Utf8               test
  #20 = Utf8               java/lang/Object
  #21 = Utf8               (Ljava/lang/String;)V
{
  public test();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 1: 0

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=3, locals=2, args_size=1
         0: new           #2                  // class java/lang/String
         3: dup
         4: ldc           #3                  // String Hydra
         6: invokespecial #4                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
         9: astore_1
        10: return
      LineNumberTable:
        line 3: 0
        line 4: 10
}
SourceFile: "test.java"
```

对于上述main方法中字节码指令的解析如下：

1. 0：new，在堆上创建一个String对象，并将它的引用压入操作数栈中，注意这时的对象还只是一个空壳，并没有调用类的构造方法进行初始化
2. 3：dup，复制栈顶元素，也就是复制了上面的对象引用，并将复制后的对象引用压入栈顶。这里之所以要进行复制，是因为之后要执行的构造方法会从操作数栈中弹出需要的参数和这个对象引用本身（这个引用起到的作用就是构造方法中的this指针），如果不进行复制，在弹出后无法得到初始化后的对象引用
3. 4：idc，在堆上创建字符串对象，驻留到字符串常量池，并将字符串的引用压入操作数栈中
   【这一步也就是使用字符串字面量创建字符串对象时字节码指令的第一步】
4. 6：invokespecial，执行String的构造方法，这一步执行完后得到一个完整对象。

到这里，我们可以看到，一共创建了两个对象，并且两个都是在堆上创建的，且字面量方式创建的String对象的引用被驻留到了字符串常量池中。而栈中的s只是一个变量，并不是实际意义上的对象，我们不把他包括在内。

## 3.3、先用字符串字面量的方式创建再用String类的构造方法方式创建

最后先用字面量的方式创建String对象，再使用构造方法创建String时创建了几个对象？

![image-20221012094758319](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012094758319.png)
答案是只创建一个对象，对于这种重复字面量的字符串，看一下反编译后的字节码指令：

```java
public class test
  minor version: 0
  major version: 52
  flags: ACC_PUBLIC, ACC_SUPER
Constant pool:
   #1 = Methodref          #6.#15         // java/lang/Object."<init>":()V
   #2 = String             #16            // Hydra
   #3 = Class              #17            // java/lang/String
   #4 = Methodref          #3.#18         // java/lang/String."<init>":(Ljava/lang/String;)V
   #5 = Class              #19            // test
   #6 = Class              #20            // java/lang/Object
   #7 = Utf8               <init>
   #8 = Utf8               ()V
   #9 = Utf8               Code
  #10 = Utf8               LineNumberTable
  #11 = Utf8               main
  #12 = Utf8               ([Ljava/lang/String;)V
  #13 = Utf8               SourceFile
  #14 = Utf8               test.java
  #15 = NameAndType        #7:#8          // "<init>":()V
  #16 = Utf8               Hydra
  #17 = Utf8               java/lang/String
  #18 = NameAndType        #7:#21         // "<init>":(Ljava/lang/String;)V
  #19 = Utf8               test
  #20 = Utf8               java/lang/Object
  #21 = Utf8               (Ljava/lang/String;)V
{
  public test();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 1: 0

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=3, locals=3, args_size=1
         0: ldc           #2                  // String Hydra
         2: astore_1
         3: new           #3                  // class java/lang/String
         6: dup
         7: ldc           #2                  // String Hydra
         9: invokespecial #4                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
        12: astore_2
        13: return
      LineNumberTable:
        line 3: 0
        line 4: 3
        line 5: 13
}
SourceFile: "test.java"
```

<font color = "red">**可以看到两次执行idc指令时后面索引相同，而idc判断是否需要创建新的String实例的依据是根据在一次执行这条指令时，StringTable是否已经保存了一个对应内容的String实例的引用。所以在第一次执行idc时会创建String实例，而在第二次idc就会直接返回不需要再创建实例了。**</font>

## 3.4、验证

想要验证使用这两种方式创建了几个String类的实例对象的方式很简单，可以使用idea中的debug功能来对比：

1. 字面量创建String方式：
   ![image-20221012093246436](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012093246436.png)
   这个对象数量的计数器似乎在debug时，点击下方右侧Memory的Load clases弹出的。对比语句执行前后可以看到，只创建了一个String对象，以及一个char数组对象，也就是String对象中的value。
2. 构造方法创建String的方式：
   ![image-20221012093444738](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012093444738.png)
   可以看到，创建了两个String对象，一个char数组对象，也说明了两个String中的value指向了同一个char数组对象，符合我们上面从字节码指令角度解释的结果。

# 4、烧脑的intern

上面我们在研究字符串对象的引用如何驻留到字符串常量池中时，还留下了调用intern方法的方式，下面我们来具体分析。

从字面量上理解intern这个单词，作为动词时它有禁闭、关押的意思，通过前面的介绍，与其说是将字符串关押到字符串常量池StringTable中，可能将他理解为缓存它的引用会更加贴切。【这个方法并不会创建一个字符串对象】

<font color = "red">***String的intern是一个本地方法，可以强制的将String驻留入字符串常量池中【并不会创建一个新对象】，可以分为两种情况：***</font>

1. **<font color = "red">如果字符串常量池中已经驻留了一个等于此String对象（调用intern方法的String类的实例对象）内容的字符串引用，则返回此字符串在常量池中的引用。看示例一。</font>**
2. **<font color = "red">否则，在常量池中创建一个引用指向这个String对象；然后返回常量池中的这个引用。看示例二。</font>**

## 4.1、示例一

看一下这段代码，它的运行结果应该是什么？
![image-20221012100503714](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012100503714.png)

输出打印：
![image-20221012100647880](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012100647880.png)

用一张图来描述它们的关系，就很容易明白了：
![image-20221012100730247](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012100730247.png)

其实有了第三题的基础，了解这个结构已经很简单了：

1. 在创建s1的时候，其实堆中已经创建了两个字符串对象StringObject1和StringObject2，并且在在字符串常量池中驻留了StringObject2
2. 当执行s1.intern()方法时，字符串常量池中已经存在内容等于"Hydra"的字符串StringObject2，直接返回这个引用并赋值给s2。
3. s1和s2指向的是两个不同的String对象，因此返回false
4. s2指向的就是驻留在字符串常量池中的StringObject2对象，因此s2 == "Hydra"为true，而s1指向的不是常量池中的对象引用所以返回false。

## 4.2、示例二

上面是常量池中已经存在内容相等的字符串驻留的情况，下面再看看常量池中不存在的情况，如下例子：
![image-20221012105239671](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012105239671.png)
执行结果：true

简单分析一下这个过程：

1. 第一步会在堆上创建"Hy"和"dra"的字符串对象，并驻留到字符串常量池中。（下面的图中都没有显示出来这两个对象）

2. 接下来，完成字符拆拼接的操作，前面我们说过，实际上jvm会把拼接优化成StringBuilder中的append方法，并最终调用toString方法返回一个String对象。在完成字符串的拼接后，字符串常量池中并没有驻留一个内容等于"Hydra"的字符串（我感觉这里的意思应该是：HashTable中并没有驻留一个内容等于"Hydra"的String实例对象的引用）。

   最后在字符串常量池+堆栈中的情况如图所示：
   ![image-20221012110343280](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012110343280.png)

3. 执行s1.intern()时，会在字符串常量池创建一个引用，指向前面StringBuilder创建的哪个字符拆，也就是变量s1所指向的字符串对象。在《深入理解Java虚拟机》这本书中，作者对这里做出了解释，因为从jdk7开始，字符串常量池已经移动到了堆中，那么这里只需要在字符串常量池中记录一下首先出现的实例引用即可。
   ![image-20221012110729496](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012110729496.png)

4. 最后执行String s2 = "Hydra"的时候，发现字符串常量池中已经驻留这个字符串，直接返回对象的引用，因此s1和s2指向的是相同的对象。
   ![image-20221012110840996](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012110840996.png)

## 4.3、1~4小总结

创建String类的实例对象的几种方式以及堆栈和字符串常量池中对象的情况。

1. 仅以String s = "123456"; 的方式创建对象

   1. 在堆中创建一个String类的实例对象
   2. 字符串常量池中的HashTableEntry中的value属性引用a中创建的String类的实例对象

2. 仅以String s = "123" + "456"; 的方式创建对象

   1. 在jdk8中这种方式和上一种方式的结果是一样的

3. 仅以String s= new String("123456"); 的方式创建对象

   1. 在堆中创建一个String类的实例对象，并在字节码指令的最后一步给这个对象初始化
   2. 在堆中以String s = "123456"; 的方式创建一个对象，并将该对象**驻留**到字符串常量池中

4. 仅以String s = new String("123") + new String("456"); 的方式创建对象

   1. 在堆中创建两个String类的实例对象，之后构造一个StringBuilder类的实例对象，调用StringBuilder类中的append方法将这两个字符串拼接到这个StringBuilder对象中，最后调用StringBuilder类中的toString方法转换为String对象存储在堆中

   2. 在堆中以new String("123")和new String("456")的方式创建两个对象，并将它们**驻留**到字符串常量池中；

      在字符串常量池中不会**驻留**4.a中最后创建的内容匹配"123456"的String实例对象的引用。



# 5、还是创建了几个对象

## 5.1、示例一

解决了前面数String对象个数的问题，那么我们接着增加点难度，看以下代码创建了几个对象？

```java
String s = "a" + "b" + "c";
```

答案是：**只创建了一个对象！**可以直观的对比一下源码和反编译后的字节码文件：
![image-20221012114943038](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012114943038.png)

如果使用前面提到的debug技巧，可以直观的看到语句执行完之后，只是增加了一个String对象，以及一个char数组对象，并且这个字符串就是驻留在字符串常量池中的哪一个，如果后面再次使用字面量"abc"的方式声明一个字符串，仍指向的是这一个，堆中String对象的数量不会发生变化。

至于为什么源代码中字符串的拼接操作在编译完成后会消失，直接呈现为一个拼接后的完整字符串，是因为在编译期间，应用了编译器优化中的一种称之为**常量折叠**的技术：

> **常量折叠**：会将<font color = "red">***<u>编译期常量</u>***的加减乘除的运算过程</font>在编译过程中折叠。编译器通过语法分析，会将常量表达式计算求值，并用求出的值替换表达式，不必等到运行期间再进行运算处理，从而在运行期间节省处理器资源。

***上面提到的<u>编译期常量</u>的特点就是：它的值在编译期就可以确定，并且需要完整满足下面要求，才可以是一个编译期常量：***

1. **<font color = "red">被声明为final</font>**
2. **<font color = "red">基本类型或字符串类型</font>**
3. **<font color = "red">声明时就已经初始化</font>**
4. **<font color = "red">使用常量表达式进行初始化</font>**

通过几段代码加深理解：
![image-20221012115914886](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012115914886.png)

执行结果：
![image-20221012115931135](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012115931135.png)

代码中字符串h1和h2都使用常量赋值，区别在于是否使用了final进行修饰，对比编译后的代码，s1进行了折叠而s2没有，可以印证上面的理论，final修饰的字符串变量才有可能是编译期常量。

![image-20221012120221085](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012120221085.png)

这是我实际测试的结果：（用jad test进行反编译）
![image-20221012122927360](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012122927360.png)

## 5.2、示例二

再看一段代码，执行下面的程序结果会返回什么呢？
![image-20221013092620955](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221013092620955.png)

答案是false，因为虽然这里的字符串h2被final修饰，但是初始化时没有使用常量表达式，因此它也不是编译期常量。那么，有小伙伴就要问了，到底什么才是常量表达式呢？

在Orcale官网的文档中，列举了很多情况，下面对常见的情况进行了列举（除了下面这些之外官方文档上还列举了不少情况，如果有兴趣的话，可以自己查看）：

1. 基本类型和Strnig类型的字面量
2. 基本类型和String类型的强制类型转换
3. 使用+、-、！等一元运算符（不包括++和--）进行运算
4. 使用加减运算符+、-，乘除运算符*、/、%进行运算
5. 使用移位运算符>>、<<、>>>进行位移操作

至于从这篇文章一开始提到的字面量（literals），是用于表达源代码中一个固定值的表示法，在Java中创建一个对象时需要使用new关键字，但是给一个基本类型的变量赋值时不需要使用new关键字，这种方式可以称之为字面量。Java中字面量主要包括了以下类型的字面量：
![image-20221013093731025](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221013093731025.png)

再说点题外话，和编译期常量相对的，另一种类型的常量是运行时常量，看一下下面这段代码：
![image-20221013093857536](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221013093857536.png)

编译器能够在编译期就得到s1的值是hello Hydra，不需要等到程序的运行期间，因此s1属于编译期常量。而对于s2来说，虽然也是被声明为final类型，并且在声明时就已经初始化，但是使用的不是常量表达式，因此不属于编译期常量，这一类型的常量也被称为运行时常量。

再看一下编译后的字节码文件的常量池区域：
![image-20221013094731943](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221013094731943.png)

可以看到常量池中只有一个String类型的常量hello Hydra，而s2对应的字符串常量则不在此区域。对于编译器来说，运行时常量在编译期无法进行折叠，编译器只会对尝试修改它的操作进行报错处理。



# 6、总结

本文是基于jdk8进行测试，不同版本的jdk可能会有很大的擦会议，例如jdk6之前，字符串常量池中存储的是String对象实例，而在jdk7以后字符串常量池就改为存储引用，做了非常大的改变。

![image-20221012121636765](D:\Tyora\AssociatedPicturesInTheArticles\String类常见面试题总结\image-20221012121636765.png)



来自知识星球JavaGuide分享的语雀中的《Java面试指北》：[String类常见面试题总结](https://www.yuque.com/books/share/04ac99ea-7726-4adb-8e57-bf21e2cc7183/daifks)










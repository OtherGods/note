#基本数据类型 #类型转换 #字符串类型 #包装类型 #自动拆箱 #拆箱 #自动装箱 #装箱 
# 1、基本数据类型  
  
Java中有哪8中基本数据类型？  
- 6种数字类型：byte、short、int、long、float、double  
- 1种字符类型：char  
- 1种布尔型：boolean。  

8种基本数据类型的默认值以及所占空间的大小：  

| 分类      | 基本类型    | 位数-字节数                                            | 范围                              | 默认值      | 包装类型        | 缓存           |
| ------- | ------- | ------------------------------------------------- | ------------------------------- | -------- | ----------- | ------------ |
| ==整型==  | byte    | 8位（1个字节）                                          | `-2^7 ~ 2^7`                    | `0`      | `Byte`      | `-128 ~ 127` |
| ==整型==  | short   | 16位（2个字节）                                         | `-2^15 ~ 2^15`                  | `0`      | `Short`     | `-128 ~ 127` |
| ==整型==  | int     | 32位（4个字节）                                         | `-2^31 ~ 2^31`                  | `0`      | `Integer`   | `-128 ~ 127` |
| ==整型==  | long    | 64位（8个字节）                                         | `-2^63 ~ 2^63`                  | `0`      | `Long`      | `-128 ~ 127` |
| 布尔型     | boolean | 如果是布尔数组则每个布尔值的长度为8位（1个字节）<br>如果是单个的布尔类型占32位（4个字节） | `-2^7 ~ 2^7`、<br>`-2^31 ~ 2^31` | `false`  | `Boolean`   | true、false   |
| 字符型     | char    | 16位（2个字节）                                         | `-2^31 ~ 2^31`                  | `'\000'` | `Character` | `0 ~ 127`    |
| ==浮点型== | float   | 32位（4个字节）                                         | `-2^128 ~ 2^128`                | `0.0`    | `Float`     | 无            |
| ==浮点型== | double  | 64位（8个字节）                                         | `-2^1024 ~ 2^1024`              | `0.0`    | `Double`    | 无            |
  
对于`boolean`，官方文档未明确定义，它依赖于JVM厂商的具体实现。逻辑上理解是占用1位，但是实际中会考虑计算机高效存储效率因素。  

注意：  
1. **==十六进制==** 数值有一个前缀`0x`或`0X`；**==八进制前==** 有一个0，例如八进制010代表十进制中的8，显然八进制表示法比较容易混淆，所以建议最好不要使用八进制常数；从Java7开始，加上前缀0b或0B就可以写 **==二进制数==**
2. Java中所有的数值类型所占据的字节数与平台无关  
3. Java中整形和浮点形都是有符号形式的

## long类型

Java中使用long数据类型一定要在数值后面加上`L或l`，否则将作为整形解析

## 浮点数类型

Java中的浮点类型用于表示有小数部分的数值。在Java中有两种浮点类型，如下图：  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181316465.png)

**==`double`表示这种类型的数值精度是`float`类型的两倍==**（有人称之为双精度数字）。在很多情况下，`float`类型的精度（`6~7`位有效数字）并不能满足需求。实际上，只有很少的情况适合使用`float`类型，例如需要单精度数的库，或者需要存储大量数据时。`float`类型的数值有一个后缀`F`或`f`，没有后缀`F`的 **==浮点数值总是默认为`double`==**。当然，也可以在浮点数值后面加后缀`D`或者`d`。  

**==float（32位）==** 的内存结构是：  
1. 1位符号位  
2. 8位 **==指数位(数值范围大小)==**：可以表示的浮点数**大小范围为：`-2^128 ~ 2^128`**，也就是：`±3.4028236692093846346337460743177e+38`
3. 23位 **==尾数位（数值精度）==**：也就精度的范围：**`2^23`，得到的十进制数字的长度是==小数点后数字的个数==，就是照片中的有效位 `6~7` 左右**
**==double（64位）==** 也是类似的，精度为float类型的两倍（也是称之为双精度数值）
1. 1位符号位  
2. 11位 **==指数位(数值范围大小)==**：可以表示的浮点数**大小范围为：`-2^1024 ~ 2^1024`**，也就是：`±1.797693134862315907729305190789e+308`
3. 52位 **==尾数位（数值精度）==**：也就精度的范围：**`2^52`，得到的十进制数字的长度是==小数点后数字的个数==，就是照片中的有效位 `15` 位左右**

## char类型

`char`类型原本用于表示单个字符，不过现在情况已经有所变化，至今 **==有些`Unicode`字符可以用一个`char`值描述==**，另 **==一些`Unicode`字符则需要两个`char`值==**。`char`类型的字面量值要用单引号括起来。  

`char`类型的值可以表示为十六进制值，其范围从 $\u0000$ 到 $\uFFFF$。

除了转义序列 $\u$ 之外，还有一些用于表示特殊字符的转义序列，如下图：  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181154453.png)

所有这些转移序列都可以出现在加引号的字符字面量或字符串中，**转义序列 $\u$ 还可以出现在==加引号的字符常量==或者==字符串==之外**（所有其他转义序列都不可以），例如：
```java
// \u005B、\u005D 分别是 [、] 的编码。  
public static void main(String\u005B\u005D args)
```

`Unicode`转义序列会在解析代码之前得到处理，例如:  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181224489.png)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181225569.png)

要是想弄清`char`类型，就必须了解`Unicode`编码机制，在没有`Unicode`之前，已经产生多种不同的标准：美国的`ASCII`、中国的`GB 18030`等，这样就会导致产生以下两问题：  
1. 对任意给定的 **==码点==**，在 **==不同的编码方案有下有可能对应不同的字符==**
2. 采用大写字符集的语言其编码长度可能有所不同，例如：有些常用的字符采用单字节编码，而另一些字符则需要两个或者多个字节  

设计`Unicode`就是为了要解决上面这两个问题。  

<font color = "blue" size=5>码点</font>：一个**编码表中的某个字符对应的代码值**；在`Unicode`标准中，码点采用十六进制书写，并加上前缀`U+`，例如`U+0041`就是拉丁字母A的码点；`Unicode`的码点分为17个代码平面，第一个代码平面称之为**基本平面**，包括码点从`U+0000` 到 `U+FFFF`的代码，其余16个**辅助平面**的码点从`U+10000` 到 `U+10FFFF`，包括辅助字符。

### UTF-16简单介绍

参考：[三、UTF-16简介](Unicode与JavaScript详解#三、UTF-16简介)

UTF-16编码采用不同长度的编码表示所Unicode码点。在**基本多语言平面中，每个字符用16位表示，通常称为**<font color = "blue" size=5>代码单元</font>，而**辅助字符编码为==一对连续的代码单元==**，在基本平面中有 `2^11` 个位置未使用，称之为 **==替代区域==**，来表示16个辅助平面的 `2^20` 个字符，表示方式：
1. 基本平面`U+D800 ~ U+DBFF`用于 **==辅助平面字符第一个代码单元==**（有1024个）
2. 基本平面`U+DC00 ~ U+DFFF`用于 **==辅助平面字符第二个代码单元==**（有1024个）

这样设计十分巧妙，我们可以从中迅速知道一个代码单元是一个字符的编码，还是一个辅助字符的第一部分或者第二部分。例如 ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181253937.png)
 是八元数集的一个数学符号，码点为`U+1D546`，编码为两个代码单元`U+D835`和`U+DD46`；
 
<font color="red" size=5>在Java中char类型描述了UTF-16编码中的一个代码单元。</font>
强烈建议不要在程序中使用char类型，除非确实需要处理UTF-16代码单元。最好将字符串作为抽象数据处理  

### Java中编码

1. Java内部的字符表示
   - 作用层面：**==JVM内存中的字符串表示==**
   - 编码方式：UTF-16（**固定不变**）
   - 为什么固定：**==Java语言规范强制要求，与操作系统无关==**
2. 外部 `I/O` 的默认编码
   - 作用层面：**==程序与外部世界的交互==**
   - 涉及场景：文件读写、字节流转换、控制台输入输出等
   - 为什么平台相关：**==保持与操作系统默认行为一致==**

为什么需要两种编码？
1. **内部统一性**：内存中使用固定UTF-16保证跨平台一致性
2. **外部兼容性**：使用系统默认编码保持与本地环境兼容
    - 中文Windows文本文件默认用GBK保存
    - Linux日志文件默认用UTF-8生成

**==黄金法则==**：
> **==永远不要依赖默认编码！==**
> 在所有 `I/O` 操作中**显式指定字符集**（推荐UTF-8）

## 字符串

Java中所有的字符串都属`CharSequence`接口。从概念上讲，Java字符串就是`Unicode`字符序列，例如：Java中字符串 $Java\u2122$（也就是`Java™`） 由5个`Unicode`字符`J`、`a`、`v`、`a`、`™` 组成。  

在操作String类型的变量的时候，**要明确String类型的变量引用的是一个==String类型的实例对象==还是常量池中的==一个字符串字面量==**。我对下面的代码编译后，查看了它的字节码（使用`javap -v test.calss`）发现：
1. 在`s1`、`s2`、`s3`创建的时候是不会调用`String`类中的构造方法的，所以可以知道在给`String`类型的变量赋值的时候，如果**使用字符串字面量，那么就不会创建`String`类的实例对象**
2. 只有在创建`s4`的时候才会实际创建`String`类的实例对象
3. 在`String`类型的变量之家使用 `==` 进行比较的时候，也要先确定两个`String`类型的变量引用的是 **字符串字面量** 还是 **`String`类的实例对象**：
```java
public class test {  
    public static void main(String[] args) {  
        String s1 = "123";  
        String s2 = "12" + "3";  
        String s3 = "1" + "2" + "3";  
        //String s4 = new String("123");  
    }  
}
```
上述代码中：
1. 值为字符串字面量 `"123"` 和 `new String("123")`的两个不同String类型的变量之间使用 `==` 操作的结果一定是不同的
2. 值为`new String("123")`的两个不同的String类型的变量之间使用 `==` 操作的结果一定是不同的：`new String("123") == new String("123")` 的结果为false
3. 值为字符串字面量 `"123"` 的两个不同的`String`类型的变量之间使用 `==` 操作的结果一定是相同的  

<font color="red" size=5><font color="blue" size=5>代码单元</font>：是char类型的，是16位的unicode字符</font>  
​<font color="red" size=5><font color="blue" size=5>码点</font>：是16位或32位的字符对应的整数</font>  
​
Java字符串由`char`值序列组成，从`char`的介绍中可以看出，**==`char`数据类型是一个采用 `UTF-16编码` 表示的`Unicode`码点的代码单元==**；
- 最常用的`Unicode`字符使用一个代码单元（对应基本平面）可以表示
- 辅助字符需要一对代码单元（对应辅助平面）表示

​涉及到的API：
1. `substring(x,y)`：获取字符串的子串，**子串的范围是==左闭右开==**，索引从0开始（Java字符串中的 **代码单元** 和 **码点** 从0开始计数）
2. `length()`：该方法返回UTF-16编码表示给定字符串所需要的 **==代码单元数量==**，也就是String类中 **==`char`字符数组的长度==**；
3. `codePointCount(int beginIndex,int endIndex)`：返回此`String`的指定文本范围中的 **==`Unicode`码点数量==**，文本范围始于指定的`beginIndex`，一直到索引`endIndex-1`处的`char`。
```java
String s =  "😊";
// 输出 2：指的是字符😊的代码单元的个数
System.out.println(s.length());
// 输出 1：指的是字符😊的码点的个数
System.out.println(s.codePointCount(0, s.length()));
```
4. `charAt(x)`：返回位置n的代码单元（也就是一个`char`，一定是16位的）
5. `codePointAt(x)`：得到x索引对应的码点（一个字符，可能16位也可能32位）
```java
String s =  "😊";  
System.out.println((int) s.charAt(0));
System.out.println((int) s.charAt(1));
System.out.println(s.codePointAt(0));
```

## 数值类型之间的转换  
  
<font color="red" size=5>Java中基本数据类型之间的合法转换图：（图中所有线条的转换都是合法的，不会发生强制类型转换）</font>  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508180947819.png)

- 6个**实线箭头**，表示<font color="red" size=5>无信息丢失的数据转换</font>；
- 3个**虚线箭头**，表示<font color="red" size=5>有精度丢失的转换</font>，并不是强制类型转换；
- <font color="blue" size=5>boolean类型：整形和布尔值之间不能进行相互交换</font>

### 自动类型转换 

例如`123456789`是一个大的整数，转换为`float`类型会丢失精度：
```java
long l = 123456789l;
float f = l;          //1.23456792E8
```
`float`类型的值在输出时用科学计数法表示（[数值输出时自动转换](2、相关技术/1、Java基础/2、Java数据类型总结.md#数值输出时自动转换)），`123456789` 科学计数法表示为：`1.23456789×10^8` ，但是`float`类型小数点后最大只能表示7位，`long`转换为`float`类型后会损失一些精度

当发生上图中的类型转换（无论是实线还是虚线），并且发生了以下三种情况的时候会发生<font color="blue" size=5>自动类型转换</font>
1. 某种类型的变量直接 **==赋值（ `=` ）==** 给另一种类型变量的时候（注意：这里的直接赋值是不会发生强制类型转换的情况，例如：int类型的值赋值给long类型的变量）  
2. **==算术运算符（`+`、`-`、`*`、`/`、`%`）==**连接两个值的时候  
3. **==参数传递==**

下图中二元运算符指的是算术运算符：`+`、`-`、`*`、`/`、`%`
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508180955296.png)

### 强制类型转换  

有两种形式：
1. <font color="red" size=5>显式强制类型转换</font>：在圆括号中给出想要转换的目标类型，后面紧跟待转换的变量名
   示例：
```java
int i = 300;
byte b = (int)i;
```
2. <font color="red" size=5>隐式强制类型转换</font>：赋值运算符中的 `+=`、`-=`、`*=`、`/=`、、`%=`等
   示例：
```java
int i = 1;
i += 3.5;
```

<font color="blue" size=5>强制类型转换</font>指的就是上面那张图中的箭头（实线+虚线）的反向过程；  

例如：将double类型的值转换为int类型的值，在Java中允许这种转换，但是这种转换可能会丢失一些信息，**这种可能丢失信息的转换要通过强制类型转换来完成**。

如果想要对浮点数进行舍入运算，以便得到 **==最接近的整数==**（在很多情况下这种操作更有用），那么就需要使用`Math.round`方法，这个方法返回的类型为`long`类型。  
例如：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181040098.png)

**强制类型转换的操作是**：按位截断，从最低位开始，直到复合目标类型可以表示的范围，例如：(byte)300的实际值为44。
```java
int i = 300;        // 0001 0010 1100‬
byte b = (int)i;    // 强制类型转换为44（int），从最低位开始截取1字节
					// 0010 1100‬
```

**==隐式强制类型转换==**：
- 如果运算符得到一个值，其**类型与左操作数的类型不同，就会发生强制类型转换**
示例：
```java
int i = 1;
i += 3.5;
```

反编译后代码如下：
```java
int i = 1;
i = (int)((double)i + 3.5D);
```

## 科学计数

科学计数法用于简化极大/极小浮点数的表示，格式为：`[基数]e[指数]`，等价于 `a × 10^n`，其中，`1≤ a <10`，而 n 是一个整数。

例如 `1.23e4 = 12300`

### 声明科学计数法字面量

直接赋值时使用 `e` 或 `E`：
```java
double earthMass = 5.972e24;     // 地球质量 ≈5.972×10²⁴ kg
float electronCharge = 1.602e-19f; // 电子电荷量 ≈1.602×10⁻¹⁹ C
```

### 数值输出时自动转换

当数值绝对值 `< 10⁻³` 或 `≥ 10⁷` 时，`toString()` 自动转为科学计数法：
```java
System.out.println(0.000000123); // 输出 1.23E-7
System.out.println(123456789.0); // 输出 1.23456789E8
```

### 强制格式化输出

用 `printf()` 或 `String.format` 控制格式：
```java
double value = 12345.6789;
System.out.printf("%.3e", value); // 输出 1.235e+04（保留3位小数）
```

### 科学计数法字符串转数值

```java
Double.parseDouble() / Float.parseFloat()
```

可直接解析：
```java
String sciStr = "6.022e23";
double avogadro = Double.parseDouble(sciStr); // 阿伏伽德罗常数
```
  
# 2、包装类型  

1. 8种基本数据类型对应的包装类型  
   `Byte`、`Short`、`Integer`、`Long`、`Float`、`Double`、`Character`、`Boolean`。
   <font color="red" size=5>包装器类是不可变的，即一旦构造了包装器类，就不允许更改包装在其中的值</font>（在包装类中的**value字段的是final**类型的），同时包装器**类也是final**，因此不能派生它们的字类  

2. 基本类型和包装类型有什么区别？  
   包装类型不复制就赋值是Null，基本类型有默认值且不是Null。  
   - **基本数据类型直接存在在Java虚拟机栈的局部变量表中**【如果基本数据类型被static修饰，那么也会被存放在堆中，但是不建议这么用】
   - 包装类型属于对象类型，我们知道对象实例都存在于堆中【不是所有的实例对象都存在于堆中，因为HotSpot虚拟机种引入了JIT优化之后，会对对象进行逃逸分析，如果发现某一个独享没有逃逸到方法外部，那么就可能通过标量替换来实现栈上分配，而避免堆上分配内存】。相比于对象类型，基本数据类型占用的空间非常小。  
  
> 局部变量表中主要存放了**编译期可知的基本数据类型**（boolean、byte、char、short、int、float、long、double）、**对象引用**（reference类型，它不同于对象本身，可能是一个指向对象起始地址的引用指针，也可能是指向一个代表对象的句柄或者其他于此对象相关的位置。  
  
## 2.1、包装类型常用的常量池技术  
  
Java基本数据类型的包装类的大部分都实现了常量池技术。这里的<font color="red" size=5>缓存是包装类对应的实例对象的引用，并不是字面量</font>。与String类中的字符串常量池的概念是类似的，在 **==字符串常量池中缓存的是包装类对象的引用，并不是String类的实例对象==**。

注意：常量池技术在基本数据类型的包装类（除了浮点数的包装类）中都有应用，需要注意的是：  
1. 在这几个包装类中**缓存的是对应包装类的实例对象的引用**，并不是相应的字面量；
2. 只有通过`valueOf`方法调用创建的指定范围内的对象才是从缓存中获取的对象；从缓存中获取对象的方式：
   - 手动调用`valueOf`方法
   - 自动装箱（反编译后可以看到调用了`valueOf`方法）
   这也解释了这段代码输出为`false`的原因：`Integer i1 = new Integer(10);Integer i2 = new Integer(10);i1 == i2;`，因为`i1`和`i2`都不是从缓存中获得的
3. **包装类类型的变量引用的只会是对应的类的实例对象，而不能是相应的字面量**，因为会发生自动装箱【无论传递的值是不是在-128到127之间】，导致最终包装类类型的变量的值始终是引用对应的类的实例对象。
 
证明：现有如下代码  
```java  
public class test{  
  public static void main(String[] args){  
        //如果s的值大于127，反编译后得到的文件的内容也是一样的，s变量始终引用的是一个Integer的实例对象，当s的值在-128到127之间的时候会使用IntegerCache中的缓存数组，当不在这个范围内的时候，会创建一个新的Integer类的实例对象；详细内容可以去看笔记《Java数据类型常见面试题总结.md》  
     Integer s = 10;  
  }  
}  
```  
 
这段代码在编译之后，使用`jad test.class`命令可以得到对应的`jad`文件：
```java  
// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.  
// Jad home page: http://www.kpdus.com/jad.html  
// Decompiler options: packimports(3) > // Source File Name:   test.java  
public class test {  
    public test() {}
    public static void main(String args[]) {
        Integer integer = Integer.valueOf(10);  
    }
}
```  
  
`Byte`、`Short`、`Integer`、`Long`这四种包装类默认创建了数值`[-128,127]`的相应类型的缓存数据，`Character`创建了数值在`[0,127]`范围的缓存数据，`Boolean`直接返回`True`、`False`。  

- `Integer`缓存的源码：
```java  
/**  
此方法将始终 缓存-128到127（包括两端的值）范围内的Integer类的实例对象，并且可以缓存此范围之外的其他值。  
*/  
public static Integer valueOf(int i)  
{  
    if (i >= IntegerCache.low && i <= IntegerCache.high) return IntegerCache.cache[i + (-IntegerCache.low)];  
    return new Integer(i);  
}  
  
private static class IntegerCache  
{  
    static final int low = -128;  
    static final int high;
    
    //这个数组会在这个静态内部类中的静态代码块中初始化    
	static final Integer cache[];      
    
    static {
	    ……
	    cache = new Integer[(high - low) + 1];
	    ……
	}
} 
```  
  
- `Character`缓存源码：  
```java  
public static Character valueOf(char c) {  
    if (c <= 127) {   
        // must cache          
return CharacterCache.cache[(int)c];      
    }      
    return new Character(c);  
}

private static class CharacterCache {  
    private CharacterCache() {  
    }  
      
    static final Character cache[] = new Character[127 + 1];  
      
    static {  
        for (int i = 0; i < cache.length; i++) cache[i] = new Character((char) i);  
    }  
}
```  
  
- `Boolean`缓存源码：  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181525139.png)
  
如果超出对应范围仍然会去创建新的对象，缓存的范围区间的大小只是在性能和资源之间的权衡。  
  
两种浮点数类型的包装类`Float`、`Double`没有实现常量池技术。  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181525902.png)

下面我们来看一个问题，下面的代码输出的结果是true还是false？  
```java  
Integer i1 = 40;  
Integer i2 = new Integer(40);  
System.out.println(i1 == i2);  
```  
`Integer i1 = 40;`这段代码会发生自动装箱，也就是说这段代码等价于`Integer i1 = Integer.valurOf(40);` 。因此 **==i1直接使用的是常量池中提前创建好的缓存的Integer的实例对象==**，但是 **==`Integer i2 = new Integer(40);`会直接创建一个`Integer`类的实例对象==**。**因此答案是`false`。**
  
**记住：** 所有整形包装对象之间值的比较，全部使用`equals`方法比较。  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181712431.png)

## 2.2、为什么要有包装类型  
  
Java本身就是一门OOP（面向对象编程）语言，对象可以说出是Java的灵魂。  
  
除了定义一些常量和局部变量之外，我们在其它地方比如方法参数、对象属性中很少会使用基本类型来定义常量。  
  
为什么呢？  
假如你有一个对象中的属性使用了基本类型，那这个属性就必然存在默认值了。这个逻辑是不正确的！因为在很多场景下，对象的某些属性没有赋值的时候，我们就希望它的值是null。  
  
另外像泛型参数不能是基本类型。因为基本类型不是`Object`的字类，应该用基本类型对应的包装类型替代。  
  
# 3、自动拆装箱  
  
## 3.1、什么是自动拆装箱？原理？  
  
基本类型和包装类型之间的相互转换，例如：  
  
```java  
public class test{  
    public static void main(String[] args){       
		Integer i = 10;       //装箱  
		int n = i;       //拆箱  
    }}  
```  
  
使用`jad test.class`反编译得到的`test.class`文件后得到的内容是：  
```java  
// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.  
// Jad home page: http://www.kpdus.com/jad.html  
// Decompiler options: packimports(3) 
// Source File Name:   test.java  

public class test {  
    public test() {}  
    public static void main(String args[]) {        //装箱  
        Integer integer = Integer.valueOf(10);        //拆箱  
        int i = integer.intValue();    
	}
}
```  

从字节码中我们发现 **==装箱是调用了包装类的`valueOf()`方法==**，**==拆箱是调用了包装类的`xxxValue()`方法==**。  

因此：  
1. `Integer i = 10;` 等价于`Integer i = Integer.valueOf(10);`
2. `Integer n = i;` 等价于 `int n = i.intVlaue();`

## 3.2 自动拆装箱发生的时机  
  
1. 自动装箱：调用`valueOf`方法  
   1. 基本数据类型的值 **==赋值==** 给对应包装器类型的变量  
2. 自动拆箱：调用`xxxValue`方法  
	1. 包装器类型的对象 **==赋值==** 给相应的基本数据类型的变量  
	2. **==三目运算符==**：`condition ? 表达式1 : 表达式2`
	   - `condition`中对包装类进行比较大小时会拆箱
	   - `表达式1`和`表达式2`在类型对齐时也会拆箱，类型对齐：
		   - `表达式1` 或 `表达式2`的值 **==只要有一个是原始类型==**
		   - `表达式1` 或 `表达式2`的值的 **==类型不一致，会强制拆箱升级为范围更大的类型==**
	    示例代码：
```java  
   //源码
   Integer n = 1;
   Double x = 2.0;
   double v = n > x ? n : x;
   // 反编译后：
   Integer integer = Integer.valueOf(1);
   Double double1 = Double.valueOf(2D);
   System.out.println((double)integer.intValue() <= double1.doubleValue() ? double1.doubleValue() : integer.intValue());
```
3. 自动装箱 + 拆箱也适用于**算术表达式**。例如，可以将自增运算符应用于一个包装器引用：  
```java  
   Integer n = 3;  
   n++;   
```  
   编译器将**自动的插入一条对象==拆箱指令==，然后进行==自增==计算，==最后再将结果进行装箱==**。  

**强调：** 装箱和拆箱是编译器需要做的工作，不是虚拟机。编译器在生成类字节码的时候会插入必要的方法调用，虚拟机只是执行这些字节码。  
  
## 3.3自动拆箱引发的NPE问题  
  
### 3.3.1 案例1  
  
在《阿里巴巴开发手册》上有这样一条规定：  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181713208.png)
  
从上与种可以看到，有一条是这样说的：**“数据库查询的结果可能是null，因为自动拆箱，用基本数据类型接受有NPE风险”**。  
  
来模拟一个实际的案例：  
```java  
public class AutoBoxTest {  
    @Test  
    void should_Throw_NullPointerException() {  
        long id = getNum();  
    }  
      
    public Long getNum() {  
        return null;  
    }  
}
```  
  
运行代码之后抛出了NPE异常。  
  
为什么会这样呢？我么对`AutoBoxTest.class`进行反编译查看其字节码（推荐使用IDE插件jclasslib来查看类的字节码）  
```java
javap -v AutoBoxTest.class  
```  

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181715191.png)
  
可以发现自动拆箱`Long`——>`long`的过程种，不过就是调用了`longVlaue()`方法而已。  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181716037.png)
  
也就是下面两行代码实际是等价的：  
```java  
long id = getNum();  
long id = getNum().longVlaue();  
```  
  
因为，`getNum()`返回的值为`null`，一个`null`值调用方法，当然会有NPE的问题了。  
  
### 3.3.2 案例2  
  
**三目运算符使用不当会导致诡异的NPE异常**  
  
请你回答下面的代码种会有NPE的问题出现吗？如果有NPE的问题出现的话，原因是什么呢？怎么分析？  
  
```java  
 public class Main {  
    public static void main(String[] args) {  
        Integer i = null;  
        Boolean flag = false;  
        System.out.println(flag ? 0 : i);  
    }  
}
```  
  
答案是：有NPE问题出现。  
  
来看其字节码来搞懂背后的原理（借助IDEA插件jclasslib来查看类字节码）。  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181720832.png)
  
使用`jad test.class`命令反编译后得到的文件内容如下：  
```java  
// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.  
// Jad home page: http://www.kpdus.com/jad.html  
// Decompiler options: packimports(3) // Source File Name:   test.java  
  
import java.io.PrintStream;  
public class test {  
    public test() {}  
    public static void main(String args[]) {  
        Integer integer = null;  
        Boolean boolean1 = Boolean.valueOf(false);  
        System.out.println(boolean1.booleanValue() ? 0 : integer.intValue());  
    }  
}  
```  
  
从字节码中可以看出，22行的位置发生了拆箱操作。  
  
详细解释下就是：`flas ? 0 : i`; 这段代码种，`0`是基本数据类型`int`，返回数据的时候`i`会被强制拆箱成`int`类型的，由于`i`的值是`null`，因此就抛出NPE异常。  
  
如果我们把代码中`flag`变量的值修改为`true`的话，就不会存在NPE问题了，因为会直接返回`0`，不会进行拆箱操作。  
  
我们在实际项目中应该避免这样的写法，正确修改之后的代码如下：  
```java  
public class Main {  
    public static void main(String[] args) {  
        Integer i = null;  
        Boolean flag = false;
        //两者类型一致就不会发生拆箱操作导致NPE问题了
        System.out.println(flag ? new Integer(0) : i);
    }  
}
```  
  
这个问题在《阿里巴巴开发手册》中被提到过：  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508181725889.png)

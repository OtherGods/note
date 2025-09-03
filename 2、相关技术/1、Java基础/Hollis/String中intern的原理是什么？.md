# 典型回答

字符串常量池中的常量有两种来源：

1. 字面量会在编译期先进入到Class常量池，然后再<font color="red" size=5>在运行期进去到字符串池</font>
2. <font color="red" size=5>在运行期通过intern将字符串对象手动添加到字符串常量池中</font>。

对照：[33、字符串常量池是如何实现的？](2、相关技术/3、JVM/Hollis/33、字符串常量池是如何实现的？.md)

> 字面量：[字面量](2、相关技术/1、Java基础/Hollis/14、String%20a%20=%20“ab”;%20String%20b%20=%20“a”%20+%20“b”;%20a%20==%20b%20吗？.md#字面量)

<font color="blue" size=6>intern的作用是这样的：</font>
1. 如果<font color="red" size=5>字符串池中已经存在</font>一个等于该**字符串字面量的对象**，`intern()`方法会<font color="red" size=5>返回这个已存在的对象的引用</font>。
2. 如果<font color="red" size=5>字符串池中没有</font>该字符串字面量的对象，`intern()`方法会<font color="red" size=5>将该字符串对象添加到字符串常量池中，并返回对新添加的字符串对象的引用</font>。【我感觉就是把字符串对象移入常量池中并返回，但是有的地方说是把对象引用放入常量池，我也不清除哪个对】
```java
String s = new String("Hollis") + new String("Chuang");
s.intern();
```

所以，**无论何时通过 `intern()` 方法获取字符串的引用，都会得到字符串池中的引用，这样可以确保相同的字符串在内存中只有一个实例**。

很多人以为知道以上信息，就算是了解intern了，那么请回答一下这个问题：
```java
public static void main(String[] args) {
    String s1 = new String("a"); 
    s1.intern(); 
    String s2 = "a";
    System.out.println(s1 == s2); // false
    
    String s3 = new String("a") + new String("a");
    s3.intern();
    String s4 = "aa";
    System.out.println(s3 == s4);//  true
}
```

大家可以在 JDK 1.7 以上版本中尝试运行以上两段代码，就会发现，`s1 == s2`的结果是 false，但是`s3 == s4`的结果是 true。

这是为什么呢？
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508111641477.png)
注：图中绿色线条代表 string 对象的内容指向。 黑色线条代表地址指向。
- 先看 s3和s4字符串。`String s3 = new String("a") + new String("a");`，这句代码中现在生成了2个最终个对象，是字符串常量池中的 `"a"` 和 JAVA Heap 中的 s3引用指向的对象 `"aa"`。中间还有2个匿名的`new String("a")`我们不去讨论它们。此时s3引用对象内容是 JAVA Heap 中的`"aa"`，但此时常量池中是没有 `"aa"` 对象的。
- 接下来 **`s3.intern();`这一句代码，是==将 s3 引用的 `"aa"` 字符串放入 String 常量池中==，因为此时==常量池中不存在 `"aa"` 字符串，可以直接存储堆中的引用==**。这份引用指向 s3 引用的对象。 也就是说引用地址是相同的。
- 最后`String s4 = "aa";` 这句代码中 `"aa"` 是显示声明的，因此会直接去常量池中创建，创建的时候发现已经有这个对象了，此时也就是指向 s3 引用对象的一个引用。所以 s4 引用就指向和 s3 一样了。因此最后的比较 `s3 == s4` 是 true。
- 再看 s 和 s2 对象。 `String s = new String("a");` 第一句代码，生成了2个对象。常量池中的 `"a"` 和 JAVA Heap 中的字符串对象 `"a"` 。`s.intern();` 这一句是 s 对象去常量池中寻找后发现 `"a"` 已经在常量池里了。
- 接下来`String s2 = "a";` 这句代码是生成一个 s2的引用指向常量池中的 `"a"` 对象。 结果就是 s 和 s2 的引用地址明显不同。图中画的很清晰。

参考：
- [intern的正确用法](13、String%20str=new%20String(“hollis”)创建了几个对象？#intern的正确用法)
- [intern原理](String中intern的原理是什么？#intern原理)（后文所有case均**基于JDK 1.8**运行）
- ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508102332257.png)

# 扩展知识

## 字符串常量进入常量池的时机

[49、字符串常量是什么时候进入到字符串常量池的？](2、相关技术/1、Java基础/Hollis/49、字符串常量是什么时候进入到字符串常量池的？.md)

## intern原理

先看一下上面这篇，了解了字符串常量进入常量池的时机之后，我们再回过头分析一下前面的例子：
```java
public static void main(String[] args) {
    String s1 = new String("a"); // ①
    s1.intern(); // ②
    String s2 = "a";// ③
    System.out.println(s1 == s2); // ④   false
    
    String s3 = new String("a") + new String("a");// ⑤
    s3.intern();// ⑥
    String s4 = "aa";// ⑦
    System.out.println(s3 == s4);// ⑧    true

}
```

这个类被编译后，Class常量池中应该有`"a"`和`"aa"`这两个字符串，这<font color="blue" size=5>两个字符串最终会进到字符串池</font>。但是：
1. 字面量 `"a"` 在代码①这一行，就会被存入字符串池
2. 字面量 `"aa"` 在代码⑦这一行才会存入字符串池
> 所以总结就是：<font color="red" size=5>只有明确自定义的字符串字面量（也包括单纯的字面量拼接，如String s = "a" + "b"），才会进入字符串常量池</font>

以上代码的执行过程：
1. 第①行，new 一个 String 对象，并让 s1指向他。
2. 第②行，**对 s1执行 intern，但是因为 `"a"` 这个字符串已经在字符串池中，所以会直接返回原来的引用，但是并没有赋值给任何一个变量**。
3. 第③行，s2指向常量池中的 `"a"` ；

**所以，s1和 s2并不相等！**

4. 第⑤行，new 一个 String 对象，并让 s3 指向他。
5. 第⑥行，对 s3 执行 intern，但是目前 **字符串池中还没有 `"aa"` 这个字符串，于是会把 `<s3指向的String对象的引用>` 放入 `<字符串常量池>`**
6. 第⑦行，因为 `"aa"` 这个字符串已经在字符串池中，所以会直接返回原来的引用，并赋值给 s4；

**所以，s3和 s4 相等！**

而如果我们对代码稍作修改：
```java
String s3 = new String("a") + new String("a");// ①
String s4 = "aa";// ②
s3.intern();// ③
System.out.println(s3 == s4);// ④
```

以上代码得到的结果则是：false
1. 第①行，new一个 String 对象，并让 s3 指向他；JAVA Heap中存储 `"aa"` 对象，字符串常量池中存储 `"a"` 对象。
2. 第②行，创建一个字符串 `aa` ，并且因为它**是字面量，字符串常量池中不存在该对象，所以把他放到字符串池**。
3. 第③行，对 s3 执行 `intern`，但是目前字符串池中已经有 `"aa"` 这个字符串，所以会直接返回s的引用，但是并没有对s3进行赋值
4. 第④行，s3引用JAVA Heap中的`"aa"`，s4引用常量池中的`"aa"`；所以，s3和 s4 不相等。

## a和1有什么不同

关于这个问题，我们还有一个变型，可以帮大家更好的理解intern，请大家分别在JDK 1.8和JDK 11及以上的版本中执行以下代码：
```java
String s3 = new String("1") + new String("1");// ①
s3.intern();// ②
String s4 = "11";
System.out.println(s3 == s4);// ③
```

你会发现，在JDK 1.8中，以上代码得到的结果是true，而JDK 11及以上的版本中结果却是false。（有人反馈自己代码执行和我文中的不一样，可能的原因有很多，比如JDK版本不同、操作系统不同、本地编译过的其他代码也有影响等。故而如果现象不一致，可以使用一些在线的Java代码执行工具测试，如：[https://www.bejson.com/runcode/java/](https://www.bejson.com/runcode/java/) 。）

那么，再稍作修改呢？在目前的所有JDK版本中，执行以下代码：
```java
String s3 = new String("3") + new String("3");// ①
s3.intern();// ②
String s4 = "33";
System.out.println(s3 == s4);// ③
```

得到的结果也是true，你知道为什么嘛？

答案在下文中：

[56、为什么这段代码在JDK不同版本中结果不同](2、相关技术/1、Java基础/Hollis/56、为什么这段代码在JDK不同版本中结果不同.md)

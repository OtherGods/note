#SubList是ArrayList内部类 #ArrayList中的subList方法返回SubList对象 #SubList类内部保存了ArrayList对象的引用、ArrayList底层数组的引用、起始偏移量、结束偏移量 

# 典型回答

List的subList方法**并没有创建一个新的List，而是使用了原List的视图，这个视图使用内部类SubList表示**。所以，我们不能把subList方法返回的List强制转换成ArrayList等类，因为他们之间没有继承关系。正如阿里巴巴Java编码规范所说：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507081951898.png)

另外，视图和原List的修改还需要注意几点，尤其是他们之间的相互影响：

1. 对父(sourceList)子(subList)List做的**非结构性修改**（non-structural changes），都会影响到彼此。
2. 对子List做**结构性修改**，操作同样会反映到父List上，同时更新子List的modCount为父List的modCount
3. 对父List做**结构性修改**，再访问子List会抛出异常ConcurrentModificationException，因为同时更新父List的modCount不会同步更新子List的modCount；
   因为每次访问子SubList都会判断与父SubList是否一致
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507082037122.png)

> <font size=5 color="red"><font color="blue">结构性修改</font>：在集合中增加或者删除元素</font>
> <font size=5 color="red"><font color="blue">非结构性</font>：在集合中修改某个元素的内容</font>

# 扩展知识

subList是List接口中定义的一个方法，该方法主要用于返回一个集合中的一段、可以理解为截取一个集合中的部分元素，他的返回值也是一个List。

如以下代码：
```java
public static void main(String[] args) {
    List<String> names = new ArrayList<String>() {{
        add("Hollis");
        add("hollischuang");
        add("H");
    }};

    List subList = names.subList(0, 1);
    System.out.println(subList);
}
```

以上代码输出的结果为：
```java
[Hollis]
```

如果我们改动下代码，将subList的返回值强转成ArrayList试一下：
```java
public static void main(String[] args) {
    List<String> names = new ArrayList<String>() {{
        add("Hollis");
        add("hollischuang");
        add("H");
    }};

    ArrayList subList = names.subList(0, 1);
    System.out.println(subList);
}
```

以上代码将抛出异常：
```java
java.lang.ClassCastException: java.util.ArrayList$SubList cannot be cast to java.util.ArrayList
```

不只是强转成ArrayList会报错，强转成LinkedList、Vector等List的实现类同样也都会报错。

那么，为什么会发生这样的报错呢？我们接下来深入分析一下。

## 底层原理

首先，我们看下subList方法给我们返回的List到底是个什么东西，这一点在JDK源码中注释是这样说的：

> Returns a view of the portion of this list between the specifiedfromIndex, inclusive, and toIndex, exclusive.

也就是说subList 返回是一个视图，那么什么叫做视图呢？

我们看下subList的源码：
```java
public List<E> subList(int fromIndex, int toIndex) {
    subListRangeCheck(fromIndex, toIndex, size);
    return new SubList(this, 0, fromIndex, toIndex);
}
```

这个方法返回了一个SubList，这个类是ArrayList中的一个内部类。

SubList这个类中单独定义了set、get、size、add、remove等方法。

当我们调用subList方法的时候，会通过调用SubList的构造函数创建一个SubList，那么看下这个构造函数做了哪些事情：
```java
SubList(AbstractList<E> parent,
        int offset, int fromIndex, int toIndex) {
    this.parent = parent;
    this.parentOffset = fromIndex;
    this.offset = offset + fromIndex;
    this.size = toIndex - fromIndex;
    this.modCount = ArrayList.this.modCount;
}
```

可以看到，这个构造函数中把原来的List以及该List中的部分属性直接赋值给自己的一些属性了。

也就是说，<font color="red" size=5>SubList并没有重新创建一个List，而是<font color="blue" size=5>直接引用了原有的List（返回了父类的视图）</font>，只是指定了一下他要使用的元素的范围而已（从fromIndex（包含），到toIndex（不包含））。</font>

在SubList类中，初始化的时候保存了此时此刻的modCount到SubList实例对象中，在每次访问这个对象中的方法时会校验此时此刻SubList对象创建时保存的modCount与它父List此时此刻的modCount是否一致，不一致抛异常ConcurrentModificationException。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507082037122.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507082040076.png)

所以，为什么不能将subList方法得到的集合直接转换成ArrayList呢？**因为SubList只是ArrayList的内部类，他们之间并没有继承关系，故无法直接进行强制类型转换**。

## 视图有什么问题

前面通过查看源码，我们知道，subList()方法并没有重新创建一个ArrayList，而是返回了一个ArrayList的内部类——SubList。

这个SubList是ArrayList的一个视图。

那么，这个视图又会带来什么问题呢？我们需要简单写几段代码看一下。

1、**非结构性改变SubList**
```java
public static void main(String[] args) {
    List<String> sourceList = new ArrayList<String>() {{
        add("H");
        add("O");
        add("L");
        add("L");
        add("I");
        add("S");
    }};

    List subList = sourceList.subList(2, 5);

    System.out.println("sourceList ： " + sourceList);
    System.out.println("sourceList.subList(2, 5) 得到List ：");
    System.out.println("subList ： " + subList);

    subList.set(1, "666");

    System.out.println("subList.set(1,666) 得到List ：");
    System.out.println("subList ： " + subList);
    System.out.println("sourceList ： " + sourceList);

}
```

得到结果：
```java
sourceList ： [H, O, L, L, I, S]
sourceList.subList(2, 5) 得到List ：
subList ： [L, L, I]
subList.set(1,666) 得到List ：
subList ： [L, 666, I]
sourceList ： [H, O, L, 666, I, S]
```

当我们尝试通过set方法，*改变subList中某个元素的值得时候，我们发现，原来的那个List中对应元素的值也发生了改变*。

同理，如果我们使用同样的方法，对sourceList中的某个元素进行修改，那么subList中对应的值也会发生改变。读者可以自行尝试一下。

2、**结构性改变SubList**

```java
public static void main(String[] args) {
    List<String> sourceList = new ArrayList<String>() {{
        add("H");
        add("O");
        add("L");
        add("L");
        add("I");
        add("S");
    }};

    List subList = sourceList.subList(2, 5);

    System.out.println("sourceList ： " + sourceList);
    System.out.println("sourceList.subList(2, 5) 得到List ：");
    System.out.println("subList ： " + subList);

    subList.add("666");

    System.out.println("subList.add(666) 得到List ：");
    System.out.println("subList ： " + subList);
    System.out.println("sourceList ： " + sourceList);

}
```

得到结果：
```java
sourceList ： [H, O, L, L, I, S]
sourceList.subList(2, 5) 得到List ：
subList ： [L, L, I]
subList.add(666) 得到List ：
subList ： [L, L, I, 666]
sourceList ： [H, O, L, L, I, 666, S]
```

我们尝试*对subList的结构进行改变，即向其追加元素，那么得到的结果是sourceList的结构也同样发生了改变*。

3、**结构性改变原List**

```java
public static void main(String[] args) {
    List<String> sourceList = new ArrayList<String>() {{
        add("H");
        add("O");
        add("L");
        add("L");
        add("I");
        add("S");
    }};

    List subList = sourceList.subList(2, 5);

    System.out.println("sourceList ： " + sourceList);
    System.out.println("sourceList.subList(2, 5) 得到List ：");
    System.out.println("subList ： " + subList);

    sourceList.add("666");

    System.out.println("sourceList.add(666) 得到List ：");
    System.out.println("sourceList ： " + sourceList);
    System.out.println("subList ： " + subList);

}
```

得到结果：
```java
Exception in thread "main" java.util.ConcurrentModificationException
    at java.util.ArrayList$SubList.checkForComodification(ArrayList.java:1239)
    at java.util.ArrayList$SubList.listIterator(ArrayList.java:1099)
    at java.util.AbstractList.listIterator(AbstractList.java:299)
    at java.util.ArrayList$SubList.iterator(ArrayList.java:1095)
    at java.util.AbstractCollection.toString(AbstractCollection.java:454)
    at java.lang.String.valueOf(String.java:2994)
    at java.lang.StringBuilder.append(StringBuilder.java:131)
    at com.hollis.SubListTest.main(SubListTest.java:28)
```

我们尝试对sourceList的结构进行改变，即向其追加元素，结果发现抛出了ConcurrentModificationException。

## 如何创建新的List

如果需要对subList作出修改，又不想动原list。那么可以创建subList的一个拷贝：
```java
subList = Lists.newArrayList(subList);
list.stream().skip(start).limit(end).collect(Collectors.toList());
```

在java中，类之间有各种关系，例如继承、实现、依赖、关联、聚合和组合。

尤其是关联、聚合和组合之间有点分不清他们之间的区别，今天通过这篇文章，带领大家彻底掌握他们之间的区别和联系。

## **1、继承**（泛化）

继承是面向对象最显著的一个特性。继承是从已有的类（父类、父接口）中派生出新的类（子类、子接口），新的类能吸收已有类的数据属性和行为，并能扩展新的能力。在Java中此类关系通过关键字extends明确标识。

例如你可以先定义一个类叫动物(Animal), 然后再定义一个子类鸟(Bird), 子类鸟具有父类Animal的一切属性和行为，同时还可以扩展自己独特的属性和行为。

一般用一个带空心箭头的实线表示继承关系，用UML图表示如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507232111341.png)

## **2、实现**

实现是类和接口之间最常见的关系。指的是一个类实现接口的功能（一个类可以实现多个接口).在Java中此类关系通过关键字implements明确标识。

例如定义一个接口Fly(表示会飞），然后定义一个类Bird实现该接口。

一般用一个带空心箭头的虚线表示实现关系，用UML表示如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507232111300.png)

## **3、依赖**

依赖关系是指一个类对另外一个类的依赖。这种关系是一种非常弱、临时性的关系。依赖关系在Java语言中体现为局域变量、方法的形参，或者对静态方法的调用。

比如说Employee类中有一个方法叫做TakeMoney(Bank bank)这个方法，在这个方法的参数中用到了Bank这个类。那么这个时候可以说Employee类依赖了Bank这个类，如果Bank这个类发生了变化那么会对Employee这个类造成影响。

一般用一条指向被依赖事物的虚线表示依赖关系，用UML图表示依赖关系如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507232109371.png)

设计模式教材示例：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507232109854.png)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507232110651.png)

## **4、关联**

关联关系是类与类之间的联接，它使一个类知道另一个类的属性和方法。关联可以是双向的，也可以是单向的，它是依赖关系更强的一种关系。

在java语言中，关联关系一般表现为被关联类B以类属性的形式出现在关联类A中，也可能是关联类A引用了一个类型为被关联类B的全局变量；

一般用实线连接有关联的两个类，用UML图表示如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507232105879.png)

## **5、聚合**

聚合是一种特殊的关联关系，它是较强的一种关联关系，强调的是整体与部分之间的关系，从语法上是没办法区分的，只能从语义上区分。

例如雁群和大雁的关系、学校和学生之间的关系。

聚合的整体和部分之间在生命周期上没有什么必然的联系，部分对象可以在整体对象创建之前创建，也可以在整体对象销毁之后销毁。

一般用带一个空心菱形（整体的一端）的实线表示，用UML图表示如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507232103427.png)

设计模式教材示例：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507232103689.png)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507232104171.png)

## **6、组合**

组合也是关联关系的一种特例，这种关系比聚合关系更强。它强调了整体与部分的生命周期是一致的，而聚合的整体和部分之间在生命周期上没有什么必然的联系。

在组合关系中，整体与部分是不可分的，整体的生命周期结束也就意味着部分的生命周期结束。

例如大雁和大雁的翅膀、人的头和嘴(头没了嘴巴也没了)是组合关系。一般用带实心菱形(整体的一端)的实线来表示。UML图如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507232101014.png)

设计模式教材示例：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507232059550.png)

## 7、总结

对于继承、实现这两种关系比较简单，他们体现的是一种类与类、或者类与接口间的纵向关系；其他的四者关系则体现的是类与类、或者类与接口间的引用、横向关系。

**总的来说，这几种关系所表现的强弱程度依次为：组合>聚合>关联>依赖**
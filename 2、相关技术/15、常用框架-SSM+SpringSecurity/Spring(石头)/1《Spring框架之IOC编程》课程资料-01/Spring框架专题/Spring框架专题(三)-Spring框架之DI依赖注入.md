![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229124353219.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70#pic_center)

[TOC]



# 1.前言

控制反转（`IoC`）是一种思想，而依赖注入（`Dependency Injection`）则是实现这种思想的方法

其实泛概念上两者是接近的，可以简单的理解为一个概念的不同角度描述

我们前面写程序的时候，通过控制反转，使得 Spring 可以创建对象，这样减低了耦合性，但是每个类或模块之间的依赖是不可能完全消失的，而这种依赖关系，我们可以完全交给 spring 来维护。

# 2.注入分类
bean 实例在调用无参构造器创建对象后，就要对 bean 对象的属性进行初始化。初始化是由容器自动完成的，称为注入。

根据注入方式的不同，常用的有两类：`set 注入`、`构造注入`。

## 2.1.set 注入(掌握)
set 注入也叫设值注入是指，通过 setter 方法传入被调用者的实例。这种注入方式简单、直观，因而在 Spring 的依赖注入中大量使用。

**A、简单类型**

```java
public class School {

    private String name;
    private String address;

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "School{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
```

**B、beans.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!--声明student对象
        注入：就是赋值的意思
        简单类型： spring中规定java的基本数据类型和String都是简单类型。
        di:给属性赋值
        1. set注入（设值注入） ：spring调用类的set方法， 你可以在set方法中完成属性赋值
         1）简单类型的set注入
            <bean id="xx" class="yyy">
               <property name="属性名字" value="此属性的值"/>
               一个property只能给一个属性赋值
               <property....>
            </bean>
    -->
    <bean id="myStudent" class="com.ba01.Student" >
        <property name="name" value="李四lisi" /><!--setName("李四")-->
        <property name="age" value="22" /><!--setAge(21)-->
        <property name="email" value="lisi@qq.com" /><!--setEmail("lisi@qq.com")-->
    </bean>

</beans>
```
**C、beans.xml**

```java
   @Test
    public void test01(){
       System.out.println("=====test01========");
       String config="ba01/applicationContext.xml";
       ApplicationContext ac = new ClassPathXmlApplicationContext(config);

       //从容器中获取Student对象
       Student myStudent =  (Student) ac.getBean("myStudent");
       System.out.println("student对象="+myStudent);
   }
```
创建 `java.util.Date` 并设置初始的日期时间：

**Spring 配置文件：**

```xml
 <bean id="mydate" class="java.util.Date">
     <property name="time" value="8364297429" /><!--setTime(8364297429)-->
 </bean>
```
**测试方法：**

```java
   @Test
    public void test01(){
       System.out.println("=====test01========");
       String config="ba01/applicationContext.xml";
       ApplicationContext ac = new ClassPathXmlApplicationContext(config);

       //从容器中获取Student对象
       Student myStudent =  (Student) ac.getBean("myStudent");
       System.out.println("student对象="+myStudent);


       Date myDate = (Date) ac.getBean("mydate");
       System.out.println("myDate="+myDate);

   }
```
**B、引用类型**

当指定 bean 的某属性值为另一 bean 的实例时，通过 ref 指定它们间的引用关系。ref的值必须为某 bean 的 id 值。

```java
public class School {

    private String name;
    private String address;

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "School{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
```

```java
public class Student {

    private String name;
    private int age;

    //声明一个引用类型
    private School school;


    public Student() {
        System.out.println("spring会调用类的无参数构造方法创建对象");
    }

    // 包名.类名.方法名称
    public void setName(String name) {
        System.out.println("setName:"+name);
        this.name = name;
    }

    public void setAge(int age) {
        System.out.println("setAge:"+age);
        this.age = age;
    }

    public void setSchool(School school) {
        System.out.println("setSchool:"+school);
        this.school = school;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", school=" + school +
                '}';
    }
}
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!--声明student对象
        注入：就是赋值的意思
        简单类型： spring中规定java的基本数据类型和String都是简单类型。
        di:给属性赋值
        1. set注入（设值注入） ：spring调用类的set方法， 你可以在set方法中完成属性赋值
         1）简单类型的set注入
            <bean id="xx" class="yyy">
               <property name="属性名字" value="此属性的值"/>
               一个property只能给一个属性赋值
               <property....>
            </bean>

         2) 引用类型的set注入 ： spring调用类的set方法
           <bean id="xxx" class="yyy">
              <property name="属性名称" ref="bean的id(对象的名称)" />
           </bean>
    -->
    <bean id="myStudent" class="com.ba02.Student" >
        <property name="name" value="李四" />
        <property name="age" value="26" />
        <!--引用类型-->
        <property name="school" ref="mySchool" /><!--setSchool(mySchool)-->
    </bean>

    <!--声明School对象-->
    <bean id="mySchool" class="com.ba02.School">
        <property name="name" value="北京大学"/>
        <property name="address" value="北京的海淀区" />
    </bean>
</beans>
```
测试方法：

```java
 @Test
  public void test01(){
     System.out.println("=====test01========");
     String config="ba02/applicationContext.xml";
     ApplicationContext ac = new ClassPathXmlApplicationContext(config);

     //从容器中获取Student对象
     Student myStudent =  (Student) ac.getBean("myStudent");
     System.out.println("student对象="+myStudent);
 }
```
## 2.2.set 构造注入(理解)
构造注入是指，在构造调用者实例的同时，完成被调用者的实例化。即，使用构造器设
置依赖关系。

**举例 1：**

```java
    /**
     * 创建有参数构造方法
     */
    public Student(String myname,int myage, School mySchool){
        System.out.println("=====Student有参数构造方法======");
        //属性赋值
        this.name  = myname;
        this.age  = myage;
        this.school = mySchool;

    }
```
spring配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!--声明student对象
        注入：就是赋值的意思
        简单类型： spring中规定java的基本数据类型和String都是简单类型。
        di:给属性赋值
        1. set注入（设值注入） ：spring调用类的set方法， 你可以在set方法中完成属性赋值
         1）简单类型的set注入
            <bean id="xx" class="yyy">
               <property name="属性名字" value="此属性的值"/>
               一个property只能给一个属性赋值
               <property....>
            </bean>

         2) 引用类型的set注入 ： spring调用类的set方法
           <bean id="xxx" class="yyy">
              <property name="属性名称" ref="bean的id(对象的名称)" />
           </bean>

        2.构造注入：spring调用类有参数构造方法，在创建对象的同时，在构造方法中给属性赋值。
          构造注入使用 <constructor-arg> 标签
          <constructor-arg> 标签：一个<constructor-arg>表示构造方法一个参数。
          <constructor-arg> 标签属性：
             name:表示构造方法的形参名
             index:表示构造方法的参数的位置，参数从左往右位置是 0 ， 1 ，2的顺序
             value：构造方法的形参类型是简单类型的，使用value
             ref：构造方法的形参类型是引用类型的，使用ref
    -->


    <!--使用name属性实现构造注入-->
    <bean id="myStudent" class="com.ba03.Student" >
        <constructor-arg name="myage" value="20" />
        <constructor-arg name="mySchool" ref="myXueXiao" />
        <constructor-arg name="myname" value="周良"/>
    </bean>

    <!--使用index属性-->
    <bean id="myStudent2" class="com.ba03.Student">
        <constructor-arg index="1" value="22" />
        <constructor-arg index="0" value="李四" />
        <constructor-arg index="2" ref="myXueXiao" />
    </bean>

    <!--省略index-->
    <bean id="myStudent3" class="com.ba03.Student">
        <constructor-arg  value="张强强" />
        <constructor-arg  value="22" />
        <constructor-arg  ref="myXueXiao" />
    </bean>
    <!--声明School对象-->
    <bean id="myXueXiao" class="com.ba03.School">
        <property name="name" value="清华大学"/>
        <property name="address" value="北京的海淀区" />
    </bean>

</beans>
```
<constructor-arg />标签中用于指定参数的属性有：
➢ name：指定参数名称。
➢ index：指明该参数对应着构造器的第几个参数，从 0 开始。不过，该属性不要也行，
但要注意，若参数类型相同，或之间有包含关系，则需要保证赋值顺序要与构造器中的参数顺序一致。


举例 2：
使用构造注入创建一个系统类 File 对象

```xml
<!--创建File,使用构造注入-->
<bean id="myfile" class="java.io.File">
    <constructor-arg name="parent" value="D:\course\JavaProjects\spring-course\ch01-hello-spring" />
    <constructor-arg name="child" value="readme.txt" />
</bean>
```
**测试类:**
```java
   @Test
    public void test01(){
       System.out.println("=====test01========");
       String config="ba03/applicationContext.xml";
       ApplicationContext ac = new ClassPathXmlApplicationContext(config);

       //从容器中获取Student对象
       Student myStudent =  (Student) ac.getBean("myStudent");
       System.out.println("student对象="+myStudent);

       File myFile = (File) ac.getBean("myfile");
       System.out.println("myFile=="+myFile.getName());

   }
```

## 2.3.引用类型属性自动注入
对于引用类型属性的注入，也可不在配置文件中显示的注入。可以通过为`<bean/>`标签
设置 autowire 属性值，为引用类型属性进行隐式自动注入（默认是不自动注入引用类型属
性）。根据自动注入判断标准的不同，可以分为两种：

```java
byName：根据名称自动注入
byType： 根据类型自动注入
```
### 2.3.1.byName 方式自动注入
当配置文件中被调用者 bean 的 id 值与代码中调用者 bean 类的属性名相同时，可使用byName 方式，让容器自动将被调用者 bean 注入给调用者 bean。容器是通过调用者的 bean类的属性名与配置文件的被调用者 bean 的 id 进行比较而实现自动注入的。

举例：

```java
public class School {

    private String name;
    private String address;

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "School{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
```

```java
public class Student {

    private String name;
    private int age;

    //声明一个引用类型
    private School school;


    public Student() {
        //System.out.println("spring会调用类的无参数构造方法创建对象");
    }

    public void setName(String name) {
        //System.out.println("setName:"+name);
        this.name = name;
    }

    public void setAge(int age) {
        //System.out.println("setAge:"+age);
        this.age = age;
    }

    public void setSchool(School school) {
        System.out.println("setSchool:"+school);
        this.school = school;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", school=" + school +
                '}';
    }
}
```
Spring配置文件:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">


    <!--
            引用类型的自动注入： spring框架根据某些规则可以给引用类型赋值。·不用你在给引用类型赋值了
       使用的规则常用的是byName, byType.
       1.byName(按名称注入) ： java类中引用类型的属性名和spring容器中（配置文件）<bean>的id名称一样，
                              且数据类型是一致的，这样的容器中的bean，spring能够赋值给引用类型。
         语法：
         <bean id="xx" class="yyy" autowire="byName">
            简单类型属性赋值
         </bean>

       2.byType(按类型注入) ： java类中引用类型的数据类型和spring容器中（配置文件）<bean>的class属性
                              是同源关系的，这样的bean能够赋值给引用类型
         同源就是一类的意思：
          1.java类中引用类型的数据类型和bean的class的值是一样的。
          2.java类中引用类型的数据类型和bean的class的值父子类关系的。
          3.java类中引用类型的数据类型和bean的class的值接口和实现类关系的
         语法：
         <bean id="xx" class="yyy" autowire="byType">
            简单类型属性赋值
         </bean>
    -->
    <!--byName-->
    <bean id="myStudent" class="com.ba04.Student"  autowire="byName">
        <property name="name" value="李四" />
        <property name="age" value="26" />
        <!--引用类型-->
        <!--<property name="school" ref="mySchool" />-->
    </bean>

    <!--声明School对象-->
    <bean id="school" class="com.ba04.School">
        <property name="name" value="清华大学"/>
        <property name="address" value="北京的海淀区" />
    </bean>


</beans>
```
测试:

```java
   @Test
    public void test01(){
       String config="ba04/applicationContext.xml";
       ApplicationContext ac = new ClassPathXmlApplicationContext(config);

       //从容器中获取Student对象
       Student myStudent =  (Student) ac.getBean("myStudent");
       System.out.println("student对象="+myStudent);
   }
```
### 2.3.2.byType 方式自动注入
使用 byType 方式自动注入，要求：配置文件中被调用者 bean 的 class 属性指定的类，
要与代码中调用者 bean 类的某引用类型属性类型同源。即要么相同，要么有 is-a 关系（子类，或是实现类）。但这样的同源的被调用 bean 只能有一个。多于一个，容器就不知该匹配
哪一个了。

举例：

```java
public class School {

    private String name;
    private String address;

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "School{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
```

```java
public class Student {

    private String name;
    private int age;

    //声明一个引用类型
    private School school;
    private School school2;


    public Student() {
        //System.out.println("spring会调用类的无参数构造方法创建对象");
    }

    public void setName(String name) {
        //System.out.println("setName:"+name);
        this.name = name;
    }

    public void setAge(int age) {
        //System.out.println("setAge:"+age);
        this.age = age;
    }

    public void setSchool(School school) {
        System.out.println("setSchool:"+school);
        this.school = school;
    }

    public void setSchool2(School school2) {
        System.out.println("setSchool2222222:"+school);
        this.school2 = school2;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", school=" + school +
                ", school2=" + school2 +
                '}';
    }
}
```

```java
// 子类
public class PrimarySchool extends  School {
}
```
Spring配置文件:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">


    <!--
            引用类型的自动注入： spring框架根据某些规则可以给引用类型赋值。·不用你在给引用类型赋值了
       使用的规则常用的是byName, byType.
       1.byName(按名称注入) ： java类中引用类型的属性名和spring容器中（配置文件）<bean>的id名称一样，
                              且数据类型是一致的，这样的容器中的bean，spring能够赋值给引用类型。
         语法：
         <bean id="xx" class="yyy" autowire="byName">
            简单类型属性赋值
         </bean>

       2.byType(按类型注入) ： java类中引用类型的数据类型和spring容器中（配置文件）<bean>的class属性
                              是同源关系的，这样的bean能够赋值给引用类型
         同源就是一类的意思：
          1.java类中引用类型的数据类型和bean的class的值是一样的。
          2.java类中引用类型的数据类型和bean的class的值父子类关系的。
          3.java类中引用类型的数据类型和bean的class的值接口和实现类关系的
         语法：
         <bean id="xx" class="yyy" autowire="byType">
            简单类型属性赋值
         </bean>

         注意：在byType中， 在xml配置文件中声明bean只能有一个符合条件的，
              多余一个是错误的
    -->
    <!--byType-->
    <bean id="myStudent" class="com.ba05.Student"  autowire="byType">
        <property name="name" value="张飒" />
        <property name="age" value="26" />
        <!--引用类型-->
        <!--<property name="school" ref="mySchool" />-->
    </bean>

    <!--声明School对象-->
    <bean id="mySchool" class="com.ba05.School">
        <property name="name" value="人民大学"/>
        <property name="address" value="北京的海淀区" />
    </bean>

    <!--声明School的子类-->
    <!--<bean id="primarySchool" class="com.ba05.PrimarySchool">
        <property name="name" value="北京小学" />
        <property name="address" value="北京的大兴区" />
    </bean>-->


</beans>
```
测试：

```java
 @Test
  public void test01(){
     String config="ba05/applicationContext.xml";
     ApplicationContext ac = new ClassPathXmlApplicationContext(config);

     //从容器中获取Student对象
     Student myStudent =  (Student) ac.getBean("myStudent");
     System.out.println("student对象="+myStudent);
 }
```
### 2.3.3.为应用指定多个 Spring 配置文件

在实际应用里，随着应用规模的增加，系统中 Bean 数量也大量增加，导致配置文件变
得非常庞大、臃肿。为了避免这种情况的产生，提高配置文件的可读性与可维护性，可以将Spring 配置文件分解成多个配置文件。

包含关系的配置文件：
多个配置文件中有一个总文件，总配置文件将各其它子文件通过`<import/>`引入。在 Java代码中只需要使用总配置文件对容器进行初始化即可。

举例：

```java
public class School {

    private String name;
    private String address;

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "School{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
```

```java
public class Student {

    private String name;
    private int age;

    //声明一个引用类型
    private School school;


    public Student() {
        //System.out.println("spring会调用类的无参数构造方法创建对象");
    }

    public void setName(String name) {
        //System.out.println("setName:"+name);
        this.name = name;
    }

    public void setAge(int age) {
        //System.out.println("setAge:"+age);
        this.age = age;
    }

    public void setSchool(School school) {
        System.out.println("setSchool:"+school);
        this.school = school;
    }


    public String getName() {
        return name;
    }


    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", school=" + school +
                '}';
    }
}
```
**spring-school.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">



    <!--School模块所有bean的声明， School模块的配置文件-->
    <!--声明School对象-->
    <bean id="mySchool" class="com.ba06.School">
        <property name="name" value="航空大学"/>
        <property name="address" value="北京的海淀区" />
    </bean>

</beans>
```
**spring-student.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">



    <!--
      student模块所有bean的声明
    -->
    <!--byType-->
    <bean id="myStudent" class="com.ba06.Student"  autowire="byType">
        <property name="name" value="张飒" />
        <property name="age" value="30" />
        <!--引用类型-->
        <!--<property name="school" ref="mySchool" />-->
    </bean>

</beans>
```
**total.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!--
         包含关系的配置文件：
         spring-total表示主配置文件 ： 包含其他的配置文件的，主配置文件一般是不定义对象的。
         语法：<import resource="其他配置文件的路径" />
         关键字："classpath:" 表示类路径（class文件所在的目录），
               在spring的配置文件中要指定其他文件的位置， 需要使用classpath，告诉spring到哪去加载读取文件。
    -->

    <!--加载的是文件列表-->
    <!--
    <import resource="classpath:ba06/spring-school.xml" />
    <import resource="classpath:ba06/spring-student.xml" />
    -->

    <!--
       在包含关系的配置文件中，可以通配符（*：表示任意字符）
       注意： 主的配置文件名称不能包含在通配符的范围内（不能叫做spring-total.xml）
    -->
    <import resource="classpath:ba06/spring-*.xml" />
</beans>
```
测试:

```java
@Test
 public void test01(){
   //加载的是总的文件
    String config= "ba06/total.xml";
    ApplicationContext ac = new ClassPathXmlApplicationContext(config);

    //从容器中获取Student对象
    Student myStudent =  (Student) ac.getBean("myStudent");
    System.out.println("student对象="+myStudent);
}
```
### 2.3.4.注入集合属性
为了演示这些方式，我们在成员中将常见的一些集合都写出来，然后补充其 set 方法

```java
private String[] strs;
private List<String> list;
private Set<String> set;
private Map<String,String> map;
private Properties props;
```
在配置中也是很简单的，只需要按照下列格式写标签就可以了，可以自己测试一下

```xml
<bean id="accountService" class="cn.ideal.service.impl.AccountServiceImpl">
	<property name="strs">
		<array>
            <value>张三</value>
            <value>李四</value>
            <value>王五</value>
        </array>
    </property>

    <property name="list">
        <list>
            <value>张三</value>
            <value>李四</value>
            <value>王五</value>
        </list>
    </property>

    <property name="set">
        <set>
            <value>张三</value>
            <value>李四</value>
            <value>王五</value>
        </set>
    </property>

    <property name="map">
        <map>
            <entry key="name" value="张三"></entry>
            <entry key="age" value="21"></entry>
        </map>
    </property>

    <property name="props">
        <props>
            <prop key="name">张三</prop>
            <prop key="age">21</prop>
        </props>
    </property>
 </bean>
```

# 3.基于注解的 DI
对于 DI 使用注解，将不再需要在 Spring 配置文件中声明 bean 实例。Spring 中使用注解，需要在原有 Spring 运行环境基础上再做一些改变。
需要在 Spring 配置文件中配置组件扫描器，用于在指定的基本包中扫描注解。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/context
       https://www.springframework.org/schema/context/spring-context.xsd">

    <!--声明组件扫描器(component-scan),组件就是java对象
        base-package：指定注解在你的项目中的包名。
        component-scan工作方式： spring会扫描遍历base-package指定的包，
           把包中和子包中的所有类，找到类中的注解，按照注解的功能创建对象，或给属性赋值。

       加入了component-scan标签，配置文件的变化：
        1.加入一个新的约束文件spring-context.xsd
        2.给这个新的约束文件起个命名空间的名称
    -->
    <context:component-scan base-package="com.ba02" />

</beans>
```
指定多个包的三种方式：
- 使用多个 context:component-scan 指定不同的包路径

```java
<context:component-scan base-package="com.ba02" />
<context:component-scan base-package="com.ba03" />
```

- 指定 base-package 的值使用分隔符
分隔符可以使用逗号（，）分号（；）还可以使用空格，不建议使用空格。

逗号分隔：

```java
  <context:component-scan base-package="com.ba02,com.ba03" />
```
分号分隔：

```java
<context:component-scan base-package="com.ba02;com.ba03" />
```
- base-package 是指定到父包名
base-package 的值表是基本包，容器启动会扫描包及其子包中的注解，当然也会扫描到子包下级的子包。所以 base-package 可以指定一个父包就可以。

```java
<context:component-scan base-package="com.bruce" />
```
或者最顶级的父包
```java
<context:component-scan base-package="com" />
```
>但不建议使用顶级的父包，扫描的路径比较多，导致容器启动时间变慢。指定到目标包和合适的。也就是注解所在包全路径。

## 3.1.定义 Bean 的注解@Component(掌握)
需要在类上使用注解@Component，该注解的 value 属性用于指定该 bean 的 id 值。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229115541700.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
另外，Spring 还提供了 3 个创建对象的注解：

```java
➢ @Repository 用于对 DAO 实现类进行注解
➢ @Service 用于对 Service 实现类进行注解
➢ @Controller 用于对 Controller 实现类进行注解
```

这三个注解与@Component 都可以创建对象，但这三个注解还有其他的含义，@Service
创建业务层对象，业务层对象可以加入事务功能，@Controller 注解创建的对象可以作为处
理器接收用户的请求。

@Repository，@Service，@Controller 是对@Component 注解的细化，标注不同层的对
象。即持久层对象，业务层对象，控制层对象。

@Component 不指定 value 属性，bean 的 id 是类名的首字母小写。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229115631213.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229115644179.png)
## 3.2.简单类型属性注入@Value(掌握)
需要在属性上使用注解@Value，该注解的 value 属性用于指定要注入的值。使用该注解完成属性注入时，类中无需 setter。当然，若属性有 setter，则也可将其加
到 setter 上。

**举例：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229115728412.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
## 3.3.byType 自动注入@Autowired(掌握)
需要在引用属性上使用注解@Autowired，该注解默认使用按类型自动装配 Bean 的方式。

使用该注解完成属性注入时，类中无需 setter。当然，若属性有 setter，则也可将其加到 setter 上。

举例：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229115808172.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229115817870.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
## 3.4.byType 自动注入@Autowired(掌握)
需要在引用属性上使用注解@Autowired，该注解默认使用按类型自动装配 Bean 的方式。

使用该注解完成属性注入时，类中无需 setter。当然，若属性有 setter，则也可将其加到 setter 上。

举例：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229115910404.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229115919980.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
## 3.5.byName 自动注入@Autowired 与@Qualifier(掌握)
需要在引用属性上联合使用注解@Autowired 与@Qualifier。@Qualifier 的 value 属性用于指定要匹配的 Bean 的 id 值。类中无需 set 方法，也可加到 set 方法上。

举例：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229115958948.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
@Autowired 还有一个属性 required，默认值为 true，表示当匹配失败后，会终止程序运行。若将其值设置为 false，则匹配失败，将被忽略，未匹配的属性值为 null。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229120020355.png)
## 3.6.JDK 注解@Resource 自动注入(掌握)
Spring提供了对 jdk中@Resource注解的支持。@Resource 注解既可以按名称匹配Bean，也可以按类型匹配 Bean。默认是`按名称注入`。使用该注解，要求 JDK 必须是 6 及以上版本。

@Resource 可在属性上，也可在 set 方法上。

## 3.6.1.byType 注入引用类型属性
@Resource 注解若不带任何参数，采用默认按名称的方式注入，按名称不能注入 bean，则会按照类型进行 Bean 的匹配注入。

举例：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229120136704.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
## 3.6.2.byName 注入引用类型属性
@Resource 注解指定其 name 属性，则 name 的值即为按照名称进行匹配的 Bean 的 id。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229120157768.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
# 4.注解与XML的对比
**注解优点是**：
- 方便
- 直观
- 高效（代码少，没有配置文件的书写那么复杂）。

**注解缺点是**：
- 以硬编码的方式写入到 Java 代码中，修改是需要重新编译代码的。

**XML 方式优点是：**
-  配置和代码是分离的
-  在 xml 中做修改，无需编译代码，只需重启服务器即可将新的配置加载。

**xml 的缺点是：**
- 编写麻烦，效率低，大型项目过于复杂。

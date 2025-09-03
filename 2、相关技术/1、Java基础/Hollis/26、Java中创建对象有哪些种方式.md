# 典型回答

## 使用`new`关键字

这是我们最常见的也是最简单的创建对象的方式，通过这种方式我们还可以调用任意的<font color="blue" size=5>构造函数（无参的和有参的）</font>。 
`User user = new User();`

## 使用反射机制

运用反射手段，调用`Java.lang.Class`或者`java.lang.reflect.Constructor`类的`newInstance()`实例方法。
1. 使用 <font color="blue" size=5>Class 类的 newInstance 方法</font>
   可以使用`Class`类的`newInstance`方法创建对象。这个`newInstance`方法调用 **==无参的构造函数==** 创建对象。
```java
User user = (User)Class.forName("xxx.xxx.User").newInstance();
User user = User.class.newInstance();
```
2. 使用 <font color="blue" size=5>Constructor 类的 newInstance 方法</font>
   和`Class`类的`newInstance`方法很像， `java.lang.reflect.Constructor`类里也有一个`newInstance`方法可以创建对象。我们可以通过这个`newInstance`方法调用 **==有参数==** 的和 **==私有的构造函数==** 等。
```java
Constructor  constructor = User.class.getConstructor();
User user = constructor.newInstance();
```

这两种`newInstance`方法就是大家所说的反射。事实上Class的`newInstance`方法内部调用`Constructor`的`newInstance`方法。

## 使用clone方法

无论何时我们调用一个对象的 <font color="blue" size=5>clone 方法</font>，jvm就会**创建一个新的对象**，将前面对象的内容全部拷贝进去。用`clone`方法创建对象并 **==不会调用任何构造函数==**。 

要使用`clone`方法，我们需要先实现`Cloneable`标记接口并实现其定义的`clone`方法。如果只实现了`Cloneable`标记接口，并没有重写`clone`方法的话，会默认使用Object类中的`clone`方法，这是一个native的方法。
```java
public class CloneTest implements Cloneable{
    private String name; 
    private int age; 
    
    public String getName() {
	    return name;
    }
    public void setName(String name) {
	    this.name = name;
    }
    public int getAge() {
	    return age;
    }
    public void setAge(int age) {
	    this.age = age;
    }
    
    public CloneTest(String name, int age) {
        super();
        this.name = name;
        this.age = age;
    }
    
    public static void main(String[] args) {
        try {
            CloneTest cloneTest = new CloneTest("wangql",18);
            CloneTest copyClone = (CloneTest) cloneTest.clone();
            System.out.println("newclone:"+cloneTest.getName());
            System.out.println("copyClone:"+copyClone.getName());
        } catch (CloneNotSupportedException e) {
	        e.printStackTrace();
        }
    }
}
```

clone方法是浅拷贝；实现深拷贝的方式有两种：对照[38、什么是深拷贝和浅拷贝？](2、相关技术/1、Java基础/Hollis/38、什么是深拷贝和浅拷贝？.md)
1. 通过 **==重新所有属性（所有对象的属性）的clone方法==** 实现
2. 通过 **==序列化==** 实现
重写所有clone方法：
```java
public class test  
{  
    public static void main(String[] args) throws CloneNotSupportedException  
    {  
        A a = new A();  
        A a1 = a.clone();  
        System.out.println("a == a1:" + (a == a1));  
        System.out.println("a.b == a1.b:" + (a.b == a1.b));  
        System.out.println("a.b.c == a1.b.c:" + (a.b.c == a1.b.c));  
    }  
}  
  
class A implements Cloneable {  
    public B b = new B();  
      
    public A clone() throws CloneNotSupportedException {  
        A clone = (A) super.clone();  
        clone.b = (B) b.clone();  
        return clone;  
    }  
}  
class B  implements Cloneable {  
    public C c = new C();  
    public B clone() throws CloneNotSupportedException {  
        B clone = (B) super.clone();  
        clone.c = (C) c.clone();  
        return clone;  
    }  
}  
class C  implements Cloneable {  
    public C clone() throws CloneNotSupportedException {  
        return (C) super.clone();  
    }  
}
```

如果只重新A类中的clone方法不重写B、C类中的clone方法，就那么main方法输出为：
```java
// 只重写A的clone方法
a == a1:false
a.b == a1.b:true
a.b.c == a1.b.c:true

// A、B、C的clone方法都重写
a == a1:false
a.b == a1.b:false
a.b.c == a1.b.c:false
```

## 使用反序列化

当我们序列化和反序列化一个对象，jvm会给我们创建一个新的单独的对象（**==深拷贝==**）。其实<font color="blue" size=5>反序列化也是基于反射实现的</font>。

反序列化虽然是基于反射实现的，但是<font color="red" size=5><font color="blue" size=5>反序列化</font>会通过<font color="blue" size=5>Unsafe直接分配内存</font>的方式来创建一个新的对象，<font color="blue" size=5>不是调用构造函数</font></font>，分析过程参考：[ObjectInputStream](如何破坏单例模式？#ObjectInputStream)
```java
public static void main(String[] args) {
    //Initializes The Object
    User1 user = new User1();
    user.setName("hollis");
    user.setAge(23);
    System.out.println(user);

    //Write Obj to File
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("tempFile"));
	oos.writeObject(user);

    //Read Obj from File
    File file = new File("tempFile");
    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
	User1 newUser = (User1) ois.readObject();
	System.out.println(newUser);
}
```

使用 `ByteArrayOutputStream`、`ByteArrayInputStream` 将序列化后数据存储到磁盘：
```java
import java.io.*;
import java.time.*;

public class SerialCloneTest
{  
   public static void main(String[] args) throws CloneNotSupportedException
   {  
      var harry = new Employee("Harry Hacker", 35000, 1989, 10, 1);
      // clone harry
      var harry2 = (Employee) harry.clone();

      // mutate harry
      harry.raiseSalary(10);

      // now harry and the clone are different
      System.out.println(harry);
      System.out.println(harry2);
   }
}

/**
 * A class whose clone method uses serialization.
 */
class SerialCloneable implements Cloneable, Serializable
{  
   public Object clone() throws CloneNotSupportedException
   {
      try {
         // save the object to a byte array
         var bout = new ByteArrayOutputStream();
         try (var out = new ObjectOutputStream(bout))
         {
            out.writeObject(this);
         }

         // read a clone of the object from the byte array
         try (var bin = new ByteArrayInputStream(bout.toByteArray()))
         {
            var in = new ObjectInputStream(bin);
            return in.readObject();
         }
      }
      catch (IOException | ClassNotFoundException e)
      {  
         var e2 = new CloneNotSupportedException();
         e2.initCause(e);
         throw e2;
      }
   }
}

/**
 * The familiar Employee class, redefined to extend the
 * SerialCloneable class. 
 */
class Employee extends SerialCloneable
{  
   private String name;
   private double salary;
   private LocalDate hireDay;

   public Employee(String n, double s, int year, int month, int day)
   {  
      name = n;
      salary = s;
      hireDay = LocalDate.of(year, month, day);
   }

   public String getName()
   {
      return name;
   }

   public double getSalary()
   {
      return salary;
   }

   public LocalDate getHireDay()
   {
      return hireDay;
   }

   /**
      Raises the salary of this employee.
      @byPercent the percentage of the raise
   */
   public void raiseSalary(double byPercent)
   {  
      double raise = salary * byPercent / 100;
      salary += raise;
   }

   public String toString()
   {  
      return getClass().getName()
         + "[name=" + name
         + ",salary=" + salary
         + ",hireDay=" + hireDay
         + "]";
   }
}
```

## 使用方法句柄

通过使用<font color="blue" size=5>方法句柄</font>，可以**间接地调用构造函数来创建对象**
```java
public static void main(String[] args) throws Throwable {
    // 定义构造函数的方法句柄类型为void类型，无参数
    MethodType constructorType = MethodType.methodType(void.class);

    // 获取构造函数的方法句柄
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle constructorHandle = lookup.findConstructor(User.class, constructorType);

    // 使用方法句柄调用构造函数创建对象
    User obj = (User) constructorHandle.invoke();
}
```

使用了`MethodHandles.lookup().findConstructor()`方法获取构造函数的方法句柄，然后通过`invoke()`方法调用构造函数来创建对象。

## 使用Unsafe分配内存

在Java中，可以使用`sun.misc.Unsafe`类来进行<font color="blue" size=5>直接的内存操作</font>，包括**内存分配和对象实例化**。然而，需要注意的是，`sun.misc.Unsafe`类是Java的内部API，它并不属于Java标准库的一部分，也不建议直接在生产环境中使用。
```java
public static void main(String[] args) throws Exception {

    Field field = Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    Unsafe unsafe = field.get(null);

    // 获取User类的字段偏移量
    long nameOffset = unsafe.objectFieldOffset(User.class.getDeclaredField("name"));
    long ageOffset = unsafe.objectFieldOffset(User.class.getDeclaredField("age"));

    // 使用allocateInstance方法创建对象，不会调用构造函数
    User user = (User) unsafe.allocateInstance(User.class);

    // 使用putObject方法设置字段的值
    unsafe.putObject(user, nameOffset, "Hollis");
    unsafe.putInt(user, ageOffset, 30);
}
```

这种方式有以下几个缺点：

1. **不可移植性**：Unsafe类的行为在不同的Java版本和不同的JVM实现中可能会有差异，因此代码在不同的环境下可能会出现不可移植的问题。
2. **安全性问题**：Unsafe类的功能是非常强大和危险的，可以绕过Java的安全机制，可能会导致内存泄漏、非法访问、数据损坏等安全问题。
3. **不符合面向对象的原则**：Java是一门面向对象的语言，鼓励使用构造函数和工厂方法来创建对象，以确保对象的正确初始化和维护对象的不变性。
















#transient #writeOBject #readObject #Serializable #Externalizable #继承中的序列化 #继承中的反序列化 

来自：
- [Java transient关键字使用小记 ](https://www.cnblogs.com/lanxuezaipiao/p/3369962.html)
- [菜鸟-Java transient 关键字](https://www.runoob.com/w3cnote/java-transient-keywords.html)

   哎，虽然自己最熟的是Java，但很多Java基础知识都不知道，比如transient关键字以前都没用到过，所以不知道它的作用是什么，今天做笔试题时发现有一题是关于这个的，于是花个时间整理下transient关键字的使用，涨下姿势

# 1. transient的作用及使用方法

我们都知道一个对象只要实现了`Serilizable`接口，这个对象就可以被序列化，java的这种序列化模式为开发者提供了很多便利，我们可以不必关系具体序列化的过程，**==只要类实现了`Serilizable`接口，这个类的所有属性和方法都会自动序列化；我们可以在类中写`writeObject(ObjectOutputStream s)`、`readObject(ObjectInputStream s)`方法，自己控制哪些字段需要被序列化、反序列化==**。
示例：手动控制被`transient`修饰的字段被序列化和反序列化
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508141103584.png)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508141103672.png)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508141103600.png)

在实际开发过程中，我们常常会遇到这样的问题，这个类的 **==有些属性需要序列化，而其他属性不需要被序列化==**，打个比方，如果一个用户有一些敏感信息（如密码，银行卡号等），为了安全起见，不希望在网络操作（主要涉及到序列化操作，本地序列化缓存也适用）中被传输，这些信息对应的变量就可以加上`transient`关键字。换句话说，这个字段的生命周期仅存于调用者的内存中而不会写到磁盘里持久化。

<font color = "red">总之，java 的transient关键字为我们提供了便利，你只需要实现Serilizable接口，将不需要序列化的属性前添加关键字transient，序列化对象的时候，<font color = "blue">被transient修饰的属性默认不会序列化到指定的目的地中</font>，<u>除非我们像上面照片中手写writeObject、readObject方法对指定字段进行序列化和反序列化</u></font>

示例code如下：
```java
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @description 使用transient关键字不序列化某个变量
 *  注意读取的时候，读取数据的顺序一定要和存放数据的顺序保持一致
 * 
 * @author Alexia
 * @date  2013-10-15
 */
public class TransientTest {
    
    public static void main(String[] args) {
        
        User user0 = new User();
        user0.setUsername("Alexia");
        user0.setPasswd("123456");
        
        System.out.println("read before Serializable: ");
        System.out.println("username: " + user0.getUsername());
        System.err.println("password: " + user0.getPasswd());
        
        try {
            ObjectOutputStream os = new ObjectOutputStream(
                    new FileOutputStream("C:/user.txt"));
            os.writeObject(user0); // 将User对象写进文件
            os.flush();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(
                    "C:/user.txt"));
            User user2 = (User) is.readObject(); // 从流中读取User的数据
            is.close();
            
            System.out.println("\nread after Serializable: ");
            // 打印结果为false，反序列化后的user2对象中String类型
            // 的字段与序列化前user1对象中String类型不是一个
            System.out.println(user1.getUsername() == user2.getUsername());
            System.out.println("username: " + user2.getUsername());
            System.err.println("password: " + user2.getPasswd());
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

class User implements Serializable {
    private static final long serialVersionUID = 8294180014912103005L;  
    
    private String username;
    private transient String passwd;
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPasswd() {
        return passwd;
    }
    
    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

}
```

输出为：
```
read before Serializable: 
username: Alexia
password: 123456

read after Serializable: 
false
username: Alexia
password: null
```

- 序列化前的`String`类型的`username`和反序列化后的`String`类型的`username`字段地址不同，说明是 **==深拷贝==**
- 密码字段为`null`，说明反序列化时根本没有从文件中获取到信息。

# 2. transient使用小结

1. 一旦变量被`transient`修饰，该变量 **==默认将不再是对象持久化的一部分==**，在序列化后无法获得访问，如果 **手动在将被序列化的类中实现writeObject、readObject方法，可以==手动控制被`transient`修饰的字段的序列化和反序列化==**。
2. `transient`关键字只能修饰变量，而不能修饰方法和类。注意，本地变量是不能被`transient`关键字修饰的。变量如果是用户自定义类变量，则该类需要实现`Serializable`接口。
3. 一个静态变量不管是否被`transient`修饰，均不能被序列化。

第三点可能有些人很迷惑，因为发现在`User`类中的`username`字段前加上`static`关键字后，<font color = "red">程序运行结果依然不变</font>，即`static`类型的`username`也读出来为 `"Alexia"`了，这不与第三点说的矛盾吗？实际上是这样的：<font color = "red">第三点确实没错（一个静态变量不管是否被transient修饰，均不能被序列化），反序列化后类中static型变量username的值为当前JVM中对应static变量的值，这个值是JVM中的不是反序列化得出的</font>，不相信？好吧，下面我来证明：
```java
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @description 使用transient关键字不序列化某个变量
 *  注意读取的时候，读取数据的顺序一定要和存放数据的顺序保持一致
 *        
 * @author Alexia
 * @date  2013-10-15
 */
public class TransientTest {
    
    public static void main(String[] args) {
        
        User user = new User();
        user.setUsername("Alexia");
        user.setPasswd("123456");
        
        System.out.println("read before Serializable: ");
        System.out.println("username: " + user.getUsername());
        System.err.println("password: " + user.getPasswd());
        
        try {
            ObjectOutputStream os = new ObjectOutputStream(
                    new FileOutputStream("C:/user.txt"));
            os.writeObject(user); // 将User对象写进文件
            os.flush();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            // 在反序列化之前改变username的值
            User.username = "jmwang";
            
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(
                    "C:/user.txt"));
            user = (User) is.readObject(); // 从流中读取User的数据
            is.close();
            
            System.out.println("\nread after Serializable: ");
            System.out.println("username: " + user.getUsername());
            System.err.println("password: " + user.getPasswd());
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

class User implements Serializable {
    private static final long serialVersionUID = 8294180014912103005L;  
    
    public static String username;
    private transient String passwd;
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPasswd() {
        return passwd;
    }
    
    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

}
```

运行结果为：
```
read before Serializable: 
username: Alexia
password: 123456

read after Serializable: 
username: jmwang
password: null
```

这说明反序列化后类中`static`型变量`username`的值为当前JVM中对应`static`变量的值，为修改后`jmwang`，而不是序列化时的值`Alexia`。

# 3. transient使用细节——被transient关键字修饰的变量真的不能被序列化吗？

1. 实现接口`Serializable`：
   在`ObjectInputStream#readObject`方法中通过 **反射 + Unsafe** 创建的对象是空的，在对象自定义的`readObject`方法中给`content`赋值，并没有因实例成员变量`content`在定义时赋值就在内存分配时给该对象赋值，跟 [32、JVM是如何创建对象的？](2、相关技术/3、JVM/Hollis/32、JVM是如何创建对象的？.md) 中描述的对象创建流程不一致，不知道为什么
```java
import java.io.*;  
  
/**  
 * @descripiton Serializable接口的使用  
 *  
 * @author lxd 
 * @date 2025年8月14日  
 */  
public class SerializableTest implements Serializable {  
      
    private transient String content = "transient关键字修饰";  
      
    private void writeObject(ObjectOutputStream out) throws IOException {  
        out.defaultWriteObject();  
        out.writeObject(content);  
    }  
      
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {  
        in.defaultReadObject();  
        content = (String) in.readObject();  
    }  
      
    public static void main(String[] args) throws Exception {  
          
        SerializableTest st = new SerializableTest();  
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("D:\\temp\\tempFile"));  
        out.writeObject(st);  
          
        ObjectInput in = new ObjectInputStream(new FileInputStream("D:\\temp\\tempFile"));  
        st = (SerializableTest) in.readObject();  
        System.out.println(st.content);  
          
        out.close();  
        in.close();  
    }  
}
```

2. 实现接口`Externalizable`：
```java
import java.io.*;

/**
 * @descripiton Externalizable接口的使用
 * 
 * @author Alexia
 * @date 2013-10-15
 *
 */
public class ExternalizableTest implements Externalizable {

    private transient String content = "是的，我将会被序列化，不管我是否被transient关键字修饰";

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(content);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        content = (String) in.readObject();
    }

    public static void main(String[] args) throws Exception {
        
        ExternalizableTest et = new ExternalizableTest();
        ObjectOutput out = new ObjectOutputStream(new FileOutputStream(
                new File("D:\\temp\\tempFile")));
        out.writeObject(et);

        ObjectInput in = new ObjectInputStream(new FileInputStream(new File(
                "D:\\temp\\tempFile")));
        et = (ExternalizableTest) in.readObject();
        System.out.println(et.content);

        out.close();
        in.close();
    }
}
```

<font color="red" size=5>这两种方式都会序列化被transient关键字修饰的字段</font>

在Java中，对象的序列化可以通过实现两种接口来实现：
1. **==实现`Serializable`接口==**，则 **==对字段的序列化会自动进行==**，我们可以 **==自定义`writeObject`、`readObject`方法，并且这两个方法必须都是私有的==**，手动控制字段的序列化和反序列化
2. **==实现`Externalizable`接口==**，**==没有任何东西可以自动序列化==**，需要我们手动在`writeExternal`、`readExternal`方法中进行手工指定所要序列化和反序列化的变量，这与是否被transient修饰无关。因此第二个例子输出的是变量`content`初始化的内容，而不是null。

# 4. `Serializable`与`Externalizable`对比

[`Externalizable`、`Serializable`对比](29、Java序列化的原理是啥#`Externalizable`、`Serializable`对比)

# 5.补充-继承中的序列化

如果要序列化的类【记作M】有父类【记作N】，要想同时将在父类中定义的变量持久化下来，有两种方式：

1. `Serializable`
   1. 父类N实现`Serializable`接口
   2. 子类自定义`writeObject`、`readObject`方法，在这两个方法中操作超类字段，父类不实现`Serializable`接口
2. `Externalizable`
   子类M实现`Externalizable`接口，并 **在==`writeExternal`方法中将父类中的变量写入到对象流==中，在==`readExternal`方法中将对象流中的数据（对象的属性）保存到反序列化创建的对象的属性==中**

方式1.2对应代码：
```java
import java.io.*;  
  
public class t2 extends t implements Serializable  
{  
    private String s1;  
    private String s2;  
    private String s3;  
      
    public t2()  
    {  
    }  
      
    public t2(String s0,String s1, String s2, String s3)  
    {  
        super(s0);  
        this.s1 = s1;  
        this.s2 = s2;  
        this.s3 = s3;  
    }  
      
    // 向ObjectOutputStream中写入数据的顺序需要和readExternal方法中  
    // 从ObjectInputStream中获取数据的顺序一致，否则反序列化属性赋值错乱  
    private void writeObject(ObjectOutputStream out) throws IOException  
    {  
        //action属性是继承自父类t  
        // 实现接口Externalizable序列化方式，如果想让反序列化的对象获取到父类中的属性，就需要在这个方法中将父类中的  
        // 字段写入到流中，同时在反序列化readExternal方法中从流中读取值并保存在父类的属性中。  
        out.writeObject(action);  
        out.writeObject(s1);  
        out.writeObject(s2);  
        out.writeObject(s3);  
    }  
      
    // 从ObjectInputStream中获取数据的顺序需要和writeExternal方法中  
    // 向ObjectOutputStream中写入数据的顺序一致，否则反序列化属性赋值错乱  
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException  
    {  
        //action属性是继承自父类t  
        action = (String) in.readObject();  
        s1 = (String) in.readObject();  
        s2 = (String) in.readObject();  
        s3 = (String) in.readObject();  
    }  
      
    public static void main(String[] args) throws IOException, ClassNotFoundException  
    {  
        t2 t20 = new t2("动作","123","456","789");  
          
        ByteArrayOutputStream bout = new ByteArrayOutputStream();  
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(bout);  
        objectOutputStream.writeObject(t20);  
          
        System.out.println("序列化前：" + t20);  
          
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());  
        ObjectInputStream objectInputStream = new ObjectInputStream(bin);  
        t2 o = (t2) objectInputStream.readObject();  
        System.out.println("反序列化后：" + o);  
        System.out.println("打印父类对象中的属性：" + o.action);  
    }  
      
      
    @Override  
    public String toString()  
    {  
        return "t2{" +  
                "s1='" + s1 + '\'' +  
                ", s2='" + s2 + '\'' +  
                ", s3='" + s3 + '\'' +  
                ", action='" + action + '\'' +  
                '}';  
    }  
}  
  
class t // implements Serializable  
{  
    public String action;  
      
    public t()  
    {  
    }  
      
    public t(String action)  
    {  
        this.action = action;  
    }  
      
    public String getAction()  
    {  
        return action;  
    }  
}
```

方式2对应代码：
```java
public class t2 extends t implements Externalizable
{
    private String s1;
    private String s2;
    private String s3;
    
    public t2()
    {
    }
    
    public t2(String s0,String s1, String s2, String s3)
    {
        super(s0);
        this.s1 = s1;
        this.s2 = s2;
        this.s3 = s3;
    }
    
	// 向ObjectOutput中写入数据的顺序需要和readExternal方法中
	// 从ObjectInput中获取数据的顺序一致，否则反序列化属性赋值错乱
    public void writeExternal(ObjectOutput out) throws IOException
    {
        //action属性是继承自父类t
        // 实现接口Externalizable序列化方式，如果想让反序列化的对象获取到父类中的属性，就需要在这个方法中将父类中的
        // 字段写入到流中，同时在反序列化readExternal方法中从流中读取值并保存在父类的属性中。
        out.writeObject(action);
        out.writeObject(s1);
        out.writeObject(s2);
        out.writeObject(s3);
    }

	// 从ObjectInput中获取数据的顺序需要和writeExternal方法中
	// 向ObjectOutput中写入数据的顺序一致，否则反序列化属性赋值错乱
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        //action属性是继承自父类t
        action = (String) in.readObject();
        s1 = (String) in.readObject();
        s2 = (String) in.readObject();
        s3 = (String) in.readObject();
    }
    
    public static void main(String[] args) throws IOException, ClassNotFoundException
    {
        t2 t20 = new t2("动作","123","456","789");
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(bout);
        objectOutputStream.writeObject(t20);
        
        System.out.println("输出序列化的内容：" + bout.toByteArray());
    
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(bin);
        t2 o = (t2) objectInputStream.readObject();
        System.out.println("反序列化的内容：" + o);
        System.out.println("打印父类对象中的属性：" + o.action);
    }
    
    @Override
    public String toString()
    {
        return "t2{" +
                "s1='" + s1 + '\'' +
                ", s2='" + s2 + '\'' +
                ", s3='" + s3 + '\'' +
                '}';
    }
}

class t// implements Serializable
{
    public String action;
    
    public t()
    {
    }
    
    public t(String action)
    {
        this.action = action;
    }
    
    public String getAction()
    {
        return action;
    }
}
```

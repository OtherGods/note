[TOC]


# 1.耦合

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201228164929762.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70,#pic_left)

>Spring框架是 Java 平台的一个开源的全栈（Full-stack）应用程序框架和控制反转容器实现，一般被直接称为 Spring。该框架的一些核心功能理论上可用于任何 Java 应用，但 Spring 还为基于Java企业版平台构建的 Web 应用提供了大量的拓展支持。虽然 Spring 没有直接实现任何的编程模型，但它已经在 Java 社区中广为流行，基本上完全代替了企业级JavaBeans（EJB）模型                                 —— 维基百科

耦合，就是模块间关联的程度，每个模块之间的联系越多，也就是其耦合性越强，那么独立性也就越差了，所以我们在软件设计中，应该尽量做到<font color=red>低耦合，高内聚</font>！

高内聚与低耦合是每个软件开发者追求的目标，那么内聚和耦合分别是什么意思呢？
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229094008395.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
>内聚是从功能角度来度量模块内的联系，一个好的内聚模块应当恰好做一件事。它描述的是模块内的功能联系。

>耦合是软件结构中各模块之间相互连接的一种度量，耦合强弱取决于模块间接口的复杂程度、进入或访问一个模块的点以及通过接口的数据。

**生活中的例子**：家里有一条串灯，上面有很多灯泡，如果灯坏了，你需要将整个灯带都换掉，这就是高耦合的表现，因为灯和灯带之间是紧密相连，不可分割的，但是如果灯泡可以随意拆卸，并不影响整个灯带，那么这就叫做低耦合！

**代码中的例子**：来看一个多态的调用，前提是B、C继承 A，引用了很多次

```java
A a = new B();  // 父类引用 指向子类对象   里式替换原则
a.method();
```
如果你想要把B变成C，就需要修改所有`new B()` 的地方为 `new C()` 这也就是高耦合

```java
A a = new C();  // 父类引用 指向子类对象   里式替换原则
a.method();

重新编译  Web源码---打包war--->编译class---->上线部署!
```

Spring的解耦方式：将B类写道配置文件中，并在配置文件中给它一个标识（xxx），在源码中引用配置文件中的这个标识（xxx），这个时候，不需要修改源码中的标识（xxx），只需要将配置文件中标识xxx下的B改为C就可以了.

```java
//xxx————>对应配置文件中的名称xxx标签——>在配置文
//件中xxx标签下配置B类或者C类，这样就只需要修改配置文件
//不用改源码；
//框架底层使用反射创建B或者C类的实例对象
A a = BeanFactory().getBean("xxx");
a.method();
```
# 2.分析耦合及改进
首先，我们简单的模拟一个对账户进行添加的操作，我们先采用我们以前常常使用的方式进行模拟，然后再给出改进方案，再引出今天要将的 Spring 框架，能帮助更好的理解这个框架!
## 2.1.以前的程序
首先，按照我们常规的方式先模拟，我们先将一套基本流程走下来

**A：Dao 层**（也就是Mybatis中的Mapper接口，数据访问层）

```java
/**
 * 账户持久层接口
 */
public interface AccountDao {
    void addAccount();
}

/**
 * 账户持久层实现类
 */
public class AccountDaoImpl implements AccountDao {

    public void addAccount() {
        System.out.println("添加用户成功！");
    }
}
```
**B：Service 层**

```java
/**
 * 账户业务层接口
 */
public interface AccountService {
    void addAccount();
}

/**
 * 账户业务层实现类
 */
public class AccountServiceImpl implements AccountService {
	
	private AccountDao ad = new AccountDaoImpl();
	
    public void addAccount() {
        ad.addAccount();
    }
}
```
**C：调用**
由于，我们创建的Maven工程并不是一个web工程，我们也只是为了简单模拟，所以在这里，创建了一个 Client 类，作为客户端，来测试我们的方法

```java
public class Client {
    public static void main(String[] args) {
		AccountService  as = new AccountServiceImpl();
		as.addAccount();
    }
}
```
运行的结果，就是在屏幕上输出一个添加用户成功的字样

**D：分析：new 的问题**
上面的这段代码，应该是比较简单也容易想到的一种实现方式了，但是它的耦合性却是很高的，其中这两句代码，就是造成耦合性高的根由，因为业务层（service）调用持久层（dao），这个时候业务层将很大的依赖于持久层的接口（AccountDao）和实现类（AccountDaoImpl）

```java
private AccountDao ad = new AccountDaoImpl();
AccountService as = new AccountServiceImpl();
```
这种通过 new 对象的方式，使得不同类之间的依赖性大大增强，其中一个类的问题，就会直接导致出现全局的问题，如果我们将被调用的方法进行错误的修改，或者说删掉某一个类，执行的结果就是：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229101038715.png)
在**编译期**就出现了错误，而我们作为一个开发者，我们应该努力让程序在编译期不依赖，而运行时才可以有一些必要的依赖（依赖是不可能完全消除的）

所以，我们应该想办法进行解耦，要解耦就要使**调用者**和**被调用者**之间没有什么直接的联系，那么**工厂模式**就可以帮助我们很好的解决这个问题. （BeanFactory核心容器就是工厂模式的体现，ApplicationContext是BeanFactory的子接口，被称为应用上下文，也是一种常用的Spring核心容器）

应该大家在 JavaWeb 或者 JavaSE的学习中，或多或少是有接触过工厂这个设计模式的，而工厂模式，我们简单提一下，工厂就是在调用者和被调用者之间起一个连接枢纽的作用，调用者和被调用者都只与工厂进行联系，从而减少了两者之间直接的依赖（如果有一些迷茫的朋友，可以了解一下这种设计模式)

**传统模式：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229101207675.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
**工厂模式：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229101221104.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

## 2.2.工厂模式改进
**A：BeanFactory**
具体怎么实现呢？在这里可以将 serivice 和 dao 均配置到配置文件中去（`xml/yaml/properties`)，通过一个类读取配置文件中的内容，并使用反射技术创建对象，然后存起来，完成这个操作的类就是我们的工厂!

>注：在这里我们使用了 properties ，主要是为了实现方便，xml还涉及到解析的一些代码，相对麻烦一些，不过我们下面要说的 Spring 就是使用了 xml做配置文件

- bean.properties：先写好配置文件，将 service 和 dao 以 key=value 的格式配置好

```java
accountService=cn.ideal.service.impl.AccountServiceImpl
accountDao=cn.ideal.dao.impl.AccountDaoImpl
```
- BeanFactory

```java
public class BeanFactory {
    //定义一个Properties对象
    private static Properties properties;
    //使用静态代码块为Properties对象赋值
    static {
        try{
            //实例化对象
            properties = new Properties();
            //获取properties文件的流对象
            InputStream in = BeanFactory.class.getClassLoader().getResourceAsStream("bean.properties");
            properties.load(in);
        }catch (Exception e){
            throw  new ExceptionInInitializerError("初始化properties失败");
        }
    }  
}
```
简单的解释一下这部分代码（当然还没写完）：首先就是要将配置文件中的内容读入，这里通过类加载器的方式操作，读入一个流文件，然后从中读取键值对，由于只需要执一次，所以放在静态代码块中，又因为 properties 对象在后面的方法中还要用，所以写在成员的位置.

接着在 BeanFactory 中继续编写一个 getBean 方法其中有两句核心代码的意义就是：

通过方法参数中传入的字符串，找到对应的全类名路径，实际上也就是通过刚才获取到的配置内容，通过key 找到 value值
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229101604832.png)
下一句就是通过 Class 的加载方法加载这个类，实例化后返回

```java
public static Object getBean(String beanName){
    Object bean = null;

    try {
        //根据key获取value
        String beanPath = properties.getProperty(beanName);
        bean = Class.forName(beanPath).newInstance();
    }catch (Exception e){
        e.printStackTrace();
    }
    return bean;
}
```
**B：测试代码：**

```java
public class Client {
    public static void main(String[] args) {
        //同理在AccountServiceImpl中可以使用这种方式给AccountDao类型的属性ad赋值
        AccountService as = 					   (AccountService)BeanFactory.getBean("accountService");
        as.addAccount();
    }
}
```
**C：执行效果：**
当我们按照同样的操作，删除掉被调用的 dao 的实现类，可以看到，这时候编译期错误已经消失了，而报出来的只是一个运行时异常，这样就解决了前面所思考的问题!
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229101647261.png)

>我们应该努力让程序在编译期不依赖，而运行时才可以有一些必要的依赖（依赖是不可能完全消除的）

## 2.3.小总结
**为什么使用工厂模式替代了 new 的方式？**

打个比方，在你的程序中，如果一段时间后，你发现在你 new 的这个对象中存在着bug或者不合理的地方，或者说你甚至想换一个持久层的框架，这种情况下，没办法，只能修改源码了，然后重新编译，部署，但是如果你使用工厂模式，你只需要重新将想修改的类，单独写好，编译后放到文件中去，只需要修改一下配置文件就可以了

【**new 对象依赖的是具体事物，而不 new 则是依赖抽象事物**】

>依赖具体事物，这个很好理解，你依赖的是一个具体的，实实在在内容，它与你系相关，所以有什么问题，都是连环的，可能为了某个点，我们需要修改 N 个地方，绝望

>依赖抽象事物，你所调用的并不是一个直接就可以触手可及的东西，是一个抽象的概念，所以不存在上面那种情况下的连环反应
## 2.4.再分析问题
到这里，似乎还不错，不过我们的程序还能够继续优化！ 来分析一下：

首先在测试中，多打印几次，工厂所创建出的对象，我们写个for循环打印下

```java
for(int i = 0; i < 4; i++){
	AccountService as = (AccountService)BeanFactory.getBean("accountService");
	System.out.println(as);
}
```
看下结果：特别显眼的四次输出，我们的问题也就出来了，我所创建的4个对象是不同的，也就是说，每一次调用，都会实例化一个新的对象，这也叫做**多例**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229101906596.png)
这有什么问题吗？

①：多次创建对象的代价就是消耗性能，导致效率会低一些
②：相比较单例，jvm会回收较多的垃圾
③：获取速度比单例慢，因为单例除了第一次，其后都是从缓存中获取

所以，我们要试着将它改成单例的，单例从表现上来看，我们查询到的对象都应该是一个
## 2.5.多例->单例之再改进

**<font color="red">`这一小节的内容对应Spring中bean标签中的作用域`</font>**

**A：分析：**
前面我们每一次调用都要将类进行 newInstance()，也就是实例化，想要不再创建新的对象，只需要将我们第一次创建的对象，在创建后就存到一个集合（容器）中，由于我们有查询的需求所以在 Map 和 List 中选择了 Map

**B：代码：**
简单解读一下：

- 首先在成员位置定义一个 Map，称作beans，至于实例化就不说了
- 通过 keys 方法，取出所有的 配置中所有的key，然后进行遍历出每一个key

- 通过每个 key 从配置中取出对应的 value 在这里就是对应类的全类名
- 将每个取出的 value，使用反射创建出对象 obj
- 将 key 与 obj 存入Map容器
- 在 getBean 方法中只需要从 Map中取就可以了

```java
/**
这里的代码还存在一些问题，例如：如果在配置文件bean.properties中配置的键值对的顺序不同，可能会导致AccountDao实例对象中没有成功注入到没有成功注入到AccountService类的实例对象中的问题，这是因为在加载BeanFactory类的时候由于配置文件中键值对顺序问题，如果在静态代码块中的循环中先创建的时AccoutService类的实例对象，那么会导致这个AccoutService类型的实例AccoutService中的字段AccoutDao的值为null；解决这种问题的方式是在创建好AccoutService和AccoutDao类型的实例对象之后，使用反射的方式检查哪个类中使用了注解注入，检查这个被注入的字段的值是否成功注入，如没有则手动调用setter方法注入一下。
*/
public class BeanFactory {
    //定义一个Properties对象
    private static Properties properties;
    //定义Map，作为存放对象的容器
    private static Map<String, Object> beans;

    //使用静态代码块为Properties对象赋值
    static {
        try {
            //实例化对象
            properties = new Properties();
            //获取properties文件的流对象
            InputStream in = BeanFactory.class.getClassLoader().getResourceAsStream("bean.properties");
            properties.load(in);
            //实例化容器
            beans = new HashMap<String, Object>();
            //取出所有key
            Enumeration keys = properties.keys();
            //遍历枚举
            while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                //根据获取到的key获取对应value
                String beanPath = properties.getProperty(key);
                //反射创对象
                Object obj = Class.forName(beanPath).newInstance();
                beans.put(key, obj);
            }

        } catch (Exception e) {
            throw new ExceptionInInitializerError("初始化properties失败");
        }
    }

    public static Object getBean(String beanName) {
        return beans.get(beanName);
    }
}
```
**C：执行效果：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229102106787.png)
测试结果已经变成了单例的

**D：单例的劣势：**
单例一个很明显的问题，就是在并发情况下，可能会出现线程安全问题

因为由于单例情况下，对象只会被实例化一次，这也就说，所有请求都会共享一个 bean 实例，若一个请求改变了对象的状态，同时对象又处理别的请求，之前的请求造成的对象状态改变，可能会影响在操作时，对别的请求做了错误的处理

**举个简单的例子帮助理解：**

```java
public class AccountDaoImpl implements AccountDao {
	//定义一个类成员
    private int i = 1;

    public void addAccount() {
        System.out.println("添加用户成功！");
        System.out.println(i);
        i++;
    }
}
```
测试中依旧是哪个循环，不过这次执行一下 addAccount() 方法
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229102235221.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
通过测试可以看到，单例的单线程情况下，我在dao实现类中 添加了一个类成员 i，然后在方法中对其进行累加并输出操作，每一个值都会被修改，但如果是多线程，那么会出现多个线程同时修改属性i，导致i最后的结果不是4，这就出现了并发问题。

但是回顾我们从前的编程习惯，似乎我们从未在 service 或 dao 中书写过 类成员，并在方法中对其进行操作，我们一般都是在方法内定义，而这种习惯，也保证了我们现在不会出现这样的问题

将变量定义到方法内

```java
public class AccountDaoImpl implements AccountDao {
    public void addAccount() {
        int i = 1;
        System.out.println("添加用户成功！");
        System.out.println(i);
        i++;
    }
}
```
测试一下
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229102259392.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
好了这样就没有问题了！

讲这么多，就是为了配合 Spring 的学习，前面我们使用工厂模式对传统的程序进行了改造，程序不再与众多资源等直接联系，而是通过工厂进行提供分配，这种被动接受获取对象的方式就是控制反转，也是它的核心之一，现在就可以开始进入正题了

# 3.IOC控制反转

## 3.1 基本概念

一张很形象的图：
![IMG_20220807_165503](D:\Tyora\AssociatedPicturesInTheArticles\Spring框架专题(二)-Spring框架之IoC控制反转\IMG_20220807_165503.jpg)

控制反转（IoC，Inversion of Control），是**<font color="red">一个概念</font>**，是**<font color="red">一种思想</font>**。指将传统上由程序代码直接操控的对象调用权交给容器，通过容器来实现对象的装配和管理。控制反转就是对对象控制权的转移，从程序代码本身反转到了外部容器。**<font color="red">通过容器实现对象的创建，属性赋值，依赖的管理。</font>**

IoC 是一个概念，是一种思想，其实现方式多种多样。当前比较流行的实现方式是依赖注入。应用广泛。

> IoC：在使用Spring框架之后，对象的实例不再由调用者来创建，而是由**Spring容器来创建对象的实例**，Spring容器会负责**控制程序之间的关系**，而不是由调用者的程序代码直接控制。这样，控制权由应用代码转移到了Spring容器，控制权发生了反转，这就是控制反转。
>
> 这是来自教材中的一段话，这段话说了两方面内容：Spring容器创建Bean对象、Spring容器控制Bean对象之间的依赖；从字面上来看IoC有两方面内容：
>
> 1. 由Spring创建实例对象
> 2. 由Spring管理这些实例对象之间的依赖（DI 依赖注入）

**依赖**：classA 类中含有 classB 的实例，在 classA 中调用 classB 的方法完成功能，即 classA对 classB 有依赖。

**Ioc 的实现：**

依赖注入：DI(Dependency Injection)，程序代码不做定位查询，这些工作由容器自行完成。依赖注入 DI 是指程序运行过程中，若需要调用另一个对象协助时，无须在代码中创建被调用者，而是依赖于外部容器，由外部容器创建后传递给程序。

> ​    从Spring容器的角度来看，Spring容器负责将被依赖对象赋值给调用者的成员变量，这相当于为调用者注入了它依赖的实例，这就是Spring的依赖注入。

Spring 的依赖注入对调用者与被调用者几乎没有任何要求，完全支持对象之间依赖关系的管理。

<font color=red>**Spring 框架使用依赖注入（DI）实现 IoC。**</font>

Spring 容器是一个超级大工厂，负责创建、管理所有的 Java 对象，这些 Java 对象被称为 Bean。Spring 容器管理着容器中 Bean 之间的依赖关系，Spring 使用“依赖注入”的方式来管理 Bean 之间的依赖关系。使用 IoC 实现对象之间的解耦和。

## 3.2.第一个入门程序
现在我们就正式开始进入到 Spring 框架的学习中去，而在这部分，并不是说做增删改查，而是通过 Spring 解决依赖的问题，这也就是我们上面众多铺垫内容的原因

由于我们使用的是 maven 创建出一个普通的 java 工程就可以了，不需要创建 java web工程，当然如果不是使用 maven的朋友可以去官网下载jar包 将需要的 bean context core spel log4j 等放到lib中

**A：Maven 导入坐标**

```xml
<dependencies>
     <dependency>
         <groupId>org.springframework</groupId>
         <artifactId>spring-context</artifactId>
         <version>5.0.2.RELEASE</version>
     </dependency>
</dependencies>
```
**B：添加配置文件bean.xml**（一般叫做applicationContext.xml，配置文件的名字可以随便起）

在resources包下面子添加![image-20220807154252319](D:\Tyora\AssociatedPicturesInTheArticles\Spring框架专题(二)-Spring框架之IoC控制反转\image-20220807154252319.png)

   - 引入头部文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">
</beans>
```
**bean.xml**
- 使用spring管理对象创建 (在beans标签中添加 bean标签)
- 也就是说在配置文件中，对service和dao进行配置
- id：对象的唯一标识
- class：指定要创建的对象的全限定类名

```xml
<!--把对象的创建交给spring来管理-->
<bean id="accountDao" class="cn.ideal.dao.impl.AccountDaoImpl"></bean>

<bean id="accountService" class="cn.ideal.service.impl.AccountServiceImpl">
    <!--通过AccountServiceImpl类中的setter方法将beanid为accountDao的实例对象注入到AccountServiceImpl类的ad属性中-->
	<property name = "ad" ref="accountDao"/>
</bean>

```
**C：测试代码**
为什么用这些，等运行后说，先让程序跑起来

```java
public class Client {
    public static void main(String[] args) {
        //获取核心容器
        ApplicationContext ac = new ClassPathXmlApplicationContext("bean.xml");
        //根据id后去Bean对象,下面两种方式都可以
        AccountService as = (AccountService)ac.getBean("accountService");
        AccountDao ad = ac.getBean("accountDao", AccountDao.class);
        System.out.println(as);
        System.out.println(ad);
    }
}
```
**D：执行效果**
程序运行起来是没有问题的，到这里一个入门例程就跑起来了
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229103753362.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)



# 4. 第2、3小节总结

从2.1小节——>2.2小节——>3小节这一过程是在逐渐的引入IOC的概念：

1. 第2.1小节中介绍的是以前的Web怎么写，但是以前的Web项目的写法中存在耦合度过大的问题，因此引入了2.2~2.5小节的内容；
2. **第2.2~2.5小节解决了2.1小节中的耦合度过高的问题**；介绍了怎么使用一个简单的配置文件+工厂模式（BeanFactory类）来降低以前的Web项目中耦合度过高的问题（使用反射创建对象而不是new 实体类去创建），同时还进一步的分析了单例以及多线程并发的问题；
3. **第3小节实现了控制反转，在代码中不需要用户手动new对象（2.2小节的工厂类），并且使用配置文件的方式实现对象之间的依赖**；在第2.2到2.5小节的基础上对配置文件进一步的细化，使用Spring中的工厂模式以及DI（依赖注入）来代替在2.2到2.5小节中需要用户手动调用自定义的工厂类BeanFactory来创建bena实例对象；
   最终形成的结果是：用户只需要手动配置xml配置文件中的bean标签，就可以使Spring自动创建Bean实例，以及Spring使用DI的方式将某个值注入到bean对象中的属性中，这样实现了控制反转



# 5.ApplicationContext

> Spring框架的主要功能是通过其核心容器来实现的，Spring框架提供了两种核心容器：
>
> 1. BeanFactory（接口）
>    是基础类型的IoC容器，它提供了完整的IoC服务支持，简单的是BeanFactory就是一个管理Bean的工厂，它主要负责初始化各种Bean，并调用它们的生命周期方法；
>    该接口最常用的一个实现类是：org.springframework.beand.factory.xml.XmlBeanFactory，该类会根据XML配置文件中的定义来配置Bean。
> 2. ApplicationContext（接口）
>    是BeanFactory接口的子接口，也被称为应用上下文，是另一种常用的Spring核心容器

首先我们来分析一下在调用时的一些内容，测试时，第一个内容，就是获取核心容器，通过了一个 ApplicationContext 进行接收，那么它是什么呢?

**A：与 BeanFactory 的区别**
首先看一下这个图
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229105046763.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
可以看到 BeanFactory 才是 Spring 管理 Bean 的顶级接口，它提供了实例化对象和取出对象的功能，但是由于BeanFactory的简单与一些局限性，有时候并不是很适合于大型企业级的开发，因此，Spring提供了一个新的内容也就是 ApplicationContext：它是一个更加高级的容器，并且功能更加分丰富!

在使用时最明显的一个区别就是：**两者创建对象的时间点不一样!**

**ApplicationContext：单例对象适用采用此接口**

- 构建核心容器时，创建对象时采用立即加载的方式。即：只要一读取完配置文件马上就创建配置文件中的bean对象(看5.4节)

**BeanFactory：多例对象适合**（默认也是单例）

- 构建核心容器时，创建对象时采用延迟加载的方式。即：什么时候根据id获取对象，什么时候才真正的创建对象

下面是使用 BeanFactory 进行测试的代码，不过有一些方法已经过时了，给大家参考使用，可以使用打断点的方式进行测试

```java
Resource resource = new ClassPathResource("bean.xml");
BeanFactory factory = new XmlBeanFactory(resource);
AccountService as  = (AccountService)factory.getBean("accountService");
System.out.println(as);
```
## 5.1.ApplicationContext三个实现类
查看 ApplicationContext 的实现类我们要说的就是红框中的几个
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229105244537.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

```java
ClassPathXmlApplicationContext：可以加载类路径下的配置文件，当然配置文件必须在类路径下（用的更多）
```
```java
FileSystemXmlApplicationContext：可以加载磁盘任意路径下的配置文件（有磁盘访问权限）
```

```java
AnnotationConfigApplicationContext：读取注解创建容器
```
我们由于这篇文章中并没有说注解的问题，所以我们先只看前两个，关于AnnotationConfigApplicationContext的使用方式可以去看《Spring框架专题（三）Spring框架之DI依赖注入.md》3.13节中的测试方法

```java
ApplicationContext ac = new ClassPathXmlApplicationContext("bean.xml");
```

```java
ApplicationContext ac = new FileSystemXmlApplicationContext("D:\\bean.xml");
```
## 5.2.Spring中的Bean配置



> ​     如果把Spring看做一个大型工厂，则Spring容器中的Bean就是该工厂的产品。要想使用这个工厂生产和管理Bean，就需要在配置文件（XML文件 、Properties文件）中告诉它需要哪些Bean，以及需要使用何种方式将这些Bean装配到一起。
>
> **Bean的本质就是Java中的类，而Spring中的Bean其实就是对实体类的引用，来生产Java类对象，从而实现生产和管理Bean **。
>
>   在实际开发中，最常使用的是XML文件格式的配置方式，这种配置方式是通过XML文件来注册并管理Bean之间的依赖关系。

XML配置文件的根元素是<beans>，<beans>中包含了多个<bean>子元素，每一个<bean>子元素定义了一个Bean，并描述了该Bean如何被装配到Spring容器中，介绍一下其中的常用属性或子元素:

- id：对象的唯一标识，Spring容器堆Bean的配置、管理都是通过该属性来完成
- name：Spring容器同样可以通过此属性对容器中的Bean进行配置和管理，name属性中可以为Bean指定多个名称，每个名称之间用逗号或分号隔开
- class：指定要创建的对象的全限定类名，如果在bean中未指定id和name，则Spring会将class的值当作id使用
- scope：指定对象的作用范围

```java
- singleton：单例的（默认）,从Spring容器中获取同一个完全限定名的多个对象，它们的地址都是相同的

- prototype：多例的，从Spring容器中获取同一个完全限定名的多个对象，它们的地址都是不同的

- request：WEB 项目中，Spring 创建 Bean 对象，将对象存入到 request 域中；在一次HTTP请求中，容器都会返回该Bean的同一个实例，对不同的HTTP请求则会产生一个新的Bean，而该Bean仅在当前HTTP Request内有效

- session：WEB 项目中，Spring 创建 Bean 的对象，将对象存入到 session 域中；在一次HTTP Session中，容器会返回该Bean的同一个实例。对不同的HTTP请求则会产生一个新的Bean，而该Bean仅在当前HTTP Session内有效

- global session：WEB 项目中， Portlet 环境使用，若没有 Portlet 环境那么globalSession 相当于 session；在一个全局的HTTP Session中，容器会返回该Bean的同一个实例。

- application：为每个ServletContext对象创建一个实例，仅在Web相关的ApplicationContext中生效

- websocket：为每个websocket对象创建一个实例。仅在Web相关的ApplicationContext中生效
```

- constructor-arg：<bean>的子元素，可以使用此元素传入构造参数进行实例化，该元素的index属性指的构造参数的序号（从0开始），type属性指定构造参数的类型，参数值可以通过ref属性或calue属性直接指定，也可以通过ref或value子元素指定
- property：<bean>的子元素，用于调用Bean实例中的setter方法完成属性赋值，从而完成依赖注入。该元素的name属性指定Bean实例中相应的属性名，ref属性或value属性指定参数值
- ref：<property>、<constructor-arg>等元素的属性或子元素，可以用于指定对Bean共查获那个中某个Bean实例的引用
- value：<property>、<constructor-arg>等元素的属性或子元素，可以用于指定对Bean工厂中某个Bean实例的引用
- list：用于封装List或数组类型的属性的依赖注入
- set：用于封装Set类型的属性的依赖注入
- map：用于封装Map类型的属性的依赖注入
- entry：<map>元素的子元素，用于设置一个键值对。其key属性指定字符串类型的键值，ref或value子元素指定其值，也可以通过value-ref或value属性指定其值
- init-method：指定类中的初始化方法名称
- destroy-method：指定类中销毁方法名称
- lazy-init：设置Spring中Bean对象初始化的时机，这个属性只有在单例模式下有效，在多例模式下Bean对象全部都是在用的时候才创建；
  - false(默认值)，Spring容器加载的时候，立即创建这个Bean对象，可以在项目启动的时候立即暴漏问题
  - true，Spring容器加载的时候，不立即创建这个Bean对象，在实际使用的时候才创建这个Bean独享

在Spring 中默认是单例的，这也就是我们在前面的自定义工厂过程中所做的，在Spring中还需要说明，补充一下：

**作用范围：**

```java
单例对象：在一个应用中只会有一个对象的实例，它的作用范围就是整个应用
多例对象：每一次访问调用对象，会重新创建对象的实例
```
## 5.3 Bean的生命周期：

```java
单例对象：创建容器时出生（调用init-method指定的方法如果有的话），容器在则活着，销毁容器（例如：调用ClassPathXmlApplicationContext类中的close方法，就会关闭Spring容器）时死亡（调用destroy-method指定的方法如果有的话）
    
多例对象：使用对象时出生，堆在则活着，当对象长时间不使用，被垃圾回收回收时死亡
```



> **Spring容器可以管理singleton作用域的Bean的生命周期，在此作用域下，Spring可以精确的的知道该Bean何时被创建，何时初始化完成以及何时被销毁。**
>
> **对于prototype作用域的Bean，Spring只负责创建，当容器创建了Bean实例之后，Bean的实例就会交给客户端代码来管理，Spring容器不会再跟踪其生命周期**；每次客户端请求protytype作用域的Bean时，Spring容器都会创建一个新的实例，并且不会管那些被配置成prototype作用域的Bean生命周期。
>
> 了解Bean生命周期的意义在于：可以在某个Bean生命周期的某些指定时刻完成一些相关操作。这种时刻很多，一般在Bean的postinitiation（初始化后）和preddestruction（销毁之前）。

Spring容器中Bean的生命周期流程图：
![image-20220807185539493](D:\Tyora\AssociatedPicturesInTheArticles\Spring框架专题(二)-Spring框架之IoC控制反转\image-20220807185539493.png)

上图中Bean的生命周期的整个执行过程描述如下：

（1）根据配置情况调用Bean构造方法或工厂方法实例化Bean。

（2）利用依赖注入来完成Bean中所有属性值的配置注入。

（3）如果Bean实现了BeanNameAware接口，则Spring调用Bean的setBeanName()方法传入当前Bean的id值。

（4）如果Bean实现了BeanFactoryAware接口，则Spring调用setBeanFactory()方法传入当前工厂实例的引用。

（5）如果Bean实现了ApplicationContextAware接口，则Spring调用setApplicationContext()方法传入当前ApplicationContext实例的引用。

（6）如果BeanPostProcessor和Bean关联，则Spring将调用该接口的预初始化方法postProcessBeforeInitialzation()对Bean进行加工操作，这个非常重要，Spring的AOP就是用它实现的。

（7）如果Bean实现了InitializingBean接口，则Spring将调用afterPropertiesSet()方法。 

（8）<font color="red">**如果在配置文件中通过init-method属性指定了初始化方法，则调用该初始化方法。**</font>

（9）如果有BeanPsotProcessor和Bean关联，则Spring将调用该接口的初始化方法postProcessAfterInitialization()。此时，Bean已经可以被应用系统使用了。

（10）如果在<bean> 中指定了该Bean的作用范围为 scope="singleton"，则将该Bean 放入Spring IoC的缓存池中，将触发Spring对该Bean的生命周期管理；如果在<bean>中指定了该Bean的作用范围为scope="prototype"，则将该Bean交给调用者，调用者管理该Bean的生命周期，Spring不再管理该Bean。

（11）<font color="red">**如果Bean实现了DisposableBean接口，则Spring会调用destory()方法将Spring中的Bean销毁；如果在配置文件中通过destory-method属性指定了Bean的销毁方法，则Spring将调用该方法进行销毁。**</font>

## 5.4.Spring实例化Bean的三种方式

**<font color = "blue" size=4>`在Spring中想要以IoC控制容器中的Bean的依赖关系，就需要先实例化Bean，Spring创建Bean对象的本质：Spring框架在底层帮我们通过反射new对象。`</font>Spring容器创建Bean对象本质上也是一种控制反转**

**①：使用默认无参构造函数**

也可以使用有参构造去实例化Bean，和使用无参构造实例化Bean的区别只是在bean子元素中使用了子元素construct-arg指定了有参构造的参数；这种方式在使用的时候也可以同时进行依赖注入。

根据默认无参构造函数来创建类对象，若没有无参构造函数，则会创建失败

```java
<bean id="accountService" class="cn.ideal.service.impl.AccountServiceImpl"></bean>
```
在某些情况下，例如我们想要使用一些别人封装好的方法，很有可能存在于jar包中，并且都是一些字节码文件，我们是没有修改的权利了（也就是Spring不能调用这些jar包或字节码文件中想要创建的Bean实体类的无参构造方法），那这时候我们想要使用还可以使用下面两种方法!

**②：Spring 实例工厂**
使用普通工厂中的方法创建对象，存入Spring

- id：指定实例工厂中 bean 的 id
- factory-bean：指定配置的实例工厂
- factory-method：指定实例工厂中创建对象的方法
  - 如果这个工厂方法有参数，那么需要用construct-arg子元素来配置这个工厂方法的参数

模拟一个实例工厂，创建业务层实现类，这种情况下，必须先有工厂实例对象，才能调用方法

```java
public class InstanceFactory {
	public AccountService createAccountService(){
		return new AccountServiceImpl();
	} 
}
```

```xml
<!--实例工厂的Bean，在静态工厂方式中不用创建静态工厂的Bean-->
<bean id="instancFactory" class="cn.ideal.factory.InstanceFactory"></bean> 

<!--AccountServiceImpl类对应的Bean-->
<bean id="accountService" factory-bean="instancFactory" factory-method="createAccountService"></bean>
```
使用场景：

1. Spring框架整合第三个jar包，如果想要在Spring中创建jar包中某个类的Bean实例，但是Spring不能调用这个类的无参构造方法（可能是私有的），而在这个jar包中有能创建这个类的实例工厂，这个时候就可以使用这种方式。

**③：Spring 静态工厂**
使用工厂中的静态方法创建对象

- id：指定 bean id
- class：静态工厂的全限定类名
- factory-method：指定生产对象的静态方法

```java
public class StaticFactory {
	public static IAccountService createAccountService(){
		return new AccountServiceImpl();
	} 
}
```

```xml
<!--不用创建静态工厂的Bean-->
<bean id="accountService" class="cn.ideal.factory.StaticFactory" factory-method="createAccountService"></bean>
```

## 5.5 Spring框架的DI

**依赖注入的作用就是在使用Spring框架创建对象（实例化Bean）的时候，动态的将其所依赖的对象注入Bean组件中；**
其实现方式通常有两种：

1. 属性setter方法注入
   指Sprig容器使用setter方法注入被依赖的实例。在实例化Bean之后，通过该Bean的setter方法，既可以实现基于setter方法的依赖注入。
   这种方式对被创建的Bean对应的类有要求：
   1. Bean类必须提供一个默认的无参构造方法
   2. Bean类必须为需要注入的属性提供对应的setter方法
   3. 在配置文件中使用<property>元素来为Meier个属性注入值
2. 构造方法注入
   指Spring容器使用构造方法注入被依赖的实例。基于构造方法的依赖注入通过调用带参数的构造方法来实现，每个参数代表一个依赖。
   这种方式对被创建的Bean对应的类有要求：
   1. Bean类需要提供带有所有参数的有参构造方法
   2. 在配置文件中使用<constructor-arg>元素来定义构造方法的参数，可以使用这个元素的value属性或子元素来折之该参数的值

有关更多详细内容请看《Spring框架专题(三)-Spring框架之DI依赖注入.md》
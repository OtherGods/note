@[TOC](文章目录)
# 1.引言
动态代理在 Java 中有着广泛的应用，比如 AOP 的实现原理、RPC远程调用、Java 注解对象获取、日志框架、全局性异常处理、事务处理等。

在了解动态代理前，我们需要先了解一下什么是代理模式。
# 2.代理模式
`代理模式(Proxy Pattern)`是 23 种设计模式的一种，属于结构型模式。他指的是一个对象本身不做实际的操作，而是通过其他对象来得到自己想要的结果。这样做的好处是可以在**目标对象实现的基础上，增强额外的功能操作，即扩展目标对象的功能**。
>这里能体现出一个非常重要的编程思想：不要随意去改源码，如果需要修改，可以通过代理的方式来扩展该方法。

![在这里插入图片描述](https://img-blog.csdnimg.cn/2020122917263491.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
如上图所示，用户不能直接使用目标对象，而是构造出一个代理对象，由代理对象作为中转，代理对象负责调用目标对象真正的行为，从而把结果返回给用户。

也就是说，**代理的关键点就是代理对象和目标对象的关系**。

**代理模式主要由三个元素共同构成：**

1）一个接口，接口中的方法是要真正去实现的。
2）被代理类，实现上述接口，这是真正去执行接口中方法的类。
3）代理类，同样实现上述接口，同时封装被代理类对象，帮助被代理类去实现方法

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201229173024989.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
使用代理模式必须要让代理类和目标类实现相同的接口，客户端通过代理类来调用目标方法，代理类会将所有的方法调用分派到目标对象上反射执行，还可以在分派过程中添加"前置通知"和后置处理!

（如在调用目标方法前校验权限，在调用完目标方法后打印日志等）等功能。


# 3.静态代理
	**第一步**：创建 UserService 接口

```java
public interface UserService {

	// 添加 user
	public void addUser(User user);

	// 删除 user
	public void deleteUser(int uid);
}
```

	**第二步**:创建 UserService的实现类

```java
public class UserServiceImpl implements UserService {

	public void addUser(User user) {
		System.out.println("增加 User");
	}

	public void deleteUser(int uid) {
		System.out.println("删除 User");
	}
}
```
	**第三步**:创建事务类

```java
public class MyTransaction {

	// 开启事务
	public void before() {
		System.out.println("开启事务");
	}

	// 提交事务
	public void after() {
		System.out.println("提交事务");
	}
}

```
	**第四步**：创建代理类 ProxyUser.java

```java
public class ProxyUser implements UserService {

	// 真实类
	private UserService userService;
	// 事务类
	private MyTransaction transaction;

	// 使用构造函数实例化
	public ProxyUser(UserService userService, MyTransaction transaction) {
		this.userService = userService;
		this.transaction = transaction;
	}

	public void addUser(User user) {
		transaction.before();
		userService.addUser(user);
		transaction.after();
	}

	public void deleteUser(int uid) {
		transaction.before();
		userService.deleteUser(uid);
		transaction.after();
	}
}

```
	**测试**：

```java
public class TestUser {

	@Test
	public void testOne() {
		MyTransaction transaction = new MyTransaction();
		UserService userService = new UserServiceImpl();
		// 产生静态代理对象
		ProxyUser proxy = new ProxyUser(userService, transaction);
		proxy.addUser(null);
		proxy.deleteUser(0);
	}
}

```
运行结果：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200831161939701.png#pic_left)

这是一个很基础的静态代理，业务类UserServiceImpl 只需要关注业务逻辑本身，保证了业务的重用性，这也是代理类的优点，没什么好说的。我们主要说说这样写的缺点：

①、代理对象的一个接口只服务于一种类型的对象，如果要代理的方法很多，势必要为每一种方法都进行代理，静态代理在程序规模稍大时就无法胜任了。

②、如果接口增加一个方法，比如 UserService 增加修改 updateUser()方法，则除了所有实现类需要实现这个方法外，所有代理类也需要实现此方法。增加了代码维护的复杂度。

# 4.使用JDK动态代理
动态代理就不要自己手动生成代理类了，我们去掉 ProxyUser.java 类，增加一个ObjectInterceptor.java 类

```java
public class ObjectInterceptor implements InvocationHandler {

	// 目标类
	private Object target;
	// 切面类（这里指事务类）
	private MyTransaction transaction;

	// 通过构造器赋值
	public ObjectInterceptor(Object target, MyTransaction transaction) {
		this.target = target;
		this.transaction = transaction;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// 开启事务
		this.transaction.before();
		// 调用目标类方法
		method.invoke(this.target, args);
		// 提交事务
		this.transaction.after();
		return null;
	}
}

```
	测试类

```java
public class TestUser2 {

	@Test
	public void testOne() {
		// 目标类
		Object target = new UserServiceImpl();
		// 事务类
		MyTransaction transaction = new MyTransaction();
		ObjectInterceptor proxyObject = new ObjectInterceptor(target, transaction);
		/**
		 * 三个参数的含义： 1、目标类的类加载器 2、目标类所有实现的接口 3、拦截器
		 */
		UserService userService = (UserService) Proxy.newProxyInstance(target.getClass().getClassLoader(), target.getClass().getInterfaces(), proxyObject);
		userService.addUser(null);
		userService.deleteUser(11);
	}
}

```
运行结果：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200831162112767.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70#pic_left)

那么使用动态代理来完成这个需求就很好了，后期在 UserService 中增加业务方法，都不用更改代码就能自动给我们生成代理对象。而且将 UserService 换成别的类也是可以的。也就是做到了代理对象能够代理多个目标类，多个目标方法。

**查看JDK动态代理的生成的class文件：**

```java
/**
 * 保存 JDK 动态代理生产的类
 * @param filePath 保存路径，默认在项目路径下生成 $Proxy0.class 文件
 */
private static void saveProxyFile(String... filePath) {
    if (filePath.length == 0) {
        System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles", "true");
    } else {
        FileOutputStream out = null;
        try {
            byte[] classFile = ProxyGenerator.generateProxyClass("$Proxy0", IronManVIPMovie.class.getInterfaces());
            String path=filePath[0] + "$Proxy0.class";
            System.out.println(path);
            out = new FileOutputStream(path);
            out.write(classFile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```
# 5.使用CGLIB动态代理
使用JDK创建代理有一个限制,它只能为接口创建代理实例.这一点可以从Proxy的接口方法 `newProxyInstance(ClassLoader loader,Class [] interfaces,InvocarionHandler h)`中看的很清楚
     第二个入参 interfaces就是需要代理实例实现的接口列表.

     对于没有通过接口定义业务方法的类,如何动态创建代理实例呢? JDK动态代理技术显然已经黔驴技穷,CGLib作为一个替代者,填补了这一空缺.

CGLib采用底层的字节码技术,可以为一个类创建子类,在子类中采用方法拦截的技术拦截所有父类方法的调用并顺势志入横切逻辑.

```xml
<dependency>
		<groupId>cglib</groupId>
		<artifactId>cglib</artifactId>
		<version>2.2</version>
</dependency>
```
	创建创建CGLib代理器

```java
public class CglibProxy implements MethodInterceptor {

	private Enhancer enhancer = new Enhancer();

	// 切面类（这里指事务类）
	private MyTransaction transaction;

	public CglibProxy(MyTransaction transaction) {
		this.transaction=transaction;
	}

	// 设置被代理对象
	public Object getProxy(Class clazz) {
		enhancer.setSuperclass(clazz);
		enhancer.setCallback(this);
		return enhancer.create();
	}

	public Object intercept(Object obj, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
		transaction.before();
		Object invoke = methodProxy.invokeSuper(obj, objects);
		transaction.after();
		return invoke;
	}
}

```
	测试类

```java
   @Test
	public void testOne() {
		// 事务类
		MyTransaction transaction = new MyTransaction();
		CglibProxy cglibProxy = new CglibProxy(transaction);
		UserServiceImpl userService = (UserServiceImpl) cglibProxy.getProxy(UserServiceImpl.class);
		userService.addUser(null);
		userService.deleteUser(11);
	}

```
**查看CGLIB动态代理的生成的class文件：**

```java
// 在指定目录下生成动态代理类
System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:\\classcglib");

System.out.println("=========CGLIB$CALLBACK_0==========");
Field h = userService.getClass().getDeclaredField("CGLIB$CALLBACK_0");
h.setAccessible(true);
Object obj = h.get(userService);
System.out.println(obj.getClass());
```

# 6.JDK和CGLIB动态代理总结
## 6.1.原理区别

java动态代理是利用反射机制生成一个实现代理接口的匿名类，在调用具体方法前调用InvokeHandler来处理。核心是实现InvocationHandler接口，使用invoke()方法进行面向切面的处理，调用相应的通知。

而cglib动态代理是利用asm开源包，对代理对象类的class文件加载进来，通过修改其字节码生成子类来处理。核心是实现MethodInterceptor接口，使用intercept()方法进行面向切面的处理，调用相应的通知。

1. 如果目标对象实现了接口，默认情况下会采用JDK的动态代理实现AOP

2. 如果目标对象实现了接口，可以强制使用CGLIB实现AOP

3. 如果目标对象没有实现了接口，必须采用CGLIB库，spring会自动在JDK动态代理和CGLIB之间转换

可以强制使用CGlib（在spring配置中加入`<aop:aspectj-autoproxy proxy-target-class=“true”/>`）
springboot项目配置： `spring.aop.proxy-target-class=false`


## 6.2.CGlib比JDK快？

1、CGLib底层采用ASM字节码生成框架，使用字节码技术生成代理类，在jdk6之前比使用Java反射效率要高。唯一需要注意的是，CGLib不能对声明为final的方法进行代理，因为CGLib原理是动态生成被代理类的子类。

2、在jdk6、jdk7、jdk8逐步对JDK动态代理优化之后，在调用次数较少的情况下，JDK代理效率高于CGLIB代理效率，只有当进行大量调用的时候，jdk6和jdk7比CGLIB代理效率低一点，但是到jdk8的时候，jdk代理效率高于CGLIB代理。

3、在对JDK动态代理与CGlib动态代理的代码实验中看，1W次执行下，JDK7及8的动态代理性能比CGlib要好20%左右。


## 6.3.各自局限：

1、JDK的动态代理机制只能代理实现了接口的类，而不能实现接口的类就不能实现JDK的动态代理。

2、cglib是针对类来实现代理的，他的原理是对指定的目标类生成一个子类，并覆盖其中方法实现增强，但因为采用的是继承，所以不能对final修饰的类进行代理。

# 7. 总结
|类型|机制  |回调方式|适用场景|效率|
|--|--|--|--|--|
|JDK动态代理  |委托机制，代理类和目标类都实现了同样的接口，InvocationHandler持有目标类，代理类委托InvocationHandler去调用目标类原始方法 |反射| 目标类是接口| 效率瓶颈在反射调用稍慢|
|CGLIB动态代理  |继承机制，代理类继承了目标类并重写了目标方法，通过回调函数MethodInterceptor调用父类方法执行原始逻辑| 通过FastClass方法索引调用| 非接口类，非final类，非final方法|第一次调用因为要生成多个Class对象较JDK慢，多次调用因为方法索引较反射方式快，如果方法多swtich case过多其效率还需测试|








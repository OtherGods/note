# 典型回答

Java 中区分 API 和 SPI，通俗的讲：API 和 SPI 都是相对的概念，他们的差别只在语义上，API 直接被应用开发人员使用，SPI 被框架扩展人员使用。

`API（Application Programming Interface）`是一组定义了软件组件之间交互的规则和约定的接口。提供方来制定接口并完成对接口的不同实现，调用方只需要调用即可。

`SPI（Service Provider Interface）`是**一种扩展机制**，通常用于在应用程序中提供可插拔的实现。 调用方可选择使用提供方提供的内置实现，也可以自己实现。

请记住这句话：**API用于定义调用接口，而SPI用于定义和提供可插拔的实现方式。**

所以说，API 是面向普通开发者的，提供一组功能，使他们可以利用一个库或框架来实现具体的功能。而SPI 是面向那些希望扩展或定制基础服务的开发者的，它定义了一种机制，让其他开发者可以提供新的实现或扩展现有的功能。

# 知识扩展

## 如何定义一个SPI

### 步骤1、定义一组接口

(假设是`org.foo.demo.IShout`)，并写出接口的一个或多个实现，(假设是`org.foo.demo.animal.Dog`、`org.foo.demo.animal.Cat`)。
```java
public interface IShout {
    void shout();
}
public class Cat implements IShout {
    @Override
    public void shout() {
    	System.out.println("miao miao");
	}
}
public class Dog implements IShout {
    @Override
    public void shout() {
    	System.out.println("wang wang");
    }
}
```

### 步骤2、定义配置

在 `src/main/resources/` 下建立 `/META-INF/services` 目录， 新增一个以接口命名的文件 (`org.foo.demo.IShout`文件)，内容是要应用的实现类（这里是`org.foo.demo.animal.Dog`和`org.foo.demo.animal.Cat`，每行一个类）。
```java
org.foo.demo.animal.Dog
org.foo.demo.animal.Cat
```

### 步骤3、加载配置

使用 `ServiceLoader` 来加载配置文件中指定的实现。
```java
public class SPIMain {
    public static void main(String[] args) {
        ServiceLoader<IShout> shouts = ServiceLoader.load(IShout.class);
        for (IShout s : shouts) {
        	s.shout();
        }
    }
}
```

代码输出：
wang wang
miao miao

## SPI的实现原理

看`ServiceLoader`类的签名类的成员变量：
```java
public final class ServiceLoader<S> implements Iterable<S>{
    private static final String PREFIX = "META-INF/services/";
    // 代表被加载的类或者接口
    private final Class<S> service;
    // 用于定位，加载和实例化providers的类加载器
    private final ClassLoader loader;
    // 创建ServiceLoader时采用的访问控制上下文
    private final AccessControlContext acc;
    // 缓存providers，按实例化的顺序排列
    private LinkedHashMap<String,S> providers = new LinkedHashMap<>();
    // 懒查找迭代器
    private LazyIterator lookupIterator;
    ......
}
```

参考具体源码，梳理了一下，实现的流程如下：
1. 应用程序调用`ServiceLoader.load`方法，`ServiceLoader.load`方法内先创建一个新的`ServiceLoader`，并实例化该类中的成员变量，包括：
	1. `loader`：ClassLoader类型，类加载器
	2. `acc`：AccessControlContext类型，访问控制器
	3. `providers`：LinkedHashMap类型，用于缓存加载成功的类
	4. `lookupIterator`：实现迭代器功能
2. 应用程序 **通过迭代器接口获取对象实例**，
	1. `ServiceLoader`先判断成员变量`providers`对象中(`LinkedHashMap`类型)是否有缓存实例对象，如果有缓存，直接返回。
	2. 如果没有缓存，执行类的装载：
		1. 读取`META-INF/services/`下的配置文件，获得所有能被实例化的类的名称
		2. 通过反射方法`Class.forName()`加载类对象，并用`instance()`方法将类实例化
		3. 把实例化后的类缓存到`providers`对象中(`LinkedHashMap`类型）
		4. 然后返回实例对象。

## SPI的应用场景

概括地说，适用于：调用者根据实际使用需要，启用、扩展、或者替换框架的实现策略。比较常见的例子：
1. 数据库驱动加载接口实现类的加载
2. JDBC加载不同类型数据库的驱动
3. 日志门面接口实现类加载
4. SLF4J加载不同提供商的日志实现类

**Spring**
Spring中大量使用了SPI,比如：对servlet3.0规范对ServletContainerInitializer的实现、自动类型转换Type Conversion SPI(Converter SPI、Formatter SPI)等

**Dubbo**
Dubbo中也大量使用SPI的方式实现框架的扩展, 不过它对Java提供的原生SPI做了封装，允许用户扩展实现Filter接口


Java中的SPI（Service Provider Interface）是一种**服务发现机制**，允许框架或库定义接口，并由第三方提供具体实现，从而实现**解耦**和**动态扩展**。它的核心思想是 **“面向接口编程 + 配置文件约定”**，使得应用程序无需修改代码即可集成新的实现。

# ChatGPT-SPI 的核心机制
1. **接口定义**  
   框架或库定义一个接口（例如`com.example.DatabaseDriver`）。
   
2. **服务实现**  
   第三方提供者实现该接口（例如`MySQLDriver`、`PostgreSQLDriver`）。

3. **配置文件注册**  
   在提供者的JAR包中，创建`META-INF/services/接口全限定名`文件（如`META-INF/services/com.example.DatabaseDriver`），文件内容为实现类的全限定名：
   ```text
   com.provider.MySQLDriver
   com.provider.PostgreSQLDriver
   ```

4. **服务加载**  
   使用`ServiceLoader`动态加载所有实现类：
   ```java
   ServiceLoader<DatabaseDriver> drivers = ServiceLoader.load(DatabaseDriver.class);
   for (DatabaseDriver driver : drivers) {
       driver.connect();
   }
   ```

## **SPI 的典型应用场景**
- **JDBC 驱动加载**  
  JDBC的`DriverManager`通过SPI自动发现数据库驱动（如MySQL的`com.mysql.cj.jdbc.Driver`）。
  
- **日志框架适配**  
  SLF4J通过SPI绑定具体的日志实现（如Logback、Log4j2）。
  
- **Spring Boot自动配置**  
  Spring Boot的`spring.factories`文件本质是SPI的扩展，用于自动加载配置类。

- **序列化框架**  
  Jackson、Fastjson等通过SPI注册模块扩展。

## **SPI 的优缺点**
**优点**：
- **解耦**：接口与实现分离，无需修改调用方代码。
- **扩展性**：新增实现只需添加JAR包和配置文件。
- **动态发现**：运行时自动加载服务。

**缺点**：
- **配置文件易出错**：路径或类名错误会导致加载失败。
- **性能**：`ServiceLoader`是懒加载，但遍历所有实现可能耗时。
- **无法按需加载**：会实例化所有实现，即使未使用。

## **SPI 与 API 的区别**
| **SPI**               | **API**               |
| --------------------- | --------------------- |
| 由**框架定义接口**，**提供者实现** | 由**框架定义接口**，**调用方使用** |
| 服务提供者主动注册（如JDBC驱动）    | 调用方直接调用接口（如Java集合API） |
| 典型工具：`ServiceLoader`  | 典型场景：方法调用             |

## **示例代码**
1. **定义接口**  
   ```java
   package com.example;
   public interface MessageService {
       String getMessage();
   }
   ```

2. **实现接口**  
   ```java
   package com.provider;
   public class EmailService implements MessageService {
       @Override
       public String getMessage() {
           return "Email message";
       }
   }
   ```

3. **创建配置文件**  
   文件：`META-INF/services/com.example.MessageService`  
   内容：`com.provider.EmailService`

4. **加载服务**  
   ```java
   ServiceLoader<MessageService> services = ServiceLoader.load(MessageService.class);
   for (MessageService service : services) {
       System.out.println(service.getMessage()); // 输出：Email message
   }
   ```

## **总结**
SPI 是 Java 实现 **插件化架构** 的核心机制，适用于需要 **动态扩展** 的场景。尽管它依赖约定式配置，但在标准库（如JDBC）和主流框架中广泛应用。对于更复杂的需求，可以结合Spring的`@Conditional`或Java模块化系统（JPMS）进一步增强。
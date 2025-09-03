javaw web三要素Filter, Servlet前面分别进行了介绍，接下来我们看一下Listener的相关知识点，本篇博文主要内容为 [SpringBoot](https://spring.hhui.top/spring-blog/2019/12/06/191206-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Listener%E5%9B%9B%E7%A7%8D%E6%B3%A8%E5%86%8C%E5%A7%BF%E5%8A%BF/#)环境下，如何自定义Listener并注册到spring容器

## 1、环境配置

### 1.2、项目搭建

首先我们需要搭建一个web工程，以方便后续的servelt注册的实例演示，可以通过spring boot官网创建工程，也可以建立一个maven工程，在pom.xml中如下配置
```xml
<parent>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-parent</artifactId>  
    <version>2.2.1.RELEASE</version>  
    <relativePath/> <!-- lookup parent from repository -->  
</parent>  
  
<properties>  
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>  
    <java.version>1.8</java.version>  
</properties>  
  
<dependencies>  
    <dependency>  
        <groupId>org.springframework.boot</groupId>  
        <artifactId>spring-boot-starter-web</artifactId>  
    </dependency>  
</dependencies>  
  
<build>  
    <pluginManagement>  
        <plugins>  
            <plugin>  
                <groupId>org.springframework.boot</groupId>  
                <artifactId>spring-boot-maven-plugin</artifactId>  
            </plugin>  
        </plugins>  
    </pluginManagement>  
</build>  
<repositories>  
    <repository>  
        <id>spring-snapshots</id>  
        <name>Spring Snapshots</name>  
        <url>https://repo.spring.io/libs-snapshot-local</url>  
        <snapshots>  
            <enabled>true</enabled>  
        </snapshots>  
    </repository>  
    <repository>  
        <id>spring-milestones</id>  
        <name>Spring Milestones</name>  
        <url>https://repo.spring.io/libs-milestone-local</url>  
        <snapshots>  
            <enabled>false</enabled>  
        </snapshots>  
    </repository>  
    <repository>  
        <id>spring-releases</id>  
        <name>Spring Releases</name>  
        <url>https://repo.spring.io/libs-release-local</url>  
        <snapshots>  
            <enabled>false</enabled>  
        </snapshots>  
    </repository>  
</repositories>
```

## 2、Listener注册

我们这里说到的Listener专指java web相关的监听器，与 [Spring](https://spring.hhui.top/spring-blog/2019/12/06/191206-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Listener%E5%9B%9B%E7%A7%8D%E6%B3%A8%E5%86%8C%E5%A7%BF%E5%8A%BF/#)本身的Listener并不一样。在java web中Listener的知识点为servlet规范的那一套，这里不详细展开。下面主要介绍在 [SpringBoot](https://spring.hhui.top/spring-blog/2019/12/06/191206-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Listener%E5%9B%9B%E7%A7%8D%E6%B3%A8%E5%86%8C%E5%A7%BF%E5%8A%BF/#)中使用Servlet Listener的四种方式

### 2.1、WebListener注解

`@WebListener`注解为Servlet3+提供的注解，可以标识一个类为Listener，使用姿势和前面的Listener、Filter并没有太大的区别
```java
@WebListener  
public class AnoContextListener implements ServletContextListener {  
    @Override  
    public void contextInitialized(ServletContextEvent sce) {  
        System.out.println("@WebListener context 初始化");  
    }  
  
    @Override  
    public void contextDestroyed(ServletContextEvent sce) {  
        System.out.println("@WebListener context 销毁");  
    }  
}
```

因为WebListener注解不是spring的规范，所以为了识别它，需要在启动类上添加注解`@ServletComponentScan`
```java
@ServletComponentScan  
@SpringBootApplication  
public class Application {  
    public static void main(String[] args) {  
        SpringApplication.run(Application.class);  
    }  
}
```

### 2.2、普通bean

第二种使用方式是将Listener当成一个普通的spring bean，spring boot会自动将其包装为`ServletListenerRegistrationBean`对象
```java
@Component  
public class BeanContextListener implements ServletContextListener {  
    @Override  
    public void contextInitialized(ServletContextEvent sce) {  
        System.out.println("bean context 初始化");  
    }  
  
    @Override  
    public void contextDestroyed(ServletContextEvent sce) {  
        System.out.println("bean context 销毁");  
    }  
}
```

### 2.3、ServletListenerRegistrationBean

通过java config来主动将一个普通的Listener对象，塞入`ServletListenerRegistrationBean`对象，创建为spring的bean对象
```java
public class ConfigContextListener implements ServletContextListener {  
    @Override  
    public void contextInitialized(ServletContextEvent sce) {  
        System.out.println("config context 初始化");  
    }  
  
    @Override  
    public void contextDestroyed(ServletContextEvent sce) {  
        System.out.println("java context 销毁");  
    }  
}
```

上面只是一个普通的类定义，下面的bean创建才是关键点
```java
@Bean  
public ServletListenerRegistrationBean configContextListener() {  
    ServletListenerRegistrationBean bean = new ServletListenerRegistrationBean();  
    bean.setListener(new ConfigContextListener());  
    return bean;  
}
```

### 2.4、ServletContextInitializer

这里主要是借助在ServletContext上下文创建的实际，主动的向其中添加Filter，Servlet， Listener，从而实现一种主动注册的效果
```java
public class SelfContextListener implements ServletContextListener {  
    @Override  
    public void contextInitialized(ServletContextEvent sce) {  
        System.out.println("ServletContextInitializer context 初始化");  
    }  
  
    @Override  
    public void contextDestroyed(ServletContextEvent sce) {  
        System.out.println("ServletContextInitializer context 销毁");  
    }  
}  
  
@Component  
public class ExtendServletConfigInitializer implements ServletContextInitializer {  
    @Override  
    public void onStartup(ServletContext servletContext) throws ServletException {  
        servletContext.addListener(SelfContextListener.class);  
    }  
}
```

注意ExtendServletConfigInitializer的主动注册时机，在启动时添加了这个Listenrer，所以它的优先级会是最高

### 2.5、测试

上面介绍了四种注册方式，都可以生效，在我们的实际开发中，按需选择一种即可，不太建议多种方式混合使用；

项目启动和关闭之后，输出日志如下


## 3、其他
## 3.1、项目源码
- 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 项目：[https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/210-web-filter](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/210-web-filter)
已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)

转载自：[【WEB系列】Listener四种注册姿势](https://spring.hhui.top/spring-blog/2019/12/06/191206-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87Listener%E5%9B%9B%E7%A7%8D%E6%B3%A8%E5%86%8C%E5%A7%BF%E5%8A%BF/)


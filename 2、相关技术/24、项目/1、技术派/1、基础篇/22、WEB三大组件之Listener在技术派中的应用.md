技术派中当前主要使用web listener来实现实时在线人数统计，当然这个统计方案目前只适用于单机的场景，更多的是给大家展示一下web listener的相关知识点。
# 1、使用实例

源码地址：com.github.paicoding.forum.web.hook.listener.OnlineUserCountListener
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308061530285.png)

关于上面这个监听器如何实现在线人数统计，详情查看教程
- [技术派实时在线人数统计](https://www.yuque.com/itwanger/az7yww/bn619l7kpy57mhbg)
# 2、JavaWeb监听器
## 2.1、基本知识点

WEB中的Listener主要由三类八种（主要监听三个领域对象）
1. **监听三个域对象的<font color = "red">创建和销毁</font>的监听器：**
	1. ServletContextListener
		1. 服务器启动时创建ServletContext
		2. 服务器销毁时移除ServletContext
	2. HttpSessionListener（技术派中主要使用的就是它）
		1. 会话HttpSession的创建
		2. 会话HttpSession的移除
	3. ServletRequestListener
		1. web请求ServletRequest的创建
		2. web请求ServletRequest的销毁
2. **监听三个域对象的<font color = "red">属性变更</font>的监听器（属性添加、移除、替换）**
	1. ServletContextAttributeListener
	2. HttpSessionAttributeListener
	3. ServletRequestAttributeListener
3. **监听<font color = "red">HttpSession中的JavaBean的状态改变</font>（绑定、解除、钝化、活化）**
	1. HttpSessionBindingListener
	2. HttpSessionActivationListener

## 2.2、使用示例

具体的使用姿势比较简单，无非是实现上面的几个接口，然后再对应的触发方法中添加自己的实际业务逻辑
接下来重点说一下web listener的几种不同注册方式
### 2.2.1、WebListener注解

@WebListener注解为Servlet3+提供的注解，可以表示为一个类为Listener，使用姿势和前面的Servlet、Filter并没有太大的区别
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

因为WebListener注解不是Spring的规范，所以为识别它，需要在启动类上添加注解@ServletComponentScan
```java
@ServletComponentScan
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
	}
}
```

### 2.2.2、普通bean

第二种使用方式是将Listener当成一个普通的spring bean，SpringBoot会自动将其包装为ServletListenerRegistrationBean对象
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

### 2.2.3、ServletListenerRegistrationBean

通过Java config来主动将一个普通Lsitener对象，塞入ServletListenerRegistrationBean对象，创建为Spring的bean对象
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

上面只是一个普通的类定义，下面的bean创建才是关键点：
```Java
@Bean
public ServletListenerRegistrationBean configContextListener() {
    ServletListenerRegistrationBean bean = new ServletListenerRegistrationBean();
    bean.setListener(new ConfigContextListener());
    return bean;
}
```

### 2.2.4、ServletContextInitializer

这里主要是借助在ServletContext上下文创建的时机，主动的向其中添加Filter、Servlet、Listener（添加三大组件时需要调用不同的addXxx方法），从而实现一种主动注册的效果
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

注意ExtendServletConfigInitializer的主动注册时机，在类启动时添加了这个Listener，所以它的优先级是最高的

## 2.3、小结
java web listener的知识点本身并不复杂，现如今的技术栈下，实际需要我们使用的场景也很少，以上相关知识点可以当做选修，看一下即可；当然技术派中也给出了一个基于HttpSessionListener的实战场景，实时的在线人数统计功能，有兴趣的小伙伴可以看一下那篇教程
- [技术派实时在线人数统计](https://www.yuque.com/itwanger/az7yww/bn619l7kpy57mhbg)







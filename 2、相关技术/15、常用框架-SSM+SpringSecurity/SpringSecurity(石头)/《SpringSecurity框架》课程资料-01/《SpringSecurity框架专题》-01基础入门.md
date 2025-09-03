## 1、安全框架概述

什么是安全框架？ 解决系统安全问题的框架。如果没有安全框架，我们需要手动处理每个资源的访问控制，非常麻烦。使用安全框架，我们可以通过配置的方式实现对资源的访问限制。
## 2、常用安全框架 
- `Spring Security`：Spring家族一员。是一个能够为基于Spring的企业应用系统提供声明式的安全访问控制解决方案的安全框架。它提供了一组可以在Spring应用上下文中配置的Bean，充分利用了Spring IoC ， DI（控制反转Inversion of Control,DI:Dependency Injection 依赖注入）和 AOP（面向切面编程） 功能，为应用系统提供声明式的安全访问控制功能，减少了为企业系统安全控制编写大量重复代码的工作。
- `Apache Shiro`：一个功能强大且易于使用的Java安全框架,提供了认证,授权,加密,和会话管理。

## 2、Spring Security 概述

### 2.1.概述
在web应用开发中，安全无疑是十分重要的，选择`Spring Security`来保护web应用是一个非常好的选择。`Spring Security` 是Spring项目之中的一个安全模块，可以非常方便与Spring项目无缝集成。利用 `Spring IoC/DI`和AOP功能，为系统提供了声明式安全访问控制功能，减少了为系统安全而编写大量重复代码的工作。特别是SpringBoot项目中加入`Spring Security`更是十分简单。本篇我们介绍`Spring Security`，以及`Spring Security`在web应用中的使用。

### 2.2.核心功能
- 认证Authentication  :（你是谁）比如用户登入、系统授权访问
- 授权Authorization :（你能干什么）当前用户有哪些资源可以访问
- 攻击防护 :（防止伪造身份）

其核心就是 **==一组过滤器链==**，项目启动后将会自动配置。最核心的就是 `Basic Authentication Filter` 用来认证用户的身份，一个在`spring security`中一种过滤器处理一种认证方式。

### 2.3.历史
`Spring Security` 以“`The Acegi Secutity System for Spring`”的名字始于2003年年底。其前身为 `acegi`项目。起因是 Spring 开发者邮件列表中一个问题，有人提问是否考虑提供一个基于 Spring 的安全实现。限制于时间问题，开发出了一个简单的安全实现，但是并没有深入研究。几周后，Spring 社区中其他成员同样询问了安全问题，代码提供给了这些人。2004 年 1 月份已经有 20 人左右使用这个项目。随着更多人的加入，在 2004 年 3 月左右在 sourceforge 中建立了一个项目。在最开始并没有认证模块，所有的认证功能都是依赖容器完成的，而 `acegi` 则注重授权。但是随着更多人的使用，基于容器的认证就显现出了不足。`acegi` 中也加入了认证功能。大约 1 年后 `acegi` 成为 Spring子项目。在 2006 年 5 月发布了 acegi 1.0.0 版本。`2007` 年底 acegi 更名为`Spring Security`。

## 3、初识Spring Security
### 3.1.Spring Security概念
Spring Security是spring采用`AOP`思想，基于`servlet`过滤器实现的安全框架。它提供了完善的认证机制和方法级的授权功能。是一款非常优秀的权限管理框架。

### 3.2. 快速入门案例
入门案例我们是通过spring+springmvc环境来搭建的。所以需要先准备项目环境

### 3.3. 环境准备
#### 3.3.1 创建web项目
通过idea工具创建一个基于maven的web项目
![在这里插入图片描述](https://img-blog.csdnimg.cn/a513b633e391416d8b73ac96a909a387.png)

#### 3.3.2 导入相关的依赖

```xml
<dependencies>
    
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-config</artifactId>
            <version>5.1.5.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-taglibs</artifactId>
            <version>5.1.5.RELEASE</version>
        </dependency>
    
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-jdbc</artifactId>
            <version>5.1.6.RELEASE</version>
        </dependency>
    
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
            <version>1.7.26</version>
        </dependency>
    
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
            <version>5.1.6.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>5.1.47</version>
        </dependency>
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis</artifactId>
            <version>3.5.1</version>
        </dependency>
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis-spring</artifactId>
            <version>2.0.1</version>
        </dependency>
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>4.0.1</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>jsp-api</artifactId>
            <version>2.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>jstl</groupId>
            <artifactId>jstl</artifactId>
            <version>1.2</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.8</version>
        </dependency>
    
        <dependency>
            <groupId>javax.annotation</groupId>
            <artifactId>jsr250-api</artifactId>
            <version>1.0</version>
        </dependency>
    
    </dependencies>
```

#### 3.3.3.创建相关配置文件
相关的配置文件有`spring`的，`springmvc`的和`log4j.properties`

**spring:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

</beans>
```
**springmvc:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context https://www.springframework.org/schema/context/spring-context.xsd">

    <!-- 配置扫描路径-->
    <context:component-scan base-package="com.bruce.controller" use-default-filters="false">
        <context:include-filter type="annotation" expression="org.springframework.stereotype.Controller" />
    </context:component-scan>
    
     <mvc:default-servlet-handler/>

    <mvc:annotation-driven/>

</beans>
```

**log4j.properties**
```properties
# Set root category priority to INFO and its only appender to CONSOLE.
#log4j.rootCategory=INFO, CONSOLE            debug   info   warn error fatal
log4j.rootCategory=debug, CONSOLE, LOGFILE
# Set the enterprise logger category to FATAL and its only appender to CONSOLE.
log4j.logger.org.apache.axis.enterprise=FATAL, CONSOLE
# CONSOLE is set to be a ConsoleAppender using a PatternLayout.
log4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender
log4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout
log4j.appender.CONSOLE.layout.ConversionPattern=%d{ISO8601} %-6r [%15.15t] %-5p %30.30c %x - %m\n
# LOGFILE is set to be a File appender using a PatternLayout.
log4j.appender.LOGFILE=org.apache.log4j.FileAppender
log4j.appender.LOGFILE.File=../logs/wlanapi/client.log
log4j.appender.LOGFILE.Append=true
log4j.appender.LOGFILE.layout=org.apache.log4j.PatternLayout
log4j.appender.LOGFILE.layout.ConversionPattern=%d{ISO8601} %-6r [%15.15t] %-5p %30.30c %x - %m\n
```
#### 3.3.4.web.xml设置

在`web.xml`文件中配置`spring`和`SpringMVC`容器的加载
```xml
<!DOCTYPE web-app PUBLIC
        "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
        "http://java.sun.com/dtd/web-app_2_3.dtd" >

<web-app>
    <display-name>Archetype Created Web Application</display-name>

    <!-- 初始化spring容器 -->
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:applicationContext.xml</param-value>
    </context-param>
    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>

    <!-- post乱码过滤器 -->
    <filter>
        <filter-name>CharacterEncodingFilter</filter-name>
        <filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
        <init-param>
            <param-name>encoding</param-name>
            <param-value>utf-8</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>CharacterEncodingFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
    <!-- 前端控制器 -->
    <servlet>
        <servlet-name>dispatcherServletb</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <!-- contextConfigLocation不是必须的， 如果不配置contextConfigLocation， springmvc的配置文件默认在：WEB-INF/servlet的name+"-servlet.xml" -->
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>classpath:springmvc.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>dispatcherServletb</servlet-name>
        <!-- 拦截所有请求jsp除外 -->
        <url-pattern>/</url-pattern>
    </servlet-mapping>

</web-app>

```
### 3.4. SpringSecurity整合
准备好`Spring+SpringMVC`的环境后我们就可以整合`SpringSecurity`了

#### 3.4.1.相关jar作用介绍
`spring-security-core.jar`:核心包，任何`SpringSecurity`功能都需要此包
`spring-security-web.jar`:web工程必须，包含过滤器和相关的web安全基础结构代码
`spring-security-config.jar`:用于解析xml配置文件，用到`SpringSecurity`的xml配置文件的就要用到此包
`spring-security-taglibs.jar`:`SpringSecurity`提供的动态标签库，jsp页面可以用

因为maven项目的依赖传递性，我们在项目中只需要设置 `config` 和 `taglibs` 这两个依赖即可

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-config</artifactId>
    <version>5.1.5.RELEASE</version>
</dependency>

<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-taglibs</artifactId>
    <version>5.1.5.RELEASE</version>
</dependency>
```
#### 3.4.2.过滤器配置

我们需要在容器启动的时候加载相关的过滤器，所以需要在`web.xml`中添加如下配置
```xml
<!-- 配置过滤器链 springSecurityFilterChain名称固定-->
<filter>
  <filter-name>springSecurityFilterChain</filter-name>
  <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
</filter>
<filter-mapping>
  <filter-name>springSecurityFilterChain</filter-name>
  <url-pattern>/*</url-pattern>
</filter-mapping>
```
#### 3.4.3.SpringSecurity配置文件spring-security.xml

单独添加一个`SpringSecurity`的配置文件
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:p="http://www.springframework.org/schema/p"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xmlns: ="http://www.springframework.org/schema/security"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans-4.2.xsd
    http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-4.2.xsd
    http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop-4.2.xsd
    http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx-4.2.xsd
    http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util-4.2.xsd
    http://www.springframework.org/schema/security http://www.springframework.org/schema/security/spring-security-4.2.xsd">

    <!--
        设置可以用spring的el表达式配置Spring Security并自动生成对应配置组件（过滤器）
        auto-config="true" 表示自动加载SpringSecurity的配置文件
        use-expressions="true" 使用Spring的EL表达式
     -->
    <security:http auto-config="true" use-expressions="true">
        <!--
            拦截资源
            pattern="/**" 拦截所有的资源
            access="hasAnyRole('role1')" 表示只有role1这个角色可以访问资源
         -->
         <!--使用spring的el表达式来指定项目所有资源访问都必须有ROLE_USER或ROLE_ADMIN角色-->
        <security:intercept-url pattern="/**" access="hasAnyRole('ROLE_USER','ROLE_ADMIN')"></security:intercept-url>
    </security:http>

    <!-- 设置置Spring Security认证用户来源  noop：SpringSecurity中默认 密码验证是要加密的  noop表示不加密 -->
    <security:authentication-manager>
        <security:authentication-provider>
            <security:user-service>
                <security:user name="zhang" password="{noop}123" authorities="ROLE_USER"></security:user>
                <security:user name="lisi" password="{noop}123" authorities="ROLE_ADMIN"></security:user>
            </security:user-service>
        </security:authentication-provider>
    </security:authentication-manager>

</beans>

```
#### 3.4.4.导入SpringSecurity的配置文件
将spring-security.xml配置文件引入到applicationContext.xml中

SpringSecurity的配置文件需要加载到Spring容器中，所以可以通过import来导入

```xml
<import resource="classpath:spring-security.xml"></import>
```
#### 3.4.4.启动项目测试
启动访问：
![在这里插入图片描述](https://img-blog.csdnimg.cn/9012f085f9a54ad59f78a3b17b7ad996.png)
唉！？说好的首页呢！？为何生活不是我想象！？
地址栏中login处理器谁写的！？这个带有歪果仁文字的页面哪来的！？这么丑！？我可以换了它吗！？

稍安勿躁……咱们先看看这个页面源代码，真正惊心动魄的还在后面呢……
这不就是一个普通的`form`表单吗？除了那个`_csrf`的input隐藏文件！
注意！这可是你想使用自定义页面时，排查问题的一条重要线索！
![在这里插入图片描述](https://img-blog.csdnimg.cn/aa9503a69caf4e27b458ee5d30f58c5a.png)
我们再去看看控制台发生了什么，这里我偷偷在项目中加了日志包和配置文件……惊不惊喜！？意不意外！？哪来这么多过滤器啊！？

```c
2021-08-18 13:01:20,828 1054   [on(3)-127.0.0.1] INFO  web.DefaultSecurityFilterChain  - Creating filter chain: any request, [org.springframework.security.web.context.SecurityContextPersistenceFilter@34429149, org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter@395d058e, org.springframework.security.web.header.HeaderWriterFilter@7c00e3de, org.springframework.security.web.csrf.CsrfFilter@6c2b0fc2, org.springframework.security.web.authentication.logout.LogoutFilter@4cc303d7, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter@1d631eb4, org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter@668610d5, org.springframework.security.web.authentication.ui.DefaultLogoutPageGeneratingFilter@526746d8, org.springframework.security.web.authentication.www.BasicAuthenticationFilter@66e2af8, org.springframework.security.web.savedrequest.RequestCacheAwareFilter@3fd123f, org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter@552bba2d, org.springframework.security.web.authentication.AnonymousAuthenticationFilter@3d1267d6, org.springframework.security.web.session.SessionManagementFilter@4adcb366, org.springframework.security.web.access.ExceptionTranslationFilter@21db2ce8, org.springframework.security.web.access.intercept.FilterSecurityInterceptor@2929102c]
```
最后，我们在这个登录页面上输入用户名zhang，密码123，点击Sign in，好了，总算再次看到首页了！
![在这里插入图片描述](https://img-blog.csdnimg.cn/022c1e66936c4684adab1c1194d40181.png)
一个Spring Security简单入门，已经是凝云重重，举步维艰了，咱们下回再分析吧！

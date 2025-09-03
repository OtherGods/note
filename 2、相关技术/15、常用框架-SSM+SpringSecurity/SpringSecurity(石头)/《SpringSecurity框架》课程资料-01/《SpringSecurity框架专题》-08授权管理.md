

[TOC]

权限管理的两大核心是：认证和授权，前面我们已经介绍完了认证的内容，本文就给大家来介绍下SpringSecurity的授权管理。
## 1.注解操作
我们在控制器或者service中实现授权操作比较理想的方式就是通过相应的注解来实现。SpringSecurity可以通过注解的方式来控制类或者方法的访问权限。注解需要对应的注解支持，若注解放在controller类中，对应注解支持应该放在mvc配置文件中，因为controller类是有mvc配置文件扫描并创建的，同理，注解放在service类中，对应注解支持应该放在spring配置文件中。由于我们现在是模拟业务操作，并没有service业务代码，所以就把注解放在controller类中了。
### 1.1.开启授权的注解支持
这里给大家演示三类注解，但实际开发中，用一类即可！

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:p="http://www.springframework.org/schema/p"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xmlns:security="http://www.springframework.org/schema/security"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:mvc="http://www.springframework.org/schema/mvc"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans-4.2.xsd
    http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-4.2.xsd
    http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop-4.2.xsd
    http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx-4.2.xsd
    http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util-4.2.xsd
    http://www.springframework.org/schema/security http://www.springframework.org/schema/security/spring-security-4.2.xsd http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd">

    <!-- 配置扫描路径-->
    <context:component-scan base-package="com.bruce.controller"></context:component-scan>

    <mvc:default-servlet-handler/>

    <mvc:annotation-driven/>

    <!--
       开启权限控制注解支持
           jsr250-annotations="enabled"表示支持jsr250-api的注解，需要jsr250-api的jar包
           pre-post-annotations="enabled"表示支持spring表达式注解
           secured-annotations="enabled"这才是SpringSecurity提供的注解
   -->
    <security:global-method-security jsr250-annotations="enabled"
                                     pre-post-annotations="enabled"
                                     secured-annotations="enabled"/>

</beans>
```

### 1.2.在注解支持对应类或者方法上添加注解

Jsr250注解的使用

```xml
<dependency>
  <groupId>javax.annotation</groupId>
  <artifactId>jsr250-api</artifactId>
  <version>1.0</version>
</dependency>
```
创建相关的控制器

```java
package com.bruce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.security.RolesAllowed;

@Controller
@RolesAllowed(value = {"ROLE_ADMIN"})
@RequestMapping("users")
public class UserController {

    @RequestMapping("/find")
    public String query(){
        System.out.println("用户查询..");
        return "/home.jsp";
    }

    @RequestMapping("/update")
    public String update(){
        System.out.println("用户更新...");
        return "/update.jsp";
    }
}
```
访问：
![在这里插入图片描述](https://img-blog.csdnimg.cn/c43404ee53b84879b8ba5094812aae9a.png)
**Spring表达式注解使用**
![在这里插入图片描述](https://img-blog.csdnimg.cn/bd88347e0e724b6abc1f1160844969d3.png)
效果
![在这里插入图片描述](https://img-blog.csdnimg.cn/b8f9112db2db46cabd45ffd46bd46769.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/e9e7b789dc594e04a51933268ac903f6.png)
**SpringSecurity提供的注解使用**
![在这里插入图片描述](https://img-blog.csdnimg.cn/ee3542b358354b0a9367669382026c6b.png)
效果
![在这里插入图片描述](https://img-blog.csdnimg.cn/906ca4193d744721b2c85eb912f76758.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/622d23524af447e58361e0b1a6b676ad.png)

### 1.3.权限异常处理

对于没有访问权限的操作，我们直接给一个403的系统错误页面，用户体验也太差了，这时我们可以通过自定义异常处理来解决

**自定义错误页面**
![在这里插入图片描述](https://img-blog.csdnimg.cn/f5f4a188d6aa4b4cada052635ea43c51.png)
**方式一：**在spring-security.xml配置文件中处理
![在这里插入图片描述](https://img-blog.csdnimg.cn/3793e97f61f84fc0af508c82fa197e63.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/4d83bc426a984fa7af96264a7cee98c8.png)
**方式二**：web.xml文件中设置

```xml
<error-page> 
  <error-code>403</error-code> 
  <location>/403.jsp</location> 
</error-page>
```
## 2.标签操作
上面介绍的注解方式可以控制服务器的访问，但是我们在前端页面上也需要把用户没有权限访问的信息给隐藏起来，这时我们需要通过`SpringSecurity`的标签库来实现，具体如下

```html
<%--
  Created by IntelliJ IDEA.
  User: dengp
  Date: 2019/12/1
  Time: 20:43
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="security"%>
<html>
<head>
    <title>Title</title>

</head>
<body>
    <h1>home界面</h1><br>
    当前登录账号:<br>
    <security:authentication property="principal.username"/><br>
    <security:authentication property="name"/><br>
    <form action="/logout" method="post">
        <security:csrfInput/>
        <input type="submit"value="注销">
    </form>

    <security:authorize access="hasAnyRole('ROLE_ADMIN')" >
        <a href="#">系统管理</a>
    </security:authorize>
    
    <security:authorize access="hasAnyRole('ROLE_USER')" >
        <a href="#">用户管理</a>
    </security:authorize>
    
</body>
</html>

```
用起来和我们前面介绍`shiro`的标签库差不多
![在这里插入图片描述](https://img-blog.csdnimg.cn/daf0f46b4a9c4d9280d6229ceea214b5.png)
大家要注意，标签管理仅仅是隐藏了页面，但并没有做权限管理，所以后台权限管理是必须的！


[TOC]

前面通过入门案例介绍，我们发现在`SpringSecurity`中如果我们没有使用自定义的登录界面，那么`SpringSecurity`会给我们提供一个系统登录界面。但真实项目中我们一般都会使用自定义的登录界面，本文我们就来介绍下如何实现该操作。

注意:本文是在入门案例代码的基础上演示的！

## 1.页面准备
我们准备如下相关的jsp页面
### 1.1.login.jsp页面

```html

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Title</title>
</head>
<body>
<h1>登录管理</h1>

<form>
    账号:<input type="text" name="username"><br>
    密码:<input type="password" name="password"><br>
    <input type="submit" value="登录"><br>
</form>
<img src="img/a1.jpg">
</body>
</html>

```
### 1.2.home.jsp页面

```html
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Title</title>
</head>
<body>
<h1>home界面</h1>
</body>
</html>

```
### 1.3.其他页面
![在这里插入图片描述](https://img-blog.csdnimg.cn/824102c1738c40a5bd8190450cf577da.png)
## 2.SpringSecurity相关配置
### 2.1.配置认证信息
配置登录和注销相关的信息

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:p="http://www.springframework.org/schema/p"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xmlns:security="http://www.springframework.org/schema/security"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans-4.2.xsd
    http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-4.2.xsd
    http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop-4.2.xsd
    http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx-4.2.xsd
    http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util-4.2.xsd
    http://www.springframework.org/schema/security http://www.springframework.org/schema/security/spring-security-4.2.xsd">


    <!--直接释放无需经过SpringSecurity过滤器的静态资源-->
    <security:http pattern="/css/**" security="none"/>
    <security:http pattern="/img/**" security="none"/>
    <security:http pattern="/plugins/**" security="none"/>
    <security:http pattern="/failer.jsp" security="none"/>
    <security:http pattern="/favicon.ico" security="none"/>

    <!--设置可以用spring的el表达式配置Spring Security并自动生成对应配置组件（过滤器）-->
    <security:http auto-config="true" use-expressions="true">
        
        <!--指定login.jsp页面可以被匿名访问-->
        <security:intercept-url pattern="/login.jsp" access="permitAll()"/>
        
        <!--使用spring的el表达式来指定项目所有资源访问都必须有ROLE_USER或ROLE_ADMIN角色-->
        <security:intercept-url pattern="/**" access="hasAnyRole('ROLE_USER','ROLE_ADMIN')"/>
        
        <!--指定自定义的认证页面-->
    <security:form-login login-page="/login.jsp"
                             login-processing-url="/login"
                             authentication-success-forward-url="/home.jsp"
                             authentication-failure-forward-url="/failer.jsp"/>
        
        <!--指定退出登录后跳转的页面-->
        <security:logout logout-url="/logout"
                         logout-success-url="/login.jsp"/>
    </security:http>
    
    <!--设置Spring Security认证用户信息的来源-->
    <security:authentication-manager>
        <security:authentication-provider>
            <security:user-service>
                <security:user name="user" password="{noop}user" authorities="ROLE_USER"/>
                <security:user name="admin" password="{noop}admin" authorities="ROLE_ADMIN"/>
            </security:user-service>
        </security:authentication-provider>
    </security:authentication-manager>

</beans>

```
## 3.登录测试
再次启动项目后就可以看到自定义的酷炫认证页面了！
![在这里插入图片描述](https://img-blog.csdnimg.cn/5aa77d661be9411b94399c2ba54cf47f.png)
可以访问到，然后提交登录看看
注意表单设置
![在这里插入图片描述](https://img-blog.csdnimg.cn/b1bc339df1694b78b0cb865a6bfb1ec3.png)
然后访问出现了403错误
然后你开开心心的输入了用户名user，密码user，就出现了如下的界面：
![在这里插入图片描述](https://img-blog.csdnimg.cn/31eced22be3041bcbd0d01d4f0e79847.png)
403什么异常？这是`SpringSecurity`中的权限不足！这个异常怎么来的？还记得上面`SpringSecurity`内置认证页面源码中的那个`_csrf`隐藏`input`吗？问题就在这了！

## 4.关闭csrf拦截
上面我们在账号和角色都正确的情况下，登录后出现了 `403`错误，原因是因为 `csrf`过滤器拦截了，那为什么系统提供的登录界面没问题呢？原因是如下
![在这里插入图片描述](https://img-blog.csdnimg.cn/7e6fbcbcb8674c3da061b36327c8c0f9.png)
在系统提供的登录表单中隐藏的有csrf相关的信息。这时我们可以关闭csrf过滤器，来实现登录工作
![在这里插入图片描述](https://img-blog.csdnimg.cn/9c20c2bdcd18402cab19ddf1993d48de.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/1633ffadf8ac4fab8122a826d5a8dd41.png)
## 5.csrf防护
上面我们通过关闭`csrf`过滤器实现了认证功能，但是系统将面临`csrf`攻击的风险，所以我们需要放开服务，同时也要能够完成认证。首先我们来看下`CsrfFilter`的源码

### 5.1.CsrfFilter源码查看

![在这里插入图片描述](https://img-blog.csdnimg.cn/d8cfaa6c7ab5468b81df3082e5281e50.png)
`this.requireCsrfProtectionMatcher.matches(request)`方法
![在这里插入图片描述](https://img-blog.csdnimg.cn/504349f7f1ae41f4809e1c3d07eac901.png)

![在这里插入图片描述](https://img-blog.csdnimg.cn/22f44cce80784c4d9443e5b37ac29632.png)
通过 GET HEAD TRACE OPTIONS 提交的数据不会csrf 验证!

通过源码分析，我们明白了，自己的认证页面，请求方式为POST，但却没有携带token，所以才出现了403权限不足的异常。那么如何处理这个问题呢？

```
方式一：直接禁用csrf，不推荐。
方式二：在认证页面携带token请求。
```

### 5.2.在认证页面携带token请求
- 前面关闭的我们需要放开
![在这里插入图片描述](https://img-blog.csdnimg.cn/fcbede06295247799f1a60f99d496993.png)
- 页面动态token
导入security标签
![在这里插入图片描述](https://img-blog.csdnimg.cn/1206ecd9694b405680eacf426dc6663f.png)

`<security:csrfInput/>`在表单中使用，作用和下面的一致
![在这里插入图片描述](https://img-blog.csdnimg.cn/10e93b1dd75743e9bda5562a27e8e2db.png)
`<security:csrfMetaTags/>`：ajax方式提交的时候使用

>注：HttpSessionCsrfTokenRepository对象负责生成token并放入session域中。

## 6.注销
在index.jsp中添加注销链接
![在这里插入图片描述](https://img-blog.csdnimg.cn/33e5638ca1da4be092a953fe5b2e974f.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/d7e4f12fcee440dea5fc8b43430be458.png)
点击后出现了404错误原因是：自定义的注销功能必须通过post方式提交才行，所以如下

![在这里插入图片描述](https://img-blog.csdnimg.cn/276c52e4aaf44381903abe2ecc6ed07f.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/ffa1653941804e3bbb1ccebc1f0e078a.png)
出现这个原因是 csrf的原因，加标签即可
![在这里插入图片描述](https://img-blog.csdnimg.cn/dabd0221a36d47b282c6d16163248fc3.png)
搞定~

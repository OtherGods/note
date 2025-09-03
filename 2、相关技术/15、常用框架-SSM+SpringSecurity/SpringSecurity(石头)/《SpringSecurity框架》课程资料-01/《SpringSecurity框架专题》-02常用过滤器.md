## 1.常见的过滤器

过滤器是一种典型的AOP思想，关于什么是过滤器，就不赘述了，谁还不知道凡是web工程都能用过滤器？

接下来咱们就一起看看Spring Security中这些过滤器都是干啥用的，源码我就不贴出来了，有名字，大家可以自己在idea中Double Shift去。也会在后续的学习过程中穿插详细解释。![在这里插入图片描述](https://img-blog.csdnimg.cn/6a3b6638e2d44fbda13c928ad1a56519.png)
Spring Security核心就是一组过滤器链，每个过滤器实现一个独立功能，用户请求通过滤器链进行登录、授权、权限校验等操作，最终执行目标服务。
### 1.1.SecurityContextPersistenceFilter

```c
org.springframework.security.web.context.SecurityContextPersistenceFilter
```

首当其冲的一个过滤器，非常重要主要是使用SecurityContextRepository在session中保存或更新一个SecurityContext，并将SecurityContext给以后的过滤器使用，来为后续filter建立所需的上下文，SecurityContext中存储了当前用户的认证和权限信息。
### 1.2.WebAsyncManagerIntegrationFilter

```c
org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter
```
此过滤器用于继承SecurityContext到Spring异步执行机制中的WebAsyncManager,和spring整合必须的.

### 1.3.HeaderWriterFilter

```c
org.springframework.security.web.header.HeaderWriterFilter
```

向请求的header中添加响应的信息，可以在http标签内部使用security:headers来控制
### 1.4.CsrfFilter

```c
org.springframework.security.web.csrf.CsrfFilter
```
csrf又称跨域请求伪造，SpringSecurity会对所有post请求验证是否包含系统生成的csrf的token信息，如果不包含，则报错。起到防止csrf攻击的效果。
### 1.5.LogoutFilter

```java
org.springframework.security.web.authentication.logout.LogoutFilter
```
匹配URL为/logout的请求，实现用户退出,清除认证信息。

### 1.6.UsernamePasswordAuthenticationFilter

```c
org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
```
认证操作全靠这个过滤器，默认匹配URL为/login且必须为POST请求。
### 1.6.DefaultLoginPageGeneratingFilter

```c
org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter
```
如果没有在配置文件中指定认证页面，则由该过滤器生成一个默认认证页面。

### 1.8.DefaultLogoutPageGeneratingFilter

```c
org.springframework.security.web.authentication.ui.DefaultLogoutPageGeneratingFilter
```
由此过滤器可以生产一个默认的退出登录页面
### 1.9.BasicAuthenticationFilter

```c
org.springframework.security.web.authentication.www.BasicAuthenticationFilter
```
此过滤器会自动解析HTTP请求中头部名字为Authentication，且以Basic开头的头信息。

### 1.10.RequestCacheAwareFilter

```c
org.springframework.security.web.savedrequest.RequestCacheAwareFilter
```
org.springframework.security.web.savedrequest.RequestCacheAwareFilter
### 1.11.SecurityContextHolderAwareRequestFilter

```c
org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter
```

针对ServletRequest进行了一次包装，使得request具有更加丰富的API

### 1.12.AnonymousAuthenticationFilter

```c
org.springframework.security.web.authentication.AnonymousAuthenticationFilter
```

当SecurityContextHolder中认证信息为空,则会创建一个匿名用户存入到SecurityContextHolder中。

spring security为了兼容未登录的访问，也走了一套认证流程，只不过是一个匿名的身份。
### 1.13.SessionManagementFilter

```c
org.springframework.security.web.session.SessionManagementFilter
```

SecurityContextRepository限制同一用户开启多个会话的数量

### 1.14.ExceptionTranslationFilter

```c
org.springframework.security.web.access.ExceptionTranslationFilter
```

异常转换过滤器位于整个springSecurityFilterChain的后方，用来转换整个链路中出现的异常

### 1.15.FilterSecurityInterceptor

```c
org.springframework.security.web.access.intercept.FilterSecurityInterceptor
```
获取所配置资源访问的授权信息，根据SecurityContextHolder中存储的用户信息来决定其是否有权
限。


好了！这一堆排山倒海的过滤器介绍完了。
那么，是不是spring security一共就这么多过滤器呢？答案是否定的！随着spring-security.xml配置的添加，还会出现新的过滤器。
那么，是不是spring security每次都会加载这些过滤器呢？答案也是否定的！随着spring-security.xml配置的修改，有些过滤器可能会被去掉。

## 2. spring security过滤器链加载原理
通过前面十五个过滤器功能的介绍，对于SpringSecurity简单入门中的疑惑是不是在心中已经有了答案了呀？

但新的问题来了！我们并没有在web.xml中配置这些过滤器啊？它们都是怎么被加载出来的？
友情提示：前方高能预警，吃饭喝水打瞌睡的请睁大眼睛，专注心神！
### 2.1.DelegatingFilterProxy
我们在web.xml中配置了一个名称为的过滤器`DelegatingFilterProxy`，接下我直接对`DelegatingFilterProxy`源码里重要代码进行说明，其中删减掉了一些不重要的代码，大家注意我写的注释就行了！
![在这里插入图片描述](https://img-blog.csdnimg.cn/6fb52b6c1158488c9797620670720592.png)
下面为`springSecurityFilterChain`过滤器：
![在这里插入图片描述](https://img-blog.csdnimg.cn/7653bd3f04894ea0beb559ef2724ce36.png)

`springSecurityFilterChain`过滤器继承：`GenericFilterBean`
![在这里插入图片描述](https://img-blog.csdnimg.cn/4970e592a8c94d16a7fba457bc89294e.png)

`GenericFilterBean`过滤器中的`init`方法进行初始化：
![在这里插入图片描述](https://img-blog.csdnimg.cn/7b167636bfb74bc28ddf607bb6e4573f.png)

`DelegatingFilterProxy`中的`initFilterBean`方法
![在这里插入图片描述](https://img-blog.csdnimg.cn/963a276d854e4802b30e86b7b6e10824.png)

执行`initDelegate`方法
![在这里插入图片描述](https://img-blog.csdnimg.cn/f5d8df0ce7c64176885fe15ad954eec6.png)

我们通过debug启动查看该方法的运行
![在这里插入图片描述](https://img-blog.csdnimg.cn/93325fca1c8b4519af878170b43921ce.png)
由此可知，`DelegatingFilterProxy`通过`springSecurityFilterChain`这个名称，得到了一个`FilterChainProxy`过滤器。

### 2.2.FilterChainProxy
通过上面的源码分析我们发现其实创建的是`FilterChainProxy`这个过滤器，那我们来看下这个过滤器。
![在这里插入图片描述](https://img-blog.csdnimg.cn/b3cd00a631814e41bb4afb68baa8886e.png)
当然我们需要首先来看下`doFilter`方法
![在这里插入图片描述](https://img-blog.csdnimg.cn/dec06f3d21f843b78b13baa62c694f8d.png)
进入`doFilterInternal`方法，然后debug查看
![在这里插入图片描述](https://img-blog.csdnimg.cn/2f3a40f1cd774d2f9b99fd06026193ee.png)
我们看到这15个过滤器被保存到了List容器中了。
对应的 this.getFilters方法如下:
![在这里插入图片描述](https://img-blog.csdnimg.cn/8d4c8154a01247f692749400c8b4ce72.png)
### 2.3.SecurityFilterChain
最后看`SecurityFilterChain`，这是个接口，实现类也只有一个，这才是web.xml中配置的过滤器链对象！
![在这里插入图片描述](https://img-blog.csdnimg.cn/440e25f771b749c99aee09e30e27a341.png)

### 2.4.DefaultSecurityFilterChain

具体的实现类
![在这里插入图片描述](https://img-blog.csdnimg.cn/e0a11c0d65b845fbb6af9917f660db65.png)
>总结：通过上面的代码分析，SpringSecurity中要使用到的过滤器最终都保存在了DefaultSecurityFilterChain对象的List filter对象中。

通过此章节，我们对SpringSecurity工作原理有了一定的认识。但理论千万条，功能第一条，探寻底层，是为了更好的使用框架。

那么，言归正传！到底如何使用自己的页面来实现`SpringSecurity`的认证操作呢？要完成此功能，首先要有一套自己的页面！

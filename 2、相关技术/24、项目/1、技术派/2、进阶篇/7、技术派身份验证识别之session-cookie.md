技术派当前的用户登录信息，主要是借助最基础的session/cookie来实现的；虽然我们现在基本上已经进入了分布式session的时代了，但是在切实去看最新的oauth, sso, jwt等各种登录方案之前，我们有必要学习一下最早的cookie/session方案，先去了解一下它的实现方式，知道其劣势，然后再此基础上，看一下技术派是怎么进行优化改造的

关于jwt、oauth等会放在下一篇教程 [8、技术派身份验证之JWT](2、相关技术/24、项目/1、技术派/2、进阶篇/8、技术派身份验证之JWT.md) 中给大家进行介绍

# 1、Session/Cookie基本使用姿势

首先看一下如何在SpringBoot项目中使用session/cookie

## 1.1、登录入口，保存session
首先我们设计一个登录的接口，用来模拟真实场景下的登录，注意下面的实现
```java
@RestController
public class SessionController {

    @RequestMapping(path = "/login")
    public String login(String uname, HttpSession httpSession) {
        httpSession.setAttribute("name", uname);
        return "欢迎登录：" + uname;
    }
}
```

在上面的实现中，方法中定义了一个HttpSession的参数类型，具体的实现中，就是表示写入sesion的操作

当session写入完毕之后，在这个会话结束之前，后续的所有请求都可以直接获取到对应的session

## 1.2、session读取测试

下面给出两种常见的session获取方式：
1. 直接从HttpSession中获取
2. 通过HttpServlet来获取
```java
@RequestMapping("time")
public String showTime(HttpSession session) {
	return session.getAttribute("name") + " ，当前时间为：" + LocalDateTime.now();
}

@RequestMapping("name")
public String showName(HttpServletRequest request) {
	return "当前登录用户：" + request.getSession().getAttribute("name");
}
```

接下来我们来模拟验证一下：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307301114516.gif)

从上面的演示图中可以看出，在登陆后，访问上面的接口，可以直接拿到session中存在的用户名。

且不同用户登录（不同的浏览器），它们的session不会出现串掉的情况。

## 1.3、退出登录

登出：
```java
@RequestMapping(path = "logout")
public String logout(HttpSession httpSession) {
	// 注销当前的session
	httpSession.invalidate();
	return "登出成功";
}
```

## 1.4、session实现原理
SpringBoot提供了一套非常简单的session机制，那么它又是怎么工作的呢？ 特别是它是怎么识别用户身份的呢？ session又是存在什么地方的呢？

session：再浏览器窗口打开期间，这个会话一直有效，即先访问login，然后再访问time，可以直接拿到name， 若再此过程中，再次访问了login更新了name，那么访问time获取到的也是新的name

当关闭浏览器后，重新访问time接口，则此时拿不到name

核心工作原理：
1. 借助cookie中的JESSIONID来作为用户身份表示，这个数据相同的，认为是同一个用户；然后会将sessin在内存中存一份，有过期时间的限制，通常每访问一次，过期时间重新刷新
2. 当浏览器不支持cookie时，借助url重写，将sessionId写入url地址中，参数名=jsessionid
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307301131199.png)

从上面的描述中，就可以看到几个关键点：
1. session主要是存在内存中，根据用户请求的cookie来识别用户身份，且有一个过期时间（那么问题来了，内存有大小限制吗？会出现oom吗？）
2. 对于用户而言，每次关闭浏览器再重新打开，会重新生成JESSIONID的cookies值，由于这个值的更改，导致后端无法记录之前访问的是谁



## 1.5、demo源码
- 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 项目：[https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/224-web-session](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/224-web-session)  
    已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)


# 2、技术派的身份认证
在技术派的实现中，其实和上面的思路没有什么差别，只不过是将读写cookie，使用session的具体逻辑展示出来了而已；

当前的技术派，针对前台和管理员的后台登录，设置了两套不同的玩法；前者是基于微信公众号，后者则是传统的用户名+密码；虽然登录方式不同，但底层的原理一致；

接下来再进行讲解session/cookie的具体实例时，以管理员的后台为例进行说明，整个流程如下图所示
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307301138796.png)

## 2.1、登陆与登出

相关代码在 AdminLoginController 中
```java
@RestController
@RequestMapping(path = {"/api/admin/login", "/admin/login"})
public class AdminLoginController {
	@Autowired
	private UserService userService;

	@Autowired
    private SessionService sessionService;

	@PostMapping(path = {"", "/"})
    public ResVo<BaseUserInfoDTO> login(HttpServletRequest request, HttpServletResponse response) {
	    String user = request.getParameter("username");
        String pwd = request.getParameter("password");
        BaseUserInfoDTO info = userService.passwordLogin(user, pwd);
        String session = sessionService.login(info.getUserId());

		if (StringUtils.isNotBlank(session)) {
            // cookie中写入用户登录信息
            response.addCookie(new Cookie(SessionService.SESSION_KEY, session));
            return ResVo.ok(info);
        } else {
	        return ResVo.fail(StatusEnum.LOGIN_FAILED_MIXED, "登录失败，请重试");
		}
    }

	@Permission(role = UserRole.LOGIN)
    @RequestMapping("logout")
    public ResVo<Boolean> logOut(HttpServletResponse response) throws IOException {
	    Optional.ofNullable(ReqInfoContext.getReqInfo()).ifPresent(s -> sessionService.logout(s.getSession()));
	    // 重定向到首页
        response.sendRedirect("/");
        return ResVo.ok(true);
    }
}
```

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307301750944.png)
重点关注上面的两步：
1. 根据用户，生成一个对应唯一session值，保存到redis缓存
2. 将session写道cookie中，返回给前端

后续用户再进行访问时，每次都需要携带这个cookie，然后后台通过这个session来识别用户身份（所以如果这个cookie被别人挟持了，那么就危险了）

## 2.2、用户身份识别

接下来我们再看一下用户身份识别，核心逻辑在ReqRecordFilter
（额外说一句，这个Filter的事情太多了，更推荐的做法是再起一个专门的用户身份识别的Filter，上面这个应该只管请求日志记录）

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307301758942.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307301802911.png)

## 2.3、小结

当前技术派中的用户身份识别这套方案，与传统的cookie/session其实没有什么本质的区别，核心思路就是：
1. 用户登录，生成一个唯一表示
	1. 服务端保存这个标识与用户的映射
	2. 服务端将这个标识，写cookie返回给客户端
2. 客户端后续的请求，携带cookie

基本流程如上，接下来重点看一下相关知识
1. 如何读写cookie
2. 如何保存用户信息（如session，如将用户标识userId保存缓存）
3. 在filter识别完用户身份
4. 识别的用户身份信息，如何提供整个访问链路使用





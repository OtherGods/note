来自阮一峰的网络日志：[Cookie 的 SameSite 属性](https://www.ruanyifeng.com/blog/2019/09/cookie-samesite.html)

关于跨域：
- [我知道的跨域与安全](https://juejin.cn/post/6844903553069219853)
- [补5-前端安全系列之二：如何防止CSRF攻击？](2、相关技术/13、J2EE基础-Servlet/JavaWeb(石头)/JavaWeb开发实战教程(JSP+Servlet+Mvc)/《第五章%20会话技术Cookie》课程资料/讲义/补5-前端安全系列之二：如何防止CSRF攻击？.md)

## 一、CSRF 攻击是什么？

又称之为跨站请求攻击。

Cookie 往往用来存储用户的身份信息，恶意网站可以设法伪造带有正确 Cookie 的 HTTP 请求，这就是 CSRF 攻击。

举例来说，用户登陆了银行网站`your-bank.com`，银行服务器发来了一个 Cookie。

```cookie
Set-Cookie:id=a3fWa;
```

用户后来又访问了恶意网站`malicious.com`，上面有一个表单。

```html
<form action="your-bank.com/transfer" method="POST">
  ...
</form>
```

用户一旦被诱骗发送这个表单，银行网站就会收到带有正确 Cookie 的请求。为了防止这种攻击，表单一般都带有一个随机 token，告诉服务器这是真实请求。
```html
<form action="your-bank.com/transfer" method="POST">
  <input type="hidden" name="token" value="dad3weg34">
  ...
</form>
```

这种第三方网站引导发出的 Cookie，就称为第三方 Cookie。它除了用于 CSRF 攻击，还可以用于用户追踪。

比如，Facebook 在第三方网站插入一张看不见的图片。

```html
<img src="facebook.com" style="visibility:hidden;">
```

浏览器加载上面代码时，就会向 Facebook 发出带有 Cookie 的请求，从而 Facebook 就会知道你是谁，访问了什么网站。

## 二、CSRF补充

来自：[前端安全系列之二：如何防止CSRF攻击？](https://juejin.cn/post/6844903689702866952)中的部分内容，具体可以去看我的转载。

对于第一小节的概述+补充：

### 什么是CSRF

CSRF（Cross-site request forgery）跨站请求伪造：攻击者诱导受害者进入第三方网站，在第三方网站中，向被攻击网站发送跨站请求。利用受害者在被攻击网站已经获取的注册凭证，绕过后台的用户验证，达到冒充用户对被攻击的网站执行某项操作的目的。

一个典型的CSRF攻击有着如下的流程：

- 受害者登录`a.com`，并保留了登录凭证（Cookie）。
- 攻击者引诱受害者访问了`b.com`。
- `b.com` 向 `a.com` 发送了一个请求：`a.com/act=xx`。
- `a.com`接收到请求后，对请求进行验证，并确认是受害者的凭证，误以为是受害者自己发送的请求。
- `a.com`以受害者的名义执行了`act=xx`。
- 攻击完成，攻击者在受害者不知情的情况下，冒充受害者，让`a.com`执行了自己定义的操作。

### 几种常见的攻击类型

- GET类型的CSRF
  GET类型的CSRF利用非常简单，只需要一个HTTP请求，一般会这样利用：
```html
 <img src="http://bank.example/withdraw?amount=10000&for=hacker" > 
```
  在受害者访问含有这个img的页面后，浏览器会自动向`http://bank.example/withdraw?account=xiaoming&amount=10000&for=hacker`发出一次HTTP请求。bank.example就会收到包含受害者登录信息的一次跨域请求。

- POST类型的CSRF
  这种类型的CSRF利用起来通常使用的是一个自动提交的表单，如：
```html
 <form action="http://bank.example/withdraw" method=POST>
    <input type="hidden" name="account" value="xiaoming" />
    <input type="hidden" name="amount" value="10000" />
    <input type="hidden" name="for" value="hacker" />
</form>
<script> document.forms[0].submit(); </script> 
```
  访问该页面后，表单会自动提交，相当于模拟用户完成了一次POST操作。
  
  POST类型的攻击通常比GET要求更加严格一点，但仍并不复杂。任何个人网站、博客，被黑客上传页面的网站都有可能是发起攻击的来源，后端接口不能将安全寄托在仅允许POST上面。
- 链接类型的CSRF
  链接类型的CSRF并不常见，比起其他两种用户打开页面就中招的情况，这种需要用户点击链接才会触发。这种类型通常是在论坛中发布的图片中嵌入恶意链接，或者以广告的形式诱导用户中招，攻击者通常会以比较夸张的词语诱骗用户点击，例如：
```html
  <a href="http://test.com/csrf/withdraw.php?amount=1000&for=hacker" taget="_blank">
  重磅消息！！
  <a/>
```
  由于之前用户登录了信任的网站A，并且保存登录状态，只要用户主动访问上面的这个PHP页面，则表示攻击成功。

### CSRF的特点

- 攻击一般发起在第三方网站，而不是被攻击的网站。被攻击的网站无法防止攻击发生。
- 攻击利用受害者在被攻击网站的登录凭证，冒充受害者提交操作；而不是直接窃取数据。
- 整个过程攻击者并不能获取到受害者的登录凭证，仅仅是“冒用”。
- 跨站请求可以用各种方式：图片URL、超链接、CORS、Form提交等等。部分请求方式可以直接嵌入在第三方论坛、文章中，难以进行追踪。

CSRF通常是跨域的，因为外域通常更容易被攻击者掌控。但是如果本域下有容易被利用的功能，比如可以发图和链接的论坛和评论区，攻击可以直接在本域下进行，而且这种攻击更加危险。

### 防御策略

CSRF通常从第三方网站发起，被攻击的网站无法防止攻击发生，只能通过增强自己网站针对CSRF的防护能力来提升安全性。

上文中讲了CSRF的两个特点：
- CSRF（通常）发生在第三方域名。
- CSRF攻击者不能获取到Cookie等信息，只是使用。

针对这两点，我们可以专门制定防护策略，如下：
- 阻止不明外域的访问
  - 同源检测
  - Samesite Cookie
- 提交时要求附加本域才能获取的信息
  - CSRF Token	★ ★ ★   是文章《补4-~》中JWT内容的引子
  - 双重Cookie验证

## 三、SameSite 属性

Cookie 的`SameSite`属性用来限制第三方 Cookie，从而减少安全风险。

它可以设置三个值。
- Strict
- Lax
- None

### 3.1 Strict

`Strict`最为严格，完全禁止第三方 Cookie，跨站点时，任何情况下都不会发送 Cookie。换言之，只有当前网页的 URL 与请求目标一致，才会带上 Cookie。

```bash
Set-Cookie: CookieName=CookieValue; SameSite=Strict;
```

这个规则过于严格，可能造成非常不好的用户体验。比如，当前网页有一个 GitHub 链接，用户点击跳转就不会带有 GitHub 的 Cookie，跳转过去总是未登陆状态。

### 3.2 Lax

`Lax`规则稍稍放宽，大多数情况也是不发送第三方 Cookie，但是导航到目标网址的 Get 请求除外。

```bash
Set-Cookie: CookieName=CookieValue;
SameSite=Lax;
```

导航到目标网址的 GET 请求，只包括三种情况：链接，预加载请求，GET 表单。详见下表。

| 请求类型  |                 示例                 |    正常情况 | Lax         |
| :-------- | :----------------------------------: | ----------: | :---------- |
| 链接      |         `<a href="..."></a>`         | 发送 Cookie | 发送 Cookie |
| 预加载    | `<link rel="prerender" href="..."/>` | 发送 Cookie | 发送 Cookie |
| GET 表单  |  `<form method="GET" action="...">`  | 发送 Cookie | 发送 Cookie |
| POST 表单 | `<form method="POST" action="...">`  | 发送 Cookie | 不发送      |
| iframe    |    `<iframe src="..."></iframe>`     | 发送 Cookie | 不发送      |
| AJAX      |            `$.get("...")`            | 发送 Cookie | 不发送      |
| Image     |          `<img src="...">`           | 发送 Cookie | 不发送      |

设置了`Strict`或`Lax`以后，基本就杜绝了 CSRF 攻击。当然，前提是用户浏览器支持 SameSite 属性。

### 3.3 None

Chrome 计划将`Lax`变为默认设置。这时，网站可以选择显式关闭`SameSite`属性，将其设为`None`。不过，前提是必须同时设置`Secure`属性（Cookie 只能通过 HTTPS 协议发送），否则无效。

下面的设置无效。
```bash
Set-Cookie: widget_session=abc123;
SameSite=None
```

下面的设置有效。
```bash
Set-Cookie: widget_session=abc123;
SameSite=None; Secure
```

## 四、关于跨站

本文留言中的内容

> 针对于第三方cookie：
>
> 需要注意 跨域请求 和 跨站请求的区别。 跨域请求不一定是跨站请求，而跨站请求一定是跨域请求。<u>***第三方 cookie 是针对跨站请求的。***</u>

> 针对同站、跨站：
>
> “同站”只要两个 URL 的 eTLD+1 相同即可，不需要考虑协议和端口。其中，eTLD 表示有效顶级域名，注册于 Mozilla 维护的公共后缀列表（Public Suffix List）中，例如，.com、.co、.uk、.github.io 等。而 eTLD+1 则表示，有效顶级域名+二级域名，例如taobao.com等。举几个例子，www.taobao.com 和 www.baidu.com 是跨站，www.a.taobao.com 和 www.b.taobao.com 是同站，a.github.io 和 b.github.io 是跨站(注意是跨站)。

## 五、关于跨域的补充

*具体内容去看掘金上的： [我知道的跨域与安全](https://juejin.cn/post/6844903553069219853)，我没有看懂文章中后面的内容，所以跳过了。*

关于跨域，有**两个误区**：
1. ✕ 动态请求就会有跨域的问题
   ✔ 跨域只存在于浏览器端，不存在于安卓、ios、Node.js、python、java等其它环境
2. ✕ 跨域就是请求发不出去了
   ✔ 跨域请求能发出去，服务端能收到请求并正常返回结果，只是结果被浏览器拦截了
   <font color = "red">之所以会跨域，是因为受到了同源策略的限制</font>，**<font color = "blue">同源策略</font>要求源相同才能正常进行通信，即<u>协议、域名、端口号</u>都完全一致。**

如下图所示：
![image-20220928113044865](D:\Tyora\AssociatedPicturesInTheArticles\补1-CSRF+Cookie的SameSite属性\image-20220928113044865.png)

这三个源分别由于域名、协议和端口号不一致，导致会受到同源策略的限制。

…………

…………

…………












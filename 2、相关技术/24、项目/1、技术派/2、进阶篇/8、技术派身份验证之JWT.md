#JWT #Session

技术派之前使用的是session的方式来实现用户身份健全，接下来我们再看一下实际项目中，大概率会使用的jwt鉴权方式
# 1、JWT知识点

jwt，全程json web token，JSON Web令牌是一种开放的行业标准RFC 7519方法，用于在两方法之间安全的表示声明
> 详情参考：[https://jwt.io/introduction](https://jwt.io/introduction)

## 1.1、数据结构

JSON Web Token由三部分组成，它们之间以圆点 `.` 进行分割，一个标准的JWT形如 `xxx.yyy.zzz`
1. Header
2. Payload
3. Signature

### 1.1.1、header

即第一部分，由两部分组成：token类型（JWT）和算法名称（比如：HMAC SHA256或RSA等等）

一个具体实例如：
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

然后，用Base64对这个JSON编码就得到JWT的第一部分

### 1.1.2、Payload

第二部分具体的实体，可以写入自定义的数据信息，有三种类型：
1. Registered claims : 这里有一组预定义的声明，它们不是强制的，但是推荐。比如：iss (issuer 签发者), exp (expiration time 有效期), sub (subject), aud (audience)等。
2. Public claims : 可以随意定义。
3. Private claims : 用于在同意使用它们的各方之间共享信息，并且不是注册的或公开的声明

如一个具体实例
```json
{
    "iss": "一灰灰blog",
    "exp": 1692256049,
    "wechat": "https://spring.hhui.top/spring-blog/imgs/info/wx.jpg",
    "site": "https://spring.hhui.top",
    "uname": "一灰"
}
```

对payload进行Base64编码就得到JWT的第二部分

### 1.1.3、Signature

为了得到签名部分，你必须有编码过的header、编码过的payload、一个秘钥，签名算法是header中指定的那个，然对它们签名即可。

如 `HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)`

签名是用于验证消息在传递过程中有没有被更改，并且，对于使用私钥签名的token，它还可以验证JWT的发送方是否为它所称的发送方。
### 1.1.4、具体实例

下面给出一个基于 java-jwt 生成的具体实例
```java
public static void main(String[] args) {
    String token = JWT.create().withIssuer("一灰灰blog").withExpiresAt(new Date(System.currentTimeMillis() + 86400_000))
	    .withPayload(MapUtils.create("uname", "一灰", "wechat", "https://spring.hhui.top/spring-blog/imgs/info/wx.jpg", "site", "https://spring.hhui.top"))
		.sign(Algorithm.HMAC256("helloWorld"));
	System.out.println(token);
}
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312321057.png)



# 2、技术派的应用姿势
## 2.1、通用的jwt及安全方案

**JWT鉴权流程**

一个简单的基于jwt的身份验证方案如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312323655.png)

基本流程分为三步：
1. 用户登录成功之后，后端将生成的jwt返回给前端，然后前端将其保存在本地缓存
2. 之后前端与后端的交互时，都将jwt放在请求中，比如说可以将其放在Http的身份认证的请求头 `Authorization` ，也可以通过自定义的请求头来传递
3. 后端接收到用户的请求，从请求头中获取jwt，然后进行校验，通过之后，才响应相关的接口；否则表示未登路
> 说明：技术派沿用session的方案，依然将jwt写入到cookie中

## 2.2、jwt使用姿势

接下来再看一下技术派中的实际使用场景，核心逻辑在 `com.github.paicoding.forum.service.user.service.help.UserSessionHelper` 中

我们直接在之前session的基础上进行优化，这里主要接触开源项目java-jwt来实现JWT的生成管理。


### 2.2.1、**生成JWT的实现**

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312337697.png)

我们定义了签发者、有效期，指定了签名算法，然后实体类中，携带两个信息
- `s`：即之前生成的sessionId，我们借助自定义的traceId生成工具来生成唯一的会话id
- `u`：用户userId

上面的实现中，有几个通用的成员属性，我们通过自定义的配置，再项目启动时进行初始化，避免后续的重复创建
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312343033.png)

自定义的jwt三个配置属性
```yaml
paicoding:
  jwt:
    issuer: pai_coding # 签发者
    secret: hello_world # 签名密钥
    expire: 2592000000 # jwt的有效期，默认30天
```


### 2.2.2、**jwt校验**
用户每次请求时，依然是沿用之前的session方式的校验逻辑，在Filter层，从请求中，获取Cookie，找到对应的jwt，然后尝试根据jwt获取对应的用户信息
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312345240.png)

注意上面的实现，即使jwt校验通过了，我们也依然从redis中查了一下，判断是否有效

为什么这样设计呢？

因为jwt本身无状态，后端完全可以将redis这一层的存储直接干掉，纯依赖jwt的特性来完成身份鉴权，但是由于它的无状态，后端减少存储压力是一个好处，同样也是一个弊端，后端市区了token的管控权限，如果我们希望提前失效某些用户身份，则无法支持

鉴于此，我们依然保留了redis中存储jwt的方案（这是主要原因，当然技术派作为一个让大家获取更多的知识点教学相长的项目，我们会尽可能的将相关知识点给引入进来，且尽量满足大厂项目规范来实现）
## 2.3、实例体验

接下来我们实际体验一下，线上运行技术派的jwt,登录之后，找到一个请求，找一下cookie中的f-session
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312354991.png)

我们解析一下看下是啥
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308312356492.png)

站在用户的角度，说实话你用session还是jwt，没有什么实质的感受差异，那么为什么还会有这两种不同的技术方向呢？

# 3、session与jwt的对比
| jwt                          | session                  |
| ---------------------------- | ------------------------ |
| 前端存储，通用的校验规则，后端再获取jwt时校验是否有效 | 前端存索引，后端判断session是否有效    |
| 验签，不可篡改                      | 无签名保证，安全性由后端保障           |
| 可存储非敏感信息，如用户名头像等             | 一般不存储业务信息                |
| jwt生成时，指定了有效期，本身不支持续期以及提前失效  | 后端控制有效期，可提前失效或者自动续期      |
| 通常以请求头方式传递                   | 通常以cookie方式传递            |
| 可预防csrf攻击                    | session-cookie方式存在csrf风险 |

关于上面的安全风险，给一个简单的扩展说明

## 3.1、csrf攻击

如再我自己的网站页面上，添加下面内容

```html
<img src="https://paicoding.com/logout" style="display:none;"/>
```

然后当你访问我的网站时，结果发现你在技术派上的登录用户被注销了!!!

使用jwt预防csrf攻击的主要原理就是jwt是通过请求头，由js主动塞进去传递给后端的，而非cookie的方式，从而避免csrf漏洞攻击

但是，喜欢思考的小伙伴，自然就会提问了，技术派的jwt也是用cookie进行携带jwt，并不能解决上面这个问题；同样的，session的方案，也可以将sessionId通过请求头的方式传递，按照这个说法也能避免csrf啊

当然上面这么说，完全没有问题，我们就一项技术本身而言，通常是基于其“官配”方案来讨论其优缺点，即以上的对比，基于下面的搭配来进行的

- session-cookie方案
- jwt-requestHeader方案

最后说一句，当你对某个知识点充分了解之后，主要你能自圆其说，就没有问题，不要迷信权威与标准答案，毕竟这世界上，谁都可能翻车，又哪来的标准答案呢




关于JWT、Session、Session Cookies、Cookies等相关概念可以参考：
- [计算机网络-面试题-2](5、计算机网络/计算机网络-面试题-2.md)
- [补1-CSRF+Cookie的SameSite属性](2、相关技术/13、J2EE基础-Servlet/JavaWeb(石头)/JavaWeb开发实战教程(JSP+Servlet+Mvc)/《第五章%20会话技术Cookie》课程资料/讲义/补1-CSRF+Cookie的SameSite属性.md)
- [18.2、浏览器同源政策及其规避方法](5、计算机网络/Hollis/18.2、浏览器同源政策及其规避方法.md)
- [18.3、跨域资源共享 CORS 详解](5、计算机网络/Hollis/18.3、跨域资源共享%20CORS%20详解.md)
- [补4-JWT跨域认证的问题](2、相关技术/13、J2EE基础-Servlet/JavaWeb(石头)/JavaWeb开发实战教程(JSP+Servlet+Mvc)/《第五章%20会话技术Cookie》课程资料/讲义/补4-JWT跨域认证的问题.md)
- [补5-前端安全系列之二：如何防止CSRF攻击？](2、相关技术/13、J2EE基础-Servlet/JavaWeb(石头)/JavaWeb开发实战教程(JSP+Servlet+Mvc)/《第五章%20会话技术Cookie》课程资料/讲义/补5-前端安全系列之二：如何防止CSRF攻击？.md)
- [补6-鉴权：cookie、session、token、jwt、单点登录](2、相关技术/13、J2EE基础-Servlet/JavaWeb(石头)/JavaWeb开发实战教程(JSP+Servlet+Mvc)/《第五章%20会话技术Cookie》课程资料/讲义/补6-鉴权：cookie、session、token、jwt、单点登录.md)
- [会话技术之Cookie](2、相关技术/13、J2EE基础-Servlet/JavaWeb(石头)/JavaWeb开发实战教程(JSP+Servlet+Mvc)/《第五章%20会话技术Cookie》课程资料/讲义/会话技术之Cookie.md)
- [会话技术之Session](2、相关技术/13、J2EE基础-Servlet/JavaWeb(石头)/JavaWeb开发实战教程(JSP+Servlet+Mvc)/《第六章%20Session%20资料中没有这一章》/会话技术之Session.md)
【主要看面试题-2和补4中的内容】





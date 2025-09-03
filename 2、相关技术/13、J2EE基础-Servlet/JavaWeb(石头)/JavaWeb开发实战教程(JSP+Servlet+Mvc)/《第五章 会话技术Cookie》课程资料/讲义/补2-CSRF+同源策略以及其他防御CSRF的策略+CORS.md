## 前言

本篇文章主要是介绍CSRF + CSRF的防御方式（如：同源策略、CSRF Token、……）+CORS三者之间的关系，关于这些东西更详细的内容可以去看 [补1-CSRF+Cookie的SameSite属性.md](D:\c_51CTO资料\Java\石头老师Java工程师养成之路（部分）\JavaWeb开发实战教程(JSP+Servlet+Mvc)\JavaWeb开发实战教程(JSP+Servlet+Mvc)\《第五章 会话技术Cookie》课程资料\讲义\补1-CSRF+Cookie的SameSite属性.md) 和 [补3-跨域资源共享CORS详解.md](D:\c_51CTO资料\Java\石头老师Java工程师养成之路（部分）\JavaWeb开发实战教程(JSP+Servlet+Mvc)\JavaWeb开发实战教程(JSP+Servlet+Mvc)\《第五章 会话技术Cookie》课程资料\讲义\补3-跨域资源共享CORS详解.md) 和 [补5-前端安全系列之二：如何防止CSRF攻击？.md](D:\c_51CTO资料\Java\石头老师Java工程师养成之路（部分）\JavaWeb开发实战教程(JSP+Servlet+Mvc)\JavaWeb开发实战教程(JSP+Servlet+Mvc)\《第五章 会话技术Cookie》课程资料\讲义\补5-前端安全系列之二：如何防止CSRF攻击？.md)

来自：[浏览器同源策略及跨域的解决方法](https://juejin.cn/post/6844903681683357710)

补充内容来自：[前端安全系列之二：如何防止CSRF攻击？](https://juejin.cn/post/6844903689702866952)
已经粘贴到本地 [补5-前端安全系列之二：如何防止CSRF攻击？.md](D:\c_51CTO资料\Java\石头老师Java工程师养成之路（部分）\JavaWeb开发实战教程(JSP+Servlet+Mvc)\JavaWeb开发实战教程(JSP+Servlet+Mvc)\《第五章 会话技术Cookie》课程资料\讲义\补5-前端安全系列之二：如何防止CSRF攻击？.md)



## 什么是浏览器同源策略

同源策略（Same origin policy）是一种约定，它是浏览器最核心也最基本的安全功能，如果缺少了同源策略，则浏览器的正常功能可能都会受到影响。可以说 Web 是构建在同源策略基础之上的，浏览器只是针对同源策略的一种实现。

它的核心就在于它认为自任何站点装载的信赖内容是不安全的。当被浏览器半信半疑的脚本运行在沙箱时，它们应该只被允许访问来自同一站点的资源，而不是那些来自其它站点可能怀有恶意的资源。

所谓同源是指：域名、协议、端口相同。

下表是相对于 `http://www.laixiangran.cn/home/index.html` 的同源检测结果：

![image-20220928120600097](D:\Tyora\AssociatedPicturesInTheArticles\补2-CSRF+同源策略以及其他防御CSRF的策略+CORS\image-20220928120600097.png)



另外，同源策略又分为以下两种：

1. DOM 同源策略：禁止对不同源页面 DOM 进行操作。这里主要场景是 iframe 跨域的情况，不同域名的 iframe 是限制互相访问的。
2. XMLHttpRequest 同源策略：禁止使用 XHR 对象向不同源的服务器地址发起 HTTP 请求。

## 为什么要有跨域限制

因为存在浏览器同源策略，所以才会有跨域问题。那么浏览器是出于何种原因会有跨域的限制呢。其实不难想到，跨域限制主要的目的就是为了用户的上网安全。

如果浏览器没有同源策略，会存在什么样的安全问题呢。下面从 DOM 同源策略和 XMLHttpRequest 同源策略来举例说明：

**如果没有 DOM 同源策略，也就是说不同域的 iframe 之间可以相互访问，那么黑客可以这样进行攻击：**

1. 做一个假网站，里面用 iframe 嵌套一个银行网站 `http://mybank.com`。
2. 把 iframe 宽高啥的调整到页面全部，这样用户进来除了域名，别的部分和银行的网站没有任何差别。
3. 这时如果用户输入账号密码，我们的主网站可以跨域访问到 `http://mybank.com` 的 dom 节点，就可以拿到用户的账户密码了。

**如果没有 XMLHttpRequest 同源策略，那么黑客可以进行 CSRF（跨站请求伪造） 攻击：**

1. 用户登录了自己的银行页面 `http://mybank.com`，`http://mybank.com` 向用户的 cookie 中添加用户标识。
2. 用户浏览了恶意页面 `http://evil.com`，执行了页面中的恶意 AJAX 请求代码。
3. `http://evil.com` 向 `http://mybank.com` 发起 AJAX HTTP 请求，请求会默认把 `http://mybank.com` 对应 cookie 也同时发送过去。【也就是在evil.com域名下的页面中向mybank.com发送一个请求，并请求中会携带cookie】
4. 银行页面从发送的 cookie 中提取用户标识，验证用户无误，response 中返回请求数据。此时数据就泄露了。
5. 而且由于 Ajax 在后台执行，用户无法感知这一过程。

因此，有了浏览器同源策略，我们才能更安全的上网。

> 补充：来自——>[前端安全系列之二：如何防止CSRF攻击？](https://juejin.cn/post/6844903689702866952)
>
> 同源验证是一个相对简单的防范方法，能够防范绝大多数的CSRF攻击。但这并不是万无一失的，对于安全性要求较高，或者有较多用户输入内容的网站，我们就要对关键的接口做额外的防护措施。

## CSRE Token（补充）

来自：[前端安全系列之二：如何防止CSRF攻击？](https://juejin.cn/post/6844903689702866952)，这一小节只贴过来一部分我认为重要的内容，更多的可以去看原文。

也和同源策略的目的是一样的，都是为了防止CSRF攻击。

前面讲到CSRF的另一个特征是，攻击者无法直接窃取到用户的信息（Cookie，Header，网站内容等），仅仅是冒用Cookie中的信息。

而CSRF攻击之所以能够成功，是因为服务器误把攻击者发送的请求当成了用户自己的请求。那么我们可以要求所有的用户请求都携带一个CSRF攻击者无法获取到的Token。服务器通过校验请求是否携带正确的Token，来把正常的请求和攻击的请求区分开，也可以防范CSRF的攻击。

### 原理

CSRF Token的防护策略分为三个步骤：

1. 将CSRF Token输出到页面中首先，用户打开页面的时候，服务器需要给这个用户生成一个Token，该Token通过加密算法对数据进行加密，一般Token都包括随机字符串和时间戳的组合，显然在提交时Token不能再放在Cookie中了，否则又会被攻击者冒用。因此，为了安全起见Token最好还是存在服务器的Session中，之后在每次页面加载时，使用JS遍历整个DOM树，对于DOM中所有的a和form标签后加入Token。这样可以解决大部分的请求，但是对于在页面加载之后动态生成的HTML代码，这种方法就没有作用，还需要程序员在编码时手动添加Token。

2. 页面提交的请求携带这个Token对于GET请求，Token将附在请求地址之后，这样URL 就变成 [http://url?csrftoken=tokenvalue。](https://link.juejin.cn?target=http%3A%2F%2Furl%3Fcsrftoken%3Dtokenvalue%E3%80%82) 而对于 POST 请求来说，要在 form 的最后加上：

   ```xml
    <input type=”hidden” name=”csrftoken” value=”tokenvalue”/>
   ```

   这样，就把Token以参数的形式加入请求了。

3. 服务器验证Token是否正确当用户从客户端得到了Token，再次提交给服务器的时候，服务器需要判断Token的有效性，验证过程是先解密Token，对比加密字符串以及时间戳，如果加密字符串一致且时间未过期，那么这个Token就是有效的。

   这种方法要比之前检查Referer或者Origin要安全一些，Token可以在产生并放于Session之中，然后在每次请求时把Token从Session中拿出，与请求中的Token进行比对，但这种方法的比较麻烦的在于如何把Token以参数的形式加入请求。 下面将以Java为例，介绍一些CSRF Token的服务端校验逻辑，代码如下：

   ```java
   HttpServletRequest req = (HttpServletRequest)request; 
   HttpSession s = req.getSession(); 
   
   // 从 session 中得到 csrftoken 属性
   String sToken = (String)s.getAttribute(“csrftoken”); 
   if(sToken == null){ 
       // 产生新的 token 放入 session 中
       sToken = generateToken(); 
       s.setAttribute(“csrftoken”,sToken); 
       chain.doFilter(request, response); 
   } else{ 
       // 从 HTTP 头中取得 csrftoken 
       String xhrToken = req.getHeader(“csrftoken”); 
       // 从请求参数中取得 csrftoken 
       String pToken = req.getParameter(“csrftoken”); 
       if(sToken != null && xhrToken != null && sToken.equals(xhrToken)){ 
           chain.doFilter(request, response); 
       }else if(sToken != null && pToken != null && sToken.equals(pToken)){ 
           chain.doFilter(request, response); 
       }else{ 
           request.getRequestDispatcher(“error.jsp”).forward(request,response); 
       } 
   }
   ```

   这个Token的值必须是随机生成的，这样它就不会被攻击者猜到，考虑利用Java应用程序的java.security.SecureRandom类来生成足够长的随机标记，替代生成算法包括使用256位BASE64编码哈希，选择这种生成算法的开发人员必须确保在散列数据中使用随机性和唯一性来生成随机标识。通常，开发人员只需为当前会话生成一次Token。在初始生成此Token之后，该值将存储在会话中，并用于每个后续请求，直到会话过期。当最终用户发出请求时，服务器端必须验证请求中Token的存在性和有效性，与会话中找到的Token相比较。如果在请求中找不到Token，或者提供的值与会话中的值不匹配，则应中止请求，应重置Token并将事件记录为正在进行的潜在CSRF攻击。

### 进阶：使用分布式校验

在大型网站中，使用Session存储CSRF Token会带来很大的压力。访问单台服务器session是同一个。但是现在的大型网站中，我们的服务器通常不止一台，可能是几十台甚至几百台之多，甚至多个机房都可能在不同的省份，用户发起的HTTP请求通常要经过像Ngnix之类的负载均衡器之后，再路由到具体的服务器上，由于Session默认存储在单机服务器内存中，因此在分布式环境下同一个用户发送的多次HTTP请求可能会先后落到不同的服务器上，导致后面发起的HTTP请求无法拿到之前的HTTP请求存储在服务器中的Session数据，从而使得Session机制在分布式环境下失效，因此在分布式集群中CSRF Token需要存储在Redis之类的公共存储空间。

由于使用Session存储，读取和验证CSRF Token会引起比较大的复杂度和性能问题，目前很多网站采用Encrypted Token Pattern方式。这种方法的Token是一个计算出来的结果，而非随机生成的字符串。这样在校验时无需再去读取存储的Token，只用再次计算一次即可。

这种Token的值通常是使用UserID、时间戳和随机数，通过加密的方法生成。这样既可以保证分布式服务的Token一致，又能保证Token不容易被破解。

在token解密成功之后，服务器可以访问解析值，Token中包含的UserID和时间戳将会被拿来被验证有效性，将UserID与当前登录的UserID进行比较，并将时间戳与当前时间进行比较。***<u>【这也就相当于引出了JWT的内容，关于JWT的内容我从阮一峰的网络日志中贴下来了《补4-JWT跨域认证的问题.md》】</u>***



在原文中防护CSRF的策略：

- CSRF自动防御策略：同源检测（Origin 和 Referer 验证）。
- CSRF主动防御措施：Token验证 或者 双重Cookie验证 以及配合Samesite Cookie。
- 保证页面的幂等性，后端接口不要在GET页面中做用户操作。

为了更好的防御CSRF，最佳实践应该是结合上面总结的防御措施方式中的优缺点来综合考虑，结合当前Web应用程序自身的情况做合适的选择，才能更好的预防CSRF的发生。

作者：美团技术团队
链接：https://juejin.cn/post/6844903689702866952
来源：稀土掘金
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。



## 跨域的解决方法

从上面我们了解到了浏览器同源策略的作用，也正是有了跨域限制，才使我们能安全的上网。但是在实际中，有时候我们需要突破这样的限制，因此下面将介绍几种跨域的解决方法。

### CORS（跨域资源共享）



CORS（Cross-origin resource sharing，跨域资源共享）是一个 W3C 标准，定义了在必须访问跨域资源时，浏览器与服务器应该如何沟通。CORS 背后的基本思想，就是使用自定义的 HTTP 头部让浏览器与服务器进行沟通，从而决定请求或响应是应该成功，还是应该失败。

CORS 需要浏览器和服务器同时支持。目前，所有浏览器都支持该功能，IE 浏览器不能低于 IE10。

整个 CORS 通信过程，都是浏览器自动完成，不需要用户参与。对于开发者来说，CORS 通信与同源的 AJAX 通信没有差别，代码完全一样。浏览器一旦发现 AJAX 请求跨源，就会自动添加一些附加的头信息，有时还会多出一次附加的请求，但用户不会有感觉。

因此，实现 CORS 通信的关键是服务器。只要服务器实现了 CORS 接口，就可以跨源通信。

浏览器将CORS请求分成两类：简单请求（simple request）和非简单请求（not-so-simple request）。

只要同时满足以下两大条件，就属于**简单请求**。

1. 请求方法是以下三种方法之一：

- HEAD
- GET
- POST

1. HTTP的头信息不超出以下几种字段：

- Accept
- Accept-Language
- Content-Language
- Last-Event-ID
- Content-Type：只限于三个值 application/x-www-form-urlencoded、multipart/form-data、text/plain

凡是不同时满足上面两个条件，就属于**非简单请求**。

浏览器对这两种请求的处理，是不一样的。

#### 简单请求

1. 在请求中需要附加一个额外的 Origin 头部，其中包含请求页面的源信息（协议、域名和端口），以便服务器根据这个头部信息来决定是否给予响应。例如：`Origin: http://www.laixiangran.cn`
2. 如果服务器认为这个请求可以接受，就在 Access-Control-Allow-Origin 头部中回发相同的源信息（如果是公共资源，可以回发 * ）。例如：`Access-Control-Allow-Origin：http://www.laixiangran.cn`
3. 没有这个头部或者有这个头部但源信息不匹配，浏览器就会驳回请求。正常情况下，浏览器会处理请求。注意，请求和响应都不包含 cookie 信息。
4. 如果需要包含 cookie 信息，ajax 请求需要设置 xhr 的属性 withCredentials 为 true，服务器需要设置响应头部 `Access-Control-Allow-Credentials: true`。

#### 非简单请求

浏览器在发送真正的请求之前，会先发送一个 Preflight 请求给服务器，这种请求使用 OPTIONS 方法，发送下列头部：

- Origin：与简单的请求相同。
- Access-Control-Request-Method: 请求自身使用的方法。
- Access-Control-Request-Headers: （可选）自定义的头部信息，多个头部以逗号分隔。

例如：

```makefile
Origin: http://www.laixiangran.cn
Access-Control-Request-Method: POST
Access-Control-Request-Headers: NCZ
复制代码
```

发送这个请求后，服务器可以决定是否允许这种类型的请求。服务器通过在响应中发送如下头部与浏览器进行沟通：

- Access-Control-Allow-Origin：与简单的请求相同。
- Access-Control-Allow-Methods: 允许的方法，多个方法以逗号分隔。
- Access-Control-Allow-Headers: 允许的头部，多个方法以逗号分隔。
- Access-Control-Max-Age: 应该将这个 Preflight 请求缓存多长时间（以秒表示）。

例如：

```makefile
Access-Control-Allow-Origin: http://www.laixiangran.cn
Access-Control-Allow-Methods: GET, POST
Access-Control-Allow-Headers: NCZ
Access-Control-Max-Age: 1728000
复制代码
```

一旦服务器通过 Preflight 请求允许该请求之后，以后每次浏览器正常的 CORS 请求，就都跟简单请求一样了。

#### 优点

- CORS 通信与同源的 AJAX 通信没有差别，代码完全一样，容易维护。
- 支持所有类型的 HTTP 请求。

#### 缺点

- 存在兼容性问题，特别是 IE10 以下的浏览器。
- 第一次发送非简单请求时会多一次请求。

### JSONP 跨域

由于 `script` 标签不受浏览器同源策略的影响，允许跨域引用资源。因此可以通过动态创建 script 标签，然后利用 src 属性进行跨域，这也就是 JSONP 跨域的基本原理。

直接通过下面的例子来说明 JSONP 实现跨域的流程：

```ini
// 1. 定义一个 回调函数 handleResponse 用来接收返回的数据
function handleResponse(data) {
    console.log(data);
};

// 2. 动态创建一个 script 标签，并且告诉后端回调函数名叫 handleResponse
var body = document.getElementsByTagName('body')[0];
var script = document.gerElement('script');
script.src = 'http://www.laixiangran.cn/json?callback=handleResponse';
body.appendChild(script);

// 3. 通过 script.src 请求 `http://www.laixiangran.cn/json?callback=handleResponse`，
// 4. 后端能够识别这样的 URL 格式并处理该请求，然后返回 handleResponse({"name": "laixiangran"}) 给浏览器
// 5. 浏览器在接收到 handleResponse({"name": "laixiangran"}) 之后立即执行 ，也就是执行 handleResponse 方法，获得后端返回的数据，这样就完成一次跨域请求了。
复制代码
```

#### 优点

- 使用简便，没有兼容性问题，目前最流行的一种跨域方法。

#### 缺点

- 只支持 GET 请求。
- 由于是从其它域中加载代码执行，因此如果其他域不安全，很可能会在响应中夹带一些恶意代码。
- 要确定 JSONP 请求是否失败并不容易。虽然 HTML5 给 script 标签新增了一个 onerror 事件处理程序，但是存在兼容性问题。

### 图像 Ping 跨域

由于 `img` 标签不受浏览器同源策略的影响，允许跨域引用资源。因此可以通过 img 标签的 src 属性进行跨域，这也就是图像 Ping 跨域的基本原理。

直接通过下面的例子来说明图像 Ping 实现跨域的流程：

```ini
var img = new Image();

// 通过 onload 及 onerror 事件可以知道响应是什么时候接收到的，但是不能获取响应文本
img.onload = img.onerror = function() {
    console.log("Done!");
}

// 请求数据通过查询字符串形式发送
img.src = 'http://www.laixiangran.cn/test?name=laixiangran';
复制代码
```

#### 优点

- 用于实现跟踪用户点击页面或动态广告曝光次数有较大的优势。

#### 缺点

- 只支持 GET 请求。
- 只能浏览器与服务器的单向通信，因为浏览器不能访问服务器的响应文本。

### 服务器代理

浏览器有跨域限制，但是服务器不存在跨域问题，所以可以由服务器请求所有域的资源再返回给客户端。

服务器代理是万能的。

### document.domain 跨域

对于主域名相同，而子域名不同的情况，可以使用 document.domain 来跨域。这种方式非常适用于 iframe 跨域的情况。

比如，有一个页面，它的地址是 `http://www.laixiangran.cn/a.html`，在这个页面里面有一个 iframe，它的 src 是 `http://laixiangran.cn/b.html`。很显然，这个页面与它里面的 iframe 框架是不同域的，所以我们是无法通过在页面中书写 js 代码来获取 iframe 中的东西的。

这个时候，document.domain 就可以派上用场了，我们只要把 `http://www.laixiangran.cn/a.html` 和 `http://laixiangran.cn/b.html` 这两个页面的 document.domain 都设成相同的域名就可以了。但要注意的是，document.domain 的设置是有限制的，我们只能把 document.domain 设置成自身或更高一级的父域，且主域必须相同。例如：`a.b.laixiangran.cn` 中某个文档的 document.domain 可以设成 `a.b.laixiangran.cn`、`b.laixiangran.cn` 、`laixiangran.cn` 中的任意一个，但是不可以设成 `c.a.b.laixiangran.cn` ，因为这是当前域的子域，也不可以设成 `baidu.com`，因为主域已经不相同了。

例如，在页面 `http://www.laixiangran.cn/a.html` 中设置document.domain：

```xml
<iframe src="http://laixiangran.cn/b.html" id="myIframe" onload="test()">
<script>
    document.domain = 'laixiangran.cn'; // 设置成主域
    function test() {
        console.log(document.getElementById('myIframe').contentWindow);
    }
</script>
复制代码
```

在页面 `http://laixiangran.cn/b.html` 中也设置 document.domain，而且这也是必须的，虽然这个文档的 domain 就是 `laixiangran.cn`，但是还是必须显式地设置 document.domain 的值：

```xml
<script>
    document.domain = 'laixiangran.cn'; // document.domain 设置成与主页面相同
</script>
复制代码
```

这样，`http://www.laixiangran.cn/a.html` 就可以通过 js 访问到 `http://laixiangran.cn/b.html` 中的各种属性和对象了。

### [window.name](https://link.juejin.cn?target=http%3A%2F%2Fwindow.name) 跨域

window 对象有个 name 属性，该属性有个特征：即在一个窗口（window）的生命周期内，窗口载入的所有的页面（不管是相同域的页面还是不同域的页面）都是共享一个 `window.name` 的，每个页面对 `window.name` 都有读写的权限，`window.name` 是持久存在一个窗口载入过的所有页面中的，并不会因新页面的载入而进行重置。

通过下面的例子介绍如何通过 [window.name](https://link.juejin.cn?target=http%3A%2F%2Fwindow.name) 来跨域获取数据的。

页面 `http://www.laixiangran.cn/a.html` 的代码：

```xml
<iframe src="http://laixiangran.cn/b.html" id="myIframe" onload="test()" style="display: none;">
<script>
    // 2. iframe载入 "http://laixiangran.cn/b.html 页面后会执行该函数
    function test() {
        var iframe = document.getElementById('myIframe');
        
        // 重置 iframe 的 onload 事件程序，
        // 此时经过后面代码重置 src 之后，
        // http://www.laixiangran.cn/a.html 页面与该 iframe 在同一个源了，可以相互访问了
        iframe.onload = function() {
            var data = iframe.contentWindow.name; // 4. 获取 iframe 里的 window.name
            console.log(data); // hello world!
        };
        
        // 3. 重置一个与 http://www.laixiangran.cn/a.html 页面同源的页面
        iframe.src = 'http://www.laixiangran.cn/c.html';
    }
</script>
复制代码
```

页面 `http://laixiangran.cn/b.html` 的代码：

```ini
<script type="text/javascript">
    // 1. 给当前的 window.name 设置一个 http://www.laixiangran.cn/a.html 页面想要得到的数据值 
    window.name = "hello world!";
</script>
复制代码
```

### location.hash 跨域

location.hash 方式跨域，是子框架修改父框架 src 的 hash 值，通过这个属性进行传递数据，且更改 hash 值，页面不会刷新。但是传递的数据的字节数是有限的。

页面 `http://www.laixiangran.cn/a.html` 的代码：

```xml
<iframe src="http://laixiangran.cn/b.html" id="myIframe" onload="test()" style="display: none;">
<script>
    // 2. iframe载入 "http://laixiangran.cn/b.html 页面后会执行该函数
    function test() {
        // 3. 获取通过 http://laixiangran.cn/b.html 页面设置 hash 值
        var data = window.location.hash;
        console.log(data);
    }
</script>
复制代码
```

页面 `http://laixiangran.cn/b.html` 的代码：

```ini
<script type="text/javascript">
    // 1. 设置父页面的 hash 值
    parent.location.hash = "world";
</script>
复制代码
```

### postMessage 跨域

window.postMessage(message，targetOrigin) 方法是 HTML5 新引进的特性，可以使用它来向其它的 window 对象发送消息，无论这个 window 对象是属于同源或不同源。这个应该就是以后解决 dom 跨域通用方法了。

调用 postMessage 方法的 window 对象是指要接收消息的那一个 window 对象，该方法的第一个参数 message 为要发送的消息，类型只能为字符串；第二个参数 targetOrigin 用来限定接收消息的那个 window 对象所在的域，如果不想限定域，可以使用通配符 *。

需要接收消息的 window 对象，可是通过监听自身的 message 事件来获取传过来的消息，消息内容储存在该事件对象的 data 属性中。

页面 `http://www.laixiangran.cn/a.html` 的代码：

```xml
<iframe src="http://laixiangran.cn/b.html" id="myIframe" onload="test()" style="display: none;">
<script>
    // 1. iframe载入 "http://laixiangran.cn/b.html 页面后会执行该函数
    function test() {
        // 2. 获取 http://laixiangran.cn/b.html 页面的 window 对象，
        // 然后通过 postMessage 向 http://laixiangran.cn/b.html 页面发送消息
        var iframe = document.getElementById('myIframe');
        var win = iframe.contentWindow;
        win.postMessage('我是来自 http://www.laixiangran.cn/a.html 页面的消息', '*');
    }
</script>
复制代码
```

页面 `http://laixiangran.cn/b.html` 的代码：

```xml
<script type="text/javascript">
    // 注册 message 事件用来接收消息
    window.onmessage = function(e) {
        e = e || event; // 获取事件对象
        console.log(e.data); // 通过 data 属性得到发送来的消息
    }
</script>
复制代码
```

## 参考资料

- [js中几种实用的跨域方法原理详解](https://link.juejin.cn/?target=https%3A%2F%2Fwww.cnblogs.com%2F2050%2Fp%2F3191744.html)
- [跨域的那些事儿](https://link.juejin.cn/?target=https%3A%2F%2Fzhuanlan.zhihu.com%2Fp%2F28562290)
- [跨域资源共享 CORS 详解](https://link.juejin.cn/?target=http%3A%2F%2Fwww.ruanyifeng.com%2Fblog%2F2016%2F04%2Fcors.html)


作者：laixiangran
链接：https://juejin.cn/post/6844903681683357710
来源：稀土掘金
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
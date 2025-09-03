

优化Web应用的典型技术：缓存控制头信息、Gzip、应用缓存、ETag、反应型技术【异步方法调用和WebSocket】

## 一、模板缓存

spring.thymeleaf.cache=true
spring.messages.cache-duration=

## 二、Gzip压缩

　　Gzip是一种能够被浏览器直接理解的压缩算法。服务器会提供压缩响应，会耗一些cpu，但是减少带宽

　　GZIP压缩是一个经常被用到的WEB性能优化的技巧，它主要是对页面代码，CSS，Javascript，PHP等文件进行压缩，而且在压缩的前后，文件的大小会有明显的改变，从而达到网站访问加速的目的。

　　GZIP压缩时，WEB服务器与浏览器之间的协商过程如下：

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

1、首先浏览器请求某个URL地址，并在请求的开始部分头(head) 设置属性accept-encoding值为gzip、deflate，表明浏览器支持gzip和deflate这两种压缩方式（事实上deflate也是使用GZIP压缩协议，在之后的内容之我们会介绍二者之间的区别）；

2、WEB服务器接收到请求后判断浏览器是否支持GZIP压缩，如果支持就传送压缩后的响应内容，否则传送不经过压缩的内容；

3、浏览器获取响应内容后，判断内容是否被压缩，如果是压缩文件则解压缩，然后显示响应页面的内容。

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

在Springboot中配置gzip

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

# 是否启用压缩 默认false
server.compression.enabled=true
# 默认"text/html", "text/xml", "text/plain","text/css", "text/javascript", "application/javascript", "application/json",
#            "application/xml"
server.compression.mime-types=text/html,text/xml,text/plain,text/css,text/javascript,application/javascript\
  ,application/json,
#content-length 在压缩启用后。返回数据多大开始启用gzip，默认2048 为了测试添加为1
server.compression.min-response-size=1

![复制代码](https://assets.cnblogs.com/images/copycode.gif)

测试1、未开启压缩

# 是否启用压缩 默认false
server.compression.enabled=false

客户端请求头

Accept:text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8
Accept-Encoding:gzip, deflate, br
Accept-Language:zh-CN,zh;q=0.9,en;q=0.8

服务端响应

Content-Length:60973
Content-Type:text/html;charset=UTF-8
Date:Wed, 30 Jan 2019 08:19:19 GMT

测试2、开启压缩

# 是否启用压缩 默认false
server.compression.enabled=true
#content-length 在压缩启用后。返回数据多大开始启用gzip，默认2048 为了测试添加为1
server.compression.min-response-size=1

客户端请求头不变

服务端响应

Content-Encoding:gzip
Content-Type:text/html;charset=UTF-8
Date:Wed, 30 Jan 2019 08:20:50 GMT
Transfer-Encoding:chunked
Vary:Accept-Encoding

## 三、缓存控制和缓存校验

## 3.1、使用chrome的开发者模式

首先浏览器请求某个URL地址，并在请求的开始部分头(head) 设置属性accept-encoding值为gzip、deflate，表明浏览器支持gzip和def

　　第一部分General是概要，包含请求地址，请求方式，状态码，服务器地址以及Referrer 策略。  
　　第二部分是应答头部，是服务器返回的。  
　　第三部分是请求头部，是客户端发送的。

　　RFC2616规定的47种http报文首部字段中与缓存相关的字段：

1、通用首部字段

　　![](https://img2018.cnblogs.com/blog/292888/201902/292888-20190211103348907-460957894.png)

2、请求首部字段

　　![](https://img2018.cnblogs.com/blog/292888/201902/292888-20190211103403683-652137886.png)

3、响应首部字段

　　![](https://img2018.cnblogs.com/blog/292888/201902/292888-20190211103419723-1258073464.png)

4、实体首部字段

　　![](https://img2018.cnblogs.com/blog/292888/201902/292888-20190211103435825-1188163467.png)

## 3.2、Http 1.0 缓存控制方式

　　在 http1.0 时代，给客户端设定缓存方式可通过两个字段——Pragma和Expires来规范。虽然这两个字段早可抛弃，但为了做http协议的向下兼容，你还是可以看到很多网站依旧会带上这两个字段。例如在访问个别网站的时候，通过浏览器调试工具可以看到部分HTTP响应是包含Expires头部的。

### 3.2.1、Pragma-禁用缓存

　　当该字段值为no-cache的时候（事实上现在RFC中也仅标明该可选值），会知会客户端不要对该资源读缓存，即每次都得向服务器发一次请求才行。

### 3.2.2、Expires-启用缓存和缓存时间

　　有了Pragma来禁用缓存，自然也需要有个东西来启用缓存和定义缓存时间，对http1.0而言，Expires就是做这件事的首部字段。 Expires的值对应一个GMT（格林尼治时间），比如Mon, 22 Jul 2002 11:12:01 GMT来告诉浏览器资源缓存过期时间，如果还没过该时间点则不发请求。　

　　需要注意的是，响应报文中Expires所定义的缓存时间是相对服务器上的时间而言的，其定义的是资源“失效时刻”，如果客户端上的时间跟服务器上的时间不一致（特别是用户修改了自己电脑的系统时间），那缓存时间可能就没意义了。　

## 3.3、Http 1.1 缓存控制

　　缓存控制由服务器端发送一组HTTP头信息，他将会控制用户浏览器如何缓存资源。

　　如果一个报文中同时出现Pragma和Cache-Control时，以Pragma为准。同时出现Cache-Control和Expires时，以Cache-Control为准。

　　即优先级从高到低是 **Pragma -> Cache-Control -> Expires**

### 3.3.1、Cache-Control

1、前提注意：

　　符合缓存策略时，服务器不会发送新的资源，但不是说客户端和服务器就没有会话了，客户端还是会发请求到服务器的。  
　　Cache-Control除了在响应中使用，在请求中也可以使用。我们用开发者工具来模拟下请求时带上Cache-Control：勾选Disable cache，刷新页面，可以看到Request Headers中有个字段Cache-Control: no-cache。  
　　同时在Response Headers中也能到Cache-Control字段，它的值是must-revalidate，这是服务端设置的。

　　Cache-Control也是一个通用首部字段，这意味着它能分别在请求报文和响应报文中使用。在RFC中规范了 Cache-Control 的格式为：

"Cache-Control" ":" cache-directive

2、Http Status 304 说明

　　Http status 304 当一个客户端（通常是浏览器）向web服务器发送一个请求，如果web服务器返回304响应，他不包含任何响应的内容，只是提示客户端缓存的内容是最新的，可以直接使用。这种方法可以节省带宽，避免重复响应。

3、作为请求首部时，cache-directive 的可选值有：

|   |   |
|---|---|
|字段名称|说明|
|no-cache|告知（代理）服务器不直接使用缓存，要求向原服务器发起请求|
|no-store|所有内容都不会被保存到缓存或Internet临时文件中|
|max-age=delta-seconds|告知服务器客户端希望接收一个存在时间（age）不大于delta-seconds秒的资源|
|max-stale[=delta-seconds]|告知（代理）服务器客户端愿意接收一个超过缓存时间的资源，若有定义<br><br>delta-seconds则为delta-srconds秒，若没有则为任意超出的时间|
|min-freash=delta-seconds|告知（代理）服务器客户端希望接收一个在小于delta-seconds秒内被更新过的资源|
|no-transform|告知（代理）服务器客户端希望获取实体数据没有被转换（比如压缩）过的资源|
|only-if-cached|告知（代理）服务器客户端希望获取缓存的内容（若有），而不用向原服务器发去请求|
|cache-extension|自定义扩展值，若服务器器不识别该值将被忽略|
|||

4、作为响应首部时，cache-directive 的可选值有：

|   |   |
|---|---|
|字段名称|说明|
|public|表名任何情况下都得缓存该资源（即使是需要http认证的资源）|
|Private[="field-name"]|表明返回报文中全部或部分（若指定了field-name则为field-name的字段数据）仅开  <br>放给某些用户（服务器指定的share-user，如代理服务器）做缓存使用，其他用户则  <br>不能缓存这些数据|
|no-cache|不直接使用缓存，要求向服务器发起（新鲜度校验）请求|
|no-store|所有内容都不会被保存到缓存或Internet临时文件中|
|max-age=delta-seconds|告知客户端该资源在delta-seconds秒内是新鲜的，无需向服务器发请求|
|s-maxage=delta-seconds|同max-age，但仅应用于共享缓存（如代理）|
|no-transform|告知客户端缓存文件时不得对实体数据做任何改变|
|only-if-cached|告知（代理）服务器客户端希望获取缓存的内容（若有），而不用向原服务器发去请求|
|must-revalidate|当前资源一定是向原服务器发去验证请求的，若请求失败会返回504（而非代理服务器  <br>上的缓存）|
|proxy-revalidate|与must-revalidate类似，但仅能应用于共享缓存（如代理）|
|cache-extension|自定义扩展值，若服务器器不识别该值将被忽略|
|||

5、no-store优先级最高

　　在Cache-Control 中，这些值可以自由组合，多个值如果冲突时，也是有优先级的，而no-store优先级最高。本地不保存，每次都需要服务器发送资源。

6、public和private的选择

　　如果你用了CDN，你需要关注下这个值。CDN厂商一般会要求cache-control的值为public，提升缓存命中率。如果你的缓存命中率很低，而访问量很大的话，可以看下是不是设置了private，no-cache这类的值。如果定义了max-age，可以不用再定义public，它们的意义是一样的。

7、max-age

　　max-age：用来指定引用文档过期时间【如页面内引用的js文件等】。　　　　

　　　　max-age>0 时 页面内引用的资源直接从游览器缓存中 提取，此时http status是304，无论被引用的资源服务器端是否改变，可以查看

　　　　　　示例：第一次请求，test.html,test.js的http status均是200

　　　　　　　　![](https://img2018.cnblogs.com/blog/292888/201901/292888-20190130174951519-1992644423.png)

　　　　　　　　![](https://img2018.cnblogs.com/blog/292888/201901/292888-20190130173439174-231674248.png)

　　　　　　第二次请求，test.html的http status是304，test.js[引用资源]的http status是200，但是数据来自缓存

　　　　　　　　![](https://img2018.cnblogs.com/blog/292888/201901/292888-20190130175039108-541551150.png)

　　　　　　第三次请求，修改服务端js，后请求，因为max-age=30000，test.html的http status是304，test.js[引用资源]的http status是200，但是数据来自缓存

　　　　　　　　![](https://img2018.cnblogs.com/blog/292888/201901/292888-20190130175140269-1518409078.png)

　　　　max-age<=0 时 页面或页面内引用的资源都会向server发送http请求，请求确认该资源是否有修改 有的话 返回200 ,无的话返回304。

　　　　　　第一次请求，test.html,test.js的http status均是200

　　　　　　　　![](https://img2018.cnblogs.com/blog/292888/201901/292888-20190130174158953-1315573811.png)

　　　　　　第二次请求，test.html,test.js的http status均是304

　　　　　　　　![](https://img2018.cnblogs.com/blog/292888/201901/292888-20190130174634647-1220609151.png)

　　　　　　第三次请求，修改远端js，客户端重新获取，test.html的http status是304，test.js[引用资源]的http status是200，数据来自服务端，size不是from cache

　　　　　　　　![](https://img2018.cnblogs.com/blog/292888/201901/292888-20190130174723174-1795724677.png) 　　　　　　　　

　　　　注意：无论max-age什么值，单独请求回车刷新是会发请求的 如果服务器端的文件没有产生变化，那么会返回304，比如单独访问 一个js

## 3.4、缓存校验

　　在缓存中，我们需要一个机制来验证缓存是否有效。比如服务器的资源更新了，客户端需要及时刷新缓存；又或者客户端的资源过了有效期，但服务器上的资源还是旧的，此时并不需要重新发送。缓存校验就是用来解决这些问题的，在http 1.1 中，我们主要关注下Last-Modified 和 etag 这两个字段。

　　HTTP提供了自带的缓存框架。你需要做的是在返回的时候加入一些返回头信息，在接受输入的时候加入输入验证。基本两种方法：

　　　**ETag：**当生成请求的时候，在HTTP头里面加入ETag，其中包含请求的校验和和哈希值，这个值和在输入变化的时候也应该变化。如果输入的HTTP请求包含IF-NONE-MATCH头以及一个ETag值，那么API应该返回304 not modified状态码，而不是常规的输出结果。

　　　**Last-Modified：**和etag一样，只是多了一个时间戳。返回头里的Last-Modified：包含了 [RFC 1123](http://www.ietf.org/rfc/rfc1123.txt) 时间戳，它和IF-MODIFIED-SINCE一致。HTTP规范里面有三种date格式，服务器应该都能处理。

1、Last-Modified

　　服务端在返回资源时，会将该资源的最后更改时间通过Last-Modified字段返回给客户端。客户端下次请求时通过If-Modified-Since或者If-Unmodified-Since带上Last-Modified，服务端检查该时间是否与服务器的最后修改时间一致：如果一致，则返回304状态码，不返回资源；如果不一致则返回200和修改后的资源，并带上新的时间。

　　　![](https://img2018.cnblogs.com/blog/292888/201902/292888-20190211102135245-1194153448.png)

　　If-Modified-Since和If-Unmodified-Since的区别是：  
　　　　If-Modified-Since：告诉服务器如果时间一致，返回状态码304  
　　　　If-Unmodified-Since：告诉服务器如果时间不一致，返回状态码412

2、etag

　　单纯的以修改时间来判断还是有缺陷，比如文件的最后修改时间变了，但内容没变。对于这样的情况，我们可以使用etag来处理。  
　　etag的方式是这样：服务器通过某个算法对资源进行计算，取得一串值(类似于文件的md5值)，之后将该值通过etag返回给客户端，客户端下次请求时通过If-None-Match或If-Match带上该值，服务器对该值进行对比校验：如果一致则不要返回资源。

　　If-None-Match和If-Match的区别是：  
　　　　If-None-Match：告诉服务器如果一致，返回状态码304，不一致则返回资源  
　　　　If-Match：告诉服务器如果不一致，返回状态码412

## 3.5、小结

　　1、缓存开关是： pragma， cache-control。

　　2、缓存校验有：Expires，Last-Modified，etag。需要兼容HTTP1.0的时候需要使用Expires，不然可以考虑直接使用Cache-Control。需要处理一秒内多次修改的情况，或者其他Last-Modified处理不了的情况，才使用ETag，否则使用Last-Modified。

　　3、缓存头部对比

|头部|优势和特点|劣势和问题|
|---|---|---|
|Expires|1、HTTP 1.0 产物，可以在HTTP 1.0和1.1中使用，简单易用。  <br>2、以时刻标识失效时间。|1、时间是由服务器发送的(UTC)，如果服务器时间和客户端时间存在不一致，可能会出现问题。  <br>2、存在版本问题，到期之前的修改客户端是不可知的。|
|Cache-Control|1、HTTP 1.1 产物，以时间间隔标识失效时间，解决了Expires服务器和客户端相对时间的问题。  <br>2、比Expires多了很多选项设置。|1、HTTP 1.1 才有的内容，不适用于HTTP 1.0 。  <br>2、存在版本问题，到期之前的修改客户端是不可知的。|
|Last-Modified|1、不存在版本问题，每次请求都会去服务器进行校验。服务器对比最后修改时间如果相同则返回304，  <br>不同返回200以及资源内容。|1、只要资源修改，无论内容是否发生实质性的变化，都会将该资源返回客户端。例如周期性重写，  <br>这种情况下该资源包含的数据实际上一样的。  <br>2、以时刻作为标识，无法识别一秒内进行多次修改的情况。  <br>3、某些服务器不能精确的得到文件的最后修改时间。|
|ETag|1、可以更加精确的判断资源是否被修改，可以识别一秒内多次修改的情况。  <br>2、不存在版本问题，每次请求都回去服务器进行校验。|1、计算ETag值需要性能损耗。  <br>2、分布式服务器存储的情况下，计算ETag的算法如果不一样，会导致浏览器从一台服务器上获得页面  <br>内容后到另外一台服务器上进行验证时发现ETag不匹配的情况。|

  
　　3、从状态码的角度来看，它们的关系如下图：

　　　　　　　　![](https://img2018.cnblogs.com/blog/292888/201902/292888-20190211102955888-1872498354.png)

　　4、cache-control的各个值关系如下图

　　　　![](https://img2018.cnblogs.com/blog/292888/201902/292888-20190211102702554-1041932278.png)

　　原文参看地址：https://imweb.io/topic/5795dcb6fb312541492eda8c

## 3.6、用户刷新访问行为

1、在URI输入栏中输入然后回车/通过书签访问

　　可以看到返回响应码是 `200 OK (from cache)`，浏览器发现该资源已经缓存了而且没有过期（通过Expires头部或者Cache-Control头部），没有跟服务器确认，而是直接使用了浏览器缓存的内容。其中响应内容和之前的响应内容一模一样，例如其中的Date时间是上一次响应的时间。

2、F5/点击工具栏中的刷新按钮/右键菜单重新加载

　　F5的作用和直接在URI输入栏中输入然后回车是不一样的，F5会让浏览器无论如何都发一个HTTP Request给Server，即使先前的响应中有Expires头部。

　　其中Cache-Control是Chrome强制加上的，而If-Modified-Since是因为获取该资源的时候包含了Last-Modified头部，浏览器会使用If-Modified-Since头部信息重新发送该时间以确认资源是否需要重新发送。 实际上Server没有修改这个index.css文件，所以返回了一个`304(Not Modified)`，这样的响应信息很小，所消耗的route-trip不多，网页很快就刷新了。

3、Ctl+F5

　　Ctrl+F5是彻底的从Server拿一份新的资源过来，所以不光要发送HTTP request给Server，而且这个请求里面连If-Modified-Since/If-None-Match都没有，这样Server不能返回304，而是把整个资源原原本本地返回一份，这样，Ctrl+F5引发的传输时间变长了，自然网页Refresh的也慢一些。我们可以看到该操作返回了200，并刷新了相关的缓存控制时间。

　　实际上，为了保证拿到的是从Server上最新的，Ctrl+F5不只是去掉了If-Modified-Since/If-None-Match，还需要添加一些HTTP Headers。按照HTTP/1.1协议，Cache不光只是存在Browser终端，从Browser到Server之间的中间节点(比如Proxy)也可能扮演Cache的作用，为了防止获得的只是这些中间节点的Cache，需要告诉他们，别用自己的Cache敷衍我，往Upstream的节点要一个最新的copy吧。  
　　在Chrome 51 中会包含两个头部信息， 作用就是让中间的Cache对这个请求失效，这样返回的绝对是新鲜的资源。

Cache-Control: no-cache
Pragma: no-cache

## 3.7、避免过度304

　　可以通过标识文件版本名、加长缓存时间的方式来减少304响应。

　　如果Expires和Cache-Control时间过长长，导致用户无法得到其最近的内容。

　　把服务侧ETag的那一套理论搬到了前端来使用。 页面的静态资源以版本形式发布，常用的方法是在文件名或参数带上一串md5或时间标记符：

https://hm.baidu.com/hm.js?e23800c454aa573c0ccb16b52665ac26
http://tb1.bdstatic.com/tb/_/tbean_safe_ajax_94e7ca2.js
http://img1.gtimg.com/ninja/2/2016/04/ninja145972803357449.jpg

　　那么在文件没有变动的时候，浏览器不用发起请求直接可以使用缓存文件；而在文件有变化的时候，由于文件版本号的变更，导致文件名变化，请求的url变了，自然文件就更新了。这样能确保客户端能及时从服务器收取到新修改的文件。通过这样的处理，增长了静态资源，特别是图片资源的缓存时间，避免该资源很快过期，客户端频繁向服务端发起资源请求，服务器再返回304响应的情况（有Last-Modified/Etag）。

转载自：[https://www.cnblogs.com/bjlhx/p/10338625.html](https://www.cnblogs.com/bjlhx/p/10338625.html)



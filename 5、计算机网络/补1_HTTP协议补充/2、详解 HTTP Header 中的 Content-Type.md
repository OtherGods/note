
# 1、简介

HTTP 请求头/响应头 Content-Type 用于向接收方说明传输资源的媒体类型。例如，如果传输的是图片，那么它的媒体类型可能会是 `image/png` 、`image/jpg`。

在浏览器中，浏览器会根据 Content-Type 判断响应体的资源类型，然后根据不同文件类型做出不同的展示。例如，对于同样一张图片，如果 Response Header 中声明了 `Content-type: image/jpeg`，浏览器会以图片形式展示；如果没有声明 Content-Type，那么浏览器会以文本形式展示。如下所示：

**代码：**
```php
<?php 
header('Content-type: image/jpeg');//with header Content type  
echo file_get_contents("https://media.geeksforgeeks.org/wp-content/uploads/geeksforgeeks-6.png"); 
?> 
```

**output：**
![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c1b0160289b34d79b9606a15823ba85f~tplv-k3u1fbpfcp-zoom-in-crop-mark:4536:0:0:0.awebp)

**代码：**
```php
<?php
// Without header 
echo file_get_contents("https://media.geeksforgeeks.org/wp-content/uploads/geeksforgeeks-6.png"); 
?> 
```

**output：**
```ruby
?PNG  IHDRX??'?iCCPsRGB IEC61966-2.1(?u??+DQ??3????????????63??P????H?U????l??RDJV???9oF?
$sn????{N???pZ??^?d?Z(p?E?]??h??QEW?f??T??{, f???????????z?aE??????y???6%]>vkrA?;S?????d??M?
¡?6???`%?????&???Q-Z?j????BSZo?a???}N ?._u {??#??N?g?{-bKGD??????? pHYs.#.#x??vtIME?4_?X 
IDATx??w?U??????MB$??$@@? 2t?"EDa???"? C?*C????Hq?ja??w ????????L{??}?}??w?;??{???{.4, ???j???
q10??_??h2]`P??:^?5??@?W?=????????XY??? w.??9??`z?1?!V??B????XM~^?|?1?qm???(?h??C?OV?js{e?+ 
L?b?{%?@`?+:sQ?@?
```

# 2、规范

Ccontent-Type包含三个指令：
- media type：声明传输数据的媒体类型（ [MIME](https://link.juejin.cn?target=https%3A%2F%2Fdeveloper.mozilla.org%2Fzh-CN%2Fdocs%2FWeb%2FHTTP%2FBasics_of_HTTP%2FMIME_types "https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Basics_of_HTTP/MIME_types") ）；
- charset：声明传输数据采用的是何种字符集；
- boundary：数据分界符，有多部分数据实体时（multipart/form-data、multipart/byteranges），该指令是必需的，用于封装消息的多个部分的边界；其由 1 到 70 个字符组成，浏览器中会自动生成，该字符集对于通过网关鲁棒性良好，不以空白结尾。

# 3、POST请求常用数据类型

GET和POST是我们最常用的两个HTTP请求方法。对于GET请求，需要传递的数据比较简单，我们通常使用QueryString的方式传递，例如：`https://test.com/api?a=1&b=2` ，那么Content-Type的值就不是那么重要了。对于POST请求，Content-Type的值就非常重要了，需要根据不同场景做不同选择。

在 Form 表单中，enctype 属性的值决定了数据以何种编码传输。其默认值为 `application/x-www-form-urlencoded`。详细请看 [W3C：Form content types。](https://link.juejin.cn/?target=https%3A%2F%2Fwww.w3.org%2FTR%2Fhtml401%2Finteract%2Fforms.html%23h-17.13.4.1 "https://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.1")

## 3.1、application/x-www-form-urlencoded

该值是 Form 默认的编码方式，使用该值时，提交表单时内容必须经过如下规则编码：

1. 空格转换为 “+” 号；非字母数字的其它字符转换为类似于“%E0”的两位 16 进制表示的 ASCII 码；换行符被转换为“CR LF”；
2. 数据项名称和数据值以“=”号分割，数据项与数据项之间以“&”分割；

示例：
```html
<form action="https://xxx.com/api/submit" method="post">
    <input type="text" name="name" value="Javon Yan">
    <input type="text" name="age" value="18">
    <button type="submit">Submit</button>
</form>
```

请求体及请求头：
```java
// Request Header 部分省略
POST /foo HTTP/1.1
Content-Length: 37
content-type: application/x-www-form-urlencoded

// Body
name=Javon+Yan&age=18
```

## 3.2、multipart/form-data

对于二进制文件或者非 ASCII 字符的传输，`application/x-www-form-urlencoded` 是低效的。对于包含文件、二进制数据、非 ASCII 字符的内容，应该使用 `multipart/form-data`。 `multipart/form-data` 的请求体包含多个部分，需要通过 boundary 字符分割。

以下示例中，Form 表单 enctype 设置为 `multipart/form-data` ，请求头及请求体如下所示，浏览器自动生成随机的 boundary 并添加在请求头 Content-Type 中，请求体也根据生成的 boundary 分割各个字段的数据。

示例：
```html
<form action="https://xxx.com/api/submit" method="post" enctype="multipart/form-data">
    <input type="text" name="name" value="Javon Yan">
    <input type="text" name="age" value="18">
    <input type="file" name="file">
    <button type="submit">Submit</button>
</form>
```

请求头及请求体：
```java
// Request Header 部分省略
POST /foo HTTP/1.1
Content-Length: 10240
Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryujecLxDFPt6acCab

// Body
------WebKitFormBoundaryujecLxDFPt6acCab
Content-Disposition: form-data; name="name"

Javon Yan
------WebKitFormBoundaryujecLxDFPt6acCab
Content-Disposition: form-data; name="age"

18
------WebKitFormBoundaryujecLxDFPt6acCab
Content-Disposition: form-data; name="file"; filename="avatar.png"
Content-Type: image/png

... (png binary data) ....
------WebKitFormBoundaryujecLxDFPt6acCab--
```

## 3.3、application/json

application/json作为响应头比较常见，目前也流行在POST请求中使用，以序列化的JSON字符串形式传输，更易于后端解析，可读性更高。

微信小程序中wx.request API默认便是使用此方式传输数据。

## 3.4、application/octet-stream

用于传输二进制数据。可用于上传文件的场景。在 Postman 中，还可以看到 "binary" 这一类型，指的就是一些二进制文件类型。如 `application/pdf`，指定了特定二进制文件的 MIME 类型。就像对于text文件类型若没有特定的子类型（subtype），就使用 `text/plain`。类似的，二进制文件没有特定或已知的 subtype，就使用 `application/octet-stream`，这是应用程序文件的默认值，一般很少直接使用 。

对于 `application/octet-stream`，只能提交二进制，而且只能提交一个二进制，如果提交文件的话，只能提交一个文件，后台接收参数只能有一个，而且只能是流（或者字节数组）。

很多 web 服务器使用默认的 `application/octet-stream` 来发送未知类型。出于一些安全原因，对于这些资源浏览器不允许设置一些自定义默认操作，导致用户必须存储到本地以使用。一般来说，设置正确的MIME类型很重要。

# 4、其他常见值

- 文本：text/plain、text/html、text/css、text/javascript、text/xml
- 图片：image/gif、image/png、image/jpeg
- 视频：video/webm、video/ogg
- 音频：audio/midi、audio/mpeg、audio/webm、audio/ogg、audio/wav
- 二进制：application/octet-stream、application/pdf、application/json

# 5、参考

- [W3C：Form content types](https://link.juejin.cn/?target=https%3A%2F%2Fwww.w3.org%2FTR%2Fhtml401%2Finteract%2Fforms.html%23h-17.13.4.1 "https://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.1")
- [MDN：MIME 类型](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.mozilla.org%2Fzh-CN%2Fdocs%2FWeb%2FHTTP%2FBasics_of_HTTP%2FMIME_types "https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Basics_of_HTTP/MIME_types")
- [cloud.tencent.com/developer/a…](https://link.juejin.cn/?target=https%3A%2F%2Fcloud.tencent.com%2Fdeveloper%2Farticle%2F1162562 "https://cloud.tencent.com/developer/article/1162562")


参考：[https://juejin.cn/post/6959742146781904904](https://juejin.cn/post/6959742146781904904)
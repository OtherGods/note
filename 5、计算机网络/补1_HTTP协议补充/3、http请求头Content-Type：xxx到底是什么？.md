
http发送post请求的时候，需要指定Content-Type，也就是内容类型,大家用得最多的可能就是json类型，其实还有很多别的类型，这些类型在服务端会做自动解析的，所以大家发现数据好像都传对了，为啥服务端没接收成功呢？

Content-Type 家族

- Content-Type: application/x-www-form-urlencoded
- Content-Type: Content-Type: multipart/form-data
- Content-Type: application/json
- Content-Type: text/plain;charset=UTF-8

# **Content-Type: application/x-www-form-urlencoded**

看起来是这样的

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/23215eee98c54a7d89984f24d219b073~tplv-k3u1fbpfcp-zoom-in-crop-mark:3024:0:0:0.awebp?)

只是浏览器默认会解析，点击查看源代码就可以了

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b9191c92f1044c80a3c46337e98933fd~tplv-k3u1fbpfcp-zoom-in-crop-mark:3024:0:0:0.awebp?)

要发送这种格式，普通表单就是这种

html

复制代码

`<form method="post"> <input name="xxx" /> <submit>提交</submit> </form>`

在axios里面发送application/x-www-form-urlencoded格式

js

复制代码

`const params = new URLSearchParams(); params.append('param1', 'value1'); params.append('param2', 'value2'); axios.post('/foo', params);`

# Content-Type: multipart/form-data

在发送请求时，如果需要带文件的时候，就需要这种格式

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4baefc64610f434c96af130bb8ffb4bf~tplv-k3u1fbpfcp-zoom-in-crop-mark:3024:0:0:0.awebp?)

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/05e8031cd0f445d9bc408ae1109d73bb~tplv-k3u1fbpfcp-zoom-in-crop-mark:3024:0:0:0.awebp?)

可以看到浏览器会自动生成boundary，就是分隔符，因为发送的内容都在body里面，需要用分隔符来解析字符串

在axios里面上传文件

js

复制代码

`const form = new FormData(); form.append('my_field', 'my value'); form.append('my_buffer', new Buffer(10)); form.append('my_file', file,filename); //file是文件对象，一般是选择文件返回的 axios.post('https://example.com', form, { headers: form.getHeaders() })`

# Content-Type: application/json

可以看到请求内容就是json字符串

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5e5b70aee1cf4affb82809e951e40af5~tplv-k3u1fbpfcp-zoom-in-crop-mark:3024:0:0:0.awebp?)

axios默认就是json

js

复制代码

`axios.post('https://example.com', {"user":"bb",name:"cc"})`

# Content-Type: text/plain;charset=UTF-8

文本类型，这种比较少，根据情况可以自己设置

总结：Content-Type的作为请求头，表示http的请求的body内容的格式，服务端会依据这个类型**自动做解析**，因此要和服务端沟通清楚，请求类型到底是什么

  



参考：[https://juejin.cn/post/7112270574486814727](https://juejin.cn/post/7112270574486814727)
大家好，我是二哥呀，今天由我来给大家讲一下技术派网站是如何配置 SSL 证书实现 HTTPS 访问的，这几乎是所有网站上线生产环境时必经的一个配置，否则用户访问网站的时候就会提示网站不安全，这将会大大降低用户对网站的信任程度。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052203639.png)

如果已经配置过 SSL 证书且正确的情况下，浏览器的地址栏里会有一个加锁的小图标，点击过去就可以看到一个提示：连接是安全的。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052204665.png)

前置条件：技术派的生产环境是一台腾讯云的香港服务器，操作系统是腾讯 OS，Web 服务器用的 Nginx，项目是以 jar 包的形式运行的。

# 1、什么是SSL

SSL，也就是 Secure Sockets Layer，中文名叫做安全套接字层，是一种加密安全协议，最初由网景公司于 1995 年开发，旨在保护网络通信中的隐私、身份验证和数据完整性。

算是 TLS 的前身。

TLS，也就是 Transport Layer Security，中文名叫做传输层安全性协议，其实和 SSL 是一回事，因为 TLS 1.0 版就是在 SSL 3.1 的版本上开发的，但在发布前更改了名字，以表明它不再和网景公司有任何关系。

换句话说，有个特工 30 岁以前一直叫王二，30 岁后觉得自己牛气哄哄，成就非凡，不想再叫这么中二的名字了，于是改名叫王三。但认识他的人还是喜欢叫他王二（dog）。

# 2、什么是HTTPS

那知道 SSL 的定义后，也就能明白它的作用了，就是为了对原来的明文 HTTP 请求进行加密，保证通信之间的数据安全。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052205439.png)

那什么是 HTTP，想必大家都已经清楚，也就是 HyperText Transfer Protocol，中文名叫做超文本传输协议，是互联网进行数据传输的基础。

在 HTTP 协议下，客户端和服务端之间的通信都是明文的，赤裸裸的。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052205055.png)

HTTPS（HyperText Transfer Protocol Secure）算是 HTTP 的安全版本，这个加密的工作就由 SSL 来完成，这次发送的内容就不知道是什么了，只有客户端和服务器端能懂的鸟语。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052206302.png)

# 3、SSL证书的工作原理

知道什么是 SSL 后，SSL 证书也就知道了。SSL 就相当于前文提到的那个王二特工，SSL 证书就像王二的签证或者护照。

SSL 证书的作用相当于这些签证或者护照，不仅能提供加密，还能够验证身份来增强通信的安全性。

**简单说一下 SSL 证书的工作原理：**

第一步，身份验证。

SSL 证书由受信任的证书颁发机构（CA）签发，它包含证书持有者（服务器）的信息，客户端（浏览器）信任这些 CA，并会验证 SSL 的真实性。

第二步，建立加密连接，也就是 SSL 握手🤝

- 客户端开始握手，发送包含支持的 TLS 版本、密码套件列表以及“客户端随机数”的消息。
- 服务器回应客户端，发送包含服务器的 SSL 证书、服务器选择的密码套件以及“服务器随机数”的消息。
- 客户端验证服务器证书的有效性（是否是信任的 CA 签发，是否过期，是否用于请求的域名等）
- 客户端生成“预主密钥”并使用服务器的公钥（从服务器的 SSL 证书中获取）进行加密，然后发送给服务器。
- 服务器使用其私钥对接收到的加密的“预主密钥”进行解密。
- 客户端和服务器都使用“客户端随机数”、“服务器随机数”和“预主密钥”生成会话密钥。这确保了双方都拥有相同的密钥。
- 客户端发送一条“已完成”消息，该消息使用会话密钥加密。
- 服务器回应一条同样使用会话密钥加密的“已完成”消息。

第三步，对称加密和非对称加密的结合。
- 握手阶段使用非对称加密（公钥和私钥）进行安全密钥交换。
- 之后的通信使用对称加密。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052211298.png)

# 4、如何申请SSL证书

搞清楚 SSL 证书的工作原理后，我们来看一下如何申请 SSL 证书，提供类似服务的平台有很多，二哥这里推荐大家使用 freessl，支持一年免费。
[https://freessl.cn/](https://freessl.cn/)

这个网站做的还是非常友好的，我自己一直在用，像技术派、二哥的 Java 进阶之路，都用的 freessl 申请的。作者我也认识，所以很放心。![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052212670.png)

第一步，进入该网站，填写域名（可点选自动补全，一般就是 paicoding.com和www.paicoding.com），选择品牌「亚洲诚信双域名一年期」，点击「创建免费的 SSL 证书」。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052212638.png)

三年自动化续期的现在是 59.9 元，如果想省钱的话，其实一年到期后自己重新申请就可以了，我主要是为了支持作者，所以技术派的域名就选了五年自动续期，我之前的所有域名都用了 freessl，所以这波不亏。

第二步，选择证书的生成方式，点击「创建」。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052213279.png)

我之前选择的是「RSA」「DNS」「离线生成」，这里简单给大家普及一下都是干嘛的。
- 证书类型为 RSA，ECC 是一种较新的公钥加密技术，基于椭圆曲线数学加密，感兴趣可以去搜一下，RSA 是最早也是最广泛的公钥加密算法，兼容性比较好。
- 验证方式为 DNS，相比较「文件验证 HTTP」，DNS 只需要在域名控制台进行解析就能完成 CA 的验证操作，不需要配置服务器，更加方便（大约一分钟就可以验证成功）。
- CSR 为「离线生成」，这种方式需要提前安装好 KeyManager，当然如果没有安装的话，会自动提示你安装，方便我们在本地通过「KeyManager」来管理证书。

如果已经安装了 KeyManager，也可以在 KeyManager 中登录 freessl 的账号，然后直接在这里申请证书。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052214595.png)

之前还没有「一键申请」，只有「离线生成」、「浏览器生成」、「我有 CSR」这三种，据说一键申请更加方便，大家可以尝试一下，不需要域名解析，比离线生成更加方便。

「浏览器生成」的话，我之前也试过，忘记了，这里不再推荐；

「我有 CSR」的话，针对之前生成过证书，又重新申请的情况，我没有试过，后面等技术派现在的五年证书过期后可以试一下。

这里再解释一下 CSR，也就是 Certificate Signing Request，中文名叫做「证书签名请求」，里面包含了申请证书的组织信息，以及公钥，这些信息将会包含在最终签发的证书中。

CSR 是申请和安装 SSL/TLS 证书过程中的关键组成部分，保证了证书中的公钥与申请者的私钥相对应，并且提供了必要的身份验证信息。

第三步，颁发证书，技术派已经颁发过了，所以，我直接给出最终的订单详情，并且把注意事项告诉大家。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052215882.png)

经过上一步的操作后，会进入到 51SSL 的订单管理页面，见上图。

我们需要域名验证，把这个验证信息在域名解析控制台添加进去。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052215572.png)

如下所示。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052215292.png)

添加完，稍等 1 分钟，刷新 51SSL 的页面，等待证书签发，签发完成后，在 KeyManager 中可以看到当前办法的 SSL 证书。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052216651.png)

点击「更多」选择「导出证书」，技术派用的 Nginx Web 服务器，所以导出格式是 pem 的。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052216465.png)

注：PEM（Privacy-Enhanced Mail）是一种用于存储和发送加密信息的文件格式。以前是为电子邮件设计的，现在也用于 SSL/TLS 证书。纯文本格式，可以用任何文本编辑器打开，文件内包含明确的开始/结束标记，可能以 -----BEGIN CERTIFICATE----- 开始，以 -----END CERTIFICATE----- 结束。 扩展名可以是 .pem、.crt、.cer 或 .key 等。

导出后是 zip 压缩包，解压有两个文件，一个是 crt 后缀，一个是 key 后缀。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052217700.png)


其中 .key 结尾的是私钥，.crt 的是证书链，这俩文件稍后会在 Nginx 中进行配置。

# 5、如何配置SSL证书

OK，有了 SSL 证书后，接下来要做的工作就是把证书上传到服务器。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052217447.png)

.crt 文件中一般包含三块内容，都 -----BEGIN CERTIFICATE----- 开始，以 -----END CERTIFICATE----- 结束。

第一块是证书（End-entity Certificate），第二块是中间证书（Intermediate Certificate），第三块是根证书（Root Certificate），三块内容共同构成了公钥的证书链（Certificate Chain），提供了一条从信任的根证书到最终目标证书的信任路径。![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052218056.png)

- 证书由中间证书颁发机构签名，包含服务器的公钥和主体信息，如域名。
- 中间证书帮助建立从根证书到最终实体证书的信任链，由根证书颁发机构签发。
- 根证书由根证书颁发机构（CA）自签名，通常预装在操作系统或浏览器中。

我把技术派在 Nginx 中的完整配置摘出来，大家可以参考一下。

对 Nginx 完全没有了解的球友先看这篇：[https://javabetter.cn/nginx/nginx.html](https://javabetter.cn/nginx/nginx.html)

```nginx

server {
    listen       80;
    server_name  paicoding.com  www.paicoding.com;
    return       301 https://$host$request_uri;
}

server {
    listen       443 ssl;
    server_name  paicoding.com www.paicoding.com;


    ssl_certificate      /etc/nginx/ssl/paicoding.com_chain.crt;
    ssl_certificate_key  /etc/nginx/ssl/paicoding.com_key.key;
    
    ssl_session_cache    shared:SSL:1m;
    ssl_session_timeout  5m;

    ssl_ciphers  HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers  on;
    
    root   /home/admin/workspace/paicoding/paicoding-web/target/classes/static;
    location / {
        proxy_next_upstream error timeout http_502 http_504;
        proxy_set_header X-real-ip  $remote_addr;
        proxy_pass http://127.0.0.1:8080/;
        proxy_redirect default;
        proxy_intercept_errors on;
    }
    # 开启502页面
    error_page  502 503 504 /error.html;
    location = /error.html {
        root   /home/admin/workspace/html;
    }
    # 支持wss websocket连接
    location /gpt {
        proxy_set_header  Host $host;
        proxy_set_header  X-Real-IP  $remote_addr;
        proxy_set_header  X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header  X-Forwarded-Proto   $scheme;
        
        proxy_pass http://127.0.0.1:8080/gpt;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
    
    location ^~ /admin-view {
        alias /home/admin/workspace/admin/dist/; # 后台页面
        index index.html;
    }
    location ^~ /admin {
        alias /home/admin/workspace/admin/dist/; # 后台页面
        index index.html;
    }
    
    location ~* ^.+\.(css|js|txt|xml|swf|wav|pptx)$ {
        access_log   off;
        expires      10m;
        proxy_pass         http://paicoding_host;
        
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For  $proxy_add_x_forwarded_for;
    }
    
    location ~* ^.+\.(ico|gif|jpg|jpeg|png)$ {
        access_log   off;
        expires      1d;
        proxy_pass         http://paicoding_host;
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For  $proxy_add_x_forwarded_for;
    }
}

```

第一块 server 是为了配置 80 端口，也就是 HTTP 访问，直接跳转到 HTTPS，很好理解。
```nginx
server {
    listen       80;
    server_name  paicoding.com  www.paicoding.com;
    return       301 https://$host$request_uri;
}
```

第二块 server 是为了配置 443 端口，也就是 HTTPS 访问，这部分内容比较多，我们慢慢来拆解。
```nginx
# 监听 443 端口，启用 SSL
listen       443 ssl;
# 定义服务器名称，支持 paicoding.com 和 www.paicoding.com
server_name  paicoding.com www.paicoding.com;

# 指定 SSL 证书和私钥的位置
ssl_certificate      /etc/nginx/ssl/paicoding.com_chain.crt;
ssl_certificate_key  /etc/nginx/ssl/paicoding.com_key.key;

# SSL 会话缓存设置，用于提高性能
ssl_session_cache    shared:SSL:1m;  # 为 SSL 会话缓存分配 1MB 的共享内存
ssl_session_timeout  5m;  # SSL 会话的超时时间设置为 5 分钟

# 定义 SSL 加密套件
ssl_ciphers  HIGH:!aNULL:!MD5;  # 使用高强度的加密套件，排除空认证和 MD5
ssl_prefer_server_ciphers  on;  # 优先使用服务器指定的加密套件
```

这部分主要就是为了配置 SSL 证书，
- ssl_certificate 指向包含服务器的证书链。
- ssl_certificate_key 指向私钥文件。
- ssl_session_cache 用于缓存 SSL 会话信息，以加快后续请求的处理速度。
- ssl_session_timeout 设置会话的有效期。
- ssl_ciphers 定义了允许的加密套件列表。这里选择了高强度的加密，并排除了不安全的选项（如空认证和 MD5）。
- ssl_prefer_server_ciphers 设置为 on，意味着 Nginx 将优先选择服务器配置的加密套件，而不是客户端提供的套件。
```nginx
# 设置静态资源的根目录
root   /home/admin/workspace/paicoding/paicoding-web/target/classes/static;

# 配置 / 路径的处理
location / {
	# 当出现以下错误时尝试下一个服务器：网络错误、超时、502 Bad Gateway 和 504 Gateway Timeout
	proxy_next_upstream error timeout http_502 http_504;
	
	# 设置请求头部，传递真实的客户端 IP 地址
	proxy_set_header X-real-ip  $remote_addr;
	
	# 将请求代理到本地的 8080 端口
	proxy_pass http://127.0.0.1:8080/;
	
	# 使用默认的代理重定向行为
	proxy_redirect default;
	
	# 当代理服务器返回错误时，允许 Nginx 处理这些错误
	proxy_intercept_errors on;
}
# 定义 502、503 和 504 错误时显示的错误页面
error_page  502 503 504 /error.html;

# 配置 error.html 页面的路径
location = /error.html {
	root   /home/admin/workspace/html;
}
```

- root 设置了静态资源的根目录，Nginx 会在此目录下查找并提供静态资源。
- 所有访问域名的请求都会被代理到本地的 8080 端口，也就是技术派的后台服务，就相当于我们本地跑的服务 http://127.0.0.1:8080
- 其他的看注释就明白了。

```nginx
# 支持wss websocket连接
location /gpt {
	# 设置请求头部，以确保正确的主机名和客户端 IP 被传递给后端服务
	proxy_set_header  Host $host;
	proxy_set_header  X-Real-IP  $remote_addr;
	proxy_set_header  X-Forwarded-For $proxy_add_x_forwarded_for;
	proxy_set_header  X-Forwarded-Proto   $scheme;
	
	# 将请求代理到本地的 8080 端口的 /gpt 路径
	proxy_pass http://127.0.0.1:8080/gpt;
	
	# 设置 HTTP 版本为 1.1，这对于 WebSocket 是必需的
	proxy_http_version 1.1;
	
	# 设置升级请求头，用于升级为 WebSocket 协议
	proxy_set_header Upgrade $http_upgrade;
	proxy_set_header Connection "upgrade";
}

# 配置 /admin-view 和 /admin 路径
location ^~ /admin-view {
	# 设置后台页面的路径
	alias /home/admin/workspace/admin/dist/; # 后台页面
	index index.html;
}
location ^~ /admin {
	# 同样设置后台页面的路径
	alias /home/admin/workspace/admin/dist/; # 后台页面
	index index.html;
}

# 为静态资源（如 CSS、JS、文本文件等）配置反向代理
location ~* ^.+\.(css|js|txt|xml|swf|wav|pptx)$ {
	# 关闭访问日志
	access_log   off;
	
	# 设置缓存过期时间为 10 分钟
	expires      10m;
	
	# 反向代理到 paicoding_host
	proxy_pass         http://paicoding_host;
	proxy_set_header   Host $host;
	proxy_set_header   X-Real-IP $remote_addr;
	proxy_set_header   X-Forwarded-For  $proxy_add_x_forwarded_for;
}

# 为图片资源（ico, gif, jpg, jpeg, png）配置反向代理
location ~* ^.+\.(ico|gif|jpg|jpeg|png)$ {
	# 关闭访问日志
	access_log   off;
	
	# 设置缓存过期时间为 1 天
	expires      1d;
	
	# 反向代理到 paicoding_host
	proxy_pass         http://paicoding_host;
	proxy_set_header   Host $host;
	proxy_set_header   X-Real-IP $remote_addr;
	proxy_set_header   X-Forwarded-For  $proxy_add_x_forwarded_for;
}
```

- 配置了 /gpt 路径来支持 WebSocket 连接，技术派这里使用了 WebSocket 来提供 GPT 的服务，目前配置有讯飞星火和 OpenAI 双通道。大家可以通过 https://paicoding.com/chat 这个链接体验。
- 配置了 /admin-view 和 /admin 路径，用于提供后台管理页面，页面文件位于 /home/admin/workspace/admin/dist/ 目录下。大家可以通过 https://paicoding.com/admin/ 来体验。
- 对于特定的文件扩展名（如 css、js、txt 等静态资源），配置了反向代理，并设置了访问日志关闭以及缓存过期时间。
- 对图片资源也配置了反向代理，如果有多台服务器，或者 CDN 服务的话，反向代理将会大大提升服务的访问性能。

paicoding_host 的定义如下：
```nginx
# 定义一个名为 paicoding_host 的 upstream（服务器组）
upstream  paicoding_host {
    # 定义组内的服务器。这里只有一个服务器：运行在本地的 8080 端口
    server 127.0.0.1:8080;
}
```

技术派的静态资源其实就用了阿里云的 OSS + CDN，所以大家在访问技术派的时候，会感觉速度还是挺不错的。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052243559.png)

# 6、如何验证SSL证书

当在 Nginx 中配置好 SSL 证书后，就可以执行以下命令让 Nginx 重载，重载后的 Nginx 配置就会起效。
```nginx
nginx -s reload
```

可以使用 nginx -t 来验证配置文件是否生效。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052245284.png)

如果出现 ok 或者 successful 就说明配置生效了。

新开一个无痕模式的浏览器窗口，访问技术派的网址 https://paicoding.com/

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052246829.png)

如果地址栏中出现了一把小锁的标记，点开它，如果能看到以下提示，就说明 SSL 证书已经起效了。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052246910.png)

查看证书详情的话，也可以看到对应的证书有效期。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404052247050.png)

# 7、小结

通过这篇内容，我们不仅了解了 SSL 证书的配置，还了解了它的工作原理，涉及到了不少计算机网络的知识，也是面试中经常会被问到的，比如说 HTTPS 和 HTTP 之间的区别。

区别就在于这个 SSL/TSL 证书上，希望大家都能彻底掌握这方面的内容，冲。



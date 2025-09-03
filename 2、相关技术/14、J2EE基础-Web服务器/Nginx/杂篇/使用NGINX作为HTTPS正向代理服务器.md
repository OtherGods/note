
简介： NGINX主要设计作为反向代理服务器，但随着NGINX的发展，它同样能作为正向代理的选项之一。正向代理本身并不复杂，而如何代理加密的HTTPS流量是正向代理需要解决的主要问题。本文将介绍利用NGINX来正向代理HTTPS流量两种方案，及其使用场景和主要问题。


# HTTP/HTTPS正向代理的分类

简单介绍下正向代理的分类作为理解下文的背景知识：

## 按客户端有无感知的分类

- **普通代理**：在客户端需要在浏览器中或者系统环境变量手动设置代理的地址和端口。如squid，在客户端指定squid服务器IP和端口3128。
- **透明代理**：客户端不需要做任何代理设置，“代理”这个角色对于客户端是透明的。如企业网络链路中的Web Gateway设备。

## 按代理是否解密HTTPS的分类

- **隧道代理** ：也就是透传代理。代理服务器只是在TCP协议上透传HTTPS流量，对于其代理的流量的具体内容不解密不感知。客户端和其访问的目的服务器做直接TLS/SSL交互。本文中讨论的NGINX代理方式属于这种模式。
- **中间人(MITM, Man-in-the-Middle)代理**：代理服务器解密HTTPS流量，对客户端利用自签名证书完成TLS/SSL握手，对目的服务器端完成正常TLS交互。在客户端-代理-服务器的链路中建立两段TLS/SSL会话。如[Charles](https://www.charlesproxy.com/)，简单原理描述可以参考[文章](https://www.jianshu.com/p/405f9d76f8c4)。  
    注：这种情况客户端在TLS握手阶段实际上是拿到的代理服务器自己的自签名证书，证书链的验证默认不成功，需要在客户端信任代理自签证书的Root CA证书。所以过程中是客户端有感的。如果要做成无感的透明代理，需要向客户端推送自签的Root CA证书，在企业内部环境下是可实现的。

# 为什么正向代理处理HTTPS流量需要特殊处理？

作为反向代理时，代理服务器通常终结 (terminate) HTTPS加密流量，再转发给后端实例。HTTPS流量的加解密和认证过程发生在客户端和反向代理服务器之间。

而作为正向代理在处理客户端发过来的流量时，HTTP加密封装在了TLS/SSL中，代理服务器无法看到客户端请求URL中想要访问的域名，如下图。所以代理HTTPS流量，相比于HTTP，需要做一些特殊处理。  
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403112210065.png)


# NGINX的解决方案

根据前文中的分类方式，NGINX解决HTTPS代理的方式都属于透传(隧道)模式，即不解密不感知上层流量。具体的方式有如下7层和4层的两类解决方案。

## HTTP CONNECT隧道 (7层解决方案)

### 历史背景

早在1998年，也就是TLS还没有正式诞生的SSL时代，主导SSL协议的Netscape公司就提出了关于利用web代理来tunneling SSL流量的[INTERNET-DRAFT](https://developer.aliyun.com/article/Tunneling)。其核心思想就是利用HTTP CONNECT请求在客户端和代理之间建立一个HTTP CONNECT Tunnel，在CONNECT请求中需要指定客户端需要访问的目的主机和端口。Draft中的原图如下：  
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403112210014.png)


整个过程可以参考HTTP权威指南中的图：

1. 客户端给代理服务器发送HTTP CONNECT请求。
2. 代理服务器利用HTTP CONNECT请求中的主机和端口与目的服务器建立TCP连接。
3. 代理服务器给客户端返回HTTP 200响应。
4. 客户端和代理服务器建立起HTTP CONNECT隧道，HTTPS流量到达代理服务器后，直接通过TCP透传给远端目的服务器。代理服务器的角色是透传HTTPS流量，并不需要解密HTTPS。  
    ![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403112211619.png)


### NGINX ngx_http_proxy_connect_module模块

NGINX作为反向代理服务器，官方一直没有支持HTTP CONNECT方法。但是基于NGINX的模块化、可扩展性好的特性，阿里的@chobits提供了[ngx_http_proxy_connect_module](https://github.com/chobits/ngx_http_proxy_connect_module)模块，来支持HTTP CONNECT方法，从而让NGINX可以扩展为正向代理。

### 环境搭建

以CentOS 7的环境为例。

1) 安装  
对于新安装的环境，参考正常的[安装步骤](https://www.cnblogs.com/stulzq/p/9291223.html)和安装这个模块的步骤([https://github.com/chobits/ngx_http_proxy_connect_module)](https://github.com/chobits/ngx_http_proxy_connect_module))，把对应版本的patch打上之后，在configure的时候加上参数--add-module=/path/to/ngx_http_proxy_connect_module，示例如下：

```
./configure \
--user=www \
--group=www \
--prefix=/usr/local/nginx \
--with-http_ssl_module \
--with-http_stub_status_module \
--with-http_realip_module \
--with-threads \
--add-module=/root/src/ngx_http_proxy_connect_module
```

对于已经安装编译安装完的环境，需要加入以上模块，步骤如下：

```
# 停止NGINX服务
# systemctl stop nginx
# 备份原执行文件
# cp /usr/local/nginx/sbin/nginx /usr/local/nginx/sbin/nginx.bak
# 在源代码路径重新编译
# cd /usr/local/src/nginx-1.16.0
./configure \
--user=www \
--group=www \
--prefix=/usr/local/nginx \
--with-http_ssl_module \
--with-http_stub_status_module \
--with-http_realip_module \
--with-threads \
--add-module=/root/src/ngx_http_proxy_connect_module
# make
# 不要make install
# 将新生成的可执行文件拷贝覆盖原来的nginx执行文件
# cp objs/nginx /usr/local/nginx/sbin/nginx
# /usr/bin/nginx -V
nginx version: nginx/1.16.0
built by gcc 4.8.5 20150623 (Red Hat 4.8.5-36) (GCC)
built with OpenSSL 1.0.2k-fips  26 Jan 2017
TLS SNI support enabled
configure arguments: --user=www --group=www --prefix=/usr/local/nginx --with-http_ssl_module --with-http_stub_status_module --with-http_realip_module --with-threads --add-module=/root/src/ngx_http_proxy_connect_module
```

2) nginx.conf文件配置

```
server {
     listen  443;
    
     # dns resolver used by forward proxying
     resolver  114.114.114.114;

     # forward proxy for CONNECT request
     proxy_connect;
     proxy_connect_allow            443;
     proxy_connect_connect_timeout  10s;
     proxy_connect_read_timeout     10s;
     proxy_connect_send_timeout     10s;

     # forward proxy for non-CONNECT request
     location / {
         proxy_pass http://$host;
         proxy_set_header Host $host;
     }
 }
```

### 使用场景

7层需要通过HTTP CONNECT来建立隧道，属于客户端有感知的普通代理方式，需要在客户端手动配置HTTP(S)代理服务器IP和端口。在客户端用curl 加-x参数访问如下：

```
# curl https://www.baidu.com -svo /dev/null -x 39.105.196.164:443
* About to connect() to proxy 39.105.196.164 port 443 (#0)
*   Trying 39.105.196.164...
* Connected to 39.105.196.164 (39.105.196.164) port 443 (#0)
* Establish HTTP proxy tunnel to www.baidu.com:443
> CONNECT www.baidu.com:443 HTTP/1.1
> Host: www.baidu.com:443
> User-Agent: curl/7.29.0
> Proxy-Connection: Keep-Alive
>
< HTTP/1.1 200 Connection Established
< Proxy-agent: nginx
<
* Proxy replied OK to CONNECT request
* Initializing NSS with certpath: sql:/etc/pki/nssdb
*   CAfile: /etc/pki/tls/certs/ca-bundle.crt
  CApath: none
* SSL connection using TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
* Server certificate:
*     subject: CN=baidu.com,O="Beijing Baidu Netcom Science Technology Co., Ltd",OU=service operation department,L=beijing,ST=beijing,C=CN
...
> GET / HTTP/1.1
> User-Agent: curl/7.29.0
> Host: www.baidu.com
> Accept: */*
>
< HTTP/1.1 200 OK
...
{ [data not shown]
```

从上面-v参数打印出的细节，可以看到客户端先往代理服务器39.105.196.164建立了HTTP CONNECT隧道，代理回复HTTP/1.1 200 Connection Established后就开始交互TLS/SSL握手和流量了。

## NGINX stream (4层解决方案)

既然是使用透传上层流量的方法，那可不可做成“4层代理”，对TCP/UDP以上的协议实现彻底的透传呢？答案是可以的。NGINX官方从1.9.0版本开始支持[ngx_stream_core_module](http://nginx.org/en/docs/stream/ngx_stream_core_module.html)模块，模块默认不build，需要configure时加上--with-stream选项来开启。

### 问题

用NGINX stream在TCP层面上代理HTTPS流量肯定会遇到本文一开始提到的那个问题：代理服务器无法获取客户端想要访问的目的域名。因为在TCP的层面获取的信息仅限于IP和端口层面，没有任何机会拿到域名信息。要拿到目的域名，必须要有拆上层报文获取域名信息的能力，所以NGINX stream的方式不是完全严格意义上的4层代理，还是要略微借助些上层能力。

### ngx_stream_ssl_preread_module模块

要在不解密的情况下拿到HTTPS流量访问的域名，只有利用TLS/SSL握手的第一个Client Hello报文中的扩展地址SNI (Server Name Indication)来获取。NGINX官方从1.11.5版本开始支持利用[ngx_stream_ssl_preread_module](http://nginx.org/en/docs/stream/ngx_stream_ssl_preread_module.html)模块来获得这个能力，模块主要用于获取Client Hello报文中的SNI和ALPN信息。对于4层正向代理来说，从Client Hello报文中提取SNI的能力是至关重要的，否则NGINX stream的解决方案无法成立。同时这也带来了一个限制，要求所有客户端都需要在TLS/SSL握手中带上SNI字段，否则NGINX stream代理完全没办法知道客户端需要访问的目的域名。

### 环境搭建

1) 安装  
对于新安装的环境，参考正常的[安装步骤](https://www.cnblogs.com/stulzq/p/9291223.html)，直接在configure的时候加上--with-stream，--with-stream_ssl_preread_module和--with-stream_ssl_module选项即可。示例如下：

```
./configure \
--user=www \
--group=www \
--prefix=/usr/local/nginx \
--with-http_ssl_module \
--with-http_stub_status_module \
--with-http_realip_module \
--with-threads \
--with-stream \
--with-stream_ssl_preread_module \
--with-stream_ssl_module
```

对于已经安装编译安装完的环境，需要加入以上3个与stream相关的模块，步骤如下：

```
# 停止NGINX服务
# systemctl stop nginx
# 备份原执行文件
# cp /usr/local/nginx/sbin/nginx /usr/local/nginx/sbin/nginx.bak
# 在源代码路径重新编译
# cd /usr/local/src/nginx-1.16.0
# ./configure \
--user=www \
--group=www \
--prefix=/usr/local/nginx \
--with-http_ssl_module \
--with-http_stub_status_module \
--with-http_realip_module \
--with-threads \
--with-stream \
--with-stream_ssl_preread_module \
--with-stream_ssl_module
# make
# 不要make install
# 将新生成的可执行文件拷贝覆盖原来的nginx执行文件
# cp objs/nginx /usr/local/nginx/sbin/nginx
# nginx -V
nginx version: nginx/1.16.0
built by gcc 4.8.5 20150623 (Red Hat 4.8.5-36) (GCC)
built with OpenSSL 1.0.2k-fips  26 Jan 2017
TLS SNI support enabled
configure arguments: --user=www --group=www --prefix=/usr/local/nginx --with-http_ssl_module --with-http_stub_status_module --with-http_realip_module --with-threads --with-stream --with-stream_ssl_preread_module --with-stream_ssl_module
```

2) nginx.conf文件配置  
NGINX stream与HTTP不同，需要在stream块中进行配置，但是指令参数与HTTP块都是类似的，主要配置部分如下：

```
stream {
    resolver 114.114.114.114;
    server {
        listen 443;
        ssl_preread on;
        proxy_connect_timeout 5s;
        proxy_pass $ssl_preread_server_name:$server_port;
    }
}
```

### 使用场景

对于4层正向代理，NGINX对上层流量基本上是透传，也不需要HTTP CONNECT来建立隧道。适合于透明代理的模式，比如将访问的域名利用DNS解定向到代理服务器。我们可以通过在客户端绑定/etc/hosts来模拟。

在客户端：

```
cat /etc/hosts
...
# 把域名www.baidu.com绑定到正向代理服务器39.105.196.164
39.105.196.164 www.baidu.com

# 正常利用curl来访问www.baidu.com即可。
# curl https://www.baidu.com -svo /dev/null
* About to connect() to www.baidu.com port 443 (#0)
*   Trying 39.105.196.164...
* Connected to www.baidu.com (39.105.196.164) port 443 (#0)
* Initializing NSS with certpath: sql:/etc/pki/nssdb
*   CAfile: /etc/pki/tls/certs/ca-bundle.crt
  CApath: none
* SSL connection using TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
* Server certificate:
*     subject: CN=baidu.com,O="Beijing Baidu Netcom Science Technology Co., Ltd",OU=service operation department,L=beijing,ST=beijing,C=CN
*     start date: 5月 09 01:22:02 2019 GMT
*     expire date: 6月 25 05:31:02 2020 GMT
*     common name: baidu.com
*     issuer: CN=GlobalSign Organization Validation CA - SHA256 - G2,O=GlobalSign nv-sa,C=BE
> GET / HTTP/1.1
> User-Agent: curl/7.29.0
> Host: www.baidu.com
> Accept: */*
>
< HTTP/1.1 200 OK
< Accept-Ranges: bytes
< Cache-Control: private, no-cache, no-store, proxy-revalidate, no-transform
< Connection: Keep-Alive
< Content-Length: 2443
< Content-Type: text/html
< Date: Fri, 21 Jun 2019 05:46:07 GMT
< Etag: "5886041d-98b"
< Last-Modified: Mon, 23 Jan 2017 13:24:45 GMT
< Pragma: no-cache
< Server: bfe/1.0.8.18
< Set-Cookie: BDORZ=27315; max-age=86400; domain=.baidu.com; path=/
<
{ [data not shown]
* Connection #0 to host www.baidu.com left intact
```

### 常见问题

1) 客户端手动设置代理导致访问不成功  
4层正向代理是透传上层HTTPS流量，不需要HTTP CONNECT来建立隧道，也就是说不需要客户端设置HTTP(S)代理。如果我们在客户端手动设置HTTP(s)代理是否能访问成功呢? 我们可以用curl -x来设置代理为这个正向服务器访问测试，看看结果：

```
# curl https://www.baidu.com -svo /dev/null -x 39.105.196.164:443
* About to connect() to proxy 39.105.196.164 port 443 (#0)
*   Trying 39.105.196.164...
* Connected to 39.105.196.164 (39.105.196.164) port 443 (#0)
* Establish HTTP proxy tunnel to www.baidu.com:443
> CONNECT www.baidu.com:443 HTTP/1.1
> Host: www.baidu.com:443
> User-Agent: curl/7.29.0
> Proxy-Connection: Keep-Alive
>
* Proxy CONNECT aborted
* Connection #0 to host 39.105.196.164 left intact
```

可以看到客户端试图于正向NGINX前建立HTTP CONNECT tunnel，但是由于NGINX是透传，所以把CONNECT请求直接转发给了目的服务器。目的服务器不接受CONNECT方法，所以最终出现"Proxy CONNECT aborted"，导致访问不成功。

2) 客户端没有带SNI导致访问不成功  
上文提到用NGINX stream做正向代理的关键因素之一是利用ngx_stream_ssl_preread_module提取出Client Hello中的SNI字段。如果客户端客户端不携带SNI字段，会造成代理服务器无法获知目的域名的情况，导致访问不成功。

在透明代理模式下(用手动绑定hosts的方式模拟)，我们可以在客户端用openssl来模拟：

```
# openssl s_client -connect www.baidu.com:443 -msg
CONNECTED(00000003)
>>> TLS 1.2  [length 0005]
    16 03 01 01 1c
>>> TLS 1.2 Handshake [length 011c], ClientHello
    01 00 01 18 03 03 6b 2e 75 86 52 6c d5 a5 80 d7
    a4 61 65 6d 72 53 33 fb 33 f0 43 a3 aa c2 4a e3
    47 84 9f 69 8b d6 00 00 ac c0 30 c0 2c c0 28 c0
    24 c0 14 c0 0a 00 a5 00 a3 00 a1 00 9f 00 6b 00
    6a 00 69 00 68 00 39 00 38 00 37 00 36 00 88 00
    87 00 86 00 85 c0 32 c0 2e c0 2a c0 26 c0 0f c0
    05 00 9d 00 3d 00 35 00 84 c0 2f c0 2b c0 27 c0
    23 c0 13 c0 09 00 a4 00 a2 00 a0 00 9e 00 67 00
    40 00 3f 00 3e 00 33 00 32 00 31 00 30 00 9a 00
    99 00 98 00 97 00 45 00 44 00 43 00 42 c0 31 c0
    2d c0 29 c0 25 c0 0e c0 04 00 9c 00 3c 00 2f 00
    96 00 41 c0 12 c0 08 00 16 00 13 00 10 00 0d c0
    0d c0 03 00 0a 00 07 c0 11 c0 07 c0 0c c0 02 00
    05 00 04 00 ff 01 00 00 43 00 0b 00 04 03 00 01
    02 00 0a 00 0a 00 08 00 17 00 19 00 18 00 16 00
    23 00 00 00 0d 00 20 00 1e 06 01 06 02 06 03 05
    01 05 02 05 03 04 01 04 02 04 03 03 01 03 02 03
    03 02 01 02 02 02 03 00 0f 00 01 01
140285606590352:error:140790E5:SSL routines:ssl23_write:ssl handshake failure:s23_lib.c:177:
---
no peer certificate available
---
No client certificate CA names sent
---
SSL handshake has read 0 bytes and written 289 bytes
...
```

openssl s_client默认不带SNI，可以看到上面的请求在TLS/SSL握手阶段，发出Client Hello后就结束了。因为代理服务器不知道要把Client Hello往哪个目的域名转发。

如果用openssl带servername参数来指定SNI，则可以正常访问成功，命令如下：

```
# openssl s_client -connect www.baidu.com:443 -servername www.baidu.com
```

# 总结

本文总结了NGINX利用HTTP CONNECT隧道和NGINX stream两种方式做HTTPS正向代理的原理，环境搭建，使用场景和主要问题，希望给大家在做各种场景的正向代理时提供参考。


转载自：[https://developer.aliyun.com/article/706196](https://developer.aliyun.com/article/706196)


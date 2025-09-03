upstream_check_module模块可以用来检测后端服务的健康状态，如果后端[服务器](https://cloud.tencent.com/act/pro/promotion-cvm?from_column=20065&from=20065)不可用，则所有的请求不转发到这台服务器 upstream_check_module模块是第三方模块，并不是Nginx提供的 模块地址 [https://github.com/yaoweibin/nginx_upstream_check_module](https://cloud.tencent.com/developer/tools/blog-entry?target=https%3A%2F%2Fgithub.com%2Fyaoweibin%2Fnginx_upstream_check_module&source=article&objectId=1700001)

### 环境

```javascript
172.16.0.132  #代理服务器
172.16.0.1  #后端WEB服务器
172.16.0.186  #后端WEB服务器
```

### 安装依赖

```javascript
yum install -y pcre pcre-devel openssl openssl-devel patch libxml2 libxml2-dev libxslt-devel gd gd-devel perl-devel perl-ExtUtils-Embed gperftools
```

### 克隆项目到本地

```javascript
git clone https://github.com/yaoweibin/nginx_upstream_check_module.git
```

### 下载Nginx源码包

```javascript
wget http://nginx.org/download/nginx-1.7.5.tar.gz
tar zxf nginx-1.7.5.tar.gz
cd nginx-1.7.5
```

### Nginx 打补丁

选择相对应Nginx版本的补丁

```javascript
patch -p1 < ../nginx_upstream_check_module/check_1.7.5+.patch 
```

### 编译安装

```javascript
./configure --prefix=/usr/local/nginx --add-module=/nginx_upstream_check_module --with-http_ssl_module --with-http_gzip_static_module --with-http_stub_status_module
make
make install  #如果没安装过Nginx就执行这条命令

#如果安装过Nginx就执行下面的命令
cp /usr/local/nginx/sbin/nginx{,.bak}  #先备份
cp objs/nginx /usr/local/nginx/sbin/nginx
```

### 编辑配置文件

```javascript
vim /usr/local/nginx/conf.d/www.conf 
upstream web{
  server 172.16.0.1;
  server 172.16.0.186;
  check interval=3000 rise=2 fall=3 timeout=1000 type=http default_down=true port=80;
}
server {
  listen 80;
  server_name 172.16.0.132;

location / {
  proxy_pass http://web;
}
location /status {
  check_status;
}
}
```

#### 参数解释

```javascript
interval=3000  #检测间隔时间，单位为毫秒
rise=2  #请求2次都成功的话，目标主机是正常状态
fall=3  #请求3次都失败的话，目标主机是宕机状态
timeout=1000 #设置请求超时时间，单位为毫秒
default_down=true  #设定初始时服务器的状态，如果是true，就说明默认是down的，如果是false，就是up的,要等rise检查次数达到一定成功次数以后才会被认为是正常的
port=80  #指定后端服务器的检查端口
tyep=http   #设置请求的协议
#支持的协议
tcp：简单的tcp连接，如果连接成功，就说明后端正常。
ssl_hello：发送一个初始的SSL hello包并接受服务器的SSL hello包。
http：发送HTTP请求，通过后端的回复包的状态来判断后端是否存活。
mysql: 向mysql服务器连接，通过接收服务器的greeting包来判断后端是否存活。
ajp：向后端发送AJP协议的Cping包，通过接收Cpong包来判断后端是否存活。
```

### 重载配置文件

```javascript
/usr/local/nginx/sbin/nginx -s reload
```

### 浏览器访问

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403172219500.png)


如果其中一台服务器关闭Nginx服务
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403172219123.png)

转载自：[https://cloud.tencent.com/developer/article/1700001](https://cloud.tencent.com/developer/article/1700001)


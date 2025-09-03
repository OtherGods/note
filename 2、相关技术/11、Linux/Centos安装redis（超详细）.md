# 1.源码下载
## 1.1官网下载
redis官网下载tar包到centos服务器

1.2 wget获取
下载到centos指定目录，我这里采用/tools/installbags

```sh
cd /tools/installbags
wget https://download.redis.io/releases/redis-7.0.2.tar.gz
```

# 2 编译安装
默认安装到了/usr/local/bin/目录，但是我想自定义安装到/tools/redis/

```sh
#解压
tar -zxf redis-7.0.2.tar.gz -C /tools/installbags
#编译
make
#安装
make install PREFIX=/tools/redis
```

安装完成了
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307200934381.png)

# 3添加环境变量设置
## 3.1配置环境变量
##添加内容
```sh
vi ~/.bash_profile
#配置生效
source ~/.bash_profile
```
添加以下内容

```sh
REDIS_HOME=/tools/redis
PATH=$PATH:$REDIS_HOME/bin[:另一个需要配置的路径,如果有的话[:另一个需要配置的路径,如果有的话]]
```

## 3.2 启动和停止
```sh
#实际是去找/tools/redis/bin的这个启动语句,并使用redis配置文件
redis-server /tools/installbags/redis-7.0.2/redis.conf
#/tools/redis/bin的这个预计进行停止
redis-cli shutdown
```

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307200938886.png)

## 3.3 测试
开启另外一个ssh窗口进行测试

```sh
redis-cli
set name potato
get name
```

![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307200939201.png)

## 3.4 redis.conf文件说明
```sh
#设置后台启动，如果不是后台启动，每次推出redis就关闭了
daemonize yes
#开启密码保护，注释则不需要密码
requirepass 密码
#设置端口号
port 端口号

#----------------------------------
#redis远程访问配置
#允许访问的ip，改为0.0.0.0就是所有ip均可
bind 0.0.0.0         #默认是bind 127.0.0.1 -::1
protected-mode no    #默认是yes
```

## 3.5设开机置自启
```sh
cd /usr/lib/systemd/system
touch redis.service
vi redis.service
```

添加内容如下：
```sh
[Unit]
Description=redis-server
After=network.target

[Service]
Type=forking

ExecStart=/tools/redis/bin/redis-server /tools/installbags/redis-7.0.2/redis.conf
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

```sh
#重载系统服务
systemctl daemon-reload
#设置开机自启
systemctl enable redis.service
#取消开机自启
systemctl disable redis.service
#启动服务
systemctl start redis.service
#停止服务
systemctl stop redis.service
#查看服务状态
systemctl status redis.service
```


# 4 可能存在的问题
1、一般云服务器有防火墙，所以当我们将配置文件的bind 127.0.0.1 改为 bind 0.0.0.0这个时候还是不能够用自己的本地客户端连接云服务器上的redis，这个时候就需要到云服务器控制台安全组下配置一下放开6379端口。
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307200943040.png)


————————————————
版权声明：本文为CSDN博主「AlanDreamer」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/qq_39187538/article/details/126485922
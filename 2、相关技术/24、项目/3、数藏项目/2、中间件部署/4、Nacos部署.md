## 配置要求

CPU：1核

内存：2G

## 相关地址

开源地址：[https://github.com/alibaba/nacos](https://github.com/alibaba/nacos)

官方网站：[https://nacos.io/zh-cn/index.html](https://nacos.io/zh-cn/index.html)

### Docker 安装

参考这个链接把docker&docker-compose安装好

[3、Docker & DokcerCompose安装](2、相关技术/24、项目/3、数藏项目/2、中间件部署/3、Docker%20&%20DokcerCompose安装.md)

### Nacos安装

1、下载最新版nacos源码：

```
git clone https://github.com/nacos-group/nacos-docker.git
cd nacos-docker
```

如果没有安装git，可以直接去github上下载压缩包，然后到服务上解压缩即可。

2、修改配置文件

> nacos配置中默认指定了root账号的密码为root，创建一个新用户账号和密码都是nacos，创建数据库nacos_devtest
> ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408252308596.png)

修改 nacos-docker-2.3.0/env/nacos-standlone-mysql.env

主要修改：MYSQL_SERVICE_HOST、MYSQL_SERVICE_DB_NAME、MYSQL_SERVICE_USER、MYSQL_SERVICE_PASSWORD等几个为你自己的数据链接信息：

```
# 配合同级目录下mysql.env的配置
PREFER_HOST_MODE=hostname  
MODE=standalone  
SPRING_DATASOURCE_PLATFORM=mysql  
MYSQL_SERVICE_HOST=rm-****.mysql.rds.aliyuncs.com  
MYSQL_SERVICE_DB_NAME=nacos_config  
MYSQL_SERVICE_PORT=3306  
MYSQL_SERVICE_USER=nfturbo  
MYSQL_SERVICE_PASSWORD=NFTurbo***  
MYSQL_SERVICE_DB_PARAM=characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
```

| 配置名                    | 含义       |               |
| ---------------------- | -------- | ------------- |
| MYSQL_SERVICE_HOST     | 数据库 连接地址 |               |
| MYSQL_SERVICE_PORT     | 数据库端口    | 默认 : **3306** |
| MYSQL_SERVICE_DB_NAME  | 数据库库名    |               |
| MYSQL_SERVICE_USER     | 数据库用户名   |               |
| MYSQL_SERVICE_PASSWORD | 数据库用户密码  |               |

3、创建数据库及建表

创建名称为nacos_config的数据库，并且初始化表结构，建表语句在下面（请找你自己安装的版本对应的SQL文件）

https://github.com/alibaba/nacos/blob/2.3.0/distribution/conf/mysql-schema.sql

4、启动nacos

在nacos-docker-2.3.0/example目录下执行：

```
# 后台启动
docker-compose -f standalone-mysql-8.yaml up -d
```

> `-f` 选项指定要使用的 Compose 文件。默认情况下，`docker-compose` 会寻找名为 `docker-compose.yml` 或 `docker-compose.yaml` 的文件。如果你的 Compose 文件有其他名字，比如 `standalone-mysql-8.yaml`，你需要使用 `-f` 选项来指定它。
> 
> 在这个例子中，`standalone-mysql-8.yaml` 文件应该包含有关如何配置和启动 MySQL 8 的 Docker 服务的信息。

即可启动nacos和msyql

```
docker-compose up  #启动所有容器
docker-compose up -d  #后台启动并运行
docker-compose stop  #停止容器
docker-compose start  #启动容器
docker-compose down #停止并销毁容器
```

5、访问nacos配置控制台

http://你的公网ip地址:8848/nacos/index.html#

![](https://cdn.nlark.com/yuque/0/2023/png/5378072/1703497001821-fd3878f7-a899-4b85-87a3-811cf8b599bd.png)

### 常见问题

1、控制台无法访问

可能是你的8848端口没开，如果是阿里云服务器，可以去安全组中把这个入口ip配置上
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408251806840.png)

同时记得也要开启9848、8848以及7848三个端口：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408251807626.png)

2、No DataSource set

可能是你的数据库或者表建的不对，按照要求表建好之后重启。

3、ERROR [internal] load metadata for docker.io/library/mysql:8.0.31

这个原因主要是 docker 镜像源的问题，需要更新成能用的国内的镜像。

可以参考下面的一些国内的源，多试几个：

```
https://docker.anyhub.us.kg/
https://hub.uuuadc.top/
https://dockerhub.jobcher.com/
https://dockerhub.icu/
https://docker.ckyl.me/
https://docker.awsl9527.cn/
https://docker2.awsl9527.cn/  -- 这个可以用
https://q7ta64ip.mirror.aliyuncs.com
https://hx983jf6.mirror.aliyuncs.com
https://docker.mirrors.ustc.edu.cn
https://hub-mirror.c.163.com
https://docker.m.daocloud.io
https://mirror.baidubce.com
https://docker.nju.edu.cn
https://jockerhub.com
https://dockerhub.azk8s.cn
https://dockerproxy.com
https://mirror.baidubce.com
https://docker.nju.edu.cn
https://mirror.iscas.ac.cn
```

在服务器配置文件
```shell
vim /etc/docker/daemon.json 

{
  "registry-mirrors": ["https://docker.m.daocloud.io"],
  "dns": ["8.8.8.8", "8.8.4.4"]
}
```

3、翻墙的时候别用新加坡的，不然启动后也没办法访问/(ㄒoㄒ)/~~

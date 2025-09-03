### 1.概述
Docker是一个开源的应用容器引擎；是一个轻量级容器技术；
Docker支持将软件编译成一个镜像；然后在镜像中各种软件做好配置，将镜像发布出去，其他使用者可以直接使用这个镜像；

运行中的这个镜像称为容器，容器启动是非常快速的。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807215012412.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
### 2.总体步骤
- 搜索镜像
- 拉取镜像
- 查看镜像
- 启动容器(端口映射)
- 停止容器
- 移除容器
### 3.安装tomcat
#### 3.1.搜索镜像
docker hub上面查找tomcat镜像
>docker search tomcat
```shell
[root@localhost ~]# docker search tomcat
INDEX       NAME                                    DESCRIPTION                                     STARS     OFFICIAL   AUTOMATED
docker.io   docker.io/tomcat                        Apache Tomcat is an open source implementa...   2482      [OK]       
docker.io   docker.io/tomee                         Apache TomEE is an all-Apache Java EE cert...   66        [OK]       
docker.io   docker.io/dordoka/tomcat                Ubuntu 14.04, Oracle JDK 8 and Tomcat 8 ba...   53                   [OK]
docker.io   docker.io/bitnami/tomcat                Bitnami Tomcat Docker Image                     29                   [OK]
docker.io   docker.io/kubeguide/tomcat-app          Tomcat image for Chapter 1                      26                   
docker.io   docker.io/consol/tomcat-7.0             Tomcat 7.0.57, 8080, "admin/admin"              16                   [OK]
docker.io   docker.io/cloudesire/tomcat             Tomcat server, 6/7/8                            15                   [OK]
docker.io   docker.io/aallam/tomcat-mysql           Debian, Oracle JDK, Tomcat & MySQL              11                   [OK]
docker.io   docker.io/tutum/tomcat                  Base docker image to run a Tomcat applicat...   11                   
docker.io   docker.io/arm32v7/tomcat                Apache Tomcat is an open source implementa...   6                    
docker.io   docker.io/rightctrl/tomcat              CentOS , Oracle Java, tomcat application s...   5                    [OK]
docker.io   docker.io/maluuba/tomcat7-java8         Tomcat7 with java8.                             3                    
docker.io   docker.io/amd64/tomcat                  Apache Tomcat is an open source implementa...   2                    
docker.io   docker.io/arm64v8/tomcat                Apache Tomcat is an open source implementa...   2                    
docker.io   docker.io/99taxis/tomcat7               Tomcat7                                         1                    [OK]
docker.io   docker.io/camptocamp/tomcat-logback     Docker image for tomcat with logback integ...   1                    [OK]
docker.io   docker.io/i386/tomcat                   Apache Tomcat is an open source implementa...   1                    
docker.io   docker.io/ppc64le/tomcat                Apache Tomcat is an open source implementa...   1                    
docker.io   docker.io/appsvc/tomcat                                                                 0                    
docker.io   docker.io/cfje/tomcat-resource          Tomcat Concourse Resource                       0                    
docker.io   docker.io/jelastic/tomcat               An image of the Tomcat Java application se...   0                    
docker.io   docker.io/oobsri/tomcat8                Testing CI Jobs with different names.           0                    
docker.io   docker.io/picoded/tomcat7               tomcat7 with jre8 and MANAGER_USER / MANAG...   0                    [OK]
docker.io   docker.io/s390x/tomcat                  Apache Tomcat is an open source implementa...   0                    
docker.io   docker.io/secoresearch/tomcat-varnish   Tomcat and Varnish 5.0                          0                    [OK]
```
#### 3.2.拉取镜像
从docker hub上拉取tomcat镜像到本地
```shell
[root@localhost ~]# docker rmi docker.io/tomcat
Error response from daemon: No such image: docker.io/tomcat:latest
[root@localhost ~]# docker pull tomcat
Using default tag: latest
Trying to pull repository docker.io/library/tomcat ... 
latest: Pulling from docker.io/library/tomcat
a4d8138d0f6b: Pull complete 
dbdc36973392: Pull complete 
f59d6d019dd5: Pull complete 
aaef3e026258: Pull complete 
5e86b04a4500: Pull complete 
1a6643a2873a: Pull complete 
2ad1e30fc17c: Pull complete 
16f4e6ee0ca6: Pull complete 
928f4d662d23: Pull complete 
b8d24294d525: Pull complete 
Digest: sha256:2785fac92d1bcd69d98f2461c6799390555a41fd50d3f847b544368d594c637b
Status: Downloaded newer image for docker.io/tomcat:latest
```
#### 3.3.查看镜像
docker images查看是否有拉取到的tomcat
```shell
[root@localhost ~]# docker images tomcat
REPOSITORY          TAG                 IMAGE ID            CREATED                  SIZE
docker.io/tomcat    latest              238e6d7313e3        Less than a second ago   506 MB
docker.io/tomcat    7-jre7              47c156f4d4e3        5 weeks ago              359 MB
```
#### 3.4.启动镜像
使用tomcat镜像创建容器(也叫运行镜像)
>docker run -it -p 8080:8080 tomcat
-p 主机端口:docker容器端口
-P 随机分配端口
i:交互
t:终端

```shell
[root@localhost ~]# docker run -it -p 8080:8080 docker.io/tomcat
Using CATALINA_BASE:   /usr/local/tomcat
Using CATALINA_HOME:   /usr/local/tomcat
Using CATALINA_TMPDIR: /usr/local/tomcat/temp
Using JRE_HOME:        /usr/local/openjdk-8
Using CLASSPATH:       /usr/local/tomcat/bin/bootstrap.jar:/usr/local/tomcat/bin/tomcat-juli.jar
18-Jun-2019 09:18:47.186 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log Server version:        Apache Tomcat/8.5.43
18-Jun-2019 09:18:47.201 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log Server built:          Jul 4 2019 20:53:15 UTC
18-Jun-2019 09:18:47.201 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log Server number:         8.5.43.0
18-Jun-2019 09:18:47.201 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log OS Name:               Linux
18-Jun-2019 09:18:47.201 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log OS Version:            3.10.0-957.el7.x86_64
18-Jun-2019 09:18:47.201 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log Architecture:          amd64
18-Jun-2019 09:18:47.201 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log Java Home:             /usr/local/openjdk-8/jre
18-Jun-2019 09:18:47.202 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log JVM Version:           1.8.0_222-b10
18-Jun-2019 09:18:47.202 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log JVM Vendor:            Oracle Corporation
18-Jun-2019 09:18:47.202 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log CATALINA_BASE:         /usr/local/tomcat
18-Jun-2019 09:18:47.202 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log CATALINA_HOME:         /usr/local/tomcat
18-Jun-2019 09:18:47.202 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log Command line argument: -Djava.util.logging.config.file=/usr/local/tomcat/conf/logging.properties
18-Jun-2019 09:18:47.202 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log Command line argument: -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager
18-Jun-2019 09:18:47.202 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log Command line argument: -Djdk.tls.ephemeralDHKeySize=2048
18-Jun-2019 09:18:47.203 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log Command line argument: -Djava.protocol.handler.pkgs=org.apache.catalina.webresources
18-Jun-2019 09:18:47.203 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log Command line argument: -Dorg.apache.catalina.security.SecurityListener.UMASK=0027
```
#### 3.5.查看
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019080721542715.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
### 4.安装mysql
#### 4.1.搜索镜像
docker hub上面查找mysql镜像
>docker search mysql
```shell
[root@localhost ~]# docker search mysql
INDEX       NAME                                        DESCRIPTION                                     STARS     OFFICIAL   AUTOMATED
docker.io   docker.io/mysql                             MySQL is a widely used, open-source relati...   8458      [OK]       
docker.io   docker.io/mariadb                           MariaDB is a community-developed fork of M...   2914      [OK]       
docker.io   docker.io/mysql/mysql-server                Optimized MySQL Server Docker images. Crea...   626                  [OK]
docker.io   docker.io/centurylink/mysql                 Image containing mysql. Optimized to be li...   60                   [OK]
docker.io   docker.io/centos/mysql-57-centos7           MySQL 5.7 SQL database server                   59                   
docker.io   docker.io/mysql/mysql-cluster               Experimental MySQL Cluster Docker images. ...   49                   
docker.io   docker.io/deitch/mysql-backup               Automated and scheduled mysql database dum...   40                   [OK]
docker.io   docker.io/tutum/mysql                       Base docker image to run a MySQL database ...   32                   
docker.io   docker.io/bitnami/mysql                     Bitnami MySQL Docker Image                      29                   [OK]
docker.io   docker.io/schickling/mysql-backup-s3        Backup MySQL to S3 (supports periodic back...   28                   [OK]
docker.io   docker.io/linuxserver/mysql                 A Mysql container, brought to you by Linux...   21                   
docker.io   docker.io/prom/mysqld-exporter                                                              20                   [OK]
docker.io   docker.io/centos/mysql-56-centos7           MySQL 5.6 SQL database server                   15                   
docker.io   docker.io/circleci/mysql                    MySQL is a widely used, open-source relati...   13                   
docker.io   docker.io/mysql/mysql-router                MySQL Router provides transparent routing ...   12                   
docker.io   docker.io/arey/mysql-client                 Run a MySQL client from a docker container      10                   [OK]
docker.io   docker.io/imega/mysql-client                Size: 36 MB, alpine:3.5, Mysql client: 10....   6                    [OK]
docker.io   docker.io/openshift/mysql-55-centos7        DEPRECATED: A Centos7 based MySQL v5.5 ima...   6                    
docker.io   docker.io/yloeffler/mysql-backup            This image runs mysqldump to backup data u...   6                    [OK]
docker.io   docker.io/fradelg/mysql-cron-backup         MySQL/MariaDB database backup using cron t...   4                    [OK]
docker.io   docker.io/genschsa/mysql-employees          MySQL Employee Sample Database                  2                    [OK]
docker.io   docker.io/jelastic/mysql                    An image of the MySQL database server main...   1                    
docker.io   docker.io/ansibleplaybookbundle/mysql-apb   An APB which deploys RHSCL MySQL                0                    [OK]
docker.io   docker.io/monasca/mysql-init                A minimal decoupled init container for mysql    0                    
docker.io   docker.io/widdpim/mysql-client              Dockerized MySQL Client (5.7) including Cu...   0                    [OK]
```
#### 4.2.拉取镜像
```shell
[root@localhost ~]# docker pull docker.io/mysql
Using default tag: latest
Trying to pull repository docker.io/library/mysql ... 
latest: Pulling from docker.io/library/mysql
0a4690c5d889: Pull complete 
98aa2fc6cbeb: Pull complete 
0777e6eb0e6f: Pull complete 
2464189c041c: Pull complete 
b45df9dc827d: Pull complete 
b42b00086160: Pull complete 
bb93567627c7: Pull complete 
bda91ab2ec35: Pull complete 
50b4c0c50b33: Pull complete 
70c2999bc788: Pull complete 
3cf3e568e51b: Pull complete 
c80d654aa77e: Pull complete 
Digest: sha256:b1b2c176a45f4ff6875d0d7461fe1fdd8606deb015b38b69545f72de97714efd
Status: Downloaded newer image for docker.io/mysql:latest
```
#### 4.3.查看镜像
```shell
[root@localhost ~]# docker images mysql
REPOSITORY          TAG                 IMAGE ID            CREATED                  SIZE
docker.io/mysql     latest              2151acc12881        Less than a second ago   445 MB
docker.io/mysql     <none>              7bb2586065cd        2 months ago             477 MB
```
#### 4.4.启动镜像
使用mysql镜像
>docker exec -it MySQL运行成功后的容器ID     /bin/bash
```shell
docker run -di --name mysql_0803 -p 33306:3306 -e MYSQL_ROOT_PASSWORD=123456 mysql
```
-p 代表端口映射，格式为  宿主机映射端口:容器运行端口
-e 代表添加环境变量  MYSQL_ROOT_PASSWORD是root用户的登陆密码

#### 4.5.进入MySQL容器,登陆MySQL 
进入mysql容器
```shell
docker exec -it mysql_0803 /bin/bash
```
登陆mysql
```shell
mysql -u root -p
```
外部Win10也来连接运行在dokcer上的mysql服务
我们在我们本机的电脑上去连接虚拟机Centos中的Docker容器，这里192.168.247.130是虚拟机操作系统的IP
关闭防火墙
>firewall-cmd --state 
service firewalld stop
firewall-cmd --state

#### 4.6.Mysql连接权限
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807215917421.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
原因：mysql 8.0 默认使用 caching_sha2_password 身份验证机制；客户端不支持新的加密方式。

解决方案：
修改用户（root）的加密方式

步骤1：进入mysql容器内部
```shell
[root@localhost ~]# docker exec -it mysql01 bash   ## mysql01是容器的别名，这里也可以用容器的id代替
```
步骤2：登录mysql
```shell
root@e285125c99d6:/# mysql -u root -p
Enter password: 
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 11
Server version: 8.0.13 MySQL Community Server - GPL

Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.
```
步骤3：设置用户配置项

- 查看用户信息
```shell
mysql> select host,user,plugin,authentication_string from mysql.user; 
+-----------+------------------+-----------------------+------------------------------------------------------------------------+
| host      | user             | plugin                | authentication_string                                                  |
+-----------+------------------+-----------------------+------------------------------------------------------------------------+
| %         | root             | caching_sha2_password | $A$005$HF7;krfwhkKHp5fPenQm4J2dm/RJtbbyjtCUVdDCcboXQw3ALxsif/sS1 |
| localhost | mysql.infoschema | caching_sha2_password | $A$005$THISISACOMBINATIONOFINVALIDSALTANDPASSWORDTHATMUSTNEVERBRBEUSED |
| localhost | mysql.session    | caching_sha2_password | $A$005$THISISACOMBINATIONOFINVALIDSALTANDPASSWORDTHATMUSTNEVERBRBEUSED |
| localhost | mysql.sys        | caching_sha2_password | $A$005$THISISACOMBINATIONOFINVALIDSALTANDPASSWORDTHATMUSTNEVERBRBEUSED |
| localhost | root             | mysql_native_password | *6BB4837EB74329105EE4568DDA7DC67ED2CA2AD9                              |
+-----------+------------------+-----------------------+------------------------------------------------------------------------+
```
备注：host为 % 表示不限制ip localhost表示本机使用 plugin非mysql_native_password 则需要修改密码

- 修改加密方式
```shell
ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '123456';  ### 123456 mysql的登录密码
flush privileges;
```
然后再查看用户信息
```shell
mysql> select host,user,plugin,authentication_string from mysql.user;
+-----------+------------------+-----------------------+------------------------------------------------------------------------+
| host      | user             | plugin                | authentication_string                                                  |
+-----------+------------------+-----------------------+------------------------------------------------------------------------+
| %         | root             | mysql_native_password | *6BB4837EB74329105EE4568DDA7DC67ED2CA2AD9                              |
| localhost | mysql.infoschema | caching_sha2_password | $A$005$THISISACOMBINATIONOFINVALIDSALTANDPASSWORDTHATMUSTNEVERBRBEUSED |
| localhost | mysql.session    | caching_sha2_password | $A$005$THISISACOMBINATIONOFINVALIDSALTANDPASSWORDTHATMUSTNEVERBRBEUSED |
| localhost | mysql.sys        | caching_sha2_password | $A$005$THISISACOMBINATIONOFINVALIDSALTANDPASSWORDTHATMUSTNEVERBRBEUSED |
| localhost | root             | mysql_native_password | *6BB4837EB74329105EE4568DDA7DC67ED2CA2AD9                              |
+-----------+------------------+-----------------------+------------------------------------------------------------------------+
5 rows in set (0.00 sec)
```
- 测试：连接成功
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807220924737.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
### 5.安装redis
#### 5.1.拉取Redis镜像
```shell
[root@localhost ~]# docker pull redis
Using default tag: latest
Trying to pull repository docker.io/library/redis ... 
latest: Pulling from docker.io/library/redis
f5d23c7fed46: Pull complete 
831c20fd50cb: Pull complete 
bc2a0f25caa5: Pull complete 
745ac314a007: Pull complete 
6deeca231441: Pull complete 
6291e84f5373: Pull complete 
Digest: sha256:854715f5cd1b64d2f62ec219a7b7baceae149453e4d29a8f72cecbb5ac51c4ad
Status: Downloaded newer image for docker.io/redis:latest
```
#### 5.2.创建Redis容器
```shell
[root@localhost ~]# docker run -di --name=my_redis -p 6388:6379 redis
f0980b9a56e90a5c3564591f9dfb1c7ecf1f9223d85c011d3aea41aeb14c9d27
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                    NAMES
f0980b9a56e9        redis               "docker-entrypoint..."   8 seconds ago       Up 7 seconds        0.0.0.0:6388->6379/tcp   my_redis
```

#### 5.3.客户端测试
在你的本地电脑命令提示符下，用window版本redis测试
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807221056733.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
### 6.安装nginx
#### 6.1.拉取Nginx镜像
```shell
[root@localhost ~]# docker pull nginx
Using default tag: latest
Trying to pull repository docker.io/library/nginx ... 
latest: Pulling from docker.io/library/nginx
f5d23c7fed46: Already exists 
918b255d86e5: Pull complete 
8c0120a6f561: Pull complete 
Digest: sha256:eb3320e2f9ca409b7c0aa71aea3cf7ce7d018f03a372564dbdb023646958770b
Status: Downloaded newer image for docker.io/nginx:latest
[root@localhost ~]# docker images
REPOSITORY            TAG                 IMAGE ID            CREATED                  SIZE
docker.io/redis       latest              857c4ab5f029        Less than a second ago   98.2 MB
docker.io/nginx       latest              e445ab08b2be        Less than a second ago   126 MB
docker.io/mongo       latest              9c02a5a12c52        Less than a second ago   413 MB
docker.io/mysql       5.6                 7b01f1418bd7        Less than a second ago   256 MB
docker.io/mysql       latest              2151acc12881        Less than a second ago   445 MB
docker.io/tomcat      latest              238e6d7313e3        Less than a second ago   506 MB
brucrliu_tomcat       latest              caefcd845de4        3 hours ago              669 MB
myip1_centos          latest              274c58d0c1e4        3 hours ago              314 MB
myip_centos           latest              41868e4e4fee        3 hours ago              314 MB
mycentos              1.3                 720d4eb5c5f0        5 hours ago              480 MB
bruceliu/centos9527   latest              071e944a589b        12 hours ago             202 MB
bruceliu/tomcat       7-jre7              a66f9aba334d        17 hours ago             359 MB
docker.io/tomcat      7-jre7              47c156f4d4e3        5 weeks ago              359 MB
docker.io/mysql       <none>              7bb2586065cd        2 months ago             477 MB
docker.io/centos      7                   9f38484d220f        3 months ago             202 MB
docker.io/centos      latest              9f38484d220f        3 months ago             202 MB
```
#### 6.2.创建Nginx容器
```shell
[root@localhost ~]# docker run -di --name=my_nginx_test -p 80:80  nginx /bin/bash
82edb98d02d40e3053e307bab26edd345aa04c5f9dc9e10d14de5ccb41831a70
[root@localhost ~]# docker run -p 8880:80 -d docker.io/nginx
b6a5fce26fb4eb4829aee8e27a673940e83ed6978f99076377e24cb728910643
[root@localhost ~]# netstat -anp | grep 8880
tcp6       0      0 :::8880                 :::*                    LISTEN      30443/docker-proxy- 
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                  NAMES
b6a5fce26fb4        docker.io/nginx     "nginx -g 'daemon ..."   26 seconds ago      Up 25 seconds       0.0.0.0:8880->80/tcp   clever_goldwasser
82edb98d02d4        nginx               "/bin/bash"              3 minutes ago       Up 3 minutes        0.0.0.0:80->80/tcp     my_nginx_test
```
#### 6.3.测试Nginx
![在这插入图片描述](https://img-blog.csdnimg.cn/20190807221244102.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
#### 6.4.Nginx配置反向代理
官方的nginx镜像,nginx配置文件nginx.conf 在/etc/nginx/目录下。
在容器内编辑配置文件不方便，我们可以先将配置文件从容器内拷贝到宿主机，编辑修改后再拷贝回去。
- 从容器拷贝配置文件到宿主机
```shell
[root@localhost ~]# docker cp clever_goldwasser:/etc/nginx/nginx.conf nginx.conf 
[root@localhost ~]# ll
总用量 8
-rw-r--r--. 1 root root    0 6月  17 19:42 111.txt
-rw-r--r--. 1 root root    0 6月  17 19:42 1.txt
-rw-------. 1 root root 1257 2月  17 21:33 anaconda-ks.cfg
drwxr-xr-x. 2 root root   80 6月  18 14:35 myfile
-rw-r--r--. 1 root root  643 7月  23 2019 nginx.conf
```
- 编辑nginx.conf，添加反向代理配置
```shell
upstream tomcat-cas {
	server 172.17.0.7:8080;
}
server {
	listen 80;
	server_name passport.easyshop.com;
	location / {
		proxy_pass http://tomcat-cas;
		index index.html index.htm;
	}
}
```
- 将修改后的配置文件拷贝到容器
```shell
docker cp nginx.conf  clever_goldwasser:/etc/nginx/nginx.conf
```
- 重新启动容器
```shell
docker restart clever_goldwasser
```

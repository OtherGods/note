### 1.容器概念

容器（Container）——镜像运行时的实体
镜像（Image）和容器（Container）的关系，就像是面向对象程序设计中的类和实例一样，镜像是静态的定义，容器是镜像运行时的实体。容器可以被创建、启动、停止、删除、暂停等 。

容器的实质是进程，但与直接在宿主执行的进程不同，容器进程运行于属于自己的独立的命名空间。前面讲过镜像使用的是分层存储，容器也是如此。

容器存储层的生存周期和容器一样，容器消亡时，容器存储层也随之消亡。因此，任何保存于容器存储层的信息都会随容器删除而丢失。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190721222509923.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

### 2.Docker容器操作
#### 2.1.查看容器
语法：

>    docker ps [OPTIONS]

OPTIONS说明（常用）：

> -a :列出当前所有正在运行的容器+历史上运行过的
> -l :显示最近创建的容器。
> -n：显示最近n个创建的容器。
> -q :静默模式，只显示容器编号。
> --no-trunc :不截断输出

- 查看正在运行容器：
```java
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
```
- 查看所有的容器（启动过的历史容器):
```java
[root@localhost ~]# docker ps -a
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS                     PORTS                               NAMES
8b3e9c904970        mysql               "docker-entrypoint..."   3 months ago        Exited (255) 4 hours ago   0.0.0.0:3306->3306/tcp, 33060/tcp   my1024_mysql
```
- 查看最后一次运行的容器
```java
[root@localhost ~]# docker ps -l
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS                     PORTS                               NAMES
8b3e9c904970        mysql               "docker-entrypoint..."   3 months ago        Exited (255) 4 hours ago   0.0.0.0:3306->3306/tcp, 33060/tcp   my1024_mysql
```
- 查看停止的容器
```java
[root@localhost ~]# docker ps -f status=exited
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS                     PORTS                               NAMES
8b3e9c904970        mysql               "docker-entrypoint..."   3 months ago        Exited (255) 4 hours ago   0.0.0.0:3306->3306/tcp, 33060/tcp   my1024_mysql
```
#### 2.2.创建与启动容器

语法：
> docker run [OPTIONS] IMAGE [COMMAND] [ARG...]

创建容器常用的参数说明：
>-i：表示运行容器 
-t：表示容器启动后会进入其命令行。加入这两个参数后，容器创建就能登录进去。即分配一个伪终端。
`--`name :为创建的容器命名。
-v：表示目录映射关系（前者是宿主机目录，后者是映射到宿主机上的目录），可以使用多个－v做多个目录或文件映射。注意：最好做目录映射，在宿主机上做修改，然后共享到容器上。
-d：在run后面加上-d参数,则会创建一个守护式容器在后台运行（这样创建容器后不会自动登录容器，如果只加-i -t两个参数，创建后就会自动进去容器）。
-p：表示端口映射，前者是宿主机端口，后者是容器内的映射端口。可以使用多个－p做多个端口映射

#### 2.2.1.交互式容器
使用镜像centos:latest以交互模式启动一个容器,在容器内执行/bin/bash命令,并取名为mycentos
>docker run -it --name=mycentos centos:7 /bin/bash
```java
[root@localhost ~]# docker run -it --name=mycentos centos:7 /bin/bash
Unable to find image 'centos:7' locally
Trying to pull repository docker.io/library/centos ... 
7: Pulling from docker.io/library/centos
8ba884070f61: Pull complete 
Digest: sha256:a799dd8a2ded4a83484bbae769d97655392b3f86533ceb7dd96bbac929809f3c
Status: Downloaded newer image for docker.io/centos:7
```
这时我们通过ps命令查看，发现可以看到启动的容器，状态为启动状态
```java
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
68af7641543c        centos:7            "/bin/bash"         2 minutes ago       Up 2 minutes                            mycentos
```
使用exit命令 退出当前容器
```java
[root@68af7641543c /]# exit 
exit
[root@localhost ~]# 
```
然后用ps -a 命令查看发现该容器也随之停止：
```java
[root@localhost ~]# docker ps -a
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS                      PORTS                               NAMES
68af7641543c        centos:7            "/bin/bash"              3 minutes ago       Exited (0) 28 seconds ago                                       mycentos
8b3e9c904970        mysql               "docker-entrypoint..."   3 months ago        Exited (255) 4 hours ago    0.0.0.0:3306->3306/tcp, 33060/tcp   my1024_mysql
```
#### 2.2.2.守护式容器
创建一个守护式容器：如果对于一个需要长期运行的容器来说，我们可以创建一个守护式容器。命令如下（容器名称不能重复）：
>docker run -di --name=mycentos2 centos:7
```java
[root@localhost ~]# docker run -di --name=mycentos2 centos:7
bf9248203458805ff33b997c50b89b58791ac1af21b22e2ab243a8bee2a0bf66
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
bf9248203458        centos:7            "/bin/bash"         19 seconds ago      Up 18 seconds                           mycentos2
```
登录守护式容器方式：
docker exec -it container_name (或者 container_id)  /bin/bash（exit退出时，容器不会停止）
```java
[root@localhost ~]# docker exec -it mycentos2 /bin/bash
[root@bf9248203458 /]# ll
total 12
-rw-r--r--.   1 root root 12082 Mar  5 17:36 anaconda-post.log
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 bin -> usr/bin
drwxr-xr-x.   5 root root   340 Jun 17 11:30 dev
drwxr-xr-x.   1 root root    66 Jun 17 11:30 etc
drwxr-xr-x.   2 root root     6 Apr 11  2018 home
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 lib -> usr/lib
lrwxrwxrwx.   1 root root     9 Mar  5 17:34 lib64 -> usr/lib64
drwxr-xr-x.   2 root root     6 Apr 11  2018 media
drwxr-xr-x.   2 root root     6 Apr 11  2018 mnt
drwxr-xr-x.   2 root root     6 Apr 11  2018 opt
dr-xr-xr-x. 125 root root     0 Jun 17 11:30 proc
dr-xr-x---.   2 root root   114 Mar  5 17:36 root
drwxr-xr-x.   1 root root    21 Jun 17 11:30 run
lrwxrwxrwx.   1 root root     8 Mar  5 17:34 sbin -> usr/sbin
drwxr-xr-x.   2 root root     6 Apr 11  2018 srv
dr-xr-xr-x.  13 root root     0 Jun 10 16:40 sys
drwxrwxrwt.   7 root root   132 Mar  5 17:36 tmp
drwxr-xr-x.  13 root root   155 Mar  5 17:34 usr
drwxr-xr-x.  18 root root   238 Mar  5 17:34 var
[root@bf9248203458 /]# 
```
#### 2.2.3.停止与启动容器
- 停止正在运行的容器：
>docker stop $CONTAINER_NAME/ID
```java
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
bf9248203458        centos:7            "/bin/bash"         4 minutes ago       Up 4 minutes                            mycentos2
[root@localhost ~]# docker stop mycentos2
mycentos2
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
```
- 启动已运行过的容器：
>docker start $CONTAINER_NAME/ID
```java
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
[root@localhost ~]# docker start mycentos2
mycentos2
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
bf9248203458        centos:7            "/bin/bash"         5 minutes ago       Up 3 seconds                            mycentos2
```
#### 2.2.4.重启容器
>docker restart 容器ID或者容器名
#### 2.2.5.强制停止容器
>docker kill 容器ID或者容器名
#### 2.2.6.文件拷贝
如果我们需要将文件拷贝到容器内可以使用cp命令
>docker cp 需要拷贝的文件或目录 容器名称:容器目录

```java
[root@localhost ~]# touch 1.txt
[root@localhost ~]# docker cp 1.txt mycentos2:/root/
[root@localhost ~]# docker exec -it mycentos2 /bin/bash
[root@bf9248203458 /]# cd /root/
[root@bf9248203458 ~]# ll
total 4
-rw-r--r--. 1 root root    0 Jun 17 11:42 1.txt
-rw-------. 1 root root 3415 Mar  5 17:36 anaconda-ks.cfg
```
也可以将文件从容器内拷贝出来
>docker cp 容器名称:容器目录 需要拷贝的文件或目录
```java
[root@localhost ~]# docker cp mycentos2:/root/111.txt /root/
[root@localhost ~]# cd /root/
[root@localhost ~]# ll
总用量 4
-rw-r--r--. 1 root root    0 6月  17 19:42 111.txt
-rw-r--r--. 1 root root    0 6月  17 19:42 1.txt
-rw-------. 1 root root 1257 2月  17 21:33 anaconda-ks.cfg
```
#### 2.2.7.目录挂载

我们可以在创建容器的时候，将**宿主机的目录**与**容器内的目录**进行映射，这样我们就可以通过修改宿主机某个目录的文件从而去影响容器。
> 创建容器 添加-v参数 后边为   宿主机目录:容器目录
> docker run -di --name=mycentos2 -v /usr/local/myhtml:/usr/local/myhtml centos:7

```java
[root@localhost myhtml]# docker run -di --name=mycentos3 -v /usr/local/myhtml:/usr/local/myhtml centos:7
d6e67c10f59d2169ba73436d51253a6280aac1ca47ed5223c4591a90b8e2a31e
[root@localhost myhtml]# docker exec -it mycentos3 /bin/bash
[root@d6e67c10f59d /]# cd /usr/local/
[root@d6e67c10f59d local]# ll
total 0
drwxr-xr-x. 2 root root  6 Apr 11  2018 bin
drwxr-xr-x. 2 root root  6 Apr 11  2018 etc
drwxr-xr-x. 2 root root  6 Apr 11  2018 games
drwxr-xr-x. 2 root root  6 Apr 11  2018 include
drwxr-xr-x. 2 root root  6 Apr 11  2018 lib
drwxr-xr-x. 2 root root  6 Apr 11  2018 lib64
drwxr-xr-x. 2 root root  6 Apr 11  2018 libexec
drwxr-xr-x. 2 root root  6 Jun 17 11:48 myhtml
drwxr-xr-x. 2 root root  6 Apr 11  2018 sbin
drwxr-xr-x. 5 root root 49 Mar  5 17:34 share
drwxr-xr-x. 2 root root  6 Apr 11  2018 src
```
如果你共享的是多级的目录，可能会出现权限不足的提示。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190721232742255.png)
这是因为CentOS7中的安全模块selinux把权限禁掉了，我们需要添加参数  --privileged=true  来解决挂载的目录没有权限的问题
```java
docker run -di --name=mycentos3 -v /usr/local/myhtml:/usr/local/myhtml --privileged=true centos:7
```
#### 2.2.8.查看容器IP地址
我们可以通过以下命令查看容器运行的各种数据
>[root@localhost myhtml]# docker inspect mycentos3

也可以直接执行下面的命令直接输出IP地址
```java
[root@localhost myhtml]# docker inspect --format='{{.NetworkSettings.IPAddress}}' mycentos3
172.17.0.2
```
测试宿主机连接容器
```java
[root@localhost myhtml]# ping 172.17.0.2
PING 172.17.0.2 (172.17.0.2) 56(84) bytes of data.
64 bytes from 172.17.0.2: icmp_seq=1 ttl=64 time=0.100 ms
64 bytes from 172.17.0.2: icmp_seq=2 ttl=64 time=0.040 ms
64 bytes from 172.17.0.2: icmp_seq=3 ttl=64 time=0.082 ms
64 bytes from 172.17.0.2: icmp_seq=4 ttl=64 time=0.040 ms
64 bytes from 172.17.0.2: icmp_seq=5 ttl=64 time=0.061 ms
64 bytes from 172.17.0.2: icmp_seq=6 ttl=64 time=0.039 ms
64 bytes from 172.17.0.2: icmp_seq=7 ttl=64 time=0.040 ms
^C
--- 172.17.0.2 ping statistics ---
7 packets transmitted, 7 received, 0% packet loss, time 6004ms
rtt min/avg/max/mdev = 0.039/0.057/0.100/0.023 ms
```
#### 2.2.9.删除容器
- 删除指定的容器：
>docker rm $CONTAINER_ID/NAME

注意，只能删除停止的容器
- 删除所有容器：
>docker rm `docker ps -a -q`



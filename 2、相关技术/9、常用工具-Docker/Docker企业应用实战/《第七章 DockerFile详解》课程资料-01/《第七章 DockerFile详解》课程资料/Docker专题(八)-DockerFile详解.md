### 1.DockerFile是什么

>当我们从docker镜像仓库中下载的镜像不能满足我们的需求时，我们可以通过以下两种方式对镜像进行更改。
>1.从已经创建的容器中更新镜像，并且提交这个镜像 
>2.使用 Dockerfile 指令来创建一个新的镜像
>
>Dockerfile是用来构建Docker镜像的构建文件，是由一系列命令和参数构成的脚本。DockerFile是描述镜像的一种文件！

Dockerfile面向开发，Docker镜像成为交付标准，Docker容器则涉及部署与运维，三者缺一不可，合力充当Docker体系的基石。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807121004858.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
- Dockerfile，需要定义一个Dockerfile，Dockerfile定义了进程需要的一切东西。Dockerfile涉及的内容包括执行代码或者是文件、环境变量、依赖包、运行时环境、动态链接库、操作系统的发行版、服务进程和内核进程(当应用进程需要和系统服务和内核进程打交道，这时需要考虑如何设计namespace的权限控制)等等;

 - 在Docker中创建镜像最常用的方式，就是使用Dockerfile。Dockerfile是一个Docker镜像的描述文件，我们可以理解成火箭发射的A、B、C、D…的步骤。Dockerfile其内部包含了一条条的指令，每一条指令构建一层，因此每一条指令的内容，就是描述该层应当如何构建。
 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20191229103714977.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
- Docker镜像，在用Dockerfile定义一个文件之后，docker build时会产生一个Docker镜像，当运行 Docker镜像时，会真正开始提供服务;

- Docker容器，容器是直接提供服务的。


- 构建三步骤

```
编写Dockerfile文件 -----> 编写DockerFile文件
docker build      -----> 构建镜像
docker run        -----> 使用镜像启动容器

DockerFile就是镜像的描述文件！
```

### 2.DockerFile文件什么样

#### 2.1.以我们熟悉的CentOS为例

网站:[https://hub.docker.com/_/centos/](https://hub.docker.com/_/centos/)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807120740957.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807120746833.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

#### 2.2.一个Dockerfile的示例如下所示

```shell
#基于centos镜像
FROM centos

#维护人的信息
MAINTAINER The CentOS Project <303323496@qq.com>

#安装httpd软件包
RUN yum -y update
RUN yum -y install httpd

#开启80端口
EXPOSE 80

#复制网站首页文件至镜像中web站点下
ADD index.html /var/www/html/index.html

#复制该脚本至镜像中，并修改其权限
ADD run.sh /run.sh
RUN chmod 775 /run.sh

#当启动容器时执行的脚本文件
CMD ["/run.sh"]
```

```
由上可知，Dockerfile结构大致分为四个部分：
　　（1）基础镜像信息
　　（2）维护者信息
　　（3）镜像操作指令
　　（4）容器启动时执行指令。
Dockerfile每行支持一条指令，每条指令可带多个参数，支持使用以#号开头的注释。下面会对上面使用到的一些常用指令做一些介绍。
```

#### 2.3.DockerFile构建过程解析

- Dockerfile内容基础知识
1：每条保留字指令都必须为大写字母且后面要跟随至少一个参数
2：指令按照从上到下，顺序执行
3：#表示注释
4：每条指令都会创建一个新的镜像层，并对镜像进行提交

- Dockerfile内容基础知识
1：docker从基础镜像运行一个容器
2：执行一条指令并对容器作出修改
3：执行类似docker commit的操作提交一个新的镜像层
4：docker再基于刚提交的镜像运行一个新容器
5：执行dockerfile中的下一条指令直到所有指令都执行完成

- 小结
从应用软件的角度来看，Dockerfile、Docker镜像与Docker容器分别代表软件的三个不同阶段：
1：Dockerfile是软件的原材料
2：Docker镜像是软件的交付品
3：Docker容器则可以认为是软件的运行态。

### 3.Dockerfile常用指令
![在这里插入图片描述](https://img-blog.csdnimg.cn/20191229104513181.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
#### 3.1.FROM
指明构建的新镜像是来自于哪个基础镜像，例如：

```
FROM centos:6
```
>基础镜像不存在会在Docker Hub上拉去(一般会是文件的第一个指令) 使用格式：
FROM <镜像>:[tag]

#### 3.2.MAINTAINER
指明镜像维护着及其联系方式（一般是邮箱地址），例如

```
MAINTAINER BRUCELIU<bruceliu9527@163.com>
```
不过，MAINTAINER并不推荐使用，更推荐使用LABEL来指定镜像作者，例如：

```
LABEL maintainer="bruceliu.cn"
```
>[逐渐废弃] LABLE --替代MAINTANIER 具体使用： LABLE maintainer="作者信息"
#### 3.3.RUN
RUN 指令是用来执行命令行命令的。由于命令行的强大能力，RUN 指令在定制镜像时是最常用的指令之一。其格式有两种：
 • shell 格式：RUN <命令>，就像直接在命令行中输入的命令一样。刚才写的 Dockerfile 中的 RUN 指令就是这种格式.
 • exec 格式：RUN ["可执行文件", "参数1", "参数2"]，这更像是函数调用中的格式。
使用格式： 
>RUN RUN ["","",""]

构建镜像时运行的Shell命令，例如：

```
RUN ["yum", "install", "httpd"]

RUN yum install httpd
```
RUN 就像 Shell 脚本一样可以执行命令，那么我们是否就可以像 Shell 脚本一样把每个命令对应一个 RUN 呢？比如
这样：

```
RUN apt-get update
RUN apt-get install -y gcc libc6-dev make
RUN wget http://download.redis.io/releases/redis-4.0.1.tar.gz
RUN tar xzf redis-4.0.1.tar.gz
RUN cd redis-4.0.1
```

Dockerfile 中每一个指令都会建立一层，RUN 也不例外。每一个 RUN 的行为，和刚才我们手工建立镜像的过程一样：新建立一层，在其上执行这些命令，执行结束后，commit 这一层的修改，构成新的镜像。 而上面的这种写法，创建了 多 层镜像。这是完全没有意义的，而且很多运行时不需要的东西，都被装进了镜像里，比如编译环境、更新的软件包等等。结果就是产生非常臃肿、非常多层的镜像，不仅仅增加了构建部署的时间，也很容易出错。 这是很多初学 Docker 的人常犯的一个错误。

Union FS 是有最大层数限制的，比如 AUFS，曾经是最大不得超过 42 层，现在是不得超过 127 层。上面的Dockerfile 正确的写法应该是这样

```
FROM centos

RUN apt-get update \
&& apt-get install -y gcc libc6-dev make \
&& wget http://download.redis.io/releases/redis-4.0.1.tar.gz \
&& tar xzf redis-4.0.1.tar.gz \
&& cd redis-4.0.1
```
首先，之前所有的命令只有一个目的，就是编译、安装 redis 可执行文件。因此没有必要建立很多层，这只是一层的事情。因此，这里没有使用很多个 RUN 对一一对应不同的命令，而是仅仅使用一个 RUN 指令，并使用 && 将各个所需命令串联起来。将之前的 7 层，简化为了 1 层。

在撰写 Dockerfile 的时候，要经常提醒自己，这并不是在写Shell 脚本，而是在定义每一层该如何构建且，这里为了格式化还进行了换行。Dockerfile 支持 Shell 类的行尾添加 \ 的命令换行方式，以及行首 # 进行注释的格式。良好的格式，比如换行、缩进、注释等，会让维护、排障更为容易，这是一个比较好的习惯。

#### 3.4.CMD
启动容器时执行的Shell命令，例如：

```
CMD ["-C", "/start.sh"] 
CMD ["/usr/sbin/sshd", "-D"] 
CMD /usr/sbin/sshd -D
```
#### 3.5.EXPOSE
声明容器运行的服务端口，为容器打开指定要监听的端口以实现与外部通信,例如：

```
EXPOSE 80 443
```
#### 3.6.ENV
ENV指令可以用于为docker容器设置环境变量 ENV设置的环境变量，可以使用 docker inspect命令来查看。同时还可以使用docker run --env =来修改环境变量。

```
具体用法：
ENV JAVA_HOME /usr/local/jdk
ENV JRE_HOME $JAVA_HOME/jre
ENV CLASSPATH $JAVA_HOME/lib/:$JRE_HOME/lib/
ENV PATH $PATH:$JAVA_HOME/bin/
```
#### 3.7.ADD
类似COPY命令，拷贝文件或目录到镜像中，例如：

```
ADD <src>...<dest>

ADD html.tar.gz /var/www/html
ADD https://xxx.com/html.tar.gz /var/www/html
```
#### 3.8.COPY
拷贝文件或目录到镜像中，用法同ADD，只是不支持自动下载和解压，例如：

```
COPY ./start.sh /start.sh
```
#### 3.9.ENTRYPOINT

启动容器时执行的Shell命令，同CMD类似，只是由ENTRYPOINT启动的程序不会被docker run命令行指定的参数所覆盖，而且，这些命令行参数会被当作参数传递给ENTRYPOINT指定指定的程序，例如：

```
ENTRYPOINT ["/bin/bash", "-C", "/start.sh"]
ENTRYPOINT /bin/bash -C '/start.sh'
```
>PS：Dockerfile文件中也可以存在多个ENTRYPOINT指令，但仅有最后一个会生效。

#### 3.10. VOLUME
创建一个可以从本地主机或其他容器挂载的挂载点，一般用来存放数据库和需要保持的数据等。例如：

```
VOLUME ["/var/lib/mysql"]
```
>只能定义docker管理的卷： VOLUME /data/mysql运行的时候会随机在宿主机的目录下生成一个卷目录！
>一般不会在Dockerfile中用到，更常见的还是在docker run的时候指定-v数据卷。

#### 3.11 USER
为RUN、CMD和ENTRYPOINT执行Shell命令指定运行用户，例如：

```
USER <user>[:<usergroup>]
USER <UID>[:<UID>]
USER bruceliu
```
#### 3.12 WORKDIR
Docker 默认的工作目录是/，只有 RUN 能执行 cd 命令切换目录，而且还只作用在当下下的 RUN，也就是说每一个 RUN 都是独立进行的。

如果想让其他指令在指定的目录下执行，就得靠 WORKDIR。WORKDIR 动作的目录改变是持久的，不用每个指令前都使用一次 WORKDIR。

```
WORKDIR /usr/local/tomcat/
```

#### 3.13.小总结
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807121254167.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

### 4.DockerFile案例
#### 4.1.Base镜像(scratch)
Docker Hub 中 99% 的镜像都是通过在 base 镜像中安装和配置需要的软件构建出来的
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807121345803.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
#### 4.2.自定义镜像mycentos

Docker Hub默认CentOS镜像什么情况??
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807121412711.png)
- 准备编写DockerFile文件
```java
[root@localhost myfile]# pwd
/root/myfile
[root@localhost myfile]# vi mycentos_dockerfile
```
- myCentOS内容DockerFile
```java
FROM centos
MAINTAINER bruceliu<bruceliu167@126.com>

ENV MYPATH /usr/local
WORKDIR $MYPATH

RUN yum -y install vim
RUN yum -y install net-tools

EXPOSE 80

CMD echo $MYPATH
CMD echo "success--------------ok"
CMD /bin/bash
```
- 构建
命令：
>docker build -t 新镜像名字:TAG .
会看到 docker build 命令最后有一个 .                                  
. 表示当前目录

```java
[root@localhost myfile]# docker build -f /root/myfile/mycentos_dockerfile -t mycentos:1.3 .
Sending build context to Docker daemon 2.048 kB
Step 1/10 : FROM centos
 ---> 9f38484d220f
Step 2/10 : MAINTAINER bruceliu<bruceliu167@126.com>
 ---> Using cache
 ---> 281446cb753d
Step 3/10 : ENV MYPATH /usr/local
 ---> Using cache
 ---> 2e09cdb61576
Step 4/10 : WORKDIR $MYPATH
 ---> Using cache
 ---> 4aa136d65f26
Step 5/10 : RUN yum -y install vim
 ---> Using cache
 ---> 585392f09407
Step 6/10 : RUN yum -y install net-tools
 ---> Using cache
 ---> 9c7971b08e59
Step 7/10 : EXPOSE 80
 ---> Using cache
 ---> 6741c0b61390
Step 8/10 : CMD echo $MYPATH
 ---> Using cache
 ---> fb1b4b72ba02
Step 9/10 : CMD echo "success--------------ok"
 ---> Using cache
 ---> 3d636347cc9a
Step 10/10 : CMD /bin/bash
 ---> Using cache
 ---> 720d4eb5c5f0
Successfully built 720d4eb5c5f0
```
- 运行
docker run -it 新镜像名字:TAG 
可以看到，我们自己的新镜像已经支持vim/ifconfig命令，扩展成功了。
```java
[root@localhost ~]# docker images
REPOSITORY            TAG                 IMAGE ID            CREATED                  SIZE
docker.io/mongo       latest              9c02a5a12c52        Less than a second ago   413 MB
mycentos              1.3                 720d4eb5c5f0        50 minutes ago           480 MB
bruceliu/centos9527   latest              071e944a589b        8 hours ago              202 MB
bruceliu/tomcat       7-jre7              a66f9aba334d        13 hours ago             359 MB
docker.io/tomcat      7-jre7              47c156f4d4e3        5 weeks ago              359 MB
docker.io/mysql       latest              7bb2586065cd        2 months ago             477 MB
docker.io/centos      7                   9f38484d220f        3 months ago             202 MB
docker.io/centos      latest              9f38484d220f        3 months ago             202 MB
[root@localhost ~]# docker run -it mycentos:1.3
[root@50784a17d114 local]# pwd
/usr/local
[root@50784a17d114 local]# vi 1.txt
[root@50784a17d114 local]# ifconfig
eth0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
        inet 172.17.0.3  netmask 255.255.0.0  broadcast 0.0.0.0
        inet6 fe80::42:acff:fe11:3  prefixlen 64  scopeid 0x20<link>
        ether 02:42:ac:11:00:03  txqueuelen 0  (Ethernet)
        RX packets 8  bytes 656 (656.0 B)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 8  bytes 656 (656.0 B)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

lo: flags=73<UP,LOOPBACK,RUNNING>  mtu 65536
        inet 127.0.0.1  netmask 255.0.0.0
        inet6 ::1  prefixlen 128  scopeid 0x10<host>
        loop  txqueuelen 1000  (Local Loopback)
        RX packets 0  bytes 0 (0.0 B)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 0  bytes 0 (0.0 B)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0
```
- 列出镜像的变更历史
命令:
>docker history 镜像名
```java
[root@localhost ~]# docker history 720d4eb5c5f0
IMAGE               CREATED             CREATED BY                                      SIZE                COMMENT
720d4eb5c5f0        53 minutes ago      /bin/sh -c #(nop)  CMD ["/bin/sh" "-c" "/b...   0 B                 
3d636347cc9a        53 minutes ago      /bin/sh -c #(nop)  CMD ["/bin/sh" "-c" "ec...   0 B                 
fb1b4b72ba02        53 minutes ago      /bin/sh -c #(nop)  CMD ["/bin/sh" "-c" "ec...   0 B                 
6741c0b61390        53 minutes ago      /bin/sh -c #(nop)  EXPOSE 80/tcp                0 B                 
9c7971b08e59        53 minutes ago      /bin/sh -c yum -y install net-tools             112 MB              
585392f09407        54 minutes ago      /bin/sh -c yum -y install vim                   167 MB              
4aa136d65f26        About an hour ago   /bin/sh -c #(nop) WORKDIR /usr/local            0 B                 
2e09cdb61576        About an hour ago   /bin/sh -c #(nop)  ENV MYPATH=/usr/local        0 B                 
281446cb753d        About an hour ago   /bin/sh -c #(nop)  MAINTAINER bruceliu<bru...   0 B                 
9f38484d220f        3 months ago        /bin/sh -c #(nop)  CMD ["/bin/bash"]            0 B                 
<missing>           3 months ago        /bin/sh -c #(nop)  LABEL org.label-schema....   0 B                 
<missing>           3 months ago        /bin/sh -c #(nop) ADD file:074f2c974463ab3...   202 MB   
```
#### 4.3.CMD/ENTRYPOINT 镜像案例
都是指定一个容器启动时要运行的命令
- CMD
Dockerfile中可以有多个 CMD 指令，但只有最后一个生效，CMD 会被 docker run 之后的参数替换
```java
[root@localhost ~]# docker run -it -p 8888:8080 docker.io/tomcat ls -l
total 124
-rw-r--r--. 1 root root  19534 Jul  4  2019 BUILDING.txt
-rw-r--r--. 1 root root   5407 Jul  4  2019 CONTRIBUTING.md
-rw-r--r--. 1 root root  57011 Jul  4  2019 LICENSE
-rw-r--r--. 1 root root   1726 Jul  4  2019 NOTICE
-rw-r--r--. 1 root root   3255 Jul  4  2019 README.md
-rw-r--r--. 1 root root   7139 Jul  4  2019 RELEASE-NOTES
-rw-r--r--. 1 root root  16262 Jul  4  2019 RUNNING.txt
drwxr-xr-x. 2 root root   4096 Jul 18  2019 bin
drwxr-sr-x. 2 root root    238 Jul  4  2019 conf
drwxr-sr-x. 2 root staff    78 Jul 18  2019 include
drwxr-xr-x. 2 root root   4096 Jul 18  2019 lib
drwxrwxrwx. 2 root root      6 Jul  4  2019 logs
drwxr-sr-x. 3 root staff   151 Jul 18  2019 native-jni-lib
drwxr-xr-x. 2 root root     30 Jul 18  2019 temp
drwxr-xr-x. 7 root root     81 Jul  4  2019 webapps
drwxrwxrwx. 2 root root      6 Jul  4  2019 work
```
- ENTRYPOINT 
docker run 之后的参数会被当做参数传递给 ENTRYPOINT，之后形成新的命令组合
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807123327666.png)
制作CMD版可以查询网页信息的容器
>curl命令可以用来执行下载、发送各种HTTP请求，指定HTTP头部等操作。
如果系统没有curl可以使用yum install curl安装，也可以下载安装。
curl是将下载文件输出到stdout
使用命令：curl http://www.baidu.com
执行后，www.baidu.com的html就会显示在屏幕上了
这是最简单的使用方法。用这个命令获得了http://curl.haxx.se指向的页面，同样，如果这里的URL指向的是一个文件或者一幅图都可以直接下载到本地。如果下载的是HTML文档，那么缺省的将只显示文件头部，即HTML文档的header。要全部显示，请加参数 -i

- 制作CMD版可以查询网页信息的容器
```java
[root@localhost myfile]# vi myip_dockerfile
FROM centos
RUN yum install -y curl
CMD [ "curl", "-s", "https://www.baidu.com" ]
[root@localhost myfile]# docker build -f myip_dockerfile -t myip_centos .
Sending build context to Docker daemon 3.072 kB
Step 1/3 : FROM centos
 ---> 9f38484d220f
Step 2/3 : RUN yum install -y curl
 ---> Running in 617a61151503

Loaded plugins: fastestmirror, ovl
Determining fastest mirrors
 * base: mirrors.aliyun.com
 * extras: mirrors.aliyun.com
 * updates: mirrors.aliyun.com
Resolving Dependencies
--> Running transaction check
---> Package curl.x86_64 0:7.29.0-51.el7 will be updated
---> Package curl.x86_64 0:7.29.0-51.el7_6.3 will be an update
--> Processing Dependency: libcurl = 7.29.0-51.el7_6.3 for package: curl-7.29.0-51.el7_6.3.x86_64
--> Running transaction check
---> Package libcurl.x86_64 0:7.29.0-51.el7 will be updated
---> Package libcurl.x86_64 0:7.29.0-51.el7_6.3 will be an update
--> Finished Dependency Resolution

Dependencies Resolved

================================================================================
 Package         Arch           Version                   Repository       Size
================================================================================
Updating:
 curl            x86_64         7.29.0-51.el7_6.3         updates         269 k
Updating for dependencies:
 libcurl         x86_64         7.29.0-51.el7_6.3         updates         222 k

Transaction Summary
================================================================================
Upgrade  1 Package (+1 Dependent package)

Total download size: 492 k
Downloading packages:
Delta RPMs disabled because /usr/bin/applydeltarpm not installed.
warning: /var/cache/yum/x86_64/7/updates/packages/curl-7.29.0-51.el7_6.3.x86_64.rpm: Header V3 RSA/SHA256 Signature, key ID f4a80eb5: NOKEY
Public key for curl-7.29.0-51.el7_6.3.x86_64.rpm is not installed
--------------------------------------------------------------------------------
Total                                              777 kB/s | 492 kB  00:00     
Retrieving key from file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
Importing GPG key 0xF4A80EB5:
 Userid     : "CentOS-7 Key (CentOS 7 Official Signing Key) <security@centos.org>"
 Fingerprint: 6341 ab27 53d7 8a78 a7c2 7bb1 24c6 a8a7 f4a8 0eb5
 Package    : centos-release-7-6.1810.2.el7.centos.x86_64 (@CentOS)
 From       : /etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
Running transaction check
Running transaction test
Transaction test succeeded
Running transaction
  Updating   : libcurl-7.29.0-51.el7_6.3.x86_64                             1/4 
  Updating   : curl-7.29.0-51.el7_6.3.x86_64                                2/4 
  Cleanup    : curl-7.29.0-51.el7.x86_64                                    3/4 
  Cleanup    : libcurl-7.29.0-51.el7.x86_64                                 4/4 
  Verifying  : curl-7.29.0-51.el7_6.3.x86_64                                1/4 
  Verifying  : libcurl-7.29.0-51.el7_6.3.x86_64                             2/4 
  Verifying  : libcurl-7.29.0-51.el7.x86_64                                 3/4 
  Verifying  : curl-7.29.0-51.el7.x86_64                                    4/4 

Updated:
  curl.x86_64 0:7.29.0-51.el7_6.3                                               

Dependency Updated:
  libcurl.x86_64 0:7.29.0-51.el7_6.3                                            

Complete!
 ---> c0ab6e8f3af7
Removing intermediate container 617a61151503
Step 3/3 : CMD curl -s https://www.baidu.com
 ---> Running in 9933fbe7593b
 ---> 41868e4e4fee
Removing intermediate container 9933fbe7593b
Successfully built 41868e4e4fee
```
查看生成的镜像
```java
[root@localhost myfile]# docker images
REPOSITORY            TAG                 IMAGE ID            CREATED                  SIZE
docker.io/mongo       latest              9c02a5a12c52        Less than a second ago   413 MB
docker.io/tomcat      latest              238e6d7313e3        Less than a second ago   506 MB
myip_centos           latest              41868e4e4fee        13 minutes ago           314 MB
mycentos              1.3                 720d4eb5c5f0        About an hour ago        480 MB
bruceliu/centos9527   latest              071e944a589b        9 hours ago              202 MB
bruceliu/tomcat       7-jre7              a66f9aba334d        13 hours ago             359 MB
docker.io/tomcat      7-jre7              47c156f4d4e3        5 weeks ago              359 MB
docker.io/mysql       latest              7bb2586065cd        2 months ago             477 MB
docker.io/centos      7                   9f38484d220f        3 months ago             202 MB
docker.io/centos      latest              9f38484d220f        3 months ago             202 MB
[root@localhost myfile]# docker run 41868e4e4fee
```
问题
如果我们希望显示 HTTP 头信息，就需要加上 -i 参数
```java
[root@localhost myfile]# docker run 41868e4e4fee -i
container_linux.go:235: starting container process caused "exec: \"-i\": executable file not found in $PATH"
/usr/bin/docker-current: Error response from daemon: oci runtime error: container_linux.go:235: starting container process caused "exec: \"-i\": executable file not found in $PATH".
```
WHY
```java
我们可以看到可执行文件找不到的报错，executable file not found。
之前我们说过，跟在镜像名后面的是 command，运行时会替换 CMD 的默认值。
因此这里的 -i 替换了原来的 CMD，而不是添加在原来的 curl -s https://www.baidu.com 后面。而 -i 根本不是命令，所以自然找不到。
 
那么如果我们希望加入 -i 这参数，我们就必须重新完整的输入这个命令：
 
$ docker run myip curl -s http://ip.cn -i
```
制作ENTROYPOINT版查询IP信息的容器
```java
[root@localhost myfile]# docker build -f myip1_dockerfile -t myip1_centos .
Sending build context to Docker daemon 4.096 kB
Step 1/3 : FROM centos
 ---> 9f38484d220f
Step 2/3 : RUN yum install -y curl
 ---> Using cache
 ---> c0ab6e8f3af7
Step 3/3 : ENTRYPOINT curl -s https://www.baidu.com
 ---> Running in b31022ed350c
 ---> 274c58d0c1e4
Removing intermediate container b31022ed350c
Successfully built 274c58d0c1e4
[root@localhost myfile]# docker run 274c58d0c1e4 
[root@localhost myfile]# docker run 274c58d0c1e4 -i
HTTP/1.1 200 OK
Accept-Ranges: bytes
Cache-Control: private, no-cache, no-store, proxy-revalidate, no-transform
Connection: keep-alive
Content-Length: 2381
Content-Type: text/html
Date: Sun, 29 Dec 2019 03:49:34 GMT
Etag: "588604c8-94d"
Last-Modified: Mon, 23 Jan 2017 13:27:36 GMT
Pragma: no-cache
Server: bfe/1.0.8.18
Set-Cookie: BDORZ=27315; max-age=86400; domain=.baidu.com; path=/
```
#### 4.4.自定义镜像Tomcat9
① mkdir -p /bruce/mydockerfile/tomcat9
```shell
[root@localhost tomcat9]# pwd
/bruce/mydockerfile/tomcat9
```
② 在上述目录下touch c.txt
```shell
[root@localhost tomcat9]# touch c.txt
[root@localhost tomcat9]# ll
总用量 0
-rw-r--r--. 1 root root 0 6月  18 14:41 c.txt
```
③ 将jdk和tomcat安装的压缩包拷贝进上一步目录
```shell
-rw-r--r--. 1 root root   8900822 12月 18 2016 apache-tomcat-7.0.68.tar.gz
-rw-r--r--. 1 root root         0 6月  18 14:41 c.txt
-rw-r--r--. 1 root root         0 6月  18 14:45 Dockerfile
-rw-r--r--. 1 root root 138094686 12月 13 2016 jdk-7u45-linux-x64.tar.gz
```
④ 在/bruceliu/mydockerfile/tomcat9目录下新建Dockerfile文件
```shell
FROM centos
MAINTAINER    bruce<bruceliu@126.com>
#把宿主机当前上下文的c.txt拷贝到容器/usr/local/路径下
COPY c.txt /usr/local/cincontainer.txt
#把java与tomcat添加到容器中
ADD jdk-7u45-linux-x64.tar.gz /usr/local/
ADD apache-tomcat-7.0.68.tar.gz /usr/local/
#安装vim编辑器
RUN yum -y install vim
#设置工作访问时候的WORKDIR路径，登录落脚点
ENV MYPATH /usr/local
WORKDIR $MYPATH
#配置java与tomcat环境变量
ENV JAVA_HOME /usr/local/jdk1.7.0_45
ENV CLASSPATH $JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
ENV CATALINA_HOME /usr/local/apache-tomcat-7.0.68
ENV CATALINA_BASE /usr/local/apache-tomcat-7.0.68
ENV PATH $PATH:$JAVA_HOME/bin:$CATALINA_HOME/lib:$CATALINA_HOME/bin
#容器运行时监听的端口
EXPOSE  8080
#启动时运行tomcat
# ENTRYPOINT ["/usr/local/apache-tomcat-7.0.68/bin/startup.sh" ]
# CMD ["/usr/local/apache-tomcat-7.0.68/bin/catalina.sh","run"]
CMD /usr/local/apache-tomcat-7.0.68/bin/startup.sh && tail -F /usr/local/apache-tomcat-7.0.68/bin/logs/catalina.out
```
⑤ 构建
```shell
[root@localhost tomcat9]# docker build -t brucrliu_tomcat .
Sending build context to Docker daemon 435.5 MB
Step 1/15 : FROM centos
 ---> 9f38484d220f
Step 2/15 : MAINTAINER bruce<bruceliu@126.com>
 ---> Running in 87cf2c0bccc2
 ---> adc219f3a447
Removing intermediate container 87cf2c0bccc2
Step 3/15 : COPY c.txt /usr/local/cincontainer.txt
 ---> 6e86a4c4315b
Removing intermediate container ef939f65bb42
Step 4/15 : ADD jdk-7u45-linux-x64.tar.gz /usr/local/
 ---> 9127c8831d40
Removing intermediate container 4b2acf47238b
Step 5/15 : ADD apache-tomcat-7.0.68.tar.gz /usr/local/
 ---> 17701beb1f0f
Removing intermediate container ca617b790d8c
Step 6/15 : RUN yum -y install vim
 ---> Running in e7742a8b237e

Loaded plugins: fastestmirror, ovl
Determining fastest mirrors
 * base: mirrors.aliyun.com
 * extras: mirrors.163.com
 * updates: mirrors.aliyun.com
Resolving Dependencies
--> Running transaction check
---> Package vim-enhanced.x86_64 2:7.4.160-6.el7_6 will be installed
--> Processing Dependency: vim-common = 2:7.4.160-6.el7_6 for package: 2:vim-enhanced-7.4.160-6.el7_6.x86_64
--> Processing Dependency: which for package: 2:vim-enhanced-7.4.160-6.el7_6.x86_64
--> Processing Dependency: perl(:MODULE_COMPAT_5.16.3) for package: 2:vim-enhanced-7.4.160-6.el7_6.x86_64
--> Processing Dependency: libperl.so()(64bit) for package: 2:vim-enhanced-7.4.160-6.el7_6.x86_64
--> Processing Dependency: libgpm.so.2()(64bit) for package: 2:vim-enhanced-7.4.160-6.el7_6.x86_64
--> Running transaction check
---> Package gpm-libs.x86_64 0:1.20.7-5.el7 will be installed
---> Package perl.x86_64 4:5.16.3-294.el7_6 will be installed
--> Processing Dependency: perl(Socket) >= 1.3 for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Scalar::Util) >= 1.10 for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl-macros for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(threads::shared) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(threads) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(constant) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Time::Local) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Time::HiRes) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Storable) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Socket) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Scalar::Util) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Pod::Simple::XHTML) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Pod::Simple::Search) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Getopt::Long) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Filter::Util::Call) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(File::Temp) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(File::Spec::Unix) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(File::Spec::Functions) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(File::Spec) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(File::Path) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Exporter) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Cwd) for package: 4:perl-5.16.3-294.el7_6.x86_64
--> Processing Dependency: perl(Carp) for package: 4:perl-5.16.3-294.el7_6.x86_64
---> Package perl-libs.x86_64 4:5.16.3-294.el7_6 will be installed
---> Package vim-common.x86_64 2:7.4.160-6.el7_6 will be installed
--> Processing Dependency: vim-filesystem for package: 2:vim-common-7.4.160-6.el7_6.x86_64
---> Package which.x86_64 0:2.20-7.el7 will be installed
--> Running transaction check
---> Package perl-Carp.noarch 0:1.26-244.el7 will be installed
---> Package perl-Exporter.noarch 0:5.68-3.el7 will be installed
---> Package perl-File-Path.noarch 0:2.09-2.el7 will be installed
---> Package perl-File-Temp.noarch 0:0.23.01-3.el7 will be installed
---> Package perl-Filter.x86_64 0:1.49-3.el7 will be installed
---> Package perl-Getopt-Long.noarch 0:2.40-3.el7 will be installed
--> Processing Dependency: perl(Pod::Usage) >= 1.14 for package: perl-Getopt-Long-2.40-3.el7.noarch
--> Processing Dependency: perl(Text::ParseWords) for package: perl-Getopt-Long-2.40-3.el7.noarch
---> Package perl-PathTools.x86_64 0:3.40-5.el7 will be installed
---> Package perl-Pod-Simple.noarch 1:3.28-4.el7 will be installed
--> Processing Dependency: perl(Pod::Escapes) >= 1.04 for package: 1:perl-Pod-Simple-3.28-4.el7.noarch
--> Processing Dependency: perl(Encode) for package: 1:perl-Pod-Simple-3.28-4.el7.noarch
---> Package perl-Scalar-List-Utils.x86_64 0:1.27-248.el7 will be installed
---> Package perl-Socket.x86_64 0:2.010-4.el7 will be installed
---> Package perl-Storable.x86_64 0:2.45-3.el7 will be installed
---> Package perl-Time-HiRes.x86_64 4:1.9725-3.el7 will be installed
---> Package perl-Time-Local.noarch 0:1.2300-2.el7 will be installed
---> Package perl-constant.noarch 0:1.27-2.el7 will be installed
---> Package perl-macros.x86_64 4:5.16.3-294.el7_6 will be installed
---> Package perl-threads.x86_64 0:1.87-4.el7 will be installed
---> Package perl-threads-shared.x86_64 0:1.43-6.el7 will be installed
---> Package vim-filesystem.x86_64 2:7.4.160-6.el7_6 will be installed
--> Running transaction check
---> Package perl-Encode.x86_64 0:2.51-7.el7 will be installed
---> Package perl-Pod-Escapes.noarch 1:1.04-294.el7_6 will be installed
---> Package perl-Pod-Usage.noarch 0:1.63-3.el7 will be installed
--> Processing Dependency: perl(Pod::Text) >= 3.15 for package: perl-Pod-Usage-1.63-3.el7.noarch
--> Processing Dependency: perl-Pod-Perldoc for package: perl-Pod-Usage-1.63-3.el7.noarch
---> Package perl-Text-ParseWords.noarch 0:3.29-4.el7 will be installed
--> Running transaction check
---> Package perl-Pod-Perldoc.noarch 0:3.20-4.el7 will be installed
--> Processing Dependency: perl(parent) for package: perl-Pod-Perldoc-3.20-4.el7.noarch
--> Processing Dependency: perl(HTTP::Tiny) for package: perl-Pod-Perldoc-3.20-4.el7.noarch
--> Processing Dependency: groff-base for package: perl-Pod-Perldoc-3.20-4.el7.noarch
---> Package perl-podlators.noarch 0:2.5.1-3.el7 will be installed
--> Running transaction check
---> Package groff-base.x86_64 0:1.22.2-8.el7 will be installed
---> Package perl-HTTP-Tiny.noarch 0:0.033-3.el7 will be installed
---> Package perl-parent.noarch 1:0.225-244.el7 will be installed
--> Finished Dependency Resolution

Dependencies Resolved

================================================================================
 Package                    Arch       Version                Repository   Size
================================================================================
Installing:
 vim-enhanced               x86_64     2:7.4.160-6.el7_6      updates     1.0 M
Installing for dependencies:
 gpm-libs                   x86_64     1.20.7-5.el7           base         32 k
 groff-base                 x86_64     1.22.2-8.el7           base        942 k
 perl                       x86_64     4:5.16.3-294.el7_6     updates     8.0 M
 perl-Carp                  noarch     1.26-244.el7           base         19 k
 perl-Encode                x86_64     2.51-7.el7             base        1.5 M
 perl-Exporter              noarch     5.68-3.el7             base         28 k
 perl-File-Path             noarch     2.09-2.el7             base         26 k
 perl-File-Temp             noarch     0.23.01-3.el7          base         56 k
 perl-Filter                x86_64     1.49-3.el7             base         76 k
 perl-Getopt-Long           noarch     2.40-3.el7             base         56 k
 perl-HTTP-Tiny             noarch     0.033-3.el7            base         38 k
 perl-PathTools             x86_64     3.40-5.el7             base         82 k
 perl-Pod-Escapes           noarch     1:1.04-294.el7_6       updates      51 k
 perl-Pod-Perldoc           noarch     3.20-4.el7             base         87 k
 perl-Pod-Simple            noarch     1:3.28-4.el7           base        216 k
 perl-Pod-Usage             noarch     1.63-3.el7             base         27 k
 perl-Scalar-List-Utils     x86_64     1.27-248.el7           base         36 k
 perl-Socket                x86_64     2.010-4.el7            base         49 k
 perl-Storable              x86_64     2.45-3.el7             base         77 k
 perl-Text-ParseWords       noarch     3.29-4.el7             base         14 k
 perl-Time-HiRes            x86_64     4:1.9725-3.el7         base         45 k
 perl-Time-Local            noarch     1.2300-2.el7           base         24 k
 perl-constant              noarch     1.27-2.el7             base         19 k
 perl-libs                  x86_64     4:5.16.3-294.el7_6     updates     688 k
 perl-macros                x86_64     4:5.16.3-294.el7_6     updates      44 k
 perl-parent                noarch     1:0.225-244.el7        base         12 k
 perl-podlators             noarch     2.5.1-3.el7            base        112 k
 perl-threads               x86_64     1.87-4.el7             base         49 k
 perl-threads-shared        x86_64     1.43-6.el7             base         39 k
 vim-common                 x86_64     2:7.4.160-6.el7_6      updates     5.9 M
 vim-filesystem             x86_64     2:7.4.160-6.el7_6      updates      10 k
 which                      x86_64     2.20-7.el7             base         41 k

Transaction Summary
================================================================================
Install  1 Package (+32 Dependent packages)

Total download size: 19 M
Installed size: 63 M
Downloading packages:
Public key for perl-Carp-1.26-244.el7.noarch.rpm is not installed
warning: /var/cache/yum/x86_64/7/base/packages/perl-Carp-1.26-244.el7.noarch.rpm: Header V3 RSA/SHA256 Signature, key ID f4a80eb5: NOKEY
Public key for perl-Pod-Escapes-1.04-294.el7_6.noarch.rpm is not installed
--------------------------------------------------------------------------------
Total                                              2.2 MB/s |  19 MB  00:08     
Retrieving key from file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
Importing GPG key 0xF4A80EB5:
 Userid     : "CentOS-7 Key (CentOS 7 Official Signing Key) <security@centos.org>"
 Fingerprint: 6341 ab27 53d7 8a78 a7c2 7bb1 24c6 a8a7 f4a8 0eb5
 Package    : centos-release-7-6.1810.2.el7.centos.x86_64 (@CentOS)
 From       : /etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
Running transaction check
Running transaction test
Transaction test succeeded
Running transaction
  Installing : gpm-libs-1.20.7-5.el7.x86_64                                1/33 
  Installing : groff-base-1.22.2-8.el7.x86_64                              2/33 
  Installing : 1:perl-parent-0.225-244.el7.noarch                          3/33 
  Installing : perl-HTTP-Tiny-0.033-3.el7.noarch                           4/33 
  Installing : perl-podlators-2.5.1-3.el7.noarch                           5/33 
  Installing : perl-Pod-Perldoc-3.20-4.el7.noarch                          6/33 
  Installing : 1:perl-Pod-Escapes-1.04-294.el7_6.noarch                    7/33 
  Installing : perl-Encode-2.51-7.el7.x86_64                               8/33 
  Installing : perl-Text-ParseWords-3.29-4.el7.noarch                      9/33 
  Installing : perl-Pod-Usage-1.63-3.el7.noarch                           10/33 
  Installing : 4:perl-libs-5.16.3-294.el7_6.x86_64                        11/33 
  Installing : 4:perl-macros-5.16.3-294.el7_6.x86_64                      12/33 
  Installing : perl-Storable-2.45-3.el7.x86_64                            13/33 
  Installing : perl-Exporter-5.68-3.el7.noarch                            14/33 
  Installing : perl-constant-1.27-2.el7.noarch                            15/33 
  Installing : perl-Time-Local-1.2300-2.el7.noarch                        16/33 
  Installing : perl-Socket-2.010-4.el7.x86_64                             17/33 
  Installing : perl-Carp-1.26-244.el7.noarch                              18/33 
  Installing : 4:perl-Time-HiRes-1.9725-3.el7.x86_64                      19/33 
  Installing : perl-PathTools-3.40-5.el7.x86_64                           20/33 
  Installing : perl-Scalar-List-Utils-1.27-248.el7.x86_64                 21/33 
  Installing : 1:perl-Pod-Simple-3.28-4.el7.noarch                        22/33 
  Installing : perl-File-Temp-0.23.01-3.el7.noarch                        23/33 
  Installing : perl-File-Path-2.09-2.el7.noarch                           24/33 
  Installing : perl-threads-shared-1.43-6.el7.x86_64                      25/33 
  Installing : perl-threads-1.87-4.el7.x86_64                             26/33 
  Installing : perl-Filter-1.49-3.el7.x86_64                              27/33 
  Installing : perl-Getopt-Long-2.40-3.el7.noarch                         28/33 
  Installing : 4:perl-5.16.3-294.el7_6.x86_64                             29/33 
  Installing : which-2.20-7.el7.x86_64                                    30/33 
install-info: No such file or directory for /usr/share/info/which.info.gz
  Installing : 2:vim-filesystem-7.4.160-6.el7_6.x86_64                    31/33 
  Installing : 2:vim-common-7.4.160-6.el7_6.x86_64                        32/33 
  Installing : 2:vim-enhanced-7.4.160-6.el7_6.x86_64                      33/33 
  Verifying  : perl-HTTP-Tiny-0.033-3.el7.noarch                           1/33 
  Verifying  : perl-threads-shared-1.43-6.el7.x86_64                       2/33 
  Verifying  : perl-Storable-2.45-3.el7.x86_64                             3/33 
  Verifying  : 1:perl-Pod-Escapes-1.04-294.el7_6.noarch                    4/33 
  Verifying  : perl-Exporter-5.68-3.el7.noarch                             5/33 
  Verifying  : perl-constant-1.27-2.el7.noarch                             6/33 
  Verifying  : perl-PathTools-3.40-5.el7.x86_64                            7/33 
  Verifying  : 2:vim-filesystem-7.4.160-6.el7_6.x86_64                     8/33 
  Verifying  : 1:perl-parent-0.225-244.el7.noarch                          9/33 
  Verifying  : which-2.20-7.el7.x86_64                                    10/33 
  Verifying  : 4:perl-libs-5.16.3-294.el7_6.x86_64                        11/33 
  Verifying  : groff-base-1.22.2-8.el7.x86_64                             12/33 
  Verifying  : perl-File-Temp-0.23.01-3.el7.noarch                        13/33 
  Verifying  : 1:perl-Pod-Simple-3.28-4.el7.noarch                        14/33 
  Verifying  : perl-Time-Local-1.2300-2.el7.noarch                        15/33 
  Verifying  : gpm-libs-1.20.7-5.el7.x86_64                               16/33 
  Verifying  : 4:perl-macros-5.16.3-294.el7_6.x86_64                      17/33 
  Verifying  : perl-Socket-2.010-4.el7.x86_64                             18/33 
  Verifying  : perl-Carp-1.26-244.el7.noarch                              19/33 
  Verifying  : 4:perl-Time-HiRes-1.9725-3.el7.x86_64                      20/33 
  Verifying  : perl-Scalar-List-Utils-1.27-248.el7.x86_64                 21/33 
  Verifying  : 2:vim-common-7.4.160-6.el7_6.x86_64                        22/33 
  Verifying  : perl-Pod-Usage-1.63-3.el7.noarch                           23/33 
  Verifying  : perl-Encode-2.51-7.el7.x86_64                              24/33 
  Verifying  : 2:vim-enhanced-7.4.160-6.el7_6.x86_64                      25/33 
  Verifying  : perl-Pod-Perldoc-3.20-4.el7.noarch                         26/33 
  Verifying  : perl-podlators-2.5.1-3.el7.noarch                          27/33 
  Verifying  : perl-File-Path-2.09-2.el7.noarch                           28/33 
  Verifying  : perl-threads-1.87-4.el7.x86_64                             29/33 
  Verifying  : perl-Filter-1.49-3.el7.x86_64                              30/33 
  Verifying  : perl-Getopt-Long-2.40-3.el7.noarch                         31/33 
  Verifying  : perl-Text-ParseWords-3.29-4.el7.noarch                     32/33 
  Verifying  : 4:perl-5.16.3-294.el7_6.x86_64                             33/33 

Installed:
  vim-enhanced.x86_64 2:7.4.160-6.el7_6                                         

Dependency Installed:
  gpm-libs.x86_64 0:1.20.7-5.el7                                                
  groff-base.x86_64 0:1.22.2-8.el7                                              
  perl.x86_64 4:5.16.3-294.el7_6                                                
  perl-Carp.noarch 0:1.26-244.el7                                               
  perl-Encode.x86_64 0:2.51-7.el7                                               
  perl-Exporter.noarch 0:5.68-3.el7                                             
  perl-File-Path.noarch 0:2.09-2.el7                                            
  perl-File-Temp.noarch 0:0.23.01-3.el7                                         
  perl-Filter.x86_64 0:1.49-3.el7                                               
  perl-Getopt-Long.noarch 0:2.40-3.el7                                          
  perl-HTTP-Tiny.noarch 0:0.033-3.el7                                           
  perl-PathTools.x86_64 0:3.40-5.el7                                            
  perl-Pod-Escapes.noarch 1:1.04-294.el7_6                                      
  perl-Pod-Perldoc.noarch 0:3.20-4.el7                                          
  perl-Pod-Simple.noarch 1:3.28-4.el7                                           
  perl-Pod-Usage.noarch 0:1.63-3.el7                                            
  perl-Scalar-List-Utils.x86_64 0:1.27-248.el7                                  
  perl-Socket.x86_64 0:2.010-4.el7                                              
  perl-Storable.x86_64 0:2.45-3.el7                                             
  perl-Text-ParseWords.noarch 0:3.29-4.el7                                      
  perl-Time-HiRes.x86_64 4:1.9725-3.el7                                         
  perl-Time-Local.noarch 0:1.2300-2.el7                                         
  perl-constant.noarch 0:1.27-2.el7                                             
  perl-libs.x86_64 4:5.16.3-294.el7_6                                           
  perl-macros.x86_64 4:5.16.3-294.el7_6                                         
  perl-parent.noarch 1:0.225-244.el7                                            
  perl-podlators.noarch 0:2.5.1-3.el7                                           
  perl-threads.x86_64 0:1.87-4.el7                                              
  perl-threads-shared.x86_64 0:1.43-6.el7                                       
  vim-common.x86_64 2:7.4.160-6.el7_6                                           
  vim-filesystem.x86_64 2:7.4.160-6.el7_6                                       
  which.x86_64 0:2.20-7.el7                                                     

Complete!
 ---> 68842422f5bb
Removing intermediate container e7742a8b237e
Step 7/15 : ENV MYPATH /usr/local
 ---> Running in 378f507c7454
 ---> 68772160bbba
Removing intermediate container 378f507c7454
Step 8/15 : WORKDIR $MYPATH
 ---> 8f720fb47d8d
Removing intermediate container 1fef2cdcd39f
Step 9/15 : ENV JAVA_HOME /usr/local/jdk1.7.0_45
 ---> Running in 031f617faaf8
 ---> 8858a95269f7
Removing intermediate container 031f617faaf8
Step 10/15 : ENV CLASSPATH $JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
 ---> Running in 81fea7bdedde
 ---> 677ff52ce8bf
Removing intermediate container 81fea7bdedde
Step 11/15 : ENV CATALINA_HOME /usr/local/apache-tomcat-7.0.68
 ---> Running in d8868d0fbf8a
 ---> 1337cdd4ae56
Removing intermediate container d8868d0fbf8a
Step 12/15 : ENV CATALINA_BASE /usr/local/apache-tomcat-7.0.68
 ---> Running in 3b1b0926e692
 ---> d63c27b9e986
Removing intermediate container 3b1b0926e692
Step 13/15 : ENV PATH $PATH:$JAVA_HOME/bin:$CATALINA_HOME/lib:$CATALINA_HOME/bin
 ---> Running in c1540c867550
 ---> 363e3b5be57d
Removing intermediate container c1540c867550
Step 14/15 : EXPOSE 8080
 ---> Running in 0557437d1396
 ---> bddf5d2259c1
Removing intermediate container 0557437d1396
Step 15/15 : CMD /usr/local/apache-tomcat-7.0.68/bin/startup.sh && tail -F /usr/local/apache-tomcat-7.0.68/bin/logs/catalina.out
 ---> Running in 54733e8ad7a8
 ---> caefcd845de4
Removing intermediate container 54733e8ad7a8
Successfully built caefcd845de4
```
⑥ 构建完成
```shell
[root@localhost tomcat9]# docker images
REPOSITORY            TAG                 IMAGE ID            CREATED                  SIZE
docker.io/mongo       latest              9c02a5a12c52        Less than a second ago   413 MB
docker.io/tomcat      latest              238e6d7313e3        Less than a second ago   506 MB
brucrliu_tomcat       latest              caefcd845de4        2 minutes ago            669 MB
myip1_centos          latest              274c58d0c1e4        19 minutes ago           314 MB
myip_centos           latest              41868e4e4fee        36 minutes ago           314 MB
mycentos              1.3                 720d4eb5c5f0        About an hour ago        480 MB
bruceliu/centos9527   latest              071e944a589b        9 hours ago              202 MB
bruceliu/tomcat       7-jre7              a66f9aba334d        14 hours ago             359 MB
docker.io/tomcat      7-jre7              47c156f4d4e3        5 weeks ago              359 MB
docker.io/mysql       latest              7bb2586065cd        2 months ago             477 MB
docker.io/centos      7                   9f38484d220f        3 months ago             202 MB
docker.io/centos      latest              9f38484d220f        3 months ago             202 MB
```
⑦ run
```shell
[root@localhost tomcat9]# docker run -d -p 9080:8080 --name mycat7 -v /brucrliu/mydockerfile/tomcat7/test:/usr/local/apache-tomcat-7.0.68/webapps/test -v /bruceliu/mydockerfile/tomcat7/tomcat9logs/:/usr/local/apache-tomcat-7.0.68/logs --privileged=true brucrliu_tomcat
8da490d0e6c81b0e78c530bd90717e2e7f187266329c9e720819b57e7b2c2466
```
备注：
Docker挂载主机目录Docker访问出现cannot open directory .: Permission denied
解决办法：在挂载目录后多加一个--privileged=true参数即可
⑧  验证
```shell
[root@localhost tomcat9]# docker ps -l
CONTAINER ID        IMAGE               COMMAND                  CREATED              STATUS              PORTS                    NAMES
8da490d0e6c8        brucrliu_tomcat     "/bin/sh -c '/usr/..."   About a minute ago   Up About a minute   0.0.0.0:9080->8080/tcp   mycat7
```
⑨ 访问
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807214304601.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
⑩ 结合前述的容器卷将测试的web服务test发布
```shell
[root@localhost test]# pwd
/brucrliu/mydockerfile/tomcat7/test
[root@localhost test]# ll
总用量 4
-rw-r--r--. 1 root root 23 6月  18 15:04 1.html
[root@localhost test]# vi 1.html 
<h1>Hello,Test!!!</h1>
```
测试访问：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807214349284.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
### 5.小结
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190807214428614.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

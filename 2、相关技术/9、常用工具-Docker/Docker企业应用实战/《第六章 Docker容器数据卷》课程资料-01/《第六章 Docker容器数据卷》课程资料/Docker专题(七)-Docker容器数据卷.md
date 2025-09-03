### 1.数据卷的概念
先来看看Docker的理念：
*  将运用与运行的环境打包形成容器运行 ，运行可以伴随着容器，但是我们对数据的要求希望是持久化的
*  容器之间希望有可能共享数据

Docker容器产生的数据，如果不通过docker commit生成新的镜像，使得数据做为镜像的一部分保存下来，那么当容器删除后，数据自然也就没有了。为了能保存数据在docker中我们使用卷。
>一句话：有点类似我们Redis里面的rdb和aof文件!   

```
宿主机---容器   数据共享 挂载！
容器-----容器   数据共享 数据卷！
```



### 2.数据卷的作用

 卷就是目录或文件，存在于一个或多个容器中，由docker挂载到容器，但不属于联合文件系统，因此能够绕过Union File System提供一些用于持续存储或共享数据的特性：

 卷的设计目的就是数据的持久化，完全独立于容器的生存周期，因此Docker不会在容器删除时删除其挂载的数据卷！

```
容器删除了，那么容器中的数据可以通过数据卷这种方式持久化保存下来！
```

特点：
1：数据卷可在容器之间共享或重用数据
2：卷中的更改可以直接生效
3：数据卷中的更改不会包含在镜像的更新中
4：数据卷的生命周期一直持续到没有容器使用它为止

>容器的持久化
>
>容器间继承+共享数据


### 3.数据卷的操作
#### 3.1.直接命令添加
语法：
>docker run -it -v /宿主机绝对路径目录:/容器内目录 镜像名

示例：
```java
[root@localhost ~]# docker run -di --name=mycentos0727 -v /localdata:/containerdata centos:7
c54a48d9e8479c7518d9aa4edd6cae5c9f453ceea878cd6d71423896f4f37724
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
c54a48d9e847        centos:7            "/bin/bash"         15 seconds ago      Up 14 seconds                           mycentos0727
[root@localhost ~]# docker exec -it  mycentos0727 /bin/bash
[root@c54a48d9e847 /]# ls -l
total 12
-rw-r--r--.   1 root root 12082 Mar  5 17:36 anaconda-post.log
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 bin -> usr/bin
drwxr-xr-x.   2 root root     6 Jun 17 19:55 containerdata
drwxr-xr-x.   5 root root   340 Jun 17 19:55 dev
drwxr-xr-x.   1 root root    66 Jun 17 19:55 etc
drwxr-xr-x.   2 root root     6 Apr 11  2018 home
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 lib -> usr/lib
lrwxrwxrwx.   1 root root     9 Mar  5 17:34 lib64 -> usr/lib64
drwxr-xr-x.   2 root root     6 Apr 11  2018 media
drwxr-xr-x.   2 root root     6 Apr 11  2018 mnt
drwxr-xr-x.   2 root root     6 Apr 11  2018 opt
dr-xr-xr-x. 119 root root     0 Jun 17 19:55 proc
dr-xr-x---.   2 root root   114 Mar  5 17:36 root
drwxr-xr-x.   1 root root    21 Jun 17 19:55 run
lrwxrwxrwx.   1 root root     8 Mar  5 17:34 sbin -> usr/sbin
drwxr-xr-x.   2 root root     6 Apr 11  2018 srv
dr-xr-xr-x.  13 root root     0 Jun 10 16:40 sys
drwxrwxrwt.   7 root root   132 Mar  5 17:36 tmp
drwxr-xr-x.  13 root root   155 Mar  5 17:34 usr
drwxr-xr-x.  18 root root   238 Mar  5 17:34 var
[root@c54a48d9e847 /]# cd containerdata/
[root@c54a48d9e847 containerdata]# ls -l
total 0
-rw-r--r--. 1 root root 0 Jun 17 19:58 1.txt
[root@c54a48d9e847 containerdata]# cat 1.txt 
[root@c54a48d9e847 containerdata]# cat 1.txt 
23423432
234234234
[root@c54a48d9e847 containerdata]# 
```
查看数据卷是否挂载成功
>docker inspect 容器ID
```xml
"HostConfig": {
            "Binds": [
                "/localdata:/containerdata"
            ],
            "ContainerIDFile": "",
            "LogConfig": {
                "Type": "journald",
                "Config": {}
            },
            "NetworkMode": "default",
            "PortBindings": {},
            "RestartPolicy": {
                "Name": "no",
                "MaximumRetryCount": 0
            },
```
容器和宿主机之间数据共享
```java
[root@localhost ~]# docker run -di --name=mycentos0727 -v /localdata:/containerdata centos:7
c54a48d9e8479c7518d9aa4edd6cae5c9f453ceea878cd6d71423896f4f37724
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
c54a48d9e847        centos:7            "/bin/bash"         15 seconds ago      Up 14 seconds                           mycentos0727
[root@localhost ~]# docker exec -it  mycentos0727 /bin/bash
[root@c54a48d9e847 /]# ls -l
total 12
-rw-r--r--.   1 root root 12082 Mar  5 17:36 anaconda-post.log
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 bin -> usr/bin
drwxr-xr-x.   2 root root     6 Jun 17 19:55 containerdata
drwxr-xr-x.   5 root root   340 Jun 17 19:55 dev
drwxr-xr-x.   1 root root    66 Jun 17 19:55 etc
drwxr-xr-x.   2 root root     6 Apr 11  2018 home
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 lib -> usr/lib
lrwxrwxrwx.   1 root root     9 Mar  5 17:34 lib64 -> usr/lib64
drwxr-xr-x.   2 root root     6 Apr 11  2018 media
drwxr-xr-x.   2 root root     6 Apr 11  2018 mnt
drwxr-xr-x.   2 root root     6 Apr 11  2018 opt
dr-xr-xr-x. 119 root root     0 Jun 17 19:55 proc
dr-xr-x---.   2 root root   114 Mar  5 17:36 root
drwxr-xr-x.   1 root root    21 Jun 17 19:55 run
lrwxrwxrwx.   1 root root     8 Mar  5 17:34 sbin -> usr/sbin
drwxr-xr-x.   2 root root     6 Apr 11  2018 srv
dr-xr-xr-x.  13 root root     0 Jun 10 16:40 sys
drwxrwxrwt.   7 root root   132 Mar  5 17:36 tmp
drwxr-xr-x.  13 root root   155 Mar  5 17:34 usr
drwxr-xr-x.  18 root root   238 Mar  5 17:34 var
[root@c54a48d9e847 /]# cd containerdata/
[root@c54a48d9e847 containerdata]# ls -l
total 0
-rw-r--r--. 1 root root 0 Jun 17 19:58 1.txt
[root@c54a48d9e847 containerdata]# cat 1.txt 
[root@c54a48d9e847 containerdata]# cat 1.txt 
23423432
234234234
[root@c54a48d9e847 containerdata]   

[root@localhost localdata]# pwd
/localdata
[root@localhost localdata]# ll
总用量 4
-rw-r--r--. 1 root root 19 6月  18 03:59 1.txt
[root@localhost localdata]# cat 1.txt 
23423432
234234234
[root@localhost localdata]# 
```
容器停止退出后，主机修改后数据是否同步
容器先停止退出
```java
docker stop c54a48d9e847
```
主机修改文件
容器重新启动
```java
docker start c54a48d9e847
docker exec -it  mycentos0727 /bin/bash
```
查看挂载的目录文件
```java
[root@c54a48d9e847 /]# cd containerdata/
[root@c54a48d9e847 containerdata]# ll
total 8
-rw-r--r--. 1 root root   19 Jun 17 19:59 1.txt
-rw-r--r--. 1 root root 1555 Jun 17 20:14 2.txt
[root@c54a48d9e847 containerdata]# cat 2.txt 
```
发现内容还是同步的.
备注：
>命令:
>docker run -it -v /宿主机绝对路径目录:/容器内目录:ro 镜像名
>![在这里插入图片描述](https://img-blog.csdnimg.cn/20190728233500465.png)
#### 3.2.DockerFile添加
根目录下新建mydocker文件夹并进入
可在Dockerfile中使用VOLUME指令来给镜像添加一个或多个数据卷

>VOLUME["/dataVolumeContainer","/dataVolumeContainer2","/dataVolumeContainer3"]

说明：
>出于可移植和分享的考虑，用-v 主机目录:容器目录这种方法不能够直接在Dockerfile中实现。
由于宿主机目录是依赖于特定宿主机的，并不能够保证在所有的宿主机上都存在这样的特定目录。

- File构建
```java
[root@localhost /]# mkdir mydocker
[root@localhost /]# cd mydocker
[root@localhost /mydocker]# vi mydockerfile
# volume test
FROM centos
VOLUME ["/dataVolumeContainer1","/dataVolumeContainer2"]
CMD echo "finished,--------success1"
CMD /bin/bash
```
- build后生成镜像
```java
[root@localhost mydocker]# docker build -f /mydocker/mydockerfile -t bruceliu/centos9527 .
Sending build context to Docker daemon 2.048 kB
Step 1/4 : FROM centos
 ---> 9f38484d220f
Step 2/4 : VOLUME /dataVolumeContainer1 /dataVolumeContainer2
 ---> Running in 1b286372176e
 ---> 96c20bf65a22
Removing intermediate container 1b286372176e
Step 3/4 : CMD echo "finished,--------success1"
 ---> Running in 316de82a95a1
 ---> 763314c45154
Removing intermediate container 316de82a95a1
Step 4/4 : CMD /bin/bash
 ---> Running in 4926a2fb1fa3
 ---> 071e944a589b
Removing intermediate container 4926a2fb1fa3
Successfully built 071e944a589b

[root@localhost mydocker]# docker images
REPOSITORY            TAG                 IMAGE ID            CREATED             SIZE
bruceliu/centos9527   latest              071e944a589b        41 seconds ago      202 MB
bruceliu/tomcat       7-jre7              a66f9aba334d        4 hours ago         359 MB
docker.io/tomcat      7-jre7              47c156f4d4e3        4 weeks ago         359 MB
docker.io/mysql       latest              7bb2586065cd        2 months ago        477 MB
docker.io/centos      7                   9f38484d220f        3 months ago        202 MB
docker.io/centos      latest              9f38484d220f        3 months ago        202 MB
```
获得一个新镜像bruceliu/centos9527
- run容器
```java
[root@localhost mydocker]# docker run -it 071e944a589b /bin/bash
[root@84e0c3a6dcdf /]# ls -l
total 12
-rw-r--r--.   1 root root 12082 Mar  5 17:36 anaconda-post.log
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 bin -> usr/bin
drwxr-xr-x.   2 root root     6 Jun 17 21:17 dataVolumeContainer1
drwxr-xr-x.   2 root root     6 Jun 17 21:18 dataVolumeContainer2
drwxr-xr-x.   5 root root   360 Jun 17 21:18 dev
drwxr-xr-x.   1 root root    66 Jun 17 21:17 etc
drwxr-xr-x.   2 root root     6 Apr 11  2018 home
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 lib -> usr/lib
lrwxrwxrwx.   1 root root     9 Mar  5 17:34 lib64 -> usr/lib64
drwxr-xr-x.   2 root root     6 Apr 11  2018 media
drwxr-xr-x.   2 root root     6 Apr 11  2018 mnt
drwxr-xr-x.   2 root root     6 Apr 11  2018 opt
dr-xr-xr-x. 119 root root     0 Jun 17 21:18 proc
dr-xr-x---.   2 root root   114 Mar  5 17:36 root
drwxr-xr-x.   1 root root    21 Jun 17 21:18 run
lrwxrwxrwx.   1 root root     8 Mar  5 17:34 sbin -> usr/sbin
drwxr-xr-x.   2 root root     6 Apr 11  2018 srv
dr-xr-xr-x.  13 root root     0 Jun 10 16:40 sys
drwxrwxrwt.   7 root root   132 Mar  5 17:36 tmp
drwxr-xr-x.  13 root root   155 Mar  5 17:34 usr
drwxr-xr-x.  18 root root   238 Mar  5 17:34 var
```
通过上述步骤，容器内的卷目录地址已经知道
对应的主机目录地址哪？？
```java
[root@84e0c3a6dcdf /]# cd dataVolumeContainer
bash: cd: dataVolumeContainer: No such file or directory
[root@84e0c3a6dcdf /]# 
[root@84e0c3a6dcdf /]# ls -l 
total 12
-rw-r--r--.   1 root root 12082 Mar  5 17:36 anaconda-post.log
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 bin -> usr/bin
drwxr-xr-x.   2 root root     6 Jun 17 21:17 dataVolumeContainer1
drwxr-xr-x.   2 root root     6 Jun 17 21:18 dataVolumeContainer2
drwxr-xr-x.   5 root root   360 Jun 17 21:18 dev
drwxr-xr-x.   1 root root    66 Jun 17 21:17 etc
drwxr-xr-x.   2 root root     6 Apr 11  2018 home
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 lib -> usr/lib
lrwxrwxrwx.   1 root root     9 Mar  5 17:34 lib64 -> usr/lib64
drwxr-xr-x.   2 root root     6 Apr 11  2018 media
drwxr-xr-x.   2 root root     6 Apr 11  2018 mnt
drwxr-xr-x.   2 root root     6 Apr 11  2018 opt
dr-xr-xr-x. 119 root root     0 Jun 17 21:18 proc
dr-xr-x---.   2 root root   114 Mar  5 17:36 root
drwxr-xr-x.   1 root root    21 Jun 17 21:18 run
lrwxrwxrwx.   1 root root     8 Mar  5 17:34 sbin -> usr/sbin
drwxr-xr-x.   2 root root     6 Apr 11  2018 srv
dr-xr-xr-x.  13 root root     0 Jun 10 16:40 sys
drwxrwxrwt.   7 root root   132 Mar  5 17:36 tmp
drwxr-xr-x.  13 root root   155 Mar  5 17:34 usr
drwxr-xr-x.  18 root root   238 Mar  5 17:34 var
[root@84e0c3a6dcdf /]# cd dataVolumeContainer1
[root@84e0c3a6dcdf dataVolumeContainer1]# pwd
/dataVolumeContainer1
[root@84e0c3a6dcdf dataVolumeContainer1]# touch 1.txt
[root@84e0c3a6dcdf dataVolumeContainer1]# 
```
- 宿主机
```java
[root@localhost _data]# pwd
/var/lib/docker/volumes/47f4c73f4f71d3376b7f3df75e2e6877c50f7c1446a1cc2ebe07d82387b6372b/_data
[root@localhost _data]# ll
总用量 0
-rw-r--r--. 1 root root 0 6月  18 05:19 1.txt
-rw-r--r--. 1 root root 0 6月  18 05:22 2.txt
```
备注：
>Docker挂载主机目录Docker访问出现cannot open directory .: Permission denied
解决办法：在挂载目录后多加一个--privileged=true参数即可

### 4.数据卷容器
#### 4.1.是什么
命名的容器挂载数据卷，其它容器通过挂载这个(父容器)实现数据共享，挂载数据卷的容器，称之为数据卷容器.
#### 4.2.总体介绍
以上一步新建的镜像bruceliu/cento9527为模板并运行容器dc01/dc02/dc03
它们已经具有容器卷
/dataVolumeContainer1
/dataVolumeContainer2
#### 4.3.容器间传递共享(--volumes-from)
- 先启动一个父容器dc01
```java
[root@localhost /]# docker images
REPOSITORY            TAG                 IMAGE ID            CREATED             SIZE
bruceliu/centos9527   latest              071e944a589b        17 minutes ago      202 MB
bruceliu/tomcat       7-jre7              a66f9aba334d        4 hours ago         359 MB
docker.io/tomcat      7-jre7              47c156f4d4e3        4 weeks ago         359 MB
docker.io/mysql       latest              7bb2586065cd        2 months ago        477 MB
docker.io/centos      7                   9f38484d220f        3 months ago        202 MB
docker.io/centos      latest              9f38484d220f        3 months ago        202 MB
[root@localhost /]# 
[root@localhost /]# docker run -it --name dc01 bruceliu/centos9527
[root@43266366c5bc /]# ls -l
total 12
-rw-r--r--.   1 root root 12082 Mar  5 17:36 anaconda-post.log
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 bin -> usr/bin
drwxr-xr-x.   2 root root     6 Jun 17 21:33 dataVolumeContainer1
drwxr-xr-x.   2 root root     6 Jun 17 21:33 dataVolumeContainer2
drwxr-xr-x.   5 root root   360 Jun 17 21:33 dev
drwxr-xr-x.   1 root root    66 Jun 17 21:33 etc
drwxr-xr-x.   2 root root     6 Apr 11  2018 home
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 lib -> usr/lib
lrwxrwxrwx.   1 root root     9 Mar  5 17:34 lib64 -> usr/lib64
drwxr-xr-x.   2 root root     6 Apr 11  2018 media
drwxr-xr-x.   2 root root     6 Apr 11  2018 mnt
drwxr-xr-x.   2 root root     6 Apr 11  2018 opt
dr-xr-xr-x. 116 root root     0 Jun 17 21:33 proc
dr-xr-x---.   2 root root   114 Mar  5 17:36 root
drwxr-xr-x.   1 root root    21 Jun 17 21:33 run
lrwxrwxrwx.   1 root root     8 Mar  5 17:34 sbin -> usr/sbin
drwxr-xr-x.   2 root root     6 Apr 11  2018 srv
dr-xr-xr-x.  13 root root     0 Jun 10 16:40 sys
drwxrwxrwt.   7 root root   132 Mar  5 17:36 tmp
drwxr-xr-x.  13 root root   155 Mar  5 17:34 usr
drwxr-xr-x.  18 root root   238 Mar  5 17:34 var
[root@43266366c5bc /]# cd dataVolumeContainer2
[root@43266366c5bc dataVolumeContainer2]# touch dc01_add.txt
```
dc02/dc03继承自dc01
命令：--volumes-from
>docker run -it --name dc02 --volumes-from dc01 zzyy/centos
```java
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE                 COMMAND                  CREATED             STATUS              PORTS               NAMES
43266366c5bc        bruceliu/centos9527   "/bin/sh -c /bin/bash"   2 minutes ago       Up 2 minutes                            dc01
[root@localhost ~]# docker attach 43266366c5bc
[root@43266366c5bc dataVolumeContainer2]# ls -l
total 0
-rw-r--r--. 1 root root 0 Jun 17 21:34 dc01_add.txt
[root@43266366c5bc dataVolumeContainer2]# exit
exit
[root@localhost ~]# docker run -it --name dc02 --volumes-from dc01 bruceliu/centos9527
[root@8b6c85d88968 /]# cd dataVolumeContainer2
[root@8b6c85d88968 dataVolumeContainer2]# ls -l
total 0
-rw-r--r--. 1 root root 0 Jun 17 21:34 dc01_add.txt
[root@8b6c85d88968 dataVolumeContainer2]# touch dc02_add.txt
[root@8b6c85d88968 dataVolumeContainer2]# ls -l
total 0
-rw-r--r--. 1 root root 0 Jun 17 21:34 dc01_add.txt
-rw-r--r--. 1 root root 0 Jun 17 21:38 dc02_add.txt
[root@8b6c85d88968 dataVolumeContainer2]# exit
exit
[root@localhost ~]# docker run -it --name dc03 --volumes-from dc01 bruceliu/centos9527

[root@2d3ff41a5f00 /]# 
[root@2d3ff41a5f00 /]# cd dataVolumeContainer2
[root@2d3ff41a5f00 dataVolumeContainer2]# ls -l
total 0
-rw-r--r--. 1 root root 0 Jun 17 21:34 dc01_add.txt
-rw-r--r--. 1 root root 0 Jun 17 21:38 dc02_add.txt
[root@2d3ff41a5f00 dataVolumeContainer2]# touch dc03_add.txt
[root@2d3ff41a5f00 dataVolumeContainer2]# 
```
回到dc01可以看到02/03各自添加的都能共享了
```java
[root@localhost /]# docker ps
CONTAINER ID        IMAGE                 COMMAND                  CREATED             STATUS              PORTS               NAMES
2d3ff41a5f00        bruceliu/centos9527   "/bin/sh -c /bin/bash"   2 minutes ago       Up 2 minutes                            dc03
8b6c85d88968        bruceliu/centos9527   "/bin/sh -c /bin/bash"   3 minutes ago       Up 6 seconds                            dc02
43266366c5bc        bruceliu/centos9527   "/bin/sh -c /bin/bash"   8 minutes ago       Up 9 seconds                            dc01
[root@localhost /]# docker attach 43266366c5bc
[root@43266366c5bc /]# ls -l
total 12
-rw-r--r--.   1 root root 12082 Mar  5 17:36 anaconda-post.log
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 bin -> usr/bin
drwxr-xr-x.   2 root root     6 Jun 17 21:33 dataVolumeContainer1
drwxr-xr-x.   2 root root    66 Jun 17 21:39 dataVolumeContainer2
drwxr-xr-x.   5 root root   360 Jun 17 21:41 dev
drwxr-xr-x.   1 root root    66 Jun 17 21:33 etc
drwxr-xr-x.   2 root root     6 Apr 11  2018 home
lrwxrwxrwx.   1 root root     7 Mar  5 17:34 lib -> usr/lib
lrwxrwxrwx.   1 root root     9 Mar  5 17:34 lib64 -> usr/lib64
drwxr-xr-x.   2 root root     6 Apr 11  2018 media
drwxr-xr-x.   2 root root     6 Apr 11  2018 mnt
drwxr-xr-x.   2 root root     6 Apr 11  2018 opt
dr-xr-xr-x. 122 root root     0 Jun 17 21:41 proc
dr-xr-x---.   1 root root    27 Jun 17 21:37 root
drwxr-xr-x.   1 root root    21 Jun 17 21:33 run
lrwxrwxrwx.   1 root root     8 Mar  5 17:34 sbin -> usr/sbin
drwxr-xr-x.   2 root root     6 Apr 11  2018 srv
dr-xr-xr-x.  13 root root     0 Jun 10 16:40 sys
drwxrwxrwt.   7 root root   132 Mar  5 17:36 tmp
drwxr-xr-x.  13 root root   155 Mar  5 17:34 usr
drwxr-xr-x.  18 root root   238 Mar  5 17:34 var
[root@43266366c5bc /]# cd dataVolumeContainer2
[root@43266366c5bc dataVolumeContainer2]# 
[root@43266366c5bc dataVolumeContainer2]# ls -l
total 0
-rw-r--r--. 1 root root 0 Jun 17 21:34 dc01_add.txt
-rw-r--r--. 1 root root 0 Jun 17 21:38 dc02_add.txt
-rw-r--r--. 1 root root 0 Jun 17 21:39 dc03_add.txt
```
删除dc01，dc02修改后dc03可否访问
```java
[root@localhost /]# docker ps
CONTAINER ID        IMAGE                 COMMAND                  CREATED             STATUS              PORTS               NAMES
2d3ff41a5f00        bruceliu/centos9527   "/bin/sh -c /bin/bash"   5 minutes ago       Up 5 minutes                            dc03
8b6c85d88968        bruceliu/centos9527   "/bin/sh -c /bin/bash"   7 minutes ago       Up 3 minutes                            dc02
[root@localhost /]# docker attach dc02
[root@8b6c85d88968 /]# cd dataVolumeContainer2
[root@8b6c85d88968 dataVolumeContainer2]# ls -l
total 0
-rw-r--r--. 1 root root 0 Jun 17 21:34 dc01_add.txt
-rw-r--r--. 1 root root 0 Jun 17 21:38 dc02_add.txt
-rw-r--r--. 1 root root 0 Jun 17 21:39 dc03_add.txt
[root@8b6c85d88968 dataVolumeContainer2]# touch dc02_add_new.txt
[root@8b6c85d88968 dataVolumeContainer2]# exit 
exit
[root@localhost /]# docker attach dc03
[root@2d3ff41a5f00 dataVolumeContainer2]# ls -l
total 0
-rw-r--r--. 1 root root 0 Jun 17 21:34 dc01_add.txt
-rw-r--r--. 1 root root 0 Jun 17 21:38 dc02_add.txt
-rw-r--r--. 1 root root 0 Jun 17 21:45 dc02_add_new.txt
-rw-r--r--. 1 root root 0 Jun 17 21:39 dc03_add.txt
```
结论：
>容器之间配置信息的传递，数据卷的生命周期一直持续到没有容器使用它为止



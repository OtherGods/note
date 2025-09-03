### 1.Docker镜像(images)概念

镜像是一种轻量级、可执行的独立软件包，用来打包软件运行环境和基于运行环境开发的软件，它包含运行某个软件所需的所有内容，包括代码、运行时、库、环境变量和配置文件。

### 2.UnionFS（联合文件系统）

UnionFS（联合文件系统）：Union文件系统（UnionFS）是一种分层、轻量级并且高性能的文件系统，它支持对文件系统的修改作为一次提交来一层层的叠加，同时可以将不同目录挂载到同一个虚拟文件系统下(unite several directories into a single virtual filesystem)。Union 文件系统是 Docker 镜像的基础。镜像可以通过分层来进行继承，基于基础镜像（没有父镜像），可以制作各种具体的应用镜像。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190728183733291.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
特性：一次同时加载多个文件系统，但从外面看起来，只能看到一个文件系统，联合加载会把各层文件系统叠加起来，这样最终的文件系统会包含所有底层的文件和目录。

### 3.Docker镜像加载原理

**docker的镜像实际上由一层一层的文件系统组成，这种层级的文件系统UnionFS。**

bootfs(boot file system)主要包含bootloader和kernel, bootloader主要是引导加载kernel, Linux刚启动时会加载bootfs文件系统，在Docker镜像的最底层是bootfs。这一层与我们典型的Linux/Unix系统是一样的，包含boot加载器和内核。当boot加载完成之后整个内核就都在内存中了，此时内存的使用权已由bootfs转交给内核，此时系统也会卸载bootfs。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190728183811559.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
rootfs (root file system) ，在bootfs之上。包含的就是典型 Linux 系统中的 /dev, /proc, /bin, /etc 等标准目录和文件。rootfs就是各种不同的操作系统发行版，比如Ubuntu，Centos等等。  

平时我们安装进虚拟机的CentOS都是好几个G，为什么docker这里才200M？？
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190728184313118.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
对于一个精简的OS，rootfs可以很小，只需要包括最基本的命令、工具和程序库就可以了，因为底层直接用Host的kernel，自己只需要提供 rootfs 就行了。由此可见对于不同的linux发行版, bootfs基本是一致的, rootfs会有差别, 因此不同的发行版可以公用bootfs。

### 4.分层的镜像
以我们的pull为例，在下载的过程中我们可以看到docker的镜像好像是在一层一层的在下载。
```java
[root@localhost ~]# docker pull mongo
Using default tag: latest
Trying to pull repository docker.io/library/mongo ... 
latest: Pulling from docker.io/library/mongo
f7277927d38a: Extracting [=>                                                 ] 1.376 MB/43.92 MB
8d3eac894db4: Download complete 
edf72af6d627: Download complete 
3e4f86211d23: Download complete 
5747135f14d2: Download complete 
f56f2c3793f6: Download complete 
f8b941527f3a: Download complete 
4000e5ef59f4: Download complete 
ad518e2379cf: Download complete 
919225fc3685: Download complete 
45ff8d51e53a: Downloading [=====>                                             ] 12.43 MB/104.8 MB
4d3342ddfd7b: Download complete 
26002f176fca: Download complete 
```
### 5.为什么Docker镜像要采用这种分层结构呢
最大的一个好处就是 - **共享资源**
比如：有多个镜像都从相同的 base 镜像构建而来，那么宿主机只需在磁盘上保存一份base镜像，
同时内存中也只需加载一份 base 镜像，就可以为所有容器服务了。而且镜像的每一层都可以被共享。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190728184521905.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
### 6.Docker镜像特点
Docker镜像都是只读的 当容器启动时，一个新的可写层被加载到镜像的顶部。 这一层通常被称作“容器层”,“容器层”之下的都叫“镜像层”。
### 7.Docker镜像commit
docker commit提交容器副本使之成为一个新的镜像
>docker commit -m=“提交的描述信息” -a=“作者” 容器ID 要创建的目标镜像名:[标签名]

案例演示:
- 从Hub上下载tomcat镜像到本地并成功运行
>docker pull tomcat:7-jre7

```java
[root@localhost ~]# docker pull tomcat:7-jre7
Trying to pull repository docker.io/library/tomcat ... 
7-jre7: Pulling from docker.io/library/tomcat
db0035920883: Pull complete 
a9ebd83b4a47: Pull complete 
02ef9e65a664: Pull complete 
b2786dccb0cc: Pull complete 
1b809e89f352: Pull complete 
bf313a79ccc8: Pull complete 
e03e64e40a06: Pull complete 
aec2911e85ab: Pull complete 
b7b5b93b2e01: Pull complete 
e1a8ee44b3b9: Pull complete 
5f6f66788a60: Pull complete 
Digest: sha256:d5bad6dde416e3214c88850ff1184de32d41afb10735b2594dd9e2d8f001fdcd
Status: Downloaded newer image for docker.io/tomcat:7-jre7
```
- 查看下载好的镜像
```java
[root@localhost ~]# docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
docker.io/tomcat    7-jre7              47c156f4d4e3        4 weeks ago         359 MB
docker.io/mysql     latest              7bb2586065cd        2 months ago        477 MB
docker.io/centos    7                   9f38484d220f        3 months ago        202 MB
```
- 通过下载好的镜像启动容器
```java
[root@localhost ~]# docker run -it -d -p 8888:8080  tomcat:7-jre7
2a0288c8cf76305ac9268073f4e7cc30d47fee4033d8944675fa864ddb0f2b02
```
- 查看运行中的容器
```java
[root@localhost ~]# docker ps 
CONTAINER ID        IMAGE               COMMAND             CREATED              STATUS              PORTS                    NAMES
2a0288c8cf76        tomcat:7-jre7       "catalina.sh run"   About a minute ago   Up About a minute   0.0.0.0:8888->8080/tcp   optimistic_nobel
```
- 访问测试

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190728204251711.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
- 故意删除上一步镜像生产tomcat容器的文档,对容器进行修改！
```java
[root@localhost ~]# docker exec -it 2a0288c8cf76 /bin/bash
root@2a0288c8cf76:/usr/local/tomcat# ls -l
total 132
-rw-r--r--. 1 root root  18099 Apr 10 16:57 BUILDING.txt
-rw-r--r--. 1 root root   6090 Apr 10 16:57 CONTRIBUTING.md
-rw-r--r--. 1 root root  56846 Apr 10 16:57 LICENSE
-rw-r--r--. 1 root root   1241 Apr 10 16:57 NOTICE
-rw-r--r--. 1 root root   3255 Apr 10 16:57 README.md
-rw-r--r--. 1 root root   9365 Apr 10 16:57 RELEASE-NOTES
-rw-r--r--. 1 root root  16978 Apr 10 16:57 RUNNING.txt
drwxr-xr-x. 2 root root   4096 May 14 01:12 bin
drwxr-xr-x. 1 root root     22 Jun 17 14:10 conf
drwxr-sr-x. 2 root staff    78 May 14 01:11 include
drwxr-xr-x. 2 root root   4096 May 14 01:10 lib
drwxrwxrwx. 1 root root    177 Jun 17 14:10 logs
drwxr-sr-x. 3 root staff   151 May 14 01:11 native-jni-lib
drwxr-xr-x. 2 root root     30 May 14 01:10 temp
drwxr-xr-x. 7 root root     81 Apr 10 16:57 webapps
drwxrwxrwx. 1 root root     22 Jun 17 14:10 work
root@2a0288c8cf76:/usr/local/tomcat# cd webapps/
root@2a0288c8cf76:/usr/local/tomcat/webapps# ls -l
total 8
drwxr-xr-x.  3 root root 4096 May 14 01:10 ROOT
drwxr-xr-x. 14 root root 4096 May 14 01:10 docs
drwxr-xr-x.  7 root root  111 May 14 01:10 examples
drwxr-xr-x.  5 root root   87 May 14 01:10 host-manager
drwxr-xr-x.  5 root root  103 May 14 01:10 manager
root@2a0288c8cf76:/usr/local/tomcat/webapps# rm -rf docs/
root@2a0288c8cf76:/usr/local/tomcat/webapps# 
```
- 测试删除之后的效果

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190728204349875.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)也即当前的tomcat运行实例是一个没有文档内容的容器，
以它为模板commit一个没有doc的tomcat新镜像bruceliu/tomcat02
```java
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS                    NAMES
2a0288c8cf76        tomcat:7-jre7       "catalina.sh run"   2 hours ago         Up 2 hours          0.0.0.0:8888->8080/tcp   optimistic_nobel
[root@localhost ~]# docker commit -a="bruce" -m="delete the tomcat docs" 2a0288c8cf76 bruceliu/tomcat:7-jre7
sha256:a66f9aba334d3a7252d0b9114aa14eff31f3a1641946b5b08cbcd86b5632d84e
[root@localhost ~]# docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
bruceliu/tomcat     7-jre7              a66f9aba334d        9 seconds ago       359 MB
docker.io/tomcat    7-jre7              47c156f4d4e3        4 weeks ago         359 MB
docker.io/mysql     latest              7bb2586065cd        2 months ago        477 MB
docker.io/centos    7                   9f38484d220f        3 months ago        202 MB
```
启动我们的新镜像并和原来的对比，启动bruceliu/tomcat02，它没有docs
```java
[root@localhost ~]# docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
bruceliu/tomcat     7-jre7              a66f9aba334d        2 minutes ago       359 MB
docker.io/tomcat    7-jre7              47c156f4d4e3        4 weeks ago         359 MB
docker.io/mysql     latest              7bb2586065cd        2 months ago        477 MB
docker.io/centos    7                   9f38484d220f        3 months ago        202 MB
[root@localhost ~]# docker run -it -p 7777:8080 bruceliu/tomcat
Unable to find image 'bruceliu/tomcat:latest' locally
Trying to pull repository docker.io/bruceliu/tomcat ... 
^C
[root@localhost ~]# ^C
[root@localhost ~]# docker run -it -p -d 7777:8080 bruceliu/tomcat:7-jre7
/usr/bin/docker-current: Invalid containerPort: -d.
See '/usr/bin/docker-current run --help'.
[root@localhost ~]# docker run -it -d -p 7777:8080 bruceliu/tomcat:7-jre7
9dfea93d6a81136e1b8b517313aeec129fe973008af07d4d6dd30de9e829775f
```
运行效果
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190728204615935.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

新启动原来的tomcat，它有docs
```java
[root@localhost ~]# docker run -it -d -p 8888:8080 docker.io/tomcat:7-jre7
51a4fdbd61d412200917b54a247f983e311ac8a5b498b8f9821b9c2f706e4d69
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190728204625199.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)



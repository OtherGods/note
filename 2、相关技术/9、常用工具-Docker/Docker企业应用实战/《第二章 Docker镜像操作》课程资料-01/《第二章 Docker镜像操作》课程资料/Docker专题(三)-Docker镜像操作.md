### 1.什么是Docker镜像
Docker镜像是由文件系统叠加而成（是一种文件的存储形式）。最底端是一个文件引导系统，即bootfs，这很像典型的Linux/Unix的引导文件系统。Docker用户几乎永远不会和引导系统有什么交互。实际上，当一个容器启动后，它将会被移动到内存中，而引导文件系统则会被卸载，以留出更多的内存供磁盘镜像使用。Docker容器启动是需要的一些文件，而这些文件就可以称为Docker镜像。
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019072116364229.png)
### 2.镜像操作
#### 2.1.列出镜像
列出docker下的所有镜像：`docker images`
```java
[root@localhost ~]# docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
docker.io/mysql     latest              7bb2586065cd        2 months ago        477 MB
```
- 参数说明:
REPOSITORY：镜像所在的仓库名称
TAG：镜像标签
IMAGE ID：镜像ID
CREATED：镜像的创建日期（不是获取该镜像的日期）
SIZE：镜像大小

- OPTIONS说明：
-a :列出本地所有的镜像（含中间映像层）
-q :只显示镜像ID。
--digests :显示镜像的摘要信息
--no-trunc :显示完整的镜像信息

这些镜像都是存储在Docker宿主机的/var/lib/docker目录下
```java
[root@localhost docker]# cd /var/lib/docker
[root@localhost docker]# ll
总用量 4
drwx------.  3 root root   78 2月  17 22:06 containers
drwx------.  3 root root   22 2月  17 21:32 image
drwxr-x---.  3 root root   19 2月  17 21:32 network
drwx------. 17 root root 4096 6月  17 16:21 overlay2
drwx------.  4 root root   32 2月  17 21:32 plugins
drwx------.  2 root root    6 2月  17 21:32 swarm
drwx------.  2 root root    6 6月  17 16:31 tmp
drwx------.  2 root root    6 2月  17 21:32 trust
drwx------.  4 root root  169 2月  17 22:06 volumes
```
#### 2.2.搜索镜像
如果你需要从网络中查找需要的镜像，可以通过以下命令搜索

 命令：`docker search [OPTIONS] 镜像名字`

```java
[root@localhost docker]# docker search tomcat
INDEX       NAME                                    DESCRIPTION                                     STARS     OFFICIAL   AUTOMATED
docker.io   docker.io/tomcat                        Apache Tomcat is an open source implementa...   2464      [OK]       
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
- 参数说明
NAME：仓库名称
DESCRIPTION：镜像描述
STARS：用户评价，反应一个镜像的受欢迎程度
OFFICIAL：是否官方
AUTOMATED：自动构建，表示该镜像由Docker Hub自动构建流程创建的
- OPTIONS说明
  --no-trunc : 显示完整的镜像描述
  -s : 列出收藏数不小于指定值的镜像。
  --automated : 只列出 automated build类型的镜像；
#### 2.3.拉取镜像
##### 2.3.1.从Docker Hub拉取
Docker Hub：https://hub.docker.com
Docker镜像首页，包括官方镜像和其它公开镜像。Docker Hub上最受欢迎的10大镜像（通过Docker registry API获取不了镜像被pull的个数，只能通过镜像的stars数量，来衡量镜像的流行度。毫无疑问，拥有最高stars数量的库都是官方库).
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019072116525937.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
Mirror与Private Registry的区别：
- Private Registry（私有仓库）是开发者或者企业自建的镜像存储库，通常用来保存企业内部的 Docker 镜像，用于内部开发流程和产品的发布、版本控制。
- Mirror是一种代理中转服务，我们(比如daocloud)提供的Mirror服务，直接对接Docker Hub的官方Registry。Docker Hub 上有数以十万计的各类 Docker 镜像。
在使用Private Registry时，需要在Docker Pull 或Dockerfile中直接键入Private Registry 的地址，通常这样会导致与 Private Registry 的绑定，缺乏灵活性。
使用 Mirror 服务，只需要在 Docker 守护进程（Daemon）的配置文件中加入 Mirror 参数，即可在全局范围内透明的访问官方的 Docker Hub，避免了对 Dockerfile 镜像引用来源的修改。
简单来说，Mirror类似CDN，本质是官方的cache；Private Registry类似私服，跟官方没什么关系。对用户来说，由于用户是要拖docker hub上的image，对应的是Mirror。yum/apt-get的Mirror又有点不一样，它其实是把官方的库文件整个拖到自己的服务器上做镜像，并定时与官方做同步；而Docker Mirror只会缓存曾经使用过的image。

使用命令拉取：

> docker pull 镜像名字[:TAG]

目前国内访问docker hub速度上有点尴尬，使用docker Mirror势在必行。现有国内提供docker镜像加速服务的商家有不少，下面重点ustc镜像。
##### 2.3.2.ustc的镜像
ustc是老牌的linux镜像服务提供者了，还在遥远的ubuntu 5.04版本的时候就在用。ustc的docker镜像加速器速度很快。ustc docker mirror的优势之一就是不需要注册，是真正的公共服务。
https://lug.ustc.edu.cn/wiki/mirrors/help/docker
步骤：
（1）编辑该文件：`vi /etc/docker/daemon.json`  // 如果该文件不存在就手动创建；说明：在centos7.x下，通过vi。
（2）在该文件中输入如下内容：

```java
{
"registry-mirrors": ["https://docker.mirrors.ustc.edu.cn"]
}
```
3）注意：一定要重启docker服务，如果重启docker后无法加速，可以重新启动OS
```java
[root@localhost docker]# service docker restart
Redirecting to /bin/systemctl restart docker.service
```
4)示例
```java
[root@localhost docker]# docker pull zookeeper
Using default tag: latest
Trying to pull repository docker.io/library/zookeeper ... 
latest: Pulling from docker.io/library/zookeeper
f5d23c7fed46: Pull complete 
eaa7ca9a16a1: Pull complete 
57f21f30136f: Pull complete 
6e7c1070bcbc: Pull complete 
f4d70619b3de: Pull complete 
e4d5dc43f630: Pull complete 
c9be43341e93: Pull complete 
135ab2963089: Pull complete 
Digest: sha256:4a70c5b011d94298f62c62d7341a374b6a3a5d15a246f76d4cba3584ae60b8f6
Status: Downloaded newer image for docker.io/zookeeper:latest
```
#### 2.4.删除镜像
作用：删除镜像
删除单个

    docker rmi  -f 镜像ID

删除多个

    docker rmi -f 镜像名1:TAG 镜像名2:TAG 

删除全部

    docker rmi -f $(docker images -qa)
示例：
```java
[root@localhost docker]# docker images
REPOSITORY            TAG                 IMAGE ID            CREATED                  SIZE
docker.io/zookeeper   latest              a873528df41f        Less than a second ago   225 MB
docker.io/mysql       latest              7bb2586065cd        2 months ago             477 MB
[root@localhost docker]# docker rmi  -f a873528df41f
Untagged: docker.io/zookeeper:latest
Untagged: docker.io/zookeeper@sha256:4a70c5b011d94298f62c62d7341a374b6a3a5d15a246f76d4cba3584ae60b8f6
Deleted: sha256:a873528df41f67308900374218cbcb334bf764074f9daa76482d86e53f058589
Deleted: sha256:6112743ce75a12d3c5db9692c88cb4fd8f26bdacbee3fd6b2801a8a11d0cafa0
Deleted: sha256:474612b9b8b92623226ef0100c04598113a74ff9b34f3c47e9fc39e8cfcf1953
Deleted: sha256:136c24ea1ecb39f9070a8e2e7b6cedbc6e31a019d171910b7fb9f6efaa1f45d1
Deleted: sha256:af0ddb7072be87b262a98e8d9a30bb01e207ae44dfa7039beb09a4f7e85dfc19
Deleted: sha256:f60a1b0a8bdfbb66a0856863d4339802eeb7f593c4c4a6bddba425c278349fa1
Deleted: sha256:d4ba2b7e7a81205a5e6a7d35bc89b55dc5802fec6b5d35d93e4965e908b46d32
Deleted: sha256:4cfe7f0f8661eef6a97f30742474149c45ce22e7ca8f93e9e069abe333d2e470
Deleted: sha256:d8a33133e477d367977987129313d9072e0ec80894ed4c52c2d88186f354c29a
[root@localhost docker]# docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
docker.io/mysql     latest              7bb2586065cd        2 months ago        477 MB
```



# 1、Docker安装

## 1.1、win环境安装

对于win10而言，参看：
[【全面详细】Windows10 Docker安装详细教程](https://zhuanlan.zhihu.com/p/441965046)

对于win11而言，就更简单了，直接下载包安装
官网地址：[https://docker.com/](https://docker.com/)
下载之后一路双击，跟着步骤走即可
> 我的win11笔记本，没有额外配置，如果有遇到问题的，看一下是不是开启子linux系统配置，见上面的连接进行设置

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202404051731356.png)


## 1.2、Linux环境安装

直接命令行方式安装即可，参考[官方教程](https://docs.docker.com/desktop/install/ubuntu/)

更见的安装方式，直接执行下面的命令
```shell
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

执行完毕之后，启动docker
```shell
sudo systemctl start docker
```

然后开始验证是否可以使用
```shell
sudo docker run hello-world
# 查看所有的容器
docker ps -a
```

## 1.3、Mac环境安装

直接使用homebrew方式进行安装
```shell
brew cask install docker
```

或者直接再官网下载dmg安装包

# 2、打包运行

接下来重点是Dockerfile文件的配置，以实现镜像构建，当前可以在 feature/docker [分支](https://github.com/itwanger/paicoding/commit/9e92daab99fdc468776ad90821c72e9ef3f37437)查看

## 2.1、项目配置

在本机，使用docker部署应用时，若选择的是dev开发环境，则也需要修改 dev/application-dal.yml
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403072251223.png)

将上面的 127.0.0.1 修改为 本地局域网内ip ( 通过 ifconfig 或者 ipconfig 获取![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403072252818.png)

请注意：
- 默认mysql， redis都是只允许本机进行访问，因此也需要修改它们的配置
- 看下面这篇文档，修改相关配置，实现可以非127.0.0.1的访问支持
[4、技术派本地多机器部署开发教程](2、相关技术/24、项目/1、技术派/3、工程篇/0、技术派架构、功能、设计.md#4、技术派本地多机器部署开发教程)

## 2.2、编译配置

首先我们需要构建一个fat-jar，直接在命令行中执行
```shell
mvn clean package -DskipTests=true
```

然后在idea直接打开Dockfile文件, 可以看到最上面有一个可执行的小标签，鼠标放上去右键
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403072253559.png)

![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403072253248.png)

## 2.3、Docker启动

![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403072254179.png)

启动之后，可以在idea的 log 看到正常的日志输出，也可以切回到Docker桌面版本进行查看
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403072255620.png)

![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403072255643.png)

然后直接在浏览器查看，是否可以正常访问即可

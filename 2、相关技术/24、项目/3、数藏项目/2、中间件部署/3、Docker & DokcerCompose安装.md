docker命令相关可以参考：
1. [Docker 备忘清单](https://wangchujiang.com/reference/docs/docker.html)
   已将这个开源项目frock到我的github中，具体质量怎么样还不清楚

在CentOS 7.x 64位、CentOS 8.x 64位、Alibaba Cloud Linux 3 64位等系统重安装Docker & DokcerCompose步骤。

### CentOS 7.x

运行以下命令，下载docker-ce的yum源。

```shell
sudo wget -O /etc/yum.repos.d/docker-ce.repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
```

运行以下命令，安装Docker。

```
sudo yum -y install docker-ce
```

执行以下命令，检查Docker是否安装成功。

```
sudo docker -v
```

如下图回显信息所示，表示Docker已安装成功。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408251740109.png)

执行以下命令，启动Docker服务，并设置开机自启动。

```shell
sudo systemctl start docker
sudo systemctl enable docker
```

执行以下命令，查看Docker是否启动。

```
sudo systemctl status docker
```

如下图回显所示，表示Docker已启动。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408251749799.png)

运行以下命令，安装setuptools。

```
sudo pip3 install -U pip setuptools
```

运行以下命令，安装docker-compose。

```
sudo pip3 install docker-compose
```

运行以下命令，验证docker-compose是否安装成功。

```
docker-compose --version
```

如果回显返回docker-compose版本信息，表示docker-compose已安装成功。

### CentOS 8.x

1. 切换CentOS 8源地址。CentOS 8操作系统版本结束了生命周期（EOL），按照社区规则，CentOS 8的源地址http://mirror.centos.org/centos/8/内容已移除，您在阿里云上继续使用默认配置的CentOS 8的源会发生报错。如果您需要使用CentOS 8系统中的一些安装包，则需要手动切换源地址。具体操作，请参见CentOS 8 EOL如何切换源？。
2. 运行以下命令，安装DNF。

```
sudo yum -y install dnf
```

1. 运行以下命令，安装Docker存储驱动的依赖包。

```
sudo dnf install -y device-mapper-persistent-data lvm2
```

1. 运行以下命令，添加稳定的Docker软件源。

```
sudo dnf config-manager --add-repo=https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
```

1. 运行以下命令，检查Docker软件源是否已添加。

```
sudo dnf list docker-ce
```

出现如下图所示回显，表示Docker软件源已添加。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408251757386.png)

1.   
    运行以下命令安装Docker。

```
sudo dnf install -y docker-ce --nobest
```

执行以下命令，检查Docker是否安装成功。

```
sudo docker -v
```

如下图回显信息所示，表示Docker已安装成功。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408251802611.png)

执行以下命令，启动Docker服务，并设置开机自启动。

```
sudo systemctl start dockersudo systemctl enable docker
```

执行以下命令，查看Docker是否启动。

```
sudo systemctl status docker
```

如下图回显所示，表示Docker已启动。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408251803115.png)

运行以下命令，安装setuptools。

```
sudo pip3 install -U pip setuptools
```

运行以下命令，安装docker-compose。

```
sudo pip3 install docker-compose
```

运行以下命令，验证docker-compose是否安装成功。

```
docker-compose --version
```

如果回显返回docker-compose版本信息，表示docker-compose已安装成功。

### Alibaba Cloud Linux 3

1. 运行以下命令，添加docker-ce的dnf源。

```
sudo dnf config-manager --add-repo=https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
```

1. 运行以下命令，安装Alibaba Cloud Linux 3专用的dnf源兼容插件。

```
sudo dnf -y install dnf-plugin-releasever-adapter --repo alinux3-plus
```

1. 运行以下命令，安装Docker。

```
sudo dnf -y install docker-ce --nobest
```

- 如果执行命令时，出现类似如下的报错信息，您需要执行sudo dnf clean packages清除软件包缓存后，重新安装docker-ce。
- 如果执行命令时，出现类似下图的报错信息，您需要注释/etc/yum.repos.d下的CentOS源，注释后重新安装docker-ce。

```
(8-9/12): docker-ce-24.0.7-1.el8.x86_64.rpm 38% [================- ] 8.2 MB/s | 38 MB 00:07 ETAThe downloaded packages were saved in cache until the next successful transaction.You can remove cached packages by executing 'dnf clean packages'.Error: Error downloading packages:containerd.io-1.6.26-3.1.el8.x86_64: Cannot download, all mirrors were already tried without success
```

执行以下命令，检查Docker是否安装成功。

```
sudo docker -v
```

如下图回显信息所示，表示Docker已安装成功。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408251804949.png)

执行以下命令，启动Docker服务，并设置开机自启动。

```
sudo systemctl start dockersudo systemctl enable docker
```

执行以下命令，查看Docker是否启动。

```
sudo systemctl status docker
```

如下图回显所示，表示Docker已启动。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408251804240.png)

运行以下命令，安装setuptools。

```
sudo pip3 install -U pip setuptools
```

运行以下命令，安装docker-compose。

```
sudo pip3 install docker-compose
```

运行以下命令，验证docker-compose是否安装成功。

```
docker-compose --version
```

如果回显返回docker-compose版本信息，表示docker-compose已安装成功。

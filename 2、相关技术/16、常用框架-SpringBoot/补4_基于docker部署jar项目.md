### 1.

将jar包放到Linux任意目录，eg: /var/project/  
在同级目录下新建文件Dockerfile,内容如下：
> FROM java:8  
> MAINTAINER bingo  
> ADD demo-0.0.1-SNAPSHOT.jar demo.jar  
> EXPOSE 8080  
> ENTRYPOINT ["java","-jar","demo.jar"]

```css
from指令指明了当前镜像的基镜像，编译当前镜像时自动下载基镜像。
MAINTAINER指明作者
ADD 复制jar文件到镜像中去并重命名为demo.jar
EXPOSE暴露8080端口
ENTRYPOINT启动时执行java -jar demo.jar
```

在当前目录下执行编译镜像

> docker build -t bingo/demo .

```undefined
bingo/demo 景象名字
. Dockerfile文件在当前文件夹下
```

等编译好后运行容器  
docker run --name demo -p 8080:8080 -d bingo/demo  
查看运行状态  
docker ps
```rust
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                    NAMES
1c7c2fd7af80        nginx               "nginx -g 'daemon off"   About an hour ago   Up About an hour    0.0.0.0:80->80/tcp       nginx
81c1456ebafc        bingo/demo         "java -jar demo.jar"    1 hours ago         Up 1 hours          0.0.0.0:8080->8080/tcp   demo
```




转载自：[https://www.jianshu.com/p/ec477d84fc7d](https://www.jianshu.com/p/ec477d84fc7d)

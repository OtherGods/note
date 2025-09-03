### 配置要求

CPU：1核

内存：4G

### 安装elasticsearch

1、使用docker下载es：

docker pull elasticsearch:8.13.0

2、创建配置

编辑 /root/package/es/config/elasticsearch.yml 文件：
```yml
cluster.name: "nfturbo-cluster"
network.host: 0.0.0.0
http.cors.enabled: true
http.cors.allow-origin: "*"
xpack.security.enabled: false
```

3、启动镜像

执行以下命令
```shell
-- 这条命令启动一个 Elasticsearch 容器，以单节点模式运行，配置内存使用限制，并
-- 将宿主机的配置文件、数据目录和插件目录挂载到容器内。这使得在容器外部可以方便
-- 地管理和维护 Elasticsearch 的配置、数据和插件。

docker run --name elasticsearch -p 9200:9200  -p 9300:9300 
       -e "discovery.type=single-node" 
       -e ES_JAVA_OPTS="-Xms256m -Xmx512m" 
       -v /root/package/es/config/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml
       -v /home/package/es/data:/usr/share/elasticsearch/data 
       -v /home/package/es/plugins:/usr/share/elasticsearch/plugins 
       -d elasticsearch:8.13.0
```
上述 docker 命令解释
- `docker run`：启动一个新的容器。
- `--name elasticsearch`：为容器命名为 `elasticsearch`。
- `-p 9200:9200 -p 9300:9300`：将宿主机的 9200 端口和 9300 端口映射到容器的 9200 端口和 9300 端口。9200 是 Elasticsearch HTTP 接口的默认端口，9300 是节点间通信的默认端口。
- `-e "discovery.type=single-node"`：设置环境变量 `discovery.type` 为 `single-node`，使 Elasticsearch 以单节点模式运行，适用于开发和测试环境。
- `-e ES_JAVA_OPTS="-Xms256m -Xmx512m"`：设置环境变量 `ES_JAVA_OPTS`，指定 Java 虚拟机的最小和最大堆内存为 256MB 和 512MB。
- `-v /root/package/es/config/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml`：将宿主机上的 `/root/package/es/config/elasticsearch.yml` 文件挂载到容器内的 `/usr/share/elasticsearch/config/elasticsearch.yml`，覆盖容器中的默认配置文件。
- `-v /home/package/es/data:/usr/share/elasticsearch/data`：将宿主机上的 `/home/package/es/data` 目录挂载到容器内的 `/usr/share/elasticsearch/data`，用于存储 Elasticsearch 数据。
- `-v /home/package/es/plugins:/usr/share/elasticsearch/plugins`：将宿主机上的 `/home/package/es/plugins` 目录挂载到容器内的 `/usr/share/elasticsearch/plugins`，用于存储 Elasticsearch 插件。
- `-d elasticsearch:8.13.0`：以守护进程模式（后台）运行容器，并使用 `elasticsearch:8.13.0` 镜像。


压缩成单行文本方便执行：
```shell
docker run --name elasticsearch -p 9200:9200  -p 9300:9300  -e "discovery.type=single-node" -e ES_JAVA_OPTS="-Xms256m -Xmx512m" -v /root/package/es/config/elasticsearch.yml:/usr/share/elasticsearch8/config/elasticsearch.yml -v /home/package/es/data:/usr/share/elasticsearch8/data -v /home/package/es/plugins:/usr/share/elasticsearch8/plugins -d elasticsearch:8.13.0
```

这里面的`/root/package/es/config/elasticsearch.yml`改成你自己的目录文件

查看镜像：
```shell
[root@iZygqcfrfbixgtZ config]# docker ps
CONTAINER ID   IMAGE                                      COMMAND                  CREATED         STATUS         PORTS                                                                                  NAMES
3981174ed6df   elasticsearch:8.13.0                       "/bin/tini -- /usr/l…"   2 seconds ago   Up 2 seconds   0.0.0.0:9200->9200/tcp, :::9200->9200/tcp, 0.0.0.0:9300->9300/tcp, :::9300->9300/tcp   elasticsearch
```

4、检查是否启动成功

首先通过 `docker logs elasticsearch` 查看是否有报错。

#elasticsearch重置账号密码
通过命令 `elasticsearch-reset-password -u elastic -i` 重置elasticsearch账号密码：我现在部署的服务，账号密码是elastic/elastic。

然后通过curl检查是否成功：
```shell
curl localhost:9200
{
  "name" : "3981174ed6df",
  "cluster_name" : "nfturbo-cluster",
  "cluster_uuid" : "9blwCZsiSiOapFjQEF42cA",
  "version" : {
    "number" : "8.13.0",
    "build_flavor" : "default",
    "build_type" : "docker",
    "build_hash" : "09df99393193b2c53d92899662a8b8b3c55b45cd",
    "build_date" : "2024-03-22T03:35:46.757803203Z",
    "build_snapshot" : false,
    "lucene_version" : "9.10.0",
    "minimum_wire_compatibility_version" : "7.17.0",
    "minimum_index_compatibility_version" : "7.0.0"
  },
  "tagline" : "You Know, for Search"
}
```

通过9200端口访问：**通过https访问**
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409011611350.png)

PS：如果是云服务器，记得开启端口号。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409011616922.png)

### 安装Kibana

Kibana 是一款适用于 Elasticsearch 的源可用数据可视化仪表板软件。

1、使用docker下载kibana

docker pull kibana:8.13.0

2、查看es的ip
```shell
$ docker inspect 3981174ed6df |grep IPAddress
```

这里的`3981174ed6df`换成你自己的容器 ID。

容器 ID 查询方式就是 docker ps。

输出结果如下：
```shell
"SecondaryIPAddresses": null,
"IPAddress": "172.17.0.3",        
"IPAddress": "172.17.0.3",
```

3、创建并配置kibana.yml

宿主机中创建文件 /root/package/es/config/kibana.yml
需要把刚刚查到的es的ip设置到elasticsearch.hosts中，其他的配置自己适当调整即可。
```yml
server.name: kibana
#server.port: 5601
server.host: 0.0.0.0
elasticsearch.hosts: [ "http://172.17.0.2:9200" ] # 改成 es 的内网 ip
#elasticsearch.username: "elastic"
#elasticsearch.password: "123456"
xpack.monitoring.ui.container.elasticsearch.enabled: true
i18n.locale: "zh-CN"
```

4、启动Kibana
```shell
sudo docker run --name kibana -d -p 5601:5601 -v /root/package/es/config/kibana.yml:/usr/share/kibana/config/kibana.yml kibana:8.13.0
```

这里的/root/package/es/config/kibana.yml，就是你刚刚创建的 kibana.yml 的路径。

5、检查是否启动成功
```shell
[root@iZygqcfrfbixgtZ config] docker ps
CONTAINER ID   IMAGE                                      COMMAND                  CREATED          STATUS          PORTS                                                                                  NAMES4913f7dd8be2   kibana:8.13.0                              "/bin/tini -- /usr/l…"   26 seconds ago   Up 25 seconds   0.0.0.0:5601->5601/tcp, :::5601->5601/tcp                                              kibana3981174ed6df   elasticsearch:8.13.0                       "/bin/tini -- /usr/l…"   11 minutes ago   Up 11 minutes   0.0.0.0:9200->9200/tcp, :::9200->9200/tcp, 0.0.0.0:9300->9300/tcp, :::9300->9300/tcp   elasticsearch
```

通过页面访问：http://ip:5601/app/home#/
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409011623953.png)

输入用户名（elastic）、密码（123456））即可访问

### 常见问题

1、ERROR: Elasticsearch did not exit normally - check the logs at /usr/share/elasticsearch/logs/docker-

需要对es挂载的宿主机文件 进行授权 ：`chmod 777 -R ./*`

2、 curl: (52) Empty reply from server
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022149401.png)

```shell
--查看容器 id，替换下面的030926f40873
docker ps
--进入容器内部
docker exec -it --user root 030926f40873  /bin/bash
-- 安装vim 命令
apt-get  update
apt-get  install vim
--修改elasticsearch.yml
vi config/elasticsearch.yml
将 xpack.security.enabled: true 改为：xpack.security.enabled: false
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409022149773.png)

```
退出容器并重启：docker restart 030926f40873
```

重启后再重新执行就可以了。
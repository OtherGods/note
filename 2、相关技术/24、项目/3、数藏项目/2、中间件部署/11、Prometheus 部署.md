**Prometheus** 是一个开源的监控和报警系统，主要用于收集和存储时序数据。他的核心作用是监控应用和服务器等基础设施的状态，并根据收集到的数据进行报警。它能够对系统、服务、应用程序和硬件的各种指标（如 CPU 使用率、内存消耗、请求延迟等）进行实时监控。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092233155.png)

上图是 Prometheus 的主要的架构图，其中分为以下几个关键组件：

Prometheus 作为一个完整的监控系统，具有多个关键组件，每个组件都在不同的阶段执行特定的功能。以下是 Prometheus 的四个关键组件：

**Prometheus Server** 是 Prometheus 的核心组件，负责数据的采集、存储和查询。Prometheus Server 主要通过pull方式定期从被监控的目标（如 HTTP 服务、Exporters、Kubernetes 等）获取指标数据。它支持拉取不同类型的监控数据，如系统资源、应用状态、性能指标等。

**Alertmanager是报警相关组件，主要用于** Prometheus 相关监控的报警规则管理和预警。

**Exporters** 是一些专门用于暴露监控数据的服务。它们将各种服务或系统的内部状态以 Prometheus 可解析的格式（通常是 HTTP endpoint）暴露出来。

- **常见的 Exporters**：
    
- **Node Exporter**：监控系统的硬件和操作系统级别的指标，如 CPU 使用率、内存消耗、磁盘空间、网络流量等。
    
- **MySQL Exporter**：监控 MySQL 数据库的性能指标，如查询响应时间、连接数等。
    
- **Blackbox Exporter**：用于监控网络服务的可用性，例如通过 HTTP、HTTPS、TCP 等协议进行探测。
    
- **Kubernetes Exporter**：为 Kubernetes 集群提供监控数据，收集集群节点、Pod、容器等的性能指标。
    
- **自定义 Exporters**：用户也可以编写自己的 Exporter，以暴露特定应用程序或服务的监控数据
    

**UI工具**，这部分就是把 Prometheus 采集到的数据做图形化展示的，他自带了 web UI，但是效果不太好，一般都是采用 Grafana 来进行图形界面展示的。

### Prometheus 安装

注意：Prometheus 和 node_exportor需要和你的要监控的服务器部署在一起，至少要把node_exportor部署在你想监控的服务器上。

因为国内 docker 镜像封的比较严重，我们直接采用安装包方式，到官网下载安装包：https://prometheus.io/download/

我下载的是3.0.1的 linux 版本的

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092246789.png)

直接在服务器上执行 `wget https://github.com/prometheus/prometheus/releases/download/v3.0.1/prometheus-3.0.1.linux-amd64.tar.gz`

下载之后就是一个压缩包了，先把他解压：`tar -xf prometheus-3.0.1.linux-amd64.tar.gz` ，解压后内容如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092247040.png)

接着我们给 Prometheus 创建 service 并且设置开机自启动。假设我的 Prometheus 在/root/package/prometheus 目录下。

```
cd /home/root/package/mv prometheus-3.0.1.linux-amd64 prometheus
```

```
vim /usr/lib/systemd/system/prometheus.service
```

输入以下内容并保存：
```text
[Unit]  
Description=prometheus  
Documentation=https://prometheus.io/  
After=network.target  
[Service]  
Type=simple  
User=root  
Group=root  
ExecStart=/root/package/prometheus/prometheus --config.file=/root/package/prometheus/prometheus.yml --web.enable-lifecycle --web.external-url=http://PrometheusIP:9090  
Restart=on-failure  
[Install]  
WantedBy=multi-user.target
```

注意这里的`/root/package/prometheus` 改成你自己的安装路径。然后执行：

```
systemctl daemon-reloadsystemctl enable prometheus --now
```

然后就启动成功了，查看方式如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092250802.png)

然后通过 ip:9090 即可访问（记得开启端口号，添加安全组）：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092251867.png)

### 安装 node_exporter

**Node Exporter**是帮助我们监控系统的硬件和操作系统级别的指标，如 CPU 使用率、内存消耗、磁盘空间、网络流量等的一个组件。需要单独安装。

下载地址也是：https://prometheus.io/download/

我用的是1.8.2版本：

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092252464.png)

对软件压缩包进行解压，并把它其中的node_exporter执行文件移动到/usr/local/bin目录下

```
tar -zxvf node_exporter-1.8.2.linux-amd64.tar.gzmv node_exporter-1.8.2.linux-amd64 node_exporter
```

接着我们给 node_exporter 创建 service 并且设置开机自启动。假设我的他 在/root/package/node_exporter 目录下。

```
vim /usr/lib/systemd/system/node_exporter.service
```

文件中写入：

```
[Unit]  
Description=node_exporter  
Documentation=https://prometheus.io/  
After=network.target  
[Service]  
Type=simple  
User=root  
Group=root  
ExecStart=/root/package/node_exporter/node_exporter  
Restart=on-failure  
[Install]  
WantedBy=multi-user.target
```

为node_exporter设置自动启动并启动服务

```
systemctl daemon-reloadsystemctl enable node_exporter --now
```

查看启动结果，9100端口：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092253507.png)

修改Prometheus配置文件，增加配置：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092253271.png)

在其中加入：localhost:9100 （这里的localhost 也建议直接改成你自己的公网 ip），增加 node_exporter的配置，然后重启 prometheus：

`systemctl restart prometheus`

就可以到控制台看效果了：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092254854.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092254993.png)

通过9100端口访问可以看到：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092254925.png)





### Grafana 介绍

**Grafana** 是一个开源的分析与可视化平台，广泛用于数据的可视化、监控和告警。它可以与多种数据源（如 Prometheus、InfluxDB、Elasticsearch、MySQL、PostgreSQL 等）集成，通过图表、仪表盘等方式展示数据。Grafana 主要用于实时数据监控和分析，特别适用于可视化时间序列数据。

因为我们会用Prometheus做监控采集，但是因为他自带的 web ui并不是特别友好，所以我们采用 Grafana 来当作 Prometheus 的UI，同时他还可以接入我们的 mysql 和 es 的监控。

### Grafana 部署

安装包请到官网获取：https://grafana.com/grafana/download?pg=get&plcmt=selfmanaged-box1-cta1

他给出了不同的操作系统的安装方式，参考着安装就行。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092257089.png)

因为我是 ubuntu，所以我是：

```
sudo apt-get install -y adduser libfontconfig1 muslwget https://dl.grafana.com/enterprise/release/grafana-enterprise_11.4.0_amd64.debsudo dpkg -i grafana-enterprise_11.4.0_amd64.deb
```

（如果这里执行失败，请看文末的常见问题，换种方式部署）

启动：

`systemctl enable grafana-server --now`

然后就可以通过3000端口访问了：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092257565.png)

使用账号密码：admin/admin 登录，第一次会让你设置一个新密码，设置一下就行了，。然后就可以进入控制台了。

### 配置数据源

安装好 Grafana 之后，就可以把他和 Prometheus 连接了。

先点击 DATA DOUECES，添加一个新的数据源。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092258301.png)

进来之后，选 Prometheus：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092259134.png)

然后配置上你的 ip 和端口：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092300762.png)

保存即可。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092300706.png)

### 配置 dashboard

添加完数据源之后，从侧边栏的 data sources进来就能找到你的配置了，然后点击他的 Build a Dashboard。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092314939.png)

然后通过 import的方式创建：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092314300.png)

然后起个名字，保存。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092315905.png)

然后进入 Dashboard 的 tab，去配置这个 dashboard：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092315301.png)

点击这个新建的 dashboard 之后，进入配置页面，然后继续import
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092316415.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092316986.png)

接下来就可以添加配置信息了，这里可以用 grafana 官方给的模板：https://grafana.com/grafana/dashboards/﻿

比如我选择这个：https://grafana.com/grafana/dashboards/1860-node-exporter-full/

那么我就在配置也页面输入：1860，然后开始导入：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092316285.png)

然后页面就出来了：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092317757.png)

### 常见问题

**sudo dpkg -i grafana-enterprise_11.4.0_amd64.deb 执行失败**

如果失败了，可以通过以下方式下载安装包，并解压：

```
wget https://dl.grafana.com/enterprise/release/grafana-enterprise-11.4.0.linux-amd64.tar.gztar -zxvf grafana-enterprise-11.4.0.linux-amd64.tar.gz
```

然后制作一个 service：

```
mv grafana-v11.4.0 grafana
```

```
vim /usr/lib/systemd/system/grafana_server.service
```

文件中写入：

```text
[Unit]  
Description=Grafana instance  
After=network.target  
  
[Service]  
User=root  
Group=root  
ExecStart=/root/package/grafana/bin/grafana-server  
WorkingDirectory=/root/package/grafana  
Restart=always  
Environment="GF_PATHS_CONFIG=/root/package/grafana/conf/grafana.ini"  
Environment="GF_PATHS_DATA=/root/package/grafana/data"  
Environment="GF_PATHS_LOGS=/root/package/grafana/logs"  
Environment="GF_PATHS_PLUGINS=/root/package/grafana/plugins"  
  
[Install]  
WantedBy=multi-user.target
```

为grafana_server设置自动启动并启动服务

```
systemctl daemon-reload
systemctl enable grafana_server --now
```


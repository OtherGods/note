### 安装sentinel dashboard

从 https://github.com/alibaba/Sentinel/releases 下载dashboard的jar文件

通过以下命令在机器上启动：

```shell
nohup java -Dserver.port=8080 -Dcsp.sentinel.dashboard.server=localhost:8080 -Dproject.name=sentinel-dashboard -jar sentinel-dashboard.jar &
```

如果8080被占用，就换一个其他的端口号即可。

启动后可以通过IP:8080访问控制台，控制台账号密码默认都是sentinel
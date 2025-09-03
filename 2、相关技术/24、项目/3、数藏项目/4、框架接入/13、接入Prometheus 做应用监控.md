[11、Prometheus 部署](2、相关技术/24、项目/3、数藏项目/2、中间件部署/11、Prometheus%20部署.md)

[12、Grafana 部署](2、相关技术/24、项目/3、数藏项目/2、中间件部署/12、Grafana%20部署.md)

Prometheus 和 Grafana 部署好之后，已经可以针对服务器做监控了，那么如何针对应用做监控呢？我们还想知道一个 java 应用的JVM 的情况，比如堆内存大小、GC 次数、GC 耗时，以及一个接口的 QPS、RT 等情况。

那么，我们介绍下如何在 SpringBoot 中接入 Prometheus 做监控。

### 添加依赖

Spring Boot 3 默认支持 Micrometer，用于暴露 Prometheus 监控指标。

> 在Spring Boot 3.x的版本中, Spring Cloud Sleuth被 Micrometer替代 ，
> 
> **Micrometer** 是一个应用监控的工具包，主要用于 **Java** 应用程序中实现性能指标的采集和导出。它是一个 **基于 JVM** 的开源库，为开发者提供了一种方便的方式来生成、处理和导出应用的度量指标（metrics），支持多种后端监控系统，如 **Prometheus、Graphite、StatsD、InfluxDB、Elastic、New Relic** 等。

添加以下依赖到 `pom.xml`

```xml
<!--  prometheus -->  
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-actuator</artifactId>  
</dependency>  
  
<dependency>  
	<groupId>io.micrometer</groupId>  
	<artifactId>micrometer-registry-prometheus</artifactId>  
</dependency>  
  
<!--  dubbo 监控:https://cn.dubbo.apache.org/zh-cn/overview/tasks/observability/metrics-start/ -->  
<dependency>  
	<groupId>org.apache.dubbo</groupId>  
	<artifactId>dubbo-spring-boot-observability-starter</artifactId>  
	<version>3.2.10</version>  
</dependency>
```

`spring-boot-starter-actuator` 是 Spring Boot 提供的一个核心依赖模块，用于简化对应用程序的 **监控和管理**。它集成了多种功能，帮助开发者快速实现应用的运行状况检查、性能指标收集、监控数据暴露、系统配置查看等功能。

- 提供一系列预定义的 HTTP 端点（endpoint），用于查看和管理应用的状态（如 `/actuator/health`, `/actuator/info`, `/actuator/metrics` 等）。
- 集成 Micrometer 提供的指标，能够通过 `/actuator/metrics` 查看应用的性能数据。
- Actuator 通过内置的 Micrometer API，采集应用的核心指标，如 JVM 性能、线程池状态、HTTP 请求数据等。

`micrometer-registry-prometheus` 是 **Micrometer** 提供的一个 Prometheus 集成库，用于将 Java 应用程序的监控指标导出到 **Prometheus**，方便实时采集和分析。它是构建现代微服务架构中监控体系的重要组成部分。

- 在 Spring Boot 集成中，它会自动注册一个 **`/actuator/prometheus`** 端点，提供 Prometheus 可读取的指标数据。
- 将 `spring-boot-starter-actuator` 暴露的指标转化为 Prometheus 格式。

`dubbo-spring-boot-observability-starter` 是 Apache Dubbo 提供的一款工具，用于将 Dubbo 的可观测性功能与 Spring Boot 集成在一起，方便采集和暴露服务调用的性能指标和分布式追踪信息。它基于 **Micrometer** 和 **OpenTelemetry** 构建，支持主流监控和分布式追踪系统，如 **Prometheus**、**Zipkin**、**Jaeger** 等。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092327215.png)

### 增加配置

接着，我们需要在配置文件中启用 Actuator 和 Prometheus 的endpoint：
```yml
management:  
  endpoints:  
    web:  
      exposure:  
        include: "*"  
  metrics:  
    export:  
      prometheus:  
        enabled: true
```

配置好之后，Micrometer 自动收集 JVM 指标，例如：

- 内存使用 (`jvm.memory.*`)
- 线程 (`jvm.threads.*`)
- GC (`jvm.gc.*`)

同时，Spring Boot 会自动收集以下 HTTP 指标：

- 接口响应时间 (RT)：`http.server.requests`
- QPS：`http.server.requests`

接着，增加对 dubbo 的接入配置：
```yml
dubbo: 
	metrics: 
		enable: 
			true protocol: prometheus
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092329091.png)

按照以上方式配置完之后，就可以启动应用了，我们的项目中，把这些配置放到了 monitor 这个包下面，所以想要在 business 中接入，还需要再 pom 和 application.yml 中增加如下配置：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092330443.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092330365.png)

### 启动应用

接下来启动NfTurboBusinessApplication，启动成功之后，访问 ip:8085/actuator/prometheus

会看到以下metrics 信息，表示接入成功了：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092331307.png)

### 配置 Prometheus

应用启动成功之后，并且已经可以看到metrics 信息之后，就可以和 Prometheus的服务端做配置采集了。

登陆到 Prometheus 部署的机器上，编辑 prometheus.yml
```
vim /root/package/prometheus/prometheus.yml
```

增加：
```
- job_name: "nft_turbo"  
  metrics_path: '/actuator/prometheus'  
  static_configs:  
    - targets: ['localhost:8085']
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092333588.png)

这里的 localhost 换成你自己的 ip，并且注意是公网ip 并且开启8085端口。

然后重启 Prometheus：systemctl restart prometheus

重启后，访问：http://ip:9090/query，可以看到：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503092333809.png)

在 target health 下面，已经新多出了一个我们刚配置的 nft_turbo 的 endpoint，只不过他的状态是有点问题的，因为我这里是本地启动的应用，而我们自己的机器是没有公网 ip 的，也没做内网穿透，所以他无法连通，我只需要把应用部署到服务器上就行了，但是记得，如果部署的机器，和 prometheus 不是同一台机器的话，prometheus.yml 中的ip 需要改成对应的地址。

### 配置dashboard

[12、Grafana 部署](2、相关技术/24、项目/3、数藏项目/2、中间件部署/12、Grafana%20部署.md)

完成 prometheus 的配置并重启之后，就可以到 grafana 上配置 dashboard了。配置流程和 grafana 部署那篇内容一模一样，只不过模板不一样。

直接用模板就行了，去这里搜：[https://grafana.com/grafana/dashboards/](https://grafana.com/grafana/dashboards/)

我用的是：4071做 JVM 监控（[https://grafana.com/grafana/dashboards/4701-jvm-micrometer/](https://grafana.com/grafana/dashboards/4701-jvm-micrometer/)）、18469做 DUBBO 监控（[https://grafana.com/grafana/dashboards/18469-dubbo-application-dashboard/](https://grafana.com/grafana/dashboards/18469-dubbo-application-dashboard/)），配置上之后，过个几分钟，页面上就能看到监控指标了。



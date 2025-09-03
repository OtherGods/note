
# 1、前言

在系统中，我们需要一个系统监控的东西。它就像我们的眼睛，有了这双眼睛我们知道系统到底发生了什么，服务器当前运行状态压力等等。因此系统监控是非常关键和重要。

接下来我就分享下技术派中利用Actuator+Prometheus+Grafana搭建的监控系统。

# 2、SpringBoot监控器—Actuator采集数据

Spring Boot自带监控功能——Actuator；可以帮助我们对程序内部运行情况监控，比如监控Bean加载情况、环境变量、日志信息和线程信息等等。

## 2.1、引入Actuator依赖

```xml
<!-- 引入actuator -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## 2.2、yml中配置Actuator

```yml
#  Actuator 配置：暴露所有断点
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

## 2.3、启动测试

启动之后访问http://127.0.0.1:8080/actuator地址，可以看到如下Actuator所暴露的系统信息：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102215569.png)

其实每个href的URL链接都是可以访问的，下面我们访问一个http://127.0.0.1:8080/actuator/beans看看。这里其实显示的就是加载的Bean情况。

# 3、Prometheus系统监控

Prometheus会定时拉取数据并保存，而且提供搜索和展示。

## 3.1、引入Prometheus依赖

```xml
<!-- 引入prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

此依赖的作用就类似于适配器，然后它会将Actuator所监控的数据转换成Prometheus所能识别的数据。

## 3.2、yml中配置Prometheus

```yml 中Prometheus配置
management:
	endpoints:
		web:
		  exposure:
			include: "*"
	# 配置暴露 Protheus，并允许将我的列表导入到 Prometheus
	endpoint:
		prometheus:
			enabled: true
		health:
			show-details: ALWAYS
	metrics:
	    export:
	      prometheus:
	        enabled: true
```

主要配置的就是开启Prometheus。

备注：在yml中配置spring.application.name=paicoding

然后在启动类中添加一个方法——设置Prometheus中的服务名称
```java
/**
 * 配置普罗米斯修中显示的服务名称
*/
@Bean
MeterRegistryCustomizer<MeterRegistry> configurer(@Value("${spring.application.name}") String applicationName) {

    return (registry -> registry.config().commonTags("application", applicationName));

}
```

## 3.3、系统启动测试

启动之后，访问http://127.0.0.1:8080/actuator地址，可以发现比刚才多了http://127.0.0.1:8080/actuator/prometheus的地址。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241058122.png)

下面我们来访问下http://127.0.0.1:8080/actuator/prometheus。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241058053.png)

可以看到prometheus拿到Actuator后所聚合展示的数据，有线程、CPU和系统开始时间等等各个监控指标。

## 3.4、Prometheus配置文件(Prometheus启动时所映射文件)

在/soft/prometheus文件夹下创建prometheus.yml文件

```yml prometheus.yml配置
global:
  scrape_interval:     5s
  evaluation_interval: 5s

scrape_configs:
	# 固定prometheus配置——必须
	- job_name: 'prometheus'
	static_configs:
		- targets: ['127.0.0.1:9090']
	# 监听pai-coding
	- job_name: 'pai-coding'
	# 5s获取一次数据
	scrape_interval: 5s
	# 接口地址固定
	metrics_path: '/actuator/prometheus'
	static_configs:
		# 你自己的系统地址
	    - targets: ['127.0.0.1:8080']
```

## 3.5、Docker启动Prometheus

先下载镜橡：
```shell
docker pull prom/prometheus
```

然后启动镜像
```shell
docker run -d --name=prometheus -p 9090:9090 -v /soft/prometheus/prometheus.yml:/soft/prometheus/prometheus.yml prom/prometheus --ip=127.0.0.1 --network macvlan31 bitnami/prometheus:latest
```

查看是否正常启动
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241105792.png)

## 3.6、访问Prometheus客户端

访问URL：http://127.0.0.1:9090/
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241517692.png)

点开Targets如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241517244.png)

State为up说明系统正常；down为异常。

# 4、Grafana可视化工具

## 4.1、Docker下载启动Grafana

Docker下载镜像
```shell 下载grafana镜像
docker pull grafana/grafana
```

Docker启动Grafana
```shell 启动Docker镜像
docker run -d --name=grafana -p 3000:3000 grafana/grafana
```
## 4.2、导入JVM (Micrometer) dashboard

grafana端口号是3000，我们访问URL：http://127.0.0.1:3000
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241519405.png)

默认用户名：admin；密码：123456

接下来开始导入JVM (Micrometer)看板
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241520876.png)

然后输入JVM (Micrometer)的id(4701)，然后点击load。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241520238.png)

选择数据源。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241520938.png)

然后就可以看到看板数据了。如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241521292.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241521213.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241521367.png)

上面可以看到很多监控指标，大家可以好后琢磨琢磨哈，我这里就不详细介绍了。
PS：Grafana的UI做的是真好看。绝绝子，实名点赞。

# 5、总结

总结：Actuator 提供端点将数据暴露出来， Prometheus 定时去拉取数据并保存和提供搜索和展示， Grafana 提供更加精美的图像化展示。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312241642503.png)


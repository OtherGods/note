# 1.概述

除了隔离依赖服务的调用以外，Hystrix还提供了**准实时**的调用监控（Hystrix Dashboard），Hystrix会持续地记录所有通过Hystrix发起的请求的执行信息，并以统计报表和图形的形式展示给用户，包括每秒执行多少请求多少成功，多少失败等。

Netflix通过hystrix-metrics-event-stream项目实现了对以上指标的监控。Spring Cloud也提供了Hystrix Dashboard的整合，对**监控内容转化成可视化界面**。

# 2.环境搭建步骤

- 在前面几节中的消费者中添加pom依赖。　　
```xml
<!-- hystrix和 hystrix-dashboard相关 -->
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
</dependency>
```

- 在启动类上添加注解
添加@EnableHystrixDashboard 开启Dashboard。
```java
@SpringBootApplication
@EnableDiscoveryClient // 开启EurekaClient功能
@EnableFeignClients // 开启Feign功能
@EnableCircuitBreaker//对hystrixR熔断机制的支持
@EnableHystrixDashboard //开启仪表盘
public class SpringcloudDemoConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringcloudDemoConsumerApplication.class, args);
	}
}
```
- 注册HystrixMetricsStreamServlet
在2.x之前的版本中，会自动注入该Servlet的，但是在2.x之后的版本，没有自动注册该Servlet。看源码的解释。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190804170422934.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)所以这里需要我们手动的注册该Servlet到容器中，代码如下：
```java
/**
 * 配置Hystrix.stream的servlet
 * @return
 */
@Bean
public ServletRegistrationBean registrationBean() {
	HystrixMetricsStreamServlet streamServlet = new HystrixMetricsStreamServlet();
	ServletRegistrationBean registrationBean = new ServletRegistrationBean(streamServlet);
	registrationBean.setLoadOnStartup(1);
	registrationBean.addUrlMappings("/hystrix.stream");
	registrationBean.setName("HystrixMetricsStreamServlet");
	return registrationBean;
}
```
- 访问http://localhost:80/hystrix 这里是你部署dashboard服务的地址和端口，会出现如下所示界面：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190804162924419.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

# 3.测试

这里是在你需要监控的路径后面跟上/hystrix.stream，就可以自动ping目标服务器的信息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190804170921279.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)点击Monitor Stream 按钮，出现如下面板:
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190804171114979.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

# 4.如何查看

- 7色
- 1圈
实心圆：
共有两种含义。它通过颜色的变化代表了实例的健康程度，它的健康度从绿色<黄色<橙色<红色递减。
该实心圆除了颜色的变化之外，它的大小也会根据实例的请求流量发生变化，流量越大该实心圆就越大。所以通过该实心圆的展示，就可以在大量的实例中快速的发现故障实例和高压力实例。
- 1线
曲线：用来记录2分钟内流量的相对变化，可以通过它来观察到流量的上升和下降趋势。
面板每个指标所代表的意义：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190804171328311.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
搞懂一个才能看懂复杂的！
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190804171401769.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

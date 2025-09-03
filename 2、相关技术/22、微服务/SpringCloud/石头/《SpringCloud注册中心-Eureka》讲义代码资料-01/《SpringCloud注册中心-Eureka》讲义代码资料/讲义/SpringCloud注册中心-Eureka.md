# 1.Eureka注册中心

## 1.1.Eureka简介

首先我们来解决第一问题，**服务的管理**。
> 问题分析

在刚才的案例中，user-service对外提供服务，需要对外暴露自己的地址。而consumer（调用者）需要记录服务提供者的地址。将来地址出现变更，还需要及时更新。这在服务较少的时候并不觉得有什么，但是在现在日益复杂的互联网环境，一个项目肯定会拆分出十几，甚至数十个微服务。此时如果还人为管理地址，不仅开发困难，将来测试、发布上线都会非常麻烦，这与DevOps的思想是背道而驰的。

> 网约车

这就好比是网约车出现以前，人们出门叫车只能叫出租车。一些私家车想做出租却没有资格，被称为黑车。而很多人想要约车，但是无奈出租车太少，不方便。私家车很多却不敢拦，而且满大街的车，谁知道哪个才是愿意载人的。一个想要，一个愿意给，就是缺少引子，缺乏管理啊。

此时滴滴这样的网约车平台出现了，所有想载客的私家车全部到滴滴注册，记录你的车型（服务类型），身份信息（联系方式）。这样提供服务的私家车，在滴滴那里都能找到，一目了然。

此时要叫车的人，只需要打开APP，输入你的目的地，选择车型（服务类型），滴滴自动安排一个符合需求的车到你面前，为你服务，完美！

Eureka是Netflix的一个子模块，也是核心模块之一。Eureka是一个基于REST服务，用于定位服务，以实现云端中间层服务发现和故障转移。服务注册与发现对于微服务架构来说是非常重要的，有了服务发现与注册，只需要使用服务的标识符，就可以访问到服务，而不需要修改服务调用的配置文件了。功能类似于dubbo的注册中心，比如Zookeeper。

## 1.2.Eureka基本架构

SpringCloud封装了Netflix公司开发的Eureka模块来实现服务注册和发现（请对比Zookeeper)。Eureka采用了C-S的设计架构。EurekaServer作为服务注册功能的服务器，它是服务注册中心。而系统中的其他微服务，使用Eureka的客户端连接到EurekaServer并维持心路连接。这样系统的维护人员就可以通过EurekaServer来监控系统中各个微服务是否正常运行。SpringCloud的一些其他模块（比如Zuul）就可以通过EurekaServer来发现系统中的其他微服务，并执行相关的逻辑。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502183246824.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

- Eureka：就是服务注册中心（可以是一个集群），对外暴露自己的地址
- 提供者：启动后向Eureka注册自己信息（地址，提供什么服务）
- 消费者：向Eureka订阅服务，Eureka会将对应服务的所有提供者地址列表发送给消费者，并且定期更新
- 心跳(续约)：提供者定期通过http方式向Eureka刷新自己的状态

注意和Dubbo对比：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502183314346.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

### 1.2.1 EurekaServer(注册中心)

EurekaServer作为一个独立的部署单元，以RESTAPI的形式为服务实例提供了注册、管理和查询等操作。同时，EurekaServer也为我们提供了可视化的监控页面，可以直观地看到各个EurekaServer当前的运行状态和所有已注册服务的情况。

### 1.2.2 EurekaClient(客户端)

●**服务注册**：
启动时，会调用服务注册方法，向EurekaServer注册自己的信息。EurekaServer会维护一个已注册服务的列表。当实例状态发生变化时（如自身检测认为Down的时候），也会向EurekaServer更新自己的服务状态，同时用replicateToPeers()向其它EurekaServer节点做状态同步。

●**续约与剔除**：
服务实例启动后，会周期性地向EurekaServer发送心跳以续约自己的信息，避免自己的注册信息被剔除。续约的方式与服务注册基本一致，首先更新自身状态，再同步到其它Peer。如果EurekaServer在一段时间内没有接收到某个微服务节点的心跳，EurekaServer将会注销该微服务节点（自我保护模式除外）。

●**服务消费**：ServiceConsumer本质上也是一个EurekaClient。它启动后，会从EurekaServer上获取所有实例的注册信息，包括IP地址、端口等，并缓存到本地。这些信息默认每30秒更新一次。前文提到过，如果与EurekaServer通信中断，ServiceConsumer仍然可以通过本地缓存与ServiceProvider通信。

●**三处缓存**
EurekaServer对注册列表进行缓存，默认时间为30s。
EurekaClient对获取到的注册信息进行缓存，默认时间为30s。
Ribbon会从上面提到的EurekaClient获取服务列表，将负载均衡后的结果缓存30s。
# 2.Eureka项目的构建

我们做三个角色
EurekaServer：提供服务注册和发现；
ServiceProvider：服务提供方，将自身服务注册到Eureka，从而使服务消费方能够找到；
ServiceConsumer：服务消费方，从Eureka获取注册服务列表，从而能够消费服务。

## 2.1 新建一个工程EurekaServer-7001

●**依然使用spring提供的快速搭建工具**：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502184941320.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
●**选择依赖**：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502185025934.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
●**完整的Pom文件**：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>2.1.4.RELEASE</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.bruceliu.eureka.server</groupId>
	<artifactId>eureka-server-7001</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>eureka-server-7001</name>
	<description>Demo project for Spring Boot</description>

	<properties>
		<java.version>1.8</java.version>
		<spring-cloud.version>Greenwich.SR1</spring-cloud.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

</project>

```
●**编写启动类**：
```java
@SpringBootApplication
@EnableEurekaServer // 声明这个应用是一个EurekaServer
public class EurekaServer7001Application {

	public static void main(String[] args) {
		SpringApplication.run(EurekaServer7001Application.class, args);
	}

}
```
●**编写配置**：
```yml
server:
  port: 7001 # 端口
spring:
  application:
    name: eureka-server-7001 # 应用名称，会在Eureka中显示
eureka:
  client:
    register-with-eureka: false # 是否注册自己的信息到EurekaServer，默认是true
    fetch-registry: false #false表示自己端就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    service-url: # EurekaServer的地址，现在是自己的地址，如果是集群，需要加上其它Server的地址。
      defaultZone: http://127.0.0.1:${server.port}/eureka
```
●**启动服务，并访问**：http://127.0.0.1:7001
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502191632905.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502192001704.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

## 2.2.将user-service注册到Eureka

注册服务，就是在服务上添加Eureka的客户端依赖，客户端代码会自动把服务注册到EurekaServer中。
我们在springcloud-demo中添加Eureka客户端依赖：
●**先添加SpringCloud依赖**：

```xml
	<!-- SpringCloud的依赖 -->
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>
```
●**然后是Eureka客户端**：
```xml
	<!-- Eureka客户端 -->
	<dependency>
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
	</dependency>
```
> 在启动类上开启Eureka客户端功能

通过添加`@EnableDiscoveryClient`来开启Eureka客户端功能
```java
@SpringBootApplication
@EnableEurekaClient // 开启EurekaClient功能
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        System.out.println("提供者启动:7001");
    }
}
```
●**编写配置**
```properties
# 应用名称
spring.application.name=springcloud-demo-service
# EurekaServer地址
eureka.client.service-url.defaultZone=http://127.0.0.1:7001/eureka
# 当调用getHostname获取实例的hostname时，返回ip而不是host名称
eureka.instance.prefer-ip-address=true
# 指定自己的ip信息，不指定的话会自己寻找
eureka.instance.ip-address=127.0.0.1
```
注意：

- 这里我们添加了spring.application.name属性来指定应用名称，将来会作为应用的id使用。
- 不用指定register-with-eureka和fetch-registry，因为默认是true

●**重启项目，访问Eureka监控页面查看**
![](https://img-blog.csdnimg.cn/20190502212641384.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
我们发现service服务已经注册成功了!!!

## 2.3 消费者从Eureka获取服务

接下来我们修改springcloud-demo-consumer，尝试从EurekaServer获取服务。
方法与消费者类似，只需要在项目中添加EurekaClient依赖，就可以通过服务名称来获取信息了！
添加依赖：
●**先添加SpringCloud依赖**：

```xml
	<!-- SpringCloud的依赖 -->
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>
```
●**然后是Eureka客户端**：
```xml
<!-- Eureka客户端 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```
●**在启动类开启Eureka客户端**：
```java
@SpringBootApplication
@EnableDiscoveryClient // 开启EurekaClient功能
public class SpringcloudDemoConsumerApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringcloudDemoConsumerApplication.class, args);
	}
}
```
●**修改配置**：
```properties
spring.application.name=springcloud-demo-consumer
# EurekaServer地址
eureka.client.service-url.defaultZone=http://127.0.0.1:7001/eureka
# 当调用getHostname获取实例的hostname时，返回ip而不是host名称
eureka.instance.prefer-ip-address=true
# 指定自己的ip信息，不指定的话会自己寻找
eureka.instance.ip-address=127.0.0.1
```
●**修改代码，用DiscoveryClient类的方法，根据服务名称，获取服务实例**：
```java
/**
 * @author bruceliu
 * @create 2019-05-02 15:52
 * @description
 */
@RestController
@RequestMapping("consumer")
public class ConsumerController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiscoveryClient client;// Eureka客户端，可以获取到服务实例信息

    @RequestMapping("/test")
    public List<User> consumerTest(){
        List<String>list=client.getServices();
        System.out.println("*服务列表*"+list);
        List<ServiceInstance> srvList=client.getInstances("springcloud-demo-service");
        for(ServiceInstance element:srvList){
            System.out.println(element.getServiceId()+"\t"+element.getHost()+"\t"+element.getPort()+"\t" +element.getUri());
        }
        // 因为只有一个UserService,因此我们直接get(0)获取
        ServiceInstance instance = srvList.get(0);

        // 获取ip和端口信息
        String baseUrl = "http://"+instance.getHost() + ":" + instance.getPort()+"/all";
        System.out.println("访问地址:"+baseUrl);

        return this.restTemplate.getForObject(baseUrl,List.class);
    }
}
```
●**注册中心**：![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502221002332.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
●**访问测试**：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502221140487.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
# 3.Eureka详解

接下来我们详细讲解Eureka的原理及配置。

## 3.1.基础架构

Eureka架构中的三个核心角色：
- **服务注册中心**
Eureka的服务端应用，提供服务注册和发现功能，就是刚刚我们建立的eureka-server-7001

- **服务提供者**
  提供服务的应用，可以是SpringBoot应用，也可以是其它任意技术实现，只要对外提供的是Rest风格服务即可。本例中就是我们实现的springcloud-demo

- **服务消费者**
  消费应用从注册中心获取服务列表，从而得知每个服务方的信息，知道去哪里调用服务方。本例中就是我们实现的springcloud-demo-consumer
## 3.2 注册微服务信息完善

●**主机名称**：服务名称修改默认：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502222441458.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

默认名字:   houstname + applicationName +  port

●**在springcloud-demo属性文件中加入**

```properties
eureka.instance.instance-id=springcloud-demo-service-80
```
●**修改后，查看页面**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502222844925.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

## 3.3.高可用的Eureka Server(HA)

配置的参考地址：https://blog.csdn.net/liupeifeng3514/article/details/85273961

Eureka Server即服务的注册中心，在刚才的案例中，我们只有一个EurekaServer，事实上EurekaServer也可以是一个集群，形成高可用的Eureka中心。

> 服务同步

多个Eureka Server之间也会互相注册为服务，当服务提供者注册到Eureka Server集群中的某个节点时，该节点会把服务的信息同步给集群中的每个节点，从而实现**数据同步**。因此，无论客户端访问到Eureka Server集群中的任意一个节点，都可以获取到完整的服务列表信息。

> 动手搭建高可用的EurekaServer

我们假设要搭建两条EurekaServer的集群，端口分别为：7001、7002、7003

1）我们修改原来的EurekaServer配置-7001：
```yaml
server:
  port: 7001 # 端口
spring:
  application:
    name: eureka-server-7001 # 应用名称，会在Eureka中显示
eureka:
  client:
    register-with-eureka: false # 是否注册自己的信息到EurekaServer，默认是true
    fetch-registry: false #false表示自己端就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    service-url: # EurekaServer的地址，现在是自己的地址，如果是集群，需要加上其它Server的地址。
      defaultZone: http://127.0.0.1:7002/eureka,http://127.0.0.1:7003/eureka
```
所谓的高可用注册中心，其实就是把EurekaServer自己也作为一个服务进行注册，这样多个EurekaServer之间就能互相发现对方，从而形成集群。因此我们做了以下修改：
- 把service-url的值改成了另外一台EurekaServer的地址，而不是自己

2）我们修改原来的EurekaServer配置-7002：
```yaml
server:
  port: 7002 # 端口
spring:
  application:
    name: eureka-server-7002 # 应用名称，会在Eureka中显示
eureka:
  client:
    register-with-eureka: false # 是否注册自己的信息到EurekaServer，默认是true
    fetch-registry: false #false表示自己端就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    service-url: # EurekaServer的地址，现在是自己的地址，如果是集群，需要加上其它Server的地址。
      defaultZone: http://127.0.0.1:7001/eureka,http://127.0.0.1:7003/eureka
```
3）我们修改原来的EurekaServer配置-7003：
```yaml
server:
  port: 7003 # 端口
spring:
  application:
    name: eureka-server-7003 # 应用名称，会在Eureka中显示
eureka:
  client:
    register-with-eureka: false # 是否注册自己的信息到EurekaServer，默认是true
    fetch-registry: false #false表示自己端就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    service-url: # EurekaServer的地址，现在是自己的地址，如果是集群，需要加上其它Server的地址。
      defaultZone: http://127.0.0.1:7001/eureka,http://127.0.0.1:7002/eureka
```
4）在自己的host中加入端口映射
```xml
127.0.0.1       eureka7001.com
127.0.0.1       eureka7002.com
127.0.0.1       eureka7003.com
```
5）启动测试
注意：idea中一个应用不能启动两次，我们需要重新配置一个启动器：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019050311032911.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190503110406628.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
然后启动即可。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190503110829934.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190503110912317.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190503110940191.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
6）springcloud-demo集群版，客户端注册服务到集群
因为EurekaServer不止一个，因此注册服务的时候，service-url参数需要变化：
```properties
# EurekaServer地址
eureka.client.service-url.defaultZone= http:// eureka7001.com:7001/eureka,http://eureka7002.com:7002/eureka,http://eureka7003.com:7003/eureka
```
7）测试服务是否注册
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190503111448875.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
## 3.4.服务提供者

服务提供者要向EurekaServer注册服务，并且完成服务续约等工作。

> 服务注册

服务提供者在启动时，会检测配置属性中的：`eureka.client.register-with-erueka=true`参数是否正确，事实上默认就是true。如果值确实为true，则会向EurekaServer发起一个Rest请求，并携带自己的元数据信息，Eureka Server会把这些信息保存到一个双层Map结构中。第一层Map的Key就是服务名称，第二层Map的key是服务的实例id。

> 服务续约

在注册服务完成以后，服务提供者会维持一个心跳（定时向EurekaServer发起Rest请求），告诉EurekaServer：“我还活着”。这个我们称为服务的续约（renew）；

有两个重要参数可以修改服务续约的行为：

```yaml
eureka:
  instance:
    lease-expiration-duration-in-seconds: 90
    lease-renewal-interval-in-seconds: 30
```

- lease-renewal-interval-in-seconds：服务续约(renew)的间隔，默认为30秒
- lease-expiration-duration-in-seconds：服务失效时间，默认值90秒

也就是说，默认情况下每个30秒服务会向注册中心发送一次心跳，证明自己还活着。如果超过90秒没有发送心跳，EurekaServer就会认为该服务宕机，会从服务列表中移除，这两个值在生产环境不要修改，默认即可。

但是在开发时，这个值有点太长了，经常我们关掉一个服务，会发现Eureka依然认为服务在活着。所以我们在开发阶段可以适当调小。

```yaml
eureka:
  instance:
    lease-expiration-duration-in-seconds: 10 # 10秒即过期
    lease-renewal-interval-in-seconds: 5 # 5秒一次心跳
```


> 实例id

先来看一下服务状态信息：

在Eureka监控页面，查看服务注册信息：



> 实例id

先来看一下服务状态信息：

在Eureka监控页面，查看服务注册信息：

[外链图片转存失败,源站可能有防盗链机制,建议将图片保存下来直接上传(img-CG1AebF5-1571148411500)(assets/1525617060656.png)]

在status一列中，显示以下信息：

- UP(1)：代表现在是启动了1个示例，没有集群
- DESKTOP-2MVEC12:user-service:8081：是示例的名称（instance-id），
  - 默认格式是：`${hostname} + ${spring.application.name} + ${server.port}`
  - instance-id是区分同一服务的不同实例的唯一标准，因此不能重复。

我们可以通过instance-id属性来修改它的构成：

```yaml
eureka:
  instance:
    instance-id: ${spring.application.name}:${server.port}
```

重启服务再试试看：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190503112350936.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
在status一列中，显示以下信息：

- UP(1)：代表现在是启动了1个示例，没有集群
- localhost:springcloud-demo-service:80：是示例的名称（instance-id），
  - 默认格式是：`${hostname} + ${spring.application.name} + ${server.port}`
  - instance-id是区分同一服务的不同实例的唯一标准，因此不能重复。

我们可以通过instance-id属性来修改它的构成：

```yaml
eureka:
  instance:
    instance-id: ${spring.application.name}:${server.port}
```

重启服务再试试看：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190503112419687.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
## 3.5.服务消费者

> 获取服务列表

当服务消费者启动是，会检测`eureka.client.fetch-registry=true`参数的值，如果为true，则会从Eureka Server服务的列表只读备份，然后缓存在本地。并且`每隔30秒`会重新获取并更新数据。我们可以通过下面的参数来修改：

```yaml
eureka:
  client:
    registry-fetch-interval-seconds: 5
```
生产环境中，我们不需要修改这个值。
但是为了开发环境下，能够快速得到服务的最新状态，我们可以将其设置小一点。
## 3. 5.失效剔除和自我保护

> 失效剔除

有些时候，我们的服务提供方并不一定会正常下线，可能因为内存溢出、网络故障等原因导致服务无法正常工作。Eureka Server需要将这样的服务剔除出服务列表。因此它会开启一个定时任务，每隔60秒对所有失效的服务（超过90秒未响应）进行剔除。

可以通过`eureka.server.eviction-interval-timer-in-ms`参数对其进行修改，单位是毫秒，生成环境不要修改。

这个会对我们开发带来极大的不变，你对服务重启，隔了60秒Eureka才反应过来。开发阶段可以适当调整，比如10S

> 自我保护

我们关停一个服务，就会在Eureka面板看到一条警告：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190503112637132.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
这是触发了Eureka的自我保护机制。当一个服务未按时进行心跳续约时，Eureka会统计最近15分钟心跳失败的服务实例的比例是否超过了85%。在生产环境下，因为网络延迟等原因，心跳失败实例的比例很有可能超标，但是此时就把服务剔除列表并不妥当，因为服务可能没有宕机。Eureka就会把当前实例的注册信息保护起来，不予剔除。生产环境下这很有效，保证了大多数服务依然可用。

但是这给我们的开发带来了麻烦， 因此开发阶段我们都会关闭自我保护模式：

```yaml
eureka:
  server:
    enable-self-preservation: false # 关闭自我保护模式（缺省为打开）
    eviction-interval-timer-in-ms: 1000 # 扫描失效服务的间隔时间（缺省为60*1000ms）
```



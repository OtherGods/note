### 1.概述
通过前面的学习，使用Spring Cloud实现微服务的架构基本成型，大致是这样的：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190805211831541.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
前面的文章我们介绍了，Eureka用于服务的注册于发现，Feign支持服务的调用以及均衡负载，Hystrix处理服务的熔断防止故障扩散，Spring Cloud Config服务集群配置中心，似乎一个微服务框架已经完成了。

服务网关是微服务架构中一个不可或缺的部分。通过服务网关统一**向外**系统提供REST API的过程中，除了具备**服务路由**、**均衡负载**功能之外，它还具备了**权限控制**等功能。Spring Cloud Netflix中的Zuul就担任了这样的一个角色，为**微服务架构提供了前门保护**的作用，同时将权限控制这些较重的非业务逻辑内容迁移到服务路由层面，使得服务集群主体能够具备更高的可复用性和可测试性。

### 2.ZUUL概述

官网：https://github.com/Netflix/zuul
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190805212346872.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
Zuul：维基百科：
电影《捉鬼敢死队》中的怪兽，Zuul，在纽约引发了巨大骚乱。
事实上，在微服务架构中，Zuul就是守门的大Boss！一夫当关，万夫莫开!!

Spring Cloud Zuul路由是微服务架构的不可或缺的一部分，提供动态路由，监控，弹性，安全等的边缘服务。Zuul是Netflix出品的一个基于JVM路由和服务端的负载均衡器。


> 在Spring Cloud体系中， Spring Cloud Zuul就是提供负载均衡、反向代理、权限认证的一个API gateway。


###  3.Zuul加入后的架构
 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20190805212628308.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
Zuul可以通过加载动态过滤机制，从而实现以下各项功能：
1.**验证与安全保障**: 识别面向各类资源的验证要求并拒绝那些与要求不符的请求。
2.**审查与监控**: 在边缘位置追踪有意义数据及统计结果，从而为我们带来准确的生产状态结论。
3.**动态路由**: 以动态方式根据需要将请求路由至不同后端集群处。
4.**压力测试**: 逐渐增加指向集群的负载流量，从而计算性能水平。
5.**负载分配**: 为每一种负载类型分配对应容量，并弃用超出限定值的请求。
6.**静态响应处理**: 在边缘位置直接建立部分响应，从而避免其流入内部集群。
7.**多区域弹性**: 跨越AWS区域进行请求路由，旨在实现ELB使用多样化并保证边缘位置与使用者尽可能接近。
　　
> 不管是来自于客户端（PC或移动端）的请求，还是服务内部调用。一切对服务的请求都会经过Zuul这个网关，然后再由网关来实现鉴权、动态路由等等操作。Zuul就是我们服务的统一入口。


###  4.快速入门
#### 4.1.新建工程
填写基本信息
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190805214657848.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)添加Zuul依赖：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.4.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <groupId>com.bruceliu.springcloud.zuul</groupId>
    <artifactId>springcloud_zuul</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <java.version>1.8</java.version>
        <spring-cloud.version>Greenwich.SR1</spring-cloud.version>
    </properties>

    <dependencies>

        <!--导入Zuul的依赖-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
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

#### 4.2.编写启动类

通过`@EnableZuulProxy `注解开启Zuul的功能：
```java
/**
 * @author bruceliu
 * @create 2019-08-05 21:52
 * @description
 */
@SpringBootApplication
@EnableZuulProxy // 开启Zuul的网关功能
public class ZuulDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZuulDemoApplication.class, args);
    }
}
```
#### 4.3.编写配置
```yml
server:
  port: 10010 #服务端口
spring:
  application:
    name: api-gateway #指定服务名
```
#### 4.4.编写路由规则

我们需要用Zuul来代理user-service服务，先看一下控制面板中的服务状态：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190805220932918.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70) ip为：127.0.0.1
端口为：8001
映射规则：
```yml
zuul:
  routes:
    user-service: # 这里是路由id，随意写
      path: /user-service/** # 这里是映射路径
      url: http://127.0.0.1:8001 # 映射路径对应的实际url地址
```
我们将符合`path` 规则的一切请求，都代理到 `url`参数指定的地址
本例中，我们将 `/user-service/**`开头的请求，代理到http://127.0.0.1:8001

#### 4.5.启动测试
访问的路径中需要加上配置规则的映射路径，我们访问：http://127.0.0.1:10010/user-service/get/1
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190805223024906.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
#### 3.6.面向服务的路由
在刚才的路由规则中，我们把路径对应的服务地址写死了！如果同一服务有多个实例的话，这样做显然就不合理了。
我们应该根据服务的名称，去Eureka注册中心查找 服务对应的所有实例列表，然后进行动态路由才对！
#### 3.6.1.添加Eureka客户端依赖
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```
#### 3.6.2.开启Eureka客户端发现功能
```java
/**
 * @author bruceliu
 * @create 2019-08-05 21:52
 * @description
 */
@SpringBootApplication
@EnableZuulProxy // 开启Zuul的网关功能
@EnableDiscoveryClient
public class ZuulDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZuulDemoApplication.class, args);
    }
}
```
#### 3.6.3.添加Eureka配置，获取服务信息
```yml
eureka:
  client:
    registry-fetch-interval-seconds: 5 # 获取服务列表的周期：5s
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/,http://eureka7002.com:7002/eureka/,http://eureka7003.com:7003/eureka/
  instance:
    prefer-ip-address: true
    ip-address: 127.0.0.1
```
#### 3.6.4.修改映射配置，通过服务名称获取
因为已经有了Eureka客户端，我们可以从Eureka获取服务的地址信息，因此映射时无需指定IP地址，而是通过服务名称来访问，而且Zuul已经集成了Ribbon的负载均衡功能。
```yml
zuul:
  routes:
    user-service: # 这里是路由id，随意写
      path: /user-service/** # 这里是映射路径
      serviceId: springcloud-demo-service # 指定服务名称
```
#### 3.6.5.启动测试
再次启动，这次Zuul进行代理时，会利用Ribbon进行负载均衡访问：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190805224748135.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
日志中可以看到使用了负载均衡器：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190805224843224.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
#### 3.7.简化的路由配置
在刚才的配置中，我们的规则是这样的：
- `zuul.routes.<route>.path=/xxx/**`： 来指定映射路径。`<route>`是自定义的路由名
- `zuul.routes.<route>.serviceId=服务名`：来指定服务名。

而大多数情况下，我们的`<route>`路由名称往往和服务名会写成一样的。因此Zuul就提供了一种简化的配置语法：`zuul.routes.<serviceId>=<path>`
比方说上面我们关于user-service的配置可以简化为一条：
```yml
# 简写配置
zuul:
  routes:
    springcloud-demo-service: /user-service/** # 这里是映射路径
```
省去了对服务名称的配置。

#### 3.8.默认的路由规则
在使用Zuul的过程中，上面讲述的规则已经大大的简化了配置项。但是当服务较多时，配置也是比较繁琐的。因此Zuul就指定了默认的路由规则：

- 默认情况下，一切服务的映射路径就是服务名本身。
- 例如服务名为：`springcloud-demo-service`，则默认的映射路径就是：`/springcloud-demo-service/**`

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190805230111457.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
也就是说，刚才的映射规则我们完全不配置也是OK的，不信就试试看。

### 3.9.路由前缀
配置示例：
```yml
zuul:
  prefix: /api # 添加路由前缀
  routes:
    user-service: # 这里是路由id，随意写
      path: /user-service/** # 这里是映射路径
      serviceId: springcloud-demo-service # 指定服务名称
```
我们通过`zuul.prefix=/api`来指定了路由的前缀，这样在发起请求时，路径就要以/api开头。
路径`/api/springcloud-demo-service/get/1`将会被代理到`/user-service/user/1`
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190805230658700.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019080523063189.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70) 
# 4.负载均衡和熔断
Zuul中默认就已经支持了Ribbon负载均衡和Hystix熔断机制。但是所有的超时策略都是走的默认值，比如熔断超时时间只有1S，很容易就触发了。因此建议我们手动进行配置：

```java
zuul:
  retryable: true
ribbon:
  ConnectTimeout: 250 # 连接超时时间(ms)
  ReadTimeout: 2000 # 通信超时时间(ms)
  OkToRetryOnAllOperations: true # 是否对所有操作重试
  MaxAutoRetriesNextServer: 2 # 同一服务不同实例的重试次数
  MaxAutoRetries: 1 # 同一实例的重试次数
hystrix:
  command:
  	default:
        execution:
          isolation:
            thread:
              timeoutInMillisecond: 6000 # 熔断超时时长：6000ms
```


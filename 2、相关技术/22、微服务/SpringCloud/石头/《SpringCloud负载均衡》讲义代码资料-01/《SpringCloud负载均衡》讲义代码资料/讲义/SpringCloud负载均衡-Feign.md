# 1.Feign负载均衡简介

## 1.1 Feign是什么

Feign是一个声明式WebService客户端。使用Feign能让编写Web Service客户端更加简单, 它的使用方法是定义一个接口，然后在上面添加注解，同时也支持JAX-RS标准的注解。Feign也支持可拔插式的编码器和解码器。Spring Cloud对Feign进行了封装，使其支持了Spring MVC标准注解和HttpMessageConverters。Feign可以与Eureka和Ribbon组合使用以支持负载均衡。
- 官网解释：
http://projects.spring.io/spring-cloud/spring-cloud.html#spring-cloud-feign
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504153811809.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
Feign是一个声明式的Web服务客户端，使得编写Web服务客户端变得非常容易，
只需要创建一个接口，然后在上面添加注解即可。
参考官网：https://github.com/OpenFeign/feign

## 1.2 Feign主要作用

Feign旨在使编写Java Http客户端变得更容易。
前面在使用`Ribbon+RestTemplate`时，利用`RestTemplate`对http请求的封装处理，形成了一套模版化的调用方法。但是在实际开发中，由于对服务依赖的调用可能不止一处，往往一个接口会被多处调用，所以通常都会针对每个微服务自行封装一些客户端类来包装这些依赖服务的调用。所以，Feign在此基础上做了进一步封装，由他来帮助我们定义和实现依赖服务接口的定义。在Feign的实现下，我们只需创建一个接口并使用注解的方式来配置它(以前是Dao接口上面标注Mapper注解,现在是一个微服务接口上面标注一个Feign注解即可)，即可完成对服务提供方的接口绑定，简化了使用Spring cloud Ribbon时，自动封装服务调用客户端的开发量。

 - Feign集成了Ribbon
利用Ribbon维护了springcloud-demo-service的服务列表信息，并且通过轮询实现了客户端的负载均衡。而与Ribbon不同的是，通过feign只需要定义服务绑定接口且以声明式的方法，优雅而简单的实现了服务调用。
有道词典的英文解释：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504154223354.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
- 为什么叫伪装？
Feign可以把Rest的请求进行隐藏，伪装成类似SpringMVC的Controller一样。你不用再自己拼接url，拼接参数等等操作，一切都交给Feign去做。
# 2.快速入门

## 2.1.导入依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```
## 2.2.Feign的客户端

```java
/**
 * @author bruceliu
 * @create 2019-05-04 18:49
 * @description Feign客户端
 */
@FeignClient(value = "SPRINGCLOUD-DEMO-SERVICE")
public interface UserClientService {

    @RequestMapping("/all")
    public List<User> queryUsers();
}
```
- 首先这是一个接口，Feign会通过动态代理，帮我们生成实现类。这点跟mybatis的mapper很像
- `@FeignClient`，声明这是一个Feign客户端，类似`@Mapper`注解。同时通过`value`属性指定服务名称
- 接口中的定义方法，完全采用SpringMVC的注解，Feign会根据注解帮我们生成URL，并访问获取结果
改造原来的调用逻辑UserController：
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
    private UserClientService userService;

    @RequestMapping("/test")
    public List<User> consumerTest(){
        return userService.queryUsers();
    }
}
```
## 2.3.开启Feign功能

我们在启动类上，添加注解，开启Feign功能

```java
@SpringBootApplication
@EnableDiscoveryClient // 开启EurekaClient功能
@EnableFeignClients // 开启Feign功能
public class SpringcloudDemoConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringcloudDemoConsumerApplication.class, args);
	}
}
```

- 你会发现RestTemplate的注册被我删除了。Feign中已经自动集成了Ribbon负载均衡，因此我们不需要自己定义RestTemplate了

## 2.4.启动测试：

访问接口：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504192124375.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
正常获取到了结果。
# 3.负载均衡

Feign中本身已经集成了Ribbon依赖和自动配置：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504192349966.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
因此我们不需要额外引入依赖，也不需要再注册`RestTemplate`对象。

另外，我们可以像上节课中讲的那样去配置Ribbon，可以通过`ribbon.xx`来进行全局配置。也可以通过`服务名.ribbon.xx`来对指定服务配置：

```yaml
SPRINGCLOUD-DEMO-SERVICE:
  ribbon:
    ConnectTimeout: 250 # 连接超时时间(ms)
    ReadTimeout: 1000 # 通信超时时间(ms)
    OkToRetryOnAllOperations: true # 是否对所有操作重试
    MaxAutoRetriesNextServer: 1 # 同一服务不同实例的重试次数
    MaxAutoRetries: 1 # 同一实例的重试次数
```
# 4.小结

Feign通过接口的方法调用Rest服务（之前是Ribbon+RestTemplate），通过Feign直接找到服务接口，由于在进行服务调用的时候融合了Ribbon技术，所以也支持负载均衡作用。


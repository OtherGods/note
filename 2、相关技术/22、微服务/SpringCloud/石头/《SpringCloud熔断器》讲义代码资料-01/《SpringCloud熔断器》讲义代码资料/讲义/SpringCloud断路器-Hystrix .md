# 1.Hystrix断路器

## 1.1分布式系统面临的问题

复杂分布式体系结构中的应用程序有数十个依赖关系，每个依赖关系在某些时候将不可避免地失败。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190505224334182.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

- **服务雪崩**
多个微服务之间调用的时候，假设微服务A调用微服务B和微服务C，微服务B和微服务C又调用其它的微服务，这就是所谓的“扇出”。如果扇出的链路上某个微服务的调用响应时间过长或者不可用，对微服务A的调用就会占用越来越多的系统资源，进而引起系统崩溃，所谓的“雪崩效应”.

示例：
在微服务架构中通常会有多个服务层调用，基础服务的故障可能会导致级联故障，进而造成整个系统不可用的情况，这种现象被称为服务雪崩效应。服务雪崩效应是一种因“服务提供者”的不可用导致“服务消费者”的不可用，并将不可用逐渐放大的过程。

如下图所示：A作为服务提供者，B为A的服务消费者，C和D是B的服务消费者。A不可用引起了B的不可用，并将不可用像滚雪球一样放大到C和D时，雪崩效应就形成了。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20191017233956352.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

## 1.2 Hystrix概述

Hystrix [hɪst'rɪks]的中文含义是豪猪，因其背上长满了刺而拥有自我保护能力。

Hystix，即熔断器。类似保险丝角色！

主页：https://github.com/Netflix/Hystrix/
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190505224635560.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

- Hystrix是一个用于处理分布式系统的延迟和容错的开源库，在分布式系统里，许多依赖不可避免的会调用失败，比如超时、异常等，Hystrix能够保证在一个依赖出问题的情况下，不会导致整体服务失败，避免级联故障，以提高分布式系统的弹性。
- “断路器”本身是一种开关装置，当某个服务单元发生故障之后，通过断路器的故障监控（类似熔断保险丝），向调用方返回一个符合预期的、可处理的备选响应（**FallBack**），而不是长时间的等待或者抛出调用方无法处理的异常，这样就保证了服务调用方的线程不会被长时间、不必要地占用，从而避免了故障在分布式系统中的蔓延，乃至雪崩。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190505224837331.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
## 1.3.熔断器的工作机制：

![在这里插入图片描述](https://img-blog.csdnimg.cn/2019050522500225.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

正常工作的情况下，客户端请求调用服务API接口：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190505225103101.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

当有服务出现异常时，直接进行失败回滚，服务降级处理：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019050522513015.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

当服务繁忙时，如果服务出现异常，不是粗暴的直接报错，而是返回一个友好的提示，虽然拒绝了用户的访问，但是会返回一个结果。

这就好比去买鱼，平常超市买鱼会额外赠送杀鱼的服务。等到逢年过节，超时繁忙时，可能就不提供杀鱼服务了，这就是服务的降级。

系统特别繁忙时，一些次要服务暂时中断，优先保证主要服务的畅通，一切资源优先让给主要服务来使用，在双十一、618时，京东天猫都会采用这样的策略。
## 1.4 Hystrix主要功能

- 服务降级
- 服务熔断
- 服务限流
- 接近实时的监控
- ......

# 2. 服务降级

## 2.1.服务降级概述

整体资源快不够了，忍痛将某些服务先关掉，待渡过难关，再开启回来。服务降级处理是在客户端实现完成的，与服务端没有关系。

Fallback相当于是降级操作。对于查询操作，我们可以实现一个fallback方法，当请求后端服务出现异常的时候，可以使用fallback方法返回的值。 fallback方法的返回值一般是设置的默认值或者来自缓存。
## 2.2.引入依赖

首先在user-consumer中引入Hystix依赖：
```xml
<!--服务熔断组件-->
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
</dependency>
```
## 2.3.修改之前的Controller

在之前的Controller中添加熔断机制:
```java
@RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
// 一旦调用服务方法失败并抛出了错误信息后，会自动调用@HystrixCommand标注好的fallbackMethod调用类中的指定方法
@HystrixCommand(fallbackMethod = "processHystrix_Get")
public User get(@PathVariable("id") Long id) {
    User u = this.userService.get(id);
    if (null == u) {
        throw new RuntimeException("该ID：" + id + "没有没有对应的信息");
    }
    return u;
}

public User processHystrix_Get(@PathVariable("id") Long id) {
    User u=new User();
    u.setId(110);
    u.setUsername("该ID：" + id + "没有没有对应的信息,null--@HystrixCommand");
    u.setNote("no this database in MySQL");
    return u;
}
```
## 2.4.修改主启动类

修改consumer并添加新注解`@EnableCircuitBreaker`
```java
@SpringBootApplication
@EnableDiscoveryClient // 开启EurekaClient功能
@EnableFeignClients // 开启Feign功能
@EnableCircuitBreaker//对hystrixR熔断机制的支持
public class SpringcloudDemoConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringcloudDemoConsumerApplication.class, args);
	}
}
```
## 2.5. 服务熔断测试

- 3个eureka先启动
- 主启动类SpringcloudDemoConsumerApplication
- 访问测试
http://127.0.0.1:88/consumer/get/2
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190804150800487.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
如果对应的ID：3，数据库里面没有这个记录，我们报错后统一返回。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190804150857905.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
# 3. 服务降级优化-彻底解耦

- 修改microservicecloud-api工程，
根据已经有的DeptClientService接口新建一个实现了
FallbackFactory接口的类DeptClientServiceFallbackFactory
```java
/**
 * @author bruceliu
 * @create 2019-08-04 15:11
 * @description
 */
@Component // 不要忘记添加
public class UserClientServiceFallbackFactory implements FallbackFactory<UserClientService> {


    @Override
    public UserClientService create(Throwable throwable) {
        return new UserClientService() {
            @Override
            public List<User> queryUsers() {
                return null;
            }

            @Override
            public User get(Long id) {
                User u=new User();
                u.setId(110);
                u.setUsername("该ID：\" + id + \"没有没有对应的信息,null--服务降级~~");
                u.setNote("no this database in MySQL----服务降级！！！");
                return u;
            }
        };
    }
}
```
- 修改consumer工程，UserClientService接口在注解@FeignClient中添加fallbackFactory属性值
```java
/**
 * @author bruceliu
 * @create 2019-05-04 18:49
 * @description Feign客户端
 */
@FeignClient(value = "SPRINGCLOUD-DEMO-SERVICE",fallbackFactory=UserClientServiceFallbackFactory.class)
public interface UserClientService {

    @RequestMapping("/all")
    public List<User> queryUsers();

    @RequestMapping("/get/{id}")
    public User get(@PathVariable("id") Long id);
}
```
- 修改配置文件
```properties
# 开启服务熔断策略
feign.hystrix.enabled=true
```
## 3.1. 测试

- 3个eureka先启动
- 微服务提供者启动
- 微服务消费者启动
正常访问测试：http://127.0.0.1:88/consumer/get/1
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190804153841423.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
故意关闭微服务提供者
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190804153635197.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
客户端自己调用提示
此时服务端provider已经down了，但是我们做了服务降级处理，让客户端在服务端不可用时也会获得提示信息而不会挂起耗死服务器。

# 4.Hystrix服务熔断

## 4.1.服务熔断概述

熔断机制是应对雪崩效应的一种微服务链路保护机制。

断路器很好理解，当Hystrix Command请求后端服务失败数量超过一定比例(默认50%)，断路器会切换到开路状态(Open)。这时所有请求会直接失败而不会发送到后端服务。 断路器保持在开路状态一段时间后(默认10秒)，自动切换到半开路状态(HALF-OPEN)。这时会判断下一次请求的返回情况， 如果请求成功，断路器切回闭路状态(CLOSED)， 否则重新切换到开路状态(OPEN)。 Hystrix的断路器就像我们家庭电路中的保险丝，一旦后端服务不可用，断路器会直接切断请求链，避免发送大量无效请求影响系统吞吐量， 并且断路器有自我检测并恢复的能力。

熔断器开关相互转换的逻辑图：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20191017234534147.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
在SpringCloud框架里熔断机制通过Hystrix实现。Hystrix会监控微服务间调用的状况，当失败的调用到一定阈值，缺省是5秒内20次调用失败就会启动熔断机制。熔断机制的注解是`@HystrixCommand`。

我们来说说断路器的工作原理。当我们把服务提供者eureka-client中加入了模拟的时间延迟之后，在服务消费端的服务降级逻辑因为hystrix命令调用依赖服务超时，触发了降级逻辑，但是即使这样，受限于Hystrix超时时间的问题，我们的调用依然很有可能产生堆积。

这个时候断路器就会发挥作用，那么断路器是在什么情况下开始起作用呢？这里涉及到断路器的三个重要参数：快照时间窗、请求总数下限、错误百分比下限。这个参数的作用分别是：

    快照时间窗：断路器确定是否打开需要统计一些请求和错误数据，而统计的时间范围就是快照时间窗，默认为最近的10秒。
    
    请求总数下限：在快照时间窗内，必须满足请求总数下限才有资格根据熔断。默认为20，意味着在10秒内，如果该hystrix命令的调用此时不足20次，即时所有的请求都超时或其他原因失败，断路器都不会打开。
    
    错误百分比下限：当请求总数在快照时间窗内超过了下限，比如发生了30次调用，如果在这30次调用中，有16次发生了超时异常，也就是超过50%的错误百分比，在默认设定50%下限情况下，这时候就会将断路器打开。



# 5.服务降级与熔断的区别

下面通过一个日常的故事来说明一下什么是服务降级，什么是熔断。

故事的背景是这样的：由于小强在工作中碰到一些问题，于是想请教一下业界大牛小壮。于是发生了下面的两个场景：

小强在拿起常用手机拨号时发现该手机没有能够拨通，所以就拿出了备用手机拨通了某A的电话，这个过程就叫做降级（主逻辑失败采用备用逻辑的过程）。

由于每次小壮的解释都属于长篇大论，不太容易理解，所以小强每次找小壮沟通的时候都希望通过常用手机来完成，因为该手机有录音功能，这样自己可以慢慢消化。由于上一次的沟通是用备用电话完成的，小强又碰到了一些问题，于是他又尝试用常用电话拨打，这一次又没有能够拨通，所以他不得不又拿出备用手机给某A拨号，就这样连续的经过了几次在拨号设备选择上的“降级”，小强觉得短期内常用手机可能因为运营商问题无法正常拨通了，所以，再之后一段时间的交流中，小强就不再尝试用常用手机进行拨号，而是直接用备用手机进行拨号，这样的策略就是熔断（常用手机因短期内多次失败，而被暂时性的忽略，不再尝试使用）。

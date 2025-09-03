# 1.Ribbon负载均衡简介

## 1.1Ribbon概述

### 1.1.1Ribbon是什么

SpringCloud Ribbon是基于Netflix Ribbon实现的一套**客户端负载均衡**的工具。

简单的说，Ribbon是Netflix发布的开源项目，主要功能是提供客户端的软件负载均衡算法，将Netflix的中间层服务连接在一起。Ribbon客户端组件提供一系列完善的配置项如**连接超时**，**重试**等。简单的说，就是在配置文件中列出**LoadBalanCer**（简称LB）后面所有的机器，Ribbon会自动的帮助你基于某种规则（如简单轮询，随机连接等）去连接这些机器。我们也很容易使用Ribbon实现自定义的负载均衡算法。

### 1.1.2 Ribbon主要职责

- LB（负载均衡）
LB，即负载均衡（ Load Balanoe )，在微服务或分布式集群中经常用的一种应用。
负载均衡简单的说就是将用户的请求平摊的分配到多个服务上，从而达到系统的HA。
常见的负载均衡有软件nginx , LVS,硬件F5等。
相应的在中间件，例如：dubbo和 SpringCloud中均给我们提供了负载均衡，SpringCloud的负载均衡算法可以自定义。
LB又分为两种，集中式LB和进程内LB

- 集中式LB（偏硬件）
即在服务的消费方和提供方之间使用独立的LB设施（可以是硬件，如F5，也可以是软件，如nginx ) ，由该设施负责把访问请求通过某种策略转发至服务的提供方；

- 进程内LB（偏软件）
将LB逻辑集成到消费方，消费方从服务注册中心获知有哪些地址可用，然后自己再从这些地址中选择出一个合适的服务器。
Ribbon就属于进程内LB，它只是一个类库，集成于消费方进程，消费方通过它来获取到服各提供方的地址。
### 1.1.3 官方资料

https://github.com/Netflix/ribbon/wiki
# 2.Ribbon实例

上一篇的案例中，我们启动了一个springcloud-demo，然后通过DiscoveryClient来获取服务实例信息，然后获取ip和端口来访问。

但是实际环境中，我们往往会开启很多个user-service的集群。此时我们获取的服务列表中就会有多个，到底该访问哪一个呢？

一般这种情况下我们就需要编写负载均衡算法，在多个实例列表中进行选择。
不过Eureka中已经帮我们集成了负载均衡组件：Ribbon，简单修改代码即可使用。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190503114156146.png)
接下来，我们就来使用Ribbon实现负载均衡。

## 2.1.Ribbon架构说明

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504112738766.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
Ribbon 在工作时分成两步:

```
第一步先选择 EurekaServer，它优先选择在同一个区域内负载较少的server。
第二步再根据用户指定的策略，在从server取到的服务注册列表中选择一个地址。
```

其中Ribbon 提供了多种策略：比如轮询、随机和根据响应时间加权。

## 2.2.启动两个服务实例

首先我们启动两个springcloud-demo实例，一个80，一个81。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190503115001988.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
Eureka监控面板：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019050313093932.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

## 2.3.开启负载均衡

- **因为Eureka中已经集成了Ribbon，所以我们无需引入新的依赖**

- **加入Ribbon的配置**
```yml
# EurekaServer地址
eureka.client.service-url.defaultZone=http://eureka7001.com:7001/eureka/,http://eureka7002.com:7002/eureka/,http://eureka7003.com:7003/eureka/
```

- **在RestTemplate的配置方法上添加`@LoadBalanced`注解**：

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate(new OkHttp3ClientHttpRequestFactory());
}
```
- **修改调用方式，不再手动获取ip和端口，而是直接通过服务名称调用**：

```java
/**
 * @author bruceliu
 * @create 2019-05-02 15:52
 * @description
 */
@RestController
@RequestMapping("consumer")
public class ConsumerController {

    //改成下面这行
    private static final String REST_URL_PREFIX = "http://SPRINGCLOUD-DEMO-SERVICE";

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/test")
    public List<User> consumerTest(){

        return this.restTemplate.getForObject(REST_URL_PREFIX+"/all",List.class);
    }
}
```
- **测试**：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190503143932572.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190503144029132.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
Ribbon整合后可以直接调用微服务而不再关心地址和端口!!!
## 2.4.源码跟踪

为什么我们只输入了service名称就可以访问了呢？之前还要获取ip和端口。显然有人帮我们根据service名称，获取到了服务实例的ip和端口。它就是`LoadBalancerInterceptor`
我们进行源码跟踪：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504111402262.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
继续跟入execute方法：发现获取了8001端口的服务
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504112358399.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
如果再跟下一次，发现获取的是8002

## 2.5.负载均衡策略

Ribbon默认的负载均衡策略是简单的轮询，我们可以测试一下：
编写测试类，在刚才的源码中我们看到拦截中是使用`RibbonLoadBalanceClient`来进行负载均衡的，其中有一个`choose`方法，是这样介绍的：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504113239509.png)
现在这个就是负载均衡获取实例的方法。我们对注入这个类的对象，然后对其测试：

```java
package com.bruceliu.test;

import com.bruceliu.SpringcloudDemoConsumerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author bruceliu
 * @create 2019-05-04 11:33
 * @description 负载均衡算法测试
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringcloudDemoConsumerApplication.class)
public class LoadBalanceTest {

    @Autowired
    RibbonLoadBalancerClient client;

    @Test
    public void test1(){
        for (int i = 0; i <10 ; i++) {
            ServiceInstance instance = this.client.choose("SPRINGCLOUD-DEMO-SERVICE");
            System.out.println(instance.getHost() + ":" + instance.getPort());
        }
    }
}
```
运行结果：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504114940325.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
符合了我们的预期推测，确实是轮询方式。
我们是否可以修改负载均衡的策略呢？
继续跟踪源码，发现这么一段代码：
![在这里插入图片描述](https://img-blog.csdnimg.cn/2019050411524162.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
我们看看这个rule是谁：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504115334807.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
这里的rule默认值是一个`RoundRobinRule`，看类的介绍：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504115434960.png)
这不就是轮询的意思嘛。
我们注意到，这个类其实是实现了接口IRule的，查看一下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504115536455.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
定义负载均衡的规则接口。
它有以下实现：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504115625863.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
SpringBoot也帮我们提供了修改负载均衡规则的配置入口：
```yaml
SPRINGCLOUD-DEMO-SERVICE:
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule
```
格式是：`{服务名称}.ribbon.NFLoadBalancerRuleClassName`，值就是IRule的实现类。
再次测试，发现结果变成了随机：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504120038222.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)

## 2.6.重试机制

Eureka的服务治理强调了CAP原则中的AP，即可用性和可靠性。它与Zookeeper这一类强调CP（一致性，可靠性）的服务治理框架最大的区别在于：Eureka为了实现更高的服务可用性，牺牲了一定的一致性，极端情况下它宁愿接收故障实例也不愿丢掉健康实例，正如我们上面所说的自我保护机制。

但是，此时如果我们调用了这些不正常的服务，调用就会失败，从而导致其它服务不能正常工作！这显然不是我们愿意看到的。

我们现在关闭一个springcloud-demo-8001实例![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504121712701.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
因为服务剔除的延迟，consumer并不会立即得到最新的服务列表，此时再次访问你会得到错误提示：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504121654584.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
但是此时，8002服务其实是正常的。
因此Spring Cloud 整合了Spring Retry 来增强RestTemplate的重试能力，当一次服务调用失败后，不会立即抛出一次，而是再次重试另一个服务。
只需要简单配置即可实现Ribbon的重试：

```yaml
spring:
  cloud:
    loadbalancer:
      retry:
        enabled: true # 开启Spring Cloud的重试功能
SPRINGCLOUD-DEMO-SERVICE:
  ribbon:
    ConnectTimeout: 250 # Ribbon的连接超时时间
    ReadTimeout: 1000 # Ribbon的数据读取超时时间
    OkToRetryOnAllOperations: true # 是否对所有操作都进行重试
    MaxAutoRetriesNextServer: 1 # 切换实例的重试次数
    MaxAutoRetries: 1 # 对当前实例的重试次数
```
根据如上配置，当访问到某个服务超时后，它会再次尝试访问下一个服务实例，如果不行就再换一个实例，如果不行，则返回失败。切换次数取决于`MaxAutoRetriesNextServer`参数的值

引入spring-retry依赖
```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```
我们重启springcloud-demo-consumer，测试，发现即使springcloud-demo-8001宕机，也能通过另一台服务实例获取到结果!
## 2.7 Ribbon核心组件IRule(面试题)

IRule：根据特定算法中从服务列表中选取一个要访问的服务。
- RoundRobinRule：轮询
- RandomRule：随机
- AvailabilityFilteringRule：会先过滤掉由于多次访问故障而处于断路器跳闸状态的服务，还有并发的连接数量超过阈值的服务，然后对剩余的服务列表按照轮询策略进行访问
- WeightedResponseTimeRule：根据平均响应时间计算所有服务的权重，响应时间越快服务权重越大被选中的概率越高。刚启动时如果统计信息不足，则使用RoundRobinRule策略，等统计信息足够，会切换到WeightedResponseTimeRule
- RetryRule：先按照RoundRobinRule的策略获取服务，如果获取服务失败则在指定时间内会进行重试，获取可用的服务
- BestAvailableRule：会先过滤掉由于多次访问故障而处于断路器跳闸状态的服务，然后选择一个并发量最小的服务
- ZoneAvoidanceRule：默认规则,复合判断server所在区域的性能和server的可用性选择服务器

Ribbon默认自带了七种算法，默认是轮询。
我们也可以自定义自己的算法。
## 2.8 修改访问服务的算法方式

切换访问的算法 很简单只需要换成我们要返回算法的实例即可
默认有七个算法:

- **新建自定义Robbin规则类**
```java
/**
 * @author bruceliu
 * @create 2019-05-04 13:52
 * @description
 */

import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RetryRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 该配置类不可以放在与注解 @ComponentScan 的同包或者子包下，否则不起作用 （自定义算法也是一样）
 */
@Configuration
public class MySelfRule {

    /*
     * 切换 访问的算法 很简单只需要换成我们要返回算法的实例即可
     * 默认有七个算法，可以自定义自己的算法
     * */
    @Bean
    public IRule myRule()
    {
        //如果 突然间一个服务挂了 访问带挂的服务器会报错，出现错误页面
        //return new RoundRobinRule();
        //return new RandomRule(); //达到的目的，用我们重新选择的随机算法替代默认的轮询。
        //如果 突然间一个服务挂了 访问带挂的服务器会报错，出现错误页面，但是过一下子他不会再访问挂的机器，不会显示出错误的页面,这也是重试的另外一种配置方式！
        return new RetryRule();
    }
}

```
- **修改主启动类**
```java
@SpringBootApplication
@EnableDiscoveryClient // 开启EurekaClient功能
@RibbonClient(name="SPRINGCLOUD-DEMO-SERVICE",configuration=MySelfRule.class)
public class SpringcloudDemoConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringcloudDemoConsumerApplication.class, args);
	}
}
```
- 细节
官方文档明确给出了警告：
这个自定义配置类不能放在@ComponentScan所扫描的当前包下以及子包下，否则我们自定义的这个配置类就会被所有的Ribbon客户端所共享，也就是说我们达不到特殊化定制的目的了。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504135600610.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
## 2.9. 自定义IRule算法(了解)

1、在主程序添加@RibbonClient(name=“SPRINGCLOUD-DEMO-SERVICE”,configuration=MySelfRule1.class)
（注意：**自定义算法不可以放在与注解 @ComponentScan 的同包或者子包下，否则不起作用** ）

```java
/**
 * @author bruceliu
 * @create 2019-05-04 13:59
 * @description
 */
@Configuration
public class MySelfRule1 {

    @Bean
    public IRule myRule() {
        // return new RandomRule();// Ribbon默认是轮询，我自定义为随机
        // return new RoundRobinRule();// Ribbon默认是轮询，我自定义为随机
        // return new RetryRule();
        return new MyRandomRule();// 我自定义为每台机器5次
    }
}
```
2、自定义算法必须继承抽象类 AbstractLoadBalancerRule
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190504140042936.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
```java
/**
 * @author bruceliu
 * @create 2019-05-04 14:01
 * @description
 */
/*
 * 参考随机数的源码来修改
 * https://github.com/Netflix/ribbon/blob/master/ribbon-loadbalancer/src/main/java/com/netflix/loadbalancer/RandomRule.java
 * */
public class MyRandomRule extends AbstractLoadBalancerRule {

    // total = 0 // 当total==5以后，我们指针才能往下走，
    // index = 0 // 当前对外提供服务的服务器地址，
    // total需要重新置为零，但是已经达到过一个5次，我们的index = 1

    private int total = 0; // 总共被调用的次数，目前要求每台被调用5次
    private int currentIndex = 0; // 当前提供服务的机器号

    public Server choose(ILoadBalancer lb, Object key) {
        if (lb == null) {
            return null;
        }
        Server server = null;

        while (server == null) {
            if (Thread.interrupted()) {
                return null;
            }
            // 活着的可以对外提供服务的机器
            List<Server> upList = lb.getReachableServers();
            // 所有的服务
            List<Server> allList = lb.getAllServers();

            // 服务的总数
            int serverCount = allList.size();
            if (serverCount == 0) {
                /*
                 * No servers. End regardless of pass, because subsequent passes
                 * only get more restrictive.
                 */
                return null;
            }

            // int index = rand.nextInt(serverCount);//
            // java.util.Random().nextInt(3);
            // server = upList.get(index);

            if (total < 5) {
                // 获取服务的第几个
                server = upList.get(currentIndex);
                total++;
            } else {
                total = 0;
                currentIndex++;
                if (currentIndex >= upList.size()) {
                    currentIndex = 0;
                }
            }

            if (server == null) {
                /*
                 * The only time this should happen is if the server list were
                 * somehow trimmed. This is a transient condition. Retry after
                 * yielding.
                 */
                Thread.yield();
                continue;
            }

            if (server.isAlive()) {
                return (server);
            }

            // Shouldn't actually happen.. but must be transient or a bug.
            server = null;
            Thread.yield();
        }

        return server;

    }

    @Override
    public Server choose(Object key) {
        return choose(getLoadBalancer(), key);
    }

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
        // TODO Auto-generated method stub

    }
}
```

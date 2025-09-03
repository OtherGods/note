💡 观前提要!!!

|Dependency|Version|
|---|---|
|spring-boot-starter-web|2.7.6|
|spring-cloud-starter-openfeign|3.1.5|
|spring-cloud-loadbalancer|3.1.5|

# **介绍**

## **什么是 `Load Balance` ?**

`LoadBalancer` "负载均衡", 是指将多个服务器、系统或者资源之间合理分配网络流量或者工作负载的过程。在微服务中，就对应着 **客户端** 通过负载均衡调用相同业务下的多个实例。 可以有效避免单个服务不可用、单节点负载过大等问题。有效提升应用承载能力，提升业务响应能力的重要组件。同时也是提升 “横向扩展” 的重要组件！

### 传统请求

假设有我们有一个客户端和一个服务端, 想让客户端访问到服务端直接填写对应的 `IP` 地址或者域名来实现对服务端的访问. 这种 1:1 的访问十分简单, 但是存在了单点故障等一些问题.
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506091632412.png)

- 单点问题
    如果 `Server` 端因为业务逻辑或者等一些异常情况导致无法提供服务, 这时 `Client` 就无法访问 `Server` . 导致整个业务无法继续.
- 网络问题
    在我们部署服务时, 如果机器没有使用静态IP 或者指定 `IP` 的情况下 (如容器环境). `IP` 变更同样会无法访问服务, 但是服务其实依旧存活.
- 横向扩容问题
    每个服务器的算力和 `IO` 都是有限资源, 在业务规模越来越大的情况下, 单机服务可能会因为缺少计算机资源 (`IO` `CPU` `Memory` `DISK` ) 导致业务处理速率降低, 进一步导致请求堆积. 很有可能会将服务进入 “假死” 状态.
    
    而且如果我们想要另外增加一台服务器来分摊负载压力, `Client` 端需要记录多个服务器地址, 通过调度算法 (轮询, 权重) 来从列表中获取可用服务器地址进行请求. 在新增服务器的情况下, 已存在的 `Client` 服务实例配置文件中并没有对应 `IP` , 就无法将请求分摊到新服务器中. 同时衍生出 “网络问题”.
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506091634005.png)

在传统请求中, 我们需要直到对应的 `IP` 才能请求. 这种请求方式限定了请求的范围, 影响到后期扩容的问题. 为了解决以上问题, `Load-Balance` 就出现了

### `Load-Balance`

`Load-Balance` 代理请求, `Client` 端不需要知道有哪些服务器, 只需要请求即可; 具体要请求哪台服务器, 由 `Load-Balance` 组件来实现.

负载均衡的实现可以对应着网络 (ISO) 的不同层级
- 传输层
    F5 负载均衡硬件, `NAT` , `LVS` 等
- 应用层
    应用层就是由程序进行负载均衡控制.

这也就是常说的 硬件负载均衡 和 软件负载均衡, 由于其分布在不同的网络层级下, 实现不同但效果相同 (米💰也不同 ).
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506091637680.png)

这样, 请求端就不需要知道有哪些服务器可用, 按照常规请求即可. 后续操作将由 `LB` 进行控制.

[What Is Load Balancing? How Load Balancers Work](https://link.juejin.cn?target=https%3A%2F%2Fwww.nginx.com%2Fresources%2Fglossary%2Fload-balancing%2F "https://www.nginx.com/resources/glossary/load-balancing/")

### **客户端 `LB` 与 服务端 `LB` 的区别**

- 客户端 `LB`
    客户端 `LB` 就是我们本文重点讨论的. 如果远程服务有多个的情况, 客户端的 `LB` 就通过其自己内置的组件 (`ribbon` `loadbalancer`) 实现的逻辑从中获取一个符合条件的远程服务进行调用, 并获取结果.
    ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506091638552.png)
- 服务端 `LB`
    服务端 `LB` 通常被称之为 `网关 (Gateway)` , 其性质与 `Nginx` 的反向代理十分相似. 在有多个可用的业务实例下, 如果外部请求需要请求该业务时, 我们无法将所有服务实例暴露给对方, 这时我们就可以创建一个网关, 统一接收用户请求, 然后分发到任意可用的业务节点中.

# **基础使用**

在深入之前我们需要有实例来支撑我们后续调试等操作. 所以我们先创建以下十分简易的提供者和消费者.

可以通过 [gitee.com/bystander_j…](https://link.juejin.cn?target=https%3A%2F%2Fgitee.com%2Fbystander_jt%2Fspring-cloud-loadbalance.git "https://gitee.com/bystander_jt/spring-cloud-loadbalance.git") 链接进行项目拉取.

💡 对于配置项, 可以简略为
```yml
server:
  port: ${random.int(8000,10000)}
spring:
  application:
		# (如果是消费者, 则改成消费者名称)
    name: spring-cloud-feign-provider-demo
	cloud:
		server-addr: <nacos 服务器地址>:8848
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506091654940.png)
调用成功！

## 讲解

如果要应用开启 `LoadBalance` , 需要声明一个 `@LoadBalancerClient` . 具体参数解析如下
```java
public @interface LoadBalancerClient {

	/**
	 * 请求的服务名称 (也就是请求的 host)
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 请求的服务名称 (也就是请求的 host)
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 何种负载均衡配置
	 */
	Class<?>[] configuration() default {};

}
```

❗在你申明的 `configuration` 的类在应用程序加载时不能被实例化, 这也是 [Spring 官方](https://link.juejin.cn?target=https%3A%2F%2Fdocs.spring.io%2Fspring-cloud-commons%2Fdocs%2F3.1.6%2Freference%2Fhtml%2F%23spring-cloud-loadbalancer "https://docs.spring.io/spring-cloud-commons/docs/3.1.6/reference/html/#spring-cloud-loadbalancer")中的说明. 其原因是在我们请求时调用负载均衡器时, 会对每一个 `serviceId (对应 @LoadBalancerClient 的 name)` 创建一个子上下文, 将对应的负载均衡器在子上下文中进行创建. 其目的是保证每个服务拥有自己的独享的负载均衡配置和实例, 不仅可以保证子容器之间的数据隔离, 也能实现配置隔离. 在不同的 `serviceId (对应 @LoadBalancerClient 的 name)` 下只管理自己的服务实例, 明确了自己的职责.

## 如何实现负载均衡

1. 声明 `@LoadBalancerClient`
    在我们请求时, 不在请求对应的服务器地址, 而是请求不同的服务名称 (也就是在 `@LoadBalanceClient` 中定义的 `name` `value`). 这个可以从我们在 `IDefaultFeignService` 类中看到.
    
    在应用初始化时, `load-balance` 会生成一个 `LoadBalancerInterceptor` 拦截器, 同时注入到我们所使用的 `RestTemplate` `WebFlux` 等客户端中.
    
    然后通过拦截, 获取对应的服务名称. 从可用的 `DiscoveryClient` 中获取对应名称可用的实例. 然后在根据所设定的算法从中选出一个符合条件的进行请求调用. 并返回对应结果.
    
2. 创建自己的负载均衡配置
    `spring-cloud-load-babalce` 默认提供了几种可用实现
    - `RoundRobinLoadBalancer`
    - `RandomLoadBalancer`
    类不要被 `@Component` 或者 `@Configuration` 修饰, 该负载均衡器是在子上下文环境下才会生成!
```java
public class RandomLoadBalancerConfiguration {

    @Bean(name = "random")
    ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(Environment environment,
                                                            LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                name
        );
    }

}
```    


### 如何创建自己的负载均衡器 ?

我们首先观察下 `RoundRobinLoadBalancer` 是如何实现的.
```java
public class RoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {

		// 算法参数
		final AtomicInteger position;

		// @LoadBalanceClient 的名称.
		final String serviceId;

		// 服务提供者
		ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

		public RoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
																	String serviceId, int seedPosition) {
			this.serviceId = serviceId;
			this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
			this.position = new AtomicInteger(seedPosition);
		}

		public Mono<Response<ServiceInstance>> choose(Request request) {
			ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
					.getIfAvailable(NoopServiceInstanceListSupplier::new);
			return supplier.get(request).next()
										.map(serviceInstances -> processInstanceResponse(supplier, serviceInstances));
		}
	
		private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier,
																															List<ServiceInstance> serviceInstances) {
			Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances);
			if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {
				((SelectedInstanceCallback) supplier).selectedServiceInstance(serviceInstanceResponse.getServer());
			}
			return serviceInstanceResponse;
		}
	
		private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances) {
			if (instances.isEmpty()) {
				return new EmptyResponse();
			}
			if (instances.size() == 1) {
				return new DefaultResponse(instances.get(0));
			}
	
			int pos = this.position.incrementAndGet() & Integer.MAX_VALUE;
			ServiceInstance instance = instances.get(pos % instances.size());
	
			return new DefaultResponse(instance);
		}
}
```

其实现了 `ReactorServiceInstanceLoadBalancer` 接口, 其内部包含了:
- `ObjectProvider<ServiceInstanceListSupplier>` 用于获取服务实例. `ObjectProvider` 和 `Optional` 类似, 区别是 `Provider` 是尝试从容器中获取对象.
- `serviceId` 用于定义该负载均衡配置的应用范围, 其值由 `@LoadBalanceClient` 控制.
- `position` `RoundRobin` 算法特定参数, 通过累加获取下一个实例.

目前我们知道, 如果我们想要实现自己的负载均衡器则需要实现 `ReactorServiceInstanceLoadBalancer` 这个接口, 然后实现内部方法, 根据我们自生的负载均衡算法实现内部逻辑即可自定义我们自己的负载均衡器.

创建一个最短响应时间的负载均衡器:
```java
public class ShortestPingLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private static final ServiceInstance NOOP = new DefaultServiceInstance();
    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceProvider;

    public ShortestPingLoadBalancer(ConfigurableApplicationContext context) {
        this.serviceId = context.getEnvironment().getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        this.serviceInstanceProvider = context.getBeanProvider(ServiceInstanceListSupplier.class);
    }

    public ShortestPingLoadBalancer(String serviceId, ObjectProvider<ServiceInstanceListSupplier> serviceInstanceProvider) {
        this.serviceId = serviceId;
        this.serviceInstanceProvider = serviceInstanceProvider;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        // 获取服务提供者
        ServiceInstanceListSupplier supplier = serviceInstanceProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
				// supplier.get(request) 用于获取可用的服务实例
				// .next() 则是将首个 Flux 包装的进行返回, 并包装成 Mono
				// map 则是对 Mono 进行处理, 和我们常用的 stream().map() 十分相似.
        return supplier.get(request).next().map(instance -> processAlgorithm(supplier, instance));
    }

		// 下面就是算法本身了 (十分简陋)
    private Response<ServiceInstance> processAlgorithm(ServiceInstanceListSupplier supplier, List<ServiceInstance> instanceList) {
        ServiceInstance instance = NOOP;
        int minimumResponseTime = Integer.MAX_VALUE;
        for (ServiceInstance serviceInstance : instanceList) {
            int ping = getPing(serviceInstance.getHost(), serviceInstance.getPort());
            if (ping > 0 && ping < minimumResponseTime) {
                minimumResponseTime = ping;
                instance = serviceInstance;
            }
        }
        if (instance == NOOP) {
            return new EmptyResponse();
        }
        if (supplier instanceof SelectedInstanceCallback) {
            ((SelectedInstanceCallback) supplier).selectedServiceInstance(instance);
        }
        return new DefaultResponse(instance);
    }

    private int getPing(String host, int port) throws RuntimeException {
        StopWatch stopWatch = new StopWatch("ping-test");
        try {
            InetAddress address = null;
            address = InetAddress.getByName(host);
            stopWatch.start();
            address.isReachable(500);
        } catch (IOException e) {
            return -1;

        } finally {
            if (stopWatch.isRunning()) stopWatch.stop();
        }
        return (int) stopWatch.getTotalTimeNanos();
    }
}
```

# 内部详解

## `LoadBalancerClient(s)` 注解

我们只要在类上声明一个 `@LoadBalancerClient` , 然后设定下 `name` 和 负载均衡配置 就可以实现应用程序请求的负载均衡. 很神奇, 但是我们并不知道是如何实现的, 所以我们深入了解下 `@LoadBalancerClient(s)` 这个注解

### `@LoadBalancerClient`
```java
@Configuration(proxyBeanMethods = false)
@Import(LoadBalancerClientConfigurationRegistrar.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoadBalancerClient {

	/**
	 * 服务名称
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 服务名称
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 负载均衡配置
	 */
	Class<?>[] configuration() default {};

}
```

首先这个注解的内部字段写的十分清楚, 配置项只有 2 个 ( `name` 和 `value` 通过 `@AliasFor` 关联, 设定一个等同设定另外一个)

- `name` 用于设定应用到的服务名称
- `configuration` 用于确定该服务名称的子上下文容器所需要加载的实例 (负载均衡器, 环境参数配置, 外部参数配置等都可以注入).

在我们声明一个 `LoadBalancerClient` 时就意味着会在之后创建一个子上下文容器, 容器内会将我们 `configuration` 中的所有类进行加载并实例化.

与寻常的注解类不同, 该类引入了 `@Configuration` , 意味着在应用程序初始化时会自动将 `@Import` 的类进行加载到应用程序上下文环境中.

而 `@Import` 引入了 `LoadBalancerClientConfigurationRegistrar` . 从名字就可以看出来， 这是用于将我们声明的 `@LoadBalancerClient` 注册到某个地方。

### `LoadBalancerClientConfigurationRegistrar`

```java
public class LoadBalancerClientConfigurationRegistrar implements ImportBeanDefinitionRegistrar {

	// 从参数 Map 中获取可用的 serviceId
	private static String getClientName(Map<String, Object> client) {
		if (client == null) {
			return null;
		}
		String value = (String) client.get("value");
		if (!StringUtils.hasText(value)) {
			value = (String) client.get("name");
		}
		if (StringUtils.hasText(value)) {
			return value;
		}
		throw new IllegalStateException("Either 'name' or 'value' must be provided in @LoadBalancerClient");
	}

	private static void registerClientConfiguration(BeanDefinitionRegistry registry, Object name,
			Object configuration) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(LoadBalancerClientSpecification.class);
		builder.addConstructorArgValue(name);
		builder.addConstructorArgValue(configuration);
		registry.registerBeanDefinition(name + ".LoadBalancerClientSpecification", builder.getBeanDefinition());
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		Map<String, Object> attrs = metadata.getAnnotationAttributes(LoadBalancerClients.class.getName(), true);
		if (attrs != null && attrs.containsKey("value")) {
			AnnotationAttributes[] clients = (AnnotationAttributes[]) attrs.get("value");
			for (AnnotationAttributes client : clients) {
				registerClientConfiguration(registry, getClientName(client), client.get("configuration"));
			}
		}
		if (attrs != null && attrs.containsKey("defaultConfiguration")) {
			String name;
			if (metadata.hasEnclosingClass()) {
				name = "default." + metadata.getEnclosingClassName();
			}
			else {
				name = "default." + metadata.getClassName();
			}
			registerClientConfiguration(registry, name, attrs.get("defaultConfiguration"));
		}
		Map<String, Object> client = metadata.getAnnotationAttributes(LoadBalancerClient.class.getName(), true);
		String name = getClientName(client);
		if (name != null) {
			registerClientConfiguration(registry, name, client.get("configuration"));
		}
	}

}
```

其实现了 `ImportBeanDefinitionRegistrar` 顶级注解, 而这个注解适用于在程序上下文初始化时, 将被 `@Configuration` 标识的类进行额外的 `BeanDefinition` 注册.

1. 从注解元数据 (`AnnotationMetadata`) 获取 `@LoadBalancerClients` 注解元信息.
2. 从 `@LoadBalancerClients` 获取多个 `@LoadBalancerClient` 的配置信息, 并将其封装成 `LoadBalancerClientSpecification` 的 `BeanDefinition` 注册到应用程序上下文中.
3. 将默认配置 (`defaultConfiguration`) 注册为默认 `LoadBalancerClientSpecification`
4. 将额外定义的 `@LoadBalancerClient` 进行加载.

这些 `LoadBalancerClientSpecification` 会保存在应用程序上下文中, 在 `LoadBalancerClientFactory` 创建时导入到其对象内部, 用于后续子上下文容器所需对象的创建.
```java
public LoadBalancerClientFactory loadBalancerClientFactory(LoadBalancerClientsProperties properties) {
	LoadBalancerClientFactory clientFactory = new LoadBalancerClientFactory(properties);
	// 注入当前所有可以获取的 LoadBalancerClientSpecification
	clientFactory.setConfigurations(this.configurations.getIfAvailable(Collections::emptyList));
	return clientFactory;
}
```

## 负载均衡内部实现

那我们就要讲一下 `LoadBalancerInterceptor` 这个类了, 这个类用于拦截我们基于 `RestTemplate` `WebFlux` 等组件的请求, 将请求交由我们定义的负载均衡器控制.
```java
public class LoadBalancerInterceptor implements ClientHttpRequestInterceptor {
	// 负载均衡器实现客户端 (BlockingLoadBalancerClient)
	private LoadBalancerClient loadBalancer;
	// 用于封装请求, 用于后续的重试等操作
	private LoadBalancerRequestFactory requestFactory;

	// 构造器省略

	// 这里就是将请求进行拦截.
	@Override
	public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
																			final ClientHttpRequestExecution execution) throws IOException {

		final URI originalUri = request.getURI();
		// 从 url 中获取 serviceName, 对应着 @LoadBalanceClient 中的 name or value.
		// 同样如果请求发现无法触发我们指定的负载均衡器, 说不定请求的 host 错了, 去 @FeignClient 查看
		String serviceName = originalUri.getHost();
		Assert.state(serviceName != null, "Request URI does not contain a valid hostname: " + originalUri);

		// 负载均衡客户端调用执行
		return this.loadBalancer.execute(serviceName, this.requestFactory.createRequest(request, body, execution));
	}

}
```

```java
@Override
public <T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException {
		String hint = getHint(serviceId);

		LoadBalancerRequestAdapter<T, TimedRequestContext> lbRequest 
												= new LoadBalancerRequestAdapter<>(request,	buildRequestContext(request, hint));

		// LoadBalancer 生命周期, 通过此接口, 我们可以监听每次调用时的实例状态, 重试次数等信息.
		// 同样, Lifecycle 都会注册到每个 serviceId 的子上下文容器中, 实现独立.
		Set<LoadBalancerLifecycle> supportedLifecycleProcessors = getSupportedLifecycleProcessors(serviceId);
		// 生命周期开始
		supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
		// 调用负载均衡组件, 并由负载均衡器获取一个可用实例
		ServiceInstance serviceInstance = choose(serviceId, lbRequest);

		// 如果没有符合的实例, 抛出错误. 并结束当前生命周期
		if (serviceInstance == null) {
			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onComplete(
					new CompletionContext<>(CompletionContext.Status.DISCARD, lbRequest, new EmptyResponse())));
			throw new IllegalStateException("No instances available for " + serviceId);
		}

		// 如果存在则继续, 生命周期将会在后续调用完成后结束 (onCompleted)
		return execute(serviceId, serviceInstance, lbRequest);
}
```

在之前我们一直提过 “子上下文容器” , 但是目前看不到创建上下文容器的地方. 那我们就继续深入以下

### `NamedContextFactory` 子应用上下文

在调用 `getSupportedLifecycleProcessors` 时, 通过传入的 `serviceId` 获取对应 `serviceId` 的子应用上下文容器.
```java
private Set<LoadBalancerLifecycle> getSupportedLifecycleProcessors(String serviceId) {
		return LoadBalancerLifecycleValidator.getSupportedLifecycleProcessors(
									// 这里哦
									loadBalancerClientFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
									// 下面不是了
									DefaultRequestContext.class, 
									Object.class, ServiceInstance.class);
}
```

`LoadBalancerClientFactory` 这个类包含了我们所定义的 `@LoadBalanceClient` 相关参数, 所有负载均衡配置信息, 不同 `serviceId` 的子上下文容器.

负载均衡配置信息包含全局配置和对应服务的负载均衡配置 (对应的负载均衡配置参数 (`LoadBalancerClientSpecification`) 存放到了 `NamedContextFactory` 中.
```java
public class LoadBalancerClientFactory extends NamedContextFactory<LoadBalancerClientSpecification>
                                       implements ReactiveLoadBalancer.Factory<ServiceInstance> {

	

}
```

其中的 `NamedContextFactory` 是 `Spring` 提供的子上下文容器, 我们通过调用其内部方法即可创建独立于当前上下文的子上下文容器, 创建的子上下文都是空的, 要使用就需要在子上下文中进行创建.

这也就说明了为什么我们不要在类上修饰 `@Configuration` 等注解在容器启动时注入到容器中. 所以这点必须注意!

💡 在我们通过 `@LoadBalancerClient(s)` 引用的 `configuration` 类时, 对应类 不能用 `@Configuration` 等注解修饰将其提前注入到容器中.
```java
public abstract class NamedContextFactory<C extends NamedContextFactory.Specification>
																			   		implements DisposableBean, ApplicationContextAware {

	// 存放对应服务名称的子上下文对象 MAP
	private Map<String, AnnotationConfigApplicationContext> contexts = new ConcurrentHashMap<>();
	// 存放对应服务名称的 LoadBalancerClientSpecification. 用于创建应用服务子上下文中的对象.
	private Map<String, C> configurations = new ConcurrentHashMap<>();
	// 当前应用程序上下文对象.
	private ApplicationContext parent;
	
	// 省略

	// 首次获取的是 LoadBalancerLifecycle
	public <T> Map<String, T> getInstances(String name, Class<T> type) {
		// 尝试从 name 获取对应的容器上下文
		AnnotationConfigApplicationContext context = getContext(name);

		return BeanFactoryUtils.beansOfTypeIncludingAncestors(context, type);
	}

	// 根据 name 从 ConcurrentHashMap 获取对应的上下文, 如果没有则 createContext(name)
	protected AnnotationConfigApplicationContext getContext(String name) {
			if (!this.contexts.containsKey(name)) {
				synchronized (this.contexts) {
					if (!this.contexts.containsKey(name)) {
						// 如果没有则创建一个上下文
						this.contexts.put(name, createContext(name));
					}
				}
			}
			return this.contexts.get(name);
		}
	
		protected AnnotationConfigApplicationContext createContext(String name) {
			AnnotationConfigApplicationContext context;
			if (this.parent != null) {
			
				// 创建一个 Bean 工厂, 并使用当前上下文 Bean 工厂的 classLoader
				DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
				if (parent instanceof ConfigurableApplicationContext) {
					beanFactory.setBeanClassLoader(
							((ConfigurableApplicationContext) parent).getBeanFactory().getBeanClassLoader());
				}	else {
					beanFactory.setBeanClassLoader(parent.getClassLoader());
				}

				// 创建一个基于注解的子应用上下文, 除了 classloader 相同, 其他都是独立的
				context = new AnnotationConfigApplicationContext(beanFactory);
				context.setClassLoader(this.parent.getClassLoader());
			}	else {
				context = new AnnotationConfigApplicationContext();
			}

			// 在使用 @LoadBalanceClient 时会将信息注册到 configuration 的 Map
			if (this.configurations.containsKey(name)) {
				for (Class<?> configuration : this.configurations.get(name).getConfiguration()) {
					// 子容器注册类信息
					context.register(configuration);
				}
			}

			// 注册默认的配置信息
			for (Map.Entry<String, C> entry : this.configurations.entrySet()) {
				if (entry.getKey().startsWith("default.")) {
					for (Class<?> configuration : entry.getValue().getConfiguration()) {
						context.register(configuration);
					}
				}
			}

			context.register(PropertyPlaceholderAutoConfiguration.class, this.defaultConfigType);
			// 应用上下文设定 loadbalancer.client.name,
			// 在后续调用时, 会根据该名称取寻找对应的可用服务列表.
			context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(this.propertySourceName,
					Collections.<String, Object>singletonMap(this.propertyName, name)));
			if (this.parent != null) {
				// Uses Environment from parent as well as beans
				context.setParent(this.parent);
			}
			context.setDisplayName(generateDisplayName(name));
			context.refresh();
			return context;
		}

}
```

### `choose(serviceId, lbRequest)` 获取一个可用的实例

在此时 `load-balancer` 就介入并开始对可用实例进行筛选
```java
@Override
public <T> ServiceInstance choose(String serviceId, Request<T> request) {
		
		// 获取负载均衡器
		ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerClientFactory.getInstance(serviceId);
		if (loadBalancer == null) {
			return null;
		}

		// 从负载均衡器中选择一个可用的 ServiceInstance 并返回.
		Response<ServiceInstance> loadBalancerResponse = Mono.from(loadBalancer.choose(request)).block();
		if (loadBalancerResponse == null) {
			return null;
		}
		return loadBalancerResponse.getServer();
}
```

这里你就知道一些信息, 无论你在类中定义多少负载均衡器, 只会有1个成功执行. 所以负载均衡器你可以高度定制化, 通过深度抽象能力来实现实时更改负载均衡策略的效果.

但是我们是不是从来没有看到所有实例的来源? 在这里 `loadbalancer.chooose(request)` 就结束了, 完全看不到如何获取实例的情况.

那我们返回到负载均衡器中再看一下

### `ServiceInstanceListSupplier` 服务实例提供者
```java
public class RandomLoadBalancer implements ReactorServiceInstanceLoadBalancer {

	private static final Log log = LogFactory.getLog(RandomLoadBalancer.class);

	private final String serviceId;

	private ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

	/**
	 * @param serviceInstanceListSupplierProvider a provider of
	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
	 * @param serviceId id of the service for which to choose an instance
	 */
	public RandomLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
			String serviceId) {
		this.serviceId = serviceId;
		this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Mono<Response<ServiceInstance>> choose(Request request) {
		ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
				.getIfAvailable(NoopServiceInstanceListSupplier::new);
		return supplier.get(request).next()
				.map(serviceInstances -> processInstanceResponse(supplier, serviceInstances));
	}

	// ...

}
```

我们从 `RandomLoadBalancer` 可以看到是通过 `ObjectProvider` 进行注入, 而 `ServiceInstanceListSupplier` 这个顶级接口却有不同的实现

- `DiscoveryClientServiceInstanceListSupplier`
- `HealthCheckServiceInstanceListSupplier`
- `HintBasedServiceInstanceListSupplier`
- `NoopServiceInstanceListSupplier`
- `RequestBasedStickySessionServiceInstanceListSupplier`
- `RetryAwareServiceInstanceListSupplier`
- `SameInstancePreferenceServiceInstanceListSupplier`
- `ZonePreferenceServiceInstanceListSupplier`

其中的 `DiscoveryClientServiceInstanceListSupplier` 则是通过发现服务去寻找可用的客户端, 而其他的则是对已有的服务进行过滤.

比如 `ZonePreferenceServiceInstanceListSupplier` 则会根据 `serviceId` 的配置信息 (或者全局) 去筛选可用实例中的元数据信息, 将元数据中 `ZONE` 相同的进行返回, 不同则过滤, 如果一个都没有则返回全部.

如果我们想 `ServiceInstanceListSupplier` 的多重判断, 我们可以通过 `ServiceInstanceListSupplier.*builder*()` 去构建 (使用 Bean 会导致循环引用的问题)
```java
@Bean
public ServiceInstanceListSupplier build(ConfigurableApplicationContext context) {
    return ServiceInstanceListSupplier.builder()
            .withZonePreference()
            .withBlockingDiscoveryClient()
            .withSameInstancePreference()
            .build(context);
}

// 循环引用问题, 你机要 ServiceInstanceListSupplier, 又提供. 导致循环
@Bean
public ServiceInstanceListSupplier zone(ServiceInstanceListSupplier supplier,
                                        LoadBalancerZoneConfig zoneConfig){
    return new ZonePreferenceServiceInstanceListSupplier(supplier, zoneConfig);
}
```

通过 `ServiceInstanceListSupplier.builder()` 会按照顺序进行包装, 在获取时会先从首个加入 `builder` 的进行获取 (定义 `DiscoveryClient` 除外). 我们看下如何实现的
```java
public final class ServiceInstanceListSupplierBuilder {

	// 基层, 都先从 DiscoveryClient 进行获取然后再筛选
	private Creator baseCreator;

	// 缓存. (如果 DiscoveryClient 已经有缓存就不要加, 避免重复缓存)
	private DelegateCreator cachingCreator;

	// 定义 DiscvoeryClient 额外的筛选器
	private final List<DelegateCreator> creators = new ArrayList<>();

	// 创建一个底层 (baseCreator) 服务发现者
	public ServiceInstanceListSupplierBuilder withBlockingDiscoveryClient() {
			if (baseCreator != null && LOG.isWarnEnabled()) {
				LOG.warn("Overriding a previously set baseCreator with a blocking DiscoveryClient baseCreator.");
			}
			this.baseCreator = context -> {
				DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);
	
				return new DiscoveryClientServiceInstanceListSupplier(discoveryClient, context.getEnvironment());
			};
			return this;
	}

	// 我们也能定义我们自己的基础发现器
	public ServiceInstanceListSupplierBuilder withBase(ServiceInstanceListSupplier supplier) {
		this.baseCreator = context -> supplier;
		return this;
	}

	// 偏向使用相同的实例. 将 Supplier 增加到 creator 列表中.
	// 为了实现偏向使用相同实例的特性, 需要对 loadbalance 生命周期进行监控, 看最后调用了哪个实例, 实现偏向性.
	public ServiceInstanceListSupplierBuilder withSameInstancePreference() {
		DelegateCreator creator = (context,
				delegate) -> new SameInstancePreferenceServiceInstanceListSupplier(delegate);
		this.creators.add(creator);
		return this;
	}

	// 实例健康检查
	public ServiceInstanceListSupplierBuilder withBlockingHealthChecks() {
		DelegateCreator creator = (context, delegate) -> {
			RestTemplate restTemplate = context.getBean(RestTemplate.class);
			LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
			return blockingHealthCheckServiceInstanceListSupplier(restTemplate, delegate, loadBalancerClientFactory);
		};
		this.creators.add(creator);
		return this;
	}

	// 区域偏向性
	public ServiceInstanceListSupplierBuilder withZonePreference() {
		DelegateCreator creator = (context, delegate) -> {
			LoadBalancerZoneConfig zoneConfig = context.getBean(LoadBalancerZoneConfig.class);
			return new ZonePreferenceServiceInstanceListSupplier(delegate, zoneConfig);
		};
		this.creators.add(creator);
		return this;
	}

	// 我们也可以自定以我们自己的实例筛选器
	public ServiceInstanceListSupplierBuilder with(DelegateCreator delegateCreator) {
		if (delegateCreator != null) {
			creators.add(delegateCreator);
		}
		return this;
	}

	// ...

	// 最后实例化 bean 注入容器中.
	public ServiceInstanceListSupplier build(ConfigurableApplicationContext context) {
		Assert.notNull(baseCreator, "A baseCreator must not be null");

		// 首先创建 DiscoveryClient 获取可用实例
		ServiceInstanceListSupplier supplier = baseCreator.apply(context);

		// 然后根据将其包装到每一个过滤器 (supplier 中).
		for (DelegateCreator creator : creators) {
			supplier = creator.apply(context, supplier);
		}

		if (this.cachingCreator != null) {
			supplier = this.cachingCreator.apply(context, supplier);
		}
		return supplier;
	}
```

通过这种方式, 将 `DiscoveryClient` 层层包裹, 形成了一个千层饼的结构然后进行返回. 这样我们在获取实例时, 会从这千层饼的最内层层层向外传递, 知道穿透整个千层饼, 就获得了我们可用的实例列表, 然后再通过负载均衡控制器从这返回的实例列表中选择一个实例进行调用.

# 总结

在本文章中, 我们讲解了什么是负载均衡器, 也说明了客户端和服务端的负载均衡器有何区别. 同时也创建了一个简单的负载均衡项目. 我们再回顾一下

## `LoadBalancerClient(s)` 定义负载均衡配置

我们可以通过增加 `@LoadBalancerClient` 或者复合型 `@LoadBalancerClients` 创建一个或多个负载均衡配置

`@LoadBalancerClients` 就是多个 `@LoadBalancerClient` 的集合版本, 我们可以在 `@LoadBalancerClients` 声明多个 `@LoadBalancerClient`

而且 `@LoadBalancerClients` 提供了默认配置 (`defaultConfiguration`) , 如果我们没有在 `@LoadBalancerClient` 中指定配置, 或者没有声明某个 `serviceId` 的负载均衡配置; 那负载均衡器就会使用该默认配置 (`defaultConfiguration`) 作为其负载均衡器.

对应的注解配置会生成 `LoadBalancerClientSpecification` 注入到 `LoadBalancerClientFactory` 对象中, 用于后续对应服务名称的子上下文容器对象创建.

## `LoadBalancerInterceptor`

其主要用于拦截我们常规的 `RestTemplate` `WebFlux` 等组件的请求, 将后续请求交由负载均衡器实现.

在负载均衡器在首次获取其对应的生命周期管理对象时, 就会触发对应的子上下文容器初始化 (在没有的情况下才会). 然后记录当前负载均衡请求的整个生命周期.

在负载均衡器尝试通过 `ServiceInstanceListSupplier` 获取可用的服务实例, 然后进行负载均衡调控. 最后进行请求.
![1.jpg](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506091827238.jpg)



## 重要组件

- `LoadBalancerClientFactory`
    用于管理所有子上下容器, 容器初始化, 获取等.
- `ServiceInstanceListSupplier`
    用于提供服务, 在 `LB` 中用于获取和过滤可用实例
- `DiscoveryClient`
    这个组件并不属于 `load-balancer` , 但是它是可用实例获取的源头!
- `LoadBalancerInterceptor`
    拦截器, 不多说了
- `NamedFactory`
    用于创建子上下文容器的抽象类
- `LoadBanalcerLifecycle`
    用于管理每次请求的 `load-balancer` 生命周期.
- `ReactorServiceInstanceLoadBalancer`
    用于创建负载均衡算法的接口.

## 引用

[Spring 官方文档](https://docs.spring.io/spring-cloud-commons/docs/3.1.6/reference/html/#spring-cloud-loadbalancer)

转载自：[https://juejin.cn/post/7247467756245876794](https://juejin.cn/post/7247467756245876794)


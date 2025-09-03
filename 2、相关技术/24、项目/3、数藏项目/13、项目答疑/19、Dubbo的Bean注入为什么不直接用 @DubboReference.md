我们都知道，**==`Dubbo` 中提供了 `@DubboReference` 注解，可以帮我们把一个 Dubbo 的 bean注入到其他的Bean中==**，非常的方便。

如：
```java
@Component  
public class OrderEventListener {  
      
    @DubboReference(version = "1.0.0")  
    private CollectionFacadeService collectionFacadeService;  
}
```

但是我们的项目中没用这种方式，而是单独定义了一个很多 `DubboConfiguration` 来定义 Dubbo 的 bean。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503161759942.png)

为什么这么做呢？这么做有啥好处么？

其实，主要是为了 **==解决直接使用`@DubboReference`注入的 bean 没有办法被 mock 的问题（会覆盖MockBean注入的Bean）==**。

当我们在做单元测试的时候，我们需要对一个我们自己的 `service` 进行单测，就会把他依赖的 bean 做 mock，但是 `@DubboReference` 这种 bean的 mock是不生效的。所以，需要使用 `@Autowired` 这种方式注入才行。

比如大家可以尝试修改一下`OrderEventListener`中的`CollectionFacadeService collectionFacadeService`的注入方式：

**==使用`@Autowired`进行属性注入时，在debug时会发现被注入的是Mock的对象==**
```java
@Component  
public class OrderEventListener {  
      
    @Autowired  
    private CollectionFacadeService collectionFacadeService;  
}
```

**==使用`@DubboReference(version = "1.0.0")`进行属性注入时，在debug时会发现被注入的是Dubbo的代理对象，而不是Mock的对象==**
```java
@Component  
public class OrderEventListener {  
      
    @DubboReference(version = "1.0.0")  
    private CollectionFacadeService collectionFacadeService;  
}
```

然后运行以下单测，就会发现只有`@Autowired`这种能被 mock。
```java
public class OrderEventListenerTest extends OrderBaseTest {     
	@Autowired
    OrderEventListener orderEventListener;  
      
    @MockBean  
    public CollectionFacadeService collectionFacadeService;     
    @Test
    public void testOnApplicationEvent() {  
        CollectionSaleResponse response = new CollectionSaleResponse();  
        response.setSuccess(true);  
        when(collectionFacadeService.trySale(any())).thenReturn(response);  
          
        orderEventListener.onApplicationEvent(new OrderCreateEvent(new OrderCreateRequest()));  
    }  
      
}
```

为了让我们的 bean 能被`@Autowired`进入到另一个 bean 中，我们定义了一个`BusinessDubboConfiguration`，在这里面把 dubbo 的 bean 都定义出来，主要方式如下：
```java
@Configuration  
public class BusinessDubboConfiguration {  
      
    @DubboReference(version = "1.0.0")  
    private CollectionFacadeService collectionFacadeService;  
      
    @Bean  
    @ConditionalOnMissingBean(name = "collectionFacadeService")  
    public CollectionFacadeService collectionFacadeService() {  
        return collectionFacadeService;  
    }  
}
```

这样，我们就可以在想要使用`CollectionFacadeService`的地方使用`@Autowired`了：
```java
@Component  
public class OrderEventListener {  
      
    @Autowired  
    private CollectionFacadeService collectionFacadeService;  
}
```

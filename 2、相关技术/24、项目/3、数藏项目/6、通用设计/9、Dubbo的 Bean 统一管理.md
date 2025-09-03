
我们在业务中，经常要通过 duboo 进行外调接口，在引入外部服务的时候，会使用：
```java
@DubboReference(timeout = 1000,version = "1.0.0")private CollectionFacadeService collectionFacadeService;
```

这样的形式把一个其他服务定义的`collectionFacadeService`引入进来，并且作为一个 bean 注入。

但是如果代码中多处需要这个 bean 的话，就需要每一个地方都要写一遍，尤其是`timeout = 1000,version = "1.0.0"`这部分，看上去就比较乱。那么我们可以把所有的 dubbo 的 bean 配置到一起，然后想要使用的时候，就像一个普通的 bean 一样注入即可。这样可以减少一些定制化的配置。

而且，用 `@DubboReference` 做 bean 的注入，还有一个关键的问题那就是没办法被 mock！

通过这样的方式进行 mock，是无法生效的：
```java
@MockBean
public CollectionFacadeService collectionFacadeService;
```

所以，我们代码中，改用以下方式配置：
```java
package cn.hollis.nft.turbo.pay.infrastructure;  
  
import cn.hollis.nft.turbo.api.collection.service.CollectionFacadeService;  
import org.apache.dubbo.config.annotation.DubboReference;  
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
  
/**  
 * @author Hollis */@Configuration  
public class PayDubboConfiguration {  
      
    @DubboReference(timeout = 1000,version = "1.0.0")  
    private CollectionFacadeService collectionFacadeService;  
      
    @Bean  
    @ConditionalOnMissingBean(name = "collectionFacadeService")  
    public CollectionFacadeService collectionFacadeService() {  
        return collectionFacadeService;  
    }  
}
```

这样我在代码中，只需要通过 `@Autowired` 就可以直接注入了。
```java
public class CollectionController {  
      
    @Autowired  
    private CollectionFacadeService collectionFacadeService;  
}
```

这里需要注意，我在代码中，最开始如此配置的时候，每次启动的时候都报错：
```java
Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.2024-04-28 20:13:42,620 ERROR [main org.springframework.boot.SpringApplication - Application run failedorg.springframework.context.annotation.ConflictingBeanDefinitionException: Annotation-specified bean name 'collectionFacadeService' for bean class [cn.hollis.nft.turbo.api.collection.service.CollectionFacadeService] conflicts with existing, non-compatible bean definition of same name and class [null] at org.springframework.context.annotation.ClassPathBeanDefinitionScanner.checkCandidate(ClassPathBeanDefinitionScanner.java:361)
```

[10、ConflictingBeanDefinitionException](2、相关技术/24、项目/3、数藏项目/14、问题排查/10、ConflictingBeanDefinitionException.md)


SpringBoot 的 starter 可以帮我们简化配置，非常的方便，定义起来其实也不复杂，我们的项目中定义了很多 starter，比如nft-turbo-job就是一个 stater，以他为例，介绍下如何定义 starter：

1、添加依赖

添加Spring Boot的依赖：
```xml
<dependencies>
	<dependency> 
		<groupId>org.springframework.boot</groupId> 
		<artifactId>spring-boot-starter</artifactId> 
	</dependency>
</dependencies>
```

2、实现自动配置

在starter项目中，创建自动配置类。这个类要使用`@Configuration`注解，并根据条件使用`@ConditionalOn...`注解来条件化地配置beans。
```java
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;  
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;  
import org.springframework.boot.context.properties.EnableConfigurationProperties;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
  
/**  
 * @author Hollis */@Configuration  
@EnableConfigurationProperties(XxlJobProperties.class)  
public class XxlJobConfiguration {  
      
    private static final Logger logger = LoggerFactory.getLogger(XxlJobConfiguration.class);  
      
    @Autowired  
    private XxlJobProperties properties;  
      
    @Bean  
    @ConditionalOnMissingBean    @ConditionalOnProperty(prefix = XxlJobProperties.PREFIX, value = "enabled", havingValue = "true")  
    public XxlJobSpringExecutor xxlJobExecutor() {  
        logger.info(">>>>>>>>>>> xxl-job config init.");  
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();  
        xxlJobSpringExecutor.setAdminAddresses(properties.getAdminAddresses());  
        xxlJobSpringExecutor.setAppname(properties.getAppName());  
        xxlJobSpringExecutor.setIp(properties.getIp());  
        xxlJobSpringExecutor.setPort(properties.getPort());  
        xxlJobSpringExecutor.setAccessToken(properties.getAccessToken());  
        xxlJobSpringExecutor.setLogPath(properties.getLogPath());  
        xxlJobSpringExecutor.setLogRetentionDays(properties.getLogRetentionDays());  
        return xxlJobSpringExecutor;  
    }  
}
```

这里面用@Bean 注解声明了一个bean，并且使用`@ConditionalOnMissingBean`类指定这个bean的创建条件，即在缺失的时候创建。

`@ConditionalOnProperty(prefix = XxlJobProperties.PREFIX, value = "enabled", havingValue = "true")`约定了当我们配置了`spring.xxl.job.enable=true`的时候才会生效。

### 创建配置类入口文件

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202501112328656.png)

在你的starter项目的src/main/resources下，创建META-INF/spring目录，并且创建一个

org.springframework.boot.autoconfigure.AutoConfiguration.imports文件，内容如下：
```
cn.hollis.nft.turbo.job.config.XxlJobConfiguration
```

以上就定义好了一个starter，只需要在需要的地方引入，并且配置上相应的配置项就行了，配置项内容就是我们定义在XxlJobProperties中的。

以前，我们配置这些Configuration的时候会用spring.factories，但是这个已经被官方标记为过期，不建议使用了。
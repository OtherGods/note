MyBatis-Plus有非常丰富的插件机制，通过这些插件，可以帮我们实现很多复杂的功能，在我们的项目中 ，用到了乐观锁插件、防全表更新和删除插件以及分页插件。

插件的配置很简单，只需要在依赖好 Mybatis-plus 之后，定义一个mybatisPlusInterceptor，然后在其中把想要的插件注册进去就行了：
```java
package cn.hollis.nft.turbo.datasource.config;  
  
import cn.hollis.nft.turbo.datasource.handler.DataObjectHandler;  
import com.baomidou.mybatisplus.annotation.DbType;  
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;  
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;  
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;  
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
  
/**  
 * @author Hollis */@Configuration  
public class DatasourceConfiguration {  
      
    @Bean  
    public MybatisPlusInterceptor mybatisPlusInterceptor() {  
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();  
        //乐观锁插件  
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());  
        //防全表更新与删除插件  
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());  
        //分页插件  
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));  
        return interceptor;  
    }  
}
```
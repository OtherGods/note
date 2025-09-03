MyBatis-Plus 是一个基于 MyBatis 的增强工具，它简化了 MyBatis 的开发工作，并提供了一些强大的功能和特性。MyBatis-Plus 旨在减少重复代码，提高开发效率，同时保持 MyBatis 的灵活性和强大功能。

# 1、依赖引入

```xml
<!--     Mybatis Plus    -->  
<dependency>  
    <groupId>com.baomidou</groupId>  
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>  
    <version>3.5.5</version>  
</dependency>
```

# 2、配置文件配置

```yml
spring:  
  datasource:  
    url: jdbc:mysql://rm-xxxx.mysql.rds.aliyuncs.com:3306/nfturbo  
    username: nfturbo  
    password: xxx  
    driver-class-name: com.mysql.cj.jdbc.Driver  
    type: com.alibaba.druid.pool.DruidDataSource  
    # Druid连接池配置  
    datasource.druid:  
      initial-size: 5 # 连接池初始化时创建的连接数。默认值为0。  
      min-idle: 5 # 连接池中保持的最小空闲连接数量。当连接池中的连接数量小于这个值时，连接池会尝试创建新的连接。默认值为0。  
      max-active: 20 # 连接池中允许的最大连接数。如果所有连接都被使用并且没有空闲连接，新的连接请求将被阻塞，直到有连接可用。默认值为8。  
      max-wait: 60000 # 获取连接时的最大等待时间，单位为毫秒。如果在指定的时间内无法获取到连接，将抛出异常。默认值为-1，表示无限等待。  
      time-between-eviction-runs-millis: 60000 # 连接池每次检测空闲连接的间隔时间，单位为毫秒。默认值为60000毫秒（1分钟）。  
      min-evictable-idle-time-millis: 300000 # 连接在连接池中的最小空闲时间，超过这个时间的连接将被回收，单位为毫秒。默认值为300000毫秒（5分钟）。  
      validation-query: SELECT 1 # 用于验证连接是否有效的SQL查询语句。Druid会定期执行此查询来检测连接的可用性。默认为"SELECT 1"。  
      test-while-idle: true # 是否在连接空闲时检测连接的有效性。如果设置为true，则连接池会定期检测空闲连接，如果连接失效，将被标记为不可用并移除。默认为true。  
      test-on-borrow: false # 是否在从连接池借用连接时检测连接的有效性。如果设置为true，每次从连接池借用连接时都会执行连接有效性检测。默认为false。  
      test-on-return: false # 是否在归还连接到连接池时检测连接的有效性。如果设置为true，连接在归还到连接池时会进行有效性检测。默认为false。  
      pool-prepared-statements: true # 是否开启预处理语句池。预处理语句池可以提高性能，特别是在执行相同SQL语句多次时。默认为true。  
      max-pool-prepared-statement-per-connection-size: 20 #每个连接上允许的最大预处理语句数。默认值为20。
```

# 3、定义实体类

```java
@Setter  
@Getter  
public class BaseEntity {  
      
    /**  
     * 主键  
     */  
    @TableId(type = IdType.AUTO)  
    private Long id;  
    /**  
     * 是否删除  
     */  
    @TableLogic  
    @TableField(fill = FieldFill.INSERT)  
    private Integer deleted;  
      
    /**  
     * 乐观锁版本号  
     */  
    @Version  
    private Integer lockVersion;  
      
    /**  
     * 创建时间  
     */  
    @TableField(fill = FieldFill.INSERT)  
    private Date gmtCreate;  
      
    /**  
     * 修改时间  
     */  
    @TableField(fill = FieldFill.INSERT_UPDATE)  
    private Date gmtModified;  
      
}
```

其中的@TableId(type = IdType.AUTO)、 @TableField(fill = FieldFill.INSERT)、@Version等都是 mybatis-plus 中的注解，具体用途在其他文档中介绍。

# 4、开启插件

```java
/**  
 * @author Hollis */@Configuration  
public class DatasourceConfiguration {  
      
    @Bean
    public DateTimeObjectHandler myMetaObjectHandler() {  
        return new DateTimeObjectHandler();  
    }  
      
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

# 5、让这个配置生效

org.springframework.boot.autoconfigure.AutoConfiguration.imports：

```java
cn.hollis.nft.turbo.datasource.config.DatasourceConfiguration
```

```
﻿@Mapperpublic interface UserMapper extends BaseMapper<User> {}
```

# 6、写单元测试

```java
@Test  
public void testMybatis() {  
    User user = new User();  
    user.setNickName("test");  
    int result = userMapper.insert(user);  
    System.out.println(result);  
    System.out.println(">>>" + user.getId());  
      
    User existUser = userMapper.findByNickname("test");  
    System.out.println(JSON.toJSONString(existUser));  
      
    user = new User();  
    user.setNickName("test2");  
    result = userMapper.insert(user);  
    System.out.println(result);  
    System.out.println(">>>" + user.getId());  
    existUser = userMapper.findByNickname("test2");  
    System.out.println(JSON.toJSONString(existUser));  
      
}﻿
```

```java
@Service
public class UserService extends ServiceImpl<UserMapper, User> {}
```




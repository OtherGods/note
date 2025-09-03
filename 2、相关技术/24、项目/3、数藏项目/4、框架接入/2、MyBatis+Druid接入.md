MyBatis 是一个优秀的持久层框架，它支持自定义 SQL、存储过程以及高级映射。MyBatis 消除了几乎所有的 JDBC 代码和手动设置参数及获取结果集的工作。MyBatis 可以通过简单的 XML 或注解来配置和映射原生类型、接口和 Java 的 POJO（Plain Old Java Object，普通的 Java 对象）为数据库中的记录。

Druid 是阿里巴巴开源的一个数据库连接池，它提供了强大的监控和扩展功能，并具有高性能和高稳定性的特点。Druid 可以用于管理数据库连接的创建、使用和关闭，从而提高应用程序的性能和可靠性。

# 1、引入依赖，主要依赖 mybatis、MySQL 驱动、以及 druid

```xml
<dependency>  
    <groupId>org.mybatis.spring.boot</groupId>  
    <artifactId>mybatis-spring-boot-starter</artifactId>  
    <version>3.0.2</version>  
</dependency>  
  
<!-- MySQL JDBC 驱动 -->  
<dependency>  
    <groupId>mysql</groupId>  
    <artifactId>mysql-connector-java</artifactId>  
    <version>8.0.27</version>  
</dependency>  
  
<!-- Druid 数据库连接池 -->  
<dependency>  
    <groupId>com.alibaba</groupId>  
    <artifactId>druid</artifactId>  
    <!-- 根据你的需求选择版本 -->  
    <version>1.2.20</version>  
</dependency>
```

# 2、配置文件中配置 datasource

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

# 3、定义一个 mapper

```java
package cn.hollis.nft.turbo.member.infrastructure.mapper; 

import cn.hollis.nft.turbo.member.infrastructure.dataObject.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select; 

@Mapper
public interface UserMapper { 
	UserDO findById(Long id); 
}
```

# 4、写一段 SQL

```xml
<?xml version="1.0" encoding="UTF-8"?>  
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">  
<mapper namespace="cn.hollis.nft.turbo.member.infrastructure.mapper.UserMapper">  
    <resultMap id="BaseResultMap" type="cn.hollis.nft.turbo.member.infrastructure.dataObject.UserDO">  
  
    </resultMap>  
  
    <select id="findById" resultMap="BaseResultMap">  
        SELECT  
	        *  
        FROM  
	        users  
        WHERE  
	        user_name = 'sss'  
    </select>  
</mapper>
```

# 5、定义一个启动类

```java
@SpringBootApplication(scanBasePackages = "cn.hollis.nft")  
@MapperScan(basePackages = "cn.hollis.nft")  
public class NfTurboApplication {  
      
    public static void main(String[] args) {  
        SpringApplication.run(NfTurboApplication.class, args);  
    }  
      
}
```

# 6、运行单元测试

```java
package cn.hollis.nft.turbo.test;  
  
import cn.hollis.nft.turbo.member.infrastructure.mapper.UserMapper;  
import org.junit.Test;  
import org.springframework.beans.factory.annotation.Autowired;  
  
public class MybatisTest extends BaseTest {  
      
    @Autowired  
    private UserMapper userMapper;  
      
    @Test  
    public void testMybatis() {  
        userMapper.findById(1L);  
    }  
}
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411082338278.png)














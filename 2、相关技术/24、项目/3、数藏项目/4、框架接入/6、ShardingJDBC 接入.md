ShardingJDBC 是 Apache ShardingSphere 的一个子项目，它是一个开源的分库分表中间件，提供了透明化的数据分片、分布式事务和数据库治理等功能。ShardingJDBC 以 JDBC Driver 的形式提供，支持任何基于 JDBC 的 ORM 框架、持久层框架和数据库连接池，具有高度兼容性。

# 1、添加依赖

```xml
<dependencies>  
  
    <!--    Mybatis    -->  
    <dependency>  
        <groupId>org.mybatis.spring.boot</groupId>  
        <artifactId>mybatis-spring-boot-starter</artifactId>  
        <version>3.0.3</version>  
    </dependency>  
  
    <!--     Mybatis Plus    -->  
    <dependency>  
        <groupId>com.baomidou</groupId>  
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>  
        <version>3.5.5</version>  
    </dependency>  
  
    <!-- MyBatis-Plus Generator -->  
    <dependency>  
        <groupId>com.baomidou</groupId>  
        <artifactId>mybatis-plus-generator</artifactId>  
        <version>3.5.5</version>  
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
        <version>1.2.20</version>  
    </dependency>  
  
    <!--shardingsphere-->  
    <dependency>  
        <groupId>org.apache.shardingsphere</groupId>  
        <artifactId>shardingsphere-jdbc-core-spring-boot-starter</artifactId>  
        <version>5.2.1</version>  
    </dependency>  
  
    <!--    shardingsphere和2.2不兼容，需要使用1.33，但是1.33和springboot 3.2.2 不兼容，所以自定义了 TagInspector和 UnTrustedTagInspector  -->    
    <dependency>  
        <groupId>org.yaml</groupId>  
        <artifactId>snakeyaml</artifactId>  
        <version>1.33</version>  
    </dependency>  
  
</dependencies>
```

主要是在 mysql、mybatis 等依赖的基础上，增加shardingsphere-jdbc-core-spring-boot-starter的依赖。

这里我们为了解决一个版本兼容问题，增加了一个snakeyaml的依赖，一般来说其实是不用的。
[1、重写snakeyaml的类，解决 ShardingSphere和 SpringBoot 的不兼容问题](2、相关技术/24、项目/3、数藏项目/14、问题排查/1、重写中间件源码/1、重写snakeyaml的类，解决%20ShardingSphere和%20SpringBoot%20的不兼容问题.md)

# 2、添加配置

```yml
spring:  
  shardingsphere:  
    mode:  
      type: Standalone  
      repository:  
        type: JDBC  
    props:  
      sql-show: true  
    datasource:  
      names: ds  
      ds:  
        type: com.alibaba.druid.pool.DruidDataSource  
        driverClassName: com.mysql.cj.jdbc.Driver  
        url: jdbc:mysql://rm-bp101vuh0edt82wo0go.mysql.rds.aliyuncs.com:3306/nfturbo  
        username: nfturbo  
        password: 'NFTurbo666'  
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
    rules:  
      sharding:  
        tables:  
          trade_order:  
            actual-data-nodes: ds.trade_order_0${0..3}  
            keyGenerateStrategy:  
              column: id  
              keyGeneratorName: snowflake  
            table-strategy:  
              standard:  
                shardingColumn: buyer_id  
                shardingAlgorithmName: t-order-inline  
        shardingAlgorithms:  
          t-order-inline:  
            type: INLINE  
            props:  
              algorithm-expression: trade_order_0${Math.abs(buyer_id.hashCode()) % 4}  
        keyGenerators:  
          snowflake:  
            type: SNOWFLAKE  
        auditors:  
          sharding_key_required_auditor:  
            type: DML_SHARDING_CONDITIONS
```

> AI对这段配置的解释：
> 这段配置是关于Spring Boot应用中ShardingSphere（一个开源的分布式数据库解决方案）的详细配置。以下是对这段配置的详细解释：
> 
> 1. **spring.shardingsphere**: 这部分是ShardingSphere的核心配置。
>     - **mode**:
>         - **type**: `Standalone` 表示ShardingSphere运行在单机模式下。
>         - **repository.type**: `JDBC` 表示使用JDBC来持久化ShardingSphere的元数据。
>     - **props**:
>         - **sql-show**: `true` 表示打印SQL语句，用于调试。
>     - **datasource**:
>         - **names**: `ds` 是数据源的名称。
>         - **ds**:
>             - **type**: `com.alibaba.druid.pool.DruidDataSource` 表示使用Druid作为连接池。
>             - **driverClassName**: MySQL的JDBC驱动类名。
>             - **url**: MySQL数据库的连接URL。
>             - **username**: 数据库用户名。
>             - **password**: 数据库密码。
>             - **datasource.druid**: 以下是Druid连接池的具体配置：
>                 - 初始化连接数、最小空闲连接数、最大连接数等。
>                 - 连接等待时间、空闲连接检测时间等。
>                 - 连接有效性检测相关的设置（如检测SQL、检测时机等）。
>                 - 预处理语句池相关设置。
>     - **rules**:
>         - **sharding**: 分片规则配置。
>             - **tables**: 定义分片表。
>                 - **trade_order**: `trade_order`表的分片配置。
>                     - **actual-data-nodes**: 实际的数据节点，表示`trade_order`表被分片到了`ds`数据源的`trade_order_00`到`trade_order_03`这四个表中。
>                     - **keyGenerateStrategy**: 主键生成策略，使用`snowflake`算法。
>                     - **table-strategy**: 分片策略，基于`buyer_id`字段进行分片，使用`t-order-inline`算法。
>             - **shardingAlgorithms**: 分片算法定义。
>                 - **t-order-inline**: INLINE类型的分片算法，基于`buyer_id`的hashCode进行取模分片。
>             - **keyGenerators**: 主键生成器定义。
>                 - **snowflake**: 使用SNOWFLAKE算法生成主键。
>             - **auditors**: 分片审计器配置，用于确保DML操作满足分片条件。
>                 - **sharding_key_required_auditor**: DML分片条件审计器。
> 
> 总的来说，这段配置详细定义了ShardingSphere如何连接到MySQL数据库、如何进行数据分片、如何生成主键等。它使用了Druid作为连接池，并通过INLINE分片算法对`trade_order`表进行了分片。

以上是一个简单的 demo，主要是 sharding 下面的这些配置是我们需要关注的，主要就是关于分库分表的一致基本配置：

spring.shardingsphere：整体的配置的前缀

spring.shardingsphere.mode：运行模式，支持 memory、standalone 和 cluster 几种，我们用的是比较简单的standalone 这种配置

spring.shardingsphere.props.sql-show：配置为 ture 会把执行的 SQL 打印出来，方便排查问题

spring.shardingsphere.datasource：这就是数据源的配置了，所谓数据源就是你的数据库

spring.shardingsphere.rules.sharding：这部分就是关于分表的一些配置了。

spring.shardingsphere.rules.sharding.tables：这部分开始配置分表信息，每一个需要做分库分表的表都需要单独配置一套

spring.shardingsphere.rules.sharding.tables.trade_order：表示是trade_order表的分库分表相关的配置

```yml
trade_order:  
  # 定义 'trade_order' 表的分片配置  
  
  # 指定用于分片的实际数据节点。  
  # 这里 'ds.trade_order_000${0..3}' 表示数据将分布在四个节点上：ds.trade_order_0000、ds.trade_order_0001、ds.trade_order_0002 和 ds.trade_order_0003。  
  actual-data-nodes: ds.trade_order_000${0..3}  
  
  # 定义主键生成策略  
  keyGenerateStrategy:  
    # 指定用于生成主键的列  
    column: id  
    # 指定使用的主键生成器名称，这里使用的是 Snowflake 算法  
    keyGeneratorName: snowflake  
  
  # 定义表的分片策略  
  table-strategy:  
    # 使用复杂分片策略  
    complex:  
      # 指定用于分片的列，这里使用 buyer_id      
      shardingColumns: buyer_id  
      # 指定使用的分片算法名称，这里使用的是 t-order-inline 算法  
      shardingAlgorithmName: t-order-inline
```

spring.shardingsphere.rules.sharding.tables.shardingAlgorithms：是分表算法相关的配置
```yml
shardingAlgorithms:  
  # 定义一个名为 t-order-inline 的分片算法  
  t-order-inline:  
    # 指定算法类型为 INLINE    
    type: INLINE  
    props:  
      # 定义分片表达式，根据 buyer_id 的哈希值进行分片  
      algorithm-expression: trade_order_0${Math.abs(buyer_id.hashCode()) % 4}  
      # 这个表达式将 buyer_id 的哈希值取模 4，以确定记录分配到哪个数据节点（trade_order_0000 到 trade_order_0003）
```

spring.shardingsphere.rules.sharding.keyGenerators：定义一个名为 snowflake 的主键生成器，指定生成器类型为雪花算法

# 3、完成配置

按照以上方式，基本上就完成了 shardingjdbc 的接入，后续就可以进行数据库操作了。


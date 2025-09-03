
再我们的实际工作过程中，会有很多的场景，是希望将我们的sql执行语句打印出来的，一般这种诉求可以怎么实现呢？

在技术派这个项目中给大家提供了两种sql日志打印的方式，给大家提供一些参考
> 实际上添加这篇文章主要是为了多数据源做的准备（比如这个sql到底使用的是哪个数据源）

# 1、MybatisPlus执行日志

因为技术派使用MybatisPlus作为ORM框架进行数据源的CURD，因此我们可以直接使用mybatis提供的日志输出

如开启控制台输出
```yml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl #开启sql日志
```

开启之后，表示形式如：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309202320109.png)

从上面的日志输出也可以看出，包含了sql、参数以及返回结果，整体来看信息比较丰富了，基本上以日常问题排查的角度来看，足以满足

但是，当我们想针对执行日志做一些更自定义的操作，如格式化sql，计算执行耗时，sql执行信息统计、上报等，此时我们可以考虑下面这种基于拦截器的实现方式

# 2、自定义拦截器

参照：[1、MyBatis从入门到精通—MyBatis插件原理探究和自定义插件实现](2、相关技术/15、常用框架-SSM+SpringSecurity/补11、Mybatis/1、MyBatis从入门到精通—MyBatis插件原理探究和自定义插件实现.md)

## 2.1、Mybatis插件机制

在Mybatis中，插件机制提供了非常强大的扩展能力，在sql最终执行之前，提供了四个拦截点，支持不同场景的功能扩展
- Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed)
- ParameterHandler (getParameterObject, setParameters)
- ResultSetHandler (handleResultSets, handleOutputParameters)
- StatementHandler (prepare, parameterize, batch, update, query)

接下来我们再看一下技术派的sql日志记录
```java
package com.github.paicoding.forum.core.dal;  
  
import com.alibaba.druid.pool.DruidPooledPreparedStatement;  
import com.baomidou.mybatisplus.core.MybatisParameterHandler;  
import com.github.paicoding.forum.core.util.DateUtil;  
import com.mysql.cj.MysqlConnection;  
import com.zaxxer.hikari.pool.HikariProxyConnection;  
import com.zaxxer.hikari.pool.HikariProxyPreparedStatement;  
import io.github.classgraph.utils.ReflectionUtils;  
import lombok.extern.slf4j.Slf4j;  
import org.apache.ibatis.executor.statement.StatementHandler;  
import org.apache.ibatis.mapping.BoundSql;  
import org.apache.ibatis.mapping.MappedStatement;  
import org.apache.ibatis.mapping.ParameterMapping;  
import org.apache.ibatis.mapping.ParameterMode;  
import org.apache.ibatis.plugin.*;  
import org.apache.ibatis.reflection.MetaObject;  
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;  
import org.apache.ibatis.session.Configuration;  
import org.apache.ibatis.session.ResultHandler;  
import org.springframework.util.CollectionUtils;  
  
import java.sql.Connection;  
import java.sql.Date;  
import java.sql.Statement;  
import java.util.List;  
import java.util.Properties;  
import java.util.regex.Matcher;  
  
/**  
 * mybatis拦截器。输出sql执行情况  
 *  
 * @author YiHui  
 * @date 2023/5/01  
 */@Slf4j  
@Intercepts({@Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}), @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})})  
public class SqlStateInterceptor implements Interceptor {  
    @Override  
    public Object intercept(Invocation invocation) throws Throwable {  
        long time = System.currentTimeMillis();  
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();  
        String sql = buildSql(statementHandler);  
        Object[] args = invocation.getArgs();  
        String uname = "";  
        if (args[0] instanceof HikariProxyPreparedStatement) {  
            HikariProxyConnection connection = (HikariProxyConnection) ((HikariProxyPreparedStatement) invocation.getArgs()[0]).getConnection();  
            uname = connection.getMetaData().getUserName();  
        }
  
        Object rs;  
        try {  
            rs = invocation.proceed();  
        } catch (Throwable e) {  
            log.error("error sql: " + sql, e);  
            throw e;  
        } finally {  
            long cost = System.currentTimeMillis() - time;  
            sql = this.replaceContinueSpace(sql);  
            // 这个方法的总耗时  
            log.info("\n\n ============= \nsql ----> {}\nuser ----> {}\ncost ----> {}\n ============= \n", sql, uname, cost);  
        }  
  
        return rs;  
    }  
  
    /**  
     * 拼接sql  
     *     * @param statementHandler  
     * @return  
     */  
    private String buildSql(StatementHandler statementHandler) {  
        BoundSql boundSql = statementHandler.getBoundSql();  
        Configuration configuration = null;  
        if (statementHandler.getParameterHandler() instanceof DefaultParameterHandler) {  
            DefaultParameterHandler handler = (DefaultParameterHandler) statementHandler.getParameterHandler();  
            configuration = (Configuration) ReflectionUtils.getFieldVal(handler, "configuration", false);  
        } else if (statementHandler.getParameterHandler() instanceof MybatisParameterHandler) {  
            MybatisParameterHandler paramHandler = (MybatisParameterHandler) statementHandler.getParameterHandler();  
            configuration = ((MappedStatement) ReflectionUtils.getFieldVal(paramHandler, "mappedStatement", false)).getConfiguration();  
        }  
  
        if (configuration == null) {  
            return boundSql.getSql();  
        }  
  
        return getSql(boundSql, configuration);  
    }  
  
  
    /**  
     * 生成要执行的SQL命令  
     *  
     * @param boundSql  
     * @param configuration  
     * @return  
     */  
    private String getSql(BoundSql boundSql, Configuration configuration) {  
        String sql = boundSql.getSql();  
        Object parameterObject = boundSql.getParameterObject();  
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();  
        if (CollectionUtils.isEmpty(parameterMappings) || parameterObject == null) {  
            return sql;  
        }  
  
        MetaObject mo = configuration.newMetaObject(boundSql.getParameterObject());  
        for (ParameterMapping parameterMapping : parameterMappings) {  
            if (parameterMapping.getMode() == ParameterMode.OUT) {  
                continue;  
            }  
  
            //参数值  
            Object value;  
            //获取参数名称  
            String propertyName = parameterMapping.getProperty();  
            if (boundSql.hasAdditionalParameter(propertyName)) {  
                //获取参数值  
                value = boundSql.getAdditionalParameter(propertyName);  
            } else if (configuration.getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass())) {  
                //如果是单个值则直接赋值  
                value = parameterObject;  
            } else {  
                value = mo.getValue(propertyName);  
            }  
            String param = Matcher.quoteReplacement(getParameter(value));  
            sql = sql.replaceFirst("\\?", param);  
        }  
        sql += ";";  
        return sql;  
    }  
  
    public String getParameter(Object parameter) {  
        if (parameter instanceof String) {  
            return "'" + parameter + "'";  
        } else if (parameter instanceof Date) {  
            // 日期格式化  
            return "'" + DateUtil.format(DateUtil.DB_FORMAT, ((Date) parameter).getTime()) + "'";  
        } else if (parameter instanceof java.util.Date) {  
            // 日期格式化  
            return "'" + DateUtil.format(DateUtil.DB_FORMAT, ((java.util.Date) parameter).getTime()) + "'";  
        }  
        return parameter.toString();  
    }  
  
    /**  
     * 替换连续的空白  
     *  
     * @param str  
     * @return  
     */  
    private String replaceContinueSpace(String str) {  
        StringBuilder builder = new StringBuilder(str.length());  
        boolean preSpace = false;  
        for (int i = 0, len = str.length(); i < len; i++) {  
            char ch = str.charAt(i);  
            boolean isSpace = Character.isWhitespace(ch);  
            if (preSpace && isSpace) {  
                continue;  
            }  
  
            if (preSpace) {  
                // 前面的是空白字符，当前的不是空白字符  
                preSpace = false;  
                builder.append(ch);  
            } else if (isSpace) {  
                // 当前字符为空白字符，前面的那个不是的  
                preSpace = true;  
                builder.append(" ");  
            } else {  
                // 前一个和当前字符都非空白字符  
                builder.append(ch);  
            }  
        }  
        return builder.toString();  
    }  
  
    @Override  
    public Object plugin(Object o) {  
        return Plugin.wrap(o, this);  
    }  
  
    @Override  
    public void setProperties(Properties properties) {  
    }  
}

```

上面的实现内容较多，接下来进行一些必要的姿势点拆解

### 2.1.1、ChatGPT解释


在 MyBatis 中，`@Intercepts` 注解和 `@Signature` 注解用于定义一个自定义的拦截器，拦截器可以在 MyBatis 执行 SQL 语句的某些关键点进行操作，例如在查询和更新操作之前或之后进行处理。下面我们详细解释这两个注解的作用：

#### 1、`@Intercepts` 注解

`@Intercepts` 注解用于定义一个 MyBatis 拦截器类。它包含一个或多个 `@Signature` 注解，每个 `@Signature` 注解指定了该拦截器要拦截的接口、方法和参数。

#### 2、`@Signature` 注解

`@Signature` 注解用于指定拦截器要拦截的具体接口方法。它包含以下几个属性：

- `type`: 要拦截的接口类型，例如 `StatementHandler`，`ResultSetHandler`，`ParameterHandler`，或 `Executor`。
- `method`: 要拦截的接口方法的名称，例如 `query` 或 `update`。
- `args`: 要拦截的方法的参数类型。

#### 3、示例解释

```java
@Intercepts({
    @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
    @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})
})
```

这个注解用于定义一个拦截器类，该拦截器类会拦截 `StatementHandler` 接口的 `query` 和 `update` 方法。

##### 1、`type` 属性

```java
type = StatementHandler.class
```
这个属性指定了拦截器要拦截的接口是 `StatementHandler`。`StatementHandler` 是 MyBatis 中处理 SQL 语句的接口。

##### 2、`method` 属性

```java
method = "query"
```
这个属性指定了要拦截的 `StatementHandler` 接口的方法名称是 `query`。

##### 3、`args` 属性

```java
args = {Statement.class, ResultHandler.class}
```
这个属性指定了 `query` 方法的参数类型是 `Statement` 和 `ResultHandler`。因此，拦截器会拦截所有参数为 `Statement` 和 `ResultHandler` 类型的 `query` 方法调用。

类似的，第二个 `@Signature` 注解定义了要拦截的 `update` 方法：

```java
@Signature(type = StatementHandler.class, method = "update", args = {Statement.class})
```

这个注解指定了要拦截 `StatementHandler` 接口中的 `update` 方法，且该方法的参数类型为 `Statement`。

#### 4、使用示例

以下是一个完整的拦截器实现示例：

```java
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;

import java.sql.Statement;
import java.util.Properties;

@Intercepts({
    @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
    @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})
})
public class MyCustomInterceptor implements Interceptor {
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 在方法执行前进行操作
        System.out.println("Before method execution: " + invocation.getMethod());
        
        // 执行原始方法
        Object result = invocation.proceed();
        
        // 在方法执行后进行操作
        System.out.println("After method execution: " + invocation.getMethod());
        
        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 设置拦截器属性
    }
}
```

#### 5、拦截器配置

在 MyBatis 配置文件中配置拦截器：

```xml
<plugins>
    <plugin interceptor="com.example.MyCustomInterceptor"/>
</plugins>
```

#### 6、总结

`@Intercepts` 和 `@Signature` 注解用于定义 MyBatis 拦截器，拦截器可以在 MyBatis 执行 SQL 语句的关键点进行处理。例如，`@Intercepts({@Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}), @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})})` 这段注解定义的拦截器会拦截 `StatementHandler` 接口的 `query` 和 `update` 方法，并对这些方法的执行进行自定义处理。

## 2.2、SQL参数解析

上面的buildSql, getSql方法都是用于sql组装的实现，由于不同的datasource会有不同的解析方式，所有做了一些特殊的判断逻辑

除了上面这些姿势之外，还可以借助OGNL的方式来进行解析参数，如
```java
OgnlContext context = (OgnlContext) Ognl.createDefaultContext(sql.getParameterObject());
List<Object> params = new ArrayList<>(list.size());
for (ParameterMapping mapping : list) {
    params.add(Ognl.getValue(Ognl.parseExpression(mapping.getProperty()), context, context.getRoot()));
}
```

文中介绍的方式则更类似与mybatis的具体实现，源码参考自: `org.apache.ibatis.scripting.defaults.DefaultParameterHandler#setParameters`

其中一种参数解析的核心逻辑如下
```java
BoundSql sql = statementHandler.getBoundSql();
DefaultParameterHandler handler = (DefaultParameterHandler) statementHandler.getParameterHandler();
Field field = handler.getClass().getDeclaredField("configuration");
field.setAccessible(true);
Configuration configuration = (Configuration) ReflectionUtils.getField(field, handler);

// 这种姿势，与mybatis源码中参数解析姿势一直
MetaObject mo = configuration.newMetaObject(sql.getParameterObject());
List<Object> args = new ArrayList<>();
for (ParameterMapping key : sql.getParameterMappings()) {
    args.add(mo.getValue(key.getProperty()));
}
```

当然再上面的具体实现中，直接将sql参数还原到具体的sql语句中了，做了一个简单的适配，针对参数类型，来判断还原时，是否需要加单引号（主要针对字符串类型），日期传参的格式化等

其次就是注意下replaceContinueSpace(String str)的实现，将sql中的连续空白字符替换成单一的空格 （why? 因为mybatis中写的sql可能有各种换行，导致最终输出的sql语句不太美观）

## 2.3、注解

接下来重点关注一下类上的 `@Intercepts` 注解，它表明这个类是一个 `mybatis` 的插件类，通过 `@Signature` 来指定切点

其中的type, method, args用来精确命中切点的具体方法

如根据上面的实例case进行说明
```java
@Intercepts(value = {
	@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
	@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
})					 
```

首先从切点为Executor，然后两个方法的执行会被拦截；这两个方法的方法名分别是query, update，参数类型也一并定义了，通过这些信息，可以精确匹配Executor接口上定义的类，如下
```java
// org.apache.ibatis.executor.Executor

// 对应第一个@Signature
<E> List<E> query(MappedStatement var1, Object var2, RowBounds var3, ResultHandler var4) throws SQLException;

// 对应第二个@Signature
int update(MappedStatement var1, Object var2) throws SQLException;
```

## 2.4、切点说明

mybatis提供了四个切点，那么他们之间有什么区别，什么样的场景选择什么样的切点呢？

一般来讲，拦截ParameterHandler是最常见的，虽然上面的实例是拦截Executor，切点的选择，主要与它的功能强相关，想要更好的理解它，需要从mybatis的工作原理出发，这里将只做最基本的介绍，待后续源码进行详细分析

- Executor：代表执行器，由它调度StatementHandler、ParameterHandler、ResultSetHandler等来执行对应的SQL，其中StatementHandler是最重要的。
- StatementHandler：作用是使用数据库的Statement（PreparedStatement）执行操作，它是四大对象的核心，起到承上启下的作用，许多重要的插件都是通过拦截它来实现的。
- ParameterHandler：是用来处理SQL参数的。
- ResultSetHandler：是进行数据集（ResultSet）的封装返回处理的，它非常的复杂，好在不常用。

借用网上的一张mybatis执行过程来辅助说明
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309262332268.png)

## 2.5、拦截器注册

上面自定义的拦截器实现，还需要注册到应用中让它生效，一般有下面几种姿势

**spring bean对象**
技术派采用的就是这种方式，再DataSourceConfig类中定义了
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309262334752.png)

**SqlSessionFactory**
除了上面的姿势之外还可以直接再sql会话工厂中指定
```java
@Bean(name = "sqlSessionFactory")
public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
    SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
    bean.setDataSource(dataSource);
    bean.setMapperLocations(
            // 设置mybatis的xml所在位置，这里使用mybatis注解方式，没有配置xml文件
            new PathMatchingResourcePatternResolver().getResources("classpath*:mapping/*.xml"));
	// 注册typehandler，供全局使用
    bean.setTypeHandlers(new Timestamp2LongHandler());
    bean.setPlugins(new SqlStateInterceptor());
    return bean.getObject();
}
```

**xml配置**
对于习惯使用myabtis的同学而言，这一种方式不少见，直接再myabtis-config.xml中进行定义
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration
        PUBLIC "-//ibatis.apache.org//DTD Config 3.1//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
	<!-- 插件定义 -->
    <plugins>
        <plugin interceptor="com.github.paicoding.forum.core.dal.SqlStateInterceptor"/>
    </plugins>
</configuration>
```

## 2.6、实测效果

接下来我们看一下实际的体验效果
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309262337017.png)

上面的输出包含了具体的sql，执行耗时，访问的数据源用户（注意这个，后续做数据源切换时用到它来验证我们的实现是否符合预期）

# 3、小结
这里介绍到的可能是大家不经常使用到，但是非常有用的知识点，再实际开发过程中，推荐直接使用Mybatis-Plus的日志输出即可；对于生产环境中有各种要求的场景，不妨考虑下借助myabtis的拦截器来实现我们的个性化诉求

自定义插件实现，重点两步
- 实现接口 `org.apache.ibatis.plugin.Interceptor`
- `@Intercepts` 注解修饰插件类，`@Signature` 定义切点
插件注册三种姿势:
- 注册为Spring Bean
- SqlSessionFactory设置插件
- myabtis.xml文件配置
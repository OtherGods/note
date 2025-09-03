# 1、ChatGPT解释：

MyBatis 插件机制允许开发者通过编写拦截器来拦截和修改 MyBatis 执行的核心方法，从而实现对 SQL 执行过程的自定义控制。通过插件机制，开发者可以在 SQL 执行的关键点进行切入，例如在执行查询、更新、插入、删除操作之前或之后进行额外的处理。下面详细解释 MyBatis 插件机制的原理和使用方法。

## 1.1、MyBatis 插件机制的核心原理

MyBatis 插件机制基于 Java 的动态代理机制，通过拦截器（Interceptor）来拦截 MyBatis 的四个核心接口的方法调用。这四个核心接口分别是：

- **Executor**: 负责执行 SQL 语句的接口。
- **ParameterHandler**: 负责设置 SQL 参数的接口。
- **ResultSetHandler**: 负责处理查询结果集的接口。
- **StatementHandler**: 负责准备 SQL 语句的接口。

通过实现拦截器接口（`Interceptor`），可以拦截这些核心接口的方法调用，并在方法调用前后执行自定义逻辑。

## 1.2、实现拦截器

要实现一个 MyBatis 拦截器，主要需要完成以下几个步骤：

1. **实现 Interceptor 接口**：编写一个类实现 `org.apache.ibatis.plugin.Interceptor` 接口。
2. **使用 @Intercepts 和 @Signature 注解**：使用这些注解来指定要拦截的接口、方法和参数类型。
3. **配置插件**：在 MyBatis 配置文件中注册该拦截器。

## 1.3、示例代码

### 1.3.1、实现 Interceptor 接口

下面是一个自定义拦截器的示例，它会在执行查询和更新操作前后打印日志：

```java
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;

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
        // 设置拦截器属性（如果需要）
    }
}
```

### 1.3.2、配置插件

在 MyBatis 的配置文件（如 `mybatis-config.xml`）中注册该拦截器：

```xml
<plugins>
    <plugin interceptor="com.example.MyCustomInterceptor"/>
</plugins>
```

## 1.4、插件机制的工作流程

1. **初始化**：在 MyBatis 初始化时，根据配置文件中的 `<plugins>` 标签注册拦截器。
2. **创建代理对象**：当 MyBatis 创建核心对象（如 `Executor`、`StatementHandler` 等）时，会通过拦截器的 `plugin` 方法创建这些对象的代理对象。
3. **方法拦截**：当代理对象的方法被调用时，会触发拦截器的 `intercept` 方法。拦截器可以在方法执行前后进行自定义操作，并且可以选择是否继续调用原方法。
4. **自定义逻辑**：在 `intercept` 方法中，拦截器可以通过 `invocation.proceed()` 调用原方法，也可以返回自定义的结果。

## 1.5、使用插件机制的注意事项

- **避免滥用**：虽然拦截器可以在 SQL 执行的关键点进行切入，但滥用拦截器可能会导致性能问题或引入难以排查的错误。
- **插件顺序**：如果配置了多个拦截器，拦截器的执行顺序与它们在配置文件中声明的顺序一致。
- **支持的接口**：MyBatis 插件机制只支持对 `Executor`、`ParameterHandler`、`ResultSetHandler` 和 `StatementHandler` 接口的拦截。

通过 MyBatis 插件机制，开发者可以在不修改 MyBatis 源代码的情况下，灵活地扩展和定制 MyBatis 的行为，以满足各种复杂的业务需求。


# 2、MyBatis从入门到精通—MyBatis插件原理探究和自定义插件实现

## 2.1、插件简介

一般情况下，开源框架都会提供插件或其他形式的拓展点，供开发者自行拓展。这样的好处是显而易见的，一是增加了框架的灵活性。二是开发者可以结合实际需求，对框架进⾏拓展，使其能够更好的工作。以MyBatis为例，我们可基于MyBatis插件机制实现分⻚、分表，监控等功能。由于插件和业务无关，业务也无法感知插件的存在。因此可以⽆感植⼊插件，在⽆形中增强功能。

## Mybatis插件介绍

Mybatis作为⼀个应⽤⼴泛的优秀的ORM开源框架，这个框架具有强⼤的灵活性，在四⼤组件(Executor、StatementHandler、ParameterHandler、ResultSetHandler)处提供了简单易⽤的插 件扩展机制。Mybatis对持久层的操作就是借助于四⼤核⼼对象。MyBatis⽀持⽤插件对四⼤核⼼对象进⾏拦截，对mybatis来说插件就是拦截器，⽤来增强核⼼对象的功能，增强功能本质上是借助于底层的 动态代理实现的，换句话说，MyBatis中的四⼤对象都是代理对象。 MyBatis所允许拦截的⽅法如下：

- **执行器Executor** (update、query、commit、rollback等⽅法)；
- **SQL语法构建器StatementHandler**（prepare、parameterize、batch、updates query等方法)；
- **参数处理器ParameterHandler** (getParameterObject、setParameters⽅法)；
- **结果集处理器ResultSetHandler** (handleResultSets、handleOutputParameters等⽅法)；

## 2.2、Mybatis插件原理

在四⼤对象创建的时候

1. 每个创建出来的对象不是直接返回的，⽽是`interceptorChain.pluginAll(parameterHandler)`;
2. 获取到所有的Interceptor (拦截器)(插件需要实现的接⼝)；调⽤ interceptor.plugin(target);返回 target 包装后的对象
3. 插件机制，我们可以使⽤插件为⽬标对象创建⼀个代理对象；AOP (⾯向切⾯)我们的插件可以为四⼤对象创建出代理对象，代理对象就可以拦截到四⼤对象的每⼀个执⾏；

**拦截** 插件具体是如何拦截并附加额外的功能的呢？以ParameterHandler来说。
```java
public ParameterHandler newParameterHandler(MappedStatement mappedStatement,
Object object, BoundSql sql, InterceptorChain interceptorChain){
    ParameterHandler parameterHandler =
    mappedStatement.getLang().createParameterHandler(mappedStatement,object,sql);
    parameterHandler = (ParameterHandler)interceptorChain.pluginAll(parameterHandler);
    return parameterHandler;
}
public Object pluginAll(Object target) {
    for (Interceptor interceptor : interceptors) {
        target = interceptor.plugin(target);
    }
    return target;
}
```

**interceptorChain**保存了所有的拦截器(interceptors)，是mybatis初始化的时候创建的。调⽤拦截器链中的拦截器依次的对⽬标进⾏拦截或增强。interceptor.plugin(target)中的target就可以理解为mybatis中的四⼤对象。返回的target是被重重代理后的对象 如果我们想要拦截Executor的query⽅法，那么可以这样定义插件：
```java
@Intercepts({
    @Signature(
    type = Executor.class,
    method = "query",
    args={MappedStatement.class,Object.class,RowBounds.class,ResultHandler.class}
    )
})
public class ExeunplePlugin implements Interceptor {
    //省略逻辑
}
```

除此之外，我们还需将插件配置到sqlMapConfig.xml中。
```xml
<plugins>
  <plugin interceptor="com.zjq.plugin.ExamplePlugin"></plugin>
</plugins>
```

这样MyBatis在启动时可以加载插件，并保存插件实例到相关对象(InterceptorChain，拦截器链) 中。待准备⼯作做完后，MyBatis处于就绪状态。我们在执⾏SQL时，需要先通过DefaultSqlSessionFactory创建 SqlSession。Executor 实例会在创建 SqlSession 的过程中被创建， Executor实例创建完毕后，MyBatis会通过JDK动态代理为实例⽣成代理类。这样，插件逻辑即可在 Executor相关⽅法被调⽤前执⾏。 以上就是MyBatis插件机制的基本原理。

## 2.3、自定义插件

### 2.3.1、插件接口

Mybatis 插件接⼝-Interceptor

- Intercept⽅法，插件的核⼼⽅法
- plugin⽅法，⽣成target的代理对象
- setProperties⽅法，传递插件所需参数

### 2.3.2、自定义插件

设计实现⼀个自定义插件
```java
Intercepts ({//注意看这个⼤花括号，也就这说这⾥可以定义多个@Signature对多个地⽅拦截，都⽤这个拦截器
    @Signature (type = StatementHandler .class , //这是指拦截哪个接⼝
    method = "prepare"，//这个接⼝内的哪个⽅法名，不要拼错了
    args = { Connection.class, Integer .class}),// 这是拦截的⽅法的⼊参，按顺序写到这，不要多也不要少，如果⽅法重载，可是要通过⽅法名和⼊参来确定唯⼀的
})
public class MyPlugin implements Interceptor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    // //这⾥是每次执⾏操作的时候，都会进⾏这个拦截器的⽅法内
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //增强逻辑
        System.out.println("对⽅法进⾏了增强....")；
        return invocation.proceed(); //执⾏原⽅法
    }
    /**
    * //主要是为了把这个拦截器⽣成⼀个代理放到拦截器链中
    * ^Description包装⽬标对象 为⽬标对象创建代理对象
    * @Param target为要拦截的对象
    * @Return代理对象
    */
    @Override
    public Object plugin(Object target) {
        System.out.println("将要包装的⽬标对象："+target);
        return Plugin.wrap(target,this);
    }
    /**获取配置⽂件的属性**/
    //插件初始化的时候调⽤，也只调⽤⼀次，插件配置的属性从这⾥设置进来
    @Override
    public void setProperties(Properties properties) {
        System.out.println("插件配置的初始化参数："+properties );
    }
}
```

sqlMapConfig.xml
```xml
<plugins>
  <plugin interceptor="com.zjq.plugin.MySqlPagingPlugin">
    <!--配置参数-->
    <property name="name" value="Bob"/>
  </plugin>
</plugins>
```

mapper接⼝
```xml
public interface UserMapper {
    List<User> findByCondition(User user);
}
```

mapper.xml
```xml
<mapper namespace="com.zjq.mapper.UserMapper">

    <!--sql语句抽取-->
    <sql id="selectUser">
      select * from user
    </sql>

    <select id="findByCondition" parameterType="user" resultType="user">
        <include refid="selectUser"></include>
        <where>
            <if test="id!=0">
                and id=#{id}
            </if>
            <if test="username!=null and username!=''">
                and username=#{username}
            </if>
            <if test="password!=null and password!=''">
                and password=#{password}
            </if>
        </where>
    </select>
</mapper>
```

测试类
```java
public class PluginTest {
    @Test
    public void test() throws IOException {
        InputStream resourceAsStream =
        Resources.getResourceAsStream("sqlMapConfig.xml");
        SqlSessionFactory sqlSessionFactory = new 				SqlSessionFactoryBuilder().build(resourceAsStream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        User condition = new User();
        //condition.setId(1);
        condition.setUsername("zjq");
        List<User> byPaging = userMapper.findByCondition(condition);
        for (User user : byPaging) {
            System.out.println(user);
        }
    }
}
```

## 2.4、源码分析

**执⾏插件逻辑** Plugin实现了 InvocationHandler接⼝，因此它的invoke⽅法会拦截所有的⽅法调⽤。invoke⽅法会 对 所拦截的⽅法进⾏检测，以决定是否执⾏插件逻辑。该⽅法的逻辑如下：
```java
// -Plugin
public Object invoke(Object proxy, Method method, Object[] args) throws
Throwable {
    try {
    /*
    *获取被拦截⽅法列表，⽐如：
    * signatureMap.get(Executor.class), 可能返回 [query, update,
    commit]
    */
    Set<Method> methods = signatureMap.get(method.getDeclaringClass());
    //检测⽅法列表是否包含被拦截的⽅法
    if (methods != null && methods.contains(method)) {
        //执⾏插件逻辑
        return interceptor.intercept(new Invocation(target, method,
        args));
        //执⾏被拦截的⽅法
        return method.invoke(target, args);
        } catch(Exception e){
    }
}
```

invoke⽅法的代码⽐较少，逻辑不难理解。⾸先,invoke⽅法会检测被拦截⽅法是否配置在插件的@Signature注解中，若是，则执⾏插件逻辑，否则执⾏被拦截⽅法。插件逻辑封装在intercept中，该⽅法的参数类型为Invocationo Invocation主要⽤于存储⽬标类，⽅法以及⽅法参数列表。下⾯简单看⼀下该类的定义
```java
public class Invocation {
    private final Object target;
    private final Method method;
    private final Object[] args;
    public Invocation(Object targetf Method method, Object[] args) {
    this.target = target;
    this.method = method;
    //省略部分代码
    public Object proceed() throws InvocationTargetException,
    IllegalAccessException { //调⽤被拦截的⽅法
```

关于插件的执⾏逻辑就分析结束

## 2.5、pageHelper分页插件

MyBatis可以使⽤第三⽅的插件来对功能进⾏扩展，分⻚助⼿PageHelper是将分⻚的复杂操作进⾏封装，使⽤简单的⽅式即可获得分⻚的相关数据 开发步骤：

1. 导⼊通⽤PageHelper坐标
```xml
<dependency>
  <groupId>com.github.pagehelper</groupId>
  <artifactId>pagehelper</artifactId>
  <version>3.7.5</version>
</dependency>
<dependency>
  <groupId>com.github.jsqlparser</groupId>
  <artifactId>jsqlparser</artifactId>
  <version>0.9.1</version>
</dependency>
```

2. 在mybatis核⼼配置⽂件中配置PageHelper插件
```xml
<!--注意：分⻚助⼿的插件 配置在通⽤馆mapper之前*-->*
<plugin interceptor="com.github.pagehelper.PageHelper">
  <!—指定⽅⾔ —>
  <property name="dialect" value="mysql"/>
</plugin>
```

3. 测试分⻚代码实现
```java
@Test
public void testPageHelper() {
    //设置分⻚参数
    PageHelper.startPage(1, 2);
    User condition = new User();
    //condition.setId(1);
    condition.setUsername("zjq");
    List<User> select = userMapper.findByCondition(condition);
    for (User user : select) {
        System.out.println(user);
    }
}
```

获得分⻚相关的其他参数
```java
//其他分⻚的数据
PageInfo<User> pageInfo = new PageInfo<User>(select);
System.out.println("总条数："+pageInfo.getTotal());
System.out.println("总⻚数："+pageInfo. getPages ());
System.out.println("当前⻚："+pageInfo. getPageNum());
System.out.println("每⻚显示⻓度："+pageInfo.getPageSize());
System.out.println("是否第⼀⻚："+pageInfo.isIsFirstPage());
System.out.println("是否最后⼀⻚："+pageInfo.isIsLastPage());
```

## 2.6、通用mapper

### 2.6.1、什么是通用Mapper

通⽤Mapper就是为了解决单表增删改查，基于Mybatis的插件机制。开发⼈员不需要编写SQL,不需要在DAO中增加⽅法，只要写好实体类，就能⽀持相应的增删改查⽅法

### 2.6.2、如何使用

1. ⾸先在maven项⽬，在pom.xml中引⼊mapper的依赖
```xml
<dependency>
  <groupId>tk.mybatis</groupId>
  <artifactId>mapper</artifactId>
  <version>3.1.2</version>
</dependency>
```

2. Mybatis配置⽂件中完成配置
```xml
<plugins>
  <!--分⻚插件：如果有分⻚插件，要排在通⽤mapper之前-->
  <plugin interceptor="com.github.pagehelper.PageHelper">
    <property name="dialect" value="mysql"/>
  </plugin>
  <plugin interceptor="tk.mybatis.mapper.mapperhelper.MapperInterceptor">
    <!-- 通⽤Mapper接⼝，多个通⽤接⼝⽤逗号隔开 -->
    <property name="mappers" value="tk.mybatis.mapper.common.Mapper"/>
  </plugin>
</plugins>
```

3. 实体类设置主键
```java
@Table(name = "t_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String username;
}
```

4. 定义通⽤mapper
```java
import com.zjq.domain.User;
import tk.mybatis.mapper.common.Mapper;
public interface UserMapper extends Mapper<User> {
}
```

5. 测试
```java
@Test
public void test1() throws IOException {
    Inputstream resourceAsStream =
    Resources.getResourceAsStream("sqlMapConfig.xml");
    SqlSessionFactory build = new
    SqlSessionFactoryBuilder().build(resourceAsStream);
    SqlSession sqlSession = build.openSession();
    UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
    User user = new User();
    user.setId(4);
    //(1)mapper基础接⼝
    //select 接⼝
    //根据实体中的属性进⾏查询，只能有一个返回值
    User user1 = userMapper.selectOne(user); 
    //查询全部结果
    List<User> users = userMapper.select(null); 
    //根据主键字段进⾏查询，⽅法参数必须包含完整的主键属性，查询条件使⽤等号
    userMapper.selectByPrimaryKey(1); 
    //根据实体中的属性查询总数，查询条件使⽤等号
    userMapper.selectCount(user); 
    
    // insert 接⼝
    //保存⼀个实体，null值也会保存，不会使⽤数据库默认值
    int insert = userMapper.insert(user);
    //保存实体，null的属性不会保存，会使⽤数据库默认值
    int i = userMapper.insertSelective(user); 
    
    // update 接⼝
    //根据主键更新实体全部字段，null值会被更新
    int i1 = userMapper.updateByPrimaryKey(user);
    
    // delete 接⼝
    //根据实体属性作为条件进⾏删除，查询条件使⽤等号
    int delete = userMapper.delete(user);
    //根据主键字段进⾏删除，⽅法参数必须包含完整的主键属性
    userMapper.deleteByPrimaryKey(1); 
    
    //(2)example⽅法
    Example example = new Example(User.class);
    example.createCriteria().andEqualTo("id", 1);
    example.createCriteria().andLike("val", "1");
    //⾃定义查询
    List<User> users1 = userMapper.selectByExample(example);
}
```


转载自：[MyBatis从入门到精通—MyBatis插件原理探究和自定义插件实现](https://juejin.cn/post/7120603272326594567)

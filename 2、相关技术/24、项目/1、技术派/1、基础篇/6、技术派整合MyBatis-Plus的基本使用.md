今天由我来给大家讲一下《技术派整合MyBatis-Plus的基本使用》。顾名思义，MyBatis-Plus 是 MyBatis 的一个增强，提供了一些额外的功能，比如说条件构造器、分页插件、代码生成器等等以便我们能更专注于业务逻辑，而不是 SQL 语句的编写。

MyBatis-Plus 的源码也是开源的，在 GitHub 上已经收获了 14.5k+ 的 star，非常受欢迎。如果打算学习源码的同学可以尝试一下。
> https://github.com/baomidou/mybatis-plus

MyBatis-Plus 还提供了官方文档，对 MyBatis-Plus 的入门、核心功能、扩展功能，以及插件功能做了详细地介绍，大家可以通过官方文档进行查漏补缺。
> https://www.baomidou.com/pages/24112f/



# 1、SpringBoot整合Mybatis-Plus步骤
技术派中整合 MyBatis-Plus 的方式非常简单。

第一步，在 pom.xml 中引入 starter。
```yml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
</dependency>
```

第二步，使用@MapperScan注解扫描mmapper文件
```java
@Configuration
@ComponentScan("com.github.paicoding.forum.service")
@MapperScan(basePackages = {
	"com.github.paicoding.forum.service.article.repository.mapper",
	"com.github.paicoding.forum.service.user.repository.mapper",
	"com.github.paicoding.forum.service.comment.repository.mapper",
	"com.github.paicoding.forum.service.config.repository.mapper",
	"com.github.paicoding.forum.service.statistics.repository.mapper",
	"com.github.paicoding.forum.service.notify.repository.mapper",})
public class ServiceAutoConfig{
}
```

ServiceAutoConfig 是单独的配置类，mapper 接口按照业务进行了分类，mapper.xml 放在 resources 目录下。有一说一，技术派这样的目录结构非常的清晰，井井有条，一目了然。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211528548.png)

第三步，在 application.yml 文件中增加MyBatis-Plus 的统一配置。
```yml
# mybatis 相关统一配置
mybatis-plus:
	configuration:
		#开启下划线转驼峰
		map-underscore-to-camel-case: true
		
		#开启sql日志
		log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

map-underscore-to-camel-case: true 的作用是将数据库表中的下划线命名方式（underscore case）映射为 Java 对象中的驼峰命名方式（camel case）。例如，数据库表中的列名为 user_name，对应的 Java 对象的属性名为 userName。

OK，以上三步就完成了 MyBatis-Plus 和 Spring Boot 项目的整合。接下来，我们来一一介绍 MyBatis-Plus 的基本使用，包括新增、注解、查询、条件构造器、自定义 SQL、分页查询、更新删除、AR 模式、主键策略，以及通过 service。

# 2、Mybatis-Plus的基本使用

## 2.1、Service CURD
我个人感觉Service CURD就是对Mapper CURD做了进一步的封装（没有实际依据）。。。

技术派中的通用增删改查是通过 MyBatis-Plus 的 Service CRUD 接口实现的。
比如说我们要保存一个文章的标签🏷，可以在业务逻辑层中通过这种方式:
```java
//Service接口中

@Autowired
private TagDao tagDao;

tagDao.save(tagDO);
```

1. tagDao 是我们定义的数据访问对象（Data Access Object，简称 DAO），它继承自 MyBatis-Plus 提供的 ServiceImpl 类。@Autowired 注解将 TagDao 自动注入到当前类中。这是 Spring 提供的依赖注入（DI）功能，可以让我们在当前类中方便地使用 TagDao。
   ```java
   @Repository
   public class TagDao extends ServiceImpl<TagMapper, TagDO> {
   ```
	- @Repository 注解：这是 Spring 提供的注解，用于标识这个类是一个数据访问层（DAO）组件。Spring 会自动扫描并将其实例化为一个 Bean，方便在其他类中通过依赖注入（DI）使用。
	- ServiceImpl<TagMapper, TagDO>：ServiceImpl 是 MyBatis-Plus 提供的一个抽象类，提供了通用的 CRUD 方法。泛型参数 <TagMapper, TagDO> 意味着 TagDao 类主要用于处理 TagDO 数据对象的数据库操作，并使用 TagMapper 接口定义的方法进行操作。
	  
	  通过继承 ServiceImpl 类，TagDao 就可以使用 MyBatis-Plus 提供的通用 CRUD 方法，如 save、getById、updateById 等。这些方法已经实现了基本的数据库操作，通常无需自己编写 SQL 语句。
	  ```java
	  /**
		 * IService 实现类（ 泛型：M 是 mapper 对象，T 是实体 ）
		 *
		 * @author hubin
		 * @since 2018-06-23
		 */
		@SuppressWarnings("unchecked")
		public class ServiceImpl<M extends BaseMapper<T>, T> implements IService<T> {
		}
	  ```
   
2. 参数 tagDO 是一个数据对象（Data Object，简称 DO），表示数据库中的 tag 表。
   ```java
   @Data  
	@EqualsAndHashCode(callSuper = true)  
	@TableName("tag")  
	public class TagDO extends BaseDO {  
	    private static final long serialVersionUID = 3796460143933607644L;  
	  
	    /**  
	     * 标签名称  
	     */  
	    private String tagName;  
	  
	    /**  
	     * 标签类型：1-系统标签，2-自定义标签  
	     */  
	    private Integer tagType;  
	  
	    /**  
	     * 状态：0-未发布，1-已发布  
	     */  
	    private Integer status;  
	  
	    /**  
	     * 是否删除  
	     */  
	    private Integer deleted;  
	}
   ```
   - @Data 注解是 Lombok 提供的，用于自动生成类的 getter、setter、equals、hashCode 和 toString 方法，简化了代码编写。
   - @EqualsAndHashCode(callSuper = true) 注解也是 Lombok 提供的注解，callSuper = true 表示要调用父类（BaseDO）的 equals 和 hashCode 方法。
 
 BaseDO 是我们自定义的 DO 基类，实现了 Serializable 接口，并且定义了主键 id（@TableId(type = IdType.AUTO) 表示自增长，是 MyBatis-Plus 提供的注解），创建时间 createTime 和更新时间 updateTime。

```java
@Data  
	public class BaseDO implements Serializable {  	  
	    @TableId(type = IdType.AUTO)  
	    private Long id;  
	  
	    private Date createTime;  
	  
	    private Date updateTime;  
	}
```

 - @TableName("tag") 注解是 MyBatis-Plus 提供的注解，用于指定数据库表名。
 - 另外定义了四个属性：tagName（标签名称）、tagType（标签类型）、status（状态）和 deleted（是否删除）。这些属性对应数据库表中的列。

启动 Redis、服务端、admin 端，通过 admin 端新增一个标签。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211609810.png)
在控制台就可以看到新添加的标签了。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211609280.png)

## 2.2、Mapper CURD
MyBatis-Plus 除了提供 Service 的 CRUD， 还提供了基于 Mapper 的 CRUD。

技术派中一些特殊的增删改查是通过 MyBatis-Plus 的 Mapper CRUD 接口实现的。

比如说我们要保存文章，可以通过下面这种方式。
```java
@Repository
public class ArticleDao extends ServiceImpl<ArticleMapper, ArticleDO> {
    @Resource
    private ArticleDetailMapper articleDetailMapper;
    public Long saveArticleContent(Long articleId, String content) {
        ArticleDetailDO detail = new ArticleDetailDO();
        detail.setArticleId(articleId);
        detail.setContent(content);
        detail.setVersion(1L);
        articleDetailMapper.insert(detail);
        return detail.getId();
    }
}   
```

1、articleDetailMapper 是我们在当前类中注入的一个 Mapper 接口。
```java
public interface ArticleDetailMapper extends BaseMapper<ArticleDetailDO> {}
```

它继承自 MyBatis-Plus 的 BaseMapper 接口。
```java
/**  
 * Mapper 继承该接口后，无需编写 mapper.xml 文件，即可获得CRUD功能  
 * <p>这个 Mapper 支持 id 泛型</p>  
 * * @author hubin * @since 2016-01-23 */
public interface BaseMapper<T> extends Mapper<T> {  
  
    /**  
     * 插入一条记录  
     *  
     * @param entity 实体对象  
     */  
    int insert(T entity);  
  
    /**  
     * 根据 entity 条件，删除记录  
     *  
     * @param queryWrapper 实体对象封装操作类（可以为 null,里面的 entity 用于生成 where 语句）  
     */  
    int delete(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);  
  
    /**  
     * 根据 whereEntity 条件，更新记录  
     *  
     * @param entity        实体对象 (set 条件值,可以为 null)  
     * @param updateWrapper 实体对象封装操作类（可以为 null,里面的 entity 用于生成 where 语句）  
     */  
    int update(@Param(Constants.ENTITY) T entity, @Param(Constants.WRAPPER) Wrapper<T> updateWrapper);  
  
    /**  
     * 根据 ID 查询  
     *  
     * @param id 主键ID  
     */    
     T selectById(Serializable id);
 //……………………
}
```
这样，articleDetailMapper 也就具备了基本的增删改查功能。

在浏览器地址栏中访问 http://localhost:8080/ 并写一篇文章。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211631195.png)

可以在控制台看到文章的插入信息。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211633860.png)

删改查后面在介绍 MyBatis-Plus 的其他功能时会讲到。

## 2.3、常用注解

1. @TableName：用于指定数据库表名，通常在实体类（DO 或 Entity）上使用。例如：@TableName("user")。
2. @TableId：用于指定表中的主键字段。通常在实体类的主键属性上使用。例如：@TableId(value = "id", type = IdType.AUTO)，其中 value 表示主键字段名，type 表示主键生成策略。
3. @TableField：用于指定表中的非主键字段。可以用于实体类的属性上，以映射属性和数据库字段。例如：@TableField(value = "user_name", exist = true)，其中 value 表示数据库中的字段名，exist 表示该字段是否存在（默认为 true，设置为 false 自然就是表示数据库中不存在了）。
4. @TableLogic：用于指定逻辑删除字段。逻辑删除是指在数据库中标记某个记录已删除，而不是真正地删除记录。例如：@TableLogic(value = "0", delval = "1")，其中 value 表示未删除状态的默认值，delval 表示删除状态的值。
5. @Version：用于指定乐观锁字段。乐观锁是一种并发控制策略，用于解决多个线程同时修改同一条记录的问题。例如：@Version private Integer version;。
6. @EnumValue：用于指定枚举类型字段的映射。例如：@EnumValue private Integer status;。
7. @InterceptorIgnore：用于忽略 Mybatis-Plus 拦截器的处理。例如：@InterceptorIgnore(tenantLine = "true")，表示忽略多租户拦截器。


# 3、Mybatis-Plus查询方法

## 3.1、普通查询
MyBatis-Plus 的 BaseMapper 提供了多种查询方法，比如说技术派中根据 ID 查找文章是这样用的：
```java
ArticleDO article = baseMapper.selectById(articleId);
```

除此之外，还有根据ID 批量查询的 selectBatchIds：
```java
List<T> selectBatchIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);
```

用法也很简单：
```java
baseMapper.selectBatchIds(Arrays.asList(1,2));
```

根据键值对查询的 selectByMap：
```java
List<T> selectByMap(@Param(Constants.COLUMN_MAP) Map<String, Object> columnMap);
```

用法如下（id为15）：
```java
Map<String, Object> map = new HashMap<>();
map.put("id", 15L);
List<ArticleDO> dtoList = baseMapper.selectByMap(map);
```


## 3.2、条件构造器
MyBatis-Plus 的 Wrapper 是一个条件构造器，用于简化复杂的 SQL 查询条件的构建。它提供了一系列易于使用的 API，让你能够以链式编程的方式编写查询条件，而不需要手动编写 SQL 语句。

假如我们来查询这样一个结果，包含“j”且状态是已发布的标签。我们可以这样来构建条件构造器：
```java
@Test  
public void testWrapper() {  
    QueryWrapper<TagDO> wrapper = new QueryWrapper<>();  
    // 包含“j”且状态是已发布  
    wrapper.like("tag_name", "j").eq("status", 1);  
    BaseMapper<TagDO> baseMapper = tagDao.getBaseMapper();  
    List<TagDO> tagList = baseMapper.selectList(wrapper);  
    tagList.forEach(System.out::println);  
}
```

QueryWrapper：用于构建查询条件。它继承自 AbstractWrapper，提供了各种查询条件的构建方法，如 eq, ne, gt, ge, lt, le, like, isNull, orderBy 等等。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211645648.png)

通过上面的方法，我们将返回全部列，如果只想返回一部分，该怎么办呢？可以通过 select 来设置查询字段。
```java
wrapper.select("tag_name","status")
      .like("tag_name", "j").eq("status", 1);
```

但是，通过表的字段总感觉很不舒服，万一哪一天数据库表发生变化了怎么办呢？代码和数据库就不匹配了呀。

更优雅的做法是采用 Lambda 的方式，技术派中的条件构造器就用的这种方式。

比如说查询标签。
```java
/**  
 * 获取已上线 Tags 列表（分页）  
 *  
 * @return 
 * */
public List<TagDTO> listOnlineTag(String key, PageParam pageParam) {
    LambdaQueryWrapper<TagDO> query = Wrappers.lambdaQuery();  
    query.eq(TagDO::getStatus, PushStatusEnum.ONLINE.getCode())  
            .eq(TagDO::getDeleted, YesOrNoEnum.NO.getCode())  
            .and(StringUtils.isNotBlank(key), v -> v.like(TagDO::getTagName, key))  
            .orderByDesc(TagDO::getId);  
    if (pageParam != null) {  
        query.last(PageParam.getLimitSql(pageParam));  
    }  
    List<TagDO> list = baseMapper.selectList(query);  
    return ArticleConverter.toDtoList(list);  
}
```

1. 可以通过 Wrappers.lambdaQuery() 静态方法创建一个 Lambda 条件构造器。
2. `eq(TagDO::getStatus, PushStatusEnum.ONLINE.getCode())`：表示查询条件为 status 等于 PushStatusEnum.ONLINE 的值（即查询上线的标签）。
3. `eq(TagDO::getDeleted, YesOrNoEnum.NO.getCode())`：表示查询条件为 deleted 等于 YesOrNoEnum.NO 的值（即查询未删除的记录）。
4. `and(!StringUtils.isEmpty(key), v -> v.like(TagDO::getTagName, key))`：表示如果 key 不为空，则添加一个查询条件，要求 tag_name 包含 key。
5. `orderByDesc(TagDO::getId)`：表示按照 id 字段降序排序。
6. `if (pageParam != null) { query.last(PageParam.getLimitSql(pageParam)); }`：如果 pageParam 不为 null，则添加分页参数。

这样的话，就可以和数据库的字段隔离开，完全通过代码的方式去查询。

再比如说查询文章列表：
```java
public List<ArticleDO> listArticles(PageParam pageParam) {
	return lambdaQuery()
		.eq(ArticleDO::getDeleted, YesOrNoEnum.NO.getCode())
		.last(PageParam.getLimitSql(pageParam))
		.orderByDesc(ArticleDO::getId)
		.list();
```

1. lambdaQuery() 是 MyBatis-Plus 的 IService 接口提供的一个默认方法，可以在 Service 中直接调用返回一个 Lambda 条件构造器。
   ```java
   default LambdaQueryChainWrapper<T> lambdaQuery() {
	   return ChainWrappers.lambdaQueryChain(getBaseMapper());
}
   ```
2. `eq(ArticleDO::getDeleted, YesOrNoEnum.NO.getCode())`：表示查询条件为 deleted 等于 YesOrNoEnum.NO 的值（即查询未删除的记录）。
3. `last(PageParam.getLimitSql(pageParam))`：在查询的最后添加一个分页语句，这里根据 pageParam 参数生成分页的 SQL 语句。
4. `orderByDesc(ArticleDO::getId)`：表示按照 id 字段降序排序。
5. list()：执行查询，并返回查询结果的列表。


# 4、Mybatis-Plus自定义SQL
MyBatis-Plus 支持自定义 SQL 语句，我们可以在 Mapper 接口中编写自定义 SQL 方法，并使用注解添加自定义的 SQL 语句。

技术派中在使用微信登录的时候会执行这条 SQL 语句：
```java
public interface UserMapper extends BaseMapper<UserDO> {  
    /**  
     * 根据三方唯一id进行查询  
     *  
     * @param accountId  
     * @return  
     */    
	@Select("select * from user where third_account_id = #{account_id} limit 1")  
	UserDO getByThirdAccountId(@Param("account_id") String accountId);  
}
```

接口中定义了一个名为 getByThirdAccountId 的方法，它接收一个名为 accountId 的参数。

该方法使用了 @Select 注解，这个注解用于编写自定义的 SQL 查询。@Select 注解内的 SQL 语句是：select * from user where third_account_id = #{account_id} limit 1，它会根据传入的 account_id 参数查询 user 表中的记录。
同时，方法参数 accountId 使用了 @Param 注解，指定了参数在 SQL 语句中的名称为 account_id。这样，在执行 SQL 语句时，MyBatis 会将参数值替换到对应的位置上。

我们来测试一下。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211705647.png)

测试结果：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211706974.png)



除此之外，技术派中还使用了 xml 的方式，用来定义一些复杂的 SQL。比如说，我们要统计网站的 PV、UV，那么我们在 resources 目录下新建一个名为 QueryCountMapper.xml 的文件，内容如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211708744.png)

1. 在 resources 目录下的好处是，MyBatis-Plus 默认帮我们配置了 xml 的位置，这样我们就不需要在 application.yml 中再配置了。
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211711142.png)
   `classpath*:\/mapper\/**\/*.xml` 表示 MyBatis-Plus 会扫描 resources 下的 mapper 文件夹及其子文件夹中的所有 XML 文件。这是一个推荐的项目结构，因为它可以将资源文件与 Java 代码分离，使项目结构更清晰。

2. 该 XML 文件定义了一个名为 RequestCountMapper 的映射器，它包含了三个自定义查询：getPvTotalCount、getPvDayList 和 getUvDayList。它与 com.github.paicoding.forum.service.statistics.repository.mapper.RequestCountMapper 相匹配。
   ```java
	/**  
	 * 请求计数mapper接口  
	 *  
	 * @author louzai * @date 2022-10-1 */
public interface RequestCountMapper extends BaseMapper<RequestCountDO> {  
	  
	    /**  
	     * 获取 PV 总数  
	     *  
	     * @return     
	     * */    
	    Long getPvTotalCount();  
	  
	    /**  
	     * 获取 PV 数据列表  
	     * @param day  
	     * @return  
	     */    
		List<StatisticsDayDTO> getPvDayList(@Param("day") Integer day);
	  
	    /**  
	     * 获取 UV 数据列表  
	     *  
	     * @param id  
	     * @return
	     */  
	    List<StatisticsDayDTO> getUvDayList(@Param("day") Integer day);
	}
   ```

3. getPvTotalCount 查询：返回类型为 java.lang.Long，查询语句为 select sum(cnt) from request_count。此查询计算 request_count 表中所有记录的 cnt 列值之和。
4. getPvDayList 查询：返回类型为 StatisticsDayDTO。此查询根据传入的 day 参数获取按日期分组的请求数量统计信息，并按日期升序排列。
5. getUvDayList 查询：返回类型同样为 StatisticsDayDTO。此查询根据传入的 day 参数获取按日期分组的唯一访客数量统计信息，并按日期升序排列。
打开 admin 端，可以查看到这三项数据。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211715171.png)


# 5、Mybatis-Plus更新和删除
## 5.1、更新
我们来先看个最简单的，直接调用 Service 的 updateById 方法，也就是根据 ID 更新，比如说更新标签内容：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211725682.png)

Service 的 update 其实是对 Mapper 的 update 做了一个封装。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211725481.png)

后台把“技术派”的标签修改为“技术派π”。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211725448.png)

后台可以看到修改的 SQL 语句日志。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211725455.png)

也可以通过 xml 的形式，当批量修改消息的状态时，技术派是通过这种方式更新的。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211735384.png)

对应的mapper是这样写的：
```java
void updateNoticeRead(@Param("ids") List<Long> ids);
```


## 5.2、删除

技术派中的删除都是逻辑删除，不是物理删除，就是修改 delete 字段，而不是真的把记录从表里删除，所以，最终调用的还是 update 方法，比如说删除文章。
```java
/**  
 * 删除文章  
 *  
 * @param articleId  
 */  
@Override  
public void deleteArticle(Long articleId, Long loginUserId) {  
    ArticleDO dto = articleDao.getById(articleId);  
    if (dto != null && !Objects.equals(dto.getUserId(), loginUserId)) {  
        // 没有权限  
        throw ExceptionUtil.of(StatusEnum.FORBID_ERROR_MIXED, "请确认文章是否属于您!");  
    }  
  
    if (dto != null && dto.getDeleted() != YesOrNoEnum.YES.getCode()) {  
        dto.setDeleted(YesOrNoEnum.YES.getCode());  
        articleDao.updateById(dto);  
  
        // 发布文章删除事件  
        SpringUtil.publishEvent(new ArticleMsgEvent<>(this, ArticleEventEnum.DELETE, articleId));  
    }  
}
```

# 6、Mybatis-Plus主键策略

技术派中的主键目前采用的是自增策略，也就是说，数据库表的 ID 会设置为 Auto Increment。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211737806.png)

然后，实体类 DO 会继承 BaseDO，比如说分类 CategoryDO：
```java
@Data  
@EqualsAndHashCode(callSuper = true)  
@TableName("category")  
public class CategoryDO extends BaseDO {  
  
    private static final long serialVersionUID = 1L;  
  
    /**  
     * 类目名称  
     */  
    private String categoryName;  
  
    /**  
     * 状态：0-未发布，1-已发布  
     */  
    private Integer status;  
  
    /**  
     * 排序  
     */  
    @TableField("`rank`")  
    private Integer rank;  
  
    private Integer deleted;  
}
```

其中 BaseDO 为 MyBatis-Plus 提供的基类，内部的 id 字段已经添加了 @TableId(type = IdType.AUTO) 注解。
```java
@Data  
public class BaseDO implements Serializable {  
  
    @TableId(type = IdType.AUTO)  
    private Long id;  
  
    private Date createTime;  
  
    private Date updateTime;  
}
```

在插入数据时，无需设置主键值，数据库会自动分配主键值。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211739756.png)

除了 IdType.AUTO，MyBatis-Plus 还提供了其他几种策略，比如说 IdType.NONE：无主键策略。表示不使用任何主键生成策略，主键值需要手动设置。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211739091.png)

IdType.ID_WORKER：使用雪花算法生成分布式唯一 ID。插入数据时，MyBatis-Plus 会自动生成一个雪花 ID 作为主键值。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307211740799.png)


# 7、小结

简单总结一下，这篇文章我们主要讲了技术派中整合 MyBatis-Plus 的基本使用：
1. MyBatis-Plus 和 Spring Boot 的整合：我们介绍了如何在 Spring Boot 项目中引入 MyBatis-Plus 依赖，并配置数据源和 MyBatis-Plus 配置类。
2. MyBatis-Plus 的基本使用：我们讨论了如何创建实体类和 Mapper 接口，并在 Service 层和 Mapper 层中使用 MyBatis-Plus 提供的通用 CRUD 方法。
3. MyBatis-Plus 的查询方法：我们介绍了 MyBatis-Plus 提供的各种查询方法， 重点介绍了 MyBatis-Plus 的条件构造器（QueryWrapper 和 LambdaQueryWrapper）。
4. MyBatis-Plus 自定义 SQL：我们讲述了如何在 MyBatis-Plus 中使用自定义 SQL 语句，包括在 Mapper 接口中使用注解定义 SQL 和在 XML 文件中编写 SQL。
5. MyBatis-Plus 更新和删除：我们介绍了 MyBatis-Plus 提供的更新和逻辑删除方法。
6. MyBatis-Plus 主键策略：我们讨论了 MyBatis-Plus 支持的主键生成策略，如自增 ID、雪花算法等，以及如何使用 @TableId 注解配置主键策略。

掌握这些，你已经是一名称职的 MyBatis-Plus 的 CRUD boy 了。




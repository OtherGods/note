

对照：
- [14、技术派Cacheable注解实现缓存](2、相关技术/24、项目/1、技术派/1、基础篇/14、技术派Cacheable注解实现缓存.md)
- [17、技术派Caffeine整合本地缓存采坑实录](2、相关技术/24、项目/1、技术派/1、基础篇/17、技术派Caffeine整合本地缓存采坑实录.md)
- [1、SpringBoot缓存注解@Cacheable之自定义key策略及缓存失效时间指定](2、相关技术/16、常用框架-SpringBoot/补10_缓存注解@Cacheable等相关/1、SpringBoot缓存注解@Cacheable之自定义key策略及缓存失效时间指定.md)

Caffeine作为当下本地缓存的王者被大量的应用在实际的项目中，可以有效的提高服务的吞吐率、qps，降低rt。

在我们的【技术派】实战项目中，也同样使用了Caffeine作为本地缓存，用于缓存侧边栏这种变动相对不频繁的信息

主要借助 `Caffeine` + `@Cacheable` 来使用，如下
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310051432751.png)

接下来给各位小伙伴看一下，我们一般怎么在实际的项目中使用缓存

# 1、配置
## 1.1、依赖

pom.xml中添加相关依赖
```xml
<!--  caffeine 缓存使用姿势   -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
<!-- Spring的 @Cacheable 注解相关 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

## 1.2、缓存管理器

添加自定义的缓存管理器， 在 ForumCoreAutoConfig.java 配置类中添加
```java
/**  
 * 定义缓存管理器，配合Spring的 @Cache 来使用  
 *  
 * @return  
 */  
@Bean("caffeineCacheManager")  
public CacheManager cacheManager() {  
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();  
    cacheManager.setCaffeine(Caffeine.newBuilder().  
            // 设置过期时间，写入后五分钟过期  
                    expireAfterWrite(5, TimeUnit.MINUTES)  
            // 初始化缓存空间大小  
            .initialCapacity(100)  
            // 最大的缓存条数  
            .maximumSize(200)  
    );  
    return cacheManager;  
}
```

请注意，除了上面这种配置方式之外，还可以直接在 application.yml 文件中，通过配置的方式添加，如
```yml
# 指定全局默认的缓存策略
spring:
  cache:
    type: caffeine
    caffeine:
      spec: initialCapacity=10,maximumSize=200,expireAfterWrite=5m
```

那么问题来了，技术派为什么不采用配置文件这种方案呢？
- 知道的小伙伴，可以在评论区给出你们的看法

# 2、使用

配置完毕之后，接下来就可以进入愉快的使用环节了

## 2.1、开启缓存注解

第一步，需要在项目的启动入口，添加@EnableCaching注解，没有它的时候，后续的缓存注解都不会生效
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310051452001.png)

## 2.2、缓存使用实例

我们这里选择 SidebarService 来添加缓存，主要是出于缓存他的价值最高，可以有效减少db访问次数（因为侧边栏的数据，更多的是后台配置的，变动较少，大多数时，都不会有变化）

实际使用case如下
```java
/**  
 * 使用caffeine本地缓存，来处理侧边栏不怎么变动的消息  
 * <p>  
 * cacheNames -> 类似缓存前缀的概念  
 * key -> SpEL 表达式，可以从传参中获取，来构建缓存的key  
 * cacheManager -> 缓存管理器，如果全局只有一个时，可以省略  
 *  
 * @return  
 */  
@Override  
@Cacheable(key = "'homeSidebar'", cacheManager = "caffeineCacheManager", cacheNames = "home")  
public List<SideBarDTO> queryHomeSidebarList() {  
    List<SideBarDTO> list = new ArrayList<>();  
    list.add(noticeSideBar());  
    list.add(columnSideBar());  
    list.add(hotArticles());  
    return list;  
}

/**  
 * 查询教程的侧边栏信息  
 *  
 * @return  
 */  
@Override  
@Cacheable(key = "'columnSidebar'", cacheManager = "caffeineCacheManager", cacheNames = "column")  
public List<SideBarDTO> queryColumnSidebarList() {  
    List<SideBarDTO> list = new ArrayList<>();  
    list.add(subscribeSideBar());  
    return list;  
}

/**  
 * 以用户 + 文章维度进行缓存设置  
 *  
 * @param author    文章作者id  
 * @param articleId 文章id  
 * @return  
 */  
@Override  
@Cacheable(key = "'sideBar_' + #articleId", cacheManager = "caffeineCacheManager", cacheNames = "article")  
public List<SideBarDTO> queryArticleDetailSidebarList(Long author, Long articleId) {  
    List<SideBarDTO> list = new ArrayList<>(2);  
    // 不能直接使用 pdfSideBar()的方式调用，会导致缓存不生效 -- 动态代理原理，直接调用时调用的不是被代理后的方法
    list.add(SpringUtil.getBean(SidebarServiceImpl.class).pdfSideBar());  
    list.add(recommendByAuthor(author, articleId, PageParam.DEFAULT_PAGE_SIZE));  
    return list;  
}

/**  
 * PDF 优质资源  
 *  
 * @return  
 */  
@Cacheable(key = "'sideBar'", cacheManager = "caffeineCacheManager", cacheNames = "article")  
public SideBarDTO pdfSideBar() {  
    List<ConfigDTO> pdfList = configService.getConfigList(ConfigTypeEnum.PDF);  
    List<SideBarItemDTO> items = new ArrayList<>(pdfList.size());
    // ……
}
```

**注意事项1**
重点注意上面的缓存@Cacheable
- cacheManager: 指定的就是前面配置类中注册的缓存管理器（so，前面的问题答案是不是有了？）
- cacheNames: 可以简单理解为缓存的前缀，比如上面分别是首页侧边栏，专栏侧边栏，文章详情页侧边栏
- key: SpEL表达式，可以基于方法参数来生成对应的缓存key；若是常量字符串，用单引号包裹

> 这个缓存注解，表明是优先从缓存中获取，缓存没有则执行方法内逻辑，并将返回的结果写入缓存

**注意事项2**
其次需要注意的点是同一个service内部，若想要缓存注解生效，请不要直接内部调用，而是需要像上面的 `SpringUtil.getBean(xxx).xxx` 的方式来中转一下，走代理调用===【类似于同一个方法内部被@Transaction注解标注的方法相互调用会导致事务不生效一样，动态代理原理】===

---

上面是缓存使用的场景，我们还给出了一个主动失效的场景

在 `ArticleSettingServiceImpl` 中，主要是文章详情的侧边栏阅读计数变更，需要主动删除缓存
```java
@Override  
@CacheEvict(key = "'sideBar_' + #req.articleId", cacheManager = "caffeineCacheManager", cacheNames = "article")  
public void updateArticle(ArticlePostReq req) {
    ArticleDO article = articleDao.getById(req.getArticleId());
    // ……
}
```

重点是 `@CacheEvict` 注解，失效缓存，他是和前面的 `queryArticleDetailSidebarList(xxx)` 方法搭配使用的
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310051523814.png)

---

**拓展知识点：**
Spring的缓存注解，除了上面介绍的两个之外，还有两个也很常用
- `@CachePut`：方法执行完毕之后，主动将对应的结果写入缓存
- `@Caching`：可以组合多个缓存注解使用
实例如：
```java
@Caching(cacheable = @Cacheable(cacheNames = "caching", key = "#age")
        ,evict = @CacheEvict(cacheNames = "t4", key = "#age"))
public String caching(int age) {
    return "caching: " + age + "-->" + UUID.randomUUID().toString();
}
```

更多关于技术派中的@Cachable缓存注解使用姿势，请移步
- [14、技术派Cacheable注解实现缓存](2、相关技术/24、项目/1、技术派/1、基础篇/14、技术派Cacheable注解实现缓存.md)

## 2.3、小结
### 2.3.1、相关提交

上面集成接入Caffeine，可以在下面这个提交中看到具体的改动
- [feat: 添加Caffeine 缓存集成实例 · itwanger/paicoding@5a6b291](https://github.com/itwanger/paicoding/commit/5a6b2916d83d7f596252eb5a01da099e0d6e9d6d)
- [fix: 修复缓存使用姿势问题 · itwanger/paicoding@c6ddb83](https://github.com/itwanger/paicoding/commit/c6ddb837f902ee3950fc0352b90e5292f05a6228)

**重点同步**
上面这种缓存的使用方式，单纯的以缓存角度来看，没有什么问题，但是，在实际的项目中，有一个非常大的隐患，极容易踩坑，很有趣的是这个坑在技术派中就踩了，详情参考：
- [17、技术派Caffeine整合本地缓存采坑实录](2、相关技术/24、项目/1、技术派/1、基础篇/17、技术派Caffeine整合本地缓存采坑实录.md)

### 2.3.2、使用姿势小结
最后小结一下Caffenine结合SpringBoot的使用姿势，非常简单：
- 添加jar包依赖
- 注册缓存管理器Bean：CacheManager
- 启动类上，添加 `@EnableCaching` 注解
- 在需要缓存的方法上，添加 `@Cacheable`、`@CachePut`、`@CacheEvit`、`@Caching` 注解
- 然后就大功告成了

### 2.3.3、灵魂拷问

虽然说上面这篇文章看完之后，照着写一下Caffeine的缓存实例，应该问题不大，如果想更进一步挖掘一下这个知识点，那么不妨看一下下面几个问题
1. 对于 `@CacheEvict` 注解，若执行方法出现异常了，缓存会被失效吗？（即先删除缓存、再执行方法；还是先执行方法，再删除缓存？）
	- beforeInvocation=false,缓存的清除是否是在方法之前执行，默认false,即在方法之后清除，当方法执行出现异常时，缓存不会清除。
	- beforeInvocation=true,方法之前清除，无论方法执行是否出现异常，缓存都会清除
2. 一个方法上，能不能放多个缓存注解，比如 `@Cacheable` ，一个用于内部缓存，一个用作redis缓存，可行吗？
	- 如果可行，那先读取那个缓存结果呢？
		- **使用@Caching注解可以放入多个@Cacheable注解。读取顺序按照放入的顺序读取，若没有缓存则读取下一个。**
	- 如果不可行，那有办法支持么？
3. 缓存注解有个sync属性，那么是不是支持缓存异步写入的功能呢？可以怎么测试验证一下呢？
4. 现在所有的缓存公用一个缓存管理器，能否不共用呢？比如我不同类型的缓存对象，缓存时间不一致，可以怎么处理呢？
	1. 指定CacheManager即可不共用。
	2. 不同缓存对象缓存时间设置不同的方式：
		1. 使用继承，在RedisCacheManager中拦截Cache的创建方法。个人感觉比较复杂。当然最简单的是：创建多个CacheManager来满足不同的过期时间。
		2. 参考文章：[springboot缓存@Cacheable的使用，及设置过期时间](https://blog.csdn.net/lpping90/article/details/117522239)
5. 上面的实例是结合了SpringBoot，优势是切其他具体的缓存实现简单；但是从上面好像体验不到Caffeine的魅力，那么直接使用caffeine可以怎么做呢？

### 2.3.4、扩展阅读
- [20、缓存注解@Cacheable @CacheEvit @CachePut使用姿势介绍](2、相关技术/5、数据库-Redis/一灰/20、缓存注解@Cacheable%20@CacheEvit%20@CachePut使用姿势介绍.md)
- [21、SpringBoot缓存注解@Cacheable之自定义key策略及缓存失效时间指定](2、相关技术/5、数据库-Redis/一灰/21、SpringBoot缓存注解@Cacheable之自定义key策略及缓存失效时间指定.md)



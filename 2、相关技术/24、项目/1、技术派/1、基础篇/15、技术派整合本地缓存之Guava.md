今天由我来给大家讲一下技术派是如何整合本地缓存 Guava 的。要知道，互联网应用一般都比较喜欢追求高并发，高可用，因此，缓存的地位可以说是举足轻重，对程序的性能提升有很大的帮助。

技术派是一个 Spring Boot 项目，那 Spring Boot 也提供了多种技术方案来支持缓存，常见的有以下几种：
1. **Redis**：基于网络的分布式缓存，可以支持数据持久化和分布式部署，适用于海量数据的缓存，同时也提供了复杂数据类型和丰富的缓存策略，比如支持LRU（Least Recently Used，优先淘汰最近最少使用的数据）、LFU（Least Frequently Used，优先淘汰最不常用的缓存数据）等淘汰算法。但是由于Redis是基于网络通信的，相比本地缓存会有一定的网络延迟
2. **Guava Cache**：也就是本篇的主角，一个轻量级的本地缓存库，提供了多种缓存策略，包括基于大小、时间和引用的回收策略。数据存储在应用程序的内存中，因此读写速度很快，适用于高并发、读多写少的场景
3. **Caffeine**：一个基于Java8的高性能本地缓存库，可以作为Guava缓存的替代品使用

在技术派中，这三种缓存方案都涉及到了，我们也会展开来讲，大家放心，包教包会，是我们的职责。

# 1、关于Guava

Guava是Google开源的一款Java工具库，提供了一些JDK没有或者增江JDK的功能，比如说：
- com.google.common.collect: 集合工具包，提供了许多 JDK 中没有的集合类型和集合操作方法。
- com.google.common.io: I/O 工具包，提供了许多实用的 I/O 操作类和工具方法。
- com.google.common.cache: 缓存工具包，提供了一个高性能的本地缓存实现。
- com.google.common.math: 数学工具包，提供了许多数学计算和运算的实用方法。
- com.google.common.eventbus: 事件总线工具包，提供了基于观察者模式的事件发布和订阅功能。
- com.google.common.reflect: 反射工具包，提供了更好用的反射 API。
- com.google.common.util.concurrent: 并发工具包，提供了许多 JDK 并发包中没有的并发实用工具类和工具方法。
可以看得出，Guava 非常的强大，Cache只是其中的一部分功能而已。

那如何在 Spring Boot 应用中整合 Guava Cache 呢？早期版本有两种方式，一种是使用 GuavaCacheManager，一种是使用 CacheBuilder。

# 2、使用GuavaCacheManager

在 Spring Boot 早期版本中，集成 Guava Cache 非常的简单，因为 Spring 定义了 CacheManager 接口来统一不同的缓存技术，比如说 Guava、 Redis、Caffeine。
- GuavaCacheManager：使用 Guava 作为缓存技术
- RedisCacheManager：使用 Redis 作为缓存技术
- CaffeineCacheManager：使用 Caffeine 作为缓存技术
- ConcurrentMapCacheManager：Spring Boot 默认的缓存实现

我们重点来说说 GuavaCacheManager，它提供了一种基于注解的缓存管理方式，通过在方法上加上 @Cacheable、@CachePut、@CacheEvict  等注解，实现对方法返回结果的缓存。
- @Cacheable: 在方法执行前，Spring 会检查缓存中是否已存在相同 key 的缓存数据，如果存在，直接返回缓存数据，如果不存在，则执行方法并将返回结果缓存起来。
- @CachePut: 无论缓存中是否已存在相同 key 的缓存数据，都会执行方法并将返回结果缓存起来，用于更新缓存数据。
- @CacheEvict: 用于删除指定 key 的缓存数据。

不过，Spring Boot 2.7.1 版本已经把 GuavaCacheManager 移除了，取而代之的是 CaffeineCacheManager。见下图，在 Spring 中已经找不到 GuavaCacheManager 的身影了（技术派用的 Spring Boot 是 2.7.1 版本，具体是哪个 Spring Boot 版本移除的，这里不再考究）。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406032314810.png)

总之是，如果我们还想用 Guava Cache 做缓存，就需要采用 CacheBuilder 的形式，接下来，我们来看看到底怎么用，少说废话多做事（😂）

# 3、使用CacheBuilder
## 3.1、在pom.xml中添加依赖

技术派是添加在 paicoding-core 中的 pom.xml 文件中。
```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
</dependency>
```

## 3.2、来个demo

```java
// 创建一个 CacheBuilder 对象  
CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
        .maximumSize(100)  // 最大缓存条目数  
        .expireAfterAccess(30, TimeUnit.MINUTES) // 缓存项在指定时间内没有被访问就过期  
        .recordStats();  // 开启统计功能  
  
// 构建一个 LoadingCache 对象  
LoadingCache<String, String> cache = cacheBuilder.build(new CacheLoader<String, String>() {  
    @Override  
    public String load(String key) throws Exception {  
        return "value：" + key; // 当缓存中没有值时，加载对应的值并返回  
    }  
});  
  
// 存入缓存  
cache.put("itwanger", "沉默王二");  
  
// 从缓存中获取值  
// put 过  
System.out.println(cache.get("itwanger"));  
// 没 put 过  
System.out.println(cache.get("chenqingyang"));  
  
// 打印缓存的命中率等统计信息  
System.out.println(cache.stats());
```

### 3.2.1、CacheBuilder

[**CacheBuilder**](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/CacheBuilder.html) 是 Guava Cache中用于构建本地缓存的构造器，通过CacheBuilder可以配置缓存的各种属性，如最大缓存项数量、缓存项过期时间、缓存项移除通知等。  

下面介绍几个常用的 CacheBuilder 方法：

**①、[newBuilder()](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/CacheBuilder.html#newBuilder())**

该方法返回一个 CacheBuildeir 实例，用于创建一个新的缓存实例。

**②、[maximumSize(long maximumSize)](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/CacheBuilder.html#maximumSize(long))**

该方法用于设置缓存的最大大小，以条目数为单位。如果缓存中的条目数超过了最大大小，则可能会触发缓存的回收策略，以释放一些缓存空间。

**③、[expireAfterWrite(long duration, TimeUnit unit)](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/CacheBuilder.html#expireAfterWrite(long,%20java.util.concurrent.TimeUnit))**

该方法用于设置缓存的过期时间。在缓存中存储的每个条目被创建或者更新后，经过指定的时间后，该条目将被自动删除。可以使用 TimeUnit 枚举类型中的常量来指定时间单位，比如 TimeUnit.SECONDS、TimeUnit.MINUTES 等。

**④、[expireAfterAccess(long duration, TimeUnit unit)](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/CacheBuilder.html#expireAfterAccess(long,%20java.util.concurrent.TimeUnit))**

该方法和 expireAfterWrite 方法类似，不同的是，该方法用于设置缓存中每个条目的最大闲置时间。如果一个条目在指定的时间内没有被访问，则该条目将被自动删除。

**⑤、[recordStats()](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/CacheBuilder.html#recordStats())**

该方法用于启用缓存统计功能，可以用于监控缓存的使用情况。

**⑥、[build()](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/CacheBuilder.html#build())**

该方法用于创建和返回一个新的缓存实例。在调用该方法之前，需要进行一些其他的配置，比如设置缓存的最大大小、过期时间等。

### 3.2.2、LoadingCache

[**LoadingCache**](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/LoadingCache.html) 是一种特殊的 Cache，在缓存中不存在某个 key 的值时，可以通过 CacheLoader 来加载该 key 对应的值，并将其加入到缓存中。

`com.google.common.cache.LoadingCache` 是 Google Guava 库中用于缓存数据的一种接口。它继承自 `Cache` 接口，并添加了在缓存未命中时自动加载新值的功能。`LoadingCache` 通常用于实现简单的缓存机制，可以自动填充数据，避免缓存未命中时手动处理加载逻辑。

`LoadingCache` 主要特性

- **自动加载**：当缓存中不存在请求的值时，`LoadingCache` 会自动调用用户提供的加载函数来加载值。
- **线程安全**：`LoadingCache` 是线程安全的，可以在多个线程中并发使用。
- **缓存回收**：支持基于时间、引用和大小的缓存回收策略。
- **统计信息**：可以启用统计信息来监视缓存的性能。

`LoadingCache` 的常用方法
- [get(K key)](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/LoadingCache.html#get(K))：获取指定 key 对应的缓存值。如果缓存中不存在该 key 对应的值，则调用 CacheLoader 中的 load 方法来加载缓存值。如果 load 方法返回 null，则 get 方法也会返回 null。如果 load 方法抛出检查形异常，则 get 方法会将异常转换为 ExecutionException，并将其抛出。
- [put(K key, V value)](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/Cache.html#put(K,%20V))：将指定 key 和 value 存入缓存中。如果之前缓存中已经存在该 key 对应的值，则会覆盖之前的缓存值。如果 value 为 null，则 put 方法会抛出 NullPointerException 异常。
- [getUnchecked](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/LoadingCache.html#getUnchecked(K))：类比get；如果 load 方法抛出检查形异常，则此方法会将异常转换为 RuntimeException。
- [getIfPresent](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/Cache.html#getIfPresent(java.lang.Object))：继承自父类，返回与此缓存中的key关联的值，如果键没有缓存值，则返回null中的值。
- [invalidate(K key)](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/Cache.html#invalidate(java.lang.Object))：使指定 key 对应的缓存值失效，并从缓存中移除该 key 对应的值。
- [invalidateAll()](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/Cache.html#invalidateAll())：使所有缓存值失效，并从缓存中移除所有缓存值。
- [size()](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/Cache.html#size())：返回缓存中当前存储的 key-value 对数量。
- [stats()](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/Cache.html#stats())：返回缓存的统计信息，包括缓存命中率、缓存加载成功率、缓存加载平均时间等。
- [ConcurrentMap<K, V> asMap()](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/Cache.html#asMap())：返回一个线程安全的 ConcurrentMap。

### 3.2.3、CacheLoader

[**CacheLoader**](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/CacheLoader.html) 是 Guava 缓存库中的一个接口，用于在缓存未命中时加载缓存值。它定义了一个方法 load(K key)，当缓存中没有 key 对应的值时，会调用该方法来获取该 key 对应的值并将其存入缓存中。

- [load](https://guava.dev/releases/18.0/api/docs/com/google/common/cache/CacheLoader.html#load(K)) 方法中可以实现从数据源中加载缓存值的逻辑。
- load 方法中可以抛出异常以表示加载失败的情况，例如数据源访问异常等。
- 当使用 LoadingCache 时，load 方法中的实现应该是幂等的，即多次调用应该返回相同的结果。

在上面的示例中，我们在缓存中放了一个键值对（itwanger，沉默王二），然后通过 get 方法从缓存中取出，key 为 paicoding 这个在缓存中没有值，所以会通过 CacheLoader 加载一个值并返回。

最后打印缓存的统计信息。来看一下结果。
```java
沉默王二
技术派 value：paicoding
CacheStats{hitCount=1, missCount=1, loadSuccessCount=1, loadExceptionCount=0, totalLoadTime=2132781, evictionCount=0}
```

符合我们的预期。

key 为itwanger的有值，key 为 paicoding 的没有值，所以命中数（hitCount）为 1，miss 数（missCount）为 1。

## 3.3、技术派中Guava Cache

目前技术派一共有四处在用 Guava Cache，我来给大家完整的讲一下，并且会对应上具体的业务场景，方便大家去理解。

### 3.3.1、第一处为获取分类(CategoryServiceImpl类中)

避免每次从DB查询。代码逻辑非常简单，给分类加一个缓存，key 为 categoryId，如果缓存中不存在，则去 DB 中查找，最后返回一个由分类 ID、分类名、分类排序的对象。

对应的业务，比如说首页展示分类的时候。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406052133882.png)

CategoryServiceImpl 类中主要用到了 Guava Cache 的 CacheBuilder、CacheLoader、LoadingCache，前面已经讲过了，这里不再赘述，来看源码。

①、通过 CacheBuilder.newBuilder来构建LoadingCache对象。
```java
private LoadingCache<Long, CategoryDTO> categoryCaches;

@PostConstruct  
public void init() {  
    categoryCaches = CacheBuilder.newBuilder().maximumSize(300).build(new CacheLoader<Long, CategoryDTO>() {  
        @Override  
        public CategoryDTO load(@NotNull Long categoryId) throws Exception {  
            CategoryDO category = categoryDao.getById(categoryId);  
            if (category == null || category.getDeleted() == YesOrNoEnum.YES.getCode()) {  
                return CategoryDTO.EMPTY;  
            }  
            return new CategoryDTO(categoryId, category.getCategoryName(), category.getRank());  
        }  
    });  
}
```

②、查询所有分类的时候会从缓存中取。
```java
/**  
 * 查询所有的分类  
 *  
 * @return */@Override  
public List<CategoryDTO> loadAllCategories() {  
    //如果缓存中的分类数小于等于5，则刷新缓存  
    if (categoryCaches.size() <= 5) {  
        refreshCache();  
    }  
    List<CategoryDTO> list = new ArrayList<>(categoryCaches.asMap().values());  
    list.removeIf(s -> s.getCategoryId() <= 0);  
    list.sort(Comparator.comparingInt(CategoryDTO::getRank));  
    return list;  
}
```

③、刷新缓存，从 DB 中获取分类，然后清空缓存并按照分类 ID 放入缓存中：
```java
/**  
 * 刷新缓存  
 */  
@Override  
public void refreshCache() {  
    // 从数据库中获取所有分类DO对象  
    List<CategoryDO> list = categoryDao.listAllCategoriesFromDb();  
    // 清空缓存  
    categoryCaches.invalidateAll();  
    categoryCaches.cleanUp();  
    // 将分类DO对象转换为DTO对象，并存入缓存  
    list.forEach(s -> categoryCaches.put(s.getId(), ArticleConverter.toDto(s)));  
}
```

### 3.3.2、第二处为UserSessionHelper类

①、生成验证码的genVerifyCode方法

key 为验证码，value 为用户 ID，当生成验证码的时候，将验证码和用户 ID 放入缓存。

对应的业务是，当我们在技术派团队的楼仔处登录时会根据微信用户生成验证码。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406052146798.png)

```java
public String genVerifyCode(Long userId) {
    int cnt = 0;
    while (true) {
        // 生成验证码
        String code = CodeGenerateUtil.genCode(cnt++);
        // 检查缓存中是否已经存在该验证码对应的用户 ID
        if (codeUserIdCache.getIfPresent(code) != null) {
            // 如果存在，则继续生成下一个验证码
            continue;
        }
        // 如果不存在，则将验证码和对应的用户 ID 存入缓存中
        codeUserIdCache.put(code, userId);
        // 返回生成的验证码
        return code;
    }
}
```

具体来说，代码首先定义了一个计数器 cnt，并进入一个 while 循环。在循环中，通过调用 CodeGenerateUtil.genCode(cnt++) 方法生成一个验证码 code（比如说 666、888 等等，便于用户输入）。
```java
private static final List<String> specialCodes = Arrays.asList(  
        "666", "888", "000", "999", "555", "222", "333", "777",  
        "520", "911",  
        "234", "345", "456", "567", "678", "789"  
);
```

然后，代码通过调用 codeUserIdCache.getIfPresent(code) 方法来检查缓存中是否已经存在该验证码对应的用户 ID，如果存在则继续生成下一个验证码，直到生成一个不存在于缓存中的验证码。

接下来，代码调用 codeUserIdCache.put(code, userId) 将验证码和对应的用户 ID 存入缓存中。最后，代码返回生成的验证码。

②、codeUserIdCache 的初始化如下：

```java
@PostConstruct // 注明该方法在Bean初始化之后执行
public void init() {
    // 创建缓存实例，最多支持300个用户登录，缓存时长为5分钟
    // 注意：当服务部署在多台机器上时，基于本地缓存会有问题，建议使用Redis或Memcache等分布式缓存
    codeUserIdCache = CacheBuilder.newBuilder().maximumSize(300).expireAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<String, Long>() {
        @Override
        public Long load(String s) throws Exception {
            // 如果缓存未命中，则抛出异常，提示缓存未命中
            throw new NoVlaInGuavaException("not hit!");
        }
	});
}
```

③、根据 code 从缓存中获取用户 ID：

```java
public Long getUserIdByCode(String code) {
    return codeUserIdCache.getIfPresent(code);
}
```

④、如果验证码验证成功了，就从缓存中移除：

```java
public String codeVerifySucceed(String code, Long userId) {
    // 生成一个随机的会话 ID
    String session = "s-" + UUID.randomUUID();
    // 将会话 ID 和用户 ID 存入 Redis 缓存中，并设置过期时间为 SESSION_EXPIRE_TIME
    RedisClient.setStrWithExpire(session, String.valueOf(userId), SESSION_EXPIRE_TIME);
    // 将验证码从 codeUserIdCache 缓存中移除，避免重复使用
    codeUserIdCache.invalidate(code);
    // 返回生成的会话 ID
    return session;
}
```

### 3.3.3、第三处为QrLoginHelper类

参考：
1. [5、技术派中的微信公众号自动登录方案](2、相关技术/24、项目/1、技术派/2、进阶篇/5、技术派中的微信公众号自动登录方案.md)
2. [6、技术派之扫码登录实现原理](2、相关技术/24、项目/1、技术派/2、进阶篇/6、技术派之扫码登录实现原理.md)

记录用户登录的时候，验证码和用户设备的绑定关系，用于微信公众号输入验证码实现扫码登录。

对应的业务就是微信扫码登录。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406052201185.png)

①、通过 CacheBuilder.newBuilder构建验证码消息留的缓存对象，以及设备与验证码的LoadingCache对象。

```java
/**  
 * sse的超时时间，默认15min  
 */private final static Long SSE_EXPIRE_TIME = 15 * 60 * 1000L;  
private final LoginService sessionService;  
/**  
 * key = 验证码, value = 长连接  
 */  
private LoadingCache<String, SseEmitter> verifyCodeCache;  
/**  
 * key = 设备 value = 验证码  
 */  
private LoadingCache<String, String> deviceCodeCache;  
  
public WxLoginHelper(LoginService loginService) {  
    this.sessionService = loginService;  
    // 创建验证码缓存实例，最多支持300个验证码，缓存时长为5分钟  
    verifyCodeCache = CacheBuilder.newBuilder().maximumSize(300).expireAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<String, SseEmitter>() {  
        @Override  
        public SseEmitter load(String s) throws Exception {  
            // 如果缓存未命中，则抛出异常，提示缓存未命中  
            throw new NoVlaInGuavaException("no val: " + s);  
        }  
    });  
      
    // 创建设备码缓存实例，最多支持300个设备码，缓存时长为5分钟  
    deviceCodeCache = CacheBuilder.newBuilder().maximumSize(300).expireAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {  
        @Override  
        public String load(String s) {  
            int cnt = 0;  
            // 生成随机设备码，直到不与已有的验证码重复为止  
            while (true) {  
                String code = CodeGenerateUtil.genCode(cnt++);  
                if (!verifyCodeCache.asMap().containsKey(code)) {  
                    return code;  
                }  
            }  
        }  
    });  
}
```

②、扫码登录前获取设备验证码。

```java
/**
 * 加一层设备id，主要目的就是为了避免不断刷新页面时，不断的往 verifyCodeCache 中塞入新的kv对
 * 其次就是确保五分钟内，不管刷新多少次，验证码都一样
 *
 * @param request
 * @param response
 * @return
 */
 public String genVerifyCode(HttpServletRequest request, HttpServletResponse response) {
    String deviceId = initDeviceId(request, response);
    String code = deviceCodeCache.getUnchecked(deviceId);
    SseEmitter lastSse = verifyCodeCache.getIfPresent(code);
    if (lastSse != null) {
        // 这个设备之前已经建立了连接，则移除旧的，重新再建立一个; 通常是不断刷新登录页面，会出现这个场景
        lastSse.complete();
        verifyCodeCache.invalidate(code);
    }
    return code;
}

/**  
 * 初始化设备id  
 * * @param request  
 * @param response  
 * @return  
 */public String initDeviceId(HttpServletRequest request, HttpServletResponse response) {  
    String deviceId = null;  
    Cookie[] cookies = request.getCookies();  
    if (cookies != null) {  
        for (Cookie cookie : request.getCookies()) {  
            if (LoginOutService.USER_DEVICE_KEY.equalsIgnoreCase(cookie.getName())) {  
                deviceId = cookie.getValue();  
                break;  
            }  
        }  
    }  
    if (deviceId == null) {  
        deviceId = UUID.randomUUID().toString();  
        response.addCookie(new Cookie(LoginOutService.USER_DEVICE_KEY, deviceId));  
    }  
    return deviceId;  
}
```

首先，调用 initDeviceId 方法获取设备 ID，并使用该设备 ID 从缓存 deviceCodeCache 中获取设备码 code。如果缓存中不存在该设备 ID 对应的设备码，则调用 CacheLoader 对象的 load 方法生成一个新的设备码，同时将该值加载到缓存中。

接着，使用 verifyCodeCache.getIfPresent(code) 方法从缓存 verifyCodeCache 中获取该设备码对应的 SseEmitter 实例 lastSse。

**SseEmitter 是 Spring 框架中的一个类，用于在 Web 应用程序中向客户端发送事件流。** 在此处，SseEmitter 主要用于向客户端发送验证码。参考：[2、ResponseBodyEmitter类](2、相关技术/25、源码/Spring框架/1、常用类、接口、方法…….md#2、ResponseBodyEmitter类)。

如果 lastSse 不为 null，则说明该设备 ID 之前已经建立了连接（比如：我扫描了二维码后，又刷新了页面，这时候我把我刚才记住的验证码发给公众号），此时需要将旧的 SseEmitter 实例 lastSse 移除，并将其 invalidate，以确保验证码只会被新的客户端接收。然后，调用 SseEmitter 的 complete 方法关闭当前连接，以便客户端可以重新建立新的连接。

最后，将生成的验证码返回。

③、刷新缓存中的设备验证码：

```java
/**  
 * 刷新验证码  
 *  
 * @param request  
 * @param response  
 * @return  
 * @throws IOException  
 */  
public String refreshCode(HttpServletRequest request, HttpServletResponse response) throws IOException {  
    String deviceId = initDeviceId(request, response);  
    // 获取旧的验证码，注意不使用 getUnchecked, 避免重新生成一个验证码  
    String oldCode = deviceCodeCache.getIfPresent(deviceId);  
    SseEmitter lastSse = oldCode == null ? null : verifyCodeCache.getIfPresent(oldCode);  
    if (lastSse == null) {  
        log.info("last deviceId:{}, code:{}, sse closed!", deviceId, oldCode);  
        return null;  
    }  
  
    // 重新生成一个验证码  
    deviceCodeCache.invalidate(deviceId);  
    String newCode = deviceCodeCache.getUnchecked(deviceId);  
    log.info("generate new loginCode! deviceId:{}, oldCode:{}, code:{}", deviceId, oldCode, newCode);  
  
    lastSse.send("updateCode!");  
    lastSse.send("refresh#" + newCode);  
    verifyCodeCache.invalidate(oldCode);  
    verifyCodeCache.put(newCode, lastSse);  
    return newCode;  
}
```

首先，调用 initDeviceId 方法获取设备 ID，并使用该设备 ID 从缓存 deviceCodeCache 中获取旧的验证码 oldCode，然后使用 verifyCodeCache.getIfPresent(oldCode) 方法从缓存 verifyCodeCache 中获取该验证码对应的 SseEmitter 实例 lastSse。

如果 lastSse 为 null，则说明该验证码对应的 SseEmitter 实例已经被关闭，此时直接返回 null。否则，说明该验证码对应的 SseEmitter 实例仍然在使用中，需要进行刷新操作。

接着，使用 deviceCodeCache.invalidate(deviceId) 方法使缓存中的旧设备码失效，并重新生成一个新的设备码 newCode。然后，使用 SseEmitter 的 send 方法向客户端发送消息，以通知客户端刷新验证码。在此处，使用了两个不同的消息："updateCode!" 和 "refresh#" + newCode。其中，"updateCode!" 用于通知客户端更新验证码，"refresh#" + newCode 用于传递新的验证码。

最后，使用 verifyCodeCache.invalidate(oldCode) 方法使旧的验证码失效，并使用 verifyCodeCache.put(newCode, lastSse) 方法将新的验证码和 SseEmitter 实例 lastSse 存储在缓存 verifyCodeCache 中。最后，返回新生成的验证码 newCode。

④、根据设备码在缓存中放入消息推送流：

```java
/**  
 * 保持与前端的长连接  
 * <p>  
 * 直接根据设备拿之前初始化的验证码，不直接使用传过来的code  
 * * @param code  
 * @return  
 */public SseEmitter subscribe(String code) throws IOException {  
    HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();  
    HttpServletResponse res = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();  
    String device = initDeviceId(req, res);  
    String realCode = deviceCodeCache.getUnchecked(device);  
  
    // fixme 设置15min的超时时间, 超时时间一旦设置不能修改；因此导致刷新验证码并不会增加连接的有效期  
    SseEmitter sseEmitter = new SseEmitter(15 * 60 * 1000L);  
    verifyCodeCache.put(code, sseEmitter);  
    sseEmitter.onTimeout(() -> verifyCodeCache.invalidate(realCode));  
    sseEmitter.onError((e) -> verifyCodeCache.invalidate(realCode));  
    if (!Objects.equals(realCode, code)) {  
        // 若实际的验证码与前端显示的不同，则通知前端更新  
        sseEmitter.send("initCode!");  
        sseEmitter.send("init#" + realCode);  
    }  
    return sseEmitter;  
}
```

首先，获取 HttpServletRequest 和 HttpServletResponse 对象，并使用 initDeviceId 方法获取设备 ID 和设备码 reaCode。注意，在此处不直接使用传过来的验证码 code，而是使用设备 ID 从缓存 deviceCodeCache 中获取设备码 reaCode。

接着，创建一个 SseEmitter 对象 sseEmitter，并将其存储在缓存 verifyCodeCache 中，以便后续可以向客户端发送事件流。

然后，为 sseEmitter 设置超时和错误的回调函数。在超时或发生错误时，将缓存中对应的验证码失效。

如果实际的验证码 reaCode 与前端显示的不同，则使用 sseEmitter 的 send 方法向客户端发送消息，以通知客户端更新验证码。在此处，使用了两个不同的消息："initCode!" 和 "init#" + reaCode。其中，"initCode!" 用于通知客户端初始化验证码，"init#" + reaCode 用于传递实际的验证码 reaCode。

最后，返回 sseEmitter 对象。

⑤、扫描登录二维码时，根据设备码从缓存中取出消息推送流：
```java
/**  
 * 二维码已扫描  
 *  
 * @param code  
 * @throws IOException  
 */  
public void scan(String code) throws IOException {  
    SseEmitter sseEmitter = verifyCodeCache.getIfPresent(code);  
    if (sseEmitter != null) {  
        sseEmitter.send("scan");  
    }  
}
```

⑥、微信公众号登录时根据设备码从缓存中取出消息推送流：
```java
public boolean login(String loginCode, String verifyCode) {  
    String session = sessionService.register(verifyCode);  
    SseEmitter sseEmitter = verifyCodeCache.getIfPresent(loginCode);  
    if (sseEmitter != null) {  
        try {  
            // 登录成功，写入session  
            sseEmitter.send(session);  
            // 设置cookie的路径  
            sseEmitter.send("login#" + LoginOutService.SESSION_KEY + "=" + session + ";path=/;");  
            return true;  
        } catch (Exception e) {  
            log.error("登录异常: {}, {}", loginCode, verifyCode, e);  
        } finally {  
            sseEmitter.complete();  
            verifyCodeCache.invalidate(loginCode);  
        }  
    }  
    return false;  
}
```

首先，调用 sessionService 的 login 方法进行验证码校验，并获取返回的 session 值。

然后，使用 loginCode 从缓存 verifyCodeCache 中获取对应的 SseEmitter 实例 sseEmitter。

如果 sseEmitter 不为 null，则说明该 SseEmitter 实例仍然在使用中，可以将登录结果返回给客户端。在此处，使用 sseEmitter 的 send 方法向客户端发送消息，以通知客户端登录结果和设置 cookie。

最后，使用 sseEmitter 的 complete 方法关闭该 SseEmitter 实例，并使用 verifyCodeCache.invalidate(loginCode) 方法使该验证码失效。

如果 sseEmitter 为 null，则说明该 SseEmitter 实例已经被关闭，此时直接返回 false，表示登录失败。


#### 3.3.3.1、整体流程扫码登录流程

1. 用户先点击登录
	![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406092219580.png)
   前端JS异步向后端发送获取验证码的请求，后端调用方法获得验证码返给前端
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406092301660.png)
2. 前端获取到验证码后，再发送异步请求(同时将验证发送给后端)，如果验证码没失效或者还存在，后端会为这个验证码建立长连接并返回给前端
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406092307508.png)

3. 用户用微信扫描二维码(二维码的内容是我自己的微信公众号地址)跳转到微信公众号，，在公众号输入验证码后向后端发送请求调用 `wx/callback`
   之所以会调用 `wx/callback` 是因为在公众号平台配置了回调地址
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406291814934.png)

   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406092324710.png)

这是后台打印的流程日志：
1. 用户在前端点击登录，JS发送获取code和长连接请求
2. 用户扫描二维码回调微信回调接口
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406142313228.png)


微信公众号扫码登录流程
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406092217996.png)

### 3.3.4、第四处为ImageServiceImpl类

对应的业务就是当我们在编辑文章的时候，如果图片不是技术派网站的，就会转链，防止外网图片限流导致图片不可用。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406172246222.png)

PS：不过自己掏钱买图片 OSS 和 CDN 还是蛮贵的，（😭）

缓存图片转存的结果，避免出现一个外网图片不断转存的情况。
```java
/**  
 * 外网图片转存缓存  
 */  
private LoadingCache<String, String> imgReplaceCache = CacheBuilder.newBuilder().maximumSize(300).expireAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {  
    @Override  
    public String load(String img) {  
        try {  
	        // 把图片从网上下载本地的InputStream中，可以将这个流中的内容写出到本地文件查看(借助方法download)
            InputStream stream = FileReadUtil.getStreamByFileName(img);  
            URI uri = URI.create(img);  
            String path = uri.getPath();  
            int index = path.lastIndexOf(".");  
            String fileType = null;  
            if (index > 0) {  
                // 从url中获取文件类型  
                fileType = path.substring(index + 1);  
            }  
            return imageUploader.upload(stream, fileType);  
        } catch (Exception e) {  
            log.error("外网图片转存异常! img:{}", img, e);  
            return "";  
        }  
    }  
});


import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

// 将文件从内存写到本地
public static void download() {
	// 示例字节数组数据
	byte[] data = "这是一个示例".getBytes();
	
	// 创建ByteArrayInputStream
	ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
	
	// 目标文件路径
	String filePath = "output.txt";
	
	// 将ByteArrayInputStream中的内容写入指定文件
	try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
		byte[] buffer = new byte[1024];
		int bytesRead;
		
		// 读取ByteArrayInputStream中的内容并写入文件
		while ((bytesRead = byteArrayInputStream.read(buffer)) != -1) {
			fileOutputStream.write(buffer, 0, bytesRead);
		}
		
		System.out.println("文件写入成功：" + filePath);
	} catch (IOException e) {
		System.err.println("文件写入失败：" + e.getMessage());
	}
}

```

# 4、小结

使用缓存可以避免重复计算，减少系统资源的消耗，提高程序的响应速度。而Guava Cache 作为一款轻量级的缓存实现，使用起来足够的轻便，希望大家能结合技术派的源码，把这部分知识吃透掉。




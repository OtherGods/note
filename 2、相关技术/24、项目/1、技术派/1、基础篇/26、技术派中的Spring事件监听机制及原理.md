
大家好，我是二哥呀。今天由我来给大家讲一下《技术派中的 Spring 事件监听机制及原理》。Spring 事件监听机制是 Spring 框架中一种重要的技术，它允许在组件之间进行松耦合的通信。通过使用事件监听机制，应用程序的各个组件可以在不直接引用其他组件的情况下，相互发送和接收消息。

在技术派中，当发布文章或下线文章时，会发布一个事件给SiteMap（站点地图，帮助搜索引擎更加有效的抓取和索引网站），SiteMap监听到该事件后会进行更新。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308262208560.png)

# 相关组件

Spring事件监听机制的本质是观察者模式的使用，包括事件、事件监听器、事件发布器等主要组件。
1. **事件（Event）**：一个**实现了 `ApplicationEvent` 类**的对象，代表了应用程序中某个特定事件。我们可以根据需要创建自定义事件，只要继承 `ApplicationEvent` 类并添加相关的属性和方法就可以了。
2. **事件监听器（Event Listener）**：**实现了 `ApplicationListener<E>` 接口**的对象，其中E表示监听器需要处理的事件类型。监听器可以通过 `onApplicationEvent(E event)` 方法处理接收到的事件。另外，**也可以使用 `@EventListener` 注解**简化事件监听器的实现，技术派采用的正是这种方式。
3. **事件发布器（Event Publisher）**：事件发布器负责将事件发布给所有关注该事件的监听器。
	1. 在Spring中，**`ApplicationEventPublisher` 接口**定义了事件发布的基本功能
	2. `ApplicationEventPublisherAwre` 接口允许组件获取到事件发布器的引用。
   ==**Spring的核心容器ApplicationContext实现了ApplicationEventPublisher接口**，因此在Spring应用中，通常直接使用ApplicationContext作为事件发布器==，技术派正是采用的这种方式。

# 1、实例

## 1.1、第一步：定义事件

创建自定义事件 ArticleMsgEvent，继承 ApplicationEvent。
```java
@Getter  
@Setter  
@ToString  
@EqualsAndHashCode(callSuper = true)  
public class ArticleMsgEvent<T> extends ApplicationEvent {  
  
    private ArticleEventEnum type;  

    private T content;  

    public ArticleMsgEvent(Object source, ArticleEventEnum type, T content) {  
        super(source);  
        this.type = type;  
        this.content = content;  
    }  
}
```

这个类中有两个字段，一个ArticleEventEnum类型的枚举，代表事件的类型（是文章上线还是下线），一个泛型content，代表事件的内容，在本例中，我们会传一个文章的ID。

构造方法中有一个source表示事件的来源，也就是事件的发布者

ApplicationEvent 是 Spring Framework 中用于定义事件的基类。

## 1.2、第二步：【发布器】定义 SpringUtil 工具类，实现 ApplicationContextAware

```java
@Component
public class SpringUtil implements ApplicationContextAware {
    private volatile static ApplicationContext context;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtil.context = applicationContext;
    }
    
    /**
     * 发布事件消息
     *
     * @param event
     */
     public static void publishEvent(ApplicationEvent event) {
        context.publishEvent(event);
    }
}
```

通过实现ApplicationContextAwre接口，可以让这个类在Spring容器启动时自动获得ApplicationContext引用。@Component注解可以使该类被Spring容器自动实例化和管理。

自动装配过程是通过Spring的 `ApplicationContextAwareProcessor` 类实现的，他是一个后置处理器，<font color = "red">作用是在 Bean 实例化完成后，检测并处理实现了 ApplicationContextAware 接口的 Bean</font>。在Spring容器初始化时，它会检查所有的Bean，如果Bean实现了 `ApplicationContextAware` 接口，它会自动调用 `setApplicationContext(ApplicationContext applicationContext)` 方法将ApplicationContext的引用传递给该Bean。

## 1.3、第三步：【发布器】调用SpringUtil.publishEvent()发发布事件

在ArticleSettingServiceImpl类中：
```java
@Override
@CacheEvict(key = "'sideBar_' + #req.articleId", cacheManager = "caffeineCacheManager", cacheNames = "article")
public void updateArticle(ArticlePostReq req) {
	ArticleDO article = articleDao.getById(req.getArticleId());
	
    ArticleEventEnum operateEvent = null;  
    if (req.getStatus() != null) {  
        article.setStatus(req.getStatus());  
        if (req.getStatus() == PushStatusEnum.OFFLINE.getCode()) {  
            operateEvent = ArticleEventEnum.OFFLINE;  
        } else if (req.getStatus() == PushStatusEnum.REVIEW.getCode()) {  
            operateEvent = ArticleEventEnum.REVIEW;  
        } else if (req.getStatus() == PushStatusEnum.ONLINE.getCode()) {  
            operateEvent = ArticleEventEnum.ONLINE;  
        }  
    }  
    articleDao.updateById(article);  
  
    if (operateEvent != null) {  
        // 发布文章待审核、上线、下线事件  
        SpringUtil.publishEvent(new ArticleMsgEvent<>(this, operateEvent, article.getId()));  
    }  
}
```

## 1.4、第四步：定义事件监听器

通过 @EventListener 注解来处理事件。在 SitemapServiceImpl 类中可以看得到。
```java
/**  
 * 基于文章的上下线，自动更新站点地图  
 *  
 * @param event  
 */  
@EventListener(ArticleMsgEvent.class)  
public void autoUpdateSiteMap(ArticleMsgEvent<Long> event) {  
    ArticleEventEnum type = event.getType();  
    if (type == ArticleEventEnum.ONLINE) {  
        addArticle(event.getContent());  
    } else if (type == ArticleEventEnum.OFFLINE || type == ArticleEventEnum.DELETE) {  
        rmArticle(event.getContent());  
    }  
}
```

当ArticleMsgEvent类型的事件被发布时，此方法将自动被触发。在该方法中，首先获取事件的类型（ArticleEventEnum枚举值），然后根据事件类型执行相应的操作，上线时将文章添加到SiteMap，下线时从SiteMap中删除。

这就是技术派中整个完整的Spring事件监听机制实例了。

启动Redis，启动服务端，启动admin端，在后端随便下线一篇文章 ：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308271255621.png)

就可以在debug模式下看到事件触发了：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308271256883.png)

# 2、原理分析

Spring事件监听机制涉及到四个主要的类：
1. ApplicationEvent：事件对象
2. ApplicationListener：事件监听器，可以通过 `@EventListener` 注解定义事件处理方法，而无需实现 `ApplicationListener` 接口
3. ApplicationEventMulticaster：事件管理者，管理监听器和发布事件，通常由 `SimpleApplictaionEventMulticaster` 类实现。它会遍历所有已注册的监听器，并调用它们的 `onApplicationEvent()` 方法
4. ApplicationEventPublisher：事件发布者，在Spring中，可以通过实现 `ApplicationEventPublisherAware` 接口或使用 `@Autowired` 注解来注入 `ApplicationEventPublisher` 实例。当事件被发布时，Spring会自动调用已注册的 `ApplicationListener` 实现类的 `onApplicationEvent()` 方法。


## 2.1、ApplicationEvent

ApplicationEvent 继承了 EventObject 对象。
```java
public abstract class ApplicationEvent extends EventObject {
	private static final long serialVersionUID = 7099057708183571937L;
	private final long timestamp; // 多了一个时间戳属性
	public ApplicationEvent(Object source) {
	super(source);
		this.timestamp = System.currentTimeMillis(); // 初始当前化时间戳
	}
	
	public final long getTimestamp() {
		return this.timestamp;
	}
}
```

来看 ApplicationEvent 的子类关系图。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308271648332.png)

ApplicationEvent 有一个重要的子类 ApplicationContextEvent，而 ApplicationContextEvent 又有 4 个重要的子类：
1. ContextStartedEvent：当 Spring 容器启动时触发该事件。这意味着所有 Bean 都已加载，并且 ApplicationContext 已初始化。
2. ContextStoppedEvent：当 Spring 容器停止时触发该事件。当容器关闭并停止处理请求时，通常会触发此事件。
3. ContextRefreshedEvent：当 ApplicationContext 刷新时触发该事件。这表示所有 Bean 都已创建，并且已初始化所有单例 Bean（前提是它们在容器初始化时需要初始化）。
4. ContextClosedEvent：当 Spring 容器关闭时触发该事件。这表示所有 Bean 都已销毁，Spring 容器已清理资源并停止。

## 2.2、ApplicationListener

ApplicationListener 继承 EventListener 接口，并要求实现 onApplicationEvent(E event) 方法。
```java
@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

	/**
	 * Handle an application event.
	 * @param event the event to respond to
	 */
	void onApplicationEvent(E event);

}
```

`onApplicationEvent(E event)` 方法：当发布某个事件时，所有注册的ApplicationListener实例的onApplicationEvent方法都会被调用，在这个方法中，可以编写处理特定事件的逻辑。此方法接收一个类型为E的参数，这是ApplicationEvent的子类，表示触发的事件。

===当Spring应用启动时，Spring会扫描所有的Bean，寻找使用了 `@EventListener` 注解的方法，一旦找到了这种方法，Spring会为这些方法创建ApplicationListener实例并将其注册到ApplicationEventMulticaster。===

## 2.3、ApplicationEventMulticaster

ApplicationEventMulticaster是一个接口负责管理监听器和发布事件，包含了注册监听器、移除监听器以及发布事件的方法。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308271705748.png)

Spring容器中通常会有一个默认的实现，如SimpleApplicationEventMulticaster，继承了AbstractApplicationEventMulticaster。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308271706951.png)

AbstractApplicationEventMulticaster主要实现了管理监听器的方法（上面接口的五个方法），比如说 addApplicationListener。
```java
@Override  
public void addApplicationListener(ApplicationListener<?> listener) {  
   synchronized (this.defaultRetriever) {  
      // Explicitly remove target for a proxy, if registered already,  
      // in order to avoid double invocations of the same listener.      Object singletonTarget = AopProxyUtils.getSingletonTarget(listener);  
      if (singletonTarget instanceof ApplicationListener) {  
         this.defaultRetriever.applicationListeners.remove(singletonTarget);  
      }  
      this.defaultRetriever.applicationListeners.add(listener);  
      this.retrieverCache.clear();  
   }  
} 
```

最核心的一句代码：`this.defaultRetriever.applicationListeners.add(listener);` ，其内部类 `DefaultListenerRetriever` 里有两个集合，用来记录维护事件监听器。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308271723892.png)

这就和设计模式中的发布订阅模式一样了，维护一个List，用来管理所有的订阅者，当发布者发布消息时，遍历对应的订阅者列表，执行各自的回调handler。

再来看 SimpleApplicationEventMulticaster 类实现的广播事件逻辑：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308271727439.png)

multicastEvent 的主要作用是将给定的 ApplicationEvent 广播给所有匹配的监听器。
1. 首先，通过检查 eventType 参数是否为 null 来确定事件类型。如果 eventType 为 null，则使用 resolveDefaultEventType(event) 方法从事件对象本身解析事件类型。
2. 获取 Executor，它是一个可选的任务执行器，用于在异步执行监听器时调用。如果没有配置 Executor，则默认为 null，表示使用同步执行。
3. 使用 getApplicationListeners(event, type) 方法获取所有匹配给定事件类型的监听器。
4. 对于每个匹配的监听器，检查是否有 Executor 配置。如果存在 Executor，则使用 executor.execute() 方法将监听器的调用封装到一个异步任务中。如果没有配置 Executor，则直接同步调用监听器。
5. 使用 invokeListener(listener, event) 方法调用监听器的 onApplicationEvent 方法，将事件传递给监听器。

通过这个实现，SimpleApplicationEventMulticaster可以将事件广播给所有关心该事件的监听器，同时支持同步和异步执行模式。

最后调用listener.onApplicationEvent(event); 也就是我们通过实现接口ApplicationListener的方式来实现监听器的onApplicationEvent实现逻辑。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308271733493.png)

## 2.4、ApplicationEventPublisher

ApplicationEventPublisher是一个接口，用于将事件发布给所有感兴趣的监听器。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308271734490.png)

这个接口的实现类会将事件委托给ApplicationEventMulticaster。在Spring中，ApplicationContext通常充当事件发布者，它就实现了ApplicationEventPublisher接口。

ApplicationContext的publishEvent方法的逻辑实现主要在类AbstractApplicationContext中：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308271741140.png)

这段代码的主要逻辑在：
```java
if (this.earlyApplicationEvents != null) {
  this.earlyApplicationEvents.add(applicationEvent);
}
else {
  getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
}
```

这段代码的主要作用是在 `ApplicationContext` 初始化时处理应用程序事件的发布。当 `ApplicationContext` 还没有完全初始化时，例如在 `refresh()` 方法中， `earlyApplicationEvents` 列表会被用来保存早期的事件。在这个阶段，ApplicationEventMulticaster还没有完全配置好，因此无法直接发布事件。这些早期的事件将在ApplicationContext初始化完成后，ApplicationEventMulticaster配置好后，通过 `finishRefresh()` 方法中的 `publishEvent(new ContextRefreshedEvent(this));` 发布。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202308271759132.png)

当 ApplicationContext 初始化完成后，earlyApplicationEvents 列表将被设置为 null。此时，事件可以直接通过 getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType) 方法发布给所有匹配的监听器。

这个机制确保了在 ApplicationContext 初始化过程中产生的事件不会丢失，而是在 ApplicationContext 初始化完成后被正确地发布给所有感兴趣的监听器。

# 3、总结

这篇内容通过源码的形式讲解了 Spring 事件监听机制及其原理，希望帮助到大家，我们下一篇见。
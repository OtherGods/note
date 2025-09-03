计数器大量应用于互联网上的大大小小项目，你可以再各色应用场景中都找到计数器的应用范畴，单纯以技术派项目为例，也有相当多的地方会有计数相关的诉求，比如
- 文章点赞数
- 收藏数
- 评论数
- 用户粉丝数
- ……

看过技术派源码的小伙伴，可以发现我们提供了两种查询计数相关信息的方案，一个是基于db中的操作记录进行实时；还有一种则是基于redis的incr特性来实现计数器

接下来我们重点看下redis的计数器是怎么用于技术派的计数场景

# 1、计数的业务场景

首先我们先看一下技术派中使用到计数器的场景，主要有两大类（业务计数+pv/uv），三个细分领域（用户、文章、站点）

1. **用户的相关统计信息**
	1. 文章数，文章总阅读数，粉丝数，关注作者数，文章被收藏数，被点赞数量
	   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309031724979.png)
2. **文章的相关统计信息**
	1. 文章点赞数，阅读数，收藏数，评论数量
	   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309031725159.png)
3. **站点的PV/UV等统计信息**
	1. 站点的总PV/UV，某一天的PV/UV
	2. 某个uri的PV/UV
	   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309031726353.png)
注意上面的几个场景，这边文章主要目的是给大家介绍redis计数器的使用姿势

其中用户与文章的相关统计将是我们的重点，因为这两个的业务属性很相似，因此我们选择一个重点，以用户统计来进行介绍；

而pv/uv的实现请关注 [32、技术派数据统计PV-UV](2、相关技术/24、项目/1、技术派/2、进阶篇/32、技术派数据统计PV-UV.md)
# 2、redis计数器

redis计数器，主要是借助原生的incr指令来实现原子的+1/-1，更棒的是不仅redis的string数据结构支持incr，hash、zset数据结构同样也是支持incr的

## 2.1、incr指令

Redis Incr 命令将 key 中储存的数字值增一。
- 如果key不存在，那么key的值会先别初始化为0，然后再执行INCR操作
- 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误
- 本操作的值限制在64位（bit）有符号数字表示之内

接下来看下技术派的封装实现
```java
/**
 * 自增
 *
 * @param key
 * @param filed
 * @param cnt
 * @return
 */
 public static Long hIncr(String key, String filed, Integer cnt) {
    return template.execute((RedisCallback<Long>) con -> con.hIncrBy(keyBytes(key), valBytes(filed), cnt));
}
```

## 2.2、用户计数统计

我们将用户的相关计数，每个用户对应一个hash数据结构
- key: `user_statistic_${userId}`
- field:
	- followCount: 关注数
	- fansCount: 粉丝数
	- articleCount: 已发布文章数
	- praiseCount: 文章点赞数
	- readCount： 文章被阅读数
	- collectionCount: 文章被收藏数

计数器的核心就在于满足计数条件之后，实现的计数+1/-1

通常的业务场景中，此类计数不建议直接与业务代码强耦合，举个例子：
用户收藏了一个文章，若按照正常的设计，就是再收藏这里，调用计数器执行+1操作
上面的这样实现有问题么？
- 当然没有问题，但是不够优雅

比如现在技术派的设计场景，点赞之后，除了计数器更新之外，还有前面说到的用户活跃度更新，若所有的逻辑都放在业务中，会导致业务的耦合较重

技术派选择消息机制来应对这种场景（扩展一下，为什么大一点的项目，会设计自己的消息总线呢？一个重要的目的就是各自业务逻辑内聚，向外只抛出自己的状态/业务变更消息，实现解耦）

对应的，计数实现逻辑在`com.github.paicoding.forum.service.statistics.listener.UserStatisticEventListener
```java
@EventListener(classes = NotifyMsgEvent.class)
@Async
public void notifyMsgListener(NotifyMsgEvent msgEvent) {
    switch (msgEvent.getNotifyType()) {
        case COMMENT:
        case REPLY:
            // 评论/回复
            CommentDO comment = (CommentDO) msgEvent.getContent();
            RedisClient.hIncr(CountConstants.ARTICLE_STATISTIC_INFO + comment.getArticleId(), CountConstants.COMMENT_COUNT, 1);
            break;
        case DELETE_COMMENT:
        case DELETE_REPLY:
            // 删除评论/回复
            comment = (CommentDO) msgEvent.getContent();
            RedisClient.hIncr(CountConstants.ARTICLE_STATISTIC_INFO + comment.getArticleId(), CountConstants.COMMENT_COUNT, -1);
            break;
        case COLLECT:
            // 收藏
            UserFootDO foot = (UserFootDO) msgEvent.getContent();
            RedisClient.hIncr(CountConstants.USER_STATISTIC_INFO + foot.getDocumentUserId(), CountConstants.COLLECTION_COUNT, 1);
            RedisClient.hIncr(CountConstants.ARTICLE_STATISTIC_INFO + foot.getDocumentId(), CountConstants.COLLECTION_COUNT, 1);
            break;
        case CANCEL_COLLECT:
            // 取消收藏
            foot = (UserFootDO) msgEvent.getContent();
            RedisClient.hIncr(CountConstants.USER_STATISTIC_INFO + foot.getDocumentUserId(), CountConstants.COLLECTION_COUNT, -1);
            RedisClient.hIncr(CountConstants.ARTICLE_STATISTIC_INFO + foot.getDocumentId(), CountConstants.COLLECTION_COUNT, -1);
            break;
        case PRAISE:
            // 点赞
            foot = (UserFootDO) msgEvent.getContent();
            RedisClient.hIncr(CountConstants.USER_STATISTIC_INFO + foot.getDocumentUserId(), CountConstants.PRAISE_COUNT, 1);
            RedisClient.hIncr(CountConstants.ARTICLE_STATISTIC_INFO + foot.getDocumentId(), CountConstants.PRAISE_COUNT, 1);
            break;
        case CANCEL_PRAISE:
            // 取消点赞
            foot = (UserFootDO) msgEvent.getContent();
            RedisClient.hIncr(CountConstants.USER_STATISTIC_INFO + foot.getDocumentUserId(), CountConstants.PRAISE_COUNT, -1);
            RedisClient.hIncr(CountConstants.ARTICLE_STATISTIC_INFO + foot.getDocumentId(), CountConstants.PRAISE_COUNT, -1);
            break;
        case FOLLOW:
            UserRelationDO relation = (UserRelationDO) msgEvent.getContent();
            // 主用户粉丝数 + 1
            RedisClient.hIncr(CountConstants.USER_STATISTIC_INFO + relation.getUserId(), CountConstants.FANS_COUNT, 1);
            // 粉丝的关注数 + 1
            RedisClient.hIncr(CountConstants.USER_STATISTIC_INFO + relation.getFollowUserId(), CountConstants.FOLLOW_COUNT, 1);
            break;
        case CANCEL_FOLLOW:
            relation = (UserRelationDO) msgEvent.getContent();
            // 主用户粉丝数 + 1
            RedisClient.hIncr(CountConstants.USER_STATISTIC_INFO + relation.getUserId(), CountConstants.FANS_COUNT, -1);
            // 粉丝的关注数 + 1
            RedisClient.hIncr(CountConstants.USER_STATISTIC_INFO + relation.getFollowUserId(), CountConstants.FOLLOW_COUNT, -1);
            break;
        default:
    }
}

/**
 * 发布文章，更新对应的文章计数
 *
 * @param event
 */
@Async
@EventListener(ArticleMsgEvent.class)
public void publishArticleListener(ArticleMsgEvent<ArticleDO> event) {
    ArticleEventEnum type = event.getType();
    if (type == ArticleEventEnum.ONLINE || type == ArticleEventEnum.OFFLINE || type == ArticleEventEnum.DELETE) {
        Long userId = event.getContent().getUserId();
        int count = articleDao.countArticleByUser(userId);
        RedisClient.hSet(CountConstants.USER_STATISTIC_INFO + userId, CountConstants.READ_COUNT, count);
    }
}
```

上面直接基于当下技术派抛出的各种消息事件，来实现用户/文章的对应计数变更

不一样的地方则在于用户的文章数统计，因为消息发布时，并没有告知这个文章是从未上线状态到发布，发布到下线/删除，因此无法直接进行+1/-1

我们直接采用的是全量的更新策略

## 2.3、用户统计信息查询

前面实现了用户的相关计数统计，查询用户的统计信息则相对更简单了，直接hgetall即可
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309031739445.png)

## 2.4、缓存一致性

基本上到上面，一个完整的计数服务就已经成型了，但是我们再的实际的生产服务中，再自信的人，也无法拍着胸脯说我这个计数100%没有问题

通常我们会做一个校对/定时同步任务来保证缓存与实际数据中的一致性

技术派中选择简单的定时同步方案来实现
- **用户统计信息每天全量同步**
  ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309031741710.png)
- **文章统计信息每天全量同步**
  ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309031741106.png)


## 2.5、小结

基于redis的incr，很容易就可以实现计数相关的需求支撑，但是为啥我们要用redis来实现一个计数器？直接用数据库的原始数据进行统计有什么问题吗？

技术派的源码中，对于用户/文章的相关统计，同时给出了基于db计数 + redis计数两套方案

通常而言，项目初期，或者项目本身非常简单，访问量低，只希望快速上线支撑业务时，使用db进行直接统计即可，优势时是简单，叙述，不容易出问题；缺点则是每次都实时统计性能差，扩展性不强

当我们项目发展起来之后，借助redis直接存储最终的结果，在展示层直接获取即可，性能更强，满足各位的高并发的遐想，缺点则是数据的一致性保障难度更高

总的来说，就我个人的观点是，实际的选型没有万能答案，不要迷信权威，当你有机会来拍板时，请优先选择一个实现代价最小的方案，而不是一个最完美、最合适的方案，给自己留一个重构的机会，不然怎么体现自己的工作量😏，我是你们的老朋友一灰灰，互动一下再走呗~


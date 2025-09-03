对照：
- [16、技术派Caffeine整合本地缓存](2、相关技术/24、项目/1、技术派/1、基础篇/16、技术派Caffeine整合本地缓存.md)
- [14、技术派Cacheable注解实现缓存](2、相关技术/24、项目/1、技术派/1、基础篇/14、技术派Cacheable注解实现缓存.md)
- [1、SpringBoot缓存注解@Cacheable之自定义key策略及缓存失效时间指定](2、相关技术/16、常用框架-SpringBoot/补10_缓存注解@Cacheable等相关/1、SpringBoot缓存注解@Cacheable之自定义key策略及缓存失效时间指定.md)

在技术派实际使用Caffeine的过程中，踩了一个非常有意思的坑，当我直接指出来的时候，相信大部分小伙伴都很容易就能识别问题所在；但是在实际的项目编码过程中，又经常会被这些坑陷进去

接下来我们来仔细看一下这个坑为什么有意思，以及我又是怎么掉进去的
# 1、问题描述

问题表现非常明显，在查看一篇文章的详情页时，会发现侧边栏的推荐文章会出现重复，而且每刷新一次，就多一块，就TM离谱了
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310052203332.png)

很明显，出现数据重复的问题了

# 2、问题定位

通常来讲，容易复现的问题，都不算什么难题，本地debug一下，一般都能很快找到问题原因
所以我们当然也是本地先复现；然后debug，看一下关键的截图信息
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310052204322.png)

从sides的结果很容易就看出来，问题确实是重复了，而且重复的是 推荐文章 这个板块
那么是为什么呢？
- 查询推荐文章的逻辑都还没有走到，为啥上面查询侧边栏pdf的结果中，包含了后面的内容了？

再看一下具体实现（就是我们在23.03.06号引入Caffeine缓存时，添加的缓存逻辑）
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310052217777.png)

具体的方法内部，就只组装了pdfSideBar的侧变栏信息，为啥吐出的是包含 推荐文章 的列表呢？

**原因肯定就是出现在缓存上了**
- 实际debug也能发现，不会走入到上面的方法内部

那么为什么缓存会有问题呢？

我举个简单的例子，帮助大家理解
```java
public Map<String, List<Integer>> map = new HashMap<>();

public void show() {
  List<Integer> list = get("key");
  list.add(10);
}

public List<Integer> get(String key) {
  return map.computeIfAbsent(key, k -> new ArrayList<>());
}
```

然后就会发现，我每调用一次 show() 方法，map中的 key 对应的列表都加了一个数字10

然后就会发现，我没调用一次 show() 方法，map中的 key 对应的列表都加了一个数字10

因为虽然我看着是修改的get()方法返回的列表，但是实际上却是直接修改Map中返回的List列表，也就是说在上面show()方法内的列表改动，直接影响到了map这个缓存的结果【某网友回复：调用的方法得到的是缓存引用，对该引用操作会直接对缓存造成永久变更】

同样我们上的问题根源就在于 ArticleRecommendService#recommend这个方法内部
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310052345830.png)

我将具体的原因写在上面截图了
# 3、解决方案

这个问题出现的根本原因，就在于我直接用了缓存对象，并对其做了修改，从而导致整个缓存内容变更，出现影响

所以一个最基本的解决方案就是不直接使用缓存对象，转而封装一层，如
```java
@Override
public List<SideBarDTO> recommend(ArticleDTO articleDO) {
    // 文章详情页的侧边栏
    List<SideBarDTO> sides = sidebarService.queryArticleDetailSidebarList();

    List<SideBarDTO> results = new ArrayList<>(sides);
    // 推荐文章
    SideBarDTO recommend = recommendByAuthor(articleDO.getAuthor(), articleDO.getArticleId(), PageParam.DEFAULT_PAGE_SIZE);
    results.add(recommend);

    return results;
}
```

但是看到技术派实际代码的小伙伴会发现我并没有这么干，相反我做了一些比较大的调整，具体提交查看
- [fix: 修复缓存使用姿势问题 · itwanger/paicoding@c6ddb83](https://github.com/itwanger/paicoding/commit/c6ddb837f902ee3950fc0352b90e5292f05a6228)

实际修改方案是直接拿掉了 ArticleRecommendService 中的 recommend 方法，将相关逻辑直接迁移到 SidebarService （主要产生这个问题的原因在于产品迭代的变更，最开始是只有文章推荐，后续多了pdf侧边栏，然后再后续的迭代中，没有重新归拢业务域）

调整之后的代码如：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202310060031269.png)

注意一点：
- pdfSideBar添加缓存注解，Service内调用若希望这个缓存生效，不能直接访问，如上图中采用的是获取Bean再访问（主要目的是走代理）

# 4、问题复盘

通常来讲采坑之后都需要反思一下，为啥会出现这个bug，是粗心大意，还是压根就没想到；是不可避免，还是单纯的疏忽; 以后是否还会出现类似的问题等等

当然我们这个不是实际的工作场景，也没有领导会这么严苛来盯着，我们还是做一遍流程，给大家看一下一般的故障复盘包含哪些因素
- 问题发现（发现时间点、上报人）
- 解决过程（关键节点，如什么时间点回滚代码，什么时间点修复代码，什么时间点上线之后完全恢复）
- 原因分析（什么原因导致的这个问题）
- 影响范围，事故等级评估
- 复盘总结及后续Action（如何避免重复采坑）
下面就主要将问题原因捞出来讲一讲（后续action一般就是针对原因进行说明）

**原因**
1. 添加新的功能模块，并没有评估对现有的功能影响
	- 这个也是在实际工作中，最常见的问题产生原因之一，本来以为上线的是个新功能，虽知道上线之后旧的功能出现问题了，这个锅背得有点冤（说实话，这个锅不怨，影响范围点评估不到位，就是你自己的问题）
2. 直接使用了缓存返回的容器，没有预料到资源共享导致的潜在风险
	- 这种问题一般比较隐晦，但是需要额外注意，当我们使用容器、非基本对象时，特别是在做了缓存相关操作时，需要格外注意对它们本身的修改，是否会出现越权的情况；如果是，那么就需要深拷贝一份出来
3. 没有充分测试
	- 这个问题如果测试过，很容易发现，但是显然过分自信了，并没有充分测试，每个页面访问了一下就完事了，所以说一个靠谱的测试，是我们开发同学的极大保障（保持好和测试妹子的关系，非常重要）




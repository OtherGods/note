[原文](https://spring.hhui.top/spring-blog/2019/05/13/190513-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8B%E5%BA%94%E7%94%A8%E7%AF%87%E4%B9%8B%E5%80%9F%E5%8A%A9Redis%E6%90%AD%E5%BB%BA%E4%B8%80%E4%B8%AA%E7%AE%80%E5%8D%95%E7%AB%99%E7%82%B9%E7%BB%9F%E8%AE%A1%E6%9C%8D%E5%8A%A1/#0-%E9%A1%B9%E7%9B%AE)

判断一个网站值不值钱的一个重要标准就是看pv/uv，那么你知道pv,uv是怎么统计的么？当然现在有第三方做的比较完善的可以直接使用，但如果让我们自己来实现这么一个功能，应该怎么做呢？

本篇内容较长，源码如右 ➡️ [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-case/124-redis-sitecount](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-case/124-redis-sitecount)

# 1、背景及需求

为了看看我的博客是不是我一个人的单机游戏，所以就想着统计一下总的访问量，每日的访问人数，哪些博文又是大家感兴趣的，点击得多的；

因此就萌发了自己撸一个pv/uv统计的服务，当然我这个也不需要特别完善高大上，能满足我自己的基本需要就可以了

- 希望统计站点(域名）总访问次数
- 希望统计站点总的访问人数，当前访问者在访问人数中的排名（即这个ip是所有访问ip中的第多少位访问的这个站点）
- 每个子页面都有访问次数，访问总人数，当前ip访问的排名统计
- 同一个ip，同一天内访问同一个子页面，pv次数只加1次；隔天之后，再次访问pv+1

# 2、方案设计

前面的背景和需求，可以说大致说明了我们要做个什么东西，以及需要注意哪些事项，再进行方案设计的过程中，则需要对需求进行详细拆解

## 2.1、术语说明

前面提到了pv,uv，在我们的实际实现中，会发现这个服务中对于pv,uv的定义和标准定义并不是完全一致的，下面进行说明

### 2.1.1、 pv

`page viste`, 每个页面的访问次数，在本服务中，我们的pv指的是总量，即从开始接入时，到现在总的访问次数

但是这里有个限制： **一个合法的ip，一天之内pv统计次数只能+1次**

- 根据ip进行区分，因此需要获取访问者ip
- 同一天内，这个ip访问相同的URI，只能算一次有效pv；第二天之后，再次访问，则可以再算一次有效pv

### 2.1.2、hot



### 2.1.3、uv


## 2.2、 流程图


## 2.3、 数据结构


### 2.3.1、pv


### 2.3.2、hot


### 2.3.3、uv


### 2.3.4、结构图


## 2.4、方案设计


### 2.4.1、接口API


## 2.4.2、hot相关api


### 2.4.3、pv相关api


### 2.4.4、uv相关api


### 2.4.5、今日是否访问


# 3、服务实现


## 3.1、pv接口实现


## 3.2、hot接口实现


## 3.3、uv接口实现


## 3.4、今天是否访问过


## 3.5、api接口实现


# 4、测试与小结


## 4.1、测试


### 4.1.1、首次访问


### 4.1.2、再次访问


### 4.1.3、同ip，不同URI


### 4.1.4、不同ip，接上一个URI


### 4.1.5、上一个ip，换第一个uri


### 4.1.6、第二天访问


## 4.2、小结


# 5、其他
## 5.1、项目

工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-case/124-redis-sitecount](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-case/124-redis-sitecount)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)

来自：[【DB系列】Redis之ZSet数据结构使用姿势](https://spring.hhui.top/spring-blog/2018/12/12/181212-SpringBoot%E9%AB%98%E7%BA%A7%E7%AF%87Redis%E4%B9%8BZSet%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84%E4%BD%BF%E7%94%A8%E5%A7%BF%E5%8A%BF/)

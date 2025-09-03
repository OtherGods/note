
站点访问统计可以说是每个网站的标配了，当然市面上也有很多成熟的产品可以直接使用，比如技术派本身是接入了百度的站点统计功能，当然我们作为一个实用+学习的项目，这么好的知识点，怎么可以放过呢？ 我们借助redis的计数器功能，同样实现一套站点统计服务

# 1、方案设计
## 1.1、术语说明

前面提到了pv,uv，在我们的实际实现中，会发现这个服务中对于pv,uv的定义和标准定义并不是完全一致的，下面进行说明

### 1.1.1、PV

page viste, 每个页面的访问次数，在本服务中，我们的pv指的是总量，一个独立的ip，每访问一次这个url，则对应的访问计数+1

在我们的设计中，我们希望按自然日统计每个url的访问计数；也希望可以统计总的访问计数，以此来识别哪些页面是更受读者的喜欢

### 1.1.2、UV

unique visitor, 这个就是统计URI的访问ip数，同样按照自然日与总数进行区分

## 1.2、统计流程

用户访问，首先获取目标ip，根据其是否有访问过站点来更新对应的计数

- 首次访问目标资源： 
	- 总pv + 1, 总uv + 1
	- 当天pv + 1, 当天uv + 1
- 非首次访问，但是为当天第一次访问 
	- 总pv + 1, 总uv不变
	- 当天pv + 1, 当天uv + 1
- 当天非首次访问 
	- 总pv+1, 总uv不变
	- 当天pv+1, 当天uv不变

![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403042126865.png)

## 1.3、数据结构

之前在介绍redis计数器时，主要使用的就是hash来存储计数信息；我们这里同样可以借助hash来存储访问信息

接下来我们看一下需要存储的信息

1. 站点的总访问信息
	1. 站点的pv/uv
	2. 每个uri的pv/uv
2. 某一天的访问信息
	1. 某一天，站点的总访问pv/uv
	2. 某一天，每个uri的pv/uv
	由于在计算uv时，需要存储一个用户是否访问过某个资源的信息，因此我们额外加了一个存储单元，来保存用户的访问历史，如下
3. 用户的访问信息
	1. 用户访问站点的总次数
	2. 访问的每个uri的总次数

那么用户每天的访问信息存储在哪里呢？
- 存在上面的第二个存储结构中，和每天的访问信息放在一起
- 原因：每天的访问信息，通常不需要持久化保存，比如我们只存储最近一个月的每天访问情况；此时就可以直接给这个redis设置一个30天的有效期，到期之后自动上的访问信息、uri的pv/uv都给清除掉

因此完整的hash定义如下
```text
站点统计hash：  
visit_info:  
	pv: 站点的总pv  
	uv: 站点的总uv  
	pv_path: 站点某个资源的总访问pv  
	uv_path: 站点某个资源的总访问uv  

用户访问统计hash
visit_info_ip:  
	pv: 用户访问的站点总次数（**对应值等于1，则确定该ip是第一次访问系统**）  
	path_pv: 用户访问的路径总次数  

每天统计hash  
visit_info_20230822每日记录, 一天一条记录  
	pv: 12  # field = 月日_pv, pv的计数  
	uv: 5   # field = 月日_uv, uv的计数  
	pv_path: 2 # 资源的当前访问计数  
	uv_path: # 资源的当天访问uv  
	pv_ip: # 用户当天的访问次数（**对应值等于1，则确定该ip是今天第一次访问系统**）  
	pv_path_ip: # 用户对资源的当天访问次数（**对应值等于1，则确定该ip是今天第一次访问该系统的path资源**）
```

# 2、实现方式
## 2.1、统计计数

核心计数的实现路径 com.github.paicoding.forum.service.sitemap.service.SitemapServiceImpl#saveVisitInfo
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403042145051.png)

注意上面的实现，与前面的流程图会有一点点出入，主要就是为了少一次是否首次/当日首次访问的判断

其原理是：
1. 用户站点总pv+1, 若返回的最新计数是1，表示是站点的新用户 
	1. 所有uv+1
2. 今日pv+1, 若返回的最新计数是1，表示当前用户今日首次访问 
	1. 进入的uv+1

所以几个uv的统计实现如下
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403042217238.png)

## 2.2、redis管道封装

具体的实现策略可以参看源码，这里重点看一下这个redis的pipeline方式

上面的pv/uv的计数更新，存在较多次的计数交互，redis管道技术，可以在服务端未响应时，客户端可以继续向服务端发送请求，并最终一次性读取所有服务端的响应，从而实现批量操作

在具体的实现中，借助函数方法，实现redis pipeline使用姿势的封装，简化调用，强烈推荐给各位小伙伴，在实际的业务编码过程中，这类的封装写法可以为使用者提供极佳的用户体验
- com.github.paicoding.forum.core.cache.RedisClient
```java
public static PipelineAction pipelineAction() {  
    return new PipelineAction();  
}  
  
/**  
 * redis 管道执行的封装链路  
 */  
public static class PipelineAction {  
    private List<Runnable> run = new ArrayList<>();  
  
    private RedisConnection connection;  
  
    public PipelineAction add(String key, BiConsumer<RedisConnection, byte[]> conn) {  
        run.add(() -> conn.accept(connection, RedisClient.keyBytes(key)));  
        return this;  
    }  
  
    public PipelineAction add(String key, String field, ThreeConsumer<RedisConnection, byte[], byte[]> conn) {  
        run.add(() -> conn.accept(connection, RedisClient.keyBytes(key), valBytes(field)));  
        return this;  
    }  
  
    public void execute() {  
        template.executePipelined((RedisCallback<Object>) connection -> {  
            PipelineAction.this.connection = connection;  
            run.forEach(Runnable::run);  
            return null;  
        });  
    }  
}  
  
@FunctionalInterface  
public interface ThreeConsumer<T, U, P> {  
    void accept(T t, U u, P p);  
}
```

## 2.3、计数更新与使用

pv/uv的更新，可以直接在Filter进行统一的调用，为了避免计数影响实际的业务操作，我们在采用异步更新策略
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403042222846.png)

目前站点的统计信息在前台只显示了全局站点的统计情况，使用姿势也比较简单，直接从上面的hash中获取对应的计数即可
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403042227588.png)

前台使用路径：
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202403042228804.png)

## 2.4、小结

最后按照惯例小结一下，pv/uv的实现总的来说还是比较简单的，主要是基于redis的计数器来实现，关键的知识点两个

- hash: incr来实现原子计数
- pipeline: 管道方式实现批量操作

最后抛出一个小问题，我们通过记录ip的访问来实现uv的计数，如果哪一天我们的站点爆了，一天几百万的访问量，这个用户的访问记录存储就是一个很大的开销了，有什么好的解决方案么？

有兴趣的小伙伴可以看一下redis中的HyperLoglog








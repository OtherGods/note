# 1、前言

大家在读这篇文章之前，请先移步至以下几篇文章，因为这篇文章是在这几篇文章基础上进行去完成的。

a、[13、技术派Mysql-Redis缓存一致性](2、相关技术/24、项目/1、技术派/2、进阶篇/13、技术派Mysql-Redis缓存一致性.md) ，这篇文章详细的讲解了6种保证缓存一致性解决方案，并且文章中实战为了保证数据的实时性，采用了先写缓存MySQL在删除Redis的方式。这篇文章中不追求数据实时性只追求最终一致性来实现；利用了[13、技术派Mysql-Redis缓存一致性](2、相关技术/24、项目/1、技术派/2、进阶篇/13、技术派Mysql-Redis缓存一致性.md)的第6种先写MySQL，通过Binlog来移步更新Redis方案。

b、[15、技术派Cancal实现MySQL和ES同步](2、相关技术/24、项目/1、技术派/2、进阶篇/15、技术派Cancal实现MySQL和ES同步.md) ，这篇文章详细的讲解了MySQL如何利用Canal中间件将数据同步至ES的原理和实现方案，大家可以通过该文章来理解其原理并且去准备接下来实战中的环境，这里我就不在过多解释原理和如何准备方案。

PS：只需要将Canal的deployer来准备好，且与MySQL建立其通信即可，其他整合ES之类的不需要哈。

c、[17、技术派Redis分布式锁](2、相关技术/24、项目/1、技术派/2、进阶篇/17、技术派Redis分布式锁.md) ，这篇文章也麻烦大家阅读下，因为该文章中讲解了整合Redis，且Article表中数据存储至Redis缓存中，该文章的代码实战就是在 [17、技术派Redis分布式锁](2、相关技术/24、项目/1、技术派/2、进阶篇/17、技术派Redis分布式锁.md) 这篇文章的代码之上进行书写的。可以只看下Article表中数据存储至Redis缓存和看代码中如何整合Redis，分布式锁如果没时间看或者不理解也没关系，这篇文章和分布式锁没有关联的。

上面这几篇文章中对今天的知识点已经做了详细的讲解，这篇文章中就不在过多进行叙述，直接进入下面的实战部分。如果大家有疑问或者文章中有错误的地方，大家可以在下面进行评论或者在知识星球中进行提问。

前期环境准备：1、MySQL正常启动且打开binlog日志；2、Redis正常启动；3、Canal正常启动且能够监听MySQL的binlog日志；

# 2、SpringBoot整合Canal
## 2.1、引入Canal客户端依赖


```xml
<!--Canal 依赖-->
<dependency>
    <groupId>top.javatool</groupId>
    <artifactId>canal-spring-boot-starter</artifactId>
    <version>1.2.1-RELEASE</version>
</dependency>
```
## 2.2、yml配置文件中配置Canal

```yml
# canal配置
canal:
  # canal的地址
  server: 127.0.0.1:11111
  # 默认的数据同步的目的地
  destination: example
```

这里需要和你的Canal配置文件相对应，一定要配置正确，包括其端口号，这里因为端口号坑了我一把。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101447928.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101449984.png)

yml中应该与上面相对应哈。
此时可以将项目启动一下看看是否报Canal连接异常，如果正常启动则说明没有什么问题。

## 2.3、监听Canal数据并进行处理

首先应该编写Canal数据将要映射的实体类，我没有单独编写其类，直接将mapper层的DO实体类拿来复用，因为字段都是一样的，我也就没有单独编写。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101504529.png)

编写监听处理器
```java
package com.github.paicoding.forum.service.article.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.paicoding.forum.service.article.repository.entity.ArticleDO;
import com.github.paicoding.forum.service.constant.RedisConstant;
import com.github.paicoding.forum.service.utils.RedisUtil;

import lombok.extern.slf4j.Slf4j;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

/**
 * 文章详情Handler：mysql——>redis
 *
 * @ClassName: ArticleHandler
 * @Author: ygl
 * @Date: 2023/6/10 19:11
 * @Version: 1.0
 */
@Slf4j
@Component
@CanalTable("article")
public class ArticleHandler implements EntryHandler<ArticleDO> {
	@Autowired
    private RedisUtil redisUtil;

    @Override
    public void insert(ArticleDO articleDO) {
    	
        log.info("Article表增加数据");
    }
    
    @Override
    public void update(ArticleDO before, ArticleDO after) {

        Long articleId = after.getId();
        // 监听到数据发生改变之后直接删除Redis对应缓存数据
        this.delRedisKey(articleId);
        log.info("Article表更新数据");
    }
    
    @Override
    public void delete(ArticleDO articleDO) {

        Long articleId = articleDO.getId();
        this.delRedisKey(articleId);

        log.info("Article表删除数据");
    }
    
    private void delRedisKey(Long articleId) {
	    String redisCacheKey =
                RedisConstant.REDIS_PAI_DEFAULT
                        + RedisConstant.REDIS_PRE_ARTICLE
                        + RedisConstant.REDIS_CACHE
                        + articleId;
		redisUtil.del(redisCacheKey);
        log.info("删除Redis的key值：" + redisCacheKey);

    }
}
```

代码中部分解释
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101522263.png)

## 2.4、测试Canal

正常启动后，我们能看到好多下面的打印日志：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101606893.png)


说明已经正常启动，项目正常从Canal中拉取数据。

### 2.4.1、对数据新增测试

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101607903.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101608504.png)

可以看到insert成功监听到，只是我这里没有做任何业务逻辑处理。

### 2.4.2、对数据更新测试

先对id是103的数据进行修改
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101615625.png)

进入断点
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101616353.png)

上面可以看到已经成功进入update方法断点，日志在控制台中成功打印，且从中还可以发现eventType和更新数量等等。

这里我做了删除Redis缓存的业务逻辑处理，也就是当监听到update时，那么在方法中会删除对应的redis缓存。

监听到的before数据。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101623094.png)

监听到的after数据
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101624700.png)

￼	根据监听的before和after数据就可以对其做自身业务处理。

### 2.4.3、对数据删除测试

删除数据
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101625182.png)

进入断点，并且在控制台打印日志
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101626241.png)

监听到的数据映射后的articleDO实体如下所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101626347.png)

这里我根据其id做了删除对应缓存的业务处理。

# 3、总结

总结还是老规矩，上图上图上图上
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101627839.png)

其实这种方案有个缺点，就是我们可以看到canal-client从Canal中获取数据一直采用的是for死循环主动拉取，这种会非常消耗我们的服务性能。其实后期我们可以搞个Canal当有数据时采用主动推送，这样就避免了一直for死循环拉取数据；可以引用Kafka中间件，Canal将数据交给Kafka，然后Kafka推送给Canal-client。



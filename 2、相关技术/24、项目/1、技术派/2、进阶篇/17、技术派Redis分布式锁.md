#redis分布式锁 

参考：
- [6、分布式锁有几种实现方式？](2、相关技术/21、分布式/Hollis/6、分布式锁有几种实现方式？.md)
- [3、分布式锁及实现方案](2、相关技术/21、分布式/Java全栈/3、分布式锁及实现方案.md)
- [18、Redis实现分布式锁（应用篇）](2、相关技术/5、数据库-Redis/一灰/18、Redis实现分布式锁（应用篇）.md)
- [19、redisson分布式锁使用及注意事项](2、相关技术/5、数据库-Redis/一灰/19、redisson分布式锁使用及注意事项.md)
- [24、实现一个分布式锁需要考虑哪些问题？](2、相关技术/21、分布式/Hollis/24、实现一个分布式锁需要考虑哪些问题？.md)
- [30、如何用SETNX实现分布式锁？](2、相关技术/5、数据库-Redis/Hollis/30、如何用SETNX实现分布式锁？.md)
- [31、什么是RedLock，他解决了什么问题？](2、相关技术/5、数据库-Redis/Hollis/31、什么是RedLock，他解决了什么问题？.md)
- [31、锁和分布式锁的核心区别是什么？](2、相关技术/21、分布式/Hollis/31、锁和分布式锁的核心区别是什么？.md)
- [32、如何用Redisson实现分布式锁？](2、相关技术/5、数据库-Redis/Hollis/32、如何用Redisson实现分布式锁？.md)
- [35、Redisson的watch dog机制是怎么样的](2、相关技术/5、数据库-Redis/Hollis/35、Redisson的watch%20dog机制是怎么样的.md)
- [51、Redis实现分布锁的时候，哪些问题需要考虑？](2、相关技术/5、数据库-Redis/Hollis/51、Redis实现分布锁的时候，哪些问题需要考虑？.md)

# 1、前言
## 1.1、本地锁和分布式锁区别

锁我想对大家都不陌生，在我们刚学习Java的时候，肯定知道synchronized和Lock锁；这两者都是本地锁；何为本地锁呢？本地锁就是该锁只针对当前节点有效，也就是当node A获取锁时，那么node B同样还可以获取锁，这种情况就是本地锁；如果服务只部署了一个节点的话，那么用这种本地锁是没有问题的，但是现如今系统为了抗高并发、高可用和高性能会部署多节点(集群部署)，那么此时如果还用本地锁的话那么就可能出现问题，因此分布式锁就诞生了；分布式锁就是当有一个节点获取到锁后，其它节点也是不可以获取锁。

## 1.2、Redis分布式锁和Zookeeper分布式锁区别

谈起分布式集群，那么就绕不开CAP理论，也就是强一致性、可用性和分区容错性。三者只能选其之二，不可兼容。这里我就不具体分析其原因之类了，直接步入其两把分布式锁区别。
1. Redis分布式锁：它追求的高可用性和分区容错性。Redis在追求高可用性时会当Redis在写入主节点数据后，会立即返回成功，不关心异步主节点同步从节点数据是否成功。并且Redis是基于内存操作，性能极其高，官方给的每秒可达到10W的吞吐量。
2. Zookeeper分布式锁：它追求的是强一致性和分区容错性。Zookeeper在写入主节点数据后会等到从节点同步数据完成后才会返回成功。那么此时为了数据的强一致性而牺牲了其可用性。

两者综合对比下来，技术派为了追求用户体验度，那么就采用了Redis分布式锁来实现。

# 2、使用Redis分布式锁背景

技术派使用Redis分布式锁的背景是当用户根据articleId去查询文章详情页，然后查询出结果后返回。

查询文章详情流程图如下所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312052245198.png)

上面这种如果并发量不特别高的情况下是没有问题的，但是就怕并发量高的时候就会出现问题；出现的问题是缓存中没有数据，然后到MySQL中查询这一步会出现问题。

问题出现点如下所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312052246642.png)

因为当高并发时，如果查询缓存中没有数据，大量的用户会同时去访问我们的DB层MySQL，MySQL的资源是非常珍贵的，并且性能没有Redis好，很容易将我们的MySQL打宕机，那么进而影响我们的整个服务。

针对这种问题，我们可以将用户同时访问同一个文章时，只允许一个用户去MySQL中获取数据，由于我们服务是集群化部署，那么就开始用到了Redis分布式锁。

逻辑如下所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312052247903.png)

这样采用加锁的方式就能很好的保护我们的DB层数据库，进而保证系统的高可用性。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312052247878.png)


# 3、Redis分布式锁几种实现方式
相关代码参考分支：redis_distributed_lock_20230531

其实可以直接给大家讲最终的实现方式，这样我也比较省事；但是心里总感觉少点什么，所以接下来我就用几种方式由简到繁一点一点的推出最佳实现方式。

## 3.1、Redis实现分布式锁

代码如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312052248847.png)


### 3.1.1、setIfAbsent(key,value,time)实现分布式锁第一种方式

redisTemplate.opsForValue().setIfAbsent(key, value, time, TimeUnit.SECONDS)对应的Redis命令是set key value EX time NX。

set key value EX time NX是个复合操作，是setNx和setEx两个复合操作，保证其原子性，要么全部成功，否则加锁失败。底层采用的是lua脚本来保证其原子性。

redisTemplate.opsForValue().setIfAbsent(key, value, time, TimeUnit.SECONDS)含义就是：如果key不存在则加锁成功，返回true；否则加锁失败，返回false。

redisTemplate.opsForValue().setIfAbsent(key, value, time, TimeUnit.SECONDS)含义就是：如果key不存在则加锁成功，返回true；否则加锁失败，返回false。

第一种加锁逻辑如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312062205132.png)

主要逻辑就是：当缓存中没有数据时，就开始加锁，加锁成功则允许访问数据库，加锁失败则自旋重新访问。

主要代码如下所示：
```java
/**
 * Redis分布式锁第一种方法
 *
 * @param articleId
 * @return ArticleDTO
 */
private ArticleDTO checkArticleByDBOne(Long articleId) {
	String redisLockKey =
            RedisConstant.REDIS_PAI + RedisConstant.REDIS_PRE_ARTICLE + RedisConstant.REDIS_LOCK + articleId;
	ArticleDTO article = null;
	
	// 加分布式锁：此时value为null，时间为90s(结合自己场景设置合适过期时间，这里我为了验证随便设置的时间)
	Boolean isLockSuccess = redisUtil.setIfAbsent(redisLockKey, null, 90L);
	if (isLockSuccess) {
        // 加锁成功可以访问数据库
        article = articleDao.queryArticleDetail(articleId);
    } else {
        try {
	        // 短暂睡眠，为了让拿到锁的线程有时间访问数据库拿到数据后set进缓存，
            // 这样在自旋时就能够从缓存中拿到数据；注意时间依旧结合自己实际情况
            Thread.sleep(200);
        } catch (InterruptedException e) {
            
            e.printStackTrace();
		}
		// 加锁失败采用自旋方式重新拿取数据
		this.queryDetailArticleInfo(articleId);
	}
	return article;
}
```

代码中我希望大家好好阅读下，因为里面主要逻辑我都在代码注释中；

由于上面我在流程图中还有代码注释中已经叙述了两次了，因此不再重复。下面我直接叙述缺点。

缺点：
这里我们在setIfAbsent中虽然设置了过期时间，但是会出现一种情况：当我们业务执行完之后，但是我们的锁还被持有着，虽然有过期时间，且Redis中有淘汰策略，但是还是不建议这么做，因为Redis缓存资源是非常重要的；正确的做法应该是当业务执行完之后，直接释放锁。

### 3.1.2、setIfAbsent(key,value,time)实现分布式锁第二种方式

针对第一种方式所产生的不能及时释放锁的问题，我们进行优化为当业务执行完后立即释放锁。代码如下所示：
```java
/**
 * Redis分布式锁第二种方法
 *
 * @param articleId
 * @return ArticleDTO
 */
private ArticleDTO checkArticleByDBTwo(Long articleId) {

	String redisLockKey =
            RedisConstant.REDIS_PAI + RedisConstant.REDIS_PRE_ARTICLE + RedisConstant.REDIS_LOCK + articleId;
	ArticleDTO article = null;
	Boolean isLockSuccess = redisUtil.setIfAbsent(redisLockKey, null, 90L);
	try {
        if (isLockSuccess) {
            article = articleDao.queryArticleDetail(articleId);
        } else {
	        Thread.sleep(200);
            this.queryDetailArticleInfo(articleId);
        }
    } catch (InterruptedException e) {
	    e.printStackTrace();
    } finally {
        // 和第一种方式相比增加了finally中删除key
        RedisClient.del(redisLockKey);
    }
    return article;
}
```

第二种方法为了解决当业务执行完毕之后立即删除key值增加了finally中删除key。这样就针对第一种方式解决了。

但是这种还是存在问题：
释放别人的锁：线程A已经获取到锁，在执行业务，但是还没有执行完成，过期时间到了，那么该锁就会被释放；此时线程B能够获取该锁，且执行业务逻辑，但是此时线程A执行完成需要释放锁，但是此时释放的锁是线程B的，也就是释放别人的锁。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312062252934.png)

### 3.1.3、setIfAbsent(key,value,time)实现分布式锁第三种方式

针对第二种加锁方式中存在误释放他人的锁的情况，我们可以采用在加锁的时候去设置个value值，然后在释放锁前判断下给key的value是否和前面设置的value值相等，相等则说明是自己的锁可以删除，否则是别人的锁不能删除。

代码如下所示：
```java
/**
 * Redis分布式锁第三种方法
 *
 * @param articleId
 * @return ArticleDTO
 */
private ArticleDTO checkArticleByDBThree(Long articleId) {

	String redisLockKey =
            RedisConstant.REDIS_PAI + RedisConstant.REDIS_PRE_ARTICLE + RedisConstant.REDIS_LOCK + articleId;
	// 设置value值，保证不误删除他人锁
    String value = RandomUtil.randomString(6);
    Boolean isLockSuccess = redisUtil.setIfAbsent(redisLockKey, value, 90L);
    ArticleDTO article = null;
    try {
        if (isLockSuccess) {
            article = articleDao.queryArticleDetail(articleId);
        } else {
            Thread.sleep(200);
            this.queryDetailArticleInfo(articleId);
        }
    } catch (InterruptedException e) {
	    e.printStackTrace();
    } finally {
	    // 这种先get出value，然后再比较删除；这无法保证原子性，为了保证原子性，采用了lua脚本
        /*
        String redisLockValue = RedisClient.getStr(redisLockKey);
        if (!ObjectUtils.isEmpty(redisLockValue) && StringUtils.equals(value, redisLockValue)) {
            RedisClient.del(redisLockKey);
        }
        */
        // 采用lua脚本来进行先判断，再删除；和上面的这种方式相比保证了原子性
        Long cad = redisLuaUtil.cad("pai_" + redisLockKey, value);
        log.info("lua 脚本删除结果：" + cad);
    }
    return article;
}
```

业务逻辑如下所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312072233976.png)

第三种方式解决了误删他人锁的问题，但是还存在一个问题——过期时间值如何设置。

时间设置过短：可能业务还没有执行完毕，过期时间已经到了，锁被释放，那么其他线程可以拿到锁去访问DB，那么违背了我们的初心。

时间设置过长：过长的话，可能在我们加锁成功后，还没有执行到释放锁，在这一段过程中节点宕机了，那么在锁未过期的这段时间其他线程是不能够获取锁，这样也不好。

因此锁的过期时间设置是个大学问。

其实针对这个问题，可以采用写一个守护线程，然后每隔固定时间去查看业务是否执行完毕，如果没有的话就延长其过期时间，也就是为其锁续期；

上面也就是俗称了看门狗机制，上述逻辑已经有技术实现——Redission。

下面我就详细讲解下Redission实现其分布式锁。

## 3.2、Redission实现分布式锁

redission实现分布式锁流程图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312072243195.png)

```java
/**
 * Redis分布式锁第四种方法
 *
 * @param articleId
 * @return ArticleDTO
 */
private ArticleDTO checkArticleByDBFour(Long articleId) {

	String redisLockKey =
            RedisConstant.REDIS_PAI + RedisConstant.REDIS_PRE_ARTICLE + RedisConstant.REDIS_LOCK + articleId;
    // 获取锁
	RLock lock = redissonClient.getLock(redisLockKey);
    //lock.lock();
    ArticleDTO article = null;
    try {
        //尝试加锁,最大等待时间3秒，上锁30秒自动解锁；时间结合自身而定
        if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
            article = articleDao.queryArticleDetail(articleId);
        } else {
            // 未获得分布式锁线程睡眠一下；然后再去获取数据
            Thread.sleep(200);
            this.queryDetailArticleInfo(articleId);
        }
    } catch (InterruptedException e) {
        e.printStackTrace();
    } finally {
	    //判断该lock是否已经锁 并且 锁是否是自己的
        if (lock.isLocked() && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }

    }
    return article;
}
```

redission首先获取锁(get lock())，然后尝试加锁，加锁成功后可以执行下面的业务逻辑，执行完毕之后，会释放该分布式锁。

redission解决了redis实现分布式锁中出现的锁过期问题，还有释放他人的锁，它还是可重入锁：它的内部机制是默认锁过期时间是30s，然后会有一个定时任务在每10s去扫描一下该锁是否被释放，如果没有释放那么就延长至30s，这个机制就是看门狗机制(watch dog)。

如果请求没有获取到锁，那么它将while循环获取继续尝试加锁。

上面的redission我只是大概讲解下用法，内部具体逻辑没有讲解；大家如果感兴趣的话可以看下它内部源码，相对来说源码也不是特别复杂。

# 4、总结

上面由简到繁的讲解了四种方式，其实我最建议的还是采用Redission实现分布式锁，它基本上解决了所有问题。

redission实际还存在一个问题，就是当redis是主从架构时，线程A刚刚成功的加锁在了master节点，还没有同步到slave节点，此时master节点给挂了，然后线程B这时过来是可以加锁的，但是实际上它已经加锁过了，这就是所出现的问题，这个问题涉及了高一致性 ，也就是C原则了；redission是无法解决高一致性问题的。

如果想要解决高一致性可以使用红锁，或者zk锁；他们保证了高一致性，但是不建议使用，因为为了保证高一致性，它丢失了高可用性，对用户体验感不好，且出现上述问题出现几率不大，不能因为这种很小的问题出现几率而舍弃其高可用性。

这里我也就不过多的对这两种锁来做具体的讲述，大家如果有兴趣的话可以自己找找文章，他们的原理无非就是必须多节点加锁成功才算加锁成功。

上面讲解的特别详细了，我也就不在过多总结了。





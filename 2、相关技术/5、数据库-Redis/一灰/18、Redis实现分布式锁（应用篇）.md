
本篇虽然名为redis实现分布式锁，但也会说一下基于数据库实现分布式锁的思路（至于ZK实现的分布式锁，没玩过zk，就不抄了）

# 1、分布式锁
## 1.1、介绍

锁，这个名词或者动词可以说频繁的出现在我们的编程生涯中，当然我们最常见的就是单体应用中的`synchronized`以及`Lock`，主要就是为了确保某一段代码逻辑（特别是写逻辑）一次只能被一个业务方调用执行

而分布式锁最大的特点就是，希望即便是在分布式的环境中，即便有多个应用、多个实例操作某段业务逻辑，也能确保这段逻辑的”串行”执行

## 1.2、应用场景

分布式锁最主要的特点就是希望能确保，在某一时间段内，只能有一个业务方，访问某段业务逻辑

从上面的描述，一个非常典型的case就是电商里面的下单减库存

我们在创建订单之前，得确保库存足够，假设有这么一个场景

- 商品只有2个库存
- 用户A，下单买两个，去判断库存为2，可以购买
- 用户B，下单买1个，去判断库存，发现也是2，可以购买
- 当用户A,B的订单都完成了，最终库存变成-1了，导致超卖

如果我们使用分布式锁，把这段逻辑包裹住

- 用户A，下单买2个，抢占分布式锁成功，判断库存为2
- 用户B，下单买1个，抢占分布式锁，已经被A占用了，抢不到，等待；直到用户A释放
- 用户A，订单生成，库存-2，现在真实库存变为0，释放锁
- 用户B，多次尝试获取锁，成功之后，再去看库存，为0，不满足，下单失败

## 1.3、DB版分布式锁

这种方式我个人没有用过，根据网上查询到一些资料，从原理上进行简单的说明

**唯一键约束方式**

借助mysql的唯一键约束，确保一次只能有一个`insert sql`是成功的，操作成功的就认为是成功的抢了锁；如果插入失败，则表示没有抢占；删除这条记录就表示释放锁

这种方式实现比较简单，但是问题比较多
- 如果抢占锁的小伙伴一直不删除这条记录，那这个锁就永不释放么？
- 抢占锁失败直接抛异常，一个是不友好，另外一个就是非阻塞方式，需要我们自己来循环的判断是否
- 非重入的，即持有锁的小伙伴，再次去获取锁的时候，也是失败
- 性能瓶颈

**乐观锁**

在数据库中添加一个version字段，在修改的时候，加一个version的查询限定，一把的业务逻辑为

- `select * from table where id=1`
- 执行业务逻辑
- `update xxx, version=version+1 wherer id=1 and version=oldVersion` 如果执行成功，则表示正确持有锁，业务流程ok；如果失败，则表示没有抢占到锁，回滚

上面这个做法，有个比较明显的问题，没有阻塞操作，和我们预期的分布式锁差别有点大

**悲观锁**

如果想解决上面的阻塞问题，我们可以考虑使用写锁

- 开启事务
- `select * from table where id=1 for update`
- 执行业务逻辑
- 提交事务，释放锁

上面是利用数据库的写锁来完成排他性，同样存在锁释放问题

**小结**

上面的几个思路主要来自于网络上的一些博文，就我个人看完的观点，基于db的分布式锁绝不是一个优雅的选择方案，如非万不得已，不要这么干

# 2、Redis版分布式锁

关于redis实现分布式锁的方案由来已久了，主要是借助redis的单线程模型，以及命令执行的原子性，通过确保同一时刻，只能有一个`setnx`成功，即表示抢占到锁；其他失败的小伙伴只能遗憾的加入下一次的抢锁计划

为了避免持有锁的小伙伴因为异常挂掉没有释放锁，从而导致其他客户端都拿不到锁的问题，因此在抢占锁的时候，我们需要考虑设置有效期；幸运的是redis已经支持一个命令执行上面的过程了

## 2.1、实现

redis分布式锁

设置锁和释放锁两个方面；

- setnx：当不存在时，设置成功；存在时，设置失败
- 为了防止持有锁的客户端挂掉，没有释放锁，从而导致其他客户端都拿不到锁的case，我们需要设置锁的有效期

这里我们借助`SET key value [EX seconds | PX milliseconds] [NX | XX] [KEEPTTL]`来实现原子的操作

下面是基于`RedisTemplate`来实现
```java
public static final String ERROR_CODE = "error";  
@Autowired  
private RedisTemplate redisTemplate;  
  
private Random random;  
  
public RedisDistributeLock() {  
    random = new Random();  
}  
  
private String randPrefix() {  
    return String.format("%04d", random.nextInt(10000));  
}  
  
public String tryLock(String lockKey, long expireSeconds, int maxRetryTime) {  
    // 为了避免value冲突，加一个随机的前缀串  
    String value = randPrefix() + "_" + (System.currentTimeMillis() + expireSeconds * 1000 + 1);  
    boolean ans;  
    int retryTimes = 0;  
    do {  
        ans = redisTemplate.opsForValue().setIfAbsent(lockKey, value, expireSeconds, TimeUnit.SECONDS);  
        if (ans) {  
            return value;  
        }  
  
        retryTimes++;  
        try {  
            Thread.sleep(100);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    } while (retryTimes < maxRetryTime);  
  
    return ERROR_CODE;  
}
```

释放锁的时候需要注意，持有者不能把别人的锁给释放掉了（比如A持有了锁，超时时间为5s，但是它的业务逻辑超过了5s，导致B也获取到了锁，如果这时候A执行完了，把B的锁删掉，那就gg了）

因此，删除的重点是，只能删自己的锁（这里就需要借助lua脚本来执行原子操作了）
```java
public boolean release(String lockKey, String value) {  
    //释放锁的lua脚本,保证判断和删除操作的原子性  
    String script =  
            "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";  
    @SuppressWarnings("unchecked")  
    RedisScript<Boolean> redisScript = RedisScript.of(script, Boolean.class);  
    return (boolean) redisTemplate.execute(redisScript, Collections.singletonList(lockKey), value);  
}
```

## 2.2、测试

我们写一个简单的并发扣库存的测试case，主要的逻辑就是当前的库存小于购买数时，下单失败
```java
@SpringBootApplication  
public class Application {  
  
    private AtomicInteger count = new AtomicInteger(35);  
  
    /**  
     * 在一个线程持有锁的过程中，不允许其他的线程持有锁  
     *  
     * @param redisDistributeLock  
     * @param lockKey  
     * @param threadName  
     * @param retryTime  
     */  
    private void threadTest(RedisDistributeLock redisDistributeLock, String lockKey, String threadName, int retryTime, int n) {  
        new Thread(new Runnable() {  
            @Override  
            public void run() {  
                String value = redisDistributeLock.tryLock(lockKey, 10_000, retryTime);  
                if (count.get() >= n) {  
                    int left = count.addAndGet(-n);  
                    System.out.println(threadName + "减库存，剩余: " + left + " 购买: " + n);  
                } else {  
                    System.out.println(threadName + "库存不足下单失败，当前库存: " + count.get() + " 购买： " + n);  
                }  
                redisDistributeLock.release(lockKey, value);  
            }  
        }).start();  
    }  
  
    public Application(RedisDistributeLock redisDistributeLock) throws InterruptedException {  
        String lockKey = "lock_key";  
        Random random = new Random();  
        for (int i = 0; i < 30; i++) {  
            threadTest(redisDistributeLock, lockKey, "t-" + i, random.nextInt(30), random.nextInt(3) + 1);  
        }  
        Thread.sleep(20 * 1000);  
    }  
  
    public static void main(String[] args) {  
        SpringApplication.run(Application.class);  
    }  
}
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202405232241506.png)

# 3、其他
## 3.1、项目

工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/122-redis-template)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)


转载自：[【DB系列】Redis实现分布式锁（应用篇）](https://spring.hhui.top/spring-blog/2020/10/30/201030-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8BRedis%E5%AE%9E%E7%8E%B0%E5%88%86%E5%B8%83%E5%BC%8F%E9%94%81%EF%BC%88%E5%BA%94%E7%94%A8%E7%AF%87%EF%BC%89/)


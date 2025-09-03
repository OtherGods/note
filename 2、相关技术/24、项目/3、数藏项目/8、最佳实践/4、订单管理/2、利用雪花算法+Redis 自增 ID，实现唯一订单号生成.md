[3、全局唯一订单号生成（分布式ID）](2、相关技术/24、项目/3、数藏项目/7、关键技术/3、全局唯一订单号生成（分布式ID）.md)

如上文，我们定义了一个全局的 ID 生成器——DistributeID，其中定义了方法——generateWithSnowflake

他就是借助雪花算法生成唯一 ID 的，这个方法的声明如下：
```java
/**  
 * 利用雪花算法生成一个唯一ID  
 */public static String generateWithSnowflake(BusinessCode businessCode,long workerId, String externalId) {  
    long id = IdUtil.getSnowflake(workerId).nextId();  
    return generate(businessCode, externalId, id);  
}
```

需要三个参数：
- BusinessCode businessCode
- 主要是区分业务的，比如订单号、支付单号、优惠券单号等等，不同的业务定义一个不同的 BusinessCode
- long workerId
- 用于区分不同的 worker，这个 woker 其实就是一个机器实例，我们需要能保证不同的机器上的 workerId 不一样。
- String externalId
- 这个就是一个业务单号，比如买家 ID，这个字段会用于基于基因法进行订单号生成，在其他文章中讲过了，我们不展开了。

本文主要介绍下这个workerId我们如何获取。

在我们的项目中，workerId 的获取我们是通过WorkerIdHolder实现的。
```java
@Component  
public class WorkerIdHolder implements CommandLineRunner {  
      
    @Autowired  
    private RedissonClient redissonClient;  
      
    public static long WORKER_ID;  
      
    @Override  
    public void run(String... args) throws Exception {  
        RAtomicLong atomicLong = redissonClient.getAtomicLong("workerId");  
        WORKER_ID = atomicLong.incrementAndGet() % 32;  
    }  
}
```

这个类实现了CommandLineRunner接口，那么在 Spring 容器启动的过程中，run方法就会被调用。

run 方法中的主要逻辑就是去 redis 中获取一个自增 id，然后我们再基于拿到的这个自增 id对32取模，就能得到一个 workerId 了。

为什么是32？主要是因为雪花算法对这个 workerId 有要求，不能超过32，否则会报错。

当我们有10台机器依次启动的过程中，就会获取到10个自增 id，比如是1000-1010吧，那么把他们对32取模就能得到一个10个不同的数字，就可以把这个数组保存在一个常量中，当作 workderId 来用了。

**但是，workerId不能超过32，如果机器数超过32，会有不同机器有相同workId，然后相同workId生成sequence会重复怎么办?**

确实超过32的话会重复，这个主要是Hutool框架的限制，但是影响也不大，因为workerId只是雪花算法中的一部分，很小的一部分。即使workerId重复的话，也要非常极端情况下（时间戳相同、随机数相同）才会导致整个id重复。

重复了咋办，插入数据库会失败的，直接提示下单失败，系统异常即可，这种发生的概率实在是太低了，没必要浪费感情去处理。


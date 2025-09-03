![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503052257774.png)

我们的项目在多个微服务中都需要进行幂等操作，我们把锁相关的操作封装在了分布式锁的组件中。这个分布式锁是基于 **`Redisson` + `Redis` + `自定义注解` + `AOP 切面`**

首先在pom文件中引入相关依赖
```xml
<?xml version="1.0" encoding="UTF-8"?>  
<project xmlns="http://maven.apache.org/POM/4.0.0"  
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">  
    <modelVersion>4.0.0</modelVersion>  
  
    <parent>  
        <groupId>cn.hollis</groupId>  
        <artifactId>nft-turbo-common</artifactId>  
        <version>1.0.0-SNAPSHOT</version>  
    </parent>  
  
    <groupId>cn.hollis</groupId>  
    <artifactId>nft-turbo-lock</artifactId>  
    <description>分布式锁组件</description>  
    <version>1.0.0-SNAPSHOT</version>  
  
    <properties>  
        <maven.compiler.source>21</maven.compiler.source>  
        <maven.compiler.target>21</maven.compiler.target>  
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    </properties>  
    <dependencies>  
        <!--        Redis  -->  
        <dependency>  
            <groupId>cn.hollis</groupId>  
            <artifactId>nft-turbo-cache</artifactId>  
        </dependency>  
  
    </dependencies>  
  
  
</project>
```

在nft-turbo-cache已经引入了redis的依赖
```xml
<!--     Redis  -->  
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-data-redis</artifactId>  
</dependency>  
  
<!--    Redisson    -->  
<dependency>  
    <groupId>org.redisson</groupId>  
    <artifactId>redisson-spring-boot-starter</artifactId>  
    <version>3.24.3</version>  
</dependency>
```

# 注解

定义切面类和注解
```java
/**  
 * 分布式锁切面  
 *  
 * @author hollis 
 */
@Aspect  
@Component  
public class DistributeLockAspect {  
      
}
```

- `@Aspect`：表示这个类是一个切面类，它封装了交叉关注点的逻辑。
- `@Component`：将这个类声明为Spring管理的bean，以便Spring AOP可以扫描到这个切面。

定义注解`@DistributeLock`
```java
/**  
 * 分布式锁注解  
 *  
 * @author Hollis 
 */
@Target(ElementType.METHOD)  
@Retention(RetentionPolicy.RUNTIME)  
public @interface DistributeLock {  
      
    /**  
     * 锁的场景  
     *  
     * @return     
     */
     public String scene();  
      
    /**  
     * 加锁的key，优先取key()，如果没有，则取keyExpression()  
     *     
     * @return     
     */
     public String key() default DistributeLockConstant.NONE_KEY;  
      
    /**  
     * SPEL表达式:  
     * <pre>  
     *     #id     
     *     #insertResult.id
     * </pre>  
     *     
     * @return
	 */
	 public String keyExpression() default DistributeLockConstant.NONE_KEY;  
      
    /**  
     * 超时时间，毫秒  
     * 默认情况下不设置超时时间，会自动续期  
     *  
     * @return     
     */
     public int expireTime() default DistributeLockConstant.DEFAULT_EXPIRE_TIME;  
      
    /**  
     * 加锁等待时长，毫秒  
     * 默认情况下不设置等待时长，不做等待  
     * @return  
     */
	 public int waitTime() default DistributeLockConstant.DEFAULT_WAIT_TIME;  
}
```

自定义的一个注解，用来标记需要分布式锁的方法

# 注解解析器

通过`@Around`通知来围绕目标方法执行，在方法执行之前和之后插入锁的逻辑
```java
/**  
 * 分布式锁切面  
 *  
 * @author hollis 
 */
@Aspect  
@Component  
public class DistributeLockAspect {  
      
    private RedissonClient redissonClient;  
      
    public DistributeLockAspect(RedissonClient redissonClient) {  
        this.redissonClient = redissonClient;  
    }  
      
    private static final Logger LOG = LoggerFactory.getLogger(DistributeLockAspect.class);  
      
    @Around("@annotation(cn.hollis.nft.turbo.lock.DistributeLock)")  
    public Object process(ProceedingJoinPoint pjp) throws Exception {  
        Object response = null;  
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();  
        DistributeLock distributeLock = method.getAnnotation(DistributeLock.class);  
          
        String key = distributeLock.key();  
        if (DistributeLockConstant.NONE_KEY.equals(key)) {  
            if (DistributeLockConstant.NONE_KEY.equals(distributeLock.keyExpression())) {  
                throw new DistributeLockException("no lock key found...");  
            }  
            SpelExpressionParser parser = new SpelExpressionParser();  
            Expression expression = parser.parseExpression(distributeLock.keyExpression());  
              
            EvaluationContext context = new StandardEvaluationContext();  
            // 获取参数值  
            Object[] args = pjp.getArgs();  
              
            // 获取运行时参数的名称  
            StandardReflectionParameterNameDiscoverer discoverer  
                    = new StandardReflectionParameterNameDiscoverer();  
            String[] parameterNames = discoverer.getParameterNames(method);  
              
            // 将参数绑定到context中  
            if (parameterNames != null) {  
                for (int i = 0; i < parameterNames.length; i++) {  
                    context.setVariable(parameterNames[i], args[i]);  
                }  
            }  
              
            // 解析表达式，获取结果  
            key = String.valueOf(expression.getValue(context));  
        }  
          
        String scene = distributeLock.scene();  
          
        String lockKey = scene + "#" + key;  
          
        int expireTime = distributeLock.expireTime();  
        int waitTime = distributeLock.waitTime();  
        RLock rLock = redissonClient.getLock(lockKey);  
        boolean lockResult = false;  
        if (waitTime == DistributeLockConstant.DEFAULT_WAIT_TIME) {  
            if (expireTime == DistributeLockConstant.DEFAULT_EXPIRE_TIME) {  
                LOG.info(String.format("lock for key : %s", lockKey));  
                rLock.lock();  
            } else {  
                LOG.info(String.format("lock for key : %s , expire : %s", lockKey, expireTime));  
                rLock.lock(expireTime, TimeUnit.MILLISECONDS);  
            }  
            lockResult = true;  
        } else {  
            if (expireTime == DistributeLockConstant.DEFAULT_EXPIRE_TIME) {  
                LOG.info(String.format("try lock for key : %s , wait : %s", lockKey, waitTime));  
                lockResult = rLock.tryLock(waitTime, TimeUnit.MILLISECONDS);  
            } else {  
                LOG.info(String.format("try lock for key : %s , expire : %s , wait : %s", lockKey, expireTime, waitTime));  
                lockResult = rLock.tryLock(waitTime, expireTime, TimeUnit.MILLISECONDS);  
            }  
        }  
          
        if (!lockResult) {  
            LOG.warn(String.format("lock failed for key : %s , expire : %s", lockKey, expireTime));  
            throw new DistributeLockException("acquire lock failed... key : " + lockKey);  
        }  
          
        try {  
            LOG.info(String.format("lock success for key : %s , expire : %s", lockKey, expireTime));  
            response = pjp.proceed();  
        } catch (Throwable e) {  
            throw new Exception(e);  
        } finally {  
            rLock.unlock();  
            LOG.info(String.format("unlock for key : %s , expire : %s", lockKey, expireTime));  
        }  
        return response;  
    }  
}
```

当`key`为`NONE_KEY`时，通过SpEL表达式，需要解析该表达式获得最终的`key`
```java
if (DistributeLockConstant.NONE_KEY.equals(key)) {  
    if (DistributeLockConstant.NONE_KEY.equals(distributeLock.keyExpression())) {  
        throw new DistributeLockException("no lock key found...");  
    }  
    SpelExpressionParser parser = new SpelExpressionParser();  
    Expression expression = parser.parseExpression(distributeLock.keyExpression());  
      
    EvaluationContext context = new StandardEvaluationContext();  
    // 获取参数值  
    Object[] args = pjp.getArgs();  
  
    // 获取运行时参数的名称  
    StandardReflectionParameterNameDiscoverer discoverer  
            = new StandardReflectionParameterNameDiscoverer();  
    String[] parameterNames = discoverer.getParameterNames(method);  
  
    // 将参数绑定到context中  
    if (parameterNames != null) {  
        for (int i = 0; i < parameterNames.length; i++) {  
            context.setVariable(parameterNames[i], args[i]);  
        }  
    }  
  
    // 解析表达式，获取结果  
    key = String.valueOf(expression.getValue(context));  
}
```

尝试获取锁
```java
int expireTime = distributeLock.expireTime();  
int waitTime = distributeLock.waitTime();  
RLock rLock = redissonClient.getLock(lockKey);  
boolean lockResult = false;  
if (waitTime == DistributeLockConstant.DEFAULT_WAIT_TIME) {  
    if (expireTime == DistributeLockConstant.DEFAULT_EXPIRE_TIME) {  
        LOG.info(String.format("lock for key : %s", lockKey));  
        rLock.lock();  
    } else {  
        LOG.info(String.format("lock for key : %s , expire : %s", lockKey, expireTime));  
        rLock.lock(expireTime, TimeUnit.MILLISECONDS);  
    }  
    lockResult = true;  
} else {  
    if (expireTime == DistributeLockConstant.DEFAULT_EXPIRE_TIME) {  
        LOG.info(String.format("try lock for key : %s , wait : %s", lockKey, waitTime));  
        lockResult = rLock.tryLock(waitTime, TimeUnit.MILLISECONDS);  
    } else {  
        LOG.info(String.format("try lock for key : %s , expire : %s , wait : %s", lockKey, expireTime, waitTime));  
        lockResult = rLock.tryLock(waitTime, expireTime, TimeUnit.MILLISECONDS);  
    }  
}  
  
if (!lockResult) {  
    LOG.warn(String.format("lock failed for key : %s , expire : %s", lockKey, expireTime));  
    throw new DistributeLockException("acquire lock failed... key : " + lockKey);  
}  
  
try {  
    LOG.info(String.format("lock success for key : %s , expire : %s", lockKey, expireTime));  
    response = pjp.proceed();  
} catch (Throwable e) {  
    throw new Exception(e);  
} finally {  
    rLock.unlock();  
    LOG.info(String.format("unlock for key : %s , expire : %s", lockKey, expireTime));  
}  
return response;
```

通过AOP切面实现分布式锁可以在不修改原有方法的情况下，为方法添加分布式锁的功能，从而实现了代码解耦、增强可维护性

# 使用方式

```java
@Override  
@DistributeLock(keyExpression = "#request.identifier", scene = "ORDER_CREATE")  
@Facade  
public OrderResponse create(OrderCreateRequest request) {  
    try {  
        orderValidatorChain.validate(request);  
    } catch (Exception e) {  
        return new OrderResponse.OrderResponseBuilder().buildFail(ORDER_CREATE_VALID_FAILED.getCode(), e.getMessage());  
    }  
      
    Boolean preDeductResult = inventoryWrapperService.preDeduct(request);  
    if (preDeductResult) {  
        return orderService.create(request);  
    }  
    throw new OrderException(OrderErrorCode.INVENTORY_DEDUCT_FAILED);  
}
```

# Redis数据结构

分布式锁对应的数据结构如下，解释下图数据结构：
1. ①：由 `ORDER_CREATE` + `#` + `token/get`接口返回的token组成的键
2. ②：由 `随机ID` + `:` + **当前线程ID** 组成的key，以及当前线程获取该锁的次数1次
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508260233739.png)

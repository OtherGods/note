在我们的项目中，为了避免订单的重复下单，我们用了 token 的机制，这个 token 是用户访问页面的时候获取到的。

然后再真正下单的时候再把这个 token 给删除，确保他只能用一次。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202502161644786.png)

同时，这个 token 我们也会当作订单创建时候的幂等号来用，一方面他是唯一的，可以帮我们做幂等判断，另外借助他也能实现 Redis 库存和订单的一致性核对。

所以，我们需要把这个 token 传递下去。但是 token 的传递是在 Header 中的，在 tokenFilter 中校验完成后，Redis 中的 token 就删除了。那么我们如何在后续的订单创建过程中拿到这个 token 呢？

我们当然也可以在 controller 中解析 header，从 header 中取。但是其实有一个更好的方案，那就是把他存在 ThreadLocal 中，这样在当前线程中，任意时刻都可以获取到这个 token 来使用。

我们在 TokenFilter 定义了一个tokenThreadLocal。
```java
public class TokenFilter implements Filter {
    public static final ThreadLocal<String> tokenThreadLocal = new ThreadLocal<>();
}
```

然后再做 token 校验的时候把 token 放到这个 tl 中，如以下代码的第16行。
```java
private boolean checkTokenValidity(String token) {  
    String luaScript = """  
        local value = redis.call('GET', KEYS[1])        redis.call('DEL', KEYS[1])        return value""";  
      
    // 6.2.3以上可以直接使用GETDEL命令  
    // String value = (String) redisTemplate.opsForValue().getAndDelete(token);  
    String result = (String) redissonClient.getScript().eval(RScript.Mode.READ_WRITE,  
            luaScript,  
            RScript.ReturnType.STATUS,  
            Arrays.asList(token));  
      
    tokenThreadLocal.set(result);  
    return result != null;  
}
```

然后再使用的时候，及后续的过程中，如tradeController 的 buy 方法中就可以直接用这个 tl 来获取了：
```java
@PostMapping("/buy")  
public Result<String> buy(@Valid @RequestBody BuyParam buyParam) {  
    String userId = (String) StpUtil.getLoginId();  
    //创建订单  
    OrderCreateRequest orderCreateRequest = new OrderCreateRequest();  
    orderCreateRequest.setIdentifier(tokenThreadLocal.get());  
}
```

如上面的第6行，就是从tokenThreadLocal中获取到这个 token 当作幂等号使用。



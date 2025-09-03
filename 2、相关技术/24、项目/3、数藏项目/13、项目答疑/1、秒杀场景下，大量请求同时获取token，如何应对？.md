[1、基于Token校验避免订单重复提交](2、相关技术/24、项目/3、数藏项目/8、最佳实践/2、秒杀（热点扣减）/1、基于Token校验避免订单重复提交.md)

上面介绍了token防止订单重复创建的方案。

但是大家有没有考虑过，如果秒杀场景，用户进入详情页就会获取一次token ，那会不会有大量的请求去获取token呢？如何扛得住呢？

其实，这个我们可以做个优化，你不如淘宝，他并不是在详情页的时候就获取token，而是在订单确认页做的token获取。

订单确认页就是这个页面：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503122241983.png)

在这个页面，用户需要确认收货地址、快递方式、优惠等等信息。

那么我们可以在这个页面唤起的时候再获取token，那么就能过滤掉很多只访问详情页，但是不下单的用户的token获取的请求。能大大降低这个数据量。

另外还有一个方案 ，**那就是我们通过限制，让同一个用户在同一个商品下同时只能获取同一个token，来避免他同时针对同一个热点商品进行提前获取token批量下单**。

这个方案是我们在之前的方案（[https://thoughts.aliyun.com/workspaces/6655879cf459b7001ba42f1b/docs/6673ed274c6050000199e4c3?spm=a2cl9.thoughts_devops2020_goldlog_.0.0.a9df670dOtVyO5](https://thoughts.aliyun.com/workspaces/6655879cf459b7001ba42f1b/docs/6673ed274c6050000199e4c3?spm=a2cl9.thoughts_devops2020_goldlog_.0.0.a9df670dOtVyO5)） 的基础上做的优化。

主要是token获取的地方做了改造。这次改造的Commit内容如下：

https://codeup.aliyun.com/66263f57d833774c93cc0418/NFTurbo/NFTurbo_Server/commit/2657f2bfd091fbf664f796c5f1d0415f0ad9e9c5?branch=master

其中主要是获取token的接口，实现逻辑如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507021540239.png)

因为`tokenKey`是基于`scene`、`userId`和`key`拼接而成的，所以他和用户+商品强相关，**同一个key向redis进行set的时候会覆盖**，这样就能确保，如果有新的`Token`生成了， 老的就会失效了。这样就保证了，同一个用户在同一商品下，同时只能有一个有效的token。

`TokenController`代码如下：
```java
@GetMapping("/get")
public Result<String> get(@NotBlank String scene, @NotBlank String key) {
    if (StpUtil.isLogin()) {
        String userId = (String) StpUtil.getLoginId();
        
        //token:buy:29:10085
        String tokenKey = TOKEN_PREFIX + scene + CACHE_KEY_SEPARATOR + userId + CACHE_KEY_SEPARATOR + key;
        // tokenValue = SecureUtil.aes(密钥).encryptBase64(tokenKey + ":" + UUID);
        String tokenValue = TokenUtil.getTokenValueByKey(tokenKey);
        
        //key：token:buy:29:10010
        //未加密value：token:buy:29:10010:dd2ddd68-1fab-4821-ad42-dea4fba68d51
        //加密value：YZdkYfQ8fy7biSTsS5oZrSI8wg3ukOYpPOPeVe2xndToDIULwDxLAdYOcfhQ5FC9D4o4ETS80zOVI5oJKikHhQ==
        stringRedisTemplate.opsForValue().set(tokenKey, tokenValue, 30, TimeUnit.MINUTES);
        return Result.success(tokenValue);
    }
    throw new AuthException(AuthErrorCode.USER_NOT_LOGIN);
}
```
上面这段代码核心原理如下：
1. Redis中存储**token摘要**与**token**的映射
2. 前端将获取到的`token`传递给后端，后端解密后用摘要算法`TokenUtil#getTokenValueByKey`获取到摘要（也就是Redis中的key），进而从redis中获取到token
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508242140058.png)

`TokenUtil`代码如下：
```java
import cn.hutool.crypto.SecureUtil;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static cn.hollis.nft.turbo.cache.constant.CacheConstant.CACHE_KEY_SEPARATOR;

public class TokenUtil {

    private static final String TOEKN_AES_KEY = "xxx保密，详见代码";

    public static final String TOKEN_PREFIX = "token:";

    public static String getTokenValueByKey(String tokenKey) {
        if (tokenKey == null) {
            return null;
        }
        String uuid = UUID.randomUUID().toString();
        //token:buy:29:10085:5ac6542b-64b1-4d41-91b9-e6c55849bb7f
        String tokenValue = tokenKey + CACHE_KEY_SEPARATOR + uuid;

        //YZdkYfQ8fy7biSTsS5oZrbsB8eN7dHPgtCV0dw/36AHSfDQzWOj+ULNEcMluHvep/txjP+BqVRH3JlprS8tWrQ==
        return SecureUtil.aes(TOEKN_AES_KEY.getBytes(StandardCharsets.UTF_8)).encryptBase64(tokenValue);
    }
}
```

这里面针对TokenValue做了AES加密，来避免被用户找到规律进行拼接。

需要注意的是，`AES`的key需要是16位的。否则加解密会报错。

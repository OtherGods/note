# 配置及代码解释

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503091714278.png)

`Sa-Token` 是一个强大的 Java 权限认证框架，通过配置文件可以很方便地实现各种功能，包括用户登录、权限控制、会话管理等。

我们的项目中 `gateway` 和 `auth` 这两个应用都会依赖这个组件。

配置文件
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
    <artifactId>nft-turbo-sa-token</artifactId>  
    <version>1.0.0-SNAPSHOT</version>  
    <description>sa token</description>  
  
    <properties>  
        <maven.compiler.source>21</maven.compiler.source>  
        <maven.compiler.target>21</maven.compiler.target>  
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    </properties>  
  
    <dependencies>  
        <!-- Sa-Token 权限认证 -->  
        <dependency>  
            <groupId>cn.dev33</groupId>  
            <artifactId>sa-token-spring-boot3-starter</artifactId>  
            <version>1.37.0</version>  
        </dependency>  
  
        <dependency>  
            <groupId>cn.dev33</groupId>  
            <artifactId>sa-token-redis-jackson</artifactId>  
            <version>1.37.0</version>  
        </dependency>  
  
        <dependency>  
            <groupId>org.apache.commons</groupId>  
            <artifactId>commons-pool2</artifactId>  
        </dependency>  
    </dependencies>  
  
</project>
```

1. 自动配置: `Sa-Token` 提供了 Spring Boot Starter，可以自动配置大部分必要的组件。通过引入 sa-token-spring-boot3-starter 依赖，框架会自动扫描和配置所需的 Bean 和组件。
```xml
<dependency>  
    <groupId>cn.dev33</groupId>  
    <artifactId>sa-token-spring-boot3-starter</artifactId>  
    <version>1.37.0</version>  
</dependency>
```

2. 默认中间件和拦截器: `Sa-Token` 自带一系列默认中间件和拦截器，比如**登录验证**、**权限验证**等，能够自动应用到项目的所有请求中

3. 多种储存策略: `Sa-Token` 支持多种会话储存策略，包括**内存（默认）**、**`Redis`**、**`MySQL`** 等。通过简单的配置，即可切换不同的储存方式
   由默认内存存储切换为Redis存储的步骤：
   1. 添加`sa-token-dao-redis-jackson`依赖
   2. 配置 Redis 连接信息
```xml
<dependency>  
    <groupId>cn.dev33</groupId>  
    <artifactId>sa-token-redis-jackson</artifactId>  
    <version>1.37.0</version>  
</dependency>
```

在`auth`的`application.yml` 中添加 `Sa-Token` 的相关配置
```yml
sa-token:  
  # token 名称（同时也是 cookie 名称）  
  token-name: satoken  
  # token 有效期（单位：秒） 默认30天，-1 代表永久有效  
  timeout: 2592000  
  # token 最低活跃频率（单位：秒），如果 token 超过此时间没有访问系统就会被冻结，默认-1 代表不限制，永不冻结  
  active-timeout: -1  
  # 是否允许同一账号多地同时登录 （为 true 时允许一起登录, 为 false 时新登录挤掉旧登录）  
  is-concurrent: true  
  # 在多人登录同一账号时，是否共用一个 token （为 true 时所有登录共用一个 token, 为 false 时每次登录新建一个 token）  
  is-share: true  
  # token 风格（默认可取值：uuid、simple-uuid、random-32、random-64、random-128、tik）  
  token-style: uuid  
  # 是否输出操作日志  
  is-log: true

spring:  
  data:  
    redis:  
      host: ${nft.turbo.redis.url}  
      port: ${nft.turbo.redis.port}  
#      password: ${nft.turbo.redis.password}  
      ssl:  
        enabled: true
```

在需要登录的控制器方法上即可使用
```java
// 7天
private static final Integer DEFAULT_LOGIN_SESSION_TIMEOUT = 60 * 60 * 24 * 7;

// 登录
StpUtil.login(userInfo.getUserId(), new SaLoginModel().setIsLastingCookie(loginParam.getRememberMe())
        .setTimeout(DEFAULT_LOGIN_SESSION_TIMEOUT));
StpUtil.getSession().set(userInfo.getUserId().toString(), userInfo);
LoginVO loginVO = new LoginVO(userInfo);
return Result.success(loginVO);

public class LoginVO implements Serializable {  
    private static final long serialVersionUID = 1L;  
    /**用户标识，如用户ID*/    
    private String userId;  
    /**访问令牌，由sa-token生成的一个UUID*/  
    private String token;  
    /**令牌过期时间*/  
    private Long tokenExpiration;
    …………
}

------------------------------------------------------
// 登出
StpUtil.logout();
```

代码解释：`StpUtil.login(userInfo.getUserId(), new SaLoginModel().setIsLastingCookie(loginParam.getRememberMe()).setTimeout(DEFAULT_LOGIN_SESSION_TIMEOUT)); `
- `new SaLoginModel().setIsLastingCookie(loginParam.getRememberMe()).setTimeout(DEFAULT_LOGIN_SESSION_TIMEOUT)`：用于指定将要存储到`redis`中的key的过期时间
- 前端`cookie`的过期时间为代码中指定的`DEFAULT_LOGIN_SESSION_TIMEOUT`
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508232336071.png)

## Redis中存储的数据结构

**注：用户id为`29`**

`StpUtil#login(id, loginModel)`方法会创建以下两个结构：

1. **String结构**：Redis中记录用户信息的键值对，以`前缀:用户ID`为键，由`创建时间、用户信息、用UUID生成的随机token、过期时间等拼接好的字符串`为值
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508232236079.png)
2. **String结构**：以`前缀:UUID随机生成的token`为键，`用户ID`为值(也就是29)；Java代码`StpUtil.isLogin()`查询的就是这个结构；
   同时会将`UUID随机生成的token`作为响应头中存储的`cookie`返回给前端，同时将这个值最为返回值存储到前端的`store`中

`Redis`中键`satoken:login:session:29`对应的值读取到内存中是一个SaSession对象，可以通过Java代码`StpUtil.getSession()`获取，以下是内存对象与Redis中存储数据的映射：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508232354847.png)

# 自动延期

Sa-Token 主要通过其**两种有效期机制**：

## 配置自动续期

`sa-token`配置中的`timeout`、`activity-timeout`
- `timeout`：用于设置可以时指定key的过期时间
- `activity-timeout`：自动续期

**对比**：

| 特性维度       | timeout (长期有效期)                          | activity-timeout (临时有效期/活跃有效期)                 |
| ---------- | ---------------------------------------- | ---------------------------------------------- |
| **定义**     | Token 的**绝对最大生存时间**，从登录开始计算              | Token 的**最大无操作存活时间**，每次活跃操作后重置                 |
| **是否可续期**  | ❌ **不可续期**，到期后 Token 必定失效，必须重新登录         | ✅ **可自动续期**，用户有操作时自动重置倒计时                      |
| **配置值示例**  | `2592000` (30天)                          | `1800` (30分钟)                                  |
| **配置为 -1** | Token **永不过期** (需谨慎使用，可能导致 Redis 无效数据积累) | Token **不会因无操作而过期**                            |
| **优先级**    | 高                                        | 低 (即使 `activity-timeout` 未过期，`timeout` 到期也会失效) |
| **典型应用场景** | 要求用户定期重新登录以保障安全                          | 保证用户活跃会话，自动踢掉不活跃的用户                            |

当在配置中设置了 `activity-timeout` 大于 0 的值（如 `1800`），并且 **`autoRenew` 配置为 true（默认值）** 时，Sa-Token 会在每次以下操作时自动检查并刷新 `Token` 的最后活跃时间：
- **间接调用**：任何调用了 `StpUtil.getLoginId()`、`StpUtil.checkLogin()` 等会触发生成登录ID的方法。
- **直接调用**：你手动调用了 `StpUtil.updateLastActivityToNow()`。
这意味着，只要用户在使用应用（有请求触发了上述操作），其 Token 的 `activity-timeout` 有效期就会不断重置，保持会话活跃。

## 手动调用API续期

1. `ttl` + `expire`
2. `sa-tken` `API`

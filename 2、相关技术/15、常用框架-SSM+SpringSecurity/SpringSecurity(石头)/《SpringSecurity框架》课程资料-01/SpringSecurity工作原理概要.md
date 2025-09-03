# 1.SpringSecurity过滤器链

SpringSecurity 采用的是 **==责任链==** 的设计模式，它有一条很长的过滤器链。现在对这条过滤器链的各个进行说明:

| 过滤器名                                     | 作用                                                                                                                                                                               |
| ---------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| WebAsyncManagerIntegrationFilter         | 将 Security 上下文与 Spring Web 中用于处理异步请求映射的 WebAsyncManager 进行集成。                                                                                                                    |
| SecurityContextPersistenceFilter         | 在每次请求处理之前将该请求相关的安全上下文信息加载到 SecurityContextHolder 中，然后在该次请求处理完成之后，将 SecurityContextHolder 中关于这次请求的信息存储到一个“仓储”中，然后将 SecurityContextHolder 中的信息清除，例如在Session中维护一个用户的安全信息就是这个过滤器处理的。 |
| HeaderWriterFilter                       | 用于将头信息加入响应中                                                                                                                                                                      |
| **CsrfFilter**                           | 用于处理跨站请求伪造                                                                                                                                                                       |
| **LogoutFilter**                         | 用于处理退出登录                                                                                                                                                                         |
| **UsernamePasswordAuthenticationFilter** | 用于处理基于表单的登录请求，从表单中获取用户名和密码。默认情况下处理来自 /login 的请求。从表单中获取用户名和密码时，默认使用的表单 name 值为 username 和 password，这两个值可以通过设置这个过滤器的usernameParameter 和 passwordParameter 两个参数的值进行修改               |
| **DefaultLoginPageGeneratingFilter**     | 如果没有配置登录页面，那系统初始化时就会配置这个过滤器，并且用于在需要进行登录时生成一个登录表单页面                                                                                                                               |
| BasicAuthenticationFilter                | 检测和处理 http basic 认证                                                                                                                                                              |
| RequestCacheAwareFilter                  | 用来处理请求的缓存                                                                                                                                                                        |
| SecurityContextHolderAwareRequestFilter  | 主要是包装请求对象request                                                                                                                                                                 |
| AnonymousAuthenticationFilter            | 检测 SecurityContextHolder 中是否存在 Authentication 对象，如果不存在为其提供一个匿名 Authentication                                                                                                    |
| **SessionManagementFilter**              | 管理 session 的过滤器                                                                                                                                                                  |
| ExceptionTranslationFilter               | 处理 AccessDeniedException 和 AuthenticationException 异常                                                                                                                            |
| FilterSecurityInterceptor                | 可以看做过滤器链的出口                                                                                                                                                                      |
| **RememberMeAuthenticationFilter**       | 当用户没有登录而直接访问资源时, 从 cookie 里找出用户的信息, 如果 Spring Security 能够识别出用户提供的remember me cookie, 用户将不必填写用户名和密码, 而是直接登录进入系统，该过滤器默认不开启                                                         |

# 2.SpringSecurity 流程图
先来看下面一个 `SpringSecurity` 执行流程图，只要把 `SpringSecurity` 的执行过程弄明白了，这个框架就会变得很简单:
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508231713769.png)

# 3.Spring Security 核心原理详解

Spring Security 是一个功能强大且高度可定制的身份验证和访问控制框架，它的核心原理基于一系列过滤器链和委托模式。下面我将详细解释其工作原理。

## 核心架构图

![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508231811564.png)

## 核心组件和工作原理

### 1. 过滤器链 (Filter Chain)

Spring Security 的核心是一个**过滤器链**，它由多个过滤器组成，每个过滤器负责特定的安全功能：
```java
// 简化的过滤器链概念
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    // 1. 安全检查 (CORS, CSRF等)
    // 2. 认证处理
    // 3. 授权检查
    // 4. 调用后续过滤器或目标资源
}
```

关键过滤器包括：
- **CorsFilter**: 处理跨域请求
- **CsrfFilter**: 防止CSRF攻击
- **UsernamePasswordAuthenticationFilter**: 处理表单登录
- **BasicAuthenticationFilter**: 处理HTTP基本认证
- **FilterSecurityInterceptor**: 进行最终的访问控制决策

### 2. 认证过程 (Authentication)

认证是验证用户身份的过程：
```java
// 简化的认证流程
public Authentication attemptAuthentication(HttpServletRequest request, 
                                            HttpServletResponse response) {
    // 1. 从请求中提取凭证 (用户名/密码等)
    String username = obtainUsername(request);
    String password = obtainPassword(request);
    
    // 2. 创建认证令牌
    UsernamePasswordAuthenticationToken authRequest = 
        new UsernamePasswordAuthenticationToken(username, password);
    
    // 3. 委托给AuthenticationManager进行认证
    return this.getAuthenticationManager().authenticate(authRequest);
}
```

#### AuthenticationManager 和 AuthenticationProvider

```java
// AuthenticationManager 接口
public interface AuthenticationManager {
    Authentication authenticate(Authentication authentication)
            throws AuthenticationException;
}

// ProviderManager 是主要实现
public class ProviderManager implements AuthenticationManager {
    private List<AuthenticationProvider> providers;
    
    public Authentication authenticate(Authentication authentication) {
        // 遍历所有AuthenticationProvider尝试认证
        for (AuthenticationProvider provider : providers) {
            if (provider.supports(authentication.getClass())) {
                return provider.authenticate(authentication);
            }
        }
        throw new ProviderNotFoundException("No AuthenticationProvider found");
    }
}
```

常见的 AuthenticationProvider:
- **DaoAuthenticationProvider**: 基于数据库的认证
- **JwtAuthenticationProvider**: JWT令牌认证
- **LdapAuthenticationProvider**: LDAP认证

### 3. 授权过程 (Authorization)

授权是决定已认证用户是否有权访问特定资源的过程：

```java
// 简化的授权流程
public void invoke(FilterInvocation filterInvocation) throws IOException, ServletException {
    // 1. 获取安全配置
    Collection<ConfigAttribute> attributes = this.obtainSecurityMetadataSource()
            .getAttributes(filterInvocation);
    
    // 2. 如果不需要授权，直接放行
    if (attributes == null || attributes.isEmpty()) {
        filterInvocation.getChain().doFilter(filterInvocation.getRequest(), 
                                            filterInvocation.getResponse());
        return;
    }
    
    // 3. 检查认证
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    // 4. 委托给AccessDecisionManager进行授权决策
    this.accessDecisionManager.decide(authentication, filterInvocation, attributes);
    
    // 5. 允许访问
    filterInvocation.getChain().doFilter(filterInvocation.getRequest(), 
                                        filterInvocation.getResponse());
}
```

#### AccessDecisionManager 和投票机制

```java
// AccessDecisionManager 接口
public interface AccessDecisionManager {
    void decide(Authentication authentication, Object object,
                Collection<ConfigAttribute> configAttributes)
            throws AccessDeniedException, InsufficientAuthenticationException;
}

// 基于投票的实现
public class AffirmativeBased implements AccessDecisionManager {
    private List<AccessDecisionVoter<?>> decisionVoters;
    
    public void decide(Authentication authentication, Object object,
                      Collection<ConfigAttribute> configAttributes) {
        int deny = 0;
        
        // 遍历所有投票器
        for (AccessDecisionVoter voter : decisionVoters) {
            int result = voter.vote(authentication, object, configAttributes);
            
            if (result == AccessDecisionVoter.ACCESS_GRANTED) {
                return; // 有一个同意就允许访问
            } else if (result == AccessDecisionVoter.ACCESS_DENIED) {
                deny++;
            }
        }
        
        if (deny > 0) {
            throw new AccessDeniedException("Access is denied");
        }
        
        // 如果没有投票器表态，且允许弃权，则抛出异常
        throw new AccessDeniedException("Access is denied");
    }
}
```

常见的投票器:
- **RoleVoter**: 基于角色的投票器
- **AuthenticatedVoter**: 处理特殊标识如 IS_AUTHENTICATED_FULLY
- **WebExpressionVoter**: 处理Web表达式投票

### 4. 安全上下文 (SecurityContext)

安全上下文持有当前已认证用户的信息：

```java
// SecurityContextHolder 存储安全上下文
public class SecurityContextHolder {
    private static SecurityContextStrategy strategy;
    
    public static SecurityContext getContext() {
        return strategy.getContext();
    }
    
    public static void setContext(SecurityContext context) {
        strategy.setContext(context);
    }
}

// 安全上下文包含认证信息
public interface SecurityContext extends Serializable {
    Authentication getAuthentication();
    void setAuthentication(Authentication authentication);
}
```

#### SecurityContextHolder 的存储策略

Spring Security 将认证信息（Authentication）存储在 `SecurityContextHolder` 中，而其底层存储策略是可配置的：
```java
// Spring Security 提供的几种存储策略
public class SecurityContextHolder {
    public static final String MODE_THREADLOCAL = "MODE_THREADLOCAL";
    public static final String MODE_INHERITABLETHREADLOCAL = "MODE_INHERITABLETHREADLOCAL";
    public static final String MODE_GLOBAL = "MODE_GLOBAL";
    // ...
}
```

1. `MODE_THREADLOCAL`：线程隔离，**这是默认策略**，使用 `ThreadLocal` 存储安全上下文
2. `MODE_INHERITABLETHREADLOCAL`：安全上下文可以在父子线程间传递

可以通过系统属性配置：`-Dspring.security.strategy=MODE_INHERITABLETHREADLOCAL`

### 5. 配置原理

Spring Security 的配置基于建造者模式：

```java
// 典型的配置示例
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
            .logout()
                .permitAll();
    }
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .inMemoryAuthentication()
                .withUser("user").password("{noop}password").roles("USER")
                .and()
                .withUser("admin").password("{noop}admin").roles("ADMIN");
    }
}
```

## 完整工作流程

1. **请求到达**: HTTP请求到达应用程序
2. **过滤器处理**: 经过Spring Security过滤器链
3. **认证检查**: 检查请求是否包含认证信息
4. **认证处理**: 如果有认证信息，验证其有效性
5. **上下文设置**: 将认证信息存入SecurityContext
6. **授权检查**: 检查用户是否有权访问请求的资源
7. **访问决策**: 基于配置的规则和投票机制决定是否允许访问
8. **请求处理**: 如果允许访问，请求到达目标资源；否则返回错误

## 关键设计模式

1. **责任链模式**: 过滤器链处理请求
2. **委托模式**: AuthenticationManager委托给AuthenticationProvider
3. **策略模式**: 不同的认证和授权策略
4. **建造者模式**: 流畅的配置API

## 总结

Spring Security 的核心原理是基于过滤器链的请求处理，通过认证管理器和访问决策管理器实现身份验证和授权控制。其强大之处在于高度模块化和可扩展的设计，允许开发者自定义几乎每一个组件来满足特定的安全需求。

理解这些核心原理有助于更好地使用和定制Spring Security，构建安全可靠的应用程序。
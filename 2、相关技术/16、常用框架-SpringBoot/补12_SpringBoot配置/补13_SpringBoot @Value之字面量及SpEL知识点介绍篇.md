
承接上一篇博文 [补12_SpringBoot之自定义配置源的使用姿势](2、相关技术/16、常用框架-SpringBoot/补12_SpringBoot配置/补12_SpringBoot之自定义配置源的使用姿势.md) 中提及到但没有细说的知识点，这一篇博文将来看一下`@Value`除了绑定配置文件中的属性配置之外，另外支持的两种姿势

- 字面量表达式支持
- SpEL语法支持

# 1、项目环境
## 1.1、项目依赖

本项目借助`SpringBoot 2.2.1.RELEASE` + `maven 3.5.3` + `IDEA`进行开发

开一个web服务用于测试
```xml
<dependencies>  
    <dependency>  
        <groupId>org.springframework.boot</groupId>  
        <artifactId>spring-boot-starter-web</artifactId>  
    </dependency>  
</dependencies>
```

# 2、@Value知识点

上一篇的博文知道通过`${}`可以获取配置文件中对应的配置值，接下来我们看一下另外两种常见的姿势

## 2.1、字面量

字面量的使用比较简单，直接在`@Value`注解中写常量

一个demo如下
```java
@Value("1 + 2")  
private String common;
```

上面这种初始化之后，common的值会是 `1 + 2`；如果只是这种用法，这个东西就有些鸡肋了，我直接赋值不香嘛，为啥还有这样多此一举呢？

当然现实中（至少我有限的代码接触中），纯上面这种写法的不多，更常见的是下面这种
```java
@Value("demo_${auth.jwt.token}")  
private String prefixConf;
```

字面量 + 配置联合使用，如我们的配置文件值为
```java
auth:  
  jwt:  
    token: TOKEN.123
```

上面的prefixConf的取值，实际为 `demo_TOKEN.123`

## 2.2、SpEL表达式

@Value另外一个很强的使用姿势是支持SpEL表达式，至于SpEL是什么鬼，推荐查看 [补14_SpEL语法扫盲与查询手册](2、相关技术/16、常用框架-SpringBoot/补12_SpringBoot配置/补14_SpEL语法扫盲与查询手册.md)

### 2.2.1、基本姿势

使用姿势是 `#{}`，表示这个大括弧里面的走SpEL表达式，如下
```java
/**  
 * 字符串  
 */  
@Value("#{'abcd'}")  
private String spelStr;  
  
/**  
 * 基本计算  
 */  
@Value("#{1 + 2}")  
private String spelVal3;  
  
/**  
 * 列表  
 */  
@Value("#{{1, 2, 3}}")  
private List<Integer> spelList;  
  
/**  
 * map  
 */  
@Value("#{{a: '123', b: 'cde'}}")  
private Map spelMap;
```

上面是几个基本的case了，字面量，表达式，列表/Map等，SpEL的基本使用姿势与扫盲博文中的没有什么区别，无外乎就是在外层多了一个`#{}`

当然如果仅仅只是介绍上面几个的话，就有点单调了，SpEL一个比较强大的就是可以访问bean的属性/方法，这就给了我们很多的想像空间了

### 2.2.2、调用静态方法

在上面这个配置类`com.git.hui.boot.properties.value.config.SpelProperties`中添加一个静态方法
```java
public static String uuid() {  
    return "spel_" + UUID.randomUUID().toString().replaceAll("_", ".");  
}
```

然后我们尝试调用它
```java
/**  
 * 调用静态方法  
 */  
@Value("#{T(com.git.hui.boot.properties.value.config.SpelProperties).uuid()}")  
private String spelStaticMethod;
```

这样`spelStaticMethod`就会是一个 `"spel_"` 开头的随机字符串了

**请注意：如果在你的实际生产项目中，写出这样的代码，那多半意味着离找下家不远了**

### 2.2.3、嵌套使用

接下来借助SpEL与配置绑定的嵌套使用，来稍微调整下上面的实现（实际上下面这种用法也不常见，虽然没问题，但这种代码就属于写时一时爽，维护火葬场了🙄）
```java
/**  
 * 调用静态方法  
 */  
@Value("#{T(com.git.hui.boot.properties.value.config.SpelProperties).uuid('${auth.jwt.token}_')}")  
private String spelStaticMethod;  
  
public static String uuid(String prefix) {  
    return prefix + UUID.randomUUID().toString().replaceAll("_", ".");  
}
```

关于嵌套使用，下面再给出一个基础的使用姿势，供打开思路用
```java
/**  
 * 嵌套使用，从配置中获取值，然后执行SpEL语句  
 */  
@Value("#{'${auth.jwt.token}'.substring(2)}")  
private String spelLen;
```

### 2.2.4、Bean方法调用

最后再来一个访问bean的方法的case

定义一个Service
```java
@Service  
public class RandomService {  
    private AtomicInteger cnt = new AtomicInteger(1);  
  
    public String randUid() {  
        return cnt.getAndAdd(1) + "_" + UUID.randomUUID().toString();  
    }  
}
```

一个使用的姿势如下
```java
/**  
 * bean 方法访问  
 */  
@Value("#{randomService.randUid()}")  
private String spelBeanMethod;
```

## 2.3、测试

最后给出一个注入的结果输出，查看下有没有什么偏离预期的场景
```java
@RestController  
@SpringBootApplication  
public class Application {  
  
    @Autowired  
    private SpelProperties spelProperties;  
  
    @GetMapping("spel")  
    public SpelProperties showSpel() {  
        return spelProperties;  
    }  
  
    public static void main(String[] args) {  
        SpringApplication.run(Application.class);  
    }  
}
```

```java
@Data  
@Component  
public class SpelProperties {  
  
    @Value("1 + 2")  
    private String common;  
  
    @Value("demo_${auth.jwt.token}")  
    private String prefixConf;  
  
    @Value("#{'abcd'}")  
    private String spelStr;  
  
    /**  
     * 基本计算  
     */  
    @Value("#{1 + 2}")  
    private String spelVal3;  
  
    /**  
     * 列表  
     */  
    @Value("#{{1, 2, 3}}")  
    private List<Integer> spelList;  
  
    /**  
     * map     
     */    
    @Value("#{{a: '123', b: 'cde'}}")  
    private Map spelMap;  
  
  
    /**  
     * 嵌套使用，从配置中获取值，然后执行SpEL语句  
     */  
    @Value("#{'${auth.jwt.token}'.substring(2)}")  
    private String spelLen;  
  
    /**  
     * 调用静态方法  
     */  
    @Value("#{T(com.git.hui.boot.properties.value.config.SpelProperties).uuid('${auth.jwt.token}_')}")  
    private String spelStaticMethod;  
  
    /**  
     * bean 方法访问 有无@ 
     */  
    @Value("#{@randomService.randUid()}")  
    private String spelBeanMethod1;  
      
    @Value("#{randomService.randUid()}")  
    private String spelBeanMethod2;  
  
    // @Value("${￥{auth.jwt.token}}")  
    //private String selfProperty;  
    public static String uuid(String prefix) {  
        return prefix + UUID.randomUUID().toString().replaceAll("_", ".");  
    }  
}
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402252147313.png)


## 2.4、小结

本篇博文主要介绍了`@Value`除了绑定配置文件中的配置之外，另外两种常见的case

- 字面量
- SpEL表达式：定义在`#{}`里面

借助SpEL的强大功能，完全可以发挥我们的脑洞，让`@Value`修饰的属性初始化不再局限于简单的配置文件，比如从db,redis,http获取完全是可行的嘛，无非就是一个表达式而已

当然这里还存在一个待解决的问题，就是值刷新的支持，已知`@Value`只在bean初始化时执行一次，后续即便配置变更了，亦不会重新更改这个值，这种设计有好有坏，好处很明显，配置的不变性可以省去很多问题；缺点就是不灵活

那么如何让`@Value`的配置可以动态刷新呢？

# 3、其他
## 3.1、源码
- 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: 
	- [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/002-properties-value](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/002-properties-value)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)


转载自：[SpringBoot @Value之字面量及SpEL知识点介绍篇](https://spring.hhui.top/spring-blog/2021/06/15/210615-SpringBoot%E5%9F%BA%E7%A1%80%E9%85%8D%E7%BD%AE%E7%AF%87-Value%E4%B9%8B%E5%AD%97%E9%9D%A2%E9%87%8F%E5%8F%8ASpEL%E7%9F%A5%E8%AF%86%E7%82%B9%E4%BB%8B%E7%BB%8D%E7%AF%87/)

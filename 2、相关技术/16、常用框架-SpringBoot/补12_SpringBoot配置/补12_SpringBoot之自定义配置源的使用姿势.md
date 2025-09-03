
前面一篇博文介绍了一个`@Value`的一些知识点，其中提了一个点，`@Value`对应的配置，除了是配置文件中之外，可以从其他的数据源中获取么，如从redis，db，http中获取配置？

了解过SpringCloud Config的可以给出确切的答案，可以，而且用起来还老爽了，远程配置，支持配置动态刷新，接下来我们来看一下，在SpringBoot中，如何配置自定义的数据源

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

# 2、自定义配置源

`@Value`修饰的成员，绑定配置时，是从`Envrionment`中读取配置的，所以我们需要做的就是注册一个自定义的配置源，借助`MapPropertySource`可以来实现我们需求场景

## 2.1、自定义数据源

演示一个最简单自定义的配置数据源，重写`MapPropertySource`的`getProperties`方法

实现如下
```java
public class SimplePropertiesSource extends MapPropertySource {  
    public SimplePropertiesSource(String name, Map<String, Object> source) {  
        super(name, source);  
    }  
  
    public SimplePropertiesSource() {  
        this("filePropertiesSource", new HashMap<>());  
    }  
  
    /**  
     * 覆盖这个方法，适用于实时获取配置  
     *  
     * @param name  
     * @return  
     */  
    @Override  
    public Object getProperty(String name) {  
        // 注意，只针对自定义开头的配置才执行这个逻辑  
        if (name.startsWith("selfdefine.")) {  
            return name + "_" + UUID.randomUUID();  
        }  
        return super.getProperty(name);  
    }  
}
```

## 2.2、数据源注册

上面只是声明了配置源，接下来把它注册到Environment中，这样就可以供应用使用了
```java
@RestController  
@SpringBootApplication  
public class Application {  
    private Environment environment;  
  
    @Bean  
    public SimplePropertiesSource simplePropertiesSource(ConfigurableEnvironment environment) {  
        this.environment = environment;  
        SimplePropertiesSource ropertiesSource = new SimplePropertiesSource();  
        environment.getPropertySources().addLast(ropertiesSource);  
        return ropertiesSource;  
    }  
  
    // 获取配置  
    @GetMapping(path = "get")  
    public String getProperty(String key) {  
        return environment.getProperty(key);  
    }  
  
    public static void main(String[] args) {  
        SpringApplication.run(Application.class);  
    }  
}
```

![1.gif](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402222028596.gif)

从上面的输出可以看出，自定义配置开头的会获取到随机的配置值；非`selfdefine`开头的，没有相应的配置，返回空
## 2.3、基于文件的自定义配置源

上面这个可能有点过于儿戏了，接下来我们将配置源放在自定义的文件中，并支持文件配置修改
```java
public class FilePropertiesSource extends MapPropertySource {  
    public FilePropertiesSource(String name, Map<String, Object> source) {  
        super(name, source);  
    }  
  
    public FilePropertiesSource() {  
        this("filePropertiesSource", new HashMap<>());  
    }  
  
    // 这种方式，适用于一次捞取所有的配置，然后从内存中查询对应的配置，提高服务性能  
    // 10s 更新一次  
    @PostConstruct  
    @Scheduled(fixedRate = 10_000)  
    public void refreshSource() throws IOException {  
        String ans =  
                FileCopyUtils.copyToString(new InputStreamReader(FilePropertiesSource.class.getClassLoader().getResourceAsStream("kv.properties")));  
        Map<String, Object> map = new HashMap<>();  
        for (String sub : ans.split("\n")) {  
            if (sub.isEmpty()) {  
                continue;  
            }  
            String[] kv = StringUtils.split(sub, "=");  
            if (kv.length != 2) {  
                continue;  
            }  
  
            map.put(kv[0].trim(), kv[1].trim());  
        }  
  
        source.clear();  
        source.putAll(map);  
    }  
}
```

上面写了一个定时器，每10s刷新一下内存中的配置信息，当然这里也是可以配置一个文件变动监听器，相关有兴趣的话，可以看下[Java实现文件变动的监听可以怎么玩](http://mp.weixin.qq.com/s?__biz=MzU3MTAzNTMzMQ==&mid=2247483855&idx=1&sn=918528761a188b664823dbf442ab681b&chksm=fce71a63cb909375a46cd1ec966881ce075f2b98ac0a84aaf2eaa33f65063c6be11378676039&token=73054292&lang=zh_CN#rd)

对应的配置文件
```yml
user=xhh  
name=一灰灰  
age=18
```

注册的姿势与上面一致，就不单独说明了，接下来演示一下使用
![1.gif](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402222027409.gif)

从上可以看到文件中的配置修改之后，过一段时间会刷新

## 2.4、@Value绑定自定义配置

接下来我们看一下，将`@Value`绑定自定义的配置，是否可以成功

调整一下上面的Application, 添加一个成员属性
```java
@Value("${name}")  
private String name;  
  
@GetMapping(path = "get")  
public String getProperty(String key) {  
    return name + "|" + environment.getProperty(key);  
}
```

再次测试发现抛异常了，说是这个配置不存在！！！
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402212315035.png)

（这就过分了啊，看了半天，结果告诉我不行，这还不得赶紧搞个差评么😡😡😡）

已经写到这里了，当然我也得继续尝试挽救一下，为啥前面直接通过`Environment`可以拿到配置，但是`@Value`注解绑定就不行呢？

”罪魁祸首“就在于初始化顺序，我自定义的配置源，还没有塞到`Envrionment`，你就开会着手绑定了，就像准备给”一灰灰blog“一个差评，结果发现还没关注…（好吧，我承认没关注也可以评论😭）

根据既往的知识点（至于是哪些知识点，那就长话短说不了了，看下面几篇精选的博文吧）
- [15、如何指定bean最先加载(应用篇)](2、相关技术/16、常用框架-SpringBoot/补13_SpringBoot%20Bean/15、如何指定bean最先加载(应用篇).md)
- [12、指定Bean初始化顺序的若干姿势](2、相关技术/16、常用框架-SpringBoot/补13_SpringBoot%20Bean/12、指定Bean初始化顺序的若干姿势.md)
- [11、Bean加载顺序之错误使用姿势辟谣](2、相关技术/16、常用框架-SpringBoot/补13_SpringBoot%20Bean/11、Bean加载顺序之错误使用姿势辟谣.md)

这个问题主要是配置文件名称不是SpringBoot默认指定的，找不到kv.properties文件，所以获取不到name属性（我自己猜的，把kv改成application后可以解决）；
要解决这个问题，一个最简单的方式如下

创建一个独立的配置类，实现自定义数据源的注册
```java
@Configuration  
public class AutoConfig {  
    @Bean  
    public FilePropertiesSource filePropertiesSource(ConfigurableEnvironment environment) {  
        FilePropertiesSource filePropertiesSource = new FilePropertiesSource();  
        environment.getPropertySources().addLast(filePropertiesSource);  
        return filePropertiesSource;  
    }  
}
```

测试类上指定bean依赖
```java
@DependsOn("filePropertiesSource")  
@EnableScheduling  
@RestController  
@SpringBootApplication  
public class Application {  
    @Autowired  
    private Environment environment;  
  
    @Value("${name}")  
    private String name;  
  
    @GetMapping(path = "get")  
    public String getProperty(String key) {  
        return name + "|" + environment.getProperty(key);  
    }  
  
    public static void main(String[] args) {  
        SpringApplication.run(Application.class);  
    }  
}
```

再次测试，结果如下
![1.gif](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402222029844.gif)

从上面的演示动图可以看到，绑定自定义的数据源配置，没有问题，但是，当配置变更时，绑定的name字段，没有随之更新

简单来讲就是不支持动态刷新，这就难受了啊，我就想要动态刷新，那该怎么搞？

- 不要急，新的博文已经安排上了，下篇奉上（怕迷路的小伙伴，不妨关注一下”一灰灰blog“🐺）

## 2.5、小结

最后按照惯例小结一下，本文篇幅虽长，但知识点比较集中，总结下来，两句话搞定

- 通过继承`MapPropertySource`来实现自定义配置源，注册到`Envrionment`可供`@Value`使用
- 使用`@Value`绑定自定义配置源时，注意注册的顺序要早于bean的初始化

好的，到这里正文结束， 我是一灰灰，欢迎各位大佬来踩一踩长草的公众号”一灰灰blog”

# 3、其他
## 3.1、源码
- 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: 
	- [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/002-dynamic-envronment](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/002-dynamic-envronment)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)


实际开发过程中，配置的多环境区分属于标配了，当我们不考虑配置中心时，将多环境的配置就放在项目的resource目录下，那么可以怎样做多环境的配置管理呢?

之前介绍过一篇基于 `spring.profiles.active` 配置来选择对应的配置文件的方式，有了解这个配置的小伙伴可以很快找到这种方式的特点

如配置值为dev，则加载 `application-dev.yml` 配置文件，如果为prod，则加载`application-prod.yml`

那么缺点就很明显了，当我每个环境的配置很多时，上面这种方式真的好用么？

接下来本文介绍另外一种常见的基于maven的多环境配置方式

# 1、项目搭建
## 1.1、项目依赖

本项目借助`SpringBoot 2.2.1.RELEASE` + `maven 3.5.3` + `IDEA`进行开发

开一个web服务用于测试
```xml
<dependencies>  
    <dependency>  
        <groupId>org.elasticsearch.client</groupId>  
        <artifactId>elasticsearch-rest-high-level-client</artifactId>  
    </dependency>  
    <dependency>  
        <groupId>org.springframework.boot</groupId>  
        <artifactId>spring-boot-starter-thymeleaf</artifactId>  
    </dependency>  
</dependencies>
```

一个简单的页面模板 `resources/templates/index.html`
```html
<!DOCTYPE html>  
<html xmlns:th="http://www.thymeleaf.org">  
<head>  
    <meta charset="UTF-8">  
    <meta name="viewport" content="width=device-width, initial-scale=1">  
    <meta name="description" content="SpringBoot thymeleaf"/>  
    <meta name="author" content="YiHui"/>  
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>  
    <title>YiHui's SpringBoot Demo</title>  
</head>  
<body>  
  <div>  
    <div class="title">hello world!</div>  
    <br/>  
    <div class="content" th:text="'配置信息:' + ${info}">默认的内容</div>  
    <br/>  
    <div class="sign" th:text="'当前时间' + ${now}">默认的签名</div>  
    <br/>  
  </div>  
</body>  
</html>
```


## 1.2、多环境配置

```xml
<profiles>  
    <!-- 开发 -->  
    <profile>  
        <id>dev</id>  
        <properties>  
            <env>dev</env>  
        </properties>  
        <activation>  
            <activeByDefault>true</activeByDefault>  
        </activation>  
    </profile>  
    <!-- 测试 -->  
    <profile>  
        <id>test</id>  
        <properties>  
            <env>test</env>  
        </properties>  
    </profile>  
    <!-- 预发 -->  
    <profile>  
        <id>pre</id>  
        <properties>  
            <env>pre</env>  
        </properties>  
    </profile>  
    <!-- 生产 -->  
    <profile>  
        <id>prod</id>  
        <properties>  
            <env>prod</env>  
        </properties>  
    </profile>  
</profiles>  
  
<build>  
    <resources>  
        <resource>  
            <directory>src/main/resources</directory>  
        </resource>  
        <resource>  
            <directory>src/main/resources-env/${env}</directory>  
            <filtering>true</filtering>  
        </resource>  
    </resources>  
</build>
```

> ChatGPT对于上面pom.xml部分标签的解释：
> 
> 在这段`pom.xml`文件中，主要涉及到两个部分：`profiles`和`build`。
> 
> **`profiles`标签**
> 
> - `<profiles>`标签用于定义多个构建配置文件，每个配置文件包含一组构建配置。
> - `<profile>`标签定义一个具体的构建配置文件，其中包含了`id`、`properties`和`activation`等子标签。
>   - `<id>`指定配置文件的唯一标识符。
>   - `<properties>`标签包含了一组属性，可以在构建过程中引用。
>   - `<activation>`标签用于指定激活配置文件的条件。在这里，`activeByDefault`设置为`true`表示默认激活该配置文件。
> 
> 在上述`profiles`节中，定义了四个配置文件：`dev`、`test`、`pre`和`prod`，分别表示开发、测试、预发和生产环境。每个配置文件都定义了一个名为`env`的属性，用于指定当前环境。
> 
> **`build`标签**
> 
> - `<build>`标签包含了项目的构建配置信息。
> - `<resources>`标签定义了项目的资源文件路径。
>   - `<resource>`标签用于指定资源文件的目录。
>     - 第一个`<resource>`标签指定了主要资源文件的目录为`src/main/resources`。
>     - 第二个`<resource>`标签指定了根据环境变量`env`指定的资源文件目录为`src/main/resources-env/${env}`，并且启用了过滤器`filtering`，表示会对该目录下的资源文件进行属性替换。
> 
> 通过定义不同的`profiles`和资源文件路径，可以实现根据不同环境变量加载不同的资源文件，从而实现在不同环境下灵活配置项目的功能。

上面定义了四个环境，默认处于dev开发环境

其次就是build标签中的`resource`，用于指定不同环境下的资源存放位置；在resources目录下的配置文件如下
```yml
spring:  
  profiles:  
    active: dal
```

上面这个表示会加载`application-dal.yml`配置文件；接下来看下不同环境中这个配置文件的具体存放位置如下
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402282237045.png)

**dev环境配置:**
```yml
spring:  
  datasource:  
    url: jdbc:mysql://127.0.0.1:3306/story?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai  
    username: root  
    password:
```

**pre环境配置**
```yml
spring:  
  datasource:  
    url: jdbc:mysql://pre.hhui.top/story?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai  
    username: pre_root  
    password:
```

**prod环境配置**
```yml
spring:  
  datasource:  
    url: jdbc:mysql://prod.hhui.top/story?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai  
    username: prod_root  
    password:
```

**test环境配置**
```yml
spring:  
  datasource:  
    url: jdbc:mysql://test.hhui.top/story?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai  
    username: test_root  
    password:
```

上面四个配置文件的主要区别在于`username`

# 2、环境选择验证
## 2.1、配置类

首先基于Spring AutoConfig定义一个配置属性类，用于映射`application-dal.yml`对应的配置
```java
@Data  
@ConfigurationProperties(prefix = "spring.datasource")  
public class DalConfig {  
    private String url;  
  
    private String username;  
  
    private String password;  
}
```

## 2.2、测试端点

写一个简单的测试端点，输出配置值
```java
@Controller  
@EnableConfigurationProperties({DalConfig.class})  
@SpringBootApplication  
public class Application {  
    private DalConfig dalConfig;  
  
    public Application(DalConfig dalConfig, Environment environment) {  
        this.dalConfig = dalConfig;  
        System.out.println(dalConfig);  
    }  
  
    public static void main(String[] args) {  
        SpringApplication application = new SpringApplication(Application.class);  
        application.run(args);  
    }  
  
  
    @GetMapping(path = {"", "/", "/index"})  
    public ModelAndView index() {  
        Map<String, Object> data = new HashMap<>(2);  
        data.put("info", dalConfig);  
        data.put("now", LocalDateTime.now().toString());  
        return new ModelAndView("index", data);  
    }  
}
```

## 2.3、启动测试

项目启动之后，默认的是dev环境，此时访问之后结果如下
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402282250588.png)

接下来如果我想启动test环境，可以如下操作

- idea右边maven，选中对应的环境
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402282252583.png)

再次启动测试一下
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402282252436.png)

上面说的是idea启动测试，那么实际打包的时候怎么整呢？
```java
mvn clean package -DskipTests=true -P dev
```

关键就是上面的 `-P` 来指定具体的环境

## 2.4、小结

最后小结一下本文介绍到基于mvn的环境配置策略，这里主要的知识点都在`pom.xml`中，指定`profiles`，然后在打包的时候通过`-P`确定具体的环境

在最终打包时，只会将对应环境的配置文件打到jar包中

# 3、其他
## 3.1、源码
- 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: 
	- [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/001-properties-env-mvn](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/001-properties-env-mvn)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)


转载自：[基于maven多环境配置](https://spring.hhui.top/spring-blog/2022/04/25/220425-SpringBoot%E7%B3%BB%E5%88%97%E4%B9%8B%E5%9F%BA%E4%BA%8Emaven%E5%A4%9A%E7%8E%AF%E5%A2%83%E9%85%8D%E7%BD%AE/)
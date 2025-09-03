
前面介绍了 [spring](https://spring.hhui.top/spring-blog/2019/09/29/190929-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87%E4%B9%8B%E9%87%8D%E5%AE%9A%E5%90%91/#) web篇数据返回的几种常用姿势，当我们在相应一个http请求时，除了直接返回数据之外，还有另一种常见的case -> 重定向；

比如我们在逛淘宝，没有登录就点击购买时，会跳转到登录界面，这其实就是一个重定向。本文主要介绍对于后端而言，可以怎样支持302重定向

## 1、环境搭建

首先得搭建一个 [web应用](https://spring.hhui.top/spring-blog/2019/09/29/190929-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87%E4%B9%8B%E9%87%8D%E5%AE%9A%E5%90%91/#)才有可能继续后续的测试，借助SpringBoot搭建一个web应用属于比较简单的活;

创建一个maven项目，pom文件如下
```xml
<parent>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-parent</artifactId>  
    <version>2.1.7</version>  
    <relativePath/> <!-- lookup parent from update -->  
</parent>  
  
<properties>  
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>  
    <spring-cloud.version>Finchley.RELEASE</spring-cloud.version>  
    <java.version>1.8</java.version>  
</properties>  
  
<dependencies>  
    <dependency>  
        <groupId>org.springframework.boot</groupId>  
        <artifactId>spring-boot-starter-web</artifactId>  
    </dependency>  
    <dependency>  
        <groupId>com.alibaba</groupId>  
        <artifactId>fastjson</artifactId>  
        <version>1.2.45</version>  
    </dependency>  
</dependencies>  
  
<build>  
    <pluginManagement>  
        <plugins>  
            <plugin>  
                <groupId>org.springframework.boot</groupId>  
                <artifactId>spring-boot-maven-plugin</artifactId>  
            </plugin>  
        </plugins>  
    </pluginManagement>  
</build>  
<repositories>  
    <repository>  
        <id>spring-milestones</id>  
        <name>Spring Milestones</name>  
        <url>https://repo.spring.io/milestone</url>  
        <snapshots>  
            <enabled>false</enabled>  
        </snapshots>  
    </repository>  
</repositories>
```

依然是一般的流程，pom依赖搞定之后，写一个程序入口
```java
/**  
 * Created by @author yihui in 15:26 19/9/13.  
 */  
@SpringBootApplication  
public class Application {  
    public static void main(String[] args) {  
        SpringApplication.run(Application.class);  
    }  
}
```

## 2、302重定向

### 1. 返回redirect

这种case通常适用于返回视图的接口，在返回的字符串前面添加`redirect:`方式来告诉 [Spring框架](https://spring.hhui.top/spring-blog/2019/09/29/190929-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87%E4%B9%8B%E9%87%8D%E5%AE%9A%E5%90%91/#)，需要做302重定向处理
```java
@Controller  
@RequestMapping(path = "redirect")  
public class RedirectRest {  
  
    @ResponseBody  
    @GetMapping(path = "index")  
    public String index(HttpServletRequest request) {  
        return "重定向访问! " + JSON.toJSONString(request.getParameterMap());  
    }  
  
    @GetMapping(path = "r1")  
    public String r1() {  
        return "redirect:/redirect/index?base=r1";  
    }  
}
```

上面给出了一个简单的demo，当我们访问`/redirect/r1`时，会重定向到请求`/redirect/index?base=r1`，实际测试结果如下
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409172257087.png)

注意上面的截图，我们实际访问的连接是 `http://127.0.0.1:8080/redirect/index?base=r1`，在浏览器中的表现则是请求url变成了`http://127.0.0.1:8080/redirect/index?base=r1`；通过控制台查看到的返回头状态码是302

**说明**

- 使用这种方式的前提是不能在接口上添加`@ResponseBody`注解，否则返回的字符串被当成普通字符串处理直接返回，并不会实现重定向

### 2、HttpServletResponse重定向

前面一篇说到 [Spring](https://spring.hhui.top/spring-blog/2019/09/29/190929-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87%E4%B9%8B%E9%87%8D%E5%AE%9A%E5%90%91/#)MVC返回数据的时候，介绍到可以直接通过`HttpServletResponse`往输出流中写数据的方式，来返回结果；我们这里也是利用它，来实现重定向
```java
@ResponseBody  
@GetMapping(path = "r2")  
public void r2(HttpServletResponse response) throws IOException {  
    response.sendRedirect("/redirect/index?base=r2");  
}
```

从上面的demo中，也可以看出这个的使用方式很简单了，直接调用`javax.servlet.http.HttpServletResponse#sendRedirect`，并传入需要重定向的url即可
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409172258702.png)

### 3. 小结

这里主要介绍了两种常见的后端重定向方式，都比较简单，这两种方式也有自己的适用场景（当然并不绝对）

- 在返回视图的前面加上`redirect`的方式，更加适用于视图的跳转，从一个网页跳转到另一个网页
- `HttpServletResponse#sendRedirec`的方式更加灵活，可以在后端接收一次http请求生命周期中的任何一个阶段来使用，比如有以下几种常见的场景
    - 某个接口要求登录时，在拦截器层针对所有未登录的请求，重定向到登录页面
    - 全局异常处理中，如果出现服务器异常，重定向到定制的500页面
    - 不支持的请求，重定向到404页面

# 3、其他
## 3.1、项目
工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/207-web-response](https://github.com/liuyueyi/spring-boot-demo/blob/master/spring-boot/207-web-response)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)



转载自：[【WEB系列】请求重定向](https://spring.hhui.top/spring-blog/2019/09/29/190929-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87%E4%B9%8B%E9%87%8D%E5%AE%9A%E5%90%91/)


# 1、多环境配置

前面一篇主要介绍的是如何获取配置信息，接下来则是另外一个非常非常基础和必要的知识点了，应用如何根据不同的环境来选择对应的配置，即配置的多环境选择问题

配置区分环境，最直观的如测试环境和生产环境的DB不同，测试环境的应用要求连接测试DB；生成环境的应用要求连生成DB；对于应用本身来说，业务代码啥的都是一样，无非就是DB的配置不同，如果在代码中写死环境判断，然后进行选择配置话，就不太优雅了；

SpringBoot本身就支持多环境配置文件，应用的配置，除了 `application.yml` 文件之外，还会有环境相关的配置，如下一个实例
```yml
application.yml  
application-dev.yml  
application-pro.yml
```

## 1.1、多环境选择

### 1.1.1、命令规则

配置文件，一般要求是以 `application` 开头，可以是yml文件也可以是properties文件

### 1.1.2、配置选择

如何确定哪个配置配置文件（application-dev.yml 与 application-pro.yml）生效呢？

- 通过配置信息 `spring.profile.active` 来指定需要加载的配置文件

通常这个配置信息会放在 `applicatin.yml` 文件中，如下
```yml
spring:  
  profiles:  
    active: dev
```

上面这个表示，当前的配置信息，会从 `application.yml` 和 `application-dev.yml` 文件中获取；且`-dev`文件中定义的配置信息，会覆盖前面的配置信息

**注意**

- 上面这个配置的value，可以指定多个配置文件，用英文逗号分隔
- 其中最右边的优先级最高，覆盖左边配置文件中重名的配置信息

### 1.1.3、实例演示

配置文件内容如下

application.yml
```yml
# 端口号  
server:  
	port: 8081  
  
spring:  
	profiles:  
		active: dev,biz  
  
  
biz:  
	total: application
```

application-dev.yml
```yml
biz:  
  env: dev-environment  
  profile: dev-profile
```

application-pro.yml
```yml
biz:  
	env: pro-environment  
	profile: pro-profile
```

application-biz.yml
```yml
biz:  
  whitelist: a,b,c,d,e,f,g  
  ratelimit: 1,2,3  
  total: application-biz  
  profile: biz-profile
```

通过前面的规则进行分析，当前选中生效的配置文件为

- application.yml, application-dev.yml, application-biz.yml
- 优先级为：biz文件的配置覆盖dev文件，dev文件的覆盖`application`的配置

代码验证如下
```java
package com.git.hui.boot.properties;  
  
import org.springframework.boot.SpringApplication;  
import org.springframework.boot.autoconfigure.SpringBootApplication;  
import org.springframework.core.env.Environment;  
  
/**  
 * Created by @author yihui in 09:17 18/9/20.  
 */  
@SpringBootApplication  
public class Application {  
  
    public Application(Environment environment) {  
        String env = environment.getProperty("biz.env");  
  
        String whitelist = environment.getProperty("biz.whitelist");  
        String ratelimit = environment.getProperty("biz.ratelimit");  
  
        String total = environment.getProperty("biz.total");  
        String profile = environment.getProperty("biz.profile");  
  
        // application.yml文件中的配置 spring.profile.active指定具体选中的配置文件，为 application-dev 和 application-biz  
        // read from application-dev.yml  
        System.out.println("env: " + env);  
  
        // read from application-biz.yml  
        System.out.println("whitelist: " + whitelist);  
        System.out.println("ratelimit: " + ratelimit);  
  
  
        // 当配置文件 application.yml, application-dev.yml, application-biz.yml 三个文件都存在时，覆盖规则为  
        // biz > dev > application.yml  （其中 biz>dev的原则是根据 spring.profile.active 中定义的顺序来的，最右边的优先级最高）  
        // read from application-biz.yml  
        System.out.println("total: " + total);  
  
        // read from application-biz.yml  
        System.out.println("profile: " + profile);  
    }  
  
    public static void main(String[] args) {  
        SpringApplication.run(Application.class);  
    }  
  
}
```

输出结果为
```java
env: dev-environment  
whitelist: a,b,c,d,e,f,g  
ratelimit: 1,2,3  
total: application-biz  
profile: biz-profile
```

## 1.2、优先级问题

上面虽然看是实现了多环境的配置问题，但看完之后有一个明显的疑问，选择环境的配置信息写死在`application.yml`文件中，难道说部署到测试和生产环境时，还得记得手动改这个配置的值么？

如果是这样的话，也太容易出问题了吧。。。

那么如何解决这个问题呢，常见的一种方式是通过启动脚本，传入当前环境的参数，来覆盖选中的环境

### 1.2.1、配置文件优先级

默认的配置文件是放在 `src/main/resources` 目录下，当然也是可以放其他位置的

- 外置，在相对于应用程序运行目录的 `/config` 子目录中（执行 `java -jar ` 命令位置下的 `/config` 文件夹下）
- 外置，在应用程序运行的目录中（执行 `java -jar ` 命令所在文件夹中）
- 内置，放在config包下(即 src/main/resources/config)目录下
- 内置，放在classpath根目录下（即默认的 src/main/resources/目录下)

上面的优先级是从高到低来的，即外置的改与内置的；config下面的高于根目录下的

以内置的两个进行对比，实测结果如下
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401271704149.png)

### 1.2.2、配置信息来源

前面一篇中，遗留了一个问题，就是在配置文件中配置了属性 `user.name = 一灰灰blog`, 但是实际取出的却是 `user` (我个人的电脑用户名)，也就是说，Environment中读取的配置信息，不仅仅是从配置文件中获取，还要其他的一些配置信息来源，对照：[外部化的配置](https://springdoc.cn/spring-boot/features.html#features.external-config)

1. 根目录下的开发工具全局设置属性(当开发工具激活时为~/.spring-boot-devtools.properties)。
2. 测试中的@TestPropertySource注解。
3. 测试中的@SpringBootTest#properties注解特性。
4. 命令行参数
5. SPRING_APPLICATION_JSON中的属性(环境变量或系统属性中的内联JSON嵌入)。
6. ServletConfig初始化参数。
7. ServletContext初始化参数。
8. java:comp/env里的JNDI属性
9. JVM系统属性
10. 操作系统环境变量
11. 随机生成的带random.* 前缀的属性（在设置其他属性时，可以应用他们，比如${random.long}）
12. 应用程序以外的application.properties或者appliaction.yml文件
13. 打包在应用程序内的application.properties或者appliaction.yml文件
14. 通过@PropertySource标注的属性源
15. 默认属性(通过SpringApplication.setDefaultProperties指定).

## 1.3、环境选择的几种方式

看了上面的配置信息来源，我们可以如何优雅的实现不同环境选择不同的配置文件呢？有下面两个容易想到和实现的方式了

- 命令行参数
- 应用程序外的配置文件

### 1.3.1、命令行参数方式

这种实现思路就是在启动脚本中，传入当前环境，然后覆盖掉属性 `--spring.profiles.active`，对业务来说，就不需要做任何的改动了，只要启动脚本本身区分环境即可，唯一的要求就是遵循统一的规范，一个简单的实现如下

假定命令行的第一个参数就是环境，取出这个参数，传入即可
```java
public static void main(String[] args) {  
      if (args.length > 0) {  
          SpringApplication.run(Application.class, "--spring.profiles.active=" + args[0] + ",biz");  
      } else {  
          SpringApplication.run(Application.class);  
      }  
  }
```

实测结果，注意下面红框内的pro，覆盖了配置文件中的dev
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202401271705636.png)


**说明**

当然可以直接传入完整的命令行参数`--spring.profiles.active=pro,biz`，这样代码内部就不需要进行特殊处理


### 1.3.2、外配置文件方式

当程序以独立的jar运行时，我个人的感觉是外置的配置文件是优于内置的配置文件的；因为修改配置的话，不需要重新打包部署，直接改即可

这种实现方式也没啥好多说的，相当于把配置文件拉出来放在外面而已，再根据环境写具体的`spring.profiles.active`的值

# 2、小结

1. SpringBoot是支持多环境的配置，通过配置属性 `spring.profiles.active` 来指定
2. `spring.profiles.active`参数指定多个配置文件时，右边的优于左边的
3. 应用外的配置文件优先于应用内，config目录下的优先于根目录下的
4. 配置参数来源及优先级可以参看前文: [配置信息来源](https://spring.hhui.top/spring-blog/2018/09/20/180920-SpringBoot%E5%9F%BA%E7%A1%80%E7%AF%87%E9%85%8D%E7%BD%AE%E4%BF%A1%E6%81%AF%E4%B9%8B%E5%A4%9A%E7%8E%AF%E5%A2%83%E9%85%8D%E7%BD%AE%E4%BF%A1%E6%81%AF/#b.-%E9%85%8D%E7%BD%AE%E4%BF%A1%E6%81%AF%E6%9D%A5%E6%BA%90)
5. 命令行参数传入时，请注意写法形同 `--key=value`

# 3、其他
## 3.1、源码
- 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/000-properties](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/000-properties)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)

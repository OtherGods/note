#### 1.微服务场景模拟
首先，我们需要模拟一个服务调用的场景。方便后面学习微服务架构

##### 1.1.服务提供者
我们新建一个项目，对外提供查询用户的服务。
###### 1.1.1.Spring脚手架创建工程
借助于Spring提供的快速搭建工具：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190501233125305.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)填写项目信息：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190501233233188.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
添加web依赖：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190501233313603.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
添加mybatis依赖：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190501233429721.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
填写项目位置：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190501233458646.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
生成的项目结构：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190501235519922.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
完整的Pom文件：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>2.1.4.RELEASE</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.bruceliu.springcloud</groupId>
	<artifactId>springcloud-demo</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>springcloud-demo</name>
	<description>Demo project for Spring Boot</description>

	<properties>
		<java.version>1.8</java.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-jdbc</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.mybatis.spring.boot</groupId>
			<artifactId>mybatis-spring-boot-starter</artifactId>
			<version>2.0.1</version>
		</dependency>

		<dependency>
			<groupId>mysql</groupId>
			<artifactId>mysql-connector-java</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

</project>

```
###### 1.1.2.编写代码
添加一个对外查询的接口：
```java
/**
 * @author bruceliu
 * @create 2019-05-01 16:04
 * @description
 */
@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @ResponseBody
    @GetMapping("/test")
    public User hello() {
        User user = this.userService.queryById(2);
        return user;
    }

    @GetMapping("/all")
    @ResponseBody
    public List<User> all(ModelMap model) {
        // 查询用户
        List<User> users = this.userService.queryUsers();
        // 放入模型
        model.addAttribute("users", users);
        // 返回模板名称（就是classpath:/templates/目录下的html文件名）
        return users;
    }
```
service:
```java
/**
 * @author bruceliu
 * @create 2019-05-01 14:52
 * @description
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Override
    public User queryById(Integer id) {
        return userMapper.queryById(id);
    }

    @Override
    public void deleteById(Integer id) {
        userMapper.deleteById(id);
    }

    @Override
    public List<User> queryUsers() {
        return userMapper.queryUsers();
    }
}

```
mapper:
```java
/**
 * @author bruceliu
 * @create 2019-05-01 14:50
 * @description
 */
@Mapper
public interface UserMapper {

    public User queryById(Integer id);

    public void deleteById(Integer id);

    public List<User> queryUsers();
}

```
实体类
```java
package com.bruceliu.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * @author bruceliu
 * @create 2019-05-01 11:16
 * @description
 */
@Data
public class User implements Serializable{

    private int id;
    private String username;
    private String password;
    private int age;
    private int sex;
    private String birthday;
    private String created;
    private String updated;
    private String note;

}

```
属性文件,properties：
```properties
server.port=80

# 设置com.leyou包的日志级别为debug
logging.level.com.bruceliu=debug

# mybatis 别名扫描
mybatis.type-aliases-package=com.bruceliu.bean
# mapper.xml文件位置,如果没有映射文件，请注释掉
mybatis.mapper-locations=classpath:mappers/*.xml

# 连接四大参数
spring.datasource.url=jdbc:mysql://localhost:3306/ssmdb
spring.datasource.username=root
spring.datasource.password=123
# 可省略，SpringBoot自动推断
spring.datasource.driverClassName=com.mysql.jdbc.Driver

#初始化连接数
spring.datasource.druid.initial-size=1
#最小空闲连接
spring.datasource.druid.min-idle=1
#最大活动连接
spring.datasource.druid.max-active=20
#获取连接时测试是否可用
spring.datasource.druid.test-on-borrow=true
#监控页面启动
spring.datasource.druid.stat-view-servlet.allow=true

#开发阶段关闭thymeleaf的模板缓存
spring.thymeleaf.cache=false

```
项目结构
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502131629848.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
###### 1.1.3.启动并测试：
启动项目，访问接口：http://127.0.0.1/all
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502131744255.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
##### 2.2.服务调用者
###### 2.2.1.创建工程
与上面类似，这里不再赘述，需要注意的是，我们是调用服务，因此不需要mybatis相关依赖了。
pom：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>2.1.4.RELEASE</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.bruceliu.consumer</groupId>
	<artifactId>springcloud-demo-consumer</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>springcloud-demo-consumer</name>
	<description>Demo project for Spring Boot</description>

	<properties>
		<java.version>1.8</java.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<!-- 添加OkHttp支持 -->
		<dependency>
			<groupId>com.squareup.okhttp3</groupId>
			<artifactId>okhttp</artifactId>
			<version>3.9.0</version>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

</project>

```
###### 2.2.2.编写代码
首先在启动类中注册`RestTemplate`：
```java
/**
 * @author bruceliu
 * @create 2019-05-02 15:50
 * @description
 */
@Configuration
public class RestTemplateConfig {

    // 这次我们使用了OkHttp客户端,只需要注入工厂即可
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate(new OkHttp3ClientHttpRequestFactory());
    }
}

```
编写controller：
```java
/**
 * @author bruceliu
 * @create 2019-05-02 15:52
 * @description
 */
@RestController
@RequestMapping("consumer")
public class ConsumerController {

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/test")
    public List<User> consumerTest(){
        String url="http://127.0.0.1/all";
        return this.restTemplate.getForObject(url,List.class);
    }
}

```
###### 2.2.3.启动测试：
因为我们没有配置端口，那么默认就是8080，我们访问：http://127.0.0.1:81/consumer/test
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190502181639482.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
一个简单的远程服务调用案例就实现了。

##### 2.3.有没有问题？

简单回顾一下，刚才我们写了什么：

- springcloud-demo：一个提供根据id查询用户的微服务
- springcloud-demo-consumer：一个服务调用者，通过RestTemplate远程调用springcloud-demo

流程如下：
![d](https://img-blog.csdnimg.cn/20190502182339498.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0JydWNlTGl1X2NvZGU=,size_16,color_FFFFFF,t_70)
存在什么问题？
- 在consumer中，我们把url地址硬编码到了代码中，不方便后期维护
- consumer需要记忆user-service的地址，如果出现变更，可能得不到通知，地址将失效
- consumer不清楚user-service的状态，服务宕机也不知道
- user-service只有1台服务，不具备高可用性
- 即便user-service形成集群，consumer还需自己实现负载均衡

其实上面说的问题，概括一下就是分布式服务必然要面临的问题：

- 服务管理
  - 如何自动注册和发现
  - 如何实现状态监管
  - 如何实现动态路由
- 服务如何实现负载均衡
- 服务如何解决容灾问题
- 服务如何实现统一配置
以上的问题，我们都将在SpringCloud中得到答案。

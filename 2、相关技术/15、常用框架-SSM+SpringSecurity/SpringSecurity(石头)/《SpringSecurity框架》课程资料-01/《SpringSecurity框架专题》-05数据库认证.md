

[TOC]

上篇文章我们介绍了`SpringSecurity`系统认证的流程，我们发现系统认证其实是通过一个`UserDetailService`的实现类来实现的，所以我们就可以使用相同的方式将认证的业务改成和数据库的对比。此案例持久层我们通过`Mybatis`来实现！
## 1.mybatis准备
sql语句

```sql
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `password` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `slat` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `nickname` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin;

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES ('1', 'admin', '12345', 'bruce', '小刘');
```

### 1.1.导入相关依赖

```xml
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.3</version>
</dependency>

<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>2.0.3</version>
</dependency>

<!-- mysql驱动包 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.15</version>
</dependency>

<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.1.10</version>
</dependency>
```
### 1.2.配置文件
db.properties

```properties
jdbc.url=jdbc:mysql://127.0.0.1:3306/srm?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
jdbc.driverClassName=com.mysql.cj.jdbc.Driver
jdbc.username=root
jdbc.password=123456
```
### 1.3.Mybatis的配置文件

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>

    <settings>
        <!-- 打印sql日志  MyBatis日志框架使用LOG4J -->
       <setting name="logImpl" value="LOG4J"/>
    </settings>

</configuration>
```
### 1.4.和spring的整合文件

```xml
<!-- 加载配置文件 -->
<context:property-placeholder location="classpath:db.properties"/>

<!-- 数据库连接池 -->
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource"
      destroy-method="close">
    <property name="url" value="${jdbc.url}"/>
    <property name="username" value="${jdbc.username}"/>
    <property name="password" value="${jdbc.password}"/>
    <property name="driverClassName" value="${jdbc.driverClassName}"/>
    <property name="maxActive" value="10"/>
    <property name="minIdle" value="5"/>
</bean>
<!-- SqlSessionFactory -->
<!-- 让spring管理sqlsessionfactory 使用mybatis和spring整合包中的 -->
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
    <!-- 数据库连接池 -->
    <property name="dataSource" ref="dataSource"/>
    <!-- 加载mybatis的全局配置文件 -->
    <property name="configLocation" value="classpath:mybatis-config.xml"/>
    <!--
        映射文件和接口文件不在同一个目录下的时候
        它的spring是不会去扫描jar包中的相应目录的，只会去他当前项目下获取。其实要改变这种情况很简单，
        在classpath后面加一个*号，*号的作用是让spring的扫描涉及全个目录包括jar
    -->
    <property name="mapperLocations" value="classpath*:mapper/*.xml"/>
</bean>
<!-- Mapper映射文件的包扫描器 -->
<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
    <property name="basePackage" value="com.bruce.mapper"/>
</bean>
```
### 1.5.pojo文件

```java
package com.bruce.pojo;

public class UserPojo {
    private Integer id;

    private String username;

    private String password;

    private String salt;

    private String nickname;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

```
### 1.6.dao接口
定义一个根据账号查询的方法即可

```java
package com.bruce.mapper;

import com.bruce.pojo.UserPojo;
import org.apache.ibatis.annotations.Param;

public interface UserDao {

    UserPojo queryByUserName(@Param("userName") String userName);
}
```
### 1.7.映射文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.bruce.mapper.UserDao">
    <select id="queryByUserName"  resultType="com.bruce.pojo.UserPojo">
        select * from t_user where username = #{userName}
    </select>
</mapper>
```
### 1.8.service
接口定义

```java
package com.bruce.service;

import com.bruce.pojo.UserPojo;
import org.apache.ibatis.annotations.Param;

public interface UserService {

    UserPojo queryByUserName(String userName);
}
```
接口实现

```java
package com.bruce.service.impl;

import com.bruce.mapper.UserDao;
import com.bruce.pojo.UserPojo;
import com.bruce.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserDao userDao;

    @Override
    public UserPojo queryByUserName(String userName) {
        return userDao.queryByUserName(userName);
    }
}

```
## 2.service修改
接下来我们看看如何将SpringSecurity引入进来

### 2.1.UserService继承UserDetailService接口

```java
package com.bruce.service;

import com.bruce.pojo.UserPojo;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService{

    UserPojo queryByUserName(String userName);
}

```
### 2.2.重写loadUserByusername方法

```java
    /**
     * 根据账号查询
     *
     * @param s 登录表单输入的账号
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        // 根据账号去数据库中查询
        UserPojo userPojo = this.queryByUserName(s);
        if (userPojo != null) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            // 设置登录账号的角色
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            UserDetails user = new User(userPojo.getUsername(), "{noop}" + userPojo.getPassword(), authorities);
            return user;
        }
        // 返回null 默认表示账号不存在
        return null;
    }
```
## 3.配置文件修改
既然使用自定义的认证方法，那么原来设置的内存中的账号就不需要了

```xml
    <!--设置Spring Security认证用户信息的来源-->
    <security:authentication-manager>
        <security:authentication-provider user-service-ref="userServiceImpl">
<!--            <security:user-service>-->
<!--                <security:user name="user" password="{noop}user" authorities="ROLE_USER"/>-->
<!--                <security:user name="admin" password="{noop}admin" authorities="ROLE_ADMIN"/>-->
<!--            </security:user-service>-->
        </security:authentication-provider>
    </security:authentication-manager>
```

## 4.登录测试
启动系统，登录测试即可
![在这里插入图片描述](https://img-blog.csdnimg.cn/48f714da9de54275b23d19034f2c0b67.png)
## 5.加密处理
显然在实际项目中，对密码加密是必须的，所以我们就来看看`SpringSecurity`中是怎么做加密的。我们在此处通过`BCryptPasswordEncoder`来加密，动态加盐的方式
![在这里插入图片描述](https://img-blog.csdnimg.cn/6cd89cae9d8842a68ff8026ff5e08942.png)

```java
package com.bruce.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class EncodingTest {

    public static void main(String[] args) {

        BCryptPasswordEncoder passwordEncoder=new BCryptPasswordEncoder();
        System.out.println(passwordEncoder.encode("123"));
        System.out.println(passwordEncoder.encode("123"));
        System.out.println(passwordEncoder.encode("123"));

    }
}

```
配置文件中设置加密规则
![在这里插入图片描述](https://img-blog.csdnimg.cn/909db7f185c049149c3f7c6a1554c72a.png?x-oss-process=image/watermark,type_ZHJvaWRzYW5zZmFsbGJhY2s,shadow_50,text_Q1NETiBASVTogIHliJg=,size_20,color_FFFFFF,t_70,g_se,x_16)

去掉`{noop}`
![在这里插入图片描述](https://img-blog.csdnimg.cn/960f85349c144f2ca51711e01929d698.png)

修改数据库中对应的密码
![在这里插入图片描述](https://img-blog.csdnimg.cn/be128605d79e491d831f09ab9f1f01d3.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/f0900336eb3c45ecad2b72c879754c99.png)
## 6.认证状态判断
我们在实际项目中因为用户的不同操作，可能会给出不同的状态，比如正常，冻结等，`SpringSecurity`也支持，我们来看下，如何实现。
![在这里插入图片描述](https://img-blog.csdnimg.cn/63007a82509e4be3bd3777c0b68e8497.png)
> 1可用  0冻结

然后我们在认证的时候使用User对象的另一个构造器就可以了
![在这里插入图片描述](https://img-blog.csdnimg.cn/48044f1156b64c25b40fcdaf5387c1b3.png)
| 参数 | 说明 |
|--|--|
| boolean enabled | 是否可用 |
| boolean accountNonExpired | 账号是否失效 |
| boolean credentialsNonExpired | 秘钥是否失效 |
|boolean accountNonLocked | 账号是否锁定 |

如此设置即可
![在这里插入图片描述](https://img-blog.csdnimg.cn/b891dd6a26ad49baab0a5e3f65992017.png)
然后状态(status)为0的记录就没法正常登陆咯~

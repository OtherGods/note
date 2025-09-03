RPC 组件，顾名思义，就是把 RPC 相关的东西都封装到这个组件中了，主要就是依赖了 Dubbo。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503052325036.png)

pom 依赖，主要就是依赖dubbo ，以及需要用到 nacos 作为注册中心：
```xml
<!--   dubbo   -->  
<dependency>  
    <groupId>org.apache.dubbo</groupId>  
    <artifactId>dubbo-spring-boot-starter</artifactId>  
</dependency>  
  
<dependency>  
    <groupId>org.apache.dubbo</groupId>  
    <artifactId>dubbo-registry-nacos</artifactId>  
</dependency>
```

同时需要在 rpc.yml中增加一下必要的配置，在 dubbo 接入文档中介绍过了。

除了以上的配置外，还增加了一个注解和注解的切面处理的相关内容：
[10、Facade注解实现统一RPC结果包装](2、相关技术/24、项目/3、数藏项目/6、通用设计/10、Facade注解实现统一RPC结果包装.md)


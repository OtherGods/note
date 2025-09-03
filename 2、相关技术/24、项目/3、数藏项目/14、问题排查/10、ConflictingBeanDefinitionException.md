```java
Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.2024-04-28 20:13:42,620 ERROR [main org.springframework.boot.SpringApplication - Application run failedorg.springframework.context.annotation.ConflictingBeanDefinitionException: Annotation-specified bean name 'collectionFacadeService' for bean class [cn.hollis.nft.turbo.api.collection.service.CollectionFacadeService] conflicts with existing, non-compatible bean definition of same name and class [null] at org.springframework.context.annotation.ClassPathBeanDefinitionScanner.checkCandidate(ClassPathBeanDefinitionScanner.java:361) at org.mybatis.spring.mapper.ClassPathMapperScanner.checkCandidate(ClassPathMapperScanner.java:345) at org.springframework.context.annotation.ClassPathBeanDefinitionScanner.doScan(ClassPathBeanDefinitionScanner.java:288) at org.mybatis.spring.mapper.ClassPathMapperScanner.doScan(ClassPathMapperScanner.java:230)
```

最开始是因为我通过 Configuration 来配置 Dubbo 的 bean 的时候，通过NfTurboBusinessApplication启动的时候出现的这个问题，我以为是和其他的 bean 发生了冲突，然后就把所有的其他Configuration，甚至 collection 这个组件都给排除了。

但是还是没用。

于是静下心来仔细看了一下堆栈，发现在竟然和mybatis有关，于是去找到堆栈报错的源码，进行 debug。

org.springframework.context.annotation.ClassPathBeanDefinitionScanner#checkCandidate
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202412302349516.png)

这里是最终抛异常的地方，通过查看上下文，发现这里已经有一个已加载的 bean 了，然后尝试再加载 bean 的时候报错了。

这两个 bean 分别是：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202412302349862.png)

这里就很奇怪了，发现一个：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202412302349848.png)

`file [/Users/user/workspace/NFTurbo/nft-turbo-common/nft-turbo-api/target/classes/cn/hollis/nft/turbo/api/collection/service/CollectionFacadeService.class]` 这玩意咋会变成一个 bean 了呢？

这只是一个接口而已，咋会变成 bean 了呢，于是继续看看这段代码是谁调用到的，看看加载链是咋样的。于是找到了org.springframework.context.annotation.ClassPathBeanDefinitionScanner#doScan
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202412302350239.png)

通过查看运行时信息，看到以下内容：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202412302350146.png)

这里竟然要把各种并不是 bean 的东西都加载一遍？这是为啥呢？

后来看到`basePackage="cn.hollis.nft.turbo"`，并且是通过 mybatis 调用过来的，于是找到了问题：
```java
@SpringBootApplication(scanBasePackages = "cn.hollis.nft.turbo")  
@MapperScan(basePackages = "cn.hollis.nft.turbo")  
@EnableDubbo  
public class NfTurboBusinessApplication {  
      
    public static void main(String[] args) {  
        SpringApplication.run(NfTurboBusinessApplication.class, args);  
    }  
      
}
```

我的启动类`NfTurboBusinessApplication`指定的`@MapperScan(basePackages = "cn.hollis.nft.turbo")` ，这是个一个很大的根路径，他无差别的把定义的各种类都当作 mapper 给整成 bean 了。。。。。。

太坑了！！！

于是改成：
```java
@SpringBootApplication(scanBasePackages = "cn.hollis.nft.turbo")  
@MapperScan(basePackages = "cn.hollis.nft.turbo.**.mapper")  
@EnableDubbo  
public class NfTurboBusinessApplication {  
      
    public static void main(String[] args) {  
        SpringApplication.run(NfTurboBusinessApplication.class, args);  
    }  
      
}
```

问题解决。因为我们的代码路径是统一的，都是cn.hollis.nft.turbo.xxx.infrastructure.mapper ，所以使用通配符进行一下配置即可只扫描 mapper 目录下的包就行了。
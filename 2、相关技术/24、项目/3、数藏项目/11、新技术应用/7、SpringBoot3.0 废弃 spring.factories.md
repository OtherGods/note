在SpringBoot 2.7中，官方已经明确的说了，使用spring.factories这种自动配置的方式已经过时了，并且将在SpringBoot 3.0中彻底移除。

官方文档以及网上有很多资料也提到了，可以使用`org.springframework.boot.autoconfigure.AutoConfiguration.imports`文件替代。而我们的项目是基于 SpringBoot 3.0的，所以就不能再用spring.factories了。

在定义 starter 的时候，我们就需要用`org.springframework.boot.autoconfigure.AutoConfiguration.imports`文件。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503120002148.png)

其中内容如下：
```
cn.hollis.nft.turbo.job.config.XxlJobConfiguration
```

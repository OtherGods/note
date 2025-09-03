正常来说，我们的 business 包下面有很多个模块模块，每一个模块都是一个单独的微服务，他们之间的是通过 dubbo 的方式调用的。

所以，在部署的时候都是分开的，独立的。所以每一个包下面都有一个自己的Application 的启动入口类。

但是为了方便大家一键启动整个 business 中的所有应用，我们提供了一个 app 这个包，方便大家一键启动的。

所以，他的作用只是方便一键启动而已，正常的业务中不会需要这个。正常的业务 business 中每个包都是单独的一个应用，比如 order、trade、collection 都是单独的应用，单独的 git 地址，单独的 jar 包。

### 怎么做到的？

1、app 的 pom 中依赖了说有其他模块。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202501022246194.png)

2、app 的扫描包路径包含了其他所有模块中的 bean
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202501022249895.png)

3、app 的 yml文件中配置了所有其他模块中的配置的全集
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202501022250765.png)


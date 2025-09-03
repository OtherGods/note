#负载均衡 #Ribbon #Ribbon与Nginx对比 #服务端负载均衡 #客户端负载均衡 
# 典型回答

当我们在在对比`Ribbon`（包括`loadbalancer`）和`Nginx`的时候，主要对比的是他们的负载均衡方面的区别。

**这两者最主要的区别是==Nginx是一种服务端负载均衡==的解决方案，而==Ribbon是一种客户端负载均衡的解决方案==。**

- <font color="red" size=5>服务端负载均衡指的是将<font color="blue" size=5>负载均衡的逻辑集成到服务提供端</font>，通过在服务端对请求进行转发，实现负载均衡。</font>
  所以`Nginx`还是一个反向代理服务器（[2.3 正向代理和反向代理](Nginx%20从入门到实践，万字详解！#2.3%20正向代理和反向代理)），因为他做的是服务端负载均衡。
  ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508182327629.png)

- <font color="red" size=5>客户端负载均衡指的是将<font color="blue" size=5>负载均衡的逻辑集成到服务消费端</font>的代码中，在客户端直接选择需要调用的服务提供端，并发起请求</font>。这样的好处是可以在客户端直接实现负载均衡、容错等功能，不需要依赖其他组件，使得客户端具有更高的灵活性和可控性。
  ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508190000295.png)

<font color="red" size=5>Nginx是需要单独部署一个Nginx服务</font>的，这样他才能做好服务端负载均衡，而<font color="red" size=5>Ribbon是需要在服务消费端的机器代码中引入</font>，和应用部署在一起，这样他才能实现客户端的负载均衡。

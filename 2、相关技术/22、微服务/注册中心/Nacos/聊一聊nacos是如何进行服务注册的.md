# **前言**

Nacos 致力于帮助您发现、配置和管理微服务。Nacos 提供了一组简单易用的特性集，帮助您快速实现动态服务发现、服务配置、服务元数据及流量管理。作为springcload alibaba中的一员，越来越深受各种公司的青睐。本文就是在这个背景下剖析nacos服务注册的核心源码。

本文是基于nacos 1.4.1版本进行源码剖析。

下图本文源码分析核心机制的原理图，有需在线观看的同学关注公众号，发送 nacos01 即可获得图片在线链接。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506141700518.png)

# **一、NamingService介绍**

NamingService是nacos提供用来实现**服务注册、服务订阅、服务发现等功能的api**，由NacosNamingService唯一实现，通过这个api就可以跟nacos服务端实现通信。

本文也会着重剖析NamingService，看看客户端注册、心跳等特性实现的源码。  

# **二、服务注册源码剖析**

服务注册是通过registerInstance方法来实现的，这个方法有很多的重载的方法，只不过最后都调用
```java
registerInstance(String serviceName, String groupName, Instance instance) throws NacosException
```

来实现真正的注册的，接下来我们就进入该方法，来看看是如何注册到服务端的。  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506141703486.png)

我先说一个参数的意思

- **serviceName**：顾名思义，就是指服务的名称，比如什么商品服务之类的
- **groupName**：服务所在组的名称，nacos是支持group级别的隔离，也就是说，在一个namespace下，不同group之间的服务是相互隔离的。相互隔离的意思就是互不干扰，举个例子来说，比如ServiceA有两个实例，一个所在groupName为dev，一个所在groupName为prd，那么当有其他的服务在groupName为dev的情况下订阅的ServiceA，那么是订阅不到prd group的服务实例的。默认名称是DEFAULT_GROUP。
- **Instance**：就是服务信息的封装

接下来我们进入该方法的解析。  

**第一步**，就是将服务名和组名进行字符串的拼接，没有什么好说的；  
**第二步**，这个if条件，默认是true，不信你可以点进去看一下，这一步是创建一个心跳的任务，从这里我们可以看出，默认的情况下，nacos会为每个服务实例创建一个心跳。只不过一般一个客户端只会注册一个服务实例（想一想，哪个框架一个客户端可能会注册多个服务实例，公众号有剖析过这个框架）。
**第三步**，这一步就是调用api，发送请求给服务端，注册服务实例。  

我们进入第三步（心跳机制源码我们稍后再剖析），进入
serverProxy.registerService(groupedServiceName, groupName, instance);

这一行代码看看是怎么注册的。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506141710647.png)

这里我们看见，这个方法显示根据入参，构建了一个Map，然后调用reqApi，进入reqApi之前，我们看一下reqApi方法参数。
- 第一个参数名称 api，是常量 /nacos/v1/ns/instance，有兴趣可以自己点进去
- 第二个参数名称 params，就是构建的map  
- 第三个参数名称是 method，是HttpMethod.POST，看到http，这里我们就可以猜想，难道客户端跟服务端通信是通过http协议来实现的么，我们带着疑惑继续往下看。

接下来进入reqApi方法，一直进入最终的重载方法。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506141711127.png)

我们再分析一下新增的两个参数，
- body，默认是空  
- servers，就是我们配置的nacos服务端所在服务的ip和端口的集合，因为我们可能配置多个，这个应该不会为空吧，至少得配置一个吧。  

接下来进入重要的一段代码  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506141712264.png)

从这里可以看出，注册之前先是从服务端地址中随机选择一个进行调用，当调用失败的话，会再次选择一个进行重试。

假设我们选择了一个服务地址，接下来进入callServer方法
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506141713319.png)

这个方法比较简单了，就是将地址和请求路径名（ /nacos/v1/ns/instance ）进行拼接，然后发送http请求进行服务注册，然后接收客户端的响应。  

到这里，客户端的服务注册就完成了。

**从这里我们可以看出，nacos客户端跟服务端的通信其实是通过http请求来的，服务注册，就是客户端发送一个简单的http请求来完成的。**  

# **三、心跳机制源码剖析**

心跳机制，就是客户端定时向服务端发送请求，告诉服务端 “我还活着” ，服务端就会知道这个服务实例仍然可用。  

服务注册我们剖析完了，接下来回过头，来看看心跳机制的源码。  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506141715677.png)

首先是构建了一个BeatInfo，就是一个心跳信息参数的封装，就不点进去看了，接下来，我们进入addBeatInfo方法。  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506141715991.png)

这个方法就是通过BeatInfo构建一个BeatTask，然后扔到调度线程池，等待一定（默认是5s，你可以自己进去构建BeatInfo代码，这个参数默认是5s）时间之后执行，那么自然而然BeatTask就是心跳机制执行的逻辑，我们进入BeatTask代码。  
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506141720985.png)

BeatTask实现了Runable接口，所以我们直接看run方法的实现。

首先就是通过serverProxy发送一个http请求到服务端，服务端进行响应，其实这个过程就完成了跟服务端的心跳，接下来就是解析服务端的响应的数据了。根据服务端返回的不同的状态码进行判断，进行不同的操作。这里我们着重看一下这个响应码RESOURCE_NOT_FOUND （资源未找到）对应的操作
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506141720438.png)

这段代码其实就是重新向服务端进行服务的注册。有人肯定会好奇，为什么发送心跳的时候，服务端会告诉客户端，该服务实例在服务端找不到？不是只有注册服务的时候才会去构建心跳么，按道理应该存在的？是的，这是正常的情况下，在正常情况下，客户端发送心跳，服务实例应该存在于服务端的，但是有些情况，比如说网络抖动的时候，因为网络的问题，客户端无法给服务端发送心跳，长时间服务端无法接收到客户端的心跳，此时服务端认为你的这个服务实例出问题了，这样服务端就会主动从服务注册表中剔除该服务实例，该服务实例就不存在服务端了。当客户端网络好了的话，那么此时会恢复跟服务端的心跳机制，就会出现服务找不到的现象，这种情况下只要重新注册一下就行了。

接下来有一行很重要的代码，我们继续往下看。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202506141721423.png)

这行代码就是重新构建了一个BeatTask,，然后重新扔到调度线程池，等待nextTime之后执行。nextTime一般是BeatInfo的period，也就是默认是5s，但是有种情况，就是当发送心跳的实话，服务端可能会响应给客户端一个下一次发送心跳的时间间隔，此时就以nextTime就是服务端响应的时间，你可以自行查看nextTime的赋值情况。

通过这种形式，就实现了定时发送心跳的功能。当这次心跳完成之后，就会继续构建下一次心跳的任务，扔到调度线程池等待一定的时间之后执行，如此往复，就实现了定时发送心跳的机制。  

这里的这个定时的机制其实大家可以学习一下，这个定时机制实现了变频的功能，所谓的变频就是定时任务执行的间隔是不固定的，这次任务的执行才决定下一次任务执行的时间，这样有一个好处就是在一些场景中，如果有的任务频繁失败，那么是不是可以考虑让下一次执行的任务时间拉长，减少资源的浪费，nacos在定时更新本地服务实例列表的缓存也使用到了这个机制。这种机制其实在很多框架，中间件都有使用。

# **总结**

本文最开始介绍了NamingService接口的作用，通过这个接口就可以向服务端注册服务实例；接下来基于该接口，剖析了服务注册的源码，说白了就是发送http请求给服务端，然后服务端保存服务实例的数据；最后我们剖析了客户端的心跳机制，说白了就是构建BeatTask，定时(默认5s)向服务端发送请求。后面我会再写几篇文章，来剖析服务订阅、故障转移等机制以及nacos是如何整合springcloud的，包括注册中心和配置中心的整合源码剖析。
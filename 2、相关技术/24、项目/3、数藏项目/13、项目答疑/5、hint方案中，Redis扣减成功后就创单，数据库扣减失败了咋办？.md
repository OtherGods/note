在我们的秒杀的hint方案中，我们在redis中扣减完库存之后，就创建订单了，但是这时候数据库库存还没扣减。这时候如果应用挂了，或者事件丢了，或者数据库里面库存没有了，该咋办？

这是个非常好的问题。但是秒杀方案，有一个基本的宗旨：**以数据库为准！**

库存的扣减都是以数据库为准的，redis扣减成功并不代表一定成功，我们都以异步链路（springevent或者mq驱动的）的库存扣减为准的。

但是，那为啥hint这个方案，代码如下，redis扣减成功后，先创建订单，然后再扣减的数据库库存呢，这时候库存扣失败了不就有问题了么
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503122319400.png)

这个代码看上去是这样的，但是我们的订单有两个操作，一个create，一个confirm，在confirm之前，订单处于CREATE状态，这个状态的订单用户是无法查询到的，用户也无法基于CREATE的订单进行支付，对于用户来说，这笔订单其实是不存在的。

只有我们的异步过程，把数据库库存扣减成功后，订单推进到CONFIRM之后，用户才能看到这笔订单，才能进行支付。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503122321597.png)

所以，前端是有一段代码的，他会基于订单号轮询订单状态，处于CONFIRM之后，才会唤醒收银台 ，让用户去支付。

大家好，我是 **华仔**, 又跟大家见面了。

对于 RocketMQ 来说，大家用的最多的还是云服务，今天我们来讲解下 **RocketMQ 阿里云服务的相关功能讲解，帮助没有使用过的朋友们快速熟悉，下面进入正题**。

# **01 开通服务**

打开阿里云官方网址，找到「**消息队列 RocketMQ 版**」，点击「**产品控制台**」，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201404226.png)

进去之后，点击 「**免费开通**」按钮，勾选服务协议，点击「**立即开通**」，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201405627.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201405259.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201405231.png)

# **02 创建资源**

在消息队列 RocketMQ 版控制台，创建资源包括 「**创建实例**」、「**创建 Topic**」、「**创建 Group**」、「**获取 AK、SK**」。

这里需要注意的是：创建这些资源需要在「**同一个地域**」下，这里以「**公网地域**」为例。

我们先来创建实例。

## **2.1 创建实例**

点击控制台的「**地域**」下拉菜单，选择右下角的 「**公网**」进入：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201406300.png)

点击 「**创建实例**」、填写实例名称、实例描述，选择实例地域，最后点击 「**确定按钮**」，完成实例创建，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201406353.png)

## **2.2 创建 Topic**

创建完实例后，我们来创建 Topic，点击左侧菜单「**Topic 管理**」进入管理列表页，再点击 「**创建 Topic**」弹出创建 Topic 页面，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201406902.png)

填写 Topic 名称、Topic 描述，最后点击 「**确定按钮**」，完成 Topic 创建，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201407517.png)

## **2.3 创建 Group**

创建完 Topic 后，我们来创建对应的 Group，点击左侧菜单「**Group 管理**」，再点击 「**创建 Group**」弹出 Group 创建页面，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201407251.png)

填写 Group ID，Group 描述，最后点击 「**确定按钮**」，完成 Group 创建，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201407657.png)

## **2.4 创建 AK、SK**

点击右上角 「**头像**」菜单，点击「**AccessKey 管理**」按钮，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201408351.png)

再点击「**继续 AccessKey**」，进入 AccessKey 管理页面，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201408862.png)

进入列表后，再点击 「**创建 AccessKey**」按钮，会弹出填写手机验证的弹窗。
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201408237.png)

填写完成校验码，点击「**确定按钮**」，会自动创建 AccessKey，点击复制即可。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201409000.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201409480.png)

# **03 查看资源**

## **3.1 实例信息**

待上面的资源创建完成后，我们来查看相关资源，打开某个实例的详情页，可以查看到该实例的具体信息，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201409459.png)

## **3.2 仪表盘统计图**

点击「**仪表盘按钮**」可以展示总览的统计仪表图，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201410274.png)

消息业务指标概览，分为「**Topic 相关指标**」、「**ConsumerGroup 相关指标**」，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201410593.png)

消息生产速率、消息消费速率，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201410578.png)

已就绪消息量、已就绪消息排队时间，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201410607.png)

堆积消息量（已就绪消息+处理中消息），如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201411439.png)

这个仪表盘的统计图非常重要，**大家一定要学会观察，可以帮助你更好的监控你的 MQ 服务**。

## **3.3 消息查询**

我们可以点击左侧菜单「**消息查询**」，有三个选项可以选择：
1. 查询方式，有三种：
2. 按 Message ID 查询
3. 按 MessageKey 查询
4. 按 Topic 查询
5. 下拉选择对应的 Topic 名称。
6. 时间范围，支持固定时间段，也支持自定义时间点，自行选择。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201411665.png)

查询结果如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201411510.png)

点击列表右侧 「**详情**」，可以查看消息内容。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201412361.png)

点击列表右侧 「**消息轨迹**」，可以查看消息轨迹详情，如果有多个消费者，红框中会显示多个消费者的消费状况，是成功还是失败。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201412326.png)

点击红框下拉会展示对应消费详情，还可以导出 JSON 内容，如下图：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201412949.png)

## **3.4 Group 管理**

Group 可以支持两种协议：「**TCP 协议**」、「**HTTP 协议**」。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201412489.png)

点击列表右侧 「**详情**」，可以查看消息消费详情。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201413525.png)

红框可以告诉你这个消费者的消费状态，消费速度、是否有积压，排队时长是多少。可以更好的让你观察到消费请情况。

## **3.5 重置消费位点**

您可通过重置消费位点，按需清除堆积的或不想消费的这部分消息再开始消费，或直接跳转到某个时间点消费该时间点之后的消息（不论是否消费过该时间点之前的消息）。

> 注意事项:
> 
> 广播消费模式不支持重置消费位点。
> 
> 目前不支持指定Message ID、Message Key和Tag来重置消息的消费位点。
> 
> 当前在控制台上仅能重置TCP协议使用的Group ID的消费位点，不支持重置HTTP协议的Group ID的消费位点。

在左侧导航栏单击「**Group 管理**」，然后单击「**TCP 协议页签**」。然后找到需要重置消费位点的 Group ID，在其**操作列**单击**更多**，然后在下拉菜单中，选择「**重置消费位点**」。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201413511.png)

在重置消费位点面板中，按需选择以下选项，然后单击「**确定**」执行消费位点重置。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410201414607.png)

这里支持 2 种方式进行重置消费位点。
1. **从最新位点开始消费**：若选择此项，该Group ID在消费该Topic下的消息时会**跳过当前堆积（未被消费）的所有消息**，从这之后发送的最新消息开始消费。对于程序返回 reconsumeLater，即走重试流程的这部分消息来说，清除无效。
2. **如果选择从最新位点开始消费，则需要注意：**Group ID 在指定 Topic 中的堆积消息将被全部清除，该操作大概 2~3 分钟后生效，请勿重复操作。期间应用所有的消费者将暂停消费 2~3 分钟，**对延迟敏感**业务请谨慎使用。
3. **从指定时间点的位点开始消费**：选择该选项后会出现时间点选择的控件。请选择一个时间点，这个时间点之后发送的消息才会被消费。可选时间范围中的起始和终止时间分别是该 Topic 中储存的最早的和最晚的一条消息的生产时间。不能选择超过可选时间范围的时间点。

至此，整个 RocketMQ 云服务功能讲解就到此完毕，后续有补充再续。




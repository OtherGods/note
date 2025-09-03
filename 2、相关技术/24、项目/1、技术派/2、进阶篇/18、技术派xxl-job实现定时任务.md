
# 1、前言

xxl-job是一个分布式任务调度平台，其核心设计目标是开发迅速、学习简单、轻量级、易扩展。现已开放源代码并接入多家公司线上产品线，开箱即用。（其中XXL是作者许雪里的简称）

xxl-job是对老牌调度平台Quartz进行的封装。在开始介绍xxl-job之前我来先简单的介绍下Quartz以及对其与xxl-job做个对比。

Quartz是一款老牌的任务调度平台，他也是用Java编写的，调度模型已经非常成熟了，而且很容易集成到 Spring 中去，用来执行业务是一个很好的选择。但是它也面临着一些问题，比如：
1. 调度逻辑（Scheduler）和任务类耦合在同一个项目中，随着调度任务数量逐渐增多，同时调度任务逻辑逐渐加重，调度系统的整体性能会受到很大的影响；
2. Quartz 集群的节点之间负载结果是随机的，谁抢到了数据库锁就由谁去执行任务，这就有可能出现旱的旱死，涝的涝死的情况，发挥不了机器的性能。
3. Quartz 本身没有提供动态调度和管理界面的功能，需要自己根据API进行开发。
4. Quartz 的日志记录、数据统计、监控不是特别完善。

xxl-job对比下来有如下特性：
1. 性能的提升：可以调度更多任务。
2. 可靠性的提升：任务超时、失败、故障转移的处理。
3. 运维更加便捷：提供操作界面、有用户权限、详细的日志、提供通知配置、自动生成报表等等。

# 2、xxl-job简述

下面先放一张我从官网找的的最新的架构图，是v2.1.0版本的，不是代码最新的架构图，但是几乎大差不差，右下角红框标出来的自研 RPC(xxl-rpc)，在 V2.2.0 里面已经替换成了 restful 风格的 http 请求方式。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101649108.png)

把上面的图精简了一下，xxl-job 的调度器和业务执行是独立的。调度器决定任务的调度，并且通过 http 的方式调用执行器接口执行任务。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101650394.png)

**架构设计思想：**
1、将调度行为抽象形成“调度中心”公共平台，而平台自身并不承担业务逻辑，“调度中心”负责发起调度请求。
2、将任务抽象成分散的JobHandler，交由“执行器”(可以理解为就是我们的服务实例)统一管理，“执行器”负责接收调度请求并执行对应的JobHandler中业务逻辑。

因此，“调度”和“任务”两部分可以相互解耦，提高系统整体稳定性和扩展性；

PS：xxl-job里面具体内容我这里不再详述，请大家移步至xxl-job的XXL开源社区里面有详细介绍。

# 3、xxl-job安装与使用
## 3.1、安装

技术派中围绕的目前xxl-job最新的v.2.4.0版本来进行整合。
首先去官网下载代码。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101659955.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101659902.png)

拉下代码后在idea中打开会有如下所示目录：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101700884.png)

1、doc：文档资料，包括"调度数据库"建表脚本
2、xxl-job-core：公共 Jar 依赖
3、xxl-job-admin：调度中心，项目源码，spring boot 项目，可以直接启动
4、xxl-job-executor-samples：执行器，sample 示例项目，其中的 spring boot 工程，可以直接启动。可以在该项目上进行开发，也可以将现有项目改造生成执行器项目。

**在拉下代码之后还需要以下几步才能正常启动：**
**第一步**：去doc文件夹中拿取SQL脚本放到自己MySQL数据库中执行SQL；
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101704921.png)

运行完SQL脚本之后，你的数据库中会有如下表：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101706356.png)

**第二步**：在admin模块中修改applicant.properties配置文件(启动端口号可以修改、数据库配置需要修改)
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312101709940.png)

## 3.2、xxl-job启动

直接启动xxl-job-admin模块
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102110390.png)

启动成功之后，在浏览器端直接访问web页面URL：http://127.0.0.1:8089/xxl-job-admin/(注意端口号是你自己所设置的)，
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102110923.png)

登录进去之后会是如下的页面：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102110899.png)

这里我先不进行去详细的讲解其操作步骤，在下面的SpringBoot和xxl-job整合之后我再进行详细的介绍。

走到这一步说明我们的xxl-job准备工作已经做好了，下一步就是与SpringBoot进行整合。

# 4、SpringBoot整合xxl-job实现定时任务
## 4.1、引入ssl-job依赖

```xml
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.1-SNAPSHOT</version>
</dependency>
```

## 4.2、yml配置文件中配置xxl-job配置

```yml
# Xxl-Job分布式定时任务调度中心
xxl:
  job:
    admin:
      # 调度中心部署跟地址 [选填]：如调度中心集群部署存在多个地址则用逗号分隔。
      # 注意这里的端口号
      addresses: http://127.0.0.1:8089/xxl-job-admin
      # addresses: http://192.168.110.2:9090/xxl-job-admin
    # 执行器通讯TOKEN [选填]：非空时启用 系统默认 default_token
    accessToken: default_token
    executor:
      # 执行器的应用名称
      appname: pai-coding
      # 执行器注册 [选填]：优先使用该配置作为注册地址
      address: ""
      # 执行器IP [选填]：默认为空表示自动获取IP
      ip: ""
      # 执行器端口号 [选填]：小于等于0则自动获取；默认端口为9999
      port: 9998
      # 执行器运行日志文件存储磁盘路径 [选填] ：需要对该路径拥有读写权限；为空则使用默认路径；
      logpath:
      #logpath: /data/logs/mls/job
      # 执行器日志文件保存天数 [选填] ： 过期日志自动清理, 限制值大于等于3时生效; 否则, 如-1, 关闭自动清理功能；
      logretentiondays: 7
```

在配置文中的注释中已经详细介绍其各个功能，这里不做详细描述，请仔细看注释哈。

注意：address属性中的端口号一定要注意，这里需要和你上面的xxl-job admin模块中的配置server.port相对应。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102128249.png)

## 4.3、编写xxl-job的config配置文件

```java
package com.xxl.job.executor.core.config;  
  
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
import org.springframework.beans.factory.annotation.Value;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
  
/**  
 * xxl-job config * * @author xuxueli 2017-04-28  
 */@Configuration  
public class XxlJobConfig {  
    private Logger logger = LoggerFactory.getLogger(XxlJobConfig.class);  
  
    @Value("${xxl.job.admin.addresses}")  
    private String adminAddresses;  
  
    @Value("${xxl.job.accessToken}")  
    private String accessToken;  
  
    @Value("${xxl.job.executor.appname}")  
    private String appname;  
  
    @Value("${xxl.job.executor.address}")  
    private String address;  
  
    @Value("${xxl.job.executor.ip}")  
    private String ip;  
  
    @Value("${xxl.job.executor.port}")  
    private int port;  
  
    @Value("${xxl.job.executor.logpath}")  
    private String logPath;  
  
    @Value("${xxl.job.executor.logretentiondays}")  
    private int logRetentionDays;  
  
  
    @Bean  
    public XxlJobSpringExecutor xxlJobExecutor() {  
        logger.info(">>>>>>>>>>> xxl-job config init.");  
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();  
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);  
        xxlJobSpringExecutor.setAppname(appname);  
        xxlJobSpringExecutor.setAddress(address);  
        xxlJobSpringExecutor.setIp(ip);  
        xxlJobSpringExecutor.setPort(port);  
        xxlJobSpringExecutor.setAccessToken(accessToken);  
        xxlJobSpringExecutor.setLogPath(logPath);  
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);  
  
        return xxlJobSpringExecutor;  
    }  
  
    /**  
     * 针对多网卡、容器内部署等情况，可借助 "spring-cloud-commons" 提供的 "InetUtils" 组件灵活定制注册IP；  
     *  
     *      1、引入依赖：  
     *          <dependency>  
     *             <groupId>org.springframework.cloud</groupId>  
     *             <artifactId>spring-cloud-commons</artifactId>  
     *             <version>${version}</version>  
     *         </dependency>  
     *     *      2、配置文件，或者容器启动变量  
     *          spring.cloud.inetutils.preferred-networks: 'xxx.xxx.xxx.'  
     *     *      3、获取IP  
     *          String ip_ = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();     */  
  
}
```

## 4.4、代码编辑执行任务

在要执行的定时任务方法上写上@XxlJob()注解，表示该方法需要被执行。代码如下所示：
```java
// 注解内部属性是任务名称，在xxl-job的任务管理中新增任务时会用到这个注解内的属性值
@XxlJob("autoRefreshCache")
public void autoRefreshCache() {
    log.info("开始刷新sitemap.xml的url地址，避免出现数据不一致问题!");
    refreshSitemap();
    log.info("刷新完成！");
}
```

SpringBoot代码编写到这里已经结束了，只需要启动该项目即可。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102133558.png)

## 4.5、在xxl-job任务调度中心添加执行器

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102133228.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102135703.png)

注意：先添加执行器，然后再启动项目，也就是在上一节中先不要启动项目。

然后当我们启动我们项目(执行器)之后，等一下下(给我们的项目注册进xxl-job中一点时间)，此时我们就会    看到成功注册(记得刷新下网页)。如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102137846.png)

点击onLine机器地址对应的查看后，可以看到我们执行器地址，如下所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102137371.png)

## 4.6、在xxl-job任务调度中心添加任务管理

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102145577.png)

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102146711.png)

然后保存后就可以看到该任务
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102147631.png)

将stop状态设置成启动状态。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102147363.png)

## 4.7、测试xxl-job

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102148211.png)

点击一下——执行一次
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102149312.png)

## 4.8、xxl-job日志查看

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102153121.png)

当然查询总的日志在菜单——运行报表中也可以查看
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102154001.png)

# 5、总结

这篇文章可以说是保姆级的教程了，下面我再来个总结吧。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202312102154387.png)



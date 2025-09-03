# 1、前言
## 1.1、什么是MQ

在正式开始分享Kafka之前，我想先讲讲MQ(Message Queue)，它是消息队列，是一种FIFO(先进先出)数据结构。消息由生产者发送给MQ中进行排队，然后按照原来顺序交由消息的消费者进行处理。QQ和微信就是典型的MQ。其大致架构如下所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311012339005.png)

## 1.2、MQ优缺点


1. **优点**：
	1. 异步：异步提高系统响应速度、吞吐量；
	2. 解耦：方便系统更好扩展性；还能减少系统服务间的影响，提升维护性；
	3. 削峰：提升系统稳定性(能够更好的应对系统突发流量冲击)。
2. **缺点**：
	1. 系统可用性降低(MQ中间件一旦宕机那么就将出现问题)；
	2. 系统复杂度提高(引入了中间件)；
	3. 消息一致性问题(消息可能会出现丢失、延迟和重复消费等问题)。
# 2、Kafka介绍

Kafka是最初由Linkedin公司开发，是一个分布式、支持分区的（partition）、多副本的（replica），基于zookeeper协调的分布式消息系统，它的最大的特性就是可以实时的处理大量数据以满足各种需求场景：比如基于hadoop的批处理系统、低延迟的实时系统、Storm/Spark流式处理引擎，web/nginx日志、访问日志，消息服务等等，用scala语言编写， Linkedin于2010年贡献给了Apache基金会并成为顶级开源项目。

## 2.1、Kafka使用场景

- 日志收集：一个公司可以用Kafka收集各种服务的log，通过kafka以统一接口服务的方式开放给各种 consumer，例如hadoop、Hbase、Solr等。
- 消息系统：解耦和生产者和消费者、缓存消息等。
- 用户活动追踪：Kafka经常被用来记录web用户或者app用户的各种活动，如浏览网页、搜索、点击等活动，这些活动信息被各个服务器发布到kafka的topic中，然后订阅者通过订阅这些topic来做实时的监控分析，或者装载到 hadoop、数据仓库中做离线分析和挖掘。
- 运营指标：Kafka也经常用来记录运营监控数据。包括收集各种分布式应用的数据，生产各种操作的集中反馈，比如报警和报告。

## 2.2、Kafka基本概念

kafka是一个分布式的，分区的消息(官方称之为commit log)服务。

首先我们来看看基础消息(Message)相关术语

| 名词         | 解释                                                                                                                                           |
| ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Broker       | 消息中间件处理节点，一个Kafa节点就是一个broker，一个或者多个Broker可以组成一个Kafa集群                                                         |
| Topic        | Kafka根据topic对消息进行归类，发布到Kafa集群的每条消息都需要指定一个topic                                                                      |
| Producer     | 消息生产者，向Broker发送消息的客户端                                                                                                           |
| Consumer     | 消息消费者，从Broker读取消息的客户端                                                                                                           |
| ConsumerGrop | 每个Consumer属于一个特定的COnsumer Group，一条消息可以被多个不同的Consumer Group消费，但是一个Consumer Group中只能有一个Consumer能够消费该消息 |
| Partition    | 物理上的概念，一个Topic可以分为多个partition，每个partition内部消息是有序的                                                                    | 

因此，从更高的层面上来看，producer通过网络发送消息到kafka集群，然后consumber来进行消费，如下图所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311022259175.png)

服务端（brokers）和4客户端（producer、consumer）之间通信通过TCP协议来完成

**注意**：
	1. 分区patition：一个topic可以有多个patition分区，每个分区只能让其同一个消费组下的一个消费者消费，不能让同一个消费组下的多个消费者消费；但是一个消费者可以同时去消费多个分区
		1. 同一个topic下的patition可以分布在不同的Broker上；kafka的副本是存放在不同Broke上面，在创建主题设置副本数时应该小于等于Broke数。
	2. ConsumerGroup：一条消息可以被多个不同的Consumer Group消费，但是一个 Consumer Group中只能有一个Consumer能够消费该消息

# 3、Kafka下载安装

这里由于下载和安装比较简单，就不单独拿来给大家讲解了，下面给大家个链接，大家来按照他的步骤来进行下载和安装；如果大家在安装中有什么问题，可以直接艾特我或者在评论中留言。

《kafka下载与安装教程》，这一篇是安装的单节点的，其实已经足够我们自己测试使用；如果大家想安装多节点构成分布式集群的，可以使用这篇文章——《 kafka 下载安装》

注意：
1、一定要安装好JDK，因为Kafka是Scala语言开发的，它是运行在JVM上的。
2、一定要先启动Zookeeper；因为它是Kafka的注册中心。

PS：在这里我想再絮叨一句就是——这篇文章只是分享大家下载安装和使用，它其实还有很多概念与知识点这里没有覆盖，只是讲了大家常用的知识点，剩下的
就需要大家课下进行补充了。

# 4、技术派中整合Kafka

上面讲到了Kafka具有这么多的优点，我们技术派中怎么能不去使用呢，下面就来引入我们的正文——技术派整合Kafka。

## 4.1、技术派使用Kafka背景

使用的背景是当我们对文章进行评论点赞、收藏、评论或者管理员用户发送系统消息的时候，那么对应的文章用户就可以实时的来接受到其消息。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311022349188.png)

在技术派中使用Kafka消息队列，能够使我们项目异步加速(只负责投递)；服务解耦(投递消息和消费消息分开来，不用关心消息是否执行成功)；拥有更好扩展性和维护性(例如可以直接扩展短信通知等等)；

下面通过图例来讲解项目中的使用逻辑：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311022351500.png)

## 4.2、技术派集成Kafka生产者

生产者核心逻辑就是将消息投递至Kafka中，技术派中利用自定义注解在AOP切面中使用环绕通知将消息投递至Kafka中。

将消息投递至Kafka流程如下所示：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311022356902.png)

### 4.2.1、SpringBoot整合Kafka

1、引入Kafka依赖
```xml
<!-- 引入Kafka -->
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
  <version>2.7.8</version>
</dependency>
```

2、yml中配置Kafka
```yml
spring:
# kafka配置
  kafka:
  # kafka地址
    bootstrap-servers: 124.222.0.1:9092
    # 生产者
    producer:
      retries: 0
      acks: 0
      batch-size: 16384
      buffer-memory: 33554432
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    # 消费者
    consumer:
      group-id: study_log
      enable-auto-commit: true
      auto-commit-interval: 100
      # 这里offset设计的是latest
      auto-offset-reset: latest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

### 4.2.2、自定义注解实现方法AOP切面

1、自定义发送消息注解@RecordOperate，代码如下所示：
RecordOperate注解
```java
package com.github.paicoding.forum.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 记录注解
 *
 * @ClassName: RecordOperate
 * @Author: ygl
 * @Date: 2023/16/14 22:50
 * @Version: 1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RecordOperate {

	/**
     * 模块
     */
    String title() default "";
    
    /**
     * 功能
     * 这个是指业务类型，一般来说有：评论、回复、点赞、收藏、关注和系统等
     */
    String businessType() default "其它";
    
    /**
     * 是否保存请求的参数
     */
    boolean isSaveRequestData() default true;
    
    /**
     * 是否保存响应的参数
     */
    boolean isSaveResponseData() default true;
    
    String desc() default "";
	
}
```

2. 切面整合消息且发送消息至Kafka，代码如下所示：
```java
package com.github.paicoding.forum.web.aspect;

import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.enums.NotifyTypeEnum;
import com.github.paicoding.forum.core.annotation.RecordOperate;
import com.github.paicoding.forum.core.dto.ArticleKafkaMessageDTO;
import com.github.paicoding.forum.core.util.IpUtils;
import com.github.paicoding.forum.core.util.ServletUtils;
import com.github.paicoding.forum.service.article.repository.entity.ArticleDO;
import com.github.paicoding.forum.service.article.service.ArticleReadService;

import lombok.extern.slf4j.Slf4j;

/**
 * 操作切面
 *
 * @ClassName: OperateAspect
 * @Author: ygl
 * @Date: 2022/11/18 14:55
 * @Version: 1.0
 */
@Aspect
@Component
@Slf4j
public class OperateAspect {

	@Autowired
    ArticleReadService articleReadService;
    
    @Autowired
    KafkaTemplate kafkaTemplate;
    
    /**
     * 1、定义切入点
     * 2、横切逻辑
     * 3、织入
     */
     @Pointcut(value = "@annotation(recordOperate)")
    public void pointcut(RecordOperate recordOperate) {
    }
    
    /**
     * 处理完请求后执行
     */
     @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
     public void doAfterReturning(JoinPoint joinPoint, RecordOperate controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }
    
    /**
     * 拦截异常操作
     *
     * @param joinPoint 切点
     * @param e         异常
     */
     @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, RecordOperate controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }
    
    protected void handleLog(final JoinPoint joinPoint, RecordOperate controllerLog, final Exception e, Object jsonResult) {
	    try{
		    // 请求的地址
		    String ip = IpUtils.getIpAddr(ServletUtils.getRequest());
		    HttpServletRequest request = ServletUtils.getRequest();
		    // url
		    String requestURI = "http://xxx.xxx.xxx.xxx/default_url";
		    // 设置请求方式
			String method = "defaultMethod";
			if (ObjectUtils.isNotEmpty(request)) {
				requestURI = request.getRequestURI();
				method = request.getMethod();
			}
		    // 设置方法名称
			String className = joinPoint.getTarget().getClass().getName();
			String methodName = joinPoint.getSignature().getName();
			// 处理设置注解上的参数
			// 处理action动作
			String businessType = controllerLog.businessType();
			// 设置标题
			String title = controllerLog.title();
			String[] params = requestValue(joinPoint, request, title).split("&");
		    // 将消息给kafka
		    this.sendKafkaMessage(params);
	    } catch {
		    // 记录本地异常日志
            log.error("==前置通知异常==");
            log.error("异常信息:{}", exp.getMessage());
            exp.printStackTrace();
	    }
	}
	
	private void sendKafkaMessage(String[] params) {
		// 谁向谁的那篇文章点赞了
		String sourceName = ReqInfoContext.getReqInfo().getUser().getUserName();
		String articleIdStr = params[0].split("=")[1];
		Long articleId = Long.parseLong(articleIdStr);
		String typeStr = params[1].split("=")[1];
        int type = Integer.parseInt(typeStr);
        String typeName = NotifyTypeEnum.typeOf(type).getMsg();
		ArticleDO articleDO = articleReadService.queryBasicArticle(articleId);
		String articleTitle = articleDO.getTitle();
		Long targetUserId = articleDO.getUserId();
		ArticleKafkaMessageDTO articleKafkaMessageDTO = new ArticleKafkaMessageDTO();
		articleKafkaMessageDTO.setType(type);
        articleKafkaMessageDTO.setSourceUserName(sourceName);
        articleKafkaMessageDTO.setTargetUserId(targetUserId);
        articleKafkaMessageDTO.setArticleTitle(articleTitle);
        articleKafkaMessageDTO.setTypeName(typeName);
        // 发送消息至Kafka;kafkaTemplate.send(topic,message);
        kafkaTemplate.send("paicoding_aricle", JSON.toJSONString(articleKafkaMessageDTO));
	}
	
	/**
     * 获取请求的参数，放到log中
     *
     * @throws Exception 异常
     */
     private String requestValue(JoinPoint joinPoint, HttpServletRequest request, String title) throws Exception {
	     String requestMethod = request.getMethod();
		 String param = "";
		 if (HttpMethod.PUT.name().equals(requestMethod) || HttpMethod.POST.name().equals(requestMethod)) {
            String params = argsArrayToString(joinPoint.getArgs());
            param = StringUtils.substring(params, 0, 2000);
        } else {
	        if (ObjectUtils.isNotEmpty(request)) {
		        Map<String, String[]> parameterMap = request.getParameterMap();
		        if (StringUtils.equals(title, "article")) {
			        String articleId = parameterMap.get("articleId")[0];
                    String type = parameterMap.get("type")[0];
                    param = "articleId=" + articleId + "&type=" + type;
		        }
	        }
        }
        return param;
     }
	 
	 /**
     * 参数拼装
     */
    private String argsArrayToString(Object[] paramsArray) {
	    String params = "";
        if (paramsArray != null && paramsArray.length > 0) {
            for (Object o : paramsArray) {
	            if (ObjectUtils.isNotEmpty(o) && !isFilterObject(o)) {
                    try {
	                    Object jsonObj = JSON.toJSON(o);
                        params += jsonObj.toString() + " ";
                    } catch (Exception e) {
                        log.info(e.toString());
                    }
				}
            }
		}
		return params.trim();
    }
    
    /**
     * 判断是否需要过滤的对象。
     *
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
     public boolean isFilterObject(final Object o) {
	     if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (
			Collection collection = (Collection) o;
            for (Object value : collection) {
                return value instanceof MultipartFile;
            }
        } else if(Map.class.isAssignableFrom(clazz)) {
            Map map = (Map) o;
            for (Object value : map.entrySet()) {
                Map.Entry entry = (Map.Entry) value;
                return entry.getValue() instanceof MultipartFile;
			}
		}
		return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
     }
}
```

上面的切面方法中使用的是环绕通知，将一些消息做了整合，然后将KafkaTemplate.send()将消息发送给Kafka中

下面我们来测试下；可以发现当我们在点赞、评论和收藏文章时将消息投递给Kafka；
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311041840640.png)

可以看到我们Kafka中所收到了点赞、评论和收藏等消息。
这就说明了生产者成功的将消息投递至Kafka中。

## 4.3、技术派集成Kafka消费者

上一节中已经成功将消息投递至生产者中，接下来就可以进行将消息消费。

在生产者中yml的kafka配置文件中已经配置消费者，其中尤其注意下group-id、enable-auto-commit和auto-offset-reset这三个配置。
- group-id：消费者组；注意下，一个topic主题下的同一个partition只能被同一个消费者组下的一个消费者消费，不能被同一个消费者组下的多个消费者消费，但是同一个消费者组下的一个消费者可以消费多个partition。这句话可能有点绕，大家要多读两遍注意下。
- enable-auto-commit：是否自动提交。这里可以根据自身的业务实际情况来进行开启或者关闭。
- auto-offset-reset：这个就涉及到你应该读取到那条消息了，一般常用latest，代表当各分区下有已提交的offset时，从提交的offset开始消费；无提交的offset时，消费新产生的该分区下的数据；

消费者代码如下所示：
```java
package com.github.paicoding.forum.service.article.repository.listener;

import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.github.paicoding.forum.api.model.constant.KafkaTopicConstant;
import com.github.paicoding.forum.api.model.dto.ArticleKafkaMessageDTO;
import com.github.paicoding.forum.service.constant.RedisConstant;
import com.github.paicoding.forum.service.utils.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * kafka监听文章数据
 *
 * @ClassName: ArticleKafkaListener
 * @Author: ygl
 * @Date: 2023/6/15 14:26
 * @Version: 1.0
 */
@Component
@Slf4j
public class ArticleKafkaListener {

    @Autowired
    private RedisUtil redisUtil;
    
    private final String totalPre = RedisConstant.REDIS_PAI + RedisConstant.REDIS_PRE_ARTICLE
            + RedisConstant.TOTAL;
	private final String praisePre = RedisConstant.REDIS_PAI + RedisConstant.REDIS_PRE_ARTICLE
            + RedisConstant.PRAISE;
	private final String collectionPre = RedisConstant.REDIS_PAI + RedisConstant.REDIS_PRE_ARTICLE
            + RedisConstant.COLLECTION;
    private final String commentPre = RedisConstant.REDIS_PAI + RedisConstant.REDIS_PRE_ARTICLE
            + RedisConstant.COMMENT;
	private final String recoverPre = RedisConstant.REDIS_PAI + RedisConstant.REDIS_PRE_ARTICLE
            + RedisConstant.RECOVER;
    
    /**
     * 监听主题为paicoding_article的消息
     * @param consumerRecord
     */
     @KafkaListener(topics = {KafkaTopicConstant.ARTICLE_TOPIC})
    public void consumer(ConsumerRecord<?, ?> consumerRecord) {
	    log.info("监听中、、、");
        Optional<?> value = Optional.ofNullable(consumerRecord.value());
        if (value.isPresent()) {
            String msg = value.get().toString();
            String msgStr = JSONObject.toJSONString(msg);
            ArticleKafkaMessageDTO articleKafkaMessageDTO = JSONObject.parseObject(msg, ArticleKafkaMessageDTO.class);
            int type = articleKafkaMessageDTO.getType();
            Long userId = articleKafkaMessageDTO.getTargetUserId();
            
            // 下面是业务逻辑处理
            // 2-点赞、4-取消点赞；3-收藏、5-取消点赞；
            if (type == 2) {
                redisUtil.incr(praisePre + userId, 1);
                redisUtil.incr(totalPre + userId, 1);
            } else if (type == 4) {
	            redisUtil.decr(praisePre + userId, 1);
                redisUtil.decr(totalPre + userId, 1);
            } else if (type == 3) {
                redisUtil.incr(collectionPre + userId, 1);
                redisUtil.incr(totalPre + userId, 1);
            } else if (type == 5) {
                redisUtil.decr(collectionPre + userId, 1);
                redisUtil.decr(totalPre + userId, 1);
            } else if (type == 6) {
                redisUtil.incr(commentPre + userId, 1);
                redisUtil.incr(totalPre + userId, 1);
            } else if (type == 8) {
                redisUtil.incr(recoverPre + userId, 1);
                redisUtil.incr(totalPre + userId, 1);
			}
			log.info("消费消息：{}", msgStr);
		}
    }
}
```

在Kafka消费者中其实最主要也就是@KafkaListener(topics = {KafkaTopicConstant.ARTICLE_TOPIC})注解，它会监听到我们设置的topic主题下的消息。

当我们监听到消息后就可以将消息根据业务需求处理了。这里的业务需求我就不处理了，在下面的总结中我来画个图来概述下实现逻辑。下面我来测试下所监听的数据：
![](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311041857239.png)


上图说明消息已经监听成功。

# 5、总结

老规矩，以一张图对该文章做个总结，这张图中我准备对其做的详细些，包含其业务实现逻辑。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202311041858483.png)


这个业务中我们只用到了AOP切面环绕通知、消息队列Kafka和分布式缓存Redis。希望大家好好总结下，这里对知识提升还是面试过程中当做项目亮点来说都是比较有技术含量的，这不就打破了我们经常所说的我们只会CRUD了嘛。

送上大家一句话：希望若干年之后回头望现在的自己，不要有后悔的想法。
# 六、SpringBoot配置全局事务处理

## 6.1 SringBoot声明式事务

Spring Boot开启声明式事务，只需要一个@Transactional就可以。因为在SpringBoot 中已经默认对jpa和mybatis，hibernate开启了事务，在引入引用的时候自动就开启了事务。

## 6.2 @Transactional注解的常用属性：

| 参数名称               | 功能描述                                                     |
| ---------------------- | ------------------------------------------------------------ |
| readOnly               | 该属性用于设置当前事务是只读，true代表只读，false代表可读写，默认为false |
| rollbackFor            | 该属性设置进行回滚的异常类数组，当方法中抛出异常类数组中的异常时则进行回滚，例如：**指定多个异常类**：@Transactional(rollbackFor={RuntimException.class,Exception.class}) |
| rollbackForClassName   | 该属性用于设置需要进行回滚的异常类名称数组，当方法中抛出指定异常名称数组中的异常时，则进行事务回滚，例如：**指定多个异常类名称**：@Transactional(rollbackFoeClassName={“RuntimeException”,“Exception”}) |
| noRollbackFor          | 该属性用于设置不需要进行回滚的异常类数组                     |
| noRollbackForClassName | 该属性用于设置不需要进行回滚的异常类名称数组                 |
| propagation            | 该属性用于设置事务的传播行为                                 |
| islation               | 该属性用于设置底层数据库的事务隔离级别，隔离级别用于处理多事务并发的情况，通常使用数据库的默认隔离级别即使可，基本不需要进行设置 |
| timeout                | 该属性用于设置事务的超时秒数，默认值为-1，表示永不超时       |
|                        |                                                              |



## 6.3 事务传播行为propagation

**`所谓事务的传播行为`**是指：如果在开始当前事务之前，一个事务上下文已进存在，此时有若干选项可以指定一个事务性方法的执行行为。在TransactionDefinition(事务定义)中包括了如下几个表示传播行为的常量:

1. PROPAGATION_REQUIRED	如果当前事务存在事务，则加入该事务；如果当前没有事务，则创建一个新的事务，这是默认值。
2. PROPAGATION_REQUIRES_NEW	创建一个新的事务，如果当前存在事务，则把当前事务挂起
3. PROPAGATION_SUPPORTS	如果当前存在事务，则加入该事务；如果不存在事务，则以非事务的方式继续执行
4. PROPAGATION_NOT_SUPPORTED	以非事务机制运行，如果当前存在事务，则把当前事务挂起
5. PROPAGATION_NEVER	以非事务机制运行，如果当前存在事务，则抛出异常
6. PROPAGATION_MANDATORY	如果当前存在事务，则加入该事务；如果当前没有事务，则抛出异常
7. PROPAGATION_NESTED	如果当前存在事务，则创建一个事务作为当前事务的嵌套事务来运行；如果当前没有事务，则该取值等价于PROPAGATION_REQUIRED

## 6.4 配置全局事务

### 1、配置注解事务管理机制

首先要在Spring Boot工程的主入口类中开启事务支持注解：

> //开启事务支持
> @EnableTransactionManagement

```java
package com.kdcrm.main;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = "com.kdcrm")
@MapperScan(basePackages = "com.kdcrm.mapper") //扫描mapper映射配置文件
@EnableTransactionManagement //开启事务支持
public class KdcrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(KdcrmApplication.class, args);
    }
}
```



### 2、使用@Transactional注解

在需要事务支持的服务类（class）或方法（method）上，加上注解并设置其属性，如果在业务层类上标注事务注解，表名此类中所有方法均开启事务机制；如果仅在某个方法上配置事务注解，仅表明仅此方法开启事务机制。

```java
/**  
     * propagation = Propagation.REQUIRED  设置事务的传播机制
     * isolation = Isolation.READ_COMMITTED  设置事务的隔离级别
     * rollbackFor = Exception.class  设置事务的异常回滚机制
     * @param record
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,isolation = Isolation.READ_COMMITTED,rollbackFor = Exception.class)
    public int updateByPrimaryKey(SystemUserinfo record) {
        return systemUserinfoMapper.updateByPrimaryKey(record);
    }

```



### 3、声明全局事务

通过配置类注解@Configuration+@Bean完成Spring全局事务声明，后续事务就不需要在每个业务层的方法上通过注解开启事务。这样统一管理，简化代码。

1. 创建一个通知bean：定义声明事务的具体逻辑。
2. 创建一个建议者bean【配置通知器】：包括切面和逻辑。定义切面并设置切点 pointcut，引用通知bean作为事务逻辑。



### 4、GlobalTransactionHandler类

这种方式等价于《Spring框架专题(八)-Spring事务管理.md》中的5.3节中配置xml文件方式

```java
package com.kdcrm.controller;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration //相当于当前类为spring的xml文件中的<beans>配置
public class GlobalTransactionHandler {


    @Autowired
    private PlatformTransactionManager transactionManager; //事务管理

    /**
     * 事务管理配置
     */
    @Bean
    public TransactionInterceptor getTxAdvice(){

        //设置第一个事务管理的模式（适用于“增删改”）
        RuleBasedTransactionAttribute txAttr_required = new RuleBasedTransactionAttribute();
        //设置事务传播行为（当前存在事务则加入其中，如果没有则新创建一个事务）
        txAttr_required.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        //指定事务回滚异常类型（设置为“Exception”级别）
        txAttr_required.setRollbackRules(Collections.singletonList(new RollbackRuleAttribute(Exception.class)));
        //设置事务隔离级别（读以提交的数据,此处可不做设置，数据库默认的隔离级别就行）
        txAttr_required.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);


        //设置第二个事务管理的模式（适用于“查”）
        RuleBasedTransactionAttribute txAttr_readonly = new RuleBasedTransactionAttribute();
        //设置事务传播行为（当前存在事务则挂起，继续执行当前逻辑，执行结束后恢复上下文事务）
        txAttr_readonly.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        //指定事务回滚异常类型（设置为“Exception”级别）
        txAttr_readonly.setRollbackRules(Collections.singletonList(new RollbackRuleAttribute(Exception.class)));
        //设置事务隔离级别（读以提交的数据,此处可不做设置，数据库默认的隔离级别就行）
        txAttr_readonly.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        //设置事务是否为“只读”（非必须，只是声明该事务中不会进行修改数据库的操作，可减轻由事务机制造成的数据库，属压力于性能优化推荐配置）
        txAttr_readonly.setReadOnly(true);


        //事务管理规则，承载需要进行事务管理的方法名（模糊匹配），及事务的传播行为和隔离级别等属性
        NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
        //source.addTransactionalMethod("insert*",txAttr_required);  可以直接设置，推荐使用map集合或者properties文件操作存储

        
        /**
         * 创建一个map用于存储需要进行事务管理的方法名（模糊匹配）
         */
        Map<String, TransactionAttribute> map = new HashMap<>();
        //增删改的操作需要设置为REQUIRED的传播行为
        map.put("insert*",txAttr_required);
        map.put("add*",txAttr_required);
        map.put("increase*",txAttr_required);
        map.put("delete*",txAttr_required);
        map.put("remove*",txAttr_required);
        map.put("update*",txAttr_required);
        map.put("alter*",txAttr_required);
        map.put("modify*",txAttr_required);
        //查询的操作设置为REQUIRED_NOT_SUPPORT非事务传播行为，并设置为只读，减轻数据库压力
        map.put("select*",txAttr_readonly);
        map.put("get*",txAttr_readonly);

        //注入上述匹配好的map集合
        source.setNameMap(map);

        //实例化事务拦截器(整合事务管理和事务操作数据源-要操作的事务方法)
        TransactionInterceptor txAdvice = new TransactionInterceptor(transactionManager,source);
        //并将事务通知返回
        return txAdvice;
    }


    /**
     * 利用AspectJExpressionPointcut设置切面
     * @return
     */
    @Bean
    public Advisor txAdviceAdvisor(){

        //声明切入面（也就是所有切入点的逻辑集合，所有切入点形成的切面）
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        //设置切入点的路径
        pointcut.setExpression("execution(* com.kdcrm.service..*.*(..))");

        //返回aop配置：整合切面（切入点集合） 和  配置好的事务通知（也就是事务拦截操作）
        Advisor advisor = new DefaultPointcutAdvisor(pointcut,getTxAdvice());

        return advisor;
    }

}


```

![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\4、SpringBoot配置全局事务处理\89dda7fe1e35445197e3a69104aae5b9.png)

![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\4、SpringBoot配置全局事务处理\33636c793c5745dc95da3c29b0e80a9a.png)

![在这里插入图片描述](D:\Tyora\AssociatedPicturesInTheArticles\4、SpringBoot配置全局事务处理\1eec387d5b534ab9b0f4784a719db372.png)



就先到这吧！！！
————————————————
版权声明：本文为CSDN博主「一宿君」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/qq_52596258/article/details/119407315
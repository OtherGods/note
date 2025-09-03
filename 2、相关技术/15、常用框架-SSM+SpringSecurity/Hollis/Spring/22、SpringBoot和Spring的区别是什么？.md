# 典型回答

Spring是一个非常强大的企业级Java开发框架（Java的腾飞他居功至伟），提供了一系列模块来支持不同的应用需求，如依赖注入、面向切面编程、事务管理、Web应用程序开发等。而**SpringBoot的出现，主要是起到了*简化Spring应用程序的开发和部署*，特别是用于构建微服务和快速开发的应用程序。**

相比于Spring，SpringBoot主要在这几个方面来提升了我们使用Spring的效率，降低开发成本：
1. **自动配置**：Spring Boot通过`Auto-Configuration`来<font color="red" size=5>减少开发人员的配置工作</font>。我们可以**通过依赖一个starter就把一坨东西全部都依赖进来，使开发人员可以更专注于业务逻辑而不是配置**。
   [18、Springboot是如何实现自动配置的？](2、相关技术/15、常用框架-SSM+SpringSecurity/Hollis/Spring/18、Springboot是如何实现自动配置的？.md)
2. **内嵌Web服务器**：Spring Boot**内置了常见的Web服务器**（如Tomcat、Jetty），这意味着您可以轻松创建可运行的独立应用程序，而无需外部Web服务器。
   [19、SpringBoot是如何实现main方法启动Web项目的？](2、相关技术/15、常用框架-SSM+SpringSecurity/Hollis/Spring/19、SpringBoot是如何实现main方法启动Web项目的？.md)
3. **约定大于配置**：SpringBoot中有很多约定大于配置的思想的体现，通过一种约定的方式，来降低开发人员的配置工作。如他默认读取spring.factories来加载Starter、读取application.properties或application.yml文件来进行属性配置等。

# 手写Web服务器

## 1.课程目标

- 综合运用JavaSE高级特性：IO流、多线程编程、网络编程、反射、面向对象，XML解析!
- 通过手写Web服务器，了解Web请求的流程和Web服务器大致的工作原理
- 积累代码量，养成良好编码习惯

## 2.什么是Web服务器

**硬件**: 实实在在的一台机器！

![1618190934582](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618190934582.png)

**软件**: 安装在硬件服务器上的一个Web服务器软件，负责监听服务器的某一端口，接收用户发送过来的请求，并解析请求资源地址，然后对外提供服务器!

课程中所说的Web服务器，指的是Web服务器这个软件！

![1618191822723](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618191822723.png)

## 3.Java中常见的Web服务器

Web服务器是运行及发布Web应用的容器，只有将开发的Web项目放置到该容器中，才能使网络中的所有用户通过浏览器进行访问。开发Java Web应用所采用的服务器主要是与JSP/Servlet兼容的Web服务器，比较常用的有Tomcat、Resin、JBoss、WebSphere 和 WebLogic 等，下面将分别进行介绍。

### 3.1.Tomcat 服务器

目前最为流行的Tomcat服务器是Apache-Jarkarta开源项目中的一个子项目，是一个小型、轻量级的支持JSP和Servlet 技术的Web服务器，也是初学者学习开发JSP应用的首选。

apache软件基金会：[https://www.apache.org](https://www.apache.org/)

##### 3.1.1 tomcat官网： http://tomcat.apache.org/

![1618192155101](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618192155101.png)

#### 3.1.2.tomcat下载：

![1618192351303](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618192351303.png)

>tomcat服务器是使用Java编写的，所以需要本地有JDK的支持。 java -version

![1618192533024](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618192533024.png)

#### 3.1.3启动Tomcat服务器

![1618192760596](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618192760596.png)

双击startup.bat启动web服务器,打开一个命令行CMD界面:

![1618192855460](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618192855460.png)

测试Web服务器(web服务器不要关闭)：

> tomcat服务器默认监听的是8080端口,后期可以人工设置

![1618192993445](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618192993445.png)



```
http://127.0.0.1:8080/
http://localhost:8080/
```

#### 3.1.4 启动Tomcat服务器问题

```
启动完毕之后页面一闪而过，就自动关闭啦：
1.有可能是JDK环境变量有问题
2.有可能JDK的版本和下载的Tomcat的版本不匹配
```

#### 3.1.5 启动Tomcat服务器控制台乱码

![1618194009599](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618194009599.png)

![1618194064220](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618194064220.png)

![1618194125875](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618194125875.png)

![1618194162063](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618194162063.png)

#### 3.1.6 修改tomcat默认的端口

```
tomcat默认的端口是8080,可以自定义端口
```

![1618194275668](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618194275668.png)

![1618194353896](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618194353896.png)

![1618194388740](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618194388740.png)

测试端口:

![1618194432756](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618194432756.png)

#### 3.1.7 tomcat部署项目

![1618194706250](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618194706250.png)

![1618194845089](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618194845089.png)

![1618194935654](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618194935654.png)



### 3.2.其他的Web服务器

**Resin 服务器**

Resin是Caucho公司的产品，是一个非常流行的支持Servlet和JSP的服务器，速度非常快。Resin本身包含了一个支持HTML的Web服务器，这使它不仅可以显示动态内容，而且显示静态内容的能力也毫不逊色，因此许多网站都是使用Resin服务器构建。

**JBoss服务器**

JBoss是一个种遵从JavaEE规范的、开放源代码的、纯Java的EJB服务器，对于J2EE有很好的支持。JBoss采用JML API实现软件模块的集成与管理，其核心服务又是提供EJB服务器，不包含Servlet和JSP的Web容器，不过它可以和Tomcat完美结合。

**WebSphere 服务器**

WebSphere是IBM公司的产品，可进一步细分为 WebSphere Performance Pack、Cache Manager 和WebSphere Application Server等系列，其中WebSphere Application Server 是基于Java 的应用环境，可以运行于 Sun Solaris、Windows NT 等多种操作系统平台，用于建立、部署和管理Internet和Intranet Web应用程序。

**WebLogic 服务器**

WebLogic 是BEA公司的产品，可进一步细分为 WebLogic Server、WebLogic Enterprise 和 WebLogic Portal 等系列，其中 WebLogic Server 的功能特别强大。WebLogic 支持企业级的、多层次的和完全分布式的Web应用，并且服务器的配置简单、界面友好。对于那些正在寻求能够提供Java平台所拥有的一切应用服务器的用户来说，WebLogic是一个十分理想的选择。

>为什么选tomcat?
>
>轻量级，开源服务器。免费！中小型企业，甚至大型的互联网公司，都在使用tomcat！性能稳定，支持定制开发，支持集群！

![1618195367583](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618195367583.png)

## 4.xml文件概念

```
XML文件：是有一定数据格式的一种文件，一般作用：
1.作为配置文件使用
2.网络传输的一种数据文件格式，因为XML文件是跨平台的！

一般都是作为配置文件使用：不能把有些信息，写在Java源码中，编译之后没法修改！但是XML文件作为一个程序的配置文件，随时可以修改，不要硬编码！！
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<students>
   <student>
        <name id="001">张三</name>
        <age>18</age>
        <hobby>计算机</hobby>
   </student>
    <student>
        <name id="002">李四</name>
        <age>19</age>
        <hobby>计算机</hobby>
   </student>
   <student>
        <name id="003">王五</name>
        <age>20</age>
        <hobby>体育</hobby>
   </student>
</students>
```

![1618196072804](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618196072804.png)

## 5.xml文件解析

>如果需要读取XML配置文件的信息，需要掌握一个技术：XML解析技术！在Java中XML解析有很多技术可以实现，SAX解析，DOM解析，DOM4J解析！目前市场上比较流行的XML解析技术是：DOM4J解析！
>
>DOM4J 是一个开源的，进行XML解析的框架，在Java以后学习的框架中内部大部分的都是使用DOM4J 解析XML的！甚至SUN公司有些技术栈也是使用DOM4J ！

[https://dom4j.github.io](https://dom4j.github.io/)

![1618197133574](2、相关技术/14、J2EE基础-Web服务器/Tomcat/手写TomCat(石头)/《课程资料》-01/讲义/手写Web服务器笔记.assets/1618197133574.png)
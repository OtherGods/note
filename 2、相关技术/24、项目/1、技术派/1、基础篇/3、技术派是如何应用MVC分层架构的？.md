这篇文章会先讲解三层架构以及 MVC，然后再给大家讲述技术派项目的代码框架。

通过这篇文章，不仅让你掌握代码的分层结构，更重要是让你对技术派项目的整体代码结构有一个整体认知，以及它的设计理念。

# 1、三层架构
三层架构就是为了符合“高内聚，低耦合”思想，把各个功能模块划分为表示层（UI）、业务逻辑层（BLL）和数据访问层（DAL）三层架构。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307201019799.png)

1. **表示层（UI）**：位于三层构架的最上层，与用户直接接触，表示层就是实现用户的界面功能，也是系统数据的输入与输出，是为用户传达和反馈信息的。
2. **业务逻辑层（BLL）**：是针对具体的问题的操作，也可以理解成对数据层的操作，对数据业务逻辑处理。同时也是表示层与数据层的桥梁，实现三层之间的数据连接和指令传达。
3. **数据访问层（DAL）**：有时候也称为是持久层，主要功能是对原始数据(数据库或者文本文件等存放数据的形式)的操作（实现数据的增加、删除、修改、查询等）。具体为业务逻辑层或表示层提供数据服务。

在三层架构程序设计中，采用面向接口编程。各层之间采用接口相互访问，并通过对象模型的实体类（Model）作为数据传递的载体。

层是一种弱耦合结构，层与层之间的依赖是向下的，上层对下层的调用，是通过接口实现的，而真正提供服务的是下层的接口实现类。服务标准接口是相同的，而实现类是可以替换的，这样就实现了层与层间的解耦。

# 2、MVC架构

MVC全名是Model View Controller，是模型(model)－视图(view)－控制器(controller)的缩写，一种软件设计典范，用一种业务逻辑、数据、界面显示分离的方法组织代码，将业务逻辑聚集到一个部件里面，在改进和个性化定制界面及用户交互的同时，不需要重新编写业务逻辑。

1. 视图(view)： 为用户提供使用界面，与用户直接进行交互。
2. 模型(model)： 代表一个存取数据的对象或 JAVA POJO（Plain Old Java Object，简单java对象）。它也可以带有逻辑，主要用于承载数据，并对用户提交请求进行计算的模块。模型分为两类，一类称为数据承载 Bean，一类称为业务处理Bean。所谓数据承载 Bean 是指实体类（如：User类），专门为用户承载业务数据的；而业务处理 Bean 则是指Service 或 Dao 对象， 专门用于处理用户提交请求的。
3. 控制器(controller)： 用于将用户请求转发给相应的 Model 进行处理，并根据 Model 的计算结果向用户提供相应响应。它使视图与模型分离。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307201024675.png)

流程步骤：
1. 用户通过View 页面向服务端提出请求，可以是表单请求、超链接请求、AJAX 请求等；
2. 服务端 Controller 控制器接收到请求后对请求进行解析，找到相应的Model，对用户请求进行处理Model 处理；
3. 将处理结果再交给 Controller（控制器其实只是起到了承上启下的作用）；
4. 根据处理结果找到要作为向客户端发回的响应View 页面，页面经渲染后发送给客户端。

# 3、三层与MVC的区别

无论是MVC架构还是三层架构，都是一种规范，都是奔着"高内聚，低耦合"的思想来设计的。

MVC 架构主要是为了解决应用程序用户界面的样式替换问题，把视图层尽可能的和业务代码分离。

而三层架构是从整个应用程序架构的角度来分层的。当然，如果有需要的话，还可以分层。在三层架构中业务逻辑层和数据访问层要遵循面向接口编程。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307201026765.png)


# 4、技术派项目(前台)分层结构
前台相关：[前端工程结构说明](D:/IntelliJ/java_code_doc_1/paicoding/docs/%E5%89%8D%E7%AB%AF%E5%B7%A5%E7%A8%8B%E7%BB%93%E6%9E%84%E8%AF%B4%E6%98%8E.md)在技术派项目的文档中。

敲重点，这个树状图就是技术派所有的目录（我把前端的内容删掉了，大家只需关注后端），大家看技术派代码前，需要先看一下这个目录结构，让大家对整个代码框架有一个全局的认识，方便大家后续更好去看代码。
每个目录我都给了备注，一目了然。

**先看一个模块组织结构及功能：**
`这个结构是前台代码，后台管理端的代码不是用Java写的，好像没有跳转到后台的代码的接口。`
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307201101501.png)

其中模块paicoding_api和paicoding_core的区别：
1. api: 设计之初，就是给别人用的，需要轻量；
   这个模块主要是为后续的微服务做的预留，目前是将一些通用的定义放在这个模块中，现在它里面还缺少了 service的接口定义，后续更行到服务拆分的时候会将这块迁移进来；
2. core: 项目内的核心武器库，通常是我们自己项目内的最佳解决方案，推出去别人不一定认可，或者里面有些价值几个亿的解决方案，不希望给别的团队白嫖；
   这个模块主要是表示整个项目中的核心工具、通用组件，你可以理解为一些相关工具类，这个通常是项目内、团队内的通用工具库，一般来说不会跨团队共享。

**环境配置说明：**
资源配置都放在 `paicoding-web` 模块的资源路径下，通过maven的env进行环境选择切换（默认是dev环境）。
当前有四种环境：
1. resources-env/dev: 本地开发环境，也是默认环境
2. resources-env/test: 测试环境
3. resources-env/pre: 预发环境
4. resources-env/prod: 生产环境

环境切换
1. maven命令：`mvn clean install -DskipTests=true -Pprod`
2. idea切换：
   ![init_02.jpg](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307201133045.jpg)


**模块中相应配置文件说明：**
1. resources
	1. application.yml: 主配置文件入口
	2. application-config.yml: 全局的站点信息配置文件
	3. logback-spring.xml: 日志打印相关配置文件
	4. liquibase: 由liquibase进行数据库表结构管理
2. resources-env
	1. xxx/application-dal.yml: 定义数据库相关的配置信息
	2. xxx/application-image.yml: 定义上传图片的相关配置信息
	3. xxx/application-web.yml: 定义web相关的配置信息

**前台代码相应模块对应目录：**
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307201037452.png)


从 MVC 分层结构来看，分别如下：
1. 视图(view)： paicoding-ui，PC 界面
2. 模型(model)：paicoding-service，后端核心逻辑
3. 控制器(controller)：paicoding-web，界面与后端交互逻辑

从三层结构来看，分别如下：
1. 表示层（UI）：paicoding-web（将后端数据吐出来）、paicoding-ui（PC 界面）；
2. 业务逻辑层（BLL）：在 paicoding-service 里面，以com/github/paicoding/forum/service/article目录为例，service 就是业务逻辑层，对 DAO 层的封装；
3. 数据访问层（DAL）：在 paicoding-service 里面，以com/github/paicoding/forum/service/article目录为例，repository 就是 DAL 层，也称为 DAO 层，负责和 DB 的交互。

所以大家去 Debug 代码时，代码的入口就在 MVC 的 Controller 层，也就是 paicoding-web，其中 paicoding/forum/web/admin 是后台，其它的就是 PC 端的后端接口。

## 配置要求

CPU：1核

内存：2G

## 代码下载

https://github.com/xuxueli/xxl-job

### 初始化数据库

找到文件`/xxl-job/doc/db/tables_xxl_job.sql` ，然后执行，把库表建好。

### 修改配置

找到文件 `/xxl-job/xxl-job-admin/src/main/resources/application.properties`

<font color = "red">重点修改数据库部分信息</font>
```properties
### 调度中心JDBC链接
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=root_pwd
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
### 报警邮箱
spring.mail.host=smtp.qq.com
spring.mail.port=25
spring.mail.username=xxx@qq.com
spring.mail.password=xxx
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.socketFactory.class=javax.net.ssl.SSLSocketFactory
### 调度中心通讯TOKEN [选填]：非空时启用；
xxl.job.accessToken=
### 调度中心国际化配置 [必填]： 默认为 "zh_CN"/中文简体, 可选范围为 "zh_CN"/中文简体, "zh_TC"/中文繁体 and "en"/英文；
xxl.job.i18n=zh_CN
## 调度线程池最大线程配置【必填】
xxl.job.triggerpool.fast.max=200
xxl.job.triggerpool.slow.max=100
### 调度中心日志表数据保存天数 [必填]：过期日志自动清理；限制大于等于7时生效，否则, 如-1，关闭自动清理功能；
xxl.job.logretentiondays=30
```

然后就可以通过XxlJobAdminApplication启动了（在服务器上，可以通过java -jar xxl-job.jar 启动），启动后访问：http://localhost:8080/xxl-job-admin

账号密码登录：admin/123456
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409011529633.png)



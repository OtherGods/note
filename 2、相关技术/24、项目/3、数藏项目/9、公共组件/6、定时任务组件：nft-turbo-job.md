

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503042352746.png)

我们的项目在多个微服务中都需要进行定时任务的处理，我们把定时任务相关的内容封装到定时任务组件中。主要是封装了 xxl-job
```xml
<?xml version="1.0" encoding="UTF-8"?>  
<project xmlns="http://maven.apache.org/POM/4.0.0"  
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">  
    <modelVersion>4.0.0</modelVersion>  
    <parent>  
        <groupId>cn.hollis</groupId>  
        <artifactId>nft-turbo-common</artifactId>  
        <version>1.0.0-SNAPSHOT</version>  
    </parent>  
  
    <groupId>cn.hollis</groupId>  
    <artifactId>nft-turbo-job</artifactId>  
    <version>1.0.0-SNAPSHOT</version>  
    <description>任务组件</description>  
  
    <properties>  
        <maven.compiler.source>21</maven.compiler.source>  
        <maven.compiler.target>21</maven.compiler.target>  
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    </properties>  
  
    <dependencies>  
        <dependency>  
            <groupId>com.xuxueli</groupId>  
            <artifactId>xxl-job-core</artifactId>  
            <version>2.4.0</version>  
        </dependency>  
    </dependencies>  
  
</project>
```

同时，定义一个 job.yml，把 xxl-job 的相关配置配置好：
```yml
spring:  
  xxl:  
    job:  
      enabled: true  
      adminAddresses: http://114.xx.xx.45:23333/xxl-job-admin/  
      appName: xxl-job-executor  
      accessToken: default_token
```

同时，为了方便使用，我们自定义了 bean——xxlJobExecutor：
```java
package cn.hollis.nft.turbo.job.config;  
  
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;  
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;  
import org.springframework.boot.context.properties.EnableConfigurationProperties;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
  
/**  
 * @author Hollis */@Configuration  
@EnableConfigurationProperties(XxlJobProperties.class)  
public class XxlJobConfiguration {  
      
    private static final Logger logger = LoggerFactory.getLogger(XxlJobConfiguration.class);  
      
    @Autowired  
    private XxlJobProperties properties;  
      
    @Bean  
    @ConditionalOnMissingBean    @ConditionalOnProperty(prefix = XxlJobProperties.PREFIX, value = "enabled", havingValue = "true")  
    public XxlJobSpringExecutor xxlJobExecutor() {  
        logger.info(">>>>>>>>>>> xxl-job config init.");  
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();  
        xxlJobSpringExecutor.setAdminAddresses(properties.getAdminAddresses());  
        xxlJobSpringExecutor.setAppname(properties.getAppName());  
        xxlJobSpringExecutor.setIp(properties.getIp());  
        xxlJobSpringExecutor.setPort(properties.getPort());  
        xxlJobSpringExecutor.setAccessToken(properties.getAccessToken());  
        xxlJobSpringExecutor.setLogPath(properties.getLogPath());  
        xxlJobSpringExecutor.setLogRetentionDays(properties.getLogRetentionDays());  
        return xxlJobSpringExecutor;  
    }  
}
```

```java
package cn.hollis.nft.turbo.job.config;  
  
import org.springframework.boot.context.properties.ConfigurationProperties;  
  
/**  
 * @author Hollis */@ConfigurationProperties(prefix = XxlJobProperties.PREFIX)  
public class XxlJobProperties {  
      
    public static final String PREFIX = "spring.xxl.job";  
      
    private boolean enabled;  
      
    private String adminAddresses;  
      
    private String accessToken;  
      
    private String appName;  
      
    private String ip;  
      
    private int port;  
      
    private String logPath;  
      
    private int logRetentionDays = 30;  
      
    public boolean isEnabled() {  
        return enabled;  
    }  
      
    public void setEnabled(boolean enabled) {  
        this.enabled = enabled;  
    }  
      
    public String getAdminAddresses() {  
        return adminAddresses;  
    }  
      
    public void setAdminAddresses(String adminAddresses) {  
        this.adminAddresses = adminAddresses;  
    }  
      
    public String getAccessToken() {  
        return accessToken;  
    }  
      
    public void setAccessToken(String accessToken) {  
        this.accessToken = accessToken;  
    }  
      
    public String getAppName() {  
        return appName;  
    }  
      
    public void setAppName(String appName) {  
        this.appName = appName;  
    }  
      
      
    public String getIp() {  
        return ip;  
    }  
      
    public void setIp(String ip) {  
        this.ip = ip;  
    }  
      
    public int getPort() {  
        return port;  
    }  
      
    public void setPort(int port) {  
        this.port = port;  
    }  
      
    public String getLogPath() {  
        return logPath;  
    }  
      
    public void setLogPath(String logPath) {  
        this.logPath = logPath;  
    }  
      
    public int getLogRetentionDays() {  
        return logRetentionDays;  
    }  
      
    public void setLogRetentionDays(int logRetentionDays) {  
        this.logRetentionDays = logRetentionDays;  
    }  
}
```

并且新建org.springframework.boot.autoconfigure.AutoConfiguration.imports，内容如下：
```java
cn.hollis.nft.turbo.job.config.XxlJobConfiguration
```


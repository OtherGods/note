![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503091734903.png)

我们的项目需要在注册用户的时候进行发送短信，我们把短信相关的内容封装到短信组件中。主要是封装了阿里云的短信api
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
    <artifactId>nft-turbo-sms</artifactId>  
    <version>1.0.0-SNAPSHOT</version>  
  
    <properties>  
        <maven.compiler.source>21</maven.compiler.source>  
        <maven.compiler.target>21</maven.compiler.target>  
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    </properties>  

    <dependencies>  
        <dependency>  
            <groupId>cn.hollis</groupId>  
            <artifactId>nft-turbo-base</artifactId>  
        </dependency>  
        <dependency>  
            <groupId>cn.hollis</groupId>  
            <artifactId>nft-turbo-lock</artifactId>  
        </dependency>  
    </dependencies>  
</project>
```

短信配置信息放在nacos上，通过spring 自动注入
```java
package cn.hollis.nft.turbo.sms.config;  
  
import org.springframework.boot.context.properties.ConfigurationProperties;  
  
@ConfigurationProperties(prefix = SmsProperties.PREFIX)  
public class SmsProperties {  
    public static final String PREFIX = "spring.sms";  
      
    private String host;  
      
    private String path;  
      
    private String appcode;  
      
    private String smsSignId;  
      
    private String templateId;  
      
    private boolean enabled;  
      
    public String getHost() {  
        return host;  
    }  
      
    public void setHost(String host) {  
        this.host = host;  
    }  
      
    public String getPath() {  
        return path;  
    }  
      
    public void setPath(String path) {  
        this.path = path;  
    }  
      
    public String getAppcode() {  
        return appcode;  
    }  
      
    public void setAppcode(String appcode) {  
        this.appcode = appcode;  
    }  
      
    public String getSmsSignId() {  
        return smsSignId;  
    }  
      
    public void setSmsSignId(String smsSignId) {  
        this.smsSignId = smsSignId;  
    }  
      
    public String getTemplateId() {  
        return templateId;  
    }  
      
    public void setTemplateId(String templateId) {  
        this.templateId = templateId;  
    }  
      
    public boolean isEnabled() {  
        return enabled;  
    }  
      
    public void setEnabled(boolean enabled) {  
        this.enabled = enabled;  
    }  
}
```

然后构造成一个smsService的bean
```java
package cn.hollis.nft.turbo.sms.config;  
  
import cn.hollis.nft.turbo.sms.SmsService;  
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;  
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;  
import org.springframework.boot.context.properties.EnableConfigurationProperties;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
  
/**  
 * @author Hollis */@Configuration  
@EnableConfigurationProperties(SmsProperties.class)  
public class SmsConfiguration {  
      
    @Autowired  
    private SmsProperties properties;  
      
    @Bean  
    @ConditionalOnMissingBean    @ConditionalOnProperty(prefix = SmsProperties.PREFIX, value = "enabled", havingValue = "true")  
    public SmsService smsService() {  
        SmsService smsService = new SmsService();  
        smsService.setHost(properties.getHost());  
        smsService.setPath(properties.getPath());  
        smsService.setAppcode(properties.getAppcode());  
        smsService.setSmsSignId(properties.getSmsSignId());  
        smsService.setTemplateId(properties.getTemplateId());  
        return smsService;  
    }  
      
}
```

```java
package cn.hollis.nft.turbo.sms;  
  
import cn.hollis.nft.turbo.base.utils.HttpUtils;  
import cn.hollis.nft.turbo.base.utils.RestClientUtils;  
import cn.hollis.nft.turbo.lock.DistributeLock;  
import cn.hollis.nft.turbo.sms.response.SmsSendResponse;  
import com.alibaba.fastjson2.JSON;  
import com.google.common.collect.Maps;  
import lombok.Setter;  
import lombok.extern.slf4j.Slf4j;  
import org.apache.commons.lang3.StringUtils;  
import org.apache.http.HttpResponse;  
import org.apache.http.util.EntityUtils;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
import org.springframework.http.ResponseEntity;  
  
import java.util.Map;  
  
import static cn.hollis.nft.turbo.base.response.ResponseCode.SYSTEM_ERROR;  
  
@Slf4j  
@Setter  
public class SmsService {  
      
    private static Logger logger = LoggerFactory.getLogger(SmsService.class);  
      
    private String host;  
      
    private String path;  
      
    private String appcode;  
      
    private String smsSignId;  
      
    private String templateId;  
      
    @DistributeLock(scene = "SEND_SMS", keyExpression = "#phoneNumber")  
    public SmsSendResponse sendMsg(String phoneNumber, String code) {  
          
        SmsSendResponse smsSendResponse = new SmsSendResponse();  
          
        String method = "POST";  
        Map<String, String> headers = Maps.newHashMapWithExpectedSize(1);  
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105  
        headers.put("Authorization", "APPCODE " + appcode);  
        Map<String, String> querys = Maps.newHashMapWithExpectedSize(4);  
        querys.put("mobile", phoneNumber);  
        querys.put("param", "**code**:" + code + ",**minute**:5");  
          
        //smsSignId（短信前缀）和templateId（短信模板），可登录国阳云控制台自助申请。参考文档：http://help.guoyangyun.com/Problem/Qm.html  
        querys.put("smsSignId", smsSignId);  
        querys.put("templateId", templateId);  
        Map<String, String> bodys = Maps.newHashMapWithExpectedSize(2);  
          
        try {  
            ResponseEntity response = RestClientUtils.doPost(host, path, headers, querys, bodys);  
            if (response.getStatusCode().is2xxSuccessful()) {  
                smsSendResponse.setSuccess(true);  
            }  
        } catch (Exception e) {  
            logger.error("sendMsg error", e);  
            smsSendResponse.setSuccess(false);  
            smsSendResponse.setResponseCode(SYSTEM_ERROR.name());  
            smsSendResponse.setResponseMessage(StringUtils.substring(e.toString(), 0, 1000));  
        }  
        return smsSendResponse;  
    }  
}
```

并且新建org.springframework.boot.autoconfigure.AutoConfiguration.imports，内容如下：

```
cn.hollis.nft.turbo.sms.config.SmsConfiguration
```


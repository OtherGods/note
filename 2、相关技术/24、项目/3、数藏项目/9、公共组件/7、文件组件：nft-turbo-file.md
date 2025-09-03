![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202503052209554.png)

我们的项目需要在用户模块管理的时候进行头像修改，我们把上传文件相关的内容封装到文件上传组件中。主要是封装了阿里云的oss
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
    <artifactId>nft-turbo-file</artifactId>  
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
            <exclusions>  
                <exclusion>  
                    <groupId>org.apache.httpcomponents</groupId>  
                    <artifactId>httpclient</artifactId>  
                </exclusion>  
            </exclusions>  
        </dependency>  
  
        <!--  OSS  -->  
  
        <dependency>  
            <groupId>com.aliyun.oss</groupId>  
            <artifactId>aliyun-sdk-oss</artifactId>  
            <version>3.15.1</version>  
        </dependency>  
        <!--  OSS:如果使用的是Java 9及以上的版本，则需要添加JAXB相关依赖   -->  
        <dependency>  
            <groupId>javax.xml.bind</groupId>  
            <artifactId>jaxb-api</artifactId>  
            <version>2.3.1</version>  
        </dependency>  
        <!-- no more than 2.3.3-->  
        <dependency>  
            <groupId>org.glassfish.jaxb</groupId>  
            <artifactId>jaxb-runtime</artifactId>  
            <version>2.3.3</version>  
        </dependency>  
  
    </dependencies>  
  
</project>
```

oss配置信息放在nacos上，通过spring 自动注入
```java
package cn.hollis.nft.turbo.file.config;  
  
import org.springframework.boot.context.properties.ConfigurationProperties;  
  
@ConfigurationProperties(prefix = OssProperties.PREFIX)  
public class OssProperties {  
    public static final String PREFIX = "spring.oss";  
      
    private String bucket;  
      
    private String endPoint;  
      
    private String accessKey;  
      
    private String accessSecret;  
      
    private boolean enabled;  
      
    public String getBucket() {  
        return bucket;  
    }  
      
    public void setBucket(String bucket) {  
        this.bucket = bucket;  
    }  
      
    public String getEndPoint() {  
        return endPoint;  
    }  
      
    public void setEndPoint(String endPoint) {  
        this.endPoint = endPoint;  
    }  
      
    public String getAccessKey() {  
        return accessKey;  
    }  
      
    public void setAccessKey(String accessKey) {  
        this.accessKey = accessKey;  
    }  
      
    public String getAccessSecret() {  
        return accessSecret;  
    }  
      
    public void setAccessSecret(String accessSecret) {  
        this.accessSecret = accessSecret;  
    }  
      
    public boolean isEnabled() {  
        return enabled;  
    }  
      
    public void setEnabled(boolean enabled) {  
        this.enabled = enabled;  
    }  
}
```

然后构造成一个ossService的bean
```java
package cn.hollis.nft.turbo.file.config;  
  
import cn.hollis.nft.turbo.file.OssService;  
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;  
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;  
import org.springframework.boot.context.properties.EnableConfigurationProperties;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
  
/**  
 * @author Hollis */@Configuration  
@EnableConfigurationProperties(OssProperties.class)  
public class OssConfiguration {  
      
      
    @Autowired  
    private OssProperties properties;  
      
    @Bean  
    @ConditionalOnMissingBean    @ConditionalOnProperty(prefix = OssProperties.PREFIX, value = "enabled", havingValue = "true")  
    public OssService ossService() {  
        OssService ossService=new OssService();  
        ossService.setBucket(properties.getBucket());  
        ossService.setEndPoint(properties.getEndPoint());  
        ossService.setAccessKey(properties.getAccessKey());  
        ossService.setAccessSecret(properties.getAccessSecret());  
        return ossService;  
    }  
      
}
```

```java
package cn.hollis.nft.turbo.file;  
  
import java.io.InputStream;  
  
import com.aliyun.oss.OSS;  
import com.aliyun.oss.OSSClientBuilder;  
import com.aliyun.oss.common.auth.CredentialsProvider;  
import com.aliyun.oss.common.auth.DefaultCredentialProvider;  
import com.aliyun.oss.model.PutObjectRequest;  
import com.aliyun.oss.model.PutObjectResult;  
import lombok.Setter;  
import lombok.extern.slf4j.Slf4j;  
import org.apache.commons.lang3.StringUtils;  
import org.springframework.stereotype.Service;  
  
@Slf4j  
@Setter  
public class OssService {  
      
    private String bucket;  
      
    private String endPoint;  
      
    private String accessKey;  
      
    private String accessSecret;  
      
    public boolean upload(String path, InputStream fileStream) {  
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。  
        String endpoint = endPoint;  
        // 从环境变量中获取RAM用户的访问密钥（AccessKey ID和AccessKey Secret）。  
        String accessKeyId = accessKey;  
        String accessKeySecret = accessSecret;  
        // 使用代码嵌入的RAM用户的访问密钥配置访问凭证。  
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);  
          
        // 填写Bucket名称，例如examplebucket。  
        String bucketName = bucket;  
        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。  
        String objectName = path;  
          
        // 创建OSSClient实例。  
        OSS ossClient = new OSSClientBuilder().build(endpoint, credentialsProvider);  
        boolean uploadRes = false;  
        try {  
              
            // 创建PutObjectRequest对象。  
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, fileStream);  
              
            // 上传字符串。  
            PutObjectResult result = ossClient.putObject(putObjectRequest);  
            if (StringUtils.isNotBlank(result.getRequestId())) {  
                uploadRes = true;  
            }  
        } catch (Exception e) {  
            log.error("OssUtil upload error,path=" + path, e);  
        } finally {  
            if (ossClient != null) {  
                ossClient.shutdown();  
            }  
        }  
        return uploadRes;  
    }  
      
}
```

并且新建org.springframework.boot.autoconfigure.AutoConfiguration.imports，内容如下：

```
cn.hollis.nft.turbo.file.config.OssConfiguration
```


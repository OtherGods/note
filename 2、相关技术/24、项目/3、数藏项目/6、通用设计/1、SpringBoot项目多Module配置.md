在 Spring Boot 项目中，配置多个模块可以帮助我们将应用分解成多个逻辑部分，每个部分都是一个独立的模块，这样可以提高代码的可管理性和可重用性。

首先，我们需要以如下方式组织我们的项目结构
```shell
.  
├── nft-turbo-common  
│   ├── pom.xml  
│   ├── src  
│   │   └── main  
│   │       ├── java  
│   │       └── resources  
├── nft-turbo-business  
│   ├── pom.xml  
│   ├── src  
│   │   └── main  
│   │       ├── java  
│   │       └── resources  
├── nft-turbo-auth  
│   └── pom.xml  
├── nft-turbo-gateway  
│   ├── pom.xml  
│   ├── src  
│   │   ├── main  
│   │   │   └── resources  
│   │   │       └── application.yml  
├── pom.xml
```

这里面有4个 module，分别是nft-turbo-gateway、nft-turbo-auth、nft-turbo-business 以及nft-turbo-common。每一个 module 中都需要有一个自己的 pom 文件，并且在整个项目中还有一个总的 pom 文件。

父 pom 中需要配置如下：
```xml
<?xml version="1.0" encoding="UTF-8"?>  
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">  
    <modelVersion>4.0.0</modelVersion>  
  
    <groupId>cn.hollis</groupId>  
    <artifactId>NFTurbo</artifactId>  
    <version>1.0.0-SNAPSHOT</version>  
    <name>NFTurbo</name>  
    <packaging>pom</packaging>  
    <description>A NFT Turbo</description>  
  
    <properties>  
        ...  
    </properties>  
  
    <parent>  
        <groupId>org.springframework.boot</groupId>  
        <artifactId>spring-boot-starter-parent</artifactId>  
        <version>3.2.2</version>  
        <relativePath/>  
    </parent>  
  
    <modules>  
        <module>nft-turbo-common</module>  
        <module>nft-turbo-auth</module>  
        <module>nft-turbo-gateway</module>  
        <module>nft-turbo-business</module>  
    </modules>  
  
    <dependencyManagement>  
        <dependencies>  
            <dependency>  
                <groupId>org.springframework.cloud</groupId>  
                <artifactId>spring-cloud-dependencies</artifactId>  
                <version>${spring-cloud.version}</version>  
                <type>pom</type>  
                <scope>import</scope>  
            </dependency>  
  
            <dependency>  
                <groupId>com.alibaba.cloud</groupId>  
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>  
                <version>${spring-cloud-alibaba.version}</version>  
                <type>pom</type>  
                <scope>import</scope>  
            </dependency>  
  
            <dependency>  
                <groupId>org.springframework</groupId>  
                <artifactId>spring-web</artifactId>  
                <version>6.1.3</version>  
            </dependency>  
  
            <dependency>  
                <groupId>cn.hollis</groupId>  
                <artifactId>nft-turbo-auth</artifactId>  
                <version>${project.version}</version>  
            </dependency>  
  
            <dependency>  
                <groupId>cn.hollis</groupId>  
                <artifactId>nft-turbo-gateway</artifactId>  
                <version>${project.version}</version>  
            </dependency>  
  
            <dependency>  
                <groupId>cn.hollis</groupId>  
                <artifactId>nft-turbo-common</artifactId>  
                <version>${project.version}</version>  
            </dependency>  
  
            <dependency>  
                <groupId>cn.hollis</groupId>  
                <artifactId>nft-turbo-business</artifactId>  
                <version>${project.version}</version>  
            </dependency>  
  
        </dependencies>  
    </dependencyManagement>  
  
    <build>  
        ...  
    </build>  
</project>
```

通过`<modules></modules>`标签来指定一共都包含哪些 module：
```xml
<modules>  
    <module>nft-turbo-common</module>  
    <module>nft-turbo-auth</module>  
    <module>nft-turbo-gateway</module>  
    <module>nft-turbo-business</module>  
</modules>
```

接下来，就在父 pom 的同级目录创建对应的 module的文件夹。并在 module 的目录中创建 pom 文件，如nft-turbo-business的 pom 内容如下：
```xml
<?xml version="1.0" encoding="UTF-8"?>  
<project xmlns="http://maven.apache.org/POM/4.0.0"  
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">  
    <modelVersion>4.0.0</modelVersion>  
  
    <parent>  
        <groupId>cn.hollis</groupId>  
        <artifactId>NFTurbo</artifactId>  
        <version>1.0.0-SNAPSHOT</version>  
    </parent>  
  
    <groupId>cn.hollis</groupId>  
    <artifactId>nft-turbo-gateway</artifactId>  
    <version>1.0.0-SNAPSHOT</version>  
  
    <properties>  
        <application.name>nfturbo-gateway</application.name>  
        <maven.compiler.source>21</maven.compiler.source>  
        <maven.compiler.target>21</maven.compiler.target>  
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  
    </properties>  
  
    <dependencies>  
        <dependency>  
            <groupId>org.springframework.boot</groupId>  
            <artifactId>spring-boot-starter</artifactId>  
        </dependency>  
  
        <!--  限流组件  -->  
        <dependency>  
            <groupId>cn.hollis</groupId>  
            <artifactId>nft-turbo-limiter</artifactId>  
            <exclusions>  
                <!--  排除spring-web，gateway应用中不能包含spring mvc组件-->  
                <exclusion>  
                    <groupId> org.springframework</groupId>  
                    <artifactId>spring-web</artifactId>  
                </exclusion>  
                <exclusion>  
                    <groupId> org.springframework</groupId>  
                    <artifactId>spring-webmvc</artifactId>  
                </exclusion>  
            </exclusions>  
        </dependency>  
  
        <dependency>  
            <groupId>cn.hollis</groupId>  
            <artifactId>nft-turbo-config</artifactId>  
        </dependency>  
  
        <!-- Sa-Token 权限认证 -->  
        <dependency>  
            <groupId>cn.dev33</groupId>  
            <artifactId>sa-token-reactor-spring-boot3-starter</artifactId>  
            <version>1.37.0</version>  
            <exclusions>  
                <exclusion>  
                    <groupId>org.springframework.boot</groupId>  
                    <artifactId>spring-boot-starter-web</artifactId>  
                </exclusion>  
            </exclusions>  
        </dependency>  
  
        <!-- Sa-Token 整合 Redis （使用 jackson 序列化方式） -->  
        <dependency>  
            <groupId>cn.dev33</groupId>  
            <artifactId>sa-token-redis-jackson</artifactId>  
            <version>1.37.0</version>  
        </dependency>  
        <dependency>  
            <groupId>org.apache.commons</groupId>  
            <artifactId>commons-pool2</artifactId>  
        </dependency>  
  
        <!--Spring Cloud Loadbalancer-->  
        <dependency>  
            <groupId>org.springframework.cloud</groupId>  
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>  
        </dependency>  
  
        <!--Spring Cloud Gateway-->  
        <dependency>  
            <groupId>org.springframework.cloud</groupId>  
            <artifactId>spring-cloud-starter-gateway</artifactId>  
        </dependency>  
  
        <!--    sensitive    -->  
        <dependency>  
            <groupId>com.github.houbb</groupId>  
            <artifactId>sensitive-logback</artifactId>  
            <version>1.7.0</version>  
        </dependency>  
  
    </dependencies>  
  
    <build>  
        <plugins>  
            <plugin>  
                <groupId>org.springframework.boot</groupId>  
                <artifactId>spring-boot-maven-plugin</artifactId>  
                <configuration>  
                    <!--                    <skip>true</skip>-->  
                    <mainClass>cn.hollis.nft.turbo.gateway.NfTurboGatewayApplication</mainClass>  
                </configuration>  
  
                <executions>  
                    <execution>  
                        <phase>package</phase>  
                        <goals>  
                            <goal>repackage</goal>  
                        </goals>  
                    </execution>  
                </executions>  
            </plugin>  
  
        </plugins>  
    </build>  
  
</project>
```

这里用<parent></parent>指定一下父包即可：

```xml
<parent>  
    <groupId>cn.hollis</groupId>  
    <artifactId>NFTurbo</artifactId>  
    <version>1.0.0-SNAPSHOT</version>  
</parent>
```

其他的子 module 同样配置即可。


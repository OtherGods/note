


Spring Boot的Maven插件（Spring Boot Maven plugin）能够以Maven的方式为应用提供Spring Boot的支持，即为Spring Boot应用提供了执行Maven操作的可能。
Spring Boot Maven plugin能够将Spring Boot应用打包为可执行的jar或war文件，然后以通常的方式运行Spring Boot应用。

Spring Boot Maven plugin的最新版本为2017.6.8发布的1.5.4.RELEASE，要求Java 8, Maven 3.2及以后。

# 1、Spring Boot Maven plugin的5个Goals

spring-boot:repackage，默认goal。在mvn package之后，再次打包可执行的jar/war，同时保留mvn package生成的jar/war为.origin
spring-boot:run，运行Spring Boot应用
spring-boot:start，在mvn integration-test阶段，进行Spring Boot应用生命周期的管理
spring-boot:stop，在mvn integration-test阶段，进行Spring Boot应用生命周期的管理
spring-boot:build-info，生成Actuator使用的构建信息文件build-info.properties

# 2、配置pom.xml文件
```xml
<build>
	<plugins>
		<plugin>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-maven-plugin</artifactId>
                        <version>1.5.4.RELEASE</version>
		</plugin>
	</plugins>
</build>
```

# 3、mvn package spring-boot:repackage说明

Spring Boot Maven plugin的最主要goal就是repackage，其在Maven的package生命周期阶段，能够将mvn package生成的软件包，再次打包为可执行的软件包，并将mvn package生成的软件包重命名为*.original。

基于上述配置，对一个生成Jar软件包的项目执行如下命令。

`mvn package spring-boot:repackage`
可以看到生成的两个jar文件，一个是*.jar，另一个是*.jar.original。

在执行上述命令的过程中，Maven首先在package阶段打包生成*.jar文件；然后执行spring-boot:repackage重新打包，查找Manifest文件中配置的Main-Class属性，如下所示：

```txt
Manifest-Version: 1.0
Implementation-Title: gs-consuming-rest
Implementation-Version: 0.1.0
Archiver-Version: Plexus Archiver
Built-By: sam
Implementation-Vendor-Id: org.springframework
Spring-Boot-Version: 1.5.3.RELEASE
Implementation-Vendor: Pivotal Software, Inc.
Main-Class: org.springframework.boot.loader.JarLauncher
Start-Class: com.ericsson.ramltest.MyApplication
Spring-Boot-Classes: BOOT-INF/classes/
Spring-Boot-Lib: BOOT-INF/lib/
Created-By: Apache Maven 3.5.0
Build-Jdk: 1.8.0_131
```

注意，其中的Main-Class属性值为org.springframework.boot.loader.JarLauncher；

Start-Class属性值为com.ericsson.ramltest.MyApplication。

其中com.ericsson.ramltest.MyApplication类中定义了main()方法，是程序的入口。

通常，Spring Boot Maven plugin会在打包过程中自动为Manifest文件设置Main-Class属性，事实上该属性究竟作用几何，还可以受Spring Boot Maven plugin的配置属性layout控制的，示例如下。

```xml
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
      <version>1.5.4.RELEASE</version>
      <configuration>
        <mainClass>${start-class}</mainClass>
        <layout>ZIP</layout>
      </configuration>
      <executions>
        <execution>
          <goals>
            <goal>repackage</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
```

注意，这里的layout属性值为ZIP。

layout属性的值可以如下：

JAR，即通常的可执行jar
Main-Class: org.springframework.boot.loader.JarLauncher

WAR，即通常的可执行war，需要的servlet容器依赖位于WEB-INF/lib-provided
Main-Class: org.springframework.boot.loader.warLauncher

ZIP，即DIR，类似于JAR
Main-Class: org.springframework.boot.loader.PropertiesLauncher

MODULE，将所有的依赖库打包（scope为provided的除外），但是不打包Spring Boot的任何Launcher。
NONE，将所有的依赖库打包，但是不打包Spring Boot的任何Launcher。


# 4、integration-test阶段中的Spring Boot Maven plugin的start/stop
```xml
<properties>
  <it.skip>false</it.skip>
</properties>
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-failsafe-plugin</artifactId>
      <configuration>
        <skip>${it.skip}</skip>
      </configuration>
    </plugin>
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
      <version>1.5.4.RELEASE</version>
      <executions>
        <execution>
          <id>pre-integration-test</id>
          <goals>
            <goal>start</goal>
          </goals>
          <configuration>
            <skip>${it.skip}</skip>
          </configuration>
        </execution>
        <execution>
          <id>post-integration-test</id>
          <goals>
            <goal>stop</goal>
          </goals>
          <configuration>
            <skip>${it.skip}</skip>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

注意，it.skip变量用作是否跳过integration-test的标志位。

maven-failsafe-plugin用作integration-test的主要执行目标。

spring-boot-maven-plugin用以为integration-test提供支持。

执行integration-test的Maven命令如下：

`mvn verify`

或者

`mvn verify -Dit.skip=false`


参考：
1. [http://docs.spring.io/spring-boot/docs/1.5.4.RELEASE/maven-plugin/](http://docs.spring.io/spring-boot/docs/1.5.4.RELEASE/maven-plugin/)
2. [https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html)



转载自：[https://blog.csdn.net/taiyangdao/article/details/75303181](https://blog.csdn.net/taiyangdao/article/details/75303181)






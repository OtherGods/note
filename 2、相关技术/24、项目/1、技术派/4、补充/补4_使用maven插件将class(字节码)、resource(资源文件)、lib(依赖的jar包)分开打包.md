

# 1、在pom文件中对各个插件进行配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.1.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <groupId>com.skd</groupId>
    <artifactId>client</artifactId>
    <version>0.0.1</version>
    <name>client</name>
    <description>client for file monitor</description>

    <properties>
        <encoding>UTF-8</encoding>
        <maven-compiler-plugin-version>3.8.0</maven-compiler-plugin-version>
        <maven-jar-plugin-version>3.1.0</maven-jar-plugin-version>
        <maven-source-plugin-version>3.0.1</maven-source-plugin-version>
        <maven-assembly-plugin-version>3.1.0</maven-assembly-plugin-version>
        <maven-dependency-plugin-version>3.1.0</maven-dependency-plugin-version>
        <maven-resources-plugin-version>3.1.0</maven-resources-plugin-version>
    </properties>

    <dependencies>
        <!--spring boot-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <!--test-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!--log4j2-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-log4j2</artifactId>
        </dependency>
        <!--http client-->
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>4.5.3</version>
        </dependency>

        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpmime</artifactId>
            <version>4.5.3</version>
        </dependency>

        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.1</version>
        </dependency>

        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.4</version>
        </dependency>
        <!--json-->
        <dependency>
            <groupId>net.sf.json-lib</groupId>
            <artifactId>json-lib</artifactId>
            <version>2.2.2</version>
            <classifier>jdk15</classifier>
        </dependency>
        <!-- lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <!--configuration-->
        <!-- 通过资源文件注入属性配置 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <!-- 生成的项目jar包的名字-->
        <finalName>client</finalName>
        <!--源代码路径-->
        <sourceDirectory>src/main/java</sourceDirectory>

        <!--maven-resources-plugin 插件打包resource文件时会参考此节点的配置-->
        <resources>
            <!--这两个resource节点负责把配置文件拷贝到classes目录下，保证运行时可以找到配置文件-->
            <resource>
                <!--开启filter功能，将路径中的属性名替换为对应的属性值-->
                <filtering>true</filtering>
                <directory>src/main/resources</directory>
                <includes>
                    <include>**/application*.yml</include>
                    <include>**/application*.yaml</include>
                    <include>**/application*.properties</include>
                </includes>
            </resource>

            <resource>
                <directory>src/main/resources</directory>
                <excludes>
                    <exclude>**/application*.yml</exclude>
                    <exclude>**/application*.yaml</exclude>
                    <exclude>**/application*.properties</exclude>
                </excludes>
            </resource>

            <!--compile 和 package 时都会使用resource节点的配置-->
            <!--resource 节点可通过配置将制定目录的文件在打包后拷贝到制定目录-->
            <!--如果只有这一个resource节点，或者如果把这个resource节点配置在最前面，则会直接把配置文件打包到conf目录classes目录下不会有配置文件，导致IDEA运行时找不到配置文件-->
            <resource>
                <directory>src/main/resources</directory>
                <targetPath>${project.build.directory}/conf</targetPath>
            </resource>
        </resources>

        <plugins>
            <!--编译插件-->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler-plugin-version}</version>
                <configuration>
                    <encoding>${encoding}</encoding>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>

            <!--将项目的源代码的class文件打包到一个jar包-->
            <!--jar包默认在target目录下-->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>${maven-jar-plugin-version}</version>
                <configuration>
                    <archive>
                        <!-- 生成的jar中，不要包含pom.xml和pom.properties这两个文件 -->
                        <addMavenDescriptor>false</addMavenDescriptor>
                        <manifest>
                            <!-- 是否要把第三方jar放到manifest的classpath中 -->
                            <addClasspath>true</addClasspath>
                            <!-- 生成的manifest中classpath的前缀，因为要把第三方jar放到lib目录下，所以classpath的前缀是lib/ -->
                            <classpathPrefix>lib/</classpathPrefix>
                            <!-- 应用的main class -->
                            <mainClass>com.skd.client.ClientApplication</mainClass>
                        </manifest>
                        <!--将资源文件目录添加到classpath中，打包后运行项目时则会在该目录下加载配置文件-->
                        <manifestEntries>
                            <Class-Path>conf/</Class-Path>
                        </manifestEntries>
                    </archive>
                    <!--项目打包为jar包时排除这些文件，如果将配置文件打到jar包，则会优先读取jar包中的配置文件，不会读取conf目录下的配置文件-->
                    <!--注意这玩意从编译结果目录开始算目录结构-->
                    <excludes>
                        <exclude>/*.yaml</exclude>
                        <exclude>/*.yml</exclude>
                        <exclude>/*.xml</exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- 用于拷贝maven依赖的plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>${maven-dependency-plugin-version}</version>
                <executions>
                    <execution>
                        <id>copy-dependencies</id>
                        <phase>package</phase>
                        <goals>
                            <goal>copy-dependencies</goal>
                        </goals>
                        <configuration>
                            <!-- 把依赖的所有maven jar包拷贝到lib目录中 -->
                            <outputDirectory>${project.build.directory}/lib</outputDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <!-- 用于拷贝resource的plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <version>${maven-resources-plugin-version}</version>
            </plugin>

        </plugins>

    </build>

</project>
```

# 2. 打包后的 target 目录结构

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406122333337.png)
conf：资源文件目录
lib：依赖的jar包目录
demo-findName：将class文件打包生成的jar包

# 3、运行项目

把上述三个目录文件放在同一级目录下，

执行 java -jar 生成的jar包名字

即可启动项目
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202406122334279.png)



转载自：[使用 Maven 插件将 class（字节码文件），resource（资源文件），lib（依赖的jar包）分开打包](https://www.cnblogs.com/virgosnail/p/10139096.html)




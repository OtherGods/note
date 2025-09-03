在 Spring Boot 应用中接入 Lombok 可以简化 Java 代码，通过自动生成样板代码（如 getter/setter、toString、equals 和 hashCode 方法）。以下是接入 Lombok 的步骤：

### 1. 添加 Lombok 依赖

在你的 `pom.xml` 文件中添加 Lombok 依赖：
```xml
<dependency>  
    <groupId>org.projectlombok</groupId>  
    <artifactId>lombok</artifactId>  
    <version>1.18.30</version>  
    <scope>provided</scope>  
</dependency>
```

并且，需要build 中增加以下配置，让代码可以在编译的时候，能够正确的处理 Lombok 的注解，基于这些注解帮我们生成我们想要的一些模板代码。
```xml
<build>  
    <plugins>  
        <plugin>  
            <groupId>org.apache.maven.plugins</groupId>  
            <artifactId>maven-compiler-plugin</artifactId>  
            <version>3.11.0</version>  
            <configuration>  
                <annotationProcessorPaths>  
                    <path>  
                        <groupId>org.projectlombok</groupId>  
                        <artifactId>lombok</artifactId>  
                        <version>1.18.30</version>  
                    </path>  
                </annotationProcessorPaths>  
            </configuration>  
        </plugin>  
    </plugins>  
</build>
```

### 2. 配置 IDE插件

Lombok 需要在 IDE 中进行配置插件，以便正确地识别和处理 Lombok 注解。

1. 打开 IntelliJ IDEA 设置（`File` -> `Settings`）。
2. 导航到 `Build, Execution, Deployment` -> `Compiler` -> `Annotation Processors`。
3. 勾选 `Enable annotation processing`。

另外，确保你已经安装了 Lombok 插件。可以通过 `File` -> `Settings` -> `Plugins` 搜索 Lombok 插件并安装。

### 3. 使用 Lombok 注解

Lombok 提供了许多注解来简化代码，例如 `@Getter`、`@Setter`、`@Data`、`@Builder` 等。以下是一些示例：

**示例 1: 使用 `@Data` 注解**

`@Data` 注解生成 getter、setter、toString、equals 和 hashCode 方法：

```java
import lombok.Data;  
  
@Data
public class User  
{  
    private String id;  
    private String name;  
    private int age;  
}
```

**示例 2: 使用 `@Builder` 注解**

`@Builder` 注解生成构建器模式的代码：

```java
import lombok.Builder;  
import lombok.Data;  
  
@Data  
@Builder  
public class User  
{  
    private String id;  
    private String name;  
    private int age;  
}
```

**示例 3: 使用 `@Getter` 和 `@Setter` 注解**

`@Getter` 和 `@Setter` 注解分别生成 getter 和 setter 方法：

```java
import lombok.Getter;  
import lombok.Setter;  
  
public class User  
{  
    @Getter  
    @Setter    
    private String id;  
    @Getter  
    @Setter    
    private String name;  
    @Getter  
    @Setter    
    private int age;  
}
```

### 4. 编译和运行

确保你的项目能够正常编译和运行。Lombok 注解在编译时生成代码，所以你的编译环境需要正确配置以处理这些注解。




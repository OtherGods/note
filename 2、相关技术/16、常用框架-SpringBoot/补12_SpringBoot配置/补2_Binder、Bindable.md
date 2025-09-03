
# 1、Binder

`org.springframework.boot.context.properties.bind.Binder`是Spring Boot中用于绑定配置属性的核心类。它提供了一种统一的方式来获取和转换配置属性的值，并支持各种绑定操作。
## 1.1、介绍
`Binder`类的详细介绍如下：

- `get(Environment environment)`：通过给定的`Environment`对象创建一个`Binder`实例。
- `bind(String name, Bindable<?> target)`：绑定指定名称的配置属性到给定的`Bindable`目标对象。
- `bind(ConfigurationPropertyName name, Bindable<?> target)`：绑定指定的`ConfigurationPropertyName`对象代表的配置属性到给定的`Bindable`目标对象。
- `bind(String name, Class<T> target)`：绑定指定名称的配置属性到给定的目标类型。
- `bind(ConfigurationPropertyName name, Class<T> target)`：绑定指定的`ConfigurationPropertyName`对象代表的配置属性到给定的目标类型。
	  用于将属性值绑定到指定类型的对象。它接受一个ConfigurationPropertyName对象作为参数，该对象表示要绑定的属性名。bind方法返回一个BindableResult对象，该对象包含了绑定结果。
- `bind(String prefix, Bindable<?> target)`：绑定以指定前缀开头的配置属性到给定的`Bindable`目标对象。
- `bind(ConfigurationPropertyName prefix, Bindable<?> target)`：绑定以指定的`ConfigurationPropertyName`对象代表的前缀开头的配置属性到给定的`Bindable`目标对象。
- `bind(String prefix, Class<T> target)`：绑定以指定前缀开头的配置属性到给定的目标类型。
- `bind(ConfigurationPropertyName prefix, Class<T> target)`：绑定以指定的`ConfigurationPropertyName`对象代表的前缀开头的配置属性到给定的目标类型。
- `bindOrCreate`：用于将属性值绑定到指定类型的对象，如果属性不存在则创建一个新的对象。它接受一个ConfigurationPropertyName对象作为参数，该对象表示要绑定的属性名。如果属性不存在，则创建一个新的对象并绑定属性值。
- `bindInstance`：用于将属性值绑定到已经存在的对象。它接受一个已经存在的对象作为参数，并将属性值绑定到该对象。

## 1.2、使用步骤
1. 创建一个Environment对象，表示应用程序的环境。
2. 使用Binder的静态方法get获取一个绑定器实例。
3. 使用ConfigurationPropertyName对象表示要绑定的属性名。
4. 调用bind方法将属性值绑定到指定类型的对象，或使用bindOrCreate方法将属性值绑定到指定类型的对象或创建一个新的对象，或使用bindInstance方法将属性值绑定到已经存在的对象。
5. 使用BindableResult对象获取绑定结果。

## 1.3、使用示例
以下是一些示例，展示了`Binder`的使用方式：

1. 绑定单个配置属性的值：

   ```java
   Binder binder = Binder.get(environment);
   String value = binder.bind("property.name", String.class).orElse(null);
   ```

   在这个示例中，我们使用`Binder.get(environment)`方法创建了一个`Binder`实例，然后使用`bind()`方法绑定了名为"property.name"的配置属性到目标类型为`String`。最后，我们使用`.orElse()`方法设置一个默认值，如果配置属性不存在，则返回默认值。

2. 绑定多个配置属性的值：

   ```java
   Binder binder = Binder.get(environment);
   List<String> values = binder.bind("property.names", Bindable.listOf(String.class)).orElse(null);
   ```

   在这个示例中，我们使用`Binder.get(environment)`方法创建了一个`Binder`实例，然后使用`bind()`方法绑定了名为"property.names"的配置属性到目标类型为`List<String>`。最后，我们使用`.orElse()`方法设置一个默认值，如果配置属性不存在，则返回默认值。

3. 绑定以指定前缀开头的配置属性：

   ```java
   Binder binder = Binder.get(environment);
   Map<String, String> properties = binder.bind("my.prefix", Bindable.mapOf(String.class, String.class)).orElse(null);
   ```

   在这个示例中，我们使用`Binder.get(environment)`方法创建了一个`Binder`实例，然后使用`bind()`方法绑定了以"my.prefix"开头的配置属性到目标类型为`Map<String, String>`。最后，我们使用`.orElse()`方法设置一个默认值，如果配置属性不存在，则返回默认值。

`Binder`类还提供了其他一些方法，用于更复杂的配置属性绑定操作，如绑定集合类型、绑定可选属性等。

需要注意的是，使用`Binder`时，需要在类路径中包含Spring Boot的相关依赖。同时，通过`Binder`对象可以获取到`Environment`对象来进行配置属性的绑定操作。


# 2、Bindable

`org.springframework.boot.context.properties.bind.Bindable`是Spring Boot中用于绑定配置属性的类。它提供了一种方便的方式来获取和转换配置属性的值。

`Bindable`类的详细介绍如下：

- `of(Class<T> type)`：创建一个用于绑定指定类型的`Bindable`实例。
- `listOf(Class<T> elementType)`：创建一个用于绑定指定元素类型的列表的`Bindable`实例。
- `setOf(Class<T> elementType)`：创建一个用于绑定指定元素类型的集合的`Bindable`实例。
- `mapOf(Class<K> keyType, Class<V> valueType)`：创建一个用于绑定指定键类型和值类型的映射的`Bindable`实例。
- `withExistingValue(T value)`：创建一个包含现有值的`Bindable`实例。
- `withDefault(T defaultValue)`：创建一个包含默认值的`Bindable`实例。

以下是一些示例，展示了`Bindable`的使用方式：

1. 获取单个配置属性的值：

   ```java
   Bindable<String> bindable = Bindable.of(String.class);
   String value = ConfigurationPropertiesBinder.get(environment).bind("property.name", bindable).orElse(null);
   ```

   在这个示例中，我们使用`Bindable.of()`方法创建了一个`Bindable`实例，绑定了类型为`String`的配置属性。然后，我们使用`ConfigurationPropertiesBinder.get(environment).bind()`方法获取了名为"property.name"的配置属性值。

2. 获取多个配置属性的值：

   ```java
   Bindable<List<String>> bindable = Bindable.listOf(String.class);
   List<String> values = ConfigurationPropertiesBinder.get(environment).bind("property.names", bindable).orElse(null);
   ```

   在这个示例中，我们使用`Bindable.listOf()`方法创建了一个`Bindable`实例，绑定了类型为`List<String>`的配置属性。然后，我们使用`ConfigurationPropertiesBinder.get(environment).bind()`方法获取了名为"property.names"的配置属性的多个值。

3. 转换配置属性的值：

   ```java
   Bindable<Integer> bindable = Bindable.of(Integer.class);
   Integer value = ConfigurationPropertiesBinder.get(environment).bind("property.age", bindable).orElse(18);
   ```

   在这个示例中，我们使用`Bindable.of()`方法创建了一个`Bindable`实例，绑定了类型为`Integer`的配置属性。然后，我们使用`ConfigurationPropertiesBinder.get(environment).bind()`方法获取了名为"property.age"的配置属性值，并通过`.orElse()`方法设置了一个默认值，如果配置属性不存在，则返回默认值。

`Bindable`类还提供了其他一些方法，用于更复杂的配置属性绑定操作，如集合类型的绑定、可选属性的绑定等。

需要注意的是，使用`Bindable`时，需要在类路径中包含Spring Boot的相关依赖。同时，`ConfigurationPropertiesBinder.get(environment)`方法用于获取一个`ConfigurationPropertiesBinder`对象，用于执行配置属性的绑定操作。


# 3、Binder、Bindable、BindHandler

为了详细介绍 `Binder`、`Bindable` 和 `BindHandler` 的作用和用法，下面将通过一个示例展示它们在实际应用中的使用场景。

### 示例：将外部配置绑定到 Java 对象

#### 1. 依赖配置

首先，在 `pom.xml` 中添加必要的依赖：

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
    </dependency>
</dependencies>
```

#### 2. 配置文件

在 `application.yml` 文件中定义一些外部配置：

```yaml
app:
  name: MyApp
  version: 1.0.0
  feature:
    enabled: true
    description: This is a sample feature
```

#### 3. 配置属性类

创建一个 Java 类，用于存储绑定的配置属性：

```java
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 在测试的时候把这两个注解注释换成@Data或setter/getter
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private String name;
    private String version;
    private Feature feature;

    // Getters and setters

    public static class Feature {
        private boolean enabled;
        private String description;

        // Getters and setters
    }
}
```

#### 4. 使用 `Binder` 进行绑定

创建一个类，演示如何使用 `Binder`、`Bindable` 和 `BindHandler` 进行属性绑定：

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.PropertySourcesPropertyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;

@Configuration
public class AppConfigBinder {

    @Autowired
    private Environment environment;

    @Bean
    public AppConfig appConfig() {
        // 获取 PropertySources
        PropertySources propertySources = ((StandardEnvironment) environment).getPropertySources();
        // 创建 Binder
        Binder binder = new Binder(ConfigurationPropertySources.from(propertySources));
        // 创建 Bindable
        Bindable<AppConfig> bindable = Bindable.of(AppConfig.class);
        // 创建 BindHandler
        BindHandler bindHandler = new IgnoreErrorsBindHandler();

        // 绑定配置到 AppConfig 实例
        AppConfig appConfig = binder.bind(ConfigurationPropertyName.of("app"), bindable, bindHandler).get();
        return appConfig;
    }
}
```

#### 5. 使用绑定的配置

创建一个控制器或服务类，演示如何使用绑定的配置：

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppConfigController {

    @Autowired
    private AppConfig appConfig;

    @GetMapping("/config")
    public AppConfig getConfig() {
        return appConfig;
    }
}
```

### 详细解释

#### `Binder`

- **作用**：`Binder` 用于将配置属性源中的属性值绑定到目标对象上。
- **用法**：通过 `ConfigurationPropertySources` 获取配置属性源，然后创建 `Binder` 实例，并使用 `bind` 方法进行属性绑定。

#### `Bindable`

- **作用**：`Bindable` 表示可以绑定的对象及其元数据（如类型和注解）。
- **用法**：使用 `Bindable.of(Class<T> type)` 方法创建一个 `Bindable` 实例，表示要绑定的目标对象的类型。

#### `BindHandler`

- **作用**：`BindHandler` 用于在绑定过程中处理事件和错误，允许自定义绑定行为。
- **用法**：可以创建不同的 `BindHandler` 实现，例如 `IgnoreErrorsBindHandler`，用于在绑定过程中忽略错误。

### 代码流程

1. **获取配置属性源**：从 `Environment` 获取 `PropertySources`。
2. **创建 `Binder` 实例**：将 `PropertySources` 转换为 `ConfigurationPropertySource`，然后创建 `Binder` 实例。
3. **创建 `Bindable` 实例**：使用 `Bindable.of(AppConfig.class)` 创建一个 `Bindable` 实例，表示要绑定的目标对象类型。
4. **创建 `BindHandler` 实例**：创建一个 `IgnoreErrorsBindHandler` 实例，用于在绑定过程中忽略错误。
5. **进行属性绑定**：使用 `Binder` 的 `bind` 方法将配置属性绑定到 `AppConfig` 实例上。
6. **使用绑定的配置**：通过 `@Autowired` 注入绑定的 `AppConfig` 实例，并在控制器中使用。

通过以上步骤，我们实现了将外部配置绑定到 Java 对象，并在应用中使用这些绑定的配置属性。

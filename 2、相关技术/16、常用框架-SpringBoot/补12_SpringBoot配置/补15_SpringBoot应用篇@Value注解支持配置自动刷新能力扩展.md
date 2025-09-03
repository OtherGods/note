在我们的日常开发中，使用`@Value`来绑定配置属于非常常见的基础操作，但是这个配置注入是一次性的，简单来说就是配置一旦赋值，则不会再修改；  
通常来讲，这个并没有什么问题，基础的SpringBoot项目的配置也基本不存在配置变更，如果有使用过SpringCloudConfig的小伙伴，会知道`@Value`可以绑定远程配置，并支持动态刷新

接下来本文将通过一个实例来演示下，如何让`@Value`注解支持配置刷新；本文将涉及到以下知识点

- BeanPostProcessorAdapter + 自定义注解：获取支持自动刷新的配置类
- MapPropertySource：实现配置动态变更
# 1、项目环境
## 1.1、项目依赖

本项目借助`SpringBoot 2.2.1.RELEASE` + `maven 3.5.3` + `IDEA`进行开发

开一个web服务用于测试
```xml
<dependencies>  
    <dependency>  
        <groupId>org.springframework.boot</groupId>  
        <artifactId>spring-boot-starter-web</artifactId>  
    </dependency>  
</dependencies>
```

# 2、配置动态刷新支持
## 2.1、思路介绍

要支持配合的动态刷新，重点在于下面两点

- 如何修改`Environment`中的配置源
- 配置变更之后，如何通知到相关的类同步更新

## 2.2、修改配置

相信很多小伙伴都不会去修改`Environment`中的数据源，突然冒出一个让我来修改配置源的数据，还是有点懵的，这里推荐之前分享过一篇博文 [补12_SpringBoot之自定义配置源的使用姿势](2、相关技术/16、常用框架-SpringBoot/补12_SpringBoot配置/补12_SpringBoot之自定义配置源的使用姿势.md)

当我们知道如何去自定义配置源之后，再来修改数据源，就会有一点思路了

定义一个配置文件`application-dynamic.yml`
```yml
xhh:  
	dynamic:  
		name: 一灰灰blog
```

然后在主配置文件中使用它
```yml
spring:  
	profiles:  
		active: dynamic
```

> ChatGPT解释：
> 	设置`spring.profiles.active=dynamic`的作用是指定Spring Boot应用程序在启动时激活名为`dynamic`的Profile。
> 	Profile是Spring框架提供的一种机制，用于根据不同的环境或条件加载不同的配置。通过设置`spring.profiles.active`属性，可以控制应用程序在不同环境下的行为。
> 	
> 	具体作用包括：
> 	1. **加载特定的配置文件**：当`spring.profiles.active`设置为`dynamic`时，Spring Boot会加载`application-dynamic.properties`或`application-dynamic.yml`等配置文件，这些文件中的配置会覆盖默认的配置。
> 	2. **激活对应的Profile**：通过设置`spring.profiles.active=dynamic`，Spring Boot会激活名为`dynamic`的Profile，从而加载与该Profile相关的配置。
> 	3. **影响Bean的加载**：在激活了`dynamic` Profile的情况下，可能会根据该Profile加载不同的Bean定义或组件。
> 	4. **影响应用程序行为**：根据不同的Profile，可以配置不同的行为、环境变量等，从而影响应用程序的运行方式。
> 	
> 	总的来说，通过设置`spring.profiles.active=dynamic`，可以动态地激活指定的Profile，从而实现根据不同环境加载不同配置和调整应用程序行为的目的。
> 	
> 	希望这个解释能够帮助你理解`spring.profiles.active=dynamic`的作用。如果你有任何其他问题或需要更多帮助，请随时告诉我。

使用配置的java config
```java
@Data  
@Component  
public class RefreshConfigProperties {  
  
    @Value("${xhh.dynamic.name}")  
    private String name;  
  
    @Value("${xhh.dynamic.age:18}")  
    private Integer age;  
  
    @Value("hello ${xhh.dynamic.other:test}")  
    private String other;  
}
```

接下来进入修改配置的正题
```java
@Autowired  
ConfigurableEnvironment environment;  
  
// --- 配置修改  
String name = "applicationConfig: [classpath:/application-dynamic.yml]";  
MapPropertySource propertySource = (MapPropertySource) environment.getPropertySources().get(name);  
Map<String, Object> source = propertySource.getSource();  
Map<String, Object> map = new HashMap<>(source.size());  
map.putAll(source);  
map.put(key, value);  
environment.getPropertySources().replace(name, new MapPropertySource(name, map));
```

上面的实现中，有几个疑问点

- name如何找到的？
    - debug…
- 配置变更
    - 注意修改配置是新建了一个Map，然后将旧的配置拷贝到新的Map，然后再执行替换；并不能直接进行修改，有兴趣的小伙伴可以实测一下为什么(不可修改的Map)

## 2.3、配置同步

上面虽然是实现了配置的修改，但是对于使用`@Value`注解修饰的变量，已经被赋值了，如何能感知到配置的变更，并同步刷新呢？

这里就又可以拆分两块

- 找到需要修改的配置
- 修改事件同步

### 2.3.1、找出需要刷新的配置变量

我们这里额外增加了一个注解，用来修饰需要支持动态刷新的场景
```java
@Target({ElementType.TYPE})  
@Retention(RetentionPolicy.RUNTIME)  
@Documented  
public @interface RefreshValue {  
}
```

接下来我们就是找出有上面这个注解的类，然后支持这些类中`@Value`注解绑定的变量动态刷新

关于这个就有很多实现方式了，我们这里选择`BeanPostProcessor`，bean创建完毕之后，借助反射来获取`@Value`绑定的变量，并缓存起来
```java
@Component  
public class AnoValueRefreshPostProcessor extends InstantiationAwareBeanPostProcessorAdapter implements EnvironmentAware {  
    private Map<String, List<FieldPair>> mapper = new HashMap<>();  
    private Environment environment;  
  
    @Override  
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {  
        processMetaValue(bean);  
        return super.postProcessAfterInstantiation(bean, beanName);  
    }  
  
    /**  
     * 这里主要的目的就是获取支持动态刷新的配置属性，然后缓存起来  
     *  
     * @param bean  
     */  
    private void processMetaValue(Object bean) {  
        Class clz = bean.getClass();  
        if (!clz.isAnnotationPresent(RefreshValue.class)) {  
            return;  
        }  
  
        try {  
            for (Field field : clz.getDeclaredFields()) {  
                if (field.isAnnotationPresent(Value.class)) {  
                    Value val = field.getAnnotation(Value.class);  
                    List<String> keyList = pickPropertyKey(val.value(), 0);  
                    for (String key : keyList) {  
                        mapper.computeIfAbsent(key, (k) -> new ArrayList<>())  
                                .add(new FieldPair(bean, field, val.value()));  
                    }  
                }  
            }  
        } catch (Exception e) {  
            e.printStackTrace();  
            System.exit(-1);  
        }  
    }  
  
    /**  
     * 实现一个基础的配置文件参数动态刷新支持  
     *  
     * @param value  
     * @return 提取key列表  
     */  
    private List<String> pickPropertyKey(String value, int begin) {  
        int start = value.indexOf("${", begin) + 2;  
        if (start < 2) {  
            return new ArrayList<>();  
        }  
  
        int middle = value.indexOf(":", start);  
        int end = value.indexOf("}", start);  
  
        String key;  
        if (middle > 0 && middle < end) {  
            // 包含默认值  
            key = value.substring(start, middle);  
        } else {  
            // 不包含默认值  
            key = value.substring(start, end);  
        }  
  
        List<String> keys = pickPropertyKey(value, end);  
        keys.add(key);  
        return keys;  
    }  
  
    @Override  
    public void setEnvironment(Environment environment) {  
        this.environment = environment;  
    }  
  
    @Data  
    @NoArgsConstructor  
    @AllArgsConstructor  
    public static class FieldPair {  
        private static PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}",  
                ":", true);  
  
        Object bean;  
        Field field;  
        String value;  
  
        public void updateValue(Environment environment) {  
            boolean access = field.isAccessible();  
            if (!access) {  
                field.setAccessible(true);  
            }  
  
            String updateVal = propertyPlaceholderHelper.replacePlaceholders(value, environment::getProperty);  
            try {  
                if (field.getType() == String.class) {  
                    field.set(bean, updateVal);  
                } else {  
                    field.set(bean, JSONObject.parseObject(updateVal, field.getType()));  
                }  
            } catch (IllegalAccessException e) {  
                e.printStackTrace();  
            }  
            field.setAccessible(access);  
        }  
    }  
}
```

上面的实现虽然有点长，但是核心逻辑就下面节点

- processMetaValue():
    - 通过反射，捞取带有`@Value`注解的变量
- pickPropertyKey()
    - 主要就是解析`@Value`注解中表达式，挑出变量名，用于缓存
    - 如: `@value("hello ${name:xhh} ${now:111}`
    - 解析之后，有两个变量，一个 `name` 一个 `now`
- 缓存`Map<String, List<FieldPair>>`
    - 缓存的key，为变量名
    - 缓存的value，自定义类，主要用于反射修改配置值

> ChatGPT对PropertyPlaceholderHelper的解释：
> 
> `PropertyPlaceholderHelper`类是Spring Framework中用于解析和替换属性占位符的实用工具类。它允许在文本中使用占位符`${...}`或者`${{...}}`，并将这些占位符替换为实际的属性值。
> 
> 下面是关于`PropertyPlaceholderHelper`类的一些介绍：
> 
> - `PropertyPlaceholderHelper`类提供了`replacePlaceholders(String text, PlaceholderResolver placeholderResolver)`方法来替换文本中的占位符。
> - 占位符可以包含在`${...}`或`${{...}}`中，其中`...`表示属性的键。
> - `PlaceholderResolver`是一个接口，用于实现占位符的解析逻辑，根据属性的键获取实际的属性值。
> 
> 以下是一个简单的示例，展示了如何在Java中使用`PropertyPlaceholderHelper`类：
> 
> ```java
> import org.springframework.util.PropertyPlaceholderHelper;
> import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
> 
> public class PropertyPlaceholderExample {
> 
>     public static void main(String[] args) {
>         PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}");
>         PlaceholderResolver resolver = placeholder -> {
>             if (placeholder.equals("name")) {
>                 return "John Doe";
>             } else if (placeholder.equals("age")) {
>                 return "30";
>             }
>             return null;
>         };
> 
>         String text = "Hello, my name is ${name} and I am ${age} years old.";
>         String resolvedText = helper.replacePlaceholders(text, resolver);
>         System.out.println(resolvedText);
>     }
> }
> ```
> 
> 在这个示例中，我们首先创建了一个`PropertyPlaceholderHelper`实例，指定了占位符的起始和结束标记为`${`和`}`。然后，我们创建了一个`PlaceholderResolver`接口的实现，根据属性的键返回相应的属性值。接着，我们定义了一个包含占位符的文本字符串，并使用`replacePlaceholders()`方法替换占位符为实际的属性值。最后，我们打印出替换后的文本。
> 
> 通过使用`PropertyPlaceholderHelper`类，可以方便地在Java应用程序中解析和替换属性占位符，实现动态配置或文本替换的功能。
### 2.3.2、修改事件同步

从命名也可以看出，我们这里选择事件机制来实现同步，直接借助Spring Event来完成

一个简单的自定义类事件类
```java
public static class ConfigUpdateEvent extends ApplicationEvent {  
    String key;  
  
    public ConfigUpdateEvent(Object source, String key) {  
        super(source);  
        this.key = key;  
    }  
}
```

消费也比较简单，直接将下面这段代码，放在上面的`AnoValueRefreshPostProcessor`， 接收到变更事件，通过key从缓存中找到需要变更的Field，然后依次执行刷新即可
```java
@EventListener  
public void updateConfig(ConfigUpdateEvent configUpdateEvent) {  
    List<FieldPair> list = mapper.get(configUpdateEvent.key);  
    if (!CollectionUtils.isEmpty(list)) {  
        list.forEach(f -> f.updateValue(environment));  
    }  
}

@Data  
@NoArgsConstructor  
@AllArgsConstructor  
public static class FieldPair {  
    private static PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}",  
            ":", true);  
  
    Object bean;  
    Field field;  
    String value;  
  
    public void updateValue(Environment environment) {  
        boolean access = field.isAccessible();  
        if (!access) {  
            field.setAccessible(true);  
        }  
  
        // replacePlaceholders方法第二个参数（函数式接口PlaceholderResolver）的实现是获取配置文件中的值，这个  
        // 值在dynamic/update方法中被修改（修改的值是浏览器url的参数），所以这里需要重新获取  
        String updateVal = propertyPlaceholderHelper.replacePlaceholders(value, environment::getProperty);  
        try {  
            if (field.getType() == String.class) {  
                field.set(bean, updateVal);  
            } else {  
                field.set(bean, JSONObject.parseObject(updateVal, field.getType()));  
            }  
        } catch (IllegalAccessException e) {  
            e.printStackTrace();  
        }  
        field.setAccessible(access);  
    }  
}
```

## 2.4、实例演示

最后将前面修改配置的代码块封装一下，提供一个接口，来验证下我们的配置刷新
```java
@RestController  
public class DynamicRest {  
    @Autowired  
    ApplicationContext applicationContext;  
    @Autowired  
    ConfigurableEnvironment environment;  
    @Autowired  
    RefreshConfigProperties refreshConfigProperties;  
  
    @GetMapping(path = "dynamic/update")  
    public RefreshConfigProperties updateEnvironment(String key, String value) {  
        String name = "applicationConfig: [classpath:/application-dynamic.yml]";  
        MapPropertySource propertySource = (MapPropertySource) environment.getPropertySources().get(name);  
        Map<String, Object> source = propertySource.getSource();  
        Map<String, Object> map = new HashMap<>(source.size());  
        map.putAll(source);  
        map.put(key, value);  
        // 这里修改内存中配置文件的值（文件内容实际上没变）是为了在FieldPair.updateValue方法中获取到修改后的值
        environment.getPropertySources().replace(name, new MapPropertySource(name, map));  
  
        applicationContext.publishEvent(new AnoValueRefreshPostProcessor.ConfigUpdateEvent(this, key));  
        return refreshConfigProperties;  
    }  
}
```

![1.gif](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202402252255922.gif)

## 2.5、小结

本文主要通过简单的几步，对`@Value`进行了拓展，支持配置动态刷新，核心知识点下面三块：

- 使用BeanPostProcess来扫描需要刷新的变量
- 利用Spring Event事件机制来实现刷新同步感知
- 至于配置的修改，则主要是`MapPropertySource`来实现配置的替换修改

请注意，上面的这个实现思路，与Spring Cloud Config是有差异的，很久之前写过一个配置刷新的博文，有兴趣的小伙伴可以看一下 [补6_SpringBoot配置信息之配置刷新](2、相关技术/16、常用框架-SpringBoot/补12_SpringBoot配置/补6_SpringBoot配置信息之配置刷新.md)

# 3、其他
## 3.1、源码
- 工程：[https://github.com/liuyueyi/spring-boot-demo](https://github.com/liuyueyi/spring-boot-demo)
- 实例源码: 
	- [https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/002-properties-value](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-boot/002-properties-value)
  已经将这个项目Fork到我的GitHub：[https://github.com/OtherGods/paicoding-yihui-spring-boot-demo](https://github.com/OtherGods/paicoding-yihui-spring-boot-demo)


转载自：[SpringBoot应用篇@Value注解支持配置自动刷新能力扩展](https://spring.hhui.top/spring-blog/2021/08/01/210801-SpringBoot%E5%BA%94%E7%94%A8%E7%AF%87-Value%E6%B3%A8%E8%A7%A3%E6%94%AF%E6%8C%81%E9%85%8D%E7%BD%AE%E8%87%AA%E5%8A%A8%E5%88%B7%E6%96%B0%E8%83%BD%E5%8A%9B%E6%89%A9%E5%B1%95/)
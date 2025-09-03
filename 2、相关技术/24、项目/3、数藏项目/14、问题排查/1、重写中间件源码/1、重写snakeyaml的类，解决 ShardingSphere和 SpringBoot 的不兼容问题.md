SnakeYaml是一个功能强大的YAML解析框架，支持YAML1.1规范，支持UTF-8/UTF-16编码，并能序列化和反序列化Java对象。 它提供了简单易用的API，方便开发者处理YAML文件。

在我们的项目中，有多处依赖了他，其中包括我们引入的分库分表框架——ShardingSphere，ShardingSphere我们的引入之后，需要依赖SnakeYaml的1.33版本，更高的版本他不支持，会报错。
```java
Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [javax.sql.DataSource]: Factory method 'shardingSphereDataSource' threw exception with message: org.yaml.snakeyaml.representer.Representer: method 'void <init>()' not found  
at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:177)  
at org.springframework.beans.factory.support.ConstructorResolver.instantiate(ConstructorResolver.java:647)  
        ... 77 common frames omitted  
Caused by: java.lang.NoSuchMethodError: org.yaml.snakeyaml.representer.Representer: method 'void <init>()' not found  
at org.apache.shardingsphere.infra.util.yaml.representer.ShardingSphereYamlRepresenter.<init>(ShardingSphereYamlRepresenter.java:41)  
at org.apache.shardingsphere.infra.util.yaml.YamlEngine.marshal(YamlEngine.java:113)
```

当时我们又不能直接把SnakeYaml仲裁成1.33，主要是因为我们的 SpringBoot 用的是3.2.2版本，他要求SnakeYaml必须要是2.2版本，低版本不兼容。

为了解决这个问题，我们经过分析，其实 SpringBoot对2.2的依赖，主要是新增了两个类，一个是TagInspector，一个是 UnTrustedTagInspector，只要我们把这两个类给定义了，那么即使我们依赖1.33版本也是可以的。

于是，我们把代码依赖的仲裁改成1.33
```xml
<dependency>  
    <groupId>org.yaml</groupId>  
    <artifactId>snakeyaml</artifactId>  
    <version>1.33</version>  
</dependency>
```

然后在我们的项目中定义两个类：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411101125703.png)

至于里面的代码，就直接把2.2中的代码复制过来就行了。
```java
/**  
 * Copyright (c) 2008, SnakeYAML * <p>  
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except * in compliance with the License. You may obtain a copy of the License at * <p>  
 * http://www.apache.org/licenses/LICENSE-2.0 * <p>  
 * Unless required by applicable law or agreed to in writing, software distributed under the License * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express * or implied. See the License for the specific language governing permissions and limitations under * the License. */

package org.yaml.snakeyaml.inspector;  
  
import org.yaml.snakeyaml.nodes.Tag;  
  
/**  
 * Check if the global tags are allowed (the local tags are always allowed). It should control the * classes to create to prevent possible remote code invocation when the data comes from untrusted * source. The standard tags are always allowed (https://yaml.org/type/index.html) * @author hollis */
public interface TagInspector {  
      
    /**  
     * Check     *     * @param tag - the global tag to check  
     * @return true when the custom global tag is allowed to create a custom Java instance     */    boolean isGlobalTagAllowed(Tag tag);  
      
}
```

```java
/**  
 * Copyright (c) 2008, SnakeYAML * <p>  
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except * in compliance with the License. You may obtain a copy of the License at * <p>  
 * http://www.apache.org/licenses/LICENSE-2.0 * <p>  
 * Unless required by applicable law or agreed to in writing, software distributed under the License * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express * or implied. See the License for the specific language governing permissions and limitations under * the License. */

package org.yaml.snakeyaml.inspector;  
  
import org.yaml.snakeyaml.nodes.Tag;  
  
/**  
 * TagInspector which does not allow to create any custom instance. It should not be used when the * data comes from untrusted source to prevent possible remote code invocation. * @author hollis */

public final class UnTrustedTagInspector implements TagInspector {  
      
    /**  
     * Allow none     *     * @param tag - the global tag to reject  
     * @return always return false     */    @Override  
    public boolean isGlobalTagAllowed(Tag tag) {  
        return false;  
    }  
}
```
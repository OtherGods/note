JetCache 是一个 Java 缓存库，旨在提供简洁、高性能且通用的缓存解决方案。它支持多种缓存策略和缓存存储类型，包括本地缓存（如 Caffeine、Guava 等）和分布式缓存（ Redis、Memcached 等）

Caffeine 是一个高性能的 Java 缓存库，受到了 Google Guava 缓存库的启发。它主要用于实现本地缓存，具有高效的缓存命中率和低延迟的特点。Caffeine 提供了灵活的缓存策略和强大的功能，使其成为许多 Java 应用程序的首选缓存解决方案。

JetCache 和 Caffeine 是两个用于 Java 应用程序中的缓存库，各有其独特的特点和用途。

> ChatGPT解释：
> 
> **JetCache** 是一个高性能的 Java 缓存库，提供了统一的 API 来访问不同类型的缓存，包括本地缓存和分布式缓存。JetCache 的设计目标是简化缓存的使用，同时提供高性能和丰富的功能。
> 
> 主要功能和特点：
> 1. **统一的 API**：JetCache 提供了一套统一的 API 来管理本地缓存和分布式缓存，支持多种缓存实现，如 Caffeine、Redis、Memcached 等。
> 2. **注解支持**：通过注解简单配置缓存，例如 @Cacheable、@CacheUpdate 和 @CacheInvalidate，大大简化了缓存的使用。
> 3. **自动刷新**：支持缓存自动刷新，防止缓存过期导致的缓存穿透。
> 4. **高性能**：JetCache 尽量减少缓存操作的开销，提供了高效的缓存访问。
> 5. **缓存一致性**：JetCache 支持多种缓存一致性策略，保证缓存数据的准确性。
> 
> **Caffeine** 是一个高性能的 Java 本地缓存库，旨在提供接近最佳性能的缓存解决方案。它是 Guava Cache 的替代品，但性能和功能更强大。
> 
> 主要功能和特点：
> 1. **高性能**：Caffeine 使用先进的算法（如 Window TinyLFU）来提高缓存命中率和性能。
> 2. **灵活配置**：提供了多种配置选项，如最大缓存大小、过期策略、缓存淘汰策略等，用户可以根据需要进行灵活配置。
> 3. **异步加载**：支持异步加载缓存数据，避免阻塞主线程。
> 4. **统计信息**：提供丰富的统计信息，可以方便地监控缓存的使用情况。
> 5. **参考策略**：提供多种缓存引用策略，如弱引用、软引用等，适应不同的内存管理需求。
> 
> **JetCache 和 Caffeine 的区别与应用场景**
> 使用场景：
> - **JetCache**：适用于需要统一管理本地缓存和分布式缓存的应用场景，尤其是在分布式系统中，JetCache 提供了简洁的 API 和注解支持，方便开发人员快速集成缓存。
> - **Caffeine**：适用于需要高性能本地缓存的应用场景。Caffeine 的设计和实现更加注重本地缓存的性能和灵活性，适合在单机或简单的分布式环境中使用。
> 
> **区别：**
> 1. **缓存类型**：
>    - JetCache 支持本地缓存和分布式缓存（如 Redis、Memcached）。
>    - Caffeine 专注于本地缓存，不支持分布式缓存。
> 2. **API 设计**：
>    - JetCache 提供统一的 API 和注解支持，简化了多种缓存类型的管理。
>    - Caffeine 提供高性能的本地缓存 API，侧重于缓存配置的灵活性和性能。
> 3. **功能特性**：
>    - JetCache 提供了多种缓存一致性策略和自动刷新功能，适合复杂的缓存场景。
>    - Caffeine 提供了高级的缓存淘汰算法和异步加载功能，适合需要高性能和灵活配置的场景。
> 
> **选择建议：**
> - 如果你的应用需要同时使用本地缓存和分布式缓存，并且希望通过统一的 API 和注解来简化开发，可以选择 JetCache。
> - 如果你的应用主要依赖本地缓存，并且需要高性能和灵活的缓存配置，Caffeine 是一个很好的选择。
> 
> 通过了解 JetCache 和 Caffeine 的特点和适用场景，可以更好地选择合适的缓存解决方案来优化应用程序的性能和可扩展性。

# 1、依赖引入

```xml
<!--    Caffeine    -->  
<dependency>  
    <groupId>com.github.ben-manes.caffeine</groupId>  
    <artifactId>caffeine</artifactId>  
    <version>3.1.8</version>  
</dependency>  
  
<!--    JetCache    -->  
<dependency>  
    <groupId>com.alicp.jetcache</groupId>  
    <artifactId>jetcache-starter-redisson</artifactId>  
    <version>2.7.5</version>  
    <exclusions>  
        <exclusion>  
            <groupId>org.springframework.boot</groupId>  
            <artifactId>spring-boot-starter-logging</artifactId>  
        </exclusion>  
    </exclusions>  
</dependency>  
  
<!--     Redis  -->  
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-data-redis</artifactId>  
</dependency>
```

# 2、文件配置

在配置文件中增加和jetcache有关的配置：
```yml
jetcache:
  statIntervalMinutes: 1        # 设置统计信息收集的时间间隔为1分钟  
  areaInCacheName: false        # 禁用在缓存名称中包含区域名称  
  local:  
    default:  
      type: caffeine            # 使用Caffeine作为本地缓存实现  
      keyConvertor: fastjson2   # 使用Fastjson2进行键转换  
  remote:  
    default:  
      type: redisson            # 使用Redisson作为远程缓存实现  
      keyConvertor: fastjson2   # 使用Fastjson2进行键转换  
      broadcastChannel: ${spring.application.name}  # 广播频道名称设置为Spring应用程序名称  
      keyPrefix: ${spring.application.name}         # 键全局前缀设置为Spring应用程序名称  
      valueEncoder: java        # 使用Java进行值编码  
      valueDecoder: java        # 使用Java进行值解码  
      defaultExpireInMillis: 5000  # 设置默认过期时间为5000毫秒（5秒）
```

> AI解释：
> 这段配置是关于JetCache的，JetCache是一个基于Java的缓存框架，它支持本地和远程缓存，并且提供了多种缓存策略。以下是对这段配置的详细解释：
> 1. **jetcache**:
>     - **statIntervalMinutes**: 统计间隔分钟数，设置为1表示每隔1分钟会统计一次缓存的相关数据（如命中率等）。
>     - **areaInCacheName**: 是否在缓存名中包含区域（area）信息，设置为false表示不包含。
> 2. **local**:
>     - 这部分配置了本地缓存的相关信息。
>     - **default**: 表示默认的本地缓存配置。
>         - **type**: 缓存类型，设置为`caffeine`表示使用Caffeine作为本地缓存实现。Caffeine是一个高性能的Java缓存库。
>         - **keyConvertor**: 缓存键的转换器，设置为`fastjson2`表示使用Fastjson2进行键的序列化和反序列化。
> 3. **remote**:
>     - 这部分配置了远程缓存的相关信息。
>     - **default**: 表示默认的远程缓存配置。
>         - **type**: 缓存类型，设置为`redisson`表示使用Redisson作为远程缓存实现。Redisson是一个在Redis的基础上实现的Java驻留型内存数据网格（In-Memory Data Grid）。
>         - **keyConvertor**: 同上，表示使用Fastjson2进行键的序列化和反序列化。
>         - **broadcastChannel**: 广播通道，设置为`${spring.application.name}`表示使用Spring应用的名字作为广播通道名，这通常用于缓存事件的广播。
>         - **keyPrefix**: 缓存键的前缀，设置为`${spring.application.name}`表示每个缓存键都会以Spring应用的名字作为前缀，这有助于在多应用共享同一Redis实例时区分不同的应用缓存。
>         - **valueEncoder** 和 **valueDecoder**: 分别表示缓存值的编码器和解码器，设置为`java`表示使用Java默认的序列化和反序列化机制。
>         - **defaultExpireInMillis**: 默认过期时间（毫秒），设置为5000表示缓存项默认在5秒后过期。
> 
> 总的来说，这段配置定义了JetCache的统计间隔、本地和远程缓存的默认实现方式、键和值的转换方式、广播通道和默认过期时间等。它使得JetCache能够按照这些设定进行缓存操作，从而提高应用的性能和响应速度。

# 3、开启注解

在启动类上增加`@EnableMethodCache`注解

```java
@EnableMethodCache(basePackages = "cn.hollis.nft.turbo")
public class Application {}
```



Easy-ES 是一个简化 Elasticsearch 使用的框架，旨在帮助开发者更容易地集成和使用 Elasticsearch。它提供了更加简洁和直观的 API，使得操作 Elasticsearch 变得更加方便和高效

# 1、pom 依赖导入

```xml
<dependency>  
    <groupId>org.elasticsearch.client</groupId>  
    <artifactId>elasticsearch-rest-high-level-client</artifactId>  
    <version>7.17.20</version>  
</dependency>  
  
<dependency>  
    <groupId>org.dromara.easy-es</groupId>  
    <artifactId>easy-es-boot-starter</artifactId>  
    <version>2.0.0-beta8</version>  
</dependency>
```

# 2、增加配置项

```yml
easy-es:  
  enable: true  
  address : 116.xx.xx.29:9200  
  username: elastic  
  password: 123456
```

# 3、增加 Mapper

```java
public interface CollectionEsMapper extends BaseEsMapper<Collection> { }
```

自定义一个 mapper，然后继承BaseEsMapper。

# 4、指定索引

在 Collection 中，需要指定和 ES 交互的时候的索引的 key：
```java
@Getter  
@Setter  
@IndexName(value = "nfturbo_collection")  
public class Collection extends BaseEntity {  
      
    /**  
     * '藏品名称'  
     */    private String name;  
      
    //省略其他字段  
}
```

# 5、增加ES 的 Mapper 扫描配置

这个配置可以配置到启动类上，也可以配置到配置类上。我们的项目中为了统一，配置到了EsConfiguration类上。
```java
@EsMapperScan("cn.hollis.nft.turbo.*.infrastructure.es.mapper")
```

# 6、单元测试

接下来就可以直接使用 Easy ES 进行调用了：
```java
// cn.hollis.nft.turbo.collection.infrastructure.repo.EsCollectionRepositoryTest  
  
@Autowired  
private CollectionEsMapper collectionEsMapper;  
  
@Test  
public void testFindByNameAndStateWithEasyEs(){  
    LambdaEsQueryWrapper<Collection> queryWrapper = new LambdaEsQueryWrapper<>();  
    queryWrapper.match(Collection::getName, "测试")  
            .and(wrapper -> wrapper  
                    .match(Collection::getState, "SUCCEED")  
                    .match(Collection::getDeleted, "0"))  
            .orderByAsc("collection_id");  
      
    EsPageInfo<Collection> results = collectionEsMapper.pageQuery(queryWrapper, 1, 1);  
    System.out.println(JSON.toJSONString(results));  
}
```






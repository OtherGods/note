
藏品的搜索是非常重要的功能，用户会通过主页搜索栏查询藏品然后进行购买，这地方需要支持模糊查询，而如果直接从数据库进行模糊查询的话`like '%xxx%'`的效率会很低，因为没办法走索引，随着数据量增大之后，根本就没办法满足用户的搜索功能。

于是，我们需要借助搜索引擎来实现藏品的搜索。我们的项目中采用 ES 作为搜索引擎。

管理员创建或者修改藏品时，系统交互如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409052130462.png)

用户搜索藏品时，系统交互如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409052130660.png)

以下是整个方案的实现过程。

### ES+Canal 部署

[7、ElasticSearch&Kibana 部署](2、相关技术/24、项目/3、数藏项目/2、中间件部署/7、ElasticSearch&Kibana%20部署.md)

[8、Canal 部署&binlog 监听](2、相关技术/24、项目/3、数藏项目/2、中间件部署/8、Canal%20部署&binlog%20监听.md)

基于以上文档，完成 ES+Kibana+Canal 的搭建，然后配置藏品信息从MySQL 到 ES 的配置。

### MySQL->Canal->ES配置

在conf/es8/目录下增加 nfturbo_collection.yml文件:
```yml
dataSourceKey: defaultDS  
destination: example  
groupId: g1  
esMapping:  
  _index: nfturbo_collection  
  _id: _id  
  #  upsert: true  
  #  pk: id  
  sql: "SELECT t.id as _id, t.name as name,t.cover as cover ,t.class_id as class_id,t.price as price,t.quantity as quantity ,t.detail as detail,t.saleable_inventory as saleable_inventory,t.occupied_inventory as occupied_inventory,t.state as state,t.create_time as create_time,t.sale_time as sale_time,t.sync_chain_time as sync_chain_time,t.deleted as deleted FROM collection as t"  
  #  objFields:  
  #    _labels: array:;  #etlCondition: "where a.c_time>={}"  
  commitBatch: 3000
```

重启 Canal，到canal-adapter/bin 下执行`./restart.sh`

到 ES 上创建索引：
```es
PUT nfturbo_collection
{
  "mappings": {
    "properties": {
      "name": {
        "type": "text"
      },
      "cover": {
        "type": "text"
      },
      "class_id": {
        "type": "text"
      },
      "price": {
        "type": "text"
      },
      "quantity": {
        "type": "long"
      },
      "detail": {
        "type": "text"
      },
      "saleable_inventory": {
        "type": "long"
      },
      "occupied_inventory": {
        "type": "long"
      },
      "state": {
        "type": "keyword"
      },
      "create_time": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss || yyyy-MM-dd'T'HH:mm:ss+08:00 || strict_date_optional_time || epoch_millis"
      },
      "sale_time": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss || yyyy-MM-dd'T'HH:mm:ss+08:00 || strict_date_optional_time || epoch_millis"
      },
      "sync_chain_time": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss || yyyy-MM-dd'T'HH:mm:ss+08:00 || strict_date_optional_time || epoch_millis"
      },
      "deleted": {
        "type": "keyword"
      }
    }
  }
}
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411101750312.png)

从 ES 控制台查询：
```es
GET nfturbo_collection/_search
{"_source": ["name","cover","price"],
  "query": {
    "match": {
      "name": "测试藏品"
    }
  }
}
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411101751710.png)

插入一条记录：
```SQL
INSERT INTO `collection` (
	`id`,
	`gmt_create`,
	`gmt_modified`,
	`name`,
	`cover`,
	`class_id`,
	`price`,
	`quantity`,
	`detail`,
	`saleable_inventory`,
	`identifier`,
	`occupied_inventory`,
	`state`,
	`create_time`,
	`sale_time`,
	`sync_chain_time`,
	`deleted`,
	`lock_version`,
	`creator_id`,
	`version` 
)
VALUES
	(
		3,
		'2024-03-11 16:03:25',
		'2024-04-30 16:52:54',
		'测试藏品11',
		'https://t7.baidu.com/it/u=1595072465,3644073269&fm=193&f=GIF',
		NULL,
		0.010000,
		100,
		'11111111321',
		51,
		NULL,
		NULL,
		'SUCCEED',
		'2024-03-11 16:04:14',
		'2024-03-11 16:04:16',
		'2024-03-11 16:04:19',
		0,
		49,
		NULL,
		NULL 
	);
```

然后从控制台查询：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411101752522.png)

### 应用直连 ES 查询

根据实际需要，接入 ES 和 Easy ES：

[10、ElasticSearch 接入](2、相关技术/24、项目/3、数藏项目/4、框架接入/10、ElasticSearch%20接入.md)
[11、Easy ES 接入](2、相关技术/24、项目/3、数藏项目/4、框架接入/11、Easy%20ES%20接入.md)

在接入之后，我们分别基于 Spring Data 和 EasyES 实现分页查询。

以下是通过elasticsearchOperations实现分页查询。根据用户传入的藏品名称的关键词以及状态进行分页查询。
```java
// cn.hollis.nft.turbo.collection.domain.service.impl.CollectionEsService  
  
public PageResponse<Collection> pageQueryByState(String name, String state, int currentPage, int pageSize) {  
    Criteria criteria = new Criteria("name").is(name).and(new Criteria("state").is(state), new Criteria("deleted").is("0"));  
    PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize);  
    Query query = new CriteriaQuery(criteria).setPageable(pageRequest).addSort(Sort.by(Sort.Order.asc("collection_id")));  
    SearchHits<Collection> searchHits = elasticsearchOperations.search(query, Collection.class);  
      
    return PageResponse.of(searchHits.getSearchHits().stream().map(SearchHit::getContent).toList(), (int) searchHits.getTotalHits(), pageSize);  
}
```

这里为了避免分页的时候数据丢失或者重复，我们选择一个唯一的字段进行排序，这里采用collection_id进行排序，collection_id是从 MySQL 的 collection 的 id 字段同步过来的，具体配置前面讲过了。

Collection 类需要使用`@Document(indexName = "nfturbo_collection")` 注解标识：
```java
@Getter  
@Setter  
@Document(indexName = "nfturbo_collection")  
@IndexName(value = "nfturbo_collection")  
public class Collection extends BaseEntity {  
      
    /**  
     * '藏品名称'  
     */    private String name;  
      
    /**  
     * '已占库存'  
     */    private Long occupiedInventory;  
      
      
    /**  
     * '藏品发售时间'  
     */    @Field(name = "sale_time",type = FieldType.Date, format = {},pattern = "yyyy-MM-dd HH:mm:ss || strict_date_optional_time || epoch_millis")  
    private Date saleTime;  
      
}
```

如果是字段名称一致的，会通过反射直接映射进来，如 name 字段，但是如果 ES中的字段名和代码中的字段名不一致，则无法直接映射，需要通过@Field指定他的 name，这样才能映射。

### 解决深度分页的问题

[2、基于 ElasticSearch 的search_after解决深分页问题](2、相关技术/24、项目/3、数藏项目/8、最佳实践/11、搜索引擎（ElasticSearch）/2、基于%20ElasticSearch%20的search_after解决深分页问题.md)




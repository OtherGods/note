在Elasticsearch中进行分页查询通常使用from和size参数。当我们对Elasticsearch发起一个带有分页参数的查询（如使用from和size参数）时，ES需要遍历所有匹配的文档直到达到指定的起始点（from），然后返回从这一点开始的size个文档。
```es
GET /your_index/_search
{
  "from": 20,
  "size": 10,
  "query": {
    "match_all": {}
  }
}
```

在这个例子中：
- from 参数定义了要跳过的记录数。在这里，它跳过了前20条记录。
- size 参数定义了返回的记录数量。在这里，它返回了10条记录。

from + size 的总数不能超过Elasticsearch索引的index.max_result_window设置，默认为10000。这意味着如果你设置from为9900，size为100，查询将会成功。但如果from为9900，size为101，则会失败。

**ES的检索机制决定了，当进行分页查询时，Elasticsearch需要先找到并处理所有位于当前页之前的记录。例如，如果你请求第1000页的数据，并且每页显示10条记录，系统需要先处理前9990条记录，然后才能获取到你请求的那10条记录。这意味着，随着页码的增加，数据库需要处理的数据量急剧增加，导致查询效率降低。**

**这就是ES的深度分页的问题**，深度分页需要数据库在内存中维护大量的数据，并对这些数据进行排序和处理，这会消耗大量的CPU和内存资源。随着分页深度的增加，查询响应时间会显著增加。在某些情况下，这可能导致查询超时或者系统负载过重。

通常来说，我们可以用 scroll 或者 search_after 来解决 ES 的深分页问题，他们的区别如下
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202411101905556.png)

我们的项目中，选择了性能更加好的 search_after 的方案，因为我们的项目中不存在随机页访问的场景，我们的页面是 APP 不断滚屏实现的分页，只能顺序翻页。所以可以完美的避开 search_after 不支持随机页访问的问题。

代码实现如下：
```java
// cn.hollis.nft.turbo.collection.domain.service.impl.CollectionEsService  
  
public SAPageInfo<Collection> deepPageQueryByState(String name, String state, int pageSize, Long lastId) {  
    LambdaEsQueryWrapper<Collection> queryWrapper = new LambdaEsQueryWrapper<>();  
    queryWrapper.match(Collection::getName, name)  
            .and(wrapper -> wrapper  
                    .match(Collection::getState, state)  
                    .match(Collection::getDeleted, "0"))  
            .orderByAsc("collection_id");  
      
    SAPageInfo<Collection> saPageInfo;  
    if (lastId == null) {  
        saPageInfo = collectionEsMapper.searchAfterPage(queryWrapper, null, pageSize);  
    } else {  
        saPageInfo = collectionEsMapper.searchAfterPage(queryWrapper, ImmutableList.of(lastId), 10);  
    }  
    return saPageInfo;  
}
```








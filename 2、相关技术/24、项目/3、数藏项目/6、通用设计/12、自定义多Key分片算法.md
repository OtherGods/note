## 1. 项目配置
在我们的项目中，我们对订单表基于买家 ID实现了分库分表，然后用基因法，将分表信息编码到订单号中。也就是说，我们的一次请求，只要带有买家 ID 或者订单号，任意一个字段就可以实现精准路由。

如下面的第12行的配置`shardingColumns: buyer_id,order_id`声明了两个路由字段。
```yml
shardingsphere:  
  rules:  
    sharding:  
      tables:  
        trade_order:  
          actual-data-nodes: ds.trade_order_000${0..3}  
          keyGenerateStrategy:  
            column: id  
            keyGeneratorName: snowflake  
          table-strategy:  
            complex:  
              shardingColumns: buyer_id,order_id  
              shardingAlgorithmName: trade-order-sharding  
        trade_order_stream:  
          actual-data-nodes: ds.trade_order_stream_000${0..3}  
          keyGenerateStrategy:  
            column: id  
            keyGeneratorName: snowflake  
          table-strategy:  
            complex:  
              shardingColumns: buyer_id,order_id  
              shardingAlgorithmName: trade-order-sharding  
      shardingAlgorithms:  
        #          t-order-inline:  
        #            type: INLINE        #            props:        #              algorithm-expression: trade_order_0${Math.abs(buyer_id.hashCode()) % 4}        trade-order-sharding:  
          type: CLASS_BASED  
          props:  
            algorithmClassName: cn.hollis.nft.turbo.datasource.sharding.algorithm.TurboKeyShardingAlgorithm  
            strategy: complex  
            tableCount: 4  
            mainColum: buyer_id  
      keyGenerators:  
        snowflake:  
          type: SNOWFLAKE  
      auditors:  
        sharding_key_required_auditor:  
          type: DML_SHARDING_CONDITIONS
```

然后，路由算法这里，我们自定义了一个`algorithmClassName: cn.hollis.nft.turbo.datasource.sharding.algorithm.TurboKeyShardingAlgorithm`来实现从buyer_id和order_id选一个更合适的进行决策。

主要代码如下：
```java
package cn.hollis.nft.turbo.datasource.sharding.algorithm;  
  
import cn.hollis.nft.turbo.datasource.sharding.id.DistributeID;  
import org.apache.commons.collections4.CollectionUtils;  
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;  
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;  
  
import java.util.Collection;  
import java.util.HashSet;  
import java.util.Properties;  
import java.util.stream.Collectors;  
  
/**  
 * 基于Turbo的分片算法实现，支持复杂键分片  
 * @author Hollis  
 */public class TurboKeyShardingAlgorithm implements ComplexKeysShardingAlgorithm<String> {  
      
    private Properties props;  
      
    // 主分片列的属性名  
    private static final String PROP_MAIN_COLUM = "mainColum";  
    // 分表的数量属性名  
    private static final String PROP_TABLE_COUNT = "tableCount";  
      
    @Override  
    public Properties getProps() {  
        return props;  
    }  
      
    @Override  
    public void init(Properties props) {  
        this.props = props;  
    }  
      
    @Override  
    public Collection<String> doSharding(Collection<String> availableTargetNames, ComplexKeysShardingValue<String> complexKeysShardingValue) {  
        Collection<String> result = new HashSet<>();  
          
        // 获取主分片键的属性名  
        String mainColum = props.getProperty(PROP_MAIN_COLUM);  
        // 获取分片键的值集合  
        Collection<String> mainColums = complexKeysShardingValue.getColumnNameAndShardingValuesMap().get(mainColum);  
          
        if (CollectionUtils.isNotEmpty(mainColums)) {  
            for (String colum : mainColums) {  
                String shardingTarget = calculateShardingTarget(colum);  
                result.add(shardingTarget);  
            }  
            return getMatchedTables(result, availableTargetNames);  
        }  
          
        // 如果主分片列没有有效值，则使用其他列进行分片  
        complexKeysShardingValue.getColumnNameAndShardingValuesMap().remove(mainColum);  
        Collection<String> otherColums = complexKeysShardingValue.getColumnNameAndShardingValuesMap().keySet();  
        if (CollectionUtils.isNotEmpty(otherColums)) {  
            for (String colum : otherColums) {  
                Collection<String> otherColumValues = complexKeysShardingValue.getColumnNameAndShardingValuesMap().get(colum);  
                for (String value : otherColumValues) {  
                    String shardingTarget = extractShardingTarget(value);  
                    result.add(shardingTarget);  
                }  
            }  
            return getMatchedTables(result, availableTargetNames);  
        }  
          
        return null;  
    }  
      
    // 根据结果集和可用的目标名筛选匹配的表  
    private Collection<String> getMatchedTables(Collection<String> results, Collection<String> availableTargetNames) {  
        Collection<String> matchedTables = new HashSet<>();  
        for (String result : results) {  
            matchedTables.addAll(availableTargetNames.parallelStream().filter(each -> each.endsWith(result)).collect(Collectors.toSet()));  
        }  
        return matchedTables;  
    }  
      
    // 根据订单ID提取分片目标  
    private String extractShardingTarget(String orderId) {  
        return DistributeID.getShardingTable(orderId);  
    }  
      
    // 根据买家ID计算分片目标，考虑表的数量  
    private String calculateShardingTarget(String buyerId) {  
        String tableCount = props.getProperty(PROP_TABLE_COUNT);  
        return DistributeID.getShardingTable(buyerId, Integer.parseInt(tableCount));  
    }  
      
}
```



## 2. ChatGPT 配置项解释

### 2.1 `actual-data-nodes`

- **作用**：指定真实的数据节点。`${0..3}`是占位符，表示会有4个节点，分别是`ds.trade_order_0000`、`ds.trade_order_0001`、`ds.trade_order_0002`、`ds.trade_order_0003`。

### 2.2 `keyGenerateStrategy`

- **作用**：指定主键生成策略。
- **配置**：`column`指定主键列名为`id`，`keyGeneratorName`指定使用的主键生成器名称为`snowflake`。

### 2.3 `table-strategy`

- **作用**：指定分片策略。
- **配置**：
    - `complex`：表示使用复合分片策略。
    - `shardingColumns`：指定分片列，这里是`buyer_id`和`order_id`。
    - `shardingAlgorithmName`：指定分片算法名称为`trade-order-sharding`。

### 2.4 `shardingAlgorithms`

- **作用**：定义分片算法。
- **配置**：
    - `trade-order-sharding`：算法名称为`trade-order-sharding`。
    - `type`：指定算法类型为`CLASS_BASED`，即基于类的分片算法。
    - `algorithmClassName`：指定分片算法类的全限定名为`cn.hollis.nft.turbo.order.infrastructure.sharding.algorithm.TurboKeyShardingAlgorithm`。
    - `strategy`：指定分片策略为`complex`。
    - `tableCount`：指定分表数量为4。
    - `mainColum`：指定主要的分片列为`buyer_id`（注意这里可能是一个拼写错误，应该是`mainColumn`）。

### 2.5 `keyGenerators`

- **作用**：定义主键生成器。
- **配置**：
    - `snowflake`：主键生成器名称为`snowflake`。
    - `type`：指定主键生成器类型为`SNOWFLAKE`，即雪花算法。

### 2.6 `auditors`

- **作用**：定义审计规则。
- **配置**：
    - `sharding_key_required_auditor`：审计器名称为`sharding_key_required_auditor`。
    - `type`：指定审计器类型为`DML_SHARDING_CONDITIONS`，即DML操作的分片条件审计。

### 2.7. 总结

该配置定义了两个表`trade_order`和`trade_order_stream`的分片策略，使用了复合分片策略，并通过自定义的分片算法`TurboKeyShardingAlgorithm`来实现分片逻辑。同时，配置了主键生成策略为雪花算法。这样的配置可以帮助实现高效的数据分片，提高系统的可扩展性和性能。


## 3. ChatGPT `TurboKeyShardingAlgorithm` 类方法作用解释

`TurboKeyShardingAlgorithm` 类实现了 `ComplexKeysShardingAlgorithm<String>` 和 `HintShardingAlgorithm<String>` 接口，用于在 ShardingSphere 框架中实现自定义的分片算法。以下是该类中所有方法的作用解释：

### 1. `getProps()`

- **作用**：返回分片算法的配置属性。
- **实现**：该方法直接返回类成员变量 `props`，该变量存储了分片算法的配置信息。

### 2. `init(Properties props)`

- **作用**：初始化分片算法的配置属性。
- **实现**：该方法接收一个 `Properties` 类型的参数 `props`，并将其赋值给类成员变量 `props`，以便在分片算法中使用。

### 3. `doSharding(Collection<String> availableTargetNames, ComplexKeysShardingValue<String> complexKeysShardingValue)`

- **作用**：根据复合分片键的值，从可用的目标表名集合中选择匹配的表名。
- **实现**：该方法首先获取分片键的值，然后根据分片键的值计算目标表名，并从可用的目标表名集合中选择匹配的表名返回。如果分片键不存在或无法匹配到目标表名，则返回 `null`。

### 4. `getMatchedTables(Collection<String> results, Collection<String> availableTargetNames)`

- **作用**：从可用的目标表名集合中选择与给定结果集合匹配的表名。
- **实现**：该方法遍历结果集合，使用并行流过滤出以结果集合中元素为后缀的目标表名，并返回匹配的表名集合。

### 5. `extractShardingTarget(String orderId)`

- **作用**：根据订单ID计算分片目标。
- **实现**：该方法调用 `DistributeID.getShardingTable(orderId)` 方法，根据订单ID计算分片目标表名。

### 6. `calculateShardingTarget(String buyerId)`

- **作用**：根据买家ID和表数量计算分片目标。
- **实现**：该方法首先从配置属性中获取表数量，然后调用 `DistributeID.getShardingTable(buyerId, tableCount)` 方法，根据买家ID和表数量计算分片目标表名。

### 7. `doSharding(Collection<String> collection, HintShardingValue<String> hintShardingValue)`

- **作用**：根据Hint分片值，从给定的表名集合中选择匹配的表名。
- **实现**：该方法首先记录日志信息，然后根据Hint分片值计算匹配的表名，并从给定的表名集合中选择匹配的表名返回。如果找不到匹配的表名，则返回空集合。

综上所述，`TurboKeyShardingAlgorithm` 类通过实现自定义的分片算法，为ShardingSphere框架提供了灵活的分片策略，使得数据库能够更高效地处理大规模数据。


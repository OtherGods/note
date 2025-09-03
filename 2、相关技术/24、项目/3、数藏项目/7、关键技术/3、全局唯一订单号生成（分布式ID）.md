在我们的项目中，我们需要定义一些全局唯一的 ID，比如订单号，支付单号等等。这些ID有以下几个基本要求：
**1、不能重复**
**2、不可被预测**
**3、能适应分库分表**

为了生成一个这样一个全局的订单号，我自定义了一个分布式 ID 生成器，其中包含了三部分内容，分别是系统标识码、表下标以及序列化。

其中系统标识码用于区分具体是什么业务（如交易订单、库存、积分等），表下标为了兼容分库分表的场景，序列号采用雪花算法生成。最终生成的订单号形如：`10（业务码） 1769649671860822016（sequence) 1023(分表）`

业务码用来区分具体的业务，如交易订单、支付单等，然后雪花算法用来保证唯一性，分表信息用来实现分表后的订单号查询。具体参考：
[1、基于基因法实现可基于订单号路由到单表](2、相关技术/24、项目/3、数藏项目/8、最佳实践/10、分库分表(ShardingJDBC)/1、基于基因法实现可基于订单号路由到单表.md)

```java
package cn.hollis.nft.turbo.datasource.sharding.id;  
  
import cn.hollis.nft.turbo.datasource.sharding.strategy.DefaultShardingTableStrategy;  
import cn.hutool.core.util.IdUtil;  
import org.apache.commons.lang3.StringUtils;  
  
/**  
 * @author Hollis * <p>  
 * 分布式ID  
 */
 public class DistributeID {  
      
    /**  
     * 系统标识码，用于区分不同的业务  
     */  
    private String businessCode;  
      
    /**  
     * 表下标，用于分表  
     */  
    private String table;  
      
    /**  
     * 序列号  
     */  
    private String seq;  
      
    /**  
     * 分表策略，默认为DefaultShardingTableStrategy  
     */
     private static DefaultShardingTableStrategy shardingTableStrategy = new DefaultShardingTableStrategy();  
      
    public DistributeID() {  
    }  
      
    /**  
     * 利用雪花算法生成一个唯一ID  
     */
     public static String generateWithSnowflake(BusinessCode businessCode, String externalId) {  
        //利用雪花算法生成一个唯一ID  
        long id = IdUtil.getSnowflake(businessCode.code()).nextId();  
        //调用 generate(businessCode, externalId, id) 方法生成格式化后的ID。  
        return generate(businessCode, externalId, id);  
    }  
      
    /**  
     * 生成一个唯一ID：10（业务码） 1769649671860822016（sequence) 1023(分表）  
     */  
    public static String generate(BusinessCode businessCode, String externalId, Long sequenceNumber) {  
        //创建一个 DistributeID 实例，并将业务码、序列号和表下标拼接成一个唯一的ID。
        DistributeID distributeId = create(businessCode, externalId, sequenceNumber);  
        return distributeId.businessCode + distributeId.seq + distributeId.table;  
    }  
      
    @Override  
    public String toString() {  
        return this.businessCode + this.seq + this.table;  
    }  
      
    public static DistributeID create(BusinessCode businessCode,  
                                      String externalId, Long sequenceNumber) {  
        //创建 DistributeID 实例并设置其字段。  
        DistributeID distributeId = new DistributeID();  
        distributeId.businessCode = businessCode.getCodeString();  
        //通过 shardingTableStrategy 的 getTable 方法计算并填充四位的表下标。
        String table = String.valueOf(shardingTableStrategy.getTable(externalId, businessCode.tableCount()));  
        distributeId.table = StringUtils.leftPad(table, 4, "0");  
        distributeId.seq = String.valueOf(sequenceNumber);  
        return distributeId;  
    }  
      
    public static String getShardingTable(DistributeID distributeId){  
        return distributeId.table;  
    }  
      
    public static String getShardingTable(String externalId, int tableCount) {  
        return StringUtils.leftPad(String.valueOf(shardingTableStrategy.getTable(externalId, tableCount)), 4, "0");  
    }  
      
    public static String getShardingTable(String id){  
        return getShardingTable(valueOf(id));  
    }  
      
    public static DistributeID valueOf(String id) {  
        DistributeID distributeId = new DistributeID();  
        distributeId.businessCode = id.substring(0, 1);  
        distributeId.seq = id.substring(1, id.length() - 4);  
        distributeId.table = id.substring(id.length() - 4, id.length());  
        return distributeId;  
    }  
}
```

这里的雪花算法，采用IdUtil.getSnowflake实现的。有了这个工具之后，需要一个分布式 ID 的时候，只需要如下调用即可：
```java
String orderId = DistributeID.generateWithSnowflake(BusinessCode.TRADE_ORDER, "传一个 workerId", request.getBuyerId());
```

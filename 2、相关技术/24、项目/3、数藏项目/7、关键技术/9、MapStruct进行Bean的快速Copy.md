项目中，经常有各种参数和对象之间的转换，尤其是 DO 转 DTO、转 VO，或者 Request 直接转成Entity 等，有很多用法，我们选择的是MapStruct，因为他性能最好。

需要在 pom 中增加mapstruct和mapstruct-processor：
```java
<?xml version="1.0" encoding="UTF-8"?>  
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">  
    <modelVersion>4.0.0</modelVersion>  
  
    <dependencies>  
        
        <dependency>  
            <groupId>org.mapstruct</groupId>  
            <artifactId>mapstruct</artifactId>  
            <version>1.6.0.Beta1</version>  
        </dependency>  
  
    </dependencies>  
  
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
                        <path>  
                            <groupId>org.mapstruct</groupId>  
                            <artifactId>mapstruct-processor</artifactId>  
                            <version>1.5.5.Final</version>  
                        </path>  
                    </annotationProcessorPaths>  
                </configuration>  
            </plugin>  
  
        </plugins>  
  
  
</project>
```

加下来定义一个 Convertor：
```java
package cn.hollis.nft.turbo.order.domain.entity.convertor;  
  
import cn.hollis.nft.turbo.api.order.model.TradeOrderVO;  
import cn.hollis.nft.turbo.api.order.request.OrderCreateRequest;  
import cn.hollis.nft.turbo.order.domain.entity.TradeOrder;  
import org.mapstruct.Mapper;  
import org.mapstruct.Mapping;  
import org.mapstruct.NullValueCheckStrategy;  
import org.mapstruct.factory.Mappers;  
  
import java.util.List;  
  
/**  
 * @author Hollis
*/
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)  
public interface TradeOrderConvertor {  
      
    TradeOrderConvertor INSTANCE = Mappers.getMapper(TradeOrderConvertor.class);  
      
    public TradeOrder mapToEntity(OrderCreateRequest request);  

	// isTimeout是TradeOrder中的方法，在TradeOrder类中如果没有timeout字段会在isTimeout上加@JSONField(serialize = false)注解，告诉fastjson在序列化时不需要关心timeout字段 
    @Mapping(target = "timeout", expression = "java(request.isTimeout())")  
    public TradeOrderVO mapToVo(TradeOrder request);  
      
    public List<TradeOrderVO> mapToVo(List<TradeOrder> request);  
}
```

这里用到@Mapper和@Mapping注解（这里的Mapper 和 mybatis 的 Mapper 不要弄混了！）

这里定义了OrderCreateRequest转TradeOrder的方法、TradeOrder转TradeOrderVO的方法。

接下来，使用方式也非常简单：
```java
public static TradeOrder createOrder(OrderCreateRequest request) {  
    TradeOrder tradeOrder = TradeOrderConvertor.INSTANCE.mapToEntity(request);  
    tradeOrder.setOrderState(TradeOrderState.CREATE);  
    tradeOrder.setPaidAmount(BigDecimal.ZERO);  
    String orderId = DistributeID.generateWithSnowflake(BusinessCode.TRADE_ORDER, request.getBuyerId());  
    tradeOrder.setOrderId(orderId);  
    return tradeOrder;  
}
```

如上，`TradeOrderConvertor.INSTANCE.mapToEntity(request);`就可以完成参数填充。

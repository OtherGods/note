Java 14 中便包含了一个新特性：EP 359: Records，

Records的目标是扩展Java语言语法，Records为声明类提供了一种紧凑的语法，用于创建一种类中是“字段，只是字段，除了字段什么都没有”的类。

通过对类做这样的声明，编译器可以通过自动创建所有方法并让所有字段参与hashCode()等方法。这是JDK 14中的一个预览特性。

我们的项目中也用到了这个特性，比如说我们的藏品管理部分，针对我们的藏品的库存相关操作，我们定义了 Try、Confirm 以及 Cancel 三种操作：
```java
/**  
 * 尝试售卖  
 *  
 * @param request  
 * @return  
 */
 public Boolean trySale(CollectionTrySaleRequest request);  
  
/**  
 * 取消售卖  
 *  
 * @param request  
 * @return  
 */
 public Boolean cancelSale(CollectionCancelSaleRequest request);  
  
/**  
 * 确认售卖  
 *  
 * @param request  
 * @return  
 */
 public CollectionConfirmSaleResponse confirmSale(CollectionConfirmSaleRequest request);
```

这里面定义了三个方法，并且定义了三个入参的参数，分别是CollectionTrySaleRequest、CollectionCancelSaleRequest以及CollectionConfirmSaleRequest。

这三个类其实他们的作用很简单，只是用于参数传递的，没有其他的业务语义及功能，于是我们就把他们定义成了 record 类型：
```java
public record CollectionTrySaleRequest(String identifier, Long collectionId, Long quantity) {  
      
    public CollectionEvent eventType() {  
        return CollectionEvent.TRY_SALE;  
    }  
}
```

```java
public record CollectionCancelSaleRequest(String identifier, Long collectionId,Long quantity) {  
      
    public CollectionEvent eventType() {  
        return CollectionEvent.CANCEL_SALE;  
    }  
}
```

```java
public record CollectionConfirmSaleRequest(String identifier, Long collectionId, Long quantity) {  
      
    public CollectionEvent eventType() {  
        return CollectionEvent.CONFIRM_SALE;  
    }  
}
```

这里就通过 record 定义了简单的记录，用于参数传递。


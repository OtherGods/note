在JDK 12中引入了Switch表达式作为预览特性。并在Java 13中修改了这个特性，引入了yield语句，用于返回值。

而在之后的Java 14中，这一功能正式作为标准功能提供出来。

在以前，我们想要在switch中返回内容，还是比较麻烦的，一般语法如下：
```java
int i;  
switch (x) {  
    case "1":  
        i=1;  
        break;  
    case "2":  
        i=2;  
        break;  
    default:  
        i = x.length();  
        break;  
}
```

在JDK13中使用以下语法：
```java
int i = switch (x) {  
    case "1" -> 1;  
    case "2" -> 2;  
    default -> {  
        int len = args[1].length();  
        yield len;  
    }  
};
```

或者
```java
int i = switch (x) {  
    case "1": yield 1;  
    case "2": yield 2;  
    default: {  
        int len = args[1].length();  
        yield len;  
    }  
};
```

在这之后，switch中就多了一个关键字用于跳出switch块了，那就是yield，他用于返回一个值。

和return的区别在于：return会直接跳出当前循环或者方法，而yield只会跳出当前switch块。

我们的项目中，也用到了这个语法，比如在GoodsFacadeServiceImpl中：
```java
@Override  
public BaseGoodsVO getGoods(String goodsId, GoodsType goodsType) {  
    return switch (goodsType) {  
        case COLLECTION -> {  
            SingleResponse<CollectionVO> response = collectionReadFacadeService.queryById(Long.valueOf(goodsId));  
            if (response.getSuccess()) {  
                yield response.getData();  
            }  
            yield null;  
        }  
          
        case BLIND_BOX -> {  
            SingleResponse<BlindBoxVO> response = blindBoxReadFacadeService.queryById(Long.valueOf(goodsId));  
            if (response.getSuccess()) {  
                yield response.getData();  
            }  
            yield null;  
        }  
        default -> throw new UnsupportedOperationException("unsupport goods type");  
    };  
}
```

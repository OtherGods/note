一般在金融业务中，钱涉及到金额、币种、单位，需要特殊处理，所以有的时候会单独定义一个 Money 类，本来我也想这么定义来着，但是后来觉得太麻烦了。

在我们的项目中其实还不涉及到多币种，所以只涉及到单位的转换，比如我们的系统都是用BigDecimal 表示金额的，但是支付渠道可能用 Long 的，所以需要互相转换。

为了简单，定义了个工具类，实现互相转换。目前还是很简单的，后续会把他扩展成Money 类
```java
package cn.hollis.nft.turbo.base.utils;  
  
import java.math.BigDecimal;  
import java.math.RoundingMode;  
  
/**  
 * @author Hollis 
 * /
public class MoneyUtils {  
      
    /**  
     * 元转分  
     *  
     * @param number     
     * @return     
     */
     public static Long yuanToCent(BigDecimal number) {
        return number.multiply(new BigDecimal("100")).longValue();  
    }  
      
    /**  
     * 分转元  
     *  
     * @param number  
     * @return  
     */    
     public static BigDecimal centToYuan(Long number) {
        if (number == null) {
            return null;  
        }
        return new BigDecimal(number.toString()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);  
    }  
}
```











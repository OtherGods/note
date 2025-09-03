我们的代码中，有很多 RPC 的调用，然后为了对 RPC 的整体异常处理，结果包装，我们定义了一个 AOP。

首先定义了一个注解 @Facade
```java
package cn.hollis.nft.turbo.rpc.facade;  
/**  
 * @author Hollis */
public @interface Facade {  
}
```

然后实现他的处理逻辑：
```java
package cn.hollis.nft.turbo.rpc.facade;  
  
/**  
 * Facade的切面处理类，统一统计进行参数校验及异常捕获  
 *  
 * @author Hollis 
 */
@Aspect
@Component
public class FacadeAspect {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FacadeAspect.class);  
      
    @Around("@annotation(cn.hollis.nft.turbo.rpc.facade.Facade)")  
    public Object facade(ProceedingJoinPoint pjp) throws Exception {  
          
        StopWatch stopWatch = new StopWatch();  
        stopWatch.start();  
          
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();  
        Object[] args = pjp.getArgs();  
        LOGGER.info("start to execute , method = " + method.getName() + " , args = " + JSON.toJSONString(args));  
          
        Class returnType = ((MethodSignature) pjp.getSignature()).getMethod().getReturnType();  
          
        //循环遍历所有参数，进行参数校验  
        for (Object parameter : args) {  
            try {  
                BeanValidator.validateObject(parameter);  
            } catch (ValidationException e) {  
                printLog(stopWatch, method, args, "failed to validate", null, e);  
                return getFailedResponse(returnType, e);  
            }  
        }  
          
        //省略其他代码  
    }  
}
```

[1、StopWatch介绍](2、相关技术/24、项目/3、数藏项目/工具类/1、StopWatch介绍.md)

在这个切面中 ，针对参数做循环遍历，然后可通过我们的BeanValidator.validateObject进行参数校验。如果校验失败，则直接返回失败。

这里的 BeanValidator 的代码如下：
```java
package cn.hollis.nft.turbo.rpc.facade;  
  
import cn.hollis.nft.turbo.base.response.BaseResponse;  
import cn.hollis.nft.turbo.base.response.ResponseCode;  
import cn.hollis.nft.turbo.base.utils.BeanValidator;  
import com.alibaba.fastjson2.JSON;  
import jakarta.validation.ValidationException;  
import org.apache.commons.lang3.StringUtils;  
import org.apache.commons.lang3.time.StopWatch;  
import org.aspectj.lang.ProceedingJoinPoint;  
import org.aspectj.lang.annotation.Around;  
import org.aspectj.lang.annotation.Aspect;  
import org.aspectj.lang.reflect.MethodSignature;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
import org.springframework.stereotype.Component;  
  
import java.lang.reflect.InvocationTargetException;  
import java.lang.reflect.Method;  
import java.util.Arrays;  
  
/**  
 * Facade的切面处理类，统一统计进行参数校验及异常捕获  
 *  
 * @author Hollis 
 */
@Aspect  
@Component  
public class FacadeAspect {  
      
    private static final Logger LOGGER = LoggerFactory.getLogger(FacadeAspect.class);  
      
    @Around("@annotation(cn.hollis.nft.turbo.rpc.facade.Facade)")  
    public Object facade(ProceedingJoinPoint pjp) throws Exception {  
          
        StopWatch stopWatch = new StopWatch();  
        stopWatch.start();  
          
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();  
        Object[] args = pjp.getArgs();  
        LOGGER.info("start to execute , method = " + method.getName() + " , args = " + JSON.toJSONString(args));  
          
        Class returnType = ((MethodSignature) pjp.getSignature()).getMethod().getReturnType();  
          
        //循环遍历所有参数，进行参数校验
        for (Object parameter : args) {  
            try {  
                BeanValidator.validateObject(parameter);  
            } catch (ValidationException e) {  
                printLog(stopWatch, method, args, "failed to validate", null, e);  
                return getFailedResponse(returnType, e);  
            }  
        }  
          
        try {  
            // 目标方法执行  
            Object response = pjp.proceed();  
            enrichObject(response);  
            printLog(stopWatch, method, args, "end to execute", response, null);  
            return response;  
        } catch (Throwable throwable) {  
            // 如果执行异常，则返回一个失败的response  
            printLog(stopWatch, method, args, "failed to execute", null, throwable);  
            return getFailedResponse(returnType, throwable);  
        }  
    }  
      
    /**  
     * 日志打印  
     *  
     * @param stopWatch     
     * @param method     
     * @param args     
     * @param action     
     * @param response     
     */ 
    private void printLog(StopWatch stopWatch, Method method, Object[] args, String action, Object response,  
                          Throwable throwable) {  
        try {  
            //因为此处有JSON.toJSONString，可能会有异常，需要进行捕获，避免影响主干流程  
            LOGGER.info(getInfoMessage(action, stopWatch, method, args, response, throwable), throwable);  
            // 如果校验失败，则返回一个失败的response  
        } catch (Exception e1) {  
            LOGGER.error("log failed", e1);  
        }  
    }  
      
    /**  
     * 统一格式输出，方便做日志统计  
     * <p>  
     * *** 如果调整此处的格式，需要同步调整日志监控 ***  
     *     * @param action    行为  
     * @param stopWatch 耗时  
     * @param method    方法  
     * @param args      参数  
     * @param response  响应  
     * @return 拼接后的字符串  
     */  
    private String getInfoMessage(String action, StopWatch stopWatch, Method method, Object[] args, Object response,  
                                  Throwable exception) {  
          
        StringBuilder stringBuilder = new StringBuilder(action);  
        stringBuilder.append(" ,method = ");  
        stringBuilder.append(method.getName());  
        stringBuilder.append(" ,cost = ");  
        stringBuilder.append(stopWatch.getTime()).append(" ms");  
        if (response instanceof BaseResponse) {  
            stringBuilder.append(" ,success = ");  
            stringBuilder.append(((BaseResponse) response).getSuccess());  
        }  
        if (exception != null) {  
            stringBuilder.append(" ,success = ");  
            stringBuilder.append(false);  
        }  
        stringBuilder.append(" ,args = ");  
        stringBuilder.append(JSON.toJSONString(Arrays.toString(args)));  
          
        if (response != null) {  
            stringBuilder.append(" ,resp = ");  
            stringBuilder.append(JSON.toJSONString(response));  
        }  
          
        if (exception != null) {  
            stringBuilder.append(" ,exception = ");  
            stringBuilder.append(exception.getMessage());  
        }  
          
        if (response instanceof BaseResponse) {  
            BaseResponse baseResponse = (BaseResponse) response;  
            if (!baseResponse.getSuccess()) {  
                stringBuilder.append(" , execute_failed");  
            }  
        }  
          
        return stringBuilder.toString();  
    }  
      
    /**  
     * 将response的信息补全，主要是code和message  
     *     * @param response     
     */    
    private void enrichObject(Object response) {  
        if (response instanceof BaseResponse) {  
            if (((BaseResponse) response).getSuccess()) {  
                //如果状态是成功的，需要将未设置的responseCode设置成SUCCESS  
                if (StringUtils.isEmpty(((BaseResponse) response).getResponseCode())) {  
                    ((BaseResponse) response).setResponseCode(ResponseCode.SUCCESS.name());  
                }  
            } else {  
                //如果状态是成功的，需要将未设置的responseCode设置成BIZ_ERROR  
                if (StringUtils.isEmpty(((BaseResponse) response).getResponseCode())) {  
                    ((BaseResponse) response).setResponseCode(ResponseCode.BIZ_ERROR.name());  
                }  
            }  
        }  
    }  
      
    /**  
     * 定义并返回一个通用的失败响应  
     */  
    private Object getFailedResponse(Class returnType, Throwable throwable)  
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {  
          
        //如果返回值的类型为BaseResponse 的子类，则创建一个通用的失败响应  
        if (returnType.getDeclaredConstructor().newInstance() instanceof BaseResponse) {  
            BaseResponse response = (BaseResponse) returnType.getDeclaredConstructor().newInstance();  
            response.setSuccess(false);  
            response.setResponseMessage(throwable.toString());  
            response.setResponseCode(ResponseCode.BIZ_ERROR.name());  
            return response;  
        }  
          
        LOGGER.error(  
                "failed to getFailedResponse , returnType (" + returnType + ") is not instanceof BaseResponse");  
        return null;  
    }  
}
```

这里对异常进行了统一的 try-catch，确保我们的RPC 接口不会返回异常，而是统一以错误码的形式返回。并且这里进行了统一的参数校验和日志打印，就不展开说了。
[7、统一入参合法性校验](2、相关技术/24、项目/3、数藏项目/6、通用设计/7、统一入参合法性校验.md)

用法如下：
```java
/**  
 * @author Hollis 
 */
@DubboService(version = "1.0.0")  
public class OrderFacadeServiceImpl implements OrderFacadeService {  
      
      
    @Override  
    @Facade  
    public OrderResponse create(OrderCreateRequest request) {  
        Boolean preDeductResult = inventoryWrapperService.preDeduct(request);  
        if (preDeductResult) {  
            return orderService.create(request);  
        }  
        throw new OrderException(OrderErrorCode.INVENTORY_DEDUCT_FAILED);  
    }  
}
```

在方法上增加 @Facade 注解即可。

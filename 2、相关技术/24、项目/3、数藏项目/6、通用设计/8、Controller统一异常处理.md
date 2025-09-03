我们的项目中基于@ControllerAdvice 实现了统一异常处理，针对 Controller层的异常做了统一的包装和处理。如以下，是我们定义的全局异常处理器：
```java
package cn.hollis.nft.turbo.web.handler;  
  
import cn.hollis.nft.turbo.base.exception.BizException;  
import cn.hollis.nft.turbo.base.exception.SystemException;  
import cn.hollis.nft.turbo.web.vo.Result;  
import com.google.common.collect.Maps;  
import org.springframework.http.HttpStatus;  
import org.springframework.validation.FieldError;  
import org.springframework.web.bind.MethodArgumentNotValidException;  
import org.springframework.web.bind.annotation.ControllerAdvice;  
import org.springframework.web.bind.annotation.ExceptionHandler;  
import org.springframework.web.bind.annotation.ResponseBody;  
import org.springframework.web.bind.annotation.ResponseStatus;  
  
import java.util.Map;  
  
/**  
 * @author Hollis 
 */
@ControllerAdvice  
public class GlobalWebExceptionHandler {  
      
    /**  
     * 自定义方法参数校验异常处理器  
     *  
     * @param ex     * @return     
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)  
    @ResponseStatus(HttpStatus.BAD_REQUEST)  
    @ResponseBody  
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {  
        Map<String, String> errors = Maps.newHashMapWithExpectedSize(1);  
        ex.getBindingResult().getAllErrors().forEach((error) -> {  
            String fieldName = ((FieldError) error).getField();  
            String errorMessage = error.getDefaultMessage();  
            errors.put(fieldName, errorMessage);  
        });  
        return errors;  
    }  
      
    /**  
     * 自定义业务异常处理器  
     *  
     * @param bizException     
     * @return     
     */
    @ExceptionHandler(BizException.class)  
    @ResponseStatus(HttpStatus.OK)  
    @ResponseBody  
    public Result exceptionHandler(BizException bizException) {  
        Result result = new Result();  
        result.setCode(bizException.getErrorCode().getCode());  
        result.setMessage(bizException.getErrorCode().getMessage());  
        result.setSuccess(false);  
        return result;  
    }  
      
    /**  
     * 自定义系统异常处理器  
     *  
     * @param systemException  
     * @return  
     */    
    @ExceptionHandler(SystemException.class)  
    @ResponseStatus(HttpStatus.OK)  
    @ResponseBody  
    public Result systemExceptionHandler(SystemException systemException) {  
        Result result = new Result();  
        result.setCode(systemException.getErrorCode().getCode());  
        result.setMessage(systemException.getErrorCode().getMessage());  
        result.setSuccess(false);  
        return result;  
    }  
}
```

然后需要注意，需要让 SpringBoot 扫描到我们定义的全局异常处理器：
```java
@SpringBootApplication(scanBasePackages = {"cn.hollis.nft.turbo.auth","cn.hollis.nft.turbo.base"})  
@MapperScan(basePackages = "cn.hollis.nft.turbo.auth")  
public class NfTurboAuthApplication {  
      
    public static void main(String[] args) {  
        SpringApplication.run(NfTurboAuthApplication.class, args);  
    }  
}
```

这里面针对MethodArgumentNotValidException、BizException以及SystemException分别做了统一的拦截和处理，把他们做转换，返回一个前端可以识别的 Result 类型。

这里面的MethodArgumentNotValidException主要是用来给入参做合法性校验的，比如必填、长度、大小等，这里以后借助 validation 框架实现的，首先需要引入：
```java
<!-- validation -->  
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-validation</artifactId>  
    <version>3.2.1</version>  
</dependency>
```

然后在 controller 中，定义一个方法，并且在入参中使用@Valid 注解对他进行校验。
```java
@Slf4j  
@RequiredArgsConstructor  
@RestController  
@RequestMapping("auth")  
public class AuthController {  
    /**  
     * 登录方法  
     *  
     * @param loginParam 登录信息  
     * @return 结果  
     */  
    @PostMapping("/login")  
    public String login(@Valid @RequestBody LoginParam loginParam) {  
        StpUtil.login("1222233", 300);  
        return "hello world";  
    }  
}
```

用了`@Valid`之后，会针对LoginParam里面的参数进行合法性校验，具体的校验内容包括手机号不能为空、验证码不能为空等。具体LoginParam内容如下：
```java
@Setter  
@Getter  
public class LoginParam extends RegisterParam {  
      
    /**  
     * 记住我  
     */  
    private Boolean rememberMe;  
}  
  
@Setter  
@Getter  
public class RegisterParam {  
      
    /**  
     * 手机号  
     */  
    @NotBlank(message = "手机号不能为空")  
    private String telephone;  
      
    /**  
     * 验证码  
     */  
    @NotBlank(message = "验证码不能为空")  
    private String captcha;  
}
```

接下来写个单元测试验证一下：
```java
package auth;  
  
import cn.hollis.nft.turbo.auth.controller.AuthController;  
import cn.hollis.nft.turbo.auth.param.LoginParam;  
import com.alibaba.fastjson2.JSON;  
import org.hamcrest.core.Is;  
import org.junit.Test;  
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;  
import org.springframework.http.MediaType;  
import org.springframework.test.web.servlet.MockMvc;  
import org.springframework.test.web.servlet.MvcResult;  
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;  
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;  
  
/**  
 * @author Hollis */@AutoConfigureMockMvc  
public class AuthControllerTest extends AuthBaseTest {  
      
    @Autowired  
    AuthController userController;  
      
    @Autowired  
    private MockMvc mockMvc;  
      
    @Test  
    public void whenPostLoginAndInValidUser_thenCorrectResponse() throws Exception {  
        LoginParam loginParam = new LoginParam();  
        loginParam.setTelephone("13555555555");  
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")  
                        .content(JSON.toJSONString(loginParam))  
                        .contentType(MediaType.APPLICATION_JSON_UTF8))  
                .andExpect(MockMvcResultMatchers.jsonPath("$.captcha", Is.is("验证码不能为空")))  
                .andExpect(MockMvcResultMatchers.status().isBadRequest()).andReturn();  
          
    }  
}
```

单测通过：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202412302333760.png)

这里用了个MockMvc，是专门用来对 Controller 进行单测的，大家也可以用起来。就不展开介绍了。
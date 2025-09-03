

[TOC]

虽然 spring security 已经提供了比较完善的加密机制，但是有时根据业务需求需要定制自己的加密方式。

spring security 提供了加密扩充的接口，下文主要介绍如何在 spring security 中添加自定义的加密器。

## 1.MD5加密工具类

```java
package com.bruce.utils;

import java.security.MessageDigest;

/**
 * @program: Student_dorm_System
 * @description: MD5加密
 * @create: 2019-04-13 13:04
 **/
public class MD5Util {

    private static String byteArrayToHexString(byte b[]) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++)
            resultSb.append(byteToHexString(b[i]));

        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0)
            n += 256;
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    /**
     * 返回大写MD5
     *
     * @param origin
     * @param charsetname
     * @return
     */
    private static String MD5Encode(String origin, String charsetname) {
        String resultString = null;
        try {
            resultString = new String(origin);
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (charsetname == null || "".equals(charsetname))
                resultString = byteArrayToHexString(md.digest(resultString.getBytes()));
            else
                resultString = byteArrayToHexString(md.digest(resultString.getBytes(charsetname)));
        } catch (Exception exception) {
        }
        return resultString.toUpperCase();
    }

    public static String MD5EncodeUtf8(String origin) {

        //盐值Salt加密
        //origin = origin + PropertiesUtil.getProperty("password.salt", "");
        return MD5Encode(origin, "utf-8");
    }


    private static final String hexDigits[] = {"0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};


    public static void main(String[] args) {
        String slat="2105";
        String s = MD5EncodeUtf8(MD5EncodeUtf8("123456"+slat));
        System.out.println("加密之后："+s);
    }

}


```

## 2.自定义加密器
实现接口 `PasswordEncoder`

```java
package com.bruce.encode;

import com.bruce.utils.MD5Util;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component("mpasswordEncoder")
public class MPasswordEncoder implements PasswordEncoder {

    /**
     * 对密码进行加密并返回
     */
    public String encode(CharSequence rawPassword) {
        String slat="2105";
        String encPassword =  MD5Util.MD5EncodeUtf8(MD5Util.MD5EncodeUtf8(rawPassword+slat));
        return encPassword;
    }

    /**
     * 验证密码是否正确
     */
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}
```
注意：
- `encode(rawPassword)`方法用来进行加密，其参数`rawPassword` 表示从表单提交上来的密码。
- `mathes(rawPassword,encodedPassword)` 方法进行密码匹配，其参数`rawPassword` 表示从表单提交上来的密码，在比较密码之前应该调用encode()方法对`rawPassword`进行加密；参数`encodedPassword`表示从数据库或者其他方式获取到的服务器上存储的密码，用来和用户提交上来的密码进行比较。

## 3.修改 security 配置文件

修改 security.xml 文件，加载并引用自定义的加密器。

```xml
    <!--设置Spring Security认证用户信息的来源-->
    <security:authentication-manager>
        <security:authentication-provider user-service-ref="userServiceImpl">
<!--            <security:user-service>-->
<!--                <security:user name="user" password="{noop}user" authorities="ROLE_USER"/>-->
<!--                <security:user name="admin" password="{noop}admin" authorities="ROLE_ADMIN"/>-->
<!--            </security:user-service>-->
            <security:password-encoder ref="mpasswordEncoder"/>
        </security:authentication-provider>
    </security:authentication-manager>
```
- `<password-encoder ref="mpasswordEncoder" />` 表示引用自定义的加密器。

至此，就可以用新的密码加密方式进行测试了！

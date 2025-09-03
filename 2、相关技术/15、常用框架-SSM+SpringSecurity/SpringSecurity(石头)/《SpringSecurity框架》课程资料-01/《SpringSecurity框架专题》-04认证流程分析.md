

[TOC]

我们前面实现了使用自定义认证界面的功能，但是后台认证校验还是使用的’/login’来处理的，对比的账号密码还是我们写在内存的数据，那我们如果想要实现和数据库中的数据比较，那么我们就必须要实现自定义认证逻辑的实现，本文我们就先来分析下系统自带的认证是怎么走的。
## 1.UsernamePasswordAuthenticationFilter
系统认证是通过`UsernamePasswordAuthenticationFilter` 过滤器实现的，所以我们需要来分析下这个过滤器的源码
### 1.1.表单提交参数
![在这里插入图片描述](https://img-blog.csdnimg.cn/fc5fd0716fea450eba5c1612b1ec9032.png)
### 1.2.doFilter方法
因为`UsernamePasswordAuthenticationFilter`就是一个过滤器，所以我们要分析他的原理，肯定需要通过doFilter方法来开始，注意该方法在父类中。

![在这里插入图片描述](https://img-blog.csdnimg.cn/5ea713593e9040d68e165c376ea1b18b.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/7d384dfdfd574983a08d2a9350bd546e.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/2bf2284a2f4b453c9a799a53ceff222c.png)
通过上面我们发现，要查看认证的流程我们需要进入`attemptAuthentication`方法中查看
## 2.认证的过程
![在这里插入图片描述](https://img-blog.csdnimg.cn/ea9dae011afd41a6bcd38be172fc39f4.png)

进入`this.getAuthenticationManager().authenticate(authRequest)`;中
![在这里插入图片描述](https://img-blog.csdnimg.cn/01cf6c36ee294e79a52f11f29e12d3c2.png)
由上面源码得知，真正认证操作在`AuthenticationManager`里面！然后看`AuthenticationManager`的实现类`ProviderManager`：
![在这里插入图片描述](https://img-blog.csdnimg.cn/ca6c8d23c2fd4332bde6187e0dc848fa.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/5df55d4ec963499d96810e0740e8fcca.png)
循环所有AuthenticationProvider，匹配当前认证类型。找到了对应认证类型就继续调用AuthenticationProvider对象完成认证业务。
![在这里插入图片描述](https://img-blog.csdnimg.cn/0ac2d80fc2f74453a5dcfafecfc4296d.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/f72c8883bb084425998744d2ee0b83c0.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/041a94d35d9f4773adae04485199da78.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/ba2a66b327274e40bc5decb2be17c7b2.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/50e112c26bf74316a3db9928b39581c3.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/0cd9fdd5090a4031a64a07c45b0383cd.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/b443c4cf0d284ea0ac454e72f4aa793f.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/38ca76a56aff45ec88b8687e9799a915.png)
我们发现最终去做认证的是 `UserDetailsService`接口的实现去完成的，那么我们要自定义认证过程，那么也只需要实现该接口接口，下篇我们具体看看如何实现。



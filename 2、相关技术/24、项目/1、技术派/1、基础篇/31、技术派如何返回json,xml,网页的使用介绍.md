
随着前后端的差异越来越大，前后端分离架构的越来越主流，后端返回json数据给前端进行渲染的方式大家比较熟悉，至于返回html页面，返回xml的方式接触的机会可能就越来越少了，虽然用得少，但不代表这个知识点不重要，接下来我们就来看下技术派中的各种返回姿势

# 1、返回文本数据

json姿势的返回实属最简单的方式了，再SpringBoot应用中，两种简单的方式

## 1.1、直接在Controller上添加 `@RestController` 注解


比如admin下的相关接口，都是直接返回json数据：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032031003.png)

返回结果如下：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032032907.png)

## 1.2、在方法上，添加 `@ResponseBody` 注解

如果对应的Controller上的注解是 `@Controller` ，若希望返回json数据，那么可以在方法上添加注解来实现，以扫码登录这里为例：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032033688.png)

## 1.3、HttpServletResponse输出

一个非常基础的写法，直接将返回的数据通过HttpServletResponse写入到输出流中，比较不太常见

如，直接在TestController中写一个测试demo
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032035180.png)

实际访问看下返回：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032037580.png)

那么问题来了，什么场景会使用这种姿势呢？还真巧，技术派中确实有这个应用场景

当前技术派的后端项目，同时支持返回页面 + json数据两种方式，因此在异常场景下，某些case希望返回500的错误页面（比如技术派的前台页面访问）；而某些场景（比如技术派的后台admin接口访问），又希望返回的是500的异常状态码

对应的实在全局异常处理中，进行的分条件输出
>com.github.paicoding.forum.web.global.ForumExceptionHandler#resolveException
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032039776.png)

# 2、返回网页

网页实际上也是一种文本，我们这里说到返回网页，更准确一点的表达应该是返回渲染视图，现在技术派的前台，是基于thymleaf渲染引擎实现的网页渲染，接下来看下具体的操作姿势

## 2.1、项目结构

所有静态资源相关的信息，存储在独立的模块 paicoding-ui 中（因此有兴趣的小伙伴完全可以使用freemaker等其他的渲染引擎来替换整个前台网站）
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032041581.png)

前端资源文件默认放在resources目录下，下面有两个目录
- templates：存放模板文件，可以理解为我们编写的html，注意这个文件名不能有问题
- static：存放静态资源文件，如js，css，image等
## 2.2、配置

web相关配置 application-web.yml
```yml
spring:
  thymeleaf:
    mode: HTML
    encoding: UTF-8
    servlet:
      content-type: text/html
    cache: false
```
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032044784.png)


## 2.3、后端返回

后台返回视图的接口实现，与前面的区别在于Controller上的注解是@Controller， 方法上没有@RestController
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032044578.png)

重点关注下上面的返回，是一个String，对应的实际上是模板的相对路径
# 3、xml返回

除了上面两种之外，技术派中还支持直接返回XML格式的文档，主要用于站点地图 + 微信公众号的回调响应

## 3.1、返回xml

主要通过设置返回的请求头，来标记返回的数据类型；其他的使用姿势，于返回文本时类似（`@RestController  + @ResponseBody`两种标注方式)

如实际使用的场景：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032058715.png)

对应的返回：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032059649.png)

## 3.2、返回xml文件

看了 `com.github.paicoding.forum.web.front.home.SiteMapController` 的实现的小伙伴，还可以看到这里有一个特殊的实现，可以直接返回xml文件，主要是给搜索引擎使用
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032103470.png)

## 3.3、返回相关配置

技术派的项目中，同时支持返回json/xml，那么它们会冲突吗？比如我希望所有的接口，默认返回都是json，只有特定的接口返回的才是xml，那么会和我们预期的一致吗？

如果不进行特殊配置，可能是无法达到我们上面的诉求的，因此在技术派中，有一个XmlWebConfig的配置，主要就是解决这个问题
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032107792.png)

# 4、返回图片

最后再给一个技术派中未提供的场景，直接接口返回图片
```java
/**
 * 返回图片
 */
@GetMapping(path = "img")
public void imgRsp(HttpServletResponse response) throws IOException {
    response.setContentType("image/png");
    ServletOutputStream outStream = response.getOutputStream();
    
    String path = "https://spring.hhui.top/spring-blog/imgs/info/info.png";
    URL uri = new URL(path);
    BufferedImage img = ImageIO.read(uri);
    ImageIO.write(img, "png", response.getOutputStream());
    System.out.println("--------");
}
```

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202309032111387.png)

# 5、小结

本文内容比较简单，但是涉及到的相关知识点挺多的，推荐有兴趣了解更多的小伙伴，扩展阅读下面的文章
- [【WEB系列】返回文本、网页、图片的操作姿势 | 一灰灰Blog](https://spring.hhui.top/spring-blog/2019/09/13/190913-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87%E4%B9%8B%E8%BF%94%E5%9B%9E%E6%96%87%E6%9C%AC%E3%80%81%E7%BD%91%E9%A1%B5%E3%80%81%E5%9B%BE%E7%89%87%E7%9A%84%E6%93%8D%E4%BD%9C%E5%A7%BF%E5%8A%BF/)
- [【WEB系列】Thymeleaf环境搭建 | 一灰灰Blog](https://spring.hhui.top/spring-blog/2019/08/20/190820-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8Bweb%E7%AF%87%E4%B9%8BThymeleaf%E7%8E%AF%E5%A2%83%E6%90%AD%E5%BB%BA/)
- [【WEB系列】 定义接口返回类型的几种方式 | 一灰灰Blog](https://spring.hhui.top/spring-blog/2022/08/17/220817-SpringBoot%E7%B3%BB%E5%88%97%E4%B9%8B%E5%AE%9A%E4%B9%89%E6%8E%A5%E5%8F%A3%E8%BF%94%E5%9B%9E%E7%B1%BB%E5%9E%8B%E7%9A%84%E5%87%A0%E7%A7%8D%E6%96%B9%E5%BC%8F/)
- [【WEB系列】xml传参与返回使用姿势 | 一灰灰Blog](https://spring.hhui.top/spring-blog/2020/07/06/200706-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8B%E4%B9%8Bxml%E4%BC%A0%E5%8F%82%E4%B8%8E%E8%BF%94%E5%9B%9E%E4%BD%BF%E7%94%A8%E5%A7%BF%E5%8A%BF/)
- [【WEB系列】 XML传参返回实战 | 一灰灰Blog](https://spring.hhui.top/spring-blog/2022/07/04/220704-SpringBoot%E7%B3%BB%E5%88%97%E6%95%99%E7%A8%8B%E4%B9%8BXML%E4%BC%A0%E5%8F%82%E8%BF%94%E5%9B%9E%E5%AE%9E%E6%88%98/)

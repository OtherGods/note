大家好，我是二哥呀。技术派的教程中一直缺一篇，图片如何上传至阿里云的 OSS 服务器，虽然是一篇很基础的内容，但总这么缺着总有球友找我要，所以还是写一篇吧。

但我会结合技术派 admin 端的业务来写，比如说：

- 图片如何复制粘贴即可完成上传？
- 图片如何自动转链（外链转为内链，否则无法访问）？
- 图片如何防止 30s 内重复上传？
- 服务端如何利用 Guava Cache 提高图片上传的效率？
- 配置文件更新时自动初始化阿里云 Client？
- 如何通过开关自由切换本地图片服务还是阿里云 OSS 服务？

# 1、什么是 OSS？

OSS 也就是 Object Storage Service，是阿里云提供的一套对象存储服务，国内的竞品还有七牛云的 Kodo 和腾讯云的 COS。

由于技术派最新的服务器是腾讯云的香港服务器，为了提升服务器到 OSS 之间的传输效率，我本来是打算使用腾讯云的 COS，但开通后发现用起来很麻烦，不如阿里云的 OSS 来得方便，所以也就没有迁移。

之所以要迁移，是因为阿里云上有这么一个规定：
> OSS 的 Bucket 在华东 1 (杭州)，客户端（比如说技术派的服务器）所在地域为中国香港，这类场景客户端会受到跨墙连路，速度就会比较慢。

![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162058614.png)

通过后台 StopWatch 日志（后面专门出篇教程来讲）打印观察到，技术派的图片上传瓶颈就是因为这个原因，很扯淡（😂）。

但不能不用！

OSS 存储比服务器端存储还是要方便很多，并且容易管理。即便是服务器迁移了，OSS 依然还能用，技术派的服务器就做过这么一个迁移。

阿里云丐版服务器（三年 200 多人民币屯的）→ 亚马逊服务器（太贵，一个月近 1000 人民币）→ 腾讯云香港地区（三年 3300 多人民币）

# 2、开通OSS

OSS 本身还算是比较便宜的，一个月可能也就几块钱吧，我这边 100GB 中国大陆 标准版 一个月 续费价格是 11 块，不过我一般都是直接续费一年。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162100360.png)

OK，开通 OSS 资源包后，直接进入 OSS 的管理控制台，点击「Bucket 列表」，点击「创建 Bucket」。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162100100.png)

Bucket 的词面意思是桶，这里指存储空间，就是用于存储对象的容器。注意读写权限为“公共读”（如果配置 CDN 的话，可以设置为私有），也就是允许互联网用户访问云空间上的图片。

点击「确定」就算是开通成功了。

开通之后，记得从 RAM（Resource Access Management） 访问控制这里拿到 accesskey ID 和 accesskey secret，这两个是访问阿里云 API 钥匙，有这两个就可以访问阿里云账户的所有权限，所以要妥善保管，千万不要泄露。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162101119.png)

我之前做编程喵的时候，为了方便球友们使用 OSS 就直接给了个别球友，结果被泄露了出去（惨，大家都没有这个安全意识），但好就好在，RAM 用户创建后可以销毁重新创建一个新的，旧的就不起效了。

注意拿到这两个关键配置后，还需要再拿到另外两个配置：Endpoint（地域节点） 和 Bucket（桶名）。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162102854.png)

那到这，OSS 的前期准备就完成了。

# 3、新增OSS配置文件

技术派的图片配置文件是在 application-image.yml 文件中，这样可以和其他配置信息很好的隔离开。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162103237.png)

这里只解释 OSS 的：
- image.oss.type：ali 为阿里云 OSS，local 为本地存储，rest 为阿里云 OSS 的中间转存服务，解决前面我们提到的 OSS 跨域限制问题
- image.oss.prefix：上传文件的前缀路径
- image.oss.endpoint：前面提到的地域节点
- image.oss.ak：前面提到的 accesskey ID
- image.oss.sk：前面提到的 accesskey secret
- image.oss.bucket：前面提到的桶名
- image.oss.host：后面要用到的 CDN 域名

本地图片我们之前讲过，在这里：
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162106376.png)

对应的 JavaBean 也不能少，用了 lombok 的 Data 注解。
```java
@Data  
public class OssProperties {  
    /**  
     * 上传文件前缀路径  
     */  
    private String prefix;  
    /**  
     * oss类型  
     */  
    private String type;  
    /**  
     * 下面几个是oss的配置参数  
     */  
    private String endpoint;  
    private String ak;  
    private String sk;  
    private String bucket;  
    private String host;  
}
```

上一层的图片配置文件类 ImageProperties。
```java
@Setter  
@Getter  
@Component  
@ConfigurationProperties(prefix = "image")  
public class ImageProperties {  
  
    /**  
     * 存储绝对路径  
     */  
    private String absTmpPath;  
  
    /**  
     * 存储相对路径  
     */  
    private String webImgPath;  
  
    /**  
     * 上传文件的临时存储目录  
     */  
    private String tmpUploadPath;  
  
    /**  
     * 访问图片的host  
     */    private String cdnHost;  
  
    private OssProperties oss;  
  
    public String buildImgUrl(String url) {  
        if (!url.startsWith(cdnHost)) {  
            return cdnHost + url;  
        }  
        return url;  
    }  
}
```

Spring Boot 的 @ConfigurationProperties 注解使得类能够方便地将配置文件（如 application.yml）中的属性绑定到类的字段上。

Spring 的注解 @Component  用于将此类标记为 Spring 容器的组件，这意味着 Spring 将在启动时自动创建 ImageProperties 的实例，并将其纳入 Spring 容器进行管理。意味着我们在其他地方可以直接这样使用。
```java
@Autowired
private ImageProperties properties;
```

@ConfigurationProperties(prefix = "image") 用于将配置文件中前缀为 image 的属性绑定到该类的字段上，也就是前面提到的那个 application-image.yml 文件中的属性。

# 4、在pom.xml文件中添加OSS依赖包

先是 version 版本：
```xml
<aliyun-sdk-oss.version>3.17.2</aliyun-sdk-oss.version>
```

可通过这个链接查看最新版本。
> https://mvnrepository.com/artifact/com.aliyun.oss/aliyun-sdk-oss

usages 最多的一般都比较稳定。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162109921.png)

然后是依赖项：
```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>${aliyun-sdk-oss.version}</version>
</dependency>
```

注意 pom.xml 文件的位置。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162110742.png)

# 5、新增图片上传接口

更了方便切换图片上传的的服务，比如说是本地还是 OSS，我们新增一个 ImageUploader 接口，提供两个关键的方法，一个是 upload （图片上传用），一个是 uploadIgnore（图片转链用）。
```java
public interface ImageUploader {  
    String DEFAULT_FILE_TYPE = "txt";  
    Set<MediaType> STATIC_IMG_TYPE = new HashSet<>(Arrays.asList(MediaType.ImagePng, MediaType.ImageJpg, MediaType.ImageWebp, MediaType.ImageGif));  
  
    /**  
     * 文件上传  
     *  
     * @param input  
     * @param fileType  
     * @return  
     */    String upload(InputStream input, String fileType);  
  
    /**  
     * 判断外网图片是否依然需要处理  
     *  
     * @param fileUrl  
     * @return true 表示忽略，不需要转存  
     */  
    boolean uploadIgnore(String fileUrl);  
  
    /**  
     * 获取文件类型  
     *  
     * @param input  
     * @param fileType  
     * @return  
     */    default String getFileType(ByteArrayInputStream input, String fileType) {  
        if (StringUtils.isNotBlank(fileType)) {  
            return fileType;  
        }  
  
        MediaType type = MediaType.typeOfMagicNum(FileReadUtil.getMagicNum(input));  
        if (STATIC_IMG_TYPE.contains(type)) {  
            return type.getExt();  
        }  
        return DEFAULT_FILE_TYPE;  
    }  
}
```

默认的 getFileType 方法相信大家一看就知道是干啥用的，获取文件的后缀名，主要用来限定我们的图片只能是 png、jpg、webp、gif 等。

> WebP 是由 Google 开发的一种现代图像格式，它提供了比传统格式（如 JPEG 和 PNG）更有效的图像数据压缩。使用 WebP，网站和应用程序可以创建更小、更快且更美观的图像，同时减少带宽和加载时间。

# 6、新增OSS实现类

AliOssWrapper 这个实现类的注释和 log 比较多，我就直接截图再做说明，在 paicoding-service 包下，用于实现与 OSS 的交互。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162112623.png)

@Slf4j  注解和 @Component  注解我就不再赘述了，一个是为了方便获取 log 对象，一个前面说过为了注入 Spring 的容器，方便创建对象。

@ConditionalOnExpression 这个 Spring 注解使得这个 Bean 只有在满足特定条件时才会被创建。这里的条件是环境配置项 image.oss.type 必须等于 ali，我们前面提到过。

在这个注解的参数 value 可以是一个 SpEL（Spring Expression Language）表达式，environment.getProperty('image.oss.type') 会从 Spring 的环境中获取名为 image.oss.type 的属性值。

该实现了 ImageUploader 接口，后面我们会重点讲解 upload 方法和 uploadIgnore 方法。

InitializingBean 接口的实现主要是用来在 Bean 的属性被初始化后执行自定义逻辑，也就是 afterPropertiesSet 方法。
```java
public interface InitializingBean {

	void afterPropertiesSet() throws Exception;

}
```

我们来看一下方法的实现。
```java
@Override  
public void afterPropertiesSet() {  
    init();  
    // 监听配置变更，然后重新初始化OSSClient实例  
    dynamicConfigContainer.registerRefreshCallback(properties, () -> {  
        init();  
        log.info("ossClient refreshed!");  
    });  
}
```

先执行 init 方法，调用 OSS 的 SDK 来创建一个 client，通过前面提到的地域节点、accesskey ID、accesskey secret 这三个配置项。
```java
private void init() {
    // 创建OSSClient实例。
    log.info("init ossClient");
    ossClient = new OSSClientBuilder().build(properties.getOss().getEndpoint(), properties.getOss().getAk(), properties.getOss().getSk());
}
```

OSSClient 是 OSS 的 Java 客户端，用于管理存储空间和文件等 OSS 资源。使用 Java SDK 发起 OSS 请求之前，需要初始化好 OSSClient 实例。

再来说 dynamicConfigContainer.registerRefreshCallback()，该方法用于注册一个回调方法，本例中主要是为了 ImageProperties 变化时，重新初始化 OSSClient。
```java
dynamicConfigContainer.registerRefreshCallback(properties, () -> {
    init();
    log.info("ossClient refreshed!");
});
```

DynamicConfigContainer 类是一个用于动态配置管理的组件，负责监听并加载外部配置源的变化，比如说 ImageProperties。

在本例中，当 ImageProperties 中的属性发生变更时（如 endpoint、access key、secret key 等），回调函数 `() -> { init(); log.info("ossClient refreshed!"); }` 被执行。

这样就非常方便，尤其是当我们在本地修改 OSS 配置文件后，只要点击一下 build，OSSClient 就会被重新初始化。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162143215.png)
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162143454.png)

DisposableBean 接口的实现主要是为了 Spring 容器在销毁该 Bean 之前调用 destroy 方法，然后执行一些清理工作，比如说本例中的关闭 OSSClient。
```java
@Override
public void destroy() {
    if (ossClient != null) {
        ossClient.shutdown();
    }
}
```

继续来看 upload 方法，这个方法的注释和 log 比较多，我们同样截图说明，其中 StopWatch 的内容先忽略，我们后面再开一篇教程来讲。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162144400.png)

upload 方法就主要是将前端传递过来图片上传到 OSS 中，`byte[] bytes = StreamUtils.copyToByteArray(input)` 是为了从输入流中获取字节对象，然后再根据字节数组获取文件名，并将字节流放入 OSS 的上传请求中并提交。
```java
ByteArrayInputStream input = new ByteArrayInputStream(bytes);
fileName = properties.getOss().getPrefix() + fileName + "." + getFileType(input, fileType);
// 创建PutObjectRequest对象。
PutObjectRequest putObjectRequest = new PutObjectRequest(properties.getOss().getBucket(), fileName, input);
// 设置该属性可以返回response。如果不设置，则返回的response为空。
putObjectRequest.setProcess("true");

// 上传文件
PutObjectResult result = stopWatchUtil.record("文件上传", () -> ossClient.putObject(putObjectRequest));
if (SUCCESS_CODE == result.getResponse().getStatusCode()) {
    return properties.getOss().getHost() + fileName;
} else {
    log.error("upload to oss error! response:{}", result.getResponse().getStatusCode());
    // Guava 不允许回传 null
    return "";
}
```

①、Md5Util.encode(bytes) 是用来计算文件名，通常是这样的：ae8d68845cf02f6ca48fe48e8211f8ae。

②、fileName = properties.getOss().getPrefix() + fileName + "." + getFileType(input, fileType); 的结果通常是 paicoding/ae8d68845cf02f6ca48fe48e8211f8ae.png。

③、PutObjectRequest putObjectRequest = new PutObjectRequest(properties.getOss().getBucket(), fileName, input); 就是将 OSS 的桶名、文件名、文件流作为参数创建一个 OSS 的请求对象。

④、putObjectRequest.setProcess("true"); 设置该属性可以返回 response。如果不设置，则返回的 response 为空。

⑤、ossClient.putObject(putObjectRequest) 主要用来完成提交请求。

⑥、如果SUCCESS_CODE == result.getResponse().getStatusCode() 就说明上传成功了，我们直接将 properties.getOss().getHost() + fileName 返回，通常是：https://cdn.tobebetterjavaer.com/paicoding/ae8d68845cf02f6ca48fe48e8211f8ae.png，这是已经加上 CDN（后面再讲）后的结果。

再来说说 uploadIgnore 方法，判断给定的文件 URL 是否应该忽略上传。：
```java
public boolean uploadIgnore(String fileUrl) {
  if (StringUtils.isNotBlank(properties.getOss().getHost()) && fileUrl.startsWith(properties.getOss().getHost())) {
      return true;
  }

  return !fileUrl.startsWith("http");
}
```

if 语句用来判断 URL 是不是 CDN 开头的，是就忽略；然后再判断 URL 是否以 http 开头，如果不是，说明可能不是一个有效的图片资源。

# 7、新增图片控制器

ImageRestController 类就比较容易理解了，它主要用来接收前端发起的图片上传和转链请求。
```java
@Permission(role = UserRole.LOGIN)
@RequestMapping(path = {"image/", "admin/image/", "api/admin/image/",})
@RestController
@Slf4j
public class ImageRestController {

    @Autowired
    private ImageService imageService;

    /**
     * 图片上传
     *
     * @return
     */

    @RequestMapping(path = "upload")
    public ResVo<ImageVo> upload(HttpServletRequest request) {
        ImageVo imageVo = new ImageVo();
        try {
            String imagePath = imageService.saveImg(request);
            imageVo.setImagePath(imagePath);
        } catch (Exception e) {
            log.error("save upload file error!", e);
            return ResVo.fail(StatusEnum.UPLOAD_PIC_FAILED);
        }
        return ResVo.ok(imageVo);
    }

    /**
     * 转存图片
     *
     * @param imgUrl
     * @return
     */
    @RequestMapping(path = "save")
    public ResVo<ImageVo> save(@RequestParam(name = "img", defaultValue = "") String imgUrl) {
        ImageVo imageVo = new ImageVo();
        if (StringUtils.isBlank(imgUrl)) {
            return ResVo.ok(imageVo);
        }

        String url = imageService.saveImg(imgUrl);
        imageVo.setImagePath(url);
        return ResVo.ok(imageVo);
    }
}
```

①、@Permission  注解主要用来进行访问权限控制，在这个控制器中 @Permission(role = UserRole.LOGIN)，要求用户必须登录才能上传图片和转链。

②、@RequestMapping(path = {"image/", "admin/image/", "api/admin/image/",}) 用来指定多个请求路径，这样用户端和 admin 端都可以通过该接口来处理图片，用户端通过 image/，admin 端通过后两个。

③、@RestController  和 @Slf4j  注解我们就略过了，前面都讲过了。 

④、ImageService 是用来进行图片处理的 service 接口。

```java
public interface ImageService {
    /**
     * 图片转存
     * @param content
     * @return
     */
    String mdImgReplace(String content);


    /**
     * 外网图片转存
     *
     * @param img
     * @return
     */
    String saveImg(String img);

    /**
     * 保存图片
     *
     * @param request
     * @return
     */
    String saveImg(HttpServletRequest request);
}
```

那实现类 ImageServiceImpl 就主要是为了实现这三个方法，其中 mdImgReplace 方法可以暂时不管，在讲文件内容保存时我们再讲。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162150386.png)

saveImg(HttpServletRequest request) 用来上传图片，public String saveImg(String img) 用来转链。

saveImg 的内容也非常简单，方法接收一个 HttpServletRequest 对象作为参数，这是当前的 HTTP 请求；检查请求是否是 MultipartHttpServletRequest 类型，这是 Spring 提供的用于处理文件上传请求的特殊请求类型；如果是，尝试从请求中获取名为 "image" 的文件。如果找不到文件，抛出异常。

通过 validateStaticImg 验证图片是否为支持的图片格式，比如说前面提到的 jpg、png 等。

然后调用前面提到的 imageUploader.upload 将图片上传至 OSS 存储桶。
```java
@Override
public String saveImg(HttpServletRequest request) {
    MultipartFile file = null;
    if (request instanceof MultipartHttpServletRequest) {
        file = ((MultipartHttpServletRequest) request).getFile("image");
    }
    if (file == null) {
        throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "缺少需要上传的图片");
    }

    // 目前只支持 jpg, png, webp 等静态图片格式
    String fileType = validateStaticImg(file.getContentType());
    if (fileType == null) {
        throw ExceptionUtil.of(StatusEnum.ILLEGAL_ARGUMENTS_MIXED, "图片只支持png,jpg,gif");
    }

    try {
        return imageUploader.upload(file.getInputStream(), fileType);
    } catch (IOException e) {
        log.error("Parse img from httpRequest to BufferedImage error! e:", e);
        throw ExceptionUtil.of(StatusEnum.UPLOAD_PIC_FAILED);
    }
}
```

顺带说一下 validateStaticImg 方法，主要是为了判断文件的类型，用到了前面提到的 ImageUploader 接口中的默认 Set STATIC_IMG_TYPE。
```java
private String validateStaticImg(String mime) {
    if ("svg".equalsIgnoreCase(mime)) {
        // fixme 上传文件保存到服务器本地时，做好安全保护, 避免上传了要给攻击性的脚本
        return "svg";
    }

    if (mime.contains(MediaType.ImageJpg.getExt())) {
        mime = mime.replace("jpg", "jpeg");
    }
    for (MediaType type : ImageUploader.STATIC_IMG_TYPE) {
        if (type.getMime().equals(mime)) {
            return type.getExt();
        }
    }
    return null;
}
```

然后是转链的 saveImg，比如说前端有一张图片是 [https://files.mdnice.com/user/3903/ef46c3ae-bb44-4fec-b3d0-dc9a84211c20.png](https://files.mdnice.com/user/3903/ef46c3ae-bb44-4fec-b3d0-dc9a84211c20.png)，那么我们可以将其转为 [https://cdn.tobebetterjavaer.com/paicoding/ae8d68845cf02f6ca48fe48e8211f8ae.png](https://cdn.tobebetterjavaer.com/paicoding/ae8d68845cf02f6ca48fe48e8211f8ae.png) 这样子。
```java
@Override
public String saveImg(String img) {
    if (imageUploader.uploadIgnore(img)) {
        // 已经转存过，不需要再次转存；非http图片，不处理
        return img;
    }

    try {
        String ans = imgReplaceCache.get(img);
        if (StringUtils.isBlank(ans)) {
            return buildUploadFailImgUrl(img);
        }
        return ans;
    } catch (Exception e) {
        log.error("外网图片转存异常! img:{}", img, e);
        return buildUploadFailImgUrl(img);
    }
}
```

其中用到了 Guava Cache 的 LoadingCache，我们前面也曾讲过。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162152977.png)

来看一下具体的代码。
```java
private LoadingCache<String, String> imgReplaceCache = CacheBuilder.newBuilder().maximumSize(300).expireAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
    @Override
    public String load(String img) {
        try {
            InputStream stream = FileReadUtil.getStreamByFileName(img);
            URI uri = URI.create(img);
            String path = uri.getPath();
            int index = path.lastIndexOf(".");
            String fileType = null;
            if (index > 0) {
                // 从url中获取文件类型
                fileType = path.substring(index + 1);
            }
            return imageUploader.upload(stream, fileType);
        } catch (Exception e) {
            log.error("外网图片转存异常! img:{}", img, e);
            return "";
        }
    }
});
```

具体的缓存规则是：最多缓存 300 个条目，每个条目在写入后 5 分钟内有效。如果需要从缓存中获取一个值但该值不存在或已过期，则会调用 load 方法来加载并存储新的值。

这是一个很实用的方法，尤其是对于避免不必要的网络请求或文件操作。

也就是说，第一次获取转链的图片时 String ans = imgReplaceCache.get(img)，如果之前没有加载过，就会调用前面定义的 load 方法获取原 URL 中的图片，然后调用 ImageUploader 的 upload 方法，将图片上传至 OSS 并返回新的 URL。

# 8、admin新增加图片上传功能

服务端的一切都搞定了，接下来我们就在 admin 端增加一个复制粘贴上传图片和一个转链的功能。

## 8.1、复制粘贴上传功能

复制粘贴图片上传的功能其实很常见，做法也不尽相同，因为要和实际的页面组件管理起来。

技术派的 admin 端用的是 bytemd，也就是字节的一个开源组件库。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162153193.png)

熟悉掘金社区的球友应该是比较清楚，挺好用的。

技术派的 admin 端已经帮大家集成过了，这里我们重点来看一下复制粘贴上传图片的功能。

代码我直接截图，大家可以按图索骥找到对应的位置。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162154598.png)

代码其实也很简单，bytemd 提供了一个 uploadImages 属性，返回一个 Promise 对象（Promise 对象表示异步操作最终的完成（或失败）以及其结果值），参数会传递一个 files，可以通过 map 方法进行遍历。
```java
uploadImages={(files) => {
    return Promise.all(
        files.map((file) => {
            // 限制图片大小，不超过 5M
            if (file.size > 5 * 1024 * 1024) {
                return  {
                    url: "图片大小不能超过 5M",
                }
            }

            const formData = new FormData();
    formData.append('image', file);

            return uploadImgApi(formData).then(({ status, result }) => {
                const { code, msg } = status || {};
                const { imagePath } = result || {};
                if (code === 0) {
                    return {
                        url: imagePath,
                    }
                }
                return {
                    url: msg,
                }
            })
        })
    )
}}
```

①、首先判断图片的大小，不能超过 5M；

②、接着封装 FormData 对象（提供了一种表示表单数据的键值对 key/value 的构造方式，并且可以轻松的将数据通过 XMLHttpRequest.send() 方法发送出去），将文件 file 添加进来。

③、将 formData 作为参数调用 uploadImgApi 发起请求。
```java
export const uploadImgApi = (data: FormData) => {
	return http.post<Login.ResAuthButtons>(`${PORT1}/image/upload`, data);
};
```

看内容就很好理解，向 /image/upload 发起 post 请求，也就是我们前面提到的控制器 ImageRestController。

④、服务端有响应结果后会回调 then 方法执行里面获取图片上传后的 URL。

我们来体验一下，随便复制一张截图粘贴过来，上传后如下图所示。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162155879.png)

## 8.2、图片转链

图片转链主要是在 handleReplaceImgUrl 方法中，会取出 bytemd 中最新的内容，然后通过正则表达式获取到需要转链的 markdown 内容，然后，将其发送至服务器端获取到转链后的新链接。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162156595.png)

①、/!\[(.*?)\]\((.*?)\)/mg 这个表达式用于匹配 Markdown 格式的图片链接。这个正则表达式可以解释为：
- `/!\[(.*?)\]: 匹配以感叹号!开头，后跟方括号[]内包含的任何内容（图片的替代文本）。`
- `\((.*?)\): 紧跟在方括号后的圆括号内的任何内容（图片的URL）。`
- `mg: 正则表达式的修饰符，“m”表示多行匹配，“g”表示全局搜索，即在整个字符串中寻找所有匹配项。`
②、matcher = pattern.exec(contentTemp)，每次调用 exec 方法时，都会返回下一个匹配项，直到没有更多匹配项为止（此时返回null），如果找到匹配项，matcher 将是一个数组 `[img, alt, src]`，其中：
- img 是完整的匹配字符串（例如，整个图片链接 `![图片标题](图片URL)` ）
- alt 是第一个捕获组的匹配内容，在这个例子中是图片标题。
- src 是第二个捕获组的匹配内容，在这个例子中是图片的URL。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162159214.png)

③、如果是外网的图片，或者没有出现转链错误的情况，将会判断 30s 内是否有重复提交。
```js
// 判断图片的链接是否已经上传过了
const canUpload = (url: string) => {
    // 当前的时间
    const now = Date.now();

    const lastUploadTime = lastUploadTimes.current.get(url);
    // 如果没有上传过，或者上传时间超过了 30s，就返回 false
    if (lastUploadTime && now - lastUploadTime < 30000) {
        return false;
    }
    // 更新上传时间
    lastUploadTimes.current.set(url, now);
    return true;
}
```

④、通过验证后就会调用 saveImgApi 请求，如果转链成功会设置一个新的图片链接，然后替换原来的内容。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162200116.png)

```java
// 如果是外网的图片链接，转成内网的图片链接
	const uploadImages = async (newVal: string) => {
		let add;
		// 如果新的内容以上次转链后的内容开头
		if (newVal.startsWith(lastContent)) {
			// 变化的内容
			add = newVal.substring(lastContent.length);
		} else if (lastContent.startsWith(newVal)) {
			// 删掉了一部分内容
			setLastContent(newVal);
			console.log("删掉了一部分内容", lastContent);
			return;
		} else {
			add = newVal;
		}

		// 正则表达式
		const reg = /!\[(.*?)\]\((.*?)\)/mg;
		let match;

		let uploadTasks = [];
    let imageInfos:ImageInfo[] = []; // 用于存储图片信息和它们在文本中的位置

		while ((match = reg.exec(add)) !== null) {
			const [img, alt, src] = match;
			console.log("img, alt, src", match, img, alt, src);
			// 如果是外网的图片链接，转成内网的图片链接
			if (src.length > 0 && src.startsWith("http") 
				&& src.indexOf("saveError") < 0) {
				// 收集图片信息
				imageInfos.push({ img, alt, src, index: match.index });
				// 判断图片的链接是否已经上传过了
				if (!canUpload(src)) {
					console.log("30秒内防重复提交，忽略:", src);
					continue;
				} else {
					uploadTasks.push(saveImgApi(src));
				}
			}
		}

		// 同时上传所有图片
		const results = await Promise.all(uploadTasks);

		// 替换所有图片链接
		let newContent = newVal;
		results.forEach((result, i) => {
				if (result.status && result.status.code === 0 && result.result) {
					// 重新组织图片的路径
					const newSrc = `![${imageInfos[i].alt}](${result.result.imagePath})`;
					console.log("newSrc", newSrc);
					// 替换后的内容
					newContent = newContent.replace(imageInfos[i].img, newSrc);
					console.log("newContent", newContent);
				}
		});
		setLastContent(newVal);

		return newContent;
	}

	const handleReplaceImgUrl = async () => {
		const { content } = form;
		const newContent = await uploadImages(content);
		if (newContent) {
			setContent(newContent);
			handleChange({ content: newContent });
		}
	}
```

由于上传图片是一个异步操作，所以我们需要确保图片的顺序和他们在文本中出现的顺序一致。

所以我们需要在处理完所有图片上传后进行一次性的文本替换，以确保替换顺序与原始顺序相符。

1、先收集所有上传的图片信息，放在 imageInfos 中；
2、异步上传所有的图片，使用 Promise.all 来等待所有上传操作完成；
3、一次性替换文本中的所有图片链接。

OK，我们来验证一把，确认是 OK 的。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202409162202485.png)

# 9、小结

看似是如何上传至阿里云的 OSS 服务器，实则带出了很多细枝末节，比如说前面提到的：
- 图片如何复制粘贴即可完成上传？
- 图片如何自动转链（外链转为内链，否则无法访问）？
- 图片如何防止 30s 内重复上传？
- 服务端如何利用 Guava Cache 提高图片上传的效率？
- 配置文件更新时自动初始化阿里云 Client？
- 如何通过开关自由切换本地图片服务还是阿里云 OSS 服务？

每一个细节展开来说，可能都是一个很好的面试引申点，那其实面试的时候，就是这样，从一个点出发，把你擅长的点循序渐进的讲给面试官听，尤其是针对面试官感兴趣的点，一定要讲清楚。


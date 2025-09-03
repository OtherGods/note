# 1、简介

WebMvcConfigurer配置类其实是Spring内部的一种配置方式，采用JavaBean的形式来代替传统的xml配置文件形式进行针对框架个性化定制，可以自定义一些Handler，Interceptor，ViewResolver，MessageConverter。基于java-based方式的spring mvc配置，需要创建一个配置类并实现WebMvcConfigurer接口；

在Spring Boot1.5版本都是靠重写WebMvcConfigurerAdapter的方法来添加自定义拦截器、消息转换器等。在SpringBoot2.0之后，该类被标记为@deprecated（弃用）。官方推荐直接实现WebMvcCOnfigurer或者直接继承WebMvcConfigurerSupport，方式一实现WebMvcConfigurer接口（推荐），方式二继承WebMvcConfigurerSupport类，具体实现可以看：https://blog.csdn.net/fmwind/article/details/82832758。

# 2、WebMvcConfigurer接口

```java
public interface WebMvcConfigurer {
    /**
    与访问路径有关，比如说PathMatchConfigurer 有个配置是setUseTrailingSlashMatch(),如果设置为true的话（默认为true），后面加个斜杠并不影响路径访问，例如“/user”等同于“/user/"。我们在开发中很少在访问路径上搞事情，所以这个方法如果有需要的请自行研究吧。
    */
    void configurePathMatch(PathMatchConfigurer var1);
 
    /**
    配置内容裁决一些选项
    */
    void configureContentNegotiation(ContentNegotiationConfigurer var1);
 
    /**
    这是处理异步请求的。只能设置两个值，一个超时时间（毫秒，Tomcat下默认是10000毫秒，即10秒），还有一个是AsyncTaskExecutor，异步任务执行器。
    */
    void configureAsyncSupport(AsyncSupportConfigurer var1);
 
    /**
    默认静态资源处理器
    */
    void configureDefaultServletHandling(DefaultServletHandlerConfigurer var1);
 
    /**
    增加转化器或者格式化器。这边不仅可以把时间转化成你需要时区或者样式。还可以自定义转化器和你数据库做交互，比如传进来userId，经过转化可以拿到user对象。
    */
    void addFormatters(FormatterRegistry var1);

	/**
	添加SpringMVC生命周期拦截器，以进行控制器方
	法调用和资源处理程序请求的预处理和后处理。可
	以注册拦截器以应用于所有请求，也可以限于URL
	模式的子集。
	*/
    void addInterceptors(InterceptorRegistry var1);
 
	/**
    静态资源处理
    */
    void addResourceHandlers(ResourceHandlerRegistry var1);
 
    /**
    解决跨域问题
    */
    void addCorsMappings(CorsRegistry var1);

    /**
    视图跳转控制器
    */
    void addViewControllers(ViewControllerRegistry var1);
 
    /**
    这里配置视图解析器
    */
    void configureViewResolvers(ViewResolverRegistry var1);
 
    /**
    添加解析器以支持自定义控制器方法参数类型
    */
    void addArgumentResolvers(List<HandlerMethodArgumentResolver> var1);
 
    void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> var1);
 
    /**
    信息转换器
    */
    void configureMessageConverters(List<HttpMessageConverter<?>> var1);
 
    void extendMessageConverters(List<HttpMessageConverter<?>> var1);
 
    /**
    配置异常解析器；
    给定的列表开始为空。如果将其留为空，则该框架
    配置了一组默认的解析器，请参见WebMvcconfigurationsupport.addddefaulthandlerexceptionResolvers（list，org.springframework.web.accept.contentnegotiationmanager）。
    或者，如果将任何异常解析器添加到列表中，则该
    应用程序有效地接管并必须提供完全初始化的异常
    解析器。
    另外，您可以使用extendHandlerExceptionResolver(List)，它允许
    你扩展或者修改默认配置的异常解析器列表
    */
    void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> var1);
 
    /**
    扩展或修改默认情况下配置的异常解析器，这对于
    插入自定义异常解析器而不干扰的默认解析器很
    有用。
    */
    void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> var1);
 
    Validator getValidator();
 
    MessageCodesResolver getMessageCodesResolver();
}
```

## 2.1、addInterceptors：拦截器

1. addInterceptor：需要一个实现HandlerInterceptor接口的拦截器实例
2. addPathPatterens：用于设置拦截器的过滤路径规则；`addPathPatterens("/**")`对于所有请求都拦截
3. excludePathPatterens：用于设置不需要拦截的过滤规则

拦截器主要用途：进行用户登录状态的拦截，日志的拦截等。

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    super.addInterceptors(registry);
    registry.addInterceptor(new TestInterceptor()).addPathPatterns("/**").excludePathPatterns("/emp/toLogin","/emp/login","/js/**","/css/**","/images/**");
}
```
## 2.2、addViewControllers：页面跳转

在以前写SpringMVC的时候，如果需要访问一个页面，必须要写Controller类，然后再写一个方法跳转到页面，感觉好麻烦，其实冲洗WebMvcConfigurer中的addViewControllers方法即可达到效果了。
```java
@Override
public void addViewControllers(ViewControllerRegistry registry) {
	registry.addViewController("/toLogin").setViewName("login");
}
```

值得指出的是，在这里重写addViewControllers方法，并不会覆盖WebMvcAutoConfiguration（Spring Boot自动配置）中的addViewController（在此方法中，Spring Boot将“/”映射至index.html），这也意味着自己的配置和Spring Boot的自动配置同时有效，这也是我们推荐添加自己的MVC配置的方式。

## 2.3、addResourceHandlers：静态资源

比如，我们想自定义静态资源映射目录的话，只需重写addResourceHandlers方法即可。

注：如果继承WebMvcConfigurationSupport类实现配置时必须要重写该方法，具体见其它文章
```java
@Configuration
public class MyWebMvcConfigurerAdapter implements WebMvcConfigurer {
    /**
     * 配置静态访问资源
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/my/**").addResourceLocations("classpath:/my/");
    }
}
```

- addResourceHandler：指的是对外暴露的访问路径
- addResourceLocations：指的是内部文件放置的目录

## 2.4、configureDefaultServletHandling：默认静态资源处理器

```java
@Override
public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
        configurer.enable("defaultServletName");
}
```

此时会注册一个默认的Handler：DefaultServletHttpRequestHandler，这个Handler也是用来处理静态文件的，他会映射 `/` 。当DispatcherServelt映射`/`时（/和/是有区别的），并且没有找到合适的Handler来处理请求时，就会交给DefaultServletHttpRequestHandler来处理。注意：这里的静态资源是放在web根目录下，而非WEB-INF下。
#SpringMVC中静态资源被拦截
举个简单的例子：在webroot目录下有一个图片：1.png 我们知道Servlet规范中web根目录（webroot）下的文件是可以直接访问的，但是由于DispatcherServlet配置了映射路径是：/，它几乎把所有的请求都拦截了，从而导致1.png访问不到，这时注册一个DefaultServletHttpRequestHandler就可以解决这个问题。可以理解为DsipatcherServlet破坏了Servlet的一个特性（根目录下的文件可以直接访问），DefaultServletHttpRequest是帮助回归这个特性的。

## 2.5、configureViewResolvers：视图解析器

这个方法是用来配置视图解析器的，该方法的参数ViewResolverRegistry是一个注册器，用来注册你想自定义的视图解析器等。ViewResolverRegistry常用的几个方法：
1. enableContentNegotiation()：
   ```java
/** 启用内容裁决视图解析器*/
public void enableContentNegotiation(View... defaultViews) {
    initContentNegotiatingViewResolver(defaultViews);
}
   ```
   该方法会创建一个内容裁决解析器<font color = "red">ContentNegotiatingViewResolver，该解析器不进行具体视图的解析，而是管理你注册的所有视图解析器</font>，所有的视图会先经过它进行解析，然后由它来决定具体使用那个解析器来进行解析。具体的映射规则是根据请求的media types来决定的。
2. jsp()
   ```java
public UrlBasedViewResolverRegistration jsp(String prefix, String suffix) {
	InternalResourceViewResolver resolver = new InternalResourceViewResolver();
	resolver.setPrefix(prefix);
	resolver.setSuffix(suffix);
	this.viewResolvers.add(resolver);
	return new UrlBasedViewResolverRegistration(resolver);
}
   ```
   该方法会注册一个内部资源视图解析器InternalResolverViewResolver显然访问的所有jsp都是它进行解析的。该方法参数用来指定路径的前缀和文件后缀，如：
   ```java
   registry.jsp("/WEB-INF/jsp/", ".jsp");
   ```
   对于以上配置，假如返回的视图名称是example，他会返回/WEB-INF/jsp/example.jsp给前端，找不到则报404。
3. beanName()
   ```java
public void beanName() {
	BeanNameViewResolver resolver = new BeanNameViewResolver();
	this.viewResolvers.add(resolver);
}
   ```
   该方法会注册一个BeanNameViewResolver视图解析器，这个解析器主要是将试图名解析成对应的bean，假如返回的视图名称是example，它会到sprinng容器中找有没有一个叫example的bean，并且这个bean是View.class类型的？如果有，返回这个bean。
4. viewResolver()
   ```java
public void viewResolver(ViewResolver viewResolver) {
	if (viewResolver instanceof ContentNegotiatingViewResolver) {
		throw new BeanInitializationException(
				"addViewResolver cannot be used to configure a ContentNegotiatingViewResolver. Please use the method enableContentNegotiation instead.");
	}
	this.viewResolvers.add(viewResolver);
}
   ```
   这个方法是用来注册各种各样的视图解析器的，包括自己定义的。

## 2.6、configureContentNegotiation：配置内容裁决的一些参数

在configureViewResolvers方法中启用了内容裁决解析器，而configureContentNegotiation方法是专门用来配置内容裁决的一些参数，看示例：
```java
 public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
       /* 是否通过请求Url的扩展名来决定media type */
        configurer.favorPathExtension(true)  
                 /* 不检查Accept请求头 */
                .ignoreAcceptHeader(true)
                .parameterName("mediaType")
                 /* 设置默认的mediatype */
                .defaultContentType(MediaType.TEXT_HTML)
                 /* 请求以.html结尾的会被当成MediaType.TEXT_HTML*/
                .mediaType("html", MediaType.TEXT_HTML)
                /* 请求以.json结尾的会被当成MediaType.APPLICATION_JSON*/
                .mediaType("json", MediaType.APPLICATION_JSON);
    }
```

举个例子来进一步熟悉上面的知识，假设MVC的配置如下：
```java
@EnableWebMvc
@Configuration
public class MyWebMvcConfigurerAdapte extends WebMvcConfigurerAdapter {

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		registry.jsp("/WEB-INF/jsp/", ".jsp");
		registry.enableContentNegotiation(new MappingJackson2JsonView());
	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.favorPathExtension(true)
				.ignoreAcceptHeader(true)
				.parameterName("mediaType")
				.defaultContentType(MediaType.TEXT_HTML)
				.mediaType("html", MediaType.TEXT_HTML)
				.mediaType("json", MediaType.APPLICATION_JSON);
	}
}
```

controller的代码如下：
```java
@Controller
public class ExampleController {
	 @RequestMapping("/test")
	 public ModelAndView test() {
		Map<String, String> map = new HashMap();
		map.put("哈哈", "哈哈哈哈");
		map.put("呵呵", "呵呵呵呵");
		return new ModelAndView("test", map);
	}
}
```

在WEB-INF/jsp目录下创建一个test.jsp文件，内容随意。现在启动tomcat，在浏览器输入以下链接：[http://localhost:8080/test.json](http://localhost:8080/test.json)，浏览器内容返回如下：
```json
{
    "哈哈":"哈哈哈哈",
    "呵呵":"呵呵呵呵"
}
```

在浏览器输入[http://localhost:8080/test](http://localhost:8080/test) 或者[http://localhost:8080/test.html](http://localhost:8080/test.html)，内容返回如下：
```html
this is test.jsp
```

显然，两次使用了不同的视图解析器，那么底层到底发生了什么？在配置里我们注册了两个视图解析器：ContentNegotiatingViewResolver和InterResourceViewResolver，还有一个默认视图：MappingJackson2JsonView。controller执行完毕后返回一个ModelAndView，其中视图的名称为example1：
1. 返回首先会交给ContentNegotiatingViewResolver 进行视图解析处理，而ContentNegotiatingViewResolver 会先把视图名example1交给它持有的所有ViewResolver尝试进行解析（本实例中只有InternalResourceViewResolver），
2. 根据请求的mediaType，再将example1.mediaType（这里是example1.json 和example1.html）作为视图名让所有视图解析器解析一遍，两步解析完毕之后会获得一堆候选的`List<View>` 再加上默认的MappingJackson2JsonView ，
3. 根据请求的media type从候选的`List<View>` 中选择一个最佳的返回，至此视图解析完毕。

现在就可以理解上例中为什么请求连接加上`.json`和不加`.json`结果会不一样。当加上`.json`的时候表示请求的media type是MediaType.APPLICATION_JSON，而InternalResourceViewResolver解析出来的视图的ContentType与其不符，而与MappingJacksonJsonView的ContentType相符，所以选择了MappingJacksonJsonView作为视图返回。当不加.json请求时，默认的media type为MediaType.TEXT_HTML，所以就使用了InternalResourceViewResource解析出来的视图作为返回值了。

## 2.7、addCorsMappings：跨域

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    super.addCorsMappings(registry);
    registry.addMapping("/cors/**")
            .allowedHeaders("*")
            .allowedMethods("POST","GET")
            .allowedOrigins("*");
}
```
## 2.8、configureMessageConverters：信息转换器

```java
 
/**
* 消息内容转换配置
 * 配置fastJson返回json转换
 * @param converters
 */
@Override
public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    //调用父类的配置
    super.configureMessageConverters(converters);
    //创建fastJson消息转换器
    FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
    //创建配置类
    FastJsonConfig fastJsonConfig = new FastJsonConfig();
    //修改配置返回内容的过滤
    fastJsonConfig.setSerializerFeatures(
            SerializerFeature.DisableCircularReferenceDetect,
            SerializerFeature.WriteMapNullValue,
            SerializerFeature.WriteNullStringAsEmpty
    );
    fastConverter.setFastJsonConfig(fastJsonConfig);
    //将fastjson添加到视图消息转换器列表内
    converters.add(fastConverter);
 
}
```


转载自：[SpringBoot---WebMvcConfigurer详解](https://blog.csdn.net/zhangpower1993/article/details/89016503)



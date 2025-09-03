技术派实现了一个不那么明显的功能，将所有的外部访问都记录在日志文件中，为什么要设计这么个功能？
1. 在不引入Prometheus进行接口监控时，基于日志文件就可以实现整个项目的监控
2. 当出现问题时，可以基于此进行流量重放

实际部署到线上的表现效果如：
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307232329285.png)

接下来我们看一下，这个请求日志的记录是怎么实现的

# 1、技术方案

如果单纯的将这个记录接口的请求信息，当作一个普通的需求来设计，我们可以怎么来实现呢？
1. 基于过滤器Filter，来拦截web请求，记录请求相关信息
2. 基于AOP来实现方法拦截，借助@Around来实现请求方法执行前后增强，记录请求相关信息

这两个方案怎么选择呢？

## 1.1、Filter过滤器方案

关于过滤器的相关知识点，请参考教程：* [20、WEB三大组件之Filter在技术派中的应用](2、相关技术/24、项目/1、技术派/1、基础篇/20、WEB三大组件之Filter在技术派中的应用.md)
若使用过滤器，则主要就是拦截web请求了，具体的实现流程如
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307232339598.png)

在过滤器的doFilter方法中，划分为三块：
1. doBefore: 表示将请求转发到Controller执行之前 
	1. 记录开始执行时间
	2. 记录请求相关信息
2. doFilter: 即将请求转发到Controller去执行
3. doAfter: Controller方法执行完 
	1. 记录结束时间，计算执行耗时
	2. 日志输出

使用这种方式的优缺点比较突出，
1. 优点是适用性强，实现简单，
2. 缺点是只能记录Controller的请求相关信息，如果我们想统计某个Service方法、Mapper方法，那么这种方式则不太合适

## 1.2、AOP切面方案

若使用AOP来实现，则关键点在于需要拦截哪些方法，即定义切点
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202307232344002.png)

基本策略与前面差不多，不过有几个关键点
1. 定义切点：可以是直接拦截包路径方式，也可以配合自定义注解，拦截某些特定注解的方法
2. 使用Around环绕方式

使用AOP来实现的优缺点也比较明显
1. 优点：灵活性非常高，可以拦截任何共有方法
2. 缺点：需要自定义切点，通常不太容易一次编写，所有项目适用

## 1.3、选型
技术派中，上面两个方案都会给出实现，所以不用担心二选一，毕竟我们的主旨是给大家提供一个完善的学习进阶项目，怎么可以遗漏知识点呢（😀）

这篇教程，主要介绍的是基于Filter来实现请求信息的记录；对于AOP的请期待下文

# 2、实现示例
## 2.1、实现类

在技术派中，核心实现类为：ReqRecordFilter，对应的包路径为：com.github.paicoding.forum.web.hook.filter.ReqRecordFilter

接下来我们看一下日志相关的核心实现（我会排除掉与日志无关的逻辑，所以会与实际的项目中代码不一致，不用因此产生疑问）
```java
@Slf4j  
@WebFilter(urlPatterns = "/*", filterName = "reqRecordFilter", asyncSupported = true)  
public class ReqRecordFilter implements Filter {  
    private static Logger REQ_LOG = LoggerFactory.getLogger("req");
    
	@Override  
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {  
	    long start = System.currentTimeMillis();  
	    HttpServletRequest request = null;  
	    try {  
	        request = this.initReqInfo((HttpServletRequest) servletRequest);
	        filterChain.doFilter(request, servletResponse);  
	    } finally {  
	        buildRequestLog(ReqInfoContext.getReqInfo(), request, System.currentTimeMillis() - start); 
	        ReqInfoContext.clear();  
	    }  
	}
	
	private HttpServletRequest initReqInfo(HttpServletRequest request, HttpServletResponse response) {  
	    if (staticURI(request)) {  
	        // 静态资源直接放行  
	        return request;  
	    }  
	    try {
	        ReqInfoContext.ReqInfo reqInfo = new ReqInfoContext.ReqInfo();
	        reqInfo.setHost(request.getHeader("host"));  
	        reqInfo.setPath(request.getPathInfo());  
	        reqInfo.setReferer(request.getHeader("referer")); 
	        reqInfo.setClientIp(IpUtil.getClientIp(request)); 
	        reqInfo.setUserAgent(request.getHeader("User-Agent"));  
	        
	        request = this.wrapperRequest(request, reqInfo);
	        ReqInfoContext.addReqInfo(reqInfo);
	    } catch (Exception e) {  
	        log.error("init reqInfo error!", e);  
	    }  
	    return request;  
	}
	
	private HttpServletRequest wrapperRequest(HttpServletRequest request, ReqInfoContext.ReqInfo reqInfo) {
	    if (!HttpMethod.POST.name().equalsIgnoreCase(request.getMethod())) {  
	        return request;  
	    } 
	    BodyReaderHttpServletRequestWrapper requestWrapper = new BodyReaderHttpServletRequestWrapper(request);  
	    reqInfo.setPayload(requestWrapper.getBodyString());  
	    return requestWrapper;  
	}
	
	private void buildRequestLog(ReqInfoContext.ReqInfo req, HttpServletRequest request, long costTime) {  
	    if (req == null || staticURI(request)) {  
	        return;  
	    }  
	    
	    StringBuilder msg = new StringBuilder();
	    msg.append("method=").append(request.getMethod()).append("; ");  
	    if (StringUtils.isNotBlank(req.getReferer())) {  
	        msg.append("referer=").append(URLDecoder.decode(req.getReferer())).append("; ");  
	    }  
	    msg.append("remoteIp=").append(req.getClientIp());  
	    msg.append("; agent=").append(req.getUserAgent());  
	    
	    if (req.getUserId() != null) {  
	        // 打印用户信息  
	        msg.append("; user=").append(req.getUserId());  
	    }  
	    
	    msg.append("; uri=").append(request.getRequestURI());  
	    if (StringUtils.isNotBlank(request.getQueryString())) {  
	        msg.append('?').append(URLDecoder.decode(request.getQueryString()));  
	    }
	    
	    msg.append("; payload=").append(req.getPayload());  
	    msg.append("; cost=").append(costTime);  
	    REQ_LOG.info("{}", msg);
	    
	    // 保存请求计数  
	    statisticsSettingService.saveRequestCount(req.getClientIp());  
	}
	
	private boolean staticURI(HttpServletRequest request) {  
	    return request == null  
	            || request.getRequestURI().endsWith("css")  
	            || request.getRequestURI().endsWith("js")  
	            || request.getRequestURI().endsWith("png")  
	            || request.getRequestURI().endsWith("ico")  
	            || request.getRequestURI().endsWith("svg");  
	}
}
```


## 2.2、排除静态资源
因为现在1.x版本的技术派，pc前台的网页也是集成在项目中的，因此我们在实际日志输出的时候，需要将一些静态资源的访问给排掉;
主要是基于request.getRequestURI() 后缀来进行过滤的
```java
	private boolean staticURI(HttpServletRequest request) {  
	    return request == null  
	            || request.getRequestURI().endsWith("css")  
	            || request.getRequestURI().endsWith("js")  
	            || request.getRequestURI().endsWith("png")  
	            || request.getRequestURI().endsWith("ico")  
	            || request.getRequestURI().endsWith("svg");  
	}
```

上面这种方式虽然实现简单，但是有缺陷：
1. 如静态资源请求带url参数
2. 如除了上面几种类型之外的静态资源（xml、MP3等）

## 2.3、请求上下文
接下来看一下请求上下文的构建，主要是基于HttpServletRequest来获取相关参数
```java
	private HttpServletRequest initReqInfo(HttpServletRequest request, HttpServletResponse response) {  
	    if (staticURI(request)) {  
	        // 静态资源直接放行  
	        return request;  
	    }  
	    try {
	        ReqInfoContext.ReqInfo reqInfo = new ReqInfoContext.ReqInfo();
	        reqInfo.setHost(request.getHeader("host"));  
	        reqInfo.setPath(request.getPathInfo());  
	        reqInfo.setReferer(request.getHeader("referer")); 
	        reqInfo.setClientIp(IpUtil.getClientIp(request)); 
	        reqInfo.setUserAgent(request.getHeader("User-Agent"));  
	        
	        request = this.wrapperRequest(request, reqInfo);
	        ReqInfoContext.addReqInfo(reqInfo);
	    } catch (Exception e) {  
	        log.error("init reqInfo error!", e);  
	    }  
	    return request;  
	}
```

重点关注俩个：
1. 请求者的ip获取（后面介绍到的如何判断用户的地理位置，就是根据这个ip来获取的）
   核心实现如下：（通用的工具类，需注意的是若使用nginx做反向代理，那么请不要将用户的请求信息给吃掉了，否则下面这个方法拿不到）
```java
/**  
 * 获取请求来源的ip地址  
 *  
 * @param request  
 * @return  
 */
public static String getClientIp(HttpServletRequest request) {
	try {
		String xIp = request.getHeader("X-Real-IP");  
		String xFor = request.getHeader("X-Forwarded-For");  
		if (StringUtils.isNotEmpty(xFor) && !UNKNOWN.equalsIgnoreCase(xFor)) {  
			//多次反向代理后会有多个ip值，第一个ip才是真实ip  
			int index = xFor.indexOf(",");  
			if (index != -1) {  
				return xFor.substring(0, index);  
			} else {  
				return xFor;  
			}  
		}  
		xFor = xIp;  
		if (StringUtils.isNotEmpty(xFor) && !UNKNOWN.equalsIgnoreCase(xFor)) {  
			return xFor;  
		}  
		if (StringUtils.isBlank(xFor) || UNKNOWN.equalsIgnoreCase(xFor)) {  
			xFor = request.getHeader("Proxy-Client-IP");  
		}  
		if (StringUtils.isBlank(xFor) || UNKNOWN.equalsIgnoreCase(xFor)) {  
			xFor = request.getHeader("WL-Proxy-Client-IP");  
		}  
		if (StringUtils.isBlank(xFor) || UNKNOWN.equalsIgnoreCase(xFor)) {  
			xFor = request.getHeader("HTTP_CLIENT_IP");  
		}  
		if (StringUtils.isBlank(xFor) || UNKNOWN.equalsIgnoreCase(xFor)) {  
			xFor = request.getHeader("HTTP_X_FORWARDED_FOR");  
		}  
		if (StringUtils.isBlank(xFor) || UNKNOWN.equalsIgnoreCase(xFor)) {  
			xFor = request.getRemoteAddr();  
		}  
		
		if ("localhost".equalsIgnoreCase(xFor) || "127.0.0.1".equalsIgnoreCase(xFor) || "0:0:0:0:0:0:0:1".equalsIgnoreCase(xFor)) {  
			return getLocalIp4Address();  
		}  
		return xFor;  
	} catch (Exception e) {  
		log.error("get remote ip error!", e);  
		return "x.0.0.1";  
	}  
}
```
   
2. 请求参数封装
   首先需要理解一下，为啥要封装请求参数？
   对于post之类的请求，若传参是json，那么需要从HttpServletRequest的请求流中读取，但是这个流是一次性的，如果打印日志的时候把这个参数读取出来了，那么在实际的业务中，就拿不到对应的参数了。
   为了解决这个问题，我们需要将这个InputStream进行封装一下，所以在技术派中定义了一个BodyReaderHttpServletRequestWrapper类，用来封装请求
   
   核心实现如下：
   1. 只拿post、put请求，非二进制、非文件上传、非表单数据上传的场景
   2. 将请求参数读取到`byte[] body`
   3. 基于body封装ServletInputStream，用于后续的传参获取

```java
/**  
 * post 流数据封装，避免因为打印日志导致请求参数被提前消费  
 *  
 * todo 知识点： 请求参数的封装，避免输入流读取一次就消耗了  
 *  
 * @author YiHui  
 * @date 2022/7/6  
 */
public class BodyReaderHttpServletRequestWrapper extends HttpServletRequestWrapper {  
    private static final List<String> POST_METHOD = Arrays.asList("POST", "PUT");  
    private final Logger logger = LoggerFactory.getLogger(this.getClass());  
  
    private final byte[] body;  
    private final String bodyString;  
  
    public BodyReaderHttpServletRequestWrapper(HttpServletRequest request) {  
        super(request);  
  
        if (POST_METHOD.contains(request.getMethod()) && !isMultipart(request) && !isBinaryContent(request) && !isFormPost(request)) {  
            bodyString = getBodyString(request);  
            body = bodyString.getBytes(StandardCharsets.UTF_8);  
        } else {  
            bodyString = null;  
            body = null;  
        }  
    }  
  
    @Override  
    public BufferedReader getReader() throws IOException {  
        return new BufferedReader(new InputStreamReader(getInputStream()));  
    }  
  
    @Override  
    public ServletInputStream getInputStream() throws IOException {  
        if (body == null) {  
            return super.getInputStream();  
        }  
  
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);  
        return new ServletInputStream() {  
            @Override  
            public int read() throws IOException {  
                return bais.read();  
            }  
  
            @Override  
            public boolean isFinished() {  
                return false;  
            }  
  
            @Override  
            public boolean isReady() {  
                return true;  
            }  
  
            @Override  
            public void setReadListener(ReadListener readListener) {  
            }  
        };  
    }  
  
    public boolean hasPayload() {  
        return bodyString != null;  
    }  
  
    public String getBodyString() {  
        return bodyString;  
    }  
  
    private String getBodyString(HttpServletRequest request) {  
        BufferedReader br;  
        try {  
            br = request.getReader();  
        } catch (IOException e) {  
            logger.warn("Failed to get reader", e);  
            return "";  
        }  
  
        String str;  
        StringBuilder body = new StringBuilder();  
        try {  
            while ((str = br.readLine()) != null) {  
                body.append(str);  
            }  
        } catch (IOException e) {  
            logger.warn("Failed to read line", e);  
        }  
  
        try {  
            br.close();  
        } catch (IOException e) {  
            logger.warn("Failed to close reader", e);  
        }  
  
        return body.toString();  
    }  
  
    /**  
     * is binary content     *     * @param request http request  
     * @return ret  
     */    private boolean isBinaryContent(final HttpServletRequest request) {  
        return request.getContentType() != null &&  
                (request.getContentType().startsWith("image") || request.getContentType().startsWith("video") ||  
                        request.getContentType().startsWith("audio"));  
    }  
  
    /**  
     * is multipart content     *     * @param request http request  
     * @return ret  
     */    private boolean isMultipart(final HttpServletRequest request) {  
        return request.getContentType() != null && request.getContentType().startsWith("multipart/form-data");  
    }  
  
    private boolean isFormPost(final HttpServletRequest request) {  
        return request.getContentType() != null && request.getContentType().startsWith("application/x-www-form-urlencoded");  
    }  
}
```
   

## 2.4、日志输出
最后在看一下日志输出，我们直接将上面封装的请求相关信息，按照具体的日志输出格式进行打印
```java
private void buildRequestLog(ReqInfoContext.ReqInfo req, HttpServletRequest request, long costTime) {  
    if (req == null || isStaticURI(request)) {  
        return;  
    }  
  
    StringBuilder msg = new StringBuilder();  
    msg.append("method=").append(request.getMethod()).append("; ");  
    if (StringUtils.isNotBlank(req.getReferer())) {  
        msg.append("referer=").append(URLDecoder.decode(req.getReferer())).append("; ");  
    }  
    msg.append("remoteIp=").append(req.getClientIp());  
    msg.append("; agent=").append(req.getUserAgent());  
  
    if (req.getUserId() != null) {  
        // 打印用户信息  
        msg.append("; user=").append(req.getUserId());  
    }  
  
    msg.append("; uri=").append(request.getRequestURI());  
    if (StringUtils.isNotBlank(request.getQueryString())) {  
        msg.append('?').append(URLDecoder.decode(request.getQueryString()));  
    }  
  
    msg.append("; payload=").append(req.getPayload());  
    msg.append("; cost=").append(costTime);  
    REQ_LOG.info("{}", msg);
}
```

## 2.5、小结
上面介绍了技术派中基于Filter实现的请求日志记录，将所有的外部请求都统一写到req日志文件中，可以基于此，查看一下当前项目的请求情况，接口耗时等，其中涉及到的知识点如下：
1. Filter基本使用姿势
2. Filter/AOP实现请求参数记录的方案
3. 如何从HttpServletRequest中获取你需要的请求参数
4. 请求参数的封装，允许请求参数InputStream的重复读取
5. 如何获取请求者的ip
6. 日志输出

上面这种请求参数的输出方案有一个潜在的风险（作者说的，但没有给出答案）




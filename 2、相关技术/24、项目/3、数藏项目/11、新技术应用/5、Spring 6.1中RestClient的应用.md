Spring Framework 6.1 M2 引入了 RestClient，一个新的同步HTTP客户端。顾名思义，RestClient 提供了 WebClient 的 fluent API和 RestTemplate 的基础架构。

只要依赖了 spring-boot-starter-web，版本在6.1.2以上即可使用。

我们的项目中，也用到了RestClient进行HTTP 调用。如：
```java
private ChainResponse  doPost(ChainRequest chainRequest, Long operateInfoId) {  
      
      
    RestClient restClient = RestClient.builder()  
            .baseUrl(chainRequest.getHost())  
            .build();  
      
    ChainResponse result = restClient.post()  
            .uri(chainRequest.getPath()).headers(  
                    headers -> configureHeaders(headers, chainRequest.getSignature(), chainRequest.getCurrentTime(), wenChangChainConfiguration.apiKey()))  
            .body(chainRequest.getBody())  
            .retrieve()  
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {  
                log.error("http client error, request: {}, response: {}", request, response);  
            }).onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {  
                log.error("http server error, request: {}, response: {}", request, response);  
            }).body(ChainResponse.class);  
      
    boolean updateResult = chainOperateInfoService.updateResult(operateInfoId, null,  
            result.getSuccess() ? result.getData().toString() : result.getError().toString());  
      
    if (!updateResult) {  
        throw new SystemException(RepoErrorCode.UPDATE_FAILED);  
    }  
    return result;  
}
```

通过 uri 指定 path，通过 body 指定请求体，通过 retrieve 进行请求发送，通过 onStatus对状态进行统一处理。

官方文档如下，大家可以参考使用：

﻿https://spring.io/blog/2023/07/13/new-in-spring-6-1-restclient

package com.bruceliu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author bruceliu
 * @create 2019-10-15 14:36
 * @description
 */
@EnableEurekaClient // 开启EurekaClient功能
@SpringBootApplication
public class ProviderAPP {

    public static void main(String[] args) {
        System.out.println("服务端生产者启动.....");
        SpringApplication.run(ProviderAPP.class,args);
    }

}

package com.bruceliu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author bruceliu
 * @create 2019-10-15 14:36
 * @description
 */
@SpringBootApplication
public class APP {

    public static void main(String[] args) {
        System.out.println("服务端生产者启动.....");
        SpringApplication.run(APP.class,args);
    }

}

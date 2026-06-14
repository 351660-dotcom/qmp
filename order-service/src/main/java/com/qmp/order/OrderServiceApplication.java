package com.qmp.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单编排中心启动类（C 端入口，对应 docs/07/09/10 文档第八节）。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.qmp.order.mapper")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

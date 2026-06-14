package com.qmp.marketing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 营销中心启动类（13 文档）。v1 优惠券中心。
 */
@SpringBootApplication
@MapperScan("com.qmp.marketing.mapper")
public class MarketingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketingServiceApplication.class, args);
    }
}

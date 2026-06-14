package com.qmp.dining;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 餐饮收银 POS 启动类（ADR-008/017，对应 docs/12 文档）。
 */
@SpringBootApplication
@MapperScan("com.qmp.dining.mapper")
public class DiningPosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiningPosServiceApplication.class, args);
    }
}

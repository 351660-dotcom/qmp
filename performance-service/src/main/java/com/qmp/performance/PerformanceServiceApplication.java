package com.qmp.performance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 演出/游船/游乐启动类（ADR-025，对应 docs/14 文档）。
 */
@SpringBootApplication
@MapperScan("com.qmp.performance.mapper")
public class PerformanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerformanceServiceApplication.class, args);
    }
}

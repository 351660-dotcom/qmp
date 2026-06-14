package com.qmp.reconciliation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 跨业态统一对账启动类（④）。
 */
@SpringBootApplication
@MapperScan("com.qmp.reconciliation.mapper")
public class ReconciliationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconciliationServiceApplication.class, args);
    }
}

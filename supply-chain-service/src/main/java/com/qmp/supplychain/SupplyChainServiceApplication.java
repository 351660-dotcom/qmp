package com.qmp.supplychain;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 供应链协同启动类（ADR-019，对应 docs/12 文档五/六）。
 */
@SpringBootApplication
@MapperScan("com.qmp.supplychain.mapper")
public class SupplyChainServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupplyChainServiceApplication.class, args);
    }
}

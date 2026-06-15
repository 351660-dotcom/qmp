package com.qmp.hotel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 酒店 PMS 启动类（ADR-007/025，对应 docs/11 文档）。
 * 在酒店域内扮演类似门票链路 order-service 的「编排者」角色。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.qmp.hotel.mapper")
public class HotelPmsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelPmsServiceApplication.class, args);
    }
}

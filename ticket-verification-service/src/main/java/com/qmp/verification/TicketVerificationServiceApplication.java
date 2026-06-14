package com.qmp.verification;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 核验中心启动类（门票全链路，对应 docs/07/09/10 文档）。
 */
@SpringBootApplication
@MapperScan("com.qmp.verification.mapper")
public class TicketVerificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketVerificationServiceApplication.class, args);
    }
}

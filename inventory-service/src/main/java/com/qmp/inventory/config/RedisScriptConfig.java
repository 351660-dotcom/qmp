package com.qmp.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 加载 inventory-kernel 提供的防超卖第一道防线 Lua 脚本（ADR-025 / 06 文档一）。
 */
@Configuration
public class RedisScriptConfig {

    @Bean
    public RedisScript<Long> inventoryReserveScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/inventory_reserve.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public RedisScript<Long> inventoryReleaseScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/inventory_release.lua"));
        script.setResultType(Long.class);
        return script;
    }
}

package com.qmp.kernel.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置：多租户 SQL 自动注入（{@link TenantLineHandlerImpl}）+ 乐观锁（{@code @Version}）+ 分页。
 * 雪花 ID（{@code IdType.ASSIGN_ID}）为 MyBatis-Plus 默认行为，无需额外配置（见 10 文档通用约定 1）。
 *
 * <p>拦截器顺序遵循 MyBatis-Plus 建议：多租户 → 乐观锁 → 分页（分页最后）。
 * 乐观锁拦截器为 {@code @Version} 字段（trade_order/room_reservation/performance_booking/sku_stock 等）
 * 填充 {@code MP_OPTLOCK_VERSION_ORIGINAL} 参数；缺少它会导致带 @Version 实体的 updateById 报
 * {@code Parameter 'MP_OPTLOCK_VERSION_ORIGINAL' not found}。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandlerImpl()));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}

package com.qmp.kernel.autoconfigure;

import com.qmp.kernel.context.RequestContextFilter;
import com.qmp.kernel.mybatis.MybatisPlusConfig;
import com.qmp.kernel.web.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * inventory-kernel 自动装配：依赖此模块的服务自动获得
 * 统一异常处理（{@link GlobalExceptionHandler}）、
 * MyBatis-Plus 分页/租户拦截器（{@link MybatisPlusConfig}）、
 * 租户/trace 上下文过滤器（{@link RequestContextFilter}），无需手写配置。
 */
@AutoConfiguration
@Import({MybatisPlusConfig.class, GlobalExceptionHandler.class})
public class KernelAutoConfiguration {

    @Bean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilter() {
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>(new RequestContextFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}

package com.qmp.kernel.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 在请求入口处解析 {@code X-Tenant-Id} / {@code X-Trace-Id} 请求头，
 * 写入 {@link TenantContext} / {@link TraceContext}，请求结束后清理，避免线程池复用导致串租户。
 */
public class RequestContextFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String tenantHeader = request.getHeader(TENANT_HEADER);
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                TenantContext.set(Long.valueOf(tenantHeader));
            }

            String traceHeader = request.getHeader(TRACE_HEADER);
            TraceContext.set(traceHeader != null && !traceHeader.isBlank() ? traceHeader : UUID.randomUUID().toString());

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            TraceContext.clear();
        }
    }
}

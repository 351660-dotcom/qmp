package com.qmp.kernel.web;

import com.qmp.kernel.context.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 后台管理接口（{@code /admin/**}）统一鉴权（ADR-011 开放平台鉴权的最小实现）。
 *
 * <p>仅当配置了 {@code admin.api.token}（环境变量 {@code ADMIN_API_TOKEN}）时启用：
 * 访问 {@code /admin/**} 必须带请求头 {@code X-Admin-Token} 且与配置值一致，否则返回 401。
 * 未配置时不拦截（便于本地开发/单测）。v1 为单一静态管理员令牌，后续可替换为基于角色的细粒度 RBAC。</p>
 */
public class AdminAuthFilter extends OncePerRequestFilter {

    public static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";
    private static final String ADMIN_PATH_PREFIX = "/admin/";

    private final String configuredToken;

    public AdminAuthFilter(String configuredToken) {
        this.configuredToken = configuredToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isEnabled() && isAdminPath(request) && !tokenMatches(request)) {
            writeUnauthorized(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isEnabled() {
        return configuredToken != null && !configuredToken.isBlank();
    }

    private boolean isAdminPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith(ADMIN_PATH_PREFIX);
    }

    private boolean tokenMatches(HttpServletRequest request) {
        return configuredToken.equals(request.getHeader(ADMIN_TOKEN_HEADER));
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        String traceId = TraceContext.get() != null ? TraceContext.get() : "";
        response.getWriter().write(
                "{\"code\":\"COMMON_UNAUTHORIZED\",\"message\":\"管理员鉴权失败\",\"data\":null,\"trace_id\":\""
                        + traceId + "\"}");
    }
}

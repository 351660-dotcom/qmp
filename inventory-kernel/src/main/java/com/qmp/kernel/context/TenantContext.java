package com.qmp.kernel.context;

/**
 * 当前请求的租户上下文（ThreadLocal）。
 * 由 {@link RequestContextFilter} 从 {@code X-Tenant-Id} 请求头注入，
 * 供 {@link com.qmp.kernel.mybatis.TenantLineHandlerImpl} 在 SQL 层自动注入 tenant_id 条件（ADR-021）。
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
    }

    public static Long get() {
        return CURRENT_TENANT_ID.get();
    }

    public static void clear() {
        CURRENT_TENANT_ID.remove();
    }
}

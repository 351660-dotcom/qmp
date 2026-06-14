package com.qmp.kernel.context;

/**
 * 当前请求的 trace_id 上下文（ThreadLocal），贯穿全链路日志与 {@link com.qmp.kernel.common.ApiResponse}。
 * 来自 API 网关注入的 {@code X-Trace-Id} 请求头（见 09 文档 1.1），本地开发缺省时自动生成。
 */
public final class TraceContext {

    private static final ThreadLocal<String> CURRENT_TRACE_ID = new ThreadLocal<>();

    private TraceContext() {
    }

    public static void set(String traceId) {
        CURRENT_TRACE_ID.set(traceId);
    }

    public static String get() {
        return CURRENT_TRACE_ID.get();
    }

    public static void clear() {
        CURRENT_TRACE_ID.remove();
    }
}

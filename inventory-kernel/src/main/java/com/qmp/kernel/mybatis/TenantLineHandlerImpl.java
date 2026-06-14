package com.qmp.kernel.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.qmp.kernel.context.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Set;

/**
 * MyBatis-Plus 多租户拦截器：从 {@link TenantContext} 读取当前租户，
 * 自动在 SQL 中追加 {@code tenant_id = ?} 条件（落地 10 文档「所有 DAO 查询强制带 tenant_id 条件」/ ADR-021）。
 *
 * <p>{@code processed_event} 表无 tenant_id 列（见 10 文档 9.1），需排除。</p>
 */
public class TenantLineHandlerImpl implements TenantLineHandler {

    private static final String TENANT_ID_COLUMN = "tenant_id";

    /** 不参与租户隔离的表：事件去重表按 (consumer_group, event_id) 主键，无 tenant_id 列。 */
    private static final Set<String> IGNORE_TABLES = Set.of("processed_event");

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.get();
        return new LongValue(tenantId != null ? tenantId : 0L);
    }

    @Override
    public String getTenantIdColumn() {
        return TENANT_ID_COLUMN;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return IGNORE_TABLES.contains(tableName.toLowerCase());
    }
}

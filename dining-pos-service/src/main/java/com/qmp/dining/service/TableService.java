package com.qmp.dining.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.dining.dto.admin.CreateTableRequest;
import com.qmp.dining.entity.DiningTable;
import com.qmp.dining.error.DiningErrorCode;
import com.qmp.dining.mapper.DiningTableMapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 桌台管理（12 文档 1.1）：后台建台、查询、状态流转。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableService {

    private final DiningTableMapper tableMapper;

    public Long createTable(CreateTableRequest request) {
        DiningTable table = new DiningTable();
        table.setTenantId(TenantContext.get());
        table.setMerchantId(request.getMerchantId());
        table.setAreaId(request.getAreaId());
        table.setTableNo(request.getTableNo());
        table.setCapacity(request.getCapacity());
        table.setStatus("IDLE");
        tableMapper.insert(table);
        log.info("后台建台: tableId={}, no={}", table.getTableId(), request.getTableNo());
        return table.getTableId();
    }

    public List<DiningTable> listTables(Long merchantId) {
        LambdaQueryWrapper<DiningTable> wrapper = new LambdaQueryWrapper<DiningTable>()
                .orderByAsc(DiningTable::getTableId);
        if (merchantId != null) {
            wrapper.eq(DiningTable::getMerchantId, merchantId);
        }
        return tableMapper.selectList(wrapper);
    }

    public DiningTable getOrThrow(Long tableId) {
        DiningTable table = tableMapper.selectById(tableId);
        if (table == null) {
            throw new BizException(DiningErrorCode.TABLE_NOT_FOUND);
        }
        return table;
    }

    public void updateTable(DiningTable table) {
        tableMapper.updateById(table);
    }
}

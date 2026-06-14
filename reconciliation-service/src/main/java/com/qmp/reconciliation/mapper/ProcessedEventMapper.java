package com.qmp.reconciliation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmp.reconciliation.entity.ProcessedEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessedEventMapper extends BaseMapper<ProcessedEvent> {
}

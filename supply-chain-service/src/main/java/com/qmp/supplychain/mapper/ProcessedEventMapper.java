package com.qmp.supplychain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmp.supplychain.entity.ProcessedEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessedEventMapper extends BaseMapper<ProcessedEvent> {
}

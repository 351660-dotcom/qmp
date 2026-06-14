package com.qmp.performance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmp.performance.entity.WristbandAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface WristbandAccountMapper extends BaseMapper<WristbandAccount> {

    @Update("UPDATE wristband_account SET balance = balance + #{amount} WHERE wristband_id = #{wristbandId}")
    int addBalance(@Param("wristbandId") Long wristbandId, @Param("amount") BigDecimal amount);

    @Update("UPDATE wristband_account SET balance = balance - #{amount} "
            + "WHERE wristband_id = #{wristbandId} AND balance >= #{amount}")
    int deductBalance(@Param("wristbandId") Long wristbandId, @Param("amount") BigDecimal amount);
}

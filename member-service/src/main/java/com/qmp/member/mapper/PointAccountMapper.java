package com.qmp.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmp.member.entity.PointAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PointAccountMapper extends BaseMapper<PointAccount> {

    /** 入账：余额原子增加。 */
    @Update("UPDATE point_account SET balance = balance + #{amount} WHERE user_id = #{userId}")
    int addBalance(@Param("userId") Long userId, @Param("amount") int amount);

    /** 扣减：条件更新防止扣为负（13 文档 1.3 / ADR-018）。 */
    @Update("UPDATE point_account SET balance = balance - #{amount} "
            + "WHERE user_id = #{userId} AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") int amount);
}

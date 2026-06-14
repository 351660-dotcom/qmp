package com.qmp.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmp.member.entity.MemberWallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface MemberWalletMapper extends BaseMapper<MemberWallet> {

    /** 充值/退款回补：余额原子增加。 */
    @Update("UPDATE member_wallet SET balance = balance + #{amount} WHERE user_id = #{userId}")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /** 消费扣减：条件更新防止扣为负。 */
    @Update("UPDATE member_wallet SET balance = balance - #{amount} "
            + "WHERE user_id = #{userId} AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}

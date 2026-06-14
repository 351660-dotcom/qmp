package com.qmp.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.member.entity.MemberAccount;
import com.qmp.member.entity.MemberLevel;
import com.qmp.member.entity.PointAccount;
import com.qmp.member.entity.PointLedger;
import com.qmp.member.error.MemberErrorCode;
import com.qmp.member.mapper.MemberAccountMapper;
import com.qmp.member.mapper.MemberLevelMapper;
import com.qmp.member.mapper.PointAccountMapper;
import com.qmp.member.mapper.PointLedgerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 积分服务（13 文档 1.3）：入账（EARN）、抵扣（REDEEM）、查询。
 *
 * <p>幂等：账本 {@code (source_ref, type)} 唯一约束 + 事务回滚——预检命中已处理则跳过，
 * 并发重复时唯一键冲突触发回滚由 MQ 重投后预检兜住。余额扣减走条件更新防止扣为负。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointAccountMapper pointAccountMapper;
    private final PointLedgerMapper pointLedgerMapper;
    private final MemberAccountMapper memberAccountMapper;
    private final MemberLevelMapper memberLevelMapper;

    /** 消费入账。幂等键 (source_ref, EARN)。同时累加成长值并按等级阈值升级。 */
    @Transactional
    public void earn(Long userId, int points, Long sourceMerchantId, String sourceRef) {
        if (points <= 0) {
            return;
        }
        if (ledgerExists(sourceRef, "EARN")) {
            return;
        }
        Long tenantId = TenantContext.get();
        ensureAccount(userId, tenantId);
        pointAccountMapper.addBalance(userId, points);
        int balanceAfter = pointAccountMapper.selectById(userId).getBalance();
        insertLedger(tenantId, userId, points, balanceAfter, "EARN", sourceMerchantId, sourceRef);
        updateGrowthAndLevel(userId, points);
        log.info("积分入账: userId={}, +{}, balance={}, ref={}", userId, points, balanceAfter, sourceRef);
    }

    /** 抵扣核销。幂等键 (source_ref, REDEEM)；余额不足抛 MEMBER_INSUFFICIENT_POINTS。 */
    @Transactional
    public int redeem(Long userId, int points, Long merchantId, String sourceRef) {
        if (points <= 0) {
            throw new BizException(MemberErrorCode.INVALID_AMOUNT);
        }
        PointLedger existing = findLedger(sourceRef, "REDEEM");
        if (existing != null) {
            return existing.getBalanceAfter();
        }
        if (pointAccountMapper.deductBalance(userId, points) == 0) {
            throw new BizException(MemberErrorCode.INSUFFICIENT_POINTS);
        }
        int balanceAfter = pointAccountMapper.selectById(userId).getBalance();
        insertLedger(TenantContext.get(), userId, -points, balanceAfter, "REDEEM", merchantId, sourceRef);
        log.info("积分抵扣: userId={}, -{}, balance={}, ref={}", userId, points, balanceAfter, sourceRef);
        return balanceAfter;
    }

    public int getBalance(Long userId) {
        PointAccount account = pointAccountMapper.selectById(userId);
        return account != null && account.getBalance() != null ? account.getBalance() : 0;
    }

    // ------------------------------------------------------------------
    private void ensureAccount(Long userId, Long tenantId) {
        if (pointAccountMapper.selectById(userId) == null) {
            PointAccount account = new PointAccount();
            account.setUserId(userId);
            account.setTenantId(tenantId);
            account.setBalance(0);
            pointAccountMapper.insert(account);
        }
    }

    private void updateGrowthAndLevel(Long userId, int points) {
        MemberAccount member = memberAccountMapper.selectById(userId);
        if (member == null) {
            return; // 无会员档案则只记积分，不做等级
        }
        int growth = (member.getGrowthValue() != null ? member.getGrowthValue() : 0) + points;
        member.setGrowthValue(growth);
        List<MemberLevel> levels = memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .orderByDesc(MemberLevel::getMinGrowthValue));
        for (MemberLevel level : levels) {
            if (growth >= level.getMinGrowthValue()) {
                member.setLevelId(level.getLevelId());
                break;
            }
        }
        memberAccountMapper.updateById(member);
    }

    private boolean ledgerExists(String sourceRef, String type) {
        return findLedger(sourceRef, type) != null;
    }

    private PointLedger findLedger(String sourceRef, String type) {
        return pointLedgerMapper.selectOne(new LambdaQueryWrapper<PointLedger>()
                .eq(PointLedger::getSourceRef, sourceRef)
                .eq(PointLedger::getType, type)
                .last("LIMIT 1"));
    }

    private void insertLedger(Long tenantId, Long userId, int change, int balanceAfter,
                              String type, Long merchantId, String sourceRef) {
        PointLedger ledger = new PointLedger();
        ledger.setTenantId(tenantId);
        ledger.setUserId(userId);
        ledger.setChangeAmount(change);
        ledger.setBalanceAfter(balanceAfter);
        ledger.setType(type);
        ledger.setSourceMerchantId(merchantId);
        ledger.setSourceRef(sourceRef);
        pointLedgerMapper.insert(ledger);
    }
}

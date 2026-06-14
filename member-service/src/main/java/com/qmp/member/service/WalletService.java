package com.qmp.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.member.entity.MemberWallet;
import com.qmp.member.entity.WalletLedger;
import com.qmp.member.error.MemberErrorCode;
import com.qmp.member.mapper.MemberWalletMapper;
import com.qmp.member.mapper.WalletLedgerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 储值服务（13 文档 1.4）：充值（RECHARGE）、消费（CONSUME，即 12 文档 DeductWallet）、查询。
 * 幂等键 (source_ref, type)；消费走条件更新防止扣为负。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final MemberWalletMapper walletMapper;
    private final WalletLedgerMapper walletLedgerMapper;

    @Transactional
    public BigDecimal recharge(Long userId, BigDecimal amount, String sourceRef) {
        requirePositive(amount);
        WalletLedger existing = findLedger(sourceRef, "RECHARGE");
        if (existing != null) {
            return existing.getBalanceAfter();
        }
        Long tenantId = TenantContext.get();
        ensureWallet(userId, tenantId);
        walletMapper.addBalance(userId, amount);
        BigDecimal balanceAfter = walletMapper.selectById(userId).getBalance();
        insertLedger(tenantId, userId, amount, balanceAfter, "RECHARGE", null, sourceRef);
        log.info("储值充值: userId={}, +{}, balance={}, ref={}", userId, amount, balanceAfter, sourceRef);
        return balanceAfter;
    }

    /** 12 文档 DeductWallet：扣减储值用于消费支付。 */
    @Transactional
    public BigDecimal deduct(Long userId, BigDecimal amount, Long merchantId, String sourceRef) {
        requirePositive(amount);
        WalletLedger existing = findLedger(sourceRef, "CONSUME");
        if (existing != null) {
            return existing.getBalanceAfter();
        }
        if (walletMapper.deductBalance(userId, amount) == 0) {
            throw new BizException(MemberErrorCode.INSUFFICIENT_BALANCE);
        }
        BigDecimal balanceAfter = walletMapper.selectById(userId).getBalance();
        insertLedger(TenantContext.get(), userId, amount.negate(), balanceAfter, "CONSUME", merchantId, sourceRef);
        log.info("储值消费: userId={}, -{}, balance={}, ref={}", userId, amount, balanceAfter, sourceRef);
        return balanceAfter;
    }

    public BigDecimal getBalance(Long userId) {
        MemberWallet wallet = walletMapper.selectById(userId);
        return wallet != null && wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
    }

    // ------------------------------------------------------------------
    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(MemberErrorCode.INVALID_AMOUNT);
        }
    }

    private void ensureWallet(Long userId, Long tenantId) {
        if (walletMapper.selectById(userId) == null) {
            MemberWallet wallet = new MemberWallet();
            wallet.setUserId(userId);
            wallet.setTenantId(tenantId);
            wallet.setBalance(BigDecimal.ZERO);
            walletMapper.insert(wallet);
        }
    }

    private WalletLedger findLedger(String sourceRef, String type) {
        return walletLedgerMapper.selectOne(new LambdaQueryWrapper<WalletLedger>()
                .eq(WalletLedger::getSourceRef, sourceRef)
                .eq(WalletLedger::getType, type)
                .last("LIMIT 1"));
    }

    private void insertLedger(Long tenantId, Long userId, BigDecimal change, BigDecimal balanceAfter,
                             String type, Long merchantId, String sourceRef) {
        WalletLedger ledger = new WalletLedger();
        ledger.setTenantId(tenantId);
        ledger.setUserId(userId);
        ledger.setChangeAmount(change);
        ledger.setBalanceAfter(balanceAfter);
        ledger.setType(type);
        ledger.setMerchantId(merchantId);
        ledger.setSourceRef(sourceRef);
        walletLedgerMapper.insert(ledger);
    }
}

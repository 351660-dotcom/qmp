package com.qmp.performance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.performance.dto.IssueWristbandRequest;
import com.qmp.performance.dto.WristbandView;
import com.qmp.performance.entity.WristbandAccount;
import com.qmp.performance.entity.WristbandLedger;
import com.qmp.performance.error.PerformanceErrorCode;
import com.qmp.performance.mapper.WristbandAccountMapper;
import com.qmp.performance.mapper.WristbandLedgerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 手牌/腕带二次消费账户（14 文档四）：办理、充值、消费、查询。
 * 幂等键 {@code (source_ref, type)}；消费走条件更新防负。接口与会员储值同构。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WristbandService {

    private final WristbandAccountMapper accountMapper;
    private final WristbandLedgerMapper ledgerMapper;

    @Transactional
    public WristbandView issue(IssueWristbandRequest request) {
        WristbandAccount account = new WristbandAccount();
        account.setTenantId(TenantContext.get());
        account.setScenicId(request.getScenicId());
        account.setUserId(request.getUserId());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus("ACTIVE");
        accountMapper.insert(account);

        BigDecimal initial = request.getInitialAmount();
        if (initial != null && initial.compareTo(BigDecimal.ZERO) > 0) {
            accountMapper.addBalance(account.getWristbandId(), initial);
            insertLedger(account.getWristbandId(), initial, initial, "RECHARGE", null,
                    "ISSUE:" + account.getWristbandId());
            account.setBalance(initial);
        }
        log.info("办理手牌: wristbandId={}, initial={}", account.getWristbandId(), initial);
        return view(account);
    }

    @Transactional
    public BigDecimal recharge(Long wristbandId, BigDecimal amount, String sourceRef) {
        requirePositive(amount);
        getOrThrow(wristbandId);
        if (ledgerExists(sourceRef, "RECHARGE")) {
            return getOrThrow(wristbandId).getBalance();
        }
        accountMapper.addBalance(wristbandId, amount);
        BigDecimal balanceAfter = getOrThrow(wristbandId).getBalance();
        insertLedger(wristbandId, amount, balanceAfter, "RECHARGE", null, sourceRef);
        return balanceAfter;
    }

    /** 二次消费扣减（dining-pos 结账「手牌支付」可调，14 文档 4.2 / 与 DeductWallet 同构）。 */
    @Transactional
    public BigDecimal consume(Long wristbandId, BigDecimal amount, Long merchantId, String sourceRef) {
        requirePositive(amount);
        getOrThrow(wristbandId);
        WristbandLedger existing = findLedger(sourceRef, "CONSUME");
        if (existing != null) {
            return existing.getBalanceAfter();
        }
        if (accountMapper.deductBalance(wristbandId, amount) == 0) {
            throw new BizException(PerformanceErrorCode.INSUFFICIENT_WRISTBAND);
        }
        BigDecimal balanceAfter = getOrThrow(wristbandId).getBalance();
        insertLedger(wristbandId, amount.negate(), balanceAfter, "CONSUME", merchantId, sourceRef);
        return balanceAfter;
    }

    public WristbandView getView(Long wristbandId) {
        return view(getOrThrow(wristbandId));
    }

    // ------------------------------------------------------------------
    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(PerformanceErrorCode.INVALID_AMOUNT);
        }
    }

    private WristbandAccount getOrThrow(Long wristbandId) {
        WristbandAccount account = accountMapper.selectById(wristbandId);
        if (account == null) {
            throw new BizException(PerformanceErrorCode.WRISTBAND_NOT_FOUND);
        }
        return account;
    }

    private boolean ledgerExists(String sourceRef, String type) {
        return findLedger(sourceRef, type) != null;
    }

    private WristbandLedger findLedger(String sourceRef, String type) {
        return ledgerMapper.selectOne(new LambdaQueryWrapper<WristbandLedger>()
                .eq(WristbandLedger::getSourceRef, sourceRef)
                .eq(WristbandLedger::getType, type)
                .last("LIMIT 1"));
    }

    private void insertLedger(Long wristbandId, BigDecimal change, BigDecimal balanceAfter,
                              String type, Long merchantId, String sourceRef) {
        WristbandLedger ledger = new WristbandLedger();
        ledger.setTenantId(TenantContext.get());
        ledger.setWristbandId(wristbandId);
        ledger.setChangeAmount(change);
        ledger.setBalanceAfter(balanceAfter);
        ledger.setType(type);
        ledger.setMerchantId(merchantId);
        ledger.setSourceRef(sourceRef);
        ledgerMapper.insert(ledger);
    }

    private WristbandView view(WristbandAccount a) {
        return WristbandView.builder()
                .wristbandId(a.getWristbandId())
                .userId(a.getUserId())
                .balance(a.getBalance())
                .status(a.getStatus())
                .build();
    }
}

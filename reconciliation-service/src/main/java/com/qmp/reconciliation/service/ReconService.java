package com.qmp.reconciliation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.reconciliation.dto.DailySummaryResponse;
import com.qmp.reconciliation.entity.ReconTransaction;
import com.qmp.reconciliation.mapper.ReconTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一对账核心：记录跨业态资金流水，按日/商户汇总。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconService {

    private final ReconTransactionMapper txnMapper;

    public void record(Long merchantId, String source, String direction, String bizRef,
                       BigDecimal amount, String channel, LocalDateTime occurredAt) {
        LocalDateTime when = occurredAt != null ? occurredAt : LocalDateTime.now();
        ReconTransaction txn = new ReconTransaction();
        txn.setTenantId(TenantContext.get());
        txn.setMerchantId(merchantId);
        txn.setSource(source);
        txn.setDirection(direction);
        txn.setBizRef(bizRef);
        txn.setAmount(amount);
        txn.setChannel(channel);
        txn.setOccurredAt(when);
        txn.setReconDate(when.toLocalDate());
        txnMapper.insert(txn);
        log.info("对账入账: source={}, dir={}, merchant={}, amount={}, ref={}",
                source, direction, merchantId, amount, bizRef);
    }

    public DailySummaryResponse dailySummary(LocalDate reconDate, Long merchantId) {
        LambdaQueryWrapper<ReconTransaction> wrapper = new LambdaQueryWrapper<ReconTransaction>()
                .eq(ReconTransaction::getReconDate, reconDate);
        if (merchantId != null) {
            wrapper.eq(ReconTransaction::getMerchantId, merchantId);
        }
        List<ReconTransaction> txns = txnMapper.selectList(wrapper);

        BigDecimal inTotal = BigDecimal.ZERO;
        BigDecimal outTotal = BigDecimal.ZERO;
        Map<String, BigDecimal> bySource = new LinkedHashMap<>();
        for (ReconTransaction t : txns) {
            BigDecimal amt = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
            if ("OUT".equals(t.getDirection())) {
                outTotal = outTotal.add(amt);
            } else {
                inTotal = inTotal.add(amt);
            }
            bySource.merge(t.getSource(), amt, BigDecimal::add);
        }
        return DailySummaryResponse.builder()
                .reconDate(reconDate)
                .merchantId(merchantId)
                .inTotal(inTotal)
                .outTotal(outTotal)
                .net(inTotal.subtract(outTotal))
                .count(txns.size())
                .bySource(bySource)
                .build();
    }

    public List<ReconTransaction> listTransactions(LocalDate reconDate, Long merchantId) {
        LambdaQueryWrapper<ReconTransaction> wrapper = new LambdaQueryWrapper<ReconTransaction>()
                .eq(ReconTransaction::getReconDate, reconDate)
                .orderByDesc(ReconTransaction::getTxnId)
                .last("LIMIT 500");
        if (merchantId != null) {
            wrapper.eq(ReconTransaction::getMerchantId, merchantId);
        }
        return txnMapper.selectList(wrapper);
    }
}

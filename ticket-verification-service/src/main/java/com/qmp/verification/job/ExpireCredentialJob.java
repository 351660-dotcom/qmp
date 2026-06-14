package com.qmp.verification.job;

import com.qmp.kernel.context.TenantContext;
import com.qmp.verification.entity.TicketCredential;
import com.qmp.verification.service.CredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 凭证过期定时任务（07 文档 2.4 状态机 {@code UNUSED -> EXPIRED}）：扫描游玩日已过
 * （{@code sale_date < today}）仍 UNUSED 的凭证，按 no-show 置为 EXPIRED（不退款、不释放库存）。
 *
 * <p>「today」取服务端（容器，通常 UTC）时区当日，与凭证 {@code sale_date} 同口径；营业日切配置后续可补。
 * 多租户行级拦截下单次查询只能扫一个租户，故按 {@code verification.expire-job.tenant-ids}
 * 逐租户扫描（默认门票黄金路径示例租户 1001），与 inventory/order 的扫描任务同构。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpireCredentialJob {

    private final CredentialService credentialService;

    @Value("${verification.expire-job.tenant-ids:1001}")
    private List<Long> tenantIds;

    @Scheduled(fixedDelayString = "${verification.expire-job.fixed-delay-ms:60000}")
    public void run() {
        LocalDate today = LocalDate.now();
        for (Long tenantId : tenantIds) {
            TenantContext.set(tenantId);
            try {
                for (TicketCredential credential : credentialService.findExpirableCredentials(today)) {
                    credentialService.expireCredential(credential.getCredentialId());
                }
            } catch (Exception e) {
                log.error("凭证过期任务执行异常: tenantId={}", tenantId, e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}

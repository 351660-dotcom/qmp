package com.qmp.performance.job;

import com.qmp.kernel.context.TenantContext;
import com.qmp.performance.entity.PerformanceBooking;
import com.qmp.performance.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 演出/游船/游乐预订超时关单任务：扫描创建已超 {@code performance.reservation.hold-minutes}
 * 仍 PENDING_PAYMENT 的预订，释放场次/座位预占并置 CANCELLED。与门票 order / 酒店预订的关单任务同构。
 *
 * <p>多租户行级拦截下单次查询只能扫一个租户，故按 {@code performance.cancel-job.tenant-ids}（默认 1001）
 * 逐租户扫描；扫描间隔 {@code performance.cancel-job.fixed-delay-ms}（默认 60s）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PerformanceExpireBookingJob {

    private final BookingService bookingService;

    @Value("${performance.reservation.hold-minutes:15}")
    private long holdMinutes;

    @Value("${performance.cancel-job.tenant-ids:1001}")
    private List<Long> tenantIds;

    @Scheduled(fixedDelayString = "${performance.cancel-job.fixed-delay-ms:60000}")
    public void run() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(holdMinutes);
        for (Long tenantId : tenantIds) {
            TenantContext.set(tenantId);
            try {
                List<PerformanceBooking> expired = bookingService.findExpiredPendingBookings(cutoff);
                for (PerformanceBooking booking : expired) {
                    bookingService.cancelExpiredBooking(booking.getBookingId());
                }
            } catch (Exception e) {
                log.error("演出超时关单任务执行异常: tenantId={}", tenantId, e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}

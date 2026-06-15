package com.qmp.hotel.job;

import com.qmp.hotel.entity.RoomReservation;
import com.qmp.hotel.service.HotelReservationService;
import com.qmp.kernel.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 酒店预订超时关单任务（11 文档 v1 缺口「未支付预订超时释放」）：扫描创建已超
 * {@code hotel.reservation.hold-minutes} 仍 PENDING_PAYMENT 的预订，释放连住各晚预占并置 CANCELLED。
 *
 * <p>与门票 order-service 的 CancelExpiredOrderJob 同构。多租户行级拦截下单次查询只能扫一个租户，
 * 故按 {@code hotel.cancel-job.tenant-ids}（默认 1001）逐租户扫描；扫描间隔
 * {@code hotel.cancel-job.fixed-delay-ms}（默认 60s）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotelExpireReservationJob {

    private final HotelReservationService reservationService;

    @Value("${hotel.reservation.hold-minutes:30}")
    private long holdMinutes;

    @Value("${hotel.cancel-job.tenant-ids:1001}")
    private List<Long> tenantIds;

    @Scheduled(fixedDelayString = "${hotel.cancel-job.fixed-delay-ms:60000}")
    public void run() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(holdMinutes);
        for (Long tenantId : tenantIds) {
            TenantContext.set(tenantId);
            try {
                List<RoomReservation> expired = reservationService.findExpiredPendingReservations(cutoff);
                for (RoomReservation reservation : expired) {
                    reservationService.cancelExpiredReservation(reservation.getReservationId());
                }
            } catch (Exception e) {
                log.error("酒店超时关单任务执行异常: tenantId={}", tenantId, e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}

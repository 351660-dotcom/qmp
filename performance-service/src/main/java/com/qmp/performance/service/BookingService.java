package com.qmp.performance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import com.qmp.performance.client.PaymentClient;
import com.qmp.performance.dto.BookSeatRequest;
import com.qmp.performance.dto.BookSessionRequest;
import com.qmp.performance.dto.BookingResponse;
import com.qmp.performance.dto.PayResponse;
import com.qmp.performance.entity.PerformanceBooking;
import com.qmp.performance.entity.PerformanceReservation;
import com.qmp.performance.entity.PerformanceSession;
import com.qmp.performance.entity.SeatInventoryBucket;
import com.qmp.performance.entity.SessionInventoryBucket;
import com.qmp.performance.error.PerformanceErrorCode;
import com.qmp.performance.mapper.PerformanceBookingMapper;
import com.qmp.performance.mapper.PerformanceReservationMapper;
import com.qmp.performance.mapper.PerformanceSessionMapper;
import com.qmp.performance.mapper.SeatInventoryBucketMapper;
import com.qmp.performance.mapper.SessionInventoryBucketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 演出/游船/游乐预订编排（14 文档一/二）。场次库存与座位库存复用同一套防超卖机制（按 bucket_ref 分桶表）。
 * 自包含编排（类比 hotel），booking_id 作 payment 的 order_id，共用 PaymentSucceeded 主题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final String REF_SESSION = "SESSION";
    private static final String REF_SEAT = "SEAT";

    private final PerformanceSessionMapper sessionMapper;
    private final SessionInventoryBucketMapper sessionBucketMapper;
    private final SeatInventoryBucketMapper seatBucketMapper;
    private final PerformanceReservationMapper reservationMapper;
    private final PerformanceBookingMapper bookingMapper;
    private final PaymentClient paymentClient;

    @Value("${performance.reservation.hold-minutes:15}")
    private int holdMinutes;

    @Transactional
    public BookingResponse bookSession(BookSessionRequest request) {
        PerformanceSession session = onSaleSession(request.getSessionId());
        SessionInventoryBucket bucket = sessionBucketMapper.selectOne(
                new LambdaQueryWrapper<SessionInventoryBucket>()
                        .eq(SessionInventoryBucket::getSessionId, request.getSessionId()));
        if (bucket == null) {
            throw new BizException(PerformanceErrorCode.BUCKET_NOT_FOUND);
        }
        int qty = request.getQuantity() != null ? request.getQuantity() : 1;
        long bookingId = IdWorker.getId();
        if (sessionBucketMapper.tryLock(bucket.getBucketId(), qty) == 0) {
            throw new BizException(PerformanceErrorCode.INSUFFICIENT);
        }
        insertReservation(bookingId, session.getSessionId(), null, REF_SESSION, bucket.getBucketId(), qty);

        BigDecimal amount = session.getBasePrice().multiply(BigDecimal.valueOf(qty));
        PerformanceBooking booking = newBooking(bookingId, session, request.getUserId(), REF_SESSION, null, qty, amount);
        bookingMapper.insert(booking);
        log.info("场次预订: bookingId={}, sessionId={}, qty={}", bookingId, session.getSessionId(), qty);
        return view(booking);
    }

    @Transactional
    public BookingResponse bookSeat(BookSeatRequest request) {
        PerformanceSession session = onSaleSession(request.getSessionId());
        SeatInventoryBucket bucket = seatBucketMapper.selectOne(new LambdaQueryWrapper<SeatInventoryBucket>()
                .eq(SeatInventoryBucket::getSessionId, request.getSessionId())
                .eq(SeatInventoryBucket::getSeatId, request.getSeatId()));
        if (bucket == null) {
            throw new BizException(PerformanceErrorCode.BUCKET_NOT_FOUND);
        }
        long bookingId = IdWorker.getId();
        if (seatBucketMapper.tryLock(bucket.getBucketId(), 1) == 0) {
            throw new BizException(PerformanceErrorCode.INSUFFICIENT);
        }
        insertReservation(bookingId, session.getSessionId(), request.getSeatId(), REF_SEAT, bucket.getBucketId(), 1);

        PerformanceBooking booking = newBooking(bookingId, session, request.getUserId(), REF_SEAT,
                request.getSeatId(), 1, session.getBasePrice());
        bookingMapper.insert(booking);
        log.info("选座预订: bookingId={}, sessionId={}, seatId={}", bookingId, session.getSessionId(), request.getSeatId());
        return view(booking);
    }

    public PayResponse pay(Long bookingId, String channel) {
        PerformanceBooking booking = getOrThrow(bookingId);
        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            throw new BizException(PerformanceErrorCode.BOOKING_INVALID_STATE);
        }
        PaymentClient.PaymentView payment = paymentClient.createPayment(
                bookingId, booking.getTenantId(), booking.getMerchantId(), booking.getTotalAmount(), channel);
        booking.setPaymentId(payment.getPaymentId());
        bookingMapper.updateById(booking);
        return PayResponse.builder().paymentId(payment.getPaymentId()).payParams(payment.getPayParams()).build();
    }

    public BookingResponse getBooking(Long bookingId) {
        return view(getOrThrow(bookingId));
    }

    @Transactional
    public void cancel(Long bookingId) {
        PerformanceBooking booking = getOrThrow(bookingId);
        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            throw new BizException(PerformanceErrorCode.BOOKING_INVALID_STATE);
        }
        releaseReservations(bookingId);
        booking.setStatus("CANCELLED");
        bookingMapper.updateById(booking);
    }

    /** 扫描当前租户下创建已超 {@code cutoff} 仍未支付的预订（供 PerformanceExpireBookingJob 调用）。 */
    public List<PerformanceBooking> findExpiredPendingBookings(LocalDateTime cutoff) {
        return bookingMapper.selectList(new LambdaQueryWrapper<PerformanceBooking>()
                .eq(PerformanceBooking::getStatus, "PENDING_PAYMENT")
                .lt(PerformanceBooking::getCreatedAt, cutoff));
    }

    /**
     * 关闭一笔超时未支付预订：释放场次/座位预占（幂等）+ 置 CANCELLED。
     * 重读校验状态 + @Version 乐观锁，防与支付成功并发误关。
     */
    @Transactional
    public void cancelExpiredBooking(Long bookingId) {
        PerformanceBooking booking = bookingMapper.selectById(bookingId);
        if (booking == null || !"PENDING_PAYMENT".equals(booking.getStatus())) {
            return; // 已支付/已取消，幂等跳过
        }
        releaseReservations(bookingId);
        booking.setStatus("CANCELLED");
        bookingMapper.updateById(booking);
        log.info("预订单超时未支付，已关闭: bookingId={}", bookingId);
    }

    /** 由 PaymentSucceeded 消费侧调用：确认预占并置预订单 CONFIRMED（幂等）。 */
    @Transactional
    public void confirmPaid(Long bookingId, String paymentId) {
        PerformanceBooking booking = bookingMapper.selectById(bookingId);
        if (booking == null || !"PENDING_PAYMENT".equals(booking.getStatus())) {
            return; // 非本域订单或已处理
        }
        for (PerformanceReservation r : listReservations(bookingId)) {
            if ("HOLDING".equals(r.getStatus())) {
                if (REF_SEAT.equals(r.getBucketRef())) {
                    seatBucketMapper.confirmLock(r.getBucketId(), r.getQuantity());
                } else {
                    sessionBucketMapper.confirmLock(r.getBucketId(), r.getQuantity());
                }
                r.setStatus("CONFIRMED");
                reservationMapper.updateById(r);
            }
        }
        booking.setStatus("CONFIRMED");
        booking.setPaymentId(paymentId);
        bookingMapper.updateById(booking);
        log.info("预订单支付成功已确认: bookingId={}", bookingId);
    }

    // ------------------------------------------------------------------
    private void releaseReservations(Long bookingId) {
        for (PerformanceReservation r : listReservations(bookingId)) {
            boolean seat = REF_SEAT.equals(r.getBucketRef());
            switch (r.getStatus()) {
                case "HOLDING" -> {
                    if (seat) {
                        seatBucketMapper.releaseLock(r.getBucketId(), r.getQuantity());
                    } else {
                        sessionBucketMapper.releaseLock(r.getBucketId(), r.getQuantity());
                    }
                    r.setStatus("RELEASED");
                    reservationMapper.updateById(r);
                }
                case "CONFIRMED" -> {
                    if (seat) {
                        seatBucketMapper.releaseSold(r.getBucketId(), r.getQuantity());
                    } else {
                        sessionBucketMapper.releaseSold(r.getBucketId(), r.getQuantity());
                    }
                    r.setStatus("RELEASED");
                    reservationMapper.updateById(r);
                }
                default -> { /* 幂等跳过 */ }
            }
        }
    }

    private void insertReservation(long bookingId, Long sessionId, String seatId, String bucketRef,
                                   Long bucketId, int qty) {
        PerformanceReservation r = new PerformanceReservation();
        r.setReservationId(bookingId + ":" + (seatId != null ? seatId : REF_SESSION));
        r.setTenantId(TenantContext.get());
        r.setPerformanceBookingId(bookingId);
        r.setSessionId(sessionId);
        r.setSeatId(seatId);
        r.setBucketRef(bucketRef);
        r.setBucketId(bucketId);
        r.setQuantity(qty);
        r.setStatus("HOLDING");
        r.setHoldExpireAt(LocalDateTime.now().plusMinutes(holdMinutes));
        reservationMapper.insert(r);
    }

    private PerformanceBooking newBooking(long bookingId, PerformanceSession session, Long userId,
                                          String bucketRef, String seatId, int qty, BigDecimal amount) {
        PerformanceBooking booking = new PerformanceBooking();
        booking.setBookingId(bookingId);
        booking.setTenantId(TenantContext.get());
        booking.setScenicId(session.getScenicId());
        booking.setMerchantId(session.getMerchantId());
        booking.setUserId(userId);
        booking.setSessionId(session.getSessionId());
        booking.setBucketRef(bucketRef);
        booking.setSeatId(seatId);
        booking.setQuantity(qty);
        booking.setStatus("PENDING_PAYMENT");
        booking.setTotalAmount(amount);
        booking.setVersion(0);
        return booking;
    }

    private PerformanceSession onSaleSession(Long sessionId) {
        PerformanceSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(PerformanceErrorCode.SESSION_NOT_FOUND);
        }
        if (!"ON_SALE".equals(session.getStatus())) {
            throw new BizException(PerformanceErrorCode.SESSION_NOT_ON_SALE);
        }
        return session;
    }

    private PerformanceBooking getOrThrow(Long bookingId) {
        PerformanceBooking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BizException(PerformanceErrorCode.BOOKING_NOT_FOUND);
        }
        return booking;
    }

    private List<PerformanceReservation> listReservations(Long bookingId) {
        return reservationMapper.selectList(new LambdaQueryWrapper<PerformanceReservation>()
                .eq(PerformanceReservation::getPerformanceBookingId, bookingId));
    }

    private BookingResponse view(PerformanceBooking b) {
        return BookingResponse.builder()
                .bookingId(b.getBookingId())
                .status(b.getStatus())
                .sessionId(b.getSessionId())
                .bucketRef(b.getBucketRef())
                .seatId(b.getSeatId())
                .quantity(b.getQuantity())
                .totalAmount(b.getTotalAmount())
                .paymentId(b.getPaymentId())
                .build();
    }
}

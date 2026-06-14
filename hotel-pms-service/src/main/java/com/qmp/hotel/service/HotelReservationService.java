package com.qmp.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.qmp.hotel.client.PaymentClient;
import com.qmp.hotel.dto.CreateReservationRequest;
import com.qmp.hotel.dto.PayResponse;
import com.qmp.hotel.dto.ReservationResponse;
import com.qmp.hotel.entity.RoomReservation;
import com.qmp.hotel.entity.RoomType;
import com.qmp.hotel.error.HotelErrorCode;
import com.qmp.hotel.mapper.RoomReservationMapper;
import com.qmp.hotel.mapper.RoomTypeMapper;
import com.qmp.kernel.common.BizException;
import com.qmp.kernel.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 预订单编排服务（11 文档 2.x）：创建预订（多夜原子预占）、发起支付、查询、取消，
 * 以及支付成功后确认预订（由 PaymentSucceeded 消费侧调用）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotelReservationService {

    private final RoomTypeMapper roomTypeMapper;
    private final RoomReservationMapper reservationMapper;
    private final HotelInventoryService hotelInventoryService;
    private final PaymentClient paymentClient;

    public ReservationResponse createReservation(CreateReservationRequest request) {
        int roomCount = request.getRoomCount() != null ? request.getRoomCount() : 1;

        RoomType roomType = roomTypeMapper.selectOne(new LambdaQueryWrapper<RoomType>()
                .eq(RoomType::getSkuId, request.getSkuId()));
        if (roomType == null) {
            throw new BizException(HotelErrorCode.ROOM_TYPE_NOT_FOUND);
        }
        if (!"ON_SALE".equals(roomType.getStatus())) {
            throw new BizException(HotelErrorCode.ROOM_TYPE_NOT_ON_SALE);
        }

        List<LocalDate> nights = hotelInventoryService.nightsOf(request.getCheckInDate(), request.getCheckOutDate());
        long reservationId = IdWorker.getId();

        // 多夜原子预占（内部含补偿）；不足时抛 HOTEL_INSUFFICIENT_ROOM
        hotelInventoryService.reserveStay(reservationId, request.getSkuId(), nights, roomCount);

        BigDecimal amount = roomType.getBasePrice()
                .multiply(BigDecimal.valueOf((long) nights.size() * roomCount));

        RoomReservation reservation = new RoomReservation();
        reservation.setReservationId(reservationId);
        reservation.setTenantId(TenantContext.get());
        reservation.setScenicId(roomType.getScenicId());
        reservation.setMerchantId(roomType.getMerchantId());
        reservation.setUserId(request.getUserId());
        reservation.setSkuId(request.getSkuId());
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setNights(nights.size());
        reservation.setRoomCount(roomCount);
        reservation.setStatus("PENDING_PAYMENT");
        reservation.setTotalAmount(amount);
        reservation.setVersion(0);
        reservationMapper.insert(reservation);

        log.info("创建预订单成功: reservationId={}, nights={}, total={}", reservationId, nights.size(), amount);
        return toResponse(reservation);
    }

    public PayResponse pay(Long reservationId, String channel) {
        RoomReservation reservation = getOrThrow(reservationId);
        if (!"PENDING_PAYMENT".equals(reservation.getStatus())) {
            throw new BizException(HotelErrorCode.RESERVATION_INVALID_STATE);
        }
        PaymentClient.PaymentView payment = paymentClient.createPayment(
                reservationId, reservation.getTenantId(), reservation.getMerchantId(),
                reservation.getTotalAmount(), channel);
        reservation.setPaymentId(payment.getPaymentId());
        reservationMapper.updateById(reservation);
        return PayResponse.builder()
                .paymentId(payment.getPaymentId())
                .payParams(payment.getPayParams())
                .build();
    }

    public ReservationResponse getReservation(Long reservationId) {
        return toResponse(getOrThrow(reservationId));
    }

    /** 取消（v1 仅未支付可取消，释放预占；已支付的退款取消留待退款链路细化）。 */
    public void cancel(Long reservationId) {
        RoomReservation reservation = getOrThrow(reservationId);
        if (!"PENDING_PAYMENT".equals(reservation.getStatus())) {
            throw new BizException(HotelErrorCode.RESERVATION_INVALID_STATE);
        }
        hotelInventoryService.releaseStay(reservationId);
        reservation.setStatus("CANCELLED");
        reservationMapper.updateById(reservation);
        log.info("取消预订单: reservationId={}", reservationId);
    }

    /** 由 PaymentSucceeded 消费侧调用：确认连住预占并置预订单 CONFIRMED（幂等）。 */
    public void confirmPaid(Long reservationId, String paymentId) {
        RoomReservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            // 非酒店预订单（可能是门票订单共用同一支付主题），忽略
            return;
        }
        if (!"PENDING_PAYMENT".equals(reservation.getStatus())) {
            return;
        }
        hotelInventoryService.confirmStay(reservationId);
        reservation.setStatus("CONFIRMED");
        reservation.setPaymentId(paymentId);
        reservationMapper.updateById(reservation);
        log.info("预订单支付成功已确认: reservationId={}", reservationId);
    }

    private RoomReservation getOrThrow(Long reservationId) {
        RoomReservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BizException(HotelErrorCode.RESERVATION_NOT_FOUND);
        }
        return reservation;
    }

    private ReservationResponse toResponse(RoomReservation r) {
        return ReservationResponse.builder()
                .reservationId(r.getReservationId())
                .status(r.getStatus())
                .skuId(r.getSkuId())
                .checkInDate(r.getCheckInDate())
                .checkOutDate(r.getCheckOutDate())
                .nights(r.getNights())
                .roomCount(r.getRoomCount())
                .totalAmount(r.getTotalAmount())
                .paymentId(r.getPaymentId())
                .build();
    }
}

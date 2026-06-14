package com.qmp.hotel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qmp.kernel.inventory.InventoryReservationBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 间夜预占（连住中的单晚）。复用 {@link InventoryReservationBase} 的预占状态机
 * （HOLDING/CONFIRMED/RELEASED/EXPIRED）。{@code reservation_id} 取 {@code 预订单ID:间夜日期}，
 * 一条连住预订对应区间内 N 条本记录（11 文档 1.3「多夜原子预占」）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("room_night_reservation")
public class RoomNightReservation extends InventoryReservationBase {

    /** 所属预订单（数字 ID，作为 payment 的 order_id）。 */
    private Long hotelReservationId;

    /** 间夜日期。 */
    private LocalDate saleDate;
}

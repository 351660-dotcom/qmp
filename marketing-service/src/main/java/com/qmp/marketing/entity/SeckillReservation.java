package com.qmp.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qmp.kernel.inventory.InventoryReservationBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 秒杀预占（复用预占状态机）。{@code reservation_id = activity_id:user_id}（v1 限购 1，唯一即限购）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_reservation")
public class SeckillReservation extends InventoryReservationBase {

    private Long activityId;

    private Long userId;
}

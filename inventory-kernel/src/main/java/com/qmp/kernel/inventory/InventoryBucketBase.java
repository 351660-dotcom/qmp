package com.qmp.kernel.inventory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 「日期/场次型」库存桶通用字段，ADR-025 参考实现。
 *
 * <p>门票 {@code inventory_bucket}（inventory-service）、酒店 {@code room_inventory_bucket} /
 * {@code meeting_room_inventory_bucket}（hotel-pms-service）等业务域<b>各自独立建表</b>，
 * 继承本类复用字段定义，并接入 06 文档「两道防线」的条件更新模式：</p>
 *
 * <pre>{@code
 * UPDATE <table>
 * SET locked_count = locked_count + :qty, version = version + 1
 * WHERE bucket_id = :bucketId
 *   AND total_quota - sold_count - locked_count >= :qty;
 * }</pre>
 */
@Data
public abstract class InventoryBucketBase {

    @TableId(type = IdType.ASSIGN_ID)
    private Long bucketId;

    private Long tenantId;

    private Long skuId;

    private LocalDate saleDate;

    /** 无分时预约固定为 0。 */
    private Long timeSlotId;

    private Integer totalQuota;

    private Integer soldCount;

    private Integer lockedCount;

    /** JSON，如 {@code {"direct": total_quota}}，多渠道共享池+渠道封顶（06 文档三）。 */
    private String channelQuota;

    @Version
    private Integer version;

    /** 由 MySQL {@code DEFAULT CURRENT_TIMESTAMP} 填充，应用层不写入。 */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    /** 由 MySQL {@code DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP} 填充，应用层不写入。 */
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}

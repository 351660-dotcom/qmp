package com.qmp.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmp.hotel.entity.RoomInventoryBucket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 房晚库存桶 Mapper。四条条件更新即第二道防线（复用门票 inventory_bucket 同一 SQL 范式，ADR-025/ADR-018）。
 */
@Mapper
public interface RoomInventoryBucketMapper extends BaseMapper<RoomInventoryBucket> {

    /** 预占：余量足够才允许。 */
    @Update("UPDATE room_inventory_bucket "
            + "SET locked_count = locked_count + #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} "
            + "AND total_quota - sold_count - locked_count >= #{qty}")
    int tryLock(@Param("bucketId") Long bucketId, @Param("qty") int qty);

    /** 确认：HOLDING -&gt; CONFIRMED。 */
    @Update("UPDATE room_inventory_bucket "
            + "SET locked_count = locked_count - #{qty}, sold_count = sold_count + #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND locked_count >= #{qty}")
    int confirmLock(@Param("bucketId") Long bucketId, @Param("qty") int qty);

    /** 释放（从 HOLDING）。 */
    @Update("UPDATE room_inventory_bucket "
            + "SET locked_count = locked_count - #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND locked_count >= #{qty}")
    int releaseLock(@Param("bucketId") Long bucketId, @Param("qty") int qty);

    /** 释放（从 CONFIRMED，已支付后取消）。 */
    @Update("UPDATE room_inventory_bucket "
            + "SET sold_count = sold_count - #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND sold_count >= #{qty}")
    int releaseSold(@Param("bucketId") Long bucketId, @Param("qty") int qty);
}

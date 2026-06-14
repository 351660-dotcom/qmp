package com.qmp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmp.inventory.entity.InventoryBucket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryBucketMapper extends BaseMapper<InventoryBucket> {

    /**
     * CreateReservation 条件更新（10 文档 5.1 第二道防线）：余量足够才允许预占。
     */
    @Update("UPDATE inventory_bucket "
            + "SET locked_count = locked_count + #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} "
            + "AND total_quota - sold_count - locked_count >= #{qty}")
    int tryLock(@Param("bucketId") Long bucketId, @Param("qty") int qty);

    /**
     * ConfirmReservation：HOLDING -&gt; CONFIRMED。
     */
    @Update("UPDATE inventory_bucket "
            + "SET locked_count = locked_count - #{qty}, sold_count = sold_count + #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND locked_count >= #{qty}")
    int confirmLock(@Param("bucketId") Long bucketId, @Param("qty") int qty);

    /**
     * ReleaseReservation（从 HOLDING 释放）。
     */
    @Update("UPDATE inventory_bucket "
            + "SET locked_count = locked_count - #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND locked_count >= #{qty}")
    int releaseLock(@Param("bucketId") Long bucketId, @Param("qty") int qty);

    /**
     * ReleaseReservation（从 CONFIRMED 释放，对应已支付后退票）。
     */
    @Update("UPDATE inventory_bucket "
            + "SET sold_count = sold_count - #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND sold_count >= #{qty}")
    int releaseSold(@Param("bucketId") Long bucketId, @Param("qty") int qty);
}

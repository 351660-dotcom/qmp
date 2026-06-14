package com.qmp.performance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmp.performance.entity.SeatInventoryBucket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 座位库存桶 Mapper（同一套防超卖 SQL 范式，粒度到单座位/舱位）。
 */
@Mapper
public interface SeatInventoryBucketMapper extends BaseMapper<SeatInventoryBucket> {

    @Update("UPDATE seat_inventory_bucket "
            + "SET locked_count = locked_count + #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND total_quota - sold_count - locked_count >= #{qty}")
    int tryLock(@Param("bucketId") Long bucketId, @Param("qty") int qty);

    @Update("UPDATE seat_inventory_bucket "
            + "SET locked_count = locked_count - #{qty}, sold_count = sold_count + #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND locked_count >= #{qty}")
    int confirmLock(@Param("bucketId") Long bucketId, @Param("qty") int qty);

    @Update("UPDATE seat_inventory_bucket "
            + "SET locked_count = locked_count - #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND locked_count >= #{qty}")
    int releaseLock(@Param("bucketId") Long bucketId, @Param("qty") int qty);

    @Update("UPDATE seat_inventory_bucket "
            + "SET sold_count = sold_count - #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND sold_count >= #{qty}")
    int releaseSold(@Param("bucketId") Long bucketId, @Param("qty") int qty);
}

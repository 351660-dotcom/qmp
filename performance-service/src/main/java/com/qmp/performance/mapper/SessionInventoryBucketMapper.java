package com.qmp.performance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmp.performance.entity.SessionInventoryBucket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 场次库存桶 Mapper。四条条件更新即第二道防线（复用门票 inventory_bucket 同 SQL 范式，ADR-025）。
 */
@Mapper
public interface SessionInventoryBucketMapper extends BaseMapper<SessionInventoryBucket> {

    @Update("UPDATE session_inventory_bucket "
            + "SET locked_count = locked_count + #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND total_quota - sold_count - locked_count >= #{qty}")
    int tryLock(@Param("bucketId") Long bucketId, @Param("qty") int qty);

    @Update("UPDATE session_inventory_bucket "
            + "SET locked_count = locked_count - #{qty}, sold_count = sold_count + #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND locked_count >= #{qty}")
    int confirmLock(@Param("bucketId") Long bucketId, @Param("qty") int qty);

    @Update("UPDATE session_inventory_bucket "
            + "SET locked_count = locked_count - #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND locked_count >= #{qty}")
    int releaseLock(@Param("bucketId") Long bucketId, @Param("qty") int qty);

    @Update("UPDATE session_inventory_bucket "
            + "SET sold_count = sold_count - #{qty}, version = version + 1 "
            + "WHERE bucket_id = #{bucketId} AND sold_count >= #{qty}")
    int releaseSold(@Param("bucketId") Long bucketId, @Param("qty") int qty);
}

package com.xiche.store.dao;

import com.xiche.store.entity.StoreBay;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StoreBayMapper {
    List<StoreBay> selectByStore(@Param("storeId") Long storeId);
    StoreBay selectById(@Param("id") Long id);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int countFree(@Param("storeId") Long storeId);

    /** P0-19/P1-22：工位 CAS 锁位，仅 free(0) 可置为 full(1)，返回受影响行数 */
    int lockBay(@Param("id") Long id, @Param("storeId") Long storeId);

    /** P0-19：释放工位 full(1)->free(0) */
    int releaseBay(@Param("id") Long id, @Param("storeId") Long storeId);
}

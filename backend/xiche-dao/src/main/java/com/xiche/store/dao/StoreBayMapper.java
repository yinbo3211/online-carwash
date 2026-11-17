package com.xiche.store.dao;

import com.xiche.store.entity.StoreBay;
import java.util.List;

public interface StoreBayMapper {
    List<StoreBay> selectByStore(Long storeId);
    StoreBay selectById(Long id);
    int updateStatus(Long id, Integer status);
    int countFree(Long storeId);

    /** P0-19/P1-22：工位 CAS 锁位，仅 free(0) 可置为 full(1)，返回受影响行数 */
    int lockBay(Long id, Long storeId);

    /** P0-19：释放工位 full(1)->free(0) */
    int releaseBay(Long id, Long storeId);
}

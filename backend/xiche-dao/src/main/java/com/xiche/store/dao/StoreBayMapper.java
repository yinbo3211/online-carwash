package com.xiche.store.dao;

import com.xiche.store.entity.StoreBay;
import java.util.List;

public interface StoreBayMapper {
    List<StoreBay> selectByStore(Long storeId);
    StoreBay selectById(Long id);
    int updateStatus(Long id, Integer status);
    int countFree(Long storeId);
}

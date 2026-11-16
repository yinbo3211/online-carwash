package com.xiche.store.dao;

import com.xiche.store.entity.Store;

import java.util.List;
import java.util.Map;

public interface StoreMapper {
    Store selectById(Long id);

    Store selectByOwner(Long ownerUserId);

    List<Store> selectList(Map<String, Object> params);

    int insert(Store store);

    int updateById(Store store);
}

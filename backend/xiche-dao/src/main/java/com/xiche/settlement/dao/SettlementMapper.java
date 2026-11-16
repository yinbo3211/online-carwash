package com.xiche.settlement.dao;

import com.xiche.settlement.entity.Settlement;

import java.util.List;
import java.util.Map;

public interface SettlementMapper {
    Settlement selectById(Long id);

    Settlement selectByOrderId(Long orderId);

    List<Settlement> selectList(Map<String, Object> params);

    int insert(Settlement settlement);
}

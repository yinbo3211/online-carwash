package com.xiche.settlement.dao;

import com.xiche.settlement.entity.Settlement;

import java.util.List;
import java.util.Map;

public interface SettlementMapper {
    Settlement selectById(Long id);

    Settlement selectByOrderId(Long orderId);

    List<Settlement> selectList(Map<String, Object> params);

    int insert(Settlement settlement);

    /** P0-20：按洗车工过滤结算单（关联 t_order.worker_id），杜绝越权拉取全平台数据 */
    List<Settlement> selectByWorker(Map<String, Object> params);
}

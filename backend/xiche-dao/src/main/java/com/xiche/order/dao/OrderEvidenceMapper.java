package com.xiche.order.dao;

import com.xiche.order.entity.OrderEvidence;
import java.util.List;

public interface OrderEvidenceMapper {
    int insert(OrderEvidence evidence);
    List<OrderEvidence> selectByOrder(Long orderId);
    int countByStage(Long orderId, String stage);
}

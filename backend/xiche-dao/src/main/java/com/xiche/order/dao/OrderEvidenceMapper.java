package com.xiche.order.dao;

import com.xiche.order.entity.OrderEvidence;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface OrderEvidenceMapper {
    int insert(OrderEvidence evidence);
    List<OrderEvidence> selectByOrder(@Param("orderId") Long orderId);
    int countByStage(@Param("orderId") Long orderId, @Param("stage") String stage);
}

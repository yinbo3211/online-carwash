package com.xiche.order.dao;
import com.xiche.order.entity.OrderItem;
import java.util.List;
/** 订单服务项明细 DAO（P1-07 增值项认证校验 / P0-11 price_snapshot） */
public interface OrderItemMapper {
    List<OrderItem> selectByOrder(Long orderId);
    int insert(OrderItem item);
}

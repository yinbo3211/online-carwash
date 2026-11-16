package com.xiche.order.service;

import com.xiche.common.util.PageUtil;
import com.xiche.order.entity.Order;

import java.util.List;
import java.util.Map;

public interface OrderService {
    Order getById(Long id);

    List<Order> list(Map<String, Object> params, PageUtil page);

    /** 派单六项合取校验；返回未通过原因列表，为空表示全部通过 */
    List<String> checkEligibility(Order order, Long workerId);

    /** 是否满足全部六项派单条件 */
    boolean isEligible(Order order, Long workerId);

    /** 接单：先校验资格，再用状态 CAS 防止并发抢单 */
    Long acceptOrder(Long orderId, Long workerId);
}

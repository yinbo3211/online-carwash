package com.xiche.order.dao;

import com.xiche.order.entity.Order;

import java.util.List;
import java.util.Map;

public interface OrderMapper {
    Order selectById(Long id);

    Order selectByOrderNo(String orderNo);

    /** 分页列表：四端订单查询统一入口（C 级禁止缓存，直查库） */
    List<Order> selectList(Map<String, Object> params);

    int countList(Map<String, Object> params);

    int insert(Order order);

    int updateStatus(Long id, Integer status);

    /** 派单成功：写入 workerId 并推进状态，同时校验旧状态防止并发重复接单 */
    int assignWorker(Long id, Long workerId, Integer fromStatus, Integer toStatus);

    /** 结算回写：恒等式计算出的全部分账字段 */
    int updateSettlement(Order order);
}

package com.xiche.settlement.service;

import com.xiche.order.entity.Order;
import com.xiche.settlement.entity.Settlement;

public interface SettlementService {
    /**
     * 纯计算：按恒等式 P + C = worker_net + store_net + B + platform_fee 计算并断言校验。
     * 不访问数据库，便于单元测试直接运行。
     */
    Settlement calculate(Order order);

    /** 计算 + 落库 + 回写订单，带幂等与事务 */
    Settlement settle(Order order);

    Settlement queryByOrderId(Long orderId);
}

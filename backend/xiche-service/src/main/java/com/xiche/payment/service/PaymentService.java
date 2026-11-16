package com.xiche.payment.service;

import com.xiche.payment.entity.PaymentOrder;

public interface PaymentService {
    /** 模拟支付入口（V1 仅此一种支付方式） */
    PaymentOrder mockPay(Long orderId, Long userId);

    /** 按业务单号查询模拟支付流水 */
    PaymentOrder queryByOrderNo(String orderNo);
}

package com.xiche.payment.service;

import com.xiche.order.entity.Order;
import com.xiche.payment.entity.PaymentOrder;

/**
 * V1 内部测试版唯一允许的支付网关接口（业务口径 1）。
 * 禁止接入真实支付宝/微信；禁止产生任何真实资金收付。
 */
public interface MockPaymentGateway {
    /**
     * 模拟支付：返回固定成功文案 + 橙色成功态 + TEST-ENV-V1 标识。
     *
     * @param order  待支付订单
     * @param userId 支付发起人
     * @return 模拟支付流水
     */
    PaymentOrder pay(Order order, Long userId);
}

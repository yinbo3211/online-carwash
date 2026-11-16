package com.xiche.payment.service.impl;

import com.xiche.common.BizException;
import com.xiche.common.constant.BusinessConstants;
import com.xiche.common.constant.OrderStatusEnum;
import com.xiche.common.util.MoneyUtil;
import com.xiche.order.dao.OrderMapper;
import com.xiche.order.entity.Order;
import com.xiche.payment.dao.PaymentOrderMapper;
import com.xiche.payment.entity.PaymentOrder;
import com.xiche.payment.service.MockPaymentGateway;
import com.xiche.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 模拟支付服务：校验订单状态与强制保险后，委托 MockPaymentGateway 生成模拟流水。
 */
@Service
public class PaymentServiceImpl implements PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private MockPaymentGateway mockPaymentGateway;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder mockPay(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在，无法模拟支付");
        }
        if (order.getStatus() == null || order.getStatus().intValue() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            throw new BizException("仅待支付订单可支付，当前状态："
                    + OrderStatusEnum.descOf(order.getStatus()));
        }
        // L1 基础保障每单强制，不可取消；每单必须有保险
        if (MoneyUtil.isNegative(order.getInsuranceFee())
                || MoneyUtil.compare(order.getInsuranceFee(), MoneyUtil.scale2(BusinessConstants.INSURANCE_L1_PREMIUM)) < 0) {
            throw new BizException("每单必须有保险：L1 基础保障强制且不可取消");
        }

        PaymentOrder payment = mockPaymentGateway.pay(order, userId);
        orderMapper.updateStatus(order.getId(), OrderStatusEnum.WAITING_ACCEPT.getCode());
        log.info("[{}] 订单 {} 支付后进入待接单", BusinessConstants.ENV_FLAG, order.getOrderNo());
        return payment;
    }

    @Override
    public PaymentOrder queryByOrderNo(String orderNo) {
        return paymentOrderMapper.selectByOrderNo(orderNo);
    }
}

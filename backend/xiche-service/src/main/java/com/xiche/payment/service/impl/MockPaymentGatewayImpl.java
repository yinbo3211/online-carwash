package com.xiche.payment.service.impl;

import com.xiche.common.BizException;
import com.xiche.common.constant.PaymentConstants;
import com.xiche.order.entity.Order;
import com.xiche.payment.dao.PaymentOrderMapper;
import com.xiche.payment.entity.PaymentOrder;
import com.xiche.payment.service.MockPaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 模拟支付实现：不调用任何第三方 SDK，不产生真实资金收付。
 * 幂等：同一 orderNo 只生成一条成功流水。
 */
@Component(PaymentConstants.GATEWAY_BEAN)
public class MockPaymentGatewayImpl implements MockPaymentGateway {
    private static final Logger log = LoggerFactory.getLogger(MockPaymentGatewayImpl.class);

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder pay(Order order, Long userId) {
        if (order == null || order.getId() == null) {
            throw new BizException("模拟支付失败：订单不存在");
        }
        String idempotentKey = PaymentConstants.IDEMPOTENT_KEY_PREFIX + order.getOrderNo();
        PaymentOrder exists = paymentOrderMapper.selectByIdempotentKey(idempotentKey);
        if (exists != null) {
            log.info("[{}] 模拟支付幂等命中 orderNo={}", PaymentConstants.ENV_FLAG, order.getOrderNo());
            return exists;
        }

        PaymentOrder payment = new PaymentOrder();
        payment.setPaymentNo("MP" + System.nanoTime());
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(userId);
        payment.setAmount(order.getPaidAmount());
        payment.setGateway(PaymentConstants.GATEWAY_BEAN);
        payment.setStatus(PaymentConstants.PAY_STATUS_SUCCESS);
        payment.setSuccessMessage(PaymentConstants.PAY_SUCCESS_MESSAGE);
        payment.setSuccessColor(PaymentConstants.PAY_SUCCESS_COLOR);
        payment.setEnvFlag(PaymentConstants.ENV_FLAG);
        payment.setIdempotentKey(idempotentKey);
        payment.setPaidAt(new Date());
        paymentOrderMapper.insert(payment);
        paymentOrderMapper.updateSuccess(payment.getId(), payment.getStatus(),
                payment.getSuccessMessage(), payment.getSuccessColor());

        log.info("[{}] 模拟支付成功 orderNo={} amount={} msg={}",
                PaymentConstants.ENV_FLAG, order.getOrderNo(), order.getPaidAmount(),
                PaymentConstants.PAY_SUCCESS_MESSAGE);
        return payment;
    }
}

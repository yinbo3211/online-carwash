package com.xiche.payment.service.impl;

import com.xiche.common.BizException;
import com.xiche.common.constant.BusinessConstants;
import com.xiche.common.constant.OrderStatusEnum;
import com.xiche.common.util.MoneyUtil;
import com.xiche.insurance.dao.PolicyMapper;
import com.xiche.insurance.entity.Policy;
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
import java.util.Date;
import java.util.Calendar;

/**
 * 模拟支付服务：校验订单状态与强制保险后，委托 MockPaymentGateway 生成模拟流水。
 * P0-03：支付成功后同步出单写入 ACTIVE 保单，覆盖 T1去程+T2作业+T3返程全窗口，
 * 否则派单资格第⑥项要求有效L1保单将必然失败，主链路中断。
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
    @Resource
    private PolicyMapper policyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder mockPay(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在，无法模拟支付");
        }
        // BUG-P0-05/B1：仅订单本人可支付，杜绝越权支付他人订单
        if (userId != null && !userId.equals(order.getUserId())) {
            throw new BizException(40300, "无权支付他人订单");
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
        // P0-03：支付成功后出单（幂等：已有有效保单则跳过）
        issuePolicyIfAbsent(order);
        orderMapper.updateStatus(order.getId(), OrderStatusEnum.WAITING_ACCEPT.getCode());
        log.info("[{}] 订单 {} 支付后进入待接单", BusinessConstants.ENV_FLAG, order.getOrderNo());
        return payment;
    }

    /** P0-03：为订单出具 L1 模拟保单，覆盖全履约窗口（支付成功后调用，事务内） */
    private void issuePolicyIfAbsent(Order order) {
        if (policyMapper.countValidL1ByOrderId(order.getId(), new Date()) > 0) {
            log.info("[{}] 订单 {} 已有有效L1保单，跳过多余出单", BusinessConstants.ENV_FLAG, order.getOrderNo());
            return;
        }
        Policy p = new Policy();
        p.setPolicyNo("PL" + System.nanoTime());
        p.setOrderId(order.getId());
        p.setProductId(1L); // INS-L1-BASE
        p.setLevel(BusinessConstants.INSURANCE_L1);
        p.setPremium(order.getInsuranceFee() == null ? BusinessConstants.INSURANCE_L1_PREMIUM : order.getInsuranceFee());
        p.setStatus("ACTIVE"); // V1 模拟出单；生产须持牌保险合作方回写
        Date now = new Date();
        p.setEffectiveFrom(now);
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.HOUR_OF_DAY, 72); // 覆盖 T1+T2+T3 全窗口，留冗余
        p.setEffectiveTo(cal.getTime());
        policyMapper.insert(p);
        log.info("[{}] 订单 {} 已出L1模拟保单 {}", BusinessConstants.ENV_FLAG, order.getOrderNo(), p.getPolicyNo());
    }

    @Override
    public PaymentOrder queryByOrderNo(String orderNo) {
        return paymentOrderMapper.selectByOrderNo(orderNo);
    }
}

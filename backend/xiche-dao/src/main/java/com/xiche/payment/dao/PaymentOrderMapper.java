package com.xiche.payment.dao;

import com.xiche.payment.entity.PaymentOrder;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

public interface PaymentOrderMapper {
    PaymentOrder selectById(Long id);

    PaymentOrder selectByOrderNo(String orderNo);

    /** 幂等键查询：同一业务单号只允许模拟支付一次 */
    PaymentOrder selectByIdempotentKey(String idempotentKey);

    int insert(PaymentOrder paymentOrder);

    int updateSuccess(@Param("id") Long id, @Param("status") String status,
                      @Param("message") String message, @Param("color") String color);
}

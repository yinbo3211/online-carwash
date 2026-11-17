package com.xiche.insurance.dao;

import com.xiche.insurance.entity.Policy;

import java.util.Date;
import java.util.List;

public interface PolicyMapper {
    List<Policy> selectByOrderId(Long orderId);

    /** 实时查询有效保单（C 级禁止缓存）；L1 强制，每单必须有保险 */
    int countValidL1ByOrderId(Long orderId, Date now);

    /** P0-03：支付成功后出单写入保单（V1 模拟台账，状态 ACTIVE 覆盖全履约窗口） */
    int insert(Policy policy);
}

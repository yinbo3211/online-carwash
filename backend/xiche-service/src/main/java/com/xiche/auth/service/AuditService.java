package com.xiche.auth.service;

import java.util.Map;

/** 审计服务：关键操作（封号/费率变更/支付/结算/权限）留痕，WORM 语义。 */
public interface AuditService {
    void record(String operatorType, Long operatorId, String operation,
                String targetType, String targetId, String detail, String clientIp);
    void record(String operatorType, Long operatorId, String operation,
                String targetType, String targetId, String detail, String beforeVal,
                String afterVal, String clientIp);
    Map<String, Object> query(Map<String, Object> params);
}

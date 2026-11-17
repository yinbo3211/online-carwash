package com.xiche.auth.service.impl;

import com.xiche.auth.dao.AuditLogMapper;
import com.xiche.auth.entity.AuditLog;
import com.xiche.auth.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditServiceImpl implements AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);
    @Resource private AuditLogMapper auditLogMapper;

    @Override
    public void record(String operatorType, Long operatorId, String operation,
                       String targetType, String targetId, String detail, String clientIp) {
        record(operatorType, operatorId, operation, targetType, targetId, detail, null, null, clientIp);
    }

    @Override
    public void record(String operatorType, Long operatorId, String operation,
                       String targetType, String targetId, String detail,
                       String beforeVal, String afterVal, String clientIp) {
        try {
            AuditLog l = new AuditLog();
            l.setOperatorType(operatorType); l.setOperatorId(operatorId);
            l.setOperation(operation); l.setTargetType(targetType); l.setTargetId(targetId);
            l.setDetail(detail); l.setBeforeVal(beforeVal); l.setAfterVal(afterVal);
            l.setClientIp(clientIp);
            auditLogMapper.insert(l);
        } catch (Exception e) {
            // 审计失败不能吞掉主流程，但必须告警（WORM 落库失败属高危）
            log.error("审计日志写入失败 operation={} target={}:{}", operation, targetType, targetId, e);
        }
    }

    @Override
    public Map<String, Object> query(Map<String, Object> params) {
        List<AuditLog> list = auditLogMapper.selectList(params);
        Map<String, Object> r = new HashMap<String, Object>();
        r.put("list", list); r.put("total", list.size());
        return r;
    }
}

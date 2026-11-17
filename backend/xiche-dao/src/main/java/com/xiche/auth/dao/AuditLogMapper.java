package com.xiche.auth.dao;

import com.xiche.auth.entity.AuditLog;
import java.util.List;
import java.util.Map;

/** 审计日志 DAO（修复 P0-17，WORM 语义只增不改） */
public interface AuditLogMapper {
    int insert(AuditLog log);
    List<AuditLog> selectList(Map<String, Object> params);
}

package com.xiche.auth.entity;

import java.util.Date;

/** 审计日志：登录、支付、权限变更等关键操作留存 >= 6 个月。 */
public class AuditLog {
    private Long id;
    private Long operatorId;
    private String operatorType;
    private String operation;
    private String targetType;
    private String targetId;
    private String detail;
    private String beforeVal;
    private String afterVal;
    private String clientIp;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorType() { return operatorType; }
    public void setOperatorType(String operatorType) { this.operatorType = operatorType; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getBeforeVal() { return beforeVal; }
    public void setBeforeVal(String beforeVal) { this.beforeVal = beforeVal; }
    public String getAfterVal() { return afterVal; }
    public void setAfterVal(String afterVal) { this.afterVal = afterVal; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

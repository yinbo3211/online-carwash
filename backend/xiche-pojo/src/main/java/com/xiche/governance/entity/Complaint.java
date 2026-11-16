package com.xiche.governance.entity;

import java.util.Date;

/** 投诉工单：status OPEN/PROCESSING/CLOSED；envFlag 标识测试环境。 */
public class Complaint {
    private Long id;
    private String complaintNo;
    private Long orderId;
    private Long userId;
    private String reason;
    private String description;
    private String status;
    private String envFlag;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getComplaintNo() { return complaintNo; }
    public void setComplaintNo(String complaintNo) { this.complaintNo = complaintNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEnvFlag() { return envFlag; }
    public void setEnvFlag(String envFlag) { this.envFlag = envFlag; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

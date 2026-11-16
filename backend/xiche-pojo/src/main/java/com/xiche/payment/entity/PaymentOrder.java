package com.xiche.payment.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付流水。V1 只允许 MockPaymentGateway，envFlag 固定 TEST-ENV-V1，
 * successMessage 固定「模拟支付成功·测试数据，不产生真实资金收付」。
 * 资金属于 C 级禁止缓存数据。
 */
public class PaymentOrder {
    private Long id;
    private String paymentNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private String gateway;
    private String status;
    private String successMessage;
    private String successColor;
    private String envFlag;
    private String idempotentKey;
    private Date paidAt;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSuccessMessage() { return successMessage; }
    public void setSuccessMessage(String successMessage) { this.successMessage = successMessage; }
    public String getSuccessColor() { return successColor; }
    public void setSuccessColor(String successColor) { this.successColor = successColor; }
    public String getEnvFlag() { return envFlag; }
    public void setEnvFlag(String envFlag) { this.envFlag = envFlag; }
    public String getIdempotentKey() { return idempotentKey; }
    public void setIdempotentKey(String idempotentKey) { this.idempotentKey = idempotentKey; }
    public Date getPaidAt() { return paidAt; }
    public void setPaidAt(Date paidAt) { this.paidAt = paidAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

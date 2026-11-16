package com.xiche.insurance.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 保单：每单必须有保险（至少一张有效 L1 保单）。
 * 保单属于 C 级禁止缓存数据，派单资格必须实时校验。
 */
public class Policy {
    private Long id;
    private String policyNo;
    private Long orderId;
    private Long productId;
    private String level;
    private BigDecimal premium;
    private String status;
    private Date effectiveFrom;
    private Date effectiveTo;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPolicyNo() { return policyNo; }
    public void setPolicyNo(String policyNo) { this.policyNo = policyNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public BigDecimal getPremium() { return premium; }
    public void setPremium(BigDecimal premium) { this.premium = premium; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Date effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public Date getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Date effectiveTo) { this.effectiveTo = effectiveTo; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

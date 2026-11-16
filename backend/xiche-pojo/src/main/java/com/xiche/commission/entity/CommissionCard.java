package com.xiche.commission.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 免佣卡：购买后在有效期内对劳务收入 L 免收平台抽成。
 * 抽成费率基准仍为 STORE 40% / ONSITE 35%。
 */
public class CommissionCard {
    private Long id;
    private Long workerId;
    private String cardNo;
    private BigDecimal quota;
    private BigDecimal usedQuota;
    private BigDecimal discountRate;
    private Date validFrom;
    private Date validTo;
    private String status;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public String getCardNo() { return cardNo; }
    public void setCardNo(String cardNo) { this.cardNo = cardNo; }
    public BigDecimal getQuota() { return quota; }
    public void setQuota(BigDecimal quota) { this.quota = quota; }
    public BigDecimal getUsedQuota() { return usedQuota; }
    public void setUsedQuota(BigDecimal usedQuota) { this.usedQuota = usedQuota; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }
    public Date getValidFrom() { return validFrom; }
    public void setValidFrom(Date validFrom) { this.validFrom = validFrom; }
    public Date getValidTo() { return validTo; }
    public void setValidTo(Date validTo) { this.validTo = validTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

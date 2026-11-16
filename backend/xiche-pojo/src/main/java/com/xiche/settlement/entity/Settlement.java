package com.xiche.settlement.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 结算单：严格满足恒等式 P + C = worker_net + store_net + B + platform_fee。
 * 抽成基数 laborIncome(L) 只含劳务收入，M/保险费/代垫款不抽成。
 * 资金与结算属于 C 级禁止缓存数据。
 */
public class Settlement {
    private Long id;
    private String settlementNo;
    private Long orderId;
    private String orderNo;
    private String serviceMode;
    private BigDecimal paidAmount;
    private BigDecimal platformSubsidy;
    private BigDecimal storeBayFee;
    private BigDecimal passThroughCost;
    private BigDecimal laborIncome;
    private BigDecimal commissionRate;
    private BigDecimal platformFee;
    private BigDecimal workerNet;
    private BigDecimal storeNet;
    private BigDecimal total;
    private String status;
    private String envFlag;
    private Date settledAt;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSettlementNo() { return settlementNo; }
    public void setSettlementNo(String settlementNo) { this.settlementNo = settlementNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getServiceMode() { return serviceMode; }
    public void setServiceMode(String serviceMode) { this.serviceMode = serviceMode; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getPlatformSubsidy() { return platformSubsidy; }
    public void setPlatformSubsidy(BigDecimal platformSubsidy) { this.platformSubsidy = platformSubsidy; }
    public BigDecimal getStoreBayFee() { return storeBayFee; }
    public void setStoreBayFee(BigDecimal storeBayFee) { this.storeBayFee = storeBayFee; }
    public BigDecimal getPassThroughCost() { return passThroughCost; }
    public void setPassThroughCost(BigDecimal passThroughCost) { this.passThroughCost = passThroughCost; }
    public BigDecimal getLaborIncome() { return laborIncome; }
    public void setLaborIncome(BigDecimal laborIncome) { this.laborIncome = laborIncome; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }
    public BigDecimal getWorkerNet() { return workerNet; }
    public void setWorkerNet(BigDecimal workerNet) { this.workerNet = workerNet; }
    public BigDecimal getStoreNet() { return storeNet; }
    public void setStoreNet(BigDecimal storeNet) { this.storeNet = storeNet; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEnvFlag() { return envFlag; }
    public void setEnvFlag(String envFlag) { this.envFlag = envFlag; }
    public Date getSettledAt() { return settledAt; }
    public void setSettledAt(Date settledAt) { this.settledAt = settledAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

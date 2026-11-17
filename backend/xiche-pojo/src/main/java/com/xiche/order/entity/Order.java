package com.xiche.order.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单主表。金额口径（恒等式 P + C = worker_net + store_net + B + platform_fee）：
 * paidAmount = P 用户实付；platformSubsidy = C 平台补贴；
 * storeBayFee = M 门店工位费（不抽成）；insuranceFee 保险费（不抽成）；
 * advanceAmount 代垫款（不抽成）；passThroughCost = B = 保险费 + 代垫款；
 * laborIncome = L = P + C - M - B（唯一抽成基数）；
 * platformFee / workerNet / storeNet 为结算结果。
 * 订单属于 C 级禁止缓存数据；envFlag 固定 TEST-ENV-V1。
 */
public class Order {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long storeId;
    private Long workerId;
    private Long serviceSkuId;
    private Integer serviceLevel;
    private String serviceMode;
    private String fulfillMode;
    private Integer status;
    private BigDecimal paidAmount;
    private BigDecimal platformSubsidy;
    private BigDecimal storeBayFee;
    private BigDecimal insuranceFee;
    private BigDecimal advanceAmount;
    private BigDecimal laborIncome;
    private BigDecimal passThroughCost;
    private BigDecimal platformFee;
    private BigDecimal workerNet;
    private BigDecimal storeNet;
    private BigDecimal rating;
    private String envFlag;
    private String remark;
    /* P0-11：三级快照，锁定下单当时口径，防止追溯改价 */
    private String priceSnapshot;
    private String rateSnapshot;
    private String cardSnapshot;
    /* P1-20：上门地址 */
    private Long addressId;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public Long getServiceSkuId() { return serviceSkuId; }
    public void setServiceSkuId(Long serviceSkuId) { this.serviceSkuId = serviceSkuId; }
    public Integer getServiceLevel() { return serviceLevel; }
    public void setServiceLevel(Integer serviceLevel) { this.serviceLevel = serviceLevel; }
    public String getServiceMode() { return serviceMode; }
    public void setServiceMode(String serviceMode) { this.serviceMode = serviceMode; }
    public String getFulfillMode() { return fulfillMode; }
    public void setFulfillMode(String fulfillMode) { this.fulfillMode = fulfillMode; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getPlatformSubsidy() { return platformSubsidy; }
    public void setPlatformSubsidy(BigDecimal platformSubsidy) { this.platformSubsidy = platformSubsidy; }
    public BigDecimal getStoreBayFee() { return storeBayFee; }
    public void setStoreBayFee(BigDecimal storeBayFee) { this.storeBayFee = storeBayFee; }
    public BigDecimal getInsuranceFee() { return insuranceFee; }
    public void setInsuranceFee(BigDecimal insuranceFee) { this.insuranceFee = insuranceFee; }
    public BigDecimal getAdvanceAmount() { return advanceAmount; }
    public void setAdvanceAmount(BigDecimal advanceAmount) { this.advanceAmount = advanceAmount; }
    public BigDecimal getLaborIncome() { return laborIncome; }
    public void setLaborIncome(BigDecimal laborIncome) { this.laborIncome = laborIncome; }
    public BigDecimal getPassThroughCost() { return passThroughCost; }
    public void setPassThroughCost(BigDecimal passThroughCost) { this.passThroughCost = passThroughCost; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }
    public BigDecimal getWorkerNet() { return workerNet; }
    public void setWorkerNet(BigDecimal workerNet) { this.workerNet = workerNet; }
    public BigDecimal getStoreNet() { return storeNet; }
    public void setStoreNet(BigDecimal storeNet) { this.storeNet = storeNet; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public String getEnvFlag() { return envFlag; }
    public void setEnvFlag(String envFlag) { this.envFlag = envFlag; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getPriceSnapshot() { return priceSnapshot; }
    public void setPriceSnapshot(String priceSnapshot) { this.priceSnapshot = priceSnapshot; }
    public String getRateSnapshot() { return rateSnapshot; }
    public void setRateSnapshot(String rateSnapshot) { this.rateSnapshot = rateSnapshot; }
    public String getCardSnapshot() { return cardSnapshot; }
    public void setCardSnapshot(String cardSnapshot) { this.cardSnapshot = cardSnapshot; }
    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

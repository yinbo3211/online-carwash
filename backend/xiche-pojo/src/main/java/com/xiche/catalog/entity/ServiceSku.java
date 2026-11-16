package com.xiche.catalog.entity;

import java.math.BigDecimal;
import java.util.Date;

/** 可售 SKU：外观 83/95、标准 133/155、精细 243；上门精细价口径未给定。 */
public class ServiceSku {
    private Long id;
    private Long categoryId;
    private String skuCode;
    private String skuName;
    private Integer serviceLevel;
    private String serviceMode;
    private BigDecimal price;
    private String priceRemark;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public Integer getServiceLevel() { return serviceLevel; }
    public void setServiceLevel(Integer serviceLevel) { this.serviceLevel = serviceLevel; }
    public String getServiceMode() { return serviceMode; }
    public void setServiceMode(String serviceMode) { this.serviceMode = serviceMode; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getPriceRemark() { return priceRemark; }
    public void setPriceRemark(String priceRemark) { this.priceRemark = priceRemark; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

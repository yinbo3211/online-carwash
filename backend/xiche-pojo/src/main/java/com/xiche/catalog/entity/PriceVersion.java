package com.xiche.catalog.entity;

import java.math.BigDecimal;
import java.util.Date;

/** 价格版本：按城市 + 生效时间隔离，B 级短 TTL 强一致缓存。 */
public class PriceVersion {
    private Long id;
    private String versionCode;
    private String city;
    private Long skuId;
    private BigDecimal price;
    private Date effectiveFrom;
    private Date effectiveTo;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVersionCode() { return versionCode; }
    public void setVersionCode(String versionCode) { this.versionCode = versionCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Date getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Date effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public Date getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Date effectiveTo) { this.effectiveTo = effectiveTo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

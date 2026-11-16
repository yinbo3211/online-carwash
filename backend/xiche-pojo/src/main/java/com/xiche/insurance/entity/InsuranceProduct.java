package com.xiche.insurance.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 保险产品：L1 基础保障每单强制不可取消；L2 增值保障自愿加购。
 */
public class InsuranceProduct {
    private Long id;
    private String productCode;
    private String productName;
    private String level;
    private BigDecimal premium;
    private BigDecimal coverageAmount;
    private Integer mandatory;
    private Integer status;
    private String remark;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public BigDecimal getPremium() { return premium; }
    public void setPremium(BigDecimal premium) { this.premium = premium; }
    public BigDecimal getCoverageAmount() { return coverageAmount; }
    public void setCoverageAmount(BigDecimal coverageAmount) { this.coverageAmount = coverageAmount; }
    public Integer getMandatory() { return mandatory; }
    public void setMandatory(Integer mandatory) { this.mandatory = mandatory; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

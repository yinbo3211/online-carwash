package com.xiche.store.entity;

import java.math.BigDecimal;
import java.util.Date;

/** 门店；bayCount 为工位数，工位费 M 在结算中不参与抽成。 */
public class Store {
    private Long id;
    private String storeName;
    private Long ownerUserId;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String address;
    private Integer bayCount;
    private BigDecimal bayFee;
    private BigDecimal rating;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Integer getBayCount() { return bayCount; }
    public void setBayCount(Integer bayCount) { this.bayCount = bayCount; }
    public BigDecimal getBayFee() { return bayFee; }
    public void setBayFee(BigDecimal bayFee) { this.bayFee = bayFee; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

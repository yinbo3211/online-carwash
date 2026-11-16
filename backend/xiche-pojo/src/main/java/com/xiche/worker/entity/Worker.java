package com.xiche.worker.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 洗车工。派单六项合取中的四项数据在这里：
 * 认证覆盖(关联 certification)、评分 rating、产能 dailyCapacity/todayOrders、封禁 banned。
 */
public class Worker {
    private Long id;
    private String realName;
    private String phone;
    private String idCardNo;
    private String city;
    private BigDecimal rating;
    private Integer completedOrders;
    private Integer dailyCapacity;
    private Integer todayOrders;
    private Integer banned;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getIdCardNo() { return idCardNo; }
    public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public Integer getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(Integer completedOrders) { this.completedOrders = completedOrders; }
    public Integer getDailyCapacity() { return dailyCapacity; }
    public void setDailyCapacity(Integer dailyCapacity) { this.dailyCapacity = dailyCapacity; }
    public Integer getTodayOrders() { return todayOrders; }
    public void setTodayOrders(Integer todayOrders) { this.todayOrders = todayOrders; }
    public Integer getBanned() { return banned; }
    public void setBanned(Integer banned) { this.banned = banned; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

package com.xiche.coupon.entity;

import java.util.Date;

/** 用户持有的优惠券；status UNUSED/USED/EXPIRED。 */
public class UserCoupon {
    private Long id;
    private Long userId;
    private Long couponId;
    private String status;
    private Long orderId;
    private Date usedAt;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Date getUsedAt() { return usedAt; }
    public void setUsedAt(Date usedAt) { this.usedAt = usedAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

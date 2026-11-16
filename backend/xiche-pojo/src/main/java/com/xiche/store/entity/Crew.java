package com.xiche.store.entity;

import java.util.Date;

/** 门店-洗车工关联（班组），role: CAPTAIN/MEMBER。 */
public class Crew {
    private Long id;
    private Long storeId;
    private Long workerId;
    private String role;
    private Date joinedAt;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Date getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Date joinedAt) { this.joinedAt = joinedAt; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

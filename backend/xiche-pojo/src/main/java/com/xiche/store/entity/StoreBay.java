package com.xiche.store.entity;

import java.util.Date;

/** 门店工位：status 0 空闲 / 1 占用 / 2 停用。产能属于 C 级禁止缓存数据。 */
public class StoreBay {
    private Long id;
    private Long storeId;
    private String bayNo;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getBayNo() { return bayNo; }
    public void setBayNo(String bayNo) { this.bayNo = bayNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

package com.xiche.cache.entity;

import java.util.Date;

/**
 * 缓存策略表：A 可缓存 / B 短TTL强一致 / C 禁止缓存（资金·订单·产能·保单）。
 * 策略以 common 模块 CacheConstants 为唯一声明源，本表用于后台可视化配置。
 */
public class CachePolicy {
    private Long id;
    private String domain;
    private String level;
    private Integer ttlSeconds;
    private String description;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Integer getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(Integer ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

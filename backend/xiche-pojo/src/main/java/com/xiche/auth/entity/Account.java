package com.xiche.auth.entity;

import java.util.Date;

/**
 * 统一登录账号（BUG-P0-05 修复）：四类终端共用一个账号源，身份不再由请求头伪造。
 * subjectType: USER(车主) / WORKER(洗车工) / STORE(门店) / ADMIN(后台)。
 * passwordHash: SHA-256(盐+明文)，V1 内部测试账号由迁移脚本初始化。
 * roles: 逗号分隔的角色码（后台账号可多角色，含 AUDITOR/FINANCE 等 SoD 角色）；
 *        为空时按 subjectType 走默认角色。
 */
public class Account {
    private Long id;
    private String username;      // 登录名（手机号或 admin 账号）
    private String passwordHash;  // SHA-256(password + salt)
    private String salt;
    private String subjectType;   // USER / WORKER / STORE / ADMIN
    private Long subjectId;       // 对应业务表主键
    private String roles;         // 逗号分隔角色码，可空
    private Integer status;       // 1 正常 / 0 停用
    private Date createdAt;
    private Date updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

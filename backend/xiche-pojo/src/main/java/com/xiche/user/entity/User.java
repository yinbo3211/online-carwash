package com.xiche.user.entity;

import java.util.Date;

/**
 * 车主用户。
 * 敏感字段（手机号/身份证）落库需加密；status 10 正常 / 20 冻结。
 */
public class User {
    private Long id;
    private String openid;
    private String unionId;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private String realName;
    private String idCardNo;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getUnionId() { return unionId; }
    public void setUnionId(String unionId) { this.unionId = unionId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getIdCardNo() { return idCardNo; }
    public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

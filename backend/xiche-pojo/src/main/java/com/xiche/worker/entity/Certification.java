package com.xiche.worker.entity;

import java.util.Date;

/**
 * 洗车工考试认证：C1/C2/C3 技能等级 + D 取送车 / O 上门专项。
 * status = PASSED 表示已通过对应考试，未通过不可接对应订单。
 */
public class Certification {
    private Long id;
    private Long workerId;
    private String certCode;
    private Integer examScore;
    private String status;
    private Date passedAt;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public String getCertCode() { return certCode; }
    public void setCertCode(String certCode) { this.certCode = certCode; }
    public Integer getExamScore() { return examScore; }
    public void setExamScore(Integer examScore) { this.examScore = examScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getPassedAt() { return passedAt; }
    public void setPassedAt(Date passedAt) { this.passedAt = passedAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

package com.xiche.learning.entity;

import java.util.Date;

/** 考试记录：score >= 80 为 PASSED，写入 certification 后解锁对应接单资格。 */
public class ExamRecord {
    private Long id;
    private Long workerId;
    private Long examId;
    private String certCode;
    private Integer score;
    private String result;
    /* P1-10：切屏计数与试卷快照（可回溯卷版本） */
    private Integer switchCount;
    private String paperSnapshot;
    private Date submittedAt;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public String getCertCode() { return certCode; }
    public void setCertCode(String certCode) { this.certCode = certCode; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Integer getSwitchCount() { return switchCount; }
    public void setSwitchCount(Integer switchCount) { this.switchCount = switchCount; }
    public String getPaperSnapshot() { return paperSnapshot; }
    public void setPaperSnapshot(String paperSnapshot) { this.paperSnapshot = paperSnapshot; }
    public Date getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Date submittedAt) { this.submittedAt = submittedAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

package com.xiche.settlement.entity;

import java.math.BigDecimal;
import java.util.Date;

/** 分账流水：按结算单逐科目记账，方向 CREDIT/DEBIT。 */
public class Ledger {
    private Long id;
    private String ledgerNo;
    private Long settlementId;
    private String accountType;
    private Long accountId;
    private String subject;
    private String direction;
    private BigDecimal amount;
    private String envFlag;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLedgerNo() { return ledgerNo; }
    public void setLedgerNo(String ledgerNo) { this.ledgerNo = ledgerNo; }
    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getEnvFlag() { return envFlag; }
    public void setEnvFlag(String envFlag) { this.envFlag = envFlag; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
}

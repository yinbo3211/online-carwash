package com.xiche.settlement.dao;

import com.xiche.settlement.entity.Ledger;
import java.util.List;
import java.util.Map;

/** 资金台账 DAO（修复 P0-18：三方台账与 T+1 对账） */
public interface LedgerMapper {
    int insert(Ledger ledger);
    List<Ledger> selectList(Map<String, Object> params);
    /** 三方对账：按结算单汇总各账户发生额 */
    List<Map<String, Object>> reconcileBySettlement(Long settlementId);
}

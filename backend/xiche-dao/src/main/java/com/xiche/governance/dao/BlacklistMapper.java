package com.xiche.governance.dao;

import java.util.Date;

/**
 * 治理域：派单资格「未拉黑」实时校验。
 * 修复 P0-15：拉黑是门店局部行为，须按 storeId 过滤，不能退化成全平台封禁。
 */
public interface BlacklistMapper {
    /** 全平台维度（仅用于真正的平台封禁检索，一般派单不用） */
    int countActive(String subjectType, Long subjectId, Date now);

    /** 门店局部拉黑：仅当订单归属门店与被拉黑门店一致时生效 */
    int countActiveByStore(String subjectType, Long subjectId, Long storeId, Date now);

    int insert(Long storeId, String subjectType, Long subjectId, String reason,
               Date effectiveFrom, Date effectiveTo);
}

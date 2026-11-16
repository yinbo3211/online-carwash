package com.xiche.governance.dao;

import java.util.Date;

/** 治理域：派单资格「未拉黑」实时校验。 */
public interface BlacklistMapper {
    int countActive(String subjectType, Long subjectId, Date now);
}

package com.xiche.auth.dao;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 认证人工复核 DAO（P1-11）：考试通过后进入复核队列，48h 内人工复核。 */
public interface CertReviewMapper {
    int insert(@Param("workerId") Long workerId, @Param("certCode") String certCode,
               @Param("examRecordId") Long examRecordId);

    List<Map<String, Object>> selectPending(@Param("offset") Integer offset, @Param("limit") Integer limit);

    int review(@Param("id") Long id, @Param("status") String status,
               @Param("reviewerId") Long reviewerId, @Param("note") String note);

    Map<String, Object> selectById(@Param("id") Long id);
}

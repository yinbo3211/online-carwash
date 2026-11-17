package com.xiche.worker.dao;

import com.xiche.worker.entity.Certification;
import org.apache.ibatis.annotations.Param;

/** 认证持久化：考试通过后写入/更新认证状态。 */
public interface CertificationMapper {
    Certification selectByWorkerAndCode(@Param("workerId") Long workerId, @Param("certCode") String certCode);

    /** 唯一键 (worker_id, cert_code) 冲突时更新成绩与状态 */
    int upsert(Certification certification);

    /** P1-11：复核驳回时撤销认证（置 REVOKED，派单谓词将拒绝） */
    int revoke(@Param("workerId") Long workerId, @Param("certCode") String certCode);
}

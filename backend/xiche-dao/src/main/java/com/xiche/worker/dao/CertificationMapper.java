package com.xiche.worker.dao;

import com.xiche.worker.entity.Certification;

/** 认证持久化：考试通过后写入/更新认证状态。 */
public interface CertificationMapper {
    Certification selectByWorkerAndCode(Long workerId, String certCode);

    /** 唯一键 (worker_id, cert_code) 冲突时更新成绩与状态 */
    int upsert(Certification certification);
}

package com.xiche.learning.dao;

import com.xiche.learning.entity.Exam;
import com.xiche.learning.entity.ExamRecord;
import com.xiche.learning.entity.Question;

import java.util.List;

public interface ExamRecordMapper {
    Exam selectExamById(Long examId);

    List<Question> selectQuestions(Long examId);

    ExamRecord selectById(Long id);

    List<ExamRecord> selectByWorker(Long workerId);

    /** P1-10：某洗车工某认证的最近一次提交时间（24h 补考冷却判定） */
    java.util.Date selectLastSubmitted(@org.apache.ibatis.annotations.Param("workerId") Long workerId,
                                       @org.apache.ibatis.annotations.Param("certCode") String certCode);

    /** P1-10：某洗车工某认证的已考次数（每期最多 3 次） */
    int countByWorkerAndCert(@org.apache.ibatis.annotations.Param("workerId") Long workerId,
                             @org.apache.ibatis.annotations.Param("certCode") String certCode);

    int insert(ExamRecord record);
}

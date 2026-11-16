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

    int insert(ExamRecord record);
}

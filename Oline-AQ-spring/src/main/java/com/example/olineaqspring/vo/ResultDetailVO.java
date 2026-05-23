package com.example.olineaqspring.vo;

import com.example.olineaqspring.entity.Exam;
import com.example.olineaqspring.entity.ExamHistory;
import com.example.olineaqspring.entity.ExamResult;
import lombok.Data;

import java.util.List;

@Data
public class ResultDetailVO {
    private ExamResult result;
    private Exam exam;
    private List<ResultAnswerVO> answers;
    private List<ExamHistory> history;
}

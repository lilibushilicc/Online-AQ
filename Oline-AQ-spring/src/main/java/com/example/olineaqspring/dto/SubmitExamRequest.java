package com.example.olineaqspring.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubmitExamRequest {
    private Integer studentId;
    private Integer useTime;
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Integer questionId;
        private String studentAnswer;
    }
}

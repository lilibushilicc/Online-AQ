package com.example.olineaqspring.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubmitPracticeRequest {
    private List<PracticeAnswer> answers;

    @Data
    public static class PracticeAnswer {
        private Integer questionId;
        private String answer;
    }
}

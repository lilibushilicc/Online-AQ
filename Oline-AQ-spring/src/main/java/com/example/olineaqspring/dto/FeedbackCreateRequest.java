package com.example.olineaqspring.dto;

import lombok.Data;

@Data
public class FeedbackCreateRequest {
    private Integer questionId;
    private Integer examId;
    private String feedbackType;
    private String description;
}

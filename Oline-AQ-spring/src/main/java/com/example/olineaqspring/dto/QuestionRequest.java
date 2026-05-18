package com.example.olineaqspring.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuestionRequest {
    private String questionContent;
    private String questionType;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer;
    private BigDecimal score;
    private String category;
}

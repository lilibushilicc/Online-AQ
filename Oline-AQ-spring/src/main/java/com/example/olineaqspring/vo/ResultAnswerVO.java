package com.example.olineaqspring.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResultAnswerVO {
    private Integer answerId;
    private Integer questionId;
    private String questionContent;
    private String questionType;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String studentAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private BigDecimal score;
    private String reviewStatus;
}

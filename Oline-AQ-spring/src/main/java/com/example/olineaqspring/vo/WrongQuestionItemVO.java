package com.example.olineaqspring.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WrongQuestionItemVO {
    private Integer answerId;
    private Integer itemId;
    private Integer questionId;
    private String questionContent;
    private String questionType;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String studentAnswer;
    private String correctAnswer;
    private BigDecimal score;
    private LocalDateTime submitTime;
    private String reviewStatus;
}

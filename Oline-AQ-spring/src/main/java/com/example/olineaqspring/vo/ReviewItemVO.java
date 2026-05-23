package com.example.olineaqspring.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReviewItemVO {
    private Integer answerId;
    private Integer questionId;
    private String questionContent;
    private String questionType;
    private String studentAnswer;
    private String correctAnswer;
    private BigDecimal score;
    private String examName;
    private String studentName;
    private LocalDateTime submitTime;
}

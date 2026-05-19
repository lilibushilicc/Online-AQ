package com.example.olineaqspring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuestionRequest {
    @NotBlank(message = "题目内容不能为空")
    private String questionContent;

    @NotBlank(message = "题型不能为空")
    private String questionType;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    @NotBlank(message = "正确答案不能为空")
    private String correctAnswer;

    @Positive(message = "分值必须大于 0")
    private BigDecimal score;

    private String category;
}

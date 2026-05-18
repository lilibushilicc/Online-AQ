package com.example.olineaqspring.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionBatchScoreRequest {
    private List<Integer> questionIds;
    private BigDecimal score;
}

package com.example.olineaqspring.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionBatchDeleteRequest {
    private List<Integer> questionIds;
}

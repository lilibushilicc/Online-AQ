package com.example.olineaqspring.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionBatchCategoryRequest {
    private List<Integer> questionIds;
    private String category;
}

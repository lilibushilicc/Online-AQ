package com.example.olineaqspring.dto;

import lombok.Data;

import java.util.List;

@Data
public class PublishExamRequest {
    private Boolean assignAll;
    private List<Integer> studentIds;
}

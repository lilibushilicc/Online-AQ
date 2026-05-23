package com.example.olineaqspring.vo;

import lombok.Data;

import java.util.List;

@Data
public class WrongQuestionGroupVO {
    private Integer examId;
    private String examName;
    private List<WrongQuestionItemVO> questions;
}

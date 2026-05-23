package com.example.olineaqspring.vo;

import com.example.olineaqspring.entity.WrongNotebook;
import lombok.Data;

import java.util.List;

@Data
public class WrongNotebookDetailVO {
    private WrongNotebook notebook;
    private List<WrongQuestionGroupVO> groups;
    private Integer count;
}

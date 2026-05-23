package com.example.olineaqspring.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WrongNotebookSummaryVO {
    private Integer notebookId;
    private String notebookName;
    private String description;
    private LocalDateTime createTime;
    private Integer itemCount;
}

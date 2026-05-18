package com.example.olineaqspring.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedbackListVO {
    private Integer feedbackId;
    private Integer questionId;
    private String questionContent;
    private String studentName;
    private String feedbackType;
    private String description;
    private String status;
    private String rejectReason;
    private LocalDateTime createTime;
    private Integer pendingCount;
}

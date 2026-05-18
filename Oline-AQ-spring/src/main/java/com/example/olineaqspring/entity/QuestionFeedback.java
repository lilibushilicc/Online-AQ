package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question_feedback")
public class QuestionFeedback {
    @TableId(type = IdType.AUTO)
    private Integer feedbackId;
    private Integer questionId;
    private Integer studentId;
    private Integer examId;
    private String feedbackType;
    private String description;
    private String status;
    private String rejectReason;
    private String resolveType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

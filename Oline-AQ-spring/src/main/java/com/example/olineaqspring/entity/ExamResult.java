package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_result")
public class ExamResult {
    @TableId(type = IdType.AUTO)
    private Integer resultId;
    private Integer examId;
    private Integer studentId;
    private String attemptId;
    private BigDecimal totalScore;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer useTime;
    private LocalDateTime submitTime;
}

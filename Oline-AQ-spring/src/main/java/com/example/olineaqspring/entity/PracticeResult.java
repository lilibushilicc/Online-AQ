package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("practice_result")
public class PracticeResult {
    @TableId(type = IdType.AUTO)
    private Integer practiceId;
    private Integer studentId;
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer wrongCount;
    private BigDecimal totalScore;
    private LocalDateTime submitTime;
}

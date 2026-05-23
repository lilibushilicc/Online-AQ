package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_answer")
public class StudentAnswer {
    @TableId(type = IdType.AUTO)
    private Integer answerId;
    private Integer examId;
    private Integer studentId;
    private String attemptId;
    private Integer questionId;
    private String studentAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private BigDecimal score;
    private LocalDateTime submitTime;
    private String reviewStatus;
}

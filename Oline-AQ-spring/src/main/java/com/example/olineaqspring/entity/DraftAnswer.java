package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("draft_answer")
public class DraftAnswer {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer examId;
    private Integer studentId;
    private String attemptId;
    private String answers;
    private String shuffleSnapshot;
    private Integer useTime;
    private LocalDateTime updatedAt;
}

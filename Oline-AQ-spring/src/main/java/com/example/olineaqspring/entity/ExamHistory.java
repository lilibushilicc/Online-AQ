package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_history")
public class ExamHistory {
    @TableId(type = IdType.AUTO)
    private Integer historyId;
    private Integer examId;
    private Integer operatorId;
    private String actionType;
    private String actionDetail;
    private LocalDateTime createTime;
    @TableField(exist = false)
    private String operatorName;
}

package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exam_student")
public class ExamStudent {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer examId;
    private Integer studentId;
}

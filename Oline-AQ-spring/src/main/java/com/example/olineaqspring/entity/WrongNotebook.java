package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wrong_notebook")
public class WrongNotebook {
    @TableId(type = IdType.AUTO)
    private Integer notebookId;
    private Integer studentId;
    private String notebookName;
    private String description;
    private LocalDateTime createTime;
}

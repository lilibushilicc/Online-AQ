package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wrong_notebook_item")
public class WrongNotebookItem {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer notebookId;
    private Integer answerId;
    private LocalDateTime createTime;
}

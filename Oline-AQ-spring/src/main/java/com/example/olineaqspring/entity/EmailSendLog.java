package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("email_send_log")
public class EmailSendLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer accountId;
    private String sendTo;
    private String sendType;
    private Boolean success;
    private LocalDateTime sendTime;
}

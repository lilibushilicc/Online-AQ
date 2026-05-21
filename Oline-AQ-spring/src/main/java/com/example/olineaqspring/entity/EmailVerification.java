package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("email_verification")
public class EmailVerification {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String email;
    private String code;
    private LocalDateTime expireTime;
    private Boolean used;
    private LocalDateTime createTime;
}

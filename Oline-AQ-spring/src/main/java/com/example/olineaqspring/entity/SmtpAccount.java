package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("smtp_account")
public class SmtpAccount {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String fromAddress;
    private Boolean active;

    private Boolean enabled;

    private Integer hourlyLimit;
    private Integer dailyLimit;
    private Integer hourlySent;
    private Integer dailySent;
    private LocalDateTime lastHourlyReset;
    private LocalDateTime lastDailyReset;

    private LocalDateTime createTime;
}

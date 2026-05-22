package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("idempotent_record")
public class IdempotentRecord {
    @TableId(type = IdType.INPUT)
    private String idempotentKey;
    private String resultType;
    private String resultJson;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}

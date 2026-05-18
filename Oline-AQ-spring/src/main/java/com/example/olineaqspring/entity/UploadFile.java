package com.example.olineaqspring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("upload_file")
public class UploadFile {
    @TableId(type = IdType.AUTO)
    private Integer fileId;
    private String fileName;
    private String fileType;
    private String filePath;
    private String rawText;
    private Integer uploadUserId;
    private String status;
    private LocalDateTime createTime;
}

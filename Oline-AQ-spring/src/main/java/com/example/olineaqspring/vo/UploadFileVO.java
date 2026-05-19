package com.example.olineaqspring.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UploadFileVO {
    private Integer fileId;
    private String fileName;
    private String fileType;
    private String status;
    private Integer questionCount;
    private LocalDateTime createTime;
}

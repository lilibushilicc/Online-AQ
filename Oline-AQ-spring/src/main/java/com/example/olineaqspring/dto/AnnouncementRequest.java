package com.example.olineaqspring.dto;

import lombok.Data;

@Data
public class AnnouncementRequest {
    private String title;
    private String content;
    private Boolean active;
}

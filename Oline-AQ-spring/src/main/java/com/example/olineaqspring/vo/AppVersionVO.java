package com.example.olineaqspring.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppVersionVO {
    private int versionCode;
    private String versionName;
    private String downloadUrl;
    private String releaseNotes;
    private boolean forceUpdate;
}

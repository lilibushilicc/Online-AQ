package com.example.olineaqspring.controller;

import com.example.olineaqspring.service.ConfigService;
import com.example.olineaqspring.vo.ApiResponse;
import com.example.olineaqspring.vo.AppVersionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
public class AppVersionController {

    private final ConfigService configService;

    public AppVersionController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/version/latest")
    public ApiResponse<AppVersionVO> getLatestVersion() {
        String code = configService.get("app.version.code");
        String name = configService.get("app.version.name");
        String url = configService.get("app.version.download_url");
        String notes = configService.get("app.version.release_notes");
        String force = configService.get("app.version.force_update");

        AppVersionVO vo = new AppVersionVO();
        vo.setVersionCode(code == null ? 1 : Integer.parseInt(code));
        vo.setVersionName(name == null ? "1.0" : name);
        vo.setDownloadUrl(url == null ? "" : url);
        vo.setReleaseNotes(notes == null ? "" : notes);
        vo.setForceUpdate(Boolean.parseBoolean(force));
        return ApiResponse.ok(vo);
    }
}

package com.example.olineaqspring.controller;

import com.example.olineaqspring.exception.UnauthorizedException;
import com.example.olineaqspring.service.ConfigService;
import com.example.olineaqspring.service.R2StorageService;
import com.example.olineaqspring.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final ConfigService configService;
    private final R2StorageService r2StorageService;

    public ConfigController(ConfigService configService, R2StorageService r2StorageService) {
        this.configService = configService;
        this.r2StorageService = r2StorageService;
    }

    @GetMapping
    public ApiResponse<Map<String, String>> getAll(HttpServletRequest request) {
        checkAdmin(request);
        return ApiResponse.ok(configService.getAll());
    }

    @PutMapping
    public ApiResponse<Void> update(@RequestBody Map<String, String> configMap, HttpServletRequest request) {
        checkAdmin(request);
        configService.update(configMap);
        return ApiResponse.ok("保存成功", null);
    }

    @PostMapping("/test-r2")
    public ApiResponse<Map<String, String>> testR2(HttpServletRequest request) {
        checkAdmin(request);
        String result = r2StorageService.testConnection();
        if ("ok".equals(result)) {
            return ApiResponse.ok(Collections.singletonMap("result", "ok"));
        }
        return ApiResponse.fail(result);
    }

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            throw new UnauthorizedException("仅管理员可操作系统配置");
        }
    }
}

package com.example.olineaqspring.controller;

import com.example.olineaqspring.exception.UnauthorizedException;
import com.example.olineaqspring.service.AiQuestionParseService;
import com.example.olineaqspring.service.ConfigService;
import com.example.olineaqspring.service.R2StorageService;
import com.example.olineaqspring.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final AiQuestionParseService aiQuestionParseService;
    private final JdbcTemplate jdbcTemplate;

    public ConfigController(ConfigService configService, R2StorageService r2StorageService,
                            AiQuestionParseService aiQuestionParseService, JdbcTemplate jdbcTemplate) {
        this.configService = configService;
        this.r2StorageService = r2StorageService;
        this.aiQuestionParseService = aiQuestionParseService;
        this.jdbcTemplate = jdbcTemplate;
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

    @PostMapping("/migrate-exam-shuffle")
    public ApiResponse<Map<String, Object>> migrateExamShuffle(HttpServletRequest request) {
        checkAdmin(request);
        try {
            jdbcTemplate.execute("ALTER TABLE exam ADD COLUMN IF NOT EXISTS shuffle_questions BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE exam ADD COLUMN IF NOT EXISTS shuffle_answers BOOLEAN DEFAULT FALSE");
            return ApiResponse.ok(Map.of("result", "ok"));
        } catch (Exception e) {
            return ApiResponse.fail("迁移失败：" + e.getMessage());
        }
    }

    @PostMapping("/test-ai")
    public ApiResponse<Map<String, Object>> testAi(HttpServletRequest request) {
        checkAdmin(request);
        try {
            String endpoint = configService.get("ai.endpoint");
            String apiKey = configService.get("ai.api_key");
            String model = configService.get("ai.model");
            if (endpoint == null || apiKey == null) {
                return ApiResponse.fail("请先填写 AI Endpoint 和 API Key");
            }
            // Simple test: ask AI to say "ok"
            String result = aiQuestionParseService.testConnection();
            return ApiResponse.ok(Map.of("result", result));
        } catch (Exception e) {
            return ApiResponse.fail("AI 连接测试失败：" + e.getMessage());
        }
    }

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            throw new UnauthorizedException("仅管理员可操作系统配置");
        }
    }
}

package com.example.olineaqspring.controller;

import com.example.olineaqspring.entity.SmtpAccount;
import com.example.olineaqspring.exception.UnauthorizedException;
import com.example.olineaqspring.service.AiQuestionParseService;
import com.example.olineaqspring.service.ConfigService;
import com.example.olineaqspring.service.EmailService;
import com.example.olineaqspring.service.R2StorageService;
import com.example.olineaqspring.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final ConfigService configService;
    private final R2StorageService r2StorageService;
    private final AiQuestionParseService aiQuestionParseService;
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;

    public ConfigController(ConfigService configService, R2StorageService r2StorageService,
                            AiQuestionParseService aiQuestionParseService, EmailService emailService,
                            JdbcTemplate jdbcTemplate) {
        this.configService = configService;
        this.r2StorageService = r2StorageService;
        this.aiQuestionParseService = aiQuestionParseService;
        this.emailService = emailService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/public/login")
    public ApiResponse<Map<String, String>> getPublicLoginConfig() {
        Map<String, String> result = new HashMap<>();
        String count = configService.get("login.logo.click.count");
        result.put("login.logo.click.count", count != null ? count : "3");
        String method = configService.get("login.admin.method");
        result.put("login.admin.method", method != null ? method : "both");
        return ApiResponse.ok(result);
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

    @PostMapping("/test-email")
    public ApiResponse<Map<String, String>> testEmail(HttpServletRequest request, @RequestBody(required = false) Map<String, Integer> body) {
        checkAdmin(request);
        try {
            Integer accountId = body != null ? body.get("accountId") : null;
            emailService.testConnection(accountId);
            return ApiResponse.ok(Collections.singletonMap("result", "ok"));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/smtp-accounts")
    public ApiResponse<List<SmtpAccount>> listAccounts(HttpServletRequest request) {
        checkAdmin(request);
        return ApiResponse.ok(emailService.listAccounts());
    }

    @PostMapping("/smtp-accounts")
    public ApiResponse<Void> createAccount(HttpServletRequest request, @RequestBody SmtpAccount account) {
        checkAdmin(request);
        emailService.saveAccount(account);
        return ApiResponse.ok("创建成功", null);
    }

    @PutMapping("/smtp-accounts/{id}")
    public ApiResponse<Void> updateAccount(HttpServletRequest request, @PathVariable Integer id, @RequestBody SmtpAccount account) {
        checkAdmin(request);
        account.setId(id);
        emailService.updateAccount(account);
        return ApiResponse.ok("更新成功", null);
    }

    @DeleteMapping("/smtp-accounts/{id}")
    public ApiResponse<Void> deleteAccount(HttpServletRequest request, @PathVariable Integer id) {
        checkAdmin(request);
        emailService.deleteAccount(id);
        return ApiResponse.ok("删除成功", null);
    }

    @PutMapping("/smtp-accounts/{id}/activate")
    public ApiResponse<Void> activateAccount(HttpServletRequest request, @PathVariable Integer id) {
        checkAdmin(request);
        emailService.activateAccount(id);
        return ApiResponse.ok("切换成功", null);
    }

    @PutMapping("/smtp-accounts/{id}/toggle-enabled")
    public ApiResponse<Void> toggleEnabled(HttpServletRequest request, @PathVariable Integer id) {
        checkAdmin(request);
        emailService.toggleEnabled(id);
        return ApiResponse.ok("操作成功", null);
    }

    @GetMapping("/smtp-accounts/{id}/stats")
    public ApiResponse<Map<String, Object>> accountStats(HttpServletRequest request, @PathVariable Integer id) {
        checkAdmin(request);
        Map<String, Object> stats = emailService.getAccountStats(id);
        SmtpAccount account = emailService.listAccounts().stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
        if (account != null) {
            stats.put("hourlyLimit", account.getHourlyLimit());
            stats.put("dailyLimit", account.getDailyLimit());
            stats.put("hourlySent", account.getHourlySent() != null ? account.getHourlySent() : 0);
            stats.put("dailySent", account.getDailySent() != null ? account.getDailySent() : 0);
        }
        return ApiResponse.ok(stats);
    }

    @PostMapping("/smtp-accounts/test")
    public ApiResponse<Map<String, String>> testSmtpInline(HttpServletRequest request, @RequestBody SmtpAccount account) {
        checkAdmin(request);
        try {
            emailService.testInlineConnection(account);
            return ApiResponse.ok(Collections.singletonMap("result", "ok"));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/email-stats")
    public ApiResponse<Map<String, Object>> emailStats(HttpServletRequest request) {
        checkAdmin(request);
        return ApiResponse.ok(emailService.getStats());
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

package com.example.olineaqspring.controller;

import com.example.olineaqspring.annotation.AdminOnly;
import com.example.olineaqspring.service.DashboardService;
import com.example.olineaqspring.vo.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @AdminOnly("仅管理员可查看仪表盘统计")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(dashboardService.stats());
    }
}

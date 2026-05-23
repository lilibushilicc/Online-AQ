package com.example.olineaqspring.controller;

import com.example.olineaqspring.dto.SubmitPracticeRequest;
import com.example.olineaqspring.entity.PracticeResult;
import com.example.olineaqspring.service.PracticeService;
import com.example.olineaqspring.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/practice")
public class PracticeController {
    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping("/submit")
    public ApiResponse<PracticeResult> submit(@RequestBody SubmitPracticeRequest request,
                                              HttpServletRequest servletRequest) {
        Integer userId = (Integer) servletRequest.getAttribute("userId");
        return ApiResponse.ok("提交成功", practiceService.submit(request, userId));
    }

    @GetMapping("/history")
    public ApiResponse<List<PracticeResult>> history(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return ApiResponse.ok(practiceService.history(userId));
    }
}

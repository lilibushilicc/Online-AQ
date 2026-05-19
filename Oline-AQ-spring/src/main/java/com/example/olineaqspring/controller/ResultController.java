package com.example.olineaqspring.controller;

import com.example.olineaqspring.entity.ExamResult;
import com.example.olineaqspring.service.ResultService;
import com.example.olineaqspring.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/results")
public class ResultController {
    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/my")
    public ApiResponse<List<ExamResult>> myResults(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return ApiResponse.ok("查询成功", resultService.myResults(userId));
    }

    @GetMapping("/wrong-questions")
    public ApiResponse<List<Map<String, Object>>> wrongQuestions(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return ApiResponse.ok("查询成功", resultService.wrongQuestions(userId));
    }

    @GetMapping("/exam/{examId}")
    public ApiResponse<List<ExamResult>> examResults(@PathVariable Integer examId) {
        return ApiResponse.ok("查询成功", resultService.examResults(examId));
    }

    @GetMapping("/{resultId}")
    public ApiResponse<Map<String, Object>> resultDetail(@PathVariable Integer resultId, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return ApiResponse.ok("查询成功", resultService.resultDetail(resultId, userId, role));
    }
}

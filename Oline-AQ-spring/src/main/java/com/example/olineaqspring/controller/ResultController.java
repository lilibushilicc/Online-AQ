package com.example.olineaqspring.controller;

import com.example.olineaqspring.entity.ExamResult;
import com.example.olineaqspring.service.ExportService;
import com.example.olineaqspring.service.ResultService;
import com.example.olineaqspring.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final ExportService exportService;

    public ResultController(ResultService resultService, ExportService exportService) {
        this.resultService = resultService;
        this.exportService = exportService;
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

    @GetMapping("/export/{examId}")
    public ResponseEntity<byte[]> exportExamResults(@PathVariable Integer examId) {
        List<ExamResult> results = resultService.examResults(examId);
        byte[] bytes = exportService.exportResults(results);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=成绩导出.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/export/my")
    public ResponseEntity<byte[]> exportMyResults(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        List<ExamResult> results = resultService.myResults(userId);
        byte[] bytes = exportService.exportResults(results);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=我的成绩.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/export/wrong")
    public ResponseEntity<byte[]> exportWrongQuestions(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        List<Map<String, Object>> groups = resultService.wrongQuestions(userId);
        byte[] bytes = exportService.exportWrongQuestions(groups);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=错题导出.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/export/{resultId}/detail")
    public ResponseEntity<byte[]> exportResultDetail(@PathVariable Integer resultId, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        Map<String, Object> detail = resultService.resultDetail(resultId, userId, role);
        byte[] bytes = exportService.exportResultDetail(detail);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=成绩详情.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}

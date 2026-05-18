package com.example.olineaqspring.controller;

import com.example.olineaqspring.dto.ExamCreateRequest;
import com.example.olineaqspring.dto.PublishExamRequest;
import com.example.olineaqspring.dto.SubmitExamRequest;
import com.example.olineaqspring.entity.Exam;
import com.example.olineaqspring.entity.ExamHistory;
import com.example.olineaqspring.service.ExamService;
import com.example.olineaqspring.service.ResultService;
import com.example.olineaqspring.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exams")
public class ExamController {
    private final ExamService examService;
    private final ResultService resultService;

    public ExamController(ExamService examService, ResultService resultService) {
        this.examService = examService;
        this.resultService = resultService;
    }

    @PostMapping
    public ApiResponse<Exam> create(@RequestBody ExamCreateRequest request, HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("创建成功", examService.create(request, userId));
    }

    @GetMapping
    public ApiResponse<List<Exam>> list() {
        return ApiResponse.ok("查询成功", examService.list());
    }

    @GetMapping("/student")
    public ApiResponse<List<Exam>> listForStudent(HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("查询成功", examService.listForStudent(userId));
    }

    @GetMapping("/{examId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Integer examId) {
        return ApiResponse.ok("查询成功", examService.detail(examId));
    }

    @GetMapping("/{examId}/history")
    public ApiResponse<List<ExamHistory>> history(@PathVariable Integer examId) {
        return ApiResponse.ok("查询成功", examService.history(examId));
    }

    @PutMapping("/{examId}/publish")
    public ApiResponse<Void> publish(@PathVariable Integer examId, @RequestBody(required = false) PublishExamRequest request,
                                     HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        examService.publish(examId, userId, request);
        return ApiResponse.ok("发布成功", null);
    }

    @PutMapping("/{examId}/close")
    public ApiResponse<Void> close(@PathVariable Integer examId, HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        examService.close(examId, userId);
        return ApiResponse.ok("关闭成功", null);
    }

    @DeleteMapping("/{examId}")
    public ApiResponse<Void> delete(@PathVariable Integer examId) {
        examService.delete(examId);
        return ApiResponse.ok("删除成功", null);
    }

    @PostMapping("/{examId}/submit")
    public ApiResponse<Map<String, Object>> submit(@PathVariable Integer examId, @RequestBody SubmitExamRequest request,
                                                   HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("提交成功", resultService.submit(examId, request, userId));
    }
}

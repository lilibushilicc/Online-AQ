package com.example.olineaqspring.controller;

import com.example.olineaqspring.dto.ExamCreateRequest;
import com.example.olineaqspring.dto.ExamSettingsRequest;
import com.example.olineaqspring.dto.PublishExamRequest;
import com.example.olineaqspring.dto.SubmitExamRequest;
import com.example.olineaqspring.entity.Exam;
import com.example.olineaqspring.entity.ExamHistory;
import com.example.olineaqspring.service.ExamService;
import com.example.olineaqspring.service.ExportService;
import com.example.olineaqspring.service.ResultService;
import com.example.olineaqspring.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exams")
public class ExamController {
    private final ExamService examService;
    private final ResultService resultService;
    private final ExportService exportService;

    public ExamController(ExamService examService, ResultService resultService, ExportService exportService) {
        this.examService = examService;
        this.resultService = resultService;
        this.exportService = exportService;
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
    public ApiResponse<Map<String, Object>> detail(@PathVariable Integer examId, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        return ApiResponse.ok("查询成功", examService.detail(examId, role));
    }

    @GetMapping("/{examId}/export/excel")
    public ResponseEntity<byte[]> exportExamExcel(@PathVariable Integer examId) {
        Map<String, Object> detail = examService.detail(examId);
        byte[] bytes = exportService.exportExamPaperExcel(detail);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentWithFilename(getExamFileName(detail) + ".xlsx"))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/{examId}/export/word")
    public ResponseEntity<byte[]> exportExamWord(@PathVariable Integer examId) {
        Map<String, Object> detail = examService.detail(examId);
        byte[] bytes = exportService.exportExamPaperWord(detail);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentWithFilename(getExamFileName(detail) + ".docx"))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(bytes);
    }

    private String attachmentWithFilename(String filename) {
        return ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    private String getExamFileName(Map<String, Object> detail) {
        Object exam = detail.get("exam");
        if (exam instanceof Exam e) {
            String name = e.getExamName();
            return name != null ? name.replaceAll("[\\\\/:*?\"<>|]", "_") : "试卷";
        }
        return "试卷";
    }

    @PutMapping("/{examId}")
    public ApiResponse<Exam> update(@PathVariable Integer examId, @RequestBody ExamCreateRequest request,
                                    HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("更新成功", examService.update(examId, request, userId));
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

    @PutMapping("/{examId}/settings")
    public ApiResponse<Exam> updateSettings(@PathVariable Integer examId, @RequestBody ExamSettingsRequest request,
                                            HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("更新成功", examService.updateSettings(examId, request, userId));
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

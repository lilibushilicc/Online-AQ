package com.example.olineaqspring.controller;

import com.example.olineaqspring.annotation.AdminOnly;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.dto.ExamCreateRequest;
import com.example.olineaqspring.dto.ExamSettingsRequest;
import com.example.olineaqspring.dto.PublishExamRequest;
import com.example.olineaqspring.dto.SubmitExamRequest;
import com.example.olineaqspring.entity.DraftAnswer;
import com.example.olineaqspring.entity.Exam;
import com.example.olineaqspring.entity.ExamHistory;
import com.example.olineaqspring.mapper.DraftAnswerMapper;
import com.example.olineaqspring.service.ExamService;
import com.example.olineaqspring.service.ExportService;
import com.example.olineaqspring.service.ResultService;
import com.example.olineaqspring.vo.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    private final DraftAnswerMapper draftAnswerMapper;
    private final ObjectMapper objectMapper;

    public ExamController(ExamService examService, ResultService resultService, ExportService exportService,
                          DraftAnswerMapper draftAnswerMapper, ObjectMapper objectMapper) {
        this.examService = examService;
        this.resultService = resultService;
        this.exportService = exportService;
        this.draftAnswerMapper = draftAnswerMapper;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @AdminOnly("仅管理员可创建考试")
    public ApiResponse<Exam> create(@RequestBody ExamCreateRequest request, HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("创建成功", examService.create(request, userId));
    }

    @GetMapping
    @AdminOnly("仅管理员可查看考试列表")
    public ApiResponse<List<Exam>> list() {
        return ApiResponse.ok("查询成功", examService.list());
    }

    @GetMapping("/student")
    public ApiResponse<List<Exam>> listForStudent(HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("查询成功", examService.listForStudent(userId));
    }

    @GetMapping("/{examId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Integer examId,
                                                   @RequestParam(required = false) String attemptId,
                                                   HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        if ("student".equals(role)) {
            examService.assertStudentCanAccessExam(examId, userId);
            return ApiResponse.ok("查询成功", examService.detail(examId, userId, attemptId, role));
        }
        return ApiResponse.ok("查询成功", examService.detail(examId, null, attemptId, role));
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
    @AdminOnly("仅管理员可编辑考试")
    public ApiResponse<Exam> update(@PathVariable Integer examId, @RequestBody ExamCreateRequest request,
                                    HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("更新成功", examService.update(examId, request, userId));
    }

    @GetMapping("/{examId}/history")
    @AdminOnly("仅管理员可查看考试历史")
    public ApiResponse<List<ExamHistory>> history(@PathVariable Integer examId) {
        return ApiResponse.ok("查询成功", examService.history(examId));
    }

    @PutMapping("/{examId}/publish")
    @AdminOnly("仅管理员可发布考试")
    public ApiResponse<Void> publish(@PathVariable Integer examId, @RequestBody(required = false) PublishExamRequest request,
                                     HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        examService.publish(examId, userId, request);
        return ApiResponse.ok("发布成功", null);
    }

    @PutMapping("/{examId}/settings")
    @AdminOnly("仅管理员可更新考试设置")
    public ApiResponse<Exam> updateSettings(@PathVariable Integer examId, @RequestBody ExamSettingsRequest request,
                                            HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("更新成功", examService.updateSettings(examId, request, userId));
    }

    @PutMapping("/{examId}/close")
    @AdminOnly("仅管理员可关闭考试")
    public ApiResponse<Void> close(@PathVariable Integer examId, HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        examService.close(examId, userId);
        return ApiResponse.ok("关闭成功", null);
    }

    @DeleteMapping("/{examId}")
    @AdminOnly("仅管理员可删除考试")
    public ApiResponse<Void> delete(@PathVariable Integer examId) {
        examService.delete(examId);
        return ApiResponse.ok("删除成功", null);
    }

    @PutMapping("/{examId}/draft")
    public ApiResponse<Void> saveDraft(@PathVariable Integer examId, @RequestBody SubmitExamRequest request,
                                       HttpServletRequest httpRequest) throws JsonProcessingException {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");
        if ("student".equals(role)) {
            examService.assertStudentCanAccessExam(examId, userId);
        }
        DraftAnswer draft = examService.getOrCreateDraft(examId, userId, request.getAttemptId());
        if (request.getAttemptId() != null && !request.getAttemptId().isBlank() && !request.getAttemptId().equals(draft.getAttemptId())) {
            draft.setAttemptId(request.getAttemptId());
        }
        Map<String, String> answerMap = new java.util.HashMap<>();
        if (request.getAnswers() != null) {
            for (SubmitExamRequest.AnswerItem item : request.getAnswers()) {
                answerMap.put(String.valueOf(item.getQuestionId()), item.getStudentAnswer());
            }
        }
        draft.setAnswers(objectMapper.writeValueAsString(answerMap));
        draft.setUseTime(request.getUseTime());
        draft.setUpdatedAt(java.time.LocalDateTime.now());
        draftAnswerMapper.updateById(draft);
        return ApiResponse.ok("草稿已保存", null);
    }

    @GetMapping("/{examId}/draft")
    public ApiResponse<DraftAnswer> loadDraft(@PathVariable Integer examId, HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");
        if ("student".equals(role)) {
            examService.assertStudentCanAccessExam(examId, userId);
        }
        DraftAnswer draft = examService.getOrCreateDraft(examId, userId, null);
        return ApiResponse.ok(draft);
    }

    @DeleteMapping("/{examId}/draft")
    public ApiResponse<Void> clearDraft(@PathVariable Integer examId, HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");
        if ("student".equals(role)) {
            examService.assertStudentCanAccessExam(examId, userId);
        }
        draftAnswerMapper.delete(new LambdaQueryWrapper<DraftAnswer>()
                .eq(DraftAnswer::getExamId, examId)
                .eq(DraftAnswer::getStudentId, userId));
        return ApiResponse.ok("草稿已清除", null);
    }

    @PostMapping("/{examId}/submit")
    public ApiResponse<Map<String, Object>> submit(@PathVariable Integer examId, @RequestBody SubmitExamRequest request,
                                                   HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("提交成功", resultService.submit(examId, request, userId));
    }
}

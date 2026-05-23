package com.example.olineaqspring.controller;

import com.example.olineaqspring.annotation.AdminOnly;
import com.example.olineaqspring.entity.ExamResult;
import com.example.olineaqspring.service.ExportService;
import com.example.olineaqspring.service.ResultService;
import com.example.olineaqspring.vo.ApiResponse;
import com.example.olineaqspring.vo.ResultDetailVO;
import com.example.olineaqspring.vo.ReviewItemVO;
import com.example.olineaqspring.vo.WrongQuestionGroupVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
        return ApiResponse.ok(resultService.myResults(userId));
    }

    @GetMapping("/wrong-questions")
    public ApiResponse<List<WrongQuestionGroupVO>> wrongQuestions(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return ApiResponse.ok(resultService.wrongQuestions(userId));
    }

    @GetMapping("/exam/{examId}")
    @AdminOnly("仅管理员可查看考试成绩")
    public ApiResponse<List<ExamResult>> examResults(@PathVariable Integer examId) {
        return ApiResponse.ok(resultService.examResults(examId));
    }

    @GetMapping("/{resultId}")
    public ApiResponse<ResultDetailVO> resultDetail(@PathVariable Integer resultId, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return ApiResponse.ok(resultService.resultDetail(resultId, userId, role));
    }

    @GetMapping("/pending-review")
    @AdminOnly("仅管理员可查看待评分列表")
    public ApiResponse<List<ReviewItemVO>> pendingReviews() {
        return ApiResponse.ok(resultService.pendingReviews());
    }

    @PutMapping("/review/{answerId}")
    @AdminOnly("仅管理员可进行评分")
    public ApiResponse<Void> reviewAnswer(@PathVariable Integer answerId, @RequestParam BigDecimal score) {
        resultService.reviewAnswer(answerId, score);
        return ApiResponse.ok("评分成功", null);
    }

    @GetMapping("/export/{examId}")
    @AdminOnly("仅管理员可导出考试成绩")
    public ResponseEntity<byte[]> exportExamResults(@PathVariable Integer examId) {
        return buildExportExcel(exportService.exportResults(resultService.examResults(examId)), "成绩导出.xlsx");
    }

    @GetMapping("/export/my")
    public ResponseEntity<byte[]> exportMyResults(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return buildExportExcel(exportService.exportResults(resultService.myResults(userId)), "我的成绩.xlsx");
    }

    @GetMapping("/export/wrong")
    public ResponseEntity<byte[]> exportWrongQuestions(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return buildExportExcel(exportService.exportWrongQuestions(resultService.wrongQuestions(userId)), "错题导出.xlsx");
    }

    @GetMapping("/export/{resultId}/detail")
    public ResponseEntity<byte[]> exportResultDetail(@PathVariable Integer resultId, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return buildExportExcel(exportService.exportResultDetail(resultService.resultDetail(resultId, userId, role)), "成绩详情.xlsx");
    }

    @GetMapping("/export/{resultId}/detail/word")
    public ResponseEntity<byte[]> exportResultDetailWord(@PathVariable Integer resultId, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return buildExportWord(exportService.exportResultDetailWord(resultService.resultDetail(resultId, userId, role)), "成绩详情.docx");
    }

    private static ResponseEntity<byte[]> buildExportExcel(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, createAttachmentWithFilename(filename))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private static ResponseEntity<byte[]> buildExportWord(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, createAttachmentWithFilename(filename))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(bytes);
    }

    private static String createAttachmentWithFilename(String filename) {
        return ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}

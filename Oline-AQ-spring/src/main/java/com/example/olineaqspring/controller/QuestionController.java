package com.example.olineaqspring.controller;

import com.example.olineaqspring.dto.QuestionBatchRequest;
import com.example.olineaqspring.dto.QuestionRequest;
import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.service.ExportService;
import com.example.olineaqspring.service.QuestionService;
import com.example.olineaqspring.vo.ApiResponse;
import com.example.olineaqspring.vo.PageResult;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    private final QuestionService questionService;
    private final ExportService exportService;

    public QuestionController(QuestionService questionService, ExportService exportService) {
        this.questionService = questionService;
        this.exportService = exportService;
    }

    @GetMapping
    public ApiResponse<PageResult<Question>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer pageSize) {
        return ApiResponse.ok("查询成功", questionService.list(category, page, pageSize));
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok("查询成功", questionService.categories());
    }

    @PostMapping
    public ApiResponse<Question> create(@RequestBody @Valid QuestionRequest request) {
        return ApiResponse.ok("新增成功", questionService.create(request));
    }

    @PutMapping("/{questionId}")
    public ApiResponse<Question> update(@PathVariable Integer questionId, @RequestBody @Valid QuestionRequest request) {
        return ApiResponse.ok("修改成功", questionService.update(questionId, request));
    }

    @DeleteMapping("/{questionId}")
    public ApiResponse<Void> delete(@PathVariable Integer questionId) {
        questionService.delete(questionId);
        return ApiResponse.ok("删除成功", null);
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> deleteBatch(@RequestBody QuestionBatchRequest request) {
        questionService.deleteBatch(request.getQuestionIds());
        return ApiResponse.ok("批量删除成功", null);
    }

    @PostMapping("/batch-score")
    public ApiResponse<Void> updateBatchScore(@RequestBody QuestionBatchRequest request) {
        questionService.updateScoreBatch(request.getQuestionIds(), request.getScore());
        return ApiResponse.ok("批量设置分值成功", null);
    }

    @PostMapping("/batch-category")
    public ApiResponse<Void> updateBatchCategory(@RequestBody QuestionBatchRequest request) {
        questionService.updateCategoryBatch(request.getQuestionIds(), request.getCategory());
        return ApiResponse.ok("批量设置分类成功", null);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String category) {
        List<Question> questions = questionService.listAll(category);
        byte[] bytes = exportService.exportQuestions(questions);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("题库导出.xlsx", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}

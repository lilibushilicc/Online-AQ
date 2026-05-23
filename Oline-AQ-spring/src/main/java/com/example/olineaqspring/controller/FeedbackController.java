package com.example.olineaqspring.controller;

import com.example.olineaqspring.annotation.AdminOnly;
import com.example.olineaqspring.dto.FeedbackCreateRequest;
import com.example.olineaqspring.dto.QuestionRequest;
import com.example.olineaqspring.entity.QuestionFeedback;
import com.example.olineaqspring.service.FeedbackService;
import com.example.olineaqspring.vo.ApiResponse;
import com.example.olineaqspring.vo.FeedbackDetailVO;
import com.example.olineaqspring.vo.FeedbackListVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<QuestionFeedback> create(@RequestBody FeedbackCreateRequest request,
                                                 HttpServletRequest httpRequest) {
        Integer studentId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("反馈已提交", feedbackService.create(request, studentId));
    }

    @GetMapping
    @AdminOnly("仅管理员可查看反馈列表")
    public ApiResponse<List<FeedbackListVO>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(feedbackService.list(status));
    }

    @GetMapping("/{id}")
    @AdminOnly("仅管理员可查看反馈详情")
    public ApiResponse<FeedbackDetailVO> detail(@PathVariable Integer id) {
        return ApiResponse.ok(feedbackService.detail(id));
    }

    @PutMapping("/{id}/resolve")
    @AdminOnly("仅管理员可处理反馈")
    public ApiResponse<Map<String, Object>> resolve(@PathVariable Integer id,
                                                     @RequestBody QuestionRequest questionRequest,
                                                     HttpServletRequest request) {
        int affected = feedbackService.resolve(id, questionRequest);
        String msg = affected > 0
                ? String.format("题目已修改，已同步处理本题目其他 %d 条待处理反馈", affected)
                : "题目已修改，反馈已采纳";
        return ApiResponse.ok(msg, Map.of("affectedOther", affected));
    }

    @PutMapping("/{id}/reject")
    @AdminOnly("仅管理员可驳回反馈")
    public ApiResponse<Map<String, Object>> reject(@PathVariable Integer id,
                                                    @RequestBody Map<String, String> body,
                                                    HttpServletRequest request) {
        String reason = body.getOrDefault("rejectReason", "");
        int affected = feedbackService.reject(id, reason);
        String msg = affected > 0
                ? String.format("反馈已驳回，已同步处理本题目其他 %d 条待处理反馈", affected)
                : "反馈已驳回";
        return ApiResponse.ok(msg, Map.of("affectedOther", affected));
    }

    @GetMapping("/my")
    public ApiResponse<List<Integer>> myFeedbacks(@RequestParam List<Integer> questionIds,
                                                   HttpServletRequest httpRequest) {
        Integer studentId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok(feedbackService.myFeedbackQuestionIds(studentId, questionIds));
    }
}

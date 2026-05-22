package com.example.olineaqspring.service;

import com.example.olineaqspring.dto.FeedbackCreateRequest;
import com.example.olineaqspring.dto.QuestionRequest;
import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.QuestionFeedback;
import com.example.olineaqspring.mapper.QuestionFeedbackMapper;
import com.example.olineaqspring.mapper.QuestionMapper;
import com.example.olineaqspring.vo.FeedbackDetailVO;
import com.example.olineaqspring.utils.BeanUtils;
import com.example.olineaqspring.vo.FeedbackListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final QuestionFeedbackMapper feedbackMapper;
    private final QuestionMapper questionMapper;

    public QuestionFeedback create(FeedbackCreateRequest request, Integer studentId) {
        String lockKey = ("feedback_" + request.getQuestionId() + "_" + studentId).intern();
        synchronized (lockKey) {
            long existingCount = feedbackMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionFeedback>()
                            .eq(QuestionFeedback::getQuestionId, request.getQuestionId())
                            .eq(QuestionFeedback::getStudentId, studentId));
            if (existingCount > 0) {
                throw new RuntimeException("你已经反馈过这道题了，无需重复提交");
            }
            QuestionFeedback feedback = new QuestionFeedback();
            feedback.setQuestionId(request.getQuestionId());
            feedback.setStudentId(studentId);
            feedback.setExamId(request.getExamId());
            feedback.setFeedbackType(request.getFeedbackType());
            feedback.setDescription(request.getDescription());
            feedback.setStatus("pending");
            feedbackMapper.insert(feedback);
            return feedback;
        }
    }

    public List<FeedbackListVO> list(String status) {
        List<FeedbackListVO> list = feedbackMapper.selectFeedbackList(status);
        Map<Integer, Integer> pendingCounts = feedbackMapper.countPendingGroupByQuestion().stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("question_id")).intValue(),
                        m -> ((Number) m.get("cnt")).intValue()));
        for (FeedbackListVO item : list) {
            item.setPendingCount(pendingCounts.getOrDefault(item.getQuestionId(), 0));
        }
        return list;
    }

    public FeedbackDetailVO detail(Integer id) {
        FeedbackListVO item = feedbackMapper.selectFeedbackListItem(id);
        if (item == null) {
            throw new RuntimeException("反馈不存在");
        }
        QuestionFeedback feedback = feedbackMapper.selectById(id);
        Question question = questionMapper.selectById(feedback.getQuestionId());
        FeedbackDetailVO vo = new FeedbackDetailVO();
        vo.setFeedback(feedback);
        vo.setQuestion(question);
        vo.setStudentName(item.getStudentName());
        return vo;
    }

    @Transactional
    public int resolve(Integer id, QuestionRequest request) {
        QuestionFeedback feedback = findFeedback(id);
        Question question = questionMapper.selectById(feedback.getQuestionId());
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }
        BeanUtils.copyQuestion(request, question);
        questionMapper.updateById(question);
        return updateFeedbackStatus(id, "resolved", "modified", null);
    }

    @Transactional
    public int reject(Integer id, String reason) {
        findFeedback(id);
        return updateFeedbackStatus(id, "rejected", "rejected", reason);
    }

    private QuestionFeedback findFeedback(Integer id) {
        QuestionFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new RuntimeException("反馈不存在");
        }
        return feedback;
    }

    private int updateFeedbackStatus(Integer id, String status, String resolveType, String rejectReason) {
        QuestionFeedback feedback = feedbackMapper.selectById(id);
        feedback.setStatus(status);
        feedback.setResolveType(resolveType);
        feedback.setRejectReason(rejectReason);
        feedback.setUpdateTime(LocalDateTime.now());
        feedbackMapper.updateById(feedback);
        return feedbackMapper.resolveOtherByQuestionId(feedback.getQuestionId(), status, resolveType, id);
    }

    public List<Integer> myFeedbackQuestionIds(Integer studentId, List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        List<QuestionFeedback> feedbacks = feedbackMapper.selectByStudent(studentId);
        return feedbacks.stream()
                .filter(f -> questionIds.contains(f.getQuestionId()))
                .map(QuestionFeedback::getQuestionId)
                .collect(Collectors.toList());
    }

}

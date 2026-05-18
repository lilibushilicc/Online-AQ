package com.example.olineaqspring.service;

import com.example.olineaqspring.dto.FeedbackCreateRequest;
import com.example.olineaqspring.dto.QuestionRequest;
import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.QuestionFeedback;
import com.example.olineaqspring.mapper.QuestionFeedbackMapper;
import com.example.olineaqspring.mapper.QuestionMapper;
import com.example.olineaqspring.vo.FeedbackDetailVO;
import com.example.olineaqspring.vo.FeedbackListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeedbackService {
    private final QuestionFeedbackMapper feedbackMapper;
    private final QuestionMapper questionMapper;

    public FeedbackService(QuestionFeedbackMapper feedbackMapper, QuestionMapper questionMapper) {
        this.feedbackMapper = feedbackMapper;
        this.questionMapper = questionMapper;
    }

    public QuestionFeedback create(FeedbackCreateRequest request, Integer studentId) {
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

    public List<FeedbackListVO> list(String status) {
        List<FeedbackListVO> list;
        if (status != null && !status.isEmpty()) {
            list = feedbackMapper.selectFeedbackListByStatus(status);
        } else {
            list = feedbackMapper.selectAllFeedbackList();
        }
        Map<Integer, Integer> pendingCountCache = new HashMap<>();
        for (FeedbackListVO item : list) {
            if ("pending".equals(item.getStatus())) {
                Integer qid = item.getQuestionId();
                if (!pendingCountCache.containsKey(qid)) {
                    int count = feedbackMapper.countPendingByQuestionId(qid);
                    pendingCountCache.put(qid, count);
                }
            }
            item.setPendingCount(pendingCountCache.getOrDefault(item.getQuestionId(), 0));
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
        QuestionFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new RuntimeException("反馈不存在");
        }
        Question question = questionMapper.selectById(feedback.getQuestionId());
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }
        copy(request, question);
        questionMapper.updateById(question);

        feedback.setStatus("resolved");
        feedback.setResolveType("modified");
        feedback.setUpdateTime(LocalDateTime.now());
        feedbackMapper.updateById(feedback);

        int affected = feedbackMapper.resolveOtherByQuestionId(
                feedback.getQuestionId(), "resolved", "modified", id);
        return affected;
    }

    @Transactional
    public int reject(Integer id, String reason) {
        QuestionFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new RuntimeException("反馈不存在");
        }
        feedback.setStatus("rejected");
        feedback.setRejectReason(reason);
        feedback.setResolveType("rejected");
        feedback.setUpdateTime(LocalDateTime.now());
        feedbackMapper.updateById(feedback);

        int affected = feedbackMapper.resolveOtherByQuestionId(
                feedback.getQuestionId(), "rejected", "rejected", id);
        return affected;
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

    private void copy(QuestionRequest request, Question question) {
        if (request.getQuestionContent() != null) {
            question.setQuestionContent(request.getQuestionContent());
        }
        if (request.getQuestionType() != null) {
            question.setQuestionType(request.getQuestionType());
        }
        if (request.getOptionA() != null) question.setOptionA(request.getOptionA());
        if (request.getOptionB() != null) question.setOptionB(request.getOptionB());
        if (request.getOptionC() != null) question.setOptionC(request.getOptionC());
        if (request.getOptionD() != null) question.setOptionD(request.getOptionD());
        if (request.getCorrectAnswer() != null) {
            question.setCorrectAnswer(request.getCorrectAnswer());
        }
        if (request.getScore() != null) question.setScore(request.getScore());
        if (request.getCategory() != null) question.setCategory(request.getCategory());
    }
}

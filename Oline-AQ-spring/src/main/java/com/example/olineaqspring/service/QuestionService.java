package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.dto.QuestionRequest;
import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.mapper.QuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    private final QuestionMapper questionMapper;

    public QuestionService(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    public List<Question> list(String category) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>().orderByDesc(Question::getQuestionId);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Question::getCategory, category);
        }
        return questionMapper.selectList(wrapper);
    }

    public List<String> categories() {
        return questionMapper.selectList(new LambdaQueryWrapper<Question>()
                        .select(Question::getCategory)
                        .isNotNull(Question::getCategory)
                        .ne(Question::getCategory, "")
                        .groupBy(Question::getCategory))
                .stream()
                .map(Question::getCategory)
                .collect(Collectors.toList());
    }

    public Question create(QuestionRequest request) {
        Question question = new Question();
        copy(request, question);
        questionMapper.insert(question);
        return question;
    }

    public Question update(Integer questionId, QuestionRequest request) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }

        copy(request, question);
        questionMapper.updateById(question);
        return question;
    }

    public void delete(Integer questionId) {
        questionMapper.deleteById(questionId);
    }

    @Transactional
    public void deleteBatch(List<Integer> questionIds) {
        validateQuestionIds(questionIds);
        questionMapper.deleteByIds(questionIds);
    }

    @Transactional
    public void updateCategoryBatch(List<Integer> questionIds, String category) {
        validateQuestionIds(questionIds);
        for (Integer questionId : questionIds) {
            Question question = questionMapper.selectById(questionId);
            if (question == null) {
                continue;
            }
            question.setCategory(category);
            questionMapper.updateById(question);
        }
    }

    @Transactional
    public void updateScoreBatch(List<Integer> questionIds, BigDecimal score) {
        validateQuestionIds(questionIds);
        if (score == null || score.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("批量分值必须大于 0");
        }

        for (Integer questionId : questionIds) {
            Question question = questionMapper.selectById(questionId);
            if (question == null) {
                continue;
            }

            question.setScore(score);
            questionMapper.updateById(question);
        }
    }

    private void validateQuestionIds(List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new RuntimeException("请至少选择一道题目");
        }
    }

    private void copy(QuestionRequest request, Question question) {
        question.setQuestionContent(request.getQuestionContent());
        question.setQuestionType(request.getQuestionType() == null ? "single" : request.getQuestionType());
        question.setOptionA(request.getOptionA());
        question.setOptionB(request.getOptionB());
        question.setOptionC(request.getOptionC());
        question.setOptionD(request.getOptionD());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setScore(request.getScore() == null ? BigDecimal.valueOf(5) : request.getScore());
        question.setCategory(request.getCategory());
    }
}

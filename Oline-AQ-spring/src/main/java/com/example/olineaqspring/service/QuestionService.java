package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.dto.QuestionRequest;
import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.mapper.QuestionMapper;
import com.example.olineaqspring.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.olineaqspring.utils.BeanUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionMapper questionMapper;

    public PageResult<Question> list(String category, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Question> countWrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            countWrapper.eq(Question::getCategory, category);
        }
        long total = questionMapper.selectCount(countWrapper);

        LambdaQueryWrapper<Question> listWrapper = new LambdaQueryWrapper<Question>().orderByDesc(Question::getQuestionId);
        if (category != null && !category.isEmpty()) {
            listWrapper.eq(Question::getCategory, category);
        }
        int offset = (page - 1) * pageSize;
        listWrapper.last("LIMIT " + pageSize + " OFFSET " + offset);
        List<Question> list = questionMapper.selectList(listWrapper);

        return new PageResult<>(list, total, page, pageSize);
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
        question.setQuestionType("single");
        question.setScore(BigDecimal.valueOf(5));
        BeanUtils.copyQuestion(request, question);
        questionMapper.insert(question);
        return question;
    }

    public Question update(Integer questionId, QuestionRequest request) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }

        BeanUtils.copyQuestion(request, question);
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
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        for (Question question : questions) {
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

        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        for (Question question : questions) {
            question.setScore(score);
            questionMapper.updateById(question);
        }
    }

    private void validateQuestionIds(List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new RuntimeException("请至少选择一道题目");
        }
    }
}

package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    public List<Question> listAll(String category) {
        return questionMapper.selectList(buildOrderedWrapper(category));
    }

    public PageResult<Question> list(String category, Integer page, Integer pageSize) {
        IPage<Question> p = questionMapper.selectPage(new Page<>(page, pageSize), buildOrderedWrapper(category));
        return new PageResult<>(p.getRecords(), p.getTotal(), (int) p.getCurrent(), (int) p.getSize());
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
        questionMapper.update(
                new Question() {{ setCategory(category); }},
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Question>()
                        .in(Question::getQuestionId, questionIds));
    }

    @Transactional
    public void updateScoreBatch(List<Integer> questionIds, BigDecimal score) {
        validateQuestionIds(questionIds);
        if (score == null || score.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("批量分值必须大于 0");
        }
        questionMapper.update(
                new Question() {{ setScore(score); }},
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Question>()
                        .in(Question::getQuestionId, questionIds));
    }

    private void validateQuestionIds(List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new RuntimeException("请至少选择一道题目");
        }
    }

    private LambdaQueryWrapper<Question> buildOrderedWrapper(String category) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>().orderByDesc(Question::getQuestionId);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Question::getCategory, category);
        }
        return wrapper;
    }
}

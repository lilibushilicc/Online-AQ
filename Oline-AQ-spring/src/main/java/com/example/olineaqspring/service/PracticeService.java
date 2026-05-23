package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.dto.SubmitPracticeRequest;
import com.example.olineaqspring.entity.PracticeResult;
import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.StudentAnswer;
import com.example.olineaqspring.mapper.PracticeResultMapper;
import com.example.olineaqspring.mapper.QuestionMapper;
import com.example.olineaqspring.mapper.StudentAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeService {
    private final PracticeResultMapper practiceResultMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final QuestionMapper questionMapper;

    @Transactional
    public PracticeResult submit(SubmitPracticeRequest request, Integer studentId) {
        List<Integer> questionIds = request.getAnswers().stream()
                .map(SubmitPracticeRequest.PracticeAnswer::getQuestionId).toList();
        Map<Integer, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        int correctCount = 0;
        int total = request.getAnswers().size();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalScore = BigDecimal.ZERO;

        for (SubmitPracticeRequest.PracticeAnswer pa : request.getAnswers()) {
            Question q = questionMap.get(pa.getQuestionId());
            if (q == null) continue;

            boolean correct = pa.getAnswer() != null
                    && pa.getAnswer().equalsIgnoreCase(q.getCorrectAnswer());
            BigDecimal score = correct ? BigDecimal.ONE : BigDecimal.ZERO;
            if (correct) correctCount++;
            totalScore = totalScore.add(score);

            StudentAnswer sa = new StudentAnswer();
            sa.setExamId(0);
            sa.setStudentId(studentId);
            sa.setQuestionId(pa.getQuestionId());
            sa.setStudentAnswer(pa.getAnswer());
            sa.setCorrectAnswer(q.getCorrectAnswer());
            sa.setIsCorrect(correct);
            sa.setScore(score);
            sa.setSubmitTime(now);
            sa.setReviewStatus("auto_scored");
            studentAnswerMapper.insert(sa);
        }

        PracticeResult result = new PracticeResult();
        result.setStudentId(studentId);
        result.setTotalQuestions(total);
        result.setCorrectCount(correctCount);
        result.setWrongCount(total - correctCount);
        result.setTotalScore(totalScore);
        result.setSubmitTime(now);
        practiceResultMapper.insert(result);
        return result;
    }

    public List<PracticeResult> history(Integer studentId) {
        return practiceResultMapper.selectList(new LambdaQueryWrapper<PracticeResult>()
                .eq(PracticeResult::getStudentId, studentId)
                .orderByDesc(PracticeResult::getSubmitTime));
    }
}

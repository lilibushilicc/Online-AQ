package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.entity.ExamResult;
import com.example.olineaqspring.entity.StudentAnswer;
import com.example.olineaqspring.mapper.ExamResultMapper;
import com.example.olineaqspring.mapper.QuestionMapper;
import com.example.olineaqspring.mapper.StudentAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final ExamResultMapper examResultMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final QuestionMapper questionMapper;

    public Map<String, Object> stats() {
        List<ExamResult> allResults = examResultMapper.selectList(null);
        Map<String, Object> data = new HashMap<>();

        // Score distribution (by percentile ranges)
        int[] dist = new int[5];
        for (ExamResult r : allResults) {
            if (r.getTotalScore() == null) continue;
            double pct = r.getTotalScore().doubleValue();
            if (pct <= 20) dist[0]++;
            else if (pct <= 40) dist[1]++;
            else if (pct <= 60) dist[2]++;
            else if (pct <= 80) dist[3]++;
            else dist[4]++;
        }
        data.put("scoreDistribution", Map.of(
            "range0_20", dist[0], "range21_40", dist[1],
            "range41_60", dist[2], "range61_80", dist[3],
            "range81_100", dist[4]
        ));

        // Pass rate (score > 0 counts as passing — simplified)
        long passCount = allResults.stream().filter(r -> r.getTotalScore() != null && r.getTotalScore().compareTo(BigDecimal.ZERO) > 0).count();
        long total = allResults.size();
        data.put("passRate", total > 0 ? BigDecimal.valueOf(passCount).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP).doubleValue() : 0);

        // Hardest questions (top 5 with lowest correct rate)
        List<StudentAnswer> allAnswers = studentAnswerMapper.selectList(
                new LambdaQueryWrapper<StudentAnswer>().isNotNull(StudentAnswer::getIsCorrect));
        Map<Integer, List<Boolean>> byQuestion = allAnswers.stream()
                .collect(Collectors.groupingBy(StudentAnswer::getQuestionId,
                        Collectors.mapping(StudentAnswer::getIsCorrect, Collectors.toList())));
        List<Map<String, Object>> hardest = byQuestion.entrySet().stream()
                .map(e -> {
                    long correct = e.getValue().stream().filter(Boolean.TRUE::equals).count();
                    long totalQ = e.getValue().size();
                    double rate = totalQ > 0 ? (double) correct / totalQ : 1;
                    Map<String, Object> m = new HashMap<>();
                    m.put("questionId", e.getKey());
                    m.put("correctRate", BigDecimal.valueOf(rate * 100).setScale(1, RoundingMode.HALF_UP).doubleValue());
                    m.put("totalAnswers", totalQ);
                    return m;
                })
                .sorted(Comparator.comparingDouble(m -> (double) m.get("correctRate")))
                .limit(5)
                .peek(m -> {
                    var q = questionMapper.selectById((Integer) m.get("questionId"));
                    m.put("questionContent", q != null ? q.getQuestionContent() : "未知题目");
                })
                .toList();
        data.put("hardestQuestions", hardest);

        return data;
    }
}

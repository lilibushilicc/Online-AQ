package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.dto.SubmitExamRequest;
import com.example.olineaqspring.entity.Exam;
import com.example.olineaqspring.entity.ExamQuestion;
import com.example.olineaqspring.entity.ExamResult;
import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.StudentAnswer;
import com.example.olineaqspring.mapper.ExamMapper;
import com.example.olineaqspring.mapper.ExamResultMapper;
import com.example.olineaqspring.mapper.QuestionMapper;
import com.example.olineaqspring.mapper.StudentAnswerMapper;
import com.example.olineaqspring.utils.AnswerMapHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultService {
    private final ExamService examService;
    private final ExamMapper examMapper;
    private final QuestionMapper questionMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final ExamResultMapper examResultMapper;

    @Transactional
    public Map<String, Object> submit(Integer examId, SubmitExamRequest request, Integer loginUserId) {
        Integer studentId = request.getStudentId() == null ? loginUserId : request.getStudentId();
        Exam exam = examService.getExam(examId);
        validateSubmit(exam, studentId);

        Map<Integer, String> answerMap = buildAnswerMap(request);
        List<ExamQuestion> relations = examService.listExamQuestions(examId);
        List<Integer> qids = relations.stream().map(ExamQuestion::getQuestionId).distinct().toList();
        Map<Integer, Question> questionMap = questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));

        BigDecimal totalScore = BigDecimal.ZERO;
        int correctCount = 0;
        LocalDateTime submitTime = LocalDateTime.now();

        for (ExamQuestion relation : relations) {
            Question question = questionMap.get(relation.getQuestionId());
            if (question == null) {
                continue;
            }

            String value = answerMap.getOrDefault(question.getQuestionId(), "");
            boolean correct;
            BigDecimal score;
            if ("short_answer".equals(question.getQuestionType())) {
                correct = false;
                score = BigDecimal.ZERO;
            } else {
                correct = value.equalsIgnoreCase(question.getCorrectAnswer());
                score = correct ? relation.getScore() : BigDecimal.ZERO;
            }
            if (correct) {
                correctCount++;
            }
            totalScore = totalScore.add(score);

            studentAnswerMapper.insert(buildStudentAnswer(examId, studentId, question, value, correct, score, submitTime));
        }

        int wrongCount = relations.size() - correctCount;
        ExamResult result = new ExamResult();
        result.setExamId(examId);
        result.setStudentId(studentId);
        result.setTotalScore(totalScore);
        result.setCorrectCount(correctCount);
        result.setWrongCount(wrongCount);
        result.setUseTime(request.getUseTime());
        result.setSubmitTime(submitTime);
        examResultMapper.insert(result);

        examService.recordHistory(examId, studentId, "SUBMIT",
                String.format("学生 %d 提交考试，得分 %s", studentId, totalScore));

        Map<String, Object> data = new HashMap<>();
        data.put("resultId", result.getResultId());
        data.put("totalScore", totalScore);
        data.put("correctCount", correctCount);
        data.put("wrongCount", wrongCount);
        return data;
    }

    public List<ExamResult> myResults(Integer studentId) {
        return examResultMapper.selectList(new LambdaQueryWrapper<ExamResult>()
                .eq(ExamResult::getStudentId, studentId)
                .orderByDesc(ExamResult::getSubmitTime));
    }

    public List<ExamResult> examResults(Integer examId) {
        examService.getExam(examId);
        return examResultMapper.selectList(new LambdaQueryWrapper<ExamResult>()
                .eq(ExamResult::getExamId, examId)
                .orderByDesc(ExamResult::getSubmitTime));
    }

    public Map<String, Object> resultDetail(Integer resultId, Integer loginUserId, String role) {
        ExamResult result = examResultMapper.selectById(resultId);
        if (result == null) {
            throw new RuntimeException("成绩记录不存在");
        }
        if ("student".equals(role) && !result.getStudentId().equals(loginUserId)) {
            throw new RuntimeException("无权查看该成绩详情");
        }

        Exam exam = examMapper.selectById(result.getExamId());
        List<StudentAnswer> answers = studentAnswerMapper.selectList(new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getExamId, result.getExamId())
                .eq(StudentAnswer::getStudentId, result.getStudentId())
                .eq(StudentAnswer::getSubmitTime, result.getSubmitTime())
                .orderByAsc(StudentAnswer::getQuestionId));

        List<Integer> qids = answers.stream().map(StudentAnswer::getQuestionId).distinct().toList();
        Map<Integer, Question> questionMap = questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));

        List<Map<String, Object>> answerDetails = answers.stream().map(answer ->
                toAnswerMap(answer, questionMap, false)
        ).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("result", result);
        data.put("exam", exam);
        data.put("answers", answerDetails);
        if ("admin".equals(role)) {
            data.put("history", examService.history(result.getExamId()));
        }
        return data;
    }

    public List<Map<String, Object>> wrongQuestions(Integer studentId) {
        List<StudentAnswer> wrongAnswers = studentAnswerMapper.selectList(
                new LambdaQueryWrapper<StudentAnswer>()
                        .eq(StudentAnswer::getStudentId, studentId)
                        .eq(StudentAnswer::getIsCorrect, false)
                        .orderByDesc(StudentAnswer::getSubmitTime));

        if (wrongAnswers.isEmpty()) {
            return Collections.emptyList();
        }

        // Get distinct question IDs and exam IDs
        List<Integer> questionIds = wrongAnswers.stream()
                .map(StudentAnswer::getQuestionId)
                .distinct()
                .toList();
        List<Integer> examIds = wrongAnswers.stream()
                .map(StudentAnswer::getExamId)
                .distinct()
                .toList();

        Map<Integer, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity(), (left, right) -> left));
        Map<Integer, Exam> examMap = examMapper.selectBatchIds(examIds).stream()
                .collect(Collectors.toMap(Exam::getExamId, Function.identity(), (left, right) -> left));

        // Group by exam for structured output
        Map<Integer, List<StudentAnswer>> byExam = wrongAnswers.stream()
                .collect(Collectors.groupingBy(StudentAnswer::getExamId));

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map.Entry<Integer, List<StudentAnswer>> entry : byExam.entrySet()) {
            Integer eid = entry.getKey();
            Exam exam = examMap.get(eid);
            List<Map<String, Object>> questions = entry.getValue().stream().map(answer ->
                    toAnswerMap(answer, questionMap, true)
            ).toList();

            Map<String, Object> group = new HashMap<>();
            group.put("examId", eid);
            group.put("examName", exam == null ? "未知考试" : exam.getExamName());
            group.put("questions", questions);
            result.add(group);
        }

        return result;
    }

    private Map<String, Object> toAnswerMap(StudentAnswer answer, Map<Integer, Question> questionMap, boolean forWrongBook) {
        Question question = questionMap.get(answer.getQuestionId());
        return AnswerMapHelper.toAnswerMap(answer, question, forWrongBook);
    }

    private void validateSubmit(Exam exam, Integer studentId) {
        if (!"published".equals(exam.getStatus())) {
            throw new RuntimeException("当前考试未发布，不能提交");
        }

        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new RuntimeException("考试尚未开始");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new RuntimeException("考试已结束");
        }

        if (!Boolean.TRUE.equals(exam.getAllowRetake())) {
            Long submittedCount = examResultMapper.selectCount(new LambdaQueryWrapper<ExamResult>()
                    .eq(ExamResult::getExamId, exam.getExamId())
                    .eq(ExamResult::getStudentId, studentId));
            if (submittedCount != null && submittedCount > 0) {
                throw new RuntimeException("该考试已提交过，不允许重复提交");
            }
        }
    }

    private Map<Integer, String> buildAnswerMap(SubmitExamRequest request) {
        Map<Integer, String> answerMap = new HashMap<>();
        if (request.getAnswers() != null) {
            request.getAnswers().forEach(answer -> answerMap.put(answer.getQuestionId(), answer.getStudentAnswer()));
        }
        return answerMap;
    }

    private StudentAnswer buildStudentAnswer(Integer examId, Integer studentId, Question question, String value,
                                             boolean correct, BigDecimal score, LocalDateTime submitTime) {
        StudentAnswer studentAnswer = new StudentAnswer();
        studentAnswer.setExamId(examId);
        studentAnswer.setStudentId(studentId);
        studentAnswer.setQuestionId(question.getQuestionId());
        studentAnswer.setStudentAnswer(value);
        studentAnswer.setCorrectAnswer(question.getCorrectAnswer());
        studentAnswer.setIsCorrect(correct);
        studentAnswer.setScore(score);
        studentAnswer.setSubmitTime(submitTime);
        return studentAnswer;
    }
}

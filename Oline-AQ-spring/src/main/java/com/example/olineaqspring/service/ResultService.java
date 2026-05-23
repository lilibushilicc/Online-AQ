package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.dto.SubmitExamRequest;
import com.example.olineaqspring.entity.Exam;
import com.example.olineaqspring.entity.ExamQuestion;
import com.example.olineaqspring.entity.ExamResult;
import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.SysUser;
import com.example.olineaqspring.entity.StudentAnswer;
import com.example.olineaqspring.entity.DraftAnswer;
import com.example.olineaqspring.mapper.ExamMapper;
import com.example.olineaqspring.mapper.ExamResultMapper;
import com.example.olineaqspring.mapper.QuestionMapper;
import com.example.olineaqspring.mapper.StudentAnswerMapper;
import com.example.olineaqspring.mapper.UserMapper;
import com.example.olineaqspring.mapper.DraftAnswerMapper;
import com.example.olineaqspring.utils.AnswerMapHelper;
import com.example.olineaqspring.utils.LockUtils;
import com.example.olineaqspring.utils.RateLimiter;
import com.example.olineaqspring.vo.ResultAnswerVO;
import com.example.olineaqspring.vo.ResultDetailVO;
import com.example.olineaqspring.vo.ReviewItemVO;
import com.example.olineaqspring.vo.WrongQuestionGroupVO;
import com.example.olineaqspring.vo.WrongQuestionItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
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
    private final UserMapper userMapper;
    private final DraftAnswerMapper draftAnswerMapper;

    @Transactional
    public Map<String, Object> submit(Integer examId, SubmitExamRequest request, Integer loginUserId) {
        Integer studentId = loginUserId;
        examService.assertStudentCanAccessExam(examId, studentId);

        // 第一层：请求限流，防恶意高并发打崩
        if (!RateLimiter.tryAcquire("submit:" + examId + ":" + studentId, 5, 10)) {
            throw new RuntimeException("提交过于频繁，请稍后重试");
        }

        // 第二层：同步锁，防同一学生并发提交
        String lockKey = "submit_" + examId + "_" + studentId;
        boolean locked = LockUtils.tryLock(lockKey, 10000);
        if (!locked) {
            throw new RuntimeException("请求繁忙，请稍后重试");
        }
        try {
            // 进入同步块后二次检查，防止队列中第一个提交后第二个继续执行
            Exam exam = examService.getExam(examId);
            validateSubmit(exam, studentId);
            DraftAnswer draft = examService.getOrCreateDraft(examId, studentId, request.getAttemptId());
            String attemptId = draft.getAttemptId();
            Map<Integer, int[]> shuffleMap = examService.readShuffleMap(draft.getShuffleSnapshot());

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

                String value = normalizeStudentAnswer(question.getQuestionId(), answerMap.getOrDefault(question.getQuestionId(), ""), shuffleMap);
                boolean correct;
                BigDecimal score;
                boolean isShortAnswer = "short_answer".equals(question.getQuestionType());
                if (isShortAnswer) {
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

                studentAnswerMapper.insert(buildStudentAnswer(examId, studentId, attemptId, question, value, correct, score, submitTime, isShortAnswer));
            }

            int wrongCount = relations.size() - correctCount;
            ExamResult result = new ExamResult();
            result.setExamId(examId);
            result.setStudentId(studentId);
            result.setAttemptId(attemptId);
            result.setTotalScore(totalScore);
            result.setCorrectCount(correctCount);
            result.setWrongCount(wrongCount);
            result.setUseTime(request.getUseTime());
            result.setSubmitTime(submitTime);

            try {
                examResultMapper.insert(result);
            } catch (DuplicateKeyException e) {
                // 第三层兜底：唯一约束防重
                throw new RuntimeException("该考试已提交过，不允许重复提交");
            }

            examService.recordHistory(examId, studentId, "SUBMIT",
                    String.format("学生 %d 提交考试，得分 %s", studentId, totalScore));

            Map<String, Object> data = new HashMap<>();
            data.put("resultId", result.getResultId());
            data.put("totalScore", totalScore);
            data.put("correctCount", correctCount);
            data.put("wrongCount", wrongCount);
            draft.setAnswers("{}");
            draft.setUseTime(0);
            draft.setShuffleSnapshot(null);
            draft.setAttemptId(java.util.UUID.randomUUID().toString());
            draft.setUpdatedAt(LocalDateTime.now());
            draftAnswerMapper.updateById(draft);
            examService.recordHistory(examId, studentId, "ATTEMPT_RESET", "提交后重置下一次重考会话");
            return data;
        } finally {
            LockUtils.unlock(lockKey);
        }
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

    public ResultDetailVO resultDetail(Integer resultId, Integer loginUserId, String role) {
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
                .eq(result.getAttemptId() != null, StudentAnswer::getAttemptId, result.getAttemptId())
                .eq(result.getAttemptId() == null, StudentAnswer::getSubmitTime, result.getSubmitTime())
                .orderByAsc(StudentAnswer::getQuestionId));

        List<Integer> qids = answers.stream().map(StudentAnswer::getQuestionId).distinct().toList();
        Map<Integer, Question> questionMap = questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));

        List<ResultAnswerVO> answerDetails = answers.stream().map(answer ->
                toResultAnswer(answer, questionMap)
        ).toList();

        ResultDetailVO data = new ResultDetailVO();
        data.setResult(result);
        data.setExam(exam);
        data.setAnswers(answerDetails);
        if ("admin".equals(role)) {
            data.setHistory(examService.history(result.getExamId()));
        }
        return data;
    }

    public List<WrongQuestionGroupVO> wrongQuestions(Integer studentId) {
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

        List<WrongQuestionGroupVO> result = new java.util.ArrayList<>();
        for (Map.Entry<Integer, List<StudentAnswer>> entry : byExam.entrySet()) {
            Integer eid = entry.getKey();
            Exam exam = examMap.get(eid);
            List<WrongQuestionItemVO> questions = entry.getValue().stream().map(answer ->
                    toWrongQuestion(answer, questionMap)
            ).toList();

            WrongQuestionGroupVO group = new WrongQuestionGroupVO();
            group.setExamId(eid);
            group.setExamName(exam == null ? "未知考试" : exam.getExamName());
            group.setQuestions(questions);
            result.add(group);
        }

        return result;
    }

    public List<ReviewItemVO> pendingReviews() {
        List<StudentAnswer> pending = studentAnswerMapper.selectList(
                new LambdaQueryWrapper<StudentAnswer>()
                        .eq(StudentAnswer::getReviewStatus, "pending_review")
                        .orderByAsc(StudentAnswer::getSubmitTime));
        if (pending.isEmpty()) return Collections.emptyList();

        List<Integer> qids = pending.stream().map(StudentAnswer::getQuestionId).distinct().toList();
        List<Integer> eids = pending.stream().map(StudentAnswer::getExamId).distinct().toList();
        List<Integer> sids = pending.stream().map(StudentAnswer::getStudentId).distinct().toList();

        Map<Integer, Question> qMap = questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity(), (a, b) -> a));
        Map<Integer, Exam> eMap = examMapper.selectBatchIds(eids).stream()
                .collect(Collectors.toMap(Exam::getExamId, Function.identity(), (a, b) -> a));
        Map<Integer, SysUser> uMap = userMapper.selectBatchIds(sids).stream()
                .collect(Collectors.toMap(SysUser::getUserId, Function.identity(), (a, b) -> a));

        return pending.stream().map(a -> {
            ReviewItemVO m = new ReviewItemVO();
            m.setAnswerId(a.getAnswerId());
            m.setQuestionId(a.getQuestionId());
            m.setQuestionContent(qMap.get(a.getQuestionId()) != null ? qMap.get(a.getQuestionId()).getQuestionContent() : "");
            m.setQuestionType(qMap.get(a.getQuestionId()) != null ? qMap.get(a.getQuestionId()).getQuestionType() : "single");
            m.setStudentAnswer(a.getStudentAnswer());
            m.setCorrectAnswer(a.getCorrectAnswer());
            m.setScore(a.getScore());
            m.setExamName(eMap.get(a.getExamId()) != null ? eMap.get(a.getExamId()).getExamName() : "未知考试");
            m.setStudentName(uMap.get(a.getStudentId()) != null ? uMap.get(a.getStudentId()).getRealName() : "未知学生");
            m.setSubmitTime(a.getSubmitTime());
            return m;
        }).toList();
    }

    @Transactional
    public void reviewAnswer(Integer answerId, BigDecimal score) {
        StudentAnswer answer = studentAnswerMapper.selectById(answerId);
        if (answer == null) throw new RuntimeException("答题记录不存在");
        if (!"pending_review".equals(answer.getReviewStatus())) throw new RuntimeException("该记录无需评分");
        Question question = questionMapper.selectById(answer.getQuestionId());
        BigDecimal maxScore = resolveQuestionScore(answer.getExamId(), answer.getQuestionId(), question);
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(maxScore) > 0) {
            throw new RuntimeException("评分必须在 0 到题目满分之间");
        }

        answer.setScore(score);
        answer.setIsCorrect(score.compareTo(BigDecimal.ZERO) > 0);
        answer.setReviewStatus("reviewed");
        studentAnswerMapper.updateById(answer);

        recalcExamResult(answer.getExamId(), answer.getStudentId(), answer.getAttemptId(), answer.getSubmitTime());
    }

    private void recalcExamResult(Integer examId, Integer studentId, String attemptId, LocalDateTime submitTime) {
        List<StudentAnswer> all = studentAnswerMapper.selectList(
                new LambdaQueryWrapper<StudentAnswer>()
                        .eq(StudentAnswer::getExamId, examId)
                        .eq(StudentAnswer::getStudentId, studentId)
                        .eq(attemptId != null, StudentAnswer::getAttemptId, attemptId)
                        .eq(attemptId == null, StudentAnswer::getSubmitTime, submitTime));
        BigDecimal total = BigDecimal.ZERO;
        int correct = 0;
        for (StudentAnswer a : all) {
            if (a.getScore() != null) total = total.add(a.getScore());
            if (Boolean.TRUE.equals(a.getIsCorrect())) correct++;
        }
        int wrong = all.size() - correct;
        ExamResult r = examResultMapper.selectOne(new LambdaQueryWrapper<ExamResult>()
                .eq(ExamResult::getExamId, examId)
                .eq(ExamResult::getStudentId, studentId)
                .eq(attemptId != null, ExamResult::getAttemptId, attemptId)
                .eq(attemptId == null, ExamResult::getSubmitTime, submitTime));
        if (r != null) {
            r.setTotalScore(total);
            r.setCorrectCount(correct);
            r.setWrongCount(wrong);
            examResultMapper.updateById(r);
        }
    }

    private ResultAnswerVO toResultAnswer(StudentAnswer answer, Map<Integer, Question> questionMap) {
        Question question = questionMap.get(answer.getQuestionId());
        return AnswerMapHelper.toResultAnswerVO(answer, question, true);
    }

    private WrongQuestionItemVO toWrongQuestion(StudentAnswer answer, Map<Integer, Question> questionMap) {
        Question question = questionMap.get(answer.getQuestionId());
        return AnswerMapHelper.toWrongQuestionItemVO(answer, question, true);
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

    private String normalizeStudentAnswer(Integer questionId, String value, Map<Integer, int[]> shuffleMap) {
        if (value == null || value.length() != 1) {
            return value == null ? "" : value;
        }
        int[] perm = shuffleMap.get(questionId);
        if (perm == null) {
            return value;
        }
        int idx = Character.toUpperCase(value.charAt(0)) - 'A';
        if (idx < 0 || idx >= perm.length) {
            return value;
        }
        return String.valueOf((char) ('A' + perm[idx]));
    }

    private BigDecimal resolveQuestionScore(Integer examId, Integer questionId, Question question) {
        List<ExamQuestion> relations = examService.listExamQuestions(examId);
        for (ExamQuestion relation : relations) {
            if (relation.getQuestionId().equals(questionId) && relation.getScore() != null) {
                return relation.getScore();
            }
        }
        return question != null && question.getScore() != null ? question.getScore() : BigDecimal.ZERO;
    }

    private StudentAnswer buildStudentAnswer(Integer examId, Integer studentId, String attemptId, Question question, String value,
                                              boolean correct, BigDecimal score, LocalDateTime submitTime,
                                              boolean isShortAnswer) {
        StudentAnswer studentAnswer = new StudentAnswer();
        studentAnswer.setExamId(examId);
        studentAnswer.setStudentId(studentId);
        studentAnswer.setAttemptId(attemptId);
        studentAnswer.setQuestionId(question.getQuestionId());
        studentAnswer.setStudentAnswer(value);
        studentAnswer.setCorrectAnswer(question.getCorrectAnswer());
        studentAnswer.setIsCorrect(correct);
        studentAnswer.setScore(score);
        studentAnswer.setSubmitTime(submitTime);
        studentAnswer.setReviewStatus(isShortAnswer ? "pending_review" : "auto_scored");
        return studentAnswer;
    }
}

package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.dto.ExamCreateRequest;
import com.example.olineaqspring.dto.ExamSettingsRequest;
import com.example.olineaqspring.dto.PublishExamRequest;
import com.example.olineaqspring.entity.Exam;
import com.example.olineaqspring.entity.ExamHistory;
import com.example.olineaqspring.entity.ExamQuestion;
import com.example.olineaqspring.entity.ExamResult;
import com.example.olineaqspring.entity.ExamStudent;
import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.StudentAnswer;
import com.example.olineaqspring.entity.SysUser;
import com.example.olineaqspring.entity.DraftAnswer;
import com.example.olineaqspring.mapper.ExamHistoryMapper;
import com.example.olineaqspring.mapper.ExamMapper;
import com.example.olineaqspring.mapper.ExamQuestionMapper;
import com.example.olineaqspring.mapper.ExamResultMapper;
import com.example.olineaqspring.mapper.ExamStudentMapper;
import com.example.olineaqspring.mapper.QuestionMapper;
import com.example.olineaqspring.mapper.StudentAnswerMapper;
import com.example.olineaqspring.mapper.UserMapper;
import com.example.olineaqspring.mapper.DraftAnswerMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {
    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionMapper questionMapper;
    private final ExamHistoryMapper examHistoryMapper;
    private final UserMapper userMapper;
    private final ExamStudentMapper examStudentMapper;
    private final ExamResultMapper examResultMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final NotificationService notificationService;
    private final DraftAnswerMapper draftAnswerMapper;
    private final ObjectMapper objectMapper;

    public List<Exam> list() {
        return examMapper.selectList(new LambdaQueryWrapper<Exam>().orderByDesc(Exam::getCreateTime));
    }

    public List<Exam> listForStudent(Integer studentId) {
        List<Exam> all = list();
        all.removeIf(exam -> {
            if (Boolean.TRUE.equals(exam.getAssignAll())) return false;
            long count = examStudentMapper.selectCount(
                    new LambdaQueryWrapper<ExamStudent>()
                            .eq(ExamStudent::getExamId, exam.getExamId())
                            .eq(ExamStudent::getStudentId, studentId));
            return count == 0;
        });
        return all;
    }

    public Map<String, Object> detail(Integer examId) {
        return detail(examId, null);
    }

    public Map<String, Object> detail(Integer examId, String role) {
        return detail(examId, null, null, role);
    }

    public void assertStudentCanAccessExam(Integer examId, Integer studentId) {
        Exam exam = getExam(examId);
        if (!"published".equals(exam.getStatus())) {
            throw new RuntimeException("当前考试不可访问");
        }
        if (Boolean.TRUE.equals(exam.getAssignAll())) {
            return;
        }
        long count = examStudentMapper.selectCount(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, examId)
                .eq(ExamStudent::getStudentId, studentId));
        if (count == 0) {
            throw new RuntimeException("你未被分配到该考试");
        }
    }

    public Map<String, Object> detail(Integer examId, Integer studentId, String attemptId, String role) {
        Exam exam = getExam(examId);
        List<ExamQuestion> relations = listExamQuestions(examId);
        Map<Integer, Question> questionMap = getQuestionMap(relations.stream()
                .map(ExamQuestion::getQuestionId)
                .toList());
        Map<Integer, BigDecimal> relationScoreMap = relations.stream()
                .collect(Collectors.toMap(ExamQuestion::getQuestionId,
                        ExamQuestion::getScore,
                        (left, right) -> left));
        List<Question> questions = relations.stream()
                .map(relation -> copyQuestionWithRelationScore(questionMap.get(relation.getQuestionId()), relation.getScore()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        boolean isStudent = "student".equals(role);
        String resolvedAttemptId = attemptId;
        Map<Integer, int[]> shuffleMap = new HashMap<>();
        if (isStudent && studentId != null) {
            DraftAnswer draft = getOrCreateDraft(examId, studentId, attemptId);
            resolvedAttemptId = draft.getAttemptId();
            StableShuffleSnapshot snapshot = readShuffleSnapshot(draft.getShuffleSnapshot());
            if (snapshot == null) {
                snapshot = buildStableShuffleSnapshot(questions, exam);
                draft.setShuffleSnapshot(writeShuffleSnapshot(snapshot));
                draftAnswerMapper.updateById(draft);
            }
            questions = applyStableShuffleSnapshot(questions, snapshot, shuffleMap, true);
        } else if (Boolean.TRUE.equals(exam.getShuffleQuestions())) {
            Collections.shuffle(questions);
            if (Boolean.TRUE.equals(exam.getShuffleAnswers())) {
                questions = questions.stream()
                        .map(q -> shuffleQuestionOptions(q, shuffleMap, true))
                        .collect(Collectors.toList());
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("exam", exam);
        data.put("questions", questions);
        data.put("relationScores", relationScoreMap);
        data.put("serverTime", System.currentTimeMillis());
        if (resolvedAttemptId != null) {
            data.put("attemptId", resolvedAttemptId);
        }
        if (!shuffleMap.isEmpty()) {
            data.put("shuffleMap", shuffleMap);
        }
        return data;
    }

    private Question shuffleQuestionOptions(Question original, Map<Integer, int[]> shuffleMap, boolean hideCorrectAnswer) {
        Question q = new Question();
        org.springframework.beans.BeanUtils.copyProperties(original, q, hideCorrectAnswer ? new String[]{"correctAnswer"} : new String[]{});

        String[] options = {original.getOptionA(), original.getOptionB(), original.getOptionC(), original.getOptionD()};
        int[] indices = {0, 1, 2, 3};
        List<Integer> list = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(list);
        for (int i = 0; i < 4; i++) indices[i] = list.get(i);

        q.setOptionA(options[indices[0]]);
        q.setOptionB(options[indices[1]]);
        q.setOptionC(options[indices[2]]);
        q.setOptionD(options[indices[3]]);

        // shuffleMap: newPosition -> originalIdx (student picks "A" at newPos 0, original was indices[0])
        shuffleMap.put(original.getQuestionId(), indices);

        String correct = original.getCorrectAnswer();
        if (correct != null && correct.length() == 1) {
            int oldIdx = correct.charAt(0) - 'A';
            for (int i = 0; i < indices.length; i++) {
                if (indices[i] == oldIdx) {
                    if (!hideCorrectAnswer) {
                        q.setCorrectAnswer(String.valueOf((char) ('A' + i)));
                    }
                    break;
                }
            }
        } else if (!hideCorrectAnswer) {
            q.setCorrectAnswer(correct);
        }
        return q;
    }

    public DraftAnswer getOrCreateDraft(Integer examId, Integer studentId, String attemptId) {
        DraftAnswer draft = draftAnswerMapper.selectOne(new LambdaQueryWrapper<DraftAnswer>()
                .eq(DraftAnswer::getExamId, examId)
                .eq(DraftAnswer::getStudentId, studentId));
        if (draft == null) {
            draft = new DraftAnswer();
            draft.setExamId(examId);
            draft.setStudentId(studentId);
            draft.setAttemptId(attemptId != null && !attemptId.isBlank() ? attemptId : UUID.randomUUID().toString());
            draft.setUpdatedAt(LocalDateTime.now());
            draftAnswerMapper.insert(draft);
            return draft;
        }
        if (draft.getAttemptId() == null || draft.getAttemptId().isBlank()) {
            draft.setAttemptId(attemptId != null && !attemptId.isBlank() ? attemptId : UUID.randomUUID().toString());
            draft.setUpdatedAt(LocalDateTime.now());
            draftAnswerMapper.updateById(draft);
        }
        if (draft.getAttemptId() == null || draft.getAttemptId().isBlank()) {
            draft.setAttemptId(UUID.randomUUID().toString());
        }
        return draft;
    }

    public Question copyQuestionWithRelationScore(Question original, BigDecimal relationScore) {
        if (original == null) return null;
        Question q = new Question();
        org.springframework.beans.BeanUtils.copyProperties(original, q);
        if (relationScore != null) {
            q.setScore(relationScore);
        }
        return q;
    }

    private StableShuffleSnapshot buildStableShuffleSnapshot(List<Question> questions, Exam exam) {
        StableShuffleSnapshot snapshot = new StableShuffleSnapshot();
        List<Integer> order = questions.stream().map(Question::getQuestionId).collect(Collectors.toCollection(ArrayList::new));
        if (Boolean.TRUE.equals(exam.getShuffleQuestions())) {
            Collections.shuffle(order);
        }
        snapshot.setQuestionOrder(order);
        if (Boolean.TRUE.equals(exam.getShuffleAnswers())) {
            Map<Integer, List<Integer>> optionOrder = new HashMap<>();
            for (Integer questionId : order) {
                List<Integer> list = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
                Collections.shuffle(list);
                optionOrder.put(questionId, list);
            }
            snapshot.setOptionOrder(optionOrder);
        }
        return snapshot;
    }

    private List<Question> applyStableShuffleSnapshot(List<Question> questions, StableShuffleSnapshot snapshot,
                                                      Map<Integer, int[]> shuffleMap, boolean hideCorrectAnswer) {
        Map<Integer, Question> byId = questions.stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity(), (left, right) -> left));
        List<Question> ordered = new ArrayList<>();
        List<Integer> questionOrder = snapshot.getQuestionOrder() == null || snapshot.getQuestionOrder().isEmpty()
                ? questions.stream().map(Question::getQuestionId).toList()
                : snapshot.getQuestionOrder();
        for (Integer questionId : questionOrder) {
            Question original = byId.get(questionId);
            if (original == null) {
                continue;
            }
            List<Integer> optionOrder = snapshot.getOptionOrder() != null ? snapshot.getOptionOrder().get(questionId) : null;
            if (optionOrder == null || optionOrder.isEmpty()) {
                Question copy = new Question();
                org.springframework.beans.BeanUtils.copyProperties(original, copy, hideCorrectAnswer ? new String[]{"correctAnswer"} : new String[]{});
                ordered.add(copy);
                continue;
            }
            int[] indices = optionOrder.stream().mapToInt(Integer::intValue).toArray();
            Question shuffled = shuffleQuestionOptionsWithIndices(original, indices, shuffleMap, hideCorrectAnswer);
            ordered.add(shuffled);
        }
        return ordered;
    }

    private Question shuffleQuestionOptionsWithIndices(Question original, int[] indices, Map<Integer, int[]> shuffleMap,
                                                       boolean hideCorrectAnswer) {
        Question q = new Question();
        org.springframework.beans.BeanUtils.copyProperties(original, q, hideCorrectAnswer ? new String[]{"correctAnswer"} : new String[]{});
        String[] options = {original.getOptionA(), original.getOptionB(), original.getOptionC(), original.getOptionD()};
        q.setOptionA(options[indices[0]]);
        q.setOptionB(options[indices[1]]);
        q.setOptionC(options[indices[2]]);
        q.setOptionD(options[indices[3]]);
        shuffleMap.put(original.getQuestionId(), indices);
        if (!hideCorrectAnswer) {
            String correct = original.getCorrectAnswer();
            if (correct != null && correct.length() == 1) {
                int oldIdx = correct.charAt(0) - 'A';
                for (int i = 0; i < indices.length; i++) {
                    if (indices[i] == oldIdx) {
                        q.setCorrectAnswer(String.valueOf((char) ('A' + i)));
                        break;
                    }
                }
            } else {
                q.setCorrectAnswer(correct);
            }
        }
        return q;
    }

    private StableShuffleSnapshot readShuffleSnapshot(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, StableShuffleSnapshot.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String writeShuffleSnapshot(StableShuffleSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("乱序快照保存失败", e);
        }
    }

    public Map<Integer, int[]> readShuffleMap(String raw) {
        StableShuffleSnapshot snapshot = readShuffleSnapshot(raw);
        Map<Integer, int[]> shuffleMap = new HashMap<>();
        if (snapshot == null || snapshot.getOptionOrder() == null) {
            return shuffleMap;
        }
        snapshot.getOptionOrder().forEach((questionId, order) -> shuffleMap.put(questionId, order.stream().mapToInt(Integer::intValue).toArray()));
        return shuffleMap;
    }

    @lombok.Data
    public static class StableShuffleSnapshot {
        private List<Integer> questionOrder;
        private Map<Integer, List<Integer>> optionOrder;
    }

    public List<ExamHistory> history(Integer examId) {
        getExam(examId);
        List<ExamHistory> historyList = examHistoryMapper.selectList(new LambdaQueryWrapper<ExamHistory>()
                .eq(ExamHistory::getExamId, examId)
                .orderByDesc(ExamHistory::getCreateTime));
        fillOperatorNames(historyList);
        return historyList;
    }

    @Transactional
    @CacheEvict(value = "exam", allEntries = true)
    public Exam create(ExamCreateRequest request, Integer operatorId) {
        List<Integer> questionIds = request.getQuestionIds();
        if (questionIds == null || questionIds.isEmpty()) {
            throw new RuntimeException("请至少选择一道题目");
        }

        validateTimeRange(request.getStartTime(), request.getEndTime());
        Map<Integer, Question> questionMap = getQuestionMap(questionIds);

        // Build custom score map if provided
        Map<Integer, BigDecimal> customScoreMap = new HashMap<>();
        if (request.getQuestionScores() != null) {
            for (ExamCreateRequest.QuestionScoreItem item : request.getQuestionScores()) {
                if (item.getScore() != null) {
                    customScoreMap.put(item.getQuestionId(), item.getScore());
                }
            }
        }

        Exam exam = new Exam();
        exam.setExamName(request.getExamName());
        exam.setDescription(request.getDescription());
        exam.setDuration(request.getDuration());
        exam.setStatus("draft");
        exam.setAllowRetake(Boolean.TRUE.equals(request.getAllowRetake()));
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setCreateTime(LocalDateTime.now());
        exam.setTotalScore(calculateTotalScore(questionIds, questionMap, customScoreMap));
        examMapper.insert(exam);

        int index = 1;
        for (Integer questionId : questionIds) {
            Question question = questionMap.get(questionId);
            if (question == null) {
                continue;
            }

            ExamQuestion relation = new ExamQuestion();
            relation.setExamId(exam.getExamId());
            relation.setQuestionId(questionId);
            relation.setSortOrder(index++);
            BigDecimal qScore = customScoreMap.getOrDefault(questionId, question.getScore());
            relation.setScore(qScore != null ? qScore : BigDecimal.ZERO);
            examQuestionMapper.insert(relation);
        }

        recordHistory(
                exam.getExamId(),
                operatorId,
                "CREATE",
                String.format("创建考试：%s，题目数 %d，允许重考：%s", exam.getExamName(), questionIds.size(), exam.getAllowRetake())
        );
        return exam;
    }

    @Transactional
    @CacheEvict(value = "exam", allEntries = true)
    public Exam update(Integer examId, ExamCreateRequest request, Integer operatorId) {
        Exam exam = getExam(examId);
        if (!"draft".equals(exam.getStatus())) {
            throw new RuntimeException("仅草稿状态的考试可以编辑");
        }

        List<Integer> questionIds = request.getQuestionIds();
        if (questionIds == null || questionIds.isEmpty()) {
            throw new RuntimeException("请至少选择一道题目");
        }

        validateTimeRange(request.getStartTime(), request.getEndTime());
        Map<Integer, Question> questionMap = getQuestionMap(questionIds);

        Map<Integer, BigDecimal> customScoreMap = new HashMap<>();
        if (request.getQuestionScores() != null) {
            for (ExamCreateRequest.QuestionScoreItem item : request.getQuestionScores()) {
                if (item.getScore() != null) {
                    customScoreMap.put(item.getQuestionId(), item.getScore());
                }
            }
        }

        exam.setExamName(request.getExamName());
        exam.setDescription(request.getDescription());
        exam.setDuration(request.getDuration());
        exam.setAllowRetake(Boolean.TRUE.equals(request.getAllowRetake()));
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setTotalScore(calculateTotalScore(questionIds, questionMap, customScoreMap));
        examMapper.updateById(exam);

        // 清除旧关联，重新插入
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, examId));
        int index = 1;
        for (Integer questionId : questionIds) {
            Question question = questionMap.get(questionId);
            if (question == null) continue;
            ExamQuestion relation = new ExamQuestion();
            relation.setExamId(examId);
            relation.setQuestionId(questionId);
            relation.setSortOrder(index++);
            BigDecimal qScore = customScoreMap.getOrDefault(questionId, question.getScore());
            relation.setScore(qScore != null ? qScore : BigDecimal.ZERO);
            examQuestionMapper.insert(relation);
        }

        recordHistory(examId, operatorId, "UPDATE",
                String.format("编辑考试：%s，题目数 %d", exam.getExamName(), questionIds.size()));
        return exam;
    }

    @Transactional
    @CacheEvict(value = "exam", allEntries = true)
    public Exam updateSettings(Integer examId, ExamSettingsRequest request, Integer operatorId) {
        Exam exam = getExam(examId);
        if ("draft".equals(exam.getStatus())) {
            throw new RuntimeException("草稿状态的考试请前往试卷管理编辑");
        }

        if (request.getEndTime() != null) {
            if (exam.getStartTime() != null && request.getEndTime().isBefore(exam.getStartTime())) {
                throw new RuntimeException("结束时间不能早于开始时间");
            }
            exam.setEndTime(request.getEndTime());
        }
        if (request.getAllowRetake() != null) {
            exam.setAllowRetake(request.getAllowRetake());
        }

        examMapper.updateById(exam);
        recordHistory(examId, operatorId, "UPDATE_SETTINGS",
                String.format("修改考试设置：%s，结束时间：%s，允许重考：%s",
                        exam.getExamName(),
                        request.getEndTime() != null ? request.getEndTime().toString() : "不变",
                        request.getAllowRetake() != null ? (request.getAllowRetake() ? "是" : "否") : "不变"));
        return exam;
    }

    public void publish(Integer examId, Integer operatorId) {
        publish(examId, operatorId, null);
    }

    @Transactional
    @CacheEvict(value = "exam", allEntries = true)
    public void publish(Integer examId, Integer operatorId, PublishExamRequest request) {
        Exam exam = getExam(examId);

        // 发布时验证时间有效性
        if (exam.getDuration() == null || exam.getDuration() <= 0) {
            throw new RuntimeException("考试时长未设置，请先设置有效的考试时长");
        }
        if (exam.getStartTime() != null) {
            validateTimeRange(exam.getStartTime(), exam.getEndTime());
        }

        exam.setStatus("published");
        if (request != null) {
            exam.setShuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()));
            exam.setShuffleAnswers(Boolean.TRUE.equals(request.getShuffleAnswers()));
        }

        if (request != null) {
            exam.setAssignAll(Boolean.TRUE.equals(request.getAssignAll()));
            // Clear existing assignments then insert new ones
            examStudentMapper.delete(new LambdaQueryWrapper<ExamStudent>().eq(ExamStudent::getExamId, examId));
            if (!Boolean.TRUE.equals(request.getAssignAll()) && request.getStudentIds() != null) {
                for (Integer sid : request.getStudentIds()) {
                    ExamStudent es = new ExamStudent();
                    es.setExamId(examId);
                    es.setStudentId(sid);
                    examStudentMapper.insert(es);
                }
            }
        } else {
            exam.setAssignAll(true);
        }

        examMapper.updateById(exam);
        recordHistory(examId, operatorId, "PUBLISHED",
                "发布考试：" + exam.getExamName() +
                (request != null && Boolean.TRUE.equals(request.getAssignAll()) ? "（全体学生）" :
                 request != null ? "（指定 " + (request.getStudentIds() != null ? request.getStudentIds().size() : 0) + " 名学生）" : ""));
        notificationService.notifyExamPublished(examId, exam.getExamName());
    }

    @CacheEvict(value = "exam", key = "#examId")
    public void close(Integer examId, Integer operatorId) {
        updateExamStatus(examId, operatorId, "closed", "关闭考试");
    }

    @Transactional
    @CacheEvict(value = "exam", allEntries = true)
    public void delete(Integer examId) {
        getExam(examId);
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, examId));
        examStudentMapper.delete(new LambdaQueryWrapper<ExamStudent>().eq(ExamStudent::getExamId, examId));
        examHistoryMapper.delete(new LambdaQueryWrapper<ExamHistory>().eq(ExamHistory::getExamId, examId));
        studentAnswerMapper.delete(new LambdaQueryWrapper<StudentAnswer>().eq(StudentAnswer::getExamId, examId));
        examResultMapper.delete(new LambdaQueryWrapper<ExamResult>().eq(ExamResult::getExamId, examId));
        examMapper.deleteById(examId);
    }

    @Cacheable(value = "exam", key = "#examId")
    public Exam getExam(Integer examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        return exam;
    }

    @Cacheable(value = "exam", key = "'questions:' + #examId")
    public List<ExamQuestion> listExamQuestions(Integer examId) {
        return examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .orderByAsc(ExamQuestion::getSortOrder));
    }

    public void recordHistory(Integer examId, Integer operatorId, String actionType, String actionDetail) {
        ExamHistory history = new ExamHistory();
        history.setExamId(examId);
        history.setOperatorId(operatorId);
        history.setActionType(actionType);
        history.setActionDetail(actionDetail);
        history.setCreateTime(LocalDateTime.now());
        examHistoryMapper.insert(history);
    }

    private void updateExamStatus(Integer examId, Integer operatorId, String status, String actionLabel) {
        Exam exam = getExam(examId);
        exam.setStatus(status);
        examMapper.updateById(exam);
        recordHistory(examId, operatorId, status.toUpperCase(), actionLabel + "：" + exam.getExamName());
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new RuntimeException("结束时间必须晚于开始时间");
        }
        if (startTime != null && endTime == null && startTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("开始时间不能是过去时间");
        }
    }

    private void fillOperatorNames(List<ExamHistory> historyList) {
        List<Integer> operatorIds = historyList.stream()
                .map(ExamHistory::getOperatorId)
                .filter(operatorId -> operatorId != null)
                .distinct()
                .toList();
        if (operatorIds.isEmpty()) {
            return;
        }

        Map<Integer, String> userNameMap = operatorIds.stream()
                .map(userMapper::selectById)
                .filter(user -> user != null)
                .collect(Collectors.toMap(SysUser::getUserId, SysUser::getRealName, (left, right) -> left));

        historyList.forEach(history -> history.setOperatorName(userNameMap.getOrDefault(history.getOperatorId(), "未知用户")));
    }

    private BigDecimal calculateTotalScore(List<Integer> questionIds, Map<Integer, Question> questionMap,
                                           Map<Integer, BigDecimal> customScoreMap) {
        BigDecimal totalScore = BigDecimal.ZERO;
        for (Integer questionId : questionIds) {
            Question question = questionMap.get(questionId);
            if (question != null) {
                BigDecimal score = customScoreMap.getOrDefault(questionId, question.getScore());
                totalScore = totalScore.add(score != null ? score : BigDecimal.ZERO);
            }
        }
        return totalScore;
    }

    private Map<Integer, Question> getQuestionMap(List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity(), (left, right) -> left));
    }
}

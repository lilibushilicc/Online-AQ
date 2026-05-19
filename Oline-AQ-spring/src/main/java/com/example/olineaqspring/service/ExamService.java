package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.dto.ExamCreateRequest;
import com.example.olineaqspring.dto.PublishExamRequest;
import com.example.olineaqspring.entity.Exam;
import com.example.olineaqspring.entity.ExamHistory;
import com.example.olineaqspring.entity.ExamQuestion;
import com.example.olineaqspring.entity.ExamResult;
import com.example.olineaqspring.entity.ExamStudent;
import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.StudentAnswer;
import com.example.olineaqspring.entity.SysUser;
import com.example.olineaqspring.mapper.ExamHistoryMapper;
import com.example.olineaqspring.mapper.ExamMapper;
import com.example.olineaqspring.mapper.ExamQuestionMapper;
import com.example.olineaqspring.mapper.ExamResultMapper;
import com.example.olineaqspring.mapper.ExamStudentMapper;
import com.example.olineaqspring.mapper.QuestionMapper;
import com.example.olineaqspring.mapper.StudentAnswerMapper;
import com.example.olineaqspring.mapper.UserMapper;
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
public class ExamService {
    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionMapper questionMapper;
    private final ExamHistoryMapper examHistoryMapper;
    private final UserMapper userMapper;
    private final ExamStudentMapper examStudentMapper;
    private final ExamResultMapper examResultMapper;
    private final StudentAnswerMapper studentAnswerMapper;

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
        Exam exam = getExam(examId);
        List<ExamQuestion> relations = listExamQuestions(examId);
        Map<Integer, Question> questionMap = getQuestionMap(relations.stream()
                .map(ExamQuestion::getQuestionId)
                .toList());
        // Map relation scores: questionId -> score in this exam
        Map<Integer, BigDecimal> relationScoreMap = relations.stream()
                .collect(Collectors.toMap(ExamQuestion::getQuestionId,
                        ExamQuestion::getScore,
                        (left, right) -> left));
        List<Question> questions = relations.stream()
                .map(relation -> questionMap.get(relation.getQuestionId()))
                .filter(question -> question != null)
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("exam", exam);
        data.put("questions", questions);
        data.put("relationScores", relationScoreMap);
        return data;
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
            relation.setScore(customScoreMap.getOrDefault(questionId, question.getScore()));
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

    public void publish(Integer examId, Integer operatorId) {
        publish(examId, operatorId, null);
    }

    @Transactional
    public void publish(Integer examId, Integer operatorId, PublishExamRequest request) {
        Exam exam = getExam(examId);
        exam.setStatus("published");

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
    }

    public void close(Integer examId, Integer operatorId) {
        updateExamStatus(examId, operatorId, "closed", "关闭考试");
    }

    @Transactional
    public void delete(Integer examId) {
        getExam(examId);
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, examId));
        examStudentMapper.delete(new LambdaQueryWrapper<ExamStudent>().eq(ExamStudent::getExamId, examId));
        examHistoryMapper.delete(new LambdaQueryWrapper<ExamHistory>().eq(ExamHistory::getExamId, examId));
        studentAnswerMapper.delete(new LambdaQueryWrapper<StudentAnswer>().eq(StudentAnswer::getExamId, examId));
        examResultMapper.delete(new LambdaQueryWrapper<ExamResult>().eq(ExamResult::getExamId, examId));
        examMapper.deleteById(examId);
    }

    public Exam getExam(Integer examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        return exam;
    }

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
                totalScore = totalScore.add(score);
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

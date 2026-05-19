package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.entity.*;
import com.example.olineaqspring.mapper.*;
import com.example.olineaqspring.utils.AnswerMapHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WrongNotebookService {
    private final WrongNotebookMapper notebookMapper;
    private final WrongNotebookItemMapper itemMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final QuestionMapper questionMapper;
    private final ExamMapper examMapper;

    public List<WrongNotebook> listNotebooks(Integer studentId) {
        return notebookMapper.selectList(new LambdaQueryWrapper<WrongNotebook>()
                .eq(WrongNotebook::getStudentId, studentId)
                .orderByDesc(WrongNotebook::getCreateTime));
    }

    @Transactional
    public WrongNotebook createNotebook(Integer studentId, String notebookName, String description) {
        WrongNotebook notebook = new WrongNotebook();
        notebook.setStudentId(studentId);
        notebook.setNotebookName(notebookName);
        notebook.setDescription(description == null ? "" : description);
        notebookMapper.insert(notebook);
        return notebook;
    }

    @Transactional
    public WrongNotebook updateNotebook(Integer notebookId, Integer studentId, String notebookName, String description) {
        WrongNotebook notebook = notebookMapper.selectById(notebookId);
        if (notebook == null) {
            throw new RuntimeException("错题本不存在");
        }
        if (!notebook.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权操作该错题本");
        }
        if (notebookName != null && !notebookName.isBlank()) {
            notebook.setNotebookName(notebookName);
        }
        if (description != null) {
            notebook.setDescription(description);
        }
        notebookMapper.updateById(notebook);
        return notebook;
    }

    @Transactional
    public void deleteNotebook(Integer notebookId, Integer studentId) {
        WrongNotebook notebook = notebookMapper.selectById(notebookId);
        if (notebook == null) {
            throw new RuntimeException("错题本不存在");
        }
        if (!notebook.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权操作该错题本");
        }
        itemMapper.delete(new LambdaQueryWrapper<WrongNotebookItem>()
                .eq(WrongNotebookItem::getNotebookId, notebookId));
        notebookMapper.deleteById(notebookId);
    }

    @Transactional
    public Map<String, Object> addItem(Integer notebookId, Integer studentId, Integer answerId) {
        WrongNotebook notebook = notebookMapper.selectById(notebookId);
        if (notebook == null) {
            throw new RuntimeException("错题本不存在");
        }
        if (!notebook.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权操作该错题本");
        }

        StudentAnswer answer = studentAnswerMapper.selectById(answerId);
        if (answer == null) {
            throw new RuntimeException("答题记录不存在");
        }
        if (!answer.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权操作该答题记录");
        }

        Long exists = itemMapper.selectCount(new LambdaQueryWrapper<WrongNotebookItem>()
                .eq(WrongNotebookItem::getNotebookId, notebookId)
                .eq(WrongNotebookItem::getAnswerId, answerId));
        if (exists != null && exists > 0) {
            throw new RuntimeException("该错题已在本错题本中");
        }

        WrongNotebookItem item = new WrongNotebookItem();
        item.setNotebookId(notebookId);
        item.setAnswerId(answerId);
        itemMapper.insert(item);

        Map<String, Object> result = new HashMap<>();
        result.put("id", item.getId());
        result.put("added", true);
        return result;
    }

    @Transactional
    public void removeItem(Integer notebookId, Integer itemId, Integer studentId) {
        WrongNotebook notebook = notebookMapper.selectById(notebookId);
        if (notebook == null) {
            throw new RuntimeException("错题本不存在");
        }
        if (!notebook.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权操作该错题本");
        }
        WrongNotebookItem item = itemMapper.selectById(itemId);
        if (item == null || !item.getNotebookId().equals(notebookId)) {
            throw new RuntimeException("记录不存在");
        }
        itemMapper.deleteById(itemId);
    }

    public Map<String, Object> getNotebookDetail(Integer notebookId, Integer studentId) {
        WrongNotebook notebook = notebookMapper.selectById(notebookId);
        if (notebook == null) {
            throw new RuntimeException("错题本不存在");
        }
        if (!notebook.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权查看该错题本");
        }

        List<WrongNotebookItem> items = itemMapper.selectList(new LambdaQueryWrapper<WrongNotebookItem>()
                .eq(WrongNotebookItem::getNotebookId, notebookId)
                .orderByDesc(WrongNotebookItem::getCreateTime));

        if (items.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("notebook", notebook);
            result.put("groups", Collections.emptyList());
            result.put("count", 0);
            return result;
        }

        List<Integer> answerIds = items.stream().map(WrongNotebookItem::getAnswerId).distinct().toList();
        List<StudentAnswer> answers = studentAnswerMapper.selectBatchIds(answerIds);

        List<Integer> questionIds = answers.stream().map(StudentAnswer::getQuestionId).distinct().toList();
        List<Integer> examIds = answers.stream().map(StudentAnswer::getExamId).distinct().toList();

        Map<Integer, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity(), (left, right) -> left));
        Map<Integer, Exam> examMap = examMapper.selectBatchIds(examIds).stream()
                .collect(Collectors.toMap(Exam::getExamId, Function.identity(), (left, right) -> left));

        // Reverse mapping: answerId -> WrongNotebookItem.id for removal
        Map<Integer, Integer> answerToItemId = items.stream()
                .collect(Collectors.toMap(WrongNotebookItem::getAnswerId, WrongNotebookItem::getId, (a, b) -> a));

        Map<Integer, List<StudentAnswer>> byExam = answers.stream()
                .collect(Collectors.groupingBy(StudentAnswer::getExamId));

        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<Integer, List<StudentAnswer>> entry : byExam.entrySet()) {
            Integer eid = entry.getKey();
            Exam exam = examMap.get(eid);
            List<Map<String, Object>> questions = entry.getValue().stream().map(answer -> {
                Question question = questionMap.get(answer.getQuestionId());
                Map<String, Object> item = AnswerMapHelper.toAnswerMap(answer, question, true);
                item.put("itemId", answerToItemId.get(answer.getAnswerId()));
                return item;
            }).toList();

            Map<String, Object> group = new HashMap<>();
            group.put("examId", eid);
            group.put("examName", exam == null ? "未知考试" : exam.getExamName());
            group.put("questions", questions);
            groups.add(group);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("notebook", notebook);
        result.put("groups", groups);
        result.put("count", items.size());
        return result;
    }

    public List<Map<String, Object>> getNotebookItemCounts(Integer studentId) {
        List<WrongNotebook> notebooks = listNotebooks(studentId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (WrongNotebook nb : notebooks) {
            Long count = itemMapper.selectCount(new LambdaQueryWrapper<WrongNotebookItem>()
                    .eq(WrongNotebookItem::getNotebookId, nb.getNotebookId()));
            Map<String, Object> m = new HashMap<>();
            m.put("notebookId", nb.getNotebookId());
            m.put("notebookName", nb.getNotebookName());
            m.put("description", nb.getDescription());
            m.put("createTime", nb.getCreateTime());
            m.put("itemCount", count != null ? count.intValue() : 0);
            result.add(m);
        }
        return result;
    }
}

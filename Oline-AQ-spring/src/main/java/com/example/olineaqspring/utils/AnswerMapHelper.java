package com.example.olineaqspring.utils;

import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.StudentAnswer;

import java.util.HashMap;
import java.util.Map;

public class AnswerMapHelper {

    public static Map<String, Object> toAnswerMap(StudentAnswer answer, Question question, boolean forWrongBook) {
        Map<String, Object> item = new HashMap<>();
        if (forWrongBook) {
            item.put("answerId", answer.getAnswerId());
        }
        item.put("questionId", answer.getQuestionId());
        item.put("questionContent", question == null ? "" : question.getQuestionContent());
        item.put("questionType", question == null ? "single" : question.getQuestionType());
        item.put("optionA", question == null ? "" : question.getOptionA());
        item.put("optionB", question == null ? "" : question.getOptionB());
        item.put("optionC", question == null ? "" : question.getOptionC());
        item.put("optionD", question == null ? "" : question.getOptionD());
        item.put("studentAnswer", answer.getStudentAnswer());
        item.put("correctAnswer", answer.getCorrectAnswer());
        item.put("score", answer.getScore());
        if (forWrongBook) {
            item.put("submitTime", answer.getSubmitTime());
        } else {
            item.put("isCorrect", answer.getIsCorrect());
        }
        return item;
    }
}

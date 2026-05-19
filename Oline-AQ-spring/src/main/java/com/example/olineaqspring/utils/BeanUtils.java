package com.example.olineaqspring.utils;

import com.example.olineaqspring.dto.QuestionRequest;
import com.example.olineaqspring.entity.Question;

public class BeanUtils {

    public static void copyQuestion(QuestionRequest source, Question target) {
        if (source.getQuestionContent() != null) {
            target.setQuestionContent(source.getQuestionContent());
        }
        if (source.getQuestionType() != null) {
            target.setQuestionType(source.getQuestionType());
        }
        if (source.getOptionA() != null) target.setOptionA(source.getOptionA());
        if (source.getOptionB() != null) target.setOptionB(source.getOptionB());
        if (source.getOptionC() != null) target.setOptionC(source.getOptionC());
        if (source.getOptionD() != null) target.setOptionD(source.getOptionD());
        if (source.getCorrectAnswer() != null) {
            target.setCorrectAnswer(source.getCorrectAnswer());
        }
        if (source.getScore() != null) {
            target.setScore(source.getScore());
        }
        if (source.getCategory() != null) {
            target.setCategory(source.getCategory());
        }
    }
}

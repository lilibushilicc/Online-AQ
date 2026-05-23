package com.example.olineaqspring.utils;

import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.StudentAnswer;
import com.example.olineaqspring.vo.ResultAnswerVO;
import com.example.olineaqspring.vo.WrongQuestionItemVO;

public class AnswerMapHelper {

    public static ResultAnswerVO toResultAnswerVO(StudentAnswer answer, Question question, boolean includeCorrectAnswer) {
        ResultAnswerVO item = new ResultAnswerVO();
        item.setAnswerId(answer.getAnswerId());
        item.setQuestionId(answer.getQuestionId());
        item.setQuestionContent(question == null ? "" : question.getQuestionContent());
        item.setQuestionType(question == null ? "single" : question.getQuestionType());
        item.setOptionA(question == null ? "" : question.getOptionA());
        item.setOptionB(question == null ? "" : question.getOptionB());
        item.setOptionC(question == null ? "" : question.getOptionC());
        item.setOptionD(question == null ? "" : question.getOptionD());
        item.setStudentAnswer(answer.getStudentAnswer());
        item.setCorrectAnswer(includeCorrectAnswer ? answer.getCorrectAnswer() : null);
        item.setScore(answer.getScore());
        item.setIsCorrect(answer.getIsCorrect());
        item.setReviewStatus(answer.getReviewStatus());
        return item;
    }

    public static WrongQuestionItemVO toWrongQuestionItemVO(StudentAnswer answer, Question question, boolean includeCorrectAnswer) {
        WrongQuestionItemVO item = new WrongQuestionItemVO();
        item.setAnswerId(answer.getAnswerId());
        item.setQuestionId(answer.getQuestionId());
        item.setQuestionContent(question == null ? "" : question.getQuestionContent());
        item.setQuestionType(question == null ? "single" : question.getQuestionType());
        item.setOptionA(question == null ? "" : question.getOptionA());
        item.setOptionB(question == null ? "" : question.getOptionB());
        item.setOptionC(question == null ? "" : question.getOptionC());
        item.setOptionD(question == null ? "" : question.getOptionD());
        item.setStudentAnswer(answer.getStudentAnswer());
        item.setCorrectAnswer(includeCorrectAnswer ? answer.getCorrectAnswer() : null);
        item.setScore(answer.getScore());
        item.setSubmitTime(answer.getSubmitTime());
        item.setReviewStatus(answer.getReviewStatus());
        return item;
    }
}

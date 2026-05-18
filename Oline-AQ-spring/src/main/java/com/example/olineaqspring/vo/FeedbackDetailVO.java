package com.example.olineaqspring.vo;

import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.QuestionFeedback;
import lombok.Data;

@Data
public class FeedbackDetailVO {
    private QuestionFeedback feedback;
    private Question question;
    private String studentName;
}

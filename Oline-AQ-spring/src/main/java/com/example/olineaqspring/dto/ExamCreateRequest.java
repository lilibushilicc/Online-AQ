package com.example.olineaqspring.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamCreateRequest {
    @Data
    public static class QuestionScoreItem {
        private Integer questionId;
        private BigDecimal score;
    }

    private String examName;
    private String description;
    private Integer duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allowRetake;
    private List<Integer> questionIds;
    private List<QuestionScoreItem> questionScores;
}

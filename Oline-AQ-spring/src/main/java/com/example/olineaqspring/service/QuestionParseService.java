package com.example.olineaqspring.service;

import com.example.olineaqspring.entity.Question;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionParseService {
    private static final Pattern ANSWER_PATTERN = Pattern.compile("答案[:：]\\s*([ABCD对错TF])", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEXT_ANSWER_PATTERN = Pattern.compile("答案[:：]\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILL_BLANK_PATTERN = Pattern.compile("[_＿]{2,}|（\\s*）|\\(\\s*\\)");

    public List<Question> parse(String rawText, Integer sourceFileId) {
        return parse(rawText, sourceFileId, null);
    }

    public List<Question> parse(String rawText, Integer sourceFileId, String category) {
        List<Question> questions = new ArrayList<>();
        String normalized = rawText == null ? "" : rawText.replace("\r\n", "\n").replace("\r", "\n");
        String[] blocks = normalized.split("\\n(?=\\d+\\.\\s*)");
        for (String block : blocks) {
            Question question = parseBlock(block.trim(), sourceFileId);
            if (question != null) {
                question.setCategory(category);
                questions.add(question);
            }
        }
        return questions;
    }

    private Question parseBlock(String block, Integer sourceFileId) {
        if (block.isBlank()) return null;
        String[] lines = block.split("\\n");
        String content = lines[0].replaceFirst("^\\d+\\.\\s*", "").trim();
        if (content.isBlank()) return null;

        // Try to match A-D options format first
        String optionA = pickOption(block, "A");
        String optionB = pickOption(block, "B");
        String optionC = pickOption(block, "C");
        String optionD = pickOption(block, "D");

        Question question = new Question();
        question.setQuestionContent(content);
        question.setScore(BigDecimal.valueOf(5));
        question.setSourceFileId(sourceFileId);

        // Has options → single choice or judge
        if (!optionA.isBlank() && !optionB.isBlank()) {
            Matcher answerMatcher = ANSWER_PATTERN.matcher(block);
            if (!answerMatcher.find()) return null;

            question.setOptionA(optionA);
            question.setOptionB(optionB);
            question.setOptionC(optionC);
            question.setOptionD(optionD);
            question.setCorrectAnswer(answerMatcher.group(1).toUpperCase());
            question.setQuestionType(!optionC.isBlank() || !optionD.isBlank() ? "single" : "judge");
            return question;
        }

        // No options → short answer or fill blank
        Matcher textAnswerMatcher = TEXT_ANSWER_PATTERN.matcher(block);
        if (!textAnswerMatcher.find()) return null;

        question.setCorrectAnswer(textAnswerMatcher.group(1).trim());
        question.setQuestionType(FILL_BLANK_PATTERN.matcher(content).find() ? "fill_blank" : "short_answer");
        return question;
    }

    private String pickOption(String block, String letter) {
        Matcher matcher = Pattern.compile("^" + letter + "\\.\\s*(.+)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(block);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
}

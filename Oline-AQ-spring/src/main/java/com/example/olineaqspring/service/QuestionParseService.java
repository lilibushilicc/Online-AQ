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
    private static final Pattern ANSWER_LINE_PATTERN = Pattern.compile("^答案[:：]\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILL_BLANK_PATTERN = Pattern.compile("[_＿]{2,}|（\\s*）|\\(\\s*\\)");
    private static final Pattern NUMBER_PREFIX_PATTERN = Pattern.compile("^\\d+[.、．)]\\s*");
    private static final Pattern OPTION_PATTERN = Pattern.compile("^([A-D])\\s*[.．、]\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION_HEADING_PATTERN = Pattern.compile("^[一二三四五六七八九十]+[、.．]\\s*.*(选择题|单选题|判断题|填空题|简答题|问答题).*");

    public List<Question> parse(String rawText, Integer sourceFileId) {
        return parse(rawText, sourceFileId, null);
    }

    public List<Question> parse(String rawText, Integer sourceFileId, String category) {
        List<Question> questions = new ArrayList<>();
        String normalized = rawText == null ? "" : rawText.replace("\r\n", "\n").replace("\r", "\n");
        for (String block : splitBlocks(normalized)) {
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
        ParsedBlock parsedBlock = parseLines(block);
        String content = NUMBER_PREFIX_PATTERN.matcher(parsedBlock.content()).replaceFirst("").trim();
        if (content.isBlank()) return null;
        if (isSectionHeading(content)) return null;

        Question question = new Question();
        question.setQuestionContent(content);
        question.setScore(BigDecimal.valueOf(5));
        question.setSourceFileId(sourceFileId);

        String optionA = parsedBlock.optionA();
        String optionB = parsedBlock.optionB();
        String optionC = parsedBlock.optionC();
        String optionD = parsedBlock.optionD();
        String answer = parsedBlock.answer().trim();

        if (!optionA.isBlank() && !optionB.isBlank()) {
            if (answer.isBlank()) return null;
            question.setOptionA(optionA);
            question.setOptionB(optionB);
            question.setOptionC(optionC);
            question.setOptionD(optionD);
            question.setCorrectAnswer(normalizeChoiceAnswer(answer));
            question.setQuestionType(isJudgeOptions(optionA, optionB, optionC, optionD) ? "judge" : "single");
            return question;
        }

        if (answer.isBlank()) return null;

        question.setCorrectAnswer(answer);
        question.setQuestionType(FILL_BLANK_PATTERN.matcher(content).find() ? "fill_blank" : "short_answer");
        return question;
    }

    private List<String> splitBlocks(String normalized) {
        List<String> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        boolean hasAnswer = false;

        for (String rawLine : normalized.split("\\n")) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }
            if (isSectionHeading(line)) {
                if (!current.isEmpty() && hasAnswer) {
                    blocks.add(String.join("\n", current));
                    current.clear();
                    hasAnswer = false;
                }
                continue;
            }
            if (!current.isEmpty() && hasAnswer && isLikelyQuestionStart(line)) {
                blocks.add(String.join("\n", current));
                current.clear();
                hasAnswer = false;
            }
            current.add(line);
            if (ANSWER_LINE_PATTERN.matcher(line).find()) {
                hasAnswer = true;
            }
        }

        if (!current.isEmpty()) {
            blocks.add(String.join("\n", current));
        }
        return blocks;
    }

    private ParsedBlock parseLines(String block) {
        StringBuilder content = new StringBuilder();
        StringBuilder answer = new StringBuilder();
        String optionA = "";
        String optionB = "";
        String optionC = "";
        String optionD = "";
        boolean readingAnswer = false;

        for (String rawLine : block.split("\\n")) {
            String line = rawLine.trim();
            if (line.isBlank() || isSectionHeading(line)) {
                continue;
            }

            Matcher optionMatcher = OPTION_PATTERN.matcher(line);
            if (!readingAnswer && optionMatcher.matches()) {
                String value = optionMatcher.group(2).trim();
                switch (optionMatcher.group(1).toUpperCase()) {
                    case "A" -> optionA = value;
                    case "B" -> optionB = value;
                    case "C" -> optionC = value;
                    case "D" -> optionD = value;
                    default -> {
                    }
                }
                continue;
            }

            Matcher answerMatcher = ANSWER_LINE_PATTERN.matcher(line);
            if (answerMatcher.matches()) {
                readingAnswer = true;
                appendLine(answer, answerMatcher.group(1).trim());
                continue;
            }

            if (readingAnswer) {
                appendLine(answer, line);
            } else {
                appendLine(content, line);
            }
        }

        return new ParsedBlock(content.toString(), optionA, optionB, optionC, optionD, answer.toString());
    }

    private void appendLine(StringBuilder builder, String line) {
        if (line.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(line);
    }

    private boolean isSectionHeading(String line) {
        return SECTION_HEADING_PATTERN.matcher(line.trim()).matches();
    }

    private boolean isLikelyQuestionStart(String line) {
        String trimmed = line.trim();
        if (OPTION_PATTERN.matcher(trimmed).matches() || ANSWER_LINE_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        if (trimmed.matches("^[^？?。.!！]{1,12}[:：].+")) {
            return false;
        }
        return NUMBER_PREFIX_PATTERN.matcher(trimmed).find()
                || trimmed.contains("（）")
                || trimmed.contains("( )")
                || trimmed.contains("什么")
                || trimmed.contains("哪些")
                || trimmed.contains("区别")
                || trimmed.contains("简述")
                || trimmed.endsWith("？")
                || trimmed.endsWith("?");
    }

    private boolean isJudgeOptions(String optionA, String optionB, String optionC, String optionD) {
        if (!optionC.isBlank() || !optionD.isBlank()) {
            return false;
        }
        String a = optionA.trim();
        String b = optionB.trim();
        return ("对".equals(a) && "错".equals(b))
                || ("正确".equals(a) && "错误".equals(b))
                || ("T".equalsIgnoreCase(a) && "F".equalsIgnoreCase(b))
                || ("True".equalsIgnoreCase(a) && "False".equalsIgnoreCase(b));
    }

    private String normalizeChoiceAnswer(String answer) {
        String normalized = answer.trim();
        if (normalized.length() > 1 && normalized.matches("(?i)^[ABCD对错TF].*")) {
            normalized = normalized.substring(0, 1);
        }
        return normalized.toUpperCase();
    }

    private record ParsedBlock(
            String content,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String answer
    ) {
    }
}

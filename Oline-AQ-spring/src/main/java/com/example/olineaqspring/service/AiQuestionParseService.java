package com.example.olineaqspring.service;

import com.example.olineaqspring.entity.Question;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiQuestionParseService {

    private final RestTemplate restTemplate;
    private final ConfigService configService;
    private final QuestionParseService questionParseService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern SECTION_HEADING_PATTERN = Pattern.compile("^[一二三四五六七八九十]+[、.．]\\s*.*(选择题|单选题|判断题|填空题|简答题|问答题).*");

    private static final String PROMPT_TEMPLATE = """
            从以下文本中提取所有题目，返回 JSON 数组（不要 markdown 代码块标记）。

            规则：
            - 文本中的"一、选择题"这类章节标题不是题目，跳过
            - 逐题提取，不要遗漏
            - 有 A/B/C/D 选项的 → type 为 "single"，correctAnswer 填字母
            - 只有 A/B 两个选项且内容是对/错 → type 为 "judge"
            - 没有选项的简答/问答 → type 为 "short_answer"
            - 有 ____ 空格的 → type 为 "fill_blank"

            每个对象格式：
            {
              "questionContent": "题干",
              "questionType": "single/judge/short_answer/fill_blank",
              "optionA": "A选项（无则空字符串）",
              "optionB": "B选项（无则空字符串）",
              "optionC": "C选项（无则空字符串）",
              "optionD": "D选项（无则空字符串）",
              "correctAnswer": "正确答案",
              "score": 5
            }

            文本：
            """;

    public List<Question> parse(String rawText, Integer sourceFileId, String category) {
        String endpoint = normalizeEndpoint(configService.get("ai.endpoint"));
        String apiKey = configService.get("ai.api_key");
        String model = configService.get("ai.model");

        if (endpoint == null || apiKey == null) {
            throw new RuntimeException("AI 解析未配置，请先在系统设置中配置 AI 参数");
        }
        if (model == null || model.isBlank()) {
            model = "deepseek-chat";
        }

        String prompt = PROMPT_TEMPLATE + rawText;

        try {
            String response = callLlm(endpoint, apiKey, model, prompt);
            List<Map<String, Object>> items = objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
            List<Question> questions = new ArrayList<>();
            for (Map<String, Object> item : items) {
                Question q = new Question();
                q.setQuestionContent(readString(item, "questionContent"));
                q.setQuestionType(normalizeQuestionType(readString(item, "questionType", "type")));
                q.setOptionA(readString(item, "optionA"));
                q.setOptionB(readString(item, "optionB"));
                q.setOptionC(readString(item, "optionC"));
                q.setOptionD(readString(item, "optionD"));
                q.setCorrectAnswer(readString(item, "correctAnswer", "answer"));
                Object score = item.get("score");
                q.setScore(score instanceof Number ? BigDecimal.valueOf(((Number) score).doubleValue()) : BigDecimal.valueOf(5));
                q.setCategory(category);
                q.setSourceFileId(sourceFileId);
                if (isValidQuestion(q)) {
                    questions.add(q);
                }
            }
            List<Question> localQuestions = questionParseService.parse(rawText, sourceFileId, category);
            if (questions.isEmpty() || localQuestions.size() > questions.size()) {
                return localQuestions;
            }
            return questions;
        } catch (Exception e) {
            throw new RuntimeException("AI 解析失败：" + e.getMessage());
        }
    }

    public String testConnection() {
        String endpoint = normalizeEndpoint(configService.get("ai.endpoint"));
        String apiKey = configService.get("ai.api_key");
        String model = configService.get("ai.model");
        if (model == null || model.isBlank()) model = "deepseek-chat";
        return callLlm(endpoint, apiKey, model, "请回复 ok");
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint == null) return null;
        endpoint = endpoint.trim();
        if (!endpoint.endsWith("/chat/completions")) {
            if (!endpoint.endsWith("/")) {
                endpoint += "/";
            }
            endpoint += "chat/completions";
        }
        return endpoint;
    }

    @SuppressWarnings("unchecked")
    private String callLlm(String endpoint, String apiKey, String model, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.1,
                "max_tokens", 8192
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);
        Map body = response.getBody();
        if (body == null) throw new RuntimeException("AI 返回为空");

        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        if (choices == null || choices.isEmpty()) throw new RuntimeException("AI 返回 choices 为空");

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) throw new RuntimeException("AI 返回 message 为空");

        String content = (String) message.get("content");
        if (content == null) throw new RuntimeException("AI 返回 content 为空");

        // Strip markdown code block if present
        content = content.trim();
        if (content.startsWith("```")) {
            content = content.replaceAll("(?s)^```\\w*\\n*", "").replaceAll("(?s)\\n*```$", "").trim();
        }

        return content;
    }

    private String readString(Map<String, Object> item, String... keys) {
        for (String key : keys) {
            Object value = item.get(key);
            if (value != null) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private String normalizeQuestionType(String questionType) {
        if (questionType == null || questionType.isBlank()) {
            return "single";
        }
        String normalized = questionType.trim();
        return switch (normalized) {
            case "单选", "选择题", "single_choice" -> "single";
            case "判断", "判断题", "true_false" -> "judge";
            case "简答", "问答", "简答题", "问答题" -> "short_answer";
            case "填空", "填空题" -> "fill_blank";
            default -> normalized;
        };
    }

    private boolean isValidQuestion(Question question) {
        String content = question.getQuestionContent() == null ? "" : question.getQuestionContent().trim();
        if (content.isBlank() || SECTION_HEADING_PATTERN.matcher(content).matches()) {
            return false;
        }
        String type = question.getQuestionType();
        if (type == null || type.isBlank()) {
            question.setQuestionType("single");
        }
        if (question.getScore() == null) {
            question.setScore(BigDecimal.valueOf(5));
        }
        return true;
    }
}
